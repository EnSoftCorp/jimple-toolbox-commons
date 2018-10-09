package com.ensoftcorp.open.jimple.commons.loops;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement.NodeDirection;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.query.Query;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.jimple.commons.loops.DecompiledLoopIdentification.CFGNode;

public class BoundaryConditions {
	
	public static final String BOUNDARY_CONDITION = "BOUNDARY_CONDITION";
	
	/** A ControlFlowCondition which is a Boundary Condition with respect to the loop it is a member of. */
	public static final String BC_CASE1 = "BC_CASE1";

	/** A Loop Header which is a Boundary Condition with respect to itself, but not necessarily the loop it is a member of. */
	public static final String BC_CASE2 = "BC_CASE2";
	
	
	/** disable for efficiency, enable for case coverage **/
	private static final boolean markCases = true;

	/** 
	 * Pre: LoopAnalyzer has identified loop headers.
	 * Post: Boundary conditions are tagged.
	 * 
	 * A ControlFlowCondition is a Boundary Condition iff there exists a 
	 * successor node in a different loop.
	 * 
	 * @param monitor
	 */
	public static void run(IProgressMonitor monitor) {
		
		// Boundary conditions are those which leave a loop
		// Successors in regular ControlFlow only; Exceptional exits are not considered at this time
		
		Q qCfg = Query.universe().edges(XCSG.ControlFlow_Edge);
		Graph cfg = qCfg.eval();

		Q conditions = Query.universe().nodesTaggedWithAll(XCSG.ControlFlowCondition);
		Q conditionsInLoops = conditions.selectNode(CFGNode.LOOP_MEMBER_ID);
		Q conditionalLoopHeaders = conditions.selectNode(CFGNode.LOOP_HEADER_ID); // outermost loop headers are missed otherwise
		
		Q candidates = conditionsInLoops.union(conditionalLoopHeaders);
		
		AtlasSet<Node> nodes = candidates.eval().nodes();
		for (Node pred : nodes) {
			AtlasSet<Edge> outEdges = cfg.edges(pred, NodeDirection.OUT);
			for (Edge outEdge : outEdges) {
				
				Node successor = outEdge.to();
				
				Object loopIdPred = null;
				Object loopIdSucc = null;
				
				if (outEdge.taggedWith(XCSG.ControlFlowBackEdge)) {
					// assert: successor is a loop header
					loopIdPred = getMemberId(pred);
					loopIdSucc = getHeaderId(successor);
					
					if (isLoopHeader(pred)) {
						// assert: predecessor is a loop header, and a condition; successor is a loop header
						// Implies that the predecessor is a Boundary Condition with respect to itself.
						// However, this is NOT a Boundary Condition with respect to the loop it is a member of.
						pred.tag(BOUNDARY_CONDITION); // existence of one successor outside the loop is sufficient
						
						if (markCases) {
							pred.tag(BC_CASE2);
						} else {
							continue;
						}
					}					
					
				} else {
					if (isLoopHeader(pred)) {
						loopIdPred = getHeaderId(pred);
						loopIdSucc = getMemberId(successor);
					} else {
						loopIdPred = getMemberId(pred);
						loopIdSucc = getMemberId(successor);
					}
				}
				
				if (objectEquals(loopIdPred, loopIdSucc)) {
					// assert: pred and succ are in the same loop
				} else {
					// assert: succ is in a different loop, therefore pred is a TC
					pred.tag(BOUNDARY_CONDITION);

					if (markCases){
						pred.tag(BC_CASE1);
					} else {
						continue; // existence of one successor outside the loop is sufficient
					}
				}
			}
		}
		
	}

	private static boolean objectEquals(Object o1, Object o2) {
		if (o1 == o2){
			return true;
		}
		
		// assert: at least one is not null
		
		if (o1 == null) {
			// assert: o2 is !null
			return o2.equals(o1);
		}
		
		// assert: o1 is !null
		return o1.equals(o2);
	}

	private static boolean isLoopHeader(Node cfNode) {
		return cfNode.taggedWith(XCSG.Loop);
	}

	private static Object getMemberId(Node cfNode) {
		return cfNode.getAttr(CFGNode.LOOP_MEMBER_ID);
	}

	private static Object getHeaderId(Node cfNode) {
		return cfNode.getAttr(CFGNode.LOOP_HEADER_ID);
	}
	
}
