package com.ensoftcorp.open.jimple.commons.transform.transforms;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.java.commons.analysis.CommonQueries;

import soot.Body;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.util.Chain;

/**
 * This class implements a soot method body transformer that assists with
 * recovering mapping between Atlas's method XCSG.ControlFlow_Node nodes and
 * Soot method body units.
 * 
 * Additionally this class also provides a corresponding mapping between each
 * Soot ValueBox unit and Atlas Data Flow Node as needed for each statement.
 * 
 * @author Ben Holland
 */
public abstract class MethodDFGTransform extends MethodCFGTransform {

	protected AtlasSet<Node> dfgNodes;
	
	public MethodDFGTransform(String phaseName, Node method) {
		super(phaseName, method);
		this.dfgNodes = CommonQueries.dfg(method).nodes(XCSG.Language.Jimple).eval().nodes();
	}
	
	@Override
	protected void transform(Body methodBody, Map<Unit,Node> atlasControlFlowNodeCorrespondence) {
		Map<ValueBox,Node> atlasDataFlowNodeCorrespondence = new HashMap<ValueBox,Node>();
		
		Chain<Unit> statements = methodBody.getUnits();
		Iterator<Unit> methodBodyUnitsIterator = statements.snapshotIterator();
		while(methodBodyUnitsIterator.hasNext()){
			Unit statement = methodBodyUnitsIterator.next();
			Node atlasControlFlowNode = atlasControlFlowNodeCorrespondence.get(statement);
			if(atlasControlFlowNode != null && !restrictedRegion.contains(atlasControlFlowNode)){
				Map<ValueBox, Node> correspondences = getDataFlowNodeCorrespondence(statement, atlasControlFlowNode);
				atlasDataFlowNodeCorrespondence.putAll(correspondences);
			}
		}
		
		// pass on the transformation duties to the implementer of this transformation
		transform(methodBody, atlasControlFlowNodeCorrespondence, atlasDataFlowNodeCorrespondence);
	}
	
	protected abstract void transform(Body methodBody, Map<Unit,Node> atlasControlFlowNodeCorrespondence, Map<ValueBox,Node> atlasDataFlowNodeCorrespondence);
	
	private Map<ValueBox,Node> getDataFlowNodeCorrespondence(Unit statement, Node atlasStatement){
		Map<ValueBox,Node> correspondence = new HashMap<ValueBox,Node>();
		
		AtlasSet<Node> atlasDFNodes = Common.toQ(atlasStatement).children().eval().nodes();
		AtlasSet<Node> defNodes = Common.toQ(atlasDFNodes).nodes(XCSG.Assignment).eval().nodes();
		List<ValueBox> defs = statement.getDefBoxes();
		for(ValueBox def : defs){
			Value value = def.getValue();
			String defName = value.toString();
			Type defType = value.getType();
			for(Node defNode : defNodes){
				String defNodeName = defNode.getAttr(XCSG.name).toString();
				if(defNodeName.contains(defName)){
					if(checkTypeEquality(defType, defNode)){
						correspondence.put(def, defNode);
					}
				}
			}
		}
		
		AtlasSet<Node> useNodes = Common.toQ(atlasDFNodes).difference(Common.toQ(defNodes)).eval().nodes();
		List<ValueBox> uses = statement.getUseBoxes();
		for(ValueBox use : uses){
			Value value = use.getValue();
			String useName = value.toString();
			Type useType = value.getType();
			for(Node useNode : useNodes){
				String useNodeName = useNode.getAttr(XCSG.name).toString();
				if(useNodeName.contains(useName)){
					if(checkTypeEquality(useType, useNode)){
						correspondence.put(use, useNode);
					}
				}
			}
		}
		
		return correspondence;
	}
}
