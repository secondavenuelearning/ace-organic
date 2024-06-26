<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ page import="
	com.epoch.qBank.QSetDescr,
	com.epoch.session.QuestionBank,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>

<%@ page errorPage="/errormsgs/errorHandler.jsp" %>



<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

    //  index - o if new
	//          else sequence number of he chapter

	QuestionBank qBank;
	synchronized (session) {
		qBank = (QuestionBank) session.getAttribute("qBank");
	}

    final int topicNum = MathUtils.parseInt(request.getParameter("topicNum"));
	final int index = MathUtils.parseInt(request.getParameter("index"));
	String name = "";
	String author = "";
	String header = "";
	String remarks = "";

	if (index != 0) {
		final QSetDescr qSetDescr = qBank.getQSetDescr(topicNum, index);
		name = qSetDescr.name;
		author = qSetDescr.author;
		header = qSetDescr.header;
		remarks = qSetDescr.remarks;
	}

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
</head>
<body style="text-align:left; background-color:#f6f7ed;">
	<table style="margin-left:10px; background-color:#f6f7ed; 
			width:100%;">
	<tr><td class="boldtext big" style="padding-top:10px; padding-left:10px; 
			padding-right:10px;">
		Question set info
	</td></tr>
	<tr><td class="boldtext" style="padding-top:10px; padding-left:10px; 
			padding-right:10px;">
		Name: <%= name %>
	</td></tr>
<!-- Author of chapter is meaningless; Qs from different books are intermingled
	<tr><td class="boldtext" style="padding-top:10px; padding-left:10px; padding-right:10px;">
	Author: <%=author%>
	</td></tr>
-->
	<tr><td class="regtext" style="padding-left:10px; padding-right:10px; 
			padding-top:10px;">
		<span class="boldtext">Remarks:</span>
		<%= remarks %>
	</td></tr>
	<tr><td class="regtext" style="padding-left:10px; padding-right:10px; 
			padding-top:10px;">
		<span class="boldtext">Statement to be repeated for every question 
		in this set:</span>
		<%= header %>
	</td></tr>
	<tr><td style="text-align:center; padding:10px;">
		<%= makeButton("Close", "self.close();") %>
	</td></tr>
	</table>
</body>
</html>
