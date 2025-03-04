<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.chem.MolString,
	com.epoch.energyDiagrams.EDiagram,
	com.epoch.energyDiagrams.DiagramCell,
	com.epoch.energyDiagrams.OED,
	com.epoch.energyDiagrams.RCD,
	com.epoch.energyDiagrams.CellsLine,
	com.epoch.genericQTypes.Choice,
	com.epoch.genericQTypes.ChooseExplain,
	com.epoch.genericQTypes.Numeric,
	com.epoch.genericQTypes.Rank,
	com.epoch.genericQTypes.TableQ,
	com.epoch.lewis.LewisMolecule,
	com.epoch.qBank.QDatum,
	com.epoch.qBank.Question,
	com.epoch.responses.StoredResponse,
	com.epoch.session.QSet,
	com.epoch.synthesis.Synthesis,
	com.epoch.utils.Utils,
	java.util.List"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../";

	Question question;
	QSet qSet;
	synchronized (session) {
		question = (Question) session.getAttribute("qBuffer");
		qSet = (QSet) session.getAttribute("qSet");
	}
	final boolean isLewis = question.isLewis();
	final boolean isSynthesis = question.isSynthesis();
	final boolean showMapping = question.showMapping();
	final boolean isOED = question.isOED();
	final boolean isRCD = question.isRCD();
	final boolean chemFormatting = question.chemFormatting();
	final QDatum[] qData = question.getQData(Question.GENERAL);

	int mviewNum = 0;
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<title>ACE View All Responses</title>
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<% if (isOED || isRCD) { %>
	<script src="<%= pathToRoot %>js/oedAndRcd.js" type="text/javascript"></script>
<% } else if (isLewis) { %>
	<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/lewisJS.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/svgGraphics.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<% } // if question type %>
<script src="<%= pathToRoot %>js/position.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/wz_jsgraphics.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= 
		AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
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

	#qEditorContents {
		position:fixed; 
		top:55px;
		left:0;
		bottom:40px; 
		right:0; 
		overflow:auto; 
	}

	* html body {
		padding:55px 0 40px 0; 
	}

	* html #footer {
		height:100%; 
	}

	* html #qEditorContents {
		height:100%; 
	}
</style>
<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>

	function reloadAuthorFrame(responseCorrectnessJS, 
			responseIndex, evaluatorMajorIndex) {
		var go = 'question.jsp?qId=same&addans=yes&responseIndex=' 
				+ responseIndex + '&evaluatorMajorIndex=' + evaluatorMajorIndex
				+ '&responseCorrectness=' + responseCorrectnessJS;
		// alert(go);
		self.location.href = go;
	}

	function goBackAgain() {
        self.location.href = "question.jsp?qId=same";
	}

	<% if (isLewis) { %>

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
			var figuresData = [];
			<% final long qFlags = question.getQFlags();
			for (int correctnessIdx = 0; correctnessIdx < 2; correctnessIdx++) { 
				final int param = (correctnessIdx == 0 ? 
						StoredResponse.CORRECT : StoredResponse.WRONG);
				final StoredResponse[] resps = qSet.getStoredResponses(param);
				if (!Utils.isEmpty(resps)) {
					for (int respNum = 0; respNum < resps.length; respNum++) { %>
						figuresData.push([getValue('figData<%= 
									correctnessIdx %>_<%= respNum %>'),
								<%= qFlags %>,
								'<%= correctnessIdx %>_<%= respNum %>']);
			<%		} // for each response
				} // if there are responses
			} // for correct and not correct
			%>
			if (!isEmpty(figuresData)) {
				loadLewisInlineImages('<%= pathToRoot %>', figuresData);
			}
		} // initLewis()

	<% } // if isLewis %>

	// -->
</script>
</head>

<body class="light" <%= isLewis ? "onload=\"initLewis();\"" : "" %>
		style="text-align:center; margin:0px; margin-top:5px;
		background-color:white; overflow:auto;">
<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

<div id="qEditorContents">
<table class="regtext"
		style="width:626px; margin-left:auto; margin-right:auto; 
		border-style:none; border-collapse:collapse;">
<tr>
<td class="boldtext big" style="padding-top:10px;">
	View students' responses for question <%= question.getQId() %>
</td>
</tr>
<tr>
<td class="regtext">
	<br/>If you have just modified the question's evaluators,
	your changes will be manifested here only if you saved them
	before pressing the View Responses button. You may need to
	exit the Question Bank and reenter it to see the changes.
</td></tr>
<%
int numCorrect = 0;
for (int correctnessIdx = 0; correctnessIdx < 2; correctnessIdx++) { 
	// once for right, once for wrong responses
	final int param = (correctnessIdx == 0 ? 
			StoredResponse.CORRECT : StoredResponse.WRONG);
	final String responseCorrectness = (correctnessIdx == 0 ? 
			"correct" : "wrong");
	final StoredResponse[] resps = qSet.getStoredResponses(param);
	if (correctnessIdx == 0) numCorrect = resps.length;
	if (resps == null) {
%>
		<script type="text/javascript">
			alert('Sorry, we cannnot acquire the data at this time. '
					+ 'Try again later.');
			goBackAgain();
		</script>
<%	} // if responses are null
%>
	<tr>
	<td class="boldtext enlarged" style="padding-top:10px">
		<%= correctnessIdx == 0 ? 
				"Correct responses" : "Incorrect responses" %>
	</td>
	</tr>
	<tr>
	<td style="padding-bottom:10px; text-align:left;">
		<table class="whiteTable"
				style="width:100%; background-color:#f6f7ed; border-collapse:collapse;">
			<% if (Utils.isEmpty(resps)) {  %>
				<tr class="greenrow"><td>
					No responses recorded.
				</td></tr>
			<% } else for (int respNum = 0; respNum < resps.length; respNum++) { 
					// each response to this Q %>
				<tr class="<%= respNum % 2 == 0 ? "greenrow" : "whiterow" %>">
				<td>
					<table style="width:100%; padding-left:10px; padding-right:10px;
							padding-bottom:5px; padding-top:5px; margin-left:auto;
							margin-right:auto;">
					<tr style="width:100%;">
					<td class="whiteTable" 
							style="padding-left:5px; width:25%; text-align:left;">
	                	<% final String origMol = resps[respNum].response;
                       	if (question.isText() || question.isFormula()) { %>
                            <%= chemFormatting ? Utils.toDisplay(origMol) : origMol %>
                       	<% } else if (question.isChoice()
								|| question.isFillBlank()) {
							final Choice choiceResp = new Choice(origMol, 
									question.getNumQData(Question.GENERAL)); %>
                            <%= choiceResp.displayChosen(qData, chemFormatting) %>
                       	<% } else if (question.isChooseExplain()) {
							final ChooseExplain chooseExplainResp = 
									new ChooseExplain(origMol, 
										question.getNumQData(Question.GENERAL)); %>
                            <%= chooseExplainResp.choice.displayChosen(qData, chemFormatting) %>
							<br />
                           	<%= chemFormatting ? Utils.toDisplay(chooseExplainResp.text) 
									: chooseExplainResp.text %>
                       	<% } else if (question.isRank()) {
							final Rank rankResp = new Rank(origMol); %>
                           	<%= rankResp.toDisplay(qData, chemFormatting) %>
                        <% } else if (question.isNumeric()) {
							final Numeric numberResp = new Numeric(origMol, qData); %>
                            <%= numberResp.toDisplay() %>
						<% } else if (question.isTable()) { 
							final TableQ tableQ = new TableQ(origMol); %>
							<%= tableQ.convertToHTML(qData, chemFormatting, TableQ.DISPLAY) %>
                        <% } else if (isOED || isRCD) {
							Utils.alwaysPrint("FIHIstory.jsp: response ", respNum,
									": ", origMol);
							final OED oedResp = (isOED ? new OED(qData) : null); 
							final RCD rcdResp = (isRCD ? new RCD(qData) : null); 
							final int canvasNum = 
									correctnessIdx * numCorrect + respNum; 
							List<CellsLine> conLines;
							if (isOED) {
								oedResp.setOrbitals(origMol); 
								conLines = oedResp.getLines();
							} else {
								rcdResp.setStates(origMol); 
								conLines = rcdResp.getLines();
							} // if isOED %>
                            <%= isOED ? oedResp.toDisplay(canvasNum) 
									: rcdResp.toDisplay(canvasNum) %>
							<script type="text/javascript">
								// <!-- >
								initGraphics(<%= canvasNum %>);
								<% for (int lNum = 0; lNum < conLines.size(); lNum++) {
									final CellsLine line = conLines.get(lNum);
									final int ARow = line.endPoints[0].getRow();
									final int ACol = line.endPoints[0].getColumn();
									final int BRow = line.endPoints[1].getRow();
									final int BCol = line.endPoints[1].getColumn();
									final String cellAId = Utils.toString('d', 
											canvasNum, 'r', ARow, 'c', ACol);
									final String cellBId = Utils.toString('d', 
											canvasNum, 'r', BRow, 'c', BCol); %>
									initLine('<%= cellAId %>', '<%= cellBId %>');
								<% } // for each line %>
								updateCanvas();
								// -->
							</script>
                        <% } else if (isLewis) {
							final boolean isMRV = origMol.startsWith("<?xml");
							final String respMod = (isMRV ? origMol
									: MolString.convertMol(origMol, MolString.MRV,
									LewisMolecule.CANVAS_WIDTH / LewisMolecule.MARVIN_WIDTH));
						%>
							<span id="fig<%= correctnessIdx %>_<%= respNum %>">
							<input type="hidden" id="origNotMRV<%= correctnessIdx 
										%>_<%= respNum %>" 
									value="<%= !isMRV %>" %/>
							<input type="hidden" id="figData<%= correctnessIdx 
										%>_<%= respNum %>"
									value="<%= Utils.toValidHTMLAttributeValue(
										respMod) %>" />
							</span>
                        <% } else { // skeletal, mapping, mechanism, synthesis, Lewis 
							final String figId = Utils.toString(correctnessIdx == 0
									? "correct" : "wrong", "Resp", respNum);
						%>
							<table summary="">
							<tr><td id="<%= figId %>" class="whiteTable">
								<%= MolString.getImage(pathToRoot, 
										origMol, question.getQFlags(), 
										figId, user.prefersPNG()) %>
							</td></tr>
							<% if (isSynthesis) { %>
								<%= Synthesis.getRxnsDisplay(origMol) %>
							<% } // synthesis %>
							</table>
	                	<% } // if question type %>
                		</td>
					<td style="padding-left:10px; text-align:left;">
						<div class="boldtext">
						Number of occurrences: <%= resps[respNum].numEntries %>
						</div>
						<% if (resps[respNum].feedbackExists) { %>
							<p class="regtext" style="color:#566dbe;">
								<%= chemFormatting 
										? Utils.toDisplay(resps[respNum].feedback) 
										: resps[respNum].feedback %> 
							</p>
						<% } else { %>
							<p class="boldtext" style="color:#ff5151;"> 
								Not an expected response.
							</p>
						<% } // if feedback exists %>
					</td>
					<td style="text-align:right;">
						<table>
						<tr><td class="boldtext" style="padding-bottom:6px">
							For this response:
						</td></tr>
						<tr><td style="padding-bottom:6px; text-align:right;">
							<%= makeButton("Add new evaluator", 
									"reloadAuthorFrame('", responseCorrectness, 
									"', ", respNum, ", 0);") %>
							<% if (resps[respNum].feedbackExists) { %>
								</td></tr>
								<tr><td style="text-align:right;">
								<%= makeButton("Modify existing evaluator", 
										"reloadAuthorFrame('", responseCorrectness, 
										"', ", respNum, ", ", 
										resps[respNum].matchingEvaluatorMajorId, ");") %>
							<% } // feedbackExists %>
						</td></tr>
					</table>
				</td>
				</tr>
				<% if (isOED || isRCD) { %>
				<script type="text/javascript">
					updateCanvas();
				</script>
				<% } %>
			</table>
		</td>
		</tr>
		<% } // for each respNum %>
	</table>
</td>
</tr>
<% } // for each correctnessIdx %>
</table>
</div>

<div id="footer">
<table style="margin-left:auto; margin-right:auto; border-collapse:collapse;">
<tr>
<td><%= makeButton("Back to Question", "goBackAgain();") %></td>
</tr>
</table>
</div>

</body>
</html>
