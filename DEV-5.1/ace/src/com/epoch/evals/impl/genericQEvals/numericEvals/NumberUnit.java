package com.epoch.evals.impl.genericQEvals.numericEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.Numeric;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the response {uses, doesn't use} the unit ...  */
public class NumberUnit implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether the evaluator is satisfied by a match or a mismatch. */
	private boolean isPositive;
	/** Colon-separated list of 1-based options (each represents a unit)
	 * to compare to the student's choice. */
	transient private String unitNumsStr = "";
	/** Separator of unit numbers in the coded data. */
	public static final String SEPARATOR = ":";

	/** Constructor. */
	public NumberUnit() {
		// intentionally empty
	} // NumberUnit()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>unitNumsStr</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public NumberUnit(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 2) {
			isPositive = Utils.isPositive(splitData[0]);
			unitNumsStr = splitData[1];
			debugPrint("NumberUnit.java: data = ", data, ", isPositive = ",
					isPositive, ", unit numbers = ", unitNumsStr);
		} else
			throw new ParameterException("NumberUnit ERROR: "
					+ "unknown input data '" + data + "'. ");
	} // NumberUnit(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>isPositive</code>/<code>unitNumsStr</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive ? "Y/" : "N/", unitNumsStr);
	} // getCodedData()

	/** Gets an English-language description of this evaluator.
	 * @param	qDataTexts	the text of question data of this question, if any
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts, boolean forPermissibleSM) {
		return toEnglish(qDataTexts);
	} // toEnglish(String[], boolean)

	/** Gets an English-language description of this evaluator.
	 * @param	qDataTexts	the names of the units
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts) {
		debugPrint("NumberUnit.toEnglish: qDataTexts = ", qDataTexts);
		final StringBuilder words = Utils.getBuilder("If the student has ",
				isPositive ? "" : "not ", "chosen ");
		try {
			final String[] unitNumStrs = unitNumsStr.split(":");
			final int[] unitNums = Utils.stringToIntArray(unitNumStrs);
			final int numUnits = unitNums.length;
			int start = 0;
			if (numUnits != 0 && unitNums[0] == 0) {
				words.append(" no units");
				if (numUnits != 1) words.append(" or");
				start = 1;
			} // if no units is an option
			if (numUnits > start) {
				final int numUnitsLeft = numUnits - start;
				final int numTexts = qDataTexts.length;
				words.append(numUnitsLeft == 1 ? " the unit"
						: " one of the units");
				for (int uNum = start; uNum < numUnits; uNum++) {
					if (uNum > start && numUnitsLeft > 2) words.append(',');
					if (uNum == numUnits - 1 && numUnitsLeft > 1)
						words.append(" or");
					words.append(' ');
					final int unitNum = unitNums[uNum];
					if (unitNum <= numTexts) {
						Utils.addSpanString(words, qDataTexts[unitNum - 1]);
					} else words.append(unitNum);
				} // for each unit
			} // if units are options
		} catch (Exception e) {
			words.append(" no units");
		}
		return words.toString();
	} // toEnglish(String[])

	/** Determines whether the user has selected the indicated unit or units.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final OneEvalResult evalResult = new OneEvalResult();
		final Numeric numberResp = (Numeric) response.parsedResp;
		final int respUnitNum = numberResp.getUnitNum();
		final String[] unitNumsArray = unitNumsStr.split(SEPARATOR);
		boolean chosen = false;
		for (final String unitNumStr : unitNumsArray) {
			if (MathUtils.parseInt(unitNumStr) == respUnitNum) {
				chosen = true;
				break;
			} // if the selected unit is among the author's choices
		} // for each author's choice
		evalResult.isSatisfied = chosen == isPositive;
		debugPrint("NumberUnit.isResponseMatching: respUnitNum = ",
				respUnitNum, ", unitNumsStr = ", unitNumsStr, ", isPositive= ", 
				isPositive, ", returning ", evalResult.isSatisfied);
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[NUM_UNIT]; } 
	/** Gets whether the evaluator is satisfied by a match or a mismatch.
	 * @return	true if the evaluator is satisfied by a match
	 */
	public boolean getIsPositive() 				{ return isPositive; }
	/** Sets whether the evaluator is satisfied by a match or a mismatch.
	 * @param	pos	true if the evaluator is satisfied by a match
	 */
	public void setIsPositive(boolean pos)	 	{ isPositive = pos; }
	/** Gets the options (each represents a unit)
	 * to compare to the student's choice.
	 * @return	the options
	 */
	public String getUnitNums() 				{ return unitNumsStr; }
	/** Sets the options (each represents a unit)
	 * to compare to the student's choice.
	 * @param	str	the options
	 */
	public void setUnitNums(String str)			{ unitNumsStr = str; }
	/** Not used.  Required by interface.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 		{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 				{ return false; }

} // NumberUnit

