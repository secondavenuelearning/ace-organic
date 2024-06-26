<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.evals.EvalResult,
	com.epoch.session.GradeSet,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.text.DateFormat,
	java.text.NumberFormat,
	java.util.ArrayList,
	java.util.Arrays,
	java.util.Calendar,
	java.util.Date,
	java.util.List,
	java.util.TimeZone"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	final String pathToRoot = "../";
	final int[] sizes = {20, 180, 100, 80, 90};
	final int NUM = 0;
	final int NAME = 1;
	final int LABEL = 2;
	final int TOTAL = 3;
	final int SCORE = 4;
	final String CLOSESPAN = "</span>";

	final int PTS_PER_Q = Assgt.PTS_PER_Q;
	final int ATTEMPT = Assgt.ATTEMPT;
	final int TIME = Assgt.TIME;
	final boolean PAST_TENSE = Assgt.PAST_TENSE;

	final char STUDENT = 'S';
	final char INSTRUCTOR_ONE = '1';
	final char INSTRUCTOR_ALL = '*';

	final int NONE = 0;
	final int SOME = 1;
	final int ALL = 2;

	final String SELECTED = " selected=\"selected\"";

	if (isTA) role = (course.tasMayGrade() ? User.INSTRUCTOR : User.TA);
	final int oneStudentNum = 
			MathUtils.parseInt(request.getParameter("oneStudentNum"), -1);
	final char view = (role == User.STUDENT ? STUDENT
			: oneStudentNum == -1 ? INSTRUCTOR_ALL : INSTRUCTOR_ONE);
	final boolean isTutorial = "true".equals(request.getParameter("isTutorial"));

	GradeSet hwGrades;
	String[] hwNames;
	String[] userIds; 
	String[] userNames; 
	String[] studentNums; 
	Date[][] hwDueDates; 
	Assgt[] tutAssgts = null; // used only if isTutorial
	synchronized (session) {
		userIds = (String[]) session.getAttribute("userIds"); 
		userNames = (String[]) session.getAttribute("userNames"); 
		studentNums = (String[]) session.getAttribute("studentNums"); 
		if (isTutorial) {
			hwNames = (String[]) session.getAttribute("tutNames"); 
			hwGrades = (GradeSet) session.getAttribute("tutGrades");
			tutAssgts = hwGrades.getAssgts();
			hwDueDates = new Date[studentNums.length][tutAssgts.length];
		} else {
			hwNames = (String[]) session.getAttribute("hwNames"); 
			hwGrades = (GradeSet) session.getAttribute("hwGrades");
			hwDueDates = (Date[][]) session.getAttribute("hwDueDates"); 
		}
	} // synchronized

	/* Utils.alwaysPrint("detailedGrade.jsp: role = ", role, ", view = ", view,
			", userNames.length = ", userNames.length, 
			", userNames[0] = ", userNames[0], ", isTutorial = ", isTutorial); /**/

	final boolean forExport = request.getParameter("forExport") != null;
	final int hwNum = MathUtils.parseInt(request.getParameter("hwNum")); // 1-based!
	// Utils.alwaysPrint("detailedGrade.jsp: got hwGrades for hwNum = ", hwNum);
	final String hwsetName = hwNames[hwNum - 1];
	final int numQs = hwGrades.getNumQuestionsAssigned(hwNum);
	final int thisHWId = hwGrades.getHWId(hwNum);
	List<ArrayList<Integer>> groupedQIds = null;
	int[] qPicks = null;
	int numGroups = 0;
	boolean getAssignedQs = view != INSTRUCTOR_ALL;
	if (getAssignedQs) {
		groupedQIds = hwGrades.getAssignedQIds(hwNum, view == INSTRUCTOR_ONE
				? userIds[oneStudentNum - 1] : userIds[0]);
		numGroups = groupedQIds.size();
		getAssignedQs = numGroups != 0;
		if (getAssignedQs) {
			qPicks = new int[numGroups];
			Arrays.fill(qPicks, 1);
		} // if Qs have been assigned
	} // if should get only assigned Qs
	if (!getAssignedQs) {
		groupedQIds = hwGrades.getGroupedQIds(hwNum);
		qPicks = hwGrades.getQuestionPicks(hwNum);
		numGroups = qPicks.length;
	} // if should get all possible Qs
	final List<Integer> qIdsByPos = new ArrayList<Integer>();
	for (final ArrayList<Integer> qIdsGroup : groupedQIds) {
		for (final Integer qId : qIdsGroup) {
			qIdsByPos.add(qId);
		} // for each Q in group
	} // for each group of Qs
	final int totalQs = qIdsByPos.size();

	if (numQs == -1) {
		%><jsp:forward page="nohwset.html"/><%
	}

	final Assgt assgt = (isTutorial ? tutAssgts[hwNum - 1] : assgts[hwNum - 1]);
	final boolean isMasteryAssgt = assgt.isMasteryAssgt();
	final String detailFlag = request.getParameter("details");
	final int details = MathUtils.parseInt(detailFlag, SOME);

	// additional output
	final int numStudents = userIds.length;
	final int[][] numTries = new int[totalQs][numStudents];
	final int[] numStudentsTried = new int[totalQs];
	final double[] sumScorePerQ = new double[totalQs];
	final int[] numCorrectEntries = new int[totalQs];
	final int[] numMasteredEntries = new int[totalQs];

	final String courseName = course.getName();
	final TimeZone timeZone = course.getTimeZone();
	final String studentNumLabel = user.getInstitutionStudentNumLabel();

	// in case grades are exported to Excel
	final Date now = new Date();
	final DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
	df.setTimeZone(timeZone);
	final String filename = Utils.toValidFileName(Utils.toString(
			courseName != null ? courseName : "grades", "_", hwsetName, "_", 
			isMasteryAssgt ? "mastery_" : "", df.format(now)));

	final String UNATTEMPTED_STR = (forExport ? "" : "-");
	final String HUMAN_NEEDED_SYMB = "???";
	final String RIGHT_COLOR = "blue";
	final String PARTIAL_COLOR = "purple";
	final String WRONG_COLOR = "red";
	final String HUMAN_NEEDED_COLOR = "gray";
	final String HUMAN_NEEDED_STR = (forExport ? "??"
			: "<span style=\"font-weight:bold; color:"
				+ HUMAN_NEEDED_COLOR + ";\">" + HUMAN_NEEDED_SYMB 
				+ CLOSESPAN);
	final String COMMENTED = "&radic;";
	final String EMPTY_TD = "<td></td>";

	final NumberFormat numberFormat = NumberFormat.getInstance();
	numberFormat.setMaximumFractionDigits(course.getNumDecimals());

	final String hideNamesStr = request.getParameter("hideNames");
	final boolean hideNames = hideNamesStr != null && "true".equals(hideNamesStr);

	final char NO_STATUS = EvalResult.NO_STATUS;
	final char INITIALIZED = EvalResult.INITIALIZED;
	final char SAVED = EvalResult.SAVED;
	final char HUMAN_NEEDED = EvalResult.HUMAN_NEEDED;
%>

<% if (!forExport) { %>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE grades for assignment <%= hwNum %></title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
		<style type="text/css">
			#footer {
				position:absolute;
				bottom:0;
				left:0;
				width:100%;
				height:55px;
				overflow:auto;
				text-align:right;
			}

			* html body {
				padding:100px 0 55px 0;
			}

			* html #footer {
				height:100%;
			}
		</style>
	<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">

		// <!-- >
		<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
		<%@ include file="/navigation/courseSidebarJS.jsp.h" %>

		var namesAreHidden = <%= hideNames %>;

		function changeDetails() {
			var go = new String.builder().
					append('detailedGrade.jsp?hwNum=<%= 
						hwNum %>&hwId=<%= thisHWId %>&isTutorial=<%= 
						isTutorial %>&oneStudentNum=<%= 
						oneStudentNum %>&details=').
					append(getValue('changeDetails'));
			if (namesAreHidden) go.append('&hideNames=true');
			if (<%= oneStudentNum > 4 %>) {
				go.append('#student<%= oneStudentNum %>');
			}
			self.location.href = go.toString();
		} // openDetailed()

		function openOneStudent(userNum) {
			var go = new String.builder().
					append('detailedGrade.jsp?hwNum=<%= hwNum %>&hwId=<%= 
						thisHWId %>&isTutorial=<%= isTutorial 
						%>&details=<%= details %>');
			if (userNum >= 0) {
				go.append('&oneStudentNum=').append(userNum); // 1-based!!
			}
			if (namesAreHidden) go.append('&hideNames=true');
			self.location.href = go.toString();
		} // openOneStudent()

		function showMol(userId, qId) {
			var go = new String.builder().
					append('showMol.jsp?hwNum=<%= hwNum %>'
						+ '&hwId=<%= thisHWId %>&isTutorial=<%= isTutorial 
							%>&qId=').append(qId).append('&userId=').
					append(encodeURIComponent(userId)).
					append('&isInstructorView=<%= view != STUDENT %>');
			openMolShowWindow(go.toString());
		} // showMol()

		function regradeQuestion(studentNum) {
			var numsStr = document.getElementById('regradeSelector').value;
			var numsArr = numsStr.split(',');
			var go = new String.builder().
					append('regradeConfirm.jsp?hwNum=<%= hwNum %>' + 
						'&isTutorial=<%= isTutorial %>' + 
						'&hwId=<%= thisHWId %>&qNum=').
					append(numsArr[0]).append('&qId=').append(numsArr[1]).
					append('&studentNum=').append(studentNum);
			openRegradeWindow(go.toString());
		} // regradeQuestion()

		<% if (view == INSTRUCTOR_ONE) { %>
		function resetRecord(qId, qNum) {
			var go = new String.builder().
					append('resetConfirm.jsp?hwNum=<%= hwNum %>'
						+ '&isTutorial=<%= isTutorial %>&hwId=<%= thisHWId 
							%>&qId=').append(qId).append('&qNum=').
					append(qNum).append('&userId=').
					append(encodeURIComponent('<%= Utils.toValidJS(
						userIds[oneStudentNum - 1]) %>'));
			openResetWindow(go.toString());
		} // resetRecord()
		<% } // if view %>

		function goGeneral() {
			var go = new String.builder().append('grade.jsp?fromDetailed=true');
			<% if (view == INSTRUCTOR_ONE) { %>
				go.append('&oneStudentNum=<%= oneStudentNum %>'); // 1-based!!
			<% } %>
			if (namesAreHidden) go.append('&hideNames=true');
			self.location.href = go.toString();
		} // goGeneral()

		function exportGrades() {
			this.location.href = 'detailedGradeTxt.jsp?filename='
					+ encodeURIComponent('<%= Utils.toValidFileName(filename) %>')
					+ '&hwNum=<%= hwNum %>&hwId=<%= thisHWId %>'
					+ '&details=<%= details %>&forExport=true';
		} // exportGrades()

		var studentNames = new Array();
		function toggleNames() {
			namesAreHidden = !namesAreHidden;
			hideNames();
		} // toggleNames()

		function hideNames() {
			var cellNum = 1;
			while (true) {
				var cell = document.getElementById('indivName' + cellNum);
				if (cell) {
					if (namesAreHidden) {
						studentNames[cellNum] = cell.innerHTML;
						cell.innerHTML = '';
					} else {
						cell.innerHTML = studentNames[cellNum];
					}
					cellNum++;
				} else break;
			} // while there are more cells
			if (namesAreHidden) {
				setInnerHTML('namesToggle', '<%= Utils.toValidJS(makeButton( 
						user.translate("Show names"), "toggleNames();")) %>');
			} else {
				setInnerHTML('namesToggle', '<%= Utils.toValidJS(makeButton( 
						user.translate("Hide names"), "toggleNames();")) %>');
			}
		} // hideNames()

		function viewQ(qId, qNum) {
			var goBld = new String.builder().append('startView.jsp?qId=').
					append(qId).append('&hwNum=<%= hwNum %>&hwId=<%= thisHWId 
						%>&isInstructorView=<%= view != STUDENT %>');
			if (qNum !== 0) goBld.append('&qNum=').append(qNum);
			<% if (view == INSTRUCTOR_ONE) { %>
				goBld.append('&userId=').append(encodeURIComponent('<%= 
						Utils.toValidJS(userIds[oneStudentNum - 1]) %>'));
			<% } // if INSTRUCTOR_ONE %>
			var go = goBld.toString();
			openPreviewWindow(go);
		} // viewQ()

	// -->
	</script>
</head>

<body style="text-align:center; margin:5px;"
		onload="setTab('<%= toTabName(user.translateJS("Grade Book")) %>');<%=
		hideNames ? " hideNames();" : "" %>">
<% } // if not forExport

	if (details == ALL) sizes[SCORE] += 50;
	int tableWidth = sizes[NUM] + sizes[TOTAL] + sizes[SCORE] * totalQs;
	if (view != INSTRUCTOR_ONE || !hideNames) tableWidth += sizes[NAME];
	if (!Utils.isEmpty(studentNumLabel)) tableWidth += sizes[LABEL];
	int leftOver = 5;
	int introTableExtra = 0;
	if (tableWidth < 626) {
		leftOver = 626 - tableWidth;
		tableWidth = 626;
	} else introTableExtra = tableWidth - 626;
	if (!forExport) { %>
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

	<div id="contentsWithTabsWithFooter">
	<table style="width:<%= tableWidth %>px; margin-left:10px; margin-right:auto;
			border-style:none; border-collapse:collapse; text-align:left;">
		<tr>
			<td class="boldtext big" >
				<%= Utils.toDisplay(hwsetName) %>
				<%= isMasteryAssgt ? Utils.toString(
						'(', user.translate("Mastery assignment"), ')') : "" %>
			</td>
		</tr>
		<tr>
			<td style="width:626px;">
				<div class="regtext">
				<%
				StringBuilder keyBld = Utils.getBuilder( 
						"A hyphen indicates that ", 
						view == STUDENT ? "you have" : "a student has", 
						" not attempted an assignment; the symbol ", 
						COMMENTED, " indicates that ", 
						view == STUDENT ? "your instructor has" : "you have", 
						" commented on ", view == STUDENT ? "your" : "a student's", 
						" response", view == STUDENT ?
								" (view the comment on the question-answering page)"
							: view == INSTRUCTOR_ONE ?
								" (click \"alter\" to alter it)"
							: " (click on the student's name to alter it);", 
						"; the symbol ", HUMAN_NEEDED_SYMB, " indicates that ", 
						view == STUDENT ? "you have" : "a student has", 
						" responded to a question, but the response requires ", 
						view == STUDENT ? "evaluation by the instructor" 
							: "your evaluation", 
						'.');
				String key = user.translate(
						keyBld.toString()).replaceAll("\\?\\?\\?", HUMAN_NEEDED_STR);
				if (details > NONE) {
					final String[] correctness = new String[]
							{"correct", "partially correct", "incorrect"};
					user.translate(correctness);
					final String SPAN = "<span style=\"font-weight:bold; color:";
					final String colorRt = Utils.toString(SPAN + RIGHT_COLOR + ";\">", 
							correctness[0], CLOSESPAN);
					final String colorPart = Utils.toString(SPAN + PARTIAL_COLOR + ";\">", 
							correctness[1], CLOSESPAN);
					final String colorWr = Utils.toString(SPAN + WRONG_COLOR + ";\">", 
							correctness[2], CLOSESPAN);
					keyBld = Utils.getBuilder(user.translate(
							"Points are color-coded as ***correct***, "
								+ "***partially correct***, and ***incorrect***.",
								new String[] {colorRt, colorPart, colorWr}), 
							' ', key, ' ');
					if (view != INSTRUCTOR_ONE || role == User.TA) {
						keyBld.append(user.translate(Utils.toString(
								"Click the \"attempt(s)\" link to view ", 
								view == STUDENT ? "your"
									: view == INSTRUCTOR_ALL ? "a student's"
									: "the student's", 
								" most recent response to a question.")));
					} // multistudent view
					key = keyBld.toString();
				} // if details %>
					<%= key %>
				<% if (assgt.hasGradingParams(PTS_PER_Q)) { %>
					<blockquote><%= assgt.gradingParamsToDisplay(
						PTS_PER_Q, PAST_TENSE, user) %></blockquote>
				<% }
				if (assgt.hasGradingParams(ATTEMPT)) { %>
					<p><%= user.translate("ACE has applied "
							+ "attempt-dependent grading as follows:") %>
					</p><blockquote><%= assgt.gradingParamsToDisplay(
							ATTEMPT, PAST_TENSE, user) %></blockquote>
				<% }
				if (assgt.hasGradingParams(TIME)) { 
					final StringBuilder timeBld = Utils.getBuilder(
							assgt.gradingParamsToDisplay(TIME, PAST_TENSE, user));
					if (view != INSTRUCTOR_ALL) {
						final Date hwDueDate = (view == STUDENT ?
								hwDueDates[0][hwNum - 1]
								: hwDueDates[oneStudentNum - 1][hwNum - 1]);
						if (hwDueDate == Assgt.NO_DATE) {
							Utils.appendTo(timeBld, "<p>", 
									user.translate(Utils.toString(
											view == STUDENT ? "Your" 
												: "The student's",
											" due date on this assignment "
											+ "is indefinite."), 
									"</p>"));
						} else {
							final DateFormat dft = 
									DateFormat.getDateTimeInstance(
										DateFormat.SHORT, DateFormat.FULL);
							dft.setTimeZone(timeZone);
							Utils.appendTo(timeBld, "<p>", 
									user.translate(Utils.toString(
										view == STUDENT 
											? "Your" : "The student's",
										" due date on this assignment "
										+ "is ***Dec. 31, 1876***."),
										dft.format(hwDueDate)), 
									"</p>");
						} // if there's a due date
					} // if not view all students

				%>
					<p><%= user.translate("ACE has applied "
							+ "time-dependent grading as follows:") %>
					</p><blockquote><%= timeBld.toString() %></blockquote>
				<% } // if has time-dependent grading parameters
				if (view == INSTRUCTOR_ALL && role != User.TA) { %>
					<%= user.translate("Click \"Regrade\" at the bottom "
							+ "of a column to regrade that question. "
							+ "Click on a student's name to see just that "
							+ "student's grades and to alter that student's "
							+ "grade or number of attempts.") %>
				<% } else if (view == INSTRUCTOR_ONE && role != User.TA) {
					keyBld = Utils.getBuilder(user.translate("Click \"alter\" "
								+ "to view the student's most recent response "
								+ "to a question or to alter "
								+ "the student's number of attempts or grade"));
					if (assgt.hasGradingParams()) {
						Utils.appendTo(keyBld, 
								" <span style=\"font-weight:bold; color:red\">", 
								user.translate("<i>before</i> attempt- or "
									+ "time-dependent grading has been applied"), 
								CLOSESPAN);
					} // if has grading parameters
					keyBld.append('.');
				%>
					<%= keyBld.toString() %>
				<% } // if instructor view %>
				</div>
			</td>
			<td style="width:<%= introTableExtra %>px; text-align:left; padding-top:10px;">
				&nbsp;
			</td>
		</tr>
	</table>
	<% } // if not forExport
	final StringBuilder lastLine1Bld = new StringBuilder();
	final StringBuilder lastLine2Bld = new StringBuilder();
	if (forExport) { %>
		<table>
	<% } else { %>
		<p>&nbsp;</p><table class="regtext" style="width:<%= tableWidth %>px; 
				margin-left:10px; margin-right:auto; 
				border-style:none; border-collapse:collapse;">
		<!-- Display question numbers -->
	<% } // if for export %>
	<tr>
	<% if (view != STUDENT) {
		if (forExport) { %>
			<td>Name</td>
			<% if (!Utils.isEmpty(studentNumLabel)) { %>
				<td><%= studentNumLabel %></td>
			<% } %>
		<% } else { 
			if (view == INSTRUCTOR_ALL) lastLine1Bld.append(EMPTY_TD); %>
			<td class="boldtext enlarged"
					style="width:<%= sizes[NUM] %>px; text-align:center;
					border-bottom-style:solid; border-width:1px;
					border-color:#49521B;">
				<%= user.translate("No.") %>
			</td>
			<% if (view == INSTRUCTOR_ALL || !hideNames) {
				if (view == INSTRUCTOR_ALL) lastLine1Bld.append(EMPTY_TD); %>
				<td class="boldtext enlarged"
						style="width:<%= sizes[NAME] %>px;
						border-bottom-style:solid; border-width:1px;
						border-color:#49521B; padding-left:10px;">
					<%= user.translate("Name") %>
				</td>
			<% } // if have a name column %>
			<% if (!Utils.isEmpty(studentNumLabel)) {
				if (view == INSTRUCTOR_ALL) lastLine1Bld.append(EMPTY_TD); %>
				<td class="boldtext enlarged" style="width:<%= sizes[LABEL] %>px;
						border-bottom-style:solid; border-width:1px;
						border-color:#49521B; padding-left:10px;">
					<%= studentNumLabel %>
				</td>
			<% } // if studentNumLabel
		} // if for export
	} else { // student view
		if (forExport) { %>
			<td><%= user.translate("Name") %></td>
		<% } else { %>
			<td class="boldtext enlarged" style="width:<%= sizes[NAME] %>px;
					border-bottom-style:solid; border-width:1px;
					border-color:#49521B;">
				<%= user.translate("Name") %>
			</td>
		<% } // if for export
	} // if view is INSTRUCTOR_ALL
	if (forExport) { %>
		<td><%= user.translate("Total") %></td>
	<% } else {
		if (view == INSTRUCTOR_ALL) lastLine1Bld.append(EMPTY_TD); %>
		<td class="boldtext enlarged" style="width:<%= sizes[TOTAL] %>px;
				text-align:center; border-bottom-style:solid;
				border-width:1px; border-color:#49521B;">
			<%= user.translate("Total") %>
		</td>
	<% } // if for export
	int qNum = 0;
	for (int grpNum = 0; grpNum < numGroups; grpNum++) {
		final List<Integer> qIdsGroup = groupedQIds.get(grpNum);
		final int numGroupQs = qIdsGroup.size();
		if (forExport) { %>
			<td colspan="<%= numGroupQs * (details > NONE && numGroupQs != 1 ? 2 : 1) %>">
				<% final StringBuilder qStrBld = new StringBuilder();
				for (int pick = 0; pick < qPicks[grpNum]; pick++) {
					qNum++;
					if (pick > 0) qStrBld.append(" &amp; ");
					Utils.appendTo(qStrBld, user.translate("Q"), qNum);
				} // for each pick 
				if (numGroupQs == 1 && view != STUDENT) {
					Utils.appendTo(qStrBld, " (#", qIdsGroup.get(0), ')');
				} // if there's just one Q %>
				<%= qStrBld.toString() %>
			</td>
			<% if (details > NONE && numGroupQs == 1) { %>
				<td><%= user.translate("Q") %><%= qNum %> <%= user.translate("tries") %></td>
			<% } // if details
		} else { // not for export %>
			<td colspan="<%= numGroupQs %>" class="boldtext enlarged"
					style="width:<%= sizes[SCORE] %>px; text-align:center;
					border-bottom-style:solid; border-width:1px; border-color:#49521B;">
				<% if (view == INSTRUCTOR_ALL) {
					Utils.appendTo(lastLine1Bld, 
							"<td class=\"boldtext enlarged\" colspan=\"", 
							numGroupQs, "\" style=\"text-align:center;\">");
				} // if view is INSTRUCTOR_ALL
				final StringBuilder qStrBld = new StringBuilder();
				for (int pick = 0; pick < qPicks[grpNum]; pick++) {
					qNum++;
					if (pick > 0) {
						qStrBld.append(" &amp; ");
						if (view == INSTRUCTOR_ALL) lastLine1Bld.append(" &amp; ");
					} // if Q is randomized
					boolean allowQView = numGroupQs == 1 
							&& (view != STUDENT || getAssignedQs);
					if (view == STUDENT && allowQView) {
						allowQView = 
								hwGrades.getOkToDisplay(userIds[0], hwNum, qNum);
					} // if this Q's display depends on another Q answered correctly
					final StringBuilder qDispBld = 
							Utils.getBuilder(user.translate("Q"), qNum);
					if (allowQView) {
						Utils.appendTo(qStrBld, 
								"<a href=\"javascript:viewQ(", qIdsGroup.get(0), 
								',', qNum, ");\">", qDispBld, "</a>");
					} else qStrBld.append(qDispBld);
					if (view == INSTRUCTOR_ALL) lastLine1Bld.append(qDispBld); 
				} // for each pick 
				if (view == INSTRUCTOR_ALL) lastLine1Bld.append("</td>"); %>
				<%= qStrBld.toString() %>
			</td>
		<% } // if for export
	} // for each grpNum
	if (!forExport) { %>
		<td style="width:<%= leftOver %>px; text-align:center; border-width:1px;
				border-color:#49521B; background-color:white;"></td>
	<% } // if not for export %>
	</tr>
	<% if (view == INSTRUCTOR_ALL && totalQs > numGroups) { 
		final String EMPTY_TD2 = "<td style=\"border-top-style:solid; "
				+ "border-width:1px; border-color:#49521B;\"></td>"; %>
		<!-- begin randomized Q headings -->
		<tr>
		<% if (forExport) { %>
			<td></td>
			<% if (!Utils.isEmpty(studentNumLabel)) { %>
				<td></td>
			<% } %>
		<% } else {
			Utils.appendTo(lastLine2Bld, EMPTY_TD2, EMPTY_TD2); %>
			<td class="boldtext enlarged" style="width:<%= sizes[NUM] %>px; 
					text-align:center; border-bottom-style:solid; 
					border-width:1px; border-color:#49521B;">
			</td>
			<td class="boldtext enlarged" style="width:<%= sizes[NAME] %>px;
					border-bottom-style:solid; border-width:1px;
					border-color:#49521B; padding-left:10px;">
			</td>
			<% if (!Utils.isEmpty(studentNumLabel)) { 
				lastLine2Bld.append(EMPTY_TD2); %>
				<td class="boldtext enlarged" style="width:<%= sizes[LABEL] %>px;
						border-bottom-style:solid; border-width:1px;
						border-color:#49521B; padding-left:10px;">
				</td>
			<% } // if studentNumLabel
		} // if for export
		if (forExport) { %>
			<td></td>
		<% } else {
			lastLine2Bld.append(EMPTY_TD2); %>
			<td class="boldtext enlarged" style="width:<%= sizes[TOTAL] %>px; 
					text-align:center; border-bottom-style:solid; 
					border-width:1px; border-color:#49521B;">
			</td>
		<% } // if for export
		for (int grpNum = 0; grpNum < numGroups; grpNum++) {
			final List<Integer> qIdsGroup = groupedQIds.get(grpNum);
			final int numGroupQs = qIdsGroup.size();
			for (int qNumInGrp = 0; qNumInGrp < numGroupQs; qNumInGrp++) {
				final int qId = qIdsGroup.get(qNumInGrp);
				if (forExport) { %>
					<td>
						<% if (numGroupQs > 1) { %>
							#<%= qNumInGrp + 1 %> 
							(#<%= qId %>) 
							<%= details > NONE ? user.translate("pts") : "" %>
						<% } // if there's more than one Q in this group %>
					</td>
					<% if (details > NONE) { %>
						<td>
						<% if (numGroupQs > 1) { %>
							#<%= qNumInGrp + 1 %> <%= user.translate("tries") %>
						<% } // if there's more than one Q in this group %>
						</td>
					<% } // if details %>
				<% } else if (numGroupQs == 1) { 
					lastLine2Bld.append(EMPTY_TD2); %>
					<td style="width:<%= sizes[SCORE] %>px; border-bottom-style:solid;
							border-width:1px; border-color:#49521B;"></td>
				<% } else {
					final String qDisp = Utils.toString('#', Utils.formatNegative(qId));
 					Utils.appendTo(lastLine2Bld, 
							"<td class=\"boldtext enlarged\"", 
							" style=\"text-align:center;"
								+ " border-top-style:solid;"
								+ " border-bottom-style:solid; border-width:1px;");
					if (qNumInGrp == 0) 
						lastLine2Bld.append(" border-left-style:solid;");
					if (qNumInGrp == numGroupQs - 1) 
						lastLine2Bld.append(" border-right-style:solid;");
					Utils.appendTo(lastLine2Bld, "\">", qDisp, "</td>"); 
					final String qStr = Utils.toString("<a href=\"javascript:viewQ(", 
							qId, ",0);\">", qDisp, "</a>");
					%>
					<td class="boldtext enlarged" style="width:<%= sizes[SCORE] %>px;
							text-align:center; border-bottom-style:solid;
							<%= numGroupQs > 1 && qNumInGrp == 0 
								? "border-left-style:solid; " : "" %>
							<%= numGroupQs > 1 && qNumInGrp == numGroupQs - 1 
								? "border-right-style:solid; " : "" %>
							border-width:1px; border-color:#49521B;">
						<%= qStr %>
					</td>
				<% } // if for export
			 } // for each Q in the group
		} // for each group
		if (!forExport) { %>
			<td class="enlarged" style="width:<%= leftOver %>px; text-align:center;
					background-color:white; border-width:1px;
					border-color:#49521B;"></td>
		<% } // if not for export %>
		</tr>
		<!-- end randomized Q headings -->
	<% } // if need a second row %>
<%
	final DateFormat respTimeFormatter = DateFormat.getDateTimeInstance(
			DateFormat.SHORT, DateFormat.MEDIUM);
	respTimeFormatter.setTimeZone(timeZone);
	double totalGrades = 0;
	int totalNumStudentsTried = 0;
	final int start = (view == INSTRUCTOR_ONE ? oneStudentNum : 1);
	final int end = (view == INSTRUCTOR_ONE ? oneStudentNum : numStudents);
	for (int userNum = start; userNum <= end; userNum++) {
		final String rowColor = (userNum % 2 != 0 ? "greenrow" : "whiterow");
	 	final String[] gradeStrs = new String[totalQs];
	 	Arrays.fill(gradeStrs, (details > NONE && forExport
				? UNATTEMPTED_STR + "</td><td>" + UNATTEMPTED_STR
				: UNATTEMPTED_STR));
	 	for (int qPos = 0; qPos < totalQs; qPos++) {
	 		numTries[qPos][userNum - 1] = -1;
	 	}
 		double sumGrade = 0;
		boolean assgtMastered = true;
		int numQsTried = 0;
 		final EvalResult[] evalResults =
				hwGrades.getOrderedResults(hwNum, userIds[userNum - 1]);
		final int[] assignedQIds = (getAssignedQs 
				? Utils.listToIntArray(qIdsByPos)
				: hwGrades.getAssignedQIdsArr(hwNum, userIds[userNum - 1]));
		// decide what to display
		for (qNum = 0; qNum < numQs; qNum++) {
			final EvalResult evalResult = evalResults[qNum];
			if (evalResult != null && !Utils.among(evalResult.status, 
					NO_STATUS, INITIALIZED, SAVED)) {
				String gradeStr = "";
				numQsTried++;
				final boolean humanReqd = evalResult.status == HUMAN_NEEDED;
				final boolean mastered = isMasteryAssgt
						&& evalResult.grade == 1.0 
						&& evalResult.tries <= assgt.getMaxTries();
				if (!mastered) assgtMastered = false;
				final String truncPts = (humanReqd ? HUMAN_NEEDED_SYMB
						: numberFormat.format(evalResult.modGrade));
				if (details > NONE) {
					final StringBuilder gradeStrBld = new StringBuilder().append(
							isMasteryAssgt && !forExport
							? Utils.toString("<img src=\"", pathToRoot, 
								"images/", mastered ? "" : "un", 
								"mastered.png\" title=\"", mastered ? "" : "un", 
								"mastered\" />")
							: Utils.toString("<span style=\"font-weight:bold; color:", 
								humanReqd ? HUMAN_NEEDED_COLOR
									: evalResult.grade == 1.0 ? RIGHT_COLOR
									: evalResult.grade > 0.0 ? PARTIAL_COLOR
									: WRONG_COLOR, 
								";\">", isMasteryAssgt && mastered ? 1
									: isMasteryAssgt && !mastered ? 0
									:truncPts, 
								CLOSESPAN));
					if (!forExport && !isMasteryAssgt) {
 						Utils.appendTo(gradeStrBld, " ", user.translate(
								evalResult.modGrade != 1 ? "pts." : "pt."));
					} // if not for export
					if (forExport) {
 						Utils.appendTo(gradeStrBld, "</td><td>", 
								evalResult.tries);
					} else {
						if (view == INSTRUCTOR_ONE && role != User.TA) {
 							Utils.appendTo(gradeStrBld, "<br/>(", 
									user.translate(evalResult.tries == 1
										? "***1*** attempt"
										: "***2*** attempts", 
										evalResult.tries), 
									')');
							if (!Utils.isEmpty(evalResult.comment)) {
								gradeStrBld.append(" " + COMMENTED);
							} // if there's a comment
 							Utils.appendTo(gradeStrBld, 
									"<br/>(<a href=\"javascript:resetRecord(", 
									evalResult.qId, ",", 
									Utils.indexOf(assignedQIds, evalResult.qId) + 1, 
									")\">", user.translate("alter"), "</a>)");
						} else {
 							Utils.appendTo(gradeStrBld, "<br/>(<a "
										+ "href=\"javascript:showMol('", 
									Utils.toValidJS(userIds[userNum - 1]), 
									"', ", evalResult.qId, 
									")\" title=\"View most recent response\">", 
									user.translate(
										evalResult.tries == 1
											? "***1*** attempt"
											: "***2*** attempts", 
										evalResult.tries), 
									"</a>)");
							if (!Utils.isEmpty(evalResult.comment)) {
								gradeStrBld.append(" " + COMMENTED);
							} // if there's a comment
						} // if view
						if (details == ALL) {
 							Utils.appendTo(gradeStrBld, "<br/>", 
									respTimeFormatter.format(
										evalResult.timeOfResponse));
							if (!Utils.isEmpty(evalResult.ipAddr)) {
 								Utils.appendTo(gradeStrBld, "<br/>", 
										evalResult.ipAddr);
							} // if an IP address is recorded
						} // if instructor view, not an exam
					} // if for export
					gradeStr = gradeStrBld.toString();
				} else { // no details
					gradeStr = truncPts;
			 	} // if details
			 	sumGrade += evalResult.modGrade;
				final int qPos = qIdsByPos.indexOf(Integer.valueOf(evalResult.qId));
				numStudentsTried[qPos]++;
				if (evalResult.grade == 1.0) {
					numCorrectEntries[qPos]++;
					if (isMasteryAssgt && mastered) numMasteredEntries[qPos]++;
				} // if correct
				numTries[qPos][userNum - 1] = evalResult.tries;
				sumScorePerQ[qPos] += evalResult.modGrade;
				gradeStrs[qPos] = gradeStr;
			} else {
				assgtMastered = false;
			} // if there's an evaluated response
		} // for each qNum
		if (numQsTried > 0) totalNumStudentsTried++;
		totalGrades += sumGrade;

 		if (view != STUDENT) {
			String studentNumDisp = (studentNums[userNum - 1] == null
						|| "-1".equals(studentNums[userNum - 1]) 
					? " " : studentNums[userNum - 1]);
			if (forExport) { %>
				<td><%= userNames[userNum - 1] %></td>
				<% if (!Utils.isEmpty(studentNumLabel)) { %>
					<td><%= studentNumDisp %></td>
				<% } 
			} else { %>
				<!-- display the grades of one student -->
				<tr class="<%= rowColor %>" style="height:40px;">
				<td class="regtext" style="border-left-style:solid;
						border-width:1px; border-color:#49521B; padding-left:10px;
						text-align:right; padding-right:10px;">
					<a name="student<%= userNum + 2 %>"></a>
					<%= userNum %>.
				</td>
				<% if (view == INSTRUCTOR_ALL || !hideNames) { %>
					<td id="indivName<%= userNum %>" class="regtext"
							style="padding-left:10px; padding-right:10px;">
						<% final StringBuilder bld = new StringBuilder();
						if (view == INSTRUCTOR_ALL) {
 							Utils.appendTo(bld, 
									"<a href=\"javascript:openOneStudent(", 
									userNum, ")\" title=\"",
									userIds[userNum - 1], "\" alt=\"",
									userIds[userNum - 1], "\">");
						} // if INSTRUCTOR_ALL
						bld.append(userNames[userNum - 1]);
						if (view == INSTRUCTOR_ALL) bld.append("</a>"); %>
						<%= bld.toString() %>
					</td>
				<% } // if have a name column %>
				<% if (!Utils.isEmpty(studentNumLabel)) { 
					if (view == INSTRUCTOR_ALL) { 
						studentNumDisp = Utils.toString(
								"<a href=\"javascript:openOneStudent(", 
								userNum, ")\">", studentNumDisp, "</a>");
					} // if viewing all students %>
					<td class="regtext" style="padding-left:10px;
							padding-right:10px;">
						<%= studentNumDisp %>
					</td>
				<% } // if studentNumLabel
			} // if for export
		} else { // student view %>
			<tr>
			<% if (forExport) { %>
				<td>
			<% } else { %>
				<td class="regtext" style="border-left-style:solid;
						border-width:1px; border-color:#49521B; 
						padding-left:20px; padding-right:10px;">
			<% } // if for export %>
			<%= Utils.isEmpty(userNames[userNum - 1]) 
					? user.translate("Your score") : userNames[userNum - 1] %>
			</td>
		<% } // if view %>
		<% final String td = (forExport ? "<td>"
				: "<td class=\"regtext\" style=\"text-align:center;\">");
		%>
		<%= td %>
			<%= numQsTried == 0 ? UNATTEMPTED_STR 
				: !isMasteryAssgt ? numberFormat.format(sumGrade) 
				: !forExport ? Utils.toString("<img src=\"", pathToRoot, 
					"images/", assgtMastered ? "" : "un", 
					"mastered.png\" title=\"", assgtMastered ? "" : "un", 
					"mastered\" />")
				: assgtMastered ? 1 : 0 %>
		</td>
		<% for (int qPos = 0; qPos < totalQs; qPos++) { %>
			<%= td %>
				<%= gradeStrs[qPos] %>
			</td>
		<% } // for each Q
		if (!forExport) { %>
			<td style="background-color:white; border-left-style:solid;
					border-width:1px; border-color:#49521B;"></td>
		<% } // if not for export %>
		</tr>
	<% } // for each student userNum
	final String rowColorA = (numStudents % 2 != 0 ? "whiterow" : "greenrow");
	final String rowColorB = (numStudents % 2 != 0 ? "greenrow" : "whiterow");
	if (view == INSTRUCTOR_ALL && !forExport) {
		// display averages, etc.
		%>
		<!-- display average score -->
		<% if (!isMasteryAssgt) { %>
	 		<tr class="<%= rowColorA %>">
			<td class="boldtext" style="border-left-style:solid;
					border-width:1px; border-color:#49521B; padding-left:10px;">
				&nbsp;
			</td>
			<td class="boldtext" <%= Utils.isEmpty(studentNumLabel) ? "" : "colspan=\"2\"" %>
					style="padding-left:10px;">
				<%= user.translate("Average score") %>
			</td>
			<td class="boldtext" style="text-align:center;">
				<%= totalNumStudentsTried > 0 
						? numberFormat.format(totalGrades / totalNumStudentsTried)
						: UNATTEMPTED_STR %>
			</td>
			<% for (int qPos = 0; qPos < totalQs; qPos++) {
				double avg = 0;
				if (numStudentsTried[qPos] > 0) {
					avg = sumScorePerQ[qPos] / numStudentsTried[qPos];
				} // if students tried this Q
			%>
				<td class="boldtext" style="text-align:center;">
					<%= numberFormat.format(avg) %>
				</td>
			<% } // for each Q %>
			<td style="background-color:white; border-left-style:solid;
					border-width:1px; border-color:#49521B;"></td>
			</tr>
		<% } // if isMasteryAssgt %>
		<!-- display number of people attempting -->
		<tr class="<%= isMasteryAssgt ? rowColorA : rowColorB %>" >
		<td class="boldtext" style="border-left-style:solid;
				border-width:1px; border-color:#49521B; padding-left:10px;">
			&nbsp;
		</td>
		<td class="boldtext" colspan="<%= Utils.isEmpty(studentNumLabel) ? 2 : 3 %>"
				style="padding-left:10px;">
			<%= user.translate("Number of students attempting") %>
		</td>
		<% for (int qPos = 0; qPos < totalQs; qPos++) { %>
			<td class="boldtext" style="text-align:center;">
				<%= numStudentsTried[qPos] %>
			</td>
		<% } // for each Q %>
		<td style="background-color:white; border-left-style:solid;
				border-width:1px; border-color:#49521B;"></td>
 		</tr>
		<!-- display number of correct responses -->
		<tr class="<%= isMasteryAssgt ? rowColorB : rowColorA %>" >
		<td class="boldtext" style="border-left-style:solid;
				border-width:1px; border-color:#49521B; padding-left:10px;">
			&nbsp;
		</td>
		<td class="boldtext" colspan="<%= Utils.isEmpty(studentNumLabel) ? 2 : 3 %>"
				style="padding-left:10px;">
			<%= user.translate("Number of correct responses") %>
		</td>
		<% for (int qPos = 0; qPos < totalQs; qPos++) { %>
			<td class="boldtext" style="text-align:center;">
				<%= numCorrectEntries[qPos] %>
			</td>
		<% } // for each Q %>
		<td style="background-color:white; border-left-style:solid;
				border-width:1px; border-color:#49521B;"></td>
		</tr>
		<% if (isMasteryAssgt) { %>
			<!-- display number of mastered responses -->
			<tr class="<%= rowColorA %>" >
			<td class="boldtext" style="border-left-style:solid;
					border-width:1px; border-color:#49521B; padding-left:10px;">
				&nbsp;
			</td>
			<td class="boldtext" colspan="<%= Utils.isEmpty(studentNumLabel) ? 2 : 3 %>"
					style="padding-left:10px;">
				<%= user.translate("Number of mastered responses") %>
			</td>
			<% for (int qPos = 0; qPos < totalQs; qPos++) { %>
				<td class="boldtext" style="text-align:center;">
					<%= numMasteredEntries[qPos] %>
				</td>
			<% } // for each Q %>
			<td style="background-color:white; border-left-style:solid;
					border-width:1px; border-color:#49521B;"></td>
			</tr>
		<% } // if mastery assignment %>
		<!-- Median of number of tries -->
		<tr class="<%= rowColorB %>" >
			<td class="boldtext" style="border-left-style:solid;
					border-width:1px; border-color:#49521B; padding-left:10px;">
				&nbsp;
			</td>
			<td class="boldtext" colspan="<%= Utils.isEmpty(studentNumLabel) ? 2 : 3 %>"
					style="padding-left:10px;">
				<%= user.translate("Median of number of tries") %>
			</td>
			<% for (int qPos = 0; qPos < totalQs; qPos++) {
				final int median = MathUtils.findMedian(numTries[qPos]); %>
				<td class="boldtext" style="text-align:center;">
					<%= median != -1 ? String.valueOf(median) : UNATTEMPTED_STR %>
				</td>
			<% } // for each Q %>
			<td style="background-color:white; border-left-style:solid;
					border-width:1px; border-color:#49521B;"></td>
		</tr>
	<% } // if view is INSTRUCTOR_ALL and not for export %>
	<% if (!forExport) { 
		if (view != INSTRUCTOR_ALL || lastLine2Bld.length() == 0) { %>
			<tr>
				<td class="boldtext" colspan="<%= totalQs
						+ ((view == INSTRUCTOR_ALL && !Utils.isEmpty(studentNumLabel))
							|| (view == INSTRUCTOR_ONE && !hideNames) ? 4
						: view != STUDENT ? 3 : 2) %>"
						style="border-top-style:solid; border-width:1px;
								border-color:#49521B;"></td>
			</tr>
		<% } else { %>
			<tr><%= lastLine2Bld.toString() %></tr>
		<% } // if table closed out by lastLine2Bld %>
		<% if (view == INSTRUCTOR_ALL) { %>
			<tr><%= lastLine1Bld.toString() %></tr>
		<% } // if view is INSTRUCTOR_ALL %>
	</table>
	</div>
	<div id="footer">
	<table style="width:95%; margin-left:auto; margin-right:auto;">
		<tr><td style="width:100%; padding-top:10px; padding-left:10px;">
			<table style="margin-left:auto; margin-right:auto;">
			<tr>
			<% if (view == INSTRUCTOR_ALL) { %>
				<td id="namesToggle" style="padding-left:10px;">
					<%= makeButton(user.translate("Hide names"),
							"toggleNames();") %>
				</td>
			<% } // if view %>
			<% if (view == INSTRUCTOR_ONE) { %>
				<% if (oneStudentNum > 1) { %>
					<td style="text-align:left; padding-left:10px;">
						<%= makeButton(user.translate("Previous student"),
								"openOneStudent(", oneStudentNum - 1, ");") %>
					</td>
				<% } // if there's a previous student %>
				<% if (oneStudentNum < numStudents) { %>
					<td style="text-align:left; padding-left:10px;">
						<%= makeButton(user.translate("Next student"),
								"openOneStudent(", oneStudentNum + 1, ");") %>
					</td>
				<% } // if there's a next student %>
				<td style="text-align:left; padding-left:10px;">
					<%= makeButton(user.translate("Show all students"),
							"openOneStudent(-1);") %>
				</td>
			<% } // if view %>
			<td style="text-align:left; padding-left:10px;">
				<select id="changeDetails" onchange="changeDetails();">
					<option value="<%= NONE %>" <%=
							details == NONE ? SELECTED : "" %>>
						<%= user.translate("No details") %>
					</option>
					<option value="<%= SOME %>" <%=
							details == SOME ? SELECTED : "" %>>
						<%= user.translate("Some details") %>
					</option>
					<option value="<%= ALL %>" <%=
							details == ALL ? SELECTED : "" %>>
						<%= user.translate("All details") %>
					</option>
				</select>
			</td>
			<% if (view != STUDENT && role != User.TA 
					&& !forExport && totalNumStudentsTried > 0) { %>
				<td style="text-align:left; padding-left:10px;">
					<%= makeButton(user.translate("Regrade"), 
							"javascript:regradeQuestion(", oneStudentNum, ");") %>
				</td>
				<td style="text-align:left; padding-left:10px;">
					<select id="regradeSelector" name="regradeSelector">
						<option value="0,0">all
						</option>
						<% for (int qPos = 0; qPos < totalQs; qPos++) { %>
							<% if (numStudentsTried[qPos] > 0) { %>
								<option value="<%= qPos + 1 %>,<%= 
									qIdsByPos.get(qPos).intValue() %>"><%= qPos + 1 %>
								</option>
							<% } // if a student has tried it %>
						<% } // for each Q %>
					</select>
				</td>
			<% } // if user is instructor and not for export %>
			<td style="text-align:left; padding-left:10px;">
				<%= makeButton(user.translate("Back to general gradebook"),
						"goGeneral();") %>
			</td>
			<td style="text-align:left; padding-left:10px;">
				<%= makeButton(user.translate("Export grades to spreadsheet"),
						"exportGrades();") %>
			</td>
			</tr></table>
		</td></tr>
	</table>
	</div>
</body>
</html>
<% } else { %>
	</table>
<% } // if for export %>

