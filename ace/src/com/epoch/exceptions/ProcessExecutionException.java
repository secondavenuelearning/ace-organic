package com.epoch.exceptions;

/** 
 * Thrown when a runtime process called by ACE does not execute properly.
 */ 
public class ProcessExecutionException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	message	an error message
	 */
	public ProcessExecutionException(String message) {
		super(message);
	}

} 


