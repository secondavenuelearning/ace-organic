package com.epoch.evals.impl.physicsEvals.eqnsEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.CompareNums;
import com.epoch.exceptions.ParameterException;
import com.epoch.physics.Equations;
import com.epoch.physics.physicsConstants.CanonicalizedUnitConstants;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** If the number of units and variables in the {first, last} entry of the 
 * response {is, is not} {=, &lt;, &gt;} <i>n</i> ... */ 
public class EqnVariables extends CompareNums 
		implements EvalInterface, CanonicalizedUnitConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Number of entries against which to compare. */
	private int authNum;
	/** Which equation to compare. */
	private int which;
		/** Value of which. */
		public static final int LAST = 0;
		/** Value of which. */
		public static final int FIRST = 1;

	/** Constructor. */
	public EqnVariables() {
		authNum = 1; // no particular reason why 1
		setOper(NOT_EQUALS); // inherited from CompareNum
		which = LAST;
	} // EqnVariables()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>oper</code>/<code>authNum</code>/<code>which</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public EqnVariables(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 3) {
			setOper(Utils.indexOf(SYMBOLS, splitData[0]));
			authNum = MathUtils.parseInt(splitData[1]);
			which = MathUtils.parseInt(splitData[2]);
		} 
		if (splitData.length < 3 || getOper() == -1) {
			throw new ParameterException("EqnVariables ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // EqnVariables(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>oper</code>/<code>authNum</code>/<code>which</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(SYMBOLS[getOper()], '/', authNum, '/', which);
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
		return Utils.toString("If the number of units and variables in the ",
				which == FIRST ? "first" : "last", " entry is",
				OPER_ENGLISH[FEWER][getOper()], authNum);
	} // toEnglish()

	/** Determines whether an entry in the response contains the indicated 
	 * number of units and variables.  
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) {
		final String SELF = "EqnVariables.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		final Equations resp = (Equations) response.parsedResp;
		final String respEntry = (which == FIRST
				? resp.getFormattedEntry(1, NO_CANONICALZN) 
				: resp.getFormattedLastEntry(NO_CANONICALZN));
		debugPrint(SELF, which == FIRST ? "first" : "last",
				" entry: ", respEntry);
		final String constants = resp.getFormattedConstants();
		final String[] respEqns = Utils.splitTrim(respEntry, ",");
		final List<String> variables = new ArrayList<String>();
		for (final String respEqn : respEqns) {
			final String[] vars = EqnsUtils.getVariables(respEqn, constants);
			for (final String var : vars) {
				if (!variables.contains(var)) variables.add(var);
			} // for each variable
		} // for each equation in entry 2
		debugPrint(SELF + "entry: ", respEntry, "\nhas variables: ",
				variables);
		evalResult.isSatisfied = compare(variables.size(), authNum);
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[EQN_VARS]; } 
	/** Gets the number of entries against which to compare. 
	 * @return	number of entries against which to compare
	 */
	public int getAuthNum() 				{ return authNum; } 
	/** Sets the number of entries against which to compare. 
	 * @param	num	number of entries against which to compare
	 */
	public void setAuthNum(int num) 		{ authNum = num; } 
	/** Gets which equation to compare. 
	 * @return	which equation to compare
	 */
	public int getWhich() 					{ return which; } 
	/** Sets which equation to compare. 
	 * @param	wh	which equation to compare
	 */
	public void setWhich(int wh) 			{ which = wh; } 

} // EqnVariables
