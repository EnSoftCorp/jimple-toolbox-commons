package com.ensoftcorp.open.jimple.commons.annotations;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.log.Log;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.codemap.PrioritizedCodemapStage;
import com.ensoftcorp.open.commons.codemap.ProjectAnalysisDefaultPropertiesInitializer;
import com.ensoftcorp.open.commons.utilities.WorkspaceUtils;
import com.ensoftcorp.open.java.commons.bytecode.BytecodeUtils;
import com.ensoftcorp.open.java.commons.bytecode.JarInspector;
import com.ensoftcorp.open.java.commons.project.ProjectJarProperties;
import com.ensoftcorp.open.java.commons.project.ProjectJarProperties.Jar;
import com.ensoftcorp.open.java.commons.wishful.JavaStopGap;
import com.ensoftcorp.open.jimple.commons.preferences.JimpleCommonsPreferences;

public class JimpleAnnotationIndexer extends PrioritizedCodemapStage {

	public static final String IDENTIFIER = "com.ensoftcorp.open.jimple.commons.annotations";
	
	@Override
	public String getDisplayName() {
		return "Jimple Annotation Indexer";
	}

	@Override
	public String getIdentifier() {
		return IDENTIFIER;
	}

	@Override
	public String[] getCodemapStageDependencies() {
		return new String[]{ ProjectAnalysisDefaultPropertiesInitializer.IDENTIFIER };
	}

	@Override
	public void performIndexing(IProgressMonitor monitor) {
		for(Node projectNode : Common.universe().nodes(XCSG.Project).eval().nodes()){
			IProject project = WorkspaceUtils.getProject(projectNode.getAttr(XCSG.name).toString());
			if(project.exists() && project.isOpen() && project.isAccessible()){
				try {
					if(JimpleCommonsPreferences.isApplicationJarAnnotationIndexerEnabled()){
						Log.info("Indexing application jar annotations");
						for(Jar jar : ProjectJarProperties.getApplicationJars(project)){
							try {
								index(jar.getFile());
							} catch (Exception e){
								Log.error("Error indexing " + project.getName() + ":" + jar.getFile().getName() + " annotations.", e);
							}
						}
					}
					if(JimpleCommonsPreferences.isLibraryJarAnnotationIndexerEnabled()){
						Log.info("Indexing library jar annotations");
						for(Jar jar : ProjectJarProperties.getLibraryJars(project)){
							try {
								index(jar.getFile());
							} catch (Exception e){
								Log.error("Error indexing " + project.getName() + ":" + jar.getFile().getName() + " annotations.", e);
							}
						}
					}
					if(JimpleCommonsPreferences.isRuntimeJarAnnotationIndexerEnabled()){
						Log.info("Indexing runtime jar annotations");
						for(Jar jar : ProjectJarProperties.getRuntimeJars(project)){
							try {
								index(jar.getFile());
							} catch (Exception e){
								Log.error("Error indexing " + project.getName() + ":" + jar.getFile().getName() + " annotations.", e);
							}
						}
					}
				} catch (Exception e) {
					Log.error("Error indexing " + project.getName() + " annotations.", e);
				}
			}
		}
	}
	
	public static void index(File jar) throws JarException, IOException {
		for(Node library : Common.universe().nodes(XCSG.Library).eval().nodes()){
			JarInspector inspector = new JarInspector(jar);
			for(String entry : inspector.getJarEntrySet()){
				if(entry.endsWith(".class")){
					byte[] bytes = inspector.extractEntry(entry);
					ClassNode classNode = null;
					try {
						classNode = BytecodeUtils.getClassNode(bytes);
					} catch (Exception e){
						Log.warning("Error reading class file: " + entry);
					}
					if(classNode == null){
						continue;
					}
					
					// class annotation
					if (classNode.invisibleAnnotations != null) {
						for (Object annotationObject : classNode.invisibleAnnotations) {
							AnnotationNode annotation = (AnnotationNode) annotationObject;
							index(library, classNode, annotation);
						}
					}
					if (classNode.visibleAnnotations != null) {
						for (Object annotationObject : classNode.visibleAnnotations) {
							AnnotationNode annotation = (AnnotationNode) annotationObject;
							index(library, classNode, annotation);
						}
					}
					if (classNode.invisibleTypeAnnotations != null) {
						for (Object annotationObject : classNode.invisibleTypeAnnotations) {
							TypeAnnotationNode annotation = (TypeAnnotationNode) annotationObject;
							index(library, classNode, annotation);
						}
					}
					if (classNode.visibleTypeAnnotations != null) {
						for (Object annotationObject : classNode.visibleTypeAnnotations) {
							TypeAnnotationNode annotation = (TypeAnnotationNode) annotationObject;
							index(library, classNode, annotation);
						}
					}
					
					// method annotations
					for (Object o : classNode.methods) {
						MethodNode methodNode = (MethodNode) o;
						if (methodNode.invisibleAnnotations != null) {
							for (Object annotationObject : methodNode.invisibleAnnotations) {
								AnnotationNode annotation = (AnnotationNode) annotationObject;
								index(library, classNode, methodNode, annotation);
							}
						}
			    	}
					for (Object o : classNode.methods) {
						MethodNode methodNode = (MethodNode) o;
						if (methodNode.visibleAnnotations != null) {
							for (Object annotationObject : methodNode.visibleAnnotations) {
								AnnotationNode annotation = (AnnotationNode) annotationObject;
								index(library, classNode, methodNode, annotation);
							}
						}
			    	}
					
					// field annotations
					for (Object o : classNode.fields) {
						FieldNode fieldNode = (FieldNode) o;
						if (fieldNode.invisibleAnnotations != null) {
							for (Object annotationObject : fieldNode.invisibleAnnotations) {
								AnnotationNode annotation = (AnnotationNode) annotationObject;
								index(library, classNode, fieldNode, annotation);
							}
						}
			    	}
					for (Object o : classNode.fields) {
						FieldNode fieldNode = (FieldNode) o;
						if (fieldNode.visibleAnnotations != null) {
							for (Object annotationObject : fieldNode.visibleAnnotations) {
								AnnotationNode annotation = (AnnotationNode) annotationObject;
								index(library, classNode, fieldNode, annotation);
							}
						}
			    	}
				}
			}
		}
	}
	
	private static void index(Node library, ClassNode clazz, AnnotationNode annotation){
		String qualifiedClassName = clazz.name.replace("/", ".");
		int index = qualifiedClassName.lastIndexOf(".");
		String className = qualifiedClassName;
		String pkgName = ""; // default package
		if(index != -1){
			pkgName = qualifiedClassName.substring(0, index);
			className = qualifiedClassName.substring(index+1, qualifiedClassName.length());
		}
		for(Node packageNode : Common.toQ(library).contained().nodes(XCSG.Package).selectNode(XCSG.name, pkgName).eval().nodes()){
			for(Node classNode : Common.toQ(packageNode).contained().nodes(XCSG.Classifier).selectNode(XCSG.name, className).eval().nodes()){
				Node annotationNode = getOrCreateAnnotationNode(library, annotation);
				
				Edge annotatedWithEdge = Graph.U.createEdge(classNode, annotationNode);
				annotatedWithEdge.tag(XCSG.Java.AnnotatedWith);
				
				String rawAnnotationText = annotationNode.getAttr(XCSG.name).toString() + "(";
				String prefix = "";
				if(annotation.values != null){
					for(Object object : annotation.values){
						String value = object.toString();
						if(value.getClass().equals(String.class)){
							value = "\"" + value.replace("\'", "\\'")
										 		.replace("\"", "\\\"")
										 		.replace("\\", "\\\\")
										 		.replace("\t", "\\t")
										 		.replace("\b", "\\b")
										 		.replace("\r", "\\r")
										 		.replace("\f", "\\f")
										 		.replace("\n", "\\n") + "\"";
						}
						rawAnnotationText += (prefix + value);
						prefix = ",";
					}
				}
				rawAnnotationText += ")";
				classNode.putAttr(JavaStopGap.ANNOTATION_RAW_TEXT, rawAnnotationText);
			}
		}
	}
	
	private static void index(Node library, ClassNode clazz, MethodNode methodNode, AnnotationNode annotation){
//		System.out.println(classNode.name + ":" + methodNode.name);
		if(annotation.values != null){
			for(Object object : annotation.values){
//				System.out.println(object.getClass().getName() + ":" + object.toString());
			}
		}
	}
	
	private static void index(Node library, ClassNode clazz, FieldNode field, AnnotationNode annotation){
		String qualifiedClassName = clazz.name.replace("/", ".");
		int index = qualifiedClassName.lastIndexOf(".");
		String className = qualifiedClassName;
		String pkgName = ""; // default package
		if(index != -1){
			pkgName = qualifiedClassName.substring(0, index);
			className = qualifiedClassName.substring(index+1, qualifiedClassName.length());
		}
		for(Node packageNode : Common.toQ(library).contained().nodes(XCSG.Package).selectNode(XCSG.name, pkgName).eval().nodes()){
			for(Node classNode : Common.toQ(packageNode).contained().nodes(XCSG.Classifier).selectNode(XCSG.name, className).eval().nodes()){
				for(Node fieldNode : Common.toQ(classNode).contained().nodes(XCSG.Field).selectNode(XCSG.name, field.name).eval().nodes()){
					Node annotationNode = getOrCreateAnnotationNode(library, annotation);
					
					Edge annotatedWithEdge = Graph.U.createEdge(fieldNode, annotationNode);
					annotatedWithEdge.tag(XCSG.Java.AnnotatedWith);
					
					String rawAnnotationText = annotationNode.getAttr(XCSG.name).toString() + "(";
					String prefix = "";
					if(annotation.values != null){
						for(Object object : annotation.values){
							String value = object.toString();
							if(value.getClass().equals(String.class)){
								value = "\"" + value.replace("\'", "\\'")
											 		.replace("\"", "\\\"")
											 		.replace("\\", "\\\\")
											 		.replace("\t", "\\t")
											 		.replace("\b", "\\b")
											 		.replace("\r", "\\r")
											 		.replace("\f", "\\f")
											 		.replace("\n", "\\n") + "\"";
							}
							rawAnnotationText += (prefix + value);
							prefix = ",";
						}
					}
					rawAnnotationText += ")";
					classNode.putAttr(JavaStopGap.ANNOTATION_RAW_TEXT, rawAnnotationText);
				}
			}
		}
	}
	
	private static Node getOrCreateAnnotationNode(Node library, AnnotationNode annotation){
		// TODO: improve parsing of class descriptors
		String annotationDescription = annotation.desc;
		if(annotationDescription.startsWith("L")){
			annotationDescription = annotationDescription.substring(1);
		}
		if(annotationDescription.endsWith(";")){
			annotationDescription = annotationDescription.substring(0,annotationDescription.length()-1);
		}
	
		String qualifiedAnnotationClassName = annotationDescription.replace("/", ".");
		int index = qualifiedAnnotationClassName.lastIndexOf(".");
		String annotationClassName = qualifiedAnnotationClassName;
		String annotationPkgName = ""; // default package
		if(index != -1){
			annotationPkgName = qualifiedAnnotationClassName.substring(0, index);
			annotationClassName = qualifiedAnnotationClassName.substring(index+1, qualifiedAnnotationClassName.length());
		}
		
		Q annotationPackage = Common.universe().nodes(XCSG.Package).selectNode(XCSG.name, annotationPkgName);
		Node annotationNode = annotationPackage.contained().nodes(XCSG.Java.Annotation).selectNode(XCSG.name, annotationClassName).eval().nodes().one();
		
		if(annotationNode == null){
			annotationNode = Graph.U.createNode();
			annotationNode.tag(XCSG.Java.Annotation);
			annotationNode.putAttr(XCSG.name, annotationClassName);
		}
		
		return annotationNode;
	}

}
