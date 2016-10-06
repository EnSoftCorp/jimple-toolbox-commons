package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class CompressionSubsystem extends Subsystem {

	public static final String TAG = "COMPRESSION_SUBSYSTEM";

	@Override
	public String getName() {
		return "Compression";
	}

	@Override
	public String getDescription() {
		return "Compression libraries";
	}

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public String[] getParentTags() {
		return new String[] { SerializationSubsystem.TAG };
	}

	@Override
	public String[] getNamespaces() {
		return new String[] { "java.util.jar", "java.util.zip" };
	}
}
