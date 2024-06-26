<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	chemaxon.struc.DPoint3,
	com.epoch.energyDiagrams.DiagramCell,
	com.epoch.energyDiagrams.EDiagram,
	com.epoch.energyDiagrams.OED,
	com.epoch.energyDiagrams.RCD,
	com.epoch.energyDiagrams.CellsLine,
	com.epoch.evals.EvalResult,
	com.epoch.genericQTypes.*,
	com.epoch.lewis.LewisMolecule,
	com.epoch.physics.*,
	com.epoch.qBank.Figure,
	com.epoch.qBank.QDatum,
	com.epoch.qBank.Question,
	com.epoch.session.GradeSet,
	com.epoch.session.HWSession,
	com.epoch.substns.SubstnUtils,
	com.epoch.synthesis.Synthesis,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	com.epoch.xmlparser.XMLUtils,
	java.text.DateFormat,
	java.util.List,
	java.util.TimeZone"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	response.setHeader("Cache-Control", "no-cache"); //HTTP 1.1
	response.setHeader("Pragma", "no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final int hwNum = MathUtils.parseInt(request.getParameter("hwNum"));
	final String studentId = request.getParameter("userId");
	final int qId = MathUtils.parseInt(request.getParameter("qId"));
	final int hwId = MathUtils.parseInt(request.getParameter("hwId"));
	final boolean isInstructorView = 
			request.getParameter("isInstructorView").equals("true");
	final String attemptNumStr = request.getParameter("attemptNum");
	final boolean isTutorial = "true".equals(request.getParameter("isTutorial"));
	/* Utils.alwaysPrint("showMol.jsp: studentId = ", studentId, 
			", isTutorial = ", isTutorial, ", hwNum = ", hwNum, ", hwId = ", hwId, 
			", qId = ", qId, ", attemptNumStr = ", attemptNumStr); /**/

	GradeSet hwGrades;
	HWSession hwsession;
	synchronized (session) {
		hwGrades = (GradeSet) session.getAttribute(isTutorial ? "tutGrades" : "hwGrades");
		if (attemptNumStr == null) {
			hwsession = new HWSession(user, hwId, qId, studentId);
			session.setAttribute("hwsession", hwsession);
		} else {
			hwsession = (HWSession) session.getAttribute("hwsession");
		} // if a previous attempt was requested
	} // synchronized

	final Question thisQ = hwsession.getCurrentQuestion();
	final long qFlags = thisQ.getQFlags();
	final QDatum[] qData = hwsession.getCurrentQData(Question.GENERAL); 
	final boolean isSynthesis = thisQ.isSynthesis();
	final boolean isMarvin = // or mechanism or synthesis
			thisQ.isMarvin() || thisQ.isMechanism() || isSynthesis; 
	final boolean isLewis = thisQ.isLewis();
	final boolean isRank = thisQ.isRank();
	final boolean isChoice = // or fillBlank
			thisQ.isChoice() || thisQ.isFillBlank();
	final boolean isChooseExplain = thisQ.isChooseExplain();
	final boolean isText = thisQ.isText();
	final boolean isFormula = thisQ.isFormula();
	final boolean isNumeric = thisQ.isNumeric();
	final boolean isOED = thisQ.isOED();
	final boolean isRCD = thisQ.isRCD();
	final boolean isTable = thisQ.isTable();
	final boolean isClickableImage = thisQ.isClickableImage();
	final boolean isDrawVectors = thisQ.isDrawVectors();
	final boolean isEquations = thisQ.isEquations();
	final boolean isLogicalStmts = thisQ.isLogicalStatements();
	final boolean chemFormatting = thisQ.chemFormatting();

	final int numAttempts = hwsession.getNumResults();
	final int attemptNum = MathUtils.parseInt(attemptNumStr, numAttempts);
	// Utils.alwaysPrint("showMol.jsp: getting attemptNum = ", attemptNum);
	final EvalResult evalResult = hwsession.getResult(attemptNum);
	final String lastResp = evalResult.lastResponse;
	synchronized (session) {
		session.setAttribute("sourceCode", lastResp);
	} // synchronized
	String addlInfo = "";
	if (isSynthesis) {
		final String[] phrases = Synthesis.getRxnsDisplayPhrases();
		user.translate(phrases);
		addlInfo = Synthesis.getRxnsDisplay(lastResp, phrases);
	} // if is synthesis

	final OED oedResp = (isOED ? new OED(qData) : null); 
	final RCD rcdResp = (isRCD ? new RCD(qData) : null); 
	final int[] assignedQIds = hwGrades.getAssignedQIdsArr(hwNum, studentId);
	final int qNum = Utils.indexOf(assignedQIds, qId) + 1;
	/*
	Utils.alwaysPrint("showMol.jsp: assignedQIds = ", assignedQIds);
	/**/
	final DateFormat respTimeFormatter = DateFormat.getDateTimeInstance(
			DateFormat.SHORT, DateFormat.MEDIUM);
	respTimeFormatter.setTimeZone(course.getTimeZone());
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<title><%= attemptNum == numAttempts 
		? "Last Attempt" : "Attempt " + attemptNum %></title>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<% if (isLewis) { %>
	<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/lewisJS.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<% } else if (isClickableImage || isDrawVectors) { %>
	<script src="<%= pathToRoot %>js/drawOnFig.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/position.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/wz_jsgraphics.js" type="text/javascript"></script>
<% } else if (isOED || isRCD) { %>
	<script src="<%= pathToRoot %>js/oedAndRcd.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/position.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/wz_jsgraphics.js" type="text/javascript"></script>
<% } else if (isEquations) { %>
	<script src="<%= pathToRoot %>js/equations.js" type="text/javascript"></script>
	<script type="text/x-mathjax-config">
		MathJax.Hub.Config({ TeX: { equationNumbers: {autoNumber: "AMS"} } });
	</script>
 	<script src="<%= pathToRoot %>nosession/mathjax/MathJax.js?config=TeX-AMS-MML_HTMLorMML.js" 
			type="text/javascript"></script>
<% } else if (isMarvin) { %>
	<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
	<script src="https://marvinjs.chemicalize.com/v1/<%= 
			AppConfig.marvinJSLicense %>/client-settings.js"></script>
	<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<% } // if question type %>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >

	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>

	function getMol() {
		return '<%= Utils.toValidJS(lastResp) %>';
	}

	function showSource() {
		openSourceCodeWindow('<%= pathToRoot %>includes/showSourceCode.jsp');
	} // showSource()

	function closeMe(e) {
		if (e && ([10, 13].contains(e.keyCode))) self.close();
	} // closeMe()

	<% if (isClickableImage || isDrawVectors) { 
		final String[] colorAndMaxMarks = 
				ClickImage.getColorAndMaxMarks(Utils.isEmpty(qData) 
					? null : qData[0].data); %>

 		<%@ include file="/js/drawOnFig.jsp.h" %>

		function initDrawOnFigure() {
			initDrawOnFigConstants();
			initDrawOnFigGraphics('<%= colorAndMaxMarks[ClickImage.COLOR] %>');
			disallowDrawing();
			<% if (!Utils.isEmpty(lastResp)) {
				if (isClickableImage) { %>
					var coords = new Array();
			<%		final ClickImage clickImage = new ClickImage(lastResp);
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
					Utils.alwaysPrint("showMol.jsp: vectors = ", vectors);
					for (final DPoint3[] vector : vectors) { %>
						drawonfig.allShapes[<%= DrawVectors.ARROW %>].push(
								[canvasSetX(<%= vector[DrawVectors.ORIGIN].x %>), 
								canvasSetY(<%= vector[DrawVectors.ORIGIN].y %>),
								canvasSetX(<%= vector[DrawVectors.TARGET].x %>), 
								canvasSetY(<%= vector[DrawVectors.TARGET].y %>)]);
			<% 		} // for each vector %>
					paintAll();
			<% 	} // if question type 
			} // if there's a last response to display %>
		} // initDrawOnFigure()

	<% } else if (isOED || isRCD) { %>

		function initED() {
			initGraphics(0);
			<% List<CellsLine> conLines;
			if (isOED) {
				oedResp.setOrbitals(lastResp); 
				conLines = oedResp.getLines();
			} else {
				rcdResp.setStates(lastResp, !RCD.THROW_IT); 
				conLines = rcdResp.getLines();
			} // if isOED
			for (int lineNum = 0; lineNum < conLines.size(); lineNum++) {
				final CellsLine line = conLines.get(lineNum);
				final int ARow = line.endPoints[0].getRow();
				final int ACol = line.endPoints[0].getColumn();
				final int BRow = line.endPoints[1].getRow();
				final int BCol = line.endPoints[1].getColumn(); %>
				initLine('d0r<%= ARow %>c<%= ACol %>', 'd0r<%= BRow %>c<%= BCol %>');
			<% } // for each line %>
			updateCanvas();
		} // initED()

	<% } else if (isLewis) { %>

		function initLewis() {
			initLewisConstants(
					[<%= LewisMolecule.CANVAS_WIDTH %>, 
						<%= LewisMolecule.CANVAS_HEIGHT %>],
					<%= LewisMolecule.MARVIN_WIDTH %>, 
					['<%= LewisMolecule.PAIRED_ELECS %>',
						'<%= LewisMolecule.UNPAIRED_ELECS %>',
						'<%= LewisMolecule.UNSHARED_ELECS %>' ],
					'<%= LewisMolecule.LEWIS_PROPERTY %>',
					'<%= LewisMolecule.HIGHLIGHT %>');
			loadLewisInlineImages('<%= pathToRoot %>', 
					[[getLewisImageMRV(getMol()), <%= qFlags %>]], 
					ADD_CLICK);
		} // initLewis()

	<% } else if (isMarvin) { %>

		function launchMView(height) {
			var url = new String.builder().
					append('<%= pathToRoot %>includes\/marvinJSViewer.jsp' +
						'?viewOpts=<%= qFlags %>&getMolMethodName=').
					append(encodeURIComponent('getMol()')).
					toString();
			openSketcherWindow(url);
			/* startMViewWebStart(getMol(), 
					'<%= pathToRoot %>', 
					'<%= user.getUserId() %>'); /**/
		} // launchMView()

	<% } // if question type %>
	// -->
</script>
</head>

<body onkeypress="closeMe(event);" 
		style="background-color:#f6f7ed; margin:0px; overflow:auto;" <%=
			isLewis ? "onload=\"initLewis();\""
			: isClickableImage || isDrawVectors ? "onload=\"initDrawOnFigure();\""
			: isOED || isRCD ? "onload=\"initED();\"" : "" %>>
	<table style="margin-left:auto; margin-right:auto; 
			background-color:#f6f7ed; width:378px;">
	<tr><td class="boldtext big" style="padding-left:10px; 
			padding-right:10px; padding-top:10px;">
		<% if (numAttempts > 1) { %>
			<form name="attemptForm" action="showMol.jsp">
			<input type="hidden" name="hwNum" value="<%= hwNum %>" />
			<input type="hidden" name="hwId" value="<%= hwId %>" />
			<input type="hidden" name="qId" value="<%= qId %>" />
			<input type="hidden" name="userId" 
					value="<%= Utils.toValidHTMLAttributeValue(studentId) %>" />
			<input type="hidden" name="isInstructorView" 
					value="<%= isInstructorView %>" />
			<%= user.translate("Question ***1*** Attempt", qNum) %>
			<select name="attemptNum" onchange="document.attemptForm.submit();">
				<% final int[] attemptNums = hwsession.getAttemptNums();
				for (int tryNum = 1; tryNum <= numAttempts; tryNum++) { %>
					<option value="<%= tryNum %>"
							<%= tryNum == attemptNum ? "selected=\"selected\"" : "" %>>
						<%= attemptNums[tryNum - 1] %> <%= tryNum == numAttempts 
								? "(" + user.translate("last") + ")" : "" %>
					</option>
				<% } // for each try %>
			</select>
			</form>
		<% } else { %>
			<%= user.translate("Question ***1*** Last Attempt", qNum) %>
		<% } // if there is more than one attempt %>
	</td><td class="boldtext" id="launchMViewCell" 
			style="text-align:right; padding-left:10px; 
				padding-right:10px; padding-top:10px; font-style:italic;">
		<% if (isLewis) { %>
			Click image to copy source
		<% } else if (isMarvin) { 
			int height = 350; // pixels
			if (thisQ.usesSubstns() && isInstructorView) {
				final String[] rGroups = hwsession.getCurrentSubstns();
				height -= 20;
				addlInfo = Utils.toDisplay(SubstnUtils.displayRGroups(rGroups), 
						Utils.SUPERSCRIPT_RGROUP_NUMS);
			} // if should show R groups 
		%>
			<a onclick="launchMView(<%= height %>);"><u>Launch MarvinJS&trade; viewer</u></a>
			or click image to copy source
		<% } // if is Marvin or Lewis %>
	</td></tr>
	<tr><td colspan="2" 
			style="text-align:center; padding-left:10px; padding-right:10px;">
		<% if (isMarvin) { 
		%>
			<table class="whiteTable" style="text-align:center;">
			<tr><td id="resp">
				<a onclick="showSource();"><%= evalResult.getImage(
						pathToRoot, user.prefersPNG(), qFlags) %></a>
			</td></tr>
			<%= addlInfo %>
			</table>
		<% } else if (isLewis) { %>
			<table class="whiteTable" style="text-align:center;">
			<tr><td id="fig"></td></tr>
			<%= addlInfo %>
			</table>
		<% } else if (isText || isFormula) { 
			final String text = Utils.toValidHTML(lastResp); %> 
			<table class="whiteTable" style="text-align:left;">
				<tr><td><%= chemFormatting ? Utils.toDisplay(text) : text %></td></tr>
			</table>
		<% } else if (isLogicalStmts) { 
			final Logic logicStmts = new Logic(lastResp); %>
			<%= logicStmts.convertToHTML() %>
		<% } else if (isChoice) { // choice or fillBlank 
			final Choice choiceResp = new Choice(lastResp);
			%>
			<table class="whiteTable" style="text-align:left;">
				<tr><td><%= choiceResp.displayChosen(qData, chemFormatting, 
						user.getLanguages()) %></td></tr>
			</table>
		<% } else if (isChooseExplain) { // choice or fillBlank 
			final ChooseExplain chooseExplainResp = new ChooseExplain(lastResp);
			%>
			<table class="whiteTable" style="text-align:left;">
				<tr><td><%= chooseExplainResp.choice.displayChosen(
						qData, chemFormatting, user.getLanguages()) %></td></tr>
				<tr><td><%= chemFormatting ? Utils.toDisplay(chooseExplainResp.text) 
						: chooseExplainResp.text %></td></tr>
			</table>
		<% } else if (isRank) { 
			final Rank rankResp = new Rank(lastResp); %>
			<table class="whiteTable" style="text-align:left;">
				<tr><td><%= rankResp.toDisplay(qData, chemFormatting, 
						user.getLanguages()) %></td></tr>
			</table>
		<% } else if (isNumeric) { 
			final Numeric numberResp = new Numeric(lastResp, qData); 
			if (thisQ.usesSubstns() && isInstructorView) {
				final String[] values = hwsession.getCurrentSubstns();
				addlInfo = Utils.toDisplay(SubstnUtils.displayValues(values));
			} // if should show values %>
			<table class="whiteTable" style="text-align:left;">
				<tr><td><%= numberResp.toDisplay() %></td></tr>
				<%= addlInfo %>
			</table>
		<% } else if (isOED) { %>
			<%= oedResp.toDisplay() %>
		<% } else if (isRCD) { %>
			<%= rcdResp.toDisplay() %>
		<% } else if (isTable) { 
			final TableQ tableQ = new TableQ(lastResp); %>
			<%= tableQ.convertToHTML(qData, chemFormatting, TableQ.DISPLAY) %>
		<% } else if (isClickableImage || isDrawVectors) { 
			final Figure[] figures = hwsession.getCurrentFigures(); 
			%>
			<div id="canvas" style="position:relative; left:0px; top:0px;">
			<img src="<%= pathToRoot + figures[0].bufferedImage %>" 
					id="clickableImage" class="unselectable" alt="picture" />
			</div>
		<% } else if (isEquations) { 
			final Equations eqnsObj = new Equations(lastResp); 
			final String eqnsFormatted = eqnsObj.getEntriesForMathJax(); %>
			<%= eqnsFormatted %>
		<% } else { // unknown %> 
			<table class="whiteTable" style="text-align:left;">
				<tr><td><%= lastResp %></td></tr>
			</table>
		<% } // if question type %>
		</td></tr>
		<tr><td class="regtext" style="padding-left:10px;">
			<%= respTimeFormatter.format(evalResult.timeOfResponse) 
					+ (!Utils.isEmpty(evalResult.ipAddr)
						? "; " + evalResult.ipAddr : "") %>
		</td></tr>
		<% if (!Utils.isEmpty(evalResult.comment)) { %>
			<tr><td colspan="2" class="regtext" style="padding-left:10px; 
					padding-right:10px; padding-top:10px; padding-bottom:10px;">
				<span class="boldtext"><%= user.translate("Instructor's comment") %>:</span>
				<%= chemFormatting ?  Utils.toDisplay(evalResult.comment) 
						: evalResult.comment %>
			</td></tr>
		<% } // if comment %>
	</table>
</body>
</html>
