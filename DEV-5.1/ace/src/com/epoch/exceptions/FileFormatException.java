package com.epoch.exceptions;

/** 
 * Thrown when any external file doesn't confirm to standards.
 * Action: Check the file to see whether it confirms to the specifications.
 */ 
public class FileFormatException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	message	an error message
	 */
	public FileFormatException(String message) {
		super(message);
	}

} 


