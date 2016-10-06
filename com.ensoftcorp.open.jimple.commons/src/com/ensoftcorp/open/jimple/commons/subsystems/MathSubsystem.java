package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class MathSubsystem extends Subsystem {

	public static final String TAG = "MATH_SUBSYSTEM";

	@Override
	public String getName() {
		return "Math";
	}

	@Override
	public String getDescription() {
		return "Math libraries";
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
