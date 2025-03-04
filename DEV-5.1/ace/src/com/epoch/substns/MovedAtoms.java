package com.epoch.substns;

import chemaxon.struc.MolAtom;
import chemaxon.struc.MoleculeGraph;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** Stores atoms that have been removed from or added to the molecule so they
 * can be removed from or added to a selected fragment molecule as well.  */
final class MovedAtoms {

	/** Generic R groups removed from the molecule. */
	transient List<MolAtom> removedAtoms = new ArrayList<MolAtom>();
	/** Atoms of instantiated R groups added to the molecule. */
	transient List<MolAtom> addedAtoms = new ArrayList<MolAtom>();

	/** Constructor.  */
	MovedAtoms() {
		// empty
	}

	/** Determines whether there are any atoms in the list of removed atoms.
	 * @return	true if there are any atoms in the list of removed atoms
	 */
	boolean haveRemovedAtoms() {
		return !removedAtoms.isEmpty();
	} // haveRemovedAtoms()

	/** Adds an atom to the list of added atoms.
	 * @param	atom	an atom that was added to the molecule
	 */
	void putAddedAtom(MolAtom atom) {
		addedAtoms.add(atom);
	} // putAddedAtom(MolAtom)

	/** Adds an atom to the list of removed atoms.
	 * @param	atom	an atom that was removed from the molecule
	 */
	void putRemovedAtom(MolAtom atom) {
		removedAtoms.add(atom);
	} // putRemovedAtom(MolAtom)

	/** Gets a string describing the contents. */
	public String toString() {
		final StringBuilder bld = Utils.getBuilder("removedAtoms: ");
		if (removedAtoms.isEmpty()) bld.append("none");
		else {
			int atomNum = 0;
			bld.append('[');
			for (final MolAtom removedAtom : removedAtoms) {
				if (atomNum > 0) bld.append(", ");
				Utils.appendTo(bld, 'R', removedAtom.getRgroup());
				atomNum++;
			} // for each removed atom
			bld.append(']');
		} // if there are removed atoms
		bld.append("\naddedAtoms: ");
		if (addedAtoms.isEmpty()) bld.append("none");
		else {
			final MoleculeGraph parent = addedAtoms.get(0).getParent();
			int atomNum = 0;
			bld.append('[');
			for (final MolAtom addedAtom : addedAtoms) {
				if (atomNum > 0) bld.append(", ");
				bld.append(addedAtom.getSymbol());
				final int chg = addedAtom.getCharge();
				if (chg != 0) {
					bld.append('[');
					if (chg > 0) bld.append('+');
					Utils.appendTo(bld, chg, ']');
				} // if atom is charged
				bld.append(parent.indexOf(addedAtom) + 1);
				atomNum++;
			} // for each added atom
			bld.append(']');
		} // if there are added atoms
		return bld.toString();
	} // toString()

} // MovedAtoms
