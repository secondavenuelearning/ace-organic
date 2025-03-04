<%@ page language="java" %>
<%@ page import="com.epoch.utils.Utils" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	String text = request.getParameter("text");
	// final String sourceCode = (editable ? text : Utils.toValidHTML(text));

%>
<html>
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>Alert</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
		// <!-- >

		function closeMe(e) {
			if (e && ([10, 13].contains(e.keyCode))) self.close();
		} // closeMe()

		// -->
	</script>
</head>
<body onkeypress="closeMe(event);" style="overflow:auto;">
<table class="regtext" style="margin-left:auto; margin-right:auto; width:95%;">
<tr><td><%= text %></td></tr>
<tr><td>
	<table>
		<tr><td style="width:100%;">&nbsp;
		</td><td><%= makeButton("Close", "self.close();") %>
		</td></tr>
	</table>
</td></tr>
</table>
</body>
</html>
