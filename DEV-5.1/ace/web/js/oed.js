// <!-- avoid parsing the following as HTML
/* Methods for drawing an editable orbital energy diagram and encoding
the data in XML.  See oedAndRcd.js for more details.
*/

/*jsl:option explicit*/
/*jsl:import oedAndRcd.js*/
/*jsl:import jslib.js*/

var oedStrConstants = [];
var OCCUP_SEP = 0;
var ORBS_TYPE_TAG = 1;
var OCCUPS_TAG = 2;
var OCCUP_TAG = 3;

var oedPhrases = [];
var REFRESH_BUTTON = 0;
var DELETE_BUTTON = 1;
var DROP_HERE = 2;
var CLICK_OED = 3;
var ADD = 4;
var ORBS_OF_TYPE = 5;

var labelOrbs = true;

// forward definitions
var getTypeSelector, drop, clearSelected;

function setOEDConstants(strConstants, phrases, labelOrbsVal) {
	"use strict";
	oedStrConstants = strConstants;
	oedPhrases = phrases;
	labelOrbs = labelOrbsVal;
} // setOEDConstants()

var orbPopupNames = [];
function setOrbPopupName(displayName) {
	"use strict";
	orbPopupNames.push(displayName);
} // setOrbPopupName()

// writes the orbital type and number selectors, text and buttons under the
// diagram
function writeOrbSelectors() {
	"use strict";
	var orbNum,
		bld = new String.builder().
			append('<table><tr><td class="regtext" style="width:50%;">').
			append(oedPhrases[ADD]).
			append('<select id="numOrbs" onchange="resetOccupancies();">');
	for (orbNum = 1; orbNum <= 7; orbNum += 1) {  // arbitrary limit
		bld.append('<option value="').append(orbNum).
				append('">').append(orbNum).append('<\/option>');
	}
	bld.append('<\/select>').append(oedPhrases[ORBS_OF_TYPE]).
			append(getTypeSelector('orbType', '1', !IN_DIAGRAM)).
			append('<input type="hidden" id="occupancies" value="0" \/>');
	if (labelOrbs) {
		bld.append('<input type="hidden" id="label" value="0" \/>');
	}
	bld.append('<\/td><td rowspan="2" style="padding-top:5px; ' +
				'text-align:left;">').
			append(oedPhrases[CLICK_OED]).
			append('<\/td><\/tr><tr><td>' +
				'<table style="border-left:0px;"><tr><td>').
			append(oedPhrases[REFRESH_BUTTON]).
			append('<\/td><td style="text-align:left; width:100%;">').
			append(oedPhrases[DELETE_BUTTON]).
			append('<\/td><\/tr><\/table><\/td><\/tr><\/table>');
	setInnerHTML('textAndButtons', bld.toString());
} // writeOrbSelectors()

// creates a pulldown menu for setting the type of orbital
function getTypeSelector(selectorId, value, inDiagram) {
	"use strict";
	var orbType, cellId,
		bld = new String.builder();
	if (inDiagram) {
		cellId = selectorId.substring('orbType_'.length, selectorId.length);
		bld.append('<span class="regtext" onclick="javascript:makeLine(\'').
				append(cellId).
				append('\');" onmouseover="javascript:rectHighlight(\'').
				append(cellId).
				append('\'); jsGraphics.paint();" ' +
					'onmouseout="javascript:updateCanvas();">' +
					'&nbsp;<font size="+1"><b>&bull;<\/b><\/font><\/span>');
	}
	bld.append('<select id="').append(selectorId).append('"');
	if (!inDiagram) {
		bld.append(' onchange="resetOccupancies();"');
	}
	bld.append('>');
	for (orbType = 0; orbType < orbPopupNames.length; orbType += 1) {
		bld.append('<option value="').append(orbType + 1).append('"');
		if (parseInt(value, 10) == orbType + 1) {
			bld.append(SELECTED);
		}
		bld.append('>').append(orbPopupNames[orbType]).append('<\/option>');
	} // for each type of orbital
	bld.append('<\/select>');
	if (inDiagram) {
		bld.append(':');
	}
	return bld.toString();
} // getTypeSelector()

function resetOccupancies() {
	"use strict";
	var orbNum,
		numOrbs = parseInt(getValue('numOrbs'), 10),
		occupBld = new String.builder().append('0');
	for (orbNum = 1; orbNum < numOrbs; orbNum += 1) {
		occupBld.append(oedStrConstants[OCCUP_SEP] + '0');
	}
	setValue('occupancies', occupBld.toString());
	setValue('label', '0');
} // resetOccupancies()

// creates a pulldown menu for setting orbital occupancy
function getElectronSelector(name, occupancy) {
	"use strict";
	/* alert('getElectronSelector : name = ' + name +
	 		', occupancy = ' + occupancy); */
	var bld = new String.builder().
			append('<select id="').append(name).append('">').
			append('<option value="2"');
	if (occupancy === 2) {
		bld.append(SELECTED);
	}
	bld.append('>&uarr;&darr;<\/option><option value="1"');
	if (occupancy === 1) {
		bld.append(SELECTED);
	}
	bld.append('>&uarr;<\/option><option value="0"');
	if (occupancy === 0) {
		bld.append(SELECTED);
	}
	bld.append('>&mdash;<\/option><\/select>');
	return bld.toString();
} // getElectronSelector()

function saveOrbitalSelectors(cellId) {
	"use strict";
	// set selectors to this cell's value
	var orbType, orbNum, occupBld, orbId, occupancies,
		orbTypeId = 'orbType_' + cellId;
	if (document.getElementById(orbTypeId)) {
		orbType = getValue(orbTypeId);
		setValue('orbType', orbType);
		orbNum = 0;
		occupBld = new String.builder();
		while (true) {
			orbId = 'occupancy_' + cellId + '_' + (orbNum + 1);
			if (document.getElementById(orbId)) {
				orbNum += 1;
				if (orbNum > 1) {
					occupBld.append(oedStrConstants[OCCUP_SEP]);
				}
				occupBld.append(getValue(orbId));
			} else {
				break;
			}
		}
		setValue('numOrbs', String(orbNum));
		occupancies = occupBld.toString();
		setValue('occupancies', occupancies);
		if (document.getElementById('label_' + cellId)) {
			setValue('label', getValue('label_' + cellId));
		} // if there's a label selector
	} // if the cell is occupied already
} // saveOrbitalSelectors()

// gets the selected orbitals and drops them into a cell of the table
function dropNew(row, col) {
	"use strict";
	var type = getValue('orbType'),
		occupancies = getValue('occupancies'),
		label = (labelOrbs ? getValue('label') : '0');
	drop(row, col, type, occupancies, label, labelOrbs);
} // dropNew()

// move startCell to cell at row, col
function moveHere(row, col) {
	"use strict";
	var cellId = 'r' + row + 'c' + col,
		cell = document.getElementById(cellId);
	saveOrbitalSelectors(startCell.id);
	// Move cell
	dropNew(row, col);
	moveLines(startCell, cell);
	clearSelected();
} // moveHere()

// drops orbitals with a type and occupancy into a cell of the table
function drop(row, col, type, occups, label) {
	"use strict";
	var orbNum, cellId, cell, bld, occupArr, occupancy;
	if (isEmpty(type)) {
		return;
	}
	cellId = 'r' + row + 'c' + col;
	cell = document.getElementById(cellId);
	bld = new String.builder().
			append(getTypeSelector('orbType_' + cellId, type, IN_DIAGRAM));
	occupArr = occups.split(oedStrConstants[OCCUP_SEP]);
	/* alert('drop: row = ' + row + ', col = ' + col + ', type = ' + type+
			', occups = ' + occups + ', label = ' + label +
			', occupArr = ' + occupArr); */
	for (orbNum = 0; orbNum < occupArr.length; orbNum += 1) {
		occupancy = parseInt(occupArr[orbNum], 10);
		/* alert('drop: occupArr[orbNum] = ' + occupArr[orbNum]+
				', occupancy = ' + occupancy); */
		bld.append(getElectronSelector(
			'occupancy_' + cellId + '_' + (orbNum + 1),
			occupancy
		));
	}
	if (labelOrbs && col === 2) {
		bld.append(getLabelSelector('label_' + cellId, label));
	}
	bld.append('<span class="regtext" onclick="javascript:makeLine(\'').
			append(cellId).
			append('\');" onmouseover="javascript:rectHighlight(\'').
			append(cellId).
			append('\'); jsGraphics.paint();" ' +
				'onmouseout="javascript:updateCanvas();">' +
				'<font size="+1"><b>&bull;<\/b><\/font><\/span>');
	cell.innerHTML = bld.toString();
	updateCanvas();
} // drop()

// clears a table cell of orbitals
function clearMe(row, col) {
	"use strict";
	var cellId = 'r' + row + 'c' + col,
		cell = document.getElementById(cellId),
		bld;
	// alert('Getting orbital value for row ' + row + ', col ' + col);
	saveOrbitalSelectors(cellId);
	// clear out cell
	bld = new String.builder().
		append('<span onmouseover="darkenCell(\'').append(cellId).
			append('\');" onmouseout="lightenCell(\'').append(cellId).
			append('\');" onclick="javascript:dropMove(').
			append(row).append(', ').append(col).append(');">').
			append(oedPhrases[DROP_HERE]).append('<\/span>');
	cell.style.color = 'white';
	cell.innerHTML = bld.toString();
	removeLines(cellId);
} // clearMe()

// returns a string in XML format describing the orbital energy diagram
function encodeOED(numRows, numCols) {
	"use strict";
	var rNum, cNum, cellId, orbTypeId, attrs, label, cellNode, occupsNode, 
		orbNum, orbId, occup;
	var xmlDoc = getParentXMLDoc(DIAGRAM_TAG);
	var diagramNode = getDiagramNode(xmlDoc, true);
	for (cNum = 1; cNum <= numCols; cNum += 1) {
		for (rNum = numRows; rNum > 0; rNum -= 1) {
			cellId = 'r' + rNum + 'c' + cNum;
			orbTypeId = 'orbType_' + cellId;
			if (document.getElementById(orbTypeId)) {
				// make cell node, add to diagram node
				attrs = [];
				attrs.push([ROW_TAG, rNum]);
				attrs.push([COLUMN_TAG, cNum]);
				label = (document.getElementById('label_' + cellId) ?
						getValue('label_' + cellId) : '0');
				attrs.push([LABEL_TAG, label]);
				cellNode = addNewNode(xmlDoc, diagramNode, CELL_TAG, attrs);
				// make orbital type node, add to cell node
				attrs = [];
				attrs.push([oedStrConstants[ORBS_TYPE_TAG], getValue(orbTypeId)]);
				addNewNode(xmlDoc, cellNode, oedStrConstants[ORBS_TYPE_TAG], attrs);
				// make occupancies node, add to cell node
				occupsNode = addNewNode(xmlDoc, cellNode, 
						oedStrConstants[OCCUPS_TAG]);
				orbNum = 1;
				while (true) {
					orbId = 'occupancy_' + cellId + '_' + orbNum;
					if (document.getElementById(orbId)) {
						occup = getValue(orbId);
						attrs = [];
						attrs.push([oedStrConstants[OCCUP_TAG], occup]);
						addNewNode(xmlDoc, occupsNode, oedStrConstants[OCCUP_TAG], attrs);
						orbNum += 1;
					} else {
						break;
					} // if there is an orbital with this number
				} // while there are more orbitals in the cell
			} // if the cell is occupied
		} // for each row
	} // for each column
	addLineNodes(xmlDoc, diagramNode);
	return getXML(xmlDoc);
} // encodeOED()

// --> end HTML comment
