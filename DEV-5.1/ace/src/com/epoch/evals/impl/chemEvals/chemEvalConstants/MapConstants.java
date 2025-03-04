package com.epoch.evals.impl.chemEvals.chemEvalConstants;

import com.epoch.constants.FormatConstants;

public interface MapConstants extends FormatConstants {

	/** Value for oper.  */
	public static final int ATLEAST = 1;
	/** Value for oper.  */
	public static final int EXACTLY = 2;
	/** Name of the molecule property containing the 1-based numbers of the
	 * selected atoms in Marvin 5.4 and earlier. */
	public static final String SELECTED = "selectedAtoms";
	/** Divider separating numbers of selected atoms in Marvin 5.4 and earlier. */
	public static final String SEL_DIV = ":";

} // MapConstants
