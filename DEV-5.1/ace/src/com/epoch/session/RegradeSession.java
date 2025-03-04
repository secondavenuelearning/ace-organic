package com.epoch.session;

import com.epoch.assgts.Assgt;
import com.epoch.db.HWRead;
import com.epoch.db.HWWrite;
import com.epoch.db.QuestionRW;
import com.epoch.db.ResponseRead;
import com.epoch.db.ResponseWrite;
import com.epoch.evals.EvalResult;
import com.epoch.evals.evalConstants.EvalResultConstants;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.NonExistentException;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.ResponseFormatException;
import com.epoch.exceptions.ResponseParseException;
import com.epoch.qBank.Question;
import com.epoch.substns.SubstnUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Used to perform the forced regrade on a single question of an assignment
 * when the question is changed after students have already begun to work it and
 * the grades of some responses may change.  */
public class RegradeSession implements EvalResultConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Unique ID number of the assignment. */
	transient private final int hwId;
	/** 1-Based serial number of the assignment. */
	transient private final int hwNum;
	/** Unique ID number of the question to be regraded; 0 if all in the
	 * assignment are to be regraded. */
	transient private final int qIdOrAll;
	/** Login ID of the instructor. */
	transient private final String instructorId;
	/** Login ID of the one student whose grades are being recalculated, or null
	 * if all students' grades are being recalculated. */
	transient private final String studentId;
	/** Used to calculate modified grades. */
	transient private final GradeSet calcGrade;
	/** Course ID number from which to retrieve students whose grades need
	 * recalculating. */
	transient private final int courseId;

	/** Establishes a session for regrading a question in an assignment after it
	 * has been edited.
	 * @param	hwId	ID number of the assignment
	 * @param	hwNum	1-based serial number of the assignment
	 * @param	qId	the unique ID number of the question to regrade, or 0 if all
	 * in the assignment are to be regraded
	 * @param	instrId	login ID of the instructor
	 * @param	studId	login ID of the one student whose grades are being
	 * recalculated, or null if all students' grades are being recalculated
	 * @param	gset	used to recalculate modified grades
	 */
	public RegradeSession(int hwId, int hwNum, int qId, String instrId, 
			String studId, GradeSet gset) {
		this.hwId = hwId;
		this.hwNum = hwNum;
		qIdOrAll = qId;
		instructorId = instrId;
		studentId = studId;
		calcGrade = gset;
		courseId = calcGrade.getCourseId();
	} // RegradeSession(int, int, int, String, String, GradeSet)

	/** Reevaluates all responses to one or all questions in an assignment
	 * and rewrites the changed EvalResults.
	 * @return	userIds of those students whose grades were changed.
	 * @throws	NonExistentException	if the question does not exist
	 * @throws	ParameterException	if the question has not been answered
	 * previously
	 * @throws	DBException	if the database can't be read or written to
	 */
	public String[] doRegrade() 
			throws DBException, NonExistentException, ParameterException {
		final String SELF = "RegradeSession.doRegrade: ";
		if (qIdOrAll != 0) return doRegrade(qIdOrAll);
		debugPrint(SELF + "regrading ALL questions in assignment with ID ",
				hwId);
		final Assgt assgt = HWRead.getHW(hwId);
		final List<Integer> qIds = assgt.getQIds();
		final List<String> changedUserIds = new ArrayList<String>();
		for (final Integer qId : qIds) {
			debugPrint(SELF + "regrading Q #", qId);
			final String[] addlChangedUserIds = doRegrade(qId.intValue());
			for (final String addlChangedUserId : addlChangedUserIds) {
				if (!changedUserIds.contains(addlChangedUserId)) {
					changedUserIds.add(addlChangedUserId);
				} // if this student's grade hasn't changed already
			} // for each student whose grade has changed this time around
		} // for each Q
		assgt.setDelayGrading(false);
		HWWrite.setFlags(assgt); // turns off delayed grading
		return changedUserIds.toArray(new String[changedUserIds.size()]);
	} // doRegrade()

	/** Reevaluates all responses to one question in an assignment
	 * and rewrites the changed EvalResults in the gradebook.
	 * @param	qId	unique question ID of the Q currently being regraded
	 * @return	userIds of those students whose grades of their most recent
	 * responses were changed
	 * @throws	NonExistentException	if the question does not exist
	 * @throws	ParameterException	if the question has not been answered
	 * previously
	 * @throws	DBException	if the database can't be read or written to
	 */
	public String[] doRegrade(int qId) 
			throws DBException, NonExistentException, ParameterException {
		final String SELF = "RegradeSession.doRegrade: ";
		final Question question = QuestionRW.getQuestion(qId, instructorId);
		if (question == null) 
			throw new NonExistentException("ACE could not find question " 
					+ qId + ".");
		// get the students who have responded to this question and the
		// evaluation results of all of their responses
		final boolean usesSubstns = question.usesSubstns();
		final Map<String, EvalResult[]> allStudentsOneQResults = 
				(studentId == null 
				? ResponseRead.getAllStudentsOneQ(hwId, qId, courseId)
				: new HashMap<String, EvalResult[]>());
		final Map<String, String[]> allStudentSubstns = 
				(usesSubstns && studentId == null
					? ResponseRead.getStoredSubstns(hwId, qId, courseId)
					: new HashMap<String, String[]>());
		if (studentId != null) {
			final EvalResult[] oneStudentOneQResults = 
					ResponseRead.getResults(studentId, hwId, qId);
			allStudentsOneQResults.put(studentId, oneStudentOneQResults);
			if (usesSubstns) {
				final String[] substns = 
						ResponseRead.getStoredSubstns(hwId, studentId, qId);
				allStudentSubstns.put(studentId, substns);
			} // if this question uses R group substitutions
		} // if regrading just one student
		final Map<String, ArrayList<EvalResult>> allChangedResults = 
				new LinkedHashMap<String, ArrayList<EvalResult>>();
		// walk through each result and reevaluate the responses
		final List<String> studentIds = 
				new ArrayList<String>(allStudentsOneQResults.keySet());
		for (final String studId : studentIds) {
			final ArrayList<EvalResult> oneStudentChangedResults =
					new ArrayList<EvalResult>();
			final EvalResult[] evalResults = 
					allStudentsOneQResults.get(studId);
			for (final EvalResult evalResult : evalResults) {
				debugPrint(SELF + "studentId ", studId, 
						", status = ", evalResult.status);
				if (Utils.among(evalResult.status, EVALUATED, HUMAN_NEEDED)) { 
					// should always be true
					final double oldGrade = evalResult.grade;
					final char oldStatus = evalResult.status;
					try {
						final String[] substns = 
								allStudentSubstns.get(studId);
						question.evaluateResponse(evalResult, 
								question.isNumeric() ? substns
									: SubstnUtils.getRGroupMols(substns));
					} catch (ResponseFormatException e) {
						debugPrint(SELF + "ResponseFormatException");
					} catch (ResponseParseException e) {
						debugPrint(SELF + "ResponseParseException");
					}
					if (oldGrade != evalResult.grade
							|| oldStatus != evalResult.status) {
						final int[] assignedQIds = 
								HWRead.getAssignedQIds(hwId, studId);
						final int qNum = Utils.indexOf(assignedQIds, qId) + 1;
						oneStudentChangedResults.add(evalResult);
						debugPrint(SELF + "student ", studId, 
								" giving ", evalResult.mostRecent 
									? "most recent" : "less-than-recent",
								" response ", evalResult.tries,
								" to Q ", qNum, " (ID ", qId,
								") in assignment ", hwNum, " (ID ", hwId,
								"); old grade = ", oldGrade, 
								", old status = ", oldStatus,
								", new grade = ", evalResult.grade,
								", new status = ", evalResult.status);
					} else debugPrint(SELF + "grade ", oldGrade, 
							" and status ", oldStatus,
							" of student ", studId, 
							" giving ", evalResult.mostRecent 
								? "most recent" : "less-than-recent",
							" response ", evalResult.tries,
							" to Q with ID ", qId,
							" in assignment ", hwNum, " have not changed.");
					if (evalResult.grade == 1.0 && !evalResult.mostRecent) {
						debugPrint(SELF + "have found correct response by ",
								studId, " before reaching most recent; "
								+ "no need to go further.");
						break;
					} // if less-than-most-recent response is correct
				} // if response has been evaluated or needs human evaluation
			} // for each response of this student
			if (!oneStudentChangedResults.isEmpty()) {
				allChangedResults.put(studId, oneStudentChangedResults);
			} // if there are changed results for this student
		} // for each student
		// store the records that have changed, and get back the most recent
		// responses with modified grades; if less-than-recent responses
		// have been marked as correct, more recent responses will be deleted
		final Map<String, EvalResult> mostRecentChangedResults = 
				ResponseWrite.setRegradedResults(hwId, allChangedResults);
		final List<String> mostRecentChangedStudents =
				new ArrayList<String>(mostRecentChangedResults.keySet());
		if (!Utils.isEmpty(mostRecentChangedStudents)) {
			for (final String studId : mostRecentChangedStudents) {
				final EvalResult evalResult = 
						mostRecentChangedResults.get(studId);
				calcGrade.putInGradebook(studId, hwNum, qId, evalResult);
			} // for each changed result
			calcGrade.getHumanGradingReqd();
		} // if some students' grades have changed
		return mostRecentChangedStudents.toArray(
				new String[mostRecentChangedStudents.size()]);
	} // doRegrade(int)

} // RegradeSession
