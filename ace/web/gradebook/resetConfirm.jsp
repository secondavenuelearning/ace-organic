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
	com.epoch.session.HWSession,
	com.epoch.substns.SubstnUtils,
	com.epoch.synthesis.Synthesis,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
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
	final int hwId = MathUtils.parseInt(request.getParameter("hwId"));
	final int qNum = MathUtils.parseInt(request.getParameter("qNum"));
	final int qId = MathUtils.parseInt(request.getParameter("qId"));
	final String studentId = request.getParameter("userId");
	final String attemptNumStr = request.getParameter("attemptNum");
	final boolean isTutorial = "true".equals(request.getParameter("isTutorial"));

	HWSession hwsession;
	synchronized (session) {
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
			(thisQ.isMarvin() || thisQ.isMechanism() || isSynthesis); 
	final boolean isLewis = thisQ.isLewis();
	final boolean isRank = thisQ.isRank();
	final boolean isChoice = // or fillBlank
			(thisQ.isChoice() || thisQ.isFillBlank());
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
	final boolean usesSubstns = thisQ.usesSubstns();
	final boolean chemFormatting = thisQ.chemFormatting();

	final int numAttempts = hwsession.getNumResults();
	final int attemptNum = MathUtils.parseInt(attemptNumStr, numAttempts);
	/* Utils.alwaysPrint("resetConfirm.jsp: hwNum = ", hwNum,
			 ", hwId = ", hwId, ", qNum = ", qNum, ", qId = ", qId,
			 ", studentId = ", studentId, ", attemptNum = ", attemptNum,
			 ", numAttempts = ", numAttempts, ", usesSubstns = ", usesSubstns,
			 ", isTutorial = ", isTutorial); /**/

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

	double grade = 0.0;
	int tries = 0;
	String comment = "";
	String isNull = "checked=\"checked\"";
	grade = evalResult.grade;
	tries = (evalResult.tries == 0 ? 1 : evalResult.tries);
	comment = evalResult.comment;
	if (comment == null) comment = "";
	isNull = "";
	/* Utils.alwaysPrint("resetConfirm.jsp: grade = ", grade, 
			", tries = ", tries, ", isNull = ", isNull, ", comment = ", 
			comment, ", lastResp = ", lastResp); /**/
	final DateFormat respTimeFormatter = DateFormat.getDateTimeInstance(
			DateFormat.SHORT, DateFormat.MEDIUM);
	respTimeFormatter.setTimeZone(course.getTimeZone());
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<title>Alter Student Record</title>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<style type="text/css">
	#contents {
		position:fixed; 
		top:0px;
		left:0;
		bottom:45px; 
		right:0; 
		overflow:auto; 
	}

	#footer {
		position:absolute; 
		bottom:0; 
		left:0;
		width:100%; 
		height:45px; 
		overflow:auto; 
	}

</style>
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

	<% if (isClickableImage || isDrawVectors) { %>
 		<%@ include file="/js/drawOnFig.jsp.h" %>
	<% } %>

	function getMol() {
		return '<%= Utils.toValidJS(lastResp) %>';
	}

	function showSource() {
		openSourceCodeWindow('<%= pathToRoot %>includes/showSourceCode.jsp');
	} // showSource()

	function init() {
		<% if (evalResult == null && usesSubstns) { %>
			setInnerHTML('preserveSubstnsRow', preserveSubstnsRow());
		<% } else { %>
			clearInnerHTML('preserveSubstnsRow');
		<% } // evalResult null %>
		<% if (isClickableImage || isDrawVectors) { 
			final String[] colorAndMaxMarks = 
					ClickImage.getColorAndMaxMarks(Utils.isEmpty(qData) 
						? null : qData[0].data); %>
			initDrawOnFigConstants();
			initDrawOnFigGraphics('<%= colorAndMaxMarks[ClickImage.COLOR] %>');
			disallowDrawing();
			var coords = new Array();
		<% 	if (!Utils.isEmpty(lastResp)) {
				if (isClickableImage) { %>
					var coords = new Array();
		<%			final ClickImage clickImage = new ClickImage(lastResp);
					final int[][] allCoords = clickImage.getAllCoords();
					for (final int[] coords : allCoords) { %>
						drawonfig.allShapes[<%= ClickImage.MARK %>].push(
								[<%= coords[ClickImage.X] %>, 
								<%= coords[ClickImage.Y] %>]);
			<%		} // for each cross %>
					paintAll();
		<%	 	} else { // isDrawVectors 
					final DrawVectors drawVectors = new DrawVectors(lastResp);
					final DPoint3[][] vectors = drawVectors.getVectorPoints();
					for (final DPoint3[] vector : vectors) { %>
						drawonfig.allShapes[<%= DrawVectors.ARROW %>].push(
								[canvasSetX(<%= vector[DrawVectors.ORIGIN].x %>), 
								canvasSetY(<%= vector[DrawVectors.ORIGIN].y %>),
								canvasSetX(<%= vector[DrawVectors.TARGET].x %>), 
								canvasSetY(<%= vector[DrawVectors.TARGET].y %>)]);
		<%	 		} // for each vector %>
					paintAll();
		<% 		} // if question type %>
		<% 	} // if there's a last response to display %>
		<% } else if (isOED || isRCD) { %>
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
		<% } else if (isLewis) { %>
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
		<% } // question type %>
	} // init()

	var setToNull = false;
	var newGrade = '<%= grade %>';
	var newAttempts = '<%= tries %>';
	function toggle() {
		setToNull = !setToNull;
		if (setToNull) {
			newGrade = getValue('newGrade');
			newAttempts = getValue('newAttempts');
			setValue('newGrade', '');
			setValue('newAttempts', '');
			disableCell('newGrade');
			disableCell('newAttempts');
			<% if (usesSubstns) { %>
				setInnerHTML('preserveSubstnsRow', preserveSubstnsRow());
			<% } // if uses R groups %>
		} else {
			setValue('newGrade', newGrade);
			setValue('newAttempts', newAttempts);
			enableCell('newGrade');
			enableCell('newAttempts');
			<% if (usesSubstns) { %>
				clearInnerHTML('preserveSubstnsRow');
			<% } // if uses R groups %>
		}
	} // toggle

	function preserveSubstnsRow() {
		return '<td id="preserveSubstns1" class="regtext" '
				+ 'style="padding-bottom:10px;">'
				+ '<b><%= user.translateJS("Preserve assigned substitutions "
					+ "(R groups, variable values?") %><\/b>' 
				+ '<\/td><td id="preserveSubstns2" class="regtext" '
				+ 'style="padding-bottom:10px;">'
				+ '<input type="checkbox" id="preserveSubstns" '
				+ 'name="preserveSubstns" \/><\/td>';
	} // preserveSubstnsRow()

	/* most recent response, change grade: just change the grade.
	 * most recent response, decrease tries from x to y: 
	 * 			delete responses y to x – 1, 
	 * 			make most recent response have y tries
	 * most recent response, increase tries from x to y:
	 * 			make most recent response have y tries
	 * earlier response x, change grade: delete responses x + 1 and up, 
	 * 			change grade of response x
	 * earlier response x, decrease or increase tries: not allowed
	 */
	function submitIt() {
		var form = document.resetForm;
		var newGrade = form.newGrade.value;
		if (!setToNull && (isWhiteSpace(newGrade)
				|| !canParseToFloat(newGrade)
				|| parseFloat(newGrade) < 0
				|| parseFloat(newGrade) > 1)) {
			toAlert('<%= user.translateJS("Please enter a value " 
					 + "between 0 and 1 inclusive for the grade.") %>');
			return;
		}
		if (newGrade !== form.oldGrade.value
				&& <%= attemptNum != numAttempts %>) {
			if (!toConfirm('<%= user.translateJS("Changing the grade "
					+ "of an earlier attempt will cause ACE to erase "
					+ "the records of all subsequent attempts so that "
					+ "the attempt whose grade you are changing becomes "
					+ "the most recent attempt. Are you sure you want "
					+ "to proceed?") %>')) {
				return;
			} // if not confirming
		}
		var newAttempts = form.newAttempts.value;
		if (!setToNull && (isWhiteSpace(newAttempts)
				|| !canParseToInt(newAttempts)
				|| parseInt(newAttempts) < 0)) { // <!-- >
			toAlert('<%= user.translateJS("Please enter a nonnegative " 
					 + "integer for the number of attempts.") %>');
			return;
		}
		form.submit();
	} // submitIt()

	<% if (isMarvin) { %>

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

	<% } // if isMarvin %>

	// -->
	</script>
</head>
<body style="background-color:#e0e6c2; margin:0px;" onload="init();">
	<div id="contents">
	<table style="width:95%; background-color:#e0e6c2; text-align:center; 
			margin-left:auto; margin-right:auto;">
		<tr>
			<td class="boldtext big" style="padding-left:10px; padding-top:10px;">
				<%= user.translate(
						"Alter Student Record for Question ***1***", qNum) %>
			</td>
		</tr>
		<tr>
			<td class="regtext" style="padding-left:10px; 
					padding-right:10px; padding-top:10px;">
				<%= user.translate(
					"Use this function to alter the record of a student's grade "
					+ "for question ***1*** in the current assignment or the "
					+ "number of times that the student has responded to it.", qNum)
					+ " " 
				+ user.translate(
					"<b>Note</b>: If you have chosen to apply attempt- or time-dependent "
					+ "grading to the assignment, the grade that you see below may "
					+ "differ from the grade that you see in the gradebook. If you "
					+ "enter a new grade below, ACE will apply attempt- or "
					+ "time-dependent grading to it when it displays the "
					+ "gradebook again.") %>
				<br/>
			</td>
		</tr>
	</table>
	<table style="width:400px; text-align:center; margin-left:auto; margin-right:auto;">
		<tr><td class="boldtext enlarged" style="padding-left:10px; 
				padding-right:10px; padding-top:10px;">
		<% if (numAttempts > 1) { %>
			<form name="attemptForm" action="resetConfirm.jsp">
			<input type="hidden" name="hwNum" value="<%= hwNum %>" />
			<input type="hidden" name="hwId" value="<%= hwId %>" />
			<input type="hidden" name="qNum" value="<%= qNum %>" />
			<input type="hidden" name="qId" value="<%= qId %>" />
			<input type="hidden" name="isTutorial" value="<%= isTutorial %>" />
			<input type="hidden" name="userId" 
					value="<%= Utils.toValidHTMLAttributeValue(studentId) %>" />
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
				if (thisQ.usesSubstns()) {
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
		<tr><td colspan="2" style="text-align:center; padding-left:10px; 
				padding-right:10px;">
			<% if (isMarvin) { %>
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
				final Choice choiceResp = new Choice(lastResp); %>
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
					<tr><td><%= chemFormatting 
							? Utils.toDisplay(chooseExplainResp.text) 
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
				if (thisQ.usesSubstns()) {
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
	</table>
	<form name="resetForm" action="doReset.jsp" method="post" accept-charset="UTF-8">
		<input type="hidden" name="hwNum" value="<%= hwNum %>" />
		<input type="hidden" name="hwId" value="<%= hwId %>" />
		<input type="hidden" name="qId" value="<%= qId %>" />
		<input type="hidden" name="isTutorial" value="<%= isTutorial %>" />
		<input type="hidden" name="userId" 
				value="<%= Utils.toValidHTMLAttributeValue(studentId) %>" />
		<input type="hidden" name="attemptNum" value="<%= attemptNum %>" />
		<input type="hidden" name="numAttempts" value="<%= numAttempts %>" />
		<input type="hidden" name="oldGrade" value="<%= grade %>" />
		<% if (attemptNum != numAttempts) { %>
			<input type="hidden" id="newAttempts" name="newAttempts" value="<%= tries %>" />
		<% } // if not allowed to change number of tries %>
	<table style="width:400px; text-align:center; margin-left:auto; margin-right:auto;">
		<tr>
			<td class="regtext" style="padding-left:10px; padding-right:10px;">
				<table>
					<tr>
					<td class="regtext" style="padding-bottom:10px;">
						<b><%= user.translate("Grade") %></b>
						(<%= user.translate("between 0 and 1 inclusive") %>): 
					</td>
					<td class="regtext" style="padding-bottom:10px;">
						<input type="text" id="newGrade" name="newGrade" size="8" 
								value="<%= grade %>" />
					</td>
					</tr>
					<% if (attemptNum == numAttempts) { %>
						<tr>
						<td class="regtext" style="padding-bottom:10px;">
							<b><%= user.translate("Number of attempts") %></b> 
							(<%= user.translate("0 or more") %>): 
						</td>
						<td class="regtext" style="padding-bottom:10px;">
							<input type="text" id="newAttempts" name="newAttempts" 
									size="5" value="<%= tries %>" />
						</td>
						</tr>
						<tr>
						<td class="regtext" style="padding-bottom:10px;">
							<%= user.translate("Or <b>set to null</b>") %>: 
						</td>
						<td class="regtext" style="padding-bottom:10px;">
							<input type="checkbox" id="setToNull" name="setToNull"
									onchange="toggle();" <%= isNull %>/>
						</td>
						</tr>
						<tr id="preserveSubstnsRow">
						</tr>
					<% } // if the most recent attempt %>
					<tr>
					<td colspan="2" class="regtext" style="padding-bottom:10px;">
						<table>
						<tr>
						<td style="vertical-align:middle">
							<%= user.translate("Comment") %>: 
						</td>
						<td class="regtext" style="padding-bottom:10px;">
							<textarea id="comment" name="comment" 
									style="overflow:auto;" cols="40" rows="4"><%= 
								Utils.toValidTextbox(comment) %></textarea>
						</td>
						</tr>
						</table>
					</td>
					</tr>
					<tr>
					<td colspan="2" class="regtext" style="padding-bottom:10px; height:40px;">
						(<%= user.translate(attemptNum == numAttempts
								? "Set to zero attempts, not to null, if the question "
									+ "appears different to different students; otherwise, "
									+ "this student will see a different version of the "
									+ "same question."
								: "If you change the grade of this response, ACE will "
									+ "erase the records of all attempts more recent "
									+ "than this one, and this response will become "
									+ "the most recent one.") %>)
					</td>
					</tr>
				</table>
			</td>
		</tr>
	</table>
	</form>
	</div>
	<div id="footer">
	<table style="margin-left:auto; margin-right:auto;">
		<tr>
		<td style="padding-bottom:10px;">
			<%= makeButton(user.translate("Alter this record now"), "submitIt();") %>
		</td>
		<td style="padding-bottom:10px;">
			<%= makeButton(user.translate("Cancel"), "self.close();") %>
		</td>
		</tr>
	</table>
	</div>
</body>
</html>
