<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.courseware.Institution,
	com.epoch.session.AnonSession"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../";

	final Institution[] institutions = AnonSession.getVerifiedInstitutions();
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>Payment Grace Periods Manager</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<style type="text/css">
		* html body {
			padding:55px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

	function saveGracePeriod(instnNum) {
		var url = 'savePaymentGracePeriod.jsp';
		var toSend = new String.builder().
				append('instnId=').
				append(getValue('instnId' + instnNum)).
				append('&gracePeriod=').
				append(getValue('gracePeriod' + instnNum)).
				append('&instnName=').
				append(encodeURIComponent(getValue('instnName' + instnNum)));
		callAJAX(url, toSend.toString());
	} // saveGracePeriod()
	
	function updatePage() {
		if (xmlHttp.readyState === 4) { // ready to continue
			alert('Grace period saved.');
		}
	} // updatePage()

	// -->
	</script>
</head>
<body class="regtext">

<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

<div id="contentsWithoutTabs">
<form name="institutionsList" action="dummy">
<table width="95%" class="regtext"> 
	<tr><td class="boldtext big">
		Payments grace period manager
	</td></tr>
	<tr><td style="padding-top:10px; padding-bottom:10px;">
		To exit, press <b>Admin Tool</b> above.
	</td></tr>
</table>

<table class="regtext" style="padding-left:10px; margin-auto:right;"> 
	<tr><th><u><br/>Institution</u></th>
	<th style="padding-left:10px; text-align:center;">
	<u>Grace period in days <br/>(&minus;1 for indefinite)</u></th>
	</tr>
	<% for (int instnNum = 1; instnNum <= institutions.length; instnNum++) {
		final Institution institution = institutions[instnNum - 1];
		final int instnId = institution.getId();
		%>
		<tr>
		<td><%= institution.getName() %>
		<input type="hidden" id="instnId<%= instnNum %>" value="<%= instnId %>" />
		<input type="hidden" id="instnName<%= instnNum %>" 
				value="<%= Utils.toValidHTMLAttributeValue(institution.getName()) %>" />
		</td>
		<td style="text-align:center;">
			<input type="text" size="5" id="gracePeriod<%= instnNum %>"
					value="<%= institution.getGraceDays() %>" />
		</td>
		<td>
			<%= makeButton("Save changes", "saveGracePeriod(" + instnNum + ");") %>
		</td>
		</tr>
	<% } // for each user %>
</table>
</form>
</div>

</body>
</html>
