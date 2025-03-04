package com.epoch.evals.impl.chemEvals.lewisEvals;

import chemaxon.struc.MolAtom;
import com.epoch.chem.ChemUtils;
import com.epoch.constants.FormatConstants;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.lewis.LewisMolecule;
import com.epoch.responses.Response;
import com.epoch.utils.Utils;
import java.util.ArrayList;

/** If {all atoms have correct, any atoms have incorrect} formal
 * charge ... */
public class LewisFormalCharge implements EvalInterface, FormatConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	/** Whether the evaluator is satisfied by all correct (true)
	 * or any incorrect (false). */
	private boolean isPositive;

	/** Constructor. */
	public LewisFormalCharge() {
		// intentionally empty
	} // LewisFormalCharge()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>
	 */
	public LewisFormalCharge(String data) {
		// split anyway in case it's longer
		final String[] splitData = data.split("/");
		isPositive = Utils.isPositive(splitData[0]);
	} // LewisFormalCharge(String)

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
		return Utils.toString("If the formal charge of ", isPositive
				? "every atom is correct" : "any atom is incorrect");
	} // toEnglish()

	/** Determines whether the response's formal charges are correct.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified
	 * with color or a message describing an inability to evaluate the
	 * response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "LewisFormalCharge.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		// String respOrig = response.unmodified;
		try {
			final LewisMolecule respLewis = 
					(LewisMolecule) response.parsedResp;
			debugPrintMRV(SELF + "Lewis molecule: ", respLewis.getNumAtoms(),
					" atom(s):\n", respLewis.getMolecule());
			final ArrayList<MolAtom> violatingAtoms = new ArrayList<MolAtom>();
			for (int atomNum = 1; atomNum <= respLewis.getNumAtoms(); atomNum++) {
				final MolAtom atom = respLewis.getAtom(atomNum);
				final int valenceElec = ChemUtils.getValenceElectrons(atom);
				final int numUnsharedElectrons =
						respLewis.getUnsharedElectrons(atomNum);
				final int totalNumBonds =
						respLewis.getSumBondOrders(atomNum);
				final int calcFormalCharge = 
						valenceElec - totalNumBonds - numUnsharedElectrons;
				final int actualFormalCharge = atom.getCharge();
				debugPrint(SELF + "Atom ", atom, atomNum,
						": valence electrons = ", valenceElec,
						", sum bond orders =", totalNumBonds,
						", num unshared electrons = ", numUnsharedElectrons,
						", calculated formal charge = ", calcFormalCharge,
						", actual formal charge = ", actualFormalCharge);
				if (calcFormalCharge != actualFormalCharge) {
					violatingAtoms.add(atom);
				} // if the indicated and actual formal chgs don't match
			} // for each atom
			final int numViolations = violatingAtoms.size();
			debugPrint(SELF + "There are ", numViolations, 
					" formal charge violations.");
			if (!isPositive && numViolations > 0) {
				evalResult.isSatisfied = true;
				for (final MolAtom violatingAtom : violatingAtoms) {
					debugPrint(SELF + "highlighting violating atom ", 
							violatingAtom, 
							respLewis.getMolecule().indexOf(violatingAtom) + 1);
					respLewis.highlight(violatingAtom);
				} // for each violating atom
				evalResult.modifiedResponse = respLewis.toString();
				debugPrint(SELF + "modified response:\n", 
						evalResult.modifiedResponse);
			} else evalResult.isSatisfied =
					(isPositive && numViolations == 0);
		} catch (Exception e) {
			e.printStackTrace();
			evalResult.verificationFailureString = "LewisFormalCharge: "
					+ e.getMessage();
		}
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[LEWIS_FORMAL_CHGS]; } 
	/** Gets whether the evaluator is satisfied by all correct or any incorrect.
	 * @return	true if the evaluator is satisfied by all correct
	 */
	public boolean getIsPositive() 			{ return isPositive; }
	/** Sets whether the evaluator is satisfied by all correct or any incorrect.
	 * @param	pos	true if the evaluator is satisfied by all correct
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

} // LewisFormalCharge
