package com.epoch.evals.impl.chemEvals.mechEvals;

import chemaxon.struc.Molecule;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.Subevaluator;
import com.epoch.evals.impl.Counter;
import com.epoch.evals.impl.chemEvals.Atoms;
import com.epoch.evals.impl.chemEvals.chemEvalConstants.ProdStartConstants;
import com.epoch.evals.impl.implConstants.CountConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.mechanisms.Mechanism;
import com.epoch.mechanisms.mechConstants.MechConstants;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the starting materials or products compare (in various ways) with
 * author-specified materials ...  */
public class MechProdStartProps extends Counter implements CountConstants, 
		EvalInterface, MechConstants, ProdStartConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether to examine starting materials, products, or intermediates. */
	private int cpdsType;
	/** Which evaluator to use. */
	// private int evalType;

	/** Constructor. */
	public MechProdStartProps() { // default values
		howMany = ANY;
		cpdsType = PRODUCT;
	} // MechProdStartProps()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>howMany</code>/<code>cpdsType</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public MechProdStartProps(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 2) {
			howMany = MathUtils.parseInt(splitData[0]);
			cpdsType = MathUtils.parseInt(splitData[1]);
		} else {
			throw new ParameterException("MechProdStartProps ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // MechProdStartProps(String)

	/** Gets a string representation of data (other than a molecule) that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>howMany</code>/<code>cpdsType</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(howMany, '/', cpdsType);
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
		final StringBuilder words = Utils.getBuilder("If", 
				HOWMANY_ENGL[howMany - 1], PROD_START_ENGL[cpdsType],
				" in the response mechanism ");
		final String evalCodeAndData = molName;
		final Subevaluator evalGen = getSubevaluator(evalCodeAndData);
		final String evalWords = evalGen.toEnglish(true);
		final String HAVE = "have";
		final String HAVE_NOT = "have not";
		final String CONTAIN = "contain";
		final String DO_NOT = "do not";
		if (evalWords.startsWith(HAVE)) {
			int start = HAVE.length();
			if (evalWords.startsWith(HAVE_NOT)) {
				words.append("does not have");
				start = HAVE_NOT.length();
			} else words.append("has");
			words.append(evalWords.substring(start)
					.replaceAll("equal to", "exactly"));
		} else if (evalWords.startsWith(CONTAIN)) {
			Utils.appendTo(words, "contains", 
					evalWords.substring(CONTAIN.length()));
		} else if (evalWords.startsWith(DO_NOT)) {
			Utils.appendTo(words, "does not",
					evalWords.substring(DO_NOT.length()));
		} // if wording needs to be modified
		words.append("satisfies an evaluator");
		return words.toString();
	} // toEnglish()

	/** Converts the evaluator match code and the codedData (stored in molName
	 * or molStruct) into a Subevaluator object.
	 * @param	evalCodeAndData	matchCode/codedData
	 * @return	an Subevaluator
	 */
	public Subevaluator getSubevaluator(String evalCodeAndData) {
		final Subevaluator subeval = new Subevaluator(); 
		final int firstDiv = evalCodeAndData.indexOf('/');
		subeval.matchCode = evalCodeAndData.substring(0, firstDiv);
		subeval.codedData = evalCodeAndData.substring(firstDiv + 1);
		return subeval;
	} // getSubevaluator(String)

	/** Creates a default Subevaluator object.
	 * @return	a Subevaluator
	 */
	public Subevaluator getSubevaluator() {
		return getSubevaluator(Utils.toString(EVAL_CODES[NUM_ATOMS], '/', 
				Atoms.SYMBOLS[Atoms.GREATER], "/4/C/N"));
	} // getSubevaluator()

	/** Determines whether the response's products, starting materials, or
	 * intermediates have the given property.
	 * @param	response	a parsed response
	 * @param	evalCodeAndData	match code and coded data for the evaluator 
	 * being called by this one
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified
	 * with color or a message describing an inability to evaluate the
	 * response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String evalCodeAndData) {
		final String SELF = "MechProdStartProps.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		final Mechanism mech = (Mechanism) response.parsedResp;
		final int FLAGS = RESON_LENIENT & STEREO_LENIENT;
		debugPrint(SELF + toEnglish());
		try {
			final Molecule[] mols = 
					(cpdsType == PRODUCT ? mech.getAllResponseProducts(FLAGS)
					: cpdsType == START ? mech.getAllResponseStarters(FLAGS)
					: mech.getAllResponseIntermediates(FLAGS));
			debugPrint(SELF + "got ", mols.length, ' ',
					PROD_START_ENGL[cpdsType], "(s): ", mols);
			final Subevaluator evalGen = getSubevaluator(evalCodeAndData);
			final int[] matches = new int[2];
			int molNum = 0;
			for (final Molecule mol : mols) {
				final Response resp = new Response(mol);
				final OneEvalResult oneMolEvalResult = 
						evalGen.isResponseMatching(resp);
				final boolean satisfied = oneMolEvalResult != null 
						&& oneMolEvalResult.verificationFailureString == null
						&& oneMolEvalResult.isSatisfied;
				if (satisfied) {
					matches[MATCHES]++;
					debugPrint(SELF + "molecule ", ++molNum, ", ", mol,
							", satisfies evaluator.");
					if (Utils.among(howMany, ANY, NONE)) break;
				} else {
					matches[NONMATCHES]++;
					debugPrint(SELF + "molecule ", ++molNum, ", ", mol,
							", doesn't satisfy evaluator.");
					if (Utils.among(howMany, NOT_ALL, ALL)) break;
				}
			} // for each response molecule molNum
			debugPrint(SELF + "matches = ", matches[MATCHES],
					", nonmatches = ", matches[NONMATCHES]);
			evalResult.isSatisfied = getIsSatisfied(matches);
		} catch (Exception e) {
			Utils.alwaysPrint(SELF + "exception thrown: ", e.getMessage());
			e.printStackTrace();
		} // try
		debugPrint(SELF + "isSatisfied = ", evalResult.isSatisfied);
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[MECH_PRODS_STARTERS_PROPS]; }
	/** Gets whether to examine starting materials, products, or intermediates.
	 * @return	what to examine
	 */
	public int getCpdsType() 				{ return cpdsType; }
	/** Sets whether to examine starting materials, products, or intermediates.
	 * @param	type	what to examine
	 */
	public void setCpdsType(int type) 		{ cpdsType = type; }
	/** Sets the coded data for the evaluator.
	 * @param	molName	coded data for the evaluator
	 */
	public void setMolName(String molName)	{ this.molName = molName; }

} // MechProdStartProps

