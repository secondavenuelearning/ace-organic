package com.epoch.evals.impl.chemEvals;

import com.epoch.evals.impl.CompareNums;
import com.epoch.utils.Utils;

/** Contains objects and methods shared by evaluators that compare numbers of
 * molecules that have a numerical property. */
public class CompareNumsOfNums extends CompareNums {

	/** Whether to calculate the property of all the compounds (false) or 
	 * to count how many individual compounds have the property (true). */
	protected boolean countEach = false;
	/** How to compare the number of molecules that have
	 * the property; <code>NOT_EQUALS</code> when <code>countEach</code> 
	 * is false. */
	protected int molsOper = NOT_EQUALS;
	/** Number of molecules that have (or don't have) the property
	 * to compare against; 0 when <code>countEach</code> is false. */
	protected int numMols = 0;

	/** Constructor.  */
	protected CompareNumsOfNums() {
		// intentionally empty
	}

	/* *************** Get-set methods *****************/

	/** Gets whether to determine the property of each compound or the 
	 * whole response.
	 * @return	true if should determine the property of each compound
	 */
	public boolean getCountEach() 			{ return countEach; }
	/** Sets whether to determine the property of each compound or the 
	 * whole response.
	 * @param	ce	whether to determine the property of each compound 
	 * or the whole response
	 */
	public void setCountEach(boolean ce) 	{ countEach = ce; }
	/** Gets the value of the molecules operator.
	 * @return	value of the molecules operator
	 */
	public int getMolsOper() 				{ return molsOper; } 
	/** Sets the value of the molecules operator.
	 * @param	op	value to which to set the molecules operator
	 */
	public void setMolsOper(int op) 		{ molsOper = op; } 
	/** Gets the value of the number of molecules to compare.
	 * @return	value of the number of molecules to compare
	 */
	public int getNumMols() 				{ return numMols; } 
	/** Sets the value of the number of molecules to compare.
	 * @param	n	value of the number of molecules to compare
	 */
	public void setNumMols(int n) 			{ numMols = n; } 

	/** Returns an English phrase belonging at the beginning of a sentence that 
	 * describes the number of compounds.
	 * @return	an English phrase as a StringBuilder
	 */
	protected StringBuilder getNumCompoundsEnglish() {
		return getNumEnglish(molsOper, numMols, "compound")
				.append(numMols == 1 ? " has" : " have");
	} // getNumCompoundsEnglish()

	/** Returns an English phrase belonging at the beginning of a sentence that 
	 * describes the number of items.
	 * @param	oper	equals, not greater than, etc.
	 * @param	num	the number of the item
	 * @param	item	name of the item
	 * @return	an English phrase as a StringBuilder
	 */
	protected static StringBuilder getNumEnglish(int oper, int num, 
			String item) {
		return Utils.getBuilder(
				oper == EQUALS ? (num == 0 ? " no" : Utils.getBuilder(' ', num))
					: oper == NOT_EQUALS ? Utils.getBuilder(" other than ", num)
					: Utils.getBuilder(OPER_ENGLISH[FEWER][oper], num),
				' ', item, num == 1 ? "" : 's');
	} // getNumEnglish(int, int, String)

} // CompareNumsOfNums
