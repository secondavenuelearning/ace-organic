// <!-- avoid parsing the following as HTML
/** JS methods for adding, deleting, editing reaction conditions in a
 * synthesis response or figure.  Requires that the calling page
 * include rxnCondsJava.jsp.h and contain an element called 'reagentTable'
 * whose inner HTML can be replaced with a table.
 * Calling page must include /js/xmlLib.js
 * Calling page must include /js/rxnCondsJava.jsp.h.
 * Calling page should have an onload method that calls
 * 		initRxnConds().
 * 		setRxnName(),
 * 		setChosenRxn(),
 * 		setAllowedRxn(), and
 * 		writeRxnConds().
 * See
 * 		authortool/evaluators/loadEvaluatorChem.jsp,
 * 		authortool/figures/loadFigure.jsp, and
 * 		homework/answerJS.jsp.h
 * for examples.
 */

/*jsl:option explicit*/
/*jsl:import jslib.js*/

var reactionNames = [];
var chosenRxns = []; // reactions are numbered 1 ... numChosenRxns
var allowedRxnConds = [];
var allowedRxnCondsStr;
var numChosenRxns;
var onlyOneRxnCondn;
var synOut;
var alteredRxnConds = false;

var synthJSConstants;
var NO_REAGENTS = 0;
var RXN_ID_SEP = 1;
var RXN_IDS = 2;
var CLICK_HERE = 3;
var INSERT_HERE = 4;
var ADD_1ST = 5;
var RXN_CONDN = 6;
var PATH_TO_CHOOSE_RXN_CONDS_USER = 7;
var REMOVE = 8;
var AFTER_HERE = 9;

function addToOut(rxnNum, id) {
	"use strict";
	synOut.append('<tr><td class="regtext" style="vertical-align:top;">').
			append(synthJSConstants[RXN_CONDN]).append(' ').append(rxnNum).
			append(':<\/td>' +
				'<td class="regtext" style="vertical-align:top;">' +
				'<input type="hidden" id="reaction').
			append(rxnNum).append('Id" value="').append(id).append('"/>' +
				'<a onclick="openReactionWindow(\'').
			append(synthJSConstants[PATH_TO_CHOOSE_RXN_CONDS_USER]).
			append('chooseRxnCondsUser.jsp?rxnNum=').append(rxnNum);
	if (allowedRxnConds.length > 0) {
		synOut.append('&allowedRxnConds=').append(allowedRxnCondsStr);
	}
	synOut.append('\')"><span id="reaction').append(rxnNum).append('Name">').
			append(reactionNames[isEmpty(reactionNames[id]) ? // shouldn't happen
				synthJSConstants[NO_REAGENTS] : id]).
			append('<\/span><\/a><\/td>' +
				'<td class="small" style="padding-left:10px; vertical-align:top;">' +
				'[<a href="javascript:removeReaction(').
			append(rxnNum).append(');">').append(synthJSConstants[REMOVE]).
			append('<\/a>]&nbsp;<\/td>');
	if (synthJSConstants[AFTER_HERE] !== '' && !onlyOneRxnCondn) {
		synOut.append('<td class="small" style="padding-left:10px; vertical-align:top;">' +
					'[<a href="javascript:insertAfterReaction(').
				append(rxnNum).append(');">').
				append(synthJSConstants[AFTER_HERE]).append('<\/a>]<\/td>');
	} // if there's a value for synthJSConstants[AFTER_HERE]
	synOut.append('<\/tr>');
} // addToOut()

function clearChosenRxns() {
	"use strict";
	chosenRxns = [];
} // clearChosenRxns()

function finishOut() {
	"use strict";
	synOut.append('<\/table>');
} // finishOut()

function getRxnIds() {
	"use strict";
	var rxnNum, rxnNumBld, rxnNumId, rxnIds,
		reactionsBld = new String.builder();
	for (rxnNum = 1; rxnNum <= numChosenRxns; rxnNum += 1) {
		if (rxnNum > 1) {
			reactionsBld.append(synthJSConstants[RXN_ID_SEP]);
		}
		rxnNumBld = new String.builder().
				append('reaction').append(rxnNum).append('Id');
		rxnNumId = rxnNumBld.toString();
		reactionsBld.append(getValue(rxnNumId));
	} // for each reaction
	rxnIds = reactionsBld.toString();
	return (rxnIds === '' ? null : rxnIds);
} // getRxnIds()

// Get reaction conditions from MRV until Marvin JS can handle molecule
// properties. NOTE: still doesn't work with Marvin JS because it doesn't import
// molecule properties either.
function getRxnIdsFromMRV(mrv) {
	"use strict";
	var molPropNodeNum, molPropNode, scalarNode,
		rxnIds = '',
		parser = new DOMParser(),
		xmlDoc = parser.parseFromString(mrv.replace(/\\n/g, ''), 'text/xml'),
		molPropNodes = xmlDoc.getElementsByTagName('property');
	if (molPropNodes && molPropNodes.length > 0) {
		// alert('found ' + molPropNodes.length + ' molecule property node(s)');
		for (molPropNodeNum = 0; molPropNodeNum < molPropNodes.length; molPropNodes += 1) {
			molPropNode = molPropNodes[molPropNodeNum];
			if (molPropNode.getAttribute('title') === synthJSConstants[RXN_IDS]) {
				scalarNode = molPropNode.getElementsByTagName('scalar')[0];
				rxnIds = (scalarNode.childNodes &&
								scalarNode.childNodes.length > 0 ?
							scalarNode.childNodes[0].nodeValue :
							scalarNode.getAttribute('value'));
				/* alert('found scalar node with title ' + synthJSConstants[RXN_IDS] +
						' with value ' + rxnIds); */
				break;
			} // if there is a molecule property
		} // for each molecule property node
	} // if there is a molecule property node
	return rxnIds;
} // getRxnIdsFromMRV()

function initializeOut(atLeastOne) {
	"use strict";
	synOut.clear();
	if (atLeastOne) {
		synOut.append('<table><tr><td class="regtext small" colspan="3">').
				append(synthJSConstants[CLICK_HERE]).append('<\/td>');
		if (!isEmpty(synthJSConstants[INSERT_HERE])) {
			synOut.append('<td class="small" style="padding-left:10px;">' +
						'[<a href="javascript:insertAfterReaction(0);">').
					append(synthJSConstants[INSERT_HERE]).
					append('<\/a>]<\/td>');
		} // if there's a value for synthJSConstants[INSERT_HERE]
		synOut.append('<\/tr>');
	} else {
		synOut.append('<table><tr><td class="regtext">');
		if (synthJSConstants[ADD_1ST].indexOf('Any') === 0) {
			synOut.append('Any reaction conditions permissible ' +
					'(<a href="javascript:insertAfterReaction(0);">' +
					'choose a specific one<\/a>)');
		} else {
			synOut.append('[<a href="javascript:insertAfterReaction(0);">').
					append(synthJSConstants[ADD_1ST]).append('<\/a>]');
		} // if synthJSConstants[ADD_1ST] starts with 'Any'
		synOut.append('<\/td><\/tr>');
	}
} // initializeOut()

function insertAfterReaction(insertPoint) {
	"use strict";
	alteredRxnConds = true;
	var ids = [],
		rxnNum,
		rxn,
		synOutStr;
	// store existing ids
	for (rxnNum = 1; rxnNum <= numChosenRxns; rxnNum += 1) {
		rxn = new String.builder().
				append('reaction').append(rxnNum).append('Id');
		ids[rxnNum] = getValue(rxn.toString());
	}
	// make new table up to and including insert point
	initializeOut(true);
	for (rxnNum = 1; rxnNum <= insertPoint; rxnNum += 1) {
		addToOut(rxnNum, ids[rxnNum]);
	}
	// add new reaction to table
	addToOut(insertPoint + 1, synthJSConstants[NO_REAGENTS]);
	// complete new table, incrementing reaction numbers from previously
	for (rxnNum = insertPoint + 1; rxnNum <= numChosenRxns; rxnNum += 1) {
		addToOut(rxnNum + 1, ids[rxnNum]);
	}
	numChosenRxns += 1;
	finishOut();
	synOutStr = synOut.toString();
	// alert(synOutStr);
	setInnerHTML('reagentTable', synOutStr);
} // insertAfterReaction()

// Display reaction conditions already stored in MRV from pasted-in
// structure to user
function displayRxnCondsFromMRV(rxnIds) {
	"use strict";
	var rxnNum, rxnIdsArray, id, prefix;
	if (!rxnIds) {
		return;
	}
	rxnIds = String(rxnIds);
	if (isWhiteSpace(rxnIds)) {
		return;
	}
	rxnIdsArray = rxnIds.split(synthJSConstants[RXN_ID_SEP]);
	for (rxnNum = 0; rxnNum < rxnIdsArray.length; rxnNum += 1) {
		id = rxnIdsArray[rxnNum];
		insertAfterReaction(rxnNum);
		prefix = 'reaction' + (rxnNum + 1);
		setInnerHTML(prefix + 'Name', reactionNames[id]);
		setValue(prefix + 'Id', id);
	} // for each stored reaction
} // displayRxnCondsFromMRV()

function removeReaction(removed) {
	"use strict";
	alteredRxnConds = true;
	var ids = [],
		rxnNum,
		rxn,
		synOutStr;
	// store existing ids
	for (rxnNum = 1; rxnNum <= numChosenRxns; rxnNum += 1) {
		rxn = new String.builder().
				append('reaction').append(rxnNum).append('Id');
		ids[rxnNum] = getValue(rxn.toString());
	}
	// make new table up to removed reaction
	initializeOut(numChosenRxns > 1);
	for (rxnNum = 1; rxnNum < removed; rxnNum += 1) {
		addToOut(rxnNum, ids[rxnNum]);
	}
	// complete new table starting at reaction after removed one;
	// decrease reaction numbers by 1
	for (rxnNum = removed + 1; rxnNum <= numChosenRxns; rxnNum += 1) {
		addToOut(rxnNum - 1, ids[rxnNum]);
	}
	finishOut();
	synOutStr = synOut.toString();
	// alert(synOutStr);
	setInnerHTML('reagentTable', synOutStr);
	numChosenRxns -= 1;
} // removeReaction()

function setChosenRxn(chosenRxnId) {
	"use strict";
	if (chosenRxns.length === 0) {
		chosenRxns.push(0); // make 1-based
	}
	chosenRxns.push(chosenRxnId);
} // setChosenRxn()

function setAllowedRxn(rxnId) {
	"use strict";
	allowedRxnConds.push(rxnId);
	allowedRxnCondsStr = allowedRxnConds.join(synthJSConstants[RXN_ID_SEP]);
} // setAllowedRxn()

function setRxnName(rxnId, rxnName) {
	"use strict";
	reactionNames[rxnId] = rxnName;
} // setRxnNames()

function writeRxnConds(onlyOneRxnCondnVal) {
	"use strict";
	var chosenRxnNum, numRxns, synOutStr;
	onlyOneRxnCondn = onlyOneRxnCondnVal;
	numChosenRxns = (chosenRxns.length === 0 ? 0 : chosenRxns.length - 1);
	initializeOut(numChosenRxns > 0);
	numRxns = (onlyOneRxnCondn && numChosenRxns > 1 ? 1 : numChosenRxns);
	for (chosenRxnNum = 1; chosenRxnNum <= numRxns; chosenRxnNum += 1) {
		addToOut(chosenRxnNum, chosenRxns[chosenRxnNum]);
	} // for each initial reaction condition
	finishOut();
	synOutStr = synOut.toString();
	// alert(synOutStr);
	setInnerHTML('reagentTable', synOutStr);
} // writeRxnConds()

function initRxnConds(constants) {
	"use strict";
	synOut = new String.builder();
	synthJSConstants = constants;
} // initRxnConds()

// Puts the reaction conditions into the MRV as a molecule property.
function modifyMrvWithRxnConditions(mrv, rxnIdsStr) {
	"use strict";
	return modifyMrvProperty(mrv, 'reactionIds', rxnIdsStr);
} // modifyMrvWithRxnConditions()

// --> end HTML comment
