package com.epoch.evals.impl.genericQEvals.textEvals.textEvalConstants;

/** Contains constants shared by <code>TextContains</code>,
<code>NumberIs</code>, and <code>Table*</code> classes. */
public interface TextConstants {

	/** Value of where.  */
	public static final int IS = 0;
	/** Value of where.  */
	public static final int STARTS = 1;
	/** Value of where.  */
	public static final int ENDS = 2;
	/** Value of where.  */
	public static final int CONTAINS = 3;
	/** Value of where.  */
	public static final int CONT_INTERNAL = 4;
	/** Value of where.  */
	public static final int IS_SUBSTRING = 5;
	/** Value of where.  */
	public static final int MATCHES_REGEX = 6;
	/** Value of where.  */
	public static final int CONT_REGEX = 7;
	/** Database values corresponding to values for where.  */
	public static final String[] WHERE = new String[] {
			"is", "starts", "ends", "contains", "internal", 
			"isSubstring", "regex", "contRegex"};
	/** English equivalents of database values for where. */
	public static final String[] WHERE_ENGLISH = {
			" equal ",
			" start with ",
			" end with ",
			" contain ",
			" contain internally ",
			" exist within ",
			" match the regular expression ",
			" contain the regular expression "
			};
	/** Parameter for addSpanString(). */
	public static final boolean TO_DISPLAY = true;

} // TextConstants
