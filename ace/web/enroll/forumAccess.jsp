<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.EnrollmentData,
	com.epoch.utils.Utils"
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

	String block = request.getParameter("block");
	final String indicesStr = request.getParameter("indices");
	Utils.alwaysPrint("forumAccess.jsp: block = ", block,
			", indicesStr = ", indicesStr);
	/**/
	final String[] indicesStrs = indicesStr.split(":");
	final int[] indices = Utils.stringToIntArray(indicesStrs);
	EnrollmentData[] students = null;
	String message = "";

	if (block != null) {
		instrSess.blockUsers(indices, "true".equals(block));
		message = user.translate("Done. If one of the students whose "
				+ "status you have changed is currently logged into ACE, "
				+ "his or her privileges will remain unchanged "
				+ "until the next time he or she logs in.");
	} else { 
		// check that all selected students are registered
		students = instrSess.getEnrolledStudents();
		for (int studNum = 0; studNum < indices.length; studNum++) {
			if (!students[indices[studNum] - 1].isRegistered()) { 
				message = user.translate("You may block students "
						+ "from the forum only if they are already "
						+ "registered with ACE.");
				block = "";
				break;
			} // if student is not registered
		} // for each selected student
	} // if block != null

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
	<title>ACE Forum Access Management</title>
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
	
		function cancel() {
			self.location.href = 'listEnrollment.jsp';
		}

		function forumAccess(block) {
			self.location.href = 'forumAccess.jsp?block='
					+ block + '&indices=<%= indicesStr %>';
		}
	// -->
	</script>
</head>
<body class="regtext"
		onload="setTab('<%= toTabName(user.translateJS("Enrollment")) %>');">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

	<div id="contentsWithTabsWithoutFooter">
	<% if (block != null) { %>
		<table class="regtext" 
				style="font-size:12px; padding-left:10px; padding-top:10px;">
		<tr><td>
		<%= message %>
		</td></tr>
		<tr><td style="padding-top:10px;">
		<%= makeButton(user.translate("OK"), "cancel();") %>
		</td></tr>
		</table>
	<% } else { // block is null %>
		<form name="blockChoice" action="dummy">
		<table class="regtext" 
				style="font-size:12px; padding-left:10px; padding-top:10px;">
		<tr>
			<td colspan="3">
				<%= user.translate("You have selected the following students "
						+ "to block or unblock from the forum") %>:
			</td>
		</tr>
		<% for (int studNum = 0; studNum < indices.length; studNum++) { 
			final EnrollmentData student = students[indices[studNum] - 1];
			final boolean studIsTA = student.isTA();
			final String color = (studIsTA ? "green" : "red"); 
		%>
			<tr>
				<td colspan="3" style="padding-left:20px;">
					<%= student.getName() %>
				</td>
			</tr>
		<% } // for each selected student %>
		<tr>
			<td style="text-align:right; padding-top:10px; padding-left:10px;">
				<%= makeButton(user.translate("Block from forum"), 
						"forumAccess(true);") %>
			</td>
			<td style="text-align:right; padding-top:10px; padding-left:10px;">
				<%= makeButton(user.translate("Unblock from forum"), 
						"forumAccess(false);") %>
			</td>
			<td style="text-align:right; padding-top:10px; padding-left:10px;">
				<%= makeButton(user.translate("Cancel"), "cancel();") %>
			</td>
		</tr>
		</table>
		</form>
	<% } // if block != null %>
	</div>
</body>
</html>
