package com.epoch.translations;

import com.epoch.db.TranslnRead;
import com.epoch.db.TranslnWrite;
import com.epoch.exceptions.DBException;
import com.epoch.utils.Utils;
import java.util.HashMap;
import java.util.Map;

/** Utility class containing static methods for writing or retrieving all of
 * the translations of a question set header, or for retrieving all of the
 * translations of a single question's statement, question data, and
 * evaluators. Used when cloning a question and in import/export. */
public final class QSetAllTranslns {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Gets translations in all languages of the common question statement of the
	 * given question set.
	 * @param	qSetId	unique ID of the question set
	 * @return	TranslnsMap of common question statement
	 */
	public static TranslnsMap getAllHeaderTranslns(int qSetId) {
		TranslnsMap headerTranslns = new TranslnsMap();
		try {
			headerTranslns = TranslnRead.getAllHeaderTranslns(qSetId);
		} catch (DBException e) {
			Utils.alwaysPrint("QSetAllTranslns.getAllHeaderTranslns: "
					+ "couldn't get question statement translations.");
		} // try
		return headerTranslns;
	} // getAllHeaderTranslns(int)

	/** Gets translations in all languages of the question statement of the
	 * given question.
	 * @param	qId	unique ID of the question whose statements to get
	 * @return	TranslnsMap of the question statement
	 */
	public static TranslnsMap getAllQStmtTranslns(int qId) {
		TranslnsMap qStmtTranslns = new TranslnsMap();
		try {
			qStmtTranslns = TranslnRead.getAllQStmtTranslns(qId);
		} catch (DBException e) {
			Utils.alwaysPrint("QSetAllTranslns.getAllQStmtTranslns: "
					+ "couldn't get question statement translations.");
		} // try
		return qStmtTranslns;
	} // getAllQStmtTranslns(int)

	/** Gets translations in all languages of the feedback of the major 
	 * evaluators of the given question.
	 * @param	qId	unique ID of the question whose feedback to get
	 * @return	TranslnsMaps of feedback, mapped by 1-based major evaluator 
	 * number
	 */
	public static Map<Integer, TranslnsMap> getAllFeedbackTranslns(int qId) {
		Map<Integer, TranslnsMap> feedbackTranslns = 
				new HashMap<Integer, TranslnsMap>();
		try {
			feedbackTranslns = TranslnRead.getAllFeedbackTranslns(qId);
		} catch (DBException e) {
			Utils.alwaysPrint("QSetAllTranslns.getAllFeedbackTranslns: "
					+ "couldn't get feedback translations.");
		} // try
		return feedbackTranslns;
	} // getAllFeedbackTranslns(int)

	/** Gets translations in all languages of the general question data of the
	 * given question.
	 * @param	qId	unique ID of the question whose data to get
	 * @return	TranslnsMaps of general question data, mapped by 1-based serial 
	 * number
	 */
	public static Map<Integer, TranslnsMap> getAllQDataTranslns(int qId) {
		Map<Integer, TranslnsMap> qDataTranslns = 
				new HashMap<Integer, TranslnsMap>();
		try {
			qDataTranslns = TranslnRead.getAllQDataTranslns(qId);
		} catch (DBException e) {
			Utils.alwaysPrint("QSetAllTranslns.getAllQDataTranslns: "
					+ "couldn't get question data translations.");
		} // try
		return qDataTranslns;
	} // getAllQDataTranslns(int)

	/** Assigns translations of a common question statement of a question set
	 * acquired from an XML file.
	 * @param	qSetId	unique ID of the question set whose headers are being
	 * translated
	 * @param	headerTranslns	TranslnsMap of the common question statement
	 * @param	translatorId	userID of the person doing the translating
	 */
	public static void assignHeaderTranslations(int qSetId,
			TranslnsMap headerTranslns, String translatorId) {
		try {
			final TranslnWrite writer = new TranslnWrite(translatorId);
			writer.assignHeaderTranslations(qSetId, headerTranslns);
		} catch (DBException e) {
			Utils.alwaysPrint("QSetAllTranslns.assignHeaderTranslations: Caught "
					+ "DBException when trying to import translations.");
		} // try
	} // assignHeaderTranslations(int, TranslnsMap, String)

	private QSetAllTranslns() {
		// not callable
	}

} // QSetAllTranslns
