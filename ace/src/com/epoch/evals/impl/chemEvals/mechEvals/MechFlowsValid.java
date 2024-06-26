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

/** If the electron-flow arrows acting on compounds in {any step do not,
 * each step} lead to the compounds (excluding acceptable starting
 * materials) in the following step ...  */ 
public class MechFlowsValid 
		implements EvalInterface, MechConstants, OneEvalConstants {
	
	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether every step is correct (true) or any step is incorrect (false).
	 */
	private boolean	isPositive; // is or is not
	/** Flags for options.  Sets resonance permissiveness, stereochemistry
	 * leniency. */
	private int flags;

	/** Constructor. */
	public MechFlowsValid() {
		isPositive = true;
		flags = RESON_LENIENT;
	} // MechFlowsValid()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>flags</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public MechFlowsValid(String data) throws ParameterException {
		debugPrint("MechFlowsValid: data = ", data);
		final String[] splitData = data.split("/");
		if (splitData.length >= 2) {
			isPositive = Utils.isPositive(splitData[0]);
			flags = MathUtils.parseInt(splitData[1]);
		} else {
			throw new ParameterException("MechFlowsValid ERROR: unknown input data " 
					+ "'" + data + "'. ");
		}
	} // MechFlowsValid(String)

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
				"If the electron-flow arrows acting on compounds in ",
				isPositive ? " each step " : " any step do not ",
				"lead to the compounds in the following step");
		if (resonLenient) words.append(" (any resonance structures "
				+ "of starting materials acceptable");
		if (resonLenient && stereoLenient) words.append(", ");
		else if (resonLenient) words.append(')');
		else if (stereoLenient) words.append(" (");
		if (stereoLenient) words.append("stereochemistry ignored)");
		return words.toString();
	} // toEnglish() 

	/** Determines whether the response's electron-flow arrows are correct.  
	 * @param	response	a parsed response
	 * @param	materials	String representation of permissible starting
	 * materials for this mechanism
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified 
	 * with color or automatically generated feedback or a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String materials)  {
		final String SELF = "MechFlowsValid.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		final Mechanism mechanism = (Mechanism) response.parsedResp;
		try {
			debugPrint(SELF + "materials MRV:\n", materials);
			final Molecule starters = MolImporter.importMol(materials);
			debugPrint(SELF + "imported materials:\n", starters);
			if (!Utils.isEmpty(response.rGroupMols)) {
				SubstnUtils.substituteRGroups(starters, response.rGroupMols);
				debugPrint(SELF + "after R group substitution: ", starters);
			} // if there are generic R groups to substitute
		 	mechanism.checkFlowsValid(starters, flags); // MechError if not
			evalResult.isSatisfied = isPositive;
		} catch (MechError e) {
			evalResult.isSatisfied = !isPositive;
			if (!isPositive) {
				evalResult.modifiedResponse = e.getMessage();
				debugPrint(SELF + "Original response:\n",
						response.unmodified, "\nmodified:\n",
						evalResult.modifiedResponse);
				final ArrayList<String> autoFeedback = new ArrayList<String>();
				final String errorFeedback = e.getErrorFeedback();
				autoFeedback.add(errorFeedback);
				if (Utils.among(e.errorNumber, FLOWS_GIVE_NO_PRODS, 
						NOT_STARTER, NOT_PROD_NOR_STARTER, 
						NOT_PROD_NOR_STARTER_1ST_CYCLIC, ODD_ELECTRON_BOND, 
						TOO_MANY_OUTER_ELECTRONS, VALENCE_ERROR)) {
					final ArrayList<String> varParts = new ArrayList<String>();
					if (!Utils.isEmpty(e.offendingCpds) 
							&& errorFeedback.contains(STARS)) {
						varParts.add(Utils.toString(
								"<a href=\"javascript:", OPEN_OFFENDERS, "('", 
								Utils.toValidJS(e.offendingCpds), "')\">"));
						varParts.add("</a>");
					} // if there are offendingCpds and a phrase to substitute
					if (!Utils.isEmpty(e.calcdProds)) {
						autoFeedback.add(Utils.toString(
								"***See the products*** ACE has "
									+ "calculated from the electron-flow arrows "
									+ "in the ", 
								e.errorNumber == NOT_PROD_NOR_STARTER 
								? "stage prior to the highlighted one."
								: e.errorNumber == 
									NOT_PROD_NOR_STARTER_1ST_CYCLIC
								? "stages prior to the highlighted one."
								: "highlighted stage."));
						varParts.add(Utils.toString(
								"<a href=\"javascript:", OPEN_CALCD_PRODS, 
								"('", Utils.toValidJS(e.calcdProds), "')\">"));
						varParts.add("</a>");
						if (e.errorNumber == ODD_ELECTRON_BOND) {
							final String DOTTED = "<b>&middot;&middot;"
									+ "&middot;&middot;&middot;</b>";
							autoFeedback.add("(Bonds of order 0.5, 1.5, and "
									+ "2.5 are indicated by " + DOTTED 
									+ ", <u>" + DOTTED + "</u>, and "
									+ "&ndash;=&ndash;=&ndash;, "
									+ "respectively.)");
						} // if errorNumber is half integral bond
					} else if (e.errorNumber != NOT_STARTER) { // not 1st step of mech
						autoFeedback.add(Utils.toString(
								"ACE has calculated no products of "
									+ "electron-flow arrows in the ",
								e.errorNumber == NOT_PROD_NOR_STARTER 
								? "stage prior to the highlighted one."
								: "highlighted stage."));
					} // if there are calcProds
					evalResult.autoFeedbackVariableParts = 
							varParts.toArray(new String[varParts.size()]);
					// insert untranslated variable parts around 
					// demarcated phrase
					evalResult.howHandleVarParts |= INSERT;
				} // if errorNumber
				evalResult.autoFeedback = 
						autoFeedback.toArray(new String[autoFeedback.size()]);
				debugPrint(SELF + "autoFeedback = ", evalResult.autoFeedback, 
						", variable parts = ", 
						evalResult.autoFeedbackVariableParts,
						", e.calcdProds = ", e.calcdProds);
			} // if !isPositive
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
		} catch (MechFormatException e) {
			Utils.alwaysPrint(SELF + "MechFormatException: ", e.getMessage());
			e.printStackTrace();
			evalResult.verificationFailureString = SELF + "threw a MechFormat"
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
	public String getMatchCode() 					{ return EVAL_CODES[MECH_FLOWS]; } 
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

} // MechFlowsValid

