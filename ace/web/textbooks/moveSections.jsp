<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.textbooks.Textbook,
	com.epoch.textbooks.TextChapter,
	com.epoch.textbooks.TextContent,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server

	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noAccess.html" /> <%
	}

	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final String CHECKED = " checked=\"checked\"";
	final String SELECTED = "selected=\"selected\" ";

	Textbook book;
	synchronized (session) {
		book = (Textbook) session.getAttribute("textbook");
	} // synchronized
	final int numChapters = book.getNumChapters();
	final int chapNum = MathUtils.parseInt(request.getParameter("chapNum"));
	final int numContents = book.getChapter(chapNum).getNumContents();
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Embedded Textbook Content Editor</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css"
		type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico"
		type="image/x-icon"/>
	<style type="text/css">
	body {
		margin:0;
		border:0;
		padding:0;
		height:100%; 
		max-height:100%; 
		font-family:arial, verdana, sans-serif; 
		font-size:76%;
		overflow: hidden; 
	}

	* html body {
		padding:0px 0 50px 0; 
	}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >

	function cancelMe() {
		self.close();
	} // cancelMe()

	var chapSizes = new Array();
	<% for (final TextChapter chapter : book.getChapters()) { %>
		chapSizes.push(<%= chapter.getNumContents() %>);
	<% } // for each chapter %>

	function adjustSelector(which) {
		var form = document.moveForm;
		var modifySelector;
		var start;
		var end;
		if (which === 'to') {
			modifySelector = form.to;
			start = parseInt(form.from.value);
			end = chapSizes[parseInt(form.targetChapNum.value) - 1];
		} else {
			modifySelector = form.from;
			start = 1;
			end = parseInt(form.to.value);
		}
		var modifySelectorValue = parseInt(modifySelector.value);
		var optsBld = new String.builder();
		for (var num = start; num <= end; num++) { // <!-- >
			optsBld.append('<option value="').append(num).append('"');
			if (num === modifySelectorValue) {
				optsBld.append(' selected="selected"');
			}
			optsBld.append('>').append(num).append('<\/option>');
		} // for each content in new chapter
		setInnerHTML(which, optsBld.toString());
	} // adjustSelector()

	function changeTargetChap() {
		var newTargetChapNum = parseInt(document.moveForm.targetChapNum.value);
		var numNewTargetChapContents = (newTargetChapNum === 0
				? 0 : chapSizes[newTargetChapNum - 1]);
		var optsBld = new String.builder();
		for (var num = 1; num <= numNewTargetChapContents; num++) { // <!-- >
			optsBld.append('<option value="').append(num).append('">').
					append(num).append('<\/option>');
		} // for each content in new chapter
		optsBld.append('<option value="').
				append(numNewTargetChapContents + 1).
				append('">[end]<\/option>');
		setInnerHTML('posn', optsBld.toString());
		if (newTargetChapNum !== <%= chapNum %>) {
			var extraBld = new String.builder();
			if (newTargetChapNum === 0) {
				extraBld.append('Name of new chapter: '
						+ '<input type="text" name="newChapName" size="70" \/><br\/>');
			} // if chapter is new
			extraBld.append('Display target chapter after move?&nbsp;&nbsp;&nbsp;'
					+ '<input type="checkbox" name="showTargetChap"\/>');
			setInnerHTML('showChapterCell', extraBld.toString());
		} // if target is not starting chapter
	} // changeTargetChap()

	function submitIt() {
		var form = document.moveForm;
		if (form.newChapName && isWhiteSpace(form.newChapName.value)) {
			alert('Please enter a name for the new chapter.');
		} else form.submit();
	} // submitIt()

	// -->
	</script>
</head>

<body class="light" style="margin:0px; margin-top:5px; background-color:#f6f7ed; 
		text-align:left; overflow:auto;">

<form name="moveForm" action="saveMove.jsp" method="post">
	<input type="hidden" name="startChapNum" value="<%= chapNum %>" />
<table class="regtext" style="margin-left:auto; margin-right:auto; 
		text-align:left; width:90%; padding-top:10px;" summary="">
	<tr><td>
		Move contents in range
		<select name="from" id="from" onchange="adjustSelector('to');">
			<% for (int num = 1; num <= numContents; num++) { %>
				<option value="<%= num %>"><%= num %></option>
			<% } // for each number %>
		</select>
		to
		<select name="to" id="to" onchange="adjustSelector('from');">
			<% for (int num = 2; num <= numContents; num++) { %>
				<option value="<%= num %>"><%= num %></option>
			<% } // for each number %>
		</select>
	</td></tr>
	<tr><td style="padding-top:10px;">
		to chapter
		<select name="targetChapNum" id="targetChapNum" onchange="changeTargetChap();">
			<% for (int num = 1; num <= numChapters; num++) { %>
				<option value="<%= num %>" <%= num == chapNum
						? "selected=\"selected\"" : "" %>>
				<%= book.getChapter(num).getName() %>
				<%= num == chapNum ? " (current chapter)" : "" %>
				</option>
			<% } // for each number %>
			<option value="0">[new chapter]</option>
		</select>,
		position
		<select name="posn" id="posn">
			<% for (int num = 1; num <= numContents; num++) { %>
				<option value="<%= num %>"><%= num %></option>
			<% } // for each number %>
			<option value="<%= numContents + 1 %>">[end]</option>
		</select>
	</td></tr>
	<tr><td id="showChapterCell" style="padding-top:10px;">
	</td></tr>
	<tr><td style="text-align:center; padding-top:20px;">
		<table style="margin-right:auto; margin-left:auto;" summary="">
		<tr>
		<td><%= makeButton("Apply Changes", "submitIt();") %></td>
		<td><%= makeButton("Cancel", "cancelMe();") %></td>
		</tr>
		</table>
	</td></tr>
</table>
</form>
</body>
</html>
