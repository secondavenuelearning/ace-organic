package com.epoch.evals.impl.genericQEvals.tableEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.exceptions.ParameterException;
import com.epoch.utils.Utils;

/** If, for {any, every} row in which the cell at column <i>m</i>
 * is a reference string, the cell at column <i>n</i>
 * has a numerical value that {is, is not} {=, &lt;, &gt;} <i>n</i> ... */
public class TableNumNum extends TableDependent implements EvalInterface { 

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	public TableNumNum() {
		isNumeric = new boolean[] {true, true};
		rowOper = ANY_ROW;
		columnRef = 1;
		columnTest = 2;
		operRef = EQUALS;
		authNumRefStr = "0";
		toleranceRefStr = "";
		setOper(NOT_EQUALS);
		emptyCell = IGNORE;
		nonnumeric = IGNORE;
	}  // TableNumNum()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>rowOper</code>/<code>columnRef</code>/<code>columnTest</code>/<code>emptyCell</code>/<code>nonnumeric</code>/<code>operRef</code>/<code>authNumRef</code>/<code>toleranceRef</code>/<code>oper</code>/<code>authNum</code>/<code>tolerance</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public TableNumNum(String data) throws ParameterException {
		debugPrint("TableNumNum.java: data = ", data);
		isNumeric = new boolean[] {true, true}; 
		final String[] splitData = data.split("/");
		if (splitData.length >= 11) {
			setValues(splitData);
		}
		if (splitData.length < 11 || rowOper == -1 || emptyCell == -1
				|| nonnumeric == -1 || operRef == -1 || getOper() == -1) {
			throw new ParameterException("TableNumNum ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // TableNumNum(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>rowOper</code>/<code>columnRef</code>/<code>columnTest</code>/<code>emptyCell</code>/<code>nonnumeric</code>/<code>operRef</code>/<code>authNumRef</code>/<code>toleranceRef</code>/<code>oper</code>/<code>authNum</code>/<code>tolerance</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(ROW_OPER[rowOper], '/', columnRef, '/',
				columnTest, '/', EMPTYCELL[emptyCell], '/',
				EMPTYCELL[nonnumeric], '/', SYMBOLS[operRef], '/',
				authNumRefStr, '/', toleranceRefStr, '/', SYMBOLS[getOper()], 
				'/', authNumStr, '/', toleranceStr);
	} // getCodedData()

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[TBL_NUM_NUM]; } 
	/** Gets the value of the independent column's operator.
	 * @return	value of the independent column's operator
	 */
	public int getOperRef() 				{ return operRef; }
	/** Sets the value of the independent column's operator.
	 * @param	op	value to which to set the independent column's operator
	 */
	public void setOperRef(int op) 			{ operRef = op; }
	/** Gets the author's number for which to test <code>columnRef</code>.
	 * @return	the author's number for which to test <code>columnRef</code>
	 */
	public String getAuthNumRef() 			{ return authNumRefStr; }
	/** Sets the author's number for which to test <code>columnRef</code>.
	 * @param	num	the author's number for which to test <code>columnRef</code>
	 */
	public void setAuthNumRef(String num) 	{ authNumRefStr = num; }
	/** Gets the tolerance of the author's number for which to test
	 * <code>columnRef</code>.
	 * @return	tolerance of the author's number for which to test
	 * <code>columnRef</code>
	 */
	public String getToleranceRef() 		{ return toleranceRefStr; }
	/** Sets the tolerance of the author's number for which to test
	 * <code>columnRef</code>.
	 * @param	tol	tolerance of the author's number for which to test
	 * <code>columnRef</code>
	 */
	public void setToleranceRef(String tol) { toleranceRefStr = tol; }

} // TableNumNum
