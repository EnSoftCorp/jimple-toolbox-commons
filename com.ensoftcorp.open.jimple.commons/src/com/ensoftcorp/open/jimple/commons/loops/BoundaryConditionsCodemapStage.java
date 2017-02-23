package com.ensoftcorp.open.jimple.commons.loops;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ensoftcorp.open.commons.codemap.PrioritizedCodemapStage;
import com.ensoftcorp.open.jimple.commons.log.Log;
import com.ensoftcorp.open.jimple.commons.preferences.JimpleCommonsPreferences;

/**
 * Runs the loop boundary conditions analysis
 * 
 * @author Jon Mathews, analysis logic
 * @author Ben Holland, conversion to prioritized codemap
 */
public class BoundaryConditionsCodemapStage extends PrioritizedCodemapStage {

	/**
	 * The unique identifier for the codemap stage
	 */
	public static final String IDENTIFIER = "com.ensoftcorp.open.jimple.commons.loops.boundaryconditions";
	
	@Override
	public String getDisplayName() {
		return "Computing loop boundary conditions";
	}

	@Override
	public String getIdentifier() {
		return IDENTIFIER;
	}

	@Override
	public String[] getCodemapStageDependencies() {
		return new String[]{ DecompiledLoopIdentificationCodemapStage.IDENTIFIER };
	}

	@Override
	public void performIndexing(IProgressMonitor monitor) {
		try {
			if(JimpleCommonsPreferences.isDecompiledLoopRecoveryEnabled()){
				Log.info("Computing Loop Boundary Conditions...");
				BoundaryConditions.run(monitor);
				Log.info("Finished Computing Loop Boundary Conditions");
			}
		} catch (Exception e) {
			Log.error("Error computing loop boundary conditions", e);
		}
	}

}
