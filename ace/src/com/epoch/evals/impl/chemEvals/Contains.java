package com.epoch.evals.impl.chemEvals;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.sss.SearchConstants;
import chemaxon.sss.search.MolSearch;
import chemaxon.sss.search.MolSearchOptions; // JChem 5.0
import chemaxon.sss.search.SearchException;
import chemaxon.struc.Molecule;
import chemaxon.struc.MoleculeGraph;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import chemaxon.struc.QueryBond;
import com.epoch.chem.MolCompare;
import com.epoch.chem.MolCompareException;
import com.epoch.chem.Normalize;
import com.epoch.chem.chemConstants.ChemConstants;
import com.epoch.evals.impl.implConstants.CountConstants;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.Counter;
import com.epoch.exceptions.ParameterException;
import com.epoch.mechanisms.Mechanism;
import com.epoch.mechanisms.MechStage;
import com.epoch.mechanisms.MechUtils;
import com.epoch.responses.Response;
import com.epoch.substns.SubstnUtils;
import com.epoch.synthesis.Synthesis;
import com.epoch.synthesis.SynthStage;
import com.epoch.synthesis.SynthUtils;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** If {no, the only, exactly one, any, not all, all} compounds in the
 * response {contain, do not contain} the {substructure, skeleton} ...  */
public class Contains extends Counter implements ChemConstants, 
		CountConstants, EvalInterface, SearchConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	private static void debugPrintSMARTS(Object... msg) {
		// Utils.printToLog(msg, SMARTS);
	}

	/** Whether to look for substructure or skeleton. See the documentation for
	 * the difference.  */
	private int method;
		/** Value for method.  */
		public static final int SUBSTRUCT = 1;
		/** Value for method.  */
		public static final int SKELETON = 2;
	/** Whether to require exact matching of charges, radical states, and
	 * isotope states, or not to require any matching, or whether to require
	 * matching only if indicated in author structure. */
	private int chgRadIso;
		/** Value for chgRadIso. */
		public static final int EXACT = EXACT_CHG_RAD_ISO;
		/** Value for chgRadIso. */
		public static final int DEFAULT = DEFAULT_CHG_RAD_ISO;
		/** Value for chgRadIso. */
		public static final int IGNORE = IGNORE_CHG_RAD_ISO;

	/** Constructor. */
	public Contains() { // default values
		howMany = NONE;
		method = SUBSTRUCT;
		chgRadIso = EXACT;
	} // Contains()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>howMany</code>/<code>method</code>/<code>chgRadIso</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public Contains(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 3) {
			howMany = MathUtils.parseInt(splitData[0]);
			method = MathUtils.parseInt(splitData[1]);
			chgRadIso = MathUtils.parseInt(splitData[2]);
		} else {
			throw new ParameterException("Contains ERROR: unknown input data "
					+ "'" + data + "', which has " + splitData.length + " tokens");
		}
		debugPrint("Contains: ", toEnglish());
	} // Contains(String)

	/** Gets a string representation of data (other than a molecule) that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>howMany</code>/<code>method</code>/<code>chgRadIso</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(howMany, '/', method, '/', chgRadIso);
	} // getCodedData()

	/** Gets an English-language description of this evaluator.
	 * @param	qDataTexts	the text of question data of this question, if any;
	 * not used, but required by interface
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question
	 * @return	short string describing this evaluator in English
	 */
	public final String toEnglish(String[] qDataTexts, boolean forPermissibleSM) {
		return toEnglish();
	} // toEnglish(String[], boolean)

	/** Gets an English-language description of this evaluator.
	 * @return	short string describing this evaluator in English
	 */
	public final String toEnglish() {
		final StringBuilder words = Utils.getBuilder(
				"If", HOWMANY_ENGL[howMany - 1],
				"compound in the response contains the ",
				method == SUBSTRUCT ? "substructure " : "skeleton ",
				molName == null
					? "indicated by the specified compound" : molName);
		switch (chgRadIso) {
			case EXACT:
				words.append(
					" (charges, radicals, isotopes must match exactly)");
				break;
			case DEFAULT:
				words.append(" (charges, radicals, isotopes in the author's "
						+ "structure must be present in the response)");
				break;
			case IGNORE:
				words.append(" (ignoring charges, radicals, isotopes)");
				break;
			default:
				System.out.println("Contains: bad chgRadIso");
				break;
		} // switch
		return words.toString();
	} // toEnglish()

	/** Determines how many molecules in the response contain the
	 * indicated skeleton or substructure.
	 * @param	response	a parsed response
	 * @param	authStruct	String representation of a molecule that the
	 * evaluator compares to the response
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified
	 * with color or a message describing an inability to evaluate the response
	 * because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authStruct) {
		final String SELF = "Contains.isResponseMatching: ";
		final OneEvalResult evalResult =  new OneEvalResult();
		Molecule authMol = null;
		try {
			authMol = (!Utils.isEmpty(response.rGroupMols)
					? SubstnUtils.substituteRGroups(authStruct,
						response.rGroupMols, SubstnUtils.ADD_H_ATOMS)
					: MolImporter.importMol(authStruct));
			if (method == SKELETON) {
				// modify author's structure for skeleton search
				// so that less oxidized bonds in authMol
				// match more oxidized bonds in response
				authMol.aromatize(MoleculeGraph.AROM_BASIC);
				for (final MolBond bond : authMol.getBondArray()) {
					final int bondType = bond.getType();
					if (bondType == 1) {
						bond.setType(MolBond.ANY);
					} else if (Utils.among(bondType, 2, MolBond.AROMATIC)) {
						final MolBond doubleOrAromOrTriple =
								new QueryBond(bond.getAtom1(),
										bond.getAtom2(), "=,:,#");
						authMol.removeBond(bond);
						authMol.add(doubleOrAromOrTriple);
					}
				} // for each bond in query
				debugPrintSMARTS(SELF + "After adjusting bonds, "
						+ "query = ", authMol);
			}
		} catch (MolFormatException e) {
			System.out.println(" MOLFORMAT EXCEPTION FOR " + authStruct);
			e.printStackTrace();
			evalResult.verificationFailureString = e.getMessage();
			return evalResult;
		} // try
		debugPrint(SELF + "normalizing author reference.");
		int[] matches = new int[2];
		final List<Integer> matchStages = new ArrayList<Integer>();
		final Mechanism mechanism = response.getMechanism();
		final Synthesis synthesis = response.getSynthesis();
		final boolean isMechanism = mechanism != null;
		final boolean isSynthesis = synthesis != null;
		Normalize.normalizeNoClone(authMol, isMechanism);
		try {
			if (isMechanism) {
				matches = getMatches(mechanism, authMol, matchStages);
			} else if (isSynthesis) {
				matches = getMatches(synthesis.getTargetStage(),
						authMol, matchStages, matches);
				// ignore Jlint complaint about line above.  Raphael 11/2010
			} else {
				Molecule respMol = null;
				// response Molecule must be cloned to avoid corruption when
				// fragmented
				if (response.normalized != null) {
					debugPrint(SELF + "response molecule(s) already normalized.");
					respMol = response.normalized.clone();
				} else try {
					debugPrint(SELF + "normalizing "
							+ "response molecule(s) for the first time.");
					response.normalized =
							Normalize.normalize(response.moleculeObj);
					respMol = response.normalized.clone();
				} catch (Exception e) {
					System.out.println("Exception thrown when converting or "
						+ "normalizing:\n" + response.unmodified);
					e.printStackTrace();
					evalResult.verificationFailureString = e.getMessage();
					return evalResult;
				} // try
				final Molecule[] respFrags = respMol.convertToFrags();
				debugPrint(SELF + "found ", respFrags.length, 
						" molecule(s) in response");
				if (Utils.isEmpty(respFrags)) // something is very wrong
					debugPrintMRV("respMol is:\n", respMol);
				matches = getMatches(respFrags, authMol, BREAK_AT_1ST_MATCH);
			}
		} catch (Exception ex) {
			System.out.println("Caught exception:\n" + ex.getMessage());
			evalResult.verificationFailureString = ex.getMessage();
		}
		evalResult.isSatisfied = getIsSatisfied(matches);
		debugPrint(SELF + "matches = ", matches[MATCHES], ", nonmatches = ",
				matches[NONMATCHES], ", howMany = ", howMany, 
				", isSatisfied = ", evalResult.isSatisfied);
		if (evalResult.isSatisfied && Utils.among(howMany, ANY, ONE)) {
			if (isMechanism) {
				// highlight stages that contain the compound
				final MechUtils util = new MechUtils(mechanism);
				evalResult.modifiedResponse = util.colorStages(matchStages);
			} else if (isSynthesis) {
				// highlight boxes that contain the compound
				final SynthUtils util = new SynthUtils(synthesis);
				evalResult.modifiedResponse = util.colorBoxes(matchStages);
			} // if mechanism or synthesis
		} // if might want to color response
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Gets the result for all stages in a mechanism response.
	 * @param	mechanism	the mechanism response
	 * @param	authMol	substructure or skeleton to look for
	 * @param	matchStages	list of stages that have matches
	 * @return	the number of matches and nonmatches of the entire mechanism
	 * @throws	MolCompareException	if something goes wrong when comparing 
	 * the author's molecule to response molecules
	 */
	private int[] getMatches(Mechanism mechanism, Molecule authMol,
			List<Integer> matchStages) throws MolCompareException {
		final String SELF = "Contains.getMatches: ";
		// look for matches stage by stage so finds can be highlighted
		final int[] matches = new int[2];
		for (int stageIndex = 0; stageIndex <
				mechanism.getNumStages(); stageIndex++) {
			final MechStage stage = mechanism.getStage(stageIndex);
			// ignore Jlint complaint about line above.  Raphael 11/2010
			final Molecule[] respFrags = stage.getNormalizedMolArray();
			debugPrint(SELF + "Found ", respFrags.length,
					" normalized molecule(s) in stage ", stageIndex + 1);
			final int[] newMatches = getMatches(respFrags, authMol);
			if (newMatches[MATCHES] > 0) {
				matchStages.add(Integer.valueOf(stageIndex));
			} // if there are matches
			matches[MATCHES] += newMatches[MATCHES];
			matches[NONMATCHES] += newMatches[NONMATCHES];
		} // for each stage
		return matches;
	} // getMatches(Mechanism, Molecule, List<Integer>)

	/** Gets the result for a particular stage in a multistep synthesis response.
	 * @param	stage	a stage in a synthesis
	 * @param	authMol	substructure or skeleton to look for
	 * @param	matchBoxes	list of stages that have matches
	 * @param	matches	the number of matches of the entire synthesis thus far
	 * @return	the number of matches and nonmatches of the entire synthesis
	 * thus far
	 * @throws	MolCompareException	if something goes wrong when comparing 
	 * the author's molecule to response molecules
	 */
	private int[] getMatches(SynthStage stage, Molecule authMol,
			List<Integer> matchBoxes, int[] matches)
			throws MolCompareException {
		final String SELF = "Contains.getMatches: ";
		if (stage.hasPrevStage()) {
			final List<SynthStage> prevStages = stage.getAllPrevStages();
			for (final SynthStage prevStage : prevStages) {
				getMatches(prevStage, authMol, matchBoxes, matches);
			} // for each previous stage stageNum
		} // if there are previous stages
		final Molecule[] respFrags = stage.getMoleculeArray();
		debugPrint(SELF + "Found ", respFrags.length, " molecules in stage ",
				stage.getBoxIndex() + 1);
		final int[] newMatches = getMatches(respFrags, authMol);
		if (newMatches[MATCHES] > 0) {
			matchBoxes.add(Integer.valueOf(stage.getBoxIndex()));
		} // if there are matches
		matches[MATCHES] += newMatches[MATCHES];
		matches[NONMATCHES] += newMatches[NONMATCHES];
		return matches;
	} // getMatches(SynthStage, Molecule, List<Integer>, int[])

	/** Finds how many of the molecules in the array match to the author's
	 * molecule.
	 * @param	respFrags	an array of molecules
	 * @param	authMol	substructure or skeleton to look for
	 * @return	an array containing the number of matches and nonmatches
	 * @throws	MolCompareException	if something goes wrong when comparing 
	 * the author's molecule to response molecules
	 */
	private int[] getMatches(Molecule[] respFrags, Molecule authMol) 
			throws MolCompareException {
		return getMatches(respFrags, authMol, !BREAK_AT_1ST_MATCH);
	} // getMatches(Molecule[], Molecule)

	/** Finds how many of the molecules in the array match to the author's
	 * molecule.
	 * @param	respFrags	an array of molecules
	 * @param	authMol	substructure or skeleton to look for
	 * @param	doBreak	whether to stop looking at the first match
	 * @return	an array containing the number of matches and nonmatches
	 * @throws	MolCompareException	if something goes wrong when comparing 
	 * the author's molecule to response molecules
	 */
	private int[] getMatches(Molecule[] respFrags, Molecule authMol,
			boolean doBreak) throws MolCompareException {
		final String SELF = "Contains.getMatches: ";
		int[] matches = new int[2];
		int fragNum = 0;
		for (final Molecule respFrag : respFrags) {
			final boolean isMatch = (method == SKELETON ?
					matchSkeleton(respFrag, authMol)
					: MolCompare.containsSubstruct(respFrag,
							authMol, chgRadIso));
			if (isMatch) {
				matches[MATCHES]++;
				debugPrint("molecule ", ++fragNum,
						" contains reference structure");
				if (doBreak && Utils.among(howMany, ANY, NONE)) break;
			} else { // no match
				matches[NONMATCHES]++;
				debugPrint("molecule ", ++fragNum,
						" doesn't contain reference structure");
				if (doBreak && Utils.among(howMany, NOT_ALL, ALL)) break;
			} // if isMatch
		} // for each response molecule
		debugPrint("getMatches: matches = ", matches[MATCHES],
				", nonmatches = ", matches[NONMATCHES]);
		return matches;
	} // getMatches(Molecule[], Molecule, boolean)

	/** Finds whether the response contains the skeleton.  True if all bonds
	 * between skeletal C atoms in response are also present in the skeleton,
	 * if all skeletal bonds have the same or greater order in the response,
	 * and if no skeletal C atoms in response are attached to nonskeletal C
	 * atoms.
	 * @param	respMol	the response molecule; has already been normalized
	 * @param	authMol	the skeleton; has already been modified for skeleton
	 * search
	 * @return whether the response contains the skeleton
	 */
	private boolean matchSkeleton(Molecule respMol, Molecule authMol) {
		final String SELF = "Contains.matchSkeleton: ";
		try {
			final MolSearch search = new MolSearch();
			final MolSearchOptions searchOpts = 
					new MolSearchOptions(SUBSTRUCTURE);
			if (chgRadIso == EXACT) {
				searchOpts.setChargeMatching(CHARGE_MATCHING_EXACT);
				searchOpts.setIsotopeMatching(ISOTOPE_MATCHING_EXACT);
				searchOpts.setRadicalMatching(RADICAL_MATCHING_EXACT);
			} else if (chgRadIso == IGNORE) {
				searchOpts.setChargeMatching(CHARGE_MATCHING_IGNORE);
				searchOpts.setIsotopeMatching(ISOTOPE_MATCHING_IGNORE);
				searchOpts.setRadicalMatching(RADICAL_MATCHING_IGNORE);
			} else { // DEFAULT
				searchOpts.setChargeMatching(CHARGE_MATCHING_DEFAULT);
				searchOpts.setIsotopeMatching(ISOTOPE_MATCHING_DEFAULT);
				searchOpts.setRadicalMatching(RADICAL_MATCHING_DEFAULT);
			}
			searchOpts.setVagueBondLevel(VAGUE_BOND_OFF);
			searchOpts.setIgnoreCumuleneOrRingCisTransStereo(false); // odd-numbered cumulenes
			searchOpts.setIgnoreAxialStereo(false); // allenes & biaryls
			searchOpts.setIgnoreSynAntiStereo(true); // otherwise random orientations match
			search.setSearchOptions(searchOpts);
			search.setTarget(respMol);
			search.setQuery(authMol);
			final int CARBON = 6;
			while (true) {
				final int[] match = search.findNext();
				if (match == null) break;
				debugPrint(SELF + "Found match ", match);
				boolean violation = false;
				for (final MolBond respBond : respMol.getBondArray()) {
					// is it a C-C bond?
					final MolAtom respAtom1 = respBond.getAtom1();
					final MolAtom respAtom2 = respBond.getAtom2();
					if (respAtom1.getAtno() != CARBON
							|| respAtom2.getAtno() != CARBON) continue;
					// get skeletal status of bond atoms
					final int respAtom1Num = respMol.indexOf(respAtom1);
					final int respAtom2Num = respMol.indexOf(respAtom2);
					final int authAtom1Num = Utils.indexOf(match, respAtom1Num);
					final int authAtom2Num = Utils.indexOf(match, respAtom2Num);
					final boolean respAtom1Skeletal = authAtom1Num >= 0;
					final boolean respAtom2Skeletal = authAtom2Num >= 0;
					// is one skeletal and the other not?
					if (respAtom1Skeletal != respAtom2Skeletal) {
						violation = true;
						debugPrint(SELF + "either C", respAtom1Num + 1,
								" or C", respAtom2Num + 1,
								" is skeletal, and the other is not;"
								+ " violation of skeleton rule.");
					} else if (respAtom1Skeletal) {
						// both are skeletal; is their bond in the skeleton?
						final MolAtom authAtom1 = authMol.getAtom(authAtom1Num);
						final MolAtom authAtom2 = authMol.getAtom(authAtom2Num);
						if (authAtom1.getBondTo(authAtom2) == null) {
							violation = true;
							debugPrint(SELF + "skeletal C atoms ", 
									respAtom1Num + 1, " and ", respAtom2Num + 1,
									" are bonded in the response, but the "
									+ "corresponding atoms ",
									authAtom1Num + 1, " and ",
									authAtom2Num + 1, " in the skeleton "
									+ "are not; violation of skeleton rule.");
						}
					} // if the ligand of the skeletal atom is also skeletal
					if (violation) break;
				} // for each bond in the response
				if (!violation) {
					debugPrint(SELF + "No violation; found skeleton.");
					return true;
				}
			} // while still searching
			debugPrint(SELF + "Found no matches without violations.");
		} catch (SearchException e) {
			System.out.println("SearchException in " + SELF);
			e.printStackTrace();
		}
		return false;
	} // matchSkeleton(Molecule, Molecule)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[SKELETON_SUBSTRUCTURE]; } 
	/** Gets whether to look for substructure or skeleton.
	 * @return	whether to look for substructure or skeleton
	 */
	public int getMethod() 					{ return method; }
	/** Sets whether to look for substructure or skeleton.
	 * @param	method	whether to look for substructure or skeleton
	 */
	public void setMethod(int method) 		{ this.method = method; }
	/** Gets how to manage charges, radicals, isotopes.
	 * @return	how to manage charges, radicals, isotopes
	 */
	public int getChgRadIso() 				{ return chgRadIso; }
	/** Sets how to manage charges, radicals, isotopes.
	 * @param	chgRadIso	how to manage charges, radicals, isotopes
	 */
	public void setChgRadIso(int chgRadIso)	{ this.chgRadIso = chgRadIso; }
	/** Sets the molecule's name.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 	{ this.molName = molName; }

} // Contains
