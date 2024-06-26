// <!-- avoid parsing the following as HTML
/* Methods for drawing shapes on a figure, as in clickable image and
draw-vectors questions. Currently, the code only allows for drawing on one
image on a page.


Calling page must:
		include jslib.js, wz_jsgraphics.js, position.js, offsets.js
		include xmlLib.js if user will be inputting data
		include file drawOnFig.jsp.h in Javascript area
		have the following HTML; the id values must be as written:
			<div id="canvas" style="position:relative; left:0px; top:0px;">
			<img id="clickableImage" class="unselectable"
					src="[filename]" alt="picture"
					onselect="return false;" ondragstart="return false;"/>
			</div>
			<input type="hidden" id="shapeChooser" value="[default value]" />
		have an element whose ID is 'scrollableDiv' *if* clickable image in
			calling page is embedded in a DIV that can be scrolled
			independently from the window (as in
			authortool/evaluators/loadEvaluatorGeneric.jsp.h)
Calling page *may* have element whose ID is 'lastCoords' (mostly for debugging).
Calling page should call the following method upon body onload:

	<% final String[] colorAndMaxMarks =
			ClickImage.getColorAndMaxMarks(Utils.isEmpty(qData) ? null : qData[0].data); %>

	function initDrawOnFigure() {
		var coords = [];
		initDrawOnFigConstants();
		if (<%= isClickableImage %>) {
			setNumShapesLimit(<%= colorAndMaxMarks[ClickImage.NUM_MARKS] %>);
		} // if clickable image
		setClickPurposeMenu();
		initDrawOnFigGraphics('<%= colorAndMaxMarks[ClickImage.COLOR] %>');
		captureClicks();
		<% if (!Utils.isEmpty(lastResp)) {
			final ClickImage clickImage = new ClickImage(lastResp);
			final int[][] allCoords = clickImage.getAllCoords();
			final String[] allMarkStrs = clickImage.getAllMarkStrs();
			int markNum = 0;
			for (final int[] coords : allCoords) { %>
				setMark([<%= coords[ClickImage.X] %>,
						<%= coords[ClickImage.Y] %>],
						'<%= Utils.toValidJS(allMarkStrs[markNum += 1]) %>');
		<%	} // for each mark %>
			paintAll();
		<% } // if there's a last response to display %>
	} // initDrawOnFigure()

The second half of the method is different in
authortool/evaluators/loadEvaluatorGeneric.jsp.h, which uses different shapes,
and for DrawVectors questions.

This file contains (but in define-before-use order):

	IE/MOZ methods
	Methods to execute user actions
	Methods correcting for scroll, div positions
	User actions
	Methods to execute user actions
	Drawing methods
	XML-building methods
	Initialization methods

*/

/*jsl:option explicit*/
/*jsl:import jslib.js*/
/*jsl:import offsets.js*/
/*jsl:import wz_jsgraphics.js*/
/*jsl:import position.js*/
/*jsl:import xmlLib.js*/

/* ******************* Constants & global variables *****************/
// X, Y, X1, Y1, X2, Y2, WIDTH, and HEIGHT are members of the global
// variable "offsets".

var drawonfig = { // globals
	// Constants representing shapes.
		// RECT; ELLIP; CIRC; MARK; ARROW;
	numShapeTypes: 5,
	// XML tags.
	XML_TAG: undefined,
	MARK_TAG: undefined,
	X_TAG: undefined,
	Y_TAG: undefined,
	WIDTH_TAG: undefined,
	HEIGHT_TAG: undefined,
	TEXT_TAG: undefined,
	VECTOR_TAG: undefined,
	ORIGIN_TAG: undefined,
	TARGET_TAG: undefined,
	// Other constants for drawing on figures.
	GHOST: true,
	UNSELECTED: -1,
	TO_DRAW_ALWAYS: true,
	// The coordinates of all shapes and text.
	allShapes: [],
	markTexts: [], // array parallel to marks for storing text
	// Other globals
	canvasImage: undefined, // the image
	isMOZ: undefined, // whether the browser is Mozilla-based or IE
	initPt: 0, // onclick location when drawing circles, rectangles, ellipses
	jsGraphics: undefined, // object that draws lines on the screen
	jsGraphColor: undefined, // color in which to draw lines
	clickPurpose: 'draw', // or select, move, or copy
	selectedShape: -1, // position of a selected mark or arrow within its array
	numShapesLimit: -1, // maximum marks or arrows that a user may draw
	prevLocn: [0, 0], // used to see if mouse has moved
	acceptText: false, // will take typed characters into mark label
	readyToAcceptText: false, // counters mousepad's onmousemove event
		// after onclick event
	textInFocus: false, // indicates whether a textbox is currently in focus
	drawOrModify: ['draw', 'modify'],
	DRAW_NEW: 'Draw new',
	SELECT_EXISTING: 'Select existing',
	CLICK_NEAR: 'Click near a mark to select it.',
	CLICK_NEAR_ARROW: 'Click near a vector to select it, ' +
		'<br/>click and hold near the vector\'s midpoint to move the vector, ' +
		'<br/>shift-click and hold to copy; ' +
		'<br/>click and hold the endpoint of a vector to move the endpoint.',
	// Options after selecting an arrow
	CHOOSE_ACTION: 'Choose an action',
	DELETE_SELECTED: 'Delete',
	UNSELECT: 'Unselect',
	INVERT_ARROW: 'Invert',
	// Buttons while drawing a shape
	CLEAR_LAST: '<input type="button" value="Clear last" ' +
		'onclick="clearLast();" /> ',
	CLEAR_ALL: '<input type="button" value="Clear all" ' +
		'onclick="clearAllOfOne();" /> ',
	// Button while moving or copying an arrow
	CANCEL: '<input type="button" value="Cancel" ' +
		'onclick="unselect();" /> '
}; // globals

(function () { // initialize drawonfig.allShapes
	"use strict";
	var arr;
	for (arr = 0; arr < drawonfig.numShapeTypes; arr += 1) {
		drawonfig.allShapes.push([]);
	} // for each shape
}());


// Corrects the position of the marks and arrows when the canvas offsets change.
function adjustShapePositions() {
	"use strict";
	var shapeSet, shapeType, shapeNum, shape;
	if (offsets.prevCanvasOffsets[offsets.X] === 0 &&
			offsets.prevCanvasOffsets[offsets.Y] === 0) {
		offsets.prevCanvasOffsets = offsets.canvasOffsets;
	} else if (offsets.canvasOffsets[offsets.X] !==
			offsets.prevCanvasOffsets[offsets.X] ||
			offsets.canvasOffsets[offsets.Y] !==
				offsets.prevCanvasOffsets[offsets.Y]) {
		for (shapeType = 0; shapeType < drawonfig.numShapeTypes;
				shapeType += 1) {
			shapeSet = drawonfig.allShapes[shapeType];
			for (shapeNum = 0; shapeNum < shapeSet.length; shapeNum += 1) {
				shape = shapeSet[shapeNum];
				if (shapeType === drawonfig.ARROW) {
					shape[offsets.X1] = canvasResetX(shape[offsets.X1]);
					shape[offsets.Y1] = canvasResetY(shape[offsets.Y1]);
					shape[offsets.X2] = canvasResetX(shape[offsets.X2]);
					shape[offsets.Y2] = canvasResetY(shape[offsets.Y2]);
				} else {
					shape[offsets.X] = canvasResetX(shape[offsets.X]);
					shape[offsets.Y] = canvasResetY(shape[offsets.Y]);
				} // if shapeType
			} // for each set of coordinates defining each shape
		} // for each array of shapes
		offsets.prevCanvasOffsets = offsets.canvasOffsets;
	} // if canvasOffsets have changed
} // adjustShapePositions()

// Turns off all mouse actions.
function disallowDrawing() {
	"use strict";
	if (drawonfig.canvasImage) {
		document.onkeydown = '';
		document.onkeypress = '';
		drawonfig.canvasImage.onclick = '';
		drawonfig.jsGraphics.setOnclick('');
		drawonfig.canvasImage.onmouseup = '';
		drawonfig.jsGraphics.setOnmouseup('');
		drawonfig.canvasImage.onmousemove = '';
		drawonfig.jsGraphics.setOnmousemove('');
		drawonfig.canvasImage.onmousedown = '';
		drawonfig.jsGraphics.setOnmousedown('');
	} // if the image exists
} // disallowDrawing()

// Adds a shape to the drawing buffer. num used only for marks.
function drawShape(shapeType, params, num) {
	"use strict";
	var bld;
	if (shapeType == drawonfig.MARK) {
		bld = new String.builder().
				append('<b>').
				append(!isEmpty(drawonfig.markTexts[num]) ?
					drawonfig.markTexts[num] :
					drawonfig.numShapesLimit === 1 ? '&times;' : num + 1).
				append('<\/b>');
		drawonfig.jsGraphics.drawString(bld.toString(),
			offsetTextX(canvasUnsetX(params[offsets.X])),
			offsetTextY(canvasUnsetY(params[offsets.Y])));
	} else if (shapeType == drawonfig.RECT) {
		drawonfig.jsGraphics.drawRect(canvasUnsetX(params[offsets.X]),
			canvasUnsetY(params[offsets.Y]),
			params[offsets.WIDTH], params[offsets.HEIGHT]);
	} else if (shapeType == drawonfig.ARROW) {
		drawonfig.jsGraphics.drawArrow(canvasUnsetX(params[offsets.X1]),
			canvasUnsetY(params[offsets.Y1]),
			canvasUnsetX(params[offsets.X2]),
			canvasUnsetY(params[offsets.Y2]));
	} else { // drawonfig.CIRC or drawonfig.ELLIP
		drawonfig.jsGraphics.drawEllipse(canvasUnsetX(params[offsets.X]),
			canvasUnsetY(params[offsets.Y]),
			params[offsets.WIDTH], params[offsets.HEIGHT]);
	}
} // drawShape()

// Gets text describing the length and angle of an arrow.
function getArrowText(regionDef, arrowNum) {
	"use strict";
	var ptA = [regionDef[offsets.X1], regionDef[offsets.Y1]],
		ptB = [regionDef[offsets.X2], regionDef[offsets.Y2]],
		length = getDistance(ptA, ptB),
		angle = toDegrees(getAngle(getVector(ptA, ptB))),
		textBld = new String.builder().
				append('<table style="text-align:left;"><tr><td>');
	if (arrowNum > 0) {
		textBld.append('Arrow ').append(arrowNum).append(': ');
	}
	textBld.append(length).append(' px, ');
	if (angle < 0) {
		textBld.append('&minus;').append(-angle);
	} else {
		textBld.append(angle);
	}
	textBld.append('&deg;</td></tr></table>');
	return textBld.toString();
} // getArrowText()

// Gets text describing the lengths and angles of the arrows.
function getArrowsText() {
	"use strict";
	var arrows = drawonfig.allShapes[drawonfig.ARROW],
		numArrows = arrows.length,
		allTextBld = new String.builder(),
		arrowNum;
	for (arrowNum = 0; arrowNum < numArrows; arrowNum += 1) {
		if (arrowNum != drawonfig.selectedShape ||
				 drawonfig.clickPurpose != 'move') {
			allTextBld.append(getArrowText(arrows[arrowNum], arrowNum + 1));
		} // if arrow is not being moved
	} // for each arrow
	return allTextBld.toString();
} // getArrowsText()

// Gets the current shape.  shapeChooser is sometimes hidden, sometimes a
// selector.
function getCurrentShape() {
	"use strict";
	return parseInt(getValue('shapeChooser'), 10);
} // getCurrentShape()

// Corrects the current pointer coordinates for the position of the window
// scroll and to place the mark at the tip of the pointer.
function correctPointer(coords) {
	"use strict";
	return correctPointerCoords(coords, getCurrentShape() === drawonfig.ARROW);
} // correctPointer()

// Adds a blue rectangle around the selected line, arrow, or mark to the
// drawing buffer.
function drawRectangleAround() {
	"use strict";
	var shapes = drawonfig.allShapes[getCurrentShape()],
		shape,
		deg_45,
		angle,
		ptA,
		ptB,
		textSize,
		xVals,
		yVals,
		startPt,
		newAngle,
		newX,
		newY,
		ptNum;
	if (drawonfig.selectedShape == drawonfig.UNSELECTED ||
			drawonfig.selectedShape >= shapes.length) {
		return;
	}
	shape = shapes[drawonfig.selectedShape];
	deg_45 = Math.PI / 4;
	angle = 0;
	if (shape.length === 4) {
		ptA = [shape[offsets.X1], shape[offsets.Y1]];
		ptB = [shape[offsets.X2], shape[offsets.Y2]];
		angle = getAngle(getVector(ptA, ptB));
	} else {
		textSize = getTextSize(drawonfig.markTexts[drawonfig.selectedShape]);
		ptA = [offsetSelectTextX(shape[offsets.X]),
			offsetSelectTextY(shape[offsets.Y])];
		ptB = [offsetSelectTextX(shape[offsets.X]) + textSize,
			offsetSelectTextY(shape[offsets.Y])];
	} // if there are four points defining the shape
	xVals = [];
	yVals = [];
	for (ptNum = 0; ptNum < 4; ptNum += 1) {
		startPt = (ptNum < 2 ? ptA : ptB);
		newAngle = angle;
		if (ptNum >= 2) {
			newAngle += (newAngle <= 0 ? Math.PI : -Math.PI);
		} // if working on ptB
		if (ptNum % 2 === 0) {
			if (newAngle < deg_45) {
				newAngle += 3 * deg_45;
			} else {
				newAngle -= 5 * deg_45;
			}
		} else {
			if (newAngle > -deg_45) {
				newAngle -= 3 * deg_45;
			} else {
				newAngle += 5 * deg_45;
			}
		} // if ptNum is even
		newX = Math.round(startPt[offsets.X] + 10 * Math.cos(newAngle));
		newY = Math.round(startPt[offsets.Y] - 10 * Math.sin(newAngle));
		xVals.push(canvasUnsetX(newX));
		yVals.push(canvasUnsetY(newY));
	} // for each of the four points of the rectangle
	drawonfig.jsGraphics.setColor('blue');
	drawonfig.jsGraphics.drawPolygon(xVals, yVals);
	drawonfig.jsGraphics.setColor(drawonfig.jsGraphColor);
} // drawRectangleAround()

// Gets the origin corrected for the canvas position.
function getOrigin() {
	"use strict";
	var origin = [],
		coordNum;
	for (coordNum = 0; coordNum < 4; coordNum += 1) {
		origin.push(canvasSet(0, coordNum % 2));
	} // for each coordinate
	return origin;
} // getOrigin()

// Gets definitions of a shape based on the most recent mouse coordinates and
// a previous click or shape.
function getRegionDef(currentShape, clickPt) {
	"use strict";
	var regionDef = [],
		origArrow,
		midPt,
		move,
		width,
		height,
		dist;
	if (currentShape == drawonfig.MARK) {
		regionDef = [clickPt[offsets.X], clickPt[offsets.Y]];
	} else if (currentShape == drawonfig.ARROW) {
		if (drawonfig.drawOrModify.contains(drawonfig.clickPurpose)) {
			regionDef = [drawonfig.initPt[offsets.X],
				drawonfig.initPt[offsets.Y],
				clickPt[offsets.X],
				clickPt[offsets.Y]
				];
		} else { // move or copy
			origArrow = drawonfig.allShapes[drawonfig.ARROW][
				drawonfig.selectedShape
			];
			if (origArrow) {
				midPt = getMidpoint([origArrow[offsets.X1], origArrow[offsets.Y1]],
					[origArrow[offsets.X2], origArrow[offsets.Y2]]);
				move = getVector(midPt, clickPt);
				regionDef = [origArrow[offsets.X1] + move[offsets.X],
						origArrow[offsets.Y1] + move[offsets.Y],
						origArrow[offsets.X2] + move[offsets.X],
						origArrow[offsets.Y2] + move[offsets.Y]];
			} // if an arrow was selected
		} // if drawing new arrow
	} else { // not mark or arrow
		width = Math.abs(drawonfig.initPt[offsets.X] - clickPt[offsets.X]);
		height = Math.abs(drawonfig.initPt[offsets.Y] - clickPt[offsets.Y]);
		if (currentShape == drawonfig.RECT) {
			regionDef = [Math.min(clickPt[offsets.X],
				drawonfig.initPt[offsets.X]),
					Math.min(clickPt[offsets.Y], drawonfig.initPt[offsets.Y]), width, height];
		} else if (currentShape == drawonfig.ELLIP) {
			regionDef = [drawonfig.initPt[offsets.X] - width,
				drawonfig.initPt[offsets.Y] - height,
				2 * width, 2 * height];
		} else if (currentShape == drawonfig.CIRC) {
			dist = Math.round(getVectorLength([width, height]));
			regionDef = [drawonfig.initPt[offsets.X] - dist,
				drawonfig.initPt[offsets.Y] - dist,
					2 * dist, 2 * dist];
		} // if currentShape
	} // if currentShape is arrow
	return regionDef;
} // getRegionDef()

// Recalculates offsets and adjusts positions of shapes when window is resized.
// We need to run this method anew for each click in case window is resized.
function getShapeOffsets() {
	"use strict";
	var canvasPos;
	if (drawonfig.canvasImage) {
		canvasPos = Position.get(drawonfig.canvasImage);
		if (whichBrowser() !== 'Internet Explorer') {
			offsets.canvasOffsets = [canvasPos.left, canvasPos.top];
		} // anything other than IE
		adjustShapePositions();
	} // if the image exists
	recalculateOffsets();
} // getShapeOffsets()

// Gets values of image position and window scroll position for modifying
// mouse click coordinates obtained from browser, and sets what method to
// use to acquire those coordinates.
function captureClicks() {
	"use strict";
	getShapeOffsets();
} // captureClicks()

// Turns off acceptance of typed text upon moving a mouse, unless the mouse
// hasn't actually moved.  (A trackpad onclick event is followed immediately by
// an onmousemove event, and shift, option, control, and caps lock can trigger
// onmousemove events, so we have to see if the mouse has actually moved.)
function doOnMouseMove(rawLocn) {
	"use strict";
	var currentLocn;
	getShapeOffsets();
	currentLocn = correctPointer(rawLocn);
	drawonfig.acceptText = drawonfig.readyToAcceptText ||
		(drawonfig.prevLocn[offsets.X] === currentLocn[offsets.X] &&
		drawonfig.prevLocn[offsets.Y] === currentLocn[offsets.Y]);
	drawonfig.prevLocn = currentLocn;
	drawonfig.readyToAcceptText = false;
} // doOnMouseMove()

// Turns off acceptance of typed text upon moving a mouse.
function doOnMouseMoveIE() {
	"use strict";
	doOnMouseMove([event.x, event.y]);
} // doOnMouseMoveIE()

// Turns off acceptance of typed text upon moving a mouse.
function doOnMouseMoveMOZ(e) {
	"use strict";
	doOnMouseMove([e.pageX, e.pageY]);
} // doOnMouseMoveMOZ()

// Gets XML representing the marks.
function getMarksXML() {
	"use strict";
	var marks = drawonfig.allShapes[drawonfig.MARK],
		coords,
		attrs,
		markNum,
		markNode;
	var xmlDoc = getParentXMLDoc(drawonfig.XML_TAG);
	var xmlNode = getFirstNode(xmlDoc, drawonfig.XML_TAG);
	for (markNum = 0; markNum < marks.length; markNum += 1) {
		coords = marks[markNum];
		attrs = [];
		attrs.push([drawonfig.X_TAG, canvasUnsetX(coords[offsets.X])]);
		attrs.push([drawonfig.Y_TAG, canvasUnsetY(coords[offsets.Y])]);
		markNode = addNewNode(xmlDoc, xmlNode, drawonfig.MARK_TAG, attrs);
		markNode.appendChild(getTextNode(xmlDoc, drawonfig.markTexts[markNum]));
	} // for each mark that the student made
	return getXML(xmlDoc);
} // getMarksXML()

// Gets XML representing the vectors.
function getVectorsXML() {
	"use strict";
	var vectors = drawonfig.allShapes[drawonfig.ARROW],
		numVectors = vectors.length,
		vectorNum,
		ptNum,
		coords,
		attrs,
		vectorNode;
	var xmlDoc = getParentXMLDoc(drawonfig.XML_TAG);
	var xmlNode = getFirstNode(xmlDoc, drawonfig.XML_TAG);
	if (numVectors === 0) {
		vectors.push(getOrigin());
	}
	for (vectorNum = 0; vectorNum < vectors.length; vectorNum += 1) {
		coords = vectors[vectorNum];
		vectorNode = addNewNode(xmlDoc, xmlNode, drawonfig.VECTOR_TAG);
		for (ptNum = 0; ptNum < 2; ptNum += 1) {
			attrs = [];
			attrs.push([drawonfig.X_TAG,
				canvasUnsetX(coords[ptNum * 2 + offsets.X])]
				);
			attrs.push([drawonfig.Y_TAG,
				canvasUnsetY(coords[ptNum * 2 + offsets.Y])]
				);
			addNewNode(xmlDoc, vectorNode, ptNum === 0 ?
					drawonfig.ORIGIN_TAG : drawonfig.TARGET_TAG, attrs);
		} // for each point
	} // for each vector that the student drew
	if (numVectors === 0) {
		vectors.pop();
	}
	return getXML(xmlDoc);
} // getVectorsXML()

// Gets XML representing the region-enclosing shapes.
function getShapesXML(shapeTags, text) {
	"use strict";
	var shapeSet,
		shapeType,
		shapeNum,
		coordsDims,
		attrs,
		textNode;
	var xmlDoc = getParentXMLDoc(drawonfig.XML_TAG);
	var xmlNode = getFirstNode(xmlDoc, drawonfig.XML_TAG);
	for (shapeType = 0; shapeType < shapeTags.length; shapeType += 1) {
		shapeSet = drawonfig.allShapes[shapeType];
		for (shapeNum = 0; shapeNum < shapeSet.length; shapeNum += 1) {
			coordsDims = shapeSet[shapeNum];
			attrs = [];
			attrs.push([drawonfig.X_TAG,
				canvasUnsetX(coordsDims[offsets.X])]
				);
			attrs.push([drawonfig.Y_TAG,
				canvasUnsetY(coordsDims[offsets.Y])]
				);
			attrs.push([drawonfig.WIDTH_TAG, coordsDims[offsets.WIDTH]]);
			attrs.push([drawonfig.HEIGHT_TAG, coordsDims[offsets.HEIGHT]]);
			addNewNode(xmlDoc, xmlNode, shapeTags[shapeType], attrs);
		} // for each set of coordinates and dimensions defining each shape
	} // for each shapeType
	if (!isEmpty(text)) {
		textNode = addNewNode(xmlDoc, xmlNode, drawonfig.TEXT_TAG);
		textNode.appendChild(getTextNode(xmlDoc, text));
	} // if there's text
	return getXML(xmlDoc);
} // getShapesXML()

// Initiates values for buttons.
function initDrawOnFigButtons(myCLEAR_LAST, myCLEAR_ALL, myCANCEL) {
	"use strict";
	drawonfig.CLEAR_LAST = myCLEAR_LAST;
	drawonfig.CLEAR_ALL = myCLEAR_ALL;
	drawonfig.CANCEL = myCANCEL;
} // initDrawOnFigButtons()

// Initiates the constants.  Called by initDrawOnFigConstants() in
// drawOnFig.jsp.h.
function initDrawOnFigConstants2(newX, newY, newWIDTH, newHEIGHT,
		newRECT, newELLIP, newCIRC, newMARK, newARROW, newXML_TAG,
		newMARK_TAG, newX_TAG, newY_TAG, newWIDTH_TAG, newHEIGHT_TAG,
		newTEXT_TAG, newVECTOR_TAG, newORIGIN_TAG, newTARGET_TAG) {
	"use strict";
	offsets.X = newX;
	offsets.Y = newY;
	offsets.WIDTH = newWIDTH;
	offsets.HEIGHT = newHEIGHT;
	offsets.X1 = offsets.X;
	offsets.Y1 = offsets.Y;
	offsets.X2 = offsets.WIDTH;
	offsets.Y2 = offsets.HEIGHT;
	offsets.textOffsets = [0, -5];
	drawonfig.RECT = newRECT;
	drawonfig.ELLIP = newELLIP;
	drawonfig.CIRC = newCIRC;
	drawonfig.MARK = newMARK;
	drawonfig.ARROW = newARROW;
	drawonfig.XML_TAG = newXML_TAG;
	drawonfig.MARK_TAG = newMARK_TAG;
	drawonfig.X_TAG = newX_TAG;
	drawonfig.Y_TAG = newY_TAG;
	drawonfig.WIDTH_TAG = newWIDTH_TAG;
	drawonfig.HEIGHT_TAG = newHEIGHT_TAG;
	drawonfig.TEXT_TAG = newTEXT_TAG;
	drawonfig.VECTOR_TAG = newVECTOR_TAG;
	drawonfig.ORIGIN_TAG = newORIGIN_TAG;
	drawonfig.TARGET_TAG = newTARGET_TAG;
} // initDrawOnFigConstants2()

// Initiates non-English phrases other than buttons.
function initDrawOnFigPhrases(myCHOOSE_ACTION, myDELETE_SELECTED,
		myUNSELECT, myINVERT_ARROW, myCLICK_NEAR, myCLICK_NEAR_ARROW,
		myDRAW_NEW, mySELECT_EXISTING) {
	"use strict";
	drawonfig.CHOOSE_ACTION = myCHOOSE_ACTION;
	drawonfig.DELETE_SELECTED = myDELETE_SELECTED;
	drawonfig.UNSELECT = myUNSELECT;
	drawonfig.INVERT_ARROW = myINVERT_ARROW;
	drawonfig.CLICK_NEAR = myCLICK_NEAR;
	drawonfig.CLICK_NEAR_ARROW = myCLICK_NEAR_ARROW;
	drawonfig.DRAW_NEW = myDRAW_NEW;
	drawonfig.SELECT_EXISTING = mySELECT_EXISTING;
} // initDrawOnFigPhrases()

// Adds the stored shapes to the drawing buffer.
function preparePermanentShapes() {
	"use strict";
	var shapeType, shapeSet, shapeNum;
	for (shapeType = 0; shapeType < drawonfig.numShapeTypes; shapeType += 1) {
		shapeSet = drawonfig.allShapes[shapeType];
		for (shapeNum = 0; shapeNum < shapeSet.length; shapeNum += 1) {
			if (shapeType != drawonfig.ARROW ||
					shapeNum != drawonfig.selectedShape ||
					drawonfig.clickPurpose != 'move') {
				drawShape(shapeType, shapeSet[shapeNum], shapeNum);
			} // if should draw shapeType
		} // for each set of coordinates defining each shape
	} // for each array of shapes
} // preparePermanentShapes()

// Causes the drawing object to draw all of the stored shapes.
function paintAll() {
	"use strict";
	drawonfig.jsGraphics.clear();
	preparePermanentShapes();
	drawonfig.jsGraphics.drawString(getArrowsText(), 5, 5);
	if (drawonfig.selectedShape != drawonfig.UNSELECTED) {
		drawRectangleAround();
	} // if an arrow has been selected
	drawonfig.jsGraphics.paint();
} // paintAll()

// Clears all shapes.
function clearAll() {
	"use strict";
	var shapeType;
	for (shapeType = 0; shapeType < drawonfig.numShapeTypes; shapeType += 1) {
		drawonfig.allShapes[shapeType] = [];
	} // for each shapeType
	drawonfig.markTexts = [];
	drawonfig.initPt = 0;
	paintAll();
} // clearAll()

// Clears all of one kind of shape.
function clearAllOfOne() {
	"use strict";
	var currentShape = getCurrentShape();
	drawonfig.allShapes[currentShape] = [];
	if (currentShape === drawonfig.MARK) {
		drawonfig.markTexts = [];
	}
	drawonfig.initPt = 0;
	paintAll();
} // clearAllOfOne()

// Clears the last shape.
function clearLast() {
	"use strict";
	var currentShape = getCurrentShape();
	drawonfig.allShapes[currentShape].pop();
	if (currentShape === drawonfig.MARK) {
		drawonfig.markTexts.pop();
	}
	drawonfig.initPt = 0;
	paintAll();
} // clearLast()

// Converts arrow A to -A.
function invertArrow() {
	"use strict";
	var currentShape = getCurrentShape(),
		shapes = drawonfig.allShapes[currentShape],
		arrowCoords = shapes[drawonfig.selectedShape];
	shapes[drawonfig.selectedShape] =
		[arrowCoords[offsets.X2], arrowCoords[offsets.Y2],
			arrowCoords[offsets.X1], arrowCoords[offsets.Y1]];
	paintAll();
} // invertArrow()

// Turns on drawing of ghost shapes on mouse move.
var addGhostShapeMOZ, addGhostShapeIE; // used here, defined below
function setAddGhostShape() {
	"use strict";
	drawonfig.canvasImage.onmousemove =
		(drawonfig.isMOZ ? addGhostShapeMOZ : addGhostShapeIE);
	drawonfig.jsGraphics.setOnmousemove('addGhostShape' +
			(drawonfig.isMOZ ? 'MOZ(event);' : 'IE();'));
} // setAddGhostShape()

// Causes the value of textInFocus to change depending on whether an input element
// is in focus.
function setFocusFunctions(input) {
	"use strict";
	input.onfocus = function () { drawonfig.textInFocus = true; };
	input.onblur = function () { drawonfig.textInFocus = false; };
} // setFocusFunctions()

// Causes the value of textInFocus to change depending on whether any textbox on
// the page is in focus.
function captureTextFocus() {
	"use strict";
	var inputs = document.getElementsByTagName('input'),
		inputNum,
		input;
	for (inputNum = 0; inputNum < inputs.length; inputNum += 1) {
		input = inputs[inputNum];
		if (input.type === 'text') {
			setFocusFunctions(input);
		}
	} // for each text input
	inputs = document.getElementsByTagName('textarea');
	for (inputNum = 0; inputNum < inputs.length; inputNum += 1) {
		setFocusFunctions(inputs[inputNum]);
	} // for each textarea
} // captureTextFocus()

// Sets the color and nature of the lines being drawn.
function setGraphicsParams(ghost) {
	"use strict";
	if (ghost) {
		drawonfig.jsGraphics.setColor('#606060');
		drawonfig.jsGraphics.setStroke(Stroke.DOTTED);
	} else {
		drawonfig.jsGraphics.setColor(drawonfig.jsGraphColor);
		drawonfig.jsGraphics.setStroke(2);
	} // if ghost
} // setGraphicsParams()

// Stores a mark.  Called by homework/answerframe.jsp, gradebook/showMol.jsp,
// and resetConfirm.jsp.
function setMark(coords, markText) {
	"use strict";
	drawonfig.allShapes[drawonfig.MARK].push([canvasSetX(coords[offsets.X]),
		canvasSetY(coords[offsets.Y])]);
	drawonfig.markTexts.push(markText);
} // setMark()

// Sets the maximum number of shapes that can be drawn.
function setNumShapesLimit(limit) {
	"use strict";
	drawonfig.numShapesLimit = limit;
} // setNumShapesLimit()

// Turns off drawing of ghost shapes on mouse move.
function unsetAddGhostShape() {
	"use strict";
	drawonfig.canvasImage.onmousemove = '';
	drawonfig.jsGraphics.setOnmousemove('');
} // unsetAddGhostShape()


// Sets the mouse actions, depending on whether the user is drawing,
// selecting, moving, or copying.
// the following are functions defined later that we need to refer to here.
var getDeleteMOZ, getDeleteIE, getTextMOZ, getTextIE,
	doOnMouseMoveMOZ, doOnMouseMoveIE, selectNearestShapeMOZ, selectNearestShapeIE,
	selectHoldNearestShapeMOZ, selectHoldNearestShapeIE,
	releaseShapeMOZ, releaseShapeIE, addPermanentShapeMOZ, addPermanentShapeIE,
	startShapeMOZ, startShapeIE, addPermanentShapeMOZ, addPermanentShapeIE;
function setMouseActions() {
	"use strict";
	if (drawonfig.canvasImage) {
		var suffix = (drawonfig.isMOZ ? 'MOZ(event);' : 'IE();'),
			mouseMoveAction =
				(drawonfig.isMOZ ? doOnMouseMoveMOZ : doOnMouseMoveIE),
			mouseMoveActionStr;
		mouseMoveActionStr = 'doOnMouseMove' + suffix;
		document.onkeydown = (drawonfig.isMOZ ? getDeleteMOZ : getDeleteIE);
		document.onkeypress = (drawonfig.isMOZ ? getTextMOZ : getTextIE);
		if (drawonfig.clickPurpose === 'select') {
			if (getCurrentShape() === drawonfig.MARK) {
				drawonfig.canvasImage.onclick =
					(drawonfig.isMOZ ? selectNearestShapeMOZ :
							selectNearestShapeIE);
				drawonfig.jsGraphics.setOnclick('selectNearestShape' + suffix);
				drawonfig.canvasImage.onmousemove = mouseMoveAction;
				drawonfig.jsGraphics.setOnmousemove(mouseMoveActionStr);
			} else {
				drawonfig.canvasImage.onclick = '';
				drawonfig.jsGraphics.setOnclick('');
				drawonfig.canvasImage.onmousedown =
					(drawonfig.isMOZ ? selectHoldNearestShapeMOZ :
							selectHoldNearestShapeIE);
				unsetAddGhostShape();
				drawonfig.jsGraphics.setOnmousedown('selectHoldNearestShape' + suffix);
				drawonfig.canvasImage.onmouseup =
					(drawonfig.isMOZ ? releaseShapeMOZ : releaseShapeIE);
				drawonfig.jsGraphics.setOnmouseup('releaseShape' + suffix);
			} // if not a mark
			setGraphicsParams(!drawonfig.GHOST);
		} else { // draw, modify, move, copy
			drawonfig.selectedShape = drawonfig.UNSELECTED;
			if (getCurrentShape() === drawonfig.MARK) {
				drawonfig.canvasImage.onclick =
					(drawonfig.isMOZ ? addPermanentShapeMOZ :
							addPermanentShapeIE);
				drawonfig.jsGraphics.setOnclick('addPermanentShape' + suffix);
				drawonfig.canvasImage.onmousemove = mouseMoveAction;
				drawonfig.jsGraphics.setOnmousemove(mouseMoveActionStr);
			} else {
				drawonfig.canvasImage.onclick = '';
				drawonfig.jsGraphics.setOnclick('');
				drawonfig.canvasImage.onmousedown =
					(drawonfig.isMOZ ? startShapeMOZ : startShapeIE);
				drawonfig.jsGraphics.setOnmousedown('startShape' + suffix);
				setAddGhostShape();
				drawonfig.canvasImage.onmouseup =
					(drawonfig.isMOZ ? addPermanentShapeMOZ :
							addPermanentShapeIE);
				drawonfig.jsGraphics.setOnmouseup('addPermanentShape' + suffix);
			}
			setGraphicsParams(drawonfig.clickPurpose !== 'draw');
		} // if drawonfig.clickPurpose
	} // if the image exists
} // setMouseActions()

// Sets the options and mouse actions when drawing shapes or selecting an arrow.
function setClickOptions() {
	"use strict";
	var bld;
	showCell('clickActions1');
	if (drawonfig.clickPurpose === 'draw') {
		bld = new String.builder().
				append('<table><tr><td>').
				append(drawonfig.CLEAR_LAST).append('<\/td>');
		if (drawonfig.numShapesLimit !== 1) {
			bld.append('<td>').append(drawonfig.CLEAR_ALL).append('<\/td>');
		} // if there's more than one shape permitted
		bld.append('<\/tr><\/table>');
		setInnerHTML('clickActions1', bld.toString());
	} else {
		setInnerHTML('clickActions1', getCurrentShape() === drawonfig.ARROW ?
				drawonfig.CLICK_NEAR_ARROW : drawonfig.CLICK_NEAR);
	}
	setInnerHTML('clickActions2', '');
	setMouseActions();
} // setClickOptions()

// Triggered by the select menu to toggle between drawing shapes and
// copying, moving, and deleting shapes.
function changeClickPurpose() {
	"use strict";
	drawonfig.clickPurpose = getValue('clickPurpose');
	setClickOptions();
	// paintAll();
} // changeClickPurpose()

// Initiates the drawing object, sets the button options and the mouse actions.
function initDrawOnFigGraphics(color) {
	"use strict";
	drawonfig.canvasImage = document.getElementById('clickableImage');
	drawonfig.isMOZ = !document.all;
	drawonfig.jsGraphColor = color;
	drawonfig.jsGraphics = new JSGraphics('canvas');
	drawonfig.jsGraphics.setFont('Helvetica', '12px', Font.PLAIN);
	captureTextFocus();
	setClickOptions();
} // initDrawOnFigGraphics()

// Converts the purpose of clicking from moving/copying back to the original
// value.
function resetClickPurpose() {
	"use strict";
	drawonfig.selectedShape = drawonfig.UNSELECTED;
	drawonfig.acceptText = false;
	drawonfig.clickPurpose = getValue('clickPurpose');
	setClickOptions();
} // resetClickPurpose()

// Adds a new ghost (temporary) or permanent shape to the canvas; if the
// shape is permament, adds it to the list of permanent shapes, and if the shape
// is a permanent mark, readies the program to accept text.
function addShape(clickPt, ghost) {
	"use strict";
	var currentShape = getCurrentShape(),
		isMark = currentShape == drawonfig.MARK,
		clickToDraw = drawonfig.drawOrModify.contains(drawonfig.clickPurpose),
		regionDef,
		isArrow,
		shapes;
	if ((typeof drawonfig.initPt) != 'number' || // have initial coordinates
			isMark || // marks placed by single click
			!clickToDraw) { // copying or moving a shape
		regionDef = getRegionDef(currentShape, clickPt);
		if (regionDef.length === 0) {
			return;
		}
		isArrow = currentShape == drawonfig.ARROW;
		if (ghost) { // mouse held down and moved, no release yet; never true
			// for drawonfig.MARK
			drawonfig.jsGraphics.clear();
			preparePermanentShapes();
			if (clickToDraw) {
				drawonfig.jsGraphics.drawEllipse(
					canvasUnsetX(drawonfig.initPt[offsets.X]),
					canvasUnsetY(drawonfig.initPt[offsets.Y]),
					1,
					1
				);
			} // if need to draw initial point
			setGraphicsParams(drawonfig.GHOST);
			drawShape(currentShape, regionDef, 0);
			setGraphicsParams(!drawonfig.GHOST);
			if (isArrow) {
				drawonfig.jsGraphics.drawString(getArrowsText() +
						getArrowText(regionDef, 0), 5, 5);
			} // if drawing an arrow
			drawonfig.jsGraphics.paint();
		} else { // mouse release
			shapes = drawonfig.allShapes[currentShape];
			if (isArrow || isMark) {
				if (!clickToDraw) { // move or copy
					if (drawonfig.clickPurpose == 'move') {
						shapes.splice(drawonfig.selectedShape, 1);
					} // if move
					resetClickPurpose();
				} else if (drawonfig.numShapesLimit > 0 &&
						shapes.length >= drawonfig.numShapesLimit) {
					shapes.pop();
					if (isMark) {
						drawonfig.markTexts.pop();
					}
				} // if drawonfig.clickPurpose
			} // if drawonfig.ARROW or drawonfig.MARK
			shapes.push(regionDef); // adds the new shape!
			if (isMark) {
				drawonfig.markTexts.push('');
			}
			drawonfig.acceptText = isMark;
			drawonfig.readyToAcceptText = isMark;
			drawonfig.initPt = 0;
			paintAll();
			if (drawonfig.clickPurpose == 'modify') {
				selectCell('select');
				resetClickPurpose();
			} // if modifying existing arrow
		} // ghost
	} else if (!ghost) { // mousedown for drawing a shape
		drawonfig.initPt = clickPt;
		adjustShapePositions();
		drawonfig.jsGraphics.drawEllipse(
			canvasUnsetX(drawonfig.initPt[offsets.X]),
			canvasUnsetY(drawonfig.initPt[offsets.Y]),
			1,
			1
		);
		drawonfig.jsGraphics.paint();
	} // if ready to draw an object
} // addShape()

// Adds a ghost (temporary) shape to the canvas.
function addGhostShape(rawPt) {
	"use strict";
	getShapeOffsets();
	addShape(correctPointer(rawPt), drawonfig.GHOST);
} // addGhostShape()

// Adds a ghost (temporary) shape to the canvas.
function addGhostShapeIE() {
	"use strict";
	addGhostShape([event.x, event.y]);
} // addGhostShapeIE()

// Adds a ghost (temporary) shape to the canvas.
function addGhostShapeMOZ(e) {
	"use strict";
	addGhostShape([e.pageX, e.pageY]);
} // addGhostShapeMOZ()

// Adds a new shape to the canvas.
function addPermanentShape(rawPt) {
	"use strict";
	getShapeOffsets();
	addShape(correctPointer(rawPt), !drawonfig.GHOST);
} // addPermanentShape()

// Adds a new shape to the canvas.
function addPermanentShapeIE() {
	"use strict";
	addPermanentShape([event.x, event.y]);
} // addPermanentShapeIE()

// Adds a new shape to the canvas.
function addPermanentShapeMOZ(e) {
	"use strict";
	addPermanentShape([e.pageX, e.pageY]);
} // addPermanentShapeMOZ()

// Deletes the selected shape.
function deleteSelected(toDrawAlways) {
	"use strict";
	var currentShape = getCurrentShape(),
		shapes = drawonfig.allShapes[currentShape];
	shapes.splice(drawonfig.selectedShape, 1);
	if (currentShape === drawonfig.MARK) {
		drawonfig.markTexts.splice(drawonfig.selectedShape, 1);
	}
	drawonfig.selectedShape = drawonfig.UNSELECTED;
	paintAll();
	if (toDrawAlways || shapes.length === 0) {
		selectCell('draw');
	} // if there are no more of the current shape
	resetClickPurpose();
} // deleteSelected()

// Gets typed text or delete.
function getTyped(code) {
	"use strict";
	var markNum = (drawonfig.selectedShape != drawonfig.UNSELECTED ?
			drawonfig.selectedShape :
			drawonfig.allShapes[drawonfig.MARK].length - 1),
		noMark = isEmpty(drawonfig.markTexts[markNum]),
		len,
		keyChar;
	if ([8, 127].contains(code)) {
		if (noMark && drawonfig.selectedShape != drawonfig.UNSELECTED) {
			deleteSelected(!drawonfig.TO_DRAW_ALWAYS);
		} else if (!noMark) {
			len = drawonfig.markTexts[markNum].length;
			drawonfig.markTexts[markNum] =
				drawonfig.markTexts[markNum].substring(0, len - 1);
		} // if there's a mark to delete
	} else {
		// var keyChar = keyCodeToXMLChar(code);
		keyChar = String.fromCharCode(code);
		drawonfig.markTexts[markNum] =
			(noMark ? keyChar : drawonfig.markTexts[markNum] + keyChar);
	} // if deleting
	paintAll();
} // getTyped()

// If delete has been typed, removes last character from text or deletes
// selected shape.  Return value of false when delete key has been pressed
// is to prevent backspace from acting as back button in Internet Explorer.
function getDelete(ev) {
	"use strict";
	var code = ev.charCode || ev.keyCode;
	if (!drawonfig.textInFocus) {
		if (code === 8) {
			if (drawonfig.acceptText) {
				getTyped(code);
			} else if (drawonfig.selectedShape != drawonfig.UNSELECTED) {
				deleteSelected(!drawonfig.TO_DRAW_ALWAYS);
			} // if not accepting text but shape is selected
		} // if delete key has been pressed
		return code !== 8;
	} // if a textbox is not in focus; if one is, delete is treated as usual
} // getDelete()

// Gets typed delete.
function getDeleteIE() {
	"use strict";
	return getDelete(event);
} // getDeleteIE()

// Gets typed delete.
function getDeleteMOZ(e) {
	"use strict";
	return getDelete(e);
} // getDeleteMOZ()

// Gets a typed character.
function getText(ev) {
	"use strict";
	var code = ev.charCode || ev.keyCode;
	if (drawonfig.acceptText) {
		if (code >= 32) {
			getTyped(code);
		}
	} // if accepting text
} // getText()

// Gets a typed character.
function getTextIE() {
	"use strict";
	getText(event);
} // getTextIE()

// Gets a typed character.
function getTextMOZ(e) {
	"use strict";
	getText(e);
} // getTextMOZ()

// Places the shape nearest to where the user clicked.
function releaseShape(rawLocn) {
	"use strict";
	var currentLocn;
	getShapeOffsets();
	if (drawonfig.selectedShape !== drawonfig.UNSELECTED) {
		currentLocn = correctPointer(rawLocn);
		if (currentLocn[offsets.X] !== drawonfig.prevLocn[offsets.X] ||
				currentLocn[offsets.Y] !== drawonfig.prevLocn[offsets.Y]) {
			addShape(currentLocn, !drawonfig.GHOST);
		} // if mouse has moved since mousedown
		unsetAddGhostShape();
	} // if a shape has been selected
	drawonfig.clickPurpose = 'select';
} // releaseShape()

// Selects the shape nearest to where the user clicked.
function releaseShapeIE() {
	"use strict";
	releaseShape([event.x, event.y]);
} // releaseShapeIE()

// Selects the shape nearest to where the user clicked.
function releaseShapeMOZ(e) {
	"use strict";
	releaseShape([e.pageX, e.pageY]);
} // releaseShapeMOZ()

// Sets up the menu to choose between drawing and selecting.
function setClickPurposeMenu() {
	"use strict";
	setInnerHTML('clickPurposeCell', '<select name="clickPurpose" ' +
			'id="clickPurpose" onchange="changeClickPurpose();">' +
			'<option id="draw" value="draw">' + drawonfig.DRAW_NEW +
			'</option>' +
			'<option id="select" value="select">' + drawonfig.SELECT_EXISTING +
			'</option></select>');
} // setClickPurposeMenu()

// Starts a new shape on the canvas when mouse is held down.
function startShapeCorrected(clickPt) {
	"use strict";
	if ((typeof drawonfig.initPt) === 'number') {
		addShape(clickPt, !drawonfig.GHOST);
	} // if shape needs to be initialized
	addShape(clickPt, drawonfig.GHOST);
} // startShapeCorrected()

// Starts a new shape on the canvas when mouse is held down.
function startShape(rawPt) {
	"use strict";
	getShapeOffsets();
	startShapeCorrected(correctPointer(rawPt));
} // startShape()

// Adds a new shape to the canvas.
function startShapeIE() {
	"use strict";
	startShape([event.x, event.y]);
} // startShapeIE()

// Adds a new shape to the canvas.
function startShapeMOZ(e) {
	"use strict";
	startShape([e.pageX, e.pageY]);
} // startShapeMOZ()

// Unselects the selected shape.
function unselect() {
	"use strict";
	resetClickPurpose();
	paintAll();
} // unselect()

// Executes the action chosen after a shape has been selected.
function actOnSelected() {
	"use strict";
	var chosenAction = getValue('selectionOptionsMenu');
	if (chosenAction === 'delete') {
		deleteSelected(!drawonfig.TO_DRAW_ALWAYS);
	} else if (chosenAction === 'invert') {
		invertArrow();
	} else if (chosenAction === 'unselect') {
		unselect();
	}
	setValue('selectionOptionsMenu', '');
} // actOnSelected()

// Selects the mark or vector nearest to where the user clicked, and paints
// all the shapes, including a box around the selected shape.  Also readies
// the program to accept typed text if a mark has been selected.
function selectNearest(pt) {
	"use strict";
	var currentShape = getCurrentShape(),
		nearestDistance = -1,
		nearestShapeNum = drawonfig.UNSELECTED,
		shapes,
		shapeNum,
		shape,
		distance,
		endPtA,
		endPtB,
		textSize,
		bld;
	shapes = drawonfig.allShapes[currentShape];
	for (shapeNum = 0; shapeNum < shapes.length; shapeNum += 1) {
		shape = shapes[shapeNum];
		distance = 0;
		if (currentShape == drawonfig.ARROW) {
			endPtA = [shape[offsets.X1], shape[offsets.Y1]];
			endPtB = [shape[offsets.X2], shape[offsets.Y2]];
			distance = (getDistance(endPtA, endPtB) === 0 ?
					getDistance(endPtA, pt) :
					getDistanceSegmentToPoint(endPtA, endPtB, pt));
		} else { // mark
			endPtA = [shape[offsets.X], shape[offsets.Y]];
			textSize = getTextSize(drawonfig.markTexts[shapeNum]);
			if (textSize === 0) {
				distance = getDistance(endPtA, pt);
			} else {
				endPtB = [shape[offsets.X] + textSize, shape[offsets.Y]];
				distance = getDistanceSegmentToPoint(endPtA, endPtB, pt);
			} // if textSize
		} // if currentShape
		if (nearestDistance == -1 || distance < nearestDistance) {
			nearestDistance = distance;
			nearestShapeNum = shapeNum;
		} // if shape is nearer point than other shapes are
	} // for each shape
	if (nearestDistance < 20 && nearestShapeNum != drawonfig.UNSELECTED) {
		drawonfig.selectedShape = nearestShapeNum;
		bld = new String.builder().
				append('<select id="selectionOptionsMenu" ' +
					'onchange="actOnSelected()">').
				append('<option value="">').
				append(drawonfig.CHOOSE_ACTION).append('</option>').
				append('<option value="delete">').
				append(drawonfig.DELETE_SELECTED).append('</option>');
		if (currentShape == drawonfig.ARROW) {
			bld.append('<option value="invert">').
					append(drawonfig.INVERT_ARROW).append('</option>');
		} // currentShape == drawonfig.ARROW
		bld.append('<option value="unselect">').
				append(drawonfig.UNSELECT).append('</option>').
				append('</select>');
		showCell('clickActions1');
		setInnerHTML('clickActions1', bld.toString());
		setInnerHTML('clickActions2', '');
		drawonfig.acceptText = currentShape == drawonfig.MARK;
		drawonfig.readyToAcceptText = drawonfig.acceptText;
	} else {
		drawonfig.selectedShape = drawonfig.UNSELECTED;
		showCell('clickActions1');
		setInnerHTML('clickActions1', currentShape == drawonfig.ARROW ?
				drawonfig.CLICK_NEAR_ARROW : drawonfig.CLICK_NEAR);
		setInnerHTML('clickActions2', '');
		drawonfig.acceptText = false;
		drawonfig.readyToAcceptText = false;
	} // if there's a nearby shape
	paintAll();
} // selectNearest()

// Selects the shape nearest to where the user clicked.
function selectHoldNearestShape(rawLocn, shiftKey) {
	"use strict";
	var currentShape,
		copyOrMove,
		vector,
		vectorEnd,
		vectorStart,
		allowCopy;
	getShapeOffsets();
	drawonfig.prevLocn = correctPointer(rawLocn);
	selectNearest(drawonfig.prevLocn);
	if (drawonfig.selectedShape !== drawonfig.UNSELECTED) {
		currentShape = getCurrentShape();
		copyOrMove = true;
		if (currentShape === drawonfig.ARROW) {
			vector = drawonfig.allShapes[drawonfig.ARROW][
				drawonfig.selectedShape
			];
			vectorEnd = [vector[offsets.X2], vector[offsets.Y2]];
			if (getDistance(vectorEnd, drawonfig.prevLocn) < 8) {
				vectorStart = [vector[offsets.X1], vector[offsets.Y1]];
				deleteSelected(drawonfig.TO_DRAW_ALWAYS);
				drawonfig.clickPurpose = 'modify';
				startShapeCorrected(vectorStart);
				addShape(vectorEnd, drawonfig.GHOST);
				copyOrMove = false;
			} // if click-hold at vector end
		} // if clicked near an arrow
		if (copyOrMove) {
			allowCopy = drawonfig.numShapesLimit < 0 ||
				drawonfig.allShapes[currentShape].length <
				drawonfig.numShapesLimit;
			drawonfig.clickPurpose = (allowCopy && shiftKey ? 'copy' : 'move');
			setAddGhostShape();
		} // if copy or move shape
	} // if a shape has been selected
} // selectHoldNearestShape()

// Selects the shape nearest to where the user clicked.
function selectHoldNearestShapeIE() {
	"use strict";
	selectHoldNearestShape([event.x, event.y], event.shiftKey);
} // selectHoldNearestShapeIE()

// Selects the shape nearest to where the user clicked.
function selectHoldNearestShapeMOZ(e) {
	"use strict";
	selectHoldNearestShape([e.pageX, e.pageY], e.shiftKey);
} // selectHoldNearestShapeMOZ()

// Selects the shape nearest to where the user clicked.
function selectNearestShape(pt) {
	"use strict";
	getShapeOffsets();
	selectNearest(correctPointer(pt));
} // selectNearestShape()

// Selects the shape nearest to where the user clicked.
function selectNearestShapeIE() {
	"use strict";
	selectNearestShape([event.x, event.y]);
} // selectNearestShapeIE()

// Selects the shape nearest to where the user clicked.
function selectNearestShapeMOZ(e) {
	"use strict";
	selectNearestShape([e.pageX, e.pageY]);
} // selectNearestShapeMOZ()

// --> end HTML comment
