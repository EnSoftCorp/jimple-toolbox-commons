package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class LogSubsystem extends Subsystem {

	public static final String TAG = "LOG_SUBSYSTEM";

	@Override
	public String getName() {
		return "Log";
	}

	@Override
	public String getDescription() {
		return "Logging libraries";
	}

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public String[] getParentTags() {
		return new String[] { IOSubsystem.TAG };
	}

	@Override
	public String[] getNamespaces() {
		return new String[] { "java.util.logging" };
	}

}
