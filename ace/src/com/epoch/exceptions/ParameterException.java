package com.epoch.exceptions;

/** 
 * Thrown when any method recieves an unexpected parameter.
 * Possible due to improper calling, wrong configuration.
 * Action: Report it as a bug to admin with the message (if any).
 */ 
public class ParameterException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	message	an error message
	 */
	public ParameterException(String message) {
		super(message);
	}

} 


