<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.EnrollmentData,
	com.epoch.courseware.Institution,
	com.epoch.session.AnonSession,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.List"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%	
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	final Institution[] institutions = AnonSession.getVerifiedInstitutions();
	int selectedInstitn = MathUtils.parseInt(request.getParameter("institnSelector"));
	if (selectedInstitn == 0) selectedInstitn = user.getInstitution().getId(); 
	String studentNumLabel = user.getInstitutionStudentNumLabel();
	for (final Institution institution : institutions) {
		if (institution.getId() == selectedInstitn) {
			studentNumLabel = institution.getStudentNumLabel();
			break;
		} // if this institution is the selected one
	} // for each institution

	final InstructorSession instrSess = (InstructorSession) userSess;
	final int numYearsBack = MathUtils.parseInt(request.getParameter("yearsBack"));
	final EnrollmentData[] regdStudents = 
			instrSess.getAllRegdStudents(numYearsBack, selectedInstitn);
	final EnrollmentData[] enrolledStudents = instrSess.getEnrolledStudents();
	final List<String> enrolledStudentNums = new ArrayList<String>();
	for (final EnrollmentData enrolledStudent : enrolledStudents) {
		String oneStudentNum = enrolledStudent.getStudentNum(); 
		if (oneStudentNum == null) oneStudentNum = ""; 
		enrolledStudentNums.add(oneStudentNum);
	} // for each enrolled student

	synchronized (session) {
		session.setAttribute("regdStudents", regdStudents);
		session.setAttribute("enrolledStudentNums", enrolledStudentNums);
	}
	/* Utils.alwaysPrint("listRegistered.jsp: ", enrolledStudentNums.size(),
			" enrolled students."); /**/
	final int[] sizes = {10, 50, 100, 100, 100, 50, 50};
	final String SELECTED = " selected=\"selected\"";
	final int colSpan = sizes.length;
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
	<title>ACE Registered Students</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<style type="text/css">
		* html body {
			padding:100px 0 50px 0; 
		}
	</style>

	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-->
		<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
		<%@ include file="/navigation/courseSidebarJS.jsp.h" %>

		function goBackAgain() {
			self.location.href = 'listEnrollment.jsp';
		}

		function relist() {
			document.restrictRegDateForm.submit();
		}

		function enrollNew() {
			var tgt_checkbox = document.enrollmentlist.studentSelector;
			var indices = getSelectedValues(tgt_checkbox);
			if (indices.length === 0) {
				toAlert('<%= user.translateJS(
						"You must select at least one student to enroll.") %>'); 
			} else {
				self.location.href = 'enrollRegistered.jsp?chosenStudents=' 
						+ indices.join(':');
			}
		}

		function toggleAll(form) {
			setAllCheckBoxes(form.studentSelector, form.toggleCB.checked);
		}

		// -->
	</script>
</head>

<body style="text-align:center;"
		onload="setTab('<%= toTabName(user.translateJS("Enrollment")) %>');">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

	<div id="contentsWithTabsWithFooter">
	<table class="regtext" style="width:95%; margin-left:auto; margin-right:auto; 
			border-style:none; border-collapse:collapse;">
		<tr><td class="boldtext big" style="padding-bottom:10px; padding-top:10px;" 
				colspan="<%= colSpan %>">
			<form name="restrictRegDateForm" action="listRegistered.jsp" method="post">
			<% final String yrsBackSelector =
						"<select name=\"yearsBack\" onchange=\"relist();\">"
						+ "<option value=\"0\""
							+ (numYearsBack == 0 ? SELECTED : "") + ">"
							+ user.translate("any number of") + "</option>"
						+ "<option value=\"5\""
							+ (numYearsBack == 5 ? SELECTED : "") + ">5</option>"
						+ "<option value=\"4\""
							+ (numYearsBack == 4 ? SELECTED : "") + ">4</option>"
						+ "<option value=\"3\""
							+ (numYearsBack == 3 ? SELECTED : "") + ">3</option>"
						+ "<option value=\"2\""
							+ (numYearsBack == 2 ? SELECTED : "") + ">2</option>"
						+ "<option value=\"1\""
							+ (numYearsBack == 1 ? SELECTED : "") + ">1</option>"
						+ "</select>";
			final StringBuilder instnSelector = new StringBuilder();
			instnSelector.append("<select name=\"institnSelector\" onchange=\"relist();\">");
			for (final Institution institution : institutions) { 
				final String instnName = institution.getName(); 
				instnSelector.append("<option value=\"").append(institution.getId())
						.append('"')
						.append(institution.getId() == selectedInstitn ? SELECTED : "")
						.append('>').append(instnName).append("</option>");
			} // for each institution
			instnSelector.append("</select>");
			%>
			<%= user.translate(Utils.toString(
						"Students from ***institution*** registered with ACE within the past ",
						numYearsBack == 1 ? "***1*** year" : "***2*** years"), 
					new String[] {instnSelector.toString(), yrsBackSelector}) %>
			</form>
		</td></tr>
		<tr><td class="regtext" style="padding-bottom:10px;" colspan="<%= colSpan %>">
			<%= user.translate("Select the students whom you want to enroll "
					+ "in your course, and press <b>Enroll selected</b> "
					+ "at the bottom of the page to enroll them.  Grayed-out "
					+ "students are already enrolled in your course.") %>
		</td></tr>
	</table>
	<form name="enrollmentlist" action="dummy">
	<table class="regtext" style="width:95%; margin-left:auto; margin-right:auto; 
			border-style:none; border-collapse:collapse;">
		<% if (regdStudents.length > 0) { %>
			<tr>
			<td class="boldtext enlarged" 
					style="width:<%= sizes[0] %>px; border-bottom-style:solid; 
					border-width:1px; border-color:#49521B; padding-left:10px;
					padding-right:20px;">
				<%= user.translate("No.") %>
			</td><td class="boldtext enlarged" 
					style="width:<%= sizes[1] %>px; border-bottom-style:solid; 
					border-width:1px; border-color:#49521B;">
				<%= studentNumLabel %>
			</td><td class="boldtext enlarged" 
					style="width:<%= sizes[2] %>px; border-bottom-style:solid; 
					border-width:1px; border-color:#49521B;">
				<%= user.translate("Name") %>
			</td><td class="boldtext enlarged" 
					style="width:<%= sizes[3] %>px; border-bottom-style:solid; 
					border-width:1px; border-color:#49521B;">
				<%= user.translate("Login name") %>
			</td><td class="boldtext enlarged" 
					style="width:<%= sizes[4] %>px; border-bottom-style:solid; 
					border-width:1px; border-color:#49521B;">
				<%= user.translate("Email") %>
			</td><td class="boldtext enlarged" 
					style="width:<%= sizes[5] %>px; border-bottom-style:solid; 
					border-width:1px; border-color:#49521B;">
				<%= user.translate("Date registered") %>
			</td><td class="boldtext enlarged" 
					style="text-align:center; width:<%= sizes[6] %>px; 
					border-bottom-style:solid; border-width:1px; 
					border-color:#49521B;">
				<input type="checkbox" name="toggleCB" 
						onclick="javascript:toggleAll(document.enrollmentlist);"/> 
						<%= user.translate("Select All") %>
			</td></tr>
			<% for (int regStudNum = 0; regStudNum < regdStudents.length; regStudNum++) {
				final EnrollmentData regdStudent = regdStudents[regStudNum];
				final String regdStudentNum = regdStudent.getStudentNum();
				String rowColor = (regStudNum % 2 == 0 ? "whiterow" : "greenrow");
				if (enrolledStudentNums.contains(regdStudentNum)) 
					rowColor += "\" style=\"background-color:gray;";
			%>
				<tr class="<%= rowColor %>">
				<td style="width:<%= sizes[0] %>px; border-left-style:solid; 
						border-width:1px; border-color:#49521B; 
						padding-right:20px; padding-left:10px;"><%= regStudNum + 1 %>.</td>
				<td style="width:<%= sizes[1] %>px;"><%= regdStudentNum %></td>
				<td style="width:<%= sizes[2] %>px;"><%= regdStudent.getName() %></td>
				<td style="width:<%= sizes[3] %>px;"><%= regdStudent.getUserId() %></td>
				<td style="width:<%= sizes[4] %>px;"><%= regdStudent.getEmail() %></td>
				<td style="width:<%= sizes[5] %>px;"><%= 
						regdStudent.getRegDateStr(course.getTimeZone()) %></td>
				<td style="width:<%= sizes[6] %>px; border-right-style:solid; 
						border-width:2px; border-color:#49521B; text-align:center;">
				<% if (!enrolledStudentNums.contains(regdStudentNum)) { %>
					<input type="checkbox" name="studentSelector" 
							value="<%= regStudNum + 1 %>"/>
				<% } // if student is enrolled in course %>
				</td>
				</tr>
			<% } // for each registered student %>
			<tr><td colspan="<%= colSpan %>" style="border-top-style:solid; border-width:1px; 
					border-color:#49521B; width:100%;">
			</td></tr>
		<% } else { %>
			<tr><td colspan="<%= colSpan %>" style="border-style:solid; border-width:1px; 
					border-color:#49521B; background-color:#f6f7ed; padding:10px;">
				<%= user.translate("There are currently no registered students.") %>
			</td></tr>
		<% } // if there are registered students %>
	</table>
	</div>
	<div id="footer">
	<table class="regtext" style="width:95%; margin-left:auto; margin-right:auto; 
			border-style:none; border-collapse:collapse;">
		<tr><td style="width:100%; text-align:right; padding-top:10px;"></td>
			<% if (regdStudents.length > 0) { %>
				<td style="text-align:right; padding-top:10px; padding-left:10px;">
					<%= makeButton(user.translate("Enroll Selected"), 
							"enrollNew();") %>
				</td>
			<% } %>
		<td style="text-align:right; padding-top:10px; padding-left:10px;">
			<%= makeButton(user.translate("Back To Enrollment List"),
					"goBackAgain();") %>
		</td></tr>
	</table>
	</div>
	</form>
</body>
</html>
