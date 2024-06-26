package com.epoch.session;

import com.epoch.assgts.Assgt;
import com.epoch.db.HWRead;
import com.epoch.db.ResponseRead;
import com.epoch.evals.EvalResult;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.NonExistentException;
import com.epoch.exceptions.ParameterException;
import com.epoch.session.sessConstants.GradeConstants;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Holds the grades of one or all students in a course.  */
public class GradeSet implements GradeConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Unique ID of course. */
	final private int courseId;
	/** Description of each assignment in order of sequence. */
	final private Assgt[] assgts;
	/** Number of assignments. */
	transient final private int numHWs;
	/** Unique IDs of assignments parallel to <code>assgts</code>. */
	transient final private int[] hwIds;
	/** Maximum grade of each assignment,  parallel to <code>assgts</code>. */
	transient final private double[] maxGrades;
	/** Number of Qs that students will see in each assignment, parallel to
	 * <code>assgts</code>. */
	final private int[] numQsSeen;
	/** For each student (or the one student), total grade on each assignment. */
	transient final private Map<String, double[]> allTotalGrades;
	/** For each student (or the one student), whether student has mastered all
	 * questions in each mastery assignment. */
	transient final private Map<String, boolean[]> allMastery;
	/** For each assignment, students who have at least one most recent response 
	 * that requires human evaluation, keyed by assignment ID. */
	transient private Map<Integer, ArrayList<String>> humanGradingReqd;
	/** All students' (or the one student's) evaluation results for all 
	 * assignments; results for individual assignments acquired as needed. */
	transient final private Map<Integer, HashMap<String, EvalResult[]>> hwResults;
	/** ID number of the current assignment in the detailed gradebook; 0 for 
	 * general gradebook. */
	transient private int currentHWId;
	/** The one student's login ID; null if there are multiple students
	 * (instructor or TA gradebook). */
	transient final private String oneStudentId; // set in constructor

	/** Constructor.
	 * @param	crsId	ID number of the course
	 * @param	assignts	assignments of the current course
	 * @param	studentId	student's login ID; null if for instructor
	 * @param	tutorials	whether we are loading tutorials grades
	 * @throws	DBException	if the database can't be read
	 * @throws	NonExistentException	if there's no such assignment
	 */
	public GradeSet(int crsId, Assgt[] assignts, String studentId, 
			boolean tutorials) throws DBException, NonExistentException {
		final String SELF = "GradeSet: ";
		courseId = crsId;
		assgts = assignts;
		oneStudentId = studentId;
		numHWs = assgts.length;
		hwIds = Assgt.getHWIds(assgts);
		maxGrades = Assgt.getMaxGrades(assgts);
		numQsSeen = Assgt.getNumQsSeen(assgts);
		allTotalGrades = (tutorials
				? ResponseRead.getTotalGrades(hwIds, courseId, oneStudentId)
				: ResponseRead.getTotalGrades(hwIds, studentId));
		if (tutorials) {
			allMastery = new HashMap<String, boolean[]>();
			humanGradingReqd = new HashMap<Integer, ArrayList<String>>();
		} else { 
			allMastery = ResponseRead.getAssgtsMastered(hwIds, studentId);
			getHumanGradingReqd();
		} // if tutorials
		hwResults = new HashMap<Integer, HashMap<String, EvalResult[]>>();
		debugPrint(SELF + "numHWs = ", numHWs, ", oneStudentId = ", 
				oneStudentId, ", tutorials = ", tutorials);
	} // GradeSet(int, Assgt[], String, boolean)

	/** Gets from the database whether any responses to each assignment require
	 * human grading.
	 * @throws	DBException	if the database can't be read
	 */
	final public void getHumanGradingReqd() throws DBException {
		humanGradingReqd = ResponseRead.getHumanGradingRequired(
				courseId, oneStudentId);
	} // getHumanGradingReqd()

/* **************** Get assignment characteristics methods *****************/

	/** Gets the course ID number.
	 * @return	the course ID number
	 */
	int getCourseId() 			{ return courseId; }

	/** Gets the assignment descriptions.
	 * @return	the assignment descriptions
	 */
	public Assgt[] getAssgts() 	{ return assgts; }

	/** Gets an assignment.
	 * @param	hwNum	1-based serial number of the assignment
	 * @return	the assignment
	 * @throws	ParameterException	if the hwNum is out of range
	 */
	public Assgt getAssgt(int hwNum) throws ParameterException {
		if (hwNum > numHWs)
			throw new ParameterException("Invalid hwNum " + hwNum
					+ " to GradeSet.getAssgt()" );
		return assgts[hwNum - 1];
	} // getAssgt(int)

	/** Gets the unique ID of an assignment.
	 * @param	hwNum	1-based serial number of the assignment
	 * @return	unique ID of an assignment
	 * @throws	ParameterException	if the hwId can't be found
	 */
	public int getHWId(int hwNum) throws ParameterException {
		return getAssgt(hwNum).id;
	} // getHWId(int)

	/** Gets the 1-based serial number of an assignment.
	 * @param	hwId	unique ID of the assignment
	 * @return	1-based serial number of the assignment
	 * @throws	ParameterException	if the hwId can't be found
	 */
	int getHWNum(int hwId) throws ParameterException {
		final int hwNum  = Utils.indexOf(hwIds, hwId) + 1;
		if (hwNum == 0) throw new ParameterException("Invalid hwId " 
				+ hwId + " to GradeSet.getHWNum()");
		else return hwNum;
	} // getHWNum(int)

	/** Gets all question IDs for an assignment, arranged into groups.
	 * @param	hwNum	1-based serial number of assignment
	 * @return	List of Lists of question IDs
	 * @throws	ParameterException	if the hwNum is out of range
	 */
	public List<ArrayList<Integer>> getGroupedQIds(int hwNum)
			throws ParameterException {
		return getAssgt(hwNum).getGroupedQIds();
	} // getGroupedQIds(int)

	/** Gets a single list of all question IDs for an assignment in the order 
	 * of their grouping.
	 * @param	hwNum	1-based serial number of assignment
	 * @return	single List of all question IDs
	 * @throws	ParameterException	if the hwNum is out of range
	 */
	public List<Integer> getQIds(int hwNum) throws ParameterException {
		return getAssgt(hwNum).getQIds();
	} // getQIds(int)

	/** Gets the number of questions in an assignment.
	 * @param	hwNum	1-based serial number of the assignment
	 * @return	number of questions in an assignment
	 * @throws	ParameterException	if the hwNum is out of range
	 */
	public int getNumQuestionsTotal(int hwNum) throws ParameterException {
		return getAssgt(hwNum).getNumAllQs();
	} // getNumQuestionsTotal(int)

	/** Gets the number of questions to be answered in an assignment.
	 * @param	hwNum	1-based serial number of the assignment
	 * @return	number of questions to be answered in assignment
	 * @throws	ParameterException	if the hwNum is out of range
	 */
	public int getNumQuestionsAssigned(int hwNum) throws ParameterException {
		return getAssgt(hwNum).getNumQsSeen();
	} // getNumQuestionsAssigned(int)

	/** Gets the number of question groups in an assigment.
	 * @param	hwNum	1-based serial number of the assignment
	 * @return	number of question groups in assignment
	 * @throws	ParameterException	if the hwNum is out of range
	 */
	public int getNumQuestionGroups(int hwNum) throws ParameterException {
		return getAssgt(hwNum).getNumGroups();
	} // getNumQuestionGroups(int)

	/** Gets how many questions were chosen for each group in
	 * an assigment.
	 * @param	hwNum	1-based serial number of the assignment
	 * @return	number of question groups in assignment
	 * @throws	ParameterException	if the hwNum is out of range
	 */
	public int[] getQuestionPicks(int hwNum) throws ParameterException {
		return getAssgt(hwNum).getPicks();
	} // getQuestionPicks(int)

	/** Gets the maximum possible points for an assignment.
	 * @param	hwNum	serial number of the assignment
	 * @return	maximum grade on an assignment
	 * @throws	ParameterException	if the hwNum is out of range
	 */
	public double getMaxGrade(int hwNum) throws ParameterException {
		final String SELF = "GradeSet.getMaxGrade: "; 
		if (hwNum > numHWs)
			throw new ParameterException("Invalid hwNum " + hwNum
					+ " to " + SELF);
		return maxGrades[hwNum - 1];
	} // getMaxGrade(int)

	/** Gets the number of Qs seen by students in an assignment.
	 * @param	hwNum	serial number of the assignment
	 * @return	number of Qs seen by students in an assignment
	 * @throws	ParameterException	if the hwNum is out of range
	 */
	public int getNumQsSeen(int hwNum) throws ParameterException {
		final String SELF = "GradeSet.getMaxGrade: "; 
		if (hwNum > numHWs)
			throw new ParameterException("Invalid hwNum " + hwNum
					+ " to " + SELF);
		return numQsSeen[hwNum - 1];
	} // getNumQsSeen(int)

	/** Gets the sums of the grades of all assignments of a student.
	 * @param	studentId	login ID of the student
	 * @return	array of total grades on each assignment
	 */
	public double[] getTotalGrades(String studentId) {
		return allTotalGrades.get(studentId);
	} // getTotalGrades(String)

	/** Gets for all assignments whether a student has mastered them.
	 * @param	studentId	login ID of the student
	 * @return	array of mastery of each assignment
	 */
	public boolean[] getAssgtsMastered(String studentId) {
		return allMastery.get(studentId);
	} // getAssgtsMastered(String)

	/** Gets IDs of a student's assignment from database in the form of a
	 * list of lists.
	 * @param	hwNum	1-based number of assignment
	 * @param	userId	login ID of student
	 * @return	List of Lists of question IDs
	 * @throws	ParameterException	if the hwId can't be found
	 * @throws	DBException	if the database can't be read
	 */
	public List<ArrayList<Integer>> getAssignedQIds(int hwNum, String userId) 
			throws DBException, ParameterException {
		final String SELF = "GradeSet.getAssignedQIds: ";
		final ArrayList<ArrayList<Integer>> assignedQIds = 
				new ArrayList<ArrayList<Integer>>();
		final int[] assignedQIdsArr = getAssignedQIdsArr(hwNum, userId);
		for (final int assignedQId : assignedQIdsArr) {
			final ArrayList<Integer> qGrp = new ArrayList<Integer>();
			qGrp.add(Integer.valueOf(assignedQId));
			assignedQIds.add(qGrp);
		} // for each assignedQId
		return assignedQIds;
	} // getAssignedQIds(int, String)

	/** Gets IDs of a student's assignment from database in the form of an array, 
	 * or creates a dummy assignment if none has been created.
	 * @param	hwNum	1-based serial number of assignment
	 * @param	userId	login ID of student
	 * @return	array of question IDs
	 * @throws	ParameterException	if the hwId can't be found
	 * @throws	DBException	if the database can't be read
	 */
	public int[] getAssignedQIdsArr(int hwNum, String userId)
			throws DBException, ParameterException {
		int[] assignedQIdsArr = 
				HWRead.getAssignedQIds(getHWId(hwNum), userId);
		if (Utils.getLength(assignedQIdsArr) == 0) {
			final List<ArrayList<Integer>> qIds = 
					getAssgt(hwNum).getGroupedQIds();
			final int numQs = Utils.getSize(qIds);
			assignedQIdsArr = new int[numQs];
			for (int qNum = 0; qNum < numQs; qNum++) {
				assignedQIdsArr[qNum] = qIds.get(qNum).get(0).intValue();
			} // for each question
		} // if no Qs from instantiated assignment table
		return assignedQIdsArr;
	} // getAssignedQIdsArr(int, String)

	/** Gets whether another question on which display of a question depends
	 * has been answered correctly.
	 * @param	studentId	login ID of student
	 * @param	hwNum	serial number of the assignment
	 * @param	qNum	1-based index of the question whose display is being
	 * queried
	 * @return	true if the question can be displayed
	 * @throws	ParameterException	if the qNum or hwNum is out of range
	 * @throws	DBException	if the database can't be read
	 * @throws	NonExistentException	if there's no such student
	 */
	public boolean getOkToDisplay(String studentId, int hwNum, int qNum) 
			throws DBException, NonExistentException, ParameterException {
		final EvalResult[] results = getOrderedResults(hwNum, studentId);
		final int[] assignedQIds = getAssignedQIdsArr(hwNum, studentId);
		return getAssgt(hwNum).getOkToDisplay(assignedQIds[qNum - 1], results);
	} // getOkToDisplay(String, int, int)

/* **************** EvalResult methods *****************/

	/** Sets the current assignment and retrieves the most recent results
	 * (excluding responses) for all questions in it.
	 * @param	hwId	unique ID of the assignment
	 * @throws	DBException	if the database can't be read
	 */
	private void setCurrentHWByHWId(int hwId) throws DBException {
		if (hwId != currentHWId) {
			currentHWId = hwId;
			if (getCurrentHW() == null) {
				// note: oneStudentId is null for instructors
				hwResults.put(Integer.valueOf(hwId), 
						new HashMap<String, EvalResult[]>(
							ResponseRead.getOneHWResults(hwId, oneStudentId)));
			} // if this assignment's results not yet retrieved from DB
		} // if new hwId
	} // setCurrentHWByHWId(int)

	/** Gets the students' most recent results (excluding responses) of the 
	 * current assignment.
	 * @return	the most recent results of the current assignment
	 */
	private Map<String, EvalResult[]> getCurrentHW() {
		return hwResults.get(Integer.valueOf(currentHWId));
	} // getCurrentHW()

	/** Sets the current assignment to none.  Used when moving back to grade.jsp. */
	public void resetCurrent() {
		currentHWId = 0;
	} // resetCurrent()

	/** Returns evaluation results (excluding responses) of all questions
	 * in this assignment for this student in an array parallel to the questions
	 * assigned to the student.
	 * @param	hwNum	serial number of the assignment
	 * @param	studentId	student's login ID
	 * @return	results of evaluation of a student's responses to questions in
	 * this assignment, in an array parallel to the questions assigned to the 
	 * student
	 * @throws	ParameterException	if the hwNum is out of range
	 * @throws	NonExistentException	if the hwId is not assigned in this
	 * course
	 * @throws	DBException	if the database can't be read, or if data is
	 * corrupted
	 */
	public EvalResult[] getOrderedResults(int hwNum, String studentId)
			throws DBException, NonExistentException, ParameterException {
		final String SELF = "GradeSet.getOrderedResults:";
		final int hwId = getHWId(hwNum);
		if (numQsSeen[hwNum - 1] == HWRead.UNSIZED)
			throw new NonExistentException(" Homework with unique id "
					+ hwId + " does not exist ");
	   	final int hwQCount = numQsSeen[hwNum - 1];
	   	final EvalResult[] orderedResults = new EvalResult[hwQCount];
		// get all qIds of this assignment, arranged into groups
		final List<ArrayList<Integer>> qIds = getGroupedQIds(hwNum);
		if (qIds.isEmpty()) return orderedResults;
		if (qIds.size() > hwQCount) {
			throw new DBException("number of question groups is " 
					+ qIds.size() + " while number of assigned Qs is " 
					+ hwQCount + "; data probably corrupted ");
		} // if the size of qIds is greater than the size of orderedResults
		setCurrentHWByHWId(hwId);
		final EvalResult[] oneStudentResults = getCurrentHW().get(studentId);
		if (Utils.isEmpty(oneStudentResults)) return orderedResults;
		// now put results from map into appropriate positions in array
		final List<Integer> resultsQIds = getQIds(oneStudentResults);
		int qNum = 0;
		for (final List<Integer> qIdsGroup : qIds) {
			for (final Integer qId : qIdsGroup) {
				final int locn = resultsQIds.indexOf(qId);
				if (locn >= 0) {
					if (qNum >= orderedResults.length) break;
					orderedResults[qNum] = oneStudentResults[locn];
					qNum++;
				} // if the Q is in the student's list
			} // for each question in a group
		} // for each group of questions
		return orderedResults;
	} // getOrderedResults(int, String)

	/** Gets a list of question ID numbers corresponding to an array of results.
	 * @param	results	array of question response results
	 * @return	list of question ID numbers
	 */
	private List<Integer> getQIds(EvalResult[] results) {
		final List<Integer> qIds = new ArrayList<Integer>();
		for (final EvalResult result : results) qIds.add(Integer.valueOf(result.qId));
		return qIds;
	} // getQIds(EvalResult[])

	/** Returns evaluation result of one question (including the last response)
	 * in this assignment for this student.
	 * @param	hwNum	serial number of the assignment (1-based)
	 * @param	studentId	student's login ID
	 * @param	qId	unique ID number of the question
	 * @return	result of evaluation of a student's responses to this question
	 * in this assignment; null if unattempted
	 * @throws	ParameterException	if there's no assignment with that serial
	 * number
	 * @throws	NonExistentException	if there's no assignment with that ID
	 * @throws	DBException	if the database can't be read
	 */
	public EvalResult getResult(int hwNum, String studentId, int qId)
			throws DBException, NonExistentException, ParameterException {
		final String SELF = "GradeSet.getResult: ";
		final int hwId = getHWId(hwNum);
		if (numQsSeen[hwNum - 1] == HWRead.UNSIZED)
			throw new NonExistentException(" Homework with unique id "
					+ hwId + " does not exist ");
		return ResponseRead.getResult(studentId, hwId, qId);
	} // getResult(int, String, int)

	/** Sets a newly modified grade in the gradebook and recalculates the
	 * student's total grade on the assignment.  Called from
	 * HWSession.setResult() and RegradeSession.doRegrade().
	 * @param	studentId	student's login ID
	 * @param	hwNum	serial number of the assignment (1-based)
	 * @param	qId	question ID number
	 * @param	newResult	result of the student's response, containing the
	 * newly modified grade; or may be null
	 */
	public void putInGradebook(String studentId, int hwNum, int qId, 
			EvalResult newResult) {
		final String SELF = "GradeSet.putInGradebook: ";
		try {
			final int hwId = getHWId(hwNum);
			setCurrentHWByHWId(hwId);
			double[] totalGrades = allTotalGrades.get(studentId);
			if (totalGrades == null) {
				totalGrades = new double[numHWs];
				allTotalGrades.put(studentId, totalGrades);
			} // if there are no grades for this student
			final EvalResult[] oneStudentResults =
					getCurrentHW().get(studentId);
			int qNum = 0;
			if (newResult == null) {
				final EvalResult[] resultsArr = 
						new EvalResult[oneStudentResults.length - 1];
				for (final EvalResult result : oneStudentResults) {
					if (result.qId != qId) resultsArr[qNum++] = result;
					else totalGrades[hwNum - 1] -= result.modGrade;
				} // for each evalResult
				getCurrentHW().put(studentId, resultsArr);
				debugPrint(SELF + "setting result for response of ", 
						studentId, " to Q with ID ", qId, " of assignment ", 
						hwNum, " to null; new totalGrade = ", 
						totalGrades[hwNum - 1]);
			} else if (oneStudentResults != null) {
				for (final EvalResult result : oneStudentResults) {
					if (result.qId == qId) {
						totalGrades[hwNum - 1] -= result.modGrade;
						oneStudentResults[qNum] = newResult;
						totalGrades[hwNum - 1] += newResult.modGrade;
						break;
					} // if we found the right evalResult
					qNum++;
				} // for each evalResult
				debugPrint(SELF + "setting new result for response of ", 
						studentId, " to Q with ID ", qId, " of assignment ", 
						hwNum, ": new grade = ", newResult.grade, 
						" and modGrade = ", newResult.modGrade, 
						"; new totalGrade = ", totalGrades[hwNum - 1]);
			} else debugPrint(SELF + "for student ", studentId, 
					", no results for assignment ", hwNum);
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + "DBException caught.");
			e.printStackTrace();
		} catch (ParameterException e) {
			Utils.alwaysPrint(SELF + "ParameterException caught.");
			e.printStackTrace();
		} // try
	} // putInGradebook(String, int, int, EvalResult)

	/** Finds out if any of the current student's responses in the current
	 * assignment require human grading.
	 * @param	studentId	student's login ID
	 * @param	hwNum	serial number of the assignment (1-based)
	 * @return	true if human grading is needed
	 * @throws	ParameterException	if the hwId can't be found
	 */
	public boolean getHumanGradingReqd(String studentId, int hwNum) 
			throws ParameterException {
		final String SELF = "GradeSet.getHumanGradingReqd: ";
		final int hwId = getHWId(hwNum);
		final List<String> oneHWHumanGradingReqd =
					humanGradingReqd.get(Integer.valueOf(hwId));
		return oneHWHumanGradingReqd != null
				&& oneHWHumanGradingReqd.contains(studentId);
	} // getHumanGradingReqd(String, int)

	/** Turns off the human grading required flag for a particular student and
	 * assignment.
	 * @param	studentId	student's login ID
	 * @param	hwNum	serial number of the assignment (1-based)
	 * @throws	ParameterException	if the hwId can't be found
	 */
	public void unsetHumanGradingReqd(String studentId, int hwNum) 
			throws ParameterException {
		final String SELF = "GradeSet.unsetHumanGradingReqd: ";
		final Integer hwIdObj = Integer.valueOf(getHWId(hwNum));
		final List<String> oneHWHumanGradingReqd =
					humanGradingReqd.get(hwIdObj);
		if (oneHWHumanGradingReqd != null
				&& oneHWHumanGradingReqd.contains(studentId)) {
			oneHWHumanGradingReqd.remove(studentId);
			if (oneHWHumanGradingReqd.isEmpty()) {
				humanGradingReqd.remove(hwIdObj);
			} // if there are no more students needing human grading
		} // if student was marked for human grading required
	} // unsetHumanGradingReqd(String, int)

} // GradeSet
