package com.epoch.evals.impl.genericQEvals.tableEvals;

import com.epoch.exceptions.ParameterException;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the number of cells in {every row, row <i>n</i>}
 * and {every column, column <i>n</i>}
 * whose value contains the string 
 * {is, is not} {=, &lt;, &gt;} <i>n</i> ... */
public class TableCellTextCt extends TableTextVal { 

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Number to which to compare the number of cells. */
	private int numCells;

	/** Constructor. */
	public TableCellTextCt() {
		row = ANY;
		column = ANY;
		emptyCell = IGNORE;
		setOper(NOT_EQUALS);
		numCells = 0;
		operNumCells = new int[] {getOper(), numCells};
	} // TableCellTextCt()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>oper</code>/<code>numCells</code>/<code>isPositive</code>/<code>row</code>/<code>column</code>/<code>emptyCell</code>/<code>where</code>/<code>ignoreCase</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public TableCellTextCt(String data) throws ParameterException {
		debugPrint("TableCellTextCt.java: data = ", data);
		final String[] splitData = data.split("/");
		if (splitData.length >= 8) { 
			setOper(Utils.indexOf(SYMBOLS, splitData[0])); // inherited from CompareNums
			numCells = MathUtils.parseInt(splitData[1]);
			isPositive = Utils.isPositive(splitData[2]); // inherited from TableTextVal
			row = MathUtils.parseInt(splitData[3]); // inherited from TableTextVal
			column = MathUtils.parseInt(splitData[4]); // inherited from TableTextVal
			emptyCell = Utils.indexOf(EMPTYCELL, splitData[5]); // inherited from TableTextVal
			where = Utils.indexOf(WHERE, splitData[6]); // inherited from TableTextVal
			ignoreCase = Utils.isPositive(splitData[7]); // inherited from TableTextVal
			operNumCells = new int[] {getOper(), numCells};
		}
		if (splitData.length < 8 || where == -1 || emptyCell == -1) {
			throw new ParameterException("TableCellTextCt ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // TableCellTextCt(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>oper</code>/<code>numCells</code>/<code>isPositive</code>/<code>row</code>/<code>column</code>/<code>emptyCell</code>/<code>where</code>/<code>ignoreCase</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(SYMBOLS[getOper()], '/', numCells, '/', 
				super.getCodedData());
	} // getCodedData()

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[TABLE_CT_TXT]; }
	/** Gets the number of cells.
	 * @return	number of cells
	 */
	public int getNumCells() 				{ return numCells; } 
	/** Sets the number of cells.
	 * @param	num	value to which to set the number of cells
	 */
	public void setNumCells(int num) 		{ numCells = num; } 

} // TableCellTextCt
