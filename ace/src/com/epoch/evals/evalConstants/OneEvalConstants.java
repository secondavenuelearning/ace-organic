package com.epoch.evals.evalConstants;

/** Holds constants for the OneEvalResult class.  */ 
public interface OneEvalConstants {

	// public static final is implied by interface

	/** Bit for howHandleVarParts; off means substitute variable part for
	 * demarcated phrase, on means insert variable parts around it. */
	int INSERT = (1 << 0);
	/** Bit for howHandleVarParts. */
	int TRANSLATE = (1 << 1);

} // OneEvalConstants
