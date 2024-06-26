package com.epoch.evals.impl.chemEvals;

import chemaxon.struc.Molecule;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the total charge {is, is not} {=, &lt;, &gt;} <i>n</i>, or
 * if the number of compounds that have a total charge of
 * {=, &lt;, &gt;, &ne;, &ge;, &le;} <i>m</i>
 * {is, is not} {=, &lt;, &gt;} <i>n</i> ... */ 
public class Charge extends CompareNumsOfNums implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Charge to compare against. */
	transient private int chargeValue;
	/** How to compare the charge. */
	private int chgOper;

	/** Constructor. */
	public Charge() {
		chargeValue = 0;
		chgOper = NOT_EQUALS;
	} // Charge()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>chgOper</code>/<code>chargeValue</code>/<code>countEach</code>/<code>molsOper</code>/<code>numMols</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public Charge(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 2) {
			chgOper = Utils.indexOf(SYMBOLS, splitData[0]);
			chargeValue = MathUtils.parseInt(splitData[1]);
			if (splitData.length >= 5) {
				countEach = Utils.isPositive(splitData[2]); // inherited from CompareNumsOfNums
				molsOper = Utils.indexOf(SYMBOLS, splitData[3]); // inherited from CompareNumsOfNums
				numMols = MathUtils.parseInt(splitData[4]); // inherited from CompareNumsOfNums
			}
		}
		if (splitData.length < 2 || chgOper == -1 || molsOper == -1) {
			throw new ParameterException("Charge ERROR: unknown input data "
					+ "'" + data + "'. ");
		} // if there are not at least two tokens
	} // Charge(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>chgOper</code>/<code>chargeValue</code>/<code>countEach</code>/<code>molsOper</code>/<code>numMols</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(SYMBOLS[chgOper], '/', chargeValue,
				countEach ? "/Y/" : "/N/", SYMBOLS[molsOper], '/', numMols);
	} // getCodedData()

	/** Gets an English-language description of this evaluator.
	 * @param	qDataTexts	the text of question data of this question, if any;
	 * not used, but required by interface
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts, boolean forPermissibleSM) {
		return toEnglish(forPermissibleSM);
	} // toEnglish(String[], boolean)

	/** Gets an English-language description of this evaluator.
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(boolean forPermissibleSM) {
		return (forPermissibleSM 
				? "are uncharged and free of alkali metals and Mg"
				: toEnglish());
	} // toEnglish(boolean)

	/** Gets an English-language description of this evaluator.
	 * @return	short string describing this evaluator in English
	 */
	 public String toEnglish() {
		final StringBuilder words = Utils.getBuilder("If ",
				countEach ? Utils.getBuilder(
						getNumCompoundsEnglish(), " a total charge of ")
					: "the total charge is",
				OPER_ENGLISH[LESSER][chgOper]);
		if (chargeValue < 0) {
			Utils.appendTo(words, "&minus;", -chargeValue);
		} else {
			if (chargeValue > 0) words.append('+');
			words.append(chargeValue);
		} // if chargeValue
		return words.toString();
	 } // toEnglish()

	/** Determines whether the response has the indicated total charge.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "Charge.isResponseMatching: ";
		debugPrint(SELF + toEnglish());
		final OneEvalResult evalResult = new OneEvalResult();
		final Molecule wholeMol = response.moleculeObj.clone();
		final Molecule[] mols = (countEach ? wholeMol.convertToFrags()
				: new Molecule[] {wholeMol});
		int molsSatisfyingCt = 0;
		for (final Molecule mol : mols) {
			// ignore Jlint complaint about line above.  Raphael 11/2010
			final int actualCharge = mol.getTotalCharge();
			setOper(chgOper);
			final boolean match = compare(actualCharge, chargeValue);
			if (match) molsSatisfyingCt++;
			debugPrint(SELF + "for mol ", mol, ", actual charge = ", 
					actualCharge, ", compared to = ", chargeValue, 
					", match = ", match);
		} // for each molecule, or once for all molecules
		setOper(molsOper);
		evalResult.isSatisfied = compare(molsSatisfyingCt, numMols);
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[TOTAL_CHARGE]; } 
	/** Gets the value of the total charge to which to compare.
	 * @return	value of the total charge to which to compare
	 */
	public int getCharge() 					{ return chargeValue; }
	/** Sets the value of the total charge to which to compare.
	 * @param	n	value to which to set the total charge
	 */
	public void setCharge(int n) 			{ chargeValue = n; }
	/** Gets the value of the charge operator.
	 * @return	value of the charge operator
	 */
	public int getChgOper() 				{ return chgOper; } 
	/** Sets the value of the charge operator.
	 * @param	op	value to which to set the charge operator
	 */
	public void setChgOper(int op)	 		{ chgOper = op; } 

} // Charge
