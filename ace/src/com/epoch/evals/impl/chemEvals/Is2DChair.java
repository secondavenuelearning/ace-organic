package com.epoch.evals.impl.chemEvals;

import chemaxon.struc.Molecule;
import com.epoch.constants.FormatConstants;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.VerifyException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the only 2D chair in the response is {identical, 
 * identical or enantiomeric} to the author's structure ...  */
public class Is2DChair extends Is implements FormatConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	/** Constructor. */
	public Is2DChair() {
		howMany = ONLY;
		flags = 0;
	} // Is2DChair()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>howMany</code>/<code>flags</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public Is2DChair(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 2) {
			howMany = MathUtils.parseInt(splitData[0]);
			flags = MathUtils.parseInt(splitData[1]);
		} else {
			throw new ParameterException("Is2DChair ERROR: unknown input data "
				+ "'" + data + "', which has " + splitData.length + " tokens");
		}
	} // Is2DChair(String)

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
		return Utils.toString("If", HOWMANY_ENGL[howMany - 1], 
				"compound in the response is a 2D chair that represents ",
				(flags & EITHER_ENANTIOMER) == 0 ? "" : "either enantiomer of ",
				molName != null ? molName : "the indicated compound");
	 } // toEnglish()

	/** Determines whether the response has the indicated number of indicated
	 * groups with the indicated orientation on a saturated six-membered ring.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "Is2DChair.isResponseMatching: ";
		OneEvalResult evalResult = new OneEvalResult();
		try { // various things can go wrong
			final Molecule respMol = response.moleculeObj.clone();
			debugPrintMRV(SELF + "Response molecule:\n", respMol);
			final SixMembRing ring = new SixMembRing(respMol);
			ring.makeWedges();
			final Response modResp = new Response(respMol);
			evalResult = super.isResponseMatching(modResp, authString);
		} catch (VerifyException e1) {
			SixMembRing.setAutoFeedback(evalResult, e1.getMessage());
		} catch (Exception e1) {
			evalResult.verificationFailureString = "ACE was unable to "
					+ "interpret your response. " + e1.getMessage();
			e1.printStackTrace();
		} // try
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[IS_2D_CHAIR]; } 

} // Is2DChair
