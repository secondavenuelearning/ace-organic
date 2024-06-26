package com.epoch.translations;

import com.epoch.db.TranslnRead;
import com.epoch.db.TranslnWrite;
import com.epoch.evals.Evaluator;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.ParameterException;
import com.epoch.qBank.QDatum;
import com.epoch.qBank.QSetDescr;
import com.epoch.qBank.Question;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.session.QSet;
import com.epoch.translations.translnConstants.TranslnConstants;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Holds the translation of all of the questions in a master-authored 
 * question set into a single language. */
public class QSetTransln implements QuestionConstants, TranslnConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Unique ID of this question set. */
	private int setId;
	/** Questions in this question set. */
	transient private List<Question> setQs;
	/** Unique IDs of questions in this question set. */
	transient private List<Integer> setQIds;
	/** Description of this question set (if any). */
	private QSetDescr qSetDescr;
	/** ID of the person doing the translating. */
	private String translatorId = null;

	/** Translation of the common question statement. */
	transient public String header = null;
	/** Translations of the question statements. */
	transient public String[] qStmts = null;
	/** Translations of the evaluator feedbacks of each question. First array
	 * element is question number, second is major evaluator number. */
	transient public String[][] evalFeedbacks = null;
	/** Translations of the question data of each question. First array element
	 * is question number, second is question datum serial number. */
	transient public String[][] qdTexts = null;

	/** Commonly used constant in debugging. */
    private static final String IN = " in ";

	/** Constructor.
	 * @param	qSet	the question set (master-authored only) to be translated
	 * @param	language	the language into which to translate the questions
	 * @throws	ParameterException	if the question set is locally created
	 * @throws	DBException	if there's a problem reading the database
	 */
	public QSetTransln(QSet qSet, String language) 
			throws ParameterException, DBException {
		init(qSet, language);
	} // QSetTransln(QSet, String)

	/** Constructor.
	 * @param	qSet	the question set (master-authored only) to be translated
	 * @param	language	the language into which to translate the questions
	 * @param	translator	userID of the person doing the translating
	 * @throws	ParameterException	if the question set is locally created
	 * @throws	DBException	if there's a problem reading the database
	 */
	public QSetTransln(QSet qSet, String language, String translator) 
			throws ParameterException, DBException {
		translatorId = translator;
		init(qSet, language);
	} // QSetTransln(QSet, String, String)

	/** Called by constructors.
	 * @param	qSet	the question set (master-authored only) to be translated
	 * @param	language	the language into which to translate the questions
	 * @throws	ParameterException	if the question set is locally created
	 * @throws	DBException	if there's a problem reading the database
	 */
	public void init(QSet qSet, String language) 
			throws ParameterException, DBException {
		final int origSetId = qSet.getQSetId();
		if (origSetId < 0) {
			throw new ParameterException("Only question sets from the master "
					+ "database may be translated.");
		} // if qSet is locally created
		final QSet masterQSet = (qSet.isMasterEdit() 
				? qSet : new QSet(origSetId));
		setId = masterQSet.getQSetId();
		qSetDescr = masterQSet.getQSetDescr();
		setQs = masterQSet.getSetQs();
		setQIds = masterQSet.getSetQIds();
		getTranslations(language);
	} // QSetTransln(QSet, String, String)

	/** Returns description of this question set.
	 * @return	the description of this question set
	 */
	public QSetDescr getQSetDescr()		{ return qSetDescr; }
	/** Returns unique ID of this question set
	 * @return	unique ID of this question set
	 */
	public int getSetId()				{ return setId; }

	/** Returns light-load Qs of this question set.
	 * @return	array of questions
	 */
	public Question[] getQuestions() {
		return (setQs == null ? null 
				: setQs.toArray(new Question[setQs.size()]));
	} // getQuestions()

	/** Gets whether the question is of the type to have translatable question 
	 * data.
	 * @param	qNum	1-based number of the question in this question set
	 * @return	true if the question is of the type to have translatable
	 * question data.
	 */
	public boolean hasTranslatableQData(int qNum) {
		return setQs.get(qNum - 1).hasTranslatableQData();
	} // hasTranslatableQData(int)

	/** Reads translations of phrases associated with this question set in a
	 * particular language from the database.
	 * @param	language	language of translation
	 */
	private void getTranslations(String language) {
		final String SELF = "QSetTransln.getTranslations: ";
		final String CAUGHT = "Caught DBException when trying to get ";
		final int numQs = setQs.size();
		// get common Q statement
		try {
			header = TranslnRead.getHeader(setId, language);
			debugPrint(SELF + "common Q statement in ", language, ": ", header);
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + CAUGHT + "common Q statement of qSet ", 
					setId, IN, language, '.');
		} // try
		// get all Q statement translations (we already have the list of Q ids)
		qStmts = new String[numQs];
		try {
			debugPrint(SELF + "getting Q statements in ", language);
			qStmts = TranslnRead.getQuestionStatements(setQIds, language);
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + CAUGHT + "Q statements of qSet ", setId, 
					IN, language, '.');
		} // try
		// set up eval and qData arrays per each Q
		evalFeedbacks = new String[numQs][0];
		qdTexts = new String[numQs][0];
		final List<HashMap<Integer, Integer>> evalNumsByMajorIdsByQNums =
				new ArrayList<HashMap<Integer, Integer>>();
		final List<HashMap<Integer, Integer>> qdNumsByQDSerialNosByQNums =
				new ArrayList<HashMap<Integer, Integer>>();
		for (int qNum = 0; qNum < numQs; qNum++) {
			final Question oneQ = setQs.get(qNum);
			evalFeedbacks[qNum] = new String[oneQ.getNumEvaluators()];
			final HashMap<Integer, Integer> evalNumsByMajorIds =
					new HashMap<Integer, Integer>();
			evalNumsByMajorIdsByQNums.add(evalNumsByMajorIds);
			int evalNum = 1;
			for (final Evaluator eval : oneQ.getAllEvaluators()) {
				evalNumsByMajorIds.put(
						Integer.valueOf(eval.majorId),
						Integer.valueOf(evalNum++));
			} // for each evaluator
			final boolean getQData = oneQ.hasTranslatableQData(); 
			qdTexts[qNum] = new String[getQData ? oneQ.getNumQData(GENERAL) : 0];
			final HashMap<Integer, Integer> qdNumsByQDSerialNos =
					new HashMap<Integer, Integer>();
			qdNumsByQDSerialNosByQNums.add(qdNumsByQDSerialNos);
			if (getQData) {
				int qdNum = 1;
				for (final QDatum qDatum : oneQ.getQData(GENERAL)) {
					qdNumsByQDSerialNos.put(
							Integer.valueOf(qDatum.serialNo),
							Integer.valueOf(qdNum++));
				} // for each general qDatum
			} // if need to translate qData
		} // for each Q
		// get eval translations
		try {
			debugPrint(SELF + "getting eval feedbacks in ", language);
			TranslnRead.getFeedbacks(setQIds, evalFeedbacks, 
					evalNumsByMajorIdsByQNums, language);
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + CAUGHT + "eval feedbacks of qSet ", setId, 
					IN, language, '.');
		} // try
		// get qData translations
		try {
			debugPrint(SELF + "getting qData text in ", language);
			TranslnRead.getQData(setQIds, qdTexts, 
					qdNumsByQDSerialNosByQNums, language);
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + CAUGHT + "qData text of qSet ", setId, IN, 
					language, '.');
		} // try
	} // getTranslations(String)

	/** Sets translations of phrases associated with this question set, and 
	 * writes them to the database. Null values are omitted from consideration.
	 * @param	newHeader	new common question statement
	 * @param	newQStmts	new question statements
	 * @param	newFeedbacks	new evaluator feedbacks for each question and
	 * evaluator
	 * @param	newQDTexts	new text of question data for each question and
	 * qDatum
	 * @param	language	language of translations
	 * @throws	ParameterException	if there is no evaluator or qDatum with 
	 * that number
	 */
	public void setTranslations(String newHeader, String[] newQStmts,
			String[][] newFeedbacks, String[][] newQDTexts, String language) 
			throws ParameterException {
		final String SELF = "QSetTransln.setTranslations: ";
		// get new translations (not null) and identifying information;
		// empty translations will be erased
		final List<Integer> qStmtQIds = new ArrayList<Integer>();
		final List<String> qStmtTranslns = new ArrayList<String>();
		final List<String> fdbkQId_MajorIds = new ArrayList<String>();
		final List<String> fdbkTranslns = new ArrayList<String>();
		final List<String> qDataQIds_SerialNos = new ArrayList<String>();
		final List<String> qDataTranslns = new ArrayList<String>();
		for (int qNum = 0; qNum < newQStmts.length; qNum++) {
			final String newQStmt = newQStmts[qNum];
			if (newQStmt != null) {
				debugPrint(SELF + "Q", qNum + 1, " statement in ", 
						language, " = ", newQStmt);
				qStmtQIds.add(setQIds.get(qNum));
				qStmtTranslns.add(newQStmt);
			} // if the Q statement is new
			final int numEvals = newFeedbacks[qNum].length;
			for (int evalNum = 0; evalNum < numEvals; evalNum++) {
				final String newEvalFeedback = newFeedbacks[qNum][evalNum];
				if (newEvalFeedback != null) {
					final int majorId = 
							setQs.get(qNum).getEvaluator(evalNum + 1).majorId;
					debugPrint(SELF + "Q", qNum + 1, " evaluator ", evalNum + 1,
							" with majorId ", majorId, " has feedback in ", 
							language, " = ", newEvalFeedback);
					fdbkQId_MajorIds.add(
							Utils.toString(setQIds.get(qNum), '_', majorId));
					fdbkTranslns.add(newEvalFeedback);
				} // if the feedback is new
			} // for each eval translation
			for (int qdNum = 0; qdNum < newQDTexts[qNum].length; qdNum++) {
				final String newQDatum = newQDTexts[qNum][qdNum];
				if (newQDatum != null) {
					final int serialNo = setQs.get(qNum)
							.getQDatum(GENERAL, qdNum + 1).serialNo;
					debugPrint(SELF + "Q", qNum + 1, " qDatum ", qdNum + 1, 
							" with serialNo ", serialNo, IN, language, " = ", 
							newQDatum);
					qDataQIds_SerialNos.add(
							Utils.toString(setQIds.get(qNum), '_', serialNo));
					qDataTranslns.add(newQDatum);
				} // if the qdatum is new
			} // for each qDatum translation
		} // for each Q
		// store the new translations
		final String CAUGHT = "Caught DBException when trying to set new ";
		final TranslnWrite writer = new TranslnWrite(translatorId);
		if (newHeader != null) try {
			writer.setHeader(setId, newHeader, language);
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + CAUGHT + "header of qSet ", setId, IN, 
					language, '.');
		} // try if there's a new translation of the common Q statement
		if (!qStmtQIds.isEmpty()) try {
			debugPrint(SELF + "setting qStmts ", qStmtQIds, ", ", 
					qStmtTranslns);
			writer.setQuestionStatements(
					Utils.listToIntArray(qStmtQIds),
					qStmtTranslns.toArray(new String[qStmtTranslns.size()]), 
					language);
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + CAUGHT + "Q statements of qSet ", setId, 
					IN, language, '.');
		} // try if there's a new translation of a Q statement
		if (!fdbkQId_MajorIds.isEmpty()) try {
			debugPrint(SELF + "setting feedbacks ", fdbkQId_MajorIds, ", ", 
					fdbkTranslns);
			writer.setFeedbacks(fdbkQId_MajorIds.toArray(
						new String[fdbkQId_MajorIds.size()]),
					fdbkTranslns.toArray(new String[fdbkTranslns.size()]), 
					language);
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + CAUGHT + "feedbacks of qSet ", setId, 
					IN, language, '.');
		} // try if there's a new translation of feedback
		if (!qDataQIds_SerialNos.isEmpty()) try {
			debugPrint(SELF + "setting qData ", qDataQIds_SerialNos, ", ", 
					qDataTranslns);
			writer.setQData(qDataQIds_SerialNos.toArray(
						new String[qDataQIds_SerialNos.size()]),
					qDataTranslns.toArray(new String[qDataTranslns.size()]), 
					language);
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + CAUGHT + "question data of qSet ", setId, 
					IN, language, '.');
		} // try if there's a new translation of question data
	} // setTranslations(String, String[], String[][], String[][], String)

} // QSetTransln
