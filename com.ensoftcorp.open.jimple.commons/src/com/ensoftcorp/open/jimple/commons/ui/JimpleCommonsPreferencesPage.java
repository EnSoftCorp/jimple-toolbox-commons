package com.ensoftcorp.open.jimple.commons.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.ensoftcorp.open.jimple.commons.Activator;
import com.ensoftcorp.open.jimple.commons.preferences.JimpleCommonsPreferences;

/**
 * UI for setting jimple commons preferences
 * 
 * @author Ben Holland
 */
public class JimpleCommonsPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final String RECOVER_DECOMPILED_LOOPS_DESCRIPTION = "Recover decompiled loops";
	
	private static boolean changeListenerAdded = false;
	
	public JimpleCommonsPreferencesPage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
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
	}

}
