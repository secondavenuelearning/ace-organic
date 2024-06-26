package com.epoch.evals.impl.genericQEvals.tableEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.exceptions.ParameterException;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the numerical value of the cell at {any row, every row, no row, row <i>n</i>}
 * and {any column, every column, no column, column <i>n</i>}
 * in the response {is, is not} {=, &lt;, &gt;} <i>n</i> ... */
public class TableNumVal extends TableVal implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	public TableNumVal() {
		typeOfValue = NUMERIC;
		row = ANY;
		column = ANY;
		emptyCell = IGNORE;
		nonnumeric = IGNORE;
	}  // TableNumVal()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>oper</code>/<code>row</code>/<code>column</code>/<code>emptyCell</code>/<code>nonnumeric</code>/<code>authNum</code>/<code>tolerance</code>/<code>colorSatisfying</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public TableNumVal(String data) throws ParameterException {
		debugPrint("TableNumVal: data = ", data);
		typeOfValue = NUMERIC;
		final String[] splitData = data.split("/");
		if (splitData.length >= 7) {
			setOper(Utils.indexOf(SYMBOLS, splitData[0])); // inherited from TextAndNumbers
			row = MathUtils.parseInt(splitData[1]); // inherited from TextAndNumbers
			column = MathUtils.parseInt(splitData[2]); // inherited from TextAndNumbers
			emptyCell = Utils.indexOf(EMPTYCELL, splitData[3]); // inherited from TextAndNumbers
			nonnumeric = Utils.indexOf(EMPTYCELL, splitData[4]); // inherited from TextAndNumbers
			authNumStr = splitData[5]; // inherited from TextAndNumbers
			toleranceStr = splitData[6]; // inherited from TextAndNumbers
			colorSatisfying = splitData.length < 8
					|| Utils.isPositive(splitData[7]); // inherited from TableVal
		}
		if (splitData.length < 7
				|| getOper() == -1 || emptyCell == -1 || nonnumeric == -1) {
			throw new ParameterException("TableNumVal ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // TableNumVal(String)

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[TABLE_NUM]; } 

} // TableNumVal
