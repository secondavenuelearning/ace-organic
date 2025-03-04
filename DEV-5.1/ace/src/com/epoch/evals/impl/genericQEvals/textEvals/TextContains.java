package com.epoch.evals.impl.genericQEvals.textEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.TextAndNumbers;
import com.epoch.evals.impl.genericQEvals.textEvals.textEvalConstants.TextConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.ChooseExplain;
import com.epoch.genericQTypes.Logic;
import com.epoch.responses.Response;
import com.epoch.utils.Utils;
import java.util.Locale;

/** If the response {is, begins with, ends with, contains, contains internally,
 * contains the regular expression} "..." ... */
public class TextContains extends TextAndNumbers 
		implements EvalInterface, TextConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Constructor. */
	public TextContains() {
		isPositive = false;
		where = IS;
		ignoreCase = true;
	} // TextContains()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>where</code>/<code>ignoreCase</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public TextContains(String data) throws ParameterException {
		debugPrint("TextContains.java: data = ", data);
		final String[] splitData = data.split("/");
		if (splitData.length >= 3) { 
			isPositive = Utils.isPositive(splitData[0]); // inherited from TextAndNumbers
			where = Utils.indexOf(WHERE, splitData[1]); // inherited from TextAndNumbers
			ignoreCase = Utils.isPositive(splitData[2]); // inherited from TextAndNumbers
		}
		if (splitData.length < 3 || where == -1) {
			throw new ParameterException("TextContains ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // TextContains(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>isPositive</code>/<code>where</code>/<code>ignoreCase</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(isPositive ? "Y/" : "N/", WHERE[where],
				ignoreCase ? "/Y" : "/N");
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
		final StringBuilder words = Utils.getBuilder("If the response");
		if (!isPositive) { 
			Utils.appendTo(words, " does not", WHERE_ENGLISH[where]);
		} else {
			final int posn = WHERE_ENGLISH[where].indexOf(" ", 1);
			Utils.appendTo(words, WHERE_ENGLISH[where].substring(0, posn),
					where == MATCHES_REGEX ? "es" : 's',
					WHERE_ENGLISH[where].substring(posn));
		} // if isPositive
		if (!Utils.among(where, MATCHES_REGEX, CONT_REGEX) && ignoreCase) {
			words.append("(ignoring case) ");
		} // if ignoring case in regex
		Utils.addSpanString(words, strName, !TO_DISPLAY);
		debugPrint("TextContains.toEnglish: ", words);
		return words.toString();
	 } // toEnglish()

	/** Determines whether the response contains the indicated text.
	 * @param	response	a parsed response
	 * @param	authString	string to which to compare the response
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "TextContains.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		final String resp = (response.parsedResp instanceof String
				? (String) response.parsedResp
				: response.parsedResp instanceof Logic
				? Utils.join(((Logic) response.parsedResp).getStatements(), " ")
				: ((ChooseExplain) response.parsedResp).text);
		debugPrint(SELF + "original strings:\nresp = ", resp, 
				"\nref = ", authString);
		String respMod = Utils.cersToUnicode(resp);
		String ref = Utils.cersToUnicode(
				authString.trim().replaceAll("\\s+", " "));
		if (ignoreCase) {
			ref = ref.toLowerCase(Locale.US);
			respMod = respMod.toLowerCase(Locale.US);
		}
		if (where == CONT_REGEX) {
			ref = Utils.toString(".*", ref, ".*");
		}
		boolean found = false;
		switch (where) {
			case IS: 			found = respMod.equals(ref); break;
			case STARTS: 		found = respMod.startsWith(ref); break;
			case ENDS: 			found = respMod.endsWith(ref); break;
			case CONTAINS: 		found = respMod.indexOf(ref) >= 0; break;
			case IS_SUBSTRING:	found = ref.indexOf(respMod) >= 0; break;
			case CONT_INTERNAL:	found = respMod.indexOf(ref) > 0
										&& !respMod.endsWith(ref); break;
			case MATCHES_REGEX:	
			case CONT_REGEX:	found = respMod.matches(ref); break;
			default:			found = respMod.equals(ref); break; // can't happen
		}
		evalResult.isSatisfied = found == isPositive;
		debugPrint(SELF + "respMod = ", respMod,
				", author string = ", ref, 
				", where = ", WHERE[where],
				", found = ", found, 
				", isPositive = ", isPositive, 
				", returning ", evalResult.isSatisfied);
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[TEXT_CONT]; }
	/** Required by interface.  Sets a possibly shortened version of the 
	 * test string for display.
	 * @param	str	the test string (maybe shortened)
	 */
	public void setMolName(String str) 		{ strName = str; } 

} // TextContains

