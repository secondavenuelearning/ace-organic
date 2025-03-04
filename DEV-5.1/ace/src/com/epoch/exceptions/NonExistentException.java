package com.epoch.exceptions;

/** 
 * Thrown when ACE cannot find the specified item from the database. 
 */ 
public class NonExistentException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	message	an error message
	 */
	public NonExistentException(String message) {
		super(message);
	}
} 


