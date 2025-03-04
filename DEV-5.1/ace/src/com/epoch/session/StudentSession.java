package com.epoch.session;

import com.epoch.AppConfig;
import com.epoch.assgts.Assgt;
import com.epoch.courseware.Course;
import com.epoch.courseware.EnrollmentData;
import com.epoch.courseware.ForumPost;
import com.epoch.courseware.ForumTopic;
import com.epoch.courseware.User;
import com.epoch.db.CourseRW;
import com.epoch.db.EnrollmentRW;
import com.epoch.db.ForumRW;
import com.epoch.db.HWRead;
import com.epoch.db.HWWrite;
import com.epoch.db.ResponseRead;
import com.epoch.db.TextbookRW;
import com.epoch.db.UserRead;
import com.epoch.db.UserWrite;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.InvalidOpException;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.StudentEmailExistsException;
import com.epoch.exceptions.StudentNumConstraintException;
import com.epoch.exceptions.UniquenessException;
import com.epoch.textbooks.Textbook;
import com.epoch.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;

/** A student's work session. */
public class StudentSession extends UserSession {

	/* Use UserSession debugPrint(). */
	
	/** Courses in which this user is enrolled (student) or owns (instructor). */
	transient protected List<Course> courses;
	/** Assignments in the currently selected course.  The selected course is a
	 * field in the superclass UserSession. */
	transient protected List<Assgt> assgts;
	/** Tutorial assignments. */
	transient protected List<Assgt> tutorials = null;
	/** The currently selected course. */
	transient protected Course activeCrs;
	/** Serial number of the currently selected course. */
	transient protected int activeCrsNum;
	/** The current student (or instructor); could actually be an administrator
	 * impersonating this person. */
	transient protected User actedUser;
	/** List of courses for which this student is a TA. */
	transient private List<Integer> TAForCourseIds = new ArrayList<Integer>();
	/** Value for parameter of sendPostAlert(). */
	final protected static int ADDED = 0;
	/** Value for parameter of sendPostAlert(). */
	final protected static int EDITED = 1;
	/** Value for parameter of sendPostAlert(). */
	final protected static int DELETED = 2;

	/** Constructor.
	 * @param	student1	the student for whom to create this session
	 * @throws	DBException	if there's a problem reading the database
	 */
	public StudentSession(User student1) throws DBException {
		super(student1);
		initialiseStud(student1);
	} // StudentSession(User)
	
	/** Required by InstructorSession(int, User).  */
	StudentSession() { 
		// empty
	}

	/** Initialize this student.  May be called by constructor or from
	 * AdminSession via InstructorSession (for impersonation).
	 * @param	student1	the student for whom to create this session
	 * @throws	DBException	if there's a problem reading the database
	 */
	final public void initialiseStud(User student1) throws DBException {
		debugPrint("StudentSession.initialiseStud: "
				+ "user to be initialized has ID ", student1.getUserId(),
				"; AppConfig.tutorialId = ", AppConfig.tutorialId);
		actedUser = student1;
		refreshCourses();
	} // initialiseStud(User)

/* ******************* Methods shared with instructors ******************/

	/** Gets information about a course.
	 * @param	courseId	unique ID of the course
	 * @return	information about the course
	 * @throws	DBException	if there's a problem reading the database
	 */
	public Course getCourseInfo(int courseId) throws DBException {
		return CourseRW.getCourseInfo(courseId);
	} // getCourseInfo(int)
	
	/** Gets information about all courses in which this user is enrolled
	 * (student) or that this user owns (instructor).
	 * @return	array of information about the courses
	 */
	public Course[] getCourses() {
		return courses.toArray(new Course[courses.size()]);
	} // getCourses()

	/** Gets the allowed IP addresses for each course that this user can enter.
	 * @return	map of arrays of allowed IP addresses keyed by this user's 
	 * courses
	 * @throws	DBException	if there's a problem reading the database
	 */
	public Map<Integer, String[]> getCoursesAllowedIPAddrs() throws DBException {
		return CourseRW.getAllowedIPAddresses(
				actedUser.getUserId(), actedUser.getRole());
	} // getCoursesAllowedIPAddrs()

	/** Gets number of courses in which this user is enrolled
	 * (student) or that this user owns (instructor).
	 * @return	number of courses
	 */
	public int getNumCourses() {
		return courses.size();
	} // getNumCourses()

	/** Gets information about a course in which this user is enrolled
	 * (student) or that this user owns (instructor).
	 * @param	crsNum	1-based serial number of the course
	 * @return	information about the course
	 * @throws	ParameterException	if crsNum is out of bounds
	 */
	public Course getCourse(int crsNum) throws ParameterException {
		if (crsNum <= 0 || crsNum > courses.size())
			throw new ParameterException(" Invalid index to getCourse ");
		return courses.get(crsNum - 1);
	} // getCourse(int)

	/** Gets information about the currently selected course.
	 * @return	information about the course
	 * @throws	InvalidOpException	when no course has been selected
	 */
	public Course getSelectedCourse() throws InvalidOpException {
		if (activeCrsNum == 0)
			throw new InvalidOpException(" no course selected ");
		return activeCrs;
	} // getSelectedCourse()

	/** Gets the 1-based serial number of the current course.
	 * @return	1-based serial number of the course
	 */
	public int getSelectedCourseNum() {
		return activeCrsNum;
	} // getSelectedCourseNum()

	/** Gets the ID number of the currently selected course.
	 * @return	ID number of the course
	 * @throws	InvalidOpException	if no course has been selected
	 */
	public int getSelectedCourseId() throws InvalidOpException {
		if (activeCrsNum == 0)
			throw new InvalidOpException(" no course selected ");
		return activeCrs.getId();
	} // getSelectedCourseId()

	/** Gets coinstructors of a course. 
	 * @param	courseId	unique ID of a course
	 * @return	coinstructors of the course
	 * @throws	DBException	if there's a problem reading the database
	 */
	public User[] getCoinstructors(int courseId) throws DBException {
		return CourseRW.getCoinstructors(courseId);
	} // getCoinstructors(int)

	/** Get all verified instructors at an institution who are not coinstructors
	 * of a particular course.
	 * @param	instnId	ID number of the institution
	 * @param	crsId	unique ID number of the coinstructed course
	 * @return	an array of verified instructor Users
	 * @throws	DBException	if there's a problem reading the database
	 */
	public User[] getNoncoinstructors(int instnId, int crsId) 
			throws DBException {
		return CourseRW.getNoncoinstructors(instnId, crsId);
	} // getNoncoinstructors(int, int)

	/** Gets all visible assignments in the currently selected course.
	 * @return	array of assignments
	 * @throws	InvalidOpException	when no course has been selected
	 */
	public Assgt[] getHWs() throws InvalidOpException {
		// debugPrint("getHWs: starting");
		if (activeCrsNum == 0)
			throw new InvalidOpException("getHWs without selection");
		return assgts.toArray(new Assgt[assgts.size()]);
	} // getHWs()

	/** Gets all assignments in a course in which this user is enrolled
	 * (student) or that this user owns (instructor).
	 * Called by course/cloneHWSets.jsp.
	 * @param	crsNum	1-based serial number of the course
	 * @return	array of assignments
	 * @throws	ParameterException	if crsNum is out of bounds
	 * @throws	DBException	if there's a problem reading the database
	 */
	public Assgt[] getHWs(int crsNum) throws ParameterException,
			DBException {
		final String SELF = "StudentSession.getHWs: ";
		if (crsNum <= 0 || crsNum > courses.size())
			throw new ParameterException("Invalid index to getHWs");
		final Course crs = courses.get(crsNum - 1);
		debugPrint(SELF, "course #", crsNum, " with ID ",
				crs.getId(), ", getting assgts.");
		final List<Assgt> hws = HWRead.getHWs(crs.getId());
		debugPrint(SELF, hws.size(), " assgts loaded.");
		if (crs.getId() == AppConfig.tutorialId 
				&& actedUser.getRole() == User.STUDENT) {
			winnowTutorials(hws);
			debugPrint(SELF + "after winnowing current tutorials course "
					+ "assgts, ", hws.size(), " assgts remain.");
		} // if the current course is the tutorial course
		return hws.toArray(new Assgt[hws.size()]);
	} // getHWs(int)

	/** Gets all tutorial assignments.
	 * @return	array of tutorial assignments
	 * @throws	DBException	if there's a problem reading the database
	 */
	public Assgt[] getTutorials() throws DBException {
		return getTutorials(actedUser.getRole());
	} // getTutorials()

	/** Gets all tutorial assignments.
	 * @param	role	the user's role; may be TA
	 * @return	array of tutorial assignments
	 * @throws	DBException	if there's a problem reading the database
	 */
	public Assgt[] getTutorials(char role) throws DBException {
		final String SELF = "StudentSession.getTutorials: ";
		debugPrint(SELF + "role = ", role);
		if (tutorials == null) {
			tutorials = HWRead.getHWs(AppConfig.tutorialId);
			debugPrint(SELF, tutorials.size(), " tutorials loaded.");
			if (role == User.STUDENT) {
				winnowTutorials();
				debugPrint(SELF + "after winnowing, ", tutorials.size(), 
						" tutorials.");
			} 
		} // if tutorials have already been loaded
		return tutorials.toArray(new Assgt[tutorials.size()]);
	} // getTutorials(char)

	/** Removes tutorials from this student's list of tutorial assignments 
	 * that are not relevant to this MarvinSketch or MarvinJS user.
	 */
	public void winnowTutorials() {
		winnowTutorials(tutorials);
	} // winnowTutorials()

	/** Removes tutorials from an array of tutorial assignments that are not
	 * relevant to a MarvinSketch or MarvinJS user.
	 * @param	hws	the tutorial assignments
	 */
	private void winnowTutorials(List<Assgt> hws) {
		winnowTutorials(hws, !actedUser.prefersJava());
	} // winnowTutorials(List<Assgt>)

	/** Removes tutorials from an array of tutorial assignments that are not
	 * relevant to this MarvinSketch or MarvinJS user.
	 * @param	allTutorials	an array of tutorial assignments
	 * @return	a new array of tutorial assignments
	 */
	public Assgt[] winnowTutorials(Assgt[] allTutorials) {
		final List<Assgt> allTuts = 
				new ArrayList<Assgt>(Arrays.asList(allTutorials));
		winnowTutorials(allTuts, !actedUser.prefersJava());
		return allTuts.toArray(new Assgt[allTuts.size()]);
	} // winnowTutorials(Assgt[])

	/** Removes tutorials from an array of tutorial assignments that are not
	 * relevant to a MarvinSketch or MarvinJS user.
	 * @param	allTuts	the tutorial assignments
	 * @param	usesMarvinJS	whether the user prefers to use MarvinJS
	 */
	private static void winnowTutorials(List<Assgt> allTuts, 
			boolean usesMarvinJS) {
		final int numTuts = allTuts.size();
		for (int tutNum = numTuts - 1; tutNum >= 0; tutNum--) {
			final Assgt assgt = allTuts.get(tutNum);
			if ((assgt.getName().indexOf("MarvinSketch") >= 0 
						&& usesMarvinJS) 
					|| (assgt.getName().indexOf("MarvinJS") >= 0 
						&& !usesMarvinJS)) {
				allTuts.remove(tutNum);
			} // if assignment name and usesMarvinJS
		} // for each assignment
	} // winnowTutorials(List<Assgt>, boolean)

	/** Gets an assignment from the currently selected course.
	 * @param	assgtNum	1-based serial number of the assignment (among
	 * visible assignments)
	 * @return	the assignment
	 * @throws	ParameterException	if assgtNum is out of bounds
	 * @throws	InvalidOpException	when no course has been selected
	 */
	public Assgt getHW(int assgtNum) throws ParameterException,
			InvalidOpException {
		if (activeCrsNum == 0)
			throw new InvalidOpException("getHW() without selected course");
		if (assgtNum <= 0 || assgtNum > assgts.size())
			throw new ParameterException("Invalid index " + assgtNum
					+ " to getHW()");
		return assgts.get(assgtNum - 1);
	} // getHW(int)

	/** Gets students enrolled in the current course of this TA, in alphabetical 
	 * order by last name and surname, excluding TAs and unregistered students.
	 * @return	array of EnrollmentData
	 * @throws	DBException	if there's a problem reading the database
	 */
	public EnrollmentData[] getEnrolledStudents() throws DBException {
		final List<EnrollmentData> studentsList = 
				EnrollmentRW.getEnrolledStudents(activeCrs.getId(), 
					activeCrs.sortByStudentNum(), actedUser.getLanguages());
		return studentsList.toArray(new EnrollmentData[studentsList.size()]);
	} // getEnrolledStudents()

	/** Gets students enrolled in all courses of this TA, in alphabetical 
	 * order by last name and surname, excluding TAs and unregistered students.
	 * @return	array of EnrollmentData
	 * @throws	DBException	if there's a problem reading the database
	 */
	public EnrollmentData[] getAllEnrolledStudents() throws DBException {
		final List<EnrollmentData> studentsList =
				EnrollmentRW.getEnrolledStudents(
					TAForCourseIds, actedUser.getLanguages());
		return studentsList.toArray(new EnrollmentData[studentsList.size()]);
	} // getAllEnrolledStudents()

	/** Gets the student's enrollment data.
	 * @return	array of EnrollmentData
	 * @throws	DBException	if there's a problem reading the database
	 */
	public EnrollmentData[] getMyEnrollmentData() throws DBException {
		final List<EnrollmentData> studentsList =
				EnrollmentRW.getEnrolledStudent(actedUser.getUserId());
		return studentsList.toArray(new EnrollmentData[studentsList.size()]);
	} // getMyEnrollmentData()

	/** Gets the number of institutions of all students enrolled in the courses
	 * of this TA, excluding TAs and unregistered students.
	 * @return	number of institutions
	 * @throws	DBException	if there's a problem reading the database
	 */
	public int getNumInstitutionsOfAllEnrolledStudents() throws DBException {
		return EnrollmentRW.getNumInstitutions(TAForCourseIds);
	} // getNumInstitutionsOfAllEnrolledStudents()

	/** Gets students' results from the selected courses, in which the question
	 * statement or keywords contain an expression.
	 * @param	studentIds	login IDs of students whose results should be
	 * retrieved
	 * @param	courseIds	ID numbers of courses of which results should be
	 * retrieved
	 * @param	searchExp	word fragment, word, phrase, simple boolean
	 * expression, or regular expression for which to search question statements
	 * and keywords
	 * @return	results of evaluation of responses
	 * @throws	DBException	if there's a problem reading the database
	 */
	public CrossCourseReport getCrossCourseReport(String[] studentIds, 
			int[] courseIds, String searchExp) throws DBException {
		return ResponseRead.getCrossCourseReport(studentIds, courseIds, 
				searchExp);
	} // getCrossCourseReport(String[], int[], String)

/* ***************** Marvin Live functions *******************/

	/** Gets the port number of the Marvin Live session associated with the 
	 * active course, checks if it's live.
	 * @return	the port number of the Marvin Live session of the active course, 
	 * or 0 if the stored port is no longer active
	 * @throws	DBException	if there's a problem reading the database
	 */
	public int getMarvinLivePort() throws DBException {
		final String SELF = "StudentSession.getMarvinLivePort: ";
		final int crsId = activeCrs.getId();
		int port = CourseRW.getMarvinLivePort(crsId);
		if (port != 0) try {
			final ServerSocket s = new ServerSocket(port);
			// no exception; port is free
			debugPrint(SELF + "stored port ", port, " of course with ID ", 
					crsId, " is not longer open; changing to 0");
			s.close();
			getRefToNonexistentMarvinLiveDir(getMarvinLiveDirName(port));
			port = 0;
			CourseRW.setMarvinLivePort(crsId, port);
		} catch (IOException e) {
			// port is active
			debugPrint(SELF + "IOException");
		} // try
		debugPrint(SELF + "port of course with ID ", crsId, ": ", port);
		return port;
	} // getMarvinLivePort()

	/** Gets the name of the directory for the JSON configuration file and the 
	 * Marvin Live database.
	 * @param	portNumber	the port number for the Marvin Live session
	 * @return	the name of the directory
	 */
	protected static String getMarvinLiveDirName(int portNumber) {
		return Utils.toString(
				AppConfig.appRoot, AppConfig.relTempDir,
				"marvinLiveSession", portNumber);
	} // getMarvinLiveDirName(int)

	/** Deletes the existing directory of the Marvin Live session.
	 * @param	marvinLiveDirName	the name of the directory
	 * @return	the directory
	 * @throws	IOException	if can't erase previously existing directory
	 */
	protected static File getRefToNonexistentMarvinLiveDir(
			String marvinLiveDirName) throws IOException {
		final String SELF = 
				"InstructorSession.getRefToNonexistentMarvinLiveDir: ";
		debugPrint(SELF + "seeing if file ", marvinLiveDirName, " exists");
		final File marvinLiveDir = new File(marvinLiveDirName);
		if (marvinLiveDir.exists()) {
			Utils.alwaysPrint(SELF + "deleting already existing "
					+ "Marvin Live directory ", marvinLiveDirName);
			FileUtils.deleteDirectory(marvinLiveDir);
		} // if directory already exists
		return marvinLiveDir;
	} // getRefToNonexistentMarvinLiveDir(String)

/* ***************** Forum functions *******************/

	/** Gets the number of topics in the forum of the current course.
	 * @return	number of topics in the forum of the current course
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	InvalidOpException	if no course has been selected
	 */
	public int getNumForumTopics() throws DBException, InvalidOpException {
		return ForumRW.getNumTopics(getSelectedCourseId());
	} // getNumForumTopics()

	/** Gets the topics in the forum of the current course.
	 * @param	groupOf50	1-based group of 50 topics to get
	 * @return	array of topics in the forum of the current course
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	InvalidOpException	if no course has been selected
	 */
	public ForumTopic[] getForumTopics(int groupOf50) 
			throws DBException, InvalidOpException {
		final int[] range = 
				new int[] {groupOf50 * 50 - 49, groupOf50 * 50};
		return ForumRW.getTopics(getSelectedCourseId(), range);
	} // getForumTopics(int)

	/** Gets the topics in the forum of the current course.
	 * @return	array of topics in the forum of the current course
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	InvalidOpException	if no course has been selected
	 */
	public ForumTopic[] getForumTopics() 
			throws DBException, InvalidOpException {
		final int[] range = null;
		return ForumRW.getTopics(getSelectedCourseId(), range);
	} // getForumTopics()

	/** Gets a topic of the current course.
	 * @param	topicId	unique ID of the forum topic
	 * @return	the topic
	 * @throws	DBException	if there's a problem reading the database
	 */
	public ForumTopic getForumTopic(int topicId) throws DBException {
		return ForumRW.getTopic(topicId);
	} // getForumTopic(int)

	/** Gets all the posts in the given forum topic.
	 * @param	topicId	unique ID of the forum topic
	 * @return	array of posts in the given forum topic
	 * @throws	DBException	if there's a problem reading the database
	 */
	public ForumPost[] getForumPosts(int topicId) throws DBException {
		return ForumRW.getPosts(topicId);
	} // getForumPosts(int)

	/** Gets all the authors of posts in a given forum topic.
	 * @param	topicId	unique ID of the forum topic
	 * @return	array of Users who authored posts in the given forum topic
	 * @throws	DBException	if there's a problem reading the database
	 */
	public User[] getForumTopicAuthors(int topicId) throws DBException {
		return ForumRW.getTopicAuthors(topicId);
	} // getForumTopicAuthors(int)

	/** Gets a group of 50 or fewer posts in the given forum topic.
	 * @param	topicId	unique ID of the forum topic
	 * @param	groupOf50	1-based group of 50 posts to get
	 * @return	array of posts in the given forum topic
	 * @throws	DBException	if there's a problem reading the database
	 */
	public ForumPost[] getForumPosts(int topicId, int groupOf50) 
			throws DBException {
		final int[] range = 
				new int[] { groupOf50 * 50 - 49, groupOf50 * 50};
		return ForumRW.getPosts(topicId, range);
	} // getForumPosts(int, int)

	/** Gets all posts within topics that contain an expression.
	 * @param	searchExp	word fragment, word, phrase, simple boolean
	 * expression, or regular expression for which to search forum posts
	 * @return	linked map of posts that contain the expression, keyed by
	 * topic; topics will be retrieved in reverse chronological order of 
	 * their most recent posts
	 * @throws	DBException	if there's a problem reading the database
	 */
	public Map<ForumTopic, ArrayList<ForumPost>> getForumPosts(String searchExp) 
			throws DBException {
		return ForumRW.getPosts(activeCrs.getId(), searchExp);
	} // getForumPosts(String)

	/** Gets a forum post.
	 * @param	postId	unique ID of the forum post
	 * @return	the post
	 * @throws	DBException	if there's a problem reading the database
	 */
	public ForumPost getForumPost(int postId) throws DBException {
		return ForumRW.getPost(postId);
	} // getForumPost(int)

	/** Adds a new topic to a course's forum. 
	 * @param	topic	the topic to add to the forum
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void addForumTopic(ForumTopic topic) throws DBException {
		ForumRW.addTopic(topic);
	} // addForumTopic(ForumTopic)

	/** Adds a new post to a course's forum. 
	 * @param	post	the post to add to the forum
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void addForumPost(ForumPost post) throws DBException {
		ForumRW.addPost(post);
		sendPostAlert(post, ADDED);
	} // addForumPost(ForumPost)

	/** Changes the text and figure of a post that this user authored. 
	 * @param	post	the new post
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void editForumPost(ForumPost post) throws DBException {
		final String SELF = "StudentSession.editForumPost: ";
		final ForumPost origPost = ForumRW.getPost(post.getId());
		if (origPost != null) {
			final String actedUserId = actedUser.getUserId();
			final String origPostAuthorId = origPost.getAuthorId();
			if (actedUserId.equals(origPostAuthorId)) {
				ForumRW.editPost(post);
			} else Utils.alwaysPrint(SELF + "cannot edit post ", post.getId(), 
					" because original post writer ", origPostAuthorId, 
					" is not current user ", actedUserId);
		} else addForumPost(post); 
		sendPostAlert(post, origPost != null ? EDITED : ADDED);
	} // editForumPost(ForumPost)

	/** Sends an email to users who are watching a forum topic that a post has
	 * been made or edited.
	 * @param	post	the post
	 * @param	action	whether the post has been added, edited, or deleted
	 * @throws	DBException	if there's a problem reading the database
	 */
	protected void sendPostAlert(ForumPost post, int action) 
			throws DBException {
		final String SELF = "StudentSession.sendPostAlert: ";
		final ForumTopic topic = ForumRW.getTopicOfPost(post.getId());
		final String msg = 
				Utils.toString(actedUser.getName(), " has ", 
					action == EDITED ? "edited a post at" 
					: action == ADDED ? "added a post to" 
					: "deleted a post at", 
					" course ", activeCrs.getName(),
					" forum topic: ", topic.getTitle())
				.replaceAll("'", "\\'");
		final String[] emails = ForumRW.getWatcherTextMessageEmails(
				topic.getId(), post.getAuthorId());
		debugPrint(SELF + "sending message \"", msg, "\" to: ", emails);
		sendTextMessages(emails, msg);
	} // sendPostAlert(ForumPost, int)

	/** Sets a topic to be watched or no longer to be watched by a student.
	 * @param	topicId	the ID of the topic to be watched or no longer watched
	 * @param	watch	whether to start or stop watching the topic
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setWatched(int topicId, boolean watch) throws DBException {
		ForumRW.setWatched(actedUser.getUserId(), topicId, watch);
	} // setWatched(int, boolean)

	/** Finds out which topics in an array are being watched by this student, and 
	 * sets a flag in each one to indicate whether it is.
	 * @param	topics	array of topics
	 * @throws	DBException	if there's a problem reading from the database
	 */
	public void getWatched(ForumTopic[] topics) throws DBException {
		ForumRW.getWatched(actedUser.getUserId(), topics);
	} // getWatched(ForumTopic[])

	/** Deletes a post from a course's forum that this user authored and that is
	 * the most recent post in the topic. 
	 * @param	postId	the post to delete
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	ParameterException	if post can't be deleted for various reasons
	 */
	public void deleteForumPost(int postId) 
			throws DBException, ParameterException {
		final String SELF = "StudentSession.deleteForumPost: ";
		final ForumPost post = ForumRW.getPost(postId);
		String failureStr = null;
		if (post != null) {
			final String actedUserId = actedUser.getUserId();
			final String postAuthorId = post.getAuthorId();
			if (actedUserId.equals(postAuthorId)) {
				final ForumPost latestPost = 
						ForumRW.getLatestPost(post.getTopicId());
				final int latestPostId = latestPost.getId();
				if (latestPostId <= postId) {
					sendPostAlert(post, DELETED);
					ForumRW.deletePost(postId);
				} else failureStr = Utils.toString(SELF, "cannot delete post ",
						postId, " because there is a more recent post ", 
						latestPostId);
			} else failureStr = Utils.toString(SELF, "cannot delete post ",
					postId, " because post writer ", postAuthorId, 
					" is not current user ", actedUserId);
		} else failureStr = Utils.toString(SELF, "cannot delete post ", postId, 
				" because it does not exist.");
		if (failureStr != null) {
			throw new ParameterException(failureStr);
		} // if an error occurred
	} // deleteForumPost(int)

	/** Gets the assignment and question numbers corresponding to the
	 * assignment and question ID numbers for this student.
	 * @param	linkedAssgtQIds	the assignment and question ID numbers
	 * @return	assignment and question numbers
	 * @throws	DBException	if there's a problem reading the database
	 */
	public int[] getAssgtAndQNum(int[] linkedAssgtQIds) throws DBException {
		final String SELF = "StudentSession.getAssgtAndQNum: ";
		final int linkedHWId = linkedAssgtQIds[0];
		final int linkedQId = linkedAssgtQIds[1];
		final int[] assgtAndQNum = new int[2];
		final int[] assignedQIds = getAssignedQIds(linkedHWId);
		if (!Utils.isEmpty(assignedQIds)) {
			assgtAndQNum[0] = Utils.indexOf(getHWIds(), linkedHWId) + 1;
			assgtAndQNum[1] = Utils.indexOf(assignedQIds, linkedQId) + 1;
		} // if the assignment and question are found
		debugPrint(SELF + "linkedAssgtQIds = ", linkedAssgtQIds,
				", assignedQIds = ", assignedQIds,
				", assgtAndQNum = ", assgtAndQNum);
		return assgtAndQNum;
	} // getAssgtAndQNum(int[])

/* ***************** Student-only methods ******************/

	/** Reloads the courses in which this student is enrolled and the courses
	 * for which this student is a TA. 
	 * @throws	DBException	if there's a problem reading the database
	 */
	final public void refreshCourses() throws DBException {
		final String actedUserId = actedUser.getUserId();
		debugPrint("StudentSession.refreshCourses: "
				+ "getting courses for ", actedUserId);
		courses = CourseRW.getCoursesEnrolled(actedUserId);
		TAForCourseIds = EnrollmentRW.getTAForCourseIds(actedUserId);
	} // refreshCourses()
	
	/** Sets the current course and (visible) assignments.
	 * @param	crsNum	1-based serial number of the course
	 * @return	OK, DISABLED if course is disabled
	 * @throws	ParameterException	if crsNum is out of bounds
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public int selectCourse(int crsNum) throws DBException, ParameterException {
		final String SELF = "StudentSession.selectCourse: ";
		debugPrint(SELF + "crsNum = ", crsNum);
		if (crsNum > courses.size() || crsNum <= 0)
			throw new ParameterException("Invalid index " + crsNum
					+ " to selectCourse()");
		activeCrsNum = crsNum;
		activeCrs = courses.get(activeCrsNum - 1);
		final int activeCrsId = activeCrs.getId();
		final boolean isTA = isTA(activeCrsId);
		debugPrint(SELF + "activeCrsId = ", activeCrsId, ", isTA = ", isTA);
		if (!activeCrs.isEnabled() && !isTA) return DISABLED;
		if (activeCrs.isExam()) {
			EnrollmentRW.setFirstEntry(activeCrsId, actedUser.getUserId());
		} // if course is an exam
		refreshAssgts(isTA);
		return OK;
	} // selectCourse(int)

	/** Sets the current course and (visible) assignments.
	 * @param	crsId	ID number of the course
	 * @return	OK, DISABLED if course is disabled
	 * @throws	ParameterException	if the course is not found
	 * @throws	DBException	if there's a problem reading the database
	 */
	public int selectCourseById(int crsId) throws DBException, ParameterException {
		final String SELF = "StudentSession.selectCourseById: ";
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

	/** Sets the current assignments.
	 * @param	showInvisible	whether to show invisible assignments (true
	 * for TAs and instructors)
	 * @throws	DBException	if there's a problem reading the database
	 */
	public void refreshAssgts(boolean showInvisible) throws DBException {
		final String SELF = "StudentSession.refreshAssgts: ";
		final int activeCrsId = activeCrs.getId();
		debugPrint(SELF + "activeCrsId = ", activeCrsId, 
				", showInvisible = " + showInvisible);
		assgts = HWRead.getHWs(activeCrsId);
		if (!showInvisible) {
			// search through docs to remove invisible docs
			for (int hwNum = assgts.size(); hwNum > 0; hwNum--) {
				final Assgt assgt = assgts.get(hwNum - 1);
				final boolean isVisible = assgt.isVisible();
				debugPrint(SELF + "assgt ", hwNum, " ", assgt.getName(), 
						" has visibility ", (isVisible ? "on." : "off."));
				if (!isVisible) {
					debugPrint(SELF + "removing assgt ", hwNum, " from list.");
					assgts.remove(hwNum - 1);
				} // if the assignment is invisible
			} // for each assignment
		} // if don't showInvisible
		if (activeCrsId == AppConfig.tutorialId 
				&& actedUser.getRole() == User.STUDENT) {
			winnowTutorials(assgts);
			debugPrint(SELF + "after winnowing current tutorials course "
					+ "assgts, ", assgts.size(), " assgts remain.");
		} // if the current course is the tutorial course
	} // refreshAssgts(boolean)

	/** Get my school student ID number.
	 * @return	student ID number
	 * @throws	DBException	if there's a problem reading the database
	 */
	public String getStudentNum() throws DBException {
		return actedUser.getStudentNum();
	} // getStudentNum()

	/** Set my school student ID number.
	 * @param	studentNum	student ID number
	 * @throws	UniquenessException	when more than one user has this user's
	 * login ID
	 * @throws	StudentNumConstraintException	when another student at the same
	 * school as this user already has that student ID number
	 * @throws	StudentEmailExistsException	when another student at the same
	 * school as this user already has that email
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setStudentNum(String studentNum) throws DBException,
			UniquenessException, StudentNumConstraintException,
			StudentEmailExistsException {
		setStudentNum(studentNum, STORE);
	} // setStudentNum(String)
	
	/** Set my school student ID number.
	 * @param	studentNum	student ID number
	 * @param	store	whether to store the new number in the database
	 * @throws	UniquenessException	when more than one user has this user's
	 * login ID
	 * @throws	StudentNumConstraintException	when another student at the same
	 * school as this user already has that student ID number
	 * @throws	StudentEmailExistsException	when another student at the same
	 * school as this user already has that email
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setStudentNum(String studentNum, boolean store)
			throws DBException, UniquenessException,
			StudentNumConstraintException, StudentEmailExistsException {
		actedUser.setStudentNum(studentNum);
		if (store) UserWrite.setUser(actedUser);
	} // setStudentNum(String. boolean)
	
	/** Gets whether I am enrolled in a course.
	 * @param	courseId	unique ID of the course
	 * @return	true if am enrolled in the course
	 */
	public boolean isEnrolled(int courseId) {
		for (int crsNum = 0; crsNum < courses.size(); crsNum++)
			if (courses.get(crsNum).getId() == courseId)
				return true;
		return false;
	} // isEnrolled(int)

	/** Gets whether I am a TA for the currently selected course.
	 * @return	true if am a TA for the currently selected course
	 */
	public boolean isTA() {
		final int activeCrsId = activeCrs.getId();
		debugPrint("StudentSession.isTA: activeCrsId = ",
				activeCrsId, ", TAForCourseIds = ", TAForCourseIds);
		return TAForCourseIds.contains(Integer.valueOf(activeCrsId));
	} // isTA()

	/** Gets whether I am a TA for any course.
	 * @return	true if am a TA for any course
	 */
	public boolean isTAForAny() {
		return !TAForCourseIds.isEmpty();
	} // isTAForAny()

	/** Gets whether I am a TA for the given course.
	 * @param	courseId	unique ID of the course
	 * @return	true if am a TA for the given course
	 */
	public boolean isTA(int courseId) {
		debugPrint("StudentSession.isTA: courseId = ", courseId,
				", TAForCourseIds = ", TAForCourseIds);
		return TAForCourseIds.contains(Integer.valueOf(courseId));
	} // isTA()

	/** Gets the external IDs of the assignments of the current course.
	 * @return	an array of external IDs
	 */
	public int[] getHWIds() {
		final int numHWs = Utils.getSize(assgts);
		final int[] allHWIds = new int[numHWs];
		for (int hwNum = 0; hwNum < numHWs; hwNum++) {
			allHWIds[hwNum] = assgts.get(hwNum).id;
		} // for each assignment
		return allHWIds;
	} // getHWIds()

	/** Gets the ID numbers of the questions assigned to this student in an
	 * assignment.
	 * @param	hwId	ID number of the assignment
	 * @return	ID numbers of the questions assigned to this student in an
	 * assignment
	 * @throws	DBException	if there's a problem reading the database
	 */
	public int[] getAssignedQIds(int hwId) throws DBException {
		return HWRead.getAssignedQIds(hwId, actedUser.getUserId());
	} // getAssignedQIds(int)

	/** Gets 1-based indices of assignments containing questions that were saved
	 * but not submitted.  We convert IDs retrieved from database into serial
	 * numbers here, rather than get the serial numbers directly from the
	 * database, because some of the course assignments may be invisible to this 
	 * student.
	 * @return	array of 1-based assignment indices
	 * @throws	DBException	if there's a problem reading the database
	 */
	public int[] getHWNumsSavedUnsubmitted() throws DBException {
		final List<Integer> unsubmittedHWIds = 
				ResponseRead.getHWIdsSavedUnsubmitted(actedUser.getUserId(), 
					activeCrs.getId());
		final List<Integer> allHWIds = Utils.intArrayToList(getHWIds());
		final int numUnsubmitted = Utils.getSize(unsubmittedHWIds);
		final int[] unsubmittedHWNums = new int[numUnsubmitted];
		for (int uhNum = 0; uhNum < numUnsubmitted; uhNum++) {
			final Integer unsubmittedHWId = unsubmittedHWIds.get(uhNum);
			unsubmittedHWNums[uhNum] = allHWIds.indexOf(unsubmittedHWId) + 1;
		} // for each assignment with an unsubmitted response
		return unsubmittedHWNums;
	} // getHWNumsSavedUnsubmitted()

	/** Changes the order of the student's courses. 
	 * @param	oldCrsNum	old 1-based serial number of a course
	 * @param	newCrsNum	new 1-based serial number of the course
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void moveStudentCourse(int oldCrsNum, int newCrsNum) 
			throws DBException {
		final String SELF = "StudentSession.moveStudentCourse: ";
		debugPrint(SELF + "oldCrsNum = ", oldCrsNum, ", newCrsNum = ",
				newCrsNum, ", actedUserId = ", actedUser.getUserId(),
				", number of courses = ", courses.size());
		courses.add(newCrsNum - 1, courses.remove(oldCrsNum - 1));
		EnrollmentRW.setCourseSerialNos(courses, actedUser.getUserId());
	} // moveStudentCourse(int, int)

	/** Gets the sum of all the extensions of this student in the current 
	 * course.
	 * @return	sum of all the extensions of a student in a course
	 * @throws	DBException	if there's a problem reading the database
	 */
	public double getSumExtensions() throws DBException {
		return HWRead.getSumExtensions(actedUser.getUserId(), 
				activeCrs.getId());
	} // getSumExtensions()

	/** Sets an extension on a single assignment.
	 * @param	hwId	the assignment ID
	 * @param	extensionStr	the string value of the extension
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void setExtension(int hwId, String extensionStr) throws DBException {
		HWWrite.setExtension(actedUser.getUserId(), activeCrs.getId(), hwId, 
				extensionStr);
	} // setExtension(int, String)

	/** Gets a student's userId by their institution and student ID number.
	 * @param	instnId	the institution ID
	 * @param	studentNum	the student number at that institution
	 * @return	the student's userId
	 * @throws	DBException	if there's a problemi reading the database
	 */
	public static String getUserId(int instnId, String studentNum) 
			throws DBException {
		return UserRead.getUserId(instnId, studentNum);
	} // getUserId(int, String)

/* ***************** ACE online textbook functions *******************/

	/** Gets a textbook.
	 * @param	bookId	ID number of the book to retrieve
	 * @return	a textbook
	 * @throws	ParameterException	if no book with this ID exists
	 * @throws	DBException	if there's a problem reading the database
	 */
	public Textbook getTextbook(int bookId) 
			throws DBException, ParameterException {
		return TextbookRW.getBook(bookId);
	} // getTextbook(int)

} // StudentSession
