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

	@Override
	public String[] getParentTags() {
		return new String[] { IOSubsystem.TAG };
	}

	@Override
	public String[] getNamespaces() {
		return new String[] { "java.util.prefs", "javax.activation", "javax.jws", "javax.jws.soap", "javax.xml",
				"javax.xml.bind", "javax.xml.bind.annotation", "javax.xml.bind.annotation.adapters",
				"javax.xml.bind.attachment", "javax.xml.bind.helpers", "javax.xml.bind.util", "javax.xml.crypto",
				"javax.xml.crypto.dom", "javax.xml.crypto.dsig", "javax.xml.crypto.dsig.dom",
				"javax.xml.crypto.dsig.keyinfo", "javax.xml.crypto.dsig.spec", "javax.xml.datatype",
				"javax.xml.namespace", "javax.xml.parsers", "javax.xml.soap", "javax.xml.stream",
				"javax.xml.stream.events", "javax.xml.stream.util", "javax.xml.transform", "javax.xml.transform.dom",
				"javax.xml.transform.sax", "javax.xml.transform.stax", "javax.xml.transform.stream",
				"javax.xml.validation", "javax.xml.ws", "javax.xml.ws.handler", "javax.xml.ws.handler.soap",
				"javax.xml.ws.http", "javax.xml.ws.soap", "javax.xml.ws.spi", "javax.xml.ws.spi.http",
				"javax.xml.ws.wsaddressing", "javax.xml.xpath", "org.w3c.dom", "org.w3c.dom.bootstrap",
				"org.w3c.dom.events", "org.w3c.dom.ls", "org.xml.sax", "org.xml.sax.ext", "org.xml.sax.helpers" };
	}

}
