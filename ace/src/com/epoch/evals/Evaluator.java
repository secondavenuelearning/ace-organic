package com.epoch.evals;

import com.epoch.constants.AppConstants;
import com.epoch.evals.evalConstants.EvalConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.ParserException;
import com.epoch.responses.Response;
import com.epoch.translations.TranslnsMap;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.XMLUtils;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** A generalized evaluator, with methods for submitting a response for
 * evaluation and combining subevaluators' results into a single result to pass
 * back to the front end.
 */
public class Evaluator implements AppConstants, EvalConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Question to which this evaluator belongs. */
	transient public int qId;
	/** Indicates 1-based position of the evaluator within the question.  
	 * Subevaluators of complex evaluators have the same majorID as the complex 
	 * evaluator itself. */
	transient public int majorId;
	/** Stores the original position of an evaluator that has been moved.
	 * Used by QuestionWrite in lieu of a unique ID for evaluators.  */
	transient public int oldMajorId;
	/** Feedback provided by ACE if this evaluator is satisfied. */
	transient public String feedback;
	/** ID number used to store translations of feedback. */
	transient public int phraseId;
	/** Grade awarded by ACE if this evaluator is satisfied. */
	transient public double grade;
	/** List of evaluators (one or more) that actually evaluate the response. */
	transient private List<Subevaluator> subevals = new ArrayList<Subevaluator>();
	/** How to combine subevaluators with AND and OR to make a complex
	 * evaluator.  Null if the evaluator is simple. */
	transient public String exprCode;
	/** For reporting exceptions, database actions, etc. */
	public String miscMessage = "";

	/** Constructor. */
	public Evaluator() { 
		// empty constructor
	}

	/** Copy constructor. Blindly copy all values.
	 * @param	copy	evaluator to copy
	 */
	public Evaluator(Evaluator copy) {
		qId = copy.qId;
		majorId = copy.majorId;
		oldMajorId = copy.oldMajorId;
		feedback = copy.feedback;
		phraseId = copy.phraseId;
		grade = copy.grade;
		exprCode = copy.exprCode;
		subevals = new ArrayList<Subevaluator>();
		for (final Subevaluator subeval : copy.subevals) {
			subevals.add(new Subevaluator(subeval));
		} // for each subevaluator
	} // Evaluator(Evaluator)

	/** Constructor called by Question.java when user creates a new evaluator.
	 * @param	input	evaluator data used to create the evaluator
	 */
	public Evaluator(Subevaluator input) {
		subevals.add(input);
	} // Evaluator(Subevaluator)

/* ****************** Get/set methods *********************/

	/** Convert the major ID of this evaluator to 0.  */
	public void clearIds() 							{ majorId = 0; }
	/** Determines whether this evaluator is complex (has subevaluators).
	 * @return	true if the evaluator is complex
	 */
	public boolean isComplex()						{ return subevals.size() > 1; }
	/** Gets the expression code of this evaluator in postfix format, 
	 * 1:2:3:@:4:|3.
	 * @return	the expression code
	 */
	public String getExpressionCode() 				{ return exprCode; }
	/** Gets the number of subevaluators.
	 * @return	the number of subevaluators
	 */
	public int getNumSubevaluators()				{ return subevals.size(); }
	/** Gets the subevaluators of this evaluator (1 or more).
	 * @return	list of the subevaluators of this evaluator
	 */
	public List<Subevaluator> getSubevaluators() 	{ return subevals; }

	/** Sets a default expression code for this evaluator.  */
	public void setDefaultExprCode() {
		final int numSubevals = getNumSubevaluators();
		exprCode = (numSubevals <= 1 ? ""
				: CombineExpr.getDefaultCode(numSubevals));
	} // setDefaultExprCode()

	/** Sets the expression code, converting from a nested format such as
	 * 1&amp;(2|3) to a postfix format such as 1:2:3:|:@.
	 * @param	nested	the expression code in a nested format, e.g.,
	 * 1&amp;(2|3)
	 */
	public void setNestedExprCode(String nested) {
		exprCode = CombineExpr.nestedToPostfix(nested);
	} // setNestedExprCode(String)

	/** Gets a subevaluator of this evaluator.
	 * @param	subevalNum	1-based number of the subevaluator
	 * @return	an Subevaluator representing the subevaluator
	 * @throws	ParameterException	if there is no subevaluator with that number
	 */
	public Subevaluator getSubevaluator(int subevalNum) throws ParameterException {
		checkInRange(subevalNum, "getSubevaluator");
		return subevals.get(subevalNum - 1);
	} // getSubevaluator(int)

	/** Populates the subevaluators of this evaluator.
	 * @param	newSubevals	list of subevaluators for this evaluator
	 */
	public void setSubevaluators(List<Subevaluator> newSubevals) {
		subevals = newSubevals;
	} // setSubevaluators(List<Subevaluator>)

	/** Sets the value of a subevaluator of this evaluator.
	 * @param	subevalNum	1-based number of the subevaluator
	 * @param	newSubeval	new value of subevaluator of this evaluator
	 * @throws	ParameterException	if there is no subevaluator with that number
	 */
	public void setSubevaluator(int subevalNum, Subevaluator newSubeval) 
			throws ParameterException {
		checkInRange(subevalNum, "setSubevaluator");
		subevals.set(subevalNum - 1, newSubeval);
	} // setSubevaluator(int, Subevaluator)

	/** Sets the value of a subevaluator of this evaluator.
	 * @param	newSubeval	new value of subevaluator of this evaluator
	 */
	public void addSubevaluator(Subevaluator newSubeval) {
		subevals.add(newSubeval);
	} // addSubevaluator(Subevaluator)

	/** Removes a subevaluator of this evaluator.
	 * @param	subevalNum	1-based number of the subevaluator
	 * @return	the removed subevaluator
	 * @throws	ParameterException	if there is no subevaluator with that number
	 */
	public Subevaluator removeSubevaluator(int subevalNum) throws ParameterException {
		checkInRange(subevalNum, "removeSubevaluator");
		return subevals.remove(subevalNum - 1);
	} // removeSubevaluator(int)

	/** Get the English description of this evaluator's subevaluators, and how
	 * to combine them if there are more than one.
	 * @param	qDataTexts	the text of question data of this question, if any
	 * @return	English descriptions of the subevaluators
	 */
	public String[] getDescription(String[] qDataTexts) {
		return getDescription(qDataTexts, majorId);
	} // getDescription(String[])

	/** Get the English description of this evaluator's subevaluators, and how
	 * to combine them if there are more than one.
	 * @param	qDataTexts	the text of question data of this question, if any
	 * @param	evalNum	number of this evaluator; may differ from majorId if
	 * question is being edited
	 * @return	English descriptions of the subevaluators
	 */
	public String[] getDescription(String[] qDataTexts, int evalNum) {
		final String SELF = "Evaluator.getDescription: ";
		final List<String> description = new ArrayList<String>();
		if (isComplex()) description.add(exprCodeToEnglish(evalNum));
		for (final Subevaluator subeval : subevals) {
			description.add(subeval.toEnglish(qDataTexts));
		} // for each subevaluator
		return description.toArray(new String[description.size()]);
	} // getDescription(String[], int)

	/** Converts the expression code into English.
	 * @return	an English expression
	 */
	public String exprCodeToEnglish() {
		return exprCodeToEnglish(majorId >= 0 ? majorId : -majorId);
	} // exprCodeToEnglish()

	/** Converts the expression code into English.
	 * @param	evalNum	number of this evaluator; may differ from majorId if
	 * question is being edited
	 * @return	an English expression
	 */
	public String exprCodeToEnglish(int evalNum) {
		return CombineExpr.postfixToEnglish(exprCode, evalNum);
	} // exprCodeToEnglish(int)

	/** Converts the postfix expression code into a nested expression code.
	 * @return	a nested expression code
	 */
	public String exprCodeToNested() {
		return CombineExpr.postfixToNested(exprCode);
	} // exprCodeToNested()

	/** Determines whether this evaluator calculates a grade based on the 
	 * response.  
	 * @return	true if the evaluator is simple and its sole subevaluator 
	 * calculates a grade based on the response and the default grade is not 1 
	 * @throws	ParameterException	if there is no first subevaluator
	 */
	public boolean calculatesGrade() throws ParameterException {
		return !isComplex() && grade < 1
				&& getSubevaluator(1).calculatesGrade();
	} // calculatesGrade()

	/** Determines whether human grading is ALWAYS required for this evaluator.
	 * Note that human grading may or may not be required if the human grading
	 * required evaluator is one of several subevaluators.
	 * @return	true if the evaluator is simple and its sole subevaluator 
	 * requires human intervention
	 * @throws	ParameterException	if there is no first subevaluator
	 */
	public boolean isHumanGradingReqd() throws ParameterException {
		return !isComplex() && getSubevaluator(1).isHumanGradingReqd();
	} // isHumanGradingReqd()

	/** Determines whether a response satisfies this evaluator.
	 * Called from Question.java.
	 * @param	response	student's parsed response
	 * @return	a OneEvalResult, which includes a boolean (true if the evaluator 
	 * is satisfied).  If the evaluator is satisfied, it may also contain a 
	 * modified response or automatic feedback.  If not, it may contain an error 
	 * message.  Complex evaluators will return automatic feedback if the last 
	 * subevaluator of an OR expression to be satisfied generated the feedback, 
	 * or if the first subevaluator of an AND expression generated it.
	 * @throws	ParameterException	if there is no first subevaluator
	 */
	public OneEvalResult matchResponse(Response response) 
			throws ParameterException {
		final String SELF = "Evaluator.matchResponse: ";
		OneEvalResult evalResult = null;
		if (Utils.isEmpty(exprCode)) {
			evalResult = getSubevaluator(1).isResponseMatching(response);
		} else {
			debugPrint(SELF + "evaluating ", exprCodeToEnglish());
			final CombineExpr combineExpr = new CombineExpr(this, response);
			evalResult = combineExpr.isSatisfied(exprCode);
		} // if expression is not empty
		return evalResult;
	} // matchResponse(Response)

	/** Determines whether a response satisfies one particular subevaluator.
	 * Called by CombineExpr.isSatisfied().
	 * @param	subevalNum	1-based number of the subevaluator
	 * @param	response	student's response
	 * @return	a OneEvalResult, which includes a boolean (true if the evaluator 
	 * is satisfied).  If the evaluator is satisfied, it may also contain a 
	 * modified response or automatic feedback.  If not, it may contain an error 
	 * message.  
	 */
	public OneEvalResult satisfiesRule(int subevalNum, Response response) {
		final String SELF = "Evaluator.satisfiesRule: ";
		OneEvalResult evalResult = null;
		try {
			final Subevaluator subeval = getSubevaluator(subevalNum);
			debugPrint(SELF + "evaluating subevaluator ", subevalNum, 
					": ", subeval.toEnglish());
			evalResult = subeval.isResponseMatching(response);
		} catch (ParameterException e) {
			Utils.alwaysPrint(SELF + "caught ParameterException on "
					+ "getting subevaluator ", subevalNum, 
					"; number of subevaluators = ", getNumSubevaluators());
			e.printStackTrace();
			evalResult = new OneEvalResult();
			evalResult.isSatisfied = false;
		} // try
		return evalResult;
	} // satisfiesRule(int, Response)

	/** Checks if the number is in range of the subevaluators. 
	 * @param	subevalNum	1-based number of the subevaluator
	 * @param	method	name of method calling this one
	 * @throws	ParameterException	if there is no subevaluator with that number
	 */
	private void checkInRange(int subevalNum, String method) throws ParameterException {
		if (!MathUtils.inRange(subevalNum, new int[] {1, subevals.size()})) {
			throw new ParameterException("Evaluator." + method 
					+ ": no subevaluator with index " + subevalNum);
		} // if out of range
	} // checkInRange(int, String)

/* ****************** XML methods *********************/

	/** Converts this evaluator to XML for export.
	 * @param	feedbackTranslns	translations of the feedback in all
	 * languages
	 * @return	XML for export
	 */
	public String toXML(TranslnsMap feedbackTranslns) {
		final StringBuilder elementContents = new StringBuilder();
		if (isComplex()) {
		   	elementContents.append(makeNode(EXPRESSION_TAG, getExpressionCode()));
		} // if is complex
		int minorId = 0;
		for (final Subevaluator subeval : subevals) {
			Utils.appendTo(elementContents, subeval.toXML(++minorId), '\n');
		} // for each subevaluator
		final StringBuilder fbContent = new StringBuilder();
		if (!Utils.isEmptyOrWhitespace(feedback)) {
			fbContent.append(makeNode(FEEDBACK_TAG, feedback));
		}
		if (feedbackTranslns != null) {
			final List<String> languages = feedbackTranslns.getLanguages();
			for (final String language : languages) {
				if (!ENGLISH.equals(language)) {
					fbContent.append(makeTranslnNode(language, 
							feedbackTranslns.get(language)));
				} // if language is not English (shouldn't be)
			} // for each translated feedback
		} // if there are translated feedbacks
		Utils.appendTo(elementContents, fbContent, makeNode(GRADE_TAG, grade));
		return Utils.toString(startEval(), elementContents, endEval());
	} // toXML(String[][])

	/** Wraps an evaluator tag with &lt; &gt; and adds attributes.
	 * @return	the tag wrapped in &lt; &gt; with attributes
	 */
	private StringBuilder startEval() {
		final String gradeStr = (grade == 0 ? WRONG_ATTR
				: grade == 1 ? CORRECT_ATTR : PARTIAL_ATTR);
		return XMLUtils.startTag(EVALUATOR_TAG, new String[][] {
					{EVAL_TYPE_TAG, gradeStr},
					{EVAL_ID_TAG, String.valueOf(majorId)}
				});
	} // startEval()

	/** Surround the given text with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	text	the text of the node
	 * @return	the text surrounded by XML tags
	 */
	private StringBuilder makeNode(String tag, String text) {
		return XMLUtils.makeNode(tag, text, NEWLINE);
	} // makeNode(String, String)

	/** Surround the given text with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	num	the number of the node
	 * @return	the text surrounded by XML tags
	 */
	private StringBuilder makeNode(String tag, double num) {
		return XMLUtils.makeNode(tag, num);
	} // makeNode(String, double)

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
	private StringBuilder endEval() {
		return XMLUtils.endTag(EVALUATOR_TAG);
	} // endEval()

	/** Converts imported XML to an evaluator.
	 * @param	node	the XML to parse
	 * @return	an evaluator
	 * @throws	ParserException	if the XML can't be parsed
	 */
	public static Evaluator parseXML(Node node) throws ParserException {
		final String SELF = "Evaluator.parseXML: ";
		final Evaluator eval = new Evaluator();
		debugPrint(SELF + "reading the attribute values.");
		String correctnessAttrVal = null;
		final NamedNodeMap nodeMap = node.getAttributes();
		eval.majorId = (nodeMap.getNamedItem("id") == null ? 0
				: MathUtils.parseInt(nodeMap.getNamedItem("id").getNodeValue()));
		if (nodeMap.getNamedItem("type") != null) {
			correctnessAttrVal = nodeMap.getNamedItem("type").getNodeValue();
		}
		debugPrint(SELF + "looking for Subevaluator info stored in this node.");
		final Subevaluator simpleEval = Subevaluator.parseXML(node);
		if (!Utils.isEmpty(simpleEval.getMatchCode())) {
			debugPrint(SELF + "found Subevaluator info stored in this node; "
					+ "must be a simple evaluator.");
			eval.addSubevaluator(simpleEval);
		} else debugPrint(SELF + "no subevaluator info found; "
				+ "must be a complex evaluator.");
		final NodeList childrenGen1 = node.getChildNodes();
		debugPrint(SELF, childrenGen1 == null ? "childrenGen1 is null."
				: "reading the contents of the child nodes.");
		final int numChildrenGen1 = 
				(childrenGen1 == null ? 0 : childrenGen1.getLength());
		for (int childGen1Num = 0; 
				childGen1Num < numChildrenGen1; childGen1Num++) {
			final Node childGen1 = childrenGen1.item(childGen1Num);
			// ignore Jlint complaint about line above.  Raphael 11/2010
			if (childGen1 == null) {
				debugPrint(SELF + "generation 1 child node ", childGen1Num + 1, 
						" is null.");
				continue;
			}
			if (childGen1.getNodeType() == Node.TEXT_NODE) continue;
			final String childGen1Name = childGen1.getNodeName();
			final Node firstChildGen2 = childGen1.getFirstChild();
			if (firstChildGen2 == null) {
				debugPrint(SELF + "child node ", childGen1Num + 1, 
						" with name ", childGen1Name, 
						" has firstChildGen2 with null value.");
				continue;
			} else debugPrint(SELF + "node ", childGen1Num + 1, " has name ", 
					childGen1Name, " with value ", 
					firstChildGen2.getNodeValue(), ".");
			if (childGen1Name.equalsIgnoreCase(EVALUATOR_TAG)) {
				debugPrint(SELF + "found evaluator tag: "
						+ "parsing firstChildGen2 node ", 
						childGen1Num + 1, " as subevaluator.");
				eval.addSubevaluator(Subevaluator.parseXML(childGen1));
			} else if (childGen1Name.equalsIgnoreCase(EXPRESSION_TAG)) {
				eval.exprCode = firstChildGen2.getNodeValue().trim();
				debugPrint(SELF + "found expression ", eval.exprCode);
			} else if (childGen1Name.equalsIgnoreCase(FEEDBACK_TAG)) {
				eval.feedback = firstChildGen2.getNodeValue().trim();
				debugPrint(SELF + "found feedback ", eval.feedback);
			} else if (childGen1Name.equalsIgnoreCase(GRADE_TAG)) {
				try {
					eval.grade = 
							Double.parseDouble(firstChildGen2.getNodeValue());
				} catch (NumberFormatException e) {
					final boolean correct =
							CORRECT_ATTR.equalsIgnoreCase(correctnessAttrVal); 
					eval.grade = (correct ? 1 : 0);
				} // try
				debugPrint(SELF + "found grade ", eval.grade);
			} // if name of generation 1 child
		} // each element of childrenGen1
		debugPrint(SELF + "formed the evaluator.");
		return eval;
	} // parseXML()

	/** Extracts feedback translations from XML.
	 * @param	node	information about the evaluator
	 * @return	map of feedback translations by language
	 */
	public static TranslnsMap getAllTranslns(Node node) {
		final TranslnsMap translns = new TranslnsMap();
		final NodeList childrenGen1 = node.getChildNodes();
		for (int childGen1Num = 0; 
				childGen1Num < childrenGen1.getLength(); childGen1Num++) {
			final Node childGen1 = childrenGen1.item(childGen1Num);
			if (childGen1.getNodeName().equalsIgnoreCase(TRANSLATION_TAG)) {
				final Node langNode = 
						childGen1.getAttributes().getNamedItem(LANGUAGE_TAG);
				if (langNode != null) {
					final String language = langNode.getNodeValue();
					final Node firstChildGen2 = childGen1.getFirstChild();
					final String transln = (firstChildGen2 == null ? "" 
							: firstChildGen2.getNodeValue().trim());
					if (language != null && !ENGLISH.equals(language)
							&& !Utils.isEmpty(transln)) {
						translns.put(language, transln);
					} // if we have a good translation
				} // if a language is specified
			} // if the current node is a translation
		} // for node index
		return translns;
	} // getAllTranslns(Node)

	/** Gets the XML tag for evaluators.
	 * @return	XML tag for evaluators
	 */
	public static String getTag() {
		return EVALUATOR_TAG;
	} // getTag()

	/** Returns human-readable description of this evaluator.  For debugging.
	 * @return	human-readable description of this evaluator
	 */
	public String toString() {
		final StringBuilder result = Utils.getBuilder("qId = ", qId, '\n', 
				"majorId = ", majorId, '\n', "grade = ", grade, '\n', 
				"feedback = ", feedback, '\n');
		if (isComplex()) {
			Utils.appendTo(result, "exprCode = ", exprCode, '\n');
		} // if complex
		int subevalNum = 0;
		for (final Subevaluator subeval : subevals) {
			if (++subevalNum > 1) result.append('\n');
			Utils.appendTo(result, "subevaluator ", subevalNum, ":\n", 
					subeval.toString("\t"));
		} // for each subevaluator
		return result.toString();
	} // toString()

} // Evaluator
