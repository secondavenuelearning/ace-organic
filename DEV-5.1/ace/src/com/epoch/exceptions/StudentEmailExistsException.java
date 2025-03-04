package com.epoch.exceptions;

/** 
 * Thrown when ACE attempts to register a student with an already 
 * existing email address.
 */ 
public class StudentEmailExistsException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;
	
	/** Constructor.
	 * @param	email	email address of the student
	 */
	public StudentEmailExistsException(String email) {
		super("A student with the email address " + email
			+ " at your institution has already registered with ACE."
			+ " If this this student is you, log in and press <b>My Profile</b> "
			+ " to change your registration information.");
	}
} 
