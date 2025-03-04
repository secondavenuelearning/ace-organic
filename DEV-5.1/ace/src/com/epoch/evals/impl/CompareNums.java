package com.epoch.evals.impl;

import com.epoch.evals.impl.implConstants.CompareNumConstants;

/** Contains objects and methods shared by evaluators that compare two numbers. */
public class CompareNums implements CompareNumConstants {

	/** Operator to compare the numbers. */
	private int oper = NOT_EQUALS;

	/** Constructor.  */
	public CompareNums() {
		// intentionally empty
	}

	/** Constructor.
	 * @param	op	value of the operator
	 */
	public CompareNums(int op) {
		oper = op;
	} // CompareNums(int)

	/** Compares two ints.
	 * @param	respNum	number derived from the response
	 * @param	authNum	author's number to which to compare
	 * @return	true if the relation defined by <code>oper</code> is satisfied
	 */
	public boolean compare(int respNum, int authNum) {
		boolean res = false;
		switch (oper) {
			case GREATER: 		res = (respNum > authNum); break;
			case LESS: 			res = (respNum < authNum); break;
			case EQUALS: 		res = (respNum == authNum); break;
			case NOT_GREATER:	res = (respNum <= authNum); break;	
			case NOT_LESS: 		res = (respNum >= authNum); break;	
			case NOT_EQUALS: 	res = (respNum != authNum); break;
			default: System.out.println("CompareNums: bad oper"); break;
		} // switch oper
		return res;
	} // compare(int, int)

	/** Compares two doubles.
	 * @param	respNum	number derived from the response
	 * @param	authNum	author's number to which to compare
	 * @param	tolerance	defines a range around <code>authNum</code> that 
	 * is considered equal to <code>authNum</code> 
	 * @return	true if the relation defined by <code>oper</code> is satisfied
	 */
	public boolean compare(double respNum, double authNum, double tolerance) {
		boolean res = false;
		switch (oper) {
			case EQUALS: 		res = (Math.abs(respNum - authNum) <= tolerance); break;
			case NOT_EQUALS: 	res = (Math.abs(respNum - authNum) > tolerance); break;
			case GREATER: 		res = (respNum > authNum + tolerance); break;
			case LESS: 			res = (respNum < authNum - tolerance); break;
			case NOT_GREATER: 	res = (respNum <= authNum + tolerance); break;
			case NOT_LESS: 		res = (respNum >= authNum - tolerance); break; 
			default: System.out.println("CompareNums: bad oper"); break;
		} // switch
		return res;
	} // compare(double, double, double)

	/* *************** Get-set methods *****************/

	/** Gets the value of the operator.
	 * @return	value of the operator
	 */
	public int getOper() 					{ return oper; } 
	/** Sets the value of the operator.
	 * @param	op	value to which to set the operator
	 */
	public void setOper(int op) 			{ oper = op; } 
	/** Not used.  Required by interface.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 	{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.  May be overridden by suubclasses.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 			{ return false; }

} // CompareNums
