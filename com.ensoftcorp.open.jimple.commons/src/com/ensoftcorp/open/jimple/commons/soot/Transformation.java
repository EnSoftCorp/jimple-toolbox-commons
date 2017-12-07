package com.ensoftcorp.open.jimple.commons.soot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;

import com.ensoftcorp.abp.common.soot.ConfigManager;
import com.ensoftcorp.open.jimple.commons.log.Log;

import soot.G;
import soot.PackManager;
import soot.SootClass;
import soot.Transform;
import soot.util.Chain;

public class Transformation {

	/**
	 * 
	 * Transforms the given jar.
	 * 
	 * Assumes only the default JDK is on the classpath.
	 * Assumes phantom references are not allowed, original names should be preserved, bytecode will be generated.
	 * 
	 * @param jar The input jar
	 * @param outputJar The transformed jar
	 * @param transforms A series of transforms to be applied to the input jar
	 * @throws SootConversionException
	 * @throws IOException
	 */
	public static void transform(File jar, File outputJar, Transform... transforms) throws SootConversionException, IOException {
		boolean allowPhantomReferences = false; // compile in strict mode
		boolean useOriginalNames = true; // preserve original local variable names (as best we can)
		boolean outputBytecode = true; // compile Jimple to class files
		transform(jar, outputJar, allowPhantomReferences, useOriginalNames, outputBytecode, transforms);
	}
	
	/**
	 * 
	 * Transforms the given jar.
	 * 
	 * Assumes only the default JDK is on the classpath.
	 * 
	 * @param jar The input jar
	 * @param outputJar The transformed jar
	 * @param allowPhantomReferences Relaxes classpath requirements
	 * @param useOriginalNames A best effort attempt to preserve original names of local variables
	 * @param outputBytecode If false outputs jimple files representing class files (good for debugging transforms)
	 * @param transforms A series of transforms to be applied to the input jar
	 * @throws SootConversionException
	 * @throws IOException
	 */
	public static void transform(File jar, File outputJar, boolean allowPhantomReferences, boolean useOriginalNames, boolean outputBytecode, Transform... transforms) throws SootConversionException, IOException {
		// add the default JVM classpath (assuming translator uses the same jvm libraries)
		List<IClasspathEntry> classpathEntries = new ArrayList<IClasspathEntry>();
		IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		LibraryLocation[] locations = JavaRuntime.getLibraryLocations(vmInstall);
		for (LibraryLocation library : locations) {
			classpathEntries.add(JavaCore.newLibraryEntry(library.getSystemLibraryPath(), null, null));
		}
		
		// convert classpath entries to files
		List<File> libraryPaths = new ArrayList<File>(classpathEntries.size());
		for(IClasspathEntry entry : classpathEntries){
			libraryPaths.add(new File(entry.getPath().toFile().getCanonicalPath()));
		}
		
		transform(jar, outputJar, libraryPaths, allowPhantomReferences, useOriginalNames, outputBytecode, transforms);
	}
	
	/**
	 * Transforms the given jar
	 * @param jar The input jar
	 * @param outputJar The transformed jar
	 * @param classpathEntries A list of library dependencies
	 * @param allowPhantomReferences Relaxes classpath requirements
	 * @param useOriginalNames A best effort attempt to preserve original names of local variables
	 * @param outputBytecode If false outputs jimple files representing class files (good for debugging transforms)
	 * @param transforms A series of transforms to be applied to the input jar
	 * @throws SootConversionException
	 * @throws IOException
	 */
	public static void transform(File jar, File outputJar, List<File> classpathEntries, boolean allowPhantomReferences, boolean useOriginalNames, boolean outputBytecode, Transform... transforms) throws SootConversionException, IOException {
		// make sure there is a directory to write the output to
		File outputDirectory = outputJar.getParentFile();
		if(!outputDirectory.exists()){
			outputDirectory.mkdirs();
		}

		StringBuilder classpath = new StringBuilder();
		for(File entry : classpathEntries){
			classpath.append(entry.getAbsolutePath());
			classpath.append(File.pathSeparator);
		}
		
		// configure soot arguments
		ArrayList<String> argList = new ArrayList<String>();
		argList.add("-src-prec"); argList.add("class");
		argList.add("-cp"); argList.add(classpath.toString());
		
		// phantom references allow deal with issues of an incomplete classpath
		if(allowPhantomReferences){
			argList.add("-allow-phantom-refs");
		}
		
		// where to read/write input and outputs
		argList.add("-process-dir"); argList.add(jar.getAbsolutePath());
		argList.add("-include-all");
		
		// use original names
		if(useOriginalNames){
			argList.add("-p"); argList.add("jb"); argList.add("use-original-names");
		}
		
		// be deterministic about variable name assignment
		argList.add("--p");argList.add("jb");argList.add("stabilize-local-names:true");
		
		// output class or jimple files (jimple files are good for debugging)
		argList.add("-output-format"); argList.add(outputBytecode ? "class" : "jimple");
        
        // this may be used to forcible generate potentially invalid bytecode
//      argList.add("--p"); argList.add("jb.tr"); argList.add("ignore-wrong-staticness:true");
        
		// TODO: options to consider
//      argList.add("-keep-bytecode-offset");
//      argList.add("-keep-line-number");
		
		// need to specifically enalbe using ASM over deprecated Jasmine library
		argList.add("-asm-backend");
		
		// output classes to a jar file
		argList.add("-output-dir"); argList.add(outputJar.getCanonicalPath()); argList.add("-output-jar");
		
		String[] sootArgs = argList.toArray(new String[argList.size()]);

		// run soot to compile jimple
		try {
			ConfigManager.getInstance().startTempConfig();
			G.reset();
			
			// register all the bytecode transformations to perform
			if(transforms != null){
				for(Transform transform : transforms){
					// add the transform to the jimple transformation pack
					PackManager.v().getPack("jtp").add(transform);
				}
			}
			
			// run soot
			soot.Main.v().run(sootArgs);
			
			// debug
//			Log.info("Transformed Jar: " + outputJar.getCanonicalPath());
		} catch (Throwable t){
			String message = "An error occurred transforming Jar.";
			if(!outputBytecode){
				message = "An error occurred while transforming Jimple files.";
			}
			SootConversionException error = new SootConversionException(message, t);
			Log.error(message + "\n\nSoot Arguments: " + Arrays.toString(sootArgs), error);
			throw error;
		} finally {
			// restore the saved config (even if there was an error)
            ConfigManager.getInstance().endTempConfig();
		}

		// warn about any phantom references
		Chain<SootClass> phantomClasses = soot.Scene.v().getPhantomClasses();
        if (!phantomClasses.isEmpty()) {
            TreeSet<String> missingClasses = new TreeSet<String>();
            for (SootClass sootClass : phantomClasses) {
                    missingClasses.add(sootClass.toString());
            }
            StringBuilder message = new StringBuilder();
            message.append("When transforming Jar, some classes were referenced that could not be found.\n\n");
            for (String sootClass : missingClasses) {
                    message.append(sootClass);
                    message.append("\n");
            }
            Log.warning(message.toString());
        }
	}

}
