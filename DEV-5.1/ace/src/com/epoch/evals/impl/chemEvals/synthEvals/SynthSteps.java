package com.epoch.evals.impl.chemEvals.synthEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.CompareNums;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.synthesis.Synthesis;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the {greatest number of linear, total number of} synthetic steps 
 * {is, is not} {=, &lt;, &gt;} <i>n</i> ... */
public class SynthSteps extends CompareNums implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether to count longest linear or total steps. */
	private int kind;
		/** Value for kind.  */
		public static final int LINEAR = 1;
		/** Value for kind.  */
		public static final int TOTAL = 2;
	/** Number of steps against which to compare. */
	private int limit;
	/** Amount by which to decrease grade for each step over the limit when
	 * evaluator is satisfied. */
	private double decrement;

	/** Constructor. */
	public SynthSteps() { 
		kind = LINEAR;
		setOper(GREATER); // inherited from CompareNums
		limit = 4; // no particular reason why 4
		decrement = 0;
	} // SynthSteps()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>kind</code>/<code>oper</code>/<code>limit</code>/<code>decrement</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public SynthSteps(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 3) {
			kind = MathUtils.parseInt(splitData[0]);
			setOper(Utils.indexOf(SYMBOLS, splitData[1]));
			limit = MathUtils.parseInt(splitData[2]);
			decrement = MathUtils.parseDouble(splitData[3]);
		} else {
			throw new ParameterException("SynthSteps ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // SynthSteps(String) 

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>kind</code>/<code>oper</code>/<code>limit</code>/<code>decrement</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(kind, '/', SYMBOLS[getOper()],
				'/', limit, '/', decrement);
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
		return Utils.toString("If the number of ",
				kind == LINEAR ? "linear" : "total",
				" synthetic steps is", OPER_ENGLISH[FEWER][getOper()], limit);
	} // toEnglish()  

	/** Determines whether the response contains the indicated number of
	 * synthetic steps as measured in the indicated way.  
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) {
		final OneEvalResult evalResult = new OneEvalResult();
		final Synthesis synthesis = (Synthesis) response.parsedResp;
		if (synthesis == null) { 
			final String msg = "SynthSteps: synthesis has not been "
					+ "initialized in Response.";
			Utils.alwaysPrint(msg);
			evalResult.verificationFailureString = msg;
			return evalResult;
		} // try 
		// get count to check from the mechanism or synthesis
		int count = 0;
		switch (kind) {
			case LINEAR:
				count = synthesis.getMaxLinearSteps();
				break;
			case TOTAL:
				count = synthesis.getNumArrows();
				debugPrint("SynthSteps: ", count, " total steps.");
				break;
			default: 
				final String msg = "SynthSteps: invalid kind = " + kind;
				Utils.alwaysPrint(msg);
				evalResult.verificationFailureString = msg;
				return evalResult;
		} // switch(kind)
		evalResult.isSatisfied = compare(count, limit);
		final int oper = getOper();
		if (evalResult.isSatisfied && decrement != 0
				&& Utils.among(oper, GREATER, NOT_LESS)) {
			int excess = count - limit;
			if (oper == NOT_LESS) excess++;
			final double reduced = 1 - ((double) excess) * decrement;
			evalResult.calcScore = Math.max(reduced, 0);
		} // if should calculate grade
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[SYNTH_STEPS]; } 
	/** Gets method for counting steps. 
	 * @return	value indicating how to count steps
	 */
	public int getKind() 					{ return kind; } 
	/** Sets method for counting steps. 
	 * @param	kind	how to count steps
	 */
	public void setKind(int kind) 			{ this.kind = kind; } 
	/** Gets number of steps against which to compare. 
	 * @return	number of steps
	 */
	public int getLimit() 					{ return limit; } 
	/** Sets number of steps against which to compare. 
	 * @param	limit	number of steps
	 */
	public void setLimit(int limit) 		{ this.limit = limit; } 
	/** Gets the decrement of the grade for each item over the limit
	 * @return	decrement of the grade for each item over the limit
	 */
	public double getDecrement() 			{ return decrement; } 
	/** Sets the decrement of the grade for each item over the limit
	 * @param	dec	decrement of the grade for each item over the limit
	 */
	public void setDecrement(double dec) 	{ decrement = dec; } 

	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() { 
		final int oper = getOper();
		return decrement != 0 && Utils.among(oper, GREATER, NOT_LESS); 
	} // getCalcGrade()

} // SynthSteps

