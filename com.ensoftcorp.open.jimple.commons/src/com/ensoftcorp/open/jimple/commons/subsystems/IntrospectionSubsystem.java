package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class IntrospectionSubsystem extends Subsystem {

	public static final String TAG = "INTROSPECTION_SUBSYSTEM";

	@Override
	public String getName() {
		return "Introspection";
	}

	@Override
	public String getDescription() {
		return "Reflection and runtime libraries";
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
