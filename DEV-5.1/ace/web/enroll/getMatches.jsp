<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.EnrollmentData,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.List"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%	
	final String pathToRoot = "../";

	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	request.setCharacterEncoding("UTF-8");
	final InstructorSession instrSess = (InstructorSession) userSess;
	final int regCourseId = MathUtils.parseInt(request.getParameter("regCourseId"));
	final EnrollmentData[] examStudents = instrSess.getRegisteredUsers();
	final EnrollmentData[] regCourseStudents = 
			instrSess.getRegisteredUsers(regCourseId);
	final List<String[]> matchedStudents = new ArrayList<String[]>();
	final List<String> regCourseStudentStrs = new ArrayList<String>();
	final List<EnrollmentData> unmatchedExamStudents = 
			new ArrayList<EnrollmentData>();
	final List<EnrollmentData> regCourseStudentsList = 
			new ArrayList<EnrollmentData>();
	final String matchMethodStr = request.getParameter("matchMethod");
	final int BOTH = 0;
	final int NAME = 1;
	final int STUDENT_NUM = 2;
	final int matchMethod = ("studentNum".equals(matchMethodStr) 
			? STUDENT_NUM : "name".equals(matchMethodStr)
			? NAME : BOTH);
	for (final EnrollmentData regCourseStudent : regCourseStudents) {
		regCourseStudentsList.add(regCourseStudent);
		final String regCourseStudentStr = 
				(matchMethod != STUDENT_NUM ? regCourseStudent.getName() : "")
				+ (matchMethod == BOTH ? ":::" : "")
				+ (matchMethod != NAME ? regCourseStudent.getStudentNum() : ""); 
		regCourseStudentStrs.add(regCourseStudentStr);
	} // for each student in regular course
	/*
	Utils.alwaysPrint("getMatches.jsp: looking for matches for ", examStudents.length, 
			" student(s) among ", regCourseStudents.length, " student(s).");
	/**/
	// find all exam students among regular course students
	for (final EnrollmentData examStudent : examStudents) {
		final String examStudentStr = 
				(matchMethod != STUDENT_NUM ? examStudent.getName() : "")
				+ (matchMethod == BOTH ? ":::" : "")
				+ (matchMethod != NAME ? examStudent.getStudentNum() : ""); 
		final int locn = regCourseStudentStrs.indexOf(examStudentStr);
		if (locn >= 0) {
			regCourseStudentStrs.remove(locn);
			final EnrollmentData regCourseStudent = 
					regCourseStudentsList.remove(locn);
			final String[] match = new String[] {
					examStudent.getName(),
					examStudent.getStudentNum(),
					examStudent.getUserId(),
					regCourseStudent.getUserId()
					};
			if (!match[2].equals(match[3])) {
				/* Utils.alwaysPrint("getMatches.jsp: found match for ", examStudentStr);
				/**/
				matchedStudents.add(match);
			/* } else {
				Utils.alwaysPrint("getMatches.jsp: ", examStudent.getName(),
						"'s work already transferred from login ID ",
						examStudent.getUserId(), " to regular course login ID ",
						regCourseStudent.getUserId());
			/**/
			} // if the matched students are not the same
		} else {
			/* 
			Utils.alwaysPrint("getMatches.jsp: '", examStudentStr,
					"' not found among ", regCourseStudentStrs);
			/**/
			unmatchedExamStudents.add(examStudent);
		} // if examStudent found among regCourseStudents
	} // for each exam student
	synchronized (session) {
		session.setAttribute("matchedStudents", matchedStudents);
		session.setAttribute("unmatchedExamStudents", unmatchedExamStudents);
		session.setAttribute("unmatchedRegCourseStudents", regCourseStudentsList);
	}
	final boolean haveMatches = !matchedStudents.isEmpty();
	final String studentNumLabel = user.getInstitutionStudentNumLabel();

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<title>ACE Transfer Student Work</title>
	<meta http-equiv="Pragma" content="no-cache">
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT">
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<style type="text/css">
		* html body {
			padding:50px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/navigation/courseSidebarJS.jsp.h" %>

	function submitIt() {
		self.location.href = 'chooseMatches.jsp?regCourseId=<%= regCourseId %>';
	}

	// -->
	</script>
</head>

<body class="light" style="background-color:white; overflow:auto;" 
		onload="setTab('<%= toTabName(user.translateJS("Enrollment")) %>');">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

	<div id="contentsWithTabsWithoutFooter">
	<table style="width:626px; text-align:center; margin-left:auto;
	margin-right:auto;">
		<tr><td class="boldtext big" style="vertical-align:top;
		padding-top:10px;">
			<%= user.translate("Student Matches by ACE") %>
		</td></tr>
		<% if (haveMatches) { %>
			<tr><td class="regtext" style="vertical-align:top; 
					padding-top:10px;">
				<% if (matchMethod == NAME) { %>
					<%= user.translate("ACE found the following students had the "
							+ "same name but different login IDs "
							+ "in the two courses.") %> 
				<% } else { %>
					<%= user.translate("ACE found the following students had the "
							+ "same " + (matchMethod == BOTH ? "name and " : "")
							+ "***student ID number*** but different login IDs "
							+ "in the two courses.", studentNumLabel) %> 
				<% } // if matchMethod %>
			</td></tr>
			<tr><td style="vertical-align:top; text-align:center; padding-top:10px;">
				<table class="whiteTable" style="background-color:#f6f7ed;
						text-align:left;">
					<tr>
					<th><%= user.translate("Name") %></th>
					<th style="padding-left:10px;"><%= studentNumLabel %></th>
					<th style="padding-left:10px;">
						<%= user.translate("Exam course login ID") %>
					</th>
					<th style="padding-left:10px;">
						<%= user.translate("Regular course login ID") %>
					</th>
					</tr>	
					<% for (final String[] match : matchedStudents) { %>
						<tr>
						<td style="padding-right:10px;"><%= match[0] %></td>
						<td><%= match[1] %></td>
						<td style="padding-left:20px;"><%= match[2] %></td>
						<td style="padding-left:20px;"><%= match[3] %></td>
						</tr>
					<% } // for each student %>
				</table>
			</td></tr>	
			<tr><td class="regtext" style="vertical-align:top; 
					padding-top:10px;">
				<%= user.translate("Transfer these students' work from their exam "
						+ "course login IDs to their regular course login IDs? "
						+ "(You will have the opportunity to handle unmatched "
						+ "students in a moment.)") %>
			</td></tr>
		<% } else { // no matched students %>	
			<tr><td class="regtext" style="vertical-align:top; 
					padding-top:10px;">
				<% if (matchMethod == NAME) { %>
					<%= user.translate("ACE found no students with the "
							+ "same name but different login IDs in the two "
							+ "courses, but you will have the opportunity "
							+ "to match students yourself on the next page.") %>
				<% } else { %>
					<%= user.translate("ACE found no students with the "
							+ "same " + (matchMethod == BOTH ? "name and " : "")
							+ "***student ID number*** but different login IDs "
							+ "in the two courses, but you will have the "
							+ "opportunity to match students yourself on the "
							+ "next page.", studentNumLabel) %>
				<% } // if matchMethod %>
			</td></tr>
		<% } // if there are matched students %>
		<tr><td><table><tr>
			<td style="padding-bottom:10px; padding-top:10px; padding-left:30px;">
				<%= makeButton(user.translate(haveMatches 
							? "Transfer work" : "Continue"), 
						"submitIt();") %>
			</td>
			<td style="padding-bottom:10px; padding-top:10px;">
				<%= makeButton(user.translate("Cancel"), 
						"self.location.href='listEnrollment.jsp';") %>
			</td>
		</tr></td></table></tr>
	</table>
	</div>
</body>
</html>
