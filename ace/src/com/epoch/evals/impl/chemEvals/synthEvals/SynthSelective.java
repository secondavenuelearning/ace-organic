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
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Map;

/** If the synthesis is {enantio-, diastereo, structure-selective} ... */
public class SynthSelective extends SynthPartCredits
		implements EvalInterface, OneEvalConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** The kind of selectivity to be assessed. */ 
	private int kind;
		/** Value for kind.  */
		public static final int ENANTIO = 1;
		/** Value for kind.  */
		public static final int DIASTEREO = 2;
		/** Value for kind.  */
		public static final int ANY = 3;

	/** Constructor. */
	public SynthSelective() {
		kind = ENANTIO;
		isPositive = false;
	} // SynthSelective() 

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>kind</code>/<code>isPositive</code>
	 * <br>or<br>
	 * <code>kind</code>/<code>isPositive</code>/<code>partCredits</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public SynthSelective(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 2) {
			kind = MathUtils.parseInt(splitData[0]);
			isPositive = Utils.isPositive(splitData[1]);
			if (splitData.length > 2) partCreditsStr = splitData[2];
		} else {
			throw new ParameterException("SynthSelective ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // SynthSelective(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>kind</code>/<code>isPositive</code>
	 * <br>or<br>
	 * <code>kind</code>/<code>isPositive</code>/<code>partCredits</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		final StringBuilder data = Utils.getBuilder(kind, 
				isPositive ? "/Y" : "/N");
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
		final StringBuilder words = Utils.getBuilder("If the synthesis ");
		if (kind == ANY) {
			Utils.appendTo(words, isPositive ? "does not give" 
					: "gives", " a mixture of nonstereoisomeric products");
		} else {
			Utils.appendTo(words, isPositive ? "is " : "is not ",
					kind == ENANTIO ? "enantio" : "diastereo", "selective");
		} // if kind
		return words.toString();
	} // toEnglish()  

	/** Determines whether any of the response reactions lead to major products 
	 * other than the ones shown.  
	 * @param	response	a parsed response
	 * @param	authRxn	a synthetic step whose selectivity is to be ignored
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified with 
	 * color or automatically generated feedback or a message describing an 
	 * inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authRxn) {
		final OneEvalResult evalResult = new OneEvalResult();
		Synthesis stepOK = Synthesis.NO_STEPOK;
		if (!Utils.isEmptyOrWhitespace(authRxn)) try {
			stepOK = new Synthesis(authRxn);
			if (!Utils.isEmpty(response.rGroupMols)) {
				stepOK.substituteRGroups(response.rGroupMols);
			} // if there are R groups to substitute
		} catch (MolFormatException e) {
			debugPrint("SynthSelective: Couldn't parse reaction "
					+ "whose selectivity should be ignored; setting to null.");
		} // if author's step couldn't be imported
		if (stepOK != Synthesis.NO_STEPOK && !stepOK.initialized) {
			debugPrint("SynthSelective: Couldn't parse reaction "
					+ "whose selectivity should be ignored; setting to null.");
			stepOK = Synthesis.NO_STEPOK;
		} // if author's step couldn't be initialized
		try {
			final Synthesis synthesis = (Synthesis) response.parsedResp;
			synthesis.isSelective(kind, stepOK); // will throw SynthError if false
			evalResult.isSatisfied = isPositive;
		} catch (SynthError e) {
			evalResult.isSatisfied = !isPositive;
			if (!isPositive) {
				evalResult.modifiedResponse = e.getMessage();
				final ArrayList<String> autoFeedback = new ArrayList<String>();
				autoFeedback.add(e.getErrorFeedback());
				if (Utils.among(e.errorNumber, UNSELECTIVE, 
							UNDIASTEREOSELECTIVE_NOT_SHOWN, 
							UNDIASTEREOSELECTIVE_NOT_DISTING, 
							UNDIASTEREOSELECTIVE_SHOWN, 
							UNENANTIOSELECTIVE_NOT_SHOWN, 
							UNENANTIOSELECTIVE_NOT_DISTING, 
							UNENANTIOSELECTIVE_SHOWN)
						&& !Utils.isEmpty(e.calcdProds)
						&& response.maySeeSynthCalcProds) {
					String out = e.calcdProds;
					try {
						final Molecule mol = MolImporter.importMol(out);
						ChemUtils.implicitizeH(mol, 
								MolAtom.ALL_H & ~MolAtom.WEDGED_H);
						out = MolString.toString(mol, SMILES);
					} catch (MolFormatException e1) {
						debugPrint("IOException in isResponseMatching");
					}
					autoFeedback.add("***See the products*** ACE has "
							+ "calculated for the highlighted stage.");
					final String href = Utils.toString(
							"<a href=\"javascript:", OPEN_CALCD_PRODS,
							"('", Utils.toValidJS(out), "')\">");
					evalResult.autoFeedbackVariableParts = 
							new String[] {href, "</a>"};
					// insert untranslated variable parts around demarcated phrase
					evalResult.howHandleVarParts |= INSERT;
				} // if errorNumber with calcdProds
				evalResult.autoFeedback = 
						autoFeedback.toArray(new String[autoFeedback.size()]);
				final Map<Integer, Integer> partCreditsMap = getPartCreditsMap();
				final Integer partCreditObj = 
						partCreditsMap.get(Integer.valueOf(e.errorNumber));
				if (partCreditObj != null) {
					evalResult.calcScore = 
							((double) partCreditObj.intValue()) / 100.0;
				} // if a partial credit is associated with this error
			} // if not isPositive
		} catch (Exception e) {
			Utils.alwaysPrint("SynthSelective: unknown exception: ", 
					e.getMessage());
			e.printStackTrace();
			evalResult.verificationFailureString = 
					"SynthSelective: Synthesis.isSelective() threw an unknown"
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
				UNSELECTIVE,
				UNDIASTEREOSELECTIVE_NOT_SHOWN,
				UNDIASTEREOSELECTIVE_NOT_DISTING,
				UNDIASTEREOSELECTIVE_SHOWN,
				UNENANTIOSELECTIVE_NOT_SHOWN,
				UNENANTIOSELECTIVE_NOT_DISTING,
				UNENANTIOSELECTIVE_SHOWN};
	} // getSynthErrorCodes()

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[SYNTH_SELEC]; } 
	/** Gets the kind of selectivity to be assessed. 
	 * @return	the kind of selectivity to be assessed
	 */ 
	public int getKind() 					{ return kind; } 
	/** Sets the kind of selectivity to be assessed. 
	 * @param	kind	the kind of selectivity to be assessed
	 */ 
	public void setKind(int kind) 			{ this.kind = kind; } 

} // SynthSelective

