package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class DataStructureSubsystem extends Subsystem {

	public static final String TAG = "DATA_STRUCTURE_SUBSYSTEM";

	@Override
	public String getName() {
		return "Data Structure";
	}

	@Override
	public String getDescription() {
		return "Common collection-like data structures";
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
		return new String[] { "java.beans", "java.beans.beancontext", "java.text", "java.text.spi" };
	}
}
