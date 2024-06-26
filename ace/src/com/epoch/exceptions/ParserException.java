package com.epoch.exceptions;

/** 
 * Thrown when there is an error parsing the external XML file. 
 * Action: Check the file to see whether it confirms to the dtd. 
 */ 
public class ParserException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	message	an error message
	 */
	public ParserException(String message) {
		super(message);
	}
} 


