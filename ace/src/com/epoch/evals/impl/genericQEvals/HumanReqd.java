package com.epoch.evals.impl.genericQEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.responses.Response;

/** If the response requires human evaluation ... */
public class HumanReqd implements EvalInterface {

	/** Constructor. */
	public HumanReqd() {
		// intentionally empty
	} // HumanReqd()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator (empty)
	 */
	public HumanReqd(String data) {
		// intentionally empty; intentionally ignores data
	} // HumanReqd(String)

	/** Gets a string representation of data (empty) that this
	 * evaluator uses to evaluate a response.  
	 * @return	the coded data
	 */
	public String getCodedData() {
		return "";
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
		return "If the response requires human evaluation";
	} // toEnglish()

	/** Determines whether the response has the indicated number of atoms of 
	 * the element.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) {
		final OneEvalResult evalResult = new OneEvalResult();
		evalResult.isSatisfied = true;	
		evalResult.humanRequired = true;	
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[HUMAN_REQD]; } 
	/** Not used.  Required by interface. 
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName)	{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 			{ return false; }

} // HumanReqd
