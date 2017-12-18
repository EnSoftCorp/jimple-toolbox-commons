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
import com.ensoftcorp.atlas.core.query.Q;
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
	
	private boolean listening = true;
	
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
	
	private String stripCFRHeader(String source){
		// strips the following comment block by knowing its exact length
//		/*
//		 * Decompiled with CFR 0_123.
//		 */
		final int HEADER_SIZE = 37;
		if(source.length() > HEADER_SIZE){
			return source.substring(HEADER_SIZE, source.length());
		} else {
			return source;
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
					String word = textArea.getSelectedText();
					if(word != null){
						word = word.trim();
						if (!word.isEmpty()){
							markOccurrences(word);
						}
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
	
	@Override
	public void selectionChanged(Graph selection) {
		synchronized (CFRDecompilerCorrespondenceView.class){
			if(!listening){
				// we are processing another selection...
				return;
			}
			listening = false;
			processSelection(selection);
			listening = true;
		}
	}

	private void processSelection(Graph selection) {
		Q filteredSelection = Common.toQ(filter(selection));
		Q selectedMethods = filteredSelection.nodes(XCSG.Method);
		Q methodContentSelections = filteredSelection.nodes(XCSG.ControlFlow_Node, XCSG.DataFlow_Node).difference(filteredSelection.nodes(XCSG.Field));
		Q containingMethods = CommonQueries.getContainingMethods(methodContentSelections);
		selectedMethods = selectedMethods.union(containingMethods);
		
		// weed out the initializers since CFR doesn't seem to support decompiling just class initializers
		// instead we will compensate by decompiling the whole class
		Q selectedInitializers = selectedMethods.nodes(XCSG.Constructor).union(selectedMethods.selectNode(XCSG.name, "<init>"), selectedMethods.selectNode(XCSG.name, "<clinit>"));
		selectedMethods = selectedMethods.difference(selectedInitializers);
		
		// selections of fields will resulting decompiling the whole class as well since what good would showing just a method signature do
		// a really fancy analysis could show just methods that read or write to the field, but currently leaving that for another day...
		Q selectedFields = filteredSelection.nodes(XCSG.Field);
		Q selectedClasses = filteredSelection.nodes(XCSG.Classifier).union(selectedFields.parent(), selectedInitializers.parent());
		
		// flush out the Qs to sets of nodes
		AtlasSet<Node> methods = new AtlasHashSet<Node>(selectedMethods.eval().nodes());
		AtlasSet<Node> classes = new AtlasHashSet<Node>(selectedClasses.eval().nodes());
		AtlasSet<Node> variableSelections = new AtlasHashSet<Node>(methodContentSelections.nodes(XCSG.DataFlow_Node).eval().nodes());
		
		if(!classes.isEmpty() && !methods.isEmpty()){
			setText("");
			statusLabel.setText("Mixed selection types not supported.");
			return;
		}
		
		if(classes.isEmpty()){
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
					text.append("\n\n");
					Node classNode = Common.toQ(method).parent().eval().nodes().one();
					if(SEARCH_FOR_JAR){
						try {
							File extractedJar = getOrCreateExtractedJar(classNode);
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
							File compiledClass = getOrCreateCompiledClassFile(classNode);
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
				text.append("\n\n");
				setText(text.toString().trim());
			} else {
				Node method = methods.one();
				statusLabel.setText("Selection: " + CommonQueries.getQualifiedMethodName(method));
				Node classNode = Common.toQ(method).parent().eval().nodes().one();
				try {
					if(SEARCH_FOR_JAR){
						try {
							File extractedJar = getOrCreateExtractedJar(classNode);
							try {
								setText("\n\n" + decompileMethodFromJar(extractedJar, method) + "\n\n");
							} catch (Exception e) {
								setText("\n\nCFR ERROR: " + CommonQueries.getQualifiedMethodName(method) + "\n\n");
							}
						} catch (Exception e) {
							setText("\n\nSEARCH ERROR: " + CommonQueries.getQualifiedMethodName(method) + "\n\n");
						}
					} else {
						try {
							File compiledClass = getOrCreateCompiledClassFile(classNode);
							try {
								setText("\n\n" + decompileMethodFromClass(compiledClass, method) + "\n\n");
							} catch (Exception e) {
								setText("\n\nCFR ERROR: " + CommonQueries.getQualifiedMethodName(method) + "\n\n");
							}
						} catch (Exception e) {
							setText("\n\nSOOT ERROR: " + CommonQueries.getQualifiedMethodName(method) + "\n\n");
						}
					}
				} catch (Exception e) {
					setText("\n\nERROR: " + CommonQueries.getQualifiedMethodName(method) + "\n\n");
				}
			}
		} else {
			if(classes.size() == 1){
				Node classNode = classes.one();
				statusLabel.setText("Selection: " + CommonQueries.getQualifiedTypeName(classNode));
				if(SEARCH_FOR_JAR){
					try {
						File extractedJar = getOrCreateExtractedJar(classNode);
						try {
							setText("\n\n" + decompileClassFromJar(extractedJar, classNode) + "\n\n");
						} catch (Exception e) {
							setText("\n\nCFR ERROR: " + CommonQueries.getQualifiedTypeName(classNode) + "\n\n");
						}
					} catch (Exception e) {
						setText("\n\nSEARCH ERROR: " + CommonQueries.getQualifiedTypeName(classNode) + "\n\n");
					}
				} else {
					try {
						File compiledClass = getOrCreateCompiledClassFile(classNode);
						try {
							setText("\n\n" + decompileClass(compiledClass) + "\n\n");
						} catch (Exception e) {
							setText("\n\nCFR ERROR: " + CommonQueries.getQualifiedTypeName(classNode) + "\n\n");
						}
					} catch (Exception e) {
						setText("\n\nSOOT ERROR: " + CommonQueries.getQualifiedTypeName(classNode) + "\n\n");
					}
				}
			} else if(classes.size() > 2){
				setText("");
				statusLabel.setText("Multiple class selections not supported.");
				return;
			} else {
				setText("");
				statusLabel.setText("Mixed selection types not supported.");
				return;
			}
		}

		markOccurrences(variableSelections);
	}
	
	public void setText(String text){
		textArea.setText(text.trim());
	}

	private void markOccurrences(AtlasSet<Node> variableSelections) {
		boolean occurrencesMarked = false;
		if(variableSelections.size() == 1){
			Node variable = variableSelections.one();
			String word = "";
			if(variable.taggedWith(XCSG.Operator)){
				// operators are a little weird, so skipping
			} if(variable.taggedWith(XCSG.Assignment)) {
				word = variable.getAttr(XCSG.name).toString().trim();
				// remove the "="
				if(word.endsWith("=")){
					word = word.substring(0, word.length() - 1);
					word = word.trim();
				}
			} else {
				word = variable.getAttr(XCSG.name).toString().trim();
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
			   .nodes(XCSG.DataFlow_Node, XCSG.ControlFlow_Node, XCSG.Method, XCSG.Field, XCSG.Classifier)
			   .nodes(XCSG.Language.Jimple).eval().nodes();
	}
	
	private File getOrCreateCompiledClassFile(Node classNode) throws IOException, CoreException, SootConversionException {
		if(compiledClassesDirectory == null){
			throw new FileNotFoundException("Could not access temporary compiled class file directory.");
		} else {
			SourceCorrespondence sc = (SourceCorrespondence) classNode.getAttr(XCSG.sourceCorrespondence);
			if(sc != null){
				// compiles all of the jimple in the corresponding project\nmight as well do as much as we can in one pass\n
				IProject project = sc.sourceFile.getProject();
				File projectClassesDirectory = new File(compiledClassesDirectory.getAbsolutePath() + File.separator + project.getName());
				if(!projectClassesDirectory.exists()){
					projectClassesDirectory.mkdirs();
					File jimpleDirectory = Compilation.getJimpleDirectory(new File(project.getLocation().toOSString()));
					File outputDirectory = projectClassesDirectory;
					boolean allowPhantomReferences = true;
					boolean outputBytecode = true;
					boolean jarify = false;
					String previousText = statusLabel.getText();
					statusLabel.setText("Compiling Jimple in " + project.getName() + "...");
					Compilation.compile(project, jimpleDirectory, outputDirectory, allowPhantomReferences, new ArrayList<File>(), outputBytecode, jarify);
					statusLabel.setText(previousText);
				}
				String qualifiedClass = CommonQueries.getQualifiedTypeName(classNode);
				File classFile = new File(projectClassesDirectory.getAbsolutePath() + File.separator + qualifiedClass.replace(".", File.separator) + ".class");
				if(!classFile.exists()){
					throw new FileNotFoundException("Could not find generated class file: " + classFile.getAbsolutePath());
				} else {
					return classFile;
				}
			} else {
				throw new FileNotFoundException("Could not locate corresponding Jimple file for class " + classNode.getAttr(XCSG.name).toString());
			}
		}
	}
	
	private File getOrCreateExtractedJar(Node classNode) throws FileNotFoundException {
		if(extractedJarsDirectory == null){
			throw new FileNotFoundException("Could not access temporary Jar extraction directory.");
		} else {
			Node container = Common.toQ(classNode).containers().nodes(XCSG.Library, XCSG.Project).eval().nodes().one();
			if(container == null){
				throw new IllegalArgumentException("Class " + classNode.getAttr(XCSG.name).toString() + " is not contained in a library or project.");
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
							// no source correspondence\nsearch indexed projects for jars
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
					// annoying we don't even know the jar name for this case\n
					throw new UnsupportedOperationException("Selections within project containers are currently not supported.");
				}
				
				if(jarFile == null){
					throw new FileNotFoundException("Could not find corresponding Jar file for class " + classNode.getAttr(XCSG.name).toString() + ".");
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
	
	private String decompileClassFromJar(File extractedJar, Node classNode){
		if(!classNode.taggedWith(XCSG.Classifier)){
			throw new IllegalArgumentException("Parameter methodNode must be an XCSG.Classifier.");
		}
		String qualifiedClass = CommonQueries.getQualifiedTypeName(classNode);
		File classFile = new File(extractedJar.getAbsolutePath() + File.separator + qualifiedClass.replace(".", File.separator) + ".class");
		if(!classFile.exists()){
			return "Could not find corresponding class file: " + classFile.getAbsolutePath();
		} else {
			return decompileClass(classFile);
		}
	}
	
	private String decompileMethodFromJar(File extractedJar, Node methodNode){
		if(!methodNode.taggedWith(XCSG.Method)){
			throw new IllegalArgumentException("Parameter methodNode must be an XCSG.Method.");
		}
		if(methodNode.taggedWith(XCSG.Constructor) || methodNode.getAttr(XCSG.name).toString().equals("<init>") || methodNode.getAttr(XCSG.name).toString().equals("<clinit>")){
			throw new IllegalArgumentException("Parameter methodNode must not be a constructor or initializer method.");
		}
		Node classNode = Common.toQ(methodNode).parent().eval().nodes().one();
		String qualifiedClass = CommonQueries.getQualifiedTypeName(classNode);
		File classFile = new File(extractedJar.getAbsolutePath() + File.separator + qualifiedClass.replace(".", File.separator) + ".class");
		if(!classFile.exists()){
			return "Could not find corresponding class file: " + classFile.getAbsolutePath();
		} else {
			return decompileMethodFromClass(classFile, methodNode);
		}
	}
	
	private String decompileClass(File classFile){
		String[] args = new String[]{classFile.getAbsolutePath(), "--silent"};
		return stripCFRHeader(runCFR(args));
	}
	
	private String decompileMethodFromClass(File classFile, Node methodNode){
		if(!methodNode.taggedWith(XCSG.Method)){
			throw new IllegalArgumentException("Parameter methodNode must be an XCSG.Method.");
		}
		if(methodNode.taggedWith(XCSG.Constructor) || methodNode.getAttr(XCSG.name).toString().equals("<init>") || methodNode.getAttr(XCSG.name).toString().equals("<clinit>")){
			throw new IllegalArgumentException("Parameter methodNode must not be a constructor or initializer method.");
		}
		String methodName = methodNode.getAttr(XCSG.name).toString();
		String[] args = new String[]{classFile.getAbsolutePath(), methodName, "--silent"};
		return runCFR(args);
	}

	private String runCFR(String[] args) {
		// temporarily redirect System.out to byte array
	    ByteArrayOutputStream stdoutbaos = new ByteArrayOutputStream();
	    PrintStream alternatePrintStream = new PrintStream(stdoutbaos);
	    ByteArrayOutputStream stderrbaos = new ByteArrayOutputStream();
	    PrintStream alternatePrintErrorStream = new PrintStream(stderrbaos);
	    PrintStream originalPrintStream = System.out;
	    PrintStream originalPrintErrorStream = System.err;
	    try {
	    	System.setOut(alternatePrintStream);
	    	System.setErr(alternatePrintErrorStream);
	    	org.benf.cfr.reader.Main.main(args);
	    	System.out.flush();
	    	System.err.flush();
	    } finally {
	    	System.setOut(originalPrintStream);
	    	System.setErr(originalPrintErrorStream);
	    }
	    String result = stdoutbaos.toString().trim();
	    String error = stderrbaos.toString().trim();
	    if(!error.isEmpty() && result.isEmpty()){
	    	throw new RuntimeException("CFR Error\n" + error);
	    } else {
	    	return result;
	    }
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
