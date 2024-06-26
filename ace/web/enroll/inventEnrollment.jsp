<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.db.UserRead,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%	
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}

	final int numStudents = MathUtils.parseInt(request.getParameter("numStudents"));
	final boolean pwdsSame = "true".equals(request.getParameter("pwdsSame"));
	final String[] randNames = new String[numStudents];
	final String[] randPwds = new String[numStudents];
	for (int studNum = 0; studNum < numStudents; studNum++) {
		while (true) {
			final String rand = Utils.getRandName(8, Utils.EXCLUDE1IlO0);
			if (!UserRead.loginExists(rand)
					&& !UserRead.studentNumExists(rand, user.getInstitutionId())) {
				randNames[studNum] = rand;
				randPwds[studNum] = (pwdsSame ? rand 
						: Utils.getRandName(8, Utils.EXCLUDE1IlO0));
				break;
			}
		} // while have not added the user successfuly
	} // for each invented student
	final String expireInfo = user.translateJS(
			"The login IDs will expire in ***2*** weeks.", 
			AppConfig.EXAM_STUDENT_LIFE_WKS); 

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache">
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT">
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Student Creation and Enrollment</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<style type="text/css">
		* html body {
			padding:50px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/md5.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/navigation/courseSidebarJS.jsp.h" %>

	function setValues() {
		var namesBld = new String.builder();
		var pwdsBld = new String.builder();
		<% for (int num = 0; num < randNames.length; num++) { %>
			<% if (num > 0) { %>
				namesBld.append('.');
				pwdsBld.append('.');
			<% } %>
			namesBld.append('<%= randNames[num] %>');
			pwdsBld.append(b64_md5('<%= randPwds[num] %>'));
		<% } // for each student %>
		document.inventForm.names.value = namesBld.toString();
		document.inventForm.passwds.value = pwdsBld.toString();
		<% if (pwdsSame) { %>
			toAlert('<%= user.translateJS("Each student's password "
					+ "will be the same as the login ID.") %> '
					+ '<%= expireInfo %>'); 
			continueMe();
		<% } // if pwdsSame %>
	} // setValues()

	function continueMe() {
		document.inventForm.submit();
	} // continueMe()
	// -->

	</script>
</head>

<body class="light" style="background-color:white; overflow:auto;" 
		onload="setTab('<%= toTabName(user.translateJS("Enrollment")) %>'); setValues();">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

	<form name="inventForm" action="inventFinish.jsp" method="post">
		<input type="hidden" name="names" value="" />
		<input type="hidden" name="passwds" value="" />
		<input type="hidden" name="pwdsSame" value="<%= pwdsSame %>" />
	<% if (!pwdsSame) { %>
		<div id="contentsWithTabsWithoutFooter">
		<table style="width:626px; text-align:center; 
				margin-left:auto; margin-right:auto;">
		<tr>
		<td class="boldtext big" style="vertical-align:top; padding-top:10px;">
			<%= user.translate("Exam usernames and passwords") %>
		</td>
		</tr>
		<tr>
		<td class="regtext" style="vertical-align:top; padding-top:10px;">
			<%= user.translate("Save this table so that you have a record "
					+ "of the passwords. ACE will not permit students using "
					+ "these accounts to change their passwords. (The "
					+ "ACE administrator will be able to change them, "
					+ "however.) Press <b>Continue</b> after you have "
					+ "saved the passwords.") %>
			<p><%= expireInfo %> 
			</p>
		</td>
		</tr>
		<tr>
		<td>
			<table class="regtext" summary="">
			<tr>
				<th><%= user.translate("Username") %></th>
				<th><%= user.translate("Password") %></th>
			</tr>
			<% for (int num = 0; num < randNames.length; num++) { %>
				<tr>
					<td><%= randNames[num] %></td>
					<td><%= randPwds[num] %></td>
				</tr>
			<% } // for each student %>
			</table>
		</td>
		<tr>
		</table>
		</div>
		<div id="footer">
		<table class="regtext" style="width:90%; margin-top:10px; margin-left:auto; 
				margin-right:auto; border-style:none; border-collapse:collapse;"
				summary="">
		<tr>
			<td style="width:100%; text-align:right;"></td>
			<td style="text-align:right; padding-left:10px;">
				<%= makeButton(user.translate("Continue"), "continueMe();") %>
			</td>
		</tr>
		</table>
		</div>
	<% } // if !pwdsSame %>
	</form>
</body>
</html>
