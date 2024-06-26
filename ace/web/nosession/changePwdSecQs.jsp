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
	final User tempUser = new User();
	String chosenLang = request.getParameter("language");
	if (chosenLang == null && !"English".equals(AppConfig.defaultLanguage)) {
		chosenLang = AppConfig.defaultLanguage;
	}
	tempUser.setLanguage(chosenLang);

	final String userId = request.getParameter("username");
	final String institutionId = request.getParameter("institutionId");
	final String ukId = request.getParameter("ukId");
	final boolean isUK = institutionId.equals(ukId);
	final User user = AnonSession.getUser(userId);
	final String[] secQsAndAnswers = 
			(user == null ? null : user.getSecurityAnswers());

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
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >
	function back() {
		this.location.href = 'getUsername.jsp?language=<%= Utils.toValidURI(chosenLang) %>';
	}

	function cancel() {
		this.location.href = '../login.jsp?language=<%= 
				Utils.toValidURI(chosenLang) %>';
	}
	// -->
	</script>
</head>
<body class="light">
	<% if (user == null) { %>
		<script type="text/javascript">
			toAlert('<%= tempUser.translateJS("ACE cannot find a user with the "
					+ "username ***username***.", userId) %>');
			back();
		</script>
	<% } else if (Utils.isEmpty(secQsAndAnswers)) { %> 
		<script type="text/javascript">
			<% if (isUK) { %>
				toAlert('<%= tempUser.translateJS(
							"ACE does not have any security " + 
							"questions on file for ***username***, so you " + 
							"are using your ***linkBlue*** credentials to log " +
							"into ACE. Your ***linkBlue*** password has " +
							"probably expired. " +
							"Visit ***https://ukam.uky.edu*** to change it.",
						new String[] {
							userId,
							AppConfig.defaultUsernameLabel, 
							AppConfig.defaultUsernameLabel, 
							"https://ukam.uky.edu"}) %>');
			<% } else { %>
				toAlert('<%= tempUser.translateJS(
						Utils.toString(
							"ACE does not have any security " + 
							"questions on file for ***username***. Ask ",
							user.getRole() == User.INSTRUCTOR
								? "the ACE administrator"
								: "your instructor",
							" to change your password."), 
						 "new String[] {userId}) %>');
			<% } // if UK %>
			cancel();
		</script>
	<% } else { 
		final int[] qNums = Utils.stringToIntArray(secQsAndAnswers[0].split(":"));
	%>
	<table style="width:100%;">
		<tr><td style="text-align:center;">
			<img src="<%= pathToRoot %>images/acelogo.jpg" alt="logo"/>
		</td></tr>
		<tr><td style="vertical-align:top;">
			<form name="resetPwdForm" action="getNewPwd.jsp" method="post">
			<input type="hidden" name="language" 
					value="<%= Utils.toValidHTMLAttributeValue(chosenLang) %>"/>
			<input type="hidden" name="userId" value="<%= userId %>"/>
			<table class="whiteTable" summary=""
					style="width:626px; padding-left:10px; padding-bottom:10px; 
						margin-left:auto; margin-right:auto;">
				<tr>
				<td class="boldtext big">
					<%= tempUser.translate("Reset your password") %>
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<%= tempUser.translate(User.SECURITY_QS[qNums[0]]) %> 
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<input type="text" name="secAns1" value="" size="16" />
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<%= tempUser.translate(User.SECURITY_QS[qNums[1]]) %> 
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<input type="text" name="secAns2" value="" size="16" />
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<table><tr><td>
					<%= makeButton(tempUser.translate("Continue"), 
							"document.resetPwdForm.submit();") %>
					</td><td>
					<%= makeButton(tempUser.translate("Cancel"), "cancel();") %>
					</td></tr></table>
				</td>
				</tr>
			</table>
			</form>
		</td></tr>
	</table>
	<% } // if username is valid %>
</body>
</html>
