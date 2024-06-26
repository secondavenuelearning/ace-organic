package com.epoch.evals.impl.chemEvals.synthEvals;

import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;
import com.epoch.chem.MolString;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.chemEvals.chemEvalConstants.ProdStartConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response; 
import com.epoch.substns.SubstnUtils;
import com.epoch.synthesis.Synthesis;
import com.epoch.synthesis.SynthSet;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the starting materials compare (in various ways) with
 * author-specified materials ... */
public class SynthStart implements EvalInterface, ProdStartConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** How to compare the sets of student and author compounds. */
	private int combination; // student vs author (S vs A)

	/** Constructor. */
	public SynthStart() { // default values
		combination = IDENTICAL;
	} // SynthStart() 

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>combination</code> 
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public SynthStart(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		combination = MathUtils.parseInt(splitData[0]);
	} // SynthStart(String)

	/** Gets a string representation of data (other than a molecule) that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>combination</code> 
	 * @return	the coded data
	 */
	public String getCodedData() {
		return String.valueOf(combination);
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
		return Utils.toString("If ", combination < COMB_ENGL.length / 2
				? Utils.getBuilder(COMB_ENGL[combination], " of the ", 
					PROD_START_ENGL[START],
					COMB_ENGL[combination].contains("all") ? "s are" : "s is",
					" present in the response")
				: Utils.getBuilder("the ", PROD_START_ENGL[START],
					"s in the response ", COMB_ENGL[combination],
					" the specified ", PROD_START_ENGL[START], 's'));
	} // toEnglish()

	/** Determines whether the response starting materials satisfy the indicated
	 * relationship with the indicated starting materials.  
	 * @param	response	a parsed response
	 * @param	materials	String representation of starting
	 * materials of this synthesis
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing an 
	 * inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String materials) { 
		final String SELF = "SynthStart.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		debugPrint(SELF + "student response string:\n", response.unmodified);
		debugPrint(SELF + "author materials string:\n", materials);
		debugPrint(SELF + "combination = ", combination);  
		boolean satisfied;
		final Synthesis respSynth = (Synthesis) response.parsedResp;
		try {
			final boolean AROMATIZE = true;
			final Molecule authMol = MolImporter.importMol(materials);
			if (!Utils.isEmpty(response.rGroupMols)) {
				SubstnUtils.substituteRGroups(authMol, response.rGroupMols);
			} // if there are R groups to replace
			final Molecule[] givenMaterials = 
					MolString.getMolArray(authMol, AROMATIZE);
			debugPrint(SELF + "author-given ", PROD_START_ENGL[START], "s:");
			int smNum = 0;
			for (final Molecule givenSM : givenMaterials) {
				debugPrint("     Compound ", ++smNum, ": ", givenSM);
			} // for each author starting material
			final Molecule[] contents = respSynth.getAllResponseStarters();
			if (contents == null) { 
				debugPrint(SELF + "student-given ", PROD_START_ENGL[START], 
						"s are null!");
			} else {
				debugPrint(SELF + "student-given ", PROD_START_ENGL[START], "s:");
				smNum = 0;
				for (final Molecule respSM : contents) {
					debugPrint("     Compound ", ++smNum, ": ", respSM);
				} // for each author starting material
			}
			// SynthSet methods always return false if contents == null, so for those
			// cases where this should give a true, skip the call and just return
			// true
			switch (combination) {
				case OVERLAP_NULL: // S ^ A = null
					satisfied = contents == null
							|| SynthSet.overlapNull(contents, givenMaterials);
					break;
				case NOT_OVERLAP_NULL: // S ^ A != null
					satisfied = !SynthSet.overlapNull(contents, givenMaterials);
					break;
				case SUPERSET: // S >= A
					satisfied = SynthSet.superset(contents, givenMaterials); 
					break;
				case NOT_SUPERSET: // S !>= A
					satisfied = contents == null
							|| !SynthSet.superset(contents, givenMaterials);
					break;
				case SUBSET: // S <= A
					satisfied = contents == null
							|| SynthSet.subset(contents, givenMaterials);
					break;
				case NOT_SUBSET: // S !<= A
					satisfied = !SynthSet.subset(contents, givenMaterials); 
					break;
				case IDENTICAL: // S = A
					satisfied = SynthSet.identical(contents, givenMaterials);
					break;
				case NOT_IDENTICAL: // S != A			 
					satisfied = contents == null
							|| !SynthSet.identical(contents, givenMaterials);
					break;
				default:
					Utils.alwaysPrint(SELF + "invalid combination = ", combination);
					satisfied = false;
					break;
			} // switch(combination)
		} catch (Exception e) {
			Utils.alwaysPrint(SELF + "exception thrown: ", e.getMessage());
			e.printStackTrace();
			satisfied = false;
		} // catch exception in switch
		debugPrint(SELF + "looking at ", PROD_START_ENGL[START], 
				"s, evaluator is ", (satisfied? "" : "not "), "satisfied");  
		evalResult.isSatisfied = satisfied;
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[SYNTH_STARTERS]; } 
	/** Gets how to compare the sets of student and author starting materials. 
	 * @return	how to compare the sets
	 */
	public int getCombination() 			{ return combination; } 
	/** Sets how to compare the sets of student and author starting materials. 
	 * @param	comb	how to compare the sets
	 */
	public void setCombination(int comb) 	{ combination = comb; } 
	/** Not used.  Required by interface. 
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 	{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 			{ return false; }

} // SynthStart

