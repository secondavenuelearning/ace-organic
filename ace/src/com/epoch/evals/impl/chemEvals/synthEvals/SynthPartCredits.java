package com.epoch.evals.impl.chemEvals.synthEvals;

import com.epoch.utils.Utils;
import com.epoch.evals.impl.chemEvals.synthEvals.synthEvalConstants.SynthPartCreditsConstants;
import java.util.LinkedHashMap;
import java.util.Map;

/** Superclass for SynthScheme and SynthSelective, allowing partial credit for
 * certain kinds of incorrect responses.
 */
public class SynthPartCredits implements SynthPartCreditsConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether the evaluator is satisfied by a selective or correct synthesis
	 * (true) or an unselective or incorrect synthesis (false). */
	protected boolean isPositive;
	/** String representing partial credit percentages keyed by kind of error. */
	transient protected String partCreditsStr = "";

	/** Converts the string representing partial credits for particular errors
	 * into a map.
	 * @return	the map of partial credits
	 */
	public Map<Integer, Integer> getPartCreditsMap() {
		final Map<Integer, Integer> partCreditsMap = 
				new LinkedHashMap<Integer, Integer>();
		if (!Utils.isEmpty(partCreditsStr)) {
			final String[] pairs = partCreditsStr.split(CREDIT_MAJOR_SEP);
			for (final String pair : pairs) {
				final String[] valueStrs = pair.split(CREDIT_MINOR_SEP);
				partCreditsMap.put(Integer.decode(valueStrs[0]),
						Integer.decode(valueStrs[1]));
			} // for each pair of error and partial credit
		} // if there are partial credits
		return partCreditsMap;
	} // getPartCreditsMap()

	/** Gets the codes of SynthErrors that an evaluator might throw.
	 * @param	evalConstant	the evaluator match code
	 * @return	codes of SynthErrors that the evaluator might throw
	 */
	public static int[] getSynthErrorCodes(int evalConstant) {
		return (evalConstant == SynthScheme.SYNTH_SCHEME 
					? SynthScheme.getSynthErrorCodes()
				: evalConstant == SynthSelective.SYNTH_SELEC 
					? SynthSelective.getSynthErrorCodes()
				: null);
	} // getSynthErrorCodes(int)

	/** Gets a description of an error for a question author.
	 * @param	errorCode	the error's code number
	 * @return	description of the error associated with the code
	 */
	public static String getSynthErrorDescription(int errorCode) {
		String descrip;
		switch (errorCode) {
			case NO_RXN_PRODUCTS:
				descrip = "A proposed reaction produces no products.";
				break;
			case LAST_NOT_RXN_PRODUCT:
				descrip = "The target compound is not among the products of "
						+ "the last reaction.";
				break;
			case NO_PRODS_IN_NEXT_STAGE:
				descrip = "None of the products of a reaction are found in the "
						+ "next stage.";
				break;
			case START_STAGE_HAS_IMPERMISSIBLE_SM:
				descrip = "A starting stage contains a compound that is not a "
						+ "permissible starting material.";
				break;
			case CONTAINS_IMPERMISSIBLE_SM:
				descrip = "A stage contains a compound that is neither a "
						+ "product of a previous reaction nor a permissible "
						+ "starting material.";
				break;
			case MINOR_PRODUCT:
				descrip = "A reaction produces a compound shown in the next "
						+ "stage, but only as a minor product of the reaction.";
				break;
			case WRONG_DIASTEREOMER:
				descrip = "A reaction produces a diastereomer of a compound "
						+ "shown in the next stage, but not the compound "
						+ "itself.";
				break;
			case WRONG_ENANTIOMER:
				descrip = "A reaction produces an enantiomer of a compound "
						+ "shown in the next stage, but not the compound "
						+ "itself.";
				break;
			case UNSPECIFIED_STEREOISOMERS:
				descrip = "A stage contains a compound with several different "
						+ "(unspecified) stereoisomeric forms, some of which "
						+ "are produced in a previous reaction, and some of "
						+ "which are not.";
				break;
			case UNSELECTIVE:
				descrip = "A reaction produces a compound found in the "
						+ "subsequent stage, but it also produces other "
						+ "nonstereoisomeric compounds in equal abundance.";
				break;
			case UNDIASTEREOSELECTIVE_SHOWN:
				descrip = "A reaction produces more than one diastereomer of "
						+ "a compound, as the student shows.";
				break;
			case UNDIASTEREOSELECTIVE_NOT_SHOWN:
				descrip = "A reaction produces more than one diastereomer of "
						+ "a compound, but the student shows only one.";
				break;
			case UNDIASTEREOSELECTIVE_NOT_DISTING:
				descrip = "A reaction produces more than one diastereomer of a "
						+ "compound, and the student does not draw the "
						+ "compound in a way that specifies a particular "
						+ "one.";
				break;
			case UNENANTIOSELECTIVE_SHOWN:
				descrip = "A reaction produces both enantiomers of a compound, "
						+ "which the student shows.";
				break;
			case UNENANTIOSELECTIVE_NOT_SHOWN:
				descrip = "A reaction produces both enantiomers of a compound, "
						+ "but the student shows only one.";
				break;
			case UNENANTIOSELECTIVE_NOT_DISTING:
				descrip = "A reaction produces both enantiomers of a compound, "
						+ "and the student does not draw the compound in a way "
						+ "that specifies a particular one.";
				break;
			case BAD_SM:
				descrip = "A stage contains a starting material that is too "
						+ "unstable to use (e.g., HCOCl or a 1-haloalcohol).";
				break;
			case TOO_MANY_REACTANTS:
				descrip =  "A stage contains too many reactants for the chosen "
						+ "reaction condition, so ACE could not calculate the "
						+ "products of the reaction.";
				break;
			default: descrip = "Unrecognized error.";
				break;
		} // switch errorCode
		return descrip;
	} // getSynthErrorDescription(int)

	/* *************** Get-set methods *****************/

	/** Gets whether the evaluator is satisfied by a correct or selective
	 * synthesis.
	 * @return	true if the evaluator is satisfied by a correct or selective
	 * synthesis
	 */
	public boolean getIsPositive() 			{ return isPositive; } 
	/** Sets whether the evaluator is satisfied by a correct or selective
	 * synthesis.
	 * @param	ip	how the evaluator is satisfied
	 */
	public void setIsPositive(boolean ip)	{ isPositive = ip; } 
	/** Gets a String representing partial credits keyed by particular errors.
	 * @return	partial credits keyed by particular errors
	 */
	public String getPartCredits()			{ return partCreditsStr; } 
	/** Sets a String representing partial credits keyed by particular errors.
	 * @param	pc	string representing partial credits for particular errors
	 */
	public void setPartCredits(String pc)	{ partCreditsStr = pc; } 
	/** Not used.  Required by interface. 
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 	{ /* intentionally empty */ }

	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() { 
		return !isPositive && !Utils.isEmpty(partCreditsStr); 
	} // getCalcGrade()

} // SynthPartCredits

