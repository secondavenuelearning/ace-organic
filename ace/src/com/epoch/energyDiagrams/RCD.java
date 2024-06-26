package com.epoch.energyDiagrams;

import com.epoch.energyDiagrams.diagramConstants.RCDConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.VerifyException;
import com.epoch.qBank.EDiagramQDatum;
import com.epoch.qBank.QDatum;
import com.epoch.translations.PhraseTransln;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.XMLUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.builder.HashCodeBuilder;

/** A diagram correlating the energies of starting materials, intermediates, 
 * transition states, and products in a reaction. Consists of a table, each 
 * cell of which may contain one "state" (starting material, etc.). */
public class RCD extends EDiagram implements RCDConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Lines connecting cells in the table that were explicitly drawn by 
	 * the user. */
	protected final List<CellsLine> origLines = new ArrayList<CellsLine>();
	/** Error in parsing diagram. */
	transient private int error = 0;

	/* ******************** Constructors and initialization ********************/

	/** Constructor.  */
	public RCD() {
		numCols = NUM_COLS_DEFAULT;
		labels = LABELS_DEFAULT;
		initializeTable();
	} // RCD()

	/** Constructor.
	 * @param	rows	number of rows (energy levels) in this diagram
	 * @param	cols	number of columns in this diagram
	 */
	public RCD(int rows, int cols) {
		numRows = rows;
		numCols = cols;
		labels = LABELS_DEFAULT;
		initializeTable();
	} // RCD(int)

	/** Constructor.
	 * @param	qData	first (and only) member contains the number of rows 
	 * (energy levels) and columns in this diagram, labels for the maxima and 
	 * minima, possibly Y-axis scale information
	 */
	public RCD(QDatum[] qData) {
		numCols = NUM_COLS_DEFAULT;
		labels = LABELS_DEFAULT;
		if (!Utils.isEmpty(qData) && !Utils.isEmpty(qData[0].data)) {
			final EDiagramQDatum eqDatum = (EDiagramQDatum) qData[0];
			final String[] dimsStrs = eqDatum.data.split(QDATA_SEP);
			final int[] dims = Utils.stringToIntArray(dimsStrs);
			numRows = dims[NUM_ROWS];
			numCols = dims[NUM_COLS];
			labels = eqDatum.labels;
			yAxisScale = eqDatum.yAxisScale;
		} // if there is a qDatum 
		initializeTable();
	} // RCD(QDatum[])

	/** Initializes the table of cells and the lists of contents of 
	 * occupied rows and the row numbers. */
	private void initializeTable() {
		table = new RCDCell[numRows][numCols];
		for (int cNum = 1; cNum <= numCols; cNum++) {
			occupStrs.add(new ArrayList<String>());
			occupRowNums.add(new ArrayList<Integer>());
			for (int rNum = 1; rNum <= numRows; rNum++) {
				table[rNum - 1][cNum - 1] = new RCDCell(rNum, cNum);
			} // for each column
		} // for each row
	} // initializeTable()

	/** Converts a string describing the locations, types, and labels of the
	 * diagrams' maxima and minima into array contents. 
	 * @param	data	String in format 5=2=4/... where 5=2 represents
	 * the 1-based row and column and 4 represents a state labeled with
	 * label 4
	 * @throws	ParameterException	if the description of a cell is no good
	 * @throws	VerifyException	if the state of each cell cannot be
	 * calculated from its connections to other cells
	 */
	public final void setStates(String data) 
			throws ParameterException, VerifyException {
		setStates(data, THROW_IT);
	} // setStates(String)

	/** Converts a string describing the locations, types, and labels of the
	 * diagrams' maxima and minima into array contents. 
	 * @param	data	String describing the diagram contents
	 * @param	throwIt	whether to throw a ParameterException when warranted, or
	 * just keep going
	 * @throws	ParameterException	if the description of a cell is no good
	 * @throws	VerifyException	if the state of each cell cannot be
	 * calculated from its connections to other cells
	 */
	public final void setStates(String data, boolean throwIt) 
			throws ParameterException, VerifyException {
		final String SELF = "RCD.setStates: ";
		if (Utils.isEmpty(data)) return;
		parseXML(XMLUtils.xmlToNode(data), !IS_OED);
		calculateStates(throwIt);
	} // setStates(String, boolean)

	/** Calculates states (maximum, minimum, etc.) of occupied cells. 
	 * @param	throwIt	whether to throw a VerifyException when warranted, or
	 * just keep going
	 * @throws	ParameterException	if the description of a cell is no good
	 * @throws	VerifyException	if the state of each cell cannot be
	 * calculated from its connections to other cells
	 */
	private void calculateStates(boolean throwIt) 
			throws ParameterException, VerifyException {
		final String SELF = "RCD.calculateStates: ";
		origLines.addAll(lines);
		for (int col = 1; col <= numCols; col++) {
			final List<Integer> colOccupRowNums = getOccupRowNums(col);
			for (final Integer rowNum : colOccupRowNums) { // just one if there are no lines
				final int row = rowNum.intValue();
				final RCDCell cell = 
						(RCDCell) table[row - 1][col - 1];
				final List<DiagramCell> connections =
						cell.getConnectedCells();
				boolean hasNextConnection = false;
				for (final DiagramCell connectedCell : connections) {
					final int connectedCol = connectedCell.getColumn();
					if (connectedCol == col + 1) {
						hasNextConnection = true;
						break;
					} // if there's a connection to a cell in next column
				} // for each connection
				if (!hasNextConnection) {
					final List<Integer> rightOccupRowNums =
							(col != numCols ? getOccupRowNums(col + 1) : null);
					if (!Utils.isEmpty(rightOccupRowNums)) {
						if (rightOccupRowNums.size() > 1) {
							debugPrint(SELF + "unconnected cells when more "
									+ "than one cell per column");
							error = TWO_UNCONN_IN_COL;
							if (throwIt) {
								throw new VerifyException(Utils.toString(
										"Because there is more than "
											+ "one state in column ", col + 1, 
										", ACE cannot determine which "
											+ "it should connect to the state "
											+ "in column ", col,
										". Please connect the states "
											+ "in different columns "
											+ "explicitly."));
							} // if throwIt
						} else {
							// make connection of this cell to the cell in the 
							// column to the right; process also adds each cell
							// to the other's list of connected cells
							final int rightRow = 
									rightOccupRowNums.get(0).intValue();
							final RCDCell rightCell = 
									(RCDCell) table[rightRow - 1][col];
							lines.add(new CellsLine(cell, rightCell));
						} // if there's exactly one state to which to connect
					} // if there's a state to the right
				} // if there are no lines connecting states
				try {
					cell.calcState();
				} catch (VerifyException e) {
					debugPrint(SELF + "could not calculate state of cell at "
							+ "row ", row, " and column ", col, ": ", 
							e.getMessage());
					error = BAD_STATE;
					if (throwIt) throw e;
				} // try
				occupStrs.get(col - 1).add(cell.getDescrip());
			} // for each occupied cell in this column
		} // for each column in this table
		debugPrint(SELF + "occupRowNums: ", occupRowNums,
				", occupStrs: ", occupStrs);
	} // calculateStates(boolean)

	/** Determines whether there is a gap between occupied columns.
	 * @return true if there is a gap between occupied columns
	 */
	public boolean hasGap() {
		for (int col = 1; col < numCols; col++) {
			final List<Integer> colOccupRowNums = occupRowNums.get(col - 1);
			if (Utils.isEmpty(colOccupRowNums)) {
				final List<Integer> nextColOccupRowNums = occupRowNums.get(col);
				if (!Utils.isEmpty(nextColOccupRowNums)) {
					debugPrint("RCD.hasGap: column ", col, " is empty whereas "
							+ "column ", col + 1, " is not.");
					return true;
				} // if next column is not empty
			} // if this column is empty
		} // for each column except the last
		return false;
	} // hasGap()

	/* ******************** Get methods ********************/

	/** Gets the lines that connect the table cells in this diagram that were
	 * drawn explicitly by the user.  Used by front end so user sees original 
	 * response.
	 * @return	a list of lines connecting table cells
	 */
	public List<CellsLine> getOrigLines()	{ 
		return origLines; 
	} // getOrigLines()

	/** Returns the contents of a particular cell.
	 * @param	row	1-based row of the cell
	 * @param	col	1-based column of the cell
	 * @throws	ParameterException	if row or cell is out of range
	 * @return	the cell's contents
	 */
	public RCDCell getCell(int row, int col) throws ParameterException {
		if (!MathUtils.inRange(row, new int[] {1, numRows})
				|| !MathUtils.inRange(col, new int[] {1, numCols})) {
			throw new ParameterException("RCD.getCell: "
					+ "row " + row + " or column " + col + " out of range.");
		} // if row and col are out of range
		return (RCDCell) table[row - 1][col - 1];
	} // getCell(int, int)

	/** Returns whether a particular cell is occupied.
	 * @param	row	1-based row of the cell
	 * @param	col	1-based column of the cell
	 * @throws	ParameterException	if row or cell is out of range
	 * @return	true if the cell is occupied
	 */
	public boolean isOccupied(int row, int col) throws ParameterException {
		if (!MathUtils.inRange(row, new int[] {1, numRows})
				|| !MathUtils.inRange(col, new int[] {1, numCols})) {
			throw new ParameterException("RCD.isOccupied: "
					+ "row " + row + " or column " + col + " out of range.");
		} // if row and col are out of range
		return ((RCDCell) table[row - 1][col - 1]).isOccupied();
	} // isOccupied(int, int)

	/** Returns the value of a cell's state (can be a maximum or a minimum).
	 * @param	row	1-based row of the cell
	 * @param	col	1-based column of the cell
	 * @throws	ParameterException	if row or cell is out of range
	 * @return	the value of a cell's state
	 */
	public int getState(int row, int col) throws ParameterException {
		if (!MathUtils.inRange(row, new int[] {1, numRows})
				|| !MathUtils.inRange(col, new int[] {1, numCols})) {
			throw new ParameterException("RCD.getState: "
					+ "row " + row + " or column " + col + " out of range.");
		} // if row or col is out of range
		return ((RCDCell) table[row - 1][col - 1]).getState();
	} // getState(int, int)

	/** Returns the string representing the contents of a cell.
	 * @param	row	1-based row of the cell
	 * @param	col	1-based column of the cell
	 * @throws	ParameterException	if row or cell is out of range
	 * @return	string representing the contents of the cell
	 */
	public String getString(int row, int col) throws ParameterException {
		if (!MathUtils.inRange(row, new int[] {1, numRows})
				|| !MathUtils.inRange(col, new int[] {1, numCols})) {
			throw new ParameterException("RCD.getString: "
					+ "row " + row + " or column " + col + " out of range.");
		} // if row or col is out of range
		return ((RCDCell) table[row - 1][col - 1]).toString();
	} // getString(int, int)

	/** Gets the kind of error associated with parsing this diagram.
	 * @return the kind of error associated with parsing this diagram
	 */
	public int getError() {
		return error;
	} // getError()

	/* ******************** String methods ********************/

	/** Determines if this reaction coordinate diagram equals another.
	 * @param	theOther	another reaction coordinate diagram
	 * @return	true if the diagrams have maxima and minima at the same
	 * energy levels with the same labels
	 */
	@Override
	public boolean equals(Object theOther) {
		boolean isEqual = false;
		if (theOther instanceof RCD) {
			final RCD comp = (RCD) theOther;
			isEqual = numRows == comp.getNumRows()
					&& numCols == comp.getNumCols()
					&& toString().equals(comp.toString());
		} // if instanceof
		return isEqual;
	} // equals(Object)

	/** Creates a hash code describing this object.
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).
			append(getNumRows()).append(getNumCols()).append(toString()).
			toHashCode();
	} // hashCode()

	/** Creates an English description of the diagram's question datum
	 * (dimensions and labels, y-axis markings)
	 * @return an English description of the diagram's dimensions,
	 * labels, y-axis markings
	 */
	public String qDatumToEnglish() {
		final StringBuilder bld = Utils.getBuilder("The diagram has ", 
				numRows, " rows, ", numCols, " columns, and ");
		final int numLabels = labels.length;
		if (numLabels == 0) {
			bld.append("no labels for maxima and minima.");
		} else {
			if (numLabels == 1) {
				bld.append("one label for maxima and minima: ");
			} else {
				Utils.appendTo(bld, numLabels, 
						" labels for maxima and minima: ");
			} // if numLabels
			for (int lblNum = 0; lblNum < numLabels; lblNum++) {
				if (lblNum == 1 && numLabels == 2) bld.append(" and ");
				else if (lblNum > 0) { 
					bld.append(", ");
					if (lblNum == numLabels - 1) bld.append("and ");
				} // if lblNum and numLabels
				Utils.addSpanString(bld, labels[lblNum]);
			} // for each label
		} // if numLabels
		bld.append('.');
		if (yAxisScale != null && yAxisScale.haveLabels()) {
			Utils.appendTo(bld, "<br/>", yAxisScale.toEnglish());
		} // if there are labels
		return bld.toString();
	} // qDatumToEnglish()

	/** Converts the states' types and labels into a String.
	 * @return	the string encoding the states' types and labels
	 */
	public String toString() {
		final StringBuilder bld = new StringBuilder();
		for (int col = 1; col <= numCols; col++) {
			try {
				final List<Integer> colOccupRowNums = getOccupRowNums(col);
				for (final Integer rowNum : colOccupRowNums) {
					final int row = rowNum.intValue();
					if (bld.length() > 0) bld.append(CELLS_SEP);
					Utils.appendTo(bld, row, LOC_SEP, col,
							LOC_SEP, getString(row, col));
				} // for each occupied row
			} catch (ParameterException e) { // unlikely
				Utils.alwaysPrint("RCD.toString: "
						+ "caught ParameterException for col ", col);
			} // try
		} // for each column
		return Utils.toString(bld, linesToString());
	} // toString()

	/** Creates an HTML table suitable for display.
	 * @return	an HTML table suitable for display
	 */
	public String toDisplay() {
		return toDisplay(null, 0);
	} // toDisplay()

	/** Creates an HTML table suitable for display.
	 * @param	dispNum	number to assign to this table
	 * @return	an HTML table suitable for display
	 */
	public String toDisplay(int dispNum) {
		return toDisplay(null, dispNum);
	} // toDisplay(int)

	/** Creates an HTML table suitable for display.
	 * @param	userLangs	a user's preferred languages
	 * @param	dispNum	number to assign to this table
	 * @return	an HTML table suitable for display
	 */
	public String toDisplay(String[] userLangs, int dispNum) {
		final StringBuilder bld = Utils.getBuilder(
				"<table class=\"whiteTable\" style=\"width:95%;\"><tr>");
		final boolean haveYAxisScale = haveYAxisScale();
		if (haveYAxisScale) {
			Utils.appendTo(bld, "<th>",
					PhraseTransln.translate("Energy", userLangs),
					" (", Utils.toDisplay(yAxisScale.getUnit()), ")</th>");
		} // if there are labels for the rows
		for (int cNum = 1; cNum <= numCols; cNum++) {
			bld.append("<td style=\"text-align:center; "
					+ "border-bottom-style:solid; border-bottom-width:1px; "
					+ "border-bottom-color:black;\"></td>");
		} // for each column
		// Set up graphics canvas to draw lines
		Utils.appendTo(bld, "</tr>\n<tr><th colspan=\"", numCols,
				"\"><div id=\"canvas", dispNum,
				"\" style=\"position:relative; left:0px; top:0px; "
					+ "overflow:visible;\"></div></th></tr>\n");
		// Generate table cells
		final String[] yAxisLabels = getYAxisLabels();
		for (int rNum = numRows; rNum >= 1; rNum--) {
			bld.append("<tr>");
			if (haveYAxisScale) {
				Utils.appendTo(bld, "<th>", yAxisLabels[rNum - 1], "</th>");
			} // if there are labels for the rows
			for (int cNum = 1; cNum <= numCols; cNum++) {
				Utils.appendTo(bld, 
						"<td style=\"text-align:center;\" id = \"d",
						dispNum, 'r', rNum, 'c', cNum, "\"><span>&nbsp;");
				try {
					if (isOccupied(rNum, cNum)) {
						final RCDCell cell = getCell(rNum, cNum); 
						final String name = getNameForLabel(cell.getLabel());
						bld.append(Utils.isEmpty(name) ? "[no selection]" 
								: name);
					} else bld.append("&nbsp;");
				} catch (ParameterException e) { // unlikely
					bld.append(PhraseTransln.translate(
							"<b>error</b>", userLangs));
				} // try
				bld.append("&nbsp;</span></td>");
			} // for each column 
			bld.append("</tr>\n");
		} // for each row
		bld.append("<tr>");
		for (int cNum = 1; cNum <= numCols; cNum++) {
			bld.append("<td class=\"boldtext\" "
					+ "style=\"text-align:center; border-top-style:solid; "
					+ "border-top-width:1px; border-top-color:black;\"></td>");
		} // for each column
		return Utils.toString(bld, "</tr></table>");
	} // toDisplay(String[])

} // RCD

