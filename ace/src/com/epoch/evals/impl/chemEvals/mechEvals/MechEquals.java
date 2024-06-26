package com.epoch.evals.impl.chemEvals.mechEvals;

import chemaxon.formats.MolFormatException;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.evalConstants.OneEvalConstants;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.mechanisms.Mechanism;
import com.epoch.responses.Response;
import com.epoch.utils.Utils;

/** If the mechanism {is, is not} exactly the same as the author's ... */
public class MechEquals implements EvalInterface, OneEvalConstants {
	
	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether the evaluator is satisfied if the mechanism matches. */
	private boolean	isPositive;

	/** Constructor. */
	public MechEquals() {
		isPositive = true;
	} // MechEquals()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public MechEquals(String data) throws ParameterException {
		debugPrint("MechEquals: data = ", data);
		isPositive = Utils.isPositive(data);
	} // MechEquals(String)

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
		return Utils.toString("If the mechanism ", isPositive 
				? "matches" : "does not match", " the given mechanism");
	} // toEnglish() 

	/** Determines whether the response mechanism matches the author's.  "Match"
	 * means <i>exactly</i>; compounds and electron-flow arrowsmust be identical;
	 * no tolerance of resonance structures, omitted coproducts, etc.
	 * @param	response	a parsed response
	 * @param	authMechMRV	MRV representation of a mechanism
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified 
	 * with color or automatically generated feedback or a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authMechMRV)  {
		final String SELF = "MechEquals.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		final Mechanism mechanism = (Mechanism) response.parsedResp;
		try {
			final Mechanism authMech = new Mechanism(authMechMRV);
			if (!Utils.isEmpty(response.rGroupMols)) {
				authMech.substituteRGroups(response.rGroupMols);
			} // if there are R groups to substitute
			if (authMech.initialized) {
				evalResult.isSatisfied = 
						isPositive == mechanism.isEqualTo(authMech);
			} else {
				Utils.alwaysPrint(SELF 
						+ "author mechanism could not be initialized.");
				evalResult.verificationFailureString = SELF + "the author's "
						+ "mechanism could not be initialized. Please report "
						+ "this error to the webmaster: " 
						+ authMech.errorObject.getMessage();
			} // if author mechanism was properly initialized
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
	public String getMatchCode() 				{ return EVAL_CODES[MECH_EQUALS]; } 
	/** Gets whether the evaluator is satisfied by match or no match
	 * @return	true if the evaluator is satisfied by a match
	 */
	public boolean getIsPositive() 				{ return isPositive; } 
	/** Sets whether the evaluator is satisfied by match or no match
	 * @param	isPos	true if the evaluator is satisfied by a match
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
	public boolean getCalcGrade()		 		{ return false; }

} // MechEquals

