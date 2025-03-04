package com.epoch.evals.impl.chemEvals.synthEvals;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolString;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.evalConstants.OneEvalConstants;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.synthesis.SynthError;
import com.epoch.synthesis.Synthesis;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Map;

/** If {every, not every} compound in the synthesis is either produced by a 
 * reaction in a previous stage or is an acceptable starting material ...  */ 
public class SynthScheme extends SynthPartCredits
		implements EvalInterface, OneEvalConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	public SynthScheme() {
		isPositive = true;
	} // SynthScheme()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>
	 * <br>or<br>
	 * <code>isPositive</code>/<code>partCredits</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public SynthScheme(String data) throws ParameterException {
		debugPrint("SynthScheme: data = ", data);
		final String[] splitData = data.split("/");
		isPositive = Utils.isPositive(splitData[0]);
		if (splitData.length > 1) partCreditsStr = splitData[1];
	} // SynthScheme(String)

	/** Gets a string representation of data (other than a molecule) that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>isPositive</code>
	 * <br>or<br>
	 * <code>isPositive</code>/<code>partCredits</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		final StringBuilder data = Utils.getBuilder(isPositive ? 'Y' : 'N');
		if (!Utils.isEmpty(partCreditsStr)) {
			Utils.appendTo(data, '/', partCreditsStr);
		} // if there are partial credits
		return data.toString();
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
		final StringBuilder words = Utils.getBuilder("If "); 
		if (!isPositive) words.append("not ");
		words.append("every compound in each step is produced by a "
				+ "reaction of a previous step or is a "
				+ "permissible starting material");
		return words.toString();
	} // toEnglish() 

	/** Determines whether the response reactions of each step produce all 
	 * of the compounds in the subsequent step that are not permissible 
	 * starting materials. 
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified 
	 * with color or automatically generated feedback or a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) {
		final String SELF = "SynthScheme.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		try {
			// will throw SynthError if false
			final Synthesis synthesis = (Synthesis) response.parsedResp;
			synthesis.checkValidRxnProducts(); 
			evalResult.isSatisfied = isPositive;
		} catch (SynthError e) {
			evalResult.isSatisfied = !isPositive;
			if (!isPositive) {
				evalResult.modifiedResponse = e.getMessage();
				final ArrayList<String> autoFeedback = new ArrayList<String>();
				autoFeedback.add(e.getErrorFeedback());
				if (e.errorNumber == NO_PRODS_IN_NEXT_STAGE 
						&& !Utils.isEmpty(e.calcdProds)
						&& response.maySeeSynthCalcProds) {
					String calcdProds = e.calcdProds;
					try {
						final Molecule mol = MolImporter.importMol(calcdProds);
						ChemUtils.implicitizeH(mol, 
								MolAtom.ALL_H & ~MolAtom.WEDGED_H);
						calcdProds = MolString.toString(mol, SMILES);
					} catch (MolFormatException e1) {
						debugPrint(SELF + "MolFormatException");
					}
					autoFeedback.add("***See the products*** ACE has "
							+ "calculated for the highlighted stage.");
					final String href = Utils.toString(
							"<a href=\"javascript:", OPEN_CALCD_PRODS,
							"('", Utils.toValidJS(calcdProds), "')\">");
					evalResult.autoFeedbackVariableParts = 
							new String[] {href, "</a>"};
					debugPrint(SELF + "calcdProds = ", calcdProds,
							", autoFeedbackVariableParts = ",
							evalResult.autoFeedbackVariableParts);
					// insert untranslated variable parts around demarcated phrase
					evalResult.howHandleVarParts |= INSERT;
				} else if (e.errorNumber == BAD_SM) {
					// calcdProds is SMILESofOffendingCpd [tab] badSMName
					final String[] pieces = e.calcdProds.split("\t");
					e.calcdProds = pieces[0];
					evalResult.autoFeedbackVariableParts = 
							new String[] {Utils.toValidJS(pieces[1])};
					// substitute demarcated phrase with translated variable part
					evalResult.howHandleVarParts |= TRANSLATE;
				} // if calcdProds aren't null and there is an error number
				evalResult.autoFeedback = 
						autoFeedback.toArray(new String[autoFeedback.size()]);
				final Map<Integer, Integer> partCreditsMap = getPartCreditsMap();
				final Integer partCreditObj = 
						partCreditsMap.get(Integer.valueOf(e.errorNumber));
				if (partCreditObj != null) {
					evalResult.calcScore = 
							((double) partCreditObj.intValue()) / 100.0;
				} // if a partial credit is associated with this error
			} // if !isPositive
		} catch (Exception e) {
			Utils.alwaysPrint(SELF + "unknown exception: ", e.getMessage());
			e.printStackTrace();
			evalResult.verificationFailureString = 
					SELF + "checkValidRxnProducts() threw an unknown "
					+ " exception. Please report this software error to "
					+ "the webmaster: " + e.getMessage();
		}
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Gets the codes of SynthErrors that this evaluator might throw.
	 * @return	codes of SynthErrors that this evaluator might throw
	 */
	static int[] getSynthErrorCodes() {
		return new int[] {
				TOO_MANY_REACTANTS,
				NO_RXN_PRODUCTS,
				WRONG_ENANTIOMER,
				WRONG_DIASTEREOMER,
				NO_PRODS_IN_NEXT_STAGE,
				MINOR_PRODUCT,
				UNSPECIFIED_STEREOISOMERS,
				LAST_NOT_RXN_PRODUCT,
				BAD_SM,
				START_STAGE_HAS_IMPERMISSIBLE_SM,
				CONTAINS_IMPERMISSIBLE_SM};
	} // getSynthErrorCodes()

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode()			{ return EVAL_CODES[SYNTH_SCHEME]; } 

} // SynthScheme

