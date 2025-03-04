package com.epoch.db;

import static com.epoch.db.dbConstants.QuestionsRWConstants.*;
import static com.epoch.db.dbConstants.TranslationsConstants.*;
import com.epoch.evals.Evaluator;
import com.epoch.exceptions.DBException;
import com.epoch.qBank.QDatum;
import com.epoch.qBank.Question;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.session.QSet;
import com.epoch.translations.PhraseTransln;
import com.epoch.translations.TranslnsMap;
import com.epoch.utils.SortUtils;
import com.epoch.utils.Utils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Contains all database read and write operations pertaining to
 * translations.  Translations of ACE phrases have negative ID numbers 
 * derived from the hashcode; translations of question content have
 * positive ID numbers derived from a sequencer.
 */
public final class TranslnRead extends DBCommon implements QuestionConstants {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Utility variable to reference nonstatic constants. */
	private static final DBTables table = new DBTables();
	/** Parameter for getTranslatedParts(). */
	static final int HEADERS = 0;
	/** Parameter for getTranslatedParts(). */
	static final int QSTMTS = 1;
	/** Parameter for getTranslatedParts(). */
	static final int EVALS = 2;
	/** Parameter for getTranslatedParts(). */
	static final int QDATA = 3;

	/** A database field including the table prefix. */
	private static final String TRANSLN = TRANSLATIONS + DOT + PHRASE_TRANSLN;
	/** A database field including the table prefix. */
	private static final String LANG = TRANSLATIONS + DOT + PHRASE_LANG;
	/** A database field including the table prefix. */
	private static final String PHRASEID = TRANSLATIONS + DOT + PHRASE_ID;

	/** Pseudonym for a search result. */
	private static final String SRCH_RESULT1 = SRCH_RESULT + "1";
	/** Pseudonym for a search result. */
	private static final String SRCH_RESULT2 = SRCH_RESULT + "2";
	/** Pseudonym for a search result. */
	private static final String SRCH_RESULT3 = SRCH_RESULT + "3";
	/** Pseudonym for a search result. */
	private static final String SRCH_RESULT4 = SRCH_RESULT + "4";
	/** Divides topic name from qSet name. */
	private static final String DIV = ": ";

/* *********** Getting translations for ordinary users ****************/

	/** Gets translations of phrases from the database, replacing English
	 * phrases in the array with their translations.  If the English
	 * phrase is not in the database, adds it.
	 * @param	phrases	array of phrases to be translated; modified by this
	 * method
	 * @param	userLangs	languages preferred by the user in order of
	 * preference
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static void translate(String[] phrases, String[] userLangs)
			throws DBException {
		translate(phrases, userLangs, PhraseTransln.ADD_NEW_PHRASES_TO_DB);
	} // translate(String[], String[])

	/** Gets translations of phrases from the database, replacing English
	 * phrases in the array with their translations.
	 * @param	phrases	array of phrases to be translated; modified by this
	 * method
	 * @param	userLangs	languages preferred by the user in order of
	 * preference
	 * @param	addNewPhrasesToDB	whether to add new English phrases to the
	 * database
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static void translate(String[] phrases, String[] userLangs,
			boolean addNewPhrasesToDB) throws DBException {
		final String SELF = "TranslnRead.translate: ";
		if (phrases.length == 0) {
			debugPrint(SELF + "no phrases to translate.");
			return;
		} // if there are no phrases
		if (userLangs.length == 0) {
			debugPrint(SELF + "no preferred languages other than English.");
			return;
		} // if there are no languages
		final List<Integer> phraseIds = new ArrayList<Integer>();
		final List<String> phrasesList = new ArrayList<String>();
		final Map<Integer, String> englPhrasesByIds = 
				new HashMap<Integer, String>();
		for (final String phrase : phrases) {
			if (!isEmpty(phrase)) {
				final String modPhrase = phrase.trim();
				final Integer phraseIdObj = getPhraseId(modPhrase);
				/* debugPrint(SELF + "got phrase ID ", phraseIdObj, 
						" for phrase: ", modPhrase); /**/
				phraseIds.add(phraseIdObj);
				phrasesList.add(modPhrase);
				englPhrasesByIds.put(phraseIdObj, modPhrase);
			} // if phrase exists
		} // for each phrase
		if (phraseIds.isEmpty()) return;
		/* debugPrint(SELF + "englPhrasesByIds: ", englPhrasesByIds); /**/
		/* debugPrint(SELF + "phrasesList: ", phrasesList); /**/
		final String qry = toString(
				SELECT, joinAll(
					PHRASE_ID,
					PHRASE_LANG,
					PHRASE_TRANSLN),
				FROM + TRANSLATIONS
				+ WHERE + PHRASE_ID + IN, parensQMarks(phraseIds),
				AND + PHRASE_LANG + IN, parensQMarks(userLangs),
				ORDER_BY + PHRASE_ID);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				phraseIds,
				userLangs);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			int phraseId = 0;
			if (rs.next()) while (!rs.isAfterLast()) {
				phraseId = rs.getInt(PHRASE_ID);
				final List<String[]> translations = new ArrayList<String[]>();
				while (!rs.isAfterLast()
						&& phraseId == rs.getInt(PHRASE_ID)) {
					final String language = rs.getString(PHRASE_LANG);
					final String translation = rs.getString(PHRASE_TRANSLN);
					/* debugPrint(SELF + "for phraseId ", phraseId, 
							", translation in ", language, ": ", translation); /**/
					translations.add(new String[] {language, translation});
					rs.next();
				} // while there are more translations of this phrase
				final String bestTransln = 
						getBestTranslation(translations, userLangs);
				/* debugPrint(SELF, bestTransln != null 
				 		? toString("bestTransln = ", bestTransln)
						: "could not find best translation"); /**/
				final Integer phraseIdObj = Integer.valueOf(phraseId);
				final String origEnglPhrase = englPhrasesByIds.get(phraseIdObj);
				final int posn = phrasesList.indexOf(origEnglPhrase);
				/* debugPrint(SELF + "original English phrase ", origEnglPhrase,
				 		" with phraseId ", phraseIdObj, " has position ",
						posn, " in phrasesList"); /**/
				if (bestTransln != null && posn >= 0) {
					phrases[posn] = bestTransln;
					/* debugPrint(SELF + "found: ", bestTransln, "\nas best "
							+ "translation of: ", origEnglPhrase); /**/
					englPhrasesByIds.remove(phraseIdObj);
				} /* else debugPrint(SELF + "found no translation among ", 
						userLangs, " for: ", origEnglPhrase); /**/
			} // while more translations are found
			// we've acquired all translations for the final phrase
			/* if (phraseId == 0) debugPrint(SELF + "no translations in ", 
					userLangs, " found for phrases: ", phrases); /**/
			if (addNewPhrasesToDB) {
				TranslnWrite.addEnglish(con, englPhrasesByIds);
			} // if should add untranslatable English phrases to database
		} catch (SQLException e) {
			alwaysPrint(SELF + "couldn't get phrase in non-English languages");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			alwaysPrint(SELF + "couldn't get phrase in non-English languages");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try
	} // translate(String[], String[], boolean)

	/** Gets translations of headers, question statements, evaluator feedbacks,
	 * or question data from the database and replaces the English versions.
	 * @param	con	database connection
	 * @param	phraseIds	ID numbers of the phrases to be translated
	 * @param	itemIdsByPhraseIds	list of arrays of identifying information of 
	 * the items whose phrases are being translated, keyed by the phrase ID. The
	 * size of the identifying information depends on <code>itemType</code>:
	 * <br>when <code>itemType</code> == <code>HEADERS</code>,
	 * its one member is the question set's ID;
	 * <br>when <code>itemType</code> == <code>QSTMTS</code>,
	 * its one member is the question's unique ID;
	 * <br>when <code>itemType</code> == <code>EVALS</code>,
	 * its two members are the question's unique ID and the evaluator's major ID
	 * (serial number);
	 * <br>when <code>itemType</code> == <code>QDATA</code>,
	 * its three members are the question's unique ID, the question datum's
	 * serial number, and the type of data (Marvin or other).
	 * @param	questionsById	map of questions being retrieved whose 
	 * components are being translated, keyed by IDs
	 * @param	headersByQSetIds	map of common question statements 
	 * associated with the questions whose components are being translated, 
	 * keyed by question set IDs
	 * @param	userLangs	languages preferred by the user in order of
	 * preference
	 * @param	itemType	whether headers, question statements, evaluator
	 * feedbacks, or question data are being translated
	 */
	static void getTranslatedParts(Connection con, List<Integer> phraseIds,
			Map<Integer, ArrayList<int[]>> itemIdsByPhraseIds,
			Map<Integer, Question> questionsById,
			Map<Integer, String> headersByQSetIds,
			String[] userLangs, int itemType) {
		if (Utils.isEmpty(phraseIds)) return;
		if (Utils.isEmpty(userLangs)) return;
		final String SELF = "TranslnRead.getTranslatedParts: ";
		final List<int[]> phraseIdsArrs = getIntGroups(phraseIds);
		debugPrint(SELF + "phrases to translate grouped into ",
				phraseIdsArrs.size(), " group(s) of < ~1000.");
		for (final int[] phraseIdsArr : phraseIdsArrs) {
			final String qry = toString(
					SELECT, joinAll(
						PHRASE_ID,
						PHRASE_LANG,
						PHRASE_TRANSLN),
					FROM + TRANSLATIONS
					+ WHERE + PHRASE_ID + IN, parensQMarks(phraseIdsArr),
					AND + PHRASE_LANG + IN, parensQMarks(userLangs),
					ORDER_BY + PHRASE_ID);
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
					phraseIdsArr,
					userLangs);
			debugPrint(SELF, sql_vals);
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try {
				stmt = getStatement(con, sql_vals);
				rs = stmt.executeQuery();
				final List<String[]> translations = new ArrayList<String[]>();
				int prevPhraseId = 0;
				while (true) {
					final boolean afterLast = !rs.next();
					final int phraseId = (afterLast ? 0
							: rs.getInt(PHRASE_ID));
					if (!Utils.among(prevPhraseId, phraseId, 0)) {
						final List<int[]> itemIdsList = itemIdsByPhraseIds.get(
								Integer.valueOf(prevPhraseId));
						debugPrint(SELF + "phraseID ", prevPhraseId,
								" corresponding to itemIdsList ", itemIdsList, 
								": ", translations.size(), 
								" translation(s) found.");
						modifyItem(translations, userLangs, itemIdsList, 
								questionsById, headersByQSetIds, itemType);
						translations.clear();
					} // if passed last translation of previous phrase
					if (afterLast) break;
					final String language = rs.getString(PHRASE_LANG);
					final String translation = rs.getString(PHRASE_TRANSLN);
					debugPrint(SELF + "for phraseID ", phraseId,
							" and language ", language,
							", translation = ", translation);
					translations.add(new String[] {language, translation});
					prevPhraseId = phraseId;
				} // while more translations are found
			} catch (Exception e0) {
				alwaysPrint(SELF + "couldn't get ",
						(itemType == HEADERS ? "common question statements"
							: itemType == QSTMTS ? "question statements"
							: itemType == EVALS ? "evaluator feedbacks"
							: "question data"), " in non-English languages");
				e0.printStackTrace();
			} finally {
				closeStmtAndRs(stmt, rs);
			} // try
		} // for each bunch of ~1000 phrases
	} // getTranslatedParts(Connection, List<Integer>, Map<Integer, int[]>, 
	//			Map<Integer, Question>, Map<Integer, String>, String[], int)

	/** From all translations of a particular phrase in all languages, gets the
	 * translation in a preferred language (if any) and replaces the English
	 * version.
	 * @param	translations	list of two-member arrays, each containing a
	 * language and the translation of the phrase into that language
	 * @param	userLangs	languages preferred by the user in order of
	 * preference
	 * @param	itemIdsList	list of arrays of identifying information about 
	 * each item (with the same phrase) being translated:
	 * <br>when <code>itemType</code> == <code>HEADERS</code>,
	 * its one member is the question set's ID;
	 * <br>when <code>itemType</code> == <code>QSTMTS</code>,
	 * its one member is the question's unique ID;
	 * <br>when <code>itemType</code> == <code>EVALS</code>,
	 * its two members are the question's unique ID and the evaluator's major ID
	 * (serial number);
	 * <br>when <code>itemType</code> == <code>QDATA</code>,
	 * its three members are the question's unique ID, the question datum's
	 * serial number, and the type of data (Marvin or other).
	 * @param	questionsById	map of questions being retrieved whose 
	 * components are being translated, keyed by IDs
	 * @param	headersByQSetIds	map of common question statements 
	 * associated with the questions whose components are being translated, 
	 * keyed by question set IDs
	 * @param	itemType	whether headers, question statements, evaluator
	 * feedbacks, or question data are being translated
	 */
	private static void modifyItem(List<String[]> translations,
			String[] userLangs, List<int[]> itemIdsList, 
			Map<Integer, Question> questionsById,
			Map<Integer, String> headersByQSetIds, int itemType) {
		final String SELF = "TranslnRead.modifyItem: ";
		if (translations.isEmpty()) return;
		debugPrint(SELF + "itemType = ", itemType == HEADERS ? "HEADERS"
					: itemType == QSTMTS ? "QSTMTS" : itemType == EVALS
					? "EVALS" : itemType == QDATA ? "QDATA" : "unknown",
				", itemIdsList = ", itemIdsList, ", number of translations = ",
				translations.size());
		final String bestTransln = getBestTranslation(translations, userLangs);
		if (bestTransln != null) {
			for (final int[] itemIds : itemIdsList) {
				if (itemType == HEADERS) {
					final int qSetId = itemIds[0];
					debugPrint(SELF + "assigning translation ", bestTransln,
							" to header of qSetId ", qSetId);
					headersByQSetIds.put(Integer.valueOf(qSetId), bestTransln);
				} else if (itemType == QSTMTS) {
					final int qId = itemIds[0];
					debugPrint(SELF + "assigning translation ", bestTransln,
							" to Q statement of Q", qId);
					final Question question = 
							questionsById.get(Integer.valueOf(qId));
					// modify statement with header if necessary
					final int setId = question.getQSetId();
					final String header = 
							headersByQSetIds.get(Integer.valueOf(setId));
					final String newBestTransln = 
							QuestionRead.appendHeader(header, bestTransln);
					debugPrint(SELF + "header-modified bestTransln = ", 
							newBestTransln);
					question.setStatement(newBestTransln);
					debugPrint(SELF + "modified question statement = ",
							question.getStatement());
				} else if (itemType == EVALS) {
					final int qId = itemIds[0];
					final int evalMajorId = itemIds[1];
					final Question question = 
							questionsById.get(Integer.valueOf(qId));
					debugPrint(SELF + "assigning translation ", bestTransln, 
							" to evaluator ", evalMajorId, " of Q", qId);
					for (final Evaluator eval : question.getAllEvaluators()) {
						if (eval.majorId == evalMajorId) {
							eval.feedback = bestTransln;
							break;
						} // if evaluator matches
					} // for each evaluator
				} else if (itemType == QDATA) {
					final int qId = itemIds[0];
					final int qdSerialNo = itemIds[1];
					final int qdType = itemIds[2];
					final Question question = 
							questionsById.get(Integer.valueOf(qId));
					debugPrint(SELF + "assigning translation ", bestTransln,
							" to qDatum ", qdSerialNo, " of Q", qId);
					for (final QDatum qDatum : question.getQData(GENERAL)) {
						if (qDatum.serialNo == qdSerialNo) {
							if (qdType == QDatum.MARVIN) {
								qDatum.name = bestTransln;
							} else qDatum.data = bestTransln;
							break;
						} // if the qDatum matches
					} // for each qDatum
				} else {
					Utils.alwaysPrint(SELF + "bad itemType");
				} // if itemType
			} // for each item with that translation
		} else debugPrint(SELF + "no translation in any of user's languages.");
	} // modifyItem(List<String[]>, String[], int[], Map<Integer, Question>, 
	//			Map<Integer, String>, int)

	/** Gets the optimum translation from a list.
	 * @param	translations	list of two-member arrays, each containing a
	 * language and the translation of the phrase into that language
	 * @param	userLangs	userLangs preferred by this user in order of
	 * preference
	 * @return	a translated phrase, or null if none was found
	 */
	private static String getBestTranslation(List<String[]> translations,
			String[] userLangs) {
		String bestTransln = null;
		final List<String> translnLangs = new ArrayList<String>();
		for (final String[] translation : translations) {
			translnLangs.add(translation[0]);
		} // for each translation
		for (final String userLang : userLangs) {
			final int posn = translnLangs.indexOf(userLang);
			if (posn >= 0) {
				bestTransln = translations.get(posn)[1];
				break;
			} // if a translation in the language was found
		} // for each user language
		return bestTransln;
	} // getBestTranslation(List<String[]>, String[])

	/** Gets a language's two-letter 
	 * <a href="http://www.loc.gov/standards/iso639-2/php/code_list.php">ISO 639-2 code</a> 
	 * from the database.  Note: We enter records into the database manually.
	 * @param	language	a language
	 * @return	the ISO 639-2 code, or null if not found
	 */
	public static String getCode(String language) {
		String code = null;
		if (language == null) return code;
		final String qry =
				SELECT + LANG_CODE_CODE
				+ FROM + LANG_CODES
				+ WHERE + LANG_CODE_NAME + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				language);
		debugPrint("TranslnRead.getCode: ", sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) code = rs.getString(LANG_CODE_CODE);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeConnection(con, stmt, rs);
		}
		return code;
	} // getCode(String)

/* *********** Getting translations for translators ****************/

	/** Gets the common question statements of the given question set translated
	 * into a language.
	 * @param	qSetId	the unique ID of the question set
	 * @param	language	the language of translation
	 * @return	array containing the translated common question statement
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String getHeader(int qSetId, String language) 
			throws DBException {
		String headerXlatn = null;
		final String qry = toString(
				SELECT + PHRASE_TRANSLN
				+ FROM + TRANSLATIONS
				+ WHERE + PHRASE_LANG + EQUALS + QMARK
				+ AND + PHRASE_ID + IN, parens(
					SELECT + QSET_COMMONQSTMT_ID
					+ FROM + QSETS
					+ WHERE + QSET_ID + EQUALS + QMARK));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				language,
				qSetId);
		debugPrint("TranslnRead.getHeaders: ", sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) headerXlatn = rs.getString(PHRASE_TRANSLN);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return headerXlatn;
	} // getHeaders(int, String)

	/** Gets the question statements of the given questions translated
	 * into a language.
	 * @param	qIds	unique IDs of the questions
	 * @param	language	the language of translation
	 * @return	array of translated question statements in the same order as the
	 * question IDs; null if a translation was not found
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[] getQuestionStatements(List<Integer> qIds,
			String language) throws DBException {
		final String SELF = "TranslnRead.getQuestionStatements: ";
		final String[] qStmts = new String[qIds.size()];
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		if (!Utils.isEmpty(qIds)) try {
			final StringBuilder tQID = getBuilder(table.QUESTIONS, DOT, Q_QID);
			final String qry = toString(
					SELECT, joinAll(
						TRANSLN,
						tQID),
					FROM, joinAll(
						TRANSLATIONS, 
						table.QUESTIONS),
					WHERE + PHRASEID + EQUALS, table.QUESTIONS, DOT, Q_STMT_ID,
					AND, tQID, IN, parensQMarks(qIds),
					AND + LANG + EQUALS + QMARK
					+ ORDER_BY, tQID);
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
					qIds,
					language);
			debugPrint(SELF, sql_vals);
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final String xlatn = rs.getString(PHRASE_TRANSLN);
				final int qId = rs.getInt(Q_QID);
				final int qNum = qIds.indexOf(qId);
				debugPrint(SELF + "qId ", qId, ", xlatn = ", xlatn);
				qStmts[qNum] = xlatn;
			} // while there are more results
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return qStmts;
	} // getQuestionStatements(List<Integer>, String)

	/** Gets the feedback of the given evaluators data translated into a 
	 * language, storing the retrieved data in a method parameter.
	 * @param	qIds	unique IDs of the questions
	 * @param	evalFeedbacks	 array of arrays in which to store translations 
	 * of evaluator feedbacks by question number and evaluator number; modified 
	 * by method
	 * @param	evalNumsByMajorIdsByQNums	for each question, table of
	 * evaluator numbers (0, 1, 2, etc.) keyed by majorIds (may skip some
	 * numbers)
	 * @param	language	the language of translation
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static void getFeedbacks(List<Integer> qIds,
			String[][] evalFeedbacks, 
			List<HashMap<Integer, Integer>> evalNumsByMajorIdsByQNums,
			String language) throws DBException {
		final String SELF = "TranslnRead.getFeedbacks: ";
		if (Utils.isEmpty(qIds)) return;
		final StringBuilder tQID = getBuilder(table.EVALUATORS, DOT + EVAL_QID);
		final StringBuilder tMAJORID = 
				getBuilder(table.EVALUATORS, DOT + EVAL_MAJORID);
		final String qry = toString(
				SELECT, joinAll(
					tQID,
					tMAJORID,
					TRANSLN),
				FROM, joinAll(
					TRANSLATIONS, 
					table.EVALUATORS),
				WHERE + PHRASEID + EQUALS, 
					table.EVALUATORS, DOT, EVAL_FEEDBACKID
				+ AND, tQID, IN, parensQMarks(qIds),
				AND, table.EVALUATORS, DOT + EVAL_MINORID + IS_ZERO
				+ AND + LANG + EQUALS + QMARK
				+ ORDER_BY, joinAll(
					tQID,
					tMAJORID));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				qIds,
				language);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final String xlatn = rs.getString(PHRASE_TRANSLN);
				final int qId = rs.getInt(EVAL_QID);
				final int qNum = qIds.indexOf(qId); // 0-based
				final HashMap<Integer, Integer> evalNumsByMajorIds =
						evalNumsByMajorIdsByQNums.get(qNum);
				final int evalMajorId = rs.getInt(EVAL_MAJORID); // 1-based
				final int evalNum = evalNumsByMajorIds.get(
						Integer.valueOf(evalMajorId)).intValue();
				debugPrint(SELF + "qId ", qId, ", qNum ", qNum + 1, 
						", evalMajorId ", evalMajorId,
						", evalNum ", evalNum, ", xlatn = ", xlatn);
				evalFeedbacks[qNum][evalNum - 1] = xlatn;
			} // while there are more retrieved feedbacks
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
	} // getFeedbacks(List<Integer>, String[][], String)

	/** Gets the data or molecule names of the given question data translated
	 * into a language, storing the retrieved data in a method parameter.
	 * @param	qIds	unique IDs of the questions
	 * @param	qdTexts	 array of arrays in which to store translations of
	 * question data by question number and datum number; modified by method
	 * @param	qdNumsByQDSerialNosByQNums	for each question, table of
	 * question datum numbers (0, 1, 2, etc.) keyed by serialNos (may skip some
	 * numbers)
	 * @param	language	the language of translation
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static void getQData(List<Integer> qIds, String[][] qdTexts, 
			List<HashMap<Integer, Integer>> qdNumsByQDSerialNosByQNums,
			String language) throws DBException {
		final String SELF = "TranslnRead.getQData: ";
		if (Utils.isEmpty(qIds)) return;
		final StringBuilder tQID = getBuilder(table.QUESTIONDATA, DOT + QD_QID);
		final StringBuilder tNUM = getBuilder(table.QUESTIONDATA, DOT + QD_NUM);
		final String qry = toString(
				SELECT, joinAll(
					tQID,
					tNUM,
					TRANSLN),
				FROM, joinAll(
					TRANSLATIONS, 
					table.QUESTIONDATA),
				WHERE + PHRASEID + EQUALS, table.QUESTIONDATA, DOT + QD_TEXT_ID
				+ AND, tQID, IN, parensQMarks(qIds),
				AND + LANG + EQUALS + QMARK
				+ ORDER_BY, joinAll(
					tQID,
					tNUM));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				qIds,
				language);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			// qIds are in numerical order in question set, so database call
			// placed question data translations in desired order
			while (rs.next()) {
				final String xlatn = rs.getString(PHRASE_TRANSLN);
				final int qId = rs.getInt(QD_QID);
				final int qNum = qIds.indexOf(qId); // 0-based
				final HashMap<Integer, Integer> qdNumsByQDSerialNos =
						qdNumsByQDSerialNosByQNums.get(qNum);
				final int dataSerialNo = rs.getInt(QD_NUM); // 1-based
				final int dataNum = qdNumsByQDSerialNos.get(
						Integer.valueOf(dataSerialNo)).intValue();
				debugPrint(SELF + "qId ", qId, ", qNum ", qNum + 1, 
						", dataSerialNo ", dataSerialNo,
						", dataNum ", dataNum, ", xlatn = ", xlatn);
				qdTexts[qNum][dataNum - 1] = xlatn;
			} // while there are more results
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
	} // getQData(List<Integer>, String[][], String)

	/** Gets the common question statements of the given question set translated
	 * into all languages.
	 * @param	qSetId	the unique ID of the question set
	 * @return	TranslnsMap of common question statement
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static TranslnsMap getAllHeaderTranslns(int qSetId) 
			throws DBException {
		final TranslnsMap translns = new TranslnsMap();
		final String qry = toString(
				SELECT, joinAll(
					PHRASE_LANG, 
					PHRASE_TRANSLN),
				FROM + TRANSLATIONS
				+ WHERE + PHRASE_ID + IN, parens(
					SELECT + QSET_COMMONQSTMT_ID
					+ FROM + QSETS
					+ WHERE + QSET_ID + EQUALS + QMARK),
				ORDER_BY + PHRASE_LANG);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				qSetId);
		debugPrint("TranslnRead.getAllHeaderTranslns: ", sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final String lang = rs.getString(PHRASE_LANG);
				final String transln = rs.getString(PHRASE_TRANSLN);
				translns.put(lang, transln);
			} // for each result
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return translns;
	} // getAllHeaderTranslns(int)

	/** Gets the question statement of the given question translated
	 * into all languages.
	 * @param	qId	unique ID of the question
	 * @return	TranslnsMap of question statement
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static TranslnsMap getAllQStmtTranslns(int qId) 
			throws DBException {
		final TranslnsMap qStmts = new TranslnsMap();
		final StringBuilder qTable = getBuilder(table.QUESTIONS, DOT);
		final String qry = toString(
				SELECT, joinAll( 
					LANG, 
					TRANSLN),
				FROM, joinAll(
					TRANSLATIONS, 
					table.QUESTIONS),
				WHERE + PHRASEID + EQUALS, qTable, Q_STMT_ID
				+ AND, qTable, Q_QID + EQUALS + QMARK
				+ ORDER_BY + LANG);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				qId);
		debugPrint("TranslnRead.getAllQStmtTranslns: ", sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final String lang = rs.getString(PHRASE_LANG);
				final String xlatn = rs.getString(PHRASE_TRANSLN);
				qStmts.put(lang, xlatn);
			} // for each result
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return qStmts;
	} // getAllQStmtTranslns(int)

	/** Gets the evaluator feedbacks of the given question translated
	 * into all languages.
	 * @param	qId	unique ID of the question
	 * @return	TranslnsMaps of feedback, mapped by 1-based major 
	 * evaluator number
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<Integer, TranslnsMap> 
			getAllFeedbackTranslns(int qId) throws DBException {
		final Map<Integer, TranslnsMap> feedbacks = 
				new HashMap<Integer, TranslnsMap>();
		final StringBuilder tMAJORID = 
				getBuilder(table.EVALUATORS, DOT + EVAL_MAJORID);
		final String qry = toString(
				SELECT, joinAll( 
					tMAJORID,
					LANG, 
					TRANSLN),
				FROM, joinAll(
					TRANSLATIONS, 
					table.EVALUATORS),
				WHERE + PHRASEID + EQUALS,
					table.EVALUATORS, DOT + EVAL_FEEDBACKID,
				AND, table.EVALUATORS, DOT + EVAL_QID + EQUALS + QMARK
				+ AND, table.EVALUATORS, DOT + EVAL_MINORID, IS_ZERO 
				+ ORDER_BY, joinAll(
					tMAJORID,
					LANG));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				qId);
		debugPrint("TranslnRead.getAllFeedbackTranslns: ", sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			TranslnsMap oneEvalTranslns = new TranslnsMap();
			int prevEvalNum = 0;
			while (true) {
				final boolean afterLast = !rs.next();
				final int evalNum = (afterLast ? 0
						: rs.getInt(EVAL_MAJORID));
				if (!Utils.among(prevEvalNum, evalNum, 0)) {
					feedbacks.put(Integer.valueOf(prevEvalNum), 
							oneEvalTranslns);
					oneEvalTranslns = new TranslnsMap();
				} // if passed last translation of previous feedback
				if (afterLast) break;
				final String lang = rs.getString(PHRASE_LANG);
				final String xlatn = rs.getString(PHRASE_TRANSLN);
				oneEvalTranslns.put(lang, xlatn);
				prevEvalNum = evalNum;
			} // while there are more retrieved feedbacks
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return feedbacks;
	} // getAllFeedbackTranslns(int)

	/** Gets the question data of the given question translated into all 
	 * languages.
	 * @param	qId	unique ID of the question
	 * @return	TranslnsMaps of question data, mapped by 1-based serial
	 * number 
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<Integer, TranslnsMap> 
			getAllQDataTranslns(int qId) throws DBException {
		final Map<Integer, TranslnsMap> texts = 
				new HashMap<Integer, TranslnsMap>();
		final StringBuilder tNUM = getBuilder(table.QUESTIONDATA, DOT, QD_NUM);
		final String qry = toString(
				SELECT, joinAll(
					tNUM,
					LANG, 
					TRANSLN),
				FROM, joinAll( 
					TRANSLATIONS, 
					table.QUESTIONDATA),
				WHERE + PHRASEID + EQUALS,
					table.QUESTIONDATA, DOT + QD_TEXT_ID
				+ AND, table.QUESTIONDATA, DOT + QD_QID + EQUALS + QMARK
				+ ORDER_BY, joinAll(
					tNUM,
					LANG));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				qId);
		debugPrint("TranslnRead.getAllQDataTranslns: ", sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			// qIds are in numerical order in question set, so database call
			// placed question data translations in desired order
			TranslnsMap oneQDTranslns = new TranslnsMap();
			int prevDataNum = 0;
			while (true) {
				final boolean afterLast = !rs.next();
				final int dataNum = (afterLast ? 0
						: rs.getInt(QD_NUM));
				if (!Utils.among(prevDataNum, dataNum, 0)) {
					texts.put(Integer.valueOf(prevDataNum), oneQDTranslns);
					oneQDTranslns = new TranslnsMap();
				} // if passed last translation of previous qDatum
				if (afterLast) break;
				final String lang = rs.getString(PHRASE_LANG);
				final String xlatn = rs.getString(PHRASE_TRANSLN);
				oneQDTranslns.put(lang, xlatn);
				prevDataNum = dataNum;
			} // while there are more retrieved qData
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return texts;
	} // getAllQDataTranslns(int)

	/** Gets all phrases translated into a language (usually English).
	 * @param	language	the language of translation
	 * @return	array of phrases in a particular language (usually English)
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[] getAllPhrases(String language) throws DBException {
		final String SELF = "TranslnRead.getAllPhrases: ";
		final String qry =
				SELECT + PHRASE_TRANSLN
				+ FROM + TRANSLATIONS
				+ WHERE + PHRASE_ID + NOT_MORE_THAN + '0'
				+ AND + PHRASE_LANG + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				language);
		debugPrint(SELF, sql_vals);
		final List<Object> translations = new ArrayList<Object>();
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final String transln = rs.getString(PHRASE_TRANSLN);
				if (!isEmpty(transln)) translations.add(transln);
			} // while
			final List<Comparable<?>> lcTranslns = new ArrayList<Comparable<?>>();
			for (final Object transln : translations) {
				String lcTransln = ((String) transln).toLowerCase(Locale.ENGLISH);
				while (true) {
					if (isEmpty(lcTransln)) break;
					final char char1st = lcTransln.charAt(0);
					if (Utils.isAlphanumeric(char1st)) break;
					else if (char1st == '<') {
						final int posn = lcTransln.indexOf('>');
						if (posn > 0 && posn < lcTransln.length() - 1) {
							lcTransln = lcTransln.substring(posn + 1);
						} else break;
					} else lcTransln = lcTransln.substring(1);
				} // while the translation doesn't begin with a letter or digit
				lcTranslns.add(lcTransln);
			} // for each sort key
			SortUtils.sort(translations, lcTranslns);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		debugPrint(SELF + "after sorting, returning ",
				translations.size(), " phrases in ", language);
		final String[] translns = new String[translations.size()];
		int tNum = 0;
		for (final Object transln : translations) {
			translns[tNum++] = (String) transln;
		} // for each translation
		return translns;
	} // getAllPhrases(String)

	/** Gets certain English phrases' translations into a language.
	 * @param	phrases	English phrases used in ACE
	 * @param	language	the language of translation
	 * @return	array of translated phrases in the same order as the
	 * English phrases; null if a translation was not found
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[] getAllPhrases(String[] phrases,
			String language) throws DBException {
		final String SELF = "TranslnRead.getAllPhrases: ";
		if (isEmpty(phrases)) return new String[0];
		final String qry = toString(
				SELECT, joinAll(
					PHRASE_TRANSLN,
					PHRASE_ID),
				FROM + TRANSLATIONS
				+ WHERE + PHRASE_ID + NOT_MORE_THAN + '0'
				+ AND + PHRASE_LANG + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				language);
		debugPrint(SELF, sql_vals);
		final List<Integer> phraseIds = new ArrayList<Integer>();
		for (final String phrase : phrases) phraseIds.add(getPhraseId(phrase));
		final String[] translations = new String[phrases.length];
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final String transln = rs.getString(PHRASE_TRANSLN);
				final int phraseId = rs.getInt(PHRASE_ID);
				final int posn = phraseIds.indexOf(Integer.valueOf(phraseId));
				if (posn >= 0) translations[posn] = transln;
			} // while more results are found
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return translations;
	} // getAllPhrases(String[], String)

	/** Calculates an ID number (always negative) from a String.  Used to get ID
	 * numbers for ACE phrases.
	 * @param	phrase	String to get an ID number from
	 * @return	an ID number
	 */
	public static Integer getPhraseId(String phrase) {
		return Integer.valueOf(-Math.abs(phrase.hashCode()));
		// even if phrase.hashCode() == MIN_INT, we are OK.
	} // getPhraseId(String)

	/** Gets the SQL query to get all languages from the translations table.
	 * @return	SQL query to get all languages from the translations table
	 */
	static String SELECT_ALL_LANGS() {
		return SELECT + PHRASE_LANG + FROM + TRANSLATIONS;
	} // SELECT_ALL_LANGS()

/* *********** Getting statistics of translations ****************/

	/** Counts the number of translatable ACE phrases.
	 * @return	the total number of translatable phrases
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int countTranslatablePhrases() throws DBException {
		final String SELF = "TranslnRead.countTranslatablePhrases: ";
		int ct = 0;
		final String qry = toString(
				SELECT, count(), AS + SRCH_RESULT
				+ FROM, parens( 
					SELECT_UNIQUE + PHRASE_ID
					+ FROM + TRANSLATIONS
					+ WHERE + PHRASE_ID + IS_NEGATIVE));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				ct = rs.getInt(SRCH_RESULT);
			} // if there's a result
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return ct;
	} // countTranslatablePhrases()

	/** Gets SQL to get ID numbers of topics whose contents should not be 
	 * translated.
	 * @return	the SQL
	 */
	private static String getExcludedTopicIDsSQL() {
		return toString(
				SELECT + TOPIC_ID 
				+ FROM + TOPICS
				+ WHERE + TOPIC_NAME + LIKE, quotes("CHE%"),
				OR + TOPIC_NAME + IN, parensJoinQuotes(
					"Development questions",
					"Questions from AWRORM"));
	} // getExcludedTopicIDsSQL()

	/** Counts the number of question set headers (common question statements), 
	 * excluding headers from certain topics.
	 * @return	the total number of headers (excluding those in sets not to be 
	 * translated).
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int countTranslatableHeaders() throws DBException {
		final String SELF = "TranslnRead.countTranslatableHeaders: ";
		int ct = 0;
		final String qry = toString(
				SELECT, count(), AS + SRCH_RESULT
				+ FROM, parensBuild( 
					SELECT_UNIQUE, clobToString(QSET_COMMONQSTATEMENT),
					FROM + QSETS
					+ WHERE + QSET_TOPIC_ID + NOT + IN,
						parens(getExcludedTopicIDsSQL())));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				ct = rs.getInt(SRCH_RESULT);
			} // while more results are found
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return ct;
	} // countTranslatableHeaders()

	/** Counts the number of question set headers (common question statements) 
	 * that have been translated into a language.
	 * @param	lang	the language
	 * @return	the number of translated headers that have been translated into
	 * the language
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int countTranslatedHeaders(String lang) throws DBException {
		final String SELF = "TranslnRead.countTranslatedHeaders: ";
		int ct = 0;
		final String qry = toString(
				SELECT, count(), AS + SRCH_RESULT
				+ FROM + TRANSLATIONS
				+ WHERE + PHRASE_LANG + EQUALS + QMARK
				+ AND + PHRASE_ID + IN, parens(
					SELECT + QSET_COMMONQSTMT_ID
					+ FROM + QSETS));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				lang);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				ct = rs.getInt(SRCH_RESULT);
			} // while more results are found
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return ct;
	} // countTranslatedHeaders(String)

	/** Gets SQL to get ID numbers of question sets whose contents should not 
	 * be translated.
	 * @return	the SQL
	 */
	private static String getExcludedQSetIDsSQL() {
		return toString(
				SELECT + QSET_ID 
				+ FROM + QSETS
				+ WHERE + QSET_TOPIC_ID + IN, 
					parens(getExcludedTopicIDsSQL()));
	} // getExcludedQSetIDsSQL()

	/** Counts the number of question statements, excluding those from 
	 * certain topics.
	 * @return	the total number of unique question statements
	 * (excluding those in sets not to be translated)
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int countTranslatableQStmts() throws DBException {
		final String SELF = "TranslnRead.countTranslatableQStmts: ";
		int ct = 0;
		final String qry = toString(
				SELECT, count(), AS + SRCH_RESULT
				+ FROM, parensBuild( 
					SELECT_UNIQUE, clobToString(Q_STATEMENT),
					FROM, table.QUESTIONS,
					WHERE + Q_QSET + NOT + IN,
						parens(getExcludedQSetIDsSQL())));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				ct = rs.getInt(SRCH_RESULT);
			} // while more results are found
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return ct;
	} // countTranslatableQStmts()

	/** Counts the number of unique question statements translated into a 
	 * language.
	 * @param	lang	the language
	 * @return	the total number of unique question statements translated into 
	 * the language 
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int countTranslatedQStmts(String lang) throws DBException {
		final String SELF = "TranslnRead.countTranslatedQStmts: ";
		int ct = 0;
		final String qry = toString(
				SELECT, count(), AS + SRCH_RESULT
				+ FROM + TRANSLATIONS
				+ WHERE + PHRASE_LANG + EQUALS + QMARK
				+ AND + PHRASE_ID + IN, parensBuild(
					SELECT + Q_STMT_ID
					+ FROM, table.QUESTIONS));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				lang);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				ct = rs.getInt(SRCH_RESULT);
			} // while more results are found
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return ct;
	} // countTranslatedQStmts(String)

	/** Gets SQL to get ID numbers of questions whose contents should not 
	 * be translated.
	 * @return	the SQL
	 */
	private static String getExcludedQIDsSQL() {
		return toString(
				SELECT + Q_QID 
				+ FROM, table.QUESTIONS,
				WHERE + Q_QSET + IN, 
					parens(getExcludedQSetIDsSQL()));
	} // getExcludedQIDsSQL()

	/** Counts the number of unique text- and marvin-based question data from 
	 * rank and choice questions translated into a language, excluding those 
	 * from certain topics.
	 * @return	the total number of unique text- and marvin-based question data
	 * (excluding those in sets not to be translated)
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int countTranslatableTextQData() throws DBException {
		final String SELF = "TranslnRead.countTranslatableTextQData: ";
		int ct = 0;
		final String AND_QD_QID_SQL = toString(
				AND + QD_QID + NOT + IN,
					parens(getExcludedQIDsSQL()),
				AND + QD_QID + IN, parensBuild(
					SELECT + Q_QID + FROM, table.QUESTIONS,
					WHERE + Q_TYPE + IN, parensJoinQuotes(
						DB_QTYPES[CHOICE],
						DB_QTYPES[RANK],
						DB_QTYPES[CHOOSE_EXPLAIN],
						DB_QTYPES[FILLBLANK])));
		final String qry = toString(
				SELECT, count(), AS + SRCH_RESULT
				+ FROM, parensBuild( 
					SELECT_UNIQUE, clobToString(QD_DATA),
					FROM, table.QUESTIONDATA,
					WHERE + QD_TYPE + EQUALS, 
						quotes(QDatum.DBVALUES[QDatum.TEXT]),
					AND_QD_QID_SQL,
					UNION + SELECT_UNIQUE + QD_NAME
					+ FROM, table.QUESTIONDATA,
					WHERE + QD_TYPE + EQUALS, 
						quotes(QDatum.DBVALUES[QDatum.MARVIN]),
					AND_QD_QID_SQL));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				ct = rs.getInt(SRCH_RESULT);
			} // while more results are found
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return ct;
	} // countTranslatableTextQData()

	/** Counts the number of unique text- and marvin-based question data from 
	 * rank and choice questions translated into a language.
	 * @param	lang	the language
	 * @return	the total number of text- and marvin-based question data 
	 * translated into the language 
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int countTranslatedTextQData(String lang) throws DBException {
		final String SELF = "TranslnRead.countTranslatedTextQData: ";
		int ct = 0;
		final String qry = toString(
				SELECT, count(), AS + SRCH_RESULT
				+ FROM + TRANSLATIONS
				+ WHERE + PHRASE_LANG + EQUALS + QMARK
				+ AND + PHRASE_ID + IN, parensBuild(
					SELECT + QD_TEXT_ID
					+ FROM, table.QUESTIONDATA,
					WHERE + QD_TYPE + IN, parensJoinQuotes(
						QDatum.DBVALUES[QDatum.TEXT],
						QDatum.DBVALUES[QDatum.MARVIN]),
					AND + QD_QID + IN, parensBuild(
						SELECT + Q_QID + FROM, table.QUESTIONS,
						WHERE + Q_TYPE + IN, parensJoinQuotes(
							DB_QTYPES[CHOICE],
							DB_QTYPES[RANK],
							DB_QTYPES[CHOOSE_EXPLAIN],
							DB_QTYPES[FILLBLANK]))));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				lang);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				ct = rs.getInt(SRCH_RESULT);
			} // while more results are found
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return ct;
	} // countTranslatedTextQData(String)

	/** Counts the number of evaluators, excluding those from certain topics.
	 * @return	the total number of unique evaluators (excluding those in sets 
	 * not to be translated)
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int countTranslatableEvals() throws DBException {
		final String SELF = "TranslnRead.countTranslatableEvals: ";
		int ct = 0;
		final String qry = toString(
				SELECT, count(), AS + SRCH_RESULT
				+ FROM, parensBuild( 
					SELECT_UNIQUE, clobToString(EVAL_FEEDBACK),
					FROM, table.EVALUATORS,
					WHERE + EVAL_QID + NOT + IN,
						parens(getExcludedQIDsSQL())));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				ct = rs.getInt(SRCH_RESULT);
			} // while more results are found
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return ct;
	} // countTranslatableEvals()

	/** Counts the number of unique evaluator feedbacks translated into a 
	 * language.
	 * @param	lang	the language
	 * @return	the total number of unique evaluators translated into the
	 * language 
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int countTranslatedEvals(String lang) throws DBException {
		final String SELF = "TranslnRead.countTranslatedEvals: ";
		int ct = 0;
		final String qry = toString(
				SELECT, count(), AS + SRCH_RESULT
				+ FROM + TRANSLATIONS
				+ WHERE + PHRASE_LANG + EQUALS + QMARK
				+ AND + PHRASE_ID + IN, parensBuild(
					SELECT + EVAL_FEEDBACKID
					+ FROM, table.EVALUATORS));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				lang);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				ct = rs.getInt(SRCH_RESULT);
			} // while more results are found
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return ct;
	} // countTranslatedEvals(String)

	/** Gets SQL to get names of topics whose contents can be translated.
	 * @return	the SQL
	 */
	private static String topicsAreUnexcluded() {
		return toString(
				TOPICS + DOT + TOPIC_NAME + NOT + LIKE, quotes("CHE%"),
				AND + TOPICS + DOT + TOPIC_NAME + NOT + IN, parensJoinQuotes(
					"Development questions",
					"Questions from AWRORM"));
	} // topicsAreUnexcluded()

	/** Gets the names of question sets whose headers (common question 
	 * statements) have not been translated into a certain language, excluding
	 * headers from certain topics.
	 * @param	lang	the language
	 * @return	an array of topic and question set names
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[] getUntransldHeaders(String lang) throws DBException {
		final String SELF = "TranslnRead.getUntransldHeaders: ";
		final ArrayList<String> untransldHeaders = new ArrayList<String>();
		final String qry = toString(
				SELECT, joinAll(
					TOPICS + DOT + TOPIC_NAME + AS + SRCH_RESULT1,
					QSETS + DOT + QSET_NAME + AS + SRCH_RESULT2),
				FROM + QSETS + JOIN + TOPICS
					+ ON + QSETS + DOT + QSET_TOPIC_ID 
					+ EQUALS + TOPICS + DOT + TOPIC_ID
				+ WHERE + QSET_COMMONQSTATEMENT + IS_NOT_NULL
				+ AND, parensBuild(
					QSET_COMMONQSTMT_ID + IS_ZERO
					+ OR + QSET_COMMONQSTMT_ID + NOT + IN, parens( 
						SELECT + PHRASE_ID
						+ FROM + TRANSLATIONS
						+ WHERE + PHRASE_LANG + EQUALS + QMARK
						+ AND + PHRASE_ID + IS_POSITIVE)),
				AND, topicsAreUnexcluded(),
				ORDER_BY, joinAll(
					TOPICS + DOT + TOPIC_NAME,
					QSETS + DOT + QSET_NAME));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				lang);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				untransldHeaders.add(toString(
					rs.getString(SRCH_RESULT1), DIV,
					rs.getString(SRCH_RESULT2)));
			} // while more results are found
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return untransldHeaders.toArray(new String[untransldHeaders.size()]);
	} // getUntransldHeaders(String)

	/** Gets the questions whose statements have not been translated into a 
	 * certain language, excluding questions from certain topics.
	 * @param	lang	the language
	 * @return	a map of question serial numbers keyed by topic and question set 
	 * names
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<String, int[]> getUntransldQStmts(String lang) 
			throws DBException {
		final String SELF = "TranslnRead.getUntransldQStmts: ";
		final Map<String, int[]> qNumsByQSetName = 
				new LinkedHashMap<String, int[]>();
		final String qry = toString(
				SELECT, joinAll(
					TOPICS + DOT + TOPIC_NAME + AS + SRCH_RESULT1,
					QSETS + DOT + QSET_NAME + AS + SRCH_RESULT2,
					QSETS + DOT + QSET_ID + AS + SRCH_RESULT3,
					toString(table.QUESTIONS, DOT + Q_QID + AS + SRCH_RESULT4)),
				FROM, table.QUESTIONS, 
				JOIN + QSETS + ON, table.QUESTIONS, DOT + Q_QSET 
					+ EQUALS + QSETS + DOT + QSET_ID 
				+ JOIN + TOPICS + ON + QSETS + DOT + QSET_TOPIC_ID 
					+ EQUALS + TOPICS + DOT + TOPIC_ID
				+ WHERE + Q_STATEMENT + IS_NOT_NULL
				+ AND, parensBuild(
					Q_STMT_ID + IS_ZERO
					+ OR + Q_STMT_ID + NOT + IN, parens( 
						SELECT + PHRASE_ID
						+ FROM + TRANSLATIONS
						+ WHERE + PHRASE_LANG + EQUALS + QMARK
						+ AND + PHRASE_ID + IS_POSITIVE)),
				AND, topicsAreUnexcluded(),
				ORDER_BY, joinAll(
					TOPICS + DOT + TOPIC_NAME,
					QSETS + DOT + QSET_NAME));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				lang);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) while (!rs.isAfterLast()) {
				final String topicName = rs.getString(SRCH_RESULT1);
				final String qSetName = rs.getString(SRCH_RESULT2);
				final int qSetId = rs.getInt(SRCH_RESULT3);
				final QSet qSet = new QSet(qSetId);
				final ArrayList<Integer> qNums = new ArrayList<Integer>();
				while (!rs.isAfterLast()
						&& qSetId == rs.getInt(SRCH_RESULT3)) {
					final int qId = rs.getInt(SRCH_RESULT4);
					final int qNum = qSet.getQNum(qId);
					qNums.add(Integer.valueOf(qNum));
					rs.next();
				} // while same QSet
				final int[] qNumsArr = Utils.listToIntArray(qNums);
				Arrays.sort(qNumsArr);
				qNumsByQSetName.put(toString(topicName, DIV, qSetName),
						qNumsArr);
			} // while there are more qSets
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return qNumsByQSetName;
	} // getUntransldQStmts(String)

	/** Gets the questions whose statements have not been translated into a 
	 * certain language, excluding questions from certain topics.
	 * @param	lang	the language
	 * @return	a map of question serial numbers keyed by topic and question set 
	 * names
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<String, int[]> getUntransldEvals(String lang) 
			throws DBException {
		final String SELF = "TranslnRead.getUntransldEvals: ";
		final Map<String, int[]> qNumsByQSetName = 
				new LinkedHashMap<String, int[]>();
		final String qry = toString(
				SELECT_DISTINCT, joinAll(
					TOPICS + DOT + TOPIC_NAME + AS + SRCH_RESULT1,
					QSETS + DOT + QSET_NAME + AS + SRCH_RESULT2,
					QSETS + DOT + QSET_ID + AS + SRCH_RESULT3,
					toString(table.QUESTIONS, DOT + Q_QID + AS + SRCH_RESULT4)),
				FROM, table.EVALUATORS, 
				JOIN, table.QUESTIONS, ON, table.EVALUATORS, DOT + EVAL_QID
					+ EQUALS, table.QUESTIONS, DOT + Q_QID 
				+ JOIN + QSETS + ON, table.QUESTIONS, DOT + Q_QSET 
					+ EQUALS + QSETS + DOT + QSET_ID 
				+ JOIN + TOPICS + ON + QSETS + DOT + QSET_TOPIC_ID 
					+ EQUALS + TOPICS + DOT + TOPIC_ID
				+ WHERE, topicsAreUnexcluded(),
				AND + EVAL_FEEDBACK + IS_NOT_NULL
				+ AND, parensBuild(
					EVAL_FEEDBACKID + IS_ZERO
					+ OR + EVAL_FEEDBACKID + NOT + IN, parens( 
						SELECT + PHRASE_ID
						+ FROM + TRANSLATIONS
						+ WHERE + PHRASE_LANG + EQUALS + QMARK
						+ AND + PHRASE_ID + IS_POSITIVE)),
				ORDER_BY, joinAll(
					TOPICS + DOT + TOPIC_NAME,
					QSETS + DOT + QSET_NAME));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				lang);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) while (!rs.isAfterLast()) {
				final String topicName = rs.getString(SRCH_RESULT1);
				final String qSetName = rs.getString(SRCH_RESULT2);
				final int qSetId = rs.getInt(SRCH_RESULT3);
				final QSet qSet = new QSet(qSetId);
				final ArrayList<Integer> qNums = new ArrayList<Integer>();
				while (!rs.isAfterLast()
						&& qSetId == rs.getInt(SRCH_RESULT3)) {
					final int qId = rs.getInt(SRCH_RESULT4);
					final int qNum = qSet.getQNum(qId);
					qNums.add(Integer.valueOf(qNum));
					rs.next();
				} // while same QSet
				final int[] qNumsArr = Utils.listToIntArray(qNums);
				Arrays.sort(qNumsArr);
				qNumsByQSetName.put(toString(topicName, DIV, qSetName),
						qNumsArr);
			} // while there are more qSets
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return qNumsByQSetName;
	} // getUntransldEvals(String)

	/** Gets the questions whose text-based question data have not been 
	 * translated into a certain language, excluding questions from certain 
	 * topics.
	 * @param	lang	the language
	 * @return	a map of question serial numbers keyed by topic and question set 
	 * names
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<String, int[]> getUntransldTextQData(String lang) 
			throws DBException {
		final String SELF = "TranslnRead.getUntransldTextQData: ";
		final Map<String, int[]> qNumsByQSetName = 
				new LinkedHashMap<String, int[]>();
		final String qry = toString(
				SELECT_DISTINCT, joinAll(
					TOPICS + DOT + TOPIC_NAME + AS + SRCH_RESULT1,
					QSETS + DOT + QSET_NAME + AS + SRCH_RESULT2,
					QSETS + DOT + QSET_ID + AS + SRCH_RESULT3,
					toString(table.QUESTIONS, DOT + Q_QID + AS + SRCH_RESULT4)),
				FROM, table.QUESTIONDATA, 
				JOIN, table.QUESTIONS, ON, table.QUESTIONDATA, DOT + QD_QID
					+ EQUALS, table.QUESTIONS, DOT + Q_QID 
				+ JOIN + QSETS + ON, table.QUESTIONS, DOT + Q_QSET 
					+ EQUALS + QSETS + DOT + QSET_ID 
				+ JOIN + TOPICS + ON + QSETS + DOT + QSET_TOPIC_ID 
					+ EQUALS + TOPICS + DOT + TOPIC_ID
				+ WHERE, topicsAreUnexcluded(),
				AND, table.QUESTIONS, DOT + Q_TYPE + IN, parensJoinQuotes(
					DB_QTYPES[CHOICE],
					DB_QTYPES[RANK],
					DB_QTYPES[CHOOSE_EXPLAIN],
					DB_QTYPES[FILLBLANK]),
				AND + QD_TYPE + IN, parensJoinQuotes(
					QDatum.DBVALUES[QDatum.TEXT],
					QDatum.DBVALUES[QDatum.MARVIN]),
				AND, parensBuild(
					QD_TEXT_ID + IS_ZERO
					+ OR + QD_TEXT_ID + NOT + IN, parens( 
						SELECT + PHRASE_ID
						+ FROM + TRANSLATIONS
						+ WHERE + PHRASE_LANG + EQUALS + QMARK
						+ AND + PHRASE_ID + IS_POSITIVE)),
				ORDER_BY, joinAll(
					TOPICS + DOT + TOPIC_NAME,
					QSETS + DOT + QSET_NAME));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				lang);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) while (!rs.isAfterLast()) {
				final String topicName = rs.getString(SRCH_RESULT1);
				final String qSetName = rs.getString(SRCH_RESULT2);
				final int qSetId = rs.getInt(SRCH_RESULT3);
				final QSet qSet = new QSet(qSetId);
				final ArrayList<Integer> qNums = new ArrayList<Integer>();
				while (!rs.isAfterLast()
						&& qSetId == rs.getInt(SRCH_RESULT3)) {
					final int qId = rs.getInt(SRCH_RESULT4);
					final int qNum = qSet.getQNum(qId);
					qNums.add(Integer.valueOf(qNum));
					rs.next();
				} // while same QSet
				final int[] qNumsArr = Utils.listToIntArray(qNums);
				Arrays.sort(qNumsArr);
				qNumsByQSetName.put(toString(topicName, DIV, qSetName),
						qNumsArr);
			} // while there are more qSets
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return qNumsByQSetName;
	} // getUntransldTextQData(String)

	/** Constructor.  Intentionally empty. */
	private TranslnRead() { }

} // TranslnRead
