package com.epoch.chem;

/** Thrown when a MolCompare method can't import a String representation 
 * of a molecule or when a MolSearch fails. */
public class MolCompareException extends Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	message	an error message
	 */
	public MolCompareException(String message) {
		super(message);
	}
	
}
