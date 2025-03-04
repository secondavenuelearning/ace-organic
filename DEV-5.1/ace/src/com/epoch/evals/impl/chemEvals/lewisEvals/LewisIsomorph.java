package com.epoch.evals.impl.chemEvals.lewisEvals;

import com.epoch.chem.MolCompare;
import com.epoch.chem.MolCompareException;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.lewis.LewisMolecule;
import com.epoch.responses.Response;
import com.epoch.utils.Utils;

/** If the response {is, is not} the Lewis structure ...  */ 
public class LewisIsomorph implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether the evaluator is satisfied by a match. */
	private boolean isPositive;
	/** Name of the Lewis structure molecule. */
	transient private String molName = null;

	/** Constructor. */
	public LewisIsomorph() {
		// intentionally empty
	} // LewisIsomorph()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>
	 */
	public LewisIsomorph(String data) {
		// split anyway in case it's longer
		final String[] splitData = data.split("/");
		isPositive = Utils.isPositive(splitData[0]);
	} // LewisIsomorph(String)

	/** Gets a string representation of data (other than a molecule) that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>isPositive</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return (isPositive ? "Y" : "N");
	} // getCodedData()

	/** Gets an English-language description of this evaluator.
	 * @param	qDataTexts	the text of question data of this question, if any;
	 * not used, but required by interface
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts, boolean forPermissibleSM) {
		return toEnglish();
	} // toEnglish(String[], boolean)

	/** Gets an English-language description of this evaluator.  
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish() {
		return Utils.toString("If the Lewis structure of the response is ",
				isPositive ? "exactly " : "not exactly ",
				molName == null ? "as given" : molName);
	} // toEnglish()
	
	/** Determines whether the response has the indicated Lewis structure.
	 * @param	response	a parsed response
	 * @param	authStruct	String representation of a molecule that the
	 * evaluator compares to the response
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authStruct) {
		final OneEvalResult evalResult = new OneEvalResult();
		try {
			boolean result = false;
			debugPrint("LewisIsomorph starting, response is:\n", 
					response.unmodified, "\nthe authStruct is:\n", 
					authStruct);
			final LewisMolecule lewis = (LewisMolecule) response.parsedResp;
			result = MolCompare.matchPerfectLewis(lewis, authStruct);
			evalResult.isSatisfied = isPositive == result;
			return evalResult;
	   	} catch (MolCompareException ex) {
			ex.printStackTrace();
			evalResult.verificationFailureString = ex.getMessage();
			return evalResult;
		} catch (Exception ee) {
			ee.printStackTrace();
			evalResult.verificationFailureString = 
					"LewisIsomorph " + ee.getMessage();
			return evalResult;
		} // try
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[LEWIS_ISOMORPHIC]; } 
	/** Gets whether the evaluator is satisfied by a match or a mismatch.
	 * @return	true if the evaluator is satisfied by a match
	 */
	public boolean getIsPositive() 			{ return isPositive; } 
	/** Sets whether the evaluator is satisfied by a match or a mismatch.
	 * @param	pos	true if the evaluator is satisfied by a match
	 */
	public void setIsPositive(boolean pos)	{ isPositive = pos; }
	/** Sets the molecule's name.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName)	{ this.molName = molName; }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 			{ return false; }

} // LewisIsomorph

