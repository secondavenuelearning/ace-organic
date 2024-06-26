package com.epoch.evals.impl.genericQEvals.tableEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.exceptions.ParameterException;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the value of the string in the cell at 
 * {any row, every row, no row, row <i>n</i>}
 * and {any column, every column, no column, column <i>n</i>}
 * in the response ...  */
public class TableTextVal extends TableVal implements EvalInterface { 

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	public TableTextVal() {
		typeOfValue = TEXT;
		row = ANY;
		column = ANY;
		emptyCell = IGNORE;
	} // TableTextVal()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>row</code>/<code>column</code>/<code>emptyCell</code>/<code>where</code>/<code>ignoreCase</code>/<code>colorSatisfying</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public TableTextVal(String data) throws ParameterException {
		debugPrint("TableTextVal: data = ", data);
		typeOfValue = TEXT;
		final String[] splitData = data.split("/");
		if (splitData.length >= 6) { 
			isPositive = Utils.isPositive(splitData[0]); // inherited from TextAndNumbers
			row = MathUtils.parseInt(splitData[1]); // inherited from TextAndNumbers
			column = MathUtils.parseInt(splitData[2]); // inherited from TextAndNumbers
			emptyCell = Utils.indexOf(EMPTYCELL, splitData[3]); // inherited from TextAndNumbers
			where = Utils.indexOf(WHERE, splitData[4]); // inherited from TextAndNumbers
			ignoreCase = Utils.isPositive(splitData[5]); // inherited from TextAndNumbers
			colorSatisfying = splitData.length < 7
					|| Utils.isPositive(splitData[6]); // inherited from TableVal
		}
		if (splitData.length < 6 || where == -1 || emptyCell == -1) {
			throw new ParameterException("TableTextVal ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // TableTextVal(String)

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[TABLE_TEXT]; }
	/** Required by interface.  Sets a possibly shortened version of the 
	 * test string for display.
	 * @param	str	the test string (maybe shortened)
	 */
	public void setMolName(String str) 		{ strName = str; } 

} // TableTextVal
