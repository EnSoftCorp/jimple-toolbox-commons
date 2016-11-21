package com.ensoftcorp.open.jimple.commons.loops;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ensoftcorp.open.commons.codemap.PrioritizedCodemapStage;
import com.ensoftcorp.open.jimple.commons.log.Log;
import com.ensoftcorp.open.jimple.commons.preferences.JimpleCommonsPreferences;

public class DecompiledLoopIdenfiticationCodemapStage extends PrioritizedCodemapStage {

	public static final String IDENTIFIER = "DECOMPILED_LOOP_RECOVERY";
	
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
				Log.info("Recovering Decompiled Loops...");
				DecompiledLoopIdentification.recoverLoops(monitor);
				Log.info("Finished Recovering Decompiled Loops");
			}
		} catch (Exception e) {
			Log.error("Error recovering decompiled loops", e);
		}
	}

}
