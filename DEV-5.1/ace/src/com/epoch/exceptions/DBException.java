
package com.epoch.exceptions;

/** 
 * Thrown when any method cannot properly initialize the database objects. 
 * Possible due to wrong configuration/server down/corrupted data.
 * Message: Report it to admin.
 * Action: Check database connection, data.
 */ 
public class DBException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	message	an error message
	 */
	public DBException(String message) { 
		super(message);
	}

} 

