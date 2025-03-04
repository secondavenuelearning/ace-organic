package com.epoch.db.dbConstants;

import com.epoch.qBank.qBankConstants.QuestionConstants;
import java.util.List;

/** Constants used by the QuestionWrite class. */
public class QuestionWriteConstants implements QuestionConstants {

	/** Parameter for processSingletonData().  */
	public static final int CHANGE_Q = 0;
	/** Member of fields. */
	public static final int QID_FIELD_NUM = 0;
	/** Member of fields. */
	public static final int UNIQUE_ID_FIELD_NUM = 1;
	/** Member of fields. */
	public static final int SERIAL_NUM_FIELD_NUM = 2;

	/** Parameter for addNewFigure() and deleteOtherThan(). */
	public static final List<Integer> NO_UNCHANGED_IMAGES = null;
	/** Maximum number of characters in the expression describing how to 
	 * combine subevaluators, limited by database varchar2 field. */
	public static final int MAX_EVAL_SUBEXPR = 100;

} // QuestionWriteConstants
