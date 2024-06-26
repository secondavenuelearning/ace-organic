<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.EnrollmentData,
	com.epoch.session.GradeSet,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.text.DateFormat,
	java.text.NumberFormat,
	java.util.ArrayList,
	java.util.Calendar,
	java.util.Date,
	java.util.List"
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
	final int[] sizes = {20, 180, 100, 70};
	final int NUM = 0;
	final int NAME = 1;
	final int LABEL = 2;
	final int SCORE = 3;

	final int PTS_PER_Q = Assgt.PTS_PER_Q;
	final int ATTEMPT = Assgt.ATTEMPT;
	final int TIME = Assgt.TIME;
	final boolean PAST_TENSE = Assgt.PAST_TENSE;

	final char STUDENT = 'S';
	final char INSTRUCTOR_ONE = '1';
	final char INSTRUCTOR_ALL = '*';

	final String courseName = course.getName();
	final int courseId = course.getId();
	final String studentNumLabel = user.getInstitutionStudentNumLabel();
	final boolean isTutorialCourse = courseId == AppConfig.tutorialId;
	final int numHWs = Utils.getLength(assgts);

	String[] hwNames;
	String[] tutNames; 
	String[] userIds; 
	String[] userNames; 
	String[] studentNums; 
	String[] userEmails; 
	Date[][] hwDueDates; 
	GradeSet hwGrades;
	GradeSet tutGrades;
	Assgt[] tutAssgts = null;
	final boolean fromDetailed = "true".equals(request.getParameter("fromDetailed"));
	final boolean reloading = "true".equals(request.getParameter("reloading"));
	if (fromDetailed || reloading) {
		synchronized (session) {
			hwGrades = (GradeSet) session.getAttribute("hwGrades");
			hwNames = (String[]) session.getAttribute("hwNames"); 
			tutGrades = (GradeSet) session.getAttribute("tutGrades");
			tutNames = (String[]) session.getAttribute("tutNames"); 
			userIds = (String[]) session.getAttribute("userIds"); 
			userNames = (String[]) session.getAttribute("userNames"); 
			studentNums = (String[]) session.getAttribute("studentNums"); 
			userEmails = (String[]) session.getAttribute("userEmails"); 
			hwDueDates = (Date[][]) session.getAttribute("hwDueDates"); 
		}
		hwGrades.resetCurrent();
		tutAssgts = tutGrades.getAssgts();
		if (isTA) role = (course.tasMayGrade() ? User.INSTRUCTOR : User.TA);
	} else {
		final boolean INCLUDE_TAS = InstructorSession.INCLUDE_TAS;
		final boolean WINNOW_TUTS = true;
		EnrollmentData[] edata = null;
		switch (role) {
			case User.ADMINISTRATOR:
			case User.INSTRUCTOR:
				if (!isTutorialCourse) {
					final InstructorSession instrSess = 
							(InstructorSession) userSess;
					edata = instrSess.getRegisteredUsers(!INCLUDE_TAS);
					tutAssgts = instrSess.getTutorials();
				}
				break;
			case User.STUDENT:
			default: // shouldn't happen
				if (isTA) {
					final InstructorSession instrSess = 
							new InstructorSession(courseId, user);
					role = (course.tasMayGrade() ? User.INSTRUCTOR : User.TA);
					if (!isTutorialCourse) {
						edata = instrSess.getRegisteredUsers(!INCLUDE_TAS);
						tutAssgts = instrSess.getTutorials(role);
					}
				} else { // student is not TA
					edata = new EnrollmentData[] { new EnrollmentData(user) };
					tutAssgts = ((StudentSession) userSess).getTutorials();
				} // if student is not TA
				break;
		} // switch
		final int numEdata = Utils.getLength(edata);
		userIds = new String[numEdata];
		userNames = new String[numEdata];
		studentNums = new String[numEdata];
		userEmails = new String[numEdata];
		hwDueDates = new Date[numEdata][numHWs];
		for (int userNum = 0; userNum < numEdata; userNum++) {
			final EnrollmentData eDatum = edata[userNum];
			userIds[userNum] = eDatum.getUserId();
			userNames[userNum] = eDatum.getName();
			studentNums[userNum] = eDatum.getStudentNum();
			userEmails[userNum] = eDatum.getEmail();
			for (int hwNum = 0; hwNum < numHWs; hwNum++) {
				final Assgt assgt = assgts[hwNum];
				hwDueDates[userNum][hwNum] = 
						assgt.getDueDate(userIds[userNum]);
			} // for each assignment
		} // for each user
		final String oneStudentId = (role == User.STUDENT ? userIds[0] : null);
		final boolean TUTORIALS = true;
		hwGrades = new GradeSet(courseId, assgts, oneStudentId, !TUTORIALS);
		tutGrades = new GradeSet(courseId, tutAssgts, oneStudentId, TUTORIALS);
		hwNames = Assgt.getHWNames(assgts);
		tutNames = Assgt.getHWNames(tutAssgts);
		synchronized (session) {
			session.setAttribute("hwGrades", hwGrades);
			session.setAttribute("hwNames", hwNames);
			session.setAttribute("hwDueDates", hwDueDates); 
			session.setAttribute("tutGrades", tutGrades);
			session.setAttribute("tutNames", tutNames); 
			session.setAttribute("userIds", userIds); 
			session.setAttribute("userNames", userNames); 
			session.setAttribute("studentNums", studentNums); 
			session.setAttribute("userEmails", userEmails); 
		} // synchronized
	} // if fromDetailed or reloading
	final int numTuts = Utils.getLength(tutNames);
	final int numStudents = userIds.length;

	final int oneStudentNum = 
			MathUtils.parseInt(request.getParameter("oneStudentNum"), -1);
	final char view = (role == User.STUDENT ? STUDENT
			: oneStudentNum == -1 ? INSTRUCTOR_ALL : INSTRUCTOR_ONE);

	/* Utils.alwaysPrint("grade.jsp: role = ", role, ", view = ", view,
			", userNames.length = ", userNames.length, 
			", userNames[0] = ", userNames[0]); /**/

	// sets shown in ascending (false) or descending (true) order
	final boolean flip = "true".equals(request.getParameter("flip"));
	final boolean showTuts = "true".equals(request.getParameter("showTuts"));
	final boolean hideNames = "true".equals(request.getParameter("hideNames"));
	final boolean forExport = request.getParameter("forExport") != null;

	final NumberFormat numberFormat = NumberFormat.getInstance(); 
	numberFormat.setMaximumFractionDigits(course.getNumDecimals());

	// in case grades are exported to Excel
	final Date now = new Date();
	final DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
	df.setTimeZone(course.getTimeZone());
	final String filename = Utils.toValidFileName(Utils.toString(
			courseName != null ? courseName : "grades", 
			"_", df.format(now)));

	final double UNATTEMPTED = GradeSet.UNATTEMPTED;
	final String UNATTEMPTED_STR = (forExport ? "" : "-");
	final double HUMAN_REQD = UNATTEMPTED - 1;
	final String HUMAN_REQD_SYMB = "???";
	final String HUMAN_REQD_COLOR = "gray";
	final String HUMAN_REQD_STR = (forExport ? "??" 
			: "<span style=\"font-weight:bold; color:" 
					+ HUMAN_REQD_COLOR + ";\">" + HUMAN_REQD_SYMB + "</span>");
%>

<% if (!forExport) { %>
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE grades</title>
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
	<script type="text/javascript" src="<%= pathToRoot %>js/openwindows.js"></script>
	<script type="text/javascript" src="<%= pathToRoot %>js/jslib.js"></script>
	<script type="text/javascript">
	// <!-- >
		<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
		<%@ include file="/navigation/courseSidebarJS.jsp.h" %>

		var finishedLoading = false;

		var IS_TUTORIAL = true;
		function openDetails(isTutorial, hwIdx, hwsetname) {
			if (!finishedLoading) {
				toAlert('<%= user.translateJS(
						"Please allow the page to finish loading.") %>');
				return;
			}
			var bld = new String.builder().
					append('detailedGrade.jsp?isTutorial=').append(isTutorial).
					append('&hwNum=').append(hwIdx).append('&hwId=');
			if (isTutorial) {
				switch (hwIdx) {
				<% for (int tutNum = 1; tutNum <= numTuts; tutNum++) { %> 
					case <%= tutNum %>: bld.append('<%= tutAssgts[tutNum - 1].id %>'); break;
				<% } // for each tutorial %>
				} // switch
			} else {
				switch (hwIdx) {
				<% for (int hwNum = 1; hwNum <= numHWs; hwNum++) { %> 
					case <%= hwNum %>: bld.append('<%= assgts[hwNum - 1].id %>'); break;
				<% } // for each HW %>
				} // switch
			} // if isTutorial
			<% if (view == INSTRUCTOR_ONE) { %>
				bld.append('&oneStudentNum=<%= oneStudentNum %>'); 
			<% } // if one-student view %>
			if (namesAreHidden || <%= hideNames && view == INSTRUCTOR_ONE %>) 
				bld.append('&hideNames=true');
			self.location.href = bld.toString();
		} // openDetails()

		function openFlipped() {
			var bld = new String.builder().
					append('grade.jsp?reloading=true&flip=<%= 
						!flip %>&showTuts=<%= showTuts %>');
			if (namesAreHidden || <%= hideNames && view == INSTRUCTOR_ONE %>) {
				bld.append('&hideNames=true');
			} // if hiding names
			self.location.href = bld.toString();
		} // openFlipped()

		function toggleTutorials() {
			var bld = new String.builder().
					append('grade.jsp?reloading=true&flip=<%= 
						flip %>&showTuts=<%= !showTuts %>');
			if (namesAreHidden || <%= hideNames && view == INSTRUCTOR_ONE %>) {
				bld.append('&hideNames=true');
			} // if hiding names
			<% if (view == INSTRUCTOR_ONE) { %>
				bld.append('&oneStudentNum=<%= oneStudentNum %>'); 
			<% } // if one-student view %>
			self.location.href = bld.toString();
		} // toggleTutorials()

		var tutorialExplanPainted = false;
		function paintTutorialExplan() {
			var out = '';
			if (!tutorialExplanPainted) {
				out = '<%= user.translateJS(
						"ACE automatically enrolls students in a course "
						+ "containing all tutorials. Columns Tut 1&ndash;***5*** "
						+ "show the progress that students have made towards "
						+ "completing these tutorials.", numTuts) %>';
			}
			setInnerHTML('tutorialExplan', out);
			tutorialExplanPainted = !tutorialExplanPainted;
		} // paintTutorialExplan()

		function exportGrades() {
			this.location.href = 'gradeTxt.jsp?filename='
					+ encodeURIComponent('<%= Utils.toValidFileName(filename) %>')
					+ '&forExport=true';
		} // exportGrades()

		function exportAllGrades() {
			this.location.href = 'allGrades.jsp?filename='
					+ encodeURIComponent('<%= Utils.toValidFileName(filename) %>');
		} // exportGrades()

		var namesAreHidden = <%= view == INSTRUCTOR_ONE && hideNames %>;
		var studentNames = new Array();
		function toggleNames() {
			var cellNum = 1;
			while (true) {
				var cell = document.getElementById('indivName' + cellNum);
				if (cell) {
					if (namesAreHidden) {
						cell.innerHTML = studentNames[cellNum];
					} else {
						studentNames[cellNum] = cell.innerHTML;
						cell.innerHTML = '';
					} 
					cellNum++;
				} else break;
			} // while there are more cells
			if (namesAreHidden) {
				setInnerHTML('namesToggle', '<%= Utils.toValidJS(makeButton( 
						user.translate("Hide names"), "toggleNames();")) %>');
			} else {
				setInnerHTML('namesToggle', '<%= Utils.toValidJS(makeButton( 
						user.translate("Show names"), "toggleNames();")) %>');
			}
			namesAreHidden = !namesAreHidden; 
		} // toggleNames()

		function openOneStudent(userNum) {
			var goBld = new String.builder();
			goBld.append('grade.jsp?reloading=true&showTuts=<%= 
					showTuts %>&oneStudentNum=').append(userNum);
			if (namesAreHidden) goBld.append('&hideNames=true');
			self.location.href = goBld.toString();
		} // openOneStudent()

		function showAllStudents() {
			var goBld = new String.builder();
			goBld.append('grade.jsp?reloading=true&showTuts=<%= showTuts %>');
			if (namesAreHidden) goBld.append('&hideNames=true');
			self.location.href = goBld.toString();
		} // showAllStudents()

		function enableSetLinks() {
			for (var hwNum = 1; hwNum <= <%= numHWs %>; hwNum++) {
				var link = 'link' + (hwNum + <%= numTuts %>);
				enableCell(link);
			}
		} // enableSetLinks()

		function anyExcluded() {
			toAlert('<%= user.translateJS("ACE has excluded from the totals the grades "
					+ "shown in gray.") %>');
		} // anyExcluded()

	// -->
	</script>
</head>
<body style="text-align:center; margin:0px;" 
		onload="setTab('<%= toTabName(user.translateJS(
				"Grade Book")) %>'); finishedLoading = true; <%= 
				hideNames ? "toggleNames();" : "" %>">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

	<div id="contentsWithTabsWithFooter">
<% } // if not forExport %>
<%
	final int width = 626; // pixels
	int numInvisibleHWs = 0;
	for (final Assgt assgt : assgts) {
		if (!assgt.isVisible()) numInvisibleHWs++;
	} // for each assgt
	int tableWidth = sizes[NUM] + sizes[NAME] + sizes[SCORE] * (numHWs - numInvisibleHWs);
	if (showTuts) tableWidth += sizes[SCORE] * numTuts;
	final boolean showStudentNums = !Utils.isEmpty(studentNumLabel);
	if (showStudentNums) tableWidth += sizes[LABEL];
	int leftOver = 5;
	if (tableWidth < width) {
		leftOver = width - tableWidth;
		tableWidth = width;
	} // if tablewidth

	if (numHWs > 0 && !Utils.isEmpty(userIds)) {
		boolean haveAnyGradingParams = false;
		for (final Assgt assgt : assgts) {
			haveAnyGradingParams = assgt.isVisible() && assgt.hasGradingParams();
			if (haveAnyGradingParams) break;
		} // for each assgt
		if (forExport) { %>
			<table>
		<% } else { %>
			<table style="width:<%= width %>px; margin-left:10px; text-align:left;
					margin-right:auto; border-style:none; border-collapse:collapse;">
				<tr><td class="boldtext big" >
					<%= user.translate("Grade Book") %>
				</td></tr>
				<tr><td>
					<div id="mySpan" class="regtext">
					<p>
					<%= user.translate("Total scores of all assignments in "
							+ "***this course***.", Utils.toString(
								"<span class=\"boldtext\">", courseName, "</span>")) %>
					<%
					final String[] keys = new String[]
							{Utils.toString("A hyphen indicates that ", 
								view == STUDENT ? "you have" : "a student has", 
								" not attempted an assignment; the symbol ", 
								HUMAN_REQD_SYMB, " indicates that ", 
								view == STUDENT ? "you have" : "a student has", 
								" responded to a question, but the response requires ", 
								view == STUDENT ? "evaluation by the instructor"
									: "your evaluation", 
								"."),
							Utils.toString("Mouse over a column heading to see the "
									+ "name of that assignment. Click on a column "
									+ "heading to view the grades for each "
									+ "question in that assignment and ", 
								view == STUDENT ? "your" : "students'", 
								" last responses to those questions", 
								view == STUDENT ? "." 
									: ", to reset a student's grade, or to "
										+ "regrade all responses to a question.")}; 
					user.translate(keys);
					%>
					<%= keys[0].replaceAll("\\?\\?\\?", HUMAN_REQD_STR) %>
					</p><p>
					<%= keys[1] %>
					</p>
					<% if (showTuts && numTuts > 1) { %>
						<p><a href="javascript:paintTutorialExplan()"><%=
								user.translate("What are Tut 1&ndash;***5***?", 
								numTuts) %></a>
						<span id="tutorialExplan"></span></p>
					<% } else if (showTuts && numTuts == 1) { %>
						<p><a href="javascript:paintTutorialExplan()"><%=
								user.translate("What is Tut 1?") %></a>
						<span id="tutorialExplan"></span></p>
					<% } // if numTuts %>
					</div>
				</td></tr>
			</table>
			<table style="margin-left:10px; text-align:left;
					margin-right:auto; border-style:none; border-collapse:collapse;">
		<% } // if for export %>
				<tr>
		<% if (view == INSTRUCTOR_ALL) { 
			if (forExport) { %>
				<td><%= user.translate("Name") %></td><%= showStudentNums ?
					Utils.toString("<td>", studentNumLabel, "</td>") : "" %>
			<% } else { %>
				<td class="boldtext enlarged" style="width:<%= sizes[NUM] %>px;
						text-align:center; border-bottom-style:solid; border-width:1px;
						border-color:#49521B; vertical-align:bottom;">
					<%= user.translate("No.") %>
				</td>
				<td id="nameHeader" class="boldtext enlarged" style="width:<%= sizes[NAME] %>px;
						border-bottom-style:solid; border-width:1px;
						border-color:#49521B; vertical-align:bottom;
						padding-left:10px;">
					<%= user.translate("Name") %>
				</td>
				<% if (showStudentNums) { %>
					<td class="boldtext enlarged" style="width:<%= sizes[LABEL] %>px;
							border-bottom-style:solid; 
							border-width:1px; border-color:#49521B; 
							vertical-align:bottom; padding-left:10px;">
						<%= studentNumLabel %>
					</td>
				<% } // if studentNumLabel
			} // if for export
		} else { // student or one-student view
			if (forExport) { %>
				<td></td>
			<% } else { %>
				<td class="boldtext enlarged" style="width:<%= sizes[NUM] %>px;
						text-align:center; border-bottom-style:solid; border-width:1px;
						border-color:#49521B; vertical-align:bottom;">
					<%= user.translate("No.") %>
				</td>
				<td class="boldtext enlarged" style="width:<%= sizes[NAME] %>px;
						border-bottom-style:solid; 
						border-width:1px; border-color:#49521B; 
						vertical-align:bottom; padding-left:20px;">
					<%= user.translate(hideNames ? studentNumLabel : "Name") %>
				</td>
			<% } // if for export
		} // if view == STUDENT or INSTRUCTOR_ONE
		if (showTuts) { 
			for (int tutNum = 1; tutNum <= numTuts; tutNum++) {
				final String newName = 
						Utils.toString(user.translate("Tut"), ' ', tutNum); 
				if (forExport) { %>
					<td><%= newName %></td>
				<% } else { %>
					<td style="width:<%= sizes[SCORE] %>px; 
							text-align:center; border-bottom-style:solid; 
							border-width:1px; border-color:#49521B; 
							vertical-align:top; padding-left:5px; padding-right:5px;">
						<a id="link<%= tutNum %>"
							title="<%= tutNames[tutNum - 1] %>" 
							alt="<%= tutNames[tutNum - 1] %>" 
							onclick="javascript:openDetails(IS_TUTORIAL, <%= tutNum %>,'<%= 
									Utils.toValidJS(tutNames[tutNum - 1]) %>'); return false;">
						<span class="boldtext enlarged" ><u><%= newName %></u></span>
						<% if (haveAnyGradingParams) { %> <br/> <% } %> 
					</td>
				<% } // if forExport
			} // for each tutorial
		} // if showTuts 
		final double[] maxGrades = new double[numHWs];
		final boolean[] excludeFromTotals = new boolean[numHWs];
		boolean anyExcludedFromTotals = false;
		for (int hwNumRaw = 1; hwNumRaw <= numHWs; hwNumRaw++) {
			final int hwNum = (flip ? numHWs - hwNumRaw + 1 : hwNumRaw);
			final Assgt assgt = assgts[hwNum - 1];
			if (!assgt.isVisible()) continue;
			final boolean isMasteryAssgt = assgt.isMasteryAssgt();
			final String prefix = (isMasteryAssgt ? " M" : " ");
			final String newName = Utils.toString(user.translate("Set"), prefix, hwNum); 
			excludeFromTotals[hwNum - 1] = assgt.excludeFromTotals();
			anyExcludedFromTotals = anyExcludedFromTotals || excludeFromTotals[hwNum - 1];
			maxGrades[hwNum - 1] = hwGrades.getMaxGrade(hwNum);
			if (forExport) { %>
				<td><%= newName %></td>
			<% } else { %>
				<td id="td<%= hwNum %>" style="width:<%= sizes[SCORE] %>px;
						text-align:center; border-bottom-style:solid; 
						border-width:1px; border-color:#49521B; 
						vertical-align:top; padding-left:5px; padding-right:5px;">
					<a id="link<%= hwNum + numTuts %>"
						title="<%= hwNames[hwNum - 1] %>" 
						alt="<%= hwNames[hwNum - 1] %>" 
						onclick="javascript:openDetails(!IS_TUTORIAL, <%= hwNum %>,'<%= 
								Utils.toValidJS(hwNames[hwNum - 1]) %>'); return false;">
						<span class="boldtext enlarged" ><u><%= newName %></u></span>
					</a>
					<% if (haveAnyGradingParams) { %> <br/> <% }  
					final boolean haveQuestionParams = assgt.hasGradingParams(PTS_PER_Q);
					final boolean haveAttemptParams = assgt.hasGradingParams(ATTEMPT);
					final boolean haveTimeParams = assgt.hasGradingParams(TIME);
					if (haveQuestionParams || haveAttemptParams || haveTimeParams) {
						final StringBuilder paramsBld = new StringBuilder();
						if (haveQuestionParams) {
							paramsBld.append(assgt.gradingParamsToDisplay(
									PTS_PER_Q, PAST_TENSE, user)); 
							if (haveAttemptParams || haveTimeParams) 
								paramsBld.append("<br/><br/>");
						} // if haveQuestionParams 
						if (haveAttemptParams) {
							paramsBld.append(assgt.gradingParamsToDisplay(
									ATTEMPT, PAST_TENSE, user)); 
							if (haveTimeParams) paramsBld.append("<br/><br/>");
						} // if haveAttemptParams 
						if (haveTimeParams) {
							paramsBld.append(assgt.gradingParamsToDisplay(
									TIME, PAST_TENSE, user));
						} // if haveTimeParams
					%>
						<span class="regtext">[<a 
							onclick="toBigAlert('<%= pathToRoot %>', '<%= 
									Utils.toValidJS(paramsBld.toString()
										).replaceAll("\\n", "<br\\/>") %>')"><%=
									user.translate("grading") %></a>]</span>
					<% } else { %>
						<br/>
					<% } // if this assignment has grading parameters %>
				</td>
			<% } // if not forExport 
		} // for each set hwNumRaw 
		if (!forExport) { %>
			<td id="tdSum" class="boldtext enlarged"
					style="width:<%= sizes[SCORE] %>px; 
					text-align:center; border-bottom-style:solid; 
					border-width:1px; border-color:#49521B; 
					vertical-align:top; padding-left:5px; padding-right:5px;">
				<% if (anyExcludedFromTotals) { %>
					<a onclick="javascript:anyExcluded();"><%= user.translate("Total") %>*</a>
				<% } else { %>
					<%= user.translate("Total") %>
				<% } // if any assignments excluded from the totals %>
			</td>
			<td class="enlarged" style="width:<%= leftOver %>px; text-align:center;
					border-width:1px; border-color:#49521B;">
			</td>
			<!-- individual scores -->
		<% } // not for export %>
				</tr>
		<%
		// Get the total grades of each student
		final double[] allUsersSumScore = new double[numHWs];
		final int[] attemptedStudentCt = new int[numHWs];
		final int[] numNeedGrading = new int[numHWs];
		double sumSumgrades = UNATTEMPTED;
		int anyAttempted = 0;
		int anyNeedGrading = 0;
		double percentAttempted = 0; 
		double percentAll = 0; 
		for (int userNum = 1; userNum <= numStudents; userNum++) {
			if (view == INSTRUCTOR_ONE && userNum != oneStudentNum) continue;
			final String userName = userNames[userNum - 1];
			String rowColor = "whiterow";
			if (userNum % 2 == 0 && view == INSTRUCTOR_ALL) rowColor = "greenrow";
			if (forExport) { %>
				<tr>
			<% } else { %>
				<tr class="<%= rowColor %>">
			<% } 
			if (view == INSTRUCTOR_ALL) { %>
				<% if (forExport) { %>
					<td><%= userName != null ? userName : Utils.toString(
							user.translate("Student"), ' ', userNum) %></td>
					<% if (showStudentNums) { %>
						<td><%= studentNums[userNum - 1] == null 
									|| "-1".equals(studentNums[userNum - 1]) ? 
								" " : studentNums[userNum - 1] %></td>
					<% } // if showStudentNums %>
				<% } else { %>
					<td class="regtext" style="width:<%= sizes[NUM] %>px;
							border-left-style:solid; border-width:1px;
							border-color:#49521B; padding-left:10px; 
							text-align:right; padding-right:10px;">
						<%= userNum %>.
					</td>
					<td id="indivName<%= userNum %>" class="regtext"
							style="width:<%= sizes[NAME] %>px; padding-left:10px;">
					<a href="javascript:openOneStudent(<%= userNum %>)"
						title="<%= userIds[userNum - 1] %>" 
						alt="<%= userIds[userNum - 1] %>">
						<%= userName != null ? userName 
								: Utils.toString(user.translate("Student"), ' ', userNum) %>
					</a>
					</td>
					<% if (showStudentNums) { %>
						<td class="regtext"
								style="width:<%= sizes[LABEL] %>px; padding-left:10px;">
							<a href="javascript:openOneStudent(<%= userNum %>)">
							<%= studentNums[userNum - 1] == null 
										|| "-1".equals(studentNums[userNum - 1]) 
									? "" : studentNums[userNum - 1] %>
							</a>
						</td>
					<% } // if showStudentNums
				} // if forExport
			} else { // STUDENT or INSTRUCTOR_ONE
				if (forExport) { %>
					<td><%= userName != null ? userName 
							: user.translate("Your score") %></td>
				<% } else { %>
					<td class="regtext" style="width:<%= sizes[NUM] %>px;
							border-left-style:solid; border-width:1px;
							border-color:#49521B; padding-left:10px; 
							text-align:right; padding-right:10px;">
						<%= userNum %>.
					</td>
					<td class="regtext" style="width:<%= sizes[NAME] %>px;
							padding-left:20px; padding-right:10px;">
						<%= view == INSTRUCTOR_ONE && hideNames
									&& studentNums[userNum - 1] != null
									&& !"-1".equals(studentNums[userNum - 1])
								? studentNums[userNum - 1]
							: view == INSTRUCTOR_ONE && hideNames ? ""
							: userName != null ? userName 
							: user.translate("Your score") %>
					</td>
				<% } // if for export
			} // if STUDENT or INSTRUCTOR_ONE
			if (showTuts) { 
				final double[] userGrades = 
						tutGrades.getTotalGrades(userIds[userNum - 1]);
				for (int tutNum = 1; tutNum <= tutAssgts.length; tutNum++) {
					final String completed = (userGrades == null ? UNATTEMPTED_STR
							: userGrades[tutNum - 1] <= 0 ? UNATTEMPTED_STR 
							: user.translate(
								userGrades[tutNum - 1] < tutGrades.getMaxGrade(tutNum) 
									? "Begun" : "Done"));
					if (forExport) { %>
						<td><%= completed %></td>
					<% } else { %>
						<td id="stud<%= userNum %>_tut<%= tutNum %>" class="regtext"
								style="width:<%= sizes[SCORE] %>px; text-align:center;">
							<%= completed %>
						</td>
					<% } // if for export 
				} // for each tutorial 
			} // if show tutorials
			final double[] userGrades = 
					hwGrades.getTotalGrades(userIds[userNum - 1]);
			final boolean[] userMasteries =
					hwGrades.getAssgtsMastered(userIds[userNum - 1]);
			double sumgrades = UNATTEMPTED;
			double sumMaxAttempted = 0;
			double sumMaxAll = 0;
			boolean anyHumanReqd = false;
			for (int hwNumRaw = 1; hwNumRaw <= numHWs; hwNumRaw++) {
				final int hwNum = (flip ? numHWs - hwNumRaw + 1 : hwNumRaw);
				final Assgt assgt = assgts[hwNum - 1];
				if (!assgt.isVisible()) continue;
				boolean humanReqd = false;
				double userGrade = (userGrades == null 
						? UNATTEMPTED : userGrades[hwNum - 1]);
				if (hwGrades.getHumanGradingReqd(userIds[userNum - 1], hwNum)) {
					userGrade = HUMAN_REQD;
					humanReqd = true;
					anyHumanReqd = anyHumanReqd || !excludeFromTotals[hwNum - 1];
				} // if human grading required
				final boolean isMasteryAssgt = assgt.isMasteryAssgt();
				final boolean mastered = (Utils.getLength(userMasteries) >= hwNum
						? userMasteries[hwNum - 1] : false);
				final String userGradeStr = (
						userGrade == UNATTEMPTED ? UNATTEMPTED_STR 
						: userGrade == HUMAN_REQD ? HUMAN_REQD_STR 
						: isMasteryAssgt ? (!forExport 
							? Utils.toString("<img src=\"", pathToRoot, 
								"images/", mastered ? "" : "un", 
								"mastered.png\" title=\"", mastered ? "" : "un", 
								"mastered\" />") 
							: mastered ? "1" : "0")
						: numberFormat.format(userGrade)); 
				String userGradePerStr = "";
				if (humanReqd) {
					attemptedStudentCt[hwNum - 1]++;
					numNeedGrading[hwNum - 1]++;
				} else {
					if (!excludeFromTotals[hwNum - 1]) {
						sumMaxAll += maxGrades[hwNum - 1];
					} // if don't exclude from totals
					if (userGrade != UNATTEMPTED) {
						if (!excludeFromTotals[hwNum - 1] && !isMasteryAssgt) {
							if (sumgrades == UNATTEMPTED) sumgrades = 0;
							sumgrades += userGrade;
							allUsersSumScore[hwNum - 1] += userGrade;
							sumMaxAttempted += maxGrades[hwNum - 1];
						} // if not excluding this assignment from the average
						if (!forExport && maxGrades[hwNum - 1] >= 0 && !isMasteryAssgt) {
							final double percent = userGrade * 100 / maxGrades[hwNum - 1]; 
							userGradePerStr = Utils.toString(" (", Math.round(percent), "%)"); 
						} // not forExport
						attemptedStudentCt[hwNum - 1]++;
					} else if (course.isExam()) {
						// this student tried this assignment if he tried any
						if (userGrades != null) { 
							for (int hwNum2 = 0; hwNum2 < numHWs; hwNum2++) {
								if (userGrades[hwNum2] != UNATTEMPTED) {
									attemptedStudentCt[hwNum - 1]++;
									break;
								} // if an assignment was attempted
							} // for each assignment
						} // if userGrades is not null
						sumMaxAttempted += maxGrades[hwNum - 1];
					} // if userGrade 
				} // if humanReqd 
				if (forExport) { %>
					<td><%= userGradeStr %></td>
				<% } else { %>
					<td class="regtext"
							style="width:<%= sizes[SCORE] %>px; text-align:center;
							<%= excludeFromTotals[hwNum - 1] ? "color:gray; " : "" %>">
						<%= userGradeStr %> <%= userGradePerStr %>
					</td>
				<% } // if for export
			} // for each set hwNumRaw
			if (!forExport) { 
				String sumgradesStr = UNATTEMPTED_STR;
				if (anyHumanReqd) {
					sumgradesStr = HUMAN_REQD_STR;
					anyAttempted++;
					anyNeedGrading++;
				} else if (sumgrades != UNATTEMPTED) {
					if (sumSumgrades == UNATTEMPTED) 
						sumSumgrades = 0;
					sumSumgrades += sumgrades;
					anyAttempted++;
					percentAttempted = sumgrades * 100 / sumMaxAttempted; 
					percentAll = sumgrades * 100 / sumMaxAll; 
					sumgradesStr = numberFormat.format(sumgrades);
				} // if anyHumanReqd or sumGrades %>
				<td class="regtext"
						style="width:<%= sizes[SCORE] %>px; text-align:center;">
					<%= sumgradesStr %>
				</td>
				<td class="whiterow" style="width:<%= leftOver %>px; 
						border-left-style:solid; border-width:1px; 
						border-color:#49521B;"></td>
			<% } // if !forExport %>
			</tr>
		<% } // for each user userNum 
		if (forExport) { %>
			<tr>
		<% } else { %>
			<tr class="<%= (numStudents % 2 == 1) ? 
					"greenrow" : "whiterow" %>">
			<!-- Maximum score -->
		<% } // if forExport
		if (forExport && view != STUDENT) { %>
			<td><%= user.translate("Max. possible score") %></td>
					<%= showStudentNums ? "<td></td>" : "" %>
		<% } else if (forExport) { %>
			<td><%= user.translate("Max. possible score") %></td>
		<% } else { %>
			<td class="boldtext" style="border-left-style:solid; 
					border-width:1px; border-color:#49521B; 
					padding-left:10px; padding-right:10px;">
			</td>
			<td class="boldtext" colspan="<%= view == INSTRUCTOR_ALL ? 2 : 1 %>"
					style="padding-left:10px; white-space:nowrap;">
				<%= user.translate("Max. possible score") %>
			</td>
		<% }  // if forExport, view
		if (showTuts) { 
			for (int tutNum = 1; tutNum <= numTuts; tutNum++) { %>
				<td<%= forExport ? "" : " style=\"border-width:0px;\"" %>></td>
			<% } // for each set tutNum 
		}  // if showTuts
		double sumMax = 0;
		for (int hwNumRaw = 1; hwNumRaw <= numHWs; hwNumRaw++) {
			final int hwNum = (flip ? numHWs - hwNumRaw + 1 : hwNumRaw);
			final Assgt assgt = assgts[hwNum - 1];
			if (!assgt.isVisible()) continue;
			final boolean isMasteryAssgt = assgt.isMasteryAssgt();
			final String display = (maxGrades[hwNum - 1] < 0 
					? user.translate("ERROR") 
					: numberFormat.format(maxGrades[hwNum - 1]));
			if (!excludeFromTotals[hwNum - 1] && !isMasteryAssgt) {
				sumMax += (maxGrades[hwNum - 1] < 0 ? 0 : maxGrades[hwNum - 1]);
			} // if should add grade to total
			if (forExport) { %>
				<td><%= isMasteryAssgt ? "" : display %></td>
			<% } else { %>
				<td class="boldtext" style="text-align:center;
						<%= excludeFromTotals[hwNum - 1] ? "color:gray; " : "" %>">
					<%= isMasteryAssgt ? "" : display %>
				</td>
			<% } 
		} // for each set hwNum 
		if (!forExport) { %>
			<td class="boldtext" style="text-align:center;
					border-right-style:solid;
					border-width:1px; border-color:#49521B;">
				<%= numberFormat.format(sumMax) %>
			</td>
		<% } // if !forExport %>
			</tr>
		<% if (view == INSTRUCTOR_ALL && !forExport) { %>
			<tr class="<%= (numStudents % 2 == 0) ? 
					"greenrow" : "whiterow" %>">
			<!-- Number of students attempting -->
			<td class="boldtext" style="border-left-style:solid; 
					border-width:1px; border-color:#49521B; 
					padding-left:10px; padding-right:10px;">
			</td>
			<td class="boldtext" colspan="<%= showStudentNums ? 2 : 1 %>" 
					style="padding-left:10px; white-space:nowrap;">
				<%= user.translate("Number of students attempting") %>
			</td>
			<% if (showTuts) { 
				for (int tutNum = 1; tutNum <= numTuts; tutNum++) { %>
					<td style="border-width:0px;"></td>
				<% } // for each set tutNum 
			} // if showTuts
			for (int hwNumRaw = 1; hwNumRaw <= numHWs; hwNumRaw++) {
				final int hwNum = (flip ? numHWs - hwNumRaw + 1 : hwNumRaw); 
				final Assgt assgt = assgts[hwNum - 1];
				if (!assgt.isVisible()) continue; %>
				<td class="boldtext" style="text-align:center;
						<%= excludeFromTotals[hwNum - 1] ? "color:gray; " : "" %>">
					<%= attemptedStudentCt[hwNum - 1] %>
				</td>
			<% } // for each set hwNum %>
			<td class="boldtext" style="border-right-style:solid;
					border-width:1px; border-color:#49521B;
					text-align:center;">
				<%= anyAttempted %>
			</td>
			</tr>
			<tr class="<%= numStudents % 2 == 1 ? "greenrow" : "whiterow" %>">
			<!-- Average score -->
			<td class="boldtext" style="border-left-style:solid; 
					border-width:1px; border-color:#49521B; 
					padding-left:10px; padding-right:10px;">
			</td>
			<td class="boldtext" colspan="<%= showStudentNums ? 2 : 1 %>" 
					style="padding-left:10px; white-space:nowrap;">
				<%= user.translate("Average score") %>
				<% if (anyNeedGrading > 0) { %>
					(<%= user.translate("excludes ungraded responses") %>)
				<% } %>
			</td>
			<% if (showTuts) { 
				for (int tutNum = 1; tutNum <= numTuts; tutNum++) { %>
					<td style="border-width:0px;"></td>
				<% } // for each set tutNum 
			} 
			for (int hwNumRaw = 1; hwNumRaw <= numHWs; hwNumRaw++) {
				final int hwNum = (flip ? numHWs - hwNumRaw + 1 : hwNumRaw);
				final Assgt assgt = assgts[hwNum - 1];
				if (!assgt.isVisible()) continue;
				String avstr = "";
				if (!assgt.isMasteryAssgt()) {
					double av = 0;
					final int denom = attemptedStudentCt[hwNum - 1] 
							- numNeedGrading[hwNum - 1];
					if (denom > 0) {
						av = allUsersSumScore[hwNum - 1] / (double) denom;
					} // if denom
					avstr = numberFormat.format(av);
				} // if not mastery assignment %>
				<td class="boldtext" style="text-align:center;
						<%= excludeFromTotals[hwNum - 1] ? "color:gray; " : "" %>">
					<%= avstr %>
				</td>
			<% } // for each set hwNum 
			double av = 0;
			String avstr = "";
			if (anyAttempted - anyNeedGrading > 0) {
				av = sumSumgrades / (double) (anyAttempted - anyNeedGrading);
				avstr = numberFormat.format(av);
			} else if (anyAttempted > 0) {
				avstr = HUMAN_REQD_STR;
			}
			%>
			<td class="boldtext" style="border-right-style:solid;
					border-width:1px; border-color:#49521B;
					text-align:center;"><%= avstr %></td>
			</tr>
		<% } // if view != STUDENT and !forExport
		if (!forExport) { %>
			<tr>
				<td class="boldtext" 
				colspan="<%= (showTuts ? numTuts : 0) + numHWs - numInvisibleHWs + 3
						+ (showStudentNums && view == INSTRUCTOR_ALL ? 1 : 0) %>"
				style="border-top-style:solid; border-width:1px;
						border-color:#49521B;"></td>
			</tr>
		<% } %>
		</table>
		<% if (!forExport) { %>
			<br/>
			<% if (view != INSTRUCTOR_ALL) { %>
				<table class="regtext" style="width:<%= width %>px; margin-left:10px; text-align:left;
						margin-right:auto; border-style:none; border-collapse:collapse;">
				<tr><td><%= user.translate(Utils.toString(view == STUDENT 
							? "You" : "The student",
							" earned ***100***% of the points of the assignments that ",
							view == STUDENT ? "you" : "the student", " attempted",
							anyExcludedFromTotals ? ", not counting excluded assignments." : "."),
						Math.round(percentAttempted)) %><%= 
						anyExcludedFromTotals 
							? "<a onclick=\"javascript:anyExcluded();\">*</a>" : "" %>
				</td></tr>
				<tr><td><%= user.translate(Utils.toString(view == STUDENT 
							? "You" : "The student",
							" earned ***100***% of the points of all assignments",
							anyExcludedFromTotals ? ", not counting excluded ones." : "."),
						Math.round(percentAll)) %><%= 
						anyExcludedFromTotals 
							? "<a onclick=\"javascript:anyExcluded();\">*</a>" : "" %>
				</td></tr>
				</table>
			<% } // if view %>
			</div>
			<div id="footer">
			<table style="width:<%= width %>px; margin-left:auto; margin-right:auto;">
				<tr><td style="width:100%; padding-top:10px; padding-left:10px;">
					<table><tr>
					<td>
						<%= makeButton(user.translate("Flip set order"), 
								"openFlipped();") %>
					</td>
					<% if (view == INSTRUCTOR_ALL) { %>
						<td id="namesToggle" style="padding-left:10px;">
							<%= makeButton(user.translate(hideNames 
									? "Show names" : "Hide names"), 
									"toggleNames();") %>
						</td>
					<% } // if INSTRUCTOR_ALL %>
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
						<td style="padding-left:10px;"> 
							<%= makeButton(user.translate("Show all students"), 
									"showAllStudents();") %>
						</td>
					<% } // if INSTRUCTOR_ONE %>
					<td id="tutorialsToggle" style="padding-left:10px;"> 
						<%= makeButton(user.translate(
								showTuts ? "Hide tutorials" : "Show tutorials"), 
								"toggleTutorials();") %>
					</td>
					<td id="exportGrades" style="padding-left:10px;">
						<%= makeButton(user.translate("Export grades to spreadsheet"), 
								"exportGrades();") %>
					</td>
					<td id="exportGrades" style="padding-left:10px;">
						<%= makeButton(user.translate("Export all detailed grades"), 
								"exportAllGrades();") %>
					</td>
					</tr></table>
				</td></tr>
			</table>
		<% } // if !forExport 
	} else { // there are no hws or no students 
		if (!forExport) { %>
			<table style="width:<%= tableWidth %>px; margin-left:auto; text-align:left;
					margin-right:auto; border-style:none; border-collapse:collapse;">
				<tr>
					<td class="boldtext big" >
						<%= user.translate("Grade Book") %>
					</td>
				</tr>
				<tr><td class="regtext" style="border-style:solid;
					border-width:1px; border-color:49521B;
					background-color:#f6f7ed; padding:10px;">
				<% if (view != STUDENT) { %>
					<%= user.translate("There are no students or "
							+ "assignments in this course.") %>
				<% } else { %>
					<%= user.translate("There are no assignments in this course.") %>
				<% } // if view != STUDENT %>
			</td></tr>
			</table>
		<% } // if !forExport 
	} // if there are no hws or no students
if (!forExport) { %>
	</div>
	</body>
	</html>
<% } // if !forExport %>
