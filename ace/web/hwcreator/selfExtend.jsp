<%@ page language="java" %>
<%@ page import="
	com.epoch.courseware.EnrollmentData,
	com.epoch.utils.DateUtils,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.List,
	java.util.Map,
	java.util.TimeZone"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../";
	if (role != User.STUDENT) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	request.setCharacterEncoding("UTF-8");

	final String userId = user.getUserId();
	final StudentSession studSess = (StudentSession) userSess;
	final int hwNum = MathUtils.parseInt(request.getParameter("hwNum"));
	final Assgt assgt = assgts[hwNum - 1];
	String currentExtensionStr = assgt.getExtensionStr(userId);
	if (currentExtensionStr == null) currentExtensionStr = "";
	final double remainingCourseExtensions = 
			Math.max(course.getMaxExtensions() - studSess.getSumExtensions()
				+ assgt.getExtension(userId), 0.0);
	final double maxAssgtExtension = assgt.getMaxExtension();
	final double maxAllowedExtension = (maxAssgtExtension < 0.0 
			? remainingCourseExtensions
			: Math.min(remainingCourseExtensions, maxAssgtExtension));
	/* Utils.alwaysPrint("selfExtend.jsp: remainingCourseExtensions = ",
			remainingCourseExtensions, " days, maxAssgtExtension = ",
			maxAssgtExtension, " days, maxAllowedExtension = ",
			maxAllowedExtension, " days."); /**/
	
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Assignment Extension Self-Granter</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
		* html body {
			padding:100px 0 50px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/md5.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/navigation/courseSidebarJS.jsp.h" %>
	
	function goBackAgain() {
		this.location.href = 'hwSetList.jsp';
	}

	function checkSubmit(e) {
		if (e && ([10, 13].contains(e.keyCode))) submitThis();
	} // checkSubmit()

	function submitThis() {
		var form = document.editform;
		var extensionStr = form.extension.value;
		if (!canParseToFloat(extensionStr) 
				|| parseToFloat(extensionStr) < 0
				|| parseToFloat(extensionStr) > <%= maxAllowedExtension %>) {
			toAlert('<%= user.translateJS("Please enter a nonnegative number "
					+ "no greater than the allowed maximum extension.") %>');
			return;
		} // if extension is illegal
		form.action = 'saveSelfExtension.jsp';
		form.submit(); 
	} // submitThis()

	// -->
	</script>
</head>

<body style="text-align:center; background-color:#FFFFFF;"
		onload="setTab('<%= toTabName(user.translateJS("Assignments")) %>');">

	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

<div id="contentsWithTabsWithFooter">

<table class="regtext" style="margin-left:auto; margin-right:auto;
		border-style:none; border-collapse:collapse;
		width:626px;">
	<tr><td class="boldtext big" style="padding-top:10px; padding-bottom:10px;">
		<%= user.translate("Grant Yourself an Extension on Assignment ***1***", hwNum) %>
	</td>
	</tr>
	<tr><td>
		<form name="editform" action="" method="post">
		<input type="hidden" name="hwNum" value="<%= hwNum %>" />
		<table class="whitetable" style="width:100%;">
		<tr>
		<td class="regtext" style="padding-left:40px;">
			<%= user.translate("Extension") %>:</td>
		<td class="regtext">	
			<input type="text" name="extension" size="8" 
					onkeypress="checkSubmit(event);"
					value="<%= currentExtensionStr %>" />
			<%= user.translate("days") %>
		</td>
		<td class="regtext" style="padding-left:40px;">	
			(<%= user.translate("maximum") %>
			<%= maxAllowedExtension %> <%= user.translate("days") %>)
		</td>
		</tr>
		</table>
		</form>
	</td></tr>
</table>
</div>
<div id="footer">
<center>
<table>
	<tr><td class="regtext" style="padding-left:40px; 
			padding-bottom:10px; text-align:center;">
		<table style="text-align:center; margin-top:10px;">
			<tr><td>
				<%= makeButton(user.translate("Submit"), "submitThis();") %> 
			</td><td style="padding-left:20px;">
				<%= makeButton(user.translate("Cancel"), "goBackAgain();") %>
			</td></tr>
		</table>
	</td></tr>
</table></center>
</form>
</div>
</body>
</html>
