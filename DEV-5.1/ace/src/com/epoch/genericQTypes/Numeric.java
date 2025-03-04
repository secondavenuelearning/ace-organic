package com.epoch.genericQTypes;

import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.genericQConstants.NumConstants;
import com.epoch.qBank.QDatum;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** Holds a response to a numerical question. */
public class Numeric implements NumConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Coefficient of the number in scientific notation, or the number itself. */
	transient private double coefficient = 0;
	/** Coefficient in String format. */
	transient private String coeffStr = "";
	/** Base 10 exponent of the number if it is in scientific notation. */
	transient private double exponent = 0;
	/** Base 10 exponent of the number if it is in scientific notation. */
	transient private String exponStr = "";
	/** 1-based number of the unit. */
	transient private int unitNum = 0;
	/** Name of the unit. */
	transient private String unit = "";

	/** Constructor. 
	 * @param	num	a number
	 */
	public Numeric(double num) {
		coefficient = num;
		coeffStr = String.valueOf(num);
	} // Numeric(double)

	/** Constructor. 
	 * @param	numStr	String representing the numeric response;
	 * format is number [:exponent] [tab unit]
	 */
	public Numeric(String numStr) {
		setData(numStr);
	} // Numeric(String)

	/** Constructor. 
	 * @param	numStr	String representing the numeric response;
	 * format is number [:exponent] [tab unit]
	 * @param	qData	the question data that the unit numbers represent
	 * @throws	ParameterException	if unit number is out of range
	 */
	public Numeric(String numStr, QDatum[] qData) throws ParameterException {
		setData(numStr);
		if (!Utils.isEmpty(qData)) { 
			if (MathUtils.inRange(unitNum, new int[] {1, qData.length})) {
				unit = qData[unitNum - 1].data;
			} else if (unitNum != 0) {
				throw new ParameterException("Unit number " + unitNum 
						+ " out of range of 1.." + qData.length);
			} // if unitNum
		} // if there are units to choose from
		debugPrint("Numeric: unit = ", unit);
	} // Numeric(String, QDatum[])

	/** Utility method for constructors. 
	 * @param	numStr	String representing the numeric response;
	 * format is number [:exponent] [tab unit]
	 */
	private void setData(String numStr) {
		final String SELF = "Numeric.setData: ";
		debugPrint(SELF + "numStr = ", numStr);
		if (numStr != null) {
			final String[] numUnit = numStr.split(NUM_UNIT_SEP);
			final String[] coeffExp = numUnit[0].split(COEFF_EXP_SEP);
			coeffStr = coeffExp[0].trim();
			coefficient = MathUtils.parseDouble(coeffStr);
			if (coeffExp.length > 1) {
				exponStr = coeffExp[1].trim();
				exponent = MathUtils.parseDouble(exponStr);
			} // if there's an exponent
			if (numUnit.length > 1) {
				unitNum = MathUtils.parseInt(numUnit[1].trim());
			} // if there's a unit
		} // if numStr not null
		debugPrint(SELF + "coefficient = ", coefficient, 
				", exponent = ", exponent, ", unitNum = ", unitNum);
	} // setData(String)

	/** Gets the coefficient.
	 * @return	the coefficient 
	 */
	public double getCoefficient() {
		return coefficient;
	} // getCoefficient()

	/** Gets the coefficient as a String.
	 * @return	the coefficient as a String
	 */
	public String getCoefficientStr() {
		return coeffStr;
	} // getCoefficientStr()

	/** Gets the exponent.
	 * @return	the exponent 
	 */
	public double getExponent() {
		return exponent;
	} // getExponent()

	/** Gets the exponent as a String.
	 * @return	the exponent  as a String
	 */
	public String getExponentStr() {
		return exponStr;
	} // getExponentStr()

	/** Gets the 1-based number of the unit.
	 * @return	the 1-based number of the unit (0 if none) 
	 */
	public int getUnitNum() {
		return unitNum;
	} // getUnitNum()

	/** Gets the name of the unit.
	 * @return	the name of the unit ("" if none)
	 */
	public String getUnit() {
		return unit;
	} // getUnit()

	/** Converts the number into HTML format suitable for display.   
	 * @return	the number in HTML format
	 */
	public String toDisplay() {
		final StringBuilder output = Utils.getBuilder(
				"".equals(coeffStr) ? "0" : Utils.formatNegative(coeffStr)); 
		if (!"".equals(exponStr)) {
			Utils.appendTo(output, " &times; 10<sup>",
					Utils.formatNegative(exponStr), "</sup>");
		}
		return Utils.toString(output, ' ', unit); // "" if none
	} // toDisplay()

} // Numeric
