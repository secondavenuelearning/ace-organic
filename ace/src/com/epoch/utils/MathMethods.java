package com.epoch.utils;

import java.util.List;
import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.math.stat.descriptive.rank.Median;
import org.apache.commons.math.util.MathUtils;

/** Holds math utility functions. */
public class MathMethods {

	private static void debugPrint(Object... msg) {
		// Utils.alwaysPrint(msg);
	}

	/** Parameter for isInt() and isDouble(). */
	public static final boolean TRIM = true;

	/** Gets the sign of an integer.
	 * @param	num	the integer
	 * @return	-1, 0, or 1
	 */
	public static int sign(int num) {
		return MathUtils.sign(num);
	} // sign(int)

	/** Gets the sign of a double as an int.
	 * @param	num	the double
	 * @return	-1, 0, or 1
	 */
	public static int sign(double num) {
		return (int) MathUtils.sign(num);
	} // sign(double)

	/** Rounds a double to an int (as opposed to a long).
	 * @param	num	double to be rounded
	 * @return	the rounded int
	 */
	public static int roundToInt(double num) {
		return (int) Math.round(num);
	} // roundtoInt(double)

	/** Gets the ceiling of a double as an int (as opposed to a double).
	 * @param	num	a double 
	 * @return	the smallest int greater than or equal to the double 
	 */
	public static int ceilToInt(double num) {
		return (int) Math.ceil(num);
	} // ceilToInt(double)

	/** Determines if a string represents an int.
	 * @param	numStr	the String representing a number
	 * @return	true if it can be parsed as an int
	 */
	public static boolean isInt(String numStr) {
		return isInt(numStr, !TRIM);
	} // isInt(String)

	/** Sums the elements of an array of ints.
	 * @param	nums	an array of ints
	 * @return	the sum of the elements
	 */
	public static int sum(int... nums) {
		int sum = 0;
		for (final int num : nums) sum += num;
		return sum;
	} // sum(int[])

	/** Sums the elements of an array of doubles.
	 * @param	nums	an array of doubles
	 * @return	the sum of the elements
	 */
	public static double sum(double... nums) {
		double sum = 0;
		for (final double num : nums) sum += num;
		return sum;
	} // sum(double[])

	/** Determines if a string represents an int.
	 * @param	numStr	the String representing a number
	 * @param	trim	whether the string should be trimmed first
	 * @return	true if it can be parsed as an int
	 */
	public static boolean isInt(String numStr, boolean trim) {
		boolean isInt = true;
		if (numStr == null) {
			isInt = false;
		} else try {
			final String toParse = processNumStr(numStr);
			Integer.parseInt(trim ? toParse.trim() : toParse);
		} catch (NumberFormatException e) {
			isInt = false;
		} // try
		return isInt;
	} // isInt(String, boolean)

	/** Removes commas and replaces en dashes with hyphens.
	 * @param	str	a string
	 * @return	the processed string
	 */
	private static String processNumStr(String str) {
		return str.replaceAll(",", "")
				.replace((char) 8211, '-')
				.replace((char) 8722, '-');
	} // processNumStr(String)

	/** Gets if a character is a hyphen, minus sign, or en dash.
	 * @param	c	a character
	 * @return true if a character is a hyphen, minus sign, or en dash
	 */
	public static boolean isMinus(char c) {
		return Utils.among(c, '-', (char) 8211, (char) 8722);
	} // isMinus(char)

	/** Parse a character into an int, returning 0 if it is null or nonnumeric.
	 * @param	c	the char representing a number
	 * @return	the number, or 0 if it can't be parsed
	 */
	public static int parseInt(char c) {
		return parseInt(String.valueOf(c));
	} // parseInt(char)

	/** Parse a String into an int, returning 0 if it is null or nonnumeric.
	 * @param	numStr	the String representing a number
	 * @return	the number, or 0 if it can't be parsed
	 */
	public static int parseInt(String numStr) {
		return parseInt(numStr, 0);
	} // parseInt(String)

	/** Parse a String into an int, returning an error value if it is 
	 * null or nonnumeric.
	 * @param	numStr	the String representing a number
	 * @param	errorVal	value to return if String can't be parsed
	 * @return	the number, or errorVal if it can't be parsed
	 */
	public static int parseInt(String numStr, int errorVal) {
		return NumberUtils.toInt(numStr == null ? null
				: processNumStr(numStr).trim(), errorVal);
	} // parseInt(String, int)

	/** Determines if a string represents a double.
	 * @param	numStr	the String representing a number
	 * @return	true if it can be parsed as a double
	 */
	public static boolean isDouble(String numStr) {
		return isDouble(numStr, !TRIM);
	} // isDouble(String)

	/** Determines if a string represents a double.
	 * @param	numStr	the String representing a number
	 * @param	trim	whether the string should be trimmed first
	 * @return	true if it can be parsed as a double
	 */
	public static boolean isDouble(String numStr, boolean trim) {
		boolean isDouble = true;
		if (numStr == null) {
			isDouble = false;
		} else try {
			final String toParse = processNumStr(numStr);
			Double.parseDouble(trim ? toParse.trim() : toParse);
		} catch (NumberFormatException e) {
			isDouble = false;
		} // try
		return isDouble;
	} // isDouble(String, boolean)

	/** Parse a String into a double, returning 0 if it is null or nonnumeric.
	 * @param	numStr	the String representing a number
	 * @return	the number, or 0 if it can't be parsed
	 */
	public static double parseDouble(String numStr) {
		return parseDouble(numStr, 0.0);
	} // parseDouble(String)

	/** Parse a String into a double, returning an error value if it is 
	 * null or nonnumeric.
	 * @param	numStr	the String representing a number
	 * @param	errorVal	value to return if String can't be parsed
	 * @return	the number, or errorVal if it can't be parsed
	 */
	public static double parseDouble(String numStr, double errorVal) {
		return NumberUtils.toDouble(numStr == null ? null
				: processNumStr(numStr).trim(), errorVal);
	} // parseDouble(String, double)

	/** Determines if a string represents a long.
	 * @param	numStr	the String representing a number
	 * @param	trim	whether the string should be trimmed first
	 * @return	true if it can be parsed as a long
	 */
	public static boolean isLong(String numStr, boolean trim) {
		boolean isLong = true;
		if (numStr == null) {
			isLong = false;
		} else try {
			final String toParse = processNumStr(numStr);
			Long.parseLong(trim ? toParse.trim() : toParse);
		} catch (NumberFormatException e) {
			isLong = false;
		} // try
		return isLong;
	} // isLong(String, boolean)

	/** Parse a String into a long, returning 0 if it is null or nonnumeric.
	 * @param	numStr	the String representing a number
	 * @return	the number, or 0 if it can't be parsed
	 */
	public static long parseLong(String numStr) {
		return parseLong(numStr, 0);
	} // parseLong(String)

	/** Parse a String into a long, returning an error value if it is 
	 * null or nonnumeric.
	 * @param	numStr	the String representing a number
	 * @param	errorVal	value to return if String can't be parsed
	 * @return	the number, or errorVal if it can't be parsed
	 */
	public static long parseLong(String numStr, long errorVal) {
		return NumberUtils.toLong(numStr == null ? null
				: processNumStr(numStr).trim(), errorVal);
	} // parseLong(String, long)

	/** Find the median of the integer array, ignoring all values &lt; 0.
	 * @param	input	an array of ints
	 * @return	the median; &minus;1 if no elements &ge; 0
	 */
	public static int findMedian(int... input) {
		final List<Double> valsList = new ArrayList<Double>();
		for (final int val : input) {
			if (val >= 0) valsList.add(Double.valueOf((double) val));
		} // for each integer
		int median = -1;
		if (!valsList.isEmpty()) {
			final double[] vals = Utils.listToDoubleArray(valsList);
			final Median medianCalc = new Median();
			median = (int) medianCalc.evaluate(vals);
		}
		return median;
	} // findMedian(int[])

	/** Find the median of the double array, ignoring all values &lt; 0.
	 * @param	input	an array of doubles
	 * @return	the median; &minus;1 if no elements &ge; 0
	 */
	public static double findMedian(double... input) {
		final List<Double> valsList = new ArrayList<Double>();
		for (final double val : input) {
			if (val >= 0) valsList.add(Double.valueOf(val));
		} // for each double
		double median = -1;
		if (!valsList.isEmpty()) {
			final double[] vals = Utils.listToDoubleArray(valsList);
			final Median medianCalc = new Median();
			median = medianCalc.evaluate(vals);
		}
		return median;
	} // findMedian(double[])

	/** Is the given value between the minimum and maximum, inclusive?
	 * @param	value	the given value
	 * @param	limits	limits[0] = min, limits[1] = max
	 * @return	whether min &le; value &le; max
	 */
	public static boolean inRange(int value, int[] limits) {
		return (value >= limits[0] && value <= limits[1]);
	} // inRange(int, int[])

	/** Is the given value between the minimum and maximum, inclusive?
	 * @param	limits	limits[0] = min, limits[1] = max
	 * @param	value	the given value
	 * @return	whether min &le; value &le; max
	 */
	public static boolean inRange(int[] limits, int value) {
		return inRange(value, limits);
	} // inRange(int[], int)

	/** Is the given value between the minimum and maximum, inclusive?
	 * @param	value	the given value
	 * @param	limits	limits[0] = min, limits[1] = max
	 * @return	whether min &le; value &le; max
	 */
	public static boolean inRange(double value, double[] limits) {
		return (value >= limits[0] && value <= limits[1]);
	} // inRange(double, double[])

	/** Is the given value between the minimum and maximum, inclusive?
	 * @param	limits	limits[0] = min, limits[1] = max
	 * @param	value	the given value
	 * @return	whether min &le; value &le; max
	 */
	public static boolean inRange(double[] limits, double value) {
		return inRange(value, limits);
	} // inRange(double[], double)

	/** Counts the number of significant figures in a double.
	 * @param	number	the number whose significant figures to count
	 * @return	number of significant figures
	 */
	public static int countSigFigs(double number) {
		return countSigFigs(String.valueOf(number));
	} // countSigFigs(double)

	/** Counts the number of significant figures in an integer.
	 * @param	number	the number whose significant figures to count
	 * @return	number of significant figures
	 */
	public static int countSigFigs(int number) {
		return countSigFigs(String.valueOf(number));
	} // countSigFigs(int)

	/** Counts the number of significant figures in a number.
	 * @param	numStr	the number whose significant figures to count
	 * @return	number of significant figures
	 */
	public static int countSigFigs(String numStr) {
		int numSigFigs = 0;
		if (!Utils.isEmptyOrWhitespace(numStr)) {
			final String sigDigits = removeInsigDigits(numStr);
			numSigFigs = sigDigits.length();
			if (sigDigits.indexOf('.') >= 0) numSigFigs--;
		} // if there's a number whose sigfigs to count
		return numSigFigs;
	} // countSigFigs(String)

	/** Counts the number of significant figures before and after the decimal
	 * point in a string representing a double.
	 * @param	numStr	string representing a double
	 * @return array containing the number of significant figures before 
	 * and after the decimal
	 */
	public static int[] countPartSigFigs(String numStr) {
		final String SELF = "MathMethods.countPartSigFigs: ";
		final int[] numPartSigFigs = new int[2];
		if (!Utils.isEmptyOrWhitespace(numStr)) {
			final String sigDigits = removeInsigDigits(numStr);
			final int numLen = sigDigits.length();
			final int decPosn = sigDigits.indexOf('.');
			if (decPosn == -1) {
				final int origDecPosn = numStr.indexOf('.');
				final boolean beforeDec = origDecPosn == -1 
						|| numStr.indexOf(sigDigits) < origDecPosn;
				numPartSigFigs[beforeDec ? 0 : 1] = numLen;
			} else if (decPosn == 0) numPartSigFigs[1] = numLen - 1;
			else if (decPosn == numLen - 1) numPartSigFigs[0] = numLen - 1;
			else {
				numPartSigFigs[0] = decPosn;
				numPartSigFigs[1] = numLen - decPosn - 1;
			}
			debugPrint(SELF + "numStr = ", numStr, 
					", sigDigits = ", sigDigits, ", numLen = ",
					numLen, ", decPosn = ", decPosn, ", numPartSigFigs = ",
					numPartSigFigs);
		} // if not empty or whitespace
		return numPartSigFigs;
	} // countPartSigFigs(String)

	/** Removes insignificant digits from a String representing a number.
	 * @param	numStr	the String representing a number whose insignificant 
	 * figures to remove
	 * @return	the string with insignificant figures removed
	 */
	public static String removeInsigDigits(final String numStr) {
		final String SELF = "MathMethods.removeInsigDigits: ";
		String sigDigits = numStr;
		if (!Utils.isEmpty(numStr)) {
			sigDigits = numStr.trim();
			debugPrint(SELF + "starting with sigDigits = ", sigDigits);
			if (sigDigits.indexOf('.') < 0) {
				sigDigits = stripFromEndUnlessAllStrippable(sigDigits, "0");
				debugPrint(SELF + "after stripping 0s from end, "
						+ "sigDigits = ", sigDigits);
			} // if there is no decimal
			final String sigDigitsTemp = 
					stripFromStartUnlessAllStrippable(sigDigits, "-0.");
			if (sigDigitsTemp.charAt(0) != '.' 
					&& !sigDigitsTemp.startsWith("0.")) {
				sigDigits = Utils.toString(sigDigits.charAt(0) == '-'
						? "-" : "", sigDigitsTemp);
				debugPrint(SELF + "after stripping 0s and . from start, "
						+ "sigDigits = ", sigDigits);
			} // if haven't stripped integer part down to decimal
		} // if there's a number whose sigfigs to count
		return sigDigits;
	} // removeInsigDigits(String)

	/** Strips characters in a set from the start of a string unless all will be
	 * stripped, in which case the original string is returned.
	 * @param	str	the string
	 * @param	stripChars	the characters that should be removed
	 * @return	the string with characters stripped from the start, unless it
	 * strips them all, in which case the original string us returned
	 */
	private static String stripFromStartUnlessAllStrippable(String str, 
			String stripChars) {
		final String s = StringUtils.stripStart(str, stripChars);
		return (s.length() == 0 ? str : s);
	} // stripFromStartUnlessAllStrippable(String, String)

	/** Strips characters in a set from the end of a string unless all will be
	 * stripped, in which case the original string is returned.
	 * @param	str	the string
	 * @param	stripChars	the characters that should be removed
	 * @return	the string with characters stripped from the start, unless it
	 * strips them all, in which case the original string us returned
	 */
	private static String stripFromEndUnlessAllStrippable(String str, 
			String stripChars) {
		final String s = StringUtils.stripEnd(str, stripChars);
		return (s.length() == 0 ? str : s);
	} // stripFromEndUnlessAllStrippable(String, String)

	/** Converts an integer into a nonnegative modulo integer; negative integers
	 * are converted to positive ones by adding a multiple of the modulus.
	 * @param	num	a positive or negative integer
	 * @param	mod	the modulus
	 * @return	the integer mod the modulus
	 */
	public static int getMod(int num, int mod) {
		final int increment = (num >= 0 ? 0
				: mod * ceilToInt(((double) -num) / ((double) mod)));
		return (num + increment) % mod;
	} // getMod(int, int)

	/** Disables external instantiation. */
	protected MathMethods() { 
		// empty
	}

} // MathMethods
