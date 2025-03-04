package com.epoch.exceptions;

/** 
 * Thrown when ACE needs to give feedback to a response submitted by the user.
 * No longer used.
 */ 
public class FeedbackException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;
	/** Describes the kind of error. */
	transient public int kindOfError = 0;

	/** Constructor.
	 * @param	message	an error message, typically a modified version of the
	 * response
	 */
	public FeedbackException(String message) {
		super(message);
		// System.out.println("FeedbackException: "+message);
	}

	/** Constructor.
	 * @param	message	an error message, typically a modified version of the
	 * response
	 * @param	kindOfError	kind of error caused by the response
	 */
	public FeedbackException(String message, int kindOfError) {
		super(message);
		this.kindOfError = kindOfError;
		// System.out.println("FeedbackException: "+message);
	}

} // class FeedbackException
