package com.epoch.evals.impl.chemEvals;

import static com.epoch.evals.impl.chemEvals.chemEvalConstants.SixMembRingConstants.*;
import chemaxon.struc.DPoint3;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import chemaxon.struc.Molecule;
import com.epoch.chem.chemConstants.ChemConstants;
import com.epoch.chem.MolCompare;
import com.epoch.chem.StereoFunctions;
import com.epoch.chem.VectorMath;
import com.epoch.constants.FormatConstants;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.VerifyException;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utility class used by <code>ConformChair</code> and <code>Is2DChair</code> classes. */
final class SixMembRing implements ChemConstants, FormatConstants {

	private static void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	/** The molecule containing the ring. */
	private final Molecule respMol;
	/** Array of six consecutive atoms in a ring. */
	transient private final MolAtom[] ringAtoms = new MolAtom[6];
	/** Bond angles about each ring atom in both 0-to-180 and -180-to-180
	 * degree ranges. */
	private final int[][] ringAngles = new int[2][6];
	/** Members of ringAtoms identified as having the most acute, most 
	 * obtuse, or intermediate angles. */
	transient private final int[][] angleTypeRingNums = new int[3][2];
	/** For each ringAtom, whether its ring angle is identified as being 
	 * among the most acute, most obtuse, or intermediate. */
	transient private final int[] ringAtomAngleTypes = new int[6];

	/** Constructor.
	 * @param	respMolecule	the molecule
	 * @throws	VerifyException	if the compound does not represent a properly
	 * drawn chair
	 */
	public SixMembRing(Molecule respMolecule) throws VerifyException {
		respMol = respMolecule;
		int ringAtomCt = 0;
		for (int atomNum = 0; atomNum < respMol.getAtomCount(); atomNum++) {
			final MolAtom atom = respMol.getAtom(atomNum);
			// Deselect all atoms in the response molecule; we need to
			// select ones ourselves as we inspect particular bonds.
			atom.setSelected(false);
			final int mapNo = atom.getAtomMap();
			if (mapNo != 0) {
				ringAtomCt++;
				if (mapNo <= 6 && ringAtoms[mapNo - 1] == null) {
					ringAtoms[mapNo - 1] = atom;
				} else {
					debugPrint("Uh-oh.  Atom ", atom, atomNum + 1,
							" has map number ", mapNo,
							", and ringAtoms[", mapNo - 1,
							"] is already occupied by atom ",
							respMol.indexOf(ringAtoms[mapNo - 1]) + 1);
					throw new VerifyException("The response has incorrectly "
							+ "mapped atoms; there must be exactly 6 "
							+ "numbered 1 through 6.  Follow the "
							+ "instructions for drawing your response.");
				} // if map number is OK
			} // if the atom is mapped
		} // for each atom
		if (ringAtomCt == 0) {
			debugPrint("SixMembRing: no mapped atoms found; "
					+ "looking for a six-membered ring.");
			final List<Integer> ringAtomIds =
					MolCompare.findAndMarkCyclohex(respMol);
			ringAtomCt = ringAtomIds.size();
			if (ringAtomCt == 6) {
				for (int atomNum = 0; atomNum < 6; atomNum++) {
					final int atomId = ringAtomIds.get(atomNum);
					ringAtoms[atomNum] = respMol.getAtom(atomId);
				} // for each ring atom
			} // if six marked atoms found
		} // if no marked atoms found
		if (ringAtomCt != 6) throw new VerifyException(
				"ACE cannot find a saturated "
				+ "six-membered ring in your response.  Please follow "
				+ "the instructions for drawing your response.");
		if (respMol.getDim() == 2) {
			getRingAngles();
		} // if ring is drawn in 2D
	} // SixMembRing(Molecule)

	/** Calculates and characterizes the bond angles of the ring.
	 * @throws	VerifyException	if the compound does not represent a properly
	 * drawn chair
	 */
	private void getRingAngles() throws VerifyException {
		final String SELF = "SixMembRing.getRingAngles: ";
		for (int atomNum = 0; atomNum < 6; atomNum++) {
			final int angle = getAngle(getRingAtom(atomNum - 1), 
					getRingAtom(atomNum), getRingAtom(atomNum + 1),
					SIGNED);
			ringAngles[POS_NEG][atomNum] = angle;
			ringAngles[POS][atomNum] = Math.abs(angle);
		} // for each ring atom
		debugPrint(SELF + "ring bond angles of atoms ", ringAtoms, 
				" are ", ringAngles[POS_NEG]);
		// store each angle twice: to code by ring atom number and to sort
		final List<Integer> anglesByRingPosn = new ArrayList<Integer>();
		final int[] sortAngles = new int[6];
		for (int ringAtomNum = 0; ringAtomNum < 6; ringAtomNum++) {
			sortAngles[ringAtomNum] = ringAngles[POS][ringAtomNum];
			anglesByRingPosn.add(Integer.valueOf(sortAngles[ringAtomNum]));
		} // for each ring atom
		Arrays.sort(sortAngles);
		final String NOT_CHAIR_SHAPE = "Your six-membered ring does not "
				+ "appear to have the shape of a chair. ";
		if (sortAngles[1] == sortAngles[2] || sortAngles[3] == sortAngles[4]) {
			throw new VerifyException(NOT_CHAIR_SHAPE 
					+ "Try moving the higher or lower ring atoms to the left "
					+ "or right so that the higher ones are not directly above "
					+ "the lower ones.");
		} // if angles not sorted into 3 distinct groups of two
		int sumAcuteIntermedSigns = 0;
		int sumObtuseSigns = 0;
		final Integer PLACEHOLDER = Integer.valueOf(1000);
		// store type of angle of each ring atom
		for (final int angleType : ANGLE_TYPES) {
			final int[] ringNumsOfType = angleTypeRingNums[angleType];
			for (int atomNum = 0; atomNum < ringNumsOfType.length; atomNum++) {
				final int arrayNum = angleType * 2 + atomNum;
				final int angle = sortAngles[arrayNum];
				// get ring atom number corresponding to this angle
				final int ringAtomNum = 
						anglesByRingPosn.indexOf(Integer.valueOf(angle));
				anglesByRingPosn.set(ringAtomNum, PLACEHOLDER); 
						// so can't be found again
				ringNumsOfType[atomNum] = ringAtomNum;
				ringAtomAngleTypes[ringAtomNum] = angleType;
				// add sign of the angle
				final int signedAngle = ringAngles[POS_NEG][ringAtomNum];
				final int sign = MathUtils.sign(signedAngle);
				if (angleType == OBTUSE) sumObtuseSigns += sign;
				else sumAcuteIntermedSigns += sign;
				final MolAtom ringAtom = getRingAtom(ringAtomNum);
				debugPrint(SELF + "ring atom ", ringAtom, 
						respMol.indexOf(ringAtom) + 1, " has angle ", 
						angle, ", ranked ", 6 - arrayNum, " in size.");
			} // for each atomNum
			if (getMod6(ringNumsOfType[0] + 3) != ringNumsOfType[1]) {
				throw new VerifyException(NOT_CHAIR_SHAPE);
			} // if atoms in each group are not across from each other
		} // for each angleType
		// sums of signs of angles should be 2 and -4, or -2 and 4
		final int OK_PROD_SIGNS_SUM = -8;
		debugPrint(SELF + "sumObtuseSigns = ", sumObtuseSigns,
				", sumAcuteIntermedSigns = ", sumAcuteIntermedSigns);
		if (sumObtuseSigns * sumAcuteIntermedSigns != OK_PROD_SIGNS_SUM) {
			throw new VerifyException(NOT_CHAIR_SHAPE);
		} // if chair angles are wrong
	} // getRingAngles()

	/** Gets an atom of the ring.
	 * @param	ringAtomNum	the number of the desired atom of the ring
	 * @return	the desired atom of the ring
	 */
	MolAtom getRingAtom(int ringAtomNum) {
		return ringAtoms[getMod6(ringAtomNum)];
	} // getRingAtom(int)

	/** Converts an integer into a nonnegative mod 6 integer.
	 * @param	num	an integer
	 * @return	an integer between 0 and 5
	 */
	private int getMod6(int num) {
		return MathUtils.getMod(num, 6);
	} // getMod6(int)

	/** Gets the angle (in degrees) formed by atoms A-B-C.
	 * @param	atom1	first atom defining the angle
	 * @param	atom2	second atom defining the angle
	 * @param	atom3	third atom defining the angle
	 * @return the angle (in degrees) formed by atoms A-B-C, as an int
	 */
	private int getAngle(MolAtom atom1, MolAtom atom2, MolAtom atom3) {
		return getAngle(atom1, atom2, atom3, NOT_SIGNED);
	} // getAngle(MolAtom, MolAtom, MolAtom)

	/** Gets the angle (in degrees) formed by atoms A-B-C.
	 * @param	atom1	first atom defining the angle
	 * @param	atom2	second atom defining the angle
	 * @param	atom3	third atom defining the angle
	 * @param	getSigned	flag for whether the angle should be signed
	 * @return the angle (in degrees) formed by atoms A-B-C, as an int
	 */
	private int getAngle(MolAtom atom1, MolAtom atom2, MolAtom atom3, 
			int getSigned) {
		final MolAtom[] atoms = new MolAtom[] {atom1, atom2, atom3};
		return MathUtils.roundToInt(StereoFunctions.getAngle(atoms, getSigned));
	} // getAngle(MolAtom, MolAtom, MolAtom, int)

	/** Converts the 2D chair representation to a representation with wedges.
	 * @return	the molecule with wedges
	 * @throws	VerifyException	if the ligand can't be identified as axial or
	 * equatorial; the message consists of the atom and its index in the
	 * molecule, with the rest of the message to be added later by
	 * setAutoFeedback().
	 */
	public Molecule makeWedges() throws VerifyException {
		final String SELF = "SixMembRing.makeWedges: ";
		final List<int[]> orientns = new ArrayList<int[]>();
		for (final int angleType : ANGLE_TYPES) {
			final int numAngleTypeAtoms = angleTypeRingNums[angleType].length;
			for (int atomNum = 0; atomNum < numAngleTypeAtoms; atomNum++) {
				final int ringAtomNum = angleTypeRingNums[angleType][atomNum];
				orientns.addAll(getOrientations(ringAtomNum));
			} // for each atomNum
		} // for each angleType
		final boolean[] areAxialUp = getAreAxialUp();
		moveObtuse();
		// members of each array in orientns
		for (final int[] orientn : orientns) {
			final int ringAtomNum = orientn[RING_ATOM_NUM];
			final MolAtom ringAtom = getRingAtom(ringAtomNum);
			final int ligIndex = orientn[LIG_MOL_INDEX];
			final MolAtom substituent = respMol.getAtom(ligIndex);
			final MolBond bond = ringAtom.getBondTo(substituent);
			// pointer equality:
			if (bond.getAtom1() != ringAtom) bond.swap();
			final boolean isAxial = orientn[ORIENTN] == AXIAL;
			final int bondType = (areAxialUp[ringAtomNum] == isAxial 
					? MolBond.UP : MolBond.DOWN);
			StereoFunctions.setBondStereoFlags(bond, bondType);
			debugPrint(SELF + "ring atom ", ringAtom, 
					respMol.indexOf(ringAtom) + 1, " has substituent ",
					substituent, ligIndex + 1, " with ", 
					(isAxial ? "axial" :  "equatorial"),
					" orientation that points ", 
					(bondType == MolBond.UP ? "up" : "down"), ".");
		} // for each orientation
		debugPrintMRV(SELF + "respMol after moving obtuse atoms "
				+ "and adding wedges:\n", respMol);
		return respMol;
	} // makeWedges()

	/** Gets the orientations of all ligands of a ring atom.
	 * @param	ringAtomNum	index in the ring of a ring atom
	 * @return	list of between 0 and 2 three-member integer arrays, where
	 * each array contains the ring atom number, the index in the molecule 
	 * of the ring atom's ligand, and whether the atom is axial or equatorial.
	 * @throws	VerifyException	if the ligand can't be identified as axial or
	 * equatorial; the message consists of the atom and its index in the
	 * molecule, with the rest of the message to be added later by
	 * setAutoFeedback().
	 */
	private List<int[]> getOrientations(int ringAtomNum) 
			throws VerifyException {
		final String SELF = "SixMembRing.getOrientations: ";
		final List<int[]> orientns = new ArrayList<int[]>();
		final MolAtom ringAtom = getRingAtom(ringAtomNum);
		final MolAtom prevRingAtom = getRingAtom(ringAtomNum - 1);
		final MolAtom nextRingAtom = getRingAtom(ringAtomNum + 1);
		final MolAtom[] ligands = ringAtom.getLigands();
		for (final MolAtom ligand : ligands) {
			// pointer equality:
			if (ligand != prevRingAtom && ligand != nextRingAtom) {
				final int[] orientn = new int[3];
				orientn[RING_ATOM_NUM] = ringAtomNum;
				orientn[LIG_MOL_INDEX] = respMol.indexOf(ligand);
				orientn[ORIENTN] = 
						getOrientation(ringAtomNum, ligand);
				orientns.add(orientn);
			} // if ligands are not ring atoms
		} // for each ligand
		return orientns;
	} // getOrientations(int)

	/** Gets the orientation of a ring atom's ligand.
	 * @param	ringAtomNum	index in the ring of a ring atom
	 * @param	ligand	the ligand
	 * @return	AXIAL or EQUATORIAL
	 * @throws	VerifyException	if the ligand can't be identified as axial or
	 * equatorial; the message consists of the atom and its index in the
	 * molecule, with the rest of the message to be added later by
	 * setAutoFeedback().
	 */
	int getOrientation(int ringAtomNum, MolAtom ligand) throws VerifyException {
		final String SELF = "SixMembRing.getOrientation: ";
		final int axOrEq = (respMol.getDim() == 3
				? get3DOrientation(ringAtomNum, ligand)
				: get2DOrientation(ringAtomNum, ligand));
		if (axOrEq == INDETERMINATE) {
			throw new VerifyException(ligand.getSymbol() + "("
					+ (respMol.indexOf(ligand) + 1) + ")");
		} // if is not axial or equatorial
		return axOrEq;
	} // getOrientation(int, MolAtom)

	/** Gets the orientation of a ring atom's ligand. The orientation is
	 * determined by the angle of the ring bonds and the relative angles of 
	 * the ring-ligand bond to the ring bonds.
	 * @param	ringAtomNum	index in the ring of a ring atom
	 * @param	ligand	the ligand
	 * @return	AXIAL, EQUATORIAL, or INTERMEDIATE
	 */
	private int get2DOrientation(int ringAtomNum, MolAtom ligand) {
		final String SELF = "SixMembRing.get2DOrientation: ";
		int axOrEq = INDETERMINATE;
		final MolAtom ringAtom = getRingAtom(ringAtomNum);
		final int angleType = ringAtomAngleTypes[ringAtomNum];
		final int prevRingAtomNum = getMod6(ringAtomNum - 1);
		final MolAtom prevRingAtom = getRingAtom(prevRingAtomNum);
		final MolAtom nextRingAtom = getRingAtom(ringAtomNum + 1);
		final int angleLigRingPrev = getAngle(ligand, ringAtom, prevRingAtom);
		final int angleLigRingNext = getAngle(ligand, ringAtom, nextRingAtom);
		final int ringAngle = ringAngles[POS][ringAtomNum];
		final int prevAngleType = ringAtomAngleTypes[prevRingAtomNum];
		if (angleType == ACUTE) {
			final boolean prevIsObtuse = prevAngleType == OBTUSE;
			final int angleLigRingObtuse = (prevIsObtuse
					? angleLigRingPrev : angleLigRingNext);
			final int angleLigRingInterm = (prevIsObtuse
					? angleLigRingNext : angleLigRingPrev);
			// deg around 90 or 180 considered indeterminate
			final int sumRingInterm = ringAngle + angleLigRingInterm;
			final int sumRingObtuse = ringAngle + angleLigRingObtuse;
			if (areNearlyEqual(sumRingInterm, angleLigRingObtuse)
					&& angleLigRingInterm >= 90 - TOLERANCE2D) {
				axOrEq = EQUATORIAL;
			} else if (areNearlyEqual(sumRingObtuse, angleLigRingInterm)
					&& angleLigRingObtuse >= 90 - TOLERANCE2D) {
				axOrEq = AXIAL;
			} // if axial or equatorial
			debugPrint(SELF + "substituent ", ligand,
					respMol.indexOf(ligand) + 1, 
					" of most-acute ring atom ", ringAtom, 
					respMol.indexOf(ringAtom) + 1, 
					" with ring index ", ringAtomNum + 1,
					" and angle ", ringAngle,
					" has angles ", angleLigRingObtuse, 
					" and ", angleLigRingInterm, " to neighboring "
					+ "obtuse and intermediate atoms, is ", 
					(axOrEq == AXIAL ? "axial"
						: axOrEq == EQUATORIAL ? "equatorial"
						: "of indeterminate orientation"), ".");
		} else { // obtuse or intermediate
			final boolean prevIsAcute = prevAngleType == ACUTE;
			final int angleLigRingAcute = (prevIsAcute
					? angleLigRingPrev : angleLigRingNext);
			final int angleLigRingNonacute = (prevIsAcute
					? angleLigRingNext : angleLigRingPrev);
			if ((angleType == OBTUSE 
						&& angleLigRingAcute >= 90 - TOLERANCE2D 
						&& angleLigRingNonacute >= 90 - TOLERANCE2D)
					|| (angleType == INTERMEDIATE
						&& angleLigRingAcute >= 90 + TOLERANCE2D 
						&& angleLigRingNonacute >= 90 + TOLERANCE2D
						&& angleLigRingAcute > angleLigRingNonacute)) {
				axOrEq = AXIAL;
				debugPrint(SELF + "substituent ", ligand,
						respMol.indexOf(ligand) + 1, " of ",
						angleType == OBTUSE ? "most-obtuse" 
							: "intermediate-angle", " ring atom ", 
						ringAtom, respMol.indexOf(ringAtom) + 1, 
						" with ring index ", ringAtomNum + 1,
						" has angles ", angleLigRingAcute, 
						" and ", angleLigRingNonacute, " to acute and "
						+ "nonacute ring atoms; both are >= 90, ",
						angleType == INTERMEDIATE 
							? "and former > latter, " : "",
						"so substituent is axial.");
			} else if ((angleType == OBTUSE 
						&& areNearlyEqual(
							angleLigRingAcute + angleLigRingNonacute, 
							ringAngle)
						&& angleLigRingAcute < angleLigRingNonacute)
					|| (angleType == INTERMEDIATE 
						&& angleLigRingNonacute <= 180 - TOLERANCE2D
						&& areNearlyEqual(
							angleLigRingAcute + ringAngle,
							angleLigRingNonacute))) {
				axOrEq = EQUATORIAL;
			} // if angle is in equatorial range
			if (axOrEq != AXIAL) debugPrint(SELF + "substituent ", 
					ligand, respMol.indexOf(ligand) + 1, " of ",
					angleType == OBTUSE ? "most-obtuse" 
						: "intermediate-angle", " ring atom ", 
					ringAtom, respMol.indexOf(ringAtom) + 1, 
					" with ring index ", ringAtomNum + 1,
					" has angles ", angleLigRingAcute, 
					" and ", angleLigRingNonacute, 
					" to acute and other neighboring ring atoms; one is "
					+ "< 90, ", angleType == INTERMEDIATE
						? "or the former < latter, " : "", 
					"so it isn't axial, but ",
					axOrEq == EQUATORIAL ? "" : "nor is it true that ",
					angleType == OBTUSE ? 
					"both are < 90 deg and angle to acute is smaller, "
						: "bond to acute is inside obtuse angle formed by "
							+ "bond to ligand and bond to obtuse, ",
					"so substituent is ",
					axOrEq == EQUATORIAL ? "equatorial."
						: "neither equatorial nor axial.");
		} // if acute
		return axOrEq;
	} // get2DOrientation(int, MolAtom)

	/** Gets the orientation of the ring's ligand.
	 * @param	ringAtomNum	the index of an atom in the ring
	 * @param	ligand	the ligand of the atom in the ring
	 * @return	AXIAL, EQUATORIAL, or INTERMEDIATE
	 */
	private int get3DOrientation(int ringAtomNum, MolAtom ligand) {
		final MolAtom atom1 = ligand;
		final MolAtom atom2 = getRingAtom(ringAtomNum);
		final MolAtom atom3 = getRingAtom(ringAtomNum - 1);
		final MolAtom atom4 = getRingAtom(ringAtomNum - 2);
		final double angle = VectorMath.calcDihedral(new MolAtom[] 
				{atom1, atom2, atom3, atom4});
		debugPrint("ConformChair.getOrientation: dihedral angle = ", angle);
		return (isAxial(angle) ? AXIAL
				: isEquatorial(angle) ? EQUATORIAL
				: INDETERMINATE);
	} // get3DOrientation(int, MolAtom)

	/** Is the given angle typical of an axial group, within a tolerance?
	 * Sign on the angle must be correct for this to work properly.
	 * @param	angle	the measured angle in degrees
	 * @return	true if the angle is typical of an axial group
	 */
	private boolean isAxial(double angle) {
		return (Math.abs(AXIAL_ANGLE - angle) <= TOLERANCE3D);
	}

	/** Is the given angle typical of an equatorial group, within a tolerance?
	 * Sign on the angle must be correct for this to work properly.
	 * @param	angle	the measured angle in degrees
	 * @return	true if the angle is typical of an equatorial group
	 */
	private boolean isEquatorial(double angle) {
		return (Math.abs(EQUATORIAL_ANGLE - angle) <= TOLERANCE3D);
	}

	/** Gets if one number is nearly equal to another.  Used to compare integers
	 * obtained from rounding doubles.
	 * @param	num1	one number
	 * @param	num2	another number
	 * @return	true if num1 == num2 or num2 &plusmn; 1
	 */
	private boolean areNearlyEqual(int num1, int num2) {
		return MathUtils.inRange(num1, new int[] {num2 - 1, num2 + 1});
	} // areNearlyEqual(int, int)

	/** Gets for each ring atom whether its axial substituent is pointing up.
	 * @return	array of booleans corresponding to each ring atom, true if the
	 * ring atom's axial substituent is up
	 */
	private boolean[] getAreAxialUp() {
		final String SELF = "SixMembRing.getAreAxialUp: ";
		final MolAtom acute0 = getRingAtom(angleTypeRingNums[ACUTE][0]);
		final MolAtom acute1 = getRingAtom(angleTypeRingNums[ACUTE][1]);
		final double acute0x = acute0.getLocation().x;
		final double acute1x = acute1.getLocation().x;
		final boolean facesLeft = facesLeft();
		final int whichAcuteIsUp = (facesLeft == (acute0x > acute1x) ? 0 : 1);
		final int acuteUpNum = angleTypeRingNums[ACUTE][whichAcuteIsUp];
		final MolAtom acuteUp = getRingAtom(acuteUpNum);
		debugPrint(SELF + "acute-angled ring atom ", 
				acuteUp, respMol.indexOf(acuteUp) + 1, 
				" in ", (facesLeft ? "left-" : "right-"),
				"facing ring has an up-pointing axial substituent.");
		final boolean[] areAxialUp = new boolean[6];
		int ringAtomNum = acuteUpNum;
		boolean isAxialUp = true;
		while (true) {
			final MolAtom ringAtom = getRingAtom(ringAtomNum);
			debugPrint(SELF + "for ring atom ", 
					ringAtom, respMol.indexOf(ringAtom) + 1, 
					" with ring index ", ringAtomNum + 1, 
					", axial is ", (isAxialUp ? "up" : "down"), ".");
			areAxialUp[ringAtomNum] = isAxialUp;
			ringAtomNum = getMod6(ringAtomNum + 1);
			if (ringAtomNum == acuteUpNum) break;
			else isAxialUp = !isAxialUp;
		} // while true
		return areAxialUp;
	} // getAreAxialUp()

	/** Determines whether the ring faces left or right. 
	 * @return	true if the ring faces left
	 */
	private boolean facesLeft() {
		final String SELF = "SixMembRing.facesLeft: ";
		final int acuteAtomNum = angleTypeRingNums[ACUTE][0];
		final MolAtom acuteAtom = getRingAtom(acuteAtomNum);
		final int prevRingAtomNum = getMod6(acuteAtomNum - 1);
		final MolAtom prevRingAtom = getRingAtom(prevRingAtomNum);
		final MolAtom nextRingAtom = getRingAtom(acuteAtomNum + 1);
		final boolean prevObtuse = 
				ringAtomAngleTypes[prevRingAtomNum] == OBTUSE;
		final MolAtom obtuseAtom = (prevObtuse ? prevRingAtom : nextRingAtom);
		final MolAtom intermedAtom = (prevObtuse ? nextRingAtom : prevRingAtom);
		final double obAcIntAngle = getAngle(obtuseAtom, acuteAtom, 
				intermedAtom, SIGNED);
		final boolean facesLeft = obAcIntAngle > 0;
		debugPrint(SELF + "obAcIntAngle around acute atom ", acuteAtom, 
				acuteAtomNum, " is ", obAcIntAngle, "; chair faces ",
				(facesLeft ? "left" : "right"), ".");
		return facesLeft;
	} // facesLeft()

	/** Translates the most-obtuse atoms so all angles are positive or negative,
	 * rotates its ligands out from within the ring. */
	private void moveObtuse() {
		final String SELF = "SixMembRing.moveObtuse: ";
		final int[] obtuseNums = angleTypeRingNums[OBTUSE];
		for (final int obtuseNum : obtuseNums) {
			final MolAtom obtuse = getRingAtom(obtuseNum);
			final MolAtom beforeObtuse = getRingAtom(obtuseNum - 1);
			final MolAtom afterObtuse = getRingAtom(obtuseNum + 1);
			final DPoint3 obtuseLocn = obtuse.getLocation();
			final DPoint3 beforeObtuseLocn = beforeObtuse.getLocation();
			final DPoint3 afterObtuseLocn = afterObtuse.getLocation();
			// get the vector along which to translate the obtuse atom
			final DPoint3 midBeforeAfter = 
					VectorMath.midpoint(afterObtuseLocn, beforeObtuseLocn);
			final DPoint3 obtuseToMid = 
					VectorMath.diff(midBeforeAfter, obtuseLocn);
			final DPoint3 obtuseTranslator = 
					VectorMath.scalarProd(obtuseToMid, 2);
			// get bonds to isolate obtuse atom and ligands from rest of ring
			final MolBond[] ringBonds = new MolBond[]
					{obtuse.getBondTo(beforeObtuse),
					obtuse.getBondTo(afterObtuse)};
			translateFrag(obtuse, ringBonds, obtuseTranslator);
			// NOTE: after transform, location values are obsolete
			// move axial group if it is inside ring
			final MolAtom[] ligands = obtuse.getLigands();
			for (final MolAtom ligand : ligands) {
				// pointer equality:
				if (ligand != beforeObtuse && ligand != afterObtuse) {
					final int angle01 = getAngle(beforeObtuse, obtuse, ligand);
					final int angle12 = getAngle(afterObtuse, obtuse, ligand);
					final int angle02 = ringAngles[POS][obtuseNum];
					if (areNearlyEqual(angle01 + angle12, angle02)) {
						final MolBond ringToLigBond = 
								obtuse.getBondTo(ligand);
						final DPoint3 rotator = new DPoint3(0, 0, 1);
						rotateFrag(ligand, ringToLigBond, rotator, 
								obtuse.getLocation(), 180);
					} // if ligand is in ring
				} // if ligand is not ring atom
			} // for each ligand
		} // for each obtuse atom
	} // moveObtuse()

	/** Translates a group of atoms.
	 * @param	atom	the atom that should be translated; its undisconnected 
	 * ligands will be translated as well
	 * @param	removeBonds	bonds whose removal will isolate the atoms to be 
	 * translated from those not to be translated
	 * @param	translateVector	the vector along which the atoms will be 
	 * translated
	 */
	private void translateFrag(MolAtom atom, MolBond[] removeBonds, 
			DPoint3 translateVector) {
		StereoFunctions.translateFrag(respMol, atom, removeBonds, 
				translateVector);
	} // translateFrag(MolAtom, MolBond[], DPoint3)

	/** Rotates a group of atoms.
	 * @param	atom	the atom that should be rotated; its undisconnected 
	 * ligands will be rotated as well
	 * @param	removeBond	bond whose removal will isolate the atoms to be 
	 * rotated from those not to be rotated
	 * @param	rotateVector	the vector along which the atoms will be 
	 * rotated
	 * @param	rotateCenter	the point around which the atoms will be 
	 * rotated
	 * @param	angle	angle by which the atoms will be rotated
	 */
	private void rotateFrag(MolAtom atom, MolBond removeBond, 
			DPoint3 rotateVector, DPoint3 rotateCenter, int angle) {
		StereoFunctions.rotateFrag(respMol, atom, removeBond, 
				rotateVector, rotateCenter, ((double) angle) * Math.PI / 180);
	} // rotateFrag(MolAtom, MolBond, DPoint3, DPoint3, int)

	/** Gets the molecule of the ring.
	 * @return	the molecule of the ring
	 */
	Molecule getRespMol() {
		return respMol;
	} // getRespMol()

	/** Adds translatable autofeedback, possibly with variable parts, 
	 * to a OneEvalResult.
	 * @param	evalResult	OneEvalResult to modify
	 * @param	msg	verification failure string that may contain the variable 
	 * part of an error message
	 */
	static void setAutoFeedback(OneEvalResult evalResult, String msg) {
		final Pattern ATOM = Pattern.compile("[A-Z][a-z]*\\([\\d]+\\)");
		final Matcher matcher = ATOM.matcher(msg);
		if (matcher.matches()) {
			debugPrint("SixMembRing.setAutoFeedback: "
					+ "autoFeedbackVariableParts[0] set to: ", msg);
			evalResult.autoFeedback = new String[]
					{"Atom ***C(1)*** cannot be identified as axial or "
					+ "equatorial. (To see the atom numbers, users should "
					+ "press the <b>View settings</b> button in the "
					+ "northern toolbar (its icon is a gear), select "
					+ "<b>Index atoms</b>, and press OK.) "};
			evalResult.autoFeedbackVariableParts = new String[] {msg};
			evalResult.verificationFailureString = "ACE could not "
					+ "interpret your drawing of a chair.";
		} else {
			evalResult.verificationFailureString = msg;
		}
	} // setAutoFeedback(OneEvalResult)

} // SixMembRing
