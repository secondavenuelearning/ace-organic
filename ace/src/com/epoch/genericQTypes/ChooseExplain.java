package com.epoch.genericQTypes;

import com.epoch.exceptions.ParameterException;
import com.epoch.utils.Utils;

/** Holds a response to a choose-and-explain question. */
public class ChooseExplain {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** The multiple-choice part of the response. */
	transient public Choice choice;
	/** The text part of the response. */
	transient public String text = "";
	/** Separates the Choice and text parts of the response. */
	public static final char SEPARATOR = '\t';

	/** Constructor. 
	 * @param	respStr	String containing the choice and text responses
	 * separated by the separator.
	 * @throws	ParameterException	if response is null or there is no separator
	 * or there is no choice string
	 * @throws	NumberFormatException	if numbers can't be parsed
	 */
	public ChooseExplain(String respStr) throws ParameterException {
		final int posn = getSeparatorPosn(respStr);
		choice = new Choice(respStr.substring(0, posn));
		if (posn < respStr.length() - 1) {
			text = Utils.condenseWhitespace(respStr.substring(posn + 1));
		} // if there is text
	} // ChooseExplain(String)

	/** Constructor. 
	 * @param	respStr	String containing the choice and text responses
	 * separated by the separator.
	 * @param	numOptions	the number of options in the question
	 * @throws	ParameterException	if response is null or there is no separator
	 * or there is no choice string
	 */
	public ChooseExplain(String respStr, int numOptions) 
			throws ParameterException {
		final int posn = getSeparatorPosn(respStr);
		choice = new Choice(respStr.substring(0, posn), numOptions);
		if (posn < respStr.length() - 1) {
			text = Utils.condenseWhitespace(respStr.substring(posn + 1));
		} // if there is text
	} // ChooseExplain(String, int)

	/** Gets the position of the separator between the choice and text parts of
	 * the response. 
	 * @param	respStr	String containing the choice and text responses
	 * separated by the separator.
	 * @return	the position of the separator
	 * @throws	ParameterException	if response is null or there is no separator
	 * or there is no choice string
	 */
	private int getSeparatorPosn(String respStr) throws ParameterException {
		if (respStr == null) throw new ParameterException("ACE could not "
				+ "analyze your response to this question.  Please report "
				+ "this error to the programmers.");
		final int posn = respStr.indexOf(SEPARATOR);
		if (posn <= 0) throw new ParameterException("ACE could not analyze "
				+ "your response to this question.  Please report this error "
				+ "to the programmers.");
		return posn;
	} // getSeparatorPosn(String)

	/** Strips the student's choices from the response.
	 * @param	resp	the response
	 * @return	the response with all choices stripped
	 * @throws	ParameterException	if response can't be parsed
	 */
	public static String getUnchosenString(String resp) 
			throws ParameterException {
		final ChooseExplain parsed = new ChooseExplain(resp);
		return Utils.toString(parsed.choice.getUnchosenString(), SEPARATOR);
	} // getUnchosenString(String)

	/** Builds an initialized string.
	 * @param	numOpts	number of options
	 * @param	scramble	whether to scramble the options
	 * @param	exceptLast	if scrambling, whether to leave the last option last
	 * @return	a string in the format 1:2:3:...[tab], maybe with options
	 * scrambled
	 */
	public static String getInitialString(int numOpts, boolean scramble,
			boolean exceptLast) {
		return Utils.toString(
				Choice.getInitialString(numOpts, scramble, exceptLast),
				SEPARATOR);
	} // getInitialString(int, boolean, boolean)

} // ChooseExplain
