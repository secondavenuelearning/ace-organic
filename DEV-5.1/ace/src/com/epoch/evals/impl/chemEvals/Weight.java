package com.epoch.evals.impl.chemEvals;

import chemaxon.struc.Molecule;
import chemaxon.struc.PeriodicSystem;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.chemEvals.chemEvalConstants.WtConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the number of compounds that have an {exact, average} 
 * molecular weight that {is, is not} {=, &lt;, &gt;} <i>m</i> 
 * {is, is not} {=, &lt;, &gt;} <i>n</i> ... */ 
public class Weight extends CompareNumsOfNums 
		implements EvalInterface, WtConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Type of weight to measure. */
	protected int wtType; 
	/** Weight to compare against.  In String format to preserve appearance to
	 * author.  */
	transient protected String authWtStr;
	/** Tolerance when comparing response and author weights.  In String format
	 * to preserve appearance to author.  */
	transient protected String toleranceStr;
	/** How to compare the weight. */
	protected int wtOper;
	
	/** Constructor. */
	public Weight() {
		wtType = AVERAGE_WT;
		authWtStr = "78.0";
		toleranceStr = "0.2";
		wtOper = NOT_EQUALS;
	} // Weight()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>authWtStr</code>/<code>toleranceStr</code>/<code>wtOper</code>/<code>wtType</code>/<code>molsOper</code>/<code>numMols</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public Weight(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 4)  {
			authWtStr = splitData[0];
			toleranceStr = splitData[1];
			wtOper = Utils.indexOf(SYMBOLS, splitData[2]); 
			wtType = Utils.indexOf(WT_TYPE, splitData[3]);
			if (splitData.length >= 6)  {
				molsOper = Utils.indexOf(SYMBOLS, splitData[4]); // inherited from CompareNumsOfNums
				numMols = MathUtils.parseInt(splitData[5]); // inherited from CompareNumsOfNums
			}
		}
		if (splitData.length < 4 || wtOper == -1 || molsOper == -1) {
			throw new ParameterException("Weight ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // Weight(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>authWtStr</code>/<code>toleranceStr</code>/<code>wtOper</code>/<code>wtType</code>/<code>molsOper</code>/<code>numMols</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(authWtStr, '/', toleranceStr, '/',
				SYMBOLS[wtOper], '/', WT_TYPE[wtType], '/',
				SYMBOLS[molsOper], '/', numMols);
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
		final StringBuilder words = Utils.getBuilder("If",
				getNumCompoundsEnglish(), " an ", wtType == EXACT_MASS 
					? "exact mass" : "average molecular weight",
				" that is", OPER_ENGLISH[LESSER][wtOper], authWtStr);
		if (!Utils.isEmptyOrWhitespace(toleranceStr)) {
			Utils.appendTo(words, " &plusmn; ", toleranceStr);
		} // if there's a tolerance
		return words.toString();
	} // toEnglish()

	/** Determines whether the response has the indicated weight of the 
	 * indicated type.  
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) {
		final String SELF = "Weight.isResponseMatching: ";
		debugPrint(SELF, toEnglish());
		final OneEvalResult evalResult = new OneEvalResult();
		final double authWt = MathUtils.parseDouble(authWtStr);
		final double tolerance = MathUtils.parseDouble(toleranceStr);
		double rgMassCorrxn = 0.0;
		if (!Utils.isEmpty(response.rGroupMols)) {
			// correct for H occupying attachment point
			final double hMass = (wtType == AVERAGE_WT 
					? PeriodicSystem.getMass(PeriodicSystem.H)
					: PeriodicSystem.getMass(PeriodicSystem.H, 1)); // weight no. of H
			for (final Molecule rgMol : response.rGroupMols) {
				final double rgWt = (wtType == EXACT_MASS 
						? rgMol.getExactMass() : rgMol.getMass());
				rgMassCorrxn += rgWt - hMass;
			} // for each R group
		} // if there are R groups
		debugPrint(SELF + "authWt = ", authWt, ", tolerance = ", 
				tolerance, ", rgMassCorrxn = ", rgMassCorrxn);
		int molsSatisfyingCt = 0;
		final Molecule wholeMol = response.moleculeObj.clone();
		final Molecule[] mols = wholeMol.convertToFrags();
		int molNum = 0;
		setOper(wtOper);
		for (final Molecule mol : mols) {
			final double respWt = (wtType == EXACT_MASS 
					? mol.getExactMass() : mol.getMass()) - rgMassCorrxn;
			final boolean match = compare(respWt, authWt, tolerance);
			debugPrint(SELF + "mol ", ++molNum, ", ", mol, " has wt ", respWt, 
					", ", match ? "matches" : "doesn't match", " authWt.");
			if (match) molsSatisfyingCt++;
		} // for each molecule, or once for all molecules
		setOper(molsOper);
		evalResult.isSatisfied = compare(molsSatisfyingCt, numMols);
		debugPrint(SELF + "number of mols satisfying condition = ", 
				molsSatisfyingCt, ", evalResult.isSatisfied = ", 
				evalResult.isSatisfied);
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[WEIGHT]; } 
	/** Gets the type of weight (average or exact).
	 * @return	the type of weight
	 */
	public int getWtType() 					{ return wtType; } 
	/** Sets the type of weight (average or exact).
	 * @param	wtType	the type of weight
	 */
	public void setWtType(int wtType) 		{ this.wtType = wtType; } 
	/** Gets the value of the weight operator.
	 * @return	value of the weight operator
	 */
	public int getWtOper() 					{ return wtOper; } 
	/** Sets the value of the weight operator.
	 * @param	op	value to which to set the weight operator
	 */
	public void setWtOper(int op) 			{ wtOper = op; } 
	/** Gets the weight.
	 * @return	the weight
	 */
	public String getMolWeight() 			{ return authWtStr; } 
	/** Sets the weight.
	 * @param	wt	the weight
	 */
	public void setMolWeight(String wt) 	{ authWtStr = wt; } 
	/** Gets the tolerance of the weight.
	 * @return	tolerance of the weight
	 */
	public String getTolerance() 			{ return toleranceStr; } 
	/** Sets the tolerance of the weight.
	 * @param	tol	tolerance of the weight
	 */
	public void setTolerance(String tol) 	{ toleranceStr = tol; }

} // Weight
