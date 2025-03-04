package com.epoch.chem;

import chemaxon.sss.search.MolComparator;
import chemaxon.struc.MolAtom;
import com.epoch.utils.Utils;

/** Alters normal JChem matching behavior so that corresponding atoms
* in the query and target must have the same number of explicit H atoms. */
public final class ExplicitHMatcher extends MolComparator {

	private void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	public ExplicitHMatcher() {
		// intentionally empty
	}

	/** Compares the explicit H count of two atoms that match otherwise.
	 * @param	queryAtomNum	the query atom (author's structure)
	 * @param	targetAtomNum	the target atom (student's response)
	 * @return true if the atoms bear the same number of explicit H atoms
	 */
	public boolean compareAtoms(int queryAtomNum, int targetAtomNum) {
		debugPrint("Entering ExplicitHMatcher.compareAtoms: "
				+ "query = ", query, ", target = ", target);
		final int qAtomNumOrig = getOrigQueryAtom(queryAtomNum);
		int qHCount = 0;
		if (qAtomNumOrig != -1) {
			final MolAtom qAtom = query.getAtom(qAtomNumOrig);
			qHCount = qAtom.getExplicitHcount();
			debugPrint("qAtom ", qAtom, qAtomNumOrig + 1,
					" has ", qHCount, " explicit H atoms.");
		} else debugPrint("qAtom H is implicit.");
		final int tAtomNumOrig = getOrigTargetAtom(targetAtomNum);
		int tHCount = 0;
		if (tAtomNumOrig != -1) {
			final MolAtom tAtom = target.getAtom(tAtomNumOrig);
			tHCount = tAtom.getExplicitHcount();
			debugPrint("tAtom ", tAtom, tAtomNumOrig + 1, 
					" has ", tHCount, " explicit H atoms.");
		} else debugPrint("tAtom H is implicit.");
		final boolean match = qHCount == tHCount;
		debugPrint(match ? "Atoms match." : "Atoms do not match.");
		return match;
	} // compareAtoms(int, int)

} // ExplicitHMatcher
