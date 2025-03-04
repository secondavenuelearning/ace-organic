package com.epoch.evals.impl.genericQEvals.rankEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.Rank;
import com.epoch.genericQTypes.genericQConstants.RankConstants;
import com.epoch.responses.Response;
import com.epoch.utils.Utils;

/** If the relative {direct, inverted} ordering of the items 
 * {agrees, doesn't agree} {contiguously, contiguously or noncontiguously} 
 * with the author's ordering ...  */
public class RankOrder implements EvalInterface, RankConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Whether the evaluator is satisfied by a match or a mismatch. */
	private boolean isPositive;
	/** Colon-separated list of items in the order they must appear in the
	 * student's response. */
	private String selection;
	/** Whether the items in the author's list must be contiguous in the
	 * student's list. */
	private boolean contiguous;
	/** Whether the items in the author's list should be increasing or
	 * decreasing in rank in the student's list. */
	private boolean increasing;
	/** Character that separates item numbers in <code>selection</code>. */
	public static final String SEPARATOR = ":";
	/** Member of return value of getRankedUnranked() containing the items in
	 * rank order.  */
	public static final int RANKED = 0;
	/** Member of return value of getRankedUnranked() containing the unranked
	 * items.  */
	public static final int UNRANKED = 1;
	/** In coded data, indicates items that remain unranked.  */
	public static final String UNRANKED_STR = "~";

	/** Constructor. */
	public RankOrder() { // default values
		isPositive = false;
		contiguous = true;
		increasing = false;
		selection = "";
	} // RankOrder()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>contiguous</code>/<code>increasing</code>/<code>selection</code>
	 * @throws	ParameterException	if the coded data is inappropriate
	 * for this evaluator
	 */
	public RankOrder(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 4) {
			isPositive = Utils.isPositive(splitData[0]);
			contiguous = Utils.isPositive(splitData[1]);
			increasing = Utils.isPositive(splitData[2]);
			selection = splitData[3];
		} else {
			throw new ParameterException("RankOrder "
					+ "ERROR: unknown input data '" + data + "'. ");
		} // four tokens
	} // RankOrder(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>isPositive</code>/<code>contiguous</code>/<code>increasing</code>/<code>selection</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive ? "Y/" : "N/", contiguous 
				? "Y/" : "N/", increasing ? "Y/" : "N/", selection);
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
	 * @param	qDataTexts	descriptions of items to be ranked
	 * @return	short string describing this evaluator in English
	 */
	public String toEnglish(String[] qDataTexts) {
		final StringBuilder words = Utils.getBuilder("If the response ",
				isPositive ? "puts" : "doesn't put");
		try {
			final String[] rankedUnranked = getRankedUnranked();
			final String[] rankedOptStrs = 
					rankedUnranked[RANKED].split(SEPARATOR);
			final int[] rankedOpts = Utils.stringToIntArray(rankedOptStrs);
			final int numRankedOpts = rankedOpts.length;
			final int numTexts = qDataTexts.length;
			for (int rNum = 0; rNum < numRankedOpts; rNum++) {
				if (rNum > 0 && numRankedOpts > 2) words.append(',');
				if (rNum == numRankedOpts - 1 && numRankedOpts > 1)
					words.append(" and");
				words.append(' ');
				final int optNum = rankedOpts[rNum];
				if (optNum <= numTexts) {
					Utils.addSpanString(words, qDataTexts[optNum - 1]);
					Utils.appendTo(words, " (#", optNum, ')');
				} else {
					Utils.appendTo(words, '#', optNum);
				} // if optNum is in range
			} // for each ranked item
			words.append(' ');
			if (contiguous) words.append("contiguously");
			Utils.appendTo(words, " in ", increasing ? "in" : "de",
					"creasing order");
			if (!Utils.isEmpty(rankedUnranked[UNRANKED])) {
				final String[] unrankedOptStrs = 
						rankedUnranked[UNRANKED].split(SEPARATOR);
				final int[] unrankedOpts = 
						Utils.stringToIntArray(unrankedOptStrs);
				final int numUnrankedOpts = unrankedOpts.length;
				words.append(" and leaves unnumbered");
				for (int rNum = 0; rNum < numUnrankedOpts; rNum++) {
					if (rNum > 0 && numUnrankedOpts > 2) words.append(',');
					if (rNum == numUnrankedOpts - 1 && numUnrankedOpts > 1)
						words.append(" and");
					words.append(' ');
					final int optNum = unrankedOpts[rNum];
					if (optNum <= numTexts) {
						Utils.addSpanString(words, qDataTexts[optNum - 1]);
					} else words.append(optNum);
				} // for each unranked item
			} // if there are unranked items
		} catch (Exception e) {
			// No exceptions are thrown
			words.append(" no items");
		}
		return words.toString();
	} // toEnglish(String[])

	/** Separates coded data into two strings of ranked and unranked items.
	 * Ranked items are listed in their rank order.
	 * @return	array of two colon-separated Strings of items
	 */
	public String[] getRankedUnranked() {
		String[] itemStrs = new String[2];
		final int beginUnranked = selection.indexOf(UNRANKED_STR);
		itemStrs[RANKED] = (beginUnranked < 0
				? selection : selection.substring(0, beginUnranked));
		itemStrs[UNRANKED] = (beginUnranked < 0 ? "" 
				: selection.substring(beginUnranked).replaceAll(UNRANKED_STR, ""));
		return itemStrs;
	} // getRankedUnranked()

	/** Determines whether the user has ordered the items in the indicated way.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final Rank rankResp = (Rank) response.parsedResp;
		final OneEvalResult evalResult = new OneEvalResult();
		debugPrint("RankOrder starts: response = ", response.unmodified,
				", author's selection = ", selection, ", isPositive = ",
				isPositive, ", contiguous = ", contiguous,
				", increasing = ", increasing);
		final String[] rankedUnranked = getRankedUnranked();
		final String authRankedStr = rankedUnranked[RANKED];
		final String authUnrankedStr = rankedUnranked[UNRANKED];
		debugPrint("RankOrder: authRankedStr = ", authRankedStr,
				", authUnrankedStr = ", authUnrankedStr);
		// find ranks in response of each pair of author-ranked items
		final String[] authRanked = ("".equals(authRankedStr)
				? new String[0] : authRankedStr.split(SEPARATOR));
		for (int rankNum = 1; rankNum < authRanked.length; rankNum++) {
			final String authItem = authRanked[rankNum];
			// ignore Jlint complaint about line above.  Raphael 11/2010
			final String prevAuthItem = authRanked[rankNum - 1];
			final int respRank = rankResp.getRankForItem(authItem);
			final int prevRespRank = rankResp.getRankForItem(prevAuthItem);
			debugPrint("RankOrder: authItem[", rankNum - 1,
					"] = ", prevAuthItem, " and authItem[", rankNum,
					"] = ", authItem, " are ranked by student at ",
					prevRespRank, " and ", respRank, ", respectively.");
			if (respRank == NOT_FOUND || prevRespRank == NOT_FOUND) {
				evalResult.verificationFailureString =
						"Item " + (respRank == NOT_FOUND
								? authItem : prevAuthItem)
						+ " in rank string " + authRankedStr
						+ " not found in response.";
				Utils.alwaysPrint("RankOrder: ", evalResult.verificationFailureString);
				return evalResult;
			} else if (respRank == 0 || prevRespRank == 0) {
				debugPrint("RankOrder: ranked authItem ",
						(respRank == 0 ? authItem : prevAuthItem),
						" unranked by student; returning ", !isPositive);
				evalResult.isSatisfied = !isPositive;
				return evalResult;
			} else if ((increasing && respRank - prevRespRank < 0)
					|| (!increasing && respRank - prevRespRank > 0)
					|| (contiguous && Math.abs(respRank - prevRespRank) != 1)) {
				debugPrint("RankOrder: Response rankings don't match "
						+ "author's; returning ", !isPositive);
				evalResult.isSatisfied = !isPositive;
				return evalResult;
			} // if response item's relative rank isn't right
		} // for each item that should be ranked
		final String[] authUnranked = ("".equals(authUnrankedStr)
				? new String[0] : authUnrankedStr.split(SEPARATOR));
		for (final String authItem : authUnranked) {
			// ignore Jlint complaint about line above.  Raphael 11/2010
			final int respRank = rankResp.getRankForItem(authItem);
			if (respRank == NOT_FOUND) {
				evalResult.verificationFailureString =
						"Item " + authItem + " that should be unranked"
						+ " not found in response.";
				Utils.alwaysPrint("RankOrder: ", evalResult.verificationFailureString);
				return evalResult;
			} else if (respRank != 0) {
				debugPrint("RankOrder: unranked authItem ", authItem,
						" ranked by student at ", respRank,
						"; returning ", !isPositive);
				evalResult.isSatisfied = !isPositive;
				return evalResult;
			} // if response item that should be unranked is ranked
		} // for each item that should be unranked
		debugPrint("RankOrder: response ", response.unmodified,
				" matches author string ", selection,
				"; returning ", isPositive);
		evalResult.isSatisfied = isPositive;
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 					{ return EVAL_CODES[RANK_ORDER]; } 
	/** Gets whether the evaluator is satisfied by a match or a mismatch.
	 * @return	true if the evaluator is satisfied by a match
	 */
	public boolean getIsPositive() 					{ return isPositive; }
	/** Sets whether the evaluator is satisfied by a match or a mismatch.
	 * @param	isPositive	true if the evaluator is satisfied by a match
	 */
	public void setIsPositive(boolean isPositive) 	{ this.isPositive = isPositive; }
	/** Gets the items, in the order they must appear in the student's response.
	 * @return	the items
	 */
	public String getSelection() 					{ return selection; }
	/** Sets the items, in the order they must appear in the student's response.
	 * @param	selection	the items
	 */
	public void setSelection(String selection) 		{ this.selection = selection; }
	/** Gets whether the author's items must be contiguous in the student's list.
	 * @return	true if the items must be contiguous
	 */
	public boolean getContiguous() 					{ return contiguous; }
	/** Sets whether the author's items must be contiguous in the student's list.
	 * @param	contiguous	whether the items must be contiguous
	 */
	public void setContiguous(boolean contiguous)	{ this.contiguous = contiguous; }
	/** Gets whether the items in the author's list should be increasing or
	 * decreasing in rank in the student's list.
	 * @return	true if the items should be increasing in rank
	 */
	public boolean getIncreasing() 					{ return increasing; }
	/** Sets whether the items in the author's list should be increasing or
	 * decreasing in rank in the student's list.
	 * @param	increasing	true if the items should be increasing in rank
	 */
	public void setIncreasing(boolean increasing) 	{ this.increasing = increasing; }
	/** Not used.  Required by interface.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 			{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 					{ return false; }

} // RankOrder

