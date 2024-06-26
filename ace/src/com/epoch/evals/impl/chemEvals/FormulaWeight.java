package com.epoch.evals.impl.chemEvals;

import com.epoch.chem.Formula;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the formula has an {exact, average} 
 * molecular weight that {is, is not} {=, &lt;, &gt;} <i>m</i> 
 * <br>Extends Weight evaluator. */ 
public class FormulaWeight extends Weight {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	public FormulaWeight() {
		wtType = AVERAGE_WT;
		authWtStr = "78.0";
		toleranceStr = "0.2";
		wtOper = NOT_EQUALS;
	} // FormulaWeight()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>authWtStr</code>/<code>toleranceStr</code>/<code>wtOper</code>/<code>wtType</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public FormulaWeight(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 4)  {
			authWtStr = splitData[0];
			toleranceStr = splitData[1];
			wtOper = Utils.indexOf(SYMBOLS, splitData[2]); 
			setOper(wtOper);
			wtType = Utils.indexOf(WT_TYPE, splitData[3]);
		}
		if (splitData.length < 4 || wtOper == -1 || molsOper == -1) {
			throw new ParameterException("FormulaWeight ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // FormulaWeight(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>authWtStr</code>/<code>toleranceStr</code>/<code>wtOper</code>/<code>wtType</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(authWtStr, '/', toleranceStr, '/',
				SYMBOLS[wtOper], '/', WT_TYPE[wtType], '/');
	} // getCodedData()

	/** Gets an English-language description of this evaluator.  
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish() {
		final StringBuilder words = Utils.getBuilder(
				"If the formula has an ",
				wtType == EXACT_MASS 
					? "exact mass" : "average molecular weight",
				" that is", OPER_ENGLISH[LESSER][wtOper], authWtStr);
		if (!Utils.isEmptyOrWhitespace(toleranceStr)) {
			Utils.appendTo(words, " &plusmn; ", toleranceStr);
		}
		return words.toString();
	} // toEnglish()

	/** Determines whether the response has the indicated weight of the 
	 * indicated type.  
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) {
		final String SELF = "FormulaWeight.isResponseMatching: ";
		debugPrint(SELF, toEnglish());
		final OneEvalResult evalResult = new OneEvalResult();
		final double authWt = MathUtils.parseDouble(authWtStr);
		final double tolerance = MathUtils.parseDouble(toleranceStr);
		final Formula respFormula = (Formula) response.parsedResp;
		final double respWt = respFormula.getWeight(wtType);
		evalResult.isSatisfied = compare(respWt, authWt, tolerance);
		debugPrint(SELF + "formula ", respFormula.getFormulaStr(), 
				" has wt ", respWt, 
				", oper is ", OPER_ENGLISH[LESSER][getOper()],
				", authWt is ", authWt, " +/- ", tolerance,
				", evaluator is ", 
				evalResult.isSatisfied ? "satisfied" : "not satisfied");
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[FORMULA_WEIGHT]; } 

} // FormulaWeight
