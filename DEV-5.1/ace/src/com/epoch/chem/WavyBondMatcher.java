package com.epoch.chem;

import chemaxon.sss.search.MolComparator;
import chemaxon.struc.StereoConstants;
import com.epoch.utils.Utils;

/** Alters normal JChem matching behavior so that a query wavy bond matches
 * only to a target wavy bond, but a query single bond continues to match
 * to any target bond.  */
public final class WavyBondMatcher extends MolComparator
		implements StereoConstants {

	private void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	public WavyBondMatcher() {
		// intentionally empty
	}

	/** Compares the parity of two atoms to ensure that they match according to
	 * the desired behavior.
	 * @param	queryAtomNum	index of the query atom (author's structure)
	 * @param	targetAtomNum	index of the target atom (student's response)
	 * @return true if the atoms match or at least one is implicit H atom
	 */
	public boolean compareAtoms(int queryAtomNum, int targetAtomNum) {
		boolean match = true;
		final int qAtomNum = getOrigQueryAtom(queryAtomNum);
		final int tAtomNum = getOrigTargetAtom(targetAtomNum);
		if (!Utils.among(-1, qAtomNum, tAtomNum)) {
			final int qParity = query.getParity(qAtomNum);
			final int tParity = target.getParity(tAtomNum);
			final int qLocalParity = query.getLocalParity(qAtomNum);
			final int tLocalParity = target.getLocalParity(tAtomNum);
			match = !(qParity == PARITY_EITHER 
						// straight or wavy bond on query stereo
					&& qLocalParity == PARITY_EITHER 
						// wavy bond on query stereo or nonstereo
					&& (tParity != PARITY_EITHER 
						// straight or wavy bond on target stereo
					|| tLocalParity != PARITY_EITHER)); 
						// no wavy bond on target stereo or nonstereo
			// overall, wavy bond on query stereocenter
			// and nonwavy bond on target stereocenter
			debugPrint("WavyBondMatcher.compareAtoms: "
					+ "query = ", query, ", target = ", target,
					"; \nqAtom ", query.getAtom(qAtomNum), qAtomNum + 1,
					" has parity ", getEnglish(qParity),
					" and local parity ", getEnglish(qLocalParity),
					"; tAtom ", target.getAtom(tAtomNum), tAtomNum + 1,
					" has parity ", getEnglish(tParity),
					" and local parity ", getEnglish(tLocalParity),
					"; atoms", match ? "" : " do not", " match");
		} // if neither query nor target atom is implicit H
		return match;
	} // compareAtoms(int, int)

	/** Gets the English name of the parity.
	 * @param	parity	the parity
	 * @return	the English name of the parity
	 */
	private String getEnglish(int parity) {
		return (parity == PARITY_EITHER ? "EITHER"
				: parity == PARITY_ODD ? "ODD (or TETRAHEDRAL)"
				: parity == PARITY_EVEN ? "EVEN (or ALLENE)"
				: parity == PARITY_UNSPEC ? "UNSPECIFIED"
				: parity == 0 ? "NONE"
				: "unknown (" + parity + ")");
	} // getEnglish(int)

} // WavyBondMatcher
