<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents** 

- [dynamic compilation in Java](#dynamic-compilation-in-java)
  - [Introduction](#introduction)
  - [Security](#security)
  - [Few notes about JDK 9](#few-notes-about-jdk-9)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Dynamic compilation in Java 11

## Introduction

Compiling a Java file on the fly is not so complicated. First, you have to instantiate a new compiler:

```java
JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
```

Then you need to create a compilation task providing a **file manager**:

```java
CompilationTask task = compiler.getTask(...,fileManager,...);
```

The file manager is responsible to provide the source code of the script and other pre-compiled classes. All those files implements the interface `JavaFileObject`

Then you run the compilation task:

```java
boolean success = task.call();
```

And voila! The compiled class will be available in the file manager.

```java
Class<?> compiledClass = fileManager.getClassLoader(null).loadClass(className);
```

## Security

Creating a plugin system could cause serious security holes if you run a malicious script. Fortunately the dynamic compilation API in Java allow you to control which class can be used by the script at compile time. This is handled by `ForwardingJavaFileManager::list`, so you have to inherit from this class to make your own file manager.

It's also a good idea to limit the scope of your script to a method class instead of an entire Java file. After all, your plugin will have to implement a known method to be usable. In this project all plugin will inherit from the abstract class `Plugin` and the abstract method will be `execute`

```java
public abstract class Plugin {
	/**
	 * This method is implemented in the script
	 * @throws Exception
	 */
	abstract public void execute() throws Exception;
}
```

## Few notes about JDK 9

Since JDK 9, you have now **modules**. So pay attention to the class `javax.tools.StandardLocation`, because there are more possible locations now and you must handle them in the file manager:

JDK 8:
| Enum Constant | Description                 |
|----------------------|--------------------------------------------------------|
| `CLASS_OUTPUT` | Location of new class files.                  |
| `CLASS_PATH` | Location to search for user class files.        |
| `NATIVE_HEADER_OUTPUT` | Location of new native header files.  |
| `PLATFORM_CLASS_PATH` | Location to search for platform classes. |
| `SOURCE_OUTPUT` | Location of new source files.                |
| `SOURCE_PATH` | Location to search for existing source files.  |

JDK 9:
| Enum Constant | Description                 |
|----------------------|--------------------------------------------------------|
| `CLASS_OUTPUT`         | Location of new class files.                       |
| `CLASS_PATH`           | Location to search for user class files.           |
| `MODULE_PATH`          | Location to search for precompiled user modules.   |
| `MODULE_SOURCE_PATH`   | Location to search for the source code of modules. |
| `NATIVE_HEADER_OUTPUT` | Location of new native header files.               |
| `PATCH_MODULE_PATH`    | Location to search for module patches.             |
| `PLATFORM_CLASS_PATH`  | Location to search for platform classes.           |
| `SOURCE_OUTPUT`        | Location of new source files.                      |
| `SOURCE_PATH`          | Location to search for existing source files.      |
| `SYSTEM_MODULES`       | Location to search for system modules.             |
| `UPGRADE_MODULE_PATH`  | Location to search for upgradeable system modules. |

# This project

I made the things as simple as possible:

- `com.hypercube.scripts.FileManager`: the most important class used by the compiler
- `com.hypercube.scripts.PluginBinary`: a class used by the compiler to store the compiled  .class, we store in memory.
- `com.hypercube.scripts.PluginScript`: a class used by the compiler to get the source code
- `com.hypercube.scripts.PrecompiledJavaFile`: a kind of wrapper used by the compiler to get some .class. We use it for `Plugin.class`
- `com.hypercube.scripts.model.Plugin`: the base class of any plugin, provides a logger to the plugin.

# Example

Script located in the file `scripts/MyScript.txt`:

```java
package com.hypercube.scripts.model;

public class MyScript extends Plugin {

	@Override
	public void execute() throws Exception {
		logger.info("Hello world !");
	}

}
```

I made some logs to make you understand what's going on, especially what the compiler ask to the file manager:

```
[2020-09-08 03:22:56 PM] com.hypercube.scripts.FileManager INFO:  Requires ClassLoader for location CLASS_PATH  
[2020-09-08 03:22:56 PM] com.hypercube.scripts.FileManager INFO:  LOCATION INCLUDED: SYSTEM_MODULES[java.base] package: java.lang kinds: SOURCE,CLASS,HTML,OTHER  
[2020-09-08 03:22:56 PM] com.hypercube.scripts.FileManager INFO:  LOCATION FILTERED: CLASS_PATH package: com.hypercube.scripts kinds: SOURCE,CLASS,HTML,OTHER  
[2020-09-08 03:22:56 PM] com.hypercube.scripts.FileManager INFO:  LOCATION FILTERED: CLASS_PATH package: com.hypercube kinds: SOURCE,CLASS,HTML,OTHER  
[2020-09-08 03:22:56 PM] com.hypercube.scripts.FileManager INFO:  LOCATION FILTERED: CLASS_PATH package: com kinds: SOURCE,CLASS,HTML,OTHER  
[2020-09-08 03:22:56 PM] com.hypercube.scripts.FileManager INFO:  LOCATION INCLUDED: SYSTEM_MODULES[java.base] package: java.lang.annotation kinds: SOURCE,CLASS,HTML,OTHER  
[2020-09-08 03:22:56 PM] com.hypercube.scripts.FileManager INFO:  LOCATION INCLUDED: SYSTEM_MODULES[java.base] package: java.io kinds: SOURCE,CLASS,HTML,OTHER  
[2020-09-08 03:22:56 PM] com.hypercube.scripts.FileManager INFO:  LOCATION INCLUDED: SYSTEM_MODULES[java.logging] package: java.util.logging kinds: SOURCE,CLASS,HTML,OTHER  
[2020-09-08 03:22:56 PM] com.hypercube.scripts.FileManager INFO:  LOCATION INCLUDED: SYSTEM_MODULES[java.base] package: java.util.function kinds: SOURCE,CLASS,HTML,OTHER  
[2020-09-08 03:22:56 PM] com.hypercube.scripts.FileManager INFO:  LOCATION INCLUDED: SYSTEM_MODULES[java.base] package: java.lang.invoke kinds: SOURCE,CLASS,HTML,OTHER  
[2020-09-08 03:22:56 PM] com.hypercube.scripts.FileManager INFO:  Requires ClassLoader for location null  
[2020-09-08 03:22:56 PM] com.hypercube.DynamicCompilerExample INFO:  Script successfully compiled  
[2020-09-08 03:22:56 PM] com.hypercube.scripts.model.MyScript INFO:  Hello world !
```

If you remove `"java.util.logging"` or `"java.util.function"` from `FileManager::acceptedPackages` you will have:

```
[2020-09-08 03:25:12 PM] com.hypercube.scripts.FileManager INFO:  Requires ClassLoader for location CLASS_PATH  
[2020-09-08 03:25:12 PM] com.hypercube.scripts.FileManager INFO:  LOCATION INCLUDED: SYSTEM_MODULES[java.base] package: java.lang kinds: SOURCE,CLASS,HTML,OTHER  
[2020-09-08 03:25:12 PM] com.hypercube.scripts.FileManager INFO:  LOCATION FILTERED: CLASS_PATH package: com.hypercube.scripts kinds: SOURCE,CLASS,HTML,OTHER  
[2020-09-08 03:25:12 PM] com.hypercube.scripts.FileManager INFO:  LOCATION FILTERED: CLASS_PATH package: com.hypercube kinds: SOURCE,CLASS,HTML,OTHER  
[2020-09-08 03:25:12 PM] com.hypercube.scripts.FileManager INFO:  LOCATION FILTERED: CLASS_PATH package: com kinds: SOURCE,CLASS,HTML,OTHER  
[2020-09-08 03:25:12 PM] com.hypercube.scripts.FileManager INFO:  LOCATION INCLUDED: SYSTEM_MODULES[java.base] package: java.lang.annotation kinds: SOURCE,CLASS,HTML,OTHER  
[2020-09-08 03:25:12 PM] com.hypercube.scripts.FileManager INFO:  LOCATION INCLUDED: SYSTEM_MODULES[java.base] package: java.io kinds: SOURCE,CLASS,HTML,OTHER  
[2020-09-08 03:25:12 PM] com.hypercube.scripts.FileManager INFO:  LOCATION FILTERED: SYSTEM_MODULES[java.logging] package: java.util.logging kinds: SOURCE,CLASS,HTML,OTHER  
[2020-09-08 03:25:12 PM] com.hypercube.DynamicCompilerExample SEVERE:  Compilation error:   
[2020-09-08 03:25:12 PM] com.hypercube.DynamicCompilerExample SEVERE:  MyScript:7: error: cannot access java.util.logging.Logger
		logger.info("Hello world !");
		      ^
  class file for java.util.logging.Logger not found   
```

