package com.epoch.evals.impl.chemEvals.energyEvals;

import com.epoch.energyDiagrams.EDiagram;
import com.epoch.energyDiagrams.RCD;
import com.epoch.energyDiagrams.diagramConstants.RCDCellConstants;
import com.epoch.energyDiagrams.diagramConstants.RCDConstants;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.VerifyException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** If the response orbital energy diagram {matches, doesn't match} 
 * that of the author ... */
public class RCDDiff extends EDiagramDiff 
		implements EvalInterface, RCDCellConstants, RCDConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Value for compareExtent. */
	public static final int TYPE_CONNXN_LABEL = 0;
	/** Value for compareExtent. */
	public static final int TYPE_CONNXN = 1;
	/** Value for compareExtent. */
	public static final int TYPE_ONLY = 2;
	/** Database values for compareExtent. */
	transient final private String[] DB_COMPARE = new String[] {"FULL", "NO_LBL", "TYPE"};

	/** Constructor. */
	public RCDDiff() {
		energies = SIGNUMS;
		tolerance = 0; // immaterial for energies = SIGNUMS
		compareExtent = TYPE_CONNXN_LABEL;
	} // RCDDiff()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>oper</code>/<code>compareExtent</code>/<code>energies</code>/<code>tolerance</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public RCDDiff(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		if (splitData.length >= 4) { 
			isPositive = Utils.isPositive(splitData[0]);
			oper = MathUtils.parseInt(splitData[1], EXACTLY);
			compareExtent = Utils.indexOf(DB_COMPARE, splitData[2]);
			energies = Utils.indexOf(HOW_DB, splitData[3]);
			if (energies < 0) {
				throw new ParameterException("RCDDiff ERROR: "
						+ "unknown input data '" + data + "'. ");
			}
			if (splitData.length == 5)
				tolerance = MathUtils.parseInt(splitData[4]);
			debugPrint("RCDDiff.java: isPositive = ", isPositive,
					", compareExtent = ", extentEnglish(),
					", energies = ", energies, ", tolerance = ", tolerance);
		} else {
			throw new ParameterException("RCDDiff ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // RCDDiff(String)

	/** Gets the English for the compareExtent. 
	 * @return	English description of the compareExtent
	 */
	private String extentEnglish() {
		return extentEnglish(compareExtent);
	} // extentEnglish()

	/** Gets the English for the extent. 
	 * @param	extent	the extent
	 * @return	English description of the extent
	 */
	private String extentEnglish(int extent) {
		return (extent == TYPE_CONNXN_LABEL ? "TYPE_CONNXN_LABEL"
				: extent == TYPE_CONNXN ? "TYPE_CONNXN" : "TYPE_ONLY");
	} // extentEnglish(int)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>isPositive</code>/<code>oper</code>/<code>compareExtent</code>/<code>energies</code>/<code>tolerance</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		final StringBuilder data = Utils.getBuilder(isPositive ? "Y/" : "N/",
				oper, '/', DB_COMPARE[compareExtent], '/', HOW_DB[energies]);
		if (Utils.among(energies, RELATIVE_HEIGHT, FIXED_HEIGHT)) {
			Utils.appendTo(data, '/', tolerance);
		}
		return data.toString();
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
		final StringBuilder words = Utils.getBuilder(
				"If the response and author diagrams ",
				isPositive ? "have" : "do not have",
				oper == ATLEAST ? " at least" : " exactly",
				" the same minima and maxima in the same columns");
		if (Utils.among(compareExtent, TYPE_CONNXN_LABEL, TYPE_CONNXN))
			words.append(" with the same correlations");
		if (compareExtent == TYPE_CONNXN_LABEL) 
			words.append(" and the same labels");
		if (energies != ANY_E) {
			words.append(", with the ");
			if (Utils.among(energies, SIGNUMS, RELATIVE_HEIGHT)) {
				words.append("differences in ");
			} // if energies
			words.append("state energies ");
			if (energies == SIGNUMS) {
				words.append("having the same arithmetic signs");
			} else {
				words.append("being the same");
				if (tolerance > 0) {
					Utils.appendTo(words, " within ", tolerance, " row");
					if (tolerance != 1) words.append('s');
				} // if there's a tolerance
			} // if energies
		} // if energies
		return words.toString();
	 } // toEnglish()

	/** Determines whether the response table's cells contain the indicated text.
	 * @param	response	a parsed response
	 * @param	authDiagramStr	String representing the author's table
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authDiagramStr) {
		final String SELF = "RCDDiff.isResponseMatching: ";
		debugPrint(SELF, toEnglish());
		final OneEvalResult evalResult = new OneEvalResult();
		respDiagram = (EDiagram) response.parsedResp;
		final int rowCt = respDiagram.getNumRows();
		final int colCt = respDiagram.getNumCols();
		final RCD authRCD = new RCD(rowCt, colCt);
		try {
			authRCD.setStates(authDiagramStr, !THROW_IT);
		} catch (ParameterException e) { // won't happen
			debugPrint(SELF + "ParameterException");
		} catch (VerifyException e) {
			debugPrint(SELF + "VerifyException");
		}
		if (!Utils.among(authRCD.getError(), NO_ERROR, BAD_STATE)) {
			evalResult.verificationFailureString = "ACE could not evaluate the "
					+ "response because the author's reaction coordinate diagram "
					+ "was malformed. Please report this error to the programmers.";
			debugPrint(SELF + "author RCD error = ", authRCD.getError(), ": ",
					evalResult.verificationFailureString);
			return evalResult;
		} // try
		// Note: author diagram states out of range of response diagram
		// dimensions will be ignored
		authDiagram = (EDiagram) authRCD;
		evalResult.isSatisfied = isPositive;
		respOccupStrs = respDiagram.getOccupStrs(); // gets a copy
		respRowNums = respDiagram.getOccupRowNums();
		authOccupStrs = authDiagram.getOccupStrs(); // gets a copy
		authRowNums = authDiagram.getOccupRowNums();
		if (oper == EXACTLY) {
			// each column must have the same number of states in the two
			// diagrams
			for (int col = 1; col <= colCt; col++) {
				try {
					final int respSize = respDiagram.getNumOccupRows(col);
					final int authSize = authDiagram.getNumOccupRows(col);
					if (respSize != authSize) {
						debugPrint(SELF + "In column ", col, ", response has ",
								respSize, " energy level(s) with states, "
								+ "whereas author's has ", authSize);
						evalResult.isSatisfied = !isPositive;
						if (evalResult.isSatisfied) {
							evalResult.autoFeedback = new String[]
									{"There is an incorrect number of maxima or "
									+ "minima in column ***1*** of your diagram."};
							evalResult.autoFeedbackVariableParts = 
									new String[] {String.valueOf(col)};
						} // if need autofeedback
						return evalResult;
					} // if the diagrams differ in # of occupied energy levels in a column
				} catch (ParameterException e) { // unlikely
					Utils.alwaysPrint(SELF + "column ", col, "out of range.");
				} // try
			} // for each column
		} // if oper is EXACTLY
		// each maximum or minimum in a column at an energy level in author 
		// diagram should match one in response diagram
		for (int col = 1; col <= colCt; col++) {
			try {
				if (!statesMatch(col, compareExtent)) {
					debugPrint(SELF + "match in column ", col, " failed at extent ", 
							extentEnglish(), "; finding out what caused it to fail.");
					evalResult.isSatisfied = !isPositive;
					if (evalResult.isSatisfied && oper == EXACTLY) {
						int mismatchAt = TYPE_ONLY;
						for (int extent = compareExtent + 1; 
								extent <= TYPE_ONLY; extent++) {
							if (statesMatch(col, extent)) {
								mismatchAt = extent - 1;
								debugPrint(SELF + "match in column ", col, 
										" at extent ", extentEnglish(extent),
										", so mismatch occurs at extent ",
										extentEnglish(mismatchAt));
								break;
							} else {
								debugPrint(SELF + "mismatch in column ", col, 
										" at extent ", extentEnglish(extent));
							} // if there's a match at this extent
						} // for each extent
						evalResult.autoFeedback = new String[] 
								{mismatchAt == TYPE_CONNXN_LABEL ? 
									"Your diagram contains the correct "
									+ "number of minima and maxima, and "
									+ "they are connected properly, but "
									+ "column ***1*** of your diagram contains "
									+ "incorrectly labeled maxima or minima."
								: mismatchAt == TYPE_CONNXN ? 
									"Your diagram contains incorrect "
									+ "correlations between maxima and "
									+ "minima in adjacent columns."
								: "Column ***1*** of your diagram contains "
									+ "an incorrect number of maxima or minima."};
						if (mismatchAt != TYPE_CONNXN) {
							evalResult.autoFeedbackVariableParts = 
									new String[] {String.valueOf(col)};
						} // if the error occurs in a particular column
					} // if need autofeedback
					return evalResult;
				} // if all groups of response orbitals are found in author's diagram
				debugPrint(SELF + "all author cells in column ",
						col, " have a match in response diagram",
						(oper == EXACTLY ? ", and vice versa." : "."));
			} catch (ParameterException e) { // unlikely
				Utils.alwaysPrint(SELF + "caught ParameterException "
						+ "for column ", col);
			} // try
		} // for each column
		// compare positions of orbitals
		if (compareExtent != TYPE_CONNXN_LABEL) {
			reduceAllCols(authOccupStrs);
			reduceAllCols(respOccupStrs); // modifies the copy
		} // if not comparing labels
		return (energies == ANY_E ? evalResult
				: energies == FIXED_HEIGHT ? fixedHeights(colCt, "maxima or minima")
				: relativeEnergies(colCt, "maxima or minima"));
	} // isResponseMatching(Response, String)

	/** Determines whether in a column, every maximum or minimum and
	 * its label in the author's diagram exists also in the response.
	 * @param	col	1-based column number
	 * @param	extent	whether to compare states and labels or just states
	 * @throws	ParameterException	if column is out of range
	 * @return	true if every state in a column of the 
	 * author's diagram can be found in the response diagram
	 */
	private boolean statesMatch(int col, int extent) 
			throws ParameterException {
		final String SELF = "RCDDiff.statesMatch: ";
		boolean match = true;
		// next two lines get copies
		final List<String> respColCells = respDiagram.getOccupStrs(col);
		final List<String> authColCells = authDiagram.getOccupStrs(col);
		if (extent != TYPE_CONNXN_LABEL) {
			debugPrint(SELF + "reducing column ", col, " author cells ", 
					authColCells, " and response cells ", respColCells);
			reduce(authColCells);
			reduce(respColCells);
		} // if extent
		debugPrint(SELF + "comparing column ", col, " author cells ", authColCells, 
				" to response cells ", respColCells);
		final List<String> authUnknownStates = new ArrayList<String>();
		for (int authCellNum = 0; authCellNum < authColCells.size(); authCellNum++) {
			final String authCell = authColCells.get(authCellNum);
			if (authCell.startsWith(String.valueOf(UNKNOWN))) {
				debugPrint(SELF + "author's cell ", authCell, 
						" in column ", col, ", row ", 
						authDiagram.getOccupRowNums(col).get(authCellNum),
						" has an unknown state.");
				authUnknownStates.add(authCell);
			} else {
				final int locn = respColCells.indexOf(authCell);
				if (locn >= 0) respColCells.remove(locn);
				else {
					debugPrint(SELF + "author's cell ", authCell, 
							" in column ", col, ", row ", 
							authDiagram.getOccupRowNums(col).get(authCellNum),
							" doesn't have a match in response diagram.");
					match = false;
					break;
				} // if author's cell is found in response diagram
			} // if author state is known
		} // for each author's state in the column
		if (!authUnknownStates.isEmpty()) {
			for (int respCellNum = 0; 
					respCellNum < respColCells.size(); respCellNum++) {
				String respCell = respColCells.get(respCellNum);
				final String[] cellParts = respCell.split(STATE_LABEL_SEP);
				cellParts[STATE] = String.valueOf(UNKNOWN);
				respCell = Utils.join(cellParts, STATE_LABEL_SEP);
				respColCells.set(respCellNum, respCell);
			} // for each response cell remaining
			debugPrint(SELF + "changed remaining resp cells to unknown states: ",
					respColCells, "; comparing to: ", authUnknownStates);
			for (final String authCell : authUnknownStates) {
				final int locn = respColCells.indexOf(authCell);
				if (locn >= 0) respColCells.remove(locn);
				else {
					debugPrint(SELF + "author's cell ", authCell, 
							" in column ", col, ", with unknown state ", 
							" doesn't have a match in response diagram.");
					match = false;
					break;
				} // if author's cell is found in response diagram
			} // for each author's state in the column
		} // if there are cells with unknown states
		if (match && Utils.among(extent, TYPE_CONNXN, TYPE_CONNXN_LABEL)) {
			debugPrint(SELF + "states match, checking correlations.");
			match = correlationsOK();
		} // if need to check orbital correlations
		return match;
	} // statesMatch(int, boolean)

	/** Modifies String representations of occupied cells to represent 
	 * states only (removes labels).
	 * @param	cellStrs	list of String representations of occupied 
	 * cells in a column -- modified by the method!
	 */
	private void reduce(List<String> cellStrs) {
		final String SELF = "RCDDiff.reduce: ";
		final List<String> colTypes = new ArrayList<String>();
		for (final String cellStr : cellStrs) {
			final String[] cellParts = cellStr.split(STATE_LABEL_SEP);
			colTypes.add(cellParts[STATE]);
		} // for each occupied cell in this column
		cellStrs.clear();
		cellStrs.addAll(colTypes);
	} // reduce(List<String>)

	/** Modifies String representations of occupied cells to represent 
	 * states only (removes labels).
	 * @param	allCellStrs	lists of String representations of occupied 
	 * cells in all columns -- modified by the method!
	 */
	private void reduceAllCols(List<ArrayList<String>> allCellStrs) {
		final String SELF = "RCDDiff.reduceAllCols: ";
		debugPrint(SELF + "reducing ", allCellStrs);
		for (final List<String> cellStrs : allCellStrs) {
			reduce(cellStrs);
		} // for each column
		debugPrint(SELF + "reduced to ", allCellStrs);
	} // reduceAllCols(List<ArrayList<String>>)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() { return EVAL_CODES[RCD_DIFF]; }

} // RCDDiff

