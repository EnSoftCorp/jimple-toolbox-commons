package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class GarbageCollectionSubsystem extends Subsystem {

	public static final String TAG = "GARBAGE_COLLECTION_SUBSYSTEM";

	@Override
	public String getName() {
		return "Garbage Collection";
	}

	@Override
	public String getDescription() {
		return "Garbage collection libraries";
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
		return new String[] { "java.lang.ref" };
	}

}
