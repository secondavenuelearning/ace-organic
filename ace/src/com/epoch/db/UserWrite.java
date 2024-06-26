package com.epoch.db;

import static com.epoch.db.dbConstants.CourseRWConstants.*;
import static com.epoch.db.dbConstants.ForumRWConstants.*;
import static com.epoch.db.dbConstants.LanguagesConstants.*;
import static com.epoch.db.dbConstants.QuestionsRWConstants.*;
import static com.epoch.db.dbConstants.ResponsesConstants.*;
import static com.epoch.db.dbConstants.TextbookRWConstants.*;
import static com.epoch.db.dbConstants.UserRWConstants.*;
import com.epoch.courseware.Name;
import com.epoch.courseware.User;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.StudentEmailExistsException;
import com.epoch.exceptions.StudentNumConstraintException;
import com.epoch.exceptions.UniquenessException;
import com.epoch.utils.Utils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Database read-write operations related to students and instructors. */
public final class UserWrite extends DBCommon {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

/* ***************** User management functions *****************/

	/** Adds a new user; may also add a new institution or change the name of
	 * the institution's student ID number.
	 * @param	user	User containing the new information
	 * @throws	UniquenessException	when the login already exists
	 * @throws	StudentNumConstraintException	when the student's ID number
	 * already exists at this institution and the student is not an exam student
	 * (temporary login)
	 * @throws	StudentEmailExistsException	when the student's email
	 * already exists at this institution and the student is not an exam student
	 * (temporary login)
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void addUser(User user) throws DBException,
			UniquenessException, StudentNumConstraintException,
			StudentEmailExistsException {
		final String SELF = "UserWrite.addUser: ";
		Connection con = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			final int instnId = user.getInstitutionId();
			if (instnId == 0) {
				InstitutionRW.addInstitution(con, user.getInstitution());
			} else if (instnId < 0) {
				InstitutionRW.setInstitutionStudentNumLabel(con, 
						user.getInstitution());
			} // if institution is new
			addUsers(con, new User[] {user});
			con.commit();
		} catch (SQLException e) {
			rollbackConnection(con);
			if (e.getErrorCode() == OracleErrors.UNIQUECONSTRAINT_ERROR)
				throw new UniquenessException(" in User.addUser()");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // addUser(User)

	/** Adds temporary logins for use in exams to the users and
	 * exam-students tables.
	 * @param	students	exam-students
	 * @param	courseId	ID of the course with which they are associated
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void addExamStudents(User[] students, int courseId)
			throws DBException {
		final String SELF = "UserWrite.addExamStudents: ";
		final String[] fields = new String[] {
				EXAM_STUDENT,
				EXAM_CRS,
				EXAM_CREATED};
		final String qry = getInsertIntoValuesQMarksSQL(EXAM_IDS, fields);
		debugPrint(SELF, qry); 
		Connection con = null;
		PreparedStatement stmt = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			addUsers(con, students);
			stmt = con.prepareStatement(qry);
			int studNum = 1;
			for (final User student : students) {
				final String userId = student.getUserId();
				final StringBuilder joinedValues = setValues(stmt,
						userId,
						courseId,
						dateToString(new Date()));
				debugPrint(SELF, "batch ", studNum, ": ", joinedValues); 
				stmt.addBatch();
				studNum++;
				if (studNum % 100 == 1) {
					stmt.executeBatch();
					stmt.clearBatch();
				} // if time to submit
			} // for each exam-student
			if (studNum % 100 != 1) {
				stmt.executeBatch();
			} // if need to submit
			con.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} catch (UniquenessException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} catch (StudentNumConstraintException e) {
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} catch (StudentEmailExistsException e) {
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
	} // addExamStudents(String[], int)

	/** Adds new users.  Connection autocommit should be set to false before
	 * coming here.
	 * @param	con	database connection
	 * @param	users	array of Users containing the new information
	 * @throws	UniquenessException	when the login already exists
	 * @throws	StudentNumConstraintException	when the student's ID number
	 * already exists at this institution and the student is not an exam student
	 * (temporary login)
	 * @throws	StudentEmailExistsException	when the student's email
	 * already exists at this institution and the student is not an exam student
	 * (temporary login)
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void addUsers(Connection con, User[] users) 
			throws SQLException, UniquenessException, 
			StudentNumConstraintException, StudentEmailExistsException {
		final String SELF = "UserWrite.addUsers: ";
		final String[] fields = new String[] {
				USER_ID,
				USER_GIVENNAME,
				USER_MIDNAME,
				USER_SURNAME,
				USER_ROLE,
				USER_FLAGS,
				USER_SCHOOLID,
				USER_STUDENTNUM,
				USER_EMAIL,
				USER_TXT_MSG,
				USER_ADDRESS,
				USER_PHONE,
				USER_REGD,
				USER_SORT,
				USER_PWDHASH};
		final String qry = getInsertIntoValuesQMarksSQL(USERS, fields);
		debugPrint(SELF, qry);
		PreparedStatement stmt = null;
		try {
			stmt = con.prepareStatement(qry);
			int userNum = 0;
			for (final User user : users) {
				final String userId = user.getUserId();
				final Name name = user.getName();
				final String studentNum = user.getStudentNum();
				final int instnId = user.getInstitutionId();
				final String email = user.getEmail();
				debugPrint(SELF + "setting record for ", userId, ": ",
						name, ", school ID ", instnId, ", ", email, ", ", 
						studentNum);
				// check if the login already exists
				if (UserRead.loginExists(con, userId)) {
					throw new UniquenessException( "The login ID "
							+ userId + " has already been selected."
							+ " Please enter another login ID.");
				}
				// check if the student number already exists
				final char role = user.getRole();
				if (role == STUDENT) {
					final String[] otherNames = UserRead.getSameStudentNums(con, 
							studentNum, instnId);
					if (otherNames != null) {
						alwaysPrint(SELF + "Found ", otherNames, 
								" at school with ID ", instnId, 
								" have same student number ",
								quotes(studentNum), " as ", name);
						throw new StudentNumConstraintException(studentNum);
					} // if other students have same student ID number
				} // if a student
				// check if the email address already exists
				if (role == STUDENT
						&& !Utils.isEmptyOrWhitespace(email)) {
					final String[] otherNames = 
							UserRead.getSameStudentEmails(con, email, instnId);
					if (otherNames != null) {
						alwaysPrint(SELF + "Found ", otherNames,
								" at school with ID ", instnId, 
								" have same email address ", 
								email, " as ", name);
						throw new StudentEmailExistsException(email);
					} // if other students have same email
				} // if student number already exists
				final String regDate = dateToString(new Date());
				final String sortKey = Utils.chopString(
						Utils.cersToAlphabetical(name.toString()).
							toUpperCase(Locale.ENGLISH),
						USER_SORT_LEN);
				final StringBuilder joinedValues = setValues(stmt,
						userId,
						name.givenName,
						name.middleName,
						name.familyName,
						String.valueOf(role),
						user.getFlags(),
						instnId,
						trimNullToEmpty(studentNum),
						trimNullToEmpty(email),
						trimNullToEmpty(user.getTextMessageEmail()),
						trimNullToEmpty(user.getAddress()),
						trimNullToEmpty(user.getPhone()),
						regDate,
						sortKey,
						user.getPasswordHash());
				debugPrint(SELF, "batch ", userNum + 1, ": ", joinedValues, 
						" [password hash]");
				stmt.addBatch();
				userNum++;
				if (userNum % 100 == 0) {
					stmt.executeBatch();
					stmt.clearBatch();
				} // if time to submit
			} // for each student
			if (userNum % 100 != 0) {
				stmt.executeBatch();
			} // if need to submit
		} finally {
			closeConnection(null, stmt, null);
		} // try
	} // addUsers(Connection, User[])

	/** Changes a user's information; may also add a new institution or change 
	 * the name of the institution's student ID number.  Disallows student 
	 * changing student ID number to one already chosen by another student at 
	 * the same institution, unless the student is working in an account 
	 * created for exams.  Also disallows student changing email address to one 
	 * already used by a student with a different student ID number at the same 
	 * institution.  If user is a student and studentNum or institution 
	 * changes, also enrolls the student in courses in which he or she is 
	 * preenrolled, although it does NOT disenroll the student from any courses.
	 * @param	user	User containing the new information
	 * @throws	UniquenessException	when the login already exists
	 * @throws	StudentNumConstraintException	when the student's ID number
	 * already exists at this institution and the student is not an exam student
	 * (temporary login)
	 * @throws	StudentEmailExistsException	when the student's email
	 * already exists at this institution and the student is not an exam student
	 * (temporary login)
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setUser(User user) throws DBException,
			UniquenessException, StudentNumConstraintException,
			StudentEmailExistsException {
		final String SELF = "UserWrite.setUser: ";
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final String userId = user.getUserId();
		final char role = user.getRole();
		final Name name = user.getName();
		final String studentNum = user.getStudentNum();
		int instnId = user.getInstitutionId();
		final String email = user.getEmail();
		try {
			con = getPoolConnection();
			if (instnId == 0) {
				InstitutionRW.addInstitution(con, user.getInstitution());
				instnId = user.getInstitutionId();
			} else if (instnId < 0) {
				InstitutionRW.setInstitutionStudentNumLabel(con, 
						user.getInstitution());
				instnId *= -1;
			} // if institution is new
			// check if the user's studentNum already exists
			if (role == STUDENT
					&& !UserRead.isExamStudent(con, userId)) {
				String[] otherNames = UserRead.getSameStudentNums(con,
						studentNum, instnId, userId);
				if (otherNames != null) {
					alwaysPrint(SELF + "Found ", otherNames,
							" at school with ID ", instnId, 
							" have same student number ",
							quotes(studentNum), " as ", name);
					throw new StudentNumConstraintException(
							studentNum);
				} // if there are students with same student number
				otherNames = UserRead.getSameStudentEmails(con, email, 
						instnId, userId);
				if (otherNames != null) {
					alwaysPrint(SELF + "Found ", otherNames,
							" at school with ID ", instnId, 
							" have same email address ", 
							email, " as ", name);
					throw new StudentEmailExistsException(email);
				} // if there are students with same student number
			} // if user is student & not temporary exam ID
			// get user's existing studentNum and school; may need to change
			// enrollment in courses
			String qry = toString(
					SELECT, joinAll(
						USER_STUDENTNUM,
						USER_SCHOOLID),
					FROM + USERS
					+ WHERE + USER_ID + EQUALS + QMARK);
			SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
					userId);
			String prevStudentNum = null;
			int prevInstnId = 0;
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) { // should always give one result
				prevStudentNum = rs.getString(USER_STUDENTNUM);
				if (prevStudentNum != null)
					prevStudentNum = prevStudentNum.trim();
				prevInstnId = rs.getInt(USER_SCHOOLID);
			} // if there's a result
			if (role != ADMINISTRATOR)
				debugPrint(SELF + "updating ",
						(role == STUDENT ? "student"
							: "instructor"), " record in USERS: ",
						name, ", school ID ", instnId, ", ", email, ", ", 
						studentNum);
			else alwaysPrint(SELF + "updating administrator record in USERS.");
			final String sortkey = 
					Utils.cersToAlphabetical(name.toString()).toUpperCase(Locale.ENGLISH);
			qry = toString(
					UPDATE + USERS + SET, equalsJoinQMarks(
						USER_GIVENNAME,
						USER_MIDNAME,
						USER_SURNAME,
						USER_ROLE,
						USER_FLAGS,
						USER_SCHOOLID,
						USER_STUDENTNUM,
						USER_EMAIL,
						USER_TXT_MSG,
						USER_ADDRESS,
						USER_PHONE,
						USER_SORT),
					WHERE + USER_ID + EQUALS + QMARK);
			sql_vals = new SQLWithQMarks(qry,
					name.givenName,
					name.middleName,
					name.familyName,
					role,
					user.getFlags(),
					instnId,
					trimNullToEmpty(studentNum),
					trimNullToEmpty(email),
					trimNullToEmpty(user.getTextMessageEmail()),
					trimNullToEmpty(user.getAddress()),
					trimNullToEmpty(user.getPhone()),
					Utils.chopString(sortkey, USER_SORT_LEN),
					userId);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
			if (user.changePassword()) setPassword(con, user);
			// if student, and studentNum or institution has
			// changed, move from preenroll to enroll in appropriate courses
			// but do not disenroll from any courses
			if (role == STUDENT
					&& (!studentNum.equals(prevStudentNum)
						|| instnId != prevInstnId)) {
				alwaysPrint(SELF,
						!studentNum.equals(prevStudentNum)
							? "studentNum has changed; " : "",
						instnId != prevInstnId
							? "student school has changed; " : "",
						"enrolling in courses in which he is preenrolled.");
				EnrollmentRW.enrollInQualified(user);
			} // if school or student number has changed
		} catch (SQLException e) {
			if (e.getErrorCode() == OracleErrors.UNIQUECONSTRAINT_ERROR)
				throw new UniquenessException(" in setUser()");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, stmt, rs);
		}
	} // setUser(User)

	/** Changes a user's password.  
	 * @param	user	User containing the new information
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setPassword(User user) throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			setPassword(con, user);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // setPassword(User)

	/** Changes a user's password.  
	 * @param	con	database connection
	 * @param	user	User containing the new information
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void setPassword(Connection con, User user) throws SQLException {
		final String SELF = "UserWrite.setPassword: ";
		final String qry =
				UPDATE + USERS 
				+ SET + USER_PWDHASH + EQUALS + QMARK 
				+ WHERE + USER_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				user.getPasswordHash(),
				user.getUserId());
		debugPrint(SELF, sql_vals);
		tryUpdate(con, sql_vals);
	} // setPassword(Connection, User)

	/** Adds a user's payment tracking ID to the database.
	 * @param	userId	the user's ID
	 * @param	trackingId	the tracking ID of the payment
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setPaymentId(String userId, String trackingId) 
			throws DBException {
		final String SELF = "UserWrite.setPaymentId: ";
		final String qry =
				UPDATE + USERS 
				+ SET + USER_PAYMENT + EQUALS + QMARK 
				+ WHERE + USER_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				trackingId,
				userId);
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
	} // setPaymentId(String, String)

	/** Sets the user's date of last login to now.
	 * @param	userId	the user's ID
	 */
	public static void setLoginDateToNow(String userId) {
		final String SELF = "UserWrite.setLoginDateToNow: ";
		final String qry =
				UPDATE + USERS 
				+ SET + USER_LASTLOGIN + EQUALS + QMARK 
				+ WHERE + USER_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				dateToString(new Date()),
				userId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "unable to store date of most recent "
					+ "login");
			e.printStackTrace();
		} finally {
			closeConnection(con);
		}
	} // setLoginDateToNow(String)

	/** Stores a user's flags.
	 * @param	user	the user
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setUserFlags(User user) throws DBException {
		final String SELF = "UserWrite.setUserFlags: ";
		final String qry = 
				UPDATE + USERS 
				+ SET + USER_FLAGS + EQUALS + QMARK
				+ WHERE + USER_ID + EQUALS + QMARK;
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				user.getFlags(),
				user.getUserId());
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, null, null);
		} // try
	} // setUserFlags(User)

	/** For all users who have the same institution and student ID number as 
	 * the given student, sets their flags so they cannot see calculated synthesis
	 * products in the feedback in any course. Used when entering an exam
	 * assignment. If an exam account is being used by a student, works on both
	 * the exam account and the student's original account, assuming same
	 * institution and student ID number in both.
	 * @param	userId	the user's ID
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setMayNotSeeSynthCalcdProds(String userId) 
			throws DBException {
		final String SELF = "UserWrite.setMayNotSeeSynthCalcdProds: ";
		final String qry = toString(
				UPDATE + USERS 
				+ SET + USER_FLAGS + EQUALS 
					+ USER_FLAGS + PLUS + DONT_SHOW_CALCD_SYNTH_PRODS
				+ WHERE, parens(joinAll(
					USER_SCHOOLID,
					clobToString(USER_STUDENTNUM))),
				IN, parensBuild(
					SELECT, joinAll(
						USER_SCHOOLID,
						clobToString(USER_STUDENTNUM)),
					FROM + USERS
					+ WHERE + USER_ID + EQUALS + QMARK),
				AND, bitand(USER_FLAGS, DONT_SHOW_CALCD_SYNTH_PRODS), IS_ZERO);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				userId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con, null, null);
		} // try
	} // setMayNotSeeSynthCalcdProds(String)

	/** Removes a user.  If an instructor, removes locally authored
	 * questions, courses, assignments, responses to those assignments,
	 * coinstructorships, locally authored question sets, modified
	 * common question statements, authoring privileges.  If a student,
	 * removes responses, instantiated assignments, and enrollment records.
	 * @param	userId	login ID of user
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void removeUser(String userId) throws DBException {
		removeUsers(new String[] {userId});
	} // removeUser(String)

	/** Removes users.  Divides the users among instructors and students.
	 * Removes instructors' locally authored questions, courses, assignments,
	 * responses to those assignments, coinstructorships, locally authored
	 * question sets, modified common question statements, authoring
	 * privileges, languages. Removes students' responses, instantiated 
	 * assignments, watched forum topics, and enrollment records.
	 * @param	userIds	array of login ID of users
	 * @return	list of two-string arrays containing name and email address of
	 * removed users
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static List<String[]> removeUsers(String[] userIds) 
			throws DBException {
		final String SELF = "UserWrite.removeUsers: ";
		final List<String[]> removedUserIds = new ArrayList<String[]>();
		if (Utils.isEmpty(userIds)) return removedUserIds;
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			// divide users into bunches
			final List<String[]> userIdsArrs = getStrGroups(userIds);
			// divide users among instructors and students
			final List<String> studentUserIds = new ArrayList<String>();
			final List<String> instrUserIds = new ArrayList<String>();
			for (final String[] userIdsArr : userIdsArrs) {
				final String qry = toString(
						SELECT, joinAll(
							USER_ROLE, 
							USER_EMAIL,
							USER_GIVENNAME,
							USER_MIDNAME,
							USER_SURNAME,
							USER_ID),
						FROM + USERS
						+ WHERE + USER_ID + IN, parensQMarks(userIdsArr));
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
				sql_vals.addValuesArray(userIdsArr);
				debugPrint(SELF, sql_vals);
				stmt = getStatement(con, sql_vals);
				rs = stmt.executeQuery();
				while (rs.next()) {
					final char role = rs.getString(USER_ROLE).charAt(0);
					final String userId = rs.getString(USER_ID);
					final String name = (new Name(rs.getString(USER_GIVENNAME),
							rs.getString(USER_MIDNAME),
							rs.getString(USER_SURNAME)).toString());	
					final String email = 
							trimNullToEmpty(rs.getString(USER_EMAIL));
					removedUserIds.add(new String[] {name, email}); 
					if (role == INSTRUCTOR) instrUserIds.add(userId);
					else studentUserIds.add(userId);
				} // for each user
				closeStmtAndRs(stmt, rs);
			} // for each bunch of users
			debugPrint(SELF + "found instrUserIds = ", instrUserIds,
					" and ", studentUserIds.size(), " studentUserId(s).");
			con.setAutoCommit(false);
			// divide instructors into bunches
			final List<String[]> instrUserIdsArrs = getStrGroups(instrUserIds);
			debugPrint(SELF + "grouped, unenquoted instrUserIdsArrs = ", 
					instrUserIdsArrs);
			for (final String[] instrIdsArr : instrUserIdsArrs) {
				final StringBuilder instrQMarks = parensQMarks(instrIdsArr);
				final String qry = toString(
						SELECT + CRS_ID 
						+ FROM + COURSES
						+ WHERE + CRS_INSTRUCTOR + IN, instrQMarks);
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
				sql_vals.addValuesArray(instrIdsArr);
				CourseRW.removeCourses(con, sql_vals);
				QuestionRW.deleteQuestions(con, instrIdsArr);
				TextbookRW.deleteBooks(con, instrIdsArr);
				final String[][] fieldSets = new String[][] {
						{COAUTHORS, COAUTH_COAUTHOR},
						{COINSTRUCTORS, COINSTR_INSTR},
						{MODIFIED_HEADERS, MODHEAD_AUTHOR},
						{QSETS, QSET_AUTHOR}
						};
				for (final String[] fields : fieldSets) {
					sql_vals.setSql( // values don't change
							DELETE_FROM, fields[0],
							WHERE, fields[1], IN, instrQMarks);
					debugPrint(SELF, sql_vals);
					tryUpdate(con, sql_vals);
				} // for each table and field
			} // for each bunch of instructors to delete
			// divide students into smaller bunches
			final List<String[]> studUserIdsArrs = 
					getStrGroups(studentUserIds);
			debugPrint(SELF + "grouped, unenquoted studUserIdsArrs = ", 
					studUserIdsArrs);
			for (final String[] studIdsArr : studUserIdsArrs) {
				final StringBuilder studQMarks = parensQMarks(studIdsArr);
				final SQLWithQMarks sql_vals = new SQLWithQMarks();
				sql_vals.setValuesArray(studIdsArr);
				final String[][] fieldSets = new String[][] {
						{WATCHED_TOPICS, WATCHED_STUDENT},
						{BLOCKED, BLOCKED_USER},
						{RESP_SUBSTNS, RESP_SUBS_STUDENT},
						{RESPONSES, RESP_STUDENT},
						{ASSIGNED_QS, ASSGND_QS_STUDENT},
						{EXTENSIONS, EXT_STUDENT},
						{ENROLLMENT, ENRL_STUDENT},
						{EXAM_IDS, EXAM_STUDENT}
						};
				for (final String[] fields : fieldSets) {
					sql_vals.setSql( // values don't change
							DELETE_FROM, fields[0],
							WHERE, fields[1], IN, studQMarks);
					debugPrint(SELF, sql_vals);
					tryUpdate(con, sql_vals);
				} // for each table and field
			} // for each bunch of students to delete
			debugPrint(SELF + "grouped, unenquoted userIdsArrs = ", userIdsArrs);
			for (final String[] userIdsArr : userIdsArrs) {
				final StringBuilder userQMarks = parensQMarks(userIdsArr);
				final String[][] fieldSets = new String[][] {
						{LANGUAGES, LANG_USER},
						{USERS, USER_ID}
						};
				for (final String[] fields : fieldSets) {
					final String qry = toString(
							DELETE_FROM, fields[0],
							WHERE, fields[1], IN, userQMarks);
					final SQLWithQMarks sql_vals = new SQLWithQMarks(qry);
					sql_vals.addValuesArray(userIdsArr);
					debugPrint(SELF, sql_vals);
					tryUpdate(con, sql_vals);
				} // for each table and field
			} // for each bunch of users
			final String qry = toString(
					DELETE_FROM + INSTITUTIONS
					+ WHERE + INSTN_ID + NOT + IN, parens(
						SELECT_UNIQUE + USER_SCHOOLID + FROM + USERS)
					);
			debugPrint(SELF, qry);
			tryUpdate(con, qry);
			/* if (userIds.length == 1) { /**/ 
				con.commit();
			/* } else throw new SQLException(SELF + "As of April 2024, "
					+ "only one user can be removed at a time."); /**/
		} catch (SQLException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (DBException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw e;
		} finally {
			closeConnection(con, stmt, rs);
		}
		return removedUserIds;
	} // removeUsers(String[])

	/** Deletes users who have no responses and have not logged in since a
	 * particular date.  
	 * @param	y_m_d	int array of years, months, days
	 * @param	instnId	the ID of the institution, or all of them, whose 
	 * students have been inactive
	 * @return	list of two-string arrays containing name and email address of
	 * removed users
	 */
	public static List<String[]> removeInactiveStudents(int[] y_m_d, 
			int instnId) {
		final String SELF = "UserWrite.removeInactiveStudents: ";
		List<String[]> removedStudents = new ArrayList<String[]>();
		try {
			final String[] inactiveStudents = 
					UserRead.getInactiveStudents(y_m_d, instnId);
			removedStudents = removeUsers(inactiveStudents);
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + "DBException: no inactive users removed.");
		}
		debugPrint(SELF, removedStudents.size(), " user(s) deleted.");
		return removedStudents;
	} // removeInactiveStudents(String[])

	/** Saves a new userId for a student at the institution and with the 
	 * student ID number, and changes the same value in all other tables 
	 * with a column that depends on USERS.USER_ID. Used to replace userIds 
	 * that have non-ASCII characters with their CER equivalents. 
	 * @param	newUserId	the new userId
	 * @param	instnId	the institution ID
	 * @param	studentNum	the student number at that institution
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void changeUserId(String newUserId, int instnId, 
			String studentNum) throws DBException {
		final String SELF = "UserWrite.changeUserId: ";
		final List<String[]> failedUpdateTablesCols = new ArrayList<String[]>();
		final String[] indepTableCol = new String[] {USERS, USER_ID};
		String updateStr = "";
		Connection con = null;
		try {
			final String whereSQL = 
					WHERE + USER_SCHOOLID + EQUALS + QMARK
					+ AND + USER_STUDENTNUM + LIKE + QMARK;
			con = getPoolConnection();
			final List<String[]> depTablesCols = 
					DBSchema.getDepTablesCols(con);
			con.setAutoCommit(false);
			// defer the deferrable constraints
			updateStr = SET + " CONSTRAINTS ALL DEFERRED";
			debugPrint(SELF, updateStr);
			tryUpdate(con, updateStr);
			// change the userId value in each dependent table and column
			for (final String[] depTableCol : depTablesCols) {
				updateStr = toString(
						UPDATE, depTableCol[TABL_NUM],
						SET, depTableCol[COL_NUM], EQUALS + QMARK
						+ WHERE, depTableCol[COL_NUM], IN, parensBuild(
							SELECT + USER_ID
							+ FROM + USERS,
							whereSQL));
				final SQLWithQMarks sql_vals = new SQLWithQMarks(updateStr,
						newUserId,
						instnId,
						studentNum);
				debugPrint(SELF, sql_vals);
				try {
					tryUpdate(con, sql_vals);
				} catch (SQLException e) {
					debugPrint(SELF + "dependent table and column ",
							depTableCol, " failed to update.");
					failedUpdateTablesCols.add(depTableCol);
				}
			} // for each dependent table and column
			// change the userId value in the independent table and column
			updateStr = toString(
					UPDATE + indepTableCol[TABL_NUM] 
					+ SET + indepTableCol[COL_NUM] + EQUALS + QMARK,
					whereSQL);
			final SQLWithQMarks sql_vals = new SQLWithQMarks(updateStr,
					newUserId,
					instnId,
					studentNum);
			debugPrint(SELF, sql_vals);
			try {
				tryUpdate(con, sql_vals);
			} catch (SQLException e) {
				debugPrint(SELF + "independent table and column ",
						indepTableCol, " also failed to update.");
				failedUpdateTablesCols.add(indepTableCol);
			}
			// deferrable constraints set back to not deferred upon commit!
			con.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		} // try
		final int numFailedUpdates = failedUpdateTablesCols.size();
		if (numFailedUpdates > 0) {
			alwaysPrint(SELF, numFailedUpdates, " update(s) failed: ",
					failedUpdateTablesCols);
		} // if there were failed updates
	} // changeUserId(String, int, String)

/* ***************** Language functions *****************/

	/** Adds a new language for a user.
	 * @param	userId	login ID of the user
	 * @param	language	name of language
	 * @throws	DBException	if language has already been selected
	 */
	public static void addLanguage(String userId, String language)
			throws DBException {
		final String SELF = "UserWrite.addLanguage: ";
		String qry = 
				SELECT + LANG_NAME
				+ FROM + LANGUAGES
				+ WHERE + LANG_USER + EQUALS + QMARK
				+ AND + LANG_NAME + EQUALS + QMARK;
		SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				userId,
				language);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getPoolConnection();
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (!rs.next()) { // language not already stored for this user
				final String[] currentLanguages = 
						UserRead.getLanguages(con, userId);
				final int preference = Utils.getLength(currentLanguages) + 1;
				final String[] fields = new String[] {
						LANG_USER,
						LANG_NAME,
						LANG_PREF};
				qry = getInsertIntoValuesQMarksSQL(LANGUAGES, fields);
				sql_vals = new SQLWithQMarks(qry,
						userId,
						language,
						preference);
				debugPrint(SELF, sql_vals);
				tryUpdate(con, sql_vals);
			} else {
				alwaysPrint(SELF, language, " already stored for ", userId);
				/**/ throw new DBException("Language " + language
						+ " has already been recorded for this user."); /**/
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
	} // addLanguage(String, String)

	/** Sets the order in which the chosen languages should be used.
	 * @param	userId	login ID of the user
	 * @param	newOrder	new position of each language in current order of
	 * languages
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setLanguageOrder(String userId, int[] newOrder)
			throws DBException {
		Connection con = null;
		try {
			con = getPoolConnection();
			final String[] languages = UserRead.getLanguages(con, userId);
			setLanguageOrder(con, userId, newOrder, languages);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // setLanguageOrder(String, int[])

	/** Sets the order in which the chosen languages should be used.
	 * @param	con	database connection
	 * @param	userId	login ID of the user
	 * @param	newOrder	new position of each language in current order of
	 * languages
	 * @param	languages	current languages of the user
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	private static void setLanguageOrder(Connection con, String userId,
			int[] newOrder, String[] languages) throws SQLException {
		final String SELF = "UserWrite.setLanguageOrder: ";
		if (languages.length == 0) return;
		final String qry = 
				UPDATE + LANGUAGES
				+ SET + LANG_PREF + EQUALS + QMARK
				+ WHERE + LANG_NAME + EQUALS + QMARK
				+ AND + LANG_USER + EQUALS + QMARK;
		debugPrint(SELF, qry);
		PreparedStatement stmt = null;
		try {
			stmt = con.prepareStatement(qry);
			int batchNum = 1;
			for (int langNum = 0; langNum < languages.length; langNum++) {
				final StringBuilder joinedValues = setValues(stmt,
						newOrder[langNum],
						languages[langNum],
						userId);
				debugPrint(SELF, "batch ", batchNum++, ": ", joinedValues); 
				stmt.addBatch();
			} // for each new position of a language
			stmt.executeBatch();
		} finally {
			closeConnection(null, stmt, null);
		}
	} // setLanguageOrder(Connection, String, int[], String[])

	/** Removes one of this user's languages.
	 * @param	userId	login ID of the user
	 * @param	removePosn	1-based position of the language to be removed, in the
	 * current order of the languages
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void removeLanguage(String userId, int removePosn)
			throws DBException {
		final String SELF = "UserWrite.removeLanguage: ";
		Connection con = null;
		try {
			con = getPoolConnection();
			con.setAutoCommit(false);
			final String[] languages = UserRead.getLanguages(con, userId);
			final String removeLang = languages[removePosn - 1];
			final int numLangs = languages.length;
			if (removePosn != numLangs) {
				// reorder languages so one to be removed is last
				int[] newOrder = new int[numLangs];
				for (int langNum = 0; langNum < numLangs; langNum++) {
					final int langPosn = langNum + 1;
					if (langPosn < removePosn) {
						newOrder[langNum] = langPosn;
					} else if (langPosn > removePosn) {
						newOrder[langNum - 1] = langPosn;
					}
				} // for each language
				newOrder[numLangs - 1] = removePosn;
				setLanguageOrder(con, userId, newOrder, languages);
			} // if languages need to be reordered before deleting one
			final String qry = 
					DELETE_FROM + LANGUAGES
					+ WHERE + LANG_USER + EQUALS + QMARK
					+ AND + LANG_NAME + EQUALS + QMARK;
			final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
					userId,
					removeLang);
			debugPrint(SELF, sql_vals);
			tryUpdate(con, sql_vals);
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
			closeConnection(con);
		}
	} // setLanguageOrder(String, int)

/* ***************** Security question functions *****************/

	/** Sets the security questions and answers for a user.
	 * @param	userId	login ID of the user
	 * @param	secQsAndAnswers	question numbers (in format m:n) and answers
	 */
	public static void setSecurityAnswers(String userId, 
			String[] secQsAndAnswers) {
		final String SELF = "UserWrite.setSecurityAnswers: ";
		final String qry = toString(
				UPDATE + USERS + SET, equalsJoinQMarks(
					USER_SEC_QS,
					USER_SEC_ANS1,
					USER_SEC_ANS2),
				WHERE + USER_ID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				secQsAndAnswers[0],
				secQsAndAnswers[1],
				secQsAndAnswers[2],
				userId);
		debugPrint(SELF, sql_vals);
		Connection con = null;
		try {
			con = getPoolConnection();
			tryUpdate(con, sql_vals);
		} catch (SQLException e) {
			alwaysPrint(SELF + "query failed.");
			e.printStackTrace();
		} finally {
			closeConnection(con);
		} // try
	} // setSecurityAnswers(String, String[])

	/** Disables external instantiation. */
	private UserWrite() { }

} // UserWrite
