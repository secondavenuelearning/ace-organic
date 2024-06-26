<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.courseware.Institution,
	com.epoch.courseware.User,
	com.epoch.session.AnonSession,
	com.epoch.utils.Utils"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%
	final String pathToRoot = "../";
	final User tempUser = new User();
	String chosenLang = request.getParameter("language");
	if (chosenLang == null && !"English".equals(AppConfig.defaultLanguage)) {
		chosenLang = AppConfig.defaultLanguage;
	}
	tempUser.setLanguage(chosenLang);
	final Institution[] institutions = AnonSession.getVerifiedInstitutions();
	int defaultInstnId = -1;
	int ukId = -1;
	for (final Institution institution : institutions) {
		if (institution.getName().equals(AppConfig.defaultInstitution)) {
			defaultInstnId = institution.getId();
		} // if this institution is the default one
		if (institution.getName().equals("University of Kentucky")) {
			ukId = institution.getId();
		} // if this institution is the default one
	} // for each institution
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
		function checkSubmit(e) {
			if (e && (e.keyCode === 10 || e.keyCode === 13)) {
				document.resetPwdForm.submit();
			}
		} // checkSubmit()
		
		function goBack() {
			this.location.href = '<%= pathToRoot %>login.jsp?language=<%= 
					Utils.toValidURI(chosenLang) %>';
		}
	// -->
	</script>
</head>
<body class="light">
	<table style="width:100%;">
		<tr><td style="text-align:center;">
			<img src="<%= pathToRoot %>images/acelogo.jpg" alt="logo"/>
		</td></tr>
		<tr><td style="vertical-align:top;">
			<form name="resetPwdForm" action="changePwdSecQs.jsp" method="post">
			<input type="hidden" name="ukId" value="<%= ukId %>"/>
			<input type="hidden" name="language" 
					value="<%= Utils.toValidHTMLAttributeValue(chosenLang) %>"/>
			<table class="whiteTable" summary=""
					style="width:626px; padding-left:10px; padding-bottom:10px; 
						margin-left:auto; margin-right:auto;">
				<tr>
				<td class="boldtext big">
					<%= tempUser.translate("Reset your password") %>
				</td>
				</tr>
				<tr>
				<td style="text-align:left; padding-top:10px;">
					<%= tempUser.translate("Institution") %>:
					<select name="institutionId">
						<% for (final Institution institution : institutions) { 
							final String instnName = institution.getName(); %>
							<option value="<%= institution.getId() %>"
									<%= defaultInstnId == institution.getId()
										? "selected=\"selected\"" : "" %> >
							<%= instnName %></option>
						<% } // for each institution %>
					</select>
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px; width:50%;">
					<%= tempUser.translate("Enter your username") %>: 
					<input type="text" name="username"
						onkeypress="checkSubmit(event);"
						value="" size="16" />
				</td>
				</tr>
				<tr>
				<td colspan="2">
					<table>
					<tr>
					<td class="regtext" style="padding-top:10px;">
						<%= makeButton(tempUser.translate("Continue"), 
								"document.resetPwdForm.submit();") %>
					</td>
					<td class="regtext" style="padding-top:10px;">
						<%= makeButton(tempUser.translate("Cancel"), 
								"goBack();") %>
					</td>
					</tr>
					</table>
				</td>
				</tr>
				<tr>
				<td colspan="3" class="regtext" style="padding-top:20px;">
					<%= tempUser.translate("If you do not know your username, "
							+ "ask your instructor to retrieve it for you. If "
							+ "you are an instructor, ask the ACE administrator.") %> 
				</td>
				</tr>
			</table>
			</form>
		</td></tr>
	</table>
</body>
</html>

