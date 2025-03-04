package com.epoch.db;

import static com.epoch.db.dbConstants.CourseRWConstants.*;
import static com.epoch.db.dbConstants.QuestionsRWConstants.*;
import static com.epoch.db.dbConstants.ResponsesConstants.*;
import com.epoch.assgts.assgtConstants.AssgtConstants;
import com.epoch.evals.EvalResult;
import com.epoch.evals.evalConstants.EvalResultConstants;
import com.epoch.exceptions.DBException;
import com.epoch.session.CrossCourseReport;
import com.epoch.session.sessConstants.GradeConstants;
import com.epoch.utils.Utils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** For retrieving student responses to questions in assignments.
*/
public final class ResponseRead extends DBCommon 
		implements AssgtConstants, EvalResultConstants, GradeConstants {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Parameter for getOneHWResults(). */
	private static final boolean GET_LAST_RESP = true;
	/** Parameter for getOneHWResults(). */
	// private static final boolean MOST_RECENT_ONLY = true; // unused 11/6/2012
	/** Parameter for populateEvalResult(). */
	private static final boolean NULL_FOR_EXCEPTION = true;
	/** Parameter for getTotalGrades() and getHumanGradingRequired(). */
	private static final String ALL_STUDENTS = null;

/* *********** Methods called by a question-answering session  ************/

	/** Retrieves the last response of a student to a single question in an 
	 * assignment.  Called by HWSession.init(), HWSession.resetEvalResult(), 
	 * and GradeSet.getResult().
	 * @param	studentId	student's login ID
	 * @param	hwId	unique ID of the assignment
	 * @param	qId	unique ID of the question
	 * @return	result of evaluating the last response of a student to a single
	 * question in an assignment
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static EvalResult getResult(String studentId, int hwId, int qId) 
			throws DBException {
		final String SELF = "ResponseRead.getResult: ";
		final String qry = toString(
				SELECT, joinAll( 
					RESP_QID,
					RESP_TRIES, 
					RESP_LATEST,
					RESP_GRADE, 
					RESP_MODGRADE, 
					RESP_LASTRESPONSE,
					RESP_WHEN, 
					RESP_IP,
					RESP_COMMENT, 
					RESP_STATUS),
				FROM + RESPONSES 
				+ WHERE + RESP_LATEST + EQUALS + Y
				+ AND + RESP_HWID + EQUALS + QMARK
				+ AND + RESP_QID + EQUALS + QMARK
				+ AND + RESP_STUDENT + EQUALS + QMARK); 
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				hwId,
				qId,
				studentId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		EvalResult evalResult = null;
		try {
			con = getPoolConnection(); 
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				evalResult = populateEvalResult(rs);
				debugPrint(SELF + "got result with ", evalResult.tries,
						" tries and status ", evalResult.status);
			} // if there's an evalResult
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return evalResult;
	} // getResult(String, int, int)

	/** Retrieves the last responses of a student to all questions in an assignment.
	 * Called by HWSession().
	 * @param	studentId	student's login ID
	 * @param	hwId	unique ID of the assignment
	 * @return	last responses of a student to all questions in an assignment
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<Integer, EvalResult> getResults(String studentId, 
			int hwId) throws DBException {
		final String SELF = "ResponseRead.getResults: ";
		final String qry = toString(
				SELECT, joinAll( 
					RESP_TRIES, 
					RESP_LATEST,
					RESP_GRADE, 
					RESP_MODGRADE, 
					RESP_LASTRESPONSE,
					RESP_WHEN, 
					RESP_IP,
					RESP_STATUS, 
					RESP_COMMENT, 
					RESP_QID),
				FROM + RESPONSES 
				+ WHERE + RESP_LATEST + EQUALS + Y
				+ AND + RESP_HWID + EQUALS + QMARK
				+ AND + RESP_STUDENT + EQUALS + QMARK
				+ ORDER_BY + RESP_QID); 
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				hwId,
				studentId);
		debugPrint(SELF, sql_vals);
		final Map<Integer, EvalResult> evalResultsByQIds = 
				new HashMap<Integer, EvalResult>();
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection(); 
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final EvalResult evalResult = populateEvalResult(rs);
				if (evalResult == null) break;
				debugPrint(SELF + "for qId ", evalResult.qId, " grade = ",
						evalResult.grade, ", tries = ", evalResult.tries);
				evalResultsByQIds.put(Integer.valueOf(evalResult.qId), 
						evalResult);
			} // while there are results
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return evalResultsByQIds;
	} // getResults(String, int)

	/** Retrieves all responses of a student to a single question in an assignment
	 * (except the zeroeth response).  Called by HWSession.init() and
	 * RegradeSession.doRegrade().
	 * @param	studentId	student's login ID
	 * @param	hwId	unique ID of the assignment
	 * @param	qId	unique ID of the question
	 * @return	results of evaluations of all responses of a student to a single
	 * question in an assignment
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static EvalResult[] getResults(String studentId, int hwId, int qId) 
			throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection(); 
			return getResults(con, studentId, hwId, qId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // getResults(String, int, int)

	/** Retrieves all responses of a student to a single question in an assignment
	 * (except the zeroeth response).  Called by HWSession.init() and
	 * ResponseWrite.addResult().
	 * @param	con	database connection
	 * @param	studentId	student's login ID
	 * @param	hwId	unique ID of the assignment
	 * @param	qId	unique ID of the question
	 * @return	results of evaluations of all responses of a student to a single
	 * question in an assignment
	 * @throws	DBException	if there's a problem reading the database
	 */
	static EvalResult[] getResults(Connection con, String studentId, 
			int hwId, int qId) throws DBException {
		final String SELF = "ResponseRead.getResults: ";
		final String qry = toString(
				SELECT, joinAll( 
					RESP_QID,
					RESP_TRIES, 
					RESP_LATEST,
					RESP_GRADE, 
					RESP_MODGRADE, 
					RESP_LASTRESPONSE,
					RESP_WHEN, 
					RESP_IP,
					RESP_COMMENT, 
					RESP_STATUS),
				FROM + RESPONSES
				+ WHERE + RESP_HWID + EQUALS + QMARK
				+ AND + RESP_QID + EQUALS + QMARK
				+ AND + RESP_STUDENT + EQUALS + QMARK
				+ AND, statusIsEOrH(),
				ORDER_BY + RESP_TRIES); 
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				hwId,
				qId,
				studentId);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		EvalResult[] evalResults; // certainly given a value before return
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			final List<EvalResult> resultsList = new ArrayList<EvalResult>();
			while (rs.next()) {
				final EvalResult result = populateEvalResult(rs);
				if (result != null) {
					resultsList.add(result);
					debugPrint(SELF + "attempt ", result.tries,
							":\n", result.lastResponse);
				} // if there's a result
			} // while there are results
			final int numResults = resultsList.size();
			debugPrint(SELF + "got ", numResults, " record",
					numResults == 1 ? "." : "s.");
			evalResults = resultsList.toArray(new EvalResult[numResults]);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		return evalResults;
	} // getResults(Connection, String, int, int)

/* **************** Methods called by the gradebook *****************/

	/** Gets one student's or all students' EvalResults (excluding the 
	 * responses) for last responses to all attempted questions in a single 
	 * assignment.  Called from GradeSet.setCurrentHW().
	 * @param	hwId	the ID number of the assignment
	 * @param	studentId	student's login ID, or null for all students
	 * @return	Map of results of evaluation of responses of students
	 * to all questions in this assignment, keyed to student login ID
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<String, EvalResult[]> getOneHWResults(int hwId,
			String studentId) throws DBException {
		final boolean oneStudent = studentId != ALL_STUDENTS;
		final String where = toString(
				WHERE + RESP_LATEST + EQUALS + Y
				+ AND + RESP_HWID + EQUALS + QMARK
				+ AND, statusIsEOrH());
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where,
				hwId);
		if (oneStudent) {
 			sql_vals.addToSql(AND + RESP_STUDENT + EQUALS + QMARK);
 			sql_vals.addValue(studentId);
		} // if getting the grades of just one student
		return getOneHWResults(sql_vals, !GET_LAST_RESP);
	} // getOneHWResults(int, String)

	/** Retrieves all evaluation results, including the less-than-most-recent
	 * results and the responses, of one question in one assignment for all 
	 * students that have a record and are enrolled in the given course.  
	 * Called by RegradeSession.doRegrade().
	 * @param	hwId	the ID number of the assignment containing the 
	 * question whose grades are to be retrieved
	 * @param	qId	the unique ID number of the question whose grades are to 
	 * be retrieved.
	 * @param	crsId	the course of the students whose responses are to be 
	 * retrieved
	 * @return	evaluation results of students responding to this question, 
	 * keyed by student ID
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<String, EvalResult[]> getAllStudentsOneQ(int hwId, 
			int qId, int crsId) throws DBException {
		final StringBuilder whereBld = getBuilder(
				WHERE + RESP_HWID + EQUALS + QMARK
				+ AND + RESP_QID + EQUALS + QMARK
				+ AND, statusIsEOrH(),
				AND + RESP_STUDENT + IN, parens(
					SELECT + ENRL_STUDENT
					+ FROM + ENROLLMENT
					+ WHERE + ENRL_COURSE + EQUALS + QMARK));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(whereBld, 
				hwId, 
				qId, 
				crsId);
		return getOneHWResults(sql_vals, GET_LAST_RESP);
	} // getAllStudentsOneQ(int, int, int)

	/** Gets all students' EvalResults (excluding the responses) for last 
	 * responses to all attempted questions in a single assignment where the
	 * modified grades are null.  Called from 
	 * ResponseWrite.setRegradedResults().
	 * @param	con	database connection
	 * @param	hwId	the ID number of the assignment
	 * @return	Map of results of evaluation of responses of students
	 * to all questions in this assignment, keyed to student login ID
	 * @throws	DBException	if there's a problem reading the database
	 */
	static Map<String, EvalResult[]> getOneHWNullModGrades(Connection con, 
			int hwId) throws DBException {
		final StringBuilder whereBld = getBuilder(
				WHERE + RESP_LATEST + EQUALS + Y
				+ AND + RESP_HWID + EQUALS + QMARK
				+ AND + RESP_STATUS + EQUALS, quotes(EVALUATED),
				AND + RESP_MODGRADE + IS_NULL);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(whereBld, 
				hwId);
		return getOneHWResults(con, sql_vals, !GET_LAST_RESP);
	} // getOneHWNullModGrades(Connection, int)

	/** Gets students' EvalResults (with or without the responses) for their
	 * last responses to all questions in an assignment or all their responses
	 * to a single question in an assignment. 
	 * @param	whereSql_vals	constraints on the database query
	 * @param	getResponse	whether to get the response as well
	 * @return	results of evaluation of responses, keyed to login ID and 
	 * assignment ID
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static Map<String, EvalResult[]> getOneHWResults(
			SQLWithQMarks whereSql_vals, boolean getResponse) 
			throws DBException {
		Map<String, EvalResult[]> allEvalResultsByStudentId;
		Connection con = null;
		try {
			con = getPoolConnection(); 
			allEvalResultsByStudentId = 
					getOneHWResults(con, whereSql_vals, getResponse);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
		return allEvalResultsByStudentId;
	} // getOneHWResults(SQLWithQMarks, boolean)

	/** Gets students' EvalResults (with or without the responses) for their
	 * last responses to all questions in an assignment or all their responses
	 * to a single question in an assignment. 
	 * @param	con	database connection
	 * @param	whereSql_vals	constraints on the database query
	 * @param	getResponse	whether to get the response as well
	 * @return	results of evaluation of responses, keyed to login ID and 
	 * assignment ID
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static Map<String, EvalResult[]> getOneHWResults(
			Connection con, SQLWithQMarks whereSql_vals, boolean getResponse) 
			throws DBException {
		final String SELF = "ResponseRead.getOneHWResults: ";
		final String qry = toString(
				SELECT, joinAll(
					RESP_STUDENT,
					RESP_HWID,
					RESP_QID,
					RESP_TRIES,
					RESP_LATEST,
					RESP_GRADE,
					RESP_MODGRADE, 
					RESP_WHEN,
					RESP_IP,
					RESP_COMMENT,
					RESP_STATUS));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
		if (getResponse) sql_vals.addToSql(postjoin(RESP_LASTRESPONSE));
		sql_vals.addToSql( 
				FROM + RESPONSES,
				whereSql_vals.getSql(),
				ORDER_BY, joinAll(
					RESP_STUDENT,
					RESP_HWID,
					RESP_QID,
					RESP_TRIES));
		sql_vals.addValuesFrom(whereSql_vals);
		debugPrint(SELF, sql_vals);
		final HashMap<String, EvalResult[]> allEvalResultsByStudentId = 
				new HashMap<String, EvalResult[]>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) while (!rs.isAfterLast()) {
				// iterate for each hw and student
				final String studentId = rs.getString(RESP_STUDENT);
				while (!rs.isAfterLast()
						&& studentId.equals(rs.getString(RESP_STUDENT))) {
					final int hwId = rs.getInt(RESP_HWID);
					final ArrayList<EvalResult> evalResultsList =
							new ArrayList<EvalResult>();
					while (!rs.isAfterLast()
							&& studentId.equals(rs.getString(RESP_STUDENT))
							&& hwId == rs.getInt(RESP_HWID)) {
						// will get last response to all Qs 
						// or all responses to one Q
						final EvalResult evalResult = 
								populateEvalResult(rs, !NULL_FOR_EXCEPTION);
						if (evalResult.tries == 0 
								&& evalResult.status == EVALUATED) {
							evalResult.tries = 1;
						} // if should set 0 tries to 1
						evalResultsList.add(evalResult);
						debugPrint(SELF + "retrieving student ", 
								studentId, ", hwId ", hwId,
								", qId ", evalResult.qId, 
								": status ", evalResult.status,
								", tries ", evalResult.tries, 
								", grade ", evalResult.grade,
								", modified grade ", evalResult.modGrade);
						rs.next();
					} // until new student or new assignment or no more results
					final EvalResult[] evalResultsArr =
							evalResultsList.toArray(
								new EvalResult[evalResultsList.size()]);
					debugPrint(SELF + "for student ", studentId, 
							" and hwId ", hwId, ", got ", 
							evalResultsArr.length, " result(s).");
					allEvalResultsByStudentId.put(studentId, evalResultsArr);
				} // until a new student or no more results
			} // while there are more students
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		debugPrint(SELF + "got results for ", 
				allEvalResultsByStudentId.size(), " student(s).");
		return allEvalResultsByStudentId;
	} // getOneHWResults(Connection, SQLWithQMarks, boolean)

	/** Gets the total of the modified grades of one student on each assignment.
	 * Called by GradeSet() constructor.
	 * @param	hwIds	the ID numbers of the assignments
	 * @param	studentId	login ID of student whose results to get, or null
	 * to get grades of all students in the course
	 * @return	map of double[] keyed by studentId; each double[<i>n</i>] 
	 * corresponds to the sum of modified grades in assignment <i>n</i>; 
	 * -1 if unattempted.
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<String, double[]> getTotalGrades(int[] hwIds, 
			String studentId) throws DBException {
		return getTotalGrades(hwIds, 0, studentId);
	} // getTotalGrades(int[], String)

	/** Gets the total of the modified grades of each student on each assignment
	 * or of one student on each assignment. Called by GradeSet() constructor.
	 * @param	hwIds	the ID numbers of the assignments
	 * @param	courseId	when retrieving tutorial results, ID number of the 
	 * course containing the students whose grades should be retrieved; 0
	 * otherwise
	 * @param	studentId	login ID of student whose results to get, or null
	 * to get grades of all students in the course
	 * @return	map of double[] keyed by studentId; each double[<i>n</i>] 
	 * corresponds to the sum of modified grades in assignment <i>n</i>; 
	 * -1 if unattempted.
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<String, double[]> getTotalGrades(int[] hwIds, 
			int courseId, String studentId) throws DBException {
		final String SELF = "ResponseRead.getTotalGrades: ";
		final Map<String, double[]> sumGrades = new HashMap<String, double[]>();
		if (Utils.isEmpty(hwIds)) return sumGrades;
		final boolean tutorials = courseId != 0;
		final String gradeCol = (tutorials ? RESP_GRADE : RESP_MODGRADE);
		// get the sums of grades for most recent tries only
		// group results and order them
		final String subqry = toString(
				SELECT, joinAll(
					RESP_STUDENT,
					RESP_HWID,
					gradeCol),
				FROM + RESPONSES
				+ WHERE + RESP_LATEST + EQUALS + Y
				+ AND + RESP_HWID + IN, parensQMarks(hwIds),
				AND + RESP_STATUS + EQUALS, quotes(EVALUATED));
		SQLWithQMarks sql_vals = new SQLWithQMarks(subqry, 
				hwIds);
		if (studentId != null) {
			sql_vals.addToSql(AND + RESP_STUDENT + EQUALS + QMARK);
			sql_vals.addValue(studentId);
		} else if (tutorials) {
			sql_vals.addToSql(
					AND + RESP_STUDENT + IN, parens(
						SELECT + ENRL_STUDENT
						+ FROM + ENROLLMENT
						+ WHERE + ENRL_COURSE + EQUALS + QMARK));
			sql_vals.addValue(courseId);
		} // if a student or tutorials
		final String qry = toString(
				SELECT, joinAll(
					RESP_STUDENT,
					RESP_HWID,
					toString(sum(gradeCol), AS + SRCH_RESULT)),
				FROM, parens(sql_vals.getSql()),
				GROUP_BY, joinAll(
					RESP_STUDENT,
					RESP_HWID),
				ORDER_BY + RESP_STUDENT);
		sql_vals = new SQLWithQMarks(qry, sql_vals);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) while (!rs.isAfterLast()) {
				final String respStudent = rs.getString(RESP_STUDENT);
				final double[] sums = new double[hwIds.length];
				Arrays.fill(sums, UNATTEMPTED);
				while (!rs.isAfterLast() 
						&& respStudent.equals(rs.getString(RESP_STUDENT))) {
					final int hwId = rs.getInt(RESP_HWID);
					final double sum = rs.getDouble(SRCH_RESULT);
					final int hwNum = Utils.indexOf(hwIds, hwId);
					if (hwNum >= 0) sums[hwNum] = sum;
					rs.next();
				} // while there are more assignments of this student
				sumGrades.put(respStudent, sums);
				debugPrint(SELF + "student ", respStudent, ": sums = ", sums);
			} // while there are more students
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return sumGrades;
	} // getTotalGrades(int[], int, String)

	/** Gets whether each (or one) student has mastered each assignment.
	 * @param	hwIds	the ID numbers of the assignments
	 * @param	studentId	login ID of student whose results to get, or null
	 * to get grades of all students in the course
	 * @return	map of boolean[] keyed by studentId; each boolean[<i>n</i>] 
	 * corresponds to whether assignment <i>n</i> has been mastered (no
	 * distinction between unattempted and unmastered)
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<String, boolean[]> getAssgtsMastered(int[] hwIds, 
			String studentId) throws DBException {
		final String SELF = "ResponseRead.getAssgtsMastered: ";
		final Map<String, boolean[]> assgtsMastered = 
				new HashMap<String, boolean[]>();
		if (hwIds.length == 0) return assgtsMastered;
		final String NUM_MASTERED = SRCH_RESULT + "1";
		final String NUM_QS_SEEN = SRCH_RESULT + "2";
		final String TABLE1 = " T1";
		final String TABLE2 = " T2";
		final SQLWithQMarks numQsSeenSql_vals =
				HWRead.GET_NUM_QS_SEEN_SQL(hwIds, NUM_QS_SEEN);
		final StringBuilder subqryBld = getBuilder(
				SELECT, joinAll(
					RESP_STUDENT,
					RESPONSES + DOT + RESP_HWID,
					RESP_GRADE),
				FROM + RESPONSES + JOIN + HWSETS 
					+ ON + RESPONSES + DOT + RESP_HWID
						+ EQUALS + HWSETS + DOT + HW_ID
				+ WHERE + RESPONSES + DOT + RESP_HWID 
					+ IN, parensQMarks(hwIds),
				AND + RESP_LATEST + EQUALS + Y
				+ AND + RESP_GRADE + IS_1
				+ AND + RESPONSES + DOT + RESP_TRIES 
					+ NOT_MORE_THAN + HWSETS + DOT + HW_TRIES);
		final SQLWithQMarks sql_vals = new SQLWithQMarks("", 
				hwIds);
		if (studentId != null) {
			subqryBld.append(AND + RESP_STUDENT + EQUALS + QMARK);
			sql_vals.addValue(studentId);
		} // if SQL factors
		final String qry = toString(
				SELECT + ALL + FROM, parensBuild(
					SELECT, joinAll(
						RESP_STUDENT,
						RESP_HWID,
						toString(sum(RESP_GRADE), AS, NUM_MASTERED)),
					FROM, parens(subqryBld),
					GROUP_BY, joinAll(
						RESP_STUDENT,
						RESP_HWID),
					ORDER_BY + RESP_STUDENT),
				TABLE1 + JOIN, parens(numQsSeenSql_vals.getSql()),
				TABLE2 + ON + TABLE1 + DOT + RESP_HWID 
					+ EQUALS + TABLE2 + DOT + HW_ID
				+ WHERE + NUM_MASTERED + NOT_LESS_THAN + NUM_QS_SEEN);
		sql_vals.addValuesFrom(numQsSeenSql_vals);
		sql_vals.setSql(qry);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) while (!rs.isAfterLast()) {
				final String respStudent = rs.getString(RESP_STUDENT);
				final boolean[] studAssgtsMastered = new boolean[hwIds.length];
				while (!rs.isAfterLast() 
						&& respStudent.equals(rs.getString(RESP_STUDENT))) {
					final int hwId = rs.getInt(RESP_HWID);
					final int hwNum = Utils.indexOf(hwIds, hwId);
					if (hwNum >= 0) studAssgtsMastered[hwNum] = true;
					rs.next();
				} // while there are more assignments of this student
				assgtsMastered.put(respStudent, studAssgtsMastered);
				debugPrint(SELF + "student ", respStudent, 
						": assgtsMastered = ", studAssgtsMastered);
			} // while there are more students
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return assgtsMastered;
	} // getAssgtsMastered(int[], String)

	/** Gets whether the given student has mastered the given assignment.
	 * @param	studentId	login ID of the student
	 * @param	hwId	assignment ID
	 * @return	whether the student has mastered the assignment
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static boolean hasMastered(String studentId, int hwId) 
			throws DBException {
		final String SELF = "ResponseRead.hasMastered: ";
		boolean mastered = false;
		final String NUM_MASTERED = SRCH_RESULT + "1";
		final String NUM_QS_SEEN = SRCH_RESULT + "2";
		final SQLWithQMarks numQsSeenSql_vals =
				HWRead.GET_NUM_QS_SEEN_SQL(hwId, NUM_QS_SEEN);
		final String qry = toString(
				SELECT + ALL + FROM, joinAll(
					parensBuild(
						SELECT, sum(RESP_GRADE), AS + NUM_MASTERED
						+ FROM + RESPONSES + JOIN + HWSETS 
							+ ON + RESPONSES + DOT + RESP_HWID
								+ EQUALS + HWSETS + DOT + HW_ID
						+ WHERE + RESP_LATEST + EQUALS + Y
						+ AND + RESP_GRADE + IS_1
						+ AND + RESPONSES + DOT + RESP_TRIES 
							+ NOT_MORE_THAN + HWSETS + DOT + HW_TRIES
						+ AND + RESPONSES + DOT + RESP_HWID 
							+ EQUALS + QMARK
						+ AND + RESP_STUDENT + EQUALS + QMARK),
					parens(numQsSeenSql_vals.getSql())));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				hwId, 
				studentId);
		sql_vals.addValuesFrom(numQsSeenSql_vals);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				final int numMastered = rs.getInt(NUM_MASTERED);
				final int numQsSeen = rs.getInt(NUM_QS_SEEN);
				mastered = numMastered >= numQsSeen;
				debugPrint(SELF + "for student ", studentId, 
						" and hwId ", hwId, ", numMastered = ", numMastered,
						", numQsSeen = ", numQsSeen, ", mastered = ", mastered);
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
		return mastered;
	} // hasMastered(String, int)

	/** For each assignment and each student, gets whether any of the student's
	 * last responses requires human grading. Called from
	 * GradeSet.getHumanGradingReqd().
	 * @param	courseId	ID number of the course containing the assignments
	 * @param	studentId	student's login ID
	 * @return	map of list of students whose most recent attempts in an
	 * assignment require human grading, mapped by assignment ID
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<Integer, ArrayList<String>> getHumanGradingRequired(
			int courseId, String studentId) throws DBException {
		final String SELF = "ResponseRead.getHumanGradingRequired: ";
		final String qry = toString(
				SELECT, joinAll(
					RESP_STUDENT,
					RESP_HWID),
				FROM + RESPONSES
				+ WHERE + RESP_HWID + IN, parens(
					SELECT + HW_ID
					+ FROM + HWSETS
					+ WHERE + HW_COURSE + EQUALS + QMARK));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				courseId);
		if (studentId != ALL_STUDENTS) {
 			sql_vals.addToSql(AND + RESP_STUDENT + EQUALS + QMARK);
 			sql_vals.addValue(studentId);
		} // if just one student
		sql_vals.addToSql(
				AND + RESP_LATEST + EQUALS + Y
				+ AND + RESP_STATUS + EQUALS, quotes(HUMAN_NEEDED),
				ORDER_BY, joinAll(
					RESP_HWID,
					RESP_STUDENT));
		debugPrint(SELF, sql_vals);
		final Map<Integer, ArrayList<String>> humanGradingReqd = 
				new HashMap<Integer, ArrayList<String>>();
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) while (!rs.isAfterLast()) {
				final int hwId = rs.getInt(RESP_HWID);
				final ArrayList<String> studentsWithHumanReqd = 
						new ArrayList<String>();
				while (!rs.isAfterLast() 
						&& hwId == rs.getInt(RESP_HWID)) {
					studentsWithHumanReqd.add(rs.getString(RESP_STUDENT));
					rs.next();
				} // while there are more assignments of this student
				humanGradingReqd.put(Integer.valueOf(hwId), 
						studentsWithHumanReqd);
			} // while there are more students
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		debugPrint(SELF + "humanGradingReqd = ", humanGradingReqd);
		return humanGradingReqd;
	} // getHumanGradingRequired(int, String)

	/** Gets students' results from particular courses, in which the question
	 * statement or keywords contain an expression.
	 * @param	studentIds	login IDs of students whose results should be
	 * retrieved
	 * @param	courseIds	ID numbers of courses of which results should be
	 * retrieved
	 * @param	searchExp	word fragment, word, phrase, simple boolean
	 * expression, or regular expression for which to search question statements
	 * and keywords
	 * @return	results of evaluation of responses
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static CrossCourseReport getCrossCourseReport(String[] studentIds, 
			int[] courseIds, String searchExp) throws DBException {
		final String SELF = "ResponseRead.CrossCourseReport: ";
		final CrossCourseReport crossCourseReport = new CrossCourseReport();
		if (Utils.isEmpty(courseIds) || Utils.isEmpty(studentIds)) {
			return crossCourseReport;
		} // if no courses or students
		final String searchExpMod = searchExp.trim();
		final String[] searchExpsAnd = searchExpMod.split(" and ");
		final String Q_STMT_OR_KEYWDS = QSET_COMMONQSTATEMENT + UNITE 
				+ Q_KEYWORDS + UNITE + Q_STATEMENT;
		final StringBuilder searchBld = new StringBuilder();
		if (searchExpsAnd.length > 1) {
			boolean first = true;
			for (final String searchExpAnd : searchExpsAnd) {
				if (first) first = false;
				else searchBld.append(AND);
 				appendTo(searchBld, regexp_instr(Q_STMT_OR_KEYWDS, 
						quotes(searchExpAnd.trim())), IS_POSITIVE);
			} // for each searchExpAnd
		} else {
			final String[] searchExpsOr = searchExpMod.split(" or ");
			boolean first = true;
			for (final String searchExpOr : searchExpsOr) {
				if (first) first = false;
				else searchBld.append(OR);
 				appendTo(searchBld, regexp_instr(Q_STMT_OR_KEYWDS, 
						quotes(searchExpOr.trim())), IS_POSITIVE);
			} // for each searchExpOr
		} // if expression contains " and "
		final DBLocalTables localTables = new DBLocalTables();
		final String LOCAL_QS = localTables.QUESTIONS;
		final String MASTER_QS = (new DBTables()).QUESTIONS;
		final String localQs = toString(
				SELECT, LOCAL_QS, DOT + Q_QID 
				+ FROM, LOCAL_QS, JOIN + QSETS 
					+ ON, LOCAL_QS, DOT + Q_QSET 
						+ EQUALS + QSETS + DOT + QSET_ID 
				+ WHERE, LOCAL_QS, DOT, DBLocalTables.Q_AUTHOR,
					EQUALS + COURSES + DOT + CRS_INSTRUCTOR);
		final String HWNUM = "hw_" + HW_NUM;
		final String QNUM = "q_" + ASSGND_QS_QNUM;
		final String qry = toString(
				SELECT, joinAll(
					HW_COURSE,
					RESPONSES + DOT + RESP_STUDENT + AS + RESP_STUDENT,
					RESPONSES + DOT + RESP_HWID + AS + RESP_HWID,
					HWSETS + DOT + HW_NUM + AS + HWNUM,
					RESPONSES + DOT + RESP_QID + AS + RESP_QID,
					ASSIGNED_QS + DOT + ASSGND_QS_QNUM + AS + QNUM,
					RESPONSES + DOT + RESP_TRIES + AS + RESP_TRIES,
					RESP_LATEST,
					RESP_GRADE,
					RESP_MODGRADE, 
					RESP_WHEN,
					RESP_IP,
					RESP_COMMENT,
					RESP_STATUS),
				FROM + RESPONSES + JOIN + HWSETS
					+ ON + RESPONSES + DOT + RESP_HWID
						+ EQUALS + HWSETS + DOT + HW_ID
				+ JOIN + ASSIGNED_QS
					+ ON + RESPONSES + DOT + RESP_STUDENT + EQUALS 
						+ ASSIGNED_QS + DOT + ASSGND_QS_STUDENT
					+ AND + RESPONSES + DOT + RESP_HWID + EQUALS
						+ ASSIGNED_QS + DOT + ASSGND_QS_HWID
					+ AND + RESPONSES + DOT + RESP_QID + EQUALS
						+ ASSIGNED_QS + DOT + ASSGND_QS_QID
				+ JOIN + COURSES 
					+ ON + COURSES + DOT + CRS_ID + EQUALS
						+ HWSETS + DOT + HW_COURSE
				+ WHERE + RESP_LATEST + EQUALS + Y
				+ AND + RESPONSES + DOT + RESP_STUDENT + IN,
					parensQMarks(studentIds),
				AND + RESPONSES + DOT + RESP_QID + IN, parensBuild(
					localQs, AND, searchBld,
					UNION_ALL + SELECT, MASTER_QS, DOT + Q_QID 
					+ FROM, MASTER_QS, JOIN + QSETS 
						+ ON, MASTER_QS, DOT + Q_QSET 
							+ EQUALS + QSETS + DOT + QSET_ID 
					+ WHERE, searchBld,
					AND, MASTER_QS, DOT + Q_QID + NOT + IN, parens(localQs)),
				AND + HW_COURSE + IN, parensQMarks(courseIds),
				AND, statusIsEOrH(),
				ORDER_BY, joinAll(
					RESP_STUDENT,
					HW_COURSE,
					HWNUM,
					QNUM));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				studentIds,
				courseIds);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection(); 
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) while (!rs.isAfterLast()) {
				final String studentId = rs.getString(RESP_STUDENT);
				while (!rs.isAfterLast()
						&& studentId.equals(rs.getString(RESP_STUDENT))) {
					final int courseId = rs.getInt(HW_COURSE);
					while (!rs.isAfterLast()
							&& studentId.equals(rs.getString(RESP_STUDENT))
							&& courseId == rs.getInt(HW_COURSE)) {
						final int hwId = rs.getInt(RESP_HWID);
						final int hwNum = rs.getInt(HWNUM);
						while (!rs.isAfterLast()
								&& studentId.equals(rs.getString(RESP_STUDENT))
								&& courseId == rs.getInt(HW_COURSE)
								&& hwId == rs.getInt(RESP_HWID)) {
							final int qNum = rs.getInt(QNUM);
							final EvalResult evalResult = 
									populateEvalResult(rs, !NULL_FOR_EXCEPTION);
							if (evalResult.tries == 0 
									&& evalResult.status == EVALUATED) {
								evalResult.tries = 1;
							} // if should set 0 tries to 1
							crossCourseReport.addResult(studentId, courseId,
									new int[] {hwId, hwNum}, qNum, evalResult);
							debugPrint(SELF + "retrieved student ", 
									studentId, ", hwNum ", hwNum,
									", hwId ", hwId,
									", qNum ", qNum,
									", qId ", evalResult.qId, 
									": status ", evalResult.status,
									", tries ", evalResult.tries, 
									", grade ", evalResult.grade,
									", modified grade ", evalResult.modGrade);
							rs.next();
						} // until new student, new course, new HW or no more results
					} // until new student or new course or no more results
				} // until a new student or no more results
			} // while there are more students
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return crossCourseReport;
	} // getCrossCourseReport(String[], int[], String)

/* *********** Methods to retrieve substitutions  ************/

	/** Retrieves substitutions (R groups or variable values) assigned to a 
	 * student for all questions in an assignment.  Called by HWSession().
	 * @param	hwId	unique ID of the assignment
	 * @param	studentId	student's login ID
	 * @return	map of substitutions assigned to a student for all questions 
	 * in an assignment, keyed by question ID number
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<Integer, String[]> getStoredSubstns(int hwId,
			String studentId) throws DBException {
		final int ALL_QS = 0;
		Connection con = null;
		try {
			con = getPoolConnection(); 
			return getStoredSubstns(con, hwId, studentId, ALL_QS);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // getStoredSubstns(int, String)

	/** Retrieves substitutions (R groups or variable values) assigned to a 
	 * student for a single question in an assignment.  Called by 
	 * HWSession.init(), RegradeSession.doRegrade(), and getCurrentSubstns().
	 * @param	hwId	unique ID of the assignment
	 * @param	studentId	student's login ID
	 * @param	qId	unique ID of the question
	 * @return	substitutions assigned to a student for a single question in 
	 * an assignment
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[] getStoredSubstns(int hwId, String studentId, 
			int qId) throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection(); 
			final Map<Integer, String[]> substns =
					getStoredSubstns(con, hwId, studentId, qId);
			return substns.get(Integer.valueOf(qId)); // may be empty
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // getStoredSubstns(int, String, int)

	/** Retrieves substitutions (R groups or variable values) assigned to a 
	 * student for one or all questions in an assignment. 
	 * @param	con	database connection
	 * @param	hwId	unique ID of the assignment
	 * @param	studentId	student's login ID
	 * @param	qId	unique ID of the question, or 0 for all questions
	 * @return	map of substitutions assigned to one student for a single 
	 * question or all questions in an assignment, keyed by student ID
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static Map<Integer, String[]> getStoredSubstns(Connection con, 
			int hwId, String studentId, int qId) throws DBException {
		final String SELF = "ResponseRead.getStoredSubstns: ";
		final String qry = toString(
				SELECT, joinAll(
					RESP_SUBS_QID,
					RESP_SUBS_SUBSTN),
				FROM + RESP_SUBSTNS
				+ WHERE + RESP_SUBS_HWID + EQUALS + QMARK
				+ AND + RESP_SUBS_STUDENT + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				hwId, 
				studentId);
		if (qId != 0) {
 			sql_vals.addToSql(AND + RESP_SUBS_QID + EQUALS + QMARK);
 			sql_vals.addValue(qId);
		} // if get values for just one Q
		sql_vals.addToSql(
				ORDER_BY, joinAll(
					RESP_SUBS_QID,
					RESP_SUBS_NUM)); 
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final Map<Integer, String[]> substnsMap = 
				new HashMap<Integer, String[]>();
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) while (!rs.isAfterLast()) {
				final List<String> oneQSubstns = new ArrayList<String>();
				final int substnsQId = rs.getInt(RESP_SUBS_QID);
				while (!rs.isAfterLast()
						&& substnsQId == rs.getInt(RESP_SUBS_QID)) {
					oneQSubstns.add(rs.getString(RESP_SUBS_SUBSTN));
					rs.next();
				} // while there are more substitutions for this Q
				debugPrint(SELF + "got substitutions ", oneQSubstns,
						" for ", studentId, " on HW ", hwId, ", Q ", 
						substnsQId);
				final int numSubstns = oneQSubstns.size();
				substnsMap.put(Integer.valueOf(substnsQId), 
						oneQSubstns.toArray(new String[numSubstns]));
			} // while there are more Qs
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		return substnsMap;
	} // getStoredSubstns(Connection, int, String, int)

	/** Retrieves substitutions (R groups or variable values) assigned to all 
	 * students for a single question in an assignment who are enrolled in a 
	 * particular course. Called by RegradeSession.doRegrade().
	 * @param	hwId	unique ID of the assignment
	 * @param	qId	unique ID of the question
	 * @param	crsId	the course of the students whose substitutions are to be 
	 * retrieved
	 * @return	map of substitutions assigned to all students for a single 
	 * question in an assignment, keyed by student login IDs.
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<String, String[]> getStoredSubstns(int hwId, int qId,
			int crsId) throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection(); 
			return getStoredSubstns(con, hwId, qId, crsId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // getStoredSubstns(int, int, int)

	/** Retrieves substitutions (R groups or variable values) assigned to all 
	 * students for a single question in an assignment who are enrolled in a 
	 * particular course.
	 * @param	con	database connection
	 * @param	hwId	unique ID of the assignment
	 * @param	qId	unique ID of the question
	 * @param	crsId	the course of the students whose substitutions are to be 
	 * retrieved
	 * @return	map of substitutions assigned to all students for a single 
	 * question in an assignment, keyed by student ID
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static Map<String, String[]> getStoredSubstns(Connection con, 
			int hwId, int qId, int crsId) throws DBException {
		final String SELF = "ResponseRead.getStoredSubstns: ";
		final String qry = toString(
				SELECT, joinAll(
					RESP_SUBS_STUDENT,
					RESP_SUBS_SUBSTN),
				FROM + RESP_SUBSTNS
					+ WHERE + RESP_SUBS_HWID + EQUALS + QMARK
				+ AND + RESP_SUBS_QID + EQUALS + QMARK
				+ AND + RESP_SUBS_STUDENT + IN, parens(
					SELECT + ENRL_STUDENT
					+ FROM + ENROLLMENT
					+ WHERE + ENRL_COURSE + EQUALS + QMARK),
				ORDER_BY, joinAll(
					RESP_SUBS_STUDENT,
					RESP_SUBS_NUM)); 
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				hwId,
				qId,
				crsId);
		debugPrint(SELF, sql_vals);
		final Map<String, String[]> substnsMap = 
				new HashMap<String, String[]>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) while (!rs.isAfterLast()) {
				final List<String> studentSubstns = 
						new ArrayList<String>();
				final String studentId = rs.getString(RESP_SUBS_STUDENT);
				while (!rs.isAfterLast()
						&& studentId.equals(rs.getString(RESP_SUBS_STUDENT))) {
					studentSubstns.add(rs.getString(RESP_SUBS_SUBSTN));
					rs.next();
				} // while there are more substitutions for this Q
				debugPrint(SELF + "got substitutions ", studentSubstns,
						" for ", studentId, " on HW ", hwId, ", Q ", 
						qId);
				final int numSubstns = studentSubstns.size();
				substnsMap.put(studentId, studentSubstns.toArray(
						new String[numSubstns]));
			} // while there are more Qs
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		return substnsMap;
	} // getStoredSubstns(Connection, int, int, int)

/* **************** Miscellaneous methods *****************/

	/** Retrieves the modified grade of a student.  Called by
	 * ResponseWrite.addResult() and setResult().
	 * @param	con	database connection
	 * @param	hwId	unique ID of the assignment
	 * @param	studentId	student's login ID
	 * @param	evalResult	student result where the modified grade will be
	 * stored
	 * @throws	DBException	if there's a problem reading the database
	 */
	static void getModifiedGrade(Connection con, int hwId, String studentId, 
			EvalResult evalResult) throws DBException {
		final Map<String, EvalResult> evalResultsByStudentIds = 
				new HashMap<String, EvalResult>();
		evalResultsByStudentIds.put(studentId, evalResult);
		getModifiedGrades(con, hwId, evalResultsByStudentIds);
	} // getModifiedGrade(Connection, int, String, EvalResult)

	/** Retrieves the modified grades of students.  Called by above and
	 * ResponseWrite.setResults().
	 * @param	con	database connection
	 * @param	hwId	unique ID of the assignment
	 * @param	evalResultsByStudentIds	map of student results (where the
	 * modified grade will be stored) keyed by student IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	static void getModifiedGrades(Connection con, int hwId, 
			Map<String, EvalResult> evalResultsByStudentIds) 
			throws DBException {
		final String SELF = "ResponseRead.getModifiedGrades: ";
		if (evalResultsByStudentIds.isEmpty()) return;
		final List<StringBuilder> groupsSql = 
				new ArrayList<StringBuilder>();
		final SQLWithQMarks groupSql_vals = new SQLWithQMarks();
		final List<String> studentIds = 
				new ArrayList<String>(evalResultsByStudentIds.keySet());
		for (final String studentId : studentIds) {
			final EvalResult evalResult = 
					evalResultsByStudentIds.get(studentId);
			groupsSql.add(parensQMarks(3));
			groupSql_vals.addValues(
					studentId,
					evalResult.qId,
					evalResult.tries);
		} // for each student
		final String qry = toString(
				SELECT, joinAll(
					RESP_STUDENT,
					RESP_MODGRADE),
				FROM + RESPONSES 
				+ WHERE + RESP_HWID + EQUALS + QMARK
				+ AND, parens(joinAll(
					RESP_STUDENT,
					RESP_QID,
					RESP_TRIES)),
				IN, parens(groupsSql)); 
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				hwId);
		sql_vals.addValuesFrom(groupSql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final String studentId = rs.getString(RESP_STUDENT);
				final EvalResult evalResult = 
						evalResultsByStudentIds.get(studentId);
				evalResult.modGrade = rs.getDouble(RESP_MODGRADE);
				debugPrint(SELF + "student ", studentId, ", hwId ", hwId,
						", qId ", evalResult.qId, ", tries ", evalResult.tries,
						": grade = ", evalResult.grade, ", modified grade = ",
						evalResult.modGrade);
			} // while there are more results
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
	} // getModifiedGrades(Connection, int, Map<String, EvalResult>)

	/** Populates an EvalResult with results from the database.
	 * @param	rs	results from the database
	 * @return	data from the database regarding the evaluation of a student's
	 * response
	 */
	private static EvalResult populateEvalResult(ResultSet rs) { 
		return populateEvalResult(rs, NULL_FOR_EXCEPTION);
	} // populateEvalResult(ResultSet)

	/** Populates an EvalResult with results from the database.
	 * @param	rs	results from the database
	 * @param	nullForException	whether to return null if we get an
	 * SQLException, or just to set lastResponse to ""
	 * @return	data from the database regarding the evaluation of a student's
	 * response
	 */
	private static EvalResult populateEvalResult(ResultSet rs,
			boolean nullForException) { 
		EvalResult evalResult = new EvalResult();
		try {
			evalResult.tries = rs.getInt(RESP_TRIES);
			evalResult.mostRecent = rs.getString(RESP_LATEST).charAt(0) == 'Y';
			evalResult.grade = rs.getDouble(RESP_GRADE);
			evalResult.modGrade = rs.getDouble(RESP_MODGRADE);
			evalResult.timeOfResponse = toDate(rs.getString(RESP_WHEN));
			evalResult.ipAddr = rs.getString(RESP_IP);
			evalResult.status = rs.getString(RESP_STATUS).charAt(0);
			evalResult.comment = rs.getString(RESP_COMMENT);
			evalResult.qId = rs.getInt(RESP_QID);
			evalResult.lastResponse = decompressIfLewis(
					rs.getString(RESP_LASTRESPONSE));
		} catch (SQLException e) {
			if (nullForException) {
				Utils.alwaysPrint("ResponseRead.populateEvalResult: "
						+ "exception while getting data, returning null.");
				e.printStackTrace();
				evalResult = null;
			} else evalResult.lastResponse = "";
		} // try
		return evalResult;
	} // populateEvalResult(ResultSet, boolean)

	/** Retrieves assignment IDs that have saved but not submitted responses.
	 * @param	studentId	student's login ID
	 * @param	courseId	unique ID of the course
	 * @return	list of assignment IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<Integer> getHWIdsSavedUnsubmitted(
			String studentId, int courseId) throws DBException {
		final String SELF = "ResponseRead.getHWIdsSavedUnsubmitted: ";
		final String qry = toString(
				SELECT_UNIQUE + RESP_HWID
				+ FROM + RESPONSES, ' ', SRCH_RESULT
				+ WHERE + RESP_STUDENT + EQUALS + QMARK
				+ AND + RESP_HWID + IN, parens(
					SELECT + HW_ID
					+ FROM + HWSETS
					+ WHERE + HW_COURSE + EQUALS + QMARK),
				AND + RESP_STATUS + EQUALS, quotes(SAVED),
				AND, parensBuild(
					SELECT, count(),
					FROM + RESPONSES
					+ WHERE + RESP_STUDENT + EQUALS + QMARK
					+ AND + RESP_HWID + EQUALS 
						+ SRCH_RESULT + DOT + RESP_HWID
					+ AND + RESP_QID + EQUALS + SRCH_RESULT + DOT + RESP_QID
					+ AND, statusIsEOrH()),
				IS_ZERO, 
				ORDER_BY + RESP_HWID);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				studentId,
				courseId,
				studentId);
		debugPrint(SELF, sql_vals);
		final List<Integer> assgtIds = new ArrayList<Integer>();
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection(); 
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				assgtIds.add(Integer.valueOf(rs.getInt(RESP_HWID)));
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
		return assgtIds;
	} // getHWIdsSavedUnsubmitted(String, int)

	/** Gets if there have been any responses to questions with the given IDs in
	 * this assignment.
	 * @param	hwId	ID number of the assignment
	 * @param	qIds	ID numbers of the questions
	 * @return	true if there have been any responses to any of the questions
	 * with the given IDs in this assignment
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static boolean haveResponses(int hwId, List<Integer> qIds) 
			throws DBException {
		final String SELF = "ResponseRead.haveResponses: ";
		boolean haveResponse = false;
		if (qIds.isEmpty()) return haveResponse;
		final String qry = toString(
				SELECT, count(), AS + SRCH_RESULT 
				+ FROM + RESPONSES
				+ WHERE + RESP_HWID + EQUALS + QMARK
				+ AND + RESP_QID + IN, parensQMarks(qIds),
				AND, statusIsEOrH());
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				hwId,
				qIds);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection(); 
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) haveResponse = rs.getInt(SRCH_RESULT) > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return haveResponse;
	} // haveResponses(int, List<Integer>)

	/** Gets an SQL phrase where status can equal EVALUATED or HUMAN_NEEDED.
	 * @return	an SQL phrase in a StringBuilder
	 */
	private static StringBuilder statusIsEOrH() {
		return parensBuild(RESP_STATUS + EQUALS, quotes(EVALUATED),
				OR + RESP_STATUS + EQUALS, quotes(HUMAN_NEEDED));
	} // statusIsEOrH()

	/** Constructor to disable external instantiation. */
	private ResponseRead() { } 

} // ResponseRead
