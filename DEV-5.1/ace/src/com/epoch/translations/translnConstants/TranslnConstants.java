package com.epoch.translations.translnConstants;

import com.epoch.constants.AppConstants;

/** Constants for QSetTransln and PhraseTransln.  */
public interface TranslnConstants extends AppConstants {

	// public static final is implied by interface

	/** Value of a translation that should be erased. */
	String ERASE_TRANSLN = "***ERASE***";
	/** Parameter for translate(). */
	boolean ADD_NEW_PHRASES_TO_DB = true;

	/** Delimiter for variable phrases (to be substituted with other 
	 * phrases) inside of translations.  To be used as a regular expression in,
	 * e.g., split() and replaceAll().  */
	String STARS_REGEX = "\\*\\*\\*";
	/** Delimiter for variable phrases (to be substituted with other 
	 * phrases) inside of translations.  */
	String STARS_SIMPLE = "***";

	/** Tag for export of question set translations. */
	String HEADER_TAG = "header";
	/** Tag for export of question set translations. */
	String QSTMT_TAG = "qStmt";
	/** Tag for export of question set translations. */
	String QDTEXT_TAG = "qdText";
	/** Tag for export of question set translations. */
	String FEEDBACK_TAG = "evalFeedback";

} // TranslnConstants
