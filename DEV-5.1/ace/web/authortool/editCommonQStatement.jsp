<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ page import="com.epoch.utils.Utils" %>

<%

	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	String statement = request.getParameter("statement");
	final String masterEdit = request.getParameter("master");
	final String topicNum = request.getParameter("topicNum");
	final String indexInTopic = request.getParameter("indexInTopic"); // refers to Qset
	/* Utils.alwaysPrint("editCommonQStatement.jsp: master = ", masterEdit,
			", topicNum (in order, not unique ID) = ", topicNum,
			", Qset indexInTopic = ", indexInTopic, ", statement:\n", statement,
			"to textbox:\n", Utils.toValidTextbox(statement.trim())); /**/
	statement = (statement != null && !"[None]".equals(statement) ? 
			Utils.toValidTextbox(statement) : "");

%>

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
	<title>Edit common question statement</title>
	<script type="text/javascript">
	// <!-- >
		function submitIt() {
			document.statementform.submit();
		}

		function cancel() {
			self.close();
		}
		// -->
	</script>
</head>
<body style="background-color:#f6f7ed;">
	<form name="statementform" 
			action="saveStatement.jsp?master=<%= masterEdit %>&amp;topicNum=<%= 
				topicNum %>&amp;indexInTopic=<%= indexInTopic %>" 
			accept-charset="UTF-8" id="statementform" method="post">
	<table class="regtext" style="width:95%; margin-top:10px; margin-left:auto;
			margin-right:auto;">
	<tr><td class="boldtext big" >
		Common question statement
	</td></tr>
	<tr><td class="regtext" style="font-style:italic;">
		Enter or paste the new question statement here. You can
		 use the HTML tags: &lt;b&gt;, &lt;u&gt;, &lt;i&gt;, &lt;br&gt;, and 
		 &lt;p&gt; for formatting.   Character entity references such as &amp;deg; 
		 for a &deg; sign may also be used.   
	</td></tr>
	<tr><td align="center">
		<textarea id="statement" name="statement" rows="20" cols="66"><%=
				statement.trim() %></textarea>
	</td></tr>
	<tr><td>
		<table style="text-align:center; margin-left:auto; margin-right:auto;">
			<tr><td>
				<%= makeButton("Save", "submitIt();") %>
			</td><td>
				<%= makeButton("Cancel", "cancel();") %>
			</td></tr>
		</table>
	</td></tr>
	</table>
	</form>
</body>
</html>


