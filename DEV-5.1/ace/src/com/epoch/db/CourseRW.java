package com.epoch.db;

import static com.epoch.db.dbConstants.CourseRWConstants.*;
import static com.epoch.db.dbConstants.ForumRWConstants.*;
import static com.epoch.db.dbConstants.UserRWConstants.*;
import com.epoch.AppConfig;
import com.epoch.courseware.Course;
import com.epoch.courseware.Name;
import com.epoch.courseware.User;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.UniquenessException;
import com.epoch.utils.Utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/** Methods for reading and writing courses. */
public final class CourseRW extends DBCommon {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Table describing which IP addresses may access a course. */
	private static final String ALLOWED_IPS = "allowed_ips_v1";
		/** Field in ALLOWED_IPS.  &rarr; CRS_ID */
		private static final String IPS_COURSE = "course_id";
		/** Field in ALLOWED_IPS. */
		private static final String IPS_ADDRESS = "address"; 

	/** Gets the courses in which a student is enrolled.
	 * @param	studentId	login ID of the student
	 * @return	list of courses in which the student is enrolled
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<Course> getCoursesEnrolled(String studentId) 
			throws DBException {
		final String SELF = "CourseRW.getCoursesEnrolled: ";
		final List<Course> courses = new ArrayList<Course>();
		if (Utils.isEmptyOrWhitespace(studentId)) return courses;
		final String qry = toString(
				SELECT, joinAll(
					CRS_ID, 
					CRS_FLAGS, 
					CRS_HOMEPG, 
					CRS_NAME, 
					CRS_DESCRIP, 
					CRS_ENABLE_DATE,
					CRS_NOTES, 
					CRS_BOOK, 
					CRS_ZONE, 
					CRS_ACEBOOKID,
					CRS_INSTRUCTOR,
					CRS_MAXEXT,
					CRS_PWDHASH), 
				FROM + ENROLLMENT + JOIN + COURSES
					+ ON + ENRL_COURSE + EQUALS + CRS_ID
				+ WHERE + ENRL_STUDENT + EQUALS + QMARK 
				+ ORDER_BY, joinAll(
					ENROLLMENT + DOT + ENRL_SERIALNO,
					CRS_ID));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				studentId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) courses.add(getCourseData(rs));
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			 e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return courses; 
	} // getCoursesEnrolled(String)

	/** Utility function to read Oracle results from getCourse*.  
	 * @param	rs	result of an Oracle query
	 * @return	a course populated with data from the database call
	 * @throws	SQLException	if there's a problem reading the results
	 */
	private static Course getCourseData(ResultSet rs) 
			throws SQLException {
		final String SELF = "CourseRW.getCourseData: ";
		final Course course = new Course(rs.getInt(CRS_ID));
		course.setFlags(rs.getInt(CRS_FLAGS));
		final String enableDateStr = rs.getString(CRS_ENABLE_DATE);
		course.setEnableDate(Utils.isEmpty(enableDateStr) 
				? null : toDate(enableDateStr));
		course.setHomePage(rs.getString(CRS_HOMEPG));
		if (course.getHomePage() == null) course.setHomePage("");
		course.setName(rs.getString(CRS_NAME));
		course.setDescription(rs.getString(CRS_DESCRIP));
		if (course.getDescription() == null) course.setDescription("");
		course.setNotes(rs.getString(CRS_NOTES));
		if (course.getNotes() == null) course.setNotes("");
		course.setBook(rs.getString(CRS_BOOK));
		if (course.getBook() == null) course.setBook("Other");
		final String zone = rs.getString(CRS_ZONE);
		course.setTimeZone(zone == null ? TimeZone.getDefault()
				: TimeZone.getTimeZone(zone));
		course.setACEBookId(rs.getInt(CRS_ACEBOOKID));
		course.setOwnerId(rs.getString(CRS_INSTRUCTOR));
		course.setMaxExtensionsStr(rs.getString(CRS_MAXEXT));
		course.setPasswordHash(rs.getBytes(CRS_PWDHASH));
		debugPrint(SELF + "course ID = ", course.getId(),
				", owner ID = ", course.getOwnerId());
		return course;
	} // getCourseData(ResultSet)

	/** Gets the courses created by an instructor, plus coinstructed courses,
	 * plus the tutorials course.
	 * @param	instructorId	login ID of the instructor
	 * @return	list of courses taught by this instructor, plus the tutorial
	 * course
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static List<Course> getCoursesCreated(String instructorId) 
			throws DBException {
		final String SELF = "CourseRW.getCoursesCreated: ";
		if (Utils.isEmptyOrWhitespace(instructorId)) 
			return new ArrayList<Course>();
		final StringBuilder courseFieldsBld = joinAll(
				CRS_ID, 
				CRS_FLAGS, 
				// UNION doesn't appear to work with CLOBS; convert them to Strings of
				// same column name
				clobToStringAs(CRS_NAME),
				clobToStringAs(CRS_DESCRIP),
				clobToStringAs(CRS_NOTES),
				clobToStringAs(CRS_HOMEPG),
				clobToStringAs(CRS_ZONE),
				CRS_ENABLE_DATE,
				CRS_BOOK, 
				CRS_ACEBOOKID,
				CRS_MAXEXT,
				CRS_PWDHASH);
		final String qry = toString(
				SELECT, joinAll(
					courseFieldsBld,
					CRS_INSTRUCTOR,
					CRS_SERIALNO,
					'0' + AS + SRCH_RESULT), 
				FROM + COURSES
				+ WHERE + CRS_INSTRUCTOR + EQUALS + QMARK
				+ OR + CRS_ID + EQUALS, AppConfig.tutorialId, 
				UNION + SELECT, joinAll(
					courseFieldsBld,
					COURSES + DOT + CRS_INSTRUCTOR,
					COINSTRUCTORS + DOT + COINSTR_SERIALNO,
					'1'), 
				FROM + COINSTRUCTORS
					+ JOIN + COURSES
					+ ON + COINSTR_CRS + EQUALS + CRS_ID 
				+ WHERE + COINSTRUCTORS + DOT + COINSTR_INSTR + EQUALS + QMARK 
				+ ORDER_BY, joinAll(
					CRS_SERIALNO,
					CRS_ID));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				instructorId, 
				instructorId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final List<Course> courses = new ArrayList<Course>();
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final Course course = getCourseData(rs);
				if (rs.getInt(SRCH_RESULT) > 0) {
					course.setAmCoinstructor(true);
				} // if is coinstructor
				courses.add(course);
			} // while there's more to read
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try block
		return courses; 
	} // getCoursesCreated(String)

	/** Gets information about a course.
	 * @param	courseId	unique ID of this course
	 * @return	the course
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Course getCourseInfo(int courseId) throws DBException {
		Connection con = null;
		Course course = null;
		try {
			con = getPoolConnection();
			course = getCourseInfo(con, courseId);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try block
		return course;	
	} // getCourseInfo(int)

	/** Gets information about a course.
	 * @param	con	database connection
	 * @param	courseId	unique ID of this course
	 * @return	the course
	 * @throws	SQLException	if there's a problem reading the database
	 */
	private static Course getCourseInfo(Connection con, int courseId) 
			throws SQLException {
		final String SELF = "CourseRW.getCourseInfo: ";
		final String qry = toString(
				SELECT, joinAll(
					CRS_ID, 
					CRS_FLAGS, 
					CRS_HOMEPG, 
					CRS_NAME, 
					CRS_DESCRIP, 
					CRS_ENABLE_DATE,
					CRS_NOTES, 
					CRS_BOOK, 
					CRS_ZONE, 
					CRS_ACEBOOKID,
					CRS_INSTRUCTOR,
					CRS_MAXEXT,
					CRS_PWDHASH), 
				FROM + COURSES 
				+ WHERE + CRS_ID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				courseId);
		debugPrint(SELF, sql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		Course course = null;
		try {
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) course = getCourseData(rs);
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try block
		return course;	
	} // getCourseInfo(Connection, int)

	/** Modifies a course record. 
	 * @param	course	new course data to store in database
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setCourse(Course course) throws DBException {
		final String SELF = "CourseRW.setCourse: ";
		final SQLWithQMarks sql_vals = new SQLWithQMarks();
		final List<String> fields = new ArrayList<String>(Arrays.asList(
				CRS_FLAGS,
				CRS_HOMEPG, // CLOB
				CRS_NAME, // CLOB
				CRS_DESCRIP, // CLOB
				CRS_ENABLE_DATE,
				CRS_NOTES, // CLOB
				CRS_BOOK,
				CRS_MAXEXT,
				CRS_ZONE)); // CLOB
		sql_vals.setValues(
				course.getFlags(),
				course.getHomePage(),
				course.getName(),
				course.getDescription(),
				dateToString(course.getEnableDate()),
				course.getNotes(),
				course.getBook(),
				course.getMaxExtensionsStr(),
				course.getTimeZone().getID());
		if (course.getACEBookId() != 0) {
			fields.add(CRS_ACEBOOKID);
			sql_vals.addValue(course.getACEBookId());
		} // if there's an ACE textbook
		sql_vals.setSql(
				UPDATE + COURSES + SET, equalsJoinQMarksList(fields),
 				WHERE + CRS_ID + EQUALS + QMARK);
		sql_vals.addValue(course.getId());
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
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
	} // setCourse(Course)

	/** Sets the password for a course. 
	 * @param	course	course whose password is changing
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setCoursePassword(Course course) throws DBException {
		final String SELF = "CourseRW.setCoursePassword: ";
		final String qry = UPDATE + COURSES 
				+ SET + CRS_PWDHASH + EQUALS + QMARK 
				+ WHERE + CRS_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				course.getPasswordHash(),
				course.getId());
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
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
	} // setCoursePassword(Course)

	/** Adds a new course record. 
	 * @param	instructorId	login ID of the instructor
	 * @param	course	new course data to store in database
	 * @return	the new course
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static Course addCourse(String instructorId, Course course) 
			throws DBException {
		final String SELF = "CourseRW.addCourse: ";
		final String dateStr = dateToString(course.getEnableDate());
		final String timeZoneId = course.getTimeZone().getID();
		Connection con = null;
		int newId = 1;
		try {
			con = getPoolConnection();
			newId = nextSequence(con, COURSES_SEQ);
			final String[] fields = new String[] {
					CRS_ID,
					CRS_INSTRUCTOR,
					CRS_FLAGS,
					CRS_NAME, // CLOB
					CRS_DESCRIP, // CLOB
					CRS_ENABLE_DATE,
					CRS_HOMEPG, // CLOB
					CRS_NOTES, // CLOB
					CRS_BOOK,
					CRS_MAXEXT,
					CRS_ZONE}; // CLOB
			String qry = getInsertIntoValuesQMarksSQL(COURSES, fields);
			SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
					newId, 
					instructorId, 
					course.getFlags(),
					course.getName(),
					course.getDescription(),
					dateStr,
					course.getHomePage(),
					course.getNotes(),
					course.getBook(),
					course.getMaxExtensionsStr(),
					timeZoneId);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals); 
			final int textbkId = course.getACEBookId();
			if (textbkId != 0) {
				qry = UPDATE + COURSES
						+ SET + CRS_ACEBOOKID + EQUALS + QMARK
						+ WHERE + CRS_ID + EQUALS + QMARK;
				sql_vals = new SQLWithQMarks(qry, 
						textbkId,
						newId);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} // if there's a course textbook
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
		return new Course(newId, course);
	} // addCourse(String, Course)

	/** Sets the serial numbers of courses for an instructor.
	 * @param	courses	list of the instructor's courses
	 * @param	instructorId	the instructor's ID
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setCourseSerialNos(List<Course> courses, 
			String instructorId) throws DBException {
		final String SELF = "CourseRW.setCourseSerialNos: ";
		final Map<Integer, Integer> ownedCourseIds = 
				new LinkedHashMap<Integer, Integer>();
		final Map<Integer, Integer> coinstructedCourseIds = 
				new LinkedHashMap<Integer, Integer>();
		int crsNum = 0;
		for (final Course course : courses) {
			crsNum++;
			if (course.getAmCoinstructor()) {
				coinstructedCourseIds.put(Integer.valueOf(course.getId()), 
					Integer.valueOf(crsNum));
			} else ownedCourseIds.put(Integer.valueOf(course.getId()), 
				Integer.valueOf(crsNum));
		} // for each course
		final boolean haveOwnedCourses = !ownedCourseIds.isEmpty();
		Connection con = null;
		PreparedStatement stmt = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			if (haveOwnedCourses) {
				final String qry = 
						UPDATE + COURSES 
						+ SET + CRS_SERIALNO + EQUALS + QMARK
						+ WHERE + CRS_ID + EQUALS + QMARK;
				debugPrint(SELF, qry); 
				stmt = con.prepareStatement(qry);
				int batchNum = 0;
				for (final Map.Entry<Integer, Integer> entry :
						ownedCourseIds.entrySet()) {
					crsNum = entry.getValue().intValue();
					final Integer crsId = entry.getKey(); 
					final StringBuilder joinedValues = setValues(stmt, 
							crsNum, 
							crsId);
					debugPrint(SELF, "batch ", ++batchNum, ": ", joinedValues); 
					stmt.addBatch();
				} // for each owned course
				stmt.executeBatch();
			} // if there are owned courses
			if (!coinstructedCourseIds.isEmpty()) {
				stmt.close();
				final String qry = 
						UPDATE + COINSTRUCTORS
						+ SET + COINSTR_SERIALNO + EQUALS + QMARK
						+ WHERE + COINSTR_CRS + EQUALS + QMARK
						+ AND + COINSTR_INSTR + EQUALS + QMARK;
				debugPrint(SELF, qry); 
				stmt = con.prepareStatement(qry);
				int batchNum = 0;
				for (final Map.Entry<Integer,Integer> entry :
						coinstructedCourseIds.entrySet()) {
					crsNum = entry.getValue().intValue();
					final StringBuilder joinedValues = setValues(stmt, 
							crsNum, 
							entry.getKey(), 
							instructorId);
					debugPrint(SELF, "batch ", ++batchNum, ": ", joinedValues); 
					stmt.addBatch();
				} // for each coinstructed course
				stmt.executeBatch();
			} // if there are coinstructed courses
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, null);
		}
	} // setCourseSerialNos(List<Course>, String)

	/** Deletes a course.
	 * @param	courseId	unique ID of this course
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void removeCourse(int courseId) throws DBException {
		removeCourses(new int[] {courseId});
	} // removeCourse(int)

	/** Deletes courses.
	 * @param	courseIds	unique IDs of the courses
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void removeCourses(int[] courseIds) throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			final StringBuilder sqlBld = parensQMarks(courseIds);
			final SQLWithQMarks sql_vals = new SQLWithQMarks(sqlBld, courseIds);
			removeCourses(con, sql_vals);
			debugPrint("CourseRW.removeCourses: committing.");
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // removeCourses(int[])

	/** Deletes a set of courses.  
	 * @param	con	database connection
	 * @param	crsIdsSql_vals	contains SQL of parenthesized question marks, to be
	 * substituted by the stored values, or a parenthesized 
	 * select statement getting a set of course IDs and containing a question
	 * mark to be substituted by the stored value
	 * @throws	DBException	if there's a problem writing to the database
	 */
	static void removeCourses(Connection con, SQLWithQMarks crsIdsSql_vals) 
			throws DBException {
		final String SELF = "CourseRW.removeCourses: ";
		try {
			debugPrint(SELF + "crsIdsSql_vals = ", crsIdsSql_vals);
			final String crsIdsSql = parens(crsIdsSql_vals.getSql());
			final StringBuilder hwIdsBld = parensBuild(
					HWRead.getHwIdsForCourses(), crsIdsSql);
			final SQLWithQMarks sql_vals = 
					new SQLWithQMarks(hwIdsBld, crsIdsSql_vals);
			debugPrint(SELF + "sql_vals = ", sql_vals);
			HWWrite.deleteHWSets(con, sql_vals);
			ForumRW.deleteAllTopics(con, crsIdsSql_vals);
			final String[][] fieldSets = new String[][] {
					{ENROLLMENT, ENRL_COURSE},
					{PREENROLLMENT, PREENRL_COURSE},
					{EXAM_IDS, EXAM_CRS},
					{COINSTRUCTORS, COINSTR_CRS},
					{ALLOWED_IPS, IPS_COURSE},
					{BLOCKED, BLOCKED_COURSE},
					{COURSES, CRS_ID}
					};
			for (final String[] fields : fieldSets) {
				sql_vals.setSql(
						DELETE_FROM, fields[0], 
						WHERE, fields[1], IN, crsIdsSql);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} // for each table
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} // try
	} // removeCourses(Connection, SQLWithQMarks)

/* ************* Coinstructor functions **********/

	/** Adds a coinstructor to a course. 
	 * @param	courseId	unique ID of a course
	 * @param	instructorId	login ID of the coinstructor to add
	 * @throws	UniquenessException	if the new coinstructor 
	 * is already a coinstructor
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void addCoinstructor(int courseId, String instructorId) 
			throws DBException, UniquenessException {
		final String SELF = "CourseRW.addCoinstructor: ";
		final String qry = SELECT + ALL + FROM + COINSTRUCTORS 
				+ WHERE + COINSTR_CRS + EQUALS + QMARK
				+ AND + COINSTR_INSTR + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				courseId, 
				instructorId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				throw new UniquenessException("This coinstructor "
						+ "already set for this course.");
			}
			final String[] fields = new String[] {
					COINSTR_CRS,
					COINSTR_INSTR}; 
			sql_vals.setSql(getInsertIntoValuesQMarksSQL(
					COINSTRUCTORS, fields));
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
	} // addCoinstructor(int, String)

	/** Gets coinstructors of a course. 
	 * @param	courseId	unique ID of a course
	 * @return	coinstructors of the course
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static User[] getCoinstructors(int courseId) throws DBException {
		final String where = toString(
				WHERE + USER_ID + IN, parens(
					SELECT + COINSTR_INSTR
					+ FROM + COINSTRUCTORS
					+ WHERE + COINSTR_CRS + EQUALS + QMARK));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where, 
				courseId);
		final List<User> users = UserRead.getUsers(sql_vals, !GET_DETAILS);
		return users.toArray(new User[users.size()]);
	} // getCoinstructors(int)

	/** Get all verified instructors at an institution who are not coinstructors
	 * of a particular course.
	 * @param	instnId	ID number of the institution
	 * @param	crsId	unique ID number of the coinstructed course
	 * @return	array of User objects representing the instructors
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static User[] getNoncoinstructors(int instnId, int crsId) 
			throws DBException {
		final String SELF = "CourseRW.getNoncoinstructors: ";
		if (instnId == 0 || crsId == 0) return new User[0];
		final String where = toString(
				WHERE + USER_ROLE + EQUALS, quotes(INSTRUCTOR), 
				AND, bitand(USER_FLAGS, ENABLED), IS_NOT_ZERO 
				+ AND + USER_SCHOOLID + EQUALS + QMARK 
				+ AND + USER_ID + NOT + IN, parens(
					SELECT + COINSTR_INSTR
					+ FROM + COINSTRUCTORS
					+ WHERE + COINSTR_CRS + EQUALS + QMARK));
		final SQLWithQMarks sql_vals = new SQLWithQMarks(where, 
				instnId, 
				crsId);
		final List<User> users = UserRead.getUsers(sql_vals, !GET_DETAILS);
		return (users.toArray(new User[users.size()]));
	} // getNoncoinstructors(int, int)

	/** Removes a coinstructor from a course. 
	 * @param	courseId	unique ID of a course
	 * @param	instructorId	login ID of the coinstructor to remove
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void removeCoinstructor(int courseId, String instructorId)
			throws DBException {
		final String SELF = "CourseRW.removeCoinstructor: ";
		final String qry = DELETE_FROM + COINSTRUCTORS 
				+ WHERE + COINSTR_CRS + EQUALS + QMARK
				+ AND + COINSTR_INSTR + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				courseId,
				instructorId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
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
	} // removeCoinstructor(int, String)

	/** Gets the beginning of SQL query to remove textbooks from courses.
	 * @return	beginning of SQL query
	 */
	static String SET_CRS_BKID_TO_0_WHERE_BKID() {
		return UPDATE + COURSES
				+ SET + CRS_ACEBOOKID + IS_ZERO
				+ WHERE + CRS_ACEBOOKID;
	} // SET_CRS_BKID_TO_0_WHERE_BKID()

	/** Gets the SQL query to get a course ID number by the course
	 * instructor (value to be substituted for question mark later).
	 * @return	the SQL query
	 */
	public static String getCourseIdByCourseInstructor() {
		return SELECT + CRS_ID + FROM + COURSES
				+ WHERE + CRS_INSTRUCTOR + EQUALS + QMARK;
	} // getCourseIdByCourseInstructor()

	/** Gets the beginning of SQL query to get course instructors by the course
	 * ID numbers.
	 * @return	beginning of SQL query
	 */
	public static String getCourseInstructorByCourseId() {
		return SELECT + CRS_INSTRUCTOR + FROM + COURSES
				+ WHERE + CRS_ID + IN;
	} // getCourseInstructorByCourseId()

	/** Gets owners of courses for which the instructor is a coinstructor and 
	 * books for which the instructor is a coauthor.
	 * @param	instructorId	login ID of the instructor
	 * @return	array of partially populated User objects containing names and
	 * login IDs
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static User[] getCoinstructedCrsAndCoauthoredBkOwners(
			String instructorId) throws DBException {
		final String SELF = "CourseRW.getCoinstructedCrsAndCoauthoredBkOwners: ";
		// simple param check
		if (Utils.isEmptyOrWhitespace(instructorId)) return new User[0];
		final SQLWithQMarks bookOwnersWithQMarks = 
				getOwnersOfCoauthoredBooksSQL(instructorId);
		final String qry = toString(
				SELECT, joinAll(
					USER_ID,
					USER_GIVENNAME,
					USER_MIDNAME,
					USER_SURNAME), 
				FROM + USERS 
				+ WHERE + USER_ID + IN, parensBuild(
					SELECT + CRS_INSTRUCTOR
					+ FROM + COURSES
					+ WHERE + CRS_ID + IN, parensBuild(
						SELECT + COINSTR_CRS
						+ FROM + COINSTRUCTORS
						+ WHERE + COINSTR_INSTR + EQUALS + QMARK), 
					UNION_ALL, bookOwnersWithQMarks.getSQL()), 
				ORDER_BY + USER_SORT);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				instructorId);
		sql_vals.addValuesFrom(bookOwnersWithQMarks);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final List<User> owners = new ArrayList<User>();
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final User owner = new User(rs.getString(USER_ID));
				owner.setName(new Name(rs.getString(USER_GIVENNAME),
						rs.getString(USER_MIDNAME), 
						rs.getString(USER_SURNAME)));
				owner.setRole(User.INSTRUCTOR);
				owners.add(owner);
			} // while there's more to read
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		} // try block
		return owners.toArray(new User[owners.size()]); 
	} // getCoinstructedCrsAndCoauthoredBkOwners(String)

	/** Gets SQL to get the owners of books coauthored by an instructor.
	 * @param	instructorId	the instructor
	 * @return	SQL to get the owners of books coauthored by an instructor
	 */
	private static SQLWithQMarks getOwnersOfCoauthoredBooksSQL(
			String instructorId) {
		return TextbookRW.getOwnersOfCoauthoredBooksSQL(instructorId);
	} // getOwnersOfCoauthoredBooksSQL(String)

/* ************* IP address functions **********/

	/** Gets the IP addresses from which a student is allowed to access a
	 * course.
	 * @param	courseId	ID number of the course
	 * @return	array of IP addresses
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[] getAllowedIPAddresses(int courseId) 
			throws DBException {
		final SQLWithQMarks sql_vals = new SQLWithQMarks(QMARK, courseId);
		return getAllowedIPAddresses(sql_vals).get(Integer.valueOf(courseId));
	} // getAllowedIPAddresses(int)

	/** Gets the allowed IP addresses for all of a user's courses.
	 * @param	userId	login ID of the user
	 * @param	role	the role of the user (STUDENT or other)
	 * @return	map of arrays of IP addresses keyed by course ID
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Map<Integer, String[]> getAllowedIPAddresses(
			String userId, char role) throws DBException {
		final String selector = (role == STUDENT
				? SELECT + ENRL_COURSE 
					+ FROM + ENROLLMENT
					+ WHERE + ENRL_STUDENT + EQUALS + QMARK
				: SELECT + COINSTR_CRS
					+ FROM + COINSTRUCTORS
					+ WHERE + COINSTR_INSTR + EQUALS + QMARK 
					+ UNION_ALL + SELECT + CRS_ID + FROM + COURSES 
					+ WHERE + CRS_INSTRUCTOR + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(selector, userId);
		if (role != STUDENT) sql_vals.addValue(userId);
		return getAllowedIPAddresses(sql_vals);
	} // getAllowedIPAddresses(String, char)

	/** For one or more courses, gets the IP addresses from which students
	 * are allowed to access them.
	 * @param	crsIdsSql_vals	SQL containing ID number of one course, or 
	 * select statement getting a group of courses, plus the values to
	 * substitute for the question marks in the SQL (1-2)
	 * @return	map of arrays of IP addresses keyed by course ID
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static Map<Integer, String[]> getAllowedIPAddresses(
			SQLWithQMarks crsIdsSql_vals) throws DBException {
		final String SELF = "CourseRW.getAllowedIPAddresses: ";
		final String qry = toString(
				SELECT, joinAll(
					IPS_COURSE,
					IPS_ADDRESS), 
				FROM + ALLOWED_IPS
				+ WHERE + IPS_COURSE + IN, parens(crsIdsSql_vals.getSQL()), 
				ORDER_BY + IPS_COURSE);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, crsIdsSql_vals);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final Map<Integer, String[]> ips = new HashMap<Integer, String[]>();
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) while (!rs.isAfterLast()) {
				final int crsId = rs.getInt(IPS_COURSE);
				final List<String> crsIPs = new ArrayList<String>();
				while (!rs.isAfterLast()
						&& crsId == rs.getInt(IPS_COURSE)) {
					crsIPs.add(rs.getString(IPS_ADDRESS));
					rs.next();
				} // while there are more addresses in this course
				ips.put(Integer.valueOf(crsId), 
						crsIPs.toArray(new String[crsIPs.size()]));
			} // while there are more courses
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return ips;
	} // getAllowedIPAddresses(SQLWithQMarks, String)

	/** Sets the IP addresses from which a student is allowed to access a
	 * course.
	 * @param	courseId	ID number of the course
	 * @param	ipAddrs	array of IP addresses
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setAllowedIPAddresses(int courseId, 
			String[] ipAddrs) throws DBException {
		final String SELF = "CourseRW.setAllowedIPAddresses: ";
		String qry = DELETE_FROM + ALLOWED_IPS
				+ WHERE + IPS_COURSE + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				courseId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			tryUpdate(con, sql_vals);
			if (!Utils.isEmpty(ipAddrs)) {
				final String[] fields = new String[] {
						IPS_COURSE,
						IPS_ADDRESS}; 
				qry = getInsertIntoValuesQMarksSQL(ALLOWED_IPS, fields);
				debugPrint(SELF, qry); 
				stmt = con.prepareStatement(qry);
				int batchNum = 0;
				for (final String address : ipAddrs) {
					final StringBuilder joinedValues = setValues(stmt, 
							courseId, 
							address);
					debugPrint(SELF, "batch ", ++batchNum, ": ", joinedValues); 
					stmt.addBatch();
				} // for each address
				stmt.executeBatch();
			} // if there are IP addresses to add
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, null);
		}
	} // setAllowedIPAddresses(int, String[])

/* ************* Marvin Live functions **********/

	/** Sets the port used for Marvin Live by a course. 
	 * @param	crsId	ID of course whose port is being set
	 * @param	port	port number of Marvin Live session
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setMarvinLivePort(int crsId, int port) 
			throws DBException {
		final String SELF = "CourseRW.setMarvinLivePort: ";
		final String qry = UPDATE + COURSES 
				+ SET + CRS_PORT + EQUALS + QMARK
				+ WHERE + CRS_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				port,
				crsId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
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
	} // setMarvinLivePort(int, int)

	/** Gets the port of the Marvin Live session associated with the course.
	 * @param	crsId	ID of course
	 * @return	the port number
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static int getMarvinLivePort(int crsId) throws DBException {
		final String SELF = "CourseRW.getMarvinLivePort: ";
		final String qry = SELECT + CRS_PORT
				+ FROM + COURSES
				+ WHERE + CRS_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				crsId);
		debugPrint(SELF, sql_vals);
		int port = 0;
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) port = rs.getInt(CRS_PORT);
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
		return port;
	} // getMarvinLivePort(int)

/* ************* Miscellaneous functions **********/

	/** Sets flags of an array of courses. 
	 * @param	courses	array of courses
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setFlags(Course[] courses) throws DBException {
		final String SELF = "CourseRW.setFlags: ";
		final String qry = UPDATE + COURSES
				+ SET + CRS_FLAGS + EQUALS + QMARK
				+ WHERE + CRS_ID + EQUALS + QMARK;
		debugPrint(SELF, qry); 
		Connection con = null;
		PreparedStatement stmt = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			stmt = con.prepareStatement(qry);
			int batchNum = 0;
			for (final Course course : courses) {
				final StringBuilder joinedValues = setValues(stmt, 
						course.getFlags(), 
						course.getId());
				debugPrint(SELF, "batch ", ++batchNum, ": ", joinedValues); 
				stmt.addBatch();
			} // for each course whose flags need to be set
			stmt.executeBatch();
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, null);
		}
	} // setFlags(Course[])

	/** Gets an alphabetized array of all course books stored in database.
	 * @return	all course books stored in database, alphabetized
	 */
	public static String[] getAllBooks() {
		final String SELF = "CourseRW.getAllBooks: ";
		final String qry = SELECT_UNIQUE + CRS_BOOK + FROM + COURSES;
		debugPrint(SELF, qry);
		final List<String> allBooksList = new ArrayList<String>();
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(qry);
			while (rs.next()) {
				final String book = rs.getString(CRS_BOOK);
				if (!Utils.isEmpty(book) && !"Other".equals(book)) {
					allBooksList.add(book);
				} // while there are more books to add
			} // while there are results
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
		} finally {
			closeConnection(con, stmt, rs);
		}
		final String[] allBooks = 
				allBooksList.toArray(new String[allBooksList.size()]);
		Arrays.sort(allBooks);
		debugPrint(SELF + "returning ", allBooks);
		return allBooks;
	} // getAllBooks()

	/** Disables external instantiation. */
	private CourseRW() { }

} // CourseRW
