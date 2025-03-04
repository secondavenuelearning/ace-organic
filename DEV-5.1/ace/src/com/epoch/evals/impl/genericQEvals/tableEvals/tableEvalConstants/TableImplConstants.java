package com.epoch.evals.impl.genericQEvals.tableEvals.tableEvalConstants;

/** Contains constants shared by <code>Table*</code> classes. */
public interface TableImplConstants {

	/** Value for <code>row</code> or <code>column</code>.  */
	public static final int ANY = -1;
	/** Value for <code>row</code> or <code>column</code>.  */
	public static final int EVERY = -2;
	/** Value for <code>row</code> or <code>column</code>.  */
	public static final int NO = -3;
	/** English descriptions of <code>row</code> or <code>column</code> 
	 * values to display in JSP pages. */
	public static final String[] ROW_COL = {"any", "every", "no"};

	/** Value for <code>rowOper</code>.  */
	public static final int ANY_ROW = 0;
	/** Value for <code>rowOper</code>.  */
	public static final int EVERY_ROW = 1;
	/** Database values (also English descriptions) corresponding 
	 * to values for <code>rowOper</code>.  */
	public static final String[] ROW_OPER = new String[] {"any", "every"};

	/** Value for <code>emptyCell</code>.  */
	public static final int EMPTY_STR = 0;
	/** Value for <code>nonnumeric</code>.  */
	public static final int ZERO = 0;
	/** Value for <code>emptyCell</code> or <code>nonnumeric</code>.  */
	public static final int IGNORE = 1;
	/** Value for <code>emptyCell</code> or <code>nonnumeric</code>.  */
	public static final int DISALLOW = 2;
	/** Database values corresponding to values for
	 * <code>emptyCell</code> and <code>nonnumeric</code>. */
	public static final String[] EMPTYCELL = 
			new String[] {"empty", "ignore", "disallow"};

	/** English versions of <code>emptyCell</code> values to display in JSP
	 * pages. */
	public static final String[] EMPTYCELL_JSP = {
			"Consider queried cells that are empty to contain the value \"\".",
			"Skip rows with empty cells.",
			"Have ACE display an error message if a queried cell is empty."
			};
	/** English versions of <code>nonnumeric</code> values to display in JSP
	 * pages. */
	public static final String[] NONNUMERIC_JSP = {
			"Consider queried cells that are supposed to have numeric "
					+ "values, but do not, to contain the value 0.",
			"Skip rows with queried cells that are supposed to have numeric "
					+ "values, but do not.",
			"Have ACE display an error message if a queried cell that is "
					+ "supposed to contain a nonnumeric value, does not."
			};
	/** English versions of <code>emptyCell</code> values for evaluator
	 * descriptions. */
	public static final String[] EMPTYCELL_ENGL = {
			"empty cells have value \"\"",
			"skip empty cells",
			"disallow empty cells"
			};
	/** English versions of <code>nonnumeric</code> values for evaluator
	 * descriptions. */
	public static final String[] NONNUMERIC_ENGL = {
			"nonnumeric values have value 0", 
			"skip nonnumeric values", 
			"disallow nonnumeric values" 
			};
	
	/** Member of array of return value of evaluateRow(). */
	public static final int CHECKED = 0;
	/** Member of array of return value of evaluateRow(). */
	public static final int MATCHED = 1;

	/** Member of isNumeric for TableDependent and subclasses. */
	public final int REF = 0;
	/** Member of isNumeric for TableDependent and subclasses. */
	public final int TEST = 1;

	/** Value for parameter operNumCells. */
	public static final int[] NOT_COUNTING = null;

	/** Character that separates the reference and test strings. */
	public static final String TWO_STR_SEP = String.valueOf((char) 7);

} // TableImplConstants
