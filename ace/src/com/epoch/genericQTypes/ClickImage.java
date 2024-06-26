package com.epoch.genericQTypes;

import com.epoch.genericQTypes.genericQConstants.ClickConstants;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import com.epoch.xmlparser.XMLUtils;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Holds a response to a clickable image question. */
public class ClickImage implements ClickConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** The coordinates where a user clicked on the image. */
	transient private int[][] allCoords;
	/** The text corresponding to each mark. */
	transient private String[] markStrs;

	/** Constructor. 
	 * @param	response	the response; if in XML format, must have ", &amp;, 
	 * &lt;, and &gt; encoded as their &amp;quot;, &amp;amp;, &amp;lt;, and 
	 * &amp;gt;, and other non-ASCII characters encoded as numerical CERs
	 */
	public ClickImage(String response) {
		final String SELF = "ClickImage: ";
		debugPrint(SELF + "response:\n", response);
		parseXML(XMLUtils.xmlToNode(Utils.cersToUnicode(response)));
		debugPrint(SELF + "allCoords = ", allCoords, 
				", markStrs = ", markStrs); 
	} // ClickImage(String)

	/** Converts a node derived from XML into an array of marks and an array of 
	 * corresponding text.  The parser will convert CERs in the XML into the 
	 * corresponding Unicode characters.
	 * @param	node	node derived from XML describing these marks
	 */
	private void parseXML(Node node) {
		final String SELF = "ClickImage.parseXML: ";
		final List<int[]> coordsList = new ArrayList<int[]>();
		final List<String> markStrsList = new ArrayList<String>();
		if (node == null) {
			debugPrint(SELF + "node is null.");
		} else if (node.getNodeName().equalsIgnoreCase(XML_TAG)) {
			if (node.hasChildNodes()) {
				final NodeList children = node.getChildNodes();
				final int numChildren = children.getLength();
				for (int childNum = 0; childNum < numChildren; childNum++) {
					final Node child = children.item(childNum);
					if (child.getNodeName().equalsIgnoreCase(MARK_TAG)) {
						parseMark(child, coordsList, markStrsList);
					} // if node is a mark
				} // for each child node
			} // if there are child nodes
		} else debugPrint(SELF + "node ", node.getNodeName(),
				" is unknown.");
		allCoords = coordsList.toArray(new int[coordsList.size()][]);
		markStrs = markStrsList.toArray(new String[markStrsList.size()]);
	} // parseXML(Node)

	/** Parses a node representing a mark and adds the coordinates and text to
	 * the lists.  The parser will convert CERs in the XML into the 
	 * corresponding Unicode characters.
	 * @param	node	the node representing the mark
	 * @param	coordsList	list of coordinates of all marks
	 * @param	markStrsList	list of text of all marks
	 */
	private void parseMark(Node node, List<int[]> coordsList, 
			List<String> markStrsList) {
		final String SELF = "ClickImage.parseMark: ";
		final int[] markCoords = new int[2];
		boolean success = false;
		if (node.hasAttributes()) {
			final NamedNodeMap attributes = node.getAttributes();
			final Node xAttr = attributes.getNamedItem(X_TAG);
			if (xAttr != null) {
				markCoords[X] = MathUtils.parseInt(xAttr.getNodeValue());
			} // if there's an x-coordinate attribute
			final Node yAttr = attributes.getNamedItem(Y_TAG);
			if (yAttr != null) {
				markCoords[Y] = MathUtils.parseInt(yAttr.getNodeValue());
			} // if there's a y-coordinate attribute
			success = xAttr != null && yAttr != null;
		} // if node has attributes
		final StringBuilder markStrBld = new StringBuilder();
		if (node.hasChildNodes()) {
			// text may be split by parser into multiple nodes
			final NodeList children = node.getChildNodes();
			final int numChildren = children.getLength();
			for (int chNum = 0; chNum < numChildren; chNum++) {
				markStrBld.append(children.item(chNum).getNodeValue());
			} // for each child node
		} // if there are child nodes
		final String markStr = Utils.unicodeToCERs(markStrBld.toString());
		if (success) {
			coordsList.add(markCoords);
			markStrsList.add(markStr);
		} // if we got coords for the mark
	} // parseMark(Node, List<int[]>, List<String>)

	/** Gets the coordinates of all the marks.
	 * @return	array of array of coordinates
	 */
	public int[][] getAllCoords() 			{ return allCoords; }

	/** Gets the coordinates of a mark.
	 * @param	markNum	1-based index of a mark
	 * @return	array of coordinates
	 */
	public int[] getCoords(int markNum) 	{ return allCoords[markNum - 1]; }

	/** Gets the markStrs associated with all the marks.
	 * @return	array of markStrs
	 */
	public String[] getAllMarkStrs() 		{ return markStrs; }

	/** Gets the text associated with a mark.
	 * @param	markNum	1-based index of a mark
	 * @return	the text
	 */
	public String getMarkStr(int markNum) 	{ return markStrs[markNum - 1]; }

	/** Splits the question data of a ClickImage question into color and number
	 * of marks.
	 * @param	qdStr	the text to be split
	 * @return	array containing color and number of marks
	 */
	public static String[] getColorAndMaxMarks(String qdStr) {
		return (Utils.isEmpty(qdStr) ? DEFAULT_QD 
				: qdStr.indexOf(QD_SEP) < 0 
					? new String[] {qdStr, DEFAULT_QD[NUM_MARKS]}
				: qdStr.split(QD_SEP));
	} // getColorAndMaxMarks(String)

} // ClickImage

