<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.qBank.Topic,
	com.epoch.session.QSet,
	com.epoch.session.QuestionBank,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<% 
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	final int QSET = 0;
	final int TOPIC = 1;
	final int BANK = 2;

	final int howMuch = MathUtils.parseInt(request.getParameter("howMuch"));
	final String what = (howMuch == QSET ? "question set" 
			: howMuch == TOPIC ? "topic" 
			: "question bank");
	final String propfilename = request.getParameter("propfilename");
	final String modified = request.getParameter("modified");
	final String serialNos = request.getParameter("serialNos");
	String file = null;
	if (modified != null) {
		final String validfilename = Utils.toValidFileName(propfilename);
		QuestionBank qBank;
		synchronized (session) {
			qBank = (QuestionBank) session.getAttribute("qBank");
		}
		switch (howMuch) {
			case QSET:
				QSet qSet; 
				synchronized (session) {
					qSet = (QSet) session.getAttribute("qSet"); 
				}
				file = qSet.exportSet(serialNos, validfilename); 
				break;
			case TOPIC:
				final Topic[] topics = qBank.getTopics();
				final int topicNum = Integer.parseInt(serialNos); // 1-based
				file = topics[topicNum - 1].exportTopic(validfilename); 
				break;
			case BANK:
				file = qBank.exportQuestionBank(validfilename); 
				break;
		} // switch
		if (file != null) file = "/ace/" + file;
	} // if modified

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Question Exporter</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
	<script type="text/javascript">
		<% if (modified == null) { %>
			function reload() {
				var filename = 
						trimWhiteSpaces(document.filenameform.propfilename.value);
				if (isWhiteSpace(filename)) {
					alert('Please enter a filename.');
					return;
				}
				document.filenameform.propfilename.value = filename;
				document.filenameform.submit();
			} // reload
		<% } %>

		function closeMe() {
			self.close();
		}

	</script>
</head>
<body style="text-align:center; vertical-align:middle;
		background-color:#f6f7ed; margin:0px;">

<% if (modified == null) { %>
	<form name="filenameform" action="exportSet.jsp" method="post">
		<input type="hidden" name="modified" value="true"/>
		<input type="hidden" name="serialNos" value="<%= serialNos %>"/>
		<input type="hidden" name="howMuch" value="<%= howMuch %>"/>
	<table class="regtext" style="width:95%; margin-left:auto; margin-top:10px;
			margin-right:auto; border-style:none; border-collapse:collapse; 
			padding-top:10px; padding-left:10px;">
	<tr><td class="boldtext big" style="padding-left:10px;">
		Export <%= what %>
	</td></tr>
	<tr><td style="text-align:left; vertical-align:middle; padding-left:20px;">
		<br/>Please enter a name for the export file, or use the suggested one.
	</td></tr>
	<tr><td style="padding-top:10px; text-align:left; 
			vertical-align:middle; padding-left:20px;">
		<input type="text" name="propfilename"
				style="width:400px; height:20px;"
				value="<%= Utils.toValidTextbox(propfilename) %>"/>
	</td></tr>
	<tr><td style="text-align:left; vertical-align:middle; padding-left:20px;">
		<table>
		<tr>
		<td style="padding-top:10px; padding-left:10px;">
			<%= makeButton("Submit", "reload();") %>
		</td>
		<td style="padding-top:10px; padding-left:10px;">
			<%= makeButton("Close", "closeMe();") %>
		</td></tr>
		</table>
	</td></tr>
	</table>
	</form>
<% } else if (file != null) { %>
	<table class="regtext" style="width:95%; margin-top:10px; margin-left:auto;
			margin-right:auto; border-style:none; border-collapse:collapse;">
	<tr><td class="boldtext" style="padding-left:10px; padding-bottom:10px;">
		ACE has successfully exported the <%= what %> into a zip file.
	</td></tr>
	<tr><td style="padding-left:10px;">
		<span class="boldtext">PC</span>
			&mdash; Right-click on the link below and select 
			"Save Target As..." to download the file to your disk.<br/>
		<span class="boldtext">Mac</span>
			&mdash; Control-click on the link below and select "Download Linked
			File" or "Download Linked File As..." to download the file to 
			your disk.<br/>
		<br/>
		[<a onclick="alert('Please follow the directions above the link.'); return false"
			href="<%= Utils.toValidHref(file) %>">Exported <%= what %> (.zip)</a>]
	</td></tr>
	<tr><td style="padding-top:10px; padding-left:10px;">
		<%= makeButton("Close", "closeMe();") %>
	</td></tr>
	</table>
<% } else {  %>
	<table class="regtext" style="width:95%; margin-left:auto; margin-top:10px;
			margin-right:auto; border-style:none; border-collapse:collapse;">
	<tr><td class="boldtext big" >
		Export <%= what %>
		</td></tr>
	<tr><td style="padding-left:10px;">
		An error occurred while exporting the <%= what %>. <br/>
		Contact the administrator if the error persists. <br/>
	</td></tr>
	<tr><td style="padding-top:10px; padding-left:10px;"><%= 
		makeButton("Close", "closeMe();") %>
	</td></tr>
	</table>
<% } // if modified or file == null %>
</body>
</html>
