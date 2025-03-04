package com.epoch.evals.impl.chemEvals;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.sss.SearchConstants;
import chemaxon.sss.search.MolSearch;
import chemaxon.sss.search.MolSearchOptions;
import chemaxon.sss.search.SearchException;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import chemaxon.struc.MoleculeGraph;
import chemaxon.struc.StereoConstants;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolCompare;
import com.epoch.chem.MolCompareException;
import com.epoch.chem.WavyBondMatcher;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.chemEvals.chemEvalConstants.MapConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.VerifyException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the response {selection, mapping, mapping pattern} is {exactly, at least} 
 * the author's mapping ...  */
public class MapProperty implements EvalInterface, MapConstants, 
		SearchConstants, StereoConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	/** Whether the evaluator is satisfied by a match (true) or a mismatch
	 * (false).  */
	private boolean isPositive;
	/** Whether the reference mapping is all of or a subset of the response 
	 * mapping. */
	private int oper;
	/** Whether the map numbers of the response should be the same as or
	 * just have the same pattern as the reference map numbers. */
	private boolean patternOnly;
	/** Whether to check the enantiomer of the mapping of a structure;
	 * inconsequential when <code>patternOnly</code> is on. */
	private boolean checkEnant;
	/** Whether to aromatize both structures. */
	private boolean aromatize;
	/** Name of the structure to be mapped.  Not currently used. */
	transient private String molName;

	/** Constructor. */
	public MapProperty() {
		isPositive = false;
		oper = ATLEAST;
		patternOnly = false;
		checkEnant = false;
		aromatize = true;
		molName = null;
	} // MapProperty()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>oper</code>/<code>patternOnly</code>/<code>checkEnant</code>/<code>aromatize</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public MapProperty(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 4) {
			isPositive = Utils.isPositive(splitData[0]);
			oper = MathUtils.parseInt(splitData[1]);
			patternOnly = Utils.isPositive(splitData[2]);
			checkEnant = Utils.isPositive(splitData[3]);
			aromatize = splitData.length < 5 || Utils.isPositive(splitData[4]);
		} else {
			throw new ParameterException("MapProperty ERROR: unknown input data "
					+ "'" + data + "'. ");
		}
	} // MapProperty(String)

	/** Gets a string representation of data (other than a molecule) that this
	 * evaluator uses to evaluate a response.  Format:
	 * <code>isPositive</code>/<code>oper</code>/<code>patternOnly</code>/<code>checkEnant</code>/<code>aromatize</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive ? "Y/" : "N/", oper, 
				patternOnly ? "/Y" : "/N", checkEnant ? "/Y" : "/N",
				aromatize ? "/Y" : "/N");
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
				"If the selection or mapping ",
				patternOnly ? "pattern" : "numbers",
				" of the response");
		if (checkEnant) {
			Utils.appendTo(words, ", or ", patternOnly 
					? "its " : "their ", "enantiomer,");
		} // check both enantiomers
		words.append(patternOnly ? " is " : " are ");
		if (!isPositive) words.append("not ");
		if (!aromatize) words.append("without aromatization ");
		Utils.appendTo(words, oper == ATLEAST ? "at least " 
				: "exactly ", Utils.isEmpty(molName) ? "as shown"
				: Utils.getBuilder("as in ", molName));
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
		final String SELF = "MapProperty.isResponseMatching: ";
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
				final boolean result = (patternOnly 
						? matchMapPattern(respMol, authMol)
						: matchMapNumbers(respMol, authMol));
				evalResult.isSatisfied = result == isPositive;
			} else {
				debugPrint(SELF + "without comparing map numbers, "
						+ "structures don't match.");
				evalResult.verificationFailureString =
						"Please draw the requested structure correctly. ";
			} // try
		} catch (Exception e1) {
			e1.printStackTrace();
			evalResult.verificationFailureString = "Error in " 
					+ SELF + e1.getMessage();
		} // try
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Determines whether the mapping of the response is the same as, or a
	 * subset of, the mapping of the author's structure.
	 * @param	respMol	the response molecule
	 * @param	authMol	the author's molecule
	 * @return	true if the mapping of the response is the same as, or a 
	 * subset of, the mapping of the author's structure
	 * @throws	MolFormatException	if the allene search goes wrong
	 * @throws	MolCompareException	if the allene search goes wrong
	 * @throws	VerifyException	if there's a problem during the search
	 */
	private boolean matchMapNumbers(Molecule respMol, Molecule authMol) 
			throws MolCompareException, MolFormatException, VerifyException {
		final String SELF = "MapProperty.matchMapNumbers: ";
		try {
			final MolSearchOptions searchOpts = getSearchOpts(checkEnant);
			searchOpts.addUserComparator(new MapMatcher(oper == ATLEAST));
			searchOpts.addUserComparator(new WavyBondMatcher());
			searchOpts.setKeepQueryOrder(true); // ChemAxon says it's needed
			final MolSearch search = new MolSearch();
			search.setSearchOptions(searchOpts);
			search.setTarget(respMol);
			search.setQuery(authMol);
			final int[] isomorphism = search.findNext();
			boolean match = isomorphism != null;
			if (match) debugPrint(SELF + "found isomorphism: ", isomorphism);
			// workaround for JChem 5.6.0.0 bug of not recognizing allene
			// enantiomers as such
			if (!match && checkEnant && MolCompare.hasAllene(respMol)) {
				search.setTarget(ChemUtils.getMirror(respMol));
				match = search.isMatching();
			} // if need to redo search with enantiomer
			debugPrint(SELF + "mapping of response ", match ? "matches" 
					: "doesn't match", " mapping of author's structure.");
			return match;
		} catch (SearchException e1) {
			e1.printStackTrace();
			throw new VerifyException(SELF + "search exception.");
		} // try
	} // matchMapNumbers(Molecule, Molecule)

	/** Gets a search options object.
	 * @return	a search options object
	 */
	static MolSearchOptions getSearchOpts() {
		return getSearchOpts(false);
	} // getSearchOpts()

	/** Gets a search options object.
	 * @param	checkEnant	whether to look at the enantiomer as well
	 * @return	a search options object
	 */
	static MolSearchOptions getSearchOpts(boolean checkEnant) {
		final MolSearchOptions searchOpts = new MolSearchOptions(FULL);
		searchOpts.setStereoSearchType(checkEnant 
				? STEREO_ENANTIOMER : STEREO_SPECIFIC);
		searchOpts.setDoubleBondStereoMatchingMode(DBS_ALL);
		searchOpts.setImplicitHMatching(IMPLICIT_H_MATCHING_DISABLED);
		// note: JChem 5.6.0.0 doesn't match allene enantiomers
		// when the search type is STEREO_ENANTIOMER
		searchOpts.setIgnoreCumuleneOrRingCisTransStereo(false); // odd-numbered cumulenes
		searchOpts.setIgnoreAxialStereo(false); // allenes & biaryls
		searchOpts.setIgnoreSynAntiStereo(true); // otherwise random orientations match
		return searchOpts;
	} // getSearchOpts(boolean)

	/** Whether the mapping pattern of the response is the same as, or a
	 * subset of, the reference mapping pattern; the mapping patterns must
	 * correspond, but the numbers don't need to be the same.
	 * This algorithm cannot use the MapMatcher comparator, so we have to 
	 * go through every match permutation and compare the map numbers 
	 * individually.
	 * @param	respMol	the response molecule
	 * @param	authMol	the author's molecule
	 * @return	true if the mapping of the response matches that of the
	 * author's structure
	 * @throws	VerifyException	if anything goes wrong
	 */
	private boolean matchMapPattern(Molecule respMol, Molecule authMol) 
			throws VerifyException {
		final String SELF = "MapProperty.matchMapPattern: ";
		try {
			final MolSearchOptions searchOpts = getSearchOpts();
			searchOpts.setExactBondMatching(true);
			searchOpts.setStereoModel(STEREO_MODEL_LOCAL);
					// required for diastereotopic atoms not to match
			searchOpts.setOrderSensitiveSearch(true); // want multiple results
			final MolSearch search = new MolSearch();
			search.setSearchOptions(searchOpts);
			search.setTarget(respMol);
			search.setQuery(authMol);
			final int MAX_MAP_NUMBER = 99;
			boolean haveMatch = false;
			while (true) {
				final int[] isomorphism = search.findNext();
				if (isomorphism == null) {
					debugPrint(SELF + "no more isomorphisms found.");
					break;
				} else debugPrint(SELF + "looking at isomorphism: {",
						Utils.join(isomorphism, 1), "}");
				final int[] maps = new int[MAX_MAP_NUMBER + 1]; // maps[respMap] = refMap
				final int[] mapsInvert = new int[MAX_MAP_NUMBER + 1]; // mapsInvert[refMap] = respMap
				haveMatch = true;
				for (int atomNum = 0; atomNum < isomorphism.length; atomNum++) {
					final MolAtom authAtom = authMol.getAtom(atomNum);
					final MolAtom respAtom = (isomorphism[atomNum] >= 0
							? respMol.getAtom(isomorphism[atomNum])
							: new MolAtom(1));
					final int refMap = authAtom.getAtomMap();
					final int respMap = respAtom.getAtomMap();
					if (refMap == 0 && (respMap == 0 || oper == ATLEAST))
						continue; // maps are OK; next atom in array
					if (respMap == 0 || (refMap == 0 && oper == EXACTLY)) {
						haveMatch = false;
						debugPrint(SELF + "bad isomorphism; keep looking.");
						break;
					} // if response atom is unmapped
					// both atoms are mapped; compare
					if (maps[respMap] == 0 && mapsInvert[refMap] == 0) {
						// first encounter with respMap && refMap
						maps[respMap] = refMap;
						mapsInvert[refMap] = respMap;
						debugPrint(SELF + "respMap ", respMap,
								" corresponds to refMap ", refMap);
					} // if maps[] and mapsInvert[]
					if (maps[respMap] != refMap) {
						haveMatch = false;
						debugPrint(SELF + "authMol atom ",
								authAtom, atomNum + 1, " with refmap ", 
								refMap, " fails to match to respMol atom ",
								respAtom, isomorphism[atomNum] + 1,
								" with respMap ", respMap,
								"; find next isomorphism.");
						break;
					} else debugPrint("   matchMapPattern: authMol atom ",
							authAtom, atomNum + 1, " with refmap ", 
							refMap, " matches to respMol atom ",
							respAtom, isomorphism[atomNum] + 1,
							" with respMap ", respMap, ".");
				} // for each atom atomNum
				if (haveMatch) {
					debugPrint(SELF + "found a match!");
					break;
				} // if there's a match
			} // loop until match found or no more isomorphisms
			debugPrint(SELF + "returning ", haveMatch);
			return haveMatch;
		} catch (Exception e1) {
			e1.printStackTrace();
			throw new VerifyException("internal error in matchMapPattern");
		}
	} // matchMapPattern(Molecule, Molecule)

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
		final String SELF = "MapProperty.mapNormalize: ";
		final MolAtom[] atoms = mol.getAtomArray();
		// don't count selections when maps are present already
		boolean molUnmapped = true;
		for (final MolAtom atom : atoms) {
			if (atom.getAtomMap() != 0) {
				molUnmapped = false;
				break;
			} // if an atom is mapped already
		} // for each atom
		if (molUnmapped) { // convert selections to maps
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
			if (patternOnly) convertHToPseudoNoClone(mol); 
					// so diastereotopic H atoms won't match
		} else {
			ChemUtils.implicitizeH(mol); 
		} // if any H is mapped
		debugPrintMRV(SELF + "converted molecule to:\n", mol);
	} // mapNormalize(Molecule, boolean)

	/** Converts old Marvin SELECTED property into actual atom selections.
	 * @param	molStr	String representation of a molecule with selections in a
	 * molecule property
	 * @return	String representation of a molecule with atom selection
	 * properties set
	public static String convertSelections(String molStr) {
		final String SELF = "MapProperty.convertSelections: ";
		String modMolStr = molStr;
		try {
			final Molecule mol = MolImporter.importMol(molStr);
			final String selectedAtoms = ChemUtils.getProperty(mol, SELECTED);
			if (!Utils.isEmpty(selectedAtoms)) {
				// don't count selections when maps are present already
				boolean molUnmapped = true;
				for (final MolAtom atom : mol.getAtomArray()) {
					if (atom.getAtomMap() != 0) {
						molUnmapped = false;
						break;
					} // if an atom is mapped already
				} // for each atom
				if (molUnmapped) {
					// convert selections to maps
					final String[] atomNumStrs = selectedAtoms.split(SEL_DIV);
					for (final String atomNumStr : atomNumStrs) {
						final int atomNum = MathUtils.parseInt(atomNumStr);
						mol.getAtom(atomNum - 1).setSelected(true);
					} // for each selected atom
				} // if no atoms are already mapped
			} // if there are selected atoms
			modMolStr = MolString.toString(mol, MRV);
		} catch (MolFormatException e) {
			Utils.alwaysPrint("Exception in " + SELF 
					+ " importing structure:\n" + molStr);
			e.printStackTrace();
		} // try
		return modMolStr;
	} // convertSelections(String)
	 */

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
	public String getMatchCode() 				{ return EVAL_CODES[MAPPED_ATOMS]; } 
	/** Gets whether the evaluator is satisfied by a match or a mismatch.
	 * @return	true if the evaluator is satisfied by a match
	 */
	public boolean getIsPositive() 				{ return isPositive; }
	/** Sets whether the evaluator is satisfied by a match or a mismatch.
	 * @param	isPos	true if the evaluator is satisfied by a match
	 */
	public void setIsPositive(boolean isPos) 	{ isPositive = isPos; }
	/** Gets whether the reference mapping is all of or a subset of the response 
	 * mapping.
	 * @return	how to compare the mapping of the response to the reference
	 * structure
	 */
	public int getOper() 						{ return oper; }
	/** Sets whether the reference mapping is all of or a subset of the response 
	 * mapping.
	 * @param	theOper	how to compare the mapping of the response to the
	 * reference structure
	 */
	public void setOper(int theOper) 			{ oper = theOper; }
	/** Gets whether the map numbers of the response should be the same as or
	 * just have the same pattern as the reference map numbers.
	 * @return	false if the map numbers should be the same
	 */
	public boolean getPatternOnly() 			{ return patternOnly; }
	/** Sets whether the map numbers of the response should be the same as or
	 * just have the same pattern as the reference map numbers.
	 * @param	pttrn	false if the map numbers should be the same
	 */
	public void setPatternOnly(boolean pttrn)	{ patternOnly = pttrn; }
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
	/** Sets the molecule's name.  Not currently used.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 		{ this.molName = molName; }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 				{ return false; }

} // MapProperty
