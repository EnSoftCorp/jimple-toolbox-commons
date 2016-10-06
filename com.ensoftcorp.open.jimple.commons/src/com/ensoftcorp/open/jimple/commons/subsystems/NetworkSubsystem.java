package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class NetworkSubsystem extends Subsystem {

	public static final String TAG = "NETWORK_SUBSYSTEM";

	@Override
	public String getName() {
		return "Network";
	}

	@Override
	public String getDescription() {
		return "Network IO libraries";
	}

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public String[] getParentTags() {
		return new String[] { SerializationSubsystem.TAG };
	}

	@Override
	public String[] getNamespaces() {
		// TODO: implement
		return new String[] {};
	}

}