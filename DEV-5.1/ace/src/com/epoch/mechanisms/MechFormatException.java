package com.epoch.mechanisms;

/** Thrown when a malformed mechanism found in a "valid" marvin document. */
public class MechFormatException extends Exception {

	/** Constant that all exceptions have. */
	private static final long serialVersionUID = 1L;

	/** Constructor. */
	public MechFormatException() {
		// intentionally empty
	}

	/** Constructor. 
	 * @param	s	a message
	 */
	public MechFormatException(String s) { super(s); }

} // MechFormatException
