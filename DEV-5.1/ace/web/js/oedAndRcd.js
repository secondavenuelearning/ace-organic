// <!-- avoid parsing the following as HTML
/* Methods for drawing an orbital energy or reaction coordinate diagram
and encoding the data in XML format.
Calling page must have previously included
		/js/edJava.jsp (imports energyDiagram classes)
Calling page must also call
		js/wz_jsgraphics.js
		js/position.js
Calling page must have an element called "canvas<%= canvasNum %>".
If just showing a diagram (not editing one), including page should call
the following code, with initED() called by body onload property:

	<% final OED oedResp = (isOED ? new OED(qData) : null);
	final RCD rcdResp = (isRCD ? new RCD(qData) : null);
	int canvasNum = 0; // or another number
	%>
		function initED() {
			initGraphics(<%= canvasNum %>);
			<% List<CellsLine> conLines;
			if (isOED) {
				oedResp.setOrbitals(lastResp);
				conLines = oedResp.getLines();
			} else {
				rcdResp.setStates(lastResp);
				conLines = rcdResp.getLines();
			} // if isOED
			for (int lineNum = 0; lineNum < conLines.size(); lineNum += 1) {
				final CellsLine line = conLines.get(lineNum);
				final int ARow = line.endPoints[0].getRow();
				final int ACol = line.endPoints[0].getColumn();
				final int BRow = line.endPoints[1].getRow();
				final int BCol = line.endPoints[1].getColumn(); %>
				initLine('r<%= ARow %>c<%= ACol %>', 'r<%= BRow %>c<%= BCol %>');
			<% } // for each line %>
			updateCanvas();
		} // initED()

	<%= isOED ? oedResp.toDisplay(canvasNum) : rcdResp.toDisplay(canvasNum) %>

(If there is only one diagram, you may omit the parameter to toDisplay().)
If editing a diagram, additional code must be present in initED(),
and calling page must also call js/oed.js and js/rcd.js; see
authortool/evaluators/loadEvaluatorChem.jsp.h and homework/answerJS.jsp.h
for examples.
*/

/*jsl:option explicit*/
/*jsl:import wz_jsgraphics.js*/
/*jsl:import oed.js*/

var SELECTED = ' selected="selected"';
var IN_DIAGRAM = true;

var DIAGRAM_TAG;
var IS_OED_TAG;
var CELL_TAG;
var LINE_TAG;
var ROW_TAG;
var COLUMN_TAG;
var LABEL_TAG;
var ENDPT_TAG;
var NEW_LINE = true;
var SELF_CLOSE = true;

var noCellSelected = true;
var startCell;	// a cell to be connected by a line
var endCell;	// a cell to be connected by a line
var lines = []; // array of lines
var labels = [];

var jsGraphics;


// line object constructor for connecting two cells
function Line(cell1, cell2) {
	"use strict";
	this.cellA = cell1;
	this.cellB = cell2;
} // Line()

// highlights a cell as the user mouses over it
function darkenCell(name) {
	"use strict";
	var lightGray = '#DDDDDD';
	document.getElementById(name).style.color = lightGray;
} // darkenCell()

// determines whether to create a new entry or move an existing one
function dropMove(row, col) {
	"use strict";
	if (noCellSelected) {
		dropNew(row, col);
	} else {
		moveHere(row, col);
	} // if no cell is selected
} // dropMove()

function getCellCol(cellId) {
	"use strict";
	return cellId.substring(cellId.indexOf('c') + 1);
} // getCellCol()

function getCellRow(cellId) {
	"use strict";
	return cellId.substring(cellId.indexOf('r') + 1, cellId.indexOf('c'));
} // getCellRow()

// creates a pulldown menu for setting the label of an orbital
// or a maximum or minimum
function getLabelSelector(name, label) {
	"use strict";
	var lblNum, attrs = [], optionNode;
	var xmlDoc = getParentXMLDoc('select');
	var selectNode = getFirstNode(xmlDoc, 'select');
	attrs.push(["id", name]);
	setAttributes(selectNode, attrs);
	for (lblNum = 0; lblNum <= labels.length; lblNum += 1) {
		attrs = [];
		attrs.push(['value', lblNum]);
		if (label == lblNum) {
			attrs.push(['selected', 'selected']);
		}
		optionNode = addNewNode(xmlDoc, selectNode, 'option', attrs);
		if (lblNum > 0) {
			optionNode.appendChild(getTextNode(xmlDoc, cerToUnicode(labels[lblNum - 1])));
		}
	} // for each label plus one
	var output = unicodeToCER(getXML(xmlDoc));
	// alert(output);
	return output;
} // getLabelSelector()

function initGraphics(num) {
	"use strict";
	jsGraphics = new JSGraphics('canvas' + num);
	jsGraphics.setColor('#3366FF'); // light blue
	jsGraphics.setStroke(2);
} // initGraphics()

// initialize a line
function initLine(cellAId, cellBId) {
	"use strict";
	lines[lines.length] = new Line(document.getElementById(cellAId),
			document.getElementById(cellBId));
} // initLine()

// unhighlights a cell as the user mouses away from it
function lightenCell(name) {
	"use strict";
	document.getElementById(name).style.color = 'white';
} // lightenCell()

function lineExists(cell1, cell2) {
	"use strict";
	var lineNum, line;
	for (lineNum = 0; lineNum < lines.length; lineNum += 1) {
		line = lines[lineNum];
		if ((line.cellA.id == cell1.id && line.cellB.id == cell2.id) ||
				(line.cellA.id == cell2.id && line.cellB.id == cell1.id)) {
			return lineNum;
		}
	}
	return -1;
} // lineExists()

// moves lines from cell1 to cell2.  Cells should be in same column.
function moveLines(cell1, cell2) {
	"use strict";
	var lineNum, line;
	for (lineNum = lines.length - 1; lineNum >= 0; lineNum -= 1) {
		line = lines[lineNum];
		if (line.cellA.id == cell1.id) {
			if (Math.abs(line.cellB.cellIndex - cell2.cellIndex) === 1) {
				lines[lineNum].cellA = cell2;
			} // if this line's cellB is one column from cell2
		} // if this line's cellA connects to cell1
		if (line.cellB.id == cell1.id) {
			if (Math.abs(line.cellA.cellIndex - cell2.cellIndex) === 1) {
				lines[lineNum].cellB = cell2;
			} // if this line's cellA is one column from cell2
		} // if this line's cellB connects to cell1
	} // for each line
} // moveLines()

// removes a line from the lines array
function removeLine(index) {
	"use strict";
	var lineNum;
	for (lineNum = index; lineNum < lines.length - 1; lineNum += 1) {
		lines[lineNum] = lines[lineNum + 1];
	}
	lines.length = lines.length - 1; // reduce lines array size by one
} // removeLine()

// removes lines to this cell
function removeLines(cellId) {
	"use strict";
	var lineNum, line;
	for (lineNum = lines.length - 1; lineNum >= 0; lineNum -= 1) {
		line = lines[lineNum];
		if ([line.cellA.id, line.cellB.id].contains(cellId)) {
			removeLine(lineNum);
		} // if this line connects to this cell
	} // for each line
	if (startCell !== undefined && cellId == startCell.id) {
		noCellSelected = true;
	}
} // removeLines()

function setEDConstants(constants) {
	"use strict";
	DIAGRAM_TAG = constants[0];
	IS_OED_TAG = constants[1];
	CELL_TAG = constants[2];
	LINE_TAG = constants[3];
	ROW_TAG = constants[4];
	COLUMN_TAG = constants[5];
	LABEL_TAG = constants[6];
	ENDPT_TAG = constants[7];
} // setEDConstants()

function setLabel(labelStr) {
	"use strict";
	labels.push(labelStr);
} // setLabel()

// translate coordinates to left side, vertical top, offset by fraction of
// width and height of the Position pos.
function translateLocalPos(pos, xscaleOffset, yscaleOffset) {
	"use strict";
	var canvasPos = Position.get(jsGraphics.cnv);
	pos.left = pos.left - canvasPos.left +
			Math.round(pos.width * xscaleOffset);
	pos.top = pos.top - canvasPos.top +
		Math.round(pos.height * yscaleOffset) - 1;
	return pos;
} // translateLocalPos()

function findCellBoundingRect(cellId, xscaleOffset, yscaleOffset) {
	"use strict";
	var childNum, nodePos,
		HTML_TAG_ELEMENT = 1,
		cell = document.getElementById(cellId),
		pos = Position.getCenter(cellId),
		left = pos.left,
		top = pos.top,
		right = pos.left,
		bottom = pos.top,
		childNode;
		// tagName; // written but not rea.  Raphael 8/2015
	for (childNum = 0; childNum < cell.childNodes.length; childNum += 1) {
		// tagName = ''; // assigned but never used?  Raphael 8/2015
		childNode = cell.childNodes[childNum];
		if (childNode.nodeType == HTML_TAG_ELEMENT) {
			nodePos = Position.get(childNode);
			if (nodePos.left < left) {
				left = nodePos.left;
			} // update left
			if (nodePos.top < top) {
				top = nodePos.top;
			} // update top
			if ((nodePos.left + nodePos.width) > right) {
				right = nodePos.left + nodePos.width;
			} // update right
			if ((nodePos.top + nodePos.height) > bottom) {
				bottom = nodePos.top + nodePos.height;
			} // update bottom
		} //if span or select node
	} // for each child element of the table cell
	pos.left = left;
	pos.top = top;
	pos.width = right - left;
	pos.height = bottom - top;
	return translateLocalPos(pos, xscaleOffset, yscaleOffset);
} // findCellBoundingRect()

// Draw rectangle around contents of table cell with cellId
function rectHighlight(cellId) {
	"use strict";
	var rectPos = findCellBoundingRect(cellId, 0, 0);
	// Draw rectangle around selected cell
	jsGraphics.drawRect(rectPos.left, rectPos.top, rectPos.width,
			rectPos.height);
} // rectHighlight()

function updateCanvas() {
	"use strict";
	var lineNum, line, startPos, endPos, cellAPos, cellBPos;
	jsGraphics.clear(); // clear canvas to draw shapes
	if (!noCellSelected) { // a line has been started but not finished
		rectHighlight(startCell.id);
	}
	for (lineNum = 0; lineNum < lines.length; lineNum += 1) {
		line = lines[lineNum];
		cellAPos = Position.get(line.cellA);
		cellBPos = Position.get(line.cellB);
		// Adjust position of a line depending on relation between cells
		if (cellAPos.left < cellBPos.left) {
			startPos = findCellBoundingRect(line.cellA.id, 1, 0.5);
			endPos = findCellBoundingRect(line.cellB.id, 0, 0.5);
		}
		if (cellBPos.left < cellAPos.left) {
			startPos = findCellBoundingRect(line.cellA.id, 0, 0.5);
			endPos = findCellBoundingRect(line.cellB.id, 1, 0.5);
		}
		// Draw line
		jsGraphics.drawLine(startPos.left, startPos.top, endPos.left,
				endPos.top);
	}
	jsGraphics.paint(); // call once for all shapes
} // updateCanvas()

function clearSelected() {
	"use strict";
	if (!noCellSelected) {
		var row = getCellRow(startCell.id),
			col = getCellCol(startCell.id);
		clearMe(row, col);
		updateCanvas();
	}
} // clearSelected()

// creates a line between different cells
function makeLine(cellId) {
	"use strict";
	var cell = document.getElementById(cellId),
		// canvasPos = // not used?  Raphael 8/2015
		// 	Position.get(document.getElementById('orbitalsTableCanvas')),
		lineIndex;
	if (noCellSelected) {
		startCell = cell;
		noCellSelected = false;
		updateCanvas();
	} else { // noCellSelected is false and we are completing a line
		endCell = cell;
		// If both cells are same cell
		if (startCell.id == endCell.id) {
			noCellSelected = true;
			updateCanvas();
			return;
		}
		// If both cells are in the same column
		if (startCell.cellIndex == endCell.cellIndex) {
			startCell = endCell;
			noCellSelected = false;
			updateCanvas();
			return;
		}
		// If the cells are not in adjacent columns
		if (Math.abs(startCell.cellIndex - endCell.cellIndex) !== 1) {
			startCell = endCell;
			noCellSelected = false;
			updateCanvas();
			return;
		}
		// Remove line if it connects these two cells
		lineIndex = lineExists(startCell, endCell);
		if (lineIndex > -1) {
			removeLine(lineIndex);
			updateCanvas();
			noCellSelected = true;
			return;
		}
		noCellSelected = true;
		// Add new line to lines array
		lines[lines.length] = new Line(startCell, endCell);
		updateCanvas();
	}
} // makeLine()

/* ********** XML methods ***********/

function getDiagramNode(xmlDoc, isOED) {
	"use strict";
	var diagramNode = getFirstNode(xmlDoc, DIAGRAM_TAG);
	var attrs = [];
	attrs.push([IS_OED_TAG, isOED]);
	return setAttributes(diagramNode, attrs);
} // getDiagramNode()

function addEndPointNodes(xmlDoc, lineNode, cell) {
	"use strict";
	var attrs = [];
	attrs.push([ROW_TAG, getCellRow(cell.id)]);
	attrs.push([COLUMN_TAG, getCellCol(cell.id)]);
	addNewNode(xmlDoc, lineNode, ENDPT_TAG, attrs);
} // addEndPointNodes()

function addLineNodes(xmlDoc, diagramNode) {
	"use strict";
	var lineNum, line, lineNode;
	if (lines.length > 0) {
		for (lineNum = 0; lineNum < lines.length; lineNum += 1) {
			lineNode = addNewNode(xmlDoc, diagramNode, LINE_TAG);
			line = lines[lineNum];
			addEndPointNodes(xmlDoc, lineNode, line.cellA);
			addEndPointNodes(xmlDoc, lineNode, line.cellB);
		} // for each line connecting a cell
	} // if there are lines connecting cells
} // addLineNodes()

// --> end HTML comment
