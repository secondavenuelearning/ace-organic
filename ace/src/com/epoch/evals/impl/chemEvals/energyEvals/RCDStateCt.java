package com.epoch.evals.impl.chemEvals.energyEvals;

import com.epoch.energyDiagrams.RCD;
import com.epoch.energyDiagrams.RCDCell;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.CompareNums;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.Locale;

/** If the number of {maxima, minima, either} that are labeled with
 * {1, 2, ...} in {none, any, each, all} of 
 * the chosen columns {is, is not} {=, &lt;, &gt;} <i>n</i> ...  */
public class RCDStateCt extends CompareNums implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** The 1-based columns whose states to count, as a string. */
	private String columnsStr;
		/** Separator of columns. */
		public static final String COLS_SEP = ";";
	/** Whether to count maxima, minima, or both. */
	private int which;
		/** Value for which. */
		public static final int MAX = 0;
		/** Value for which. */
		public static final int MIN = 1;
		/** Value for which. */
		public static final int BOTH = 2;
		/** Database values for which. */
		static final String[] WHICH_DB = new String[] 
				{"MAX", "MIN", "BOTH"};
		/** English names for which. */
		public static final String[] WHICH_ENGL = new String[] 
				{"maxima", "minima", "maxima and minima"};
	/** The 1-based label of the states to count; 0 means any. */
	private int label;
	/** Whether to count none, any, each, or all of the chosen columns. */
	private int mode;
		/** Value for mode. */
		public static final int NONE = 0;
		/** Value for mode. */
		public static final int ANY = 1;
		/** Value for mode. */
		public static final int EACH = 2;
		/** Value for mode. */
		public static final int ALL = 3;
		/** Database values for mode. */
		public static final String[] MODE = new String[] 
				{"NONE", "ANY", "EACH", "ALL"};
	/** The number of states to compare. */
	private int number;

	/** Constructor. */
	public RCDStateCt() {
		columnsStr = "1";
		which = BOTH;
		mode = ANY;
		setOper(NOT_EQUALS); // inherited from CompareNums
		number = 1;
		label = 0;
	} // RCDStateCt()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>oper</code>/<code>number</code>/<code>columnsStr</code>/<code>which</code>/<code>mode</code>/<code>label</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public RCDStateCt(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 6) {
			setOper(Utils.indexOf(SYMBOLS, splitData[0]));
			number = MathUtils.parseInt(splitData[1]);
			columnsStr = splitData[2];
			which = Utils.indexOf(WHICH_DB, splitData[3]);
			mode = Utils.indexOf(MODE, splitData[4]);
			label = MathUtils.parseInt(splitData[5]);
		}
		if (splitData.length < 6 || getOper() == -1 || which == -1 || mode == -1) {
			throw new ParameterException("RCDStateCt ERROR: unknown input data "
					+ "'" + data + "'. ");
		} // if there aren't five tokens
	} // RCDStateCt(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>oper</code>/<code>number</code>/<code>columnsStr</code>/<code>which</code>/<code>mode</code>/<code>label</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(SYMBOLS[getOper()], '/', number, '/', columnsStr, 
				'/', WHICH_DB[which], '/', MODE[mode], '/', label);
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
	 * @param	qDataTexts	text from question data to use for English
	 * description of evaluator
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts) {
		final StringBuilder words = 
				Utils.getBuilder("If the number of ", WHICH_ENGL[which]);
		if (label > 0) {
			words.append(" labeled ");
			if (label <= qDataTexts.length) {
				Utils.addSpanString(words, qDataTexts[label - 1]);
			} else {
				words.append(label);
			} // if we have text for the label
		} // if a label has been chosen
		words.append(" in ");
		if (columnsStr.indexOf(COLS_SEP) < 0) {
			Utils.appendTo(words, "column ", columnsStr, " is");
			if (mode == NONE) words.append(" not");
		} else {
			Utils.appendTo(words, 
					MODE[mode].toLowerCase(Locale.ENGLISH), " of columns {",
					columnsStr.replaceAll(COLS_SEP, ", "), "} is");
		}
		return Utils.toString(words, OPER_ENGLISH[FEWER][getOper()], number);
	} // toEnglish(String[])

	/** Determines whether the response diagram's columns have the right number
	 * of states.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final OneEvalResult evalResult = new OneEvalResult();
		final RCD respRCD = (RCD) response.parsedResp;
		evalResult.isSatisfied = mode != ANY;
				/* NONE is true until we find a match;
				 * EACH is true until we find a nonmatch;
				 * ANY is false until we find a match;
				 * ALL can't be evaluated until all columns are counted.  */
		final int[] columns = 
				Utils.stringToIntArray(columnsStr.split(COLS_SEP));
		final int numRows = respRCD.getNumRows();
		int allColsCt = 0;
		for (final int col : columns) {
			int colCt = 0;
			for (int row = 1; row <= numRows; row++) {
				try {
					if (respRCD.isOccupied(row, col) && (label == 0 
								|| label == respRCD.getLabel(row, col))) {
						if (which == BOTH) colCt++;
						else {
							final int state = respRCD.getState(row, col);
							if ((which == MAX && state == RCDCell.MAXIMUM)
									|| (which == MIN 
										&& state == RCDCell.MINIMUM)) {
								colCt++;
							} // if should count this state
						} // if nature of state matters
					} // if there's a state with the right label
				} catch (ParameterException e) { // unlikely
					Utils.alwaysPrint("RCDStateCt.isResponseMatching: "
							+ "unable to count electrons in response");
					evalResult.verificationFailureString = "ACE was unable "
							+ "to count the electrons in your response.  Please "
							+ "report this error to the programmers.";
				} // try
			} // for each row
			if (mode != ALL) {
				final boolean match = compare(colCt, number);
				if (mode == ANY && match) {
					evalResult.isSatisfied = true;
					return evalResult;
				} else if ((mode == NONE && match)
						|| (mode == EACH && !match)) {
					evalResult.isSatisfied = false;
					return evalResult;
				} // if we found our answer
			} else allColsCt += colCt;
		} // for each column chosen
		if (mode == ALL) {
			evalResult.isSatisfied = compare(allColsCt, number);
		} // if comparing sum across all columns
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[RCD_STATE_CT]; } 
	/** Gets the columns whose states to count.
	 * @return	the column or columns whose states to count
	 */
	public String getColumnsStr() 			{ return columnsStr; }
	/** Sets the columns whose states to count.
	 * @param	col	the columns whose states to count
	 */
	public void setColumnsStr(String col) 	{ columnsStr = col; }
	/** Gets whether to count maxima, minima, or both.
	 * @return	whether to count maxima, minima, or both
	 */
	public int getWhich() 					{ return which; }
	/** Sets whether to count maxima, minima, or both.
	 * @param	w	whether to count maxima, minima, or both
	 */
	public void setWhich(int w) 			{ which = w; }
	/** Gets the label of the states to count.
	 * @return	label of the states to count
	 */
	public int getLabel() 					{ return label; }
	/** Sets the label of the states to count.
	 * @param	n	label of the states to count
	 */
	public void setLabel(int n) 			{ label = n; }
	/** Gets how many of the columns must satisfy the condition.
	 * @return	how many of the columns must satisfy the condition
	 */
	public int getMode() 					{ return mode; }
	/** Sets how many of the columns must satisfy the condition.
	 * @param	m	how many of the columns must satisfy the condition
	 */
	public void setMode(int m) 				{ mode = m; }
	/** Gets the value of the number to compare.
	 * @return	value of the number to compare
	 */
	public int getNumber() 					{ return number; }
	/** Sets the value of the number to compare.
	 * @param	n	value of the number to compare
	 */
	public void setNumber(int n) 			{ number = n; }

} // RCDStateCt
