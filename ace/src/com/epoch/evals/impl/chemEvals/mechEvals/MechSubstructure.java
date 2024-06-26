package com.epoch.evals.impl.chemEvals.mechEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.mechanisms.Mechanism;
import com.epoch.mechanisms.mechConstants.MechConstants;
import com.epoch.mechanisms.MechSubstructSearch;
import com.epoch.mechanisms.MechUtils;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the student's response contains the author-provided substructure
 * and electron-flow arrows ...  */
public class MechSubstructure implements EvalInterface, MechConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether the evaluator is satisfied by a match or a mismatch. */
	private boolean	isPositive;
	/** Flags that determine what aspects of the response (charge,
	 * radicals, isotopes) to ignore when looking for the substructure. */
	private int ignoreFlags;
	
	/** Constructor. */
	public MechSubstructure() { // default values
		isPositive = true;
		ignoreFlags = ALL_MASK;
	} // MechSubstructure()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>flag</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public MechSubstructure(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		isPositive = Utils.isPositive(splitData[0]);
		ignoreFlags = (splitData.length == 1 ? ALL_MASK
				: MathUtils.parseInt(splitData[1]));
	} // MechSubstructure(String)

	/** Gets a string representation of data (other than a molecule) that this
	 * evaluator uses to evaluate a response.  Format:<br>
	 * <code>isPositive</code>/<code>flag</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive? "Y/" : "N/", ignoreFlags);
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
		return Utils.toString("If the response ",
				isPositive ? "contains" : "does not contain",
				" the substructure and electron-flow arrows");
	} // toEnglish()

	/** Determines whether the response contains the indicated substructure and
	 * electron-flow arrows.
	 * @param	response	a parsed response
	 * @param	materials	String representation of substructure and
	 * electron-flow arrows for which to search
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified
	 * with color or a message describing an inability to evaluate the
	 * response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String materials) {
		final String SELF = "MechSubstructure.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		boolean isMatch = false;
		final Mechanism mechanism = (Mechanism) response.parsedResp;
		final MechSubstructSearch mechSub =
				new MechSubstructSearch(mechanism, materials);
		try {
			final int[] match = mechSub.hasSubstructure(ignoreFlags);
				// hasSubstructure() returns match indices AND matching flow
				// number (not object index) if it finds a match
			isMatch = !Utils.isEmpty(match);
			if (isMatch && isPositive) {
				debugPrint(SELF + "match found in stage ",
						match[match.length - 1] + 1);
				final MechUtils util = new MechUtils(mechanism);
				evalResult.modifiedResponse =
						util.colorStage(match[match.length - 1]);
				// ignore Jlint complaint about line above.  Raphael 11/2010
			}
		} catch (Exception e) {
			Utils.alwaysPrint(SELF + "exception thrown, msg = ",
					e.getMessage());
			e.printStackTrace();
		}
		debugPrint(SELF + "author structure & flow arrows ",
				(isMatch ? "" : "not "), "found in student response");
		evalResult.isSatisfied = isPositive == isMatch;
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[MECH_SUBSTRUCTURE]; } 
	/** Gets whether the evaluator is satisfied by a match or a mismatch.
	 * @return	true if the evaluator is satisfied by a match
	 */
	public boolean getIsPositive() 				{ return isPositive; }
	/** Sets whether the evaluator is satisfied by a match or a mismatch.
	 * @param	isPos	true if the evaluator is satisfied by a match
	 */
	public void setIsPositive(boolean isPos)	{ isPositive = isPos; }
	/** Gets flags that determine what aspects of the response
	 * to ignore when looking for the substructure.
	 * @return	the flags
	 */
	public int getIgnoreFlags() 				{ return ignoreFlags; }
	/** Sets flags that determine what aspects of the response
	 * to ignore when looking for the substructure.
	 * @param	ignoreFlags	the flags
	 */
	public void setIgnoreFlags(int ignoreFlags) { this.ignoreFlags = ignoreFlags; }
	/** Not used.  Required by interface.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 		{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 				{ return false; }

} // MechSubstructure

