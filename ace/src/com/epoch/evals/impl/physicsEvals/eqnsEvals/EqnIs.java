package com.epoch.evals.impl.physicsEvals.eqnsEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.EquationFormatException;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.ProcessExecutionException;
import com.epoch.physics.EquationFunctions;
import com.epoch.physics.Equations;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** If the {first, last} equation in the response {is, is not} equivalent to the
 * author's equation... */ 
public class EqnIs extends EqnsEquiv implements EvalInterface {

	private static void debugPrint(Object... msg) {
		 Utils.printToLog(msg);
	}

	/** Which equation to compare. */
	private int which;
		/** Value of which. */
		public static final int LAST = 0;
		/** Value of which. */
		public static final int FIRST = 1;
	/** Whether the student's equation should equal or should nto equal the
	 * author's for this evaluator to be satisfied. */
	private boolean isPositive;
	/** Representation of the author's equation. */
	transient private String authEqn = null;

	/** Constructor. */
	public EqnIs() {
		which = LAST;
		isPositive = false;
		howManySolutions = ONE; // inherited from EqnsEquiv
	} // EqnIs()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>which</code>/<code>isPositive</code>/<code>howManySolutions</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public EqnIs(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 3)  {
			which = MathUtils.parseInt(splitData[0]);
			isPositive = Utils.isPositive(splitData[1]);
			howManySolutions = 
					Utils.indexOf(HOW_MANY_SOLNS_DB_VALUES, splitData[2]);
		} 
		if (splitData.length < 3) {
			throw new ParameterException("EqnIs ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // EqnIs(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>which</code>/<code>isPositive</code>/<code>howManySolutions</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(which, isPositive ? "/Y/" : "/N/",
				HOW_MANY_SOLNS_DB_VALUES[howManySolutions]);
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
		final StringBuilder words = Utils.getBuilder("If ");
		if (howManySolutions == ONE) {
			if (!isPositive) words.append("not ");
			words.append("all of ");
		} // if howManySolutions
		Utils.appendTo(words, "the equations or expressions in the ",
				which == FIRST ? "first" : "last", " entry in the response ");
		if (howManySolutions == ONE) {
			words.append("are equivalent to ");
			if (authEqn.indexOf(',') >= 0) {
				words.append("any of ");
			} // if the author lists more than one solution
		} else {
			if (!isPositive) words.append("do not ");
			words.append("correspond exactly to ");
		} // if howManySolutions
		Utils.addSpanString(words, authEqn, !Utils.TO_DISPLAY);
		return words.toString();
	} // toEnglish()

	/** Determines whether the first or last mathematical equation or expression 
	 * of the response equals the author's equation or expression.
	 * @param	response	a parsed response
	 * @param	origAuthEqn	the author's equation
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String origAuthEqn) {
		final String SELF = "EqnIs.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		final Equations resp = (Equations) response.parsedResp;
		final String constants = resp.getFormattedConstants();
		final String respEntry = (which == FIRST 
				? resp.getFormattedEntry(1) : resp.getFormattedLastEntry());
		final String[] respEqns = splitTrim(respEntry);
		final String authEqn = EquationFunctions.formatExpression(
				origAuthEqn, resp.getVariablesNotUnits());
		final List<String> authEqns = 
				new ArrayList<String>(Arrays.asList(splitTrim(authEqn)));
		boolean broke = false;
		for (final String respEqn : respEqns) {
			try {
				if (!areComparable(respEqn, authEqn)) {
					debugPrint(SELF + "response equation: ", respEqn, 
							"\nand author equation: ", authEqn,
							"\nare not both expressions or both equations.");
					evalResult.isSatisfied = !isPositive;
					broke = true;
				} else {
					final int equivEqnNum = 
							areEquivalent(respEqn, authEqn, constants);
					final boolean areEquiv = equivEqnNum > 0;
					if (areEquiv) debugPrint(SELF + "response equation: ", 
							respEqn, "\nis equivalent to author's equation ", 
							equivEqnNum, ": ", authEqns.get(equivEqnNum - 1));
					else debugPrint(SELF + "response equation: ", respEqn, 
							"\nis equivalent to none of author's equations: ",
							authEqn);
					evalResult.isSatisfied = areEquiv == isPositive;
					if (areEquiv) authEqns.set(equivEqnNum - 1, null);
					else broke = true;
				} // if equations are comparable
			} catch (ProcessExecutionException e) {
				Utils.alwaysPrint(SELF + "ProcessExecutionException: ", 
						e.getMessage());
				evalResult.verificationFailureString = "A process execution "
						+ "error occurred when ACE tried to evaluate a "
						+ "mathematical expression. Please report this error "
						+ "to the programmers.";
				broke = true;
			} catch (EquationFormatException e) {
				debugPrint(SELF + "EquationFormatException: ", 
						e.getMessage(), "\n", e.getEquation());
				evalResult.autoFeedback = new String[] {e.getMessage()};
				evalResult.autoFeedbackVariableParts = new String[] 
						{Utils.toString("<p>", respEqn, "</p>")};
				evalResult.verificationFailureString = ""; // not null
				broke = true;
			} // try
			if (broke) break;
		} // for each response equation in the entry
		if (!broke && howManySolutions == ALL) {
			evalResult.isSatisfied = 
					isPositive == Utils.allMembersAreNull(authEqns);
		} // if response must contain all solutions
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Determines whether two expressions or equations are equivalent.
	 * @param	exprnOrEqn1	one expression or equation
	 * @param	exprnOrEqn2	the other expression or equation
	 * @param	constants	the given constants
	 * @return	the number of the matching author's expression or equation
	 * if the expressions or equations are equivalent
	 * @throws	ProcessExecutionException	if the process can't be executed
	 * properly
	 * @throws	EquationFormatException	if the result is not a reduced 
	 * equation or expression
	 */
	private int areEquivalent(String exprnOrEqn1, String exprnOrEqn2,
			String constants) 
			throws ProcessExecutionException, EquationFormatException {
		return (isEquation(exprnOrEqn2) 
				? areEquivalentEquations(exprnOrEqn1, exprnOrEqn2, constants)
				: areEquivalentExpressions(exprnOrEqn1, exprnOrEqn2, 
					constants));
	} // areEquivalent(String, String, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[EQN_IS]; } 
	/** Gets which equation to compare. 
	 * @return	which equation to compare
	 */
	public int getWhich() 					{ return which; } 
	/** Sets which equation to compare. 
	 * @param	wh	which equation to compare
	 */
	public void setWhich(int wh) 			{ which = wh; } 
	/** Gets whether the evaluator is satisfied by a match or a mismatch.
	 * @return	true if the evaluator is satisfied by a match
	 */
	public boolean getIsPositive() 			{ return isPositive; } 
	/** Sets whether the evaluator is satisfied by a match or a mismatch.
	 * @param	pos	true if the evaluator is satisfied by a match
	 */
	public void setIsPositive(boolean pos)	{ isPositive = pos; } 
	/** Sets the value of the author's equation. 
	 * @param	eqn	the author's equation
	 */
	public void setMolName(String eqn) 		{ authEqn = eqn; }

} // EqnIs
