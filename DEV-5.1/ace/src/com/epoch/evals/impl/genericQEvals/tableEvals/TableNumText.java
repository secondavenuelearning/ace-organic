package com.epoch.evals.impl.genericQEvals.tableEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.exceptions.ParameterException;
import com.epoch.utils.Utils;

/** If, for {any, every} row in which the cell at column <i>m</i>
 * has a numerical value that {is, is not} {=, &lt;, &gt;} <i>n</i>, 
 * the cell at column <i>n</i> is a reference string ... */
public class TableNumText extends TableDependent implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	public TableNumText() {
		isNumeric = new boolean[] {true, false};
		rowOper = ANY_ROW;
		columnRef = 1;
		columnTest = 2;
		emptyCell = IGNORE;
		nonnumeric = IGNORE;
		isPositive = false;
		where = IS;
		ignoreCase = true;
	}  // TableNumText()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>rowOper</code>/<code>columnRef</code>/<code>columnTest</code>/<code>emptyCell</code>/<code>nonnumeric</code>/<code>oper</code>/<code>authNum</code>/<code>tolerance</code>/<code>isPositive</code>/<code>where</code>/<code>ignoreCase</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public TableNumText(String data) throws ParameterException {
		debugPrint("TableNumText.java: data = ", data);
		isNumeric = new boolean[] {true, false}; 
		final String[] splitData = data.split("/");
		if (splitData.length >= 11) {
			setValues(splitData);
		}
		if (splitData.length < 11 
				|| rowOper == -1 || emptyCell == -1
				|| nonnumeric == -1 || getOper() == -1 || where == -1) {
			throw new ParameterException("TableNumText ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // TableNumText(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>rowOper</code>/<code>columnRef</code>/<code>columnTest</code>/<code>emptyCell</code>/<code>nonnumeric</code>/<code>oper</code>/<code>authNum</code>/<code>tolerance</code>/<code>isPositive</code>/<code>where</code>/<code>ignoreCase</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(ROW_OPER[rowOper], '/', columnRef, '/',
				columnTest, '/', EMPTYCELL[emptyCell], '/',
				EMPTYCELL[nonnumeric], '/', SYMBOLS[getOper()], '/',
				authNumStr, '/', toleranceStr, isPositive ? "/Y/" : "/N/",
				WHERE[where], ignoreCase ? "/Y" : "/N");
	} // getCodedData()

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[TBL_NUM_TXT]; } 

} // TableNumText
