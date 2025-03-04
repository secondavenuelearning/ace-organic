<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.courseware.Course,
	com.epoch.courseware.EnrollmentData,
	com.epoch.utils.Utils,
	java.util.List"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");

	boolean isInstructorOrTA = true;
	Course[] courses;
	EnrollmentData[] students;
	int numInstns;
	final StudentSession studSess = (StudentSession) userSess;
	switch (role) {
		case User.ADMINISTRATOR: 
			%> <jsp:forward page="/errormsgs/noAccess.html" /> <%
			break;
		case User.INSTRUCTOR:
			final InstructorSession instrSess = (InstructorSession) userSess;
			students = instrSess.getAllEnrolledStudents();
			numInstns = instrSess.getNumInstitutionsOfAllEnrolledStudents();
			courses = instrSess.getCourses();
			break;
		case User.STUDENT:
		default: // shouldn't happen
			if (studSess.isTAForAny()) {
				students = studSess.getAllEnrolledStudents();
				numInstns = studSess.getNumInstitutionsOfAllEnrolledStudents();
			} else {
				isInstructorOrTA = false;
				students = studSess.getMyEnrollmentData();
				numInstns = 1;
			} // if a TA
			courses = studSess.getCourses();
			break;
	} // switch role
	synchronized (session) {
		session.setAttribute("allEnrolledStudents", students);
		session.setAttribute("courses", courses);
	}
	/*
	Utils.alwaysPrint("chooseStudents.jsp: num courses = ", courses.length,
			", num students = ", students.length, ", isInstructorOrTA = ",
			isInstructorOrTA, ", numInstns = ", numInstns);
	/**/

	final String studentIndicesStr = request.getParameter("studentIndices");
	final String courseIdsStr = request.getParameter("courseIds");
	String searchExp = request.getParameter("searchExp");
	if (searchExp == null) searchExp = "";
	final int[] studentIndices = (studentIndicesStr == null ? new int[0]
			: Utils.stringToIntArray(studentIndicesStr.split(":")));
	final int[] courseIds = (courseIdsStr == null ? new int[0] 
			: Utils.stringToIntArray(courseIdsStr.split(":")));

	final String goBack = request.getParameter("goBack");
	final String CHECKED = " checked=\"checked\"";
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
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	
	function toggleAll(courses) {
		var form = document.chooseForm;
		if (courses) setAllCheckBoxes(form.courseSelector, form.toggleCB1.checked);
		else setAllCheckBoxes(form.studentSelector, form.toggleCB2.checked);
	} // toggleAll()

	function submitIt() {
		var form = document.chooseForm;
		if (isWhiteSpace(form.searchExp.value)) {
			toAlert('<%= user.translateJS(
					"Please enter a search term.  (Enter . to match anything.)") %>');
			return;
		}
		var tgt_checkbox1 = form.courseSelector;
		var courses = getSelectedValues(tgt_checkbox1);
		if (courses.length === 0) {
			toAlert('<%= user.translateJS(
					"Please select one or more courses.") %>');
			return;
		}
		<% if (isInstructorOrTA) { %>
			var tgt_checkbox2 = form.studentSelector;
			var students = getSelectedValues(tgt_checkbox2);
			if (students.length === 0) {
				toAlert('<%= user.translateJS(
						"Please select one or more students.") %>');
				return;
			}
			form.studentIndices.value = students.join(':');
		<% } else { %>
			form.studentIndices.value = '1';
		<% } // if a single student %>
		form.courseIds.value = courses.join(':');
		form.submit();
	} // submitIt()

	function goBackAgain() {
		self.location.href = '<%= Utils.toValidJS(goBack) %>';
	} // goBackAgain()

	function sorry() {
		alert('<%= Utils.toValidJS("No students are enrolled in any of your courses.") %>');
		goBackAgain();
	} // sorry()

	// -->

</script>
</head>
<body class="light" style="background-color:white;" <%= 
		students.length == 0 ? "onload=\"sorry();\"" : "" %>>

<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
<div id="contentsWithoutTabsWithFooter">

<form name="chooseForm" action="getReports.jsp" method="post" accept-charset="UTF-8">
	<input type="hidden" name="studentIndices" value="" />
	<input type="hidden" name="courseIds" value="" />
	<input type="hidden" name="goBack" 
			value="<%= Utils.toValidHTMLAttributeValue(goBack) %>"/>
	<input type="hidden" name="isInstructorOrTA" value="<%= isInstructorOrTA %>" />
<table style="width:95%; margin-left:auto; margin-right:auto;" summary="">
	<tr>
	<td colspan="2" class="boldtext big" 
			style="vertical-align:top; padding-top:10px; padding-bottom:10px;">
		<%= user.translate("Crosscourse reports") %>
	</td>
	</tr>
	<tr>
	<td class="regtext" colspan="2" style="text-align:left; padding-bottom:10px;">
		<%= user.translate("Enter a word fragment, word, phrase, "
				+ "simple boolean expression, or regular expression "
				+ "for which to search the question keywords and "
				+ "statements:") %>
	</td>
	</tr>
	<tr>
	<td class="regtext" colspan="2" style="text-align:left; padding-bottom:10px;">
		<input type="text" name="searchExp" size="80" 
				value="<%= Utils.toValidTextbox(searchExp) %>"/>
	</td>
	</tr>
	<tr>
	<td class="regtext" colspan="2" style="text-align:left; padding-bottom:10px;">
		<%= user.translate("If you are not entering a regular expression, "
					+ "and your search term contains any of the characters "
					+ "***metacharacters***, you must precede each instance of the "
					+ "character with a backslash ***\\***.", 
					new String[] { 
						"<span class=\"boldtext\" style=\"font-family:Courier;\">"
							+ Utils.spanString("\\^$.|?*+[()") + "</span>",
						"<span class=\"boldtext\" style=\"font-family:Courier;\">"
							+ Utils.spanString("\\") + "</span>"
					}) %>
	</td>
	</tr>
	<tr>
	<td class="regtext">
		<table style="width:90%;">
		<tr>
		<td>
			<%= user.translate("Select the courses:") %>
		</td>
		<td style="text-align:right; margin-left:auto; margin-right:10px;">
			<%= user.translate("Select all:") %>
			<input type="checkbox" name="toggleCB1" onclick="toggleAll(true);"/> 
		</td>
		</tr>
		</table>
	</td>
	<% if (isInstructorOrTA) { %>
		<td class="regtext">
			<table style="width:90%;">
			<tr>
			<td>
				<%= user.translate("Select the students:") %>
			</td>
			<td style="text-align:right; margin-left:auto; margin-right:10px;">
				<%= user.translate("Select all:") %>
				<input type="checkbox" name="toggleCB2" onclick="toggleAll(false);"/> 
			</td>
			</tr>
			</table>
		</td>
	<% } // if isInstructorOrTA %>
	<tr>
	<td style="background-color:#ffffff;
			vertical-align:top; text-align:left; width:50%;">
		<table class="whiteTable" summary="courses" 
				style="width:100%; border-collapse:collapse;">
		<% int courseNum = 0;
		for (final Course course : courses) {
			if (course.hide()) continue;
			final int courseId = course.getId();
			if (role == User.STUDENT && isInstructorOrTA && !studSess.isTA(courseId)) continue;
			if (courseId == AppConfig.tutorialId) continue;
			final String rowColor = (++courseNum % 2 == 0 ? "greenrow" : "whiterow");
		%>
			<tr class="<%= rowColor %>">
				<td style="text-align:right;"><%= courseNum %>.</td>
				<td style="padding-left:10px;">
					<%= course.getName() %> (<%= courseId %>)
				</td>
				<td style="padding-left:10px;"> 
					<input type="checkbox" name="courseSelector" <%=
							Utils.contains(courseIds, courseId) ? CHECKED : "" %>
							value="<%= courseId %>"/>
				</td>
			</tr>
		<% } // for each course %>
		</table>
	</td>
	<% if (isInstructorOrTA) { %>
		<td style="background-color:#ffffff;
				vertical-align:top; text-align:left; width:50%;">
			<table class="whiteTable" summary="students" 
					style="width:100%; border-collapse:collapse;">
			<% int studNum = 0;
			for (final EnrollmentData student : students) {
				final String rowColor = (++studNum % 2 == 0 ? "greenrow" : "whiterow");
			%>
				<tr class="<%= rowColor %>">
					<td style="text-align:right;"><%= studNum %>.</td>
					<td style="padding-left:10px;"><%= student.getName() %></td>
					<td style="padding-left:10px;"><%= student.getStudentNum() %></td>
					<% if (numInstns > 1) { %>
						<td style="padding-left:10px;"><%= student.getInstitutionName() %></td>
					<% } // if should show institutions %>
					<td style="padding-left:10px;"> 
						<input type="checkbox" name="studentSelector" <%=
								Utils.contains(studentIndices, studNum) ? CHECKED : "" %>
								value="<%= studNum %>"/>
					</td>
				</tr>
			<% } // for each student %>
			</table>
		</td>
	<% } // if isInstructorOrTA %>
	</tr>
</table>

</form>
</div>
<div id="footer">
<table summary="navigation" style="width:95%; margin-left:auto; margin-right:auto;">
<tr>
<td style="width:95%;"></td>
<td>
	<%= makeButton(user.translate("Get results"), "submitIt();") %>
</td>
<td>
	<%= makeButton(user.translate("Cancel"), "goBackAgain();") %>
</td>
</tr>
</table>
</div>
</body>
</html>
