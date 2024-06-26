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
	final String from = request.getParameter("from");

	String name = "";
	String author = "";
	String header = "";
	String remarks = "";

	if (index != 0) {
		final QSetDescr qSetDescr = qBank.getQSetDescr(topicNum,index);
		name = Utils.toValidTextbox(qSetDescr.name);
		author = Utils.toValidTextbox(qSetDescr.author);
		header = Utils.toValidTextbox(qSetDescr.header);
		remarks = Utils.toValidTextbox(qSetDescr.remarks);
	}

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Question Set Editor</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
<script src="<%= pathToRoot %>js/jslib.js"  type="text/javascript"></script>
<script type="text/javascript">
	function submitIt(form) {
		if (isWhiteSpace(form.name.value)) {
			alert('Enter a name');
			return;
		}
		var header = form.header_text.value;
		var remarks = form.remarks_text.value;
		form.header.value = header;
		form.remarks.value = remarks;
		form.submit();			
	}
</script>
</head>
<body style="background-color:#f6f7ed;">
<form name="qSetform" action="saveQset.jsp" method="post" accept-charset="UTF-8">
	<input type="hidden" name="topicNum" value="<%= topicNum %>"  />
	<input type="hidden" name="index" value="<%= index %>"  />
	<input type="hidden" name="remarks" />
	<input type="hidden" name="header" />
	<% if (from != null) { %>
		<input type="hidden" name="from" value="<%= from %>"  />
	<% } %>
	<table style="background-color:#f6f7ed; width:100%;">
	<tr><td class="boldtext big" 
			style="padding-top:10px; padding-left:10px; padding-right:10px;">
		<% if (index == 0) { %>
		Add a new question set 
		<% } else { %>
		Edit question set <%= index %>:
		<% } %>
	</td></tr>
	<tr><td class="boldtext" style="padding-top:10px; 
			padding-left:10px; padding-right:10px;">
		Name: <input type="text" name="name" 
				value="<%= name %>" size="40" />
	</td></tr>
<!-- Author is meaningless; Qs from different authors are intermingled
	<tr><td class="boldtext" style="padding-top:10px; 
			padding-left:10px; padding-right:10px;">
		Author: <input type="text" name="author" 
				value="<%= author %>" size="40" />
	</td></tr>
-->
	<tr><td class="regtext" style="padding-left:10px; 
			padding-right:10px; padding-top:10px;">
	<span class="boldtext">Remarks:</span><br />
	<textarea name="remarks_text" cols="55" rows="3"><%= 
			remarks %></textarea>
	</td></tr>
	<tr><td class="regtext" style="padding-left:10px; 
			padding-right:10px; padding-top:10px;">
	<span class="boldtext">Statement to be repeated for every 
			question in this set:</span><br />
	<textarea name="header_text" cols="55" rows="8"><%= 
			header %></textarea>
	</td></tr>
	<tr><td style="text-align:center; padding:10px;">
	<table><tr><td>
		<%= makeButton("Save", "submitIt(document.qSetform);") %>
	</td><td>
		<%= makeButton("Cancel", "self.close();") %>
	</td></tr></table>
	</td></tr>
	</table>
</form>
</body>
</html>
