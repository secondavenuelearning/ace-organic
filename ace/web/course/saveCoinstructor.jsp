<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.exceptions.DBException,
	com.epoch.exceptions.UniquenessException,
	com.epoch.session.AnonSession,
	com.epoch.utils.MathUtils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%	
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final int crsId = MathUtils.parseInt(request.getParameter("crsId"));
	final String instructorId = request.getParameter("instructorId");

	User instructor = null;
	String message = "";
	try {
		if (userSess instanceof AdminSession) {
			final AdminSession adminSess = (AdminSession) userSess;
			adminSess.addCoinstructor(crsId, instructorId);
			instructor = AnonSession.getUser(instructorId);
		} else if (userSess instanceof InstructorSession) {
			final InstructorSession instrSess = (InstructorSession) userSess;
			instrSess.addCoinstructor(crsId, instructorId);
			instructor = AnonSession.getUser(instructorId);
		} else message = "This action is not permitted.";
	} catch (DBException e) {
		message = e.getMessage();
	} catch (UniquenessException e) {
		message = e.getMessage();
	}

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
	<title>ACE Coinstructor Management</title>
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

	function done() {
		this.location.href = '<%= pathToRoot %>userHome.jsp';
	}

	function back() {
		this.location.href = 'addCoinstructor.jsp?crsId=<%= crsId %>';
	}
	// -->
	</script>
</head>
<body class="light" style="background-color:white;">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<div id="contentsWithoutTabs">
	<table class="regtext" style="padding-left:10px; padding-top:10px;">
	<% if (!"".equals(message)) { %>
		<tr><td colspan="2">
			<%= message %>
		</td></tr>
		<tr><td style="padding-top:10px;">
			<%= makeButton("Back", "back();") %></td>
		<td style="padding-top:10px;">
			<%= makeButton("Cancel", "done();") %>
		</td></tr>
	<% } else { %>
		<tr><td>
			Coinstructor 
			<%= instructor != null ? instructor.getName().toString() : "" %> 
			added successfully.  
		</td></tr>
		<tr><td style="padding-top:10px;">
			<%= makeButton("Return", "done();") %>
		</td></tr>
	<% } %>
	</table>
	</div>
</body>
</html>
