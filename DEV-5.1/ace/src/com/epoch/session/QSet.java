package com.epoch.session;

import com.epoch.constants.AppConstants;
import com.epoch.db.QSetRW;
import com.epoch.db.QuestionRW;
import com.epoch.db.ResponseLogger;
import com.epoch.evals.Evaluator;
import com.epoch.exceptions.ConfigurationException;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.FileFormatException;
import com.epoch.exceptions.InvalidOpException;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.ParserException;
import com.epoch.qBank.QSetDescr;
import com.epoch.qBank.Question;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.qBank.Topic;
import com.epoch.qBank.qBankConstants.TopicQSetConstants;
import com.epoch.responses.StoredResponse;
import com.epoch.translations.QTranslns;
import com.epoch.translations.QSetAllTranslns;
import com.epoch.translations.TranslnsMap;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.XMLUtils;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** A question set for editing. Corresponds to the authoring session.  */
public class QSet 
		implements AppConstants, QuestionConstants, TopicQSetConstants {

	protected static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Questions in this set; merge of master and local. */
	transient List<Question> setQs;
	/** Unique IDs of questions in this set. */
	transient List<Integer> setQIds;
	/** Whether each question in this set has been assigned as part of a random
	 * set of questions in an assignment.  Loaded on demand.  */
	transient boolean[] assignedAsRandom = null;
	/** Description of this question set. */
	transient QSetDescr qSetDescr;

	/** 0-based serial number of last getQuestion(). */
	transient int currentQNum = 0;
	/** The last getQuestion(). */
	private transient Question currentQ;

	/** Login ID of the instructor authoring in this question set. */
	public transient String instructorId = MASTER_AUTHOR;
	/** Unique ID of this question set. */
	transient int setId;
	/** Whether this question set is being master- or locally edited. */
	transient boolean masterEdit;
	/** Error message from last database operation. */
	transient String dbMessage = "";

	/** All responses to the current question, for the feedback improver. */
	transient StoredResponse[] storedResps = null;
	/** Number of correct responses to the current question, for the
	 * feedback improver. */
	transient int correctStoredRespsCt;
	/** Number of incorrect responses to the current question, for the
	 * feedback improver. */
	transient int wrongStoredRespsCt;
	/** Unique ID of the question for which above info is loaded (to avoid reload). */
	transient int storeLoadedQId;

	/** Basic constructor. */
	public QSet() {
		setQs = new ArrayList<Question>();
		setQIds = new ArrayList<Integer>();
		qSetDescr = new QSetDescr();
	} // QSet()

	/**  Constructor.
	 * @param	instructId	login ID of the instructor
	 */
	public QSet(String instructId) {
		masterEdit = false;
		instructorId = instructId;
		setQs = new ArrayList<Question>();
		setQIds = new ArrayList<Integer>();
		qSetDescr = new QSetDescr();
	} // QSet(String)

	/** Initialize a question set (qSet) for editing the master copy.
	 * @param	set_id	unique ID of the master question set
	 * @throws	DBException	if there's a problem reading the database
	 */
	public QSet(int set_id) throws DBException {
		masterEdit = true;
		setId = set_id;
		initialize();
	} // QSet(int)
	
	/**  Initialize a question set (qSet) in database, for editing/viewing local copy.
	 * @param	instructId	login ID of the instructor
	 * @param	set_id	unique ID of the qSet (in local/master)
	 * @throws	DBException	if there's a problem reading the database
	 */
	public QSet(int set_id, String instructId) throws DBException {
		masterEdit = false;
		instructorId = instructId;
		setId = set_id;
		initialize();
	} // QSet(int, String)

	/** Actual initialization routine called by the constructors.  Loads all
	 * master-authored questions in the set.
	 * @throws	DBException	if there's a problem reading the database
	 */
	private void initialize() throws DBException {
		final String SELF = "QSet.initialize: ";
		// load the master questions
		debugPrint(SELF + "getting master questions for question set ", setId);
		setQs = QuestionRW.getQuestions(setId);
		setQIds = new ArrayList<Integer>();
		for (final Question oneQ : setQs) {
			setQIds.add(Integer.valueOf(oneQ.getQId()));
		} // for each question
		qSetDescr = QSetRW.getQSetDescr(setId, masterEdit 
				? MASTER_AUTHOR : instructorId);
		if (!masterEdit) {
			// load the local questions 
			debugPrint(SELF + "getting local questions for question set ", setId);
			final List<Question> setLocalQs =
					QuestionRW.getQuestions(setId, instructorId);
			initializeSets(setLocalQs);
		} // not master edit
	} // initialize()
	
	/** Inserts locally authored questions into the set; overrides master
	 * questions with locally modified versions.
	 * @param	setLocalQs	locally authored and modified questions of this set
	 * @throws	DBException	if there's a problem reading the database
	 */
	final protected void initializeSets(List<Question> setLocalQs)
			throws DBException {
		final String SELF = "QSet.initializeSets: ";
		debugPrint(SELF + "LOCAL set size = ", setLocalQs.size(), 
				"; instructorId = ", instructorId);
		for (final Question localQ : setLocalQs) {
			final int localQId = localQ.getQId();
			if (localQId < 0) { // locally authored
				// Ordering is assured since abs(id) is considered
				debugPrint(SELF + "adding locally authored Q with qId = ", 
						localQId, " to set.");
				localQ.setEditType(NEW);
				setQs.add(localQ);
				setQIds.add(Integer.valueOf(localQId));
			} else { // modified version of database Q
				// Override master questions with modified local questions
				final int posn = setQIds.indexOf(Integer.valueOf(localQId));
				if (posn >= 0) {
					debugPrint(SELF + "overriding db Q ", posn + 1, " with qId = ", 
							localQId, " with local, modified version.");
					localQ.setEditType(CHANGED);
					setQs.set(posn, localQ);
				} else Utils.alwaysPrint(SELF + "locally modified Q with qId = ",
						localQId, " has no corresponding ",
						" master db version in qSet ", setId,
						", '", qSetDescr.name, "'.");
			} // sign of localQ qId
		} // for each question localQ in localSet
		debugPrint(SELF + "setQIds = ", setQIds);
		// misc initialization
		correctStoredRespsCt = 0;
		wrongStoredRespsCt = 0;
		storeLoadedQId = 0;
	} // initializeSets(List<Question>)

/* ************** Reading methods **************/

	/** Returns description of this question set.
	 * @return	the description of this question set
	 */
	public QSetDescr getQSetDescr()			{ return qSetDescr; }
	/** Returns serial number of the most recently edited question in this set.
	 * @return	serial number of the current Q
	 */
	public int getCurrentIndex() 			{ return currentQNum + 1; }
	/** Returns the number of questions in this question set.
	 * @return	the number of Qs in this set
	 */
	public int getCount() 					{ return setQs.size(); }
	/** Returns the unique ID number of this question set.
	 * @return	the unique ID number of this set
	 */
	public int getQSetId() 					{ return setId; }
	/** Returns the login ID of the instructor
	 * authoring in this question set.
	 * @return	the ID of the instructor authoring in this question set
	 */
	public String getInstructorId()			{ return instructorId; }
	/** Returns the messages of any of the last setQuestion/addQuestion call.
	 * @return	the message from the database operation
	 */
	public String getDbMessage() 			{ return dbMessage; }
	/** Returns the list of questions of this question set.
	 * @return	the list of questions of this set
	 */
	public List<Question> getSetQs() 		{ return setQs; }
	/** Returns the list of unique ID numbers of this question set.
	 * @return	the list of unique ID numbers of this set
	 */
	public List<Integer> getSetQIds() 		{ return setQIds; }
	/** Gets if this question set is master-edited.
	 * @return	true if this question set is master-edited
	 */
	public boolean isMasterEdit() 			{ return masterEdit; }

	/** Gets the serial number of the question with the given ID.
	 * @param	qId	unique ID of the question
	 * @return	1-based serial number of the question in the set
	 */
	public int getQNum(int qId) { 
		return setQIds.indexOf(Integer.valueOf(qId)) + 1;
	} // getQNum(int)

	/** Returns the unique ID of the question in the set following the current
	 * one; if it is the last, returns 0.
	 * @return	unique ID number of the next question
	 */
	public int getNextQId() {
		debugPrint("QSet.getNextQId: setQs.size() = ",
				setQs.size(), ", currentQNum = ", currentQNum);
		return (setQs.isEmpty() || currentQNum + 1 >= setQs.size() ? 0 
				: setQs.get(currentQNum + 1).getQId());
	} // getNextQId()

	/** Returns the unique ID of the question in the set preceding the current
	 * one; if it is the first, returns 0.
	 * @return	unique ID number of the previous question
	 */
	public int getPrevQId() {
		return (setQs.isEmpty() || currentQNum == 0 ? 0 
				: setQs.get(currentQNum - 1).getQId());
	} // getPrevQId()

	/** Returns the unique ID of a question in this set by its serial number.
	 * @param	qNum	1-based serial number of the question
	 * @return	unique ID number of the question
	 * @throws	InvalidOpException	if the question serial number is invalid or
	 * the question set is empty
	 */
	public int getQId(int qNum) throws InvalidOpException {
		if (setQs.isEmpty())
			throw new InvalidOpException(" Question set is empty ");
		if (qNum <= 0 || qNum > setQs.size())
			throw new InvalidOpException(" Invalid serial number " + qNum
					+ " to getQId(); must be 1 to " + setQs.size());
		return setQIds.get(qNum - 1).intValue();
	} // getQId(int)

	/** Returns light-load Qs of this question set.
	 * @return	array of questions
	 */
	public Question[] getQuestions() {
		return (setQs == null ? null 
				: setQs.toArray(new Question[setQs.size()]));
	} // getQuestions()

	/** Gets a question in the set and sets it as the current question.  It is
	 * not safe to perform operations on the returned object, if the class is
	 * bound to session.  Null is returned if the current question is corrupted
	 * or the question does not exist.  
	 * @param	qId	unique ID number of the question to get
	 * @return	the desired question
	 * @throws	InvalidOpException	if the question set is empty
	 */
	public Question getQuestion(int qId) throws InvalidOpException {
		if (setQs.isEmpty())
			throw new InvalidOpException("Question set is empty");
		final int qNum = getQNum(qId);
		debugPrint("QSet.getQuestion: qId ", qId, " has qNum ", qNum);
		return getQuestionBySerialNo(qNum);
	} // getQuestion(int)

	/** Gets a question in the set and sets it as the current question.  It is
	 * not safe to perform operations on the returned object, if the class is
	 * bound to session.  Null is returned if the current question is corrupted
	 * or the question does not exist.
	 * @param	qNum	1-based serial number of the question in the set
	 * @return	the desired question
	 * @throws	InvalidOpException	if the question serial number is invalid or
	 * the question set is empty
	 */
	public Question getQuestionBySerialNo(int qNum) throws InvalidOpException {
		if (setQs.isEmpty())
			throw new InvalidOpException("Question set is empty");
		// advance currentQNum to point to appropriate item in setQs
		if (qNum <= 0 || qNum > setQs.size()) {
			throw new InvalidOpException(" Invalid qNum " + qNum
					+ " to getQuestion(): must be 1 to " + setQs.size());
		}
		currentQNum = qNum - 1;
		// duplicate Q so changes can be discarded if not saved
		currentQ = new Question(setQs.get(qNum - 1), PRESERVE_ID);
		debugPrint("QSet.getQuestion: question ", currentQ.getQId(),
				" with serial number ", qNum, " has qType = ",
				currentQ.getQType(), ", qFlags = ", currentQ.getQFlags());
		return currentQ;
	} // getQuestionBySerialNo(int)

	/** Gets whether a question in this set has been made part of a random 
	 * group in an assignment.
	 * @param	qNum	1-based serial number of the question in the set
	 * @return	true if the question has been made part of a random group in an
	 * assignment
	 * @throws	DBException	if there's a problem reading the database
	 */
	public boolean isAssignedAsRandom(int qNum) throws DBException {
		if (Utils.isEmpty(assignedAsRandom)) {
			assignedAsRandom = 
					QuestionRW.areAssignedAsRandom(setQIds, instructorId);
		} // if assignedAsRandom hasn't yet been determined
		return assignedAsRandom[qNum - 1];
	} // isAssignedAsRandom(int)

/* ************** Editing methods **************/

	/** Resets a question in the set to the database version.
	 * @param	qId	unique ID number of the question to reset
	 * @return	the reset question
	 * @throws	InvalidOpException	if the question set is empty
	 */
	public Question resetQuestion(int qId) throws InvalidOpException {
		final String SELF = "QSet.resetQuestion: ";
		if (setQs.isEmpty())
			throw new InvalidOpException("Question set is empty");
		final String instructId = (masterEdit ? MASTER_AUTHOR : instructorId);
		try {
			final Question resetQ = QuestionRW.getQuestion(qId, instructId,
					!ADD_HEADER, FULL_LOAD);
			final int qNum = getQNum(qId);
			debugPrint(SELF + "qId ", qId, " has qNum ",
					qNum, "; resetting to database value");
			setQs.set(qNum - 1, resetQ);
			return getQuestionBySerialNo(qNum);
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + "DBException "
					+ "caught when retrieving Q ", qId, " from database.");
		}
		return null;
	} // resetQuestion(int)

	/** Saves an existing question in the database.
	 * Messages can be seen by calling getDBMessage().
	 * @param	question	the question to be saved
	 * @return	the saved question, or null if it could not be saved
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	InvalidOpException	if the question serial number is invalid or
	 * the question set is empty
	 */
	public Question setQuestion(Question question) throws DBException, 
			InvalidOpException {
		if (setQs.isEmpty())
			throw new InvalidOpException(" Question set is empty ");
		if (!question.validate()) {
			dbMessage = question.miscMessage;
			return null;
		}
		final int qId = question.getQId();
		final int changeIndex = setQIds.indexOf(Integer.valueOf(qId));
		if (changeIndex < 0) {
			throw new InvalidOpException(Utils.toString(" Invalid qId ", qId,
					" to setQuestion(): valid qIds are ", setQIds.toString()));
		}
		Question savedQ = null;
		if (masterEdit) {
			debugPrint(" Going to change current question with qType = ",
					currentQ.getQType());
			currentQ = QuestionRW.setQuestion(question);
			// Change display?
			dbMessage = currentQ.miscMessage;
			debugPrint(" ... successfully changed, qType = ",
					currentQ.getQType());
			setQs.set(changeIndex, currentQ);
			savedQ = currentQ;
		} else { // local edit
			final Question loadedCurrentQ = setQs.get(changeIndex);
			Question changedQ = null;
			if (loadedCurrentQ.isMaster()) {
				// This question must be installed in the local table
				changedQ = QuestionRW.addQuestion(question,
						setId, instructorId, PRESERVE_ID);
			} else {
				// Local table already contained this question, just change it
				changedQ = QuestionRW.setQuestion(question,
						instructorId);
			}
			dbMessage = changedQ.miscMessage;
			changedQ.setEditType(loadedCurrentQ.isNew() ? NEW : CHANGED);
			setQs.set(changeIndex, changedQ);
			savedQ = changedQ;
		}
		return savedQ;
	} // setQuestion(Question)

	/** Adds a question.  Messages can be obtained by calling getDbMessage().
	 * If the question was duplicated from another question, a new qId is
	 * assigned when it is about to be written to the database.  Any
	 * translations associated with unmodified portions of the duplicated
	 * question are preserved and assigned new phraseId values.
	 * @param	question	the question to be saved
	 * @return	the added question, or null if it could not be saved
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	ParameterException	never
	 */
	public Question addQuestion(Question question) throws DBException,
			ParameterException {
		final String SELF = "QSet.addQuestion: ";
		final boolean valid = question.validate();
		debugPrint(SELF + "valid = ", valid);
		if (!valid) {
			dbMessage = question.miscMessage;
			return null;
		}
		Question storedQ = null;
		if (masterEdit) {
			debugPrint(SELF + "Adding the question to the "
					+ "database and to setQs.");
			final QTranslns qTranslns = question.getAllTranslations();
			storedQ = QuestionRW.addQuestion(question, setId);
			dbMessage = storedQ.miscMessage;
			setQs.add(storedQ);
			setQIds.add(Integer.valueOf(storedQ.getQId()));
			if (qTranslns != null) {
				debugPrint(SELF + "adding copied translations.");
				qTranslns.storeFor(storedQ, instructorId);
			} else debugPrint(SELF + "no translations to add.");
		} else {
			storedQ = QuestionRW.addQuestion(question, setId,
					instructorId, !PRESERVE_ID);
			dbMessage = storedQ.miscMessage;
			storedQ.setEditType(NEW);
			setQs.add(storedQ);
			setQIds.add(Integer.valueOf(storedQ.getQId()));
		} // if master edit
		currentQNum = setQs.size() - 1;
		currentQ = storedQ;
		assignedAsRandom = null;
		return storedQ;
	} // addQuestion(Question)

	/** Deletes a question from the database and the question set.
	 * @param	qId	unique ID of the question to be saved
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	InvalidOpException	if the question is not valid
	 */
	public void deleteQuestion(int qId) throws DBException,
			InvalidOpException {
		final int delIndex = setQIds.indexOf(Integer.valueOf(qId));
		if (delIndex < 0) {
			throw new InvalidOpException(" Invalid qId " + qId
					+ " to deleteQuestion(): valid qIds are "
					+ setQIds.toString());
		}
		if (masterEdit) {
			QuestionRW.deleteQuestion(qId);
		} else {
			// allow the deletion of only newly added questions.
			final Question delQ = setQs.get(delIndex);
			if (!delQ.isNew()) { // shouldn't happen
				throw new InvalidOpException(" Only locally added "
						+ "questions can be deleted" );
			}
			QuestionRW.deleteQuestion(qId, instructorId);
		}
		setQs.remove(delIndex);
		setQIds.remove(delIndex);
		assignedAsRandom = null;
		debugPrint("QSet.deleteQuestion: size = ", setQs.size(),
				", currentQNum = ", currentQNum);
		// deletions are done in whole view. so currentQNum does not matter
		// safely put it to 0
		currentQNum = 0;
	} // deleteQuestion(int)

	/** Renumbers a question within the set.  Front-end code already ensures
	 * that locally authored questions must remain positioned after
	 * master-authored questions.
	 * @param	from	1-based index of question to renumber
	 * @param	to	1-based new number of question
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public void renumberQuestion(int from, int to) throws DBException {
		final String SELF = "QSet.renumberQuestion: ";
		final boolean isLocal = instructorId != MASTER_AUTHOR;
		debugPrint(SELF + "moving ", (isLocal ? "locally " : "master-"),
				"authored Q", from, " to ", to);
		setQs.add(to - 1, setQs.remove(from - 1));
		setQIds.add(to - 1, setQIds.remove(from - 1));
		final List<Integer> renumberedQIds = new ArrayList<Integer>();
		for (final Integer setQId : setQIds) {
			if (!isLocal || setQId.intValue() < 0) renumberedQIds.add(setQId);
		} // for each Q in set
		final int[] qIdsArr = Utils.listToIntArray(renumberedQIds);
		debugPrint(SELF + "new order of ", (isLocal ? "locally " : "master-"), 
				"authored qIds in this set: ", qIdsArr);
		QuestionRW.setQSerialNos(qIdsArr, instructorId);
		assignedAsRandom = null;
	} // renumberQuestion(int, int)

	/** Moves questions from this set into a new one.
	 * @param	serialNos	1-based serial numbers of the questions to be moved
	 * @param	qSetId	unique ID of the question set into which the questions
	 * will be moved
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	InvalidOpException	if the question is not valid
	 */
	public void moveQtoNewQset(String serialNos, int qSetId)
			throws DBException, InvalidOpException {
		final String SELF = "QSet.moveQtoNewQset: ";
		if (Utils.isEmpty(serialNos))
			throw new InvalidOpException("No questions to move.");
		final String[] qNumStrs = serialNos.split(":");	
		final int[] qNums = new int[qNumStrs.length]; // 1-based!
		final int[] qIds = new int[qNumStrs.length];
		final StringBuilder out = Utils.getBuilder(SELF + "will move");
		for (int qNum = 0; qNum < qNumStrs.length; qNum++) {
			qNums[qNum] = Integer.parseInt(qNumStrs[qNum]); // 1-based!
			qIds[qNum] = setQIds.get(qNums[qNum] - 1);
			Utils.appendTo(out, " Q ", qNums[qNum], " with unique ID ", 
					qIds[qNum], qNum < qNumStrs.length - 1 ? ", " : '.');
		} // for each Q to move
		debugPrint(out);
		// Qs are all local or all master because local instructors can move
		// only self-authored Qs
		final Question moveQ = setQs.get(qNums[0] - 1);
		final boolean isLocal = moveQ.isNew();
		debugPrint(SELF + "moving Qs ", serialNos,
				" that are ", (isLocal ? "" : "not "), "local.");
		QuestionRW.moveQtoNewQset(qIds, qSetId, isLocal);
		for (int qNum = qNumStrs.length - 1; qNum >= 0; qNum--) {
			setQs.remove(qNums[qNum] - 1);
			setQIds.remove(qNums[qNum] - 1);
		}
		assignedAsRandom = null;
		debugPrint(SELF + "Success! Current qSet now has ",
				setQs.size(), " members.");
	} // moveQtoNewQset(int, int)

	/** Reverts a locally modified question back to the master copy
	 * by deleting the local copy.
	 * @param	qId	unique ID of the question to be reverted
	 * @throws	DBException	if there's a problem writing to the database
	 * @throws	InvalidOpException	if the question is not valid
	 */
	public void revertQuestion(int qId) throws DBException, InvalidOpException {
		if (masterEdit) return;
		final int revIndex = setQIds.indexOf(Integer.valueOf(qId));
		if (revIndex < 0) {
			throw new InvalidOpException(" Invalid qId " + qId
					+ " to revertQuestion(): valid qIds are "
					+ setQIds.toString());
		}
		final Question revQ = setQs.get(revIndex);
		if (!revQ.isModified()) { // shouldn't happen
			throw new InvalidOpException("Only locally modified "
					+ "questions can be reverted ");
		}
		final Question originalQ = QuestionRW.getQuestion(revQ.getQId(),
				!ADD_HEADER);
		if (originalQ == null) {
			throw new DBException(" The original question has been deleted");
		}
		QuestionRW.deleteQuestion(revQ.getQId(), instructorId);
		originalQ.setEditType(SAME);
		setQs.set(revIndex, originalQ);
	} // revertQuestion(int)

	/** Stores a locally modified common question statement.
	 * @param	newHeader	the local version of the common question statement
	 */
	public void addLocalHeader(String newHeader) {
		try {
			QSetRW.addLocalHeader(instructorId, setId,
					newHeader);
			qSetDescr.headerModifiedLocally = true;
			qSetDescr.header = newHeader;
		} catch (DBException e) {
			Utils.alwaysPrint("QSet.addLocalHeader: "
					+ "threw DBException while storing local "
					+ "description; leaving descr unchanged.");
		}
	} // addLocalHeader(String)

	/** Sets the common question statement.  It has already been stored in the
	 * database.  Called by <code>authortool/saveStatement.jsp</code>.
	 * @param	newHeader	the new common question statement
	 */
	public void setHeader(String newHeader) {
		qSetDescr.header = newHeader;
	} // setHeader(String)

	/** Reverts the locally modified common question statement of
	 * this question set to the master copy.  
	 * @return	the QSetDescr with the restored common question statement
	 */
	public QSetDescr revertQSetDescr() {
		try {
			QSetRW.removeLocalHeader(instructorId, setId);
			qSetDescr = QSetRW.getQSetDescr(setId, MASTER_AUTHOR);
		} catch (DBException e) {
			Utils.alwaysPrint("QSet.revertQSetDescr: "
					+ "threw DBException while reverting to master "
					+ "description; leaving descr unchanged.");
		}
		return qSetDescr;
	} // revertQSetDescr()

/* ************** View responses methods **************/

	/** Loads all the response history of the current question.
	 * Must be called before calling getStoredResponses().  Cannot apply to
	 * R-group questions because each response will have different R groups
	 * associated with them, and they are not stored with the logged responses.
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	InvalidOpException	if the question is not valid
	 * @throws	ParameterException	if an invalid index to an evaluator,
	 * question datum, or evaluator is given
	 */
	public void loadStoredResponses() throws InvalidOpException,
			DBException, ParameterException {
		final String SELF = "QSet.loadStoredResponses: ";
		if (currentQ == null)
			throw new InvalidOpException(" getStoredResponse() with no prob");
		final int currentQId = currentQ.getQId();
		// load only if this is not loaded already
		if (storeLoadedQId == currentQId) {
			debugPrint(SELF + "stored responses are already loaded.");
			return;
		}
		final int qType = currentQ.getQType();
		final long qFlags = currentQ.getQFlags();
		storedResps = null;
		correctStoredRespsCt = 0;
		wrongStoredRespsCt = 0;
		storeLoadedQId = currentQId;
		try {
			if (masterEdit) {
				storedResps = 
						ResponseLogger.getResponses(currentQId, qType, qFlags);
			} else {
				final Question currentQ = setQs.get(currentQNum);
				storedResps = (currentQ.isMaster()
						? ResponseLogger.getResponses(currentQId, qType, qFlags)
						: ResponseLogger.getResponses(currentQId,
								instructorId, qType, qFlags));
			} // if masterEdit
		} catch (IllegalStateException e) {
			Utils.alwaysPrint(SELF + "threw "
					+ "channel exception when trying to get responses, "
					+ "meaning file was already open.");
			return;
		} catch (ConfigurationException e) {
			Utils.alwaysPrint(SELF + "threw "
					+ "configuration exception when trying to get responses, "
					+ "meaning file could not be found.");
			return;
		}
		if (storedResps == null) return;
		int respNum = 0;
		for (final StoredResponse storedResp : storedResps) {
			if (storedResp == null) continue;
			String responseStr = storedResp.response;
			if ((currentQ.isChoice() || currentQ.isFillBlank())
					&& responseStr.indexOf('+') < 0) { // FILLBLANK by Jay
				responseStr = ":" + responseStr + ":";
				final StringBuilder respBld = new StringBuilder();
				final int numQData = currentQ.getNumQData(GENERAL);
				for (int qdNum = 0; qdNum < numQData; qdNum++) {
					final String srch = Utils.toString(':', qdNum, ':');
					Utils.appendTo(respBld, qdNum, 
							responseStr.indexOf(srch) >= 0 ? "+:" : ':');
				} // for each Qdata number
				responseStr = respBld.toString();
			} else if (currentQ.isRank()
					&& (responseStr.indexOf(';') < 0
						|| responseStr.indexOf(':') < 0)) {
				responseStr = responseStr.replace(':', ';');
				if (!responseStr.endsWith(";")) responseStr += ';';
				int pos = 0;
				int ct = 1;
				while (pos < responseStr.length()
						&& responseStr.substring(pos).indexOf(';') >= 0) {
					pos += responseStr.substring(pos).indexOf(';');
					responseStr = Utils.toString(responseStr.substring(0, pos),
							':', ct, responseStr.substring(pos));
					ct++;
					pos += responseStr.substring(pos).indexOf(';') + 1;
				} // while more options are ranked
			} // if Q type is choice, fill in the blank, or rank
			debugPrint(SELF + "the ", ++respNum, 
					"th stored response is ", storedResp.response,
					" , submitting ", responseStr, " for evaluation");
			final int[] matchingEvaluatorIds = 
					currentQ.firstEvaluatorMatching(responseStr);
			if (matchingEvaluatorIds.length == 0) {
				// no evaluator matches
				debugPrint(SELF + "no evaluator matches");
				storedResp.type = StoredResponse.WRONG;
				storedResp.feedbackExists = false;
				wrongStoredRespsCt++;
			} else {
				final Evaluator matchingEval = 
						currentQ.getEvaluator(matchingEvaluatorIds[0]);
				storedResp.matchingEvaluatorMajorId = matchingEvaluatorIds[0];
				storedResp.matchingEvaluatorMinorId = matchingEvaluatorIds[1];
				storedResp.feedbackExists = true;
				storedResp.feedback = matchingEval.feedback;
				if (matchingEval.grade > 0) {
					// fully or partially correct
					debugPrint(SELF + "into fully or part correct");
					storedResp.type = StoredResponse.CORRECT;
					correctStoredRespsCt++;
				} else { // wrong, but caught
					debugPrint(SELF + "into wrong, but caught");
					storedResp.type = StoredResponse.WRONG;
					wrongStoredRespsCt++;
				} // if grade
			} // if a match was found
		} // each StoredResponse
		debugPrint(SELF + "history loaded; counts = ", correctStoredRespsCt,
				", ", wrongStoredRespsCt);
	} // loadStoredResponses()

	/** Gets the correct|wrong responses to the current question.
	 * @param	type	correct or wrong
	 * @return	array of correct or incorrect responses
	 * @throws	InvalidOpException	if the responses haven't been loaded
	 * @throws	ParameterException	if the type value is invalid
	 */
	public StoredResponse[] getStoredResponses(int type) 
		throws InvalidOpException, ParameterException {
		final String SELF = "QSet.getStoredResponses: ";
		if (!Utils.among(type, StoredResponse.CORRECT, StoredResponse.WRONG))
			throw new ParameterException(SELF + "wrong param ");
		// is the history already loaded?
		final int currentQId = currentQ.getQId();
		if (storeLoadedQId != currentQId)
			throw new InvalidOpException(
					"loadResponses() must precede getStoredResponses() ");
		if (storedResps == null) {
			Utils.alwaysPrint(SELF + "storedResps is null, "
					+ "probably because file was locked.");
			return null;
		}
		debugPrint(SELF + "total number of stored responses is ", 
				storedResps.length,
				"; correctStoredRespsCt = ", correctStoredRespsCt,
				", wrongStoredRespsCt = ", wrongStoredRespsCt);
		// return the appropriate stored responses (correct|wrong)
		StoredResponse[] desiredResps = (type == StoredResponse.CORRECT 
				? new StoredResponse[correctStoredRespsCt]
			 	: new StoredResponse[wrongStoredRespsCt]);
		int count = 0;
		for (final StoredResponse storedResp : storedResps) {
			if (storedResp == null) continue;
			if (count >= desiredResps.length) break;
			if (storedResp.type == type) {
				desiredResps[count++] = storedResp;
			}
		} // for each storedResp
		return desiredResps;
	} // getStoredResponses(int)

/* ************** Export/import methods **************/

	/** Export questions to a zip file.
	 * @param	serialNos	serial numbers of the questions to be exported
	 * @param	filename	name of the file in which the exported questions
	 * will be stored
	 * @return	the name of the zip file
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	ParameterException	if an invalid index to an evaluator,
	 * question datum, or evaluator is given
	 */
	public String exportSet(String serialNos, String filename)
			throws DBException, ParameterException {
		return XMLUtils.zipXML(filename, toXML(serialNos));
	} // exportSet(serialNos, filename)

	/** Convert all questions in this set to XML.
	 * @return	array where the first member is XML, remaining are file names of
	 * images
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	ParameterException	if an invalid index to an evaluator,
	 * question datum, or evaluator is given
	 */
	public List<String> toXML() throws DBException, ParameterException {
		return toXML(Utils.joinInts(1, getCount(), ":"));
	} // toXML()

	/** Convert questions in this set to XML.
	 * @param	serialNos	1-based serial numbers of the questions to be exported
	 * @return	list where the first member is XML, remaining are file names of
	 * images
	 * @throws	DBException	if there's a problem reading the database
	 * @throws	ParameterException	if an invalid index to an evaluator,
	 * question datum, or evaluator is given
	 */
	private List<String> toXML(String serialNos) 
			throws DBException, ParameterException {
		debugPrint("QSet.toXML: serialNos = ", serialNos);
		final List<String> xmlAndImageNames = new ArrayList<String>();
		final StringBuilder opXML = Utils.getBuilder(startQSet(),
				makeNode(NAME_TAG, qSetDescr.name),
				makeNode(AUTHOR_TAG, qSetDescr.author),
				makeNode(HEADER_TAG, qSetDescr.header));
		final TranslnsMap headerTranslns =
				QSetAllTranslns.getAllHeaderTranslns(qSetDescr.id);
		final List<String> headerLangs = headerTranslns.getLanguages();
		for (final String language : headerLangs) {
			final String translation = headerTranslns.get(language);
			if (!ENGLISH.equals(language)) {
				opXML.append(makeTranslnNode(language, translation));
			} // if language is not English (shouldn't be)
		} // for each translated opXML
		opXML.append(makeNode(REMARKS_TAG, qSetDescr.remarks));
		if (!Utils.isEmpty(serialNos)) { // in case question set is empty
			final String[] serialNoStrArray = serialNos.split(":");
			for (final String serialNoStr : serialNoStrArray) {
				final int serialNo = Integer.parseInt(serialNoStr);
				final Question oneQ = setQs.get(serialNo - 1);
				final List<String> qXmlAndImageNames = oneQ.toXML();
				debugPrint("QSet.exportSet: Q", serialNoStr,
						" of qSet ", setId, " converted to xml.");
				Utils.appendTo(opXML, 
						qXmlAndImageNames.remove(0), "\n\n");
				xmlAndImageNames.addAll(qXmlAndImageNames);
			} // for each serialNo
		} // if there are questions in the set
		opXML.append(endQSet());
		xmlAndImageNames.add(0, opXML.toString());
		return xmlAndImageNames;
	} // toXML(String)

	/** Wraps a tag with &lt; &gt;.
	 * @return	the tag wrapped in &lt; &gt;
	 */
	private StringBuilder startQSet() {
		return XMLUtils.startTag(QSET_TAG, new String[][] {
					{QSET_ID_TAG, String.valueOf(qSetDescr.id)}
				});
	} // startQSet()

	/** Surround the given text with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	text	the text of the node
	 * @return	the text surrounded by XML tags
	 */
	private StringBuilder makeNode(String tag, String text) {
		return XMLUtils.makeNode(tag, text);
	} // makeNode(String, String)

	/** Surround a translation with appropriate XML tags.
	 * @param	language	the language
	 * @param	transln	the translation
	 * @return	the translation wrapped in XML tags
	 */
	private StringBuilder makeTranslnNode(String language, String transln) {
		return XMLUtils.makeTranslnNode(language, transln);
	} // makeTranslnNode(String, String)

	/** Wraps a tag with &lt;/ &gt;.
	 * @return	the tag wrapped in &lt;/ &gt;
	 */
	private StringBuilder endQSet() {
		return XMLUtils.endTag(QSET_TAG);
	} // endQSet()

	/** Import questions from a zipped file, saving images to disk.
	 * @param	zipFilename	name and location of zip file containing the questions
	 * @return	how the import process went
	 * @throws	FileFormatException	if the XML cannot be parsed
	 */
	public String importSet(String zipFilename) throws FileFormatException {
		return parseXML(XMLUtils.extractNodes(zipFilename), this);
	} // importSet(String)

	/** Parse question set XML.  Two situations in which this method may be
	 * called:
	 * <ul><li>Importing questions into this question set.
	 * <li>Importing an entirely new question set as part of a new or existing
	 * topic.
	 * </ul>
	 * @param	nodeList	list of nodes under questionSet obtained from the XML
	 * @param	topicOrQSet	either the topic in which this new question set will
	 * be stored, or the existing question set in which questions will be stored
	 * @return	how the import process went
	 */
	public String parseXML(NodeList nodeList, Object topicOrQSet) {
		final String SELF = "QSet.parseXML: ";
		final StringBuilder parserOutput = new StringBuilder();
		int success = 0;
		int failure = 0;
		int count = 0;
		debugPrint(SELF + "nodes length = ", nodeList.getLength());
		final boolean newQSet = topicOrQSet instanceof Topic;
		final Topic topic = (newQSet ? (Topic) topicOrQSet : null);
		QSet qSet = (newQSet ? null : (QSet) topicOrQSet);
		try {
			final QuestionBank qBank = new QuestionBank();
			final QSetDescr newQSetDescr = new QSetDescr();
			final String QUESTION_TAG = Question.getTag();
			boolean headerOut = false;
			final TranslnsMap headerTranslns = new TranslnsMap();
			for (int nodeNum = 0; nodeNum < nodeList.getLength(); nodeNum++) {
				final Node n = nodeList.item(nodeNum);
				if (n.getNodeType() == Node.TEXT_NODE) continue;
				else if (n.getNodeName().equalsIgnoreCase(TOPIC_TAG)) {
					Utils.alwaysPrint(SELF + "encountered new topic "
							+ "unexpectedly; calling parseXML() recursively.");
					parserOutput.append(
							parseXML(n.getChildNodes(), topicOrQSet));
				} else if (n.getNodeName().equalsIgnoreCase(QSET_TAG)) {
					Utils.alwaysPrint(SELF + "encountered new question"
							+ " set unexpectedly; calling parseXML() "
							+ "recursively.");
					parserOutput.append(
							parseXML(n.getChildNodes(), topicOrQSet));
				} else if (n.getNodeName().equalsIgnoreCase(HEADER_TAG)) {
					if (!headerOut && !newQSet) {
						parserOutput.append("<P class=\"boldtext\">"
								+ "ACE will not store the common question "
								+ "statements or their translations.");
						headerOut = true;
					} // if not header out already
					final Node langNode = 
							n.getAttributes().getNamedItem("language");
					final Node child = n.getFirstChild();
					final String header = (child == null ? "" 
							: child.getNodeValue().trim());
					if (langNode != null) {
						final String language = langNode.getNodeValue();
						if (newQSet) {
							debugPrint(SELF + "header in ", language, " = ", 
									header);
							headerTranslns.put(language, header);
						} else {
							Utils.appendTo(parserOutput, 
									"<P><span class=\"boldtext\">"
										+ "Common question statement in ",
									language, ":</span><br/>"
										+ "<span class=\"regtext\">",
									header, "</span>");
						} // if storing the header
					} else {
						if (newQSet) {
							debugPrint(SELF + "header = ", header);
							newQSetDescr.header = header;
						} else {
							Utils.appendTo(parserOutput, 
									"<P><span class=\"boldtext\">"
										+ "Common question statement:</span>"
										+ "<br/><span class=\"regtext\">",
									header, "</span>");
						} // if storing the header
					} // if langNode
				} else if (n.getNodeName().equalsIgnoreCase(NAME_TAG) && newQSet) {
					final Node child = n.getFirstChild();
					newQSetDescr.name = (child == null ? "" 
							: child.getNodeValue().trim());
					debugPrint(SELF + "name = ", newQSetDescr.name);
				} else if (n.getNodeName().equalsIgnoreCase(AUTHOR_TAG) && newQSet) {
					final Node child = n.getFirstChild();
					newQSetDescr.author = (child == null ? "" 
							: child.getNodeValue().trim());
					debugPrint(SELF + "author = ", newQSetDescr.author);
				} else if (n.getNodeName().equalsIgnoreCase(REMARKS_TAG) && newQSet) {
					final Node child = n.getFirstChild();
					newQSetDescr.remarks = (child == null ? "" 
							: child.getNodeValue().trim());
					debugPrint(SELF + "remarks = ", newQSetDescr.remarks);
				} else if (n.getNodeName().equalsIgnoreCase(QUESTION_TAG)) {
					if (newQSet && qSet == null) { // topic is not null
						// store qSetDescr in database, add to topic
						final int newQSetId = qBank.addQSet(topic, newQSetDescr);
						qSet = (topic.instructorId == null
								? new QSet(newQSetId)
								: new QSet(newQSetId, topic.instructorId));
					} // if qSet has not yet been stored in database, assigned ID
					// parse the question
					count++;
					debugPrint(SELF + "Parse question ", count);
					try {
						final Question newQ = Question.parseXML(n);
						final Question resultQ = qSet.addQuestion(newQ);
						if (resultQ != null) {
							success++;
							Utils.appendTo(parserOutput, 
									"<P class=\"boldtext\">Question ", 
									count, " imported.  ", 
									resultQ.miscMessage);
							debugPrint(SELF + "Success! Now getting "
									+ "translations for Q ", count);
							final QTranslns qTranslns = 
									resultQ.getAllTranslations(n);
							if (qTranslns != null) {
								qTranslns.storeFor(resultQ, instructorId);
								debugPrint(SELF + "translations stored.");
							} else debugPrint(SELF 
									+ "no translations to store.");
						} else {
							failure++;
							Utils.appendTo(parserOutput, 
									"<P class=\"boldtext\">"
										+ "Internal error while importing "
										+ "question ", count, "<br> >>> ",
									qSet.getDbMessage(), "<br>");
						}
					} catch (ParserException e11) {
						Utils.appendTo(parserOutput, 
								"<P class=\"boldtext\">Parser error while "
								+ "importing question ", count, "<br> >>> ", 
								e11.getMessage(), "<br>");
						e11.printStackTrace();
						failure++;
					} catch (Exception e22) {
						Utils.appendTo(parserOutput, 
								"<P class=\"boldtext\">Generic error while "
								+ "parsing question: ", count, "<br> >>> ",
								e22.getMessage(), "<br>");
						e22.printStackTrace();
						failure++;
					} // try
				} // if it's a question
			} // for each node
			if (newQSet && newQSetDescr.id != 0 && !headerTranslns.isEmpty()) {
				QSetAllTranslns.assignHeaderTranslations(newQSetDescr.id,
						headerTranslns, instructorId);
			} // if storing the header and there are translations to store
		} catch (DBException e) {
			e.printStackTrace();
		} // try
		final StringBuilder headerOutput = Utils.getBuilder(
				"<P class=\"regtext\">Total questions found: ",
				count, "<br>Successfully imported ", success, " questions",
				"<br>Import failed for ", failure, " questions", "<br><br>");
		return parserOutput.insert(0, headerOutput).toString();
	} // parseXML(String)

} // QSet
