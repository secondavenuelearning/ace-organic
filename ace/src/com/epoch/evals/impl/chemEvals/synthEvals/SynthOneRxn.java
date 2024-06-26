package com.epoch.evals.impl.chemEvals.synthEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.synthesis.synthConstants.SynthConstants;
import com.epoch.synthesis.SynthError;
import com.epoch.synthesis.Synthesis;
import com.epoch.utils.Utils;

/**	If {any, no} synthetic step {is the indicated reaction, 
 * contains the indicated substructures} */ 
public class SynthOneRxn implements EvalInterface, SynthConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether the evaluator is satisfied by a match. */
	private boolean isPositive; 
	/** Whether to look for whole structures or substructures. */
	private int type; 
		/** Value for type.  */
		public static final int IS = 0;
		/** Value for type.  */
		public static final int CONTAINS = 1;
		/** Database values corresponding to type. */
		transient public String[] SYMBOLS = new String[] {"is", "contains"};
	/** Name of the synthetic step; not used, but here for consistency. */
	transient private String molName = ""; // required by EvalInterface; never read
	
	/** Constructor. */
	public SynthOneRxn() {
		isPositive = true;
		type = IS;
	} // SynthOneRxn()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>type</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public SynthOneRxn(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 2) {
			isPositive = Utils.isPositive(splitData[0]);
			type = Utils.indexOf(SYMBOLS, splitData[1]);
		} else {
			throw new ParameterException("SynthOneRxn ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // SynthOneRxn(String)

	/** Gets a string representation of data (other than a molecule) that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>isPositive</code>/<code>type</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive ? "Y/" : "N/", SYMBOLS[type]);
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
		return Utils.toString("If ", isPositive ? "one" : "no",
				" reaction of the synthesis ",
				type == IS ? "is the indicated reaction"
					: "contains the indicated substructures");
	} // toEnglish()  

	/** Determines whether one step of the synthesis is a particular step.
	 * @param	response	a parsed response
	 * @param	authRxn	one synthetic step for which to search
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authRxn) {
		final OneEvalResult evalResult = new OneEvalResult();
		if (authRxn == null) {
			Utils.alwaysPrint("SynthOneRxn: author reaction is null.");
			evalResult.verificationFailureString = 
					"The author's reaction step is null. "
					+ "Please report this data error to the webmaster.";
		} else try {
			final Synthesis authSyn = new Synthesis(authRxn);
			if (!Utils.isEmpty(response.rGroupMols)) {
				authSyn.substituteRGroups(response.rGroupMols);
			} // if there are R groups to substitute
			final Synthesis respSyn = (Synthesis) response.parsedResp;
			final boolean oneStepIs = respSyn.oneStepIs(authSyn, type);
			evalResult.isSatisfied = isPositive == oneStepIs;
		} catch (SynthError e) {
			if (e.errorNumber == SynthError.IS_RXN) {
				// response contains the reaction
				evalResult.isSatisfied = isPositive;
				evalResult.modifiedResponse = e.getMessage();
			} else { // some other reason for SynthError
				Utils.alwaysPrint("SynthOneRxn: Exception when trying to "
						+ "compare structures.  ", e.getMessage());
				e.printStackTrace();
				evalResult.verificationFailureString = 
						"ACE can't read the author's reaction step. Please "
						+ "report this software error to the webmaster: "
						+ e.getMessage();
			}
		} catch (Exception e) {
			Utils.alwaysPrint("SynthOneRxn: Exception when trying to parse "
					+ "reference synthesis.  ", e.getMessage());
			e.printStackTrace();
			evalResult.verificationFailureString = 
					"ACE can't read the author's reaction step. "
					+ "Please report this software error to the webmaster: "
					+ e.getMessage();
		} // try
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[SYNTH_ONE_RXN]; } 
	/** Gets whether the evaluator is satisfied by a match or a mismatch.
	 * @return	true if the evaluator is satisfied by a match
	 */
	public boolean getIsPositive() 				{ return isPositive; } 
	/** Sets whether the evaluator is satisfied by a match or a mismatch.
	 * @param	isPos	true if the evaluator is satisfied by a match
	 */
	public void setIsPositive(boolean isPos)	{ isPositive = isPos; } 
	/** Gets whether to look for whole structures or substructures.
	 * @return	whether to look for whole structures or substructures
	 */
	public int getType() 						{ return type; } 
	/** Sets whether to look for whole structures or substructures.
	 * @param	type	whether to look for whole structures or substructures
	 */
	public void setType(int type)			 	{ this.type = type; } 
	/** Sets the molecule's name.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 		{ this.molName = molName; }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 				{ return false; }

} // SynthOneRxn

