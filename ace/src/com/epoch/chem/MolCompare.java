package com.epoch.chem;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.sss.SearchConstants;
import chemaxon.sss.search.MolSearch;
import chemaxon.sss.search.MolSearchOptions;
import chemaxon.sss.search.SearchException;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import chemaxon.struc.Molecule;
import chemaxon.struc.StereoConstants;
import com.epoch.chem.chemConstants.ChemConstants;
import com.epoch.exceptions.VerifyException;
import com.epoch.lewis.LewisMolecule;
import com.epoch.lewis.lewisConstants.LewisConstants;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** Contains molecule matching methods.
 * <ul>
 * <li>matchPrecise() -- diagonal-only match; explicit and implicit H
 * atoms are considered equivalent
 * <li>matchPerfect() -- diagonal-only match; explicit H atoms in two structures
 * must match exactly, too
 * <li>matchNearPerfect() -- utility for matchPrecise() and matchPerfect()
 * <li>matchExact() -- nondiagonal match; straight bond in author structure
 * matches to any bond in response; explicit and implicit H are considered
 * equivalent
 * <li>matchPerfectLewis() -- like matchPerfect(), but unshared electrons must match too
 * <li>areResonanceStructures() -- if charge and &sigma;-bond network are same but
 * structures are different
 * <li>areResonanceOrIdentical() -- if charge and &sigma;-bond network are same
 * <li>matchSigmaNetwork() -- checks if the response contains the &sigma;-bond
 * network of the author's structure.  All H atoms in response (unless it is a
 * Lewis structure) and author's structure are made explicit,
 * all multiple bonds are converted to single bonds, and two are compared ignoring
 * charges and radicals (and implicit H atoms!) but considering explicit H atoms.
 * <li>containsSubstruct() -- contains a substructure
 * <li>substructMatches() -- contains a substructure, returning all the matches
 * <li>matchConformers() -- determines whether two identical compounds
 * are in the same conformation
 * </ul>
 */
public final class MolCompare implements ChemConstants, LewisConstants, 
		SearchConstants, StereoConstants {

	private static void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	/** Text reporting error. */
	private static final String ERROR = "MolFile error ";
	/** Number of atoms in a chair-shaped ring. */
	private static final int CHAIR_SIZE = 6;
	/** Number of atoms in a bond. */
	private static final int BOND_SIZE = 2;
	/** Parameter for matchPerfect() and matchExact(). */
	public static final boolean OR_ENANTIOMER = true;
	/** Parameter for matchPerfect() and matchExact(). */
	public static final boolean ALLOW_ISOTOPES = true;

/* ************ Begin matching functions *****************/

	/** Determines whether two molecules are identical: stereo bonds must match
	 * exactly, but explicit H atoms in one may be implicit in the other, and
	 * vice versa.  Molecules are NOT modified, so they don't need to be cloned
	 * before coming here.  Irrelevant which molecule is respMol and which is
	 * authMol, but retained for consistency.
	 * @param	respMol	a molecule
	 * @param	authMol	a molecule
	 * @return	true if the molecules are exactly alike, including
	 * stereo bonds but not explicit/implicit H atoms
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean matchPrecise(Molecule respMol, Molecule authMol)
			throws MolCompareException {
		return matchNearPerfect(respMol, authMol, WAVY_AND);
	} // matchPrecise(Molecule, Molecule)

	/** Determines whether two MRVs represent identical molecules: stereo
	 * bonds must match exactly, and all explicit H atoms in one must also be
	 * present in the other.  Irrelevant which molecule is respMRV and which is
	 * authMRV, but retained for consistency.
	 * @param	respMRV	String representation of a molecule
	 * @param	authMRV	String representation a molecule
	 * @return	true if the molecules are exactly alike, including all explicit
	 * H atoms and stereo bonds
	 * @throws	MolCompareException	if the molecules can't be compared, because 
	 * one can't be imported or something goes wrong in the matching process
	 */
	public static boolean matchPerfect(String respMRV, String authMRV)
			throws MolCompareException {
		final String SELF = "MolCompare.matchPerfect: ";
		boolean match = false;
		try {
			final Molecule respMol = MolImporter.importMol(respMRV);
			final Molecule authMol = MolImporter.importMol(authMRV);
			match = matchPerfect(respMol, authMol);
		} catch (MolFormatException e1) {
			Utils.alwaysPrint(SELF + "MOLFORMAT "
					+ "EXCEPTION FOR either of these two: \n"
					+ respMRV + "\n\n\n" + authMRV);
			e1.printStackTrace();
			throw new MolCompareException(ERROR + SELF + e1.getMessage());
		}
		return match;
	} // matchPerfect(String, String)

	/** Determines whether two molecules are identical: stereo bonds must match
	 * exactly, and all explicit H atoms in one must also be present in the
	 * other.  Molecules are NOT modified, so they don't need to be cloned
	 * before coming here.  Irrelevant which molecule is respMol and which is
	 * authMol, but retained for consistency.
	 * @param	respMol	a response molecule
	 * @param	authMol	an author's molecule
	 * @return	true if the molecules are exactly alike, including all explicit
	 * H atoms and stereo bonds
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean matchPerfect(Molecule respMol, Molecule authMol)
			throws MolCompareException {
		return matchPerfect(respMol, authMol, !OR_ENANTIOMER, !ALLOW_ISOTOPES);
	} // matchPerfect(Molecule, Molecule)

	/** Determines whether two molecules are identical: stereo bonds must match
	 * exactly, and all explicit H atoms in one must also be present in the
	 * other.  Molecules are NOT modified, so they don't need to be cloned
	 * before coming here.  Irrelevant which molecule is respMol and which is
	 * authMol, but retained for consistency.
	 * @param	respMol	a molecule
	 * @param	authMol	a molecule
	 * @param	orEnantiomer	whether the enantiomer should match as well
	 * @return	true if the molecules are exactly alike
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean matchPerfect(Molecule respMol, Molecule authMol,
			boolean orEnantiomer) throws MolCompareException {
		return matchPerfect(respMol, authMol, orEnantiomer, !ALLOW_ISOTOPES);
	} // matchPerfect(Molecule, Molecule, boolean)

	/** Determines whether two molecules are identical: stereo bonds must match
	 * exactly, and all explicit H atoms in one must also be present in the
	 * other.  Molecules are NOT modified, so they don't need to be cloned
	 * before coming here.  Irrelevant which molecule is respMol and which is
	 * authMol, but retained for consistency.
	 * @param	respMol	a molecule
	 * @param	authMol	a molecule
	 * @param	orEnantiomer	whether the enantiomer should match as well
	 * @param	isotopeLenient	whether to allow specified isotopes in the
	 * response to match unspecified in the author's structure
	 * @return	true if the molecules are exactly alike
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean matchPerfect(Molecule respMol, Molecule authMol,
			boolean orEnantiomer, boolean isotopeLenient) 
			throws MolCompareException {
		int matchFlags = (orEnantiomer ? CONSIDER_ENANT : 0);
		if (isotopeLenient) matchFlags |= ISOTOPE_PERMISSIVE;
		return matchPerfect(respMol, authMol, matchFlags);
	} // matchPerfect(Molecule, Molecule, boolean, boolean)

	/** Called by all public matchPerfect() methods.  Determines whether two 
	 * molecules are identical: stereo bonds must match
	 * exactly, and all explicit H atoms in one must also be present in the
	 * other.  Molecules are NOT modified, so they don't need to be cloned
	 * before coming here.  Irrelevant which molecule is respMol and which is
	 * authMol, but retained for consistency.
	 * @param	respMol	a molecule
	 * @param	authMol	a molecule
	 * @param	matchFlags	flags for treating stereochemistry, H matching,
	 * electron matching, isotope matching
	 * @return	true if the molecules are exactly alike
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	private static boolean matchPerfect(Molecule respMol, Molecule authMol,
			int matchFlags) throws MolCompareException {
		final int perfectFlags = matchFlags | WAVY_AND | EXACT_EXPLICIT_H;
		return matchNearPerfect(respMol, authMol, perfectFlags);
	} // matchPerfect(Molecule, Molecule, int)

	/** Utility method for matchPerfect() and matchPrecise().  Determines 
	 * whether two molecules are identical: stereo bonds must match
	 * exactly.  Molecules are NOT modified, so they don't need to be cloned
	 * before coming here.  Irrelevant which molecule is respMol and which is
	 * authMol, but retained for consistency.
	 * @param	respMol	a molecule
	 * @param	authMol	a molecule
	 * @param	matchFlags	flags for treating stereochemistry, H matching,
	 * electron matching, isotope matching
	 * @return	true if the molecules are exactly alike
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	private static boolean matchNearPerfect(Molecule respMol, Molecule authMol,
			int matchFlags) throws MolCompareException {
		final String SELF = "MolCompare.matchNearPerfect: ";
		boolean match = false;
		final MolSearchOptions searchOpts = new MolSearchOptions(DUPLICATE);
		searchOpts.setVagueBondLevel(VAGUE_BOND_OFF);
		searchOpts.setStereoModel(STEREO_MODEL_GLOBAL);
				// required for comparing nonaromatized aromatic rings
		setMatchOptions(searchOpts, matchFlags);
		final MolSearch search = new MolSearch();
		search.setSearchOptions(searchOpts);
		final Molecule respMolNorm = 
				Normalize.normalizeCoordinateBonds(respMol);
		final Molecule authMolNorm =
				Normalize.normalizeCoordinateBonds(authMol);
		search.setTarget(respMolNorm);
		search.setQuery(authMolNorm);
		debugPrintMRV(SELF + "respMolNorm:\n", respMol,
				SELF + "authMolNorm:\n", authMol);
		try {
			match = search.isMatching();
			debugPrint(SELF + "for target ", respMolNorm, " and query ", 
					authMolNorm, ", search result is ", match);
		} catch (SearchException e2) {
			Utils.alwaysPrint("Error in " + SELF);
			e2.printStackTrace();
			throw new MolCompareException(ERROR + SELF + e2.getMessage());
		} // try
		return match;
	} // matchNearPerfect(Molecule, Molecule, int)

	/** Sets the search options related to stereochemistry, H matching, electron
	 * matching, and isotope matching.
	 * @param	searchOpts	contains the search options
	 * @param	matchFlags	flags for treating stereochemistry, H matching,
	 * electron matching, isotope matching
	 */
	private static void setMatchOptions(MolSearchOptions searchOpts,
			int matchFlags) {
		final String SELF = "MolCompare.setMatchOptions: ";
		final int searchType = searchOpts.getSearchType();
		debugPrint(SELF + "searchType = ", 
				(searchType == DUPLICATE ? "DUPLICATE" 
					: searchType == FULL ? "FULL" : searchType));
		final boolean ignore2D = 
				(matchFlags & IGNORE_DBL_BOND_STEREO) != 0;
		final boolean ignore3D = 
				(matchFlags & IGNORE_TETRAHEDRAL_STEREO) != 0;
		final boolean wavyAnd = (matchFlags & WAVY_AND) != 0;
		final boolean considerEnant = (matchFlags & CONSIDER_ENANT) != 0;
		final boolean exactExplicitHMatching = 
				(matchFlags & EXACT_EXPLICIT_H) != 0;
		final boolean electronMatching = (matchFlags & ELECTRONS) != 0;
		final boolean isotopeLenient = (matchFlags & ISOTOPE_PERMISSIVE) != 0;
		if (exactExplicitHMatching) {
			debugPrint(SELF + "adding ExplicitHMatcher.");
			searchOpts.addUserComparator(new ExplicitHMatcher());
		} else debugPrint(SELF + "not adding ExplicitHMatcher.");
		if (electronMatching) {
			debugPrint(SELF + "adding ElectronMatcher.");
			searchOpts.addUserComparator(new ElectronMatcher());
		} else debugPrint(SELF + "not adding ElectronMatcher.");
		if (ignore2D) {
			debugPrint(SELF + "ignoring 2D stereochemistry.");
			searchOpts.setDoubleBondStereoMatchingMode(DBS_NONE);
		} else {
			debugPrint(SELF + "pay attention to 2D stereochemistry.");
			searchOpts.setDoubleBondStereoMatchingMode(DBS_ALL);
		} // if ignore2D
		if (ignore3D) {
			debugPrint(SELF + "ignoring 3D stereochemistry.");
			searchOpts.setStereoSearchType(STEREO_IGNORE);
		} else {
			debugPrint(SELF + "pay attention to 3D stereochemistry.");
			if (searchType != DUPLICATE) {
				searchOpts.setStereoSearchType(STEREO_SPECIFIC);
			} // if search initially set to FULL, not DUPLICATE
			if (considerEnant) {
				debugPrint(SELF + "enantiomer will match as well.");
				searchOpts.setStereoSearchType(STEREO_ENANTIOMER);
			} // if should consider enantiomers as well
			if (wavyAnd) {
				searchOpts.setKeepQueryOrder(true); // ChemAxon says it's needed
				debugPrint(SELF + "adding WavyBondMatcher.");
				searchOpts.addUserComparator(new WavyBondMatcher());
			} else debugPrint(SELF + "not adding WavyBondMatcher.");
			searchOpts.setIgnoreCumuleneOrRingCisTransStereo(false); // odd-numbered cumulenes
			searchOpts.setIgnoreAxialStereo(false); // allenes & biaryls
			searchOpts.setIgnoreSynAntiStereo(true); // otherwise random orientations matter
		} // if ignore3D
		if (isotopeLenient) {
			debugPrint(SELF + "author's unspecified isotopes can match any "
					+ "isotopes in response.");
			searchOpts.setIsotopeMatching(ISOTOPE_MATCHING_DEFAULT);
		} else {
			debugPrint(SELF + "author's unspecified isotopes must be unspecified "
					+ "in response.");
			searchOpts.setIsotopeMatching(ISOTOPE_MATCHING_EXACT);
		} // isotopeLenient
	} // setMatchOptions(MolSearchOptions, int)

	/** Determines whether a response molecule (target in ChemAxon parlance)
	 * matches to an author's molecule (query).
	 * Any stereo bond in the author's molecule must be present in the response
	 * molecule, but a nonstereobond in the author's molecule matches to any
	 * stereo bond (or none) in the response molecule.
	 * @param	respMol	a response molecule
	 * @param	authMol	an author's molecule
	 * @return	true if the response molecule matches the author's molecule
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean matchExact(Molecule respMol, Molecule authMol)
			throws MolCompareException {
		return matchExact(respMol, authMol, !OR_ENANTIOMER, !ALLOW_ISOTOPES);
	} // matchExact(Molecule, Molecule)

	/** Determines whether a response molecule (target in ChemAxon parlance)
	 * matches to an author's molecule (query).
	 * Any stereo bond in the author's molecule must be present in the response
	 * molecule, but a nonstereobond in the author's molecule matches to any
	 * stereo bond (or none) in the response molecule.
	 * @param	respMol	a response molecule
	 * @param	authMol	an author's molecule
	 * @param	orEnantiomer	whether the enantiomer should match as well
	 * @return	true if the response molecule matches the author's molecule
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean matchExact(Molecule respMol, Molecule authMol,
			boolean orEnantiomer) throws MolCompareException {
		return matchExact(respMol, authMol, orEnantiomer, !ALLOW_ISOTOPES);
	} // matchExact(Molecule, Molecule, boolean)

	/** Determines whether a response molecule (target in ChemAxon parlance)
	 * (or maybe its enantiomer) matches to an author's molecule (query).
	 * Any stereo bond in the author's molecule must be present in the response
	 * molecule, but a nonstereobond in the author's molecule matches to any
	 * stereo bond (or none) in the response molecule.
	 * @param	respMol	a molecule
	 * @param	authMol	a molecule
	 * @param	orEnantiomer	whether the enantiomer should match as well
	 * @param	isotopeLenient	whether to allow specified isotopes in the
	 * response to match unspecified in the author's structure
	 * @return	true if the molecules are exactly alike
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean matchExact(Molecule respMol, Molecule authMol,
			boolean orEnantiomer, boolean isotopeLenient) 
			throws MolCompareException {
		int stereoType = WAVY_AND; 
		if (orEnantiomer) stereoType |= CONSIDER_ENANT;
		if (isotopeLenient) stereoType |= ISOTOPE_PERMISSIVE;
		return matchExact(respMol, authMol, stereoType);
	} // matchExact(Molecule, Molecule, boolean, boolean)

	/** Determines whether a response molecule (target in ChemAxon parlance)
	 * (or maybe its enantiomer) matches to an author's molecule (query).  
	 * If stereochemistry is not
	 * ignored, any stereo bond in the author's molecule must be present in
	 * the response molecule, but a nonstereobond in the author's molecule
	 * matches to any stereo bond (or none) in the response molecule.
	 * (Value of setImplicitHMatching() is set to IMPLICIT_H_MATCHING_ENABLED
	 * by default.  DISABLED would mean that explicit H atoms in the author's
	 * substructure would have to be explicit in the response.)
	 * @param	respMol	a response molecule
	 * @param	authMol	an author's molecule
	 * @param	matchFlags	flags for treating stereochemistry, H matching,
	 * electrons matching, isotope matching
	 * @return	true if the response molecule matches the author's molecule
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean matchExact(Molecule respMol, Molecule authMol,
			int matchFlags) throws MolCompareException {
		final String SELF = "MolCompare.matchExact: ";
		final Molecule respMolNorm = // respMol;
				Normalize.normalizeCoordinateBonds(respMol);
		final Molecule authMolNorm = // authMol;
				Normalize.normalizeCoordinateBonds(authMol);
		debugPrint(SELF + "matchFlags = ", matchFlags);
		debugPrintMRV(SELF + "response:\n", respMolNorm,
				"\nauthor structure:\n", authMolNorm);
		boolean match = false;
		final MolSearchOptions searchOpts = new MolSearchOptions(FULL);
		searchOpts.setStereoModel(STEREO_MODEL_GLOBAL);
		searchOpts.setVagueBondLevel(VAGUE_BOND_OFF);
				// required for comparing nonaromatized aromatic rings
		searchOpts.setChargeMatching(CHARGE_MATCHING_EXACT);
		searchOpts.setRadicalMatching(RADICAL_MATCHING_EXACT);
		searchOpts.setValenceMatching(true);
		setMatchOptions(searchOpts, matchFlags);
		final MolSearch search = new MolSearch();
		search.setSearchOptions(searchOpts);
		search.setTarget(respMolNorm);
		search.setQuery(authMolNorm);
		try {
			match = search.isMatching();
			debugPrint(SELF + "for target ", respMolNorm, " and query ", 
					authMolNorm, ", search result is ", match);
			// workaround for JChem 5.9 bug of not recognizing allene
			// enantiomers as such
			if (!match && needAlleneEnant(respMolNorm, matchFlags)) {
				debugPrint(SELF + "no match, but structure contains an "
						+ "allene, so checking its enantiomer.");
				match = matchExact(ChemUtils.getMirror(respMolNorm), 
						authMolNorm, matchFlags & ~CONSIDER_ENANT);
			} // if need to redo search with enantiomer
		} catch (MolFormatException e1) { // extremely unlikely
			Utils.alwaysPrint("Error in " + SELF);
			e1.printStackTrace();
			throw new MolCompareException(ERROR + SELF + e1.getMessage());
		} catch (SearchException e2) {
			Utils.alwaysPrint("Error in " + SELF);
			e2.printStackTrace();
			throw new MolCompareException(ERROR + SELF + e2.getMessage());
		} // try
		return match;
	} // matchExact(Molecule, Molecule, int)

	/** Determines if a response molecule contains an allene and we are
	 * considering its enantiomer as well. 
	 * @param	respMol	the response molecule
	 * @param	matchFlags	flags for treating stereochemistry, H matching,
	 * electrons matching, isotope matching
	 * @return	true if the response contains an allene and we are considering
	 * its enantiomer as well
	 * @throws	MolFormatException	if the shortcut group can't be imported into
	 * a Molecule
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	private static boolean needAlleneEnant(Molecule respMol, int matchFlags) 
			throws MolFormatException, MolCompareException {
		final boolean considerEnant = (matchFlags & CONSIDER_ENANT) != 0;
		return hasAllene(respMol) && considerEnant;
	} // needAlleneEnant(Molecule, int)

	/** Determines if a response molecule contains an allene.
	 * @param	respMol	the response molecule
	 * @return	true if the response contains an allene
	 * @throws	MolFormatException	if the substructure can't be imported into
	 * a Molecule
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean hasAllene(Molecule respMol) 
			throws MolFormatException, MolCompareException {
		return containsSubstruct(respMol, "[C,N]=C=[C,N]");
	} // hasAllene(Molecule)

	/** Determines whether a response molecule (target in ChemAxon parlance)
	 * is the same Lewis structure as an author's molecule (query).  The
	 * structures must match exactly, as well as the number of shared electrons
	 * of each atom.
	 * @param	respMRV	String representation of a response Lewis structure molecule
	 * @param	authMRV	String representation of an author's Lewis structure molecule
	 * @return	true if the response's Lewis structure is the same as the author's
	 * @throws	MolCompareException	if the molecules can't be compared, because 
	 * one can't be imported or something goes wrong in the matching process
	 */
	public static boolean matchPerfectLewis(String respMRV, String authMRV)
			throws MolCompareException {
		try {
			final LewisMolecule respLewis = new LewisMolecule(respMRV);
			return matchPerfectLewis(respLewis, authMRV);
		} catch (VerifyException ex) {
			ex.printStackTrace();
			throw new MolCompareException("MolCompare.matchPerfectLewis: "
					+ ex.getMessage());
		}
	} // matchPerfectLewis(String, String)

	/** Determines whether a response molecule (target in ChemAxon parlance)
	 * is the same Lewis structure as an author's molecule (query).  The
	 * structures must match exactly, as well as the number of shared electrons
	 * of each atom.
	 * @param	respLewis	response parsed as a LewisMolecule
	 * @param	authMOL	String representation of an author's Lewis structure molecule
	 * @return	true if the response's Lewis structure is the same as the author's
	 * @throws	MolCompareException	if the molecules can't be compared, because 
	 * one can't be imported or something goes wrong in the matching process
	 */
	public static boolean matchPerfectLewis(LewisMolecule respLewis,
			String authMOL) throws MolCompareException {
		final String SELF = "MolCompare.matchPerfectLewis: ";
		boolean match = false;
		try {
			final LewisMolecule authLewis = new LewisMolecule(authMOL);
			final Molecule authMol = authLewis.getMolecule();
			final Molecule respMol = respLewis.getMolecule();
			debugPrintMRV(SELF + "response (target):\n", respMol,
					"\nand reference (query):\n", authMol);
			match = matchPerfect(respMol, authMol, ELECTRONS | IGNORE_STEREO);
			debugPrint(SELF + "returning ", match);
		} catch (VerifyException ex1) {
			ex1.printStackTrace();
			throw new MolCompareException(SELF + ex1.getMessage());
		} // try
		return match;
	} // matchPerfectLewis(LewisMolecule, String)

	/** Determines whether two molecules are resonance structures.
	 * Two structures are resonance structures of
	 * each other if they have the same &sigma;-bond network
	 * and the same total charge, and are not the same molecule.
	 * @param	respMRV	String representation of a molecule
	 * @param	authMRV	String representation of another molecule
	 * @return	true if the molecules are resonance structures
	 * @throws	MolCompareException	if the molecules can't be compared, because 
	 * one can't be imported or something goes wrong in the matching process
	 */
	public static boolean areResonanceStructures(String respMRV,
			String authMRV) throws MolCompareException {
		boolean match = false;
		try {
			final Molecule respMol = MolImporter.importMol(respMRV);
			final Molecule authMol = MolImporter.importMol(authMRV);
			match = areResonanceStructures(respMol, authMol);
		} catch (MolFormatException e) {
			Utils.alwaysPrint("MolCompare.areResonanceStructures: "
					+ "can't convert either to Molecule");
		}
		return match;
	} // areResonanceStructures(String, String)

	/** Determines whether two molecules are resonance structures.
	 * Two structures are resonance structures of
	 * each other if they have the same &sigma;-bond network
	 * and the same total charge, and are not the same molecule.
	 * @param	respMol	a molecule
	 * @param	authMol	another molecule
	 * @return	true if the molecules are resonance structures
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean areResonanceStructures(Molecule respMol,
			Molecule authMol) throws MolCompareException {
		final String isLewisStr = 
				ChemUtils.getProperty(respMol, LEWIS_PROPERTY);
		final boolean respIsLewis = TRUE.equals(isLewisStr);
		return areResonanceStructures(respMol, authMol, respIsLewis);
	} // areResonanceStructures(Molecule, Molecule)

	/** Determines whether two molecules are resonance structures.
	 * Two structures are resonance structures of
	 * each other if they have the same &sigma;-bond network
	 * and the same total charge, and are not the same molecule.
	 * If the response is a Lewis structure, implicit H atoms in the
	 * response are ignored.
	 * @param	respMol	a molecule
	 * @param	authMol	another molecule
	 * @param	respIsLewis	whether the response is a Lewis structure
	 * @return	true if the molecules are resonance structures
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean areResonanceStructures(Molecule respMol,
			Molecule authMol, boolean respIsLewis) throws MolCompareException {
		final boolean resOrIdentical =
				areResonanceOrIdentical(respMol, authMol, respIsLewis, WAVY_AND);
		final boolean identical = matchExact(respMol, authMol);
		debugPrint("MolCompare.areResonanceStructures: ",
					respIsLewis ? "Lewis " : "",
					"structures ", respMol, " and ", authMol,
					": resOrIdentical = ", resOrIdentical,
					", identical = ", identical,
					", returning ", resOrIdentical && !identical);
		return resOrIdentical && !identical;
	} // areResonanceStructures(Molecule, Molecule, boolean)

	/** Determines whether two molecules have the same &sigma;-bond
	 * network and the same total charge.
	 * @param	respMRV	String representation of a molecule
	 * @param	authMRV	String representation of another molecule
	 * @return	true if the molecules are resonance structures or identical
	 * @throws	MolCompareException	if the molecules can't be compared, because 
	 * one can't be imported or something goes wrong in the matching process
	 */
	public static boolean areResonanceOrIdentical(String respMRV,
			String authMRV) throws MolCompareException {
		boolean match = false;
		try {
			final boolean respIsLewis = respMRV.contains("Lewis");
			final Molecule respMol = MolImporter.importMol(respMRV);
			final Molecule authMol = MolImporter.importMol(authMRV);
			match = areResonanceOrIdentical(respMol, authMol, 
					respIsLewis, WAVY_AND);
		} catch (MolFormatException e) {
			Utils.alwaysPrint("MolCompare.areResonanceOrIdentical: "
					+ "can't convert either to Molecule");
		}
		return match;
	} // areResonanceOrIdentical(String, String)

	/** Determines whether two molecules have the same &sigma;-bond
	 * network and the same total charge.
	 * @param	respMol	a molecule
	 * @param	authMol	another molecule
	 * @return	true if the molecules are resonance structures or identical
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean areResonanceOrIdentical(Molecule respMol,
			Molecule authMol) throws MolCompareException {
		return areResonanceOrIdentical(respMol, authMol, WAVY_AND);
	} // areResonanceOrIdentical(Molecule, Molecule)

	/** Determines whether two molecules have the same &sigma;-bond
	 * network and the same total charge.
	 * If the response is a Lewis structure, implicit H atoms in the
	 * response are ignored.
	 * @param	respMol	a molecule
	 * @param	authMol	another molecule
	 * @param	respIsLewis	whether the molecules are Lewis structures
	 * @param	orEnantiomer	whether the enantiomer should match as well
	 * @return	true if the molecules are resonance structures or identical
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean areResonanceOrIdentical(Molecule respMol,
			Molecule authMol, boolean respIsLewis, boolean orEnantiomer) 
			throws MolCompareException {
		return areResonanceOrIdentical(respMol, authMol, respIsLewis, 
				orEnantiomer, !ALLOW_ISOTOPES);
	} // areResonanceOrIdentical(Molecule, Molecule, boolean, boolean)

	/** Determines whether two molecules have the same &sigma;-bond
	 * network and the same total charge.
	 * If the response is a Lewis structure, implicit H atoms in the
	 * response are ignored.
	 * @param	respMol	a molecule
	 * @param	authMol	another molecule
	 * @param	respIsLewis	whether the molecules are Lewis structures
	 * @param	orEnantiomer	whether the enantiomer should match as well
	 * @param	isotopeLenient	whether to allow specified isotopes in the
	 * response to match unspecified in the author's structure
	 * @return	true if the molecules are resonance structures or identical
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean areResonanceOrIdentical(Molecule respMol,
			Molecule authMol, boolean respIsLewis, boolean orEnantiomer,
			boolean isotopeLenient) throws MolCompareException {
		int matchFlags = WAVY_AND;
		if (orEnantiomer) matchFlags |= CONSIDER_ENANT;
		if (isotopeLenient) matchFlags |= ISOTOPE_PERMISSIVE;
		return areResonanceOrIdentical(respMol, authMol, respIsLewis, 
				matchFlags);
	} // areResonanceOrIdentical(Molecule, Molecule, boolean, boolean, boolean)

	/** Determines whether two molecules have the same &sigma;-bond
	 * network and the same total charge.
	 * If the response is a Lewis structure, implicit H atoms in the
	 * response are ignored.
	 * @param	respMol	a molecule
	 * @param	authMol	another molecule
	 * @param	matchFlags	flags for treating stereochemistry, H matching,
	 * electrons matching, isotope matching
	 * @return	true if the molecules are resonance structures or identical
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean areResonanceOrIdentical(Molecule respMol,
			Molecule authMol, int matchFlags) throws MolCompareException {
		final String isLewisStr = 
				ChemUtils.getProperty(respMol, LEWIS_PROPERTY);
		final boolean respIsLewis = TRUE.equals(isLewisStr);
		return areResonanceOrIdentical(respMol, authMol, 
				respIsLewis, matchFlags);
	} // areResonanceOrIdentical(Molecule, Molecule, int)

	/** Determines whether two molecules have the same &sigma;-bond
	 * network and the same total charge.
	 * If the response is a Lewis structure, implicit H atoms in the
	 * response are ignored.
	 * @param	respMol	a molecule
	 * @param	authMol	another molecule
	 * @param	respIsLewis	whether the molecules are Lewis structures
	 * @param	matchFlags	flags for treating stereochemistry, H matching,
	 * electrons matching, isotope matching
	 * @return	true if the molecules are resonance structures or identical
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean areResonanceOrIdentical(Molecule respMol,
			Molecule authMol, boolean respIsLewis, int matchFlags) 
			throws MolCompareException {
		final boolean chgs = 
				respMol.getTotalCharge() == authMol.getTotalCharge();
		final boolean sigmasMatch = matchSigmaNetwork(
				Normalize.resonateCoordinateBonds(respMol), 
				Normalize.resonateCoordinateBonds(authMol), 
				respIsLewis, matchFlags);
		debugPrint("MolCompare.areResonanceOrIdentical: chgs same = ", 
				chgs, ", sigmas match = ", sigmasMatch, ", structures are ",
				(chgs && sigmasMatch ? "resonance structures or"
					: "neither resonance structures nor"), " identical.");
		return chgs && sigmasMatch;
	} // areResonanceOrIdentical(Molecule, Molecule, boolean, int)

	/** Determines whether a response molecule (target in ChemAxon parlance)
	 * has the same &sigma;-bond network as an author's molecule (query).
	 * @param	respMol	a response molecule
	 * @param	authMol	an author's molecule
	 * @return	true if the response molecule has the &sigma;-bond network of
	 * the author's molecule
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean matchSigmaNetwork(Molecule respMol,
			Molecule authMol) throws MolCompareException {
		return matchSigmaNetwork(respMol, authMol, WAVY_AND);
	} // matchSigmaNetwork(Molecule, Molecule)

	/** Determines whether a response molecule (target in ChemAxon parlance)
	 * has the same &sigma;-bond network as an author's molecule (query).
	 * @param	respMol	a response molecule
	 * @param	authMol	an author's molecule
	 * @param	stereoType	flags for treating stereochemistry
	 * @return	true if the response molecule has the &sigma;-bond network of
	 * the author's molecule
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean matchSigmaNetwork(Molecule respMol,
			Molecule authMol, int stereoType) throws MolCompareException {
		final String isLewisStr = 
				ChemUtils.getProperty(respMol, LEWIS_PROPERTY);
		final boolean respIsLewis = TRUE.equals(isLewisStr);
		return matchSigmaNetworkNoClone(respMol, authMol, 
				respIsLewis, stereoType);
	} // matchSigmaNetwork(Molecule, Molecule, int)

	/** Determines whether a response molecule (target in ChemAxon parlance)
	 * has the same &sigma;-bond network as an author's molecule (query).
	 * @param	respMol	a response molecule
	 * @param	authMol	an author's molecule
	 * @param	respIsLewis	whether the response is a Lewis structure
	 * @param	orEnantiomer	whether the enantiomer should match as well
	 * @return	true if the response molecule has the &sigma;-bond network of
	 * the author's molecule
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean matchSigmaNetwork(Molecule respMol, 
			Molecule authMol, boolean respIsLewis, boolean orEnantiomer) 
			throws MolCompareException {
		return matchSigmaNetwork(respMol, authMol, respIsLewis, orEnantiomer, 
				!ALLOW_ISOTOPES);
	} // matchSigmaNetwork(Molecule, Molecule, boolean, boolean)

	/** Determines whether a response molecule (target in ChemAxon parlance)
	 * has the same &sigma;-bond network as an author's molecule (query).
	 * @param	respMol	a response molecule
	 * @param	authMol	an author's molecule
	 * @param	respIsLewis	whether the response is a Lewis structure
	 * @param	orEnantiomer	whether the enantiomer should match as well
	 * @param	isotopeLenient	whether to allow specified isotopes in the
	 * response to match unspecified in the author's structure
	 * @return	true if the response molecule has the &sigma;-bond network of
	 * the author's molecule
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean matchSigmaNetwork(Molecule respMol, Molecule authMol, 
			boolean respIsLewis, boolean orEnantiomer, boolean isotopeLenient) 
			throws MolCompareException {
		int matchFlags = WAVY_AND;
		if (orEnantiomer) matchFlags |= CONSIDER_ENANT;
		if (isotopeLenient) matchFlags |= ISOTOPE_PERMISSIVE;
		return matchSigmaNetwork(respMol, authMol, respIsLewis, matchFlags);
	} // matchSigmaNetwork(Molecule, Molecule, boolean, boolean, boolean)

	/** Determines whether a response molecule (target in ChemAxon parlance)
	 * has the same &sigma;-bond network as an author's molecule (query).
	 * @param	respMol	a response molecule
	 * @param	authMol	an author's molecule
	 * @param	respIsLewis	whether the response is a Lewis structure
	 * @param	matchFlags	flags for treating stereochemistry, H matching,
	 * electrons matching, isotope matching
	 * @return	true if the response molecule has the &sigma;-bond network of
	 * the author's molecule
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean matchSigmaNetwork(Molecule respMol, Molecule authMol, 
			boolean respIsLewis, int matchFlags) throws MolCompareException {
		return matchSigmaNetworkNoClone(respMol.clone(), authMol.clone(), 
				respIsLewis, matchFlags);
	} // matchSigmaNetwork(Molecule, Molecule, boolean, int)

	/** Determines whether a response molecule (target in ChemAxon parlance)
	 * has the same &sigma;-bond network as an author's molecule (query).
	 * NOTE: Original molecules are modified by this method.
	 * @param	respMol	a response molecule
	 * @param	authMol	an author's molecule
	 * @param	respIsLewis	whether the response is a Lewis structure
	 * @param	matchFlags	flags for treating stereochemistry, H matching,
	 * electrons matching, isotope matching
	 * @return	true if the response molecule has the &sigma;-bond network of
	 * the author's molecule
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	private static boolean matchSigmaNetworkNoClone(Molecule respMol,
			Molecule authMol, boolean respIsLewis, int matchFlags) 
			throws MolCompareException {
		final String SELF = "MolCompare.matchSigmaNetwork: ";
		boolean match = false;
		debugPrint(SELF + "respIsLewis = ", respIsLewis);
		debugPrintMRV(SELF + "response before sigmanormalizing:\n", respMol,
				"\n" + SELF + "author structure before sigmanormalizing:\n", 
				authMol);
		Normalize.normalizeCoordinateBondsNoClone(respMol);
		Normalize.normalizeCoordinateBondsNoClone(authMol);
		// respMol may have been drawn with LewisSketch
		sigmaNormalize(respMol, respIsLewis);
		// authMol always drawn with Marvin
		sigmaNormalize(authMol, false);
		debugPrintMRV(SELF + "response after adding H atoms and "
				+ "sigmanormalizing:\n", respMol,
				"\n" + SELF + "author structure after adding H atoms and "
				+ "sigmanormalizing:\n", authMol);
		final MolSearchOptions searchOpts = new MolSearchOptions(FULL);
		setMatchOptions(searchOpts, matchFlags);
		searchOpts.setExactBondMatching(false);
		searchOpts.setChargeMatching(CHARGE_MATCHING_IGNORE);
		searchOpts.setRadicalMatching(RADICAL_MATCHING_IGNORE);
		searchOpts.setValenceMatching(false);
		searchOpts.setStereoModel(STEREO_MODEL_GLOBAL);
		searchOpts.addUserComparator(new ExplicitHMatcher());
		final MolSearch search = new MolSearch();
		search.setSearchOptions(searchOpts);
		search.setTarget(respMol);
		search.setQuery(authMol);
		try {
			match = search.findFirst() != null;
			debugPrint(SELF + "for target ", respMol, " and query ", 
					authMol, ", search result is ", match);
			// workaround for JChem 5.9 bug of not recognizing allene
			// enantiomers as such
			if (!match && needAlleneEnant(respMol, matchFlags)) {
				match = matchExact(ChemUtils.getMirror(respMol), authMol, 
						matchFlags & ~CONSIDER_ENANT);
				debugPrint(SELF + "contains an allene, so checked its "
						+ "enantiomer: result is ", match);
			} // if need to redo search with enantiomer
		} catch (MolFormatException e1) { // extremely unlikely
			e1.printStackTrace();
			throw new MolCompareException(e1.getMessage());
		} catch (SearchException e2) {
			e2.printStackTrace();
			throw new MolCompareException(e2.getMessage());
		}
		return match;
	} // matchSigmaNetworkNoClone(Molecule, Molecule, boolean, int)

	/** Prepare molecule for &sigma;-bond network comparison:
	 * explicitize H atoms (unless is Lewis structure), convert 
	 * cis-or-trans double bonds from wavy to criss-cross representation,
	 * convert multiple bonds to single.
	 * @param	mol	a molecule
	 * @param	isLewis	whether the molecule is a Lewis structure
	 */
	private static void sigmaNormalize(Molecule mol, boolean isLewis) {
		if (!isLewis) ChemUtils.explicitizeHnoClone(mol);
		StereoFunctions.allWavyToCrissCross(mol);
		for (final MolBond bond : mol.getBondArray()) {
			final int bondType = bond.getType();
			if (!Utils.among(bondType, MolBond.UP, MolBond.DOWN, 
					MolBond.WAVY)) {
				bond.setType(1);
			} // if not stereobond
		} // for each bond
	} // sigmaNormalize(Molecule)

	/** Determines whether a response molecule (target in ChemAxon parlance)
	 * contains an author's substructure (query).
	 * Explicit H atoms in the author's substructure must be present (implicit
	 * or explicit) in the response.  (Value of setImplicitHMatching() is set to
	 * IMPLICIT_H_MATCHING_ENABLED by default.  DISABLED would mean that explicit
	 * H atoms in the author's substructure would have to be explicit in the
	 * response.)
	 * Charges, isotopes, and radicals in the author's structure must be present
	 * in the response, and vice versa.
	 * @param	respMol	the response molecule
	 * @param	substructStr	the author's substructure as a String
	 * @return	true if the response contains the substructure
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process or the substructure can't be imported
	 */
	public static boolean containsSubstruct(Molecule respMol,
			String substructStr) throws MolCompareException {
		return containsSubstruct(respMol, substructStr, EXACT_CHG_RAD_ISO);
	} // containsSubstruct(Molecule, String)

	/** Determines whether a response molecule (target in ChemAxon parlance)
	 * contains an author's substructure (query).
	 * Explicit H atoms in the author's substructure must be present (implicit
	 * or explicit) in the response.  (Value of setImplicitHMatching() is set to
	 * IMPLICIT_H_MATCHING_ENABLED by default.  DISABLED would mean that explicit
	 * H atoms in the author's substructure would have to be explicit in the
	 * response.)
	 * Charges, isotopes, and radicals in the author's structure must be present
	 * in the response, and vice versa.
	 * @param	respMol	the response molecule
	 * @param	substruct	the author's substructure as a molecule
	 * @return	true if the response contains the substructure
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean containsSubstruct(Molecule respMol,
			Molecule substruct) throws MolCompareException {
		return containsSubstruct(respMol, substruct, EXACT_CHG_RAD_ISO);
	} // containsSubstruct(Molecule, Molecule)

	/** Determines whether a response molecule (target in ChemAxon parlance)
	 * contains an author's substructure (query).
	 * Explicit H atoms in the author's substructure must be present (implicit
	 * or explicit) in the response.  (Value of setImplicitHMatching() is set to
	 * IMPLICIT_H_MATCHING_ENABLED by default.  DISABLED would mean that explicit
	 * H atoms in the author's substructure would have to be explicit in the
	 * response.)
	 * @param	respMol	the response molecule
	 * @param	substructStr	the author's substructure as a String
	 * @param	chgRadIso	whether to ignore charge, radical state, and isotope
	 * state, or to require exact matching, or to allow no indication in the
	 * author's structure to match any in the response (JChem default)
	 * @return	true if the response contains the substructure
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process or the substructure can't be imported
	 */
	public static boolean containsSubstruct(Molecule respMol,
			String substructStr, int chgRadIso) throws MolCompareException {
		try {
			final Molecule substruct = MolImporter.importMol(substructStr);
			return containsSubstruct(respMol, substruct, chgRadIso);
		} catch (MolFormatException e) {
			e.printStackTrace();
			throw new MolCompareException(e.getMessage());
		} // try
	} // containsSubstruct(Molecule, String, int)

	/** Determines whether a response molecule (target in ChemAxon parlance)
	 * contains an author's substructure (query).
	 * Explicit H atoms in the author's substructure must be present (implicit
	 * or explicit) in the response.  (Value of setImplicitHMatching() is set to
	 * IMPLICIT_H_MATCHING_ENABLED by default.  DISABLED would mean that explicit
	 * H atoms in the author's substructure would have to be explicit in the
	 * response.)
	 * @param	respMol	the response molecule
	 * @param	substruct	the author's substructure as a molecule
	 * @param	chgRadIso	whether to ignore charge, radical state, and isotope
	 * state, or to require exact matching, or to allow no indication in the
	 * author's structure to match any in the response (JChem default)
	 * @return	true if the response contains the substructure
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean containsSubstruct(Molecule respMol,
			Molecule substruct, int chgRadIso) throws MolCompareException {
		final String SELF = "MolCompare.containsSubstruct: ";
		boolean match = false;
		final MolSearch search = 
				setUpSubstructMatch(respMol, substruct, chgRadIso);
		try {
			match = search.isMatching();
			debugPrint(SELF + "response ", search.getTarget(), ' ',
					match ? "contains" : "does not contain",
					" the substructure, ", search.getQuery());
		} catch (SearchException e) {
			Utils.alwaysPrint("Error in " + SELF);
			e.printStackTrace();
			throw new MolCompareException(ERROR + SELF + e.getMessage());
		}
		return match;
	} // containsSubstruct(Molecule, Molecule, int)

	/** Gets the match indices in a response molecule (target in ChemAxon 
	 * parlance) if it contains an author's substructure (query).
	 * Explicit H atoms in the author's substructure must be present (implicit
	 * or explicit) in the response.  (Value of setImplicitHMatching() is set to
	 * IMPLICIT_H_MATCHING_ENABLED by default.  DISABLED would mean that explicit
	 * H atoms in the author's substructure would have to be explicit in the
	 * response.)
	 * Charges, isotopes, and radicals in the author's structure must be present
	 * in the response, and vice versa.
	 * @param	respMol	the response molecule
	 * @param	substructStr	the author's substructure as a String
	 * @return	array of matches; each match is an array of 1-based indices of 
	 * the target atoms whose positions correspond to the atom indices of the 
	 * query
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process or the substructure can't be imported
	 */
	public static int[][] substructMatches(Molecule respMol, String substructStr) 
			throws MolCompareException {
		return substructMatches(respMol, substructStr, EXACT_CHG_RAD_ISO);
	} // substructMatches(Molecule, String)

	/** Gets the match indices in a response molecule (target in ChemAxon 
	 * parlance) if it contains an author's substructure (query).
	 * Explicit H atoms in the author's substructure must be present (implicit
	 * or explicit) in the response.  (Value of setImplicitHMatching() is set to
	 * IMPLICIT_H_MATCHING_ENABLED by default.  DISABLED would mean that explicit
	 * H atoms in the author's substructure would have to be explicit in the
	 * response.)
	 * Charges, isotopes, and radicals in the author's structure must be present
	 * in the response, and vice versa.
	 * @param	respMol	the response molecule
	 * @param	substruct	the author's substructure as a molecule
	 * @return	array of matches; each match is an array of 1-based indices of 
	 * the target atoms whose positions correspond to the atom indices of the 
	 * query
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static int[][] substructMatches(Molecule respMol, Molecule substruct) 
			throws MolCompareException {
		return substructMatches(respMol, substruct, EXACT_CHG_RAD_ISO);
	} // substructMatches(Molecule, Molecule)

	/** Gets the match indices in a response molecule (target in ChemAxon 
	 * parlance) if it contains an author's substructure (query).
	 * Explicit H atoms in the author's substructure must be present (implicit
	 * or explicit) in the response.  (Value of setImplicitHMatching() is set to
	 * IMPLICIT_H_MATCHING_ENABLED by default.  DISABLED would mean that explicit
	 * H atoms in the author's substructure would have to be explicit in the
	 * response.)
	 * @param	respMol	the response molecule
	 * @param	substructStr	the author's substructure as a String
	 * @param	chgRadIso	whether to ignore charge, radical state, and isotope
	 * state, or to require exact matching, or to allow no indication in the
	 * author's structure to match any in the response (JChem default)
	 * @return	array of matches; each match is an array of 1-based indices of 
	 * the target atoms whose positions correspond to the atom indices of the 
	 * query
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process or the substructure can't be imported
	 */
	public static int[][] substructMatches(Molecule respMol,
			String substructStr, int chgRadIso) throws MolCompareException {
		try {
			final Molecule substruct = MolImporter.importMol(substructStr);
			return substructMatches(respMol, substruct, chgRadIso);
		} catch (MolFormatException e) {
			e.printStackTrace();
			throw new MolCompareException(e.getMessage());
		} // try
	} // substructMatches(Molecule, String, int)

	/** Gets the match indices in a response molecule (target in ChemAxon 
	 * parlance) if it contains an author's substructure (query).
	 * Explicit H atoms in the author's substructure must be present (implicit
	 * or explicit) in the response.  (Value of setImplicitHMatching() is set to
	 * IMPLICIT_H_MATCHING_ENABLED by default.  DISABLED would mean that explicit
	 * H atoms in the author's substructure would have to be explicit in the
	 * response.)
	 * @param	respMol	the response molecule
	 * @param	substruct	the author's substructure as a molecule
	 * @param	chgRadIso	whether to ignore charge, radical state, and isotope
	 * state, or to require exact matching, or to allow no indication in the
	 * author's structure to match any in the response (JChem default)
	 * @return	array of matches; each match is an array of 1-based indices of 
	 * the target atoms whose positions correspond to the atom indices of the 
	 * query
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static int[][] substructMatches(Molecule respMol,
			Molecule substruct, int chgRadIso) throws MolCompareException {
		final String SELF = "MolCompare.substructMatches: ";
		int[][] matches;
		final MolSearch search = 
				setUpSubstructMatch(respMol, substruct, chgRadIso);
		try {
			matches = search.findAll();
			debugPrint(SELF + "response ", search.getTarget(), ' ',
					Utils.isEmpty(matches) ? "does not contain" : "contains", 
					" the substructure, ", search.getQuery());
		} catch (SearchException e) {
			Utils.alwaysPrint("Error in " + SELF);
			e.printStackTrace();
			throw new MolCompareException(ERROR + SELF + e.getMessage());
		}
		return matches;
	} // substructMatches(Molecule, Molecule, int)

	/** Sets up the search for a substructure match.
	 * @param	respMol	the response molecule
	 * @param	substruct	the author's substructure as a molecule
	 * @param	chgRadIso	whether to ignore charge, radical state, and isotope
	 * state, or to require exact matching, or to allow no indication in the
	 * author's structure to match any in the response (JChem default)
	 * @return	the search object
	 */
	private static MolSearch setUpSubstructMatch(Molecule respMol,
			Molecule substruct, int chgRadIso) {
		final String SELF = "MolCompare.setUpSubstructMatch: ";
		final MolSearchOptions searchOpts = new MolSearchOptions(SUBSTRUCTURE);
		searchOpts.setStereoSearchType(STEREO_SPECIFIC);
		searchOpts.setStereoModel(STEREO_MODEL_GLOBAL);
		if (chgRadIso == EXACT_CHG_RAD_ISO) {
			debugPrint(SELF + "exact chgRadIso.");
			searchOpts.setChargeMatching(CHARGE_MATCHING_EXACT);
			searchOpts.setRadicalMatching(RADICAL_MATCHING_EXACT);
			searchOpts.setIsotopeMatching(ISOTOPE_MATCHING_EXACT);
		} else if (chgRadIso == DEFAULT_CHG_RAD_ISO) {
			debugPrint(SELF + "default chgRadIso.");
			searchOpts.setChargeMatching(CHARGE_MATCHING_DEFAULT);
			searchOpts.setRadicalMatching(RADICAL_MATCHING_DEFAULT);
			searchOpts.setIsotopeMatching(ISOTOPE_MATCHING_DEFAULT);
		} else {
			debugPrint(SELF + "ignore chgRadIso.");
			searchOpts.setChargeMatching(CHARGE_MATCHING_IGNORE);
			searchOpts.setRadicalMatching(RADICAL_MATCHING_IGNORE);
			searchOpts.setIsotopeMatching(ISOTOPE_MATCHING_IGNORE);
		} // if chgRadIso
		searchOpts.setValenceMatching(false);
		searchOpts.setVagueBondLevel(VAGUE_BOND_OFF);
		searchOpts.setIgnoreCumuleneOrRingCisTransStereo(false); // odd-numbered cumulenes
		searchOpts.setIgnoreAxialStereo(false);
		searchOpts.setIgnoreSynAntiStereo(true);
		final MolSearch search = new MolSearch();
		search.setSearchOptions(searchOpts);
		final Molecule respMolNorm = 
				Normalize.normalizeCoordinateBonds(respMol);
		final Molecule substructNorm =
				Normalize.normalizeCoordinateBonds(substruct);
		search.setTarget(respMolNorm);
		search.setQuery(substructNorm);
		return search;
	} // setUpSubstructMatch(Molecule, Molecule, int)

	/** Determines whether a diene is a cyclic diene.
	 * @param	diene	the diene
	 * @return	true if the diene is a cyclic diene
	 * @throws	MolFormatException	if the substructure can't be imported into
	 * a Molecule
	 * @throws	MolCompareException	if something goes wrong in the matching 
	 * process
	 */
	public static boolean dieneIsCyclic(Molecule diene) 
			throws MolCompareException, MolFormatException {
		final String SELF = "MolCompare.dieneIsCyclic: ";
		final String cyclicDieneStr = "<?xml version=\"1.0\" ?>"
				+ "<cml>"
				+ "<MDocument>"
				+ "<MChemicalStruct>"
				+ "<molecule molID=\"m1\">"
				+ "<atomArray"
				+ " atomID=\"a1 a2 a3 a4 a5\""
				+ " elementType=\"C C C C C\""
				+ " mrvQueryProps=\"A: A: A: A: A:\""
				+ " mrvLinkNodeRep=\"1 1 1 1 3\""
				+ " mrvLinkNodeOut=\"- - - - a4,a1\""
				+ " x2=\"-3.320625066757202 -4.566491395774229 -4.090628978387009 "
					+ "-2.550621155127395 -2.074758737740174\""
				+ " y2=\"1.1656172583538367 0.2604006598418356 -1.2041467800780574 "
					+ "-1.2041467800780574 0.2604006598418356\""
				+ "/>"
				+ "<bondArray>"
				+ "<bond atomRefs2=\"a1 a2\" order=\"2\" />"
				+ "<bond atomRefs2=\"a2 a3\" order=\"1\" />"
				+ "<bond atomRefs2=\"a3 a4\" order=\"2\" />"
				+ "<bond atomRefs2=\"a4 a5\" order=\"1\" />"
				+ "<bond atomRefs2=\"a1 a5\" order=\"1\" />"
				+ "</bondArray>"
				+ "</molecule>"
				+ "</MChemicalStruct>"
				+ "</MDocument>"
				+ "</cml>";
		return containsSubstruct(diene, cyclicDieneStr);
	} // dieneIsCyclic(Molecule)

	/** Gets the indices within the molecule of both terminal N atoms of 
	 * the azide anion.
	 * @param	mol	the molecule
	 * @return	array of indices within the molecule of terminal N atoms
	 */
	static int[][] getNumsOfAzideAtoms(Molecule mol) {
		return getNumsOfAzideAtoms(mol, false);
	} // getNumsOfAzideAtoms(Molecule)

	/** Gets the indices within the molecule of both terminal N atoms of 
	 * hydrazoic acid or other RN<sub>3</sub>.
	 * @param	mol	the molecule
	 * @return	array of indices within the molecule of terminal N atoms
	 */
	static int[][] getNumsOfAzidoAtoms(Molecule mol) {
		return getNumsOfAzideAtoms(mol, true);
	} // getNumsOfAzidoAtoms(Molecule)

	/** Gets the indices within the molecule of both terminal N atoms of 
	 * N<sub>3</sub><sup>&minus;</sup> or RN<sub>3</sub>
	 * @param	mol	the molecule
	 * @param	neutral	whether to look for N<sub>3</sub><sup>&minus;</sup> or
	 * RN<sub>3</sub>
	 * @return	array of indices within the molecule of terminal N atoms
	 */
	private static int[][] getNumsOfAzideAtoms(Molecule mol, boolean neutral) {
		final String SELF = "MolCompare.getNumsOfAzideAtoms: ";
		final List<int[]> azideNNums = new ArrayList<int[]>();
		final Molecule respMol = Normalize.normalize(mol);
		final MolSearchOptions searchOpts = 
				new MolSearchOptions(neutral ? SUBSTRUCTURE : FULL);
		searchOpts.setExactBondMatching(false);
		searchOpts.setChargeMatching(CHARGE_MATCHING_EXACT);
		searchOpts.setRadicalMatching(RADICAL_MATCHING_EXACT);
		searchOpts.setIsotopeMatching(ISOTOPE_MATCHING_IGNORE);
		searchOpts.setValenceMatching(false);
		searchOpts.setStereoModel(STEREO_MODEL_GLOBAL);
		final String azideStr = neutral 
				? "[N-]=[N+]=N" : "[N-]=[N+]=[N-]";
		final int[] TERMINAL_Ns = new int[] {0, 2};
		try {
			final MolSearch search = new MolSearch();
			final Molecule azide = MolImporter.importMol(azideStr);
			Normalize.normalizeNoClone(azide);
			search.setSearchOptions(searchOpts);
			search.setQuery(azide);
			search.setTarget(respMol);
			debugPrint(SELF + "query ", azide, ", target ", respMol);
			while (true) {
				final int[] matchIndices = search.findNext();
				if (matchIndices == null) break;
				azideNNums.add(new int[] {matchIndices[TERMINAL_Ns[0]], 
						matchIndices[TERMINAL_Ns[1]]});
			} // while there are matches
		} catch (MolFormatException e1) {
			Utils.alwaysPrint("Error in " + SELF);
			e1.printStackTrace();
		} catch (SearchException e2) {
			Utils.alwaysPrint("Error in " + SELF);
			e2.printStackTrace();
		} // try
		return azideNNums.toArray(new int[azideNNums.size()][]);
	} // getNumsOfAzideAtoms(Molecule, boolean)

	/** Gets the indices within the molecule of all C atoms that are between a
	 * cyano group and a carbonyl group, whether protonated or deprotonated.
	 * @param	mol	the molecule
	 * @return	array of indices within the molecule of &alpha;-cyano carbonyl C
	 * atoms
	 */
	static int[] getNumsOfCyanoCarbonylCAtoms(Molecule mol) {
		final String SELF = "MolCompare.getNumsOfCyanoCarbonylCAtoms: ";
		final List<Integer> cyanoCarbonylCNums = new ArrayList<Integer>();
		// final int matchIndex = -1; // unused 11/6/2012
		final MolSearchOptions searchOpts = new MolSearchOptions(SUBSTRUCTURE);
		searchOpts.setStereoSearchType(STEREO_SPECIFIC);
		searchOpts.setStereoModel(STEREO_MODEL_GLOBAL);
		searchOpts.setChargeMatching(CHARGE_MATCHING_EXACT);
		searchOpts.setRadicalMatching(RADICAL_MATCHING_DEFAULT);
		searchOpts.setIsotopeMatching(ISOTOPE_MATCHING_DEFAULT);
		searchOpts.setValenceMatching(false);
		searchOpts.setVagueBondLevel(VAGUE_BOND_OFF);
		searchOpts.setIgnoreCumuleneOrRingCisTransStereo(false); // odd-numbered cumulenes
		searchOpts.setIgnoreAxialStereo(false);
		searchOpts.setIgnoreSynAntiStereo(true);
		final String[] substructStrs = new String[] {
				"O=CCC#N", "[O-]C=CC#N", "O=C[CH-]C#N", "O=CC=C=[N-]"};
		final int CYANO_CARBONYL_C_NUM = 2;
		for (final String substructStr : substructStrs) {
			try {
				final MolSearch search = new MolSearch();
				final Molecule substruct = MolImporter.importMol(substructStr);
				search.setSearchOptions(searchOpts);
				search.setTarget(mol);
				search.setQuery(substruct);
				while (true) {
					final int[] matchIndices = search.findNext();
					if (matchIndices == null) break;
					cyanoCarbonylCNums.add(Integer.valueOf(
							matchIndices[CYANO_CARBONYL_C_NUM]));
				} // while there are matches
			} catch (MolFormatException e1) {
				Utils.alwaysPrint("Error in " + SELF);
				e1.printStackTrace();
			} catch (SearchException e2) {
				Utils.alwaysPrint("Error in " + SELF);
				e2.printStackTrace();
			} // try
		} // for each substructure
		return Utils.listToIntArray(cyanoCarbonylCNums);
	} // getNumsOfCyanoCarbonylCAtoms(Molecule)

/* ************ Begin conformer comparison functions *************/

	/** Determines whether two molecules are identical and have the same
	 * 3D conformation in a ring or about a bond.
	 * @param	cpdMRV1	String representation of a molecule
	 * @param	cpdMRV2	String representation of another molecule
	 * @return	true if the molecules are identical and have the same
	 * conformation in a ring or about a bond
	 */
	public static boolean matchConformers(String cpdMRV1, String cpdMRV2) {
		// if map numbers don't match exactly, not the same response
		final String SELF = "MolCompare.matchConformers: ";
		boolean match = false;
		try {
			final Molecule cpd1 = MolImporter.importMol(cpdMRV1);
			final Molecule cpd2 = MolImporter.importMol(cpdMRV2);
			if (!matchPrecise(cpd1, cpd2)) {
				debugPrint(SELF + "cpds don't have same structures");
				return match;
			} // if structures don't match exactly
			// find the indices of mapped atoms in cpd 1
			List<Integer> cpd1MappedAtomIds = new ArrayList<Integer>();
			for (int atomNum = 0; atomNum < cpd1.getAtomCount(); atomNum++) {
				final MolAtom atom = cpd1.getAtom(atomNum);
				if (atom.getAtomMap() != 0) {
					cpd1MappedAtomIds.add(Integer.valueOf(atomNum));
					addAttachedMapped(cpd1, atom, cpd1MappedAtomIds);
					break;
				} // if atom is mapped
			} // for each atom atomNum in molecule
			int numMappedCpd1 = cpd1MappedAtomIds.size();
			if (numMappedCpd1 == 0) {
				// try to find a saturated six-membered ring
				cpd1MappedAtomIds = findAndMarkCyclohex(cpd1);
				numMappedCpd1 = cpd1MappedAtomIds.size();
			} // if no mapped atoms were found
			if (numMappedCpd1 > 0) {
				final StringBuilder out = new StringBuilder();
				boolean first = true;
				for (final Integer cpd1MappedAtomId : cpd1MappedAtomIds) {
					final int index = cpd1MappedAtomId.intValue();
					if (first) first = false;
					else out.append(", ");
					Utils.appendTo(out, 
							cpd1.getAtom(index).getSymbol(), index + 1);
				} // for each mapped atom
				debugPrint(SELF + "found or added these mapped atoms in cpd1: ", 
						out);
			} // if numMappedCpd1 > 0
			// find the mapped atoms in cpd 2
			List<Integer> cpd2MappedAtomIds = new ArrayList<Integer>();
			for (int atomNum = 0; atomNum < cpd2.getAtomCount(); atomNum++) {
				final MolAtom atom = cpd2.getAtom(atomNum);
				if (atom.getAtomMap() != 0) {
					cpd2MappedAtomIds.add(Integer.valueOf(atomNum));
					addAttachedMapped(cpd2, atom, cpd2MappedAtomIds);
					break;
				} // if atom is mapped
			} // for each atom atomNum in molecule
			int numMappedCpd2 = cpd2MappedAtomIds.size();
			if (numMappedCpd2 == 0) {
				// try to find a saturated six-membered ring
				cpd2MappedAtomIds = findAndMarkCyclohex(cpd2);
				numMappedCpd2 = cpd2MappedAtomIds.size();
			} // if no mapped atoms were found
			if (numMappedCpd2 > 0) {
				final StringBuilder out = new StringBuilder();
				boolean first = true;
				for (final Integer cpd2MappedAtomId : cpd2MappedAtomIds) {
					final int index = cpd2MappedAtomId.intValue();
					if (first) first = false;
					else out.append(", ");
					Utils.appendTo(out, 
							cpd2.getAtom(index).getSymbol(), index + 1);
				} // for each mapped atom
				debugPrint(SELF + "found or added these mapped atoms in cpd2: ", 
						out);
			} // if numMappedCpd2 > 0
			if (numMappedCpd1 != numMappedCpd2)
				debugPrint(SELF + "conformers must have same number of mapped "
						+ "atoms (", numMappedCpd1, " and ", numMappedCpd2, ")");
			else if (Utils.among(numMappedCpd1, BOND_SIZE, CHAIR_SIZE))
				match = matchConformers(cpd1, cpd2, cpd1MappedAtomIds,
						numMappedCpd1 == CHAIR_SIZE);
			else debugPrint(SELF + "number of mapped atoms (", 
					numMappedCpd1, ") indicates neither acyclic nor chair");
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "can't convert either to Molecule");
		} catch (MolCompareException e) {
			Utils.alwaysPrint(SELF + "can't determine whether the cpds are identical");
		}
		return match;
	} // matchConformers(String, String)

	/** Adds to a list of contiguous mapped atoms in a molecule.
	 * @param	cpd	a molecule
	 * @param	start	atom from which to start looking for contiguous mapped
	 * atoms
	 * @param	cpd1MappedAtomIds	atoms already found to be mapped; modified by
	 * the method
	 */
	private static void addAttachedMapped(Molecule cpd, MolAtom start,
			List<Integer> cpd1MappedAtomIds) {
		final MolAtom[] ligs = start.getLigands();
		for (final MolAtom lig : ligs) {
			final int ligIndex = cpd.indexOf(lig);
			if (lig.getAtomMap() != 0 && !cpd1MappedAtomIds.contains(ligIndex)) {
				cpd1MappedAtomIds.add(Integer.valueOf(ligIndex));
				addAttachedMapped(cpd, lig, cpd1MappedAtomIds); // recursive call
			} // if lig is mapped
		} // for each ligand of mapped atom
	} // addAttachedMapped(List<Integer>)

	/** Finds a saturated six-membered ring and maps its atoms with numbers
	 * 1&ndash;6.  Modifies the molecule! Called from matchConformers() here and
	 * also from ConformChair.
	 * @param	mol	a molecule
	 * @return	list of indices of the six-membered ring, and its atoms
	 * are mapped with numbers 1&ndash;6; empty list if not found
	 */
	public static List<Integer> findAndMarkCyclohex(Molecule mol) {
		final String SELF = "MolCompare.findAndMarkCyclohex: ";
		final List<Integer> cpd1MappedAtomIds = new ArrayList<Integer>();
		// look first for saturated carbocycle, then saturated
		// one-heteroatom, then any saturated six-membered ring
		final String[] rings = 
				new String[] {"C1CCCCC1", "*1CCCCC1", "*1*****1"};
		try {
			final MolSearch search = new MolSearch();
			search.setTarget(mol);
			for (final String ring : rings) {
				final Molecule query = MolImporter.importMol(ring);
				search.setQuery(query);
				final int[] sixRing = search.findFirst();
				if (sixRing != null) {
					debugPrint(SELF + "Found ", ring, 
							" ! marking its atoms ...");
					for (int mapNum = 0; mapNum < 6; mapNum++) {
						debugPrint("Assigning map number ",
								mapNum + 1, " to atom ", sixRing[mapNum] + 1);
						mol.getAtom(sixRing[mapNum]).setAtomMap(mapNum + 1);
						cpd1MappedAtomIds.add(Integer.valueOf(sixRing[mapNum]));
					} // for each six-membered ring atom
					break;
				} // if there's a six-membered ring
			} // for each ring to search for
		} catch (SearchException e) {
			Utils.alwaysPrint(SELF + "SearchException: ", e.getMessage());
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "MolFormatException: ", e.getMessage());
		} // try
		if (cpd1MappedAtomIds.isEmpty()) {
			debugPrint(SELF + "unable to find saturated six-membered ring.");
		} // if list is empty
		return cpd1MappedAtomIds;
	} // findAndMarkCyclohex(Molecule)

	/** Determines whether two identical molecules have the same 3D
	 * conformation.
	 * @param	cpd1	a molecule
	 * @param	cpd2	a molecule
	 * @param	cpd1MappedAtomIds	indices of atoms from cpd1 about which to 
	 * measure the conformation
	 * @param	chair	whether to look at chair or bond conformation
	 * @return	true if the molecules have the same 3D conformation
	 */
	private static boolean matchConformers(Molecule cpd1,
			Molecule cpd2, List<Integer> cpd1MappedAtomIds, boolean chair) {
		final String SELF = "MolCompare.matchConformers: ";
		debugPrintMRV(SELF + "cpd1:\n", cpd1, "\ncpd2:\n", cpd2);
		final MolSearchOptions searchOpts = new MolSearchOptions(FULL);
		searchOpts.setStereoModel(STEREO_MODEL_GLOBAL);
		searchOpts.setChargeMatching(CHARGE_MATCHING_EXACT);
		searchOpts.setIsotopeMatching(ISOTOPE_MATCHING_EXACT);
		searchOpts.setRadicalMatching(RADICAL_MATCHING_EXACT);
		searchOpts.setVagueBondLevel(VAGUE_BOND_OFF);
		searchOpts.setStereoSearchType(STEREO_SPECIFIC);
		searchOpts.setExactBondMatching(true);
		searchOpts.setOrderSensitiveSearch(true); // all permutations needed
		searchOpts.setIgnoreCumuleneOrRingCisTransStereo(false); // odd-numbered cumulenes
		searchOpts.setIgnoreAxialStereo(false); // allenes & biaryls
		searchOpts.setIgnoreSynAntiStereo(true);
		final MolSearch search = new MolSearch();
		search.setSearchOptions(searchOpts);
		search.setTarget(cpd2);
		search.setQuery(cpd1);
		try {
			while (true) {
				final int[] match = search.findNext();
				if (match == null) break;
				// get indices of attachment points of unique groups on two mapped
				// atoms of cpd1
				debugPrint(SELF + "match array is [", Utils.join(match, 1), "]");
				boolean badMapMatch = false;
				for (final Integer cpd1MappedAtomId : cpd1MappedAtomIds) {
					final int matchAtomId = match[cpd1MappedAtomId.intValue()];
					if (cpd2.getAtom(matchAtomId).getAtomMap() == 0) {
						debugPrint(SELF +
								"cpd2 doesn't have mapped atoms in right place");
						badMapMatch = true;
						break;
					} // if atom in cpd2 corresponding to mapped atom is unmapped
				} // for each mapped atom
				if (badMapMatch) continue;
				final boolean foundMatch = (chair 
						? matchChairConformation(cpd1, cpd2, match, cpd1MappedAtomIds)
						: matchBondConformation(cpd1, cpd2, match, cpd1MappedAtomIds));
				if (foundMatch) {
					// found a match!
					debugPrint(SELF + "Conformers match!");
					return true;
				} // if did not find a mismatch
			} // while there are matches
		} catch (SearchException e) {
			Utils.alwaysPrint(SELF + "search exception");
		} // try
		// no match found
		debugPrint("Could not find match between conformers");
		return false;
	} // matchConformers(Molecule, Molecule, List<Integer>, boolean)

	/** Determines whether in a particular match, two identical molecules have
	 * the same 3D conformation about a bond.
	 * @param	cpd1	a molecule
	 * @param	cpd2	a molecule
	 * @param	match	a match between the two compounds
	 * @param	cpd1MappedAtomIds	indices of atoms from cpd1 about which to 
	 * measure the conformation
	 * @return	true if the molecules have the same 3D conformation about a bond
	 */
	private static boolean matchBondConformation(Molecule cpd1,
			Molecule cpd2, int[] match, List<Integer> cpd1MappedAtomIds) {
		final String SELF = "MolCompare.matchBondConformation: ";
		final int cpd1BondAtom1Id = cpd1MappedAtomIds.get(0).intValue();
		final List<Integer> testSubstitIndicesAtom0 =
				getTestSubstitIndices(cpd1,
						cpd1.getAtom(cpd1BondAtom1Id));
		final int cpd1BondAtom2Id = cpd1MappedAtomIds.get(1).intValue();
		final List<Integer> testSubstitIndicesAtom1 =
				getTestSubstitIndices(cpd1,
						cpd1.getAtom(cpd1BondAtom2Id));
		// dihedral angles between unique groups on two mapped atoms in
		// cpd1 must match the same in cpd2
		boolean foundMismatch = false;
		final VectorMath dihedral1 = new VectorMath(cpd1);
		final VectorMath dihedral2 = new VectorMath(cpd2);
		int atom0grpNum = 0;
		for (final Integer atom0grpIndex : testSubstitIndicesAtom0) {
			final int cpd1Subst1Id = atom0grpIndex.intValue();
			atom0grpNum++;
			int atom1grpNum = 0;
			for (final Integer atom1grpIndex : testSubstitIndicesAtom1) {
				final int cpd1Subst2Id = atom1grpIndex.intValue();
				double angle1 = dihedral1.calcDihedral(new int[]
						{cpd1Subst1Id,
						cpd1BondAtom1Id,
						cpd1BondAtom2Id,
						cpd1Subst2Id},
						360);
				double angle2 = dihedral2.calcDihedral(new int[]
						{match[cpd1Subst1Id],
						match[cpd1BondAtom1Id],
						match[cpd1BondAtom2Id],
						match[cpd1Subst2Id]},
						360);
				if (angle1 < 0) angle1 += 360;
				if (angle2 < 0) angle2 += 360;
				debugPrint(SELF, "atom 0 group ", atom0grpNum, 
						" with index ", cpd1Subst1Id + 1, 
						" in cpd1 and index ", match[cpd1Subst1Id] + 1,
						" in cpd2, and atom 1 group ",
						++atom1grpNum, " with index ",
						cpd1Subst2Id + 1, " in cpd1 and index ",
						match[cpd1Subst2Id] + 1,
						" in cpd2, have dihedral angles of ",
						((int) angle1), " deg in cpd1 and ",
						((int) angle2), " deg in cpd2");
				if (!dihedralsEqual(angle1, angle2)) { // within tolerance
					foundMismatch = true;
					break;
				} // if found a mismatch
			} // for each unique group on atom2
			if (foundMismatch) break;
		} // for each unique group on atom1
		return !foundMismatch;
	} // matchBondConformation(Molecule, Molecule, int[], List<Integer>)

	/** Determines whether in a particular match, two identical molecules have
	 * the same 3D conformation in a six-membered ring.
	 * @param	cpd1	a molecule
	 * @param	cpd2	a molecule
	 * @param	match	a match between the two compounds
	 * @param	cpd1MappedAtomIds	indices of atoms from cpd1 about which to measure
	 * the conformation
	 * @return	true if the molecules have the same 3D conformation in a 
	 * six-membered ring
	 */
	private static boolean matchChairConformation(Molecule cpd1,
			Molecule cpd2, int[] match, List<Integer> cpd1MappedAtomIds) {
		final String SELF = "MolCompare.matchChairConformation: ";
		boolean foundMismatch = false;
		for (int atomNum = 0; atomNum < CHAIR_SIZE; atomNum++) {
			final int cpd1RingAtomId = 
					cpd1MappedAtomIds.get(atomNum).intValue();
			final List<Integer> testSubstitIndicesRingAtomCpd1 =
					getTestSubstitIndices(cpd1,
							cpd1.getAtom(cpd1RingAtomId));
			if (testSubstitIndicesRingAtomCpd1.isEmpty())
				continue; // two identical or no substituents
			final int cpd1RingSubstId =
					testSubstitIndicesRingAtomCpd1.get(0).intValue();
			final int cpd1NextRingAtomId = cpd1MappedAtomIds.get(
					(atomNum + 1) % CHAIR_SIZE).intValue();
			final int cpd1FurtherRingAtomId = cpd1MappedAtomIds.get(
					(atomNum + 2) % CHAIR_SIZE).intValue();
			// only need to test one substituent for same orientation in
			// two cpds
			final VectorMath dihedral1 = new VectorMath(cpd1);
			final VectorMath dihedral2 = new VectorMath(cpd2);
			final double angle1 = dihedral1.calcDihedral(new int[]
					{cpd1RingSubstId,
					cpd1RingAtomId,
					cpd1NextRingAtomId,
					cpd1FurtherRingAtomId});
			final double angle2 = dihedral2.calcDihedral(new int[]
					{match[cpd1RingSubstId],
					match[cpd1RingAtomId],
					match[cpd1NextRingAtomId],
					match[cpd1FurtherRingAtomId]});
			debugPrint(SELF + "cpd1 ring atom ",
					atomNum + 1, " with index ", cpd1RingAtomId + 1,
					" has substituent with index ", 
					cpd1RingSubstId + 1,
					" with ", ((int) angle1), 
					" deg dihedral angle; cpd2 ring atom ", 
					atomNum + 1, " with index ",
					match[cpd1RingAtomId] + 1,
					" has same substituent with index ",
					match[cpd1RingSubstId] + 1,
					" with ", ((int) angle2), " deg dihedral angle");
			if (!dihedralsEqual(angle1, angle2)) { // within tolerance
				debugPrint(SELF + "dihedral angles don't match; "
						+ "going to next match");
				foundMismatch = true;
				break;
			} // if not same orientation in both cpds
		} // for each ring atom atomNum
		return !foundMismatch;
	} // matchChairConformation(Molecule, Molecule, int[], List<Integer>)

	/** Finds which groups attached to the atom are unique, returns a list of the
	 * indices of the attachment points of those groups.
	 * @param	mol	a molecule
	 * @param	atom	atom whose unique substituents to find
	 * @return	a List of indices of atoms attached to atom that are part
	 * of unique groups attached to atom
	 */
	private static List<Integer> getTestSubstitIndices(
			Molecule mol, MolAtom atom) {
		final String SELF = "MolCompare.getTestSubstitIndices: ";
		debugPrint(SELF + "getting list of unique substituents of ",
				atom, mol.indexOf(atom) + 1);
		final List<String> testSubstits = new ArrayList<String>();
		final List<Integer> testSubstitIndices = new ArrayList<Integer>();
		int ligNum = 0;
		for (final MolAtom ligand : atom.getLigands()) {
			ligNum++;
			if (ligand.getAtomMap() == 0) {
				final String substit = getSubstituent(mol, atom, ligNum - 1);
				debugPrint(SELF + "atom ", atom, mol.indexOf(atom) + 1, 
						"'s ", ligNum, ligNum == 1 ? "st" : ligNum == 2
							? "nd" : ligNum == 3 ? "rd" : "th", " ligand, ", 
						ligand, mol.indexOf(ligand) + 1, ", is 1st atom of "
						+ "substituent ", substit, "; is it a duplicate?");
				// will test if there are 1 or 3 of the substituents attached 
				// to the atom, but not if there are 2
				if (testSubstits.contains(substit)) {
					debugPrint(SELF + "substituent ", substit, " is already "
							+ "in list of unique substituents of ", atom, 
							mol.indexOf(atom) + 1, "; removing from list");
					final int arrayListIndex = testSubstits.indexOf(substit);
					testSubstits.remove(arrayListIndex);
					testSubstitIndices.remove(arrayListIndex);
				} else {
					debugPrint(SELF + "substituent ", substit, " is not "
							+ "already in list of unique substituents of ",
							atom, mol.indexOf(atom) + 1, "; adding to list");
					testSubstits.add(substit);
					testSubstitIndices.add(mol.indexOf(ligand));
				} // if new substituent is already in list of unique ones
			} else {
				debugPrint(SELF + "atom ", ligand, mol.indexOf(ligand) + 1,
						" is mapped; don't consider it.");
			} // if ligand is mapped
		} // for each ligand of mapped atom in cpd1
		debugPrint(SELF + " atom ", atom, mol.indexOf(atom) + 1, 
				" has ", testSubstits.size(),
				" unique substituent(s) whose orientation we will test: ");
		for (int substitNum = 0; substitNum < testSubstits.size(); substitNum++)
			debugPrint("   ", testSubstits.get(substitNum),
					", index ", testSubstitIndices.get(substitNum) + 1);
		return testSubstitIndices;
	} // getTestSubstitIndices(Molecule, atom)

	/** Gets a description of a group attached to an atom.
	 * @param	mol	a molecule
	 * @param	atom	atom bearing the attached group
	 * @param	ligIndex	0-based index of the attached group among the
	 * ligands of atom
	 * @return	CXSMILES representation of the attached group, with a pseudoatom
	 * replacing the attachment point
	 */
	private static String getSubstituent(Molecule mol, MolAtom atom,
			int ligIndex) {
		final String SELF = "MolCompare.getSubstituent: ";
		// get indices of reference atom and attached group
		final int atomIndex = mol.indexOf(atom);
		final int ligAtomIndex = mol.indexOf(atom.getLigand(ligIndex));
		// make clone to modify, fragment
		final Molecule clonedCpd = mol.clone();
		// add attachment point to ligand, remove bond from ligand to mapped atom
		final MolAtom attachPt = new MolAtom(MolAtom.PSEUDO);
		attachPt.setAliasstr("ATTACH_PT");
		clonedCpd.add(attachPt);
		final MolAtom cloneLigand = clonedCpd.getAtom(ligAtomIndex);
		final MolBond ligToAttachPt = new MolBond(cloneLigand, attachPt);
		clonedCpd.add(ligToAttachPt);
		clonedCpd.removeBond(clonedCpd.getAtom(atomIndex).getBond(ligIndex));
		// fragment clone, find the fragment with the attachment point
		final Molecule[] molFragList = clonedCpd.convertToFrags();
		for (final Molecule molFrag : molFragList) {
			if (molFrag.contains(attachPt)) {
				// return the fragment that contains the attachment point
				final String smiles = MolString.toString(molFrag);
				debugPrint(SELF + "returning fragment ", smiles);
				return smiles;
			} // if fragment contains the attachment point
		}
		debugPrint(SELF + "couldn't find fragment with the attachment point");
		return null;
	} // getSubstituent(Molecule, MolAtom, int)

	/** Determines whether two dihedral angles are equal.
	 * @param	angle1	a dihedral angle (in degrees)
	 * @param	angle2	another dihedral angle (in degrees)
	 * @return	true if the angles are equal within tolerance
	 */
	private static boolean dihedralsEqual(double angle1, double angle2) {
		final double TOLERANCE = 15.0;
		return (Math.abs(angle1 - angle2) <= TOLERANCE);
	} // dihedralsEqual(double, double)

	/** Disables external instantiation. */
	private MolCompare() { }

} // MolCompare
