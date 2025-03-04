<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.qBank.Topic,
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

    //  index - o if new
	//          else sequence number of the topic

	QuestionBank qBank;
	synchronized (session) {
		qBank = (QuestionBank) session.getAttribute("qBank");
	}

	final int index = MathUtils.parseInt(request.getParameter("index"));
	String name = "";
	String remarks = "";
	if (index != 0) {
		final Topic topic = qBank.getTopic(index);
		name = topic.name;
		remarks = topic.remarks;
	}

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Topic Editor</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css"
		type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico"
		type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/jslib.js"  type="text/javascript"></script>
	<script type="text/javascript">
		function submitIt(form) {
			if (isWhiteSpace(form.name.value)) {
				alert('Enter a name');
				return;
			}
			form.remarks.value = form.remarks_text.value;
			form.submit();			
		}
	</script>
</head>
<body style="background-color:#f6f7ed;">


	<form id="mainForm" name="mainForm" action="saveTopic.jsp" method="post" 
			accept-charset="UTF-8">
	<input type="hidden" name="index" value="<%= index %>"  />
	<input type="hidden" name="remarks" />

	<span class="boldtext big" >
	<%if (index == 0) { %>
		Add new topic
	<% } else { %>
		Edit topic:&nbsp;<%= index %>
	<% } %>
	</span>

	<table class="regtext">
	<tr><td>
	Name: </td><td><input type="text" name="name" 
			value="<%= Utils.toValidTextbox(name) %>" style="width:450px;" />
	</td></tr>
	<tr><td colspan="2">
	Remarks:<br />
	<textarea name="remarks_text" style="width:490px; height:100px;"><%= 
			Utils.toValidTextbox(remarks) %></textarea>
	</td></tr>
	<tr><td colspan="2" style="text-align:center;">
	<table style="margin-left:auto; margin-right:auto;"><tr>
	<td style="width:50%; text-align:right;">
		<%= makeButton("Save", "submitIt(document.mainForm);") %>
	</td><td style="width:50%; text-align:left;">
		<%= makeButton("Cancel", "self.close();") %>
	</td>
	</tr></table>
	</td></tr>

	</table>

	</form>

</body>
</html>
