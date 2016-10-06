package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class NetworkSubsystem extends Subsystem {

	public static final String TAG = "NETWORK_SUBSYSTEM";

	@Override
	public String getName() {
		return "Network";
	}

	@Override
	public String getDescription() {
		return "Network IO libraries";
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
		return new String[] { "java.net", "java.rmi", "java.rmi.activation", "java.rmi.dgc", "java.rmi.registry",
				"java.rmi.server", "javax.net", "javax.net.ssl" };
	}

}