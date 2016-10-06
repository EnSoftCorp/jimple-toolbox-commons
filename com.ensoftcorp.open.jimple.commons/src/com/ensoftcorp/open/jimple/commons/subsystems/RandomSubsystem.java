package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class RandomSubsystem extends Subsystem {

	public static final String TAG = "RANDOM_SUBSYSTEM";

	@Override
	public String getName() {
		return "Random";
	}

	@Override
	public String getDescription() {
		return "Random libraries";
	}

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public String[] getParentTags() {
		return new String[] { MathSubsystem.TAG };
	}

	@Override
	public String[] getNamespaces() {
		// TODO: implement
		return new String[] {};
	}
}
