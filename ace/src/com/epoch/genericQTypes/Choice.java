package com.epoch.genericQTypes;

import com.epoch.genericQTypes.genericQConstants.ChoiceConstants;
import com.epoch.qBank.QDatum;
import com.epoch.translations.PhraseTransln;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Holds a response to a multiple-choice or fill-blank question. */
public class Choice implements ChoiceConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Array containing the 1-based options in the order in which they were
	 * presented to the user. */
	transient private int[] options = new int[0];
	/** Array containing whether each parallel option was chosen. */
	transient private boolean[] choices = new boolean[0];
	/** Whether each option was chosen, in binary format. */
	transient private long binaryArray[];
	/** List of chosen 1-based options. */
	transient private List<Integer> chosenOptions = new ArrayList<Integer>();

	/** Constructor. 
	 * @param	respStr	String representing the multiple-choice response;
	 * has the format 5:1+:2:4:3+ where each number is the option number, the
	 * order is the order of display, and + indicates the chosen options.
	 * @throws	NumberFormatException	if numbers can't be parsed
	 */
	public Choice(String respStr) {
		final String[] optionsWithChoices = respStr.split(SEPARATOR);
		debugPrint("Choice: optionsWithChoices = ", optionsWithChoices);
		final int numOptions = optionsWithChoices.length;
		options = new int[numOptions];
		choices = new boolean[numOptions];
		binaryArray = new long[1 + numOptions / BITS_IN_LONG];
		for (int optNum = 0; optNum < numOptions; optNum++) {
			final String option = optionsWithChoices[optNum];
			final int posn = option.indexOf(CHOSEN);
			if (posn >= 0) { // this option was chosen
				options[optNum] = MathUtils.parseInt(option.substring(0, posn));
				final int choice = options[optNum];
				if (choice < 1) {
					Utils.alwaysPrint("Choice: invalid value ", choice);
				} else {
					choices[optNum] = true;
					chosenOptions.add(Integer.valueOf(choice));
					binaryArray[(choice - 1) / BITS_IN_LONG] |= 
						(1L << ((choice - 1) % BITS_IN_LONG));
				}
			} else { // the option was available but not chosen
				options[optNum] = MathUtils.parseInt(option);
			} // if option was chosen
		} // for each option
	} // Choice(String)

	/** Constructor for logged responses (short format). 
	 * @param	respStr	String representing the multiple-choice response;
	 * has the format 1:3: where each number is a chosen option
	 * @param	numOptions	the number of options in the question
	 */
	public Choice(String respStr, int numOptions) {
		if (respStr.indexOf(CHOSEN) < 0) {
			options = new int[numOptions];
			binaryArray = new long[1 + numOptions / BITS_IN_LONG];
			for (int optNum = 0; optNum < numOptions; optNum++) {
				options[optNum] = optNum + 1;
			} // for each option
			// account for early use of ; instead of :
			final String separator = (respStr.indexOf(SEPARATOR) >= 0
					? SEPARATOR : OLD_SEPARATOR);
			final String[] chosenOptsArr = respStr.split(separator);
			chosenOptions = Utils.stringToIntList(chosenOptsArr);
			choices = new boolean[numOptions];
			for (final Integer choice : chosenOptions) {
				final int optNum = choice.intValue() - 1;
				if (optNum < 0) {
					Utils.alwaysPrint("Choice: invalid optNum value ", optNum);
				} else {
					choices[optNum] = true;
					binaryArray[optNum / BITS_IN_LONG] |= 
						(1L << (optNum % BITS_IN_LONG));
				}
			} // for each chosen option
		} else { // long format (shouldn't happen)
			final Choice resp = new Choice(respStr);
			options = resp.options;
			choices = resp.choices;
			chosenOptions = resp.chosenOptions;
		} // if really in short format
	} // Choice(String, int)

	/** Gets the 1-based options in the response in the order in which they 
	 * were displayed.
	 * @return	array of options 
	 */
	public int[] getAllOptions() {
		return options.clone();
	} // getAllOptions()

	/** Gets the number of options.
	 * @return	number of options 
	 */
	public int getNumOptions() {
		return options.length;
	} // getNumOptions()

	/** Gets the choices of the response in the order in which the 
	 * options were displayed.
	 * @return	array of choices 
	 */
	public boolean[] getAllChoices() {
		return choices.clone();
	} // getAllChoices()

	/** Gets the student's response as a binary long[], where each option is
	 * represented by 0 or 1 (chosen or unchosen).
	 * @return	student's response as a binary int
	 */
	public long[] getBinaryArray() {
		return binaryArray;
	} // getBinary()

	/** Gets the number of chosen options. 
	 * @return number of chosen options
	 */
	public int getNumChosen() {
		return chosenOptions.size();
	} // getNumChosen()

	/** Gets the chosen 1-based options. 
	 * @param	sort	whether to return in the order that they were 
	 * displayed or in sorted order.
	 * @return array of chosen options
	 */
	public int[] getChosenOptions(boolean sort) {
		final int[] chosenOpts = Utils.listToIntArray(chosenOptions);
		if (sort) Arrays.sort(chosenOpts);
		return chosenOpts;
	} // getChosenOptions(boolean)

	/** Gets a response String of only the chosen options.  Used by
	 * ResponseLogger. 
	 * @param	sort	whether to return in the order that they were 
	 * displayed or in sorted order.
	 * @return	a String such as 2:4:
	 */
	public String getStringChosenOptions(boolean sort) {
		final int[] optionsArr = getChosenOptions(sort);
		final StringBuilder respBld = new StringBuilder();
		for (final int chosenOpt : optionsArr) {
			Utils.appendTo(respBld, chosenOpt, SEPARATOR);
		} // for each option
		return respBld.toString();
	} // getStringChosenOptions(boolean)
	
	/** Converts the chosen items into HTML format suitable for display.   
	 * @param	qData	the question data that the item numbers represent
	 * @param	chemFormatting	whether to use chemistry formatting
	 * @return	the chosen items in HTML format
	 */
	public String displayChosen(QDatum[] qData, boolean chemFormatting) {
		return displayChosen(qData, chemFormatting, null);
	} // displayChosen(QDatum[], boolean)
	
	/** Converts the chosen items into HTML format suitable for a short display.   
	 * @param	qData	the question data that the item numbers represent
	 * @param	chemFormatting	whether to use chemistry formatting
	 * @param	userLangs	non-English languages of the viewing user in order 
	 * of preference
	 * @return	the chosen items in HTML format
	 */
	public String displayChosen(QDatum[] qData, boolean chemFormatting,
			String[] userLangs) {
		final StringBuilder output = new StringBuilder();
		if (getNumChosen() == 0) { 
			Utils.appendTo(output, '[', PhraseTransln.translate(
					"none chosen", userLangs), ']');
		} else {
			Utils.appendTo(output, PhraseTransln.translate(
					"Chosen items", userLangs), ":<br>");
			for (final Integer chosenOpt : chosenOptions) {
				final int qdNum = chosenOpt.intValue() - 1;
				if (qdNum < qData.length) {
					Utils.appendTo(output, "&bull; ",
							qData[qdNum].toShortDisplay(chemFormatting),
							"<br>");
				} // if option is still available
			} // for each chosen option 
		} // if chosenOptions.size()
		return output.toString();
	} // displayChosen(QDatum[], boolean, String[])

	/** Adds choice items to a question statement of a fill-in-the-blank 
	 * question, incorporating them into pulldown menus.
	 * @param	qStmt	the original question statement, containing [[1, 3, 4]]
	 * to indicate a dropdown menu containing 1-based question data numbers 
	 * 1, 3, and 4.
	 * @param	qData	the question data that the item numbers represent
	 * @return	the modified question statement
	 */
	public String fillBlankToDisplay(String qStmt, QDatum[] qData) {
		return fillBlankToDisplay(qStmt, qData, ADD_MENUS, !CHEM_FORMATTING);
	} // fillBlankToDisplay(String, QDatum[])

	/** Adds choice items to a question statement of a fill-in-the-blank 
	 * question, perhaps incorporating them into pulldown menus.
	 * @param	qStmt	the original question statement, containing [[1, 3, 4]]
	 * to indicate a dropdown menu containing 1-based question data numbers 
	 * 1, 3, and 4.
	 * @param	qData	the question data that the item numbers represent
	 * @param	addMenus	incorporate the items into pulldown menus
	 * @param	chemFormatting when true, convert to chemistry 
	 * formatting
	 * @return	the modified question statement
	 */
	public String fillBlankToDisplay(String qStmt, QDatum[] qData, 
			boolean addMenus, boolean chemFormatting) {
		final String SELF = "Choice.fillBlankToDisplay: ";
		debugPrint(SELF + "qStmt: ", qStmt);
		String newQStmt = null;
		if (qData != null) {
		 	final int numQData = qData.length;
			final int[] optRange = new int[] {1, numQData};
			// final String[] dataItems = new String[numQData];
			newQStmt = qStmt;
			if (newQStmt.startsWith(PD_START)) {
				newQStmt = Utils.toString(' ', newQStmt);
			}
			final String[] qStmtParts = newQStmt.split(PD_BOUNDS);
			final StringBuilder qStmtBld = new StringBuilder();
			debugPrint(SELF + "chosenOptions = ", chosenOptions);
			for (int partNum = 0; partNum < qStmtParts.length; partNum++) {
				debugPrint(SELF + "partNum = ", partNum, ", qStmtParts[partNum]: ",
						qStmtParts[partNum]);
				if (partNum % 2 == 0) qStmtBld.append(chemFormatting
						? Utils.toDisplay(qStmtParts[partNum])
						: qStmtParts[partNum]);
				else {
					final int[] optNums = 
							getOrderedItemNums(qStmtParts[partNum]);
					debugPrint(SELF + "optNums in order of presentation = ", 
							optNums); 
					if (addMenus) {
						final int menuNum = (partNum + 1) / 2;
						Utils.appendTo(qStmtBld, 
								"<select class=\"big\" name=\"" + MENU,
								menuNum, "\">",
								"<option value=\"0\" id=\"" + BLANK,
								menuNum, "\"></option>");
						for (final int optNum : optNums) { // 1-based
							if (MathUtils.inRange(optNum, optRange)) {
								Utils.appendTo(qStmtBld, 
										"<option value=\"", optNum, 
										"\" id=\"" + MENU_ITEM,
										optNum, "\"");
								final boolean isChosen =
										chosenOptions.contains(
											Integer.valueOf(optNum));
								if (isChosen) {
									qStmtBld.append(" selected=\"selected\"");
								} // if option has been chosen
								Utils.appendTo(qStmtBld, '>',
										Utils.toPopupMenuDisplay(
											qData[optNum - 1].data),
										"</option>");
								debugPrint(SELF + "optNum = ", optNum, 
										", isChosen = ", isChosen,
										", qData[optNum - 1].data = ",
										qData[optNum - 1].data);
							} // if optNum is in range
						} // for each ddNum
						qStmtBld.append("</select>");
					} else {
						qStmtBld.append("<b>{</b><i>");
						boolean first = true;
						for (final int optNum : optNums) { // 1-based
							if (MathUtils.inRange(optNum, optRange)) {
								if (first) first = false;
								else qStmtBld.append(", ");
								qStmtBld.append(qData[optNum - 1].data);
							} // if optNum is in range
						} // for each Qdatum listed in this pulldown
						qStmtBld.append("</i><b>}</b>");
					} // if adding pulldowns
				} // if even
			} // for each part
			if (addMenus) {
				final int numMenus = qStmtParts.length / 2;
				qStmtBld.insert(0, "\"/>").insert(0, numMenus)
						.insert(0, "<input type=\"hidden\" id=\"" 
							+ NUM_MENUS + "\" value=\"");
			} // if adding pulldowns
			newQStmt = qStmtBld.toString();
		} else newQStmt = (chemFormatting ? Utils.toDisplay(qStmt) : qStmt);
		return newQStmt;
	} // fillBlankToDisplay(String, QDatum[], boolean, boolean)

	/** Gets the option numbers in the dropdown menu in the order given in 
	 * the response.
	 * @param	menuText	the option numbers as a comma-separated string
	 * @return	array of option numbers ordered as in the response
	 */
	private int[] getOrderedItemNums(String menuText) {
		final List<Integer> orderedItems = Utils.intArrayToList(options);
		final List<Integer> menuItemsList = 
				Utils.stringToIntList(menuText.split(","));
		// remove options from orderedItems if they are not in menu text;
		// items remaining in orderedItems were found in dd list
		for (int itemNum = orderedItems.size() - 1; itemNum >= 0; itemNum--) {
			final Integer item = orderedItems.get(itemNum);
			if (menuItemsList.contains(item)) { // found in menu; remove
				menuItemsList.remove(item);
			} else { // remove from ordered options
				orderedItems.remove(itemNum);
			} // if the option is found in this menu
		} // for each response option in order of presentation
		// append any unfound options in menu to orderedItems
		orderedItems.addAll(menuItemsList);
		return Utils.listToIntArray(orderedItems);
	} // getOrderedItemNums(String)

	/** Gets a string of options in the order in which they appear in a question
	 * statement.
	 * @param	qStmt	a question statement
	 * @return	colon-separated string representing the options
	 */
	public static String getOptions(String qStmt) {
		final StringBuilder optsBld = new StringBuilder();
		String stmt = qStmt;
		if (stmt.startsWith(PD_START)) stmt = " " + stmt;
		final String[] qStmtParts = stmt.split(PD_BOUNDS);
		for (int partNum = 1; partNum < qStmtParts.length; partNum += 2) {
			if (optsBld.length() > 0) optsBld.append(',');
			optsBld.append(qStmtParts[partNum]);
		} // for each pulldown menu
		final String optsStr = optsBld.toString();
		return optsStr.replaceAll(",[ ]*", SEPARATOR);
	} // getOptions(String)

	/** Returns a string with only the options in the received order.
	 * @return	string of the options
	 */
	public String getUnchosenString() {
		return Utils.join(options, SEPARATOR);
	} // getUnchosenString()

	/** Strips the student's choices from the response.
	 * @param	resp	the response
	 * @return	the response with all choices stripped
	 */
	public static String getUnchosenString(String resp) {
		final Choice choice = new Choice(resp);
		return choice.getUnchosenString();
	} // getUnchosenString(String)

	/** Builds an initialized string.
	 * @param	numOpts	number of options
	 * @param	scramble	whether to scramble the options
	 * @param	exceptLast	if scrambling, whether to leave the last option last
	 * @return	a string in the format 1:2:3: ..., maybe with options
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
			if (bld.length() > 0) bld.append(SEPARATOR);
			bld.append(opt);
		} // for each option
		return bld.toString();
	} // getInitialString(int, boolean, boolean)

	/** Builds an initialized string for a fill-in-the-blank question in which
	 * the options are scrambled.
	 * @param	numOpts	the number of options
	 * @param	qStmt	the question statement; contains information on which
	 * options can be scrambled with one another
	 * @return	the initialized response
	 */
	public static String getScrambledFillBlank(int numOpts, String qStmt) {
		final String SELF = "Choice.getScrambledFillBlank: ";
		debugPrint(SELF + "getting scrambled initial string for ", qStmt);
		final StringBuilder initBld = new StringBuilder();
		String stmt = qStmt;
		if (stmt.startsWith(PD_START)) stmt = " " + stmt;
		final String[] qStmtParts = stmt.split(PD_BOUNDS);
		final Pattern NUMBERS = Pattern.compile("[0-9]+");
		for (int partNum = 1; partNum < qStmtParts.length; partNum += 2) {
			final Matcher matchNums = NUMBERS.matcher(qStmtParts[partNum]);
			final ArrayList<Integer> options = new ArrayList<Integer>();
			// while we can find a number in the pulldown
			while (matchNums.find()) {
				final int qdNum = Integer.parseInt(matchNums.group());
				if (qdNum >= 1 && qdNum <= numOpts) {
					options.add(Integer.valueOf(qdNum));
				} // if valid index
			} // while we found a number
			Collections.shuffle(options);
			for (final Integer option : options) {
				if (initBld.length() > 0) initBld.append(Choice.SEPARATOR);
				initBld.append(option);
			} // for each option
		} // for each pulldown
		final String init = initBld.toString();
		debugPrint(SELF + "returning ", init);
		return init;
	} // getScrambledFillBlank(int, String)

} // Choice
