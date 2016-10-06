package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class RMISubsystem extends Subsystem {

	public static final String TAG = "RMI_SUBSYSTEM";

	@Override
	public String getName() {
		return "Remote Method Invocation";
	}

	@Override
	public String getDescription() {
		return "Remote method invocation libraries";
	}

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public String[] getParentTags() {
		return new String[] { NetworkSubsystem.TAG };
	}

	@Override
	public String[] getNamespaces() {
		return new String[] { "javax.activity", "javax.management.remote.rmi", "javax.rmi", "javax.rmi.CORBA",
				"javax.rmi.ssl", "javax.transaction", "javax.transaction.xa", "org.omg.CORBA", "org.omg.CORBA_2_3",
				"org.omg.CORBA_2_3.portable", "org.omg.CORBA.DynAnyPackage", "org.omg.CORBA.ORBPackage",
				"org.omg.CORBA.portable", "org.omg.CORBA.TypeCodePackage", "org.omg.CosNaming",
				"org.omg.CosNaming.NamingContextExtPackage", "org.omg.CosNaming.NamingContextPackage",
				"org.omg.Dynamic", "org.omg.DynamicAny", "org.omg.DynamicAny.DynAnyFactoryPackage",
				"org.omg.DynamicAny.DynAnyPackage", "org.omg.IOP", "org.omg.IOP.CodecFactoryPackage",
				"org.omg.IOP.CodecPackage", "org.omg.Messaging", "org.omg.PortableInterceptor",
				"org.omg.PortableInterceptor.ORBInitInfoPackage", "org.omg.PortableServer",
				"org.omg.PortableServer.CurrentPackage", "org.omg.PortableServer.POAManagerPackage",
				"org.omg.PortableServer.POAPackage", "org.omg.PortableServer.portable",
				"org.omg.PortableServer.ServantLocatorPackage", "org.omg.SendingContext", "org.omg.stub.java.rmi" };
	}

}
