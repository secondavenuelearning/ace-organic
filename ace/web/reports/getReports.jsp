<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.assgts.Assgt,
	com.epoch.courseware.Course,
	com.epoch.courseware.EnrollmentData,
	com.epoch.evals.EvalResult,
	com.epoch.session.CrossCourseReport,
	com.epoch.utils.Utils,
	java.text.NumberFormat,
	java.util.ArrayList,
	java.util.HashMap,
	java.util.List,
	java.util.Map"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");

	Course[] courses;
	EnrollmentData[] students;
	synchronized (session) {
		students = (EnrollmentData[]) session.getAttribute("allEnrolledStudents");
		courses = (Course[]) session.getAttribute("courses");
	}
	final StudentSession studSess = (StudentSession) userSess;
	final String studentIndicesStr = request.getParameter("studentIndices");
	final String courseIdsStr = request.getParameter("courseIds");
	final String searchExp = Utils.inputToCERs(request.getParameter("searchExp"));
	final int[] studentIndices = 
			Utils.stringToIntArray(studentIndicesStr.split(":"));
	final int[] courseIds = 
			Utils.stringToIntArray(courseIdsStr.split(":"));
	final boolean isInstructorOrTA = 
			"true".equals(request.getParameter("isInstructorOrTA"));
	/* Utils.alwaysPrint("getReports.jsp: studentIndices = ", studentIndices, 
			", courseIds = ", courseIds, ", searchExp = ", searchExp,
			", isInstructorOrTA = ", isInstructorOrTA); /**/
	final String[] studentIds = new String[studentIndices.length];
	final Map<String, EnrollmentData> studentDataByIds =
			new HashMap<String, EnrollmentData>();
	int studNum = 0;
	for (final int studentIndex : studentIndices) {
		final EnrollmentData student = students[studentIndex - 1];
		studentIds[studNum++] = student.getUserId();
		studentDataByIds.put(student.getUserId(), student);
	} // for each selected student
	final Map<Integer, Course> coursesByIds = new HashMap<Integer, Course>();
	for (final Course course : courses) {
		final int courseId = course.getId();
		coursesByIds.put(Integer.valueOf(courseId), course);
	} // for each course
	final CrossCourseReport allResults = 
			studSess.getCrossCourseReport(studentIds, courseIds, searchExp);

	final String goBack = request.getParameter("goBack");
	final boolean forExport = "true".equals(request.getParameter("forExport"));

	final char HUMAN_NEEDED = EvalResult.HUMAN_NEEDED;
	final String CLOSESPAN = "</span>";
	final String HUMAN_NEEDED_SYMB = "???";
	final String RIGHT_COLOR = "blue"; // "#000000";
	final String PARTIAL_COLOR = "purple"; // "#FE8502";
	final String WRONG_COLOR = "red"; // "#FF0000";
	final String HUMAN_NEEDED_COLOR = "gray"; // "#FF0000";
	final String COMMENTED = "&radic;";

	final String filename = Utils.toValidFileName(
			Utils.toString("gradesSearch_", searchExp));
%>
<% if (!forExport) { %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<meta http-equiv="Pragma" content="no-cache"/>
<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<title>ACE Student Reports</title>
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<style type="text/css">
		#footer {
			position:absolute; 
			bottom:0; 
			left:0;
			width:100%; 
			height:50px; 
			overflow:auto; 
			text-align:right; 
		}

		* html body {
			padding:55px 0 50px 0; 
		}
</style>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

	function viewQ(courseId, hwId, hwNum, qId, studentId) {
		openPreviewWindow(getUrl('startView.jsp', courseId, hwId, hwNum, qId, studentId));
	} // viewQ()

	function showMol(courseId, hwId, hwNum, qId, studentId) {
		openMolShowWindow(getUrl('showMol.jsp', courseId, hwId, hwNum, qId, studentId));
	} // showMol()

	function getUrl(filename, courseId, hwId, hwNum, qId, studentId) {
		var go = new String.builder().append(filename).
				append('?courseId=').append(courseId).
				append('&hwNum=').append(hwNum).append('&hwId=').append(hwId).
				append('&qId=').append(qId).append('&userId=').
				append(encodeURIComponent(studentId)).
				append('&isInstructorOrTA=<%= isInstructorOrTA %>');
		return go.toString();
	} // getUrl()

	function exportMe() {
		document.exportForm.submit();
	} // exportMe()

	function goBack() {
		self.location.href = 'chooseStudents.jsp?courseIds=<%= 
				courseIdsStr %>&studentIndices=<%= studentIndicesStr %>&searchExp=' 
				+ encodeURIComponent('<%= Utils.toValidJS(searchExp) %>');
	} // goBack()

	function goBackAgain() {
		self.location.href = '<%= Utils.toValidJS(goBack) %>';
	}

	// -->

</script>
</head>
<body class="light" style="background-color:white;">
<% } else { %>
	<body>
<% } // if not for export %>
<% if (!forExport) { %>
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<div id="contentsWithoutTabsWithFooter">

	<form name="exportForm" action="reportTxt.jsp" method="post">
	<input type="hidden" name="studentIndices" value="<%= studentIndicesStr %>" />
	<input type="hidden" name="courseIds" value="<%= courseIdsStr %>" />
	<input type="hidden" name="isInstructorOrTA" value="<%= isInstructorOrTA %>" />
	<input type="hidden" name="searchExp" value="<%= Utils.toValidHTMLAttributeValue(searchExp) %>" />
	<input type="hidden" name="filename" value="<%= Utils.toValidHTMLAttributeValue(filename) %>" />
	<input type="hidden" name="forExport" value="true" />
	</form>

	<p class="boldtext big" style="margin-left:10px; vertical-align:top; 
			padding-bottom:10px;">
		<%= user.translate("Crosscourse reports") %>
	</p>

	<p class="regtext" style="margin-left:20px; vertical-align:top; 
			padding-bottom:10px;">
		<%= user.translate("Responses to questions containing the term(s): ") %> 
		<%= Utils.spanString(searchExp) %>
	</p>

<% } // if not for export %>
<% if (allResults.isEmpty() && !forExport) { %>
	<p class="regtext" style="margin-left:20px;"><%= 
			user.translate("Alas, ACE found no results that met those criteria.") %>
<% } else { 
	final String style = (forExport ? "" : "style=\"padding-left:10px;\"");
	final String COMMA = ",";
%>
<table class="whiteTable" style="width:95%; margin-left:auto; 
		margin-right:auto; border-collapse:collapse;" summary="">
	<tr class="boldtext" style="border-bottom-style:solid; border-bottom-width:1px; 
			border-bottom-color:black; text-align:center;">
	<% if (isInstructorOrTA) { %>
		<th <%= style %>><%= user.translate("Name") %></th>
		<th <%= style %>><%= user.translate("Student ID number") %></th>
	<% } // if isInstructorOrTA %>
	<th <%= style %>><%= user.translate("Course") %></th>
	<th <%= style %>><%= user.translate("Assignment") %></th>
	<th <%= style %>><%= user.translate("Question") %></th>
	<th <%= style %>><%= user.translate("Grade") %></th>
	<% if (forExport) { %>
		<th <%= style %>><%= user.translate("Tries") %></th>
	<% } // forExport %>
	</tr>
	<% boolean toggle = true;
	final List<String> studentIdsList = allResults.getStudentIds();
	for (final String studentId : studentIdsList) {
		final EnrollmentData student = studentDataByIds.get(studentId); 
		final String studentName = student.getName();
		final String studentNum = student.getStudentNum();
	 	final List<Integer> courseIdsObjs = allResults.getCourseIds(studentId);
		for (final Integer courseIdObj : courseIdsObjs) {
			final Course course = coursesByIds.get(courseIdObj);
			final NumberFormat numberFormat = NumberFormat.getInstance();
			numberFormat.setMaximumFractionDigits(course.getNumDecimals());
			final int courseId = courseIdObj.intValue();
			final String courseName = course.getName();
			final List<int[]> hwIdsAndNums = 
					allResults.getHWIdsAndNums(studentId, courseId);
			for (final int[] hwIdAndNum : hwIdsAndNums) {
				final Assgt assgt = allResults.getHW(hwIdAndNum[0]);
				final String hwName = assgt.getName();
				final List<Integer> qNumObjs = 
						allResults.getQNums(studentId, courseId, hwIdAndNum);
				for (final Integer qNumObj : qNumObjs) {
					final int qNum = qNumObj.intValue();
					final EvalResult evalResult = allResults.getResult(studentId, 
							courseId, hwIdAndNum, qNum);
					final boolean humanReqd =
							(evalResult.status == HUMAN_NEEDED);
					final String truncPts = (humanReqd ? HUMAN_NEEDED_SYMB
							: numberFormat.format(evalResult.modGrade));
					final StringBuilder rowBld = new StringBuilder();
					final StringBuilder gradeOpBld = new StringBuilder();
					final StringBuilder qNumOpBld = new StringBuilder();
					if (forExport) {
						qNumOpBld.append(qNum);
 						Utils.appendTo(gradeOpBld, truncPts, "</td><td>", 
								evalResult.tries);
					} else {
 						Utils.appendTo(rowBld, "class=\"", 
								toggle ? "greenrow" : "whiterow", 
								"\" style=\"padding-top:5px; "
									+ "border-collapse:collapse;\"");
						final StringBuilder paramsBld = Utils.getBuilder(
								'(', courseId, COMMA, hwIdAndNum[0], COMMA, 
								hwIdAndNum[1], COMMA, evalResult.qId, COMMA, 
								'\'', Utils.toValidJS(studentId), "');");
 						Utils.appendTo(qNumOpBld, "<a href=\"javascript:viewQ", 
								paramsBld, "\">", qNum, "</a>");
 						Utils.appendTo(gradeOpBld, 
								"<span style=\"font-weight:bold; color:", 
								humanReqd ? HUMAN_NEEDED_COLOR
									: evalResult.grade == 1.0 ? RIGHT_COLOR
									: evalResult.grade > 0.0 ? PARTIAL_COLOR
									: WRONG_COLOR, 
								";\">", truncPts, CLOSESPAN, ' ', 
								user.translate(evalResult.modGrade != 1 
									? "pts." : "pt."), 
								"<br/>(<a href=\"javascript:showMol", paramsBld, 
								"\" title=\"View most recent response\">", 
								user.translate(evalResult.tries == 1
									? "***1*** attempt"
									: "***2*** attempts", evalResult.tries), 
								"</a>)");
						if (!Utils.isEmpty(evalResult.comment)) {
 							Utils.appendTo(gradeOpBld, ' ', COMMENTED);
						} // if there's a comment
					} // if for export
					toggle = !toggle;
	%>
					<tr <%= rowBld.toString() %>>
	<%	 			if (isInstructorOrTA) { %>
						<td <%= style %>><%= studentName %></td>
						<td <%= style %>><%= studentNum %></td>
	<% 				} // if isInstructorOrTA %>
					<td <%= style %>><i><%= courseName %></i></td>
					<td <%= style %>><i><%= hwName %></i></td>
					<td <%= style %>><%= qNumOpBld.toString() %></td>
					<td <%= style %>><%= gradeOpBld.toString() %></td>
					</tr>
	<%			} // for each question answered 
			} // for each assignment with a response 
		} // for each course with a response 
	} // for each student %>
</table>
<% } // if there were results %>

<% if (!forExport) { %>
	</div>
	<div id="footer">
	<table summary="navigation" style="width:95%; margin-left:auto; margin-right:auto;">
	<tr>
	<td style="width:95%;"></td>
	<td>
		<%= makeButton(user.translate("New search"), "goBack();") %>
	</td>
	<td>
		<%= makeButton(user.translate("Export to spreadsheet"), "exportMe();") %>
	</td>
	<td>
		<%= makeButton(user.translate("Cancel"), "goBackAgain();") %>
	</td>
	</tr>
	</table>
	</div>
<% } // if not for export %>
</body>
</html>
