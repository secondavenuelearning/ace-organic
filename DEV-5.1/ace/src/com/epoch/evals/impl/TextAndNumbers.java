package com.epoch.evals.impl;

/** Contains fields and methods shared by <code>TextContains</code>,
<code>NumberIs</code>, and <code>Table*</code> classes. */
public class TextAndNumbers extends CompareNums {

	/** Constructor. */
	protected TextAndNumbers() {
		// intentionally empty
	}

/* ************ Fields and values for text methods *************** */

	/** Whether the evaluator is satisfied by a match or a mismatch. */
	protected boolean isPositive;
	/** For toEnglish(), string for which to search (possibly shortened). */
	protected String strName;
	/** Location of string in student's trimmed response. */
	protected int where;
	/** Policy on letter case. */
	protected boolean ignoreCase;
	
	/* *************** Get-set methods *****************/

	/** Gets whether the evaluator is satisfied by a match or a mismatch.
	 * @return	true if the evaluator is satisfied by a match
	 */
	public boolean getIsPositive() 			{ return isPositive; } 
	/** Sets whether the evaluator is satisfied by a match or a mismatch.
	 * @param	pos	true if the evaluator is satisfied by a match
	 */
	public void setIsPositive(boolean pos)	{ isPositive = pos; } 
	/** Gets whether case should be ignored.
	 * @return	true if case should be ignored
	 */
	public boolean getIgnoreCase() 			{ return ignoreCase; } 
	/** Sets whether case should be ignored.
	 * @param	ign	true if case should be ignored
	 */
	public void setIgnoreCase(boolean ign)	{ ignoreCase = ign; } 
	/** Gets where the string should appear in the response. 
	 * @return	where the string should appear in the response
	 */
	public int getWhere() 					{ return where; } 
	/** Sets where the string should appear in the response. 
	 * @param	where	where the string should appear in the response
	 */
	public void setWhere(int where) 		{ this.where = where; } 

/* ************ Fields and methods only for numerical classes *************** */

	/** Number against which to compare. */
	transient protected String authNumStr = "0";
	/** Exponent of number against which to compare. */
	transient protected String authExponentStr = "";
	/** Tolerance of one of the numbers when comparing it to the other. */
	transient protected String toleranceStr = "";

	/* *************** Get-set methods *****************/

	/** Gets the author's number.
	 * @return	the author's number
	 */
	public String getAuthNum() 				{ return authNumStr; } 
	/** Sets the author's number.
	 * @param	num	the author's number
	 */
	public void setAuthNum(String num) 		{ authNumStr = num; } 
	/** Gets the exponent of the author's number.
	 * @return	exponent of the author's number
	 */
	public String getAuthExponent() 		{ return authExponentStr; } 
	/** Sets the exponent of the author's number.
	 * @param	num	exponent of the author's number
	 */
	public void setAuthExponent(String num) { authExponentStr = num; } 
	/** Gets the tolerance of the author's number.
	 * @return	tolerance of the author's number
	 */
	public String getTolerance() 			{ return toleranceStr; } 
	/** Sets the tolerance of the author's number.
	 * @param	tol	tolerance of the author's number
	 */
	public void setTolerance(String tol) 	{ toleranceStr = tol; } 

/* ************ Fields and methods only for table classes *************** */

	/** The 1-based row of the cell, or negative for ANY, EVERY, or NO row. */
	protected int row;
	/** The 1-based column of the cell, or negative for ANY, EVERY, or NO 
	 * column. */
	protected int column;
	/** Policy on an empty table cell. */
	protected int emptyCell;
	/** Policy on a nonnumeric table cell. */
	protected int nonnumeric;
		
	/* *************** Get-set methods *****************/

	/** Gets the 1-based cell row.
	 * @return	the cell row; -1 for any, -2 for every, -3 for none
	 */
	public int getRow() 						{ return row; } 
	/** Sets the 1-based cell row.
	 * @param	row	the 1-based cell row; -1 for any, -2 for every, -3 for none
	 */
	public void setRow(int row) 				{ this.row = row; } 
	/** Gets the 1-based cell column.
	 * @return	the cell column; -1 for any, -2 for every, -3 for none
	 */
	public int getColumn() 						{ return column; } 
	/** Sets the 1-based cell column.
	 * @param	column	the 1-based cell column; -1 for any, -2 for every, -3
	 * for none
	 */
	public void setColumn(int column)			{ this.column = column; } 
	/** Gets the policy on empty cells.
	 * @return	the policy on empty cells
	 */
	public int getEmptyCell() 					{ return emptyCell; } 
	/** Sets the policy on empty cells.
	 * @param	emptyCell	the policy on empty cells
	 */
	public void setEmptyCell(int emptyCell)		{ this.emptyCell = emptyCell; } 
	/** Gets the policy on nonnumeric cells.
	 * @return	the policy on nonnumeric cells
	 */
	public int getNonnumeric() 					{ return nonnumeric; } 
	/** Sets the policy on nonnumeric cells.
	 * @param	nonnumeric	the policy on nonnumeric cells
	 */
	public void setNonnumeric(int nonnumeric)	{ this.nonnumeric = nonnumeric; } 

} // TextAndNumbers
