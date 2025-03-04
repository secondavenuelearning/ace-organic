<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../";

	final AdminSession admSess = (AdminSession) userSess;
	final User[] unverifiedInstructors = admSess.getAllUnverifiedInstructors();
	final String userIdsStr = request.getParameter("userIds");
	if (userIdsStr != null) {
		final String[] userIds = userIdsStr.split(",");
		admSess.verifyInstructors(userIds);
	} // if there is a userIds string

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>Verify multiple instructors</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<style type="text/css">
		* html body {
			padding:55px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

	function checkAllBoxes() {
		var checkboxes = document.userForm.usersChecker;
		setAllCheckBoxes(checkboxes, true);
		checkboxes = document.userForm.allUsersChecker;
		setAllCheckBoxes(checkboxes, true);
	} // checkAllBoxes()

	function verifySelected() {
		var checkboxes = document.userForm.usersChecker;
		var selectedInstructors = getSelectedValues(checkboxes);
		if (selectedInstructors.length == 0) {
			alert('No instructors have been selected.');
			return;
		} // if there are no selected users
		var url = 'verifyInstructors.jsp';
		var toSend = new String.builder()
				.append('userIds=');
		for (var userIdNum = 0; userIdNum < selectedInstructors.length; userIdNum++) {
			toSend.append(userIdNum === 0 ? '' : ',');
			toSend.append(encodeURIComponent(selectedInstructors[userIdNum]));
		} // for each userIdNum
		callAJAX(url, toSend.toString());
	} // verifySelected()
	
	function updatePage() {
		if (xmlHttp.readyState === 4) { // ready to continue
			alert('All selected instructors have been verified.');
			self.location.href = 'listProfiles.jsp';
		}
	} // updatePage()

	function init() {
		<% if (unverifiedInstructors.length == 0) { %> 
			alert('All registered instructors have already been verified.');
			self.location.href = 'listProfiles.jsp';
		<% } else { %>
			checkAllBoxes();
		<% } %>
	} // init()

	// -->
	</script>
</head>
<body class="regtext" onload="init();">

<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

<div id="contentsWithoutTabs">
<form name="userForm" action="dummy">
<table class="regtext" style="margin-left:auto; margin-right:auto; width:95%"> 
	<tr><td class="boldtext big" style="width:100%;">
		Verify multiple instructors
	</td></tr>
	<tr><td style="padding-top:10px; padding-bottom:10px;">
		To exit, press <b>Admin Tool</b> above.
	</td></tr>
</table>

<table class="regtext" 
		style="border-collapse:collapse; margin-right:auto;"> 
	<tr>
	<th style="padding-left:10px; padding-right:10px; width:40px; text-align:center;">
	<input type="checkbox" title="check all" name="allUsersChecker"
			onclick="setAllCheckBoxes(document.userForm.usersChecker, 
			this.checked)" />&nbsp;<u>All</u></th>
	<th style="width:250px;">
	<u>User</u></th>
	<th style="width:250px; padding-left:10px;">
	<u>Institution</u></th>
	</tr>
		<% 
		boolean rowToggle = false;
		for (int instrNum = 0; instrNum < unverifiedInstructors.length; instrNum++) {
			rowToggle = !rowToggle;
			final String rowColor = (rowToggle ? "whiterow" : "greenrow");
			final User instructor = unverifiedInstructors[instrNum];
			final String nameFamily1st = instructor.getName().toString();
				%>
				<tr class="<%= rowColor %>">
				<td style="width:40px; text-align:center;">
					<input type="checkbox" title="check" name="usersChecker"
						value="<%= Utils.toValidHTMLAttributeValue(
							instructor.getUserId()) %>" />
				</td>
				<td><span id="name<%= instrNum + 1 %>"><%= nameFamily1st %></span>
				</td>
				<td style="padding-left:10px;">
				<%= instructor.getShortInstitutionName() %></td>
				</tr>
		<% } // for each user %>
	</tr> 
	<tr>
	<td></td><td>
		<%= makeButton("Verify selected instructors", "verifySelected();") %>
	</td>
	</tr>
</table>
</form>
</div>

</body>
</html>
