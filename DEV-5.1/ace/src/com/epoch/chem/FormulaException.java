package com.epoch.chem;

/** Thrown when there is an error parsing the molecular formula.  */ 
public class FormulaException extends Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * @param	msg	an error message
	 */
	public FormulaException(String msg) {
		super(msg);
	}
}
