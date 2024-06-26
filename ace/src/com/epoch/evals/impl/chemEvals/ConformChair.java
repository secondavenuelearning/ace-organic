package com.epoch.evals.impl.chemEvals;

import chemaxon.struc.Molecule;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import com.epoch.chem.ChemUtils;
import com.epoch.constants.FormatConstants;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.chemEvals.chemEvalConstants.ChairConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.VerifyException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the number of {axial, equatorial} _ groups {is, is not} {=, &lt;, &gt;}
 * <i>n</i> ...  */
final public class ConformChair extends Conformations 
		implements ChairConstants, EvalInterface, FormatConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	/** Formula of the group to be counting.  May be a ChemAxon-defined shortcut
	 * group, or may be a SMILES representation with the 1st atom counting as
	 * the attachment point.  */
	private String formula;
	/** Number of groups to compare against. */
	private int number;
	/** Whether to count axial or equatorial groups. */
	private int orientation;
	/** Whether to override the author's feedback when ACE cannot determine whether 
	 * a group is axial or equatorial. */
	transient private boolean overrideForIndeterminate;

	/** Constructor. */
	public ConformChair() {
		formula = "C";
		orientation = EQUATORIAL;
		setOper(NOT_EQUALS); // inherited from CompareNums
		overrideForIndeterminate = true;
	} // ConformChair()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>orientation</code>/<code>formula</code>/<code>oper</code>/<code>number</code>/<code>overrideForIndeterminate</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public ConformChair(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 4) {
			orientation = MathUtils.parseInt(splitData[0]);
			formula = splitData[1];
			setOper(Utils.indexOf(SYMBOLS, splitData[2]));
			number = MathUtils.parseInt(splitData[3]);
			overrideForIndeterminate = (splitData.length == 4
					|| Utils.isPositive(splitData[4]));
		}
		if (splitData.length < 4 || getOper() == -1) {
			throw new ParameterException("ConformChair ERROR: unknown input data "
					+ "'" + data + "'. ");
		}
		debugPrint("ConformChair: data = ", data, ", overrideForIndeterminate = ",
				overrideForIndeterminate, ", toEnglish() = ", toEnglish());
	} // ConformChair(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>orientation</code>/<code>formula</code>/<code>oper</code>/<code>number</code>/<code>overrideForIndeterminate</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(orientation, '/', formula, '/',
				SYMBOLS[getOper()], '/',
				number, overrideForIndeterminate ? "/Y" : "/N");
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
		return Utils.toString("If the number of ",
				orientation == AXIAL ? "axial " : "equatorial ",
				formula, " groups is", OPER_ENGLISH[FEWER][getOper()], number);
	 } // toEnglish()

	/** Determines whether the response has the indicated number of indicated
	 * groups with the indicated orientation on a saturated six-membered ring.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "ConformChair.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		int axialCount = 0;
		int equatorialCount = 0;
		try { // various things can go wrong
			final Molecule respMol = response.moleculeObj.clone();
			debugPrintMRV(SELF + "Response molecule:\n", respMol);
			// Convert the specified target group (smiles) to a Molecule.
			final Molecule groupMol = ChemUtils.getSGroupMolecule(formula);
			if (groupMol == null) {
				throw new Exception("Unable to interpret " + formula
						+ "as a group. Please report this error to "
						+ "the system administrators.");
			} // if couldn't import group
			// Modify target group's attachment point to ATTACH_PT_element
			// modifyAttachmentPoint() inherited from Conformations
			modifyAttachmentPoint(groupMol);
			debugPrint(SELF + "successfully imported ", formula,
					"; will be searching for ", groupMol);
			// atom and bond numbers will be disrupted by matchGroup(), so
			// reference only MolBonds and MolAtoms, not their indices
			final SixMembRing ring = new SixMembRing(respMol);
			String indeterminateMsg = null;
			for (int ringAtomNum = 0; ringAtomNum < 6; ringAtomNum++) {
				final MolAtom ringAtom = ring.getRingAtom(ringAtomNum);
				final MolAtom[] ligands = ringAtom.getLigands();
				final MolAtom prevRingAtom = ring.getRingAtom(ringAtomNum - 1);
				final MolAtom nextRingAtom = ring.getRingAtom(ringAtomNum + 1);
				for (final MolAtom ligand : ligands) {
					// pointer equality:
					if (ligand == prevRingAtom || ligand == nextRingAtom)
						continue;
					final MolBond bondToLig = ringAtom.getBondTo(ligand);
					debugPrint(SELF + "looking to see if atom ", ligand,
							respMol.indexOf(ligand) + 1, 
							" attached to ring atom ", ringAtom,
							respMol.indexOf(ringAtom) + 1,
							" (number ", ringAtomNum + 1,
							") is a(n) ", formula, " group.");
					// matchGroup() inherited from Conformations
					if (matchGroup(respMol, bondToLig, groupMol)) {
						try {
							final int orientn = 
									ring.getOrientation(ringAtomNum, ligand);
							if (orientn == AXIAL) {
								debugPrint("   -- It is, and it is axial!");
								axialCount++;
							} else if (orientn == EQUATORIAL) {
								debugPrint("   -- It is, and it is equatorial!");
								equatorialCount++;
							} else { // unlikely; will throw error instead
								debugPrint("   -- It is, but it has "
										+ "an indeterminate orientation.");
							}
						} catch (VerifyException e) {
							if (overrideForIndeterminate) throw e;
							debugPrint("   -- It is, but it has "
									+ "an indeterminate orientation.");
							indeterminateMsg = e.getMessage();
						} // try
					} else {
						debugPrint("   -- It's not.");
					} // if we found a groupMol attached to the mapped atom
				} // for each neighbor of a marked atom
			}  // for each atom in respMol
			debugPrint(SELF + "equatorialCount = ", equatorialCount,
					", axialCount = ", axialCount);
			final int relevantCount = (orientation == EQUATORIAL
					? equatorialCount : axialCount);
			evalResult.isSatisfied = compare(relevantCount, number);
			if (evalResult.isSatisfied && indeterminateMsg != null) {
				debugPrint(SELF + "have indeterminate group, but will append "
						+ "author's feedback to autofeedback instead of "
						+ "overwriting it.");
				SixMembRing.setAutoFeedback(evalResult, indeterminateMsg);
				evalResult.verificationFailureString = null;
				evalResult.isSatisfied = true;
			}
		} catch (VerifyException e1) {
			SixMembRing.setAutoFeedback(evalResult, e1.getMessage());
		} catch (Exception e1) {
			evalResult.verificationFailureString = "ACE was unable to "
					+ "interpret your response or the question author's "
					+ "parameters.  " + e1.getMessage();
			e1.printStackTrace();
		}
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[CONFORMATION_CHAIR]; } 
	/** Gets the formula or SMILES of the group to count.
	 * @return	formula or SMILES of the group to count
	 */
	public String getFormula() 				{ return formula; }
	/** Sets the formula or SMILES of the group to count.
	 * @param	formula	formula or SMILES of the group to count
	 */
	public void setFormula(String formula) 	{ this.formula = formula; }
	/** Gets whether to count axial or equatorial groups.
	 * @return	whether to count axial or equatorial groups
	 */
	public int getOrientation() 			{ return orientation; }
	/** Sets whether to count axial or equatorial groups.
	 * @param	angle	whether to count axial or equatorial groups
	 */
	public void setOrientation(int angle) 	{ orientation = angle; }
	/** Gets the value of the number to compare.
	 * @return	value of the number to compare
	 */
	public int getNumber() 					{ return number; }
	/** Sets the value of the number to compare.
	 * @param	num	value of the number to compare
	 */
	public void setNumber(int num) 			{ number = num; }
	/** Gets whether to override the author's feedback when a group is 
	 * neither axial nor equatorial.
	 * @return	true if ACE should override the author's feedback when a 
	 * group is neither axial nor equatorial
	 */
	public boolean getOverrideFor() 		{ return overrideForIndeterminate; }
	/** Sets whether to override the author's feedback when a group is 
	 * neither axial nor equatorial.
	 * @param	ofi	whether to override the author's feedback
	 */
	public void setOverrideFor(boolean ofi) { overrideForIndeterminate = ofi; }

} // ConformChair
