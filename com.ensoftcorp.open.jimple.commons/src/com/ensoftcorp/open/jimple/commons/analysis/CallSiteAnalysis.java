package com.ensoftcorp.open.jimple.commons.analysis;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.db.set.EmptyAtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.query.Query;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.analysis.CallSiteAnalysis.LanguageSpecificCallSiteAnalysis;
import com.ensoftcorp.open.commons.analysis.G;
import com.ensoftcorp.open.java.commons.log.Log;

public class CallSiteAnalysis extends com.ensoftcorp.open.java.commons.analysis.CallSiteAnalysis {

	// constructor must be visible to contribute as language specific analysis extension point
	public CallSiteAnalysis(){}

	@Override
	public String getName() {
		return "Jimple Call Site Analysis";
	}

	@Override
	public Set<String> getSupportedLanguages() {
		HashSet<String> languages = new HashSet<String>();
		languages.add(XCSG.Language.Jimple);
		return languages;
	}
}
