package com.epoch.evals.impl.chemEvals;

import chemaxon.struc.Molecule;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.VectorMath;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** If two specified groups {are, are not} {anti, gauche, staggered, eclipsed} ...  */
public class ConformBond extends Conformations implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Formula of one of the groups whose dihedral angle is being measured.
	 * May be a ChemAxon-defined shortcut group, or may be a SMILES
	 * representation with the 1st atom counting as the attachment point.  */
	private String formula1 = "";
	/** Formula of the other group whose dihedral angle is being measured.
	 * May be a ChemAxon-defined shortcut group, or may be a SMILES
	 * representation with the 1st atom counting as the attachment point.  */
	private String formula2	= "";
	/** Nature of dihedral angle: anti, gauche, etc. */
	private int	groupRelationship;
		/** Value for groupRelationship.  */
		public static final int ANTI = 1;
		/** Value for groupRelationship.  */
		public static final int GAUCHE = 2;
		/** Value for groupRelationship.  */
		public static final int STAGGERED = 3;
		/** Value for groupRelationship.  */
		public static final int ECLIPSED = 4;
		/** Value for groupRelationship.  */
		public static final int NOTANTI = 5;
		/** Value for groupRelationship.  */
		public static final int NOTGAUCHE = 6;
		/** Value for groupRelationship.  */
		public static final int NOTSTAGGERED = 7;
		/** Value for groupRelationship.  */
		public static final int NOTECLIPSED = 8;
		/** Database values for groupRelationship. */
		public static final String[] DB_VALUES = 
				new String[] {"", "anti", "gauche", "staggered", "eclipsed",
					"not anti", "not gauche", "not staggered", "not eclipsed"};

	/** Constructor. */
	public ConformBond() {
		groupRelationship = ANTI;
	} // ConformBond()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>formula1</code>/<code>formula2</code>/<code>groupRelationship</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public ConformBond(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 3) {
			formula1 = splitData[0];
			formula2 = splitData[1];
			groupRelationship = MathUtils.parseInt(splitData[2], -1);
			if (groupRelationship == -1) {
				groupRelationship = Utils.indexOf(DB_VALUES, splitData[2]);
				if (groupRelationship == -1) {
					throw new ParameterException("ConformBond ERROR: "
							+ "unknown input data '" + data + "'. ");
				} // if groupRelationship not found
			} // if groupRelationship not integral
		} else {
			throw new ParameterException("ConformBond ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // ConformBond(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>formula1</code>/<code>formula2</code>/<code>groupRelationship</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(formula1, '/', formula2, '/',
				DB_VALUES[groupRelationship]);
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
		return Utils.toString("If the ", formula1, " group and the ", 
				formula2, " group are ", DB_VALUES[groupRelationship]);
	} // toEnglish()

	/** Determines whether the response contains the two indicated groups in the
	 * indicated conformational relationship about a marked bond.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing an
	 * inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "ConformBond.isResponseMatching: ";
		OneEvalResult evalResult = new OneEvalResult();
		try { // various things can go wrong
			debugPrint(SELF, formula1, " vs. ", formula2, ": ", 
					DB_VALUES[groupRelationship], "?");
			final Molecule respMol = response.moleculeObj.clone();
			MolAtom atomB = null, atomC = null; // the two marked atoms
			int numMappedAtoms = 0;
			for (final MolAtom atom : respMol.getAtomArray()) {
				// Deselect all atoms in the response molecule; we need to
				// select ones ourselves as we inspect particular bonds.
				atom.setSelected(false);
				if (atom.getAtomMap() != 0) {
					numMappedAtoms++;
					if (atomC == null) {
						if (atomB == null) atomB = atom;
						else atomC = atom;
					} // if atomC is null
				} // if we have found a mapped atom
			} // for each atom in respMol
			if (numMappedAtoms != 2) {
				debugPrint(SELF + "The number of mapped atoms is ",
						numMappedAtoms, ", not 2.");
				evalResult.verificationFailureString = "Please follow "
						+ "the instructions for drawing your response.";
				return evalResult;
			} // if number of mapped atoms is not 2
			// Verify that atomB and atomC are connected by a single bond
			final MolBond bondBC = atomB.getBondTo(atomC);
			if (bondBC == null) {
				debugPrint(SELF + "The two mapped atoms share no bond.");
				evalResult.verificationFailureString =	
						"You have changed the molecule: The two "
						+ "skeletal atoms are no longer connected by a bond.";
				return evalResult;
			} // if bond is null
			if (bondBC.getType() != 1) {	
				debugPrint(SELF + "The two mapped atoms are linked but "
						+ "not by a single bond");
				evalResult.verificationFailureString =
						"The skeletal bond must be a single bond.";
				return evalResult;
			} // if bond is not single
			debugPrint(SELF + "Good: The two mapped atoms, with maps ",
					atomB.getAtomMap(), " and ", atomC.getAtomMap(),
					", share a single bond");
			// Convert the specified target groups (smiles) to Molecules.
			final Molecule groupMol1 = ChemUtils.getSGroupMolecule(formula1);
			final Molecule groupMol2 = ChemUtils.getSGroupMolecule(formula2);
			if (groupMol1 == null || groupMol2 == null) {
				throw new Exception("Unable to interpret either " + formula1
						+ " or " + formula2 + "as a group.  Please report "
						+ "this error to the system administrators.");
			} // if couldn't import the fragments
			// Modify target groups' attachment point to ATTACH_PT_element
			// modifyAttachmentPoint() inherited from Conformations
			modifyAttachmentPoint(groupMol1);
			modifyAttachmentPoint(groupMol2);
			debugPrint(SELF + "Successfully imported ", formula1, " and ", 
					formula2, "; will be searching for ", groupMol1, 
					" and ", groupMol2);
			// Compute lists of atoms attached to atomB and atomC that match
			// groups 1 and 2
			final List<MolAtom> ligListG1B = new ArrayList<MolAtom>();
			final List<MolAtom> ligListG2B = new ArrayList<MolAtom>();
			final List<MolAtom> ligListG1C = new ArrayList<MolAtom>();
			final List<MolAtom> ligListG2C = new ArrayList<MolAtom>();
			populateLists(respMol, atomB, ligListG1B, ligListG2B,
					groupMol1, groupMol2);
			populateLists(respMol, atomC, ligListG1C, ligListG2C,
					groupMol1, groupMol2);
			debugPrint(SELF + "atom B=", atomB.getAtomMap(), " connects to ",
					ligListG1B.size(), " group 1 atoms and ",
					ligListG2B.size(), " group 2 atoms");
			debugPrint(SELF + "atom C=", atomC.getAtomMap(), " connects to ",
					ligListG1C.size(), " group 1 atoms and ",
					ligListG2C.size(), " group 2 atoms");
			// see if any dihedral angle is good
			for (final MolAtom atomJ : ligListG1B) {
				debugPrint(SELF + "atom J attached to atom B has formula ", 
						formula1);
				for (final MolAtom atomK : ligListG2C) {
					debugPrint(SELF + "atom K attached to atom C has formula ", 
							formula2);
					final double angle = VectorMath.calcDihedral(new MolAtom[]
							{atomK, atomC, atomB, atomJ});
					evalResult = goodAngle(angle);
					if (evalResult.isSatisfied) {
						return evalResult;
					}
				} // for each atomK
			} // for each atomJ
			for (final MolAtom atomJ : ligListG1C) {
				debugPrint(SELF + "atom J attached to atom C has formula ", 
						formula1);
				for (final MolAtom atomK : ligListG2B) {
					debugPrint(SELF + "atom K attached to atom B has formula ", 
							formula2);
					final double angle = VectorMath.calcDihedral(new MolAtom[]
							{atomK, atomB, atomC, atomJ});
					evalResult = goodAngle(angle);
					if (evalResult.isSatisfied) {
						return evalResult;
					}
				} // for each atomK
			} // for each atomK
			debugPrint(SELF + "none of the angles looks good");
			evalResult.isSatisfied = false;
		} catch (Exception e1) {
			evalResult.verificationFailureString = "ACE was unable to "
					+ "interpret your response or the question author's "
					+ "parameters.  Please report this error to the "
					+ "programmers: " + e1.getMessage();
			e1.printStackTrace();
			return evalResult;
		} // try
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Determines whether the measured angle conforms to the relationship
	 * designated by the coded data.
	 * @param	angleIn	the measured angle
	 * @return	a OneEvalResult that contains true if the measured angle
	 * conforms to the relationship designated by the coded data
	 */
	private OneEvalResult goodAngle(double angleIn) {
		final String SELF = "ConformBond.goodAngle: ";
		final double TOLERANCE = 15.0;
		final OneEvalResult evalResult = new OneEvalResult();
		debugPrint(SELF + "angle is ", angleIn);
		// bug workaround: angle == 180 can be reported as NaN
		final double angle = (angleIn >= 181) ? 180.0 : angleIn;
		boolean result = false;
		switch (groupRelationship) {
			case ANTI: result = Math.abs(180.0 - angle) <= TOLERANCE; break;
			case NOTANTI: result = Math.abs(180.0 - angle) > TOLERANCE; break;
			case GAUCHE: result = Math.abs(60.0 - angle) <= TOLERANCE; break;
			case NOTGAUCHE: result = Math.abs(60.0 - angle) > TOLERANCE; break;
			case STAGGERED:
				result = (Math.abs(60.0 - angle) <= TOLERANCE)
						|| (Math.abs(180 - angle) <= TOLERANCE);
				break;
			case NOTSTAGGERED:
				result = (Math.abs(60.0 - angle) > TOLERANCE)
						&& (Math.abs(180 - angle) > TOLERANCE);
				break;
			case ECLIPSED: result = Math.abs(angle) <= TOLERANCE; break;
			case NOTECLIPSED: result = Math.abs(angle) > TOLERANCE; break;
			default:
				evalResult.verificationFailureString = 	
					"Internal error: I don't recognize group relationship "
					+ groupRelationship + ".";
				break;
		} // switch
		debugPrint(SELF + "good angle? ", result);
		evalResult.isSatisfied = result;
		return evalResult;
	} // goodAngle(double)

	/** Populate two atom lists with attachment points of group1 and group2,
	 * respectively, that are attached to an atom of the bond about which the
	 * dihedral angle is being measured.
	 * @param	mol	the molecule
	 * @param	mappedAtom	atom of the bond about which the dihedral angle is being
	 * measured
	 * @param	ligListG1	a list of atoms that are attached to the bond atom
	 * and are the attachment points of groupMol1
	 * @param	ligListG2	a list of atoms that are attached to the bond atom
	 * and are the attachment points of groupMol2
	 * @param	groupMol1	molecule of a group whose dihedral angle is being
	 * measured
	 * @param	groupMol2	molecule of the other group whose dihedral angle is being
	 * measured
	 */
	private void populateLists(Molecule mol, MolAtom mappedAtom,
			List<MolAtom> ligListG1, List<MolAtom> ligListG2,
			Molecule groupMol1, Molecule groupMol2) {
		final String SELF = "ConformBond.populateLists: ";
		// collect all the interesting ligands of mappedAtom and bonds thereto
		// first, before calling matchGroup(), which removes and restores bonds,
		// ruining the order.
		final List<MolAtom> ligList = new ArrayList<MolAtom>();
		final List<MolBond> bondToLigList = new ArrayList<MolBond>();
		for (final MolAtom ligand : mappedAtom.getLigands()) {
			if (ligand.getAtomMap() == 0) {
				ligList.add(ligand);
				bondToLigList.add(mappedAtom.getBondTo(ligand));
			} // if ligand is not mapped
		} // for all bonds on mappedAtom
		// check each interesting bond for desired group
		for (int unmapLigNum = 0; unmapLigNum < ligList.size(); unmapLigNum++) {
			final MolAtom ligand = ligList.get(unmapLigNum);
			final MolBond bondToLig = bondToLigList.get(unmapLigNum);
			// matchGroup() inherited from Conformations
			if (matchGroup(mol, bondToLig, groupMol1)) {
				debugPrint(SELF + "atom with map ", mappedAtom.getAtomMap(),
						" connects to a group 1 = ", formula1);
				ligListG1.add(ligand);
			} // if found the desired group 1
			if (matchGroup(mol, bondToLig, groupMol2)) {
				debugPrint(SELF + "atom with map ", mappedAtom.getAtomMap(),
						" connects to a group 2 = ", formula2);
				ligListG2.add(ligand);
			} // if found the desired group 2
		} // each unmapped ligand to mappedAtom
	} // populateLists(Molecule, MolAtom, List<MolAtom>,
	//			List<MolAtom>, Molecule, Molecule)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[CONFORMATION_ACYCLIC]; } 
	/** Gets the formula or SMILES of the first of the groups whose dihedral
	 * angle to measure.
	 * @return	formula or SMILES of the first of the groups whose dihedral angle
	 * to measure
	 */
	public String getFormula1() 				{ return formula1; }
	/** Sets the formula or SMILES of the first of the groups whose dihedral
	 * angle to measure.
	 * @param	formula	formula or SMILES of the first of the groups whose dihedral angle
	 * to measure
	 */
	public void setFormula1(String formula)		{formula1 = formula; }
	/** Gets the formula or SMILES of the second of the groups whose dihedral
	 * angle to measure.
	 * @return	formula or SMILES of the second of the groups whose dihedral angle
	 * to measure
	 */
	public String getFormula2() 				{ return formula2; }
	/** Sets the formula or SMILES of the second of the groups whose dihedral
	 * angle to measure.
	 * @param	formula	formula or SMILES of the second of the groups whose dihedral angle
	 * to measure
	 */
	public void setFormula2(String formula) 	{formula2 = formula; }
	/** Gets whether the dihedral angle should be anti, gauche, eclipsed, etc.
	 * @return	whether the dihedral angle should be anti, gauche, eclipsed,
	 * etc.
	 */
	public int getGroupRelationship() 			{ return groupRelationship; }
	/** Sets whether the dihedral angle should be anti, gauche, eclipsed, etc.
	 * @param	rel	whether the dihedral angle should be anti, gauche, eclipsed,
	 * etc.
	 */
	public void setGroupRelationship(int rel) 	{ groupRelationship = rel; }
	
} // ConformBond

