package com.epoch.evals.impl.physicsEvals.vectorsEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.CompareNums;
import com.epoch.exceptions.ParameterException;
import com.epoch.physics.DrawVectors;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the number of vectors {is, is not} {=, &lt;, &gt;} <i>n</i> ... */ 
public class VectorsCt extends CompareNums implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Number of vectors against which to compare. */
	private int authNum;

	/** Constructor. */
	public VectorsCt() { // default values
		setOper(LESS); // inherited from CompareNums
		authNum = 4; // no particular reason why 4
	} // VectorsCt()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>oper</code>/<code>authNum</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public VectorsCt(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 2) {
			setOper(Utils.indexOf(SYMBOLS, splitData[0]));
			authNum = MathUtils.parseInt(splitData[1]);
		} else {
			throw new ParameterException("VectorsCt ERROR: unknown input data " 
					+ "'" + data + "'. ");
		}
	} // VectorsCt(String)

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
		return Utils.toString("If the number of vectors is",
				OPER_ENGLISH[FEWER][getOper()], authNum);
	} // toEnglish() 

	/** Determines whether the response has the indicated number of vectors.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) {
		final OneEvalResult evalResult = new OneEvalResult();
		// final int count = 0; // unused 11/6/2012
		final DrawVectors respVectors = (DrawVectors) response.parsedResp;
		if (respVectors == null) { 
			final String msg = "VectorsCt: respVectors has not "
					+ "been initialized in Response.";
			Utils.alwaysPrint(msg);
			evalResult.verificationFailureString = msg;
			return evalResult;
		} // try 
		final int respNum = respVectors.getVectorPoints().length;
		evalResult.isSatisfied = compare(respNum, authNum);
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[VECTORS_CT]; } 
	/** Gets the value of the number against which to compare. 
	 * @return	value of the number against which to compare
	 */
	public int getAuthNum() 				{ return authNum; } 
	/** Sets the value of the number against which to compare. 
	 * @param	num	value of the number against which to compare
	 */
	public void setAuthNum(int num) 		{ authNum = num; } 
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 			{ return false; }

} // VectorsCt

