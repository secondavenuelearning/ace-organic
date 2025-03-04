package com.epoch.evals.impl.implConstants;

import com.epoch.evals.evalConstants.EvalImplConstants;

/** Contains constants used by CompareNum. */
public interface CompareNumConstants extends EvalImplConstants {

	/** Value for <code>oper</code>.  */
	public static final int EQUALS = 0;
	/** Value for <code>oper</code>.  */
	public static final int GREATER = 1;
	/** Value for <code>oper</code>.  */
	public static final int LESS = 2;
	/** Value for <code>oper</code>.  */
	public static final int NOT_EQUALS = 3;
	/** Value for <code>oper</code>.  */
	public static final int NOT_GREATER = 4;
	/** Value for <code>oper</code>.  */
	public static final int NOT_LESS = 5;

	/** Database values corresponding to values for <code>oper</code>. */
	public static final String[] SYMBOLS = new String[] {
			"Y=", "Y>", "Y<", "N=", "N>", "N<"};

	/** English description of each value of <code>oper</code>. */
	public static final String[][] OPER_ENGLISH = 
		{ 	{
			" equal to ",
			" greater than ",
			" less than ",
			" not equal to ",
			" less than or equal to ",
			" greater than or equal to "
			}, {
			" equal to ",
			" more than ",
			" fewer than ",
			" not equal to ",
			" fewer than or equal to ",
			" more than or equal to "
			}
		};
	/** Member of OPER_ENGLISH for values that are continuous ("less than"). */
	public static final int LESSER = 0;
	/** Member of OPER_ENGLISH for values that are integral ("fewer than"). */
	public static final int FEWER = 1;

} // CompareNums
