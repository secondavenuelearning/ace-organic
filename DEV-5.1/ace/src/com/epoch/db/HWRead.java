package com.epoch.db;

import static com.epoch.db.dbConstants.CourseRWConstants.*;
import static com.epoch.db.dbConstants.ResponsesConstants.*;
import com.epoch.assgts.Assgt;
import com.epoch.assgts.assgtConstants.AssgtConstants;
import com.epoch.assgts.AssgtQGroup;
import com.epoch.evals.evalConstants.EvalResultConstants;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.NonExistentException;
import com.epoch.qBank.Question;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.utils.Utils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Sets, retrieves assignments during assembly or for homework session.  */
public final class HWRead extends DBCommon 
		implements AssgtConstants, EvalResultConstants, QuestionConstants {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Default value for the number of questions in an assignment. */
	public static final int UNSIZED = -1;
	/** Parameter for getQuestions().  */
	public final static String LOAD_ALL_QS = null;
	/** Parameter for getQuestions().  */
	public static final String ANY_STUDENT = null;
	/** Parameter for getQuestions().  */
	public static final boolean DONT_CARE_IF_STUDENT = false;
	/** Parameter for getQuestions().  */
	public static final boolean FULL_LOAD = true;

/* *********************** Reading Assgts ******************/

	/** Read assignment with the given ID.
	 * @param	hwId	assignment ID number
	 * @return	an assignment description
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Assgt getHW(int hwId) throws DBException {
		final SQLWithQMarks sql_vals = new SQLWithQMarks(
				WHERE + HW_ID + EQUALS + QMARK, hwId);
		return getHWs(sql_vals).get(0);
	} // getHW(int)

	/** Read assignments with the given IDs.
	 * @param	hwIds	array of assignment ID numbers
	 * @return	array of assignments
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Assgt[] getHWs(int[] hwIds) throws DBException {
		if (Utils.isEmpty(hwIds)) return new Assgt[0];
		final String qry = toString(
				WHERE + HW_ID + IN, parensQMarks(hwIds));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				hwIds);
		final List<Assgt> assgts = getHWs(sql_vals);
		return assgts.toArray(new Assgt[assgts.size()]);
	} // getHWs(int[])

	/** Gets assignments (properties only) of a course.
	 * @param	courseId	unique ID of the course
	 * @return	list of assignments
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<Assgt> getHWs(int courseId) throws DBException {
		return getHWs(new SQLWithQMarks(WHERE + HW_COURSE + EQUALS + QMARK, 
				courseId));
	} // getHWs(int)

	/** Read assignments.
	 * @param	sql_vals	how to select the assignments to get, and values to
	 * substitute for question marks
	 * @return	list of assignments
	 * @throws	DBException	if there's a problem reading the database
	 */
	static List<Assgt> getHWs(SQLWithQMarks sql_vals) throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			return getHWs(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // getHWs(SQLWithQMarks)

	/** Read assignments.
	 * @param	con	database connection
	 * @param	whereSql_vals	how to select the assignments to get, and values to
	 * substitute for question marks
	 * @return	list of assignments
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static List<Assgt> getHWs(Connection con, 
			SQLWithQMarks whereSql_vals) throws DBException {
		final String SELF = "HWRead.getHWs: ";
		final String qry = toString(
				SELECT, joinAll(
					HW_ID, 
					HW_COURSE,
					CRS_INSTRUCTOR,
					HWSETS + DOT + HW_NAME,
					HW_REMARKS,
					HW_CREATED,
					HW_DUE,
					HW_DURATION,
					HW_TRIES,
					HW_MAXEXT,
					HW_DEPEND,
					HWSETS + DOT + HW_FLAGS,
					HWSETS + DOT + HW_NUM),
				FROM + HWSETS 
					+ JOIN + COURSES 
					+ ON + HW_COURSE + EQUALS + CRS_ID,
				whereSql_vals.getSql(),
				ORDER_BY + HWSETS + DOT + HW_NUM);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, whereSql_vals);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final List<Assgt> assgts = new ArrayList<Assgt>();
		final List<Integer> hwIds = new ArrayList<Integer>();
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final Assgt assgt = new Assgt();
				assgt.id = rs.getInt(HW_ID);
				assgt.courseId = rs.getInt(HW_COURSE);
				assgt.instructorId = rs.getString(CRS_INSTRUCTOR);
				assgt.setName(rs.getString(HW_NAME));
				assgt.setRemarks(rs.getString(HW_REMARKS));
				if (NULL.equals(assgt.getRemarks())) assgt.setRemarks("");
				assgt.creationDate = toDate(rs.getString(HW_CREATED));
				assgt.setDueDate(toDate(rs.getString(HW_DUE)));
				assgt.setDuration(rs.getInt(HW_DURATION));
				assgt.setMaxTries(rs.getInt(HW_TRIES));
				assgt.setMaxExtensionStr(rs.getString(HW_MAXEXT));
				assgt.setDependsOnId(rs.getInt(HW_DEPEND));
				assgt.setFlags(rs.getInt(HW_FLAGS));
				assgt.setNoChanges();
				assgts.add(assgt);
				debugPrint(SELF + "assgt ", rs.getInt(HW_NUM),
						" has ID ", assgt.id, " and name ",
						assgt.getName(), " and duration ",
						assgt.getDuration());
				hwIds.add(Integer.valueOf(assgt.id));
			} // while
			debugPrint(SELF + "got assignments ", hwIds);
			if (!hwIds.isEmpty()) {
				setAssgtQGroups(con, assgts, hwIds);
				setAllowedRxnCondns(con, assgts, hwIds);
				setGradingParams(con, assgts, hwIds);
				setExtensions(con, assgts, hwIds);
			} // if there are assignments
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (NonExistentException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		return assgts;
	} // getHWs(Connection, SQLWithQMarks)

	/** Get the complete questions of the assignment.
	 * @param	assgt	an assignment
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	NonExistentException	if the assignment doesn't exist
	 */
	public static void getHW(Assgt assgt) 
			throws DBException, NonExistentException {
		Connection con = null;
		try {
			con = getPoolConnection();
			getHW(con, assgt);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // getHW(Assgt)

	/** Get the assignment with complete questions.
	 * @param	con	database connection
	 * @param	assgt	an assignment
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	SQLException	if there's a problem reading the database
	 * @throws	NonExistentException	if the assignment doesn't exist
	 */
	private static void getHW(Connection con, Assgt assgt) 
			throws SQLException, NonExistentException, DBException {
		final String SELF = "HWRead.getHW: ";
		final List<Integer> allQIds = assgt.getQIds();
		// load the master questions
		final SQLWithQMarks masterSel = 
				new SQLWithQMarks(parensQMarks(allQIds), allQIds);
		debugPrint(SELF + "masterSel = ", masterSel);
		final Map<Integer, Question> masterSet =
				QuestionRW.getQuestionsMap(con, masterSel,
					getTables(!LOCAL), assgt.instructorId, ADD_HEADER);
		// load the local questions
		final DBLocalTables localTables = (DBLocalTables) getTables(LOCAL);
		final String localSelSql = toString(masterSel.getSql(), 
				AND, DBLocalTables.Q_AUTHOR, EQUALS + QMARK);
		final SQLWithQMarks localSel =
				new SQLWithQMarks(localSelSql, allQIds, assgt.instructorId);
		debugPrint(SELF + "localSel = ", localSel);
		final Map<Integer, Question> localSet =
				QuestionRW.getQuestionsMap(con, localSel,
					localTables, assgt.instructorId, ADD_HEADER);
		final int numQGrps = assgt.getNumGroups();
		debugPrint(SELF + "qList has ", numQGrps, " groups.");
		for (int qGrpNum = numQGrps; qGrpNum > 0; qGrpNum--) {
			final AssgtQGroup qGrp = assgt.getQGroup(qGrpNum);
			int numGrpQs = qGrp.getNumQs();
			for (int grpQNum = numGrpQs; grpQNum > 0; grpQNum--) {
				final Integer qId = 
						Integer.valueOf(qGrp.getQId(grpQNum));
				// first check if there is a local copy of the question
				Question hwQ = localSet.get(qId);
				if (hwQ == null) { // check the master table
					hwQ = masterSet.get(qId);
				} // if Q not in local table
				if (hwQ != null) {
					// replace empty Q in qList with filled Q
					qGrp.setQ(grpQNum, hwQ);
					debugPrint(SELF + "placed populated Q ", qId,
							" in group ", qGrpNum, " position ", 
							grpQNum);
				} else {
					debugPrint(SELF + "could not find Q with ID ", qId,
							"; removing it from the list.");
					assgt.removeGroupQ(qGrpNum, grpQNum);
					numGrpQs--;
				} // if hwQ is not null
			} // for each question within a group (1 if not random)
		} // for each question
	} // getHW(Connection, Assgt)

	/** Get allowed reaction conditions for all the assignments in the list.
	 * @param	con	database connection
	 * @param	assgts	assignments
	 * @param	hwIds	assignment IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static void setAllowedRxnCondns(Connection con, 
			List<Assgt> assgts, List<Integer> hwIds) throws DBException {
		final String SELF = "HWRead.setAllowedRxnCondns: ";
		PreparedStatement stmt = null;
		ResultSet rs = null;
		if (!Utils.isEmpty(hwIds)) try {
			final String qry = toString(
					SELECT, joinAll(
						HWRXNCONDN_HWID,
						HWRXNCONDN_RXNCOND_ID),
					FROM + HWRXNCONDNS
					+ WHERE + HWRXNCONDN_HWID + IN, parensQMarks(hwIds),
					ORDER_BY + joinAll(
						HWRXNCONDN_HWID,
						HWRXNCONDN_RXNCOND_ID));
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
					hwIds);
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) while (!rs.isAfterLast()) {
				final int hwId = rs.getInt(HWRXNCONDN_HWID);
				final int posn = hwIds.indexOf(Integer.valueOf(hwId));
				final Assgt assgt = assgts.get(posn);
				final List<Integer> rxnCondnIds = new ArrayList<Integer>();
				while (!rs.isAfterLast()
						&& hwId == rs.getInt(HWRXNCONDN_HWID)) {
					final int rxnCondnId = rs.getInt(HWRXNCONDN_RXNCOND_ID);
					rxnCondnIds.add(Integer.valueOf(rxnCondnId));
					rs.next();
				} // while still in the same assignment
				debugPrint(SELF + "for assignment ", hwId, ", got "
						+ "reaction conditions ", rxnCondnIds);
				assgt.setAllowedRxnCondns(
						Utils.listToIntArray(rxnCondnIds));
			} // while there are still results
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
	} // setAllowedRxnCondns(Connection, List<Assgt>, List<Integer>)

	/** Get time- and attempt-dependent grading parameters of all assignments
	 * in the list.
	 * @param	con	database connection
	 * @param	assgts	assignments
	 * @param	hwIds	assignment IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static void setGradingParams(Connection con, 
			List<Assgt> assgts, List<Integer> hwIds) throws DBException {
		final String SELF = "HWRead.setGradingParams: ";
		PreparedStatement stmt = null;
		ResultSet rs = null;
		if (!Utils.isEmpty(hwIds)) try {
			final String qry = toString(
					SELECT, joinAll(
						GRADING_HWID,
						GRADING_TYPE,
						GRADING_MAX_STR,
						GRADING_FACTOR_STR),
					FROM + GRADING_PARAMS
					+ WHERE + GRADING_HWID + IN, parensQMarks(hwIds),
					ORDER_BY, joinAll(
						GRADING_HWID,
						GRADING_TYPE,
						GRADING_MAX));
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
					hwIds);
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) while (!rs.isAfterLast()) {
				final int hwId = rs.getInt(GRADING_HWID);
				final int posn = hwIds.indexOf(Integer.valueOf(hwId));
				final Assgt assgt = assgts.get(posn);
				while (!rs.isAfterLast()
						&& hwId == rs.getInt(GRADING_HWID)) {
					final char type = rs.getString(GRADING_TYPE).charAt(0);
					final List<String> limits = new ArrayList<String>();
					final List<String> factors = new ArrayList<String>();
					while (!rs.isAfterLast()
							&& hwId == rs.getInt(GRADING_HWID)
							&& type == rs.getString(GRADING_TYPE).charAt(0)) {
						final String limit = rs.getString(GRADING_MAX_STR);
						limits.add(NO_MAXIMUM.equals(limit) ? "" : limit);
						factors.add(rs.getString(GRADING_FACTOR_STR));
						rs.next();
					} // while there are more parameters of this type
					debugPrint(SELF + "for assignment ", hwId, ", got "
							+ "grading params of type ", type, " with "
							+ "limits ", limits, " and factors ", factors);
					final int typeNum = Utils.indexOf(DB_PARAM_TYPES, type);
					if (typeNum >= 0) assgt.setGradingParams(new String[][] {
								limits.toArray(new String[limits.size()]),
								factors.toArray(new String[factors.size()]) 
							}, typeNum); // artifact from remaining 'Q'
				} // while still in the same assignment
			} // while there are still results
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
	} // setGradingParams(Connection, List<Assgt>, List<Integer>)

	/** Get all extensions for all assignments in the list.
	 * @param	con	database connection
	 * @param	assgts	assignments
	 * @param	hwIds	assignment IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static void setExtensions(Connection con, List<Assgt> assgts,
			List<Integer> hwIds) throws DBException {
		final Map<Integer, LinkedHashMap<String, String>> allExtensions =
				getExtensions(con, hwIds);
		for (final Assgt assgt : assgts) {
			final Map<String, String> extensions =
					allExtensions.get(Integer.valueOf(assgt.id));
			if (!Utils.isEmpty(extensions)) {
				assgt.setExtensions(extensions);
			} // if there are extensions for this assignment
		} //  for each description
	} // setExtensions(Connection, List<Assgt>, List<Integer>)

	/** Get one assignment's extensions.
	 * @param	con	database connection
	 * @param	hwId	assignment ID
	 * @return	map of extensions keyed by userId, or null if none found
	 * @throws	DBException	if there's a problem reading the database
	 */
	static Map<String, String> getExtensions(Connection con, int hwId) 
			throws DBException {
		final List<Integer> hwIds = new ArrayList<Integer>();
		hwIds.add(Integer.valueOf(hwId));
		final Map<Integer, LinkedHashMap<String, String>> allExtensions =
				getExtensions(con, hwIds);
		final Map<String, String> hwExtensions = 
				allExtensions.get(Integer.valueOf(hwId));
		return (hwExtensions != null ? hwExtensions
				: new LinkedHashMap<String, String>());
	} // getExtensions(Connection, int)

	/** Read assignment extensions.
	 * @param	con	database connection
	 * @param	hwIds	assignment IDs
	 * @return maps of maps of extensions keyed by userId, keyed by hwId
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static Map<Integer, LinkedHashMap<String, String>>  
			getExtensions(Connection con, List<Integer> hwIds) 
			throws DBException {
		final String SELF = "HWRead.getExtensions: ";
		final Map<Integer, LinkedHashMap<String, String>> allExtensions =
				new HashMap<Integer, LinkedHashMap<String, String>>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		if (!Utils.isEmpty(hwIds)) try {
			final String qry = toString(
					SELECT, joinAll(
						EXT_HWID,
						EXT_STUDENT,
						EXT_EXTENSION_STR),
					FROM + EXTENSIONS
					+ WHERE + EXT_HWID + IN, parensQMarks(hwIds),
					ORDER_BY, joinAll(
						EXT_HWID,
						EXT_NUM));
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
					hwIds);
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) while (!rs.isAfterLast()) {
				final int hwId = rs.getInt(EXT_HWID);
				final LinkedHashMap<String, String> extensions = 
						new LinkedHashMap<String, String>();
				while (!rs.isAfterLast()
						&& hwId == rs.getInt(EXT_HWID)) {
					extensions.put(
							rs.getString(EXT_STUDENT),
							rs.getString(EXT_EXTENSION_STR));
					rs.next();
				} // while still in the same assignment
				allExtensions.put(Integer.valueOf(hwId), extensions);
			} // while there are still results
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		debugPrint(SELF + "returning ", allExtensions);
		return allExtensions;
	} // getExtensions(Connection, List<Integer>)

	/** Gets the sum of all the extensions of a student in a course.
	 * @param	studentId	login ID of the student
	 * @param	courseId	the course ID
	 * @return	sum of all the extensions of a student in a course
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static double getSumExtensions(String studentId, int courseId) 
			throws DBException {
		final String SELF = "HWRead.getSumExtensions: ";
		final String qry = toString(
				SELECT, sum(EXT_EXTENSION), AS + SRCH_RESULT
				+ FROM + EXTENSIONS
				+ WHERE + EXT_STUDENT + EQUALS + QMARK
				+ AND + EXT_HWID + IN, parens(
					SELECT + HW_ID  
					+ FROM + HWSETS 
					+ WHERE + HW_COURSE + EQUALS + QMARK));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				studentId, 
				courseId);
		debugPrint(SELF, sql_vals);
		double sumExtensions = 0.0;
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) sumExtensions = rs.getDouble(SRCH_RESULT);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return sumExtensions;
	} // getSumExtensions(String, int)

	/** Gets the start of an SQL query to get the hwIds for a group of courses.
	 * @return	start of an SQL query to get the hwIds for a group of courses
	 */
	public static String getHwIdsForCourses() {
		return SELECT + HW_ID  
				+ FROM + HWSETS 
				+ WHERE + HW_COURSE + IN;
	} // getHwIdsForCourses()

	/** Gets lists of assignments intended to be used as templates.
	 * @return	array of 3-membered arrays of assignment id, name, and XML
	 * @throws	DBException	if the database can't be read
	 */
	public static String[][] getAssgtsTemplates() throws DBException {
		final String SELF = "HWRead.getAssgtsTemplates: ";
		final String qry = toString(
				SELECT + ALL + FROM + HWSETS_4IMPORT
				+ ORDER_BY, clobToString(HWS4IMP_NAME));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
		debugPrint(SELF, sql_vals);
		final List<String[]> assgtTemplates = new ArrayList<String[]>();
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				assgtTemplates.add(new String[] {
						toString(rs.getInt(HWS4IMP_ID)),
						rs.getString(HWS4IMP_NAME),
						rs.getString(HWS4IMP_XML)
						});
			} // while
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		return assgtTemplates.toArray(new String[assgtTemplates.size()][]); 
	} // getAssgtsTemplates()

	/** Gets the XML of a template list of assignments.
	 * @param	id	the template ID number
	 * @return	the assignment XML
	 * @throws	DBException	if the database can't be read
	 */
	public static String getAssgtsTemplate(int id) throws DBException {
		final String SELF = "HWRead.getAssgtsTemplate: ";
		final String qry = toString(
				SELECT + HWS4IMP_XML + FROM + HWSETS_4IMPORT
				+ WHERE + HWS4IMP_ID + EQUALS + QMARK
				+ ORDER_BY, clobToString(HWS4IMP_NAME));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				id);
		debugPrint(SELF, sql_vals);
		String assgtsTemplate = "";
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) { // only one result possible
				assgtsTemplate = rs.getString(HWS4IMP_XML);
			} // while
			debugPrint(SELF + "got assgtsTemplate with length ",
					Utils.getLength(assgtsTemplate));
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		return assgtsTemplate;
	} // getAssgtsTemplate(int)

/* **************** Reading assignment questions ******************/

	/** Gets the question groups (ID numbers only) of a list of assignments.
	 * @param	con	database connection
	 * @param	assgts	array of assignments
	 * @param	hwIds	assignment IDs
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	SQLException	if there's a problem reading the database
	 * @throws	NonExistentException	if an assignment doesn't exist
	 */
	private static void setAssgtQGroups(Connection con, List<Assgt> assgts,
			List<Integer> hwIds) 
			throws SQLException, NonExistentException, DBException {
		final Map<Integer, ArrayList<AssgtQGroup>> qGrpsMap =
				getAssgtQGroups(con, hwIds);
		final List<Integer> hwIdGroupKeys = 
				new ArrayList<Integer>(qGrpsMap.keySet());
		for (final Integer hwId : hwIdGroupKeys) {
			final int posn = hwIds.indexOf(hwId);
			assgts.get(posn).setQGroups(qGrpsMap.get(hwId));
		} // for each list of question groups
	} // setAssgtQGroups(Connection, Assgt[])

	/** Gets the question groups (ID numbers only) of an assignment.
	 * @param	con	database connection
	 * @param	hwId	assignment ID number
	 * @return	list of question groups of the assignment
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	SQLException	if there's a problem reading the database
	 * @throws	NonExistentException	if the assignment doesn't exist
	 */
	private static List<AssgtQGroup> getAssgtQGroups(Connection con, int hwId) 
			throws SQLException, NonExistentException, DBException {
		final Integer hwIdObj = Integer.valueOf(hwId);
		final List<Integer> hwIds = new ArrayList<Integer>();
		hwIds.add(hwIdObj);
		return getAssgtQGroups(con, hwIds).get(hwIdObj);
	} // getAssgtQGroups(Connection, int)

	/** Get the lists of question groups (question IDs only) of a list of 
	 * assignments.
	 * @param	con	database connection
	 * @param	hwIds	assignment ID numbers
	 * @return	map of lists of question groups, keyed by assignment ID
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	SQLException	if there's a problem reading the database
	 * @throws	NonExistentException	if an assignment doesn't exist
	 */
	private static Map<Integer, ArrayList<AssgtQGroup>> 
			getAssgtQGroups(Connection con, List<Integer> hwIds) 
			throws SQLException, NonExistentException, DBException {
		final String SELF = "HWRead.getAssgtQGroup: ";
		final Map<Integer, ArrayList<AssgtQGroup>> qGrpsMap =
				new HashMap<Integer, ArrayList<AssgtQGroup>>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		if (!Utils.isEmpty(hwIds)) try {
			final String qry = toString(
					SELECT, joinAll(
						HWQS_HWID,
						HWQS_GRP_NUM,
						HWQS_GRP_PICK,
						HWQS_BUNDLE_SIZE,
						HWQS_QID,
						HWQS_PTS_STR,
						HWQS_DEPENDS),
					FROM + HW_QS
					+ WHERE + HWQS_HWID + IN, parensQMarks(hwIds),
					ORDER_BY, joinAll(
						HWQS_HWID,
						HWQS_GRP_NUM,
						HWQS_QNUM));
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
					hwIds);
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				throw new NonExistentException("Homeworks "
						+ hwIds.toString() + " do not exist");
			} else while (!rs.isAfterLast()) {
				final int hwId = rs.getInt(HWQS_HWID);
				final ArrayList<AssgtQGroup> qGrps = new ArrayList<AssgtQGroup>();
				while (!rs.isAfterLast()
						&& hwId == rs.getInt(HWQS_HWID)) {
					final int grpNum = rs.getInt(HWQS_GRP_NUM);
					final int pick = rs.getInt(HWQS_GRP_PICK);
					final int bundleSize = rs.getInt(HWQS_BUNDLE_SIZE);
					final String ptsStr = rs.getString(HWQS_PTS_STR);
					final int dependsOn = rs.getInt(HWQS_DEPENDS);
					final List<Integer> qIds = new ArrayList<Integer>();
					while (!rs.isAfterLast()
							&& hwId == rs.getInt(HWQS_HWID)
							&& grpNum == rs.getInt(HWQS_GRP_NUM)) {
						qIds.add(Integer.valueOf(rs.getInt(HWQS_QID)));
						rs.next();
					} // while in the same group
					final int[] qIdsArr = Utils.listToIntArray(qIds);
					/* debugPrint(SELF + "qGroup ", grpNum, " with pick ",
							pick, " and bundle size ", bundleSize,
							" has qIds ", qIdsArr); /**/
					final AssgtQGroup qGrp = new AssgtQGroup(qIdsArr);
					qGrp.setPick(pick);
					qGrp.setBundleSize(bundleSize);
					qGrp.setPts(ptsStr);
					qGrp.setDependsOn(dependsOn);
					qGrps.add(qGrp);
				} // while there are more Qs in this assignment
				qGrpsMap.put(Integer.valueOf(hwId), qGrps);
			} // while there are more assignments
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try
		return qGrpsMap;
	} // getAssgtQGroups(Connection, List<Integer>) 

/* *********************** Homework session methods ******************/

	/** For homework session; get the list of questions for this assignment.
	 * @param	hwId	assignment ID number
	 * @param	userId	login ID
	 * @return	array of questions
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	NonExistentException	if the assignment doesn't exist
	 */
	public static Question[] getQuestions(int hwId, String userId)
			throws DBException, NonExistentException {
		return getQuestions(hwId, userId, LOAD_ALL_QS, NO_LANGUAGES,
				DONT_CARE_IF_STUDENT, FULL_LOAD);
	} // getQuestions(int, String)

	/** For homework session; get the list of questions for this assignment.
	 * @param	hwId	assignment ID number
	 * @param	userId	login ID
	 * @param	languages	languages of this user in order of preference; null
	 * if English only
	 * @param	isStudent	true if this request is for a student's assignment
	 * @return	array of questions
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	NonExistentException	if the assignment doesn't exist
	 */
	public static Question[] getQuestions(int hwId, String userId,
			String[] languages, boolean isStudent)
			throws DBException, NonExistentException {
		return getQuestions(hwId, userId, LOAD_ALL_QS, languages,
				isStudent, FULL_LOAD);
	} // getQuestions(int, String, String[], boolean)

	/** For gradebook session (show response) via homework session; get one 
	 * particular question in this assignment.
	 * @param	hwId	assignment ID number
	 * @param	userId	login ID
	 * @param	qNumStr	null if should load all questions in this
	 * assignment, or serial number of single question to load
	 * @param	languages	languages of this user in order of preference; null
	 * if English only
	 * @return	array of questions
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	NonExistentException	if the assignment doesn't exist
	 */
	public static Question[] getQuestions(int hwId, String userId,
			String qNumStr, String[] languages)
			throws DBException, NonExistentException {
		final boolean fullLoad = qNumStr == LOAD_ALL_QS;
		return getQuestions(hwId, userId, qNumStr, languages,
				DONT_CARE_IF_STUDENT, fullLoad);
	} // getQuestions(int, String, String, String[])

	/** For homework or gradebook session; get all questions or one particular
	 * question in this assignment.
	 * @param	hwId	assignment ID number
	 * @param	userId	login ID
	 * @param	qNumStr	null if should load all questions in this
	 * assignment, or serial number of single question to load
	 * @param	languages	languages of this user in order of preference; null
	 * if English only
	 * @param	isStudent	true if this request is for a student's assignment
	 * @param	fullLoad	true if the entire question should be loaded;
	 * otherwise, only flags and question data are loaded
	 * @return	array of questions
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	NonExistentException	if the assignment doesn't exist
	 */
	public static Question[] getQuestions(int hwId, String userId,
			String qNumStr, String[] languages, boolean isStudent, 
			boolean fullLoad) throws DBException, NonExistentException {
		final String SELF = "HWRead.getQuestions: ";
		Connection con = null;
		int qNum = 0;
		boolean previouslyAssigned = false;
		final boolean loadAllQs = qNumStr == LOAD_ALL_QS; // i.e., null
		try {
			con = getPoolConnection();
			int[] qIdsToLoad = new int[1];
			List<Integer> qIdsAssigned = new ArrayList<Integer>();
			debugPrint(SELF + "isStudent = ", isStudent);
			if (isStudent && loadAllQs) {
				// Load previously assigned questions if they exist
				qIdsToLoad = getAssignedQIds(con, hwId, userId);
				debugPrint(SELF + "qIdsToLoad.length = ", qIdsToLoad.length);
				if (!isEmpty(qIdsToLoad) && qIdsToLoad[0] != 0) {
					debugPrint(SELF + "qIdsToLoad[0] = ", qIdsToLoad[0]);
					previouslyAssigned = true;
					debugPrint(SELF + "student's previously assigned "
							+ "Qs are ", qIdsToLoad);
					qIdsAssigned = Utils.intArrayToList(qIdsToLoad);
				} else debugPrint(SELF + "student has no previously "
						+ "assigned Qs.");
			} // if is a student and loading all Qs in an assignment
			if (!previouslyAssigned) {
				final List<AssgtQGroup> qGroups = getAssgtQGroups(con, hwId);
				int numGrp = 0;
				for (final AssgtQGroup qGroup : qGroups) {
					final List<Integer> instantiatedQIds = 
							qGroup.instantiate();
					debugPrint(SELF + "instantiated qGroup ", 
							++numGrp, " to give ", instantiatedQIds);
					qIdsAssigned.addAll(instantiatedQIds);
				} // for each qGroup
				if (!qIdsAssigned.isEmpty()) {
					if (loadAllQs) {
						qIdsToLoad = Utils.listToIntArray(qIdsAssigned);
						if (isStudent) {
							debugPrint(SELF + "storing newly assigned Qs ", 
									qIdsAssigned);
							HWWrite.storeAssignedQuestions(con, hwId, userId, 
									qIdsAssigned);
						} // if a student
					} else { // load only a single Q given by qNumStr
						qNum = Integer.parseInt(qNumStr);
						qIdsToLoad[0] = qIdsAssigned.get(qNum - 1).intValue();
						debugPrint(SELF + "qNumStr = ", qNumStr,
								", qIdsAssigned = ", qIdsAssigned, 
								", qIdsToLoad[0] = ", qIdsToLoad[0]);
					} // if only one Q is wanted
				} // if studentQStr is not empty or null
			} // if not previously assigned
			final String instructorId = getHWInstructor(con, hwId);
			// load master set
			final SQLWithQMarks sql_vals = 
					new SQLWithQMarks(parensQMarks(qIdsToLoad));
			sql_vals.addValuesArray(qIdsToLoad);
			debugPrint(SELF + "master sql_vals = ", sql_vals);
			final Map<Integer, Question> masterQs =
					QuestionRW.getQuestionsMap(con, sql_vals, 
						getTables(!LOCAL), instructorId, fullLoad, 
						ADD_HEADER, languages);
			// load local Set
			final DBLocalTables localTables = (DBLocalTables) getTables(LOCAL);
			sql_vals.addToSql(AND, DBLocalTables.Q_AUTHOR, EQUALS + QMARK);
			sql_vals.addValue(instructorId);
			debugPrint(SELF + "local sql_vals = ", sql_vals);
			final Map<Integer, Question> localQs =
					QuestionRW.getQuestionsMap(con, sql_vals, localTables, 
						instructorId, fullLoad, ADD_HEADER, languages);
			final int numAssignedQs = qIdsAssigned.size();
			Question[] questions = new Question[numAssignedQs];
			if (loadAllQs) {
				// Actual questions loaded may be fewer than the ids stored.
				// Unlikely now that when Qs are deleted from the DB, their IDs are
				// also deleted from all assignments, but better safe than sorry
				final List<Question> existentQs = new ArrayList<Question>();
				for (int hwQNum = 0; hwQNum < qIdsToLoad.length; hwQNum++) {
					final Integer qId = Integer.valueOf(qIdsToLoad[hwQNum]);
					Question hwQ = localQs.get(qId);
					if (hwQ == null) hwQ = masterQs.get(qId);
					questions[hwQNum] = hwQ;
					if (hwQ != null) existentQs.add(hwQ);
					else alwaysPrint(SELF + "ERROR: Question ",
							qId, " cannot be loaded for instructorid = ",
							instructorId, "; ignored.");
				} // for each stored qId
				final int actualCount = existentQs.size();
				debugPrint(SELF + "actualCount = ", actualCount,
						", numAssignedQs (desiredCount) = ", numAssignedQs);
				if (actualCount != numAssignedQs) {
					questions = existentQs.toArray(new Question[actualCount]);
				} // if some questions don't exist
			} else {
				// set the index of the requested Q to the one Q acquired
				final Integer qId = Integer.valueOf(qIdsToLoad[0]);
				Question hwQ = localQs.get(qId);
				if (hwQ == null) hwQ = masterQs.get(qId);
				questions[qNum - 1] = hwQ;
			} // if loading all Qs
			return questions;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
	} // getQuestions(int, String, String, String[], boolean, boolean)

	/** Gets the userId of the instructor who owns an assignment.
	 * @param	con	database connection
	 * @param	hwId	id of the assignment whose values to retrieve
	 * @return	instructor's userId
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	NonExistentException	if the assignment doesn't exist
	 */
	static String getHWInstructor(Connection con, int hwId) 
			throws DBException, NonExistentException {
		final String SELF = "HWRead.getHWInstructor: ";
		final String qry = toString(
				SELECT + COURSES + DOT + CRS_INSTRUCTOR 
				+ FROM, joinAll(
					COURSES,
					parens(
						SELECT + HW_COURSE
						+ FROM + HWSETS
						+ WHERE + HW_ID + EQUALS + QMARK)), ' ', SRCH_RESULT,
				WHERE + COURSES + DOT + CRS_ID
					+ EQUALS + SRCH_RESULT + DOT + HW_COURSE);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				hwId);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (!rs.next())
				throw new NonExistentException(toString("Homework (",
						hwId, ") does not exist"));
			return rs.getString(CRS_INSTRUCTOR);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try
	} // getHWInstructor(Connection, int)

	/** Get the unique IDs of the questions that have been assigned to a
	 * student.
	 * @param	hwId	assignment ID number
	 * @param	userId	login ID
	 * @return	array of unique question IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int[] getAssignedQIds(int hwId, String userId) 
			throws DBException {
		final String SELF = "HWRead.getAssignedQIds: ";
		Connection con = null;
		try {
			con = getPoolConnection();
			return getAssignedQIds(con, hwId, userId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
	} // getAssignedQIds(int, String)

	/** Get the unique IDs of the questions that have been assigned to a
	 * student.
	 * @param	con	database connection
	 * @param	hwId	assignment ID number
	 * @param	userId	login ID
	 * @return	array of unique question IDs
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static int[] getAssignedQIds(Connection con, int hwId,
			String userId) throws SQLException {
		final String SELF = "HWRead.getAssignedQIds: ";
		final List<Integer> qIds = new ArrayList<Integer>();
		final String qry = 
				SELECT + ASSGND_QS_QID 
				+ FROM + ASSIGNED_QS
				+ WHERE + ASSGND_QS_HWID + EQUALS + QMARK
				+ AND + ASSGND_QS_STUDENT + EQUALS + QMARK
				+ ORDER_BY + ASSGND_QS_QNUM;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				hwId,
				userId);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				qIds.add(Integer.valueOf(rs.getInt(ASSGND_QS_QID)));
			} // while there are results
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		return Utils.listToIntArray(qIds);
	} // getAssignedQIds(Connection, int, String)

	/** Get the date that the student entered an assignment.
	 * @param	hwId	assignment ID number
	 * @param	userId	login ID
	 * @return	the entry date
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Date getAssgtEntryDate(int hwId, String userId) 
			throws DBException {
		final String SELF = "HWRead.getAssgtEntryDate: ";
		Connection con = null;
		try {
			con = getPoolConnection();
			return getAssgtEntryDate(con, hwId, userId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
	} // getAssgtEntryDate(int, String)

	/** Get the date that the student entered an assignment.
	 * @param	con	database connection
	 * @param	hwId	assignment ID number
	 * @param	userId	login ID
	 * @return	the entry date
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static Date getAssgtEntryDate(Connection con, int hwId,
			String userId) throws SQLException {
		final String SELF = "HWRead.getAssgtEntryDate: ";
		Date entry = null;
		final String qry = 
				SELECT_UNIQUE + ASSGND_QS_ENTRY
				+ FROM + ASSIGNED_QS
				+ WHERE + ASSGND_QS_HWID + EQUALS + QMARK
				+ AND + ASSGND_QS_STUDENT + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				hwId,
				userId);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				final String entryStr = rs.getString(ASSGND_QS_ENTRY);
				debugPrint(SELF + "found first entry for ", userId,
						" in assignment with ID ", hwId, ": ", entryStr);
				entry = toDate(entryStr);
			} else debugPrint(SELF + "no first entry found for ", userId,
					" in assignment with ID ", hwId);
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try
		return entry;
	} // getAssgtEntryDate(Connection, int, String)

	/** Check if assignment has been viewed by any students.
	* @param	hwId	assignment ID number
	* @return	true if assignment has been viewed by any students.
	 * @throws	DBException	if there's a problem reading the database
	*/
	public static boolean assignmentViewed(int hwId)
			throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			return assignmentViewed(con, hwId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
	} // assignmentViewed(int)

	/** Check if assignment has been viewed by any students.
	 * @param	con	database connection
	 * @param	hwId	assignment ID number
	 * @return	true if assignment has been viewed by any students.
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static boolean assignmentViewed(Connection con, int hwId)
			throws SQLException {
		final String SELF = "HWRead.assignmentViewed: ";
		final String qry = toString(
				SELECT + RESP_STATUS
				+ FROM + RESPONSES
				+ WHERE + RESP_STATUS + NOT_EQUALS, quotes(INITIALIZED),
				AND + RESP_HWID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				hwId);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				debugPrint(SELF, "returned false");
				return false;
			}
			debugPrint(SELF, "returned true");
			return true;
		} finally {
			closeStmtAndRs(stmt, rs);
		}
	} // assignmentViewed(Connection, int)

	/** Refreshes the due date and extensions of the assignment. Called by
	 * HWSession for exams.
	 * @param	assgt	assignment
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static void refreshDueData(Assgt assgt) throws DBException {
		final String SELF = "HWRead.refreshDueData: ";
		final String qry = 
				SELECT + HW_DUE
				+ FROM + HWSETS
				+ WHERE + HW_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				assgt.id);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				assgt.setDueDate(toDate(rs.getString(HW_DUE)));
				assgt.setNoChanges();
			}
			final List<Assgt> assgts = new ArrayList<Assgt>();
			assgts.add(assgt);
			final List<Integer> hwIds = new ArrayList<Integer>();
			hwIds.add(Integer.valueOf(assgt.id));
			setExtensions(con, assgts, hwIds);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
	} // refreshDueData(Assgt)

	/** Gets all the assignment IDs.
	 * @return list of assignment IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<Integer> getHWIds() throws DBException {
		final String qry = 
				SELECT + HW_ID 
				+ FROM + HWSETS 
				+ ORDER_BY + HW_ID;
		debugPrint("HWRead.getHWIds: ", qry);
		final List<Integer> hwIds = new ArrayList<Integer>();
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs.next()) hwIds.add(Integer.valueOf(rs.getInt(HW_ID)));
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return hwIds;
	} // getHWIds()

	/** Gets the SQL to get the number of questions seen by each student in the
	 * given assignment.
	 * @param	hwId	assignment ID number
	 * @param	srchResultName	name of a search result returned by query
	 * @return	SQL and values to get the number of questions seen by the 
	 * students
	 */
	static SQLWithQMarks GET_NUM_QS_SEEN_SQL(int hwId, String srchResultName) {
		return GET_NUM_QS_SEEN_SQL(new int[] {hwId}, srchResultName);
	} // GET_NUM_QS_SEEN_SQL(int, String)

	/** Gets the SQL to get the number of questions seen by each student in the
	 * given assignments.
	 * @param	hwIds	array of assignment ID numbers
	 * @param	srchResultName	name of a search result returned by query
	 * @return	SQL to get the number of questions seen by the students
	 */
	static SQLWithQMarks GET_NUM_QS_SEEN_SQL(int[] hwIds, 
			String srchResultName) {
		final String sql = toString(
				SELECT, joinAll(
					HWQS_HWID,
					getBuilder(sum(HWQS_GRP_PICK + TIMES + HWQS_BUNDLE_SIZE),
						AS, srchResultName)),
				FROM + HW_QS
				+ WHERE + HWQS_QNUM + IS_1
				+ AND + HWQS_HWID + IN, parensQMarks(hwIds), 
				GROUP_BY + HWQS_HWID);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(sql, 
				hwIds);
		return sql_vals;
	} // GET_NUM_QS_SEEN_SQL(int[], String)

	/** Constructor to disable external instantiation. */
	private HWRead() { }

} // HWRead

