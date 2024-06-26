<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.qBank.Topic,
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
		index
		name
		remarks
			
	*/

	QuestionBank qBank;
	synchronized (session) {
		qBank = (QuestionBank) session.getAttribute("qBank");
	}
	final int index = MathUtils.parseInt(request.getParameter("index"));

	final Topic topic = new Topic();
	topic.name = Utils.inputToCERs(request.getParameter("name"));
	topic.remarks = Utils.inputToCERs(request.getParameter("remarks"));

	if (index == 0) qBank.addTopic(topic);
	else qBank.setTopic(index, topic);

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >

	<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<script type="text/javascript">
		function closeThis() {
			var go = 'topicsEditor.jsp';
			if (<%= index != 0 %>) go += '?topicNum=<%= index %>';
			opener.location.href = go;
			self.close();
		} // closeThis
	</script>
	</head>

<body onload="closeThis()">
</body>
</html>

