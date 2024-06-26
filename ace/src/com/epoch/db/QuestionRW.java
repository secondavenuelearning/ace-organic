package com.epoch.db;

import static com.epoch.db.dbConstants.HWRWConstants.*;
import static com.epoch.db.dbConstants.QuestionsRWConstants.*;
import com.epoch.constants.AuthorConstants;
import com.epoch.exceptions.DBException;
import com.epoch.qBank.Figure;
import com.epoch.qBank.Question;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Contains all static methods pertaining to reading and writing questions.  */
final public class QuestionRW extends DBCommon 
		implements AuthorConstants, QuestionConstants {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

/* *********** Methods to write a question ****************/

	/** Write the question to the master table, falling back under failure.
	 * @param	question	question to be written
	 * @return	question that was written
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static Question setQuestion(Question question) throws DBException {
		return setQuestion(question, MASTER_AUTHOR);
	} // setQuestion(Question)

	/** Write the question to the master table or a user's private 
	 * table, falling back under failure.
	 * @param	question	question to be written
	 * @param	authorId	login ID of the question author (null if master
	 * author)
	 * @return	question that was written
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static Question setQuestion(Question question, String authorId) 
			throws DBException {
		final QuestionWrite qWriter = new QuestionWrite(question, authorId);
		return qWriter.setQuestion();
	} // setQuestion(Question, String)

	/** Add the question to the master table, falling back under failure.
	 * @param	question	question to be written
	 * @param	qSetId	set into which the question will be written
	 * @return	question that was written
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static Question addQuestion(Question question, int qSetId) 
			throws DBException {
		return addQuestion(question, qSetId, MASTER_AUTHOR, !PRESERVE_ID);
	} // addQuestion(Question, int)

	/** Add the question to the master table or a user's private 
	 * table, falling back under failure.
	 * @param	question	question to be written
	 * @param	qSetId	set into which the question will be written
	 * @param	authorId	login ID of the question author (null if master
	 * author)
	 * @param	preserveId	whether to preserve the ID number of the question
	 * being added
	 * @return	question that was written
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static Question addQuestion(Question question, int qSetId, 
			String authorId, boolean preserveId) throws DBException {
		final QuestionWrite qWriter = 
				new QuestionWrite(question, qSetId, authorId);
		return qWriter.addQuestion(preserveId);
	} // addQuestion(Question, int, String, boolean)

/* *************** Methods to get a single question *****************/

	/** Gets a light load (flags and qData only) of a single question.  
	 * Called by ResponseLogger.
	 * @param	qId	unique ID of the question
	 * @param	instructorId	instructor's login ID
	 * @return	a question, or null if it can't be found
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Question getLightQuestion(int qId, String instructorId) 
			throws DBException {
		final QuestionRead qReader = 
				new QuestionRead(instructorId, !ADD_HEADER, !FULL_LOAD);
		Question question = qReader.getQuestion(qId);
		if (question == null) {
			qReader.setToMaster();
			question = qReader.getQuestion(qId);
		} // if not found in local table
		return question;
	} // getLightQuestion(int, String)

	/** Gets a single question.  Called by RegradeSession.
	 * @param	qId	unique ID of the question
	 * @param	instructorId	instructor's login ID
	 * @return	a question, or null if it can't be found
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Question getQuestion(int qId, String instructorId) 
			throws DBException {
		final QuestionRead qReader = 
				new QuestionRead(instructorId, !ADD_HEADER);
		Question question = qReader.getQuestion(qId);
		if (question == null) {
			qReader.setToMaster();
			question = qReader.getQuestion(qId);
		} // if not found in local table
		return question;
	} // getQuestion(int, String)

	/** Gets a single question.  Called by QSet.
	 * @param	qId	unique ID of the question
	 * @param	addHeader	whether to add the common question statement to this
	 * question's statement
	 * @return	a question, or null if it can't be found
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Question getQuestion(int qId, boolean addHeader)
			throws DBException {
		final QuestionRead qReader = new QuestionRead(addHeader);
		return qReader.getQuestion(qId);
	} // getQuestion(int, boolean)

	/** Gets a single question.  Called by HWCreateSession.
	 * @param	qId	unique ID of the question
	 * @param	instructorId	instructor's login ID
	 * @param	addHeader	whether to add the common question statement to this
	 * question's statement
	 * @return	a question, or null if it can't be found
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Question getQuestion(int qId, String instructorId,
			boolean addHeader) throws DBException {
		final QuestionRead qReader = 
				new QuestionRead(instructorId, addHeader);
		Question question = qReader.getQuestion(qId);
		if (question == null) {
			qReader.setToMaster();
			question = qReader.getQuestion(qId);
		} // if not found in local table
		return question;
	} // getQuestion(int, String, boolean)

	/** Gets a single question.  Called by QSet.
	 * @param	qId	unique ID of the question
	 * @param	instructorId	instructor's login ID
	 * @param	addHeader	whether to add the common question statement to this
	 * question's statement
	 * @param	fullLoad	full or light (flags, question data only)
	 * @return	a question, or null if it can't be found
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Question getQuestion(int qId, String instructorId,
			boolean addHeader, boolean fullLoad) throws DBException {
		final QuestionRead qReader = 
				new QuestionRead(instructorId, addHeader, fullLoad);
		return qReader.getQuestion(qId);
	} // getQuestion(int, String, boolean, boolean)

	/** Gets a single question.  Called by HWSession constructor.
	 * @param	qId	unique ID of the question
	 * @param	instructorId	instructor's login ID
	 * @param	addHeader	whether to add the common question statement to this
	 * question's statement
	 * @param	fullLoad	full or light (flags, question data only)
	 * @param	userLangs	languages of this user in order of preference; null
	 * if English only
	 * @return	a question, or null if it can't be found
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Question getQuestion(int qId, String instructorId,
			boolean addHeader, boolean fullLoad, String[] userLangs)
			throws DBException {
		final QuestionRead qReader = new QuestionRead(instructorId, 
				addHeader, fullLoad, userLangs);
		return qReader.getQuestion(qId);
	} // getQuestion(int, String, boolean, boolean, String[])

/* *************** Methods to get a list of questions *****************/

	/** Gets the master-authored questions in a question set sorted by 
	 * serial number first and qId second.  
	 * Called by QSet.
	 * @param	qSetId	unique ID of the question set
	 * @return	questions sorted by unique ID
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<Question> getQuestions(int qSetId) 
			throws DBException {
		return getQuestions(qSetId, MASTER_AUTHOR);
	} // getQuestions(int)

	/** Gets the locally or master-authored questions in a question set 
	 * sorted by master/local first, serial number second, and
	 * qId third.  Locally authored Qs have negative qId, so they need to 
	 * be sorted greater to lesser.
	 * Called by QSet and above.
	 * @param	qSetId	unique ID of the question set
	 * @param	instructorId	instructor's login ID
	 * @return	questions sorted by unique ID
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<Question> getQuestions(int qSetId, 
			String instructorId) throws DBException {
		final String SELF = "QuestionRW.getQuestions: ";
		final QuestionRead qReader = 
				new QuestionRead(instructorId, !ADD_HEADER);
		final SQLWithQMarks ifLocalSql_Vals = 
				getIfLocalSQLBld(qReader, instructorId);
		final String selector = toString(
				parensBuild(
					SELECT + Q_QID + FROM, qReader.tables().QUESTIONS,
					WHERE + Q_QSET + EQUALS + QMARK,
					ifLocalSql_Vals.getSql()),
				ifLocalSql_Vals.getSql());
		final SQLWithQMarks sql_vals = new SQLWithQMarks(selector, 
				qSetId);
		sql_vals.addValuesFrom(ifLocalSql_Vals);
		sql_vals.addValuesFrom(ifLocalSql_Vals);
		debugPrint(SELF, sql_vals);
		qReader.setQIdSelector(sql_vals);
		final String orderBy = getOrderSql(!BY_REMARKS, qReader.isLocal());
		return qReader.getQuestions(orderBy);
	} // getQuestions(int, String)

	/** Gets the master-authored questions in a book chapter sorted by 
	 * remarks first, serial number second, and qId third.  
	 * Called by BookSet.
	 * @param	book	textbook author
	 * @param	chapter	chapter number
	 * @return	questions sorted by remarks (numbers in textbook)
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<Question> getQuestions(String book, String chapter) 
			throws DBException {
		return getQuestions(book, chapter, MASTER_AUTHOR);
	} // getQuestions(String, String)

	/** Gets the locally or master-authored questions in a book chapter 
	 * sorted by remarks first, serial number second, and qId third.  
	 * Called by BookSet and above.
	 * @param	book	textbook author
	 * @param	chapter	chapter number
	 * @param	instructorId	instructor's login ID
	 * @return	questions sorted by remarks (numbers in textbook)
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<Question> getQuestions(String book, String chapter, 
			String instructorId) throws DBException {
		final String SELF = "QuestionRW.getQuestions: ";
		final QuestionRead qReader = new QuestionRead(instructorId);
		final SQLWithQMarks ifLocalSql_Vals = 
				getIfLocalSQLBld(qReader, instructorId);
		final String selector = toString(
				parensBuild(
					SELECT + Q_QID + FROM, qReader.tables().QUESTIONS,
					WHERE + Q_BOOK + EQUALS + QMARK
					+ AND + Q_BOOKCHAP + EQUALS + QMARK,
					ifLocalSql_Vals.getSql()),
				ifLocalSql_Vals.getSql());
		final SQLWithQMarks sql_vals = new SQLWithQMarks(selector, 
				book, 
				chapter);
		sql_vals.addValuesFrom(ifLocalSql_Vals);
		sql_vals.addValuesFrom(ifLocalSql_Vals);
		debugPrint(SELF, sql_vals);
		qReader.setQIdSelector(sql_vals);
		final String orderBy = getOrderSql(BY_REMARKS, qReader.isLocal());
		return qReader.getQuestions(orderBy);
	} // getQuestions(String, String, String)

	/** Gets the master-authored questions that have the keyword(s).  
	 * Questions sorted by remarks first, serial number second, and qId third.  
	 * Called by KeywordSet.
	 * @param	keywords	the keyword(s)
	 * @return	questions containing the keyword(s)
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<Question> getQuestionsByKeywords(String keywords) 
			throws DBException {
		return getQuestionsByKeywords(keywords, MASTER_AUTHOR);
	} // getQuestionsByKeywords(String)

	/** Gets the locally or master-authored questions that have the keyword(s).  
	 * Questions sorted by remarks first, serial number second, and qId third.  
	 * Called by KeywordSet and above.
	 * @param	keywords	the keyword(s)
	 * @param	instructorId	instructor's login ID
	 * @return	questions containing the keyword(s)
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<Question> getQuestionsByKeywords(String keywords, 
			String instructorId) throws DBException {
		final String SELF = "QuestionRW.getQuestionsByKeywords: ";
		final QuestionRead qReader = new QuestionRead(instructorId);
		final SQLWithQMarks ifLocalSql_Vals = 
				getIfLocalSQLBld(qReader, instructorId);
		final StringBuilder selectorBld = getBuilder(
				SELECT + Q_QID + FROM, qReader.tables().QUESTIONS, WHERE);
		final Pattern BRACKETS_CONJXNS = 
				Pattern.compile("(\\[|\\]| and | or )");
		final Matcher matcher = BRACKETS_CONJXNS.matcher(keywords);
		debugPrint(SELF + "keywords = '", keywords, "'");
		int prevEnd = 0;
		final SQLWithQMarks sql_vals = new SQLWithQMarks();
		while (true) {
			if (!matcher.find()) break;
			final int start = matcher.start();
			final int end = matcher.end();
			final String prevPart = keywords.substring(prevEnd, start);
			final String foundPart = keywords.substring(start, end);
			debugPrint(SELF + "prevPart = ", prevPart, 
					", foundPart = ", foundPart);
			if (!Utils.isEmptyOrWhitespace(prevPart)) {
				selectorBld.append(clobContainsQMark(Q_KEYWORDS));
				sql_vals.addValue(sqlIgnoreCase(prevPart.trim()));
			} // if previous part is not empty
			final char firstChar = foundPart.charAt(0);
			if (firstChar == ' ') {
				selectorBld.append(foundPart);
				sql_vals.addValue(foundPart);
			} else {
				selectorBld.append(firstChar == '[' ? '(' : ')');
			} // if firstChar
			prevEnd = end;
		} // while true
		final String last = keywords.substring(prevEnd);
		debugPrint(SELF + "last = ", last); 
		if (!Utils.isEmptyOrWhitespace(last)) {
			selectorBld.append(clobContainsQMark(Q_KEYWORDS));
			sql_vals.addValue(sqlIgnoreCase(last.trim()));
		} // if there is a last keyword
		parens(selectorBld.append(ifLocalSql_Vals.getSql()))
				.append(ifLocalSql_Vals.getSql());
		sql_vals.setSql(selectorBld);
		sql_vals.addValuesFrom(ifLocalSql_Vals);
		sql_vals.addValuesFrom(ifLocalSql_Vals);
		debugPrint(SELF, sql_vals);
		qReader.setQIdSelector(sql_vals);
		final String orderBy = getOrderSql(BY_REMARKS, qReader.isLocal());
		debugPrint(SELF + "orderBy = ", orderBy);
		return qReader.getQuestions(orderBy);
	} // getQuestionsByKeywords(String, String)

	/** Gets SQL and value to find questions authored by a particular instructor.
	 * @param	qReader	the question-reading object
	 * @param	instructorId	instructor's login ID
	 * @return	SQL and value to find questions authored by this instructor
	 */
	private static SQLWithQMarks getIfLocalSQLBld(QuestionRead qReader, 
			String instructorId) {
		final SQLWithQMarks sql_vals = new SQLWithQMarks();
		if (qReader.isLocal()) {
			sql_vals.addToSql(AND, qReader.Q_AUTHOR(), EQUALS + QMARK);
			sql_vals.addValue(instructorId);
		} // if local author
		return sql_vals;
	} // getIfLocalSQLBld(QuestionRead, String)

	/** Gets an SQL fragment describing how to order retrieved questions.
	 * @param	byRemarks	whether to order by remarks
	 * @param	isLocal	whether the questions being ordered are locally authored
	 * @return	String containing the SQL fragment
	 */
	private static String getOrderSql(boolean byRemarks, boolean isLocal) {
		final StringBuilder orderBld = getBuilder(ORDER_BY);
		if (byRemarks) {
			orderBld.append(prejoin(clobToUpper(Q_REMARKS)));
		} // if ordering by remarks
		return toString(orderBld, joinAll(
				Q_NUM, 
				isLocal ? toString('-', Q_QID) : Q_QID));
	} // getOrderSql(boolean, boolean)

	/** Converts a string into a regular expression that is case- and
	 * whitespace-agnostic.
	 * @param	str	a string
	 * @return	the string with every letter X or x changed to [Xx]
	 * and with contiguous whitespace characters changed to \\s+.
	 */
	private static String sqlIgnoreCase(String str) {
		if (str == null) return "";
		final StringBuilder bld = new StringBuilder();
		final char[] chs = str.toCharArray();
		final int[] UPPERCASE = new int[] {(int) 'A', (int) 'Z'};
		final int[] LOWERCASE = new int[] {(int) 'a', (int) 'z'};
		for (final char ch : chs) {
			final int chAsInt = (int) ch;
			if (MathUtils.inRange(chAsInt, UPPERCASE)) {
 				appendTo(bld, '[', ch, (char) (chAsInt + 32), ']');
			} else if (MathUtils.inRange(chAsInt, LOWERCASE)) {
 				appendTo(bld, '[', (char) (chAsInt - 32), ch, ']');
			} else bld.append(ch);
		} // for each character
		return bld.toString().replaceAll("\\s+", "\\\\s+");
	} // sqlIgnoreCase(String)

/* **************** Methods to get questions mapped by ID ********/

	/** Gets a map of questions keyed by their ID numbers.  Called by HWRead.
	 * @param	con	connection to database
	 * @param	qIdSelector	SQL containing question marks representing unique 
	 * IDs of desired questions, appended with SQL for instructor's userId if 
	 * they are local, plus values to substitute for the question marks
	 * @param	tables	tables for master- or locally authored questions
	 * @param	instructorId	instructor's login ID
	 * @param	addHeader	whether to add the common question statement to this
	 * question's statement
	 * @return	questions keyed by IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	static Map<Integer, Question> getQuestionsMap(Connection con, 
			SQLWithQMarks qIdSelector, DBTables tables, String instructorId, 
			boolean addHeader) throws DBException {
		return getQuestionsMap(con, qIdSelector, tables, 
				instructorId, FULL_LOAD, addHeader, NO_LANGUAGES);
	} // getQuestionsMap(Connection, SQLWithQMarks, DBTables, String, boolean)

	/** Gets a map of questions keyed by their ID numbers.  Called by HWRead.
	 * @param	con	connection to database
	 * @param	qIdSelector	SQL containing question marks representing unique 
	 * IDs of desired questions, appended with SQL for instructor's userId if 
	 * they are local, plus values to substitute for the question marks
	 * @param	tables	tables for master- or locally authored questions
	 * @param	instructorId	instructor's login ID
	 * @param	fullLoad	full or light (flags, question data only)
	 * @param	addHeader	whether to add common question statements to
	 * these questions' statements
	 * @return	questions keyed by IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	static Map<Integer, Question> getQuestionsMap(Connection con, 
			SQLWithQMarks qIdSelector, DBTables tables, String instructorId, 
			boolean fullLoad, boolean addHeader) throws DBException {
		return getQuestionsMap(con, qIdSelector, tables, 
				instructorId, fullLoad, addHeader, NO_LANGUAGES);
	} // getQuestionsMap(Connection, SQLWithQMarks, DBTables, String, boolean, 
	//					boolean)

	/** Gets a map of questions keyed by their ID numbers.  Called by HWRead
	 * and above.
	 * @param	con	connection to database
	 * @param	qIdSelector	SQL containing question marks representing unique 
	 * IDs of desired questions, appended with SQL for instructor's userId if 
	 * they are local, plus values to substitute for the question marks
	 * @param	tables	tables for master- or locally authored questions
	 * @param	instructorId	instructor's login ID
	 * @param	fullLoad	full or light (flags, question data only)
	 * @param	addHeader	whether to add common question statements to
	 * these questions' statements
	 * @param	userLangs	languages of this user in order of preference; null
	 * if English only
	 * @return	questions keyed by IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	static Map<Integer, Question> getQuestionsMap(Connection con, 
			SQLWithQMarks qIdSelector, DBTables tables, String instructorId, 
			boolean fullLoad, boolean addHeader, String[] userLangs) 
			throws DBException {
		final QuestionRead qReader = new QuestionRead(instructorId, 
				addHeader, fullLoad, userLangs, tables);
		qReader.setQIdSelector(qIdSelector);
		// can discard list of Qs because we only want the map of Q by ID
		qReader.getQuestions(con, ANY_ORDER);
		return qReader.getQMap();
	} // getQuestionsMap(Connection, SQLWithQMarks, DBTables, String, 
	//						boolean, boolean, String[])

/* **************** Methods for getting other question information ********/

	/** Gets the login ID of an author of a question.
	 * @param	qId	unique ID of the question
	 * @return	login ID of an author of a question, or
	 * null if the question is master-authored
	 */
	public static String getAuthorIdByQId(int qId) {
		final String SELF = "QuestionRW.getAuthorIdByQId: ";
		String authorId = null; // default
		if (qId > 0) return authorId;
		final DBLocalTables localTables = new DBLocalTables();
		final String qry = toString(
				SELECT, DBLocalTables.Q_AUTHOR,
				FROM, localTables.QUESTIONS,
				WHERE + Q_QID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				qId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) authorId = rs.getString(DBLocalTables.Q_AUTHOR);
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
		} finally {
			closeConnection(con, stmt, rs);
		}
		return authorId;
	} // getAuthorIdByQId(int)

	/** Gets whether each question in a set has been made part of a random group 
	 * in an assignment.
	 * @param	qIds	unique IDs of the question
	 * @param	authorId	question author's login ID
	 * @return	array of booleans; each is true if the question has been made part 
	 * of a random group in an assignment
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static boolean[] areAssignedAsRandom(List<Integer> qIds, String authorId) 
			throws DBException {
		final String SELF = "QuestionRW.areAssignedAsRandom: ";
		if (Utils.isEmpty(qIds)) return new boolean[0];
		// a question is assigned as random if it is part of a question
		// group where the number of questions in the group != 
		// pick * bundle_size.
		// Here's the query for a local author:
		// select pb_id from hwset_qs_v1, hwsets_v5 where pb_id in [qIds] 
		// and (hwset_qs_v1.hw_id, group_num) in (select hw_id, group_num from 
		// (select hw_id, group_num, count(pb_num_in_group) as srch_result, 
		// max(group_pick) as group_pick, max(group_bundle_size) 
		// as group_bundle_size from hwset_qs_v1 group by hw_id, group_num) 
		// where srch_result != group_pick * group_bundle_size) 
		// and hwsets_v5.hw_id = hwset_qs_v1.hw_id and course_id in 
		// (select id from cw_courses_v3 where instructor_id = [username]);
		// Simpler for a master author:
		// select pb_id from hwset_qs_v1 where pb_id in [qIds] and 
		// (hw_id, group_num) in (select hw_id, group_num from (select hw_id,
		// group_num, count(pb_num_in_group) as srch_result, max(group_pick) 
		// as group_pick, max(group_bundle_size) as group_bundle_size from 
		// hwset_qs_v1 group by hw_id, group_num) where srch_result != 
		// group_pick * group_bundle_size);
		final StringBuilder grpIsRandBld = parensBuild(
				SELECT, joinAll(
					HWQS_HWID,
					HWQS_GRP_NUM),
				FROM, parensBuild(
					SELECT, joinAll(
						HWQS_HWID,
						HWQS_GRP_NUM,
						toString(count(HWQS_QNUM), AS + SRCH_RESULT),
						toString(max(HWQS_GRP_PICK), AS + HWQS_GRP_PICK),
						toString(max(HWQS_BUNDLE_SIZE), AS + HWQS_BUNDLE_SIZE)),
					FROM + HW_QS + GROUP_BY, joinAll(
						HWQS_HWID,
						HWQS_GRP_NUM)),
				WHERE + SRCH_RESULT + NOT_EQUALS
					+ HWQS_GRP_PICK + TIMES + HWQS_BUNDLE_SIZE);
		final String qry = SELECT + HWQS_QID + FROM;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				qIds);
		if (authorId == MASTER_AUTHOR) {
			sql_vals.addToSql(HW_QS
					+ WHERE + HWQS_QID + IN, parensQMarks(qIds),
					AND, parensJoin( 
						HWQS_HWID,
						HWQS_GRP_NUM), 
					IN, grpIsRandBld);
		} else {
			sql_vals.addToSql(joinAll(
						HW_QS,
						HWSETS),
					WHERE + HWQS_QID + IN, parensQMarks(qIds),
					AND, parensJoin( 
						HW_QS + DOT + HWQS_HWID,
						HWQS_GRP_NUM),
					IN, grpIsRandBld,
					AND + HW_QS + DOT + HWQS_HWID
						+ EQUALS + HWSETS + DOT + HW_ID,
					AND + HW_COURSE + IN, parens(
						CourseRW.getCourseIdByCourseInstructor())); // one QMARK
			sql_vals.addValue(authorId);
		} // if master author
		debugPrint(SELF, sql_vals);
		final boolean[] areAssignedAsRandom = new boolean[qIds.size()];
		Arrays.fill(areAssignedAsRandom, false);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final int asRandomQId = rs.getInt(HWQS_QID);
				final int posn = qIds.indexOf(Integer.valueOf(asRandomQId));
				areAssignedAsRandom[posn] = true;
			} // while there are more Qs in random groups
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
			throw new DBException(SELF + "Couldn't get whether Q "
					+ qIds.toString() + (authorId != null 
						? " by " + authorId : "")
					+ " have been assigned as randomized questions.");
		} finally {
			closeConnection(con, stmt, rs);
		}
		debugPrint(SELF + "for Q ", qIds, (authorId == null ? "" 
				: " and author " + authorId), ", returning ", 
				areAssignedAsRandom);
		return areAssignedAsRandom;
	} // areAssignedAsRandom(List<Integer>, String)

	/** Gets the type of a question.
	 * @param	qId	unique ID of the question
	 * @param	authorId	question author's login ID
	 * @return	the question type, or 0 if not found
	 */
	public static int getQuestionType(int qId, String authorId) {
		final String SELF = "QuestionRW.getQuestionType: ";
		int qType = 0;
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			if (authorId != MASTER_AUTHOR) {
				final DBLocalTables localTables = new DBLocalTables();
				final String qry = toString(
						SELECT + Q_TYPE
						+ FROM, localTables.QUESTIONS,
						WHERE + Q_QID + EQUALS + QMARK
						+ AND, DBLocalTables.Q_AUTHOR, EQUALS + QMARK);
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
						qId,
						authorId);
				debugPrint(SELF, sql_vals);
				stmt = getStatement(con, sql_vals);
				rs = stmt.executeQuery();
				if (rs.next()) {
					qType = Utils.indexOf(DB_QTYPES, rs.getString(Q_TYPE));
				} // if there's a result
			} // if there's an author
			if (qType < 0) {
				closeStmtAndRs(stmt, rs);
				// no locally authored version of question; try master table
				final DBTables masterTables = new DBTables();
				final String qry = toString(
						SELECT + Q_TYPE
						+ FROM, masterTables.QUESTIONS,
						WHERE + Q_QID + EQUALS + QMARK);
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
						qId);
				debugPrint(SELF, sql_vals);
				stmt = getStatement(con, sql_vals);
				rs = stmt.executeQuery();
				if (rs.next()) {
					qType = Utils.indexOf(DB_QTYPES, rs.getString(Q_TYPE));
				} // if there's a result
			} // if no qType have been obtained
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		return (qType < 0 ? OTHER : qType);
	} // getQuestionType(int, String)

	/** Gets the flags of a question.
	 * @param	qId	unique ID of the question
	 * @param	authorId	question author's login ID
	 * @return	the question flags, or 0 if not found
	 */
	public static long getQuestionFlags(int qId, String authorId) {
		return getQuestionFlags(new int[] {qId}, authorId)[0];
	} // getQuestionFlags(int, String)

	/** Gets the flags of a group of questions.
	 * @param	qIds	unique IDs of the questions
	 * @param	authorId	question author's login ID
	 * @return	array of question flags
	 */
	public static long[] getQuestionFlags(int[] qIds, String authorId) {
		final String SELF = "QuestionRW.getQuestionFlags: ";
		final long[] qFlags = new long[qIds.length];
		Arrays.fill(qFlags, -1);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		if (!Utils.isEmpty(qIds)) try {
			con = getPoolConnection();
			if (authorId != MASTER_AUTHOR) {
				final DBLocalTables localTables = new DBLocalTables();
				final String qry = toString(
						SELECT, joinAll(
							Q_QID,
							Q_FLAGS),
						FROM, localTables.QUESTIONS,
						WHERE + Q_QID + IN, parensQMarks(qIds),
						AND, DBLocalTables.Q_AUTHOR, EQUALS + QMARK);
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
						qIds,
						authorId);
				debugPrint(SELF, sql_vals);
				stmt = getStatement(con, sql_vals);
				rs = stmt.executeQuery();
				while (rs.next()) {
					final int qId = rs.getInt(Q_QID);
					final int qPosn = Utils.indexOf(qIds, qId);
					qFlags[qPosn] = rs.getLong(Q_FLAGS);
				} // while there are results
			} // if there's an author
			closeStmtAndRs(stmt, rs);
			final DBTables masterTables = new DBTables();
			final String qry = toString(
					SELECT, joinAll(
						Q_QID,
						Q_FLAGS),
					FROM, masterTables.QUESTIONS,
					WHERE + Q_QID + IN, parensQMarks(qIds));
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
					qIds);
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final int qId = rs.getInt(Q_QID);
				final int qPosn = Utils.indexOf(qIds, qId);
				if (qFlags[qPosn] == -1) {
					qFlags[qPosn] = rs.getLong(Q_FLAGS);
				} // if already got flags for this question
			} // if there's a result
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		debugPrint(SELF + "qIds = ", qIds, ", qFlags = ", qFlags);
		return qFlags;
	} // getQuestionFlags(int[], String)

	/** Gets all ID numbers of master-authored questions in a question set.
	 * @param	qSetId	ID number of the question set
	 * @return	colon-separated string of ID numbers of all master-authored
	 * questions in the question set; null if query fails
	 */
	public static String getAllQuestionIds(int qSetId) {
		final String SELF = "QuestionRW.getAllQuestionIds: ";
		StringBuilder allQIds = new StringBuilder();
		final DBTables masterTables = new DBTables();
		final String qry = toString(
				SELECT + Q_QID 
				+ FROM, masterTables.QUESTIONS,
				WHERE + Q_QSET + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				qSetId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			boolean first = true;
			while (rs.next()) {
				if (!first) allQIds.append(':');
				else first = false;
				allQIds.append(rs.getString(Q_QID));
			} // while there are results
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
			allQIds = null;
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		debugPrint(SELF + "returning ", allQIds);
		return (allQIds == null ? null : allQIds.toString());
	} // getAllQuestionIds(int)

	/** Gets an inventory of the questions in the database.
	 * @return	2D array of question information
	 */
	public static String[][] getInventory() {
		final String SELF = "QuestionRW.getInventory: ";
		final DBTables masterTables = new DBTables();
		final String SRCH_RESULT1 = SRCH_RESULT + "1";
		final String SRCH_RESULT2 = SRCH_RESULT + "2";
		final String qry = toString(
				SELECT, joinAll( 
					TOPICS + DOT + TOPIC_NAME + AS + SRCH_RESULT1,
					QSETS + DOT + QSET_NAME + AS + SRCH_RESULT2,
					toString(masterTables.QUESTIONS, DOT + Q_QID),
					toString(masterTables.QUESTIONS, DOT + Q_BOOK),
					toString(masterTables.QUESTIONS, DOT + Q_BOOKCHAP),
					toString(masterTables.QUESTIONS, DOT + Q_REMARKS),
					toString(masterTables.QUESTIONS, DOT + Q_TYPE),
					toString(masterTables.QUESTIONS, DOT + Q_FLAGS),
					toString(masterTables.QUESTIONS, DOT + Q_CREATED), 
					toString(masterTables.QUESTIONS, DOT + Q_LAST_MODIFIED), 
					toString(masterTables.QUESTIONS, DOT + Q_KEYWORDS)),
				FROM, joinAll(
					masterTables.QUESTIONS,
					QSETS,
					TOPICS),
				WHERE, masterTables.QUESTIONS, DOT + Q_QSET 
						+ EQUALS + QSETS + DOT + QSET_ID
					+ AND + QSETS + DOT + QSET_TOPIC_ID 
						+ EQUALS + TOPICS + DOT + TOPIC_ID,
				ORDER_BY, joinAll(
					SRCH_RESULT1,
					SRCH_RESULT2,
					Q_QID));
		alwaysPrint(SELF, qry);
		final List<String[]> inventory = new ArrayList<String[]>();
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs.next()) {
				final long qFlags = rs.getLong(Q_FLAGS);
				inventory.add(new String[] {
						rs.getString(SRCH_RESULT1),
						rs.getString(SRCH_RESULT2),
						String.valueOf(rs.getInt(Q_QID)),
						rs.getString(Q_BOOK),
						rs.getString(Q_BOOKCHAP),
						rs.getString(Q_REMARKS),
						Question.getQTypeDescription(
							Utils.indexOf(DB_QTYPES, rs.getString(Q_TYPE)),
							qFlags),
						String.valueOf(qFlags),
						rs.getString(Q_CREATED),
						rs.getString(Q_LAST_MODIFIED),
						rs.getString(Q_KEYWORDS)
						});
			} // while there are results
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
			inventory.clear();
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		inventory.add(0, new String[] {
				"Topic",
				"Question set",
				"Question ID",
				"Text",
				"Chapter",
				"Remarks",
				"Question type",
				"Created",
				"Last modified",
				"Keywords"
				});
		return inventory.toArray(
				new String[inventory.size()][inventory.get(0).length]);
	} // getInventory()

/* **************** Methods for deleting questions ********/

	/** Delete a question from the master table.  ACE allows users to delete
	 * questions only when they are NOT part of a random group of questions in
	 * an assignment.
	 * @param	qId	unique ID of question to be deleted
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void deleteQuestion(int qId) throws DBException {
		deleteQuestion(qId, MASTER_AUTHOR);
	} // deleteQuestion(int)

	/** Delete a question from the master table or a user's private table.
	 * ACE allows users to delete questions only when they are NOT part of a
	 * random group of questions in an assignment.
	 * @param	qId	unique ID of question to be deleted
	 * @param	authorId	login ID of the question author (null if master)
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void deleteQuestion(int qId, String authorId)
			throws DBException {
		final QuestionWrite qWriter = new QuestionWrite(authorId);
		qWriter.deleteQuestion(qId);
	} // deleteQuestion(int, String)

	/** Delete all of a user's locally authored questions.  Called by UserWrite.
	 * We don't need to delete responses to these questions here because 
	 * all of this user's courses and assignments will be deleted as well.
	 * @param	con	connection to database
	 * @param	authorIds	author IDs
	 * @throws	DBException	if there's a problem writing to the database
	 */
	static void deleteQuestions(Connection con, String[] authorIds) 
			throws DBException {
		final String SELF = "QuestionRW.deleteQuestions: ";
		debugPrint(SELF + " ****** About to delete Qs locally authored by ", 
				authorIds);
		final DBLocalTables localTables = new DBLocalTables();
		final StringBuilder authorIdsQMarks = parensQMarks(authorIds);
		final String qry = toString(
				DELETE_FROM, localTables.IMAGES,
				WHERE + IMG_ID + IN, parensBuild(
					SELECT + FIG_FIGID 
					+ FROM, localTables.FIGURES,
					WHERE + FIG_TYPE + EQUALS, 
						quotes(Figure.DBVALUES[Figure.IMAGE]),
					AND, DBLocalTables.Q_AUTHOR, IN, authorIdsQMarks));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
		sql_vals.setValuesArray(authorIds);
		debugPrint(SELF, sql_vals);
		try {
			// delete images for this question
			tryUpdate(con, sql_vals);
			// delete figures, question data, evaluators, questions
			final String[] tables = new String[] 
					{localTables.FIGURES, 
					localTables.QUESTIONDATA,
					localTables.EVALUATORS, 
					localTables.QUESTIONS};
			for (final String table : tables) {
				sql_vals.setSql(
						DELETE_FROM, table,
						WHERE, DBLocalTables.Q_AUTHOR, 
							IN, authorIdsQMarks); // values don't change
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} // for each table
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		}
		debugPrint(SELF + " ****** Deleted Qs locally authored by ", 
				authorIds);
	} // deleteQuestions(Connection, List<String>)

/* **************** Methods for writing other question information ********/

	/** Sets serial numbers of questions in a set.  Called by QSet.
	 * @param	qIds	unique ID numbers of questions in order of their serial
	 * numbers
	 * @param	authorId	login ID of the question author (null if master)
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setQSerialNos(int[] qIds, String authorId) 
			throws DBException {
		final QuestionWrite qWriter = new QuestionWrite(authorId);
		qWriter.setQSerialNos(qIds);
	} // setQSerialNos(int[], String)

	/** Moves a question to a different question set.  Called from QSet.
	 * Local instructor can evoke this method only for Qs that that instructor
	 * has written.
	 * @param	qIds	unique ID numbers of questions to be moved
	 * @param	qSetId	unique ID of question set to which to move questions
	 * @param	qIsLocal	whether questions are locally authored
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void moveQtoNewQset(int[] qIds, int qSetId, boolean qIsLocal) 
			throws DBException {
		final String SELF = "QuestionRW.moveQtoNewQset: ";
		Connection con = null;
		if (!Utils.isEmpty(qIds)) try {
			debugPrint(SELF + "About to move ", qIsLocal ? "locally authored Qs"
						: "master Qs and their locally modified versions",
					" with unique ids ", qIds,
					" to qSet with unique id ", qSetId);
			final StringBuilder setBld = getBuilder(
					SET, equalsJoin(
						Q_QSET, QMARK,
						Q_NUM, '0'),
					WHERE + Q_QID + IN, parensQMarks(qIds));
			// move locally authored Qs and locally modified versions of master Qs
			final DBLocalTables localTables = new DBLocalTables();
			final String qry = toString(
					UPDATE, localTables.QUESTIONS, setBld);
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
					qSetId, 
					qIds);
			con = getPoolConnection();
			con.setAutoCommit(false);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
			if (!qIsLocal) {
				// move master Qs
				final DBTables masterTables = new DBTables();
				sql_vals.setSql(UPDATE, masterTables.QUESTIONS, setBld);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} // if Q is not local
			con.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // moveQtoNewQset(int[], int, boolean)

/* *************** One-time data conversion methods *********************/

	/** Gets all evaluator text data where inputToCERs(data) != data.
	 * @param	local	whether to edit local author table
	 * @return	Map of evaluator text data by qId, majorId, minorId
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<String, String> getConvertibleEvalText(
			boolean local) throws DBException {
		final DBTables tables = getTables(local);
		final String qry = toString(
				SELECT, joinAll(
					EVAL_QID,
					EVAL_MAJORID,
					EVAL_MINORID,
					EVAL_MOLSTRUCT),
				FROM, tables.EVALUATORS);
		debugPrint("QuestionRW.getConvertibleEvalText: ", qry);
		final Map<String, String> evalTexts = new HashMap<String, String>();
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs.next()) {
				final String qId = rs.getString(EVAL_QID);
				final String majorId = rs.getString(EVAL_MAJORID);
				final String minorId = rs.getString(EVAL_MINORID);
				final String text = rs.getString(EVAL_MOLSTRUCT);
				if (text != null && !text.equals(Utils.inputToCERs(text))) {
					evalTexts.put(toString(qId, ':', majorId, ':', minorId), 
							text);
				}
			} // if there's a result
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return evalTexts;
	} // getConvertibleEvalText(boolean)

	/** Replaces an existing evaluator text data with a new one.  Used to 
	 * convert UTF-8 text into ASCII with character entity representations.
	 * @param	key	the qID and eval IDs of the data
	 * @param	text	the new data
	 * @param	local	whether to edit local author tables
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void putEvalText(String key, String text, boolean local)
			throws DBException {
		final String SELF = "QuestionRW.putEvalText: ";
		final DBTables tables = getTables(local);
		final int[] ids = Utils.stringToIntArray(key.split(":"));
		final String qry = toString(
				UPDATE, tables.EVALUATORS,
				SET + EVAL_MOLSTRUCT + EQUALS + QMARK // CLOB 
				+ WHERE + EVAL_QID + EQUALS + QMARK
				+ AND + EVAL_MAJORID + EQUALS + QMARK
				+ AND + EVAL_MINORID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				text,
				ids[0],
				ids[1],
				ids[2]);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // putEvalText(String, String, boolean)

	/** Gets all feedback where inputToCERs(feedback) != feedback.
	 * @param	local	whether to edit local author table
	 * @return	Map of feedback by qId, majorId, minorId
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<String, String> getConvertibleFeedback(
			boolean local) throws DBException {
		final DBTables tables = getTables(local);
		final String qry = toString(
				SELECT, joinAll(
					EVAL_QID,
					EVAL_MAJORID,
					EVAL_MINORID,
					EVAL_FEEDBACK),
				FROM, tables.EVALUATORS);
		debugPrint("QuestionRW.getConvertibleFeedback: ", qry);
		final Map<String, String> feedbacks =
				new HashMap<String, String>();
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs.next()) {
				final String qId = rs.getString(EVAL_QID);
				final String majorId = rs.getString(EVAL_MAJORID);
				final String minorId = rs.getString(EVAL_MINORID);
				final String feedback = rs.getString(EVAL_FEEDBACK);
				if (feedback != null
						&& !feedback.equals(Utils.inputToCERs(feedback))) {
					feedbacks.put(toString(qId, ':', majorId, ':', minorId),
							feedback);
				}
			} // if there's a result
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return feedbacks;
	} // getConvertibleFeedback(boolean)

	/** Replaces an existing feedback with a new one.  Used to convert UTF-8
	 * text into ASCII with character entity representations.
	 * @param	key	the qID and eval IDs of the feedback
	 * @param	feedback	the new feedback
	 * @param	local	whether to edit local author table
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void putFeedback(String key, String feedback, boolean local)
			throws DBException {
		final String SELF = "QuestionRW.putFeedback: ";
		final DBTables tables = getTables(local);
		final int[] ids = Utils.stringToIntArray(key.split(":"));
		final String qry = toString(
				UPDATE, tables.EVALUATORS,
				SET + EVAL_FEEDBACK + EQUALS + QMARK // CLOB 
				+ WHERE + EVAL_QID + EQUALS + QMARK
				+ AND + EVAL_MAJORID + EQUALS + QMARK
				+ AND + EVAL_MINORID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				feedback,
				ids[0],
				ids[1],
				ids[2]);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // putFeedback(String, String, boolean)

	/** Gets all qData where inputToCERs(qData) != qData.
	 * @param	local	whether to edit local author table
	 * @return	Map of qData by unique ID
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<String, String> getConvertibleQData(boolean local) 
			throws DBException {
		final DBTables tables = getTables(local);
		final String qry = toString(
				SELECT, joinAll(
					QD_DATUMID,
					QD_DATA),
				FROM, tables.QUESTIONDATA);
		debugPrint("QuestionRW.getConvertibleQData: ", qry);
		final Map<String, String> qDatas = new HashMap<String, String>();
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs.next()) {
				final String qDataId = rs.getString(QD_DATUMID);
				final String qData = rs.getString(QD_DATA);
				if (qData != null && !qData.equals(Utils.inputToCERs(qData))) {
					qDatas.put(qDataId, qData);
				}
			} // if there's a result
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return qDatas;
	} // getConvertibleQData(boolean)

	/** Replaces an existing qDatum with a new one.  Used to convert UTF-8
	 * text into ASCII with character entity representations.
	 * @param	qDataId	the unique ID of the qData
	 * @param	qData	the new qDatum
	 * @param	local	whether to edit local author table
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void putQData(String qDataId, String qData, boolean local)
			throws DBException {
		final String SELF = "QuestionRW.putQData: ";
		final DBTables tables = getTables(local);
		final String qry = toString(
				UPDATE, tables.QUESTIONDATA,
				SET + QD_DATA + EQUALS + QMARK // CLOB
				+ WHERE + QD_DATUMID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				qData, 
				qDataId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // putQData(String, String, boolean)

	/** Gets all statements where inputToCERs(statement) != statement.
	 * @param	local	whether to edit local author table
	 * @return	Map of statement by qId
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<String, String> getConvertibleStatements(
			boolean local) throws DBException {
		final DBTables tables = getTables(local);
		final String qry = toString(
				SELECT, joinAll(
					Q_QID,
					Q_STATEMENT),
				FROM, tables.QUESTIONS);
		debugPrint("QuestionRW.getConvertibleStatements: ", qry);
		final Map<String, String> statements = new HashMap<String, String>();
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs.next()) {
				final String qId = rs.getString(Q_QID);
				final String statement = rs.getString(Q_STATEMENT);
				if (statement != null
						&& !statement.equals(Utils.inputToCERs(statement))) {
					statements.put(qId, statement);
				}
			} // if there's a result
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return statements;
	} // getConvertibleStatements(boolean)

	/** Replaces an existing statement with a new one.  Used to convert UTF-8
	 * text into ASCII with character entity representations.
	 * @param	qId	the qID of the statement
	 * @param	statement	the new statement
	 * @param	local	whether to edit local author table
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void putStatement(String qId, String statement, boolean local)
			throws DBException {
		final String SELF = "QuestionRW.putStatement: ";
		final DBTables tables = getTables(local);
		final String qry = toString(
				UPDATE, tables.QUESTIONS,
				SET + Q_STATEMENT + EQUALS + QMARK // CLOB
					+ WHERE + Q_QID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				statement,
				qId);
		Connection con = null;
		try {
			con = getPoolConnection();
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // putStatement(String, String, boolean)

	private QuestionRW() {
		// not instantiable
	}

} // QuestionRW
