package com.epoch.evals.impl.chemEvals.energyEvals;

import com.epoch.energyDiagrams.OED;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the number of electrons in {column 1, column 2, column 3, columns 1 and 3} 
 * {is, is not} {=, &lt;, &gt;} {<i>n</i>, column 2} ...  */
public class OEDElecCt extends OEDOrbType implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Value for column. */
	public static final int COLUMNS1AND3 = 0;

	/** Constructor. */
	public OEDElecCt() {
		column = COLUMNS1AND3;
		orbType = ANY;
		setOper(EQUALS); // inherited from CompareNums
		number = -2;
	} // OEDElecCt()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>oper</code>/<code>number</code>/<code>column</code>/<code>orbType</code>
	 * <br>where number &lt; 0 indicates electron count of column &ndash;number
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public OEDElecCt(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 3) {
			setOper(Utils.indexOf(SYMBOLS, splitData[0]));
			number = MathUtils.parseInt(splitData[1]);
			column = MathUtils.parseInt(splitData[2]);
			orbType = (splitData.length == 3 ? ANY
					: MathUtils.parseInt(splitData[3]));
		}
		if (splitData.length < 3 || getOper() == -1
				|| !MathUtils.inRange(column, new int[] {0, 3})
				|| number < -3) {
			throw new ParameterException("OEDElecCt ERROR: unknown input data "
					+ "'" + data + "'. ");
		} // if there aren't three tokens
	} // OEDElecCt(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>oper</code>/<code>number</code>/<code>column</code>/<code>orbType</code>
	 * <br>where number &lt; 0 indicates electron count of column &ndash;number
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(SYMBOLS[getOper()], '/', number, '/',
				column, '/', orbType);
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
		final StringBuilder words = Utils.getBuilder(
				"If the number of electrons in ",
				orbType <= 0 ? GRP_NAMES[-orbType] : INDIV_NAMES[orbType],
				" orbitals in column");
		if (column == COLUMNS1AND3) words.append("s 1 and 3");
		else Utils.appendTo(words, ' ', column);
		Utils.appendTo(words, " is ", OPER_ENGLISH[FEWER][getOper()]);
		if (number < 0) {
			Utils.appendTo(words, "the number in column ", -number);
		} else words.append(number);
		return words.toString();
	} // toEnglish()

	/** Determines whether the response diagram's columns have the right number
	 * of electrons.
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
			final int count = (column == COLUMNS1AND3
					? respOED.getColumnElectronCount(1, orbTypes)
						+ respOED.getColumnElectronCount(3, orbTypes)
					: respOED.getColumnElectronCount(column, orbTypes));
			final int toCompare = (number >= 0 ? number
					: respOED.getColumnElectronCount(-number, orbTypes));
			evalResult.isSatisfied = compare(count, toCompare);
		} catch (ParameterException e) { // unlikely
			Utils.alwaysPrint("OEDElecCt.isResponseMatching: "
					+ "unable to count electrons in response");
			e.printStackTrace();
			evalResult.verificationFailureString = "ACE was unable "
					+ "to count the electrons in your response.  Please "
					+ "report this error to the programmers.";
		} // try
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() { return EVAL_CODES[OED_ELEC]; }

} // OEDElecCt
