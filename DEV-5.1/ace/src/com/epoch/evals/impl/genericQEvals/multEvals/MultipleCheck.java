package com.epoch.evals.impl.genericQEvals.multEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.Choice;
import com.epoch.genericQTypes.ChooseExplain;
import com.epoch.genericQTypes.genericQConstants.ChoiceConstants;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.Arrays;

/** If the response choices {do, don't} 
 * {match exactly, contain at least, appear among, overlap partly with} 
 * the author's choices ...  */
public class MultipleCheck implements ChoiceConstants, EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether the evaluator is satisfied by a match or a mismatch. */
	private boolean isPositive;
	/** How the student's choices relate to options listed by the author. */
	private int oper;
		/** Value for oper. Student's choices match exactly the author's. */
		public static final int EXACTLY = 1;
		/** Value for oper. Student's choices contain all the author's. */
		public static final int AT_LEAST = 2;
		/** Value for oper. Student's choices are all among the author's. */
		public static final int SOME_OF = 3;
		/** Value for oper. Student's choices overlap with the author's, but
		 * each has chosen an option not among the other's. */
		public static final int OVERLAPS = 4;
		/** English description of each value of <code>oper</code>. */
		public static final String[] OPER_ENGLISH = {
				"exactly",
				"at least",
				"only from among",
				"options that only partly overlap"
				};
	/** Colon-separated list of options chosen by the author. */
	private String selection = "";
		/** Colon-separated list of options chosen by the author. */
		public static final String NO_SELECTION = "0";

	/** Constructor. */
	public MultipleCheck() { // default values
		isPositive = false;
		oper = EXACTLY;
	} // MultipleCheck()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>oper</code>/<code>selection</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public MultipleCheck(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 3) { // newer format
			isPositive = Utils.isPositive(splitData[0]);
			oper = MathUtils.parseInt(splitData[1], EXACTLY);
			if (!NO_SELECTION.equals(splitData[2])) selection = splitData[2];
		} else {
			throw new ParameterException("MultipleCheck ERROR: "
					+ "unknown input data '" + data + "'. ");
		} // two or three tokens
		debugPrint("MultipleCheck: selection = ", selection,
				", oper = ", oper, ", isPositive = ", isPositive);
	} // MultipleCheck(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>isPositive</code>/<code>oper</code>/<code>selection</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive? "Y/" : "N/", oper, '/',
				Utils.isEmpty(selection) ? NO_SELECTION : selection);
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
	 * @param	qDataTexts	descriptions of options that may be chosen
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts) {
		final StringBuilder words = Utils.getBuilder("If the student has ");
		if (!isPositive) words.append("not ");
		Utils.appendTo(words, "chosen ", OPER_ENGLISH[oper - 1]);
		try {
			if (Utils.isEmpty(selection)) {
				words.append(" no options");
			} else {
				final String[] chosenStrs = selection.split(SEPARATOR);
				final int[] chosenOpts = Utils.stringToIntArray(chosenStrs);
				final int numChosenOpts = chosenOpts.length;
				final int numTexts = qDataTexts.length;
				for (int cNum = 0; cNum < numChosenOpts; cNum++) {
					if (cNum > 0 && numChosenOpts > 2) words.append(',');
					if (cNum == numChosenOpts - 1 && numChosenOpts > 1)
						words.append(" and");
					words.append(' ');
					final int optNum = chosenOpts[cNum];
					if (optNum <= numTexts) {
						Utils.addSpanString(words, qDataTexts[optNum - 1]);
						Utils.appendTo(words, " (#", optNum, ')');
					} else {
						Utils.appendTo(words, '#', optNum);
					} // if optNum is in range
				} // for each unit
			} // if options were chosen
		} catch (Exception e) {
			words.append(" no options");
		}
		return words.toString();
	} // toEnglish(String[])

	/** Converts author selection into a bit int.
	 * @param	numOptions	the number of options in the response
	 * @return	the chosen and unchosen options as a bit long array; the number
	 * of elements in the array is 1 + maxOption/64
	 */
	public long[] getBinaryArray(int numOptions) {
		final int BITS_IN_LONG = 64; // bits per long
		final long[] binaryArray = new long[1 + numOptions / BITS_IN_LONG];
		Arrays.fill(binaryArray, 0L);
		if (!Utils.isEmpty(selection)) {
			final String[] chosenOpts = selection.split(SEPARATOR);
			for (final String chosenOpt : chosenOpts) {
				final int choice = MathUtils.parseInt(chosenOpt);
				if (choice > 0) {
					binaryArray[(choice - 1) / BITS_IN_LONG] |=
							1L << (((choice - 1) % BITS_IN_LONG));
				} // if the option was chosen
			} // for each option chosen by author
		} // if the selection is not empty
		return binaryArray;
	} // getBinaryArray(int)

	/** Determines whether the user selected the indicated options.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "MultipleCheck.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		debugPrint(SELF + "the response is ",
				response.unmodified, ", author's selection = ",
				selection, ", operator = ", oper,
				", isPositive = ", isPositive);
		final Choice choiceResp = (response.parsedResp instanceof Choice
				? (Choice) response.parsedResp
				: ((ChooseExplain) response.parsedResp).choice);
		final int numOptions = choiceResp.getNumOptions();
		final long[] binStudent = choiceResp.getBinaryArray();
		final long[] binAuthor = getBinaryArray(numOptions);
		final String binStudStr = Utils.padBinary(
				Long.toBinaryString(binStudent[0]), numOptions);
		final String binAuthStr = Utils.padBinary(
				Long.toBinaryString(binAuthor[0]), numOptions);
		// Only the first 64 bits of the following debug output are valid.
		debugPrint(SELF + "student says ", binStudStr, 
				", author says ", binAuthStr);
		boolean result = false;
		switch (oper) {
		case EXACTLY: // AU = ST
			debugPrint("Exactly");
			result = true; // until shown otherwise
			for (int index = 0; index < binStudent.length; index += 1) {
				result = result && (binStudent[index] == binAuthor[index]);
			}
			break;
		case AT_LEAST: // AU contained in ST
			debugPrint("At least");
			result = containedIn(binAuthor, binStudent);
			break;
		case SOME_OF: // ST contained in AU
			debugPrint("Some or all of");
			result = containedIn(binStudent, binAuthor);
			break;
		case OVERLAPS: // AU intersect ST != 0, but neither contains the other
			debugPrint("Overlaps only partly");
			if (containedIn(binStudent, binAuthor) ||
					containedIn(binAuthor, binStudent)) {
				debugPrint("one is contained in the other");
				result = false;
				break;
			}
			result = false; // until we find some overlap
			for (int index = 0; index < binStudent.length; index += 1) {
				result = result ||
						((binStudent[index] & binAuthor[index]) != 0);
			}
			break;
		default:
			Utils.alwaysPrint("MultipleCheck: bad oper");
		} // switch(oper)
		debugPrint("result of comparison is " + result);
		evalResult.isSatisfied = isPositive == result;
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Returns whether the bits of the first array are a subset (proper or
	 * improper) of the bits of the second array.
	 * @param   first  an array of bits
	 * @param   second  an array of bits
	 * @return	boolean
	 */
	private boolean containedIn(long first[], long second[]) {
		for (int index = 0; index < first.length; index += 1) {
			if ((first[index] & second[index]) != first[index]) {
				return false;
			}
		}
		return true;
	} // containedIn(long[], long[])

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[CHOICE_WHICH_CHECKED]; } 
	/** Gets whether the evaluator is satisfied by a match or a mismatch.
	 * @return	true if the evaluator is satisfied by a match
	 */
	public boolean getIsPositive() 				{ return isPositive; }
	/** Sets whether the evaluator is satisfied by a match or a mismatch.
	 * @param	isPos	true if the evaluator is satisfied by a match
	 */
	public void setIsPositive(boolean isPos)	{ isPositive = isPos; }
	/** Gets how the student's choices relate to options listed by the author.
	 * @return	how the student's set relates to the author's set
	 */
	public int getOper() 						{ return oper; }
	/** Sets how the student's choices relate to options listed by the author.
	 * @param	oper	how the student's set relates to the author's set
	 */
	public void setOper(int oper) 				{ this.oper = oper; }
	/** Gets options chosen by the author.
	 * @return	options chosen by the author, as a colon-separated string
	 */
	public String getSelection() 				{ return selection; }
	/** Sets options chosen by the author.
	 * @param	selection	options chosen by the author, as a colon-separated string
	 */
	public void setSelection(String selection)	{ this.selection = selection; }
	/** Not used.  Required by interface.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 		{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 				{ return false; }

} // MultipleCheck

