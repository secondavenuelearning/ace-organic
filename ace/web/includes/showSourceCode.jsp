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
	final boolean editable = "true".equals(request.getParameter("editable"));
	final String tdTag = (editable ? "td" : "td id=\"sourceCode\"");
	final String textFieldTag = (editable ? "textarea" : "pre");
	final String textFieldAttrs = (editable
			? " id=\"sourceCode\" name=\"sourceCode\" style=\"font-family:Courier;\"" 
				+ " cols=\"80\" rows=\"35\""
			: "");
	String sourceCodeRaw = request.getParameter("sourceCode");
	if (sourceCodeRaw == null) {
		String sourceCodeNum = request.getParameter("sourceCodeNum");
		if (sourceCodeNum == null) sourceCodeNum = "";
		sourceCodeRaw = (String) session.getAttribute("sourceCode" + sourceCodeNum);
	}
	final String sourceCode = (editable ? sourceCodeRaw
			: Utils.toValidHTML(sourceCodeRaw));

%>
<html>
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>Structure Source Code</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script type="text/javascript" src="<%= pathToRoot %>js/jslib.js"></script>
	<script type="text/javascript">
	// <!-- > avoid parsing the following as HTML

<% if (editable) { %>

	function importSource() {
		var newMRV = document.sourceForm.sourceCode.value;
		if (!isEmpty(newMRV)) opener.parseLewisMRV(newMRV);
		self.close();
	} // importSource()

<% } // if editable %>

	function selectAll() {
		selectText(document.getElementById('sourceCode'));
	} // selectAll()

	function closeMe(e) {
		if (e && ([10, 13].contains(e.keyCode))) self.close();
	} // closeMe()

	// --> end HTML comment
	</script>
</head><body onload="selectAll();" onkeypress="closeMe(event);" style="overflow:auto;"><form 
		name="sourceForm"><table 
		style="margin-left:auto; margin-right:auto; width:95%;"><tr><<%= tdTag %>><<%= 
			textFieldTag + textFieldAttrs %>><%= sourceCode %></<%= textFieldTag %>><% 
if (editable) { %>
	<table>
		<tr><td><%= makeButton("Import", "importSource();") %>
		</td><td><%= makeButton("Cancel", "self.close();") %>
		</td></tr>
	</table><% 
} // if editable %></td></tr></table></form></body></html>
