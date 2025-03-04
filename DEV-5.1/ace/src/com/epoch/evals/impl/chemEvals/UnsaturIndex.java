package com.epoch.evals.impl.chemEvals;

import com.epoch.chem.Formula;
import com.epoch.chem.FormulaException;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.CompareNums;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the formula's unsaturation index {is, is not} {=, &lt;, &gt;} 
 * <i>n</i> or is negative or fractional ... */
public class UnsaturIndex extends CompareNums implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg, Utils.MRV);
	}

	/** Number of compounds against which to compare. */
	transient private int authUI;
	/** Value for oper, not used in compare(). */
	public static final int BAD_UNSATUR = 6;
	/** Value for oper, not used in compare(). */
	public static final int NEG_UNSATUR = 7;
	/** Value for oper, not used in compare(). */
	public static final int FRACT_UNSATUR = 8;
	/** Value for oper, not used in compare(). */
	public static final int GOOD_UNSATUR = 9;
	/** Database values for additional operators. */
	public static final String[] ADDL_SYMBOLS = new String[] 
			{"bad", "negative", "fractional", "good"};

	/** Constructor. */
	public UnsaturIndex() {
		authUI = 1;
		setOper(EQUALS); // inherited from CompareNums
	} // UnsaturIndex()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>oper</code>/<code>authUI</code>
	 * @throws	ParameterException	if the coded data is inappropriate
	 * for this evaluator
	 */
	public UnsaturIndex(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		final int numData = splitData.length;
		if (numData >= 2) {
			final String operStr = splitData[0];
			int myOper = Utils.indexOf(SYMBOLS, operStr);
			if (myOper > -1) setOper(myOper);
			else {
				myOper = Utils.indexOf(ADDL_SYMBOLS, operStr);
				if (myOper > -1) setOper(myOper + SYMBOLS.length);
				else setOper(myOper);
			} // if myOper
			authUI = MathUtils.parseInt(splitData[1]);
		}
		if (numData < 2 || getOper() == -1) {
			throw new ParameterException("UnsaturIndex ERROR: "
					+ "unknown input data '" + data + "'. ");
		} // if there are not two tokens
	} // UnsaturIndex(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>oper</code>/<code>authUI</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		final int myOper = getOper();
		return Utils.toString(myOper < SYMBOLS.length ? SYMBOLS[myOper] 
				: ADDL_SYMBOLS[myOper - SYMBOLS.length], '/', authUI);
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
		final int myOper = getOper();
		return Utils.toString("If the formula's unsaturation index is", 
				myOper < OPER_ENGLISH[FEWER].length 
					? Utils.getBuilder(OPER_ENGLISH[FEWER][myOper], authUI)
					: myOper == NEG_UNSATUR ? " negative"
					: myOper == FRACT_UNSATUR ? " fractional"
					: myOper == BAD_UNSATUR ? " negative or fractional"
					: " neither negative nor fractional");
	 } // toEnglish()

	/** Determines whether the response contains the indicated number of
	 * compounds.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "UnsaturIndex.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		final Formula respFormula = (Formula) response.parsedResp;
		final int myOper = getOper();
		try {
			final int respUI = respFormula.getUnsaturationIndex();
			if (myOper < SYMBOLS.length) {
				evalResult.isSatisfied = compare(respUI, authUI);
				debugPrint(SELF + "respUI = ", respUI, ", expected UI = ", 
						authUI, ", result = ", evalResult.isSatisfied);
			} else {
				if (myOper == GOOD_UNSATUR) {
					evalResult.isSatisfied = true;
				} // if myOper
				debugPrint(SELF + "operator is ", 
						ADDL_SYMBOLS[myOper - SYMBOLS.length],
						", formula doesn't have negative or fractional UI, "
						+ "evaluator ", myOper == GOOD_UNSATUR ? "" : "not ", 
						"satisfied.");
			} // if myOper
		} catch (FormulaException e) {
			if (myOper >= SYMBOLS.length) {
				final String msg = e.getMessage();
				evalResult.isSatisfied = myOper == BAD_UNSATUR
						|| (myOper == NEG_UNSATUR 
							&& msg.indexOf("negative") >= 0)
						|| (myOper == FRACT_UNSATUR 
							&& msg.indexOf("fractional") >= 0);
				if (evalResult.isSatisfied) {
					evalResult.autoFeedback = new String[] {msg};
				} // if evaluator is satisfied
				debugPrint(SELF + "operator is ", 
						ADDL_SYMBOLS[myOper - SYMBOLS.length],
						", formula has negative or fractional UI, "
						+ "evaluator ", evalResult.isSatisfied ? ""
							: "not ", "satisfied.");
			} else {
				evalResult.verificationFailureString = e.getMessage();
				debugPrint(SELF + "formula has negative or fractional UI, "
						+ "sending verificationFailure.");
			} // if myOper
		} // try
		return evalResult;	
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[UNSATURATION]; } 
	/** Gets the unsaturation index against which to compare.
	 * @return	the unsaturation index against which to compare
	 */
	public int getUI() 						{ return authUI; }
	/** Sets the unsaturation index against which to compare.
	 * @param	n	the unsaturation index against which to compare
	 */
	public void setUI(int n) 				{ authUI = n; }

} // UnsaturIndex
