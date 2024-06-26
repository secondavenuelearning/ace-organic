// <!-- avoid parsing the following as HTML <!-- >
	// initialize values of interest
	var submissionValue = '<%= Utils.toValidJS(isTable ? tableDisp : lastResp) %>';
	var feedbackValue = '<%= Utils.toValidJS(feedback) %>';
	var statusValue = '<%= Utils.toValidJS(status) %>';
	var status2Value = '<%= Utils.toValidJS(status2) %>';
	var gradingAlertValue = '<%= Utils.toValidJS(gradingAlert) %>';
	var disallowSubmitValue = <%= disallowSubmit %>;
	var dueDatePastValue = <%= dueDatePast %>;
	var evalStatusValue = '<%= evalResult == null ? NO_STATUS : evalResult.status %>';
	var gradeValue = <%= evalResult == null ? 0 : evalResult.grade %>;
	var triesValue = <%= evalResult == null ? 0 : evalResult.tries %>;

	<% final String submitMe = "submitButtonClicked(true);"; %>
	var submitButtonCode = 
			'<%= mode == VIEW || shouldntHaveResponded || disallowSubmit
				? Utils.toString("<table align=\"center\"><tr>",
					"<td class=\"boldtext\">", 
					user.translateJS("View Mode"), 
					"<\\/td><td style=\"padding-left:20px;\">",
					Utils.toValidJS(makeButton(
						user.translateJS("Enter Practice Mode"), 
						"enterPracticeMode();")),
					"<\\/td><\\/tr><\\/table>")
			: mode == PREVIEW 
				? Utils.toValidJS(makeButton("Submit Response (Preview)", submitMe)) 
			: mode == SIMILAR 
				? Utils.toValidJS(makeButton(
					user.translate("Submit Response (Practice similar)"), submitMe)) 
			: mode == PRACTICE || (mode == GRADEBOOK_VIEW 
					&& (!disallowSubmit || isInstructorOrTA))
				? Utils.toValidJS(makeButton(
					user.translate("Submit Response (Practice)"), submitMe)) 
			: Utils.toValidJS(makeButton(user.translate("Submit Response"), submitMe))
			%>';

	<% if (hwsession.isExam()) { %>
		<%@ include file="/js/setUpClock.jsp.h" %>
	<% } // if is an exam 
	if (isClickableImage || isDrawVectors) { %>
 		<%@ include file="/js/drawOnFig.jsp.h" %>
	<% } else if (isEquations) { %>
 		<%@ include file="/js/equations.jsp.h" %>
	<% } // if question type %>

	function getButtons() {
		"use strict";
		var buttons = new String.builder();
		buttons.append('<p>&nbsp;<\/p>');
		if (!disallowSubmitValue || <%= Utils.among(mode, VIEW, SIMILAR, GRADEBOOK_VIEW) %>) {
			buttons.append('<table '
						+ 'style="margin-left:auto; margin-right:auto;">').
					append('<tr><td style="padding-right:10px;">').
					append(disallowSubmitValue && <%= mode == SIMILAR %>
						? '<%= Utils.toValidJS(makeButton(
							user.translate("Practice another"), 
								"practiceAgain();")) %>'
						: submitButtonCode);
			buttons.append('<\/td>');
			if (evalStatusValue !== '<%= EVALUATED %>' 
					&& evalStatusValue !== '<%= HUMAN_NEEDED %>' 
					&& <%= mode == SOLVE && hwsession.showSaveWOSubmitting() %>) {
				buttons.append('<td style="text-align:center; padding-left:10px;"><%= 
						Utils.toValidJS(makeButton(
							user.translate("Save without submitting"), 
							"submitButtonClicked(false);")) %><\/td>');
			} 
			<% if (preload && (isLewis || usesChemAxon) && mode != VIEW) { %>
				buttons.append('<td style="text-align:center; padding-left:10px;"><%= 
						Utils.toValidJS(makeButton(user.translate("Reset figure"), 
							"resetFigure();")) %><\/td>');
			<% } // if preload %>
			if (<%= mode == SOLVE && isMasteryAssgt %>) {
				if (triesValue > <%= maxTries %> 
						|| (triesValue === <%= maxTries %> && gradeValue < 1.0)) { // <!-- >
					buttons.append('<td style="text-align:center; padding-left:10px;"><%= 
							Utils.toValidJS(makeButton(user.translate("Solve related"), 
								"solveRelated();")) %><\/td>');
				} /* else alert('triesValue = ' + triesValue + ', maxTries = <%= 
						maxTries %>, gradeValue = ' + gradeValue); */
			} // if a mastery question
			buttons.append('<\/tr><\/table>');
		}
		setInnerHTML('submitCell', buttons.toString());
	} // getButtons()

	function setNavigation() {
		"use strict";
		<% if (currentQNum <= 1 || !hwsession.getOkToDisplay(currentQNum - 1)) { %>
			hideCell('previousCell');
		<% } // if OK to display Previous arrow
		if (!okToDisplayNext) { %>
			hideCell('nextCell');
		<% } // if OK to display Next arrow %>
	} // setNavigation()

// ****** submission functions

	function submitButtonClicked(evaluate) {
		"use strict";
		if (disallowSubmitValue) {
			toAlert('<%= Utils.toValidJS(ansPhrases.get(CANT_SUBMIT)) %>');
		} else { 
			callServer(evaluate);
		} // if disallowSubmitValue
	} // submitButtonClicked()

	function setStatusFeedback() {
		"use strict";
		if (!isEmpty(statusValue)) {
			setInnerHTML('statusText', statusValue);
			setInnerHTML('status2Text', status2Value);
			setInnerHTML('feedbackText', feedbackValue);
		} else if (<%= !Utils.among(mode, VIEW, GRADEBOOK_VIEW) %>) {
			setInnerHTML('statusText', 
					'<%= Utils.toValidJS(ansPhrases.get(PLEASE_SUBMIT)) %>');
			<% if (isMechanism || isSynthesis) { %>
				setInnerHTML('status2Text', 
						'<%= Utils.toValidJS(Utils.toString(ansPhrases.get(MORE_ROOM1), 
							' ', ansPhrases.get(MORE_ROOM2), "<br/><br/>")) %>');
			<% } // mechanism or synthesis %>
		} else if (<%= mode == VIEW %>) {
			setInnerHTML('statusText', 
					'<%= Utils.toValidJS(ansPhrases.get(NO_RESP)) %>');
		} // if statusValue or mode
	} // setStatusFeedback()

// ****** AJAX functions

	// Values of interest.  Follows ACE conventions: 
	//		valueName = @@@@...
	// Whitespace around = sign must be spaces
	//	molValue = @@@@<%= Utils.lineBreaksToJS(isTable ? tableDisp : lastResp) %>@@@@
	//	selectionsStrValue = @@@@<%= selectionsStr %>@@@@
	//	feedbackValue = @@@@<%= Utils.lineBreaksToJS(feedback) %>@@@@
	//	statusValue = @@@@<%= Utils.lineBreaksToJS(status) %>@@@@
	//	status2Value = @@@@<%= Utils.lineBreaksToJS(status2) %>@@@@
	//	gradingAlertValue = @@@@<%= Utils.lineBreaksToJS(gradingAlert) %>@@@@
	//	statusColorValue = @@@@<%= statusColor %>@@@@
	//	disallowSubmitValue = @@@@<%= disallowSubmit %>@@@@
	//	dueDatePastValue = @@@@<%= dueDatePast %>@@@@
	//  evalStatusValue = @@@@<%= evalResult == null ? '0' : evalResult.status %>@@@@
	//  triesValue = @@@@<%= evalResult == null ? 0 : evalResult.tries %>@@@@
	//  gradeValue = @@@@<%= evalResult == null ? 0 : evalResult.grade %>@@@@
	//	okToDisplayNextValue = @@@@<%= okToDisplayNext %>@@@@

	function callServer(evaluate, modifySubmission) {
		"use strict";
		<% if (usesChemAxon) { %>
			marvinSketcherInstances['<%= APPLET_NAME %>'].exportStructure('<%= 
					MRV_EXPORT %>').then(function(source) {
				if (marvinSketcherInstances['<%= APPLET_NAME %>'].getStructureInfo
						&& marvinSketcherInstances['<%= APPLET_NAME %>'].
							getStructureInfo().atomCount === 0) {
					toAlert('<%= Utils.toValidJS(ansPhrases.get(EMPTY_RESP)) %>');
					return;
				} // if response is empty
				// method closed below after call to AJAX
		<% } // if usesChemAxon %> 
		if (navigator.appName.toUpperCase().match(/NETSCAPE/)) { // breaks on Internet Explorer
			if (getSelection().removeAllRanges) { // must check availability for Safari
				getSelection().removeAllRanges(); // unselect for Firefox
			} // if can remove all ranges
		} // if browser
		var submissionBld = new String.builder();
		var selections = '';
		var rxnIds = null;
		<% if (usesChemAxon) { %>
			var molstruct = source;
			if (molstruct.indexOf('mrvAlias') >= 0 && <%= !is3D %>) {
				toAlert('<%= Utils.toValidJS(ansPhrases.get(ALIAS)) %>');
				return;
			} // if response contains an alias
			if (molstruct.indexOf('z3=') > 0
					&& (molstruct.indexOf('<bondStereo>W') > 0
						|| molstruct.indexOf('<bondStereo>H') > 0)) {
				if (!toConfirm('<%= Utils.toValidJS(ansPhrases.get(STEREOBOND)) %>')) {
					toAlert('<%= Utils.toValidJS(ansPhrases.get(CONVERT2D)) %>');
					return;
				} // if no confirm
			} // if response is in 3D and has wedge bonds 
			if (molstruct.indexOf('elementType=\"Ac\"') > 0) {
				if (!toConfirm('<%= Utils.toValidJS(ansPhrases.get(AC_SHORTCUT_IS_ELEMENT)) %>')) {
					return;
				} // if no confirm
			} // if response has an actinium atom 
			if (molstruct.indexOf('elementType=\"Pr\"') > 0) {
				if (!toConfirm('<%= Utils.toValidJS(ansPhrases.get(PR_SHORTCUT_IS_ELEMENT)) %>')) {
					return;
				} // if no confirm
			} // if response has a praseodymium atom 
			if (molstruct.indexOf('elementType=\"Cn\"') > 0) {
				if (!toConfirm('<%= Utils.toValidJS(ansPhrases.get(CN_SHORTCUT_IS_ELEMENT)) %>')) {
					return;
				} // if no confirm
			} // if response has a copernicium atom 
			if (molstruct.indexOf('elementType=\"Ts\"') > 0) {
				if (!toConfirm('<%= Utils.toValidJS(ansPhrases.get(TS_SHORTCUT_IS_ELEMENT)) %>')) {
					return;
				} // if no confirm
			} // if response has a tennessine atom 
			<% if (isMarvin || isMechanism) { %>
				marvinSketcherInstances['<%= APPLET_NAME %>'].
						getSelection().then(function(selection) { 
					selections = selection.atoms;
					// method closed below after call to AJAX
			<% } // if Marvin or mechanism %>
			submissionBld.append(molstruct);
		<% } else if (isLewis) { %> 
			submissionBld.append(getLewisMRV());
		<% } else if ((isChoice || isChooseExplain) && qData != null) { %> 
			// response is in the form 3+:1:2:5:4+: where options 3 and 4 were
			// chosen and the order of numbers is the order of presentation
			<% for (int optNum = 1; optNum <= qData.length; optNum += 1) { 
				final String option = Utils.toString("document.getElement",
						disallowMultipleResponses
 							? Utils.getBuilder(
								"sByName('options')[", optNum - 1, ']')
 							: Utils.getBuilder(
								"ById('option", optNum, "')")); %>
				submissionBld.append(<%= option %>.value); 
				if (<%= option %>.checked) submissionBld.append('<%= Choice.CHOSEN %>');
				submissionBld.append('<%= Choice.SEPARATOR %>');
			<% } // for each optNum
			if (isChooseExplain) { %>
				var resp = getValue('textResp');
				if (isWhiteSpace(resp)) {
					toAlert('<%= Utils.toValidJS(ansPhrases.get(NO_TEXT)) %>');
					return;
				}
				submissionBld.append('<%= ChooseExplain.SEPARATOR %>').
						append(resp);
			<% } // if is chooseExplain %>
		<% } else if (isFillBlank && qData != null) { %>
			// response is in the form 5:2:3+:4+:1: 
			// In this example, the response chooses options 3 and 4, 
			// and the options were presented in order 5, 2, 3, 4, 1.
			var numMenus = getValue('<%= Choice.NUM_MENUS %>');
			var menuNum;
			for (menuNum = 1; menuNum <= numMenus; menuNum += 1) {  // <!-- >
				if (getCell('<%= Choice.BLANK %>' + menuNum).selected) {
					toAlert('<%= Utils.toValidJS(ansPhrases.get(PULLDOWN)) %>');
					return;
				}
			} // for each pulldown menu
			<% final Choice choiceObj = new Choice(lastResp);
			final int[] orderedOpts = choiceObj.getAllOptions();
			for (final int optNum : orderedOpts) { // 1-based %>
				submissionBld.append('<%= optNum %>'); 
				var menuItem = getCell('<%= Choice.MENU_ITEM %><%= optNum %>');
				if (menuItem && menuItem.selected) {
					submissionBld.append('<%= Choice.CHOSEN %>');
				} // if selected
				submissionBld.append('<%= Choice.SEPARATOR %>');
			<% } // for each 1-based option %>
		<% } else if (isRank && qData != null) { %>
			// response is in the form 5:1;2:3;3:4;4:5;1:2;  
			// In this example, the options were presented in order 5, 2, 3, 4, 1, 
			// and they were numbered 1, 3, 4, 5, and 2.
			var majorSep = '<%= Rank.MAJOR_SEP %>';
			var minorSep = '<%= Rank.MINOR_SEP %>';
			var ranksChosen = majorSep;
			var ranksArray = new Array();
			var rankNotIndicated = false;
			var itemRanks = submissionValue.split(majorSep); 
			var sameRanks = false;
			var itemNum;
			var qdNum;
	 		for (itemNum = 1; itemNum <= <%= qData.length %>; itemNum += 1) { 
				var itemRank = itemRanks[itemNum - 1].split(minorSep);
				var thisQDNum = parseInt(itemRank[0]); 
				var newRank = getValue('qdValue' + itemNum);
				if (newRank !== '0') {
					if (ranksChosen.indexOf(majorSep + newRank + majorSep) >= 0) {
						toAlert('<%= Utils.toValidJS(ansPhrases.get(SAME_RANK)) %>');
						return;
					} else {
						var numRanks = ranksArray.length;
						ranksArray[numRanks] = parseInt(newRank);
					}
				<% if (!allowUnranked) { %>
					} else {
						toAlert('<%= Utils.toValidJS(ansPhrases.get(RANK_EVERY)) %>');
						return;
				<% } %>
				} // if optValue is not 0
				ranksChosen += newRank + majorSep;
				submissionBld.append(thisQDNum).append(minorSep).
						append(newRank).append(majorSep);
	 		} // for each option index itemNum 
			ranksArray.sort(function(a, b) { return a - b } );
			if (ranksArray[0] !== 1) {
				toAlert('<%= Utils.toValidJS(ansPhrases.get(RANK_1)) %>');
				return;
			}
			for (qdNum = 1; qdNum < ranksArray.length; qdNum += 1) {
				if (ranksArray[qdNum] - ranksArray[qdNum - 1] !== 1) {
					toAlert('<%= Utils.toValidJS(ansPhrases.get(RANK_CONSEC)) %>');
					return;
				}
			} // for each rank
		<% } else if (isNumeric) { %>
			var number = getValue('numericResponse');
			<% if (requireInt) { %>
				if (isWhiteSpace(number) || !canParseToInt(number)) {
					toAlert('<%= Utils.toValidJS(ansPhrases.get(ENTER_INT)) %>');
					return;
				}
			<% } else { %>
				if (isWhiteSpace(number) || !canParseToFloat(number)) {
					toAlert('<%= Utils.toValidJS(ansPhrases.get(ENTER_NUM)) %>');
					return;
				}
			<% } // if integral response required %>
			submissionBld.append(number);
			<% if (useSciNotn) { %>
				var exp = getValue('exponent');
				if (isWhiteSpace(exp)) {
					toAlert('<%= Utils.toValidJS(ansPhrases.get(ENTER_EXPON)) %>');
					return;
				} else if (!canParseToInt(exp)) {
					toAlert('<%= Utils.toValidJS(ansPhrases.get(INTEGRAL)) %>');
					return;
				}
				submissionBld.append('<%= Numeric.COEFF_EXP_SEP %>').
						append(exp);
			<% } // if there's an exponent %>
			if (cellExists('unit')) {
				var unit = getValue('unit');
				submissionBld.append('<%= Numeric.NUM_UNIT_SEP_JS %>').
						append(unit);
			} // if there's a choice of units
		<% } else if (isText || isFormula) { %>
			var resp = getValue('textResp');
			if (isWhiteSpace(resp)) {
				toAlert('<%= Utils.toValidJS(ansPhrases.get(NO_TEXT)) %>');
				return;
			}
			submissionBld.append(trim(resp));
		<% } else if (isLogicalStmts) { %>
			var respXML = getStmtsXML();
			if (isEmpty(respXML)) { return;
			} else { submissionBld.append(respXML); }
		<% } else if (isEquations) { %>
			var respXML = getEqnsXML();
			if (isEmpty(respXML)) { return;
			} else { submissionBld.append(respXML); }
		<% } else if (isClickableImage) { %>
			if (drawonfig.allShapes[drawonfig.MARK].length === 0) {
				toAlert('<%= Utils.toValidJS(ansPhrases.get(NO_POINT)) %>');
				return;
			} 
			submissionBld.append(getMarksXML());
		<% } else if (isDrawVectors) { %>
			if (drawonfig.allShapes[drawonfig.ARROW].length === 0) {
				toAlert('<%= Utils.toValidJS(ansPhrases.get(NO_ARROW)) %>');
				return;
			} 
			submissionBld.append(getVectorsXML());
		<% } else if (isOED || isRCD) { %>
			var numRows = parseInt(getValue('numRows'));
			var numCols = parseInt(getValue('numCols'));
			submissionBld.append(encode<%= isOED ? "OED" : "RCD" %>(numRows, numCols));
		<% } else if (isTable) { %>
			var numRows = parseInt(getValue('<%= TableQ.NUM_ROWS_TAG %>'));
			var numCols = parseInt(getValue('<%= TableQ.NUM_COLS_TAG %>'));
			var rNum, cNum;
			submissionBld.append('<%= TableQ.NUM_ROWS_TAG %>=').
					append(numRows).append('&<%= TableQ.NUM_COLS_TAG %>=').
					append(numCols);
			for (rNum = 1; rNum <= numRows; rNum += 1) { // <!-- >
				for (cNum = 1; cNum <= numCols; cNum += 1) { // <!-- >
					var cellIdBld = new String.builder().
							append(rNum).append('<%= TableQ.ROW_COL_SEP %>').
							append(cNum);
					var cellId = cellIdBld.toString();
					var contentsId = '<%= TableQ.CELL_ID_START %>' + cellId; 
					var contents = getValue(contentsId);
					if (!isWhiteSpace(contents)) {
						<% if (requireInt) { %>
							if (!canParseToInt(contents)) {
								toAlert('<%= Utils.toValidJS(ansPhrases.get(ENTER_INT)) %>');
								return;
							}
						<% } else if (numsOnly) { %>
							if (!canParseToFloat(contents)) {
								toAlert('<%= Utils.toValidJS(ansPhrases.get(ENTER_NUM)) %>');
								return;
							}
						<% } // if nums only %>
						submissionBld.append(getURIParam(contentsId, 
								encodeURIComponent(contents)));
					} // if the cell has contents
					var ckboxId = '<%= TableQ.CKBOX_ID_START %>' + cellId; 
					var ckbox = getCell(ckboxId);
					var disabledVal = '<%= TableQ.DISABLED_VALUE %>';
					if (ckbox && ckbox.value === disabledVal) {
						submissionBld.append(getURIParam(ckboxId, disabledVal));
					} // if the cell is disabled
				} // for each column
			} // for each row
		<% } // if question type %> 
		var submissionStr = submissionBld.toString();
		// alert('submission =\n' + submissionStr);
		if (isEmpty(submissionStr)) {
			toAlert('<%= Utils.toValidJS(ansPhrases.get(EMPTY_RESP)) %>');
			return;
		} // if response is empty string
		ajaxURIArr = [];
		<% if (isTable) { %>
			ajaxURIArr.push(['', submissionStr]);
		<% } else { %>
			if (modifySubmission) {
				submissionStr = submissionStr.replace(/></g, '>\n<'); // '<!-- >
			}
			ajaxURIArr.push(['submission', encodeURIComponent(submissionStr)]);
		<% } // if is table %>
		<% if (isSynthesis) { %> 
			rxnIds = getRxnIds();
			if (!isWhiteSpace(rxnIds)) { 
				ajaxURIArr.push(['rxnIds', rxnIds]);
			} // if there are reaction IDs to store
		<% } // if synthesis Q %>
		// evaluate is false for save without submitting
		ajaxURIArr.push(['currentQNum', <%= currentQNum %>]);
		ajaxURIArr.push(['haveNewResponse', true]);
		ajaxURIArr.push(['evaluate', evaluate]);
		if (!isEmpty(selections)) {
			ajaxURIArr.push(['selections', selections]);
		} // if selections
		setInnerHTML('submitCell', '<span class="boldtext"><%= 
				Utils.toValidJS(ansPhrases.get(PROCESSING)) %><\/span>');
		clearInnerHTML('statusText');
		clearInnerHTML('status2Text');
		clearInnerHTML('feedbackText');
		callAJAX('<%= caller %>', getAjaxURI());
		<% if (usesChemAxon) { %> 
			<% if (isMarvin || isMechanism) { %>
				}, function(error) {
					alert('Failed to acquire selected atoms: ' + error);	
				});
			<% } // if Marvin or mechanism %>
			}, function(error) {
				alert('Molecule export failed:' + error);	
			});
		<% } // if usesChemAxon %>
	} // callServer

	function updatePage() {
		"use strict";
		var paramNum, selectionsStrValue;
		if (xmlHttp.readyState === 4) { // ready to continue
			var responsePage = xmlHttp.responseText;
			// alert('responsePage:\n' + responsePage);
			<% if (mode == SOLVE) { %>
				dueDatePastValue = extractField(responsePage, 'dueDatePastValue');
				if (dueDatePastValue === 'true') {
					toAlert('<%= Utils.toValidJS(ansPhrases.get(TIME_UP)) %>');
					closeHW();
				} // if due date is past
			<% } // if mode is solve %>
			submissionValue = extractField(responsePage, 'molValue');
			// alert('value back from server:\n' + submissionValue);
			<% if (isTable) { %>
				setInnerHTML('tableDisp', submissionValue);
			<% } else if (isLewis) { %>
				parseLewisMRV(submissionValue);
			<% } else if (usesChemAxon) { %>
				marvinSketcherInstances['<%= APPLET_NAME %>'].
						importStructure('<%= Utils.MRV %>', submissionValue);
				selectionsStrValue = 
						extractField(responsePage, 'selectionsStrValue');
				if (!isEmpty(selectionsStrValue)) {
					var selectionsJSO = { 'atoms': selectionsStrValue };
					marvinSketcherInstances['<%= APPLET_NAME %>'].
							setSelection(selectionsJSO);
				} // if there are atoms to select
			<% } else { %>
				var reformattedValue = 
						extractField(responsePage, 'reformattedValue');
				if (reformattedValue === 'true' && <%= isNumeric %>) {
					setValue('numericResponse', submissionValue);
				}
			<% } // if question type %>
			feedbackValue = extractField(responsePage, 'feedbackValue');
			statusValue = extractField(responsePage, 'statusValue');
			status2Value = extractField(responsePage, 'status2Value');
			var statusColorValue = extractField(responsePage, 'statusColorValue');
			gradingAlertValue = extractField(responsePage, 'gradingAlertValue');
			evalStatusValue = extractField(responsePage, 'evalStatusValue');
			triesValue = parseInt(extractField(responsePage, 'triesValue'));
			gradeValue = parseFloat(extractField(responsePage, 'gradeValue'));
			disallowSubmitValue = <%= !previewOrTextbookMode 
						&& (mode != GRADEBOOK_VIEW || !isInstructorOrTA) %> && 
					extractField(responsePage, 'disallowSubmitValue') === 'true';
			setInnerHTML('statusText', statusValue);
			setInnerHTML('status2Text', status2Value);
			sendInnerHTML('feedbackText', feedbackValue);
			var okToDisplayNextValue = extractField(responsePage, 'okToDisplayNextValue');
			if (okToDisplayNextValue === 'true') {showCell('nextCell');}
			getButtons();
		} // if ready to continue
	} // updatePage()

	function gradeAlert() {
		"use strict";
		toAlert(gradingAlertValue);
	} // gradeAlert()

	function sendInnerHTML(element, text) {
		"use strict";
		text = text.replace(/\\r/g, '');
		text = text.replace(/\\n/g, '');
		setInnerHTML(element, text);
	} // sendInnerHTML()

// ****** navigation functions 

	<% if (Utils.among(prevMode, PRACTICE, VIEW, SIMILAR) && prevMode != mode) { %>
		toAlert('<%= Utils.toValidJS(ansPhrases.get(NOW_MODE)) %>');
	<% } %>

	function viewList() {
		"use strict";
		var bld = new String.builder().append('list.jsp?showClock=').
				append(showTimeRemaining);
		<% if (mode == SIMILAR && usesSubstns) { %>
			bld.append('&resetNum=<%= currentQNum %>');
		<% } // if exiting practice similar mode %>
		bld.append('#Q<%= qId %>');
		self.location.href = bld.toString();
	} // viewList()

	function movePrev() {
		"use strict";
		self.location.href = new String.builder().append('<%= 
				caller %>?prevMode=<%= mode %>&currentQNum=<%= 
				currentQNum - 1 %>&showClock=').append(showTimeRemaining).
				toString();
	} // movePrev()

	function moveNext() {
		"use strict";
		self.location.href = new String.builder().append('<%= 
				caller %>?prevMode=<%= mode %>&currentQNum=<%= 
				currentQNum + 1 %>&showClock=').append(showTimeRemaining).
				toString();
	} // moveNext()

	function enterPracticeMode() {
		"use strict";
		self.location.href = new String.builder().append(
				'jumpGo.jsp?mode=<%= PRACTICE %>&qNum=<%= 
				currentQNum %>&showClock=').append(showTimeRemaining).
				toString();
	} // enterPracticeMode()

	function practiceAgain() {
		"use strict";
		self.location.href = new String.builder().append(
				'jumpGo.jsp?mode=<%= SIMILAR %>&qNum=<%= 
				currentQNum %>&showClock=').append(showTimeRemaining).
				toString();
	} // practiceAgain()

	function solveRelated() {
		"use strict";
		self.location.href = new String.builder().append(
				'jumpGo.jsp?mode=<%= SIMILAR %>&qNum=<%= 
				currentQNum %>&solveRelatedMasteryQ=true&showClock=').
				append(showTimeRemaining).toString();
	} // solveRelated()

	function closeHW() {
		"use strict";
		<% for (int qNum = 1; qNum <= hwsession.getCount(); qNum += 1)  {
			final EvalResult qResult = hwsession.getResult(qNum); 
			if (qResult != null && qResult.status == SAVED) { 
				// maybe last saved response was just now submitted; wouldn't be
				// in Java record yet, but would be in Javascript 
				final String mustSubmit = (!previewOrTextbookMode
						? user.translate("You have saved a response "
							+ "for question ***5*** without submitting "
							+ "it for evaluation. Remember to submit it "
							+ "later, or you will not receive credit.", qNum)
						: Utils.toString("You have saved a response for question ", 
							qNum, " without submitting "
								+ "it for evaluation. Remember to submit it "
								+ "later, or you will not receive credit."));
				final String unsubmitted = (!previewOrTextbookMode
						? user.translate("You saved but never submitted your "
							+ "response to question ***5***.", qNum)
						: Utils.toString("You saved but never submitted your "
							+ "response to question ", qNum, '.'));
		%>
				if (<%= qNum != currentQNum %> 
						|| evalStatusValue === '<%= SAVED %>') {
					if (!dueDatePastValue) {
						toAlert('<%= Utils.toValidJS(mustSubmit) %>');
						return;
					} else {
						toAlert('<%= Utils.toValidJS(unsubmitted) %>');
					} // if time expired
				} // if response was not just now submitted
			<% } // if a response was saved but not submitted
		} // for each Q %>
		document.location.href = '<%= pathToRoot %>hwcreator/hwSetList.jsp';
	} // closeHW()

// ****** molecule-related functions

	function <%= Mechanism.OPEN_CALCD_PRODS %>(calcProds) { // same name for Synthesis
		"use strict";
		setAndOpen(calcProds.replace(/\\\\/g, '\\'), 'calculated');
	} // <%= Mechanism.OPEN_CALCD_PRODS %>

	function <%= Mechanism.OPEN_OFFENDERS %>(offendingCpds) {
		"use strict";
		setAndOpen(offendingCpds.replace(/\\\\/g, '\\'), 'offending');
	} // <%= Mechanism.OPEN_OFFENDERS %>

	function setAndOpen(cpds, title) {
		"use strict";
		var calcProds = cpds.replace(/\\\\/g, '\\');
		setCookie('calculatedProds', calcProds);
		var bld = new String.builder().
				append('showMol.jsp?title=').append(title).
				append('<%= isMechanism ? "&lonePairs=true" : "" %>').
				append('&cpds=').append(encodeURIComponent(cpds));
		openMolShowWindow(bld.toString());
	} // setAndOpen()

	function getCalcdProds() {
		"use strict";
		return getCookie('calculatedProds');
	} // getCalcdProds()

	function loadSelections() {
		"use strict";
		<% if (isMarvin || isMechanism) { %>
			var selectionsJSO = { 'atoms': '<%= selectionsStr %>' };
			marvinSketcherInstances['<%= APPLET_NAME %>'].
					setSelection(selectionsJSO);
		<% } // if Marvin %>
	} // loadSelections()

	<% if (preload && (isLewis || usesChemAxon)) { %>
		// NOTE: for vectors, resetFigure() is handled by startOver() below
		function resetFigure() {
			"use strict";
			<% if (usesChemAxon) { %>
				loadJSMol('<%= Utils.toValidJS(preloadMol) %>', 
						'<%= APPLET_NAME %>');
			<% } else { %>
				parseLewisMRV('<%= Utils.toValidJS(preloadMol) %>');
			<% } // if usesChemAxon 
			if (isSynthesis) { %>
				clearChosenRxns();
				<% final String rxnIds = 
						Synthesis.getRxnConditions(preloadMol);
				if (!Utils.isEmpty(rxnIds)) {
					final String[] preloadChosenRxns = 
							rxnIds.split(Synthesis.RXN_ID_SEP);
					for (final String chosenRxn : preloadChosenRxns) { 
						if (MathUtils.isInt(chosenRxn)) { %>
							setChosenRxn(<%= chosenRxn %>); 
					<% 	} // if chosenRxn is an int; bug makes it sometimes </scalar
					} // for each initial reaction condition
				} // if there are reactions to preload %>
				writeRxnConds(<%= onlyOneRxnCondn %>);
				setInnerHTML('pasteSynCell', 
						'<p><%= Utils.toValidJS(ansPhrases.get(PASTE_SYN)) %> <\/p>' +
						'<p><textarea id="pasteSynthesis" name="pasteSynthesis" ' + 
						'rows="1" cols="10" ' +
						'onkeyup="loadPastedSynthMRV();" ' +
						'style="height:40px; width:95%;"><\/textarea>');
			<% } // if isSynthesis %>
		} // resetFigure()
	<% } // if preload %>

	function showSource(figNum) {
		"use strict";
		var bld = new String.builder().
				append('<%= pathToRoot %>includes/showSourceCode.jsp'
					+ '?sourceCodeNum=').append(figNum);
		openSourceCodeWindow(bld.toString());
	} // showSource()

// ****** question-type-specific functions

<% if (question.hasJmolFigure()) { %>

	jmolInitialize('<%= pathToRoot %>nosession/jmol'); 

<% } // if question has a Jmol image %>

function initLewis() {
	"use strict";
	initLewisConstants(
			[<%= LewisMolecule.CANVAS_WIDTH %>, 
				<%= LewisMolecule.CANVAS_HEIGHT %>],
			<%= LewisMolecule.MARVIN_WIDTH %>, 
			['<%= LewisMolecule.PAIRED_ELECS %>',
				'<%= LewisMolecule.UNPAIRED_ELECS %>',
				'<%= LewisMolecule.UNSHARED_ELECS %>' ],
			'<%= LewisMolecule.LEWIS_PROPERTY %>',
			'<%= LewisMolecule.HIGHLIGHT %>',
			'<%= Utils.toValidJS(ansPhrases.get(ENTER_ELEMENT)) %>',
			'<%= Utils.toValidJS(ansPhrases.get(NO_SUCH_ELEMENT)) %>',
			'<%= Utils.toValidJS(ansPhrases.get(OTHER_LABEL)) %>');
	<% if (isLewis) { %>
		initLewisGraphics('<%= pathToRoot %>', 
				'lewisJSCanvas', 
				'lewisJSToolbars');
		parseLewisMRV('<%= Utils.toValidJS(lastResp) %>');
	<% } // if the question is a Lewis structure question %>
	var figuresData = [];
	<% int qFigNum = 1;
	for (final Figure figure : figures) {
		if (figure.isLewis()) {
			final String[] figData = figure.getDisplayData();
	%>
			figuresData.push(['<%= Utils.toValidJS(
						figData[Figure.STRUCT]) %>',
					<%= qFlags %>,
					<%= qFigNum %>]);
	<%	} // if figure is a Lewis structure
		qFigNum += 1;
	} // for each figure
	%>
	if (!isEmpty(figuresData)) {
		loadLewisInlineImages('<%= pathToRoot %>', figuresData, ADD_CLICK);
	}
} // initLewis()

<% if (isSynthesis) { %>
	
	function initSynthesis() {
		"use strict";
		var constants = new Array();
		constants[NO_REAGENTS] = '<%= RxnCondition.NO_REAGENTS %>';
		constants[RXN_ID_SEP] = '<%= Synthesis.RXN_ID_SEP %>';
		constants[RXN_IDS] = '<%= Synthesis.RXN_IDS %>';
		constants[CLICK_HERE] = '<%= Utils.toValidJS(rcPhrases.get(CLICK_RXN)) %>';
		constants[INSERT_HERE] = '<%= rcPhrases.get(INSERT_HERE) == null ? "" 
					: Utils.toValidJS(rcPhrases.get(INSERT_HERE)) %>';
		constants[ADD_1ST] = '<%= Utils.toValidJS(rcPhrases.get(ADD_1ST)) %>';
		constants[RXN_CONDN] = '<%= Utils.toValidJS(rcPhrases.get(RXN_CONDN)) %>';
		constants[PATH_TO_CHOOSE_RXN_CONDS_USER] = '<%= pathToChooseRxnCondsUser %>';
		constants[REMOVE] = '<%= Utils.toValidJS(rcPhrases.get(REMOVE)) %>';
		constants[AFTER_HERE] = '<%= rcPhrases.get(AFTER_HERE) == null ? ""
				: Utils.toValidJS(rcPhrases.get(AFTER_HERE)) %>';
		initRxnConds(constants);
		<% final int[] allRxnIds = RxnCondition.getAllReactionIds();
		for (final int rxnId : allRxnIds) { %>
			setRxnName(<%= rxnId %>, '<%= Utils.toValidJS(
					Utils.toDisplay(reactionNamesByIds.get(
						Integer.valueOf(rxnId)))) %>');
		<% } // for each rxn id
		for (final int rxnId : allowedRxns) { %>
			setAllowedRxn(<%= rxnId %>);
		<% } // for each allowed rxn id
		final int numRxns = (onlyOneRxnCondn && chosenRxns.length > 1
				? 1 : chosenRxns.length);
		for (final String chosenRxn : chosenRxns) { 
			if (MathUtils.isInt(chosenRxn)) { %>
				setChosenRxn(<%= chosenRxn %>); 
		<%	} // if chosenRxn is an int; bug makes it sometimes </scalar
		} // for each initial reaction condition %>
		writeRxnConds(<%= onlyOneRxnCondn %>);
	} // initSynthesis()

	function loadPastedSynthMRV() {
		"use strict";
		var pastedSynMRV = getValue('pasteSynthesis');
		loadJSMol(pastedSynMRV, '<%= APPLET_NAME %>');
		ajaxURIArr = [];
		ajaxURIArr.push(['mrvStr', encodeURIComponent(pastedSynMRV)]);
		callAJAX('<%= pathToRoot %>includes/getRxnIdsFromMRV.jsp', 
				getAjaxURI(), showRxnCondns);
	} // loadPastedSynthMRV()

	function showRxnCondns() {
		"use strict";
		if (xmlHttp.readyState === 4) { // ready to continue
			var responsePage = xmlHttp.responseText;
			var rxnIdsStr = extractField(responsePage, 'rxnIdsStrValue');
			if (isEmpty(rxnIdsStr)) chosenRxns = [];
			else {
				chosenRxns = rxnIdsStr.split('<%= Synthesis.RXN_ID_SEP %>');
				chosenRxns.unshift('');
			} // if rxnIds have been chosen
			writeRxnConds(false);
			clearInnerHTML('pasteSynCell');
		} // if ready
	} // showRxnCondns()

<% } else if (isClickableImage || isDrawVectors) { 
	final String[] colorAndMaxMarks = 
			ClickImage.getColorAndMaxMarks(Utils.isEmpty(qData) ? null : qData[0].data); %>

	function initDrawOnFigure() {
		"use strict";
		initDrawOnFigConstants();
		initDrawOnFigButtons(
				'<%= Utils.toValidJS(makeButton(ansPhrases.get(CLEAR_LAST), 
					"clearLast();")) %>',
				'<%= Utils.toValidJS(makeButton(ansPhrases.get(CLEAR_ALL), 
					"clearAllOfOne();")) %>',
				'<%= Utils.toValidJS(makeButton(ansPhrases.get(CANCEL), 
					"unselect();")) %>');
		initDrawOnFigPhrases(
				'<%= Utils.toValidJS(ansPhrases.get(CHOOSE_ACTION)) %>',
				'<%= Utils.toValidJS(ansPhrases.get(DELETE_SELECTED)) %>',
				'<%= Utils.toValidJS(ansPhrases.get(UNSELECT)) %>',
				'<%= Utils.toValidJS(ansPhrases.get(INVERT_SELECTED)) %>',
				'<%= Utils.toValidJS(ansPhrases.get(CLICK_NEAR)) %>',
				'<%= Utils.toValidJS(ansPhrases.get(CLICK_NEAR_ARROW)) %>',
				'<%= Utils.toValidJS(ansPhrases.get(DRAW_NEW)) %>',
				'<%= Utils.toValidJS(ansPhrases.get(SELECT_EXISTING)) %>');
		if (<%= isClickableImage %>) {
			setNumShapesLimit(<%= colorAndMaxMarks[ClickImage.NUM_MARKS] %>);
		} // if clickable image
		setClickPurposeMenu();
		initDrawOnFigGraphics('<%= colorAndMaxMarks[ClickImage.COLOR] %>');
		captureClicks();
		<% if (!Utils.isEmpty(lastResp)) {
			if (isClickableImage) { 
				final ClickImage clickImage = new ClickImage(lastResp);
				final int[][] allCoords = clickImage.getAllCoords();
				final String[] allMarkStrs = clickImage.getAllMarkStrs();
				int markNum = 0;
				for (final int[] coords : allCoords) { %>
					setMark([<%= coords[ClickImage.X] %>, 
							<%= coords[ClickImage.Y] %>], 
							'<%= Utils.toValidJS(allMarkStrs[markNum++]) %>');
		<%		} // for each cross %>
				paintAll();
		<% 	} else { // isDrawVectors 
				final DrawVectors drawVectors = new DrawVectors(lastResp);
				final DPoint3[][] vectors = drawVectors.getVectorPoints();
				for (final DPoint3[] vector : vectors) { %>
					drawonfig.allShapes[<%= DrawVectors.ARROW %>].push(
							[canvasSetX(<%= vector[DrawVectors.ORIGIN].x %>), 
							canvasSetY(<%= vector[DrawVectors.ORIGIN].y %>),
							canvasSetX(<%= vector[DrawVectors.TARGET].x %>), 
							canvasSetY(<%= vector[DrawVectors.TARGET].y %>)]);
		<% 		} // for each vector %>
				paintAll();
		<% 	} // if question type %>
		<% } // if there's a last response to display %>
	} // initDrawOnFigure()

	<% if (!Utils.isEmpty(preloadMol)) { %>
		function startOver() {
			"use strict";
			selectedShape = drawonfig.UNSELECTED;
			clearAllOfOne();
			<% final DrawVectors drawVectors = new DrawVectors(preloadMol);
			final DPoint3[][] vectors = drawVectors.getVectorPoints();
			for (final DPoint3[] vector : vectors) { %>
				drawonfig.allShapes[<%= DrawVectors.ARROW %>].push(
						[canvasSetX(<%= vector[DrawVectors.ORIGIN].x %>), 
						canvasSetY(<%= vector[DrawVectors.ORIGIN].y %>),
						canvasSetX(<%= vector[DrawVectors.TARGET].x %>), 
						canvasSetY(<%= vector[DrawVectors.TARGET].y %>)]);
	<% 		} // for each vector %>
			paintAll();
		} // startOver()
	<% } // if there are starting arrows %>

<% } else if (isLogicalStmts) { %>

	function setUpStmts() {
		"use strict";
		var texts = new Array(
				'<%= Utils.toValidJS(ansPhrases.get(ADD_STMT)) %>',
				'<%= Utils.toValidJS(ansPhrases.get(REMOVE_STMT)) %>',
				'<%= Utils.toValidJS(ansPhrases.get(THEREFORE)) %>',
				'<%= Utils.toValidJS(ansPhrases.get(BLANK_STMT)) %>',
				'<%= Utils.toValidJS(ansPhrases.get(BLANK_SUBMIT)) %>',
				'<%= Utils.toValidJS(ansPhrases.get(UNKNOWN_SUBMIT)) %>',
				'<%= Utils.toValidJS(ansPhrases.get(SEE_WORDS)) %>'
				);
		var stmts = new Array();
		<% final Logic logic = new Logic(lastResp);
		final String[] stmts = logic.getStatements();
		for (final String stmt : stmts) { %>
			stmts.push('<%= Utils.toValidTextbox(
					Utils.unicodeToCERs(stmt)) %>');
		<% } // for each current statement 
		final String addlWordsStr = (Utils.isEmpty(qData) ? "" : qData[0].data); %>
		setStmts(stmts, texts, '<%= Utils.toValidJS(addlWordsStr) %>', 
				'<%= Utils.toValidJS(pathToRoot) %>');
	} // setUpStmts()

<% } else if (isEquations) { %>

	function setUpEqns() {
		"use strict";
		initEqnConstants();
		initEqnPhrases(
				'<%= Utils.toValidJS(ansPhrases.get(ADD_EQN)) %>',
				'<%= Utils.toValidJS(ansPhrases.get(REMOVE_EQN)) %>',
				'<%= Utils.toValidJS(ansPhrases.get(BLANK_SUBMIT)) %>',
				'<%= Utils.toValidJS(ansPhrases.get(CONSTANTS)) %>'
				);
		setPathToRoot('<%= Utils.toValidJS(pathToRoot) %>');
		var eqns = new Array();
		<% final Equations eqnsObj = new Equations(lastResp);
		final String constants = eqnsObj.getConstants();
		String[] eqns = eqnsObj.getEntries();
		String variablesNotUnits = eqnsObj.getVariablesNotUnitsStr();
		int numInitialEqns = 0;
		boolean disableConstantsField = false;
		if (!Utils.isEmpty(qData)) {
			final Equations initEqnsObj = new Equations(qData[0].data);
			final String[] initEqns = initEqnsObj.getEntries();
			if (Utils.isEmpty(lastResp)) {eqns = initEqns;}
			numInitialEqns = initEqns.length;
			disableConstantsField = initEqnsObj.getDisableConstants();
			if (variablesNotUnits == null) {
				variablesNotUnits = initEqnsObj.getVariablesNotUnitsStr();
			} // if need to initialize variables-not-units
		} // if there are question data
		for (int eqnNum = 0; eqnNum < eqns.length; eqnNum += 1) { %>
			eqns.push('<%= Utils.toValidTextbox(eqns[eqnNum]) %>');
		<% } // for each current equation %>
		if (eqns.length === 0) eqns.push('');
		setNumInitialEqns(<%= numInitialEqns %>);
		setConstantsFieldOptions(<%= question.omitConstantsField() %>,
				<%= disableConstantsField %>);
		setEqns(eqns, '<%= Utils.toValidJS(constants) %>');
		setValue(equations.VARS_NOT_UNITS_TAG, '<%= Utils.toValidJS(variablesNotUnits) %>');
	} // setUpEqns()

<% } else if (isOED || isRCD) { 
	final OED oed = (!isOED ? null 
			: Utils.isEmpty(qData) ? new OED() : new OED(qData));
	final RCD rcd = (!isRCD ? null 
			: Utils.isEmpty(qData) ? new RCD() : new RCD(qData));
%>
	function initED() {
		"use strict";
		setEDConstants(new Array('<%= EDiagram.DIAGRAM_TAG %>',
				'<%= EDiagram.IS_OED_TAG %>',
				'<%= EDiagram.CELL_TAG %>',
				'<%= EDiagram.LINE_TAG %>',
				'<%= DiagramCell.ROW_TAG %>',
				'<%= DiagramCell.COLUMN_TAG %>',
				'<%= DiagramCell.LABEL_TAG %>',
				'<%= CellsLine.ENDPT_TAG %>'));
		var phrases = new Array();
		phrases[REFRESH_BUTTON] = '<%= Utils.toValidJS(makeButton(
				edPhrases.get(REFRESH_BUTTON), "updateCanvas();")) %>';
		phrases[DELETE_BUTTON] = '<%= Utils.toValidJS(makeButton(
				edPhrases.get(DELETE_BUTTON), "clearSelected();")) %>';
		phrases[DROP_HERE] = '<%= Utils.toValidJS(edPhrases.get(DROP_HERE)) %>';
		<% int numRows = 0;
		int numCols = 0;
		List<CellsLine> conLines;
		final String[] labels = (isOED ? oed.getLabels() : rcd.getLabels());
		for (int lblNum = 0; lblNum < labels.length; lblNum += 1) { %>
			setLabel('<%= Utils.toPopupMenuDisplay(labels[lblNum]) %>');
		<% } // for each label
		if (isOED) {
			numRows = oed.getNumRows();
			numCols = oed.getNumCols();
			conLines = oed.getLines();
			if (!Utils.isEmpty(lastResp)) oed.setOrbitals(lastResp);
			final int numTypes = Orbital.getNumTypes();
			for (int orbType = 1; orbType < numTypes; orbType += 1) { %>
				setOrbPopupName('<%= Utils.toValidJS(Orbital.getPopupMenuName(orbType)) %>');
			<% } // for each orbital type %>
			phrases[CLICK_OED] = '<%= Utils.toValidJS(edPhrases.get(CLICK_OED)) %>';
			phrases[ADD] = '<%= Utils.toValidJS(edPhrases.get(ADD)) %> ';
			phrases[ORBS_OF_TYPE] = ' <%= Utils.toValidJS(edPhrases.get(ORBS_OF_TYPE)) %> ';
			var strConstants = new Array();
			strConstants[OCCUP_SEP] = '<%= OEDCell.OCCUP_SEP %>';
			strConstants[ORBS_TYPE_TAG] = '<%= OEDCell.ORBS_TYPE_TAG %>';
			strConstants[OCCUPS_TAG] = '<%= OEDCell.OCCUPS_TAG %>';
			strConstants[OCCUP_TAG] = '<%= OEDCell.OCCUP_TAG %>';
			setOEDConstants(strConstants, phrases, <%= labelOrbitals %>);
		<% } else {
			numRows = rcd.getNumRows();
			numCols = rcd.getNumCols();
			conLines = rcd.getOrigLines();
			if (!Utils.isEmpty(lastResp)) rcd.setStates(lastResp, !RCD.THROW_IT); %>
			phrases[CLICK_RCD] = '<%= Utils.toValidJS(edPhrases.get(CLICK_RCD)) %>';
			setRCDConstants(phrases);
		<% } // if isOED %>
		initGraphics(0);
		<% for (int rNum = 0; rNum < numRows; rNum += 1) {
			for (int cNum = 0; cNum < numCols; cNum += 1) { 
				final int row = numRows - rNum; 
				final int col = cNum + 1; 
				if (isOED) {
					final OEDCell cell = oed.getCell(row, col);
					if (cell.hasOrbitals()) { %>
						drop(<%= row %>, <%= col %>,
								'<%= cell.getOrbitalsType() %>', 
								'<%= Utils.toValidJS(cell.getOccupancies()) %>',
								'<%= cell.getLabel() %>');
					<% } else { %>
						clearMe(<%= row %>, <%= col %>);
					<% } // if there are orbitals in this cell
				} else { // isRCD
					if (rcd.isOccupied(row, col)) { %>
						drop(<%= row %>, <%= col %>, <%= rcd.getLabel(row, col) %>);
					<% } else { %>
						clearMe(<%= row %>, <%= col %>);
					<% } // if there is a state in this cell
				} // if question type
			} // for each column
		} // for each row
		for (int lineNum = 0; lineNum < conLines.size(); lineNum += 1) {
			final CellsLine line = conLines.get(lineNum);
			final int ARow = line.endPoints[0].getRow();
			final int ACol = line.endPoints[0].getColumn();
			final int BRow = line.endPoints[1].getRow();
			final int BCol = line.endPoints[1].getColumn(); %>
			initLine('r<%= ARow %>c<%= ACol %>', 'r<%= BRow %>c<%= BCol %>');
		<% } // for each line %>
		write<%= isOED ? "OrbSelectors" : "TextAndButtons" %>();
		window.onscroll = updateCanvas;
		window.onresize = updateCanvas;
		updateCanvas();
	} // initED()

<% } else if ((isChoice || isChooseExplain || isRank) && !Utils.isEmpty(qData)) { %>

	var qDataToDisplay = new Array();
	var qDataTypes = new Array();
	<% for (int itemNum = 0; itemNum < qData.length; itemNum += 1) { %>
		qDataTypes[<%= itemNum %>] = <%= qData[itemNum].dataType %>;
		qDataToDisplay[<%= itemNum %>] = '<%= qData[itemNum].isMarvin() 
				? Utils.toValidJS(qData[itemNum].getImage(
					pathToRoot, user.prefersPNG(), itemNum))
				: Utils.toString("<br\\/>", Utils.toValidJS(chemFormatting 
					? Utils.toDisplay(qData[itemNum].data)
					: qData[itemNum].data)) %>';
	<% 	} // for each item %>

	function getMolForMView(itemNum) {
		var newQDNum = parseInt(getValue('qdNumOfCell' + itemNum));
		return qDataToDisplay[newQDNum - 1];
	} // getMolForMView()

	function launchMView(itemNum) {
		"use strict";
		var newQDNum = parseInt(getValue('qdNumOfCell' + itemNum));
		var getMolMethodName = new String.builder().
				append('getMolForMView(').append(itemNum).append(')').toString();
		var url = new String.builder().
				append('<%= pathToRoot %>includes\/marvinJSViewer.jsp' +
					'?viewOpts=<%= qFlags %>&getMolMethodName=').
				append(encodeURIComponent(getMolMethodName)).
				toString();
		openSketcherWindow(url);
	} // launchMView()

	<% if (isRank) { %>

		function repaintRank() {
			"use strict";
			var majorSep = '<%= Rank.MAJOR_SEP %>';
			var minorSep = '<%= Rank.MINOR_SEP %>';
			// get item numbers and current ranks in current order of presentation
			var initNums = new Array();
			var initRanks = new Array();
			var initRanksSort = new Array();
			var itemRanks = submissionValue.split(majorSep);
			var numItems = itemRanks.length;
			var qdNum;
			if (itemRanks[numItems - 1].indexOf(minorSep) < 0) { // <!-- >
				numItems -= 1; 
			} // if no minor separator
			for (qdNum = 1; qdNum <= numItems; qdNum += 1) { // <!-- >
				var pieces = itemRanks[qdNum - 1].split(minorSep);
				var itemNum = parseInt(pieces[0]);
				var rank = getValue('qdValue' + qdNum);
				initNums[qdNum - 1] = itemNum;
				initRanks[itemNum - 1] = parseInt(rank);
				initRanksSort[itemNum - 1] = initRanks[itemNum - 1];
			}
			initRanksSort.sort(function(a, b) {
				return a < b ? -1 : a === b ?  0 : 1; // <!-- >
			});
			// find qData corresponding to sorted initRanks; 
			// set newQDNums array in order of ranking
			var numUnranked = 0;
			var newRanks = new Array();
			var newQDNums = new Array();
			var itemNum;
			// get the ranked item QD numbers in their new rank order
			for (itemNum = 1; itemNum <= <%= qData.length %>; itemNum += 1) {
				var currentRank = initRanksSort[itemNum - 1];
				if (currentRank !== 0) {
					// find which qdNum goes with this rank
					for (qdNum = 1; qdNum <= <%= qData.length %>; qdNum += 1) {
						if (currentRank === initRanks[qdNum - 1]) {
							newQDNums[itemNum - 1 - numUnranked] = qdNum; 
							newRanks[qdNum - 1] = currentRank;
							initRanks[qdNum - 1] = -1;
							break;
						} // if found the rank
					} // for each qdNum - 1
				} else { 
					numUnranked += 1;
				}
			} // for each rank
			var numRanked = <%= qData.length %> - numUnranked;
			var unrankedNum = 1;
			// add unranked items to end of list, preserving their current
			// order
			for (itemNum = 1; itemNum <= <%= qData.length %>; itemNum += 1) {
				var qdNum = initNums[itemNum - 1];
				if (initRanks[qdNum - 1] === 0) {
					newQDNums[unrankedNum - 1 + numRanked] = qdNum;
					newRanks[qdNum - 1] = 0;
					unrankedNum += 1;
				} // if item is unranked
			} // for each item
			// reconstruct the HTML, submissionValue
			var molValueBld = new String.builder();
			for (itemNum = 1; itemNum <= <%= qData.length %>; itemNum += 1) {
				var newQDNum = newQDNums[itemNum - 1];
				var newRank = newRanks[newQDNum - 1];
				if (itemNum > 1) molValueBld.append(majorSep);
				molValueBld.append(newQDNum).append(minorSep).append(newRank); 
				var isMarvin = (qDataTypes[newQDNum - 1] === <%= QDatum.MARVIN %>);
				var displayLocnId = 'qdDisplay' + itemNum;
				var displayLocnCell = getCell(displayLocnId);
				/* alert('Question datum ' + newQDNum + 
						' of type ' + qDataTypes[newQDNum - 1] + 
				 		' has new position ' + itemNum + 
						' with rank ' + newRank + 
						' and will be displayed in cell with ID ' + 
						displayLocnId + ':\n' + 
						qDataToDisplay[newQDNum - 1]); /**/
				if (displayLocnCell) {
					displayLocnCell.innerHTML = qDataToDisplay[newQDNum - 1];
					displayLocnCell.style.verticalAlign = 'middle';
				} // if the cell exists
				setValue('qdValue' + itemNum, '' + newRank);
				setValue('qdNumOfCell' + itemNum, '' + newQDNum);
				if (isMarvin) setInnerHTML('launchMViewCell' + itemNum, 
						new String.builder().append('<a onclick="launchMView(').
						append(itemNum).
						append(');"><u>Launch MarvinJS&trade; viewer<\/u><\/a>').
						toString());
			} // for each itemNum
			submissionValue = molValueBld.toString();
			// alert('New submissionValue = ' + submissionValue);
		} // repaintRank()

	<% } // if rank %>

<% } // if question type %>

// --> end HTML comment
// vim:filetype=jsp:
