package com.epoch.evals.impl.genericQEvals.clickEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.genericQEvals.numericEvals.NumberIs;
import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.ClickImage;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If {no, the only, exactly one, any, not every, every} mark in the 
 * region(s) defined by the author has a numerical value that
 * {is, is not} {=, &lt;, &gt;} <i>n</i> ... */
public class ClickNumber extends ClickHere 
		implements EvalInterface, QuestionConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	public ClickNumber() {
		howMany = ANY;
		setOper(NOT_EQUALS); // inherited from CompareNums
	} // ClickNumber()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>howMany</code>/<code>oper</code>/<code>authCoeff</code>/<code>tolerance</code>/0
	 * @throws	ParameterException	if the coded data is inappropriate
	 * for this evaluator
	 */
	public ClickNumber(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 4) { 
			howMany = MathUtils.parseInt(splitData[0]); // inherited from Counter
			setOper(Utils.indexOf(SYMBOLS, splitData[1]));
			authNumStr = splitData[2];
			toleranceStr = splitData[3];
		}
		if (splitData.length < 4 || getOper() == -1) {
			throw new ParameterException("ClickNumber ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // ClickNumber(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>howMany</code>/<code>oper</code>/<code>authCoeff</code>/<code>tolerance</code>/0
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(howMany, '/', SYMBOLS[getOper()],
				'/', authNumStr, '/', toleranceStr, "/0");
	} // getCodedData()

	/** Gets an English-language description of this evaluator.
	 * @param	qDataTexts	the text of question data of this question, if any;
	 * not used, but required by interface
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts, boolean forPermissibleSM) {
		return toEnglish();
	} // toEnglish(String[], boolean)

	/** Gets an English-language description of this evaluator.
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish() {
		final StringBuilder words = Utils.getBuilder("If", 
				HOWMANY_ENGL[howMany - 1], "mark in the given region(s)",
				OPER_ENGLISH[LESSER][getOper()], authNumStr);
		if (MathUtils.parseDouble(toleranceStr) != 0) {
			Utils.appendTo(words, " &plusmn; ", toleranceStr);
		} // if there's a tolerance
		return words.toString();
	} // toEnglish()

	/** Determines whether the user has clicked in the indicated region(s).
	 * @param	response	a parsed response
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * @param	authString	XML describing the shapes of the regions and their 
	 * coordinates
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "ClickNumber.isResponseMatching: ";
		debugPrint(SELF, toEnglish());
		final OneEvalResult evalResult = new OneEvalResult();
		final ClickImage clickResp = (ClickImage) response.parsedResp;
		final int[][] allRespCoords = clickResp.getAllCoords();
		final String modCodedData = getCodedData().substring(2);
		extractShapes(authString);
		debugPrint(SELF + "allRespCoords = ", allRespCoords,
				", modCodedData = ", modCodedData,
				", authString = ", authString,
				", allShapes = ", allShapes);
		final int[] matches = new int[2];
		int markNum = 1;
		try {
			final NumberIs numberIs = new NumberIs(modCodedData);
			debugPrint(SELF + "will subject text of each mark in the "
					+ "appropriate region to: ", numberIs.toEnglish(null));
			for (final int[] respCoords : allRespCoords) {
				boolean markInRegion = false;
				int shapeNum = 0;
				for (final int[][] shapes : allShapes) {
					for (final int[] shape : shapes) {
						markInRegion = inRegion(respCoords, shape, shapeNum); 
						if (markInRegion) break; 
					} // for each shape
					if (markInRegion) break;
					shapeNum++;
				} // for each type of shape
				if (markInRegion) {
					final String markStr = clickResp.getMarkStr(markNum);
					final Response numResp = new Response(NUMERIC, markStr);
					final OneEvalResult matchResult = 
							numberIs.isResponseMatching(numResp, null);
					if (matchResult.isSatisfied) {
						matches[MATCHES]++;
						debugPrint(SELF + "mark ", markNum, " with coords ", 
								respCoords, " is in region, and its number ", 
								markStr, " matches the author's string.");
						if (Utils.among(howMany, ANY, NONE)) {
							debugPrint(SELF 
									+ "we know enough to make a decision.");
							break;
						} // if we know enough now
					} else {
						matches[NONMATCHES]++;
						debugPrint(SELF + "mark ", markNum, " with coords ", 
								respCoords, " is in region, but its number ", 
								markStr, " doesn't match the author's string.");
						if (Utils.among(howMany, NOT_ALL, ALL)) {
							debugPrint(SELF 
									+ "we know enough to make a decision.");
							break;
						} // if we know enough now
					} // if text matches
				} // if mark was in region
				markNum++;
			} // for each mark
			evalResult.isSatisfied = getIsSatisfied(matches);
		} catch (ParameterException e) {
			Utils.alwaysPrint(SELF + "caught ParameterException when trying "
					+ "to instantiate NumberIs with ", modCodedData);
			e.printStackTrace();
			evalResult.verificationFailureString = "ACE could not evaluate the "
					+ "response because NumberIs could not parse the coded "
					+ "data " + modCodedData + ".  Please report this error "
					+ "to the programmers.";
		} // try
		debugPrint(SELF + "matches = ", matches[MATCHES],
				", nonmatches = ", matches[NONMATCHES],
				", isSatisfied = ", evalResult.isSatisfied);
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[CLICK_NUM]; } 

} // ClickNumber

