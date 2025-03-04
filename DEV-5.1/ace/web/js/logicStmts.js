// <!-- avoid parsing the following as HTML
/** Collects a response consisting of a series of statements in order.
 * Requires that the including page contain
 * an element with ID 'stmtsTable' whose inner HTML can be replaced.
 * Including page must also include wordCheck.js.
 */

/*jsl:option explicit*/
/*jsl:import jslib.js*/
/*jsl:import wordCheck.js*/
/*jsl:import openwindows.js*/
/*jsl:import xmlLib.js*/

var stmtsCt = 0;
var ADD_STMT = 0;
var REMOVE_STMT = 1;
var THEREFORE = 2;
var BLANK_STMT = 3;
var BLANK_SUBMIT = 4;
var UNKNOWN_SUBMIT = 5;
var SEE_WORDS = 6;
var texts = [
	'Add statement',
	'Remove last statement',
	'Therefore',
	'Please enter a statement before adding another line.',
	'Please do not submit any blank statements.',
	'Please do not submit any statements that contain ' +
			'unknown words (those struck through and in red).',
	'See acceptable words.'
];
var pathToRoot = '';

/* Finishes the table showing the logical sequence of statements. */
function finishStmtOut(stmtOut) {
	"use strict";
	stmtOut.append('<tr><td colspan="2" class="regtext small">' +
				'<table style="width:100%;"><tr><td>' +
				'<a href="javascript:addStmt();">').
			append(texts[ADD_STMT]).
			append('<\/a><\/td>');
	if (stmtsCt > 1) {
		stmtOut.append('<td id="removeStmt" class="regtext small" ' +
					'style="padding-left:20px; text-align:right;">' +
					'<a href="javascript:removeStmt();">').
				append(texts[REMOVE_STMT]).
				append('<\/a><\/td>');
	}
	stmtOut.append('<td id="seeWords" class="regtext small" ' +
				'style="padding-left:20px; text-align:right;">' +
				'<a href="javascript:openAcceptableWords();">').
			append(texts[SEE_WORDS]).append('<\/a><\/td>').
			append('<\/tr><\/table><\/td><\/tr><\/table>');
} // finishStmtOut()

/** Converts the entered statements to English logical statements. */
function getParagraph() {
	"use strict";
	var stmtNum, stmt, checked, last,
		stmtsBld = new String.builder();
	for (stmtNum = 1; stmtNum <= stmtsCt; stmtNum += 1) {
		stmt = trimWhiteSpaces(getValue('stmt' + stmtNum));
		if (isWhiteSpace(stmt)) {
			toAlert(texts[BLANK_SUBMIT]);
			return '';
		}
		checked = checkText(stmt);
		if (checked.indexOf('unknownWord') >= 0) {
			toAlert(texts[UNKNOWN_SUBMIT]);
			return '';
		}
		if (stmtNum > 1) {
			stmtsBld.append('; ').append(texts[THEREFORE]).append(', ');
		}
		last = stmt.length - 1;
		stmtsBld.append(stmt.charAt(last) == '.' && stmtNum < stmtsCt ?
				stmt.substring(1, last) : stmt);
		if (stmtNum == stmtsCt && stmt.charAt(last) != '.') {
			stmtsBld.append('.');
		}
	} // for each statement
	return stmtsBld.toString();
} // getParagraph()

/** Converts the entered statements to XML. */
function getStmtsXML() {
	"use strict";
	var stmtNum, stmt, checked,
		stmtsBld = new String.builder();
	stmtsBld.append('<table>');
	for (stmtNum = 1; stmtNum <= stmtsCt; stmtNum += 1) {
		stmt = getValue('stmt' + stmtNum);
		if (isWhiteSpace(stmt)) {
			toAlert(texts[BLANK_SUBMIT]);
			return '';
		}
		checked = checkText(stmt);
		if (checked.indexOf('unknownWord') >= 0) {
			toAlert(texts[UNKNOWN_SUBMIT]);
			return '';
		}
		stmtsBld.append('<tr><td>').append(toValidXML(stmt)).
				append('<\/td><\/tr>');
	} // for each statement
	stmtsBld.append('<\/table>');
	return stmtsBld.toString();
} // getStmtsXML()

/* Starts writing the table of statements. */
function initializeStmtOut() {
	"use strict";
	var stmtOut = new String.builder();
	stmtOut.append('<table style="width:100%; margin-left:auto; margin-right:auto;">');
	return stmtOut;
} // initializeStmtOut()

/* Trims and removes final punctuation from an entered phrase before
 * redisplaying it on the next line.
 */
function modifyStmt(stmt) {
	"use strict";
	var stmtMod = trim(stmt),
		len = stmtMod.length,
		lastChar;
	if (len === 0) {
		return stmtMod;
	}
	lastChar = stmtMod.substring(len - 1);
	if ('.,:;'.indexOf(lastChar) >= 0 &&
			(lastChar != ';' ||
				(len >= 7 && stmtMod.substring(len - 7, len).indexOf('&') < 0) ||
				(len < 7 && stmtMod.indexOf('&') < 0))) {
		stmtMod = stmtMod.substring(0, len - 1);
	} // if ends with punctuation
	return stmtMod;
} // modifyStmt()

/* Writes a statement to the table. */
function addToStmtOut(stmtOut, stmt, stmtNum) {
	"use strict";
	// var parts = stmt.split('***'); // unused?  Raphael 7/2015
	stmtOut.append('<tr><td class="regtext">');
	if (stmtNum > 1) {
		stmtOut.append(texts[THEREFORE]);
	}
	stmtOut.append('<input type="text" name="stmt').
			append(stmtNum).append('" id="stmt').append(stmtNum).
			append('" size="30" value="').append(modifyStmt(stmt)).
			append('" onkeyup="updateText(').append(stmtNum).
			append(');"\/>');
	if (stmtNum < stmtsCt) {
		stmtOut.append(';');
	}
	stmtOut.append('<\/td><td id="mirror').append(stmtNum).
			append('" style="padding-left:40px;">').
			append(checkText(stmt)).append('<\/td><\/tr>');
} // addToStmtOut()

/* Adds a line for entering a new statement. */
function addStmt() {
	"use strict";
	var stmtNum, stmtOut, stmt,
		newStmt = trim(getValue('stmt' + stmtsCt));
	if (newStmt === '') {
		toAlert(texts[BLANK_STMT]);
		return;
	} // if newStmt
	stmtOut = initializeStmtOut();
	stmtsCt += 1;
	for (stmtNum = 1; stmtNum < stmtsCt; stmtNum += 1) {
		stmt = trim(getValue('stmt' + stmtNum));
		addToStmtOut(stmtOut, stmt, stmtNum);
	} // for each current statement
	addToStmtOut(stmtOut, '', stmtsCt);
	finishStmtOut(stmtOut);
	setInnerHTML('stmtsTable', stmtOut.toString());
} // addStmt()

function openAcceptableWords() {
	"use strict";
	var url = pathToRoot + 'homework/acceptableWords.html';
	openAcceptableWordsWindow(url);
} // openAcceptableWords()

/* Removes the last statement from the table. */
function removeStmt() {
	"use strict";
	var stmtNum, stmtOut, stmt;
	if (!document.getElementById('stmtsTable')) {
		// return if the statements table is not available
		// (for some unfathomable reason, this function is being called early)
		return;
	}
	stmtOut = initializeStmtOut();
	stmtsCt -= 1;
	for (stmtNum = 1; stmtNum <= stmtsCt; stmtNum += 1) {
		stmt = trim(getValue('stmt' + stmtNum));
		addToStmtOut(stmtOut, stmt, stmtNum);
	} // for each up to the penultimate statement
	finishStmtOut(stmtOut);
	setInnerHTML('stmtsTable', stmtOut.toString());
} // removeStmt()

/* Initializes the table showing the logical sequence of statements. */
function setStmts(stmts, translatedTexts, addlWordsStr, pathToRt) {
	"use strict";
	var txtNum, stmtNum, stmtOut;
	pathToRoot = pathToRt;
	wordsInit(addlWordsStr);
	if (translatedTexts) {
		for (txtNum = 0; txtNum < texts.length; txtNum += 1) {
			if (txtNum < translatedTexts.length) {
				texts[txtNum] = translatedTexts[txtNum];
			} else {
				break;
			}
		} // for each value in texts
	} // if there are translated texts
	if (stmts.length === 0) {
		stmts.push('');
	}
	stmtsCt = stmts.length;
	stmtOut = initializeStmtOut();
	for (stmtNum = 0; stmtNum < stmtsCt; stmtNum += 1) {
		addToStmtOut(stmtOut, stmts[stmtNum], stmtNum + 1);
	} // for each current statement
	finishStmtOut(stmtOut);
	setInnerHTML('stmtsTable', stmtOut.toString());
} // setStmts()

// --> end HTML comment
