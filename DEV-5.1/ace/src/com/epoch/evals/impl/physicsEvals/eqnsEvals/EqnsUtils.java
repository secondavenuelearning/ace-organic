package com.epoch.evals.impl.physicsEvals.eqnsEvals;

import com.epoch.exceptions.EquationFormatException;
import com.epoch.physics.EquationFunctions;
import com.epoch.utils.Utils;

/** Contains utilities for evaluating equations or expressions. */
public class EqnsUtils {

	/** Determines whether two expressions or equations are comparable.
	 * @param	exprnOrEqn1	one expression or equation
	 * @param	exprnOrEqn2	the other expression or equation
	 * @return	true if both are expressions or both are equations
	 */
	static boolean areComparable(String exprnOrEqn1, String exprnOrEqn2) {
		return isEquation(exprnOrEqn1) == isEquation(exprnOrEqn2);
	} // areComparable(String, String)

	/** Splits the individual equations or expressions in a comma-separated list.
	 * @param	eqnsStr	comma-separated list of equations or expressions
	 * @return	array of individual equations or expressions
	 */
	static String[] splitTrim(String eqnsStr) {
		return Utils.splitTrim(eqnsStr, ",");
	} // splitTrim(String)

	/** Gets the left and right sides of an equation.
	 * @param	eqn	an equation
	 * @return	the left and right sides of the equation
	 * @throws	EquationFormatException	if the equation has more than one equals
	 * sign
	 */
	static String[] getEqnHalves(String eqn) throws EquationFormatException {
		final String[] parts = eqn.split("=");
		parts[0] = parts[0].trim();
		if (parts.length > 2) {
			final String msg = "ACE is unable to analyze the mathematical "
					+ "equation: ***expression*** because it contains "
					+ "more than one equal sign.";
			final String eqnOut = Utils.toString("<p>", eqn, "</p>");
			throw new EquationFormatException(msg, eqnOut);
		} // if there is more than one = sign in the equation
		if (parts.length > 1) parts[1] = parts[1].trim();
		return parts;
	} // getEqnHalves(String)

	/** Determines whether an expression or equation is an equation.
	 * @param	exprnOrEqn	an expression or equation
	 * @return	true if the expression or equation contains an equal sign
	 */
	static boolean isEquation(String exprnOrEqn) {
		return EquationFunctions.isEquation(exprnOrEqn);
	} // isEquation(String)

	/** Gets whether Maxima failed to solve an equation because there were more
	 * variables than equations.
	 * @param	solvedEqn	result from Maxima
	 * @return true if there were more variables than equations
	 */
	static boolean isMultivariable(String solvedEqn) {
		return EquationFunctions.isMultivariable(solvedEqn);
	} // isMultivariable(String)

	/** Extracts the variables from an equation or expression.
	 * @param	origEqn	the equation or expression
	 * @return	array of names of variables
	 */
	static String[] getVariables(String origEqn) {
		return EquationFunctions.getVariables(origEqn);
	} // getVariables(String)

	/** Extracts the variables from an equation or expression.
	 * @param	origEqn	the equation or expression
	 * @param	constants	constants associated with the equation or
	 * expression, already formatted for submission to Maxima
	 * @return	array of names of variables
	 */
	static String[] getVariables(String origEqn, String constants) {
		return EquationFunctions.getVariables(origEqn, constants);
	} // getVariables(String, String)

} // EqnsUtils
