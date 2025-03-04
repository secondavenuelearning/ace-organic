<%@ page language="java" %>
<%@ page import="
	com.epoch.qBank.Topic,
	com.epoch.session.QuestionBank,
	com.epoch.session.QSet,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	int howMuch;
	QuestionBank qBank; 
	QSet qSet; 
	synchronized (session) {
		howMuch = MathUtils.parseInt((String) session.getAttribute("howMuch"));
		qBank = (QuestionBank) session.getAttribute("qBank"); 
		qSet = (QSet) session.getAttribute("qSet"); 
	}
	final String tempfile = request.getParameter("tempfile");
	Utils.alwaysPrint("importSet.jsp: tempfile = ", tempfile, 
			", howMuch = ", howMuch);

	final int QSET = 0;
	final int TOPIC = 1;
	// final int BANK = 2;
	String message = (qBank == null ? "qBank is null" : "");
	try {
		if (howMuch == QSET) {
			Utils.alwaysPrint("importSet.jsp: calling qSet.importSet() "
					+ "with tempfile ", tempfile);
			message = qSet.importSet(tempfile);
		} else if (howMuch == TOPIC && qBank != null) {
			int topicNum;
			synchronized (session) {
				topicNum = MathUtils.parseInt(
						(String) session.getAttribute("topicNum"));
			}
			Utils.alwaysPrint("importSet.jsp: calling topic.importSets()"
					+ ", topicNum = ", topicNum, "...");
			final Topic topic = qBank.getTopic(topicNum); // uses 1-based
			if (topic == null) {
				Utils.alwaysPrint("importSet.jsp: couldn't get topic #", topicNum);
			} else {
				Utils.alwaysPrint("importSet.jsp: calling topic.importSets()...");
				message = topic.importSets(tempfile);
			}
		} else if (qBank != null) {
			Utils.alwaysPrint("importSet.jsp: calling qBank.importTopics()...");
			message = qBank.importTopics(tempfile);
		}
	} catch (Exception e) {
		message = e.getMessage();
	}
	synchronized (session) {
		session.removeAttribute("howMuch");
		session.removeAttribute("topicNum");
	}

%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
</head>
<body onload="hideDoneMessage()">
		<!-- messageValue = @@@@<%= message %>@@@@ -->
</body>
</html>
