package com.ensoftcorp.open.jimple.commons.annotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.jar.JarException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
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
		if(Common.universe().nodes(XCSG.Language.Jimple).eval().nodes().isEmpty()){
			return; // there is no jimple in the universe to annotate
		}
		for(Node projectNode : Common.universe().nodes(XCSG.Project).nodes(XCSG.Language.Java).eval().nodes()){
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
					
					// TODO: what are TypeAnnotations vs Annotations?
//					if (classNode.invisibleTypeAnnotations != null) {
//						for (Object annotationObject : classNode.invisibleTypeAnnotations) {
//							TypeAnnotationNode annotation = (TypeAnnotationNode) annotationObject;
//							index(library, classNode, annotation);
//						}
//					}
//					if (classNode.visibleTypeAnnotations != null) {
//						for (Object annotationObject : classNode.visibleTypeAnnotations) {
//							TypeAnnotationNode annotation = (TypeAnnotationNode) annotationObject;
//							index(library, classNode, annotation);
//						}
//					}
					
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
					
					// TODO: what are TypeAnnotations vs Annotations?
//					for (Object o : classNode.methods) {
//						MethodNode methodNode = (MethodNode) o;
//						if (methodNode.invisibleTypeAnnotations != null) {
//							for (Object annotationObject : methodNode.invisibleTypeAnnotations) {
//								TypeAnnotationNode annotation = (TypeAnnotationNode) annotationObject;
//								index(library, classNode, methodNode, annotation);
//							}
//						}
//			    	}
//					for (Object o : classNode.methods) {
//						MethodNode methodNode = (MethodNode) o;
//						if (methodNode.visibleTypeAnnotations != null) {
//							for (Object annotationObject : methodNode.visibleTypeAnnotations) {
//								TypeAnnotationNode annotation = (TypeAnnotationNode) annotationObject;
//								index(library, classNode, methodNode, annotation);
//							}
//						}
//			    	}
					
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
					
					// TODO: what are TypeAnnotations vs Annotations?
//					for (Object o : classNode.fields) {
//						FieldNode fieldNode = (FieldNode) o;
//						if (fieldNode.invisibleTypeAnnotations != null) {
//							for (Object annotationObject : fieldNode.invisibleTypeAnnotations) {
//								TypeAnnotationNode annotation = (TypeAnnotationNode) annotationObject;
//								index(library, classNode, fieldNode, annotation);
//							}
//						}
//			    	}
//					for (Object o : classNode.fields) {
//						FieldNode fieldNode = (FieldNode) o;
//						if (fieldNode.visibleTypeAnnotations != null) {
//							for (Object annotationObject : fieldNode.visibleTypeAnnotations) {
//								TypeAnnotationNode annotation = (TypeAnnotationNode) annotationObject;
//								index(library, classNode, fieldNode, annotation);
//							}
//						}
//			    	}
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
				
				String rawAnnotationText = getRawAnnotation(annotation, annotationNode);
				classNode.putAttr(JavaStopGap.ANNOTATION_RAW_TEXT, rawAnnotationText);
			}
		}
	}
	
	// TODO: consider parameter annotations
	// skipping local variable annotations, we probably won't be able to match those up
	private static void index(Node library, ClassNode clazz, MethodNode method, AnnotationNode annotation){
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
				AtlasSet<Node> methodNodes = new AtlasHashSet<Node>(Common.toQ(classNode).contained().nodes(XCSG.Method).selectNode(XCSG.name, method.name).eval().nodes());
				Node methodNode = null;
				if(methodNodes.size() == 1){
					methodNode = methodNodes.one();
				} else {
					Q paramEdges = Common.universe().edgesTaggedWithAny(XCSG.HasParameter);
					if(method.desc.equals("()V")){
						// void method
						// save only the methods without any parameters
						methodNodes = Common.toQ(methodNodes).difference(paramEdges.forward(Common.toQ(methodNodes)).retainEdges().retainNodes()).eval().nodes();
						if(methodNodes.size() == 1){
							methodNode = methodNodes.one();
						}
					} else {
						// strip method descriptor prefix and suffix
						String descriptor = method.desc;
						String[] parameters = descriptor.substring(descriptor.indexOf("(")+1, descriptor.indexOf(")")).split(";");
						try {
							Q typeOfEdges = Common.universe().edges(XCSG.TypeOf);
							for(int i=0; i<parameters.length; i++){
								String parameter = parameters[i];
								if(!parameter.equals("")){
									Node parameterType = getParameterTypeNode(parameter);
									methodNodes = Common.toQ(methodNodes).intersection(typeOfEdges.predecessors(Common.toQ(parameterType)).selectNode(XCSG.parameterIndex, i).parent()).eval().nodes();
									if(methodNodes.size() == 1){
										// found our match
										methodNode = methodNodes.one();
										break;
									}
								}
							}
						} catch (Exception e){
							Log.warning("Error parsing method parameter descriptors: " + method.desc);
							return;
						}
					}
				}
				
				if(methodNode != null){
					Node annotationNode = getOrCreateAnnotationNode(library, annotation);
					
					Edge annotatedWithEdge = Graph.U.createEdge(methodNode, annotationNode);
					annotatedWithEdge.tag(XCSG.Java.AnnotatedWith);
					
					String rawAnnotationText = getRawAnnotation(annotation, annotationNode);
					methodNode.putAttr(JavaStopGap.ANNOTATION_RAW_TEXT, rawAnnotationText);
				} else {
					Log.warning("Could not find matching method for " + method.name + method.desc);
					return;
				}
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
					
					String rawAnnotationText = getRawAnnotation(annotation, annotationNode);
					fieldNode.putAttr(JavaStopGap.ANNOTATION_RAW_TEXT, rawAnnotationText);
				}
			}
		}
	}
	
	private static Node getParameterTypeNode(String descriptor){
		int arrayDimension = 0;
		while(descriptor.startsWith("[")){
			descriptor = descriptor.substring(1);
			arrayDimension++;
		}
		
		Node typeNode = null;
		if(descriptor.equals("I") || descriptor.equals("int")){
			typeNode = Common.universe().nodes(XCSG.Primitive).selectNode(XCSG.name, "int").eval().nodes().one();
		} else if(descriptor.equals("J") || descriptor.equals("long")){
			typeNode = Common.universe().nodes(XCSG.Primitive).selectNode(XCSG.name, "long").eval().nodes().one();
		} else if(descriptor.equals("S") || descriptor.equals("short")){
			typeNode = Common.universe().nodes(XCSG.Primitive).selectNode(XCSG.name, "short").eval().nodes().one();
		} else if(descriptor.equals("F") || descriptor.equals("float")){
			typeNode = Common.universe().nodes(XCSG.Primitive).selectNode(XCSG.name, "float").eval().nodes().one();
		} else if(descriptor.equals("D") || descriptor.equals("double")){
			typeNode = Common.universe().nodes(XCSG.Primitive).selectNode(XCSG.name, "double").eval().nodes().one();
		} else if(descriptor.equals("C") || descriptor.equals("char")){
			typeNode = Common.universe().nodes(XCSG.Primitive).selectNode(XCSG.name, "char").eval().nodes().one();
		} else if(descriptor.equals("B") || descriptor.equals("byte")){
			typeNode = Common.universe().nodes(XCSG.Primitive).selectNode(XCSG.name, "byte").eval().nodes().one();
		} else if(descriptor.equals("Z") || descriptor.equals("boolean")){
			typeNode = Common.universe().nodes(XCSG.Primitive).selectNode(XCSG.name, "boolean").eval().nodes().one();
		} else if(descriptor.startsWith("L")){
			// any non-primitive Object
			descriptor = descriptor.substring(1);
			descriptor = descriptor.replace("/", ".").trim();
			String qualifiedClassName = descriptor;
			int index = qualifiedClassName.lastIndexOf(".");
			String className = qualifiedClassName;
			String pkgName = ""; // default package
			if(index != -1){
				pkgName = qualifiedClassName.substring(0, index);
				className = qualifiedClassName.substring(index+1, qualifiedClassName.length());
			}
			Q pkgs = Common.universe().nodes(XCSG.Package).selectNode(XCSG.name, pkgName);
			AtlasSet<Node> classNodes = pkgs.contained().nodes(XCSG.Classifier).selectNode(XCSG.name, className).eval().nodes();
			if(classNodes.isEmpty()){
				Log.warning("Could not find class: " + qualifiedClassName);
				return null;
			} else if(classNodes.size() > 1){
				Log.warning("Found multiple class matches for " + qualifiedClassName);
				return null;
			} else {
				typeNode = classNodes.one();
			}
		}
		
		if(arrayDimension > 0){
			Q arrayElementTypeEdges = Common.universe().edges(XCSG.ArrayElementType);
			Q arrayTypes = arrayElementTypeEdges.predecessors(Common.toQ(typeNode));
			AtlasSet<Node> arrayDimensionTypes = arrayTypes.selectNode(XCSG.Java.arrayTypeDimension, arrayDimension).eval().nodes();
			if(arrayDimensionTypes.size() != 1){
				Log.warning("Could not find a matching array dimension for type [" + typeNode.address().toAddressString() + "]");
				return null;
			} else {
				return arrayDimensionTypes.one();
			}
		}
		
		return typeNode;
	}
	
	private static String parseJVMDescriptor(String descriptor){
		String suffix = "";
		while(descriptor.startsWith("[")){
			descriptor = descriptor.substring(1);
			suffix+="[]";
		}
		if(descriptor.equals("I") || descriptor.equals("int")){
			descriptor = "int";
		} else if(descriptor.equals("J") || descriptor.equals("long")){
			descriptor = "long";
		} else if(descriptor.equals("S") || descriptor.equals("short")){
			descriptor = "short";
		} else if(descriptor.equals("F") || descriptor.equals("float")){
			descriptor = "float";
		} else if(descriptor.equals("D") || descriptor.equals("double")){
			descriptor = "double";
		} else if(descriptor.equals("C") || descriptor.equals("char")){
			descriptor = "char";
		} else if(descriptor.equals("B") || descriptor.equals("byte")){
			descriptor = "byte";
		} else if(descriptor.equals("Z") || descriptor.equals("boolean")){
			descriptor = "boolean";
		} else if(descriptor.startsWith("L")){
			// any non-primitive Object
			descriptor = descriptor.substring(1);
			descriptor = descriptor.replace("/", ".").trim();
		}
		descriptor += suffix;
		return descriptor.replace(";", "");
	}

	private static String getRawAnnotation(AnnotationNode annotation, Node annotationNode) {
		String rawAnnotationText = annotationNode.getAttr(XCSG.name).toString() + "(";
		String prefix = "";
		if(annotation.values != null){
			for(int i=0; i<annotation.values.size(); i+=2){
				String attribute = annotation.values.get(i).toString();
				Object value = annotation.values.get(i+1);
				String valueString = value.toString();
				if(value.getClass().equals(String.class)){
					valueString = "\"" + valueString.replace("\'", "\\'")
											 		.replace("\"", "\\\"")
											 		.replace("\\", "\\\\")
											 		.replace("\t", "\\t")
											 		.replace("\b", "\\b")
											 		.replace("\r", "\\r")
											 		.replace("\f", "\\f")
											 		.replace("\n", "\\n") + "\"";
				}
				rawAnnotationText += (prefix + (attribute + "=" + valueString));
				prefix = ",";
			}
		}
		rawAnnotationText += ")";
		return rawAnnotationText;
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
