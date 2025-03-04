package com.epoch.evals.impl.chemEvals;

import com.epoch.chem.Formula;
import com.epoch.chem.FormulaException;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.evalConstants.OneEvalConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the format of the formula {violates, does not violate} the
 * rule ...  */ 
public class FormulaFormat implements EvalInterface, OneEvalConstants {
	
	private void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Which rule to check for violations. */
	private int rule;
		/** Value for rule.   */
		public static final int CAPITALIZATION = 1;
		/** Value for rule.   */
		public static final int HILL_ORDER = 2;
		/** Value for rule.   */
		public static final int EXPLICIT_1 = 3;
	/** Whether the evaluator is satisfied if the rule is upheld (true) or 
	 * if it is violated (false).  */
	private boolean isPositive; 

	/** Array (1-based) containing English-language versions of each rule. */
	public static final String[] RULES_TEXT = { "",
		"The first letter of an element's symbol is always capitalized, and "
			+ "subsequent letters are never capitalized",
		"In a formula, the elements C, H, D, and T are listed first, and "
			+ "other elements are listed alphabetically",
		"When a formula contains only one atom of an element, the number 1 "
			+ "is omitted"
	};

	/** Constructor. */
	public FormulaFormat() {
		isPositive = false;
	} // FormulaFormat()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>rule</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public FormulaFormat(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 2) {
			isPositive = Utils.isPositive(splitData[0]);
			rule = MathUtils.parseInt(splitData[1]);
		} else {
			throw new ParameterException("FormulaFormat ERROR: unknown input data " 
					+ "'" + data + "'. "); 
		}
	} // FormulaFormat(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format:<br>  
	 * <code>isPositive</code>/<code>rule</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive ? "Y/" : "N/", rule);
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
		return Utils.toString("If the response formula ",
				isPositive ? " upholds " : "violates ", "the rule: ", 
				RULES_TEXT[rule]);
	} // toEnglish() 

	/** Determines whether the response violates the indicated rule.  
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified 
	 * with color or automatically generated feedback or a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) { 
		final String SELF = "FormulaFormat.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		boolean valid = false;
		final Formula respFormula = (Formula) response.parsedResp;
		try {
			switch (rule) {
				case CAPITALIZATION:	respFormula.noBadCaseElement(); break;
				case HILL_ORDER: 		respFormula.inHillOrder();		break;
				case EXPLICIT_1:		respFormula.noExplicit1(); 		break;
				default: 
					evalResult.verificationFailureString = 
							"FormulaFormat: invalid rule number given: "
							+ rule + " (valid = 1..." 
							+ (RULES_TEXT.length - 1) + ")";
					return evalResult;
			} // switch (rule)
			valid = true;
		} catch (FormulaException e) {
			debugPrint(SELF + "caught FormulaException; isPositive = ", 
					isPositive);
			if (!isPositive) {
				evalResult.autoFeedback = 
						new String[] {Utils.toString(RULES_TEXT[rule], '.')};
			} // if not positive
		} catch (Exception e) {
			evalResult.verificationFailureString = 
					"FormulaFormat: error in formula setup: " + e.getMessage();
			e.printStackTrace();
			return evalResult;
		} // try
		evalResult.isSatisfied = isPositive == valid;
		return evalResult;	 
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[FORMULA_FORMAT]; } 
	/** Gets which rule to check for violations. 
	 * @return	which rule to check for violations
	 */
	public int getRule() 						{ return rule; } 
	/** Sets which rule to check for violations. 
	 * @param	rule	which rule to check for violations
	 */
	public void setRule(int rule) 				{ this.rule = rule; } 
	/** Gets whether the evaluator is satisfied if the rule is upheld or 
	 * if it is violated.  
	 * @return	true if the evaluator is satisfied by being upheld
	 */
	public boolean getIsPositive() 				{ return isPositive; } 
	/** Sets whether the evaluator is satisfied if the rule is upheld or 
	 * if it is violated.  
	 * @param	isPos	true if the evaluator is satisfied by being upheld
	 */
	public void setIsPositive(boolean isPos)	{ isPositive = isPos; } 
	/** Not used.  Required by interface. 
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 		{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 				{ return false; }

} // FormulaFormat
