package com.epoch.evals.impl.genericQEvals.tableEvals;

import com.epoch.evals.OneEvalResult;
import com.epoch.evals.evalConstants.OneEvalConstants;
import com.epoch.evals.impl.TextAndNumbers;
import com.epoch.evals.impl.genericQEvals.numericEvals.NumberIs;
import com.epoch.evals.impl.genericQEvals.tableEvals.tableEvalConstants.TableImplConstants;
import com.epoch.evals.impl.genericQEvals.textEvals.textEvalConstants.TextConstants;
import com.epoch.evals.impl.genericQEvals.textEvals.TextContains;
import com.epoch.genericQTypes.TableQ;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.Locale;

/** If, for {any, every} row in which the cell at column <i>m</i>
 * has a value, the cell at column <i>n</i> has the value ...  */
public class TableDependent extends TextAndNumbers implements OneEvalConstants,
		QuestionConstants, TableImplConstants, TextConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether any or every row will satisfy the evaluator. */ 
	protected int rowOper;
	/** The 1-based column that may contain the reference value. */
	protected int columnRef;
	/** Operator to compare the number in <code>columnRef</code> to
	 * <code>authNumRef</code>. */
	transient protected int operRef;
	/** Number which should be found in <code>columnRef</code> before testing
	 * the value in <code>columnTest</code>. */
	transient protected String authNumRefStr = "";
	/** Tolerance of <code>authNumRef</code>. */
	transient protected String toleranceRefStr = "";
	/** The 1-based column of the cell whose contents are evaluated
	 * when the reference column contains a certain value. */
	protected int columnTest;
	/** Whether the reference and test values of the evaluator are numeric. */
	transient protected boolean[] isNumeric;

	/** Constructor.  */
	protected TableDependent() { /* empty */ }

	/** Sets values for variables based on which evaluator has been called.
	 * Magic numbers are due to the peculiarities of each evaluator's coded 
	 * data.
	 * @param	splitData	the coded data split into pieces
	 */
	protected void setValues(String[] splitData) {
		rowOper = Utils.indexOf(ROW_OPER, splitData[0]); 
		columnRef = MathUtils.parseInt(splitData[1]); 
		columnTest = MathUtils.parseInt(splitData[2]); 
		emptyCell = Utils.indexOf(EMPTYCELL, splitData[3]);
		if (isNumeric[REF]) {
			if (isNumeric[TEST]) { // TableNumNum
				operRef = Utils.indexOf(SYMBOLS, splitData[5]); 
				authNumRefStr = splitData[6]; 
				toleranceRefStr = splitData[7]; 
				setNumericValues(splitData, 8);
			} else { // TableNumText
				setNumericValues(splitData, 5);
				setStringValues(splitData, 8);
			} // isNumeric[TEST]
		} else { // ref value is text
			if (isNumeric[TEST]) { // TableTextNum
				setNumericValues(splitData, 5);
				setIgnoreCase(splitData, 8);
			} else { // TableTextText
				setStringValues(splitData, 4);
			} // isNumeric[TEST]
		} // isNumeric[REF]
	} // setValues(String[])

	/** Sets values for nonnumeric, oper, authNumStr, and toleranceStr. 
	 * @param	splitData   the coded data split into pieces
	 * @param	start	the index at which the values start 
	 */
	private void setNumericValues(String[] splitData, int start) {
		nonnumeric = Utils.indexOf(EMPTYCELL, splitData[4]);
		setOper(Utils.indexOf(SYMBOLS, splitData[start]));
		authNumStr = splitData[start + 1];
		toleranceStr = splitData[start + 2];
	} // setNumericValues(String[], int)

	/** Sets values for isPositive, where, and ignoreCase.
	 * @param	splitData   the coded data split into pieces
	 * @param	start	the index at which the values start 
	 */
	private void setStringValues(String[] splitData, int start) {
		isPositive = Utils.isPositive(splitData[start]);
		where = Utils.indexOf(WHERE, splitData[start + 1]);
		setIgnoreCase(splitData, start + 2);
	} // setStringValues(String[], int)

	/** Sets value for ignoreCase.
	 * @param	splitData   the coded data split into pieces
	 * @param	locn	the index at which the value is found 
	 */
	private void setIgnoreCase(String[] splitData, int locn) {
		ignoreCase = Utils.isPositive(splitData[locn]);
	} // setIgnoreCase(String[], int)

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
		debugPrint("TableDependent.toEnglish(): strName = ", strName);
		final String[] strNames = splitStrings(strName);
		final StringBuilder words = Utils.getBuilder("If, for ", 
				ROW_OPER[rowOper], " row in which the ",
				isNumeric[REF] ? "numerical " : "text ",
				"value in the cell at column ", columnRef, " is");
		if (isNumeric[REF]) {
			appendNumber(words, REF);
		} else {
			words.append(' ');
			Utils.addSpanString(words, strNames[0], !TO_DISPLAY);
			if (ignoreCase) words.append(" (ignoring case)");
		} // isNumeric[REF]
		Utils.appendTo(words, ", the ", isNumeric[TEST] ? "numerical " 
				: "text ", "value in the cell at column ", columnTest);
		if (isNumeric[TEST]) {
			words.append(" is");
			appendNumber(words, TEST);
		} else {
			words.append(" does");
			if (!isPositive) words.append(" not");
			words.append(WHERE_ENGLISH[where]);
			if (ignoreCase) words.append(" (ignoring case)");
			words.append(' ');
			Utils.addSpanString(words, strNames[strNames.length - 1], 
					!TO_DISPLAY);
		} // isNumeric[TEST]
		words.append(" (");
		if (!isNumeric[REF] && !isNumeric[TEST]) {
			words.append(EMPTYCELL_ENGL[emptyCell]);
		} else { 
			final boolean isNumNum = isNumeric[REF] && isNumeric[TEST];
			Utils.appendTo(words, EMPTYCELL_ENGL[emptyCell].replaceAll(
						"\"\"", isNumNum ? "0" : "\"\" or 0"),
					", ", NONNUMERIC_ENGL[nonnumeric]);
		} // isNumeric[REF or TEST]
		return Utils.toString(words, ')');
	} // toEnglish()

	/** Splits a string that may actually be two concatenated strings at the
	 * separation character.
	 * @param	str	a string
	 * @return	array with one or two strings as members
	 */
	public static String[] splitStrings(String str) {
		return str.split(TableTextText.TWO_STR_SEP);
	} // splitStrings(String)

	/** Appends operator, number, tolerance to a growing StringBuilder.
	 * @param	words	the growing StringBuilder
	 * @param	refOrTest	whether we are appending a reference number or a
	 * test number
	 */
	private void appendNumber(StringBuilder words, int refOrTest) {
		String tolStr;
		if (refOrTest == REF && isNumeric[REF] && isNumeric[TEST]) {
			Utils.appendTo(words, 
					OPER_ENGLISH[LESSER][operRef], authNumRefStr);
			tolStr = toleranceRefStr;
		} else {
			Utils.appendTo(words, OPER_ENGLISH[LESSER][getOper()], authNumStr);
			tolStr = toleranceStr;
		} // if should use operRef or oper
		if (MathUtils.parseDouble(tolStr) != 0) {
			Utils.appendTo(words, " &plusmn; ", tolStr);
		} // there is a tolerance
	} // appendNumber(StringBuilder)

	/** Determines whether the response table's cells correlate as indicated.
	 * @param	response	a parsed response
	 * @param	authString	string to which to compare the response
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "TableDependent.isResponseMatching: ";
		OneEvalResult evalResult = new OneEvalResult();
		final TableQ respTableQ = (TableQ) response.parsedResp;
		String refString = "";
		String testString = "";
		if (!isNumeric[REF] && !isNumeric[TEST]) {
			final String[] strs = splitStrings(authString);
			refString = strs[0].trim(); 
			testString = strs[1].trim();
		} else if (!isNumeric[REF]) {
			refString = authString;
		} else if (!isNumeric[TEST]) {
			testString = authString;
		}
		final String[][] table = respTableQ.entries;
		debugPrint(SELF + "response has ", table.length, " rows and ", 
				table[0].length, " columns; ", toEnglish());
		// evaluate relevant cells
		final int numCols = table[0].length;
		final int[] cols = new int[] {columnRef, columnTest};
		for (int colNum = 0; colNum < 2; colNum++) {
			if (!MathUtils.inRange(cols[colNum], new int[] {1, numCols})) {
				evalResult.verificationFailureString = "ACE could not "
						+ "evaluate cells in column " + cols[colNum]
						+ " because the response table has only "
						+ numCols + " columns.";
				return evalResult;
			} // if column is out of range
		} // for each column
		int numChecked = 0;
		int numMatches = 0;
		final boolean NEED_NUM = true;
		final int numRows = table.length;
		final StringBuilder colorCellsBld = new StringBuilder();
		for (int rowNum = 1; rowNum <= numRows; rowNum++) {
			boolean skipToNext = false;
			String[] cellVals = new String[2];
			// check that reference and test values can be evaluated
			for (int colNum = 0; colNum < 2; colNum++) {
				cellVals[colNum] = table[rowNum - 1][cols[colNum] - 1].trim();
				if ("".equals(cellVals[colNum])) {
					switch (emptyCell) {
					case IGNORE: skipToNext = true; break;
					case DISALLOW:
						debugPrint(SELF + "disallowed empty cell.");
						getErrorData(evalResult, rowNum, 
								cols[colNum], !NEED_NUM);
						return evalResult;
					case EMPTY_STR: break; // do nothing
					default: debugPrint(SELF + "bad case"); break;
					} // switch(emptyCell)
				} // if cell is empty
				if (((isNumeric[REF] && colNum == 0) 
							|| (isNumeric[TEST] && colNum == 1)) 
						&& !MathUtils.isDouble(cellVals[colNum])) { 
					switch (nonnumeric) {
						case IGNORE: skipToNext = true; break;
						case DISALLOW:
							debugPrint(SELF + "disallowed nonnumeric "
									+ "value ", cellVals[colNum]);
							getErrorData(evalResult, rowNum, 
									cols[colNum], NEED_NUM);
							return evalResult;
						case ZERO: cellVals[colNum] = "0"; break;
						default: debugPrint(SELF + "bad case"); break;
					} // switch nonnumeric
				} // if cell value should be numeric but it isn't
				if (skipToNext) break;
			} // for each column
			if (skipToNext) continue;
			// prepare to evaluate value in reference column
			if (ignoreCase && !isNumeric[REF]) {
				cellVals[REF] = cellVals[REF].toLowerCase(Locale.US);
			} // if ignore case for reference string
			boolean refMatches;
			if (isNumeric[REF]) {
				final OneEvalResult refResult =	(isNumeric[TEST] 
					? numberMatches(cellVals[REF], operRef, authNumRefStr, 
							toleranceRefStr)
					: numberMatches(cellVals[REF], getOper(), authNumStr, 
							toleranceStr));
				refMatches = refResult.isSatisfied;
			} else refMatches = cellVals[REF].equals(ignoreCase 
					? refString.toLowerCase(Locale.US) : refString);
			if (refMatches) {
				debugPrint(SELF + "reference row ", rowNum, " column ", 
						columnRef, " value ", cellVals[REF], " satisfies test.");
				numChecked++;
				// evaluate value in test column
				final OneEvalResult testResult = (isNumeric[TEST] 
						? numberMatches(cellVals[TEST], getOper(), 
							authNumStr, toleranceStr)
						: textMatches(cellVals[TEST], testString));
				if (testResult.verificationFailureString != null) {
					debugPrint(SELF + "verification failure.");
					testResult.toModifyResponse = Utils.toString(rowNum, ':', 
							columnRef, ';', rowNum, ':', columnTest);
					return testResult;
				} else if (testResult.isSatisfied) {
					debugPrint(SELF + "column ", columnTest, " value ", 
							cellVals[TEST], " satisfies test.");
					numMatches++;
					if (colorCellsBld.length() > 0) colorCellsBld.append(';');
					Utils.appendTo(colorCellsBld, rowNum, ':', 
							columnRef, ';', rowNum, ':', columnTest);
				} else debugPrint(SELF + "column ", columnTest, 
						" value ", cellVals[TEST], " fails test.");
			} else debugPrint(SELF + "row ", rowNum, " column ", columnRef,
					" value ", cellVals[REF], " fails test.");
		} // for each row rowNum
		if ((rowOper == EVERY_ROW && numMatches == numChecked) 
				|| (rowOper == ANY_ROW && numMatches > 0)) {
			evalResult.isSatisfied = true;
			evalResult.toModifyResponse = colorCellsBld.toString();
			if (!Utils.isEmpty(evalResult.toModifyResponse)) {
				evalResult.autoFeedback = new String[]
						{"Relevant table cells have "
						+ "***this background color***."};
				evalResult.autoFeedbackVariableParts = new String[] 
						{"<span style=\"background-color:" 
							+ TableQ.WRONG_BACKGROUND_COLOR
							+ ";\">", 
						"</span>"};
				// insert untranslated variable parts around demarcated phrase
				evalResult.howHandleVarParts |= INSERT;
			} // if there are cells that will be colored
		} // if evaluator is satisfied
		debugPrint(SELF, numMatches, " matches out of ", numChecked,
				" cells checked, returning ", evalResult.isSatisfied);
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Adds data to a OneEvalResult about an error.
	 * @param	evalResult	the OneEvalResult
	 * @param	rowNum	row of the table containing the error
	 * @param	col	column of the table containing the error
	 * @param	needsNumeric	whether the cell is supposed to contain a
	 * numeric value
	 */
	private void getErrorData(OneEvalResult evalResult, int rowNum, 
			int col, boolean needsNumeric) {
		evalResult.toModifyResponse = Utils.toString(rowNum, ':', col);
		final StringBuilder colorCellsBld = Utils.getBuilder(
				"All cells in column ", col, " must contain a ");
		if (needsNumeric) colorCellsBld.append("numerical ");
		colorCellsBld.append("value.");
		evalResult.verificationFailureString = colorCellsBld.toString();
	} // getErrorData(OneEvalResult, int, int, boolean)

	/** Determines whether a number in a cell matches a given number within a
	 * tolerance.
	 * @param	cellVal	value in the cell
	 * @param	cellOper	how to compare the numbers
	 * @param	numStr	number to compare
	 * @param	tolStr	tolerance of comparison
	 * @return	the result of the comparison
	 */
	private OneEvalResult numberMatches(String cellVal, int cellOper,
			String numStr, String tolStr) {
		final Response cell = new Response(NUMERIC, cellVal);
		final NumberIs numIs = new NumberIs();
		numIs.setOper(cellOper);
		numIs.setAuthNum(numStr);
		numIs.setTolerance(tolStr);
		return numIs.isResponseMatching(cell, null);
	} // numberMatches(String, int, String, String);

	/** Determines whether a string in a cell matches a given string.
	 * @param	cellVal	value in the cell
	 * @param	str	string to which to compare the value
	 * @return	the result of the comparison
	 */
	private OneEvalResult textMatches(String cellVal, String str) {
		final Response cell = new Response(TEXT, cellVal);
		final TextContains tc = new TextContains();
		tc.setIsPositive(isPositive);
		tc.setIgnoreCase(ignoreCase);
		tc.setWhere(where);
		return tc.isResponseMatching(cell, str);
	} // textMatches(String, String);

	/** Gets whether to be satisfied by any or every row.
	 * @return	whether to be satisfied by any or every row
	 */
	public int getRowOper() 				{ return rowOper; } 
	/** Sets whether to be satisfied by any or every row.
	 * @param	ro	whether to be satisfied by any or every row
	 */
	public void setRowOper(int ro)			{ rowOper = ro; } 
	/** Gets the 1-based independent column.
	 * @return	the 1-based independent column
	 */
	public int getColumnRef() 				{ return columnRef; }
	/** Sets the 1-based independent column.
	 * @param	cRef	the 1-based independent column
	 */
	public void setColumnRef(int cRef)		{ columnRef = cRef; }
	/** Gets the 1-based dependent column.
	 * @return	the 1-based dependent column
	 */
	public int getColumnTest() 				{ return columnTest; }
	/** Sets the 1-based dependent column.
	 * @param	cTest	the 1-based dependent column
	 * for none
	 */
	public void setColumnTest(int cTest)	{ columnTest = cTest; }
	/** Required by interface.  Sets a possibly shortened version of the 
	 * test string for display.
	 * @param	str	the test string (maybe shortened)
	 */
	public void setMolName(String str) 		{ strName = str; } 

} // TableDependent
