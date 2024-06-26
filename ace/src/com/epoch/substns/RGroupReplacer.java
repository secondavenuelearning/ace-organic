package com.epoch.substns;

import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import chemaxon.struc.Molecule;
import chemaxon.struc.SelectionMolecule;
import com.epoch.chem.ChemUtils;
import com.epoch.utils.Utils;

/** Replaces generic numbered R groups with the corresponding shortcut group.
 */
final public class RGroupReplacer {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, ChemUtils.MRV);
	}

	/** A Molecule that may contain generic numbered R groups. */
	transient final private Molecule authorMol;
	/** Array of the shortcut groups to replace the generic R groups. */
	transient final private Molecule[] rgMols;
	/** Whether to add explicit H atoms to the shortcut groups. */
	transient final private boolean addHAtoms;

	/** Constructor.
	 * @param	mol	a Molecule
	 * @param	rgpMols	array of the shortcut groups as Molecules
	 * @param	addHs	whether to add explicit H atoms to the shortcut
	 * groups
	 */
	RGroupReplacer(Molecule mol, Molecule[] rgpMols, boolean addHs) {
		authorMol = mol;
		rgMols = rgpMols;
		addHAtoms = addHs;
	} // RGroupReplacer(Molecule, Molecule[], boolean)

	/** Replace generic R groups in a molecule with shortcut groups. Modifies
	 * the molecule!  
	 */
	void replaceRGroups() {
		final String SELF = "RGroupReplacer.replaceRGroups: ";
		final MolAtom[] atomsToExamine = authorMol.getAtomArray();
		final MovedAtoms movedAtoms = replaceRGroups(atomsToExamine);
		if (movedAtoms.haveRemovedAtoms()) {
			debugPrintMRV(SELF + "after replacing all generic R groups, "
					+ "authorMol is:\n", authorMol);
			ChemUtils.clean2D(authorMol);
			debugPrintMRV(SELF + "after 2D cleaning:\n", authorMol);
		} else debugPrint(SELF + "no generic R groups in molecule.");
	} // replaceRGroups()

	/** Replace generic R groups in a selected fragment molecule within a 
	 * Molecule with shortcut groups. Modifies the molecule!
	 * @param	fragMol	a selected fragment molecule within a Molecule
	 * @return	true if there were generic R groups that were replaced
	 */
	boolean replaceRGroups(SelectionMolecule fragMol) {
		final String SELF = "RGroupReplacer.replaceRGroups: ";
		final MolAtom[] atomsToExamine = fragMol.getAtomArray();
		final MovedAtoms movedAtoms = replaceRGroups(atomsToExamine);
		final boolean hadRGroups = movedAtoms.haveRemovedAtoms();
		if (hadRGroups) {
			debugPrint(SELF + "after replacing all generic R groups in "
					+ "fragment ", fragMol, ", authorMol is:\n", authorMol);
			// alter selection of fragment molecule accordingly
			for (final MolAtom removedAtom : movedAtoms.removedAtoms) {
				fragMol.removeAtom(removedAtom);
			} // for each generic R group removed from the molecule 
			for (final MolAtom addedAtom : movedAtoms.addedAtoms) {
				fragMol.add(addedAtom);
			} // for each atom in an instantiated R group added to the molecule 
			ChemUtils.clean2D(fragMol);
			debugPrintMRV(SELF + "after 2D cleaning of fragment:\n", authorMol);
		} else debugPrint(SELF + "no generic R groups in fragment ", fragMol);
		return hadRGroups;
	} // replaceRGroups(SelectionMolecule)

	/** Replaces any generic numbered R groups among the atoms with a shortcut 
	 * group.  Modifies the molecule!
	 * @param	atomsToExamine	atoms of either a whole Molecule or a fragment
	 * molecule within a Molecule
	 * @return	the atoms that are removed from or added to the molecule
	 */
	private MovedAtoms replaceRGroups(MolAtom[] atomsToExamine) {
		final String SELF = "RGroupReplacer.replaceRGroups: ";
		debugPrintMRV(SELF + "authorMol starts as:\n", authorMol);
		final MovedAtoms movedAtoms = new MovedAtoms();
		for (final MolAtom authAtom : atomsToExamine) {
			final int rgIndex = authAtom.getRgroup();
			if (rgIndex != 0) {
				final Molecule rgMol = rgMols[rgIndex - 1].clone();
				debugPrint(SELF + "found numbered generic R group ", 
						rgIndex, " to replace with ", rgMol);
				final MolAtom superatomAttach =
						ChemUtils.getAttachmentPoint(rgMol);
				if (addHAtoms) {
					// only for substructure/skeleton searches
					// add explicit H atoms to instantiated R group
					ChemUtils.explicitizeHnoClone(rgMol);
					// remove one H atom from attach point
					final int newNumLigs = superatomAttach.getBondCount();
					final MolAtom extraH = 
							superatomAttach.getLigand(newNumLigs - 1);
					rgMol.removeAtom(extraH);
					debugPrint(SELF + "hydrogenized R group is ", rgMol);
				} // if should hydrogenate the R group
				boolean orphanRGroup = false;
				// find to what the generic R group is attached in
				// author structure (if anything)
				final MolAtom atomLigand = authAtom.getLigand(0);
				final MolBond atomBond = authAtom.getBond(0);
				if (atomLigand != null && atomBond != null) {
					authorMol.removeBond(atomBond);
				} else {
					debugPrint(SELF + "R group ", rgIndex, 
							" is not attached to anything.");
					orphanRGroup = true;
				} // if generic R group is connected to rest of molecule
				// replace generic R group with instantiated group
				movedAtoms.putRemovedAtom(authAtom);
				authorMol.removeAtom(authAtom);
				for (final MolAtom rgAtom : rgMol.getAtomArray()) {
					movedAtoms.putAddedAtom(rgAtom);
				} // for each atom in the instantiated R group
				authorMol.fuse(rgMol, false);
				if (!orphanRGroup) {
					// attach instantiated R group to remainder of structure
					debugPrint(SELF + "making bond from ", superatomAttach, 
							authorMol.indexOf(superatomAttach) + 1, " to ", 
							atomLigand, authorMol.indexOf(atomLigand) + 1);
					authorMol.add(new MolBond(superatomAttach, atomLigand));
				} // if not an orphan R group
			} // if we found a numbered generic R group
		} // for each atom to examine
		return movedAtoms;
	} // replaceRGroups(MolAtom[])

} // RGroupReplacer
