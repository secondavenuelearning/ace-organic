// <!-- avoid parsing the following as HTML
/* Contains code for correcting for offsets of cursor position based on position
of canvas, scrollbar on screen. Also contains vector math methods.  */

/*jsl:option explicit*/
/*jsl:import drawOnFig.js*/
/*jsl:import jslib.js*/

// Rectangles, ellipses, and circles are defined by four numbers: XY
// coordinates of a point, a width, and a height.  Marks are defined only
// by their XY coordinates.

var offsets = { // globals used here and in clients of this file
	X: undefined,
	Y: undefined,
	WIDTH: undefined,
	HEIGHT: undefined,
	X1: undefined,
	Y1: undefined,
	X2: undefined,
	Y2: undefined,
	canvasOffsets: [0, 0], // accounts for position of browser window on screen
	prevCanvasOffsets: [0, 0], // previous position of browser window on screen
	scrollOffsets: [0, 0], // accounts for position of scrolling of div
	textOffsets: [0, -5] // corrects the coordinate at which text is displayed
	    // so it is vertically centered around the mark position.
	// divOffsets: [0, 0] // accounts for position of div containing image inside window
};

// In arrows and lines, the width and height are replaced by the XY coordinates
// of a second point.


// Gets values of image position and window scroll position for modifying
// mouse click coordinates obtained from browser.
// We need to run this method anew for each click in case window is resized.
function recalculateOffsets() {
	"use strict";
	var scrollDivName = document.getElementById('scrollableDiv'),
		scrollDiv;
	if (scrollDivName) {
		scrollDiv = document.getElementById(scrollDivName.value);
		offsets.scrollOffsets = [scrollDiv.scrollLeft, scrollDiv.scrollTop];
		// offsets.divOffsets = [scrollDiv.offsetLeft, scrollDiv.offsetTop];
	} // if the clickable image is embedded in a scrollable DIV
	if (whichBrowser() !== 'Safari') {
		offsets.canvasOffsets[offsets.X] += offsets.scrollOffsets[offsets.X];
		offsets.canvasOffsets[offsets.Y] += offsets.scrollOffsets[offsets.Y];
	} // if whichBrowser
} // recalculateOffsets()

// Corrects a coordinate for the position of the image in the window.
function canvasSetXY(coord, X_or_Y) {
	"use strict";
	return coord + offsets.canvasOffsets[X_or_Y];
} // canvasSetXY()

// Corrects a coordinate for the position of the image in the window.
function canvasSetX(coord) {
	"use strict";
	return canvasSetXY(coord, offsets.X);
} // canvasSetX()

// Corrects a coordinate for the position of the image in the window.
function canvasSetY(coord) {
	"use strict";
	return canvasSetXY(coord, offsets.Y);
} // canvasSetY()

// Corrects coordinates for the position of the image in the window.  Modifies
// the original array AND returns it!
function canvasSet(coords) {
	"use strict";
	coords[offsets.X] = canvasSetX(coords[offsets.X]);
	coords[offsets.Y] = canvasSetY(coords[offsets.Y]);
	return coords;
} // canvasSet()

// Corrects a coordinate if the canvas offsets have changed.
function canvasResetXY(coord, X_or_Y) {
	"use strict";
	return canvasSetXY(coord - offsets.prevCanvasOffsets[X_or_Y], X_or_Y);
} // canvasResetXY()

// Corrects a coordinate if the canvas offsets have changed.
function canvasResetX(coord) {
	"use strict";
	return canvasResetXY(coord, offsets.X);
} // canvasResetX()

// Corrects a coordinate if the canvas offsets have changed.
function canvasResetY(coord) {
	"use strict";
	return canvasResetXY(coord, offsets.Y);
} // canvasResetY()

// Corrects coordinates if the canvas offsets have changed.  Modifies the
// original array AND returns it!
function canvasReset(coords) {
	"use strict";
	coords[offsets.X] = canvasResetX(coords[offsets.X]);
	coords[offsets.Y] = canvasResetY(coords[offsets.Y]);
	return coords;
} // canvasReset()

// Corrects a coordinate so it no longer depends on the position of the image in
// the window.
function canvasUnsetXY(coord, X_or_Y) {
	"use strict";
	return coord - offsets.canvasOffsets[X_or_Y];
} // canvasUnsetXY()

// Corrects a coordinate so it no longer depends on the position of the image in
// the window.
function canvasUnsetX(coord) {
	"use strict";
	return canvasUnsetXY(coord, offsets.X);
} // canvasUnsetX()

// Corrects a coordinate so it no longer depends on the position of the image in
// the window.
function canvasUnsetY(coord) {
	"use strict";
	return canvasUnsetXY(coord, offsets.Y);
} // canvasUnsetY()

// Corrects coordinates so they no longer depend on the position of the image in
// the window.  Modifies the original array AND returns it!
function canvasUnset(coords) {
	"use strict";
	coords[offsets.X] = canvasUnsetX(coords[offsets.X]);
	coords[offsets.Y] = canvasUnsetY(coords[offsets.Y]);
	return coords;
} // canvasUnset()

// Corrects the coordinate at which text is displayed so it is vertically
// centered around the mark position.
function offsetTextXY(coord, X_or_Y) {
	"use strict";
	return coord + offsets.textOffsets[X_or_Y];
} // offsetTextXY()

// Corrects the coordinate at which text is displayed so it is vertically
// centered around the mark position.
function offsetTextX(coord) {
	"use strict";
	return offsetTextXY(coord, offsets.X);
} // offsetTextX()

// Corrects the coordinate at which text is displayed so it is vertically
// centered around the mark position.
function offsetTextY(coord) {
	"use strict";
	return offsetTextXY(coord, offsets.Y);
} // offsetTextY()

// Corrects the coordinates at which text is displayed so it is vertically
// centered around the mark position.  Modifies the original array AND returns
// it!
function offsetText(coords) {
	"use strict";
	coords[offsets.X] = offsetTextX(coords[offsets.X]);
	coords[offsets.Y] = offsetTextY(coords[offsets.Y]);
	return coords;
} // offsetText()

// Corrects the coordinate at which a select box is displayed so that it
// surrounds the text more or less symmetrically.
function offsetSelectTextXY(coord, X_or_Y) {
	"use strict";
	var markSelectBoxOffsets = [2, 7];
	return offsetTextXY(coord, X_or_Y) + markSelectBoxOffsets[X_or_Y];
} // offsetSelectTextXY()

// Corrects the coordinate at which a select box is displayed so that it
// surrounds the text more or less symmetrically.
function offsetSelectTextX(coord) {
	"use strict";
	return offsetSelectTextXY(coord, offsets.X);
} // offsetSelectTextX()

// Corrects the coordinate at which a select box is displayed so that it
// surrounds the text more or less symmetrically.
function offsetSelectTextY(coord) {
	"use strict";
	return offsetSelectTextXY(coord, offsets.Y);
} // offsetSelectTextY()

// Corrects the coordinates at which a select box is displayed so that it
// surrounds the text more or less symmetrically.  Modifies the original array
// AND returns it!
function offsetSelectText(coords) {
	"use strict";
	coords[offsets.X] = offsetSelectTextX(coords[offsets.X]);
	coords[offsets.Y] = offsetSelectTextY(coords[offsets.Y]);
	return coords;
} // offsetSelectText()

// Corrects the current pointer coordinates for the position of the window
// scroll and to place the mark at the tip of the pointer.  Modifies the
// original array AND returns it!
function correctPointerCoords(coords, fourCoords) {
	"use strict";
	var pointerOffsets = [-5, -3];
	if (fourCoords) {
		coords[offsets.X1] += offsets.scrollOffsets[offsets.X];
		coords[offsets.Y1] += offsets.scrollOffsets[offsets.Y];
		coords[offsets.X2] += offsets.scrollOffsets[offsets.X];
		coords[offsets.Y2] += offsets.scrollOffsets[offsets.Y];
	} else {
		coords[offsets.X] += offsets.scrollOffsets[offsets.X];
		coords[offsets.Y] += offsets.scrollOffsets[offsets.Y];
	} // if should correct all four coordinates or first two
	coords[offsets.X] += pointerOffsets[offsets.X];
	coords[offsets.Y] += pointerOffsets[offsets.Y];
	return coords;
} // correctPointerCoords()

/* ******************* Vector and other math *****************/

// Sets all the values of an array to the given value.
function fill(arr, value) {
	"use strict";
	var index;
	for (index = 0; index < arr.length; index += 1) {
		arr[index] = value;
	}
} // fill()

function zero(arr) {
	"use strict";
	return fill(arr, 0);
}
function round(num) {
	"use strict";
	return Math.round(num);
}
function floor(num) {
	"use strict";
	return Math.floor(num);
}
function roundEvenIfHalf(num) {
	"use strict";
	var floored;
	if ((num * 2) % 2 === 1) {
		floored = floor(num);
		return (floored % 2 === 1 ? Math.ceil(num) : floored);
	}
	return round(num);
} // roundEvenIfHalf()

function getAverage(int1, int2) {
	"use strict";
	return round((int1 + int2) / 2);
}

// Gets AB, the vector from A to B, B - A.
function getVector(ptA, ptB) {
	"use strict";
	return [ptB[offsets.X] - ptA[offsets.X], ptB[offsets.Y] - ptA[offsets.Y]];
} // getVector()

// Gets the midpoint between two points.
function getMidpoint(ptA, ptB) {
	"use strict";
	return [getAverage(ptA[offsets.X], ptB[offsets.X]), getAverage(ptA[offsets.Y], ptB[offsets.Y])];
} // getMidpoint()

// Computes the dot product AB·AC.
function dotProd(ptA, ptB, ptC) {
	"use strict";
	var AB = getVector(ptA, ptB),
		BC = getVector(ptB, ptC);
	return AB[offsets.X] * BC[offsets.X] + AB[offsets.Y] * BC[offsets.Y];
} // dotProd()

// Computes the cross product AB x AC.
function crossProd(ptA, ptB, ptC) {
	"use strict";
	var AB = getVector(ptA, ptB),
		AC = getVector(ptA, ptC);
	return AB[offsets.X] * AC[offsets.Y] - AB[offsets.Y] * AC[offsets.X];
} // crossProd()

// Computes the angle of a vector relative to the XY axes (-pi to pi).
function getAngle(vec) {
	"use strict";
	return (vec[offsets.X] !== 0 && vec[offsets.Y] !== 0 ?
			Math.atan(-vec[offsets.Y] / vec[offsets.X]) +
				(vec[offsets.X] < 0 ? -signum(vec[offsets.Y]) * Math.PI : 0) :
			vec[offsets.X] === 0 ? -signum(vec[offsets.Y]) * Math.PI / 2 :
					vec[offsets.X] < 0 ? Math.PI :
							0);
} // getAngle()

// Gets the difference of two angles, normalizing so the result is between -pi
// and pi.
function anglesDiff(angle2, angle1) {
	"use strict";
	var diff = angle2 - angle1;
	if (diff <= -Math.PI) {
		diff += Math.PI * 2;
	} else if (diff > Math.PI) {
		diff -= Math.PI * 2;
	}
	return diff;
} // anglesDiff

// Gets whether a point is between two other points on the line joining them.
function isBetween(endPtA, endPtB, ptC) {
	"use strict";
	return dotProd(endPtA, endPtB, ptC) <= 0 &&
			dotProd(endPtB, endPtA, ptC) <= 0;
} // isBetween()

// Computes the length of a vector as a real number.
function getVectorLength(vector) {
	"use strict";
	return Math.sqrt(vector[offsets.X] * vector[offsets.X] +
		vector[offsets.Y] * vector[offsets.Y]);
} // getVectorLength()

// Computes the distance from A to B as an integer.
function getDistance(ptA, ptB) {
	"use strict";
	return round(getVectorLength(getVector(ptA, ptB)));
} // getDistance()

// Computes the distance from a line segment AB to point C as an integer.
function getDistanceSegmentToPoint(endPtA, endPtB, ptC) {
	"use strict";
	return (dotProd(endPtA, endPtB, ptC) > 0 ? getDistance(endPtB, ptC) :
			dotProd(endPtB, endPtA, ptC) > 0 ? getDistance(endPtA, ptC) :
					Math.abs(crossProd(endPtA, endPtB, ptC) /
						getDistance(endPtA, endPtB)));
} // getDistanceSegmentToPoint()

// Multiplies every member of an array by a factor.
function scalarProd(arr, factor) {
	"use strict";
	var prod = [],
		membNum;
	for (membNum = 0; membNum < arr.length; membNum += 1) {
		prod.push(arr[membNum] * factor);
	} // for each member of the first array
	return prod;
} // scalarProd()

// Converts radians to degrees as an integer.
function toDegrees(radians) {
	"use strict";
	return round(radians * 180 / Math.PI);
} // toDegrees()

// Converts degrees to radians.
function toRadians(degrees) {
	"use strict";
	return degrees * Math.PI / 180;
} // toRadians()

// Returns the array derived from summing the elements of two arrays.
function arraysSum(arr1, arr2) {
	"use strict";
	var sum = [],
		membNum;
	for (membNum = 0; membNum < arr1.length; membNum += 1) {
		if (membNum < arr2.length) {
			sum.push(arr1[membNum] + arr2[membNum]);
		}
	} // for each member of the first array
	return sum;
} // arraysSum()

// Returns an array whose members are rounded from the initial array.
function arrayRound(arr) {
	"use strict";
	var rounded = [],
		membNum;
	for (membNum = 0; membNum < arr.length; membNum += 1) {
		rounded.push(round(arr[membNum]));
	} // for each member of the first array
	return rounded;
} // arrayRound()

// Returns the array derived from substracting the elements of the second
// array from the corresponding elements of the first.
function arraysDiff(arr1, arr2) {
	"use strict";
	return arraysSum(arr1, scalarProd(arr2, -1));
} // arraysDiff()

// Gets an array of a certain length filled with the given value.
function getArray(length, value) {
	"use strict";
	var arr = [],
		val = (!value ? 0 : value),
		index;
	for (index = 0; index < length; index += 1) {
		arr.push(val);
	}
	return arr;
} // getArray()

// Gets whether two arrays are equal in length and have equal members at each
// position.
function arraysMatch(arr1, arr2) {
	"use strict";
	var membNum;
	if (!arr1 && !arr2) {
		return true;
	}
	if (!arr1 || !arr2 || arr1.length !== arr2.length) {
		return false;
	}
	for (membNum = 0; membNum < arr1.length; membNum += 1) {
		if (arr1[membNum] !== arr2[membNum]) {
			return false;
		}
	} // for each member
	return true;
} // arraysMatch()

// --> end HTML comment
