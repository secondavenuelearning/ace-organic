package com.epoch.physics;

import com.epoch.db.CanonicalizedUnitRW;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.EquationFormatException;
import com.epoch.exceptions.ProcessExecutionException;
import com.epoch.physics.physicsConstants.CanonicalizedUnitConstants;
import com.epoch.physics.physicsConstants.EquationFunctionsConstants;
import com.epoch.utils.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Functions that use Maxima to evaluate mathematical equations and
 * expressions. */
final public class EquationFunctions 
		implements CanonicalizedUnitConstants, EquationFunctionsConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Determines whether an expression (or equation) is valid.
	 * @param	exprn	the expression
	 * @throws	ProcessExecutionException	if the command can't be executed
	 * properly
	 * @throws	EquationFormatException	if the equation's format is bad
	 */
	public static void isValidExpression(String exprn) 
			throws ProcessExecutionException, EquationFormatException {
		isValidExpression(exprn, !Equations.SAVE_EQN);
	} // isValidExpression(String)

	/** Determines whether an expression (or equation) is valid.
	 * @param	exprn	the expression
	 * @param	saveEqnInException	when true and the equation is malformed, 
	 * make a new error message and throw out the one from Maxima
	 * @throws	ProcessExecutionException	if the command can't be executed
	 * properly
	 * @throws	EquationFormatException	if the equation's format is bad
	 */
	public static void isValidExpression(String exprn, 
			boolean saveEqnInException) 
			throws ProcessExecutionException, EquationFormatException {
		final String SELF = "EquationFunctions.isValidExpression: ";
		debugPrint(SELF + "checking validity of expression:\n", exprn);
		final String calcn = Utils.toString("factor(",
				explicitizeMultiplication(exprn), ')');
		try {
			reduce(calcn, false); // ignore return value
		} catch (EquationFormatException e) {
			e.printStackTrace();
			throw (saveEqnInException 
					? new EquationFormatException(exprn) : e);
		} // try
	} // isValidExpression(String, boolean)

	/** Extracts the Maxima solution(s) from the output string.
	 * @param	calcn	the maxima calculation to run
	 * @return	one or more results of the maxima calculation
	 * @throws	ProcessExecutionException	if the command can't be executed
	 * properly
	 * @throws	EquationFormatException	if the result is not a reduced equation or
	 * expression
	 */
	public static String[] reduce(String calcn) 
			throws ProcessExecutionException, EquationFormatException {
		return reduce(calcn, true);
	} // reduce(String)

	/** Extracts the Maxima solution(s) from the output string.
	 * @param	calcn	the maxima calculation to run
	 * @param	saveEqnInException	when true and the equation is malformed, 
	 * make a new error message and throw out the one from Maxima
	 * @return	one or more results of the maxima calculation
	 * @throws	ProcessExecutionException	if the command can't be executed
	 * properly
	 * @throws	EquationFormatException	if the result is not a reduced equation or
	 * expression
	 */
	public static String[] reduce(String calcn, boolean saveEqnInException) 
			throws ProcessExecutionException, EquationFormatException {
		final String SELF = "EquationFunctions.reduce: ";
		final String output = execute(calcn, saveEqnInException);
		final int start = output.indexOf('[') + 1;
		final int end = output.lastIndexOf(']');
		final String reductionStr = output.substring(start, 
				end == -1 ? output.length() : end);
		return reductionStr.split(",");
	} // reduce(String, boolean)

	/** Converts an equation in Maxima format to TeX.
	 * @param	eqn	the equation
	 * @return	the equation in TeX format
	 */
	public static String toTeX(String eqn) {
		final String SELF = "EquationFunctions.toTeX: ";
		String texEqn = substitute(eqn, EXTEND_SUPER_SUB);
		try {
			final String calcn = Utils.toString("tex(", eqn, ')');
			final String output = execute(calcn).trim();
			if (output.startsWith("$$") && output.endsWith("$$")) {
				texEqn = output.substring(2, output.length() - 2);
			} // if boundaries have been found
		} catch (EquationFormatException e) {
			debugPrint(SELF + "EquationFormatException");
		} catch (ProcessExecutionException e) {
			debugPrint(SELF + "ProcessExecutionException");
		} // try
		debugPrint(SELF + "reformatting equation: ", eqn, "\nto: ", texEqn);
		return texEqn;
	} // toTeX(String)

	/** Uses Maxima to reduce an expression or to solve an equation.  May give 
	 * more than one solution.
	 * @param	calcn	the maxima calculation to run
	 * @return	one or more results of the maxima calculation
	 * @throws	ProcessExecutionException	if the command can't be executed
	 * properly
	 * @throws	EquationFormatException	if the result is not a reduced equation or
	 * expression
	 */
	public static String execute(String calcn) 
			throws ProcessExecutionException, EquationFormatException {
		return execute(calcn, true);
	} // execute(String)

	/** Uses Maxima to reduce an expression or to solve an equation.  May give 
	 * more than one solution.
	 * @param	calcn	the maxima calculation to run
	 * @param	saveEqnInException	when true and the equation is malformed, make a 
	 * new error message and throw out the one from Maxima
	 * @return	one or more results of the maxima calculation
	 * @throws	ProcessExecutionException	if the command can't be executed
	 * properly
	 * @throws	EquationFormatException	if the result is not a reduced equation or
	 * expression
	 */
	public static String execute(String calcn, boolean saveEqnInException) 
			throws ProcessExecutionException, EquationFormatException {
		final String SELF = "EquationFunctions.execute: ";
		String output = "";
		try {
			final Maxima maxima = Maxima.getMaxima();
			output = maxima.evaluate(calcn); 
			debugPrint(SELF + "input:\n", calcn, "\nfull output:\n", output);
			if (output.indexOf("incorrect syntax") >= 0) {
				throw new EquationFormatException(
						saveEqnInException ? calcn : output);
			} // if Maxima did not return an equation or expression
		} catch (IOException e) {
			Utils.alwaysPrint(SELF + "IOException caught.");
			e.printStackTrace();
			throw new ProcessExecutionException(e.getMessage());
		} // try
		return output;
	} // execute(String, boolean)

/* ********************* Formatting and units methods *********************/

	/** Formats the expression for Maxima, with implicit multiplication made
	 * explicit, &micro; and &mu; converted to u, and units canonicalized,
	 * and log(x) changed to log(1.0*x).  
	 * @param	exprn	the expression
	 * @return	the expression formatted for Maxima
	 */
	public static String formatExpression(String exprn) {
		return formatExpression(exprn, new String[0], FULL_CANONICALZN);
	} // formatExpression(String)

	/** Formats the expression for Maxima, with implicit multiplication made
	 * explicit, &micro; and &mu; converted to u, and units canonicalized,
	 * and log(x) changed to log(1.0*x).  
	 * @param	exprn	the expression
	 * @param	canonicaliznExtent	to what extent to convert units to their 
	 * canonicalized form: FULL_CANONICALZN, OMIT_FACTORS, or NO_CANONICALZN
	 * @return	the expression formatted for Maxima
	 */
	public static String formatExpression(String exprn, 
			int canonicaliznExtent) {
		return formatExpression(exprn, new String[0], canonicaliznExtent);
	} // formatExpression(String, int)

	/** Formats the expression for Maxima, with implicit multiplication made
	 * explicit, &micro; and &mu; converted to u, and units canonicalized,
	 * and log(x) changed to log(1.0*x).  
	 * @param	exprn	the expression
	 * @param	variablesNotUnits	names of variables that should not be
	 * treated as units
	 * @return	the expression formatted for Maxima
	 */
	public static String formatExpression(String exprn, 
			String[] variablesNotUnits) {
		return formatExpression(exprn, variablesNotUnits, FULL_CANONICALZN);
	} // formatExpression(String, String[])

	/** Formats the expression for Maxima, with implicit multiplication made
	 * explicit, &micro; and &mu; converted to u, and units canonicalized.  
	 * @param	exprn	the expression
	 * @param	variablesNotUnits	names of variables that should not be
	 * treated as units
	 * @param	canonicaliznExtent	to what extent to convert units to their 
	 * canonicalized form: FULL_CANONICALZN, OMIT_FACTORS, or NO_CANONICALZN
	 * @return	the expression formatted for Maxima
	 */
	public static String formatExpression(String exprn, 
			String[] variablesNotUnits, int canonicaliznExtent) {
		final String SELF = "EquationFunctions.formatExpression: ";
		final String exprnMod = Utils.cersToUnicode(exprn);
		String formatted = makeLogInputRational(
				explicitizeMultiplication(exprnMod));
		if (canonicaliznExtent != NO_CANONICALZN) {
			final List<String> variablesList = new ArrayList<String>(
					Arrays.asList(getVariables(formatted)));
			for (final String variableNotUnit : variablesNotUnits) {
				variablesList.remove(variableNotUnit);
			} // for each variable
			final String[] variables = 
					variablesList.toArray(new String[variablesList.size()]);
			debugPrint(SELF + "variables in equation: ", exprn,
					"\nexcluding reserved names ", variablesNotUnits,
					" are ", variables);
			formatted = canonicalizeUnits(formatted, variables, 
					canonicaliznExtent);
		} // if canonicaliznExtent
		return formatted;
	} // formatExpression(String, String[], int)

	/** Explicitizes multiplication in the expression, and converts &micro; and
	 * &mu; to u.
	 * <p>If consecutive digits are preceded by beginning of line, open
	 * parenthesis, white space, comma, or semicolon and followed by zero or
	 * more space characters and then a letter or open parenthesis or beginning
	 * of a numerical character entity reference, then put an asterisk between 
	 * the digits and the letter or open parenthesis or character entity 
	 * reference.
	 * </p>
	 * @param	entry	the expression
	 * @return	the entry formatted for Maxima
	 */
	public static String explicitizeMultiplication(String entry) {
		final String SELF = "EquationFunctions.explicitizeMultiplication: ";
		debugPrint(SELF + "formatting ", entry);
		return (entry == null ? null : substitute(entry, MICRO, TIMES_SIGNS));
	} // explicitizeMultiplication(String)

	/** Changes log(x) to log(1.0*x) because Maxima won't evaluate the
	 * logarithms of integers.
	 * @param	entry	the expression
	 * @return	the entry formatted for Maxima
	 */
	public static String makeLogInputRational(String entry) {
		return entry.replaceAll("log\\(", "log(1.0*");
	} // makeLogInputRational(String)

	/** Replaces each variable that is recognized as a unit with the equivalent
	 * SI unit.
	 * @param	eqn	the equation or expression
	 * @param	variables	variables in the equation or expression that may be
	 * units
	 * @return	the equation or expression, with each unit replaced with the
	 * equivalent SI unit
	 */
	public static String canonicalizeUnits(String eqn, String[] variables) {
		return canonicalizeUnits(eqn, variables, FULL_CANONICALZN);
	} // canonicalizeUnits(String, String[])

	/** Replaces each variable that is recognized as a unit with the equivalent
	 * SI unit.
	 * @param	eqn	the equation or expression
	 * @param	variables	variables in the equation or expression that may be
	 * units
	 * @param	canonicaliznExtent	to what extent to convert units to their 
	 * canonicalized form
	 * @return	the equation or expression, with each unit replaced with the
	 * equivalent SI unit
	 */
	public static String canonicalizeUnits(String eqn, String[] variables,
			int canonicaliznExtent) {
		final String SELF = "EquationFunctions.canonicalizeUnits: ";
		String eqnMod = eqn;
		for (final String variable : variables) {
			final String[] baseUnitAndMagnitude = 
					separateMagnitudePrefix(variable);
			String unitSymbol = baseUnitAndMagnitude[0];
			try {
				final CanonicalizedUnit[] canonicalizedUnits = 
						CanonicalizedUnitRW.getUnits(
							Utils.unicodeToCERs(unitSymbol));
				final boolean haveCanonicalizedUnit = 
						!Utils.isEmpty(canonicalizedUnits);
				final boolean haveMagnitudePrefix = 
						baseUnitAndMagnitude.length > 1;
				if (haveCanonicalizedUnit || haveMagnitudePrefix) {
					if (haveCanonicalizedUnit) {
						unitSymbol = canonicalizedUnits[0].toFormula(
								canonicaliznExtent);
					} // if there is >= 1 stored unit that matches the variable
					final StringBuilder unitBld = Utils.getBuilder('(');
					if (haveMagnitudePrefix 
							&& canonicaliznExtent == FULL_CANONICALZN) {
						Utils.appendTo(unitBld, baseUnitAndMagnitude[1], " * ");
					} // if there's a magnitude prefix
					unitSymbol = Utils.toString(unitBld, unitSymbol, ')');
					debugPrint(SELF + "replacing ", variable, " with ", 
							unitSymbol);
					final String varRegEx = 
							Utils.toString("\\b", variable, "\\b");
					eqnMod = eqnMod.replaceAll(varRegEx, unitSymbol);
				} // if need to do a substitution
			} catch (DBException e) {
				Utils.alwaysPrint(SELF + "caught exception trying to "
						+ "canonicalize unit ", variable);
			} // try
		} // for each variable
		debugPrint(SELF + "converted: ", eqn, "\nto: ", eqnMod);
		return eqnMod;
	} // canonicalizeUnits(String, String[], int)

	/** Separates the magnitude prefix of a unit into the base unit and the
	 * power of ten.
	 * @param	unit	a unit that may have a magnitude prefix
	 * @return	the base unit and, if it had a magnitude prefix, the prefix's 
	 * power of ten 
	 */
	private static String[] separateMagnitudePrefix(String unit) {
		final String SELF = "EquationFunctions.separateMagnitudePrefix: ";
		String[] separated = new String[] {unit};
		if (unit != null && unit.length() > 1
				&& !Utils.contains(PREFIX_EXCEPTIONS, unit)) {
			final int unitLen = unit.length();
			for (final String[] prefixMagnitude : PREFIXES_MAGNITUDES) {
				final String prefix = prefixMagnitude[0];
				if (unit.startsWith(prefix)
						&& unitLen > prefix.length()) {
					separated = new String[]
							{unit.substring(prefix.length()),
							prefixMagnitude[1]};
					break;
				} // if unit begins with prefix
			} // for each prefix
		} // if unit might have a magnitude prefix
		return separated;
	} // separateMagnitudePrefix(String)

	/** Extracts the variables from an equation or expression.
	 * @param	origEqn	the equation or expression
	 * @return	array of names of variables
	 */
	public static String[] getVariables(String origEqn) {
		return getVariables(origEqn, null);
	} // getVariables(String)

	/** Extracts the variables from an equation or expression.
	 * @param	origEqn	the equation or expression
	 * @param	constants	constants associated with the equation or
	 * expression, already formatted for submission to Maxima
	 * @return	array of names of variables
	 */
	public static String[] getVariables(String origEqn, String constants) {
		final String SELF = "EquationFunctions.getVariables: ";
		String[] vars = new String[0];
		final String eqn = origEqn + (isEquation(origEqn) ? "" : " = 0");
		debugPrint(SELF + "getting variables of equation: ", eqn);
		final String calcn = Utils.toString(Utils.isEmpty(constants) ? ""
					: Utils.getBuilder(constants, "; "),
				"solve(", eqn, ')');
		debugPrint(SELF + "submitting to Maxima expression:\n", calcn);
		try {
			String solvedEqn = execute(calcn);
			debugPrint(SELF + "got solved equation(s): ", solvedEqn);
			final boolean multipleVars = isMultivariable(solvedEqn);
			// clip off any constants from start of results string
			final int firstBracket = solvedEqn.indexOf('[');
			solvedEqn = solvedEqn.substring(firstBracket);
			if (multipleVars) {
				final int endBracket = solvedEqn.indexOf(']');
				solvedEqn = solvedEqn.substring(1, endBracket);
				vars = Utils.splitTrim(solvedEqn, ",");
			} else {
				final int endBracket = solvedEqn.lastIndexOf(']');
				solvedEqn = solvedEqn.substring(1, endBracket); 
				if (isEquation(solvedEqn)) {
					final String[] halves = Utils.splitTrim(solvedEqn, "="); 
					vars = new String[] {halves[0]};
				} // if we have equation and not empty set
			} // if equation was multivariable
		} catch (StringIndexOutOfBoundsException e) {
			debugPrint(SELF + "StringIndexOutOfBoundsException");
		} catch (EquationFormatException e) {
			debugPrint(SELF + "EquationFormatException");
		} catch (ProcessExecutionException e) {
			debugPrint(SELF + "ProcessExecutionException");
		} // try
		return vars;
	} // getVariables(String, String)

	/** Determines whether an expression or equation is an equation.
	 * @param	exprnOrEqn	an expression or equation
	 * @return	true if the expression or equation contains an equal sign
	 */
	public static boolean isEquation(String exprnOrEqn) {
		return exprnOrEqn.indexOf('=') > 0;
	} // isEquation(String)

	/** Gets whether Maxima failed to solve an equation because there were more
	 * variables than equations.
	 * @param	solvedEqn	result from Maxima
	 * @return true if there were more variables than equations
	 */
	public static boolean isMultivariable(String solvedEqn) {
		return solvedEqn.indexOf("more unknowns than") >= 0;
	} // isMultivariable(String)

	/** Replaces all instances of one or more regular expressions in a string 
	 * with new patterns. 
	 * @param	str	the string
	 * @param	substns	which substitutions to make
	 * @return	a string with the substitutions made
	 */
	private static String substitute(String str, int... substns) {
		String repl = str;
		for (final int substn : substns) {
			repl = repl.replaceAll(PATTERNS[substn], REPLACEMENTS[substn]);
		} // for each substitution
		return repl;
	} // substitute(String, int...)

	/** Disable instantiation. */
	private EquationFunctions() { } // intentionally empty

} // EquationFunctions
