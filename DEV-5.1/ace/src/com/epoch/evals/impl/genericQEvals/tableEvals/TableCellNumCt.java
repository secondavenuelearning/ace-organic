package com.epoch.evals.impl.genericQEvals.tableEvals;

import com.epoch.exceptions.ParameterException;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the number of cells in {every row, row <i>n</i>}
 * and {every column, column <i>n</i>}
 * whose value {is, is not} {=, &lt;, &gt;} <i>n</i>
 * {is, is not} {=, &lt;, &gt;} <i>n</i> ... */
public class TableCellNumCt extends TableNumVal { 

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** How to compare the number of cells with the author's number. */
	private int operCells;
	/** Number to which to compare the number of cells. */
	private int numCells;

	/** Constructor. */
	public TableCellNumCt() {
		row = EVERY;
		column = EVERY;
		emptyCell = IGNORE;
		nonnumeric = IGNORE;
		operCells = NOT_EQUALS;
		numCells = 0;
		operNumCells = new int[] {operCells, numCells};
	}  // TableCellNumCt()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>operCells</code>/<code>numCells</code>/<code>oper</code>/<code>row</code>/<code>column</code>/<code>emptyCell</code>/<code>nonnumeric</code>/<code>authNum</code>/<code>tolerance</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public TableCellNumCt(String data) throws ParameterException {
		debugPrint("TableCellNumCt: data = ", data);
		final String[] splitData = data.split("/");
		if (splitData.length >= 9) {
			operCells = Utils.indexOf(SYMBOLS, splitData[0]);
			numCells = MathUtils.parseInt(splitData[1]);
			setOper(Utils.indexOf(SYMBOLS, splitData[2])); // inherited from TableNumVal
			row = MathUtils.parseInt(splitData[3]); // inherited from TableNumVal
			column = MathUtils.parseInt(splitData[4]); // inherited from TableNumVal
			emptyCell = Utils.indexOf(EMPTYCELL, splitData[5]); // inherited from TableNumVal
			nonnumeric = Utils.indexOf(EMPTYCELL, splitData[6]); // inherited from TableNumVal
			authNumStr = splitData[7]; // inherited from TableNumVal
			toleranceStr = splitData[8]; // inherited from TableNumVal
			operNumCells = new int[] {operCells, numCells};
		}
		if (splitData.length < 9 || getOper() == -1 
				|| emptyCell == -1 || nonnumeric == -1) {
			throw new ParameterException("TableCellNumCt ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // TableCellNumCt(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>operCells</code>/<code>numCells</code>/<code>oper</code>/<code>row</code>/<code>column</code>/<code>emptyCell</code>/<code>nonnumeric</code>/<code>authNum</code>/<code>tolerance</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(SYMBOLS[operCells], '/', numCells, '/', 
				super.getCodedData());
	} // getCodedData()

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[TABLE_CT_NUM]; } 
	/** Gets the value of the operator for the number of cells.
	 * @return	value of the operator for the number of cells
	 */
	public int getOperCells() 				{ return operCells; } 
	/** Sets the value of the operator for the number of cells.
	 * @param	op	value to which to set the operator for the number of cells
	 */
	public void setOperCells(int op) 		{ operCells = op; } 
	/** Gets the number of cells.
	 * @return	number of cells
	 */
	public int getNumCells() 				{ return numCells; } 
	/** Sets the number of cells.
	 * @param	num	value to which to set the number of cells
	 */
	public void setNumCells(int num) 		{ numCells = num; } 

} // TableCellNumCt
