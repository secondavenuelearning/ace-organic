package com.epoch.db;

import static com.epoch.db.dbConstants.CourseRWConstants.*;
import static com.epoch.db.dbConstants.ResponsesConstants.*;
import static com.epoch.db.dbConstants.TimePeriodConstants.*;
import static com.epoch.db.dbConstants.UserRWConstants.*;
import static com.epoch.utils.utilConstants.DateConstants.*;
import com.epoch.assgts.assgtConstants.AssgtConstants;
import com.epoch.evals.EvalResult;
import com.epoch.evals.evalConstants.EvalResultConstants;
import com.epoch.exceptions.DBException;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** For storing a student's response to a question in an assignment. */
public final class ResponseWrite extends DBCommon 
		implements AssgtConstants, EvalResultConstants {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Value for qNum.  */
	public static final int NO_QNUM = 0;
	/** Parameter for setResult().  */
	static final boolean[] DEFAULT_FLAGS = new boolean[] {true, true, false};
		/** Member of flags: should the response be written to the
		 * record as well?  */
		public static final int REWRITE_RESP = 0;
		/** Member of flags: is the response being edited the most
		 * recent response? */
		public static final int EDIT_MOST_RECENT = 1;
		/** Member of flags: should the R groups of this response be
		 * preserved when all of a student's responses are deleted? */
		public static final int PRESERVE_SUBSTNS = 2;
	/** Parameter for deleteResults().  */
	private static final String ALL_STUDENTS = null;
	/** Substitute name for a table field. */
	private static final String IN_DAYS = "in_days";

	/** Stores the substitutions (R groups or variable values) of a student's 
	 * view of a question in an assignment.  
	 * @param	studentId	student's login ID
	 * @param	hwId	unique ID of the assignment
	 * @param	qId	unique ID of the question
	 * @param	substns	R groups or variable values to be stored
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void storeSubstns(int hwId, String studentId, int qId,
			String[] substns) throws DBException {
		final String SELF = "ResponseWrite.storeSubstns: ";
		final String[] fields = new String[] {
				RESP_SUBS_HWID, 
				RESP_SUBS_STUDENT, 
				RESP_SUBS_QID, 
				RESP_SUBS_NUM, 
				RESP_SUBS_SUBSTN};
		final String qry = getInsertIntoValuesQMarksSQL(RESP_SUBSTNS, fields);
		debugPrint(SELF, qry); 
		Connection con = null;
		PreparedStatement stmt = null;
		if (!Utils.isEmpty(substns)) try {
			con = getPoolConnection(); 
			con.setAutoCommit(false);
			stmt = getStatement(con, qry);
			int grpNum = 1;
			for (final String substn : substns) {
				final StringBuilder joinedValues = setValues(stmt,
						hwId,
						studentId,
						qId,
						grpNum++,
						substn);
				debugPrint(SELF, "batch ", grpNum - 1, ": ", joinedValues); 
				stmt.addBatch();
			} // for each R group
			stmt.executeBatch();
			con.commit();
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
	} // storeSubstns(int, String, int, String[])

	/** Stores the response of a student to a question.  It may actually 
	 * be an initialized view of the question and not an actual response.
	 * @param	studentId	student's login ID
	 * @param	hwId	unique ID of the assignment
	 * @param	qNum	1-based number of the question within the assignment
	 * @param	evalResult	information to be stored
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void addResult(String studentId, int hwId, int qNum, 
			EvalResult evalResult) throws DBException {
		final String SELF = "ResponseWrite.addResult: ";
		Connection con = null;
		try {
			con = getPoolConnection(); 
			addResult(con, studentId, hwId, qNum, evalResult);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (DBException e) {
			e.printStackTrace();
			throw e;
		} finally {
			closeConnection(con);
		}
	} // addResult(String, int, int, EvalResult)

	/** Stores the response of a student to a question.  It may 
	 * be an initialized view of the question and not an actual response.
	 * @param	con	database connection
	 * @param	studentId	student's login ID
	 * @param	hwId	unique ID of the assignment
	 * @param	qNum	1-based number of the question within the assignment
	 * @param	evalResult	information to be stored
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private static void addResult(Connection con, String studentId, 
			int hwId, int qNum, EvalResult evalResult) throws DBException {
		final String SELF = "ResponseWrite.addResult: ";
		try {
			con.setAutoCommit(false);
			if (evalResult.timeOfResponse == null) {
				debugPrint(SELF + "timeOfResponse is null, setting to now.");
				evalResult.timeOfResponse = new Date();
			}
			// delete any previously existing record with same number of tries;
			// may occur for 0th try (initialized, save without submitting) 
			// or nth try (response following an unparsable mechanism or 
			// synthesis response)
			final SQLWithQMarks whereSql_vals = new SQLWithQMarks( 
					WHERE + RESP_STUDENT + EQUALS + QMARK
						+ AND + RESP_HWID + EQUALS + QMARK
						+ AND + RESP_QID + EQUALS + QMARK,
					studentId,
					hwId,
					evalResult.qId);
			String qry = toString(
					DELETE_FROM + RESPONSES,
					whereSql_vals.getSql(),
					AND + RESP_TRIES + EQUALS + QMARK);
			SQLWithQMarks sql_vals = new SQLWithQMarks(qry, whereSql_vals);
			sql_vals.addValue(evalResult.tries);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
			qry = toString(
					UPDATE + RESPONSES
					+ SET + RESP_LATEST + EQUALS + N,
					whereSql_vals.getSql());
			sql_vals = new SQLWithQMarks(qry, whereSql_vals);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
			final SQLWithQMarks modGradeSql_vals =
					getModGradeSQL(hwId, studentId, evalResult);
			final String dateStr = dateToString(evalResult.timeOfResponse);
			final String[] fields = new String[] {
					RESP_HWID, 
					RESP_STUDENT, 
					RESP_QID, 
					RESP_TRIES, 
					RESP_WHEN,
					RESP_IP,
					RESP_STATUS,
					RESP_LATEST,
					RESP_COMMENT, // CLOB field
					RESP_LASTRESPONSE,
					RESP_GRADE, 
					RESP_MODGRADE}; // CLOB field
			qry = toString(
					INSERT_INTO + RESPONSES, parens(fields),
					valuesJoin(
						getQMarks(fields.length - 1),
						evalResult.grade == 0 ? QMARK 
							: modGradeSql_vals.getSql())); // contains several QMARKs
			sql_vals = new SQLWithQMarks(qry, 
					hwId,
					studentId,
					evalResult.qId,
					evalResult.tries,
					dateStr,
					evalResult.ipAddr,
					evalResult.status,
					'Y',
					evalResult.comment, 
					evalResult.lastResponse,
					evalResult.grade,
					evalResult.grade == 0 ? 0 : modGradeSql_vals.getValues());
			// don't just print sql_vals or log will become infinitely long due
			// to responses
			alwaysPrint(SELF, qry, "; ",
					hwId, ", ",
					studentId, ", ",
					evalResult.qId, ", ",
					evalResult.tries, ", ",
					dateStr, ", ",
					evalResult.ipAddr, ", ",
					evalResult.status, ", ",
					"Y, ",
					evalResult.comment, ", ", 
					"[response], ",
					evalResult.grade, ", ",
					evalResult.grade == 0 ? '0' : modGradeSql_vals.getValues());
			tryUpdate(con, sql_vals);
			con.commit();
			ResponseRead.getModifiedGrade(con, hwId, studentId, evalResult);
		} catch (SQLException e) {
			// Most likely an attempt to insert where we should be just
			// updating an existing entry.  This situation arises if there is
			// an existing entry that is somehow malformed.
			e.printStackTrace();
			rollbackConnection(con);
			final StringBuilder errBld = getBuilder(
					"ACE could not add record for ", studentId, 
					", assignment ID ", hwId, ", question ID ", evalResult.qId,
					COMMA, evalResult.tries, 
					" tries, because record already exists with response:\n");
			final EvalResult[] dbResults = ResponseRead.getResults(con, 
					studentId, hwId, evalResult.qId);
			for (final EvalResult dbResult : dbResults) {
				if (dbResult.tries == evalResult.tries) {
					errBld.append(dbResult.lastResponse);
					break;
				} // if DB result has same number of tries
			} // for each result to this Q
			final String err = errBld.toString();
			alwaysPrint(SELF, err);
			throw new DBException(err);
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		}
	} // addResult(Connection, String, int, int, EvalResult)

	/** Clears the response of a student to a question. Called by 
	 * HWSession.deleteResult().
	 * @param	studentId	student's login ID
	 * @param	hwId	unique ID of the assignment
	 * @param	qId	unique ID of the question
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void deleteResult(String studentId, int hwId, int qId) 
			throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection(); 
			final List<Integer> qIds = new ArrayList<Integer>();
			qIds.add(Integer.valueOf(qId));
			deleteResults(con, hwId, qIds, studentId, 
					DEFAULT_FLAGS[PRESERVE_SUBSTNS]);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (DBException e) {
			e.printStackTrace();
			throw e;
		} finally {
			closeConnection(con);
		}
	} // deleteResult(String, int, int)

	/** Overwrites the response of a student to a question with a new response.  
	 * Called by HWSession.submitResponse().
	 * @param	studentId	student's login ID
	 * @param	hwId	unique ID of the assignment
	 * @param	qNum	1-based number of the question within the assignment
	 * @param	evalResult	information to be stored; NOT null
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setResult(String studentId, int hwId, int qNum, 
			EvalResult evalResult) throws DBException {
		setResult(studentId, hwId, qNum, evalResult.qId, evalResult, 
				DEFAULT_FLAGS);
	} // setResult(String, int, int, EvalResult)

	/** Overwrites the response of a student to a question with a new response.  
	 * Called by HWSession.setResult() (manual alteration of student grade) and
	 * above.
	 * @param	studentId	student's login ID
	 * @param	hwId	unique ID of the assignment
	 * @param	qNum	1-based number of the question within the assignment, or
	 * 0 if the question number is not known (the case when called by 
	 * HWSession.setResult())
	 * @param	qId	unique ID of the question
	 * @param	evalResult	information to be stored; MAY be null
	 * @param	flags	whether to rewrite the last response and its
	 * time, whether the record being modified is the most recent response, 
	 * whether the grade is being changed, whether to preserve the R groups 
	 * assigned to this student
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setResult(String studentId, int hwId, int qNum, int qId,
			EvalResult evalResult, boolean[] flags) throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			if (evalResult == null) {
				final List<Integer> qIds = Arrays.asList(Integer.valueOf(qId));
				deleteResults(con, hwId, qIds, studentId, 
						flags[PRESERVE_SUBSTNS]);
				con.commit();
			} else {
				setResult(con, studentId, hwId, qNum, evalResult, flags);
				con.commit();
				ResponseRead.getModifiedGrade(con, hwId, studentId, evalResult);
			} // if evalResult is null
		} catch (SQLException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // setResult(String, int, int, int, EvalResult, boolean[])

	/** Overwrites the evaluation results of a set of students' responses to a
	 * question with new results from a forced regrade.  Less-than-recent
	 * responses whose grades have been changed to fully correct will be noted
	 * as most recent, and more recent responses will be removed.
	 * @param	hwId	unique ID of the assignment
	 * @param	evalResultsByStudentIds	map of list of results to be stored 
	 * keyed by students' login IDs
	 * @return	map of most recent results with modified grades stored, keyed by
	 * student IDs
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static Map<String, EvalResult> setRegradedResults(int hwId, 
			Map<String, ArrayList<EvalResult>> evalResultsByStudentIds) 
			throws DBException {
		final String SELF = "ResponseWrite.setRegradedResults: ";
		final Map<String, EvalResult> mostRecentResultsByStudentIds =
				new HashMap<String, EvalResult>();
		final boolean[] flags = Utils.getCopy(DEFAULT_FLAGS);
		flags[EDIT_MOST_RECENT] = false;
		flags[REWRITE_RESP] = false;
		List<String> studentIds = 
				new ArrayList<String>(evalResultsByStudentIds.keySet());
		Connection con = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			for (final String studentId : studentIds) {
				final List<EvalResult> evalResults = 
						evalResultsByStudentIds.get(studentId);
				for (final EvalResult evalResult : evalResults) {
					if (evalResult.grade == 1.0 && !evalResult.mostRecent) {
						debugPrint(SELF + "writing correct response of "
								+ "student ", studentId, " that is not most "
								+ "recent; will delete responses to same Q ", 
								evalResult.qId, " with tries > ", 
								evalResult.tries);
						evalResult.mostRecent = true;
						setResult(con, studentId, hwId, NO_QNUM, evalResult, 
								flags);
					} else {
						debugPrint(SELF + "updating record of student ", 
								studentId, evalResult.mostRecent
									? " most recent" : " less-than-recent",
								" response ", evalResult.tries, 
								" to Q ", evalResult.qId, " to grade ", 
								evalResult.grade, 
								"; not deleting any other responses.");
						updateResult(con, studentId, hwId, NO_QNUM, evalResult,
								evalResult.tries, flags[REWRITE_RESP]);
					} // if response is correct but is not most recent
					if (evalResult.mostRecent) {
						mostRecentResultsByStudentIds.put(studentId, 
								evalResult);
					} // if response is most recent
				} // for each result
			} // for each student
			// when a question is deleted from an assignment, students who
			// already have that assignment open and then work that problem may
			// get a null modified grade; change those nulls to grades
			final Map<String, EvalResult[]> nullModGradesResults = 
					ResponseRead.getOneHWNullModGrades(con, hwId); 
			studentIds = new ArrayList<String>(nullModGradesResults.keySet());
			for (final String studentId : studentIds) {
				final EvalResult[] evalResults = 
						nullModGradesResults.get(studentId);
				for (final EvalResult evalResult : evalResults) {
					debugPrint(SELF + "recalculating null modified grade of "
							+ "student ", studentId, " most recent response ", 
							evalResult.tries, " to Q ", evalResult.qId, 
							" with grade ", evalResult.grade);
					updateResult(con, studentId, hwId, NO_QNUM, evalResult, 
							evalResult.tries, flags[REWRITE_RESP]);
					mostRecentResultsByStudentIds.put(studentId, evalResult);
				} // for each result
			} // for each student
			con.commit();
			ResponseRead.getModifiedGrades(con, hwId, 
					mostRecentResultsByStudentIds);
		} catch (DBException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw e;
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
		return mostRecentResultsByStudentIds;
	} // setRegradedResults(int, Map<String, ArrayList<EvalResult>>)

	/** Writes the response of a student to a question with a new response when
	 * there is a previous response to this question.  
	 * Responses with more tries than the current response will be removed.
	 * <p>It is the caller's job to disable autoCommit before and to commit 
	 * the transaction after; this activity requires several database operations.
	 * </p>
	 * @param	con	database connection
	 * @param	studentId	student's login ID
	 * @param	hwId	unique ID of the assignment
	 * @param	qNum	1-based number of the question within the assignment, 
	 * or 0 if unknown (when called from RegradeSession)
	 * @param	evalResult	information to be stored; NOT null
	 * @param	flags	whether to rewrite the last response and its time, 
	 * whether the record being modified is the most recent response 
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void setResult(Connection con, String studentId, int hwId, 
			int qNum, EvalResult evalResult, boolean[] flags)
			throws DBException, SQLException {
		final String SELF = "ResponseWrite.setResult: ";
		debugPrint(SELF + "studentId = ", studentId, ", hwId = ", hwId, 
				", qId = ", evalResult.qId, ", qNum = ", qNum);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			if (evalResult.timeOfResponse == null) {
				Utils.alwaysPrint("ResponseWrite.setResult: "
						+ "timeOfResponse is null, setting to now.");
				evalResult.timeOfResponse = new Date();
			}
			// get the number of tries of most recent response
			String qry = toString(
					SELECT, max(RESP_TRIES),
					FROM + RESPONSES
					+ WHERE + RESP_STUDENT + EQUALS + QMARK
					+ AND + RESP_HWID + EQUALS + QMARK
					+ AND + RESP_QID + EQUALS + QMARK);
			SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
					studentId,
					hwId,
					evalResult.qId);
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			int maxTries = 0;
			if (rs.next()) maxTries = rs.getInt(1);
			final boolean newTriesNotMax = evalResult.tries < maxTries;
			debugPrint(SELF + "evalResult.tries = ", evalResult.tries, 
					", maxTries = ", maxTries, ", newTriesNotMax = ",
					newTriesNotMax, ", mostRecent = ", flags[EDIT_MOST_RECENT],
					", rewriteResponse = ", flags[REWRITE_RESP]);
			if (newTriesNotMax) {
				// either reducing number of tries of most recent response, 
				// or making earlier response the most recent
				qry = toString(
						DELETE_FROM + RESPONSES
						+ WHERE + RESP_STUDENT + EQUALS + QMARK
						+ AND + RESP_HWID + EQUALS + QMARK
						+ AND + RESP_QID + EQUALS + QMARK,
						flags[EDIT_MOST_RECENT]
							// number of tries of most recent record is being 
							// reduced; remove records with more than or same 
							// number of tries as new evalResult, except for 
							// most recent record
						? AND + RESP_TRIES + NOT_LESS_THAN + QMARK
							+ AND + RESP_TRIES + LESS_THAN + QMARK
							// earlier record is being made most recent; remove
							// records with more tries than new evalResult
 						: AND + RESP_TRIES + MORE_THAN + QMARK);
				debugPrint(flags[EDIT_MOST_RECENT] 
						? SELF + "changing most recent record; removing "
							+ "records with tries >= new evalResult, except "
							+ "for record with maxTries"
						: SELF + "making earlier response the most recent; "
							+ "removing records with more tries than new "
							+ "evalResult");
				sql_vals = new SQLWithQMarks(qry,
						studentId,
						hwId,
						evalResult.qId,
						evalResult.tries);
				if (flags[EDIT_MOST_RECENT]) {
					sql_vals.addValue(maxTries);
				} // if edit most recent
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} // if the new record has fewer tries than an existing record
			// change most recent record, including number of tries
			final int triesToModify = (newTriesNotMax && !flags[EDIT_MOST_RECENT] 
					? evalResult.tries : maxTries);
			debugPrint(SELF + "modifying record of response with ", 
					triesToModify, " tries");
			evalResult.mostRecent = true;
			updateResult(con, studentId, hwId, qNum, evalResult, 
					triesToModify, flags[REWRITE_RESP]);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		} // if evalResult
	} // setResult(Connection, String, int, int, EvalResult, boolean[])

	/** Writes the response of a student to a question with a new response when
	 * there is a previous response to this question.  
	 * It is the caller's job to disable autoCommit before and to commit 
	 * the transaction after.
	 * @param	con	database connection
	 * @param	studentId	student's login ID
	 * @param	hwId	unique ID of the assignment
	 * @param	qNum	1-based number of the question within the assignment, or
	 * 0 if unknown (when called from RegradeSession)
	 * @param	evalResult	information to be stored; NOT null
	 * @param	triesToModify	number of tries of record to be modified
	 * @param	rewriteResp	whether to rewrite the last response and its time
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void updateResult(Connection con, String studentId, int hwId, 
			int qNum, EvalResult evalResult, int triesToModify, boolean rewriteResp) 
			throws SQLException {
		final String SELF = "ResponseWrite.updateResult: ";
		final SQLWithQMarks modGradeSql_vals =
				getModGradeSQL(hwId, studentId, evalResult);
		final List<Object> updateData = new ArrayList<Object>(Arrays.asList(
				RESP_GRADE, QMARK,
				RESP_MODGRADE, evalResult.grade == 0 ? QMARK
					: modGradeSql_vals.getSql(), // contains several QMARKs
				RESP_STATUS, QMARK, 
				RESP_LATEST, QMARK,
				RESP_COMMENT, QMARK, // CLOB field
				RESP_TRIES, QMARK));
		if (rewriteResp) updateData.addAll(Arrays.asList(
				RESP_WHEN, QMARK,
				RESP_IP, QMARK,
				RESP_LASTRESPONSE, QMARK)); // CLOB field
		final String qry = toString(
				UPDATE + RESPONSES + SET, equalsJoin(updateData),
				WHERE + RESP_HWID + EQUALS + QMARK
				+ AND + RESP_STUDENT + EQUALS + QMARK
				+ AND + RESP_QID + EQUALS + QMARK
				+ AND + RESP_TRIES + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				evalResult.grade,
				evalResult.grade == 0 ? 0 : modGradeSql_vals.getValues(),
				evalResult.status, 
				evalResult.mostRecent ? 'Y' : 'N',
				evalResult.comment, 
				evalResult.tries);
		if (rewriteResp) sql_vals.addValues(
				dateToString(evalResult.timeOfResponse),
				evalResult.ipAddr,
				evalResult.lastResponse);
		sql_vals.addValues(
				hwId,
				studentId,
				evalResult.qId,
				triesToModify);
		// don't just print sql_vals or log will become infinitely long due
		// to responses
		alwaysPrint(SELF, qry, "; ",
				evalResult.grade, ", ",
				evalResult.grade == 0 ? '0' : modGradeSql_vals.getValues(), ", ",
				evalResult.status, ", ", 
				evalResult.mostRecent ? 'Y' : 'N', ", ",
				evalResult.comment, ", ", 
				evalResult.tries, ", ",
				rewriteResp 
					? toString(
						dateToString(evalResult.timeOfResponse), ", ",
						evalResult.ipAddr, ", ",
						"[response], ") 
					: "", 
				hwId, ", ",
				studentId, ", ",
				evalResult.qId, ", ",
				triesToModify);
		tryUpdate(con, sql_vals);
	} // updateResult(Connection, String, int, int, EvalResult, boolean)

	/** Deletes all the responses to questions within an assignment.
	 * Called by HWWrite after removing questions from an assignment.
	 * @param	con	database connection
	 * @param	hwId	unique ID of the assignment
	 * @param	qIds	unique IDs of the questions whose responses to remove
	 * @throws	DBException	if there's a problem writing to the database
	 */
	static void deleteResults(Connection con, int hwId, List<Integer> qIds) 
			throws DBException {
		deleteResults(con, hwId, qIds, ALL_STUDENTS, 
				DEFAULT_FLAGS[PRESERVE_SUBSTNS]);
	} // deleteResults(Connection, int, List<Integer>)

	/** Deletes all the responses of a student to questions within an assignment.
	 * Called by HWWrite after choosing new questions for instantiation of an 
	 * assignment.
	 * @param	con	database connection
	 * @param	hwId	unique ID of the assignment
	 * @param	qIds	unique IDs of the questions whose responses to remove
	 * @param	studentId	student's login ID
	 * @throws	DBException	if there's a problem writing to the database
	 */
	static void deleteResults(Connection con, int hwId, int[] qIds,
			String studentId) throws DBException {
		deleteResults(con, hwId, Utils.intArrayToList(qIds), studentId, 
				DEFAULT_FLAGS[PRESERVE_SUBSTNS]);
	} // deleteResults(Connection, int, List<Integer>, String)

	/** Deletes all the responses to questions within an assignment.
	 * Called by setResult() and deleteResults().
	 * @param	con	database connection
	 * @param	hwId	unique ID of the assignment
	 * @param	qIds	unique IDs of the questions whose responses to remove
	 * @param	studentId	student's login ID, or null for all students
	 * @param	preserveSubstns	when true, preserve the R groups and variable
	 * values (default is false)
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private static void deleteResults(Connection con, int hwId, 
			List<Integer> qIds, String studentId, boolean preserveSubstns) 
			throws DBException {
		final String SELF = "ResponseWrite.deleteResults: ";
		if (Utils.isEmpty(qIds)) return;
		try {
			final List<String[]> fieldSets = new ArrayList<String[]>();
			if (!preserveSubstns) fieldSets.add(new String[] {
					RESP_SUBSTNS, 
					RESP_SUBS_HWID, 
					RESP_SUBS_QID, 
					RESP_SUBS_STUDENT});
			fieldSets.add(new String[] {
					RESPONSES, 
					RESP_HWID, 
					RESP_QID, 
					RESP_STUDENT});
			final boolean oneStudent = studentId != ALL_STUDENTS;
			for (final String[] fields : fieldSets) {
				final StringBuilder qryBld = getBuilder(
						DELETE_FROM, fields[0],
						WHERE, fields[1], EQUALS + QMARK
						+ AND, fields[2], IN, parensQMarks(qIds));
				final SQLWithQMarks sql_vals = new SQLWithQMarks("",
						hwId,
						qIds);
				if (oneStudent) {
					appendTo(qryBld, AND, fields[3], EQUALS + QMARK);
					sql_vals.addValue(studentId);
				} // if one student
				sql_vals.setSql(qryBld);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} // for each table
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		}
	} // deleteResults(Connection, int, List<Integer>, String, boolean)

	/** Removes old responses from the database.
	 * @param	y_m_d	int array of years, months, days
	 * @param	instnId	the ID of the institution, or all of them, whose 
	 * students' responses will be removed
	 * @return	the number of rows deleted
	 * @throws	DBException	if the database can't be read
	 */
	public static int removeOldResponses(int[] y_m_d, int instnId) 
			throws DBException {
		final String SELF = "ResponseWrite.removeOldResponses: ";
		final int year = y_m_d[YEAR];
		final int month = y_m_d[MONTH] - 1; // because month in Calendar is 0-based
		final int day = y_m_d[DAY];
		final Calendar calendar = Calendar.getInstance();
		if (year > 1900) {
			debugPrint(SELF, y_m_d, " represents a specific date.");
			calendar.set(Calendar.YEAR, year);
			calendar.set(Calendar.MONTH, month);
			calendar.set(Calendar.DAY_OF_MONTH, day);
		} else {
			debugPrint(SELF, y_m_d, " represents a time interval before now.");
			calendar.add(Calendar.YEAR, -year);
			calendar.add(Calendar.MONTH, -month);
			calendar.add(Calendar.DAY_OF_MONTH, -day);
		} // if year
		final SQLWithQMarks sql_vals = new SQLWithQMarks(
				DELETE_FROM + RESPONSES
				+ WHERE + RESP_WHEN + LESS_THAN + QMARK);
		final String formattedDate = dateToString(calendar.getTime());
		sql_vals.addValue(formattedDate);
		if (instnId != ANY_INSTITUTION) {
			sql_vals.addToSql(
				AND + RESP_STUDENT + IN, parens(
					SELECT + USER_ID + FROM + USERS
					+ WHERE + USER_SCHOOLID + EQUALS + QMARK));
			sql_vals.addValue(instnId);
		} // if not any institution
		debugPrint(SELF, sql_vals);
		Connection con = null;
		int count = 0; 
		try {
			con = getPoolConnection(); 
			count = tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "caught SQLException.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
		debugPrint(SELF, count, " rows affected.");
		return count;
   	} // removeOldResponses(int[], int)

/* *********** Modified grade calculation methods *************/

	/** Gets the SQL, and values to substitute for question marks, that 
	 * calculates the factor by which to multiply the grade of a single 
	 * response, giving a modified grade that is stored in the responses table.
	 * @param	hwId	ID of the assignment
	 * @param	studentId	login ID of student
	 * @param	evalResult	evaluation result containing the question ID, number
	 * of tries, grade, and response time
	 * @return	SQL to calculate the modified grade, and values to substitute
	 * for question marks
	 */
	private static SQLWithQMarks getModGradeSQL(int hwId, String studentId, 
			EvalResult evalResult) {
		final String qry = toString(QMARK + TIMES + GRADE_FACTOR, 
				parensQMarks(5));
		return new SQLWithQMarks(qry,
				evalResult.grade,
				hwId, 
				studentId,
				evalResult.qId,
				evalResult.tries,
				dateToString(evalResult.timeOfResponse)); 
	} // getModGradeSQL(int, String, EvalResult)

	/** Calculates and stores in the EvalResult the modified grade of an 
	 * instructor's response to a question.  Called from HWSession.submitResponse().
	 * Subjects the data to the database's instructor_grade_factor_v1(), which
	 * calculates the factor by which to multiply the grade of the instructor's 
	 * response.  Use this query to get the SQL of the function:
	 * <pre>select text from user_source where name = 'INSTRUCTOR_GRADE_FACTOR_V2'
	 * order by line;
	 * </pre>
	 * @param	hwId	ID of the assignment whose grading parameters have been
	 * modified
	 * @param	qId	ID of the question being evaluated
	 * @param	evalResult	result of evaluation of response
	 */
	public static void calculateModifiedGrade(int hwId, int qId, 
			EvalResult evalResult) {
		final String SELF = "ResponseWrite.calculateModifiedGrade: ";
		evalResult.modGrade = evalResult.grade;
		if (evalResult.grade == 0) return;
		final String qry = toString(
				SELECT + INSTRUCTOR_GRADE_FACTOR, parensQMarks(4),
				AS + SRCH_RESULT + FROM + DUMMY_TABLE);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				hwId, 
				qId,
				evalResult.tries,
				dateToString(evalResult.timeOfResponse));
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection(); 
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) evalResult.modGrade *= rs.getDouble(SRCH_RESULT);
			debugPrint(SELF + "original grade = ", evalResult.grade,
					", modified grade = ", evalResult.modGrade);
		} catch (SQLException e) {
			debugPrint(SELF + "exception thrown; setting modGrade to grade.");
			e.printStackTrace();
		} finally {
			closeConnection(con, stmt, rs);
		} // try
	} // calculateModifiedGrade(int, int, EvalResult)

	/** Sets modified grades of all students' responses to an assignment
	 * after the instructor changes the assignment's grading parameters.
	 * Called from HWWrite.setHWPage1 and setHWPage2().
	 * @param	con	database connection
	 * @param	hwId	ID of the assignment whose grading parameters have been
	 * modified
	 * @throws	DBException	if there's a problem writing to the database
	 */
	static void recalculateModifiedGrades(Connection con, int hwId) 
			throws DBException {
		final String ALL_STUDENTS = null;
		recalculateModifiedGrades(con, hwId, ALL_STUDENTS);
	} // recalculateModifiedGrades(Connection, int)

	/** Sets modified grades of one student's or all students' responses to an 
	 * assignment after the instructor changes one student's extension or the
	 * assignment's grading parameters.  Called from above and below.
	 * @param	con	database connection
	 * @param	hwId	ID of the assignment whose grading parameters have been
	 * modified
	 * @param	studentId	student whose grades to recalculate
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private static void recalculateModifiedGrades(Connection con, int hwId, 
			String studentId) throws DBException {
		final String SELF = "ResponseWrite.recalculateModifiedGrades: ";
		final String qry = toString(braces(getBuilder(
				CALL,
				studentId != null 
					? getBuilder(RECALC_1_STUDENT_MOD_GRADES, parens(joinAll(
						QMARK,
						QMARK)))
					: getBuilder(RECALC_MOD_GRADES, parens(QMARK)))));
		debugPrint(SELF, qry);
		CallableStatement cstmt = null;
		try {
			cstmt = con.prepareCall(qry);
			cstmt.setInt(1, hwId);
			if (studentId != null) cstmt.setString(2, studentId);
			cstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(null, cstmt, null);
		} // try
	} // recalculateModifiedGrades(Connection, int, String)

	/** Sets modified grades of one or some students' responses after the 
	 * instructor changes the grading parameters or an extension.  Called 
	 * from HWWrite.setHWPage1 and setHWPage2().  We can't
	 * convert this code into a database function because it takes a variable
	 * number of parameters for studentIds.  SQL:
	 * <pre>
	 * update responses_v6 set modified_grade = 
	 * (select srch_result4.grade 
	 * 		* srch_result0.factor0
	 * 		* srch_result1.factor1 
	 * from (
	 * 		select limit_min, limit_max, factor as factor0
	 * 		from hwset_grading_params_v1
	 * 		where param_type = 'A' and hw_id = [hwId]
	 * 		union all select 0, 9999999999, 1 from dual 
	 * 		where [hwId] not in (
	 * 			select hw_id from hwset_grading_params_v1 where param_type = 'A'
	 * 		)
	 * ) srch_result0, (
	 * 		select limit_min, limit_max, factor as factor1
	 * 		from hwset_grading_params_v1 
	 * 		where param_type = 'T' and hw_id = [hwId]
	 * 		union all select -9999999999, 0, 1 from dual 
	 * 		where [hwId] in (
	 * 			select hw_id from hwset_grading_params_v1 where param_type = 'T'
	 * 		)
	 * 		union all select -9999999999, 9999999999, 1 from dual 
	 * 		where [hwId] not in (
	 * 			select hw_id from hwset_grading_params_v1 where param_type = 'T'
	 * 		)
	 * ) srch_result1, (
	 * 		select to_date(date_due, 'YYYY/MM/DD HH24:MI:SS') as date_due, 
	 * 			decode(bitand(flags, 4), 0, 1, 1 / (60 * 24)) as in_days 
	 * 		from hwsets_v5 where hw_id = [hwId]
	 * ) srch_result2, (
	 * 		select student_id, extension 
	 * 		from hwset_extensions_v1 where hw_id = [hwId]
	 * 		and student_id in ([studentIds])
	 * 		union all select unique student_id, 0 from responses_v6 
	 * 		where hw_id = [hwId] and student_id not in (
	 * 			select student_id from hwset_extensions_v1 where hw_id = [hwId]
	 * 		)
	 * ) srch_result3, (
	 * 		select student_id, 
	 * 			responses_v6.pb_id, 
	 * 			tries, 
	 * 			grade * points as grade, 
	 * 			to_date(date_time, 'YYYY/MM/DD HH24:MI:SS') as date_time
	 * 		from responses_v6 
	 * 		join hwset_qs_v2
	 * 		on responses_v6.hw_id = hwset_qs_v2.hw_id
	 * 		and responses_v6.pb_id = hwset_qs_v2.pb_id
	 * 		where grade != 0
	 * 		and responses_v6.hw_id = [hwId]
	 * 		and responses_v6.student_id in ([studentIds])
	 * ) srch_result4
	 * where srch_result4.tries &gt; srch_result0.limit_min
	 * and srch_result4.tries &lt;= srch_result0.limit_max
	 * and srch_result4.date_time 
	 * 		- srch_result2.date_due 
	 * 		- srch_result3.extension * srch_result2.in_days 
	 * 		&gt; srch_result1.limit_min * srch_result2.in_days
	 * and srch_result4.date_time 
	 * 		- srch_result2.date_due 
	 * 		- srch_result3.extension * srch_result2.in_days 
	 * 		&lt;= srch_result1.limit_max * srch_result2.in_days 
	 * and srch_result4.student_id = srch_result3.student_id
	 * and responses_v6.student_id = srch_result4.student_id
	 * and responses_v6.pb_id = srch_result4.pb_id
	 * and responses_v6.tries = srch_result4.tries
	 * )
	 * where hw_id = [hwId]
	 * and grade != 0
	 * and responses_v6.student_id in ([studentIds])
	 * </pre>
	 * @param	con	database connection
	 * @param	hwId	ID of the assignment whose grading parameters have been
	 * modified
	 * @param	studentIds	list of students whose grades to recalculate
	 * @throws	DBException	if there's a problem writing to the database
	 */
	static void recalculateModifiedGrades(Connection con, int hwId,
			List<String> studentIds) throws DBException {
		final String SELF = "ResponseWrite.recalculateModifiedGrades: ";
		if (Utils.isEmpty(studentIds)) return;
		else if (studentIds.size() == 1) {
			recalculateModifiedGrades(con, hwId, studentIds.get(0));
			return;
		} // if fewer than two studentIds are specified
		// get the SQL that retrieves the appropriate parameters required to
		// calculate the factors
		// already have ATTEMPT = 0, TIME = 1
		final int DUE = 2;
		final int EXT = 3;
		final int RESP = 4;
		final StringBuilder[] subqrySQLs = new StringBuilder[5];
		final Object[][] subqryValues = new Object[5][0];
		final StringBuilder[] subqryNames = new StringBuilder[5];
		for (int qryNum = 0; qryNum < subqrySQLs.length; qryNum++) {
			final SQLWithQMarks subqry_vals = 
					(qryNum == ATTEMPT ? getTriesSQL(hwId)
					: qryNum == TIME ? getSubmitTimeSQL(hwId)
					: qryNum == DUE ? getDueDateAndIsExamSQL(hwId)
					: qryNum == EXT ? getExtensionSQL(hwId, studentIds)
					: getResponseSQL(hwId, studentIds));
			subqrySQLs[qryNum] = getBuilder(subqry_vals.getSql());
			subqryValues[qryNum] = subqry_vals.getValues();
			subqryNames[qryNum] = getBuilder(SRCH_RESULT, qryNum);
			parens(subqrySQLs[qryNum]);
			appendTo(subqrySQLs[qryNum], ' ', subqryNames[qryNum]);
			subqryNames[qryNum].append(DOT);
		} // for each subqry
		final String qry = toString(
				UPDATE + RESPONSES
				+ SET + RESP_MODGRADE + EQUALS, parensBuild(
					SELECT, subqryNames[RESP], RESP_GRADE // grade * points_per_Q
						+ TIMES, subqryNames[ATTEMPT], GRADING_FACTOR + ATTEMPT
						+ TIMES, subqryNames[TIME], GRADING_FACTOR + TIME
 					+ FROM, join(subqrySQLs),
					WHERE, subqryNames[RESP], RESP_TRIES 
						+ MORE_THAN, subqryNames[ATTEMPT], GRADING_MIN
					+ AND, subqryNames[RESP], RESP_TRIES 
						+ NOT_MORE_THAN, subqryNames[ATTEMPT], GRADING_MAX
					+ AND, subqryNames[RESP], RESP_WHEN 
							+ MINUS, subqryNames[DUE], HW_DUE 
							+ MINUS, subqryNames[EXT], EXT_EXTENSION 
							+ TIMES, subqryNames[DUE], IN_DAYS 
						+ MORE_THAN, subqryNames[TIME], GRADING_MIN 
							+ TIMES, subqryNames[DUE], IN_DAYS
					+ AND, subqryNames[RESP], RESP_WHEN 
							+ MINUS, subqryNames[DUE], HW_DUE 
							+ MINUS, subqryNames[EXT], EXT_EXTENSION 
								+ TIMES, subqryNames[DUE], IN_DAYS 
						+ NOT_MORE_THAN, subqryNames[TIME], GRADING_MAX 
							+ TIMES, subqryNames[DUE], IN_DAYS
					+ AND, subqryNames[RESP], RESP_STUDENT 
						+ EQUALS, subqryNames[EXT], EXT_STUDENT
					+ AND + RESPONSES + DOT + RESP_STUDENT 
						+ EQUALS, subqryNames[RESP], RESP_STUDENT 
					+ AND + RESPONSES + DOT + RESP_QID 
						+ EQUALS, subqryNames[RESP], RESP_QID 
					+ AND + RESPONSES + DOT + RESP_TRIES 
						+ EQUALS, subqryNames[RESP], RESP_TRIES),
				WHERE + RESP_HWID + EQUALS + QMARK
				+ AND + RESP_GRADE + IS_NOT_ZERO
				+ AND + RESP_STUDENT + IN, parensQMarks(studentIds));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				subqryValues,
				hwId,
				studentIds);
		try {
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} // try
	} // recalculateModifiedGrades(Connection, int, List<String>)

	/** Gets the SQL to get the limits and factor for the number of tries, plus
	 * values to substitute for the question marks in the SQL. 
	 * Returns:
	 * <pre>
	 * select limit_min, limit_max, factor as factor0
	 * from hwset_grading_params_v1 where param_type = 'A' and hw_id = [hwId]
	 * union all select 0, 9999999999, 1 from dual 
	 * where [hwId] not in (
	 * 		select hw_id from hwset_grading_params_v1 where param_type = 'A'
	 * )
	 * </pre>
	 * @param	hwId	ID of the assignment
	 * @return	SQL and values to get the factor for the number of tries
	 */
	private static SQLWithQMarks getTriesSQL(int hwId) {
		return getLimitsSQL(hwId, ATTEMPT);
	} // getTriesSQL(int)

	/** Gets the SQL to get the limits and factor for the submission time,
	 * plus values to substitute for the question marks in the SQL.
	 * Returns:
	 * <pre>
	 * select limit_min, limit_max, factor as factor1
	 * from hwset_grading_params_v1 where param_type = 'T' and hw_id = [hwId]
	 * union all select -9999999999, 0, 1 from dual 
	 * where [hwId] in (
	 * 		select hw_id from hwset_grading_params_v1 where param_type = 'T'
	 * )
	 * union all select -9999999999, 9999999999, 1 from dual 
	 * where [hwId] not in (
	 * 		select hw_id from hwset_grading_params_v1 where param_type = 'T'
	 * )
	 * </pre>
	 * @param	hwId	ID of the assignment
	 * @return	SQL and values to get the factor for the submission time
	 */
	private static SQLWithQMarks getSubmitTimeSQL(int hwId) {
		return getLimitsSQL(hwId, TIME);
	} // getSubmitTimeSQL(int)

	/** Gets the SQL to get the factor for the number of tries or the 
	 * submission time, plus values to substitute for question marks.
	 * @param	hwId	ID of the assignment
	 * @param	paramType	number of tries or submission time
	 * @return	SQL and values to get the factor for the number of tries or the 
	 * submission time
	 */
	private static SQLWithQMarks getLimitsSQL(int hwId, int paramType) {
		final String NO_MINIMUM = toString('-', NO_MAXIMUM);
		final String quotedType = quotes(DB_PARAM_TYPES[paramType]);
		final StringBuilder hasParamsBld = parensBuild(
				SELECT + GRADING_HWID
				+ FROM + GRADING_PARAMS
				+ WHERE + GRADING_TYPE + EQUALS, quotedType);
		final String subqry = toString(
				SELECT, joinAll(
					GRADING_MIN,
					GRADING_MAX,
					getBuilder(
						GRADING_FACTOR + AS + GRADING_FACTOR, paramType)),
				FROM + GRADING_PARAMS
				+ WHERE + GRADING_TYPE + EQUALS, quotedType,
				AND + GRADING_HWID + EQUALS + QMARK,
				paramType != TIME ? "" : getBuilder(
					UNION_ALL + SELECT, joinAll(
						NO_MINIMUM,
						'0',
						'1'),
					FROM + DUMMY_TABLE 
					+ WHERE + QMARK + IN, hasParamsBld),
				UNION_ALL + SELECT, joinAll(
					paramType == ATTEMPT ? '0' : NO_MINIMUM,
					NO_MAXIMUM,
					'1'),
				FROM + DUMMY_TABLE 
				+ WHERE + QMARK + NOT + IN, hasParamsBld);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(subqry, 
				hwId);
		if (paramType == TIME) sql_vals.addValue(hwId);
		sql_vals.addValue(hwId);
		return sql_vals;
	} // getLimitsSQL(int, int)

	/** Gets the SQL to get the assignment due date and a factor to convert days
	 * into minutes if the assignment is an exam (or 1 if it is not), plus
	 * values to substitute for question marks in the SQL. Returns:
	 * <pre>
	 * select to_date(date_due, 'YYYY/MM/DD HH24:MI:SS') as date_due, 
	 * 		decode(bitand(flags, 4), 0, 1, 1 / (60 * 24)) as in_days 
	 * from hwsets_v5 where hw_id = ?
	 * </pre>
	 * @param	hwId	ID of the assignment
	 * @return	SQL and values to get the assignment due date
	 */
	private static SQLWithQMarks getDueDateAndIsExamSQL(int hwId) {
		final String subqry = toString(
				SELECT, joinAll(
					getBuilder(
						toDate(HW_DUE, quotes(DB_DATE_FORMAT[ORACLE])),
						AS + HW_DUE), 
					getBuilder(
						decode(
							bitand(HW_FLAGS, EXAM_ASSGT),
							'0', 
							'1', 
							1.0 / (MINS_IN_HR * HRS_IN_DAY)),
						AS + IN_DAYS)),
				FROM + HWSETS
				+ WHERE + HW_ID + EQUALS + QMARK);
		return new SQLWithQMarks(subqry, 
				hwId);
	} // getDueDateAndIsExamSQL(int)

	/** Gets the SQL to get the students' extensions, plus values to substitute
	 * for question marks in the SQL. Returns:
	 * <pre>
	 * select student_id, extension 
	 * from hwset_extensions_v1 where hw_id = ?
	 * and student_id in (?, ?, ..., ?)
	 * union all select unique student_id, 0 from responses_v6 
	 * where hw_id = ? and student_id not in (
	 * 		select student_id from hwset_extensions_v1 where hw_id = ?
	 * )
	 * </pre>
	 * @param	hwId	ID of the assignment
	 * @param	studentIds	login IDs of students whose grades should be 
	 * recalculated
	 * @return	SQL and values to get the students' extensions
	 */
	private static SQLWithQMarks getExtensionSQL(int hwId, 
			List<String> studentIds) {
		final String subqry = toString(
				SELECT, joinAll(
					EXT_STUDENT,
					EXT_EXTENSION),
				FROM + EXTENSIONS
				+ WHERE + EXT_HWID + EQUALS + QMARK
				+ AND + EXT_STUDENT + IN, parensQMarks(studentIds),
				UNION_ALL + SELECT_UNIQUE, joinAll(
					RESP_STUDENT,
					0),
				FROM + RESPONSES
				+ WHERE + RESP_HWID + EQUALS + QMARK
				+ AND + RESP_STUDENT + NOT + IN, parens(
					SELECT + EXT_STUDENT
					+ FROM + EXTENSIONS 
					+ WHERE + EXT_HWID + EQUALS + QMARK));
		return new SQLWithQMarks(subqry, 
				hwId,
				studentIds,
				hwId,
				hwId);
	} // getExtensionSQL(int, List<String>)

	/** Gets the SQL to view the response when recalculating multiple 
	 * modified grades, plus values to substitute for question marks in the 
	 * SQL. Returns:
	 * <pre>
	 * select student_id, 
	 * 		responses_v6.pb_id, 
	 * 		tries, 
	 * 		grade * points as grade, 
	 * 		to_date(date_time, 'YYYY/MM/DD HH24:MI:SS') as date_time
	 * from responses_v6 
	 * join hwset_qs_v2
	 * on responses_v6.hw_id = hwset_qs_v2.hw_id
	 * and responses_v6.pb_id = hwset_qs_v2.pb_id
	 * where grade != 0
	 * and responses_v6.hw_id = [hwId]
	 * and responses_v6.student_id in ([studentIds])
	 * </pre>
	 * @param	hwId	ID of the assignment
	 * @param	studentIds	login IDs of students whose grades should be 
	 * recalculated
	 * @return	SQL and values to view the response and the serial number
	 */
	private static SQLWithQMarks getResponseSQL(int hwId, 
			List<String> studentIds) {
		final String subqry = toString(
				SELECT, joinAll(
					RESP_STUDENT,
					RESPONSES + DOT + RESP_QID,
					RESP_TRIES,
					RESP_GRADE + TIMES + HWQS_PTS + AS + RESP_GRADE,
					getBuilder(
						toDate(RESP_WHEN, quotes(DB_DATE_FORMAT[ORACLE])),
						AS + RESP_WHEN)),
				FROM + RESPONSES 
				+ JOIN + HW_QS
				+ ON + RESPONSES + DOT + RESP_HWID 
					+ EQUALS + HW_QS + DOT + HWQS_HWID
				+ AND + RESPONSES + DOT + RESP_QID 
					+ EQUALS + HW_QS + DOT + HWQS_QID
				+ WHERE + RESP_GRADE + IS_NOT_ZERO
				+ AND + RESPONSES + DOT + RESP_HWID + EQUALS + QMARK
				+ AND + RESP_STUDENT + IN, parensQMarks(studentIds));
		return new SQLWithQMarks(subqry, 
				hwId,
				studentIds);
	} // getResponseSQL(int, List<String>)

	/** Constructor to disable external instantiation. */
	private ResponseWrite() { } 

} // ResponseWrite
