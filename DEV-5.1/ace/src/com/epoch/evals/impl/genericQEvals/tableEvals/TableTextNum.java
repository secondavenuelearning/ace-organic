package com.epoch.evals.impl.genericQEvals.tableEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.exceptions.ParameterException;
import com.epoch.utils.Utils;

/** If, for {any, every} row in which the cell at column <i>m</i>
 * is a reference string, the cell at column <i>n</i>
 * has a numerical value that {is, is not} {=, &lt;, &gt;} <i>n</i> ... */
public class TableTextNum extends TableDependent implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	public TableTextNum() {
		isNumeric = new boolean[] {false, true};
		rowOper = ANY_ROW;
		columnRef = 1;
		columnTest = 2;
		emptyCell = IGNORE;
		nonnumeric = IGNORE;
		ignoreCase = true;
	}  // TableTextNum()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>rowOper</code>/<code>columnRef</code>/<code>columnTest</code>/<code>emptyCell</code>/<code>nonnumeric</code>/<code>oper</code>/<code>authNum</code>/<code>tolerance</code>/<code>ignoreCase</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public TableTextNum(String data) throws ParameterException {
		debugPrint("TableTextNum.java: data = ", data);
		isNumeric = new boolean[] {false, true}; 
		final String[] splitData = data.split("/");
		if (splitData.length >= 9) { 
			setValues(splitData);
		}
		if (splitData.length < 9 
				|| rowOper == -1 || emptyCell == -1
				|| nonnumeric == -1 || getOper() == -1) {
			throw new ParameterException("TableTextNum ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // TableTextNum(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>rowOper</code>/<code>columnRef</code>/<code>columnTest</code>/<code>emptyCell</code>/<code>nonnumeric</code>/<code>oper</code>/<code>authNum</code>/<code>tolerance</code>/<code>ignoreCase</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(ROW_OPER[rowOper], '/', columnRef, '/',
				columnTest, '/', EMPTYCELL[emptyCell], '/',
				EMPTYCELL[nonnumeric], '/', SYMBOLS[getOper()], '/',
				authNumStr, '/', toleranceStr, ignoreCase ? "/Y" : "/N");
	} // getCodedData()

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[TBL_TXT_NUM]; } 

} // TableTextNum
