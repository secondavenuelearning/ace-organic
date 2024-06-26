package com.epoch.evals.impl.genericQEvals.clickEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.CompareNums;
import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.ClickImage;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the total number of marks {is, is not} {=, &lt;, &gt;} <i>n</i> ... */ 
public class ClickCount extends CompareNums implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Number of words against which to compare. */
	private int authNum;

	/** Constructor. */
	public ClickCount() {
		authNum = 1; // no particular reason why 1
		setOper(NOT_EQUALS); // inherited from CompareNum
	} // ClickCount()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>oper</code>/<code>authNum</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public ClickCount(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 2)  {
			setOper(Utils.indexOf(SYMBOLS, splitData[0]));
			authNum = MathUtils.parseInt(splitData[1]);
		} 
		if (splitData.length < 2 || getOper() == -1) {
			throw new ParameterException("ClickCount ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // ClickCount(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>oper</code>/<code>authNum</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(SYMBOLS[getOper()], '/', authNum);
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
		return Utils.toString("If the total number of marks is",
				OPER_ENGLISH[FEWER][getOper()], authNum);
	} // toEnglish()

	/** Determines whether the response contains the indicated number of marks.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) {
		final OneEvalResult evalResult = new OneEvalResult();
		final ClickImage clickResp = (ClickImage) response.parsedResp;
		final int[][] allRespCoords = clickResp.getAllCoords();
		final int clickCt = allRespCoords.length;
		debugPrint("ClickCount.isResponseMatching: number of marks is ", 
				clickCt);
		evalResult.isSatisfied = compare(clickCt, authNum);
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[CLICK_CT]; } 
	/** Gets the number of marks against which to compare. 
	 * @return	number of marks against which to compare
	 */
	public int getAuthNum() 				{ return authNum; } 
	/** Sets the number of marks against which to compare. 
	 * @param	num	number of marks against which to compare
	 */
	public void setAuthNum(int num) 		{ authNum = num; } 

} // ClickCount
