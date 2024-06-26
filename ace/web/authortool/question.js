// <!-- avoid parsing the following as HTML
/* javascript functions of question.jsp that do not require Java values */

/* Values for parameter how in setEditables(). */
var SUBMIT_FORM = 1;
var EVAL_WINDOW = 2;
var FIG_EDIT_WINDOW = 3;
var PREVIEW_WINDOW = 4;

// any changes in Question.java should be reflected here
function calculateQFlags() {
	"use strict";
	var form = document.questionForm,
		qFlags = 0,
		preloadFig;
	if (isChecked(form.howShowHydrogens)) {
		qFlags |= parseInt(form.allNo.value, 10);
	}
	if (isChecked(form.withMapping)) { qFlags |= SHOWMAPPING; }
	if (isChecked(form.withLonePairs)) { qFlags |= SHOWLONEPAIRS; }
	if (isChecked(form.withRSLabels)) { qFlags |= SHOWRSLABELS; }
	if (isChecked(form.threeDim)) { qFlags |= THREEDIM; }
	if (isChecked(form.hideFromOthers)) { qFlags |= HIDE; }
	if (isChecked(form.useSubstns)) { qFlags |= USES_SUBSTNS; }
	if (isChecked(form.disallowMult)) { qFlags |= DISALLOW_MULT; }
	if (isChecked(form.allowUnranked)) { qFlags |= ALLOW_UNRANKED; }
	if (isChecked(form.useSciNotn)) { qFlags |= USE_SCI_NOTN; }
	if (isChecked(form.numsOnly)) { qFlags |= NUMS_ONLY; }
	if (isChecked(form.labelOrbs)) { qFlags |= LABEL_ORBS; }
	if (isChecked(form.requireInt)) { qFlags |= REQUIRE_INT; }
	if (isChecked(form.noChemFormatting)) { qFlags |= NO_CHEM_FORMATTING; }
	if (isChecked(form.omitConstantsField)) { qFlags |= NO_CONSTANTS_FIELD; }
	if (isChecked(form.disallowSuperfluousWedges)) {
		qFlags |= DISALLOW_SUPERFLUOUS_WEDGES;
	}
	if (isChecked(form.scramble) || (!form.scramble && initScramble)) {
		qFlags |= SCRAMBLE;
	}
	if (isChecked(form.exceptLast) || (!form.exceptLast && initExceptLast)) {
		qFlags |= EXCEPT_LAST;
	}
	if (isChecked(form.badValenceInvisible) ||
			(!form.badValenceInvisible && initBadValence)) {
		qFlags |= BADVALENCEINVISIBLE;
	}
	if (isChecked(form.withPreload)) {
		qFlags |= PRELOAD;
		preloadFig = form.preloadFig;
		if (preloadFig) {
			qFlags |=
				(parseInt(preloadFig.value, 10) << FIG_TO_PRELOAD_SHIFT);
		}
	}
	return qFlags;
} // calculateQFlags()

function clearMessage() {
	"use strict";
	hideLayer('qMessage');
} // clearMessage()

function collapseQdata(qDataTableNum) {
	"use strict";
	clearInnerHTML('QData' + qDataTableNum);
	if (getMajorQType() === NUMERIC) {
		clearInnerHTML('statementAdvice');
	}
	rewriteUniversalDataButtons();
} // collapseQdata()

function convertFields() {
	"use strict";
	if (document.actionForm.statement.value) {
		document.actionForm.statement.value = // no effect?  Raphael 8/2015
			document.actionForm.statement.value;
	}
} // convertFields

function getBook() {
	"use strict";
	return document.questionForm.book.value;
}

function getBookQNumber() {
	"use strict";
	return document.questionForm.bookQNumber.value;
}

function getChapter() {
	"use strict";
	return document.questionForm.chapter.value;
}

function getKeywords() {
	"use strict";
	return document.questionForm.keywords.value;
}

function getLoadEvaluatorSuffix(qType) {
	"use strict";
	return ([MARVIN, LEWIS, MECHANISM, SYNTHESIS,
				ORB_E_DIAGRAM, RXN_COORD, FORMULA].contains(qType) ? 'Chem' :
			[DRAW_VECTORS, EQUATIONS].contains(qType) ? 'Physics' :
					'Generic');
} // getLoadEvaluatorSuffix()

function getMajorQType() {
	"use strict";
	return parseInt(document.questionForm.qType.value, 10);
}

function getNumEvaluators() {
	"use strict";
	return numEvaluators;
}

function getNumQData(tableNum) {
	"use strict";
	return (tableNum === GENERAL ? numQDataGeneral : numQDataRGrpColls);
}

function getStatement() {
	"use strict";
	return document.questionForm.statement.value;
}

function hideMsgTable() {
	"use strict";
	clearInnerHTML('msgTable');
} // hideMsgTable()

function jump() {
	"use strict";
	document.actionForm.action.value = 'jump';
	document.actionForm.submit();
} // jump

function makeCheckBox(name, checked, text, where, extraOnClick) {
	"use strict";
	var optionBld = new String.builder()
			.append('<span id="').append(name)
			.append('"><input type="checkbox" name="').append(name)
			.append('" onclick="hideMsgTable();');
	if (extraOnClick) {
		optionBld.append(' ').append(extraOnClick);
	} // if there is stuff to do after clicking
	optionBld.append('"');
	if (checked) {
		optionBld.append(CHECKED);
	}
	optionBld.append(' \/> ').append(text).append('<\/span>');
	setInnerHTML(where, optionBld.toString());
} // makeCheckBox()

function moveNext(nextQId) {
	"use strict";
	if (nextQId !== 0) {
		document.actionForm.action.value = 'movenext';
		document.actionForm.submit();
	} else {
		alert('There are no more entries.');
	}
} // moveNext

function movePrev(prevQId) {
	"use strict";
	if (prevQId !== 0) {
		document.actionForm.action.value = 'moveprev';
		document.actionForm.submit();
	} else {
		alert('There are no previous entries.');
	}
} // movePrev

function omitConstantsField(qFlags) {
	"use strict";
	return (qFlags & NO_CONSTANTS_FIELD) !== 0;
} // omitConstantsField()

function resetQ() {
	"use strict";
	document.actionForm.action.value = 'reset';
	document.actionForm.submit();
} // resetQ

function returnUserMain(qSetId) {
	"use strict";
	self.location.href = 'questionsList.jsp?qSetId=' + qSetId;
}

function setAppletName(name) {
	"use strict";
	setInnerHTML('usesApplet', name + ' initialization parameters');
}

function setEditables(destination, how) {
	"use strict";
	document.saveEditablesForm.qType.value = getMajorQType();
	document.saveEditablesForm.qFlags.value = calculateQFlags();
	// alert('qFlags = ' + document.saveEditablesForm.qFlags.value);
	document.saveEditablesForm.book.value = getBook();
	document.saveEditablesForm.chapter.value = getChapter();
	document.saveEditablesForm.bookQNumber.value = getBookQNumber();
	document.saveEditablesForm.qStmt.value = getStatement();
	document.saveEditablesForm.keywords.value = getKeywords();
	document.saveEditablesForm.destination.value = destination;
	if (how === SUBMIT_FORM) {
		document.saveEditablesForm.submit();
	} else if (how === EVAL_WINDOW) {
		openEvaluatorWindow('newWindow.jsp');
	} else if (how === FIG_EDIT_WINDOW) {
		openFigureEditingWindow('newWindow.jsp');
	} else if (how === PREVIEW_WINDOW) {
		openPreviewWindow('newWindow.jsp');
	}
} // setEditables()

function addEval() {
	"use strict";
	var go = new String.builder().append('evaluators/loadEvaluator')
			.append(getLoadEvaluatorSuffix(getMajorQType()))
			.append('.jsp?evalNum=0&virgin=true'),
		destination = go.toString();
	setEditables(destination, EVAL_WINDOW);
} // addEval()

function addFigure() {
	"use strict";
	var destination = 'figures/loadFigure.jsp?figNum=0';
	setEditables(destination, EVAL_WINDOW);
}

function addQDatum(tableNum) {
	"use strict";
	var go = new String.builder()
			.append('qData/loadQData.jsp?qDatumNum=0&virgin=true&tableNum=')
			.append(tableNum),
			destination = go.toString();
	setEditables(destination, EVAL_WINDOW);
} // addQDatum()

function changeSelection(reloadFigs) {
	"use strict";
	var destination = 'figures/changeFigure.jsp?figureIndex='  +
			document.questionForm.figurelist.value;
	if (reloadFigs) {
		destination += '&reloadFigs=true';
	}
	setEditables(destination, SUBMIT_FORM);
} // changeSelection()

function cloneEval(evalNum) {
	"use strict";
	var go = new String.builder()
			.append('evaluators/loadEvaluator')
			.append(getLoadEvaluatorSuffix(getMajorQType()))
			.append('.jsp?evalNum=')
			.append(evalNum)
			.append('&cloneEdit=true&virgin=true'),
		destination = go.toString();
	setEditables(destination, EVAL_WINDOW);
} // cloneEval()

function cloneJoinedEval(evalNum) {
	"use strict";
	var go = new String.builder()
			.append('reentryActions.jsp?action=cloneEval&evalNum=')
			.append(evalNum),
			destination = go.toString();
	setEditables(destination, SUBMIT_FORM);
} // cloneJoinedEval()

function cloneQDatum(tableNum, index) {
	"use strict";
	var go = new String.builder()
			.append('qData/loadQData.jsp?qDatumNum=')
			.append(index)
			.append('&cloneEdit=true&virgin=true&tableNum=')
			.append(tableNum),
			destination = go.toString();
	setEditables(destination, EVAL_WINDOW);
} // cloneQDatum()

function deleteEval(evalNum) {
	"use strict";
	var go = new String.builder()
			.append('reentryActions.jsp?action=deleteEval&evalNum=')
			.append(evalNum),
		destination = go.toString();
	setEditables(destination, SUBMIT_FORM);
} // deleteEval()

function deleteFigure() {
	"use strict";
	var figSelector = document.questionForm.figurelist,
		destination = 'reentryActions.jsp?action=deleteFigure&index=' +
			(figSelector ? figSelector.value : 1);
	setEditables(destination, SUBMIT_FORM);
}

function deleteQDatum(tableNum, index) {
	"use strict";
	var go = new String.builder()
			.append('reentryActions.jsp?action=deleteQDatum&qDatumNum=')
			.append(index)
			.append('&tableNum=')
			.append(tableNum),
			destination = go.toString();
	setEditables(destination, SUBMIT_FORM);
} // deleteQDatum()

function editFigure() {
	"use strict";
	var figSelector = document.questionForm.figurelist,
		destination = 'figures/loadFigure.jsp?figNum=' +
			(figSelector ? figSelector.value : 1);
	setEditables(destination, FIG_EDIT_WINDOW);
}

function editQDatum(tableNum, index) {
	"use strict";
	var go = new String.builder()
			.append('qData/loadQData.jsp?qDatumNum=')
			.append(index)
			.append('&virgin=true&tableNum=')
			.append(tableNum),
			destination = go.toString();
	setEditables(destination, EVAL_WINDOW);
} // editQDatum()

function joinEvals() {
	"use strict";
	var c, length, evalNums, go, destination,
		numAll = 0,
		all_checkbox = document.questionForm.all_checker;
	if (all_checkbox) {
		length = all_checkbox.length || 1;
		if (length === 1) {
			if (all_checkbox.selected || all_checkbox.checked) {
				numAll += 1;
			}
		} else {
			for (c = 0; c < length; c += 1) {
				if (all_checkbox[c].selected || all_checkbox[c].checked) {
					numAll += 1;
				}
			} // length !== 1
		} // length !== 1
	} // all_checkbox
	if (numAll <= 1) {
		alert('You must select more than one item to combine');
		return;
	}
	evalNums = getSelectedValues(all_checkbox);
	go = new String.builder()
			.append('evaluators/joinEvaluators.jsp?virgin=true&evalNums=')
			.append(evalNums.join(':'));
	destination = go.toString();
	setEditables(destination, EVAL_WINDOW);
} // joinEvals()

function moveEval(selector, from) {
	"use strict";
	var go = new String.builder()
			.append('reentryActions.jsp?action=moveEval&from=')
			.append(from)
			.append('&to=')
			.append(selector.value),
		destination = go.toString();
	setEditables(destination, SUBMIT_FORM);
} // moveEval()

function moveQDatum(tableNum, selector, from) {
	"use strict";
	var go = new String.builder()
			.append('reentryActions.jsp?action=moveQDatum&from=')
			.append(from)
			.append('&to=')
			.append(selector.value)
			.append('&tableNum=')
			.append(tableNum),
		destination = go.toString();
	setEditables(destination, SUBMIT_FORM);
} // moveQDatum()

function openEval(evalNum, subevalNum) {
	"use strict";
	var go = new String.builder().append('evaluators/loadEvaluator')
			.append(getLoadEvaluatorSuffix(getMajorQType()))
			.append('.jsp?virgin=true&evalNum=').append(evalNum)
			.append('&subevalNum=')
			.append(subevalNum),
		destination = go.toString();
	setEditables(destination, EVAL_WINDOW);
} // loadOthers()

function openJoinedEval(evalNum) {
	"use strict";
	var go = new String.builder()
			.append('evaluators/joinEvaluators.jsp?virgin=true&evalNums=')
			.append(evalNum),
		destination = go.toString();
	setEditables(destination, EVAL_WINDOW);
} // loadOthers()

function preview(qSetId, masterEdit) {
	"use strict";
	if (getMajorQType() === TABLE && getNumQData(GENERAL) < 2) {
		alert('You cannot preview this question until you have ' +
				'entered the information about the number of ' +
				'rows and columns in the response table.');
		return;
	}
	var destination = 'startPreview.jsp?qSetId=' + qSetId +
			'&masterEdit=' + masterEdit;
	setEditables(destination, PREVIEW_WINDOW);
} // preview

function setFigureSelector(numFigs, preloadFig, SELECTED) {
	"use strict";
	var figNum, out;
	if (numFigs > 1) {
		out = new String.builder()
				.append('<select name="preloadFig" id="preloadFig">');
		for (figNum = 0; figNum < numFigs; figNum += 1) {
			out.append('<option value="').append(figNum).append('"');
			if (figNum === preloadFig) {
				out.append(SELECTED);
			}
			out.append('>').append(figNum + 1).append('<\/option>');
		} // for each figure up to 4
		out.append('<\/select>');
		setInnerHTML('figureSelector', out.toString());
	} // if there are multiple figures
} // setFigureSelector()

function setFormValues() {
	"use strict";
	var qType = getMajorQType();
	document.actionForm.qType.value = qType;
	document.actionForm.qFlags.value = calculateQFlags();
	if (getNumEvaluators() === 0) {
		alert('You must write at least one evaluator' +
			' before you can save this question.');
		return false;
	}
	if (qType === TABLE && getNumQData(GENERAL) < 2) {
		alert('You must enter the number of rows and columns' +
				' in the table that the users will complete' +
				' before you can save this question.');
		return false;
	}
	if (qType === SYNTHESIS && getNumQData(GENERAL) === 0) {
		alert('You must enter at least one rule about ' +
				'permissible starting materials ' +
				'before you can save this question.');
		return false;
	}
	if (qType === CLICK_IMAGE && !firstFigIsImage) {
		alert('Figure 1 of a clickable map question must be an image.');
		return false;
	}
	if (qType === DRAW_VECTORS && !firstFigIsImage) {
		alert('Figure 1 of a draw-vectors question must be an image.');
		return false;
	}
	var stmnt = trimWhiteSpaces(document.questionForm.
			statement.value),
		book,
		chapter,
		bookQNumber,
		keywords;
	document.actionForm.statement.value = stmnt;
	book = trimWhiteSpaces(document.questionForm.book.value);
	if (book === '') {
		book = 'Other';
	}
	document.actionForm.book.value = book;
	chapter = trimWhiteSpaces(document.questionForm.chapter.value);
	if (!['Other', 'Literature'].contains(book) && !canParseToInt(chapter)) {
		alert('Illegal chapter title; deleting.');
		chapter = '0';
	}
	document.actionForm.chapter.value = chapter;
	bookQNumber = trimWhiteSpaces(document.questionForm.bookQNumber.value);
	if (!bookQNumber) {
		bookQNumber = '';
	}
	document.actionForm.bookQNumber.value = bookQNumber;
	if (document.questionForm.keywords) {
		keywords = trimWhiteSpaces(document.questionForm.keywords.value);
		if (!keywords) {
			keywords = '';
		}
		document.actionForm.keywords.value = keywords;
	}
	return true;
} // setFormValues()

function addContinue() {
	"use strict";
	var success = setFormValues();
	if (success) {
		document.actionForm.action.value = 'add_continue';
		document.actionForm.submit();
	}
}

function saveAndClone(qId) {
	"use strict";
	var success = setFormValues();
	if (success) {
		document.actionForm.action.value =
				(qId === 0 ? 'add' : 'save') + '_clone';
		document.actionForm.submit();
	}
}

function saveChanges() {
	"use strict";
	var success = setFormValues();
	if (success) {
		convertFields();
		document.actionForm.action.value = 'save';
		document.actionForm.submit();
	}
} // saveChanges

function saveContinue() {
	"use strict";
	var success = setFormValues();
	if (success) {
		document.actionForm.action.value = 'add_addnew';
		document.actionForm.submit();
	}
}

function saveExit() {
	"use strict";
	var success = setFormValues();
	if (success) {
		convertFields();
		document.actionForm.action.value = 'save_return';
		document.actionForm.submit();
	}
} // saveExit

function saveReturn() {
	"use strict";
	var success = setFormValues();
	if (success) {
		document.actionForm.action.value = 'add_return';
		document.actionForm.submit();
	}
}

function setNoChemFormatting(qFlags) {
	"use strict";
	makeCheckBox('noChemFormatting',
			(qFlags & NO_CHEM_FORMATTING) !== 0,
			'eschew chemistry formatting of text', 'qProperties3');
} // setNoChemFormatting()

function setSourceTags() {
	"use strict";
	var bookValue = getValue('booktag');
	if (bookValue === 'Literature') {
		setInnerHTML('chaptertag', 'Reference');
		setInnerHTML('bookQNumberTag', 'URL');
	} else {
		setInnerHTML('chaptertag', bookValue === 'Other' ? 'Author' : 'Chapter');
		setInnerHTML('bookQNumberTag', 'Number');
	} // if bookValue
} // setSourceTags()

function setUniversalDataButtons(buttonsStr) {
	"use strict";
	setInnerHTML('editUniversalData', buttonsStr);
} // setUniversalDataButtons()

function setVectorsOrFigure(word) {
	"use strict";
	setInnerHTML('vectorsOrFigure', word);
} // setVectorsOrFigure()

function setVisible(identifier, on) {
	"use strict";
	setVisibility(identifier, on ? 'visible' : 'hidden');
} // hideOption()

function hideOption(identifier) {
	"use strict";
	setVisible(identifier, false);
} // hideOption()

function hidePreload(form) {
	"use strict";
	hideOption('withPreload');
	if (form.preloadFig) {
		hideOption('preloadFig');
	}
	hideOption('figureSelector');
} // hidePreload()

function setExceptLastVisibility() {
	"use strict";
	var form = document.questionForm,
		scrambleChecked = form.scramble && form.scramble.checked;
	setVisible('exceptLastCell', scrambleChecked);
	if (!scrambleChecked) {
		uncheckCell('exceptLastCell');
	}
} // setExceptLastVisibility()

function setWithRGroups(qFlags) {
	"use strict";
	makeCheckBox('useSubstns',
			(qFlags & USES_SUBSTNS) !== 0,
			'uses R groups', 'qProperties1',
			'if (this.checked) rewriteQdata(SUBSTNS);' +
				' else collapseQdata(SUBSTNS);');
} // setWithRGroups()

function showOption(identifier) {
	"use strict";
	setVisible(identifier, true);
} // showOption()

function setDisallowSuperfluousWedges(qFlags) {
	"use strict";
	makeCheckBox('disallowSuperfluousWedges',
			(qFlags & DISALLOW_SUPERFLUOUS_WEDGES) !== 0,
			'disallow unnecessary wedge bonds', 'qProperties4');
	showOption('disallowSuperfluousWedges');
} // setDisallowSuperfluousWedges()

function setHowShowHydrogens(form, qFlags) {
	"use strict";
	showOption('howShowHydrogens');
	setFormBoxChecked(form.howShowHydrogens,
			(qFlags & IMPLICITHMASK) !== 0 || (qFlags & SHOWALLC) !== 0);
} // setHowShowHydrogens()

function setThreeDim(form, qFlags) {
	"use strict";
	showOption('threeDim');
	setFormBoxChecked(form.threeDim, (qFlags & THREEDIM) !== 0);
} // setThreeDim()

function setWithLonePairs(form, qFlags) {
	"use strict";
	showOption('withLonePairs');
	setFormBoxChecked(form.withLonePairs, (qFlags & SHOWLONEPAIRS) !== 0);
} // setWithLonePairs()

function setWithMapping(form, qFlags) {
	"use strict";
	showOption('withMapping');
	setFormBoxChecked(form.withMapping, (qFlags & SHOWMAPPING) !== 0);
} // setWithMapping()

function setWithPreload(form, qFlags) {
	"use strict";
	showOption('withPreload');
	setFormBoxChecked(form.withPreload, (qFlags & PRELOAD) !== 0);
	if (form.preloadFig) {
		if (form.withPreload.checked) {
			showOption('preloadFig');
		} else {
			hideOption('preloadFig');
		}
	}
	showOption('figureSelector');
} // setWithPreload()

function setWithRSLabels(form, qFlags) {
	"use strict";
	showOption('withRSLabels');
	setFormBoxChecked(form.withRSLabels, (qFlags & SHOWRSLABELS) !== 0);
} // setWithRSLabels()

function setupOptions() {
	"use strict";
	// Applet-initialization parameters are question-type specific.
	var qType = getMajorQType(),
		qFlags = calculateQFlags(),
		form = document.questionForm;
	setFormBoxChecked(form.hideFromOthers, (qFlags & HIDE) !== 0);
	switch (qType) {
	case MARVIN:
		setAppletName('Marvin');
		setWithLonePairs(form, qFlags);
		setHowShowHydrogens(form, qFlags);
		setWithRSLabels(form, qFlags);
		setWithMapping(form, qFlags);
		setThreeDim(form, qFlags);
		setDisallowSuperfluousWedges(qFlags);
		setWithPreload(form, qFlags);
		setWithRGroups(qFlags);
		makeCheckBox('badValenceInvisible',
				(qFlags & BADVALENCEINVISIBLE) !== 0,
				'don\'t highlight valence errors', 'qProperties2');
		showOption('qPropTitle');
		setVectorsOrFigure('figure');
		collapseQdata(GENERAL);
		if (isChecked(form.useSubstns)) {
			rewriteQdata(SUBSTNS);
		} else {
			collapseQdata(SUBSTNS);
		}
		break;
	case MECHANISM:
		setWithLonePairs(form, qFlags);
		setWithMapping(form, qFlags);
		setHowShowHydrogens(form, qFlags);
		setWithRSLabels(form, qFlags);
		setDisallowSuperfluousWedges(qFlags);
		setWithPreload(form, qFlags);
		setWithRGroups(qFlags);
		hideOption('disallowSuperfluousWedges');
		showOption('qPropTitle');
		hideOption('threeDim');
		clearInnerHTML('qProperties2');
		setVectorsOrFigure('figure');
		setAppletName('Marvin');
		collapseQdata(GENERAL);
		if (isChecked(form.useSubstns)) {
			rewriteQdata(SUBSTNS);
		} else {
			collapseQdata(SUBSTNS);
		}
		break;
	case SYNTHESIS:
		setWithLonePairs(form, qFlags);
		setWithMapping(form, qFlags);
		setHowShowHydrogens(form, qFlags);
		setWithRSLabels(form, qFlags);
		setDisallowSuperfluousWedges(qFlags);
		setWithPreload(form, qFlags);
		setWithRGroups(qFlags);
		hideOption('disallowSuperfluousWedges');
		showOption('qPropTitle');
		hideOption('threeDim');
		clearInnerHTML('qProperties2');
		clearInnerHTML('qProperties3');
		setVectorsOrFigure('figure');
		setAppletName('Marvin');
		rewriteQdata(GENERAL);
		if (isChecked(form.useSubstns)) {
			rewriteQdata(SUBSTNS);
		} else {
			collapseQdata(SUBSTNS);
		}
		break;
	case LEWIS:
		setWithLonePairs(form, qFlags);
		setWithMapping(form, qFlags);
		setHowShowHydrogens(form, qFlags);
		setWithRSLabels(form, qFlags);
		setWithPreload(form, qFlags);
		hideOption('threeDim');
		hideOption('qPropTitle');
		clearInnerHTML('qProperties1');
		clearInnerHTML('qProperties2');
		clearInnerHTML('qProperties3');
		clearInnerHTML('qProperties4');
		setVectorsOrFigure('figure');
		setAppletName('Figure');
		collapseQdata(GENERAL);
		collapseQdata(SUBSTNS);
		break;
	case CHOICE:
	case CHOOSE_EXPLAIN:
	case RANK:
	case FILLBLANK:
		setNoChemFormatting(qFlags);
		setWithLonePairs(form, qFlags);
		setWithMapping(form, qFlags);
		setHowShowHydrogens(form, qFlags);
		setWithRSLabels(form, qFlags);
		hideOption('threeDim');
		hideOption('qProperties4');
		hidePreload(form);
		setAppletName('Figure');
		rewriteQdata(GENERAL);
		collapseQdata(SUBSTNS);
		switch (qType) {
		case CHOICE:
		case CHOOSE_EXPLAIN:
			showOption('qPropTitle');
			makeCheckBox('disallowMult',
					(qFlags & DISALLOW_MULT) !== 0,
					'disallow multiple responses', 'qProperties1');
			setVectorsOrFigure('figure');
			break;
		case RANK:
			showOption('qPropTitle');
			makeCheckBox('allowUnranked',
					(qFlags & ALLOW_UNRANKED) !== 0,
					'allow unnumbered items', 'qProperties1');
			setVectorsOrFigure('figure');
			break;
		default: // FILL_BLANK
			hideOption('qPropTitle');
			clearInnerHTML('qProperties1');
			setVectorsOrFigure('figure');
		} // switch
		makeCheckBox('scramble',
				(qFlags & SCRAMBLE) !== 0,
				'scramble options <span name="exceptLastCell" ' +
					'id="exceptLastCell"><\/span>',
				'qProperties2',
				'setExceptLastVisibility();');
		makeCheckBox('exceptLast',
				(qFlags & EXCEPT_LAST) !== 0,
				'except last', 'exceptLastCell');
		setExceptLastVisibility();
		clearInnerHTML('qProperties4');
		break;
	case NUMERIC:
	case ORB_E_DIAGRAM:
	case RXN_COORD:
	case TABLE:
	case TEXT:
	case FORMULA:
	case LOGIC:
		setWithLonePairs(form, qFlags);
		setWithMapping(form, qFlags);
		setHowShowHydrogens(form, qFlags);
		setWithRSLabels(form, qFlags);
		hidePreload(form);
		hideOption('threeDim');
		setAppletName('Figure');
		switch (qType) {
		case NUMERIC:
			rewriteQdata(GENERAL);
			if (isChecked(form.useSubstns)) {
				rewriteQdata(SUBSTNS);
			} else {
				collapseQdata(SUBSTNS);
			}
			setNoChemFormatting(qFlags);
			showOption('qPropTitle');
			makeCheckBox('useSubstns',
					(qFlags & USES_SUBSTNS) !== 0,
					'substitutes values for variables', 'qProperties1',
					'if (this.checked) rewriteQdata(SUBSTNS);' +
						' else collapseQdata(SUBSTNS);');
			makeCheckBox('useSciNotn',
					(qFlags & USE_SCI_NOTN) !== 0,
					'use scientific notation', 'qProperties4');
			makeCheckBox('requireInt',
					(qFlags & REQUIRE_INT) !== 0,
					'integral responses only', 'qProperties2');
			setVectorsOrFigure('figure');
			break;
		case TABLE:
			rewriteQdata(GENERAL);
			collapseQdata(SUBSTNS);
			setNoChemFormatting(qFlags);
			showOption('qPropTitle');
			makeCheckBox('numsOnly',
					(qFlags & NUMS_ONLY) !== 0,
					'numerical entries only', 'qProperties1');
			makeCheckBox('requireInt',
					(qFlags & REQUIRE_INT) !== 0,
					'integral responses only', 'qProperties2');
			clearInnerHTML('qProperties4');
			setVectorsOrFigure('figure');
			break;
		case ORB_E_DIAGRAM:
			rewriteQdata(GENERAL);
			collapseQdata(SUBSTNS);
			showOption('qPropTitle');
			makeCheckBox('labelOrbs',
					(qFlags & LABEL_ORBS) !== 0,
					'enable orbital labeling', 'qProperties1');
			clearInnerHTML('qProperties2');
			clearInnerHTML('qProperties3');
			clearInnerHTML('qProperties4');
			setVectorsOrFigure('figure');
			break;
		case RXN_COORD:
		case LOGIC:
			rewriteQdata(GENERAL);
			collapseQdata(SUBSTNS);
			if (qType === LOGIC) {
				setNoChemFormatting(qFlags);
			} else {
				clearInnerHTML('qProperties3');
			}
			hideOption('qPropTitle');
			clearInnerHTML('qProperties1');
			clearInnerHTML('qProperties2');
			clearInnerHTML('qProperties4');
			setVectorsOrFigure('figure');
			break;
		default: // TEXT, FORMULA
			collapseQdata(GENERAL);
			collapseQdata(SUBSTNS);
			if (qType !== FORMULA) {
				setNoChemFormatting(qFlags);
			}
			hideOption('qPropTitle');
			clearInnerHTML('qProperties1');
			clearInnerHTML('qProperties2');
			clearInnerHTML('qProperties4');
			setVectorsOrFigure('figure');
			break;
		} // switch(qType)
		break;
	case CLICK_IMAGE:
	case DRAW_VECTORS:
	case EQUATIONS:
		if (qType === CLICK_IMAGE) {
			setNoChemFormatting(qFlags);
		} else {
			clearInnerHTML('qProperties3');
		}
		if (qType === EQUATIONS) {
			makeCheckBox('omitConstantsField',
					omitConstantsField(qFlags),
					'omit constants field', 'qProperties4',
					'if (this.checked) ' +
						'setInnerHTML(\'constantsLabel\', \'\'); ' +
						'else setInnerHTML(\'constantsLabel\', ' +
						'\' constants and\');');
			showOption('omitConstantsField');
		} else {
			clearInnerHTML('qProperties4');
		}
		if (qType === DRAW_VECTORS) {
			setWithPreload(form, qFlags);
			setVectorsOrFigure('vectors');
		} else {
			hidePreload(form);
		}
		hideOption('threeDim');
		hideOption('withLonePairs');
		hideOption('withMapping');
		hideOption('howShowHydrogens');
		hideOption('withRSLabels');
		hideOption('qPropTitle');
		rewriteQdata(GENERAL);
		collapseQdata(SUBSTNS);
		clearInnerHTML('usesApplet');
		clearInnerHTML('qProperties1');
		clearInnerHTML('qProperties2');
		break;
	case OTHER:
		setThreeDim(form, qFlags);
		setWithLonePairs(form, qFlags);
		setWithMapping(form, qFlags);
		setDisallowSuperfluousWedges(qFlags);
		setNoChemFormatting(qFlags);
		setHowShowHydrogens(form, qFlags);
		setWithRSLabels(form, qFlags);
		setWithPreload(form, qFlags);
		setWithRGroups(qFlags);
		setFormBoxChecked(form.badValenceInvisible,
				(qFlags & BADVALENCEINVISIBLE) !== 0);
		showOption('qPropTitle');
		setAppletName('Marvin');
		clearInnerHTML('qProperties2');
		rewriteQdata(GENERAL);
		rewriteQdata(SUBSTNS);
		break;
	default:
		alert('cannot match Qtype to ' + qFlags);
	} // switch qType
	setInnerHTML('statementAdvice', qType === FILLBLANK ?
				'<br/>pulldown menu syntax: [[1, 2, 5]]'  +
					' (numbers correspond to menu options)' :
			qType === NUMERIC && (qFlags & USES_SUBSTNS) !== 0 ?
				'<br/>enter variables as [[x1]], [[x2]], etc.' :
			'');
	setSourceTags();
} // setupOptions()

function splitEval() {
	"use strict";
	var c,
		all_checkbox = document.questionForm.all_checker,
		length = all_checkbox.length || 1,
		numAll = 0,
		evalNums,
		go,
		destination;
	if (length === 1) {
		if (all_checkbox.selected || all_checkbox.checked) {
			numAll += 1;
		}
	} else {
		for (c = 0; c < length; c += 1) {
			if (all_checkbox[c].selected || all_checkbox[c].checked) {
				numAll += 1;
			}
		} // length !== 1
	}
	if (numAll > 1) {
		alert('You must select only one item to split');
		return;
	}
	if (numAll < 1) {
		alert('You must select at least one item to split');
		return;
	}
	evalNums = getSelectedValues(all_checkbox); // only one selected
	go = new String.builder()
			.append('reentryActions.jsp?action=split&evalNum=')
			.append(evalNums[0]);
	destination = go.toString();
	setEditables(destination, SUBMIT_FORM);
} // splitEval()

// inserts Lewis figure or updates Marvin figure
function updateFigure(pathToRoot) {
	"use strict";
	if (numFigures >= 1 && shownFigureIsMolecule()) {
		var qFlags = calculateQFlags();
		// if (figIsLewis() || !shownFigureIsFromMarvinJS()) {
			loadLewisInlineImages(pathToRoot,
					[[getDisplayMol(), qFlags]], ADD_CLICK);
		/* } else {
			var dims = getBestDimensions();
			displayImage(pathToRoot,
					getOrigMol(),
					{ flags: qFlags,
					imageId: '1',
					width: dims[0],
					height: dims[1],
					figCellPrefix: 'figJS',
					prefersPNG: getPrefersPNG() }); 
		} // if is a Lewis figure
		/**/
	} // if the shown figure is a molecule
} // updateFigure()

function usesSubstns() {
	"use strict";
	return ((calculateQFlags() & USES_SUBSTNS) !== 0);
} // usesSubstns()

function viewBadSMs(masterEdit) {
	"use strict";
	var go = 'universalData/viewBadSMs.jsp?masterEdit=' + masterEdit;
	// alert(go);
	openEvaluatorWindow(go);
} // viewBadSMs()

function viewCanonicalizedUnits(masterEdit) {
	"use strict";
	var go = 'universalData/viewCanonicalizedUnits.jsp?masterEdit=' + masterEdit;
	// alert(go);
	openEvaluatorWindow(go);
} // viewCanonicalizedUnits()

function viewFnalGroups(masterEdit) {
	"use strict";
	var go = 'universalData/viewFnalGroups.jsp?masterEdit=' + masterEdit;
	// alert(go);
	openEvaluatorWindow(go);
} // viewFnalGroups()

function viewMenuOnlyRgts(masterEdit) {
	"use strict";
	var go = 'universalData/viewMenuOnlyRgts.jsp?masterEdit=' + masterEdit;
	// alert(go);
	openEvaluatorWindow(go);
} // viewMenuOnlyRgts()

function viewPreloadTable() {
	"use strict";
	var go = 'showTable.jsp';
	// alert(go);
	openMolShowWindow(go);
} // viewPreloadTable()

function viewReactions(masterEdit) {
	"use strict";
	var go = 'universalData/viewReactions.jsp?masterEdit=' + masterEdit;
	// alert(go);
	openEvaluatorWindow(go);
} // viewReactions()

function viewResponses() {
	"use strict";
	var success = setFormValues();
	if (success) {
		document.actionForm.action.value = 'view_responses';
		document.actionForm.submit();
	}
} // viewResponses

function viewUnitConversions(masterEdit) {
	"use strict";
	var go = 'universalData/viewUnitConversions.jsp?masterEdit=' + masterEdit;
	// alert(go);
	openEvaluatorWindow(go);
} // viewUnitConversions()

// --> end HTML comment
