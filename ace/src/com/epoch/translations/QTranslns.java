package com.epoch.translations;

import com.epoch.db.TranslnWrite;
import com.epoch.exceptions.DBException;
import com.epoch.qBank.Question;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Stores all of the translations of the components of a question. Used in
 * question import. */
public class QTranslns implements QuestionConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** TranslnsMap of the question statement.  */
	transient private TranslnsMap qStmt = new TranslnsMap();
	/** Translations of the evaluator feedbacks in various languages  
	 * keyed by major evaluator number.  */
	transient private Map<Integer, TranslnsMap> evalFeedbacks =
			new HashMap<Integer, TranslnsMap>();
	/** Translations of the question data in various languages  
	 * keyed by qData[GENERAL] serial number.  */
	transient private Map<Integer, TranslnsMap> qdTexts =
			new HashMap<Integer, TranslnsMap>(); 

	/** Constructor.  */
	public QTranslns() { 
		// empty constructor
	}

	/** Gets if there are no translations of the question statement.
	 * @return	true if there are no translations of the question statement
	 */
	public TranslnsMap getQStmt()	{ return qStmt; }
	/** Gets if there are no translations of the question statement.
	 * @return	true if there are no translations of the question statement
	 */
	public boolean noQStmt() 		{ return qStmt.isEmpty(); }
	/** Gets if there are no translations of the feedbacks.
	 * @return	true if there are no translations of the feedbacks
	 */
	public boolean noFeedbacks() 	{ return evalFeedbacks.isEmpty(); }
	/** Gets if there are no translations of the general question data.
	 * @return	true if there are no translations of the question data
	 */
	public boolean noQData() 		{ return qdTexts.isEmpty(); }
	/** Sets the TranslnsMap of the question statement.
	 * @param	map	the map of the question statement
	 */
	public void setQStmtTranslations(TranslnsMap map)	{ qStmt = map; }

	/** Gets the 1-based evaluator numbers that key the feedback translations.
	 * @return	list of 1-based evaluator numbers
	 */
	public List<Integer> getFeedbackNums() {
		return new ArrayList<Integer>(evalFeedbacks.keySet());
	} // getFeedbackNums()

	/** Gets the 1-based serial numbers of question data that key their 
	 * translations.
	 * @return	list of 1-based question data serial numbers
	 */
	public List<Integer> getQDataNums() {
		return new ArrayList<Integer>(qdTexts.keySet());
	} // getQDataNums()

	/** Gets the TranslnsMap for one evaluator's feedback.
	 * @param	fNum	1-based major evaluator number
	 * @return	a TranslnsMap for the feedback
	 */
	public TranslnsMap getFeedbackTranslations(int fNum) {
		return getFeedbackTranslations(Integer.valueOf(fNum));
	} // getFeedbackTranslations(int)

	/** Gets the TranslnsMap for the text of one question datum.
	 * @param	fNum	1-based serial number
	 * @return	a TranslnsMap for the text of one question datum
	 */
	public TranslnsMap getQDatumTranslations(int fNum) {
		return getQDatumTranslations(Integer.valueOf(fNum));
	} // getQDatumTranslations(int)

	/** Gets the TranslnsMap for one evaluator's feedback.
	 * @param	fNumObj	1-based major evaluator number
	 * @return	a TranslnsMap for the feedback
	 */
	public TranslnsMap getFeedbackTranslations(Integer fNumObj) {
		return evalFeedbacks.get(fNumObj);
	} // getFeedbackTranslations(Integer)

	/** Gets the TranslnsMap for the text of one question datum.
	 * @param	fNumObj	1-based serial number
	 * @return	a TranslnsMap for the text of one question datum
	 */
	public TranslnsMap getQDatumTranslations(Integer fNumObj) {
		return qdTexts.get(fNumObj);
	} // getQDatumTranslations(Integer)

	/** Sets the map of evaluator feedback TranslnsMaps by 1-based major
	 * evaluator number.
	 * @param	map	the map of evaluator feedback TranslnsMaps
	 */
	public void setFeedbackTranslations(Map<Integer, TranslnsMap> map) {
		evalFeedbacks = new HashMap<Integer, TranslnsMap>(map);
	} // setFeedbackTranslations(Map<Integer, TranslnsMap>)

	/** Sets the map of question datum TranslnsMaps by 1-based serial
	 * number.
	 * @param	map	the map of question datum TranslnsMaps
	 */
	public void setQDatumTranslations(Map<Integer, TranslnsMap> map) {
		qdTexts = new HashMap<Integer, TranslnsMap>(map);
	} // setQDatumTranslations(Map<Integer, TranslnsMap>)

	/** Puts one evaluator's TranslnsMap into the map.
	 * @param	fNum	1-based major evaluator number
	 * @param	map	TranslnsMap for the feedback
	 */
	public void putFeedbackTranslations(int fNum, TranslnsMap map) {
		putFeedbackTranslations(Integer.valueOf(fNum), map);
	} // putFeedbackTranslations(int, TranslnsMap)

	/** Puts one question datum's TranslnsMap into the map.
	 * @param	fNum	1-based serial number
	 * @param	map	TranslnsMap for the question datum
	 */
	public void putQDatumTranslations(int fNum, TranslnsMap map) {
		putQDatumTranslations(Integer.valueOf(fNum), map);
	} // putQDatumTranslations(int, TranslnsMap)

	/** Puts one evaluator's TranslnsMap into the map.
	 * @param	fNumObj	1-based major evaluator number
	 * @param	map	TranslnsMap for the feedback
	 */
	public void putFeedbackTranslations(Integer fNumObj, TranslnsMap map) {
		evalFeedbacks.put(fNumObj, map);
	} // putFeedbackTranslations(Integer, TranslnsMap)

	/** Puts one question datum's TranslnsMap into the map.
	 * @param	fNumObj	1-based serial number
	 * @param	map	TranslnsMap for the question datum
	 */
	public void putQDatumTranslations(Integer fNumObj, TranslnsMap map) {
		qdTexts.put(fNumObj, map);
	} // putQDatumTranslations(Integer, TranslnsMap)

	/** Assigns translations of a duplicated question or one acquired from
	 * an XML file.  Called by QSet.addQuestion() and parseXML().
	 * @param	newQ	question whose translations to assign
	 * @param	translatorId	userID of the person doing the translating
	 */
	public void storeFor(Question newQ, String translatorId) {
		try {
			final TranslnWrite writer = new TranslnWrite(translatorId);
			writer.assignQTranslations(newQ, this);
		} catch (DBException e) {
			Utils.alwaysPrint("QTranslns.storeFor: Caught "
					+ "DBException when trying to import translations.");
		} // try
	} // storeFor(Question, String)

} // QTranslns
