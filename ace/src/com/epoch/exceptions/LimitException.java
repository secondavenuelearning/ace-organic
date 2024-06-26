package com.epoch.exceptions;

/** 
 * Thrown when any method violates the limit constraints.
 * Action: must not be allowed to display at error page; catch and display.
 * License restriction can use this.
 */ 
public class LimitException extends java.lang.Exception {

	/** Constant found in all exceptions.  */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	message	an error message
	 */
	public LimitException(String message) { 
		super(message);
	}
} 

