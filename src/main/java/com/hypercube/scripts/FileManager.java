package com.hypercube.scripts;

import java.io.IOException;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import com.hypercube.scripts.model.Plugin;

public class FileManager extends ForwardingJavaFileManager<JavaFileManager> {
	private Logger logger = Logger.getLogger(FileManager.class.getName());

	// SECURITY: List of packages accepted in the Script
	private Set<String> acceptedPackages = Set.of("java.lang", "java.util.logging", "java.util.function", "java.io",
			"java.math", "java.lang.annotation", "java.lang.invoke", Plugin.class.getPackageName());

	// SECURITY: List of modules accepted in the Script
	private Set<String> acceptedModules = Set.of(StandardLocation.SYSTEM_MODULES.getName() + "[java.base]",
			StandardLocation.SYSTEM_MODULES.getName() + "[java.logging]");

	// List of scripts to compile
	private List<PluginScript> compilationUnits = new ArrayList<PluginScript>();

	private final Map<String, PluginScript> compiledScripts = new HashMap<String, PluginScript>();

	public List<PluginScript> getCompilationUnits() {
		return compilationUnits;
	}

	public FileManager(JavaFileManager fileManager) {
		super(fileManager);
	}

	/**
	 * The classloader returned by this FileManager is only used to retreive
	 * compiled scripts. It is never used to retreive core classes or precompiled
	 * classes in the WAR/JAR
	 */
	private ClassLoader classLoader = new SecureClassLoader(FileManager.class.getClassLoader()) {

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {

			// NOTE: we use a closure here.
			PluginScript fileObject = compiledScripts.get(name);

			if (fileObject != null) {
				byte[] bytes = fileObject.getBinary().getBytes();
				return defineClass(name, bytes, 0, bytes.length);
			} else {
				throw new ClassNotFoundException();
			}
		}
	};

	/**
	 * We provides our own classloader to deliver our compiled scripts in the map compiledScripts
	 */
	@Override
	public ClassLoader getClassLoader(Location location) {
		logger.info("Requires ClassLoader for location " + (location==null?"null":location.getName()));
		return classLoader;
	}

	/**
	 * When the compiler have to compile a .Java, this method must return the
	 * corresponding .class Our class PluginScript will receive the bytecode of the
	 * class file. Note that we never use the disk, everything take place in memory.
	 */
	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind,
			FileObject sibling) throws IOException {

		PluginScript script = (PluginScript) sibling;
		compiledScripts.put(className, script);
		return script.getBinary();
	}

	/*
	 * This is a very important method that let us to filter what the compiler can
	 * use. This prevent the script to use unauthorized classes delivered in the
	 * WAR.
	 * 
	 * In our case we return only the Script and the class BusinessRulesScript
	 * 
	 * If the called (the compiler) ask for core classes, we let the default
	 * behaviour
	 * 
	 * @see javax.tools.ForwardingJavaFileManager#list(javax.tools.JavaFileManager.
	 * Location, java.lang.String, java.util.Set, boolean)
	 */
	@Override
	public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds,
			boolean recurse) throws IOException {
		Iterable<JavaFileObject> result = Collections.emptyList();

		String kindNames = kinds.stream().map(Object::toString).reduce("",
				(acc, current) -> acc.length() == 0 ? current : acc + "," + current);

		if (acceptedModules.contains(location.getName()) && acceptedPackages.contains(packageName)) {
			logger.info(
					"LOCATION INCLUDED: " + location.getName() + " package: " + packageName + " kinds: " + kindNames);
			// core classes are retreived by the default implementation.
			result = super.list(location, packageName, kinds, recurse);
		} else if (location == StandardLocation.CLASS_PATH && kinds.contains(JavaFileObject.Kind.CLASS)) {
			if (packageName.equals(Plugin.class.getPackage().getName())) {
				//
				// the package is composed with a precompiled class "BusinessRulesScript"
				// and the classes that we compiled
				//
				ArrayList<JavaFileObject> r = new ArrayList<JavaFileObject>();
				for (PluginScript src : compilationUnits) {
					r.add(src);
				}
				r.add(new PrecompiledJavaFile(Plugin.class.getCanonicalName()));
				result = r;
			} else {
				// result = super.list(location, packageName, kinds, recurse);
				logger.info("LOCATION FILTERED: " + location.getName() + " package: " + packageName + " kinds: "
						+ kindNames);
			}
		} else {
			logger.info(
					"LOCATION FILTERED: " + location.getName() + " package: " + packageName + " kinds: " + kindNames);
		}
		return result;
	}

	/*
	 * A binary name is simply a full class name like
	 * "com.hypercube.scripts.model.MyScript"
	 * 
	 * @see javax.tools.ForwardingJavaFileManager#inferBinaryName(javax.tools.
	 * JavaFileManager.Location, javax.tools.JavaFileObject)
	 */
	@Override
	public String inferBinaryName(Location location, JavaFileObject file) {
		if (file instanceof PluginScript) {
			return ((PluginScript) file).getBinaryName();
		} else if (file instanceof PrecompiledJavaFile) {
			return ((PrecompiledJavaFile) file).getBinaryName();
		} else {
			return super.inferBinaryName(location, file);
		}
	}
}
