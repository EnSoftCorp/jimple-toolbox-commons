package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class SerializationSubsystem extends Subsystem {

	public static final String TAG = "SERIALIZATION_SUBSYSTEM";

	@Override
	public String getName() {
		return "Serialization";
	}

	@Override
	public String getDescription() {
		return "Serialization libraries";
	}

	@Override
	public String getTag() {
		return TAG;
	}

}
