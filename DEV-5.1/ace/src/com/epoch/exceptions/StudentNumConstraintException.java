package com.epoch.exceptions;

/** 
 * Thrown when ACE attempts to register a student with an already 
 * existing student number.
 */ 
public class StudentNumConstraintException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;
	
	/** Constructor.
	 * @param	studentNum	student's school ID number
	 */
	public StudentNumConstraintException(String studentNum) {
		super("The student ID number " + studentNum
			+ " has already been selected by another student at your institution."
			+ " Please enter another student number.");
	}
} 
