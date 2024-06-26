package com.epoch.genericQTypes;

import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.genericQConstants.TableQConstants;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.XMLUtils;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Class for the logical statements question type. */
public final class Logic implements TableQConstants {

	private static void debugPrint(Object... msg) {
		 Utils.printToLog(msg);
	}

	/** List of statements that constitute the response. */
	transient public List<String> stmts = new ArrayList<String>();

	/** Constructor. 
	 * @param	response	a string representing a response to a Logic question
	 * @throws	ParameterException	if string cannot be parsed
	 */
	public Logic(String response) throws ParameterException {
		final String SELF = "Logic: ";
		if (!Utils.isEmptyOrWhitespace(response)) {
			debugPrint(SELF + "response:\n", response);
			final boolean success = parseXML(XMLUtils.xmlToNode(response));
			if (!success) throw new ParameterException(
					"Logic: could not parse:\n" + response);
		} // if response is not empty
	} // Logic(String)

	/** Converts a node derived from XML into the list of statements.  
	 * The parser will convert CERs in the XML into the corresponding 
	 * Unicode characters.
	 * @param	node	node derived from XML describing this list of 
	 * statements
	 * @return	true if the XML was parsed successfully
	 */
	private boolean parseXML(Node node) {
		final String SELF = "Logic.parseXML: ";
		boolean success = false;
		if (node == null) {
			debugPrint(SELF + "node is null, returning false.");
		} else if (node.getNodeName().equalsIgnoreCase(TABLE_TAG)) {
			if (node.hasChildNodes()) parseRowsXML(node.getChildNodes());
			success = true;
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

	/** Converts a nodelist derived from XML into the list of statements.  
	 * The parser will convert CERs in the XML into the corresponding Unicode 
	 * characters.  
	 * @param	nodeList	nodeList of cells derived from XML describing 
	 * this list of statements
	 */
	private void parseRowsXML(NodeList nodeList) {
		final String SELF = "Logic.parseRowsXML: ";
		final int numNodes = nodeList.getLength();
		for (int nodeNum = 0; nodeNum < numNodes; nodeNum++) {
			final Node node = nodeList.item(nodeNum);
			if (node.getNodeName().equalsIgnoreCase(ROW_TAG)) {
				if (node.hasChildNodes()) {
					parseCellsXML(node.getChildNodes());
				} // if there are child nodes
			} else if (node.getNodeType() != Node.TEXT_NODE) {
				Utils.alwaysPrint(SELF + "unknown node ", 
						node.getNodeName());
			} // if node name or type
		} // for each node
	} // parseRowsXML(NodeList)

	/** Converts a nodelist of cells derived from XML into the list of 
	 * statements.  The parser will convert CERs in the XML into the 
	 * corresponding Unicode characters.
	 * @param	nodeList	nodeList of cells derived from XML describing 
	 * this list of statements
	 */
	private void parseCellsXML(NodeList nodeList) {
		final String SELF = "Logic.parseCellsXML: ";
		final int numNodes = nodeList.getLength();
		for (int nodeNum = 0; nodeNum < numNodes; nodeNum++) {
			final Node node = nodeList.item(nodeNum);
			if (node.getNodeName().equalsIgnoreCase(CELL_TAG)) {
				final StringBuilder dataBld = new StringBuilder();
				// data may be split by parser into multiple nodes
				if (node.hasChildNodes()) {
					final NodeList children = node.getChildNodes();
					final int numChildren = children.getLength();
					for (int chNum = 0; chNum < numChildren; chNum++) {
						dataBld.append(children.item(chNum).getNodeValue());
					} // for each child node
				} // if there are children
				final String stmt = Utils.cersToUnicode(dataBld.toString().trim());
				stmts.add(stmt);
				debugPrint(SELF + "statement ", stmts.size(),
						" value: ", stmt);
			} else if (node.getNodeType() != Node.TEXT_NODE) {
				Utils.alwaysPrint(SELF + "unknown node ", node.getNodeName());
			} // if node name or type
		} // for each node
	} // parseCellsXML(NodeList)

	/** Gets (a copy of) the statements of the response.
	 * @return	an array of statements
	 */
	public String[] getStatements() {
		return stmts.toArray(new String[stmts.size()]);
	} // getStatements()

	/** Gets the statements of the response as a single paragraph.
	 * @return	a paragraph of sentences
	 */
	public String getParagraph() {
		final StringBuilder bld = new StringBuilder();
		for (final String stmt : stmts) {
			if (bld.length() > 0) bld.append(' ');
			bld.append(stmt);
			if (!stmt.endsWith(".")) bld.append('.');
		} // for each stmt
		return bld.toString();
	} // getParagraph()

	/** Converts the parsed response to HTML.
	 * @return	the parsed response as HTML
	 */
	public String convertToHTML() {
		final StringBuilder bld = Utils.getBuilder(
				"<table class=\"whiteTable\" style=\"text-align:left;\">\n");
		boolean first = true;
		for (final String stmt : stmts) {
			Utils.appendTo(bld, "<tr><td>", first ? "" : "&rarr; ",
					modifyStmt(stmt), '.', "</td></tr>");
			if (first) first = false;
		} // for each statement
		return Utils.toString(bld, "</table>");
	} // convertToHTML()

	/** Removes a final punctuation mark from a statement, if necessary.
	 * @param	stmt	the statement
	 * @return the statement minus the final punctuation mark
	 */
	private String modifyStmt(String stmt) {
		if (Utils.isEmpty(stmt)) return "";
		final int len = stmt.length();
		final String lastChar = stmt.substring(len - 1);
		return Utils.unicodeToCERs(
				Utils.toDisplay(".,;:".indexOf(lastChar) >= 0 
					? Utils.rightChop(stmt, 1) : stmt));
	} // modifyStmt(String)

} // Logic
