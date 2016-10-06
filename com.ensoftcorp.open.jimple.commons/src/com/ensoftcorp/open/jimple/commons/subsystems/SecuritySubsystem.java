package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class SecuritySubsystem extends Subsystem {

	public static final String TAG = "SECURITY_SUBSYSTEM";

	@Override
	public String getName() {
		return "Security";
	}

	@Override
	public String getDescription() {
		return "Security libraries";
	}

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public String[] getNamespaces() {
		// TODO: implement
		return new String[] { Subsystem.ROOT_SUBSYSTEM_TAG };
	}

}
