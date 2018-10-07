package com.ensoftcorp.open.jimple.commons.analysis;

import org.eclipse.core.resources.IProject;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.query.Query;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.commons.utilities.WorkspaceUtils;
import com.ensoftcorp.open.java.commons.project.ProjectJarProperties;
import com.ensoftcorp.open.java.commons.project.ProjectJarProperties.Jar;
import com.ensoftcorp.open.jimple.commons.log.Log;

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
		AtlasSet<Node> result = new AtlasHashSet<Node>();
		AtlasSet<Node> libraries = com.ensoftcorp.open.java.commons.analysis.SetDefinitions.libraries().eval().nodes();
		for(Node projectNode : Query.universe().nodes(XCSG.Project).eval().nodes()) {
			try {
				IProject project = WorkspaceUtils.getProject(projectNode.getAttr(XCSG.name).toString());
				for(Jar jar : ProjectJarProperties.getLibraryJars(project)) {
					for(Node library : libraries) {
						if(library.getAttr(XCSG.name).toString().endsWith(jar.getFile().getName())) {
							result.add(library);
						}
					}
				}
			} catch (Exception e) {
				Log.warning("Error accessing project jar properties", e);
			}
		}
		return Common.toQ(result);
	}
	
	/**
	 * Everything in the universe which is part of the app (not part of the
	 * libraries, or any "floating" nodes).
	 * 
	 * For Jimple everything can appear under a library, so we define the application with respect to the JDK libraries
	 */
	public static Q app() {
		return Query.universe().difference(JDKLibraries().contained(), libraries().contained(), SetDefinitions.primitiveTypes(), SetDefinitions.arrayTypes());
	}
	
}
