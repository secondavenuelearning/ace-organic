package com.epoch.exceptions;

/** 
 * Thrown when a compound has a valence error.
 */ 
public class ValenceException extends java.lang.Exception {

	/** Constant found in all exceptions. */
	private static final long serialVersionUID = 1L;
	/** 1-based numbers of the atoms that have bad valences. */
	private int[] badAtomNums = new int[0];

	/** Constructor.
	 * @param	message	an error message
	 */
	public ValenceException(String message) {
		super(message);
	}

	/** Sets 1-based numbers of the atoms that have bad valences.
	 * @param	atomNums	numbers of the atoms that have bad valences
	 */
	public void setBadAtomNums(int[] atomNums) 	{ badAtomNums = atomNums; }
	/** Gets 1-based numbers of the atoms that have bad valences.
	 * @return	numbers of the atoms that have bad valences
	 */
	public int[] getBadAtomNums() 				{ return badAtomNums; }
} 


