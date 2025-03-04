package com.epoch.exceptions;

/** 
 * Any problem with current configuration.
 * Action: Report it as a bug to admin with the message (if any).
 */ 
public class ConfigurationException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	message	an error message
	 */
	public ConfigurationException(String message) {
		super(message);
	}

} 


