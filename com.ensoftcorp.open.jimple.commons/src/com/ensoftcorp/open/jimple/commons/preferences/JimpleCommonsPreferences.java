package com.ensoftcorp.open.jimple.commons.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.ensoftcorp.open.jimple.commons.Activator;
import com.ensoftcorp.open.jimple.commons.log.Log;

public class JimpleCommonsPreferences extends AbstractPreferenceInitializer {

	private static boolean initialized = false;
	
	/**
	 * Enable/disable indexing application jar annotations
	 */
	public static final String INDEX_APPLICATION_JAR_ANNOTATIONS = "INDEX_APPLICATION_JAR_ANNOTATIONS";
	public static final Boolean INDEX_APPLICATION_JAR_ANNOTATIONS_DEFAULT = false;
	private static boolean indexApplicationJarAnnotationsValue = INDEX_APPLICATION_JAR_ANNOTATIONS_DEFAULT;
	
	public static boolean isApplicationJarAnnotationIndexerEnabled() {
		if(!initialized){
			loadPreferences();
		}
		return indexApplicationJarAnnotationsValue;
	}
	
	/**
	 * Configures whether or not to use indexing of application jar annotation is enabled
	 */
	public static void enableApplicationJarAnnotationIndexer(boolean enabled){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(INDEX_APPLICATION_JAR_ANNOTATIONS, enabled);
		loadPreferences();
	}

	/**
	 * Enable/disable indexing library jar annotations
	 */
	public static final String INDEX_LIBRARY_JAR_ANNOTATIONS = "INDEX_LIBRARY_JAR_ANNOTATIONS";
	public static final Boolean INDEX_LIBRARY_JAR_ANNOTATIONS_DEFAULT = false;
	private static boolean indexLibraryJarAnnotationsValue = INDEX_LIBRARY_JAR_ANNOTATIONS_DEFAULT;
	
	public static boolean isLibraryJarAnnotationIndexerEnabled() {
		if(!initialized){
			loadPreferences();
		}
		return indexLibraryJarAnnotationsValue;
	}
	
	/**
	 * Configures whether or not to use indexing of library jar annotation is enabled
	 */
	public static void enableLibraryJarAnnotationIndexer(boolean enabled){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(INDEX_LIBRARY_JAR_ANNOTATIONS, enabled);
		loadPreferences();
	}

	/**
	 * Enable/disable indexing runtime jar annotations
	 */
	public static final String INDEX_RUNTIME_JAR_ANNOTATIONS = "INDEX_RUNTIME_JAR_ANNOTATIONS";
	public static final Boolean INDEX_RUNTIME_JAR_ANNOTATIONS_DEFAULT = false;
	private static boolean indexRuntimeJarAnnotationsValue = INDEX_RUNTIME_JAR_ANNOTATIONS_DEFAULT;
	
	public static boolean isRuntimeJarAnnotationIndexerEnabled() {
		if(!initialized){
			loadPreferences();
		}
		return indexRuntimeJarAnnotationsValue;
	}
	
	
	/**
	 * Configures whether or not to use indexing of runtime jar annotation is enabled
	 */
	public static void enableRuntimeJarAnnotationIndexer(boolean enabled){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(INDEX_RUNTIME_JAR_ANNOTATIONS, enabled);
		loadPreferences();
	}
	
	/**
	 * Enable/disable using original jars for CFR Decompiler Correspondence view
	 */
	public static final String CFR_CORRESPONDENCE_USE_ORIGINAL_JARS = "CFR_CORRESPONDENCE_USE_ORIGINAL_JARS";
	public static final Boolean CFR_CORRESPONDENCE_USE_ORIGINAL_JARS_DEFAULT = true;
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
		preferences.setDefault(INDEX_APPLICATION_JAR_ANNOTATIONS, INDEX_APPLICATION_JAR_ANNOTATIONS_DEFAULT);
		preferences.setDefault(INDEX_LIBRARY_JAR_ANNOTATIONS, INDEX_LIBRARY_JAR_ANNOTATIONS_DEFAULT);
		preferences.setDefault(INDEX_RUNTIME_JAR_ANNOTATIONS, INDEX_RUNTIME_JAR_ANNOTATIONS_DEFAULT);
		preferences.setDefault(CFR_CORRESPONDENCE_USE_ORIGINAL_JARS, CFR_CORRESPONDENCE_USE_ORIGINAL_JARS_DEFAULT);
		preferences.setDefault(RECOVER_DECOMPILED_LOOPS, RECOVER_DECOMPILED_LOOPS_DEFAULT);
		preferences.setDefault(COMPUTE_LOOP_BOUNDARIES, COMPUTE_LOOP_BOUNDARIES_DEFAULT);
	}
	
	/**
	 * Restores the default preferences
	 */
	public static void restoreDefaults(){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(INDEX_APPLICATION_JAR_ANNOTATIONS, INDEX_APPLICATION_JAR_ANNOTATIONS_DEFAULT);
		preferences.setValue(INDEX_LIBRARY_JAR_ANNOTATIONS, INDEX_LIBRARY_JAR_ANNOTATIONS_DEFAULT);
		preferences.setValue(INDEX_RUNTIME_JAR_ANNOTATIONS, INDEX_RUNTIME_JAR_ANNOTATIONS_DEFAULT);
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
			indexApplicationJarAnnotationsValue = preferences.getBoolean(INDEX_APPLICATION_JAR_ANNOTATIONS);
			indexLibraryJarAnnotationsValue = preferences.getBoolean(INDEX_LIBRARY_JAR_ANNOTATIONS);
			indexRuntimeJarAnnotationsValue = preferences.getBoolean(INDEX_RUNTIME_JAR_ANNOTATIONS);
			cfrCorrespondenceUseOriginalJarsValue = preferences.getBoolean(CFR_CORRESPONDENCE_USE_ORIGINAL_JARS);
			decompiledLoopRecoveryValue = preferences.getBoolean(RECOVER_DECOMPILED_LOOPS);
			computeLoopBoundariesValue = preferences.getBoolean(COMPUTE_LOOP_BOUNDARIES);
		} catch (Exception e){
			Log.warning("Error accessing jimple commons analysis preferences, using defaults...", e);
		}
		initialized = true;
	}
}