package com.ensoftcorp.open.jimple.commons.handlers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

public class JarToJimpleConfigurationsDialog extends Dialog {

	protected Shell shell;
	
	private boolean cancelled = false;
	private JimpleGenerationConfigurations result;
	private Button allowPhantomReferencesCheckbox;
	private Button useOriginalNamesCheckbox;
	private Button stabilizeNamesCheckbox;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public JarToJimpleConfigurationsDialog(Shell shell) {
		super(shell, SWT.TITLE | SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		setText("Jar To Jimple Configurations");
	}

	public static class JimpleGenerationConfigurations {
		private boolean allowPhantomReferences;
		private boolean useOriginalNames;
		private boolean stabilizeNames;
		
		private JimpleGenerationConfigurations(boolean allowPhantomReferences, boolean useOriginalNames, boolean stabilizeNames) {
			this.allowPhantomReferences = allowPhantomReferences;
			this.useOriginalNames = useOriginalNames;
			this.stabilizeNames = stabilizeNames;
		}
	
		public boolean isAllowPhantomReferencesEnabled() {
			return allowPhantomReferences;
		}
		
		public boolean isUseOriginalNamesEnabled() {
			return useOriginalNames;
		}
		
		public boolean isStabilizeNamesEnabled(){
			return stabilizeNames;
		}
	}
	
	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open(boolean allowPhantomReferences, boolean useOriginalNames, boolean stabilizeNames) {
		result = new JimpleGenerationConfigurations(allowPhantomReferences, useOriginalNames, stabilizeNames);
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		if(cancelled){
			return null;
		} else {
			return result;
		}
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), getStyle());
		shell.setSize(350, 150);
		shell.setText(getText());
		
		// center dialog
		Monitor monitor = shell.getMonitor();
	    Rectangle bounds = monitor.getBounds();
	    Rectangle rect = shell.getBounds();
	    int x = bounds.x + (bounds.width - rect.width) / 2;
	    int y = bounds.y + (bounds.height - rect.height) / 2;
	    shell.setLocation(x, y);
		
		shell.setLayout(new GridLayout(1, false));
		
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label allowPhantomReferencesLabel = new Label(composite, SWT.NONE);
		allowPhantomReferencesLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		allowPhantomReferencesLabel.setText("Allow Phantom References: ");
		allowPhantomReferencesCheckbox = new Button(composite, SWT.CHECK);
		allowPhantomReferencesCheckbox.setSelection(result.allowPhantomReferences);
		allowPhantomReferencesCheckbox.setText("");
		allowPhantomReferencesCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				result.allowPhantomReferences = allowPhantomReferencesCheckbox.getSelection();
			}
		});
		
		Label useOriginalNamesLabel = new Label(composite, SWT.NONE);
		useOriginalNamesLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		useOriginalNamesLabel.setText("Use Original Names: ");
		useOriginalNamesCheckbox = new Button(composite, SWT.CHECK);
		useOriginalNamesCheckbox.setSelection(result.useOriginalNames);
		useOriginalNamesCheckbox.setText("");
		useOriginalNamesCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				result.useOriginalNames = useOriginalNamesCheckbox.getSelection();
			}
		});
		
		Label stabilizeNamesLabel = new Label(composite, SWT.NONE);
		stabilizeNamesLabel.setEnabled(false);
		stabilizeNamesLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		stabilizeNamesLabel.setText("Stabilize Names: ");
		stabilizeNamesCheckbox = new Button(composite, SWT.CHECK);
		stabilizeNamesCheckbox.setSelection(result.stabilizeNames);
		stabilizeNamesCheckbox.setEnabled(false);
		stabilizeNamesCheckbox.setText("");
		stabilizeNamesCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				result.stabilizeNames = stabilizeNamesCheckbox.getSelection();
			}
		});
		
		Composite actionsComposite = new Composite(shell, SWT.NONE);
		actionsComposite.setLayout(new GridLayout(2, false));
		actionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Button cancelButton = new Button(actionsComposite, SWT.NONE);
		cancelButton.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true, true, 1, 1));
		cancelButton.setText("Cancel");
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cancelled = true;
				shell.close();
			}
		});
		
		Button generateJimpleButton = new Button(actionsComposite, SWT.NONE);
		generateJimpleButton.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, true, 1, 1));
		generateJimpleButton.setText("Generate Jimple");
		
		generateJimpleButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});

	}

}
