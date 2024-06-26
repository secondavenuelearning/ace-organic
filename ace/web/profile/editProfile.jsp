<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.chem.MolString,
	com.epoch.courseware.Institution,
	com.epoch.courseware.Name,
	com.epoch.session.AnonSession,
	com.epoch.utils.Utils,
	java.util.Arrays"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%	final String pathToRoot = "../"; 
	request.setCharacterEncoding("UTF-8");

	/* parameters 
			editindex - number if the user is edited by the admin
						 0  if the user is added by the admin
						null, if an ordinary user is editing his profile
	*/

	User editUser = user;
	boolean adminEditMode = false;
	boolean adminAddMode = false;
	final String editIndex = request.getParameter("editindex");
	if (editIndex != null) {
		adminEditMode = true;
		final int index = Integer.parseInt(editIndex);
		if (index == 0) {
			// create an editUser
			editUser = new User();
			adminAddMode = true;
		} else {
			editUser = ((AdminSession) userSess).getUser(index);
		} // if creating a new user
	} // if the admin is editing a user
	final Name editName = editUser.getName();
	final boolean pwdInACE = editUser.getPasswordStoredInACE();
	final Institution[] institutions = (editUser.getRole() == User.STUDENT 
			? AnonSession.getVerifiedInstitutions()
			: AnonSession.getAllInstitutions());
	int defaultInstnId = -1;
	for (final Institution institution : institutions) {
		if (institution.getName().equals(AppConfig.defaultInstitution)) {
			defaultInstnId = institution.getId();
			break;
		} // if this institution is the default one
	} // for each institution
	synchronized (session) {
		session.setAttribute("institutions", institutions);
	}

	user.refreshLanguages();
	editUser.refreshLanguages();
	final String[] userLangs = user.getLanguages();
	final String[] editUserLangs = editUser.getLanguages();
	String[] secQsAndAnswers = editUser.getSecurityAnswers();
	if (Utils.isEmpty(secQsAndAnswers)) {
		secQsAndAnswers = new String[] {"0:1", "", ""};
	} // if there are no security answers
	final int[] secQNums = Utils.stringToIntArray(secQsAndAnswers[0].split(":"));

	final String goBack = request.getParameter("goBack");
	final String OTHER = "[" + user.translate("Other institution") + "]";
	final String SELECTED = "selected=\"selected\"";
	final String CHECKED = "checked=\"checked\"";
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
	<title>ACE Profile Management</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<style type="text/css">
		* html body {
			padding:50px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/md5.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

	var studentNumLabels = new Array();
	var instnIds = new Array();
	<% for (final Institution instn : institutions) { %>
		studentNumLabels[<%= instn.getId() %>] = 
				'<%= Utils.toValidJS(Utils.toValidTextbox(
					instn.getStudentNumLabel())) %>';
		instnIds['<%= Utils.toValidJS(instn.getName()) %>'] = <%= instn.getId() %>;
	<% } // for each institution %>

	function goBackAgain() {
		self.location.href = '<%= Utils.toValidJS(goBack) %>';
	} // goBackAgain()

	function changeRole() {
		var form = document.editform;
		var instSelOpts = form.institutionSelector.options;
		var len = instSelOpts.length;
		if (form.role.value === '<%= User.STUDENT %>') {
			if (document.getElementById('verified1')) {
				clearInnerHTML('verified1');
				clearInnerHTML('verified2');
			}
			setInnerHTML('studentId1', 
					studentNumLabels[instnIds['<%= editUser.getInstitutionName() %>']] + ':');
			setInnerHTML('studentId2',
					'<input type="text" name="studentnum" size="20" '
					+ 'value="<%= Utils.toValidJS(Utils.toValidTextbox(
							editUser.getStudentNum())) %>" \/>');
			clearInnerHTML('address1');
			setInnerHTML('address2', '<input type="hidden" name="address" value="" \/>');
			clearInnerHTML('phone1');
			setInnerHTML('phone2', '<input type="hidden" name="phone" value="" \/>');
			if (instSelOpts[len - 1].value === '<%= OTHER %>')
				instSelOpts.length -= 1;
		} else { // instructor or admin
			if (form.role.value === '<%= User.INSTRUCTOR %>' 
					&& document.getElementById('verified1')) {
				setInnerHTML('verified1', 'Verified?');
				setInnerHTML('verified2', '<input type="checkbox" name="enabled" '
						+ 'value="Yes" <%= editUser.isEnabled() ? CHECKED : "" %> \/>');
			} else {
				setInnerHTML('verified2', 
						'<input type="hidden" name="enabled" value="Yes" \/>');
			}
			setInnerHTML('studentId1', 
					'<%= user.translateJS("Name for \"Student ID number\"") %>:');
			var chosenInstn = form.institutionSelector.value;
			setInnerHTML('studentId2', 
					'<input type="text" name="studentNumLabel" size="40" value="'
						+ (chosenInstn == '<%= OTHER %>'
							? '<%= Utils.toValidJS(Utils.toValidTextbox(
								user.translate("Student ID number"))) %>'
							: studentNumLabels[parseInt(chosenInstn)])
						+ '"\/>');
			setInnerHTML('address1', '<%= user.translateJS("Address") %>:');
			setInnerHTML('address2', '<textarea name="address" cols="50" rows="5"><%= 
							Utils.toValidJS(Utils.toValidTextbox(
								editUser.getAddress())) %><\/textarea>');
			setInnerHTML('phone1', '<%= user.translateJS("Phone") %>:');
			setInnerHTML('phone2', '<input type="text" name="phone" size="20" '
					+ 'value="<%= Utils.toValidJS(Utils.toValidTextbox(
							editUser.getPhone())) %>" \/>');
			instSelOpts.length++;
			instSelOpts[len].value = '<%= OTHER %>';
			instSelOpts[len].text = '<%= OTHER %>';
		}
	} // changeRole()

	function setSavePasswd() {
		if (getChecked('savepasswd')) {
			setInnerHTML('pwdCell1', '<%= user.translateJS("New password") %>:');
			setInnerHTML('pwdCell2', '<input type="password" id="passphrase_entry" '
					+ 'name="passphrase_entry" size="20" value="" />');
			setInnerHTML('pwdCell3', '<%= user.translateJS("Confirm password") %>:');
			setInnerHTML('pwdCell4', '<input type="password" id="passphrase_confirm" '
					+ 'name="passphrase_confirm" size="20" value="" />');
		} else {
			for (var num = 1; num <= 4; num++) { // <!-- >
				clearInnerHTML('pwdCell' + num);
			}
		}
	} // setSavePasswd()

	function changeInstitution() {
		var form = document.editform;
		var chosenInstn = form.institutionSelector.value;
		if (chosenInstn === '<%= OTHER %>') {
			setInnerHTML('otherInst1', '<%= user.translateJS("Other institution") %>:');
			setInnerHTML('otherInst2', '<input type="text" id="other_institution" '
					+ 'name="other_institution" size="40" value="" />'); 
			// won't be a student, so no need to change studentId text
			setInnerHTML('studentId2', 
					'<input type="text" name="studentNumLabel" size="40" '
					+ 'value="<%= Utils.toValidJS(Utils.toValidTextbox(
						user.translate("Student ID number"))) %>"\/>');
		} else {
			for (var num = 1; num <= 2; num++) { // <!-- >
				clearInnerHTML('otherInst' + num);
			}
			if (form.role.value === '<%= User.STUDENT %>') {
				setInnerHTML('studentId1', 
						studentNumLabels[parseInt(chosenInstn)] + ':');
				setInnerHTML('studentId2', 
						'<input type="text" name="studentnum" '
						+ 'size="20" value="<%= Utils.toValidJS(
								Utils.toValidTextbox(
									editUser.getStudentNum())) %>" \/>');
			} else {
				setInnerHTML('studentId2', 
						'<input type="text" name="studentNumLabel" size="40" value="'
						+ studentNumLabels[parseInt(chosenInstn)] + '"\/>');
			} // if a student
		} // if has chosen "other" institution
		if (<%= pwdInACE %> || chosenInstn !== '<%= defaultInstnId %>' || 
				<%= adminEditMode || adminAddMode %>) {
			var bld = new String.builder().append(
					'<table><tr><td class="regtext" '
						+ 'style="padding-left:40px; vertical-align:middle">'
						+ '<%= user.translateJS("Email") %>:'
					+ '<\/td><td colspan="2">'
						+ '<input type="text" name="email" size="40" value="<%= 
							editUser.getEmail() %>" \/>'
					+ '<\/td><\/tr>');
			<% if (editUser.mayChangePwd() || adminEditMode || adminAddMode) { %>
				bld.append('<tr><td class="regtext" '
							+ 'style="padding-left:40px; vertical-align:middle">'
							+ '<%= user.translateJS("Change password?") %>'
						+ '<\/td><td colspan="2">'
							+ '<input type="checkbox" id="savepasswd" name="savepasswd"'
							+ ' onclick="setSavePasswd()" \/>');
				<% if (!pwdInACE) { %>
					bld.append('&nbsp;&nbsp;<%= user.translateJS(
							"[currently using linkBlue]") %>');
				<% } // if user's password is not stored in ACE %>
				bld.append('<\/td><\/tr>');
			<% } // if user may change pwd or admin is adding/editing %>
			bld.append('<tr>'
					+ '<td id="pwdCell1" class="regtext" '
						+ 'style="padding-left:40px; vertical-align:middle">'
					+ '<\/td><td id="pwdCell2" colspan="2">'
					+ '<\/td><\/tr>'
					+ '<tr><td id="pwdCell3" class="regtext" '
						+ 'style="padding-left:40px; vertical-align:middle">'
					+ '<\/td><td id="pwdCell4" colspan="2">'
					+ '<\/td><\/tr>');
			<% if (!adminEditMode && !adminAddMode && !editUser.isExamStudent()) {
				int qNum = 0; %>
				bld.append('<tr><td class="regtext" style="padding-left:40px;">'
							+ '<%= user.translateJS("Choose a security question") %>:'
						+ '<\/td><td colspan="2" id="secQ1Cell">'
						+ '<select name="secQ1" id="secQ1" onchange="changeSecQ(1);">');
				<% for (final String secQ : User.SECURITY_QS) { %>
					bld.append('<option value="<%= qNum %>" <%= 
							qNum == secQNums[0] ? SELECTED : "" %>><%= 
							user.translateJS(secQ) %><\/option>');
				<% 	qNum++;
				} // for each security question %>
				bld.append('<\/select><\/td><\/tr>'
						+ '<tr><td class="regtext" style="padding-left:40px;"><%=
							user.translateJS("Answer the question you chose") %>:'
						+ '<\/td>'
						+ '<td colspan="2">'
							+ '<input type="text" name="secAns1" size="20" value="<%= 
								Utils.toValidHTMLAttributeValue(secQsAndAnswers[1]) %>"\/>'
						+ '<\/td><\/tr>'
						+ '<tr><td class="regtext" style="padding-left:40px;"><%= 
							user.translateJS("Choose another security question") %>:'
						+ '<\/td>'
						+ '<td colspan="2" id="secQ2Cell">'
							+ '<select name="secQ2" id="secQ2" onchange="changeSecQ(2);">');
				<% qNum = 0;
				for (final String secQ : User.SECURITY_QS) { %>
					bld.append('<option value="<%= qNum %>" <%= 
							qNum == secQNums[1] ? SELECTED : "" %>><%= 
								user.translateJS(secQ) %><\/option>');
				<% 	qNum++;
				} // for each security question %>
				bld.append('<\/select><\/td><\/tr>'
						+ '<tr><td class="regtext" style="padding-left:40px;"><%=
							user.translateJS("Answer the second question you chose") %>:'
						+ '<\/td><td colspan="2">'
						+ '<input type="text" name="secAns2" size="20" value="<%=
							Utils.toValidHTMLAttributeValue(secQsAndAnswers[2]) %>"\/>'
						+ '<\/td><\/tr>');
			<% } // if user editing himself and not an exam student %>
			bld.append('<\/table>');
			setInnerHTML('notAtDefaultInstitution', bld.toString());
		} else {
			clearInnerHTML('notAtDefaultInstitution');
		} // if other institution
	} // changeInstitution()

	function submitIt(form) {
		// checking for addmode
		<% if (adminAddMode) { %>
			if (form.userId.value == null || isWhiteSpace(form.userId.value)) {
				toAlert('<%= user.translate("You must enter a userId.") %>');
				return;
			} // if no userId
		<% } // if adding a new user %>
		// check if needed values are entered 
		if (isWhiteSpace(form.givenName.value)) {
			toAlert('<%= user.translate("You must enter a first name.") %>');
			return;
		} // if no given name
		if (form.role.value === '<%= User.STUDENT %>' 
				 && isWhiteSpace(form.studentnum.value)) {
			toAlert('<%= user.translate("You must enter a valid student ID "
					+ "number.") %>');
			return;
		} // if no student ID number
		var instnSel = form.institutionSelector;
		form.institution.value = instnSel.value;
		if (form.institution.value === '<%= OTHER %>') { // instructors only
			if (isWhiteSpace(form.other_institution.value)) {
				toAlert('<%= user.translateJS("Please enter an institution name "
						+ "if you choose ***other*** from the menu.", OTHER) %>');
				return;
			} // if need an institution name
			form.institution.value = form.other_institution.value;
		} // if a new institution
		var chosenInstn = instnSel.options[instnSel.selectedIndex].value;
		if (chosenInstn !== '<%= defaultInstnId %>' || <%= pwdInACE %> ||
				<%= adminEditMode || adminAddMode %>) {
			if (form.savepasswd.checked && isWhiteSpace(form.passphrase_entry.value)) {
				toAlert('<%= user.translate("You must enter a password.") %>');
				return;
			} // if no password
			// check if both passwords match
			if (form.savepasswd.checked 
					&& form.passphrase_entry.value !== form.passphrase_confirm.value) { 
				toAlert('<%= user.translate("The passwords you entered don\\'t "
						+ "match.") %>');
				return;
			} // if passwords don't match
			// replace password with md5 hash
			if (form.savepasswd.checked) {
				form.passphrase.value = b64_md5(form.passphrase_entry.value);
				form.storeNewPwd.value = 'true';
			} else {
				form.storeNewPwd.value = 'false';
			} // if should save the password
			if (isWhiteSpace(form.email.value) || !isValidEmail(form.email.value)) {
				toAlert('<%= user.translateJS("You must enter a valid email address.") %>');
				return;
			} // if no email address entered
			<% if (!adminEditMode && !adminAddMode && !editUser.isExamStudent()) { %>
				if (form.savepasswd.checked 
			 			&& (isWhiteSpace(form.secAns1.value) 
							|| isWhiteSpace(form.secAns2.value))) {
		 			toAlert('<%= user.translateJS(
							"You must answer both security questions.") %>');
					return;
				} // if a security question answer is missing
			<% } // if not administrator or exam student making the changes %>
		} // if has not chosen default institution
		form.action = 'saveProfile.jsp';
		form.submit();
	} // submitIt()

	function editLanguages() {
		var go = new String.builder().append('editLanguages.jsp?goBack=').
				append(encodeURIComponent('<%= Utils.toValidJS(goBack) %>'));
		<% if (editIndex != null) { %>
			go.append('&editindex=<%= editIndex %>');
		<% } %>
		document.location.href = go.toString();
	} // editLanguages()

	function changeSecQ(chgdSelectorNum) {
		var otherSelectorNum = 3 - chgdSelectorNum;
		var chgdSelector = document.getElementById('secQ' + chgdSelectorNum);
		var otherSelector = document.getElementById('secQ' + otherSelectorNum);
		if (chgdSelector && otherSelector) {
			var chgdSelectorQNum = parseInt(chgdSelector.value);
			var otherSelectorQNum = parseInt(otherSelector.value);
			var bld = new String.builder().append('<select name="secQ').
					append(otherSelectorNum).append('" id="secQ').
					append(otherSelectorNum).
					append('" onchange="changeSecQ(').
					append(otherSelectorNum).append(');">');
			var qNum = 0;
			<% for (final String secQ : User.SECURITY_QS) { %>
				if (qNum !== chgdSelectorQNum) {
					bld.append('<option value="').append(qNum).append('"');
					if (qNum === otherSelectorQNum) bld.append(' <%= SELECTED %>');
					bld.append('><%= user.translateJS(secQ) %><\/option>');
				} // if Q hasn't already been selected
				qNum++;
			<% } // for each security question %>
			bld.append('<\/select>');
			setInnerHTML('secQ' + otherSelectorNum + 'Cell', bld.toString());
		} // if there are selectors
	} // changeSecQ()

	// -->
	</script>
</head>
<body class="light" style="background-color:white;" 
		onload="changeRole(); changeInstitution(); changeSecQ(1); changeSecQ(2)">
<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
<div id="contentsWithoutTabs">

<table style="width:626px; text-align:center; margin-left:auto; 
		margin-right:auto;" summary="">
	<tr>
	<td class="boldtext big" style="vertical-align:top; padding-top:10px;">
		<%= user.translate("Edit profile") %>
	</td>
	</tr>
	<tr>
	<td style="vertical-align:top; text-align:center;">
		<form name="editform" action="javascript:submitIt(document.editform)" 
				method="post" accept-charset="UTF-8"> <div>
		<input type="hidden" name="storeNewPwd" value=""/>
		<input type="hidden" name="passphrase" value=""/>
		<input type="hidden" name="institution" value=""/>
		<input type="hidden" name="flags" value="<%= editUser.getFlags() %>"/>
		<input type="hidden" name="goBack" 
				value="<%= Utils.toValidHTMLAttributeValue(goBack) %>"/>
		<% if (editIndex != null) { %>
			<input type="hidden" name="editindex" value="<%= editIndex %>"/>
		<% } %>
		<table class="whiteTable" style="width:626px; background-color:#f6f7ed;
				text-align:left;" summary="">
			<tr>
			<td class="regtext" style="padding-left:40px; padding-top:10px; 
					vertical-align:middle;">
				<%= user.translate("Login name") %>:
			</td>
			<td colspan="2" style="padding-top:10px;">
				<% if (adminAddMode) { %>
					<input type="text" name="userId" size="40"
						value=""/>
				<% } else { 
					final String userId = editUser.getUserId(); %>
					<input type="hidden" name="userId" 
							value="<%= Utils.toValidHTMLAttributeValue(userId) %>"/>
					<%= userId %>
				<% } %>
			</td>
			</tr>
			<tr>
			<td class="regtext" style="padding-left:40px;">
				<% if (Utils.isEmpty(userLangs)) { %>
					 First (given) name
				<% } else { %>
					<%= user.translate("Given name") %>:
				<% } // if English-speaking %>
			</td>
			<td colspan="2">
				<table style="position:relative; left:-1px; 
						border-collapse:collapse; padding:0px;
						border-width:0px;" summary="">
					<tr>
					<td>
						<input type="text" name="givenName" size="40" 
								value="<%= Utils.toValidTextbox(
									editName.givenName) %>" />
					</td>
					<td class="regtext">
						&nbsp;&nbsp;&nbsp;<%= user.translate("M.I.") %>:
					</td>
					<td>
						<input type="text" name="middleName" size="1"
								value="<%= Utils.toValidTextbox(
									editName.middleName) %>" />
					</td>
					</tr>
				</table>
			</td>
			</tr>
			<tr>
			<td class="regtext" style="padding-left:40px; vertical-align:middle">
				<% if (Utils.isEmpty(userLangs)) { %>
					 Last (family) name
				<% } else { %>
					<%= user.translate("Family name") %>:
				<% } // if English-speaking %>
			</td>
			<td colspan="2">
				<input type="text" name="familyName" size="40" 
						value="<%= Utils.toValidTextbox(editName.familyName) %>" />
			</td>
			</tr>
			<tr><td class="regtext" style="padding-left:40px;">
			</td>
			<td colspan="2">
				<input type="checkbox" name="familyName1st"
						<%= editUser.prefersFamilyName1st() ? CHECKED : "" %> />
				<%= user.translate("I prefer my family name to come "
						+ "before my given name.") %>
			</td></tr>
			<tr><td class="regtext" style="padding-left:40px;">
			</td>
			<td colspan="2">
				<input type="checkbox" name="day1st"
						<%= editUser.prefersDay1st() ? CHECKED : "" %> />
				<%= user.translate("I prefer dates in the format day-month-year "
						+ "instead of month-day-year.") %>
			</td></tr>
			<tr>
			<td class="regtext" style="padding-left:40px; vertical-align:middle">
				<%= user.translate("Institution") %>:
			</td>
			<td colspan="2">
				<select name="institutionSelector" id="institutionSelector" 
						onchange="changeInstitution();">
				<% final int editUserInstnId = editUser.getInstitutionId();
				for (final Institution instn : institutions) { 
					final int instnId = instn.getId(); %>
					<option value="<%= instnId %>" 
							<%= editUserInstnId == instnId ? SELECTED : "" %>>
						<%= instn.getName() %> </option>
				<% } // for each institution %>
				</select>
			</td>
			</tr>
			<tr>
			<td id="otherInst1" class="regtext" 
					style="padding-left:40px; vertical-align:middle">
			</td>
			<td id="otherInst2" colspan="2" >
			</td>
			</tr>
			<tr><td colspan="3">
				<span id="notAtDefaultInstitution"></span>
			</td></tr>
			<% final char editUserRole = editUser.getRole();
			if (adminEditMode) { %>
				<tr>
				<td class="regtext" 
						style="padding-left:40px; vertical-align:middle">
					Role:
				</td>
				<td colspan="2">
					<% if (editUserRole == User.ADMINISTRATOR) { %>
						<input type="hidden" name="role" value="A">
						Administrator
					<% } else { %>
						<select name="role" onchange="changeRole()">
							<option value="<%= User.INSTRUCTOR %>" 
									<%= editUserRole == User.INSTRUCTOR 
											? SELECTED : "" %> >Instructor
							</option>	
							<option value="<%= User.STUDENT %>" 
									<%= editUserRole == User.STUDENT 
											? SELECTED : "" %> >Student
							</option>	
						</select>
					<% } // if editUserRole %>
				</td>
				</tr>
				<tr>
				<td id="verified1" class="regtext" 
						style="padding-left:40px; vertical-align:top">
				</td>
				<td id="verified2">
				</td>
				</tr>
			<% } else { %>
				<tr><td>
				  <input type="hidden" name="enabled" value=<%= editUser.isEnabled() 
						? "\"Yes\"" : "\"No\"" %> />
				  <input type="hidden" name="role" value="<%= editUserRole %>" />
				</td></tr>
			<% } // if adminEditMode %>
			<tr>
			<td id="studentId1" class="regtext" 
					style="padding-left:40px; vertical-align:middle">
			</td>
			<td id="studentId2"  colspan="2">
			</td>
			</tr>
			<tr>
			<td class="regtext" 
					style="padding-left:40px; vertical-align:middle">
				<%= user.translate("Languages") %>:
			</td>
			<td colspan="2">
				<table summary=""><tr><td>
					<% if (Utils.getLength(editUserLangs) == 0) { %>
						English only
					<% } else {
						String langStr = Arrays.toString(editUserLangs);
						langStr = Utils.endsChop(langStr, 1, 1); %>
						<%= Utils.capitalize(langStr) %>, English
					<% } // if there are languages %>
				</td>
				<td style="padding-left:10px;">
					<% if (!adminAddMode) { %>
						<%= makeButtonIcon("edit", pathToRoot, "editLanguages();") %>
					<% } // if not adding a new user %>
				</td></tr></table>
			</td>
			</tr>
			<tr>
			<td class="regtext"
				style="padding-left:40px; vertical-align:middle">
				<%= user.translate("Use PNG graphics instead of SVG?") %>
			</td>
			<td>
				<input type="checkbox" id="prefersPNG" name="prefersPNG" 
						<%= editUser.prefersPNG() ? CHECKED : "" %> />
			</td>
			<td>
				<table>
				<tr><td>
				<%= Utils.toDisplay(user.translate("Check the box if the current browser "
						+ "doesn't display the H2O molecule:")) %>
				</td><td>
				<%= MolString.getImage(pathToRoot, "[H]O[H]") %>
				</td></tr>
				</table>
			</td>
			</tr>
			<tr>
			<td id="address1" class="regtext" 
					style="padding-left:40px; vertical-align:top">
			</td>
			<td id="address2" colspan="2">
			</td>
			</tr>
			<tr>
			<td id="phone1" class="regtext" 
					style="padding-left:40px; vertical-align:middle">
			</td>
			<td id="phone2" colspan="2">
			</td>
			</tr>
			<tr><td class="regtext" style="padding-left:40px;">
				<%= user.translate("Email address to use to send "
						+ "text messages to your phone") %>:
			</td>
			<td colspan="2">
				<input type="text" name="textMessageEmail" size="40" 
						value="<%= Utils.toValidTextbox(
							editUser.getTextMessageEmail()) %>"/>
			</td></tr>
			<tr>
			<td style="padding-bottom:10px; padding-top:10px; padding-left:40px;">
				<%= makeButton(user.translate("Save changes"), 
						"submitIt(document.editform);") %>
			</td>
			<td style="padding-bottom:10px; padding-top:10px;">
				<%= makeButton(user.translate("Cancel"), "goBackAgain();") %>
			</td>
			</tr>
		</table>
		</div></form>
	</td>
	</tr>
</table>
</div>
</body>
</html>
