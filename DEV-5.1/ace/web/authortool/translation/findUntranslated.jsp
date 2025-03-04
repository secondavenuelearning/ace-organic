<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	java.util.ArrayList,
	java.util.LinkedHashMap,
	java.util.Map,
	java.util.Set,
	com.epoch.db.TranslnRead,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	final String language = request.getParameter("language");
	final String[] untransldHeaders = 
			TranslnRead.getUntransldHeaders(language);
	final Map<String, int[]> untransldQStmts = 
			TranslnRead.getUntransldQStmts(language);
	final Map<String, int[]> untransldEvals = 
			TranslnRead.getUntransldEvals(language);
	final Map<String, int[]> untransldTextQData = 
			TranslnRead.getUntransldTextQData(language);

	final Map<String, int[]> untransldAnyPart = 
			new LinkedHashMap<String, int[]>();
	Utils.mergeMaps(untransldAnyPart, untransldQStmts);
	Utils.mergeMaps(untransldAnyPart, untransldEvals);
	Utils.mergeMaps(untransldAnyPart, untransldTextQData);

	final int[] translatableCts = new int[] {
			TranslnRead.countTranslatableHeaders(),
			TranslnRead.countTranslatableQStmts(),
			TranslnRead.countTranslatableTextQData(),
			TranslnRead.countTranslatableEvals()
			};
	final int[] translatedCts = new int[] {
			TranslnRead.countTranslatedHeaders(language),
			TranslnRead.countTranslatedQStmts(language),
			TranslnRead.countTranslatedTextQData(language),
			TranslnRead.countTranslatedEvals(language)
			};
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head> 
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Translation</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
		* html body {
			padding:55px 0 50px 0; 
		}
	</style>
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

	function exitPage() {
		var go = '<%= pathToRoot %>authortool/questionsList.jsp';
		self.location.href = go;
	}

	// -->
	</script>

</head>
<body class="light" style="background-color:white; text-align:center; 
		overflow:auto;">

	<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

	<div id="contentsWithoutTabsWithFooter" class="regtext" 
			style="overflow:auto; padding-left:10px;">
	<a name="top"></a>
	<p>[<a href="#headers">Headers</a>]
	[<a href="#qStmts">Question statements</a>]
	[<a href="#qData">Options in rank, choice, fill-blank questions</a>]
	[<a href="#evals">Evaluator feedbacks</a>]
	</p>

	<p class="boldtext">Questions with any part untranslated into <%= language %>:</p>
	<table>
	<% ArrayList<String> setNames = new ArrayList<String>(untransldAnyPart.keySet());
	setNames.sort(null);
	for (final String setName : setNames) { 
		final int[] qNums = untransldAnyPart.get(setName); %>
		<tr><td><%= Utils.toDisplay(setName) %>:</td>
		<td><%= Utils.contains(untransldHeaders, setName) ? "header, " : "" %>
		<%= Utils.join(qNums) %></td></tr>
	<% } // for each qSet containing untranslated parts %>
	</table>

	<a name="headers"></a>
	<a href="#top"><img src="<%= pathToRoot %>images/top.png" title="Go to top" alt="Top"></a>

	<p class="boldtext">There are <%= translatableCts[0] %> translatable headers, of which 
	<%= translatedCts[0] %> have been translated into <%= language %>.</p>

	<a name="qStmts"></a>
	<p class="boldtext">There are <%= translatableCts[1] %> translatable question 
	statements, of which <%= translatedCts[1] %> have been translated into <%= language %>.
	<br/>Questions whose question statements have not yet been translated into <%= language %>:</p>
	<table>
	<% setNames = new ArrayList<String>(untransldQStmts.keySet());
	for (final String setName : setNames) { 
		final int[] qNums = untransldQStmts.get(setName); %>
		<tr><td><%= Utils.toDisplay(setName) %>:</td><td><%= Utils.join(qNums) %></td></tr>
	<% } // for each qSet containing untranslated qStmts %>
	</table>

	<a name="qData"></a>
	<a href="#top"><img src="<%= pathToRoot %>images/top.png" title="Go to top" alt="Top"></a>

	<p class="boldtext">There are <%= translatableCts[2] %> translatable options 
	(text or compound names) in rank, multiple-choice, and 'select to fill in the blank'
	questions, of which <%= translatedCts[2] %> have been translated into <%= language %>.
	<br/>Questions with such options in which the options have not yet all been translated 
	into <%= language %>:</p>
	<table>
	<% setNames = new ArrayList<String>(untransldTextQData.keySet());
	for (final String setName : setNames) { 
		final int[] qNums = untransldTextQData.get(setName); %>
		<tr><td><%= Utils.toDisplay(setName) %>:</td><td><%= Utils.join(qNums) %></td></tr>
	<% } // for each qSet containing untranslated qData %>
	</table>

	<a name="evals"></a>
	<a href="#top"><img src="<%= pathToRoot %>images/top.png" title="Go to top" alt="Top"></a>

	<p class="boldtext">There are <%= translatableCts[3] %> translatable evaluator 
	feedbacks, of which <%= translatedCts[3] %> have been translated into <%= language %>.
	<br/>Questions whose evaluators have not yet all been translated into <%= language %>:</p>
	<table>
	<% setNames = new ArrayList<String>(untransldEvals.keySet());
	for (final String setName : setNames) { 
		final int[] qNums = untransldEvals.get(setName); %>
		<tr><td><%= Utils.toDisplay(setName) %>:</td><td><%= Utils.join(qNums) %></td></tr>
	<% } // for each qSet containing untranslated evaluator feedback %>
	</table>
	<a href="#top"><img src="<%= pathToRoot %>images/top.png" title="Go to top" alt="Top"></a>
	</div>

	<a href="#top"><img src="<%= pathToRoot %>images/top.png" title="Go to top" alt="Top"></a>

	<div id="footer">
	<table class="regtext" style="margin-left:auto; margin-right:auto;
			margin-top:5px; border-style:none; border-collapse:collapse;">
		<tr><td>
			<table style="margin:0px;"><tr>
				<td id="backButton" style="text-align:right; margin-right:0px;">
					 <%= makeButton("Back", "exitPage();") %>
				</td>
			</tr></table>
		</td>
		</tr>
	</table>
</body>
</html>
