package com.ensoftcorp.open.jimple.commons.loops;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ensoftcorp.atlas.core.query.Query;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.codemap.PrioritizedCodemapStage;
import com.ensoftcorp.open.jimple.commons.log.Log;
import com.ensoftcorp.open.jimple.commons.preferences.JimpleCommonsPreferences;

public class DecompiledLoopIdentificationCodemapStage extends PrioritizedCodemapStage {

	public static final String IDENTIFIER = "com.ensoftcorp.open.jimple.commons.loops.recovery";
	
	@Override
	public String getDisplayName() {
		return "Decompiled Loop Identification";
	}

	@Override
	public String getIdentifier() {
		return IDENTIFIER;
	}

	@Override
	public String[] getCodemapStageDependencies() {
		return new String[]{}; // no dependencies
	}

	@Override
	public void performIndexing(IProgressMonitor monitor) {
		try {
			if(JimpleCommonsPreferences.isDecompiledLoopRecoveryEnabled()){
				Log.info("Recovering decompiled loops...");
				if(Query.universe().nodes(XCSG.ControlFlow_Node).nodes(XCSG.Language.Jimple, XCSG.Language.Java).eval().nodes().isEmpty()){
					Log.info("No decompiled loops to detect.");
				} else {
					DecompiledLoopIdentification.recoverLoops(monitor);
				}
			}
		} catch (Exception e) {
			Log.error("Error recovering decompiled loops", e);
		}
	}

}
