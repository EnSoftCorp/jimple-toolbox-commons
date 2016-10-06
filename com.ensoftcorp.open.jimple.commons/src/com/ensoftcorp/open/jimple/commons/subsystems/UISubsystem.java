package com.ensoftcorp.open.jimple.commons.subsystems;

import com.ensoftcorp.open.commons.subsystems.Subsystem;

public class UISubsystem extends Subsystem {

	public static final String TAG = "UI_SUBSYSTEM";

	@Override
	public String getName() {
		return "User Interface";
	}

	@Override
	public String getDescription() {
		return "User interface libraries";
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
		return new String[] { "java.applet", "java.awt", "java.awt.color", "java.awt.datatransfer", "java.awt.dnd",
				"java.awt.event", "java.awt.font", "java.awt.geom", "java.awt.im", "java.awt.im.spi", "java.awt.image",
				"java.awt.image.renderable", "java.awt.print", "javax.accessibility", "javax.swing",
				"javax.swing.border", "javax.swing.colorchooser", "javax.swing.event", "javax.swing.filechooser",
				"javax.swing.plaf", "javax.swing.plaf.basic", "javax.swing.plaf.metal", "javax.swing.plaf.multi",
				"javax.swing.plaf.nimbus", "javax.swing.plaf.synth", "javax.swing.table", "javax.swing.text",
				"javax.swing.text.html", "javax.swing.text.html.parser", "javax.swing.text.rtf", "javax.swing.tree",
				"javax.swing.undo" };
	}

}
