package com.ensoftcorp.open.jimple.commons.project;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFolder;

import com.ensoftcorp.open.java.commons.project.ProjectJarProperties.Jar;

public class ProjectJarJimpleProperties {

	private static final String JAR_JIMPLE_PROPERTIES_JIMPLE_DIRECTORY_PATH = "jimple-path";
	private static final String JAR_JIMPLE_PROPERTIES_PHANTOM_REFERENCES = "jimple-allow-phantom-references";
	private static final String JAR_JIMPLE_PROPERTIES_USE_ORIGINAL_NAMES = "jimple-use-original-names";
	
	private Jar jar;
	private IFolder jimpleDirectory;
	boolean allowPhantomReferences;
	boolean useOriginalNames;
	
	public ProjectJarJimpleProperties(Jar jar, IFolder jimpleDirectory, boolean allowPhantomReferences, boolean useOriginalNames) {
		this.jar = jar;
		this.jimpleDirectory = jimpleDirectory;
		this.allowPhantomReferences = allowPhantomReferences;
		this.useOriginalNames = useOriginalNames;
	}
	
	public Jar getJar() {
		return jar;
	}
	
	public IFolder getJimpleDirectory() {
		return jimpleDirectory;
	}
	
	public Boolean isAllowPhantomReferencesEnabled() {
		return allowPhantomReferences;
	}
	
	public Boolean isUseOriginalNamesEnabled() {
		return useOriginalNames;
	}
	
	public static void setJimpleProperties(ProjectJarJimpleProperties jimpleProperties) throws Exception {
		Map<String,String> attributes = new HashMap<String,String>();
		attributes.put(JAR_JIMPLE_PROPERTIES_JIMPLE_DIRECTORY_PATH, jimpleProperties.getJimpleDirectory().getProjectRelativePath().toPortableString());
		attributes.put(JAR_JIMPLE_PROPERTIES_PHANTOM_REFERENCES, jimpleProperties.isAllowPhantomReferencesEnabled().toString());
		attributes.put(JAR_JIMPLE_PROPERTIES_USE_ORIGINAL_NAMES, jimpleProperties.isUseOriginalNamesEnabled().toString());
		jimpleProperties.getJar().setJarAttributes(attributes);
	}
	
}
