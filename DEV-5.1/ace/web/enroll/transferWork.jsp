<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%	
	final String pathToRoot = "../"; 

	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	final Course[] courses = ((InstructorSession) userSess).getCourses();
	final String studentNumLabel = user.getInstitutionStudentNumLabel();
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<title>ACE Transfer Student Work</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon">
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css">
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
			document.editform.submit();
		}
	// -->
	</script>
</head>
<body class="light" style="background-color:white; overflow:auto;" 
		onload="setTab('<%= toTabName(user.translateJS("Enrollment")) %>');">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

	<div id="contentsWithTabsWithoutFooter">
	<table style="width:626px; text-align:center; margin-left:auto; margin-right:auto;">
		<tr>
		<td class="boldtext big" style="vertical-align:top; padding-top:10px;">
			<%= user.translate("Transfer Student Work from "
					+ "Temporary to Permanent Accounts") %>
		</td>
		</tr>
		<tr>
		<td style="vertical-align:top; text-align:center; padding-top:10px;">
			<form name="editform" action="getMatches.jsp" method="post">
			<table class="whiteTable" style="width:626px; background-color:#f6f7ed;
					text-align:left;">
				<tr>
				<td colspan="2">
					<%= user.translate("This page will transfer the "
							+ "work that students did in temporary ACE "
							+ "accounts to their regular ACE accounts, "
							+ "and then erase the temporary accounts.") %>
				</td>
				</tr>
				<tr>
				<td class="regtext" style="width:60%; padding-right:10px; 
						padding-left:30px; padding-top:10px;">
					<%= user.translate("Choose the course in which the "
							+ "students are enrolled under their regular "
							+ "ACE accounts.") %>
				</td>
				<td style="text-align:left; padding-top:10px;">
					<select name="regCourseId">
					<% for (final Course crs : courses) { 
						final int crsId = crs.getId();
						if (!crs.hide() && crsId != AppConfig.tutorialId) { %>
							<option value="<%= crsId %>">
								<%= crs.getName() %> (<%= crsId %>)
							</option>
						<% } // if not a hidden course
					} // for each course %>
					</select>
				</td>
				</tr>
				<tr>
				<td class="regtext" style="width:60%; padding-right:10px; 
						padding-left:30px; padding-top:10px;">
					<%= user.translate("Choose how to match the students.") %>
				</td>
				<td>
					<select name="matchMethod">
					<option value="both"><%= user.translate(
							"Match by name and ***student ID number***",
							studentNumLabel) %></option>
					<option value="name"><%= user.translate(
							"Match by name only") %></option>
					<option value="studentNum"><%= user.translate(
							"Match by ***student ID number*** only",
							studentNumLabel) %></option>
					</select>
				</td>
				</tr>
				<tr>
				<td colspan="2">
					<table>
					<tr>
					<td style="padding-bottom:10px; padding-top:10px; padding-left:30px;">
						<%= makeButton(user.translate("Match Accounts"), 
								"submitIt();") %>
					</td>
					<td style="padding-bottom:10px; padding-top:10px;">
						<%= makeButton(user.translate("Cancel"), 
								"self.location.href='listEnrollment.jsp';") %>
					</td>
					</tr>
					</table>
				</td>
				</tr>
			</table>
			</form>
		</td>
		</tr>
	</table>
	</div>
</body>
</html>

