package com.epoch.chem;

/** Thrown when there is an error involving wedge bonds.  */ 
public class WedgeException extends Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;
	/** Index of the atom with the bad wedge bond. */
	transient public int atomNum = -1;

	/** Constructor.
	 * @param	msg	an error message
	 */
	public WedgeException(String msg) {
		super(msg);
	} // WedgeException(String)

	/** Constructor.
	 * @param	msg	an error message
	 * @param	atNum	number of the atom with the bad wedge
	 */
	public WedgeException(String msg, int atNum) {
		super(msg);
		atomNum = atNum;
	} // WedgeException(String, int)
}
