/* Methods for user to write an expression combining rules for combination of
 * starting materials or for combination of evaluators.
 * Parent page must call initiateConstants() and initiateSelectors().
 * Parent page must contain an element called "combinationExpression".
 */

/*jsl:option explicit*/
/*jsl:import jslib.js*/

// <!-- avoid parsing the following as HTML
var OPEN_PAREN;
var CLOSE_PAREN;
var AND;
var OR;
var OF;
var TO;
var BLANK = ' ';
var CONJXN = 'CONJXN';
var NUM_RULES = 'NUM_RULES';
var SELECTED = ' selected="selected"';
var numRules = 0;
var numRuleSels = 0;

function initiateConstants(PAREN_OPEN, PAREN_CLOSE, ET, OU, DE, A) {
	"use strict";
	OPEN_PAREN = PAREN_OPEN;
	CLOSE_PAREN = PAREN_CLOSE;
	AND = ET;
	OR = OU;
	OF = DE;
	TO = A;
} // initiateConstants()

function beginsWithDigit(str) {
	"use strict";
	var char1st = str.charAt(0);
	return char1st >= '0' && char1st <= '9';
} // beginsWithDigit()

function is_OF_or_TO(str) {
	"use strict";
	return [OF, TO].contains(str);
} // is_OF_or_TO()

function appendParen(selValue, parenType, selNum, selsBld) {
	"use strict";
	selsBld.append('<select id="sel').append(selNum).
			append('" name="').append(parenType).
			append('" onchange="javascript:rewriteSelectors(').
			append(selNum).append(');"><option value="').
			append(BLANK).append('"');
	if (selValue == BLANK) {
		selsBld.append(SELECTED);
	}
	selsBld.append('>').append(BLANK).
			append('<\/option><option value="').append(parenType).
			append('"');
	if (selValue == parenType) {
		selsBld.append(SELECTED);
	}
	selsBld.append('>').append(parenType);
	if (parenType == OPEN_PAREN) {
		selsBld.append('<\/option><option value="').
				append(OF).append('"');
		if (selValue == OF) {
			selsBld.append(SELECTED);
		}
		selsBld.append('>').append(selValue == OF ? 'of' : 'n of');
	} // if parenType
	selsBld.append('<\/option><\/select>\n');
} // appendParen()

function appendNumRulesSelector(firstNumRulesSel, selValue, selNum, selsBld) {
	"use strict";
	var num;
	if (!firstNumRulesSel) {
		selsBld.append(' and ');
	}
	selsBld.append('<select id="sel').append(selNum).
			append('" name="').append(NUM_RULES).append('">');
	selNum += 1;
	for (num = 0; num <= numRuleSels; num += 1) {
		selsBld.append('<option value="').append(num).append('"');
		if (selValue == num) {
			selsBld.append(SELECTED);
		}
		selsBld.append('>').append(firstNumRulesSel ? '&ge; ' : '&le; ').
				append(num).append('<\/option>');
	} // for each rule
	selsBld.append('<\/select>\n');
} // appendNumRulesSelector()

function appendConjxn(selValue, selNum, selsBld) {
	"use strict";
	selsBld.append('<br \/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' +
				'<select id="sel').append(selNum).
			append('" name="').append(CONJXN).
			append('" onchange="javascript:rewriteSelectors(').
			append(selNum).append(');"><option value="').
			append(BLANK).append('"');
	if (selValue == BLANK) {
		selsBld.append(SELECTED);
	}
	selsBld.append('>').append(BLANK).
			append('<\/option><option value="').
			append(AND).append('"');
	if (selValue == AND) {
		selsBld.append(SELECTED);
	}
	selsBld.append('>and<\/option><option value="').append(OR).
			append('"');
	if (selValue == OR) {
		selsBld.append(SELECTED);
	}
	selsBld.append('>or<\/option><\/select>\n');
} // appendConjxn()

function appendRuleSelector(selValue, selNum, selsBld) {
	"use strict";
	var ruleNum;
	selsBld.append('<select id="sel').append(selNum).
			append('" name="RULE_NUM">');
	for (ruleNum = 1; ruleNum <= numRules; ruleNum += 1) {
		selsBld.append('<option value="').append(ruleNum).append('"');
		if (selValue == ruleNum) {
			selsBld.append(SELECTED);
		}
		selsBld.append('>Rule ').append(ruleNum).append('<\/option>');
	} // for each rule
	selsBld.append('<\/select>\n');
} // appendRuleSelector()

function initiateSelectors(combParts, numOfRules) {
	"use strict";
	var selsBld = new String.builder(),
		selNum = 0,
		past1stRuleSelector = false,
		END = '.',
		numCombParts = combParts.length,
		combPartNum,
		combPart,
		numRulesSatisfied = 0,
		nextCombPart,
		selsText;
	numRules = numOfRules;
	if (combParts[0] != OPEN_PAREN) {
		appendParen(BLANK, OPEN_PAREN, selNum, selsBld);
		selNum += 1;
	} // if first combPart is not OPEN_PAREN
	for (combPartNum = 0; combPartNum < numCombParts; combPartNum += 1) {
		combPart = combParts[combPartNum];
		if (beginsWithDigit(combPart) &&
				(combPartNum == numCombParts - 1 ||
				!is_OF_or_TO(combParts[combPartNum + 1]))) {
			numRuleSels += 1;
		}
	} // for each combPart
	for (combPartNum = 0; combPartNum < numCombParts; combPartNum += 1) {
		combPart = combParts[combPartNum];
		nextCombPart = (combPartNum == numCombParts - 1 ?
				END : combParts[combPartNum + 1]);
		switch (combPart) {
		case OPEN_PAREN:
			if (nextCombPart != combPart) {
				appendParen(BLANK, combPart, selNum, selsBld);
				selNum += 1;
			} // if the last PAREN of a series
			appendParen(combPart, combPart, selNum, selsBld);
			selNum += 1;
			break;
		case CLOSE_PAREN:
			appendParen(combPart, combPart, selNum, selsBld);
			selNum += 1;
			if (nextCombPart != combPart) {
				appendParen(BLANK, combPart, selNum, selsBld);
				selNum += 1;
			} // if the last PAREN of a series
			break;
		case OF:
			appendNumRulesSelector(false, numRulesSatisfied, selNum,
				selsBld);
			selNum += 1;
			appendParen(combPart, OPEN_PAREN, selNum, selsBld);
			selNum += 1;
			appendParen(BLANK, OPEN_PAREN, selNum, selsBld);
			selNum += 1;
			break;
		case TO:
			appendNumRulesSelector(true, numRulesSatisfied, selNum,
				selsBld);
			selNum += 1;
			break;
		case AND:
		case OR:
			appendConjxn(combPart, selNum, selsBld);
			selNum += 1;
			if (nextCombPart != OPEN_PAREN) {
				appendParen(BLANK, OPEN_PAREN, selNum, selsBld);
				selNum += 1;
			} // if need to insert an open paren
			break;
		default: // rule number or number of following rules to be satisfied
			if (!is_OF_or_TO(nextCombPart)) {
				appendRuleSelector(combPart, selNum, selsBld);
				selNum += 1;
				if (past1stRuleSelector &&
						[AND, OR, END].contains(nextCombPart)) {
					appendParen(BLANK, CLOSE_PAREN, selNum, selsBld);
					selNum += 1;
				} // if 2nd or greater rule selector and no CLOSE_PAREN follows
				past1stRuleSelector = true;
			} else {
				numRulesSatisfied = parseInt(combPart, 10);
			} // nextCombPart is OF
			break;
		} // switch
	} // for each combPart
	appendConjxn(BLANK, selNum, selsBld);
	selsText = selsBld.toString();
	// alert(selsText);
	setInnerHTML('combinationExpression', selsText);
} // initiateSelectors()

function rewriteSelectors(selNumChanged) {
	"use strict";
	var oldSelNum = 0,
		newSelNum = 0,
		selsBld = new String.builder(),
		nextSel,
		nextNotParen,
		selector,
		selType,
		value,
		prevSel,
		prevNotParen,
		prevPrevSel,
		selsText;
	while (true) {
		selector = document.getElementById('sel' + oldSelNum);
		if (!selector) {
			break;
		}
		selType = selector.name;
		value = selector.value;
		/*
		alert('selType = ' + selType+
				', value = "' + value + '"'+
				', oldSelNum = ' + oldSelNum +
				', newSelNum = ' + newSelNum +
				', selNumChanged = ' + selNumChanged
				);
		*/
		switch (selType) {
		case OPEN_PAREN:
		case CLOSE_PAREN:
			prevSel = document.getElementById('sel' + (oldSelNum - 1));
			prevNotParen = !prevSel || prevSel.name != OPEN_PAREN;
			if (value == OF || (prevSel && prevSel.name == NUM_RULES)) {
				if (oldSelNum != selNumChanged) { // was and still is OF
					prevPrevSel = document.getElementById(
						'sel' + (oldSelNum - 2)
					);
					appendNumRulesSelector(true, prevPrevSel.value,
						newSelNum, selsBld);
					newSelNum += 1;
					appendNumRulesSelector(false, prevSel.value,
						newSelNum, selsBld);
					newSelNum += 1;
					appendParen(OF, OPEN_PAREN, newSelNum, selsBld);
					newSelNum += 1;
				} else if (value == OF) { // changed to OF
					if (prevNotParen) {
						appendParen(BLANK, OPEN_PAREN, newSelNum, selsBld);
						newSelNum += 1;
					} // if not preceded by paren
					appendNumRulesSelector(true, 0, newSelNum, selsBld);
					newSelNum += 1;
					appendNumRulesSelector(false, numRuleSels, newSelNum,
						selsBld);
					newSelNum += 1;
					appendParen(OF, OPEN_PAREN, newSelNum, selsBld);
					newSelNum += 1;
					nextSel = document.getElementById('sel' +
						(oldSelNum + 1));
					nextNotParen = !nextSel ||
						nextSel.name != OPEN_PAREN;
					if (nextNotParen) {
						appendParen(BLANK, OPEN_PAREN, newSelNum, selsBld);
						newSelNum += 1;
					} // if not followed by paren
				} else { // was OF, now is not; ignore this selector and skip next
					oldSelNum += 1;
				} // if was or is OF
			} else { // neither was or is OF
				// if changed to a PAREN, add another of same PAREN after it;
				// if changed to BLANK, and there is a PAREN before or after, remove
				if (oldSelNum == selNumChanged && value == OPEN_PAREN) {
					appendParen(BLANK, selType, newSelNum, selsBld);
					newSelNum += 1;
				} // if selector was changed to paren
				if (oldSelNum != selNumChanged ||
						[OPEN_PAREN, CLOSE_PAREN].contains(value)) {
					appendParen(value, selType, newSelNum, selsBld);
					newSelNum += 1;
				} // if selector was unchanged, or changed to paren
				if (oldSelNum == selNumChanged && value == CLOSE_PAREN) {
					appendParen(BLANK, selType, newSelNum, selsBld);
					newSelNum += 1;
				} // if selector was changed to paren
				if (oldSelNum == selNumChanged && value == BLANK) {
					nextSel = document.getElementById('sel' + (oldSelNum + 1));
					nextNotParen = !nextSel || nextSel.name != selType;
					if (prevNotParen && nextNotParen) {
						appendParen(value, selType, newSelNum, selsBld);
						newSelNum += 1;
					} // if neither preceded nor followed by paren
				} // if selector was changed to blank
			} // if this selector is and was not set to OF
			break;
		case CONJXN:
			// if changed to AND or OR,
			//		add OPEN_PAREN, RULE_NUM, CLOSE_PAREN after it
			// if changed to OF, add NUM_RULES before it
			// if changed to BLANK,
			//		remove the OPEN_PAREN, RULE_NUM, CLOSE_PAREN after it
			appendConjxn(value, newSelNum, selsBld);
			newSelNum += 1;
			if (oldSelNum == selNumChanged && value != BLANK) {
				nextSel = document.getElementById('sel' + (oldSelNum + 1));
				if (!nextSel) {
					appendParen(BLANK, OPEN_PAREN, newSelNum, selsBld);
					newSelNum += 1;
					appendRuleSelector(0, newSelNum, selsBld);
					newSelNum += 1;
					appendParen(BLANK, CLOSE_PAREN, newSelNum, selsBld);
					newSelNum += 1;
					appendConjxn(BLANK, newSelNum, selsBld);
					newSelNum += 1;
					numRuleSels += 1;
				} // if there's no next selection
			} // if selector was changed to AND or OR
			if (oldSelNum == selNumChanged && value == BLANK) {
				numRuleSels -= 1;
				while (true) {
					oldSelNum += 1;
					selector = document.getElementById('sel' + oldSelNum);
					if (!selector || selector.name == CONJXN) {
						break;
					}
				} // until we reach the next conjunction or the end
			} // if selector was changed to blank
			break;
		case NUM_RULES: // do nothing
			break;
		default: // case RULE_NUM
			appendRuleSelector(value, newSelNum, selsBld);
			newSelNum += 1;
			break;
		} // switch
		oldSelNum += 1;
	} // while true
	selsText = selsBld.toString();
	// alert(selsText);
	setInnerHTML('combinationExpression', selsText);
} // rewriteSelectors()

function getCombineExpr() {
	"use strict";
	var dataBld = new String.builder(),
		selNum = 0,
		numOpen = 0,
		numClosed = 0,
		blankConjxns = [],
		numBlankConjxns = 0,
		firstNumRulesSel = true,
		selector,
		type,
		combPart,
		data;
	while (true) {
		selector = document.getElementById('sel' + selNum);
		if (!selector) {
			break;
		}
		type = selector.name;
		combPart = selector.value;
		if (type == OPEN_PAREN && combPart == OPEN_PAREN) {
			numOpen += 1;
		}
		if (type == CLOSE_PAREN && combPart == CLOSE_PAREN) {
			numClosed += 1;
		}
		if (type == CONJXN && combPart == BLANK) {
			blankConjxns[numBlankConjxns] = selNum;
			numBlankConjxns += 1;
		}
		if (numOpen - numClosed < 0) {
			return 'Traversing from left to right, the ' +
				'number of close-parentheses must not exceed the number of ' +
				'open-parentheses at any time.';
		}
		if (type == NUM_RULES) {
			dataBld.append(combPart).append(firstNumRulesSel ? TO : OF);
			firstNumRulesSel = !firstNumRulesSel;
		} else if (![BLANK, OF].contains(combPart)) {
			dataBld.append(combPart);
		}
		selNum += 1;
	} // while true
	data = dataBld.toString();
	// alert(data);
	return (numOpen != numClosed ?
				'The number of open- and close-parentheses must be the same.' :
			numBlankConjxns > 1 ?
					'You need to add conjunctions between the rules.' :
					numBlankConjxns === 0 || blankConjxns[0] != selNum - 1 ?
							'Your expression ends with a dangling conjunction.' :
							data);
} // getCombineExpr()

// --> end HTML comment

