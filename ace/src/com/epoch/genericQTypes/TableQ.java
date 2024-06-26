package com.epoch.genericQTypes;

import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.genericQConstants.TableQConstants;
import com.epoch.qBank.QDatum;
import com.epoch.qBank.CaptionsQDatum;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.XMLUtils;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Class for the complete-the-table question type. */
public final class TableQ implements TableQConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Response that has been parsed into an array of arrays. */
	transient public String[][] entries;
	/** Parallel array of arrays denoting whether the input cells are disabled. */
	transient public boolean[][] disabled;

/* ****************** Construction and initialization *******************/

	/** Constructor. 
	 * @param	response	a string representing a response to a Table question
	 */
	public TableQ(String response) {
		final String SELF = "TableQ: ";
		debugPrint(SELF + "response:\n", response);
		if (!parseXML(XMLUtils.xmlToNode(response))) {
			parseOldFormat(response);
		} // if can't be parsed as XML
	} // TableQ(String)

	/** Converts a node derived from XML into the table.  The parser will
	 * convert CERs in the XML into the corresponding Unicode characters.
	 * @param	node	node derived from XML describing this table
	 * @return	true if the XML was parsed successfully
	 */
	private boolean parseXML(Node node) {
		final String SELF = "TableQ.parseXML(Node): ";
		boolean success = false;
		if (node == null) {
			debugPrint(SELF + "node is null, returning false.");
		} else if (node.getNodeName().equalsIgnoreCase(TABLE_TAG)) {
			debugPrint(SELF + "entering table.");
			int numRows = 0;
			int numCols = 0;
			if (node.hasAttributes()) {
				final NamedNodeMap attributes = node.getAttributes();
				final Node rowAttr = attributes.getNamedItem(NUM_ROWS_TAG);
				if (rowAttr != null) {
					numRows = MathUtils.parseInt(rowAttr.getNodeValue());
				} // if there's a row attribute
				final Node colAttr = attributes.getNamedItem(NUM_COLS_TAG);
				if (colAttr != null) {
					numCols = MathUtils.parseInt(colAttr.getNodeValue());
				} // if there's a col attribute
			} // if there are attributes
			success = !Utils.among(0, numRows, numCols);
			if (success) {
				debugPrint(SELF + "got numRows = ", numRows, 
						", numCols = ", numCols); 
				entries = new String[numRows][numCols];
				for (final String[] row : entries) Arrays.fill(row, "");
				disabled = new boolean[numRows][numCols];
				if (node.hasChildNodes()) parseRowsXML(node.getChildNodes());
				debugPrint(SELF, "XML parsed successfully.");
			} else debugPrint(SELF, "parsing XML failed, must be old format.");
		} else if (node.getNodeName().equalsIgnoreCase(XML_TAG)) {
			if (node.hasChildNodes()) {
				final NodeList children = node.getChildNodes();
				final int numChildren = children.getLength();
				for (int childNum = 0; childNum < numChildren; childNum++) {
					final Node child = children.item(childNum);
					success = parseXML(child);
					if (success) break;
				} // for each child node
			} // if there are child nodes
		} else debugPrint(SELF + "node is unknown, returning false.");
		return success;
	} // parseXML(Node)

	/** Converts a nodelist derived from XML into the table.  The parser will
	 * convert CERs in the XML into the corresponding Unicode characters.
	 * @param	nodeList	nodeList derived from XML describing this table
	 */
	private void parseRowsXML(NodeList nodeList) {
		final String SELF = "TableQ.parseRowsXML: ";
		final int numNodes = nodeList.getLength();
		int rowNum = 0;
		for (int nodeNum = 0; nodeNum < numNodes; nodeNum++) {
			final Node node = nodeList.item(nodeNum);
			if (node.getNodeName().equalsIgnoreCase(ROW_TAG)) {
				debugPrint(SELF + "entering row ", ++rowNum, ".");
				if (node.hasChildNodes()) {
					parseCellsXML(node.getChildNodes(), rowNum);
				} // if there are child nodes
			} else if (node.getNodeType() != Node.TEXT_NODE) {
				Utils.alwaysPrint(SELF + "unknown node ", 
						node.getNodeName());
			} // if node name or type
		} // for each node
	} // parseRowsXML(NodeList)

	/** Converts a nodelist of cells derived from XML into the table.  The 
	 * parser will convert CERs in the XML into the corresponding Unicode 
	 * characters.
	 * @param	nodeList	nodeList of cells derived from XML describing 
	 * this table
	 * @param	rowNum	current 1-based row number
	 */
	private void parseCellsXML(NodeList nodeList, int rowNum) {
		final String SELF = "TableQ.parseCellsXML: ";
		final int numNodes = nodeList.getLength();
		int colNum = 0;
		for (int nodeNum = 0; nodeNum < numNodes; nodeNum++) {
			final Node node = nodeList.item(nodeNum);
			if (node.getNodeName().equalsIgnoreCase(CELL_TAG)) {
				colNum++;
				final int[] rowRange = new int[] {1, getNumRows()};
				final int[] colRange = new int[] {1, getNumCols()};
				if (MathUtils.inRange(rowNum, rowRange)
						&& MathUtils.inRange(colNum, colRange)) {
					if (node.hasAttributes()) {
						final NamedNodeMap attributes = node.getAttributes();
						disabled[rowNum - 1][colNum - 1] =
								attributes.getNamedItem(DISABLED_TAG) != null;
					} // if there are attributes
					final StringBuilder dataBld = new StringBuilder();
					// data may be split by parser into multiple nodes
					if (node.hasChildNodes()) {
						final NodeList children = node.getChildNodes();
						final int numChildren = children.getLength();
						for (int chNum = 0; chNum < numChildren; chNum++) {
							dataBld.append(children.item(chNum).getNodeValue());
						} // for each child node
					} // if there are children
					entries[rowNum - 1][colNum - 1] = dataBld.toString();
					debugPrint(SELF + "row ", rowNum, ", col ", colNum,
							", disabled = ", disabled[rowNum - 1][colNum - 1], 
							", stored value: ", entries[rowNum - 1][colNum - 1]);
				} else debugPrint(SELF + "row ", rowNum, " or col ", colNum,
						" out of range");
			} else if (node.getNodeType() != Node.TEXT_NODE) {
				Utils.alwaysPrint(SELF + "unknown node ", node.getNodeName());
			} // if node name or type
		} // for each node
	} // parseCellsXML(NodeList, int)

	/** Parses a response to a table question that's in the old, non-XML format.
	 * Obsolete when we migrate to a database with no table questions.
	 * @param	response	the response in the old format
	 */
	private void parseOldFormat(String response) {
		final String SELF = "TableQ.parseOldFormat: ";
		final String ROW_SEP = "~~~";
		final String COL_SEP = "\t";
		final char DISABLED = '`';
		String correctedResponse = (response == null 
				? "" : Utils.unicodeToCERs(response));
		debugPrint(SELF + "parsing response:\n",
				convertToDebugPrint(correctedResponse));
		correctedResponse = correctedResponse.replaceAll("\r", "");
		final String[] rows = correctedResponse.split(ROW_SEP);
		final int numRows = rows.length;
		int numCols = 0;
		for (int rowNum = 0; rowNum < numRows; rowNum++) {
			String[] oneRowCells = rows[rowNum].split(COL_SEP);
			if (rowNum == 0) {
				numCols = oneRowCells.length;
				entries = new String[numRows][numCols];
				for (final String[] row : entries) Arrays.fill(row, "");
				disabled = new boolean[numRows][numCols];
				debugPrint(SELF + "found ", numRows, " rows and ",
						numCols, " columns in the table.");
			} // if this is the first row
			for (int cellNum = 0; cellNum < oneRowCells.length; cellNum++) {
				debugPrint("Entry row ", rowNum + 1, " and column ", 
						cellNum + 1, ": ", oneRowCells[cellNum]);
				if (!Utils.isEmpty(oneRowCells[cellNum])
						&& oneRowCells[cellNum].charAt(0) == DISABLED) {
					debugPrint("Entry row ", rowNum + 1, " and column ", 
							cellNum + 1, " should be disabled.");
					oneRowCells[cellNum] = oneRowCells[cellNum].substring(1);
					debugPrint("Entry row ", rowNum + 1,
							" and column ", cellNum + 1,
							" now: ", oneRowCells[cellNum]);
					disabled[rowNum][cellNum] = true;
				} else disabled[rowNum][cellNum] = false;
			} // for each cell
			entries[rowNum] = oneRowCells;
			debugPrint(SELF, oneRowCells.length,
					" cells in row ", rowNum + 1, ": ", entries[rowNum]);
		} // for each row
	} // parseOldFormat(String)

	/** Converts an unparsed, old-format response to a complete-the-table 
	 * question to a form that is easier to read in the log.
	 * @param	resp	an old-format, unparsed response to a 
	 * complete-the-table question
	 * @return	the unparsed response in an easier-to-read format
	 */
	private static String convertToDebugPrint(String resp) {
		return resp.replaceAll("\t", "[TAB]\t")
				.replaceAll("\r", "")
				.replaceAll("\n", "[CR]\n");
	} // convertToDebugPrint(String)

/* ****************** Short get methods *******************/

	/** Gets the number of rows in this parsed table.
	 * @return	the number of rows
	 */
	public int getNumRows()		{ return entries.length; }
	/** Gets the number of columns in this parsed table.
	 * @return	the number of columns
	 */
	public int getNumCols()		{ return entries[0].length; }

/* ****************** Output methods *******************/

	/** Converts the parsed response to a complete-the-table question into HTML.
	 * @param	qData	the question qData; they contain row and column captions
	 * @param	chemFormatting	whether to use chemistry formatting of captions
	 * @param	outputType	display-only, disable some boxes, include checkboxes
	 * to indicate whether to disable
	 * @return	the parsed response as an HTML table
	 * @throws	ParameterException	if there aren't enough question data to
	 * construct the HTML table
	 */
	public String convertToHTML(QDatum[] qData, boolean chemFormatting, 
			int outputType) throws ParameterException {
		return (Utils.among(outputType, DISPLAY, AUTH_DISPLAY) 
				? convertToHTMLDisplay(qData, chemFormatting, 
					outputType == DISPLAY)
				: convertToHTMLInput(qData, chemFormatting, 
					outputType == AUTH_INPUT));
	} // convertToHTML(QDatum[], boolean, int)

	/** Converts the parsed response to a complete-the-table question into HTML for
	 * input.
	 * @param	qData	the question qData; they contain row and column captions
	 * @param	chemFormatting	whether to use chemistry formatting of captions
	 * @param	allCellsEditable	whether table is being constructed for a question
	 * author
	 * @return	the parsed response as an HTML table
	 * @throws	ParameterException	if there aren't enough question data to
	 * construct the HTML table
	 */
	public String convertToHTMLInput(QDatum[] qData, 
			boolean chemFormatting, boolean allCellsEditable) 
			throws ParameterException {
		return convertToHTMLInput(qData, chemFormatting, allCellsEditable, null);
	} // convertToHTMLInput(QDatum[], boolean, boolean)

	/** Converts the parsed response to a complete-the-table question into HTML for
	 * input.
	 * @param	qData	the question qData; they contain row and column captions
	 * @param	chemFormatting	whether to use chemistry formatting of captions
	 * @param	allCellsEditable	whether disabled cells should be made
	 * editable; true when authoring a preload table
	 * @param	colorCellsStr	cells whose background to color, as
	 * semicolon-separated list of row:column (1-based)
	 * @return	the parsed response as an HTML table
	 * @throws	ParameterException	if there aren't enough question data to
	 * construct the HTML table
	 */
	public String convertToHTMLInput(QDatum[] qData, 
			boolean chemFormatting, boolean allCellsEditable, 
			String colorCellsStr) throws ParameterException {
		if (qData.length < MIN_QDATA)
			throw new ParameterException("Insufficient number of question data "
					+ " to construct the input table.  Please alert the "
					+ "instructor to this error.");
		final CaptionsQDatum rowQDatum = new CaptionsQDatum(qData[ROW_DATA]);
		final CaptionsQDatum colQDatum = new CaptionsQDatum(qData[COL_DATA]);
		final String[] rowsCaptions = rowQDatum.captions;
		final String[] colsCaptions = colQDatum.captions;
		final int numAuthRows = rowQDatum.getNumRowsOrCols();
		final int numAuthCols = colQDatum.getNumRowsOrCols();
		// see if entries needs to be enlarged
		final int numRespRows = entries.length;
		final int numRespCols = entries[0].length;
		if (numAuthRows > numRespRows) {
			final String[][] moreRows = 
					new String[numAuthRows][entries[0].length];
			System.arraycopy(entries, 0, moreRows, 0, numRespRows);
			for (int rowNum = numRespRows; rowNum < numAuthRows; rowNum++) {
				Arrays.fill(moreRows[rowNum], " ");
			}
			entries = moreRows;
		} // if there should be more rows in entries
		final StringBuilder out = Utils.getBuilder(
				"<table class=\"whiteTable\">\n", "<tr>");
		for (int colNum = 0; colNum <= numAuthCols + 1; colNum++) {
			Utils.appendTo(out, colNum < colsCaptions.length 
							&& !Utils.isEmptyOrWhitespace(colsCaptions[colNum])
						? Utils.getBuilder("<th style=\"border-bottom:solid; "
							+ "border-width:1px;\">", chemFormatting ? 
								Utils.toDisplay(colsCaptions[colNum].trim())
								: colsCaptions[colNum].trim())
						: "<th>",
					"</th>");
			if (allCellsEditable && colNum > 1) out.append("<th></th>");
		} // for each column in caption row
		out.append("</tr>\n");
		final int lesserNumRows = (numRespRows < numAuthRows
				? numRespRows : numAuthRows);
		final int lesserNumCols = (numRespCols < numAuthCols
				? numRespCols : numAuthCols);
		final boolean[] columnsDisabled = getColumnsDisabled();
		final boolean NO_COLOR = false;
		final boolean[][] colorCells = new boolean[lesserNumRows][lesserNumCols];
		getColorCells(colorCells, colorCellsStr);
		for (int rowNum = 1; rowNum <= lesserNumRows; rowNum++) {
			Utils.appendTo(out, "<tr>", rowNum <= rowsCaptions.length
							&& !Utils.isEmptyOrWhitespace(rowsCaptions[rowNum - 1])
						? Utils.getBuilder("<td style=\"text-align:left; "
								+ "padding-right:5px;\"><b>", 
							chemFormatting 
								? Utils.toDisplay(rowsCaptions[rowNum - 1])
								: rowsCaptions[rowNum - 1],
							"</b>")
						: "<td>",
					"</td>");
			final String[] oneRow = entries[rowNum - 1];
			for (int colNum = 1; colNum <= lesserNumCols; colNum++) {
				populateCell(out, rowNum, colNum, columnsDisabled[colNum - 1], 
						allCellsEditable, PART_OF_RESP, 
						colorCells[rowNum - 1][colNum - 1]);
			} // for each column in the parsed response
			// add columns if author wants more than the parsed response has
			for (int colNum = numRespCols + 1; colNum <= numAuthCols; colNum++) {
				populateCell(out, rowNum, colNum, COL_ENABLED, 
						allCellsEditable, !PART_OF_RESP, NO_COLOR);
			} // for each column
			out.append("</tr>\n");
		} // for each row
		// add rows if author wants more than the parsed response has
		for (int rowNum = numRespRows + 1; rowNum <= numAuthRows; rowNum++) {
			Utils.appendTo(out, "<tr>", rowNum <= rowsCaptions.length
							&& !Utils.isEmptyOrWhitespace(rowsCaptions[rowNum - 1])
						? Utils.getBuilder("<td style=\"text-align:left; "
								+ "padding-right:5px;\"><b>",
							chemFormatting 
								? Utils.toDisplay(rowsCaptions[rowNum - 1])
								: rowsCaptions[rowNum - 1],
							"</b>")
						: "<td>",
					"</td>");
			for (int colNum = 1; colNum <= numAuthCols; colNum++) {
				populateCell(out, rowNum, colNum, COL_ENABLED, 
						allCellsEditable, !PART_OF_RESP, NO_COLOR);
			} // for each column
			out.append("</tr>\n");
		} // for each row
		out.append("</table>");
		return out.toString();
	} // convertToHTMLInput(QDatum[], boolean, boolean, ArrayList<int[]>)

	/** Determines whether all cells in a table column are disabled.
	 * @return	array of booleans, each true if the corresponding column
	 * contains only disabled values 
	 */
	private boolean[] getColumnsDisabled() {
		boolean[] columnsDisabled = new boolean[disabled[0].length];
		Arrays.fill(columnsDisabled, true);
		for (final boolean[] rowDisabled : disabled)
			for (int colNum = 0; colNum < rowDisabled.length; colNum++)
				if (!rowDisabled[colNum]) columnsDisabled[colNum] = false;
		return columnsDisabled;
	} // getColumnsDisabled()

	/** Converts a string containing 0-based row and column numbers of 
	 * cells to color into a two-dimensional boolean array.
	 * @param	colorCells	2D array of booleans to fill
	 * @param	colorCellsStr	cells whose background to color, as
	 * semicolon-separated list of row:column (1-based)
	 */
	private void getColorCells(boolean[][] colorCells, String colorCellsStr) {
		if (!Utils.isEmpty(colorCellsStr)) {
			final String SELF = "TableQ.getColorCells: ";
			debugPrint(SELF + "colorCellsStr = ", colorCellsStr);
			final String[] cells = colorCellsStr.split(";");
			// ignore Jlint complaint about line above.  Raphael 11/2010
			for (final String cell : cells) {
				final String[] nums = cell.split(":");
				final int rowNum = MathUtils.parseInt(nums[0]);
				final int colNum = MathUtils.parseInt(nums[1]);
				final int[] rowRange = new int[] {1, colorCells.length};
				final int[] colRange = new int[] {1, colorCells[0].length};
				if (MathUtils.inRange(rowNum, rowRange)
						&& MathUtils.inRange(colNum, colRange)) {
					colorCells[rowNum - 1][colNum - 1] = true;
					debugPrint(SELF + "color cell[", rowNum, "][", colNum, "]");
				} // if row & column are in range
			} // for each cell
		} // if there are cells to color
	} // getColorCells(String)

	/** Adds the HTML for a table cell to the growing StringBuilder.
	 * @param	out	the StringBuilder
	 * @param	rowNum	1-based row number of cell
	 * @param	colNum	1-based column number of cell
	 * @param	columnDisabled	whether all cells in the column are disabled
	 * @param	allCellsEditable	whether this table is being created for an author
	 * (all cells should be editable) or a question-solver (some may not)
	 * @param	partOfResp	whether this cell is part of a student's response or
	 * has to be added to a smaller table because the author specifies a larger
	 * table
	 * @param	colorBackground	whether to color the cell's background
	 */
	private void populateCell(StringBuilder out, int rowNum, int colNum, 
			boolean columnDisabled, boolean allCellsEditable, boolean partOfResp,
			boolean colorBackground) {
		final String INPUT_SIZE = "10";
		final String contents = (!partOfResp ? ""
				: Utils.unicodeToCERs(entries[rowNum - 1][colNum - 1].trim()));
		final boolean allowChange = !partOfResp
				|| !disabled[rowNum - 1][colNum - 1] || allCellsEditable;
		if (!Utils.isEmpty(contents)) 
			debugPrint("TableQ.populateCell: rowNum = ", rowNum, 
					", colNum = ", colNum, ", allowChange = ", 
					allowChange, ", contents before unicodeToCERs: ", 
					entries[rowNum - 1][colNum - 1], 
					", after unicodeToCERs: ", contents);
		out.append("<td");
		if (!allowChange && columnDisabled) {
			out.append(" style=\"text-align:left; "
					+ "padding-left:5px; padding-right:5px;\"");
		} // if not allowing change and column is disabled
		Utils.appendTo(out, "><input type=\"", 
				allowChange ? "text" : "hidden", "\" ");
		if (allowChange) out.append("size=\"" + INPUT_SIZE + "\" ");
		final String boxName = getTextboxName(rowNum, colNum);
		Utils.appendTo(out, "id=\"", boxName, "\" name=\"", boxName, "\" ");
		if (colorBackground) out.append("style=\"background-color:" 
				+ WRONG_BACKGROUND_COLOR + ";\" ");
		Utils.appendTo(out, "value=\"", allowChange 
				? Utils.toValidTextbox(contents)
				: Utils.toValidHTMLAttributeValue(contents), "\" />");
		if (!allowChange) {
			final String disabledName = getCheckboxName(rowNum, colNum);
			Utils.appendTo(out, "<input type=\"hidden\" id=\"",
					disabledName, "\" name=\"", disabledName, "\" value=\"",
					DISABLED_VALUE, "\" />", contents);
		} // if not editable
		if (allCellsEditable) {
			final String disabledName = getCheckboxName(rowNum, colNum);
			Utils.appendTo(out, 
					"</td><td><input type=\"checkbox\" id=\"",
					disabledName, "\" name=\"", disabledName, "\"");
			if (partOfResp && disabled[rowNum - 1][colNum - 1]) {
				out.append(" checked=\"checked\"");
			} // if checkbox should be checked
			out.append("/>");
		} // if allCellsEditable
		out.append("</td>");
	} // populateCell(StringBuilder, int, int, boolean, boolean, boolean)

	/** Converts the parsed response to a complete-the-table question into HTML 
	 * for display only.
	 * @param	qData	the question qData; they contain row and column captions
	 * @param	chemFormatting	whether to use chemistry formatting of captions
	 * @return	the parsed response as an HTML table
	 * @throws	ParameterException	if there aren't enough question data to
	 * construct the HTML table
	 */
	public String convertToHTMLDisplay(QDatum[] qData, boolean chemFormatting) 
			throws ParameterException {
		return convertToHTMLDisplay(qData, chemFormatting, true);
	} // convertToHTMLDisplay(QDatum[], boolean)

	/** Converts the parsed response to a complete-the-table question into HTML for
	 * display only.
	 * @param	qData	the question qData; they contain row and column captions
	 * @param	chemFormatting	whether to use chemistry formatting of captions
	 * @param	general	true if creating for general display (as opposed to
	 * author-only)
	 * @return	the parsed response as an HTML table
	 * @throws	ParameterException	if there aren't enough question data to
	 * construct the HTML table
	 */
	public String convertToHTMLDisplay(QDatum[] qData, boolean chemFormatting, 
			boolean general) throws ParameterException {
		if (qData.length < MIN_QDATA)
			throw new ParameterException("Insufficient number of question data "
					+ "to construct the HTML table.  Please alert the "
					+ "instructor to this error.");
		final String[] rowCaptions = 
				(new CaptionsQDatum(qData[ROW_DATA])).captions;
		final int numRowCapts = rowCaptions.length;
		final String[] colCaptions = 
				(new CaptionsQDatum(qData[COL_DATA])).captions;
		final int numColCapts = colCaptions.length;
		final boolean skip1stCol = numRowCapts == 0 
				&& (numColCapts == 0 || Utils.isEmpty(colCaptions[0]));
		final int colStart = (skip1stCol ? 1 : 0);
		final String space = "&blank;";
		final StringBuilder htmlBld = new StringBuilder();
		htmlBld.append("<table class=\"whiteTable\">\n");
		if (numColCapts != 0) {
			htmlBld.append("<tr>");
			for (int colCaptNum = colStart; colCaptNum < numColCapts; 
					colCaptNum++) {
				htmlBld.append("<th style=\"padding-left:10px; "
							+ "border-bottom:solid; "
							+ "border-width:1px; border-color:#49521B;\">");
				String colCaption = colCaptions[colCaptNum].trim();
				if (Utils.isWhitespace(colCaption)) colCaption = space;
				Utils.appendTo(htmlBld, chemFormatting 
							? Utils.toDisplay(colCaption) : colCaption,
						"</th>");
			} // for each column caption
			htmlBld.append("</tr>\n");
		} // if there are column captions
		for (int rowNum = 0; rowNum < entries.length; rowNum++) {
			htmlBld.append("<tr>");
			if (rowNum < rowCaptions.length) {
				htmlBld.append("<td style=\"padding-left:10px;\"><b>");
				String rowCaption = rowCaptions[rowNum].trim();
				if (Utils.isWhitespace(rowCaption)) rowCaption = space;
				Utils.appendTo(htmlBld, chemFormatting 
							? Utils.toDisplay(rowCaption) : rowCaption,
						"</b></td>");
			} else if (!skip1stCol) htmlBld.append("<td></td>");
			for (int colNum = 0; colNum < entries[rowNum].length; colNum++) {
				htmlBld.append("<td style=\"padding-left:10px;");
				if (!general && disabled[rowNum][colNum]) {
					htmlBld.append(DISABLED_STYLE);
				}
				htmlBld.append("\">");
				final String entry = entries[rowNum][colNum];
				Utils.appendTo(htmlBld, Utils.isWhitespace(entry) ? space 
							: MathUtils.isDouble(entry, MathUtils.TRIM)
							? Utils.formatNegative(entry) 
							: Utils.unicodeToCERs(entry), 
						"</td>");
			} // for each column
			htmlBld.append("</tr>\n");
		} // for each row
		final String html = Utils.toString(htmlBld, "</table>");
		debugPrint("TableQ.convertToHTMLDisplay: converted to:\n", html);
		return html;
	} // convertToHTMLDisplay(QDatum[], boolean, boolean)

/* ****************** Input parsing and comparison methods *******************/

	/** Determines whether two parsed responses to a complete-the-table question are
	 * equal.
	 * @param	theOther	the other parsed response to a complete-the-table question
	 * @return	true if every trimmed cell value is equal to the trimmed value
	 * in the corresponding cell in the other parsed response
	 */
	@Override
	public boolean equals(Object theOther) {
		boolean isEqual = false;
		if (theOther instanceof TableQ) {
			final TableQ tq2 = (TableQ) theOther;
			if (entries.length != tq2.entries.length
					|| entries[0].length != tq2.entries[0].length) {
				return isEqual;
			} // if number of entries
			for (int rowNum = 0; rowNum < entries.length; rowNum++) {
				for (int colNum = 0; colNum < entries.length; colNum++) {
					if (!entries[rowNum][colNum].trim().equals(
							tq2.entries[rowNum][colNum].trim()))
						return isEqual;
				} // for each column
			} // for each row
			isEqual = true;
		} // if instanceof
		return isEqual;
	} // equals(Object)

	/** Creates a hash code describing this object.
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		final HashCodeBuilder hcb = new HashCodeBuilder(17, 37);
		hcb.append(entries.length).append(entries[0].length);
		for (int rowNum = 0; rowNum < entries.length; rowNum++) {
			for (int colNum = 0; colNum < entries.length; colNum++) {
				hcb.append(entries[rowNum][colNum].trim());
			}
		}
		return hcb.toHashCode();
	} // hashCode

	/** Converts the individual values of textboxes from the front end into a
	 * single XML string.  Values are subjected to inputToCERs().
	 * @param	request	holds the values of the textboxes
	 * @return	table in XML format; non-ASCII characters are converted to 
	 * CERs, as are &lt;, &gt;, ", ', and &amp; when it is not followed by #\d+.
	 */
	public static String responseToXML(HttpServletRequest request) {
		return responseToXML(request, !TableQConstants.URIS_ENCODED);
	} // responseToXML(HttpServletRequest)

	/** Converts the individual values of textboxes from the front end into a
	 * single XML string.  Values are subjected perhaps to urisToText(), always 
	 * to inputToCERs().
	 * @param	request	holds the values of the textboxes
	 * @param	urisEncoded	true if the text was subjected to
	 * encodeURIComponent() before being submitted; if so, subject to
	 * urisToText() before inputToCERs()
	 * @return	table in XML format; non-ASCII characters are converted to 
	 * CERs, as are &lt;, &gt;, ", ', and &amp; when it is not followed by #\d+.
	 */
	public static String responseToXML(HttpServletRequest request, 
			boolean urisEncoded) {
		final String SELF = "TableQ.responseToXML: ";
		final String numRowsStr = request.getParameter(NUM_ROWS_TAG);
		final String numColsStr = request.getParameter(NUM_COLS_TAG);
		final StringBuilder xml = 
				Utils.getBuilder(startTable(numRowsStr, numColsStr));
		debugPrint(SELF + "after starting table, urisEncoded = ",
				urisEncoded, ", xml = ", xml);
		final int numRows = MathUtils.parseInt(numRowsStr);
		final int numCols = MathUtils.parseInt(numColsStr);
		int row = 0;
		while (row < numRows) {
			row++;
			final StringBuilder rowXml = new StringBuilder();
			int col = 0;
			while (col < numCols) {
				final String cellName = getTextboxName(row, ++col);
				final String origCellVal = request.getParameter(cellName);
				String cellVal = Utils.inputToCERs(urisEncoded 
						? Utils.urisToText(origCellVal) : origCellVal);
				if (cellVal == null) cellVal = "";
				final String ckboxName = getCheckboxName(row, col);
				final String disabledSetter = request.getParameter(ckboxName);
				debugPrint(SELF + "row ", row, ", col ", col,
						", cellName = ", cellName,
						", ckboxName = ", ckboxName,
						", disabledSetter = ", disabledSetter,
						", origCellVal = ", origCellVal,
						", cellVal = ", cellVal);
				final boolean isDisabled = disabledSetter != null
						&& disabledSetter.equals(DISABLED_VALUE);
				rowXml.append(makeCellNode(isDisabled, cellVal));
			} // while column
			Utils.appendTo(xml, startRow(), rowXml, endTag(ROW_TAG));
		} // while row
		final String table = Utils.toString(xml, endTag(TABLE_TAG));
		debugPrint(SELF + "table:\n", table);
		return table;
	} // responseToXML(HttpServletRequest, boolean)

	/** Starts the table XML.
	 * @param	numRowsStr	number of rows in the table
	 * @param	numColsStr	number of columns in the table
	 * @return	the tag and attributes wrapped in &lt; &gt;
	 */
	private static StringBuilder startTable(String numRowsStr, String numColsStr) {
		return XMLUtils.startTag(TABLE_TAG, 
				new String[][] { 
					{NUM_ROWS_TAG, numRowsStr}, 
					{NUM_COLS_TAG, numColsStr} 
				});
	} // startTable(String, String)

	/** Wraps a tag and attributes with &lt; &gt;.
	 * @return	the tag and attributes wrapped in &lt; &gt;
	 */
	private static StringBuilder startRow() {
		return XMLUtils.startTag(ROW_TAG, !NEWLINE);
	} // startRow()

	/** Surround the given text with the given XML tags.  Don't use
	 * XMLUtils.makeNode() to make this node because we don't want to apply 
	 * toValidXML().
	 * @param	isDisabled	whether the disabled attribute should be included
	 * @param	text	the text of the node
	 * @return	the text surrounded by XML tags
	 */
	private static StringBuilder makeCellNode(boolean isDisabled, 
			String text) {
		return Utils.getBuilder(isDisabled 
					? XMLUtils.startTag(CELL_TAG,
						new String[][] { {DISABLED_TAG, DISABLED_VALUE} },
						!NEWLINE)
					: XMLUtils.startTag(CELL_TAG, !NEWLINE),
				Utils.toValidTextbox(text),
				XMLUtils.endTag(CELL_TAG, !NEWLINE));
	} // makeCellNode(boolean, String)

	/** Wraps a tag with &lt;/ &gt;.
	 * @param	tag	the name of the XML tag
	 * @return	the tag wrapped in &lt;/ &gt;
	 */
	private static StringBuilder endTag(String tag) {
		return XMLUtils.endTag(tag);
	} // endTag(String)

	/** Makes the name of a textbox in a table question.
	 * @param	row	1-based row
	 * @param	col	1-based column
	 * @return	name of the textbox
	 */
	private static String getTextboxName(int row, int col) {
		return getBoxName(CELL_ID_START, row, col);
	} // getTextboxName(int, int)

	/** Makes the name of a checkbox in a table question.
	 * @param	row	1-based row
	 * @param	col	1-based column
	 * @return	name of the textbox
	 */
	private static String getCheckboxName(int row, int col) {
		return getBoxName(CKBOX_ID_START, row, col);
	} // getCheckboxName(int, int)

	/** Makes the name of a checkbox or textbox in a table question.
	 * @param	start	start of the name of the box
	 * @param	row	1-based row
	 * @param	col	1-based column
	 * @return	name of the box
	 */
	private static String getBoxName(String start, int row, int col) {
		return Utils.toString(start, row, ROW_COL_SEP, col);
	} // getBoxName(String, int, int)

} // TableQ
