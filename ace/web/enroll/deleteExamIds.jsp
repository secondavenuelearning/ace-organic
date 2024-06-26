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
	request.setCharacterEncoding("UTF-8");

	final InstructorSession instrSess = (InstructorSession) userSess;
	final String[] examIds = instrSess.getExamIds(instrSess.UNUSED_ONLY);
	final boolean haveUnusedIds = examIds != null && examIds.length > 0;
	synchronized (session) {
		session.setAttribute("examIds", examIds);
	}
	final String refresh = (request.getParameter("refresh") != null ?
			"?refresh=true" : ""); 
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<title>ACE Delete Unused Exam IDs</title>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
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
		self.location.href = 'deleteFinish.jsp';
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
			<%= user.translate("Delete unused exam IDs") %>
		</td></tr>
		<tr><td class="regtext" style="vertical-align:top; 
				padding-top:10px;">
			<%= user.translate("ACE found " 
					+ (haveUnusedIds ? "the following" : "no")
					+ " unused exam IDs associated with this course.") %>
		</td></tr>
		<% if (haveUnusedIds) { %>
			<tr><td style="vertical-align:top; text-align:center; padding-top:10px;">
				<table class="whiteTable" style="background-color:#f6f7ed;
						text-align:left;">
					<% for (int studNum = 0; studNum < examIds.length;
							studNum++) { %>
						<tr>
						<td style="text-align:right;"><%= studNum + 1 %>.</td>
						<td style="padding-left:5px;"><%= examIds[studNum] %></td>
						</tr>
					<% } // for each student %>
				</table>
			</td></tr>	
			<tr><td class="regtext" style="vertical-align:top; 
					padding-top:10px;">
				<%= user.translate("Delete these IDs?") %>
			</td></tr>
		<% } // if there are unused IDs %>
		<tr><td><table><tr>
			<td style="padding-bottom:10px; padding-top:10px; padding-left:30px;">
				<%= makeButton(user.translate(haveUnusedIds ? "Delete" : "Continue"), 
						"submitIt();") %>
			</td>
			<td style="padding-bottom:10px; padding-top:10px;">
				<%= makeButton(user.translate("Cancel"), 
						"self.location.href='listEnrollment.jsp", refresh, "';") %>
			</td>
		</tr></table></td></tr>
	</table>
	</div>
</body>
</html>
