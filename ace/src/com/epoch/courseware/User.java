package com.epoch.courseware;

import com.epoch.AppConfig;
import com.epoch.courseware.courseConstants.UserConstants;
import com.epoch.db.ForumRW;
import com.epoch.db.UserRead;
import com.epoch.db.UserWrite;
import com.epoch.exceptions.DBException;
import com.epoch.translations.PhraseTransln;
import com.epoch.utils.DateUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A single ACE user. */
public class User implements UserConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** User's login ID. */
	private String userId = "";
	/** Hashed password of this user.  */
	transient private byte[] passwordHash = new byte[0];
	/** Role of user: student, instructor, administrator. */
	private char role = STUDENT;
	/** Flags setting permissions for this user.  */
	private int flags = ENABLED;
	/** Name of this user. */
	private Name name = new Name();
	/** User's institution (school). */
	private Institution institution = new Institution();
	/** User's ID number at his or her institution. */
	private String studentNum = "";
	/** User's email address. */
	private String email = "";
	/** User's email address for receiving text messages by phone. */
	private String textMessageEmail = "";
	/** User's phone number. */
	private String phone = "";
	/** User's physical address. */
	transient private String contactAddress = "";
	/** User's registration date. */
	transient private Date registrationDate = new Date(System.currentTimeMillis());
	/** User's last login date. */
	transient private Date lastLoginDate = new Date(System.currentTimeMillis());
	/** User's payment transaction number. */
	private String paymentTrackingId = "";
	/** Non-English languages of this user in order of preference. */
	transient private String[] myLanguages = (AppConfig.notEnglish
			? new String[] {AppConfig.defaultLanguage} : null);
	/** Phrases encountered by this user that have been translated. */
	transient private final Map<String, String> myTranslations = 
			new HashMap<String, String>();
	/** A flag to say that DB layer must not overwrite the
   	 * current password with the value in this object;
	 * instead leave it as it is.  */
	transient private boolean savePassword = false;

	/** Delimiter for variable phrases (to be substituted with other
	 * phrases) inside of translations. */
	private static final String STARS = PhraseTransln.STARS_REGEX;

	/** Constructor. */
	public User() { 
		// empty constructor
	}

	/** Constructor.
	 * @param	userId1	login ID of the user
	 */
	public User(String userId1) {
		userId = userId1;
		refreshLanguages();
	} // User(String)

	/** Constructor.
	 * @param	userId1	login ID of the user
	 * @param	passwordHash1	hashed password of the user
	 */
	public User(String userId1, byte[] passwordHash1) {
		userId = userId1;
		passwordHash = passwordHash1;
		refreshLanguages();
	} // User(String, byte[])

	/** Constructor.
	 * @param	userId1	login ID of the user
	 * @param	passwordHash1	hashed password of the user
	 * @param	retrieveLanguages	whether to get the user's languages
	 */
	public User(String userId1, byte[] passwordHash1, 
			boolean retrieveLanguages) {
		userId = userId1;
		passwordHash = passwordHash1;
		if (retrieveLanguages) refreshLanguages();
	} // User(String, byte[], boolean)

	/** Copies most of another User's values into this User.
	 * @param	copy	a User
	 */
	public void copyEditables(User copy) {
		passwordHash = copy.passwordHash;
		role = copy.role;
		flags = copy.flags;
		name = copy.name;
		studentNum = copy.studentNum;
		email = copy.email;
		institution = new Institution(copy.institution);
		phone = copy.phone;
		textMessageEmail = copy.textMessageEmail;
		contactAddress = copy.contactAddress;
		registrationDate = copy.registrationDate;
		lastLoginDate = copy.lastLoginDate;
		savePassword = copy.savePassword;
		paymentTrackingId = copy.paymentTrackingId;
		myLanguages = copy.myLanguages;
	} // copyEditables(User)

	/** Gets the user's login ID.
	 * @return	the user's login ID
	 */
	public String getUserId()						{ return userId; }
	/** Sets the user's login ID.
	 * @param	id	the user's login ID
	 */
	public void setUserId(String id)				{ userId = id; }
	/** Gets the user's role (admin, instructor, TA, student).
	 * @return	the user's role
	 */
	public char getRole()							{ return role; }
	/** Sets the user's role (admin, instructor, TA, student).
	 * @param	rl	the user's role
	 */
	public void setRole(char rl)					{ role = rl; }
	/** Gets the user's flags.  Used only by db/UserRead.java.
	 * @return	the user's flags
	 */
	public int getFlags()							{ return flags; }
	/** Sets the user's flags.
	 * @param	fl	the user's flags
	 */
	public void setFlags(int fl)					{ flags = fl; }
	/** Gets the user's name.
	 * @return	the user's name
	 */
	public Name getName()							{ return name; }
	/** Sets the user's name.
	 * @param	nm	the user's name
	 */
	public void setName(Name nm)					{ name = nm; }
	/** Gets the user's student ID number.
	 * @return	the user's student ID number
	 */
	public String getStudentNum()					{ return studentNum; }
	/** Sets the user's student ID number.
	 * @param	num	the user's student ID number
	 */
	public void setStudentNum(String num)			{ studentNum = num; }
	/** Gets the user's email address.
	 * @return	the user's email address
	 */
	public String getEmail()						{ return email; }
	/** Sets the user's email address.
	 * @param	eml	the user's email address
	 */
	public void setEmail(String eml)				{ email = eml; }
	/** Gets the user's institution.
	 * @return	the user's institution
	 */
	public Institution getInstitution()				{ return institution; }
	/** Gets the ID number of the user's institution.
	 * @return	the ID number of the user's institution
	 */
	public int getInstitutionId()					{ return institution.getId(); }
	/** Gets the name of the user's institution.
	 * @return	the name of the user's institution
	 */
	public String getInstitutionName()				{ return institution.getName(); }
	/** Gets the name of the student ID number at the user's institution.
	 * @return	the name of the student ID number at the user's institution
	 */
	public String getInstitutionStudentNumLabel()	{ return institution.getStudentNumLabel(); }
	/** Sets the user's institution.
	 * @param	inst	the user's institution
	 */
	public void setInstitution(Institution inst)	{ institution = inst; }
	/** Gets the user's phone number.
	 * @return	the user's phone number
	 */
	public String getPhone()						{ return phone; }
	/** Sets the user's phone number.
	 * @param	ph	the user's phone number
	 */
	public void setPhone(String ph)					{ phone = ph; }
	/** Gets the user's email address for receiving text messages by phone.
	 * @return	the user's email address for receiving text messages by phone
	 */
	public String getTextMessageEmail()				{ return textMessageEmail; }
	/** Sets the user's email address for receiving text messages by phone.
	 * @param	tme	the user's email address for receiving text messages by phone
	 */
	public void setTextMessageEmail(String tme)		{ textMessageEmail = tme; }
	/** Gets the user's contact address.
	 * @return	the user's contact address
	 */
	public String getAddress()						{ return contactAddress; }
	/** Sets the user's contact address.
	 * @param	addr	the user's contact address
	 */
	public void setAddress(String addr)				{ contactAddress = addr; }
	/** Gets the user's registration date.
	 * @return	the user's registration date
	 */
	public Date getRegDate()						{ return registrationDate; }
	/** Sets the user's registration date.
	 * @param	rd	the user's registration date
	 */
	public void setRegDate(Date rd)					{ registrationDate = rd; }
	/**  Gets the user's last login date.
	 * @return	the user's last login date
	 */
	public Date getLastLoginDate()					{ return lastLoginDate; }
	/** Sets the user's last login date.
	 * @param	lld	the user's last login date
	 */
	public void setLastLoginDate(Date lld)			{ lastLoginDate = lld; }
	/** Gets whether the user has paid.
	 * @return	true if the user has paid
	 */
	public boolean hasPaid()						{ return !Utils.isEmpty(paymentTrackingId); }
	/** Gets the user's payment transaction number.
	 * @return	the user's payment transaction number
	 */
	public String getPaymentTrackingId()			{ return paymentTrackingId; }
	/** Sets the user's payment transaction number.
	 * @param	pt	the user's payment transaction number
	 */
	public void setPaymentTrackingId(String pt)		{ paymentTrackingId = pt; }
	/** Gets the user's languages.
	 * @return	the user's languages
	 */
	public String[] getLanguages()					{ return myLanguages; }
	/** Sets the user's languages.
	 * @param	langs	the user's languages
	 */
	public void setLanguages(String[] langs)		{ myLanguages = langs; }
	/** Gets whether the password is stored in the ACE database.
	 * @return	true if the password is stored in the ACE database
	 */
	public boolean getPasswordStoredInACE()			{ return !Utils.isEmpty(passwordHash); }
	/** Get the hashed password.
	 * @return	the hashed password
	 */
	public byte[] getPasswordHash()					{ return passwordHash; }
	/** Set the flag to save a new password to the database. */
	public void setSavePassword()					{ savePassword = true; }
	/** Gets whether a new password for this user should be written to the
	 * database.
	 * @return	true if a new password should be written
	 */
	public boolean changePassword()					{ return !savePassword; }
	/** Gets if this user is enabled. 
	 * @return	true if this user is enabled
	 */
	public boolean isEnabled()						{ return isFlagOn(ENABLED); }
	/** Gets if this user is a master author. 
	 * @return	true if this user is a master author
	 */
	public boolean isMasterAuthor()					{ return isFlagOn(IS_MASTER_AUTHOR); }
	/** Gets if this user is a translator. 
	 * @return	true if this user is a translator
	 */
	public boolean isTranslator()					{ return isFlagOn(IS_TRANSLATOR); }
	/** Gets if this user may change his or her password.   Will be false only
	 * for exam login IDs.
	 * @return	true if this user is may change his or her password
	 */
	public boolean mayChangePwd()					{ return !isFlagOn(MAYNT_CHANGE_PWD); }
	/** Gets if ACE should show calculated synthesis products to this user in 
	 * the feedback to a synthesis question.  Set to true only when a student 
	 * is taking an exam.
	 * @return	true if this user may see calculated synthesis products
	 */
	public boolean showCalcdSynthProds()	{ return !isFlagOn(DONT_SHOW_CALCD_SYNTH_PRODS); }
	/** Gets if this user prefers MarvinSketch Java applet over MarvinJS. 
	 * @return	true if this user prefers MarvinSketch Java applet over MarvinJS
	 */
	public boolean prefersJava()					{ return isFlagOn(PREFERS_JAVA); }
	/** Gets if this user prefers PNG graphics over SVG. 
	 * @return	true if this user prefers PNG graphics over SVG
	 */
	public boolean prefersPNG()						{ return isFlagOn(PREFERS_PNG); }
	/** Gets if this user prefers family name first. 
	 * @return	true if this user prefers family name first
	 */
	public boolean prefersFamilyName1st()			{ return isFlagOn(FAMILY_NAME_1ST); }
	/** Gets if this user prefers the day to come first in dates. 
	 * @return	true if this user prefers the day to come first in dates
	 */
	public boolean prefersDay1st()					{ return isFlagOn(DAY_MON_YR); }
	/** Gets if the flag is on.
	 * @param	flagVal	the value of the flag
	 * @return	true if the flag is on
	 */
	private boolean isFlagOn(int flagVal)			{ return (flags & flagVal) != 0; }
	/** Sets that this user may not change his or her password.  Applied only to
	 * exam login IDs.  */
	public void disallowChangePwd()					{ flags |= MAYNT_CHANGE_PWD; }

	/** Gets the registration date in the default time zone as a string.
	 * @return	date this user registered (MMM d, yyyy, h:mm aa)
	 */
	public String getRegDateStr() { 
		return (registrationDate == null ? null 
				: DateUtils.getStringNoTimeZone(registrationDate)); 
	} // getRegDateStr()

	/** Gets the last login date in the default time zone as a string.
	 * @return	date this user last logged in (MMM d, yyyy, h:mm aa)
	 */
	public String getLastLoginDateStr() { 
		return (lastLoginDate == null ? null 
				: DateUtils.getStringNoTimeZone(lastLoginDate)); 
	} // getLastLoginDateStr()

	/** Sets whether this user is enabled.  Called by profile/saveProfile.jsp
	 * and nosession/saveNewUser.jsp.  No need to save here, because all this 
	 * user's data will be saved momentarily.
	 * @param	enabled	whether this user should be enabled
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setEnabled(boolean enabled) throws DBException { 
		setFlag(ENABLED, enabled); 
	} // setEnabled(boolean)

	/** Sets whether this user prefers MarvinSketch Java applet over MarvinJS.  
	 * Called by profile/saveProfile.jsp and nosession/saveNewUser.jsp. 
	 * No need to save here, because all this user's data will be saved momentarily.
	 * @param	prefersJava	whether this user prefers MarvinSketch Java applet
	 * over MarvinJS
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setPrefersJava(boolean prefersJava) throws DBException { 
		setFlag(PREFERS_JAVA, prefersJava); 
	} // setPrefersJava(boolean)

	/** Sets whether this user prefers PNG graphics over SVG.
	 * Called by profile/saveProfile.jsp and nosession/saveNewUser.jsp. 
	 * No need to save here, because all this user's data will be saved momentarily.
	 * @param	prefersPNG	whether this user prefers PNG graphics over SVG
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setPrefersPNG(boolean prefersPNG) throws DBException { 
		setFlag(PREFERS_PNG, prefersPNG); 
	} // setPrefersPNG(boolean)

	/** Sets whether this user prefers family name first.
	 * Called by profile/saveProfile.jsp and nosession/saveNewUser.jsp. 
	 * No need to save here, because all this user's data will be saved momentarily.
	 * @param	familyName1st	whether this user prefers family name first
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setFamilyName1st(boolean familyName1st) throws DBException { 
		setFlag(FAMILY_NAME_1ST, familyName1st); 
	} // setFamilyName1st(boolean)

	/** Sets whether this user prefers the day to come first in dates.
	 * Called by profile/saveProfile.jsp and nosession/saveNewUser.jsp. 
	 * No need to save here, because all this user's data will be saved momentarily.
	 * @param	day1st	whether this user prefers the day first in dates
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setDay1st(boolean day1st) throws DBException { 
		setFlag(DAY_MON_YR, day1st); 
	} // setDay1st(boolean)

	/** Sets whether this user is a master author or translator, stores the 
	 * result in the database. 
	 * @param	master	whether this user should be a master author
	 * @param	translator	whether this user should be a translator
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setInstructorPermissions(boolean master, boolean translator) 
			throws DBException { 
		setFlag(IS_MASTER_AUTHOR, master); 
		setFlag(IS_TRANSLATOR, translator); 
		UserWrite.setUserFlags(this);
	} // setInstructorPermissions(boolean, boolean)

	/** Sets if ACE should show calculated synthesis products to this user in 
	 * the feedback to a synthesis question.  Set to true only when a student 
	 * is taking an exam.
	 * @param	dontShow whether this user may see calculated synthesis products
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setDontShowCalcdSynthProds(boolean dontShow) 
			throws DBException { 
		setFlag(DONT_SHOW_CALCD_SYNTH_PRODS, dontShow); 
		UserWrite.setUserFlags(this);
	} // setDontShowCalcdSynthProds(boolean)

	/** Turns a flag on or off.
	 * @param	flagVal	value of the flag
	 * @param	on	whether the flag should be turned on
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private void setFlag(int flagVal, boolean on) throws DBException { 
		flags = (on ? (flags | flagVal) : (flags & ~flagVal));
	} // setFlag(int, boolean)

	/** Gets the number of days since a date.
	 * @param	date	the date
	 * @return	number of days since a date
	 */
	private double daysSince(Date date) {
		final String SELF = "User.daysSince: ";
		double daysSince = 0.0;
		if (date != null) {
			final long now_ms = (new Date()).getTime();
			final long regd_ms = date.getTime();
			daysSince = DateUtils.secsToDays((now_ms - regd_ms) / 1000);
			debugPrint(SELF, daysSince, " days since the date.");
		} else {
			debugPrint(SELF + "no date & time to compare now to.");
		} // if there is a date in the database
		return daysSince;
	} // daysSince()

	/** Gets the number of days since a student has registered.
	 * @return	number of days since a student has registered
	 */
	private double daysSinceRegistered() {
		return daysSince(registrationDate);
	} // daysSinceRegistered()

	/** Gets the number of days since a student last logged in.
	 * @return	number of days since a student last logged in
	 */
	public double daysSinceLastLogin() {
		return daysSince(lastLoginDate);
	} // daysSinceLastLogin()

	/** Gets whether a student's grace period to use ACE without paying has 
	 * expired.
	 * @return	true if a student's grace period has expired
	 */
	public boolean isPastGracePeriod() {
		return role == STUDENT
				&& !institution.isExempt() 
				&& daysSinceRegistered() > institution.getGraceDays();
	} // isPastGracePeriod()

	/** Gets if this user is blocked from a course's forum.
	 * @param	courseId	ID number of the course
	 * @return	true if this user is blocked from the course's forum
	 * @throws	DBException	if there's a problem reading the database
	 */
	public boolean isBlockedFromForum(int courseId) throws DBException {
		return ForumRW.isBlocked(courseId, userId);
	} // isBlockedFromForum(int)

	/** Determines whether this user is a temporary exam-student.
	 * @return	true if this user is a temporary exam-student
	 */
	public boolean isExamStudent() {
		return isExamStudent(userId);
	} // isExamStudent()

	/** Determines whether a user is a temporary exam-student.
	 * @param	userId	a user's login ID
	 * @return	true if a user is a temporary exam-student
	 */
	public static boolean isExamStudent(String userId) {
		boolean isExamStudent = false;
		try {
			isExamStudent = UserRead.isExamStudent(userId);
		} catch (DBException e) {
			Utils.alwaysPrint("User.isExamStudent: caught DBException "
					+ "while looking up user; returning ", isExamStudent);
		}
		return isExamStudent;
	} // isExamStudent(String)

	/** Determines whether a login belongs to an exam-student and has expired.
	 * @return	true if the login belongs to an exam-student and it has expired
	 */
	public boolean isExpiredExamStudent() {
		final String SELF = "User.isExpiredExamStudent: ";
		boolean isExpired = false;
		Date created;
		try {
			created = UserRead.getDateCreated(userId);
		} catch (DBException e) {
			Utils.alwaysPrint("User.isExpiredExamStudent: caught DBException "
				+ "while looking up user");
			created = null;
		}
		if (created != null) {
			final Calendar expiry = Calendar.getInstance();
			expiry.setTime(created);
			expiry.add(Calendar.WEEK_OF_MONTH,
					AppConfig.EXAM_STUDENT_LIFE_WKS);
			final Calendar now = Calendar.getInstance();
			now.setTime(new Date());
			isExpired = now.after(expiry);
			debugPrint(SELF + "expiration date & time = ",
					DateUtils.getString(expiry.getTime()),
					", current time = ",
					DateUtils.getString(now.getTime()),
					"; returning ", isExpired);
		} else {
			debugPrint(SELF + "no expiration date & time; returning ", 
					isExpired);
		} // if there is a creation date in the database
		return isExpired;
	} // isExpiredExamStudent(String)

	/** Determines whether the profile of an exam-student has been changed from
	 * the default value.
	 * @return	true if the profile has been changed from the default value
	 */
	public boolean hasChangedProfile() {
		return !Utils.among(userId, studentNum, name.givenName)
				&& !RANDOM_SURNAME.equals(name.familyName);
	} // hasChangedProfile()

	/** Sets this user's last login date to now. */
	public void setLoginDateToNow() {
		UserWrite.setLoginDateToNow(userId);
	} // setLoginDateToNow()

	/** Sets this user's security questions and answers.
	 * @param	secQsAndAnswers	question numbers (in format m:n) and answers
	 */
	public void setSecurityAnswers(String[] secQsAndAnswers) {
		UserWrite.setSecurityAnswers(userId, secQsAndAnswers);
	} // setSecurityAnswers(String[])

	/** Gets this user's security questions and answers.
	 * @return	question numbers (in format m:n) and answers
	 */
	public String[] getSecurityAnswers() {
		return getSecurityAnswers(BY_USER_ID);
	} // getSecurityAnswers()

	/** Gets this user's security questions and answers.
	 * @param	byUserId	if true, get the security answers by userId;
	 * otherwise, by institution and student number
	 * @return	question numbers (in format m:n) and answers
	 */
	public String[] getSecurityAnswers(boolean byUserId) {
		return (byUserId ? UserRead.getSecurityAnswers(userId)
				: UserRead.getSecurityAnswers(institution.getId(), studentNum));
	} // getSecurityAnswers()

	/** Determines whether the user's responses to the security questions match
	 * the stored answers.
	 * @param	secResponses	array of user's two responses to the security 
	 * questions
	 * @return	true if the current answers to the security questions match the
	 * stored answers
	 */
	public boolean matchSecurityAnswers(String[] secResponses) {
		return matchSecurityAnswers(secResponses, BY_USER_ID);
	} // matchSecurityAnswers(String[])

	/** Determines whether the user's responses to the security questions match
	 * the stored answers.
	 * @param	secResponses	array of user's two responses to the security 
	 * questions
	 * @param	byUserId	if true, get the security answers by userId;
	 * otherwise, by institution and student number
	 * @return	true if the current answers to the security questions match the
	 * stored answers
	 */
	public boolean matchSecurityAnswers(String[] secResponses, boolean byUserId) {
		final String SELF = "User.matchSecurityAnswers: ";
		debugPrint(SELF + "secResponses = ", Arrays.toString(secResponses));
		final String[] secQsAndAnswers = getSecurityAnswers(byUserId);
		debugPrint(SELF + "secQsAndAnswers = ", Arrays.toString(secQsAndAnswers));
		return !Utils.membersAreEmpty(secQsAndAnswers)
				&& secQsAndAnswers[1].equalsIgnoreCase(secResponses[0])
				&& secQsAndAnswers[2].equalsIgnoreCase(secResponses[1]);
	} // matchSecurityAnswers(String[], boolean)

	/** If a userId contains non-ASCII characters, saves the userId with CERs
	 * instead.
	 * @param	unicodeUserId	replaces the current userId after conversion to
	 * CERs
	 * @return	true if a new username was stored
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public boolean fixNonAsciiUserId(String unicodeUserId) throws DBException {
		final String SELF = "User.fixNonAsciiUserId: ";
		boolean success = false;
		final String storedUserId = getUserId();
		final String cersUserId = Utils.unicodeToCERs(unicodeUserId);
		if (cersUserId.equals(storedUserId)) {
			System.out.println(SELF + "userId " + unicodeUserId
					+ " already stored with only ASCII characters.");
		} else try {
			final int instnId = institution.getId();
			UserWrite.changeUserId(cersUserId, instnId, studentNum);
			success = true;
			System.out.println(SELF + "super-ASCII-containing userId " 
					+ unicodeUserId + " previously stored as " + storedUserId 
					+ " changed to cersUserId " + cersUserId);
		} catch (DBException e) {
			System.out.println(SELF + "userId " + unicodeUserId
					+ " could not be stored.");
		} // if stored userId is same as new userId converted to CERs-only
		return success;
	} // fixNonAsciiUserId()

/* ************** Language and translation methods *******************/

	/** Gets from the database the non-English languages selected by this user in 
	 * the preference order set by the user.  Called here and from saveLanguages.jsp.
	 */
	public final void refreshLanguages() {
		final String SELF = "User.refreshLanguages: ";
		String[] newLanguages;
		newLanguages = UserRead.getLanguages(userId);
		if (!Arrays.deepEquals(newLanguages, myLanguages)) {
			debugPrint(SELF + "changing languages of ", userId, " from ", 
					myLanguages, " to ", newLanguages);
			myLanguages = newLanguages;
			myTranslations.clear();
		} else {
			debugPrint(SELF + "languages of ", userId, " already set to ", 
					myLanguages);
		} // if languages have changed
	} // refreshLanguages()

	/** Sets a temporary non-English language for this user without saving 
	 * it to the database.  Called by nologin/entries.jsp.
	 * @param	language	non-English language
	 */
	public void setLanguage(String language) {
		myLanguages = (language == null || ENGLISH.equals(language) ? null 
				: new String[] {language});
	} // setLanguage(String)

	/** Adds a non-English language for this user.
	 * @param	language	non-English language
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void addLanguage(String language) throws DBException {
		UserWrite.addLanguage(userId, language);
		refreshLanguages();
	} // addLanguage(String)

	/** Sets the order in which this user's chosen languages should be used.
	 * @param	newOrder	new position of each language in current order of
	 * languages
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setLanguageOrder(int[] newOrder) throws DBException {
		UserWrite.setLanguageOrder(userId, newOrder);
		refreshLanguages();
	} // setLanguageOrder(int[])

	/** Removes one of this user's languages.
	 * @param	removePosn	1-based position of the language to be removed, in the
	 * current order of the languages
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void removeLanguage(int removePosn) throws DBException {
		UserWrite.removeLanguage(userId, removePosn);
		refreshLanguages();
	} // removeLanguage(int)

	/** Gets Javascript-compatible translation of a phrase, or returns 
	 * the original in Javascript-compatible form
	 * if a translation in one of the user's selected languages is not found.  
	 * <p>This method also converts Unicode to character entity references.
	 * @param	phrase	a phrase to translate
	 * @return	Javascript-compatible translated phrase or the original
	 */
	public String translateJS(String phrase) {
		return Utils.toValidJS(translate(phrase));
	} // translateJS(String)

	/** Gets translation of a phrase, or returns the original
	 * if a translation in one of the user's selected languages is not found.  
	 * <p>The translation (or original phrase) is stored in a Map for lookup
	 * later.  If no translations at all are found, ACE stores the phrase in the 
	 * database.
	 * @param	phrase	a phrase to translate
	 * @return	translated phrase or the original
	 */
	public String translate(String phrase) {
		return translate(phrase, PhraseTransln.ADD_NEW_PHRASES_TO_DB);
	} // translate(String)

	/** Gets translation of a phrase, or returns the original
	 * if a translation in one of the user's selected languages is not found.  
	 * <p>The translation (or original phrase) is stored in a Map for lookup
	 * later.  
	 * @param	phrase	a phrase to translate
	 * @param	addNewPhraseToDB	whether to add a new English phrase to the
	 * database
	 * @return	translated phrase or the original
	 */
	public String translate(String phrase, boolean addNewPhraseToDB) {
		if (Utils.isEmpty(myLanguages)) return phrase;
	 	String transln = myTranslations.get(phrase);
		if (transln == null) {
			transln = PhraseTransln.translate(phrase, myLanguages,
					addNewPhraseToDB);
			debugPrint("User.translate(String): called db for translation of: ", 
					phrase, "\ngot: ", transln);
			myTranslations.put(phrase, transln);
		} // if translation has not been acquired yet
		return transln;
	} // translate(String, boolean)

	/** Gets Javascript-compatible translations of phrases, or leaves them 
	 * unmodified if a translation in one of the user's selected languages is not 
	 * found.  Modifies the original array!
	 * <p>This method also converts Unicode to character entity references.
	 * @param	phrases	phrases to translate
	 */
	public void translateJS(String[] phrases) {
		translate(phrases);
		for (int phrNum = 0; phrNum < phrases.length; phrNum++) {
			phrases[phrNum] = Utils.toValidJS(phrases[phrNum]);
		} // for each phrase
	} // translateJS(String[])

	/** Translates phrases, or leaves unmodified if a translation in one of the
	 * user's selected languages is not found.  Modifies the original array!
	 * <p>Each translation (or original phrase) is stored in a Map for lookup
	 * later.  If no translations at all are found for a phrase, ACE stores the 
	 * phrase in the database.
	 * @param	phrases	phrases to translate
	 */
	public void translate(String[] phrases) {
		final String SELF = "User.translate(String[]): ";
		if (Utils.isEmpty(myLanguages)) return;
		final String[] translations = new String[phrases.length];
		final List<String> toTranslate = new ArrayList<String>();
		// sort phrases by which have already been translated
		for (int phrNum = 0; phrNum < phrases.length; phrNum++) {
	 		final String transln = myTranslations.get(phrases[phrNum]);
			if (transln == null) toTranslate.add(phrases[phrNum]);
			else translations[phrNum] = transln;
		} // for each phrase
		// translate phrases not already translated
		final String[] newlyTranslated =
				toTranslate.toArray(new String[toTranslate.size()]);
		debugPrint(SELF + "called db for translation of: ", toTranslate);
		PhraseTransln.translate(newlyTranslated, myLanguages);
		debugPrint(SELF + "got translation: ", newlyTranslated);
		// put new translations in map, copy all into original array
		int translnNum = 0;
		for (int phrNum = 0; phrNum < phrases.length; phrNum++) {
			if (translations[phrNum] == null) {
				final String transln = newlyTranslated[translnNum++];
				myTranslations.put(phrases[phrNum], transln);
				translations[phrNum] = transln;
			} // if phrase has not already been translated
			phrases[phrNum] = translations[phrNum];
		} // for each phrase
	} // translate(String[])

	/** Translates an array of phrases, and returns a map with each 
	 * original phrase mapped to its translation.
	 * <p>Each translation (or original phrase) is stored in a Map for lookup
	 * later.  If no translations at all are found for a phrase, ACE stores the 
	 * phrase in the database.
	 * @param	phrasesOrig	phrases to translate
	 * @return	a map of each original phrase keyed to itself
	 */
	public Map<String, String> translateToMap(String[] phrasesOrig) {
		final String SELF = "User.translateToMap: ";
		final String[] phrasesTransld = Utils.getCopy(phrasesOrig);
		translate(phrasesTransld);
		final Map<String, String> phrasesMap = new HashMap<String, String>();
		for (int phrNum = 0; phrNum < phrasesTransld.length; phrNum++) {
			phrasesMap.put(phrasesOrig[phrNum], phrasesTransld[phrNum]);
		} // for each translated phrase
		return phrasesMap;
	} // translateToMap(String[])

	/** Gets translation of selected phrase, or returns the original (and stores
	 * it in the database) if the translation is not found, and substitutes the
	 * given number into the phrase.  Both English and translation will have the
	 * form, "blah blah blah ***word*** blah".
	 * @param	phrase	a phrase to translate
	 * @param	num	a number to substitute into the phrase
	 * @return	translated phrase or the original
	 */
	public String translate(String phrase, int num) {
	 	return translate(phrase, String.valueOf(num));
	} // translate(String, int)

	/** Gets Javascript-compatible translation of selected phrase or
	 * the original.  Both English and translation will have the
	 * form, "blah blah blah ***word*** blah".
	 * <p>This method also converts Unicode to character entity references.
	 * @param	phrase	a phrase to translate
	 * @param	num	a number to substitute into the phrase
	 * @return	Javascript-compatible translated phrase or the original
	 */
	public String translateJS(String phrase, int num) {
		return Utils.toValidJS(translate(phrase, num));
	} // translateJS(String, int)

	/** Gets translation of a phrase, or returns the original
	 * if a translation in one of the user's selected languages is not found,
	 * and substitutes the given number into the phrase.  Both English and
	 * translation will have the form, "blah blah blah ***word*** blah".  
	 * @param	phrase	a phrase to translate
	 * @param	num	a number to substitute into the phrase
	 * @return	translated phrase or the original
	 */
	public String translate(String phrase, double num) {
	 	return translate(phrase, String.valueOf(num));
	} // translate(String, double)

	/** Gets Javascript-compatible translation of selected phrase or
	 * the original.  Both English and translation will have the
	 * form, "blah blah blah ***word*** blah".
	 * <p>This method also converts Unicode to character entity references.
	 * @param	phrase	a phrase to translate
	 * @param	num	a number to substitute into the phrase
	 * @return	Javascript-compatible translated phrase or the original
	 */
	public String translateJS(String phrase, double num) {
		return Utils.toValidJS(translate(phrase, num));
	} // translateJS(String, double)

	/** Gets Javascript-compatible translation of selected phrase or
	 * the original.  Both English and translation will have the
	 * form, "blah blah blah ***word*** blah".
	 * <p>This method also converts Unicode to character entity references.
	 * @param	phrase	a phrase to translate
	 * @param	substn	a word or two to substitute into the translated phrase
	 * @return	Javascript-compatible translated phrase or the original
	 */
	public String translateJS(String phrase, String substn) {
		return Utils.toValidJS(translate(phrase, substn));
	} // translateJS(String, String)

	/** Gets translation of a phrase, or returns the original
	 * if a translation in one of the user's selected languages is not found,
	 * and substitutes the second string into the phrase.  Both English and
	 * translation will have the form, "blah blah blah ***word*** blah".  
	 * <p>The translation (or original phrase) is stored in a Map for lookup
	 * later.  If no translations at all are found, ACE stores the phrase in the 
	 * database.
	 * @param	phrase	a phrase to translate
	 * @param	substn	a word or two to substitute into the translated phrase
	 * @return	translated phrase or the original
	 */
	public String translate(String phrase, String substn) {
	 	String transln = translate(phrase);
		transln = " " + transln + " ";
		final String[] parts = transln.split(STARS);
		final String output = parts[0] + substn + parts[2];
		return output.trim();
	} // translate(String, String)

	/** Gets Javascript-compatible translation of selected phrase or
	 * the original.  Both English and translation will have the
	 * form, "blah blah blah ***word1*** blah blah ***word2*** blah".  The order
	 * of ***word1*** and ***word2*** may be different in the translation.
	 * <p>This method also converts Unicode to character entity references.
	 * @param	phrase	a phrase to translate
	 * @param	nums	 numbers to substitute into the phrase
	 * @return	Javascript-compatible translated phrase or the original
	 */
	public String translateJS(String phrase, int[] nums) {
		return Utils.toValidJS(translate(phrase, nums));
	} // translateJS(String, int[])

	/** Gets translation of a phrase, or returns the original
	 * if a translation in one of the user's selected languages is not found,
	 * and substitutes the given numbers into the phrase.  
	 * Both English and translation will have the
	 * form, "blah blah blah ***word1*** blah blah ***word2*** blah".  The order
	 * of ***word1*** and ***word2*** may be different in the translation.
	 * <p>The translation (or original phrase) is stored in a Map for lookup
	 * later.  If no translations at all are found, ACE stores the phrase in the 
	 * database.
	 * @param	phrase	a phrase to translate
	 * @param	nums	 numbers to substitute into the phrase
	 * @return	translated phrase or the original
	 */
	public String translate(String phrase, int[] nums) {
	 	final String transln = translate(phrase);
		final String[] translnParts = pad(transln).split(STARS);
		final String[] englParts = pad(phrase).split(STARS);
		// words enclosed in *** may not be in same order in translation as in
		// English; make list of such words to correlate to English
		final List<String> translnPartsList = new ArrayList<String>();
		if (!phrase.equals(transln)) {
			for (int partNum = 1; partNum < translnParts.length; partNum += 2) {
				translnPartsList.add(translnParts[partNum]);
			} // for each word enclosed in *** in the translation
		} // if there is a translation
		final StringBuilder output = Utils.getBuilder(translnParts[0]);
		for (int numNum = 0; numNum < nums.length; numNum++) {
			// find ***-enclosed English phrase's position in translation
			int englPosnInTransln = translnPartsList.indexOf(
					englParts[numNum * 2 + 1]);
			if (englPosnInTransln < 0 || englPosnInTransln >= nums.length) {
				englPosnInTransln = numNum;
			} // if couldn't find position of English in translation
			output.append(nums[englPosnInTransln]);
			final int partNum = (numNum + 1) * 2;
			if (partNum < translnParts.length)
				output.append(translnParts[partNum]);
		} // for each nums
		return output.toString().trim();
	} // translate(String, int[])

	/** Gets Javascript-compatible translation of selected phrase or
	 * the original.  Both English and translation will have the
	 * form, "blah blah blah ***word1*** blah blah ***word2*** blah".  The order
	 * of ***word1*** and ***word2*** may be different in the translation.
	 * <p>This method also converts Unicode to character entity references.
	 * @param	phrase	a phrase to translate
	 * @param	substns	 strings to substitute into the phrase
	 * @return	Javascript-compatible translated phrase or the original
	 */
	public String translateJS(String phrase, String[] substns) {
		return Utils.toValidJS(translate(phrase, substns));
	} // translateJS(String, String[])

	/** Gets translation of a phrase, or returns the original
	 * if a translation in one of the user's selected languages is not found,
	 * and substitutes the given Strings into the phrase.  
	 * Both English and translation will have the
	 * form, "blah blah blah ***word1*** blah blah ***word2*** blah".  The order
	 * of ***word1*** and ***word2*** may be different in the translation.
	 * <p>The translation (or original phrase) is stored in a Map for lookup
	 * later.  If no translations at all are found, ACE stores the phrase in the 
	 * database.
	 * @param	phrase	a phrase to translate
	 * @param	substns	 strings to substitute into the phrase
	 * @return	translated phrase or the original
	 */
	public String translate(String phrase, String[] substns) {
		final String SELF = "User.translate(String, String[]): ";
	 	final String transln = translate(phrase);
		if (Utils.isEmpty(substns) || substns[0] == null) return transln;
		final String[] translnParts = pad(transln).split(STARS);
		final String[] englParts = pad(phrase).split(STARS);
		debugPrint(SELF + "translnParts = ", translnParts, 
				", englParts = ", englParts, ", substns = ", substns);
		// words enclosed in *** may not be in same order in translation as in
		// English; make list of such words to correlate to English
		final List<String> translnPartsList = new ArrayList<String>();
		if (!phrase.equals(transln)) {
			for (int partNum = 1; partNum < translnParts.length; partNum += 2) {
				translnPartsList.add(translnParts[partNum]);
			} // for each word enclosed in *** in the translation
		} // if there is a translation
		final StringBuilder output = Utils.getBuilder(translnParts[0]);
		for (int substnNum = 0; substnNum < substns.length; substnNum++) {
			// find ***-enclosed English phrase's position in translation
			if (substnNum * 2 + 1 < englParts.length) {
				int englPosnInTransln = translnPartsList.indexOf(
						englParts[substnNum * 2 + 1]);
				if (englPosnInTransln < 0 || englPosnInTransln >= substns.length) {
					englPosnInTransln = substnNum;
				} // if couldn't find position of English in translation
				output.append(substns[englPosnInTransln]);
				final int partNum = (substnNum + 1) * 2;
				if (partNum < translnParts.length)
					output.append(translnParts[partNum]);
			} // if there's a substitution to make
		} // for each substns
		return output.toString().trim();
	} // translate(String, String[])

	/** Pads a string with one space at beginning and end.
	 * @param	str	the string
	 * @return	the padded string
	 */
	private final String pad(String str) {
		return Utils.toString(' ', str, ' ');
	} // pad(String)

} // User

