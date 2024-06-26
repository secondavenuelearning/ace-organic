package com.epoch.session;

import static com.epoch.db.dbConstants.CourseRWConstants.*;
import static com.epoch.db.dbConstants.UserRWConstants.*;
import com.epoch.courseware.User;
import com.epoch.db.CourseRW;
import com.epoch.db.EnrollmentRW;
import com.epoch.db.ForumRW;
import com.epoch.db.HWWrite;
import com.epoch.db.InstitutionRW;
import com.epoch.db.ResponseWrite;
import com.epoch.db.UserRead;
import com.epoch.db.UserWrite;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.StudentEmailExistsException;
import com.epoch.exceptions.StudentNumConstraintException;
import com.epoch.exceptions.UniquenessException;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Holds an administrator's session.
   AdminSession can access all the functions of InstructorSession,
   and StudentSession,
   but the call will be valid only after a call to setActedUser().
   InvalidOpException is thrown otherwise.
   AdminSession can indirectly use all basic functions of the user session.
*/
public class AdminSession extends InstructorSession {

	/* Use UserSession debugPrint(). */
	
	/** List of all instructors.  */
	transient private final List<User> instructors;
	/** List of instructors plus maybe some students whose name begins with a
	 * certain string.  */
	private final List<User> allUsers;
	/** Selector for finding particular students.  Only those students
	 * are selected where their last name begins with sel.  */
	transient private String selector;
	/** When administrator impersonates someone, the role of the person they
	 * are impersonating. */
	transient private char actedRole;
	/** When administrator impersonates someone, the login ID of the person they
	 * are impersonating. */
	transient private String actedUserId;

	/** Constructor.
	 * @param	user1	the user for whom to create this admin session
	 * @throws	DBException	if the database can't be read
	 */
	public AdminSession(User user1) throws DBException {
		super(user1);
		selector = null;
		instructors = UserRead.getAllNonstudents();
		allUsers = new ArrayList<User>();
		allUsers.addAll(instructors);
		actedRole = ADMINISTRATOR;
	} // AdminSession(User)

	// ----------------------------------
	// Functions for user administration
	// ---------------------------------

	/** Gets all students, instructors, and administrators in the current list.
	 * @return	array of students, instructors, and administrators in the
	 * current list
	 */
	public User[] getAllUsers() {
		return (allUsers.toArray(new User[allUsers.size()]));
   	} // getAllUsers()

	/** Gets all instructors and administrators in the current list as an array.
	 * @return	array of instructors and administrators in the
	 * current list
	 */
	public User[] getAllInstructors() {
		return (instructors.toArray(new User[instructors.size()]));
   	} // getAllInstructors()

	/** Resets the list of all users sorted by role, name. 
	 * @throws	DBException	if the database can't be read
	 */
	public void resetAllUsers() throws DBException {
		resetAllUsers(!INSTN_TOO);
	} // resetAllUsers()

	/** Resets the list of all users. 
	 * @param	instnToo	when true, sort by role, school, name; otherwise,
	 * sort by role, name
	 * @throws	DBException	if the database can't be read
	 */
	public void resetAllUsers(boolean instnToo) throws DBException {
		instructors.clear();
		instructors.addAll(UserRead.getAllNonstudents(instnToo));
		allUsers.clear();
		allUsers.addAll(instructors);
	} // resetAllUsers(boolean)

	/** Gets users with the given IDs.
	 * @param	userIds	array of login ID of users
	 * @return	array of users
	 */
	public User[] getUsers(String[] userIds) {
		try {
			return UserRead.getUsers(userIds);
		} catch (DBException e) {
			Utils.alwaysPrint("AdminSession.getUsers: caught "
					+ "DBException, returning empty array.");
			e.printStackTrace();
		}
		return new User[0];
   	} // getUsers(String[])

	/** Gets a user.
	 * @param	userNum	1-based position of the user in the list
	 * @return	User object representing the user
	 * @throws	ParameterException	when userNum is out of bounds
	 * @throws	DBException	if the database can't be read
	 */
	public User getUser(int userNum) throws ParameterException, DBException {
		if (userNum <= 0 || userNum > allUsers.size())
				throw new ParameterException(" invalid userNum to getUser ");
		// get the user id and load User
		final String userId = allUsers.get(userNum - 1).getUserId();
		return UserRead.getUser(userId);
	} // getUser(int)

	/** Adds a user to the database.
	 * @param	user	contains the new User information
	 * @throws	UniquenessException	when more than one user has this user's
	 * login ID
	 * @throws	StudentNumConstraintException	when another student at the same
	 * school as this user already has that student ID number
	 * @throws	StudentEmailExistsException	when another student has the
	 * same email address
	 * @throws	DBException	if the database can't be written to
	 */
	public void addUser(User user) throws DBException,
			UniquenessException, StudentNumConstraintException,
			StudentEmailExistsException {
		UserWrite.addUser(user);
		allUsers.add(user);
		// enroll the student in any courses he is preenrolled in
		if (user.getRole() == STUDENT) {
			EnrollmentRW.enrollInQualified(user);
		} // if user is student
	} // addUser(User)

	/** Sets a user's information.
	 * @param	userNum	1-based position of the user in the list
	 * @param	user	contains the new User information
	 * @throws	ParameterException	when userNum is out of bounds
	 * @throws	UniquenessException	when more than one user has this user's
	 * login ID
	 * @throws	StudentNumConstraintException	when another student at the same
	 * school as this user already has that student ID number
	 * @throws	StudentEmailExistsException	when another student at the same
	 * school as this user already has that email
	 * @throws	DBException	if the database can't be written to
	 */
	public void setUser(int userNum, User user)
			throws ParameterException, DBException, UniquenessException,
			StudentNumConstraintException, StudentEmailExistsException {
	   	if (userNum <= 0 || userNum > allUsers.size())
			   	throw new ParameterException(" invalid userNum to setUser ");
		// get the userid and set User
		// all the parameters from user are taken except userId
		final String userId = allUsers.get(userNum - 1).getUserId();
		user.setUserId(userId);
		debugPrint("AdminSession.setUser: "
				+ "userNum = ", userNum,
				", user userId = ", user.getUserId(),
				", user role = ", user.getRole(),
				", user name = ", user.getName());
		// Precaution: the admin must not change his own role
		if (Utils.among(userId, "admin", "administrator"))
			user.setRole(ADMINISTRATOR);
		UserWrite.setUser(user);
		allUsers.set(userNum - 1, user);
	} // setUser(int, User)

	/** Gets languages preferred by users in order of preference and
	 * stores them in the User objects.
	 */
	public void getAllInstructorsLanguages() {
		UserRead.getLanguages(getAllInstructors());
	} // getAllInstructorsLanguages()

	/** Deletes a user from the database.
	 * @param	userNum	1-based position of the user in the list
	 * @throws	ParameterException	when userNum is out of bounds
	 * @throws	UniquenessException	when more than one user has this user's
	 * login ID
	 * @throws	DBException	if the database can't be written to
	 */
	public void removeUser(int userNum) throws ParameterException,
			DBException, UniquenessException {
		if (userNum <= 0 || userNum > allUsers.size())
				throw new ParameterException(" invalid userNum to removeUser ");
		// get the userid and remove User
		final String userId = allUsers.get(userNum - 1).getUserId();
		// Precaution: the admin entry must not be deleted by any chance
		if (Utils.among(userId, "admin", "administrator")) return;
		UserWrite.removeUser(userId);
		allUsers.remove(userNum - 1);
	} // removeUser(int)

	/** Deletes users with the given IDs.
	 * @param	userIds	array of login ID of users
	 * @param	userType	students or instructors
	 * @return	IDs of the users who were deleted
	 */
	public List<String[]> removeUsers(String[] userIds, char userType) {
		List<String[]> removedUsers = new ArrayList<String[]>();
		try {
			removedUsers = UserWrite.removeUsers(userIds);
			if (userType == INSTRUCTOR) {
				instructors.clear();
				instructors.addAll(UserRead.getAllNonstudents());
			}
			allUsers.clear();
			allUsers.addAll(instructors);
		} catch (DBException e) {
			Utils.alwaysPrint("AdminSession.removeUsers: caught DBException.");
			e.printStackTrace();
		}
		return removedUsers;
   	} // removeUsers(String[], char)

	/** Remove from the database those students who haven't logged in recently. 
	 * @param	y_m_d	String array of years, months, days; may represent a
	 * specific date or a time interval
	 * @param	instnId	the ID of the institution, or all of them, whose 
	 * inactive students are being removed
	 * @return	list of two-string arrays containing name and email address of
	 * removed students
	 */
	public List<String[]> removeInactiveStudents(String[] y_m_d, int instnId) {
		final String SELF = "AdminSession.removeInactiveStudents: ";
		final List<String[]> inactiveStudents = 
				UserWrite.removeInactiveStudents(
					Utils.stringToIntArray(y_m_d), instnId);
		debugPrint(SELF + "got ", inactiveStudents.size(), 
				" inactive student(s) with old last login(s).");
		allUsers.clear();
		allUsers.addAll(instructors);
		return inactiveStudents;
   	} // removeInactiveStudents(String[], int)

	/** Removes old responses from the database.
	 * @param	y_m_d	String array of years, months, days; may represent a
	 * specific date or a time interval
	 * @param	instnId	the ID of the institution, or all of them, whose 
	 * students' responses will be removed
	 * @return	number of rows affected
	 * @throws	DBException	if the database can't be read
	 */
	public int removeOldResponses(String[] y_m_d, int instnId) 
			throws DBException {
		final String SELF = "AdminSession.removeOldResponses: ";
		int count = 0;
		try {
			count = ResponseWrite.removeOldResponses(
					Utils.stringToIntArray(y_m_d), instnId);
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + "caught DBException.");
			e.printStackTrace();
		}
		debugPrint(SELF, count, " record(s) deleted.");
		return count;
   	} // removeOldResponses(String[], int)

	/** Find students in the database whose surnames begin with the string,
	 * populate the list of students with the results, and prepend this list 
	 * to the list of all users.
	 * @param	selector1	the selector string
	 * @throws	DBException	if the database can't be read
	 */
	public void setSearchString(String selector1) throws DBException {
		final String SELF = "AdminSession.setSearchString: ";
		selector = selector1;
		allUsers.clear();
		// selected students listed first
		final List<User> students = 
				UserRead.getSelectedStudents(selector);
	   	allUsers.addAll(students);
		// then instructors
		allUsers.addAll(instructors);
		debugPrint(SELF, students.size(), " student(s), ", instructors.size(),
				" instructors or admins, ", allUsers.size(), " all users.");
	} // setSearchString(String)

	/** Get the string, the basis on which the students in the list were 
	 * selected.
	 * @return	the selector string
	 */
	public String getSearchString()	{ return selector; }

	/** Gets the number of post images.
	 * @return	the number of post images
	 * @throws	DBException	if the database can't be read
	 */
	public static int getNumPostImages() throws DBException {
		return ForumRW.getNumPostImages();
	} // getNumPostImages()

	/** Gets the number of post images that are a year or more old.
	 * @return	the number of post images that are a year or more old
	 * @throws	DBException	if the database can't be read
	 */
	public static int getNumYearOldPostImages() throws DBException {
		return ForumRW.getNumYearOldPostImages();
	} // getNumYearOldPostImages()

	/** Deletes images from forum posts that have not been edited in a year or
	 * more. 
	 * @return	the number of images removed
	 * @throws	DBException	if the database can't be read
	 */
	public int deleteYearOldImages() throws DBException {
		return ForumRW.deleteYearOldImages();
	} // deleteYearOldImages()

	/** Sets the number of days students at an institution can avoid paying to 
	 * use ACE.
	 * @param	instnId	unique ID of the institution
	 * @param	gracePeriod	the grace period in days
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setPaymentGracePeriod(int instnId, int gracePeriod) 
			throws DBException {
		InstitutionRW.setGracePeriod(instnId, gracePeriod);
	} // setPaymentGracePeriod(int, int)

	/** Allows the admin to correct the name of an institution entered by a
	 * user.
	 * @param	instnId	unique ID of the institution
	 * @param	newName	the new name of the institution
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setInstitutionName(int instnId, String newName)
			throws DBException {
		InstitutionRW.setName(instnId, newName);
	} // setInstitutionName(int, String)

	/** Adds to the database an assignments list intended to be used as a 
	 * template.
	 * @param	name	name of the assignment template
	 * @param	xml	XML defining the assignment template
	 * @throws	DBException	if the database can't be read
	 */
	public void manageAssgtsTemplate(String name, String xml) 
			throws DBException {
		HWWrite.addAssgtsTemplate(name, xml);
	} // manageAssgtsTemplate(String, String)

	/** Saves to the database a modified assignments list intended to be used 
	 * as a template.
	 * @param	id	ID number of the assignment template
	 * @param	name	name of the assignment template
	 * @param	xml	XML defining the assignment template
	 * @throws	DBException	if the database can't be read
	 */
	public void manageAssgtsTemplate(int id, String name, String xml) 
			throws DBException {
		HWWrite.setAssgtsTemplate(id, name, xml);
	} // manageAssgtsTemplate(int, String, String)

	/** Deletes from the database an assignments list intended to be used as a 
	 * template.
	 * @param	id	ID number of the assignment template
	 * @throws	DBException	if the database can't be read
	 */
	public void manageAssgtsTemplate(int id) throws DBException {
		HWWrite.deleteAssgtsTemplate(id);
	} // manageAssgtsTemplate(int)

	/** Sets the primary language of instruction of an institution.
	 * @param	instnId	the institution's ID
	 * @param	language	the institution's primary language of instruction;
	 * null if English
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void setInstitutionPrimaryLanguage(int instnId, String language) 
			throws DBException {
		InstitutionRW.setPrimaryLanguage(instnId, language);
	} // setInstitutionPrimaryLanguage(int, String)

/* ************** Functions on an impersonated user *****************/

	/** Sets the user to be impersonated.
	 * @param	userNum	1-based position of the user in the list
	 * @throws	ParameterException	when userNum is out of bounds
	 * @throws	DBException	if the database can't be read
	 */
	public void setActedUser(int userNum) 
			throws ParameterException, DBException {
		if (userNum <= 0 || userNum > allUsers.size())
			throw new ParameterException(" Invalid userNum to setActedUser ");
		final User user1 = allUsers.get(userNum - 1);
		setActedUser(user1.getUserId());
	} // setActedUser(int)

	/** Sets the user to be impersonated.
	 * @param	userId	the user's login ID
	 * @throws	DBException	if the database can't be read
	 */
	public void setActedUser(String userId) throws DBException {
		actedUserId = userId;
		actedUser = UserRead.getUser(userId);
		actedRole = actedUser.getRole();
		debugPrint("AdminSession.setActedUser: Impersonating ",
				actedUserId, " with role ", actedRole, ".");
		initialiseInstr(actedUser);
	} // setActedUser(String)

	/** Gets the user the administrator is impersonating.
	 * @return	the impersonated user
	 */
	public User getActedUser() 		{ return actedUser; }
	/** Gets the login ID of the user the administrator is impersonating.
	 * @return	the login ID of the impersonated user
	 */
	public String getActedUserId() 	{ return actedUserId; }
	/** Gets the role of the user the administrator is impersonating.
	 * @return	the role of the impersonated user
	 */
	public char getActedRole() 		{ return actedRole; }
	
	/** Returns the acted role of the administrator back to administrator.
	 * Used after impersonation is over.
	 */
	public void resetActedRole() 	{ 
		actedRole = ADMINISTRATOR; 
		actedUser = me;
	} // resetActedRole()
	
} // AdminSession
