package com.ensoftcorp.open.jimple.commons.ui.views.decompiler;

import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
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
import java.util.HashSet;
import java.util.Set;

import javax.swing.ScrollPaneConstants;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wb.swt.ResourceManager;
import org.fife.rsyntaxtextarea.themes.Themes;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
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
import com.ensoftcorp.open.commons.utilities.DisplayUtils;
import com.ensoftcorp.open.commons.utilities.NodeSourceCorrespondenceSorter;
import com.ensoftcorp.open.commons.utilities.WorkspaceUtils;
import com.ensoftcorp.open.commons.utilities.selection.GraphSelectionListenerView;
import com.ensoftcorp.open.java.commons.analysis.CommonQueries;
import com.ensoftcorp.open.java.commons.bytecode.JarInspector;
import com.ensoftcorp.open.java.commons.project.ProjectJarProperties;
import com.ensoftcorp.open.java.commons.project.ProjectJarProperties.Jar;
import com.ensoftcorp.open.jimple.commons.preferences.JimpleCommonsPreferences;
import com.ensoftcorp.open.jimple.commons.soot.Compilation;
import com.ensoftcorp.open.jimple.commons.soot.SootConversionException;

public class CFRDecompilerCorrespondenceView extends GraphSelectionListenerView {
	
	private static final String ID = "com.ensoftcorp.open.jimple.commons.ui.views.decompiler";
	
	private File tempDirectory = null;
	private File extractedJarsDirectory = null;
	private File compiledClassesDirectory = null;
	private Label statusLabel;
	private RSyntaxTextArea textArea;
	private boolean processing = true;
	
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
		
		// add an increase font size button
		final Action increaseFontSizeAction = new Action() {
			public void run() {
				increaseFontSize();
			}
		};
		increaseFontSizeAction.setText("Increase Font");
		increaseFontSizeAction.setToolTipText("Increase Font");
		ImageDescriptor increaseFontIcon = ImageDescriptor.createFromImage(ResourceManager.getPluginImage("com.ensoftcorp.open.jimple.commons", "icons/increase.gif"));
		increaseFontSizeAction.setImageDescriptor(increaseFontIcon);
		increaseFontSizeAction.setDisabledImageDescriptor(increaseFontIcon);
		increaseFontSizeAction.setHoverImageDescriptor(increaseFontIcon);
		getViewSite().getActionBars().getToolBarManager().add(increaseFontSizeAction);
		
		// add a decrease font size button
		final Action decreaseFontSizeAction = new Action() {
			public void run() {
				decreaseFontSize();
			}
		};
		decreaseFontSizeAction.setText("Decrease Font");
		decreaseFontSizeAction.setToolTipText("Decrease Font");
		ImageDescriptor decreaseFontIcon = ImageDescriptor.createFromImage(ResourceManager.getPluginImage("com.ensoftcorp.open.jimple.commons", "icons/decrease.gif"));
		decreaseFontSizeAction.setImageDescriptor(decreaseFontIcon);
		decreaseFontSizeAction.setDisabledImageDescriptor(decreaseFontIcon);
		decreaseFontSizeAction.setHoverImageDescriptor(decreaseFontIcon);
		getViewSite().getActionBars().getToolBarManager().add(decreaseFontSizeAction);
		
		// add a copy text to clipboard button
		final Action copyTextToClipboardAction = new Action() {
			public void run() {
				copyTextToClipboard();
			}
		};
		copyTextToClipboardAction.setText("Copy to Clipboard");
		copyTextToClipboardAction.setToolTipText("Copy to Clipboard");
		ImageDescriptor copyTextToClipboardIcon = ImageDescriptor.createFromImage(ResourceManager.getPluginImage("com.ensoftcorp.open.jimple.commons", "icons/copy.gif"));
		copyTextToClipboardAction.setImageDescriptor(copyTextToClipboardIcon);
		copyTextToClipboardAction.setDisabledImageDescriptor(copyTextToClipboardIcon);
		copyTextToClipboardAction.setHoverImageDescriptor(copyTextToClipboardIcon);
		getViewSite().getActionBars().getToolBarManager().add(copyTextToClipboardAction);
		
		// add a highlight text button
		final Action highlightTextAction = new Action() {
			public void run() {
				String text = DisplayUtils.promptString("Text Highlighter", "Enter Text to Highlight");
				if(text != null){
					markOccurrences(text);
				}
			}
		};
		highlightTextAction.setText("Highlight Text");
		highlightTextAction.setToolTipText("Highlight Text");
		ImageDescriptor highlightTextIcon = ImageDescriptor.createFromImage(ResourceManager.getPluginImage("com.ensoftcorp.open.jimple.commons", "icons/search.gif"));
		highlightTextAction.setImageDescriptor(highlightTextIcon);
		highlightTextAction.setDisabledImageDescriptor(highlightTextIcon);
		highlightTextAction.setHoverImageDescriptor(highlightTextIcon);
		getViewSite().getActionBars().getToolBarManager().add(highlightTextAction);
		
		// add a toggle selection listener button
		// icon from http://eclipse-icons.i24.cc
		final ImageDescriptor activeSelectionListenerIcon = ImageDescriptor.createFromImage(ResourceManager.getPluginImage("com.ensoftcorp.open.jimple.commons", "icons/play.gif"));
		final ImageDescriptor pausedSelectionListenerIcon = ImageDescriptor.createFromImage(ResourceManager.getPluginImage("com.ensoftcorp.open.jimple.commons", "icons/pause.gif"));
		final Action toggleSelectionListenerAction = new Action() {
			public void run() {
				toggleGraphSelectionListener();
				if(isGraphSelectionListenerEnabled()){
					this.setImageDescriptor(pausedSelectionListenerIcon);
					this.setDisabledImageDescriptor(pausedSelectionListenerIcon);
					this.setHoverImageDescriptor(pausedSelectionListenerIcon);
				} else {
					this.setImageDescriptor(activeSelectionListenerIcon);
					this.setDisabledImageDescriptor(activeSelectionListenerIcon);
					this.setHoverImageDescriptor(activeSelectionListenerIcon);
				}
			}
		};
		toggleSelectionListenerAction.setText("Toggle Selection Listener");
		toggleSelectionListenerAction.setToolTipText("Toggle Selection Listener");
		toggleSelectionListenerAction.setImageDescriptor(pausedSelectionListenerIcon);
		toggleSelectionListenerAction.setDisabledImageDescriptor(pausedSelectionListenerIcon);
		toggleSelectionListenerAction.setHoverImageDescriptor(pausedSelectionListenerIcon);
		getViewSite().getActionBars().getToolBarManager().add(toggleSelectionListenerAction);
		
		// pause and open new window button
		// icon from http://eclipse-icons.i24.cc
		final Action pauseAndOpenNewWindowAction = new Action() {
			public void run() {
				disableGraphSelectionListener();
				toggleSelectionListenerAction.setImageDescriptor(activeSelectionListenerIcon);
				toggleSelectionListenerAction.setDisabledImageDescriptor(activeSelectionListenerIcon);
				toggleSelectionListenerAction.setHoverImageDescriptor(activeSelectionListenerIcon);
				openNewWindow();
			}
		};
		pauseAndOpenNewWindowAction.setText("Pause and Open New Window");
		pauseAndOpenNewWindowAction.setToolTipText("Pause and Open New Window");
		ImageDescriptor newWindowIcon = ImageDescriptor.createFromImage(ResourceManager.getPluginImage("com.ensoftcorp.open.jimple.commons", "icons/new.png"));
		pauseAndOpenNewWindowAction.setImageDescriptor(newWindowIcon);
		pauseAndOpenNewWindowAction.setDisabledImageDescriptor(newWindowIcon);
		pauseAndOpenNewWindowAction.setHoverImageDescriptor(newWindowIcon);
		getViewSite().getActionBars().getToolBarManager().add(pauseAndOpenNewWindowAction);
		
		registerGraphHandlers();
	}
	
	private static int viewInstance = 1;
	private void openNewWindow(){
		try {
			int instance = viewInstance++;
			String secondaryID = (ID + "." + instance);
			CFRDecompilerCorrespondenceView view = (CFRDecompilerCorrespondenceView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(ID, secondaryID, IWorkbenchPage.VIEW_CREATE | IWorkbenchPage.VIEW_VISIBLE | IWorkbenchPage.VIEW_CREATE);
			view.setFocus();
		} catch (PartInitException e) {
			Log.error("Could not open CFR Decompiler Correspondence view", e);
		}
	}
	
	private void increaseFontSize(){
		Font font = textArea.getFont();
		int fontSize = font.getSize();
		Font newFont = font.deriveFont((float)(fontSize+1));
		setFont(textArea, newFont);
		textArea.revalidate();
	}
	
	private static final int MIN_SIZE = 8;
	private void decreaseFontSize(){
		Font font = textArea.getFont();
		int fontSize = font.getSize();
		if(fontSize != MIN_SIZE){
			Font newFont = font.deriveFont((float)(fontSize-1));
			setFont(textArea, newFont);
			textArea.revalidate();
		}
	}
	
	private void copyTextToClipboard(){
	    StringSelection selection = new StringSelection(textArea.getText());
	    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    clipboard.setContents(selection, selection);
	}
	
	/**
	 * Set the font for all token types.
	 * 
	 * @param textArea
	 *            The text area to modify.
	 * @param font
	 *            The font to use.
	 */
	private void setFont(RSyntaxTextArea textArea, Font font) {
		if (font != null) {
			SyntaxScheme ss = textArea.getSyntaxScheme();
			ss = (SyntaxScheme) ss.clone();
			for (int i = 0; i < ss.getStyleCount(); i++) {
				if (ss.getStyle(i) != null) {
					ss.getStyle(i).font = font;
				}
			}
			textArea.setSyntaxScheme(ss);
			textArea.setFont(font);
		}
	}

	@Override
	public void setFocus() {}
	
	@Override
	public void selectionChanged(Graph selection) {
		synchronized (CFRDecompilerCorrespondenceView.class){
			if(!processing){
				// we are processing another selection...
				return;
			}
			processing = false;
			processSelection(selection);
			processing = true;
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
					if(JimpleCommonsPreferences.isCFRCorrespondenceUseOriginalJarsEnabled()){
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
					if(JimpleCommonsPreferences.isCFRCorrespondenceUseOriginalJarsEnabled()){
						try {
							File extractedJar = getOrCreateExtractedJar(classNode);
							try {
								setText("\n\n" + decompileMethodFromJar(extractedJar, method) + "\n\n");
							} catch (Exception e) {
								setText("\n\nCFR ERROR: " + CommonQueries.getQualifiedMethodName(method) + ":" + e.getMessage() + "\n\n");
							}
						} catch (Exception e) {
							setText("\n\nSEARCH ERROR: " + CommonQueries.getQualifiedMethodName(method) + ":" + e.getMessage() + "\n\n");
						}
					} else {
						try {
							File compiledClass = getOrCreateCompiledClassFile(classNode);
							try {
								setText("\n\n" + decompileMethodFromClass(compiledClass, method) + "\n\n");
							} catch (Exception e) {
								setText("\n\nCFR ERROR: " + CommonQueries.getQualifiedMethodName(method) + ":" + e.getMessage() + "\n\n");
							}
						} catch (Exception e) {
							setText("\n\nSOOT ERROR: " + CommonQueries.getQualifiedMethodName(method) + ":" + e.getMessage() + "\n\n");
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
				if(JimpleCommonsPreferences.isCFRCorrespondenceUseOriginalJarsEnabled()){
					try {
						File extractedJar = getOrCreateExtractedJar(classNode);
						try {
							setText("\n\n" + decompileClassFromJar(extractedJar, classNode) + "\n\n");
						} catch (Exception e) {
							setText("\n\nCFR ERROR: " + CommonQueries.getQualifiedTypeName(classNode) + ":" + e.getMessage() + "\n\n");
						}
					} catch (Exception e) {
						setText("\n\nSEARCH ERROR: " + CommonQueries.getQualifiedTypeName(classNode) + ":" + e.getMessage() + "\n\n");
					}
				} else {
					try {
						File compiledClass = getOrCreateCompiledClassFile(classNode);
						try {
							setText("\n\n" + decompileClass(compiledClass) + "\n\n");
						} catch (Exception e) {
							setText("\n\nCFR ERROR: " + CommonQueries.getQualifiedTypeName(classNode) + ":" + e.getMessage() + "\n\n");
						}
					} catch (Exception e) {
						setText("\n\nSOOT ERROR: " + CommonQueries.getQualifiedTypeName(classNode) + ":" + e.getMessage() + "\n\n");
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
					boolean useOriginalNames = true;
					boolean outputBytecode = true;
					boolean jarify = false;
					String previousText = statusLabel.getText();
					statusLabel.setText("Compiling Jimple in " + project.getName() + "...");
					Compilation.compile(project, jimpleDirectory, outputDirectory, allowPhantomReferences, useOriginalNames, new ArrayList<File>(), outputBytecode, jarify);
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
			Set<IProject> indexedProjects = new HashSet<IProject>();
			for(Node projectNode : Common.universe().nodes(XCSG.Project).eval().nodes()){
				indexedProjects.add(WorkspaceUtils.getProject(projectNode.getAttr(XCSG.name).toString()));
			}
			Set<Jar> indexedProjectJars = new HashSet<Jar>();
			for(IProject project : indexedProjects){
				try {
					indexedProjectJars.addAll(ProjectJarProperties.getJars(project));
				} catch (Exception e) {
					Log.warning("Failed to parse project jar properties for project " + project.getName() + ".", e);
				}
			}
			
			// assuming class nodes are always in jars for jimple
			Node container = Common.toQ(classNode).containers().nodes(XCSG.Library).eval().nodes().one();
			if(container == null){
				throw new IllegalArgumentException("Class " + classNode.getAttr(XCSG.name).toString() + " is not contained in a library or project.");
			} else {
				File jarFile = null;
				if(container.taggedWith(XCSG.Library)){
					String jarName = new File(container.getAttr(XCSG.name).toString()).getName();
					Jar match = null;
					for(Jar jar : indexedProjectJars){
						if(jar.getFile().getName().equals(jarName)){
							if(match == null){
								jarFile = jar.getFile();
							} else {
								Log.warning("Found multiple matches for " + jarName);
							}
						}
					}
				} else {
					// TODO: how to find corresponding jar for a project selection?
					// annoying we don't even know the jar name for this case\n
					throw new UnsupportedOperationException("Jimple selections within project containers are currently not supported.");
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
							if(path.getName().endsWith(".class")){
								path.getParentFile().mkdirs();
								FileOutputStream writer = new FileOutputStream(path);
								writer.write(bytes);
								writer.close();
							}
						}
						return extractedJarDirectory;
					} catch (Exception e){
						throw new RuntimeException("Failed to extract library Jar contents for " + jarFile.getAbsolutePath());
					}
				}
			}
		}
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
