package com.ensoftcorp.open.jimple.commons.highlighter;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.ensoftcorp.atlas.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.markup.Markup;
import com.ensoftcorp.atlas.core.markup.MarkupProperty;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.query.Query;
import com.ensoftcorp.atlas.java.core.script.Common;
import com.ensoftcorp.open.jimple.commons.loops.DecompiledLoopIdentification.CFGNode;

public class CFGHighlighter {

	public static final Color cfgDefault = com.ensoftcorp.open.commons.highlighter.CFGHighlighter.cfgDefault;
	public static final Color cfgTrue = com.ensoftcorp.open.commons.highlighter.CFGHighlighter.cfgTrue;
	public static final Color cfgFalse = com.ensoftcorp.open.commons.highlighter.CFGHighlighter.cfgFalse;
	public static final Color cfgExceptional = com.ensoftcorp.open.commons.highlighter.CFGHighlighter.cfgExceptional;
	
	/** 
	 * The default blue fill color for CFG nodes
	 */
	public static final Color cfgNodeFillColor = new Color(51, 175, 243);
	
	/**
	 * GRAY  = Unconditional ControlFlow Edge
	 * WHITE = Conditional True ControlFlow Edge
	 * BLACK = Conditional False ControlFlow Edge
	 * BLUE  = Exceptional ControlFlow Edge
	 * @param m
	 */
	public static void applyHighlightsForCFEdges(Markup m) {
		com.ensoftcorp.open.commons.highlighter.CFGHighlighter.applyHighlightsForCFEdges(m);
	}
	
	/**
	 * Return marker for Loop Headers and members. Nodes are colored a darker
	 * color than the normal CFG color, that depend on the nesting depth of the
	 * loop header.
	 * 
	 * @param cfg
	 * @return
	 */
	public static Markup applyHighlightsForLoopDepth(Markup m) {
		Q loopHeadersQ = Common.universe().nodesTaggedWithAll(CFGNode.LOOP_HEADER);
		AtlasSet<Node> loopHeaders = loopHeadersQ.eval().nodes();
		
		Map<GraphElement, Color> colorMap = new HashMap<>();
		for (GraphElement loopHeader : loopHeaders) {
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

	private static Color applyHighlightsForLoopDepth(Map<GraphElement, Color> colorMap, GraphElement loopHeader) {
		Color color = colorMap.get(loopHeader);
		if (color == null) {
			Object idObj = loopHeader.getAttr(CFGNode.LOOP_MEMBER_ID);
			if (idObj == null) {
				// loop is not nested
				color = cfgNodeFillColor;
			} else {
				GraphElement parentLoopHeader = Query.universe().selectNode(CFGNode.LOOP_HEADER_ID, idObj).eval().nodes().one();
				color = applyHighlightsForLoopDepth(colorMap, parentLoopHeader);
				color = color.darker();
			}
			colorMap.put(loopHeader, color);
		}
		return color;
	}
}
