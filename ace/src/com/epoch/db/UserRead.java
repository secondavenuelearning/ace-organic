package com.epoch.db;

import static com.epoch.db.dbConstants.CourseRWConstants.*;
import static com.epoch.db.dbConstants.LanguagesConstants.*;
import static com.epoch.db.dbConstants.ResponsesConstants.*;
import static com.epoch.db.dbConstants.TimePeriodConstants.*;
import static com.epoch.db.dbConstants.UserRWConstants.*;
import com.epoch.AppConfig;
import com.epoch.courseware.EnrollmentData;
import com.epoch.courseware.Institution;
import com.epoch.courseware.Name;
import com.epoch.courseware.User;
import com.epoch.exceptions.DBException;
import com.epoch.utils.SortUtils;
import com.epoch.utils.Utils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Database read-write operations related to students and instructors. */
public final class UserRead extends DBCommon {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}
	
	/** Parameter for getSameStudentData().  */
	private static final String ANY_STUDENT = null;
	/** Parameter for getSameStudentData().  */
	private static final int GET_STUDENTNUM = 0;
	/** Parameter for getAllNonstudents().  */
	public static final boolean GET_LANGUAGES = true;
	/** Parameter for getSameStudentData().  */
	private static final int GET_EMAIL = 1;
	/** Parameter for getAllRegdStudents().  */
	private static final Date NO_DATE = null;
	/** Parameter for getAllRegdStudents().  */
	private static final String NO_DATE_STR = null;

/* ***************** User read methods *****************/

	/** Gets all instructors and administrators. Called by AdminSession.
	 * @return	list of instructors and administrators
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<User> getAllNonstudents() throws DBException {
		final String whereSql = toString(
				WHERE + USER_ROLE + NOT_EQUALS, quotes(STUDENT));
		return getAllNonstudents(whereSql, !INSTN_TOO);
	} // getAllNonstudents()

	/** Gets all instructors and administrators. Called by AdminSession.
	 * @param	instnToo	when true, sort by role, school, name; otherwise,
	 * sort by role, name
	 * @return	list of instructors and administrators
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<User> getAllNonstudents(boolean instnToo) 
			throws DBException {
		final String whereSql = toString(
				WHERE + USER_ROLE + NOT_EQUALS, quotes(STUDENT));
		return getAllNonstudents(whereSql, instnToo);
	} // getAllNonstudents(boolean)

	/** Gets all unverified instructors.
	 * @return	list of unverified instructors
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<User> getAllUnverifiedInstructors() throws DBException {
		final String whereSql = toString(
				WHERE + USER_ROLE + EQUALS, quotes(INSTRUCTOR),
				AND, bitand(USER_FLAGS, 1), IS_ZERO);
		return getAllNonstudents(whereSql, INSTN_TOO);
	} // getAllUnverifiedInstructors()

	/** Gets list of instructors and administrators. Called by AdminSession.
	 * @param	whereSql	how to select the users
	 * @param	instnToo	when true, sort by role, school, name; otherwise,
	 * sort by role, name
	 * @return	list of instructors and administrators
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<User> getAllNonstudents(String whereSql, boolean instnToo) 
			throws DBException {
		final String SELF = "UserRead.getAllNonstudents: ";
		final List<User> users = new ArrayList<User>();
		final String qry = toString(
				SELECT, joinAll(
					USER_ID,
					USER_GIVENNAME,
					USER_MIDNAME,
					USER_SURNAME,
					USER_EMAIL,
					USER_TXT_MSG,
					USER_ROLE,
					USER_FLAGS,
					USER_SCHOOLID,
					USER_REGD,
					USER_LASTLOGIN,
					USER_PAYMENT,
					INSTN_NAME,
					INSTN_LANG,
					INSTN_GRACE,
					INSTN_STUDENTNUMLABEL,
					USER_STUDENTNUM),
				FROM + USERS + JOIN + INSTITUTIONS
					+ ON + USER_SCHOOLID + EQUALS + INSTN_ID,
				whereSql,
				ORDER_BY, instnToo
					? joinAll(
						USER_ROLE,
						clobToString(INSTN_NAME),
						USER_SORT)
					: joinAll(
						USER_ROLE,
						USER_SORT));
		debugPrint(SELF, qry);
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs.next()) {
				users.add(getOneUserData(rs, !GET_DETAILS));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return users;
	} // getAllNonstudents(String, boolean)

	/** Utility for retrieving data from an Oracle search.
	 * @param	rs	result of Oracle search
	 * @param	getDetails	whether to get the user's password, address, phone,
	 * text message email, registration date
	 * @return	a user
	 * @throws	SQLException	if there's a problem reading the results
	 */
	private static User getOneUserData(ResultSet rs, boolean getDetails) 
			throws SQLException {
		return getOneUserData(rs, getDetails, !GET_LANGUAGES);
	} // getOneUserData(ResultSet, boolean)

	/** Utility for retrieving data from an Oracle search.
	 * @param	rs	result of Oracle search
	 * @param	getDetails	whether to get the user's password, address, phone,
	 * text message email, registration date
	 * @param	retrieveLanguages	whether to get the user's languages
	 * @return	a user
	 * @throws	SQLException	if there's a problem reading the results
	 */
	private static User getOneUserData(ResultSet rs, boolean getDetails,
			boolean retrieveLanguages) throws SQLException {
		final String SELF = "UserRead.getOneUserData: ";
		final byte[] hashPass = (getDetails ? rs.getBytes(USER_PWDHASH)
				: new byte[0]);
		final User user = new User(rs.getString(USER_ID), hashPass, 
				retrieveLanguages);
		user.setName(new Name(rs.getString(USER_GIVENNAME),
				rs.getString(USER_MIDNAME),
				rs.getString(USER_SURNAME)));
		user.setRole(rs.getString(USER_ROLE).charAt(0));
		user.setFlags(rs.getInt(USER_FLAGS));
		final int instnId = rs.getInt(USER_SCHOOLID);
		final String instnName = rs.getString(INSTN_NAME);
		final String instnLang = rs.getString(INSTN_LANG);
		final String instnStudentNumLabel = rs.getString(INSTN_STUDENTNUMLABEL);
		final int gracePeriod = rs.getInt(INSTN_GRACE);
		user.setInstitution(new Institution(instnId, trimNullToEmpty(instnName), 
				instnLang, instnStudentNumLabel, gracePeriod));
		user.setStudentNum(Utils.trim(rs.getString(USER_STUDENTNUM)));
		user.setEmail(trimNullToEmpty(rs.getString(USER_EMAIL)));
		final String regDateStr = rs.getString(USER_REGD);
		user.setRegDate(regDateStr == null ? new Date() : toDate(regDateStr));
		final String lastLoginStr = rs.getString(USER_LASTLOGIN);
		user.setLastLoginDate(lastLoginStr == null 
				? new Date() : toDate(lastLoginStr));
		user.setPaymentTrackingId(Utils.trim(rs.getString(USER_PAYMENT)));
		if (getDetails) {
			final String addr = rs.getString(USER_ADDRESS);
			user.setAddress(trimNullToEmpty(addr));
			final String phone = rs.getString(USER_PHONE);
			user.setPhone(trimNullToEmpty(phone));
			final String txtMsgEmail = rs.getString(USER_TXT_MSG);
			user.setTextMessageEmail(trimNullToEmpty(txtMsgEmail));
		} // if getDetails
		return user;
	} // getOneUserData(ResultSet, boolean, boolean)

	/** Gets all students whose surname begins with the selector.
	 * Each object contains only id, name, student ID number. 
	 * Called by AdminSession.
	 * @param	selector	beginning of students' last names
	 * @return	list of students
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<User> getSelectedStudents(String selector)
			throws DBException {
		final String SELF = "UserRead.getSelectedStudents: ";
		final List<User> students = new ArrayList<User>();
		final String qry = toString(
				SELECT, joinAll(
					USER_ID,
					USER_GIVENNAME,
					USER_MIDNAME,
					USER_SURNAME,
					USER_EMAIL,
					USER_TXT_MSG,
					USER_ROLE,
					USER_FLAGS,
					USER_SCHOOLID,
					USER_REGD,
					USER_LASTLOGIN,
					USER_PAYMENT,
					INSTN_NAME,
					INSTN_LANG,
					INSTN_GRACE,
					INSTN_STUDENTNUMLABEL,
					USER_STUDENTNUM),
				FROM + USERS + JOIN + INSTITUTIONS
					+ ON + USER_SCHOOLID + EQUALS + INSTN_ID
				+ WHERE + USER_ROLE + EQUALS, quotes(STUDENT),
				AND, toUpper(USER_SURNAME),
					LIKE, toUpper(QMARK),
				ORDER_BY, joinAll(
					USER_SORT,
					clobToString(INSTN_NAME)));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				toString(selector, '%'));
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) students.add(getOneUserData(rs, !GET_DETAILS));
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return students;
	} // getSelectedStudents(String)

	/** Gets all EnrollmentData of students at a particular institution who are
	 * registered with ACE.  Each object contains only id, name, number, email. 
	 * @param	instnId	ID number of institution
	 * @param	userLangs	languages of the user
	 * @return	list of EnrollmentData of students at the institution
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<EnrollmentData> getAllRegdStudents(int instnId,
			String[] userLangs) throws DBException {
		return getAllRegdStudents(instnId, NO_DATE, userLangs);
	} // getAllRegdStudents(int, String[])

	/** Gets all EnrollmentData of students at a particular institution who are
	 * registered with ACE.  Each object contains only id, name, number, email. 
	 * @param	instnId	ID number of institution
	 * @param	numYears	number of years back from now in which students
	 * should have registered to be listed
	 * @param	userLangs	languages of the user
	 * @return	list of EnrollmentData of students at the institution
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<EnrollmentData> getAllRegdStudents(int instnId,
			int numYears, String[] userLangs) throws DBException {
		final Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.YEAR, -numYears);
		return getAllRegdStudents(instnId, calendar.getTime(), userLangs);
	} // getAllRegdStudents(int, int, String[])

	/** Gets all EnrollmentData of students at a particular institution who are
	 * registered with ACE.  Each object contains only id, name, number, email. 
	 * @param	instnId	ID number of institution
	 * @param	afterDate	earliest registration date for students to be
	 * retrieved
	 * @param	userLangs	languages of the user
	 * @return	list of EnrollmentData of students at the institution
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static List<EnrollmentData> getAllRegdStudents(int instnId,
			Date afterDate, String[] userLangs) throws DBException {
		final String SELF = "UserRead.getAllRegdStudents: ";
		final List<EnrollmentData> students = new ArrayList<EnrollmentData>();
		final String qry = toString(
				SELECT, joinAll(
					USER_ID,
					USER_GIVENNAME,
					USER_MIDNAME,
					USER_SURNAME,
					USER_EMAIL,
					USER_STUDENTNUM,
					USER_SCHOOLID,
					USER_REGD,
					INSTN_NAME,
					INSTN_LANG,
					INSTN_GRACE,
					INSTN_STUDENTNUMLABEL),
				FROM + USERS + JOIN + INSTITUTIONS
					+ ON + USER_SCHOOLID + EQUALS + INSTN_ID
				+ WHERE + USER_ROLE + EQUALS, quotes(STUDENT),
				AND + USER_SCHOOLID + EQUALS + QMARK
				+ AND + USER_ID + NOT + IN,
					parens(SELECT + EXAM_STUDENT + FROM + EXAM_IDS));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				instnId);
		if (afterDate != null) {
			sql_vals.addToSql(AND + USER_REGD + MORE_THAN + QMARK);
			sql_vals.addValue(dateToString(afterDate));
		} // if there's a date cutoff
		sql_vals.addToSql(ORDER_BY + USER_SORT);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			final boolean realphabetize = !Utils.isEmpty(userLangs)
					&& Utils.realphabetize(userLangs[0]);
			final List<Comparable<?>> sortKeys = new ArrayList<Comparable<?>>();
			while (rs.next())  {
				final EnrollmentData edata = getEnrollmentData(rs);
				final String name = edata.getName();
				debugPrint(SELF, name);
				students.add(edata);
				if (realphabetize) sortKeys.add(Utils.cersToUnicode(name));
			} // while there are results
			if (realphabetize) SortUtils.sort(students, sortKeys, userLangs[0]);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return students;
	} // getAllRegdStudents(int, Date, String[])

	/** Utility for retrieving EnrollmentData from an Oracle search.
	 * @param	rs	result of Oracle search
	 * @return	a populated EnrollmentData
	 * @throws	SQLException	if there's a problem reading the results
	 */
	private static EnrollmentData getEnrollmentData(ResultSet rs)
			throws SQLException {
		final EnrollmentData edata = new EnrollmentData();
		edata.setUserId(rs.getString(USER_ID));
		edata.setName(new Name(rs.getString(USER_GIVENNAME),
				rs.getString(USER_MIDNAME),
				rs.getString(USER_SURNAME)).toString());	
		edata.setStudentNum(trimNullToEmpty(rs.getString(USER_STUDENTNUM)));
		edata.setEmail(trimNullToEmpty(rs.getString(USER_EMAIL)));
		final int instnId = rs.getInt(USER_SCHOOLID);
		final String instnName = rs.getString(INSTN_NAME);
		final String instnLang = rs.getString(INSTN_LANG);
		final String instnStudentNumLabel = rs.getString(INSTN_STUDENTNUMLABEL);
		final int gracePeriod = rs.getInt(INSTN_GRACE);
		edata.setInstitution(new Institution(instnId, trimNullToEmpty(instnName), 
				instnLang, instnStudentNumLabel, gracePeriod));
		final String regDateStr = rs.getString(USER_REGD);
		edata.setRegDate(regDateStr == null ? new Date()
				: toDate(regDateStr)); // null registration date is legacy
		return edata;
	} // getEnrollmentData(ResultSet)

	/** Gets a user's information.
	 * @param	userId	login ID of user
	 * @return	the user, or null if not found
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static User getUser(String userId) throws DBException {
		if (Utils.isEmptyOrWhitespace(userId) 
				|| userId.indexOf('\'') >= 0) return null;
		final String where = WHERE + USER_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where, 
				userId);
		final List<User> users = getUsers(sql_vals, GET_DETAILS);
		debugPrint("UserRead.getUser: user ", userId, " is at ",
				users.get(0).getInstitution().getName());
		return (!Utils.isEmpty(users) ? users.get(0) : null);
	} // getUser(String)

	/** Gets whether a user from the default institution, with the given
	 * email address, and either an instructor role or the same student ID 
	 * number is in the database.
	 * @param	userId	login ID of user
	 * @param	email	email address of user
	 * @param	studentNum	student ID number
	 * @return	true if the specified user is in the database
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static boolean isRegdACEUser(String userId, String email, 
			String studentNum) throws DBException {
		final User user = getUser(userId);
		return user != null
				&& user.getInstitution() != null
				&& AppConfig.defaultInstitution.equals(
					user.getInstitution().getName())
				&& email.equals(user.getEmail())
				&& (user.getRole() == INSTRUCTOR 
					|| studentNum.equals(user.getStudentNum()));
	} // isRegdACEUser(String, String, String)

	/** Gets users' information.  Called from AdminSession.getUsers().
	 * @param	userIds	array of login ID of users
	 * @return	array of Users
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static User[] getUsers(String[] userIds) throws DBException {
		final String SELF = "UserRead.getUsers: ";
		debugPrint(SELF + "requesting ", userIds.length, " user(s).");
		if (Utils.isEmpty(userIds)) return new User[0];
		final List<User> users = new ArrayList<User>();
		final List<String[]> userIdGrps = getStrGroups(userIds);
		for (final String[] userIdGrp : userIdGrps) {
			final SQLWithQMarks sql_vals = new SQLWithQMarks(
					toString(WHERE + USER_ID + IN, parensQMarks(userIdGrp)));
			sql_vals.addValuesArray(userIdGrp);
			users.addAll(getUsers(sql_vals, GET_DETAILS));
		} // for each bunch of userIds
		final int numUsers = users.size();
		debugPrint(SELF + "returning ", numUsers, " user(s).");
		return users.toArray(new User[numUsers]);
	} // getUsers(String[])

	/** Gets users' information.  Called from CourseRW.getCoinstructors(),
	 * ForumRW.getTopicAuthors(), above.
	 * @param	whereSql_vals	how to select the users, and the values to 
	 * substitute for question marks
	 * @param	getDetails	whether to get the user's password and other details
	 * of this user
	 * @return	list of Users
	 * @throws	DBException	if there's a problem reading the database
	 */
	static List<User> getUsers(SQLWithQMarks whereSql_vals, 
			boolean getDetails) throws DBException {
		final String SELF = "UserRead.getUsers: ";
		final String qry = toString(
				SELECT, joinAll(
					USER_ID,
					USER_GIVENNAME,
					USER_MIDNAME,
					USER_SURNAME,
					USER_EMAIL,
					USER_TXT_MSG,
					USER_ROLE,
					USER_FLAGS,
					USER_SCHOOLID,
					USER_REGD,
					USER_LASTLOGIN,
					USER_PAYMENT,
					INSTN_NAME,
					INSTN_LANG,
					INSTN_GRACE,
					INSTN_STUDENTNUMLABEL,
					USER_STUDENTNUM,
					USER_ADDRESS,
					USER_PHONE,
					USER_PWDHASH),
				FROM + USERS + JOIN + INSTITUTIONS
					+ ON + USER_SCHOOLID + EQUALS + INSTN_ID,
				whereSql_vals.getSql(),
				ORDER_BY + USER_SORT);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, whereSql_vals);
		debugPrint(SELF, "getDetails = ", getDetails);
		debugPrint(SELF, sql_vals);
		final List<User> users = new ArrayList<User>();
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) 
				users.add(getOneUserData(rs, getDetails));
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return users;
	} // getUsers(SQLWithQMarks, boolean)

	/** Gets students who have not logged in since a particular date.  
	 * @param	y_m_d	int array of years, months, days
	 * @param	instnId	the ID of the institution, or all of them, whose 
	 * students have been inactive
	 * @return	array of usernames
	 * @throws	DBException	if there's a problem deleting from the database
	 */
	public static String[] getInactiveStudents(int[] y_m_d, int instnId) 
			throws DBException {
		final String SELF = "UserRead.getInactiveStudents: ";
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
		final String qry = toString(
				SELECT, joinAll(
					USER_ID,
					USER_LASTLOGIN),
				FROM + USERS 
				+ WHERE + USER_ROLE + EQUALS, quotes(STUDENT),
				AND + USER_LASTLOGIN + LESS_THAN + QMARK); 
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				dateToString(calendar.getTime()));
		if (instnId != ANY_INSTITUTION) {
			sql_vals.addToSql(AND + USER_SCHOOLID + EQUALS + QMARK);
			sql_vals.addValue(instnId);
		} // if not any institution
		sql_vals.addToSql(ORDER_BY + USER_LASTLOGIN);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final List<String> inactiveStudents = new ArrayList<String>();
		try {
			con = getPoolConnection(); 
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
			 	final String userId = rs.getString(USER_ID);
			 	final String lastLogin = rs.getString(USER_LASTLOGIN);
				debugPrint(SELF, userId, " last logged in ", lastLogin);
				inactiveStudents.add(userId);
			} // while there are results
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "caught SQLException.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		debugPrint(SELF, inactiveStudents.size(), " inactive students found.");
		return inactiveStudents.toArray(new String[inactiveStudents.size()]);
	} // getInactiveStudents(int[], int)

	/** Determines whether another user has the same login ID.
	 * @param	userId	proposed login ID
	 * @return	true if someone else has the same login ID
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static boolean loginExists(String userId) throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			return loginExists(con, userId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // loginExists(String)

	/** Determines whether another user has the same login ID.
	 * @param	con	database connection
	 * @param	userId	proposed login ID
	 * @return	true if someone else has the same login ID
	 * @throws	SQLException	if there's a problem reading the database
	 */
	static boolean loginExists(Connection con, String userId)
			throws SQLException {
		final String SELF = "UserRead.loginExists: ";
		final String qry = 
				SELECT + USER_ID
				+ FROM + USERS
				+ WHERE + USER_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				userId);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			return rs.next();
		} finally {
			closeStmtAndRs(stmt, rs);
		}
	} // loginExists(Connection, String)

	/** Determines whether there is a student with the given student number
	 * (not the userId) at the same institution.
	 * @param	studentNum	student's school ID number
	 * @param	instnId	ID number of institution
	 * @return	true if there is a student with the given student number
	 * at the same institution
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static boolean studentNumExists(String studentNum,
			int instnId) throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			final String[] studentNames = getSameStudentData(con, 
					studentNum, instnId, ANY_STUDENT, GET_STUDENTNUM);
			return studentNames != null;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // studentNumExists(String, int)

	/** Determines whether there is a student with the given student number
	 * (not the userId) at the same institution.
	 * @param	con	database connection
	 * @param	studentNum	student's school ID number
	 * @param	instnId	ID number of institution
	 * @return	names of students with the given student number
	 * at the same institution, null otherwise
	 * @throws	SQLException	if there's a problem reading the database
	 */
	static String[] getSameStudentNums(Connection con, String studentNum, 
			int instnId) throws SQLException {
		return getSameStudentData(con, studentNum, instnId,
				ANY_STUDENT, GET_STUDENTNUM);
	} // getSameStudentNums(Connection, String, int)

	/** Determines whether there is a student with the given student number
	 * (not the userId) at the same institution.
	 * @param	con	database connection
	 * @param	studentNum	student's school ID number
	 * @param	instnId	ID number of institution
	 * @param	userId	login ID of user, or null to
	 * return any user
	 * @return	names of students with the given student number
	 * at the same institution, null otherwise
	 * @throws	SQLException	if there's a problem reading the database
	 */
	static String[] getSameStudentNums(Connection con, String studentNum, 
			int instnId, String userId) throws SQLException {
		return getSameStudentData(con, studentNum, instnId,
				userId, GET_STUDENTNUM);
	} // getSameStudentNums(Connection, String, int, String)

	/** Determines whether there is a student with the given email address
	 * at the same institution.  
	 * @param	con	database connection
	 * @param	email	student's email address
	 * @param	instnId	ID number of institution
	 * @return	names of students with the given email address
	 * at the same institution, null otherwise
	 * @throws	SQLException	if there's a problem reading the database
	 */
	static String[] getSameStudentEmails(Connection con, String email, 
			int instnId) throws SQLException {
		return getSameStudentData(con, email, instnId,
				ANY_STUDENT, GET_EMAIL);
	} // getSameStudentEmails(Connection, String, int)

	/** Determines whether there is a student with the given email address
	 * at the same institution.  
	 * @param	con	database connection
	 * @param	email	student's email address
	 * @param	instnId	ID number of institution
	 * @param	userId	login ID of user, or null to
	 * return any user
	 * @return	names of students with the given email address
	 * at the same institution, null otherwise
	 * @throws	SQLException	if there's a problem reading the database
	 */
	static String[] getSameStudentEmails(Connection con, String email, 
			int instnId, String userId) throws SQLException {
		return getSameStudentData(con, email, instnId,
				userId, GET_EMAIL);
	} // getSameStudentEmails(Connection, String, int, String)

	/** Determines whether there is a student with the given student number
	 * or email at the same institution OTHER than the one with the given userId
	 * (unless userId is null).
	 * @param	con	database connection
	 * @param	data	student's school ID number or email address
	 * @param	instnId	ID number of institution
	 * @param	userId	login ID of user, or null to
	 * return any user
	 * @param	type	GET_STUDENTNUM or GET_EMAIL
	 * @return	array of names of students with the given student number
	 * or email address at the same institution
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static String[] getSameStudentData(Connection con, String data,
			int instnId, String userId, int type) throws SQLException {
		final String SELF = "UserRead.getSameStudentData: ";
		final List<String> names = new ArrayList<String>();
		final String qry = toString(
				SELECT, joinAll(
					USER_ID,
					USER_GIVENNAME,
					USER_MIDNAME,
					USER_SURNAME),
				FROM + USERS
				+ WHERE, type == GET_STUDENTNUM ? USER_STUDENTNUM : USER_EMAIL, 
					LIKE + QMARK
				+ AND + USER_SCHOOLID + EQUALS + QMARK
				+ AND + USER_ROLE + EQUALS, quotes(STUDENT));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				data.trim(),
				instnId);
		if (userId != ANY_STUDENT) {
			sql_vals.addToSql(AND + USER_ID + NOT_EQUALS + QMARK);
			sql_vals.addValue(userId);
		} // if there's a user
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final String name = new Name(rs.getString(USER_GIVENNAME),
						rs.getString(USER_MIDNAME),
						rs.getString(USER_SURNAME)).toString().trim();	
				debugPrint("UserRead.getSameStudentData: student ",
						quotes(name), " wit ", rs.getString(USER_ID),
						" at institution with ID ", instnId, " has ", 
						type == GET_STUDENTNUM ? "studentNum " : "email ", 
						quotes(data));
				names.add(name);
			}
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try
		return (names.isEmpty() ? null
				: names.toArray(new String[names.size()]));
	} // getSameStudentData(Connection, String, int, String, int)

	/** Determines whether the login ID is a temporary one for an exam.
	 * @param	userId	proposed login ID
	 * @return	true if the login ID is a temporary one for an exam
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static boolean isExamStudent(String userId) throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			return isExamStudent(con, userId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // isExamStudent(String)

	/** Determines whether the login ID is a temporary one for an exam.
	 * @param	con	database connection
	 * @param	userId	proposed login ID
	 * @return	true if the login ID is a temporary one for an exam
	 * @throws	SQLException	if there's a problem reading the database
	 */
	static boolean isExamStudent(Connection con, String userId)
			throws SQLException {
		final String SELF = "UserRead.isExamStudent: ";
		final String qry = 
				SELECT + EXAM_STUDENT
				+ FROM + EXAM_IDS
				+ WHERE + EXAM_STUDENT + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				userId);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			return rs.next();
		} finally {
			closeStmtAndRs(stmt, rs);
		}
	} // isExamStudent(Connection, String)

	/** Get temporary exam IDs associated with a course.
	 * @param	courseId	ID of the course
	 * @param	unused	whether to get only unused IDs (without associated
	 * responses)
	 * @return	an array of login IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[] getExamIds(int courseId, boolean unused)
			throws DBException {
		final String SELF = "UserRead.getExamIds: ";
		final String qry = 
				SELECT + EXAM_STUDENT
				+ FROM + EXAM_IDS
				+ WHERE + EXAM_CRS + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				courseId);
		if (unused) {
			sql_vals.addToSql(
					AND + EXAM_STUDENT + NOT + IN, parensBuild(
						SELECT + RESP_STUDENT
						+ FROM + RESPONSES
						+ WHERE + RESP_HWID + IN, parensBuild(
							SELECT + HW_ID
							+ FROM + HWSETS
							+ WHERE + HW_COURSE + EQUALS + QMARK)));
			sql_vals.addValue(courseId);
		} // if should get only unused IDs
		sql_vals.addToSql(ORDER_BY + EXAM_STUDENT);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			final List<String> userIds = new ArrayList<String>();
			while (rs.next()) userIds.add(rs.getString(EXAM_STUDENT));
			return (userIds.toArray(new String[userIds.size()]));
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
	} // getExamIds(int, boolean)

	/** Get the date a temporary exam ID was created.
	 * @param	userId	login of the student
	 * @return	date of creation, or null if not found
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Date getDateCreated(String userId) throws DBException {
		final String SELF = "UserRead.getDateCreated: ";
		final String qry = 
				SELECT + EXAM_CREATED
				+ FROM + EXAM_IDS
				+ WHERE + EXAM_STUDENT + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				userId);
		debugPrint(SELF, sql_vals);
		Date created = null;
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				final String createdStr = rs.getString(EXAM_CREATED);
				debugPrint(SELF + "createdStr = '", createdStr, "'");
				created = toDate(createdStr);
			}
		} catch (SQLException e) {
			alwaysPrint(SELF + "caught SQLException while "
					+ "retrieving creation date.");
			e.printStackTrace();
		} finally {
			closeConnection(con, stmt, rs);
		}
		return created;
	} // getDateCreated(String)

	/** Gets whether ACE should show a user calculated synthesis products in the
	 * feedback.
	 * @param	userId	login ID of the user
	 * @return	true if ACE should show the user calculated synthesis products
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static boolean maySeeSynthCalcdProds(String userId) 
			throws DBException {
		final String SELF = "UserRead.maySeeSynthCalcdProds: ";
		final String qry = toString(
				SELECT + USER_ID
				+ FROM + USERS 
				+ WHERE + USER_ID + EQUALS + QMARK
				+ AND, bitand(USER_FLAGS, DONT_SHOW_CALCD_SYNTH_PRODS), IS_ZERO);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				userId);
		debugPrint(SELF, sql_vals);
		boolean maySee = true;
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			maySee = rs.next();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		return maySee;
	} // maySeeSynthCalcdProds(String)

	/** Gets the userId for a student at the institution and with the 
	 * student ID number.
	 * @param	instnId	the institution ID
	 * @param	studentNum	the student number at that institution
	 * @return	the userId
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static String getUserId(int instnId, String studentNum) 
			throws DBException {
		final String SELF = "UserWrite.getUserId: ";
		String userId = null;
		final String qry = 
				SELECT + USER_ID + FROM + USERS 
				+ WHERE + USER_SCHOOLID + EQUALS + QMARK
				+ AND + USER_STUDENTNUM + LIKE + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				instnId,
				studentNum);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			int numResults = 0;
			while (rs.next()) {
				userId = rs.getString(USER_ID);
				numResults++;
			}
			if (numResults != 1) {
				throw new DBException(SELF + numResults 
						+ " result(s) obtained with institution "
						+ instnId + " and student ID number " + studentNum);
			} // if not 1 result
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		debugPrint(SELF + "userId = ", userId);
		return userId;
	} // getUserId(int, String)

/* ***************** Language functions *****************/

	/** Gets an alphabetized array of all languages stored in database. 
	 * @return	all languages stored in database, alphabetized
	 */
	public static String[] getAllLanguages() {
		final String SELF = "UserRead.getAllLanguages: ";
		final String qry = toString(
				SELECT + LANG_NAME + AS + SRCH_RESULT
				+ FROM + LANGUAGES
				+ UNION + SELECT + INSTN_LANG
				+ FROM + INSTITUTIONS
				+ WHERE, length(INSTN_LANG), IS_POSITIVE
				+ UNION, TranslnRead.SELECT_ALL_LANGS());
		debugPrint(SELF, qry);
		final List<String> allLangsList = new ArrayList<String>();
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs.next()) {
				final String language = rs.getString(SRCH_RESULT);
				if (!Utils.isEmpty(language) && !ENGLISH.equals(language)) {
					allLangsList.add(language);
				} // if there is a language
			} // while there are more languages
		} catch (SQLException e) {
			alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		final String[] allLanguages = 
				allLangsList.toArray(new String[allLangsList.size()]);
		final SortIgnoreCase sortRule = new SortIgnoreCase();
		Arrays.sort(allLanguages, sortRule);
		debugPrint(SELF + "returning ", allLanguages);
		return allLanguages;
	} // getAllLanguages()

	/** Gets languages preferred by this user in order of preference.
	 * @param	userId	login ID of the user
	 * @return	this user's languages in order of preference
	 */
	public static String[] getLanguages(String userId) {
		final Map<String, String[]> languagesMap = 
				getLanguages(new String[] {userId});
		return languagesMap.get(userId);
	} // getLanguages(String)

	/** Gets languages preferred by these users in order of preference.
	 * @param	userIds	login IDs of the users
	 * @return	map of array of the users' languages (in order of preference) 
	 * keyed by userIDs
	 */
	public static Map<String, String[]> getLanguages(String[] userIds) {
		final String SELF = "UserRead.getLanguages: ";
		Connection con = null;
		Map<String, String[]> languagesMap = new HashMap<String, String[]>();
		try {
			con = getPoolConnection();
			languagesMap = getLanguages(con, userIds);
		} catch (SQLException e) {
			debugPrint(SELF + "SQLException when looking for languages of ",
					userIds);
		} catch (DBException e) {
			debugPrint(SELF + "DBException when looking for languages of ",
					userIds);
		} finally {
			closeConnection(con);
		} // try
		return languagesMap;
	} // getLanguages(String[])

	/** Gets languages preferred by users in order of preference and
	 * stores them in the User object.
	 * @param	users	the users
	 */
	public static void getLanguages(User[] users) {
		final String SELF = "UserRead.getLanguages: ";
		final List<String> userIds = new ArrayList<String>();
		for (final User user : users) {
			userIds.add(user.getUserId());
		} // for each user
		final Map<String, String[]> languagesMap = getLanguages( 
				userIds.toArray(new String[userIds.size()]));
		for (final User user : users) {
			final String userId = user.getUserId();
			final String[] userLangs = languagesMap.get(userId);
			debugPrint(SELF + "user ", userId, " prefers languages ", 
					userLangs);
			user.setLanguages(userLangs);
		} // for each user
	} // getLanguages(User[])

	/** Gets languages preferred by this user in order of preference.
	 * @param	con	database connection
	 * @param	userId	login ID of the user
	 * @return	this user's languages in order of preference
	 */
	static String[] getLanguages(Connection con, String userId) {
		final String SELF = "UserRead.getLanguages: ";
		String[] languages = new String[0];
		try {
			final Map<String, String[]> languagesMap = 
					getLanguages(con, new String[] {userId});
			languages = languagesMap.get(userId);
		} catch (DBException e) {
			debugPrint(SELF + "DBException when looking for languages of ",
					userId);
		} // try
		return languages;
	} // getLanguages(Connection, String)

	/** Gets languages preferred by these users in order of preference.
	 * @param	con	database connection
	 * @param	userIds	login IDs of the users
	 * @return	map of array of the users' languages (in order of preference) 
	 * keyed by userIDs
	 * @throws	DBException	if the database can't be read
	 */
	static Map<String, String[]> getLanguages(Connection con, 
			String[] userIds) throws DBException {
		final String SELF = "UserRead.getLanguages: ";
		final String qry = toString(
				SELECT, joinAll(
					LANG_USER,
					LANG_NAME),
				FROM + LANGUAGES
				+ WHERE + LANG_USER + IN, parensQMarks(userIds),
				ORDER_BY, joinAll(
					LANG_USER,
					LANG_PREF));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
		sql_vals.addValuesArray(userIds);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final Map<String, String[]> usersLangs = 
				new LinkedHashMap<String, String[]>();
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) while (!rs.isAfterLast()) {
				// iterate for each user and language 
				final String userName = rs.getString(LANG_USER);
				final List<String> oneUserLangs = new ArrayList<String>();
				while (!rs.isAfterLast()
						&& userName.equals(rs.getString(LANG_USER))) {
					final String language = rs.getString(LANG_NAME);
					if (!Utils.isEmpty(language)) {
						oneUserLangs.add(language);
					} // if there is a language
					rs.next();
				} // while there are more languages
				debugPrint(SELF + "user ", userName, " has languages ",
						oneUserLangs);
				usersLangs.put(userName, oneUserLangs.toArray(
						new String[oneUserLangs.size()]));
			} // while there are more languages
		} catch (SQLException e) {
			alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try
		return usersLangs;
	} // getLanguages(Connection, String[])

/* ***************** Security question functions *****************/

	/** Gets the security questions and answers for a user as a three-membered
	 * array, with [0] containing question numbers x:y.
	 * @param	userId	login ID of the user
	 * @return	question numbers (in format m:n) and answers
	 */
	public static String[] getSecurityAnswers(String userId) {
		final String SELF = "UserRead.getSecurityAnswers: ";
		final String qry = toString(
				SELECT, joinAll(
					USER_SEC_QS,
					USER_SEC_ANS1,
					USER_SEC_ANS2),
				FROM + USERS
				+ WHERE + USER_ID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				userId);
		final String[] secQsAndAnswers = getSecurityAnswers(sql_vals);
		if (Utils.membersAreEmpty(secQsAndAnswers)) {
			debugPrint(SELF + "no security questions or answers found for ", 
					userId);
		} else {
			debugPrint(SELF + "returning 2 security questions and answers for ", 
					userId, ": ", Arrays.toString(secQsAndAnswers));
		} // if length
		return secQsAndAnswers;
	} // getSecurityAnswers(String)

	/** Gets the security questions and answers for a user as a three-membered
	 * array, with [0] containing question numbers x:y.
	 * @param	instnId	the institution ID
	 * @param	studentNum	the student number at that institution
	 * @return	question numbers (in format m:n) and answers
	 */
	public static String[] getSecurityAnswers(int instnId, String studentNum) {
		final String SELF = "UserRead.getSecurityAnswers: ";
		final String qry = toString(
				SELECT, joinAll(
					USER_SEC_QS,
					USER_SEC_ANS1,
					USER_SEC_ANS2),
				FROM + USERS
				+ WHERE + USER_SCHOOLID + EQUALS + QMARK
				+ AND + USER_STUDENTNUM + LIKE + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				instnId,
				studentNum);
		final String[] secQsAndAnswers = getSecurityAnswers(sql_vals);
		if (Utils.membersAreEmpty(secQsAndAnswers)) {
			debugPrint(SELF + "no security questions or answers found for "
					+ "user of institution ", instnId, " and studentNum ", 
					studentNum);
		} else {
			debugPrint(SELF + "returning 2 security questions and answers for "
					+ "user of institution ", instnId, " and studentNum ", 
					studentNum, ": ", Arrays.toString(secQsAndAnswers));
		} // if length
		return secQsAndAnswers;
	} // getSecurityAnswers(int, String)

	/** Gets the security questions and answers for a user as a three-membered
	 * array, with [0] containing question numbers x:y.
	 * @param	sql_vals	SQL for getting the security questions and answers
	 * @return	question numbers (in format m:n) and answers
	 */
	private static String[] getSecurityAnswers(SQLWithQMarks sql_vals) {
		final String SELF = "UserRead.getSecurityAnswers: ";
		String[] secQsAndAnswers = new String[3];
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) { // one result
				secQsAndAnswers[0] = rs.getString(USER_SEC_QS);
				if (!Utils.isEmpty(secQsAndAnswers[0])) {
					secQsAndAnswers[1] = rs.getString(USER_SEC_ANS1);
					secQsAndAnswers[2] = rs.getString(USER_SEC_ANS2);
				} else {
					secQsAndAnswers = new String[0];
				} // if there are answers to security questions
			} // if there is such a user
		} catch (SQLException e) {
			alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
		} finally {
			closeConnection(con, stmt, rs);
		} // try
		return secQsAndAnswers;
	} // getSecurityAnswers(SQLWithQMarks)

	/** Disables external instantiation. */
	private UserRead() { }

} // UserRead
