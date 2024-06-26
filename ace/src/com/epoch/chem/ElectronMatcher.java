package com.epoch.chem;

import chemaxon.sss.search.MolComparator;
import chemaxon.struc.MolAtom;
import com.epoch.lewis.lewisConstants.LewisConstants;
import com.epoch.utils.Utils;

/** Alters normal JChem matching behavior so that corresponding atoms
* in the query and target must have the same number of unshared electrons. */
public final class ElectronMatcher extends MolComparator 
		implements LewisConstants {

	private void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	public ElectronMatcher() {
		// intentionally empty
	}

	/** Compares the unshared electron count of two atoms that match otherwise.
	 * @param	queryAtomNum	the query atom (author's structure)
	 * @param	targetAtomNum	the target atom (student's response)
	 * @return true if the atoms have the same number of unshared electrons
	 */
	public boolean compareAtoms(int queryAtomNum, int targetAtomNum) {
		debugPrint("Entering ElectronMatcher.compareAtoms: "
				+ "query = ", query, ", target = ", target);
		final int qAtomNumOrig = getOrigQueryAtom(queryAtomNum);
		int qElecCt = 0;
		if (qAtomNumOrig != -1) {
			final MolAtom qAtom = query.getAtom(qAtomNumOrig);
			qElecCt = ChemUtils.getElectrons(qAtom);
			debugPrint("qAtom ", qAtom, qAtomNumOrig + 1,
					" has ", qElecCt, " unshared electron",
					qElecCt == 1 ? "." : "s.");
		} else debugPrint("qAtom is implicit H, has 0 unshared electrons.");
		final int tAtomNumOrig = getOrigTargetAtom(targetAtomNum);
		int tElecCt = 0;
		if (tAtomNumOrig != -1) {
			final MolAtom tAtom = target.getAtom(tAtomNumOrig);
			tElecCt = ChemUtils.getElectrons(tAtom);
			debugPrint("tAtom ", tAtom, tAtomNumOrig + 1, 
					" has ", tElecCt, " unshared electron",
					tElecCt == 1 ? "." : "s.");
		} else debugPrint("tAtom is implicit H, has 0 unshared electrons.");
		final boolean match = qElecCt == tElecCt;
		debugPrint(match ? "Atoms match." : "Atoms do not match.");
		return match;
	} // compareAtoms(int, int)

} // ElectronMatcher
