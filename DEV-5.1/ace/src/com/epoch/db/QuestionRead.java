package com.epoch.db;

import static com.epoch.db.dbConstants.LanguagesConstants.*;
import static com.epoch.db.dbConstants.QuestionsRWConstants.*;
import com.epoch.AppConfig;
import com.epoch.constants.AuthorConstants;
import com.epoch.evals.Evaluator;
import com.epoch.evals.Subevaluator;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.genericQConstants.TableQConstants;
import com.epoch.qBank.Figure;
import com.epoch.qBank.QDatum;
import com.epoch.qBank.Question;
import com.epoch.qBank.CaptionsQDatum;
import com.epoch.qBank.EDiagramQDatum;
import com.epoch.qBank.qBankConstants.CaptionsQDatumConstants;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.utils.Utils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Instantiable class containing methods to read a question from the database. */
final class QuestionRead extends DBCommon 
		implements AuthorConstants, CaptionsQDatumConstants,
		QuestionConstants, TableQConstants {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Login ID of the instructor (null if master). */
	transient private String instructorId = MASTER_AUTHOR;
	/** Full or light (flags, question data only). */
	transient private boolean fullLoad = FULL_LOAD;
	/** Whether to add common question statements to these questions'
	 * statements. */
	transient private boolean addHeader = ADD_HEADER;
	/** Languages of this user in order of preference; null
	 * if English only. */
	transient private String[] userLangs = (AppConfig.notEnglish 
			? new String[] {AppConfig.defaultLanguage} : NO_LANGUAGES);
	/** Partial SQL query specifying which questions to choose from the 
	 * database, and values to substitute for question marks. */
	transient private SQLWithQMarks qIdSelector;
	/** Map of questions by IDs. */
	final private Map<Integer, Question> qMap = 
			new HashMap<Integer, Question>();
	/** Map of common question statements by question set IDs. */
	final private Map<Integer, String> headers = new HashMap<Integer, String>();
	/** Connection to the database (pooled). */
	transient private Connection con;
	/** Master or local database tables. */
	transient private DBTables tables;

	/** Constructor.  */
	QuestionRead() {
		setTables();
	} // QuestionRead()

	/** Constructor.  
	 * @param	connxn	an established database connection (pooled)
	 */
	QuestionRead(Connection connxn) {
		con = connxn;
		setTables();
	} // QuestionRead(Connection)

	/** Constructor. 
	 * @param	instrId	instructor's login ID
	 */
	QuestionRead(String instrId) {
		instructorId = instrId;
		setTables();
	} // QuestionRead(String)

	/** Constructor. 
	 * @param	addHead	whether to add the common question statement to this
	 * question's statement
	 */
	QuestionRead(boolean addHead) {
		addHeader = addHead;
		setTables();
	} // QuestionRead(boolean)

	/** Constructor. 
	 * @param	instrId	instructor's login ID
	 * @param	addHead	whether to add the common question statement to this
	 * question's statement
	 */
	QuestionRead(String instrId, boolean addHead) {
		instructorId = instrId;
		addHeader = addHead;
		setTables();
	} // QuestionRead(String, boolean)

	/** Constructor. 
	 * @param	addHead	whether to add the common question statement to this
	 * question's statement
	 * @param	full	full or light (flags, question data only)
	 */
	QuestionRead(boolean addHead, boolean full) {
		addHeader = addHead;
		fullLoad = full;
		setTables();
	} // QuestionRead(boolean, boolean)

	/** Constructor. 
	 * @param	instrId	instructor's login ID
	 * @param	addHead	whether to add the common question statement to this
	 * question's statement
	 * @param	full	full or light (flags, question data only)
	 */
	QuestionRead(String instrId, boolean addHead, boolean full) {
		instructorId = instrId;
		addHeader = addHead;
		fullLoad = full;
		setTables();
	} // QuestionRead(String, boolean, boolean)

	/** Constructor. 
	 * @param	instrId	instructor's login ID
	 * @param	addHead	whether to add the common question statement to this
	 * question's statement
	 * @param	full	full or light (flags, question data only)
	 * @param	langs	languages of this user in order of preference; null
	 * if English only
	 */
	QuestionRead(String instrId, boolean addHead, boolean full, 
			String[] langs) {
		instructorId = instrId;
		addHeader = addHead;
		fullLoad = full;
		userLangs = langs;
		setTables();
	} // QuestionRead(String, boolean, boolean, String[])

	/** Constructor. 
	 * @param	instrId	instructor's login ID
	 * @param	addHead	whether to add the common question statement to this
	 * question's statement
	 * @param	full	full or light (flags, question data only)
	 * @param	langs	languages of this user in order of preference; null
	 * if English only
	 * @param	myTables	tables for master- or locally authored questions
	 */
	QuestionRead(String instrId, boolean addHead, boolean full, 
			String[] langs, DBTables myTables) {
		instructorId = instrId;
		addHeader = addHead;
		fullLoad = full;
		userLangs = langs;
		tables = myTables;
	} // QuestionRead(String, boolean, boolean, String[], Connection, DBTables)

/* *************** Short methods *****************/

	/** Sets the value of the partial SQL query specifying which questions to 
	 * choose from the database, and values to substitute for question marks.
	 * @param	selector	partial query specifying which questions to choose
	 * from the database, and values to substitute for question marks
	 */
	void setQIdSelector(SQLWithQMarks selector) 	{ qIdSelector = selector; }
	/** Gets whether the instructor is the master author.
	 * @return	true if the instructor is not the master author
	 */
	boolean isLocal() 						{ return instructorId != MASTER_AUTHOR; }
	/** Gets the database tables of this instance.
	 * @return	database tables
	 */
	DBTables tables() 						{ return tables; }
	/** Gets the name of the Q_AUTHOR field in local tables.
	 * @return	Q_AUTHOR value
	 */
	String Q_AUTHOR() 						{ return DBLocalTables.Q_AUTHOR; }
	/** Gets the map of questions keyed by their IDs.
	 * @return	map of questions keyed by their IDs
	 */
	Map<Integer, Question> getQMap() 		{ return qMap; }

	/** Sets the instructor to the master author. */
	void setToMaster() { 
		instructorId = MASTER_AUTHOR;
		setTables();
	} // setToMaster()

	/** Initializes database tables from which to retrieve the questions. */
	private void setTables() 				{ tables = getTables(isLocal()); }
	/** Get a question from the map.
	 * @param	qId	question ID number
	 * @return	the question
	 */
	private Question getQFromMap(int qId) 	{ return qMap.get(Integer.valueOf(qId)); }

	/** Populates the map of questions keyed by qId from a list of questions.
	 * @param	qList	a list of questions
	 */
	private void setQMap(List<Question> qList) {
		for (final Question oneQ : qList) {
			qMap.put(Integer.valueOf(oneQ.getQId()), oneQ);
		} // for each question
	} // setQMap(List<Question>)

	/** Appends the common question statement to a question statement.  Called
	 * by getSingletonData() and TranslnRead.modifyItem().
	 * @param	header	common question statement
	 * @param	qStmt	one question's question statement
	 * @return	modified question statement
	 */
	static String appendHeader(String header, String qStmt) {
		debugPrint("QuestionRead.appendHeader: appending header:\n",
				header, "\nto qStmt:\n", qStmt);
		return (header == null ? qStmt : toString(header, ' ', qStmt));
	} // appendHeader(String, String)

/* *************** Methods to get questions *****************/

	/** Gets a single question.
	 * @param	qId	unique ID of the question
	 * @return	a question, null if not found
	 * @throws	DBException	if there's a problem reading the database
	 */
	Question getQuestion(int qId) throws DBException {
		final String SELF = "QuestionRead.getQuestion: ";
		debugPrint(SELF + "qId = ", qId);
		final String qIdsQry = toString(parens(QMARK));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qIdsQry,
				qId);
		if (isLocal()) {
			sql_vals.addToSql(AND, Q_AUTHOR(), EQUALS + QMARK);
			sql_vals.addValue(instructorId);
		} // if question is local
		setQIdSelector(sql_vals);
		final List<Question> list = getQuestions(ANY_ORDER);
		final boolean empty = list.isEmpty();
		debugPrint(SELF, (empty ? "got no questions back for"
				: "returning question with"), " qId = ", qId);
		return (empty ? null : list.get(0));
	} // getQuestion(int)

	/** Sets up connection pool to return all desired questions.
	 * @param	orderBy	how to order the questions
	 * @return	questions keyed by IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	List<Question> getQuestions(String orderBy) throws DBException {
		Connection connxn = null;
		try {
			connxn = getPoolConnection();
			return getQuestions(connxn, orderBy);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(connxn);
		}
	} // getQuestions(String)

	/** Gets all desired questions.
	 * @param	connxn	an established database connection (pooled)
	 * @param	orderBy	how to order the questions
	 * @return	questions keyed by IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	List<Question> getQuestions(Connection connxn, String orderBy) 
			throws DBException {
		final String SELF = "QuestionRead.getQuestions: ";
		con = connxn;
		debugPrint("Entering " + SELF + "qIdSelector = ", 
				qIdSelector, ", orderBy = ", orderBy);
		if (addHeader && fullLoad) getHeaders();
		final List<Question> questions = getSingletonData(orderBy);
		if (!questions.isEmpty()) {
			// getSingletonData() may already have populated the map
			if (qMap.isEmpty()) setQMap(questions);
			boolean gotEvals = true;
			if (fullLoad) {
				// get evaluators and figures
				gotEvals = getEvaluators();
				if (gotEvals) setFigures();
				else {
					debugPrint(SELF + "eval load failed, "
							+ "returning empty list.");
					questions.clear();
				} // if failed to load the evaluators
			} // if full load
			// get qData for either full or light load
			if (gotEvals) setQData();
		} // if there are questions to populate
		return questions;
	} // getQuestions(Connection, String)

	/** Utility method to retrieve headers.
	 * @throws	DBException	if there's a problem reading the database
	 */
	private void getHeaders() throws DBException {
		final String SELF = "QuestionRead.getHeaders: ";
		final boolean haveUserLangs = !Utils.isEmpty(userLangs);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			final StringBuilder qSetsSqlBld = parensBuild(
					SELECT + Q_QSET + FROM, tables.QUESTIONS,
					WHERE + Q_QID + IN, qIdSelector.getSql());
			final SQLWithQMarks qSetsSql_vals = 
					new SQLWithQMarks(qSetsSqlBld, qIdSelector);
			final Map<Integer, String> modHeadersByHeaderIds =
					new HashMap<Integer, String>();
			if (instructorId != null) {
				try {
					final String qry = toString(
							SELECT, joinAll( 
								MODHEAD_QSETID, 
								MODHEAD_COMMONQSTATEMENT),
							FROM + MODIFIED_HEADERS 
							+ WHERE + MODHEAD_AUTHOR + EQUALS + QMARK 
							+ AND + MODHEAD_QSETID + IN, 
								qSetsSql_vals.getSql());
					final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
							instructorId);
					sql_vals.addValuesFrom(qSetsSql_vals);
					debugPrint(SELF, sql_vals);
					stmt = getStatement(con, sql_vals);
					rs = stmt.executeQuery();
					while (rs.next()) {
						final int setId = rs.getInt(MODHEAD_QSETID);
						final String modHeader =
								rs.getString(MODHEAD_COMMONQSTATEMENT);
						if (modHeader != null) {
							modHeadersByHeaderIds.put(
									Integer.valueOf(setId), modHeader);
						} // if modHeader is not null
					} // while there are more modified headers to find
				} catch (Exception e0) {
					Utils.alwaysPrint(SELF 
							+ "couldn't get locally modified headers.");
					e0.printStackTrace();
				} finally {
					closeConnection(null, null, rs);
				} // try
			} // if instructor is not null
			debugPrint(SELF + "found ", modHeadersByHeaderIds.size(),
					" modified headers");
			final String qry = toString(
					SELECT, joinAll( 
						QSET_ID, 
						QSET_COMMONQSTATEMENT, 
						QSET_COMMONQSTMT_ID),
					FROM, QSETS,
					WHERE + QSET_ID + IN, qSetsSql_vals.getSql());
			final SQLWithQMarks sql_vals = 
					new SQLWithQMarks(qry, qSetsSql_vals);
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			final List<Integer> headerIds = new ArrayList<Integer>();
			final Map<Integer, ArrayList<int[]>> qSetIdsByHeaderIds =
					new HashMap<Integer, ArrayList<int[]>>();
			while (rs.next()) {
				final int setId = rs.getInt(QSET_ID);
				String header = rs.getString(QSET_COMMONQSTATEMENT);
				final int headerId = rs.getInt(QSET_COMMONQSTMT_ID);
				final String modHeader = 
						modHeadersByHeaderIds.get(Integer.valueOf(setId));
				debugPrint(SELF + "for qSet ", setId, ", header = ",
						header, "\nand modHeader = ", modHeader);
				if (modHeader != null) {
					header = modHeader;
				} else if (haveUserLangs && headerId != 0) {
					final Integer commonQStmtId = Integer.valueOf(headerId);
					headerIds.add(commonQStmtId);
					// qSetIdsByHeaderIds.put(commonQStmtId, new int[] {setId});
					ArrayList<int[]> qSetIdsList = 
							qSetIdsByHeaderIds.get(commonQStmtId);
					if (qSetIdsList == null) {
						qSetIdsList = new ArrayList<int[]>();
						qSetIdsByHeaderIds.put(commonQStmtId, qSetIdsList);
					} // if starting a new list
					qSetIdsList.add(new int[] {setId});
				} // if we should look for a translation
				if (!Utils.isEmptyOrWhitespace(header))
					headers.put(Integer.valueOf(setId), header);
			} // while there are results
			if (haveUserLangs) {
				debugPrint(SELF + "userLangs = ", userLangs,
						", headerIds with translations = ", headerIds);
				TranslnRead.getTranslatedParts(con,
						headerIds, qSetIdsByHeaderIds, null, headers,
						userLangs, TranslnRead.HEADERS);
			} else debugPrint(SELF + "haveUserLangs = false");
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
	} // getHeaders()

	/** Utility method to retrieve a question's singleton data such as
	 * statement, flags, etc.  If translations are sought, will also populate
	 * the map of questions by ID numbers.
	 * @param	orderBy	how to order the questions
	 * @return	true if at least one Q was retrieved successfully
	 * @throws	DBException	if there's a problem reading the database
	 */
	private List<Question> getSingletonData(String orderBy) 
			throws DBException {
		final String SELF = "QuestionRead.setSingletonData: ";
		final List<Question> questions = new ArrayList<Question>();
		final boolean mayTranslate = !Utils.isEmpty(userLangs);
		final StringBuilder qryBld = getBuilder(
				SELECT, joinAll( 
					Q_QID, 
					Q_TYPE,
					Q_FLAGS));
		if (fullLoad) {
 			qryBld.append(postjoin(
					Q_STATEMENT, 
					Q_STMT_ID,
					Q_QSET, 
					Q_BOOK, 
					Q_BOOKCHAP, 
					Q_REMARKS, 
					Q_NUM, 
					Q_KEYWORDS));
		} // if full load
		final String qry = toString(qryBld,
				FROM, tables.QUESTIONS,
				WHERE + Q_QID + IN, qIdSelector.getSql(),
				orderBy);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, qIdSelector);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				debugPrint(SELF + "No questions retrieved.");
				return questions;
			}
			final List<Integer> qStmtIds = new ArrayList<Integer>();
			final Map<Integer, ArrayList<int[]>> qIdsByQStmtIds =
					new HashMap<Integer, ArrayList<int[]>>();
			do {
				final int qId = rs.getInt(Q_QID);
				debugPrint(SELF + "retrieving Q ", qId);
				final Question question = new Question(qId);
				question.setQType(
						Utils.indexOf(DB_QTYPES, rs.getString(Q_TYPE)));
				question.setQFlags(rs.getLong(Q_FLAGS));
				question.setMasterQ(!tables.local);
				question.setAuthorId(instructorId);
				if (fullLoad) {
					String qStmt = rs.getString(Q_STATEMENT);
					final int setId = rs.getInt(Q_QSET);
					final int serialNo = rs.getInt(Q_NUM);
					final String book = rs.getString(Q_BOOK);
					final String chapter = rs.getString(Q_BOOKCHAP);
					final String remarks = rs.getString(Q_REMARKS);
					final String keywords = rs.getString(Q_KEYWORDS);
					final int qStmtId = rs.getInt(Q_STMT_ID);
					if (qStmtId != 0 && mayTranslate) {
						final Integer stmtId = Integer.valueOf(qStmtId);
						qStmtIds.add(stmtId);
						ArrayList<int[]> qStmtIdsList = 
								qIdsByQStmtIds.get(stmtId);
						if (qStmtIdsList == null) {
							qStmtIdsList = new ArrayList<int[]>();
							qIdsByQStmtIds.put(stmtId, qStmtIdsList);
						} // if starting a new list
						qStmtIdsList.add(new int[] {qId});
					} // if there may be a translation
					if (qStmt == null) qStmt = "";
					// add the header, if any
					final String header = headers.get(Integer.valueOf(setId));
					question.setStatement(appendHeader(header, qStmt));
					question.setOrigStatement(qStmt);
					question.setPhraseId(qStmtId);
					question.setBook(book);
					question.setChapter(chapter);
					question.setRemarks(remarks);
					question.setKeywords(keywords);
					question.setQSetId(setId);
					question.setSerialNo(serialNo);
				} // if not light load
				questions.add(question);
			} while (rs.next());
			// get translation of question statements if master Qs
			if (mayTranslate) {
				debugPrint(SELF + "userLangs = ", userLangs,
						", qStmtIds with translations = ", qStmtIds);
				setQMap(questions);
				TranslnRead.getTranslatedParts(con, qStmtIds,
						qIdsByQStmtIds, qMap, headers, userLangs,
						TranslnRead.QSTMTS);
			} else debugPrint(SELF + "mayTranslate = false");
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		return questions;
	} // getSingletonData(String)

	/** Utility method to retrieve evaluators.
	 * Browses through the set with fixed qId, majorId and all minorIds.
	 * <P>qId, majorId, minorId:
	 * <br>10, 1, 0
	 * <br>10, 2, 0
	 * <br>10, 2, 1
	 * <br>10, 2, 2
	 * <p>Conditions: result must contain at least one element
	 * <br>	 	res must be sorted by qId, majorId, minorId
	 * @return	true if no exceptions occurred
	 * @throws	DBException	if there's a problem reading the database
	 */
	private boolean getEvaluators() throws DBException {
		final String SELF = "QuestionRead.getEvaluators: ";
		final boolean mayTranslate = !Utils.isEmpty(userLangs);
		final String qry = toString(
				SELECT, joinAll(
					EVAL_QID,
					EVAL_MAJORID,
					EVAL_MINORID,
					EVAL_SUBEXPR,
					EVAL_TYPE,
					EVAL_FEEDBACK,
					EVAL_FEEDBACKID,
					EVAL_GRADE,
					EVAL_CODEDDATA,
					EVAL_MOLNAME,
					EVAL_MOLSTRUCT),
				FROM, tables.EVALUATORS,
				WHERE + EVAL_QID + IN, qIdSelector.getSql(),
				ORDER_BY, joinAll(
					EVAL_QID,
					EVAL_MAJORID,
					EVAL_MINORID));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, qIdSelector);
		debugPrint(SELF, sql_vals);
		// create a statement whose ResultSet can be read twice
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (!rs.next()) return false; // no evaluators!
			final List<Integer> feedbackIds = new ArrayList<Integer>();
			final Map<Integer, ArrayList<int[]>> evalIdsByFeedbackIds =
					new HashMap<Integer, ArrayList<int[]>>();
			while (!rs.isAfterLast()) {
				final int qId = rs.getInt(EVAL_QID);
				final Question question = getQFromMap(qId);
				if (question == null) { // unlikely
					Utils.alwaysPrint(SELF + "question #", qId, 
							" is not present in the list of questions.");
					while (!rs.isAfterLast()
							&& qId == rs.getInt(EVAL_QID)) {
						rs.next(); // advance to next Q
					} // until the next question
					continue;
				} // if question not found null 
				while (!rs.isAfterLast()
						&& qId == rs.getInt(EVAL_QID)) {
					final int majorId = rs.getInt(EVAL_MAJORID);
					final Evaluator eval = new Evaluator();
					eval.qId = qId;
					eval.majorId = majorId;
					eval.oldMajorId = majorId;
					eval.exprCode = rs.getString(EVAL_SUBEXPR);
					eval.feedback = rs.getString(EVAL_FEEDBACK);
					eval.grade = rs.getDouble(EVAL_GRADE);
					eval.phraseId = rs.getInt(EVAL_FEEDBACKID);
					while (!rs.isAfterLast()
							&& qId == rs.getInt(EVAL_QID)
							&& majorId == rs.getInt(EVAL_MAJORID)) {
						final Subevaluator subeval = new Subevaluator();
						final int minorId = rs.getInt(EVAL_MINORID);
						debugPrint(SELF + "record qId = ", qId, ", majorId = ",
								majorId, ", minorId = ", minorId);
						subeval.matchCode = rs.getString(EVAL_TYPE);
						subeval.codedData = rs.getString(EVAL_CODEDDATA);
						subeval.molName = rs.getString(EVAL_MOLNAME);
						subeval.molStruct = 
								decompressIfLewis(rs.getString(EVAL_MOLSTRUCT));
						final boolean haveMatchCode = 
								!Utils.isEmpty(subeval.matchCode);
						if (haveMatchCode) {
							subeval.setEvaluatorImpl();
							if (subeval.getEvalImpl() == null) {
								Utils.alwaysPrint(SELF + "subevaluator "
										+ "class ", subeval.matchCode, 
										" cannot be instantiated.");
								question.setCorrupted();
								while (!rs.isAfterLast()
										&& qId == rs.getInt(EVAL_QID)) {
									rs.next(); // advance to next Q
								} // while still at this question
							} else {
								eval.addSubevaluator(subeval);
								debugPrint(SELF + "adding subevaluator ",
										eval.getNumSubevaluators(), 
										" with minorId ", minorId, 
										" to evaluator ", majorId,
										" of Q ", qId, ": matchCode = ",
										subeval.matchCode, ", codedData = ",
										subeval.codedData, ", molName = ",
										subeval.molName);
							} // if evaluator implementation worked
						} else if (minorId != 0) {
							Utils.alwaysPrint(SELF + "subevaluator ", 
									minorId, " of evaluator ", 
									majorId, " of Q ", qId, " has no "
									+ "match code but has minorId > 1; "
									+ "question is corrupted.");
							question.setCorrupted();
							while (!rs.isAfterLast()
									&& qId == rs.getInt(EVAL_QID)) {
								rs.next(); // advance to next Q
							} // while still at this question
						} else debugPrint(SELF + "evaluator ", majorId, 
								" of Q ", qId, " is complex; expect to "
								+ "load subevaluators.");
						if (!question.isCorrupted()) rs.next();
					} // while there are more evaluatorData
					if (!question.isCorrupted()) {
						question.addEvaluator(eval);
						if (eval.phraseId != 0) {
							debugPrint(SELF + "eval with qId ", eval.qId,
									" and majorId ", eval.majorId,
									" has feedbackId ", eval.phraseId);
							final Integer feedbackId = 
									Integer.valueOf(eval.phraseId);
							feedbackIds.add(feedbackId);
							ArrayList<int[]> evalIdsList = 
									evalIdsByFeedbackIds.get(feedbackId);
							if (evalIdsList == null) {
								evalIdsList = new ArrayList<int[]>();
								evalIdsByFeedbackIds.put(feedbackId, 
										evalIdsList);
							} // if starting a new list
							evalIdsList.add(new int[] {eval.qId, eval.majorId});
						} // if there's a feedback ID
					} // if eval is not null
				} // while there are evaluators for this Q
			} // while there are more Qs
			// get translation of evaluator feedbacks if master Qs
			if (mayTranslate) {
				debugPrint(SELF + "userLangs = ", userLangs,
						", feedbackIds with translations = ", feedbackIds);
				TranslnRead.getTranslatedParts(con, feedbackIds,
						evalIdsByFeedbackIds, qMap, null, userLangs,
						TranslnRead.EVALS);
			} else debugPrint(SELF + "mayTranslate = false");
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		return true;
	} // getEvaluators()

	/** Utility method to retrieve figures.
	 * @throws	DBException	if there's a problem reading the database
	 */
	private void setFigures() throws DBException {
		final String SELF = "QuestionRead.setFigures: ";
		final String qry = toString(
				SELECT, joinAll(
					FIG_QID,
					FIG_NUM,
					FIG_TYPE,
					FIG_FIGID,
					FIG_ADDL_DATA,
					FIG_MAIN_DATA),
				FROM, tables.FIGURES,
				WHERE + FIG_QID + IN, qIdSelector.getSql(),
				ORDER_BY, joinAll(
					FIG_QID,
					FIG_NUM));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, qIdSelector);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs != null && rs.next()) {
				// read the next figure
				final Figure figure = new Figure();
				figure.questionId = rs.getInt(FIG_QID);
				figure.serialNo = rs.getInt(FIG_NUM);
				figure.type = figure.getFigureTypeIntValue(
						rs.getString(FIG_TYPE));
				debugPrint(SELF + "getting figure ", figure.serialNo,
						" of Q", figure.questionId, " of type ", figure.type);
				if (figure.isUnknownType()) continue;
				figure.figureId = rs.getInt(FIG_FIGID);
				final Question question = getQFromMap(figure.questionId);
				if (question == null) {
					Utils.alwaysPrint(SELF + "ERROR: Q ",
							figure.questionId, " is null.");
					continue;
				} // if Q is null
				// create or retrieve the image associated with the figure
				if (figure.isImage() || figure.isImageAndVectors()) {
					final int figId = figure.figureId;
					ImageRW imgReader = new ImageRW(con, instructorId);
					String ext = imgReader.getExtension(figId);
					if (ext == null) {
						imgReader = new ImageRW(con);
						ext = imgReader.getExtension(figId);
					} // if extension is null
					figure.bufferedImage = question.makeImageFileName(figId, 
							ext, tables.local 
								? DBLocalTables.LOCAL_PREFIX : "");
					debugPrint(SELF + "figure.bufferedImage = ", 
							figure.bufferedImage);
					if (figure.isImageAndVectors()) {
						figure.data = rs.getString(FIG_MAIN_DATA);
					}
				} else { // build image for any other figure type
					if (figure.isReaction() 
							|| figure.isSynthesis() || figure.isJmol()) {
						figure.addlData = rs.getString(FIG_ADDL_DATA);
					}
					figure.data = 
							decompressIfLewis(rs.getString(FIG_MAIN_DATA));
					if (figure.data == null) { // shouldn't happen
						Utils.alwaysPrint(SELF + "figure ", figure.figureId,
								" of type ", Figure.DBVALUES[figure.type],
								" with serialNo ", figure.serialNo,
								" in question ", figure.questionId,
								" has null structure.");
						continue;
					} /* else debugPrint(SELF + "figure data:\n", 
							figure.data); /**/
				} // if figure type
				question.addFigure(figure);
			} // while there is another figure
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
	} // setFigures()

	/** Utility method to retrieve question data.
	 * @throws	DBException	if there's a problem reading the database
	 */
	private void setQData() throws DBException {
		final String SELF = "QuestionRead.setQData: ";
		final boolean mayTranslate = !Utils.isEmpty(userLangs);
		final String qry = toString(
				SELECT, joinAll(
					QD_DATUMID,
					QD_QID,
					QD_NUM,
					QD_TYPE,
					QD_DATA,
					QD_NAME,
					QD_TEXT_ID),
				FROM, tables.QUESTIONDATA,
				WHERE + QD_QID + IN, qIdSelector.getSql(),
				ORDER_BY, joinAll(
					QD_QID,
					QD_NUM));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, qIdSelector);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			final List<Integer> textIds = new ArrayList<Integer>();
			final Map<Integer, ArrayList<int[]>> dataIdsByTextIds =
					new HashMap<Integer, ArrayList<int[]>>();
			final List<Integer> captionQIds = new ArrayList<Integer>();
			while (rs != null && rs.next()) {
				final int qId = rs.getInt(QD_QID);
				final Question question = getQFromMap(qId);
				if (question == null) {
					Utils.alwaysPrint(SELF + "ERROR: Q ",
							qId, " is null.");
					continue;
				} // if Q is null
				final int serialNo = rs.getInt(QD_NUM);
				final boolean mayHaveCapsOrLabels = 
						(question.isTable() && serialNo <= COL_DATA + 1)
						|| (question.isED() && serialNo <= 1);
				final QDatum qDatum = (mayHaveCapsOrLabels
							&& question.isED() ? new EDiagramQDatum()
						: mayHaveCapsOrLabels ? new CaptionsQDatum() 
						: new QDatum());
				if (mayHaveCapsOrLabels) debugPrint(SELF + "qDatum ",
						serialNo, " of ", question.isED() ?
							"energy diagram" : "complete-the-table",
						" Q ", qId, " may have captions",
						question.isED() ? " or labels" : "", 
						"; set to class ", question.isED() ?
						"EDiagramQDatum." : "CaptionsQDatum.");
				qDatum.questionId = qId;
				qDatum.serialNo = serialNo;
				qDatum.dataId = rs.getInt(QD_DATUMID);
				qDatum.dataType = qDatum.getQdataTypeIntValue(
						rs.getString(QD_TYPE));
				qDatum.name = rs.getString(QD_NAME);
				qDatum.phraseId = rs.getInt(QD_TEXT_ID);
				if (qDatum.phraseId != 0) {
					debugPrint(SELF + "qDatum with qId ", 
							qDatum.questionId,
							" and serialNo ", qDatum.serialNo,
							" has textId ", qDatum.phraseId);
					final Integer textId = 
							Integer.valueOf(qDatum.phraseId);
					textIds.add(textId);
					ArrayList<int[]> dataIdsList = 
							dataIdsByTextIds.get(textId);
					if (dataIdsList == null) {
						dataIdsList = new ArrayList<int[]>();
						dataIdsByTextIds.put(textId, dataIdsList);
					} // if starting a new list
					dataIdsList.add(new int[] {qDatum.questionId, 
							qDatum.serialNo, qDatum.dataType});
				} // if there may be a translation
				if (fullLoad || qDatum.isText()) {
					// data not needed for light load unless it's text
					qDatum.data = decompressIfLewis(rs.getString(QD_DATA));
				} // if TEXT or full load
				question.populateQDatum(qDatum);
				if (mayHaveCapsOrLabels) {
					final Integer qIdObj = Integer.valueOf(qDatum.questionId);
					if (!captionQIds.contains(qIdObj)) {
						captionQIds.add(qIdObj);
					} // if not already in list
				} // if question is complete-the-table
			} // while rs.next
			if (!Utils.isEmpty(captionQIds)) {
				setCaptions(captionQIds);
			} // if there are complete-the-table questions
			// get translation of question data if master Qs
			if (mayTranslate) {
				debugPrint(SELF + "userLangs = ", userLangs,
						", textIds with translations = ", textIds);
				TranslnRead.getTranslatedParts(con, textIds,
						dataIdsByTextIds, qMap, null, userLangs,
						TranslnRead.QDATA);
			} else debugPrint(SELF + "mayTranslate = false");
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
	} // setQData()
	
	/** Utility method to retrieve row and column captions for
	 * complete-the-table questions.
	 * @param	captionQIds	unique IDs of complete-the-table questions
	 * whose question data have been retrieved
	 * @throws	DBException	if there's a problem reading the database
	 */
	private void setCaptions(List<Integer> captionQIds) 
			throws DBException {
		final String SELF = "QuestionRead.setCaptions: ";
		PreparedStatement stmt = null;
		ResultSet rs = null;
		if (!Utils.isEmpty(captionQIds)) try {
			debugPrint(SELF + "getting row/column captions in "
					+ "energy diagram and complete-the-table questions, "
					+ "and labels and y-axis scale info for energy diagram "
					+ "questions, in questions with IDs ", captionQIds);
			final char ROW_DBVALUE = CAPTS_TYPE_DBVALUES[ROW_DATA].charAt(0);
			final char COL_DBVALUE = CAPTS_TYPE_DBVALUES[COL_DATA].charAt(0);
			final char LABEL_DBVALUE = CAPTS_TYPE_DBVALUES[LABEL_DATA].charAt(0);
			final char Y_AXIS_DBVALUE = CAPTS_TYPE_DBVALUES[Y_AXIS_DATA].charAt(0);
			final String qry = toString(
					SELECT, joinAll(
						CAPTS_QID,
						CAPTS_TYPE,
						CAPTS_NUM,
						CAPTS_TEXT),
					FROM, tables.CAPTIONS,
					WHERE + CAPTS_QID + IN, qIdSelector.getSql(),
					AND + CAPTS_QID + IN, parensQMarks(captionQIds),
					ORDER_BY, joinAll(
						CAPTS_QID,
						CAPTS_TYPE,
						CAPTS_NUM));
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, qIdSelector);
			sql_vals.addValues(captionQIds);
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				debugPrint(SELF + "no captions, etc. found.");
				return;
			} // if no next
			while (!rs.isAfterLast()) {
				final int qId = rs.getInt(CAPTS_QID);
				while (!rs.isAfterLast()
						&& rs.getInt(CAPTS_QID) == qId) {
					final char captionType = 
							rs.getString(CAPTS_TYPE).charAt(0);
					final List<String> capsOrLabels = 
							new ArrayList<String>();
					while (!rs.isAfterLast()
							&& rs.getInt(CAPTS_QID) == qId
							&& rs.getString(CAPTS_TYPE).charAt(0) 
								== captionType) {
						final String capOrLabel = rs.getString(CAPTS_TEXT);
						capsOrLabels.add(capOrLabel == null ? "" : capOrLabel);
						rs.next();
					} // while same row/column and question
					debugPrint(SELF + "setting ", captionType == ROW_DBVALUE
								? "row captions" : captionType == COL_DBVALUE
								? "column captions" : captionType == LABEL_DBVALUE
								? "labels" : "y-axis values", " of Q",
							qId, " to ", capsOrLabels);
					final Question question = getQFromMap(qId);
					final int qdNum = (question.isTable() 
								&& captionType == COL_DBVALUE
							? COL_DATA : ROW_DATA) + 1;
					try {
						if (captionType == Y_AXIS_DBVALUE) {
							final EDiagramQDatum qDatum = (EDiagramQDatum) 
									question.getQDatum(GENERAL, qdNum);
							final String[] yAxisParamsArr = 
									capsOrLabels.toArray(
										new String[capsOrLabels.size()]);
							qDatum.setYAxisScale(yAxisParamsArr);
						} else if (captionType == LABEL_DBVALUE) {
							final EDiagramQDatum qDatum = (EDiagramQDatum) 
									question.getQDatum(GENERAL, qdNum);
							qDatum.labels = capsOrLabels.toArray(
									new String[capsOrLabels.size()]);
						} else { // captions for table or OED Qs
							final CaptionsQDatum qDatum = (CaptionsQDatum) 
									question.getQDatum(GENERAL, qdNum);
							if (question.isOED()) {
								for (int cNum = capsOrLabels.size();
										cNum < 3; cNum++) {
									capsOrLabels.add("");
								} // for each label
							} // if OED
							qDatum.captions = capsOrLabels.toArray(
									new String[capsOrLabels.size()]);
						} // if captionType
					} catch (ParameterException e1) {
						alwaysPrint(SELF + "can't get question datum ",
								qdNum, " of question with ID ", qId, 
								", so can't set captions/labels");
					} // try
				} // while same question
			} // while more data
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
	} // setCaptions(List<Integer>)

} // QuestionRead
