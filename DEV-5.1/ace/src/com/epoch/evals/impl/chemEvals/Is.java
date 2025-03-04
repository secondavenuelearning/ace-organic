package com.epoch.evals.impl.chemEvals;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolCompare;
import com.epoch.chem.MolCompareException;
import com.epoch.chem.Normalize;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.Counter;
import com.epoch.evals.impl.chemEvals.chemEvalConstants.MolCompareConstants;
import com.epoch.evals.impl.implConstants.CountConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.lewis.LewisMolecule;
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

/** If {no, the only, exactly one, any, not all, all} compounds in the response
 * are identical, or are identical or enantiomeric, or are identical without
 * normalization, or are identical or resonance structures, or have the same
 * &sigma;-bond network ...  */
public class Is extends Counter 
		implements EvalInterface, CountConstants, MolCompareConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Flags to modify the search for either enantiomer, eschew normalization,
	 * treat resonance structures as equivalent, look at sigma-bond network only,
	 * allow author's unspecified isotopes to be any isotopes in response. */
	protected int flags;
	/** Common phrase in debugging output. */
	final static String RESPONSEFRAGMENT = " response fragment ";
	/** Common phrase in debugging output. */
	final static String ANDAUTHORREF = " and author reference ";
	/** Common phrase in debugging output. */
	final static String SIGMANETWORKSOF = "sigma networks of";
	/** Common phrase in debugging output. */
	final static String OR_ENANT = " or its enantiomer";
	
	/** Constructor. */
	public Is() { // default values
		howMany = ONLY;
		flags = 0;
	} // Is()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>howMany</code>/<code>flags</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public Is(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 2) {
			howMany = MathUtils.parseInt(splitData[0]);
			flags = MathUtils.parseInt(splitData[1]);
		} else {
			throw new ParameterException("Is ERROR: unknown input data "
				+ "'" + data + "', which has " + splitData.length + " tokens");
		}
	} // Is(String)

	/** Gets a string representation of data (other than a molecule) that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>howMany</code>/<code>flags</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(howMany, '/', flags);
	} // getCodedData()

	/** Gets an English-language description of this evaluator.
	 * @param	qDataTexts	the text of question data of this question, if any;
	 * not used, but required by interface
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts, boolean forPermissibleSM) {
		return toEnglish(forPermissibleSM);
	} // toEnglish(String[], boolean)

	/** Gets an English-language description of this evaluator.
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(boolean forPermissibleSM) {
		if (!forPermissibleSM) return toEnglish();
		return Utils.toString("are (or are among) ", molName);
	} // toEnglish(boolean)

	/** Gets an English-language description of this evaluator.
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish() {
		final StringBuilder words = Utils.getBuilder("If", 
				HOWMANY_ENGL[howMany - 1], "compound in the response ",
				!checkSigmaNetwork() ? "is "
					: "has the same &sigma;-bond network as ");
		if (eitherEnantiomer()) words.append("either enantiomer of ");
		final boolean noNormalization = noNormalization();
		final boolean isotopeLenient = isotopeLenient();
		if (noNormalization || isotopeLenient) {
			words.append('(');
			if (noNormalization) words.append("without normalization");
			if (noNormalization && isotopeLenient) words.append(", ");
			if (isotopeLenient) words.append("isotope-leniently");
			words.append(") ");
		} // if noNormalization or isotopeLenient
		if (resonancePermissive()) {
			words.append("or is a resonance structure of ");
		} // if resonancePermissive
		words.append(molName != null ? molName : "the indicated compound");
		return words.toString();
	} // toEnglish()

	/** Determines whether the response is the same as the indicated compound
	 * (or its enantiomer, maybe without normalization, maybe resonance-tolerant,
	 * maybe comparing only &sigma;-bond networks).
	 * @param	response	a parsed response
	 * @param	authStruct	String representation of a molecule that the
	 * evaluator compares to the response
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified with
	 * color or a message describing an inability to evaluate the response
	 * because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authStruct) {
		final String SELF = "Is.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		final String respOrig = response.unmodified;
		debugPrint(SELF + "response MRV is ", respOrig,
				", and authStruct is ", authStruct);
		Molecule authMol = null;
		try {
			authMol = (!Utils.isEmpty(response.rGroupMols)
					? SubstnUtils.substituteRGroups(authStruct,
						response.rGroupMols)
					: MolImporter.importMol(authStruct));
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "MolFormatException for: ", authStruct);
			e.printStackTrace();
			evalResult.verificationFailureString = e.getMessage();
			return evalResult;
		} // try
		final boolean noNormalization = noNormalization();
		final boolean isLewis =
				response.parsedResp instanceof LewisMolecule;
		int[] matches = new int[2];
		final List<Integer> matchStages = new ArrayList<Integer>();
		final Mechanism mechanism = response.getMechanism();
		final Synthesis synthesis = response.getSynthesis();
		final boolean isMechanism = mechanism != null;
		final boolean isSynthesis = synthesis != null;
		if (!noNormalization && !isLewis) {
			Normalize.normalizeNoClone(authMol, isMechanism);
			debugPrint(SELF + "after normalization, authStruct is ", authMol);
		} // if should normalize
		try {
			if (isMechanism) {
				matches = getMatches(mechanism, authMol, matchStages);
			} else if (isSynthesis) {
				matches = getMatches(synthesis.getTargetStage(),
						authMol, matchStages, matches);
			} else try {
				Molecule respMol = null;
				// need to clone response Molecule before fragmenting
				if (noNormalization || isLewis) {
					respMol = response.moleculeObj.clone();
				} else { // use normalized molecule
					if (response.normalized == null) {
						response.normalized =
								Normalize.normalize(response.moleculeObj);
					} // if haven't already normalized
					respMol = response.normalized.clone();
				} // if no-normalization flag is on, or if is Lewis structure
				final Molecule[] respFrags = respMol.convertToFrags();
				debugPrint(SELF + "Found ", respFrags.length, 
						" molecule(s) in response");
				if (Utils.isEmpty(respFrags)) { // something is very wrong
					debugPrint(SELF + "respMol is: ", respMol);
				} // if no response fragments
				matches = getMatches(respFrags, authMol, isLewis,
						BREAK_AT_1ST_MATCH);
			} catch (Exception e) {
				Utils.alwaysPrint("Exception in " + SELF + " for:\n", 
						response.unmodified);
				e.printStackTrace();
				evalResult.verificationFailureString = e.getMessage();
				return evalResult;
			} // try
		} catch (Exception ex) {
			Utils.alwaysPrint("Caught exception:\n", ex.getMessage());
			evalResult.verificationFailureString = ex.getMessage();
		}
		evalResult.isSatisfied = getIsSatisfied(matches);
		debugPrint(SELF + "match = ", matches[MATCHES], ", notMatch = ", 
				matches[NONMATCHES], ", isSatisfied = ", 
				evalResult.isSatisfied);
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
	 * @param	mechanism	a mechanism response
	 * @param	authMol	substructure or skeleton to look for
	 * @param	matchStages	list of stages that have matches
	 * @return	the number of matches and nonmatches of the entire mechanism 
	 * @throws	MolCompareException	if something goes wrong when comparing 
	 * the author's molecule to response molecules
	 */
	private int[] getMatches(Mechanism mechanism, Molecule authMol,
			List<Integer> matchStages)
			throws MolCompareException {
		final String SELF = "Is.getMatches: ";
		final boolean noNormalization = noNormalization();
		final int[] matches = new int[2];
		for (int stageIndex = 0; stageIndex < mechanism.getNumStages(); 
				stageIndex++) {
			final MechStage stage = mechanism.getStage(stageIndex);
			final Molecule[] respFrags = (noNormalization
					? stage.getMoleculeArray()
					: stage.getNormalizedMolArray());
			debugPrint(SELF + "Found ", respFrags.length, noNormalization 
						? "" : " normalized", " molecules in stage ", 
					stageIndex + 1);
			final int[] newMatches = getMatches(respFrags, authMol);
			if (newMatches[MATCHES] > 0) {
				matchStages.add((Integer) stageIndex);
			} // if there are matches
			matches[MATCHES] += newMatches[MATCHES];
			matches[NONMATCHES] += newMatches[NONMATCHES];
		} // for each stage
		return matches;
	} // getMatches(Mechanism, Molecule, List<Integer>)

	/** Gets the result for a particular stage in a multistep synthesis response.
	 * Called recursively!
	 * @param	stage	a stage in a synthesis
	 * @param	authMol	substructure or skeleton to look for
	 * @param	matchBoxes	list of boxes of stages that have matches
	 * @param	matches	the number of matches of the entire synthesis thus far
	 * @return	the number of matches and nonmatches of the entire synthesis thus 
	 * far
	 * @throws	MolCompareException	if something goes wrong when comparing 
	 * the author's molecule to response molecules
	 */
	private int[] getMatches(SynthStage stage, Molecule authMol,
			List<Integer> matchBoxes, int[] matches)
			throws MolCompareException {
		final String SELF = "Is.getMatches: ";
		if (stage.hasPrevStage()) {
			for (final SynthStage prevStage : stage.getAllPrevStages()) {
				getMatches(prevStage, authMol, matchBoxes, matches); // recursion
			} // for each previous stage
		} // if there are previous stages
		final boolean noNormalization = noNormalization();
		final Molecule[] respFrags = (noNormalization
				? stage.getMoleculeArray() : stage.getNormalizedMols());
		debugPrint(SELF + "Found ", respFrags.length, noNormalization 
					? "" : " normalized", " molecules in stage with box index ", 
				stage.getBoxIndex() + 1);
		final int[] newMatches = getMatches(respFrags, authMol);
		if (newMatches[MATCHES] > 0) {
			matchBoxes.add((Integer) stage.getBoxIndex());
		} // if there are more matches
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
		return getMatches(respFrags, authMol, !IS_LEWIS, !BREAK_AT_1ST_MATCH);
	} // getMatches(Molecule[], Molecule)
					
	/** Finds how many of the molecules in the array match to the author's
	 * molecule.
	 * @param	respFrags	an array of molecules
	 * @param	authMol	substructure or skeleton to look for
	 * @param	isLewis	whether the response is a Lewis structure
	 * @param	doBreak	whether to stop looking at the first match
	 * @return	an array containing the number of matches and nonmatches
	 * @throws	MolCompareException	if something goes wrong when comparing 
	 * the author's molecule to response molecules
	 */
	private int[] getMatches(Molecule[] respFrags, Molecule authMol,
			boolean isLewis, boolean doBreak)
			throws MolCompareException {
		final int[] matches = new int[2];
		int fragNum = 0;
		for (final Molecule respFrag : respFrags) {
			debugPrint("Is.getMatches: molecule ", ++fragNum, ": ", respFrag);
			if (is(respFrag, authMol, isLewis)) {
				matches[MATCHES]++;
				debugPrint("molecule ", fragNum, " matches reference structure");
				if (doBreak && Utils.among(howMany, ANY, NONE)) break;
			} else {
				matches[NONMATCHES]++;
				debugPrint("molecule ", fragNum, 
						" doesn't match reference structure");
				if (doBreak && Utils.among(howMany, NOT_ALL, ALL)) break;
			}
		} // for each response molecule fragNum
		debugPrint("getMatches: matches = ", matches[MATCHES],
				", nonmatches = ", matches[NONMATCHES]);
		return matches;
	} // getMatches(Molecule[], Molecule, boolean, boolean)
	
	/** Finds whether the response matches the author's molecule.
	 * @param	responseFrag	the response molecule
	 * @param	authMol	the author's molecule
	 * @param	isLewis	whether the response is a Lewis structure
	 * @return whether the response matches the author's molecule
	 * @throws	MolCompareException	if something goes wrong when comparing 
	 * the author's molecule to response molecules
	 */
	private boolean is(Molecule responseFrag, Molecule authMol, boolean isLewis)
			throws MolCompareException {
		final String SELF = "Is.is: ";
		final boolean eitherEnantiomer = eitherEnantiomer();
		final boolean isotopeLenient = isotopeLenient();
		debugPrint(SELF + "comparing ", RESPONSEFRAGMENT, responseFrag, 
				ANDAUTHORREF, authMol, "; eitherEnantiomer = ", eitherEnantiomer, 
				", isotopeLenient = ", isotopeLenient, ", isLewis = ", isLewis);
		boolean value = false;
		if (checkSigmaNetwork()) {
			value = MolCompare.matchSigmaNetwork(responseFrag,
					authMol, isLewis, eitherEnantiomer, isotopeLenient);
			debugPrint(SELF + RESPONSEFRAGMENT, responseFrag, ANDAUTHORREF, 
					authMol, eitherEnantiomer ? OR_ENANT : "", value 
					? "do" : "do not", "have matching sigma-bond networks");
		} else if (resonancePermissive()) {
			value = MolCompare.areResonanceOrIdentical(responseFrag,
					authMol, isLewis, eitherEnantiomer, isotopeLenient);
			debugPrint(SELF + RESPONSEFRAGMENT, responseFrag, ANDAUTHORREF, 
					authMol, eitherEnantiomer ? OR_ENANT : "", " are ", value 
					? "" : "not ", "resonance structures or identical");
		} else if (isLewis) {
			// no stereo to compare, but compare explicit H atoms, too, after
			// explicitizing H atoms in author's structure
			final Molecule authMolH = ChemUtils.explicitizeH(authMol);
 			value = MolCompare.matchPerfect(responseFrag, authMolH);
			debugPrint(SELF + RESPONSEFRAGMENT, responseFrag, 
					ANDAUTHORREF, authMolH, (value? " are" : " aren't"), 
					" identical, including explicit H atoms");
		} else {
			value = MolCompare.matchExact(responseFrag, 
					authMol, eitherEnantiomer, isotopeLenient);
			debugPrint(SELF + RESPONSEFRAGMENT, responseFrag, 
					ANDAUTHORREF, authMol, eitherEnantiomer ? OR_ENANT : "",
					" are ", value ? "" : "not ", "identical");
		} // if flags
		return value;
	} // is(String, String)

	/** Gets if the "&sigma;-network" flag is set.
	 * @return	true if the "&sigma;-network" flag is set
	 */
	private boolean checkSigmaNetwork() {
		return (flags & SIGMA_NETWORK) != 0;
	} // checkSigmaNetwork()

	/** Gets if the "either enantiomer" flag is set.
	 * @return	true if the "either enantiomer" flag is set
	 */
	private boolean eitherEnantiomer() {
		return (flags & EITHER_ENANTIOMER) != 0;
	} // eitherEnantiomer()

	/** Gets if the "no normalization" flag is set.
	 * @return	true if the "no normalization" flag is set
	 */
	private boolean noNormalization() {
		return (flags & NO_NORMALIZATION) != 0;
	} // noNormalization()

	/** Gets if the "any resonance structure" flag is set.
	 * @return	true if the "any resonance structure" flag is set
	 */
	private boolean resonancePermissive() {
		return (flags & RESONANCE_PERMISSIVE) != 0;
	} // resonancePermissive()

	/** Gets if the "isotope-lenient" flag is set.
	 * @return	true if the "isotope-lenient" flag is set
	 */
	private boolean isotopeLenient() {
		return (flags & ISOTOPE_LENIENT) != 0;
	} // isotopeLenient()

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[IS_OR_HAS_SIGMA_NETWORK]; } 
	/** Gets the flags for this search.
	 * @return	flags for this search
	 */
	public int getFlags() 					{ return flags; }
	/** Sets the flags for this search.
	 * @param	flags	flags for this search
	 */
	public void setFlags(int flags) 		{ this.flags = flags; }
	/** Sets the molecule's name.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 	{ this.molName = molName; }

} // Is

