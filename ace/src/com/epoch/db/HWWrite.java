package com.epoch.db;

import static com.epoch.db.dbConstants.ResponsesConstants.*;
import static com.epoch.db.dbConstants.UserRWConstants.*;
import com.epoch.assgts.Assgt;
import com.epoch.assgts.assgtConstants.AssgtConstants;
import com.epoch.assgts.AssgtQGroup;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.NonExistentException;
import com.epoch.utils.DateUtils;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Handles storage of assignments, including students' instantiations.  */
public final class HWWrite extends DBCommon implements AssgtConstants {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

/* ************* Public methods ***************/

	/** Adds a new assignment.
	 * @param	assgt	the assignment
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void addHW(Assgt assgt) throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			addHW(con, assgt);
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // addHW(Assgt)

	/** Adds new assignments.  Used in assignment import or cloning a course.
	 * @param	assgts	the assignments
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void addHWs(Assgt[] assgts) throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			for (final Assgt assgt : assgts) addHW(con, assgt);
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // addHWs(Assgt[])

	/** Modifies an existing assignment.
	 * @param	assgt	properties and content of the assignment
	 * @param	editorPage1	true if the questions-editing page called this method
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setHW(Assgt assgt, boolean editorPage1) throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			if (editorPage1) setHWPage1(con, assgt);
			else setHWPage2(con, assgt);
			con.commit();
		} catch (NonExistentException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // setHW(Assgt, boolean)

	/** Sets an assignment into which other assignments have been merged, 
	 * deleting the other assignments.
	 * @param	mergedAssgt	properties and content of the merged assignment
	 * @param	otherAssgtIds	ID numbers of the other assignments
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setHWMerger(Assgt mergedAssgt, int[] otherAssgtIds) 
			throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			setHWPage1(con, mergedAssgt);
			setHWPage2(con, mergedAssgt);
			deleteHWSets(con, new SQLWithQMarks(parensQMarks(otherAssgtIds), 
					otherAssgtIds));
			con.commit();
		} catch (NonExistentException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // setHWMerger(Assgt, int[])

	/** Saves the new flags of an existing assignment without recalculating
	 * modified grades.
	 * @param	assgt	properties and content of the assignment
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setFlags(Assgt assgt) throws DBException {
		final String SELF = "HWWrite.setFlags: ";
		final String qry = 
				UPDATE + HWSETS 
				+ SET + HW_FLAGS + EQUALS + QMARK
				+ WHERE + HW_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				assgt.getFlags(),
				assgt.id);
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
	} // setFlags(Assgt)

	/** Removes an assignment.  
	 * @param	id	unique ID of the assignment
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void deleteHWSet(int id) throws DBException {
		Connection con = null; 
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			deleteHWSets(con, new SQLWithQMarks(parens(QMARK), id));
			debugPrint("HWWrite.deleteHWSet: committing.");
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // deleteHWSet(int)

	/** Reorders the assignments in a course.
	 * @param	assgts	the assignments in the desired order
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void reorderHWs(List<Assgt> assgts) 
			throws DBException {
		if (Utils.isEmpty(assgts)) return;
		final String SELF = "HWWrite.reorderHWs: ";
		final int courseId = assgts.get(0).courseId;
		final String qry = 
				UPDATE + HWSETS
				+ SET + HW_NUM + EQUALS + QMARK
				+ WHERE + HW_ID + EQUALS + QMARK
				+ AND + HW_COURSE + EQUALS + QMARK;
		debugPrint(SELF, qry); 
		Connection con = null;
		PreparedStatement stmt = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			stmt = con.prepareStatement(qry);
			int hwNum = 1;
			for (final Assgt assgt : assgts) {
				final StringBuilder joinedValues = setValues(stmt,
						hwNum++,
						assgt.id,
						courseId);
				debugPrint(SELF, "batch ", hwNum, ": ", joinedValues); 
				stmt.addBatch();
				if (hwNum % 100 == 1) {
					stmt.executeBatch();
					stmt.clearBatch();
				} // if time to submit
			} // for each assignment
			if (hwNum % 100 != 1) {
				stmt.executeBatch();
			} // if need to submit
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, null);
		}
	} // reorderHWs(List<Assgt>)

	/** Sets an extension for a single student on a single assignment.
	 * @param	studentId	the student
	 * @param	courseId	the course ID
	 * @param	hwId	the assignment ID
	 * @param	extensionStr	the string value of the extension
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setExtension(String studentId, int courseId, int hwId, 
			String extensionStr) throws DBException {
		final String SELF = "HWWrite.setExtension: ";
		String qry = toString(
				SELECT, count(), AS + SRCH_RESULT
				+ FROM + EXTENSIONS
				+ WHERE + EXT_HWID + EQUALS + QMARK
				+ AND + EXT_STUDENT + EQUALS + QMARK);
		SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				hwId,
				studentId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			final boolean alreadyExists = 
					rs.next() && rs.getInt(SRCH_RESULT) > 0;
			closeStmtAndRs(stmt, rs);
			final double extension = MathUtils.parseDouble(extensionStr);
			if (alreadyExists) {
				sql_vals = new SQLWithQMarks();
				if (extension == 0.0) {
					sql_vals.setSql(DELETE_FROM + EXTENSIONS);
				} else {
					sql_vals.setSql(UPDATE + EXTENSIONS 
							+ SET, equalsJoinQMarks(
								EXT_EXTENSION_STR,
								EXT_EXTENSION));
					sql_vals.setValues(
							extensionStr, 
							extension);
				} // if there's no extension
				sql_vals.addToSql(
						WHERE + EXT_HWID + EQUALS + QMARK
						+ AND + EXT_STUDENT + EQUALS + QMARK);
				sql_vals.addValues(
						hwId,
						studentId);
				debugPrint(SELF + "alreadyExists = ", alreadyExists, 
						'\n', sql_vals);
				tryUpdate(con, sql_vals);
			} else {
				// We need to fit the new extension into the existing extensions
				// with the correct serial number, set by alphabetical order by
				// family name and given name. Note: currently this code doesn't
				// take instructor language into account.
				con.setAutoCommit(false);
				// first insert with bad serial number
				final String[] fields = new String[] {
						EXT_HWID,
						EXT_STUDENT,
						EXT_EXTENSION_STR,
						EXT_EXTENSION,
						EXT_NUM};
				qry = getInsertIntoValuesQMarksSQL(EXTENSIONS, fields);
				sql_vals = new SQLWithQMarks(qry,
						hwId,
						studentId,
						extensionStr,
						extension,
						0);
				debugPrint(SELF + "alreadyExists = ", alreadyExists, 
						'\n', sql_vals);
				tryUpdate(con, sql_vals);
				// get extensionees in order of their names
				qry = SELECT + EXT_STUDENT
						+ FROM + EXTENSIONS
						+ JOIN + USERS
						+ ON + EXT_STUDENT + EQUALS + USER_ID
						+ WHERE + EXT_HWID + EQUALS + QMARK
						+ ORDER_BY + USER_SORT;
				sql_vals = new SQLWithQMarks(qry,
						hwId);
				debugPrint(SELF, sql_vals);
				stmt = getStatement(con, sql_vals);
				rs = stmt.executeQuery();
				final List<String> extensionStudentIds = 
						new ArrayList<String>();
				while (rs.next()) {
					extensionStudentIds.add(rs.getString(EXT_STUDENT));
				} // while there are results
				closeStmtAndRs(stmt, rs);
				// update the serial numbers of extensions according to order
				// of the extensionees' names
				qry = UPDATE + EXTENSIONS 
						+ SET + EXT_NUM + EQUALS + QMARK
						+ WHERE + EXT_STUDENT + EQUALS + QMARK
						+ AND + EXT_HWID + EQUALS + QMARK;
				debugPrint(SELF, qry); 
				stmt = con.prepareStatement(qry);
				int extNum = 0;
				for (final String extStudentId : extensionStudentIds) {
					final StringBuilder joinedValues = setValues(stmt,
							extNum++,
							extStudentId,
							hwId);
					debugPrint(SELF, "batch ", extNum, ": ", joinedValues); 
					stmt.addBatch();
					if (extNum % 100 == 0) {
						stmt.executeBatch();
						stmt.clearBatch();
					} // if time to submit
				} // for each assignment
				if (extNum % 100 != 0) {
					stmt.executeBatch();
				} // if need to submit
				con.commit();
			} // if the student already has an extension for this assignment
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
	} // setExtension(String, int, String)

	/** Makes the visibility of an assignment dependent on another assignment 
	 * being mastered.
	 * @param	dependentHWId	the unique ID of the dependent assignment
	 * @param	masteryHWId	the unique ID of the mastery assignment that must be
	 * mastered before the dependent assignment can be displayed to a student
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setAssgtDependency(int dependentHWId, 
			int masteryHWId) throws DBException {
		final String SELF = "HWWrite.setAssgtDependency: ";
		final String qry = toString(
				UPDATE + HWSETS + SET + HW_DEPEND + EQUALS,
					masteryHWId != 0 ? masteryHWId : EMPTY,
				WHERE + HW_ID + EQUALS, dependentHWId);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
		if (masteryHWId != 0) sql_vals.addValue(masteryHWId);
		sql_vals.addValue(dependentHWId);
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
	} // setAssgtDependency(int, int)

	/** Removes dependencies of other assignments on an assignment.
	 * @param	id	unique ID of an assignment 
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void removeDependenciesOn(int id) throws DBException {
		final String SELF = "HWWrite.removeDependenciesOn: ";
		Connection con = null;
		try {
			con = getPoolConnection();
			removeDependenciesOn(con, id);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // removeDependenciesOn(int)

	/** Replaces some of the questions assigned to a student with other
	 * questions, deleting all responses to the replaced questions.
	 * @param	studentId	login ID of the student
	 * @param	hwId	assignment ID number
	 * @param	oldQIds	ID numbers of the questions to be replaced
	 * @param	newQIds	ID numbers of the new questions
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void rewriteAssignedQuestions(String studentId, int hwId, 
			int[] oldQIds, int[] newQIds) throws DBException {
		final String SELF = "HWWrite.rewriteAssignedQuestions: ";
		final String qry = 
				UPDATE + ASSIGNED_QS
				+ SET + ASSGND_QS_QID + EQUALS + QMARK
				+ WHERE + ASSGND_QS_QID + EQUALS + QMARK
				+ AND + ASSGND_QS_STUDENT + EQUALS + QMARK
				+ AND + ASSGND_QS_HWID + EQUALS + QMARK;
		debugPrint(SELF, qry); 
		Connection con = null;
		PreparedStatement stmt = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			ResponseWrite.deleteResults(con, hwId, oldQIds, studentId);
			stmt = con.prepareStatement(qry);
			for (int qNum = 0; qNum < oldQIds.length; qNum++) {
				final StringBuilder joinedValues = setValues(stmt,
						newQIds[qNum],
						oldQIds[qNum],
						studentId,
						hwId);
				debugPrint(SELF, "batch ", qNum + 1, ": ", joinedValues); 
				stmt.addBatch();
			} // for each question
			stmt.executeBatch();
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, null);
		}
	} // rewriteAssignedQuestions(String, int, int[], int[])

/* ************* Private methods for adding data ********/

	/** Adds an assignment.
	 * @param	con	connection to database
	 * @param	assgt	the assignment with information to store
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void addHW(Connection con, Assgt assgt) 
			throws DBException, SQLException {
		final String SELF = "HWWrite.addHW: ";
		if (assgt.courseId == 0) throw new DBException(
				"Invalid course ID of 0");
		final String newNumQry = toString(
				SELECT, max(HW_NUM), AS + SRCH_RESULT 
				+ FROM + HWSETS
				+ WHERE + HW_COURSE + EQUALS + QMARK);
		SQLWithQMarks sql_vals = new SQLWithQMarks(newNumQry,
				assgt.courseId);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			final int newSerialNum = (rs.next() 
					? rs.getInt(SRCH_RESULT) + 1 : 1);
			assgt.id = nextSequence(con, HWSETS_SEQ);
			final String[] fields = new String[] {
					HW_ID,
					HW_COURSE,
					HW_NUM,
					HW_FLAGS,
					HW_NAME, // CLOB
					HW_REMARKS, // CLOB
					HW_CREATED,
					HW_DUE,
					HW_DURATION,
					HW_MAXEXT,
					HW_TRIES};
			final String qry = 
					getInsertIntoValuesQMarksSQL(HWSETS, fields);
			sql_vals = new SQLWithQMarks(qry,
					assgt.id,
					assgt.courseId,
					newSerialNum, 
					assgt.getFlags(),
					assgt.getDbName(),
					assgt.getDbRemarks(),
					dateToString(assgt.creationDate),
					dateToString(assgt.getDueDate()),
					assgt.getDuration(),
					assgt.getMaxExtensionStr(),
					assgt.getDbTries());
			con.setAutoCommit(false);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals); 
			addHWQs(con, assgt);
			addAllowedRxnCondns(con, assgt);
			if (assgt.hasGradingParams()) {
				addGradingParams(con, assgt);
			} // if there are grading parameters
			debugPrint(SELF + "committing.");
			con.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			con.rollback();
			throw new DBException(e.getMessage());
		} catch (DBException e) {
			e.printStackTrace();
			con.rollback();
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
	} // addHW(Connection, Assgt)

	/** Adds the questions in a new assignment to their table.
	 * @param	con	connection to database
	 * @param	assgt	the assignment with questions to store
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void addHWQs(Connection con, Assgt assgt) 
			throws DBException, SQLException {
		final String SELF = "HWWrite.addHWQs: ";
		PreparedStatement stmt = null;
		int numQs = 0;
		int grpNum = 0;
		final List<AssgtQGroup> qGrps = assgt.getQGroups();
		try {
			final String[] fields = new String[] {
					HWQS_HWID,
					HWQS_GRP_NUM,
					HWQS_GRP_PICK,
					HWQS_BUNDLE_SIZE,
					HWQS_QNUM,
					HWQS_QID,
					HWQS_PTS,
					HWQS_PTS_STR,
					HWQS_DEPENDS};
			final String qry = getInsertIntoValuesQMarksSQL(HW_QS, fields);
			debugPrint(SELF, qry); 
			stmt = con.prepareStatement(qry); 
			grpNum = 0;
			for (final AssgtQGroup qGrp : qGrps) {
				grpNum++;
				final int pick = qGrp.getPick();
				final int bundleSize = qGrp.getBundleSize();
				final String ptsStr = qGrp.getPts();
				final int dependsOn = qGrp.getDependsOn();
				final List<Integer> grpQIds = qGrp.getQIds();
				int qNumInQGrp = 0;
				for (final Integer grpQId : grpQIds) {
					final StringBuilder joinedValues = setValues(stmt,
							assgt.id,
							grpNum,
							pick,
							bundleSize,
							++qNumInQGrp,
							grpQId,
							MathUtils.parseDouble(ptsStr),
							ptsStr,
							dependsOn);
					debugPrint(SELF, "batch ", qNumInQGrp, ": ", joinedValues); 
					stmt.addBatch();
					numQs++;
					if (numQs % 100 == 0) {
						stmt.executeBatch();
						stmt.clearBatch();
					} // if time to submit
				} // for each qId
			} // for each group of questions in the assignment
			if (numQs % 100 != 0) {
				stmt.executeBatch();
			} // if need to submit
		} catch (SQLException e) {
			alwaysPrint(SELF + "batch insert of Qs failed on assgt ", 
					assgt.id, " with ", qGrps.size(), " groups after ", 
					numQs, " question(s).");
			throw e;
		} finally {
			closeConnection(null, stmt, null);
		} // try
	} // addHWQs(Connection, Assgt)

	/** Adds allowed reaction conditions of an assignment.
	 * @param	con	database connection
	 * @param	assgt	the assignment with allowed reaction conditions 
	 * to store
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void addAllowedRxnCondns(Connection con, 
			Assgt assgt) throws SQLException {
		final String SELF = "HWWrite.addAllowedRxnCondns: ";
		PreparedStatement stmt = null;
		int numCondns = 0;
		final int[] rxnCondnIds = assgt.getAllowedRxnCondns();
		if (!Utils.isEmpty(rxnCondnIds)) try {
			final String[] fields = new String[] {
					HWRXNCONDN_HWID,
					HWRXNCONDN_RXNCOND_ID};
			final String qry = 
					getInsertIntoValuesQMarksSQL(HWRXNCONDNS, fields);
			debugPrint(SELF, qry);
			stmt = con.prepareStatement(qry);
			for (final int rxnCondId : rxnCondnIds) {
				final StringBuilder joinedValues = setValues(stmt,
						assgt.id,
						rxnCondId);
				debugPrint(SELF, "batch ", numCondns + 1, ": ", joinedValues); 
				stmt.addBatch();
				numCondns++;
				if (numCondns % 100 == 0) {
					stmt.executeBatch();
					stmt.clearBatch();
				} // if time to submit
			} // for each allowed reaction condition
			if (numCondns % 100 != 0) {
				stmt.executeBatch();
			} // if need to submit
		} catch (SQLException e) {
			alwaysPrint(SELF + "batch insert of allowed reaction "
					+ "conditions failed on assgt ", assgt.id, " after ", 
					numCondns, " condition(s).");
			throw e;
		} finally {
			closeConnection(null, stmt, null);
		} // try
	} // addAllowedRxnCondns(Connection, Assgt)

/* ************* Private methods for modifying an assignment ***************/

	/** Stores modification to assignment made on first (question-editing) page
	 * of assignment creation tool. Modifications may include: name, remarks,
	 * questions, groupings of questions for randomization, question 
	 * dependencies, question points, allowed number of tries, permissible
	 * reaction conditions to be displayed.
	 * @param	con	database connection
	 * @param	newAssgt	the modified assignment
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	SQLException	if there's a problem writing to the database
	 * @throws	NonExistentException	if the assignment doesn't exist
	 */
	private static void setHWPage1(Connection con, Assgt newAssgt) 
			throws DBException, NonExistentException, SQLException {
		final String SELF = "HWWrite.setHWPage1: ";
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con.setAutoCommit(false);
			if (newAssgt.basicsHaveChanged() || newAssgt.flagsHaveChanged()) {
				final String qry = toString(
						UPDATE + HWSETS + SET, equalsJoinQMarks(
							HW_NAME, // CLOB
							HW_REMARKS, // CLOB
							HW_FLAGS, 
							HW_TRIES),
						WHERE + HW_ID + EQUALS + QMARK);
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
						newAssgt.getDbName(),
						newAssgt.getDbRemarks(),
						newAssgt.getFlags(),
						newAssgt.getDbTries(),
						newAssgt.id);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} // if should write the basics
			if (!newAssgt.isMasteryAssgt() && newAssgt.flagsHaveChanged()) {
				removeDependenciesOn(con, newAssgt.id);
			} // if maybe changed from mastery to nonmastery
			if (newAssgt.questionsHaveChanged()) {
				// delete all responses to questions from this
				// hwSet that refer to deleted questions.
				final String dbInstructorId = 
						HWRead.getHWInstructor(con, newAssgt.id);
				if (!newAssgt.instructorId.equals(dbInstructorId)) {
					alwaysPrint(SELF
							+ "stored owner of assignment ", newAssgt.id, 
							" with name '", newAssgt.getName(),
							"', ", dbInstructorId, ", not the same as ",
							"owner recorded in description of assignment "
							+ "being stored, ", newAssgt.instructorId,
							"; aborting modification.");
					return;
				} // if person modifying assignment is not original instructor
				// remove responses to fixed or random questions no longer 
				// in the assignment
				final Assgt origAssgt = HWRead.getHW(newAssgt.id);
				final List<Integer> oldQIds = origAssgt.getQIds();
				final List<Integer> newQIds = newAssgt.getQIds();
				debugPrint(SELF + "Q list from DB = ", oldQIds, 
						", new Q list being stored = ", newQIds);
				final List<Integer> removedQIds = new ArrayList<Integer>();
				for (final Integer oldQId : oldQIds) {
					if (!newQIds.contains(oldQId)) {
						removedQIds.add(oldQId);
					} // if new list doesn't contain previously assigned Q
				} // each question in old assignment
				if (!removedQIds.isEmpty()) {
					debugPrint(SELF + "Q(s) ", removedQIds, " are no longer "
							+ "in assignment.");
					ResponseWrite.deleteResults(con, newAssgt.id, removedQIds);
					ForumRW.deleteTopicLinks(con, newAssgt.id, removedQIds);
				} // if questions have been removed from assignment
				// update the assignment questions and dependencies
				deleteHWQs(con, newAssgt.id);
				addHWQs(con, newAssgt);
				// now update the instantiated assignments
				if (newAssgt.lacksRandomGroups()) {
					// replace all instantiated assignments with new one
					debugPrint(SELF + "new version of assignment contains no "
							+ "random questions; globally replacing all old "
							+ "instantiated assignments with new one, ", newQIds);
					rewriteAssignedQuestions(con, newAssgt.id, newQIds);
				} else { // oy, we have randomized questions
					debugPrint(SELF + "new version of assignment contains "
							+ "random questions.");
					// find which posns in old instantiations belonged to which groups
					final int[] oldGrpStartPosns = origAssgt.getRealStartPosns();
					debugPrint(SELF + "0-based start positions of each group "
							+ "in the old list ", oldQIds, " are ", 
							oldGrpStartPosns);
					// find which posns in new instantiations will belong to which groups
					final int[] newQGrpNums = newAssgt.getGroupNumbers();
					debugPrint(SELF + "each question in the new assignment ", 
							newQIds, " belongs to the corresponding "
							+ "0-based group: ", newQGrpNums);
					final int numNewQs = newQGrpNums.length;
					final int[] oldNumsOfNewQs = new int[numNewQs];
					// correlate positions of identical Q groups and Qs in new 
					// and old assignments
					int newQNum = 0;
					for (final AssgtQGroup newQGrp : newAssgt.getQGroups()) {
						final int numQsSeen = newQGrp.getNumQsSeen();
						final int newGrpPosnInOld = origAssgt.indexOf(newQGrp);
						for (int seenQNum = 0; 
								seenQNum < numQsSeen; 
								seenQNum++) {
							oldNumsOfNewQs[newQNum] = (newGrpPosnInOld < 0 ? -1
									: oldGrpStartPosns[newGrpPosnInOld] 
										+ seenQNum);
							newQNum++;
						} // for each pick
					} // for each group of Qs
					debugPrint(SELF + "0-based correspondence of old questions "
							+ "to new ones is ", oldNumsOfNewQs);
					final String assgtQry = toString(
							SELECT, joinAll( 
								ASSGND_QS_STUDENT,
								ASSGND_QS_QID),
							FROM + ASSIGNED_QS
							+ WHERE + ASSGND_QS_HWID + EQUALS + QMARK
							+ ORDER_BY, joinAll(
								ASSGND_QS_STUDENT,
								ASSGND_QS_QNUM));
					final SQLWithQMarks sql_vals = new SQLWithQMarks(assgtQry,
							newAssgt.id);
					debugPrint(SELF, sql_vals);
					stmt = getStatement(con, sql_vals);
					rs = stmt.executeQuery();
					if (rs.next()) while (!rs.isAfterLast()) {
						final String studId = rs.getString(ASSGND_QS_STUDENT);
						final List<Integer> oldStudQs = 
								new ArrayList<Integer>();
						while (!rs.isAfterLast() && studId.equals(
								rs.getString(ASSGND_QS_STUDENT))) {
							oldStudQs.add(Integer.valueOf(
									rs.getInt(ASSGND_QS_QID)));
							rs.next();	
						} // while more qIds to get for this student
						debugPrint(SELF + "student ", studId, "'s originally "
								+ "instantiated assignment is: ", oldStudQs);
						// build new assignment, preserving instantiations of groups
						// that remain unchanged from the old assignment
						final List<Integer> newStudQs = 
								new ArrayList<Integer>();
						newQNum = 0;
						while (newQNum < numNewQs) {
							final int oldNumOfNewQ = oldNumsOfNewQs[newQNum];
							if (oldNumOfNewQ >= 0) { // previously assigned
								// put prev. assigned random or fixed Q in new list
								final Integer preservedQId = 
										oldStudQs.get(oldNumOfNewQ);
								debugPrint(SELF + "Q ", newQNum + 1, 
										" with ID ", preservedQId, 
										" was at position ", oldNumOfNewQ + 1, 
										" in old list.");
								newStudQs.add(preservedQId);
								newQNum++;
							} else { // not previously assigned
								// need to generate new group of questions
								final int newGrpNum = newQGrpNums[newQNum];
								final AssgtQGroup newQGroup = 
										newAssgt.getQGroup(newGrpNum + 1);
								final List<Integer> addlQs = 
										newQGroup.instantiate();
								newStudQs.addAll(addlQs);
								final int numQsSeen = newQGroup.getNumQsSeen();
								newQNum += numQsSeen;
								debugPrint(SELF + "Q ", newQNum + 1, 
										(numQsSeen > 1 ? toString("-", 
												newQNum + numQsSeen, " weren't")
											: " wasn't"), 
										" in old list; added ", addlQs);
							} // if Q has previously been assigned
						} // while there are more questions to add to the list
						rewriteAssignedQuestions(con, newAssgt.id, studId, 
								newStudQs);
					} // while there are more students with instantiated assignments 
				} // if assignment has only fixed questions
			} // if questions have changed
			if (newAssgt.ptsPerQHaveChanged()) {
				setPtsPerQ(con, newAssgt);
			} // if extensions or grading parameters have changed
			if (newAssgt.dependenciesHaveChanged()) {
				setQDependencies(con, newAssgt);
			} // if question dependencies have changed
			if (newAssgt.allowedRxnCondnsHaveChanged()) {
				deleteAllowedRxnCondns(con, newAssgt.id);
				addAllowedRxnCondns(con, newAssgt);
			} // if allowed reaction conditions have changed
			if (newAssgt.ptsPerQHaveChanged()
					|| newAssgt.questionsHaveChanged()) {
				debugPrint(SELF, "recalculating modified grades of "
						+ "all responses to assignment ", newAssgt.id);
				ResponseWrite.recalculateModifiedGrades(con, newAssgt.id);
			} // if extensions or grading parameters have changed
			debugPrint(SELF + "committing.");
			con.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		} finally {
			closeStmtAndRs(stmt, rs);
		}
	} // setHWPage1(Connection, Assgt)

	/** Resets the points per question in an assignment. 
	 * @param	con	database connection
	 * @param	newAssgt	the edited assignment 
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void setPtsPerQ(Connection con, Assgt newAssgt) 
			throws SQLException {
		final String SELF = "HWWrite.setPtsPerQ: ";
		PreparedStatement stmt = null;
		final String qry = toString(
				UPDATE + HW_QS + SET, equalsJoinQMarks(
					HWQS_PTS,
					HWQS_PTS_STR),
				WHERE + HWQS_HWID + EQUALS + QMARK
				+ AND + HWQS_GRP_NUM + EQUALS + QMARK);
		debugPrint(SELF, qry); 
		int grpNum = 0;
		try {
			stmt = con.prepareStatement(qry);
			for (final AssgtQGroup newQGrp : newAssgt.getQGroups()) {
				final String ptsStr = newQGrp.getPts();
				final double pts = MathUtils.parseDouble(ptsStr, 1);
				final StringBuilder joinedValues = setValues(stmt,
						pts,
						ptsStr,
						newAssgt.id,
						++grpNum);
				debugPrint(SELF, "batch ", grpNum, ": ", joinedValues); 
				stmt.addBatch();
				if (grpNum % 100 == 0) {
					stmt.executeBatch();
					stmt.clearBatch();
				} // if time to submit
			} // for each group of Qs
			if (grpNum % 100 != 0) {
				stmt.executeBatch();
			} // if need to submit
		} catch (SQLException e) {
			alwaysPrint(SELF + "caught SQLException at question group ",
					grpNum, ".\n", qry);
			e.printStackTrace();
			throw e;
		} finally {
			closeConnection(null, stmt, null);
		} // try
	} // setPtsPerQ(Connection, Assgt)

	/** Resets the question dependencies in an assignment. 
	 * @param	con	database connection
	 * @param	newAssgt	the edited assignment 
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void setQDependencies(Connection con, Assgt newAssgt) 
			throws SQLException {
		final String SELF = "HWWrite.setQDependencies: ";
		PreparedStatement stmt = null;
		String qry = UPDATE + HW_QS
				+ SET + HWQS_DEPENDS + EQUALS + NULL
				+ WHERE + HWQS_HWID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				newAssgt.id);
		int grpNum = 0;
		try {
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals); 
			qry = UPDATE + HW_QS
					+ SET + HWQS_DEPENDS + EQUALS + QMARK
					+ WHERE + HWQS_HWID + EQUALS + QMARK
					+ AND + HWQS_GRP_NUM + EQUALS + QMARK;
			debugPrint(SELF, qry); 
			stmt = con.prepareStatement(qry);
			for (final AssgtQGroup newQGrp : newAssgt.getQGroups()) {
				grpNum++;
				if (newQGrp.getNumQs() == 1) {
					final int dependsOn = newQGrp.getDependsOn();
					if (dependsOn != 0) {
						final StringBuilder joinedValues = setValues(stmt,
								dependsOn,
								newAssgt.id,
								grpNum);
						debugPrint(SELF, "batch ", grpNum, ": ", joinedValues); 
						stmt.addBatch();
						if (grpNum % 100 == 0) {
							stmt.executeBatch();
							stmt.clearBatch();
						} // if time to submit
					} // if the Q is dependent on another one
				} // if there's one Q in this group
			} // for each group of Qs
			if (grpNum % 100 != 0) {
				stmt.executeBatch();
			} // if need to submit
		} catch (SQLException e) {
			alwaysPrint(SELF + "caught SQLException at question group ",
					grpNum, ".\n", qry);
			e.printStackTrace();
			throw e;
		} finally {
			closeConnection(null, stmt, null);
		} // try
	} // setQDependencies(Connection, Assgt)

	/** Deletes questions belonging to an assignment. 
	 * @param	con	database connection
	 * @param	hwId	unique ID of the assignment 
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private static void deleteHWQs(Connection con, int hwId) 
			throws DBException {
		final String SELF = "HWWrite.deleteHWQs: ";
		final String qry = 
				DELETE_FROM + HW_QS
				+ WHERE + HWQS_HWID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				hwId);
		try {
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals); 
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		}
	} // deleteHWQs(Connection, int)

	/** Deletes questions and question dependencies belonging to an assignment.
	 * @param	con	database connection
	 * @param	hwId	unique ID of the assignment 
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private static void deleteAllowedRxnCondns(Connection con, int hwId) 
			throws DBException {
		final String SELF = "HWWrite.deleteAllowedRxnCondns: ";
		final String qry = 
				DELETE_FROM + HWRXNCONDNS
				+ WHERE + HWRXNCONDN_HWID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				hwId);
		try {
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals); 
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		}
	} // deleteAllowedRxnCondns(Connection, int)

	/** Stores modification to assignment made on second page of assignment
	 * creation tool. Modifications may include: due date &amp; time, flags
	 * regarding exam status and recording of responses, attempt- and time-
	 * dependent grading parameters, and extensions.
	 * @param	con	database connection
	 * @param	assgt	properties of the assignment
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	SQLException	if there's a problem writing to the database
	 * @throws	NonExistentException	if the assignment doesn't exist
	 */
	private static void setHWPage2(Connection con, Assgt assgt) 
			throws DBException, NonExistentException, SQLException {
		final String SELF = "HWWrite.setHWPage2: ";
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con.setAutoCommit(false);
			boolean recalculateModifiedGrades = false;
			if (assgt.dueDateHasChanged()) {
				String qry = 
						SELECT + HW_DUE
						+ FROM + HWSETS
						+ WHERE + HW_ID + EQUALS + QMARK;
				SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
						assgt.id);
				debugPrint(SELF, sql_vals);
				stmt = getStatement(con, sql_vals);
				rs = stmt.executeQuery();
				rs.next();
				final Date oldDueDate = toDate(rs.getString(HW_DUE));
				final Date newDueDate = assgt.getDueDate();
				qry = UPDATE + HWSETS 
						+ SET + HW_DUE + EQUALS + QMARK
						+ WHERE + HW_ID + EQUALS + QMARK;
				sql_vals = new SQLWithQMarks(qry,
						dateToString(newDueDate),
						assgt.id);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals); 
				if (newDueDate.after(oldDueDate)) {
					reduceExtensions(con, assgt, oldDueDate);
				} // if the new due date is later than the old one
				recalculateModifiedGrades = true;
			} // if should write the due date
			if (assgt.flagsHaveChanged() || assgt.maxExtensionHasChanged()) {
				final String qry = toString(
						UPDATE + HWSETS + SET, equalsJoinQMarks(
							HW_MAXEXT,
							HW_FLAGS),
						WHERE + HW_ID + EQUALS + QMARK);
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
						assgt.getMaxExtensionStr(),
						assgt.getFlags(),
						assgt.id);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals); 
				recalculateModifiedGrades = true;
			} // if should write the flags or maximum extension
			if (assgt.durationHasChanged()) {
				final String qry = 
						UPDATE + HWSETS 
						+ SET + HW_DURATION + EQUALS + QMARK
						+ WHERE + HW_ID + EQUALS + QMARK;
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
						assgt.getDuration(),
						assgt.id);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals); 
			} // if should write the assignment duration
			if (assgt.gradingParamsHaveChanged()) {
				deleteGradingParams(con, assgt.id);
				addGradingParams(con, assgt);
				recalculateModifiedGrades = true;
			} // if extensions or grading parameters have changed
			if (assgt.extensionsHaveChanged()) {
				final Map<String, String> oldExts = 
						HWRead.getExtensions(con, assgt.id);
				final Map<String, String> newExts = new 
						LinkedHashMap<String, String>(assgt.getExtensions());
				debugPrint(SELF, "comparing old extensions ", oldExts,
						" to new ones ", newExts);
				final List<String> oldExtees = 
						new ArrayList<String>(oldExts.keySet());
				for (int extNum = oldExtees.size() - 1; extNum >= 0; extNum--) {
					final String oldExtee = oldExtees.get(extNum);
					final String newExt = newExts.get(oldExtee);
					if (newExt != null) {
						final String oldExt = oldExts.get(oldExtee);
						if (newExt.equals(oldExt)) {
							oldExtees.remove(extNum);
							newExts.remove(oldExtee);
						} // if extension hasn't changed
					} // if extensionee is in both old and new sets
				} // for each prior extensionee
				if (!oldExtees.isEmpty() || !newExts.isEmpty()) {
					// need to delete and add all to keep in sequence
					debugPrint(SELF, "rewriting all extensions for "
							+ "assignment ", assgt.id);
					deleteExtensions(con, assgt.id);
					addExtensions(con, assgt);
				} // if any extensions have changed
				oldExtees.addAll(newExts.keySet()); // all changed extensions
				if (!recalculateModifiedGrades && !oldExtees.isEmpty()) {
					debugPrint(SELF, "recalculating modified grades of ", 
							oldExtees, " on assignment ", assgt.id);
					ResponseWrite.recalculateModifiedGrades(con, 
							assgt.id, oldExtees);
				} // if not going to recalculate all of them anyway
			} // if extensions or grading parameters have changed
			if (recalculateModifiedGrades) {
				debugPrint(SELF, "recalculating modified grades of "
						+ "all responses to assignment ", assgt.id);
				ResponseWrite.recalculateModifiedGrades(con, assgt.id);
			} // if should recalculate modified grades
			debugPrint(SELF + "committing.");
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw e;
		} catch (DBException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw e;
		} finally {
			closeStmtAndRs(stmt, rs);
		}
	} // setHWPage2(Connection, Assgt)

	/** Deletes time- and attempt-dependent grading parameters belonging 
	 * to an assignment. 
	 * @param	con	database connection
	 * @param	id	unique ID of the assignment 
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private static void deleteGradingParams(Connection con, int id) 
			throws DBException {
		final int BOTH_PARAM_TYPES = -1;
		deleteGradingParams(con, id, BOTH_PARAM_TYPES);
	} // deleteGradingParams(Connection, int)

	/** Deletes grading parameters belonging to an assignment. 
	 * @param	con	database connection
	 * @param	hwId	unique ID of the assignment 
	 * @param	paramType	type of parameters to delete, or -1 for both 
	 * time- and attempt-dependent parameters
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private static void deleteGradingParams(Connection con, int hwId,
			int paramType) throws DBException {
		final String SELF = "HWWrite.deleteGradingParams: ";
		final String qry = toString(
				DELETE_FROM + GRADING_PARAMS
				+ WHERE + GRADING_HWID + EQUALS + QMARK,
				paramType < 0 ? ""
					: getBuilder(AND + GRADING_TYPE + EQUALS, 
						quotes(DB_PARAM_TYPES[paramType])));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				hwId);
		try {
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals); 
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		}
	} // deleteGradingParams(Connection, int, int)

	/** Adds the time- and attempt-dependent grading parameters of an 
	 * assignment to their own table.
	 * @param	con	connection to database
	 * @param	assgt	the assignment with grading parameters to store
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void addGradingParams(Connection con, Assgt assgt) 
			throws DBException, SQLException {
		final String SELF = "HWWrite.addGradingParams: ";
		PreparedStatement stmt = null;
		int numRows = 0;
		try {
			final String[] fields = new String[] {
					GRADING_HWID,
					GRADING_TYPE,
					GRADING_MIN,
					GRADING_MAX,
					GRADING_FACTOR,
					GRADING_MAX_STR,
					GRADING_FACTOR_STR};
			final String qry = 
					getInsertIntoValuesQMarksSQL(GRADING_PARAMS, fields);
			debugPrint(SELF, qry); 
			stmt = con.prepareStatement(qry);
			for (int pType = 0; pType < DB_PARAM_TYPES.length; pType++) {
				final String[][] gradingParams = assgt.getGradingParams(pType);
				final int numVals = gradingParams[LIMITS].length;
				final String typeStr = 
						String.valueOf(DB_PARAM_TYPES[pType]);
				for (int valNum = 0; valNum < numVals; valNum++) {
					String maxLimitStr = gradingParams[LIMITS][valNum];
					if (Utils.isEmpty(maxLimitStr)) maxLimitStr = NO_MAXIMUM;
					final double maxLimit = MathUtils.parseDouble(maxLimitStr);
					final double minLimit = (valNum == 0 ? 0 
							: MathUtils.parseDouble(
								gradingParams[LIMITS][valNum - 1]));
					final String factorStr = gradingParams[FACTORS][valNum];
					final double factor = MathUtils.parseDouble(factorStr);
					final StringBuilder joinedValues = setValues(stmt,
							assgt.id,
							typeStr,
							minLimit,
							maxLimit,
							factor,
							maxLimitStr,
							factorStr);
					debugPrint(SELF, "batch ", numRows + 1, ": ", joinedValues); 
					stmt.addBatch();
					numRows++;
					if (numRows % 100 == 0) {
						stmt.executeBatch();
						stmt.clearBatch();
					} // if time to submit
				} // for each limit and factor
			} // for each paramType
			if (numRows % 100 != 0) {
				stmt.executeBatch();
			} // if need to submit
		} catch (SQLException e) {
			alwaysPrint(SELF + "batch insert of grading parameters "
					+ "failed for assgt ", assgt.id, " after ", 
					numRows, " parameter(s).");
			throw e;
		} finally {
			closeConnection(null, stmt, null);
		} // try
	} // addGradingParams(Connection, Assgt)

	/** Deletes extensions belonging to an assignment. 
	 * @param	con	database connection
	 * @param	hwId	unique ID of the assignment 
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private static void deleteExtensions(Connection con, int hwId) 
			throws DBException {
		final String SELF = "HWWrite.deleteExtensions: ";
		final String qry = 
				DELETE_FROM + EXTENSIONS 
				+ WHERE + EXT_HWID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				hwId);
		try {
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals); 
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		}
	} // deleteExtensions(Connection, int)

	/** Adds the extensions of an assignment to their own table.
	 * @param	con	connection to database
	 * @param	assgt	the assignment with extensions to store
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void addExtensions(Connection con, Assgt assgt) 
			throws DBException, SQLException {
		final String SELF = "HWWrite.addExtensions: ";
		final Map<String, String> extensions = assgt.getExtensions();
		if (Utils.isEmpty(extensions)) return;
		PreparedStatement stmt = null;
		int numExts = 0;
		try {
			final String[] fields = new String[] {
					EXT_HWID,
					EXT_STUDENT,
					EXT_EXTENSION_STR,
					EXT_EXTENSION,
					EXT_NUM};
			final String qry = getInsertIntoValuesQMarksSQL(EXTENSIONS, fields);
			debugPrint(SELF, qry); 
			stmt = con.prepareStatement(qry);
			int extNum = 0;
			final List<String> studentIds = 
					new ArrayList<String>(extensions.keySet());
			for (final String studentId : studentIds) {
				final String extensionStr = extensions.get(studentId);
				final double extension = MathUtils.parseDouble(extensionStr);
				final StringBuilder joinedValues = setValues(stmt,
						assgt.id,
						studentId,
						extensionStr,
						extension,
						extNum++);
				debugPrint(SELF, "batch ", numExts + 1, ": ", joinedValues); 
				stmt.addBatch();
				numExts++;
				if (numExts % 100 == 0) {
					stmt.executeBatch();
					stmt.clearBatch();
				} // if time to submit
			} // for each dependency
			if (numExts % 100 != 0) {
				stmt.executeBatch();
			} // if need to submit
		} catch (SQLException e) {
			alwaysPrint(SELF + "batch insert of ", extensions.size(), 
					" extension(s) failed for assgt ", assgt.id, 
					" after ", numExts, " extension(s).");
			throw e;
		} finally {
			closeConnection(null, stmt, null);
		} // try
	} // addExtensions(Connection, Assgt)

	/** Reduces the extensions in an assignment when the due date is changed so
	 * that the extended dates don't change.
	 * @param	con	database connection
	 * @param	assgt	the assignment with a new due date
	 * @param	oldDueDate	the old due date of the assignment
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void reduceExtensions(Connection con, Assgt assgt, 
			Date oldDueDate) throws SQLException {
		final String SELF = "HWWrite.reduceExtensions: ";
		final Date newDueDate = assgt.getDueDate();
		final long timeDiffSecs = Math.round((newDueDate.getTime() 
				- oldDueDate.getTime()) / 1000.0);
		final double timeDiff = (assgt.isExam() 
				? DateUtils.secsToMins(timeDiffSecs)
				: DateUtils.secsToDays(timeDiffSecs));
		String timeDiffTrunc = String.valueOf(timeDiff);
		if (timeDiffTrunc.length() > EXT_EXTENSION_STR_LEN) {
			timeDiffTrunc = timeDiffTrunc.substring(0, EXT_EXTENSION_STR_LEN);
		} // if the time difference is too long
		final String extensionSQL = 
				greatest(EXT_EXTENSION + MINUS + QMARK + ", 0");
		String qry = toString(
				UPDATE + EXTENSIONS 
				+ SET, equalsJoin(
					EXT_EXTENSION, extensionSQL, 
					EXT_EXTENSION_STR, toVarChar(extensionSQL)),
				WHERE + EXT_HWID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				timeDiff,
				Double.valueOf(timeDiffTrunc),
				assgt.id);
		debugPrint(SELF, sql_vals);
		tryUpdate(con, sql_vals); 
		qry = toString(
				DELETE_FROM + EXTENSIONS 
				+ WHERE + EXT_EXTENSION + IS_ZERO
				+ OR + EXT_EXTENSION_STR + EQUALS, quotes('0'));
		debugPrint(SELF, qry);
		tryUpdate(con, qry); 
	} // reduceExtensions(Connection, Assgt, Date)

	/** Removes a batch of assignments. Also removes dependencies of other
	 * assignments on this batch of assignments.
	 * @param	con	database connection with commitment already set
	 * @param	hwIdsSql_vals	contains SQL of parenthesized unique ID of one 
	 * assignment, to be substituted by the stored value, or parenthesized 
	 * question marks, to be * substituted by the stored values
	 * @throws	DBException	if there's a problem writing to the database
	 */
	static void deleteHWSets(Connection con, SQLWithQMarks hwIdsSql_vals) 
			throws DBException {
		final String SELF = "HWWrite.deleteHWSets: ";
		try {
			debugPrint(SELF + "hwIdsSql_vals = ", hwIdsSql_vals);
			ForumRW.deleteTopicLinks(con, hwIdsSql_vals);
			removeDependenciesOn(con, hwIdsSql_vals);
			final String[][] fieldSets = new String[][] {
					{HWRXNCONDNS, HWRXNCONDN_HWID},
					{EXTENSIONS, EXT_HWID},
					{GRADING_PARAMS, GRADING_HWID},
					{RESP_SUBSTNS, RESP_SUBS_HWID},
					{RESPONSES, RESP_HWID},
					{ASSIGNED_QS, ASSGND_QS_HWID},
					{HW_QS, HWQS_HWID},
					{HWSETS, HW_ID}
					};
			final String hwIdsSql = hwIdsSql_vals.getSql();
			for (final String[] fields : fieldSets) {
				final String qry = toString(
						DELETE_FROM, fields[0], 
						WHERE, fields[1], IN, hwIdsSql);
				final SQLWithQMarks sql_vals = 
						new SQLWithQMarks(qry, hwIdsSql_vals);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} // for each table
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		}
	} // deleteHWSets(Connection, SQLWithQMarks)

	/** Removes dependencies of other assignments on this assignment.
	 * @param	con	database connection with commitment already set
	 * @param	hwId	unique ID of the assignment that others can no longer 
	 * depend on
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private static void removeDependenciesOn(Connection con, int hwId) 
			throws DBException {
		removeDependenciesOn(con, new SQLWithQMarks(parens(hwId), hwId)); 
	} // removeDependenciesOn(Connection, int)

	/** Removes dependencies of other assignments on this batch of assignments.
	 * @param	con	database connection with commitment already set
	 * @param	hwIdsSql_vals	contains SQL describing one or more assignments 
	 * that others can no longer depend on, plus values to substitute for the
	 * question marks in the SQL
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private static void removeDependenciesOn(Connection con, 
			SQLWithQMarks hwIdsSql_vals) throws DBException {
		final String SELF = "HWWrite.removeDependenciesOn: ";
		final String qry = toString(
				UPDATE + HWSETS 
				+ SET + HW_DEPEND + EQUALS + EMPTY
				+ WHERE + HW_DEPEND + IN, hwIdsSql_vals.getSql());
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, hwIdsSql_vals);
		try {
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			throw new DBException(e.getMessage());
		}
	} // removeDependenciesOn(Connection, SQLWithQMarks)

/* ****** Private methods for students' instantiated assignments *******/

	/** Stores the unique IDs of the questions that have been assigned to this
	 * student.
	 * @param	con	database connection
	 * @param	hwId	assignment ID number
	 * @param	studentId	login ID
	 * @param	qIds	question ID numbers
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	static void storeAssignedQuestions(Connection con, int hwId, 
			String studentId, List<Integer> qIds) throws SQLException {
		final String[] fields = new String[] {
				ASSGND_QS_STUDENT,
				ASSGND_QS_HWID,
				ASSGND_QS_ENTRY,
				ASSGND_QS_QNUM,
				ASSGND_QS_QID};
		final String qry = getInsertIntoValuesQMarksSQL(ASSIGNED_QS, fields);
		storeAssignedQuestions(con, hwId, studentId, qIds, qry);
	} // storeAssignedQuestions(Connection, int, String, List<Integer>)

	/** Stores the unique IDs of the questions that have been assigned to this
	 * student.
	 * @param	con	database connection
	 * @param	hwId	assignment ID number
	 * @param	studentId	login ID
	 * @param	qIds	question ID numbers
	 * @param	insertSQL	SQL query for inserting instantiated assignments
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void storeAssignedQuestions(Connection con, int hwId,
			String studentId, List<Integer> qIds, String insertSQL) 
			throws SQLException {
		final String SELF = "HWWrite.storeAssignedQuestions: ";
		PreparedStatement stmt = null;
		int serialNo = 1;
		final String dateStr = dateToString(new Date());
		try {
			debugPrint(SELF, insertSQL); 
			stmt = con.prepareStatement(insertSQL);
			for (final Integer qId : qIds) {
				final StringBuilder joinedValues = setValues(stmt,
						studentId,
						hwId,
						dateStr,
						serialNo++,
						qId);
				debugPrint(SELF, "batch ", serialNo - 1, ": ", joinedValues); 
				stmt.addBatch();
				if (serialNo % 100 == 0) {
					stmt.executeBatch();
					stmt.clearBatch();
				} // if time to submit
			} // for each qId
			if (serialNo % 100 != 0) {
				stmt.executeBatch();
			} // if need to submit
		} catch (SQLException e) {
			alwaysPrint(SELF + "batch insert of questions for ", 
					studentId, " on assignment ", hwId, " failed after ", 
					serialNo - 1, " question(s).");
			throw e;
		} finally {
			closeConnection(null, stmt, null);
		} // try
	} // storeAssignedQuestions(Connection, int, String, List<Integer>, String)

	/** Sets the date that the student entered an assignment.
	 * @param	hwId	assignment ID number
	 * @param	userId	login ID
	 * @param	firstEntry	the entry date
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setAssgtEntryDate(int hwId, String userId, 
			Date firstEntry) throws DBException {
		final String SELF = "HWRead.setAssgtEntryDate: ";
		Connection con = null;
		try {
			con = getPoolConnection();
			setAssgtEntryDate(con, hwId, userId, firstEntry);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
	} // setAssgtEntryDate(int, String, Date)

	/** Sets the date that the student entered an assignment.
	 * @param	con	database connection
	 * @param	hwId	assignment ID number
	 * @param	userId	login ID
	 * @param	firstEntry	the entry date
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void setAssgtEntryDate(Connection con, int hwId,
			String userId, Date firstEntry) throws SQLException {
		final String SELF = "HWRead.setAssgtEntryDate: ";
		final String qry = 
				UPDATE + ASSIGNED_QS
				+ SET + ASSGND_QS_ENTRY + EQUALS + QMARK
				+ WHERE + ASSGND_QS_HWID + EQUALS + QMARK
				+ AND + ASSGND_QS_STUDENT + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				dateToString(firstEntry),
				hwId,
				userId);
		debugPrint(SELF, sql_vals);
		tryUpdate(con, sql_vals);
	} // setAssgtEntryDate(Connection, int, String, Date)

	/** Replaces the list of questions that have been assigned to all
	 * students with a new list.
	 * @param	con	database connection
	 * @param	hwId	assignment ID number
	 * @param	qIds	new question ID numbers
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void rewriteAssignedQuestions(Connection con, int hwId,
			List<Integer> qIds) throws SQLException {
		rewriteAssignedQuestions(con, hwId, null, qIds);
	} // rewriteAssignedQuestions(Connection, int, List<Integer>)

	/** Replaces the list of questions that have been assigned to
	 * one or all students with a new list.
	 * @param	con	database connection
	 * @param	hwId	assignment ID number
	 * @param	studentId	login ID of one student, or null indicates we
	 * get list of students from database
	 * @param	qIds	new question ID numbers
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void rewriteAssignedQuestions(Connection con, int hwId,
			String studentId, List<Integer> qIds) throws SQLException {
		final String SELF = "HWWrite.rewriteAssignedQuestions: ";
		final List<String> studentIds = new ArrayList<String>();
		if (isEmpty(studentId)) {
			studentIds.addAll(getAssignedStudents(con, hwId));
		} else studentIds.add(studentId);
		if (!studentIds.isEmpty()) {
			final String qry = 
					DELETE_FROM + ASSIGNED_QS
					+ WHERE + ASSGND_QS_HWID + EQUALS + QMARK;
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
					hwId); 
			if (!isEmpty(studentId)) {
				sql_vals.addToSql(AND + ASSGND_QS_STUDENT + EQUALS + QMARK);
				sql_vals.addValue(studentId);
			} // if there's a student
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
			final String[] fields = new String[] {
					ASSGND_QS_STUDENT,
					ASSGND_QS_HWID,
					ASSGND_QS_ENTRY,
					ASSGND_QS_QNUM,
					ASSGND_QS_QID};
			final String insert = 
					getInsertIntoValuesQMarksSQL(ASSIGNED_QS, fields);
			for (final String eachUserId : studentIds) {
				storeAssignedQuestions(con, hwId, eachUserId, qIds, insert);
			} // for each user who needs a new instantiated assignment
		} // if have studentIds
	} // rewriteAssignedQuestions(Connection, int, String, List<Integer>)

	/** Gets the students who have an instantiated list of questions for an
	 * assignment.
	 * @param	con	database connection
	 * @param	hwId	assignment ID number
	 * @return	list of student login IDs
	 */
	private static List<String> getAssignedStudents(Connection con, 
			int hwId) {
		final String SELF = "HWWrite.getAssignedStudents: ";
		final String qry = 
				SELECT_UNIQUE + ASSGND_QS_STUDENT
				+ FROM + ASSIGNED_QS
				+ WHERE + ASSGND_QS_HWID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				hwId); 
		final List<String> studentIds = new ArrayList<String>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				studentIds.add(rs.getString(ASSGND_QS_STUDENT));
			} // for each studentId
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try
		return studentIds;
	} // getAssignedStudents(int)

	/** Adds to the database an assignments list intended to be used as a 
	 * template.
	 * @param	name	name of the assignment template
	 * @param	xml	XML defining the assignment template
	 * @throws	DBException	if the database can't be written to
	 */
	public static void addAssgtsTemplate(String name, String xml) 
			throws DBException {
		final String SELF = "HWWrite.addAssgtsTemplate: ";
		final String[] fields = new String[] {
				HWS4IMP_NAME, // CLOB field
				HWS4IMP_XML, // CLOB field
				HWS4IMP_ID};
		final String qry = toString(
				INSERT_INTO + HWSETS_4IMPORT, parens(fields),
					valuesQMarks(fields));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				name,
				xml);
		Connection con = null;
		try {
			con = getPoolConnection();
			sql_vals.addValue(nextSequence(con, HWSETS_4IMPORT_SEQ));
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // addAssgtsTemplate(String, String)

	/** Adds to the database a modified assignments list intended to be used 
	 * as a template.
	 * @param	id	ID number of the assignment template
	 * @param	name	name of the assignment template
	 * @param	xml	XML defining the assignment template
	 * @throws	DBException	if the database can't be written to
	 */
	public static void setAssgtsTemplate(int id, String name, String xml) 
			throws DBException {
		final String SELF = "HWWrite.setAssgtsTemplate: ";
		final String qry = toString(
				UPDATE + HWSETS_4IMPORT
				+ SET, equalsJoinQMarks(
					HWS4IMP_NAME, // CLOB field
					HWS4IMP_XML), // CLOB field
				WHERE + HWS4IMP_ID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				name,
				xml,
				id);
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
	} // setAssgtsTemplate(int, String, String)

	/** Deletes an assignment template.
	 * @param	id	ID number of the assignment template
	 * @throws	DBException	if the database can't be written to
	 */
	public static void deleteAssgtsTemplate(int id) throws DBException {
		final String SELF = "HWWrite.deleteAssgtsTemplate: ";
		final String[] fields = new String[] {
				HWS4IMP_NAME, // CLOB field
				HWS4IMP_XML, // CLOB field
				HWS4IMP_ID};
		final String qry = 
				DELETE_FROM + HWSETS_4IMPORT
				+ WHERE + HWS4IMP_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				id);
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
	} // deleteAssgtsTemplate(int)

	/** Constructor to disable external instantiation. */
	private HWWrite() { }

} // HWWrite
