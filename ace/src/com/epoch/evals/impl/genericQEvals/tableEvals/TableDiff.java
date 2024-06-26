package com.epoch.evals.impl.genericQEvals.tableEvals;

import com.epoch.evals.OneEvalResult;
import com.epoch.evals.evalConstants.OneEvalConstants;
import com.epoch.evals.impl.genericQEvals.numericEvals.NumberIs;
import com.epoch.evals.impl.genericQEvals.tableEvals.tableEvalConstants.TableImplConstants;
import com.epoch.evals.impl.genericQEvals.textEvals.TextContains;
import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.TableQ;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the response table matches the author's table ... */
public class TableDiff extends TextContains 
		implements OneEvalConstants, QuestionConstants, TableImplConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether this evaluator's calculated grade should override the author's
	 * grade. */
	private boolean calcGrade;
	/** Whether any cells containing incorrect values should be highlighted. */
	private boolean highlightWrong;

	/** Constructor. */
	public TableDiff() {
		where = IS;
		emptyCell = EMPTY_STR;
		calcGrade = true;
		highlightWrong = true;
	} // TableDiff()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>where</code>/<code>ignoreCase</code>/<code>calcGrade</code>/<code>highlightWrong</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public TableDiff(String data) throws ParameterException {
		debugPrint("TableDiff.java: data = ", data);
		final String[] splitData = data.split("/");
		if (splitData.length >= 5) { 
			isPositive = Utils.isPositive(splitData[0]); // inherited from TextAndNumbers
			where = Utils.indexOf(WHERE, splitData[1]); // inherited from TextAndNumbers
			ignoreCase = Utils.isPositive(splitData[2]); // inherited from TextAndNumbers
			calcGrade = Utils.isPositive(splitData[3]);
			highlightWrong = Utils.isPositive(splitData[4]);
			debugPrint("TableDiff.java: isPositive = ", isPositive,
					", ignoreCase = ", ignoreCase, ", calcGrade = ", calcGrade,
					", highlightWrong = ", highlightWrong);
		} else {
			throw new ParameterException("TableDiff ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // TableDiff(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>isPositive</code>/<code>where</code>/<code>ignoreCase</code>/<code>calcGrade</code>/<code>highlightWrong</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive ? "Y/" : "N/", WHERE[where],
				ignoreCase ? "/Y" : "/N", calcGrade ? "/Y" : "/N",
				highlightWrong ? "/Y" : "/N");
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
		final StringBuilder words = Utils.getBuilder("If ", isPositive 
				? "every" : "any", " cell of the response table does ");
		if (!isPositive) words.append("not ");
		words.append(WHERE_ENGLISH[where]);
		if (ignoreCase) words.append("(ignoring case) ");
		return Utils.toString(words, 
				"the corresponding cell of the author's table");
	 } // toEnglish()

	/** Determines whether the response table's cells contain the indicated text.
	 * @param	response	a parsed response
	 * @param	authTableStr	String representing the author's table
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authTableStr) {
		final String SELF = "TableDiff.isResponseMatching: ";
		OneEvalResult evalResult = new OneEvalResult();
		final TableQ respTableQ = (TableQ) response.parsedResp;
		final TableQ authTableQ = new TableQ(authTableStr);
		final int respRowCt = respTableQ.getNumRows();
		final int respColCt = respTableQ.getNumCols();
		final int authRowCt = authTableQ.getNumRows();
		final int authColCt = authTableQ.getNumCols();
		if (respRowCt != authRowCt || respColCt != authColCt) {
			evalResult.verificationFailureString = "The response table's "
					+ "dimensions are different from those of the question "
					+ "author's reference table; please report this error "
					+ "to your instructor.";
			debugPrint(SELF + evalResult.verificationFailureString);
			return evalResult;
		} // if table shapes are different
		debugPrint(SELF + "response and author tables have ", respRowCt, 
				" rows and ", respColCt, " columns");
		// evaluate relevant cells
		int numEditable = 0;
		int numMatches = 0;
		final StringBuilder colorCellsBld = new StringBuilder();
		for (int rowNum = 0; rowNum < authRowCt; rowNum++) {
			for (int colNum = 0; colNum < authColCt; colNum++) {
				if (respTableQ.disabled[rowNum][colNum]) continue;
				numEditable++;
				final String cellVal = 
						respTableQ.entries[rowNum][colNum].trim();
				final String testString = 
						authTableQ.entries[rowNum][colNum].trim();
				final OneEvalResult cellResult = 
						evaluateCell(cellVal, testString);
				// cellResult.isSatisfied will be false if isPositive 
				// is false and there is a match
				final boolean match = cellResult.isSatisfied == isPositive;
				debugPrint(SELF + "row ", rowNum + 1, ", column ", colNum + 1,
						", resp cellVal = ", cellVal, ", auth cellVal = ",
						testString, ", isPositive = ", isPositive,
						", match = ", match);
				if (match) numMatches++;
				else if (!isPositive) {
					if (colorCellsBld.length() > 0) colorCellsBld.append(';');
					Utils.appendTo(colorCellsBld, rowNum + 1, ':', colNum + 1);
				} else { 
					evalResult.isSatisfied = false;
					debugPrint(SELF, "found mismatch, returning false");
					return evalResult;
				} // if isPositive
			} // for each column
		} // for each row
		evalResult.isSatisfied = (isPositive ? numMatches == numEditable
				: numMatches < numEditable);
		if (!isPositive && evalResult.isSatisfied) {
			if (calcGrade) evalResult.calcScore = 
					((double) numMatches) / ((double) numEditable);
			if (highlightWrong) {
				evalResult.toModifyResponse = colorCellsBld.toString();
				evalResult.autoFeedback = new String[]
						{"Table cells containing incorrect responses have "
						+ "***this background color***."};
				evalResult.autoFeedbackVariableParts = new String[] 
						{"<span style=\"background-color:" 
							+ TableQ.WRONG_BACKGROUND_COLOR
							+ ";\">", 
						"</span>"};
				// insert untranslated variable parts around demarcated phrase
				evalResult.howHandleVarParts |= INSERT;
			} else {
				evalResult.toModifyResponse = ""; // don't color any
			} // if highlight cells with incorrect responses
			debugPrint(SELF, numMatches, " matches out of ", numEditable,
					" editable cells, calcScore = ", evalResult.calcScore,
					", highlightWrong = ", highlightWrong,
					", toModifyResponse = ", evalResult.toModifyResponse,
					", returning ", evalResult.isSatisfied);
		} else debugPrint(SELF, numMatches, " matches out of ", numEditable,
				" editable cells, highlightWrong = ", highlightWrong,
				", returning ", evalResult.isSatisfied);
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Determines whether a single cell of the response table contains
	 * the indicated text or number.
	 * @param	cellVal	a single cell of the table, trimmed
	 * @param	testString	the given text or number
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	private OneEvalResult evaluateCell(String cellVal, String testString) {
		final String SELF = "TableDiff.evaluateCell: ";
		OneEvalResult evalResult = null;
		if (where == IS) {
			final String testStr = Utils.cersToUnicode(testString);
			final char PLUS_MINUS = 177;
			final String[] parts = testStr.split(String.valueOf(PLUS_MINUS));
			final boolean hasTolerance = parts.length == 2;
			if (parts.length <= 2 
					&& MathUtils.isDouble(cellVal)
					&& MathUtils.isDouble(parts[0], MathUtils.TRIM)
					&& (!hasTolerance || MathUtils.isDouble(parts[1], MathUtils.TRIM))) {
				if (hasTolerance) {
					debugPrint(SELF, "testString ", testStr, 
							" contains ", PLUS_MINUS, ", and ", 
							parts[0], ", ", parts[1], ", and ", cellVal, 
							" are all numbers; comparing with NumberIs.");
				} else debugPrint(SELF, parts[0], " and ", cellVal, 
						" are both numbers; comparing with NumberIs.");
				final NumberIs numIs = new NumberIs();
				numIs.setOper(isPositive ? EQUALS : NOT_EQUALS);
				numIs.setAuthNum(parts[0].trim());
				if (hasTolerance) numIs.setTolerance(parts[1].trim());
				final Response cell = new Response(NUMERIC, cellVal);
				evalResult = numIs.isResponseMatching(cell, null);
			} // if string does not contain ± or contains one instance
		} // if comparing entire strings
		if (evalResult == null) {
			final Response cell = new Response(TEXT, cellVal);
			evalResult = super.isResponseMatching(cell, testString);
		} // if haven't already acquired evalResult
		return evalResult;
	} // evaluateCell(String[], String)

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[TABLE_DIFF]; } 
	/** Gets whether to calculate the grade from the response.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 				{ return calcGrade; }
	/** Sets whether to calculate the grade from the response.
	 * @param	calc	whether to calculate the grade from the response
	 */
	public void setCalcGrade(boolean calc)		{ calcGrade = calc; }
	/** Gets whether to highlight cells containing incorrect values.
	 * @return	true if should highlight cells containing incorrect values
	 */
	public boolean getHighlightWrong() 			{ return highlightWrong; }
	/** Sets whether to highlight cells containing incorrect values.
	 * @param	hl	whether to highlight cells containing incorrect values
	 */
	public void setHighlightWrong(boolean hl)	{ highlightWrong = hl; }

} // TableDiff

