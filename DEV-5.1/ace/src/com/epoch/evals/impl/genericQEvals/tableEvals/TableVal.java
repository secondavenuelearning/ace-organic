package com.epoch.evals.impl.genericQEvals.tableEvals;

import com.epoch.evals.evalConstants.OneEvalConstants;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.CompareNums;
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

/** If the value of the cell at {any row, every row, no row, row <i>n</i>}
 * and {any column, every column, no column, column <i>n</i>}
 * in the response ... */
public class TableVal extends TextAndNumbers implements OneEvalConstants, 
		QuestionConstants, TableImplConstants, TextConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether we will be evaluating text or numerical values. */
	transient protected int typeOfValue = NUMERIC;
	/** Stores the operator and number of cells for evaluators that count cells.
	 */
	transient int[] operNumCells = null;
	/** Whether matching cells should be colored. */
	protected boolean colorSatisfying = true;
	/** Stores the cells that need to be colored. */  
	transient private StringBuilder colorCellsBld = new StringBuilder();

	/** Empty constructor.  */
	public TableVal() {
		// empty
	} // TableVal()

	/** Constructor.
	 * @param	valueType	whether we will be evaluating text or numerical
	 * values
	 * @param	data	the coded data for either a TableNumVal or TableTextVal evaluator
	 */
	public TableVal(int valueType, String data) {
		typeOfValue = valueType; 
		final String[] splitData = data.split("/");
		row = MathUtils.parseInt(splitData[1]); // inherited from TextAndNumbers
		column = MathUtils.parseInt(splitData[2]); // inherited from TextAndNumbers
		emptyCell = Utils.indexOf(EMPTYCELL, splitData[3]); // inherited from TextAndNumbers
		if (typeOfValue == NUMERIC) {
			setOper(Utils.indexOf(SYMBOLS, splitData[0])); // inherited from TextAndNumbers
			nonnumeric = Utils.indexOf(EMPTYCELL, splitData[4]); // inherited from TextAndNumbers
			authNumStr = splitData[5]; // inherited from TextAndNumbers
			toleranceStr = splitData[6]; // inherited from TextAndNumbers
			colorSatisfying = splitData.length < 8
					|| Utils.isPositive(splitData[7]);
		} else {
			isPositive = Utils.isPositive(splitData[0]); // inherited from TextAndNumbers
			where = Utils.indexOf(WHERE, splitData[4]); // inherited from TextAndNumbers
			ignoreCase = Utils.isPositive(splitData[5]); // inherited from TextAndNumbers
			colorSatisfying = splitData.length < 7
					|| Utils.isPositive(splitData[6]);
		} // if typeOfValue
	} // TableVal(long, String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format for numerical values is:<br>
	 * <code>oper</code>/<code>row</code>/<code>column</code>/<code>emptyCell</code>/<code>nonnumeric</code>/<code>authNum</code>/<code>tolerance</code>/<code>colorSatisfying</code>
	 * <br>Format for text values is:<br>
	 * <code>isPositive</code>/<code>row</code>/<code>column</code>/<code>emptyCell</code>/<code>where</code>/<code>ignoreCase</code>/<code>colorSatisfying</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(typeOfValue == NUMERIC
					? SYMBOLS[getOper()] : isPositive ? 'Y' : 'N',
				'/', row, '/', column, '/', EMPTYCELL[emptyCell], '/',
				typeOfValue == NUMERIC
					? Utils.getBuilder(EMPTYCELL[nonnumeric], '/',
						authNumStr, '/', toleranceStr)
					: Utils.getBuilder(WHERE[where], ignoreCase ? "/Y" : "/N"),
				colorSatisfying ? "/Y" : "/N");
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
		final String valueType = 
				(typeOfValue == NUMERIC ? "numerical" : "text");
		return (operNumCells != NOT_COUNTING
				? Utils.toString("If the number of cells in ",
					row == EVERY ? "every row"
						: Utils.getBuilder("row ", row),
					" and ", column == EVERY ? "every column"
						: Utils.getBuilder("column ", column),
					" that have a ", valueType,
					" value that", valueToEnglish(),
					" is", OPER_ENGLISH[FEWER][operNumCells[0]],
					operNumCells[1])
				: row == EVERY && Utils.among(column, ANY, NO)
				? Utils.toString("If ", 
					column == ANY ? "any" : "no",
					" column has in every row a ",
					valueType, " value that ",
					valueToEnglish())
				: Utils.toString("If the ", valueType,
					" value in the cell at ", row > 0
						? Utils.getBuilder("row ", row)
						: Utils.getBuilder(row == ANY ? "any"
							: row == EVERY ? "every"
							: "no", " row"),
					" and ", column > 0
						? Utils.getBuilder("column ", column)
						: Utils.getBuilder(column == ANY ? "any"
							: column == EVERY ? "every"
							: "no", " column"),
					valueToEnglish()));
	} // toEnglish(int[])

	/** Gets the part of an English-language description of this evaluator
	 * describing the value.
	 * @return	short string describing the value of this evaluator in English
	 */
	private StringBuilder valueToEnglish() {
		final StringBuilder words = new StringBuilder();
		if (typeOfValue == NUMERIC) {
			Utils.appendTo(words, " is", OPER_ENGLISH[LESSER][getOper()], 
					authNumStr);
			if (MathUtils.parseDouble(toleranceStr) != 0) {
				Utils.appendTo(words, " &plusmn; ", toleranceStr);
			}
			Utils.appendTo(words, " (", 
					EMPTYCELL_ENGL[emptyCell].replaceAll("\"\"", "0"), 
					", ", NONNUMERIC_ENGL[nonnumeric], ')');
		} else {
			words.append(" does");
			if (!isPositive) words.append(" not");
			words.append(WHERE_ENGLISH[where]);
			if (ignoreCase) words.append("(ignoring case) ");
			Utils.addSpanString(words, strName, !TO_DISPLAY);
			Utils.appendTo(words, " (", EMPTYCELL_ENGL[emptyCell], ')');
		} // typeOfValue
		return words;
	} // valueToEnglish()

	/** Determines whether the response table's cells contain the indicated
	 * value.
	 * @param	response	a parsed response
	 * @param	authString	string to which to compare the response
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) {
		final String SELF = "TableVal.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		final TableQ respTableQ = (TableQ) response.parsedResp;
		final String[][] table = respTableQ.entries;
		final int numRows = table.length;
		final int[] sums = new int[2];
		final String theAuthString = authString == null ? strName : authString;
		debugPrint(SELF + "response has ", table.length, " rows and ",
				table[0].length, " columns; evaluate row = ",
				(row > 0 ? row : row == ANY ? "any"
					: row == EVERY ? "every" : "no"),
				", column = ", (column > 0 ? column : column == ANY ?
					"any" : column == EVERY ? "every" : "no"),
				", emptyCell = ",
				(emptyCell == IGNORE ? "ignore" : emptyCell == DISALLOW ?
					"disallow" : typeOfValue == NUMERIC ? "is zero" : "is \"\""),
				(typeOfValue == NUMERIC ? ", nonnumeric = "
					+ (nonnumeric == IGNORE ? "ignore" : nonnumeric == DISALLOW ?
					"disallow" : "is zero") : "")
				);
		if (row == EVERY && Utils.among(column, ANY, NO)) {
			// "If any/no column has in every row a value that is ..."
			final int numCols = table[0].length;
			final int columnOrig = column;
			for (int colNum = 1; colNum <= numCols; colNum++) {
				column = colNum;
				// column = columnOrig;
				sums[CHECKED]++;
				final TableVal newEval = 
						new TableVal(typeOfValue, getCodedData());
				newEval.operNumCells = operNumCells;
				final OneEvalResult colResult = 
						newEval.isResponseMatching(response, theAuthString);
				if (colResult.verificationFailureString != null) {
					return colResult;
				} else if (colResult.isSatisfied) {
					sums[MATCHED]++;
					if (!Utils.isEmpty(colResult.toModifyResponse)
							&& operNumCells == NOT_COUNTING
							&& colorSatisfying) {
						if (colorCellsBld.length() > 0) 
							colorCellsBld.append(';');
						colorCellsBld.append(colResult.toModifyResponse);
					} // if there are cells to color
				} // if there's a match or a verification failure 
			} // for each column
			column = columnOrig;
		} else if (row < 0) {
			// "If in every row, the value in every/nth column is "
			// or "If in any/no row, the value in any/every/no/nth column is "
			for (int rowNum = 1; rowNum <= numRows; rowNum++) {
				final Object rowResult = 
						evaluateRow(table, rowNum, theAuthString);
				if (rowResult instanceof String) {
					evalResult.verificationFailureString = (String) rowResult;
					evalResult.toModifyResponse = colorCellsBld.toString();
					return evalResult;
				} else {
					final int[] rowSums = (int[]) rowResult;
					sums[CHECKED] += rowSums[CHECKED];
					sums[MATCHED] += rowSums[MATCHED];
				} // if verification failed
			} // for each row
		} else {
			// "If in row n, the value in any/every/no/nth column is "
			if (MathUtils.inRange(row,  new int[] {1, numRows})) {
				final Object rowResult = evaluateRow(table, row, theAuthString);
				if (rowResult instanceof String) {
					evalResult.verificationFailureString = (String) rowResult;
					evalResult.toModifyResponse = colorCellsBld.toString();
					return evalResult;
				} else {
					final int[] rowSums = (int[]) rowResult;
					sums[CHECKED] += rowSums[CHECKED];
					sums[MATCHED] += rowSums[MATCHED];
				} // if verification failed
			} else {
				evalResult.verificationFailureString = "ACE could not "
						+ "evaluate cells in row " + row + (row == 0
							? "." : " because the response table has only "
							+ numRows + " rows.");
				return evalResult;
			} // if row is in range
		} // if evaluate all rows
		if (operNumCells == NOT_COUNTING) {
			if (Utils.among(NO, row, column)) {
				evalResult.isSatisfied = sums[MATCHED] == 0;
			} else if (Utils.among(ANY, row, column)) {
				evalResult.isSatisfied = sums[MATCHED] > 0;
			} else {
				evalResult.isSatisfied = sums[MATCHED] == sums[CHECKED];
				if (evalResult.isSatisfied && row == EVERY && column > 0
						&& colorSatisfying) {
					for (int rowNum = 1; rowNum <= numRows; rowNum++) {
						if (colorCellsBld.length() > 0) {
							colorCellsBld.append(';');
						} // if there are colors already
						Utils.appendTo(colorCellsBld, rowNum, ':', column);
					} // for each row
				} // if every row and one column has satisfied the condition
			} // if row, column
			if (evalResult.isSatisfied) {
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
		} else {
			final CompareNums comparer = new CompareNums(operNumCells[0]);
			evalResult.isSatisfied = 
					comparer.compare(sums[MATCHED], operNumCells[1]);
		} // if counting cells
		debugPrint(SELF + "sums[CHECKED] = ", sums[CHECKED], 
				", sums[MATCHED] = ", sums[MATCHED], 
				", returning ", evalResult.isSatisfied);
		return evalResult;
	} // isResponseMatching(Response)

	/** Determines whether a single row of the response table contains the 
	 * indicated value.
	 * @param	table	the response table
	 * @param	rowNum	the 1-based row number to examine
	 * @param	authString	string to which to compare the response
	 * @return	{numChecked, numMatches} if no verification failure has 
	 * been found; otherwise, String describing the failure
	 */
	private Object evaluateRow(String[][] table, int rowNum, 
			String authString) {
		final String SELF = "TableVal.evaluateRow: ";
		final int[] sums = new int[2];
		final String[] oneRow = table[rowNum - 1];
		final int numCols = oneRow.length;
		debugPrint(SELF + "row ", rowNum, " has ", numCols, " cells.");
		if (column < 0) { // evaluate all cells
			for (int cellNum = 1; cellNum <= numCols; cellNum++) {
				final String cellVal = oneRow[cellNum - 1].trim();
				debugPrint("Row ", rowNum, ", cell ", cellNum, 
						" has value '", cellVal, "'.");
				final Object cellResult = 
						evaluateCell(rowNum, cellNum, cellVal, authString);
				if (cellResult instanceof String) {
					return (String) cellResult;
				} else {
					final int[] matchValues = (int[]) cellResult;
					sums[CHECKED] += matchValues[CHECKED];
					sums[MATCHED] += matchValues[MATCHED];
				} // if were able to evaluate cell
			} // for each column
		} else if (MathUtils.inRange(column, new int[] {1, numCols})) {
			debugPrint("Row ", rowNum, ", cell ", column, " has",
					" untrimmed value '", oneRow[column - 1], "'.");
			final String cellVal = (oneRow[column - 1] == null
					? null : oneRow[column - 1].trim());
			final Object cellResult = 
					evaluateCell(rowNum, column, cellVal, authString);
			if (cellResult instanceof String) {
				return (String) cellResult;
			} else {
				final int[] matchValues = (int[]) cellResult;
				sums[CHECKED] += matchValues[CHECKED];
				sums[MATCHED] += matchValues[MATCHED];
			} // if were able to evaluate cell
		} else { // out of bounds
			return "ACE could not evaluate cells in column " + column
					+ (column == 0 ? "." : " because the response "
							+ "table has only " + numCols + " columns.");
		} // if column is in range
		debugPrint(SELF + "for row ", rowNum, ", numChecked = ",
				sums[CHECKED], ", numMatched = ", sums[MATCHED]);
		if (column == EVERY && sums[CHECKED] == sums[MATCHED]
				&& operNumCells == NOT_COUNTING && colorSatisfying) {
			for (int cellNum = 1; cellNum <= numCols; cellNum++) {
				if (colorCellsBld.length() > 0) colorCellsBld.append(';');
				Utils.appendTo(colorCellsBld, rowNum, ':', cellNum);
			} // for each column
		} // if every column satisfies the condition
		return sums;
	} // evaluateRow(String[][], int, String)
	
	/** Determines whether a single cell of the response table contains
	 * the indicated value.
	 * @param	rowNum	the 1-based row number being examined; for debugging and
	 * verification failure only
	 * @param	column	the 1-based column number being examined; for debugging 
	 * and verification failure only
	 * @param	cellVal	a single cell of the table
	 * @param	authString	string to which to compare the response
	 * @return	0 (nonmatch) or 1 (match) if no verification failure has 
	 * been found; otherwise, String describing the failure
	 */
	private Object evaluateCell(int rowNum, int column, String cellVal, 
			String authString) {
		final String SELF = "TableVal.evaluateCell: ";
		String myCellVal = cellVal;
		final int[] checkAndMatch = new int[2];
		if (Utils.isEmpty(myCellVal)) {
			switch (emptyCell) {
				case DISALLOW:
					debugPrint(SELF + "disallowed empty cell.");
					if (colorSatisfying) {
						colorCellsBld = Utils.getBuilder(rowNum, ':', column);
					} // if should color
					return (row > 0 ?  "The cell in  row " + row + " and"
								: "All cells in") + " column " + column
							+ " must contain a value.";
				case IGNORE:
					debugPrint(SELF + "empty cell, returning no change in "
							+ "numChecked or numMatched.");
					return checkAndMatch;
				default:
					if (emptyCell != EMPTY_STR) { // or, same, ZERO
						Utils.alwaysPrint(SELF + "bad emptyCell value");
					} else if (typeOfValue == NUMERIC) myCellVal = "0";
					break;
			} // switch emptyCell 
		} // if cell is empty
		if (typeOfValue == NUMERIC && !MathUtils.isDouble(myCellVal)) {
			switch (nonnumeric) {
				case DISALLOW:
					debugPrint(SELF + "disallowed nonnumeric cell.");
					if (colorSatisfying) {
						colorCellsBld = Utils.getBuilder(rowNum, ':', column);
					} // if should color
					return (row > 0 ? "The cell in  row " + row + " and"
								: "All cells in") + " column " + column
							+ " must contain a numerical value.";
				case IGNORE:
					debugPrint(SELF + "nonnumeric cell, returning no change "
							+ "in numChecked or numMatched.");
					return checkAndMatch;
				case ZERO: myCellVal = "0"; break;
				default:
					Utils.alwaysPrint(SELF + "bad nonnumeric");
					break;
			} // switch(nonnumeric)
		} // if cell value should be numeric but it isn't
		checkAndMatch[CHECKED]++;
		final Response cell = new Response(typeOfValue, myCellVal);
		OneEvalResult cellResult = null;
		if (typeOfValue == NUMERIC) {
			final NumberIs numIs = new NumberIs();
			numIs.setOper(getOper());
			numIs.setAuthNum(authNumStr);
			numIs.setTolerance(toleranceStr);
			cellResult = numIs.isResponseMatching(cell, null);
		} else {
			final TextContains tc = new TextContains();
			tc.setIsPositive(isPositive);
			tc.setIgnoreCase(ignoreCase);
			tc.setWhere(where);
			cellResult = tc.isResponseMatching(cell, authString);
		} // if typeOfValue
		if (cellResult.verificationFailureString != null) {
			colorCellsBld = Utils.getBuilder(rowNum, ':', column);
			return (typeOfValue == NUMERIC 
					? "The entry in row " + rowNum
						+ " and column " + column + " is not"
						+ " a number like it is supposed to be."
					: "ACE could not evaluate the entry in row " + rowNum
						+ " and column " + column + ".");
		} // if there was a verification error
		debugPrint(SELF + "for cell ", column, 
				", satisfied = ", cellResult.isSatisfied);
		if (cellResult.isSatisfied) checkAndMatch[MATCHED]++;
		if (colorSatisfying && ((row == EVERY && !cellResult.isSatisfied) 
				|| ((Utils.among(row, NO, ANY) || row > 0) 
						&& cellResult.isSatisfied 
						&& operNumCells == NOT_COUNTING))) {
			if (colorCellsBld.length() > 0) colorCellsBld.append(';');
			Utils.appendTo(colorCellsBld, rowNum, ':', column);
		} // if cell value causes condition not to be satisfied
		return checkAndMatch;
	 } // evaluateCell(int, int, String, String)

	/* *************** Get-set methods *****************/

	/** Gets whether to color cells that satisfy the condition.
	 * @return	true if should color cells that satisfy the condition
	 */
	public boolean getColorSatisfying() 		{ return colorSatisfying; } 
	/** Sets whether to color cells that satisfy the condition.
	 * @param	cs	whether to color cells that satisfy the condition
	 */
	public void setColorSatisfying(boolean cs) 	{ colorSatisfying = cs; } 

} // TableVal
