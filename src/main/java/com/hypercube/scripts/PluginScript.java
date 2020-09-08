package com.hypercube.scripts;

import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class PluginScript  extends SimpleJavaFileObject {
	private String name;
	private String binaryName;
	private String sourceCode;
	private PluginBinary binary;
	
	
	public PluginBinary getBinary() {
		return binary;
	}

	public String getName() {
		return name;
	}

	public String getBinaryName() {
		return binaryName;
	}

	public String getSourceCode() {
		return sourceCode;
	}

	public PluginScript(String binaryName,String sourceCode) {
		super(URI.create("file:///" + binaryName.replace('.', '/')+".java"), Kind.SOURCE);
		this.sourceCode = sourceCode;
		this.binaryName = binaryName;
		this.binary = new PluginBinary(binaryName);
		int idx = binaryName.lastIndexOf('.');
		this.name = binaryName.substring(idx+1);
	}
	
	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		return this.sourceCode;
	}
}
