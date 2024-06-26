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
	final String instnIdStr = request.getParameter("instnId");
	if (instnIdStr != null) {
		chosenInstnId = MathUtils.parseInt(instnIdStr);
	} else {
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
<title>ACE Organic</title>
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
				append('register.jsp?userType=').
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
					append('register.jsp?userType=').
					append(form.userSelector.value).
					append('&instnId=').append(chosenInst).
					toString();
		} 
		<% if (isStudent) { %>
			setInnerHTML('studentIdName', studentNumLabels[parseInt(chosenInst)] + ':');
		<% } else { // instructor %>
			if (chosenInst === '<%= OTHER %>') {
				setInnerHTML('otherNameLabel', 
						'<%= tempUser.translateJS("Other institution") %>:');
				setInnerHTML('otherStudentIdNameLabel', 
						'<%= tempUser.translateJS("Name of \"student ID number\"") %>:');
				setInnerHTML('otherLanguageLabel', 
						'<%= tempUser.translateJS(
						"Institution's primary language of instruction") %>:');
				setInnerHTML('otherNameBox', '<table style="position:relative; left:-1px; '
						+ 'border-collapse:collapse; padding:0px; border-width:0px;">'
						+ '<tr><td>'
							+ '<input type="text" id="other_institution" '
							+ 'name="other_institution" size="40" value=""\/>'
						+ '<\/td><td class="regtext" style="padding-left:10px;">'
							+ '<%= tempUser.translateJS(
									"Enter the full name without abbreviations.") %>'
						+ '<\/td><\/tr>'
						+ '<\/table>');
				setInnerHTML('otherStudentIdNameBox', 
						'<input type="text" name="studentNumLabel" size="40" ' 
						+ 'value="<%= tempUser.translateJS(
							"Student ID number") %>"\/>');
				bld = new String.builder();
				bld.append('<select name="institutionLanguage" '
						+ 'onchange="openLanguageBox();">'
						+ '<option value="English">English<\/option>');
				<% for (final String lang : allLanguages) { %>
					bld.append('<option value="<%= Utils.toValidJS(
								Utils.toValidHTMLAttributeValue(lang)) %>" <%= 
							lang.equals(chosenLang) ? SELECTED : "" %>><%= 
							Utils.toValidJS(Utils.capitalize(lang)) %> <\/option>');
				<% } // for each language %>
				bld.append('<option value="other"><%=
						tempUser.translateJS(OTHER) %>:<\/option><\/select>');
				setInnerHTML('otherLanguageSelector', bld.toString());
				setInnerHTML('otherInstnHR', '<hr \/>');
			} else {
				clearInnerHTML('otherNameLabel');
				clearInnerHTML('otherNameBox');
				clearInnerHTML('otherStudentIdNameLabel');
				clearInnerHTML('otherStudentIdNameBox');
				clearInnerHTML('otherLanguageLabel');
				clearInnerHTML('otherLanguageSelector');
				clearInnerHTML('otherLanguageBox');
				clearInnerHTML('otherInstnHR');
			} // if other institution
		<% } // if student %>
		if (chosenInst !== '<%= defaultInstnId %>') {
			setInnerHTML('usernameLabel',
					'<%= tempUser.translateJS("Enter a username") %>:');
			bld = new String.builder().append(
					'<table><tr><td class="regtext" style="padding-left:40px;">'
					+ '<%= tempUser.translateJS("Password") %>:'
					+ '<\/td>'
					+ '<td colspan="2">'
						+ '<input type="password" name="passphrase_entry" size="20" value=""\/>'
						+ '<span style="color:red;">*<\/span>'
					+ '<\/td><\/tr>'
					+ '<tr><td class="regtext" style="padding-left:40px;">'
						+ '<%= tempUser.translateJS("Confirm password") %>:'
					+ '<\/td>'
					+ '<td colspan="2">'
						+ '<input type="password" name="passphrase_confirm" size="20" value=""\/>'
						+ '<span style="color:red;">*<\/span>'
					+ '<\/td><\/tr>'
					+ '<tr><td class="regtext" style="padding-left:40px;">'
						+ '<%= tempUser.translateJS("Choose a security question") %>:'
					+ '<\/td>'
					+ '<td colspan="2" id="secQ1Cell">'
						+ '<select name="secQ1" id="secQ1" onchange="changeSecQ(1);">');
			<% int qNum1 = 0;
			for (final String secQ : User.SECURITY_QS) { %>
				bld.append('<option value="<%= qNum1 %>"><%= 
								tempUser.translateJS(secQ) %><\/option>');
			<% qNum1++;
			} // for each security question %>
			bld.append('<\/select>'
					+ '<\/td><\/tr>'
					+ '<tr><td class="regtext" style="padding-left:40px;">'
						+ '<%= tempUser.translateJS("Answer") %>:'
					+ '<\/td>'
					+ '<td colspan="2">'
						+ '<input type="text" name="secAns1" size="20" value=""\/>'
						+ '<span style="color:red;">*<\/span>'
					+ '<\/td><\/tr>'
					+ '<tr><td class="regtext" style="padding-left:40px;">'
						+ '<%= tempUser.translateJS("Choose another security question") %>:'
					+ '<\/td>'
					+ '<td colspan="2" id="secQ2Cell">'
						+ '<select name="secQ2" id="secQ2" onchange="changeSecQ(2);">');
			<% int qNum2 = 0;
			for (final String secQ : User.SECURITY_QS) { %>
				bld.append('<option value="<%= qNum2 %>" <%= 
								qNum2 == 1 ? SELECTED : "" %>><%= 
								tempUser.translateJS(secQ) %><\/option>');
				<% qNum2++;
			} // for each security question %>
			bld.append('<\/select>'
					+ '<\/td><\/tr>'
					+ '<tr><td class="regtext" style="padding-left:40px;">'
						+ '<%= tempUser.translateJS("Answer") %>:'
					+ '<\/td>'
					+ '<td colspan="2">'
						+ '<input type="text" name="secAns2" size="20" value=""\/>'
						+ '<span style="color:red;">*<\/span>'
					+ '<\/td><\/tr>'
					+ '<tr><td class="regtext" style="padding-left:40px;">'
						+ '<%= tempUser.translateJS("Email") %>:'
					+ '<\/td>'
					+ '<td colspan="2">'
						+ '<input type="text" name="email" size="20" value=""\/>'
						+ '<span style="color:red;">*<\/span>'
					+ '<\/td><\/tr>'
					+ '<tr><td class="regtext" style="padding-left:40px;">'
						+ '<%= tempUser.translateJS("Confirm email") %>:'
					+ '<\/td>'
					+ '<td colspan="2">'
						+ '<input type="text" name="email_confirm" size="20" value=""\/>'
						+ '<span style="color:red;">*<\/span>'
					+ '<\/td><\/tr><\/table>');
			setInnerHTML('notAtDefaultInstitution', bld.toString());
		} else {
			setInnerHTML('usernameLabel',
					'<%= tempUser.translateJS(Utils.toString("Enter your ",
						AppConfig.defaultUsernameLabel, " username")) %>:');
			setInnerHTML('notAtDefaultInstitution', new String.builder().append(
					'<table><tr><td class="regtext" style="padding-left:40px; width:125px;">'
					+ '<%= AppConfig.defaultUsernameLabel %> '
					+ '<%= tempUser.translateJS("Password") %>:'
					+ '<\/td>'
					+ '<td>'
						+ '<input type="password" name="passphrase_entry" size="20" value=""\/>'
						+ '<span style="color:red;">*<\/span>'
					+ '<\/td><\/tr><\/table>'));
		} // if other institution
	} // changeInstitution()

	function openLanguageBox() {
		var form = document.editform;
		if (form.institutionLanguage.value === 'other') {
			setInnerHTML('otherLanguageBox', '<input type="text" '
					+ 'name="otherInstructionLanguage" size="40" value="" \/>');
			toAlert('<%= tempUser.translateJS("Write the name of the language as "
					+ "speakers of that language refer to it (e.g., fran&ccedil;ais, "
					+ "not French.) If the language uses the Roman alphabet, "
					+ "capitalize it as it would appear in the middle of a "
					+ "sentence in that language (e.g., espa&ntilde;ol, but "
					+ "Deutsch).") %>');
		} else {
			clearInnerHTML('otherLanguageBox'); 
		} // if other language is chosen
	} // openLanguageBox()

	function changeSecQ(chgdSelectorNum) {
		var otherSelectorNum = 3 - chgdSelectorNum;
		if (!cellExists('secQ' + chgdSelectorNum)) return;
		var chgdSelectorQNum = 
				parseInt(document.getElementById('secQ' + chgdSelectorNum).value);
		var otherSelectorQNum = 
				parseInt(document.getElementById('secQ' + otherSelectorNum).value);
		var bld = new String.builder().
				append('<select name="secQ').
				append(otherSelectorNum).
				append('" id="secQ').
				append(otherSelectorNum).
				append('" onchange="changeSecQ(').
				append(otherSelectorNum).
				append(');">');
		var qNum = 0;
		<% for (final String secQ : User.SECURITY_QS) { %>
			if (qNum !== chgdSelectorQNum) {
				bld.append('<option value="').append(qNum).append('"');
				if (qNum === otherSelectorQNum) bld.append(' <%= SELECTED %>');
				bld.append('><%= tempUser.translateJS(secQ) %><\/option>');
			} // if Q hasn't already been selected
			qNum++;
		<% } // for each security question %>
		bld.append('<\/select>');
		document.getElementById('secQ' + otherSelectorNum + 'Cell').innerHTML =
				bld.toString();
	} // changeSecQ()

	function checksPrefersJava() {
		if (getChecked('prefersJava')) {
			toAlert('<%= tempUser.translateJS("Google Chrome does not support "
					+ "Java. Do not use Chrome with ACE Organic if you prefer "
					+ "to use Java applets.") %>');
		} // if user prefers Java
	} // checksPrefersJava()

	function submitIt() {
		var form = document.editform;
		var userid = form.userid.value;
		// checking for addmode
		if (userid == null || isWhiteSpace(userid)) {
 			toAlert('<%= tempUser.translateJS("You must enter a username.") %>');
 			return;
		}
		if (containsWhiteSpace(userid)) {
 			toAlert('<%= tempUser.translateJS("Whitespace (space, tab, etc.) "
					+ "is not allowed in the username.") %>');
  			return;
		}
		if (userid.length > 14) {
			toAlert('<%= tempUser.translateJS(
					"The username may contain no more than 14 characters.") %>');
 			return;
		}
		if (userid === 'admin') {
			toAlert('<%= tempUser.translateJS(
					"The username must not be \"admin\".") %>');
 			return;
		}
		if (userid.indexOf('@') >= 0) {
			toAlert('<%= tempUser.translateJS(
					"The username must not contain the character \"@\". "
					+ "If you entered your email address, delete "
					+ "the @ character and everything following it.") %>');
 			return;
		}
		// check if needed values are entered
		if 	(isWhiteSpace(form.firstName.value)) {
			toAlert('<%= tempUser.translateJS(
					"You must enter a first (given) name.") %>');
			return;
		}
		if (isWhiteSpace(form.lastName.value)) {
 			toAlert('<%= tempUser.translateJS("You must enter a last name "
					+ "(surname, family name).") %>');
 			return;
 		}
		if (isWhiteSpace(form.passphrase_entry.value)) {
			toAlert('<%= tempUser.translateJS("You must enter a password.") %>');
			return;
		}
		var instnSel = form.institutionSel;
		form.institution.value = instnSel.value;
		var chosenInst = instnSel.options[instnSel.selectedIndex].value;
		form.isDefaultInstn.value = chosenInst === '<%= defaultInstnId %>';
		if (!form.isDefaultInstn.value) {
			if (form.passphrase_entry.value.length > 14) {
				toAlert('<%= tempUser.translateJS(
						"The password may contain no more than 14 characters.") %>');
	 			return;
	 		}
			if (form.passphrase_entry.value !== form.passphrase_confirm.value) {
				toAlert('<%= tempUser.translateJS(
						"The passwords you entered don't match.") %>');
				return;
			}
	 		if (isWhiteSpace(form.secAns1.value) || isWhiteSpace(form.secAns2.value)) {
	 			toAlert('<%= tempUser.translateJS(
						"You must answer both security questions.") %>');
	 			return;
	 		}
			if (isWhiteSpace(form.email.value) ||
					!isValidEmail(form.email.value)) {
	 			toAlert('<%= tempUser.translateJS(
						"You must enter a valid email address.") %>');
	 			return;
	 		}
	 		if (isWhiteSpace(form.email_confirm.value)) {
	 			toAlert('<%= tempUser.translateJS(
						"You must enter the email address twice.") %>');
	 			return;
	 		}
	 		if (form.email.value !== form.email_confirm.value) {
	 			toAlert('<%= tempUser.translateJS(
						"The email addresses you entered don't match.") %>');
	 			return;
	 		}
		} // if not the default institution
		// replace password with md5 hash
		form.passphrase.value = b64_md5(form.passphrase_entry.value);
	<% if (isStudent) { %>
		if (isWhiteSpace(form.studentnum.value)) {
 				toAlert('<%= tempUser.translateJS(
					"You must enter a student ID number.") %>');
			return;
 			}
	<% } else { %>
		// validate selected institution
		if (form.institution.value === '<%= OTHER %>') {
			if (form.other_institution.value.length < 1) { // >
				toAlert('<%= tempUser.translateJS("If you choose \""
						+ OTHER + "\" from the pulldown menu, you must "
						+ "enter an institution name.") %>');
				return;
			} else {
				form.institution.value = form.other_institution.value;
			} // if another institution was entered
		} // if OTHER was selected
	<% } // if not student %>
	form.submit();
} // submitIt()
// accept-charset="UTF-8"
// -->
	</script>
</head>
<body class="light" style="overflow:auto;" 
		onload="changeSecQ(1); changeSecQ(2); changeInstitution(false);">
	<table style="width:100%;">
		<tr><td style="text-align:center;">
			  <img src="<%= pathToRoot %>images/acelogo.jpg" alt="logo"/>
		</td></tr>
		<tr><td style="vertical-align:top;">
			<form name="editform" method="post" action="saveNewUser.jsp" 
					accept-charset="UTF-8">
				<input type="hidden" name="passphrase" value=""/>
				<input type="hidden" name="institution" value=""/>
				<input type="hidden" name="isDefaultInstn" value=""/>
				<input type="hidden" name="userType" value="<%= userType %>"/>
				<input type="hidden" name="language" 
						value="<%= Utils.toValidHTMLAttributeValue(chosenLang) %>"/>
			<table class="whiteTable" style="width:626px; text-align:left;">
				<tr><td class="boldtext enlarged" colspan="3"
						style="vertical-align:top;
						padding-top:10px; padding-left:40px;">
					<%= tempUser.translate("Fill in the details to register with ACE.") %>
				</td></tr>
				<tr><td class="regtext" colspan="3"
						style="padding-bottom:10px; padding-left:40px;
						font-style:italic;">
					<%= tempUser.translate("Required fields are marked with ***[star]***.",
							"<span style=\"color:red;\">&#42;</span>") %>
				</td></tr>
				<tr><td class="regtext" colspan="3"
						style="padding-bottom:10px; padding-left:40px;">
					<span class="boldtext" style="color:red;">
					<%= tempUser.translate("Attention") %></span>:
					<%= tempUser.translate("Do <i>not</i> reregister with ACE if you "
							+ "have already registered.  If you have forgotten your login "
							+ "ID or password, contact your instructor.  If you wish to "
							+ "change your password, student ID number, or email address, "
							+ "log in and press the <b>My Profile</b> button.") %>
				</td></tr>
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
									<%= chosenInstnId == institution.getId()
										? SELECTED : "" %> >
							<%= instnName %></option>
						<% } // for each institution %>
						<% if (!isStudent) { %>
							<option value="<%= OTHER %>">
								<%= tempUser.translate(OTHER) %></option>
						<% } // if instructor %>
					</select>
				</td></tr>
				<% if (!isStudent) { %>
					<tr><td id="otherNameLabel" class="regtext" style="padding-left:40px;"></td>
					<td id="otherNameBox" colspan="2"></td></tr>
					<tr><td id="otherStudentIdNameLabel" class="regtext" style="padding-left:40px;"></td>
					<td id="otherStudentIdNameBox" colspan="2"></td></tr>
					<tr><td id="otherLanguageLabel" class="regtext" style="padding-left:40px;"></td>
					<td id="otherLanguageSelector"></td>
					<td id="otherLanguageBox"></td></tr>
					<tr><td colspan="3" id="otherInstnHR"></td></tr>
				<% } // if instructor %>
				<tr><td class="regtext" style="padding-left:40px;">
					<%= tempUser.translate("First (given) name") %>:
				</td>
				<td colspan="2" style="width:100%;">
					<table style="position:relative; left:-1px;
							border-collapse:collapse; padding:0px; border-width:0px;">
						<tr><td>
							<input type="text" name="firstName" size="40" value=""/>
							<span style="color:red;">*</span>
						</td>
						<td class="regtext" style="padding-left:10px;">
							<%= tempUser.translate("M.I.") %>:
						</td>
						<td>
							<input type="text" name="middleName" size="1" value=""/>
						</td></tr>
					</table>
				</td></tr>
				<tr><td class="regtext" style="padding-left:40px;">
					<%= tempUser.translate("Last (family) name") %>:
				</td>
				<td colspan="2">
					<input type="text" name="lastName" size="40" value=""/>
					<span style="color:red;">*</span>
				</td></tr>
				<tr><td class="regtext" style="padding-left:40px;">
				</td>
				<td colspan="2">
					<input type="checkbox" name="familyName1st" />
					<%= tempUser.translate("I prefer my family name to come "
							+ "before my given name.") %>
				</td></tr>
				<tr><td class="regtext" style="padding-left:40px;">
				</td>
				<td colspan="2">
					<input type="checkbox" name="day1st" />
					<%= tempUser.translate("I prefer dates in the format day-month-year "
							+ "instead of month-day-year.") %>
				</td></tr>
				<tr><td id="usernameLabel" class="regtext" style="padding-left:40px;">
				</td>
				<td colspan="2">
					<input type="text" name="userid" size="20" value=""/>
					<span style="color:red;">*</span>
				</td></tr>
				<tr><td colspan="3">
				<span id="notAtDefaultInstitution"></span>
				</td></tr>
				<% if (isStudent) { %>
					<tr><td id="studentIdName" class="regtext" style="padding-left:40px;">
						Student ID number:
					</td>
					<td colspan="2">
						<input type="text" name="studentnum" size="20" value=""/>
						<span style="color:red;">*</span>
					</td></tr>
				<% } else { // instructor %>
					<tr><td class="regtext" style="padding-left:40px;" colspan="3">
						<br/>
					</td></tr>
					<tr><td class="regtext" style="padding-left:40px; vertical-align:top;">
						<%= tempUser.translate("Address") %>:
					</td>
					<td colspan="2">
						<textarea name="address" cols="50" rows="5"></textarea>
					</td></tr>
					<tr><td class="regtext" style="padding-left:40px;">
						<%= tempUser.translate("Telephone") %>:
					</td>
					<td colspan="2">
						<input type="text" name="phone" size="20" value=""/>
					</td></tr>
				<% } // if student or instructor %>
				<tr><td class="regtext" style="padding-left:40px;">
					<%= tempUser.translate("Email address to use to send "
							+ "text messages to your phone") %>:
				</td>
				<td colspan="2">
					<input type="text" name="textMessageEmail" size="40" value=""/>
				</td></tr>
				<tr><td style="padding-bottom:10px; padding-top:10px; padding-left:40px;">
					<%= makeButton(tempUser.translate("Register"), "submitIt();") %>
				</td>
				<td style="padding-bottom:10px; padding-top:10px;">
					<%= makeButton(tempUser.translate("Reset"), "document.editform.reset();") %>
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
