package com.ensoftcorp.open.jimple.commons.ui.views.decompiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wb.swt.ResourceManager;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.index.common.SourceCorrespondence;
import com.ensoftcorp.atlas.core.log.Log;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.utilities.NodeSourceCorrespondenceSorter;
import com.ensoftcorp.open.commons.utilities.WorkspaceUtils;
import com.ensoftcorp.open.commons.utilities.selection.GraphSelectionListenerView;
import com.ensoftcorp.open.java.commons.analysis.CommonQueries;
import com.ensoftcorp.open.java.commons.bytecode.JarInspector;

public class CFRDecompilerCorrespondenceView extends GraphSelectionListenerView {
	
	private File tempDirectory = null;
	
	public CFRDecompilerCorrespondenceView() {
		setPartName("CFR Decompiler Correspondence");
		setTitleImage(ResourceManager.getPluginImage("com.ensoftcorp.open.jimple.commons", "icons/partial.gif"));
		
		try {
			tempDirectory = Files.createTempDirectory("CFRDecompilerExtractedJars").toFile();
			tempDirectory.deleteOnExit(); // cleanup later if something bad happens
		} catch (IOException e) {
			Log.error("Could not create temp directory.", e);
		}
	}
	
	/**
	 * Helper function to recursively delete a file or directory
	 * @param file
	 * @throws FileNotFoundException
	 */
	private static void delete(File file) throws FileNotFoundException {
		if(file.exists()) {
			if (file.isDirectory()) {
				for (File c : file.listFiles()) {
					delete(c);
				}
			}
			if (!file.delete()){
				throw new FileNotFoundException("Failed to delete file: " + file);
			}
		}
	}
	
	@Override
	public void dispose(){
		try {
			delete(tempDirectory);
		} catch (FileNotFoundException e) {
			// just a best effort...
		}
		super.dispose();
	}

	private StyledText textArea;
	
	@Override
	public void createPartControl(Composite arg0) {
		textArea = new StyledText(arg0, SWT.BORDER);
		textArea.setTopMargin(5);
		textArea.setBottomMargin(5);
		textArea.setLeftMargin(5);
		textArea.setRightMargin(5);
		if(indexExists()){
			textArea.setText("Empty Selection.");
		} else {
			textArea.setText("Map a Jimple project.");
		}
		textArea.setEditable(false);
		
		registerGraphHandlers();
	}

	@Override
	public void setFocus() {}

	@Override
	public void selectionChanged(Graph selection) {
		AtlasSet<Node> filteredSelection = filter(selection);
		AtlasSet<Node> selectedMethods = Common.toQ(filteredSelection).nodes(XCSG.Method).eval().nodes();
		AtlasSet<Node> containingMethods = CommonQueries.getContainingMethods(Common.toQ(filteredSelection).difference(Common.toQ(selectedMethods))).eval().nodes();
		AtlasSet<Node> methods = Common.toQ(selectedMethods).union(Common.toQ(containingMethods)).eval().nodes();
		if(methods.isEmpty()){
			textArea.setText("Empty Selection.");
		} else if(methods.size() > 1){
			ArrayList<Node> sortedMethods = new ArrayList<Node>((int) methods.size());
			for(Node method : methods){
				sortedMethods.add(method);
			}
			Collections.sort(sortedMethods, new NodeSourceCorrespondenceSorter());
			StringBuilder text = new StringBuilder();
			for(Node method : sortedMethods){
				text.append("\n...\n");
				try {
					File extractedJar = getOrCreateExtractedJar(method);
					text.append(decompileMethod(extractedJar, method));
				} catch (Exception e) {
					String qualifiedMethod = CommonQueries.getQualifiedMethodName(method);
					text.append("ERROR: " + qualifiedMethod);
					Log.warning("Decompilation Error in " + qualifiedMethod, e);
				}
			}
			text.append("\n...");
			textArea.setText(text.toString().trim());
		} else {
			Node method = methods.one();
			try {
				File extractedJar = getOrCreateExtractedJar(method);
				textArea.setText("...\n" + decompileMethod(extractedJar, method) + "\n...");
			} catch (Exception e) {
				String qualifiedMethod = CommonQueries.getQualifiedMethodName(method);
				textArea.setText("...\nERROR: " + CommonQueries.getQualifiedMethodName(method) + "\n...");
				Log.warning("Decompilation Error in " + qualifiedMethod, e);
			}
		}
	}
	
	private AtlasSet<Node> filter(Graph selection){
		return Common.toQ(selection)
			   .nodes(XCSG.DataFlow_Node, XCSG.ControlFlow_Node, XCSG.Method)
			   .nodes(XCSG.Language.Jimple).eval().nodes();
	}
	
	private File getOrCreateExtractedJar(Node method) throws FileNotFoundException {
		if(tempDirectory == null){
			throw new FileNotFoundException("Could not access temp directory.");
		} else {
			Node container = Common.toQ(method).containers().nodes(XCSG.Library, XCSG.Project).eval().nodes().one();
			if(container == null){
				throw new IllegalArgumentException("Method " + method.getAttr(XCSG.name).toString() + " is not contained in a library or project.");
			} else {
				File jarFile = null;
				if(container.taggedWith(XCSG.Library)){
					SourceCorrespondence sc = (SourceCorrespondence) container.getAttr(XCSG.sourceCorrespondence);
					if(sc != null){
						try {
							jarFile = WorkspaceUtils.getFile(sc.sourceFile);
						} catch (CoreException e) {
							// best effort
						}
					} else {
						// is the container name the file path that exists?
						File containerFile = new File(container.getAttr(XCSG.name).toString());
						if(containerFile.exists()){
							jarFile = containerFile;
						} else {
							// no source correspondence...search indexed projects for jars
							for(Node projectNode : Common.universe().nodes(XCSG.Project).eval().nodes()){
								IProject project = WorkspaceUtils.getProject(projectNode.getAttr(XCSG.name).toString());
								if(project.exists()){
									File projectDirectory = new File(project.getLocation().toOSString());
									for(File jar : findJars(projectDirectory)){
										if(jar.getName().equals(containerFile.getName())){
											jarFile = jar;
											break;
										}
									}
								}
							}
						}
					}
				} else {
					// TODO: how to find corresponding jar for a project selection?
					// annoying we don't even know the jar name for this case...
					throw new UnsupportedOperationException("Selections within project containers are currently not supported.");
				}
				
				if(jarFile == null){
					throw new FileNotFoundException("Could not find corresponding Jar file for method " + method.getAttr(XCSG.name).toString() + ".");
				}
				
				File extractedJarDirectory = new File(tempDirectory.getAbsolutePath() + File.separator + jarFile.getName());
				if(extractedJarDirectory.exists()){
					return extractedJarDirectory;
				} else {
					try {
						extractedJarDirectory.mkdirs();
						JarInspector inspector = new JarInspector(jarFile);
						for(String entry : inspector.getJarEntrySet()){
							byte[] bytes = inspector.extractEntry(entry);
							File path = new File(extractedJarDirectory.getAbsolutePath() + File.separator + entry.replace("/", File.separator));
							path.getParentFile().mkdirs();
							FileOutputStream writer = new FileOutputStream(path);
							writer.write(bytes);
							writer.close();
						}
						return extractedJarDirectory;
					} catch (Exception e){
						throw new RuntimeException("Failed to extract library Jar contents for " + jarFile.getAbsolutePath());
					}
				}
			}
		}
	}
	
	// helper method for recursively finding jar files in a given directory
	private static List<File> findJars(File directory){
		ArrayList<File> jimple = new ArrayList<File>();
		if(directory.exists()){
			if (directory.isDirectory()) {
				for (File f : directory.listFiles()) {
					jimple.addAll(findJars(f));
				}
			}
			File file = directory;
			if(file.getName().endsWith(".jar")){
				jimple.add(file);
			}
		}
		return jimple;
	}
	
	private String decompileMethod(File extractedJar, Node method){
		Node classNode = Common.toQ(method).parent().nodes(XCSG.Classifier).eval().nodes().one();
		if(classNode == null){
			return method.getAttr(XCSG.name).toString() + " has no containing class.";
		} else {
			String qualifiedClass = CommonQueries.getQualifiedTypeName(classNode);
			File classFile = new File(extractedJar.getAbsolutePath() + File.separator + qualifiedClass.replace(".", File.separator) + ".class");
			if(!classFile.exists()){
				return "Could not find corresponding class file: " + classFile.getAbsolutePath();
			} else {
				String[] args = new String[]{classFile.getAbsolutePath(), method.getAttr(XCSG.name).toString(), "--silent"};

				// temporarily redirect System.out to byte array
			    ByteArrayOutputStream baos = new ByteArrayOutputStream();
			    PrintStream alternatePrintStream = new PrintStream(baos);
			    PrintStream originalPrintStream = System.out;
			    try {
			    	System.setOut(alternatePrintStream);
			    	org.benf.cfr.reader.Main.main(args);
			    	System.out.flush();
			    } finally {
			    	System.setOut(originalPrintStream);
			    }
			    
			    return baos.toString().trim();
			}
		}
	}
	
	@Override
	public void indexBecameUnaccessible() {
		textArea.setText("Map a Jimple project.");
	}

	@Override
	public void indexBecameAccessible() {
		textArea.setText("Empty Selection.");
	}
}
