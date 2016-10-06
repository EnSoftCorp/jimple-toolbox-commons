package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class TestingSubsystem extends Subsystem {

	public static final String TAG = "TESTING_SUBSYSTEM";

	@Override
	public String getName() {
		return "Testing";
	}

	@Override
	public String getDescription() {
		return "Java mock interface and test libraries";
	}

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public String[] getParentTags() {
		return new String[] { JavaCoreSubsystem.TAG };
	}

	@Override
	public String[] getNamespaces() {
		// TODO: implement
		return new String[] {};
	}

}
