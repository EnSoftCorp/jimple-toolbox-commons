package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class IOSubsystem extends Subsystem {

	public static final String TAG = "IO_SUBSYSTEM";

	@Override
	public String getName() {
		return "Input/Output";
	}

	@Override
	public String getDescription() {
		return "General input/output";
	}

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public String[] getParentTags() {
		return new String[] { JavaCoreSubsystem.TAG, HardwareSubsystem.TAG };
	}

	@Override
	public String[] getNamespaces() {
		// TODO: implement
		return new String[] {};
	}

}
