package com.epoch.qBank.qBankConstants;

/** Constants used by question data. */
public interface CaptionsQDatumConstants extends QDatumConstants {

	// public static final is implied by interface

	/** Database row/column values. */
	String[] CAPTS_TYPE_DBVALUES = new String[] {"R", "C", "L", "Y"};
	/** Position of label datum in CAPTS_TYPE_DBVALUES. */
	int LABEL_DATA = 2;
	/** Position of y-axis datum in CAPTS_TYPE_DBVALUES. */
	int Y_AXIS_DATA = 3;

} // CaptionsQDatumConstants
