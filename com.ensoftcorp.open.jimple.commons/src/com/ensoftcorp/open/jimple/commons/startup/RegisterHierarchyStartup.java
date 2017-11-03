package com.ensoftcorp.open.jimple.commons.startup;

import org.eclipse.ui.IStartup;

import com.ensoftcorp.open.jimple.commons.loops.DecompiledLoopIdentification;

public class RegisterHierarchyStartup implements IStartup {

	@Override
	public void earlyStartup() {
		DecompiledLoopIdentification.registerHierarchy();
	}

}
