package com.epoch.evals.impl.chemEvals.mechEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.CompareNums;
import com.epoch.exceptions.ParameterException;
import com.epoch.mechanisms.Mechanism;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the number of graphical mechanism components 
 * {is, is not} {=, &lt;, &gt;} <i>n</i> ... */ 
public class MechCounter extends CompareNums implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether to count boxes, reaction arrows, all straight arrows, or all of
	 * the above. */
	private int component;
		/** Value for component.  */
		public static final int BOXES = 1;
		/** Value for component.  */
		public static final int REACTION_ARROWS = 2;
		/** Value for component.  */
		public static final int REACTION_AND_RESONANCE_ARROWS = 3;
		/** Value for component.  */
		public static final int NUM_COMPONENTS = 3; // how many are in above list
	/** Number of components against which to compare. */
	private int limit;
	/** Amount by which to decrease grade for each component over the limit when
	 * evaluator is satisfied. */
	private double decrement;

	/** English version of each component. */
	public static final String COMPONENT_ENGLISH[] = { "",
		"boxes",
		"reaction arrows",
		"reaction and resonance arrows",
	};

	/** Constructor. */
	public MechCounter() { // default values
		component = REACTION_ARROWS;
		setOper(GREATER); // inherited from CompareNums
		limit = 4; // no particular reason why 4
		decrement = 0;
	} // MechCounter()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>component</code>/<code>oper</code>/<code>limit</code>/<code>decrement</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public MechCounter(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 3) {
			component = MathUtils.parseInt(splitData[0]);
			setOper(Utils.indexOf(SYMBOLS, splitData[1]));
			limit = MathUtils.parseInt(splitData[2]);
			decrement = (splitData.length == 3 ? 0
					: MathUtils.parseDouble(splitData[3]));
		} else {
			throw new ParameterException("MechCounter ERROR: unknown input data " 
					+ "'" + data + "'. ");
		}
	} // MechCounter(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>component</code>/<code>oper</code>/<code>limit</code>/<code>decrement</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(component, '/', SYMBOLS[getOper()], '/', limit,
				decrement == 0 ? "" : Utils.getBuilder('/', decrement));
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
		return Utils.toString("If the number of ", COMPONENT_ENGLISH[component], 
				" is", OPER_ENGLISH[FEWER][getOper()], limit);
	} // toEnglish() 

	/** Determines whether the response has the indicated number of the
	 * indicated type of graphical object.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) {
		final OneEvalResult evalResult = new OneEvalResult();
		final Mechanism mechanism = (Mechanism) response.parsedResp;
		if (mechanism == null) { 
			final String msg = "MechCounter: mechanism has not "
					+ "been initialized in Response.";
			Utils.alwaysPrint(msg);
			evalResult.verificationFailureString = msg;
			return evalResult;
		} // try 
		int count = 0;
		switch (component) {
			case BOXES:
				count = mechanism.getNumBoxes();
				break;
			case REACTION_ARROWS:
				count = mechanism.getNumReactionArrows();
				break;
			case REACTION_AND_RESONANCE_ARROWS:
				count = mechanism.getNumReactionArrows() 
						+ mechanism.getNumResonanceArrows();
				break;
			default: 
				final String msg = "MechCounter: invalid component = " 
						+ component;
				Utils.alwaysPrint(msg);
				evalResult.verificationFailureString = msg;
				return evalResult;
		} // switch(component) 
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
	public String getMatchCode() 			{ return EVAL_CODES[MECH_PIECES_COUNT]; } 
	/** Gets what to count (boxes, resonance arrows, all straight arrows). 
	 * @return	what to count
	 */
	public int getComponent() 				{ return component; } 
	/** Sets what to count (boxes, resonance arrows, all straight arrows). 
	 * @param	component	what to count
	 */
	public void setComponent(int component)	{ this.component = component; } 
	/** Gets the value of the number against which to compare. 
	 * @return	value of the number against which to compare
	 */
	public int getLimit() 					{ return limit; } 
	/** Sets the value of the number against which to compare. 
	 * @param	limit	value of the number against which to compare
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

} // MechCounter

