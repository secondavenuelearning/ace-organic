package com.epoch.qBank.qBankConstants;

import com.epoch.constants.FormatConstants;

/** Constants used by question data. */
public interface QDatumConstants extends FormatConstants {

	// public static final is implied by interface

	/** Value for type. */
	int UNKNOWN = -1;
	/** Value for type. */
	int TEXT = 1;
	/** Value for type. */
	int MARVIN = 2;
	/** Value for type.  No longer used. */
	int RXN_CONDN = 3;
	/** Value for type. */
	int SUBSTN = 4;
	/** Value for type.  */
	int SYNTH_OK_SM = 5;
	/** Value for type.  */
	int SM_EXPR = 6;
	/** Database type values corresponding to internal type values.
	 * rxn_condn is no longer used.  */
	String[] DBVALUES = new String[] 
			{"", "text", "marvin", "rxn_condn", 
			"substn", "synthOkSM", "SMExpr"};  
	/** Tag for XML IO. */
	String QUESTION_DATUM_TAG = "questionData";
	/** Tag for XML IO. */
	String DATUM_ID_TAG = "dataId";
	/** Tag for XML IO. */
	String QID_TAG = "questionId";
	/** Tag for XML IO. */
	String SERIALNO_TAG = "serialNo";
	/** Tag for XML IO. */
	String DATUM_TYPE_TAG = "dataType";
	/** Tag for XML IO. */
	String DATA_TAG = "data";
	/** Tag for XML IO. */
	String NAME_TAG = "name";

	/** Attribute values. */
	String[] TYPE_ATTRIBUTES = new String[] {
			"", "TEXT", "MARVIN", "RXN_CONDN", 
			"RGROUP", "SYNTH_OK_SM", "SM_EXPR", "VALUE"};

	/** Used by toShortDisplay(). */
	String CHEM_STRUCT = "chemical structure";
	/** Parameter for toShortDisplay(). */
	boolean ADD_CHEM_STRUCT = true;
	/** Name of molecule property containing display options. */
	String DISPLAY_OPTS = "displayOpts";

} // QDatumConstants
