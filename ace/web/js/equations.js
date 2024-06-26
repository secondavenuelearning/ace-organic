// <!-- avoid parsing the following as HTML
/** Collects a response consisting of a series of expressions or equations in
order.
Requires that the including page import com.epoch.physics.Equations
Requires that the including page contain an element with ID 'eqnsTable'
whose inner HTML can be replaced.

Requires this *relative* pathway be included:

	<script src="<%= pathToRoot %>nosession/mathjax/MathJax.js?config=TeX-AMS-MML_HTMLorMML.js"
			type="text/javascript"></script>

Also include <%= pathToRoot %>js/xmlLib.js if user will be inputting data

Requires that body onload="setUpEqns();", which reads as follows:

	<%@ include file="/js/equations.jsp.h" %>
	function setUpEqns() {
		initEqnConstants();
		initEqnPhrases( // optional
				'<%= Utils.toValidJS(ansPhrases[equations.ADD_EQN]) %>',
				'<%= Utils.toValidJS(ansPhrases[equations.REMOVE_EQN]) %>',
				'<%= Utils.toValidJS(ansPhrases[equations.BLANK_SUBMIT]) %>'
				);
		setPathToRoot('<%= Utils.toValidJS(pathToRoot) %>'); // only necessary for AJAX
		var eqns = [];
		<% final Equations eqnsObj = new Equations(lastResp);
		final String[] eqns = eqnsObj.getEntries();
		final String constants = eqnsObj.getConstants();
		for (final String eqn : eqns) { %>
			eqns.push('<%= Utils.toValidTextbox(eqns[eqnNum]) %>');
		<% } // for each current equation %>
		setEqns(eqns, '<%= Utils.toValidJS(constants) %>');
	} // setUpEqns()
*/

/*jsl:option explicit*/

var equations = { // global and local variables
	ADD_EQN: 'Insert here',
	REMOVE_EQN: 'Remove',
	BLANK_SUBMIT: 'Please do not submit any blank equations.',
	CONSTANTS: 'Constants',
	XML_TAG: undefined,
	EQUATION_TAG: undefined,
	CONSTANTS_TAG: undefined,
	VARS_NOT_UNITS_TAG: undefined,
	DISABLED_ATTR_TAG: undefined,
	numInitialEqns: 0,
	pathToRoot: '../',
	updateDynamically: true,
	omitConstantsField: false,
	disableConstantsField: false
};

/** Finishes the table showing the equations. */
function finishEqnBld(eqnBld) {
	"use strict";
	eqnBld.append('<\/table>');
} // finishEqnBld()

/** Converts the entered equations to XML. */
function getEqnsXML() {
	"use strict";
	var constants = getValue(equations.CONSTANTS_TAG),
		constantsNode,
		attributes,
		variablesNotUnits,
		varsNode,
		eqnNum,
		eqnName,
		eqn,
		eqnNode;
	var xmlDoc = getParentXMLDoc(equations.XML_TAG);
	var xmlNode = getFirstNode(xmlDoc, equations.XML_TAG);
	if (!isEmpty(constants)) {
		attributes = (cellExists('equations.disableConstantsField') &&
					getChecked('equations.disableConstantsField') ?
					[[equations.DISABLED_ATTR_TAG, 'true']] : []);
		constantsNode = addNewNode(xmlDoc, xmlNode, 
				equations.CONSTANTS_TAG, attributes);
		constantsNode.appendChild(getTextNode(xmlDoc, trim(constants)));
	} // if there are constants
	variablesNotUnits = getValue(equations.VARS_NOT_UNITS_TAG);
	if (!isEmpty(variablesNotUnits)) {
		varsNode = addNewNode(xmlDoc, xmlNode, 
				equations.VARS_NOT_UNITS_TAG, attributes);
		varsNode.appendChild(getTextNode(xmlDoc, trim(variablesNotUnits)));
	} // if there are names of variables not to be considered as units
	eqnNum = 1;
	while (true) {
		eqnName = 'eqn' + eqnNum;
		eqnNum += 1;
		if (!cellExists(eqnName)) {
			break;
		}
		eqn = getValue(eqnName);
		if (isWhiteSpace(eqn)) {
			toAlert(equations.BLANK_SUBMIT);
			return '';
		}
		eqnNode = addNewNode(xmlDoc, xmlNode, 
				equations.EQUATION_TAG, attributes);
		eqnNode.appendChild(getTextNode(xmlDoc, eqn));
	} // for each equation
	var xml = getXML(xmlDoc);
	// alert(xml);
	return xml;
} // getEqnsXML()

/** Called by initEqnConstants in js/equations.jsp.h. */
function initEqnConstants2(myXML_TAG, myEQUATION_TAG, myCONSTANTS_TAG,
		myVARS_NOT_UNITS_TAG, myDISABLED_ATTR_TAG) {
	"use strict";
	equations.XML_TAG = myXML_TAG;
	equations.EQUATION_TAG = myEQUATION_TAG;
	equations.CONSTANTS_TAG = myCONSTANTS_TAG;
	equations.VARS_NOT_UNITS_TAG = myVARS_NOT_UNITS_TAG;
	equations.DISABLED_ATTR_TAG = myDISABLED_ATTR_TAG;
} // initEqnConstants2()

/** For initializing non-English phrases. */
function initEqnPhrases(myADD_EQN, myREMOVE_EQN, myBLANK_SUBMIT, myCONSTANTS) {
	"use strict";
	equations.ADD_EQN = myADD_EQN;
	equations.REMOVE_EQN = myREMOVE_EQN;
	equations.BLANK_SUBMIT = myBLANK_SUBMIT;
	equations.CONSTANTS = myCONSTANTS;
} // initEqnPhrases()

/** Starts writing the table of equations. */
function initializeEqnBld(constants) {
	"use strict";
	var eqnBld = new String.builder().append('<table>\n');
	if (!equations.omitConstantsField) {
		eqnBld.append('<tr><td class="regtext" ' +
					'style="vertical-align:middle; padding-right:5px;">').
				append(equations.CONSTANTS).
				append(':<\/td><td class="regtext" style="vertical-align:middle;">' +
					'<input type="text" name="').
				append(equations.CONSTANTS_TAG).
				append('" id="').
				append(equations.CONSTANTS_TAG).
				append('" size="30"');
		if (equations.disableConstantsField) {
			eqnBld.append(' style="background-color:yellow;" disabled="disabled"');
		} // if the field is disabled
		if (constants) {
			eqnBld.append(' value="').append(constants).append('"');
		} // if there are constants to preload
		eqnBld.append('>\n<\/td><\/tr>\n');
	} // if should include the constants field
	eqnBld.append('<tr>').
			append('<td><\/td>').
			append('<td class="regtext small">');
	if (equations.numInitialEqns === 0) {
		eqnBld.append('<a href="javascript:addEqn(0);">').
				append(equations.ADD_EQN).
				append('<\/a>');
	} // if equations.numInitialEqns
	eqnBld.append('<\/td><\/tr>\n');
	return eqnBld;
} // initializeEqnBld()

/** Sets options about the constants field. */
function setConstantsFieldOptions(omitField, disableField) {
	"use strict";
	equations.omitConstantsField = omitField;
	if (disableField) {
		equations.disableConstantsField = disableField;
	}
} // setOmitConstantsField()

/** Sets the number of initial equations. */
function setNumInitialEqns(num) {
	"use strict";
	equations.numInitialEqns = num;
} // setNumInitialEqns()

/** Sets the number of initial equations. */
function setPathToRoot(path) {
	"use strict";
	equations.pathToRoot = path;
} // setNumInitialEqns()

/** Updates the typeset expression or equation. */
function updateTeX() {
	"use strict";
	var response, tex, eqnNum;
	if (xmlHttp.readyState === 4) { // ready to continue
		response = xmlHttp.responseText;
		tex = extractField(response, 'texValue');
		eqnNum = extractField(response, 'eqnNum');
		window.UpdateMath(eqnNum, tex);
		// setInnerHTML('eqnsAlerts', 'eqn ' + eqnNum + ': ' + tex);
	}
} // updateTeX()

/** Uses AJAX to call EquationFunctions.toTeX() to convert the Maxima format for
 * expressions and equations into the TeX format. */
function callServerForEqn(eqnNum) {
	"use strict";
	var eqnName, url, toSend;
	if (xmlHttp && equations.updateDynamically) {
		eqnName = 'eqn' + eqnNum;
		if (!cellExists(eqnName)) {
			return;
		}
		url = equations.pathToRoot + 'homework/maximaToTex.jsp';
		toSend = new String.builder().
				append('maximaEqn=').
				append(encodeURIComponent(getValue(eqnName))).
				append('&eqnNum=').
				append(eqnNum).toString();
		callAJAX(url, toSend, updateTeX);
	} else {
		setInnerHTML('eqnsAlerts', 'no xmlHttp');
	}
} // callServerForEqn()

/** Writes an equation to the table. */
function addToEqnBld(eqnBld, eqn, eqnNum) {
	"use strict";
	eqnBld.append('<tr><td class="regtext" ' +
			'style="vertical-align:middle; padding-right:5px;">(').
			append(eqnNum).
			append(')<\/td><td class="regtext" style="vertical-align:middle;">').
			append('<input type="text" name="eqn').
			append(eqnNum).
			append('" id="eqn').
			append(eqnNum).
			append('" size="30" ');
	if (eqnNum <= equations.numInitialEqns) {
		eqnBld.append('style="background-color:yellow;" disabled="disabled" ');
	} // if equation has been disabled
	eqnBld.append('onkeyup="callServerForEqn(').
			append(eqnNum).
			append(');" ').
			append('value="').
			append(eqn).
			append('">\n').
			append('<\/td>').
			append('<td class="regtext small" style="vertical-align:middle;">');
	if (eqnNum > equations.numInitialEqns) {
		eqnBld.append('<a href="javascript:removeEqn(').
				append(eqnNum).
				append(');">').
				append(equations.REMOVE_EQN).
				append('<\/a>');
	} // if we have passed the initial equations
	eqnBld.append('<\/td>').
			append('<td class="regtext" ' +
			'style="vertical-align:middle; padding-left:40px;"><div id="mirror').
			append(eqnNum).
			append('">').
			append('$${}$$').
			append('<\/div>').
			append('<\/td>').
			append('<\/tr>\n');
	if (eqnNum >= equations.numInitialEqns) {
		eqnBld.append('<tr><td class="regtext" style="padding-right:10px;"><\/td>').
				append('<td class="regtext small">').
				append('<a href="javascript:addEqn(').
				append(eqnNum).
				append(');">').
				append(equations.ADD_EQN).
				append('<\/a>\n').
				append('<\/td>').
				append('<\/tr>\n');
	} // if we have passed the initial equations
} // addToEqnBld()

/** Inserts a line for a new equation. */
function addEqn(insertAfter) {
	"use strict";
	var eqnBld = initializeEqnBld(getValue(equations.CONSTANTS_TAG)),
		eqn,
		eqnNum,
		eqnName,
		modEqnNum,
		eqnOut;
	if (insertAfter === 0) {
		addToEqnBld(eqnBld, '', 1);
	} // if at insertion point
	eqnNum = 1;
	while (true) {
		eqnName = 'eqn' + eqnNum;
		if (!cellExists(eqnName)) {
			break;
		}
		eqn = getValue(eqnName);
		modEqnNum = eqnNum + (eqnNum > insertAfter ? 1 : 0);
		addToEqnBld(eqnBld, eqn, modEqnNum);
		if (eqnNum === insertAfter) {
			addToEqnBld(eqnBld, '', eqnNum + 1);
		} // if at insertion point
		eqnNum += 1;
	} // for each equation in new table
	finishEqnBld(eqnBld);
	eqnOut = eqnBld.toString();
	setInnerHTML('eqnsTable', eqnOut);
} // addEqn()

/** Removes an equation from the table. */
function removeEqn(removed) {
	"use strict";
	var eqnBld, eqnNum, eqnName, eqn, modEqnNum, eqnOut;
	if (!document.getElementById('eqnsTable')) {
		// return if the equations table is not available
		// (for some unfathomable reason, this function is being called early)
		return;
	}
	eqnBld = initializeEqnBld(getValue(equations.CONSTANTS_TAG));
	eqnNum = 1;
	while (true) {
		eqnName = 'eqn' + eqnNum;
		if (!cellExists(eqnName)) {
			break;
		}
		eqn = getValue(eqnName);
		if (eqnNum !== removed) {
			modEqnNum = eqnNum - (eqnNum > removed ? 1 : 0);
			addToEqnBld(eqnBld, eqn, modEqnNum);
		} // if eqn is not to be removed
		eqnNum += 1;
	} // for each equation in old table
	finishEqnBld(eqnBld);
	eqnOut = eqnBld.toString();
	setInnerHTML('eqnsTable', eqnOut);
} // removeEqn()

/** Initializes the table showing the logical sequence of equations. */
function setEqns(eqns, constants) {
	"use strict";
	var eqnBld = initializeEqnBld(constants),
		eqnNum,
		eqnOut;
	for (eqnNum = 1; eqnNum <= eqns.length; eqnNum += 1) {
		addToEqnBld(eqnBld, eqns[eqnNum - 1], eqnNum);
	} // for each current equation
	finishEqnBld(eqnBld);
	eqnOut = eqnBld.toString();
	setInnerHTML('eqnsTable', eqnOut);
	for (eqnNum = 1; eqnNum <= eqns.length; eqnNum += 1) {
		callServerForEqn(eqnNum);
	} // for each TeX-formatted equation
} // setEqns()

/** Toggles whether to update the MathJax-formatted equations dynamically. */
function toggleUpdateDynamically() {
	"use strict";
	var eqnNum, eqnName;
	equations.updateDynamically = !equations.updateDynamically;
	if (equations.updateDynamically) {
		eqnNum = 1;
		while (true) {
			eqnName = 'eqn' + eqnNum;
			eqnNum += 1;
			if (cellExists(eqnName)) {
				callServerForEqn(eqnNum);
			} else {
				break;
			}
		} // for each equation
	} // if updating
} // toggleUpdateDynamically()

// --> end HTML comment
