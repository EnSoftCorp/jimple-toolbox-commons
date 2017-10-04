package com.ensoftcorp.open.jimple.commons.transform.transforms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.index.common.SourceCorrespondence;
import com.ensoftcorp.atlas.core.query.Q;
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
	
	public MethodCFGTransform(String phaseName, Node method) {
		if(phaseName == null){
			throw new IllegalArgumentException("Phase name cannot be null");
		}
		this.phaseName = phaseName;
		
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
		this.packageNode = Common.toQ(classNode).parent().nodes(XCSG.Package).nodes(XCSG.Language.Jimple).eval().nodes().one();
		if(packageNode == null){
			throw new IllegalArgumentException("Class node [" + classNode.address().toAddressString() + "] is not contained in an XCSG.Pacakge node.");
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
	
	@Override
	protected void internalTransform(Body methodBody, String phaseName, @SuppressWarnings("rawtypes") Map options) {
		SootMethod sootMethod = methodBody.getMethod();
		SootClass sootClass = sootMethod.getDeclaringClass();
		
		String sootPackageName = sootClass.getJavaPackageName();
		String atlasPackageName = packageNode.getAttr(XCSG.name).toString();
		if(sootPackageName.equals(atlasPackageName)){
			
			String sootClassName = sootClass.getShortName();
			String atlasClassName = classNode.getAttr(XCSG.name).toString();
			if(sootClassName.equals(atlasClassName)){
				
				// TODO: would be nice to figure out how we could just match by signature instead
//				// using signatures would be nice if they were formatted the same, but they arn't...
//				String sootMethodSignature = sootMethod.getSignature();
//				String atlasMethodSignature = methodNode.getAttr("##signature").toString();
				
				String sootMethodName = sootMethod.getName();
				String atlasMethodName = methodNode.getAttr(XCSG.name).toString();
				if(sootMethodName.equals(atlasMethodName)){
					if(parametersMatch(new ArrayList<Type>(sootMethod.getParameterTypes()), methodNode)){
						// map each method body unit to its corresponding atlas CFG node
						Chain<Unit> methodBodyUnits = methodBody.getUnits();
						HashMap<Unit,Node> atlasCorrespondence = new HashMap<Unit,Node>();
						
						if(methodBodyUnits.size() != cfgNodeSourceOrdering.size()){
							Log.warning("Unequal number of statements!"
									+ "\nSoot Statements: " + methodBodyUnits.size()
									+ "\nAtlas Statements: " + cfgNodeSourceOrdering.size());
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
		Q parameterEdges = Common.universe().edges(XCSG.HasParameter);
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
			
			Q typeOfEdges = Common.universe().edges(XCSG.TypeOf);
			Node atlasMethodParameterType = typeOfEdges.successors(Common.toQ(atlasMethodParameter)).eval().nodes().one();
			if(atlasMethodParameterType == null){
				Log.warning("Method parameter " + atlasMethodParameter.address().toAddressString() + " has no type.");
				return false;
			} else {
				String qualifiedSootParameterType = sootMethodParameterType.toString();
				String qualifiedAtlasParameterType = atlasMethodParameterType.getAttr(XCSG.name).toString();
				
				// check if Atlas node is an array type
				if(atlasMethodParameterType.taggedWith(XCSG.ArrayType)){
					Q arrayElementTypeEdges = Common.universe().edges(XCSG.ArrayElementType);
					Node arrayType = atlasMethodParameterType;
					atlasMethodParameterType = arrayElementTypeEdges.successors(Common.toQ(arrayType)).eval().nodes().one();
					if(atlasMethodParameterType == null){
						Log.warning("Array type " + arrayType.address().toAddressString() + " has no array element type.");
					}
				}
				
				// add the Atlas type package qualification
				Node atlasParameterTypePackage = Common.toQ(atlasMethodParameterType).containers().nodes(XCSG.Package).eval().nodes().one();
				if(atlasParameterTypePackage == null){
					Log.warning("Method parameter type " + atlasMethodParameterType.address().toAddressString() + " has no package.");
					return false;
				} else {
					qualifiedAtlasParameterType = atlasParameterTypePackage.getAttr(XCSG.name) + "." + qualifiedAtlasParameterType;
				}
				if(!qualifiedSootParameterType.equals(qualifiedAtlasParameterType)){
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
	protected abstract void transform(Body methodBody, Map<Unit,Node> atlasCorrespondence);
	
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
