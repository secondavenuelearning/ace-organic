package com.epoch.evals.impl.chemEvals.lewisEvals;

import chemaxon.struc.MolAtom;
import com.epoch.chem.ChemUtils;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.CompareNums;
import com.epoch.exceptions.ParameterException;
import com.epoch.lewis.lewisConstants.LewisConstants;
import com.epoch.lewis.LewisMolecule;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** If the number of electrons in the outer shell
 * <ul>
 * <li>of every atom is not greater than the maximum ...
 * <li>of any atom is greater than the maximum ...
 * <li>of {any, every} atom of element <i>X</i> is {=, &lt;, &gt;}
 * <i>n</i> ...
 * </ul>
 */
public class LewisOuterNumber extends CompareNums 
		implements EvalInterface, LewisConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** How to count the outer electrons: any element, every element, or just
	 * element X. */
	private int condNumber;
		/** Value for condNumber.  */
		public static final int EVERY_NOT_GREATER = 1;
		/** Value for condNumber.  */
		public static final int ANY_GREATER = 2;
		/** Value for condNumber.  */
		public static final int ELEMENT_OPER = 3;
	/** When condNumber != ELEMENT_OPER, whether any atom is more than the
	 * maximum (false) or every atom is less than or equal to the maximum (true). */
	private boolean isPositive;
		/** Value for isPositive.  */
		public static final boolean ANY = false;  // WRONG
		/** Value for isPositive.  */
		public static final boolean EVERY = true; // RIGHT
	/** When condNumber == ELEMENT_OPER, element for which to count outer
	 * electrons. */
	private String element;
	/** When condNumber == ELEMENT_OPER, number of electrons to compare against.
	 */
	private int number;

	/** Constructor. */
	public LewisOuterNumber() {
		setOper(NOT_EQUALS); // inherited from CompareNums
		isPositive = ANY;
		element = "C";
		condNumber = ELEMENT_OPER;
	} // LewisOuterNumber()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>oper</code>/<code>number</code>/<code>element</code>/<code>anyEvery</code>/<code>condNumber</code>
	 * <br>where: 
	 * <br><code>condNumber</code> is 1 (every atom &le; maximum), 2 (any atom
	 * &gt; the maximum), or 3 ([any/every] [element] [oper] [number]);
	 * <br><code>number</code> is -1 for "the maximum", a positive integer
	 * otherwise;
	 * <br><code>element</code> may be X or a specific element;
	 * <br><code>anyEvery</code> is A (any atom) or E (every atom).
	 * <br>
	 * <table>
	 * <caption>Examples</caption>
	 * <tr><td>Y&gt;/4/C/A/3</td><td>if number of electrons of any of the C atoms 
	 * is greater than 4</td></tr>
	 * <tr><td>N=/4/C/E/3</td><td>if number of electrons of every C atom is not 
	 * equal to 4</td></tr>
	 * <tr><td>N&gt;/-1/X/E/1</td><td>if every atom is not greater than the 
	 * maximum</td></tr>
	 * </table>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public LewisOuterNumber(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 5) {
			setOper(Utils.indexOf(SYMBOLS, splitData[0]));
			number = MathUtils.parseInt(splitData[1]);
			element = splitData[2];
			isPositive = ("E".equals(splitData[3]) ? EVERY : ANY);		
			condNumber = MathUtils.parseInt(splitData[4]);
		} 
		if (splitData.length < 5 || getOper() == -1) {
			throw new ParameterException("LewisOuterNumber ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // LewisOuterNumber(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>oper</code>/<code>number</code>/<code>element</code>/<code>anyEvery</code>/<code>condNumber</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return Utils.toString(SYMBOLS[getOper()], '/', number, '/', element,
				isPositive == EVERY ? "/E/" : "/A/", condNumber);
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
		/*  any   atom is not equal to 5
			every atom is less than	3
			any   atom is greater than the maximum */
		return Utils.toString("If the number of electrons in the outer "
				+ "shell of ", condNumber == ELEMENT_OPER
					? Utils.getBuilder(isPositive == EVERY ? "every " : "any ",
						element, " atom is",
						OPER_ENGLISH[FEWER][getOper()], number)
					: Utils.getBuilder(condNumber == EVERY_NOT_GREATER
							? "every atom is not" : "any atom is",
						" greater than the maximum"));
	 } // toEnglish()

	/** Determines whether the response satisfies the evaluator.
	 * @param	response	a parsed response
	 * @param	authStruct	null (required by interface)
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a response modified
	 * with color or a message describing an inability to evaluate the
	 * response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authStruct) {
		OneEvalResult evalResult;
		final LewisMolecule lewis = (LewisMolecule) response.parsedResp;
		evalResult = (condNumber == ELEMENT_OPER 
				? outerNumberCompare(lewis, authStruct)
				: outerCountViolation(lewis, authStruct));
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Is the total electron count of any atom &gt; maximum?
	 * @param	respLewis	the Lewis molecule
	 * @param	authStruct	author's structure in MOL format
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain
	 * a response modified with color or a message describing an inability
	 * to evaluate the response because it was malformed.
	 */
	private OneEvalResult outerCountViolation(LewisMolecule respLewis,
			String authStruct) {
		final OneEvalResult evalResult = new OneEvalResult();
		final List<Integer> violatingAtoms = new ArrayList<Integer>();
		for (int atomNum = 1; atomNum <= respLewis.getNumAtoms(); atomNum++) {
			final MolAtom atom = respLewis.getAtom(atomNum);
			final int maxOuterElec = ChemUtils.getMaxOuterElectrons(atom);
			final int numUnsharedElectrons =
					respLewis.getUnsharedElectrons(atomNum);
			final int totalNumBonds =
					respLewis.getSumBondOrders(atomNum);
			final int outerElectrons = 
					numUnsharedElectrons + (totalNumBonds * 2);
			debugPrint("By nature atom ", atom, atomNum, " can have ", 
					maxOuterElec, " maximum outer electrons;",
					" it has ", outerElectrons, " outer electrons");
			if (outerElectrons > maxOuterElec) {
				violatingAtoms.add(Integer.valueOf(atomNum));
			} // if outerElectrons > maxOuterElec
		} // for each atom atomNum
		final int numViolations = violatingAtoms.size();
		debugPrint("There are ", numViolations, " violating atoms.");
		if (isPositive == ANY && numViolations > 0) {
			evalResult.isSatisfied = true;
			for (final Integer violatingAtom : violatingAtoms) {
				respLewis.highlight(violatingAtom.intValue());
			} // for each violating atom
			evalResult.modifiedResponse = respLewis.toString();
		} else evalResult.isSatisfied =
				(isPositive == EVERY && numViolations == 0);
		return evalResult;
	} // outerCountViolation(LewisMolecule, String)
	
	/** Is the total electron count of every atom of element <i>X</i>
	 * {&lt;, &gt;, =} <i>n</i>?
	 * @param	respLewis	the Lewis molecule
	 * @param	authStruct	author's structure in MOL format
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain
	 * a response modified with color or a message describing an 
	 * inability to evaluate the response because it was malformed.
	 */
	private OneEvalResult outerNumberCompare(LewisMolecule respLewis,
			String authStruct) {
		final OneEvalResult evalResult = new OneEvalResult();
		final List<Integer> satisfyingAtoms = new ArrayList<Integer>();
		final List<int[]> outerElecsList =
				respLewis.getOuterElectronsList(element);
		for (final int[] outerElecs : outerElecsList) {
			final int electronCount = outerElecs[OUTER_ELECS];
			final boolean isSatisfied = compare(electronCount, number);
			debugPrint("LewisOuterNumber: Actual electron count of atom ",
					element, outerElecs[ATOM_INDEX], " = ", electronCount, 
					", expected = ", number, "; isSatisfied = ", isSatisfied);
			if (isSatisfied)
				satisfyingAtoms.add(Integer.valueOf(outerElecs[ATOM_INDEX]));
		} // for each atom whose outer electrons were measured
		final int numSatisfying = satisfyingAtoms.size();
		debugPrint("LewisOuterNumber: There are ", numSatisfying, 
				" satisfying atoms.");
		evalResult.isSatisfied = numSatisfying > 0;
		if (numSatisfying > 0) {
			for (final Integer satisfyingAtom : satisfyingAtoms) {
				respLewis.highlight(satisfyingAtom.intValue());
			} // for each satisfying atom
			evalResult.modifiedResponse = respLewis.toString();
		} // if there are satisfying atoms
		return evalResult;
	} // outerNumberCompare(LewisMolecule, String)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 			{ return EVAL_CODES[LEWIS_OUTER_SHELL_COUNT]; } 
	/** Gets the value of the number to compare.
	 * @return	value of the number to compare
	 */
	public int getNumber() 					{ return number; }
	/** Sets the value of the number to compare.
	 * @param	n	value of the number to compare
	 */
	public void setNumber(int n) 			{ number = n; }
	/** Gets the value of the element whose outer electrons to count.
	 * @return	value of the element whose outer electrons to count
	 */
	public String getElement() 				{ return element; }
	/** Sets the element whose outer electrons to count.
	 * @param	l	the element whose outer electrons to count
	 */
	public void setElement(String l) 		{ element = l; }
	/** Gets whether the evaluator is satisfied by every atom less than or equal
	 * to the maximum or any atom more than the maximum.
	 * @return	true if the evaluator is satisfied by every atom less than or
	 * equal to the maximum
	 */
	public boolean getIsPositive() 			{ return isPositive; }
	/** Sets whether the evaluator is satisfied by every atom less than or equal
	 * to the maximum or any atom more than the maximum.
	 * @param	pos	true if the evaluator is satisfied by every atom less than
	 * the maximum
	 */
	public void setIsPositive(boolean pos)	{ isPositive = pos; }
	/** Gets how to count the outer electrons.
	 * @return	how to count the outer electrons
	 */
	public int getCondNumber() 				{ return condNumber; }
	/** Sets how to count the outer electrons.
	 * @param	theCondition	how to count the outer electrons
	 */
	public void setCondNumber(int theCondition) { condNumber = theCondition; }

} // LewisOuterNumber
