<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.genericQTypes.TableQ,
	com.epoch.qBank.QDatum,
	com.epoch.qBank.Question"
%>
<%
	response.setHeader("Cache-Control", "no-cache"); //HTTP 1.1
	response.setHeader("Pragma", "no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../";

	Question question = null;
	synchronized (session) {
		question = (Question) session.getAttribute("qBuffer");
	}
	final QDatum[] qData = question.getQData(Question.GENERAL);
	final String tableData = qData[TableQ.PRELOAD_DATA].data;
	final TableQ tableQ = new TableQ(tableData); 
	final String table = tableQ.convertToHTML(qData, question.chemFormatting(), 
			TableQ.AUTH_DISPLAY);
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
	<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>Preload table</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
</head>
<body style="background-color:#f6f7ed; overflow:auto;">
	<table style="margin-left:0px; margin-right:0px; 
			background-color:#f6f7ed; width:80%;">
	<tr><td class="boldtext big" style="padding-left:10px; 
			padding-right:10px; padding-top:10px;">
		Preload table
	</td></tr>
	<tr><td style="text-align:center; padding-left:10px; padding-right:10px;">
		<%= table %>
	</td></tr>
	<% if (table.indexOf(TableQ.DISABLED_STYLE) >= 0) { %>
	<tr><td class="regtext" style="text-align:left; padding-top:20px;
			padding-left:10px; padding-right:10px;">
		Respondents may not change the contents of cells displayed here with
		<span style="<%= TableQ.DISABLED_STYLE %>">this background color</span>.
		The &blank; character indicates an empty cell.
	</td></tr>
	<% } %>
	</table>
</body>
</html>
