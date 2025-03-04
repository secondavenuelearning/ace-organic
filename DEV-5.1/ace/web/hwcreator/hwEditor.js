// <!-- avoid parsing the following as HTML

var CONTINUE = true;
var paintedChooseRxnConds = false;
var numRandGroupsChecked = 0;

// forward declarations
var afterSave;

function cancelOp() {
	"use strict";
	self.location.href = 'hwSetList.jsp';
} // cancelOp()

function changeBundleSize(ddBox, group) {
	"use strict";
	var bundleSize = ddBox.value,
		toSend = new String.builder()
			.append('bundleSize=').append(bundleSize)
			.append('&groupNum=').append(group)
			.append('&editAction=changeBundleSize')
			.toString();
	callAJAX('hwEditor.jsp', toSend, doNoMore);
	setSumPoints();
} // changeBundleSize()

function changePick(ddBox, group) {
	"use strict";
	var count = ddBox.value,
		toSend = new String.builder()
			.append('count=').append(count)
			.append('&groupNum=').append(group)
			.append('&editAction=changePick')
			.toString();
	callAJAX('hwEditor.jsp', toSend, doNoMore);
	setSumPoints();
} // changePick()

function chooseAllowedCondns() {
	"use strict";
	doBeforeReload();
	openEvaluatorWindow('chooseRxnConds.jsp');
} // chooseAllowedCondns()

// Called by chooseRxnConds.jsp.
function getAllowedRxnCondns() {
	"use strict";
	return document.selectform.allowedRxnCondns.value;
} // getAllowedRxnCondns()

// Called by chooseRxnConds.jsp.
function setAllowedRxnCondns(allowedRxnCondns) {
	"use strict";
	document.selectform.allowedRxnCondns.value = allowedRxnCondns;
	reloadMe();
} // setAllowedRxnCondns()

function getDependencies(numQGroups, DEP_PAIRS_SEP) {
	"use strict";
	var qGrpNum, dependSelector, independentQIdStr,
		dependBld = new String.builder(),
		noDependencies = true;
	for (qGrpNum = 1; qGrpNum <= numQGroups; qGrpNum += 1) {
		dependSelector = document.getElementById('dependenceOf' + qGrpNum);
		if (dependSelector) {
			independentQIdStr = dependSelector.value;
			if (isEmpty(independentQIdStr)) independentQIdStr = '0';
		} else {
			independentQIdStr = '0';
		} // if the selector exists
		if (!noDependencies) dependBld.append(DEP_PAIRS_SEP);
		else noDependencies = false;
		dependBld.append(independentQIdStr);
	} // for each group of questions
	return dependBld.toString();
} // getDependencies()

function hideChooseRandom() {
	"use strict";
	hideCell('groupUngroup');
} // hideChooseRandom()

function hideSelectorButtons() {
	"use strict";
	if (document.getElementById('fixed1')) {
		hideCell('fixed1');
		hideCell('fixed2');
	}
} // hideSelectorButtons()

function paintChooseRxnCondnsNow(alertAny, chooseButton) {
	"use strict";
	var out1 = '',
		out2,
		allowedRxnCondns = document.selectform.allowedRxnCondns.value,
		rxnConds,
		cell1,
		cell2;
	if (allowedRxnCondns === 'null') {
		allowedRxnCondns = '';
	}
	if (isEmpty(allowedRxnCondns) || allowedRxnCondns == 'all') { // do not use ===
		out1 = alertAny;
	} else {
		rxnConds = allowedRxnCondns.split(':');
		out1 = new String.builder()
				.append('In multistep synthesis questions, the ' +
					'students will be able to choose from ')
				.append(rxnConds.length)
				.append(' reaction conditions that you have chosen.')
				.toString();
	}
	out2 = chooseButton;
	cell1 = document.getElementById('chooseRxnConds1');
	cell2 = document.getElementById('chooseRxnConds2');
	cell1.innerHTML = out1;
	cell2.innerHTML = out2;
	cell1.style.paddingBottom = '10px;';
	cell2.style.paddingBottom = '10px;';
} // paintChooseRxnCondns()

function reloadMe() {
	"use strict";
	var proceed = doBeforeReload();
	if (proceed) {
		document.selectform.submit();
	}
} // reloadMe()

function addSelected() {
	"use strict";
	var form = document.selectform,
		indices = getSelectedValues(form.qChecker);
	if (indices.length === 0) {
		return;
	}
	form.qNums.value = indices.join(':');
	form.editAction.value = 'addQ';
	reloadMe();
} // addSelected()

function exitContinue() {
	"use strict";
	document.selectform.editAction.value = 'exit';
	reloadMe();
} // exitContinue()

function moveSelected(serialNo) {
	"use strict";
	var form = document.selectform,
			newSerialNo = parseInt(getValue('moveMenu' + serialNo), 10);
	form.qNums.value = serialNo;
	form.editAction.value = 'moveQ';
	form.action += '?moveTo=' + newSerialNo;
	reloadMe();
} // moveSelected()

function removeErrorMessage() {
	"use strict";
	clearInnerHTML('error_message');
}

function removeSelected() {
	"use strict";
	var form = document.selectform,
			removeQNums = getSelectedValues(form.removeBoxes),
			removeRandQNums;
	removeQNums.sort(function (a, b) {return a.value - b.value; }); // sort
	//	numerically
	form.qNums.value = removeQNums.join(':');
	form.editAction.value = 'removeQ';
	if (form.randQBoxes) {
		removeRandQNums = getSelectedValues(form.randQBoxes);
		form.action += '?randIndices=' + removeRandQNums.join(':');
	} // if there are random question boxes
	reloadMe();
} // removeSelected()

function saveHWNow(exitContinue, alertName, alertTries) {
	"use strict";
	var form = document.selectform,
		assgtName = form.assgtName.value,
		numTries = UNLIMITED,
		ptsPerQGrpBld = new String.builder(),
		cellName,
		qGrpNum = 1,
		first = true,
		ptsPerQGrp,
		toSend;
	if (isWhiteSpace(assgtName)) {
		toAlert(alertName);
		return;
	}
	form.assgtName.value = trim(assgtName);
	if (!form.unlimited.checked) {
		numTries = form.numTries.value;
		if (isWhiteSpace(numTries) || !canParseToInt(numTries)) {
			toAlert(alertTries);
			return;
		} // if number of tries not good value
	} // if unlimited tries
	form.exitContinue.value = exitContinue;
	doBeforeReload();
	toSend = new String.builder()
			.append('editAction=save&exitContinue=').append(exitContinue)
			.append('&assgtName=')
			.append(encodeURIComponent(trim(form.assgtName.value)))
			.append('&remarks=')
			.append(encodeURIComponent(trim(form.remarks.value)))
			.append('&numTries=').append(numTries)
			.append('&allowedRxnCondns=').append(form.allowedRxnCondns.value)
			.append('&dependencies=').append(form.dependencies.value)
			.append('&ptsPerQGrpStr=').append(form.ptsPerQGrpStr.value)
			.append('&viewType=').append(form.viewType.value)
			.append('&qSetId=').append(form.qSetId.value)
			.append('&isMastery=').append(form.isMastery.checked)
			.toString();
	callAJAX('hwEditor.jsp', toSend, afterSave);
} // saveHWNow() /**/

function getPtsPerQGrpStr() {
	var form = document.selectform,
		ptsPerQGrpBld = new String.builder(),
		cellName,
		qGrpNum = 1,
		first = true,
		ptsPerQGrp;
	while (true) {
		cellName = new String.builder().
				append('pointsOfQGrp').append(qGrpNum).toString();
		if (cellExists(cellName)) {
			if (first) first = false;
			else ptsPerQGrpBld.append('/');
			ptsPerQGrp = getValue(cellName);
			if (isWhiteSpace(ptsPerQGrp) ||
				!canParseToFloat(ptsPerQGrp) ||
				parseToFloat(ptsPerQGrp) <= 0) ptsPerQGrp = 1;
			ptsPerQGrpBld.append(ptsPerQGrp);
		} else break;
		qGrpNum++;
	} // while the cells exist
	return ptsPerQGrpBld.toString();
} // getPtsPerQGrpStr()

function afterSave() {
	"use strict";
	if (xmlHttp.readyState === 4) { // ready to continue
		var form = document.selectform,
				responsePage = xmlHttp.responseText,
				saveMessageValue =
					extractField(responsePage, 'saveMessageValue'),
				footerErrorMessageValue =
					extractField(responsePage, 'footerErrorMessageValue');
		if (!isEmpty(saveMessageValue)) {
			setMsg(saveMessageValue);
		} else if (!isEmpty(footerErrorMessageValue)) {
			setMsg(footerErrorMessageValue);
		} else {
			clearInnerHTML('msgCell');
		} // if there are messages
		setAfterSaveButtons();
		setSumPoints();
		form.hasBeenSaved.value = 'true';
		if (form.exitContinue.value === 'true') {
			self.location.href = new String.builder().
					append('editHWProps.jsp?hwNum=').
					append(form.hwNum.value).toString();
		} // if continuing to next editing page
	} // if ready
} // afterSave()

function selectAll(box) {
	"use strict";
	setAllCheckBoxes(document.selectform.qChecker, box.checked);
}

function selectAllSetQs() {
	"use strict";
	setAllCheckBoxes(document.selectform.removeBoxes, true);
}

function selectNoSetQs() {
	"use strict";
	setAllCheckBoxes(document.selectform.removeBoxes, false);
}

function selectNone() {
	"use strict";
	setAllCheckBoxes(document.selectform.qChecker, false);
}

function setMsg(msg) {
	"use strict";
	setInnerHTML('msgCell', new String.builder()
			.append('<div class="whiteTable" ' +
				'style="padding-bottom:5px; padding-left:10px; ' +
				'padding-right:10px; border-color:#FF0000; ' +
				'color:#FF0000; padding:5px;">')
			.append(msg).append('</div>')
			.toString());
} // setMsg()

function doNoMore() {
	"use strict";
	if (xmlHttp.readyState === 4) { // ready to continue
		setMsg('');
	} // if ready
} // doNoMore()

function showChooseRandom() {
	"use strict";
	showCell('groupUngroup');
} // showChooseRandom()

function showSelectorButtons() {
	"use strict";
	if (document.getElementById('fixed1')) {
		showCell('fixed1');
		showCell('fixed2');
	}
} // showSelectorButtons()

function toggleNumTries() {
	"use strict";
	var form = document.selectform;
	if (form.unlimited.checked) {
		form.numTries.value = '';
		form.numTries.disabled = true;
	} else {
		form.numTries.disabled = false;
	}
} // toggleNumTries()

function ungroupRandom() {
	"use strict";
	var form = document.selectform,
		selGroupBoxes = getSelectedValues(form.removeBoxes);
	form.qNums.value = selGroupBoxes.join(':');
	form.editAction.value = 'ungroupRandom';
	reloadMe();
} // ungroupRandom()

function updateRandomVisibility() {
	"use strict";
	var selQNums = getSelected(document.selectform.removeBoxes);
	if (selQNums.length > 1) {
		if (numRandGroupsChecked === 0) {
			showChooseRandom();
		}
	} else {
		hideChooseRandom();
	}
} // updateRandomVisibility()

function setAllPoints() {
	"use strict";
	var newVal = getValue('allQPts'),
			newValFloat = parseToFloat(newVal),
			qNum = 1,
			cellName,
			sum = 0;
	while (true) {
		cellName = new String.builder().
				append('pointsOfQGrp').append(qNum).toString();
		if (cellExists(cellName)) {
			setValue(cellName, newVal);
			sum += newValFloat;
			qNum++;
		} else break;
	} // while the cells exist
	setSumPoints();
} // setAllPoints()

function setSumPoints() {
	"use strict";
	var sum = 0,
			qNum = 1,
			val,
			pts,
			bundleSize,
			cellName;
	while (true) {
		cellName = new String.builder().
				append('pointsOfQGrp').append(qNum).toString();
		if (cellExists(cellName)) {
			pts = 1;
			val = getValue(cellName);
			if (!isWhiteSpace(val) && canParseToFloat(val)) {
				pts = parseToFloat(val);
				if (pts <= 0) pts = 1;
			} // if there's a value
			cellName = new String.builder().
					append('picker').append(qNum).toString();
			if (cellExists(cellName)) {
				pts *= parseToInt(getValue(cellName));
				cellName = new String.builder().
						append('sizer').append(qNum).toString();
				if (cellExists(cellName)) {
					pts *= parseToInt(getValue(cellName));
				} // if there's a bundle size
			} // if it's a random group
			sum += pts;
		} else break;
		qNum++;
	} // while the cells exist
	setInnerHTML('sumPoints', sum);
} // setSumPoints()

// --> end HTML comment
