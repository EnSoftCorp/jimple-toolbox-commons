package com.ensoftcorp.open.jimple.commons.analysis;

import java.util.HashSet;
import java.util.Set;

import com.ensoftcorp.atlas.core.xcsg.XCSG;

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
