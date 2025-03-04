package com.epoch.exceptions;

/** 
 * Thrown when ACE can import but cannot parse a response submitted by the user.
 */ 
public class ResponseParseException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	message	an error message
	 */
	public ResponseParseException(String message) {
		super(message);
	}

} 


