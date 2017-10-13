package com.ensoftcorp.open.jimple.commons.transform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.ensoftcorp.abp.common.soot.ConfigManager;
import com.ensoftcorp.open.commons.utilities.WorkspaceUtils;
import com.ensoftcorp.open.jimple.commons.log.Log;

import soot.G;
import soot.PackManager;
import soot.SootClass;
import soot.Transform;
import soot.util.Chain;

public class Compilation {

	/**
	 * Compiles a Jimple in a project to an output JAR file
	 * @param projectName The name of the project in the workspace to compile
	 * @param outputJar The location to output the resulting jar
	 * @throws IOException
	 * @throws CoreException 
	 */
	public static void compile(String projectName, File outputJar) throws IOException, CoreException {
		compile(WorkspaceUtils.getProject(projectName), null, outputJar);
	}
	
	/**
	 * Compiles a Jimple in a project to an output JAR file
	 * @param project The project to compile
	 * @param outputJar The location to output the resulting jar
	 * @throws IOException
	 * @throws CoreException 
	 */
	public static void compile(IProject project, File outputJar) throws IOException, CoreException {
		compile(project, null, outputJar);
	}
	
	/**
	 * Compiles a Jimple in a project to an output JAR file
	 * @param project The project to compile
	 * @param jimpleDirectory The directory path containing the jimple source (example: "sootOutput" or "WEB-INF/jimple"), otherwise null
	 * @param outputJar The location to output the resulting jar
	 * @throws IOException
	 * @throws CoreException 
	 */
	public static void compile(IProject project, File jimpleDirectory, File outputJar) throws IOException, CoreException {
		compile(project, jimpleDirectory, outputJar, new Transform[]{});
	}
	
	/**
	 * Compiles a Jimple in a project to an output JAR file
	 * @param project The project to compile
	 * @param jimpleDirectory The directory path containing the jimple source (example: "sootOutput" or "WEB-INF/jimple"), otherwise null
	 * @param outputJar The location to output the resulting jar
	 * @param transforms An array of transformations to apply during compilation, if non pass empty array or null
	 * @throws IOException
	 * @throws CoreException 
	 */
	public static void compile(IProject project, File jimpleDirectory, File outputJar, Transform... transforms) throws IOException, CoreException {
		compile(project, jimpleDirectory, outputJar, false, transforms);
	}
	
	
	
	/**
	 * Compiles a Jimple in a project to an output JAR file
	 * @param project The project to compile
	 * @param jimpleDirectory The directory path containing the jimple source (example: "sootOutput" or "WEB-INF/jimple"), otherwise null
	 * @param outputJar The location to output the resulting jar
	 * @param allowPhantomReferences allows phantom references to exist
	 * @param transforms An array of transformations to apply during compilation, if non pass empty array or null
	 * @throws IOException
	 * @throws CoreException 
	 */
	
	public static void compile(IProject project, File jimpleDirectory, File outputJar, boolean allowPhantomReferences, Transform... transforms) throws IOException, CoreException {
		compile(project, jimpleDirectory, outputJar, false, true, transforms);
	}
	
	public static void compile(IProject project, File jimpleDirectory, File outputJar, boolean allowPhantomReferences, boolean outputBytecode, Transform... transforms) throws IOException, CoreException {
		// make sure there is a directory to write the output to
		File outputDirectory = outputJar.getParentFile();
		if(!outputDirectory.exists()){
			outputDirectory.mkdirs();
		}

		File projectDirectory = project.getLocation().toFile();
		
		// if the jimple is located entirely within a subdirectory of the project
		File inputDirectory = projectDirectory;
		if(jimpleDirectory != null){
			inputDirectory = jimpleDirectory;
		}
		
		// locate classpath jars
		StringBuilder classpath = new StringBuilder();
		addJarsToClasspath(JavaCore.create(project), classpath);
		
		// locate classpath jars for project dependencies
		for(IProject dependency : project.getReferencedProjects()){
			addJarsToClasspath(JavaCore.create(dependency), classpath);
		}
		
		// configure soot arguments
		ArrayList<String> argList = new ArrayList<String>();
		
		// take jimple as input
		argList.add("-src-prec"); argList.add("jimple");
		
		// specify the input directory of class files
		// consider all specified classes
		argList.add("-process-dir"); argList.add(inputDirectory.getCanonicalPath());
		argList.add("-include-all");

		// set the classpath
		argList.add("-cp"); argList.add(classpath.toString());
		
		// optionally allow phantom references to exist
		if(allowPhantomReferences){
			argList.add("-allow-phantom-refs");
		}
		
		// output class or jimple files
		argList.add("-output-format"); argList.add(outputBytecode ? "class" : "jimple");
		
		// try to preserve as much of the original implementation as possible
		argList.add("--p");argList.add("jb");argList.add("use-original-names:true");
        argList.add("--p");argList.add("jb");argList.add("stabilize-local-names:true");
        argList.add("-keep-bytecode-offset");
        argList.add("-keep-line-number");
		
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
//			Log.info("Compiled Jimple to Jar: " + outputJar.getCanonicalPath());
		} catch (Throwable t){
			String message = "An error occurred compiling Jimple to class files.";
			if(!outputBytecode){
				message = "An error occurred while transforming Jimple files.";
			}
			RuntimeException trace = new RuntimeException(t);
			Log.error("An error occurred processing Jimple.\n\nSoot Arguments: " + Arrays.toString(sootArgs), trace);
			throw trace;
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
            message.append("When compiling Jimple, some classes were referenced that could not be found.\n\n");
            for (String sootClass : missingClasses) {
                    message.append(sootClass);
                    message.append("\n");
            }
            Log.warning(message.toString());
        }
	}

	// helper method for locating and adding the jar locations to the classpath
	// should handle library paths and absolute jar locations
	private static void addJarsToClasspath(IJavaProject jProject, StringBuilder classpath) throws JavaModelException {
		IPackageFragmentRoot[] fragments = jProject.getAllPackageFragmentRoots();
		for(IPackageFragmentRoot fragment : fragments){
			if(fragment.getKind() == IPackageFragmentRoot.K_BINARY){				
				String jarLocation;
				try {
					// get the path to the jar resource
					jarLocation = fragment.getResource().getLocation().toFile().getCanonicalPath();
				} catch (Exception e){
					try {
						// if its a library then the first try will fail
						jarLocation = fragment.getPath().toFile().getCanonicalPath();
					} catch (Exception e2){
						// just get the name of the jar
						jarLocation = fragment.getElementName();
					}
				}
				classpath.append(jarLocation);
				classpath.append(File.pathSeparator);
			}
		}
	}
	
	// returns the common parent of all discovered jimple files
	public static File getJimpleDirectory(File projectDirectory) throws IOException {
		LinkedList<File> jimpleFiles = findJimple(projectDirectory);
		if(jimpleFiles.isEmpty()){
			throw new RuntimeException("Project does not contain Jimple files.");
		}
		return commonParent(jimpleFiles.toArray(new File[jimpleFiles.size()]));
	}
	
	// a helper method for finding the common parent of a set of files
	// modified source from http://rosettacode.org/wiki/Find_common_directory_path
	private static File commonParent(File[] files) throws IOException {
		String delimeter = File.separator.equals("\\") ? "\\\\" : File.separator;
		String[] paths = new String[files.length];
		for(int i=0; i<files.length; i++){
			if(files[i].isDirectory()){
				paths[i] = files[i].getCanonicalPath();
			} else {
				paths[i] = files[i].getParentFile().getCanonicalPath();
			}
		}
		String commonPath = "";
		String[][] folders = new String[paths.length][];
		for (int i = 0; i < paths.length; i++){
			folders[i] = paths[i].split(delimeter);
		}
		for (int j = 0; j < folders[0].length; j++){
			String thisFolder = folders[0][j];
			boolean allMatched = true;
			for (int i = 1; i < folders.length && allMatched; i++){
				if (folders[i].length < j){
					allMatched = false;
					break;
				}
				// otherwise
				allMatched &= folders[i][j].equals(thisFolder);
			}
			if (allMatched){
				commonPath += thisFolder + File.separatorChar;
			} else {
				break;
			}
		}
		return new File(commonPath);
	}
	
	// helper method for recursively finding jar files in a given directory
	private static LinkedList<File> findJimple(File directory){
		LinkedList<File> jimple = new LinkedList<File>();
		if(directory.exists()){
			if (directory.isDirectory()) {
				for (File f : directory.listFiles()) {
					jimple.addAll(findJimple(f));
				}
			}
			File file = directory;
			if(file.getName().endsWith(".jimple")){
				jimple.add(file);
			}
		}
		return jimple;
	}

}
