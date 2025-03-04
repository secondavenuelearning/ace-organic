
package com.epoch.exceptions;

/** 
 * Thrown when the number of searches performed exceeds the limit allowed
 * by license.
 */ 
public class LicenseException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	message	an error message
	 */
	public LicenseException(String message) { 
		super(message);
	}
} 

