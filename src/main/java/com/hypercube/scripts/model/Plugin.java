package com.hypercube.scripts.model;

import java.util.logging.Logger;

public abstract class Plugin {
	protected Logger logger = Logger.getLogger(this.getClass().getName());
	
	/**
	 * This method is implemented in the script
	 * @throws Exception
	 */
	abstract public void execute() throws Exception;
}
