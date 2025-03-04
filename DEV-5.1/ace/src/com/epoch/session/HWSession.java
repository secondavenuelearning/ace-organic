package com.epoch.session;

import chemaxon.struc.Molecule;
import com.epoch.assgts.Assgt;
import com.epoch.assgts.AssgtQGroup;
import com.epoch.courseware.User;
import com.epoch.db.ForumRW;
import com.epoch.db.HWRead;
import com.epoch.db.HWWrite;
import com.epoch.db.QSetRW;
import com.epoch.db.QuestionRW;
import com.epoch.db.ResponseLogger;
import com.epoch.db.ResponseRead;
import com.epoch.db.ResponseWrite;
import com.epoch.db.UserRead;
import com.epoch.db.UserWrite;
import com.epoch.evals.EvalResult;
import com.epoch.evals.evalConstants.EvalResultConstants;
import com.epoch.exceptions.ConfigurationException;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.NonExistentException;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.ResponseFormatException;
import com.epoch.exceptions.ResponseParseException;
import com.epoch.exceptions.VerifyException;
import com.epoch.genericQTypes.Choice;
import com.epoch.genericQTypes.ChooseExplain;
import com.epoch.genericQTypes.Rank;
import com.epoch.qBank.Figure;
import com.epoch.qBank.QDatum;
import com.epoch.qBank.QSetDescr;
import com.epoch.qBank.Question;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.session.sessConstants.HWConstants;
import com.epoch.substns.SubstnUtils;
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
import java.util.TimeZone;

/** Corresponds to a homework session taken by the student, a preview
 * of a question by an author, or a way to get information about a question,
 * given its serial number in an assignment.
 */
public class HWSession 
		implements EvalResultConstants, HWConstants, QuestionConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Unique (external) ID number of this assignment. */
	transient private final int hwId;
	/** Information about this assignment. */
	transient private Assgt assgt;
	/** Questions in this assignment. */
	transient private Question[] questions;
	/** Results of evaluation of this user's responses to the questions in this
	 * assignment; may also contain saved but not evaluated responses or
	 * initialized views of questions for this particular user. */
	transient private EvalResult[] evalResults;
	/** R groups of each question in this assignment assigned to this student, 
	 * hashed by question ID number, as strings. */
	transient final private Map<Integer, String[]> allQsSubstns = 
			new HashMap<Integer, String[]>();
	/** R groups of each question in this assignment assigned to this student, 
	 * hashed by question ID number, as molecules. */
	transient final private Map<Integer, Molecule[]> allQsRGroupMols = 
			new HashMap<Integer, Molecule[]>();
	/** When doing an R group question in SIMILAR mode, stores the original 
	 * R groups of the question. */
	transient private String[] origSubstns;
	/** Links from assignment questions to forum topics. */
	transient private Map<Integer, Integer> topicIdsByQIds;
	/** 1-based serial number of the question being solved. */
	private int currentIndex = 1;
	/** The due date and time of this assignment. */
	transient private Date dueDate = null;

	/** The user whose homework session this is. */
	transient private final User user;
	/** Login ID of the user doing this assignment. */
	private final String userId;
	/** Name of the user doing this assignment; used only in exams. */
	transient private String userName;
	/** First time of entry into this assignment; used for timed assignments,
	 * acquired if necessary. */
	private Date firstEntry;
	/** Non-English languages of this user in order of preference. */
	transient private String[] userLangs = new String[0];

	/** Whether the user of this assignment is an instructor or a TA. */
	transient private final boolean userIsInstructorOrTA;
	/**	IP address of the user. */
	transient private String ipAddr = "unknown";

	/** Constructor called by <code>homework/hwmain.jsp</code> and
	 * <code>submitUnsubmitted.jsp</code>.
	 * @param	user	the user working this assignment
	 * @param	hw	the assignment
	 * @param	isTAOrCoinstructor	whether or not this user is a TA or a
	 * coinstructor
	 * @param	ipAddress	IP address of the user
	 * @param	hideSynthCalcdProds	whether to hide calculated synthesis
	 * products from the user in all assignments
	 * @throws	DBException	if the database can't be read
	 * @throws	NonExistentException	if there's no such assignment
	 */
	public HWSession(User user, Assgt hw, boolean isTAOrCoinstructor, 
			String ipAddress, boolean hideSynthCalcdProds) 
			throws DBException, NonExistentException {
		debugPrint("HWSession for solving starting...");
		this.user = user;
		userId = user.getUserId();
		userLangs = user.getLanguages();
		userName = user.getName().toString();
		assgt = hw;
		hwId = assgt.id;
		ipAddr = ipAddress;
		userIsInstructorOrTA = isTAOrCoinstructor || Utils.among(userId, 
				assgt.instructorId, "admin", "administrator");
		// load the Qs and store them
		questions = HWRead.getQuestions(hwId, userId, userLangs,
				!userIsInstructorOrTA); // if student, check previously assigned Qs
		debugPrint("HWSession: got ", questions.length, " Qs.");
		// fetch evalResults only for students
		evalResults = new EvalResult[questions.length];
		if (!userIsInstructorOrTA) {
			final Map<Integer, EvalResult> storedResults =
					ResponseRead.getResults(userId, hwId);
			for (int qNum = 0; qNum < questions.length; qNum++) {
				final int qId = questions[qNum].getQId();
				evalResults[qNum] = storedResults.get(Integer.valueOf(qId));
			} // for each question
			allQsSubstns.putAll(
					ResponseRead.getStoredSubstns(hwId, userId));
		} // user is not instructor
		dueDate = assgt.getDueDate(userId, getFirstEntry());
		topicIdsByQIds = ForumRW.getTopicLinks(hwId);
		debugPrint("HWSession: default due date = ",
				(assgt.getDueDate() != null 
					? DateUtils.getString(assgt.getDueDate())
					: "indefinite"),
				", due date & time for ", userId, " = ",
				(dueDate != null ? DateUtils.getString(dueDate)
					: "indefinite"));
		if (hideSynthCalcdProds && !isDueDatePast()) {
			UserWrite.setMayNotSeeSynthCalcdProds(userId);
		} // if need to hide calculated synthesis products
	} // HWSession(User, Assgt, boolean, String, boolean)

	/** Constructor called by <code>authortool/startPreview.jsp</code>.
	 * @param	author	the question author
	 * @param	question	question being previewed
	 * @param	masterEdit	whether author is master author
	 * @throws	DBException	if the database can't be read
	 * @throws	NonExistentException	if there's no such assignment
	 */
	public HWSession(User author, Question question, boolean masterEdit) 
			throws DBException, NonExistentException {
		debugPrint("HWSession for preview starting...");
		user = author;
		userId = author.getUserId();
		hwId = -1;
		assgt = new Assgt();
		assgt.id = -1;
		assgt.instructorId = userId;
		assgt.setName("Author Preview");
		questions = new Question[1];
		questions[0] = question;
		evalResults = new EvalResult[questions.length];
		userIsInstructorOrTA = true;
		// need to get header for this question from its question set
		final int qSetId = question.getQSetId();
		try {
			final QSetDescr qSetDescr = (masterEdit 
					? QSetRW.getQSetDescr(qSetId)
					: QSetRW.getQSetDescr(qSetId, userId));
			questions[0].miscMessage = qSetDescr.header;
		} catch (Exception e) {
			Utils.alwaysPrint("HWSession: unable to get common question "
					+ "statement for preview Q in qSet with ID ", qSetId);
		} // try
	} // HWSession(User, Question, boolean)

	/** Constructor called by <code>textbooks/startQuestion.jsp</code>.
	 * @param	instructorId	login ID of textbook owner
	 * @param	qId	unique ID number of the single question to obtain from the 
	 * database
	 * @param	user	the user working this question
	 * @throws	DBException	if the database can't be read
	 * @throws	NonExistentException	if there's no such assignment
	 */
	public HWSession(String instructorId, int qId, User user) 
			throws DBException, NonExistentException {
		debugPrint("HWSession for textbook starting...");
		this.user = user;
		userId = instructorId;
		userLangs = user.getLanguages();
		hwId = -1;
		assgt = new Assgt();
		assgt.id = -1;
		assgt.instructorId = instructorId;
		assgt.setName("");
		userIsInstructorOrTA = true;
		init(qId, FULL_LOAD, LAST_ONLY);
		evalResults = new EvalResult[questions.length];
	} // HWSession(String, int, User)

	/** Constructor called by <code>gradebook/doReset.jsp</code>
	 * to reset the grade of a single question.
	 * @param	hwsetId	unique set ID
	 * @param	qId	unique ID number of the single question to obtain from the 
	 * database
	 * @param	studentId	login ID of student whose grade is being changed
	 * @throws	DBException	if the database can't be read
	 * @throws	NonExistentException	if there's no such assignment
	 */
	public HWSession(int hwsetId, int qId, String studentId) 
			throws DBException, NonExistentException {
		user = null;
		hwId = hwsetId;
		userId = studentId;
		userIsInstructorOrTA = false;
		init(qId, !FULL_LOAD, LAST_ONLY);
		dueDate = assgt.getDueDate(userId, getFirstEntry());
	} // HWSession(int, int, String)

	/** Constructor called by <code>gradebook/showMol.jsp</code> or
	 * <code>reports/showMol.jsp</code> or
	 * <code>gradebook/resetConfirm.jsp</code> to view the response to a 
	 * single question.  
	 * @param	user	the user viewing this response
	 * @param	hwsetId	unique set ID
	 * @param	qId	unique ID number of the single question to obtain from the 
	 * database
	 * @param	studentId	login ID of student who responded
	 * @throws	DBException	if the database can't be read
	 * @throws	NonExistentException	if there's no such assignment
	 */
	public HWSession(User user, int hwsetId, int qId, String studentId) 
			throws DBException, NonExistentException {
		hwId = hwsetId;
		this.user = user;
		userId = studentId;
		userIsInstructorOrTA = false;
		userLangs = user.getLanguages();
		init(qId, !FULL_LOAD, !LAST_ONLY);
		final Question question = questions[0];
		if (question != null && question.responseDisplayRequiresFigure()) {
			questions[0] = QuestionRW.getQuestion(qId, question.getAuthorId(), 
					ADD_HEADER, FULL_LOAD, userLangs);
		} // if need to get figures
	} // HWSession(User, int, int, String)

	/** Constructor called by <code>gradebook/startView.jsp</code> or 
	 * <code>reports/startView.jsp</code> to view a single question.
	 * @param	user	the user working this question
	 * @param	hwsetId	unique set ID
	 * @param	qId	unique ID number of the single question to obtain from the 
	 * database
	 * @param	isInstructorOrTA	true if this user is an instructor or a TA
	 * @throws	DBException	if the database can't be read
	 * @throws	NonExistentException	if there's no such assignment
	 */
	public HWSession(User user, int hwsetId, int qId, boolean isInstructorOrTA) 
			throws DBException, NonExistentException {
		hwId = hwsetId;
		this.user = user;
		userId = user.getUserId();
		userIsInstructorOrTA = isInstructorOrTA;
		userLangs = user.getLanguages();
		init(qId, FULL_LOAD, LAST_ONLY);
	} // HWSession(User, int, int, boolean)

	/** Code for loading a single question common to four constructors.
	 * @param	qId	unique ID number of the single question to obtain from the 
	 * database
	 * @param	fullLoad	full or super-light (qType, qFlags, question data only)
	 * @param	lastOnly	when there's an assignment, whether to load the 
	 * last response only or all responses
	 * @throws	DBException	if the database can't be read
	 * @throws	NonExistentException	if there's no such assignment
	 */
	private void init(int qId, boolean fullLoad, boolean lastOnly) 
			throws DBException, NonExistentException {
		final String SELF = "HWSession.init: ";
		if (hwId > 0) assgt = HWRead.getHW(hwId);
		// Load the one Q and store it
		final String instructorId = assgt.instructorId;
		debugPrint(SELF + "trying to get local version of Q", qId);
		Question question = QuestionRW.getQuestion(qId, instructorId, 
				ADD_HEADER, fullLoad, userLangs);
		if (question == null) { // check for master author version
			debugPrint(SELF + "couldn't get local version of Q", qId,
					", so trying to get master version.");
			question = QuestionRW.getQuestion(qId, 
					QuestionRW.MASTER_AUTHOR, ADD_HEADER, 
					fullLoad, userLangs);
		} // if question returned was null (no local version)
		if (question == null) {
			Utils.alwaysPrint(SELF + "Q is null");
		} // if question
		questions = new Question[] {question};
		if (hwId > 0) { // always true except for startQuestion.jsp
			if (lastOnly) {
				evalResults = new EvalResult[1];
				if (!userIsInstructorOrTA) {
					evalResults[0] = ResponseRead.getResult(userId, hwId, qId);
				} // if this is a student
			} else {
				evalResults = ResponseRead.getResults(userId, hwId, qId);
			} // if should load all response results or just the last one
			if (question != null && question.usesSubstns()) {
				allQsSubstns.put(Integer.valueOf(qId),
						ResponseRead.getStoredSubstns(hwId, userId, qId));
			} // if question uses R groups
		} // if there's a stored assignment
	} // init(int, String[], boolean, boolean)

/* ***************** Simple get/set methods ******************/

	/** Gets the unique ID of this assignment.
	 * @return	unique ID of the assignment
	 */
	public int getId() 						{ return hwId; }
	/** Gets the information associated with this assignment.
	 * @return	information associated with this assignment
	 */
	public Assgt getHW()			 		{ return assgt; }
	/** Gets the number of questions in the assignment.
	 * @return	number of questions in the assignment
	 */
	public int getCount() 					{ return questions.length; }
	/** Gets the login ID of the user doing this assignment.
	 * @return	login ID of the user doing this assignment
	 */
	public String getUserId()			 	{ return userId; }
	/** Gets the name of the user doing this assignment.
	 * @return	name of the user doing this assignment
	 */
	public String getUserName()			 	{ return userName; }
	/** Gets the IP address of the user doing this assignment.
	 * @return	IP address of the user doing this assignment
	 */
	public String getIpAddr()			 	{ return ipAddr; }
	/** Gets whether the user doing this assignment is an instructor or a TA.
	 * @return	whether the user doing this assignment is an instructor or a TA
	 */
	public boolean isUserInstructorOrTA() 	{ return userIsInstructorOrTA; }
	/** Gets the serial number of the current question in this assignment.
	 * @return	serial number of the current question in this assignment
	 */
	public int getCurrentIndex() 			{ return currentIndex; }
	/** Gets whether this assignment is an exam.
	 * @return	whether this assignment is an exam
	 */
	final public boolean isExam()			{ return assgt.isExam(); }
	/** Gets whether this assignment is timed.
	 * @return	whether this assignment is timed
	 */
	final public boolean isTimed()			{ return assgt.isTimed(); }
	/** Gets whether this assignment logs all responses to disk.
	 * @return	whether this assignment logs all responses to disk
	 */
	final public boolean logsAllToDisk()	{ return assgt.logsAllToDisk(); }
	/** Gets whether this assignment should show a "save without submitting"
	 * button.
	 * @return	whether this assignment should show a "save without submitting"
	 * button
	 */
	final public boolean showSaveWOSubmitting()	{ return assgt.showSaveWOSubmitting(); }
	/** Gets the duration of this assignment, if it is timed.
	 * @return	duration of this assignment
	 */
	public int getDuration() 				{ return assgt.getDuration(); }
	/** Gets if all responses to a question in this assignment (not just the
	 * most recent response) should be saved in the database.
	 * @return	true if all responses to a question should be kept
	 */
	final public boolean savePrevTries()	{ return assgt.savePrevTries(); }
	/** Get the number of responses to this question recorded in the database.
	 * @return	number of responses to this question
	 */
	public int getNumResults()				{ return evalResults.length; }
	/** Gets the due time of this assignment.
	 * @return	due time of this assignment
	 */
	public Date getDueDate()				{ return dueDate; }

	/** Gets the due time of this assignment in msec since the Epoch.
	 * @return	due time of this assignment in msec since the Epoch
	 */
	public long getDueDateInMillis()		{ 
		long millis = 0L;
		if (dueDate != null) {
			final Calendar cal = Calendar.getInstance();
			cal.setTime(dueDate);
			millis = cal.getTimeInMillis();
		} // if there's a due date
		return millis;
	} // getDueDateInMillis()

	/** Gets the number of tries of each result of the current question in the
	 * assignment.  Should usually return 1..n, but may not if number of tries
	 * has been increased.  Called by resetConfirm.jsp and showMol.jsp.
	 * @return	number of tries of each result of the current question
	 */
	public int[] getAttemptNums() {
		final int[] attemptNums = new int[getNumResults()];
		int attemptNum = 0;
		for (final EvalResult evalResult : evalResults) {
			attemptNums[attemptNum++] = evalResult.tries;
		} // for each result
		return attemptNums;
	} // getAttemptNums()

	/** Gets an English description of the question-answering page's mode.
	 * @param	mode	a mode
	 * @return	the mode's description in English
	 */
	public static String getModeName(int mode) {
		switch (mode) {
			case SOLVE: 			return "SOLVE";
			case VIEW: 				return "VIEW";
			case GRADEBOOK_VIEW: 	return "GRADEBOOK_VIEW";
			case PRACTICE: 			return "PRACTICE";
			case SIMILAR: 			return "SIMILAR";
			case PREVIEW: 			return "PREVIEW";
			case TEXTBOOK: 			return "TEXTBOOK";
			default: 				return "UNKNOWN";
		} // switch
	} // getModeName(int)

	/** Gets the current question.
	 * @return	current question
	 */
	public Question getCurrentQuestion() {
		return questions[currentIndex - 1];
	} // getCurrentQuestion()

	/** Sets the current question to a new value.  Used only in PREVIEW mode so
	 * that changes in question by author are reflected in Preview results.
	 * @param	qBuffer	current version of question being authored
	 */
	public void setCurrentQuestion(Question qBuffer) {
		questions[currentIndex - 1] = qBuffer;
	} // setCurrentQuestion(Question)

	/** Gets the figures of the current question.
	 * @return	figures of the current question
	 */
	public Figure[] getCurrentFigures() {
		return questions[currentIndex - 1].getFigures();
	} // getCurrentFigures()

	/** Gets all the question data of the current question.
	 * @return	question data of the current question
	 */
	public QDatum[][] getAllCurrentQData() {
		return questions[currentIndex - 1].getAllQData();
	} // getAllCurrentQData()

	/** Gets some of the question data of the current question.
	 * @param	listNum	0-based number of the list of question data
	 * @return	question data of the current question
	 */
	public QDatum[] getCurrentQData(int listNum) {
		return questions[currentIndex - 1].getQData(listNum);
	} // getCurrentQData(int)

	/** Gets the figures of the current question.
	 * @param	listNum	0-based number of the list of question data
	 * @return	figures of the current question
	 */
	public int getCurrentNumQData(int listNum) {
		return questions[currentIndex - 1].getNumQData(listNum);
	} // getCurrentNumQData(int)

	/** Gets the statement of the current question.
	 * @return	statement of the current question
	 */
	public String getCurrentStatement() {
		return questions[currentIndex - 1].getStatement();
	} // getCurrentStatement()

	/** Gets the type of the current question.
	 * @return	type of the current question
	 */
	public int getCurrentType() {
		return questions[currentIndex - 1].getQType();
	} // getCurrentType()

	/** Gets the flags of the current question.
	 * @return	flags of the current question
	 */
	public long getCurrentFlags() {
		return questions[currentIndex - 1].getQFlags();
	} // getCurrentFlags()

	/** Gets the unique ID of the current question.
	 * @return	unique ID of the current question
	 */
	final public int getCurrentQId() {
		return questions[currentIndex - 1].getQId();
	} // getCurrentQId()

	/** Gets the unique ID of the original question set of the current question.
	 * @return	unique ID of the original question set of the current question
	 */
	public int getCurrentQSetId() {
		return questions[currentIndex - 1].getQSetId();
	} // getCurrentQSetId()

	/** Gets the molecule to preload for the current question.
	 * @return	molecule to preload for the current question
	 * @throws	ParameterException	if there is no preloadMol
	 */
	public String getCurrentPreloadMol() throws ParameterException {
		return questions[currentIndex - 1].getPreloadMol();
	} // getCurrentPreloadMol()

	/** Gets the topic linked to the current question in this assignment.
	 * @return	topic ID number, or 0 if none found
	 */
	public int getCurrentLinkedTopic() {
		final Integer topicIdObj = 
				topicIdsByQIds.get(Integer.valueOf(getCurrentQId()));
		return (topicIdObj == null ? 0 : topicIdObj.intValue());
	} // getCurrentLinkedTopic()

	/** Gets the information stored about the most recent response to the
	 * current question.
	 * @return	result of evaluation of the last response to the current
	 * question, or the saved response to the current question, or the
	 * student's particular initialized view of the Q
	 */
	public EvalResult getCurrentResult() {
		return evalResults[currentIndex - 1];
	} // getCurrentResult()

	/** Get the information regarding how a student saw or responded to a Q.
	 * @param	index	1-based serial number of Q in assignment
	 * @return	result of evaluation of the last response, or the saved
	 * response, or the student's particular initialized view of the Q;
	 * null if unattempted and uninitialized; when called from showMol.jsp or
	 * resetConfirm.jsp, gets the result of evaluation of the nth attempt
	 * @throws	ParameterException	if the index is out of range
	 */
	public EvalResult getResult(int index) throws ParameterException {
		if (index <= 0 || index > evalResults.length)
			throw new ParameterException(" Cannot get index " + index );
		return evalResults[index - 1];
	} // getResult(int)

	/** Get a question in the assignment.
	 * @param	index	1-based serial number of Q in assignment
	 * @return	the question
	 * @throws	ParameterException	if the index is out of range
	 */
	public Question getQuestion(int index) throws ParameterException {
		if (index <= 0 || index > questions.length)
			throw new ParameterException(" Cannot get index " + index );
		return questions[index - 1];
	} // getQuestion()

	/** Gets an array of the ID numbers of each question in this instantiated
	 * assignment.
	 * @return	array of question ID numbers in this particular instance of the
	 * assignment
	 */
	private int[] getAllQIds() {
		final List<Integer> allQIds = new ArrayList<Integer>();
		for (final Question question : questions) {
			allQIds.add(Integer.valueOf(question.getQId()));
		} // for each question
		return Utils.listToIntArray(allQIds);
	} // getAllQIds()

	/** Select the question to solve.
	 * Range 1 .. qSet.length + 1; last is to tolerate "move next"
	 * from the last question.
	 * @param	index	1-based serial number of Q in assignment
	 * @throws	ParameterException	if the index is out of range
	 */
	public void setCurrentIndex(int index) throws ParameterException {
		if (index <= 0 || index > (questions.length + 1))
			throw new ParameterException(" Cannot set index to " + index
				+ "; size is " + questions.length);
		currentIndex = (index == questions.length + 1) ? 1 : index;
	} // setCurrentIndex(int)

	/** Gets whether another question on which display of a question depends
	 * has been answered correctly.
	 * @param	index	1-based index of the question whose display is being
	 * queried
	 * @return	true if the question can be displayed
	 * @throws	ParameterException	if the index is out of range
	 */
	public boolean getOkToDisplay(int index) throws ParameterException {
		return assgt.getOkToDisplay(getQuestion(index).getQId(), evalResults);
	} // getOkToDisplay(int)

	/** Gets the 1-based serial number of the question that must be answered 
	 * correctly before the current question can be displayed.
	 * @return	1-based serial number of the question that must be answered 
	 * correctly before the current question can be displayed, or -1 if no such
	 * question is found
	 */
	public int getDependsOnQNum() {
		int dependsOnQNum = -1;
		int qNum = 0;
		final int currentQId = getCurrentQuestion().getQId();
		final int dependsOn = assgt.getDependsOn(currentQId);
		for (final Question question : questions) {
			qNum++;
			if (question.getQId() == dependsOn) {
				dependsOnQNum = qNum;
				break;
			} // if the qId matches
		} // for each question
		return dependsOnQNum;
	} // getDependsOnQNum()

/* ***************** Initialization methods ******************/

	/** Called from <code>homework/hwmain.jsp</code> when this assignment
	 * is an exam; sets values required for logging of exam responses, logs
	 * fact of entry into exam.
	 * @param	zone	course time zone
	 * @param	host	remote host of user
	 */
	public void prepareForExam(TimeZone zone, String host) {
		if (!userIsInstructorOrTA && (!isDueDatePast() 
				|| assgt.recordAfterDue() || user.isExamStudent())) {
			try {
				ResponseLogger.logExamEntry(hwId, userName, zone, host, ipAddr);
			} catch (ConfigurationException e) {
				Utils.alwaysPrint("HWSession.prepareForExam: logging "
						+ "of entry of ", userName, " into exam from ",
						ipAddr, " failed.");
				e.printStackTrace();
			} // try
		} // if student, and exam still active
	} // prepareForExam(TimeZone, String)

	/** For R-group Qs or numeric Qs with variables in the statement, retrieves 
	 * substitutions from the database or initializes and stores them.
	 * @return	the R groups or variable values of this question, as Strings
	 * @throws	DBException	if the database can't be read
	 */
	public String[] getCurrentSubstns() throws DBException {
		final String SELF = "HWSession.getCurrentSubstns: ";
		final int qId = getCurrentQId();
		String[] substns = allQsSubstns.get(Integer.valueOf(qId));
		if (substns == null) {
			substns = generateNewSubstns();
			if (!userIsInstructorOrTA) {
				debugPrint(SELF + "writing R groups or values to database");
				writeSubstnsToDB(substns);
			} // if should store R groups
		} else debugPrint(SELF + "got stored R groups or values ", substns);
		return substns;
	} // getCurrentSubstns()

	/** For R-group Qs, gets the Molecule versions of the R groups.
	 * @return	the R groups of this question, as Molecules
	 * @throws	DBException	if the database can't be read
	 */
	public Molecule[] getCurrentRGroupMols() throws DBException {
		final String SELF = "HWSession.getCurrentRGroupMols: ";
		final Integer qIdObj = Integer.valueOf(getCurrentQId());
		Molecule[] rGroups = allQsRGroupMols.get(qIdObj);
		if (rGroups == null) {
			rGroups = SubstnUtils.getRGroupMols(getCurrentSubstns());
			allQsRGroupMols.put(qIdObj, rGroups);
		} // if R groups have not yet been converted to Molecules
		debugPrint(SELF + "got R group molecules ", rGroups);
		return rGroups;
	} // getCurrentRGroupMols()

	/** Generates a new set of R groups or variable values and stores them in 
	 * the map.
	 * @return	the new R groups or variable values of this question
	 * @throws	DBException	if the database can't be read
	 */
	public String[] generateNewSubstns() throws DBException {
		return generateNewSubstns(false);
	} // generateNewSubstns()

	/** Generates a new set of R groups or variable values and stores them in 
	 * the map.
	 * @param	saveNewSubstns	whether to overwrite the question's current R
	 * groups or variable values with the new ones
	 * @return	the new R groups or variable values of this question
	 * @throws	DBException	if the database can't be read or written to
	 */
	public String[] generateNewSubstns(boolean saveNewSubstns) 
			throws DBException {
		final String SELF = "HWSession.generateNewSubstns: ";
		final Integer qIdObj = Integer.valueOf(getCurrentQId());
		final boolean isNumeric = getCurrentQuestion().isNumeric();
		debugPrint(SELF + "generating ", isNumeric ? "variable values"
				: "R groups", " for Q", currentIndex, " (ID ", qIdObj, ")");
		origSubstns = allQsSubstns.get(qIdObj);
		String[] substns = origSubstns;
		boolean getSubstns = true;
		if (!isNumeric) {
			final Figure[] figures = getCurrentFigures();
			final Figure figure = (Utils.isEmpty(figures) ? null : figures[0]);
			if (figure == null) {
				substns = new String[0];
				getSubstns = false;
			} // if figure is null
		} // if !isNumeric
		if (getSubstns) do {
			substns = SubstnUtils.chooseSubstnValues(
					getCurrentQData(SUBSTNS), isNumeric);
		} while (Arrays.deepEquals(substns, origSubstns));
		debugPrint(SELF + "generated new ", isNumeric ? "variable values " 
				: "R groups ", substns, " to replace ", origSubstns, 
				saveNewSubstns ? " permanently" : " temporarily");
		if (saveNewSubstns) {
			origSubstns = substns;
			if (!userIsInstructorOrTA) {
				ResponseWrite.deleteResult(userId, hwId, getCurrentQId()); 
				writeSubstnsToDB(substns);
			} // if need to write to database
			evalResults[currentIndex - 1] = new EvalResult();
		} // if overwriting the old R groups or variable values with new ones (mastery question)
		allQsSubstns.put(qIdObj, substns);
		allQsRGroupMols.remove(qIdObj);
		return substns;
	} // generateNewSubstns(boolean)

	/** Writes substitutions (R groups or variable values) to database.
	 * @param	substns	the R groups to write
	 */
	private void writeSubstnsToDB(String[] substns) {
		final String SELF = "HWSession.writeSubstnsToDB: ";
		debugPrint(SELF + "writing substitutions to database");
		final int qId = getCurrentQId();
		try {
			ResponseWrite.storeSubstns(hwId, userId, qId, substns);
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + "DBException: Couldn't store "
					+ "substitutions for hwId = ", hwId, ", qId = ",
					qId, ", studentId = ", userId);
		} // try
	} // writeSubstnsToDB(String[])

	/** Resets a Q's R groups to the originally assigned value after SIMILAR 
	 * mode in answerframe.jsp.
	 * @return	the old and new R groups for the question
	 * @throws	ParameterException	if the index is out of range
	 * @throws	DBException	if the database can't be read
	 */
	public String[][] resetSubstns() throws DBException, ParameterException {
		return resetSubstns(currentIndex);
	} // resetSubstns()

	/** Resets a Q's R groups to the originally assigned value after SIMILAR 
	 * mode in answerframe.jsp.
	 * @param	qNum	1-based index of the Q to reset
	 * @return	the old and new R groups for the question
	 * @throws	ParameterException	if the index is out of range
	 * @throws	DBException	if the database can't be read
	 */
	public String[][] resetSubstns(int qNum) 
			throws DBException, ParameterException {
		final String SELF = "HWSession.resetSubstns: ";
		final Question resetQ = getQuestion(qNum);
		final int qId = resetQ.getQId();
		final Integer qIdObj = Integer.valueOf(qId);
		final String[][] oldNewSubstns = 
				new String[][] {allQsSubstns.get(qIdObj), origSubstns};
		if (!Utils.isEmpty(origSubstns)) {
			allQsSubstns.put(qIdObj, origSubstns);
			allQsRGroupMols.remove(qIdObj);
			debugPrint(SELF + "reset R groups of Q", qNum, 
					" with ID ", qId, " from ", oldNewSubstns[0], 
					" to stored version ", origSubstns);
			final Figure[] figures = resetQ.getFigures();
			if (!Utils.isEmpty(figures) && figures[0] != null) {
				figures[0].resetInstantiatedMol();
			} // if there's a first figure
		} else debugPrint(SELF + "no stored R groups for Q", qNum, 
				" with ID ", qId, ", so not resetting.");
		return oldNewSubstns;
	} // resetSubstns(int)

	/** Initializes a particular student's view of the question.
	 * @return	an EvalResult with status set to INITIALIZED;
	 * for rank, fill in the blank, or multiple-choice, it also contains the
	 * initialized response string with options initialized; and for R group
	 * Qs, it also contains a colon-separated list of R groups
	 */
	public EvalResult initializeStudentView() {
		return initializeStudentView(STORE_RESULT, SOLVE);
	} // initializeStudentView()

	/** Initializes the order of options seen by a particular student in
	 * Choice, Rank, FillBlank, and ChooseExplain questions where options have
	 * been randomized.
	 * @param	store	whether to store the result of the initialization;
	 * will be false for PREVIEW, TEXTBOOK, or SIMILAR mode or for 
	 * GRADEBOOK_VIEW mode if there's no evalResult already
	 * @param	mode	PREVIEW, TEXTBOOK, SIMILAR, GRADEBOOK_VIEW, or SOLVE 
	 * (when called from list.jsp)
	 * @return	an EvalResult with status set to INITIALIZED and a
	 * response string with no options chosen or ranked, or null if
	 * something goes wrong with the initialization process
	 */
	public EvalResult initializeStudentView(boolean store, int mode) {
		final String SELF = "HWSession.initializeStudentView: ";
		EvalResult evalResult = null;
		final String initStr = getInitializedString();
		if (initStr != null) {
			evalResult = new EvalResult();
			evalResult.status = INITIALIZED;
			evalResult.timeOfResponse = new Date();
			evalResult.qId = getCurrentQId();
			evalResult.lastResponse = initStr;
			evalResults[currentIndex - 1] = evalResult;
			if (!userIsInstructorOrTA && store) try {
				ResponseWrite.addResult(userId, hwId, currentIndex, evalResult);
			} catch (DBException e) {
				Utils.alwaysPrint(SELF + "DBException: Couldn't store "
						+ "initialization value ", evalResult.lastResponse);
			} // try
			// if user is instructor, initiated string
			// will remain unchanged for this session
		} // if initialized string is not null
		return evalResult;
	} // initializeStudentView(boolean, int)

	/** For rank, fill in the blank, multiple-choice, and choose-and-explain
	 * Qs, creates a particular student's view of the question.
	 * @return	a response with options initialized
	 */
	public String getInitializedString() {
		final String SELF = "HWSession.getInitializedString: ";
		final Question currentQ = getCurrentQuestion();
		final boolean isRank = currentQ.isRank();
		final boolean isChoice = currentQ.isChoice();
		final boolean isChooseExplain = currentQ.isChooseExplain();
		final boolean isFillBlank = currentQ.isFillBlank();
		String initValue = null; // must be initialized as null
		if (isRank || isChoice || isChooseExplain || isFillBlank) {
			final int numOpts = getCurrentNumQData(GENERAL);
			final boolean scramble = currentQ.scrambleOptions();
			final boolean exceptLast = currentQ.exceptLast();
			debugPrint(SELF, "currentIndex = ", currentIndex,
					", numOpts = ", numOpts, ", scramble = ", scramble,
					", exceptLast = ", exceptLast);
			initValue = (isRank 
						? Rank.getInitialString(numOpts, scramble, exceptLast)
					: isChooseExplain 
						? ChooseExplain.getInitialString(numOpts, scramble, 
							exceptLast)
					: isFillBlank && scramble 
						? Choice.getScrambledFillBlank(numOpts,
							getCurrentStatement())
					: Choice.getInitialString(numOpts, scramble, exceptLast));
			debugPrint(SELF + "rank, fill-blank, choice, or "
					+ "choose-and-explain Q: returning ", initValue);
		} // if rank, fill-blank, choice
		return initValue;
	} // getInitializedString()

	/** Replaces an existing question (and other questions in its bundle) with
	 * a new bundle in this instantiated assignment.
	 * @param	replacedQNum	1-based position of the question in this 
	 * assignment that is to be replaced
	 */
	public void reinstantiate(int replacedQNum) {
		final String SELF = "Assgt.reinstantiate: ";
		final int[] allQIds = getAllQIds();
		final int toBeReplacedQId = allQIds[replacedQNum - 1];
		// get 0-based qGroup number of each question
		final int[] qGrpNum0sByQNums = assgt.getGroupNumbers();
		final int qGrpNum0 = qGrpNum0sByQNums[replacedQNum - 1];
		final AssgtQGroup qGrp = assgt.getQGroup(qGrpNum0 + 1);
		// get a new bundle of Qs from the group
		List<Integer> newBundleQIds = null;
		while (true) {
			newBundleQIds = qGrp.instantiateOneBundle();
			// make sure it is not already in instantiated assignment
			if (!Utils.contains(allQIds, newBundleQIds.get(0))) break;
		} // until a not-already-used bundle is chosen
		final int[] newQIds = Utils.listToIntArray(newBundleQIds);
		// need to replace entire bundle of questions
		// get 0-based index of group start
		final int qGrpStartPosn0 = Utils.indexOf(qGrpNum0sByQNums, qGrpNum0);
		final int replacedQPosn0InQGroup = replacedQNum - qGrpStartPosn0 - 1;
		// get 0-based index of bundle start
		final int bundleSize = qGrp.getBundleSize();
		final int bundleNum0 = replacedQPosn0InQGroup / bundleSize;
		final int bundleStartPosn0 = qGrpStartPosn0 + bundleNum0 * bundleSize;
		// get IDs of all Qs that need to be replaced
		final int[] oldQIds = new int[newQIds.length];
		for (int qNum = 0; qNum < newQIds.length; qNum++) {
			oldQIds[qNum] = allQIds[qNum + bundleStartPosn0];
		} // for each old question
		debugPrint(SELF + "choosing new Q to replace ", toBeReplacedQId,
				" in 1-based position ", replacedQNum, "; allQIds = ", 
				allQIds, ", qGrpNum0 = ", qGrpNum0, ", qGrpStartPosn0 = ",
				qGrpStartPosn0, ", replacedQPosn0InQGroup = ", 
				replacedQPosn0InQGroup, ", qGroup picks ", qGrp.getPick(), 
				" of ", bundleSize, " question(s), bundleNum0 of Q "
				+ "within qGrp = ", bundleNum0, ", bundleStartPosn0 = ",
				bundleStartPosn0, ", replacing bundle of oldQIds ", oldQIds,
				" with ", newQIds);
		try {
			if (!userIsInstructorOrTA) {
				HWWrite.rewriteAssignedQuestions(userId, hwId, oldQIds, 
						newQIds);
			} // if a student
			int qNum = bundleStartPosn0 + 1; // convert back to 1-based
			for (final int newQId : newQIds) {
				Question question = QuestionRW.getQuestion(newQId, userId, 
						ADD_HEADER, FULL_LOAD, userLangs);
				if (question == null) { // check for master author version
					debugPrint(SELF + "couldn't get local version of Q", 
							newQId, ", so trying to get master version.");
					question = QuestionRW.getQuestion(newQId, 
							QuestionRW.MASTER_AUTHOR, ADD_HEADER, 
							FULL_LOAD, userLangs);
				} // if question returned was null (no local version)
				questions[qNum - 1] = question;
				if (formatOK(null)) evalResults[qNum - 1] = null;
				else {
					currentIndex = qNum;
					initializeStudentView();
				} // if formatOK()
				qNum++;
			} // for each question
			currentIndex = replacedQNum;
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + "threw DBException when replacing "
					+ "question number ", replacedQNum);
			e.printStackTrace();
		} // try
	} // reinstantiate(int)

	/** Gets whether a response to a multiple-choice, fill-in-the-blank, rank,
	 * or choose-and-explain question is in a parsable format.  Badly formatted 
	 * responses to other question types will not cause Javascript errors.
	 * @param	resp	the response
	 * @return	true if the response's format is parsable
	 */
	public boolean formatOK(String resp) {
		final String SELF = "HWSession.formatOK: ";
		final Question currentQ = getCurrentQuestion();
		final boolean isRank = currentQ.isRank();
		final boolean isChoice = currentQ.isChoice();
		final boolean isChooseExplain = currentQ.isChooseExplain();
		final boolean isFillBlank = currentQ.isFillBlank();
		if (resp == null) return !isChoice 
				&& !isChooseExplain
				&& !isFillBlank
				&& !isRank;
		final String qTypeStr = 
				(isChoice ? "multiple choice"
				: isChooseExplain ? "choose and explain"
				: isFillBlank ? "fill-in-the-blank"
				: "rank");
		try { // and see if they throw an error
			if (isChoice || isFillBlank) { 
				new Choice(resp); // might elicit exception
			} else if (isChooseExplain) {
				new ChooseExplain(resp); // might elicit exception
			} else if (isRank) {
				new Rank(resp); // might elicit exception
			} // if question type
		} catch (NumberFormatException e) {
			Utils.alwaysPrint(SELF + "Incorrectly formatted response to ", 
					qTypeStr, " question ", currentIndex, ":\n", resp);
			return false;
		} catch (ParameterException e) {
			Utils.alwaysPrint(SELF + "Incorrectly formatted response to ", 
					qTypeStr, " question ", currentIndex, ":\n", resp);
			return false;
		} // try to parse
		return true;
	} // formatOK(String)

/* ***************** Evaluation methods ******************/

	/** A user has submitted a response to a question.  Depending on the
	 * situation, take some of these actions: check for limits on response
	 * number, evaluate response, record the response, and log the response.
	 * @param	response	the response
	 * @param	mode	SOLVE, PRACTICE, SIMILAR, PREVIEW, TEXTBOOK, VIEW,
	 * GRADEBOOK_VIEW
	 * @return	the result of evaluating the response
	 * @throws	DBException	if the database can't be read or written to
	 * @throws	VerifyException	if the response isn't permitted
	 */
	public EvalResult submitResponse(String response, int mode)
			throws VerifyException, DBException {
		return submitResponse(response, mode, EVALUATE);
	} // submitResponse(String, int)

	/** A user has submitted a response to a question.  Depending on the
	 * situation, take some of these actions: check for limits on response
	 * number, evaluate response, record the response, and log the response.
	 * @param	response	the response
	 * @param	mode	SOLVE, PRACTICE, SIMILAR, PREVIEW, TEXTBOOK, VIEW,
	 * GRADEBOOK_VIEW
	 * @param	evaluate	whether to evaluate the response or just save it;
	 * default is true
	 * @return	the result of evaluating the response
	 * @throws	DBException	if the database can't be read or written to
	 * @throws	VerifyException	if the response isn't permitted
	 */
	public EvalResult submitResponse(String response, int mode,
			boolean evaluate) throws VerifyException, DBException {
		final String SELF = "HWSession.submitResponse: ";
		debugPrint(SELF + "mode = ", getModeName(mode), 
				", evaluate = ", evaluate,
				", response = \n", response);
		EvalResult evalResult = new EvalResult(); // to return
		evalResult.lastResponse = response;
		evalResult.qId = getCurrentQId();
		evalResult.ipAddr = ipAddr;
		final EvalResult prevResult = getCurrentResult();
		if (prevResult != null) {
			evalResult.tries = prevResult.tries;
		} // if previously stored values need to be passed on
		try {
			if (mode == SOLVE) {
				if (evaluate) evalResult.tries++;
				evalResult.timeOfResponse = new Date();
				if (!userIsInstructorOrTA) { // log response before evaluating
					logResponse(response);
				} // if user is student
				final boolean noPastRespEvaluated = prevResult == null
						|| Utils.among(prevResult.status, NO_STATUS, 
							INITIALIZED, SAVED);
				if (noPastRespEvaluated && !evaluate) {
					evalResult.status = SAVED;
				} else {
					if (!noPastRespEvaluated) {
						throwIfExcessReplies(prevResult, response);
					} // if a previous response was evaluated
					try {
						submitResponse(evalResult);
					} catch (ResponseParseException e) {
						if (!noPastRespEvaluated && evaluate) {
							respDoesntCount(evalResult, userLangs);
						} // if shouldn't count response as a try
					} // try
				} // if the first response or if should evaluate
				if (!userIsInstructorOrTA
						&& evalResult.status != INITIALIZED) { 
					debugPrint(SELF + "recording.");
					if (prevResult == null || prevResult.status == NO_STATUS 
							|| savePrevTries()) {
						ResponseWrite.addResult(userId, hwId, currentIndex, 
								evalResult);
					} else {
						ResponseWrite.setResult(userId, hwId, currentIndex, 
								evalResult);
					} // if should save previous tries
				} else if (userIsInstructorOrTA) {
					ResponseWrite.calculateModifiedGrade(hwId, evalResult.qId, 
							evalResult);
				} // if user is student
				if (evalResult.status == HUMAN_NEEDED) {
					evalResult.feedback = Utils.toString(BEFORE_HUMAN_1,
							evalResult.feedback == null ? BEFORE_HUMAN_2
								: Utils.getBuilder(' ', evalResult.feedback));
				} // if response requires human evaluation
				evalResults[currentIndex - 1] = evalResult;
			} else { // not SOLVE mode
				// evaluate response, don't record or log (unless exam)
				try {
					submitResponse(evalResult);
				} catch (ResponseParseException e) {
					debugPrint(SELF + "caught response parsing exception.");
				} // try
				if (evalResult.status == HUMAN_NEEDED) {
					final boolean useBefore = prevResult == null
							|| prevResult.status == HUMAN_NEEDED
							|| Utils.among(mode, PRACTICE, PREVIEW, TEXTBOOK);
					evalResult.feedback = Utils.toString(useBefore 
								? BEFORE_HUMAN_1 : AFTER_HUMAN_1,
							evalResult.feedback != null 
								? Utils.getBuilder(' ', evalResult.feedback)
								: useBefore ? BEFORE_HUMAN_2 : AFTER_HUMAN_2);
				} // if response requires human evaluation
			} // if mode is SOLVE
			debugPrint(SELF + "mode = ", getModeName(mode), 
					", evaluate = ", evaluate, 
					", evalResult.status = ", evalResult.status,
					", tries = ", evalResult.tries, 
					", grade = ", evalResult.grade,
					", modified grade = ", evalResult.modGrade,
					", feedback = ", evalResult.feedback, 
					", modifiedResponse:\n", evalResult.modifiedResponse);
		} catch (ResponseFormatException e) {
			evalResult.feedback = Utils.toString(
					PhraseTransln.translate("An error occurred, "
						+ "and ACE could not evaluate your response. "
						+ "Please report the following error to the "
						+ "programmers:", userLangs),
					'\n', e.getMessage());
			Utils.alwaysPrint(SELF + "bad format of response:\n", response,
					" by ", userId, " (", userName != null ? userName
						: UserRead.getUser(userId).getName(), 
					") to ", getCurrentQuestion().getQTypeDescription(),
					" question ", currentIndex, " (ID ", evalResult.qId, 
					") of assignment ", hwId, " at ",
					DateUtils.getString(Calendar.getInstance().getTime()),
					"; not recording.");
		/* } catch (InterruptedException e) {
			evalResult.feedback = PhraseTransln.translate("An error occurred, "
					+ "and ACE could not evaluate your response in a "
					+ "timely fashion. Please report this error to the "
					+ "programmers.", userLangs);
			Utils.alwaysPrint(SELF + "interrupted the thread because it took "
					+ "too long.\nuser name:\t", user.getName(),
					"\nuser login:\t", userId,
					"\nhwId:\t", hwId,
					"\nQ number:\t", currentIndex,
					"\nQ ID:\t", evalResult.qId,
					"\nsubmit time:\t", evalResult.timeOfResponse != null
						? DateUtils.getString(evalResult.timeOfResponse) : null,
					"\nnow:\t", DateUtils.getString(
						new Date()),
					"\nresponse:\n", response); /**/
		} // try
		return evalResult;
	} // submitResponse(String, int, boolean)

	/** Sends the response to be parsed or parsed and evaluated, monitoring it
	 * to make sure it doesn't take too long.
	 * @param	evalResult	contains the result of the evaluation
	 * instance of the question; may be null or empty
	 * @throws	DBException	if the database can't be read or written to
	 * @throws	ResponseFormatException	when the response format is incorrect
	 * @throws	ResponseParseException	when the synthesis or mechanism response
	 * cannot be parsed or the formula is malformed
	 */
	private void submitResponse(EvalResult evalResult) throws DBException, 
			/* InterruptedException,/**/ ResponseFormatException, 
			ResponseParseException {
		final String SELF = "HWSession.submitResponse: ";
		final Question currentQ = getCurrentQuestion();
		final Object[] substns = (!currentQ.usesSubstns()
				? null : currentQ.isNumeric() ? getCurrentSubstns() 
				: getCurrentRGroupMols());
		final boolean maySeeSynthCalcProds =
				UserRead.maySeeSynthCalcdProds(userId);
		if (assgt.delayGrading()) {
			currentQ.parseResponse(evalResult, substns, userLangs, 
					maySeeSynthCalcProds);
			evalResult.status = HUMAN_NEEDED;
		} else {
			/* final int timeWithin = 3000; // milliseconds
			final Thread myself = Thread.currentThread();
			final Timer timer = new Timer(timeWithin, myself);
			timer.start();
			// might take a long time but is sensitive to interrupts
			currentQ.evaluateResponse(evalResult, substns, userLangs, 
					maySeeSynthCalcProds);
			timer.interrupt(); // kill the timer
			Thread.sleep(0); // overcautious: clear any pending interrupt from 
						// the timer /**/
			currentQ.evaluateResponse(evalResult, substns, userLangs, 
					maySeeSynthCalcProds); /**/
		} // if delay grading
	} // submitResponse(EvalResult)

	/** Logs the response to disk.
	 * @param	response	the response
	 */
	private void logResponse(String response) {
		final String SELF = "HWSession.logResponse: ";
		final Question currentQ = getCurrentQuestion();
		final int qId = getCurrentQId();
		final int type = getCurrentType();
		final long flags = getCurrentFlags();
		try {
			debugPrint(SELF + "logging response to Q ", qId);
			if (currentQ.isMasterQ()) {
				ResponseLogger.logResponse(qId, type, flags, response);
			} else { // locally authored or modified question
				ResponseLogger.logResponse(qId, type, flags, 
						currentQ.getAuthorId(), response);
			} // if master Q
		} catch (ConfigurationException e) {
			Utils.alwaysPrint(SELF + "logging of response failed.");
			e.printStackTrace();
		} catch (ParameterException e) {
			Utils.alwaysPrint(SELF + "logging of response failed.");
			e.printStackTrace();
		} // try
	} // logResponse(String)

	/** Checks to see if student has already submitted the maximum number of
	 * responses or has already submitted the correct response.
	 * @param	prevResult	the result from the previous response
	 * @param	response	the current response
	 * @throws	VerifyException	if the student has already submitted the
	 * maximum number of responses or has already submitted the correct response
	 */
	private void throwIfExcessReplies(EvalResult prevResult, String response) 
			throws VerifyException {
		final String SELF = "HWSession.throwIfExcessReplies: ";
		final int qId = getCurrentQId();
		if (!assgt.isMasteryAssgt() 
				&& !assgt.allowUnlimitedTries() 
				&& prevResult.tries >= assgt.getMaxTries()) {
			Utils.alwaysPrint(SELF + "correct answer to question ", 
					currentIndex, " (ID ", qId, ") of assignment ", hwId, 
					" by ", userId, userName == null ? "" 
						: Utils.toString(" (", userName,  ") "),
					" already received; response:\n", response, " at ",
					DateUtils.getString(Calendar.getInstance().getTime()),
					" not recorded.");
			throw new VerifyException("You have already responded the "
					+ "maximum number of times allowed.");
		} // if not mastery assignment and max replies already reached
		if (prevResult.grade == 1) {
			Utils.alwaysPrint(SELF + "correct answer to question ", 
					currentIndex, " (ID ", qId, ") of assignment ", hwId, 
					" by ", userId, userName == null ? ""
						: Utils.toString(" (", userName,  ") "),
					" already received; response:\n", response, " at ",
					DateUtils.getString(Calendar.getInstance().getTime()),
					" not recorded.");
			throw new VerifyException("You have already submitted "
					+ "the correct answer.");
		} // if correctly answered already
	} // throwIfExcessReplies(EvalResult, String)

	/** Makes a response not count as an attempt.
	 * @param	evalResult	the EvalResult that holds the number of tries
	 * and feedback
	 * @param	userLangs	languages preferred by the user in order of
	 * preference
	 */
	private void respDoesntCount(EvalResult evalResult, 
			String[] userLangs) {
		debugPrint("HWSession.respDoesntCount: caught response parsing "
				+ "exception; don't count against number of tries.");
		evalResult.tries--;
		evalResult.feedback = Utils.toString(evalResult.feedback, "<p>",
				PhraseTransln.translate(
					"This response will not count as an attempt.", userLangs),
				"</p>");
	} // respDoesntCount(EvalResult, String[])

	/** Overwrites the grade and number of tries of a student's response to a 
	 * question. Called by gradebook/doReset.jsp and generateNewSubstns().  
	 * @param	newResult	information to be stored
	 * @param	flags	whether to rewrite the last response and its
	 * time, whether the record being modified is the most recent response, 
	 * whether to preserve the R groups assigned to this student
	 * @throws	DBException	if the database can't be written to
	 */
	public void setResult(EvalResult newResult, boolean[] flags) 
			throws DBException {
		ResponseWrite.setResult(userId, hwId, ResponseWrite.NO_QNUM, 
				getCurrentQId(), newResult, flags);
	} // setResult(EvalResult, boolean[])

/* ***************** Due methods ******************/

	/** Determines whether the assignment is past due, either because the due
	 * date is past, or because the exam is timed and the allowed time since 
	 * first entry has been exceeded. If the assignment is an
	 * exam, returns to the database to retrieve the due date and extensions;
	 * otherwise, uses the values stored in this HWSession when it was initiated.
	 * @return	whether the assignment is past due
	 */
	final public boolean isDueDatePast() {
		final String SELF = "HWSession.isDueDatePast: ";
		final Date firstEntry = getFirstEntry();
		if (isExam()) try {
			// due date or extension may have changed; get afresh
			debugPrint(SELF + "is exam, retrieving current due data.");
			HWRead.refreshDueData(assgt);
			dueDate = assgt.getDueDate(userId, firstEntry);
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + "caught exception trying to refresh "
					+ "assignment due date and extensions.");
			e.printStackTrace();
		} // try
		return !assgt.isSolvingAllowed(userId, firstEntry);
	} // isDueDatePast()

	/** Gets the date of first entry into this assignment, retrieving it from
	 * the database if necessary.
	 * @return	the date of first entry into this assignment
	 */
	public final Date getFirstEntry() {
		final String SELF = "HWSession.getFirstEntry: ";
		if (firstEntry == null) {
			if (userIsInstructorOrTA) {
				firstEntry = new Date();
			} else try {
				firstEntry = HWRead.getAssgtEntryDate(hwId, userId);
			} catch (DBException e) {
				Utils.alwaysPrint(SELF + "caught DBException trying to get "
						+ "first entry into assignment ", hwId);
			} finally {
				if (firstEntry == null) { 
					// first time assignment's been entered
					try {
						debugPrint(SELF + "first entry into assignment "
								+ "has not been set yet; setting it now.");
						firstEntry = setFirstEntry();
					} catch (DBException e2) { // unlikely
						firstEntry = new Date();
					} // try
				} // if no value retrieved for firstEntry
			} // try
		} // if firstEntry hasn't been retrieved
		return firstEntry;
	} // getFirstEntry()

	/** Sets the first entry into this assignment and stores it in the database.
	 * Shouldn't be necessary.
	 * @return	the date and time of first entry
	 * @throws	DBException	if the database can't be written to
	 */
	private Date setFirstEntry() throws DBException {
		final Date firstEntry = new Date();
		HWWrite.setAssgtEntryDate(hwId, userId, firstEntry);
		return firstEntry;
	} // setFirstEntry()

} // HWSession
