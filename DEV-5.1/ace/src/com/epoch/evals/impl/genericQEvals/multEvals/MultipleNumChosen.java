package com.epoch.evals.impl.genericQEvals.multEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.CompareNums;
import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.Choice;
import com.epoch.genericQTypes.ChooseExplain;
import com.epoch.genericQTypes.genericQConstants.ChoiceConstants;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/**  If the number of multiple-choice options chosen
 * {is, is not} {=, &lt;, &gt;} <i>n</i> ... */
public class MultipleNumChosen extends CompareNums 
		implements ChoiceConstants, EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether the evaluator is satisfied by a match or a mismatch. */
	private boolean isPositive;
	/** Number of options to which to compare the student's count. */
	private int numChosen;
	/** Colon-separated list of options; count only those student-chosen options
	 * that are in this list. */
	transient private String amongOpts;

	/** Constructor. */
	public MultipleNumChosen() { // default values
		isPositive = false;
		setOper(EQUALS);
		numChosen = 1;
		amongOpts = ""; // empty string means all options
		resetOperEnglish();
	} // MultipleNumChosen()

	/** Changes the English description of each value of <code>oper</code> 
	 * inherited from CompareNums and CompareNumConstants.  */
	private void resetOperEnglish() {
		OPER_ENGLISH[FEWER] = new String[] {
				" exactly ",
				" more than ",
				" fewer than ",
				" not exactly ",
				" fewer than or equal to ",
				" more than or equal to "
				};
	} // resetOperEnglish()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>operator</code>/<code>numChosen</code>/<code>amongOpts</code>
	 * <br>where <code>amongOpts</code> may be empty.
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public MultipleNumChosen(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 3) {
			isPositive = Utils.isPositive(splitData[0]);
			setOper(Utils.indexOf(SYMBOLS, splitData[1]));
			numChosen = MathUtils.parseInt(splitData[2]);
			amongOpts = (splitData.length >= 4
					&& splitData[3] != null ? splitData[3] : "");
		} 
		if (splitData.length < 3 || getOper() == -1) {
			throw new ParameterException("MultipleNumChosen: "
					+ "ERROR: unknown input data '" + data + "'. ");
		} // three or four tokens
		debugPrint("MultipleNumChosen: data = " + splitData.length 
				+ ", numChosen = ", numChosen,
				", oper = ", SYMBOLS[getOper()], ", isPositive = ", isPositive);
		resetOperEnglish();
	} // MultipleNumChosen(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>isPositive</code>/<code>operator</code>/<code>numChosen</code>/<code>amongOpts</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive? "Y/" : "N/", SYMBOLS[getOper()],
				'/', numChosen, '/', amongOpts);
	} // getCodedData()

	/** Gets an English-language description of this evaluator.
	 * @param	qDataTexts	the text of question data of this question, if any;
	 * @param	forPermissibleSM	whether to word the English to describe a
	 * permissible starting material in a multistep synthesis question
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts, boolean forPermissibleSM) {
		return toEnglish(qDataTexts);
	} // toEnglish(String[], boolean)

	/** Gets an English-language description of this evaluator.
	 * @param	qDataTexts	the text of question data of this question, if any;
	 * not used, but required by interface
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts) {
		final StringBuilder words = Utils.getBuilder("If the student has ",
				isPositive ? "chosen" : "not chosen",
				OPER_ENGLISH[FEWER][getOper()], numChosen, " option");
		if (numChosen != 1) words.append('s');
		if (!Utils.isEmpty(amongOpts)) {
			words.append(" from among options ");
			final int[] opts = Utils.stringToIntArray(
					amongOpts.split(SEPARATOR));
			final int numChosenOpts = opts.length;
			final int numTexts = qDataTexts.length;
			for (int cNum = 0; cNum < numChosenOpts; cNum++) {
				if (cNum > 0 && numChosenOpts > 2) words.append(',');
				if (cNum == numChosenOpts - 1 && numChosenOpts > 1)
					words.append(" and");
				words.append(' ');
				final int optNum = opts[cNum];
				if (optNum <= numTexts) {
					Utils.addSpanString(words, qDataTexts[optNum - 1]);
					Utils.appendTo(words, " (#", optNum, ')');
				} else {
					Utils.appendTo(words, '#', optNum);
				} // if optNum is in range
			} // for each chosen option
		} // amongOpts not empty
		return words.toString();
	} // toEnglish()

	/** Determines whether the user selected the indicated number of options.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final OneEvalResult evalResult = new OneEvalResult();
		debugPrint("MultipleNumChosen.isResponseMatching: "
				+ "respOrig is ", response.unmodified,
				", numChosen = ", numChosen, ", oper = ", SYMBOLS[getOper()],
				", isPositive = ", isPositive, ", amongOpts = ", amongOpts);
		final Choice choiceResp = (response.parsedResp instanceof Choice
				? (Choice) response.parsedResp
				: ((ChooseExplain) response.parsedResp).choice);
		final int[] respOptions = choiceResp.getAllOptions();
		final boolean[] respChoices = choiceResp.getAllChoices();
		final String[] authOptions = (Utils.isEmpty(amongOpts) 
				? new String[0] : amongOpts.split(SEPARATOR));
		final boolean testAllOpts = authOptions.length == 0;
		int count = 0;
		for (int optNum = 0; optNum < respOptions.length; optNum++) {
			if (respChoices[optNum] // student chose this option
					&& (testAllOpts || Utils.contains(authOptions, 
						String.valueOf(respOptions[optNum])))) {
				count++;
			} // if respOrig optNum was chosen & is among options that count
		} // for each option
		evalResult.isSatisfied = isPositive == compare(count, numChosen);
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[CHOICE_NUM_CHECKED]; } 
	/** Gets whether the evaluator is satisfied by a match or a mismatch.
	 * @return	true if the evaluator is satisfied by a match
	 */
	public boolean getIsPositive() 				{ return isPositive; }
	/** Sets whether the evaluator is satisfied by a match or a mismatch.
	 * @param	isPos	true if the evaluator is satisfied by a match
	 */
	public void setIsPositive(boolean isPos)	{ isPositive = isPos; }
	/** Get the number of options to which to compare the student's count.
	 * @return	the number of options
	 */
	public int getNumChosen() 					{ return numChosen; }
	/** Set the number of options to which to compare the student's count.
	 * @param	numChosen	the number of options
	 */
	public void setNumChosen(int numChosen) 	{ this.numChosen = numChosen; }
	/** Get the only options of the student that should contribute to the count.
	 * @return	the list of options
	 */
	public String getAmongOptions() 			{ return amongOpts; }
	/** Set the only options of the student that should contribute to the count.
	 * @param	opts	the list of options
	 */
	public void setAmongOptions(String opts)	{ amongOpts = (opts == null 
														? "" : opts); }

} // MultipleNumChosen

