package com.epoch.evals.impl.chemEvals.energyEvals;

import com.epoch.energyDiagrams.CellsLine;
import com.epoch.energyDiagrams.EDiagram;
import com.epoch.energyDiagrams.OED;
import com.epoch.energyDiagrams.OEDCell;
import com.epoch.energyDiagrams.diagramConstants.OEDCellConstants;
import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.exceptions.ParameterException;
import com.epoch.responses.Response;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** If the response orbital energy diagram {matches, doesn't match} 
 * that of the author ... */
public class OEDDiff extends EDiagramDiff 
		implements EvalInterface, OEDCellConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Value for compareExtent. */
	public static final int TYPE_CT_OCCUP_LINE_LABEL = 0;
	/** Value for compareExtent. */
	public static final int TYPE_CT_OCCUP_LINE = 1;
	/** Value for compareExtent. */
	public static final int TYPE_CT_OCCUP = 2;
	/** Value for compareExtent. */
	public static final int TYPE_CT = 3;
	/** Value for compareExtent. */
	public static final int TYPE_ONLY = 4;
	/** Database values for compareExtent. */
	transient final private String[] DB_COMPARE = new String[] 
			{"FULL", "NO_LBL", "OCCUPS", "TYPE_NUM", "TYPE"};

	/** Constructor. */
	public OEDDiff() {
		energies = SIGNUMS;
		tolerance = 0; // immaterial for energies = SIGNUMS
		compareExtent = TYPE_CT_OCCUP_LINE_LABEL;
	} // OEDDiff()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>isPositive</code>/<code>oper</code>/<code>compareExtent</code>/<code>energies</code>/<code>tolerance</code>
	 * @throws	ParameterException	if the coded data is inappropriate for this
	 * evaluator
	 */
	public OEDDiff(String data) throws ParameterException {
		debugPrint("OEDDiff.java: data = ", data);
		final String[] splitData = data.split("/");
		if (splitData.length >= 4) { 
			isPositive = Utils.isPositive(splitData[0]);
			oper = MathUtils.parseInt(splitData[1], EXACTLY);
			compareExtent = Utils.indexOf(DB_COMPARE, splitData[2]);
			energies = Utils.indexOf(HOW_DB, splitData[3]);
			if (energies < 0) {
				throw new ParameterException("OEDDiff ERROR: "
						+ "unknown input data '" + data + "'. ");
			}
			if (splitData.length == 5)
				tolerance = MathUtils.parseInt(splitData[4]);
			debugPrint("OEDDiff.java: isPositive = ", isPositive,
					", compareExtent = ", compareExtent,
					", energies = ", energies, ", tolerance = ", tolerance);
		} else {
			throw new ParameterException("OEDDiff ERROR: "
					+ "unknown input data '" + data + "'. ");
		}
	} // OEDDiff(String)

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
				compareExtent == TYPE_CT_OCCUP_LINE_LABEL 
						? " the same orbitals, occupancies, correlations, "
							+ "and labels"
					: compareExtent == TYPE_CT_OCCUP_LINE 
						? " the same orbitals, occupancies, and correlations"
					: compareExtent == TYPE_CT_OCCUP 
						? " the same orbitals and occupancies"
					: compareExtent == TYPE_CT 
						? " the same number of orbitals of each type"
					: "the same types of orbitals",
				" in the same columns");
		if (energies != ANY_E) {
			words.append(", with the ");
			if (Utils.among(energies, SIGNUMS, RELATIVE_HEIGHT)) {
				words.append("differences in ");
			} // if energies
			words.append("orbital energies ");
			if (energies == SIGNUMS) {
				words.append("having the same arithmetic signs");
			} else {
				words.append("being the same");
				if (tolerance > 0) {
					Utils.appendTo(words, 
							" within ", tolerance, " row");
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
		final String SELF = "OEDDiff.isResponseMatching: ";
		final OneEvalResult evalResult = new OneEvalResult();
		respDiagram = (EDiagram) response.parsedResp;
		final int rowCt = respDiagram.getNumRows();
		final int colCt = respDiagram.getNumCols();
		final OED authOED = new OED(rowCt);
		authOED.setOrbitals(authDiagramStr);
		// Note: author diagram orbitals out of range of response diagram
		// dimensions will be ignored
		authDiagram = (EDiagram) authOED;
		evalResult.isSatisfied = isPositive;
		// get the strings describing occupied cells, reduce if necessary
		debugPrint(SELF + toEnglish());
		respOccupStrs = respDiagram.getOccupStrs(); // gets a copy
		authOccupStrs = authDiagram.getOccupStrs(); 
		respRowNums = respDiagram.getOccupRowNums();
		authRowNums = authDiagram.getOccupRowNums();
		if (oper == EXACTLY) {
			// each column must have the same number
			// of occupied rows in each diagram 
			for (int col = 1; col <= colCt; col++) {
				try {
					final int respSize = respDiagram.getNumOccupRows(col);
					final int authSize = authDiagram.getNumOccupRows(col);
					if (respSize != authSize) {
						debugPrint(SELF + "In column ", col, ", response has ",
								respSize, " energy level(s) with orbitals, "
								+ "whereas author's has ", authSize);
						evalResult.isSatisfied = !isPositive;
						if (evalResult.isSatisfied) {
							evalResult.autoFeedback = new String[]
									{"The number of orbital-containing energy "
									+ "levels in column ***1*** of your "
									+ "diagram is incorrect." };
							evalResult.autoFeedbackVariableParts = 
									new String[] {String.valueOf(col)};
						} // if need autofeedback
						return evalResult;
					} // if the diagrams differ in # of occupied rows
				} catch (ParameterException e) { // unlikely
					Utils.alwaysPrint(SELF + "caught ParameterException "
							+ "for column ", col);
				} // try
			} // for each column
		} // if oper is EXACTLY
		// each group of orbitals in a column at an energy level in author 
		// diagram should match one in response diagram
		for (int col = 1; col <= colCt; col++) {
			try {
				if (!orbsMatch(col, compareExtent)) {
					evalResult.isSatisfied = !isPositive;
					debugPrint(SELF + "no match at extent ", 
							extentText(compareExtent));
					if (evalResult.isSatisfied && oper == EXACTLY) {
						// find to what extent the mismatch holds
						int mismatchAt = TYPE_ONLY;
						for (int extent = compareExtent + 1; 
								extent <= TYPE_ONLY; extent++) {
							if (orbsMatch(col, extent)) {
								debugPrint(SELF + "match at extent ", 
										extentText(extent), "; mismatch "
										+ "due to problem at extent ", 
										extentText(extent - 1));
								mismatchAt = extent - 1;
								break;
							} else {
								debugPrint(SELF + "no match at extent ", 
										extentText(extent), " either");
							} // if there's a match at this extent
						} // for each extent
						if (mismatchAt == TYPE_CT_OCCUP_LINE && energies != ANY_E) {
							debugPrint(SELF + "checking whether mismatch "
									+ "at extent ", extentText(mismatchAt), 
									" is due to a problem other than correlations.");
							for (int extent = TYPE_CT_OCCUP; 
									extent <= TYPE_ONLY; extent++) {
								reduceAllCols(authOccupStrs, extent);
								reduceAllCols(respOccupStrs, extent); // modifies the copy
								final OneEvalResult energyMeasure = 
										relativeEnergies(colCt, "orbitals");
								if (energyMeasure.isSatisfied == isPositive) {
									debugPrint(SELF + "match at extent ", 
											extentText(extent), "; mismatch "
											+ "due to problem at extent ", 
											extentText(extent - 1));
									mismatchAt = extent - 1;
									break;
								} else {
									debugPrint(SELF + "mismatch remains at extent ", 
											extentText(extent));
								} // if relative energies of orbitals are OK
							} // for each extent
						} // if mismatch from line correlations, & energies are relevant
						setAutoFeedback(evalResult, mismatchAt, col);
						debugPrint(SELF, evalResult.autoFeedback);
					} // if need autofeedback
					return evalResult;
				} // if all groups of response orbitals are found in author's diagram
				debugPrint(SELF + "all response cells in column ",
						col, " have a match in author's diagram at extent ",
						extentText(compareExtent));
			} catch (ParameterException e) { // unlikely
				Utils.alwaysPrint(SELF + "caught ParameterException "
						+ "for column ", col);
			} // try
		} // for each column
		// compare positions of orbitals
		reduceAllCols(authOccupStrs, compareExtent);
		reduceAllCols(respOccupStrs, compareExtent); // modifies the copy
		return (energies == ANY_E ? evalResult
				: energies == FIXED_HEIGHT ? fixedHeights(colCt, "orbitals")
				: relativeEnergies(colCt, "orbitals"));
	} // isResponseMatching(Response, String)

	/** Sets the autofeedback for the extent to which a response fails to match
	 * the author's diagram.
	 * @param	evalResult	the OneEvalResult being returned
	 * @param	extent	extent to which the diagrams don't match
	 * @param	col	the column in which the error occurred
	 */
	private void setAutoFeedback(OneEvalResult evalResult, int extent, int col) {
		evalResult.autoFeedback = new String[] 
				{extent == TYPE_CT_OCCUP_LINE_LABEL ? 
					"Column ***1*** of your response contains "
					+ "the right number "
					+ "of orbitals of each type, and the number "
					+ "of electrons in each orbital is correct, "
					+ "and the orbitals are correlated correctly, "
					+ "but the column contains an orbital or "
					+ "group of orbitals that is labeled "
					+ "incorrectly. "
				: extent == TYPE_CT_OCCUP_LINE ? 
					"Your diagram contains incorrect "
					+ "correlations between orbitals. "
				: extent == TYPE_CT_OCCUP ? 
					"Column ***1*** of your response contains "
					+ "the right number of orbitals of each "
					+ "type, but it contains an orbital or "
					+ "group of orbitals containing an "
					+ "incorrect number of electrons."
		 		: extent == TYPE_CT ?
					"Column ***1*** of your response contains "
					+ "the right "
					+ "types of orbitals, but it contains "
					+ "an incorrect number of orbitals "
					+ "in a group of the correct type."
				: "Column ***1*** of your response contains "
					+ "an orbital or group of orbitals "
					+ "of the wrong type."};
		if (extent != TYPE_CT_OCCUP_LINE) {
			evalResult.autoFeedbackVariableParts = 
					new String[] {String.valueOf(col)};
		} // if error is not in cross-column correlations
	} // setAutoFeedback(OneEvalResult)

	/** Determines whether in a column, every group of orbitals and
	 * occupancies in the author's diagram exists also in the response.
	 * @param	col	1-based column number
	 * @param	extent	whether to compare information on occupancies and 
	 * number of orbitals
	 * @throws	ParameterException	if column is out of range
	 * @return	true if every group of orbitals in a column of the 
	 * author's diagram can be found in the response diagram
	 */
	private boolean orbsMatch(int col, int extent) throws ParameterException {
		final String SELF = "OEDDiff.orbsMatch: ";
		boolean match = true;
		// next two lines get copies
		final List<String> respColCells = respDiagram.getOccupStrs(col);
		final List<String> authColCells = authDiagram.getOccupStrs(col);
		if (extent != TYPE_CT_OCCUP_LINE_LABEL) {
			debugPrint(SELF + "reducing column ", col, " author cells ", 
					authColCells, " and response cells ", respColCells,
					" to extent ", extentText(extent));
			reduce(authColCells, extent);
			reduce(respColCells, extent);
		} // if extent
		debugPrint(SELF + "comparing column ", col, " author cells ", authColCells, 
				" to response cells ", respColCells);
		final int authSize = authColCells.size();
		for (int authCellNum = 0; authCellNum < authSize; authCellNum++) {
			final String authCell = authColCells.get(authCellNum);
			final int locn = respColCells.indexOf(authCell);
			if (locn >= 0) {
				respColCells.remove(locn);
			} else {
				debugPrint(SELF + "author's cell ", authCell, 
						" in column ", col, ", row ", 
						authDiagram.getOccupRowNums(col).get(authCellNum),
						" doesn't have a match in response diagram.");
				match = false;
				break;
			} // if author's cell is found in response diagram
		} // for each group of author's orbitals in the column
		if (match && Utils.among(extent, TYPE_CT_OCCUP_LINE, 
				TYPE_CT_OCCUP_LINE_LABEL)) {
			match = correlationsOK(energies == ANY_E); 
		} // if need to check orbital correlations
		return match;
	} // orbsMatch(int, int)

	/** Determines whether the lines that connect the occupied cells in the
	 * two diagrams are the same.  "Same" here means EITHER:
	 * <ul><li>that the energy order of
	 * the occupied cells that are connected are the same within each column; 
	 * e.g., the second-highest energy cell in column 2 is connected to the 
	 * highest energy cell in column 3 in both diagrams, OR
	 * </li><li>that the energies of the occupied cells are ignored,
	 * but their contents are considered.  
	 * </li></ul>
	 * @param	ignoreE	whether to ignore energies of occupied cells
	 * @return	true if the lines connecting occupied cells are the same
	 */
	private boolean correlationsOK(boolean ignoreE) {
		final String SELF = "OEDDiff.correlationsOK_ignoreE: ";
		if (!ignoreE) return correlationsOK();
		final List<CellsLine> respLines = respDiagram.getLines();
		final List<CellsLine> authLines = authDiagram.getLines();
		final int respSize = respLines.size();
		final int authSize = authLines.size();
		if (respSize != authSize) return false;
		final String[] respLinesArr = convertLines(respLines);
		final String[] authLinesArr = convertLines(authLines);
		Arrays.sort(respLinesArr);
		Arrays.sort(authLinesArr);
		debugPrint(SELF + "response sorted no-energy lines = ", respLinesArr);
		debugPrint(SELF + "author's sorted no-energy lines = ", authLinesArr);
		return Arrays.equals(respLinesArr, authLinesArr);
	} // correlationsOK_ignoreE()

	/** Converts a list of lines connecting cells to their String 
	 * representations, omitting row information but including orbital
	 * information.
	 * @param	lines	a list of lines connecting cells
	 * @return	String representation of the lines with row information omitted
	 * but orbital information included
	 */
	private String[] convertLines(List<CellsLine> lines) {
		final String SELF = "OEDDiff.convertLines: ";
		final List<String> orbLines = new ArrayList<String>();
		for (final CellsLine line : lines) {
			final StringBuilder bld = new StringBuilder();
			for (int endNum = 0; endNum < 2; endNum++) {
				final OEDCell end = (OEDCell) line.endPoints[endNum];
				Utils.appendTo(bld, end.getColumn(), LOC_SEP, 
						end.toString(SORT));
				if (endNum == 0) bld.append(CellsLine.ENDPTS_SEP);
			} // for each end of the line
			final String lineStr = bld.toString();
			debugPrint(SELF + "converted ", line.toString(), " to ", lineStr);
			orbLines.add(lineStr);
		} // for each line
		return orbLines.toArray(new String[orbLines.size()]);
	} // convertLines(List<CellsLine>)

	/** Modifies String representations of occupied cells to remove information
	 * about labels, occupancies, or number of orbitals
	 * @param	cellStrs	list of String representations of occupied cells in
	 * a column -- modified by the method!
	 * @param	extent	whether comparing type of orbital 
	 * only, type and number of orbitals, type, number, and occupancies, or
	 * type, number, occupancies, and labels
	 */
	private void reduce(List<String> cellStrs, int extent) {
		final String SELF = "OEDDiff.reduce: ";
		final List<String> colTypes = new ArrayList<String>();
		for (final String cellStr : cellStrs) {
			final String[] cellParts = cellStr.split(CELL_CONTENTS_SEP);
			final StringBuilder cellDescrip = 
					Utils.getBuilder(cellParts[ORBS_TYPE]);
			if (extent != TYPE_ONLY) {
				Utils.appendTo(cellDescrip, CELL_CONTENTS_SEP, 
						extent == TYPE_CT
							? cellParts[OCCUPS].replaceAll("\\d+", "0")
							: cellParts[OCCUPS]);
			} // if not type only
			colTypes.add(cellDescrip.toString());
		} // for each occupied cell in this column
		cellStrs.clear();
		cellStrs.addAll(colTypes);
	} // reduce(List<String>, int)

	/** Modifies String representations of occupied cells to remove information
	 * about labels, occupancies, or number of orbitals
	 * @param	allCellStrs	lists of String representations of occupied 
	 * cells in all columns -- modified by the method!
	 * @param	extent	whether comparing type of orbital 
	 * only, type and number of orbitals, type, number, and occupancies, or
	 * type, number, occupancies, and labels
	 */
	private void reduceAllCols(List<ArrayList<String>> allCellStrs, 
			int extent) {
		final String SELF = "OEDDiff.reduceAllCols: ";
		debugPrint(SELF + "reducing ", allCellStrs, " to extent ", 
				extentText(extent));
		for (final List<String> cellStrs : allCellStrs) {
			reduce(cellStrs, extent);
		} // for each column
		debugPrint(SELF + "reduced to ", allCellStrs);
	} // reduceAllCols(List<ArrayList<String>>, int)

	/** Gets the name of the extent of comparison.
	 * @param	extent the extent of comparison
	 * @return	name of the extent of comparison
	 */
	private String extentText(int extent) {
		return (extent == TYPE_ONLY ? "TYPE_ONLY" 
				: extent == TYPE_CT ? "TYPE_CT" 
				: extent == TYPE_CT_OCCUP ? "TYPE_CT_OCCUP"
				: extent == TYPE_CT_OCCUP_LINE ? "TYPE_CT_OCCUP_LINE"
				: "TYPE_CT_OCCUP_LINE_LABEL"); 
	} // extentText(int)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 				{ return EVAL_CODES[OED_DIFF]; } 

} // OEDDiff

