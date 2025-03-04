<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.qBank.QSetDescr,
	com.epoch.qBank.Topic,
	com.epoch.session.QuestionBank,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	// determine if this is a master edit
	EpochEntry entry1;
	String userId;
	QuestionBank qBank;
	synchronized (session) {
		entry1 = (EpochEntry) session.getAttribute("entry");
		userId = (String) session.getAttribute("userId");
		qBank = (QuestionBank) session.getAttribute("qBank");
	}
	final boolean masterEdit = entry1.isMasterEdit();

	final String serialNos = request.getParameter("serialNos");
	// Utils.alwaysPrint("changeQSet.jsp: serialNos = ", serialNos);
	final String[] array = serialNos.split(":");
	final String buttonLabel = "Move question" + (array.length > 1 ? "s" : ""); 
	final int currentQSetId = MathUtils.parseInt(request.getParameter("currentQSetId"));

	if (qBank == null) {
		qBank = (masterEdit ? new QuestionBank() : new QuestionBank(userId));
		synchronized (session) {
			session.setAttribute("qBank", qBank);
		}
	}
	final Topic[] topics = qBank.getTopics();
	final int initTopicNum = MathUtils.parseInt(request.getParameter("topicNum"));

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Question Mover</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	var topicNames = [];
	var qSetcounts = [];

	function loadAllQSets() {
		var form = document.selectform;
        var topicNum = form.topics.value;
		if (topicNum === '0') return;
		var out = new String.builder().
				append('<select id="qSetId" name="qSetId" style="width:375px;">'
					+ '<option value="0" selected="selected">'
					+ 'Select a question set');
       	<% for (int topicNum = 1; topicNum <= topics.length; topicNum++) { %>
			topicNames[<%= topicNum %>] = 
					'<%= Utils.toValidJS(topics[topicNum - 1].name) %>';
			if (topicNum === '<%= topicNum %>') {
				<% final QSetDescr[] qSetDescrs = qBank.getQSetDescrs(topicNum);
				for (final QSetDescr qSet : qSetDescrs) {
					if (currentQSetId != qSet.id) { %>
						out.append('<option value="<%= qSet.id 
								%>"><%= Utils.toValidJS(Utils.toPopupMenuDisplay(
										qSet.name)) %>');
					<% } // if current qSet is not this qSet
				} // for each qSet %>
				if (<%= qSetDescrs.length %> === 1) out.append(' [only set]');
				qSetcounts['<%= topicNum %>'] = <%= qSetDescrs.length %>;
			} // if the topic is selected
		<% } %>
		out.append('<\/select>');
		setInnerHTML('qSetSelector', out.toString());
		if (qSetcounts[topicNum] === 1) {
			form.qSetId.selectedIndex = 1;
		} 
    }

	function move() {
		if (document.selectform.qSetId.value === '0') {
			alert('Please choose a topic and a question set '
					+ 'to which to move this question.');
			return;
		}
		document.selectform.submit();
	}		

	function cancel() {
		self.close();
	} 

	// -->
</script>

</head>
<body class="light" style="text-align:center; margin:0px; 
		margin-bottom:3px; background-color:white;"
		onload="loadAllQSets();">
<form name="selectform" action="moveQ.jsp" method="post">
	<input type="hidden" name="serialNos" value="<%= serialNos %>" />
	
<table class="regtext" style="width:90%; margin-left:auto; margin-right:auto;">
	<tr style="vertical-align:middle;">
		<td class="boldtext enlarged" >
			Move the question to a different question set
		</td>
	</tr>
	<tr>
		<td style="width:15%; padding-left:10px; 
				vertical-align:top; padding-top:10px;">
			Select the topic:
		</td>
	</tr>
	<tr>
		<td style="padding-left:10px; padding-right:10px; vertical-align:top;">
			<select id="topics" name="topics" style="width:375px;" 
					onchange="loadAllQSets();">
				<% if (initTopicNum == 0) { %>
					<option value="0" selected="selected">Choose a topic.</option>
				<% } // if there is no current topic (shouldn't happen)
				for (int topicNum = 1; topicNum <= topics.length; topicNum++)  { 
					final Topic topic = topics[topicNum - 1]; %>
					<option value="<%= topicNum %>" <%= topicNum == initTopicNum 
							? "selected=\"selected\"" : "" %>>
					<%= Utils.toValidJS(Utils.toPopupMenuDisplay(topic.name)) %> 
					</option>
				<% } // for topicNum %>
			</select>
		</td>
	</tr>
	<tr>
		<td style="padding-top:20px; padding-left:10px; vertical-align:top;">
			Select the question set:
		</td>
	</tr>
	<tr>
		<td class="boldtext" style="padding-left:10px; 
				padding-right:10px; vertical-align:top; padding-bottom:6px;">
			<div id="qSetSelector">
				<select id="qSetId" name="qSetId" style="width:375px;">
					<option value="0">
						Choose a topic to view the question sets.
					</option>
				</select>
			</div>
		</td>
	</tr>
	<tr>
		<td style="padding-top:10px; padding-left:10px; 
				vertical-align:top; padding-bottom:10px;">
			<table style="padding-left:0px;">
				<tr>
					<td style="padding-left:0px">
						<%= makeButton(buttonLabel, "move();") %>
					</td>
					<td>
						<%= makeButton("Cancel", "cancel();") %>
					</td>
				</tr>
			</table>
		</td>
	</tr>
</table>

</form>
</body>
</html> 

