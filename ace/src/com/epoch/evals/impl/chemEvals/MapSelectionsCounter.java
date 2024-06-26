package com.epoch.evals.impl.chemEvals;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.sss.SearchConstants;
import chemaxon.sss.search.MolSearch;
import chemaxon.sss.search.MolSearchOptions;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import chemaxon.struc.MoleculeGraph;
import chemaxon.struc.StereoConstants;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolCompare;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.chemEvals.chemEvalConstants.MapConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.VerifyException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** Calculate the grade from the number of response selections that match and
 * don't match the author's selections ... */
public class MapSelectionsCounter implements EvalInterface, MapConstants, 
		SearchConstants, StereoConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	/** Whether to check the enantiomer of the mapping of a structure. */
	private boolean checkEnant;
	/** Whether to aromatize both structures. */
	private boolean aromatize;
	/** The number of points to add for each match. */
	private String matchPtsStr;
	/** The number of points to subtract for each mismatch. */
	private String mismatchPtsStr;
	/** Name of the structure to be mapped.  Not currently used. */
	transient private String molName;

	/** Constructor. */
	public MapSelectionsCounter() {
		checkEnant = false;
		aromatize = true;
		matchPtsStr = "0.25";
		mismatchPtsStr = "0.25";
		molName = null;
	} // MapSelectionsCounter()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>checkEnant</code>/<code>aromatize</code>/<code>matchPtsStr</code>/<code>mismatchPtsStr</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public MapSelectionsCounter(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 4) {
			checkEnant = Utils.isPositive(splitData[0]);
			aromatize = Utils.isPositive(splitData[1]);
			matchPtsStr = splitData[2];
			mismatchPtsStr = splitData[3];
		} else {
			throw new ParameterException("MapSelectionsCounter ERROR: unknown input data "
					+ "'" + data + "'. ");
		}
	} // MapSelectionsCounter(String)

	/** Gets a string representation of data (other than a molecule) that this
	 * evaluator uses to evaluate a response.  Format:
	 * <code>checkEnant</code>/<code>aromatize</code>/<code>matchPtsStr</code>/<code>mismatchPtsStr</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(checkEnant ? 'Y' : 'N', aromatize ? "/Y/" : "/N/",
				matchPtsStr, '/', mismatchPtsStr);
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
		final StringBuilder words = Utils.getBuilder(
				"If the selections of the response");
		if (checkEnant) words.append(" or its enantiomer");
		if (!aromatize) words.append(", without aromatization,");
		Utils.appendTo(words, " match or mismatch the author's selections,"
				+ " add ", matchPtsStr, " or subtract ", mismatchPtsStr, 
				" points per response selection, respectively");
		return words.toString();
	} // toEnglish()

	/** Determines whether the response has the indicated mapping pattern.
	 * @param	response	a parsed response
	 * @param	authStruct	String representation of a molecule that the
	 * evaluator compares to the response
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authStruct) {
		final String SELF = "MapSelectionsCounter.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		Molecule authMol = null;
		try {
			authMol = MolImporter.importMol(authStruct);
		} catch (MolFormatException e) {
			Utils.alwaysPrint("Exception in " + SELF 
					+ " for author structure:\n" + authStruct);
			e.printStackTrace();
			evalResult.verificationFailureString = e.getMessage();
			return evalResult;
		} // try
		try {
			final Molecule respMol = response.moleculeObj.clone();
			debugPrintMRV(SELF + "original response:\n", respMol,
					"\noriginal author's structure:\n", authMol);
			if (MolCompare.matchExact(respMol, authMol)) {
				debugPrint(SELF + "before comparing map numbers, "
						+ "structures match.");
				final boolean anyHMapped = 
						hasMappedH(respMol) || hasMappedH(authMol);
				mapNormalize(respMol, anyHMapped);
				mapNormalize(authMol, anyHMapped);
				evalResult.calcScore = countMatches(respMol, authMol);
				evalResult.isSatisfied = true;
				evalResult.autoFeedback = new String[] {"Your grade is "
						+ "calculated by adding ***0.25*** points for "
						+ "each correct selection you made and "
						+ "subtracting ***0.25*** points for each "
						+ "incorrect selection you made."};
				evalResult.autoFeedbackVariableParts = 
						new String[] {matchPtsStr, mismatchPtsStr};
			} else {
				debugPrint(SELF + "without comparing map numbers, "
						+ "structures don't match.");
				evalResult.verificationFailureString =
						"Please draw the requested structure correctly. ";
			} // try
		} catch (Exception e1) {
			e1.printStackTrace();
			evalResult.verificationFailureString = Utils.toString(
					"Error in ", SELF, e1.getMessage());
		} // try
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Calculates a grade from the number of response selections 
	 * matching the author's minus the number of response selections not
	 * matching the author's.
	 * @param	respMol	the response molecule
	 * @param	authMol	the author's molecule
	 * @return	the grade 
	 * @throws	VerifyException	if anything goes wrong while counting the nuber
	 * of matches
	 */
	private double countMatches(Molecule respMol, Molecule authMol) 
			throws VerifyException {
		final String SELF = "MapSelectionsCounter.countMatches: ";
		final double matchPts = MathUtils.parseDouble(matchPtsStr);
		final double mismatchPts = MathUtils.parseDouble(mismatchPtsStr);
		double grade = 0.0;
		try {
			final MolSearchOptions searchOpts = 
					MapProperty.getSearchOpts(checkEnant);
			searchOpts.setExactBondMatching(true);
			searchOpts.setStereoModel(STEREO_MODEL_LOCAL);
					// required for diastereotopic atoms not to match
			searchOpts.setOrderSensitiveSearch(true); // want multiple results
			final MolSearch search = new MolSearch();
			search.setSearchOptions(searchOpts);
			search.setTarget(respMol);
			search.setQuery(authMol);
			while (true) {
				final int[] isomorphism = search.findNext();
				if (isomorphism == null) {
					debugPrint(SELF + "no more isomorphisms found.");
					break;
				} else debugPrint(SELF + "looking at isomorphism: {",
						Utils.join(isomorphism, 1), "}");
				double oneGrade = 0.0;
				for (int atomNum = 0; atomNum < isomorphism.length; atomNum++) {
					final MolAtom authAtom = authMol.getAtom(atomNum);
					final MolAtom respAtom = (isomorphism[atomNum] >= 0
							? respMol.getAtom(isomorphism[atomNum])
							: new MolAtom(1));
					final int refMap = authAtom.getAtomMap();
					final int respMap = respAtom.getAtomMap();
					if (respMap == 1) {
						final boolean match = refMap == 1;
						oneGrade += (match ? matchPts : -mismatchPts);
						debugPrint(SELF + "respMol atom ",
								respAtom, isomorphism[atomNum] + 1,
								" is selected, authMol atom ",
								authAtom, atomNum + 1, " is ", 
								match ? "as well" : "not",
								", accumulating grade of this isomorphism is ",
								oneGrade);
					} // if response atom is selected
				} // for each atom atomNum
				if (oneGrade < 0) oneGrade = 0.0;
				if (oneGrade > 1) oneGrade = 1.0;
				if (oneGrade > grade) {
					debugPrint(SELF + "grade of this iteration ", oneGrade, 
							" is an improvement over ", grade);
					grade = oneGrade;
				} else {
					debugPrint(SELF + "grade of this iteration ", oneGrade, 
							" is not an improvement over ", grade);
				} // if got a better grade
				if (grade >= 1.0) break; 
			} // loop until no more isomorphisms
			debugPrint(SELF + "returning ", grade);
			return grade;
		} catch (Exception e1) {
			e1.printStackTrace();
			throw new VerifyException("internal error in countMatches");
		}
	} // countMatches(Molecule, Molecule)

	/** Gets whether the molecule has any mapped H atoms.
	 * @param	mol	the molecule to normalize
	 * @return	true if the molecule has any mapped H atoms
	 */
	private boolean hasMappedH(Molecule mol) {
		boolean hasMappedH = false;
		for (final MolAtom atom : mol.getAtomArray()) {
			if (atom.getAtno() == 1 
					&& atom.getMassno() == 0 // no isotope specified
					&& atom.getAtomMap() != 0) {
				hasMappedH = true;
				break;
			} // if an atom is mapped already
		} // for each atom
		return hasMappedH;
	} // hasMappedH(Molecule)

	/** Maps selected atoms with map number 1 (assuming no atoms are already
	 * mapped), makes all H atoms explicit, and expands shortcut groups while 
	 * putting their map numbers on the attachment points.
	 * @param	mol	the molecule to normalize
	 * @param	anyHMapped	when true, explicitize the H atoms and convert them
	 * to pseudoatoms.
	 */
	private void mapNormalize(Molecule mol, boolean anyHMapped) {
		final String SELF = "MapSelectionsCounter.mapNormalize: ";
		final MolAtom[] atoms = mol.getAtomArray();
		// don't count selections when maps are present already
		boolean molUnmapped = true;
		for (final MolAtom atom : atoms) {
			if (atom.getAtomMap() != 0) {
				molUnmapped = false;
				break;
			} // if an atom is mapped already
		} // for each atom
		if (molUnmapped) { // convert selections to maps of 1
			for (final MolAtom atom : atoms) {
				if (atom.isSelected()) atom.setAtomMap(1);
			} // for each atom
		} // if no atoms are already mapped
		ChemUtils.ungroupRemapSGroups(mol);
		if (aromatize) mol.aromatize(MoleculeGraph.AROM_GENERAL);
		if (anyHMapped) {
			debugPrint(SELF + "mapped H atoms exist; adding explicit H atoms "
					+ "and converting them to pseudoatoms");
			ChemUtils.explicitizeHnoClone(mol); 
			convertHToPseudoNoClone(mol); // so diastereotopic H atoms won't match
		} else {
			ChemUtils.implicitizeH(mol); 
		} // if any H is mapped
		debugPrintMRV(SELF + "converted molecule to:\n", mol);
	} // mapNormalize(Molecule, boolean)

	/** Converts explicit 1H atoms to PSEUDO_H pseudoatoms that must match
	 * exactly in searches.  Modifies the original!
	 * @param	mol	a molecule with all explicit H atoms converted to 
	 * pseudoatoms
	 */
	private void convertHToPseudoNoClone(Molecule mol) {
		for (final MolAtom atom : mol.getAtomArray()) {
			if (atom.getAtno() == 1 && atom.getMassno() <= 1) {
				atom.setAtno(MolAtom.PSEUDO);
				atom.setAliasstr("PSEUDO_H");
			} // if atom is 1H
		} // for each atom
	} // convertHToPseudoNoClone(Molecule)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[MAPPED_COUNT]; } 
	/** Gets whether to check the enantiomer.
	 * @return	whether to check the enantiomer
	 */
	public boolean getCheckEnant() 				{ return checkEnant; }
	/** Sets whether to check the enantiomer.
	 * @param	enant	whether to check the enantiomer
	 */
	public void setCheckEnant(boolean enant)	{ checkEnant = enant; }
	/** Gets whether to aromatize the structures.
	 * @return	whether to aromatize the structures
	 */
	public boolean getAromatize() 				{ return aromatize; }
	/** Sets whether to aromatize the structures.
	 * @param	arom	whether to aromatize the structures
	 */
	public void setAromatize(boolean arom)		{ aromatize = arom; }
	/** Gets points awarded when a selection matches.
	 * @return	points awarded when a selection matches
	 */
	public String getMatchPtsStr() 				{ return matchPtsStr; }
	/** Sets points awarded when a selection matches.
	 * @param	pts	points awarded when a selection matches
	 */
	public void setMatchPtsStr(String pts)		{ matchPtsStr = pts; }
	/** Gets points subtracted when a selection mismatches.
	 * @return	points subtracted when a selection mismatches
	 */
	public String getMismatchPtsStr() 			{ return mismatchPtsStr; }
	/** Sets points subtracted when a selection mismatches.
	 * @param	pts	points subtracted when a selection mismatches
	 */
	public void setMismatchPtsStr(String pts)	{ mismatchPtsStr = pts; }
	/** Sets the molecule's name.  Not currently used.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 		{ this.molName = molName; }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 				{ return true; }

} // MapSelectionsCounter
