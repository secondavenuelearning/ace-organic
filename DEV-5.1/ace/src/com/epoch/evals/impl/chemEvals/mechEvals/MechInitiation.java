package com.epoch.evals.impl.chemEvals.mechEvals;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.sss.search.SearchException;
import chemaxon.struc.Molecule;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.evalConstants.OneEvalConstants;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.mechanisms.Mechanism;
import com.epoch.mechanisms.mechConstants.MechConstants;
import com.epoch.mechanisms.MechError;
import com.epoch.mechanisms.MechFormatException;
import com.epoch.responses.Response;
import com.epoch.substns.SubstnUtils;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;

/** If the initiation part {is, is not} properly drawn (is present, initiator
 * does not appear in propagation part of the mechanism) ... */
public class MechInitiation 
		implements EvalInterface, MechConstants, OneEvalConstants {
	
	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether a properly drawn initiation satisfies this evaluator. */
	private boolean	isPositive;
	/** Flags for options.  Sets resonance permissiveness, stereochemistry
	 * leniency. */
	private int flags;

	/** Constructor. */
	public MechInitiation() {
		isPositive = true;
		flags = RESON_LENIENT;
	} // MechInitiation()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>flags</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public MechInitiation(String data) throws ParameterException {
		debugPrint("MechInitiation: data = ", data);
		final String[] splitData = data.split("/");
		if (splitData.length >= 2) {
			isPositive = Utils.isPositive(splitData[0]);
			flags = MathUtils.parseInt(splitData[1]);
		} else {
			throw new ParameterException("MechInitiation ERROR: unknown input data " 
					+ "'" + data + "'. ");
		}
	} // MechInitiation(String)

	/** Gets a string representation of data (other than a molecule) that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>isPositive</code>/<code>flags</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive ? "Y/" : "N/", flags);
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
		final boolean resonLenient = (flags & RESON_LENIENT) != 0;
		final boolean stereoLenient = (flags & STEREO_LENIENT) != 0;
		final StringBuilder words = Utils.getBuilder(
				"If the initiation part of the mechanism is ",
				isPositive ? "present, and" : "absent, or",
				" the initiator is ");
		if (isPositive) words.append("not ");
		words.append("used in the propagation part of the mechanism");
		if (resonLenient) words.append(" (any resonance structures "
				+ "of initiator acceptable");
		if (resonLenient && stereoLenient) words.append(", ");
		else if (resonLenient) words.append(')');
		else if (stereoLenient) words.append(" (");
		if (stereoLenient) words.append("stereochemistry ignored)");
		return words.toString();
	} // toEnglish() 

	/** Determines whether the initiation part is drawn correctly.
	 * @param	response	a parsed response
	 * @param	initiatorXML	String representation of the initiator of this
	 * this mechanism
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified 
	 * with color or automatically generated feedback or a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String initiatorXML)  {
		final String SELF = "MechInitiation.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		final Mechanism mechanism = (Mechanism) response.parsedResp;
		try {
			final Molecule initiator = MolImporter.importMol(initiatorXML);
			if (!Utils.isEmpty(response.rGroupMols)) {
				SubstnUtils.substituteRGroups(initiator, response.rGroupMols);
			} // if there are generic R groups to substitute
		 	mechanism.initiationOK(initiator, flags); // MechError if not
			evalResult.isSatisfied = isPositive;
		} catch (MechError e) {
			evalResult.isSatisfied = !isPositive;
			if (!isPositive) {
				evalResult.modifiedResponse = e.getMessage();
				debugPrint(SELF + "Original response:\n",
						response.unmodified, "\nmodified:\n",
						evalResult.modifiedResponse);
				final ArrayList<String> autoFeedback = new ArrayList<String>();
				autoFeedback.add(e.getErrorFeedback());
				evalResult.autoFeedback = 
						autoFeedback.toArray(new String[autoFeedback.size()]);
				debugPrint(SELF + "autoFeedback = ", evalResult.autoFeedback); 
			} // if !isPositive
		} catch (MechFormatException e) {
			Utils.alwaysPrint(SELF + "MechFormatException: ", e.getMessage());
			e.printStackTrace();
			evalResult.verificationFailureString = SELF + "threw a MechFormat"
					+ "Exception. Please report this evaluator error "
					+ "to your instructor.";
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "MolFormatException: ", e.getMessage());
			e.printStackTrace();
			evalResult.verificationFailureString = SELF + "threw a MolFormat"
					+ "Exception. Please report this software error "
					+ "to the webmaster: " + e.getMessage();
		} catch (SearchException e) {
			Utils.alwaysPrint(SELF + "SearchException: ", e.getMessage());
			e.printStackTrace();
			evalResult.verificationFailureString = SELF + "threw a Search"
					+ "Exception. Please report this software error "
					+ "to the webmaster: " + e.getMessage();
		} catch (Exception e) {
			Utils.alwaysPrint(SELF + "unknown exception: ", e.getMessage());
			e.printStackTrace();
			evalResult.verificationFailureString = SELF + "threw an unknown "
					+ "Exception. Please report this software error "
					+ "to the webmaster: " + e.getMessage();
		}
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 					{ return EVAL_CODES[MECH_INIT]; } 
	/** Gets whether the evaluator is satisfied by every step correct or any
	 * step incorrect.
	 * @return	true if the evaluator is satisfied by every step correct
	 */
	public boolean getIsPositive() 					{ return isPositive; } 
	/** Sets whether the evaluator is satisfied by every step correct or any
	 * step incorrect.
	 * @param	isPositive	true if the evaluator is satisfied by every step
	 * correct
	 */
	public void setIsPositive(boolean isPositive) 	{ this.isPositive = isPositive; } 
	/** Gets flags for this evaluator.
	 * @return	the flags
	 */
	public int getFlags() 							{ return flags; } 
	/** Gets flags for this evaluator.
	 * @param	flags	flags for this evaluator
	 */
	public void setFlags(int flags)					{ this.flags = flags; } 
	/** Not used.  Required by interface. 
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 			{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade()		 			{ return false; }

} // MechInitiation

