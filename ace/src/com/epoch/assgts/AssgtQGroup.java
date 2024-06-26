package com.epoch.assgts;

import com.epoch.assgts.assgtConstants.AssgtQGroupConstants;
import com.epoch.evals.EvalResult;
import com.epoch.qBank.Question;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.XMLUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Contains a question or a group of questions within an assignment, as well as
 * their point value and, when there's just one question, which question must be
 * answered correctly before this one can be displayed.
 * When there is more than one question in this group, they are grouped 
 * into bundles of <code>bundleSize</code> consecutive questions; when 
 * an assignment is instantiated, ACE randomly chooses <code>pick</code> 
 * bundles for the student. */
public class AssgtQGroup implements AssgtQGroupConstants, QuestionConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** The questions in this group. */
	final private List<Question> questions;
	/** How many bundles of questions to pick from this group. */
	private int pick = 1;
	/** The size of each bundle of questions to pick from this group. */
	private int bundleSize = 1;
	/** The grading parameter for each seen question in this group. */
	private String pts = "1";
	/** ID number of the question that must be answered correctly before this
	 * question can be displayed. */
	private int dependsOn = 0;

	/** Constructor.  */
	public AssgtQGroup() {
		questions = new ArrayList<Question>();
	} // AssgtQGroup()

	/** Constructor.  
	 * @param	oneQ	a question
	 */
	public AssgtQGroup(Question oneQ) {
		questions = new ArrayList<Question>();
		questions.add(oneQ);
	} // AssgtQGroup(Question)

	/** Constructor.  
	 * @param	qList	a list of questions
	 */
	public AssgtQGroup(List<Question> qList) {
		questions = qList;
	} // AssgtQGroup(List<Question>)

	/** Constructor.  
	 * @param	qIds	an array of question ID numbers
	 */
	public AssgtQGroup(int[] qIds) {
		questions = new ArrayList<Question>();
		for (final int qId : qIds) {
			questions.add(new Question(qId));
		} // for each Q in the group
	} // AssgtQGroup(int[])

	/** Constructor.  
	 * @param	qIds	a list of question ID numbers
	 * @param	pk	how many bundles of questions to pick from this group
	 * @param	bsz	size of each bundle of questions to pick from this group
	 * @param	ptsVal	grading parameter for each seen question in this group
	 * @param	dependsQId	ID number of the question that must be answered
	 * before this question can be worked
	 */
	public AssgtQGroup(List<Integer> qIds, int pk, int bsz, String ptsVal,
			int dependsQId) {
		pick = pk;
		bundleSize = bsz;
		pts = ptsVal;
		dependsOn = dependsQId;
		questions = new ArrayList<Question>();
		for (final Integer qId : qIds) {
			questions.add(new Question(qId.intValue()));
		} // for each Q in the group
	} // AssgtQGroup(List<Integer>, int, int, String, int)

	/** Constructor.  
	 * @param	qGroupStr	a string representing the questions in this group
	 */
	public AssgtQGroup(String qGroupStr) {
		final String[] pickAndIds = qGroupStr.split(PICK_FROM);
		setPick(MathUtils.parseInt(pickAndIds[PICK_NUM], 1));
		final String[] qBundles = pickAndIds[QIDS].split(RANDOM_SEP);
		questions = new ArrayList<Question>();
		boolean first = true;
		for (final String qBundle : qBundles) {
			final String[] qIdStrs = qBundle.split(BUNDLED_SEP);
			if (first) {
				setBundleSize(qIdStrs.length);
				first = false;
			} // if first bundle
			for (final String qIdStr : qIdStrs) {
				questions.add(new Question(MathUtils.parseInt(qIdStr)));
			} // for each Q in the group
		} // for each bundle
	} // AssgtQGroup(String)

	/** Copy constructor.  
	 * @param	qGrp	the group to copy
	 */
	public AssgtQGroup(AssgtQGroup qGrp) {
		questions = new ArrayList<Question>();
		for (final Question question : qGrp.getQuestions()) {
			questions.add(new Question(question, PRESERVE_ID));
		} // for each question to copy
		pick = qGrp.getPick();
		bundleSize = qGrp.getBundleSize();
		pts = qGrp.getPts();
		dependsOn = qGrp.getDependsOn();
	} // AssgtQGroup(AssgtQGroup)

	/** Gets the questions in the group.
	 * @return	list of questions in the group
	 */
	public List<Question> getQuestions()		{ return questions; }
	/** Gets the total number of questions in the group.
	 * @return	the total number of questions in the group
	 */
	final public int getNumQs() 				{ return questions.size(); }
	/** Calculates the number of questions from this group that each student
	 * will see.
	 * @return	the number of questions from this group that each student 
	 * will see
	 */
	public int getNumQsSeen() 					{ return getPick() * getBundleSize(); }
	/** Gets whether there are no questions in the group.
	 * @return	true if there are no questions in the group
	 */
	public boolean isEmpty() 					{ return questions.isEmpty(); }
	/** Gets whether the group consists of more than one question from which a
	 * subset should be chosen when an assignment is instantiated.
	 * @return	true if the group contains more than one question
	 */
	public boolean isRandom()					{ return getNumQs() > getNumQsSeen(); }
	/** Adds a question to the list. 
	 * @param	q	a question
	 */
	public void addQ(Question q) 				{ questions.add(q); }
	/** Replaces a question in the list with a new one. 
	 * @param	qNum	1-based number of the question
	 * @param	q	a question
	 */
	public void setQ(int qNum, Question q)		{ questions.set(qNum - 1, q); }
	/** Inserts a question into the list. 
	 * @param	qNum	1-based number of the question after it is inserted
	 * @param	q	a question
	 */
	public void insertQ(int qNum, Question q)	{ questions.add(qNum - 1, q); }
	/** Gets the number of question bundles to pick from this group.
	 * @return	the number of question bundles to pick from this group
	 */
	public final int getPick() 					{ return pick; }
	/** Sets the number of question bundles to pick from this group.
	 * @param	count	new number of question bundles to pick from this group
	 */
	public final void setPick(int count) 		{ pick = count; }
	/** Gets the String value of each seen question in this group.
	 * @return	the value of each seen question in this group
	 */
	public final String getPts() 				{ return pts; }
	/** Sets the value of each seen question in this group.
	 * @param	val	new value of each seen question in this group
	 */
	public final void setPts(String val) 		{ pts = val; }
	/** Gets the ID number of the question that must be answered correctly 
	 * before this question can be worked.
	 * @return	the ID number of the question that must be answered correctly 
	 * before this question can be worked
	 */
	public final int getDependsOn() 			{ return dependsOn; }
	/** Sets the ID number of the question that must be answered correctly 
	 * before this question can be worked.
	 * @param	qId	ID number of the question that must be answered correctly
	 * before this question can be worked
	 */
	public final void setDependsOn(int qId) 	{ dependsOn = qId; }
	/** Gets whether this question has a dependency on another.
	 * @return true if this question has a dependency on another
	 */
	public final boolean hasDependency()		{ return dependsOn != 0; }
	/** Gets the size of the question bundles of this group.
	 * @return	the size of the question bundles of this group
	 */
	public int getBundleSize() 					{ return bundleSize; }

	/** Sets the size of the question bundles of this group.
	 * @param	count	new size of question bundles of this group
	 */
	public final void setBundleSize(int count) { 
		bundleSize = count; 
		final int numQs = getNumQs();
		if (numQs > 1) {
			final int maxPick = (numQs / bundleSize) - 1;
			if (getPick() > maxPick) setPick(maxPick);
		} // if this group has more than one Q
	} // setBundleSize(int)

	/** Gets the unique IDs of all questions in the group.
	 * @return	list of unique IDs
	 */
	public List<Integer> getQIds() {
		final List<Integer> qIds = new ArrayList<Integer>();
		for (final Question question : questions) {
			qIds.add(Integer.valueOf(question.getQId()));
		} // for each question
		return qIds;
	} // getQIds()

	/** Gets the unique ID of a question.
	 * @param	qNum	1-based number of the question
	 * @return	the unique ID of the question
	 */
	public int getQId(int qNum) {
		return (qNum > 0 && qNum <= getNumQs()
				? getQ(qNum).getQId() : 0);
	} // getQId(int)

	/** Gets a question.
	 * @param	qNum	1-based number of the question
	 * @return	the question
	 */
	public Question getQ(int qNum) {
		return (qNum > 0 && qNum <= getNumQs()
				? questions.get(qNum - 1) : null);
	} // getQ(int)

	/** Removes a question and adjusts the bundles and picks.
	 * @param	qNum	1-based number of the question
	 * @return	the removed question
	 */
	public Question removeQ(int qNum) {
		final String SELF = "AssgtQGroup.removeGroupQ: ";
		int numQs = getNumQs();
		if (qNum < 1 || qNum > numQs) return null;
		final Question removedQ = questions.remove(qNum - 1);
		numQs--;
		if (numQs == 1) {
			pick = 1;
			debugPrint(SELF + "pick changed to 1, Q made not random.");
		} else if (pick >= numQs) {
			pick = numQs - 1;
			debugPrint(SELF + "numQs now ", numQs, ", pick changed to ", pick);
		} // if newNumQs
		return removedQ;
	} // removeQ(int)

	/** Determines whether this group of questions contains random questions.
	 * @return	false if the group contains random questions
	 */
	public boolean isFixedGroup() {
		return getNumQs() == getNumQsSeen();
	} // isFixedGroup()

	/** Gets the question ID numbers, grouped into a list of bundles, each of
	 * which is itself a list.
	 * @return	a list of lists of question ID numbers
	 */
	private List<ArrayList<Integer>> getQBundles() {
		final String SELF = "AssgtQGroup.getQBundles: ";
		final List<ArrayList<Integer>> qBundles = 
				new ArrayList<ArrayList<Integer>>();
		final List<Integer> qIds = getQIds();
		debugPrint(SELF + "qIds = ", qIds, ", bundle size = ", 
				getBundleSize());
		int qIdNum = 0;
		while (qIdNum < qIds.size()) {
			final ArrayList<Integer> qBundle = new ArrayList<Integer>();
			while (qBundle.size() < getBundleSize()) {
				qBundle.add(qIds.get(qIdNum++));
			} // if reached maximum size of bundle
			qBundles.add(qBundle);
			debugPrint(SELF + "next bundle: ", qBundle);
		} // for each qId
		debugPrint(SELF, qBundles.size(), " bundles formed");
		return qBundles;
	} // getQBundles()

	/** Selects questions at random to create part of an instantiated 
	 * assignment.
	 * @return	list of IDs of randomly selected questions
	 */
	public List<Integer> instantiate() {
		final String SELF = "AssgtQGroup.instantiate: ";
		final boolean isFixed = isFixedGroup();
		final List<Integer> instantiatedQIds = (isFixed 
				? getQIds() : new ArrayList<Integer>());
		if (!isFixed) {
			final List<ArrayList<Integer>> qBundles = getQBundles();
			if (!Utils.isEmpty(qBundles)) {
				final int numToPick = getPick();
				final Random randGen = new Random();
				for (int pick = 0; pick < numToPick; pick++) {
					final int selRandom = randGen.nextInt(qBundles.size());
					final List<Integer> qBundle = qBundles.remove(selRandom);
					instantiatedQIds.addAll(qBundle);
				} // for each pick
				debugPrint(SELF + "random group; qBundles = ", qBundles, 
						", pick = ", numToPick); 
			} // if there are bundles
		} // if is random assignment
		debugPrint(SELF + "instantiated group of questions = ", 
				instantiatedQIds);
		return instantiatedQIds;
	} // instantiate()

	/** Selects questions at random to create part of an instantiated 
	 * assignment.
	 * @return	list of IDs of randomly selected questions
	 */
	public List<Integer> instantiateOneBundle() {
		final String SELF = "AssgtQGroup.instantiate: ";
		List<Integer> instantiatedQIds = new ArrayList<Integer>();
		final List<ArrayList<Integer>> qBundles = getQBundles();
		if (!Utils.isEmpty(qBundles)) {
			final Random randGen = new Random();
			final int selRandom = randGen.nextInt(qBundles.size());
			instantiatedQIds = qBundles.get(selRandom);
		} // if there are bundles
		debugPrint(SELF + "instantiated bundle = ", instantiatedQIds);
		return instantiatedQIds;
	} // instantiateOneBundle()

	/** Gets whether this question should be displayed, based on whether it 
	 * has a question on which it depends and whether that question has been 
	 * answered correctly.
	 * @param	evalResults	student's results for a particular assignment
	 * @return	true if this question should be displayed
	 */
	boolean getOkToDisplay(EvalResult[] evalResults) {
		final String SELF = "AssgtQGroup.getOkToDisplay: ";
		boolean okToDisplay = getNumQs() != 1;
		if (okToDisplay) {
			debugPrint(SELF + "more than one question in this group, "
					+ "so dependencies not allowed.");
		} else {
			final int qId = getQId(1);
			okToDisplay = dependsOn == 0;
			if (!okToDisplay) {
				debugPrint(SELF + "Q with ID ", qId, " depends on Q with ID ", 
						dependsOn, "; looking for corresponding response.");
				for (final EvalResult evalResult : evalResults)
					if (evalResult != null && evalResult.qId == dependsOn) {
						debugPrint(SELF + "grade for response to Q with ID ",
								dependsOn, " = ", evalResult.grade);
						okToDisplay = evalResult.grade == 1.0;
						break;
					} // if this question is the independent one
			} // if this Q's display status depends on another Q answered correctly
			debugPrint(SELF + "for q with ID ", qId, 
					", okToDisplay = ", okToDisplay);
		} // if this is a random grouping
		return okToDisplay;
	} // getOkToDisplay(EvalResult[])

	/** Encodes this group of questions as a string.
	 * @return	the string encoding this question group
	 */
	String getString() {
		final StringBuilder bld = new StringBuilder();
		final List<Integer> grpQIds = getQIds();
		if (grpQIds.size() == 1) {
			bld.append(grpQIds.get(0));
		} else {
	 	 	Utils.appendTo(bld, pick, PICK_FROM);
			int grpQNum = 0;
			for (final Integer grpQId : grpQIds) {
				if (grpQNum > 0) {
					final boolean endBundle = grpQNum % bundleSize == 0;
					bld.append(endBundle ? RANDOM_SEP : BUNDLED_SEP);
				}
				bld.append(grpQId);
				grpQNum++;
			} // for each Q
		} // if number of Qs in the group
		return bld.toString();
	} // getString()

	/** Converts this question group into XML.
	 * @return	the XML
	 */
	String toXML() {
		final String SELF = "AssgtQGroup.toXML: ";
		final StringBuilder xmlBld = new StringBuilder();
		final List<Integer> qIds = getQIds();
		for (final Integer qId : qIds) {
			final String[] qAttrs = 
					new String[] { ID_TAG, String.valueOf(qId) }; 
			xmlBld.append(XMLUtils.startAndCloseTag(Q_TAG, qAttrs))
					.append('\n');
		} // for each Q
		final String[][] qGrpAttrs = (dependsOn == 0
				? new String[][] { 
					{ PICK_TAG, String.valueOf(pick) }, 
					{ BUNDLE_SIZE_TAG, String.valueOf(bundleSize) }, 
					{ PTS_TAG, pts } } 
				: new String[][] { 
					{ PICK_TAG, String.valueOf(pick) }, 
					{ BUNDLE_SIZE_TAG, String.valueOf(bundleSize) }, 
					{ PTS_TAG, pts },
					{ DEPENDS_TAG, String.valueOf(dependsOn) } }); 
		XMLUtils.wrapNode(Q_GROUP_TAG, qGrpAttrs, xmlBld);
		final String xml = xmlBld.toString();
		debugPrint(SELF, xml);
		return xml;
	} // toXML()

	/** Converts XML in an imported assignment to a list of question groups. 
	 * @param	node	information about the assignment
	 * @return	the parsed Assgt
	 */
	public static List<AssgtQGroup> parseXML(Node node) {
		final String SELF = "AssgtQGroup.parseXML: ";
		debugPrint(SELF + "parsing assignment's question groups.");
		final NodeList qGrpsNodes = node.getChildNodes();
		final List<AssgtQGroup> qGrps = new ArrayList<AssgtQGroup>();
		for (int nodeNum = 0; nodeNum < qGrpsNodes.getLength(); nodeNum++) {
			final Node qGrpsNode = qGrpsNodes.item(nodeNum);
			if (qGrpsNode.getNodeType() != Node.TEXT_NODE
					&& qGrpsNode.getNodeName().equalsIgnoreCase(Q_GROUP_TAG)) {
				final NamedNodeMap qGrpAttrs = qGrpsNode.getAttributes();
				final int pick = (qGrpAttrs.getNamedItem(PICK_TAG) == null ? 1
						: MathUtils.parseInt(
							qGrpAttrs.getNamedItem(PICK_TAG).getNodeValue()));
				final int bundleSize = 
						(qGrpAttrs.getNamedItem(BUNDLE_SIZE_TAG) == null ? 1
						: MathUtils.parseInt(qGrpAttrs.getNamedItem(
							BUNDLE_SIZE_TAG).getNodeValue()));
				final String pts = (qGrpAttrs.getNamedItem(PTS_TAG) == null 
						? "1" : qGrpAttrs.getNamedItem(PICK_TAG).getNodeValue());
				final int dependsOn = 
						(qGrpAttrs.getNamedItem(DEPENDS_TAG) == null ? 0
						: MathUtils.parseInt(
							qGrpAttrs.getNamedItem(DEPENDS_TAG).getNodeValue()));
				final NodeList qNodesList = qGrpsNode.getChildNodes();
				final List<Integer> qIds = new ArrayList<Integer>();
				for (int qNodeNum = 0; qNodeNum < qNodesList.getLength(); 
						qNodeNum++) {
					final Node qNode = qNodesList.item(qNodeNum);
					if (qNode.getNodeType() != Node.TEXT_NODE
							&& qNode.getNodeName().equalsIgnoreCase(Q_TAG)) {
						final NamedNodeMap qAttrs = qNode.getAttributes();
						if (qAttrs.getNamedItem(ID_TAG) != null) {
							qIds.add(Integer.decode(
									qAttrs.getNamedItem(ID_TAG).getNodeValue()));
						} // if id
					} // if qNode
				} // for each question node
				final AssgtQGroup qGrp = 
						new AssgtQGroup(qIds, pick, bundleSize, pts, dependsOn);
				debugPrint(SELF + "parsed question group: ", qGrp.toString());
				qGrps.add(qGrp);
			} // if qGrpsNode
		} // for each question group node
		return qGrps; 
	} // parseXML(Node)

	/** Gets whether another question group equals this one, i.e., has the same
	 * number of picks, bundle size, number of questions, and question ID
	 * numbers in the same order.
	 * @param	theOther	the other question group
	 * @return	true if the question group equals this one
	 */
	@Override
	public boolean equals(Object theOther) {
		boolean isEqual = false;
		if (theOther instanceof AssgtQGroup) {
			final AssgtQGroup qGrp = (AssgtQGroup) theOther;
			isEqual = getPick() == qGrp.getPick()
					&& getBundleSize() == qGrp.getBundleSize()
					&& getPts().equals(qGrp.getPts())
					&& getDependsOn() == qGrp.getDependsOn()
					&& getQIds().equals(qGrp.getQIds());
		} // if instanceof
		return isEqual;
	} // equals(Object)

	/** Creates a hash code describing this object.
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		final HashCodeBuilder codeBld = new HashCodeBuilder(17, 37);
		codeBld.append(getPick())
				.append(getBundleSize())
				.append(getPts())
				.append(getDependsOn())
				.append(getQIds().hashCode());
		return codeBld.toHashCode();
	} // hashCode()

	/** Returns a description of this question group.
	 * @return	a string describing this question group
	 */
	public String toString() {
		return Utils.toString("pick = ", pick,
				", bundle size = ", bundleSize,
				", points = ", pts,
				dependsOn == 0 ? ""
					: Utils.getBuilder(", depends on Q", dependsOn),
				", question IDs = ", Utils.join(getQIds()));
	} // toString()

} // AssgtQGroup
