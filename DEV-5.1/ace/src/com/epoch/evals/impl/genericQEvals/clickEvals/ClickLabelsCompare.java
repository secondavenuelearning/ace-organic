package com.epoch.evals.impl.genericQEvals.clickEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.ClickImage;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** If {no, any} mark in the given region(s) has text that is 
 * {identical to, different from} that of another mark in the same
 * region(s) after character <i>n</i> ...  */
public class ClickLabelsCompare extends ClickHere implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}
	
	/** One-based number of the character at which to start comparing text. */
	private int startChar;

	/** Constructor. */
	public ClickLabelsCompare() {
		howMany = ANY;
		isPositive = true;
		startChar = 1;
		ignoreCase = true;
	} // ClickLabelsCompare()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>howMany</code>/<code>isPositive</code>/<code>startChar</code>/<code>ignoreCase</code>
	 * @throws	ParameterException	if the coded data is inappropriate
	 * for this evaluator
	 */
	public ClickLabelsCompare(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 4) { 
			howMany = MathUtils.parseInt(splitData[0]); // inherited from Counter
			isPositive = Utils.isPositive(splitData[1]); // inherited from TextAndNumbers
			startChar = MathUtils.parseInt(splitData[2]);
			ignoreCase = Utils.isPositive(splitData[3]); // inherited from TextAndNumbers
		}
		if (splitData.length < 4 || where == -1) {
			throw new ParameterException("ClickLabelsCompare ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // ClickLabelsCompare(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>howMany</code>/<code>isPositive</code>/<code>startChar</code>/<code>ignoreCase</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(howMany, isPositive ? "/Y/" : "/N/",
				startChar, ignoreCase ? "/Y" : "/N");
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
				HOWMANY_ENGL[howMany - 1],
				"marks in the given region(s) have text that is ",
				isPositive ? "identical to" : "different from",
				" that of other marks in the given region(s)");
		if (startChar > 1) {
			Utils.appendTo(words, " starting at character ", startChar);
		} // if not starting at character 1
		if (ignoreCase) words.append(" (ignoring case)");
		return words.toString();
	} // toEnglish()

	/** Determines whether any marks have identical text.
	 * @param	response	a parsed response
	 * @param	authString	XML describing the shapes of the regions and their 
	 * coordinates
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "ClickLabelsCompare.isResponseMatching: ";
		debugPrint(SELF, toEnglish());
		final OneEvalResult evalResult = new OneEvalResult();
		final ClickImage clickResp = (ClickImage) response.parsedResp;
		final int[][] allRespCoords = clickResp.getAllCoords();
		extractShapes(authString);
		debugPrint(SELF + "allRespCoords = ", allRespCoords,
				", authString = ", authString,
				", allShapes = ", allShapes);
		final List<String> clippedStrs = new ArrayList<String>();
		boolean foundEquals = !isPositive;
		int markNum = 1;
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
				if (markStr != null && markStr.length() >= startChar) {
					final String clipped = markStr.substring(startChar - 1);
					if (!clippedStrs.isEmpty()) {
						for (final String clippedStr : clippedStrs) {
							foundEquals = (ignoreCase
									? clipped.equalsIgnoreCase(clippedStr)
									: clipped.equals(clippedStr));
							if (foundEquals == isPositive) break;
						} // for each already clipped string
					} // if at first mark
					if (foundEquals == isPositive) break;
					clippedStrs.add(clipped);
				} // if markStr can be clipped
			} // if mark is in region
			markNum++;
		} // for each mark
		evalResult.isSatisfied = 
				(foundEquals == isPositive) == (howMany == ANY);
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[CLICK_LABELS_COMP]; } 
	/** Gets the 1-based number of the character at which to start comparing 
	 * marks to one another.
	 * @return	the 1-based number of the character at which to start comparing
	 * marks to one another
	 */
	public int getStartChar()				{ return startChar; }
	/** Sets the 1-based number of the character at which to start comparing 
	 * marks to one another.
	 * @param	sc	the 1-based number of the character at which to start 
	 * comparing marks to one another
	 */
	public void setStartChar(int sc)		{ startChar = sc; }

} // ClickLabelsCompare

