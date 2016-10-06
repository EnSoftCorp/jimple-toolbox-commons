package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class UISubsystem extends Subsystem {

	public static final String TAG = "UI_SUBSYSTEM";

	@Override
	public String getName() {
		return "User Interface";
	}

	@Override
	public String getDescription() {
		return "User interface libraries";
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
