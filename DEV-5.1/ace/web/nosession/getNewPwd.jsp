<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.courseware.User,
	com.epoch.session.AnonSession,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>

<%	final String pathToRoot = "../";
	final String userId = request.getParameter("userId");
	// Utils.alwaysPrint("getNewPwd,jsp: userId = ", userId);
	final User user = AnonSession.getUser(userId);
	// if (user == null) Utils.alwaysPrint("getNewPwd.jsp: user is null");
	String chosenLang = request.getParameter("language");
	if (chosenLang == null && !"English".equals(AppConfig.defaultLanguage)) {
		chosenLang = AppConfig.defaultLanguage;
	}

	final String[] secAnswers = new String[]
			{Utils.inputToCERs(request.getParameter("secAns1")), 
			Utils.inputToCERs(request.getParameter("secAns2"))};
	final boolean match = (user == null ? false : user.matchSecurityAnswers(secAnswers));
	final String newPwd = (match ? Utils.getRandName(8, Utils.EXCLUDE1IlO0) : null);
	final String sorry = "Sorry, your responses do not match those ACE has on record.";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
	<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Password Reset</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<script src="<%= pathToRoot %>js/md5.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >
	function goNext() {
		document.resetPwdForm.password.value = b64_md5('<%= newPwd %>');
		document.resetPwdForm.submit();
	}

	function cancel() {
		this.location.href = '<%= pathToRoot %>login.jsp?language=<%= 
				Utils.toValidURI(chosenLang) %>';
	}
	// -->
	</script>
</head>
<body class="light">
	<% if (match && user != null) { %>
		<form name="resetPwdForm" action="setNewPwd.jsp" method="post">
		<input type="hidden" name="language" 
				value="<%= Utils.toValidHTMLAttributeValue(chosenLang) %>"/>
		<input type="hidden" name="userId" value="<%= userId %>"/>
		<input type="hidden" name="password" value=""/>
		</form>
		<table style="width:100%;">
			<tr><td style="text-align:center;">
				<img src="<%= pathToRoot %>images/acelogo.jpg" alt="logo"/>
			</td></tr>
			<tr><td style="vertical-align:top;">
				<table class="whiteTable" summary=""
						style="width:626px; padding-left:10px; padding-bottom:10px; 
							margin-left:auto; margin-right:auto;">
					<tr>
					<td class="boldtext big">
						<%= user.translate("New password") %>: <%= newPwd %>
					</td>
					</tr>
					<tr>
					<td class="regtext" style="padding-top:10px;">
						<%= user.translate("Copy your new password, or "
							+ "write it down! You can change it after you log "
							+ "in again: press My Profile.") %>
					</td>
					</tr>
					<tr>
					<td class="regtext" style="padding-top:10px;">
						<%= makeButton(user.translate("Continue"), "goNext();") %>
					</td>
					</tr>
				</table>
			</td>
			</tr>
		</table>
	<% } else { %> 
		<script type="text/javascript">
			toAlert('<%= Utils.toValidJS(user == null ? sorry : user.translateJS(sorry)) %>');
			cancel();
		</script>
	<% } // if the answers match %> 
</body>
</html>
