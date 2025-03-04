package com.epoch.evals.impl.chemEvals.mechEvals;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.sss.search.SearchException;
import chemaxon.struc.Molecule;
import com.epoch.chem.MolString;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.chemEvals.chemEvalConstants.ProdStartConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.mechanisms.Mechanism;
import com.epoch.mechanisms.mechConstants.MechConstants;
import com.epoch.mechanisms.MechSet;
import com.epoch.responses.Response;
import com.epoch.substns.SubstnUtils;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the starting materials or products compare (in various ways) with
 * author-specified materials ...  */
public class MechProdStartIs 
		implements EvalInterface, MechConstants, ProdStartConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** How to compare the sets of student and author compounds. */
	private int combination; // student vs author (S vs A)
	/** Whether to compare sets of starting materials or sets of products. */
	transient private int prodOrStart; // product or starting materials?
	/** Flags for options.  Sets resonance permissiveness, stereochemistry
	 * leniency. */
	private int flags;

	/** Constructor. */
	public MechProdStartIs() { // default values
		combination = IDENTICAL;
		prodOrStart = PRODUCT;
		flags = RESON_LENIENT;
	} // MechProdStartIs()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>combination</code>/<code>prodOrStart</code>/<code>flags</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public MechProdStartIs(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 3) {
			combination = MathUtils.parseInt(splitData[0]);
			prodOrStart = MathUtils.parseInt(splitData[1]);
			flags = MathUtils.parseInt(splitData[2]);
		} else {
			throw new ParameterException("MechProdStartIs ERROR: unknown input data "
					+ "'" + data + "'. ");
		}
	} // MechProdStartIs(String)

	/** Gets a string representation of data (other than a molecule) that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>combination</code>/<code>prodOrStart</code>/<code>flags</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(combination, '/', prodOrStart, '/', flags);
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
		final StringBuilder words = (combination < COMB_ENGL.length / 2
				? Utils.getBuilder("If ", COMB_ENGL[combination],
					" of the specified ", PROD_START_ENGL[prodOrStart],
					COMB_ENGL[combination].contains("all") ? "s are" : "s is",
					" present in the response")
				: Utils.getBuilder("If the ", PROD_START_ENGL[prodOrStart],
					"s in the response ", COMB_ENGL[combination],
					" the specified ", PROD_START_ENGL[prodOrStart], 's'));
		final boolean resonLenient = (flags & RESON_LENIENT) != 0;
		final boolean stereoLenient = (flags & STEREO_LENIENT) != 0;
		if (resonLenient) words.append(
				" (any resonance structures acceptable");
		if (resonLenient && stereoLenient) words.append(", ");
		else if (resonLenient) words.append(')');
		else if (stereoLenient) words.append(" (");
		if (stereoLenient) words.append("stereochemistry ignored)");
		return words.toString();
	} // toEnglish()

	/** Determines whether the response contains the indicated products
	 * or starting materials.
	 * @param	response	a parsed response
	 * @param	materials	String representation of permissible starting
	 * materials or expected products for this mechanism
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified
	 * with color or a message describing an inability to evaluate the
	 * response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String materials) {
		final String SELF = "MechProdStartIs.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		debugPrint(SELF, toEnglish());
		boolean satisfied;		
		final Mechanism mech = (Mechanism) response.parsedResp;
		final String combinationStr = (
				combination == OVERLAP_NULL ? "OVERLAP_NULL" 
				: combination == NOT_OVERLAP_NULL ? "NOT_OVERLAP_NULL"
				: combination == SUPERSET ? "SUPERSET" 
				: combination == NOT_SUPERSET ? "NOT_SUPERSET" 
				: combination == SUBSET ? "SUBSET" 
				: combination == NOT_SUBSET ? "NOT_SUBSET" 
				: combination == IDENTICAL ? "IDENTICAL" 
				: combination == NOT_IDENTICAL ? "NOT_IDENTICAL" 
				: "unknown");
		debugPrint(SELF + "determining whether student materials ", 
				combinationStr, " author materials");
		int molNum = 0;
		try {
			// author starters/products
			final boolean DONT_AROMATIZE = false;
			final Molecule authMol = MolImporter.importMol(materials);
			if (!Utils.isEmpty(response.rGroupMols)) {
				debugPrint(SELF + "substituting R groups into authMol.");
				SubstnUtils.substituteRGroups(authMol, response.rGroupMols);
			} // if there are R groups to replace
			final Molecule[] givenMaterials = 
					MolString.getMolArray(authMol, DONT_AROMATIZE);
			debugPrint(SELF + "\nauthor ", PROD_START_ENGL[prodOrStart], "s:");
			molNum = 0;
			for (final Molecule mol : givenMaterials) {
				debugPrint("\t\t", ++molNum, ":\t", mol);
			} // for each author starter/product
			// response starters/products
			Molecule[] contents = (prodOrStart == PRODUCT
					? mech.getAllResponseProducts(flags)
					: mech.getAllResponseStarters(flags));
			debugPrint(SELF + "response ", PROD_START_ENGL[prodOrStart], "s:");
			molNum = 0;
			for (final Molecule mol : contents) {
				debugPrint("\t\t", ++molNum, ":\t", mol);
			} // for each response starter/product
			if (Utils.among(combination, 
					OVERLAP_NULL, 
					NOT_OVERLAP_NULL, 
					SUPERSET, 
					NOT_SUPERSET)) { 
				final Molecule[] respIntermeds = 
						mech.getAllResponseIntermediates(flags);
				debugPrint(SELF + "adding response intermediates:");
				molNum = 0;
				for (final Molecule mol : respIntermeds) {
					debugPrint("\t\t", ++molNum, ":\t", mol);
				} // for each response intermediate
				contents = MechSet.union(contents, respIntermeds, flags);
				debugPrint(SELF + "after union, response ", 
						PROD_START_ENGL[prodOrStart], "s and intermediates:");
				molNum = 0;
				for (final Molecule mol : contents) {
					debugPrint("\t\t", ++molNum, ":\t", mol);
				} // for each response starter/product and intermediate
			} // if given materials are present in response
			// MechSet methods always return false if contents == null, so for
			// those cases where this should give a true, skip the call and
			// just return true.
			switch (combination) {
				case OVERLAP_NULL: // S ^ A = null
					satisfied = contents == null
							|| MechSet.overlapNull(contents, givenMaterials, 
								flags);
					break;
				case NOT_OVERLAP_NULL: // S ^ A != null
					satisfied = !MechSet.overlapNull(contents, givenMaterials, 
							flags);
					break;
				case SUPERSET: // S >= A
					satisfied = MechSet.superset(contents, givenMaterials, 
							flags);
					break;
				case NOT_SUPERSET: // S !>= A
					satisfied = contents == null
							|| !MechSet.superset(contents, givenMaterials, 
								flags);
					break;
				case SUBSET: // S <= A
					satisfied = contents == null
							|| MechSet.subset(contents, givenMaterials, flags);
					break;
				case NOT_SUBSET: // S !<= A
					satisfied = !MechSet.subset(contents, givenMaterials, 
							flags);
					break;
				case IDENTICAL: // S = A
					satisfied = MechSet.identical(contents, givenMaterials, 
							flags);
					break;
				case NOT_IDENTICAL: // S != A
					satisfied = contents == null 
							|| !MechSet.identical(contents, givenMaterials, 
								flags);
					break;
				default:
					Utils.alwaysPrint(SELF + "invalid combination = ", 
							combination);
					satisfied = false;
					break;
			} // switch(combination)
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "MolFormatException thrown: ", 
					e.getMessage());
			e.printStackTrace();
			satisfied = false;
		} catch (SearchException e) {
			Utils.alwaysPrint(SELF + "SearchException thrown: ", 
					e.getMessage());
			e.printStackTrace();
			satisfied = false;
		} // try
		debugPrint(SELF + "looking at ", PROD_START_ENGL[prodOrStart],
				"s, evaluator is ", (satisfied ? "" : "not "), "satisfied");
		evalResult.isSatisfied = satisfied;
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[MECH_PRODS_STARTERS_IS]; }
	/** Gets how to compare the sets of student and author compounds.
	 * @return	how to compare the sets
	 */
	public int getCombination() 			{ return combination; }
	/** Sets how to compare the sets of student and author compounds.
	 * @param	combo	how to compare the sets
	 */
	public void setCombination(int combo) 	{ combination = combo; }
	/** Gets whether to compare sets of starting materials or sets of products.
	 * @return	what to compare
	 */
	public int getProductOrStart() 			{ return prodOrStart; }
	/** Sets whether to compare sets of starting materials or sets of products.
	 * @param	ps	what to compare
	 */
	public void setProductOrStart(int ps) 	{ prodOrStart = ps; }
	/** Gets flags for this evaluator.
	 * @return	the flags
	 */
	public int getFlags() 					{ return flags; }
	/** Gets flags for this evaluator.
	 * @param	f	flags for this evaluator
	 */
	public void setFlags(int f)				{ flags = f; }
	/** Not used.  Required by interface.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName)	{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 			{ return false; }

} // MechProdStartIs

