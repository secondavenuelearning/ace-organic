package com.epoch.session;

import static com.epoch.db.dbConstants.CourseRWConstants.*;
import com.epoch.AppConfig;
import com.epoch.assgts.Assgt;
import com.epoch.courseware.Course;
import com.epoch.courseware.EnrollmentData;
import com.epoch.courseware.ForumPost;
import com.epoch.courseware.ForumTopic;
import com.epoch.courseware.User;
import com.epoch.courseware.courseConstants.UserConstants;
import com.epoch.db.CourseRW;
import com.epoch.db.EnrollmentRW;
import com.epoch.db.ForumRW;
import com.epoch.db.HWRead;
import com.epoch.db.HWWrite;
import com.epoch.db.InstitutionRW;
import com.epoch.db.QuestionRW;
import com.epoch.db.TextbookRW;
import com.epoch.db.TranslnWrite;
import com.epoch.db.UserRead;
import com.epoch.db.UserWrite;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.InvalidOpException;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.UniquenessException;
import com.epoch.textbooks.Textbook;
import com.epoch.utils.MathUtils;
import com.epoch.utils.SortUtils;
import com.epoch.utils.Utils;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** An instructor's work session. */
public class InstructorSession extends StudentSession implements UserConstants {

	/* Use UserSession debugPrint(). */
	
	/** Course ID for which enrollment data is already loaded.  May be different
	 * from the selected course for a short time.  */
	transient private int enrollmentPreloadedId;
	/** Students enrolled in the course. */
	transient private List<EnrollmentData> enrolledUsers;
	/** Temporary login IDs created for the purpose of taking exams. */
	transient private String[][] examIds = new String[2][0];

	/** Constructor.
	 * @param	instructor1	the instructor for whom to create this session
	 * @throws	DBException	if the instructor can't be populated
	 */
	public InstructorSession(User instructor1) throws DBException {
		super(instructor1);
		initialiseInstr(instructor1);
	} // InstructorSession(User)

	/** Called only by TAs from gradebook/gradebook.jsp
	 * and enroll/listEnrollment.jsp so they can get information about other
	 * students in the course.
	 * @param	courseId	ID number of the currently selected course
	 * @param	userTA	the User object representing the TA
	 */
	public InstructorSession(int courseId, User userTA) {
		try {
			actedUser = userTA;
			activeCrs = CourseRW.getCourseInfo(courseId);
			courses = new ArrayList<Course>();
			courses.add(activeCrs);
			activeCrsNum = 1;
			assgts = HWRead.getHWs(courseId);
			examIds[ALL] = null;
			examIds[UNUSED] = null;
		} catch (DBException e) {
			Utils.alwaysPrint("InstructorSession: DBException while "
					+ "trying to initialize dummy instructor.");
			e.printStackTrace();
		} // try
	} // InstructorSession(int, User)

	/** Initialize this instructor.  May be called by constructor or from
	 * AdminSession (for impersonation).
	 * @param	instructor1	the instructor for whom to create this session
	 * @throws	DBException	if the instructor can't be populated
	 */
	final public void initialiseInstr(User instructor1) throws DBException {
		final String SELF = "InstructorSession.initialiseInstr: ";
		if (instructor1.getRole() == User.INSTRUCTOR) {
			actedUser = instructor1;
			courses = CourseRW.getCoursesCreated(actedUser.getUserId());
			examIds[ALL] = null;
			examIds[UNUSED] = null;
		} else {
			debugPrint(SELF + "user to be initialized is student.");
			initialiseStud(instructor1);
		} // if user to be initialized is instructor
	} // initialiseInstr(User)

/* ***************** Course functions *******************/

	/** Records a new course, assigns it an ID number, and adds it to the 
	 * instructor's list of courses.
	 * @param	course	a new course to be saved to the database
	 * @param	ipAddrs	IP addresses from which students are allowed to access
	 * this course
	 * @throws	DBException	if the database can't be written to
	 */
	public void addCourse(Course course, String[] ipAddrs) throws DBException {
		final String SELF = "InstructorSession.addCourse: ";
		final Course newCourse = 
				CourseRW.addCourse(actedUser.getUserId(), course);
		courses.add(newCourse);
		final int newCourseId = newCourse.getId();
		course.setId(newCourseId);
		debugPrint(SELF + "new course has been assigned ID = ", newCourseId);
		if (!Utils.isEmpty(ipAddrs)) {
			CourseRW.setAllowedIPAddresses(newCourseId, ipAddrs);
		} // if there are IP addresses to set
	} // addCourse(Course, String[])

	/** Modifies one of this instructor's courses.
	 * @param	course	a course containing the new information
	 * @param	ipAddrs	IP addresses from which students are allowed to access
	 * this course
	 * @throws	DBException	if the database can't be written to
	 */
	public void setCourse(Course course, String[] ipAddrs) throws DBException {
		CourseRW.setCourse(course);
		CourseRW.setAllowedIPAddresses(course.getId(), ipAddrs);
	} // setCourse(Course, String[])

	/** Reveals the given hidden courses. 
	 * @param	crsIds	the hidden courses' ID numbers
	 * @throws	DBException	if the database can't be written to
	 */
	public void revealCourses(int[] crsIds) throws DBException {
		final Course[] revealCourses = new Course[crsIds.length];
		int numCrsId = 0;
		for (final Course crs : courses) {
			if (Utils.contains(crsIds, crs.getId())) {
				crs.setHide(false);
				revealCourses[numCrsId++] = crs;
			} // if course should be revealed
		} // for each selected course
		CourseRW.setFlags(revealCourses);
	} // revealCourses(int[])

	/** Modifies the password of one of this instructor's courses.
	 * @param	course	a course containing the new information
	 * @throws	DBException	if the database can't be written to
	 */
	public void setCoursePassword(Course course) throws DBException {
		CourseRW.setCoursePassword(course);
	} // setCoursePassword(Course)

	/** Deletes one of this instructor's courses.
	 * @param	crsNum	1-based serial number of the course
	 * @throws	ParameterException	when crsNum is out of bounds
	 * @throws	DBException	if the database can't be written to
	 */
	public void removeCourse(int crsNum) throws ParameterException,
			DBException {
		if (crsNum <= 0 || crsNum > courses.size())
			throw new ParameterException("Invalid index to removeCourse");
		final int courseId = courses.get(crsNum - 1).getId();
		CourseRW.removeCourse(courseId);
		courses.remove(crsNum - 1);
	} // removeCourse(int)

	/** Gets information about all hidden courses that this instructor owns.
	 * @return	array of information about the courses
	 */
	public Course[] getHiddenCourses() {
		final List<Course> hiddenCourses = new ArrayList<Course>(courses);
		final int numCourses = hiddenCourses.size();
		final List<Comparable<?>> hiddenCrsIds = new ArrayList<Comparable<?>>();
		for (int crsNum = numCourses - 1; crsNum >= 0; crsNum--) {
			final Course course = hiddenCourses.get(crsNum);
			if (!course.hide()) hiddenCourses.remove(crsNum);
			else hiddenCrsIds.add(0, Integer.valueOf(course.getId()));
		} // for each course 
		try {
			SortUtils.sort(hiddenCourses, hiddenCrsIds);
		} catch (ParameterException e) {
			debugPrint("ParameterException in getHiddenCourses");
		}
		return hiddenCourses.toArray(new Course[hiddenCourses.size()]);
	} // getHiddenCourses()

	/** Deletes one or more of this instructor's hidden courses.
	 * @param	crsIds	the courses' ID numbers
	 * @throws	DBException	if the database can't be written to
	 */
	public void removeHiddenCourses(int[] crsIds) throws DBException {
		final String SELF = "InstructorSession.removeHiddenCourses: ";
		final List<Course> removeCoursesList = new ArrayList<Course>();
		for (final Course crs : courses) {
			if (Utils.contains(crsIds, crs.getId())) {
				removeCoursesList.add(crs);
			} // if course should be removed
		} // for each selected course
		debugPrint(SELF + "will remove courses with IDs ", crsIds,
				" from this instructor's list of ", courses.size(), 
				" courses.");
		CourseRW.removeCourses(crsIds);
		for (final Course course : removeCoursesList) {
			courses.remove(course);
		} // for each course to be removed
		debugPrint(SELF + "now this instructor has ", courses.size(), 
				" courses.");
	} // removeHiddenCourses(int[])

	/** Sets the current course.  Overrides StudentSession.selectCourse(int).
	 * @param	crsNum	1-based serial number of the course
	 * @return	OK
	 * @throws	ParameterException	when crsNum is out of bounds
	 * @throws	DBException	if the database can't be read
	 */
	public int selectCourse(int crsNum) 
			throws DBException, ParameterException {
		final String SELF = "InstructorSession.selectCourse: ";
		debugPrint(SELF + "crsNum = ", crsNum);
		if (crsNum > courses.size() || crsNum <= 0)
			throw new ParameterException("Invalid index to selectCourse");
		activeCrsNum = crsNum;
		activeCrs = courses.get(activeCrsNum - 1);
		final int activeCrsId = activeCrs.getId();
		debugPrint(SELF + "activeCrsId = ", activeCrsId);
		assgts = HWRead.getHWs(activeCrsId);
		for (int hwNum = assgts.size(); hwNum > 0; hwNum--) {
			final Assgt assgt = assgts.get(hwNum - 1);
			final boolean isVisible = assgt.isVisible();
			debugPrint(SELF + "assignment ", hwNum, ' ', assgt.getName(), 
					" has visibility ", (isVisible ? "on." : "off."));
		} // for each assignment
		examIds[ALL] = null;
		examIds[UNUSED] = null;
		if (actedUser.getRole() == STUDENT && activeCrs.isExam()) {
			EnrollmentRW.setFirstEntry(activeCrsId, actedUser.getUserId());
		} // if course is an exam
		return OK;
	} // selectCourse(int)

	/** Sets the current course.  Overrides StudentSession.selectCourseById(int).
	 * @param	crsId	ID number of the course
	 * @return	OK
	 * @throws	ParameterException	if the course is not found
	 * @throws	DBException	if the database can't be read
	 */
	public int selectCourseById(int crsId) 
			throws DBException, ParameterException {
		final String SELF = "InstructorSession.selectCourseById: ";
		debugPrint(SELF + "crsId = ", crsId);
		boolean success = false;
		int crsNum = 1;
		for (final Course course : courses) {
			if (course.getId() == crsId) {
				success = true;
				break;
			} else crsNum++;
		} // for each course
		if (!success) throw new ParameterException("Invalid course ID " + crsId
					+ " submitted to selectCourseById()");
		return selectCourse(crsNum);
	} // selectCourseById(int)

	/** Gets the current course information.
	 * @return	the current course
	 * @throws	InvalidOpException	when no course has been selected
	 */
	public Course getSelectedCourse() throws InvalidOpException {
		if (activeCrsNum == 0)
			throw new InvalidOpException(" no course selected ");
		return activeCrs;
	} // getSelectedCourse()

	/** Sets the current course's note to students.
	 * @param	notes	note to the students
	 * @param	notify	whether to notify the class participants that new notes
	 * have been posted
	 * @throws	InvalidOpException	when no course has been selected
	 * @throws	DBException	if the database can't be written to
	 */
	public void setCourseNotes(String notes, boolean notify) 
			throws DBException, InvalidOpException {
		if (activeCrsNum == 0)
			throw new InvalidOpException(" no course selected ");
		final String notesStr = notes.trim();
		activeCrs.setNotes(notesStr);
		CourseRW.setCourse(activeCrs);
		if (notify) {
			final String msg = Utils.toString(actedUser.getName(), 
						" has changed the course notes: ", notesStr)
					.replaceAll("'", "\\'");
			notifyAll(msg);
		} // if notify
	} // setCourseNotes(String, boolean)

	/** Adds a coinstructor to a course.
	 * @param	courseId	unique ID of a course
	 * @param	instructorId	login ID of the
	 * coinstructor to be added
	 * @throws	UniquenessException	if the new coinstructor can't be found
	 * or uniquely identified or is already a coinstructor
	 * @throws	DBException	if the database can't be written to
	 */
	public void addCoinstructor(int courseId, String instructorId)
			throws DBException, UniquenessException {
		CourseRW.addCoinstructor(courseId, instructorId);
	} // addCoinstructor(String, String)

	/** Removes a coinstructor from a course.
	 * @param	courseId	unique ID of a course
	 * @param	instructorId	login ID of the
	 * coinstructor to be removed
	 * @throws	DBException	if the database can't be written to
	 */
	public static void removeCoinstructor(int courseId,
			String instructorId) throws DBException {
		CourseRW.removeCoinstructor(courseId, instructorId);
	} // removeCoinstructor(String, String)

	/** Gets owners of courses for which this instructor is a coinstructor.
	 * @return	array of partially populated User objects containing names and
	 * login IDs
	 * @throws	DBException	if the database can't be read
	 */
	public User[] getCoinstructedCrsAndCoauthoredBkOwners() throws DBException {
		return CourseRW.getCoinstructedCrsAndCoauthoredBkOwners(
				actedUser.getUserId());
	} // getCoinstructedCrsAndCoauthoredBkOwners()

	/** Gets coinstructors of the currently selected course. 
	 * @return	coinstructors of the currently selected course
	 * @throws	DBException	if the database can't be read
	 */
	public User[] getCoinstructors() throws DBException {
		return CourseRW.getCoinstructors(activeCrs.getId());
	} // getCoinstructors()

	/** Gets if any of the instructors of the selected course is at a 
	 * different institution from the course owner.
	 * @return	true if any of the instructors of the selected course is at a
	 * different institution from the course owner
	 * @throws	DBException	if the database can't be read
	 */
	public boolean courseIsMultiinstitution() throws DBException {
		final String SELF = "InstructorSession.courseIsMultiinstitution: ";
		boolean isMultiinstitution = false;
		debugPrint(SELF + "Entering.");
		if (activeCrs != null) {
			final String activeCrsOwnerId = activeCrs.getOwnerId();
			debugPrint(SELF + "activeCrsOwnerId = ", activeCrsOwnerId);
			final User owner = UserRead.getUser(activeCrsOwnerId);
			for (final User coinstructor : getCoinstructors()) {
				if (coinstructor.getInstitutionId() != owner.getInstitutionId()) {
					isMultiinstitution = true;
					break;
				} // if coinstructor is at a different institution from course owner
			} // for each coinstructor
		} // if there's an active course
		return isMultiinstitution;
	} // courseIsMultiinstitution()

	/** Gets if any of the students in the selected course are at a 
	 * different institution from the others.
	 * @return	true if any of the students in the selected course are at a
	 * different institution from the others
	 * @throws	DBException	if the database can't be read
	 */
	public boolean studentsAreMultiinstitution() throws DBException {
		boolean areMultiinstitution = false;
		final EnrollmentData[] enrolledStudents = getEnrolledStudents();
		if (!Utils.isEmpty(enrolledStudents)) {
			final int institnId = enrolledStudents[0].getInstitution().getId();
			for (final EnrollmentData student : enrolledStudents) {
				if (student.getInstitution().getId() != institnId) {
					areMultiinstitution = true;
					break;
				} // if any student is at a different institution from first
			} // for each enrolled student
		} // if there are enrolled students
		return areMultiinstitution;
	} // studentsAreMultiinstitution()

	/** Changes the order of the instructor's courses. 
	 * @param	oldCrsNum	old 1-based serial number of a course
	 * @param	newCrsNum	new 1-based serial number of the course
	 * @throws	DBException	if the database can't be written to
	 */
	public void moveCourse(int oldCrsNum, int newCrsNum) throws DBException {
		final String SELF = "InstructorSession.moveCourse: ";
		debugPrint(SELF + "oldCrsNum = ", oldCrsNum, ", newCrsNum = ",
				newCrsNum, ", actedUserId = ", actedUser.getUserId(),
				", number of courses = ", courses.size());
		courses.add(newCrsNum - 1, courses.remove(oldCrsNum - 1));
		CourseRW.setCourseSerialNos(courses, actedUser.getUserId());
	} // moveCourse(int, int)

/* ***************** Forum functions *******************/

	/** Notifies students and coinstructors with a message. 
	 * @param	msg	the message
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void notifyAll(String msg) throws DBException {
		final String SELF = "InstructorSession.notifyAll: ";
		getEnrolledStudents();
		final List<String> txtMsgEmails = new ArrayList<String>();
		for (final EnrollmentData enrolledUser : enrolledUsers) {
			final String txtMsgEmail = enrolledUser.getTextMessageEmail();
			if (!Utils.isEmpty(txtMsgEmail)) {
				txtMsgEmails.add(txtMsgEmail);
			} // if there is a text message email
		} // for each enrolled student
		for (final User coinstructor : getCoinstructors()) {
			final String txtMsgEmail = coinstructor.getTextMessageEmail();
			if (!Utils.isEmpty(txtMsgEmail)) {
				txtMsgEmails.add(txtMsgEmail);
			} // if there is a text message email
		} // for each coinstructor
		debugPrint(SELF + "sending message \"", msg, "\" to: ", txtMsgEmails);
		sendTextMessages(txtMsgEmails.toArray(
				new String[txtMsgEmails.size()]), msg);
	} // notifyAll(String)

	/** Adds a new topic to a course's forum, and notifies students and
	 * coinstructors. 
	 * @param	topic	the topic to add to the forum
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void addForumTopic(ForumTopic topic) throws DBException {
		ForumRW.addTopic(topic);
		final String msg = 
				Utils.toString(actedUser.getName(), 
					" has added a new forum topic: ", topic.getTitle())
				.replaceAll("'", "\\'");
		notifyAll(msg);
	} // addForumTopic(ForumTopic)

	/** Sets the title and stickiness of a forum topic.
	 * @param	topicId	unique ID of the forum topic
	 * @param	title	title of the topic
	 * @param	stickiness	stickiness of the topic
	 * @throws	DBException	if the database can't be written to
	 */
	public void setForumTopic(int topicId, String title, boolean stickiness) 
			throws DBException {
		ForumRW.setTopic(topicId, title, stickiness);
	} // setForumTopic(int, String, boolean)

	/** Changes the text and figure of a post, regardless of who authored it. 
	 * @param	post	the new post
	 * @throws	DBException	if the database can't be written to
	 */
	public void editForumPost(ForumPost post) throws DBException {
		final String SELF = "InstructorSession.editForumPost: ";
		final ForumPost origPost = ForumRW.getPost(post.getId());
		if (origPost != null) {
			ForumRW.editPost(post);
			sendPostAlert(post, EDITED);
		} else addForumPost(post); 
	} // editForumPost(ForumPost)

	/** Deletes a post from a course's forum, regardless of who authored it
	 * or whether it's the most recent post in the topic. 
	 * @param	postId	the post to delete
	 * @throws	DBException	if the database can't be written to
	 */
	public void deleteForumPost(int postId) throws DBException {
		ForumRW.deletePost(postId);
	} // deleteForumPost(int)

	/** Deletes a topic from a course's forum. 
	 * @param	topicId	the topic to delete
	 * @throws	DBException	if the database can't be written to
	 */
	public void deleteForumTopic(int topicId) throws DBException {
		ForumRW.deleteTopic(topicId);
	} // deleteForumTopic(int)

	/** Gets the assignment and an array of question numbers corresponding to
	 * the assignment and question ID numbers for this instructor.  The array of
	 * question numbers has one member for fixed questions and two members for
	 * questions that are part of a randomized group.
	 * @param	linkedAssgtQIds	the assignment and question ID numbers
	 * @return	array of two arrays: a 1-membered array with the assignment 
	 * number, and a 0- to 2-membered array with the question number or numbers
	 * @throws	InvalidOpException	when no course has been selected
	 * @throws	DBException	if the database can't be read
	 */
	public int[][] getAssgtAndQNums(int[] linkedAssgtQIds) 
			throws DBException, InvalidOpException {
		final String SELF = "InstructorSession.getAssgtAndQNums: ";
		final int[][] assgtAndQNums = new int[2][0];
		final int hwNum = Utils.indexOf(getHWIds(), linkedAssgtQIds[0]) + 1;
		assgtAndQNums[0] = new int[] {hwNum};
		assgtAndQNums[1] = (hwNum <= 0 ? new int[0]
				: assgts.get(hwNum - 1).getQNum(linkedAssgtQIds[1]));
		debugPrint(SELF + "linkedAssgtQIds = ", linkedAssgtQIds,
				", assgtAndQNums = ", assgtAndQNums);
		return assgtAndQNums;
	} // getAssgtAndQNums(int[])

	/** Blocks or unblocks users from the current course's forum.
	 * @param	modifyNums 1-based indices in enrolledUsers of the students
	 * to block or unblock
	 * @param	block	whether to block or unblock them
	 * @throws	ParameterException	when a student index is out of bounds
	 * or a student is not registered
	 * @throws	InvalidOpException	when no course has been selected or the list
	 * of already-enrolled or enrolled-but-not-registered students has not been 
	 * initialized
	 * @throws	DBException	if the database can't be written to
	 */
	public void blockUsers(int[] modifyNums, boolean block) 
			throws InvalidOpException, DBException, ParameterException {
		final String SELF = "InstructorSession.blockUsers: ";
		if (activeCrsNum == 0)
			throw new InvalidOpException("blockUsers without selected course");
		final int crsId = activeCrs.getId();
		if (enrolledUsers == null || enrollmentPreloadedId != crsId)
			throw new InvalidOpException(
				" getEnrolledStudents must be called before blockUsers");
		final List<String> modifyIds = new ArrayList<String>();
		for (int modifyNum = 0; modifyNum < modifyNums.length; modifyNum++) {
			final int enrolledUserNum = modifyNums[modifyNum]; // 1-based
			if (enrolledUserNum <= 0
					|| enrolledUserNum > enrolledUsers.size())
				throw new ParameterException(" invalid index to blockUsers ");
			final EnrollmentData entry = enrolledUsers.get(enrolledUserNum - 1);
			if (entry.isRegistered()) {
				modifyIds.add(entry.getUserId());
			} else throw new ParameterException(SELF
					+ "cannot block/unblock student " + enrolledUserNum 
					+ " with ID " + entry.getUserId() 
					+ " from forum because s/he's not registered.");
		} // for each modifyNum
		if (!modifyIds.isEmpty()) {
			ForumRW.blockUsers(crsId, modifyIds, block);
		} // if there are IDs to modify
	} // blockUsers(int[], boolean)

/* ***************** Assignment functions *******************/

	/** Sets the current assignments.
	 * @throws	DBException	if the database can't be read
	 */
	public void refreshAssgts() throws DBException {
		final boolean SHOW_INVISIBLE = true;
		refreshAssgts(SHOW_INVISIBLE);
	} // refreshAssgts()

	/** Deletes an assignment from the current course.
	 * @param	hwNum	1-based serial number of the assignment
	 * @throws	ParameterException	when hwNum is out of bounds
	 * @throws	InvalidOpException	when no course has been selected
	 * @throws	DBException	if the database can't be written to
	 */
	public void deleteHWSet(int hwNum) throws DBException, 
			ParameterException, InvalidOpException {
		if (activeCrsNum == 0)
			throw new InvalidOpException("removeDoc without selection");
		if (hwNum <= 0 || hwNum > assgts.size())
			throw new ParameterException("Invalid index " + hwNum 
					+ " to deleteHWSet");
		final Assgt removedAssgt = assgts.remove(hwNum - 1); 
		final int removedHWId = removedAssgt.id;
		HWWrite.deleteHWSet(removedHWId); // also removes assgt dependencies from db
		// now remove dependencies on removed assgt from remaining assgts
		for (final Assgt assgt : assgts) {
			if (assgt.getDependsOnId() == removedHWId) {
				assgt.setDependsOnId(0); 
			} // if remaining assignment depends on removed assignment
		} // for each remaining assignment
	} // deleteHWSet(int)

	/** Moves an assignment up or down.
	 * @param	from	1-based position of the assignment to move
	 * @param	to	new 1-based position of the assignment
	 * @throws	ParameterException	when a position is out of bounds
	 * @throws	InvalidOpException	when no course has been selected
	 * @throws	DBException	if the database can't be written to
	 */
	public void moveHWSet(int from, int to) throws DBException, 
			ParameterException, InvalidOpException {
		if (activeCrsNum == 0)
			throw new InvalidOpException("moveHW without course selection");
		final int[] range = new int[] {1, assgts.size()};
		if (!MathUtils.inRange(from, range) || !MathUtils.inRange(to, range))
			throw new ParameterException("Invalid starting indices " + from 
					+ ", " + to + " to moveHW()");
		// apply changes on a temporary list
		// copy it back only if the db transaction is successful
		final List<Assgt> tempAssgts = assgts;
		tempAssgts.add(to - 1, tempAssgts.remove(from - 1));
		HWWrite.reorderHWs(tempAssgts);
		assgts = tempAssgts;
	} // moveHWSet(int, int)

	/** Merges two or more assignments into a single assignment.
	 * @param	hwNums	1-based numbers of assignments to merge in order of
	 * their position in the list of assignments; first
	 * assignment in list will be retained, others discarded after their content
	 * is added to the first assignment
	 * @throws	DBException	if the database can't be written to
	 */
	public void mergeAssgts(int[] hwNums) throws DBException {
		final Assgt parentAssgt = assgts.get(hwNums[0] - 1);
		final int numHWs = hwNums.length;
		final int[] otherAssgtIds = new int[numHWs - 1];
		for (int hwNumNum = 1; hwNumNum < numHWs; hwNumNum++) {
			final Assgt daughterAssgt = assgts.get(hwNums[hwNumNum] - 1);
			parentAssgt.merge(daughterAssgt);
			otherAssgtIds[numHWs - hwNumNum - 1] = daughterAssgt.id;
		} // for each assignment except the first
		HWWrite.setHWMerger(parentAssgt, otherAssgtIds);
		for (int hwNumNum = numHWs - 1; hwNumNum >= 1; hwNumNum--) {
			assgts.remove(hwNums[hwNumNum] - 1);
		} // for each assignment except the first, in reverse
	} // mergeAssgts(int[])

	/** Makes the visibility of an assignment dependent on another assignment 
	 * being mastered.
	 * @param	dependentHWNum	1-based position of the dependent assignment
	 * @param	masteryHWNum	1-based position of the mastery assignment that 
	 * must be mastered before the dependent assignment can be displayed to a 
	 * student
	 * @throws	ParameterException	when positions are not in range
	 * @throws	DBException	if the database can't be written to
	 */
	public void setAssgtDependency(int dependentHWNum, int masteryHWNum) 
			throws DBException, ParameterException {
		final int[] range = new int[] {1, assgts.size()};
		if (!MathUtils.inRange(dependentHWNum, range) 
				|| !MathUtils.inRange(masteryHWNum, range))
			throw new ParameterException("Invalid indices " + dependentHWNum 
					+ ", " + masteryHWNum + " to setAssgtDependency()");
		final Assgt dependentAssgt = assgts.get(dependentHWNum - 1);
		final int masteryHWId = assgts.get(masteryHWNum - 1).id;
		HWWrite.setAssgtDependency(dependentAssgt.id, masteryHWId);
		dependentAssgt.setDependsOnId(masteryHWId);
	} // setAssgtDependency(int, int)

/* ***************** Enrollment functions *******************/

	/** Gets all students registered in ACE who attend either the institution of
	 * an instructor or coinstructor of the currently selected course or a given
	 * institution, and who registered within a certain number of years.
	 * @param	numYears	number of years back from now in which students
	 * @param	selectedInstitnId	ID of the institution whose registered
	 * students to list; if 0, the institution of the instructor or coinstructor 
	 * of the currently selected course
	 * @return	array of EnrollmentData of the registered students
	 * @throws	DBException	if the database can't be read
	 */
	final public EnrollmentData[] getAllRegdStudents(int numYears, 
			int selectedInstitnId) throws DBException {
		final String SELF = "InstructorSession.getAllRegdStudents: ";
		debugPrint(SELF + "numYears = ", numYears, ", selectedInstitnId = ",
				selectedInstitnId);
		final List<EnrollmentData> studentList = new ArrayList<EnrollmentData>();
		final List<User> instructors = 
				new ArrayList<User>(Arrays.asList(getCoinstructors()));
		final List<Integer> instnIds = new ArrayList<Integer>();
		if (selectedInstitnId == 0) {
			final User owner = UserRead.getUser(activeCrs.getOwnerId());
			instructors.add(0, owner);
			debugPrint(SELF, instructors.size(), " instructors of current course.");
			for (final User instructor : instructors) {
				final int instnId = instructor.getInstitutionId();
				final Integer instnIdObj = Integer.valueOf(instnId);
				if (!instnIds.contains(instnIdObj)) {
					studentList.addAll(numYears == 0
							? UserRead.getAllRegdStudents(instnId, 
								actedUser.getLanguages())
							: UserRead.getAllRegdStudents(instnId, numYears, 
								actedUser.getLanguages()));
					instnIds.add(instnIdObj);
				} // if we don't already have students from this institution
			} // for each coinstructor
		} else {
			studentList.addAll(numYears == 0
					? UserRead.getAllRegdStudents(selectedInstitnId, 
						actedUser.getLanguages())
					: UserRead.getAllRegdStudents(selectedInstitnId, numYears, 
						actedUser.getLanguages()));
		} // if selectedInstitnId
		debugPrint(SELF, studentList.size(), " students at institution",
				selectedInstitnId == 0 ? Utils.toString(" with ID ",
					selectedInstitnId) : "s of instructor and coinstructors");
		return studentList.toArray(new EnrollmentData[studentList.size()]);
	} // getAllRegdStudents(int, int)

	/** Gets students enrolled in all courses of this instructor, in
	 * alphabetical order by last name and surname, excluding TAs and 
	 * unregistered students.
	 * @return	array of EnrollmentData
	 * @throws	DBException	if the database can't be read
	 */
	public EnrollmentData[] getAllEnrolledStudents() throws DBException {
		final List<EnrollmentData> studentsList =
				EnrollmentRW.getEnrolledStudents(
					actedUser.getUserId(), actedUser.getLanguages());
		return studentsList.toArray(new EnrollmentData[studentsList.size()]);
	} // getAllEnrolledStudents()

	/** Gets the number of institutions of all students enrolled in the courses
	 * of this instructor, excluding TAs and unregistered students.
	 * @return	number of institutions
	 * @throws	DBException	if the database can't be read
	 */
	public int getNumInstitutionsOfAllEnrolledStudents() throws DBException {
		return EnrollmentRW.getNumInstitutions(actedUser.getUserId());
	} // getNumInstitutionsOfAllEnrolledStudents()

	/** Gets all students enrolled in the currently selected course.
	 * @return	array of EnrollmentData of the enrolled students
	 * @throws	DBException	if the database can't be read
	 */
	public EnrollmentData[] getEnrolledStudents() throws DBException {
		if (activeCrs != null && (enrolledUsers == null 
				|| enrollmentPreloadedId != activeCrs.getId())) {
			debugPrint("InstructorSession.getEnrolledStudents: "
					+ "Loading enrollment again........ ");
			refreshEnrolledStudents();
			enrollmentPreloadedId = activeCrs.getId();
		}
		return (enrolledUsers == null ? new EnrollmentData[0]
				: enrolledUsers.toArray(new EnrollmentData[enrolledUsers.size()]));
	} // getEnrolledStudents()

	/** Refreshes the list of students enrolled in the currently selected course.
	 * @throws	DBException	if the database can't be read
	 */
	public void refreshEnrolledStudents() throws DBException {
		if (activeCrs != null) {
			enrolledUsers = EnrollmentRW.getEnrolledStudents(activeCrs.getId(), 
					activeCrs.sortByStudentNum(), actedUser.getLanguages());
		} // if there's an active course
	} // refreshEnrolledStudents()

	/** Gets all students enrolled in a course.
	 * @param	courseId	ID number of a course
	 * @return	array of EnrollmentData of the enrolled students
	 * @throws	InvalidOpException	when no course has been selected
	 * @throws	DBException	if the database can't be read
	 */
	public EnrollmentData[] getEnrolledStudents(int courseId)
			throws InvalidOpException, DBException {
		boolean owned = false;
		for (int crsNum = 0; crsNum < courses.size(); crsNum++) {
			if (courseId == courses.get(crsNum).getId()) {
				owned = true;
				break;
			} // if the selected course is owned by this instructor
		} // for each course owned by this instructor
		if (!owned)
			throw new InvalidOpException("invalid course number");
		final List<EnrollmentData> enrolledStudents =
				EnrollmentRW.getEnrolledStudents(courseId, INCLUDE_TAS, 
					INCLUDE_UNREGISTERED, activeCrs.sortByStudentNum(), 
					actedUser.getLanguages());
		return (enrolledStudents.toArray(new EnrollmentData[enrolledStudents.size()]));
	} // getEnrolledStudents(int)

	/** Gets all student ID numbers of students who are enrolled in 
	 * the currently selected course.
	 * @return	list of student ID numbers of the enrolled students 
	 * @throws	InvalidOpException	when no course has been selected
	 * @throws	DBException	if the database can't be read
	 */
	public List<String> getEnrolledStudentNums()
			throws InvalidOpException, DBException {
		return getEnrolledStudentNums(ANY_INSTITUTION);
	} // getEnrolledStudentNums()

	/** Gets all student ID numbers of students from an institution who are 
	 * enrolled in the currently selected course.
	 * @param	instnId	ID number of school from which to get the enrolled 
	 * students; ANY_INSTITUTION if should ignore school
	 * @return	list of student ID numbers of the enrolled students 
	 * @throws	InvalidOpException	when no course has been selected
	 * @throws	DBException	if the database can't be read
	 */
	public List<String> getEnrolledStudentNums(int instnId)
			throws InvalidOpException, DBException {
		if (activeCrsNum == 0)
		   throw new InvalidOpException("getEnrolled without selection");
		return EnrollmentRW.getEnrolledStudentNums(activeCrs.getId(), instnId);
	} // getEnrolledStudentNums(int)

	/** Gets all students enrolled in the currently selected course who are
	 * registered with ACE.  
	 * @return	array of EnrollmentData of the enrolled students
	 * @throws	InvalidOpException	when no course has been selected
	 * @throws	DBException	if the database can't be read
	 */
	public EnrollmentData[] getRegisteredUsers()
			throws InvalidOpException, DBException {
		return getRegisteredUsers(INCLUDE_TAS);
	} // getRegisteredUsers()

	/** Gets all students enrolled in the currently selected course who are
	 * registered with ACE.  
	 * @param	includeTAs	whether to include this course's TAs in the list
	 * @return	array of EnrollmentData of the enrolled students
	 * @throws	InvalidOpException	when no course has been selected
	 * @throws	DBException	if the database can't be read
	 */
	public EnrollmentData[] getRegisteredUsers(boolean includeTAs)
			throws InvalidOpException, DBException {
		if (activeCrsNum == 0)
			throw new InvalidOpException("getRegistered without selection");
		// load all enrolled users, if not already loaded
		if (enrolledUsers == null ||
				enrollmentPreloadedId != activeCrs.getId()) {
			getEnrolledStudents();
		}	
		// filter out preregistered students
		final List<EnrollmentData> regStuds = new ArrayList<EnrollmentData>();
		for (int studNum = 0; studNum < enrolledUsers.size(); studNum++) {
			final EnrollmentData edata = enrolledUsers.get(studNum);
			if (edata.isRegistered() && (includeTAs || !edata.isTA()))
				regStuds.add(edata);
		}
		return regStuds.toArray(new EnrollmentData[regStuds.size()]);
	} // getRegisteredUsers(boolean)

	/** Gets all students enrolled in a course owned by this instructor who are
	 * registered with ACE.  
	 * @param	courseId	ID number of a course
	 * @return	array of EnrollmentData of the enrolled students
	 * @throws	InvalidOpException	when no course has been selected
	 * @throws	DBException	if the database can't be read
	 */
	public EnrollmentData[] getRegisteredUsers(int courseId)
			throws InvalidOpException, DBException {
		return getRegisteredUsers(courseId, INCLUDE_TAS);
	} // getRegisteredUsers(int)

	/** Gets all students enrolled in a course owned by this instructor who are
	 * registered with ACE.  
	 * @param	courseId	ID number of a course
	 * @param	includeTAs	whether to include this course's TAs in the list
	 * @return	array of EnrollmentData of the enrolled students
	 * @throws	InvalidOpException	when no course has been selected
	 * @throws	DBException	if the database can't be read
	 */
	public EnrollmentData[] getRegisteredUsers(int courseId,
			boolean includeTAs) throws InvalidOpException, DBException {
		boolean owned = false;
		for (int crsNum = 0; crsNum < courses.size(); crsNum++) {
			if (courseId == courses.get(crsNum).getId()) {
				owned = true;
				break;
			} // if the selected course is owned by this instructor
		} // for each course owned by this instructor
		if (!owned)
			throw new InvalidOpException("invalid course number");
		final List<EnrollmentData> regStuds =
				EnrollmentRW.getEnrolledStudents(courseId, includeTAs,
						!INCLUDE_UNREGISTERED, activeCrs.sortByStudentNum(), 
						actedUser.getLanguages());
		return (regStuds.toArray(new EnrollmentData[regStuds.size()]));
	} // getRegisteredUsers(int, boolean)

	/** Enroll a student in the currently selected course.
	 * @param	entry EnrollmentData of the student to enroll
	 * @throws	InvalidOpException	when no course has been selected
	 * @throws	DBException	if the database can't be written to
	 */
	public void enroll(EnrollmentData entry) throws InvalidOpException,
			DBException {
		if (activeCrsNum == 0)
			throw new InvalidOpException("enroll() without course selection");
		final int crsId = activeCrs.getId();
		if (enrolledUsers == null || enrollmentPreloadedId != crsId)
			throw new InvalidOpException(
					"getEnrolledStudents() must be called before enroll()");
		// institution of this EnrollmentData contains only the ID; flesh it out
		final int institnId = entry.getInstitution().getId();
		entry.setInstitution(InstitutionRW.getInstitution(institnId));
		enrolledUsers.addAll(EnrollmentRW.enroll(crsId, entry));
	} // enroll(EnrollmentData)

	/** Enroll students in the currently selected course.  Used for batch
	 * enrollment.
	 * @param	entries EnrollmentData of the students to enroll
	 * @throws	InvalidOpException	when no course has been selected or the list
	 * of already-enrolled or enrolled-but-not-registered
	 * students has not been initialized
	 * @throws	DBException	if the database can't be written to
	 */
	public void enroll(EnrollmentData[] entries)
			throws InvalidOpException, DBException {
		if (activeCrsNum == 0)
			throw new InvalidOpException("enroll() without course selection");
		final int crsId = activeCrs.getId();
		if (enrolledUsers == null || enrollmentPreloadedId != crsId)
			throw new InvalidOpException(
					"getEnrolledStudents() must be called before enroll()");
		final List<EnrollmentData> newEntries = 
				EnrollmentRW.enroll(crsId, entries, actedUser.getInstitution());
		enrolledUsers.addAll(newEntries);
	} // enroll(EnrollmentData[])

	/** Enroll students at this instructor's institution who are registered in
	 * ACE in the currently selected course.
	 * @param	regdStudents	registered students from which enrollees were
	 * chosen
	 * @param	toEnroll	1-based indices of students in regdStudents
	 * @throws	ParameterException	when a student index is out of bounds
	 * @throws	DBException	if the database can't be written to
	 */
	public void enrollRegistered(EnrollmentData[] regdStudents, 
			List<Integer> toEnroll) throws ParameterException, DBException {
		final int numToEnroll = toEnroll.size();
		final String[] studIds = new String[numToEnroll];
		for (int studNum = 0; studNum < numToEnroll; studNum++) {
			final int regdStudNum = toEnroll.get(studNum).intValue(); // 1-based
			if (regdStudNum <= 0 || regdStudNum > regdStudents.length)
				throw new ParameterException(
						"Invalid index in enrollRegistered");
			final EnrollmentData edata = regdStudents[regdStudNum - 1];
			studIds[studNum] = edata.getUserId();
			enrolledUsers.add(edata);
		} // for each student to enroll
		EnrollmentRW.enrollInCourse(studIds, activeCrs.getId());
	} // enrollRegistered(EnrollmentData[], List<Integer>)

	/** Enroll a batch of students in the currently selected course.
	 * @param	toEnroll array of students to enroll
	 * @throws	InvalidOpException	when no course has been selected or the list
	 * of already-enrolled or enrolled-but-not-registered
	 * students has not been initialized
	 * @throws	DBException	if the database can't be written to
	 */
	public void saveBatchEnrollment(EnrollmentData[] toEnroll) 
			throws DBException, InvalidOpException {
		if (!Utils.isEmpty(toEnroll)) enroll(toEnroll);
	} // saveBatchEnrollment(EnrollmentData[])

	/** Disenroll students from the currently selected course.
	 * @param	disenrolleeNumStrs 1-based indices in enrolledUsers of the students
	 * to disenroll
	 * @throws	ParameterException	when student indices are not in ascending
	 * order or one is out of bounds
	 * @throws	InvalidOpException	when no course has been selected or the list
	 * of already-enrolled or enrolled-but-not-registered students has not been 
	 * initialized
	 * @throws	DBException	if the database can't be written to
	 */
	public void disenroll(String[] disenrolleeNumStrs) throws InvalidOpException,
			DBException, ParameterException {
		if (activeCrsNum == 0)
			throw new InvalidOpException("disenroll() without course selection");
		final int crsId = activeCrs.getId();
		if (enrolledUsers == null || enrollmentPreloadedId != crsId)
			throw new InvalidOpException(
				" getEnrolledStudents() must be called before disenroll()");
		// make sure that the disenrolleeNumStrs are sorted in ascending order
		// or enrolledUsers.remove() will not work properly
		final int[] disenrolleeNums = Utils.stringToIntArray(disenrolleeNumStrs, -1);
		int prevDisenrolleeNum = -1;
		for (final int disenrolleeNum : disenrolleeNums) {
			if (disenrolleeNum < 0)
				throw new ParameterException("Nonnumerical input to disenroll()");
			if (disenrolleeNum < prevDisenrolleeNum)
				throw new ParameterException("Unsorted input to disenroll()");
			prevDisenrolleeNum = disenrolleeNum;
		} // for each targeted student
		final List<String> registered = new ArrayList<String>();
		final List<String> unregistered = new ArrayList<String>();
		for (int studNum = disenrolleeNums.length - 1; studNum >= 0; studNum--) {
			final int enrolledUserNum = disenrolleeNums[studNum]; // 1-based
			if (enrolledUserNum <= 0
					|| enrolledUserNum > enrolledUsers.size())
				throw new ParameterException(" invalid index to disenroll() ");
			final EnrollmentData entry = 
					enrolledUsers.remove(enrolledUserNum - 1);
			if (entry.isRegistered()) registered.add(entry.getUserId());
			else unregistered.add(entry.getStudentNum());
		} // for each studNum
		final String[] regd = registered.toArray(new String[registered.size()]);
		final String[] unregd = unregistered.toArray(new String[registered.size()]);
		EnrollmentRW.disenrollRegistered(crsId, regd);
		EnrollmentRW.disenrollUnregistered(crsId, unregd);
	} // disenroll(String[])

	/** Modifies the TA status of students in the currently selected course.
	 * @param	modifyNums 1-based indices in enrolledUsers of the students
	 * to promote or demote
	 * @param	promote	whether to promote or demote them
	 * @throws	ParameterException	when a student index is out of bounds
	 * or a student is not registered
	 * @throws	InvalidOpException	when no course has been selected or the list
	 * of already-enrolled or enrolled-but-not-registered students has not been 
	 * initialized
	 * @throws	DBException	if the database can't be written to
	 */
	public void modifyTAs(int[] modifyNums, boolean promote)
			throws InvalidOpException, DBException, ParameterException {
		final String SELF = "InstructorSession.modifyTAs: ";
		if (activeCrsNum == 0)
			throw new InvalidOpException("modifyTAs without selected course");
		final int crsId = activeCrs.getId();
		if (enrolledUsers == null || enrollmentPreloadedId != crsId)
			throw new InvalidOpException(
				" getEnrolledStudents must be called before modifyTAs");
		final List<String> modifyIds = new ArrayList<String>();
		for (int modifyNum = 0; modifyNum < modifyNums.length; modifyNum++) {
			final int enrolledUserNum = modifyNums[modifyNum]; // 1-based
			if (enrolledUserNum <= 0
					|| enrolledUserNum > enrolledUsers.size())
				throw new ParameterException(" invalid index to modifyTAs ");
			final EnrollmentData entry = enrolledUsers.get(enrolledUserNum - 1);
			if (entry.isRegistered()) {
				modifyIds.add(entry.getUserId());
				entry.setTA(promote);
			} else throw new ParameterException(SELF 
					+ "cannot modify TA status of student " 
					+ enrolledUserNum + " with ID " + entry.getUserId() 
					+ " because s/he's not registered.");
		} // for each modifyNum
		if (!modifyIds.isEmpty()) {
			EnrollmentRW.modifyTAs(crsId, modifyIds, promote);
		} // if there are IDs to modify
	} // modifyTAs(int[], boolean)

/* ***************** Temporary exam ID functions *******************/

	/** Adds temporary student login IDs (for taking exams) to the database
	 * and enrolls them in the current course.
	 * @param	users	information about the exam-students
	 * @throws	DBException	if the database can't be written to
	 */
	public void addExamStudents(User[] users) throws DBException {
		UserWrite.addExamStudents(users, activeCrs.getId());
		resetExamIdsArrays();
	} // addExamStudents(String[], int)

	/** Voids out the record of exam IDs so it has to be reacquired from the
	 * database.
	 */
	public void resetExamIdsArrays() {
		examIds[ALL] = null;
		examIds[UNUSED] = null;
	} // resetExamIdsArrays()

	/** Get the temporary exam IDs associated with the current course.
	 * @return	an array of login IDs
	 */
	public String[] getExamIds() {
		return getExamIds(!UNUSED_ONLY);
	} // getExamIds()

	/** Get the temporary exam IDs associated with the current course.
	 * @param	unused	whether to get only unused IDs (without associated
	 * responses)
	 * @return	an array of login IDs
	 */
	public String[] getExamIds(boolean unused) {
		final int which = (unused ? UNUSED : ALL);
		if (examIds[which] == null) try {
			examIds[which] = UserRead.getExamIds(activeCrs.getId(), unused);
		} catch (DBException e) {
			examIds[which] = new String[0];
		}
		return examIds[which];
	} // getExamIds(boolean)

	/** Get the number of temporary exam IDs associated with the current course.
	 * @return	the number of login IDs
	 */
	public int getNumExamIds() {
		return getNumExamIds(!UNUSED_ONLY);
	} // getExamIds()

	/** Get the number of temporary exam IDs associated with the current course.
	 * @param	unused	whether to count only unused IDs (without associated
	 * responses)
	 * @return	the number of login IDs
	 */
	public int getNumExamIds(boolean unused) {
		final int which = (unused ? UNUSED : ALL);
		if (examIds[which] == null) try {
			examIds[which] = UserRead.getExamIds(activeCrs.getId(), unused);
		} catch (DBException e) {
			examIds[which] = new String[0];
		}
		return examIds[which].length;
	} // getExamIds(boolean)

	/** Deletes a set of users from the database.  Used to remove temporary
	 * exam IDs.
	 * @param	userIds	array of user login IDs to delete
	 * @throws	DBException	if the database can't be written to
	 */
	public void removeUsers(String[] userIds) throws DBException {
		UserWrite.removeUsers(userIds);
		resetExamIdsArrays();
		refreshEnrolledStudents();
	} // removeUsers(String[])

	/** Transfers work that was done in a course under one login ID to a
	 * different login ID, and deletes the first login ID.
	 * @param	students	names, student ID numbers, old login IDs, and new
	 * login IDs of students whose work is to be transferred
	 * @param	OLD_LOGIN	the member of <code>students</code> containing
	 * the old login ID
	 * @param	NEW_LOGIN	the member of <code>students</code> containing
	 * the new login ID
	 * @throws	DBException	if the database can't be written to
	 */
	public void transferWork(List<String[]> students, int OLD_LOGIN,
			int NEW_LOGIN) throws DBException {
		EnrollmentRW.transferWork(activeCrs.getId(), students, 
				OLD_LOGIN, NEW_LOGIN);
		resetExamIdsArrays();
		refreshEnrolledStudents();
	} // transferWork(List<String[]>, int, int)

/* ***************** ACE online textbook functions *******************/

	/** Gets my textbooks.
	 * @param	others	whether to include textbooks written by others that I am
	 * authorized to see
	 * @return	my textbooks
	 * @throws	DBException	if the database can't be read
	 */
	public Textbook[] getTextbooks(boolean others) throws DBException {
		return TextbookRW.getBooks(actedUser.getUserId(), others);
	} // getTextbooks(boolean)

	/** Gets authors, names, ID numbers, and flags, but no chapters or content,
	 * of textbooks of which I am the author or a coauthor but I may not edit.
	 * @return	my textbooks that I may not edit
	 * @throws	DBException	if the database can't be read
	 */
	public Textbook[] getLockedTextbooks() throws DBException {
		return TextbookRW.getLockedBooks(actedUser.getUserId());
	} // getLockedTextbooks()

	/** Gets a textbook, may lock it for editing by me.
	 * @param	bookId	ID number of the book to retrieve
	 * @param	lock	when true, lock the book for editing by me
	 * @return	a textbook
	 * @throws	ParameterException	if userId is null (unlikely)
	 * @throws	DBException	if the database can't be read
	 */
	public Textbook getTextbook(int bookId, boolean lock) 
			throws DBException, ParameterException {
		Textbook book = null;
		try {
			if (lock) {
				TextbookRW.setLockHolder(bookId, actedUser.getUserId());
			} // if should lock the book for editing
			book = TextbookRW.getBook(bookId);
		} catch (ParameterException e) {
			Utils.alwaysPrint("InstructorSession.getTextbook: could "
					+ "not find book with ID ", bookId);
			book = new Textbook(actedUser.getUserId());
		} // try
		return book;
	} // getTextbook(int, boolean)

	/** Releases the lock on a textbook.
	 * @param	bookId	ID number of the book to unlock
	 * @throws	DBException	if the database can't be written to
	 */
	public void releaseLockedTextbook(int bookId) throws DBException {
		TextbookRW.setLockHolder(bookId, null);
	} // releaseLockedTextbook(int)

	/** Deletes a textbook.
	 * @param	bookId	ID number of the book to delete
	 * @throws	DBException	if the database can't be written to
	 */
	public void deleteTextbook(int bookId) throws DBException {
		TextbookRW.deleteBook(bookId);
	} // deleteTextbook(int)

	/** Gets if I can read a question with a particular ID.
	 * @param	qId	unique ID of the question
	 * @return	true if I can read the question
	 * @throws	DBException	if the database can't be read
	 */
	public boolean canReadQuestion(int qId) throws DBException {
		return QuestionRW.getLightQuestion(qId, actedUser.getUserId()) != null;
	} // canReadQuestion(int)

/* ***************** Marvin Live functions *******************/

	/** Assigns a port number and activates a Marvin Live session for the 
	 * active course.
	 * @param	rootURL	the root of all app URLs, e.g., http://epoch.uky.edu. 
	 * Never https (always http), no port numbers.
	 * @param	idleTime	amount of time session is idle before it is turned
	 * off
	 * @return	the port number of the Marvin Live session of the current active
	 * course
	 * @throws	DBException	if the database can't be written to
	 */
	public int createMarvinLivePort(String rootURL, String idleTime) 
			throws DBException {
		final String SELF = "InstructorSession.createMarvinLivePort: ";
		final int crsId = activeCrs.getId();
		int portNumber = 0;
		try {
			final ServerSocket newPort = new ServerSocket(0);
			portNumber = newPort.getLocalPort();
			debugPrint(SELF + "got port ", portNumber, ".");
			final String json = getJson(rootURL, portNumber, idleTime);
			debugPrint(SELF + "json:\n", json);
			final String jsonFileName = writeJsonToFile(json, portNumber);
			debugPrint(SELF + "json file name is ", jsonFileName);
			final ProcessBuilder builder = new ProcessBuilder(
					AppConfig.nodeProgram, 
					"marvinLive", 
					"-c",
					jsonFileName);
			builder.directory(new File(AppConfig.allAppsRoot));
			debugPrint(SELF + "set working directory of process builder.");
			newPort.close(); // keep open as long as possible 
							 // so less chance someone else will grab it
			debugPrint(SELF + "closed the ServerSocket object so MarvinLive "
					+ "can use the port.");
			builder.start();
			debugPrint(SELF + "Marvin Live process started at port ", 
					portNumber, ".");
		} catch (IOException e) {
			Utils.alwaysPrint(SELF + "couldn't get open port for Marvin "
					+ "Live session for course with ID ", crsId, 
					" at port ", portNumber, ", or couldn't create session "
					+ "directory or write configuration file");
			portNumber = 0;
		} // try
		CourseRW.setMarvinLivePort(crsId, portNumber);
		return portNumber;
	} // createMarvinLivePort(String, String)

	/** Terminates the Marvin Live session for the active course and deletes its
	 * directory.
	 * @throws	DBException	if the database can't be written to
	 */
	public void endMarvinLiveSession() throws DBException {
		final String SELF = "InstructorSession.endMarvinLiveSession: ";
		final int crsId = activeCrs.getId();
		final int portNumber = CourseRW.getMarvinLivePort(crsId);
		if (portNumber != 0) try {
			final ProcessBuilder builder = new ProcessBuilder(
					"fuser", 
					"-k",
					Utils.toString(portNumber, "/tcp"));
			final Map<String, String> env = builder.environment();
			env.put("PATH", "/bin:/usr/bin:/usr/local/bin");
			builder.start();
			debugPrint(SELF + "Marvin Live process for course ", crsId,
					" at port ", portNumber, " terminated.");
			getRefToNonexistentMarvinLiveDir(getMarvinLiveDirName(portNumber));
		} catch (IOException e) {
			Utils.alwaysPrint(SELF + "couldn't get open port for Marvin "
					+ " Live session for course with ID ", crsId, 
					", or couldn't create session directory or write "
					+ "configuration file");
		} // try
		CourseRW.setMarvinLivePort(crsId, 0);
	} // endMarvinLiveSession()

	/** Creates the JSON configuration file for Marvin Live.
	 * @param	rootURL	the root of all app URLs, e.g., http://epoch.uky.edu. 
	 * Never https (always http), no port numbers.
	 * @param	portNumber	the port number for the Marvin Live session
	 * @param	idleTime	amount of time session is idle before it is turned
	 * off
	 * @return	the contents of the JSON configuration file
	 */
	private String getJson(String rootURL, int portNumber, String idleTime) {
		StringBuilder jsonBld = Utils.getBuilder(
				"{\n\"converterService\": "
					+ "\"", rootURL, "/webservices/\""
				+ ",\n\"servicesDirectory\": "
					+ "\"", AppConfig.allAppsRoot, "jchemWebservices/\""
				+ ",\n\"port\": ", portNumber, 
				",\n\"license\": "
					+ "\"", AppConfig.appRoot, "nosession/license.cxl\""
				+ ",\n\"themeOverrides\": "
					+ "\"", AppConfig.appRoot, "includes/marvinLive.css\""
				+ ",\n\"secret_key\": \"perhapsnot\""
				+ ",\n\"marvinjs\": { "
					+ "\n\t\"displaySettings\": { "
						+ "\n\t\t\"toolbars\": \"reporting\" "
					+ "\n\t} "
				+ "\n}");
		if (!Utils.isEmptyOrWhitespace(idleTime)) {
			jsonBld = Utils.getBuilder(jsonBld,
				",\n\"deleteUnusedRooms\": \"", idleTime, "\"");
		} // if there is a time limit on session
		jsonBld.append("\n}");
		return jsonBld.toString();
	} // getJson(String, int String)

	/** Writes the given string to a file.
	 * @param	json	the string to write
	 * @param	portNumber	the port number for the Marvin Live session
	 * @return	absolute name of the output file
	 * @throws	IOException if can't write file
	 */
	private String writeJsonToFile(String json, int portNumber) 
			throws IOException {
		final String SELF = "InstructorSession.writeJsonToFile: ";
		final String marvinLiveDirName = makeMarvinLiveDir(portNumber);
		final String jsonFileName = Utils.toString(marvinLiveDirName, 
				'/', "config-json_", portNumber, ".json");
		debugPrint(SELF + "JSON for Marvin Live being written to ", 
				jsonFileName, ":\n", json);
		final FileOutputStream fos = 
				new FileOutputStream(new File(jsonFileName));
		final DataOutputStream dos = new DataOutputStream(fos);
		final FileLock lock = fos.getChannel().lock();
		dos.writeBytes(json);
		lock.release();
		dos.close();
		fos.close();
		return jsonFileName;
	} // writeJsonToFile(String, int)

	/** Creates the directory for the JSON configuration file and the Marvin
	 * Live database.
	 * @param	portNumber	the port number for the Marvin Live session
	 * @return	the name of the directory
	 * @throws	IOException	if can't make directory or erase previously existing
	 * directory
	 */
	private String makeMarvinLiveDir(int portNumber) throws IOException {
		final String SELF = "InstructorSession.makeMarvinLiveDir: ";
		final String marvinLiveDirName = getMarvinLiveDirName(portNumber);
		final File marvinLiveDir = 
				getRefToNonexistentMarvinLiveDir(marvinLiveDirName);
		debugPrint(SELF + "making Marvin Live directory ", marvinLiveDirName);
		if (!marvinLiveDir.mkdir()) {
			Utils.alwaysPrint(SELF + "failed to make Marvin Live directory ",
				marvinLiveDirName);
		}
		return marvinLiveDirName;
	} // makeMarvinLiveDir(int)

/* ***************** miscellaneous functions *******************/

	/** Deletes an English phrase and all its translations from the database.
	 * @param	phraseId	ID of the phrase to delete
	 * @throws	DBException	if the database can't be written to
	 */
	public static void deleteEnglishPhrase(int phraseId) throws DBException {
		TranslnWrite.deleteEnglish(phraseId);
	} // deleteEnglishPhrase(int)

	/** Copies topics and posts from "Tutorial course" into the forum of a new
	 * course.
	 * @param	newCourseId	ID of the new course 
	 * @throws	DBException	if the database can't be written to
	 */
	public static void initializeForum(int newCourseId) throws DBException {
		final String SELF = "InstructorSession.initializeForum: ";
		final ForumTopic[] origTopics = 
				ForumRW.getTopics(AppConfig.tutorialId);
		debugPrint(SELF + "got ", origTopics.length, " topic(s) from "
				+ "tutorial course with ID ", AppConfig.tutorialId, 
				"; copying to course with ID ", newCourseId);
		for (final ForumTopic topic : origTopics) {
			// alter topic from tutorial course to belong to new course
			final int origTopicId = topic.getId();
			topic.setCourseId(newCourseId);
			ForumRW.addTopic(topic); // sets topic's new topic ID
			final int newTopicId = topic.getId();
			// get posts belonging to topic with original ID
			final ForumPost[] origPosts = ForumRW.getPosts(origTopicId);
			debugPrint(SELF + "got ", origPosts.length, " post(s) "
					+ "belonging to topic with original ID ", origTopicId,
					"; assigning them to topic with new ID ", newTopicId);
			for (final ForumPost post : origPosts) {
				// set post to belong to topic with new ID
				post.setTopicId(newTopicId);
				ForumRW.addPost(post);
			} // for each post in the topic
		} // for each topic
	} // initializeForum(int)

	/** Gets a template list of assignments.
	 * @param	id	the ID number of the template
	 * @return	the assignment XML
	 * @throws	DBException	if the database can't be read
	 */
	public String getAssgtsTemplate(int id) throws DBException {
		return HWRead.getAssgtsTemplate(id);
	} // getAssgtsTemplate()

	/** Gets lists of assignments intended to be used as templates.
	 * @return	array of 3-membered arrays of assignment id, name, and XML
	 * @throws	DBException	if the database can't be read
	 */
	public String[][] getAssgtsTemplates() throws DBException {
		return HWRead.getAssgtsTemplates();
	} // getAssgtsTemplates()

} // InstructorSession
