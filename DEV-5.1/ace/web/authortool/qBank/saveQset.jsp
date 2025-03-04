<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.qBank.QSetDescr,
	com.epoch.session.QSet,
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

	/*
		topicNum
		index
		name
		author
		header
		remarks
	*/
	
	QuestionBank qBank;
	synchronized (session) {
		qBank = (QuestionBank) session.getAttribute("qBank");
	}

    final int topicNum = MathUtils.parseInt(request.getParameter("topicNum"));
	final int indexInTopic = MathUtils.parseInt(request.getParameter("index"));

	final QSetDescr qSetDescr = new QSetDescr();
	qSetDescr.name = Utils.inputToCERs(request.getParameter("name"));
	qSetDescr.author = Utils.inputToCERs(request.getParameter("author"));
	qSetDescr.header = Utils.inputToCERs(request.getParameter("header"));
	qSetDescr.remarks = Utils.inputToCERs(request.getParameter("remarks"));

	if (indexInTopic == 0) qBank.addQSet(topicNum, qSetDescr);
	else qBank.setQSetDescr(topicNum, indexInTopic, qSetDescr);

	QSet currentQSet;
	synchronized (session) {
		currentQSet = (QSet) session.getAttribute("qSet");
	}
	if (currentQSet != null && currentQSet.getQSetId() == qSetDescr.id) {
		final QSet modQSet = new QSet(qSetDescr.id);
		synchronized (session) {
			session.setAttribute("qSet", modQSet);
		}
	}
	final String from = request.getParameter("from");
	
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
	<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<script type="text/javascript">
		// <!-- >
		function closeThis() {
			opener.location.href = 
			<% if (!"questionsList".equals(from)) { %>
				'topicsEditor.jsp?topicNum=<%= topicNum %>&qSetId=<%= qSetDescr.id %>';
			<% } else { %>
				'../questionsList.jsp?qSetId=<%= qSetDescr.id %>';
			<% } %>
			self.close();
		} // closeThis

		// -->
	</script>
	</head>

<body onload="closeThis()">
</body>
</html>

