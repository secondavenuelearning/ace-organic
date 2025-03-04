// <!-- avoid parsing the following as HTML
/* Methods for drawing an editable reaction coordinate diagram and encoding
the data in XML.  See oedAndRcd.js for more details.
*/

/*jsl:option explicit*/
/*jsl:import jslib.js*/
/*jsl:import oedAndRcd.js*/

var rcdPhrases;
var REFRESH_BUTTON = 0;
var DELETE_BUTTON = 1;
var DROP_HERE = 2;
var CLICK_RCD = 3;

function setRCDConstants(phrases) {
	"use strict";
	rcdPhrases = phrases;
} // setRCDConstants()

// writes the text and buttons under the diagram; stores most recently selected
// label
function writeTextAndButtons() {
	"use strict";
	var bld = new String.builder().
			append('<table summary=""><tr>' +
				'<td colspan="2" class="regtext" ' +
				'style="border-left-style:none; border-right-style:none;">').
			append(rcdPhrases[CLICK_RCD]).
			append('<input type="hidden" id="label" value="0" />' +
				'<\/td><\/tr><tr><td ' +
				'style="border-left-style:none; border-right-style:none;">').
			append(rcdPhrases[REFRESH_BUTTON]).
			append('<\/td><td style="width:100%; text-align:left; ' +
				'border-left-style:none; border-right-style:none;">').
			append(rcdPhrases[DELETE_BUTTON]).
			append('<\/td><\/tr><\/table>');
	setInnerHTML('textAndButtons', bld.toString());
} // writeTextAndButtons()

function saveRCDSelectors(cellId) {
	"use strict";
	// set selectors to this cell's value
	var labelId = 'label_' + cellId;
	if (document.getElementById(labelId)) {
		setValue('label', getValue(labelId));
	} // if the cell is occupied already
} // saveRCDSelectors()

function moveHere(row, col) {
	"use strict";
	var cellId = 'r' + row + 'c' + col,
		cell = document.getElementById(cellId);
	saveRCDSelectors(startCell.id);
	// Move cell
	dropNew(row, col);
	moveLines(startCell, cell);
	clearSelected();
} // moveHere()

// gets the selected state and label and drops them into a cell of the table
function dropNew(row, col) {
	"use strict";
	var label = parseInt(getValue('label'), 10);
	drop(row, col, label);
} // dropNew()

// drops a state and label into a cell of the table
function drop(row, col, label) {
	"use strict";
	var cellId = 'r' + row + 'c' + col,
		where = document.getElementById(cellId),
		bld = new String.builder().
			append('<span class="regtext" onclick="javascript:makeLine(\'').
			append(cellId).
			append('\');" onmouseover="javascript:rectHighlight(\'').
			append(cellId).
			append('\'); jsGraphics.paint();" ' +
				'onmouseout="javascript:updateCanvas();">' +
				'<b><font size="+1">&bull;<\/font><\/b><\/span>').
			append(getLabelSelector('label_' + cellId, label)).
			append('<span class="regtext" onclick="javascript:makeLine(\'').
			append(cellId).
			append('\');" onmouseover="javascript:rectHighlight(\'').
			append(cellId).append('\'); jsGraphics.paint();" ' +
				'onmouseout="javascript:updateCanvas();">' +
				'<font size="+1"><b>&bull;<\/b><\/font><\/span>');
	setValue('label', '0');
	where.innerHTML = bld.toString();
	updateCanvas();
} // drop()

// clears a table cell of a state
function clearMe(row, col) {
	"use strict";
	var bld,
		cellId = 'r' + row + 'c' + col,
		cell = document.getElementById(cellId);
	saveRCDSelectors();
	// clear out cell
	bld = new String.builder().
			append('<span onmouseover="darkenCell(\'').append(cellId).
			append('\');" onmouseout="lightenCell(\'').append(cellId).
			append('\');" onclick="javascript:dropMove(').
			append(row).append(', ').append(col).append(');">').
			append(rcdPhrases[DROP_HERE]).append('<\/span>');
	cell.style.color = 'white';
	cell.innerHTML = bld.toString();
	removeLines(cellId);
} // clearMe()

// returns a string in XML format describing the reaction coordinate diagram
function encodeRCD(numRows, numCols) {
	"use strict";
	var rNum, cNum, cellId, labelId, attrs = [];
	var xmlDoc = getParentXMLDoc(DIAGRAM_TAG);
	var diagramNode = getDiagramNode(xmlDoc, false);
	for (cNum = 1; cNum <= numCols; cNum += 1) {
		for (rNum = numRows; rNum > 0; rNum -= 1) {
			cellId = 'r' + rNum + 'c' + cNum;
			labelId = 'label_' + cellId;
			if (document.getElementById(labelId)) {
				attrs = [];
				attrs.push([ROW_TAG, rNum]);
				attrs.push([COLUMN_TAG, cNum]);
				attrs.push([LABEL_TAG, getValue(labelId)]);
				addNewNode(xmlDoc, diagramNode, CELL_TAG, attrs);
			} // if the cell is occupied
		} // for each row
	} // for each column
	addLineNodes(xmlDoc, diagramNode);
	return getXML(xmlDoc);
} // encodeRCD()

// --> end HTML comment
