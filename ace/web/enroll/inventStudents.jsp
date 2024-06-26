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
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<title>ACE Student Creation and Enrollment</title>
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
		var form = document.editform;
		if (isWhiteSpace(form.numStudents.value)
				|| !canParseToInt(form.numStudents.value)
				|| parseInt(form.numStudents.value) <= 0) { // <!-- >
			toAlert('<%= user.translateJS("You must enter the number of student "
					+ "identities you wish to create.") %>');
			return;
		}
		form.submit();
	} // submitIt()
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
			<%= user.translate("Create and Enroll students") %>
		</td>
		</tr>
		<tr>
		<td style="vertical-align:top; text-align:center; padding-top:10px;">
			<form name="editform" action="inventEnrollment.jsp" method="post">
			<table class="whiteTable" style="width:626px; background-color:#f6f7ed;
					text-align:left;">
				<tr>
				<td colspan="2">
					<%= user.translate("This page will create a set of fictional-student "
					+ "ACE accounts, enrolled in this course, to which you may "
					+ "assign real students when they take an exam.  The purpose "
					+ "is to prevent students from being able to share their "
					+ "login IDs and passwords for their exam accounts.") %>
				</td>
				</tr>
				<tr>
				<td class="regtext" style="width:60%; padding-right:10px; 
						padding-left:30px; padding-top:10px;">
					<%= user.translate(
							"How many student identities do you wish to create?") %>
				</td>
				<td style="text-align:left; padding-top:10px;">
					<input type="text" name="numStudents" size="8" value=""/>
				</td>
				</tr>
				<tr>
				<td class="regtext" style="width:60%; padding-right:10px; 
						padding-left:30px; padding-top:10px;">
					<%= user.translate(
							"Should the passwords be the same as the login IDs?") %>
				</td>
				<td style="text-align:left; padding-top:10px;">
					<select name="pwdsSame">
						<option value="true" selected="selected">
							<%= user.translate("Yes") %></option>
						<option value="false">
							<%= user.translate("No") %></option>
					</select>
				</td>
				</tr>
				<tr><td colspan="2"><table><tr>
				<td style="padding-bottom:10px; padding-top:10px; padding-left:30px;">
					<%= makeButton(user.translate("Create"), "submitIt();") %>
				</td>
				<td style="padding-bottom:10px; padding-top:10px;">
					<%= makeButton(user.translate("Cancel"), 
							"self.location.href='listEnrollment.jsp';") %>
				</td>
				</tr></table></td></tr>
			</table>
			</form>
		</td>
		</tr>
	</table>
	</div>
</body>
</html>

