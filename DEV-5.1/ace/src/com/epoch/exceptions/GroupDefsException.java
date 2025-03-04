package com.epoch.exceptions;

/** 
 * Thrown when there is a problem in the loading of functional 
 * or R group definitions.
 */ 
public class GroupDefsException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	message	an error message
	 */
	public GroupDefsException(String message) {
		super(message);
	}

} 


