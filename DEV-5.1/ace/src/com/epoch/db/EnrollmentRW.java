package com.epoch.db;

import static com.epoch.db.dbConstants.CourseRWConstants.*;
import static com.epoch.db.dbConstants.ResponsesConstants.*;
import static com.epoch.db.dbConstants.UserRWConstants.*;
import com.epoch.AppConfig;
import com.epoch.courseware.Course;
import com.epoch.courseware.EnrollmentData;
import com.epoch.courseware.Institution;
import com.epoch.courseware.Name;
import com.epoch.courseware.User;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.ParameterException;
import com.epoch.utils.SortUtils;
import com.epoch.utils.Utils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/** Database read-write methods regarding enrollment of students in courses. */
public final class EnrollmentRW extends DBCommon {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

/* **************** Methods to get enrolled students *****************/

	/** Gets students enrolled in a course, including TAs and unregistered
	 * students, ordered by last name and given name or by student ID number.
	 * @param	courseId	unique ID of this course
	 * @param	sortByStudentNum	whether to sort students by student ID
	 * numbers
	 * @param	userLangs	languages of this user
	 * @return	list of EnrollmentData
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<EnrollmentData> getEnrolledStudents(int courseId,
			boolean sortByStudentNum, String[] userLangs) throws DBException {
		return getEnrolledStudents(courseId, INCLUDE_TAS,
				INCLUDE_UNREGISTERED, sortByStudentNum, userLangs);
	} // getEnrolledStudents(int, boolean, String[])

	/** Gets userIds of registered students, excluding TAs, enrolled in a course, 
	 * ordered by last name and given name or by student ID number.
	 * @param	courseId	unique ID of this course
	 * @param	sortByStudentNum	whether to sort students by student ID
	 * numbers
	 * @return	list of user IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	static List<String> getEnrolledUserIds(int courseId, 
			boolean sortByStudentNum) throws DBException {
		final List<EnrollmentData> enrolledStudents = 
				getEnrolledStudents(courseId, !INCLUDE_TAS, 
					!INCLUDE_UNREGISTERED, sortByStudentNum, null);
		final List<String> enrolledUserIds = new ArrayList<String>();
		for (final EnrollmentData student : enrolledStudents) {
			enrolledUserIds.add(student.getUserId());
		} // for each enrolled student
		return enrolledUserIds;
	} // getEnrolledUserIds(int, boolean)

	/** Gets email addresses used to receive text messages of students enrolled 
	 * in a course.
	 * @param	courseId	unique ID of this course
	 * @return	array of text message email addresses
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[] getEnrolledUserTextMessageEmails(int courseId) 
			throws DBException {
		final List<EnrollmentData> enrolledStudents = 
				getEnrolledStudents(courseId, INCLUDE_TAS, 
					!INCLUDE_UNREGISTERED, false, null);
		final List<String> textMsgEmails = new ArrayList<String>();
		for (final EnrollmentData student : enrolledStudents) {
			final String textMsgEmail = student.getTextMessageEmail();
			if (!Utils.isEmpty(textMsgEmail)) {
				textMsgEmails.add(textMsgEmail);
			} // if not empty or null
		} // for each enrolled student
		return textMsgEmails.toArray(new String[textMsgEmails.size()]);
	} // getEnrolledUserTextMessageEmails(int)

	/** Gets students enrolled in a course in alphabetical order by
	 * family name and given name or by student number.
	 * @param	courseId	unique ID of this course
	 * @param	includeTAs	whether to include this course's TAs in the list
	 * @param	includeUnregd	whether to include students not registered with
	 * ACE
	 * @param	sortByStudentNum	whether to sort students by student ID
	 * numbers
	 * @param	userLangs	languages of this user
	 * @return	list of EnrollmentData
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<EnrollmentData> getEnrolledStudents(int courseId, 
			boolean includeTAs, boolean includeUnregd, boolean sortByStudentNum,
			String[] userLangs) throws DBException {
		final String SELF = "EnrollmentRW.getEnrolledStudents: ";
		debugPrint(SELF + "includeTAs = ", includeTAs, ", includeUnregd = ",
				includeUnregd, ", sortByStudentNum = ", sortByStudentNum);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final List<EnrollmentData> students = new ArrayList<EnrollmentData>();
		try {
			con = getPoolConnection();
			final boolean realphabetize = includeUnregd 
					|| (!Utils.isEmpty(userLangs)
						&& Utils.realphabetize(userLangs[0]));
			final List<Comparable<?>> sortKeys = new ArrayList<Comparable<?>>();
			if (includeUnregd) {
				final String qry = toString(
						SELECT, joinAll(
							PREENRL_STUDENTNUM,
							PREENRL_SCHOOLID,
							INSTN_NAME,
							INSTN_STUDENTNUMLABEL,
							PREENRL_STUDENTNAME), 
						FROM + PREENROLLMENT
							+ JOIN + INSTITUTIONS
								+ ON + PREENRL_SCHOOLID + EQUALS + INSTN_ID
						+ WHERE + PREENRL_COURSE + EQUALS + QMARK
						+ AND, clobToString(PREENRL_STUDENTNUM), 
							NOT + IN, parensBuild(
								SELECT, clobToString(USER_STUDENTNUM), 
								FROM + USERS 
								+ WHERE + USER_ID + IN, parensBuild(
									SELECT + ENRL_STUDENT
									+ FROM + ENROLLMENT
									+ WHERE + ENRL_COURSE + EQUALS + QMARK)), 
						ORDER_BY, sortByStudentNum 
							? joinAll(
								clobToString(PREENRL_STUDENTNUM),
								clobToString(INSTN_NAME),
								clobToString(PREENRL_STUDENTNAME))
							: joinAll(
								clobToString(PREENRL_STUDENTNAME),
								clobToString(INSTN_NAME),
								clobToString(PREENRL_STUDENTNUM)));
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
						courseId, 
						courseId);
				debugPrint(SELF, sql_vals);
				stmt = getStatement(con, sql_vals);
				rs = stmt.executeQuery();
				while (rs.next()) {
					final EnrollmentData student = getPreenrollmentData(rs);
					final String name = student.getName();
					debugPrint(SELF + "preenrolled student ", name, 
							" with student ID '", student.getStudentNum(), "'.");
					students.add(student);
					sortKeys.add(sortByStudentNum 
							? toString(student.getStudentNum(), ' ',
								student.getInstitution().getName(), ' ',
								Utils.cersToUnicode(name))
							: toString(Utils.cersToUnicode(name), ' ',
								student.getInstitution().getName(), ' ',
								student.getStudentNum()));
				} // while there are more students enrolled but not registered
			} // if include unregistered
			final String qry = toString(
					SELECT, joinAll(
						USER_STUDENTNUM,
						USER_GIVENNAME,
						USER_MIDNAME,
						USER_SURNAME,
						USER_EMAIL,
						USER_TXT_MSG,
						USER_SCHOOLID,
						USER_REGD,
						INSTN_NAME,
						INSTN_STUDENTNUMLABEL,
						USER_ID),
					!includeTAs ? "" : postjoin(ENRL_IS_TA),
 					FROM + ENROLLMENT + " INNER " + JOIN + USERS
						+ ON + USER_ID + EQUALS + ENRL_STUDENT
					+ JOIN + INSTITUTIONS
						+ ON + USER_SCHOOLID + EQUALS + INSTN_ID
					+ WHERE + ENRL_COURSE + EQUALS + QMARK,
					includeTAs ? "" : getBuilder(
 						AND, parensBuild(
							ENRL_IS_TA + IS_NULL 
							+ OR + ENRL_IS_TA + EQUALS + N)),
					ORDER_BY, sortByStudentNum 
						? joinAll(
							clobToString(USER_STUDENTNUM),
							clobToString(INSTN_NAME),
							USER_SORT)
						: joinAll(
							USER_SORT,
							clobToString(INSTN_NAME),
							clobToString(USER_STUDENTNUM)));
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
					courseId);
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final EnrollmentData student = getEnrollmentData(rs, includeTAs);
				final String name = student.getName();
				debugPrint(SELF + "enrolled student ", name, 
						" has student ID '", student.getStudentNum(), "'.");
				students.add(student);
				if (realphabetize) {
					sortKeys.add(sortByStudentNum 
							? toString(student.getStudentNum(), ' ',
								student.getInstitution().getName(), ' ',
								Utils.cersToUnicode(name))
							: toString(Utils.cersToUnicode(name), ' ',
								student.getInstitution().getName(), ' ',
								student.getStudentNum()));
				} // if need to realphabetize
			} // while there are more enrolled students
			if (realphabetize) {
				if (Utils.isEmpty(userLangs)) SortUtils.sort(students, sortKeys);
				else SortUtils.sort(students, sortKeys, userLangs[0]);
			} // if need to realphabetize
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (ParameterException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try block
		return students;
	} // getEnrolledStudents(int, boolean, boolean, boolean, String[])

	/** Gets students enrolled in certain courses in alphabetical order by last 
	 * name and given name, excluding TAs and unregistered students.
	 * @param	courseIds	unique IDs of courses
	 * @param	userLangs	languages of this user
	 * @return	list of EnrollmentData
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<EnrollmentData> getEnrolledStudents(
			List<Integer> courseIds, String[] userLangs) throws DBException {
		final String coursesSql = toString(parensQMarks(courseIds), 
				AND + ENRL_COURSE + NOT_EQUALS, AppConfig.tutorialId);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(coursesSql, 
				courseIds);
		return getEnrolledStudents(sql_vals, userLangs);
	} // getEnrolledStudents(List<Integer>, String[])

	/** Gets students enrolled in all courses of an instructor, in alphabetical 
	 * order by last name and surname, excluding TAs and unregistered students.
	 * @param	instructorId	login ID of instructor
	 * @param	userLangs	languages of the instructor
	 * @return	list of EnrollmentData
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<EnrollmentData> getEnrolledStudents(
			String instructorId, String[] userLangs) throws DBException {
		final String coursesSql = toString(
				SELECT + CRS_ID
					+ FROM + COURSES
					+ WHERE + CRS_INSTRUCTOR + EQUALS + QMARK 
					+ AND + CRS_ID + NOT_EQUALS, AppConfig.tutorialId, 
				UNION_ALL + SELECT + COINSTR_CRS
					+ FROM + COINSTRUCTORS
					+ WHERE + COINSTR_INSTR + EQUALS + QMARK 
					+ AND + COINSTR_CRS + NOT_EQUALS, AppConfig.tutorialId);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(coursesSql, 
				instructorId, 
				instructorId);
		return getEnrolledStudents(sql_vals, userLangs);
	} // getEnrolledStudents(String, String[])

	/** Gets students enrolled in specified courses, in alphabetical 
	 * order by last name and surname, excluding TAs and unregistered students.
	 * @param	coursesSql	SQL to follow WHERE ENRL_COURSE IN and values to
	 * substitute into it
	 * @param	userLangs	languages of the instructor
	 * @return	list of EnrollmentData
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static List<EnrollmentData> getEnrolledStudents(
			SQLWithQMarks coursesSql, String[] userLangs) throws DBException {
		final String SELF = "EnrollmentRW.getEnrolledStudents: ";
		final String enrolled = toString(
				SELECT + ENRL_STUDENT
				+ FROM + ENROLLMENT
				+ WHERE + ENRL_COURSE + IN, parens(coursesSql.getSql()), 
				AND, parensBuild(ENRL_IS_TA + IS_NULL
					+ OR + ENRL_IS_TA + EQUALS + N));
		final SQLWithQMarks enrolledSql = new SQLWithQMarks(enrolled,
				coursesSql);
		final boolean realphabetize = !Utils.isEmpty(userLangs)
					&& Utils.realphabetize(userLangs[0]);
		final List<Comparable<?>> sortKeys = (realphabetize
				? new ArrayList<Comparable<?>>() : null);
		final List<EnrollmentData> students =
				getEnrolledStudents(enrolledSql, sortKeys);
		if (realphabetize) try {
			if (Utils.isEmpty(userLangs)) SortUtils.sort(students, sortKeys);
			else SortUtils.sort(students, sortKeys, userLangs[0]);
		} catch (ParameterException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} // if need to realphabetize
		return students;
	} // getEnrolledStudents(SQLWithQMarks, String[])

	/** Gets one student's enrollment data in any course.
	 * @param	studentId	login ID of student
	 * @return	list of one enrollment datum of the student
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<EnrollmentData> getEnrolledStudent(String studentId) 
			throws DBException {
		final SQLWithQMarks sql_vals = new SQLWithQMarks(QMARK, 
				studentId);
		final List<Comparable<?>> sortKeys = null;
		return getEnrolledStudents(sql_vals, sortKeys);
	} // getEnrolledStudent(String)

	/** Gets enrolled students, excluding unregistered students, and sort keys
	 * to alphabetize the students by family name and given name.
	 * @param	studentsSql	SQL to follow WHERE USER_ID IN, and values to
	 * substitute for question marks
	 * @param	sortKeys	list of keys for alphabetization
	 * @return	list of EnrollmentData
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static List<EnrollmentData> getEnrolledStudents(
			SQLWithQMarks studentsSql, List<Comparable<?>> sortKeys) 
			throws DBException {
		final String SELF = "EnrollmentRW.getEnrolledStudents: ";
		Connection con = null;
		try {
			con = getPoolConnection();
			return getEnrolledStudents(con, studentsSql, sortKeys);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try block
	} // getEnrolledStudents(SQLWithQMarks, List<Comparable<?>>)

	/** Gets enrolled students, excluding unregistered students.
	 * @param	con	database connection
	 * @param	studentsSql	SQL to follow WHERE USER_ID IN, and values to
	 * substitute for question marks
	 * @return	list of EnrollmentData
	 * @throws	SQLException	if there's a problem reading the database
	 */
	static List<EnrollmentData> getEnrolledStudents(Connection con,
			SQLWithQMarks studentsSql) throws SQLException {
		final List<Comparable<?>> sortKeys = null;
		return getEnrolledStudents(con, studentsSql, sortKeys);
	} // getEnrolledStudents(Connection, SQLWithQMarks)

	/** Gets enrolled students, excluding unregistered students, and sort keys
	 * to alphabetize the students by family name and given name.
	 * @param	con	database connection
	 * @param	studentsSql	SQL to follow WHERE USER_ID IN, and values to
	 * substitute for question marks
	 * @param	sortKeys	list of keys for alphabetization
	 * @return	list of EnrollmentData
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static List<EnrollmentData> getEnrolledStudents(Connection con,
			SQLWithQMarks studentsSql, List<Comparable<?>> sortKeys) 
			throws SQLException {
		final String SELF = "EnrollmentRW.getEnrolledStudents: ";
		final String qry = toString(
				SELECT, joinAll(
					USER_STUDENTNUM,
					USER_GIVENNAME,
					USER_MIDNAME,
					USER_SURNAME,
					USER_EMAIL,
					USER_TXT_MSG,
					USER_SCHOOLID,
					USER_REGD,
					INSTN_NAME,
					INSTN_STUDENTNUMLABEL,
					USER_ID), 
				FROM + USERS + JOIN + INSTITUTIONS
					+ ON + USER_SCHOOLID + EQUALS + INSTN_ID
				+ WHERE + USER_ID + IN, parens(studentsSql.getSql()), 
				ORDER_BY, joinAll(
					USER_SORT,
					clobToString(INSTN_NAME),
					clobToString(USER_STUDENTNUM)));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, studentsSql);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final List<EnrollmentData> students = new ArrayList<EnrollmentData>();
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final EnrollmentData student = getEnrollmentData(rs);
				final String name = student.getName();
				debugPrint(SELF + "enrolled student ", name, 
						" with student ID '", student.getStudentNum(), "'.");
				students.add(student);
				if (sortKeys != null) sortKeys.add(Utils.cersToUnicode(name));
			} // while there are more enrolled students
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try block
		return students;
	} // getEnrolledStudents(Connection, SQLWithQMarks, List<Comparable<?>>)

	/** Utility function to store Oracle results from USER in an EnrollmentData.
	 * @param	rs	result of an Oracle query
	 * @return	EnrollmentData populated with data from the database call
	 * @throws	SQLException	if there's a problem reading the results
	 */
	private static EnrollmentData getEnrollmentData(ResultSet rs) 
			throws SQLException {
		return getEnrollmentData(rs, !INCLUDE_TAS);
	} // getEnrollmentData(ResultSet)

	/** Utility function to store Oracle results from USER in an EnrollmentData.
	 * @param	rs	result of an Oracle query
	 * @param	includeTAs	whether to include this course's TAs in the list
	 * @return	EnrollmentData populated with data from the database call
	 * @throws	SQLException	if there's a problem reading the results
	 */
	private static EnrollmentData getEnrollmentData(ResultSet rs, 
			boolean includeTAs) throws SQLException {
		final EnrollmentData student = new EnrollmentData();
		student.setUserId(rs.getString(USER_ID));
		student.setName(new Name(rs.getString(USER_GIVENNAME),
				rs.getString(USER_MIDNAME),
				rs.getString(USER_SURNAME)).toString());
		final int instnId = rs.getInt(USER_SCHOOLID);
		final String instnName = rs.getString(INSTN_NAME);
		final String instnStudentNumLabel = rs.getString(INSTN_STUDENTNUMLABEL);
		student.setInstitution(new Institution(instnId, 
				instnName, instnStudentNumLabel));
		student.setStudentNum(rs.getString(USER_STUDENTNUM));
		student.setEmail(rs.getString(USER_EMAIL));
		student.setTextMessageEmail(rs.getString(USER_TXT_MSG));
		final String regDateStr = rs.getString(USER_REGD);
		student.setRegDate(regDateStr == null ? new Date() // null registration date is legacy
				: toDate(regDateStr));
		if (includeTAs) {
			final String isTA = rs.getString(ENRL_IS_TA);
			student.setTA(!Utils.isEmpty(isTA) && isTA.charAt(0) == 'Y');
		} // if including TAs
		return student;
	} // getEnrollmentData(ResultSet, boolean)

	/** Gets the number of institutions of students enrolled in certain courses.
	 * @param	courseIds	unique IDs of courses
	 * @return	number of institutions
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int getNumInstitutions(List<Integer> courseIds) 
			throws DBException {
		final String coursesSql = toString(parensQMarks(courseIds), 
				AND + ENRL_COURSE + NOT_EQUALS, AppConfig.tutorialId);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(coursesSql, 
				courseIds);
		return getNumInstitutions(sql_vals);
	} // getNumInstitutions(List<Integer>)

	/** Gets the number of institutions of students enrolled in courses of an
	 * instructor.
	 * @param	instructorId	login ID of instructor
	 * @return	number of institutions
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int getNumInstitutions(String instructorId) 
			throws DBException {
		final String coursesSql = toString(parensBuild(
				SELECT + CRS_ID
					+ FROM + COURSES
					+ WHERE + CRS_INSTRUCTOR + EQUALS + QMARK
					+ AND + CRS_ID + NOT_EQUALS, AppConfig.tutorialId, 
				UNION_ALL + SELECT + COINSTR_CRS
					+ FROM + COINSTRUCTORS
					+ WHERE + COINSTR_INSTR + EQUALS + QMARK 
					+ AND + COINSTR_CRS + NOT_EQUALS, AppConfig.tutorialId));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(coursesSql, 
				instructorId, 
				instructorId);
		return getNumInstitutions(sql_vals);
	} // getNumInstitutions(String)

	/** Gets the number of institutions of students enrolled in certain courses.
	 * @param	coursesSql	SQL to follow WHERE USER_ID IN, and values to
	 * substitute for question marks
	 * @return	number of institutions
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static int getNumInstitutions(SQLWithQMarks coursesSql) 
			throws DBException {
		final String SELF = "EnrollmentRW.getNumInstitutions: ";
		final String qry = toString(
				SELECT, count(), AS + SRCH_RESULT 
				+ FROM, parensBuild(
					SELECT_UNIQUE + USER_SCHOOLID
					+ FROM + USERS
					+ WHERE + USER_ID + IN, parensBuild(
						SELECT + ENRL_STUDENT
						+ FROM + ENROLLMENT
						+ WHERE + ENRL_COURSE + IN, coursesSql.getSql(), 
						AND, parensBuild(ENRL_IS_TA + IS_NULL
							+ OR + ENRL_IS_TA + EQUALS + N))));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, coursesSql);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int numInstns = 0;
		try {
			con = getPoolConnection();
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) numInstns = rs.getInt(SRCH_RESULT);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try block
		return numInstns;
	} // getNumInstitutions(StringBuilder)

	/** Gets student ID numbers of students enrolled in a course,
	 * in no particular order.
	 * @param	courseId	unique ID of this course
	 * @return	list of student ID numbers
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<String> getEnrolledStudentNums(int courseId) 
			throws DBException {
		return getEnrolledStudentNums(courseId, ANY_INSTITUTION);
	} // getEnrolledStudentNums(int)

	/** Gets student ID numbers of students at a school enrolled in a course,
	 * in no particular order.
	 * @param	courseId	unique ID of this course
	 * @param	instnId	ID number of school from which to get the enrolled 
	 * students; ANY_INSTITUTION if should ignore school
	 * @return	list of student ID numbers
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<String> getEnrolledStudentNums(int courseId,
			int instnId) throws DBException {
		final String SELF = "EnrollmentRW.getEnrolledStudentNums: ";
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final List<String> studentNums = new ArrayList<String>();
		try {
			con = getPoolConnection();
			// load all the enrolled and registered students
			final String enrolledStudentNums = toString(
					SELECT, clobToString(USER_STUDENTNUM), AS + USER_STUDENTNUM 
					+ FROM + USERS
					+ WHERE + USER_ID + IN, parens(
						SELECT + ENRL_STUDENT
						+ FROM + ENROLLMENT
						+ WHERE + ENRL_COURSE + EQUALS + QMARK),
					instnId == ANY_INSTITUTION ? "" 
						: AND + USER_SCHOOLID + EQUALS + QMARK);
			SQLWithQMarks sql_vals = new SQLWithQMarks(
					enrolledStudentNums, courseId);
			if (instnId != ANY_INSTITUTION) sql_vals.addValue(instnId);
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				studentNums.add(rs.getString(USER_STUDENTNUM));
			} // while there are more enrolled students
			rs.close();
			// load students who are enrolled but not registered with ACE
			final int numStudents = studentNums.size();
			final String qry = SELECT + PREENRL_STUDENTNUM
					+ FROM + PREENROLLMENT
					+ WHERE + PREENRL_COURSE + EQUALS + QMARK;
			sql_vals = new SQLWithQMarks(qry, courseId);
			if (!studentNums.isEmpty()) {
				sql_vals.addToSql(AND, clobToString(PREENRL_STUDENTNUM), 
						NOT + IN, parensQMarks(numStudents));
				sql_vals.addValuesArray(studentNums);
			} // if there are students already enrolled
			if (instnId != ANY_INSTITUTION) {
				sql_vals.addToSql(AND + PREENRL_SCHOOLID + EQUALS + QMARK);
				sql_vals.addValue(instnId);
			} // if not any institution
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				studentNums.add(rs.getString(USER_STUDENTNUM));
			} // while there are more enrolled students
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try block
		return studentNums;
	} // getEnrolledStudentNums(int, int)

	/** Utility function to store Oracle results from PREENROLLMENT in an
	 * EnrollmentData.
	 * @param	rs	result of an Oracle query
	 * @return	EnrollmentData populated with data from the database call
	 * @throws	SQLException	if there's a problem reading the results
	 */
	private static EnrollmentData getPreenrollmentData(ResultSet rs)
			throws SQLException {
		final EnrollmentData student = new EnrollmentData();
		student.setStudentNum(rs.getString(PREENRL_STUDENTNUM));
		final int instnId = rs.getInt(PREENRL_SCHOOLID);
		final String instnName = rs.getString(INSTN_NAME);
		final String instnStudentNumLabel = rs.getString(INSTN_STUDENTNUMLABEL);
		student.setInstitution(new Institution(instnId, 
				instnName, instnStudentNumLabel));
		final String name = rs.getString(PREENRL_STUDENTNAME);
		final int nameLen = (name != null ? name.length() : -1);
		if (nameLen > 1 && name.charAt(0) == '"'
				&& name.charAt(nameLen - 1) == '"') {
			student.setName(nameLen > 2 
					? Utils.endsChop(name, 1, 1) : "");
		} else student.setName(name);
		student.setEmail("");
		student.setRegDate(null);
		return student;
	} // getPreenrollmentData(ResultSet)

/* **************** Methods to enroll students in courses *****************/

	/** Enrolls a student in a course.  
	 * Student needs not be at same institution as course owner.
	 * @param	courseId	unique ID of this course
	 * @param	entry	an EnrollmentData
	 * @return	list of EnrollmentData; the EnrollmentData of students who
	 * were already registered with ACE are modified
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static List<EnrollmentData> enroll(int courseId,
			EnrollmentData entry) throws DBException {
		return enroll(courseId, new EnrollmentData[] {entry}, 
				entry.getInstitution());
	} // enroll(int, EnrollmentData)

	/** Enrolls a list of students at a single institution in a course.  
	 * All students must be at same institution as course owner.
	 * @param	courseId	unique ID of this course
	 * @param	entries	array of EnrollmentData
	 * @param	institution	school of the enrolling instructor
	 * @return	list of EnrollmentData; the EnrollmentData of students who
	 * were already registered with ACE are modified
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static List<EnrollmentData> enroll(int courseId,
			EnrollmentData[] entries, Institution institution) 
			throws DBException {
		final String SELF = "EnrollmentRW.enroll: ";
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		// return value: all students newly enrolled or preenrolled
		final List<EnrollmentData> enrollStudents = 
				new ArrayList<EnrollmentData>();
		try {
			con = getPoolConnection();
			// parallel to entries, for searching
			final List<String> studentNumsList = new ArrayList<String>();
			// for manipulation
			final List<String> toPreenroll = new ArrayList<String>();
			for (final EnrollmentData entry : entries) {
				final String studentNum = entry.getStudentNum();
				// add now to preenroll list, remove later if enrolled
				toPreenroll.add(studentNum);
				studentNumsList.add(studentNum);
			} // for each entry
			// get bunches of studentNums
			final List<String[]> studentNumsArrs = getStrGroups(toPreenroll);
			// find students in the enrollment list
			// who are already registered
			// and not already enrolled in this course
			// and at given school
			final int instnId = institution.getId();
			for (final String[] studentNumsArr : studentNumsArrs) {
				final int numStudents = studentNumsArr.length;
				final String qry = toString(
						SELECT, joinAll(
							USER_STUDENTNUM,
							USER_GIVENNAME,
							USER_MIDNAME,
							USER_SURNAME,
							USER_EMAIL,
							USER_TXT_MSG,
							USER_SCHOOLID,
							USER_REGD,
							INSTN_NAME,
							INSTN_STUDENTNUMLABEL,
							USER_ID), 
						FROM + USERS + JOIN + INSTITUTIONS
							+ ON + USER_SCHOOLID + EQUALS + INSTN_ID
						+ WHERE, clobToString(USER_STUDENTNUM), IN, 
							parensQMarks(numStudents), 
						AND + USER_SCHOOLID + EQUALS + QMARK 
						+ AND + USER_ID + NOT + IN, parensBuild(
							SELECT + ENRL_STUDENT
							+ FROM + ENROLLMENT
							+ WHERE + ENRL_COURSE + EQUALS + QMARK), 
						ORDER_BY, joinAll(
							USER_SORT,
							clobToString(INSTN_NAME),
							clobToString(USER_STUDENTNUM)));
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
						studentNumsArr, 
						instnId, 
						courseId);
				debugPrint(SELF, sql_vals);
				stmt = getStatement(con, sql_vals);
				rs = stmt.executeQuery();
				while (rs.next()) {
					final EnrollmentData student = getEnrollmentData(rs);
					// add to list of students to enroll
					enrollStudents.add(student);
					// remove from list of students to preenroll
					toPreenroll.remove(student.getStudentNum());
				} // while there are more students to enroll
				closeStmtAndRs(stmt, rs);
			} // for each bunch of students to enroll
			debugPrint(SELF, toPreenroll.size(), " student(s) out of ", 
					entries.length, " not already registered.");
			// remove students already preenrolled
			String qry = SELECT + PREENRL_STUDENTNUM
					+ FROM + PREENROLLMENT
					+ WHERE + PREENRL_COURSE + EQUALS + QMARK 
					+ AND + PREENRL_SCHOOLID + EQUALS + QMARK;
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
					courseId, 
					instnId);
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final String studentNum = rs.getString(PREENRL_STUDENTNUM);
				debugPrint(SELF + "found ", studentNum, 
						" already preenrolled.");
				toPreenroll.remove(studentNum);
			} // while more students are already preenrolled
			// ready to modify database
			con.setAutoCommit(false);
			// enroll registered students
			String[] fields = new String[] {
					ENRL_COURSE,
					ENRL_STUDENT,
					ENRL_IS_TA}; 
			qry = getInsertIntoValuesQMarksSQL(ENROLLMENT, fields);
			debugPrint(SELF, qry); 
			stmt = con.prepareStatement(qry);
			int batchNum = 0;
			final List<String> studentIds =  new ArrayList<String>();
			for (final EnrollmentData student : enrollStudents) {
				final String studentId = student.getUserId();
				studentIds.add(studentId);
				final StringBuilder joinedValues = setValues(stmt, 
						courseId, 
						studentId,
						'N');
				debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
				stmt.addBatch();
				batchNum++;
				if (batchNum % 100 == 0) {
					stmt.executeBatch();
					stmt.clearBatch();
				} // if time to submit
			} // for each student to enroll
			if (batchNum % 100 != 0) {
				stmt.executeBatch();
			} // if need to submit
			closeConnection(null, stmt, null);
			// set enrolled students to watch sticky topics in the course
			ForumRW.setStudentsToWatchStickyTopics(con, courseId, studentIds);
			// preenroll remaining students
			debugPrint(SELF, toPreenroll.size(), " student(s) out of ", 
					entries.length, " not already registered or preenrolled.");
			// insert preenroll values except for already existing entries
			fields = new String[] {
					PREENRL_COURSE,
					PREENRL_SCHOOLID,
					PREENRL_STUDENTNUM,
					PREENRL_STUDENTNAME}; 
			qry = toString(
					INSERT_INTO + PREENROLLMENT, parens(fields), ' ', 
					SELECT, getQMarks(fields), // no parens around these qMarks!
					FROM + DUMMY_TABLE 
					+ WHERE + NOT_EXISTS, parensBuild(
						SELECT + PREENRL_COURSE
						+ FROM + PREENROLLMENT
						+ WHERE + PREENRL_STUDENTNUM + LIKE + QMARK
						+ AND + PREENRL_SCHOOLID + EQUALS + QMARK 
						+ AND + PREENRL_COURSE + EQUALS + QMARK));
			stmt = con.prepareStatement(qry);
			debugPrint(SELF, qry); 
			batchNum = 0;
			for (final String studentNum : toPreenroll) {
				// set EnrollmentData as not registered
				final int entryIndex = studentNumsList.indexOf(studentNum);
				final EnrollmentData entry = entries[entryIndex];
				entry.setInstitution(institution);
				entry.setRegDate(null);
				// add to list of students newly (pre)enrolled
				final String studNumDB = trimNullToEmpty(studentNum);
				final String nameDB = trimNullToEmpty(entry.getName());
				final StringBuilder joinedValues = setValues(stmt, 
						courseId, 
						instnId, 
						studNumDB,
						nameDB,
						studNumDB, 
						instnId, 
						courseId);
				debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
				stmt.addBatch();
				enrollStudents.add(entry);
				batchNum++;
				if (batchNum % 100 == 0) {
					stmt.executeBatch();
					stmt.clearBatch();
				} // if time to submit
			} // for each student to preenroll
			if (batchNum % 100 != 0) {
				stmt.executeBatch();
			} // if need to submit
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try block
		return enrollStudents;
	} // enroll(int, EnrollmentData[], Institution)

	/** Enroll a newly registered student in courses in which he or she was
	 * preenrolled.
	 * @param	student	student who has newly registered
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void enrollInQualified(User student) throws DBException {
		final String SELF = "EnrollmentRW.enrollInQualified: ";
		final String studentNum = student.getStudentNum();
		final String studentUserId = student.getUserId();
		Connection con = null;
		PreparedStatement stmt = null;
		PreparedStatement stmtInsert = null;
		PreparedStatement stmtDelete = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			// select the courses in which s/he is preenrolled,
			// for which s/he is not already enrolled,
			// and for which the instructor or a coinstructor is at his/her school
			final String qry = toString(
					SELECT + PREENRL_COURSE
					+ FROM + PREENROLLMENT
					+ WHERE + PREENRL_STUDENTNUM + LIKE + QMARK 
					+ AND + PREENRL_COURSE + NOT + IN, parens(
						SELECT + ENRL_COURSE
						+ FROM + ENROLLMENT
						+ WHERE + ENRL_STUDENT + EQUALS + QMARK), 
					AND + QMARK + IN, parensBuild(
						SELECT + USER_SCHOOLID
						+ FROM + USERS
						+ WHERE + USER_ID + IN, parens(
							SELECT + CRS_INSTRUCTOR
							+ FROM + COURSES
							+ WHERE + CRS_ID + EQUALS
								+ PREENROLLMENT + DOT + PREENRL_COURSE),
						OR + USER_ID + IN, parens(
							SELECT + COINSTR_INSTR
							+ FROM + COINSTRUCTORS
							+ WHERE + COINSTR_CRS + EQUALS
								+ PREENROLLMENT + DOT + PREENRL_COURSE)));
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
					studentNum,
					studentUserId, 
					student.getInstitutionId());
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			final List<Integer> preenrolledCourseIds = new ArrayList<Integer>();
			while (rs.next()) {
				preenrolledCourseIds.add(
						Integer.valueOf(rs.getInt(PREENRL_COURSE)));
			} // while preenrollment courses are found
			if (!preenrolledCourseIds.isEmpty()) {
				con.setAutoCommit(false);
				// enroll in courses, remove from preenrollment
				final String[] fields = new String[] {
						ENRL_COURSE,
						ENRL_STUDENT,
						ENRL_IS_TA}; 
				final String insertQry = getInsertIntoValuesQMarksSQL(
						ENROLLMENT, fields);
				debugPrint(SELF, insertQry);
				stmtInsert = con.prepareStatement(insertQry);
				final String delQry = DELETE_FROM + PREENROLLMENT
						+ WHERE + PREENRL_STUDENTNUM + LIKE + QMARK 
						+ AND + PREENRL_COURSE + EQUALS + QMARK;
				debugPrint(SELF, delQry); 
				stmtDelete = con.prepareStatement(delQry);
				int batchNum = 0;
				for (final Integer courseIdObj : preenrolledCourseIds) {
					final int courseId = courseIdObj.intValue();
					StringBuilder joinedValues = setValues(stmtInsert, 
							courseId, 
							studentUserId,
							'N');
					debugPrint(SELF, "insert batch ", batchNum + 1, ": ", 
							joinedValues); 
					joinedValues = setValues(stmtDelete, 
							studentNum, 
							courseId);
					debugPrint(SELF, "delete batch ", batchNum + 1, ": ", 
							joinedValues); 
					stmtInsert.addBatch();
					stmtDelete.addBatch();
					batchNum++;
					if (batchNum % 100 == 0) {
						stmtInsert.executeBatch();
						stmtInsert.clearBatch();
						stmtDelete.executeBatch();
						stmtDelete.clearBatch();
					} // if time to execute
				} // while preenrollment courses are found
				if (batchNum % 100 != 0) {
					stmtInsert.executeBatch();
					stmtDelete.executeBatch();
				} // if time to execute
				// set student to watch sticky topics in the course
				ForumRW.setStudentToWatchStickyTopics(con, 
						preenrolledCourseIds, studentUserId);
				con.commit();
			} // if there are courses in which the student was preenrolled
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(null, stmtInsert, null);
			closeConnection(null, stmtDelete, null);
			closeConnection(con, stmt, rs);
		} // try block
	} // enrollInQualified(String, String)

	/** Enroll a registered student in a course.
	 * @param	userId	login ID of student
	 * @param	courseId	unique ID of the course
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void enrollInCourse(String userId, int courseId)
			throws DBException {
		if (userId == null) return;
		enrollInCourse(new String[] {userId}, courseId);
	} // enrollInCourse(String, int)

	/** Enroll registered students in a course.
	 * @param	userIds	login IDs of students to enroll
	 * @param	courseId	unique ID of the course
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void enrollInCourse(String[] userIds, int courseId)
			throws DBException {
		final String SELF = "EnrollmentRW.enrollInCourse: ";
		if (Utils.isEmpty(userIds)) return;
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final List<String> toEnroll =
				new ArrayList<String>(Arrays.asList(userIds));
		try {
			con = getPoolConnection();
			// divide students into bunches
			final List<String[]> userIdsArrs = getStrGroups(userIds);
			// find students already enrolled in this course
			for (final String[] userIdsArr : userIdsArrs) {
				final String qry = toString(
						SELECT + ENRL_STUDENT
						+ FROM + ENROLLMENT
						+ WHERE + ENRL_COURSE + EQUALS + QMARK 
						+ AND + ENRL_STUDENT + IN, 
							parensQMarks(userIdsArr));
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
						courseId, 
						userIdsArr);
				debugPrint(SELF, sql_vals);
				stmt = getStatement(con, sql_vals);
				rs = stmt.executeQuery();
				while (rs.next()) {
					final String userId = rs.getString(ENRL_STUDENT);
					// remove from list of students to preenroll
					toEnroll.remove(userId);
				} // while there are more students to enroll
			} // for each bunch of students
			debugPrint(SELF + toEnroll.size(), " student(s) out of ",
					userIds.length, " need to be enrolled: ", toEnroll);
			// enroll students not already enrolled
			con.setAutoCommit(false);
			final String[] fields = new String[] {
					ENRL_COURSE,
					ENRL_STUDENT,
					ENRL_IS_TA}; 
			final String qry = getInsertIntoValuesQMarksSQL(ENROLLMENT, fields);
			debugPrint(SELF, qry); 
			stmt = con.prepareStatement(qry);
			int batchNum = 0;
			for (final String userId : toEnroll) {
				final StringBuilder joinedValues = setValues(stmt, 
						courseId, 
						userId,
						'N');
				debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
				stmt.addBatch();
				batchNum++;
				if (batchNum % 100 == 0) {
					stmt.executeBatch();
					stmt.clearBatch();
				} // if time to submit
			} // for each student to enroll
			if (batchNum % 100 != 0) {
				stmt.executeBatch();
			} // if need to submit
			// set enrolled students to watch sticky topics in the course
			ForumRW.setStudentsToWatchStickyTopics(con, courseId, userIds);
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			alwaysPrint(SELF + "SQLException caught for students ",
					userIds, " enrolling in course ID ", courseId);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			rollbackConnection(con);
			alwaysPrint(SELF + "Exception caught for students ",
					userIds, " enrolling in course ID ", courseId);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try block
	} // enrollInCourse(String[], int)

/* **************** Miscellaneous methods *****************/

	/** Disenrolls unregistered students from a course.
	 * @param	courseId	unique ID of the course
	 * @param	studentNums	students' school ID numbers
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void disenrollUnregistered(int courseId, String[] studentNums)
			throws DBException {
		final String SELF = "EnrollmentRW.disenrollUnregistered: ";
		final int numStudents = studentNums.length;
		Connection con = null;
		if (numStudents > 0) try {
			final String qry = toString(
					DELETE_FROM + PREENROLLMENT
					+ WHERE, clobToString(PREENRL_STUDENTNUM),
						IN, parensQMarks(numStudents),
					AND + PREENRL_COURSE + EQUALS + QMARK);
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
					studentNums, 
					courseId);
			con = getPoolConnection();
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try block
	} // disenrollUnregistered(int, String[])

	/** Disenroll registered students from a course.
	 * @param	courseId	unique ID of the course
	 * @param	userIds	login IDs of students
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void disenrollRegistered(int courseId, String[] userIds)
			throws DBException {
		final String SELF = "EnrollmentRW.disenrollRegistered: ";
		final int numUsers = userIds.length;
		Connection con = null;
		if (numUsers > 0) try {
			final String hwIdsQry = parens(
					SELECT + HW_ID
					+ FROM + HWSETS
					+ WHERE + HW_COURSE + EQUALS + QMARK);
			final String[][] fieldSets = new String[][] {
					{RESP_SUBSTNS, RESP_SUBS_STUDENT, RESP_SUBS_HWID},
					{RESPONSES, RESP_STUDENT, RESP_HWID},
					{ASSIGNED_QS, ASSGND_QS_STUDENT, ASSGND_QS_HWID},
					{EXTENSIONS, EXT_STUDENT, EXT_HWID},
					{ENROLLMENT, ENRL_STUDENT, ENRL_COURSE}
					};
			final StringBuilder parenQMarks = parensQMarks(numUsers);
			con = getPoolConnection();
			con.setAutoCommit(false);
			ForumRW.unwatchTopics(con, courseId, userIds);
			for (final String[] fields : fieldSets) {
				final String qry = toString(
						DELETE_FROM, fields[0], 
						WHERE, fields[1], IN, parenQMarks, 
						AND, fields[2], IN, ENROLLMENT.equals(fields[0]) 
							? parens(QMARK) : hwIdsQry);
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
						userIds, 
						courseId);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} // for each table
			con.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try block
	} // disenrollRegistered(int, String[])

	/** Gets the date and time of first entry into a course. 
	 * @param	courseId	unique ID of a course
	 * @param	studentId	login ID of the costudent to add
	 * @return	date of the student's first entry into the course, or null if
	 * not recorded
	 */
	public static Date getFirstEntry(int courseId, String studentId) {
		final String SELF = "CourseRW.getFirstEntry: ";
		Date entryDate = null; 
		final String qry = 
				SELECT + ENRL_1ST_ENTRY
				+ FROM + ENROLLMENT
				+ WHERE + ENRL_COURSE + EQUALS + QMARK
				+ AND + ENRL_STUDENT + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				courseId, 
				studentId);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				final String entryDateStr = rs.getString(ENRL_1ST_ENTRY);
				if (!Utils.isEmpty(entryDateStr)) {
					entryDate = toDate(entryDateStr);
				} // if a date has been recorded
			} // if there is a result
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
		} finally {
			closeConnection(con, stmt, rs);
		}
		return entryDate;
	} // getFirstEntry(int, String)

	/** Sets the date and time of first entry into a course. 
	 * @param	courseId	unique ID of a course
	 * @param	studentId	login ID of the costudent to add
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setFirstEntry(int courseId, String studentId) 
			throws DBException {
		final String SELF = "CourseRW.setFirstEntry: ";
		final String dateStr = dateToString(new Date());
		final String qry = 
				UPDATE + ENROLLMENT
				+ SET + ENRL_1ST_ENTRY + EQUALS + QMARK 
				+ WHERE + ENRL_COURSE + EQUALS + QMARK
				+ AND + ENRL_STUDENT + EQUALS + QMARK
				+ AND + ENRL_1ST_ENTRY + IS_NULL;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				dateStr,
				courseId,
				studentId);
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
	} // setFirstEntry(int, String)

	/** Promote registered students in a course to TA, or demote them from TA.
	 * @param	courseId	unique ID of the course
	 * @param	userIds	list of login IDs of students
	 * @param	promote	whether to promote or demote them
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void modifyTAs(int courseId, List<String> userIds,
			boolean promote) throws DBException {
		final String SELF = "EnrollmentRW.modifyTAs: ";
		Connection con = null;
		if (!Utils.isEmpty(userIds)) try {
			final String qry = toString(
					UPDATE + ENROLLMENT
					+ SET + ENRL_IS_TA + EQUALS, quotes(promote ? "Y" : ""), 
					WHERE + ENRL_STUDENT + IN, parensQMarks(userIds),
					AND + ENRL_COURSE + EQUALS + QMARK);
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
					userIds, 
					courseId);
			con = getPoolConnection();
			con.setAutoCommit(false);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
			ForumRW.changeTAWatching(con, courseId, userIds, promote);
			con.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try block
	} // modifyTAs(int, List<String>, boolean)

	/** Get ID numbers of all courses for which this user is a TA.
	 * @param	userId	login ID of student
	 * @return	list of course ID numbers
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<Integer> getTAForCourseIds(String userId)
			throws DBException {
		final String SELF = "EnrollmentRW.getTAForCourseIds: ";
		final String qry = 
				SELECT + ENRL_COURSE
				+ FROM + ENROLLMENT
				+ WHERE + ENRL_IS_TA + EQUALS + Y
				+ AND + ENRL_STUDENT + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				userId);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final List<Integer> courseIds = new ArrayList<Integer>();
		try {
			con = getPoolConnection();
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			// make list of student names + IDs for alphabetizing
			// and table of same names keyed to enrollment data
			while (rs.next()) {
				courseIds.add(Integer.valueOf(rs.getInt(ENRL_COURSE)));
			} // while there are more enrolled students
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try block
		debugPrint(SELF + "returning ", courseIds);
		return courseIds;
	} // getTAForCourseIds(String)

	/** Transfers work that was done in a course under one login ID to a
	 * different login ID, and deletes the first login ID.  Used only in UK
	 * version.
	 * @param	courseId	ID number of the course containing the work to be
	 * transferred
	 * @param	students	names, student ID numbers, old login IDs, and new
	 * login IDs of students whose work is to be transferred
	 * @param	OLD_LOGIN	the member of <code>students</code> containing
	 * the old login ID
	 * @param	NEW_LOGIN	the member of <code>students</code> containing
	 * the new login ID
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void transferWork(int courseId, List<String[]> students,
			int OLD_LOGIN, int NEW_LOGIN) throws DBException {
		final String SELF = "EnrollmentRW.transferWork: ";
		final int numStudents = students.size();
		debugPrint(SELF + "transferring work of ",
				numStudents, " student(s).");
		String[] oldUserIds = new String[numStudents];
		final String hwIdsQry = parens(
				SELECT + HW_ID
				+ FROM + HWSETS
				+ WHERE + HW_COURSE + EQUALS + QMARK);
		final String[][] fieldSets = new String[][] {
				{ENROLLMENT, ENRL_STUDENT, ENRL_COURSE},
				{RESPONSES, RESP_STUDENT, RESP_HWID},
				{ASSIGNED_QS, ASSGND_QS_STUDENT, ASSGND_QS_HWID},
				{EXTENSIONS, EXT_STUDENT, EXT_HWID},
				{RESP_SUBSTNS, RESP_SUBS_STUDENT, RESP_SUBS_HWID}
				};
		Connection con = null;
		PreparedStatement stmt = null;
		boolean transferred = false;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			boolean enrollment = true;
			int batchNum = 0;
			for (final String[] fields : fieldSets) {
				final String qry = toString(
						UPDATE, fields[0], 
						SET, fields[1], EQUALS + QMARK 
						+ WHERE, fields[1], EQUALS + QMARK 
						+ AND, fields[2],
							enrollment ? EQUALS + QMARK
								: toString(IN, hwIdsQry)); // contains one QMARK
				debugPrint(SELF, qry); 
				stmt = con.prepareStatement(qry);
				for (int studNum = 0; studNum < numStudents; studNum++) {
					final String[] student = students.get(studNum);
					if (enrollment) {
						oldUserIds[studNum] = student[OLD_LOGIN];
						debugPrint(student[0], ", student ID ",
								student[1], ": ", student[OLD_LOGIN],
								" to ", student[NEW_LOGIN]);
					} // if setNum
					final StringBuilder joinedValues = setValues(stmt, 
							student[NEW_LOGIN], 
							student[OLD_LOGIN], 
							courseId);
					debugPrint(SELF, "batch ", batchNum + 1, ": ", joinedValues); 
					stmt.addBatch();
					batchNum++;
					if (batchNum % 100 == 0) {
						stmt.executeBatch();
						stmt.clearBatch();
					} // if time to submit
				} // for each student
				if (batchNum % 100 != 0) {
					stmt.executeBatch();
				} // if need to submit
				closeConnection(null, stmt, null);
				enrollment = false;
			} // for each update
			con.commit();
			transferred = true;
			debugPrint(SELF + "work transferred, deleting old logins.");
			UserWrite.removeUsers(oldUserIds);
			debugPrint(SELF + "old logins deleted.");
		} catch (SQLException e) {
			rollbackConnection(con);
			debugPrint(SELF, (transferred
						? "old logins not deleted due to exception: "
						: "work not transferred due to exception: "));
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, null);
		} // try block
	} // transferWork(int, List<String[]>, int, int)

	/** For all users who have the same institution and student ID number as 
	 * the students in a course, turns off the flag that prevents them from
	 * seeing calculated synthesis products in feedback in any course. 
	 * @param	courseId	ID number of the course
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void letSeeSynthCalcdProds(int courseId) throws DBException {
		final String SELF = "EnrollmentRW.letSeeSynthCalcdProds: ";
		final String qry = toString(
				UPDATE + USERS 
				+ SET + USER_FLAGS + EQUALS 
					+ USER_FLAGS + MINUS, DONT_SHOW_CALCD_SYNTH_PRODS,
				WHERE + USER_ID + IN, parensBuild(
					SELECT + USER_ID 
					+ FROM + USERS
					+ WHERE, parens(joinAll(
						USER_SCHOOLID, 
						clobToString(USER_STUDENTNUM))), 
					IN, parensBuild(
						SELECT, joinAll(
							USER_SCHOOLID, 
							clobToString(USER_STUDENTNUM)),
						FROM + USERS
						+ WHERE + USER_ID + IN, parens(
							SELECT + ENRL_STUDENT
							+ FROM + ENROLLMENT
							+ WHERE + ENRL_COURSE + EQUALS + QMARK))),
				AND, bitand(USER_FLAGS, DONT_SHOW_CALCD_SYNTH_PRODS), 
					IS_NOT_ZERO);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				courseId);
		Connection con = null;
		try {
			con = getPoolConnection();
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // letSeeSynthCalcdProds(int)

	/** Sets the serial numbers of a student's courses.
	 * @param	courses	list of the student's courses
	 * @param	studentId	the student's ID
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setCourseSerialNos(List<Course> courses, 
			String studentId) throws DBException {
		final String SELF = "EnrollmentRW.setCourseSerialNos: ";
		final String qry = 
				UPDATE + ENROLLMENT
				+ SET + ENRL_SERIALNO + EQUALS + QMARK
				+ WHERE + ENRL_COURSE + EQUALS + QMARK
				+ AND + ENRL_STUDENT + EQUALS + QMARK;
		debugPrint(SELF, qry);
		Connection con = null;
		PreparedStatement stmt = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			stmt = con.prepareStatement(qry);
			int crsNum = 0;
			for (final Course course : courses) {
				crsNum++;
				final int crsId = course.getId();
				final StringBuilder joinedValues = setValues(stmt, 
						crsNum, 
						crsId, 
						studentId);
				debugPrint(SELF, "batch ", crsNum, ": ", joinedValues); 
				stmt.addBatch();
			} // for each coinstructed course
			stmt.executeBatch();
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, null);
		}
	} // setCourseSerialNos(List<Course>, String)

	/** Disables external instantiation. */
	private EnrollmentRW() {
		// disable external instantiation
	}

} // EnrollmentRW
