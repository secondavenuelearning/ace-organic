package com.epoch.session;

import com.epoch.AppConfig;
import com.epoch.courseware.Institution;
import com.epoch.courseware.User;
import com.epoch.db.EnrollmentRW;
import com.epoch.db.InstitutionRW;
import com.epoch.db.UserRead;
import com.epoch.db.UserWrite;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.StudentEmailExistsException;
import com.epoch.exceptions.StudentNumConstraintException;
import com.epoch.exceptions.UniquenessException;

/** All functions performed by a user who is not logged in yet. */ 
public final class AnonSession {

	/** Get all institutions already in the database.
	 * @return	an array of names of institutions
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Institution getDefaultInstitution() throws DBException {
		return InstitutionRW.getInstitution(AppConfig.defaultInstitution);
	} // getDefaultInstitution()

	/** Get all institutions already in the database.
	 * @return	an array of names of institutions
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Institution[] getAllInstitutions() throws DBException {
		return InstitutionRW.getAllInstitutions();
	} // getAllInstitutions()

	/** Get all institutions of verified instructors.
	 * @return	an array of names of institutions
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static Institution[] getVerifiedInstitutions() throws DBException {
		return InstitutionRW.getVerifiedInstitutions();
	} // getVerifiedInstitutions()

	/** Get a user by userId.
	 * @param	userId	login ID of user
	 * @return	the user
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static User getUser(String userId) throws DBException {
		return UserRead.getUser(userId);
	} // getUser(String)

	/** Get all languages of verified instructors.
	 * @return	an array of languages
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[] getAllLanguages() throws DBException {
		return UserRead.getAllLanguages();
	} // getAllLanguages()

	/** Register a user in ACE.
	 * @param	user	a new user
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	UniquenessException	when more than one user has this user's
	 * login ID
	 * @throws	StudentNumConstraintException	when another student at the same
	 * school as this user already has that student ID number
	 * @throws	StudentEmailExistsException	when another student at the same
	 * school as this user already has that email
	 */
	public static void registerUser(User user) throws DBException,
			UniquenessException, StudentNumConstraintException, 
			StudentEmailExistsException {
		UserWrite.addUser(user);
		EnrollmentRW.enrollInQualified(user);
	} // registerUser(User)

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
		return UserRead.isRegdACEUser(userId, email, studentNum);
	} // isRegdACEUser(String, String, String)

	/** Enroll a registered student in the tutorials course.
	 * @param	userId	login ID of student
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void enrollInTutorialsCourse(String userId)
			throws DBException {
		EnrollmentRW.enrollInCourse(userId, AppConfig.tutorialId);
	} // enrollInCourse(String)

	/** Disables external instantiation. */
	private AnonSession() { }

} // AnonSession
