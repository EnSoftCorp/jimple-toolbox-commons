package com.ensoftcorp.open.jimple.commons.soot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;

import com.ensoftcorp.abp.common.soot.ConfigManager;
import com.ensoftcorp.abp.common.util.JimpleUtil;
import com.ensoftcorp.abp.common.util.JimpleUtil.JimpleSource;
import com.ensoftcorp.open.jimple.commons.log.Log;

import soot.G;
import soot.SootClass;
import soot.util.Chain;

public class Decompilation {
	
	public static final String JIMPLE_USE_ORIGINAL_NAMES = "JimpleUseOriginalNames";
	public static final String JIMPLE_STABALIZE_LOCAL_NAMES = "JimpleStabalizeLocalNames";
	public static final String JIMPLE_ALLOW_PHANTOM_REFERENCES = "JimpleAllowPhantomReferences";
	
	/**
	 * Converts a jar to jimple using the default classpath
	 * @param jar
	 * @param outputDirectory
	 * @throws SootConversionException
	 * @throws IOException
	 */
	public static void decompile(File jar, File outputDirectory) throws SootConversionException, IOException {
		decompile(jar, outputDirectory, true, false);
	}
	
	/**
	 * Converts a jar file to jimple using the default classpath
	 * @param jar
	 * @param outputDirectory
	 * @param allowPhantomReferences
	 * @param useOriginalNames
	 * @throws IOException
	 * @throws SootConversionException 
	 */
	public static void decompile(File jar, File outputDirectory, boolean allowPhantomReferences, boolean useOriginalNames) throws SootConversionException, IOException {
		// add the default JVM classpath (assuming translator uses the same jvm libraries)
		List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
		IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		LibraryLocation[] locations = JavaRuntime.getLibraryLocations(vmInstall);
		for (LibraryLocation library : locations) {
			entries.add(JavaCore.newLibraryEntry(library.getSystemLibraryPath(), null, null));
		}
		
		decompile(jar, outputDirectory, entries, allowPhantomReferences, useOriginalNames);
	}
	
	/**
	 * Converts a jar file to jimple
	 * @param projectDirectory
	 * @param jar
	 * @param outputDirectory
	 * @param classpathEntries
	 * @throws SootConversionException
	 * @throws IOException
	 */
	public static void decompile(File jar, File outputDirectory, List<IClasspathEntry> classpathEntries, boolean allowPhantomReferences, boolean useOriginalNames) throws SootConversionException, IOException {
		if(!outputDirectory.exists()){
			outputDirectory.mkdirs();
		}
		ConfigManager.getInstance().startTempConfig();
		try {
			G.reset();
		
			StringBuilder classpath = new StringBuilder();
			for(IClasspathEntry entry: classpathEntries){
				classpath.append(entry.getPath().toFile().getCanonicalPath());
				classpath.append(File.pathSeparator);
			}

			ArrayList<String> argList = new ArrayList<String>();
			argList.add("-src-prec"); argList.add("class");
			argList.add("--xml-attributes");
			argList.add("-output-format"); argList.add("jimple");
			argList.add("-cp"); argList.add(classpath.toString());
			if(allowPhantomReferences){
				argList.add("-allow-phantom-refs");
			}
			argList.add("-output-dir"); argList.add(outputDirectory.getAbsolutePath());
			argList.add("-process-dir"); argList.add(jar.getAbsolutePath());
			argList.add("-include-all");
			
			// use original names
			if(useOriginalNames){
				argList.add("-p"); argList.add("jb"); argList.add("use-original-names");
			}
			argList.add("--p");argList.add("jb");argList.add("stabilize-local-names:true");
			
			String[] args = argList.toArray(new String[argList.size()]);
			
			try {
				soot.Main.main(args);
				JimpleUtil.writeHeaderFile(JimpleSource.JAR, jar.getAbsolutePath(), outputDirectory.getAbsolutePath());
				
				// warn about any phantom references
				Chain<SootClass> phantomClasses = soot.Scene.v().getPhantomClasses();
                if (!phantomClasses.isEmpty()) {
                        TreeSet<String> missingClasses = new TreeSet<String>();
                        for (SootClass sootClass : phantomClasses) {
                                missingClasses.add(sootClass.toString());
                        }
                        StringBuilder message = new StringBuilder();
                        message.append("Some classes were referenced, but could not be found.\n\n");
                        for (String sootClass : missingClasses) {
                                message.append(sootClass);
                                message.append("\n");
                        }
                        Log.warning(message.toString());
                }
			} catch (RuntimeException e) {
				throw new SootConversionException(e);
			}
		} finally {
			// restore the saved config (even if there was an error)
            ConfigManager.getInstance().endTempConfig();
		}
	}
	
}
