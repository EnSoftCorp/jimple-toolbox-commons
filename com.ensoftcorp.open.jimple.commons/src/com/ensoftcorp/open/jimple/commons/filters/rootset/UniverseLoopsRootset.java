package com.ensoftcorp.open.jimple.commons.filters.rootset;

import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.open.commons.filters.rootset.FilterableRootset;
import com.ensoftcorp.open.jimple.commons.loops.DecompiledLoopIdentification;

public class UniverseLoopsRootset extends FilterableRootset {

	@Override
	public String getName() {
		return "Universe Loops";
	}

	@Override
	public String getDescription() {
		return "Every loop header in the program graph";
	}

	@Override
	public Q getRootSet() {
		return Common.universe().nodesTaggedWithAny(DecompiledLoopIdentification.CFGNode.LOOP_HEADER);
	}

}
