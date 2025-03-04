<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	chemaxon.struc.Molecule,
	com.epoch.chem.MolString,
	com.epoch.evals.EvalResult,
	com.epoch.genericQTypes.Choice,
	com.epoch.genericQTypes.ChooseExplain,
	com.epoch.genericQTypes.Rank,
	com.epoch.lewis.LewisMolecule,
	com.epoch.qBank.Figure,
	com.epoch.qBank.QDatum,
	com.epoch.qBank.Question,
	com.epoch.session.HWSession,
	com.epoch.synthesis.Synthesis,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>

<%
	response.setHeader("Cache-Control","no-store, must-revalidate"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final int UNATTEMPTED = 0;
	final int SAVED = 1;
	final int CORRECT = 2;
	final int PART_RIGHT = 3;
	final int WRONG = 4;
	final int HUMAN_NEEDED = 5;

	HWSession hwsession;
	synchronized (session) {
		hwsession = (HWSession) session.getAttribute("hwsession");
	}
	if (hwsession == null) {
		%> <jsp:forward page="../errormsgs/noSession.html"/> <%
	}
	final int numQs = hwsession.getCount();
	final Assgt assgt = hwsession.getHW();
	final boolean dueDatePast = 
			hwsession.isDueDatePast() && !assgt.recordAfterDue();
	final boolean isInstructorOrTA = hwsession.isUserInstructorOrTA();
	final boolean isMasteryAssgt = assgt.isMasteryAssgt();
	final boolean allowUnlimitedTries = assgt.allowUnlimitedTries();
	final int maxTries = assgt.getMaxTries();
	final boolean printable = "true".equals(request.getParameter("printable"));
	final int resetNum = MathUtils.parseInt(request.getParameter("resetNum"), -1);
	if (resetNum > 0) {
		final String[][] oldNewSubstns = hwsession.resetSubstns(resetNum);
		/* Utils.alwaysPrint("list.jsp: set R groups for Q ", resetNum,
				" from practice similar groups ", oldNewSubstns[0],
				" back to ", oldNewSubstns[1]); /**/
	} // if resetting R groups
	final double ptsAllQs = assgt.allQsPointValue();
	final boolean allQsWorthSame = ptsAllQs > 0;
	final String[] ptsPerQ = (allQsWorthSame 
			? new String[] {assgt.allQsPointString()}
			: assgt.getMaxPointsPerQArray());
	/* Utils.alwaysPrint("list.jsp: assgt.allQsPointString() = ",
			assgt.allQsPointString(), ", ptsAllQs = ",
			ptsAllQs, ", allQsWorthSame = ", allQsWorthSame, 
			", ptsPerQ = ", ptsPerQ); */
	int jmolNum = 0;
	int mviewNum = 0;
	final int courseId = course.getId();
	final boolean notBlocked = !user.isBlockedFromForum(courseId); 
	final boolean showClock = "true".equals(request.getParameter("showClock"));
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<title>ACE Assignment Questions List</title>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<style type="text/css">
	* html body {
		padding:100px 0 0px 0; 
	}
</style>
<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/clock.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jmolStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/lewisJS.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= 
		AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
	<!-- the next two resources must be called in the given order -->
<script src="<%= pathToRoot %>nosession/jsmol/JSmol.min.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>nosession/jsmol/Jmol2.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/navigation/courseSidebarJS.jsp.h" %>
	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>

	function openQuestion(qNum, mode, uriPts, solveRelatedMasteryQ) {
		var bld = new String.builder().append('jumpGo.jsp?qNum=').
				append(qNum).append('&mode=').append(mode).
				append('&showClock=').append(showTimeRemaining);
		if (uriPts !== 0) {
			bld.append('&pts=').append(uriPts);
		}
		if (solveRelatedMasteryQ) {
			bld.append('&solveRelatedMasteryQ=true');
		}
		document.location.href = bld.toString();
	} // openQuestion()

	function openForumTopic(topicId, qId) {
		var bld = new String.builder().append('<%= pathToRoot %>forum/');
		if (topicId === 0) bld.append('addPost.jsp?');
		else {
			bld.append('posts.jsp?topicId=').append(topicId).append('&');
		}
		bld.append('linkedHWId=<%= assgt.id %>&linkedQId=').append(qId);
		document.location.href = bld.toString();
	} // openForumTopic()

	<% if (hwsession.isExam()) { %>
		<%@ include file="/js/setUpClock.jsp.h" %>
	<% } // if is an exam %>

	jmolInitialize('<%= pathToRoot %>nosession/jmol'); 

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
		<% for (int qNum = 1; qNum <= numQs; qNum++)  {
			final Question oneQ = hwsession.getQuestion(qNum);
			for (int figNum = 1; figNum <= oneQ.getNumFigures(); figNum++) { %>
				if (cellExists('figData<%= qNum %>_<%= figNum %>')) {
					figuresData.push([
							getValue('figData<%= qNum %>_<%= figNum %>'),
							getValue('figQFlags<%= qNum %>_<%= figNum %>'),
							'<%= qNum %>_<%= figNum %>']);
				} // if figure is Lewis
		<%	} // for each figure
		} // for each question %>
		if (!isEmpty(figuresData)) {
			loadLewisInlineImages('<%= pathToRoot %>', figuresData);
		}
	} // initLewis()

	function closeHW() {
		<% for (int qNum = 1; qNum <= numQs; qNum++) {
			final EvalResult evalResult = hwsession.getResult(qNum); 
			if (evalResult != null 
					&& evalResult.status == EvalResult.SAVED) { %>
				toAlert('<%= user.translateJS(
						"You have saved at least one response without "
						+ "submitting it for evaluation. Remember to submit "
						+ "it later, or you will not receive credit.") %>');
			<% break;
			} // if submitted but not saved
		} // for each question %>
		document.location.href = '<%= pathToRoot %>hwcreator/hwSetList.jsp';
	} // closeHW()

	// -->
</script>
</head>
<body style="text-align:center; margin:0px; overflow:auto;" 
		onload="setTab('<%= toTabName(user.translateJS("Assignments")) %>');
				initLewis(); <%= hwsession.isExam() ? "setUpClock();" : "" %><%=
				showClock ? " showClockRemaining();" : "" %>">

	<% if (!printable) { %>
		<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
		<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>
		<div id="contentsWithTabsWithoutFooter">
	<% } // if not printable %>
	<table style="width:90%; margin-left:auto; margin-right:auto;
			text-align:left;">
		<tr>
			<td class="regtext">
				<table style="width:100%;">
				<tr><td>
					<span class="boldtext big" >
						<%= Utils.toDisplay(assgt.getName()) %>
					</span><br/>
						<%= isMasteryAssgt
							? user.translate("Maximum allowed tries to demonstrate mastery")
							: user.translate("Maximum allowed tries per question") %>: <b>
						<%= allowUnlimitedTries ? user.translate("Unlimited")
								: maxTries %></b><br/>
					<% if (allQsWorthSame && ptsAllQs != 1) { %>
						<b><%= user.translate("Each question is worth "
								+ "***2*** points.", ptsPerQ[0]) %></b><br/>
					<% } // if not every Q worth 1 point
					if (!Utils.isWhitespace(assgt.getRemarks())) { %>
						<i><%= Utils.toDisplay(assgt.getRemarks()) %></i>
					<% } // if there are remarks %>
				</td>
				<% if (hwsession.isExam() && !dueDatePast && !printable) { %>
					<td class="boldtext big" style="text-align:right;">
						<span id="clockRemaining"></span>
					</td>
				<% } // if is exam %>
				<% if (!printable) { %>
					<td style="text-align:right;">
						<table style="margin-left:auto; margin-right:0px;">
							<tr>
							<td style="padding-left:10px;">
								<%= makeButton(user.translate("Printable List"),
									"openPrintableList('list.jsp?printable=true');") %>
							</td>
							</tr>
						</table>
					</td> 
				<% } // not the printable list %>
				</tr>
				</table>
			</td>
		</tr>
	</table>

	<table class="whiteTable" style="width:90%; border-collapse:collapse;
			 text-align:left;">
		<%
		for (int qNum = 1; qNum <= numQs; qNum++)  {
			hwsession.setCurrentIndex(qNum);
			final Question oneQ = hwsession.getCurrentQuestion();
			final int qId = oneQ.getQId();
			final long qFlags = oneQ.getQFlags();
			final boolean isChoice = oneQ.isChoice();
			final boolean isChooseExplain = oneQ.isChooseExplain();
			final boolean isFillBlank = oneQ.isFillBlank();
			final boolean isRank = oneQ.isRank();
			final boolean isNumeric = oneQ.isNumeric();
			final boolean usesSubstns = oneQ.usesSubstns();
			final boolean chemFormatting = oneQ.chemFormatting();
			final QDatum[] qData = (isRank || isChoice || isFillBlank 
					|| isChooseExplain ? oneQ.getQData(Question.GENERAL) : null);
			final Figure[] figures = hwsession.getCurrentFigures();
			final String[] substnStrs = (usesSubstns
					? hwsession.getCurrentSubstns() : null);
			EvalResult evalResult = hwsession.getCurrentResult(); 
			boolean haveResponse = evalResult != null;
			final int respStatus = (!haveResponse 
						|| Utils.among(evalResult.status, EvalResult.INITIALIZED, 
							EvalResult.NO_STATUS) ? UNATTEMPTED 
					: evalResult.status == EvalResult.HUMAN_NEEDED ? HUMAN_NEEDED 
					: evalResult.status == EvalResult.SAVED ? SAVED 
					: evalResult.grade == 1.0 ? CORRECT
					: evalResult.grade > 0.0 ? PART_RIGHT
					: WRONG);
			if (!haveResponse || !hwsession.formatOK(evalResult.lastResponse)) {
				final boolean badFormat = haveResponse;
				// Returns value only for rank, choice, choose-explain, and fillblank.
				// Value is stored as response 0.
				// Student: response 0 stored in DB; 
				// instructor: response 0 kept for this HWSession only.
				evalResult = hwsession.initializeStudentView();
				haveResponse = evalResult != null;
				if (badFormat) Utils.alwaysPrint("list.jsp: bad format for "
						+ "response to Q", qNum, "; new lastResp = ", 
						haveResponse ? evalResult.lastResponse : "null");
			} // if there's no previous result
			String lastResp = (haveResponse && evalResult.lastResponse != null 
					? evalResult.lastResponse : "");
			final boolean okToDisplay = hwsession.getOkToDisplay(qNum);
			final String rowColor = (qNum % 2 == 0 ? "whiterow" : "greenrow");
			/* Utils.alwaysPrint("list.jsp: for Q ", qNum,
					", qId = ", qId,
					", okToDisplay = ", okToDisplay,
					", status = ", haveResponse ? evalResult.status : "null",
					haveResponse ? ", grade = " + evalResult.grade : "",
					", lastResp =\n", lastResp
					); /**/
		%>
			<tr class="<%= rowColor %>">
			<td class="regtext" style="padding-left:10px; padding-top:10px; 
					padding-right:10px; padding-bottom:10px;">
				<a name="Q<%= qId %>"></a>
		<% 		if (respStatus > 0) { 
					final String imgSrc =
							(respStatus == SAVED ? "savedUnsubmitted" 
							: respStatus == HUMAN_NEEDED ? "threeQMarks" 
							: respStatus == CORRECT ? "b_check_green" 
							: respStatus == PART_RIGHT ? "b_check_orange" 
							: "b_check_red");
					final String imgTitle =
							(respStatus == SAVED 
								? "saved but not submitted for evaluation" 
							: respStatus == HUMAN_NEEDED 
								? "requires grading by instructor" 
							: respStatus == CORRECT ? "correct" 
							: respStatus == PART_RIGHT ? "partially correct" 
							: "incorrect"); 
		%>
					<img src="<%= pathToRoot %>images/<%= imgSrc %>.gif"
							title="<%= imgTitle %>" alt="picture" />
		<%			if (isMasteryAssgt) {
						final String masteryResult = (
								respStatus == CORRECT 
									&& evalResult.tries <= maxTries ? "mastered" 
								: Utils.among(respStatus, SAVED, HUMAN_NEEDED)
									&& evalResult.tries <= maxTries ? "tbd_mastered"
								: "unmastered"); 
		%>
						<img src="<%= pathToRoot %>images/<%= masteryResult %>.png"
								title="<%= masteryResult %>" alt="picture" />
		<% 			} // if mastery assignment
		 		} // if there's a response 
		%>
				(<b><%= qNum %></b>)&nbsp;&nbsp;
		<%		final String pts = (allQsWorthSame ? ptsPerQ[0]
						: qNum <= ptsPerQ.length ? ptsPerQ[qNum - 1] : "1");
				if (!allQsWorthSame) { 
		%>
					<b>[<%= user.translate("1".equals(pts) ? "***1*** point"
							: "***2*** points", pts) %>]</b>&nbsp;&nbsp;
		<%		} // if different Qs worth different points %>
				<% if (okToDisplay || isInstructorOrTA) { %>
					<div class="regtext" style="display:inline;">
					<% 
		 			final String qStatement = hwsession.getCurrentStatement();
					if (isFillBlank) { 
						final Choice fillBlankResp = new Choice(lastResp); %>
						<%= oneQ.getDisplayStatement(qStatement, fillBlankResp, 
								!Choice.ADD_MENUS) %>
					<% } else if (isNumeric && usesSubstns) { 
						final String qStmt = 
								oneQ.getDisplayStatement(qStatement, substnStrs); %>
						<%= chemFormatting ? Utils.toDisplay(qStmt) : qStmt %>
					<% } else { // no substitutions in statement %>
						<%= chemFormatting ? Utils.toDisplay(qStatement) : qStatement %>
					<% } // if fillBlank or substituted numeric %>
					</div>
				<% } // if should display 
				final int dependsOnQNum = hwsession.getDependsOnQNum();
				if (!okToDisplay || (isInstructorOrTA && dependsOnQNum > 0)) { %>
					<span class="boldtext" style="color:red;">
					<%= user.translate(Utils.toString("ACE will display this question ",
							isInstructorOrTA ? "and allow the student to "
									+ "solve it only after he or she has" 
								: "after you have",
							" answered question ***1*** correctly."), dependsOnQNum) %>
					</span>
				<% } // if should not display %>
				<br />
		<% 		if (!printable && (okToDisplay || isInstructorOrTA)) { %>
					<table>
					<tr>
					<td>
						<table>
						<tr>
		<% 			final String uriPts = 
							(!allQsWorthSame || ptsAllQs != 1 ? pts : "0");
					final boolean reachedMaxTries = haveResponse
							&& !allowUnlimitedTries && evalResult.tries >= maxTries;
					final boolean noMoreTries = respStatus == CORRECT
							|| (!isMasteryAssgt && reachedMaxTries);
					final String solveButton = makeButton(user.translate("Solve"),
							"openQuestion(", qNum, ", ", HWSession.SOLVE, 
							", ", uriPts, ");");
					final String viewButton = makeButton(user.translate("View"),
							"openQuestion(", qNum, ", ", HWSession.VIEW, 
							", ", uriPts, ");");
					final String practiceButton = makeButton(user.translate("Practice"),
							"openQuestion(", qNum, ", ", HWSession.PRACTICE, 
							", ", uriPts, ");");
					final String similarButton = makeButton(user.translate("Practice similar"),
							"openQuestion(", qNum, ", ", HWSession.SIMILAR, 
							", ", uriPts, ");");
					final String relatedButton = makeButton(user.translate("Solve related"),
							"openQuestion(", qNum, ", ", HWSession.SIMILAR, 
							", ", uriPts, ", true);");
					if (isMasteryAssgt) { 
						if (evalResult != null) { %>
							<!-- <td style="padding-top:10px;"><%= 
									evalResult.tries %> <%= 
									evalResult.tries == 1 ? "try" : "tries" %></td> -->
		<% 				}
						if (dueDatePast || respStatus == CORRECT) { %>
							<td style="padding-top:10px;"><%= viewButton %></td>
							<td style="padding-top:10px;"><%= practiceButton %></td>
		<% 				} else { %>
							<td style="padding-top:10px;"><%= solveButton %></td>
		<% 				} // if should display solve button
						if (!dueDatePast && evalResult != null) {
							/* Utils.alwaysPrint("list.jsp: for Q", qNum, 
							 		", respStatus = ", respStatus == SAVED 
											? "saved but not submitted for evaluation" 
										: respStatus == HUMAN_NEEDED 
											? "requires grading by instructor" 
										: respStatus == CORRECT ? "correct" 
										: respStatus == PART_RIGHT ? "partially correct" 
										: "incorrect", 
									", evalResult.tries = ", evalResult.tries, 
									", maxTries = ", maxTries); /**/
							if ((respStatus == CORRECT && evalResult.tries > maxTries)
									|| evalResult.tries >= maxTries) { %>
								<td style="padding-top:10px;"><%= relatedButton %></td>
		<% 					} // if should allow solve related
		 				} else if (dueDatePast || respStatus == CORRECT) { %>
							<td style="padding-top:10px;"><%= similarButton %></td>
		<%				} // if should display new version of problem for mastery
					} else if (dueDatePast || (haveResponse && noMoreTries)) { %>
						<td style="padding-top:10px;"><%= viewButton %></td>
						<td style="padding-top:10px;"><%= practiceButton %></td>
		<% 				if (usesSubstns) { %>
							<td style="padding-top:10px;"><%= similarButton %></td>
		<% 				} // if has R groups
		 			} else { %>
						<td style="padding-top:10px;"><%= solveButton %></td>
		<% 			} // if view or solve %>
						</tr>
						</table>
					</td>
					</tr>
		<%			if (course.forumEnabled() && notBlocked) {
						final int topicId = hwsession.getCurrentLinkedTopic(); %>
						<tr>
						<td>
						<table>
						<tr>
						<td style="padding-top:10px;">
		<% 					if (topicId > 0) { %>
								<%= makeButton(user.translate("Go to forum topic"),
										"openForumTopic(", topicId, ", ", qId, ");") %>
		<%					} else { %>
								<%= makeButton(user.translate("Start forum topic"),
										"openForumTopic(0, ", qId, ");") %>
		<%					} // if a topic is linked %>
						</td>
						</tr>
						</table>
						</td>
						</tr>
		<%			} // if forum is enabled %>
					</table>
		<% 		} // not the printable list %>
		<%		final String book = oneQ.getBook();
				final String chapter = oneQ.getChapter();
				final String remarks = oneQ.getRemarks();
				final boolean displayRef = book != null 
						&& "Literature".equals(book)
						&& (isInstructorOrTA || assgt.showRefsBeforeAnswered()
							|| (assgt.showRefsAfterAnswered() 
								&& respStatus == CORRECT));
				final StringBuilder displayBld = new StringBuilder();
				if (isInstructorOrTA) {
					Utils.appendTo(displayBld, 
							"<p><span style=\"color:red;\">", 
							user.translate("Question"), ' ', qId, ", ");
					if (displayRef) displayBld.append("</span>");
				} // if is instructor or TA
				if (displayRef) { 
					if (!isInstructorOrTA) displayBld.append("<p>");
					if (!Utils.isEmpty(remarks)) {
						displayBld.append("<a href=\"");
						if (!remarks.startsWith("http://")) {
							displayBld.append("http://");
						}
						Utils.appendTo(displayBld, 
								remarks, "\" target=\"window2\">");
					}
					displayBld.append(chapter);
					if (!Utils.isEmpty(remarks)) displayBld.append("</a>");
					if (!isInstructorOrTA) displayBld.append("</p>");
				} else if (isInstructorOrTA) { 
					if ("Other".equals(book)) {
						Utils.appendTo(displayBld, 
								user.translate("by"), ' ', chapter);
					} else {
						displayBld.append(book);
						if (!"[None]".equals(remarks)) {
							Utils.appendTo(displayBld, ' ', remarks);
						}
					} // if book is other  
					displayBld.append("</span>");
				} // if is instructor or TA 
				if (isInstructorOrTA) displayBld.append("</p>"); %>
				<%= displayBld.toString() %>
			</td> 

		<% 	if (okToDisplay || isInstructorOrTA) {
				// display figures
				final int numFigs = oneQ.getNumFigures();
				final int numFigsToShow = 
						(printable || numFigs <= 1 ? numFigs : 1);
				boolean haveImage = false;
				for (int figNum = 1; figNum <= numFigs; figNum++) {
					if (figures[figNum - 1].hasImage()) {
						haveImage = true;
						break;
					} // if have an image
				} // for each figure 
				if (haveImage && printable) { // start new row %>
					<td>&nbsp;</td></tr>
					<tr>
		<% 		} // if the figure is an image and this is a printable list %>
			<td style="vertical-align:top; padding-right:10px;
					padding-top:10px; padding-bottom:10px;">
		<% 		if (numFigs > 0) { %>
						<table style="width:100%;">
						<tr><td class="regtext">
							<table class="regtext" summary="outer">
		<% 			for (int figNum = 1; figNum <= numFigsToShow; figNum++) { 
						final Figure figure = figures[figNum - 1]; 
						final boolean subRGroups = usesSubstns 
								&& !isNumeric && figNum == 1
								&& !figure.hasImage();
						final boolean figIsSynth = figure.isSynthesis();
						final Molecule[] rGroups = (subRGroups
								? hwsession.getCurrentRGroupMols() : null);
						final String[] figData = (figIsSynth && subRGroups
								? figure.getDisplayData(rGroups, 
									Synthesis.getRxnsDisplayPhrases(user))
								: figIsSynth ? figure.getDisplayData(
									Synthesis.getRxnsDisplayPhrases(user))
								: subRGroups ? figure.getDisplayData(rGroups)
								: figure.getDisplayData());
						 /* Utils.alwaysPrint("list.jsp: Q", qNum, ", figure ", 
								figNum, ", subRGroups = ", subRGroups, 
								", figData[Figure.STRUCT] =\n", 
								figData[Figure.STRUCT] ); /**/
						// begin display
						final boolean showNumFigs = numFigs > 1 
								&& (printable || !figure.hasImage()); 
						if (showNumFigs) {
							String figNumStr = "";
							if (showNumFigs) { 
								final StringBuilder figNumBld = 
										new StringBuilder()
										.append(user.translate(
											"Fig. ***1*** of ***2***", 
											new int[] {figNum, numFigs})); 
								if (printable) {
									figNumBld.insert(0, 
											"</td></tr><tr><td class=\"boldtext\" "
											+ "style=\"padding-left:10px;\">");
									if (figure.hasImage())
										figNumBld.insert(0, "</td><td>&nbsp;");
									figNumBld.append("</td><td>&nbsp;</td>"); 
								} // if printable
								figNumStr = figNumBld.toString();
							} // if showNumFigs
		%>
							<tr><td class="boldtext" style="width:100%;">
								<%= figNumStr %>
							</td></tr>
		<%				} // if there's text above the figure
			 			if (figure.isJmol()) { 
							jmolNum++;
		%>
							<tr><td class="whiteTable">
							<script type="text/javascript">
								// <!-- >
								setJmol(<%= jmolNum %>, 
										'<%= Utils.toValidJS(
											figData[Figure.STRUCT]) %>',
										'white', 250, 250,
										'<%= Utils.toValidJS(
											figData[Figure.JMOL_SCRIPTS]) %>');
								// -->
							</script>
							</td></tr>
		<%				} else if (figure.isLewis()) { %>
							<tr><td class="whiteTable" id="fig<%= qNum %>_<%= figNum %>">
							<input type="hidden" id="figData<%= qNum %>_<%= figNum %>"
									value="<%= Utils.toValidHTMLAttributeValue(
										figData[Figure.STRUCT]) %>" />
							<input type="hidden" id="figQFlags<%= qNum %>_<%= figNum %>"
									value="<%= oneQ.getQFlags() %>" />
							</td></tr>
		<%				} else if (!figure.hasImage()) { 
							final boolean isLewisFig = figure.isLewis();
							final String figIdStr = Utils.toString(
									"fig", qNum, "_", figNum);
							if (figure.isReaction()) {
		%>
								<tr><td>
								<table class="whiteTable"
										style="border-collapse:collapse; 
											margin-left:0px; margin-right:0px;">
									<tr><td id="<%= figIdStr %>" 
											style="background-color:#FFFFFF;">
		<%					} else {
		%>
								<tr><td id="<%= figIdStr %>" class="whiteTable">
		<%					} // if figure.type
		%>
							<%= figure.getImage(pathToRoot, user.prefersPNG(), qFlags, 
									subRGroups ? substnStrs : null, figIdStr) %>
		<%					if (figure.isReaction()) {
								final String above = Utils.toDisplay(figData[Figure.RXN_ABOVE]);
								final String below = Utils.toDisplay(figData[Figure.RXN_BELOW]);
								final int arrowSize = 36;
		%>
								</td><td style="width:85px; vertical-align:middle;
										background-color:#FFFFFF; 
										text-align:center;">
									<%@ include file="/includes/reactionArrow.jsp.h" %>
								</td>
								</tr>
							 	</table> <!-- table A ends -->
		<%					} else if (figure.isSynthesis()) {
		%>
								<%= figData[Synthesis.RXNID] %>
		<%					} // if synthesis figure
		%>
							</td></tr> <!-- tr B ends -->
		<%				} else if (printable) { // an image & the printable list 
		%>
							<tr><td class="boldtext" colspan="2">
							<%= figure.isImageAndVectors()
									? user.translate("This image has vectors "
										+ "overlaid on it. Click on the image "
										+ "in the original question list to "
										+ "see them.") 
									: "&nbsp;" %>
							</td></tr>
							<tr><td style="vertical-align:top; padding-right:10px;
									padding-bottom:20px; text-align:center; 
									border-collapse:collapse;" colspan="2">
								<img id="image<%= qNum %>.<%= figNum %>"
										class="whiteTable"
										src="<%= pathToRoot %><%= 
											figure.bufferedImage %>"
										onload="fixTheImageSize(this, 800);"
										alt="picture" />
							</td></tr>
							<tr><td style="vertical-align:top; 
									border-collapse:collapse; padding-right:10px;">
							</td></tr>
		<%				} else { // an image & not the printable list 
							final String srcFile = Utils.toString(pathToRoot, 
									figure.bufferedImage); 
							final String color = 
									(!figure.isImageAndVectors() ? "" 
									: Utils.isEmpty(qData) ? "red" 
									: qData[0].data);
		%>
							<tr><td>
							<table summary="dummy">
							<tr><td class="boldtext">
								<%= numFigs <= 1 ? "&nbsp;" : user.translate(
										"Fig. ***1*** of ***2***", 
										new int[] {figNum, numFigs}) %>
							</td><td class="boldtext" style="text-align:right;">
								<% if (figure.isImage()) { %>
									<span id="enlargeCell<%= figNum %>"><%= 
										user.translate("Click image to enlarge") 
										%></span>
								<% } else { %>
									<%= user.translate("ACE will display vectors "
											+ "on this image. Click the image to "
											+ "see them.") %>
								<% } // if image %>
							</td></tr>
							<tr><td style="text-align:right;" colspan="2">
								<a href="javascript:enlargeImage('<%= 
										srcFile %>', '<%= figure.isImage() ?  "" 
											: Utils.toValidHTMLAttributeValue(
												figData[Figure.COORDS]) 
										%>', '<%= color %>')">
								<img id="image<%= qNum %>.<%= figNum %>" 
										class="whiteTable" src="<%= srcFile %>"
										onload="prepareImage(this, 'enlargeCell<%= 
											figNum %>');" 
										alt="picture" />
								</a>
							</td></tr>
							</table>
							</td></tr>
		<%				} // if figure type 
			 		} // for each figure figNum  
		%>
						</table>
					</td></tr></table>
		<%		} // if there's at least one figure  

				// display QData
				if ((isRank || isChoice || isChooseExplain) 
						&& oneQ.getNumQData(Question.GENERAL) > 0) {
					final String delimiter = (isRank
							? Rank.MAJOR_SEP : Choice.SEPARATOR);
					if (Utils.isEmpty(lastResp)) {
						lastResp = Utils.joinInts(1, qData.length, delimiter);
					} // if there is no last or initiated response
					int[] optIndices = null;
					if (isRank) {
						final Rank rankResp = new Rank(lastResp);
						optIndices = rankResp.getAllItems(); // 1-based
					} else {
						final Choice choiceResp = (isChoice
								? new Choice(lastResp)
								: (new ChooseExplain(lastResp)).choice);
						optIndices = choiceResp.getAllOptions(); // 1-based
					} // choice or rank
		%>
					<table style="padding-left:10px;">
		<%				for (int qdNum = 0; qdNum < qData.length; qdNum++) { 
							final int optIndex = (qdNum < optIndices.length
										&& optIndices[qdNum] > 0
									? optIndices[qdNum] - 1 : qdNum);
							if (optIndex < qData.length) {
								final QDatum qDatum = qData[optIndex];
								final String qDatumDisp = qDatum.toShortDisplay(
										chemFormatting, user);
								final String optItemStr = Utils.toString("<b>", 
										user.translate(isChoice ? "Option" : "Item"), 
										' ', qdNum + 1, ".</b>");
		%>
								<tr><td class="regtext" style="vertical-align:middle;">
									<%= Utils.group(optItemStr) %>
		<%							if (qDatum.isText()) { 
		%>
										<%= qDatumDisp %>
		<%							} else if (printable) { 
										final String qdIdStr = Utils.toString(
												"qd", qNum, '_', qdNum);
		%>
										</td></tr>
										<tr><td id="<%= qdIdStr %>" 
												class="whiteTable"
												style="padding-left:20px;">
											<%= qDatum.getImage(pathToRoot,
												user.prefersPNG(), qdIdStr) %>
											<br/>
		<%							} else { // molecule, not the printable list %>
										<%= qDatumDisp %>
		<%		 					} // if the printable list
		%>
								</td></tr>
		<%					} // optIndex < qData.length 
			 			} // for each Qdatum optIndex 
		%>
					</table>
		<%		} // if Qtype is choice or rank %>
			</td>
		<%		if (haveImage && printable) { %>
					<td>&nbsp;</td>
		<%		} %>
		<%	} else { %>
			<td></td>
		<%	} // if okToDisplay %>
			</tr>
		<% } // for each Q qNum in assignment %>
	</table>
	<% if (!printable) { %>
		</div>
	<% } %>
</body>
</html>
