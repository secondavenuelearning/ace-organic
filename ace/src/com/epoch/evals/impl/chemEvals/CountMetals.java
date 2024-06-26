package com.epoch.evals.impl.chemEvals;

import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import com.epoch.chem.ChemUtils;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.chemEvals.chemEvalConstants.CountMetalsConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the number of compounds that
 * {have, don't have} {=, &lt;, &gt;} <i>m</i> {metal, transition-metal} atoms 
 * {is, is not} {=, &lt;, &gt;} <i>n</i> ... 
 * This evaluator is available only for defining permissible starting materials
 * in multistep synthesis questions. */
public class CountMetals extends CompareNumsOfNums 
		implements EvalInterface, CountMetalsConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** How to compare the number of metallic atoms. */
	private int metalsOper;
	/** Number of metallic atoms to compare against. */
	private int numMetals;
	/** Kind of metals to count. */
	private int metalKind;

	/** Constructor. */
	public CountMetals() {
		metalsOper = NOT_EQUALS;
		numMetals = 0;
		metalKind = ALL_METALS;
		countEach = true; // inherited from CompareNumsOfNums
	} // CountMetals()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>metalsOper</code>/<code>numMetals</code>/<code>metalKind</code>/<code>molsOper</code>/<code>numMols</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public CountMetals(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 5) {
			metalsOper = Utils.indexOf(SYMBOLS, splitData[0]);
			numMetals = MathUtils.parseInt(splitData[1]);
			metalKind = Utils.indexOf(DB_METALS, splitData[2]);
			molsOper = Utils.indexOf(SYMBOLS, splitData[3]); // inherited from CompareNumsOfNums
			numMols = MathUtils.parseInt(splitData[4]); // inherited from CompareNumsOfNums
			countEach = true; // inherited from CompareNumsOfNums
		}
		if (splitData.length < 5 || metalsOper == -1) {
			throw new ParameterException("CountMetals ERROR: unknown input data "
					+ "'" + data + "'. ");
		} // if there aren't four tokens
	} // CountMetals(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>metalsOper</code>/<code>numMetals</code>/<code>metalKind</code>/<code>molsOper</code>/<code>numMols</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(SYMBOLS[metalsOper], '/', numMetals, '/', 
				DB_METALS[metalKind], '/', SYMBOLS[molsOper], '/', numMols);
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
		return Utils.toString(forPermissibleSM ? "have no " // always
					: Utils.getBuilder(getNumCompoundsEnglish(), // never
						OPER_ENGLISH[FEWER][metalsOper], numMetals),
				ENGL_METALS[metalKind], " metal atom", 
				numMetals != 1 ? 's' : "");
	} // toEnglish(boolean)

	/** Gets an English-language description of this evaluator.
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish() {
		return toEnglish(false);
	} // toEnglish()

	/** Determines how many compounds of the response have the indicated number
	 * of metal atoms.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "CountMetals.isResponseMatching: ";
		debugPrint(SELF + toEnglish());
		final OneEvalResult evalResult = new OneEvalResult();
		int rgCount = 0; // count from instantiated R groups - Jay
		// handle R groups first
		if (!Utils.isEmpty(response.rGroupMols)) {
			for (final Molecule rgMol : response.rGroupMols) {
				final int thisRgCount = countMetalAtoms(rgMol);
				debugPrint(SELF, thisRgCount, ENGL_METALS[metalKind],
						" metal atoms in R group ", rgMol.getFormula());
				rgCount += thisRgCount;
			} // for each R group
			debugPrint(SELF, rgCount, ENGL_METALS[metalKind], 
					" metal atoms in all R groups.");
		} // if there are R groups
		final Molecule wholeMol = response.moleculeObj.clone();
		final Molecule[] mols = (countEach ? wholeMol.convertToFrags()
				: new Molecule[] {wholeMol});
		int molsSatisfyingCt = 0;
		for (final Molecule mol : mols) {
			// ignore Jlint complaint about line above.  Raphael 11/2010
			final int actualCount = countMetalAtoms(mol) - rgCount;
			setOper(metalsOper);
			final boolean match = compare(actualCount, numMetals);
			if (match) molsSatisfyingCt++;
			debugPrint(SELF, actualCount, ENGL_METALS[metalKind], 
					" metal atoms in ", mol.getFormula(), 
					" after correcting for ", rgCount, 
					" from instantiated R groups; match = ", match);
		} // for each molecule, or once for all molecules
		setOper(molsOper);
		evalResult.isSatisfied = compare(molsSatisfyingCt, numMols);
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Counts the number of metal atoms in a molecule.
	 * @param	mol	a molecule
	 * @return	the number of metal atoms in the molecule
	 */
	private int countMetalAtoms(Molecule mol) {
		int ct = 0;
		for (final MolAtom atom : mol.getAtomArray()) {
			switch (metalKind) {
				case TRANSITION: 
					if (ChemUtils.isTransitionMetal(atom)) ct++; break;
				case COLS_1_2: 
					if (ChemUtils.isCol1Or2Metal(atom)) ct++; break;
				case MAIN_GROUP: 
					if (ChemUtils.isMainGroupMetal(atom)) ct++; break;
				case NOT_TRANSITION: 
					if (ChemUtils.isNontransitionMetal(atom)) ct++; break;
				case METALS_METALLOIDS: 
					if (ChemUtils.isMetalloid(atom)
							|| ChemUtils.isMetal(atom)) ct++; break;
				default: // ALL_METALS 
					if (ChemUtils.isMetal(atom)) ct++; break;
			} // if is appropriate kind of metal
		} // for each atom
		return ct;
	} // countMetalAtoms(Molecule)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[COUNT_METALS]; } 
	/** Gets the value of the number of metal atoms to compare.
	 * @return	value of the number of metal atoms to compare
	 */
	public int getNumMetals() 				{ return numMetals; }
	/** Sets the value of the number of metal atoms to compare.
	 * @param	n	value of the number of metal atoms to compare
	 */
	public void setNumMetals(int n) 		{ numMetals = n; }
	/** Gets the value of the metal atoms operator.
	 * @return	value of the metal atoms operator
	 */
	public int getMetalsOper() 				{ return metalsOper; } 
	/** Sets the value of the metal atoms operator.
	 * @param	op	value to which to set the metal atoms operator
	 */
	public void setMetalsOper(int op) 		{ metalsOper = op; } 
	/** Gets the kind of metal to count.
	 * @return	the kind of metal to count
	 */
	public int getMetalKind() 				{ return metalKind; }
	/** Sets the kind of metal to count.
	 * @param	kind	the kind of metal to count
	 */
	public void setMetalKind(int kind) 		{ metalKind = kind; }

} // CountMetals
