<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.EnrollmentData,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.io.File,
	java.util.ArrayList,
	java.util.HashMap,
	java.util.List,
	java.util.Map"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	response.setHeader("Cache-Control", "no-cache, must-revalidate"); // HTTP 1.1
	response.setHeader("Pragma", "no-cache"); // HTTP 1.0
	response.setDateHeader ("Expires", 0); // prevents caching at the proxy server
	final String pathToRoot = "../";

	/* Assignments page of a course 
		Components will be editable for the instructor and administrator
		The session can be of an
			- administrator impersonating an instructor or student
			- instructor
			- student
	*/
	EnrollmentData[] students = new EnrollmentData[0];
	String userId = null; // leave as null if not student
	int[] savedUnsubmittedNums = new int[0];

	final int PTS_PER_Q = Assgt.PTS_PER_Q;
	final int ATTEMPT = Assgt.ATTEMPT;
	final int TIME = Assgt.TIME;
	final boolean PAST_TENSE = Assgt.PAST_TENSE;

	final int courseId = course.getId();
	double sumExtensions = 0.0;
	switch (role) {
		case User.ADMINISTRATOR:
			final AdminSession adminSess = (AdminSession) userSess;
			if (courseId != AppConfig.tutorialId && isInstructor) {
				students = adminSess.getEnrolledStudents();
			} // if instructor and not tutorial course
			break;
		case User.INSTRUCTOR:
			final InstructorSession instrSess = (InstructorSession) userSess;
			if (courseId != AppConfig.tutorialId) {
				students = instrSess.getEnrolledStudents();
			} // if not tutorial course
			if (course.isExam()) {
				instrSess.refreshAssgts(isInstructor);
			} // if is exam
			break;
		case User.STUDENT:
		default: // shouldn't happen
			userId = user.getUserId();
			final StudentSession studSess = (StudentSession) userSess;
			savedUnsubmittedNums = studSess.getHWNumsSavedUnsubmitted();
			if (course.isExam()) {
				studSess.refreshAssgts(isTA);
			} // if is exam
			if (isTA) students = studSess.getEnrolledStudents();
			sumExtensions = studSess.getSumExtensions();
			break;
	} // switch role
	final double maxExtensions = course.getMaxExtensions();
	final String crsBk = course.getBook();
	final String courseBook = (crsBk == null || "Other".equals(crsBk) 
			? "your text" : crsBk);

	Course tutorialCourse; 
	synchronized (session) {	
		tutorialCourse = (Course) session.getAttribute("tutorialCourse");
	}
	final boolean isTutCrs = courseId == tutorialCourse.getId();
	final boolean isTutCrsOwner = 
			user.getUserId().equals(tutorialCourse.getOwnerId());
	final boolean isInstructorMod = 
			isInstructor && (!isTutCrs || isTutCrsOwner);
	final int numHWs = Utils.getLength(assgts);

	final Map<Integer, Integer> assgtNumsByIds = new HashMap<Integer, Integer>(); 
	final boolean[] anySolvingAllowed = new boolean[numHWs];
	for (int hwNum = 0; hwNum < numHWs; hwNum++) { 
		final Assgt assgt = assgts[hwNum];
		assgtNumsByIds.put(Integer.valueOf(assgt.id), Integer.valueOf(hwNum + 1));
		final Map<String, String> extensions = assgt.getExtensions();
		if (!Utils.isEmpty(extensions)) {
			final List<String> extStudentIds = 
					new ArrayList<String>(extensions.keySet());
			for (final String extStudentId : extStudentIds) {
				if (assgt.isSolvingAllowed(extStudentId)) {
					anySolvingAllowed[hwNum] = true;
				} // if student may submit responses to this assignment
			} // for each extension
		} // if there are extensions
	} // for each assignment

	final String EDIT1 = Utils.toValidHTMLAttributeValue(
			user.translate("Assignment name, maximum tries, questions "
			+ "and their dependencies, permissible reaction conditions "
			+ "for synthesis questions"));
	final String EDIT2 = Utils.toValidHTMLAttributeValue(
			user.translate("Due date and time, grading options, extensions"));
	final String SAVED_NOT_SUBMITTED = Utils.toValidHTMLAttributeValue(
			user.translate("contains a response that was saved "
					+ "but not submitted for evaluation"));	

	final String[] loggerDirParts = AppConfig.responseLogDir.split("/web/");
	final String loggerDirRelative = loggerDirParts[loggerDirParts.length - 1];

	final String SELECTED = "selected=\"selected\"";

	final StringBuilder onloadBld = new StringBuilder();
	onloadBld.append("onload=\"setTab('")
			.append(toTabName(user.translateJS("Assignments")))
			.append("');");
	/* if (user.isPastGracePeriod() && !user.hasPaid()) { 
		onloadBld.append(" openPaymentWindow('")
				.append(pathToRoot)
				.append("enroll/payment.jsp');");
	} // if should pay
	/**/
	final String onload = onloadBld.append("\"").toString();

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Assignments List</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<style type="text/css">
		* html body {
			padding:50px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/md5.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/navigation/courseSidebarJS.jsp.h" %>

	function openHW(hwNum) {
		self.location.href = new String.builder().
				append('<%= pathToRoot %>homework/hwmain.jsp?hwNum=').
				append(hwNum).toString();
	} // openHW()

	<% if (role == User.STUDENT && !isTA) { %>

		function selfExtendHW(hwNum) {
			self.location.href = new String.builder().
					append('selfExtend.jsp?hwNum=').append(hwNum).toString();
		} // selfExtendHW()

		function submitUnsubmitted(hwNums) {
			self.location.href = new String.builder().
					append('<%= pathToRoot %>homework/submitUnsubmitted.jsp?hwNums=').
					append(hwNums).toString();
		} // submitUnsubmitted()

	<% } else { %>

		function addHW() {
			self.location.href = 'addNewHWSet.jsp';
		} // addHW()

		function editHW(hwNum, extra) {
			self.location.href = new String.builder().
					append('editHWSet.jsp?hwNum=').append(hwNum).
					append(extra).toString();
		} // editHW()

		function dupeHW(hwNum) {
			self.location.href = new String.builder().
					append('dupeHWSet.jsp?hwNum=').append(hwNum).toString();
		} // dupeHW()

		function editExams() {
			self.location.href = 'editExams.jsp';
		} // editExams()

		function deleteHW(hwNum, hwName) {
			if (toConfirm(hwName + '\n<%= user.translateJS(
					"If you delete this assignment, you will also delete " 
					+ "all grades associated with it. You are advised to save " 
					+ "the grades associated with this assignment before you "
					+ "delete it. Do you wish to continue?") %>')) {
				self.location.href = new String.builder().
						append('deleteHWSet.jsp?hwNum=').append(hwNum).toString();
			} // if confirmed
		} // deleteHW()

		function moveHW(selector, from) {
			var to = selector.value;
			self.location.href = new String.builder()
					.append('moveHWSet.jsp?from=')
					.append(from)
					.append('&to=')
					.append(to)
					.toString();
		} // moveHW()

		function exportHWs() {
			var hwNums = getSelectedValues(document.editform.hwSet_checker);
			if (hwNums.length <= 0) { <% %>
				toAlert('<%= user.translateJS(
						"You must select at least one item to export.") %>');
			} else {
				self.location.href = new String.builder().
						append('exportHWSets.jsp?hwNums=').
						append(hwNums.join(':'))
						.toString();
			} // if hwNums.length
		} // exportHWs()

		function importHWs() {
			self.location.href = 'uploadHWSets.jsp';
		} // importHWs()

		function mergeHWs() {
			var hwNums = getSelectedValues(document.editform.hwSet_checker);
			if (hwNums.length <= 1) { <% %>
				toAlert('<%= user.translateJS(
						"You must select at least two items to merge.") %>');
			} else {
				self.location.href = new String.builder().
						append('mergeHWSets.jsp?hwNums=').
						append(hwNums.join(':')).toString();
			} // if hwNums.length
		} // mergeHWs()

		function showExtensions(hwNum) {
			openBigAlertWindow('showExtensions.jsp?hwNum=' + hwNum);
		} // showExtensions()

		function selectDependencyForHW(dependentHWNum) {
			var masteryHWNum = getCell('selectDependencyForHW' + dependentHWNum).value;
			var toSend = new String.builder()
					.append('dependentHWNum=')
					.append(dependentHWNum)
					.append('&masteryHWNum=')
					.append(masteryHWNum);
			callAJAX('setAssgtDependency.jsp', toSend.toString());
		} // selectDependencyForHW()

		function updatePage() { 
			if (xmlHttp.readyState === 4) { // ready to continue
				// nothing needs to be done
				// alert('dependency has been set');
			} // if ready
		} // updatePage()

	<% } // if instructor %>

	// -->
</script>
</head>

<body style="text-align:center; margin:0px;" <%= onload %>>

	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

<form name="editform" action="dummy">
<div id="contentsWithTabsWithoutFooter">
<table class="regtext" style="width:90%; margin-left:auto; margin-right:auto;
		border-style:none; border-collapse:collapse;" summary="">
	<tr>
		<td class="boldtext big" style="padding-bottom:10px;"
				colspan="<%= isInstructorMod ? 9 : 4 %>">
			<%= user.translate("Assignments") %>
		</td>
	</tr>

<% if (numHWs > 0) { %>
	<tr>
		<td class="boldtext enlarged" style="border-bottom-style:solid;
				border-width:1px; border-color:#49521B;
				padding-left:5px; padding-right:10px;">
			<%= isInstructorMod ? user.translate("Move") : "" %>	
		</td>
		<td class="boldtext enlarged" style="border-bottom-style:solid;
				border-width:1px; border-color:#49521B;
				padding-right:5px;">
			<%= user.translate("Do assignment") %>
		</td>
		<td class="boldtext enlarged" style="border-bottom-style:solid;
				border-width:1px; border-color:#49521B;
				padding-right:5px; text-align:center;">
			<%= user.translate("Length") %>
		</td>
		<td class="boldtext enlarged" style="padding-left:20px;
				border-bottom-style:solid;
				border-width:1px; border-color:#49521B;">
			<%= user.translate("Due date") %>
		</td>
		<% if (isInstructorMod) { %>
			<td class="boldtext enlarged" style="border-bottom-style:solid;
					border-width:1px; border-color:#49521B;
					text-align:center; padding-left:10px;"
					title="<%= EDIT1 %>" alt="<%= EDIT1 %>">
				<%= user.translate("Edit") %>&nbsp;1
			</td>
			<td class="boldtext enlarged" style="border-bottom-style:solid;
					border-width:1px; border-color:#49521B;
					text-align:center; padding-left:10px;"
					title="<%= EDIT2 %>" alt="<%= EDIT2 %>">
				<%= user.translate("Edit") %>&nbsp;2
			</td>
			<td class="boldtext enlarged" style="border-bottom-style:solid;
					border-width:1px; border-color:#49521B;
					text-align:center; padding-left:10px;">
				<%= user.translate("Clone") %>
			</td>
			<td class="boldtext enlarged" style="border-bottom-style:solid;
					border-width:1px; border-color:#49521B;
					text-align:center; padding-left:10px;">
				<%= user.translate("Delete") %>
			</td>
			<td class="boldtext enlarged" style="border-bottom-style:solid;
					border-width:1px; border-color:#49521B;
					text-align:center; padding-left:10px;">
				<%= user.translate("Depends on") %>
			</td>
			<td class="boldtext enlarged" style="width:100px;
					border-bottom-style:solid; border-width:1px;
					border-color:#49521B;
					text-align:center;">
				<input type="checkbox" title="check all"
				onclick="setAllCheckBoxes(document.editform.hwSet_checker, this.checked)" />
				<%= user.translate("Export all") %>
			</td>
		<% } else if (isTA) { %>
			<td class="boldtext enlarged" style="border-bottom-style:solid;
					border-width:1px; border-color:#49521B;
					text-align:center; padding-left:10px;"
					title="<%= EDIT2 %>" alt="<%= EDIT2 %>">
				<%= user.translate("Edit") %>
			</td>
			<td class="boldtext enlarged" style="border-bottom-style:solid;
					border-width:1px; border-color:#49521B;
					text-align:center; padding-left:10px;">
				<%= user.translate("Depends on") %>
			</td>
		<% } else { %>
			<td class="boldtext enlarged" style="border-bottom-style:solid;
					border-width:1px; border-color:#49521B;
					text-align:center; padding-left:10px;">
				<% if (!course.isExam() && maxExtensions > 0.0) { %>
					<%= user.translate("Self-granted Extensions") %>
					<br/>(<%= Math.max(maxExtensions - sumExtensions, 0.0) %>
					<%= user.translate("days remaining") %>)
				<% } // if not an exam course %>
			</td>
		<% } // if isInstructorMod %>
	</tr>
	<% for (int hwNum = 1; hwNum <= numHWs; hwNum++) { 
		final Assgt assgt = assgts[hwNum - 1];
		final String rowColor = (hwNum % 2 == 0 ? "whiterow" : "greenrow");
		final boolean isExam = assgt.isExam();
		final int numQsSeen = assgt.getNumQsSeen();
		String dueDateStr = assgt.getDueDateString(userId, course.getTimeZone(), 
				user.prefersDay1st());
		if (assgt.isTimed()) { 
			dueDateStr = Utils.toString(dueDateStr, "<br/>",
					user.translate("or ***10*** minutes past first entry",
						assgt.getDuration() + (int) assgt.getExtension(userId)));
		} else if (assgt.recordAfterDue()) {
			dueDateStr = Utils.toString(dueDateStr, " (",
					user.translate("suggested"), ')');
		} // if assignment is timed or deadline is suggested
		final double totalPts = assgt.getMaxGrade(); 
		String totalPtsStr = String.valueOf(totalPts); 
		if (totalPtsStr.endsWith(".0")) {
			totalPtsStr = Utils.rightChop(totalPtsStr, 2);
		} // if string ends in .0
	%>
		<tr class="<%= rowColor %>">
			<td class="regtext" style="padding-left:5px; border-width:1px;
					border-color:#49521B; border-left:solid; border-width:1px;
					border-color:#49521B;">
				<% if (isInstructorMod) { %>
					<select onchange="moveHW(this, <%= hwNum %>)" title="move">
					<% for (int optHwNum = 1; optHwNum <= numHWs; 
							optHwNum++) { %>
						<option value="<%= optHwNum %>" <%= hwNum == optHwNum 
								? SELECTED : "" %>><%= optHwNum %>
						</option>
					<% } // for each assignment optHwNum %>
					</select>
				<% } else { %>
					<%= hwNum %>. 
				<% } // if isInstructorMod %>
			</td>
			<td class="regtext" style="border-width:1px; border-color:#49521B;
					padding-left:5px; padding-right:5px;">
				<% if (isInstructorMod || isTA 
						|| assgt.getDependsOnId() == 0 
						|| assgt.hasMasteredRequiredHW(userId)) { %>
					<a href="javascript:openHW(<%= hwNum %>)" 
							<%= isInstructorMod || isTA 
								? Utils.toString("title=\"ID ", assgt.id, "\"") : "" %>>
					<%= Utils.toDisplay(assgt.getName()) %></a>
				<% } else if (!isInstructorMod && !isTA) { 
					/* Utils.alwaysPrint("hwSetsList.jsp: assignment ", hwNum,
							" with ID ", assgt.id, 
							" depends on assignment with ID ",
							assgt.getDependsOnId(), " which is assignment #",
							assgtNumsByIds.get(Integer.valueOf(
								assgt.getDependsOnId()))); /**/ %>
					<%= Utils.toDisplay(assgt.getName()) %>
					<span style="color:#FF0000;">[<%=
							user.translate("can be solved after assignment "
									+ "***1*** is mastered",
								assgtNumsByIds.get(Integer.valueOf(
									assgt.getDependsOnId()))) 
							%>]</span>
				<% } // if student who has not mastered assignment
				if (!assgt.isVisible()) { %>
					<span style="color:#FF0000;">[<%=
							 user.translate("invisible") %>]</span>
				<% } // if not visible to students %>
				<% if (isExam) { %>
					<span style="color:#FF0000;">[<%= user.translate(
							assgt.isTimed() ? "timed exam" : "exam") %>]</span>
				<% } // if is exam %>
				<% if (!assgt.isSolvingAllowed(userId)) { %>
					<span style="color:#FF0000;">[<%=
							user.translate("view/practice only") %>]</span>
				<% } // if past due %>
				<% if (Utils.contains(savedUnsubmittedNums, hwNum)) { %>
					<img src="<%= pathToRoot %>images/savedUnsubmitted.gif"
						title="<%= SAVED_NOT_SUBMITTED %>"
						alt="<%= SAVED_NOT_SUBMITTED %>"/>
				<% } // if there's a saved but not submitted response %>
			</td>
			<td class="regtext" style="border-width:1px; border-color:#49521B;
					text-align:center; padding-left:5px; padding-right:5px;">
				<%= numQsSeen %> <%= user.translate(numQsSeen == 1 
						? "question" : "questions") %>
				<br />(<%= totalPtsStr %> <%= user.translate(totalPts == 1.0 
						? "point" : "points") %>)
			</td>
			<% if (isInstructorMod) { %>
				<td class="regtext" style="padding-left:20px;">
					<%= dueDateStr %>
				</td>
				<td class="regtext" style="border-width:1px; padding-left:10px;
						border-color:#49521B; text-align:center;"
						title="<%= EDIT1 %>" alt="<%= EDIT1 %>">
					<%= makeButtonIcon("edit", pathToRoot, 
							"editHW(", hwNum, ", '');") %>
				</td>
				<td class="regtext" style="border-width:1px; padding-left:10px;
						border-color:#49521B; text-align:center;"
						title="<%= EDIT2 %>" alt="<%= EDIT2 %>">
					<%= makeButtonIcon("edit", pathToRoot, 
							"editHW(", hwNum, ", '%26editAction=exit');") %>
				</td>
				<td class="regtext" style="border-width:1px; padding-left:10px;
						border-color:#49521B; text-align:center;">
					<%= makeButtonIcon("duplicate", pathToRoot,
							"dupeHW(", hwNum, ");") %>
				</td>
				<td class="regtext" style="border-width:1px; padding-left:10px;
						border-color:#49521B; text-align:center;">
					<%= makeButtonIcon("delete", pathToRoot,
							"deleteHW(", hwNum, ", '", 
							Utils.toValidJS(assgt.getName()), "');") %>
				</td>
				<td class="regtext" style="border-width:1px; padding-left:10px;
						border-color:#49521B; text-align:center;">
					<select id="selectDependencyForHW<%= hwNum %>"
							onchange="selectDependencyForHW(<%= hwNum %>);">
						<option value="0"></option>
						<% for (int optNum = 1; optNum <= numHWs; optNum++) { 
							final Assgt anAssgt = assgts[optNum - 1];
							if (optNum != hwNum && anAssgt.isVisible()
									&& anAssgt.isMasteryAssgt()) { %>
								<option value="<%= optNum %>"
									<%= assgt.getDependsOnId() == anAssgt.id
										? SELECTED : "" %>><%= optNum %></option>
							<% } // if visible mastery assignment
						} // for each option %>
					</select>
				</td>
				<td class="regtext" style="border-width:1px;
						border-color:#49521B; text-align:center;
						border-right-style:solid; border-width:1px;">
					<input type="checkbox" title="check" name="hwSet_checker"
							value="<%= hwNum %>" />
				</td>
			<% } else if (isTA) { %>
				<td class="regtext" style="border-width:1px; 
						padding-left:10px; border-color:#49521B;">
					<%= dueDateStr %>
				</td>
				<td class="regtext" style="border-width:1px; padding-left:10px;
						border-color:#49521B; text-align:center;"
						title="<%= EDIT2 %>" alt="<%= EDIT2 %>">
					<%= makeButtonIcon("edit", pathToRoot, 
							"editHW(", hwNum, ", '%26editAction=exit');") %>
				</td>
				<td class="regtext" style="border-width:1px; padding-left:10px;
						border-right-style:solid;
						border-color:#49521B; text-align:center;">
					<select id="selectDependencyForHW<%= hwNum %>">
						<% final Integer masteryAssgtIdObj =
								assgtNumsByIds.get(Integer.valueOf(
									assgt.getDependsOnId())); %> 
						<option><%= masteryAssgtIdObj == null ? " " 
								: masteryAssgtIdObj %></option>
					</select>
				</td>
			<% } else { %>
				<td class="regtext" style="border-width:1px; 
						padding-left:10px; border-color:#49521B;">
					<%= dueDateStr %>
				</td>
				<td class="regtext" colspan="3" style="text-align:center; 
						border-right-style:solid; border-width:1px; 
						border-color:#49521B; padding-left:20px;">
					<% final double currentExtension = assgt.getExtension(userId); 
					final boolean allowSelfExtension =
							!course.isExam() && !assgt.isExam()
							&& (!assgt.recordAfterDue()
								|| assgt.hasGradingParams(TIME))
							&& assgt.isSolvingAllowed() // before general due date
							&& (maxExtensions > sumExtensions
								|| currentExtension > 0.0)
							&& assgt.getMaxExtension() != 0.0;
					/* Utils.alwaysPrint("hwSetsList.jsp: for HW ", hwNum,
							", currentExtension = ",
							currentExtension, ", course.isExam() = ",
							course.isExam(), ", assgt.isExam() = ",
							assgt.isExam(), ", assgt.recordAfterDue() = ",
							assgt.recordAfterDue(), 
							", assgt.hasGradingParams(TIME) = ",
							assgt.hasGradingParams(TIME), 
							", assgt.isSolvingAllowed() = ",
							assgt.isSolvingAllowed(),
							", maxExtensions = ", maxExtensions,
							", sumExtensions = ", sumExtensions,
							", assgt.getMaxExtension() = ",
							assgt.getMaxExtension(),
							", allowSelfExtension = ", allowSelfExtension); /**/
					final StringBuilder extHrefBld = new StringBuilder();
					if (allowSelfExtension) {
 						Utils.appendTo(extHrefBld, "<a href=\"javascript:"
								+ "selfExtendHW(", hwNum, ");\">");
					} // if allowSelfExtension
					if (maxExtensions > 0.0) {
						extHrefBld.append(
								currentExtension == 0.0 ? user.translate("none")
								: user.translate(currentExtension == 1.0
									? "***1*** day" : "***4*** days",
									assgt.getExtensionStr(userId)));
					} // if maxExtensions
					if (allowSelfExtension) extHrefBld.append("</a>"); %> 
					<%= extHrefBld.toString() %>
				</td>
			<% } // if isInstructorMod %>
		</tr>
		<tr class="<%= rowColor %>">
			<td class="regtext" style="padding-left:10px; border-left:solid; 
					border-width:1px; border-color:#49521B;">
			</td>
			<td class="regtext" style="padding-left:20px; border-right:solid; 
					border-width:1px; border-color:#49521B;" 
					colspan="<%= isInstructorMod ? 10 : isTA ? 5 : 4 %>">
				<table style="vertical-align:top; border-width:0px;
						text-align:left;" summary="">
					<% if (assgt.hasExtensions() && (isInstructorMod || isTA)) { 
						final String extColor1 = (anySolvingAllowed[hwNum - 1] ? ""
								: "<span style=\"color:#E0E0E0;\">");
						final String extColor2 = (anySolvingAllowed[hwNum - 1] ? ""
								: "</span>"); %>
						<tr><td style="vertical-align:middle;<%= 
								!anySolvingAllowed[hwNum - 1] ? " color:#E0E0E0;" : "" %>">
							[<a onclick="showExtensions(<%= hwNum %>);"><%= extColor1 %><%=
									user.translate("extensions") %><%= extColor2 %></a>]
						</td></tr>
					<% } // if isInstructorMod and there are extensions
					final int[] paramNums = new int[] {ATTEMPT, TIME};
					final String[] paramNames = new String[] {
							"attempt-dependent grading parameters",
							"time-dependent grading parameters"};
					final String BR = "<br\\\\/>";
					final String NL = "\\\\n";
					for (int paramNum = 0; paramNum < 2; paramNum++) {
						if (assgt.hasGradingParams(paramNums[paramNum])) { %>
						<tr><td <%= isInstructorMod ? "colspan=\"2\"" : "" %>>
							[<a onclick="toBigAlert('<%= pathToRoot %>', '<%= 
									Utils.toValidJS(assgt.gradingParamsToDisplay(
										paramNums[paramNum], !PAST_TENSE, user)
									).replaceAll(NL, BR) %>');"><%= 
								user.translate(paramNames[paramNum]) %></a>]
						</td></tr>
						<% } // if there are grading parameters of this type
					} // for each type of grading parameter
					if (isInstructorMod || isTA) { 
						final String logResponseFile = Utils.toString(
								AppConfig.responseLogDir, "exam", assgt.id, 
								".html"); 
						final String logEntryFile = Utils.toString(
								AppConfig.responseLogDir, "exam", assgt.id, 
								"Entry.html"); 
						final boolean respLogExists = 
								(new File(logResponseFile)).exists();
						final boolean entryLogExists = 
								(new File(logEntryFile)).exists();
						if (respLogExists || entryLogExists) {
							final StringBuilder respLogBld = new StringBuilder();
							final StringBuilder entryLogBld = new StringBuilder();
							if (respLogExists) {
 								Utils.appendTo(respLogBld, "<a href=\"", 
										pathToRoot, loggerDirRelative, "exam", 
										assgt.id, ".html\" target=\"window", 
										hwNum, "\"><u>", 
										user.translate("response"), "</u></a>");
							} else respLogBld.append(user.translate("response"));
							if (entryLogExists) {
 								Utils.appendTo(entryLogBld, "<a href=\"", 
										pathToRoot, loggerDirRelative, "exam", 
										assgt.id, "Entry.html\" target=\"window", 
										hwNum + numHWs, "\"><u>", 
										user.translate("entry"), "</u></a>");
							} else entryLogBld.append(user.translate("entry"));
							final String phrase = user.translate(
									"exam ***response*** and ***entry*** logs", 
									new String[] {respLogBld.toString(),
											entryLogBld.toString() });
					%>
							<tr><td <%= isInstructorMod ? "colspan=\"2\"" : "" %>>
								[<%= phrase %>]
							</td></tr>
					<% 	} // if there's a response or entry log
					} // if is exam & is instructor or TA %>
				</table>
			</td>
		</tr>
	<% } // for each assignment hwNum %>
	<tr>
		<td colspan="<%= isInstructorMod ? 10 : 5 %>" style="border-top-style:solid; 
				border-width:1px; border-color:#49521B;">
				<% if (isInstructorMod) { %>
					<table style="margin-left:auto; margin-right:0px; text-align:left;"
							summary="">
					<tr>
					<td><%= makeButton(user.translate("Add New"), "addHW();") %></td>
					<% if (course.isExam() && numHWs > 1) { %>	
						<td><%= makeButton(user.translate("Edit exam features"), 
								"editExams();") %></td>
					<% } // if isExam %>
					<td><%= makeButton(user.translate("Import"), "importHWs();") %></td>
					<td><%= makeButton(user.translate("Export"), "exportHWs();") %></td>
					<td><%= makeButton(user.translate("Merge"), "mergeHWs();") %></td>
					</tr>
					</table>
				<% } else if (!Utils.isEmpty(savedUnsubmittedNums)) { %>	
					<table style="margin-left:auto; margin-right:0px; text-align:left;"
							summary="">
					<tr><td><%= makeButton(user.translate("Submit unsubmitted"), 
							"submitUnsubmitted('", 
							Utils.join(savedUnsubmittedNums, ":"), "');") %>
					</td></tr>
					</table>
				<% } else { %>
					<br/><br/>
				<% } // if isInstructorMod %>
		</td>
	</tr>
<% } else { // there are no assignments %>
	<tr>
		<td colspan="<%= isInstructorMod ? 7 : 2 %>" style="border-style:solid; border-width:1px; 
				border-color:#49521B; padding:10px; background-color:#f6f7ed;">
			<%= user.translate("There are no assignments currently available.") %><br />
			<% if (isInstructorMod) { %>
			<%= user.translate("Click <b>Add New</b> to add an assignment.") %>
			<% } // if isInstructorMod %>
		</td>
	</tr>
	<tr><td colspan="<%= isInstructorMod ? 7 : 2 %>" style="text-align:right;">
	<% if (isInstructorMod) { %>
		<table style="margin-left:auto;" summary=""><tr>
			<td><%= makeButton(user.translate("Add New"), "addHW();") %></td>
			<td><%= makeButton(user.translate("Import"), "importHWs();") %></td>
		</tr></table>
	<% } // if isInstructorMod %>
	</td></tr>
<% } // if there are assignments %>
<% if (isInstructorMod && (courseBook.startsWith("Bruice")
		|| courseBook.startsWith("Wade"))) { %>
	<% if (numHWs > 0) { %>
		<tr><td colspan="4" style="text-align:left; padding-top:20px;">
	<% } else { %>
		<tr><td style="text-align:left; padding-top:20px; width:60%;">
	<% } // if there's an assignment %>
		<h3><%= user.translate("Looking for more questions?") %></h3>
		<p><%= user.translate(
				"In addition to the questions associated with ***Bruice***, ACE "
				+ "also contains many questions written by one of ACE's creators "
				+ "(they are labelled RBG) as well as questions "
				+ "from other sources. These questions aren't included among those "
				+ "listed for each chapter in ***Bruice***, but you can find them "
				+ "by viewing the question bank by topic organization instead of by "
				+ "chapter organization when you create or modify an assignment. "
				+ "(Look for the link on the upper left.)", 
				new String[] {courseBook, courseBook}) %></p>
 
		<!-- <p>For your convenience, we've already assembled some assignments 
		that contain questions that are relevant to chapters in your textbook 
		but don't duplicate the questions that are available through the chapter listing. 
		<a href="javascript:paintAssgntInstructions()">Check them out</a>.
		-->
	</td></tr>
<% } // if isInstructorMod, uses Wade or Bruice  %>
</table>
</div>
</form>
</body>
</html>
