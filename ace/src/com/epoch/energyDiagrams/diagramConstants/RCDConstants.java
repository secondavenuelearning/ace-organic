package com.epoch.energyDiagrams.diagramConstants;

/** Holds constants for RCD. */
public interface RCDConstants extends EDiagramConstants {

	// public static final is implied by interface

	/** Member of array from splitting question data. */
	int NUM_ROWS = 0;
	/** Member of array from splitting question data. */
	int NUM_COLS = 1;
	/** Default horizontal size of a diagram. */
	int NUM_COLS_DEFAULT = 3;
	/** Default pulldown labels of a diagram. */
	String[] LABELS_DEFAULT = 
			new String[] {"maximum", "minimum"};

	/** Parameter for setStates(). */
	boolean THROW_IT = true;

	/** Value of error. */
	int NO_ERROR = 0;
	/** Value of error. */
	int LABEL_OUT_OF_RANGE = 1;
	/** Value of error. */
	int TWO_UNCONN_IN_COL = 2;
	/** Value of error. */
	int BAD_STATE = 3;

} // RCDConstants
