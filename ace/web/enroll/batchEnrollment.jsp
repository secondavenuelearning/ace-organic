<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.EnrollmentData,
	java.util.ArrayList,
	java.util.HashMap,
	java.util.List,
	java.util.Map"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%	
	final String pathToRoot = "../";
	final int[] sizes = {160, 300, 160, 160, 100, 50};

	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	request.setCharacterEncoding("UTF-8");
	final InstructorSession instrSess = (InstructorSession) userSess;

	EnrollmentData[] students = null;
	final String uploadedFile = request.getParameter("uploadedfile");
	if (uploadedFile != null) {
		students = EnrollmentData.tablFileToList(AppConfig.appRoot + uploadedFile);
		final List<String> enrolledStudNums = 
				instrSess.getEnrolledStudentNums(user.getInstitutionId());
		final List<EnrollmentData> toEnrollList = new ArrayList<EnrollmentData>();
		for (final EnrollmentData student : students) {
			if (!enrolledStudNums.contains(student.getStudentNum())) {
				toEnrollList.add(student);
			} // if the student is already enrolled
		} // for each new student
		final EnrollmentData[] studentsToEnroll = 
				(EnrollmentData[]) toEnrollList.toArray(
					new EnrollmentData[toEnrollList.size()]);
		synchronized (session) {
			session.setAttribute("studentsToEnroll", studentsToEnroll);
		}
	} else {
		students = new EnrollmentData[0];  
	} // if uploaded file is null
	final String studentNumLabel = user.getInstitutionStudentNumLabel();

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
	<title>ACE Student Enrollment</title>
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

	function upload(form) {
		if (isWhiteSpace(form.excelfile.value)) {
			toAlert('<%= user.translateJS("You must provide "
					+ "the location of the spreadsheet on your computer.") %>');
			return;
		} else {
			form.submit();
		}
	}
	function goBackAgain() {
		self.location.href = 'listEnrollment.jsp';
	}

	function enrollBatch() {
		self.location.href = 'saveBatchEnrollment.jsp';
	}
	// -->
	</script>
</head>

<body class="light" style="background-color:white;"
		onload="setTab('<%= toTabName(user.translateJS("Enrollment")) %>');">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>
	
	<div id="contentsWithTabsWithFooter">
	<form name="fileupload" action="fileUpload.jsp" method="post" enctype="multipart/form-data">
	<table style="width:626px; text-align:left; 
			margin-left:auto; margin-right:auto;">
		<tr>
		<td class="boldtext big" style="vertical-align:top; padding-top:10px;">
			<%= user.translate("Enroll Students Using Spreadsheet") %>
		</td>
		</tr>
		<tr>
		<td style="vertical-align:top; text-align:center;">
			<table class="whiteTable" style="width:626px; text-align:left; 
					background-color:#f6f7ed;">
				<tr><td><table><tr>
				<td class="regtext" style="padding-top:10px; padding-left:30px; 
						padding-right:30px;">
					<%= user.translate(
							"Upload a tab-delimited text file in which the first "
							+ "two columns correspond to ***ID number*** and name. "
							+ "The file must not have any blank lines.",
							studentNumLabel) %>
					<%= user.translate("All students in the list must have chosen "
							+ "***your institution*** as their institution "
							+ "when they registered with ACE.", user.getInstitutionName()) %>
				</td>
				</tr></table></td></tr>
				<tr><td><table><tr>
				<td class="regtext" style="padding-top:10px; padding-left:30px;">
					<%= user.translate("Spreadsheet location") %>:
				</td>
				<td style=" text-align:left; padding-top:10px;">
					<input type="file" name="excelfile" size="30"/>
				</td>
				</tr></table></td></tr>
				<tr><td><table><tr>
				<td style="padding-bottom:10px; padding-top:10px; padding-left:30px;">
					<%= makeButton(user.translate("Upload"), 
							"upload(document.fileupload);") %>
				</td>
				<td style="padding-bottom:10px; padding-top:10px;">
					<%= makeButton(user.translate("Cancel"), "goBackAgain();") %>
				</td>
				</tr></table></td></tr>
			</table>
		</td>
		</tr>
	</table>
	</form>
	<% if (students.length != 0) { %>
		<table class="regtext" style="width:626px; margin-left:auto; margin-right:auto; 
		  		border-style:none; border-collapse:collapse;">
			<tr>
				<td class="boldtext big" style="padding-top:10px; padding-bottom:10px;"
						colspan="6">
					<%= user.translate("Spreadsheet contents") %>
				</td>
			</tr>
			<tr>
				<td class="boldtext enlarged" style="width:<%= sizes[0] %>; border-bottom-style:solid; 
						border-width:1px; border-color:#49521B; padding-left:10px;">
					<%= studentNumLabel %>
				</td>
				<td class="boldtext enlarged" style="width:<%= sizes[1] %>; border-bottom-style:solid; 
						border-width:1px; border-color:#49521B;">
					<%= user.translate("Name") %>
				</td>
			</tr>
			<% boolean parity = true;
			for (final EnrollmentData student : students) {
				final String rowColor = (parity ? "whiterow" : "greenrow");
				parity = !parity;
			%>
			<tr class="<%= rowColor %>">
				<td style="width:300; border-left-style:solid; border-width:1px; 
						border-color:#49521B; padding-left:10px;"><%= 
								student.getStudentNum() %>
				</td>
				<td style="border-right-style:solid; border-width:2px; 
						border-color:#49521B;"><%= student.getName() %>
				</td>
			</tr>
			<% } // for each student to be enrolled %>
			<tr>
				<td colspan="6" style="border-top-style:solid; border-width:1px; 
						border-color:#49521B; width:100%;"></td>
			</tr>
		</table>
		</div>
		<div id="footer">
		<table class="regtext" style="width:626px; margin-left:auto; margin-right:auto; 
				border-style:none; border-collapse:collapse;">
			<tr>
				<td style="width:100%; text-align:right; padding-top:10px;"></td>
				<td style="text-align:right; padding-top:10px;">
					<%= makeButton(user.translate("Enroll students"), 
							"enrollBatch();") %>
				</td>
				<td style="padding-top:10px;">
					<%= makeButton(user.translate("Cancel"), "goBackAgain();") %>
				</td>
			</tr>
		</table>
	<% } // if there are students uploaded %>
	</div>
</body>
</html>
