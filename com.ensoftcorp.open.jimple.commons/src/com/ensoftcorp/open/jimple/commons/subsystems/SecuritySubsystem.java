package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class SecuritySubsystem extends Subsystem {

	public static final String TAG = "SECURITY_SUBSYSTEM";

	@Override
	public String getName() {
		return "Security";
	}

	@Override
	public String getDescription() {
		return "Security libraries";
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
		return new String[] { "java.security", "java.security.acl", "java.security.cert", "java.security.interfaces",
				"java.security.spec", "javax.naming", "javax.naming.directory", "javax.naming.event",
				"javax.naming.ldap", "javax.naming.spi", "javax.security.auth", "javax.security.auth.callback",
				"javax.security.auth.kerberos", "javax.security.auth.login", "javax.security.auth.spi",
				"javax.security.auth.x500", "javax.security.cert", "javax.security.sasl", "org.ietf.jgss" };
	}

}
