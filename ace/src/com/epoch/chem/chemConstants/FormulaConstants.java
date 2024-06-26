package com.epoch.chem.chemConstants;

/** Holds constants used in Formula. */
public interface FormulaConstants {

	// public static final is implied by interface
	/** Symbol to denote any number (zero or more) in chemical formula.
	 * NOTE: Don't change it, as this value may already be part of database
	 * entries. */
	char ANY_NUMBER_IN_FORMULA = '*';
	/** Symbol to denote any number (zero or more) in chemical formula,
	 * for use in regular expressions.  */
	String ANY_NUMBER_REGEX = "\\*";
	/** Bit 0 of flag for constructors and parseFormulaStr. */
	int FIX_CASE = 1 << 0;
	/** Opposite of bit 0 of flag for constructors and parseFormulaStr. */
	int KEEP_CASE = 0;
	/** Bit 1 of flag for  constructors and parseFormulaStr. */
	int ALLOW_ASTERISK = 1 << 1;

} // FormulaConstants
