package com.epoch.evals.impl.chemEvals;

import chemaxon.struc.AtomProperty.Radical;
import chemaxon.struc.Molecule;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolCompare;
import com.epoch.evals.impl.CompareNums;
import com.epoch.utils.Utils;

/** Parent class for <code>ConformChair</code> and <code>ConformBond</code>; 
 * contains some common methods and constants.  */
public class Conformations extends CompareNums {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	public Conformations() {
		// intentionally empty
	}

	/** Name with which to mark the attachment point of a group attached to the
	 * ring; the element of the attachment point should be appended. */
	private static final String ATTACH_PT = "ATTACH_PT_";

	/** Convert a group's attachment point to a pseudoatom with name
	 * ATTACH_PT + element.
	 * @param	groupMol	molecule to modify
	 */
	protected void modifyAttachmentPoint(Molecule groupMol) {
		final MolAtom attachPt = ChemUtils.getAttachmentPoint(groupMol);
		if (attachPt != null) modifyAtom(attachPt);
	} // modifyAttachmentPoint(Molecule)

	/** Convert an atom to a pseudoatom with name ATTACH_PT + element.
	 * The element name prevents, e.g., NMe2 from matching to iPr.  The
	 * alternative is to match the unmodified fragments first and then match
	 * them again after the attachment points are modified.
	 * @param	attachPt	atom to modify
	 */
	protected void modifyAtom(MolAtom attachPt) {
		final String element = attachPt.getSymbol();
		attachPt.setAtno(MolAtom.PSEUDO);
		attachPt.setAliasstr(ATTACH_PT + element);
		if (attachPt.getRadicalCount() > 0) {
			attachPt.setRadicalValue(Radical.NO_RADICAL);
		} // if it's a radical; needed for H atom
	} // modifyAtom(MolAtom)

	/** When we split 'mol' at 'bond', and we modify the attachment point
	 * of that piece to the pseudoatom ATTACH_PT_element, then it should look
	 * like targetMol.  Return success of finding all that.
	 * @param	mol	the molecule
	 * @param	bond	the bond to split
	 * @param	targetMol	group the split-off piece should look like
	 * when its attachment point is modified
	 * @return	true if the split-off group matches the target group
	 */
	protected boolean matchGroup(Molecule mol, MolBond bond, Molecule targetMol) {
		final String SELF = "Conformations.matchGroup: ";
		debugPrint(SELF + "looking for the group ", targetMol, " in ",
				mol, " after removing bond connecting atoms ",
				mol.indexOf(bond.getAtom1()) + 1, " and ",
				mol.indexOf(bond.getAtom2()) + 1);
		// select the two ends of the bond so we can replace them with
		// pseudoatoms
		bond.getAtom1().setSelected(true);
		bond.getAtom2().setSelected(true);
		// remove edge from molecule to split it into two pieces
		mol.removeBond(bond);
		// We expect the interesting group to be in the second piece, but it
		// isn't certain.  convertToFrags() destroys map info, which we still
		// need, so we work with a clone.
		final Molecule[] molFragList = mol.clone().convertToFrags();
		// Restore the original molecule now that we have a clone.
		mol.add(bond);
		bond.getAtom1().setSelected(false);
		bond.getAtom2().setSelected(false);
		// Try both fragments
		int molFragIdx = 0;
		for (final Molecule foundGroup : molFragList) {
			// see if this fragment matches
			molFragIdx++;
			final MolAtom selected = ChemUtils.getSelectedAtom(foundGroup);
			if (selected != null) {
				modifyAtom(selected);
				debugPrint(SELF + "comparing fragment ", molFragIdx,
						": ", foundGroup, ", to: ", targetMol);
				try { // matchExact can throw an exception
					if (MolCompare.matchExact(foundGroup, targetMol)) {
						debugPrint(SELF + "found target!");
						return true;
					} // complete match
				} catch (Exception e) {
					// matchExact can throw an exception; ignore it.
				} // try
			} // if there's a selected atom
		} // for each of the two fragments when bond is removed
		return false; // neither fragment is what we are looking for.
	} // matchGroup(Molecule, MolBond, Molecule)

} // Conformations

