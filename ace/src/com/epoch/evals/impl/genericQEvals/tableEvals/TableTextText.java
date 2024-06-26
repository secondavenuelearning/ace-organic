package com.epoch.evals.impl.genericQEvals.tableEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.exceptions.ParameterException;
import com.epoch.utils.Utils;

/** If, for {any, every} row in which the cell at column <i>m</i>
 * is a reference string, the cell at column <i>n</i>
 * contains the string ...  */
public class TableTextText extends TableDependent implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	public TableTextText() {
		isNumeric = new boolean[] {false, false};
		rowOper = ANY_ROW;
		columnRef = 1;
		columnTest = 2;
		emptyCell = IGNORE;
	} // TableTextText()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>rowOper</code>/<code>columnRef</code>/<code>columnTest</code>/<code>emptyCell</code>/<code>isPositive</code>/<code>where</code>/<code>ignoreCase</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public TableTextText(String data) throws ParameterException {
		debugPrint("TableTextText.java: data = ", data);
		isNumeric = new boolean[] {false, false}; 
		final String[] splitData = data.split("/");
		if (splitData.length >= 7) {
			setValues(splitData);
		}
		if (splitData.length < 7 || where == -1 || emptyCell == -1 || rowOper == -1) {
			throw new ParameterException("TableTextText ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // TableTextText(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>rowOper</code>/<code>columnRef</code>/<code>columnTest</code>/<code>emptyCell</code>/<code>isPositive</code>/<code>where</code>/<code>ignoreCase</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(ROW_OPER[rowOper], '/', columnRef, '/',
				columnTest, '/', EMPTYCELL[emptyCell], isPositive 
				? "/Y/" : "/N/", WHERE[where], ignoreCase ? "/Y" : "/N");
	} // getCodedData()

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[TBL_TXT_TXT]; } 

} // TableTextText

