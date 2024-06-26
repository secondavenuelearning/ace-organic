package com.epoch.evals.impl.genericQEvals.numericEvals;

import static com.epoch.constants.UnitConvertConstants.*; 
import com.epoch.db.UnitConvertRW;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.TextAndNumbers;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.EquationFormatException;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.ProcessExecutionException;
import com.epoch.genericQTypes.Numeric;
import com.epoch.physics.EquationFunctions;
import com.epoch.physics.physicsConstants.CanonicalizedUnitConstants;
import com.epoch.responses.Response;
import com.epoch.substns.SubstnUtils;
import com.epoch.substns.substnConstants.SubstnConstants;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the numerical value of the response {is, is not} {=, &lt;, &gt;}
 * <i>m</i> in the unit <i>X</i> ... */
public class NumberIs extends TextAndNumbers 
		implements CanonicalizedUnitConstants, EvalInterface, SubstnConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Colon-separated list of 1-based options (each represents a unit)
	 * to compare to the student's choice. */
	private String authUnit = "";

	/** Constructor. */
	public NumberIs() {
		setOper(NOT_EQUALS); // inherited from CompareNums
	}  // NumberIs()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>oper</code>/<code>authCoeff</code>/<code>tolerance</code>/<code>authExponent</code>
	 * <br>or<br>
	 * <code>oper</code>/<code>authCoeff</code>/<code>tolerance</code>/<code>authExponent</code>/<code>authUnit</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public NumberIs(String data) throws ParameterException {
		debugPrint("NumberIs: data = ", data);
		final String[] splitData = data.split("/");
		if (splitData.length >= 4)  {
			setOper(Utils.indexOf(SYMBOLS, splitData[0]));
			authNumStr = splitData[1];
			toleranceStr = splitData[2];
			authExponentStr = splitData[3];
			final int len = splitData.length;
			final StringBuilder unitBld = new StringBuilder();
			for (int dNum = 4; dNum < len; dNum++) {
				if (dNum != 4) unitBld.append('/');
				unitBld.append(splitData[dNum]);
			}
			authUnit = unitBld.toString();
		}
		if (splitData.length < 4 || getOper() == -1) {
			throw new ParameterException("NumberIs ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // NumberIs(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>oper</code>/<code>authCoeff</code>/<code>tolerance</code>/<code>authExponent</code>
	 * <br>or<br>
	 * <code>oper</code>/<code>authCoeff</code>/<code>tolerance</code>/<code>authExponent</code>/<code>authUnit</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		final StringBuilder data = Utils.getBuilder(SYMBOLS[getOper()], '/',
				authNumStr, '/', toleranceStr, '/', authExponentStr);
		if (!Utils.isEmpty(authUnit)) {
			Utils.appendTo(data, '/', authUnit);
		} // if there is an author unit
		return data.toString();
	} // getCodedData()

	/** Gets an English-language description of this evaluator.
	 * @param	qDataTexts	the text of question data of this question, if any
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question; required
	 * by interface
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts, boolean forPermissibleSM) {
		return toEnglish(qDataTexts);
	} // toEnglish(String[], boolean)

	/** Gets an English-language description of this evaluator.
	 * @param	qDataTexts	the text of question data of this question, if any
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts) {
		final boolean tolIs0 = MathUtils.parseDouble(toleranceStr) == 0;
		final boolean expIs0 = MathUtils.parseDouble(authExponentStr) == 0;
		final StringBuilder words = Utils.getBuilder("If the response is",
				OPER_ENGLISH[LESSER][getOper()]);
		if (tolIs0) {
			Utils.appendTo(words, MathUtils.isMinus(authNumStr.charAt(0))
						? Utils.getBuilder("&minus;", authNumStr.substring(1))
						: authNumStr);
			if (!expIs0) {
				Utils.appendTo(words, " &times; 10<sup>",
						MathUtils.isMinus(authExponentStr.charAt(0))
							? Utils.getBuilder("&minus;", 
								authExponentStr.substring(1))
				 			: authExponentStr,
				 		"</sup>");
			} // if there's an exponent
		} else {
			final StringBuilder numTol = Utils.getBuilder(
					MathUtils.isMinus(authNumStr.charAt(0))
						? Utils.getBuilder("&minus;", authNumStr.substring(1))
						: authNumStr,
					" &plusmn; ", toleranceStr);
			if (expIs0) words.append(numTol);
			else {
				Utils.appendTo(words, '(', numTol, ") &times; 10<sup>",
						MathUtils.isMinus(authExponentStr.charAt(0))
							? Utils.getBuilder("&minus;", 
								authExponentStr.substring(1))
				 			: authExponentStr,
						"</sup>");
			} // if there's an exponent
		} // if there's a tolerance
		if (!Utils.isEmpty(authUnit)) {
			Utils.appendTo(words, ' ', authUnit);
			if (!Utils.isEmpty(qDataTexts) && qDataTexts.length > 1) {
				words.append(", or the equivalent in another unit");
			} // if there are multiple units
		} // if there is an author unit
		return words.toString();
	} // toEnglish(String[])

	/** Determines whether the response in its unit equals the author's number
	 * in its unit.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "NumberIs.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		final Numeric numberResp = (Numeric) response.parsedResp;
		double authNum = 0;
		if (Utils.isEmpty(response.values)) {
			authNum = MathUtils.parseDouble(authNumStr);
		} else {
			String authExprn = EquationFunctions.formatExpression(
					restoreSlashes(authNumStr), NO_CANONICALZN);
			authExprn = SubstnUtils.substituteValues(
					authExprn, response.values, NUMERIC_VALUE);
			debugPrint(SELF + "values = ", response.values,
					", original authExprn = ", authNumStr, 
					", modified authExprn = ", authExprn);
			try {
				authExprn = EquationFunctions.execute(authExprn);
				debugPrint(SELF + "Maxima returned value ", authExprn);
				authNum = MathUtils.parseDouble(authExprn);
			} catch (ProcessExecutionException e) {
				Utils.alwaysPrint(SELF + "caught ProcessExecutionException "
						+ "while trying to evaluate Maxima expression ",
						authExprn);
				e.printStackTrace();
			} catch (EquationFormatException e) {
				Utils.alwaysPrint(SELF + "caught EquationFormatException "
						+ "while trying to evaluate Maxima expression ",
						authExprn);
				e.printStackTrace();
			} // try
		} // if there are substitution values
		double tolerance = MathUtils.parseDouble(toleranceStr);
		final int authExponent = MathUtils.parseInt(authExponentStr);
		if (authExponent != 0) {
			final double power = Math.pow(10, authExponent);
			authNum *= power;
			tolerance *= power;
		}
		// prevent precision errors
		if (tolerance == 0) {
			final int sigFigs = MathUtils.countSigFigs(authNum);
			tolerance = Math.abs(authNum) * Math.pow(10, -(sigFigs + 6));
			debugPrint(SELF + "number of sigfigs in ", authNum, " is ", 
					sigFigs, ", tolerance of 0 changed to ", tolerance);
		} else {
			tolerance *= 1.000001;
		}
		final String respUnit = numberResp.getUnit();
		if (!Utils.isEmpty(authUnit)) {
			if (Utils.isEmpty(respUnit)) {
				evalResult.autoFeedback = new String[]
						{"You must choose a unit."};
				evalResult.verificationFailureString = 
						"ACE could not interpret your numeric response.";
				return evalResult;
			} else {
				if (!respUnit.equals(authUnit)) {
					double[] powerFactor = new double[] {0.0, 0.0};
					try {
						powerFactor = UnitConvertRW.
								getUnitConversion(authUnit, respUnit);
					} catch (DBException e) {
						debugPrint(SELF + "DBException");
					} // try
					if (powerFactor[FACTOR] == 0.0) {
						evalResult.verificationFailureString = Utils.toString(
								"ACE has no way to convert numbers in author "
								+ "unit \"", authUnit, "\" to response unit \"",
								respUnit, "\", so it cannot evaluate this "
								+ "response.  Please report "
								+ "this error to the programmers.");
						return evalResult;
					} // if conversion factor could not be found
					authNum = powerFactor[FACTOR] 
							* Math.pow(authNum, powerFactor[POWER]);
					tolerance = powerFactor[FACTOR] 
							* Math.pow(tolerance, powerFactor[POWER]);
				} // if the author and student have different response units
			} // if there's a response authUnit
		} // if should take unit into account
		final double respCoeff = numberResp.getCoefficient();
		final double respExponent = numberResp.getExponent();
		final double respNum = respCoeff * Math.pow(10, respExponent);
		evalResult.isSatisfied = compare(respNum, authNum, tolerance);
		if (!Utils.isEmpty(authUnit)) {
			debugPrint(SELF + "respNum = ", respNum, 
					", respUnit = ", respUnit, 
					", orig authNum = ", authNumStr, 
					", orig tolerance = ", toleranceStr, 
					", authUnit = ", authUnit, 
					", recalculated authNum = ", authNum, 
					", recalculated tolerance = ", tolerance, 
					", oper = ", SYMBOLS[getOper()], ", returning ",
					evalResult.isSatisfied);
		} else {
			debugPrint(SELF + "respNum = ", respNum, 
					", orig authNum = ", authNumStr, 
					", orig tolerance = ", toleranceStr, 
					", modified tolerance = ", tolerance, 
					", oper = ", SYMBOLS[getOper()], ", returning ",
					evalResult.isSatisfied);
		} // if took unit into account
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Replaces CERs for slashes with slashes.
	 * @param	str	a string
	 * @return	the string with / characters restored
	 */
	private static String restoreSlashes(String str) {
		return str.replaceAll("&#47;", "/");
	} // restoreSlashes(String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[NUM_IS]; } 
	/** Gets the author's number's unit.
	 * @return	the author's number's unit
	 */
	public String getAuthUnit() 				{ return authUnit; }
	/** Sets the author's number's unit.
	 * @param	authUnit	the author's number's unit
	 */
	public void setAuthUnit(String authUnit)	{ this.authUnit = authUnit; }

} // NumberIs
