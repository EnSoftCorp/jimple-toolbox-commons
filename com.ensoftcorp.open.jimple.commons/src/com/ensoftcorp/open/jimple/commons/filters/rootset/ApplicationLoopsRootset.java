package com.ensoftcorp.open.jimple.commons.filters.rootset;

import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.open.commons.analysis.SetDefinitions;
import com.ensoftcorp.open.commons.filters.rootset.FilterableRootset;
import com.ensoftcorp.open.jimple.commons.loops.DecompiledLoopIdentification.CFGNode;

public class ApplicationLoopsRootset extends FilterableRootset {

	@Override
	public String getName() {
		return "Application Loops";
	}

	@Override
	public String getDescription() {
		return "Every loop header in the application (excludes libraries)";
	}

	@Override
	public Q getRootSet() {
		return SetDefinitions.app().nodesTaggedWithAny(CFGNode.LOOP_HEADER);
	}

}
