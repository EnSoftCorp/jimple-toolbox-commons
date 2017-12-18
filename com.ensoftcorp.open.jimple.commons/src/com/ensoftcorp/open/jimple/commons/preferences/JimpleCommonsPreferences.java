package com.ensoftcorp.open.jimple.commons.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.ensoftcorp.open.jimple.commons.Activator;
import com.ensoftcorp.open.jimple.commons.log.Log;

public class JimpleCommonsPreferences extends AbstractPreferenceInitializer {

	private static boolean initialized = false;
	
	/**
	 * Enable/disable using original jars for CFR Decompiler Correspondence view
	 */
	public static final String CFR_CORRESPONDENCE_USE_ORIGINAL_JARS = "CFR_CORRESPONDENCE_USE_ORIGINAL_JARS";
	public static final Boolean CFR_CORRESPONDENCE_USE_ORIGINAL_JARS_DEFAULT = false;
	private static boolean cfrCorrespondenceUseOriginalJarsValue = CFR_CORRESPONDENCE_USE_ORIGINAL_JARS_DEFAULT;
	
	/**
	 * Configures whether or not to use original jars for CFR Decompiler Correspondence view
	 */
	public static void enableCFRCorrespondenceUseOriginalJars(boolean enabled){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(CFR_CORRESPONDENCE_USE_ORIGINAL_JARS, enabled);
		loadPreferences();
	}
	
	/**
	 * Returns true if using the original jars for CFR Decompiler Correspondence view
	 * @return
	 */
	public static boolean isCFRCorrespondenceUseOriginalJarsEnabled(){
		if(!initialized){
			loadPreferences();
		}
		return cfrCorrespondenceUseOriginalJarsValue;
	}
	
	/**
	 * Enable/disable decompiled loop identification
	 */
	public static final String RECOVER_DECOMPILED_LOOPS = "RECOVER_DECOMPILED_LOOPS";
	public static final Boolean RECOVER_DECOMPILED_LOOPS_DEFAULT = false;
	private static boolean decompiledLoopRecoveryValue = RECOVER_DECOMPILED_LOOPS_DEFAULT;
	
	/**
	 * Configures whether or not to run recovery of decompiled loop analysis
	 */
	public static void enableDecompiledLoopRecovery(boolean enabled){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(RECOVER_DECOMPILED_LOOPS, enabled);
		loadPreferences();
	}
	
	/**
	 * Returns true if recovery of decompiled loop analysis is enabled
	 * @return
	 */
	public static boolean isDecompiledLoopRecoveryEnabled(){
		if(!initialized){
			loadPreferences();
		}
		return decompiledLoopRecoveryValue;
	}
	
	/**
	 * Enable/disable loop boundary condition identification
	 */
	public static final String COMPUTE_LOOP_BOUNDARIES = "COMPUTE_LOOP_BOUNDARIES";
	public static final Boolean COMPUTE_LOOP_BOUNDARIES_DEFAULT = false;
	private static boolean computeLoopBoundariesValue = COMPUTE_LOOP_BOUNDARIES_DEFAULT;
	
	/**
	 * Configures whether or not to run loop boundary analysis
	 */
	public static void enableLoopBoundaryAnalysis(boolean enabled){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(COMPUTE_LOOP_BOUNDARIES, enabled);
		loadPreferences();
	}
	
	/**
	 * Returns true if loop boundary analysis is enabled
	 * @return
	 */
	public static boolean isComputeLoopBoundariesEnabled(){
		if(!initialized){
			loadPreferences();
		}
		return computeLoopBoundariesValue;
	}
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setDefault(CFR_CORRESPONDENCE_USE_ORIGINAL_JARS, CFR_CORRESPONDENCE_USE_ORIGINAL_JARS_DEFAULT);
		preferences.setDefault(RECOVER_DECOMPILED_LOOPS, RECOVER_DECOMPILED_LOOPS_DEFAULT);
		preferences.setDefault(COMPUTE_LOOP_BOUNDARIES, COMPUTE_LOOP_BOUNDARIES_DEFAULT);
	}
	
	/**
	 * Restores the default preferences
	 */
	public static void restoreDefaults(){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(CFR_CORRESPONDENCE_USE_ORIGINAL_JARS, CFR_CORRESPONDENCE_USE_ORIGINAL_JARS_DEFAULT);
		preferences.setValue(RECOVER_DECOMPILED_LOOPS, RECOVER_DECOMPILED_LOOPS_DEFAULT);
		preferences.setValue(COMPUTE_LOOP_BOUNDARIES, COMPUTE_LOOP_BOUNDARIES_DEFAULT);
		loadPreferences();
	}
	
	/**
	 * Loads or refreshes current preference values
	 */
	public static void loadPreferences() {
		try {
			IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
			cfrCorrespondenceUseOriginalJarsValue = preferences.getBoolean(CFR_CORRESPONDENCE_USE_ORIGINAL_JARS);
			decompiledLoopRecoveryValue = preferences.getBoolean(RECOVER_DECOMPILED_LOOPS);
			computeLoopBoundariesValue = preferences.getBoolean(COMPUTE_LOOP_BOUNDARIES);
		} catch (Exception e){
			Log.warning("Error accessing jimple commons analysis preferences, using defaults...", e);
		}
		initialized = true;
	}
}