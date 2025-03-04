<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.courseware.Institution,
	com.epoch.courseware.Name,
	com.epoch.courseware.User,
	com.epoch.exceptions.StudentEmailExistsException,
	com.epoch.exceptions.StudentNumConstraintException,
	com.epoch.exceptions.UniquenessException,
	com.epoch.servlet.Base64Coder,
	com.epoch.servlet.Ldap,
	com.epoch.session.AnonSession,
	com.epoch.session.UserSession,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>

<%	final String pathToRoot = "../"; 
	request.setCharacterEncoding("UTF-8");

	final String pwd = request.getParameter("passphrase");
	final boolean isDefaultInstn = 
			"true".equals(request.getParameter("isDefaultInstn"));
	final String userId = request.getParameter("userid").trim();
	final String userIdToCERs = Utils.inputToCERs(userId);
	/*/ System.out.println("saveNewUser.jsp: userId = " + userId
			+ ", toCERS = " + userIdToCERs); /**/
	final User user = new User(userIdToCERs, isDefaultInstn
			? new byte[0] : Base64Coder.decode(pwd));
	final String instnName = request.getParameter("institution");
	final String studentNumLabel = request.getParameter("studentNumLabel");
	final int instnId = MathUtils.parseInt(instnName, 0);
	if (instnId > 0) {
		user.setInstitution(new Institution(instnId));
	} else {
		String instnLanguage = request.getParameter("institutionLanguage");
		if ("other".equals(instnLanguage)) {
			instnLanguage = request.getParameter("otherInstructionLanguage");
			if (instnLanguage != null) instnLanguage = instnLanguage.trim();
		} // if a new language is being introduced
		// Utils.alwaysPrint("saveNewUser.jsp: instnLanguage = ", instnLanguage);
		final Institution instn = 
				(Utils.isEmpty(instnLanguage) || "English".equals(instnLanguage)
				? new Institution(Utils.inputToCERs(instnName.trim()),
					Utils.inputToCERs(studentNumLabel.trim()))
				: new Institution(Utils.inputToCERs(instnName.trim()),
					Utils.inputToCERs(instnLanguage),
					Utils.inputToCERs(studentNumLabel.trim())));
		user.setInstitution(instn);
	} // if instnId
	user.setRole(request.getParameter("userType").charAt(0));
	user.setName(new Name(Utils.inputToCERs(request.getParameter("firstName").trim()),
			Utils.inputToCERs(request.getParameter("middleName").trim()),
			Utils.inputToCERs(request.getParameter("lastName").trim()))); 
	final String email = request.getParameter("email");
	user.setEmail(Utils.isEmpty(email) 
			? Utils.toString(userId, '@', AppConfig.defaultDomain)
			: Utils.inputToCERs(email.trim()));
	final String textMessageEmail = request.getParameter("textMessageEmail");
	user.setTextMessageEmail(Utils.inputToCERs(textMessageEmail.trim()));
	if (user.getRole() == User.INSTRUCTOR) {
		user.setEnabled(false);
		user.setPhone(Utils.inputToCERs(request.getParameter("phone").trim()));
		user.setAddress(Utils.inputToCERs(request.getParameter("address").trim()));
	} else {
		user.setStudentNum(Utils.inputToCERs(request.getParameter("studentnum").trim()));
	} // if instructor
	user.setFamilyName1st("on".equals(request.getParameter("familyName1st")));
	user.setDay1st("on".equals(request.getParameter("day1st")));
	final String language = request.getParameter("language");
	user.setLanguage(language);

	boolean authenticationError = isDefaultInstn && !Ldap.authenticate(
			userId, request.getParameter("passphrase_entry"));
	boolean notUniqueError = false;
	boolean studentNumExists = false; 
	boolean studentEmailExists = false; 
	boolean registerError = false;
	if (!authenticationError) try {
		AnonSession.registerUser(user); 
	} catch (StudentNumConstraintException e) {
		studentNumExists = true;
	} catch (StudentEmailExistsException e) {
		studentEmailExists = true;
	} catch (UniquenessException e) {
		notUniqueError = true;
	} catch (Exception e) {
		registerError = true;
	}
	final boolean haveError = authenticationError || notUniqueError 
			|| studentNumExists || studentEmailExists || registerError;
	if (!haveError && !isDefaultInstn) {
		final String[] secQsAndAnswers = new String[3];
		secQsAndAnswers[0] = 
				request.getParameter("secQ1") + ":" + request.getParameter("secQ2");
		secQsAndAnswers[1] = Utils.inputToCERs(request.getParameter("secAns1"));
		secQsAndAnswers[2] = Utils.inputToCERs(request.getParameter("secAns2"));
		// Utils.alwaysPrint("saveNewUser.jsp: secQsAndAnswers = ", secQsAndAnswers);
		user.setSecurityAnswers(secQsAndAnswers);
	} // if there's no error

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
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon">
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css">
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>

<script type="text/javascript">
	// <!-- >

	function redo() {
		self.location.href = 'register.jsp?userType=<%= user.getRole() %>&language='
				+ encodeURIComponent('<%= Utils.toValidJS(language) %>');
	}

	function finish() {
		<% if (user.getRole() == User.INSTRUCTOR) { 
			UserSession.sendTextMessage(AppConfig.verifierEmail, 
					Utils.toString(user.getName(), 
						" has registered as an instructor.")); %>
			toAlert('<%= user.translateJS("Email ***Robert Grossman*** at "
					+ "***robert.grossman@uky.edu*** "
					+ "to request full instructor privileges.",
				new String[] {AppConfig.verifierName, 
						AppConfig.verifierEmail}) %>'); 
		<% } else { %>
			toAlert('<%= user.translateJS("Before you start to use ACE, "
					+ "do the browser check-up.  The link is underneath "
					+ "where you log in.") %>');
		<% } // if instructor %>	
		self.location.href = '<%= pathToRoot %>login.jsp?flag=Success&language='
				+ encodeURIComponent('<%= Utils.toValidJS(language) %>');
	} // finish()

	// -->
</script>

</head>

<body class="light" onload="<%= haveError ? "" : "finish();" %>">
<table class="regtext" style="width:100%;">
	<tr>
	<td style="text-align:center;">
		<img src="<%= pathToRoot %>images/acelogo.jpg" alt="logo"/>
	</td>
	</tr>
	<tr>
	<td style="vertical-align:top; text-align:center;">
		<table class="whiteTable" style="width:626px;">
			<tr>
			<td class="regtext" style="padding-top:10px; 
					padding-bottom:10px; padding-left:30px; padding-right:30px;">
  				<% if (studentNumExists || studentEmailExists) { %>
					<%= user.translate(" The ***student number*** "
							+ "you entered is already associated "
							+ "with an ACE account of a student at ***your institution***. "
							+ "<p>If you have registered with ACE before, and you want "
							+ "to change the information you added when you registered "
							+ "previously, log in to ACE and "
							+ "press the <b>Edit Profile</b> button. "
							+ "(If you don't remember your login or password, contact "
							+ "your instructor and ask him or her to retrieve it.) "
							+ "</p><p>If you have not registered with ACE before, invent "
							+ "a student number and let your instructor know "
							+ "it.  He or she will enroll you manually in the course.<p>",
					new String[] {(studentNumExists ? "student number" : "email address"),
							instnName}) %>
  				<% } else if (notUniqueError) { %>
					<%= user.translate("Someone has already registered with the "
							+ "username you selected. Please select a different one.") %>
				<% } else if (registerError) { %>
					<%= user.translate("An error occurred while registering your entry. Please "
							+ "contact the administrator if you get this error repeatedly.") %>
				<% } else if (authenticationError) { %>
					<%= user.translate("An error occurred while authenticating your "
							+ "username and password. Please try to register again, and "
							+ "contact the administrator if you get this error repeatedly.") %>
				<% } else { %>
				<% } // if kind of error %>
			</td>
			</tr>
			<tr>
			<td><table style="margin-right:auto; margin-left:auto; 
					padding-bottom:10px;"><tr>
			<td><%= makeButton(user.translate("Back"), "redo();") %></td>
			</tr></table></td>
			</tr>
		</table>
	</td>
	</tr>
</table>
</body>
</html>
