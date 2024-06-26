package com.epoch.evals.impl.chemEvals;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.sss.SearchConstants;
import chemaxon.sss.search.MolSearch;
import chemaxon.sss.search.MolSearchOptions;
import chemaxon.sss.search.SearchException;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import chemaxon.struc.Molecule;
import com.epoch.chem.chemConstants.ChemConstants;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.FnalGroupDef;
import com.epoch.chem.Normalize;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the number of compounds that {have, don't have} {=, &lt;, &gt;} 
 * <i>m</i> occurrences of a functional group
 * {is, is not} {=, &lt;, &gt;} <i>n</i> ... */
public final class FnalGroup extends CompareNumsOfNums 
		implements ChemConstants, EvalInterface, SearchConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** ID number of the functional group, given by its position in the text
	 * file that lists all of them. */
	private int groupId;
	/** Number of functional groups to compare against. */
	private int numGroups;
	/** How to compare the number of functional groups. */
	private int groupOper;
	/** Instantiation of functional group. */
	transient private FnalGroupDef group;
	
	/** Constructor. */
	public FnalGroup() {
		groupId = 1;
		groupOper = EQUALS;
		group = new FnalGroupDef();
	} // FnalGroup()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>groupId</code>/<code>groupOper</code>/<code>numGroups</code>/<code>countEach</code>/<code>molsOper</code>/<code>numMols</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public FnalGroup(String data) throws ParameterException {
		try {
			final String[] splitData = data.split("/");
			groupId = MathUtils.parseInt(splitData[0]);
			groupOper = Utils.indexOf(SYMBOLS, splitData[1]);
			numGroups = MathUtils.parseInt(splitData[2]);
			if (splitData.length >= 6) {
				countEach = Utils.isPositive(splitData[3]); // inherited from CompareNumsOfNums
				molsOper = Utils.indexOf(SYMBOLS, splitData[4]); // inherited from CompareNumsOfNums
				numMols = MathUtils.parseInt(splitData[5]); // inherited from CompareNumsOfNums
			}
			setGroup();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ParameterException("Error thrown when trying to parse "
					+ "functional group data: " + data);
		} // try
	} // FnalGroup(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>groupId</code>/<code>groupOper</code>/<code>numGroups</code>/<code>countEach</code>/<code>molsOper</code>/<code>numMols</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(groupId, '/', SYMBOLS[groupOper], '/', numGroups, 
				countEach ? "/Y/" : "/N/", SYMBOLS[molsOper], '/', numMols);
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
		return (!forPermissibleSM ? toEnglish()
				: Utils.toString(groupOper != NOT_EQUALS ? "do not " : "",
				"contain the functional group ", getGroupDisplayName()));
	} // toEnglish(boolean)

	/** Gets an English-language description of this evaluator.
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish() {
		return Utils.toString("If",
				countEach ? getNumCompoundsEnglish() 
					: " the response contains ",
				getNumEnglish(groupOper, numGroups, Utils.toString(
					getGroupDisplayName(), " functional group")));
	} // toEnglish()

	/** Determines whether the response contains the indicated functional group.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing an
	 * inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "FnalGroup.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		debugPrint(SELF, toEnglish());
		if (response.normalized == null) {
			response.normalized = 
					Normalize.normalize(response.moleculeObj);
		} // if there is no normalized response Molecule
		try {
			final Molecule groupMol = MolImporter.importMol(group.definition);
			final MolSearchOptions searchOpts = 
					new MolSearchOptions(SUBSTRUCTURE);
			final Molecule wholeMol = 
					ChemUtils.explicitizeH(response.normalized);
			debugPrint(SELF + "after adding H atoms, "
					+ "target (response) is:\n", wholeMol);
			debugPrint(SELF, "query (fnal group defn) is ", 
					group.definition, "\nafter importing:\n", groupMol);
			searchOpts.setStereoSearchType(STEREO_IGNORE);
			searchOpts.setChargeMatching(CHARGE_MATCHING_EXACT);
			searchOpts.setIsotopeMatching(ISOTOPE_MATCHING_EXACT);
			searchOpts.setRadicalMatching(RADICAL_MATCHING_EXACT);
			searchOpts.setVagueBondLevel(VAGUE_BOND_OFF);
			final MolSearch search = new MolSearch();
			search.setSearchOptions(searchOpts);
			search.setQuery(groupMol);
			int numRgMatches = 0;
			if (!Utils.isEmpty(response.rGroupMols)) {
				for (final Molecule rgMolOrig : response.rGroupMols) {
					Molecule rgMol = rgMolOrig.clone();
					final MolAtom pseudoAtom = new MolAtom(MolAtom.PSEUDO);
					pseudoAtom.setAliasstr(ATTACH_PT);
					final MolAtom attachAtom = 
							ChemUtils.getAttachmentPoint(rgMol);
					rgMol.add(pseudoAtom);
					rgMol.add(new MolBond(attachAtom, pseudoAtom));
					rgMol = Normalize.normalize(rgMol);
					search.setTarget(rgMol);
					final int[][] rgMatches = search.findAll();
					final int numOneRgMatches = Utils.getLength(rgMatches);
					debugPrint(SELF + "Found ", numOneRgMatches, 
							" in R group ", rgMol);
					numRgMatches += numOneRgMatches;
				} // for each R group
				debugPrint(SELF + "number of functional groups in "
						+ "all R groups is ", numRgMatches);
			} // if we have R groups
			final Molecule[] mols = (countEach ? wholeMol.convertToFrags()
					: new Molecule[] {wholeMol});
			int molsSatisfyingCt = 0;
			for (final Molecule mol : mols) {
				search.setTarget(mol);
				final int[][] allMatches = search.findAll();
				final int numMatches = 
						Utils.getLength(allMatches) - numRgMatches;
				setOper(groupOper);
				final boolean match = compare(numMatches, numGroups);
				if (match) molsSatisfyingCt++;
				debugPrint(SELF + "for mol ", mol, ", number of groups = ", 
						numMatches, ", compared to = ", numGroups, 
						", match = ", match);
			} // for each molecule, or once for all molecules
			setOper(molsOper);
			evalResult.isSatisfied = compare(molsSatisfyingCt, numMols);
			debugPrint(SELF + "molsSatisfyingCt = ", molsSatisfyingCt,
					", numMols = ", numMols, ", isSatisfied = ",
					evalResult.isSatisfied);
		} catch (MolFormatException e1) {
			Utils.alwaysPrint("MolFormatException in FnalGroup.java for:\n",
					group.definition);
			e1.printStackTrace();
			evalResult.verificationFailureString =
					"Error when importing functional group definition "
					+ group.definition;
		} catch (SearchException e2) {
			Utils.alwaysPrint("Error in FnalGroup.isResponseMatching search ");
			e2.printStackTrace();
			evalResult.verificationFailureString =
					"Error when searching for " + group.definition
					+ " in " + response.unmodified;
		}
		return evalResult;
	} // isResponseMatching(Response, String)
	
	/* *************** Get-set methods *****************/

	/** Initializes the functional group. 
	 * @throws	ParameterException	if the group ID is no good
	 */
	public void setGroup() throws ParameterException {
		try {
			group = FnalGroupDef.getFnalGroupDef(groupId);
		} catch (DBException e) {
			throw new ParameterException(e.getMessage());
		}
	} // setGroup()

	/** Sets the functional group ID number, initializes the functional group. 
	 * @param	grpId	ID number of the chosen functional group
	 * @throws	ParameterException	if the group ID is no good
	 */
	public void setGroup(int grpId) throws ParameterException {
		groupId = grpId;
		setGroup();
	} // setGroup()

	/** Gets the name of the functional group of this evaluator.
	 * @return name of the functional group
	 */
	public String getGroupDisplayName() 	{ return group.getDisplayName(); }
	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[FUNCTIONAL_GROUP]; } 
	/** Gets the ID number of the chosen functional group.
	 * @return	ID number of the chosen functional group
	 */
	public int getGroupId() 				{ return groupId; }
	/** Sets the ID number of the chosen functional group.
	 * @param	grpId	ID number of the chosen functional group
	 */
	public void setGroupId(int grpId)		{ groupId = grpId; }
	/** Gets the value of the number to compare.
	 * @return	value of the number to compare
	 */
	public int getNumGroups() 				{ return numGroups; }
	/** Sets the value of the number to compare.
	 * @param	num	value of the number to compare
	 */
	public void setNumGroups(int num) 		{ numGroups = num; }
	/** Gets the value of the functional-group operator.
	 * @return	value of the functional-group operator
	 */
	public int getGroupOper() 				{ return groupOper; } 
	/** Sets the value of the functional-group operator.
	 * @param	op	value to which to set the functional-group operator
	 */
	public void setGroupOper(int op)	 	{ groupOper = op; } 

} // FnalGroup
