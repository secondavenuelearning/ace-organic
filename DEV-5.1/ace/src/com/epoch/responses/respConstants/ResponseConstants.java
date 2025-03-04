package com.epoch.responses.respConstants;

import com.epoch.chem.chemConstants.ChemConstants;

/** Holds constants used for Response. */
public class ResponseConstants implements ChemConstants {

	/** Value of errorReason. */
	public static final int NONE = 0;
	/** Value of errorReason. */
	public static final int FORMAT = 1;
	/** Value of errorReason. */
	public static final int PARSING = 2;

	/** Commonly repeated phrase in feedback. */
	public static final String CANNOT = "ACE cannot interpret your ";
	/** Commonly repeated tag in feedback. */
	public static final String BR = "<br/>";
	/** Parameter for formatError(). */
	public static final boolean PRINT_STACK = true;

} // ResponseConstants
