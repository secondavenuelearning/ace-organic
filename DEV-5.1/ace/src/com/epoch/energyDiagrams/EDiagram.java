package com.epoch.energyDiagrams;

import com.epoch.energyDiagrams.diagramConstants.EDiagramConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** A diagram in which the y-axis is energy.  Consists of a table, each 
 * cell of which may contain one or more items. */
public class EDiagram implements EDiagramConstants {

	private void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Number of rows (energy levels) in this diagram.  Numbered from bottom 
	 * up. */
	transient protected int numRows = NUM_ROWS_DEFAULT;
	/** Number of columns in this diagram.  Numbered left to right. */
	transient protected int numCols;
	/** Labels for the orbitals or maxima and minima. */
	transient protected String[] labels = new String[0];
	/** Table corresponding to the diagram. */
	transient protected DiagramCell[][] table;
	/** Labels for the rows (y-axis) of the diagram. */
	transient protected YAxisScale yAxisScale = new YAxisScale();

	/** Arrays of string representations of the contents of occupied
	 * cells in the columns of a diagram.  */
	protected final List<ArrayList<String>> occupStrs = 
			new ArrayList<ArrayList<String>>();
	/** Row numbers of occupied cells in a diagram, parallel to
	 * <code>occupStrs</code>. */
	protected final List<ArrayList<Integer>> occupRowNums = 
			new ArrayList<ArrayList<Integer>>();

	/** Lines connecting cells in the table.  Includes lines explicitly drawn by
	 * the user as well as those inferred to be present. */
	protected final List<CellsLine> lines = new ArrayList<CellsLine>();
	
	/** Converts a node derived from XML into the locations, types, 
	 * and occupancies of the diagram's states or orbitals.
	 * @param	node	node derived from XML describing this diagram
	 * @param	isOED	whether the XML to be parsed is of an orbital energy
	 * diagram
	 */
	public void parseXML(Node node, boolean isOED) {
		final String SELF = "EDiagram.parseXML: ";
		if (node == null) {
			debugPrint(SELF + "node is null.");
		} else if (node.getNodeName().equalsIgnoreCase(DIAGRAM_TAG)) {
			if (node.hasChildNodes()) {
				final NodeList children = node.getChildNodes();
				final int numNodes = children.getLength();
				for (int nodeNum = 0; nodeNum < numNodes; nodeNum++) {
					final Node child = children.item(nodeNum);
					parseXML(child, isOED); // recursive call
				} // for each node
			} else debugPrint(SELF + "no children.");
		} else if (node.getNodeName().equalsIgnoreCase(CELL_TAG)) {
			final DiagramCell cell = 
					DiagramCell.parseXML(node, table, isOED);
			if (cell != null) {
				final int row = cell.getRow();
				final int col = cell.getColumn();
				if (!Utils.among(0, row, col)) {
					occupRowNums.get(col - 1).add(Integer.valueOf(row));
				} else Utils.alwaysPrint(SELF + "parsing of cell failed");
			} else Utils.alwaysPrint(SELF + "parsing of cell failed");
		} else if (node.getNodeName().equalsIgnoreCase(LINE_TAG)) {
			parseLineXML(node);
		} else if (node.getNodeType() != Node.TEXT_NODE) {
			Utils.alwaysPrint(SELF + "unknown node ",
					node.getNodeName());
		} // if node name or type
	} // parseXML(Node, boolean)

	/** Converts a node derived from XML describing a line into an actual line
	 * and adds it to the list of lines of this diagram.
	 * @param	lineNode	node describing the line
	 */
	protected void parseLineXML(Node lineNode) {
		final CellsLine line = new CellsLine(lineNode, table);
		debugPrint("EDiagram.parseLineXML: set new line ", 
				line.toString());
		lines.add(line);
	} // parseLineXML(Node)

	/** Gets the number of rows in the diagram. 
	 * @return	the number of rows
	 */
	public int getNumRows()				{ return numRows; }
	/** Gets the number of columns in the diagram. 
	 * @return	the number of columns
	 */
	public int getNumCols()				{ return numCols; }
	/** Gets all the labels of the orbitals or maxima and minima (not just those 
	 * used). 
	 * @return	the labels of the orbitals or maxima and minima
	 */
	public String[] getLabels()			{ return labels; }
	/** Gets the number of possible labels in the diagram (not just those used). 
	 * @return	the number of labels
	 */
	public int getNumLabels()			{ return labels.length; }
	/** Gets the lines that connect the table cells in this diagram, including
	 * those inferred to be present. 
	 * @return	a list of lines
	 */
	public List<CellsLine> getLines() 	{ return lines; }
	/** Gets whether there are labels for the y-axis of this diagram.
	 * @return	true if there are labels for the y-axis of this diagram
	 */
	public boolean haveYAxisScale() 	{ return yAxisScale.haveLabels(); }
	/** Gets the labels for the y-axis of this diagram.
	 * @return	array of labels for the y-axis of this diagram
	 */
	public String[] getYAxisLabels()	{ return yAxisScale.getLabels(numRows); }
	/** Gets the energy unit of this diagram.
	 * @return	energy unit of this diagram
	 */
	public String getYAxisUnit()		{ return yAxisScale.getUnit(); }
	
	/** Returns the number of the label of a cell.
	 * @param	row	1-based row of the cell
	 * @param	col	1-based column of the cell
	 * @throws	ParameterException	if row or cell is out of range
	 * @return	label number of a maximum or minimum
	 */
	public int getLabel(int row, int col) throws ParameterException {
		if (!MathUtils.inRange(row, new int[] {1, numRows})
				|| !MathUtils.inRange(col, new int[] {1, numCols})) {
			throw new ParameterException("EDiagram.getLabel: "
					+ "row " + row + " or column " + col + " out of range.");
		} // if row or col is out of range
		return table[row - 1][col - 1].getLabel();
	} // getLabel(int, int)

	/** Returns the label name of a maximum or minimum.
	 * @param	row	1-based row of the cell
	 * @param	col	1-based column of the cell
	 * @throws	ParameterException	if row or cell is out of range
	 * @return	label name of a maximum or minimum
	 */
	public String getLabelName(int row, int col) throws ParameterException {
		return getNameForLabel(getLabel(row, col));
	} // getLabelName(int, int)

	/** Returns the name associated with a label number.
	 * @param	labelNum	1-based label number
	 * @return the name associated with the label number
	 */
	public String getNameForLabel(int labelNum) {
		return (MathUtils.inRange(labelNum, new int[] {1, labels.length}) 
				? labels[labelNum - 1] : "");
	} // getNameForLabel(int)

	/** Gets copies of lists of string representations of the occupied 
	 * cells in all columns.  
	 * @return	lists of string representations of the occupied cells 
	 * in all columns
	 */
	public List<ArrayList<String>> getOccupStrs() {
		final ArrayList<ArrayList<String>> copy = 
				new ArrayList<ArrayList<String>>();
		for (final List<String> colOccupStrs : occupStrs) {
			copy.add(new ArrayList<String>(colOccupStrs));
		} // for each list in the list
		return copy;
	} // getOccupStrs()

	/** Gets lists of row numbers of the occupied cells in all columns.  
	 * @return	lists of row numbers of the occupied cells in all columns
	 */
	public List<ArrayList<Integer>> getOccupRowNums() {
		return occupRowNums;
	} // getOccupRowNums()

	/** Gets a copy of the list of string representations of the occupied 
	 * cells in a column.  
	 * @param	col	1-based column to get
	 * @throws	ParameterException	if col is out of range
	 * @return	list of string representations of the occupied cells in a
	 * column
	 */
	public List<String> getOccupStrs(int col) throws ParameterException {
		if (!MathUtils.inRange(col, new int[] {1, numCols})) {
			throw new ParameterException("EDiagram.getOccupStrs: "
					+ "column " + col + " out of range.");
		} // if col is out of range
		return new ArrayList<String>(occupStrs.get(col - 1));
	} // getOccupStrs(int)

	/** Gets a list of row numbers of the occupied cells in a column.  
	 * @param	col	1-based column to get
	 * @throws	ParameterException	if col is out of range
	 * @return	list of row numbers of the occupied cells in a column
	 */
	public List<Integer> getOccupRowNums(int col) throws ParameterException {
		if (!MathUtils.inRange(col, new int[] {1, numCols})) {
			throw new ParameterException("EDiagram.getOccupStrs: "
					+ "column " + col + " out of range.");
		} // if col is out of range
		return occupRowNums.get(col - 1);
	} // getOccupRowNums(int)

	/** Gets the number of occupied cells in a column.  
	 * @param	col	1-based column to get
	 * @throws	ParameterException	if col is out of range
	 * @return	number of occupied cells in a column
	 */
	public int getNumOccupRows(int col) throws ParameterException {
		if (!MathUtils.inRange(col, new int[] {1, numCols})) {
			throw new ParameterException("EDiagram.getOccupStrs: "
					+ "column " + col + " out of range.");
		} // if col is out of range
		return occupRowNums.get(col - 1).size();
	} // getNumOccupRows(int)

	/** Converts the lines connecting table cells in this diagram to a string.
	 * @return	String representation of the lines connecting table cells in
	 * this diagram
	 */
	String linesToString() {
		final StringBuilder bld = new StringBuilder();
		for (final CellsLine line : lines) {
			if (bld.length() > 0) bld.append(LINES_SEP);
			bld.append(line.toString());
		} // for each line
		if (bld.length() > 0) bld.insert(0, CELLS_LINES_SEP);
		return bld.toString();
	} // linesToString()

	/** Returns lines with empty rows removed (so rows are numbered 1, 2, ...)
	 * but with the rows kept in their order within each column.  E.g., lines
	 * 13=1:3=2/5=1:15=2 will become 2=1:1=2/1=1:2=2.
	 * @return	a list of strings representing lines that have been vertically
	 * compressed
	 */
	public List<String> getCompareLines() {
		final String SELF = "EDiagram.getCompareLines: ";
		// sort occupied rows within each column
		final int[][] allRowNums = new int[numCols][];
		for (int col = 1; col <= numCols; col++) {
			try {
				allRowNums[col - 1] = 
						Utils.listToIntArray(getOccupRowNums(col));
				Arrays.sort(allRowNums[col - 1]);
				debugPrint(SELF + "column ", col, " sorted occupied "
						+ "row numbers are ", allRowNums[col - 1]);
			} catch (ParameterException e) {
				debugPrint(SELF + "ParameterException");
			} // try
		} // for each column
		final List<String> flattened = new ArrayList<String>();
		// for each line endpoint, get position of row with respect to other 
		// rows in the column, and make new endpoint with position as row
		for (final CellsLine line : lines) {
			final int row0 = line.getRow(0);
			final int column0 = line.getColumn(0);
			final int row1 = line.getRow(1);
			final int column1 = line.getColumn(1);
			debugPrint(SELF + "looking at line connecting row ", row0, " column ",
					column0, " with row ", row1, " column ", column1);
			final int[] col0RowNums = allRowNums[column0 - 1];
			final int posn0 = Utils.indexOf(col0RowNums, row0) + 1;
			final int[] col1RowNums = allRowNums[column1 - 1];
			final int posn1 = Utils.indexOf(col1RowNums, row1) + 1;
			debugPrint(SELF + "flattened connection is row ", posn0, " column ",
					column0, " with row ", posn1, " column ", column1);
			if (posn0 > 0 && posn1 > 0) {
				final DiagramCell cell0 = new DiagramCell(posn0, column0);
				final DiagramCell cell1 = new DiagramCell(posn1, column1);
				final CellsLine flatLine = new CellsLine(cell0, cell1);
				flattened.add(flatLine.toString());
			} // if rows of line endpoints are found in list of occupied rows
		} // for each line
		return flattened;
	} // getCompareLines()

} // EDiagram
