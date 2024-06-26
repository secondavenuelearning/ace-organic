<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.courseware.Institution,
	com.epoch.courseware.User,
	com.epoch.session.AnonSession,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");

	final String userType = request.getParameter("userType");
	final boolean isStudent = Utils.isEmpty(userType) 
			|| userType.charAt(0) == User.STUDENT;

	final Institution[] institutions = (isStudent 
			? AnonSession.getVerifiedInstitutions()
			: AnonSession.getAllInstitutions());
	int defaultInstnId = -1;
	for (final Institution institution : institutions) {
		if (institution.getName().equals(AppConfig.defaultInstitution)) {
			defaultInstnId = institution.getId();
			break;
		} // if this institution is the default one
	} // for each institution
	int chosenInstnId = defaultInstnId;
	String instnIdStr = request.getParameter("instnId");
	if (instnIdStr != null) {
		chosenInstnId = MathUtils.parseInt(instnIdStr);
	} // if an institution has already been chosen
	/* Utils.alwaysPrint("register.jsp: instnIdStr = ", instnIdStr,
			", chosenInstnId = ", chosenInstnId,
			", defaultInstnId = ", defaultInstnId); /**/

	final User tempUser = new User();
	final String[] allLanguages = AnonSession.getAllLanguages();
	String chosenLang = request.getParameter("language");
	if (chosenLang == null) {
		if (instnIdStr != null) {
			for (final Institution institution : institutions) {
				if (institution.getId() == chosenInstnId) {
					chosenLang = institution.getPrimaryLanguage();
					break;
				} // if this institution is the default one
			} // for each institution
		} else if (AppConfig.notEnglish) {
			chosenLang = AppConfig.defaultLanguage;
		} // if instnIdStr or AppConfig.notEnglish
	} // if no chosen language
	tempUser.setLanguage(chosenLang);

	final String SELECTED = "selected=\"selected\"";
	final String OTHER = "Other";
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
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<title>ACE Username Rectification</title>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/md5.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >

	var studentNumLabels = new Array();
	var instnIds = new Array();
	var instnLangs = new Array();
	<% for (Institution instn : institutions) { %>
		studentNumLabels[<%= instn.getId() %>] = 
				'<%= Utils.toValidJS(Utils.toValidTextbox(
					instn.getStudentNumLabel())) %>';
		instnIds['<%= Utils.toValidJS(instn.getName()) %>'] = <%= instn.getId() %>;
		instnLangs[<%= instn.getId() %>] = 
				'<%= Utils.toValidJS(instn.getPrimaryLanguage()) %>';
	<% } // for each institution %>

	function goBackAgain() {
		self.location.href = '../login.jsp?language='
				+ encodeURIComponent(document.editform.language.value);
	}

	function reloadMe() {
		var form = document.editform;
		self.location.href = new String.builder().
				append('findNonAsciiUserId.jsp?userType=').
				append(form.userSelector.value).
				append('&language=').
				append(encodeURIComponent(form.language.value)).
				toString();
	} // reloadMe()

	function changeInstitution(doReload) {
		var bld = new String.builder();
		var form = document.editform;
		var instnSel = form.institutionSel;
		var chosenInst = instnSel.options[instnSel.selectedIndex].value;
		if (chosenInst !== '<%= OTHER %>' && 
				instnLangs[parseInt(chosenInst)] !== form.language.value && 
				doReload) {
			self.location.href = new String.builder().
					append('findNonAsciiUserId.jsp?userType=').
					append(form.userSelector.value).
					append('&instnId=').append(chosenInst).
					toString();
		} 
		<% if (isStudent) { %>
			setInnerHTML('studentIdName', studentNumLabels[parseInt(chosenInst)] + ':');
		<% } // if student %>
		if (chosenInst !== '<%= defaultInstnId %>') {
			setInnerHTML('usernameLabel',
					'<%= tempUser.translateJS("Enter your current username") %>:');
		} else {
			setInnerHTML('usernameLabel',
					'<%= tempUser.translateJS(Utils.toString("Enter your current ",
						AppConfig.defaultUsernameLabel, " username")) %>:');
			setInnerHTML('notAtDefaultInstitution', new String.builder().append(
					'<table><tr><td class="regtext" style="padding-left:40px; width:125px;">'
					+ '<%= AppConfig.defaultUsernameLabel %> '
					+ '<\/td><\/tr><\/table>'));
		} // if other institution
	} // changeInstitution()

	function submitIt() {
		var form = document.editform;
		var userId = form.userId.value;
		// checking for addmode
		if (userId == null || isWhiteSpace(userId)) {
 			toAlert('<%= tempUser.translateJS("You must enter a username.") %>');
 			return;
		}
		if (containsWhiteSpace(userId)) {
 			toAlert('<%= tempUser.translateJS("Whitespace (space, tab, etc.) "
					+ "is not allowed in the username.") %>');
  			return;
		}
		if (userId.length > 14) {
			toAlert('<%= tempUser.translateJS(
					"The username may contain no more than 14 characters.") %>');
 			return;
		}
		if (userId === 'admin') {
			toAlert('<%= tempUser.translateJS(
					"The username must not be \"admin\".") %>');
 			return;
		}
		var instnSel = form.institutionSel;
		form.instnId.value = instnSel.value;
		var chosenInst = instnSel.options[instnSel.selectedIndex].value;
		form.isDefaultInstn.value = chosenInst === '<%= defaultInstnId %>';
	<% if (isStudent) { %>
		if (isWhiteSpace(form.studentNum.value)) {
 				toAlert('<%= tempUser.translateJS(
					"You must enter a student ID number.") %>');
			return;
 			}
	<% } else { %>
		// validate selected institution
		if (form.instnId.value === '<%= OTHER %>') {
			if (form.other_institution.value.length < 1) { // >
				toAlert('<%= tempUser.translateJS("If you choose \""
						+ OTHER + "\" from the pulldown menu, you must "
						+ "enter an institution name.") %>');
				return;
			} else {
				form.instnId.value = form.other_institution.value;
			} // if another institution was entered
		} // if OTHER was selected
	<% } // if not student %>
	/*/ alert('User type = ' + form.userSelector.value
			+ ', form.instnId.value = ' + form.instnId.value
			+ ', form.userId.value = ' + form.userId.value
			+ ', form.studentNum.value = ' + form.studentNum.value);
	if (confirm('<%= tempUser.translateJS("Submit?") %>')) /**/ 
	form.submit();
} // submitIt()

// -->
	</script>
</head>
<body class="light" style="overflow:auto;" onload="changeInstitution(false);">
	<table style="width:100%;">
		<tr><td style="text-align:center;">
			  <img src="<%= pathToRoot %>images/acelogo.jpg" alt="logo"/>
		</td></tr>
		<tr><td style="vertical-align:top;">
			<form name="editform" method="post" action="fixNonAsciiSecQs.jsp" 
					accept-charset="UTF-8">
				<input type="hidden" name="instnId" value=""/>
				<input type="hidden" name="isDefaultInstn" value=""/>
				<input type="hidden" name="userType" value="<%= userType %>"/>
				<input type="hidden" name="language" 
						value="<%= Utils.toValidHTMLAttributeValue(chosenLang) %>"/>
			<table class="whiteTable" style="width:626px; text-align:left;">
				<tr><td class="regtext" style="padding-left:40px; padding-top:10px; 
						padding-bottom:20px;" colspan="2">
					<%= tempUser.translate("This page gathers information that "
							+ "will allow you to log in again. This exercise "
							+ "should be necessary only once.") %>
				</td>
				<tr><td class="regtext" style="padding-left:40px; width:33%;">
					<%= tempUser.translate("Select your user type") %>:
				</td>
				<td colspan="2">
					<select name="userSelector" onchange="reloadMe()">
						<option value="<%= User.STUDENT %>" <%= isStudent ? SELECTED : "" %>>
							<%= tempUser.translate("Student") %></option>
						<option value="<%= User.INSTRUCTOR %>" <%= isStudent ? "" : SELECTED %>>
							<%= tempUser.translate("Instructor") %></option>
					</select>
				</td></tr>
				<tr><td class="regtext" style="padding-left:40px;">
					<%= tempUser.translate("Institution") %>:
				</td>
				<td colspan="2">
					<select name="institutionSel" id="institutionSel"
							onchange="changeInstitution(true);">
						<% for (final Institution institution : institutions) { 
							final String instnName = institution.getName(); %>
							<option value="<%= institution.getId() %>"
							<%= instnName %></option>
						<% } // for each institution %>
						<% if (!isStudent) { %>
							<option value="<%= OTHER %>">
								<%= tempUser.translate(OTHER) %></option>
						<% } // if instructor %>
					</select>
				</td></tr>
				<tr><td id="usernameLabel" class="regtext" style="padding-left:40px;">
				</td>
				<td colspan="2">
					<input type="text" name="userId" size="20" value=""/>
				</td></tr>
				<% if (isStudent) { %>
					<tr><td id="studentIdName" class="regtext" style="padding-left:40px;">
						<%= tempUser.translate("Student ID number") %>:
					</td>
					<td colspan="2">
						<input type="text" name="studentNum" size="20" value=""/>
					</td></tr>
				<% } else { // instructor %>
					<tr><td class="regtext" style="padding-left:40px;" colspan="3">
						<br/>
					</td></tr>
				<% } // if student or instructor %>
				<tr><td style="padding-bottom:10px; padding-top:10px; padding-left:40px;">
					<%= makeButton(tempUser.translate("Submit"), "submitIt();") %>
				</td>
				<td style="width:100%; padding-bottom:10px; padding-top:10px;">
					<%= makeButton(tempUser.translate("Cancel"), "goBackAgain();") %>
				</td></tr>
			</table>
			</form>
		</td></tr>
	</table>
	</body>
</html>
