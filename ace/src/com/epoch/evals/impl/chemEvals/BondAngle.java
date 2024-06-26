package com.epoch.evals.impl.chemEvals;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.sss.SearchConstants;
import chemaxon.sss.search.MolSearch;
import chemaxon.sss.search.MolSearchOptions;
import chemaxon.sss.search.SearchException;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolCompare;
import com.epoch.chem.Normalize;
import com.epoch.chem.StereoFunctions;
import com.epoch.constants.FormatConstants;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.CompareNums;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.VerifyException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/**	If the bond angle in the response {is, is not} {=, &lt;, &gt;} <i>n</i> ... */
public class BondAngle extends CompareNums 
		implements EvalInterface, FormatConstants, SearchConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	/** Angle to compare against (in degrees). */
	private int authAngle;
	/** Tolerance in measuring the angle (in degrees). */
	private int tolerance;
	/** Name of the Lewis structure molecule. */
	private static final String molName = null; // dummy; never used

	/** Constructor. */
	public BondAngle() {
		authAngle = 180;
		tolerance = 15;
		setOper(NOT_EQUALS); // inherited from CompareNums
	} // BondAngle()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>oper</code>/<code>authAngle</code>/<code>tolerance</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public BondAngle(String data) throws ParameterException {
		debugPrint("initializing BondAngle: codedData = ", data);
		final String[] splitData = data.split("/");
		if (splitData.length >= 3) {
			setOper(Utils.indexOf(SYMBOLS, splitData[0]));
			authAngle = MathUtils.parseInt(splitData[1]);
			tolerance = MathUtils.parseInt(splitData[2]);
		}
		if (splitData.length < 3 || getOper() == -1) {
			throw new ParameterException("BondAngle ERROR: unknown input data "
					+ "'" + data + "'. ");
		} // if there are not at least three tokens
	} // BondAngle(String)

	/** Gets a string representation of data (other than a molecule) that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>oper</code>/<code>authAngle</code>/<code>tolerance</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(SYMBOLS[getOper()], '/',
				authAngle, '/', tolerance);
	} // getCodedData()

	/** Gets an English-language description of this evaluator.
	 * @param	qDataTexts	the text of question data of this question, if any;
	 * not used, but required by interface
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts, boolean forPermissibleSM) {
		return toEnglish();
	} // toEnglish(String[], boolean)

	/** Gets an English-language description of this evaluator.
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish() {
		return Utils.toString("If the indicated bond angle ",
				// molName == null ? "" : Utils.getBuilder("in ", molName),
				" is", OPER_ENGLISH[LESSER][getOper()],
				tolerance == 0 ?  Utils.getBuilder(authAngle, "&deg;")
				: Utils.getBuilder('(', authAngle, " &plusmn; ",
					tolerance, ")&deg;"));
	} // toEnglish()

	/** Determines whether the indicated bond angle in the response has the
	 * indicated value.
	 * @param	response	a parsed response
	 * @param	authStruct	String representation of a molecule that the
	 * evaluator compares to the response
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authStruct) {
		final OneEvalResult evalResult = new OneEvalResult();
		if (response.normalized == null) {
			response.normalized =
					Normalize.normalize(response.moleculeObj);
		} // if there is not yet a normalized response Molecule
		Molecule authMol = null;
		try {
			authMol = MolImporter.importMol(authStruct);
		} catch (MolFormatException e) {
			System.out.println("MolFormatException in BondAngle.java for "
					+ "author structure:\n" + authStruct);
			e.printStackTrace();
			evalResult.verificationFailureString = e.getMessage();
			return evalResult;
		} // try
		// convert shortcut groups to pseudoatoms and remap attachment
		// points to avoid MolSearch.findNext() bug in JChem 5.0.6 and before
		ChemUtils.ungroupRemapSGroups(authMol);
		Normalize.normalizeNoClone(authMol);
		try {
			// check if legitimate to be calling this routine at all
			final Molecule respMol = response.normalized;
			if (MolCompare.matchExact(respMol, authMol)) {
				evalResult.isSatisfied =
						matchRespAngle(respMol, authMol);
			} else {
				evalResult.verificationFailureString =
						"Please draw the requested structure correctly.";
			} // if the compounds are the same
		} catch (VerifyException e1) {
			evalResult.verificationFailureString =
							e1.getMessage();
		} catch (Exception e2) {
			e2.printStackTrace();
			evalResult.verificationFailureString =
					"Error in BondAngle.isResponseMatching: "
							+ e2.getMessage();
		}
		return evalResult;
	} // isResponseMatching(Response, String)
	
	/** Compares the bond angle formed by the designated atoms in the author's
	 * molecule to the angle of the corresponding atoms in the response.
	 * @param	respMol	the response molecule
	 * @param	authMol	the author's molecule
	 * @return	whether the angles compare in the way designated by the coded
	 * data
	 * @throws	VerifyException	if something goes wrong when trying to find the
	 * bond whose angle to measure
	 */
	private boolean matchRespAngle(Molecule respMol, Molecule authMol)
			throws VerifyException {
		final String SELF = "BondAngle.matchRespAngle: ";
		try {
			final int[] angleAtomNums = getAngleAtomNums(authMol);
			final MolSearchOptions mySearchOpts = 
					new MolSearchOptions(DUPLICATE);
			final MolSearch mySearch = new MolSearch();
			mySearch.setSearchOptions(mySearchOpts);
			mySearch.setTarget(respMol);
			mySearch.setQuery(authMol);
			debugPrintMRV(SELF + "respMol:\n", respMol, "authMol:\n", 
					authMol, "angle of authMol atoms ",
					authMol.getAtom(angleAtomNums[0]),
					angleAtomNums[0] + 1, "-",
					authMol.getAtom(angleAtomNums[1]),
					angleAtomNums[1] + 1, "-",
					authMol.getAtom(angleAtomNums[2]),
					angleAtomNums[2] + 1, " in respMol, authAngle = ", 
					authAngle, ", tolerance = ", tolerance, ", oper = ",
					getOper(), " (", SYMBOLS[getOper()], ").");
			int ct = 0;
			while (true) { 
				final int[] isomorphism = mySearch.findNext();
				if (isomorphism == null) break;
				ct++;
				final MolAtom[] respAngleAtoms = new MolAtom[3];
				for (int atNum = 0; atNum < 3; atNum++) {
					if (isomorphism[angleAtomNums[atNum]] == -1) {
						// unlikely due to response normalization
						throw new VerifyException(
								"ACE could not measure a bond angle involving "
								+ "an H atom in your structure. To avoid "
								+ "getting this message, draw out all H "
								+ "atoms explicitly.");
					} else {
						respAngleAtoms[atNum] =
							respMol.getAtom(isomorphism[angleAtomNums[atNum]]);
					} // if one of the matched atoms is implicit H
				} // for each angle atom
				final double respAngle =
						StereoFunctions.getAngle(respAngleAtoms);
				debugPrint(SELF, "isomorphism ", ct, ": respAngle = ", 
						respAngle, ", authAngle = ", authAngle);
				final boolean isSat = compare(respAngle, authAngle, tolerance);
				if (isSat) {
					debugPrint(SELF + "resp angle ", respAngle, " of ",
							respAngleAtoms[0],
							isomorphism[angleAtomNums[0]] + 1, "-",
							respAngleAtoms[1],
							isomorphism[angleAtomNums[1]] + 1, "-",
							respAngleAtoms[2],
							isomorphism[angleAtomNums[2]] + 1,
							" satisfies condition, returning true.");
					return true;
				} // if the angle satisfies the condition
			} // while there are isomorphisms
			debugPrint(SELF, ct,
					" isomorphism(s) found; condition not satisfied.");
			return false;
		} catch (SearchException e) {
			e.printStackTrace();
			throw new VerifyException(
					"Internal error in BondAngle.matchRespAngle.");
		}
	} // matchRespAngle(Molecule, Molecule)
	
	/** Gets the atom indices of the three mapped atoms in the author's
	 * molecule.
	 * @param	authMol	the author's molecule
	 * @return	array of atom indices of the three mapped atoms
	 * @throws	VerifyException	if there are not exactly three mapped atoms
	 * mapped as 1, 2, and 3; doesn't check for contiguity
	 */
	private int[] getAngleAtomNums(Molecule authMol) throws VerifyException {
		int[] angleAtomNums = new int[3];
		boolean[] found = new boolean[3];
		for (final MolAtom atom : authMol.getAtomArray()) {
			final int map = atom.getAtomMap();
			if (Utils.among(map, 1, 2, 3)) {
				if (found[map - 1]) {
					throw new VerifyException(
							"Author error in BondAngle.getAngleAtomNums: "
							+ "author structure can have only one map "
							+ "number " + map + ".");
				} else {
					angleAtomNums[map - 1] = authMol.indexOf(atom);
					found[map - 1] = true;
				} // if already found an atom with this map
			} else if (map != 0) {
				throw new VerifyException(
						"Author error in BondAngle.getAngleAtomNums: "
						+ "author structure can have map numbers 1-3 only.");
			} // if map number is 1-3
		} // for each atom in author molecule
		for (int mapNum = 0; mapNum < 3; mapNum++) {
			if (!found[mapNum]) {
				throw new VerifyException(
						"Author error in BondAngle.getAngleAtomNums: "
						+ "author structure must include map numbers 1-3.");
			} // if didn't find an atom with this map
		} // for each map number 1-3
		return angleAtomNums;
	} // getAngleAtomNums(Molecule)
	
	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[BOND_ANGLE]; } 
	/** Gets the value of the.angle to compare.
	 * @return	value of the angle to compare
	 */
	public int getAuthAngle() 				{ return authAngle; }
	/** Sets the value of the angle to compare.
	 * @param	num	value to which to set the angle to compare
	 */
	public void setAuthAngle(int num)		{ authAngle = num; }
	/** Gets the tolerance of the value of the.angle to compare.
	 * @return	tolerance of the value of the angle to compare
	 */
	public int getTolerance() 				{ return tolerance; }
	/** Sets the tolerance of the value of the angle to compare.
	 * @param	tol	tolerance of the value of the angle to compare
	 */
	public void setTolerance(int tol) 		{ tolerance = tol; }

} // BondAngle
