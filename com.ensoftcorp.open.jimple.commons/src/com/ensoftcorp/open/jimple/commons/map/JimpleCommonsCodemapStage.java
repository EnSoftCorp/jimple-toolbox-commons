package com.ensoftcorp.open.jimple.commons.map;


import org.eclipse.core.runtime.IProgressMonitor;

import com.ensoftcorp.atlas.core.indexing.providers.ToolboxIndexingStage;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.jimple.commons.log.Log;
import com.ensoftcorp.open.jimple.commons.loops.DecompiledLoopIdentification;
import com.ensoftcorp.open.jimple.commons.preferences.JimpleCommonsPreferences;

/**
 * A hook for automatically jimple commons analyses
 * 
 * @author Ben Holland
 */
public class JimpleCommonsCodemapStage implements ToolboxIndexingStage {

	@Override
	public String displayName() {
		return "Jimple Commons";
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