<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.EnrollmentData,
	com.epoch.db.EnrollmentRW,
	com.epoch.db.CourseRW,
	java.util.ArrayList"
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

	ArrayList<String[]> matchedStudents;
	ArrayList<EnrollmentData> unmatchedExamStudents;
	ArrayList<EnrollmentData> unmatchedRegCourseStudents;
	synchronized (session) {
		matchedStudents = (ArrayList<String[]>)
				session.getAttribute("matchedStudents");
		session.removeAttribute("matchedStudents");
		unmatchedExamStudents = 
				(ArrayList<EnrollmentData>)
				session.getAttribute("unmatchedExamStudents");
		unmatchedRegCourseStudents = 
				(ArrayList<EnrollmentData>)
				session.getAttribute("unmatchedRegCourseStudents");
	}
	final int examCourseId = course.getId();
	EnrollmentRW.transferWork(examCourseId, matchedStudents,
			2, 3); // indices of exam login, regular course login in matches
	final String regCourseIdStr = request.getParameter("regCourseId");
	final int regCourseId = Integer.parseInt(regCourseIdStr);
	final Course regCourse = CourseRW.getCourseInfo(regCourseId); 
	final int numUnmatchedExam = unmatchedExamStudents.size();
	final int numUnmatchedRegCourse = unmatchedRegCourseStudents.size();
	final boolean mayHaveMatches = 
			(numUnmatchedExam != 0 && numUnmatchedRegCourse != 0);

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
		var form = document.matchesForm;
		var bld = new String.builder();
		<% for (int examStudNum = 0; examStudNum < numUnmatchedExam; examStudNum++) { %>
			var choice = parseInt(form.selector<%= examStudNum + 1 %>.value);
			if (choice !== 0) {
				if (bld.length() > 0) bld.append(';');
				bld.append('<%= examStudNum %>:').append(choice - 1);
			}
		<% } // for each unmatched exam student's selector %>
		form.matches.value = bld.toString();
		if (isEmpty(form.matches.value)) {
			form.action = 'deleteExamIds.jsp';
		}
		form.submit();
	}

	// -->
	</script>
</head>

<body class="light" style="background-color:white; overflow:auto;" 
		onload="setTab('<%= toTabName(user.translateJS("Enrollment")) %>');">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

	<div id="contentsWithTabsWithoutFooter">
	<form name="matchesForm" action="transferFinish.jsp" method="post">
		<input type="hidden" name="matches" value="" />
	<table style="width:90%; text-align:center; margin-left:auto; margin-right:auto;">
		<tr><td class="boldtext big"
		style="vertical-align:top; padding-top:10px;">
			<%= user.translate("Match Students Manually") %>
		</td></tr>
		<% if (mayHaveMatches) { 
			final String headerStyle = "border-bottom-style:solid; "
					+ "border-width:1px; "
					+ "border-color:#49521B;"; %>
			<tr><td class="regtext" style="vertical-align:top; 
					padding-top:10px;">
				<%= user.translate(
						"Each of the following students in each course was found not to "
						+ "have exactly the same name and ***ID number*** as any student "
						+ "in the other course.  Choose which students (if any) in "
						+ "***this exam course*** correspond to students in "
						+ "***the regular course***.", 
						new String[] { user.getInstitutionStudentNumLabel(), 
						"<b>" + course.getName() + "</b>", 
						"<b>" + regCourse.getName() + "</b>"}) %>
			</td></tr>
			<tr><td style="vertical-align:top; text-align:center; padding-top:10px;">
				<table class="whiteTable" style="background-color:#f6f7ed;
						text-align:left; width:90%;">
					<tr>
					<th colspan="3" style="text-align:center; padding-right:10px;">
						<%= regCourse.getName() %> (<%= regCourse.getId() %>)
					</th>
					<th style="width:20px;">&nbsp;</th>
					<th colspan="3" style="text-align:center;">
						<%= course.getName() %> (<%= examCourseId %>)
					</th>
					</tr>
					<tr>
					<th style="<%= headerStyle %>"><%= user.translate("No.") %></th>
					<th style="padding-left:10px; <%= headerStyle %>">
						<%= user.translate("Name") %>
					</th>
					<th style="padding-left:10px; padding-right:10px; <%= headerStyle %>">
						<%= user.getInstitutionStudentNumLabel() %>
					</th>
					<th></th>
					<th style="<%= headerStyle %>"><%= user.translate("Match") %></th>
					<th style="padding-left:10px; <%= headerStyle %>">
						<%= user.translate("Name") %>
					</th>
					<th style="padding-left:10px; <%= headerStyle %>">
						<%= user.getInstitutionStudentNumLabel() %>
					</th>
					</tr>
					<% final int max = Math.max(numUnmatchedExam, numUnmatchedRegCourse); 
					for (int studNum = 0; studNum < max; studNum++) { %>
						<tr>
						<% if (studNum < numUnmatchedRegCourse) { 
							final EnrollmentData student =
									unmatchedRegCourseStudents.get(studNum); %>
							<td style="text-align:right;"><%= studNum + 1 %>.</td>
							<td style="padding-left:10px;"><%= student.getName() %></td>
							<td style="padding-left:10px;"><%= student.getStudentNum() %></td>
						<% } else { %>
							<td style="text-align:right;">&nbsp;</td>
							<td style="text-align:right;">&nbsp;</td>
							<td style="padding-left:10px;">&nbsp;</td>
						<% } // if there's an unmatched student studNum from reg course %>
						<td></td>
						<% if (studNum < numUnmatchedExam) { 
							final EnrollmentData student =
									unmatchedExamStudents.get(studNum); %>
							<td style="text-align:right; padding-left:10px;">
								<select name="selector<%= studNum + 1 %>"
										id="selector<%= studNum + 1 %>">
									<option value="0"></option>
									<% for (int regStudNum = 1; 
											regStudNum <= numUnmatchedRegCourse;
											regStudNum++) { %>
										<option value="<%= regStudNum %>">
											<%= regStudNum %>
										</option>
									<% } // for each student in regular course %>
								</select>
							</td>
							<td style="padding-left:10px;"><%= student.getName() %></td>
							<td style="padding-left:10px;"><%= student.getStudentNum() %></td>
						<% } else { %>
							<td style="text-align:right;">&nbsp;</td>
							<td style="text-align:right;">&nbsp;</td>
							<td style="padding-left:10px;">&nbsp;</td>
						<% } // if there's an unmatched student studNum from reg course %>
						</tr>
					<% } // for each student in both lists %>
				</table>
			</td></tr>	
			<tr><td class="regtext" style="vertical-align:top; 
					padding-top:10px;">
				<%= user.translate("Transfer the chosen students' work from "
						+ "their exam course login IDs to their regular course "
						+ "login IDs?") %> 
			</td></tr>
		<% } else if (numUnmatchedExam != 0) { %>
			<tr><td class="regtext" style="vertical-align:top; 
					padding-top:10px;">
				<%= user.translate(
						"There are no unmatched students remaining in "
						+ "***the regular course***, but the following students in "
						+ "***this exam course*** remain unmatched:",
						new String[] {
						"<b>" + regCourse.getName() + "</b>",
						"<b>" + course.getName() + "</b>"}) %>
				<p><table class="whiteTable">
					<% for (int studNum = 0; studNum < numUnmatchedExam; studNum++) { 
						final EnrollmentData student =
								unmatchedExamStudents.get(studNum); %>
						<tr>
						<td style="text-align:right;"><%= studNum + 1 %>.</td>
						<td style="padding-left:10px;"><%= student.getName() %></td>
						<td style="padding-left:10px;"><%= student.getStudentNum() %></td>
						</tr>
					<% } // for each unmatched student in regCourse %>
				</table>
			</td></tr>
		<% } else { %>
			<tr><td class="regtext" style="vertical-align:top;
			padding-top:10px;">
				<%= user.translate("There are no unmatched students remaining "
						+ "in ***this exam course***.", 
						"<b>" + course.getName() + "</b>") %>
		<% } // if mayHaveMatches %>
		<tr><td><table><tr>
			<% if (mayHaveMatches) { %>
				<td style="padding-bottom:10px; padding-top:10px; padding-left:30px;">
					<%= makeButton(user.translate("Transfer work"), "submitIt();") %>
				</td>
			<% } // if mayHaveMatches %>
			<td style="padding-bottom:10px; padding-top:10px;">
				<%= makeButton(user.translate(mayHaveMatches ? "Do not transfer" : "Continue"), 
						"self.location.href='deleteExamIds.jsp?refresh=true';") %>
			</td>
			<td style="padding-bottom:10px; padding-top:10px;">
				<%= makeButton(user.translate("Cancel"), 
						"self.location.href='listEnrollment.jsp?refresh=true';") %>
			</td>
		</tr></td></table></tr>
	</table>
	</form>
	</div>
</body>
</html>
