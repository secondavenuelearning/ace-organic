package com.epoch.session;

import com.epoch.assgts.Assgt;
import com.epoch.assgts.AssgtQGroup;
import com.epoch.db.EnrollmentRW;
import com.epoch.db.HWRead;
import com.epoch.db.HWWrite;
import com.epoch.db.QuestionRW;
import com.epoch.db.ResponseRead;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.NonExistentException;
import com.epoch.exceptions.ParameterException;
import com.epoch.qBank.Question;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.session.sessConstants.HWCreateConstants;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** Session object to create a single assignment.
	Represents a single assignment created by instructor.
*/
public class HWCreateSession implements HWCreateConstants, QuestionConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Description of the assignment. */
	transient public Assgt assgt;

	/** Constructor for creating a new assignment.
	 * @param	instructorId	login ID of instructor
	 */
	public HWCreateSession(String instructorId) {
		assgt = new Assgt();
		assgt.instructorId = instructorId;
		assgt.setName("");
		assgt.setRemarks("");
	} // HWCreateSession(String)

	/** Constructor for modifying an existing assignment.
	 * @param	hwId	unique ID number of the assignment
	 * @throws	DBException	if the database can't be read
	 * @throws	NonExistentException	if there's no such assignment
	 */
	public HWCreateSession(int hwId) throws DBException, NonExistentException {
		assgt = HWRead.getHW(hwId);
		HWRead.getHW(assgt); // gets the details of the questions
	} // HWCreateSession(int)
	
	/** Constructor for duplicating an existing assignment.
	 * @param	hwId	unique ID number of the new assignment
	 * @param	hwSess	the existing assignment; data obtained from database
	 * @throws	DBException	if the database can't be read
	 * @throws	NonExistentException	if there's no such assignment
	 */
	public HWCreateSession(int hwId, HWCreateSession hwSess) 
			throws DBException, NonExistentException {
		HWRead.getHW(hwSess.assgt); // gets the details of the questions
		assgt = new Assgt(hwSess.assgt);
		assgt.id = hwId;
	} // HWCreateSession(int, HWCreateSession)

/* ****************** Short get methods *******************/

	/** Gets the total number of questions in this assignment.
	 * @return	total number of questions
	 */
	public int getNumQGroups() 					{ return assgt.getNumGroups(); }
	/** Gets the total number of questions in this assignment.
	 * @return	total number of questions
	 */
	public int getNumAllQs() 					{ return assgt.getNumAllQs(); } 
	/** Gets the number of questions each student will see.
	 * @return	total number of questions
	 */
	public int getNumQsSeen() 					{ return assgt.getNumQsSeen(); } 
	/** Gets the questions of the assignment.
	 * @return	questions of the assignment
	 */
	public List<AssgtQGroup> getQGroups() 		{ return assgt.getQGroups(); }
	/** Gets a group of questions.
	 * @param	grpNum	1-based index of group
	 * @return	the question group
	 */
	public AssgtQGroup getQGroup(int grpNum) 	{ return assgt.getQGroup(grpNum); }
	/** Gets the maximum grade of this assignment.
	 * @return	maximum grade
	 */
	public double getMaxGrade() 				{ return assgt.getMaxGrade(); } 

/* ******** Assignment creation and modification methods ********/

	/** Adds a question to the assignment.
	 * @param	qId	unique ID of the question to add
	 * @return	value indicating success (0) or failure (&lt; 0) of
	 * the addition
	 * @throws	ParameterException	if the question is null
	 */
	public int addQuestion(int qId) throws ParameterException {
		Question newQ = null;
		try {
			newQ = QuestionRW.getQuestion(qId, assgt.instructorId, ADD_HEADER);
		} catch (DBException e) {
			Utils.alwaysPrint("HWCreateSession.addQuestion: "
					+ "DBException caught when retrieving question ", qId);
		}
		if (newQ == null) return CANNOTFINDQ;
		return addQuestion(newQ);
	} // addQuestion(int)
			
	/** Adds a question to the assignment.
	 * @param	hwQ	the question to add
	 * @return	value indicating success (0) or failure (&lt; 0) of the addition
	 * @throws	ParameterException	if the question is null
	 */
	public int addQuestion(Question hwQ) throws ParameterException {
		final String SELF = "HWCreateSession.addQuestion: ";
		if (hwQ == null)
			throw new ParameterException(SELF + "question input null");
		final int newQId = hwQ.getQId();
		if (newQId == 0) {
			Utils.alwaysPrint(SELF + "hwQ id is 0.");
			return QID_0;
		}
		final Integer newQIdInt = Integer.valueOf(newQId);
		// check whether we already added this question
		if (assgt.getQIds().contains(newQIdInt)) {
			Utils.alwaysPrint(SELF + "hwQ ", newQId, " is already in set.");
			return ALREADY;
		}
		final int oldSize = getNumQsSeen();
		final AssgtQGroup grpOfOneQ = new AssgtQGroup(hwQ);
		assgt.addGroup(grpOfOneQ);
		debugPrint(SELF + "size of question set increased from ", oldSize,
				" to ", getNumQsSeen());
		return OK;
	} // addQuestion(Question)

	/** Removes a group of questions from the assignment.
	 * @param	qNum	1-based serial number of the group of questions to 
	 * remove
	 * @return	ID numbers of removed questions
	 */
	public List<Integer> removeQGroup(int qNum) {
		final AssgtQGroup removedGroup = assgt.removeGroup(qNum);
		final List<Integer> removedQIds = new ArrayList<Integer>();
		if (removedGroup != null) {
			removedQIds.addAll(removedGroup.getQIds());
		} // if a group was removed
		return removedQIds;
	} // removeQGroup(int)

	/** Removes a question within a group from the assignment.
	 * @param	qGrpNum	1-based serial number of the group of questions 
	 * from which the question will be removed
	 * @param	grpQNum	1-based serial number within the group of the question 
	 * to remove
	 * @return	ID number of removed question
	 */
	public int removeGroupQ(int qGrpNum, int grpQNum) {
		return assgt.removeGroupQ(qGrpNum, grpQNum);
	} // removeGroupQ(int, int)

	/** Moves a question group in the assignment.
	 * @param	oldQNum	1-based serial number of the question to move
	 * @param	newQNum	desired 1-based serial number of the question
	 */
	public void moveQuestion(int oldQNum, int newQNum) {
		debugPrint("HWCreateSession.moveQuestion: oldQNum = ",
				oldQNum, ", newQNum = ", newQNum);
		if (newQNum < 1) return;
		final AssgtQGroup qGrp = assgt.removeGroup(oldQNum);
		assgt.insertGroup(newQNum, qGrp);
		assgt.setPtsPerQHaveChanged();
	} // moveQuestion(int, int)

	/** Makes existing questions random.
	 * @param 	randList	list of question groups (by 1-based serial #) 
	 * to make random.
	 * @param 	pick		number of questions to choose from this group
	 */
	 public void makeRandom(List<Integer> randList, int pick) {
		final String SELF = "HWCreateSession.makeRandom: ";
	 	if (!randList.isEmpty()) {
			debugPrint(SELF + "randList = ", randList);
			final int startPos = randList.get(0).intValue();
			final AssgtQGroup newlyRandGrp = assgt.getQGroup(startPos);
			// for each group in reverse order, except the first
			for (int randListNum = randList.size() - 1; randListNum > 0;
						randListNum--) {
				final int posn = randList.get(randListNum).intValue();
				final AssgtQGroup oldQGrp = assgt.removeGroup(posn);
				final int oldGrpNumQs = oldQGrp.getNumQs();
				debugPrint(SELF + "removing group ", posn, " with ",
						oldGrpNumQs, " Qs.");
				for (int oldGrpQNum = oldGrpNumQs; oldGrpQNum > 0; oldGrpQNum--) {
					final Question hwQ = oldQGrp.getQ(oldGrpQNum);
					final int qId = hwQ.getQId();
					debugPrint(SELF + "adding hwQ ", qId, " to random group.");
					newlyRandGrp.insertQ(2, hwQ); // maintains order of questions
				} // for each question
			} // for each group that's being made part of a random group
			final int numNewRandQs = newlyRandGrp.getNumQs();
			newlyRandGrp.setPick(pick < numNewRandQs ? pick
					: numNewRandQs > 1 ? numNewRandQs - 1 : 1);
			debugPrint(SELF + "after job is complete, numGroups = ", 
					assgt.getNumGroups(), " new random group has ",
					numNewRandQs, " Qs.");
		} // if list is not empty
		assgt.setPtsPerQHaveChanged();
	} // makeRandom(List<Integer>, int)
	
	/** Converts a group of random questions from the assignment 
	 * to individual questions.
	 * @param	groupNum	1-based index of the group to be converted
	 */
	public void ungroupRandom(int groupNum) {
		final String SELF = "HWCreateSession.ungroupRandom: ";
		final AssgtQGroup qGrp = assgt.getQGroup(groupNum);
		final int numGrpQs = qGrp.getNumQs();
		if (numGrpQs > 1) {
			debugPrint(SELF + "assignment has ", assgt.getNumGroups(),
					" groups; ungrouping group ", groupNum, 
					" of size ", numGrpQs);
			final String pts = qGrp.getPts();
			for (int grpQNum = numGrpQs; grpQNum > 1; grpQNum--) {
				debugPrint(SELF + "ungrouping group member ", grpQNum);
				final Question randQ = qGrp.removeQ(grpQNum);
				final AssgtQGroup grpOfOneQ = new AssgtQGroup(randQ);
				grpOfOneQ.setPts(pts);
				assgt.insertGroup(groupNum + 1, grpOfOneQ);
			} // for each random Q to be ungrouped
			qGrp.setPick(1);
			qGrp.setBundleSize(1);
			debugPrint(SELF + "after ungrouping, assignment has ", 
					assgt.getNumGroups(), " groups.");
		} // if there are Qs to ungroup
		assgt.setPtsPerQHaveChanged();
	} // ungroupRandom(int)

	/** Gets the number of points for each question in a group of 
	 * questions in the assignment.
	 * @param	groupNum	1-based serial number of the question set among all the
	 * random question sets
	 * @return	the number of points, as a String
	 */
	public String getPts(int groupNum) {
		return assgt.getQGroup(groupNum).getPts();
	} // getPts(int)

	/** Sets the number of points for each question in a group of 
	 * questions in the assignment.
	 * @param	groupNum	1-based serial number of the question group among all the
	 * question groups
	 * @param	pts	the number of points, as a String
	 */
	public void setPts(int groupNum, String pts) {
		final String oldPts = getPts(groupNum);
		if (oldPts == null || !oldPts.equals(pts)) {
			assgt.getQGroup(groupNum).setPts(pts);
			assgt.setPtsPerQHaveChanged();
		} // if points have changed
	} // setPts(int, String)

	/** Gets the number of random question bundles to choose from a group of 
	 * questions in the assignment.
	 * @param	groupNum	1-based serial number of the question set among all the
	 * random question sets
	 * @return	the number of random question bundles to choose
	 */
	public int getBundlesPick(int groupNum) {
		return assgt.getQGroup(groupNum).getPick();
	} // getBundlesPick(int)

	/** Sets the number of random question bundles to choose from a group of 
	 * questions in the assignment.
	 * @param	groupNum	1-based serial number of the question group among all the
	 * question groups
	 * @param	pick	the number of random question bundles to choose from the 
	 * group
	 */
	public void setBundlesPick(int groupNum, int pick) {
		assgt.getQGroup(groupNum).setPick(pick <= 0 ? 1 : pick);
		assgt.setQuestionsHaveChanged();
		assgt.setPtsPerQHaveChanged();
	} // setBundlesPick(int, int)

	/** Gets the size of the question bundles in a group of questions in the 
	 * assignment.
	 * @param	groupNum	1-based serial number of the question set among all the
	 * random question sets
	 * @return	the size of the question bundles
	 */
	public int getBundleSize(int groupNum) {
		return assgt.getQGroup(groupNum).getBundleSize();
	} // getBundleSize(int)

	/** Sets the size of the question bundles in a group of questions in the 
	 * assignment.
	 * @param	groupNum	1-based serial number of the question set among all the
	 * random question sets
	 * @param	size	the size of the question bundles
	 */
	public void setBundleSize(int groupNum, int size) {
		assgt.getQGroup(groupNum).setBundleSize(size <= 0 ? 1 : size);
		assgt.setQuestionsHaveChanged();
		assgt.setPtsPerQHaveChanged();
	} // setBundleSize(int, int)

	/** Sets the ID of the question on which display of this question depends.
	 * @param	groupNum	1-based serial number of the question set among all the
	 * random question sets
	 * @return	the ID of the question on which display of this question depends
	 */
	public int getDependsOn(int groupNum) {
		return assgt.getQGroup(groupNum).getDependsOn();
	} // getDependsOn(int)

	/** Sets the ID of the question on which display of this question depends.
	 * @param	groupNum	1-based serial number of the question set among all the
	 * question sets
	 * @param	newDependsOn	the ID of the question on which display of this
	 * question depends
	 */
	public void setDependsOn(int groupNum, int newDependsOn) {
		final AssgtQGroup qGrp = assgt.getQGroup(groupNum);
		final int oldDependsOn = qGrp.getDependsOn();
		if (oldDependsOn != newDependsOn) {
			qGrp.setDependsOn(newDependsOn);
			assgt.setDependenciesHaveChanged();
		} // if dependsOn has changed
	} // setDependsOn(int, int)

	/** Save the parts of the current assignment handled by the editor's first 
	 * (question-editing) page. Called from hwEditor.jsp and dupeHWSet.jsp only.
	 * @throws	DBException	if the database can't be written to
	 */
	public void save() throws DBException {
		save(EDITOR_PAGE1, assgt.id == 0, false);
	} // save()

	/** Save the parts of the current assignment handled by the editor's second
	 * page. Called from saveHW.jsp and saveExamData.jsp only.
	 * @param	newAssgt	true if on the question-editing page and the 
	 * assignment questions have not previously been saved, or not on the 
	 * question-editing page and the assignment is new
	 * @param	madeVisible	true if the assignment visibility was changed from 
	 * off to on
	 * @throws	DBException	if the database can't be written to
	 */
	public void save(boolean newAssgt, boolean madeVisible) throws DBException {
		save(!EDITOR_PAGE1, newAssgt, madeVisible);
	} // save(boolean, boolean)

	/** Save parts of the current assignment. Called from above.
	 * @param	editorPage1	true if the first (question-editing) page called 
	 * this method
	 * @param	newAssgt	true if editorPage1 and the assignment questions 
	 * have not previously been saved or !editorPage1 and the assignment is new
	 * @param	madeVisible	true if the assignment visibility was changed from 
	 * off to on
	 * @throws	DBException	if the database can't be written to
	 */
	private void save(boolean editorPage1, boolean newAssgt, boolean madeVisible) 
			throws DBException {
		final String SELF = "HWCreateSession.save: ";
		final int numQsSeen = getNumQsSeen();
		if (numQsSeen == 0) return;
		debugPrint(SELF + "saving page ", editorPage1 ? 1 : 2, " of ", 
				newAssgt ? "new" : "modified", " assignment ", assgt.id, 
				": ", assgt.toString());
		if (editorPage1 && newAssgt) {
			HWWrite.addHW(assgt);
		} else {
			HWWrite.setHW(assgt, editorPage1);
			if (editorPage1) {
				if (!assgt.isMasteryAssgt() && assgt.flagsHaveChanged()) {
					HWWrite.removeDependenciesOn(assgt.id);
				} // if modifying questions/points in nonmastery assignment
			} else {
				final boolean newToStudent = newAssgt || madeVisible;
				if (assgt.isVisible() 
						&& (newToStudent || assgt.dueDateHasChanged())) {
					sendDueDateChangeAlert(newToStudent
							? "Your instructor has added an assignment, "
							: "Your instructor has changed the due date "
							+ "or time of the assignment, ");
				} // if due date has changed
			} // if editorPage1
		} // if editorPage1 && newAssgt
		assgt.setNoChanges();
	} // save(boolean, boolean, boolean)

	/** Sends an email to students that an assignment due date has changed.
	 * @param	msg	the message to send to the student
	 * @throws	DBException	if there's a problem reading the database
	 */
	private void sendDueDateChangeAlert(String msg) throws DBException {
		final String SELF = "HWCreateSession.sendDueDateChangeAlert: ";
		final String modMsg = Utils.toString(msg, 
				assgt.getName().replaceAll("'", "\""));
		final String[] emails = 
				EnrollmentRW.getEnrolledUserTextMessageEmails(assgt.courseId);
		debugPrint(SELF + "for course with ID ", assgt.courseId,
				", text message emails are: ", emails, ", message is ",
				modMsg);
		UserSession.sendTextMessages(emails, modMsg);
	} // sendDueDateChangeAlert(String)

	/** Gets if there have been any responses to questions with the given IDs.
	 * @param	qIds	ID numbers of the questions
	 * @return	true if there have been any responses to any of the questions
	 * with the given IDs
	 * @throws	DBException	if the database can't be read
	 */
	public boolean haveResponses(List<Integer> qIds) throws DBException {
		return ResponseRead.haveResponses(assgt.id, qIds);
	} // haveResponses(List<Integer>)

} // HWCreateSession

