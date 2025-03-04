<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page language="java" %>
<%@ page import="
	com.epoch.utils.Utils"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../";

%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" >
<title>
Constrained response collector
</title>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/wordCheck.js" type="text/javascript"></script>
</head>
<body onload="wordsInit('');">
<center>
<h2>Constrained response collector</h2>
</center>
<center>
<table style="width:80px;" summary=""> <tr>
<td><textarea id="stmt1" name="stmt1" rows="5" cols="50"
		onkeyup="updateText(1);"></textarea>
</td></tr>
<!-- <tr><td>Acceptable words: <span id="wordList" style="color:blue;"> -->
</span></td></tr>
<tr><td class="regtext"><div id="mirror1"></div></td></tr>
</table>
</center>

</body>
</html>

