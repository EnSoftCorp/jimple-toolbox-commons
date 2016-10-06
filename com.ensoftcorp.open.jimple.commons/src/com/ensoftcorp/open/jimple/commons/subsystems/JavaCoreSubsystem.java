package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class JavaCoreSubsystem extends Subsystem {

	public static final String TAG = "JAVACORE_SUBSYSTEM";

	@Override
	public String getName() {
		return "Java Core";
	}

	@Override
	public String getDescription() {
		return "Java core language libraries";
	}

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public String[] getParentTags() {
		return new String[] { Subsystem.ROOT_SUBSYSTEM_TAG };
	}

	@Override
	public String[] getNamespaces() {
		// TODO: implement
		return new String[] {};
	}

}
