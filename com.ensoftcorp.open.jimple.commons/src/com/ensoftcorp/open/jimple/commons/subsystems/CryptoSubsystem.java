package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class CryptoSubsystem extends Subsystem {

	public static final String TAG = "CRYPTO_SUBSYSTEM";

	@Override
	public String getName() {
		return "Cryptography";
	}

	@Override
	public String getDescription() {
		return "Cryptography libraries";
	}

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public String[] getParentTags() {
		return new String[] { MathSubsystem.TAG, SecuritySubsystem.TAG };
	}

	@Override
	public String[] getNamespaces() {
		return new String[] { "javax.crypto", "javax.crypto.interfaces", "javax.crypto.spec" };
	}
}
