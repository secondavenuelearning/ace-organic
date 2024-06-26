package com.epoch.chem;

import chemaxon.standardizer.Standardizer;
import chemaxon.struc.CTransform3D;
import chemaxon.struc.DPoint3;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import chemaxon.struc.Molecule;
import chemaxon.struc.SelectionMolecule;
import chemaxon.marvin.calculations.TopologyAnalyserPlugin;
import chemaxon.marvin.plugin.PluginException;
import com.chemaxon.calculations.stereoisomers.StereoisomerEnumeration;
import com.chemaxon.calculations.stereoisomers.StereoisomerSettings;
import com.epoch.chem.chemConstants.ChemConstants;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** Contains stereoisomer generation and manipulation functions.  */
public final class StereoFunctions implements ChemConstants {

	private static void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

/* ****************** Stereoisomer generation methods ******************** */

	/** Converts an array of compounds with unspecified configurations at
	 * some sp<sup>3</sup> stereocenters and double bonds into all the 
	 * possible stereoisomers.
	 * @param	initCpds	an array of molecules that have stereocenters
	 * with unspecified configurations
	 * @return	an array of Molecules that have all possible combinations
	 * of configurations at previously unspecified stereocenters
	 */
	public static Molecule[] enumerateStereo(Molecule[] initCpds) {
		return Utils.molListToArray(enumerateStereo(
				Utils.molArrayToList(initCpds)));
	} // enumerateStereo(Molecule[])

	/** Converts a list of compounds with unspecified configurations at
	 * some sp<sup>3</sup> stereocenters and double bonds into all the 
	 * possible stereoisomers.
	 * @param	initCpds	a list of molecules that have stereocenters
	 * with unspecified configurations
	 * @return	a list of Molecules that have all possible combinations
	 * of configurations at previously unspecified stereocenters
	 */
	public static List<Molecule> enumerateStereo(List<Molecule> initCpds) {
		final String SELF = "StereoFunctions.enumerateStereo: ";
		final List<Molecule> configuredAll = new ArrayList<Molecule>();
		debugPrint(SELF + "Enumerating stereoisomers of ", initCpds); 
		for (final Molecule cpd : initCpds) {
			final List<Molecule> configured = enumerateStereo(cpd);
			try {
				if (isFlaggedRacemic(cpd) && isChiral(cpd)) {
					debugPrint(SELF, cpd, " is chiral and flagged racemic; "
							+ "is its enantiomer already in initial list ",
							initCpds, "?");
					final Molecule cpdEnt = ChemUtils.getMirror(cpd);
					boolean add = true;
					for (final Molecule initCpd : initCpds) {
						if (MolCompare.matchExact(initCpd, cpdEnt)) {
							add = false;
							break;
						} // if enantiomer is already in initial list
					} // for each compound in initial list
					if (add) {
						debugPrint(SELF, cpdEnt, " not found in initial "
								+ "list of compounds; adding it and its "
								+ "enumerated stereoisomers.");
						configured.addAll(enumerateStereo(cpdEnt));
						debugPrint(SELF + "Total of ", configured.size(),
								" stereoisomers enumerated from ", cpd);
					} else debugPrint(SELF + "initial list ", initCpds, 
							" already contains ", cpdEnt);
				} // if should get all enantiomers as well
			} catch (MolCompareException e) {
				Utils.alwaysPrint(SELF + "caught MolCompareException while "
						+ "getting chirality of ", cpd);
			} // try
			configuredAll.addAll(configured);
		} // for each cpd
		debugPrint(SELF + "Enumerated ", configuredAll.size(), 
				" stereoisomer(s) from ", initCpds.size(), 
				" initial compound(s): ", configuredAll);
		return configuredAll;
	} // enumerateStereo(List<Molecule>)

	/** Generates stereoisomers of a compound with sp<sup>3</sup> stereocenters
	 * and double bonds.
	 * @param	cpd	a molecule that has stereocenters and double bonds
	 * with unspecified configurations
	 * @return	a list of stereoisomers
	 */
	public static List<Molecule> enumerateStereo(Molecule cpd) {
		final String SELF = "StereoFunctions.enumerateStereo: ";
		convert0DTo2D(cpd);
		debugPrint(SELF + "after converting 0D to 2D: ", cpd);
		allWavyToCrissCross(cpd);
		debugPrint(SELF + "after converting wavy bonds to crisscross bonds: ", 
				cpd);
		final StereoisomerSettings settings = StereoisomerSettings.create()
				.setStereoisomerType(BOTH_STEREO_TYPES)
				.setProtectDoubleBondStereo(true)
				.setProtectTetrahedralStereo(true);
		final StereoisomerEnumeration enumeration = 
				new StereoisomerEnumeration(cpd, settings);
		final List<Molecule> stereoisomers = new ArrayList<Molecule>();
		while (enumeration.hasNext()) {
			final Molecule stereoMol = enumeration.next();
			stereoMol.setAbsStereo(false);
			stereoisomers.add(stereoMol);
		} // while there are generated stereoisomers
		debugPrint(SELF + "enumerated ", stereoisomers.size(), 
				" stereoisomer(s) from ", cpd, ": ", stereoisomers);
		return stereoisomers;
	} // enumerateStereo(Molecule)

	/** Gets if any of the molecules in the array is chiral, is explicitly 
	 * drawn as such, and does not have its RACEMIZE flag set to true.  For
	 * example, CCC(C)O (2-butanol) would not be found to be chiral by this 
	 * method, but CC[C@@H](C)O ((R)-2-butanol) would be.
	 * @param	mols	an array of molecules
	 * @return	true if any of the molecules is chiral and does not have its
	 * RACEMIZE flag set to true
	 * @throws	MolCompareException	if something goes wrong in matching 
	 * a mirror image to the original
	 */
	public static boolean anyAreChiralNonracemic(Molecule[] mols) 
			throws MolCompareException {
		for (final Molecule mol : mols) {
			if (isChiral(mol) && !isFlaggedRacemic(mol)) return true;
		} // for each molecule
		return false;
	} // anyAreChiralNonracemic(Molecule[])

	/** Gets if a molecule has been flagged as racemic.
	 * @param	mol	a molecule
	 * @return	true if the molecule has been flagged as racemic
	 */
	public static boolean isFlaggedRacemic(Molecule mol) {
		return Utils.isPositive(ChemUtils.getProperty(mol, RACEMIZE));
	} // isFlaggedRacemic(Molecule)

/* ****************** Bond angle, dihedral angle methods ******************** */

	/** Sets the dihedral angle in a molecule to a given amount in integral
	 * degrees.
	 * @param	molecule	a molecule
	 * @param	atomIndex	array of indices of four contiguous atoms that
	 * define a dihedral angle
	 * @param	setAngleDegrees	the angle in degrees
	 */
	public static void setDihedral(Molecule molecule, int[] atomIndex,
			int setAngleDegrees) {
		final double setAngleRadians = (double) setAngleDegrees * Math.PI / 180;
		setDihedral(molecule, atomIndex, setAngleRadians);
	} // setDihedral(Molecule, int[], int)

	/** Sets the dihedral angle in a molecule to a given amount in fractional
	 * radians.
	 * @param	molecule	a molecule
	 * @param	atomIndex	array of indices of four contiguous atoms that
	 * define a dihedral angle
	 * @param	setAngleRadians	the angle in radians
	 */
	public static void setDihedral(Molecule molecule, int[] atomIndex,
			double setAngleRadians) {
		// atomIndex contains four integers; bond rotation is
		// about bond between 2nd and 3rd atoms
		final MolAtom atom2 = molecule.getAtom(atomIndex[1]);
		final MolAtom atom3 = molecule.getAtom(atomIndex[2]);
		double moveAngle = 0;
		final MolAtom atom1 = molecule.getAtom(atomIndex[0]);
		final MolAtom atom4 = molecule.getAtom(atomIndex[3]);
		final double initDihedralRadians =
				VectorMath.calcDihedral(new MolAtom[] 
					{atom1, atom2, atom3, atom4}, "radians", 360);
		moveAngle = setAngleRadians - initDihedralRadians;
		rotateDihedral(molecule, atom2, atom3, moveAngle);
	} // setDihedral(Molecule, int[], double)

	/** Rotate around a bond by the given amount.  Rotation occurs about the
	 * atom0 &rarr; atom1 vector.  Modifies the molecule.
	 * @param	molecule	a molecule
	 * @param	atom0Index	index of one of the bond atoms
	 * @param	atom1Index	index of the other bond atom
	 * @param	moveAngleDegrees	the size of rotation in degrees
	 */
	public static void rotateDihedral(Molecule molecule, int atom0Index,
			int atom1Index, int moveAngleDegrees) {
		final MolAtom atom0 = molecule.getAtom(atom0Index);
		final MolAtom atom1 = molecule.getAtom(atom1Index);
		rotateDihedral(molecule, atom0, atom1, moveAngleDegrees);
	} // rotateDihedral(Molecule, int, int, int)

	/** Rotate around a bond by the given amount.  Rotation occurs about the
	 * atom0 &rarr; atom1 vector.  Modifies the molecule.
	 * @param	molecule	a molecule
	 * @param	atom0	one of the bond atoms
	 * @param	atom1	the other bond atom
	 * @param	moveAngleDegrees	the size of rotation in degrees
	 */
	public static void rotateDihedral(Molecule molecule, MolAtom atom0,
			MolAtom atom1, int moveAngleDegrees) {
		final double moveAngleRadians = (double) moveAngleDegrees * Math.PI / 180;
		rotateDihedral(molecule, atom0, atom1, moveAngleRadians);
	} // rotateDihedral(Molecule, MolAtom, MolAtom, int)

	/** Rotate around a bond by the given amount.  Rotation occurs about the
	 * atom0 &rarr; atom1 vector.  Modifies the molecule.
	 * @param	molecule	a molecule
	 * @param	atom0	one of the bond atoms
	 * @param	atom1	the other bond atom
	 * @param	moveAngle	the size of rotation in radians
	 */
	public static void rotateDihedral(Molecule molecule, MolAtom atom0,
			MolAtom atom1, double moveAngle) {
		final String SELF = "StereoFunctions.rotateDihedral: ";
		// set bond rotation vector, amount, origin
		// rotation vector is 1 -> 0 bond
		final DPoint3 atom0Loc = atom0.getLocation();
		final DPoint3 atom1Loc = atom1.getLocation();
		final CTransform3D fragRotate = new CTransform3D();
		fragRotate.setRotation(
				atom0Loc.x - atom1Loc.x,
				atom0Loc.y - atom1Loc.y,
				atom0Loc.z - atom1Loc.z,
				moveAngle
				);
		fragRotate.setRotationCenter(atom1Loc);
		// remove rotatable bond to allow selection of one fragment;
		// will restore soon
		final MolBond bond1To0 = atom1.getBondTo(atom0);
		molecule.removeBond(bond1To0);
		// find (newly) disconnected fragments
		SelectionMolecule[] fragments = molecule.findFrags();
		int numFragments = fragments.length;
		// restore the deleted bond(s)
		molecule.add(bond1To0);
		final List<MolBond> moreRemovedBonds = new ArrayList<MolBond>();
		final int atom0Num = molecule.indexOf(atom0);
		final int atom1Num = molecule.indexOf(atom1);
		try {
			while (numFragments < 2) {
				if (moreRemovedBonds.isEmpty())
					debugPrint(SELF + "disconnecting the rotatable bond "
							+ "isn't enough to find two fragments.");
				else if (moreRemovedBonds.size() == 1)
					debugPrint(SELF + "disconnecting the rotatable bond and "
							+ "one ring bond isn't enough to find two "
							+ "fragments.");
				else debugPrint(SELF + "disconnecting the rotatable bond and ", 
						moreRemovedBonds.size(), " ring bonds "
						+ "isn't enough to find two fragments.");
				// bond is in ring; need to disconnect a ring bond as well
				final TopologyAnalyserPlugin analyzer = 
						new TopologyAnalyserPlugin();
				analyzer.setMolecule(molecule);
				analyzer.run();
				final int[][] rings = analyzer.getRings();
				debugPrintMRV(SELF + "found ", rings.length, 
						" ring(s) in:\n", molecule);
				boolean bondRingFound = false;
				for (int ringNum = 0; ringNum < rings.length; ringNum++) {
					debugPrint(SELF + "ring ", ringNum + 1, " has these "
							+ "atoms: ", Utils.join(rings[ringNum], 1));
					final List<Integer> ringAtomNums =
							Utils.intArrayToList(rings[ringNum]);
					bondRingFound = false;
					if (ringAtomNums.contains(Integer.valueOf(atom0Num))
							&& ringAtomNums.contains(Integer.valueOf(atom1Num))) {
						bondRingFound = true;
						if (ringAtomNums.size() <= 3) {
							Utils.alwaysPrint(SELF + "can't rotate bond "
									+ "in three-membered ring: ", molecule);
							return;
						} // if three-membered ring
						// find a ring bond that is not adjacent to
						// the rotatable bond
						MolAtom[] otherBondAtoms = new MolAtom[2];
						MolBond ringBond = null;
						for (final int ringAtomNum : rings[ringNum]) { 
							// rule out atoms of original bond
							if (Utils.among(ringAtomNum, atom0Num, atom1Num))
								continue;
							final MolAtom ringAtom = 
									molecule.getAtom(ringAtomNum);
							if (otherBondAtoms[0] == null) {
								otherBondAtoms[0] = ringAtom;
							} else {
								ringBond = otherBondAtoms[0].getBondTo(ringAtom);
								if (ringBond != null) {
									otherBondAtoms[1] = ringAtom;
									break;
								} // if found ring bond
							} // if found first ring atom
						} // for each atom in the ring
						debugPrint(SELF + "found ring bond ", otherBondAtoms[0],
								molecule.indexOf(otherBondAtoms[0]) + 1,
								"-", otherBondAtoms[1],
								molecule.indexOf(otherBondAtoms[1]) + 1,
								" to break for rotation.");
						// add ring bond to list of removed bonds
						moreRemovedBonds.add(ringBond);
						// remove ring bond
						final MolAtom atom2 = ringBond.getAtom1();
							// ringBond is not null because there are rings.
						final MolAtom atom3 = ringBond.getAtom2();
						debugPrint(SELF + "removing ring bond ", 
								atom2, molecule.indexOf(atom2) + 1,
								"-", atom3, molecule.indexOf(atom3) + 1);
						molecule.removeBond(ringBond);
						debugPrintMRV(SELF + "after removing ring bond:\n",
								molecule);
						// remove rotatable bond
						molecule.removeBond(bond1To0);
						// find fragments
						fragments = molecule.findFrags();
						numFragments = fragments.length;
						debugPrint(SELF + "found ", numFragments, 
								" fragment(s).");
						// restore rotatable bond
						molecule.add(bond1To0);
						break;
					} // if the ring contains the rotatable bond
				} // for each ring in molecule
				// have we removed enough bonds to find fragments?
				if (numFragments >= 2 || rings.length == 0 
						|| !bondRingFound) break;
			} // while there is only one fragment
		} catch (PluginException e) {
			Utils.alwaysPrint(SELF + "PluginException caught while finding "
					+ "rings in ", molecule);
		} // try
		// restore ring bonds in opposite order of removal
		for (int bondNum = moreRemovedBonds.size() - 1; bondNum >= 0;
				bondNum--) {
			// ignore Jlint complaint about line above.  Raphael 11/2010
			final MolBond restoreBond = moreRemovedBonds.get(bondNum);
			final MolAtom atom2 = restoreBond.getAtom1();
			final MolAtom atom3 = restoreBond.getAtom2();
			debugPrint(SELF + "restoring ring bond ", bondNum + 1, ": ", 
					atom2, molecule.indexOf(atom2) + 1, "-", atom3, 
					molecule.indexOf(atom3) + 1);
			molecule.add(restoreBond);
		} // for each bond to be restored
		debugPrintMRV(SELF + "after restoring ring bonds:\n", molecule);
		debugPrint(SELF + "looking for the fragment containing atom ",
				atom1, atom1Num + 1, " to rotate.");
		final SelectionMolecule atom1Graph = new SelectionMolecule();
		atom1Graph.add(atom1);
		int fragNum = 0;
		for (final SelectionMolecule frag : fragments) {
			fragNum++;
			if (frag.contains(atom1Graph)) {
				debugPrint(SELF + "fragment ", fragNum, " contains atom ", 
						atom1, atom1Num + 1, "; rotating.");
				frag.transform(fragRotate);
				break;
			} // if fragment contains atom1
		} // for each fragment
		debugPrintMRV(SELF + "after rotating:\n", molecule);
	} // rotateDihedral(Molecule, MolAtom, MolAtom, double)

	/** Translates a group of atoms.
	 * @param	molecule	molecule part of which should be translated
	 * @param	atom	the atom that should be translated; its undisconnected 
	 * ligands will be translated as well
	 * @param	removeBond	bond whose removal will isolate the atoms to be 
	 * translated from those not to be translated
	 * @param	translateVector	the vector along which the atoms will be 
	 * translated
	 */
	public static void translateFrag(Molecule molecule, MolAtom atom, 
			MolBond removeBond, DPoint3 translateVector) {
		translateFrag(molecule, atom, new MolBond[] {removeBond}, 
				translateVector);
	} // translateFrag(Molecule, MolAtom, MolBond, DPoint3)

	/** Translates a group of atoms.
	 * @param	molecule	molecule part of which should be translated
	 * @param	atom	the atom that should be translated; its undisconnected 
	 * ligands will be translated as well
	 * @param	removeBonds	bonds whose removal will isolate the atoms to be 
	 * translated from those not to be translated
	 * @param	translateVector	the vector along which the atoms will be 
	 * translated
	 */
	public static void translateFrag(Molecule molecule, MolAtom atom, 
			MolBond[] removeBonds, DPoint3 translateVector) {
		final String SELF = "StereoFunctions.translateFrag: ";
		debugPrint(SELF + "translateVector = ", translateVector);
		debugPrint(SELF + "before translation, atom posn = ", 
				atom.getLocation());
		// remove bonds to isolate atom
		for (final MolBond bond : removeBonds) {
			molecule.removeBond(bond);
		} // for each bond to remove
		final SelectionMolecule[] molFrags = molecule.findFrags();
		// find and move fragment containing atom
		for (final SelectionMolecule molFrag : molFrags) {
			if (molFrag.contains(atom)) {
				// translate fragment
				final CTransform3D moveRingAtomFrag = new CTransform3D();
				moveRingAtomFrag.setTranslation(translateVector);
				molFrag.transform(moveRingAtomFrag);
				break;
			} // if fragment contains atom
		} // for each fragment
		// restore bonds
		for (final MolBond bond : removeBonds) {
			molecule.add(bond);
		} // for each bond to restore
		debugPrint(SELF + "after translation, atom posn = ", 
				atom.getLocation());
	} // translateFrag(Molecule, MolAtom, MolBond[], DPoint3)

	/** Rotates a group of atoms.
	 * @param	molecule	molecule part of which should be rotated
	 * @param	atom	the atom that should be rotated; its undisconnected 
	 * ligands will be rotated as well
	 * @param	removeBond	bond whose removal will isolate the atoms to be 
	 * rotated from those not to be rotated
	 * @param	rotateVector	the vector along which the atoms will be 
	 * rotated
	 * @param	rotateCenter	the point around which the atoms will be 
	 * rotated
	 * @param	angle	angle in radians by which the atoms will be rotated
	 */
	public static void rotateFrag(Molecule molecule, MolAtom atom, 
			MolBond removeBond, DPoint3 rotateVector, 
			DPoint3 rotateCenter, double angle) {
		rotateFrag(molecule, atom, new MolBond[] {removeBond}, rotateVector, 
				rotateCenter, angle);
	} // rotateFrag(Molecule, MolAtom, MolBond, DPoint3, DPoint3, double)

	/** Rotates a group of atoms.
	 * @param	molecule	molecule part of which should be rotated
	 * @param	atom	the atom that should be rotated; its undisconnected 
	 * ligands will be rotated as well
	 * @param	removeBonds	bonds whose removal will isolate the atoms to be 
	 * rotated from those not to be rotated
	 * @param	rotateVector	the vector along which the atoms will be 
	 * rotated
	 * @param	rotateCenter	the point around which the atoms will be 
	 * rotated
	 * @param	angle	angle in radians by which the atoms will be rotated
	 */
	public static void rotateFrag(Molecule molecule, MolAtom atom, 
			MolBond[] removeBonds, DPoint3 rotateVector, 
			DPoint3 rotateCenter, double angle) {
		final String SELF = "StereoFunctions.rotateFrag: ";
		debugPrint(SELF + "rotating atom ", atom, molecule.indexOf(atom) + 1,
				" with location ", atom.getLocation(), " about vector ",
				rotateVector, " by ", angle * 180 / Math.PI, 
				" degrees, with center of rotation at ", rotateCenter);
		// remove bonds to isolate atom
		for (final MolBond bond : removeBonds) {
			molecule.removeBond(bond);
		} // for each bond to remove
		final SelectionMolecule[] molFrags = molecule.findFrags();
		// find and move fragment containing atom
		for (final SelectionMolecule molFrag : molFrags) {
			if (molFrag.contains(atom)) {
				// rotate fragment
				final CTransform3D moveRingAtomFrag = new CTransform3D();
				moveRingAtomFrag.setRotation(rotateVector.x,
						rotateVector.y, rotateVector.z, angle);
				moveRingAtomFrag.setRotationCenter(rotateCenter);
				molFrag.transform(moveRingAtomFrag);
				break;
			} // if fragment contains atom
		} // for each fragment
		// restore bonds
		for (final MolBond bond : removeBonds) {
			molecule.add(bond);
		} // for each bond to restore
		debugPrint(SELF + "after rotation, atom ", atom, 
				molecule.indexOf(atom) + 1, " has location ", 
				atom.getLocation());
	} // rotateFrag(Molecule, MolAtom, MolBond[], DPoint3, DPoint3, double)

	/** Gets the angle (in degrees) defined by three atoms.
	 * @param	angleAtoms	atoms A-B-C
	 * @return	the angle in degrees (0 through 180)
	 */
	public static double getAngle(MolAtom[] angleAtoms) {
		return getAngle(angleAtoms, 0);
	} // getAngle(MolAtom[])

	/** Gets the positive or negative angle defined by three atoms in the xy
	 * plane.
	 * @param	angleAtoms	atoms A-B-C
	 * @param	flags	whether the response should be signed, in degrees
	 * @return	the angle about B defined by A and C (0 through 180 or &pi;, 
	 * or -180 or -&pi; through 180 or &pi;)
	 */
	public static double getAngle(MolAtom[] angleAtoms, int flags) {
		final DPoint3 atomALoc = angleAtoms[0].getLocation();
		final DPoint3 atomBLoc = angleAtoms[1].getLocation();
		final DPoint3 atomCLoc = angleAtoms[2].getLocation();
		final DPoint3 vecBToA = VectorMath.diff(atomALoc, atomBLoc);
		final DPoint3 vecBToC = VectorMath.diff(atomCLoc, atomBLoc);
		double angle = VectorMath.angle(vecBToA, vecBToC);
		if ((flags & SIGNED) != 0) angle *= VectorMath.angleSign(
				new DPoint3[] {atomALoc, atomBLoc, atomCLoc});
		if ((flags & RADIANS) == 0) angle *= 180 / Math.PI;
		return angle;
	} // getAngle(MolAtom[], int)

	/** Scales a molecule's coordinates.
	 * @param	molecule	molecule to be rescaled
	 * @param	factor	scaling factor
	 */
	public static void rescale(Molecule molecule, double factor) {
		final String SELF = "StereoFunctions.rescale: ";
		final CTransform3D scaleFn = new CTransform3D();
		scaleFn.setScale(factor);
		molecule.transform(scaleFn);
	} // rescale(Molecule, double)

/* *********** Bond stereochemistry representation methods ************* */

	/** Gets a value for bond stereochemistry.  Possible values:
	 * <ul>
	 * <li>0, MolBond.UP, MolBond.DOWN, MolBond.WAVY for single bonds;
	 * <li>0, MolBond.CIS, MolBond.TRANS, CRISSCROSS for double bonds.
	 * </ul>
	 * Double bonds return CRISSCROSS if mixture is indicated by wavy bond
	 * to ligand, by ligand geometry, or by criss-cross double bond.
	 * @param	bond	the bond whose stereochemistry is being queried
	 * @return	the stereochemistry value
	 */
	public static int getBondStereo(MolBond bond) {
		int flags = getBondStereoFlags(bond);
		if (bond.getType() == 2 && flags == 0) {
			final Molecule parent = (Molecule) bond.getParent();
			final Molecule mol = parent.clone();
			allWavyToCrissCross(mol);
			final MolBond bondClone = mol.getBond(parent.indexOf(bond));
			flags = getBondStereoFlags(bondClone);
			if (flags == 0) flags = bond.calcStereo2();
		} // if unflagged double bond
		return flags;
	} // getBondStereo(MolBond)

	/** Gets single- or double-bond stereochemistry flags.  Possible values:
	 * <ul>
	 * <li>0, MolBond.UP, MolBond.DOWN, MolBond.WAVY for single bonds;
	 * <li>0, CRISSCROSS for double bonds.
	 * </ul>
	 * @param	bond	the bond whose stereochemistry is being queried
	 * @return	the stereochemistry flags
	 */
	public static int getBondStereoFlags(MolBond bond) {
		return (bond.getFlags() & MolBond.STEREO_MASK);
	} // getBondStereoFlags(MolBond)

	/** Sets single- or double-bond stereochemistry flags.
	 * @param	bond	the bond whose stereochemistry is being set
	 * @param	stereoType	stereochemistry flag: MolBond.UP,
	 * MolBond.DOWN, MolBond.WAVY for single bonds, CRISSCROSS for double
	 */
	public static void setBondStereoFlags(MolBond bond, int stereoType) {
		bond.setFlags((bond.getFlags() & ~MolBond.STEREO_MASK) | stereoType);
	} // setBondStereoFlags(MolBond, int)

	/** Removes all single- and double-bond stereochemistry flags.
	 * @param	bond	the bond whose stereochemistry is being removed
	 */
	public static void removeBondStereoFlags(MolBond bond) {
		setBondStereoFlags(bond, 0);
	} // removeBondStereoFlags(MolBond)

	/** Converts criss-cross representation of stereorandom double bonds into
	 * wavy-bond-to-substituent representation.  Modifies the original!
	 * @param	molecule	a molecule
	 */
	public static void allCrissCrossToWavy(Molecule molecule) {
		allCrissCrossToWavy(new Molecule[] {molecule});
	} // allCrissCrossToWavy(Molecule)

	/** Converts criss-cross representation of stereorandom double bonds into
	 * wavy-bond-to-substituent representation.  Modifies the original!
	 * @param	mols	array of molecules
	 */
	public static void allCrissCrossToWavy(Molecule[] mols) {
		// final String ERROR = "ChemUtils.allCrissCrossToWavy: ";
		if (mols != null) /* try */ {
			final Standardizer stdizer =
					new Standardizer("convertdoublebonds:wiggly");
			for (final Molecule mol : mols) {
				if (mol != null) stdizer.standardize(mol);
			} // for each molecule
		} // if there are molecules
	} // allCrissCrossToWavy(Molecule[])
			
	/** Converts wavy-bond-to-substituent representation of
	 * stereorandom double bonds into criss-cross representation.
	 * @param	molecule	a molecule
	 */
	public static void allWavyToCrissCross(Molecule molecule) {
		// final String ERROR = "ChemUtils.allWavyToCrissCross: ";
		if (molecule != null) /* try */ {
			final Standardizer stdizer =
					new Standardizer("convertdoublebonds:crossed");
			stdizer.standardize(molecule);
		} // if there is a molecule
	} // allWavyToCrissCross(Molecule)

/* ****************** Other methods ******************** */

	/** Checks if the parity of a stereocenter cannot be determined from the
	 * wedge bonds or if any wedge bonds originate at nonstereogenic atoms.
	 * @param	origMol	molecule to check for ambiguous or unnecessary wedge 
	 * bonds
	 * @param	disallowSuperfluousWedges	when true, throw exception when a
	 * wedge bond originates at a nonstereogenic atom
	 * @param	prefersJava	true if the user prefers using Java MarvinSketch
	 * over MarvinJS
	 * @throws	WedgeException	if there's an ambiguous or unnecessary wedge 
	 * bond
	 */
	public static void checkForWedgesFromNonstereo(Molecule origMol, 
			boolean disallowSuperfluousWedges, boolean prefersJava) 
			throws WedgeException {
		final Molecule mol = origMol.clone();
		final String VIEW_NUMS = (prefersJava 
				? " Choose View &rarr; Misc &rarr; Atom Numbers to view "
					+ "the atom numbers."
				: " To view the atom numbers, press the View Settings button "
					+ "(the gear symbol to the right of the H&plusmn; button "
					+ "on the northern toolbar), check the Index atoms box, "
					+ "and press Ok. ");
		allWavyToCrissCross(mol);
		final int PARITY_EITHER = MolAtom.PARITY_EITHER;
		for (final MolBond bond : mol.getBondArray()) {
			if (bond.getType() == 1 && getBondStereo(bond) != 0) {
				final MolAtom parityAtom = bond.getAtom1();
				final int parityAtomNum = mol.indexOf(parityAtom);
				final int atomParity = mol.getParity(parityAtomNum);
				final int atomLocalParity = mol.getLocalParity(parityAtomNum);
				if ((atomParity == 0 && disallowSuperfluousWedges)
						|| (atomParity == PARITY_EITHER 
							&& atomLocalParity == 0)) {
					int numWedges = 0;
					boolean isStereogenicAllene = false;
					for (final MolBond parityAtomBond : parityAtom.getBondArray()) {
						if (parityAtomBond.getType() == 2) {
							MolAtom parityAtomLig = parityAtomBond.getAtom1();
							if (parityAtomLig == parityAtom) 
								parityAtomLig = parityAtomBond.getAtom2();
							final int ligNum = mol.indexOf(parityAtomLig);
							if (mol.getParity(ligNum) != 0) {
								isStereogenicAllene = true;
								break;
							} // if double-bonded atom has parity
						} else if (parityAtomBond.getType() == 1 
								&& getBondStereo(parityAtomBond) != 0) {
							numWedges++;
						} // if bond is a wedge
					} // for each bond of the atom
					final int numBonds = parityAtom.getBondCount();
					if (!isStereogenicAllene) throw new WedgeException(
							numBonds < 4 ? "A wedge bond begins at atom "
								+ "***1***, but this atom, like most "
								+ "atoms with fewer than four bonds, is "
								+ "not stereogenic." + VIEW_NUMS
							: numWedges == 1 ? "A wedge bond begins at "
								+ "atom ***1***, but this atom is not "
								+ "stereogenic." + VIEW_NUMS
							: "At least two wedge bonds begin at atom "
								+ "***1***, but either this atom is not "
								+ "stereogenic, or ACE cannot determine "
								+ "its configuration from the way you "
								+ "have drawn the wedge bonds." 
								+ VIEW_NUMS,
							parityAtomNum + 1);
				} // if atom has no parity
			} // if is wedge bond
		} // for each bond of the molecule
	} // checkForWedgesFromNonstereo(Molecule, boolean)

	/** Gets if a molecule is chiral and is explicitly drawn as such.  For 
	 * example, CCC(C)O (2-butanol) would not be found to be chiral by this 
	 * method, but CC[C@@H](C)O ((R)-2-butanol) would be.
	 * @param	mol	a molecule
	 * @return	true if the molecule is chiral
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean isChiral(Molecule mol) throws MolCompareException {
		return !MolCompare.matchExact(mol, ChemUtils.getMirror(mol));
	} // isChiral(Molecule)

	/** Converts a zero-dimensional compound (no atomic coordinates) such as one
	 * produced by Reactor into a two-dimensional compound.
	 * @param	cpd	the compound
	 */
	public static void convert0DTo2D(Molecule cpd) {
		if (cpd != null && cpd.getDim() == 0) {
			debugPrint("StereoFunctions.convert0DTo2D: converting ", cpd, 
					" from 0D to 2D");
			ChemUtils.clean2D(cpd); // don't use setDim(), it messes up coordinates
		}
	} // convert0DTo2D(Molecule)

	/** Converts a compound into three dimensions.
	 * @param	cpd	the compound
	 */
	public static void convertTo3D(Molecule cpd) {
		if (cpd != null && cpd.getDim() != 3) {
			ChemUtils.clean3D(cpd, "S{fine}");
		}
	} // convertTo3D(Molecule)

	/** Fixes ambiguous stereochemistry derived from more than one wedge bond
	 * connected to a stereocenter, assuming only one wedge originates at the
	 * stereocenter.  Modifies the original!
	 * @param	cpd	a molecule
	 */
	public static void stereoClean(Molecule cpd) {
		// debugPrintMRV("Cleaning stereo of:\n" + cpd);
		final int numAtoms = cpd.getAtomCount();
		final List<Integer> stereocenters = new ArrayList<Integer>();
		final List<Integer> parities = new ArrayList<Integer>();
		for (int atmIdx = 0; atmIdx < numAtoms; atmIdx++) {
			final MolAtom atm = cpd.getAtom(atmIdx);
			final List<MolBond> stereoBonds = new ArrayList<MolBond>();
			for (final MolBond ligBond : atm.getBondArray()) {
				final int stereo = getBondStereoFlags(ligBond);
				if (ligBond.getType() == 1 && (stereo & MolBond.WAVY) != 0) {
					stereoBonds.add(ligBond);
				} // if bond is single and has stereo
			} // for each atom ligand
			if (stereoBonds.size() <= 1) continue;
			// atom is participating in more than one stereoBond;
			// find out which one originates at this atom
			for (int stereoBondNum = stereoBonds.size() - 1; 
					stereoBondNum >= 0; stereoBondNum--) {
				final MolBond stereoBond = stereoBonds.get(stereoBondNum);
				if (stereoBond.getAtom1() == atm) {
					stereoBonds.remove(stereoBondNum);
				} // if the originating atom is atm
			} // for each stereoBond
			final Molecule cpdNew = cpd.clone();
			// convert stereoBonds not originating at atm in cpd to type 1, get parity
			for (final MolBond stereoBond : stereoBonds) {
				final int bondIdx = cpd.indexOf(stereoBond);
				final MolBond bondNew = cpdNew.getBond(bondIdx);
				removeBondStereoFlags(bondNew);
			} // for each unoriginating stereoBond
			// store atom index and parity of stereocenter
			final int parity = cpdNew.getParity(atmIdx);
			stereocenters.add(Integer.valueOf(atmIdx));
			parities.add(Integer.valueOf(parity));
		} // for each atom in cpd
		// modify the original cpd
		for (int atomNum = 0; atomNum < stereocenters.size(); atomNum++) {
			try {
				final int atmIdx = stereocenters.get(atomNum).intValue();
				final int parity = parities.get(atomNum).intValue();
				cpd.setParity(atmIdx, parity);
			} catch (ArrayIndexOutOfBoundsException e) {
				Utils.alwaysPrintMRV("StereoFunctions.stereoClean: caught "
						+ "ArrayIndexOutOfBoundsException when trying "
						+ "to set parity of an atom in:\n", cpd);
			} // try
		} // for each stereocenter
	} // stereoClean(Molecule)

	/** Disables external instantiation. */
	private StereoFunctions() { }

} // StereoFunctions
