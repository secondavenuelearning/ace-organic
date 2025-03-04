<%@ page language="java" %>
<%@ page import="
	com.epoch.qBank.Question"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../";

	Question question;
	synchronized (session) {
		question = (Question) session.getAttribute("qBuffer");
	}
	final String qFlagsStr = (question.isMechanism()
			? "mechanism" : question.isSynthesis()
			? "multistep synthesis" : question.usesSubstns()
			? "R-group" : "");
%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE View All Responses</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon">
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<style type="text/css">
		#footer {
			position:absolute; 
			bottom:0; 
			left:0;
			width:100%; 
			height:40px; 
			overflow:auto; 
			text-align:right; 
		}

		#qEditorContents {
			position:fixed; 
			top:55px;
			left:0;
			bottom:40px; 
			right:0; 
			overflow:auto; 
		}

		* html body {
			padding:55px 0 40px 0; 
		}

		* html #footer {
			height:100%; 
		}

		* html #qEditorContents {
			height:100%; 
		}
	</style>
	<script type="text/javascript">
	// <!-- >

		<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

		function goBackAgain() {
        	self.location.href = "question.jsp?qId=same";
		}
	// -->
	</script>
</head>

<body class="light" 
		style="text-align:center; margin:0px; margin-top:5px; background-color:white;">
<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

<div id="qEditorContents">
<table class="regtext" style="width:626px; margin-left:auto; margin-right:auto; 
		border-style:none; border-collapse:collapse;">
<tr>
<td class="boldtext big" style="padding-top:10px;">
	View students' responses
</td>
</tr>
<tr>
<td class="regtext">
	<br/><br/>Sorry, ACE is not yet able to sort and display the responses to 
	<%= qFlagsStr %> questions such as this one.
</td>
</tr>
</table>
</div>

<div id="footer">
<table style="margin-left:auto; margin-right:auto; border-collapse:collapse;">
<tr>
<td><%= makeButton("Back to Question", "goBackAgain();") %></td>
</tr>
</table>
</div>

</body>
</html>
