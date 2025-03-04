package com.epoch.evals.impl.chemEvals;

import chemaxon.struc.Molecule;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.EvalInterface;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the number of rings {is, is not} {=, &lt;, &gt;} <i>n</i>, or
 * if the number of compounds {with, without} {=, &lt;, &gt;} <i>m</i> rings
 * {is, is not} {=, &lt;, &gt;} <i>n</i> ... */ 
public class Rings extends CompareNumsOfNums implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** How to compare the number of rings. */
	private int ringsOper;
	/** Number of rings to compare against. */
	private int numRings;

	/** Constructor. */
	public Rings() {
		ringsOper = NOT_EQUALS;
		numRings = 0;
	} // Rings()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>countEach</code>/<code>ringsOper</code>/<code>numRings</code>/<code>molsOper</code>/<code>numMols</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public Rings(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 5) {
			countEach = Utils.isPositive(splitData[0]); // inherited from CompareNumsOfNums
			ringsOper = Utils.indexOf(SYMBOLS, splitData[1]);
			numRings = MathUtils.parseInt(splitData[2]);
			molsOper = Utils.indexOf(SYMBOLS, splitData[3]); // inherited from CompareNumsOfNums
			numMols = MathUtils.parseInt(splitData[4]); // inherited from CompareNumsOfNums
		} 
		if (splitData.length < 5 || ringsOper == -1 || molsOper == -1) {
			throw new ParameterException("Rings ERROR: "
					+ "unknown input data '" + data + "'. ");
		} // if there aren't six tokens
	} // Rings(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>countEach</code>/<code>ringsOper</code>/<code>numRings</code>/<code>molsOper</code>/<code>numMols</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(countEach ? "Y/" : "N/", SYMBOLS[ringsOper], 
				'/', numRings, '/', SYMBOLS[molsOper], '/', numMols);
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
		return (!forPermissibleSM ? toEnglish()
				: Utils.toString("have", OPER_ENGLISH[FEWER][ringsOper],
					numRings, " ring", numRings == 1 ? "" : 's'));
	} // toEnglish(boolean)

	/** Gets an English-language description of this evaluator.  
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish() {
		return (countEach
				? Utils.toString("If", getNumCompoundsEnglish(),
					getNumEnglish(ringsOper, numRings, "ring"))
				: Utils.toString("If the total number of rings is",
					OPER_ENGLISH[FEWER][ringsOper], numRings));
	} // toEnglish() 

	/** Determines whether the response contains the indicated number of rings 
	 * or the number of molecules with the indicated number of rings.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) { 
		final String SELF = "Rings.isResponseMatching: ";
		debugPrint(SELF + toEnglish());
		final OneEvalResult evalResult = new OneEvalResult();
		int rgDecrement = 0;
		if (!Utils.isEmpty(response.rGroupMols)) {
			for (final Molecule rgMol : response.rGroupMols) {
				rgDecrement += rgMol.getSSSR().length;
			}
		} // if there are R groups
		final Molecule wholeMol = response.moleculeObj.clone();
		final Molecule[] mols = (countEach ? wholeMol.convertToFrags()
				: new Molecule[] {wholeMol});
		int molsSatisfyingCt = 0;
		for (final Molecule mol : mols) {
			// ignore Jlint complaint about line above.  Raphael 11/2010
			final int ringCt = mol.getSSSR().length - rgDecrement;
			setOper(ringsOper);
			final boolean match = compare(ringCt, numRings);
			if (match) molsSatisfyingCt++;
			debugPrint(SELF + "for mol ", mol, ", ringCt = ", 
					ringCt, ", compared to = ", numRings, 
					", match = ", match);
		} // for each molecule, or once for all molecules
		setOper(molsOper);
		evalResult.isSatisfied = compare(molsSatisfyingCt, numMols);
		debugPrint(SELF + "molsSatisfyingCt = ", molsSatisfyingCt,
				", numMols = ", numMols, ", isSatisfied = ",
				evalResult.isSatisfied);
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[NUM_RINGS]; } 
	/** Gets the value of the rings operator.
	 * @return	value of the rings operator
	 */
	public int getRingsOper() 				{ return ringsOper; } 
	/** Sets the value of the rings operator.
	 * @param	op	value to which to set the rings operator
	 */
	public void setRingsOper(int op) 		{ ringsOper = op; } 
	/** Gets the value of the number of rings to compare.
	 * @return	value of the number of rings to compare
	 */
	public int getNumRings() 				{ return numRings; } 
	/** Sets the value of the number of rings to compare.
	 * @param	n	value of the number of rings to compare
	 */
	public void setNumRings(int n) 			{ numRings = n; } 

} // Mols
