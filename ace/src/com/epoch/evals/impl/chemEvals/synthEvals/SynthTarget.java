package com.epoch.evals.impl.chemEvals.synthEvals;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;
import com.epoch.chem.MolCompare;
import com.epoch.chem.MolCompareException;
import com.epoch.chem.Normalize;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.substns.SubstnUtils;
import com.epoch.synthesis.Synthesis;
import com.epoch.synthesis.SynthStage;
import com.epoch.utils.Utils;

/**	If the target in the synthesis is the expected one, maybe enantiomeric ...  */ 
public class SynthTarget implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether the evaluator is satisfied by a match. */
	private boolean	isPositive; 
	/** Whether to check both enantiomers of the author's structure. */
	private boolean	checkEnantiomer;
	/** Name of the Lewis structure molecule. */
	transient private String molName = "";
	
	/** Constructor. */
	public SynthTarget() {
		isPositive = true;
		checkEnantiomer = true;
	} // SynthTarget()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>checkEnantiomer</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public SynthTarget(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 2) {
			isPositive = Utils.isPositive(splitData[0]);
			checkEnantiomer = Utils.isPositive(splitData[1]);
		} else {
			throw new ParameterException("SynthTarget ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // SynthTarget(String)

	/** Gets a string representation of data (other than a molecule) that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>isPositive</code>/<code>checkEnantiomer</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive ? "Y/" : "N/",
				checkEnantiomer ? 'Y' : 'N');
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
				"If the target of the synthesis is "); 
		if (!isPositive) words.append("not ");
		words.append(molName);
		if (checkEnantiomer) words.append(" or its enantiomer"); 
		return words.toString();
	} // toEnglish()  

	/** Determines whether the response describes the synthesis of the indicated
	 * target.  
	 * @param	response	a parsed response
	 * @param	authTarget	String representation of the target of the synthesis
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authTarget) {
		final String SELF = "SynthTarget.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		if (authTarget == null) {
			Utils.alwaysPrint(SELF + "author target is null.");
			evalResult.verificationFailureString = 
					"The author's target structure is null. "
					+ "Please report this data error to the webmaster.";
			return evalResult;
		} // if authTarget is null
		boolean validTarget = false;
		try {
			final Molecule authMol = (!Utils.isEmpty(response.rGroupMols)
					? SubstnUtils.substituteRGroups(authTarget,
						response.rGroupMols)
					: MolImporter.importMol(authTarget));
			debugPrint(SELF + "authTarget: ", authMol);
			Normalize.normalizeNoClone(authMol);
			final Synthesis synthesis = (Synthesis) response.parsedResp;
			final int targetStageIndex = synthesis.getTargetStageIndex();
			final SynthStage targetStage = synthesis.getStage(targetStageIndex);
			final Molecule respTarget = targetStage.getMolecule(0).clone(); 
				// already checked there's only one molecule in target stage
			Normalize.normalizeNoClone(respTarget);
			validTarget = MolCompare.matchExact(respTarget, authMol, 
					checkEnantiomer);
		} catch (NullPointerException e) {
			Utils.alwaysPrint(SELF + "NullPointerException for:\n",
					authTarget, e.getMessage());
			evalResult.verificationFailureString = 
					"A NullPointerException was thrown when ACE tried to "
					+ "interpret your mechanism.  "
					+ "Please report this software error to the webmaster: "
					+ e.getMessage();
			e.printStackTrace();
			return evalResult;
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "MolFormatException for:\n",
					authTarget, e.getMessage());
			e.printStackTrace();
			evalResult.verificationFailureString = 
					"ACE can't obtain the author's target structure. "
					+ "Please report this software error to the webmaster: "
					+ e.getMessage();
			return evalResult;
		} catch (MolCompareException e) {
			Utils.alwaysPrint(SELF + "MolCompareException when "
					+ "trying to match response target to reference.",
					e.getMessage());
			e.printStackTrace();
			evalResult.verificationFailureString = 
					"ACE can't match your target to the author's target. "
					+ "Please report this software error to the webmaster: "
					+ e.getMessage();
			return evalResult;
		}
		evalResult.isSatisfied = isPositive == validTarget;
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 					{ return EVAL_CODES[SYNTH_TARGET]; } 
	/** Gets whether the evaluator is satisfied by a match or a mismatch.
	 * @return	true if the evaluator is satisfied by a match
	 */
	public boolean getIsPositive() 					{ return isPositive; } 
	/** Sets whether the evaluator is satisfied by a match or a mismatch.
	 * @param	isPositive	true if the evaluator is satisfied by a match
	 */
	public void setIsPositive(boolean isPositive) 	{ this.isPositive = isPositive; } 
	/** Gets whether to check both enantiomers of the author's structure. 
	 * @return	true if should check both enantiomers
	 */
	public boolean getCheckEnantiomer() 			{ return checkEnantiomer; } 
	/** Gets whether to check both enantiomers of the author's structure. 
	 * @param	ce	whether should check both enantiomers
	 */
	public void setCheckEnantiomer(boolean ce) 		{ this.checkEnantiomer = ce; } 
	/** Sets the molecule's name.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 			{ this.molName = molName; }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 					{ return false; }

} // SynthTarget

