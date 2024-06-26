package com.epoch.assgts;

import com.epoch.assgts.assgtConstants.AssgtConstants;
import com.epoch.courseware.User;
import com.epoch.courseware.courseConstants.UserConstants;
import com.epoch.db.HWRead;
import com.epoch.db.QuestionRW;
import com.epoch.db.ResponseRead;
import com.epoch.evals.EvalResult;
import com.epoch.exceptions.DBException;
import com.epoch.qBank.Question;
import com.epoch.utils.DateUtils;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.XMLUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Holds the attributes of an assignment, including question, grading parameters,
 * and extensions.  */
public class Assgt implements AssgtConstants, UserConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Unique ID of this assignment. */
	transient public int id;
	/** ID number of the course in which this assignment resides. */
	transient public int courseId;
	/** Creator of this assignment. */
	transient public String instructorId = null;
	/** Name of this assignment. */
	private String name = "";
	/** Remarks about this assignment. */
	private String remarks = "";
	/** Flags for assignment properties. */
	private int flags = 0;
	/** Creation date of the assignment. */
	transient public Date creationDate;
	/** Due date of the assignment.  May be altered for individual students
	 * by extensions.  */
	private Date dueDate;
	/** Duration of the assignment if it is timed, in minutes. */
	private int duration = 30;
	/** Whether to limit the number of tries. */ 
	transient private boolean allowUnlimitedTries = true;
	/** The number of permitted tries. */
	private int maxTries = UNLIMITED;
	/** The maximum extension allowed for this assignment; negative number means
	 * no maximum assigned. */
	private String maxExtensionStr = NO_MAX_EXTENSION;
	/** A list of AssgtQGroups, each of which contains a list of one or more
	 * questions. The questions are divided into an integral number of
	 * equal-sized bundles. Each AsstQGroup contains the number of questions
	 * in each question bundle and the number of bundles to pick when an 
	 * assignment is instantiated, plus the points value of each question in the
	 * bundle.  */
	transient private List<AssgtQGroup> qGrps = new ArrayList<AssgtQGroup>();
	/** Parameters used to adjust grades, as originally entered.  First
	 * dimension represents attempt- or time-dependent grading; second 
	 * dimension represents limits or factors; third dimension represents the 
	 * serial number of the limits or factors.  We use Oracle to do the 
	 * calculations based on these values. */
	private String[][][] gradingParams = new String[2][2][0];
	/** Map of students who have extensions, and the length of their extensions 
	 * in days (usually) or minutes.  */
	private Map<String, String> extensions = null;
	/** The mastery assignment that must be completed before this assignment
	 * can be displayed to a student. */
	private int dependsOnId = 0;
	/** Reaction conditions that may be used to solve synthesis questions in
	 * this assignment. */
	private int[] allowedRxnCondns = new int[0];
	/** Used to indicate whether a particular value has been changed from the
	 * value stored in the DB. */
	transient public final boolean[] changed = new boolean[11];

	/** Constructor. */
	public Assgt() { 
		setCreationDate();
		setDueDateHence(WEEK);
		flags = IS_VISIBLE;
		qGrps = new ArrayList<AssgtQGroup>();
		Arrays.fill(changed, false);
	} // Assgt()

	/** Constructor. 
	 * @param	hwId	ID number of the assignment
	 * @param	qString	questions of the assignment
	 */
	public Assgt(int hwId, String qString) { 
		id = hwId;
		setCreationDate();
		setDueDateHence(WEEK);
		flags = IS_VISIBLE;
		Arrays.fill(changed, false);
		setQGroups(qString);
	} // Assgt()

	/** Copy constructor.  Doesn't copy the assignment ID.
	 * @param	assgt	description to copy
	 */
	public Assgt(Assgt assgt) { 
		copy(assgt);
	} // Assgt(Assgt)

	/** Copy constructor. 
	 * @param	assgt	description to copy
	 * @param	copyId	whether to copy the ID number as well
	 */
	public Assgt(Assgt assgt, boolean copyId) { 
		copy(assgt);
		if (copyId) id = assgt.id;
	} // Assgt(Assgt, boolean)

	/** Copies all fields except the assignment ID. 
	 * @param	assgt	description to copy
	 */
	private void copy(Assgt assgt) { 
		courseId = assgt.courseId;
		instructorId = assgt.instructorId;
		name = assgt.name;
		remarks = assgt.remarks;
		allowUnlimitedTries = assgt.allowUnlimitedTries;
		maxTries = assgt.maxTries;
		maxExtensionStr = assgt.maxExtensionStr;
		flags = assgt.flags;
		dependsOnId = assgt.dependsOnId;
		creationDate = new Date(assgt.creationDate.getTime());
		dueDate = new Date(assgt.dueDate.getTime());
		duration = assgt.duration;
		for (final AssgtQGroup qGrp : assgt.qGrps) {
			qGrps.add(new AssgtQGroup(qGrp));
		} // for each qGrp to copy
		gradingParams = new String[2][2][0];
		for (int type = 0; type < 2; type++) {
			for (int params = 0; params < 2; params++) {
				final String[] arr = assgt.gradingParams[type][params];
				if (arr.length > 0) {
					gradingParams[type][params] = Utils.getCopy(arr);
				} // if arr is not empty
			} // for each set of parameters
		} // for each type of grading parameter
		if (assgt.extensions != null)
			extensions = new LinkedHashMap<String, String>(assgt.extensions);
		Arrays.fill(changed, false);
	} // copy(Assgt)

	/** Merges an assignment's questions into this assignment.
	 * @param	daughterAssgt	the assignment to be merged
	 */
	public final void merge(Assgt daughterAssgt) {
		addGroups(daughterAssgt);
	} // merge(Assgt)

/* ***************** Short get methods ******************/

	/** Gets the name of the assignment.
	 * @return	the name of the assignment
	 */
	public String getName()						{ return name; }
	/** Gets the remarks of the assignment.
	 * @return	the remarks of the assignment
	 */
	public String getRemarks()					{ return remarks; }
	/** Gets the maximum tries of the assignment.
	 * @return	the maximum tries of the assignment
	 */
	public int getMaxTries()					{ return maxTries; }
	/** Gets the ID of the mastery assignment that a student must master before
	 * this assignment is displayed.
	 * @return	the ID of the mastery assignment
	 */
	public int getDependsOnId()					{ return dependsOnId; }
	/** Gets the string value of the maximum extension students may self-grant 
	 * for this assignment; NO_MAX_EXTENSION if none specified.
	 * @return	string value of the maximum extension students may self-grant 
	 * for this assignment
	 */
	public String getMaxExtensionStr()			{ return maxExtensionStr; }
	/** Gets the maximum extension students may self-grant for this 
	 * assignment; -1 if none specified.
	 * @return	maximum extension students may self-grant for this assignment
	 */
	public double getMaxExtension()				{ return MathUtils.parseDouble(
														maxExtensionStr); }
	/** Gets whether unlimited tries are allowed.
	 * @return	whether unlimited tries are allowed
	 */
	public boolean allowUnlimitedTries()		{ return allowUnlimitedTries; }
	/** Gets the flags of the assignment.
	 * @return	the flags of the assignment
	 */
	public int getFlags()						{ return flags; }
	/** Gets the due date of the assignment.
	 * @return	the due date of the assignment
	 */
	public Date getDueDate()					{ return dueDate; }
	/** Gets the duration of the assignment.
	 * @return	the duration of the assignment
	 */
	public int getDuration()					{ return duration; }
	/** Gets the groups of questions of the assignment.
	 * @return	groups of questions of the assignment
	 */
	public List<AssgtQGroup> getQGroups()		{ return qGrps; }
	/** Gets the grading parameters of the assignment.
	 * @return	the grading parameters of the assignment
	 */
	public String[][][] getGradingParams()		{ return gradingParams; }
	/** Gets the extensions.
	 * @return	the extensions
	 */
	public Map<String, String> getExtensions() 	{ return extensions; }
	/** Gets the allowed reaction conditions of the assignment.
	 * @return	the allowed reaction conditions of the assignment
	 */
	public int[] getAllowedRxnCondns()			{ return allowedRxnCondns; }

	/** Converts a string of allowed reaction conditions into an array.
	 * @param	rxnCondnsStr	colon-separated string of reaction condition IDs
	 * @return	array of allowed reaction conditions for this assignment
	 */
	public static int[] getAllowedRxnCondns(String rxnCondnsStr) {
		return (Utils.isEmpty(rxnCondnsStr) ? new int[0]
				: Utils.stringToIntArray(rxnCondnsStr.split(RXN_CONDN_ID_SEP)));
	} // getAllowedRxnCondns(String)

	/** Gets a string representing the allowed reaction conditions of the 
	 * assignment.
	 * @return	the allowed reaction conditions of the assignment
	 */
	public String getAllowedRxnCondnsStr()	{ 
		return Utils.join(allowedRxnCondns, RXN_CONDN_ID_SEP); 
	} // getAllowedRxnCondnsStr()

	/** Gets grading parameters of a particular kind, as Strings.
	 * @param	type	the type of grading parameter
	 * @return	grading parameters of the kind, as Strings
	 */
	public String[][] getGradingParams(int type) {
		return (gradingParams == null ? null : gradingParams[type]);
	} // getGradingParams(int)

	/** Gets an array of assignment ID numbers from an array of assignments.
	 * @param	assgts	an array of assignments
	 * @return	an array of assignment ID numbers
	 */
	public static int[] getHWIds(Assgt[] assgts) {
		final int[] hwIds = new int[assgts.length];
		int hwNum = 0;
		for (final Assgt assgt : assgts) hwIds[hwNum++] = assgt.id;
		return hwIds;
	} // getHWIds(Assgt[])

	/** Gets an array of assignment ID numbers from an array of assignments.
	 * @param	assgts	an array of assignments
	 * @return	an array of assignment ID numbers
	 */
	public static int[] getHWIds(List<Assgt> assgts) {
		return getHWIds(assgts.toArray(new Assgt[assgts.size()]));
	} // getHWIds(List<Assgt>)

	/** Gets an array of assignment names from an array of assignments.
	 * @param	assgts	an array of assignments
	 * @return	an array of assignment names
	 */
	public static String[] getHWNames(Assgt[] assgts) {
		final String[] hwNames = new String[assgts.length];
		int hwNum = 0;
		for (final Assgt assgt : assgts) hwNames[hwNum++] = assgt.getName();
		return hwNames;
	} // getHWNames(Assgt[])

/* ***************** Short set and get-changed methods ******************/

	/** Sets the name of the assignment.
	 * @param	nm	the name of the assignment
	 */
	public void setName(String nm) { 
		if ((name == null && nm != null) || name != null && !name.equals(nm)) {
			name = nm; 
			changed[BASICS] = true;
		}
	} // setName(String)

	/** Sets the remarks of the assignment.
	 * @param	rmks	the remarks of the assigrmksent
	 */
	public void setRemarks(String rmks) { 
		if ((remarks == null && rmks != null) ||
				remarks != null && !remarks.equals(rmks)) {
			remarks = rmks; 
			changed[BASICS] = true;
		}
	} // setRemarks(String)

	/** Sets the maximum tries of the assignment.
	 * @param	mt	the maximum tries of the assignment
	 */
	public void setMaxTries(int mt) { 
		if (maxTries != mt) {
			maxTries = mt; 
			allowUnlimitedTries = maxTries < 0;
			changed[BASICS] = true;
		}
	} // setMaxTries(int)

	/** Sets the maximum extension students may self-grant.
	 * @param	maxExtStr	the maximum extension students may self-grant
	 */
	public void setMaxExtensionStr(String maxExtStr) { 
		final String newMaxExtStr = (maxExtStr == null 
				? NO_MAX_EXTENSION : maxExtStr); 
		if (!maxExtensionStr.equals(newMaxExtStr)) {
			maxExtensionStr = newMaxExtStr;
			changed[MAX_EXT] = true;
		}
	} // setMaxExtensionStr(String)

	/** Sets the allowed reaction conditions for this assignment.
	 * @param	rxnCondns	array of reaction condition IDs
	 */
	public void setAllowedRxnCondns(int[] rxnCondns) {
		if (!Arrays.equals(rxnCondns, allowedRxnCondns)) {
			allowedRxnCondns = rxnCondns;
			changed[ALLOWED_RXN_CONDNS] = true;
		} 
	} // setAllowedRxnCondns(int[])

	/** Sets the allowed reaction conditions for this assignment.
	 * @param	rxnCondnsStr	colon-separated string of reaction 
	 * condition IDs
	 */
	public void setAllowedRxnCondns(String rxnCondnsStr) {
		final int[] rxnCondns = (Utils.isEmpty(rxnCondnsStr) ? new int[0]
				: Utils.stringToIntArray(rxnCondnsStr.split(RXN_CONDN_ID_SEP)));
		setAllowedRxnCondns(rxnCondns);
	} // setAllowedRxnCondns(String)

	/** Sets the flags of the assignment.
	 * @param	flgs	the flags of the assigflgsent
	 */
	public void setFlags(int flgs) { 
		if (flags != flgs) {
			flags = flgs; 
			changed[FLAGS] = true;
		}
	} // setFlags(int)

	/** Sets the assignment visibility flag.
	 * @param	on	whether to turn the assignment visibility flag on or off
	 */
	public void setVisible(boolean on) {
		final int oldFlags = flags;
		if (on) flags |= IS_VISIBLE;
		else flags &= ~IS_VISIBLE;
		if (flags != oldFlags) changed[FLAGS] = true;
	} // setVisible(boolean)

	/** Sets the record-after-due flag.
	 * @param	on	whether to turn the record-after-due flag on or off
	 */
	public void setRecordAfterDue(boolean on) {
		final int oldFlags = flags;
		if (on) flags |= RECORD_AFTER_DUE;
		else flags &= ~RECORD_AFTER_DUE;
		if (flags != oldFlags) changed[FLAGS] = true;
	} // setRecordAfterDue(boolean)

	/** Sets the exam flag.
	 * @param	on	whether to turn the exam flag on or off
	 */
	public void setIsExam(boolean on) {
		final int oldFlags = flags;
		if (on) flags |= EXAM_ASSGT;
		else flags &= ~EXAM_ASSGT;
		if (flags != oldFlags) changed[FLAGS] = true;
	} // setIsExam(boolean)

	/** Sets the mastery assignment flag.
	 * @param	on	whether to turn the mastery assignment flag on or off
	 */
	public void setIsMasteryAssgt(boolean on) {
		final int oldFlags = flags;
		if (on) flags |= MASTERY_ASSGT;
		else flags &= ~MASTERY_ASSGT;
		if (flags != oldFlags) changed[FLAGS] = true;
	} // setIsMasteryAssgt(boolean)

	/** Sets the flag for this assignment to be timed.
	 * @param	on	whether to turn the timed assignment flag on or off
	 */
	public void setIsTimed(boolean on) {
		final int oldFlags = flags;
		if (on) flags |= TIMED;
		else flags &= ~TIMED;
		if (flags != oldFlags) changed[FLAGS] = true;
	} // setIsTimed(boolean)

	/** Sets the flag for this assignment to log all responses to disk.
	 * @param	on	whether to turn the log-all assignment flag on or off
	 */
	public void setLogAllToDisk(boolean on) {
		final int oldFlags = flags;
		if (on) flags |= LOG_ALL_TO_DISK;
		else flags &= ~LOG_ALL_TO_DISK;
		if (flags != oldFlags) changed[FLAGS] = true;
	} // setLogAllToDisk(boolean)

	/** Sets the flag for this assignment to show a "save without submitting"
	 * button.
	 * @param	on	whether to show a "save without submitting" button
	 */
	public void setShowSaveWOSubmitting(boolean on) {
		final int oldFlags = flags;
		if (on) flags |= SAVE_WO_SUBMITTING;
		else flags &= ~SAVE_WO_SUBMITTING;
		if (flags != oldFlags) changed[FLAGS] = true;
	} // setShowSaveWOSubmitting(boolean)

	/** Sets the flag for saving previous tries.
	 * @param	on	whether to turn the flag for saving previous tries on or off
	 */
	public void setSavePrevTries(boolean on) {
		final int oldFlags = flags;
		if (on) flags |= SAVE_PREV_TRIES;
		else flags &= ~SAVE_PREV_TRIES;
		if (flags != oldFlags) changed[FLAGS] = true;
	} // setSavePrevTries(boolean)

	/** Sets the flag for delaying grading.
	 * @param	on	whether to turn the flag for delaying grading on or off
	 */
	public void setDelayGrading(boolean on) {
		final int oldFlags = flags;
		if (on) flags |= DELAY_GRADING;
		else flags &= ~DELAY_GRADING;
		if (flags != oldFlags) changed[FLAGS] = true;
	} // setDelayGrading(boolean)

	/** Sets the flag for excluding the grades of this assignment from the total
	 * grades.
	 * @param	on	whether to turn the flag for excluding the grades of this 
	 * assignment from the total grades on or off
	 */
	public void setExcludeFromTotals(boolean on) {
		final int oldFlags = flags;
		if (on) flags |= EXCLUDE_FROM_GRADE_AVERAGE;
		else flags &= ~EXCLUDE_FROM_GRADE_AVERAGE;
		if (flags != oldFlags) changed[FLAGS] = true;
	} // setExcludeFromTotals(boolean)

	/** Sets new flags for when to show references.
	 * @param	showRefs	new flags for showing references
	 */
	public void setShowReferences(int showRefs) {
		final int oldFlags = flags;
		flags &= ~SHOW_REFS_BEFORE_ANSWERED & ~SHOW_REFS_AFTER_ANSWERED;
		flags |= showRefs;
		if (flags != oldFlags) changed[FLAGS] = true;
	} // setShowReferences(int)

	/** Sets a new due date for the assignment.
	 * @param	dd	the new due date of the assigddent
	 */
	public void setDueDate(Date dd) { 
		if ((dueDate == NO_DATE && dd != NO_DATE) || !dueDate.equals(dd)) {
			dueDate = dd; 
			changed[DUE_DATE] = true;
		}
	} // setDueDate(Date)

	/** Sets a new duration for the assignment.
	 * @param	durn	the new duration of the assignment
	 */
	public void setDuration(int durn) { 
		if (duration != durn) {
			duration = (durn == 0 ? 30 : durn); 
			changed[DURATION] = true;
		}
	} // setDuration(int)

	/** Sets the question groups associated with this assignment and adds to
	 * each one the points-per-Q grading parameter. Called from
	 * HWRead.getAssgtQGroups().
	 * @param	groups	list of question groups
	 */
	public final void setQGroups(List<AssgtQGroup> groups) {
		qGrps = groups;
		changed[QUESTIONS] = false;
	} // setQGroups(List<AssgtQGroup>)

	/** Constructs a list of question groups from a string of the question 
	 * ID numbers.  The questions in the resulting qGrps contain only their 
	 * ID numbers.  Called from parseXML() and from db/DataConversion.
	 * @param	qString	the question list (string format) to parse
	 */
	public final void setQGroups(String qString) {
		qGrps = new ArrayList<AssgtQGroup>();
		addGroups(qString);
	} // setQGroups(String)

	/** Sets this assignment's extensions.
	 * @param	extensionsMap	a map of extensions
	 */
	public void setExtensions(Map<String, String> extensionsMap) {
		extensions = extensionsMap;
		changed[EXTS] = false;
	} // setExtensions(Map<String, String>)

	/** Sets the ID of the mastery assignment that a student must master before
	 * this assignment is displayed.
	 * @param	id	the ID of the mastery assignment
	 */
	public void setDependsOnId(int id)				{ dependsOnId = id; }
	/** Gets if the name, remarks, or tries have changed. 
	 * @return	true if the name, remarks, or tries have changed
	 */
	public boolean basicsHaveChanged() 				{ return changed[BASICS]; }
	/** Gets if the questions have changed. 
	 * @return	true if the questions have changed
	 */
	public boolean questionsHaveChanged() 			{ return changed[QUESTIONS]; }
	/** Gets if the allowed reaction conditions have changed. 
	 * @return	true if the allowed reaction conditions have changed
	 */
	public boolean allowedRxnCondnsHaveChanged()	{ return changed[ALLOWED_RXN_CONDNS]; }
	/** Gets if the due date has changed. 
	 * @return	true if the due date has changed
	 */
	public boolean dueDateHasChanged() 				{ return changed[DUE_DATE]; }
	/** Gets if the duration has changed. 
	 * @return	true if the duration has changed
	 */
	public boolean durationHasChanged() 			{ return changed[DURATION]; }
	/** Gets if the flags have changed. 
	 * @return	true if the flags have changed
	 */
	public boolean flagsHaveChanged() 				{ return changed[FLAGS]; }
	/** Gets if the time- or attempt-dependent grading parameters have changed. 
	 * @return	true if the time- or attempt-dependent grading parameters have changed
	 */
	public boolean gradingParamsHaveChanged() 		{ return changed[GRADE_PARAMS]; }
	/** Gets if the points per question have changed. 
	 * @return	true if the points per question have changed
	 */
	public boolean ptsPerQHaveChanged() 			{ return changed[PTS_PER_Q_PARAMS]; }
	/** Gets if the question dependencies have changed. 
	 * @return	true if the question dependencies have changed
	 */
	public boolean dependenciesHaveChanged() 		{ return changed[DEPENDENCIES]; }
	/** Gets if the extensions have changed. 
	 * @return	true if the extensions have changed
	 */
	public boolean extensionsHaveChanged() 			{ return changed[EXTS]; }
	/** Gets if the maximum extension has changed. 
	 * @return	true if the maximum extension has changed
	 */
	public boolean maxExtensionHasChanged() 		{ return changed[MAX_EXT]; }
	/** Sets that all parameters are as written in the database.  */
	public void setNoChanges()						{ Arrays.fill(changed, false); }
	/** Sets that the questions have changed. */
	public void setQuestionsHaveChanged()			{ changed[QUESTIONS] = true; }
	/** Sets that the points per question have changed. */
	public void setPtsPerQHaveChanged()				{ changed[PTS_PER_Q_PARAMS] = true; }
	/** Sets that the question dependencies have changed. */
	public void setDependenciesHaveChanged()		{ changed[DEPENDENCIES] = true; }

	/** Sets one type of grading parameters of the assignment.  Called ONLY from
	 * HWRead, which is why we don't set changed[GRADE_PARAMS] to true.
	 * @param	params	one type of grading parameters of the assignment
	 * @param	type	type of grading parameter
	 */
	public void setGradingParams(String[][] params, int type) { 
		gradingParams[type] = params; 
		changed[GRADE_PARAMS] = false;
	} // setGradingParams(String[][], int)

	/** Converts a grading parameters string to a pair of arrays, storing the
	 * value in this Assgt.  Used for XML and for communication with the front
	 * end.
	 * @param	paramsStr	the string encoding the /-separated limits and
	 * factors (factors only for points-per-question parameters)
	 * @param	type	the type of grading parameters
	 */
	public void setGradingParams(String paramsStr, int type) {
		final String SELF = "Assgt.setGradingParams: ";
		if (paramsStr == null
				|| paramsStr.equals(getParamsString(type))) return;
		debugPrint(SELF + "old paramsStr = ", getParamsString(type),
				", new paramsStr = ", paramsStr, 
				", type = ", DB_PARAM_TYPES[type]);
		final String[] paramVals = ("".equals(paramsStr)
				? new String[0] : paramsStr.split("/"));
		final int numVals = paramVals.length;
		gradingParams[type] = new String[2][numVals / 2];
		debugPrint(SELF + "length of LIMITS or FACTORS = ", 
				gradingParams[type][LIMITS].length);
		for (int valNum = 0; valNum < paramVals.length; valNum += 2) {
			gradingParams[type][LIMITS][valNum / 2] = paramVals[valNum];
			gradingParams[type][FACTORS][valNum / 2] = paramVals[valNum + 1];
		} // for each parameter value
		changed[GRADE_PARAMS] = true;
		debugPrint(SELF + "new LIMITS: ", gradingParams[type][LIMITS],
				"\nnew FACTORS: ", gradingParams[type][FACTORS]);
	} // setGradingParams(String, int)

	/* Takes a /-separated string of points per seen Q and stores an
	 * appropriate value in each Q group. Used for XML.
	 * @param	paramStr	the string encoding the /-separated factors
	private void setPointsPerQ(String paramStr) {
		final String SELF = "Assgt.setPointsPerQ: ";
		final String[] paramVals = paramStr.split("/");
		int seenQNum = 0;
		for (final AssgtQGroup qGrp : qGrps) {
			if (seenQNum < paramVals.length) {
				qGrp.setPts(paramVals[seenQNum]);
			} else break;
			seenQNum += qGrp.getNumQsSeen();
		} // for each Q group
		changed[PTS_PER_Q_PARAMS] = true;
	} // setPointsPerQ(String)
	 */

	/** Sets the values that can be modified on page 2 of the assignment
	 * assembly tool to changed so that they will be saved; used when
	 * duplicating an assignment. */
	public void setPage2ValuesChanged() {
		changed[FLAGS] = true;
		changed[DUE_DATE] = true;
		changed[EXTS] = hasExtensions();
		changed[GRADE_PARAMS] = hasGradingParams();
	} // setPage2ValuesChanged()

/* ***************** Methods involving flags ******************/

	/** Gets if this assignment is visible.
	 * @return	true if this assignment is visible
	 */
	public boolean isVisible() {
		return isVisible(flags);
	} // isVisible()

	/** Gets if an assignment is visible.
	 * @param	hwFlags	flags of the assignment
	 * @return	true if the assignment is visible
	 */
	public static boolean isVisible(int hwFlags) {
		return (hwFlags & IS_VISIBLE) != 0; 
	} // isVisible(int)

	/** Gets if responses to this assignment should be recorded after the due
	 * date.
	 * @return	true if responses to this assignment should be recorded after
	 * the due date
	 */
	public boolean recordAfterDue() {
		return recordAfterDue(flags);
	} // recordAfterDue()

	/** Gets if responses to this assignment should be recorded after the due
	 * date.
	 * @param	hwFlags	flags of the assignment
	 * @return	true if responses to this assignment should be recorded after
	 * the due date
	 */
	public static boolean recordAfterDue(int hwFlags) {
		return (hwFlags & RECORD_AFTER_DUE) != 0; 
	} // recordAfterDue(int)

	/** Gets if this assignment is an exam.
	 * @return	true if this assignment is an exam
	 */
	public boolean isExam() {
		return isExam(flags);
	} // isExam()

	/** Gets if an assignment is an exam.
	 * @param	hwFlags	flags of the assignment
	 * @return	true if the assignment is an exam
	 */
	public static boolean isExam(int hwFlags) {
		return (hwFlags & EXAM_ASSGT) != 0; 
	} // isExam(int)

	/** Gets if this assignment is a mastery assignment.
	 * @return	true if this assignment is a mastery assignment
	 */
	public boolean isMasteryAssgt() {
		return isMasteryAssgt(flags);
	} // isMasteryAssgt()

	/** Gets if an assignment is a mastery assignment.
	 * @param	hwFlags	flags of the assignment
	 * @return	true if the assignment is a mastery assignment
	 */
	public static boolean isMasteryAssgt(int hwFlags) {
		return (hwFlags & MASTERY_ASSGT) != 0; 
	} // isMasteryAssgt(int)

	/** Gets if this assignment is a timed assignment.
	 * @return	true if this assignment is a timed assignment
	 */
	public boolean isTimed() {
		return isTimed(flags);
	} // isTimed()

	/** Gets if an assignment is a timed assignment.
	 * @param	hwFlags	flags of the assignment
	 * @return	true if the assignment is a timed assignment
	 */
	public static boolean isTimed(int hwFlags) {
		return (hwFlags & TIMED) != 0; 
	} // isTimed(int)

	/** Gets if this assignment logs all responses to disk.
	 * @return	true if this assignment logs all responses to disk
	 */
	public boolean logsAllToDisk() {
		return logsAllToDisk(flags);
	} // logsAllToDisk()

	/** Gets if an assignment logs all responses to disk.
	 * @param	hwFlags	flags of the assignment
	 * @return	true if the assignment logs all responses to disk
	 */
	public static boolean logsAllToDisk(int hwFlags) {
		return (hwFlags & LOG_ALL_TO_DISK) != 0; 
	} // logsAllToDisk(int)

	/** Gets if this assignment shows a "save without submitting" button.
	 * @return	true if this assignment shows a "save without submitting" button
	 */
	public boolean showSaveWOSubmitting() {
		return showSaveWOSubmitting(flags);
	} // showSaveWOSubmitting()

	/** Gets if an assignment shows a "save without submitting" button.
	 * @param	hwFlags	flags of the assignment
	 * @return	true if the assignment shows a "save without submitting" button
	 */
	public static boolean showSaveWOSubmitting(int hwFlags) {
		return (hwFlags & SAVE_WO_SUBMITTING) != 0; 
	} // showSaveWOSubmitting(int)

	/** Gets if all responses to a question in this assignment (not just the
	 * most recent response) should be saved in the database.
	 * @return	true if all responses to a question should be kept
	 */
	public boolean savePrevTries() {
		return savePrevTries(flags);
	} // savePrevTries()

	/** Gets if all responses to a question in this assignment (not just the
	 * most recent response) should be saved in the database.
	 * @param	hwFlags	flags of the assignment
	 * @return	true if all responses to a question should be kept
	 */
	public static boolean savePrevTries(int hwFlags) {
		return (hwFlags & SAVE_PREV_TRIES) != 0; 
	} // savePrevTries(int)

	/** Gets if this assignment's questions should not be graded or provide 
	 * feedback.
	 * @return	true if this assignment's questions should not be graded or 
	 * provide feedback
	 */
	public boolean delayGrading() {
		return delayGrading(flags);
	} // delayGrading()

	/** Gets if an assignment's questions should not be graded or provide 
	 * feedback.
	 * @param	hwFlags	flags of the assignment
	 * @return	true if the assignment's questions should not be graded or 
	 * provide feedback
	 */
	public static boolean delayGrading(int hwFlags) {
		return (hwFlags & DELAY_GRADING) != 0; 
	} // delayGrading(int)

	/** Gets if ACE should show a reference for a question in this assignment 
	 * before the student has answered the question correctly.
	 * @return	true if ACE should show a reference for a question in this
	 * assignment before the student has answered the question correctly
	 */
	public boolean showRefsBeforeAnswered() {
		return showRefsBeforeAnswered(flags);
	} // showRefsBeforeAnswered()

	/** Gets if ACE should show a reference for a question in an assignment 
	 * before the student has answered the question correctly.
	 * @param	hwFlags	flags of the assignment
	 * @return	true if ACE should show a reference for a question in an 
	 * assignment before the student has answered the question correctly
	 */
	public static boolean showRefsBeforeAnswered(int hwFlags) {
		return (hwFlags & SHOW_REFS_BEFORE_ANSWERED) != 0; 
	} // showRefsBeforeAnswered(int)

	/** Gets if ACE should show a reference for a question in this assignment 
	 * after the student has answered the question correctly.
	 * @return	true if ACE should show a reference for a question in this
	 * assignment after the student has answered the question correctly
	 */
	public boolean showRefsAfterAnswered() {
		return showRefsAfterAnswered(flags);
	} // showRefsAfterAnswered()

	/** Gets if ACE should show a reference for a question in an assignment 
	 * after the student has answered the question correctly.
	 * @param	hwFlags	flags of the assignment
	 * @return	true if ACE should show a reference for a question in an 
	 * assignment after the student has answered the question correctly
	 */
	public static boolean showRefsAfterAnswered(int hwFlags) {
		return (hwFlags & SHOW_REFS_AFTER_ANSWERED) != 0; 
	} // showRefsAfterAnswered(int)

	/** Gets if the grade on this assignment should not be averaged in with the
	 * total grade for the course.
	 * @return	true if the grade on this assignment should not be averaged in
	 * with the total grade for the course
	 */
	public boolean excludeFromTotals() {
		return excludeFromTotals(flags);
	} // excludeFromTotals()

	/** Gets if the grade on this assignment should not be averaged in with the
	 * total grade for the course.
	 * @param	hwFlags	flags of the assignment
	 * @return	true if the grade on this assignment should not be averaged in
	 * with the total grade for the course
	 */
	public static boolean excludeFromTotals(int hwFlags) {
		return (hwFlags & EXCLUDE_FROM_GRADE_AVERAGE) != 0; 
	} // excludeFromTotals(int)

/* ***************** Methods involving the set of questions ******************/

	/** Gets whether the question list is empty.
	 * @return	true if the question list is empty
	 */
	public boolean noQGroups() { 
		return Utils.isEmpty(qGrps); 
	} // noQGroups()

	/** Gets the number of question groups in the assignment.
	 * @return	the number of question groups in the assignment
	 */
	public int getNumGroups() { 
		return qGrps.size(); 
	} // getNumGroups()

	/** Calculates the number of questions each student will see.
	 * @return	the number of questions each student will see
	 */
	public int getNumQsSeen() {
		int numQs = 0;
		for (final AssgtQGroup qGrp : qGrps) {
			numQs += qGrp.getNumQsSeen();
		} // for each group
		return numQs;
	} // getNumQsSeen()

	/** Gets the number of questions each student will see in each assignment.
	 * @param	assgts	an array of assignments
	 * @return	the number of seen questions in a parallel array
	 */
	public static int[] getNumQsSeen(Assgt[] assgts) {
		final int[] numQsSeen = new int[assgts.length];
		int assgtNum = 0;
		for (final Assgt assgt : assgts) {
			numQsSeen[assgtNum++] = assgt.getNumQsSeen();
		} // for each assignment
		return numQsSeen;
	} // getNumQsSeen(Assgt[])

	/** Calculates the number of questions in the set, including both fixed
	 * questions and questions that are part of random groups.
	 * @return	the number of questions in the set
	 */
	public int getNumAllQs() {
		int numQs = 0;
		for (final AssgtQGroup qGrp : qGrps) {
			numQs += qGrp.getNumQs();
		} // for each group
		return numQs;
	} // getNumAllQs()

	/** Gets the unique IDs of all of the questions in the assignment.
	 * @return	list of question IDs
	 */
	public List<Integer> getQIds() {
		final List<Integer> qIds = new ArrayList<Integer>();
		for (final AssgtQGroup qGrp : qGrps) {
			qIds.addAll(qGrp.getQIds());
		} // for each group
		return qIds;
	} // getQIds()

	/** Gets the unique IDs of the fixed questions in the assignment.
	 * @return	list of fixed question IDs
	 */
	private List<Integer> getFixedQIds() {
		return getFixedQIds(!BY_POSN);
	} // getFixedQIds()

	/* Gets the unique IDs of the fixed questions in the assignment in an order
	 * parallel to their position in the assignment.  Positions corresponding to
	 * random questions are set to null.
	 * @return	list of fixed question IDs and null for random questions
	private List<Integer> getFixedQIdsByPosn() {
		return getFixedQIds(BY_POSN);
	} // getFixedQIdsByPosn()
	 */

	/** Gets the unique IDs of the fixed questions in the assignment, perhaps 
	 * in an order parallel to their position in the assignment.
	 * @param	byPosn	whether the position of each fixed ID should reflect its
	 * position in an instantiated assignment, with positions for randomized
	 * questions occupied by null values
	 * @return	list of fixed question IDs, maybe null for random questions
	 */
	private List<Integer> getFixedQIds(boolean byPosn) {
		final List<Integer> qIds = new ArrayList<Integer>();
		for (final AssgtQGroup qGrp : qGrps) {
			if (qGrp.isRandom()) {
				if (byPosn) {
					for (int qNum = 0; qNum < qGrp.getNumQsSeen(); qNum++) {
						qIds.add(null);
					} // for each random Q
				} // if should add null for random Qs
			} else qIds.addAll(qGrp.getQIds());
		} // for each group
		return qIds;
	} // getFixedQIds(boolean)

	/** Gets the number of questions to pick from each group.
	 * @return	number of questions to pick from each group
	 */
	public int[] getPicks() {
		final int[] picks = new int[getNumGroups()];
		int grpNum = 0;
		for (final AssgtQGroup qGrp : qGrps) {
			picks[grpNum++] = qGrp.getPick();
		} // for each group
		return picks;
	} // getPicks()

	/** Returns all the question ids in the qString, grouped into lists of lists.
	 * @return	a list of lists of IDs
	 */
	public List<ArrayList<Integer>> getGroupedQIds() {
		final List<ArrayList<Integer>> qIds = 
				new ArrayList<ArrayList<Integer>>();
		for (final AssgtQGroup qGrp : qGrps) {
			qIds.add(new ArrayList<Integer>(qGrp.getQIds()));
		} // for each group
		return qIds;
	} // getGroupedQIds()

	/** Gets the 1-based question number of a particular question in this 
	 * assignment. If the question is random, gets the range of questions in 
	 * which it might appear.
	 * @param	qId	question ID number
	 * @return array of either a single question number, a range of question
	 * numbers, or nothing
	 */
	public int[] getQNum(int qId) {
		final String SELF = "Assgt.getQNum: ";
		debugPrint(SELF + "qId = ", qId);
		final Integer qIdObj = Integer.valueOf(qId);
		int grpStart = 0;
		for (final AssgtQGroup qGrp : qGrps) {
			final List<Integer> grpQIds = qGrp.getQIds();
			debugPrint(SELF + "grpQIds = ", grpQIds);
			final int qNumInGrp = grpQIds.indexOf(qIdObj) + 1;
			if (qNumInGrp > 0) {
				final int realQNum = qNumInGrp + grpStart;
				return (grpQIds.size() > 1
						? new int[] {realQNum, realQNum + qGrp.getPick() - 1}
						: new int[] {realQNum});
			} // if found in group
			grpStart += qGrp.getPick();
		} // for each group
		return new int[0];
	} // getQNum(int)

	/** Gets a group of questions from the list.
	 * @param	grpNum	1-based serial number of the group
	 * @return	the list of questions in the group; null if out of range
	 */
	public AssgtQGroup getQGroup(int grpNum) {
		if (grpNum < 1 || grpNum > getNumGroups()) return null;
		return qGrps.get(grpNum - 1);
	} // getQGroup(int)

	/** Determines whether the assignment contains a group of random questions.
	 * @return	true if the assignment contains a group of random questions
	 */
	public boolean lacksRandomGroups() {
		for (final AssgtQGroup qGrp : qGrps) {
			if (!qGrp.isFixedGroup()) return false;
		} // for each group
		return true;
	} // lacksRandomGroups()

	/** Adds a group of questions to the list.
	 * @param	group	the group of Qs to insert
	 */
	public void addGroup(AssgtQGroup group) {
		qGrps.add(group);
		changed[QUESTIONS] = true;
	} // addGroup(AssgtQGroup)

	/** Adds to the existing question groups a list of question groups from 
	 * a string of the question ID numbers. The questions in the resulting 
	 * qGrps contain only their ID numbers. 
	 * @param	qString	the question list (string format) to parse
	 */
	private final void addGroups(String qString) {
		final String[] groupStrs = (qString == null 
				? new String[0] : qString.split(GROUP_SEP));
		// final List<Integer> assgtQIds = getQIds(); // unused?
		for (final String groupStr : groupStrs) {
			final AssgtQGroup newQGrp = new AssgtQGroup(groupStr);
			final int numGrpQs = newQGrp.getNumQs();
			if (numGrpQs > 0) {
				qGrps.add(newQGrp);
			} // if there are Qs in the group
		} // for each group of Qs from which to pick one or more
		changed[QUESTIONS] = true;
	} // addGroups(String)

	/** Adds to the existing question groups a new set of question groups.
	 * Called from merge().
	 * @param	daughterAssgt	the assignment from which to copy the question
	 * groups
	 */
	private final void addGroups(Assgt daughterAssgt) {
		int numQsAdded = 0;
		final List<Integer> assgtQIds = getQIds();
		for (final AssgtQGroup newQGrp : daughterAssgt.qGrps) {
			final List<Integer> grpQIds = newQGrp.getQIds();
			for (int grpQNum = 0; grpQNum < grpQIds.size(); grpQNum++) {
				final Integer grpQId = grpQIds.get(grpQNum);
				if (assgtQIds.contains(grpQId)) {
					newQGrp.removeQ(grpQNum + 1);
				} // if the Q is already in the assignment
			} // for each question in the group
			// adjust bundle size and number to pick
			final int numGrpQs = newQGrp.getNumQs();
			if (numGrpQs % newQGrp.getBundleSize() != 0) {
				newQGrp.setBundleSize(1);
				newQGrp.setPick(1);
			} // if number of Qs not divisible by bundle size
			if (numGrpQs == 1) {
				newQGrp.setPick(1);
			} else {
				final int maxPick = (numGrpQs / newQGrp.getBundleSize()) - 1;
				if (newQGrp.getPick() > maxPick) newQGrp.setPick(maxPick);
			} // if numGrpQs
			if (numGrpQs > 0) {
				qGrps.add(newQGrp);
				numQsAdded += numGrpQs;
			} // if there are Qs in the group
		} // for each group of Qs from which to pick one or more
		if (numQsAdded > 0) {
			changed[QUESTIONS] = true;
		} // if questions were added
	} // addGroups(Assgt)

	/** Inserts a group of questions into the list.
	 * @param	posn	where to insert the group
	 * @param	group	the group of Qs to insert
	 */
	public void insertGroup(int posn, AssgtQGroup group) {
		final String SELF = "Assgt.insertGroup: ";
		debugPrint(SELF + "posn = ", posn, ", numQGroups = ", getNumGroups());
		if (posn < 1 || posn - 1 > getNumGroups()) return;
		qGrps.add(posn - 1, group);
		changed[QUESTIONS] = true;
	} // insertGroup(int, AssgtQGroup)

	/** Removes a group of questions from the list.
	 * @param	grpNum	1-based serial number of the group
	 * @return	the list of questions in the group; null if out of range
	 */
	public AssgtQGroup removeGroup(int grpNum) {
		if (grpNum < 1 || grpNum > getNumGroups()) return null;
		final AssgtQGroup removedGroup = qGrps.remove(grpNum - 1);
		changed[PTS_PER_Q_PARAMS] = true;
		changed[QUESTIONS] = true;
		return removedGroup;
	} // removeGroup(int)

	/** Removes a question from a group of questions.
	 * @param	qGrpNum	1-based serial number of the group
	 * @param	grpQNum	1-based serial number of the question within the group
	 * @return	unique ID of the question to be removed; 0 if unsuccessful
	 */
	public int removeGroupQ(int qGrpNum, int grpQNum) {
		final String SELF = "Assgt.removeGroupQ: ";
		int removedQId = 0;
		if (qGrpNum >= 1 && qGrpNum <= getNumGroups()) {
			final AssgtQGroup qGrp = getQGroup(qGrpNum);
			final Question removedQ = qGrp.removeQ(grpQNum);
			if (removedQ != null) {
				changed[QUESTIONS] = true;
				removedQId = removedQ.getQId();
				final int numGrpQs = qGrp.getNumQs();
				debugPrint(SELF + "after removing, numGrpQs = ", numGrpQs);
				if (numGrpQs == 0) {
					debugPrint(SELF + "no more Qs in group, removing it.");
					removeGroup(qGrpNum);
				} // if Q's group now empty
			} // if a Q has been removed
		} // if the group number is in range
		return removedQId;
	} // removeGroupQ(int, int)

	/** Gets the 0-based position of a question group identical to the 
	 * given one in this assignment.
	 * @param	qGrp	a question group
	 * @return	index of the question group in this assignment, or -1 if not
	 * found
	 */
	public int indexOf(AssgtQGroup qGrp) {
		int index = 0;
		for (final AssgtQGroup myQGrp : getQGroups()) {
			if (myQGrp.equals(qGrp)) return index;
			else index++;
		} // for each question group
		return -1;
	} // indexOf(AssgtQGroup)

	/** Gets an array containing the 0-based group number of each question 
	 * in the instantiated assignments' list of questions.
	 * @return	array of 0-based group numbers corresponding to each question
	 * that a student would see
	 */
	public int[] getGroupNumbers() {
		final int numGrps = getNumGroups();
		final List<Integer> grpNums = new ArrayList<Integer>();
		for (int grpNum = 0; grpNum < numGrps; grpNum++) {
			final AssgtQGroup qGrp = getQGroup(grpNum + 1);
			final int numGrpQsSeen = qGrp.getNumQsSeen();
			final Integer grpNumMember = Integer.valueOf(grpNum);
			for (int grpQNum = 0; grpQNum < numGrpQsSeen; grpQNum++) {
				grpNums.add(grpNumMember);
			} // for each Q in the group seen by the student
		} // for each group of Qs
		return Utils.listToIntArray(grpNums);
	} // getGroupNumbers()

	/** Gets the positions of each group of questions in the instantiated
	 * assignments' list of questions.
	 * @return	array of 0-based numbers corresponding to the start positions 
	 * of each group in the instantiated assignment
	 */
	public int[] getRealStartPosns() {
		final int numGrps = getNumGroups();
		final int[] realStartPosns = new int[numGrps];
		int newStart = 0;
		for (int grpNum = 0; grpNum < numGrps; grpNum++) {
			realStartPosns[grpNum] = newStart;
			newStart += getQGroup(grpNum + 1).getNumQsSeen();
		} // for each group of Qs
		return realStartPosns;
	} // getRealStartPosns()

	/** Makes sure that all fixed questions in this mastery assignment are
	 * R-group or variable numeric questions.
	 * @return	true if all fixed questions in this mastery assignment are
	 * R-group or variable numeric questions
	 */
	public boolean validateMastery() {
		final String SELF = "Assgt.validateMastery: ";
		boolean valid = true;
		if (isMasteryAssgt()) {
			final List<Integer> fixedQIds = getFixedQIds();
			final long[] qFlags = QuestionRW.getQuestionFlags(
					Utils.listToIntArray(fixedQIds), instructorId);
			debugPrint(SELF + "fixedQIds = ", fixedQIds,
					", qFlags = ", qFlags);
			for (int qNum = 0; qNum < fixedQIds.size(); qNum++) {
				valid = Question.usesSubstns(qFlags[qNum]);
				if (!valid) break;
			} // for each position in the assignment
		} // if is mastery assignment
		return valid;
	} // validateMastery()

/* ***************** Methods involving dates and extensions ******************/

	/** Sets the creation date to now. */
	final public void setCreationDate() {
		creationDate = new Date(System.currentTimeMillis());
	} // setCreationDate()

	/** Sets the due date a certain length of time from now.
	 * @param	hence	the amount of time from now
	 */
	final public void setDueDateHence(long hence) {
		final Date tempDueDate = new Date(System.currentTimeMillis() + hence);
		final Calendar tempCal = Calendar.getInstance();
		tempCal.setTime(tempDueDate);
		// assignment is due at 11:00 PM by default
		tempCal.set(Calendar.HOUR_OF_DAY, 23);
		tempCal.set(Calendar.MINUTE, 0);
		dueDate = tempCal.getTime();
	} // setDueDateHence(long)

	/** Converts a string describing students' extensions into a map.
	 * @param	extensionsStr	the string, userId1/ext1/...
	 * @return	map of extensions by userIds
	 */
	public static Map<String, String> getExtensions(String extensionsStr) {
		final String SELF = "Assgt.getExtensions: ";
		debugPrint(SELF + "extensionsStr = ", extensionsStr);
		final Map<String, String> extMap = new LinkedHashMap<String, String>();
		if (Utils.isEmpty(extensionsStr)) return extMap;
		String extStr = extensionsStr;
		if (extStr.startsWith(EXTENSION_SEP)) 
			extStr = extStr.substring(1);
		if (extStr.endsWith(EXTENSION_SEP)) 
			extStr = Utils.rightChop(extStr, 1);
		final String[] data = extStr.split(EXTENSION_SEP);
		for (int dNum = 0; dNum < data.length; dNum += 2) {
			if (!Utils.isEmpty(data[dNum])) {
				extMap.put(data[dNum], data[dNum + 1]);
			} // if there is a student to give an extension to
		} // for each extension
		debugPrint(SELF + "returning ", extMap);
		return extMap;
	} // getExtensions(String)

	/** Sets this assignment's extensions from a string.
	 * @param	extensionsStr	the string, userId1/ext1/...
	 */
	public void setExtensions(String extensionsStr) {
		final String currentExts = extensionsToString();
		if (!currentExts.equals(extensionsStr)) {
			setExtensions(getExtensions(extensionsStr));
			changed[EXTS] = true;
		}
	} // setExtensions(String)

	/** Clears this assignment's extensions.  */
	public void clearExtensions() {
		if (!Utils.isEmpty(extensions)) {
			extensions.clear();
			changed[EXTS] = true;
		} // if there are extensions already
	} // clearExtensions()

	/** Adds an extension, or replaces an existing extension.  Called from
	 * homework/submitUnsubmitted.jsp.
	 * @param	userId	the username of the student getting the extension
	 * @param	extension	the extension
	 */
	public void addExtension(String userId, double extension) {
		if (extensions == null) {
			extensions = new LinkedHashMap<String, String>();
		} // if extensions is null
		extensions.put(userId, String.valueOf(extension));
	} // addExtension(String, double)

	/** Removes a username from the extensions map.
	 * @param	userId	the student's username
	 */
	public void removeExtension(String userId) {
		if (extensions != null && userId != null) {
			final String extension = extensions.remove(userId);
			if (extension != null) changed[EXTS] = true;
		} 
	} // removeExtension(String)

	/** Gets the number of days or minutes in extension of a user.
	 * @param	userId	login ID of the user working on this assignment
	 * @return	length of extension of a user; 0 if user is not found in
	 * extensions, INDEFINITE if value can't be parsed as a double
	 */
	public double getExtension(String userId) {
		final double extension = getExtension(userId, extensions);
		debugPrint("Assgt.getExtension: extension for ", userId, 
				" on assignment ", name, " is ", extension, 
				isExam() ? " minutes" : " days");
		return extension;
	} // getExtension(String)

	/** Gets the number of days or minutes in extension of a user.
	 * @param	userId	login ID of the user working on this assignment
	 * @param	extensions	Map containing which students have what
	 * extensions
	 * @return	length of extension of a user; 0 if user is not found in
	 * extensions, INDEFINITE if value can't be parsed as a double
	 */
	public static double getExtension(String userId, 
			Map<String, String> extensions) {
		final String SELF = "Assgt.getExtension: ";
		double extension = 0.0;
		final String extensionStr = getExtensionStr(userId, extensions);
		if (extensionStr != null) {
			extension = MathUtils.parseDouble(extensionStr, INDEFINITE);
		} else debugPrint(SELF + "userId or extensions is null, returning 0.");
		return extension;
	} // getExtension(String, Map<String, String>)

	/** Gets the string value of the number of days or minutes in extension of a 
	 * user.
	 * @param	userId	login ID of the user working on this assignment
	 * @return	string value of length of extension of a user; null if user is 
	 * not found in extensions
	 */
	public String getExtensionStr(String userId) {
		return getExtensionStr(userId, extensions);
	} // getExtensionStr(String)

	/** Gets the string value of the number of days or minutes in extension of a 
	 * user.
	 * @param	userId	login ID of the user working on this assignment
	 * @param	extensions	Map containing which students have what
	 * extensions
	 * @return	string value of length of extension of a user; null if user is 
	 * not found in extensions
	 */
	public static String getExtensionStr(String userId, 
			Map<String, String> extensions) {
		return (userId != null && extensions != null
				? extensions.get(userId) : null);
	} // getExtensionStr(String, Map<String, String>)

	/** Gets whether the assignment is not past due, accounting for any
	 * extensions. Does not take timed exam duration into account. Called 
	 * only by hwcreator/hwSetList.jsp.
	 * @return	true if current date is before due date
	 * or if RECORD_AFTER_DUE flag is on
	 */
	public boolean isSolvingAllowed() {
		final String NO_NAME = null;
		return isSolvingAllowed(NO_NAME, NO_DATE);
	} // isSolvingAllowed()

	/** Gets whether the assignment is not past due or timed out, accounting 
	 * for any extensions. Called only by hwcreator/hwSetList.jsp.
	 * @param	userId	login ID of the user working on this assignment
	 * @return	true if current date is before due date
	 * or if RECORD_AFTER_DUE flag is on
	 */
	final public boolean isSolvingAllowed(String userId) {
		final String SELF = "Assgt.isSolvingAllowed(String): ";
		Date firstEntry = NO_DATE;
		try {
			firstEntry = HWRead.getAssgtEntryDate(id, userId);
			debugPrint(SELF + "firstEntry from DB = ", firstEntry == null 
					? "null" : DateUtils.getString(firstEntry));
		} catch (DBException e) {
			Utils.alwaysPrint(SELF + "caught DBException trying to get "
					+ "first entry into assignment ", id, " for ",
					userId);
		} // try
		return isSolvingAllowed(userId, firstEntry);
	} // isSolvingAllowed(String)

	/** Gets whether the assignment is not past due, accounting for any
	 * extensions, and accounting for the duration of a timed exam. 
	 * Called by HWSession.
	 * @param	userId	login ID of the user working on this assignment
	 * @param	firstEntry	when the student first entered the assignment
	 * @return	true if current date is before due date
	 * or if RECORD_AFTER_DUE flag is on
	 */
	final public boolean isSolvingAllowed(String userId, Date firstEntry) {
		final String SELF = "Assgt.isSolvingAllowed(String, Date): ";
		boolean notPastDue = true;
		if (!recordAfterDue()) {
			final Date modDueDate = getDueDate(userId, firstEntry);
			if (modDueDate == null) {
				debugPrint(SELF + "no limit to extension.");
			} else {
				final Date now = new Date();
				notPastDue = now.before(modDueDate);
				debugPrint(SELF + "due date & time = ",
						DateUtils.getString(modDueDate),
						", current time = ", DateUtils.getString(now),
						", notPastDue = ", notPastDue);
			} // if there's not an indefinite extension for the user
		} else debugPrint(SELF + "record-after-due flag turned on, "
				+ "so solving is always allowed.");
		return notPastDue;
	} // isSolvingAllowed(String, Date)

	/** Gets the due date plus the extension for a user.
	 * @param	userId	login ID of the user working on this assignment
	 * @return	due date + extension; null if extension &lt; 0
	 */
	public Date getDueDate(String userId) {
		return getDueDate(dueDate, getExtension(userId), isExam());
	} // getDueDate(String)

	/** Gets the due date plus an extension.
	 * @param	extension	number of days or minutes in this user's extension
	 * @return	due date + extension; null if extension &lt; 0
	 */
	public Date getDueDate(double extension) {
		return getDueDate(dueDate, extension, isExam());
	} // getDueDate(double)

	/** Gets the due date plus the extension.
	 * @param	dueDate	due date before the extension has been applied
	 * @param	extension	number of days or minutes in this user's extension
	 * @param	isExam	true if this assignment is an exam
	 * @return	due date + extension; null if extension &lt; 0
	 */
	public static Date getDueDate(Date dueDate, double extension,
			boolean isExam) {
		final String SELF = "Assgt.getDueDate(Date, double, boolean): ";
		Calendar dueCal = null;
		if (extension != INDEFINITE && dueDate != NO_DATE) {
			dueCal = Calendar.getInstance();
			debugPrint(SELF + "setting due to default value ",
					DateUtils.getString(dueDate));
			dueCal.setTime(dueDate);
			if (extension != 0.0) {
				final int extensionInSecs = (int) 
						(extension * (isExam ? 60 : 60 * 60 * 24));
				dueCal.add(Calendar.SECOND, extensionInSecs);
				debugPrint(SELF + "after adding ", extensionInSecs,
						" seconds, setting due to ",
						DateUtils.getString(dueCal.getTime()));
			} // if extension > 0
		} else {
			debugPrint(SELF + "extension is indefinite or current due date "
					+ "is null; setting new due date to null.");
		} // if extension is not indefinite
		final Date due = (dueCal == null ? NO_DATE : dueCal.getTime());
		debugPrint(SELF + "due = ", due == NO_DATE ? "null"
				: DateUtils.getString(due));
		return due;
	} // getDueDate(Date, double, boolean)

	/** For a user, gets the due date plus the extension, or, for a timed exam, 
	 * the time of first entry plus the duration plus the extension, if it is
	 * sooner.
	 * @param	userId	login ID of the user working on this assignment
	 * @param	firstEntry	date of first entry of the user into the exam
	 * @return	due date + extension; null if extension &lt; 0
	 */
	public Date getDueDate(String userId, Date firstEntry) {
		final String SELF = "Assgt.getDueDate(String, Date): ";
		Date modDueDate = getDueDate(dueDate, getExtension(userId), isExam());
		debugPrint(SELF + "got extension-modified due date of ",
				modDueDate == NO_DATE ? "null"
					: DateUtils.getString(modDueDate));
		final boolean isTimed = isTimed();
		if (modDueDate != NO_DATE) {
			if (isTimed) {
				if (firstEntry == NO_DATE) {
					debugPrint(SELF + "assignment is timed, but firstEntry "
							+ "is null, so return this value.");
				} else {
					debugPrint(SELF + "assignment is timed, firstEntry = ",
							DateUtils.getString(firstEntry), "; see if allowed "
							+ "time on assignment has been exceeded.");
				} // if firstEntry
			} else {
				debugPrint(SELF + "assignment is not timed, so return this value.");
			} // if timed assignment
		} // if no due date
		if (modDueDate != NO_DATE && isTimed && firstEntry != NO_DATE) {
			final Date durationDue = getDurationDue(userId, firstEntry);
			debugPrint(SELF + "timed assgt; first entry = ",
					DateUtils.getString(firstEntry),
					", duration of timed assgt = ", duration,
					" min, duration due date = ",
					durationDue == null ? "null" 
						: DateUtils.getString(durationDue),
					", assignment due date = ",
					DateUtils.getString(modDueDate));
			if (durationDue != NO_DATE 
					&& durationDue.before(modDueDate)) {
				debugPrint(SELF + "duration due date comes before assignment "
						+ "due date; setting due date to duration due date.");
				modDueDate = durationDue;
			} // if duration due date comes first
		} // if assignment is timed
		return (modDueDate == null ? null : modDueDate);
	} // getDueDate(String, Date)

	/** Gets the time since the student first entered this assignment plus
	 * the assignment's duration plus the student's extension in minutes.
	 * @param	userId	login ID of the user working on this assignment
	 * @param	firstEntry	when the student first entered the assignment
	 * @return	true if the assignment has timed out
	 */
	private Date getDurationDue(String userId, Date firstEntry) {
		final String SELF = "Assgt.getDurationDue: ";
		final double extension = getExtension(userId);
		if (extension == INDEFINITE) {
			debugPrint(SELF + "extension is indefinite, returning null.");
			return NO_DATE;
		} // if extension is indefinite
		if (firstEntry == null) {
			debugPrint(SELF + "no first entry, returning null.");
			return NO_DATE;
		} // if extension is indefinite
		final Calendar timedEnd = Calendar.getInstance();
		timedEnd.setTime(firstEntry);
		timedEnd.add(Calendar.MINUTE, duration);
		if (extension != 0.0) {
			timedEnd.add(Calendar.SECOND, (int) (extension * 60));
		} // if extension > 0
		debugPrint(SELF + "assgt is timed; due time, i.e. ",
				duration + extension, " min after first entry "
				+ "including any extension, is ",
				DateUtils.getString(timedEnd.getTime()));
		return timedEnd.getTime();
	} // getDurationDue(String, Date)

	/** Gets the due date plus the extension for a user in String format.
	 * @param	userId	login ID of the user working on this assignment
	 * @param	zone	time zone of this course
	 * @return	String representing due date + extension for this user
	 */
	public String getDueDateString(String userId, TimeZone zone) {
		return getDueDateString(userId, zone, false);
	} // getDueDateString(String, TimeZone)

	/** Gets the due date plus the extension for a user in String format.
	 * @param	userId	login ID of the user working on this assignment
	 * @param	zone	time zone of this course
	 * @param	day1st	user prefers to put the day first
	 * @return	String representing due date + extension for this user
	 */
	public String getDueDateString(String userId, TimeZone zone, 
			boolean day1st) {
		final Date modDueDate = getDueDate(userId);
		return (modDueDate == NO_DATE /* null */ ? "No due date."
				: Utils.toString(
					DateUtils.getStringDate(modDueDate, zone, day1st), ", ",
					DateUtils.getStringTime(modDueDate, zone)));
	} // getDueDateString(String, TimeZone, boolean)

	/** Gets if this assignment has extensions. 
	 * @return	true if this assignment has extensions
	 */
	public boolean hasExtensions() {
		return !Utils.isEmpty(extensions);
	} // hasExtensions()

	/** Converts a map of extensions into a string.  
	 * @return	a string of userId/extension/...
	 */
	public String extensionsToString() {
		final StringBuilder bld = new StringBuilder();
		if (hasExtensions()) {
			final List<String> userIds = 
					new ArrayList<String>(extensions.keySet());
			for (final String userId : userIds) {
				final String extension = extensions.get(userId);
				if (bld.length() > 0) bld.append(EXTENSION_SEP);
				Utils.appendTo(bld, userId, EXTENSION_SEP, extension);
			} // for each pair
		} // if there are extensions
		return bld.toString();
	} // extensionsToString()

/* ***************** Methods involving grading ******************/

	/** Gets if there any grading parameters.
	 * @return	true if there are any grading parameters
	 */
	public boolean hasGradingParams() {
		return hasGradingParams(ATTEMPT) || hasGradingParams(TIME);
	} // hasGradingParams()

	/** Gets if there any grading parameters of a particular kind.
	 * @param	type	the type of grading parameter
	 * @return	true if there are any grading parameters of the kind
	 */
	public boolean hasGradingParams(int type) {
		return (type == PTS_PER_Q ? allQsPointValue() != 1.0
				: !Utils.isEmpty(gradingParams[type][FACTORS]));
	} // hasGradingParams(int)

	/** Gets the value of every question in an assignment, or -1 if they
	 * differ from one another.
	 * @return	the point value of every question, or -1 if they differ from one
	 * another
	 */
	public double allQsPointValue() {
		final String valueStr = allQsPointString();
		return (valueStr == null ? -1.0 : MathUtils.parseDouble(valueStr));
	} // allQsPointValue()

	/** Gets the value of every question in an assignment as entered (as a
	 * string).
	 * @return	the point value of every question as a string; null if different
	 * questions have different point values
	 */
	public String allQsPointString() {
		double firstVal = 1.0;
		boolean first = true;
		for (final AssgtQGroup qGrp : getQGroups()) {
			if (first) {
				firstVal = MathUtils.parseDouble(qGrp.getPts());
				first = false;
			} else if (MathUtils.parseDouble(qGrp.getPts()) != firstVal) {
				return null;
			} // if first
		} // for each question group
		if (hasGradingParams(ATTEMPT)) {
			final String[][] attemptParams = getGradingParams(ATTEMPT);
			firstVal *= MathUtils.parseDouble(attemptParams[FACTORS][0]);
		} // if there are attempt-dependent grading parameters
		final String firstValStr = String.valueOf(firstVal);
		return (firstValStr.endsWith(".0") 
				? Utils.rightChop(firstValStr, 2) : firstValStr);
	} // allQsPointString()

	/** Gets the maximum grade achievable for each seen question from the 
	 * points-per-Q grading parameters and the maximum attempt grading
	 * parameter.
	 * @return	array of maximum points, as Strings
	 */
	 public String[] getMaxPointsPerQArray() {
		final String[] maxPtsArr = getPointsPerQArray();
		if (hasGradingParams(ATTEMPT)) {
			final String[][] attParams = getGradingParams(ATTEMPT);
			final double factor = MathUtils.parseDouble(attParams[FACTORS][0]);
			if (factor != 1.0) {
				for (int qNum = 0; qNum < maxPtsArr.length; qNum++) {
					final double val = MathUtils.parseDouble(maxPtsArr[qNum]);
					maxPtsArr[qNum] = String.valueOf(val * factor);
					if (maxPtsArr[qNum].endsWith(".0")) {
						maxPtsArr[qNum] = Utils.rightChop(maxPtsArr[qNum], 2);
					} // if needs to be truncated
				} // for each question's points
			} // if the first attempt parameter is 1
		} // if there are attempt-dependent grading parameters
		return maxPtsArr;
	} // getMaxPointsPerQArray()

	/** Gets an array of points per seen Q.
	 * @return	array of points per seen Q
	 */
	 private String[] getPointsPerQArray() {
		final int numQsSeen = getNumQsSeen();
		final String[] ptsPerQArr = new String[numQsSeen];
		int seenQNum = 0;
		for (final AssgtQGroup qGrp : qGrps) {
			final int grpQsSeen = qGrp.getNumQsSeen();
			for (int seenQGrpNum = 0; seenQGrpNum < grpQsSeen; seenQGrpNum++) {
				ptsPerQArr[seenQNum] = qGrp.getPts();
				seenQNum++;
			} // for each seen Q in the group
		} // for each Q group
		return ptsPerQArr;
	} // getPointsPerQArray()

	/** Gets the maximum grade of the assignment.
	 * @return	the maximum grade of the assignment
	 */
	public double getMaxGrade() {
		double sum = 0.0;
		for (final AssgtQGroup qGrp : getQGroups()) {
			final String pts = qGrp.getPts();
			final int bundleSize = qGrp.getBundleSize();
			final int pick = qGrp.getPick();
			sum += MathUtils.parseDouble(pts) * bundleSize * pick;
		} // for each question group
		return sum;
	} // getMaxGrade()

	/** Gets the maximum grade of each assignment in an array.
	 * @param	assgts	an array of assignments
	 * @return	the maximum grades of the assignments in a parallel array
	 */
	public static double[] getMaxGrades(Assgt[] assgts) {
		final double[] maxGrades = new double[assgts.length];
		int assgtNum = 0;
		for (final Assgt assgt : assgts) {
			maxGrades[assgtNum++] = assgt.getMaxGrade();
		} // for each assignment
		return maxGrades;
	} // getMaxGrades(Assgt[])

	/** Converts grading parameters to a readable description.
	 * @param	type	indicates question-, attempt-, or time-dependent grading 
	 * parameters
	 * @param	past	whether to use past or future tense
	 * @param	user	the user for whom this description is being prepared
	 * @return	readable description of the grading parameters
	 */
	public String gradingParamsToDisplay(int type, boolean past, User user) {
		return gradingParamsToDisplay(type, past, EVERY_Q, user);
	} // gradingParamsToDisplay(int, boolean, User)

	/** Converts grading parameters to a readable description.
	 * @param	type	indicates question-, attempt-, or time-dependent grading 
	 * parameters
	 * @param	past	whether to use past or future tense
	 * @param	currentQNum	the 1-based question number of whose grading to get
	 * a description; 0 if any question
	 * @param	user	the user for whom this description is being prepared to
	 * @return	readable description of the grading parameters
	 */
	public String gradingParamsToDisplay(int type, boolean past, 
			int currentQNum, User user) {
		return (type == PTS_PER_Q ? pointsPerQToDisplay(past, currentQNum, user)
				: type == ATTEMPT ? attemptGradingParamsToDisplay(past, user)
				: timeGradingParamsToDisplay(past, user));
	} // gradingParamsToDisplay(int, boolean, int, User)

	/** Converts points per question in this assignment into a readable
	 * description.
	 * @param	past	whether to use past or future tense
	 * @param	currentQNum	the 1-based question number of whose grading to get 
	 * a description; 0 if any question
	 * @param	user	the user for whom this description is being prepared
	 * @return	readable description of the points per question
	 */
	private String pointsPerQToDisplay(boolean past, int currentQNum, 
			User user) {
		String words = "";
		final String ptsPerEveryQ = allQsPointString();
		final boolean everyQSame = ptsPerEveryQ != null;
		final StringBuilder ptsPerQBld = new StringBuilder();
		int realQNum = 1;
		if (currentQNum == EVERY_Q && !everyQSame) {
			for (final AssgtQGroup qGrp : getQGroups()) {
				final String pts = qGrp.getPts();
				final int numQsSeen = qGrp.getNumQsSeen();
				for (int seenQNum = 0; seenQNum < numQsSeen; seenQNum++) {
					if (ptsPerQBld.length() > 0) ptsPerQBld.append("<br/>");
					final String sent = Utils.toString(
							"Question ***1*** ", past ? "was" : "is",
							" worth ***4*** point", 
							"1".equals(pts) ? "." : "s.");
					final String[] nums = 
							new String[] {String.valueOf(realQNum++), pts};
					ptsPerQBld.append(user.translate(sent, nums));
				} // for each seen question in the group
			} // for each group
			words = ptsPerQBld.toString();
		} else if (everyQSame) {
			final String pts = ptsPerEveryQ;
			words = user.translate(Utils.toString(currentQNum == EVERY_Q 
						? "Every" : "This", " question ", past ? "was" : "is",
					" worth ***4*** point", "1".equals(pts) ? "." : "s."), pts);
		} else {
			int numIndex = 1;
			for (final AssgtQGroup qGrp : getQGroups()) {
				final int numQsSeen = qGrp.getNumQsSeen();
				if (currentQNum >= numIndex 
						&& currentQNum < numIndex + numQsSeen) {
					final String pts = qGrp.getPts();
					words = user.translate(Utils.toString("This question ", 
							past ? "was" : "is", " worth ***4*** point", 
							"1".equals(pts) ? "." : "s."), pts);
					break;
				} // for each seen question in the group
				numIndex += numQsSeen;
			} // for each group
		} // if describing all assignment Qs and they are not all worth same
		return words;
	} // pointsPerQToDisplay(boolean, int, User)

	/** Converts attempt-dependent grading parameters to a readable description.
	 * @param	past	whether to use past or future tense
	 * @param	user	the user for whom this description is being prepared
	 * @return	readable description of the attempt-dependent grading parameters
	 */
	private String attemptGradingParamsToDisplay(boolean past, User user) {
		final String[][] attemptParams = gradingParams[ATTEMPT];
		if (Utils.isEmpty(attemptParams[LIMITS])
				|| Utils.isEmpty(attemptParams[FACTORS])) return "";
		final StringBuilder attemptGradingInEnglish = new StringBuilder();
		final int numGradingParams = attemptParams[LIMITS].length;
		int prevLim = 0; 
		for (int paramNum = 1; paramNum <= numGradingParams; paramNum++) {
			final String limitStr = attemptParams[LIMITS][paramNum - 1];
			String factor = attemptParams[FACTORS][paramNum - 1];
			if (!Utils.isEmpty(factor) && factor.charAt(0) == '.') 
				factor = "0" + factor;
			final boolean lesser = paramNum < numGradingParams;
			final int limit = MathUtils.parseInt(limitStr);
			final boolean equals = prevLim + 1 == limit;
			final String sentence = Utils.toString(
					"If the number of attempts is ",
					lesser && equals ? "***10***"
						: lesser ? "from ***4*** to ***6***" 
						: "***8*** or more",
					", ACE ", past ? "has multiplied" : "will multiply",
					" the grade by ***0.8***.");
			final String start = String.valueOf(prevLim + 1);
			final String[] nums = 
					(lesser && equals ? new String[] {limitStr, factor}
					: lesser ? new String[] {start, limitStr, factor}
					: new String[] {start, factor});
			attemptGradingInEnglish.append(user.translate(sentence, nums));
			if (paramNum < numGradingParams)
				attemptGradingInEnglish.append("<br/>");
			prevLim = limit;
		} // for each grading parameter set paramNum 
		return attemptGradingInEnglish.toString();
	} // attemptGradingParamsToDisplay(boolean, User)

	/** Converts time-dependent grading parameters to an English description.
	 * @param	past	whether to use past or future tense
	 * @param	user	the user for whom this description is being prepared
	 * @return	English description of the time-dependent grading parameters
	 */
	private String timeGradingParamsToDisplay(boolean past, User user) {
		final String[][] timeParams = gradingParams[TIME];
		if (Utils.isEmpty(timeParams[LIMITS])
				|| Utils.isEmpty(timeParams[FACTORS])) return "";
		final StringBuilder timeGradingInEnglish = new StringBuilder();
		final int numGradingParams = timeParams[LIMITS].length;
		String prevLim = "0";
		for (int paramNum = 1; paramNum <= numGradingParams; paramNum++) {
			String limit = timeParams[LIMITS][paramNum - 1];
			String factor = timeParams[FACTORS][paramNum - 1];
			if (!Utils.isEmpty(limit) && limit.charAt(0) == '.') 
				limit = "0" + limit;
			if (!Utils.isEmpty(factor) && factor.charAt(0) == '.') 
				factor = "0" + factor;
			final String sentence = Utils.toString(
					"If a student has submitted his or her "
						+ "last response more than ***1*** ",
					paramNum == numGradingParams ? ""
						: "and not more than ***2*** ",
					isExam() ? "minute(s)" : "day(s)",
					" past the due ", isExam() ? "" : "date and ",
					"time, ACE ", past ? "has multiplied" : "will multiply",
					" the grade by ***0.8***.");
			final String[] nums = (paramNum == numGradingParams
					? new String[] {prevLim, factor}
					: new String[] {prevLim, limit, factor});
			timeGradingInEnglish.append(user.translate(sentence, nums));
			if (paramNum < numGradingParams)
				timeGradingInEnglish.append("<br/>");
			prevLim = limit;
		} // for each grading parameter set paramNum 
		return timeGradingInEnglish.toString();
	} // timeGradingParamsToDisplay(boolean, User)

	/** Converts a grading parameters string to a pair of arrays, returning the
	 * value.  Called by db/DataConversion.
	 * @param	paramsStr	the string encoding the /-separated limits and
	 * factors (factors only for points-per-question parameters)
	 * @param	type	the type of grading parameters
	 * @return	a pair of arrays with limits and factors
	 */
	public static String[][] getParamsArrs(String paramsStr, int type) {
		final Assgt assgt = new Assgt();
		assgt.setGradingParams(paramsStr, type);
		return assgt.gradingParams[type];
	} // getParamsArrs(String, int)

	/** Converts a grading parameters array to a string containing /-separated 
	 * limits and factors (factors only for points-per-question parameters).
	 * Used for XML and for communication with the front end.
	 * @param	type	the type of grading parameters
	 * @return	a String encoding the grading parameters
	 */
	public String getParamsString(int type) {
		if (type == PTS_PER_Q) {
			final List<String> ptsPerSeenQList = new ArrayList<String>();
			for (final AssgtQGroup qGrp : getQGroups()) {
				final String pts = qGrp.getPts();
				final int numQsSeen = qGrp.getNumQsSeen();
				for (int seenQNum = 0; seenQNum < numQsSeen; seenQNum++) {
					ptsPerSeenQList.add(pts);
				} // for each seen question in the group
			} // for each group
			return Utils.join(ptsPerSeenQList, "/");
		} // if points per question
		final StringBuilder bld = new StringBuilder();
		final int numParams = gradingParams[type][LIMITS].length;
		for (int paramNum = 0; paramNum < numParams; paramNum ++) {
			if (bld.length() > 0) bld.append('/');
			bld.append(Utils.getBuilder(gradingParams[type][LIMITS][paramNum], 
					'/', gradingParams[type][FACTORS][paramNum]));
		} // for each parameter value
		return bld.toString();
	} // getParamsString(int)

/* ******** Methods regarding question and assignment dependencies ********/

	/** Gets the ID of the question that must be answered correctly before the
	 * given question can be displayed.
	 * @param	qId	ID number of the question whose display is being queried
	 * @return	the ID of the question that must be answered correctly before
	 * the given question can be displayed
	 */
	public int getDependsOn(int qId) {
		final Integer qIdObj = Integer.valueOf(qId);
		int dependsOn = 0;
		for (final AssgtQGroup qGrp : getQGroups()) {
			final List<Integer> qIds = qGrp.getQIds();
			if (qIds.contains(qIdObj)) {
				dependsOn = qGrp.getDependsOn();
				break;
			} // if the group contains the question
		} // for each question group
		return dependsOn;
	} // getDependsOn(int)

	/** Gets whether a question should be displayed, based on whether it 
	 * has a question on which it depends and whether that question has been 
	 * answered correctly.
	 * @param	qId	ID number of the question whose display is being
	 * queried
	 * @param	evalResults	student's results for a particular assignment
	 * @return	true if the question can be displayed
	 */
	public boolean getOkToDisplay(int qId, EvalResult[] evalResults) {
		boolean okToDisplay = true;
		final Integer qIdObj = Integer.valueOf(qId);
		for (final AssgtQGroup qGrp : getQGroups()) {
			final List<Integer> qIds = qGrp.getQIds();
			if (qIds.size() == 1 && qIds.contains(qIdObj)) {
				okToDisplay = qGrp.getOkToDisplay(evalResults);
				break;
			} // if the group contains the question
		} // for each question group
		return okToDisplay;
	} // getOkToDisplay(int, EvalResult[])

	/** Gets whether the given student has mastered the mastery assignment which
	 * must be mastered before this assignment can be begun.
	 * @param	studentId	login ID of the student
	 * @return	whether the student has mastered the required assignment
	 */
	public boolean hasMasteredRequiredHW(String studentId) {
		boolean hasMastered = false;
		try {
			hasMastered = ResponseRead.hasMastered(studentId, dependsOnId);
		} catch (DBException e) {
			e.printStackTrace();
		}
		return hasMastered;
	} // hasMasteredRequiredHW(String)

/* ***************** Database methods ******************/

	/** Processes the name to write.
	 * @return modified name to store in database; also modifies
	 * the name
	 */
	public String getDbName() {
		return (Utils.isEmpty(name) ? ""
				: Utils.unicodeToCERs(name.trim()));
	} // getDbName()

	/** Processes the remarks to write.
	 * @return modified remarks to store in database; also modifies
	 * the remarks
	 */
	public String getDbRemarks() {
		return (Utils.isEmpty(remarks) ? ""
				: Utils.unicodeToCERs(remarks.trim()));
	} // getDbRemarks()

	/** Processes the number of tries.
	 * @return number of tries to store in database
	 */
	public int getDbTries() {
		return (allowUnlimitedTries ? UNLIMITED 
				: maxTries == 0 ? 20 : maxTries);
	} // getDbTries()

/* ***************** XML methods ******************/

	/** Gets the XML tag for Assgts. 
	 * @return	the XML tag for Assgts
	 */
	public static String getTag() { return HWSET_DESCR_TAG; }

	/** Converts the Assgt to XML.
	 * @return	 the XML
	 */
	public String toXML() {
		final String SELF = "Assgt.toXML: ";
		final StringBuilder xmlBld = Utils.getBuilder(
				startDescr(),
				makeNode(NAME_TAG, name),
				makeNode(REMARKS_TAG, remarks),
				makeNode(FLAGS_TAG, flags),
				makeNode(ALLOW_UNLIMITED_TAG, allowUnlimitedTries),
				makeNode(MAX_TRIES_TAG, maxTries),
				makeNode(INSTRUCTORID_TAG, instructorId));
		final int[] allowedRxnCondns = getAllowedRxnCondns();
		if (!Utils.isEmpty(allowedRxnCondns)) {
			xmlBld.append(makeNode(ALLOWED_RXNCONDNS_TAG,
					Utils.join(allowedRxnCondns, RXN_CONDN_ID_SEP)));
		}
		if (hasGradingParams(ATTEMPT)) {
			xmlBld.append(makeNode(ATTEMPT_GRADING_TAG, getParamsString(ATTEMPT)));
		}
		if (hasGradingParams(TIME)) {
			xmlBld.append(makeNode(TIME_GRADING_TAG, getParamsString(TIME)));
		}
		final StringBuilder qGrpsBld = new StringBuilder();
		for (final AssgtQGroup qGrp : qGrps) {
			qGrpsBld.append(qGrp.toXML());
		} // for each qGrp
		final String xml = Utils.toString(xmlBld, 
				XMLUtils.wrapNode(Q_GRPS_TAG, qGrpsBld), endDescr());
		debugPrint(SELF, xml);
		return xml;
	} // toXML()

	/** Wraps a tag and its id attribute with &lt; &gt;.
	 * @return	the tag and its id attribute wrapped in &lt; &gt;
	 */
	private StringBuilder startDescr() {
		return XMLUtils.startTag(HWSET_DESCR_TAG, new String[][] {
					{"id", String.valueOf(id)}
				});
	} // startDescr()

	/** Wraps a tag with &lt;/ &gt;.
	 * @return	the tag wrapped in &lt;/ &gt;
	 */
	private StringBuilder endDescr() {
		return XMLUtils.endTag(HWSET_DESCR_TAG);
	} // endDescr()

	/** Surround the given text with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	text	the text of the node
	 * @return	the text surrounded by XML tags
	 */
	private StringBuilder makeNode(String tag, int text) {
		return XMLUtils.makeNode(tag, text);
	} // makeNode(String, int)

	/** Surround the given text with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	text	the text of the node
	 * @return	the text surrounded by XML tags
	 */
	private StringBuilder makeNode(String tag, boolean text) {
		return XMLUtils.makeNode(tag, text);
	} // makeNode(String, boolean)

	/** Surround the given text with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	text	the text of the node
	 * @return	the text surrounded by XML tags
	 */
	private StringBuilder makeNode(String tag, String text) {
		return XMLUtils.makeNode(tag, text);
	} // makeNode(String, String)

	/** Converts XML in an imported assignment to an Assgt. 
	 * @param	node	information about the assignment
	 * @return	the parsed Assgt
	 */
	public static Assgt parseXML(Node node) {
		debugPrint("Assgt.parseXML: parsing hwset assgt ---------------------" );
		final NodeList nodeList = node.getChildNodes();
		final Assgt assgt = new Assgt();
		assgt.id = 0;
		for (int nodeNum = 0; nodeNum < nodeList.getLength(); nodeNum++) {
			final Node n = nodeList.item(nodeNum);
			if (n.getNodeType() == Node.TEXT_NODE) {
				continue;
			} else if (n.getNodeName().equalsIgnoreCase(NAME_TAG)) {
				assgt.setName(n.getFirstChild().getNodeValue().trim());
			} else if (n.getNodeName().equalsIgnoreCase(REMARKS_TAG)) {
				assgt.setRemarks(n.getFirstChild() == null ? ""
						: n.getFirstChild().getNodeValue().trim());
			} else if (n.getNodeName().equalsIgnoreCase(
					ALLOW_UNLIMITED_TAG)) {
				final String val = n.getFirstChild().getNodeValue().trim();
				if ("true".equals(val)) assgt.setMaxTries(UNLIMITED);
			} else if (n.getNodeName().equalsIgnoreCase(MAX_TRIES_TAG)) {
				final String maxTriesStr = n.getFirstChild().getNodeValue();
				assgt.setMaxTries(MathUtils.parseInt(maxTriesStr));
			} else if (n.getNodeName().equalsIgnoreCase(FLAGS_TAG)) {
				final String flagsStr = n.getFirstChild().getNodeValue();
				assgt.setFlags(MathUtils.parseInt(flagsStr));
			} else if (n.getNodeName().equalsIgnoreCase(INSTRUCTORID_TAG)) {
				assgt.instructorId = n.getFirstChild().getNodeValue().trim();
			} else if (n.getNodeName().equalsIgnoreCase(Q_GRPS_TAG)
					&& n.getFirstChild() != null) {
				assgt.setQGroups(AssgtQGroup.parseXML(n));
			} else if (n.getNodeName().equalsIgnoreCase(ATTEMPT_GRADING_TAG)
					&& n.getFirstChild() != null) {
				assgt.setGradingParams(
						n.getFirstChild().getNodeValue().trim(), ATTEMPT);
			} else if (n.getNodeName().equalsIgnoreCase(TIME_GRADING_TAG)
					&& n.getFirstChild() != null) {
				assgt.setGradingParams(
						n.getFirstChild().getNodeValue().trim(), TIME);
			} else if (n.getNodeName().
					equalsIgnoreCase(ALLOWED_RXNCONDNS_TAG)) {
				assgt.setAllowedRxnCondns(n.getFirstChild() == null ? ""
						: n.getFirstChild().getNodeValue().trim());
			} // if node
		} // for each node
		debugPrint("Assgt.parseXML:  hwset assgt parsed  ---------------- ");
		return assgt; 
	} // parseXML(Node)

	/** For debugging. 
	 * @return	String representation of this Assgt 
	 */
	public String toString() {
		final StringBuilder bld = Utils.getBuilder(
				"\nid = ", id,
				"\ninstructor id = ", instructorId,
				"\ncourse id = ", courseId,
				"\nname = ", name,
				"\nremarks = ", remarks,
				"\nflags = ", flags,
				"\nallowUnlimitedTries = ", allowUnlimitedTries,
				"\nmaxTries = ", maxTries,
				"\ninstructorId = ", instructorId,
				"\npointsPerQuestion = ", allQsPointString(),
				"\nattemptGrading = ", getParamsString(ATTEMPT),
				"\ntimeGrading = ", getParamsString(TIME),
				"\nextensions = ", extensionsToString(),
				"\nchanged = ", Arrays.toString(changed));
		int grpNum = 0;
		for (final AssgtQGroup qGrp : getQGroups()) {
			bld.append(Utils.getBuilder("\nquestion group ", ++grpNum, ": ",
					qGrp.toString()));
		} // for each question group
		return bld.toString();
	} // toString()

} // Assgt
