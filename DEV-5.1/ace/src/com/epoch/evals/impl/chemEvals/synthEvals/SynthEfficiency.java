package com.epoch.evals.impl.chemEvals.synthEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.synthesis.SynthError;
import com.epoch.synthesis.Synthesis;
import com.epoch.utils.Utils;

/** If any product in the synthesis is an acceptable starting material ...  */ 
public class SynthEfficiency implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether any product of the synthesis is an acceptable starting material
	 * (true) or every compound is not (false). */
	private boolean	isPositive; 

	/** Constructor. */
	public SynthEfficiency() {
		isPositive = false;
	} // SynthEfficiency()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public SynthEfficiency(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		isPositive = Utils.isPositive(splitData[0]);
	} // SynthEfficiency(String)

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
		return Utils.toString("If ", isPositive ? "any" : "none",
				" of the compounds in each step produced by"
					+ " a reaction of a previous step is a "
					+ "permissible starting material");
	} // toEnglish() 

	/** Determines whether the response reactions produce any compounds that are
	 * permissible starting materials.  
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified with 
	 * color or a message describing an inability to evaluate the response
	 * because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) {
		final OneEvalResult evalResult = new OneEvalResult();
		boolean noRespProdsArePermissibleSMs = true;
		final Synthesis synthesis = (Synthesis) response.parsedResp;
		try {
		 	noRespProdsArePermissibleSMs = 
					synthesis.noRespProductsArePermissibleSMs();
		} catch (SynthError e) {
			noRespProdsArePermissibleSMs = false;
			if (isPositive) { // = any prods could be SMs 
				evalResult.isSatisfied = true;
				evalResult.modifiedResponse = e.getMessage();
				evalResult.autoFeedback = new String[] {e.getErrorFeedback()};
				return evalResult;
			} // if isPositive
			else Utils.alwaysPrint("SynthEfficiency: SynthError generated, "
					+ "but not throwing it because isPositive is false");
		} catch (Exception e) {
			Utils.alwaysPrint("SynthEfficiency:: exception: ", e.getMessage());
			e.printStackTrace();
			evalResult.verificationFailureString = 
					"SynthEfficiency: noRespProductsArePermissibleSMs() "
					+ "threw an exception. "
					+ "Please report this software error to the webmaster: "
					+ e.getMessage();
			return evalResult;
		}
		evalResult.isSatisfied = isPositive != noRespProdsArePermissibleSMs; 
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[SYNTH_SM_MADE]; } 
	/** Gets whether any product of the synthesis is an acceptable starting material
	 * or every compound is not. 
	 * @return	true if any product is an acceptable starting material
	 */
	public boolean getIsPositive() 				{ return isPositive; } 
	/** Sets whether any product of the synthesis is an acceptable starting material
	 * or every compound is not. 
	 * @param	isPos	how the evaluator is satisfied
	 */
	public void setIsPositive(boolean isPos) 	{ isPositive = isPos; } 
	/** Not used.  Required by interface. 
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 		{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 				{ return false; }

} // SynthEfficiency

