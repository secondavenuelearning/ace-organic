<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.qBank.Question,
	com.epoch.session.AnonSession,
	com.epoch.textbooks.Textbook,
	com.epoch.textbooks.TextChapter,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.List"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>

<%	final String pathToRoot = "../";

	final int bookId = course.getACEBookId();
	Textbook book;
	synchronized (session) {
		book = (Textbook) session.getAttribute("textbook");
		if (book == null) {
			final StudentSession studSess = (StudentSession) userSess;
			book = studSess.getTextbook(bookId);
			session.setAttribute("textbook", book);
			final User owner = AnonSession.getUser(book.getOwnerId());
		} // if book is not already in memory
	} // synchronized
	final List<TextChapter> chapters = book.getChapters();
	final int numChaps = book.getNumChapters();
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
<title>ACE Embedded Textbooks</title>
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<style type="text/css">
		* html body {
			padding:110px 0 0px 0; 
		}
</style>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/navigation/courseSidebarJS.jsp.h" %>
	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>

	function openChapter(chapNum) {
		self.location.href = 'readChapter.jsp?chapNum=' + chapNum;
	} // openChapter()

	// -->

</script>
</head>
<body class="light" style="background-color:white;">

<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>
<div id="contentsWithTabsWithoutFooter">

<table style="width:95%; margin-left:auto; margin-right:auto;" summary="">
	<tr>
	<tr>
	<td class="boldtext big" style="text-align:left; padding-top:10px;">
		<% final int numAuthors = allAuthorNames.length;
		if (numAuthors == 1) { %>
			<%= allAuthorNames[0] %>
		<% } else if (numAuthors == 2) {  %>
			<%= allAuthorNames[0] %> and <%= allAuthorNames[1] %>
		<% } else { 
			for (int authNum = 1; authNum <= numAuthors; authNum++) { %>
				<%= allAuthorNames[authNum - 1] 
						+ (authNum == numAuthors ? "" 
							: authNum == numAuthors - 1 ? ", and" : ",") %>
			<% } // for each author
		} // if numAuthors %>
	</td>
	</tr>
	<td class="boldtext big" style="text-align:left; padding-bottom:10px; padding-top:10px;">
		<i><%= Utils.toDisplay(book.getName()) %></i> 
	</td>
	</tr>
	<% if (numChaps > 0) { %>
		<tr>
		<td class="regtext" style="text-align:left; padding-bottom:10px;">
			<table class="regtext" style="width:100%; margin-left:auto; 
					margin-right:auto; border-collapse:collapse;" summary="">
			<% int chapNum = 0;
			for (final TextChapter chapter : chapters) { 
				chapNum++;
			%>
				<tr class="whiterow" style="padding-top:5px; border-collapse:collapse;">
				<td style="width:20px; text-align:right;">
					<%= chapNum %>.
				</td>
				<td style="text-align:left; padding-left:10px;">
					<a href="javascript:openChapter(<%= chapNum %>);"><%= 
							Utils.toDisplay(chapter.getName()) %></a>
				</td>
				</tr>
			<% } // for each chapter %>
			</table>
		</td>
		</tr>
	<% } // if there are chapters %>
</table>

</body>
</html>
