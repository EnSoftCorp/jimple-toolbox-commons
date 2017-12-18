package com.ensoftcorp.open.jimple.commons.ui.views.decompiler;

import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

import javax.swing.ScrollPaneConstants;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wb.swt.ResourceManager;
import org.fife.rsyntaxtextarea.themes.Themes;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
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
import com.ensoftcorp.open.jimple.commons.soot.Compilation;
import com.ensoftcorp.open.jimple.commons.soot.SootConversionException;

public class CFRDecompilerCorrespondenceView extends GraphSelectionListenerView {
	
	private static final boolean SEARCH_FOR_JAR = false;
	
	private File tempDirectory = null;
	private File extractedJarsDirectory = null;
	private File compiledClassesDirectory = null;
	private Label statusLabel;
	private RSyntaxTextArea textArea;
	
	public CFRDecompilerCorrespondenceView() {
		setPartName("CFR Decompiler Correspondence");
		setTitleImage(ResourceManager.getPluginImage("com.ensoftcorp.open.jimple.commons", "icons/partial.gif"));
		try {
			tempDirectory = Files.createTempDirectory("CFRDecompiler").toFile();
			tempDirectory.deleteOnExit(); // cleanup later if something bad happens
			extractedJarsDirectory = new File(tempDirectory.getAbsolutePath() + File.separator + "ExtractedJars");
			extractedJarsDirectory.mkdirs();
			compiledClassesDirectory = new File(tempDirectory.getAbsolutePath() + File.separator + "CompiledClasses");
			compiledClassesDirectory.mkdirs();
		} catch (IOException e) {
			Log.error("Could not create temp directory.", e);
		}
	}
	
	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		
		// add a syntax highlighted text area
		// size will grow on demand in scroll panel, this is really just a min size
		textArea = new RSyntaxTextArea(5, 5); 
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		textArea.setCodeFoldingEnabled(true);
		textArea.setAntiAliasingEnabled(true);
		textArea.setEditable(false);
		if(Themes.ECLIPSE != null){
			Themes.ECLIPSE.apply(textArea);
		}
		
		// add a mark occurrences helper
		// adapted from: https://github.com/bobbylight/RSyntaxTextArea/issues/88
		textArea.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1) {
					textArea.clearMarkAllHighlights();
				} else if (e.getClickCount() == 2) {
					String word = textArea.getSelectedText().trim();
					if (word != null && !word.isEmpty()){
						markOccurrences(word);
					}
				}
			}
		});
		
		// place highlighted text inside a scroll panel
		RTextScrollPane scrollPanel = new RTextScrollPane(textArea);
		scrollPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		// convert swing component to swt
		Composite composite = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		Frame frame = SWT_AWT.new_Frame(composite);
		frame.add(scrollPanel);
		
		statusLabel = new Label(parent, SWT.NONE);
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		if(indexExists()){
			statusLabel.setText("Empty Selection.");
		} else {
			statusLabel.setText("Map a Jimple project.");
		}
		
		registerGraphHandlers();
	}

	@Override
	public void setFocus() {}

	private AtlasSet<Node> lastSelectedMethodSet = new AtlasHashSet<Node>();
	
	@Override
	public void selectionChanged(Graph selection) {
		AtlasSet<Node> filteredSelection = filter(selection);
		AtlasSet<Node> selectedMethods = Common.toQ(filteredSelection).nodes(XCSG.Method).eval().nodes();
		AtlasSet<Node> containingMethods = CommonQueries.getContainingMethods(Common.toQ(filteredSelection).difference(Common.toQ(selectedMethods))).eval().nodes();
		AtlasSet<Node> methods = Common.toQ(selectedMethods).union(Common.toQ(containingMethods)).eval().nodes();
		
		boolean sizesEqual = lastSelectedMethodSet.size() == methods.size();
		boolean setContentsEqual = Common.toQ(lastSelectedMethodSet).intersection(Common.toQ(methods)).eval().nodes().size() == lastSelectedMethodSet.size();
		if(sizesEqual && setContentsEqual){
			// effectively the same selection...no need to recompute results
			// however we might want to update marked occurrences
			markOccurrences(filteredSelection);
			return;
		} else {
			lastSelectedMethodSet.clear();
			lastSelectedMethodSet.addAll(methods);
		}
		
		if(methods.isEmpty()){
			statusLabel.setText("Empty Selection.");
		} else if(methods.size() > 1){
			statusLabel.setText("Selection: " + methods.size() + " methods.");
			ArrayList<Node> sortedMethods = new ArrayList<Node>((int) methods.size());
			for(Node method : methods){
				sortedMethods.add(method);
			}
			Collections.sort(sortedMethods, new NodeSourceCorrespondenceSorter());
			StringBuilder text = new StringBuilder();
			for(Node method : sortedMethods){
				text.append("\n...\n");
				if(SEARCH_FOR_JAR){
					try {
						File extractedJar = getOrCreateExtractedJar(method);
						try {
							text.append(decompileMethodFromJar(extractedJar, method));
						} catch (Exception e) {
							String qualifiedMethod = CommonQueries.getQualifiedMethodName(method);
							text.append("CFR ERROR: " + qualifiedMethod);
						}
					} catch (Exception e) {
						String qualifiedMethod = CommonQueries.getQualifiedMethodName(method);
						text.append("SEARCH ERROR: " + qualifiedMethod);
					}
				} else {
					try {
						File compiledClass = getOrCreateCompiledClassFile(method);
						try {
							text.append(decompileMethodFromClass(compiledClass, method));
						} catch (Exception e) {
							String qualifiedMethod = CommonQueries.getQualifiedMethodName(method);
							text.append("CFR ERROR: " + qualifiedMethod);
						}
					} catch (Exception e) {
						String qualifiedMethod = CommonQueries.getQualifiedMethodName(method);
						text.append("SOOT ERROR: " + qualifiedMethod);
					}
				}
			}
			text.append("\n...");
			textArea.setText(text.toString().trim());
		} else {
			statusLabel.setText("Selection: " + methods.size() + " method.");
			Node method = methods.one();
			try {
				if(SEARCH_FOR_JAR){
					try {
						File extractedJar = getOrCreateExtractedJar(method);
						try {
							textArea.setText("...\n" + decompileMethodFromJar(extractedJar, method) + "\n...");
						} catch (Exception e) {
							textArea.setText("...\nCFR ERROR: " + CommonQueries.getQualifiedMethodName(method) + "\n...");
						}
					} catch (Exception e) {
						textArea.setText("...\nSEARCH ERROR: " + CommonQueries.getQualifiedMethodName(method) + "\n...");
					}
				} else {
					try {
						File compiledClass = getOrCreateCompiledClassFile(method);
						try {
							textArea.setText("...\n" + decompileMethodFromClass(compiledClass, method) + "\n...");
						} catch (Exception e) {
							textArea.setText("...\nCFR ERROR: " + CommonQueries.getQualifiedMethodName(method) + "\n...");
						}
					} catch (Exception e) {
						textArea.setText("...\nSOOT ERROR: " + CommonQueries.getQualifiedMethodName(method) + "\n...");
					}
				}
			} catch (Exception e) {
				textArea.setText("...\nERROR: " + CommonQueries.getQualifiedMethodName(method) + "\n...");
			}
			markOccurrences(filteredSelection);
		}
	}

	private void markOccurrences(AtlasSet<Node> selection) {
		AtlasSet<Node> dataFlowNodes = Common.toQ(selection).nodes(XCSG.DataFlow_Node).eval().nodes();
		boolean occurrencesMarked = false;
		if(dataFlowNodes.size() == 1){
			Node df = dataFlowNodes.one();
			String word = "";
			if(df.taggedWith(XCSG.Operator)){
				// operators are a little weird, so skipping
			} if(df.taggedWith(XCSG.Assignment)) {
				word = df.getAttr(XCSG.name).toString().trim();
				// remove the "="
				if(word.endsWith("=")){
					word = word.substring(0, word.length() - 1);
					word = word.trim();
				}
			} else {
				word = df.getAttr(XCSG.name).toString().trim();
			}
			// if we have a word, mark the occurrences of the word
			if(!word.isEmpty()){
				markOccurrences(word);
				occurrencesMarked = true;
			}
		}
		if(!occurrencesMarked){
			textArea.clearMarkAllHighlights();
		}
	}

	private void markOccurrences(String word) {
		textArea.clearMarkAllHighlights();
		SearchContext context = new SearchContext(word);
		context.setMarkAll(true);
		context.setMatchCase(true);
		context.setWholeWord(true);
		SearchEngine.markAll(textArea, context);
	}
	
	private AtlasSet<Node> filter(Graph selection){
		return Common.toQ(selection)
			   .nodes(XCSG.DataFlow_Node, XCSG.ControlFlow_Node, XCSG.Method)
			   .nodes(XCSG.Language.Jimple).eval().nodes();
	}
	
	private File getOrCreateCompiledClassFile(Node method) throws IOException, CoreException, SootConversionException {
		if(compiledClassesDirectory == null){
			throw new FileNotFoundException("Could not access temporary compiled class file directory.");
		} else {
			SourceCorrespondence sc = (SourceCorrespondence) method.getAttr(XCSG.sourceCorrespondence);
			if(sc != null){
				// compiles all of the jimple in the corresponding project...might as well do as much as we can in one pass...
				IProject project = sc.sourceFile.getProject();
				File projectClassesDirectory = new File(compiledClassesDirectory.getAbsolutePath() + File.separator + project.getName());
				if(!projectClassesDirectory.exists()){
					projectClassesDirectory.mkdirs();
					File jimpleDirectory = Compilation.getJimpleDirectory(new File(project.getLocation().toOSString()));
					File outputDirectory = projectClassesDirectory;
					boolean allowPhantomReferences = true;
					boolean outputBytecode = true;
					boolean jarify = false;
					Compilation.compile(project, jimpleDirectory, outputDirectory, allowPhantomReferences, new ArrayList<File>(), outputBytecode, jarify);
				}
				Node classNode = Common.toQ(method).parent().nodes(XCSG.Classifier).eval().nodes().one();
				String qualifiedClass = CommonQueries.getQualifiedTypeName(classNode);
				File classFile = new File(projectClassesDirectory.getAbsolutePath() + File.separator + qualifiedClass.replace(".", File.separator) + ".class");
				if(!classFile.exists()){
					throw new FileNotFoundException("Could not find generated class file: " + classFile.getAbsolutePath());
				} else {
					return classFile;
				}
			} else {
				throw new FileNotFoundException("Could not locate corresponding Jimple file for method " + method.getAttr(XCSG.name).toString());
			}
		}
	}
	
	private File getOrCreateExtractedJar(Node method) throws FileNotFoundException {
		if(extractedJarsDirectory == null){
			throw new FileNotFoundException("Could not access temporary Jar extraction directory.");
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
				
				File extractedJarDirectory = new File(extractedJarsDirectory.getAbsolutePath() + File.separator + jarFile.getName());
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
	
	private String decompileMethodFromJar(File extractedJar, Node method){
		Node classNode = Common.toQ(method).parent().nodes(XCSG.Classifier).eval().nodes().one();
		if(classNode == null){
			return method.getAttr(XCSG.name).toString() + " has no containing class.";
		} else {
			String qualifiedClass = CommonQueries.getQualifiedTypeName(classNode);
			File classFile = new File(extractedJar.getAbsolutePath() + File.separator + qualifiedClass.replace(".", File.separator) + ".class");
			if(!classFile.exists()){
				return "Could not find corresponding class file: " + classFile.getAbsolutePath();
			} else {
				return decompileMethodFromClass(classFile, method);
			}
		}
	}
	
	private String decompileMethodFromClass(File classFile, Node method){
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
	
	@Override
	public void indexBecameUnaccessible() {
		statusLabel.setText("Map a Jimple project.");
	}

	@Override
	public void indexBecameAccessible() {
		statusLabel.setText("Empty Selection.");
	}
	
	@Override
	public void dispose(){
		lastSelectedMethodSet.clear();
		try {
			delete(tempDirectory);
		} catch (FileNotFoundException e) {
			// just a best effort...
		}
		super.dispose();
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
}
