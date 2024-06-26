package com.epoch.evals.impl.genericQEvals.clickEvals;

import com.epoch.evals.EvalInterface;
import com.epoch.evals.OneEvalResult;
import com.epoch.evals.impl.Counter;
import com.epoch.evals.impl.implConstants.CountConstants;
import com.epoch.exceptions.ParameterException;
import com.epoch.genericQTypes.ClickImage;
import com.epoch.genericQTypes.genericQConstants.ClickConstants;
import com.epoch.responses.Response;
import com.epoch.xmlparser.XMLUtils;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** If {no, the only, exactly one, any, not every, every} mark is in the 
 * region(s) defined by the author ...  */
public class ClickHere extends Counter 
		implements ClickConstants, CountConstants, EvalInterface {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Author's shapes in which response marks may reside, set by 
	 * isResponseMatching() when authString is parsed.  Three-D array; 1st 
	 * dimension is shape type, second dimension is shape number, third 
	 * dimension is x, y, width, height. */
	transient protected int[][][] allShapes = new int[SHAPE_TAGS.length][0][4];
	/** Tags for shapes in XML format, in order of their constants in
	 * ClickConstants. */
	public static final String[] SHAPE_TAGS = new String[] 
			{"rectangle", "ellipse", "circle"};

	/** Constructor. */
	public ClickHere() { // default value
		howMany = ANY;
	} // ClickHere()

	/** Constructor.
	 * @param	data	the coded data for this evaluator; format:<br>
	 * <code>howMany</code>
	 * @throws	ParameterException	if the coded data is inappropriate
	 * for this evaluator
	 */
	public ClickHere(String data) throws ParameterException {
		final String[] splitData = data.split("/");
		howMany = MathUtils.parseInt(splitData[0]); // inherited from Counter
	} // ClickHere(String)

	/** Gets a string representation of data that this
	 * evaluator uses to evaluate a response.  Format is:<br>
	 * <code>howMany</code>
	 * @return	the coded data
	 */
	public String getCodedData() {
		return String.valueOf(howMany);
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
		return Utils.toString("If", HOWMANY_ENGL[howMany - 1],
				"mark is in the given region(s)");
	} // toEnglish()

	/** Determines whether the user has clicked in the indicated region(s).
	 * @param	response	a parsed response
	 * @param	authString	XML describing the shapes of the regions and their 
	 * coordinates
	 * @return	a OneEvalResult containing a boolean that is true if the
	 * evaluator has been satisfied.  May also contain a message describing
	 * an inability to evaluate the response because it was malformed.
	 */
	public OneEvalResult isResponseMatching(Response response,
			String authString) {
		final String SELF = "ClickHere.isResponseMatching: ";
		debugPrint(SELF, toEnglish());
		final OneEvalResult evalResult = new OneEvalResult();
		final ClickImage clickResp = (ClickImage) response.parsedResp;
		final int[][] allRespCoords = clickResp.getAllCoords();
		extractShapes(authString);
		debugPrint(SELF + "allRespCoords = ", allRespCoords,
				", authString = ", authString,
				", allShapes = ", allShapes);
		final int[] matches = new int[2];
		int markNum = 0;
		for (final int[] respCoords : allRespCoords) {
			boolean markInRegion = false;
			int shapeNum = 0;
			for (final int[][] shapes : allShapes) {
				for (final int[] shape : shapes) {
					markInRegion = inRegion(respCoords, shape, shapeNum); 
					if (markInRegion) break;
				} // for each shape
				if (markInRegion) break;
				shapeNum++;
			} // for each type of shape
			if (markInRegion) {
				matches[MATCHES]++;
				debugPrint(SELF + "mark ", ++markNum, " with coords ",
						respCoords, " is in region.");
				if (Utils.among(howMany, ANY, NONE)) {
					debugPrint(SELF + "we know enough to make a decision.");
					break;
				} // if we know enough now
			} else {
				matches[NONMATCHES]++;
				debugPrint(SELF + "mark ", ++markNum, " with coords ",
						respCoords, " is not in region.");
				if (Utils.among(howMany, NOT_ALL, ALL)) {
					debugPrint(SELF + "we know enough to make a decision.");
					break;
				} // if we know enough now
			} // if mark was in region
		} // for each mark
		evalResult.isSatisfied = getIsSatisfied(matches);
		debugPrint(SELF + "matches = ", matches[MATCHES],
				", nonmatches = ", matches[NONMATCHES],
				", isSatisfied = ", evalResult.isSatisfied);
		return evalResult;
	} // isResponseMatching(Response, String)

	/** Extract coordinates and dimensions of shapes (and maybe some text) from
	 * XML.
	 * @param	allShapesStr	XML describing the shapes
	 * @return	author's text (empty string if none)
	 */
	public String extractShapes(String allShapesStr) {
		final String SELF = "ClickHere.extractShapes: ";
		debugPrint(SELF + "extracting shapes from:\n", allShapesStr);
		final String authText = parseXML(XMLUtils.xmlToNode(allShapesStr));
		debugPrint(SELF + "allShapes = ", allShapes, ", authText = ", authText);
		return authText;
	} // extractShapes(String)

	/** Converts a node derived from XML into an array of three arrays of 
	 * coordinates and dimensions.
	 * @param	node	node derived from XML describing these shapes
	 * @return	author's text (if any)
	 */
	private String parseXML(Node node) {
		final String SELF = "ClickHere.parseXML: ";
		// final boolean success = false; // unused 11/6/2012
		final List<ArrayList<int[]>> allShapesList = 
				new ArrayList<ArrayList<int[]>>();
		for (final String dummy : SHAPE_TAGS) {
			allShapesList.add(new ArrayList<int[]>());
		} // for each shape
		final StringBuilder textBld = new StringBuilder();
		if (node.getNodeName().equalsIgnoreCase(XML_TAG) &&
				node.hasChildNodes()) {
			final NodeList children = node.getChildNodes();
			final int numChildren = children.getLength();
			for (int childNum = 0; childNum < numChildren; childNum++) {
				final Node child = children.item(childNum);
				final String childName = child.getNodeName();
				final int shapeNum = Utils.indexOf(SHAPE_TAGS, childName);
				if (shapeNum >= 0) {
					allShapesList.get(shapeNum).add(parseShape(child));
				} else if (childName.equalsIgnoreCase(TEXT_TAG)
						&& child.hasChildNodes()) {
					final NodeList grandkids = child.getChildNodes();
					final int numGrandkids = grandkids.getLength();
					for (int gkNum = 0; gkNum < numGrandkids; gkNum++) {
						textBld.append(
								grandkids.item(gkNum).getNodeValue());
					} // for each grandchild node
				} // if node is a shape or text
			} // for each child node
		} // if node ix XML and there are child nodes
		int shapeNum = 0;
		for (final ArrayList<int[]> shapesList : allShapesList) {
			allShapes[shapeNum++] = 
					shapesList.toArray(new int[shapesList.size()][]);
		} // for each shapes list
		return textBld.toString();
	} // parseXML(Node)

	/** Parses a node representing a shape.
	 * @param	node	the node representing the mark
	 * @return	four-membered array representing coordinates and dimensions
	 */
	private static int[] parseShape(Node node) {
		final String SELF = "ClickImage.parseShape: ";
		final int[] coordsDims = new int[4];
		if (node.hasAttributes()) {
			final NamedNodeMap attributes = node.getAttributes();
			final Node xAttr = attributes.getNamedItem(X_TAG);
			if (xAttr != null) {
				coordsDims[X] = MathUtils.parseInt(xAttr.getNodeValue());
			} // if there's an x-coordinate attribute
			final Node yAttr = attributes.getNamedItem(Y_TAG);
			if (yAttr != null) {
				coordsDims[Y] = MathUtils.parseInt(yAttr.getNodeValue());
			} // if there's a y-coordinate attribute
			final Node widthAttr = attributes.getNamedItem(WIDTH_TAG);
			if (widthAttr != null) {
				coordsDims[WIDTH] = MathUtils.parseInt(widthAttr.getNodeValue());
			} // if there's a width-coordinate attribute
			final Node heightAttr = attributes.getNamedItem(HEIGHT_TAG);
			if (heightAttr != null) {
				coordsDims[HEIGHT] = MathUtils.parseInt(heightAttr.getNodeValue());
			} // if there's a height-coordinate attribute
		} // if node has attributes
		return coordsDims;
	} // parseShape(Node)

	/** Gets whether a point is on or inside a shape.
	 * @param	coords	coordinates of the point
	 * @param	region	four numbers describing the shape: first two numbers are
	 * coordinates, second two are width and height.  For rectangles, the
	 * coordinates describe the upper left corner of the rectangle; for circles
	 * and ellipses, they describe the upper left corner of the rectangle or
	 * square containing the ellipse or circle.
	 * @param	shapeNum	type of shape: rectangle, ellipse, or circle
	 * @return	true if the point is in the shape
	 */
	protected boolean inRegion(int[] coords, int[] region, int shapeNum) {
		final String SELF = "ClickHere.inRegion: ";
		boolean isInRegion = false;
		if (shapeNum == RECT) {
			final int[] xRange = 
					new int[] {region[X], region[X] + region[WIDTH]};
			final int[] yRange = 
					new int[] {region[Y], region[Y] + region[HEIGHT]};
			isInRegion = MathUtils.inRange(coords[X], xRange)
					&& MathUtils.inRange(coords[Y], yRange);
			debugPrint(SELF + "coords = ", coords, ", rectangle = ",
					region, ", isInRegion = ", isInRegion);
		} else { // CIRC, ELLIP
			final double xHalfAxis = ((double) region[WIDTH]) / 2.0;
			final double yHalfAxis = ((double) region[HEIGHT]) / 2.0;
			final double[] center = new double[] {
					((double) region[X]) + xHalfAxis,
					((double) region[Y]) + yHalfAxis};
			final double xDist = ((double) coords[X]) - center[X];
			final double yDist = ((double) coords[Y]) - center[Y];
			final double xRatio = xDist / xHalfAxis;
			final double yRatio = yDist / yHalfAxis;
			final double normDistSqr = xRatio * xRatio + yRatio * yRatio;
			isInRegion = normDistSqr <= 1;
			debugPrint(SELF + "coords = ", coords, ", ",
					(shapeNum == CIRC ? "circle" : "ellipse"),
					" center = ", center, ", xHalfAxis = ",
					xHalfAxis, ", yHalfAxis = ", yHalfAxis, 
					", xDist = ", xDist, ", yDist = ", yDist,
					", xRatio = ", xRatio, ", yRatio = ", yRatio,
					", normDistSqr = ", normDistSqr,
					", isInRegion = ", isInRegion);
		} // CIRC, ELLIP
		return isInRegion;
	} // inRegion(int[], int[], int)

	/* *************** Get-set methods *****************/

	/** Gets the code for identifying this evaluator's type in the database.
	 * @return	short string describing the type of this evaluator
	 */
	public String getMatchCode() 					{ return EVAL_CODES[CLICK_HERE]; } 
	/** Gets the coordinates and dimensions of the parsed shapes.
	 * @return 3-membered array of arrays of four coordinates/dimensions
	 */
	public int[][][] getAllShapes()					{ return allShapes; }
	/** Not used.  Required by interface.
	 * @param	molName	name of the molecule
	 */
	public void setMolName(String molName) 			{ /* intentionally empty */ }
	/** Gets whether to calculate the grade from the response.  Required by
	 * interface.
	 * @return	true if should calculate the grade from the response
	 */
	public boolean getCalcGrade() 					{ return false; }

} // ClickHere

