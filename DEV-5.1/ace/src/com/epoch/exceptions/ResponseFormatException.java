package com.epoch.exceptions;

/** 
 * Thrown when ACE cannot import or parse a response submitted by the user.
 */ 
public class ResponseFormatException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	message	an error message
	 */
	public ResponseFormatException(String message) {
		super(message);
	}

} 


