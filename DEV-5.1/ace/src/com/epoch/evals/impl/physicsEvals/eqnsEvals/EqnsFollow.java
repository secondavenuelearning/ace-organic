package com.epoch.evals.impl.physicsEvals.eqnsEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.EquationFormatException;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.ProcessExecutionException;
import com.epoch.physics.EquationFunctions;
import com.epoch.physics.Equations;
import com.epoch.physics.physicsConstants.EquationsConstants;
import com.epoch.responses.Response;
import com.epoch.utils.Utils;

/** If {each equation or expression is, any equation or expression is not} 
 * equivalent to the next equation or expression... */ 
public class EqnsFollow extends EqnsEquiv 
		implements EvalInterface, EquationsConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether every equation or expression should equal the next, or not. */
	private boolean isPositive;

	/** Constructor. */
	public EqnsFollow() {
		isPositive = false;
	} // EqnsFollow()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public EqnsFollow(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		isPositive = Utils.isPositive(splitData[0]);
	} // EqnsFollow(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>isPositive</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return (isPositive ? "Y" : "N");
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
		return Utils.toString("If ", isPositive ? "every" : "any",
				" equation or expression in the response is ",
				isPositive ? "" : "not ",
				"equivalent to the next equation or expression");
	} // toEnglish()

	/** Determines whether each mathematical equation or expression 
	 * of the response equals the next one.
	 * @param	response	a parsed response
	 * @param	authString	null; required by interface
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) {
		final String SELF = "EqnsFollow.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		final Equations resp = (Equations) response.parsedResp;
		evalResult.isSatisfied = isPositive;
		try {
			int eqnProperty = resp.getStoredEntryProperty();
			if (resp.getStoredEntryProperty() == UNCALCULATED) {
				debugPrint(SELF + "logicality of equation sequence needs "
						+ "to be calculated.");
				calculateLogicality(resp);
				eqnProperty = resp.getStoredEntryProperty();
			} else debugPrint(SELF + "logicality of equation sequence has "
					+ "already been calculated.");
			final int entryNum = resp.getStoredEntryNum();
			if (eqnProperty == INCOMPARABLE) {
				debugPrint(SELF + "eqnProperty is INCOMPARABLE, entryNum = ",
						entryNum);
				evalResult.autoFeedback = new String[] {
						"ACE cannot compare entries ***1*** and ***2*** to see "
						+ "if they are equivalent because one is an equation "
						+ "(with an equals sign) and the other is an "
						+ "expression (without one)."};
				evalResult.autoFeedbackVariableParts = new String[] 
						{String.valueOf(entryNum - 1), 
						String.valueOf(entryNum)};
				evalResult.verificationFailureString = ""; // not null
				evalResult.isSatisfied = false;
			} else if (eqnProperty == FORMAT_EXCEPTION) {
				debugPrint(SELF + "eqnProperty is FORMAT_EXCEPTION, "
						+ "entryNum = ", entryNum);
				evalResult.autoFeedback = new String[] {
		 				EquationFormatException.getStandardMessage()};
				evalResult.autoFeedbackVariableParts = new String[] 
						{Utils.toString("<p>", resp.getEntry(entryNum), 
						"</p>")};
				evalResult.verificationFailureString = ""; // not null
				evalResult.isSatisfied = false;
			} else if (entryNum > 0) {
				debugPrint(SELF + "eqnProperty is DOESNT_FOLLOW, entryNum = ",
						entryNum);
				evalResult.isSatisfied = !isPositive;
				if (evalResult.isSatisfied) {
					final boolean isEqn = 
							isEquation(resp.getEntry(entryNum));
					evalResult.autoFeedback = new String[] {Utils.toString(
							isEqn ? "Equation" : "Expression",
							" ***2*** does not follow logically from ",
							isEqn ? "equation" : "expression",
							" ***1***.")};
					evalResult.autoFeedbackVariableParts = new String[] 
							{String.valueOf(entryNum), 
							String.valueOf(entryNum - 1)};
				} // if any equation is not equal to the next
			} else debugPrint(SELF + "equations follow logically");
			if (evalResult.isSatisfied && isPositive) {
				evalResult.autoFeedback = new String[] 
						{"Every entry is equivalent to the next."};
			} // if all equations are equivalent
		} catch (ProcessExecutionException e) {
			Utils.alwaysPrint(SELF + "ProcessExecutionException: ", 
					e.getMessage());
			evalResult.verificationFailureString = "A process execution "
					+ "error occurred when ACE tried to evaluate a "
					+ "mathematical expression. Please report this error "
					+ "to the programmers.";
			evalResult.isSatisfied = false;
		} // try
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Calculates whether each equation or expression follows logically from 
	 * the previous one, and stores the result in the response.
	 * @param	resp	the response
	 * @throws	ProcessExecutionException	if the equation-solving program 
	 * isn't executing properly
	 */
	private void calculateLogicality(Equations resp) 
			throws ProcessExecutionException {
		final String SELF = "EqnsFollow.calculateLogicality: ";
		final String[] respEntries = resp.getFormattedEntries();
		final String constants = resp.getFormattedConstants();
		resp.storeEntryPropertyAndNum(DOESNT_FOLLOW, 0);
		for (int entryNum = 1; entryNum < respEntries.length; entryNum++) {
			final int nextEntryNum = entryNum + 1;
			final String respEntry = respEntries[entryNum - 1];
			final String nextRespEntry = respEntries[nextEntryNum - 1];
			if (!areComparable(respEntry, nextRespEntry)) {
				debugPrint(SELF + "response entries ", entryNum, ": ", 
						respEntry, "\nand ", nextEntryNum, ": ", nextRespEntry, 
						"\nare not both expressions or both equations.");
				resp.storeEntryPropertyAndNum(INCOMPARABLE, nextEntryNum);
				break;
			} else try {
				final boolean equiv = 
						areEquivalent(respEntry, nextRespEntry, constants);
				debugPrint(SELF + "response entries ", entryNum, ": ", 
						respEntry, "\nand ", nextEntryNum, ": ", nextRespEntry, 
						"\nare ", equiv ? "" : "not ", "equivalent.");
				if (!equiv) {
					resp.storeEntryPropertyAndNum(DOESNT_FOLLOW, nextEntryNum);
					break;
				} // if any pair don't match
			} catch (EquationFormatException e) {
				try {
					EquationFunctions.isValidExpression(respEntry);
					resp.storeEntryPropertyAndNum(FORMAT_EXCEPTION, 
							nextEntryNum);
				} catch (EquationFormatException e2) {
					resp.storeEntryPropertyAndNum(FORMAT_EXCEPTION, entryNum);
				} // try
				break;
			} // try
		} // for each pair of equations or expressions
	} // calculateLogicality(Equations)

	/** Determines whether two math entries are equivalent.
	 * @param	entry1	one entry
	 * @param	entry2	the other entry
	 * @param	constants	the given constants
	 * @return	the number of the matching author's entry if the entries are 
	 * equivalent
	 * @throws	ProcessExecutionException	if the equation-solving program 
	 * isn't executing properly
	 * @throws	EquationFormatException	if the result is not a solved 
	 * equation or expression
	 */
	private boolean areEquivalent(String entry1, String entry2,
			String constants) 
			throws ProcessExecutionException, EquationFormatException {
		return (isEquation(entry2) 
				? areEquivalentRespEquations(entry1, entry2, constants)
				: areEquivalentExpressions(entry1, entry2, constants) > 0);
	} // areEquivalent(String, String, String)

	/** Determines whether two entries of the response that contain equations
	 * are equivalent.
	 * We need to isolate a variable in one equation before sending both
	 * equations to areEquivalentEquations().  We first split entry 2 into
	 * its ocnstituent equations.  We then try to reduce each
	 * equation 2 without specifying a variable.  If that fails, we find which
	 * of the variables identified by Maxima we can use to solve equation 2 for
	 * that variable.  We then submit the equation 1 and the solved equation 2 
	 * to areEquivalentEquations().
	 * @param	respEqnsStr1	one equation
	 * @param	respEqnsStr2	another equation
	 * @param	constants	the given constants
	 * @return	true if the equations are equivalent
	 * @throws	ProcessExecutionException	if the equation-solving program 
	 * isn't executing properly
	 * properly
	 * @throws	EquationFormatException	if the result is not a solved 
	 * equation
	 */
	private boolean areEquivalentRespEquations(String respEqnsStr1, 
			String respEqnsStr2, String constants) 
			throws ProcessExecutionException, EquationFormatException {
		final String SELF = "EqnsFollow.areEquivalentRespEquations: ";
		debugPrint(SELF + "comparing entire entry:\n", respEqnsStr1, 
				"\nto prior entry:\n", respEqnsStr2);
		final String[] respEqn2s = splitTrim(respEqnsStr2);
		boolean equiv = false; 
		for (final String respEqn2 : respEqn2s) {
			debugPrint(SELF + "comparing individual equation:\n", respEqn2, 
					"\nto prior entry:\n", respEqnsStr1);
			String calcn = Utils.toString(Utils.isEmpty(constants) ? ""
						: Utils.getBuilder(constants, "; "),
					"solve(", respEqn2, ')');
			debugPrint(SELF + "submitting to Maxima expression:\n", calcn);
			String solvedRespEqn2sStr = EquationFunctions.execute(calcn);
			debugPrint(SELF + "got solved equation(s): ", solvedRespEqn2sStr);
			final boolean multipleVars = isMultivariable(solvedRespEqn2sStr);
			// clip off any constants from start of results string
			int firstBracket = solvedRespEqn2sStr.indexOf('[');
			solvedRespEqn2sStr = solvedRespEqn2sStr.substring(firstBracket);
			if (multipleVars) {
				final int endBracket = solvedRespEqn2sStr.indexOf(']');
				solvedRespEqn2sStr = 
						solvedRespEqn2sStr.substring(1, endBracket);
				debugPrint(SELF + "too many variables to solve expression: ", 
						solvedRespEqn2sStr);
				final String[] vars = solvedRespEqn2sStr.split(",");
				final StringBuilder calcnBld = Utils.getBuilder(
						Utils.isEmpty(constants) ? ""
							: Utils.getBuilder(constants, "; "),
						"solve(", respEqn2, ", ");
				// solve for variables from left to right, opposite of list
				for (int varNum = vars.length; varNum > 0; varNum--) {
					calcn = Utils.toString(calcnBld, vars[varNum - 1], ')');
					debugPrint(SELF + "submitting to Maxima expression:\n", 
							calcn);
					solvedRespEqn2sStr = EquationFunctions.execute(calcn);
					if (!"[]".equals(solvedRespEqn2sStr.trim())) {
						debugPrint(SELF + "got solved equation: ", 
								solvedRespEqn2sStr);
						// clip off any constants from start of results string
						firstBracket = solvedRespEqn2sStr.indexOf('[');
						solvedRespEqn2sStr = 
								solvedRespEqn2sStr.substring(firstBracket);
						break;
					} else debugPrint(SELF + "could not solve expression.");
				} // for each variable
			} // if equation wasn't single-variable
			final int endBracket = solvedRespEqn2sStr.lastIndexOf(']');
			solvedRespEqn2sStr = solvedRespEqn2sStr.substring(1, endBracket); 
			if (Utils.isEmpty(solvedRespEqn2sStr)) {
				throw new EquationFormatException("The following equation:"
						+ "***1***could not be solved for a single variable.", 
						respEqn2);
			} // if could not solve equation
			final String[] solvedRespEqn2s = solvedRespEqn2sStr.split(",");
			debugPrint(SELF + "individual response equation: ", respEqn2, 
					"\ncan be solved for a single variable: ", solvedRespEqn2s);
			// if ANY solution is equivalent, the equations are equivalent
			for (final String solvedRespEqn2 : solvedRespEqn2s) {
				equiv = areEquivalentEquations(respEqnsStr1, 
						solvedRespEqn2.trim(), constants) > 0;
				if (equiv) {
					debugPrint(SELF + "solved form:\n", solvedRespEqn2, 
							"\nof individual equation: ", respEqn2,
							"\nof entry: ", respEqnsStr2,
							"\nmatches prior entry:\n", respEqnsStr1);
					break; 
				} // if equiv
			} // for each solved expression
			if (!equiv) {
				debugPrint(SELF + "no solutions of individual equation:\n", 
						respEqn2, "\nof entry: ", respEqnsStr2,
						"\nmatch prior entry:\n", respEqnsStr1);
				break;
			} // if equiv
		} // for each equation in entry 2
		if (equiv) debugPrint(SELF + "every equation in response entry: ",
				respEqnsStr2, "\nmatches at least one solution to prior "
				+ "entry: ", respEqnsStr1);
		else debugPrint(SELF + "at least one equation in response entry: ",
				respEqnsStr2, "\ndoesn't match at least one solution to prior "
				+ "entry: ", respEqnsStr1);
		return equiv;
	} // areEquivalentRespEquations(String, String, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[EQNS_FOLLOW]; } 
	/** Gets whether the evaluator is satisfied by a match or a mismatch.
	 * @return	true if the evaluator is satisfied by a match
	 */
	public boolean getIsPositive() 			{ return isPositive; } 
	/** Sets whether the evaluator is satisfied by a match or a mismatch.
	 * @param	pos	true if the evaluator is satisfied by a match
	 */
	public void setIsPositive(boolean pos)	{ isPositive = pos; } 
	/** Not used.  Required by interface. 
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName)	{ /* intentionally empty */ }

} // EqnsFollow
