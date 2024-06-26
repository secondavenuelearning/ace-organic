package com.epoch.mechanisms;

import chemaxon.struc.Molecule;
import com.epoch.chem.MolCompare;
import com.epoch.chem.MolCompareException;
import com.epoch.chem.Normalize;
import com.epoch.mechanisms.mechConstants.MechConstants;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** Compares sets of molecules for mechanisms.  All methods are static.  */
public final class MechSet implements MechConstants {  

	private static void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	//----------------------------------------------------------------------
	//						   molInArray 
	//----------------------------------------------------------------------
	/** Determines whether a student's response molecule is in an array.  
	 * Assumes all molecules have had S-Groups ungrouped and have been 
	 * hydrogenized.  Aromatization and radical normalization are also 
	 * needed, but are done at this level.
	 * @param	srchMol	molecule for which to search
	 * @param	mols	array of molecules to search
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @return	index in mols where srchMol found, or NOT_FOUND
	 */
	static int molInArray(Molecule srchMol, Molecule[] mols, int flags) {
		return molInArray(srchMol, mols, flags, SRCHFRAG_RESP);
	} // molInArray(Molecule, Molecule[], int)

	/** Determines whether a molecule is in a list.  
	 * Assumes all molecules have had S-Groups ungrouped and have been 
	 * hydrogenized.  Aromatization and radical normalization are also 
	 * needed, but are done at this level.
	 * @param	srchMol	molecule for which to search
	 * @param	mols	list of molecules to search
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @return	index in mols where srchMol found, or NOT_FOUND
	 */
	static int molInList(Molecule srchMol, List<Molecule> mols, int flags) {
		return molInArray(srchMol, Utils.molListToArray(mols), flags, 
				SRCHFRAG_RESP);
	} // molInList(Molecule, List<Molecule>, int)

	/** Determines whether a molecule is in an array.  Assumes all 
	 * molecules have had S-Groups ungrouped and have been hydrogenized.
	 * Aromatization and radical normalization are also needed, but are done 
	 * at this level.
	 * @param	srchMol	molecule for which to search
	 * @param	mols	array of molecules to search
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @param	srchMolIsResp	whether the molecule for which to search is 
	 * from a student's response
	 * @return	index in mols where srchMol found, or NOT_FOUND
	 */
	static int molInArray(Molecule srchMol, Molecule[] mols, int flags, 
			boolean srchMolIsResp) {
		final String SELF = "MechSet.molInArray: ";
		if (srchMol == null) {
			debugPrint(SELF + "srchMol is null!");
			return NOT_FOUND;
		} // if no search molecule
		if (Utils.isEmpty(mols)) {
			debugPrint(SELF + "mols is empty!");
			return NOT_FOUND;
		} // if no mols
		// make copies of given compounds, normalize their radicals and coordinate 
		// bonds, and aromatize. Do NOT modify the originals!!!
		final Molecule copySrchMol = Normalize.mechNormalize(srchMol);
		Normalize.resonateCoordinateBondsNoClone(copySrchMol);
		final boolean resonLenient = (flags & RESON_LENIENT) != 0;
		final boolean stereoLenient = (flags & STEREO_LENIENT) != 0;
		debugPrint(SELF + "looking for srchMol ", copySrchMol, "; resonLenient = ",
				resonLenient, ", stereoLenient = ", stereoLenient, 
				", srchMolIsResp = ", srchMolIsResp);
		final int stereoType = (stereoLenient ? IGNORE_STEREO : WAVY_AND);
		for (int molNum = 0; molNum < Utils.getLength(mols); molNum++) {
			final Molecule copyArrayMol = Normalize.mechNormalize(mols[molNum]);
			Normalize.resonateCoordinateBondsNoClone(copyArrayMol);
			debugPrint(SELF + "comparing to molecule ", molNum + 1, ": ", 
					copyArrayMol);
			try {
				final boolean haveMatch = (srchMolIsResp 
						? MolCompare.matchExact(copySrchMol, copyArrayMol, 
							stereoType)
						: MolCompare.matchExact(copyArrayMol, copySrchMol, 
							stereoType));
				if (haveMatch) {
					debugPrint(SELF + "found match at molNum = ", molNum + 1);
					return molNum;
				} // have a match
				if (resonLenient) 
					debugPrint(SELF + "no match but checking resonance structures.");
				if (resonLenient && MolCompare.areResonanceOrIdentical(
						copySrchMol, copyArrayMol, stereoType)) {
					debugPrint(SELF + "found match at molNum = ", molNum + 1);
					return molNum;
				} // have a match
				debugPrint(SELF + "no match.");
			} catch (MolCompareException e) {
				Utils.alwaysPrint(SELF + "MolCompareException "
						+ "thrown on molecule ", molNum);
			} // try
		} // for each molecule in the list
		debugPrint(SELF + "no match found.");
		return NOT_FOUND;
	} // molInArray(Molecule, Molecule[], int, boolean)

	//----------------------------------------------------------------------
	//							 union
	//----------------------------------------------------------------------
	/** Unites two arrays of molecules, weeding out equivalent compounds.
	 * @param	mols1	first array
	 * @param	mols2	second array
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @return	an array of all the unique molecules 
	 */
	public static Molecule[] union(Molecule[] mols1, Molecule[] mols2, 
			int flags) {
		if (Utils.isEmpty(mols1)) return (mols2 == null ? new Molecule[0] : mols2);
		if (Utils.isEmpty(mols2)) return mols1;
		final List<Molecule> joined = Utils.molArrayToList(mols1);
		for (final Molecule mol : mols2) {
			if (molInArray(mol, mols1, flags) == NOT_FOUND) joined.add(mol);
		} // for each molecule in mols2
		return Utils.molListToArray(joined);
	} // union(Molecule[], Molecule[], int)

	//----------------------------------------------------------------------
	//							 addDifferent
	//----------------------------------------------------------------------
	/** Adds members of an array that are not already in a list to the list.
	 * @param	mols1	list of molecules
	 * @param	mols2	array of molecules maybe to be added
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 */
	static void addDifferent(List<Molecule> mols1, Molecule[] mols2, 
			int flags) {
		if (Utils.isEmpty(mols2)) return;
		final boolean mols1Empty = Utils.isEmpty(mols1);
		final Molecule[] mols1Arr = (mols1Empty ? null 
				: Utils.molListToArray(mols1));
		for (final Molecule mol2 : mols2) { 
			if (mols1Empty 
					|| molInArray(mol2, mols1Arr, flags) == NOT_FOUND) {
				mols1.add(mol2);
			} // if molecule not in list1
		} // for each molecule in mols2
	} // addDifferent(List<Molecule>, Molecule[], int)

	//----------------------------------------------------------------------
	//							 overlapNull
	//----------------------------------------------------------------------
	/** Determines whether two arrays contain any equivalent compounds.
	 * @param	mols1	first array; should be author-derived
	 * @param	mols2	second array; should be student-derived
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @return	true if the two arrays have no common compounds
	 */
	public static boolean overlapNull(Molecule[] mols1, Molecule[] mols2, 
			int flags) {
		for (final Molecule mol1 : mols1)
			if (molInArray(mol1, mols2, flags) >= 0)
				return false;
		return true;
	} // overlapNull(Molecule[], Molecule[], int)
	
	//----------------------------------------------------------------------
	//							 intersection
	//----------------------------------------------------------------------
	/** Gets an array of equivalent compounds shared by two arrays.
	 * @param	mols1	first array
	 * @param	mols2	second array
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @return	the common compounds
	 */
	static Molecule[] intersection(Molecule[] mols1, Molecule[] mols2, 
			int flags) {
		final ArrayList<Molecule> commonCpds = new ArrayList<Molecule>();
		for (final Molecule mol1 : mols1)
			if (molInArray(mol1, mols2, flags) >= 0)
				commonCpds.add(mol1);
		return Utils.molListToArray(commonCpds);
	} // intersection(Molecule[], Molecule[], int)
	
	//----------------------------------------------------------------------
	//							 subsetIndex
	//----------------------------------------------------------------------
	/** Gets the index of the first compound in the first array that is present 
	 * in the second array.
	 * @param	mols1	first array; should be author-derived
	 * @param	mols2	second array; should be student-derived
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @return	index of the first compound in the first array that is present in 
	 * the second array; NOT_FOUND if there is no such compound; SUBSET_NULL if 
	 * the first array is empty
	 */
	static int subsetIndex(Molecule[] mols1, Molecule[] mols2, int flags) {
		return subsetIndex(mols1, mols2, flags, SRCHFRAG_RESP);
	} // subsetIndex(Molecule[], Molecule[], int)

	/** Gets the index of the first compound in the first array that is present 
	 * in the second array.
	 * @param	mols1	first array
	 * @param	mols2	second array
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @param	srchMolIsResp	whether the compounds in the first array are
	 * from a student's response
	 * @return	index of the first compound in the first array that is present in 
	 * the second array; NOT_FOUND if there is no such compound; SUBSET_NULL if 
	 * the first array is empty
	 */
	static int subsetIndex(Molecule[] mols1, Molecule[] mols2, int flags, 
			boolean srchMolIsResp) {
		final String SELF = "MechSet.subsetIndex: ";
		if (mols1 == null) {
			debugPrint(SELF + "mols1 (subset) is null; returning SUBSET_NULL");
			return SUBSET_NULL;
		}
		if (Utils.isEmpty(mols2)) {
			debugPrint(SELF + "mols2 (superset) is null or empty; "
					+ "returning NOT_FOUND");
			return NOT_FOUND;
		}
		debugPrint(SELF + "looking for 1st molecule in:\n", mols1, 
				"\nthat is in superset:\n", mols2);
		for (int molNum = 0; molNum < Utils.getLength(mols1); molNum++) {
			final Molecule mol1 = mols1[molNum];
			if (molInArray(mol1, mols2, flags, srchMolIsResp) < 0) {
				debugPrint("   superset doesn't contain ", mol1);
				return molNum;
			} else debugPrint("   superset contains ", mol1);
		} // for each molecule in mols1
		return NOT_FOUND;
	} // subsetIndex(Molecule[], Molecule[], int, boolean)

	//----------------------------------------------------------------------
	//							 getNonmembers
	//----------------------------------------------------------------------
	/** Gets all of the compounds in the first array that are not present in the
	 * second array.
	 * @param	mols1	first array; should be author-derived
	 * @param	mols2	second array; should be student-derived
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @return	all of the compounds in the first array that are not present in 
	 * the second array
	 */
	static Molecule[] getNonmembers(Molecule[] mols1, Molecule[] mols2, 
			int flags) {
		return getNonmembers(mols1, mols2, flags, SRCHFRAG_RESP);
	} // getNonmembers(Molecule[], Molecule[], int)

	/** Gets all of the compounds in the first array that are not present in the
	 * second array.
	 * @param	mols1	first array; should be author-derived
	 * @param	mols2	second array (as list); should be student-derived
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @return	all of the compounds in the first array that are not present in 
	 * the second array
	 */
	static Molecule[] getNonmembers(Molecule[] mols1, List<Molecule> mols2, 
			int flags) {
		return getNonmembers(mols1, Utils.molListToArray(mols2), flags, 
				SRCHFRAG_RESP);
	} // getNonmembers(Molecule[], List<Molecule>, int)

	/** Gets all of the compounds in the first array that are not present in the
	 * second array.
	 * @param	mols1	first array
	 * @param	mols2	second array
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @param	srchMolIsResp	whether the compounds in the first array are
	 * from a student's response
	 * @return	all of the compounds in the first array that are not present in 
	 * the second array
	 */
	static Molecule[] getNonmembers(Molecule[] mols1, Molecule[] mols2, 
			int flags, boolean srchMolIsResp) {
		final String SELF = "MechSet.getNonmembers: ";
		if (Utils.isEmpty(mols1)) {
			debugPrint(SELF + "mols1 (subset) is null or empty; "
					+ "returning empty array");
			return new Molecule[0];
		}
		if (Utils.isEmpty(mols2)) {
			debugPrint(SELF + "mols2 (superset) is null or empty; "
					+ "returning subset");
			return mols1;
		}
		final List<Molecule> nonmembers = new ArrayList<Molecule>();
		debugPrint(SELF + "looking for:\n", mols1, "\nin superset:\n", mols2);
		for (final Molecule mol1 : mols1) {
			if (molInArray(mol1, mols2, flags, srchMolIsResp) < 0) {
				debugPrint("   superset doesn't contain ", mol1);
				nonmembers.add(mol1);
			} else {
				debugPrint("   superset contains ", mol1);
			}	
		} // for each molecule in mols1
		return Utils.molListToArray(nonmembers);
	} // getNonmembers(Molecule[], Molecule[], int, boolean)

	//----------------------------------------------------------------------
	//							   subset
	//----------------------------------------------------------------------	
	/** Finds whether every compound in the first array is present in 
	 * the second array (whether the first is a subset of the second).
	 * @param	mols1	first array; should be author-derived
	 * @param	mols2	second array; should be student-derived
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @return	true if every compound in the first array is present in the 
	 * second array (the first is a subset of the second)
	 */
	public static boolean subset(Molecule[] mols1, Molecule[] mols2, 
			int flags) {
		return subsetIndex(mols1, mols2, flags) == NOT_FOUND;
	} // subset(Molecule[], Molecule[], int)

	/** Finds whether every compound in the array is present in 
	 * the list (whether the first is a subset of the second).
	 * @param	mols1	array; should be author-derived
	 * @param	mols2	list; should be student-derived
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @return	true if every compound in the first array is present in the 
	 * second array (the first is a subset of the second)
	 */
	static boolean subset(Molecule[] mols1, List<Molecule> mols2, 
			int flags) {
		return subset(mols1, Utils.molListToArray(mols2), flags);
	} // subset(Molecule[], List<Molecule>, int)

	/** Finds whether every compound in the first array is present in 
	 * the second array (whether the first is a subset of the second).
	 * @param	mols1	first array
	 * @param	mols2	second array
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @param	srchMolIsResp	whether the compounds in the first array are
	 * from a student's response
	 * @return	true if every compound in the first array is present in the 
	 * second array (the first is a subset of the second)
	 */
	static boolean subset(Molecule[] mols1, Molecule[] mols2, int flags, 
			boolean srchMolIsResp) {
		return (subsetIndex(mols1, mols2, flags, 
					srchMolIsResp) == NOT_FOUND);
	} // subset(Molecule[], Molecule[], int, boolean)
	
	//----------------------------------------------------------------------
	//							 superset
	//----------------------------------------------------------------------	
	/** Finds whether every compound in the second array is present in
	 * the first array (whether the first is a superset of the second).
	 * This method calls subset and switches the student response and the
	 * author's materials, so we need to alert subset() and molInArray() to it.
	 * @param	mols1	first array; should be author-derived
	 * @param	mols2	second array; should be student-derived
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @return	true if every compound in the second array is present in the 
	 * first array (the first is a superset of the second)
	 */
	public static boolean superset(Molecule[] mols1, Molecule[] mols2, 
			int flags) {
		return subset(mols2, mols1, flags, SRCHFRAG_AUTH);
	} // superset(Molecule[], Molecule[], int)

	/** Finds whether every compound in the second array is present in
	 * the first array (whether the first is a superset of the second).
	 * This method calls subset and switches the student response and the
	 * author's materials, so we need to alert subset() and molInArray() to it.
	 * @param	mols1	first array
	 * @param	mols2	second array
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @param	srchMolIsResp	whether the compounds in the first array are
	 * from a student's response
	 * @return	true if every compound in the second array is present in the 
	 * first array (the first is a superset of the second)
	 */
	static boolean superset(Molecule[] mols1, Molecule[] mols2, int flags, 
			boolean srchMolIsResp) {
		return subset(mols2, mols1, flags, !srchMolIsResp);
	} // superset(Molecule[], Molecule[], int, boolean)
	
	//----------------------------------------------------------------------
	//							 identical
	//----------------------------------------------------------------------	
	/** Finds whether every compound in the first array is found in the second,
	 * and vice versa.
	 * @param	mols1	first array
	 * @param	mols2	second array
	 * @param	flags	whether to consider resonance structures,
	 * stereoisomers as equivalent
	 * @return	true if all of the compounds in the arrays match up
	 */
	public static boolean identical(Molecule[] mols1, Molecule[] mols2, 
			int flags) {
		if (Utils.getLength(mols1) != Utils.getLength(mols2)) return false;
		return subset(mols1, mols2, flags);
	} // identical(Molecule[], Molecule[], int)

	/** Disables external instantiation. */
	private MechSet() { }

} // MechSet
