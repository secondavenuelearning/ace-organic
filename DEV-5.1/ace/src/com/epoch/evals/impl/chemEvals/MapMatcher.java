package com.epoch.evals.impl.chemEvals;

import chemaxon.sss.search.MolComparator;
import chemaxon.struc.MolAtom;
import com.epoch.utils.Utils;

/** Alters normal JChem matching behavior so that if a query atom is mapped,
 * the target atom must have the same map, and if a query atom is unmapped,
 * the target atom may or may not have to have the same map. */
final class MapMatcher extends MolComparator {

	/** Determines whether an unmapped query matches any map in the target. */
	transient final boolean matchUnmappedQuery;

	private void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	MapMatcher() {
		matchUnmappedQuery = false;
	} // MapMatcher()

	/** Constructor.
	 * @param	matchUnmapped	whether an unmapped query will match any map in
	 * the target
	 */
	MapMatcher(boolean matchUnmapped) {
		matchUnmappedQuery = matchUnmapped;
	} // MapMatcher(boolean)

	/** Compares the mapping of two atoms that match otherwise.
	 * @param	queryAtomNum	the query atom (author's structure)
	 * @param	targetAtomNum	the target atom (student's response)
	 * @return true if the query atom is unmapped or if the target atom's map
	 * matches the query atom's map.
	 */
	public boolean compareAtoms(int queryAtomNum, int targetAtomNum) {
		int qMap = 0;
		MolAtom qAtom = null;
		final int qAtomNumOrig = getOrigQueryAtom(queryAtomNum);
		if (qAtomNumOrig != -1) {
			qAtom = query.getAtom(qAtomNumOrig);
			qMap = qAtom.getAtomMap();
		} // if there's a query atom
		int tMap = 0;
		MolAtom tAtom = null;
		int tAtomNumOrig = -1;
		if (qMap != 0 || !matchUnmappedQuery) {
			tAtomNumOrig = getOrigTargetAtom(targetAtomNum);
			if (tAtomNumOrig != -1) {
				tAtom = target.getAtom(tAtomNumOrig);
				tMap = tAtom.getAtomMap();
			} // if there's a target atom
		} // if don't need map of target atom
		final boolean match = (matchUnmappedQuery && qMap == 0) || qMap == tMap;
		if (match && qMap == 0 && matchUnmappedQuery) {
			debugPrint("Query atom ", qAtom, qAtomNumOrig + 1, 
					" is unmapped, matches target atom with any map."); 
		} else if (match) {
			debugPrint("Query atom ", qAtom, qAtomNumOrig + 1, 
					" with ", qMap == 0 ? "no map" : "map " + qMap, 
					" matches target atom ",
					tAtom == null ? "H" : tAtom, tAtomNumOrig + 1,
					" with ", tMap == 0 ? "no map." : "map " + tMap + ".");
		} else {
			debugPrint("Query atom ", qAtom, qAtomNumOrig + 1, 
					" with ", qMap == 0 ? "no map" : "map " + qMap, 
					" DOES NOT MATCH target atom ",
					tAtom == null ? "H" : tAtom, tAtomNumOrig + 1,
					" with ", tMap == 0 ? "no map." : "map " + tMap + ".");
		} // if match
		return match;
	} // compareAtoms(int, int)

} // MapMatcher
