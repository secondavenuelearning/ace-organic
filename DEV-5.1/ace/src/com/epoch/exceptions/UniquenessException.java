package com.epoch.exceptions;

/** 
 * Thrown when courseware cannot find the specified item from the database. 
 */ 
public class UniquenessException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	message	an error message
	 */
	public UniquenessException(String message) {
		super(message);
	}
} 


