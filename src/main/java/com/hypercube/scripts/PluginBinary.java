package com.hypercube.scripts;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class PluginBinary extends SimpleJavaFileObject  {

	protected PluginBinary(String binaryName) {
		super(URI.create("file:///" + binaryName.replace('.', '/')+".class"), Kind.CLASS);
	}

	private ByteArrayOutputStream out = new ByteArrayOutputStream();
	
	@Override
	public OutputStream openOutputStream() {
		return out;
	}
	public byte[] getBytes() {
		return out.toByteArray();
	}
}
