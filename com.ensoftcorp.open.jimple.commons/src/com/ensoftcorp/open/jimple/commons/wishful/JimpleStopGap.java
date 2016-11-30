package com.ensoftcorp.open.jimple.commons.wishful;

import java.util.ArrayList;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.java.commons.wishful.JavaStopGap;
import com.ensoftcorp.open.jimple.commons.log.Log;

/**
 * This class contains utilities that probably shouldn't exist outside of Atlas.
 * 
 * It's used as a stop gap measure until EnSoft can integrate or implement a
 * better solution.
 * 
 * @author Ben Holland
 */
public class JimpleStopGap {

	/**
	 * A tag added to "data flow display nodes". A data flow display node
	 * concept does not exist outside of Jimple, except for the acceptable
	 * instance variable accesses. In some analyses it is important to identify
	 * the data flow display node as having no significant analysis value.
	 */
	public static final String DATAFLOW_DISPLAY_NODE = "DATAFLOW_DISPLAY_NODE";

	/**
	 * Adds DATAFLOW_DISPLAY_NODE tags to display nodes Data flow display nodes
	 * are added for graph display reasons...
	 */
	public static void addDataFlowDisplayNodeTags() {
		Log.info("Adding data flow display node tags...");
		ArrayList<String> nonDataFlowDisplayNodeTags = new ArrayList<String>();
		for (String tag : XCSG.HIERARCHY.childrenOfOneParent(XCSG.DataFlow_Node)) {
			nonDataFlowDisplayNodeTags.add(tag);
		}
		String[] nonDataFlowDisplayNodeTagArray = new String[nonDataFlowDisplayNodeTags.size()];
		nonDataFlowDisplayNodeTags.toArray(nonDataFlowDisplayNodeTagArray);
		Q dataFlowNodes = Common.universe().nodesTaggedWithAny(XCSG.DataFlow_Node);
		Q classVariableAccessNodes = Common.universe().nodesTaggedWithAny(JavaStopGap.CLASS_VARIABLE_ACCESS);
		Q nonVanillaDataFlowNodes = Common.universe().nodesTaggedWithAny(nonDataFlowDisplayNodeTagArray);
		for (GraphElement dataFlowDisplayNode : dataFlowNodes
				.difference(classVariableAccessNodes, nonVanillaDataFlowNodes).eval().nodes()) {
			dataFlowDisplayNode.tag(DATAFLOW_DISPLAY_NODE);
		}

		// sanity check, better to fail fast here than later...
		Q localDataFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.LocalDataFlow);
		Q displayNodes = Common.universe().nodesTaggedWithAny(DATAFLOW_DISPLAY_NODE);

		// data flow display nodes should be accessible only from a local data
		// flow edge
		Q localDataFlowDisplayNodes = localDataFlowEdges.reverseStep(displayNodes).retainEdges();
		if (localDataFlowDisplayNodes.intersection(displayNodes).eval().nodes().size() != displayNodes.eval().nodes()
				.size()) {
			throw new RuntimeException("Unexpected data flow display nodes!");
		}

		// data flow display nodes parents should not also be data flow display
		// nodes
		Q dataFlowDisplayNodeParents = localDataFlowEdges.predecessors(displayNodes);
		if (!dataFlowDisplayNodeParents.nodesTaggedWithAny(DATAFLOW_DISPLAY_NODE).eval().nodes().isEmpty()) {
			throw new RuntimeException("Unexpected data flow display nodes parents!");
		}
	}

	/**
	 * Removes DATAFLOW_DISPLAY_NODE tags to display nodes
	 */
	public static void removeDataFlowDisplayNodeTags() {
		Log.info("Removing data flow display node tags...");
		AtlasSet<Node> dataFlowDisplayNodes = Common.universe().nodesTaggedWithAny(DATAFLOW_DISPLAY_NODE).eval()
				.nodes();
		AtlasHashSet<Node> dataFlowDisplayNodesToUntag = new AtlasHashSet<Node>();
		for (Node dataFlowDisplayNode : dataFlowDisplayNodes) {
			dataFlowDisplayNodesToUntag.add(dataFlowDisplayNode);
		}
		while (!dataFlowDisplayNodesToUntag.isEmpty()) {
			Node dataFlowDisplayNode = dataFlowDisplayNodesToUntag.getFirst();
			dataFlowDisplayNodesToUntag.remove(dataFlowDisplayNode);
			dataFlowDisplayNode.tags().remove(DATAFLOW_DISPLAY_NODE);
		}
	}

	/**
	 * Returns the set local references that the data flow display node is
	 * representing
	 * 
	 * @param displayNode
	 * @return
	 */
	public static AtlasSet<Node> getDisplayNodeReferences(GraphElement displayNode) {
		Q localDataFlowEdges = Common.universe().edgesTaggedWithAny(XCSG.LocalDataFlow);
		Q dataFlowDisplayNodeParents = localDataFlowEdges.predecessors(Common.toQ(displayNode));
		return dataFlowDisplayNodeParents.eval().nodes();
	}

}
