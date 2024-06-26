package com.epoch.genericQTypes;

import com.epoch.genericQTypes.genericQConstants.RankConstants;
import com.epoch.qBank.QDatum;
import com.epoch.translations.PhraseTransln;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Holds a response to a rank or numbering question.  */
public class Rank implements RankConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** List containing the 1-based items as Strings. */
	transient private final List<String> itemStrs = new ArrayList<String>();
	/** List containing the 1-based items as Integers. */
	transient private final List<Integer> items = new ArrayList<Integer>();
	/** List containing the ranks parallel to the items. */
	transient private final List<Integer> ranks = new ArrayList<Integer>();

	/** Constructor. 
	 * @param	rankStr	String representing the rank response.  Long format is
	 * 5:1;2:3;3:5;4:4;1:2 where the first number in each colon-separated pair is
	 * the item number, the second number is the rank, and the order of items is
	 * the order in which they were displayed.  Short format, used for logging, 
	 * is 2:3:5:4:1 where each number is the rank, and its position gives the 
	 * item number.  
	 * @throws	NumberFormatException	if numbers can't be parsed
	 */
	public Rank(String rankStr) {
		if (rankStr.indexOf(MAJOR_SEP) >= 0
				&& rankStr.indexOf(MINOR_SEP) >= 0) { // long format
			final String[] itemRanks = rankStr.split(MAJOR_SEP);
			for (int itemNum = 0; itemNum < itemRanks.length; itemNum++) {
				final String[] pieces = itemRanks[itemNum].split(MINOR_SEP);
				if (pieces.length == 2) {
					itemStrs.add(pieces[0]);
					items.add(Integer.decode(pieces[0]));
					ranks.add(Integer.decode(pieces[1]));
				} else if (pieces.length == 1) {
					itemStrs.add(itemRanks[itemNum]);
					items.add(Integer.decode(itemRanks[itemNum]));
					ranks.add(Integer.valueOf(0));
				} // if there are one or two pieces
			} // for each item & rank
		} else { // short format
			final String separator = (rankStr.indexOf(MAJOR_SEP) >= 0 
					? MAJOR_SEP : MINOR_SEP);
			final String[] ranksArr = rankStr.split(separator);
			for (int itemNum = 0; itemNum < ranksArr.length; itemNum++) {
				itemStrs.add(String.valueOf(itemNum + 1));
				items.add(Integer.valueOf(itemNum + 1));
				ranks.add(Integer.decode(ranksArr[itemNum]));
			} // for each rank
		} // if rank is long or short format
	} // Rank(String)

	/** Gets the 1-based items in the response in the order in which they were displayed.
	 * @return	array of 1-based items 
	 */
	public int[] getAllItems() {
		return Utils.listToIntArray(items);
	} // getAllItems()

	/** Gets the ranks of the response in the order in which the items were displayed.
	 * @return	array of ranks 
	 */
	public int[] getAllRanks() {
		return Utils.listToIntArray(ranks);
	} // getAllRanks()

	/** Gets the rank of a particular 1-based item number.
	 * @param	item	the 1-based number of the item
	 * @return	rank of the item
	 */
	public int getRankForItem(int item) {
		final int posn = items.indexOf(Integer.valueOf(item));
		return (posn < 0 ? NOT_FOUND : ranks.get(posn).intValue());
	} // getRankForItem(int)

	/** Gets the rank of a particular 1-based item number that is a String.
	 * @param	item	the 1-based number of the item
	 * @return	rank of the item
	 */
	public int getRankForItem(String item) {
		final int posn = itemStrs.indexOf(item);
		return (posn < 0 ? NOT_FOUND : ranks.get(posn).intValue());
	} // getRankForItem(String)

	/** Gets a String of ranks in the natural order of the item numbers.  Used 
	 * by ResponseLogger.
	 * @return	a String such as 3:1:0:2: for item 1 ranked as 3, item 2 as 1,
	 * item 3 unranked, item 4 ranked as 2.
	 */
	public String getRankStringOrderedByItem() {
		final StringBuilder respStr = new StringBuilder();
		for (int itemNum = 1; itemNum <= items.size(); itemNum++) {
			final int rank = getRankForItem(itemNum);
			Utils.appendTo(respStr, rank == NOT_FOUND ? 0 : rank, MINOR_SEP);
		} // for each item
		return respStr.toString();
	} // getRankStringOrderedByItem()
	
	/** Converts the response into a format suitable for display. 
	 * Assumes that ranks start with 1 and are consecutive, 
	 * which is ensured by the Javascript on the question-answering page.
	 * @param	qData	the question data that the item numbers represent
	 * @param	chemFormatting	whether to use chemistry formatting
	 * @return	the response in HTML suitable for display
	 */
	public String toDisplay(QDatum[] qData, boolean chemFormatting) {
		return toDisplay(qData, chemFormatting, null);
	} // toDisplay(QDatum[], boolean)
	
	/** Converts the response into a format suitable for display. 
	 * Assumes that ranks start with 1 and are consecutive, 
	 * which is ensured by the Javascript on the question-answering page.
	 * @param	qData	the question data that the item numbers represent
	 * @param	chemFormatting	whether to use chemistry formatting
	 * @param	userLangs	non-English languages of the viewing user in order 
	 * of preference
	 * @return	the response in HTML suitable for display
	 */
	public String toDisplay(QDatum[] qData, boolean chemFormatting,
			String[] userLangs) {
		final List<Integer> itemsList = new ArrayList<Integer>(items); 
		final List<Integer> ranksList = new ArrayList<Integer>(ranks); 
		final StringBuilder output = new StringBuilder();
		int rank = 1;
		while (true) {
			final int posn = ranksList.indexOf(rank);
			if (posn < 0) break;
			ranksList.remove(posn);
			final int qdNum = itemsList.remove(posn) - 1;
			Utils.appendTo(output, '(', rank, ") ",
					qData[qdNum].toShortDisplay(chemFormatting), "<br>");
			rank++;
		} // while there are more ranks
		// remaining items are unranked (rank 0)
		for (int itemNum = 0; itemNum < itemsList.size(); itemNum++) {
			final int qdNum = itemsList.get(itemNum) - 1;
			Utils.appendTo(output, '(',
					PhraseTransln.translate("unranked", userLangs),
					") ", qData[qdNum].toShortDisplay(chemFormatting),
					"<br>");
		} // for each unranked item
		return output.toString();
	} // toDisplay(QDatum[], boolean, String[])

	/** Strips the student's orderings from a Rank response.
	 * @param	resp	a string representing a response to a Rank question
	 * @return	the response with all orderings stripped
	 */
	public static String getUnrankedString(String resp) {
		return resp.replaceAll(MINOR_SEP + "\\d+", MINOR_SEP + '0');
	} // getUnrankedString(String)

	/** Builds an initialized string.
	 * @param	numOpts	number of options
	 * @param	scramble	whether to scramble the options
	 * @param	exceptLast	if scrambling, whether to leave the last option last
	 * @return	a string in the format 1:0;2:0;3:0 ..., maybe with options
	 * scrambled
	 */
	public static String getInitialString(int numOpts, boolean scramble,
			boolean exceptLast) {
		final StringBuilder bld = new StringBuilder();
		final ArrayList<Integer> options = new ArrayList<Integer>();
		final int lastOptMaybe = numOpts - (scramble && exceptLast ? 1 : 0);
		for (int optNum = 1; optNum <= lastOptMaybe; optNum++)
			options.add(Integer.valueOf(optNum));
		if (scramble) {
			Collections.shuffle(options);
			if (exceptLast) options.add(Integer.valueOf(numOpts));
		} // if scrambling
		for (final Integer opt : options) {
			if (bld.length() > 0) bld.append(MAJOR_SEP);
			Utils.appendTo(bld, opt, MINOR_SEP, 0);
		} // for each option
		return bld.toString();
	} // getInitialString(int, boolean, boolean)

} // Rank
