package com.ensoftcorp.open.jimple.commons.analysis;

import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;

public class SetDefinitions {

	// hide constructor
	private SetDefinitions() {}

	/**
	 * A collection of specifically just the JDK libraries
	 * @return
	 */
	public static Q JDKLibraries(){
		return com.ensoftcorp.open.java.commons.analysis.SetDefinitions.JDKLibraries(); 
	}
	
	/**
	 * Types which represent arrays of other types
	 * 
	 * NOTE: These nodes are NOT declared by anything. They are outside of any
	 * project.
	 */
	public static Q arrayTypes() {
		return com.ensoftcorp.open.java.commons.analysis.SetDefinitions.arrayTypes();
	}

	/**
	 * Types which represent language primitive types
	 * 
	 * NOTE: These nodes are NOT declared by anything. They are outside of any
	 * project.
	 */
	public static Q primitiveTypes() {
		return com.ensoftcorp.open.java.commons.analysis.SetDefinitions.primitiveTypes();
	}

	/**
	 * Everything declared under any of the known API projects, if they are in
	 * the index.
	 */
	public static Q libraries() {
		return com.ensoftcorp.open.java.commons.analysis.SetDefinitions.libraries();
	}
	
	/**
	 * Everything in the universe which is part of the app (not part of the
	 * libraries, or any "floating" nodes).
	 * 
	 * For Jimple everything can appear under a library, so we define the application with respect to the JDK libraries
	 */
	public static Q app() {
		return Common.universe().difference(JDKLibraries().contained(), SetDefinitions.primitiveTypes(), SetDefinitions.arrayTypes());
	}
	
}
