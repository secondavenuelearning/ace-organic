package com.epoch.evals.impl.genericQEvals.rankEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.Rank;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;

/** If the absolute rank of an item {agrees, doesn't agree} with the 
 * author's suggestion ...  */
public class RankPosition implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether the evaluator is satisfied by a match or a mismatch. */
	private boolean isPositive;
	/** Option and its rank, separated by a colon. */
	private String selection;
	/** Character that separates item and rank in <code>selection</code>. */
	public static final String SEPARATOR = ":";
	/** Position of item in <code>selection</code>.  */
	public static final int ITEM = 0;
	/** Position of rank in <code>selection</code>.  */
	public static final int RANK = 1;

	/** Constructor. */
	public RankPosition() { // default values
		isPositive = false;
		selection = "";
	} // RankPosition()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>selection</code>
	 * @throws	ParameterException	if the coded data is inappropriate
	 * for this evaluator
	 */
	public RankPosition(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 2) {
			isPositive = Utils.isPositive(splitData[0]);
			selection = splitData[1];
		} else {
			throw new ParameterException("RankPosition ERROR: "
					+ "unknown input data '" + data + "'. ");
		} // two tokens
	} // RankPosition(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>isPositive</code>/<code>selection</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive ? "Y/" : "N/", selection);
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
	 * @param	qDataTexts	the text of question data of this question, if any;
	 * not used, but required by interface
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts) {
		final String[] itemRank = selection.split(SEPARATOR);
		final int item = MathUtils.parseInt(itemRank[ITEM]);
		final int numTexts = Utils.getLength(qDataTexts);
		final StringBuilder words = Utils.getBuilder("If the response ");
		if ("0".equals(itemRank[RANK])) {
			words.append(isPositive ? "leaves " : "doesn't leave ");
			if (item <= numTexts) {
				Utils.addSpanString(words, qDataTexts[item - 1]);
			} else {
				Utils.appendTo(words, "item ", item);
			}
			words.append(" unnumbered");
		} else {
			words.append(isPositive ? "numbers " : "doesn't number ");
			if (item <= numTexts) {
				Utils.addSpanString(words, qDataTexts[item - 1]);
			} else {
				Utils.appendTo(words, "item ", item);
			}
			Utils.appendTo(words, " as ", itemRank[RANK]);
		} // if the item is unranked
		return words.toString();
	} // toEnglish(String[])

	/** Determines whether the user has numbered the indicated item in the
	 * indicated way.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final OneEvalResult evalResult = new OneEvalResult();
		final String[] itemRank = selection.split(SEPARATOR);
		final Rank rankResp = (Rank) response.parsedResp;
		final int respRank = rankResp.getRankForItem(itemRank[ITEM]);
		final int authRank = MathUtils.parseInt(itemRank[RANK]);
		final boolean sameRank = respRank == authRank;
		evalResult.isSatisfied = sameRank == isPositive;
		debugPrint("RankPosition: respOrigStr ", response.unmodified,
				(sameRank ? " contains" : " does not contain"), " selection ", 
				selection, "; returning ", evalResult.isSatisfied);
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 					{ return EVAL_CODES[RANK_POSITION]; } 
	/** Gets whether the evaluator is satisfied by a match or a mismatch.
	 * @return	true if the evaluator is satisfied by a match
	 */
	public boolean getIsPositive() 					{ return isPositive; }
	/** Sets whether the evaluator is satisfied by a match or a mismatch.
	 * @param	isPositive	true if the evaluator is satisfied by a match
	 */
	public void setIsPositive(boolean isPositive) 	{ this.isPositive = isPositive; }
	/** Gets an item and rank.
	 * @return	item and rank
	 */
	public String getSelection() 					{ return selection; }
	/** Sets an item and rank.
	 * @param	selection	item and rank
	 */
	public void setSelection(String selection) 		{ this.selection = selection; }
	/** Not used.  Required by interface.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 			{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 					{ return false; }

} // RankPosition

