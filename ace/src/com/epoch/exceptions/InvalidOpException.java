package com.epoch.exceptions;

/**
 * Thrown when a method is called at an untimely instance.
 * Cause: Front end has to prevent this call from occurring at this instance.
 * Action: Report it as a bug to developer.
 */
public class InvalidOpException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	message	an error message
	 */
	public InvalidOpException(String message) {
		super(message);
	}

} 
