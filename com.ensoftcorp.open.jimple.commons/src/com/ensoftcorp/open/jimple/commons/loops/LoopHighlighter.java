package com.ensoftcorp.open.jimple.commons.loops;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.log.Log;
import com.ensoftcorp.atlas.core.markup.Markup;
import com.ensoftcorp.atlas.core.markup.MarkupProperty;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.query.Query;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.atlas.java.core.script.Common;
import com.ensoftcorp.open.jimple.commons.loops.DecompiledLoopIdentification.CFGNode;

public class LoopHighlighter {

	/** 
	 * The default blue fill color for CFG nodes
	 */
	public static final Color cfgNodeFillColor = new Color(51, 175, 243);
	
	/**
	 * Adds markup for loops and loop children. Nodes are colored a darker color
	 * than the normal CFG color, that depend on the nesting depth of the loop
	 * header.
	 * 
	 * @param cfg
	 * @return
	 */
	public static Markup applyHighlightsForLoopDepth(Markup m) {
		Q loopHeadersQ = Common.universe().nodesTaggedWithAll(XCSG.Loop);
		AtlasSet<Node> loopHeaders = loopHeadersQ.eval().nodes();
		
		Map<Node, Color> colorMap = new HashMap<Node, Color>();
		for (Node loopHeader : loopHeaders) {
			Color color = applyHighlightsForLoopDepth(colorMap, loopHeader);
			m.set(loopHeader, MarkupProperty.NODE_BACKGROUND_COLOR, color);
		}
		
		// set color of loop members (other than loop headers) to same color as header 
		Q loopFragments = Common.universe().selectNode(CFGNode.LOOP_MEMBER_ID).difference(loopHeadersQ);
		for (GraphElement member : loopFragments.eval().nodes()) {
			Object id = member.getAttr(CFGNode.LOOP_MEMBER_ID);
			GraphElement loopHeader = Query.universe().selectNode(CFGNode.LOOP_HEADER_ID, id).eval().nodes().one();
			m.set(member, MarkupProperty.NODE_BACKGROUND_COLOR, colorMap.get(loopHeader));
		}
		return m;
	}

	private static Color applyHighlightsForLoopDepth(Map<Node, Color> colorMap, Node loopHeader) {
		Color color = colorMap.get(loopHeader);
		if (color == null) {
			Object idObj = loopHeader.getAttr(CFGNode.LOOP_MEMBER_ID);
			if (idObj == null) {
				// loop is not nested
				color = cfgNodeFillColor;
			} else {
				Node parentLoopHeader = Query.universe().selectNode(CFGNode.LOOP_HEADER_ID, idObj).eval().nodes().one();
				if(parentLoopHeader != null){
					color = applyHighlightsForLoopDepth(colorMap, parentLoopHeader);
					color = color.darker();
				} else {
					Log.warning("Parent loop header is null for loop header id: " + idObj);
				}
			}
			colorMap.put(loopHeader, color);
		}
		return color;
	}
}
