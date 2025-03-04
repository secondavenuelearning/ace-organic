package com.epoch.synthesis;

import chemaxon.sss.search.SearchException;
import chemaxon.struc.MoleculeGraph;
import chemaxon.struc.Molecule;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolCompare;
import com.epoch.chem.MolCompareException;
import com.epoch.chem.Normalize;
import com.epoch.synthesis.synthConstants.SynthConstants;
import com.epoch.utils.Utils;
import java.util.List;

/** Compares sets of molecules for mechanisms.  All methods are static.  */
public final class SynthSet implements SynthConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	//----------------------------------------------------------------------
	//						   molInArray
	//----------------------------------------------------------------------
	/** Determines whether a student's response molecule is in an array.
	 * Assumes all molecules have had S-Groups ungrouped.
	 * Aromatization and radical normalization are also
	 * needed, but are done at this level.
	 * @param	srchMol	molecule for which to search
	 * @param	mols	array of molecules to search
	 * @return	index in mols where srchMol found, or NOT_FOUND
	 * @throws	SearchException	if something goes wrong when comparing 
	 * the molecule to array molecules
	 */
	public static int molInArray(Molecule srchMol, Molecule[] mols)
			throws SearchException {
		return molInArray(srchMol, mols, EXACT_STEREO);
	} // molInArray(Molecule, Molecule[])

	/** Determines whether a student's response molecule is in a list.
	 * Assumes all molecules have had S-Groups ungrouped.
	 * Aromatization and radical normalization are also
	 * needed, but are done at this level.
	 * @param	srchMol	molecule for which to search
	 * @param	mols	array of molecules to search
	 * @return	index in mols where srchMol found, or NOT_FOUND
	 * @throws	SearchException	if something goes wrong when comparing 
	 * the molecule to array molecules
	 */
	public static int molInArray(Molecule srchMol, List<Molecule> mols)
			throws SearchException {
		return molInArray(srchMol, mols, EXACT_STEREO);
	} // molInArray(Molecule, List<Molecule>)

	/** Determines whether a student's response molecule is in a list.
	 * Assumes all molecules have had S-Groups ungrouped.
	 * Aromatization and radical normalization are also
	 * needed, but are done at this level.
	 * @param	srchMol	molecule for which to search
	 * @param	mols	array of molecules to search
	 * @param	stereoTolerance	what kinds of isomers should be considered
	 * equivalent
	 * @return	index in mols where srchMol found, or NOT_FOUND
	 * @throws	SearchException	if something goes wrong when comparing 
	 * the molecule to array molecules
	 */
	public static int molInArray(Molecule srchMol, List<Molecule> mols,
			int stereoTolerance) throws SearchException {
		return molInArray(srchMol, Utils.molListToArray(mols), stereoTolerance);
	} // molInArray(Molecule, List<Molecule>, int)

	/** Determines whether a student's response molecule is in an array or list.
	 * Assumes all molecules have had S-Groups ungrouped.
	 * Aromatization and radical normalization are also
	 * needed, but are done at this level.
	 * @param	srchMol	molecule for which to search
	 * @param	mols	array of molecules to search
	 * @param	stereoTolerance	what kinds of isomers should be considered
	 * equivalent
	 * @return	index in mols where srchMol found, or NOT_FOUND
	 * @throws	SearchException	if something goes wrong when comparing 
	 * the molecule to array molecules
	 */
	public static int molInArray(Molecule srchMol, Molecule[] mols,
			int stereoTolerance) throws SearchException {
		final String SELF = "SynthSet.molInArray: ";
		if (srchMol == null || mols == null) {
			debugPrint(SELF, (srchMol == null ? "srchMol" :  "mols"), 
					" is null!");
			return NOT_FOUND;
		} // if either search item is null
		debugPrint(SELF + "searching for ", srchMol, " among ", mols);
		// make copies of given compounds, aromatize them, and normalize their
		// radicals. Do NOT modify the originals!!!
		final Molecule copySrchMol = srchMol.clone();
		copySrchMol.aromatize(MoleculeGraph.AROM_GENERAL);
		Normalize.normalizeRadicals(copySrchMol);
		for (int molNum = 0; molNum < mols.length; molNum++) {
			final Molecule copyArrayMol = mols[molNum].clone();
			debugPrint(SELF + "comparing ", srchMol, " to array molecule ",
					molNum + 1, ": ", copyArrayMol);
			copyArrayMol.aromatize(MoleculeGraph.AROM_GENERAL);
			Normalize.normalizeRadicals(copyArrayMol);
			boolean haveMatch = false;	
			try {
				switch (stereoTolerance) {
					case EXACT_STEREO:
					case TOLERATE_ENANT:
						haveMatch = MolCompare.matchExact(
								copySrchMol, copyArrayMol,
								stereoTolerance == TOLERATE_ENANT);
						break;
					case TOLERATE_DIASTEREO_ONLY:
					case TOLERATE_ANY_STEREO:
						haveMatch = MolCompare.matchExact(copySrchMol,
								copyArrayMol, IGNORE_STEREO);
						if (haveMatch && stereoTolerance == TOLERATE_DIASTEREO_ONLY)
							haveMatch = !MolCompare.matchExact(copySrchMol,
									ChemUtils.getMirror(copyArrayMol));
						break;
					default:
						Utils.alwaysPrint(SELF + "bad stereoTolerance");
						break;
				} // switch(stereoTolerance)
				if (haveMatch) {
					debugPrint(SELF + "molNum = ", molNum + 1,
							", leaving SynthSet.molInArray.");
					return molNum;
				} // if there's a match
			} catch (MolCompareException e) {
				Utils.alwaysPrint(SELF + "MolCompareException "
						+ "thrown on molecule ", molNum + 1);
			} // try
		} // for each molecule in the array
		debugPrint(SELF + "no match found, leaving SynthSet.molInArray.");
		return NOT_FOUND;
	} // molInArray(Molecule, Molecule[], int)

	//----------------------------------------------------------------------
	//							 union
	//----------------------------------------------------------------------
	/** Unites two arrays of molecules, weeding out equivalent compounds.
	 * @param	mols1	first array
	 * @param	mols2	second array
	 * @return	an array of all the unique molecules
	 * @throws	SearchException	if something goes wrong when comparing 
	 * the molecules to each other
	 */
	public static Molecule[] union(Molecule[] mols1, Molecule[] mols2) 
			throws SearchException {
		if (Utils.isEmpty(mols1)) return mols2;
		if (Utils.isEmpty(mols2)) return mols1;
		final List<Molecule> joined = Utils.molArrayToList(mols1);
		for (final Molecule mol : mols2) {
			if (molInArray(mol, mols1) == NOT_FOUND) joined.add(mol);
		} // for each molecule in mols2
		return Utils.molListToArray(joined);
	} // union(Molecule[], Molecule[])

	//----------------------------------------------------------------------
	//							 overlapNull
	//----------------------------------------------------------------------
	/** Determines whether two arrays contain any equivalent compounds.
	 * @param	mols1	first array
	 * @param	mols2	second array
	 * @return	true if the two arrays have no common compounds
	 * @throws	SearchException	if something goes wrong when comparing 
	 * the molecules to each other
	 */
	public static boolean overlapNull(Molecule[] mols1, Molecule[] mols2) 
			throws SearchException {
		for (final Molecule mol1 : mols1)
			if (molInArray(mol1, mols2) >= 0)
				return false;
		return true;
	} // overlapNull(Molecule[]. Molecule[])

	//----------------------------------------------------------------------
	//							 subsetIndex
	//----------------------------------------------------------------------
	/** Gets the index of the first compound in the first array that is present 
	 * in the second array.
	 * @param	mols1	first array
	 * @param	mols2	second array
	 * @return	index of the first compound in the first array that is present in
	 * the second array; NOT_FOUND if there is no such compound; SUBSET_NULL if
	 * the first array is empty
	 * @throws	SearchException	if something goes wrong when comparing 
	 * the molecules to each other
	 */
	public static int subsetIndex(Molecule[] mols1, Molecule[] mols2) 
			throws SearchException {
		final String SELF = "SynthSet.subsetIndex: ";
		if (mols1 == null) {
			debugPrint(SELF + "mols1 (subset) is null; returning SUBSET_NULL");
			return SUBSET_NULL;
		}
		if (mols2 == null) {
			debugPrint(SELF + "mols2 (superset) is null; returning NOT_FOUND");
			return NOT_FOUND;
		}
		debugPrint(SELF + "Superset is: ", mols2,
				";\n  Comparing molecules to superset.");
		for (int mol1Num = 0; mol1Num < mols1.length; mol1Num++) {
			if (molInArray(mols1[mol1Num], mols2, EXACT_STEREO) < 0) {
				debugPrint("   superset doesn't contain ",
						mols1[mol1Num]);
				return mol1Num;
			} else {
				debugPrint("   superset contains ",
						mols1[mol1Num]);
			} // if molInArray
		} // for each mol in mol1
		return NOT_FOUND;
	} // subsetIndex(Molecule[], Molecule[])

	//----------------------------------------------------------------------
	//						 subsetNotContainedIndex
	//----------------------------------------------------------------------
	/** Gets the index of the first substructure in the first array that is 
	 * not present in any of the compounds in the second array.
	 * @param	substructs	first array
	 * @param	mols	second array
	 * @return	index of the substructure that is not found, NOT_FOUND
	 * otherwise
	 * @throws	SearchException	if something goes wrong when comparing 
	 * the molecules to each other
	 */
	public static int subsetNotContainedIndex(Molecule[] substructs,
			Molecule[] mols) throws SearchException {
		final String SELF = "SynthSet.subsetNotContainedIndex: ";
		debugPrint(SELF + "Looking for each substructure ",
				substructs, " among ", mols);
		if (substructs == null) {
			debugPrint(SELF + "substructs (subset) is null; returning NOT_FOUND.");
			return NOT_FOUND;
		} // if substructs is null
		if (mols == null) {
			debugPrint(SELF + "mols (superset) is null, so every substructure "
					+ "is not found in mols; returning index 0.");
			return 0;
		} // if mols is null
		for (int subNum = 0; subNum < substructs.length; subNum++) {
			final Molecule substruct = substructs[subNum];
			boolean found = false;
			for (final Molecule mol : mols) {
				try {
					if (MolCompare.containsSubstruct(mol, substruct)) {
						debugPrint(SELF + "mol ", mol, " contains ", substruct);
						found = true;
						break;
					} // if mol contains substruct
				} catch (MolCompareException e) {
					Utils.alwaysPrint(SELF
							+ "MolCompareException thrown on substructure ",
							substruct, " and mol ", mol, "; returning ", subNum);
					return subNum;
				} // try
			} // for each mol in mol1
			if (!found) {
				debugPrint(SELF + "substructure ", substruct, 
						" not found among mols ", mols);
				return subNum;
			} // if substruct not found among mols
		} // for each mol in substruct
		debugPrint(SELF + "every substruct "
				+ "is found in mols, so returning NOT_FOUND.");
		return NOT_FOUND;
	} // subsetNotContainedIndex(Molecule[], Molecule[])

	//----------------------------------------------------------------------
	//						 subsetContained
	//----------------------------------------------------------------------
	/** Finds whether every compound in an array is a substructure of at least
	 * one compound in another array.
	 * @param	substructs	first array
	 * @param	mols	second array
	 * @return	true if every member of substructs is found in at least one
	 * member of mols
	 * @throws	SearchException	if something goes wrong when comparing 
	 * the molecules to each other
	 */
	public static boolean subsetContained(Molecule[] substructs,
			Molecule[] mols) throws SearchException {
		return subsetNotContainedIndex(substructs, mols) == NOT_FOUND;
	} // subsetContained(Molecule[], Molecule[])
	
	//----------------------------------------------------------------------
	//							   subset
	//----------------------------------------------------------------------	
	/** Finds whether every compound in the first array is present in
	 * the second array (whether the first is a subset of the second).
	 * @param	mols1	first array
	 * @param	mols2	second array
	 * @return	true if every compound in the first array is present in the
	 * second array (the first is a subset of the second)
	 * @throws	SearchException	if something goes wrong when comparing 
	 * the molecules to each other
	 */
	public static boolean subset(Molecule[] mols1, Molecule[] mols2)
			throws SearchException {
		return subsetIndex(mols1, mols2) == NOT_FOUND;
	} // subset(Molecule[], Molecule[])

	//----------------------------------------------------------------------
	//							 superset
	//----------------------------------------------------------------------	
	/** Finds whether every compound in the second array is present in
	 * the first array (whether the first is a superset of the second).
	 * @param	mols1	first array
	 * @param	mols2	second array
	 * @return	true if every compound in the second array is present in the
	 * first array (the first is a superset of the second)
	 * @throws	SearchException	if something goes wrong when comparing 
	 * the molecules to each other
	 */
	public static boolean superset(Molecule[] mols1, Molecule[] mols2)
			throws SearchException {
		return subset(mols2, mols1);
	} // superset(Molecule[], Molecule[])

	//----------------------------------------------------------------------
	//							 identical
	//----------------------------------------------------------------------	
	/** Finds whether every compound in the first array is found in the second,
	 * and vice versa.
	 * @param	mols1	first array
	 * @param	mols2	second array
	 * @return	true if all of the compounds in the arrays match up
	 * @throws	SearchException	if something goes wrong when comparing 
	 * the molecules to each other
	 */
	public static boolean identical(Molecule[] mols1, Molecule[] mols2)
			throws SearchException {
		if (mols1.length != mols2.length) return false;
		return subset(mols1, mols2);
	} // identical(Molecule[], Molecule[])

	/** Disables external instantiation. */
	private SynthSet() { }

} // SynthSet
