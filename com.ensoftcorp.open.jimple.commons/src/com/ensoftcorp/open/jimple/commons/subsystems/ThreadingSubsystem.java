package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class ThreadingSubsystem extends Subsystem {

	public static final String TAG = "THREADING_SUBSYSTEM";

	@Override
	public String getName() {
		return "Threading";
	}

	@Override
	public String getDescription() {
		return "Threading libraries";
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
		return new String[] { "java.util.concurrent", "java.util.concurrent.atomic", "java.util.concurrent.locks" };
	}

}
