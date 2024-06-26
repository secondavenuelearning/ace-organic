<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.db.QSetRW,
	com.epoch.db.QuestionRW,
	com.epoch.qBank.QSetDescr,
	com.epoch.qBank.Question,
	com.epoch.qBank.Topic,
	com.epoch.session.QuestionBank,
	com.epoch.session.QSet,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	String userId;
	synchronized (session) {
		userId = (String) session.getAttribute("userId");
	}
	final int qId = MathUtils.parseInt(request.getParameter("qId"));
	final int topicId = QSetRW.getTopicIdByQId(qId);
	final int qSetId = QSetRW.getQSetIDbyQId(qId);
	final String ownerId = QuestionRW.getAuthorIdByQId(qId);
	
	final boolean masterEdit = "true".equals(request.getParameter("masterEdit"));
	final QSet qSet = (masterEdit ? new QSet(qSetId) : new QSet(qSetId, userId));
	synchronized (session) {
		session.setAttribute("qSet", qSet);
	}
	// Utils.alwaysPrint("jumpToQ.jsp: getting question ", qId);
	// see if Q is master-authored and has been hidden from local authors
	final long qFlags = QuestionRW.getQuestionFlags(qId, userId); 
	final boolean isHidden = !masterEdit && Question.hide(qFlags);
	
	/* Utils.alwaysPrint("jumpToQ.jsp: inherent topicId = ", topicId,
			", inherent qSetId = ", qSetId,
			", ownerId = ", ownerId,
			", userId = ", userId,
			", masterEdit = ", masterEdit,
			", isHidden = ", isHidden);
	*/

	// to set the topic and qSet cookies, we need to get their positions 
	// in the arrays presented by qBank
	
	QuestionBank qBank;
	synchronized (session) {
		qBank = (QuestionBank) session.getAttribute("qBank");
	}
	if (qBank == null) {
		qBank = (masterEdit ? new QuestionBank() : new QuestionBank(userId));
		Utils.alwaysPrint("jumpToQ.jsp: qBank was null ");
	} 
	
	int topicArrayIndex = 0;
	int qSetArrayIndex = 0;
	final Topic[] topics = qBank.getTopics();
	for (int topicNum = 1; topicNum <= topics.length; topicNum++) {
		if (topics[topicNum - 1].id == topicId) {
			topicArrayIndex = topicNum;
			final QSetDescr[] qSetDescrs = qBank.getQSetDescrs(topicNum);
			for (int qSetNum = 1; qSetNum <= qSetDescrs.length; qSetNum++) {
				if (qSetDescrs[qSetNum - 1].id == qSetId) {
					qSetArrayIndex = qSetNum;
					break;
				} // qSetDescrs[qSetNum - 1].id
			} // for each qSet qSetNum
			break;
		} // topics[topicNum - 1].id
	} // for each topic topicNum
	
	/* Utils.alwaysPrint("jumpToQ.jsp: qId = ", qId, ", topic array index = ",
			topicArrayIndex, ", qSet array index = ", qSetArrayIndex); */
	
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache" />
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT" />
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>  
</head>
<body class="boldtext" style="text-align:center; vertical-align:middle;">
<p><br/><br/>Looking for the question...
<script type="text/javascript">
	// <!-- >
	var goTo = 'questionsList.jsp';
	if (<%= qId < 0 && masterEdit %>) {
		alert('You\'re in master edit mode, and you\'ve requested '
				+ 'a locally authored question; access denied.');
	} else if (<%= qId < 0 && ownerId == null %>) {
		alert('Sorry, ACE can\'t find a question with that ID number.');
	} else if (<%= qId < 0 && !userId.equals(ownerId) %>) {
		alert('You\'ve requested a question that <%= ownerId %>'
				+ ' authored locally; you are <%= userId %>.');
	} else if (<%= isHidden %>) {
		alert('The question you\'ve requested is not yet available.');
	} else if (<%= qSetId %> === 0) {
		alert('Sorry, ACE can\'t find a question with that ID number.');
	} else {
		setCookie('selected_topic', '<%= topicArrayIndex %>');
		setCookie('selected_qSet', '<%= qSetArrayIndex %>');
		goTo = 'question.jsp?qId=<%= qId %>';
	}
	// alert('final goTo = ' + goTo);
	self.location.href = goTo;
	// -->
</script>  
</body>
</html>


