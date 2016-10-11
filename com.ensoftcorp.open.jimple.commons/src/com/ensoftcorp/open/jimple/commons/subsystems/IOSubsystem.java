package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class IOSubsystem extends Subsystem {

	public static final String TAG = "IO_SUBSYSTEM";

	@Override
	public String getName() {
		return "Input/Output";
	}

	@Override
	public String getDescription() {
		return "General input/output";
	}

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public String[] getParentTags() {
		return new String[] { JavaCoreSubsystem.TAG, HardwareSubsystem.TAG };
	}

	@Override
	public String[] getNamespaces() {
		return new String[] { "java.io", "java.nio", "java.nio.channels", "java.nio.channels.spi", "java.nio.charset",
				"java.nio.charset.spi", "java.nio.file", "java.nio.file.attribute", "java.nio.file.spi",
				"javax.imageio", "javax.imageio.event", "javax.imageio.metadata", "javax.imageio.plugins.bmp",
				"javax.imageio.plugins.jpeg", "javax.imageio.spi", "javax.imageio.stream", "javax.print",
				"javax.print.attribute", "javax.print.attribute.standard", "javax.print.event" };
	}

}