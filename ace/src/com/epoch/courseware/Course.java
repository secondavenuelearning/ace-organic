package com.epoch.courseware;

import com.epoch.courseware.courseConstants.CourseConstants;
import com.epoch.db.CourseRW;
import com.epoch.db.EnrollmentRW;
import com.epoch.db.ForumRW;
import com.epoch.exceptions.DBException;
import com.epoch.utils.AuthUtils;
import com.epoch.utils.DateUtils;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;

/** An ACE course. */
final public class Course implements CourseConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}
	
	/** Unique ID of the course. */
	private int id;
	/** Flags of this course.  */
	private int flags = 0;
	/** Name of the course. */
	private String name = "";
	/** Description of the course. */
	private String description = "";
	/** Date at which course is enabled. */
	private Date enableDate = new Date(System.currentTimeMillis());
	/** Home page of the course at instructor's institution. Informational only. */
	private String homePage = "";
	/** Notes to display to the student; easily edited by instructor. */
	private String notes = "";
	/** Time zone of the instructor's institution. */
	private TimeZone timeZone = TimeZone.getDefault();
	/** Textbook of this course.  Relevant only when displaying
	 * questions in by-text mode in homework assembly tool.  */
	private String book = DEFAULT_AUTHOR; 
	/** When not zero, ID number of the ACE online textbook associated with this
	 * course. */
	transient private int aceBookId = 0;
	/** When this course is one of the courses of an instructor, indicates
	 * whether the instructor is really a coinstructor. */
	private boolean amCoinstructor = false;
	/** Login ID of the owner of this course. */
	private String ownerId = "";
	/** Maximum length of extensions across the course; can be overriden by
	 * instructor. */
	private String maxExtensionsStr = "0.0";
	/** Hashed password of this course; may be empty.  */
	private byte[] passwordHash = new byte[0];

	/** Constructor. */
	public Course() {
		id = 0;
		setNumDecimals(1);
	} // Course()

	/** Constructor. 
	 * @param	id1	unique ID number of the course
	 */
	public Course(int id1) {
		id = id1;
		setNumDecimals(1);
	} // Course(int)

	/** Copy constructor. 
	 * @param	id1	unique ID number of the course
	 * @param	c	course to copy
	 */
	public Course(int id1, Course c) {
		id = id1;
		flags = c.flags;
		homePage = c.homePage;
		name = c.name;
		description = c.description;
		notes = c.notes;
		book = c.book;
		aceBookId = c.aceBookId;
		timeZone = c.timeZone;
		ownerId = c.ownerId;
		maxExtensionsStr = c.maxExtensionsStr;
		passwordHash = Utils.getCopy(c.passwordHash);
		enableDate = c.enableDate;
	} // Course(int, Course)

	/** Gets the unique ID number of this course.
	 * @return	unique ID number of this course
	 */
	public int getId() 							{ return id; }
	/** Sets the unique ID number of this course. Used only by
	 * admin/saveCourse.jsp.
	 * @param	newId	unique ID number of this course
	 */
	public void setId(int newId) 				{ id = newId; }
	/** Gets the flags of this course. Used only by db/CourseRW.java.
	 * @return	flags of this course
	 */
	public int getFlags() 						{ return flags; }
	/** Sets the flags of this course.
	 * @param	flgs	flags of this course
	 */
	public void setFlags(int flgs) 				{ flags = flgs; }
	/** Gets the name of this course.
	 * @return	name of this course
	 */
	public String getName() 					{ return name; }
	/** Sets the name of this course.
	 * @param	nm	name of this course
	 */
	public void setName(String nm) 				{ name = nm; }
	/** Gets the date this course becomes enabled.
	 * @return	date this course becomes enabled
	 */
	public Date getEnableDate() 				{ return enableDate; }
	/** Gets the notes of this course.
	 * @return	notes of this course
	 */
	public String getNotes() 					{ return notes; }
	/** Sets the notes of this course.
	 * @param	n	notes of this course
	 */
	public void setNotes(String n)				{ notes = n; }
	/** Gets the book of this course.
	 * @return	book of this course
	 */
	public String getBook() 					{ return book; }
	/** Sets the book of this course.
	 * @param	bk	book of this course
	 */
	public void setBook(String bk)				{ book = bk; }
	/** Gets whether an ACE online textbook is associated with this course.
	 * @return	true if an ACE online textbook is associated with this course
	 */
	public boolean hasACEBook() 				{ return aceBookId != 0; }
	/** Gets the ID number of the ACE online textbook associated with this course.
	 * @return	ID number of ACE online textbook associated with this course
	 */
	public int getACEBookId() 					{ return aceBookId; }
	/** Sets the ID number of the ACE online textbook associated with this course.
	 * @param	bkId	ID number of ACE online textbook associated with this course
	 */
	public void setACEBookId(int bkId) 			{ aceBookId = bkId; }
	/** Gets the login ID of the owner of this course.
	 * @return	login ID of the owner of this course
	 */
	public String getOwnerId() 					{ return ownerId; }
	/** Sets the login ID of the owner of this course.
	 * @param	oi	login ID of the owner of this course
	 */
	public void setOwnerId(String oi)			{ ownerId = oi; }
	/** Gets the home page of this course.
	 * @return	home page of this course
	 */
	public String getHomePage() 				{ return homePage; }
	/** Sets the home page of this course.
	 * @param	hp	home page of this course
	 */
	public void setHomePage(String hp)			{ homePage = hp; }
	/** Gets the description of this course.
	 * @return	description of this course
	 */
	public String getDescription() 				{ return description; }
	/** Sets the description of this course.
	 * @param	desc	description of this course
	 */
	public void setDescription(String desc)		{ description = desc; }
	/** Gets the maximum extensions across all assignments of this course.
	 * @return	maximum extensions across all assignments of this course
	 */
	public double getMaxExtensions() 			{ return MathUtils.parseDouble(
														maxExtensionsStr); }
	/** Gets the string value of the maximum extensions across all assignments 
	 * of this course.
	 * @return	string value of the maximum extensions across all assignments 
	 * of this course
	 */
	public String getMaxExtensionsStr() 		{ return maxExtensionsStr; }
	/** Sets the string value of the maximum extensions across all assignments 
	 * of this course.
	 * @param	str	string value of the maximum extensions across all 
	 * assignments of this course
	 */
	public void setMaxExtensionsStr(String str)	{ maxExtensionsStr = str; }
	/** Gets the time zone of this course.
	 * @return	time zone of this course
	 */
	public TimeZone getTimeZone() 				{ return timeZone; }
	/** Sets the time zone of this course.
	 * @param	tz	time zone of this course
	 */
	public void setTimeZone(TimeZone tz)		{ timeZone = tz; }
	/** Gets whether the owner of this course is really a coinstructor of it.
	 * @return	true if the owner of this course is really a coinstructor of it
	 */
	public boolean getAmCoinstructor() 			{ return amCoinstructor; }
	/** Sets whether the owner of this course is really a coinstructor of it.
	 * @param	co	true if the owner of this course is really a
	 * coinstructor of it
	 */
	public void setAmCoinstructor(boolean co)	{ amCoinstructor = co; }
	/** Gets whether this course should not be displayed on the 
	 * course list page.
	 * @return	true if this course should not be displayed
	 */
	public boolean hide() 						{ return isFlagOn(HIDE); }
	/** Gets whether this course is used for exams.
	 * @return	true if this course is used for exams
	 */
	public boolean isExam() 					{ return isFlagOn(EXAM_CRS); }
	/** Gets whether TAs may grade students in this course.
	 * @return	true if TAs may grade students in this course
	 */
	public boolean tasMayGrade() 				{ return isFlagOn(TAS_MAY_GRADE); }
	/** Gets whether this course's forum is enabled.
	 * @return	true if this course's forum is enabled
	 */
	public boolean forumEnabled() 				{ return isFlagOn(FORUM_ON); }
	/** Gets whether to hide synthesis calculated products from students in this
	 * course.  Turn on before an exam, turn off after the exam.
	 * @return	true if ACE should hide synthesis calculated products from
	 * students in this course
	 */
	public boolean hideSynthCalcdProds() 		{ return isFlagOn(HIDE_SYNTH_CALCD_PRODS); }
	/** Gets whether to sort students in this course by student ID number
	 * @return	true if should sort students in this course by student ID number
	 */
	public boolean sortByStudentNum() 			{ return isFlagOn(SORT_BY_STUDENT_NUM); }
	/** Gets if the flag is on.
	 * @param	flagVal	the value of the flag
	 * @return	true if the flag is on
	 */
	private boolean isFlagOn(int flagVal)		{ return (flags & flagVal) != 0; }
	/** Gets whether this course has a password.
	 * @return	true if this course has a password
	 */
	public boolean hasPassword()				{ return !Utils.isEmpty(passwordHash); }
	/** Get the hashed password.
	 * @return	the hashed password
	 */
	public byte[] getPasswordHash()				{ return passwordHash; }
	/** Sets whether this course should not be displayed on the course list 
	 * page.
	 * @param	on	whether this course should not be displayed
	 */
	public void setHide(boolean on) 			{ setFlag(on, HIDE); }
	/** Sets whether this course is used for exams.
	 * @param	on	whether this course is used for exams
	 */
	public void setIsExam(boolean on) 			{ setFlag(on, EXAM_CRS); }
	/** Sets whether TAs may grade students in this course.
	 * @param	on	whether TAs may grade students in this course
	 */
	public void setTAsMayGrade(boolean on) 		{ setFlag(on, TAS_MAY_GRADE); }
	/** Sets whether this course's forum is enabled.
	 * @param	on	whether this course's forum is enabled
	 */
	public void setForumEnabled(boolean on) 	{ setFlag(on, FORUM_ON); }
	/** Sets whether to hide synthesis calculated products from students in this
	 * course.  Turn on before an exam, turn off after the exam.
	 * @param	on whether ACE should hide synthesis calculated products from
	 * students in this course
	 */
	public void setHideSynthCalcdProds(boolean on) { setFlag(on, HIDE_SYNTH_CALCD_PRODS); }
	/** Sets whether to sort students in this course by student ID number
	 * @param	on	whether to sort students in this course by student ID number
	 */
	public void setSortByStudentNum(boolean on) { setFlag(on, SORT_BY_STUDENT_NUM); }

	/** For all users in the course, turns off the flag that prevents them from
	 * seeing calculated synthesis products in any course.
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void letUsersSeeSynthCalcdProds() throws DBException {
		EnrollmentRW.letSeeSynthCalcdProds(id);
	} // letUsersSeeSynthCalcdProds()

	/** Gets the date in a time zone that this course becomes enabled, as a 
	 * string.
	 * @return	date this course becomes enabled (MMM d, yyyy, h:mm aa)
	 * or null if the course is never enabled
	 */
	public String getEnableDateStr() { 
		return (enableDate == null ? null 
				: DateUtils.getStringNoTimeZone(enableDate, timeZone)); 
	} // getEnableDateStr()

	/** Gets whether this course is enabled.
	 * @return	true if this course is enabled
	 */
	public boolean isEnabled() { 
		return enableDate != null
				&& enableDate.before(new Date(System.currentTimeMillis())); 
	} // isEnabled()

	/** Gets the number of decimals to display for each grade in the gradebook.
	 * @return	the number of decimals to display for each grade in the
	 * gradebook
	 */
	public int getNumDecimals() { 
		return ((flags & DECIMAL_MASK) >> DECIMAL_SHIFT); 
	} // getNumDecimals()

	/** Gets the IP addresses from which a student may enter this course.
	 * @return	array of IP addresses from which a student may enter this course
	 */
	public String[] getAllowedIPAddrs() { 
		try {
			return CourseRW.getAllowedIPAddresses(id);
		} catch (DBException e) {
			Utils.alwaysPrint("Course.getAllowedIPAddrs: caught DBException "
					+ "getting IP addresses for course with ID ", id);
			e.printStackTrace();
		} // try
		return new String[0];
	} // getAllowedIPAddrs()

	/** Sets the value of the hashed password of this course without storing it.
	 * @param	pwd	the hashed password
	 */
	public void setPasswordHash(byte[] pwd)	{ 
		passwordHash = (pwd == null ? new byte[0] : pwd); 
	} // setPasswordHash(byte[])

	/** Checks if the entered password matches the course password.
	 * @param	enteredPwd	the entered password
	 * @return	true if the entered password matches the course password
	 */
	public boolean checkPassword(String enteredPwd) {
		debugPrint("Course.checkPassword: enteredPwd = ", enteredPwd);
		final byte[] hashValueArr = Base64.getDecoder().decode(enteredPwd + "==");
			// the == is just prophylactic; it is ignored if unneeded.
		return AuthUtils.verifyHashValue(getPasswordHash(), "", hashValueArr);
	} // checkPassword(String)

	/** Sets when this course should be enabled.
	 * @param	date	when this course should be enabled
	 */
	public void setEnableDate(Date date) {
		enableDate = date;
	} // setEnableDate(Date)

	/** Sets a flag.
	 * @param	on	whether to turn the flag on or off
	 * @param	flag	the flag to set
	 */
	private void setFlag(boolean on, int flag) {
		if (on) flags |= flag;
		else flags &= ~flag;
	} // setFlag(boolean, int)

	/** Sets the number of decimals to display for each grade in the gradebook.
	 * @param	numDecimals	the number of decimals to display for each grade in 
	 * the gradebook
	 */
	public void setNumDecimals(int numDecimals) {
		flags = (flags & ~DECIMAL_MASK) | (numDecimals << DECIMAL_SHIFT);
	} // setNumDecimals(int)

	/** Gets whether a student's IP address matches one of the allowed IP
	 * addresses.  If the allowed IP address has four numbers, the student's IP
	 * address must match it exactly; if it has fewer, the student's IP address
	 * must merely begin with it; if it contains a / character, it is sent to
	 * the class IpSubnet, which interprets it as a classful or classless range
	 * of routing numbers.
	 * @param	ipAddr	a student's IP address
	 * @param	user	the user
	 * @return	true if this course does not check IP addresses, or
	 * if there are no allowed IP addresses listed, or 
	 * if the student's IP address matches one of the allowed IP addresses 
	 */
	public boolean isOkIPAddress(String ipAddr, User user) {
		final String SELF = "Course.isOkIPAddress: ";
		final String[] allowedIPAddrs = getAllowedIPAddrs();
		if (Utils.isEmpty(allowedIPAddrs)) return true;
		debugPrint(SELF + "for course ", id, ", checking IP: ", ipAddr);
		final String modIPAddr = ipAddr + ".";
		for (final String allowedIPAddr : allowedIPAddrs) {
			if (allowedIPAddr.indexOf('/') != -1) {
				try {
					final IpSubnet ipSubnet = new IpSubnet(allowedIPAddr);
					if (ipSubnet.contains(ipAddr)) return true;
				} catch (UnknownHostException e) {
					debugPrint(SELF + "UnknownHostException");
				}
			} else {
				String allowedAddr = allowedIPAddr.trim();
				if (!allowedAddr.endsWith(".")) allowedAddr += ".";
				if (modIPAddr.startsWith(allowedAddr)) return true;
			} // if have range of IP addresses
		} // for each allowed IP address
		Utils.alwaysPrint(SELF + "disallowed IP address ", ipAddr, 
				" is being used by ", user.getUserId(), " (", 
				user.getName(), ")");
		return false;
	} // isOkIPAddress(String, User)

	/** Gets whether this course has any topics posted on the forum
	 * @return	true if this course has any topics posted on the forum 
	 * @throws	DBException	if there's a problem reading the database
	 */
	public boolean hasForumTopics() throws DBException {
		return ForumRW.getNumTopics(id) > 0;
	} // hasForumTopics()

} // Course
