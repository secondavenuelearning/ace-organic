package com.epoch.evals.impl.chemEvals.energyEvals;

import com.epoch.energyDiagrams.OED;
import com.epoch.energyDiagrams.diagramConstants.OrbitalConstants;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.CompareNums;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the number of {s, p, ..., hybrid, atomic, molecular} orbitals in 
 * {column 1, column 2, column 3} {is, is not} {=, &lt;, &gt;} <i>n</i> ...  */
public class OEDOrbType extends CompareNums 
		implements EvalInterface, OrbitalConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** The type of orbital to count. */
	protected int orbType;

	/** The column in which to count orbitals or electrons. */
	protected int column;
	/** The number to compare, or, if negative, the number of the column whose
	 * orbitals or electrons to compare. */
	protected int number;

	/** Constructor. */
	public OEDOrbType() {
		column = 1;
		orbType = SP3;
		setOper(EQUALS); // inherited from CompareNums
		number = 4;
	} // OEDOrbType()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>oper</code>/<code>number</code>/<code>column</code>/<code>orbType</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public OEDOrbType(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 4) {
			setOper(Utils.indexOf(SYMBOLS, splitData[0]));
			number = MathUtils.parseInt(splitData[1]);
			column = MathUtils.parseInt(splitData[2]);
			orbType = MathUtils.parseInt(splitData[3]);
		}
		if (splitData.length < 4 || getOper() == -1
				|| !MathUtils.inRange(column, new int[] {0, 3})
				|| number < 0) {
			throw new ParameterException("OEDOrbType ERROR: unknown input data "
					+ "'" + data + "'. ");
		} // if there aren't three tokens
	} // OEDOrbType(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>oper</code>/<code>number</code>/<code>column</code>/<code>orbType</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(SYMBOLS[getOper()], '/', number, '/', column, 
				'/', orbType);
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
		return Utils.toString("If the number of ",
				orbType <= 0 ? GRP_NAMES[-orbType] : INDIV_NAMES[orbType],
				" orbitals in column ", column, " is", 
				OPER_ENGLISH[FEWER][getOper()], number);
	} // toEnglish()

	/** Determines whether the response diagram's columns have the right number
	 * of orbitals.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final OneEvalResult evalResult = new OneEvalResult();
		final OED respOED = (OED) response.parsedResp;
		try {
			final int[] orbTypes = 
					(orbType == ANY ? new int[0]
					: orbType == HYBRID ? new int[] {SP, SP2, SP3}
					: orbType == ATOMIC ? new int[] {S, P, SP, SP2, SP3}
					: orbType == MOLECULAR ? new int[] {SIGMA, PI, SIGMA_STAR, PI_STAR}
					: orbType == BONDING ? new int[] {SIGMA, PI}
					: orbType == ANTIBONDING ? new int[] {SIGMA_STAR, PI_STAR}
					: new int[] {orbType});
			final int count = respOED.getColumnOrbitalCount(column, orbTypes);
			evalResult.isSatisfied = compare(count, number);
		} catch (ParameterException e) { // unlikely
			Utils.alwaysPrint("OEDOrbType.isResponseMatching: "
					+ "unable to count orbitals in response");
			e.printStackTrace();
			evalResult.verificationFailureString = "ACE was unable "
					+ "to count the orbitals in your response.  Please "
					+ "report this error to the programmers.";
		} // try
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[OED_TYPE]; } 
	/** Gets the column whose orbitals to count.
	 * @return	the column whose orbitals to count
	 */
	public int getColumn() 					{ return column; }
	/** Sets the column whose orbitals to count.
	 * @param	col	the column whose orbitals to count
	 */
	public void setColumn(int col) 			{ column = col; }
	/** Gets the value of the number to compare.
	 * @return	value of the number to compare
	 */
	public int getNumber() 					{ return number; }
	/** Sets the value of the number to compare.
	 * @param	n	value of the number to compare
	 */
	public void setNumber(int n) 			{ number = n; }
	/** Gets the orbital type to count.
	 * @return	value of the orbital type to count
	 */
	public int getOrbType() 				{ return orbType; }
	/** Sets the orbital type to count.
	 * @param	type	orbital type to count
	 */
	public void setOrbType(int type) 		{ orbType = type; }

} // OEDOrbType
