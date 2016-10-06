package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class RMISubsystem extends Subsystem {

	public static final String TAG = "RMI_SUBSYSTEM";

	@Override
	public String getName() {
		return "Remote Method Invocation";
	}

	@Override
	public String getDescription() {
		return "Remote method invocation libraries";
	}

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public String[] getParentTags() {
		return new String[] { NetworkSubsystem.TAG };
	}

	@Override
	public String[] getNamespaces() {
		// TODO: implement
		return new String[] {};
	}

}
