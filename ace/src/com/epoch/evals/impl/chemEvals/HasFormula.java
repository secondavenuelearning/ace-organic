package com.epoch.evals.impl.chemEvals;

import chemaxon.struc.Molecule;
import com.epoch.chem.Formula;
import com.epoch.chem.FormulaException;
import com.epoch.chem.FormulaFunctions;
import com.epoch.chem.chemConstants.FormulaConstants;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.lewis.LewisMolecule;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the number of compounds that {have, don't have} the given formula {is, is not}
 * {=, &lt;, &gt;} <i>n</i> ...
 * <br>or, if the total formula of the response {is, isn't} the given formula ...
 * <br>or, if the response formula {is, isn't} the given formula ... */
public class HasFormula extends CompareNumsOfNums 
		implements EvalInterface, FormulaConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** The formula to which to compare the response. The character * in the
	 * formula matches against any number, including 0.  */
	transient private String authFormulaStr;
	/** Whether to count molecules that have (true) or don't have (false) the
	 * formula; always true when <code>countEach</code> is false. */
	protected boolean withFormula = true;
	
	/** Constructor. */
	public HasFormula() {
		authFormulaStr = "C6H6";
	} // HasFormula()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>molsOper</code>/<code>numMols</code>/<code>countEach</code>/<code>authFormulaStr</code>/<code>withFormula</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public HasFormula(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 4) {
			molsOper = Utils.indexOf(SYMBOLS, splitData[0]); // inherited from CompareNums
			numMols = MathUtils.parseInt(splitData[1]); // inherited from CompareNums
			countEach = Utils.isPositive(splitData[2]); // inherited from CompareNums
			authFormulaStr = splitData[3];
			if (splitData.length >= 5) withFormula = Utils.isPositive(splitData[4]);
		} 
		if (splitData.length < 4 || molsOper == -1) {
			throw new ParameterException("HasFormula ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
		debugPrint("HasFormula.java: molsOper = ", OPER_ENGLISH[FEWER][molsOper],
				", numMols = ", numMols, ", countEach = ", countEach, 
				", formula = ", authFormulaStr, ", withFormula = ", withFormula);
	} // HasFormula(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>molsOper</code>/<code>numMols</code>/<code>countEach</code>/<code>authFormulaStr</code>/<code>withFormula</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(SYMBOLS[molsOper], '/', numMols,
				countEach ? "/Y/" : "/N/", authFormulaStr, 
				withFormula ? "/Y" : "/N");
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
		if (!forPermissibleSM) return toEnglish();
		return Utils.toString(
				molsOper == EQUALS && numMols == 0 ? "do not " : "",
				"have the formula ", authFormulaStr);
	} // toEnglish(boolean)

	/** Gets an English-language description of this evaluator.
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish() {
		// final StringBuilder words = new StringBuilder();
		return (countEach
			? Utils.toString("If", getNumCompoundsEnglish(),
					withFormula ? "" : " other than",
					" the molecular formula ", authFormulaStr)
			: Utils.toString("If the response formula is ",
					molsOper == EQUALS && numMols == 0 ? "not " : "",
					authFormulaStr));
	 } // toEnglish()

	/** Determines how many compounds in the response have the indicated 
	 * formula, or whether the response formula matches the given formula.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "HasFormula.isResponseMatching: ";
		debugPrint(SELF, toEnglish());
		final OneEvalResult evalResult = new OneEvalResult();
		try {
			int count = 0;
			if (response.parsedResp instanceof Formula) {
				final Formula respFormula = (Formula) response.parsedResp;
				final Formula authFormula = 
						new Formula(authFormulaStr, ALLOW_ASTERISK); 
						// no need to fix case
				if (authFormula.matches(respFormula)) count++;
			} else {
				int numRGroups = 0;
				Molecule[] mols = new Molecule[1];
				String modAuthFormula = authFormulaStr;
				if (response.parsedResp instanceof LewisMolecule) {
					numRGroups = -1;
					mols[0] = response.moleculeObj;
					debugPrint(SELF + "Getting formula of Lewis molecule ", 
							mols[0]);
				} else {
					final StringBuilder modBld = 
							Utils.getBuilder(authFormulaStr);
					if (!Utils.isEmpty(response.rGroupMols)) {
						// assumes one R1, one R2, etc.
						for (final Molecule rgMol : response.rGroupMols) {
							modBld.append(rgMol.getFormula());
						} // for each R group
					} // if there are R groups
					modAuthFormula = modBld.toString();
					if (countEach) {
						final Molecule wholeMol = response.moleculeObj.clone();
						mols = wholeMol.convertToFrags();
					} else mols[0] = response.moleculeObj;
				} // if is Lewis structure
				debugPrint(SELF + "formula of R groups + author's structure "
						+ "is ", modAuthFormula, "\n(extra H from ", numRGroups,
						" attachment point(s) not counted in evaluation)");
				for (final Molecule mol : mols) {
					debugPrint(SELF + "checking formula of ", mol);
					final boolean hasFormula = FormulaFunctions.hasFormula(mol, 
							modAuthFormula, numRGroups);
					if (hasFormula == withFormula) count++;
				} // for each molecule 
			} // if is a structural drawing
			setOper(molsOper);
			evalResult.isSatisfied = compare(count, numMols);
		} catch (FormulaException e) {
			evalResult.verificationFailureString = 
					SELF + "FormulaException: " + e.getMessage();
		} // try
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets whether the number of compounds with the given formula should be
	 * zero or greater than zero.
	 * @return true if the number of compounds with the given formula should be
	 * greater than zero; false if number should be zero or is ambiguous
	 */
	public boolean getIsPositive() { 
		return (numMols == 0 && Utils.among(molsOper, NOT_EQUALS, GREATER))
				|| (numMols > 0 && Utils.among(molsOper, EQUALS, NOT_LESS));
	} // getIsPositive()

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[HAS_FORMULA]; } 
	/** Gets the formula to compare against the response's formula.
	 * @return	the formula to compare against the response's formula
	 */
	public String getFormula() 				{ return authFormulaStr; }
	/** Sets the formula to compare against the response's formula.
	 * @param	formula	the formula to compare against the response's formula
	 */
	public void setFormula(String formula)	{ authFormulaStr = formula; }
	/** Gets whether to count the molecules that have or don't have the
	 * formula.
	 * @return	true if should count the molecules that have the formula
	 */
	public boolean getWithFormula() 		{ return withFormula; }
	/** Sets whether to count the molecules that have or don't have the
	 * formula.
	 * @param	w	whether to count the molecules that have or don't have
	 * the formula
	 */
	public void setWithFormula(boolean w)	{ withFormula = w; }
	/** Not used.  Required by interface.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 	{ /* intentionally empty */ }

} // HasFormula

