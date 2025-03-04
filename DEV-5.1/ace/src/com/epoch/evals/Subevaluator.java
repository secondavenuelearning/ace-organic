package com.epoch.evals;

import com.epoch.evals.evalConstants.EvalConstants;
import com.epoch.evals.impl.chemEvals.Atoms;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.ParserException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.XMLUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Contains the information needed to instantiate an evaluator implementation
 * that actually does the work of evaluating a response.
 */
public class Subevaluator implements EvalConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Code representing the evaluator. */
	transient public String matchCode; 
	/** External coded form of data required by the evaluator. */ 
	transient public String codedData; 
	/** String representation of the molecule associated with this evaluator (if
	 * there is one). */
	transient public String molStruct;
	/** Name of the molecule associated with this evaluator (if
	 * there is one). */
	transient public String molName;
	/** Implementation of the evaluator's specific class, one of &gt; 50, that is used
	 * to evaluate a response. */
	transient private EvalInterface evalImpl;
	/** Whether the evaluator's implementation has been initiated. */
	transient private boolean evalImplInitd = false;

	/** Constructor. */
	public Subevaluator() {
		// empty
	} // Subevaluator()

	/** Copy constructor. 
	 * @param	ed	evaluatordata to be copied
	 */
	public Subevaluator(Subevaluator ed) {
		matchCode = ed.matchCode;
		codedData = ed.codedData;
		molStruct = ed.molStruct;
		molName = ed.molName;
	} // Subevaluator(Subevaluator)

	/** Gets the name of the molecule associated with this evaluator.
	 * @return	name of the molecule associated with this evaluator
	 */
	public String getMolName()		{ return molName; }
	/** Gets the String representation (most likely MRV) of the molecule associated
	 * with this evaluator.
	 * @return	structure of the molecule associated with this evaluator
	 */
	public String getMolStruct()	{ return molStruct; }
	/** Gets the coded data associated with this evaluator.  Coded data is
	 * a slash-separated series of short strings and numbers, such as "Y&gt;/5".
	 * The meaning of the data is specific to each type of evaluator.  Not every
	 * evaluator has coded data, although most do.
	 * @return	the coded data
	 */
	public String getCodedData() 	{ return codedData; }
	/** Gets the match code for this evaluator.  The match code is a short
	 * String such as MUL_CHK that is used to identify the type of evaluator.
	 * @return	the match code
	 */
	public String getMatchCode() 	{ return matchCode; }
	/** Gets the actual evaluator implementation.
	 * @return	the actual evaluator implementation
	 */
	public EvalInterface getEvalImpl()	{ return evalImpl; }

	/** Loads the implementation of this evaluator. */
	public void setEvaluatorImpl() {
		final String SELF = "Subevaluator.setEvaluatorImpl: ";
		try {
			evalImpl = EvalManager.loadEvaluatorImpl(matchCode, 
					codedData, molName == null ? "" : molName);
		} catch (ParameterException e) {
			Utils.alwaysPrint(SELF + "could not load evalImpl.");
			e.printStackTrace();
		} // try
		evalImplInitd = true;
		if (evalImpl == null)
			debugPrint(SELF + "tried to load evalImpl but it is null.");
	} // setEvaluatorImpl()

	/** Sets the evaluator interface for this evaluator. 
	 * @param	evalInterface	the evaluator interface
	 */
	public void setEvaluatorImpl(EvalInterface evalInterface) {
		evalImpl = evalInterface;
		evalImplInitd = true;
	} // setEvaluatorImpl(EvalInterface)

	/** Determines whether this evaluator's implementation is null.
	 * May happen if class can't be found, but shouldn't happen.
	 * @return	true if the evaluator's implementation is null
	 */
	public boolean evalImplIsNull()	{ 
		if (!evalImplInitd) setEvaluatorImpl();
		return evalImpl == null; 
	} // evalImplIsNull()

	/** Gets whether a response satisfies this evaluator.
	 * @param	response	student's parsed response
	 * @return	a OneEvalResult, which includes a boolean (true if the
	 * evaluator is satisfied).  If the evaluator is satisfied, it may also
	 * contain a modified response or automatic feedback.  If not, 
	 * it may contain an error message.
	 */
	public OneEvalResult isResponseMatching(Response response) {
		final String SELF = "Subevaluator.isResponseMatching: ";
		if (!evalImplInitd) setEvaluatorImpl();
		debugPrint(SELF + "evaluating ", matchCode, 
				" with codedData ", codedData);
		final OneEvalResult evalResult = (evalImpl == null ? new OneEvalResult()
				: evalImpl.isResponseMatching(response, molStruct));
		debugPrint(SELF + "evaluation successful.");
		return evalResult;
	} // isResponseMatching(Response)

	/** Gets the English description of this evaluator.
	 * @return	the English description
	 */
	public String toEnglish() {
		return toEnglish(new String[0], false);
	} // toEnglish()

	/** Gets the English description of this evaluator.
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question
	 * @return	the English description
	 */
	public String toEnglish(boolean forPermissibleSM) {
		return toEnglish(new String[0], forPermissibleSM);
	} // toEnglish(boolean)

	/** Gets the English description of this evaluator.
	 * @param	qDataTexts	the text of question data of this question, if any
	 * @return	the English description
	 */
	public String toEnglish(String[] qDataTexts) {
		return toEnglish(qDataTexts, false);
	} // toEnglish(String[])

	/** Gets the English description of this evaluator.
	 * @param	qDataTexts	the text of question data of this question, if any
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question
	 * @return	the English description
	 */
	public String toEnglish(String[] qDataTexts, boolean forPermissibleSM) {
		if (!evalImplInitd) setEvaluatorImpl();
		return (evalImpl == null ? "Evaluator is null." 
				: evalImpl.toEnglish(qDataTexts, forPermissibleSM));
	} // toEnglish(String[], boolean)

	/** Gets the internal constant corresponding to this evaluator's match code.
	 * @return	the internal constant corresponding to the match code
	 */
	public int getEvalType() {
		return EvalManager.getEvalType(matchCode);
	} // getEvalType()

	/** Determines whether this evaluator gives autofeedback.
	 * @return	true if this evaluator gives autofeedback
	 */
	public boolean givesAutoFeedback() {
		return Utils.contains(AUTOFEEDBACK_EVALS, matchCode);
	} // givesAutoFeedback()

	/** Determines whether this evaluator subtracts the properties of
	 * embedded instantiated R groups when calculating the properties of a
	 * molecule.
	 * @return	true if the evaluator excludes R groups
	 */
	public boolean excludesRGroups() {
		return Utils.contains(EXCLUDE_R_GROUPS, matchCode);
	} // excludesRGroups()

	/** Gets text describing how this evaluator handles R groups when
	 * calculating the properties of a molecule.
	 * @return	text describing how this evaluator handles R groups
	 */
	public String getRGroupsExcludedText() {
		final int matchConstant = Utils.indexOf(EVAL_CODES, matchCode);
		return (!excludesRGroups() ? ""
				: Utils.among(matchConstant, HAS_FORMULA, NUM_ATOMS) 
					? " (R groups' atoms excluded)" 
					: " (R groups excluded)");
	} // getRGroupsExcludedText()

	/** Determines whether this evaluator uses permissible starting materials as
	 * defined by the question data.
	 * @return	true if the evaluator uses permissible starting materials
	 */
	public boolean usesPermissibleSMs() {
		return Utils.contains(USE_PERMISSIBLE_SMS, matchCode);
	} // usesPermissibleSMs()

	/** Determines whether this evaluator requires at least one question image.
	 * @return	true if the evaluator requires at least one question image
	 */
	public boolean requiresImage() {
		return Utils.contains(NEEDS_IMAGE, matchCode);
	} // requiresImage()

	/** Determines whether an evaluator requires at least one question image.
	 * @param	matchConstant	constant corresponding to an evaluator
	 * @return	true if the evaluator requires at least one question image
	 */
	public static boolean requiresImage(int matchConstant) {
		return Utils.contains(NEEDS_IMAGE, EVAL_CODES[matchConstant]);
	} // requiresImage(int)

	/** Determines whether this evaluator calculates a grade based on the 
	 * response.  
	 * @return	true if the evaluator calculates a grade based on the response 
	 */
	public boolean calculatesGrade() {
		if (!evalImplInitd) setEvaluatorImpl();
		return evalImpl != null && Utils.contains(CALC_GRADE_EVALS, matchCode) 
				&& evalImpl.getCalcGrade();
	} // calculatesGrade()

	/** Determines whether human grading is required for this evaluator.
	 * @return	true if the evaluator requires human grading 
	 */
	public boolean isHumanGradingReqd() {
		return getEvalType() == HUMAN_REQD;
	} // isHumanGradingReqd()

	/** Converts this subevaluator to XML for export.
	 * @param	minorId	the ID of the subevaluator
	 * @return	XML for export
	 */
	public String toXML(int minorId) {
		final String matchCode = getMatchCode();
		final String codedData = getCodedData();
		final StringBuilder subEvalBld = Utils.getBuilder(
				startSubeval(minorId), makeNode(IF_TAG, matchCode),
				XMLUtils.comment(toEnglish()), '\n');
		if (!Utils.isEmptyOrWhitespace(molStruct)) {
			Utils.appendTo(subEvalBld, startTag(MOL_TAG),
					makeNode(MOLNAME_TAG, molName), makeMolNode(molStruct),
					endTag(MOL_TAG));
		} // if there's a molecule
		if (!Utils.isEmpty(codedData)) {
			subEvalBld.append(makeNode(CODEDDATA_TAG, codedData, !NEWLINE));
		} // if there is coded data
		return Utils.toString(subEvalBld, endSubeval());
	} // toXML()

	/** Wraps an evaluator tag with &lt; &gt; and adds attributes.
	 * @param	minorId	the ID of the subevaluator
	 * @return	the tag wrapped in &lt; &gt; with attributes
	 */
	private StringBuilder startSubeval(int minorId) {
		return XMLUtils.startTag(EVALUATOR_TAG, new String[][] {
					{EVAL_ID_TAG, String.valueOf(minorId)}
				});
	} // startSubeval(int)

	/** Wraps a tag with &lt; &gt;.
	 * @param	tag	the name of the XML tag
	 * @return	the tag wrapped in &lt; &gt;
	 */
	private StringBuilder startTag(String tag) {
		return XMLUtils.startTag(tag);
	} // startTag(String)

	/** Surround the given text with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	text	the text of the node
	 * @return	the text surrounded by XML tags
	 */
	private StringBuilder makeNode(String tag, String text) {
		return makeNode(tag, text, NEWLINE);
	} // makeNode(String, String)

	/** Surround the given text with the given XML tags.
	 * @param	tag	the name of the XML tag
	 * @param	text	the text of the node
	 * @param	newLine	whether to add a new line
	 * @return	the text surrounded by XML tags
	 */
	private StringBuilder makeNode(String tag, String text, 
			boolean newLine) {
		return XMLUtils.makeNode(tag, text, newLine);
	} // makeNode(String, String, boolean)

	/** Surround the given molecule description with the given XML tags.
	 * @param	text	the text of the node
	 * @return	the text surrounded by XML tags
	 */
	private StringBuilder makeMolNode(String text) {
		return XMLUtils.makeNode(MOLSTRUCT_TAG, text, !NEWLINE);
	} // makeNode(String)

	/** Wraps a tag with &lt;/ &gt;.
	 * @param	tag	the name of the XML tag
	 * @return	the tag wrapped in &lt;/ &gt;
	 */
	private StringBuilder endTag(String tag) {
		return XMLUtils.endTag(tag);
	} // endTag(String)

	/** Wraps a tag with &lt;/ &gt;.
	 * @return	the tag wrapped in &lt;/ &gt;
	 */
	private StringBuilder endSubeval() {
		return endTag(EVALUATOR_TAG);
	} // endSubeval()

	/** Converts imported XML to a subevaluator.
	 * @param	node	the XML to parse
	 * @return	an evaluator
	 * @throws	ParserException	if the XML can't be parsed
	 */
	static Subevaluator parseXML(Node node) throws ParserException {
		final String SELF = "Subevaluator.parseXML: ";
		final Subevaluator subeval = new Subevaluator();
		debugPrint(SELF + "looking at parent node with name ", node.getNodeName());
		final NodeList childrenGen1 = node.getChildNodes();
		final int numChildrenGen1 = 
				(childrenGen1 == null ? 0 : childrenGen1.getLength());
		debugPrint(SELF, childrenGen1 == null ? "childrenGen1 is null."
				: "reading the contents of the " + numChildrenGen1 
					+ " generation 1 node(s).");
		String codedDataContent = null;
		for (int childGen1Num = 0; childGen1Num < numChildrenGen1; childGen1Num++) {
			final Node childGen1 = childrenGen1.item(childGen1Num);
			// ignore Jlint complaint about line above.  Raphael 11/2010
			if (childGen1 == null) {
				debugPrint(SELF + "generation 1 child ", childGen1Num + 1, 
						" is null.");
				continue;
			}
			if (childGen1.getNodeType() == Node.TEXT_NODE) continue;
			final String childGen1Name = childGen1.getNodeName();
			debugPrint(SELF + "generation 1 child node ", childGen1Num + 1, 
					" has name ", childGen1Name);
			final Node firstChildGen2 = childGen1.getFirstChild();
			if (childGen1Name.equalsIgnoreCase(IF_TAG)) {
				// removeFromEnd() is for pre-ACE 3.1 questions
				subeval.matchCode =  Utils.removeFromEnd(
						firstChildGen2.getNodeValue().trim(), "_Y");
				debugPrint(SELF + "got subevaluator match code ", 
						subeval.matchCode);
			} else if (childGen1Name.equalsIgnoreCase(CODEDDATA_TAG)) {
				codedDataContent = firstChildGen2.getNodeValue();
				debugPrint(SELF + "got subevaluator coded data ", 
						codedDataContent);
			} else if (childGen1Name.equalsIgnoreCase(MOL_TAG)) {
				debugPrint(SELF + "reading molstruct.");
				final NodeList childrenGen2 = childGen1.getChildNodes();
				if (childrenGen2 == null)
					debugPrint(SELF + "subnode List is null.");
				final int numChildrenGen2 = 
						(childrenGen2 == null ? 0 : childrenGen2.getLength());
				for (int childGen2Num = 0; 
						childGen2Num < numChildrenGen2; 
						childGen2Num++) {
					final Node childGen2 = childrenGen2.item(childGen2Num);
					// ignore Jlint complaint about line above.  Raphael 11/2010
					if (childGen2 == null) {
						debugPrint(SELF + "generation 2 child ", childGen2Num + 1, 
								" is null.");
						continue;
					}
					if (childGen2.getNodeType() == Node.TEXT_NODE) continue;
					final String childGen2Name = childGen2.getNodeName();
					final Node firstChildGen3 = childGen2.getFirstChild();
					if (firstChildGen3 == null) {
						debugPrint(SELF + "generation 2 child ", childGen2Num + 1, 
								" with name ", childGen2Name, " has null value.");
						continue;
					}
					debugPrint(SELF + "generation 2 child ", childGen2Num + 1, 
							" has name ", childGen2Name, " and value ", 
							firstChildGen3.getNodeValue(), ".");
					if (childGen2Name.equalsIgnoreCase(MOLNAME_TAG)) {
						subeval.molName = firstChildGen3.getNodeValue().trim();
					} else if (childGen2Name.equalsIgnoreCase(MOLSTRUCT_TAG)) {
						subeval.molStruct = firstChildGen3.getNodeValue(); // do not trim!
						debugPrint(SELF + "molstruct:\n", subeval.molStruct);
					} else if (childGen2Name.equalsIgnoreCase(CODEDDATA_TAG)) {
						codedDataContent = firstChildGen3.getNodeValue();
					}
				} // each generation 2 child 
			} // if name of generation 1 child 
		} // for each generation 1 child
		// updateCodedData() is for pre-ACE 3.1 questions
		subeval.codedData = updateCodedData(codedDataContent, subeval.matchCode);
		return subeval;
	} // parseXML(Node)

	/** Converts old-style coded data to new format.  Useful for importing old
	 * questions into ACE 3.1.
	 * <br><code>Atoms</code>: add flag for contiguity of counted atoms
	 * <br><code>Contains</code>: add flag for exact, default, or ignoring of
	 * charges, radicals, and isotopes; exact for substructures, 
	 * ignore for skeletons
	 * <br><code>FnalGroup</code>: convert isPos/groupId to groupId/=&lt;&gt;number/number
	 * <br><code>HasFormula</code>: convert isPos to =&lt;&gt;/number/countEach
	 * <br><code>MapProperty</code>: change 1|2 and true|false to Y|N, add patternOnly flags
	 * <br><code>MechRule</code>: convert from 3 to 4 data by appending integer of last
	 * (double) flag
	 * <br><code>MechFlowsValid</code>: convert leniency flag from Y|N to integer so can
	 * handle both resonance and double bond stereochemistry
	 * <br><code>MechCounter</code>: convert =&lt;&gt; number to symbol, add decrement
	 * <br><code>MechProdStartIs</code>: convert leniency flag from Y|N to integer so can
	 * handle both resonance and double bond stereochemistry
	 * <br><code>MechSubstructure</code>: change true|false to Y|N, add flag to
	 * ignore or consider charge, radicals, isotopes
	 * <br><code>MultipleCheck</code>: convert old 8-member operator to isPos and
	 * 4-member operator
	 * <br><code>MultipleNumChosen</code>: convert true|false to Y|N, convert old
	 * (nonstandard) numerical =&lt;&gt; operators to symbols
	 * <br><code>NumMols</code>: add flags for whether to count distinct molecules
	 * and what to consider when deciding whether they are distinct
	 * <br><code>Rings</code>: convert ringsOper/numRings to
	 * countEach/ringsOper/numRings/molsOper/numMols
	 * <br><code>SynthOneRxn</code>: add flag to look for whole cpds or substructures
	 * <br><code>SynthSteps</code>: convert numerical =&lt;&gt; operators to symbols, add
	 * decrement
	 * <br><code>Weight</code>: convert numerical =&lt;&gt; operators to symbols, convert
	 * last flag from number to word
	 * @param	oldCodedData	coded data in old format
	 * @param	matchCode	match code of evaluator
	 * @return	coded data in new format
	 * @see	com.epoch.evals.impl.chemEvals.Atoms
	 * @see	com.epoch.evals.impl.chemEvals.Contains
	 * @see	com.epoch.evals.impl.chemEvals.FnalGroup
	 * @see	com.epoch.evals.impl.chemEvals.HasFormula
	 * @see	com.epoch.evals.impl.chemEvals.MapProperty
	 * @see	com.epoch.evals.impl.chemEvals.NumMols
	 * @see	com.epoch.evals.impl.chemEvals.Rings
	 * @see	com.epoch.evals.impl.chemEvals.mechEvals.MechRule
	 * @see	com.epoch.evals.impl.chemEvals.mechEvals.MechFlowsValid
	 * @see	com.epoch.evals.impl.chemEvals.mechEvals.MechCounter
	 * @see	com.epoch.evals.impl.chemEvals.mechEvals.MechProdStartIs
	 * @see	com.epoch.evals.impl.chemEvals.mechEvals.MechSubstructure
	 * @see	com.epoch.evals.impl.chemEvals.synthEvals.SynthOneRxn
	 * @see	com.epoch.evals.impl.chemEvals.synthEvals.SynthSteps
	 * @see	com.epoch.evals.impl.chemEvals.Weight
	 * @see	com.epoch.evals.impl.genericQEvals.multEvals.MultipleCheck
	 * @see	com.epoch.evals.impl.genericQEvals.multEvals.MultipleNumChosen
	 */
	private static String updateCodedData(String oldCodedData, String matchCode) {
		if (oldCodedData == null || matchCode == null) return oldCodedData;
		final int codeNum = Utils.indexOf(EvalManager.EVAL_CODES, matchCode);
		StringBuilder newCodedData = Utils.getBuilder(oldCodedData);
		final String[] parts = oldCodedData.split("/");
		final int numParts = parts.length;
		// final StringBuilder bld;
		char first;
		int symbolNum;
		int oldCode;
		switch (codeNum) {
			case NUM_ATOMS:
				// Atoms: if 3 tokens, add flag for contiguity of counted atoms
				if (numParts < 4) newCodedData.append("/N");
				break;
			case SKELETON_SUBSTRUCTURE:
				// Contains: if 2 tokens, add flag for exact, default, or 
				// ignoring of charges, radicals, and isotopes; 
				// exact for substructures, ignore for skeletons
				if (numParts < 3) {
					newCodedData.append(oldCodedData.endsWith("/1") ? "/0" : "/2");
				} 
				break;
			case FUNCTIONAL_GROUP:
				// FnalGroup: convert isPos/groupId to groupId/YN=<>/number,
				// convert numerical =<> operators to YN=<>
				if (numParts < 3) {
					newCodedData = Utils.getBuilder(parts[1], '/',
							oldCodedData.startsWith("false") ? 'Y' : 'N', 
							"=/0");
					// parts = newCodedData.toString().split("/");
				} else {
					symbolNum = MathUtils.parseInt(parts[1], -1);
					if (symbolNum >= 0) {
						parts[1] = Atoms.SYMBOLS[symbolNum];
						newCodedData = Utils.getBuilder(Utils.join(parts, "/"));
					}
				}
				break;
			case HAS_FORMULA:
				// HasFormula: convert isPos/formula to YN=<>/number/countEach/formula
				if (numParts < 4) {
					first = parts[0].charAt(0);
					newCodedData = Utils.getBuilder('Y',
							Utils.among(first, 'f', 'N') ? '=' : '>',
							"/0/N/", parts[1]);
				}
				break;
			case MAPPED_ATOMS:
				// MapProperty: change 1|2 and true|false to Y|N, add patternOnly 
				// and checkEnant flags
				first = parts[0].charAt(0);
				if (Utils.among(first, 't', '1')) parts[0] = "Y";
				else if (Utils.among(first, 'f', '2')) parts[0] = "N";
				newCodedData = Utils.getBuilder(Utils.join(parts, "/"));
				if (numParts == 2) newCodedData.append("/N/N");
				break;
			case MECH_RULE:
				// MechRule: convert from 3 to 4 data by appending integer of last
				// (double) flag
				if (numParts == 3) {
					newCodedData = Utils.getBuilder(
							oldCodedData, '/', parts[2].split("\\.")[0]);
				}
				break;
			case MECH_FLOWS:
				// MechFlowsValid: convert leniency flag from Y|N to integer so can
				// handle both resonance and double bond stereochemistry
				first = parts[1].charAt(0);
				if (first == 'N') parts[1] = "0";
				else if (first == 'Y') parts[1] = "1";
				newCodedData = Utils.getBuilder(Utils.join(parts, "/"));
				break;
			case MECH_PRODS_STARTERS_IS:
				// MechProdStartIs: convert leniency flag from Y|N to integer so can
				// handle both resonance and double bond stereochemistry
				first = parts[2].charAt(0);
				if (first == 'N') parts[2] = "0";
				else if (first == 'Y') parts[2] = "1";
				newCodedData = Utils.getBuilder(Utils.join(parts, "/"));
				break;
			case MECH_SUBSTRUCTURE:
				// MechSubstructure: change true|false to Y|N, add flag to
				// ignore or consider charge, radicals, isotopes
				first = parts[0].charAt(0);
				if (first == 't') parts[0] = "Y";
				else if (first == 'f') parts[0] = "N";
				newCodedData = Utils.getBuilder(Utils.join(parts, "/"));
				if (numParts < 3) newCodedData.append("/7");
				break;
			case MECH_PIECES_COUNT:
			case SYNTH_STEPS:
				// MechCounter: convert numerical =<> operators to YN=<>, add decrement
				// SynthSteps: convert numerical =<> operators to YN=<>, add
				// decrement
				symbolNum = MathUtils.parseInt(parts[1], -1);
				if (symbolNum >= 0) {
					parts[1] = Atoms.SYMBOLS[symbolNum];
					newCodedData = Utils.getBuilder(Utils.join(parts, "/"));
				}
				if (numParts < 4) newCodedData.append("/0.0");
				break;
			case SYNTH_ONE_RXN:
				// SynthOneRxn: add flag to look for whole cpds or substructures
				if (numParts < 2) newCodedData.append("/is");
				break;
			case CHOICE_WHICH_CHECKED:
				// MultipleCheck: convert old 8-member operator (1st of two
				// tokens) to isPos and 4-member operator (1st and 2nd of 3
				// tokens)
				if (numParts == 3) {
					first = parts[0].charAt(0);
					if (first == 't') parts[0] = "Y";
					else if (first == 'f') parts[0] = "N";
				} else if (numParts == 2) {
					oldCode = MathUtils.parseInt(parts[0]);
					final boolean even = oldCode % 2 == 0;
					parts[0] = Utils.toString(even ? 'N' : 'Y', '/', 
							(oldCode + (even ? 0 : 1)) / 2);
				}
				newCodedData = Utils.getBuilder(Utils.join(parts, "/"));
				break;
			case CHOICE_NUM_CHECKED:
				// MultipleNumChosen: convert true|false to Y|N, convert old
				// (nonstandard) numerical =<> operators to YN=<>
				first = parts[0].charAt(0);
				if (first == 't') parts[0] = "Y";
				else if (first == 'f') parts[0] = "N";
				oldCode = MathUtils.parseInt(parts[1], -1);
				if (oldCode >= 1) {
					final int mod3 = oldCode % 3;
					parts[1] = Utils.toString(oldCode <= 3 ? 'Y' : 'N', 
							mod3 == 1 ? '=' : mod3 == 2 ? '<' : '>');
				}
				newCodedData = Utils.getBuilder(Utils.join(parts, "/"));
				break;
			case NUM_MOLECULES:
				// NumMols: add flags for whether to count distinct molecules
				// and what to consider when deciding whether they are distinct
				if (numParts < 3) newCodedData.append("/N");
				if (numParts < 4) newCodedData.append("/0");
				break;
			case NUM_RINGS:
				// Rings: convert ringsOper/numRings to
				// countEach/ringsOper/numRings/molsOper/numMols
				if (numParts < 5) {
					newCodedData = Utils.getBuilder("N/", oldCodedData, "/N=/0");
				}
				break;
			case TABLE_DIFF:
				// TableDiff: add parameter for highlighting wrong answers
				newCodedData.append("/Y");
				break;
			case WEIGHT:
				// Weight: convert numerical =<> operators to YN=<>, convert
				// last flag from number to word
				symbolNum = MathUtils.parseInt(parts[0], -1);
				if (symbolNum >= 0) parts[0] = Atoms.SYMBOLS[symbolNum];
				if (numParts == 2) {
					symbolNum = MathUtils.parseInt(parts[1], -1);
					if (symbolNum >= 0) {
						parts[1] = (symbolNum == 0 ? "exact" : "average");
					}
					newCodedData = Utils.getBuilder(Utils.join(parts, "/"));
				} else {
					newCodedData = Utils.getBuilder(parts[0], "/average");
				}
				break;
			default:
				break; // shouldn't happen
		} // switch
		return newCodedData.toString();
	} // updateCodedData(String, String)

	/** Returns values of fields of this evaluator.  For debugging.
	 * @return	description of fields of this evaluator
	 */
	public String toString() {
		return toString("");
	} // toString()

	/** Returns values of fields of this evaluator.  For debugging.
	 * @param	pre	prefix for each line
	 * @return	description of fields of this evaluator
	 */
	public String toString(String pre) {
		return Utils.toString(pre, "matchCode = ", matchCode, '\n', 
				pre, "codedData = ", codedData, '\n',
				Utils.isEmpty(molName) ? "" : Utils.getBuilder(
					pre, "molName = ", molName, '\n'));
	} // toString(String)

} // Subevaluator
