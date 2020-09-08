package com.hypercube.scripts;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

/**
 * This class get access to classes already compiled in the current WAR/JAR This
 * allow us to use them inside our scripts that we want to compile.
 * 
 * When the compiler will ask for the bytecode of the class, openInputStream()
 * will be called. At this time we will use getResourceAsStream() on the default
 * classloader to retreive the content of the .class file (located in
 * WEB-INF/classes in the WAR).
 * 
 */
public class PrecompiledJavaFile extends SimpleJavaFileObject {
	private String binaryName;

	/**
	 * Called by ForwardingJavaFileManager.inferBinaryName
	 * 
	 * @return a full classname like "com.hypercube.model.Plugin"
	 */
	public String getBinaryName() {
		return binaryName;
	}

	public PrecompiledJavaFile(String name) {
		super(URI.create("file:///" + name.replace('.', '/') + ".class"), Kind.CLASS);
		binaryName = name;
	}

	@Override
	public InputStream openInputStream() throws IOException {
		return getClass().getClassLoader().getResourceAsStream(binaryName.replace('.', '/') + ".class");
	}
}
