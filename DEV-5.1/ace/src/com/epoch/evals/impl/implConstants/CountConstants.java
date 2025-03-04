package com.epoch.evals.impl.implConstants;

import com.epoch.evals.evalConstants.EvalImplConstants;

/** Contains constants used by classes that count individual items,
 * such as <code>Is</code> and <code>Contains</code>. */
public interface CountConstants extends EvalImplConstants {

	/** Value for <code>howMany</code>.  */
	public static final int NONE = 1;
	/** Value for <code>howMany</code>.  */
	public static final int ONLY = 2;
	/** Value for <code>howMany</code>.  */
	public static final int ONE = 3;
	/** Value for <code>howMany</code>.  */
	public static final int ANY = 4;
	/** Value for <code>howMany</code>.  */
	public static final int NOT_ALL = 5;
	/** Value for <code>howMany</code>.  */
	public static final int ALL = 6;
	/** English descriptions of the <code>howMany</code> values. */
	public static final String[] HOWMANY_ENGL = {
			" no ", " the only ", " exactly one ",
			" any ", " not every ", " every "};
	
	/** Parameter for <code>getMatches()</code>.  */
	public static final boolean IS_LEWIS = true;
	/** Parameter for <code>getMatches()</code>.  */
	public static final boolean BREAK_AT_1ST_MATCH = true;
	/** Index for <code>int[] matches</code>.  */
	public static final int MATCHES = 0;
	/** Index for <code>int[] matches</code>.  */
	public static final int NONMATCHES = 1;

} // CountConstants

