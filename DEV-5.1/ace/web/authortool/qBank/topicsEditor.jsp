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
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	QuestionBank qBank;
	synchronized (session) {
		qBank = (QuestionBank) session.getAttribute("qBank");
	}
	final int initTopic = MathUtils.parseInt(request.getParameter("topicNum"), -1);
	final String moveQSetFromStr = request.getParameter("moveFrom");
	if (!Utils.isEmpty(moveQSetFromStr)) {
		final int moveQSetFrom = MathUtils.parseInt(moveQSetFromStr);
		final int moveTo = MathUtils.parseInt(request.getParameter("moveTo"));
		qBank.moveQSet(initTopic, moveQSetFrom, moveTo);
	} // if there's a moveFrom parameter
	final Topic[] topics = qBank.getTopics();
	final String qSetIdStr = request.getParameter("qSetId");
	final boolean masterEdit = qBank.isMasterEdit();
	/* Utils.alwaysPrint("topicEditor.jsp: is masterEdit ", masterEdit,
			", qSetId = ", qSetIdStr);
	*/
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head> 
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Topics and Question Sets Editor</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css"
		type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico"
		type="image/x-icon"/>
	<style type="text/css">
		#footer {
			position:absolute; 
			bottom:0; 
			left:0;
			width:100%; 
			height:50px; 
			overflow:auto; 
			text-align:right; 
		}

		#topicEditorContents {
			position:fixed; 
			top:55px;
			left:0;
			bottom:50px; 
			right:0; 
			overflow:auto; 
		}

		* html body {
			padding:55px 0 50px 0; 
		}

		* html #topicEditorContents {
			height:100%; 
		}
	</style>
	<script src="<%= pathToRoot %>js/openwindows.js"
		type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	
	// <!-- >

	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

	<% if (qSetIdStr != null) { %>
		setCookie('topicEditor_qSetId', '<%= qSetIdStr %>');
	<% } %>

	function addTopic() {
		openTopicWindow('editTopic.jsp?index=0');
	}

	function viewTopic(index) {
		openTopicWindow('viewTopic.jsp?index=' + index);
	}

	function editTopic(index) {
		openTopicWindow('editTopic.jsp?index=' + index);
	}

	function addQSet(topicNum) {
		openQSetWindow('editQset.jsp?topicNum=' + topicNum + '&index=0');
	}

	function editQSet(topicNum, index) {
		openQSetWindow('editQset.jsp?topicNum=' + topicNum + '&index=' + index);
	}

	function viewQSet(topicNum, index) {
		openQSetWindow2('viewQset.jsp?topicNum=' + topicNum + '&index=' + index);
	}

	function deleteQSet(topicNum, index) {
		if (confirm('Are you sure you want to delete question set '
				+ index + ' in topic ' + topicNum + '?')) {
			openQSetWindow2('deleteQset.jsp?topicNum=' + topicNum 
					+ '&index=' + index);
		} // if confirm
	} // deleteQSet()

	function moveQSet(topicNum, index) {
		var newIndex = getValue(new String.builder().
				append('qSetSelector').append(topicNum).
				append('_').append(index).toString());
		self.location.href = new String.builder().
				append('topicsEditor.jsp?topicNum=').append(topicNum).
				append('&moveFrom=').append(index).
				append('&moveTo=').append(newIndex).toString();
	} // moveQSet()

	function exitEditor() {
		self.location.href = '../questionsList.jsp?qSetId=' 
				+ getCookie('topicEditor_qSetId');
	}

	var topicOpen = new Array(<%= topics.length %>);
	for (var topicNum = 1; topicNum <= <%= topics.length %>; topicNum++) {
		topicOpen[topicNum - 1] = false;
	}

	function paintQSets(topicNumJS) {
		if (topicOpen[topicNumJS - 1]) {
			clearInnerHTML('topic' + topicNumJS);
			hideCell('addQSetTo' + topicNumJS);
			topicOpen[topicNumJS - 1] = false;
			return;
		} // if the topic has been selected previously
		<% for (int topicNum = 1; topicNum <= topics.length; topicNum++) { %>
			if (topicNumJS === <%= topicNum %>) {
				var out = new String.builder().
						append('<table class="whiteTable" style='
							+ '"width:100%; background-color:#f6f7ed; '
							+ 'border-collapse:collapse;">');
				<% final QSetDescr[] qSetDescrs = 
						qBank.getQSetDescrs(topicNum);
				for (int qSetNum = 1; qSetNum <= qSetDescrs.length; qSetNum++) { 
					final QSetDescr qSetDescr = qSetDescrs[qSetNum - 1]; 
					final int numQs = qBank.getNumQsInQSet(qSetDescr.id);
					final String rowClass = (qSetNum % 2 == 0 
							? "greenrow" : "whiterow"); 
				%>
					out.append('<tr class="<%= rowClass %>"><td class="regtext" style='
							+ '"white-space:nowrap; padding-left:10px;">');
				<%	if (masterEdit) { %>
						out.append('<select name="qSetSelector<%= topicNum %>_<%= qSetNum %>" '
								+ 'id="qSetSelector<%= topicNum %>_<%= qSetNum %>" '
								+ 'onchange="moveQSet(<%= topicNum %>, <%= qSetNum %>);">');
						for (var selQSetNum = 1; selQSetNum <= <%= 
								qSetDescrs.length %>; selQSetNum++) {
							out.append('<option value="').
									append(selQSetNum).append('"');
							if (selQSetNum === <%= qSetNum %>) {
								out.append(' selected="selected"');
							} // if this set
							out.append('>').append(selQSetNum).append('<\/option>');
						} // for each qSet in this topic
						out.append('<\/select> ');
				<%	} else { %>
						out.append('[<%= qSetNum %>] ');
				<%	} // if masterEdit %>
					out.append('<%= Utils.toValidJS(Utils.toDisplay(qSetDescr.name)) %>'
							+ '<\/td><td style="width:100%;"><\/td>'
							+ '<td style="text-align:right; vertical-align:top;">'
							+ '<table align="right"><tr>');
					<% if (masterEdit || qSetDescr.id < 0) { 
						if (qSetDescr.id < 0) { %>
							out.append('<td class="boldtext" '
									+ 'style="white-space:nowrap; padding-bottom:10px;">'
									+ '[Local]<\/td>');
						<% } // if a local qSet %>			
						out.append('<td style="white-space:nowrap;">'
								+ '<%= Utils.toValidJS(makeButtonIcon("edit", pathToRoot,
										"editQSet(", topicNum, ", ", qSetNum, ");")) %>');
						<% if (numQs == 0) { %> 
							out.append('<%= Utils.toValidJS(
									makeButtonIcon("delete", pathToRoot,
									"deleteQSet(", topicNum, ", ", qSetNum, ");")) %>');
						<% } // if qSet is empty %>			
						out.append('<\/td>');
					<% } else { // local edit, master qSet %>
						out.append('<td class="boldtext" '
								+ 'style="white-space:nowrap; padding-bottom:10px;">'
								+ '[Master]<\/td>'
								+ '<td class="boldtext" style="white-space:nowrap;">'
								+ '<%= Utils.toValidJS(makeButtonIcon("view", pathToRoot,
										"viewQSet(", topicNum, ", ", qSetNum, ");")) %><\/td>');
					<% } // if is master edit %>
					out.append('<\/tr><\/table><\/td><\/tr>');
				<% } // each qSetDescr index qSetNum %>
				out.append('<\/table>');
				setInnerHTML('topic' + topicNumJS, out.toString());
				showCell('addQSetTo' + topicNumJS);
			} // if this is the topicNum
		<% } // for each topicNum %>
		topicOpen[topicNumJS - 1] = true;
	} // paintQSets()

	// -->
	</script>

</head>
<body class="light" style="background-color:white; text-align:center;"
		onload="paintQSets(<%= initTopic %>);">

	<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

	<form name="topicform" action="dummy">
	<div id="topicEditorContents">
	<table class="regtext" style="width:626px; margin-left:auto; margin-right:auto;
			border-style:none; border-collapse:collapse;">
		<tr>
			<td class="boldtext big"
			style="white-space:nowrap; padding-bottom:10px;">
				Topics and question sets overview
			</td>
		</tr>
		<tr>
			<td class="regtext" colspan="3" 
			style="padding-left:5px; padding-bottom:10px;">
				Click on a topic to see, edit, add to, or delete its question 
				sets.  (You are able to delete only question sets that 
				<%= masterEdit ? "" : "you have created and that" %>
				contain no questions<%= masterEdit ? 
					", including locally authored ones" : "" %>.)
			</td>
		</tr>
		<tr>
			<td class="boldtext enlarged" colspan="3"
					style="padding-left:5px; padding-bottom:10px;">
				Topics <%= masterEdit ?
		   			 	makeButtonIcon("add", pathToRoot, "addTopic();") : "" %>
			</td>
		</tr>
		<% for (int topicNum = 1; topicNum <= topics.length; topicNum++) { 
			final Topic topic = topics[topicNum - 1]; %>
			<tr>
				<td class="boldtext" style="padding-left:10px; 
						white-space:nowrap;">
					(<%= topicNum %>) 
					<a onclick="javascript:paintQSets(<%= topicNum %>)"><%= 
							topic.name %></a>
				</td>
				<td style="width:100%;"></td>
				<td id="addQSetTo<%= topicNum %>" 
						style="visibility:hidden; text-align:right;">
					<%= makeButton("Add new question set", 
							"addQSet(", topicNum, ");") %>
				</td>
				<td id="topicEdit<%= topicNum %>" 
						style="text-align:right; margin-right:0px;">
					<%= masterEdit 
							? makeButtonIcon("edit", pathToRoot, 
								"editTopic(", topicNum, ");") 
							: makeButtonIcon("view", pathToRoot, 
								"viewTopic(", topicNum, ");") %>
				</td>
			</tr> 
			<tr>
				<td colspan="4" id="topic<%= topicNum %>" style="padding-bottom:10px;">
				</td>
			</tr>
		<% } // for each topic index topicNum %>
	
	</table>
	</div>
	<div id="footer">
	<table class="regtext" style="width:626px; margin-left:auto; margin-right:auto;
			margin-top:5px; border-style:none; border-collapse:collapse;">
		<tr><td>
			<table style="margin:0px;"><tr>
				<td style="width:100%;">
				</td> <td style="text-align:right; margin-right:0px;">
					 <%= makeButton("Exit", "exitEditor();") %>
				</td>
			</tr></table>
		</td></tr>
	</table>
	</div>
	</form>
	
</body>
</html>
