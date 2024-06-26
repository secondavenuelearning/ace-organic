package com.epoch.db;

import static com.epoch.db.dbConstants.QuestionsRWConstants.*;
import static com.epoch.db.dbConstants.TranslationsConstants.*;
import com.epoch.exceptions.DBException;
import com.epoch.qBank.QDatum;
import com.epoch.qBank.Question;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.translations.QTranslns;
import com.epoch.translations.TranslnsMap;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Contains all database read and write operations pertaining to
 * translations.  Translations of ACE phrases have negative ID numbers 
 * derived from the hashcode; translations of question content have
 * positive ID numbers derived from a sequencer.
 */
public final class TranslnWrite extends DBCommon implements QuestionConstants {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Utility variable to reference nonstatic constants. */
	private static final DBTables table = new DBTables();
	/** ID of the person doing the translating. */
	private String translatorId = null;

	public TranslnWrite(String translator) {
		translatorId = translator;
	} // TranslnWrite(String)

	/** Sets the translated common question statement of the given question set.
	 * @param	qSetId	the unique ID of the question set
	 * @param	translation	the new common question statement
	 * @param	language	the language of translation
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setHeader(int qSetId, String translation,
			String language) throws DBException {
		final String SELF = "TranslnWrite.setHeader: ";
		final String qry = 
				SELECT + QSET_COMMONQSTMT_ID
				+ FROM + QSETS
				+ WHERE + QSET_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				qSetId);
		debugPrint(SELF, sql_vals);
		int phraseId = 0;
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) phraseId = rs.getInt(QSET_COMMONQSTMT_ID);
			rs.close();
			final boolean newPhrase = phraseId == 0;
			final List<Integer> qSetIds = new ArrayList<Integer>();
			qSetIds.add(Integer.valueOf(qSetId));
			final Map<Integer, String> newPhrases = 
					new HashMap<Integer, String>();
			final Map<Integer, String> existingPhrases = 
					new HashMap<Integer, String>();
			if (newPhrase) {
				phraseId = nextSequence(con, PHRASE_SEQ);
				setSameHeaderPhraseIds(con, qSetIds, phraseId);
				newPhrases.put(Integer.valueOf(phraseId), translation);
			} else {
				existingPhrases.put(Integer.valueOf(phraseId), translation);
			} // if new phrase
			debugPrint(SELF, "qSetIds = ", qSetIds);
			// get identical, untranslated phrases
			sql_vals.setSql(
					SELECT + QSET_ID
					+ FROM + QSETS
					+ WHERE + QSET_COMMONQSTMT_ID + IS_ZERO
					+ AND + QSET_ID + NOT_EQUALS + QMARK
					+ AND, clobToString(QSET_COMMONQSTATEMENT), IN, 
						parensBuild(
						SELECT, clobToString(QSET_COMMONQSTATEMENT),
						FROM + QSETS
						+ WHERE + QSET_ID + EQUALS + QMARK));
			sql_vals.addValue(qSetId);
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				qSetIds.add(Integer.valueOf(rs.getInt(QSET_ID)));
			} // while there are other identical headers
			debugPrint(SELF + "this and identical, untranslated phrases "
					+ "have IDs: ", qSetIds);
			setSameHeaderPhraseIds(con, qSetIds, phraseId);
			setTranslations(con, language, newPhrases, existingPhrases);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
	} // setHeader(int, String, String)

	/** Sets phrase IDs of qSets with common question statements that are 
	 * identical to the one that has just been translated.
	 * @param	con	database connection
	 * @param	qSetIds	qSet ID numbers
	 * @param	phraseId	phrase ID number
	 */
	private void setSameHeaderPhraseIds(Connection con, 
			List<Integer> qSetIds, int phraseId) {
		final String SELF = "TranslnWrite.setSameHeaderPhraseIds: ";
		final String qry = 
				UPDATE + QSETS
				+ SET + QSET_COMMONQSTMT_ID + EQUALS + QMARK
				+ WHERE + QSET_ID + EQUALS + QMARK;
		debugPrint(SELF, qry); 
		PreparedStatement stmt = null;
		try {
			stmt = con.prepareStatement(qry);
			int batchNum = 0;
			for (final Integer qSetId : qSetIds) {
				final StringBuilder joinedValues = setValues(stmt,
						phraseId,
						qSetId);
				debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
				stmt.addBatch();
				batchNum++;
				if (batchNum % 100 == 0) {
					stmt.executeBatch();
					stmt.clearBatch();
				} // if time to submit
			} // for each qSetId and the corresponding phraseId
			if (batchNum % 100 != 0) {
				stmt.executeBatch();
			} // if need to submit
		} catch (SQLException e) { 
			alwaysPrint(SELF + "update threw SQLException"); 
			e.printStackTrace();
		} finally {
			closeConnection(null, stmt, null);
		} // do nothing
	} // setSameHeaderPhraseIds(Connection, List<Integer>, int)

	/** Sets the translated question statements of both the given question
	 * statements and other question statements that are identical.
	 * @param	qIds	the unique IDs of the questions
	 * @param	translations	the new question statements
	 * @param	language	the language of translation
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setQuestionStatements(int[] qIds, 
			String[] translations, String language) throws DBException {
		final String SELF = "TranslnWrite.setQuestionStatements: ";
		if (Utils.isEmpty(qIds)) return;
		debugPrint(SELF, "qIds = ", qIds, ", translations = ", translations);
		String qry = toString(
				SELECT, joinAll(
					Q_QID,
					Q_STMT_ID),
				FROM, table.QUESTIONS,
				WHERE + Q_QID + IN, parensQMarks(qIds));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				qIds);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			final Map<Integer, String> newPhrases = 
					new HashMap<Integer, String>();
			final Map<Integer, String> existingPhrases = 
					new HashMap<Integer, String>();
			final List<int[]> newPhraseIdGrps = new ArrayList<int[]>();
			final List<Integer> qIdsList = Utils.intArrayToList(qIds);
			while (rs.next()) {
				final int qId = rs.getInt(Q_QID);
				int phraseId = rs.getInt(Q_STMT_ID);
				final int qNum = qIdsList.indexOf(Integer.valueOf(qId));
				if (phraseId == 0) { // has not already been set
					phraseId = nextSequence(con, PHRASE_SEQ);
					newPhrases.put(Integer.valueOf(phraseId),
							translations[qNum]);
					newPhraseIdGrps.add(new int[] {phraseId, qId});
				} else { // has already been set
					existingPhrases.put(Integer.valueOf(phraseId),
							translations[qNum]);
				} // if phraseId
			} // while more qIds
			closeStmtAndRs(stmt, rs);
			debugPrint(SELF, newPhrases.size(), " newPhrases, ", 
					existingPhrases.size(), " existingPhrases");
			// add new phraseIds
			if (!newPhraseIdGrps.isEmpty()) {
				con.setAutoCommit(false);
				qry = toString(UPDATE, table.QUESTIONS,
						SET + Q_STMT_ID + EQUALS + QMARK
						+ WHERE + Q_QID + EQUALS + QMARK);
				debugPrint(SELF, qry); 
				stmt = con.prepareStatement(qry);
				int batchNum = 0;
				for (final int[] group : newPhraseIdGrps) {
					final StringBuilder joinedValues = setValues(stmt,
							group[0], // phraseId
							group[1]); // qId
					debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
					stmt.addBatch();
					batchNum++;
					if (batchNum % 100 == 0) {
						stmt.executeBatch();
						stmt.clearBatch();
					} // if time to submit
				} // for each new phraseId
				if (batchNum % 100 != 0) {
					stmt.executeBatch();
				} // if need to submit
				closeConnection(null, stmt, null);
				// set phraseIds of identical question statements in both local
				// and master-authored tables
				final String[] questionTables = new String[]
						{table.QUESTIONS, 
						DBLocalTables.getAntitable(table.QUESTIONS)};
				for (final String questionTable : questionTables) {
					qry = toString(
							UPDATE, questionTable,
							SET + Q_STMT_ID + EQUALS + QMARK
							+ WHERE + Q_STMT_ID + IS_ZERO
							+ AND, clobToString(Q_STATEMENT), 
									IN, parensBuild(
								SELECT, clobToString(Q_STATEMENT),
								FROM, table.QUESTIONS,
								WHERE + Q_QID + EQUALS + QMARK));
					debugPrint(SELF, qry); 
					stmt = con.prepareStatement(qry);
					batchNum = 0;
					for (final int[] group : newPhraseIdGrps) {
						final StringBuilder joinedValues = setValues(stmt,
								group[0], // phraseId
								group[1]); // qId
						debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
						stmt.addBatch();
						batchNum++;
						if (batchNum % 100 == 0) {
							stmt.executeBatch();
							stmt.clearBatch();
						} // if time to submit
					} // for each new phraseId
					if (batchNum % 100 != 0) {
						stmt.executeBatch();
					} // if need to submit
					closeConnection(null, stmt, null);
					// let setTranslations() commmit
				} // for each version of QUESTIONS table
			} // if there are new phraseIds to set
			setTranslations(con, language, newPhrases, existingPhrases); // will commit
		} catch (SQLException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
	} // setQuestionStatements(int[], String[], String)

	/** Sets the translated feedbacks of both the given feedbacks and other 
	 * feedbacks that are identical.
	 * @param	qId_MajorIds	array of qId_majorId of each translated 
	 * evaluator
	 * @param	translations	the new feedbacks
	 * @param	language	the language of translation
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setFeedbacks(String[] qId_MajorIds,
			String[] translations, String language) throws DBException {
		final String SELF = "TranslnWrite.setFeedbacks: ";
		if (Utils.isEmpty(qId_MajorIds)) return;
		// find out which feedbacks already have phraseIds set
		final String QID_MAJORID = 
				EVAL_QID + " || '_' || " + EVAL_MAJORID;
		String qry = toString(
				SELECT, joinAll(
					QID_MAJORID + AS + SRCH_RESULT,
					EVAL_FEEDBACKID),
				FROM, table.EVALUATORS,
				WHERE + EVAL_MINORID + IS_ZERO
				+ AND + QID_MAJORID + IN, parensQMarks(qId_MajorIds));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
		sql_vals.setValuesArray(qId_MajorIds);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			final Map<Integer, String> newPhrases = 
					new HashMap<Integer, String>();
			final Map<Integer, String> existingPhrases = 
					new HashMap<Integer, String>();
			final List<int[]> newPhraseIdGrps = new ArrayList<int[]>();
			while (rs.next()) {
				final String qId_MajorId = rs.getString(SRCH_RESULT);
				int phraseId = rs.getInt(EVAL_FEEDBACKID);
				final int evalNum = Utils.indexOf(qId_MajorIds, qId_MajorId);
				if (phraseId == 0) { // has not already been set
					phraseId = nextSequence(con, PHRASE_SEQ);
					newPhrases.put(Integer.valueOf(phraseId),
							translations[evalNum]);
					final String[] ids = qId_MajorId.split("_");
					newPhraseIdGrps.add(new int[] {phraseId, 
							MathUtils.parseInt(ids[0]), 
							MathUtils.parseInt(ids[1])});
				} else { // has already been set
					existingPhrases.put(Integer.valueOf(phraseId),
							translations[evalNum]);
				} // if phraseId
			} // while more qIds
			closeStmtAndRs(stmt, rs);
			debugPrint(SELF, newPhrases.size(), " newPhrases, ", 
					existingPhrases.size(), " existingPhrases");
			// add new phraseIds
			if (!newPhraseIdGrps.isEmpty()) {
				con.setAutoCommit(false);
				qry = toString(
						UPDATE, table.EVALUATORS,
						SET + EVAL_FEEDBACKID + EQUALS + QMARK
						+ WHERE + EVAL_QID + EQUALS + QMARK
						+ AND + EVAL_MAJORID + EQUALS + QMARK
						+ AND + EVAL_MINORID + IS_ZERO);
				debugPrint(SELF, qry); 
				stmt = con.prepareStatement(qry);
				int batchNum = 0;
				for (final int[] group : newPhraseIdGrps) {
					final StringBuilder joinedValues = setValues(stmt,
							group[0], // phraseId
							group[1], // qId
							group[2]); // majorId
					debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
					stmt.addBatch();
					batchNum++;
					if (batchNum % 100 == 0) {
						stmt.executeBatch();
						stmt.clearBatch();
					} // if time to submit
				} // for each new phraseId
				if (batchNum % 100 != 0) {
					stmt.executeBatch();
				} // if need to submit
				closeConnection(null, stmt, null);
				// set phraseIds of identical feedbacks
				final String[] evalTables = new String[]
						{table.EVALUATORS, 
						DBLocalTables.getAntitable(table.EVALUATORS)};
				for (final String evalTable : evalTables) {
					qry = toString(
							UPDATE, evalTable,
							SET + EVAL_FEEDBACKID + EQUALS + QMARK
							+ WHERE + EVAL_FEEDBACKID + IS_ZERO
							+ AND + EVAL_MINORID + IS_ZERO 
							+ AND, clobToString(EVAL_FEEDBACK), 
									IN, parensBuild(
								SELECT, clobToString(EVAL_FEEDBACK),
								FROM, table.EVALUATORS,
								WHERE + EVAL_QID + EQUALS + QMARK
								+ AND + EVAL_MAJORID + EQUALS + QMARK
								+ AND + EVAL_MINORID + IS_ZERO));
					debugPrint(SELF, qry); 
					stmt = con.prepareStatement(qry);
					batchNum = 0;
					for (final int[] group : newPhraseIdGrps) {
						final StringBuilder joinedValues = setValues(stmt,
								group[0], // phraseId
								group[1], // qId
								group[2]); // majorId
						debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
						stmt.addBatch();
						batchNum++;
						if (batchNum % 100 == 0) {
							stmt.executeBatch();
							stmt.clearBatch();
						} // if time to submit
					} // for each new phraseId
					if (batchNum % 100 != 0) {
						stmt.executeBatch();
					} // if need to submit
					closeConnection(null, stmt, null);
					// let setTranslations() commmit
				} // for each kind of authoring
			} // if there are new phraseIds to set
			setTranslations(con, language, newPhrases, existingPhrases); // will commit
		} catch (SQLException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
	} // setFeedbacks(String[], String[], String)

	/** Sets the translated question data of both the given question data 
	 * and other question data that are identical.
	 * @param	qIds_SerialNos	array of qId_serialNo of each 
	 * question and question datum
	 * @param	translations	the new question data
	 * @param	language	the language of translation
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setQData(String[] qIds_SerialNos,
			String[] translations, String language) throws DBException {
		final String SELF = "TranslnWrite.setQData: ";
		if (Utils.isEmpty(qIds_SerialNos)) return;
		// find out which question data already have phraseIds set
		final String QID_NUM = QD_QID + " || '_' || " + QD_NUM;
		String qry = toString(
				SELECT, joinAll(
					QID_NUM + AS + SRCH_RESULT,
					QD_TEXT_ID),
				FROM, table.QUESTIONDATA,
				WHERE + QID_NUM + IN, parensQMarks(qIds_SerialNos));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
		sql_vals.setValuesArray(qIds_SerialNos);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			final Map<Integer, String> newPhrases = 
					new HashMap<Integer, String>();
			final Map<Integer, String> existingPhrases = 
					new HashMap<Integer, String>();
			final List<int[]> newPhraseIdGrps = new ArrayList<int[]>();
			while (rs.next()) {
				final String qId_SerialNo = rs.getString(SRCH_RESULT);
				int phraseId = rs.getInt(QD_TEXT_ID);
				final int qdNum = Utils.indexOf(qIds_SerialNos, qId_SerialNo);
				if (phraseId == 0) { // has not already been set
					phraseId = nextSequence(con, PHRASE_SEQ);
					newPhrases.put(Integer.valueOf(phraseId),
							translations[qdNum]);
					final String[] ids = qId_SerialNo.split("_");
					newPhraseIdGrps.add(new int[] {phraseId,
							MathUtils.parseInt(ids[0]),
							MathUtils.parseInt(ids[1])});
				} else { // has already been set
					existingPhrases.put(Integer.valueOf(phraseId),
							translations[qdNum]);
				} // if phraseId
			} // while more qIds_SerialNos
			debugPrint(SELF, newPhrases.size(), " newPhrases, ", 
					existingPhrases.size(), " existingPhrases");
			// add new phraseIds
			if (!newPhraseIdGrps.isEmpty()) {
				con.setAutoCommit(false);
				qry = toString(
						UPDATE, table.QUESTIONDATA,
						SET + QD_TEXT_ID + EQUALS + QMARK
						+ WHERE + QD_QID + EQUALS + QMARK
						+ AND + QD_NUM + EQUALS + QMARK);
				debugPrint(SELF, qry); 
				stmt = con.prepareStatement(qry);
				int batchNum = 0;
				for (final int[] group : newPhraseIdGrps) {
					final StringBuilder joinedValues = setValues(stmt,
							group[0], // phraseId
							group[1], // qId
							group[2]); // serialNo
					debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
					stmt.addBatch();
					batchNum++;
					if (batchNum % 100 == 0) {
						stmt.executeBatch();
						stmt.clearBatch();
					} // if time to submit
				} // for each new phraseId
				if (batchNum % 100 != 0) {
					stmt.executeBatch();
				} // if need to submit
				closeConnection(null, stmt, null);
				// set phraseIds of identical qData, 
				// once for text and once for molecule names,
				// each once for local and once for master
				final String[] qDataTables = new String[]
						{table.QUESTIONDATA, 
						DBLocalTables.getAntitable(table.QUESTIONDATA)};
				for (int datumType = 0; datumType < 2; datumType++) {
					for (final String qDataTable : qDataTables) {
						qry = toString(
								UPDATE, qDataTable,
								SET + QD_TEXT_ID + EQUALS + QMARK
								+ WHERE + QD_TEXT_ID + IS_ZERO 
								+ AND + QD_TYPE + EQUALS, 
									quotes(datumType == 1
										? QDatum.DBVALUES[QDatum.TEXT]
										: QDatum.DBVALUES[QDatum.MARVIN]),
								AND, clobToString(datumType == 1
										? QD_DATA : QD_NAME), IN, parensBuild(
									SELECT, clobToString(datumType == 1
											? QD_DATA : QD_NAME),
									FROM, table.QUESTIONDATA,
									WHERE + QD_QID + EQUALS + QMARK
									+ AND + QD_NUM + EQUALS + QMARK
									+ AND + QD_TYPE + EQUALS, 
										quotes(QDatum.DBVALUES[datumType == 1
											? QDatum.TEXT : QDatum.MARVIN])));
						debugPrint(SELF, qry); 
						stmt = con.prepareStatement(qry);
						batchNum = 0;
						for (final int[] group : newPhraseIdGrps) {
							final StringBuilder joinedValues = 
									setValues(stmt,
										group[0], // phraseId
										group[1], // qId
										group[2]); // serialNo
							debugPrint(SELF, "batch ", batchNum + 1, ": ", 
									joinedValues); 
							stmt.addBatch();
							batchNum++;
							if (batchNum % 100 == 0) {
								stmt.executeBatch();
								stmt.clearBatch();
							} // if time to submit
						} // for each new phraseId
						if (batchNum % 100 != 0) {
							stmt.executeBatch();
						} // if need to submit
						closeConnection(null, stmt, null);
						// let setTranslations() commmit
					} // for each kind of qData table
				} // for each kind of qData table
			} // if there are new phraseIds to set
			setTranslations(con, language, newPhrases, existingPhrases); // will commit
		} catch (SQLException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
	} // setQData(String[], String[], String)

	/** Sets the translated common ACE phrases.
	 * @param	phrasesTranslns	each phrase and its corresponding translation
	 * @param	language	the language of translation
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setPhrases(List<String[]> phrasesTranslns,
			String language) throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			final Map<Integer, String> newPhrases = 
					new HashMap<Integer, String>();
			final Map<Integer, String> existingPhrases = 
					new HashMap<Integer, String>();
			for (final String[] phraseTransln : phrasesTranslns) {
				final Integer phraseIdObj = 
						TranslnRead.getPhraseId(phraseTransln[0]);
				existingPhrases.put(phraseIdObj, phraseTransln[1]);
			} // for each phrase and associated translation
			setTranslations(con, language, newPhrases, existingPhrases);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // setPhrases(List<String[]>, String)

	/** Stores translations associated with phraseIds that have already
	 * been assigned to phrases that are being translated, and commits all
	 * previous transactions.  The kind of phrase is not needed, so this 
	 * method can be used for question statements, evaluator feedbacks, 
	 * and question data.
	 * @param	con	database connection
	 * @param	language	the language of translation
	 * @param	insertPhrases	translations of phrases that don't already have
	 * translations in the current language (because IDs were just now
	 * assigned), mapped by IDs
	 * @param	existingPhrases	translations of phrases that already had 
	 * IDs associated with them but may or may not already have translations 
	 * in the current language, mapped by IDs
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private void setTranslations(Connection con, 
			String language, Map<Integer, String> insertPhrases,
			Map<Integer, String> existingPhrases) throws DBException {
		final String SELF = "TranslnWrite.setTranslations: ";
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			final List<Integer> deletePhraseIds = new ArrayList<Integer>();
			final Map<Integer, String> updatePhrases = 
					new HashMap<Integer, String>();
			// sort IDs (and corresponding translations) that have existing records
			// into those that already have records in the current language
			// (--> update or delete) and those that do not (--> insert)
			if (!existingPhrases.isEmpty()) {
				final List<Integer> existingPhraseIds = 
						new ArrayList<Integer>(existingPhrases.keySet());
				final String qry = toString(
						SELECT + PHRASE_ID
						+ FROM + TRANSLATIONS
						+ WHERE + PHRASE_ID + IN, 
							parensQMarks(existingPhraseIds),
						AND + PHRASE_LANG + EQUALS + QMARK);
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
						existingPhraseIds,
						language);
				debugPrint(SELF, sql_vals);
				stmt = getStatement(con, sql_vals);
				rs = stmt.executeQuery();
				while (rs.next()) {
					final int phraseId = rs.getInt(PHRASE_ID);
					debugPrint(SELF + "translation in ", language
							+ " already exists for phraseId ", phraseId);
					final Integer phraseIdObj = Integer.valueOf(phraseId);
					final String transln = existingPhrases.remove(phraseIdObj);
					if (Utils.isEmpty(transln)) {
						deletePhraseIds.add(phraseIdObj);
					} else {
						updatePhrases.put(phraseIdObj, transln);
					} // if there's a translation
				} // while more qIds
				closeStmtAndRs(stmt, rs);
			} // if existingPhraseIds is not empty
			// remaining existingPhraseIds do not have record in the current
			// language, so need to be inserted
			insertPhrases.putAll(existingPhrases);
			debugPrint(SELF, insertPhrases.size(), " new records(s) to insert: ", 
					insertPhrases, ";\n", SELF,
					updatePhrases.size(), " existing records(s) to update: ",
					updatePhrases, ";\n", SELF,
					deletePhraseIds.size(), " existing records(s) to delete: ",
					deletePhraseIds);
			con.setAutoCommit(false);
			// delete existing records whose new translation is empty
			if (!deletePhraseIds.isEmpty()) {
				final String qry = toString(
						DELETE_TRANSLATIONS_BY_IDS(), 
							parensQMarks(deletePhraseIds),
						AND + PHRASE_LANG + EQUALS + QMARK);
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
						deletePhraseIds,
						language);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} // if there are translations to delete
			if (!insertPhrases.isEmpty()) {
				// insert records for new translations into this language
				final String[] fields = new String[] {
						PHRASE_ID,
						PHRASE_LANG,
						PHRASE_TRANSLN, // CLOB field
						PHRASE_TRANSLATOR};
				final String qry = 
						getInsertIntoValuesQMarksSQL(TRANSLATIONS, fields);
				debugPrint(SELF, qry); 
				stmt = con.prepareStatement(qry);
				int batchNum = 0;
				final List<Integer> phraseIds = 
						new ArrayList<Integer>(insertPhrases.keySet());
				for (final Integer phraseIdObj : phraseIds) {
					final String transln = insertPhrases.get(phraseIdObj);
					if (!Utils.isEmpty(transln)) {
						final StringBuilder joinedValues = setValues(stmt,
								phraseIdObj,
								language,
								transln,
								translatorId);
						debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
						stmt.addBatch();
						batchNum++;
						if (batchNum % 100 == 0) {
							stmt.executeBatch();
							stmt.clearBatch();
						} // if time to submit
					} // if there's a translation
				} // for each insert phraseId
				if (batchNum % 100 != 0) {
					stmt.executeBatch();
				} // if need to submit
				closeConnection(null, stmt, null);
			} // if there are phrases to insert
			if (!updatePhrases.isEmpty()) {
				// update records of existing translations
				final String qry = toString(
						UPDATE + TRANSLATIONS 
						+ SET, equalsJoinQMarks(
							PHRASE_TRANSLN, // CLOB field
							PHRASE_TRANSLATOR),
						WHERE + PHRASE_ID + EQUALS + QMARK
						+ AND + PHRASE_LANG + EQUALS + QMARK);
				debugPrint(SELF, qry); 
				stmt = con.prepareStatement(qry);
				int batchNum = 0;
				final List<Integer> updatePhraseIds = 
						new ArrayList<Integer>(updatePhrases.keySet());
				for (final Integer phraseIdObj : updatePhraseIds) {
					final String transln = updatePhrases.get(phraseIdObj);
					if (!Utils.isEmpty(transln)) {
						final StringBuilder joinedValues = setValues(stmt,
								transln,
								translatorId,
								phraseIdObj,
								language);
						debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
						stmt.addBatch();
						batchNum++;
						if (batchNum % 100 == 0) {
							stmt.executeBatch();
							stmt.clearBatch();
						} // if time to submit
					} // if there's a translation
				} // for each update phraseId
				if (batchNum % 100 != 0) {
					stmt.executeBatch();
				} // if need to submit
			} // if there are phrases to update
			con.commit();
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "Caught SQLException while "
					+ "trying to insert or update translations.");
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try
	} // setTranslations(Connection, String, Map<Integer, String>, Map<Integer, String>)

	/** Assigns translations of a duplicated question or one acquired from
	 * an XML file.  All translations need phraseIds assigned.
	 * @param	newQ	question whose translations to assign
	 * @param	translns	translations of question statement, evaluator
	 * feedbacks, question data in various languages
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void assignQTranslations(Question newQ, QTranslns translns) 
			throws DBException {
		final String SELF = "TranslnWrite.assignQTranslations: ";
		Connection con = null;
		PreparedStatement stmt = null;
		try {
			final int qId = newQ.getQId();
			con = getPoolConnection();
			con.setAutoCommit(false);
			final Map<Integer, TranslnsMap> phrases = 
					new HashMap<Integer, TranslnsMap>();
			if (!translns.noQStmt()) {
				final int phraseId = nextSequence(con, PHRASE_SEQ);
				newQ.setPhraseId(phraseId);
				phrases.put(Integer.valueOf(phraseId), translns.getQStmt());
				final String qry = toString(
						UPDATE, table.QUESTIONS,
						SET + Q_STMT_ID + EQUALS + QMARK
						+ WHERE + Q_QID + EQUALS + QMARK);
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
						phraseId,
						qId);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} // if there are translated question statements
			if (!translns.noFeedbacks()) {
				final String qry = toString(
						UPDATE, table.EVALUATORS,
						SET + EVAL_FEEDBACKID + EQUALS + QMARK
						+ WHERE + EVAL_MAJORID + EQUALS + QMARK
						+ AND + EVAL_MINORID + IS_ZERO
						+ AND + EVAL_QID + EQUALS + QMARK);
				debugPrint(SELF, qry); 
				stmt = con.prepareStatement(qry);
				int evalNum = 1;
				final List<Integer> feedbackNumObjs = 
						translns.getFeedbackNums();
				for (final Integer feedbackNumObj : feedbackNumObjs) { 
					final TranslnsMap feedbackTranslns = 
							translns.getFeedbackTranslations(feedbackNumObj);
					if (!feedbackTranslns.isEmpty()) { // shouldn't happen
						final int phraseId = nextSequence(con, PHRASE_SEQ);
						newQ.setPhraseId(phraseId);
						phrases.put(Integer.valueOf(phraseId), feedbackTranslns);
						final StringBuilder joinedValues = setValues(stmt,
								phraseId,
								evalNum,
								qId);
						debugPrint(SELF, "batch ", evalNum + 1, ": ", joinedValues); 
						stmt.addBatch();
						evalNum++;
						if (evalNum % 100 == 1) {
							stmt.executeBatch();
							stmt.clearBatch();
						} // if time to submit
					} // if there are translations for this evaluator
				} // for each new evaluator's translations
				if (evalNum % 100 != 1) {
					stmt.executeBatch();
				} // if need to submit
				closeConnection(null, stmt, null);
			} // if there are qData to be translated
			if (!translns.noQData()) {
				final String qry = toString(
						UPDATE, table.QUESTIONDATA,
						SET + QD_TEXT_ID + EQUALS + QMARK
						+ WHERE + QD_NUM + EQUALS + QMARK
						+ AND + QD_QID + EQUALS + QMARK);
				debugPrint(SELF, qry); 
				stmt = con.prepareStatement(qry);
				int qdNum = 1;
				final List<Integer> qDatumNumObjs = translns.getQDataNums();
				for (final Integer qDatumNumObj : qDatumNumObjs) { 
					final TranslnsMap qDatumTranslns = 
							translns.getQDatumTranslations(qDatumNumObj);
					if (!qDatumTranslns.isEmpty()) { // shouldn't happen
						final int phraseId = nextSequence(con, PHRASE_SEQ);
						newQ.setPhraseId(phraseId);
						phrases.put(Integer.valueOf(phraseId), qDatumTranslns);
						final StringBuilder joinedValues = setValues(stmt,
								phraseId,
								qdNum,
								qId);
						debugPrint(SELF, "batch ", qdNum + 1, ": ", joinedValues); 
						stmt.addBatch();
						qdNum++;
						if (qdNum % 100 == 1) {
							stmt.executeBatch();
							stmt.clearBatch();
						} // if time to submit
					} // if there are translations for this question datum
				} // for each new question datum's translations
				if (qdNum % 100 != 1) {
					stmt.executeBatch();
				} // if need to submit
			} // if there are qData to be translated
			con.commit();
			final List<Integer> phraseIds = 
					new ArrayList<Integer>(phrases.keySet());
			for (final Integer phraseIdObj : phraseIds) {
				final TranslnsMap phraseTranslns = phrases.get(phraseIdObj);
				storeTranslations(con, phraseIdObj.intValue(), phraseTranslns);
			} // for each item with a translation
		} catch (SQLException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, null);
		}
	} // assignQTranslations(Question, QTranslns)

	/** Assigns translations of a common question statement of a question set
	 * acquired from an XML file.
	 * @param	qSetId	unique ID of the question set whose headers are being
	 * translated
	 * @param	headerTranslns	TranslnsMap of the common question
	 * statement
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void assignHeaderTranslations(int qSetId,
			 TranslnsMap headerTranslns) throws DBException {
		final String SELF = "TranslnWrite.assignHeaderTranslations: ";
		Connection con = null;
		if (headerTranslns != null && !headerTranslns.isEmpty()) try {
			con = getPoolConnection();
			final int phraseId = nextSequence(con, PHRASE_SEQ);
			final String qry = 
					UPDATE + QSETS
					+ SET + QSET_COMMONQSTMT_ID + EQUALS + QMARK
					+ WHERE + QSET_ID + EQUALS + QMARK;
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
					phraseId,
					qSetId);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
			storeTranslations(con, phraseId, headerTranslns);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // assignHeaderTranslations(int, TranslnsMap)

	/** Stores one phrase's translations into several languages.
	 * @param	con	database connection
	 * @param	phraseId	unique ID of the item being translated
	 * @param	phraseTranslns	TranslnsMap of a phrase
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private void storeTranslations(Connection con, int phraseId,
			TranslnsMap phraseTranslns) throws DBException {
		final String SELF = "TranslnWrite.storeTranslations: ";
		final String[] fields = new String[] {
				PHRASE_ID,
				PHRASE_LANG,
				PHRASE_TRANSLN, // CLOB field
				PHRASE_TRANSLATOR};
		final String qry = getInsertIntoValuesQMarksSQL(TRANSLATIONS, fields);
		debugPrint(SELF, qry); 
		PreparedStatement stmt = null;
		try {
			stmt = con.prepareStatement(qry);
			int batchNum = 0;
			final List<String> languages = phraseTranslns.getLanguages();
			for (final String language : languages) {
				final String translation = phraseTranslns.get(language);
				final StringBuilder joinedValues = setValues(stmt,
						phraseId,
						language,
						translation,
						translatorId);
				debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
				stmt.addBatch();
				batchNum++;
				if (batchNum % 100 == 0) {
					stmt.executeBatch();
					stmt.clearBatch();
				} // if time to submit
			} // for each language and translation
			if (batchNum % 100 != 0) {
				stmt.executeBatch();
			} // if need to submit
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(null, stmt, null);
		}
	} // storeTranslations(Connection, int, TranslnsMap)

	/** Adds English phrases to the database if they are not already in it.
	 * @param	phrases	array of English phrases
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void addEnglish(String[] phrases) throws DBException {
		final String SELF = "TranslnWrite.addEnglish: ";
		if (Utils.isEmpty(phrases)) {
			debugPrint(SELF + "no phrases to translate.");
			return;
		} // if there are no phrases
		final Map<Integer, String> phrasesMap = new HashMap<Integer, String>();
		for (final String phrase : phrases) {
			final String modPhrase = phrase.trim();
			phrasesMap.put(TranslnRead.getPhraseId(modPhrase), modPhrase);
		} // for each phrase
		Connection con = null;
		try {
			con = getPoolConnection();
			addEnglish(con, phrasesMap);
		} catch (SQLException e) {
			alwaysPrint(SELF + "couldn't add English phrases to database");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
	} // addEnglish(String[])

	/** Adds English phrases to the database if they are not already in it.
	 * @param	con	database connection
	 * @param	phrases	map of English phrases by IDs
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	static void addEnglish(Connection con, Map<Integer, String> phrases) 
			throws SQLException {
		cullToNotInDatabase(con, phrases);
		addToDatabase(con, phrases);
	} // addEnglish(Connection, Map<Integer, String>)

	/** Removes from the map those English phrases that are already in the database.
	 * @param	con	database connection
	 * @param	phrases	map of English phrases by IDs
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static void cullToNotInDatabase(Connection con, 
			Map<Integer, String> phrases) throws SQLException {
		final String SELF = "TranslnWrite.cullToNotInDatabase: ";
		if (phrases.isEmpty()) return;
		// find which of the English phrases are in database
		final List<Integer> phraseIds = 
				new ArrayList<Integer>(phrases.keySet());
		final String qry = toString(
				SELECT + PHRASE_ID
				+ FROM + TRANSLATIONS
				+ WHERE + PHRASE_ID + IN, parensQMarks(phraseIds),
				AND + PHRASE_LANG + EQUALS, quotes(ENGLISH));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				phraseIds);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				phrases.remove(Integer.valueOf(rs.getInt(PHRASE_ID)));
			} // if we have a new phraseID
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try
	} // cullToNotInDatabase(Connection, Map<Integer, String>)

	/** Adds English phrases (known not to be in the database) to the database.
	 * @param	con	database connection
	 * @param	phrases	map of English phrases by IDs
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void addToDatabase(Connection con, 
			Map<Integer, String> phrases) throws SQLException {
		final String SELF = "TranslnWrite.addToDatabase: ";
		if (phrases.isEmpty()) return;
		alwaysPrint(SELF, phrases.size(), " phrase(s) new to the "
				+ "database to store in English.");
		final String[] fields = new String[] {
				PHRASE_ID,
				PHRASE_LANG,
				PHRASE_TRANSLN}; // CLOB field
		final String qry = getInsertIntoValuesQMarksSQL(TRANSLATIONS, fields);
		debugPrint(SELF, qry); 
		PreparedStatement stmt = null;
		try {
			stmt = con.prepareStatement(qry);
			final List<Integer> phraseIds = 
					new ArrayList<Integer>(phrases.keySet());
			int batchNum = 0;
			for (final Integer phraseIdObj : phraseIds) {
				final String phrase = phrases.get(phraseIdObj);
				final StringBuilder joinedValues = setValues(stmt,
						phraseIdObj,
						ENGLISH,
						phrase);
				debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
				stmt.addBatch();
				batchNum++;
				if (batchNum % 100 == 0) {
					stmt.executeBatch();
					stmt.clearBatch();
				} // if time to submit
			} // for each phrase
			if (batchNum % 100 != 0) {
				stmt.executeBatch();
			} // if need to submit
		} finally {
			closeConnection(null, stmt, null);
		} // try
	} // addToDatabase(Connection, Map<Integer, String>)

	/** Deletes an English phrase and all its translations from the database.
	 * @param	phrase	phrase to delete
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void deleteEnglish(String phrase) throws DBException {
		if (phrase != null) {
			final Integer phraseIdObj = TranslnRead.getPhraseId(phrase);
			if (phraseIdObj != null) deleteEnglish(phraseIdObj.intValue());
		} // if phrase is not null
	} // deleteEnglish(String)

	/** Deletes an English phrase and all its translations from the database.
	 * @param	phraseId	ID of the phrase to delete
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void deleteEnglish(int phraseId) throws DBException {
		final String SELF = "TranslnWrite.deleteEnglish: ";
		final String qry = toString(DELETE_TRANSLATION_BY_ID(), QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				phraseId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			alwaysPrint(SELF + "couldn't delete English phrase "
					+ "and its translations: ", phraseId);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
	} // deleteEnglish(int)

	/** Gets the start of an SQL query to delete a translation by its ID.
	 * @return	start of an SQL query to delete a translation by its ID
	 */
	static String DELETE_TRANSLATION_BY_ID() {
		return DELETE_FROM + TRANSLATIONS + WHERE + PHRASE_ID + EQUALS;
	} // DELETE_TRANSLATION_BY_ID()

	/** Gets the start of an SQL query to delete translations by their IDs.
	 * @return	start of an SQL query to delete translations by their IDs
	 */
	static String DELETE_TRANSLATIONS_BY_IDS() {
		return DELETE_FROM + TRANSLATIONS + WHERE + PHRASE_ID + IN;
	} // DELETE_TRANSLATIONS_BY_IDS()

} // TranslnWrite
