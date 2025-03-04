<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.EnrollmentData"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%	
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	/*
	  List of students enrolled in this course
	*/
	// only name, student-id and email-id are used
	EnrollmentData[] students = null;
	boolean multiinstitution = false;
	/* The session can be of an
			- administrator impersonating an instructor or TA
			- instructor
			- TA
	*/
	InstructorSession instrSess = null;
	final boolean refresh = request.getParameter("refresh") != null;
	switch (role) {
		case User.ADMINISTRATOR: 
			%> <jsp:forward page="/errormsgs/noAccess.html" /> <%
			break;
		case User.INSTRUCTOR:
			instrSess = (InstructorSession) userSess;
			if (refresh) {
				instrSess.refreshEnrolledStudents();
			}
			multiinstitution = instrSess.courseIsMultiinstitution()
					|| instrSess.studentsAreMultiinstitution();
			students = instrSess.getEnrolledStudents();
			break;
		case User.STUDENT:
		default: // shouldn't happen
			if (!isTA) {
				%> <jsp:forward page="/errormsgs/noSession.html" /> <%
			} // if not a TA
			final StudentSession studSess = (StudentSession) userSess;
			final int courseId = studSess.getSelectedCourseId();
			instrSess = new InstructorSession(courseId, user);
			if (refresh) {
				instrSess.refreshEnrolledStudents();
			}
			students = instrSess.getEnrolledStudents();
			multiinstitution = instrSess.courseIsMultiinstitution()
					|| instrSess.studentsAreMultiinstitution();
			break;
	} // switch role

	String studentNumLabel = user.getInstitutionStudentNumLabel();
	if (multiinstitution) studentNumLabel = 
			Utils.toString(studentNumLabel, ' ', user.translate("(or other)"));
	final int numExamIds = instrSess.getNumExamIds();
	final int numUnusedExamIds = (numExamIds == 0 ? 0
			: instrSess.getNumExamIds(instrSess.UNUSED_ONLY));
	
	int numCols = 7;
	if (multiinstitution) numCols++;
	int[] sizes = null;
	switch (numCols) {
		case 4: sizes = new int[] {15, 210,	  0,   0, 210,  0,  0, 60}; break;
		case 5: sizes = new int[] {15, 210, 120,   0, 210,  0,  0, 60}; break;
		case 6: sizes = new int[] {15, 160,	  0, 140, 210,  0, 60, 60}; break;
		case 7: sizes = new int[] {15, 150, 110, 110, 150,  0, 60, 60}; break;
		case 8: sizes = new int[] {15, 150, 110, 110, 150, 60, 60, 60}; break;
	}
	if (isTA) numCols--;
	final String rightCellStyle = (isTA ? "style=\"border-right-style:solid; "
			+ "border-width:1px; border-color:#49521B; text-align:center;\"" 
			: "style=\"text-align:center;\"");

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
<title>ACE Course Enrollment</title>
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<style type="text/css">
	* html body {
		padding:100px 0 50px 0; 
	}
</style>

<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/navigation/courseSidebarJS.jsp.h" %>

	function enrollTask() {
		var method = getValue('enrollTaskSel');
		if (method === 'disenroll') {
			disenroll();
		} else if (method === 'block') {
			forumAccess();
		} else if (method === 'modifyTAStatus') {
			modifyTAStatus();
		} else if (method === 'name') {
			self.location.href = 'editEnrollment.jsp';
		} else if (method === 'spreadsheet') {
			self.location.href = 'batchEnrollment.jsp';
		} else if (method === 'create') {
			self.location.href = 'inventStudents.jsp';
		} else if (method === 'transfer') {
			self.location.href = 'transferWork.jsp';
		} else if (method === 'delete') {
			self.location.href = 'deleteExamIds.jsp';
		} else if (method === 'list') {
			self.location.href = 'listRegistered.jsp?yearsBack=1';
		}
	} // enrollTask()

	function disenroll() {
		var noneSelected = '<%= user.translateJS(
				"You must select at least one student "
				+ "to disenroll.") %>';
		var destination = 'deleteEnrollment.jsp';
		toggleSelected(noneSelected, destination);
	} // disenroll()

	function modifyTAStatus() {
		var noneSelected = '<%= user.translateJS(
					"You must select at least one student"
					+ " whose TA status should be altered.") %>';
		var destination = 'modifyTAStatus.jsp';
		toggleSelected(noneSelected, destination);
	} // modifyTAStatus()

	function forumAccess() {
		var noneSelected = '<%= user.translateJS(
					"You must select at least one student"
					+ " whose forum access should be changed.") %>';
		var destination = 'forumAccess.jsp';
		toggleSelected(noneSelected, destination);
	} // forumAccess()

	function toggleSelected(noneSelected, destination) {
		var tgt_checkbox = document.enrollmentlist.studentSelector;
		var indices = getSelectedValues(tgt_checkbox);
		if (indices.length === 0) {
			toAlert(noneSelected);
			setValue('enrollTaskSel', '');
		} else if (destination !== 'deleteEnrollment.jsp') {
			self.location.href = destination + '?indices=' 
					+ indices.join(':');
		} else {
			var bld = new String.builder();
			<% for (int studNum = 0; studNum < students.length; studNum++) {
				final String name = students[studNum].getName();
			%>
				if (indices.indexOf('<%= studNum + 1 %>') >= 0) {
					bld.append('<%= Utils.toValidJS(name) %>\n');
				} // if student was chosen
			<% } %> // for each student
			bld.append('\n');
			if (toConfirm(bld.toString() + '<%= user.translateJS(
					"Disenrolling a student will delete all of "
					+ "the student's grades in this course. "
					+ "Do you wish to continue?") %>')) {
				self.location.href = destination + '?indices=' 
						+ indices.join(':');
			} else {
				setValue('enrollTaskSel', '');
			} // if confirm disenrollment
		} // if task
	} // toggleSelected()

	function exportMe() {
		self.location.href = 'listEnrollmentTxt.jsp';
	} // exportMe()

	function toggleAll(form) {
		setAllCheckBoxes(form.studentSelector, form.toggleCB.checked);
	} // toggleAll()

	// -->
</script>
</head>

<body style="text-align:center; margin:0px;"
		onload="setTab('<%= toTabName(user.translateJS("Enrollment")) %>');">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

	<form name="enrollmentlist" action="dummy" method="post">
	<div id="contentsWithTabsWithFooter">
		<table id="tabley" class="regtext" style="width:90%; margin-left:auto; 
				margin-right:auto; border-style:none; border-collapse:collapse;"
				summary="">
			<tr>
			<td class="boldtext big" style="padding-bottom:10px;" 
					colspan="<%= numCols %>">
				<%= user.translate("Enrollment") %>
			</td>
			</tr>
		<% if (students != null && students.length > 0) { %>
			<tr>
			<td class="boldtext enlarged" style="width:<%= sizes[0] %>px; 
					text-align:right;
					border-bottom-style:solid; border-width:1px; 
					border-color:#49521B; padding-left:10px; 
					padding-right:10px;">
				<%= user.translate("No.") %>
			</td>
			<td id="tdy" class="boldtext enlarged" style="width:<%= sizes[1] %>px; 
					border-bottom-style:solid; border-width:1px; 
					border-color:#49521B;">
				<%= user.translate("Name") %>
			</td>
			<td class="boldtext enlarged" style="width:<%= sizes[2] %>px; 
					border-bottom-style:solid; border-width:1px; 
					border-color:#49521B;">
				<%= studentNumLabel %>
			</td>
			<td id="tdy2" class="boldtext enlarged" style="width:<%= sizes[3] %>px; 
					border-bottom-style:solid; border-width:1px; 
					border-color:#49521B;">
				<%= user.translate("Login name") %>
			</td>
			<td class="boldtext enlarged" style="width:<%= sizes[4] %>px; 
					border-bottom-style:solid; border-width:1px; 
					border-color:#49521B;">
				<%= user.translate("Email") %>
			</td>
		<% if (multiinstitution) { %>
			<td class="boldtext enlarged" style="width:<%= sizes[5] %>px; 
					border-bottom-style:solid; border-width:1px; 
					border-color:#49521B;">
				<%= user.translate("Institution") %>
			</td>
		<% } // if multiinstitution %>
			<td class="boldtext enlarged" style="text-align:center; 
					width:<%= sizes[6] %>px; border-bottom-style:solid; 
					border-width:1px; border-color:#49521B;">
				<%= user.translate("Registered") %>
			</td>
		<% if (!isTA) { %>
			<td class="boldtext enlarged" style="text-align:center; 
					width:<%= sizes[7] %>px; border-bottom-style:solid; 
					border-width:1px; border-color:#49521B;">
				<input type="checkbox" name="toggleCB" 
						onclick="javascript:toggleAll(document.enrollmentlist);"/> 
						<%= user.translate("Select All") %>
			</td>
		<% } // if not TA %>
			</tr>

		<% for (int studNum = 0; studNum < students.length; studNum++) {
			final EnrollmentData student = students[studNum];
			final String rowColor = (studNum % 2 == 0 ? "greenrow" : "whiterow");
		%>
			<tr class="<%= rowColor %>">
				<td style="border-left-style:solid; border-width:1px; 
						border-color:#49521B; padding-left:10px; text-align:right; 
						padding-right:10px;"><%= studNum + 1 %>.</td>
				<td><%= student.getName() + (!student.isTA() ? "" 
							: " (<span style=\"color:green;\"><b>TA</b></span>)") %> 
				</td>
				<td><%= student.getStudentNum() %></td>
				<% if (student.isRegistered()) { %>
					<td><%= student.getUserId() %></td>
					<td><a href="mailto:<%= student.getEmail() 
							%>"><%= student.getEmail() %></a></td>
					<% if (multiinstitution) { %>
						<td><%= student.getInstitutionName() %></td>
					<% } // if multiinstitution %>
					<td <%= rightCellStyle %>><%= user.translate("Yes") %></td>
				<% } else { %>
					<td>-----</td>
					<td>-----</td>
					<% if (multiinstitution) { %>
						<td><%= student.getInstitutionName() %></td>
					<% } // if multiinstitution %>
					<td <%= rightCellStyle %>><span style="color:red;">
						<%= user.translate("No") %></span></td>
				<% } // if isRegistered
				if (!isTA) { %>
					<td style="border-right-style:solid; border-width:1px; 
							border-color:#49521B; text-align:center;">
						<input type="checkbox" name="studentSelector" 
								value="<%= studNum + 1 %>"/>
					</td>
				<% } // if not TA %>
			</tr>
		<% } // for each student%>
		  	<tr>
				<td colspan="<%= numCols %>" style="border-top-style:solid; 
						border-width:1px; border-color:#49521B; width:100%;"></td>
			</tr>
		<% } else { // no students enrolled %>
		  	<tr>
				<td colspan="<%= numCols %>" style="border-style:solid; 
						border-width:1px; border-color:#49521b; 
						padding:10px; background-color:#f6f7ed;">
					<%= user.translate(
							"There are currently no students enrolled in this course.") %>
				</td>
			</tr>
		<% } // if there are students enrolled %>
		</table>
	</div>
	<div id="footer">
	<table class="regtext" style="width:90%; margin-top:10px; margin-left:auto; 
			margin-right:auto; border-style:none; border-collapse:collapse;"
			summary="">
		<tr>
		<td style="width:100%; text-align:right;"></td>
	<% if (!isTA) { %>
		<td style="text-align:right; padding-left:10px;">
			<select name="enrollTaskSel" id="enrollTaskSel" onchange="enrollTask()">
				<option value=""><%= user.translate("Select an enrollment task here") %></option> 
				<option value="list"><%= user.translate("Enroll from list") %></option> 
				<option value="spreadsheet"><%= user.translate("Enroll via spreadsheet") %></option> 
				<option value="name"><%= user.translate("Enroll by name") %></option> 
				<option value="create"><%= user.translate("Create exam IDs") %></option> 
				<option value="block"><%= user.translate("Block/unblock from forum") %></option> 
		<% if (numExamIds - numUnusedExamIds > 0) { %>
				<option value="transfer"><%= user.translate("Transfer exam work") %></option> 
		<% } // if there are used exam IDs
		if (numUnusedExamIds > 0) { %>
				<option value="delete"><%= user.translate("Delete unused exam IDs") %></option> 
		<% } // if there are unused exam IDs 
		if (students.length > 0) { %>
				<option value="disenroll"><%= user.translate("Disenroll selected") %></option> 
				<option value="modifyTAStatus"><%= user.translate("Alter TA Status") %></option> 
		<% } // if there are students %>
			</select>
		</td>
	<% } // if not TA %>
		<td style="text-align:right; padding-left:10px;">
			<%= makeButton(user.translate("Export"), "exportMe();") %>
		</td>
		</tr>
	</table>
	</div>

	</form>
</body>
</html>
