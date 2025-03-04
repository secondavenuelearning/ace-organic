package com.epoch.evals.impl.physicsEvals.eqnsEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.EquationFormatException;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.ProcessExecutionException;
import com.epoch.physics.Equations;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** If the last equation in the response {is, is not} solved for a variable...
 */
public class EqnSolved extends EqnsEquiv implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether the last equation should or should not be solved for a single
	 * variable for this evaluator to be satisfied. */
	private boolean isPositive;
	/** Whether the other side of the equation must be reduced to a single
	 * number. */
	private boolean mustBeReduced;
	/** The variable for which the equation should be solved. */
	private String variable = null;
	/** The author's unit. */
	// final private String authUnit = null;
	/** The extent to which the response unit must equal the author's unit. */
	// private int unitAgreement;
		/** Value of unitAgreement. */
		// final public static int UNIT_EXACTLY = 0;
		/** Value of unitAgreement. */
		// final public static int UNIT_EQUIVALENT = 1;
		/** Value of unitAgreement. */
		// final public static int UNIT_DISREGARD = 2;

	/** Constructor. */
	public EqnSolved() {
		isPositive = false;
		mustBeReduced = false;
		variable = "x";
		// unitAgreement = UNIT_EXACTLY;
	} // EqnSolved()

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>mustBeReduced</code>/<code>variable</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public EqnSolved(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 3)  {
			isPositive = Utils.isPositive(splitData[0]);
			mustBeReduced = Utils.isPositive(splitData[1]);
			variable = splitData[2];
			// if (splitData.length >= 4) unitAgreement = MathUtils.parseInt(splitData[3]);
		} 
		if (splitData.length < 3) {
			throw new ParameterException("EqnSolved ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // EqnSolved(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>isPositive</code>/<code>mustBeReduced</code>/<code>variable</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive ? "Y/" : "N/",
				mustBeReduced ? "Y/" : "N/", variable);
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
		if (!isPositive) words.append("not ");
		words.append("all of the equations in the last entry of the response are "); 
		if (mustBeReduced) words.append("both ");
		words.append("solved for ");
		Utils.addSpanString(words, variable, !Utils.TO_DISPLAY);
		if (mustBeReduced) {
			words.append(" and reduced to a single number");
		} // if equation must be reduced
		return words.toString();
	} // toEnglish()

	/** Determines whether the last mathematical equation is solved for a
	 * variable.
	 * @param	response	a parsed response
	 * @param	authString	the author's string (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing 
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) {
		final String SELF = "EqnSolved.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		final Equations resp = (Equations) response.parsedResp;
		final String lastRespEntry = 
				resp.getFormattedLastEntry(Equations.OMIT_FACTORS);
		final String[] respEqns = splitTrim(lastRespEntry);
		for (final String respEqn : respEqns) {
			boolean isSolved = false;
			if (!isEquation(respEqn)) {
				evalResult.autoFeedback = new String[] {
						"The expression:***1***is not an equation."};
				evalResult.autoFeedbackVariableParts = 
						new String[] {Utils.toString("<p>", respEqn, "</p>")};
				evalResult.verificationFailureString = ""; // not null
			} else try {
				final String[] respEqnHalves = getEqnHalves(respEqn);
				final boolean leftIsVar = variable.equals(respEqnHalves[0]);
				final boolean rightIsVar = variable.equals(respEqnHalves[1]);
				isSolved = leftIsVar || rightIsVar;
				debugPrint(SELF + "respEqn: ", respEqn, "\nis ",
						isSolved ? "" : "not ", "solved for ", variable);
				if (isSolved && mustBeReduced) {
					final int numHalf = (leftIsVar ? 1 : 0);
					String constantsStr = resp.getFormattedConstants();
					if (constantsStr != null) {
						constantsStr = constantsStr.replace(':', '=');
					} // if there are constants
					isSolved = isReduced(respEqnHalves[numHalf], constantsStr);
				} // if numerical side must be reduced to single number
			} catch (EquationFormatException e) {
				debugPrint(SELF + "EquationFormatException: ", 
						e.getMessage(), "\n", e.getEquation());
				evalResult.autoFeedback = new String[] {e.getMessage()};
				evalResult.autoFeedbackVariableParts = new String[] 
						{Utils.toString("<p>", respEqn, "</p>")};
				evalResult.verificationFailureString = ""; // not null
				break;
			} catch (ProcessExecutionException e) {
				Utils.alwaysPrint(SELF + "ProcessExecutionException: ", 
						e.getMessage());
				evalResult.verificationFailureString = "A process execution "
						+ "error occurred when ACE tried to evaluate a "
						+ "mathematical expression. Please report this error "
						+ "to the programmers.";
				break;
			} // try
			evalResult.isSatisfied = isSolved == isPositive;
			debugPrint(SELF + "isSolved = ", isSolved, ", isPositive = ",
					isPositive, ", isSatisfied = ", evalResult.isSatisfied);
			if (!isSolved) break;
		} // for each response
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Determines whether an expression that is a solution to a variable is 
	 * just a number, with or without units.
	 * @param	soln	the expression
	 * @param	constantsStr	constants used to solve the equation
	 * @return	true if the expression is just a number or is a number plus
	 * known units
	 * @throws	ProcessExecutionException	if the process can't be executed
	 * properly
	 * @throws	EquationFormatException	if the result is not a reduced
	 * expression
	 */
	private boolean isReduced(String soln, String constantsStr) 
			throws EquationFormatException, ProcessExecutionException {
		final String SELF = "EqnSolved.isReduced: ";
		boolean isReduced = MathUtils.isDouble(soln);
		debugPrint(SELF + "soln: ", soln, "\nconstants: ", constantsStr);
		if (!isReduced) { // may contain units
			int operatorPosn = soln.indexOf('*');
			if (operatorPosn < 0) operatorPosn = soln.indexOf('/');
			if (operatorPosn >= 0) {
				debugPrint(SELF + "solved expression ", soln, 
						" may contain units.");
				final String[] constantEqns = 
						Utils.splitTrim(constantsStr, ";");
				final List<String> constantsUnits = new ArrayList<String>();
				for (final String constantEqn : constantEqns) {
					final String[] constantEqnHalves =
							getEqnHalves(constantEqn);
					final String[] constantUnits = 
							getVariables(constantEqnHalves[1]);
					debugPrint(SELF + "constant equation ", constantEqn,
							" has unit(s) ", constantUnits);
					for (final String unit : constantUnits) {
						if (!constantsUnits.contains(unit)) {
							constantsUnits.add(unit);
						} // if unit isn't already in list
					} // for each unit in the constant equation
				} // for each equation defining a constant
				final StringBuilder unitsBld = new StringBuilder();
				for (final String unit : constantsUnits) {
					if (unitsBld.length() > 0) unitsBld.append("; ");
					Utils.appendTo(unitsBld, unit, ": 1");
				} // for each known unit
				final String possibleUnitsStr =
						soln.substring(operatorPosn + 1).trim();
				debugPrint(SELF + "evaluating expression ", possibleUnitsStr,
						" with known unit(s) from constants: ", 
						constantsUnits);
				isReduced = areEquivalentExpressions(possibleUnitsStr, "1", 
						unitsBld.toString()) > 0;
			} // if there is an operator
		} // if not just a number
		debugPrint(SELF + "expression is ", isReduced ? "" : "not ",
				"reduced to a single number.");
		return isReduced;
	} // isReduced(String, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[EQN_SOLVED]; } 
	/** Gets whether the evaluator is satisfied by a match or a mismatch.
	 * @return	true if the evaluator is satisfied by a match
	 */
	public boolean getIsPositive() 				{ return isPositive; } 
	/** Sets whether the evaluator is satisfied by a match or a mismatch.
	 * @param	pos	true if the evaluator is satisfied by a match
	 */
	public void setIsPositive(boolean pos)		{ isPositive = pos; } 
	/** Gets whether the other side of the equation must be reduced to a 
	 * single number.
	 * @return	true if the other side of the equation must be reduced to a
	 * single number
	 */
	public boolean getMustBeReduced() 			{ return mustBeReduced; } 
	/** Sets whether the other side of the equation must be reduced to a 
	 * single number.
	 * @param	mbr	true if the other side of the equation must be reduced to a
	 * single number
	 */
	public void setMustBeReduced(boolean mbr)	{ mustBeReduced = mbr; } 
	/** Gets the variable for variable the equation must be solved. 
	 * @return	the variable for variable the equation must be solved
	 */
	public String getVariable() 				{ return variable; } 
	/** Sets the variable for variable the equation must be solved. 
	 * @param	var	the variable for variable the equation must be solved
	 */
	public void setVariable(String var) 		{ variable = var; } 
	/** Not used.  Required by interface. 
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName)		{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 				{ return false; }

} // EqnSolved
