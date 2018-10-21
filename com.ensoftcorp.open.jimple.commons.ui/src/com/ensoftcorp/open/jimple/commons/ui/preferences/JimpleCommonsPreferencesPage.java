package com.ensoftcorp.open.jimple.commons.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.ensoftcorp.open.jimple.commons.preferences.JimpleCommonsPreferences;

/**
 * UI for setting jimple commons preferences
 * 
 * @author Ben Holland
 */
public class JimpleCommonsPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final String RECOVER_DECOMPILED_LOOPS_DESCRIPTION = "Recover decompiled loops";
	private static final String COMPUTE_LOOP_BOUNDARIES_DESCRIPTION = "Compute loop boundary conditions";
	private static final String INDEX_APPLICATION_JAR_ANNOTATIONS_DESCRIPTION = "Index application JAR annotations";
	private static final String INDEX_LIBRARY_JAR_ANNOTATIONS_DESCRIPTION = "Index library JAR annotations";
	private static final String INDEX_RUNTIME_JAR_ANNOTATIONS_DESCRIPTION = "Index runtime JAR annotations";
	private static final String CFR_CORRESPONDENCE_USE_ORIGINAL_JARS_DESCRIPTION = "Use original JARs if available in CFR Decompiler Correspondence view";
	
	private static boolean changeListenerAdded = false;
	
	public JimpleCommonsPreferencesPage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		IPreferenceStore preferences = JimpleCommonsPreferences.getPreferenceStore();
		setPreferenceStore(preferences);
		setDescription("Configure preferences for the Jimple Commons Toolbox plugin.");
		
		// use to update cached values if user edits a preference
		if(!changeListenerAdded){
			getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
				@Override
				public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {
					JimpleCommonsPreferences.loadPreferences();
				}
			});
			changeListenerAdded = true;
		}
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(JimpleCommonsPreferences.RECOVER_DECOMPILED_LOOPS, "&" + RECOVER_DECOMPILED_LOOPS_DESCRIPTION, getFieldEditorParent()));
		addField(new BooleanFieldEditor(JimpleCommonsPreferences.COMPUTE_LOOP_BOUNDARIES, "&" + COMPUTE_LOOP_BOUNDARIES_DESCRIPTION, getFieldEditorParent()));
		addField(new BooleanFieldEditor(JimpleCommonsPreferences.INDEX_APPLICATION_JAR_ANNOTATIONS, "&" + INDEX_APPLICATION_JAR_ANNOTATIONS_DESCRIPTION, getFieldEditorParent()));
		addField(new BooleanFieldEditor(JimpleCommonsPreferences.INDEX_LIBRARY_JAR_ANNOTATIONS, "&" + INDEX_LIBRARY_JAR_ANNOTATIONS_DESCRIPTION, getFieldEditorParent()));
		addField(new BooleanFieldEditor(JimpleCommonsPreferences.INDEX_RUNTIME_JAR_ANNOTATIONS, "&" + INDEX_RUNTIME_JAR_ANNOTATIONS_DESCRIPTION, getFieldEditorParent()));
		addField(new BooleanFieldEditor(JimpleCommonsPreferences.CFR_CORRESPONDENCE_USE_ORIGINAL_JARS, "&" + CFR_CORRESPONDENCE_USE_ORIGINAL_JARS_DESCRIPTION, getFieldEditorParent()));
	}

}
