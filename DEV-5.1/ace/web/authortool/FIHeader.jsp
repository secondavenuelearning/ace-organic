<%@ page language="java" %>
<%@ page import="
	com.epoch.qBank.Question,
	com.epoch.session.QSet,
	com.epoch.utils.Utils"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../";

	QSet qSet;
	Question question;
	synchronized (session) {
		qSet = (QSet) session.getAttribute("qSet");
		question = (Question) session.getAttribute("qBuffer");
	}
	final boolean viewResp = !question.usesSubstns();
	if (viewResp) { 
		qSet.loadStoredResponses();
	} // if go ahead
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
	<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
		// <!-- 
		function loadHistory() {
			self.location.href = (<%= viewResp %> ? 'FIHistory.jsp' : 'noViewResp.jsp');
        }

		function goBackAgain() {
        	self.location.href = 'question.jsp?qId=same';
		}
		// -->
	</script>
	</head>
<body style="text-align:center; margin:0px; margin-top:5px; background-color:#f6f7ed;" 
		onload="loadHistory();">
	<table style="margin-left:auto; margin-right:auto; border-collapse:collapse;">
	<tr>
	<td><%= makeButton("Back to Question", "goBackAgain();") %></td>
	</tr>
	</table>
	<img src="<%= pathToRoot %>images/border.jpg" alt=""
		style="position:absolute; left:0; top:0; width:100%; height:1px;" />
</body>
</html>

