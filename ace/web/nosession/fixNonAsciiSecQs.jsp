<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.courseware.Institution,
	com.epoch.courseware.User,
	com.epoch.session.StudentSession,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>

<%	final String pathToRoot = "../";

	final String unicodeUserId = request.getParameter("userId").trim();
	final int instnId = MathUtils.parseInt(request.getParameter("instnId"));
	final String studentNum = request.getParameter("studentNum").trim();
	final String storedUserId = StudentSession.getUserId(instnId, studentNum);
	final char userRole = request.getParameter("userSelector").charAt(0);
	String chosenLang = request.getParameter("language");
	if (chosenLang == null && !"English".equals(AppConfig.defaultLanguage)) {
		chosenLang = AppConfig.defaultLanguage;
	}

	final User tempUser = new User(storedUserId);
	tempUser.setInstitution(new Institution(instnId));
	tempUser.setStudentNum(studentNum);
	tempUser.setLanguage(chosenLang);
	
	final boolean BY_INSTN_AND_STUDENTNUM = false;
	final String[] secQsAndAnswers = 
			tempUser.getSecurityAnswers(!User.BY_USER_ID);
	/*/ System.out.println("fixNonAsciiSecQs.jsp: storedUserId = " 
			+ storedUserId + ", unicodeUserId = " + unicodeUserId 
			+ ", instnId = " + instnId + ", studentNum = " + studentNum
			+ ", secQs = " + secQsAndAnswers[0]); /**/

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
	<title>ACE Username Rectification</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >
	function cancel() {
		this.location.href = '../login.jsp?language=<%= 
				Utils.toValidURI(chosenLang) %>';
	}

	function continue() {
		var form = document.rectifyUserIdForm;
		if (isWhiteSpace(form.secResp1.value) || isWhiteSpace(form.secResp2.value)) {
			alert('<%= tempUser.translateJS("Please answer both questions.") %>');
		} else {
			form.submit();
		} // if an answer is missing
	} // continue()

	// -->
	</script>
</head>
<body class="light">
	<% if (Utils.isEmpty(secQsAndAnswers)) { %> 
		<script type="text/javascript">
			toAlert('<%= tempUser.translateJS(
					Utils.toString(
						"ACE does not have any security " + 
					"questions on file for ***username***. Ask ",
						userRole == User.INSTRUCTOR
							? "the ACE administrator"
							: "your instructor",
						" to change your password."), 
					 new String[] {unicodeUserId}) %>');
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
			<form name="rectifyUserIdForm" action="fixNonAsciiUserId.jsp" method="post">
			<input type="hidden" name="language" 
					value="<%= Utils.toValidHTMLAttributeValue(chosenLang) %>"/>
			<input type="hidden" name="unicodeUserId" value="<%= unicodeUserId %>"/>
			<input type="hidden" name="storedUserId" value="<%= storedUserId %>"/>
			<input type="hidden" name="studentNum" value="<%= studentNum %>"/>
			<input type="hidden" name="instnId" value="<%= instnId %>"/>
			<table class="whiteTable" summary=""
					style="width:626px; padding-left:10px; padding-bottom:10px; 
						margin-left:auto; margin-right:auto;">
				<tr>
				<td class="boldtext big">
					<%= tempUser.translate("Verify your identity") %>
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<%= tempUser.translate(User.SECURITY_QS[qNums[0]]) %> 
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<input type="text" name="secResp1" value="" size="16" />
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<%= tempUser.translate(User.SECURITY_QS[qNums[1]]) %> 
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<input type="text" name="secResp2" value="" size="16" />
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<table><tr><td>
					<%= makeButton(tempUser.translate("Continue"), 
							"document.rectifyUserIdForm.submit();") %>
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
