<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.evals.Evaluator,
	com.epoch.qBank.QDatum,
	com.epoch.qBank.QSetDescr,
	com.epoch.qBank.Question,
	com.epoch.session.QSet,
	com.epoch.translations.QSetTransln,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	QSetTransln translator;
	String language;
	QSet qSet;
	synchronized (session) {
		translator = (QSetTransln) session.getAttribute("translationObj");
		language = (String) session.getAttribute("translationLanguage");
		qSet = (QSet) session.getAttribute("qSet");
	}
	String qSetId = request.getParameter("qSetId");
	if (translator == null || translator.getSetId() != qSet.getQSetId()) {
		// always true after saving a translation
		if (language == null) language = request.getParameter("language");
		translator = new QSetTransln(qSet, language, user.getUserId());
		synchronized (session) {
			session.setAttribute("translationLanguage", language);
			session.setAttribute("translationObj", translator);
		}
		qSetId = String.valueOf(qSet.getQSetId());
	} /* else Utils.alwaysPrint("translate.jsp: using existing translator for ",
			language, "; translated header = ", translator.header); /**/
	final String saved = request.getParameter("saved");

	final QSetDescr qSetDescr = translator.getQSetDescr();
	final Question[] setQs = translator.getQuestions();
	final int numQs = qSet.getCount();
	final int[] numEvals = new int[numQs];
	final int[] numQData = new int[numQs];
	for (int qNum = 0; qNum < numQs; qNum++) {
		numEvals[qNum] = translator.evalFeedbacks[qNum].length;
		numQData[qNum] = translator.qdTexts[qNum].length;
	} // for each Q

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head> 
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Translation</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
		#footer {
			position:absolute; 
			bottom:0; 
			left:0;
			width:100%; 
			height:40px; 
			overflow:auto; 
			text-align:right; 
		}

		#translationContents {
			position:fixed; 
			top:255px;
			left:0;
			bottom:40px; 
			right:0; 
			overflow:auto; 
		}

		* html body {
			padding:55px 0 40px 0; 
		}

		* html #translationContents {
			height:100%; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

	function exitTranslator() {
		var go = '<%= pathToRoot %>authortool/'
				+ 'questionsList.jsp?qSetId=<%= qSetId %><%= AppConfig.notEnglish
					? "&refreshQSet=true" : "" %>';
		self.location.href = go;
	}

	function exportMe(english) {
		var url = 'exportTranslations.jsp?qSetId=<%= qSetId %>';
		if (english) url += '&language=English';
		openExportWindow(url);
	}

	function importTranslations() {
		openExportWindow('importTranslations.jsp?which=questions'
				+ '&qSetId=<%= qSetId %>&language='
				+ encodeURIComponent('<%= Utils.toValidJS(language) %>')); 
	}

	// create array to store whether the header or any Q is open
	var qOpen = new Array(<%= numQs + 1 %>);

	// create triple array to store phrases and translations
	// first element: 0 = statements/feedbacks, 1 = question data
	// second element: qNum (1-based for statements/feedbacks,
	// 0-based for question data
	// third element: statement or 1-based feedback or 0-based question data
	var phrasesQStmts = new Array(<%= numQs %>);
	var translnsQStmts = new Array(<%= numQs %>);
	var translnsQStmtsForTextbox = new Array(<%= numQs %>);
	var phrasesEvals = new Array(<%= numQs %>);
	var translnsEvals = new Array(<%= numQs %>);
	var translnsEvalsForTextbox = new Array(<%= numQs %>);
	var phrasesQData = new Array(<%= numQs %>);
	var translnsQData = new Array(<%= numQs %>);
	var translnsQDataForTextbox = new Array(<%= numQs %>);

	// initialize values
	qOpen[0] = false;
	var phrasesHeader = 
			'<%= Utils.toValidJS(Utils.toDisplay(qSetDescr.header)) %>';
	var translnsHeader =
			'<%= Utils.toValidJS(Utils.toValidTextbox(translator.header)) %>';
	<% for (int qNum = 0; qNum < numQs; qNum++) { 
		final Question setQ = setQs[qNum]; 
		final boolean usesSubstns = setQ.usesSubstns();
		final Evaluator[] evals = setQ.getAllEvaluators(); 
		final QDatum[] qData = setQ.getQData(Question.GENERAL); 
		final String[] qDataTexts = setQ.getQDataTexts();
		final String statement = setQ.getStatement(); 
	%>
		qOpen[<%= qNum + 1 %>] = false;
		// set the question statements
		phrasesQStmts[<%= qNum %>] = 
					'<%= Utils.toValidJS(Utils.toDisplay(statement)) %>';
		translnsQStmts[<%= qNum %>] = 
					'<%= Utils.toValidJS(translator.qStmts[qNum]) %>';
		translnsQStmtsForTextbox[<%= qNum %>] = 
					'<%= Utils.toValidJS(Utils.toValidTextbox(translator.qStmts[qNum])) %>';

		// initialize arrays of evals and question data
		phrasesEvals[<%= qNum %>] = new Array(<%= numEvals[qNum] %>);
		translnsEvals[<%= qNum %>] = new Array(<%= numEvals[qNum] %>);
		translnsEvalsForTextbox[<%= qNum %>] = new Array(<%= numEvals[qNum] %>);
		phrasesQData[<%= qNum %>] = new Array(<%= numQData[qNum] %>);
		translnsQData[<%= qNum %>] = new Array(<%= numQData[qNum] %>);
		translnsQDataForTextbox[<%= qNum %>] = new Array(<%= numQData[qNum] %>);

		<% for (int evalNum = 0; evalNum < numEvals[qNum]; evalNum++) { 
			final Evaluator eval = evals[evalNum]; 
			final String[] evalDescrip = eval.getDescription(qDataTexts);
			final StringBuilder evalBld = new StringBuilder();
			for (final String line : evalDescrip) {
				if (evalBld.length() > 0) evalBld.append("<br />");
				evalBld.append(line);
			} // for each line in the evaluator description
		%>
			phrasesEvals[<%= qNum %>][<%= evalNum %>] = 
					'<span style="color:green;"><%= 
					Utils.toValidJS(Utils.toDisplay(
							evalBld.toString(), usesSubstns) 
					+ "</span>:<p>" + Utils.toDisplay(eval.feedback)) %>';
			translnsEvals[<%= qNum %>][<%= evalNum %>] =
					trimWhiteSpaces('<%= Utils.toValidJS(
							translator.evalFeedbacks[qNum][evalNum]) %>');
			translnsEvalsForTextbox[<%= qNum %>][<%= evalNum %>] =
					trimWhiteSpaces('<%= Utils.toValidJS(
							Utils.toValidTextbox(
								translator.evalFeedbacks[qNum][evalNum])) %>');
		<% } // for each evaluator %>
		<% for (int qdNum = 0; qdNum < numQData[qNum]; qdNum++) { 
			final QDatum qDatum = qData[qdNum]; 
		%>
			phrasesQData[<%= qNum %>][<%= qdNum %>] = 
					'<%= Utils.toValidJS(Utils.toDisplay(
						qDatum.isMarvin() ? qDatum.name : qDatum.data)) %>';
			translnsQData[<%= qNum %>][<%= qdNum %>] =
					trimWhiteSpaces('<%= Utils.toValidJS(
							translator.qdTexts[qNum][qdNum]) %>');
			translnsQDataForTextbox[<%= qNum %>][<%= qdNum %>] =
					trimWhiteSpaces('<%= Utils.toValidJS(
							Utils.toValidTextbox(
								translator.qdTexts[qNum][qdNum])) %>');
		<% } // for each qDatum %>
	<% } // for each Q %>

	function initializeOpenQs() {
		<% final String openQsStr = request.getParameter("openQs");
		if (!Utils.isEmpty(openQsStr)) {
			final String[] openQs = openQsStr.split(":");
			for (final String openQ : openQs) { %>
				paintQuestion(<%= openQ %>);
			<% } // for each open Q
		} // if there are initially open Qs %> 	
		if (<%= saved %>) setInnerHTML('savedFlag', 'Saved.');
	} // initializeOpenQs()
	
	function paintQuestion(qNumJS) {
		if (qOpen[qNumJS]) unpaintQuestion(qNumJS);
		else repaintQuestion(qNumJS);
	} // paintQuestion()

	function unpaintQuestion(qNumJS) {
		// qNumJS is 0 for common Q statement, 1-based Q number otherwise
		if (qNumJS === 0) { // common Q statement
			var translation = trimWhiteSpaces(document.getElementById(
					'xlatnHeaderBox').value);
			var origTranslation = translnsHeader;
			if (origTranslation !== translation) setNotSavedFlag();
			translnsHeader = translation;
			setInnerHTML('xlatnHeaderCell', translnsHeader); 
		} else { // a question
			// get the current value of Q statement translation textbox 
			var translation = trimWhiteSpaces(document.getElementById(
					'xlatnQStmtBox' + qNumJS).value);
			// store the translation
			var origTranslation = translnsQStmts[qNumJS - 1];
			if (origTranslation !== translation) setNotSavedFlag();
			translnsQStmts[qNumJS - 1] = translation;
			translnsQStmtsForTextbox[qNumJS - 1] = translation;
			// store evaluators
			var numEvals = translnsEvals[qNumJS - 1].length;
			for (var evalNum = 0; evalNum < numEvals; evalNum++) { // >
				var evalBoxBld = new String.builder().
						append('xlatnEvalBox').append(qNumJS).
						append('_').append(evalNum + 1);
				var evalBox = evalBoxBld.toString();
				// get the current value of an evaluator translation textbox 
				var translation = trimWhiteSpaces(getValue(evalBox));
				// store the translation
				var origTranslation = translnsEvals[qNumJS - 1][evalNum];
				if (origTranslation !== translation) setNotSavedFlag();
				translnsEvals[qNumJS - 1][evalNum] = translation;
				translnsEvalsForTextbox[qNumJS - 1][evalNum] = translation;
			} // for each evaluator
			// store question data
			var numQData = translnsQData[qNumJS - 1].length;
			for (var qdNum = 0; qdNum < numQData; qdNum++) { // >
				var qdBoxBld = new String.builder().
						append('xlatnQDBox').append(qNumJS).
						append('_').append(qdNum + 1);
				var qdBox = qdBoxBld.toString();
				// get the current value of a translation textbox (qDatum)
				var translation = trimWhiteSpaces(getValue(qdBox));
				// store the translation
				var origTranslation = translnsQData[qNumJS - 1][qdNum];
				if (origTranslation !== translation) setNotSavedFlag();
				translnsQData[qNumJS - 1][qdNum] = translation;
				translnsQDataForTextbox[qNumJS - 1][qdNum] = translation;
			} // for the qData
			// remove the evaluators
			clearInnerHTML('evals' + qNumJS);
			clearInnerHTML('xlatnQStmtCell' + qNumJS);
		}
		qOpen[qNumJS] = false;
	} // unpaintQuestion()

	function repaintQuestion(qNumJS) {
		// qNumJS is 0 for common Q statement, 1-based Q number otherwise
		if (qNumJS === 0) {
			// common Q statement
			var headerBld = new String.builder().
					append('<textarea name="xlatnHeaderBox" '
						+ 'id="xlatnHeaderBox" '
						+ 'cols="65" rows="4" '
						+ 'onchange="setNotSavedFlag();">').
					append(translnsHeader).
					append('<\/textarea>');
			setInnerHTML('xlatnHeaderCell', headerBld.toString());
		} else {
			// set textbox for Q statement
			var qStmtBld = new String.builder().
					append('<textarea name="xlatnQStmtBox').
					append(qNumJS).append('" id="xlatnQStmtBox').
					append(qNumJS).append('" cols="65" rows="4" '
						+ 'onchange="setNotSavedFlag();">').
					append(translnsQStmtsForTextbox[qNumJS - 1]).
					append('<\/textarea>');
			setInnerHTML('xlatnQStmtCell' + qNumJS, qStmtBld.toString());
			// start table for qData and evals
			var bld = new String.builder().
					append('<table class="regtext" style="width:100%; '
						+ 'margin-left:auto; margin-right:auto; '
						+ 'border-style:none; border-collapse:collapse;">');
			// create row for each qDatum
			var numQData = phrasesQData[qNumJS - 1].length;
			for (var qdNum = 0; qdNum < numQData; qdNum++) { // >
				var qdBoxBld = new String.builder().
						append('xlatnQDBox').append(qNumJS).
						append('_').append(qdNum + 1);
				var qdBox = qdBoxBld.toString();
				bld.append('<tr style="background-color:#CCFFFF;">').
						append('<td class="boldtext" style="padding-top:5px; '
							+ 'padding-bottom:10px; padding-left:20px;">Item (').
						append(qdNum + 1).
						append(')<\/td><td class="regtext" style="'
							+ 'padding-left:10px; padding-top:5px; '
							+ 'padding-bottom:10px; width:45%;">').
						append(phrasesQData[qNumJS - 1][qdNum]).
						append('<\/td><td class="regtext" style="width:45%; '
							+ 'padding-top:5px; padding-bottom:10px; '
							+ 'padding-left:15px;"><textarea name="').
						append(qdBox).append('" id="').append(qdBox).
						append('" cols="65" rows="2" '
							+ 'onchange="setNotSavedFlag();">').
						append(translnsQDataForTextbox[qNumJS - 1][qdNum]).
						append('<\/textarea><\/td><\/tr>'); 
			} // for the qData
			// create row for each evaluator
			var numEvals = phrasesEvals[qNumJS - 1].length;
			for (var evalNum = 0; evalNum < numEvals; evalNum++) { // >
				var evalBoxBld = new String.builder().
						append('xlatnEvalBox').append(qNumJS).
						append('_').append(evalNum + 1);
				var evalBox = evalBoxBld.toString();
				bld.append('<tr style="background-color:#F5F5DC;">').
						append('<td class="boldtext" style="padding-top:5px; '
							+ 'padding-bottom:10px; padding-left:20px;">Eval (').
						append(evalNum + 1).
						append(')<\/td><td class="regtext" style="'
							+ 'padding-left:10px; padding-top:5px; '
							+ 'padding-bottom:10px; width:45%;">').
						append(phrasesEvals[qNumJS - 1][evalNum]).
						append('<\/td><td class="regtext" style="width:45%;'
							+ 'padding-top:5px; padding-bottom:10px; '
							+ 'padding-left:15px;"><textarea name="').
						append(evalBox).append('" id="').append(evalBox).
						append('" cols="65" rows="4" '
							+ 'onchange="setNotSavedFlag();">').
						append(translnsEvalsForTextbox[qNumJS - 1][evalNum]).
						append('<\/textarea><\/td><\/tr>'); 
			} // for each evaluator
			bld.append('<\/table>');
			setInnerHTML('evals' + qNumJS, bld.toString());
		} // if a Q
		qOpen[qNumJS] = true;
	} // repaintQuestion()

	var backNoSaveButton = '<%= Utils.toValidJS(makeButton("Back w/o Saving", 
			"exitTranslator();")) %>';
			
	function setNotSavedFlag() {
		setInnerHTML('savedFlag', '<span class="boldtext">Not saved.<\/span>');
		setInnerHTML('backButton', backNoSaveButton);
	}

	function saveTranslations() {
		var openQBld = new String.builder();
		<% for (int qNum = 0; qNum <= numQs; qNum++) { %>
			if (qOpen[<%= qNum %>]) {
				openQBld.append('<%= qNum %>:');
			}
			<% if (qNum == 0) { %>
				var box = document.getElementById('xlatnHeaderBox');
				document.translatorForm.xlatnHeader.value =
						(box ? box.value : translnsHeader);
			<% } else { // note: qNum is 1-based at this point %>
				var box = document.getElementById('xlatnQStmtBox<%= qNum %>');
				document.translatorForm.xlatnQStmt<%= qNum %>.value =
						(box ? box.value : translnsQStmts[<%= qNum - 1 %>]);
				<% for (int evalNum = 0; 
						evalNum < numEvals[qNum - 1]; evalNum++) { %>
					var box = document.getElementById(
							'xlatnEvalBox<%= qNum %>_<%= evalNum + 1 %>');
					document.translatorForm.xlatnEval<%= qNum %>_<%= evalNum + 1 %>.value =
							(box ? box.value 
							: translnsEvals[<%= qNum - 1 %>][<%= evalNum %>]);
				<% } // for the Q statement and each evaluator 
				for (int qdNum = 0; qdNum < numQData[qNum - 1]; qdNum++) { %>
					var box = document.getElementById(
							'xlatnQDBox<%= qNum %>_<%= qdNum + 1 %>');
					document.translatorForm.xlatnQD<%= qNum %>_<%= qdNum + 1 %>.value =
							(box ? box.value 
							: translnsQData[<%= qNum - 1 %>][<%= qdNum %>]);
				<% } // for the qData
			} // if this is a real Q
		} // for each Q %>
		document.translatorForm.openQs.value = openQBld.toString();
		document.translatorForm.submit();
	} // saveTranslations()

	// -->
	</script>

</head>
<body class="light" style="background-color:white; text-align:center; 
		overflow:auto;" onload="initializeOpenQs();">

	<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

	<form name="translatorForm" method="post" action="saveTranslations.jsp"
			accept-charset="UTF-8">
		<input type="hidden" name="language" 
				value="<%= Utils.toValidHTMLAttributeValue(language) %>" /> 
		<input type="hidden" name="qSetId" value="<%= qSetId %>" /> 
		<input type="hidden" name="openQs" value="" /> 
		<input type="hidden" name="xlatnHeader" value="" />
	<% for (int qNum = 0; qNum < numQs; qNum++) { %>
		<input type="hidden" name="xlatnQStmt<%= qNum + 1 %>" value="" />
		<% for (int evalNum = 0; evalNum < numEvals[qNum]; evalNum++) { %>
			<input type="hidden" name="xlatnEval<%= qNum + 1 %>_<%= evalNum + 1 %>" value="" />
		<% } // for each evaluator %>
		<% for (int qdNum = 0; qdNum < numQData[qNum]; qdNum++) { %>
			<input type="hidden" name="xlatnQD<%= qNum + 1 %>_<%= qdNum + 1 %>" value="" />
		<% } // for each qDatum
	} // for each Q %>
	&nbsp;<br/>
	&nbsp;<br/>
	&nbsp;<br/>
	&nbsp;<br/>
	&nbsp;<br/>
	<span class="boldtext big">
		Translatable contents of topic 
			"<%= Utils.toDisplay(qSetDescr.topicName.replace("[null]", "")) 
				+ "\", question set \""
				+ Utils.toDisplay(qSetDescr.name) %>"
	</span>
	<div class="regtext" style="margin-left:100px;margin-right:100px;">
		<p>Click on the edit icon next to a question number to translate 
		its various parts into <%= language %>.</p>
		<p>Click on the edit icon again to hide the translations.  ACE
		will retain your work even when it is hidden. </p>
		<p>If you leave a textbox blank, ACE will leave unchanged any
		existing translation for that phrase.  If you wish to erase a
		translation, type <%= QSetTransln.ERASE_TRANSLN %>.  </p>
		<p>Character entity references of the form &amp;#<i>n</i>;
		will appear as the characters they represent; those of the form
		&amp;<i>foo</i>; will appear as such.</p>
		<p>ACE does not substitute translations of question statements, etc. 
		for English in the question bank part of the program, so do not
		expect your translations to appear when you return to the
		questions list page.</p>
	</div>
	<div id="translationContents">
	<table class="regtext" style="width:95%; margin-left:auto; margin-right:auto;
			border-style:none; border-collapse:collapse;">
		<tr style="background-color:#ffffff;">
			<th colspan="2" class="boldtext enlarged" style="padding-left:5px;">
				Question
			</th>
			<th class="boldtext enlarged" style="padding-left:5px;">
				Phrase in English
			</th>
			<th class="boldtext enlarged" 
					style="width:45%;
					padding-left:15px; padding-bottom:10px;">
				Translation into <%= language %>
			</th>
		</tr>
		<% if (!Utils.isEmpty(qSetDescr.header)) { 
			final String bgColor = "#E6E6FA";
			%>
			<tr style="background-color:<%= bgColor %>;">
				<td class="regtext" colspan="3"
				style="padding-left:5px; padding-top:10px; padding-bottom:10px;">
					<span class="boldtext">
					<a onclick="javascript:paintQuestion(0);">Common 
					question statement</a>:</span> 
					<%= Utils.toDisplay(qSetDescr.header) %>
				</td>
				<td id="xlatnHeaderCell" class="regtext" 
						style="width:45%;
						padding-left:15px; padding-bottom:10px; padding-top:10px;">
					<%= translator.header == null ? "" : translator.header %>
				</td>
			</tr>
			<tr><td colspan="4"></td></tr>
		<% } // if there is a common Q statement %>
		<% 
		for (int qNum = 0; qNum < numQs; qNum++) { 
			final String bgColor = "#E6E6FA";
			final Question setQ = setQs[qNum]; 
			final String statement = setQ.getStatement(); %>
			<tr style="background-color:<%= bgColor %>;">
				<td class="boldtext" style="padding-top:5px; padding-bottom:10px; 
						padding-left:10px;">
					(<%= qNum + 1 %>) 
				</td>
				<td class="boldtext" style="padding-top:5px; padding-bottom:10px; 
						padding-left:10px;">
					<%= makeButtonIcon("edit", pathToRoot, 
							"paintQuestion('", qNum + 1, "');") %> 
				</td>
				<td class="regtext" style="padding-top:5px; padding-bottom:10px; 
						padding-left:10px; width:45%;">
					<%= Utils.toDisplay(statement) %>
				</td>
				<td id="xlatnQStmtCell<%= qNum + 1 %>" class="regtext" 
						style="padding-top:5px; padding-bottom:10px; width:45%; 
						padding-left:15px;">
				</td>
			</tr> 
			<tr><td colspan="4" id="evals<%= qNum + 1 %>"></td></tr>
		<% } // for the header and each Q %>
	
	</table>
	</div>
	<div id="footer">
	<table class="regtext" style="width:626px; margin-left:auto; margin-right:auto;
			margin-top:5px; border-style:none; border-collapse:collapse;">
		<tr><td>
			<table style="margin:0px;"><tr>
				<td style="width:100%;">
				</td> 
				<td style="text-align:right; margin-right:0px;">
					 <%= makeButton("Import", "importTranslations();") %>
				</td>
				<td style="text-align:right; margin-right:0px;">
					 <%= makeButton("Save", "saveTranslations();") %>
				</td>
				<td style="text-align:right; margin-right:0px;">
					 <%= makeButton("Export Saved", "exportMe(false);") %>
				</td>
				<td style="text-align:right; margin-right:0px;">
					 <%= makeButton("Export English", "exportMe(true);") %>
				</td>
				<td id="backButton" style="text-align:right; margin-right:0px;">
					 <%= makeButton("Back", "exitTranslator();") %>
				</td>
			</tr></table>
		</td>
		<td id="savedFlag" style="text-align:right; margin-right:0px; 
				padding-left:10px; width:80px;">
			<%= saved == null ? "" : "true".equals(saved) 
					? "Saved." : "<span class=\"boldtext\">Not saved</span>" %> 
		</td>
		</tr>
	</table>
	</div>
	</form>
	
</body>
</html>
