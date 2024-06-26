package com.epoch.evals.impl.chemEvals.lewisEvals;

import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import com.epoch.chem.ChemUtils;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.lewis.LewisMolecule;
import com.epoch.responses.Response;
import com.epoch.utils.Utils;

/**  If the number of total valence electrons in the response is {correct,
 * incorrect} ... */ 
public class LewisValenceTotal implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether the evaluator is satisfied by correct sum (true) 
	 * or incorrect sum (false). */
	private boolean isPositive;

	/** Constructor. */
	public LewisValenceTotal() {
		// intentionally empty
	} // LewisValenceTotal() 

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>
	 */
	public LewisValenceTotal(String data) {
		// split anyway in case it's longer
		final String[] splitData = data.split("/");
		isPositive = Utils.isPositive(splitData[0]);
	} // LewisValenceTotal(String)

	/** Gets a string representation of data that this
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
		return Utils.toString("If the total number of valence electrons shown ",
				isPositive ? "equals" : "does not equal",
				" the calculated number");
	} // toEnglish()

	/** Determines whether the response has the indicated number of valence
	 * electrons.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) {
		final String SELF = "LewisValenceTotal.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		try {
			final LewisMolecule respLewis = 
					(LewisMolecule) response.parsedResp;
			final Molecule respMol = respLewis.getMolecule();
			int sumAtomValences = 0;
			for (final MolAtom atom : respMol.getAtomArray()) {
				sumAtomValences += ChemUtils.getValenceElectrons(atom);
			} // for each atom
			final int calcdValElectrons = 
					sumAtomValences - respLewis.getTotalCharge();
			final int actualNumElectrons = respLewis.getValenceElectrons();
			debugPrint(SELF + "sumAtomValences = ", sumAtomValences,
					", calcdValElectrons = ", calcdValElectrons,
					", actual numValElectrons = ", actualNumElectrons);
			final boolean result = (calcdValElectrons == actualNumElectrons); 
			evalResult.isSatisfied = isPositive == result;
			return evalResult;
		} catch (Exception e) {
			evalResult.verificationFailureString = 
					"Cannot verify response: " + e.getMessage();
			return evalResult;
		} // try
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[LEWIS_VALENCE_ELECS]; } 
	/** Gets whether the evaluator is satisfied by correct sum or incorrect sum.
	 * @return	true if the evaluator is satisfied by a correct sum
	 */
	public boolean getIsPositive() 			{ return isPositive; } 
	/** Sets whether the evaluator is satisfied by correct sum or incorrect sum.
	 * @param	pos	true if the evaluator is satisfied by a correct sum
	 */
	public void setIsPositive(boolean pos)	{ isPositive = pos; }
	/** Not used.  Required by interface. 
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName)	{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 			{ return false; }

} // LewisValenceTotal
