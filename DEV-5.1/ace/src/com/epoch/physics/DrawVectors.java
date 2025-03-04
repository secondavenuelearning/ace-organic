package com.epoch.physics;

import chemaxon.struc.DPoint3;
import com.epoch.exceptions.ParameterException;
import com.epoch.physics.physicsConstants.DrawVectorsConstants;
import com.epoch.chem.VectorMath;
import com.epoch.xmlparser.XMLUtils;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Holds a response to a draw-vectors question. */
public class DrawVectors implements DrawVectorsConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** An array of vectors, each described by an array of two points. */
	transient private DPoint3[][] vectorsPts;

	/** Constructor. 
	 * @param	vectorCoords	array of four points describing an vector
	 */
	public DrawVectors(int[] vectorCoords) {
		vectorsPts = new DPoint3[1][];
		vectorsPts[0] = getVectorPoints(vectorCoords);
	} // DrawVectors(int[])

	/** Constructor. 
	 * @param	response	a response in XML format. Must have ", &amp;, 
	 * &lt;, and &gt; encoded as &amp;quot;, &amp;amp;, &amp;lt;, and 
	 * &amp;gt;, and non-ASCII characters encoded as numerical CERs
	 * @throws	ParameterException	if there are not four numbers in any set
	 */
	public DrawVectors(String response) throws ParameterException {
		final String SELF = "DrawVectors: ";
		debugPrint(SELF + "response:\n", response);
		if (Utils.isEmpty(response)) vectorsPts = new DPoint3[0][2];
		else parseXML(XMLUtils.xmlToNode(Utils.cersToUnicode(response)));
		debugPrint(SELF + "vectorsPts = ", vectorsPts);
	} // DrawVectors(String)

	/** Converts a node derived from XML into the array of vector origins and
	 * targets.  
	 * @param	node	node derived from XML describing the vectors
	 */
	private void parseXML(Node node) {
		final String SELF = "DrawVectors.parseXML: ";
		// final boolean success = false; // unused 11/6/2012
		final List<DPoint3[]> vectorsPtsList = new ArrayList<DPoint3[]>();
		if (node == null) {
			debugPrint(SELF + "node is null, returning false.");
		} else if (node.getNodeName().equalsIgnoreCase(XML_TAG)) {
			if (node.hasChildNodes()) {
				final NodeList children = node.getChildNodes();
				final int numChildren = children.getLength();
				debugPrint(SELF + "XML node has ", numChildren, 
						" child(ren).");
				for (int childNum = 0; childNum < numChildren; childNum++) {
					final Node child = children.item(childNum);
					if (child.getNodeName().equalsIgnoreCase(VECTOR_TAG)) {
						debugPrint(SELF + "found vector node.");
						final DPoint3[] vectorPts = parseVector(child);
						if (vectorPts != null) vectorsPtsList.add(vectorPts);
					} // if node is a vector
				} // for each child node
			} // if there are child nodes
		} else debugPrint(SELF + "node ", node.getNodeName(),
				" is unknown, returning false.");
		vectorsPts = 
				vectorsPtsList.toArray(new DPoint3[vectorsPtsList.size()][]);
	} // parseXML(Node)

	/** Parses a node representing a vector.
	 * @param	node	the node representing the vector
	 * @return	an array of two points, representing a vector
	 */
	private DPoint3[] parseVector(Node node) {
		final String SELF = "DrawVectors.parseVector: ";
		final DPoint3[] vectorPts = new DPoint3[2];
		boolean success = false;
		final NodeList children = node.getChildNodes();
		final int numChildren = children.getLength();
		debugPrint(SELF + "vector node has ", numChildren, " child(ren).");
		for (int childNum = 0; childNum < numChildren; childNum++) {
			final Node child = children.item(childNum);
			if (child.getNodeName().equalsIgnoreCase(ORIGIN_TAG)) {
				debugPrint(SELF + "found origin node");
				vectorPts[ORIGIN] = parsePoint(child);
			} else if (child.getNodeName().equalsIgnoreCase(TARGET_TAG)) {
				debugPrint(SELF + "found target node");
				vectorPts[TARGET] = parsePoint(child);
			} // if node is a vector
			success = vectorPts[ORIGIN] != null && vectorPts[TARGET] != null;
		} // for each child node
		debugPrint(SELF + "vectorPts = ", vectorPts, ", success = ", success);
		return (success ? vectorPts : null);
	} // parseVector(Node)

	/** Parses a node representing a point.
	 * @param	node	the node representing the point 
	 * @return	the point
	 */
	private DPoint3 parsePoint(Node node) {
		final String SELF = "DrawVectors.parsePoint: ";
		boolean success = false;
		final int[] ptCoords = new int[2];
		if (node.hasAttributes()) {
			final NamedNodeMap attributes = node.getAttributes();
			final Node xAttr = attributes.getNamedItem(X_TAG);
			if (xAttr != null) {
				ptCoords[X] = MathUtils.parseInt(xAttr.getNodeValue());
				debugPrint(SELF + "X attribute has value ", ptCoords[X]);
			} // if there's an x-coordinate attribute
			final Node yAttr = attributes.getNamedItem(Y_TAG);
			if (yAttr != null) {
				ptCoords[Y] = MathUtils.parseInt(yAttr.getNodeValue());
				debugPrint(SELF + "Y attribute has value ", ptCoords[Y]);
			} // if there's a y-coordinate attribute
			success = xAttr != null && yAttr != null;
		} // if node has attributes
		debugPrint(SELF + "ptCoords = ", ptCoords, ", success = ", success);
		return (success ? new DPoint3(ptCoords[X], ptCoords[Y], 0) : null);
	} // parsePoint(Node)

	/** Gets an array of four-coordinate arrays, each describing the starting 
	 * and ending points of a vector.
	 * @return	array of four-coordinate arrays
	 */
	public int[][] getAllCoords() {
		final int[][] allCoords = new int[vectorsPts.length][4];
		int vecNum = 0;
		for (final DPoint3[] vectorPts : vectorsPts) {
			allCoords[vecNum++] = getCoords(vectorPts);
		} // for each vector
		return allCoords;
	} // getAllCoords()

	/** Gets the four coordinates describing a vector.
	 * @param	vectorPts	array of two points describing a vector
	 * @return	array of four coordinates
	 */
	public static int[] getCoords(DPoint3[] vectorPts) {
		return new int[] {
				MathUtils.roundToInt(vectorPts[ORIGIN].x),
				MathUtils.roundToInt(vectorPts[ORIGIN].y),
				MathUtils.roundToInt(vectorPts[TARGET].x),
				MathUtils.roundToInt(vectorPts[TARGET].y)};
	} // getCoords(DPoint3[])

	/** Gets the vectors' coordinates.
	 * @return	the vectors' coordinates
	 */
	public DPoint3[][] getVectorPoints() 	{ return vectorsPts; }

	/** Gets the two points describing a vector from an array of four 
	 * coordinates.
	 * @param	vectorCoords	four coordinates of the two points
	 * @return	two-membered array of points
	 */
	public static DPoint3[] getVectorPoints(int[] vectorCoords) {
		return new DPoint3[] {
				new DPoint3(vectorCoords[X1], vectorCoords[Y1], 0),
				new DPoint3(vectorCoords[X2], vectorCoords[Y2], 0)};
	} // getVectorPoints(int[])

	/** Gets the vectors (starting from the origin) of this object.
	 * @return	the vectors
	 */
	public DPoint3[] getVectors() {
		final List<DPoint3> vectors = new ArrayList<DPoint3>();
		for (final DPoint3[] vectorPts : vectorsPts) {
			vectors.add(getVector(vectorPts));
		} // for each vector
		return vectors.toArray(new DPoint3[vectors.size()]);
	} // getVectors()

	/** Gets a vector (starting from the origin).
	 * @param	vectorNum	1-based number of the vector
	 * @return	the vector
	 */
	public DPoint3 getVector(int vectorNum) {
		return (vectorNum >= 1 && vectorNum <= vectorsPts.length 
				? getVector(vectorsPts[vectorNum - 1]) : new DPoint3());
	} // getVector(int)

	/** Gets a vector (starting from the origin) from two points.
	 * @param	vectorPoints	array of two points describing the vector
	 * @return	the difference between the points
	 */
	public static DPoint3 getVector(DPoint3[] vectorPoints) {
		return VectorMath.diff(vectorPoints[TARGET], vectorPoints[ORIGIN]);
	} // getVector(DPoint3[])

} // DrawVectors

