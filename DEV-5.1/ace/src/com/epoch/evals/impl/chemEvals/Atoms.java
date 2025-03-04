package com.epoch.evals.impl.chemEvals;

import chemaxon.struc.Molecule;
import com.epoch.chem.Formula;
import com.epoch.chem.FormulaFunctions;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.lewis.LewisMolecule;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the {largest number of contiguous, total number of} <i>X</i> atoms 
 * {is, is not} {=, &lt;, &gt;} <i>n</i>, or if the number of compounds
 * whose {largest number of contiguous, total number of} 
 * <i>X</i> atoms {is, is not} {=, &lt;, &gt;} <i>m</i> 
 * {is, is not} {=, &lt;, &gt;} <i>n</i> ... */
public class Atoms extends CompareNumsOfNums implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** How to compare the number of atoms. */
	private int atomsOper;
	/** Number of atoms to compare against. */
	private int numAtoms;
	/** Element to count. */
	private String element;
	/** Whether to count largest number of contiguous atoms. */
	private boolean contiguous;

	/** Constructor. */
	public Atoms() {
		element = "C";
		atomsOper = NOT_EQUALS;
		numAtoms = 0;
		contiguous = false;
	} // Atoms()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>atomsOper</code>/<code>numAtoms</code>/<code>element</code>/<code>contiguous</code>/<code>countEach</code>/<code>molsOper</code>/<code>numMols</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public Atoms(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 4) {
			atomsOper = Utils.indexOf(SYMBOLS, splitData[0]);
			numAtoms = MathUtils.parseInt(splitData[1]);
			element = splitData[2];
			contiguous = Utils.isPositive(splitData[3]);
			if (splitData.length >= 7) {
				countEach = Utils.isPositive(splitData[4]); // inherited from CompareNumsOfNums
				molsOper = Utils.indexOf(SYMBOLS, splitData[5]); // inherited from CompareNumsOfNums
				numMols = MathUtils.parseInt(splitData[6]); // inherited from CompareNumsOfNums
			}
		}
		if (splitData.length < 4 || atomsOper == -1) {
			throw new ParameterException("Atoms ERROR: unknown input data "
					+ "'" + data + "'. ");
		} // if there aren't four tokens
	} // Atoms(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>atomsOper</code>/<code>numAtoms</code>/<code>element</code>/<code>contiguous</code>/<code>countEach</code>/<code>molsOper</code>/<code>numMols</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(SYMBOLS[atomsOper], '/', numAtoms, '/',
				element, contiguous ? "/Y" : "/N", countEach ? "/Y/" : "/N/",
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
		return toEnglish(forPermissibleSM);
	} // toEnglish(String[], boolean)

	/** Gets an English-language description of this evaluator.
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(boolean forPermissibleSM) {
		if (!forPermissibleSM) return toEnglish();
		return Utils.toString("have", OPER_ENGLISH[FEWER][atomsOper], 
				numAtoms, contiguous ? " contiguous " : " total ",
				element, " atom", numAtoms != 1 ? 's' : "");
	} // toEnglish(boolean)

	/** Gets an English-language description of this evaluator.
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish() {
		final StringBuilder words = Utils.getBuilder("If");
		if (countEach) {
			Utils.appendTo(words, getNumCompoundsEnglish(),
				contiguous ? Utils.getBuilder(
						" a largest number of contiguous ", element, 
						" atoms that is ",
						OPER_ENGLISH[FEWER][atomsOper], numAtoms)
					: Utils.getBuilder(
						OPER_ENGLISH[FEWER][atomsOper], numAtoms,
						' ', element, " atoms"));
		} else {
			Utils.appendTo(words, " the ", contiguous 
						? "largest number of contiguous "
						: "number of ", element, " atoms is", 
					OPER_ENGLISH[FEWER][atomsOper], numAtoms);
		} // if countEach
		return words.toString();
	} // toEnglish()

	/** Determines whether the response has the indicated number
	 * or largest contiguous number of atoms of the element.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "Atoms.isResponseMatching: ";
		debugPrint(SELF, toEnglish());
		final OneEvalResult evalResult = new OneEvalResult();
		int molsSatisfyingCt = 0;
		if (response.parsedResp instanceof Formula) {
			final Formula respFormula = (Formula) response.parsedResp;
			final int actualCount = respFormula.getNumberOf(element);
			setOper(atomsOper);
			final boolean match = compare(actualCount, numAtoms);
			if (match) molsSatisfyingCt++;
		} else {
			final boolean isLewis =
					response.parsedResp instanceof LewisMolecule;
			int rgCount = 0; // count from instantiated R groups - Jay
			// handle R groups first
			if (!Utils.isEmpty(response.rGroupMols)) {
				for (final Molecule rgMol : response.rGroupMols) {
					final int thisRgCount = (contiguous 
							? FormulaFunctions.countContiguous(rgMol, element)
							: FormulaFunctions.countAtoms(rgMol, element));
					debugPrint(SELF, thisRgCount, " ", element, 
							" atoms in R group ", rgMol.getFormula());
					rgCount += thisRgCount;
					// correct for H occupying attachment point
					if ("H".equals(element)) rgCount -= 1;
				} // for each R group
				debugPrint(SELF, rgCount, " ", element, " atoms in all R groups.");
			} // if there are R groups
			final Molecule wholeMol = response.moleculeObj.clone();
			final Molecule[] mols = (countEach ? wholeMol.convertToFrags()
					: new Molecule[] {wholeMol});
			for (final Molecule mol : mols) {
				// ignore Jlint complaint about line above.  Raphael 11/2010
				final int actualCount = (contiguous 
							? FormulaFunctions.countContiguous(mol, element)
							: FormulaFunctions.countAtoms(mol, element, isLewis))
				 		- rgCount;
				setOper(atomsOper);
				final boolean match = compare(actualCount, numAtoms);
				if (match) molsSatisfyingCt++;
				debugPrint(SELF, actualCount,
						contiguous ? " is largest contiguous number of "
							: " total ", element, " atoms in ",
						isLewis ? ((LewisMolecule) response.parsedResp).getFormula()
							: mol.getFormula(), " after correcting for ",
						rgCount, " from instantiated R groups; match = ", match);
			} // for each molecule, or once for all molecules
		} // if response is molecule or formula
		setOper(molsOper);
		evalResult.isSatisfied = compare(molsSatisfyingCt, numMols);
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[NUM_ATOMS]; } 
	/** Gets the value of the number of atoms to compare.
	 * @return	value of the number of atoms to compare
	 */
	public int getNumAtoms() 				{ return numAtoms; }
	/** Sets the value of the number of atoms to compare.
	 * @param	n	value of the number of atoms to compare
	 */
	public void setNumAtoms(int n) 			{ numAtoms = n; }
	/** Gets the value of the atoms operator.
	 * @return	value of the atoms operator
	 */
	public int getAtomsOper() 				{ return atomsOper; } 
	/** Sets the value of the atoms operator.
	 * @param	op	value to which to set the atoms operator
	 */
	public void setAtomsOper(int op) 		{ atomsOper = op; } 
	/** Gets the element to count.
	 * @return	the element to count
	 */
	public String getElement() 				{ return element; }
	/** Sets the element to count.
	 * @param	elem	the element to count
	 */
	public void setElement(String elem) 	{ element = elem; }
	/** Gets whether to count largest number of contiguous atoms.
	 * @return	true if counting largest number of contiguous atoms
	 */
	public boolean getContiguous() 			{ return contiguous; }
	/** Sets whether to count largest number of contiguous atoms.
	 * @param	cont	whether to count largest number of contiguous atoms
	 */
	public void setContiguous(boolean cont)	{ contiguous = cont; }

} // Atoms
