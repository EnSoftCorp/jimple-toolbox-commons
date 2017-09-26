package com.ensoftcorp.open.jimple.commons.transform.transforms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.index.common.SourceCorrespondence;
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
	
	public MethodCFGTransform(String phaseName, Node method) {
		if(phaseName == null){
			throw new IllegalArgumentException("Phase name cannot be null");
		}
		this.phaseName = phaseName;
		
		if(!method.taggedWith(XCSG.Method)){
			throw new IllegalArgumentException("The method parameter must be an XCSG.Method node.");
		}
		
		this.methodNode = method;
		
		// grab the container class node
		this.classNode = Common.toQ(method).parent().nodes(XCSG.Java.Class).eval().nodes().one();
		if(classNode == null){
			throw new IllegalArgumentException("Method node [" + methodNode.address().toAddressString() + "] is not contained in an XCSG.Java.Class node.");
		}
		
		// grab the container package node
		this.packageNode = Common.toQ(classNode).parent().nodes(XCSG.Package).eval().nodes().one();
		if(packageNode == null){
			throw new IllegalArgumentException("Class node [" + classNode.address().toAddressString() + "] is not contained in an XCSG.Pacakge node.");
		}
		this.cfgNodes = CommonQueries.cfg(method).eval().nodes();
		
		// assert each CFG node has a source correspondence
		for(Node cfgNode : cfgNodes){
			SourceCorrespondence sc = (SourceCorrespondence) cfgNode.getAttr(XCSG.sourceCorrespondence);
			if(sc == null){
				throw new IllegalArgumentException("CFG Node [" + cfgNode.address().toAddressString() + "] is missing a source correspondence.");
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
	protected void internalTransform(Body methodBody, String phaseName, Map options) {
		SootMethod sootMethod = methodBody.getMethod();
		SootClass sootClass = sootMethod.getDeclaringClass();
		
		String sootPackageName = sootClass.getJavaPackageName();
		String atlasPackageName = packageNode.getAttr(XCSG.name).toString();
		if(sootPackageName.equals(atlasPackageName)){
			
			String sootClassName = sootClass.getShortName();
			String atlasClassName = classNode.getAttr(XCSG.name).toString();
			if(sootClassName.equals(atlasClassName)){
				
				// using signatures would be nice if they were formatted the same, but they arn't...
				String sootMethodSignature = sootMethod.getSignature();
				String atlasMethodSignature = methodNode.getAttr("##signature").toString();
				
				String sootMethodName = sootMethod.getName();
				String atlasMethodName = methodNode.getAttr(XCSG.name).toString();
				if(sootMethodName.equals(atlasMethodName)){
					if(parametersMatch(sootMethod.getParameterTypes(), methodNode)){
						// map each method body unit to its corresponding atlas CFG node
						Chain<Unit> methodBodyUnits = methodBody.getUnits();
						HashMap<Unit,Node> atlasCorrespondence = new HashMap<Unit,Node>();
						int i = 1;
						
						// Using snapshopIterator avoids ConcurrentModificationException
						// form being thrown.
						Iterator<Unit> statementIterator = methodBodyUnits.snapshotIterator();
						
						while (statementIterator.hasNext()){
							Unit statement = statementIterator.next();
							
							try {
								Log.info(i + ") " + statement.toString());
								Node cfgNode = getMethodBodyStatement(i++);
								atlasCorrespondence.put(statement, cfgNode);
								Log.info("Mapped: " + statement.toString() + " to " + cfgNode.getAttr(XCSG.name));
							} catch (Exception e){
								Log.warning("Debug", e);
							}
						}
						
						// pass on the transformation duties to the implementer of this transformation
						transform(methodBody, atlasCorrespondence);
					}
				}
			}
		}
	}
	
	private boolean parametersMatch(List<Type> parameterTypes, Node methodNode) {
		return true; // TODO: implement
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
