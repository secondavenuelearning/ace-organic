package com.epoch.energyDiagrams;

import com.epoch.energyDiagrams.diagramConstants.DiagramCellConstants;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/** A cell in an energy diagram. */
public class DiagramCell implements DiagramCellConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** 1-based row of this cell.  Numbered from bottom up. */
	transient protected int row = 1;
	/** 1-based column of this cell.  Numbered left to right. */
	transient protected int col = 1;
	/** 1-based number of the label of the item in the cell; 0 is unknown. */
	protected int label = 0;

	/** Cells connected to this one with lines. */
	transient protected List<DiagramCell> connectedCells = 
			new ArrayList<DiagramCell>();

	/** Constructor.  */
	public DiagramCell() {
		// intentionally empty
	} // DiagramCell()

	/** Constructor.
	 * @param	r	row
	 * @param	c	column
	 */
	public DiagramCell(int r, int c) {
		row = r;
		col = c;
	} // DiagramCell(int, int)

	/** Sets the contents of this table cell from a node derived from XML.
	 * @param	cellNode	node derived from XML describing this cell
	 * @param	table	table of diagram cells
	 * @param	isOED	whether the XML to be parsed is of an orbital energy
	 * diagram
	 * @return	the cell whose values were set
	 */
	static DiagramCell parseXML(Node cellNode, DiagramCell[][] table,
			boolean isOED) {
		final String SELF = "DiagramCell.parseXML: ";
		int row = 0;
		int col = 0;
		int label = 0;
		if (cellNode != null && cellNode.hasAttributes()) {
			final NamedNodeMap attributes = cellNode.getAttributes();
			if (attributes.getNamedItem(ROW_TAG) != null) {
				row = MathUtils.parseInt(
						attributes.getNamedItem(ROW_TAG).getNodeValue());
			} // if there's a row attribute
			if (attributes.getNamedItem(COLUMN_TAG) != null) {
				col = MathUtils.parseInt(
						attributes.getNamedItem(COLUMN_TAG).getNodeValue());
			} // if there's a column attribute
			if (attributes.getNamedItem(LABEL_TAG) != null) {
				label = MathUtils.parseInt(
						attributes.getNamedItem(LABEL_TAG).getNodeValue());
			} // if there's a label attribute
		} // if there are attributes
		debugPrint(SELF + "got row = ", row, ", col = ", col, 
				", label = ", label);
		DiagramCell cell = null;
		if (MathUtils.inRange(row, new int[] {1, table.length})
				&& MathUtils.inRange(col, new int[] {1, table[0].length})) {
			cell = table[row - 1][col - 1];
			cell.setLabel(label);
			if (isOED) ((OEDCell) cell).parseXML(cellNode);
		} // if row and column obtained from attributes
		return cell;
	} // parseXML(Node, DiagramCell[][], boolean)

	/** Adds a cell to the list of cells connected to this one with lines.
	 * @param	cell	another cell connected to this one
	 */
	void addConnection(DiagramCell cell) {
		connectedCells.add(cell);
	} // addConnection(DiagramCell)

	/** Sets the label of the cell contents. 
	 * @param	aLabel	label of the cell contents
	 */
	final protected void setLabel(int aLabel)	{ label = aLabel; }
	/** Gets the cell label. 
	 * @return	the cell label
	 */
	public int getLabel()						{ return label; }
	/** Gets the cell row. 
	 * @return	the cell row
	 */
	public int getRow()							{ return row; }
	/** Gets the cell column. 
	 * @return	the cell column
	 */
	public int getColumn()						{ return col; }

	/** Gets the cells connected to this one. 
	 * @return	the cells connected to this one
	 */
	public List<DiagramCell> getConnectedCells() { 
		return connectedCells; 
	} // getConnectedCells()

	/** Gets the position of a cell relative to this one.
	 * @param	cell    another cell
	 * @return	positions of the cell with respect to this one as an array
	 */
	public int[] directionsTo(DiagramCell cell) {
		final int[] directions = new int[2];
		directions[HORIZONTAL] = directionTo(cell, HORIZONTAL);
		directions[VERTICAL] = directionTo(cell, VERTICAL);
		return directions;
	} // directionsTo(DiagramCell)

	/** Gets the position of a cell relative to this one.
	 * @param	cell    another cell
	 * @param	direction	the vertical or horizontal relative position
	 * @return	position of the cell with respect to this one
	 */
	private int directionTo(DiagramCell cell, int direction) {
		final int diff = (direction == VERTICAL ? cell.getRow() - getRow()
				: cell.getColumn() - getColumn());
		return MathUtils.sign(diff);
	} // directionTo(DiagramCell, boolean)

	/** Gets whether a cell is in a column adjacent to this one.
	 * @param	cell    another cell
	 * @return	true if the cells are in adjacent columns
	 */
	public boolean inAdjacentColumn(DiagramCell cell) {
		final int diff = cell.getColumn() - getColumn();
		return (diff * diff == 1);
	} // inAdjacentColumn(DiagramCell)

	/** Converts this cell into a String representation.
	 * @return String representation of this cell 
	 */
	public String toString() {
		return getLocnString();
	} // toString()

	/** Converts the location of this cell into a String representation.
	 * @return String representation of the location of this cell 
	 */
	public String getLocnString() {
		return Utils.toString(row, LOC_SEP, col);
	} // getLocnString()

} // DiagramCell
