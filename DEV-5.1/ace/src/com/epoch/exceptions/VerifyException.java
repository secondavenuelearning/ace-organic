package com.epoch.exceptions;

/** 
 * Thrown when ACE cannot verify the response submitted by the user.
 */ 
public class VerifyException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	message	an error message
	 */
	public VerifyException(String message) {
		super(message);
	}

} // VerifyException 
