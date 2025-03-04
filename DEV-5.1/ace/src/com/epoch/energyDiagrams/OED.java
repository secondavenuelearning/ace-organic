package com.epoch.energyDiagrams;

import com.epoch.energyDiagrams.diagramConstants.CellsLineConstants;
import com.epoch.energyDiagrams.diagramConstants.OEDConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.qBank.EDiagramQDatum;
import com.epoch.qBank.QDatum;
import com.epoch.translations.PhraseTransln;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.XMLUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.builder.HashCodeBuilder;

/** A diagram correlating the energies and occupancies of atomic 
 * and molecular orbitals. Consists of a table, each cell of which may contain
 * one or more orbitals, each of which has an occupancy. */
public class OED extends EDiagram implements CellsLineConstants, OEDConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Captions for the three columns. */
	transient private String[] captions = new String[] {"", "MOs", ""};

	/* ******************** Constructors and initialization ********************/

	/** Constructor.  */
	public OED() {
		numCols = 3;
		initializeTable();
	} // OED()

	/** Constructor.
	 * @param	rows	number of rows (energy levels) in this diagram
	 */
	public OED(int rows) {
		numRows = rows;
		numCols = 3;
		initializeTable();
	} // OED(int)

	/** Constructor.
	 * @param	qData	first (and only) member contains the number of rows 
	 * (energy levels) in this diagram, captions for the columns, possibly
	 * labels for the orbitals in the second column, and possibly the
	 * Y-axis scale information
	 */
	public OED(QDatum[] qData) {
		numCols = 3;
		if (!Utils.isEmpty(qData) && !Utils.isEmpty(qData[0].data)) {
			final EDiagramQDatum eqDatum = (EDiagramQDatum) qData[0];
			numRows = MathUtils.parseInt(eqDatum.data);
			captions = eqDatum.captions;
			labels = eqDatum.labels;
			yAxisScale = eqDatum.yAxisScale;
		} // if there is a qDatum 
		initializeTable();
	} // OED(QDatum[])

	/** Initializes the table of cells and the lists of contents of 
	 * occupied rows and the row numbers. */
	private void initializeTable() {
		table = new OEDCell[numRows][numCols];
		for (int cNum = 1; cNum <= numCols; cNum++) {
			occupStrs.add(new ArrayList<String>());
			occupRowNums.add(new ArrayList<Integer>());
			for (int rNum = 1; rNum <= numRows; rNum++) {
				table[rNum - 1][cNum - 1] = new OEDCell(rNum, cNum);
			} // for each row
		} // for each column
	} // initializeTable()

	/** Converts a string describing the locations, types, and occupancies 
	 * of the diagram's orbitals into array contents.
	 * @param	data	string describing the contents
	 */
	public final void setOrbitals(String data) {
		final String SELF = "OED.setOrbitals: ";
		debugPrint(SELF + "parsing:\n", data);
		parseXML(XMLUtils.xmlToNode(data), IS_OED);
		for (int cNum = 1; cNum <= numCols; cNum++) {
			final List<Integer> colOccupRowNums = occupRowNums.get(cNum - 1);
			final List<String> colOccupStrs = occupStrs.get(cNum - 1);
			for (final Integer rowNum : colOccupRowNums) {
				final int rNum = rowNum.intValue();
				final OEDCell cell = (OEDCell) table[rNum - 1][cNum - 1];
				colOccupStrs.add(cell.toString(OEDCell.SORT));
			} // for each occupied row in this column
		} // for each column
		debugPrint(SELF + "occupRowNums: ", occupRowNums,
				", occupStrs: ", occupStrs);
	} // setOrbitals(String)

	/** Determines whether there is an empty column.
	 * @return true if there is an empty column
	 */
	public boolean hasEmpty() {
		for (int colNum = 0; colNum < numCols; colNum++) {
			final List<Integer> colOccupRowNums = 
					occupRowNums.get(colNum);
			if (Utils.isEmpty(colOccupRowNums)) {
				debugPrint("OED.hasEmpty: column ", 
						colNum + 1, " is empty.");
				return true;
			} // if this column is empty
		} // for each column except the last
		return false;
	} // hasEmpty()

	/* ******************** Get methods ********************/

	/** Gets the column captions. 
	 * @return	the column captions
	 */
	public String[] getCaptions()	{ return captions; }

	/** Returns a particular cell.
	 * @param	row	1-based row of the cell
	 * @param	col	1-based column of the cell
	 * @throws	ParameterException	if row or cell is out of range
	 * @return	the cell
	 */
	public OEDCell getCell(int row, int col) throws ParameterException {
		if (!MathUtils.inRange(row, new int[] {1, numRows})
				|| !MathUtils.inRange(col, new int[] {1, numCols})) {
			throw new ParameterException("OED.getCell: "
					+ "row " + row + " or column " + col + " out of range.");
		} // if row and col are out of range
		return (OEDCell) table[row - 1][col - 1];
	} // getCell(int, int)

	/** Returns whether a particular cell has orbitals.
	 * @param	row	1-based row of the cell
	 * @param	col	1-based column of the cell
	 * @throws	ParameterException	if row or cell is out of range
	 * @return	true if the cell has orbitals
	 */
	public boolean hasOrbitals(int row, int col) throws ParameterException {
		if (!MathUtils.inRange(row, new int[] {1, numRows})
				|| !MathUtils.inRange(col, new int[] {1, numCols})) {
			throw new ParameterException("OED.hasOrbitals: "
					+ "row " + row + " or column " + col + " out of range.");
		} // if row and col are out of range
		return ((OEDCell) table[row - 1][col - 1]).hasOrbitals();
	} // hasOrbitals(int, int)

	/** Returns the type of a cell's orbitals.
	 * @param	row	1-based row of the cell
	 * @param	col	1-based column of the cell
	 * @throws	ParameterException	if row or cell is out of range
	 * @return	type of the cell's orbitals
	 */
	public int getOrbitalsType(int row, int col) throws ParameterException {
		if (!MathUtils.inRange(row, new int[] {1, numRows})
				|| !MathUtils.inRange(col, new int[] {1, numCols})) {
			throw new ParameterException("OED.getOrbitalType: "
					+ "row " + row + " or column " + col + " out of range.");
		} // if row or col is out of range
		return ((OEDCell) table[row - 1][col - 1]).getOrbitalsType();
	} // getOrbitalsType(int, int)

	/** Returns the name of a cell's orbitals.
	 * @param	row	1-based row of the cell
	 * @param	col	1-based column of the cell
	 * @throws	ParameterException	if row or cell is out of range
	 * @return	name of the cell's orbitals
	 */
	public String getOrbitalsName(int row, int col) throws ParameterException {
		if (!MathUtils.inRange(row, new int[] {1, numRows})
				|| !MathUtils.inRange(col, new int[] {1, numCols})) {
			throw new ParameterException("OED.getOrbitalType: "
					+ "row " + row + " or column " + col + " out of range.");
		} // if row or col is out of range
		return ((OEDCell) table[row - 1][col - 1]).getOrbitalsName();
	} // getOrbitalsName(int, int)

	/** Returns the orbitals in a particular cell.
	 * @param	row	1-based row of the cell
	 * @param	col	1-based column of the cell
	 * @throws	ParameterException	if row or cell is out of range
	 * @return	array of Orbitals
	 */
	public Orbital[] getOrbitals(int row, int col) throws ParameterException {
		if (!MathUtils.inRange(row, new int[] {1, numRows})
				|| !MathUtils.inRange(col, new int[] {1, numCols})) {
			throw new ParameterException("OED.getOrbitals: "
					+ "row " + row + " or column " + col + " out of range.");
		} // if row or col is out of range
		return ((OEDCell) table[row - 1][col - 1]).getOrbitals();
	} // getOrbitals(int, int)

	/** Returns the occupancies of orbitals in a particular cell as a
	 * colon-separated string.
	 * @param	row	1-based row of the cell
	 * @param	col	1-based column of the cell
	 * @throws	ParameterException	if row or cell is out of range
	 * @return	colon-separated string of occupancies
	 */
	public String getOccupancies(int row, int col) throws ParameterException {
		if (!MathUtils.inRange(row, new int[] {1, numRows})
				|| !MathUtils.inRange(col, new int[] {1, numCols})) {
			throw new ParameterException("OED.getOccupancies: "
					+ "row " + row + " or column " + col + " out of range.");
		} // if row or col is out of range
		return ((OEDCell) table[row - 1][col - 1]).getOccupancies();
	} // getOccupancies(int, int)

	/** Returns the total orbital count of a column.
	 * @param	col	1-based column of the cell
	 * @throws	ParameterException	if column is out of range
	 * @return	total orbital count of the column 
	 */
	public int getColumnOrbitalCount(int col) throws ParameterException {
		return getColumnOrbitalCount(col, new int[0]);
	} // getColumnOrbitalCount(int)

	/** Returns the number of orbitals of a type in a column.
	 * @param	col	1-based column of the cell
	 * @param	type	type of orbital to count
	 * @throws	ParameterException	if column is out of range
	 * @return	number of orbitals of a type in the column 
	 */
	public int getColumnOrbitalCount(int col, int type) 
			throws ParameterException {
		return getColumnOrbitalCount(col, new int[] {type});
	} // getColumnOrbitalCount(int, int)

	/** Returns the number of orbitals of one or more types in a column.
	 * @param	col	1-based column of the cell
	 * @param	types	types of orbitals to count; any if empty
	 * @throws	ParameterException	if column is out of range
	 * @return	number of orbitals of one or more types in the column 
	 */
	public int getColumnOrbitalCount(int col, int[] types) 
			throws ParameterException {
		return getColumnCount(col, types, ORBS);
	} // getColumnOrbitalCount(int, int[])

	/** Returns the total electron count of a column.
	 * @param	col	1-based column of the cell
	 * @throws	ParameterException	if column is out of range
	 * @return	total electron count of the column 
	 */
	public int getColumnElectronCount(int col) throws ParameterException {
		return getColumnElectronCount(col, new int[0]);
	} // getColumnElectronCount(int)

	/** Returns the number of electrons in orbitals of a type in a column.
	 * @param	col	1-based column of the cell
	 * @param	type	type of orbital whose electrons to count
	 * @throws	ParameterException	if column is out of range
	 * @return	number of electrons in orbitals of a type in the column 
	 */
	public int getColumnElectronCount(int col, int type) 
			throws ParameterException {
		return getColumnElectronCount(col, new int[] {type});
	} // getColumnElectronCount(int, int)

	/** Returns the number of electrons in orbitals of one or more types in a 
	 * column.
	 * @param	col	1-based column of the cell
	 * @param	types	types of orbitals whose electrons to count; any if empty
	 * @throws	ParameterException	if column is out of range
	 * @return	number of electrons in orbitals of one or more types in the 
	 * column 
	 */
	public int getColumnElectronCount(int col, int[] types) 
			throws ParameterException {
		return getColumnCount(col, types, ELECS);
	} // getColumnElectronCount(int, int[])

	/** Returns the number of orbitals of one or more types in a column, or the
	 * number of electrons therein.
	 * @param	col	1-based column of the cell
	 * @param	types	types of orbitals whose electrons to count; any if empty
	 * @param	what	what to count (orbitals or electrons)
	 * @throws	ParameterException	if column is out of range
	 * @return	number of orbitals of one or more types, or electrons therein, 
	 * in the column 
	 */
	private int getColumnCount(int col, int[] types, int what) 
			throws ParameterException {
		if (!MathUtils.inRange(col, new int[] {1, numCols})) {
			throw new ParameterException("OED.getColumnCount: column " + col 
					+ " out of range.");
		} // if col is out of range
		final boolean any = Utils.isEmpty(types);
		final List<Integer> typesList = Utils.intArrayToList(types);
		int count = 0;
		for (int rNum = 0; rNum < numRows; rNum++) {
			final OEDCell cell = (OEDCell) table[rNum][col - 1];
			final int cellOrbType = cell.getOrbitalsType();
			if (any || typesList.contains(Integer.valueOf(cellOrbType))) {
				count += (what == ORBS ? cell.getNumOrbitals()
						: cell.getNumElectrons());
			} // if should count orbitals of this cell
		} // for each cell in the column
		return count;
	} // getColumnCount(int, int[], int)

	/* ******************** String methods ********************/

	/** Determines if this orbital energy diagram equals another.
	 * @param	theOther	another orbital energy diagram
	 * @return	true if the diagrams contain the same number of orbitals at the
	 * same energy levels with the same occupancies
	 */
	@Override
	public boolean equals(Object theOther) {
		boolean isEqual = false;
		if (theOther instanceof OED) {
			final OED comp = (OED) theOther;
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
			append(getNumRows()).append(getNumCols()).
			append(toString()).toHashCode();
	} // hashCode()

	/** Creates an English description of the diagram's question datum
	 * (size, captions, labels, y-axis markings)
	 * @return an English description of the diagram's dimensions, 
	 * captions, labels, y-axis markings
	 */
	public String qDatumToEnglish() {
		final StringBuilder bld = 
				Utils.getBuilder("The diagram has ", numRows, " rows");
		final boolean haveCap1 = !Utils.isEmpty(captions[0]);
		final boolean haveCap2 = !Utils.isEmpty(captions[1]);
		final boolean haveCap3 = !Utils.isEmpty(captions[2]);
		if (haveCap1 || haveCap2 || haveCap3) {
			bld.append("; the caption");
			if (haveCap1) {
				bld.append(" of column 1 is ");
				Utils.addSpanString(bld, captions[0]);
			} // if haveCap1
			if (haveCap2) {
				if (haveCap1) {
					Utils.appendTo(bld, haveCap3 ? ',' : " and", " that");
				}
				bld.append(" of column 2 is ");
				Utils.addSpanString(bld, captions[1]);
			} // if haveCap2
			if (haveCap3) {
				if (haveCap1 && haveCap2) bld.append(',');
				if (haveCap1 || haveCap2) bld.append(" and that");
				bld.append(" of column 3 is ");
				Utils.addSpanString(bld, captions[2]);
			} // if haveCap2
		} // if have any captions
		final int numLabels = getNumLabels();
		if (numLabels > 0) {
			bld.append("; one may label orbitals in column 2 as ");
			for (int lblNum = 0; lblNum < numLabels; lblNum++) {
				Utils.addSpanString(bld, labels[lblNum]);
				if (lblNum == numLabels - 2)
					bld.append(lblNum == 0 ? " or " : ", or ");
				else if (lblNum < numLabels - 2) bld.append(", ");
			} // for each label
		} // if there are labels
		bld.append('.');
		if (yAxisScale != null && yAxisScale.haveLabels()) {
			Utils.appendTo(bld, "<br/>", yAxisScale.toEnglish());
		} // if there are labels
		return bld.toString();
	} // qDatumToEnglish()

	/** Converts the orbitals' locations, types, and occupancies into a String.
	 * @return	the string encoding the orbitals' locations, types, and 
	 * occupancies
	 */
	public String toString() {
		final StringBuilder bld = new StringBuilder();
		for (int col = 1; col <= numCols; col++) {
			try {
				final List<String> colOccupStrs = getOccupStrs(col);
				final List<Integer> colOccupRowNums = getOccupRowNums(col);
				for (int occNum = 0; occNum < colOccupStrs.size(); occNum++) {
					final String occupStr = colOccupStrs.get(occNum);
					final Integer rowNum = colOccupRowNums.get(occNum);
					if (bld.length() > 0) bld.append(CELLS_SEP);
					Utils.appendTo(bld, 
							rowNum, LOC_SEP, col, LOC_SEP, occupStr);
				} // for each occupied row
			} catch (ParameterException e) { // unlikely
				Utils.alwaysPrint("OED.toString: "
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
	 * @param	userLangs	the user's preferred languages
	 * @param	dispNum	number to assign to this table
	 * @return	an HTML table suitable for display
	 */
	public String toDisplay(String[] userLangs, int dispNum) {
		final String th = "<th class=\"boldtext\" "
				+ "style=\"text-align:center; border-bottom-style:solid; "
				+ "border-bottom-width:1px; border-bottom-color:black;\">";
		final StringBuilder bld = Utils.getBuilder(
				"<table class=\"whiteTable\" style=\"width:95%;\"><tr>");
		final boolean haveYAxisScale = haveYAxisScale();
		if (haveYAxisScale) {
			Utils.appendTo(bld, "<th>",
					PhraseTransln.translate("Energy", userLangs),
					" (", Utils.toDisplay(yAxisScale.getUnit()), ")</th>");
		} // if there are labels for the rows
		for (final String caption : captions) {
			Utils.appendTo(bld, th, Utils.toDisplay(caption), "</th>");
		} // for each caption
		// Set up graphics canvas to draw lines
		Utils.appendTo(bld, "</tr>\n<tr><th colspan=\"", 
				numCols, "\"><div id=\"canvas", dispNum,
				"\" style=\"position:relative; left:0px; top:0px; "
					+ "overflow:visible;\"></div></th></tr>\n");
		// Generate table cells
		final String[] yAxisLabels = getYAxisLabels();
		for (int rNum = numRows; rNum >= 1; rNum--) {
			bld.append("<tr>");
			if (haveYAxisScale) {
				Utils.appendTo(bld, "<th>", yAxisLabels[rNum], "</th>");
			} // if there are labels for the rows
			for (int cNum = 1; cNum <= numCols; cNum++) {
				Utils.appendTo(bld, "<td style=\"text-align:center;\" ",
						"id = \"d", dispNum, 'r', rNum, 'c', cNum,
						"\"><span>&nbsp;");
				try {
					final OEDCell cell = getCell(rNum, cNum); 
					final int numOrbs = cell.getNumOrbitals();
					if (numOrbs > 0) {
						Utils.appendTo(bld, cell.getOrbitalsName(), ": ");
						for (int oNum = 1; oNum <= numOrbs; oNum++) {
							if (oNum > 1) bld.append(", ");
							final int occup = cell.getOccupancy(oNum);
							if (occup > 0) {
								bld.append("&uarr;");
								if (occup == 2) bld.append("&darr;");
							} else bld.append("&mdash;");
							bld.append(' ');
						} // for each orbital in the cell
						final int label = cell.getLabel();
						if (label > 0) {
							Utils.appendTo(bld, 
									'[', getNameForLabel(label), "] ");
						}
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
		int numTimes = captions.length;
		while (numTimes > 0) {
			bld.append("<td class=\"boldtext\" "
					+ "style=\"text-align:center; border-top-style:solid; "
					+ "border-top-width:1px; border-top-color:black;\"></td>");
			numTimes--;
		} // for each caption
		return Utils.toString(bld, "</tr></table>");
	} // toDisplay(String[])

} // OED

