package com.epoch.substns.substnConstants;

/** Contains constants for substituting R groups or variable values. */
public interface SubstnConstants {

	// public static final is implied by interface

	/** Separates R-group collection ID numbers or values in QDatum.data. */
	String SUBSTNS_SEP = ":";
	/** Separates words and corresponding values in QDatum.data. */
	String WORD_VALUE_SEP = "=";
	/** Parameter for substituteRGroups().  */
	boolean ADD_H_ATOMS = true;
	/** Parameter for substituteValues().  */
	boolean NUMERIC_VALUE = true;

} // SubstnConstants
