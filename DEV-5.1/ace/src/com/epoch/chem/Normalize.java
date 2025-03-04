package com.epoch.chem;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.license.LicenseException;
import chemaxon.standardizer.Standardizer;
import chemaxon.struc.AtomProperty.Radical;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import chemaxon.struc.Molecule;
import chemaxon.struc.MoleculeGraph;
import chemaxon.struc.Sgroup;
import chemaxon.struc.SgroupType;
import chemaxon.struc.sgroup.MulticenterSgroup;
import com.epoch.AppConfig;
import com.epoch.chem.chemConstants.ChemConstants;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Contains molecule normalization methods.  */
public final class Normalize implements ChemConstants {

	private static void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	/** Converts the String representation of this molecule into a "normalized"
	 * String representation to implement some chemical equivalencies.
	 * <ol>
	 * <li>Aromatize.
	 * <li>Ungroup shortcut groups.
	 * <li>Remove spin state information from radicals.
	 * <li>Converts every single bond of the form
	 * (S|Se|N|P|As)<sup><i>m</i>+</sup>&ndash;(O|S|Se|N|C)<sup><i>n</i>&ndash;</sup>
	 * into
	 * (S|Se|N|P|As)<sup>(<i>m</i>&nbsp;&ndash;&nbsp;1)+</sup>=(O|S|Se|N|C)<sup>(<i>n</i>&nbsp;&ndash;&nbsp;1)&ndash;</sup>,
	 * and treat similar double bonds similarly.
	 * <li>For every stereo bond, if it is from a nonstereocenter to a stereocenter
	 * or to a double-bond atom, flip it.   This will make sure that the
	 * student's response with opposite convention will be treated as correct.
	 * </ol>
	 * @param	molStruct	String representation of a molecule
	 * @return	String representation of the normalized molecule
	 * @throws	MolFormatException	if the molecule can't be imported 
	 */
	public static String normalize(String molStruct) throws MolFormatException {
		final String SELF = "Normalize.normalize: ";
		Molecule mol = null;
		try {
			mol = MolImporter.importMol(molStruct);
			normalizeNoClone(mol);
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "MolFormatException for ", molStruct);
			e.printStackTrace();
			throw e;
		}
		return MolString.toString(mol, MRV);
	} // normalize(String)

	/** Converts a copy of this molecule into a "normalized" form to implement
	 * some chemical equivalencies.
	 * <ol>
	 * <li>Aromatize.
	 * <li>Ungroup shortcut groups.
	 * <li>Remove spin state information from radicals.
	 * <li>Converts every single bond of the form
	 * (S|Se|N|P|As)<sup><i>m</i>+</sup>&ndash;(O|S|Se|N|C)<sup><i>n</i>&ndash;</sup>
	 * into
	 * (S|Se|N|P|As)<sup>(<i>m</i>&nbsp;&ndash;&nbsp;1)+</sup>=(O|S|Se|N|C)<sup>(<i>n</i>&nbsp;&ndash;&nbsp;1)&ndash;</sup>,
	 * and treat similar double bonds similarly.
	 * <li>For every stereo bond, if it is from a nonstereocenter to a stereocenter
	 * or to a double-bond atom, flip it.   This will make sure that the
	 * student's response with opposite convention will be treated as correct.
	 * </ol>
	 * @param	origMol	a molecule
	 * @return	normalized copy of the original molecule
	 */
	public static Molecule normalize(Molecule origMol) {
		final Molecule mol = origMol.clone();
		normalizeNoClone(mol);
		return mol;
	} // normalize(Molecule)

	/** Converts a copy of this molecule into a "normalized" form to implement
	 * some chemical equivalencies.
	 * <ol>
	 * <li>Aromatize.
	 * <li>Remove spin state information from radicals.
	 * <li>For every stereo bond, if it is from a nonstereocenter to a stereocenter
	 * or to a double-bond atom, flip it.   This will make sure that the
	 * student's response with opposite convention will be treated as correct.
	 * <li>Remove unbonded multicenter attachment points.
	 * </ol>
	 * @param	origMol	a molecule
	 * @return	normalized copy of the original molecule
	 */
	public static Molecule mechNormalize(Molecule origMol) {
		final Molecule mol = origMol.clone();
		normalizeNoClone(mol, true);
		return mol;
	} // mechNormalize(Molecule)

	/** Converts this molecule into a "normalized" form to implement
	 * some chemical equivalencies.  Modifies the original molecule!
	 * <ol>
	 * <li>Aromatize.
	 * <li>Ungroup shortcut groups.
	 * <li>Remove spin state information from radicals.
	 * <li>Converts every single bond of the form
	 * (S|Se|N|P|As)<sup><i>m</i>+</sup>&ndash;(O|S|Se|N|C)<sup><i>n</i>&ndash;</sup>
	 * into
	 * (S|Se|N|P|As)<sup>(<i>m</i>&nbsp;&ndash;&nbsp;1)+</sup>=(O|S|Se|N|C)<sup>(<i>n</i>&nbsp;&ndash;&nbsp;1)&ndash;</sup>,
	 * and treat similar double bonds similarly.
	 * <li>For every stereo bond, if it is from a nonstereocenter to a stereocenter
	 * or to a double-bond atom, flip it.   This will make sure that the
	 * student's response with opposite convention will be treated as correct.
	 * </ol>
	 * @param	mol	a molecule
	 */
	public static void normalizeNoClone(Molecule mol) {
		normalizeNoClone(mol, false);
	} // normalizeNoClone(Molecule)

	/** Converts this molecule into a "normalized" form to implement
	 * some chemical equivalencies.  Modifies the original molecule!
	 * <ol>
	 * <li>Aromatize.
	 * <li>Remove spin state information from radicals.
	 * <li>For every stereo bond, if it is from a nonstereocenter to a stereocenter
	 * or to a double-bond atom, flip it.   This will make sure that the
	 * student's response with opposite convention will be treated as correct.
	 * <li>Remove unbonded multicenter attachment points.
	 * </ol>
	 * @param	mol	a molecule
	 */
	public static void mechNormalizeNoClone(Molecule mol) {
		normalizeNoClone(mol, true);
	} // mechNormalizeNoClone(Molecule)

	/** Converts this molecule into a "normalized" form to implement
	 * some chemical equivalencies.  Modifies the original molecule!
	 * <ol>
	 * <li>Aromatize.
	 * <li>Remove spin state information from radicals.
	 * <li>For every stereo bond, if it is from a nonstereocenter to a stereocenter
	 * or to a double-bond atom, flip it.   This will make sure that the
	 * student's response with opposite convention will be treated as correct.
	 * <li>Ungroup shortcut groups (nonmechanisms only).
	 * <li>Converts every single bond of the form
	 * (S|Se|N|P|As)<sup><i>m</i>+</sup>&ndash;(O|S|Se|N|C)<sup><i>n</i>&ndash;</sup>
	 * into
	 * (S|Se|N|P|As)<sup>(<i>m</i>&nbsp;&ndash;&nbsp;1)+</sup>=(O|S|Se|N|C)<sup>(<i>n</i>&nbsp;&ndash;&nbsp;1)&ndash;</sup>,
	 * and treat similar double bonds similarly (nonmechanisms only).
	 * </ol>
	 * @param	mol	a molecule
	 * @param	isMech	whether this molecule represents a mechanism
	 */
	public static void normalizeNoClone(Molecule mol, boolean isMech) {
		final String SELF = "Normalize.normalizeNoClone: ";
		debugPrintMRV(SELF + "starting as \n", mol);
		mol.aromatize(MoleculeGraph.AROM_GENERAL);
		if (!isMech) mol.ungroupSgroups(SHORTCUT_GROUPS);
		normalizeRadicals(mol);
		normalizeStereoBonds(mol);
		if (isMech) removeUnbondedMulticenterAtoms(mol);
		else try {
			final Standardizer localStandardizer = 
					new Standardizer(AppConfig.standardizer);
			localStandardizer.standardize(mol);
		} catch (IllegalArgumentException e) {
			Utils.alwaysPrint(SELF + "caught IllegalArgumentException trying "
					+ "to create copy of AppConfig.standardizer or trying "
					+ "to standardize:\n", mol);
			e.printStackTrace();
		} catch (LicenseException e) {
			Utils.alwaysPrint(SELF + "caught LicenseException.");
			e.printStackTrace();
		} // try
		mol.valenceCheck(); // resets implicit H counts
		debugPrintMRV(SELF + "mol converted to \n", mol);
	} // normalizeNoClone(Molecule, boolean)

	/** Removes spin state information from radicals to allow
	 * the match/search to work. Atoms with RAD2_SINGLET and RAD2_TRIPLET
	 * are set to RAD2, and atoms with RAD3_DOUBLET and RAD3_QUARTET are
	 * set to RAD3.
	 * @param	mol	a molecule
	 */
	public static void normalizeRadicals(Molecule mol) {
		for (final MolAtom atom : mol.getAtomArray()) {
			normalizeRadical(atom);
		} // for each atom
	} // normalizeRadicals(Molecule)

	/** Removes spin state information from radicals to allow
	 * the match/search to work. Atoms with DIVALENT_SINGLET and 
	 * DIVALENT_TRIPLET are set to DIVALENT, and atoms with TRIVALENT_DOUBLET 
	 * and TRIVALENT_QUARTET are set to MONOVALENT.
	 * @param	atom	an atom
	 */
	public static void normalizeRadical(MolAtom atom) {
		final String SELF = "Normalize.normalizeRadical: "; 
		final int rad = atom.getRadicalValue().getIntValue();
		if (Utils.among(rad, 
				Radical.DIVALENT_SINGLET.getIntValue(),
				Radical.DIVALENT_TRIPLET.getIntValue())) {
			debugPrint(SELF, atom, " normalized to DIVALENT");
			atom.setRadicalValue(Radical.DIVALENT);
		} else if (Utils.among(rad, 
				Radical.TRIVALENT.getIntValue(),
				Radical.TRIVALENT_DOUBLET.getIntValue(),
				Radical.TRIVALENT_QUARTET.getIntValue())) {
			debugPrint(SELF, atom, " normalized to MONOVALENT");
			atom.setRadicalValue(Radical.MONOVALENT);
		}
	} // normalizeRadical(MolAtom)

	/** For every stereo bond, if it is from a nonstereocenter to a stereocenter
	 * or to a double-bond atom, flips it.
	 * @param	mol	a molecule
	 */
	public static void normalizeStereoBonds(Molecule mol) {
		final String SELF = "Normalize.normalizeStereoBonds: ";
		int bondNum = 0;
		for (final MolBond bond : mol.getBondArray()) {
			final int stereo = StereoFunctions.getBondStereoFlags(bond);
			if (Utils.among(stereo, MolBond.UP, MolBond.DOWN, MolBond.WAVY)) {
				final MolAtom atom1 = bond.getAtom1();
				final MolAtom atom2 = bond.getAtom2();
				final int atom1Num = mol.indexOf(atom1);
				final int atom2Num = mol.indexOf(atom2);
				// flip switched up, down, wavy bonds
				final int parity1 = mol.getParity(atom1Num);
				final int parity2 = mol.getParity(atom2Num);
				debugPrint(SELF + " for stereobond ", ++bondNum, " between ", 
						atom1, atom1Num + 1, " and ", atom2, atom2Num + 1,
						", parity1 = ", parity1, ", parity2 = ", parity2,
						" (PARITY_EVEN = ", MolAtom.PARITY_EVEN,
						", PARITY_ODD = ", MolAtom.PARITY_ODD,
						", PARITY_EITHER = ", MolAtom.PARITY_EITHER, ")");
				final boolean atom1InStereoDbl = isInStereoDoubleBond(atom1);
				final boolean atom2InStereoDbl = isInStereoDoubleBond(atom2);
				if (!Utils.among(parity1, MolAtom.PARITY_EVEN, 
							MolAtom.PARITY_ODD, MolAtom.PARITY_EITHER)
						&& (Utils.among(parity2, MolAtom.PARITY_EVEN, 
								MolAtom.PARITY_ODD, MolAtom.PARITY_EITHER)
							|| (atom2InStereoDbl && !atom1InStereoDbl))) {
					if (atom2InStereoDbl && !atom1InStereoDbl) 
						debugPrint(SELF + "Parity at narrow end = ", parity1,
								", atom at wide end is part of stereogenic "
								+ "double bond, atom at narrow end is not"
								+ "; direction of bond must be switched");
					else debugPrint(SELF + "Parity at narrow end = ", parity1,
							", parity at wide end = ", parity2,
							"; direction of bond must be switched");
					bond.swap();
				} // if bond is pointing from nonstereocenter to stereocenter
			} // up or down or wavy
		} // for each bond
	} // normalizeStereoBonds(Molecule)

	/** Determines whether an atom is part of a stereogenic double bond.
	 * @param	atom	the atom
	 * @return	true if the atom is part of a stereogenic double bond
	 */
	private static boolean isInStereoDoubleBond(MolAtom atom) {
		for (final MolBond bond : atom.getBondArray()) {
			if (bond.getType() == 2) {
				final int stereo = bond.calcStereo2();
				return stereo != 0;
			} // if bond is double bond
		} // for each bond
		return false;
	} // isInStereoDoubleBond(MolAtom)

	/** Removes multicenter attachment points that aren't attached to anything.
	 * Modifies the original!  
	 * @param	mol	a molecule
	 */
	private static void removeUnbondedMulticenterAtoms(Molecule mol) {
		final String SELF = "Normalize.removeUnbondedMulticenterAtoms: ";
		final Molecule origMol = mol.clone();
		boolean changed = false;
		final int numSgroups = mol.getSgroupCount();
		for (int sgNum = numSgroups; sgNum > 0; sgNum--) {
			final Sgroup sgroup = mol.getSgroup(sgNum - 1);
			if (sgroup.getSgroupType().equals(SgroupType.MULTICENTER)) {
				final MolAtom multicenterAttachPt = 
						((MulticenterSgroup) sgroup).getCentralAtom();
				if (multicenterAttachPt.getBondCount() == 0) {
					debugPrint(SELF + "ungrouping MulticenterSgroup whose "
							+ "attachment point has 0 bonds.");
					mol.ungroupSgroup(sgroup);
					changed = true;
				} // if is multicenter group
			} // if is multicenter group
		} // for each Sgroup
		if (changed) debugPrint(SELF + "before removing unbonded "
				+ "MulticenterSgroups:\n", origMol, 
				"\nafter removing unbonded MulticenterSgroups:\n", mol);
		else debugPrint(SELF + "no change in ", mol);
	} // removeUnbondedMulticenterAtoms(Molecule)

	/** Converts &eta;<sup>1</sup>,&eta;<sup>2</sup> allyl-metal complexes to
	 * &eta;<sup>3</sup> allyl-metal complexes, and 
	 * &eta;<sup>1</sup>,&eta;<sup>4</sup> dienyl-metal complexes to
	 * &eta;<sup>5</sup> dienyl-metal complexes. Modifies the original!
	 * @param	mol	the molecule
	 */
	public static void standardizeAllylAndDienyl(Molecule mol) {
		final String SELF = "Normalize.standardizeAllylAndDienyl: ";
		try {
			final Standardizer localStandardizer = 
					new Standardizer(AppConfig.allylDienylStdizer);
			localStandardizer.standardize(mol);
		} catch (IllegalArgumentException e) {
			Utils.alwaysPrint(SELF + "caught IllegalArgumentException trying "
					+ "to create copy of AppConfig.allylDienylStdizer or "
					+ "trying to standardize:\n", mol);
			e.printStackTrace();
		} catch (LicenseException e) {
			Utils.alwaysPrint(SELF + "no license found");
			e.printStackTrace();
		} // try
	} // standardizeAllylAndDienyl()

	/** Makes a copy, and converts every coordinate bond X&rarr;Y in the copy 
	 * not involving a multiatom group to X(+)-Y(-), and removes multicenter
	 * attachment points that aren't attached to anything. Always called 
	 * before a MolCompare method is run, which is why it doesn't need to 
	 * be done as part of standard normalization.
	 * @param	mol	a molecule
	 * @return	a copy of the molecule in which coordinate bonds are changed to
	 * regular single bonds
	 */
	public static Molecule normalizeCoordinateBonds(Molecule mol) {
		final Molecule molCopy = mol.clone();
		normalizeCoordinateBondsNoClone(molCopy);
		return molCopy;
	} // normalizeCoordinateBonds(Molecule)

	/** Converts every coordinate bond X&rarr;Y not involving a multiatom 
	 * group to X(+)-Y(-) and removes multicenter attachment points that aren't 
	 * attached to anything.  Modifies the original!  
	 * @param	mol	a molecule
	 */
	public static void normalizeCoordinateBondsNoClone(Molecule mol) {
		final String SELF = "Normalize.normalizeCoordinateBondsNoClone: ";
		for (final MolBond bond : mol.getBondArray()) {
			if (bond.getType() == MolBond.COORDINATE) {
				final MolAtom atom1 = bond.getAtom1();
				if (!ChemUtils.isMulticenterAtom(atom1)) {
					final MolAtom atom2 = bond.getAtom2();
					debugPrint(SELF + "converting bond ", 
							atom1, mol.indexOf(atom1), "->", atom2,
							mol.indexOf(atom2));
					atom1.setCharge(atom1.getCharge() + 1);
					atom2.setCharge(atom2.getCharge() - 1);
					bond.setType(1); // this step MUST follow setCharge()
					debugPrint(SELF + "bond is now ", atom1, 
							mol.indexOf(atom1), "-", atom2,
							mol.indexOf(atom2));
				} // if bond involves multiatom group
			} // if bond is coordinate
		} // for each bond
	} // normalizeCoordinateBondsNoClone(Molecule)

	/** Makes a copy, and converts &pi; complexes of multiatom groups into
	 * &sigma; complexes.  Called by MolCompare.areResonanceStructures().
	 * @param	mol	a molecule
	 * @return	a copy of the molecule in which &pi; complexes of multiatom
	 * groups are converted into &sigma; complexes
	 */
	public static Molecule resonateCoordinateBonds(Molecule mol) {
		final Molecule molCopy = mol.clone();
		resonateCoordinateBondsNoClone(molCopy);
		return molCopy;
	} // resonateCoordinateBonds(Molecule)

	/** Converts &pi; complexes of multiatom groups into &sigma; complexes.
	 * Modifies the original!  
	 * @param	mol	a molecule
	 */
	public static void resonateCoordinateBondsNoClone(Molecule mol) {
		final String SELF = "Normalize.resonateCoordinateBondsNoClone: ";
		debugPrint(SELF + "before processing: ", mol);
		for (final MolBond bond : mol.getBondArray()) {
			if (bond.getType() == MolBond.COORDINATE) {
				final MolAtom atom1 = bond.getAtom1();
				if (ChemUtils.isMulticenterAtom(atom1)) {
					piToSigma(mol, bond);
				} // if bond involves multiatom group
			} // if bond is coordinate
		} // for each bond
		debugPrint(SELF + "before aromatizing: ", mol);
		mol.aromatize(MoleculeGraph.AROM_GENERAL);
		debugPrint(SELF + "after processing: ", mol);
	} // resonateCoordinateBondsNoClone(Molecule)

	/** Converts a &pi; complex of a multiatom group into a series of &sigma;
	 * bonds.  Modifies the original!  
	 * @param	mol	the molecule
	 * @param	piBond	a bond between a multiatom group (via the multicenter
	 * attachment point) and a metal
	 */
	private static void piToSigma(Molecule mol, MolBond piBond) {
		final String SELF = "Normalize.piToSigma: ";
		final MolAtom mcAtom = piBond.getAtom1();
		final MolAtom metalAtom = piBond.getAtom2();
		final Sgroup multiatomGroup = 
				mol.findContainingMulticenterSgroup(mcAtom);
		final MolAtom[] groupAtoms = multiatomGroup.getAtomArray();
		final List<MolBond> groupBonds = 
				Arrays.asList(multiatomGroup.getBondArray());
		final List<MolBond> changedBonds = new ArrayList<MolBond>();
		for (final MolAtom groupAtom : groupAtoms) {
			mol.add(new MolBond(groupAtom, metalAtom));
			for (final MolBond groupAtomBond : groupAtom.getBondArray()) {
				if (groupBonds.contains(groupAtomBond) // pointer equality
						&& !changedBonds.contains(groupAtomBond)) { // pointer equality
					final int bondType = groupAtomBond.getType();
					if (bondType != 1) {
						groupAtomBond.setType(bondType == MolBond.AROMATIC
								? 1 : bondType - 1);
					} // if bond is not single
					changedBonds.add(groupAtomBond);
				} // if multiatom group contains bond and bond remains unchanged
			} // for each bond of the atom in the group
		} // for each atom of the group
		mol.ungroupSgroup(multiatomGroup);
	} // piToSigma(Molecule, MolBond)

	/** Disables external instantiation. */
	private Normalize() { }

} // Normalize
