package com.hypercube;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import com.hypercube.scripts.FileManager;
import com.hypercube.scripts.PluginScript;
import com.hypercube.scripts.model.Plugin;

public class DynamicCompilerExample {
	private static Logger logger = Logger.getLogger(DynamicCompilerExample.class.getName());

	public static void initLogs() {
	    try {
	        // Load a properties file from class path java.util.logging.config.file
	        final LogManager logManager = LogManager.getLogManager();
	        URL configURL = DynamicCompilerExample.class.getResource("/logging.properties");
	        if (configURL != null) {
	            try (InputStream is = configURL.openStream()) {
	                logManager.readConfiguration(is);
	            }
	        } 
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public static void main(String[] args) {

		try {
			initLogs();
			
			PluginScript script = new PluginScript("com.hypercube.scripts.model.MyScript",
					Files.readString(Paths.get("scripts/MyScript.txt"), Charset.forName("UTF-8")));
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			if (compiler == null) {
				throw new Exception("No compiler availalble, are you running inside a JRE ?");
			}

			// used to collect compilation errors
			DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

			// used to handle virtual .java and .class files that we build
			FileManager fileManager = new FileManager(compiler.getStandardFileManager(diagnostics, null, null));

			// add the plugin source code
			fileManager.getCompilationUnits().add(script);

			JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null,
					fileManager.getCompilationUnits());

			boolean success = task.call();

			if (success) {
				Class<?> compiledClass = fileManager.getClassLoader(null).loadClass(script.getBinaryName());
				logger.log(Level.INFO, "Script successfully compiled");
				Plugin pluginInstance = (Plugin)compiledClass.getDeclaredConstructor().newInstance();
				pluginInstance.execute();
				
			} else {
				// collect errors
				logger.log(Level.SEVERE, "Compilation error: ");
				for (Diagnostic<? extends JavaFileObject> diag : diagnostics.getDiagnostics()) {
					String message = diag.getMessage(Locale.ENGLISH);
					long line = diag.getLineNumber();
					long column = diag.getColumnNumber();
					String kind = diag.getKind().toString();

					logger.log(Level.SEVERE, diag.toString());
				}
			}

			fileManager.close();

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Unexpected error", e);
		}

	}

}
