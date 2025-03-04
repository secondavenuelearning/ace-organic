package com.epoch.synthesis.synthConstants;

/** Constants used by SingleRxnSolver. */
public class SingleRxnConstants implements SynthConstants {

	/** To prevent infinite loops, the maximum number of times Reactor products 
	 * can be resubjected to the reaction conditions. */
	public static final int MAX_ONE_RXN_LOOPS = 4;
	/** Parameter for isResponseMatching().  */
	public static final String NO_AUTHSTRING = null;

	/** Array member of return value of getGroupNameNumber(). */
	public static final int NAME = 0;
	/** Array member of return value of getGroupNameNumber(). */
	public static final int NUMBER = 1;
	/** Value for racemizedSM. */
	public static final int NO_SM_RACEMIZED = 0;
	/** Value for racemizedSM. */
	public static final int INIT_ARRAY = 1;
	/** Value for racemizedSM. */
	public static final int RXN_PRODS = 2;
	/** Maximum number of times we can start the whole reaction over after
	 * generating an enantiomer of a starting material. */
	public static final int NUM_RESTARTS = 4;
	/** Maximum number of times to return to Reactor for more products. */
	public static final int MAX_REACT = 6;

} // SingleRxnConstants
