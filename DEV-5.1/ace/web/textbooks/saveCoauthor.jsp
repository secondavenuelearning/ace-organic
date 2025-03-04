<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.exceptions.DBException,
	com.epoch.exceptions.UniquenessException,
	com.epoch.session.AnonSession,
	com.epoch.textbooks.Textbook,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");

	final String instructorId = request.getParameter("instructorId");
	Textbook book;
	synchronized (session) {
		book = (Textbook) session.getAttribute("textbook");
	} // synchronized
	User instructor = null;
	String message = null;
	try {
		book.addCoauthor(instructorId);
		instructor = AnonSession.getUser(instructorId);
	} catch (DBException e) {
		message = e.getMessage();
	}
	final String[] allAuthorNames = book.getAllAuthorNames();
	synchronized (session) {
		session.setAttribute("allAuthorNames", allAuthorNames);
	} // synchronized

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
	<title>ACE Coauthor Management</title>
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
		this.location.href = 'writeTextbook.jsp';
	}

	function back() {
		this.location.href = 'addCoauthor.jsp';
	}
	// -->
	</script>
</head>
<body class="light" style="background-color:white;">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<div id="contentsWithoutTabs">
	<table class="regtext" style="padding-left:10px; padding-top:10px;">
	<% if (!Utils.isEmpty(message)) { %>
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
			Coauthor 
			<%= instructor != null ? instructor.getName().toString() : "" %> 
			added successfully.  
			<p>ACE allows only one author of a textbook to write or edit the
			text at a time.  If one author has been editing the 
			textbook, and another wishes to edit it, the second author will
			have to claim the lock.  Before claiming the lock, the second 
			author should check with the first one that he or she has 
			finished writing.
			</p>
		</td></tr>
		<tr><td style="padding-top:10px;">
			<%= makeButton("Return", "done();") %>
		</td></tr>
	<% } %>
	</table>
	</div>
</body>
</html>
