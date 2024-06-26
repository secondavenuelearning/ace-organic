package com.epoch.evals.impl.chemEvals.synthEvals;

import chemaxon.formats.MolFormatException;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.evalConstants.OneEvalConstants;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.synthesis.synthConstants.SynthConstants;
import com.epoch.synthesis.Synthesis;
import com.epoch.responses.Response;
import com.epoch.utils.Utils;

/** If the synthesis {is, is not} exactly the same as the author's ... */
public class SynthEquals 
		implements EvalInterface, OneEvalConstants, SynthConstants {
	
	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether the evaluator is satisfied if the synthesis matches. */
	private boolean	isPositive;
	/** Whether to consider reaction conditions when comparing syntheses. */
	private boolean	considerRxnCondns;

	/** Constructor. */
	public SynthEquals() {
		isPositive = true;
		considerRxnCondns = true;
	} // SynthEquals()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>considerRxnCondns</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public SynthEquals(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		isPositive = Utils.isPositive(splitData[0]);
		considerRxnCondns = splitData.length == 1 
				|| Utils.isPositive(splitData[1]);
	} // SynthEquals(String)

	/** Gets a string representation of data (other than a molecule) that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>isPositive</code>/<code>considerRxnCondns</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive ? 'Y' : 'N', 
				considerRxnCondns ? "/Y" : "/N");
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
		return Utils.toString("If the response synthesis ",
				isPositive ? "matches" : "does not match",
				" the given synthesis (",
				considerRxnCondns ? "considering" : "ignoring",
				" reaction conditions)");
	} // toEnglish() 

	/** Determines whether the response synthesis matches the author's.
	 * @param	response	a parsed response
	 * @param	authSynMRV	MRV representation of a synthesis
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified 
	 * with color or automatically generated feedback or a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authSynMRV)  {
		final String SELF = "SynthEquals.isResponseMatching: ";
		debugPrint(SELF, toEnglish());
		final OneEvalResult evalResult = new OneEvalResult();
		final Synthesis synthesis = (Synthesis) response.parsedResp;
		try {
			final Synthesis authSyn = new Synthesis(authSynMRV);
			if (!Utils.isEmpty(response.rGroupMols)) {
				authSyn.substituteRGroups(response.rGroupMols);
			} // if there are R groups to substitute
			if (authSyn.initialized) {
				final boolean areEqual =
						synthesis.isEqualTo(authSyn, considerRxnCondns);
				evalResult.isSatisfied = isPositive == areEqual;
			} else {
				Utils.alwaysPrint(SELF 
						+ "author synthesis could not be initialized.");
				evalResult.verificationFailureString = SELF + "the author's "
						+ "synthesis could not be initialized. Please report "
						+ "this error to the webmaster: " 
						+ authSyn.errorObject.getMessage();
			} // if author synthesis was properly initialized
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "MolFormatException: ", e.getMessage());
			e.printStackTrace();
			evalResult.verificationFailureString = SELF + "threw a MolFormat"
					+ "Exception. Please report this software error "
					+ "to the webmaster: " + e.getMessage();
		}
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[SYNTH_EQUALS]; } 
	/** Gets whether the evaluator is satisfied by match or no match
	 * @return	true if the evaluator is satisfied by a match
	 */
	public boolean getIsPositive() 				{ return isPositive; } 
	/** Sets whether the evaluator is satisfied by match or no match
	 * @param	isPos	true if the evaluator is satisfied by a match
	 */
	public void setIsPositive(boolean isPos)	{ isPositive = isPos; } 
	/** Gets whether to consider reaction conditions when comparing two
	 * syntheses.
	 * @return	true if should consider reaction conditions
	 */
	public boolean getConsiderRxnCondns() 		{ return considerRxnCondns; } 
	/** Sets whether to consider reaction conditions when comparing two
	 * syntheses.
	 * @param	c	true if should consider reaction conditions
	 */
	public void setConsiderRxnCondns(boolean c) { considerRxnCondns = c; } 
	/** Not used.  Required by interface. 
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 		{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade()		 		{ return false; }

} // SynthEquals

