package com.ensoftcorp.open.jimple.commons.soot.transforms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.index.common.SourceCorrespondence;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.query.Query;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.utilities.NodeSourceCorrespondenceSorter;
import com.ensoftcorp.open.java.commons.analysis.CommonQueries;
import com.ensoftcorp.open.jimple.commons.log.Log;

import soot.Body;
import soot.BodyTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.Unit;
import soot.util.Chain;

/**
 * This class implements a soot method body transformer that assists with
 * recovering mapping between Atlas's method XCSG.ControlFlow_Node nodes 
 * and Soot method body units.
 * 
 * @author Ben Holland
 */
public abstract class MethodCFGTransform extends BodyTransformer {

	private String phaseName;
	
	protected Node methodNode;
	protected Node classNode;
	protected Node packageNode;
	protected AtlasSet<Node> cfgNodes;
	protected ArrayList<Node> cfgNodeSourceOrdering;
	protected AtlasSet<Node> restrictedRegion;
	
	private static int id = 0;
	
	public MethodCFGTransform(String phaseName, Node method) {
		if(phaseName == null){
			throw new IllegalArgumentException("Phase name cannot be null");
		}
		
		// TODO: this is a nicer name scheme, but its not very reliable
//		String qualifiedMethodName = CommonQueries.getQualifiedMethodName(method);
//		this.phaseName = phaseName + "-" + qualifiedMethodName.substring(qualifiedMethodName.lastIndexOf(".")) + method.getAttr(JavaStopGap.SIGNATURE);
		
		this.phaseName = phaseName + ": " + (id++);
		
		if(!method.taggedWith(XCSG.Language.Jimple)){
			throw new IllegalArgumentException("The method parameter must be an XCSG.Language.Jimple node.");
		}
		
		if(!method.taggedWith(XCSG.Method)){
			throw new IllegalArgumentException("The method parameter must be an XCSG.Method node.");
		}
		
		this.methodNode = method;
		
		// grab the container class node
		this.classNode = Common.toQ(method).parent().nodes(XCSG.Java.Class).nodes(XCSG.Language.Jimple).eval().nodes().one();
		if(classNode == null){
			throw new IllegalArgumentException("Method node [" + methodNode.address().toAddressString() + "] is not contained in an XCSG.Java.Class node.");
		}
		
		// grab the container package node
		this.packageNode = Common.toQ(classNode).containers().nodes(XCSG.Package).eval().nodes().one();
		if(packageNode == null){
			throw new IllegalArgumentException("Class node [" + classNode.address().toAddressString() + "] is not contained in an XCSG.Package node.");
		}
		this.cfgNodes = CommonQueries.cfg(method).nodes(XCSG.Language.Jimple).eval().nodes();
		
		// assert each CFG node has a source correspondence
		restrictedRegion = new AtlasHashSet<Node>();
		for(Node cfgNode : cfgNodes){
			SourceCorrespondence sc = (SourceCorrespondence) cfgNode.getAttr(XCSG.sourceCorrespondence);
			if(sc == null){
				throw new IllegalArgumentException("CFG Node [" + cfgNode.address().toAddressString() + "] is missing a source correspondence.");
			}
			String jimpleStatementText = cfgNode.getAttr(XCSG.name).toString();
			if(jimpleStatementText.contains(" := @this") || jimpleStatementText.contains(" := @parameter") || jimpleStatementText.contains(":= @caughtexception")){
				restrictedRegion.add(cfgNode);
			}
		}
		
		// recover the body unit ordering
		ArrayList<Node> sortedCFGNodes = new ArrayList<Node>((int) cfgNodes.size());
		for(Node cfgNode : cfgNodes){
			sortedCFGNodes.add(cfgNode);
		}
		Collections.sort(sortedCFGNodes, new NodeSourceCorrespondenceSorter());
		this.cfgNodeSourceOrdering = sortedCFGNodes;
	}
	
	private boolean classNamesMatch(String sootClassName, Node classNode){
		if(sootClassName.equals(classNode.getAttr(XCSG.name).toString())){
			// simple match
			return true;
		}
		
		// check inner class names
		if(sootClassName.contains("$")){
			String[] sootClassNames = sootClassName.split("\\$");
			for(int i=sootClassNames.length-1; i>=0; i--){
				if(classNode.taggedWith(XCSG.Package)){
					return false;
				} else {
					if(!sootClassNames[i].equals(classNode.getAttr(XCSG.name).toString())){
						return false;
					} else {
						classNode = Common.toQ(classNode).parent().eval().nodes().one();
					}
				}
			}
		}
		
		return true;
	}
	
	@Override
	protected void internalTransform(Body methodBody, String phaseName, @SuppressWarnings("rawtypes") Map options) {
		SootMethod sootMethod = methodBody.getMethod();
		SootClass sootClass = sootMethod.getDeclaringClass();
		
		String sootPackageName = sootClass.getJavaPackageName();
		String atlasPackageName = packageNode.getAttr(XCSG.name).toString();
		if(sootPackageName.equals(atlasPackageName)){
			
			String sootClassName = sootClass.getShortName();
			if(classNamesMatch(sootClassName, classNode)){
				String sootMethodName = sootMethod.getName();
				String atlasMethodName = methodNode.getAttr(XCSG.name).toString();
				if(sootMethodName.equals(atlasMethodName)){
					if(parametersMatch(new ArrayList<Type>(sootMethod.getParameterTypes()), methodNode)){
						// map each method body unit to its corresponding atlas CFG node
						Chain<Unit> methodBodyUnits = methodBody.getUnits();
						HashMap<Unit,Node> atlasCorrespondence = new HashMap<Unit,Node>();
						
						if(methodBodyUnits.size() != cfgNodeSourceOrdering.size()){
							Log.error("Unequal number of statements! All statements in " + CommonQueries.getQualifiedMethodName(methodNode) + " will be ignored."
									+ "\nSoot Statements: " + methodBodyUnits.size()
									+ "\nAtlas Statements: " + cfgNodeSourceOrdering.size(), new RuntimeException("Unequal number of statements!"));
						} else {
							// Using snapshopIterator avoids ConcurrentModificationException
							// form being thrown.
							Iterator<Unit> statementIterator = methodBodyUnits.snapshotIterator();
							int i = 0;
							while (statementIterator.hasNext()){
								Unit statement = statementIterator.next();
								try {
									Node cfgNode = getMethodBodyStatement(i++);
									atlasCorrespondence.put(statement, cfgNode);
									// debug
//									Log.info("Mapped: " + statement.toString() + " to " + cfgNode.getAttr(XCSG.name));
								} catch (Exception e){
									Log.warning("Error mapping soot statement to Atlas CFG node", e);
								}
							}
							
							// pass on the transformation duties to the implementer of this transformation
							transform(methodBody, atlasCorrespondence);
						}
					}
				}
			}
		}
	}
	
	private boolean parametersMatch(ArrayList<Type> sootParameterTypes, Node atlasMethodNode) {
		Q parameterEdges = Query.universe().edges(XCSG.HasParameter);
		AtlasSet<Node> atlasMethodParameters = parameterEdges.successors(Common.toQ(methodNode)).eval().nodes();
		
		// cheap check, should have the same number of parameters
		if(sootParameterTypes.size() != atlasMethodParameters.size()){
			return false;
		}
		
		// parameters should match types in the order parameters are specified
		ArrayList<Node> atlasSortedMethodParameters = new ArrayList<Node>((int) atlasMethodParameters.size());
		for(Node atlasMethodParameter : atlasMethodParameters){
			atlasSortedMethodParameters.add(atlasMethodParameter);
		}
		Collections.sort(atlasSortedMethodParameters, new Comparator<Node>(){
			@Override
			public int compare(Node p1, Node p2) {
				Integer p1Index = Integer.parseInt(p1.getAttr(XCSG.parameterIndex).toString());
				Integer p2Index = Integer.parseInt(p2.getAttr(XCSG.parameterIndex).toString());
				return p1Index.compareTo(p2Index);
			}
		});
		
		for(int i=0; i<atlasSortedMethodParameters.size(); i++){
			Type sootMethodParameterType = sootParameterTypes.get(i);
			Node atlasMethodParameter = atlasSortedMethodParameters.get(i);
			
			if(!checkTypeEquality(sootMethodParameterType, atlasMethodParameter)){
				return false;
			}
		}
		
		return true;
	}

	protected boolean checkTypeEquality(Type sootType, Node atlasDataFlowNode){
		Q typeOfEdges = Query.universe().edges(XCSG.TypeOf);
		Node atlasType = typeOfEdges.successors(Common.toQ(atlasDataFlowNode)).eval().nodes().one();
		if(atlasType == null){
			Log.warning("Data Flow Node " + atlasDataFlowNode.address().toAddressString() + " has no type.");
			return false;
		} else {
			String qualifiedSootType = sootType.toString();
			String qualifiedAtlasType = atlasType.getAttr(XCSG.name).toString();
			
			// primitives are unqualified
			if(qualifiedSootType.equals(qualifiedAtlasType)){
				return true;
			}
			
			// check if Atlas node is an array type
			if(atlasType.taggedWith(XCSG.ArrayType)){
				Q arrayElementTypeEdges = Query.universe().edges(XCSG.ArrayElementType);
				Node arrayType = atlasType;
				atlasType = arrayElementTypeEdges.successors(Common.toQ(arrayType)).eval().nodes().one();
				if(atlasType == null){
					Log.warning("Array type " + arrayType.address().toAddressString() + " has no array element type.");
					return false;
				}
			}
			
			if(!atlasType.taggedWith(XCSG.Primitive)){
				// add the Atlas type package qualification
				Node atlasTypePackage = Common.toQ(atlasType).containers().nodes(XCSG.Package).eval().nodes().one();
				if(atlasTypePackage == null){
					Log.warning("Data Flow Node " + atlasType.address().toAddressString() + " has no package.");
					return false;
				} else {
					qualifiedAtlasType = atlasTypePackage.getAttr(XCSG.name) + "." + qualifiedAtlasType;
				}
				if(!qualifiedSootType.equals(qualifiedAtlasType)){
					return false;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * The method body transformation logic
	 * @param methodBody
	 * @param atlasCorrespondence
	 */
	protected abstract void transform(Body methodBody, Map<Unit,Node> atlasControlFlowNodeCorrespondence);
	
	/**
	 * Returns the corresponding Atlas node for the given Soot unit body index
	 * @param unitIndex
	 * @return
	 */
	private Node getMethodBodyStatement(int unitIndex){
		return cfgNodeSourceOrdering.get(unitIndex);
	}
	
	/**
	 * Returns this transformations phase name
	 * @return
	 */
	public String getPhaseName(){
		return phaseName;
	}
	
	/**
	 * Returns a soot Transform for this transformation
	 * @return
	 */
	public Transform getTransform(){
		return new Transform(("jtp." + phaseName), this);
	}
	
}
