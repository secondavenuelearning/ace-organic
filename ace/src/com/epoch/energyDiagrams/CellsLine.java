package com.epoch.energyDiagrams;

import com.epoch.energyDiagrams.diagramConstants.CellsLineConstants;
import com.epoch.energyDiagrams.diagramConstants.EDiagramConstants;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Represents a line connecting two energy diagram cells. */
public class CellsLine implements CellsLineConstants, EDiagramConstants {

	private void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** The endpoints of the line, ordered by column. */
	transient public final DiagramCell[] endPoints = new DiagramCell[2];

	/** Constructor.  Also adds each cell to the list of connections of the
	 * other cell.
	 * @param	data	String in form 5=2:8=1 describing the endpoints of the
	 * line as 1-based row/column pairs.
	 * @param	table	table of diagram cells that may be connected
	 */
	public CellsLine(String data, DiagramCell[][] table) {
		final String SELF = "CellsLine: ";
		debugPrint(SELF + "data = ", data);
		final String[] ends = data.split(ENDPTS_SEP);
		if (ends.length == 2) {
			for (int endNum = 0; endNum < 2; endNum++) {
				final String[] coordsStr = ends[endNum].split(LOC_SEP);
				if (coordsStr.length == 2) {
					final int row = MathUtils.parseInt(coordsStr[ROW]);
					final int col = MathUtils.parseInt(coordsStr[COL]);
					debugPrint(SELF + "row = ", row, ", col = ", col);
					endPoints[endNum] = table[row - 1][col - 1];
				} // if there is a row and column
			} // for each endpoint
			finalizeEndPoints();
		} // if there are two endpoints
	} // CellsLine(String, DiagramCell[][])

	/** Constructor.  Also adds each cell to the list of connections of the
	 * other cell.
	 * @param	lineNode	node derived from XML describing this line
	 * @param	table	table of diagram cells that may be connected
	 */
	CellsLine(Node lineNode, DiagramCell[][] table) {
		final String SELF = "CellsLine: ";
		debugPrint(SELF + "table length = ", table.length, ", width = ",
				table[0].length);
		final NodeList endPtNodes = lineNode.getChildNodes();
		final int numEndPtNodes = endPtNodes.getLength();
		int endPtNum = LEFT_END;
		for (int endNum = 0; endNum < numEndPtNodes; endNum++) {
			final Node endPtNode = endPtNodes.item(endNum);
			if (endPtNode.getNodeName().equalsIgnoreCase(ENDPT_TAG)) {
				int row = 0;
				int col = 0;
				final NamedNodeMap attributes = endPtNode.getAttributes();
				if (attributes != null) {
					if (attributes.getNamedItem(ROW_TAG) != null) {
						row = MathUtils.parseInt(
								attributes.getNamedItem(ROW_TAG).getNodeValue());
					} // if there's a row attribute
					if (attributes.getNamedItem(COLUMN_TAG) != null) {
						col = MathUtils.parseInt(
								attributes.getNamedItem(COLUMN_TAG).getNodeValue());
					} // if there's a column attribute
				} // if there are attributes
				if (MathUtils.inRange(row, new int[] {1, table.length})
						&& MathUtils.inRange(col, new int[] {1, table[0].length})) {
					endPoints[endPtNum++] = table[row - 1][col - 1];
					debugPrint(SELF + "row = ", row, ", col = ", col);
				} else {
					debugPrint(SELF + "row out of range 1-", table.length, 
							" or column out of range 1-", table[0].length);
				} // if row and column in range
			} else if (endPtNode.getNodeType() != Node.TEXT_NODE) {
				Utils.alwaysPrint(SELF + "unknown node ",
						endPtNode.getNodeName());
			} // if node name or type
			if (endPtNum > RIGHT_END) break; // probably unnecessary
		} // for each node
		if (!Utils.anyMembersAreNull(endPoints)) finalizeEndPoints();
	} // CellsLine(Node)

	/** Puts endpoints in left-to-right order, adds each endpoint to the other's
	 * list of connected cells. */
	private void finalizeEndPoints() {
		if (endPoints[LEFT_END].getColumn() > endPoints[RIGHT_END].getColumn()) {
			swap();
		} // if line not recorded left to right
		endPoints[LEFT_END].addConnection(endPoints[RIGHT_END]);
		endPoints[RIGHT_END].addConnection(endPoints[LEFT_END]);
	} // finalizeEndPoints()

	/** Constructor.  Also adds each cell to the list of connections of the
	 * other cell.
	 * @param	cell0	one cell to be connected
	 * @param	cell1	the other cell to be connected
	 */
	public CellsLine(DiagramCell cell0, DiagramCell cell1) {
		cell0.addConnection(cell1);
		cell1.addConnection(cell0);
		if (cell0.getColumn() < cell1.getColumn()) {
			endPoints[LEFT_END] = cell0;
			endPoints[RIGHT_END] = cell1;
		} else {
			endPoints[LEFT_END] = cell1;
			endPoints[RIGHT_END] = cell0;
		} // if line not recorded left to right
	} // CellsLine(DiagramCell, DiagramCell)

	/** Swaps the two endpoints. */
	final public void swap() {
		final DiagramCell temp = endPoints[LEFT_END];
		endPoints[LEFT_END] = endPoints[RIGHT_END];
		endPoints[RIGHT_END] = temp;
	} // swap()

	/** Gets the row of one of the endpoints.
	 * @param	end	0-based number of endpoint
	 * @return	1-based row of the endpoint, -1 if out of range
	 */
	public int getRow(int end) {
		return (Utils.among(end, 0, 1) ? endPoints[end].getRow() : -1);
	} // getRow(int)

	/** Gets the column of one of the endpoints.
	 * @param	end	0-based number of endpoint
	 * @return	1-based column of the endpoint, -1 if out of range
	 */
	public int getColumn(int end) {
		return (Utils.among(end, 0, 1) ? endPoints[end].getColumn() : -1);
	} // getColumn(int)

	/** Determines whether this line and another line connect cells with the 
	 * same row and column numbers.
	 * @param	line	another line
	 * @return	true if the lines connect cells with the same row and column
	 * numbers
	 */
	public boolean equals(CellsLine line) {
		return (endPoints[LEFT_END].equals(line.endPoints[LEFT_END]) 
					&& endPoints[RIGHT_END].equals(line.endPoints[RIGHT_END])) 
				|| (endPoints[LEFT_END].equals(line.endPoints[RIGHT_END]) 
					&& endPoints[RIGHT_END].equals(line.endPoints[LEFT_END])); 
	} // equals(CellsLine)

	/** Determines whether this line connects to a cell with the same row and
	 * column numbers as one of the ends of another line.
	 * @param	line	another line
	 * @return	true if the lines have an endpoint with the same row and column
	 * numbers
	 */
	public boolean oneEndEquals(CellsLine line) {
		return (endPoints[LEFT_END].equals(line.endPoints[LEFT_END]) 
				|| endPoints[RIGHT_END].equals(line.endPoints[RIGHT_END]) 
				|| endPoints[LEFT_END].equals(line.endPoints[RIGHT_END])); 
	} // oneEndEquals(CellsLine)

	/** Determines whether this line and another line connect the same cells.
	 * Uses pointer equality.
	 * @param	line	another line
	 * @return	true if the lines connect the same cells
	 */
	public boolean connectsSame(CellsLine line) {
		return (endPoints[LEFT_END] == line.endPoints[LEFT_END]
					&& endPoints[RIGHT_END] == line.endPoints[RIGHT_END])
				|| (endPoints[LEFT_END] == line.endPoints[RIGHT_END]
					&& endPoints[RIGHT_END] == line.endPoints[LEFT_END]);
	} // connectsSame(CellsLine)

	/** Determines whether this line and another line join to a common cell.
	 * Uses pointer equality.
	 * @param	line	another line
	 * @return	true if the lines join to a common cell
	 */
	public boolean sharesEndWith(CellsLine line) {
		return (endPoints[LEFT_END] == line.endPoints[LEFT_END]
				|| endPoints[RIGHT_END] == line.endPoints[RIGHT_END]
				|| endPoints[LEFT_END] == line.endPoints[RIGHT_END]);
	} // sharesEndWith(CellsLine)

	/** Converts this line into a String representation.
	 * @return String representation of this line
	 */
	public String toString() {
		return (Utils.anyMembersAreNull(endPoints) ? "" 
				: Utils.toString(endPoints[LEFT_END].getLocnString(), 
					ENDPTS_SEP, endPoints[RIGHT_END].getLocnString()));
	} // toString()

} // CellsLine
