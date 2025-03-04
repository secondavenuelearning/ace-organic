package com.epoch.evals.impl.chemEvals.lewisEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.CompareNums;
import com.epoch.exceptions.ParameterException;
import com.epoch.lewis.LewisMolecule;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.List;

/** If the number of electron-deficient <i>X</i> atoms {is, is not} 
 * {=, &lt;, &gt;} <i>n</i> ... */
public class LewisElecDeficCt extends CompareNums 
		implements EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Number of atoms to compare against. */
	private int number;
	/** Element whose atoms will be tested for electron-deficiency. */
	private String element;

	/** Constructor. */
	public LewisElecDeficCt() {
		element = "X";
		setOper(NOT_EQUALS); // inherited from CompareNums
	} // LewisElecDeficCt() 

	/** Constructor. 
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>oper</code>/<code>number</code>/<code>element</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public LewisElecDeficCt(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 3) {
			setOper(Utils.indexOf(SYMBOLS, splitData[0]));
			number = MathUtils.parseInt(splitData[1]);
			element = splitData[2];
		} 
		if (splitData.length < 3 || getOper() == -1) {
			throw new ParameterException("LewisElecDeficCt "
					+ "ERROR: unknown input data '" + data + "'. ");
		}
	} // LewisElecDeficCt(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>  
	 * <code>oper</code>/<code>number</code>/<code>element</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(SYMBOLS[getOper()], '/', number, '/', element);
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
		return Utils.toString("If the number of electron-deficient ", 
				"X".equals(element) ? "" : element, " atoms is", 
				OPER_ENGLISH[FEWER][getOper()], number);
	} // toEnglish() 

	/** Determines whether the response has the indicated number of
	 * electron-deficient atoms of the indicated element.
	 * @param	response	a parsed response
	 * @param	authString	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified 
	 * with color or a message describing an inability to evaluate the 
	 * response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response, 
			String authString) { 
		final OneEvalResult evalResult = new OneEvalResult();
		final LewisMolecule lewis = (LewisMolecule) response.parsedResp;
		final List<Integer> violatingAtoms = 
				lewis.getElectronDeficientAtoms(element);
		final int numElecDefAtoms = violatingAtoms.size();
		evalResult.isSatisfied = compare(numElecDefAtoms, number);
		if (evalResult.isSatisfied && numElecDefAtoms != 0) {  
			for (final Integer violatingAtom : violatingAtoms) {
				lewis.highlight(violatingAtom.intValue());
			} // for each violating atom
			evalResult.modifiedResponse = lewis.toString();
		}  // if need to color the response
		return evalResult;
	} // isResponseMatching(Response, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[LEWIS_ELECTRON_DEFICIENT]; } 
	/** Gets the value of the number to compare.
	 * @return	value of the number to compare
	 */
	public int getNumber() 					{ return number; }
	/** Sets the value of the number to compare.
	 * @param	n	value of the number to compare
	 */
	public void setNumber(int n) 			{ number = n; } 
	/** Gets the element to test for electron-deficiency.
	 * @return	the element to test for electron-deficiency
	 */
	public String getElement() 				{ return element; }
	/** Sets the element to test for electron-deficiency.
	 * @param	l	the element to test for electron-deficiency
	 */
	public void setElement(String l) 		{ element = l; } 

} // LewisElecDeficCt
