package com.epoch.evals.impl.genericQEvals.clickEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.genericQEvals.textEvals.textEvalConstants.TextConstants;
import com.epoch.evals.impl.genericQEvals.textEvals.TextContains;
import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.ClickImage;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If {no, the only, exactly one, any, not every, every} mark in the 
 * region(s) defined by the author has text that {is, begins with, ends 
 * with, contains, contains internally, contains the regular expression} 
 * "..." ... */
public class ClickText extends ClickHere 
		implements EvalInterface, QuestionConstants, TextConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	public ClickText() {
		howMany = ANY;
		isPositive = true;
		where = IS;
		ignoreCase = true;
	} // ClickText()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>howMany</code>/<code>isPositive</code>/<code>where</code>/<code>ignoreCase</code>
	 * @throws	ParameterException	if the coded data is inappropriate
	 * for this evaluator
	 */
	public ClickText(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 4) { 
			howMany = MathUtils.parseInt(splitData[0]); // inherited from Counter
			isPositive = Utils.isPositive(splitData[1]); // inherited from TextAndNumbers
			where = Utils.indexOf(WHERE, splitData[2]); // inherited from TextAndNumbers
			ignoreCase = Utils.isPositive(splitData[3]); // inherited from TextAndNumbers
		}
		if (splitData.length < 4 || where == -1) {
			throw new ParameterException("ClickText ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // ClickText(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>howMany</code>/<code>isPositive</code>/<code>where</code>/<code>ignoreCase</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(howMany, isPositive ? "/Y/" : "/N/",
				WHERE[where], ignoreCase ? "/Y" : "/N");
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
				HOWMANY_ENGL[howMany - 1], "mark in the given region(s)");
		if (!isPositive) { 
			Utils.appendTo(words, " does not", WHERE_ENGLISH[where]);
		} else {
			final int posn = WHERE_ENGLISH[where].indexOf(" ", 1);
			Utils.appendTo(words, 
					WHERE_ENGLISH[where].substring(0, posn),
					where == MATCHES_REGEX ? "es" : 's',
					WHERE_ENGLISH[where].substring(posn));
		} // if isPositive
		if (!Utils.among(where, MATCHES_REGEX, CONT_REGEX) && ignoreCase) {
			words.append("(ignoring case) ");
		} // if ignoring case in regex search
		Utils.addSpanString(words, strName, !TO_DISPLAY);
		return words.toString();
	} // toEnglish()

	/** Determines whether the text of a mark in the indicated region(s) matches
	 * the author's text.
	 * @param	response	a parsed response
	 * @param	authString	XML describing the shapes of the regions and their 
	 * coordinates, plus text
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "ClickText.isResponseMatching: ";
		debugPrint(SELF, toEnglish());
		final OneEvalResult evalResult = new OneEvalResult();
		final String modCodedData = getCodedData().substring(2);
		final ClickImage clickResp = (ClickImage) response.parsedResp;
		final int[][] allRespCoords = clickResp.getAllCoords();
		final String authText = extractShapes(authString);
		debugPrint(SELF + "allRespCoords = ", allRespCoords,
				", modCodedData = ", modCodedData,
				", authString = ", authString,
				", allShapes = ", allShapes,
				", authText = ", authText);
		final int[] matches = new int[2];
		int markNum = 1;
		try {
			final TextContains textContains = new TextContains(modCodedData);
			debugPrint(SELF + "will subject text of each mark in the "
					+ "appropriate region to: ", textContains.toEnglish());
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
					final String respMarkStr = clickResp.getMarkStr(markNum);
					final Response textResp = new Response(TEXT, respMarkStr);
					final OneEvalResult matchResult = 
							textContains.isResponseMatching(textResp, authText);
					if (matchResult.isSatisfied) {
						matches[MATCHES]++;
						debugPrint(SELF + "mark ", markNum, " with coords ", 
								respCoords, " is in region, and its text ", 
								respMarkStr, " matches the author's string.");
						if (Utils.among(howMany, ANY, NONE)) {
							debugPrint(SELF 
									+ "we know enough to make a decision.");
							break;
						} // if we know enough now
					} else {
						matches[NONMATCHES]++;
						debugPrint(SELF + "mark ", markNum, " with coords ", 
								respCoords, " is in region, but its text ", 
								respMarkStr, 
								" doesn't match the author's string.");
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
					+ "to instantiate TextContains with ", modCodedData);
			e.printStackTrace();
			evalResult.verificationFailureString = "ACE could not evaluate the "
					+ "response because TextContains could not parse the coded "
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
	public String getMatchCode() 			{ return EVAL_CODES[CLICK_TEXT]; } 
	/** Required by interface.  Sets a possibly shortened version of the 
	 * test string for display.
	 * @param	str	the test string (maybe shortened)
	 */
	public void setMolName(String str) 		{ strName = str; } 

} // ClickText

