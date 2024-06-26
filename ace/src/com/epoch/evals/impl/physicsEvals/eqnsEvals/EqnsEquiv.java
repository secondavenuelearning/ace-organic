package com.epoch.evals.impl.physicsEvals.eqnsEvals;

import com.epoch.exceptions.EquationFormatException;
import com.epoch.exceptions.ProcessExecutionException;
import com.epoch.physics.EquationFunctions;
import com.epoch.utils.Utils;

/** Contains methods for evaluating equation or expression equivalencies. */
class EqnsEquiv extends EqnsUtils {

	private static void debugPrint(Object... msg) {
		 Utils.printToLog(msg);
	}

	/** How many response solutions must be present for a match. */
	protected int howManySolutions;
		/** Value of howManySolutions. */
		public static int ONE = 0;
		/** Value of howManySolutions. */
		public static int ALL = 1;
		/** Database values for howManySolutions. */
		public static String[] HOW_MANY_SOLNS_DB_VALUES = new String[]
				{"one", "all"};

	/** Determines whether two equations are equivalent.  We assume that the 
	 * left side of each equation in solvedEqnsStr is the same single variable.
	 * @param	respEqn	an equation
	 * @param	solvedEqnsStr	comma-separated equations solved for the same 
	 * single variable
	 * @param	constants	the given constants
	 * @return	the number of the matching author's equation if the equations 
	 * are equivalent
	 * @throws	ProcessExecutionException	if the process can't be executed
	 * properly
	 * @throws	EquationFormatException	if the result is not a reduced 
	 * equation
	 */
	int areEquivalentEquations(String respEqn, String solvedEqnsStr,
			String constants) 
			throws ProcessExecutionException, EquationFormatException {
		final String SELF = "EqnsEquiv.areEquivalentEquations: ";
		debugPrint(SELF + "comparing response equation:\n",
				respEqn, "\nto solved equation(s):\n", solvedEqnsStr);
		final String[] solvedEqns = splitTrim(solvedEqnsStr);
		final String variable = getEqnHalves(solvedEqns[0])[0];
		final String calcn = Utils.toString(Utils.isEmpty(constants) ? ""
					: Utils.getBuilder(constants, "; "),
				"solve((", respEqn, "), ", variable, ')');
		debugPrint(SELF + "reducing expression:\n", calcn);
		final String[] reducedExprns = EquationFunctions.reduce(calcn);
		debugPrint(SELF + "got ", reducedExprns.length, 
				" reduced expression(s): ", reducedExprns);
		int equivEqnNum = 0; 
		if (!Utils.membersAreEmpty(reducedExprns)) {
			int eqnNum = 1;
			for (final String solvedEqn : solvedEqns) {
				final String[] authParts = getEqnHalves(solvedEqn);
				boolean equiv = false;
				// if ANY solution is equivalent, the equations are equivalent
				for (final String reducedExprn : reducedExprns) {
					final String[] respParts = getEqnHalves(reducedExprn);
					equiv = areEquivalentExpressions(authParts[1], respParts[1],
							constants) > 0;
					if (equiv) {
						debugPrint(SELF + "equation: ", solvedEqn, 
								"\nand equation: ", reducedExprn, 
								"\nare equivalent.");
						equivEqnNum = eqnNum;
						break; 
					} else debugPrint(SELF + "equation: ", solvedEqn, 
							"\nand equation: ", reducedExprn, 
							"\nare not equivalent.");
				} // for each reduced expression
				if (equiv) break;
				eqnNum++;
			} // for each equation of the author
		} // if response equation could be reduced
		return equivEqnNum;
	} // areEquivalentEquations(String, String, String)

	/** Determines whether two expressions (not equations) are equivalent.
	 * @param	exprn1	one expression or equation
	 * @param	exprn2	the other expression or equation
	 * @param	constants	the given constants
	 * @return	1 if the expressions are equivalent, 0 otherwise
	 * @throws	ProcessExecutionException	if the process can't be executed
	 * properly
	 * @throws	EquationFormatException	if the result is not a reduced
	 * expression
	 */
	int areEquivalentExpressions(String exprn1, String exprn2, String constants) 
			throws ProcessExecutionException, EquationFormatException {
		final String SELF = "EqnsEquiv.areEquivalentExpressions: ";
		debugPrint(SELF + "comparing response expression:\n",
				exprn1, "\nto expression:\n", exprn2, "\nwith constants: ", 
				constants);
		final String calcn = Utils.toString(
				Utils.isEmpty(constants) ? ""
					: Utils.getBuilder(constants, "; "),
				"factor((", exprn2, ") - (", exprn1, "))");
		debugPrint(SELF + "reducing expression:\n", calcn);
		final String[] reducedExprns = EquationFunctions.reduce(calcn);
		debugPrint(SELF + "got reduced expression: ", reducedExprns);
		int eqnSatisfied = 0;
		if (!Utils.isEmpty(reducedExprns)) {
			final String reducedExprn = reducedExprns[0].trim();
			final String[] values = reducedExprn.split("\\s+");
			debugPrint(SELF + "values are: ", values);
			eqnSatisfied = ("0".equals(values[values.length - 1]) ? 1 : 0);
		} // if there are solutions
		return eqnSatisfied;
	} // areEquivalentExpressions(String, String, String)

	/** Gets how many response solutions must be present for a match. 
	 * @return	how many response solutions must be present for a match
	 */
	public int getHowManySolutions() 			{ return howManySolutions; } 
	/** Sets how many response solutions must be present for a match. 
	 * @param	hms	how many response solutions must be present for a match
	 */
	public void setHowManySolutions(int hms) 	{ howManySolutions = hms; } 
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 				{ return false; }

} // EqnsEquiv
