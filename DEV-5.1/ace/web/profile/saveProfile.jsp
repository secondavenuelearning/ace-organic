<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.Institution,
	com.epoch.courseware.Name,
	com.epoch.exceptions.UniquenessException,
	com.epoch.servlet.Base64Coder,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
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
	User editUser = null;
	final String userId = Utils.inputToCERs(request.getParameter("userId"));
	final String pwd = request.getParameter("passphrase");
	final boolean havePwd = !Utils.isEmpty(pwd);
	if ("true".equals(request.getParameter("storeNewPwd"))) {
		// write a new password. Note: password hash is Base64 decoded.
		final byte[] hashValueArr = (havePwd 
				? Base64Coder.decode(pwd) : new byte[0]);
		editUser = new User(userId, hashValueArr);
	} else {
		editUser = new User(userId, new byte[0]);
		editUser.setSavePassword();
	}
	Institution[] institutions;
	synchronized (session) {
		institutions = (Institution[]) session.getAttribute("institutions");
	}

	final String instnName = request.getParameter("institution");
	final int instnId = MathUtils.parseInt(instnName, 0);
	final String studentNumLabel = request.getParameter("studentNumLabel");
	if (instnId > 0) {
		Institution institution = null;
		for (final Institution instn : institutions) {
			if (instn.getId() == instnId) {
				institution = instn;
				break;
			} // if we found the institution
		} // for each institution
		boolean studentNumLabelChanged = false;
		if (studentNumLabel != null) {
			final String oldStudentNumLabel = institution.getStudentNumLabel();
			studentNumLabelChanged = oldStudentNumLabel == null
					|| !oldStudentNumLabel.equals(studentNumLabel.trim());
		} // if there's a studentNumLabel
		editUser.setInstitution(studentNumLabelChanged
				? new Institution(-instnId, institution.getName(),
						Utils.inputToCERs(studentNumLabel.trim()))
				: new Institution(institution));
	} else {
		editUser.setInstitution(new Institution(Utils.inputToCERs(instnName.trim()),
				Utils.inputToCERs(studentNumLabel.trim())));
	}
	final String givenName = request.getParameter("givenName");
	final String familyName = request.getParameter("familyName");
	editUser.setName(new Name(Utils.inputToCERs(givenName),
			Utils.inputToCERs(request.getParameter("middleName")),
			Utils.inputToCERs(familyName))); 
	editUser.setRole(request.getParameter("role").charAt(0));
	final String email = request.getParameter("email");
	editUser.setEmail(Utils.isEmpty(email) 
			? Utils.toString(userId, '@', AppConfig.defaultDomain)
			: Utils.inputToCERs(email.trim()));
	final String textMessageEmail = request.getParameter("textMessageEmail");
	editUser.setTextMessageEmail(Utils.inputToCERs(textMessageEmail.trim()));
	editUser.setStudentNum(Utils.inputToCERs(request.getParameter("studentnum")));
	editUser.setAddress(Utils.inputToCERs(request.getParameter("address")));
	editUser.setPhone(Utils.inputToCERs(request.getParameter("phone")));
	editUser.setFlags(MathUtils.parseInt(request.getParameter("flags")));
	editUser.setEnabled("Yes".equals(request.getParameter("enabled")));
	editUser.setFamilyName1st("on".equals(request.getParameter("familyName1st")));
	editUser.setDay1st("on".equals(request.getParameter("day1st")));
	editUser.setPrefersPNG("on".equals(request.getParameter("prefersPNG")));

	boolean notUniqueError = false;
	final String editIndex = request.getParameter("editindex");
	if (editIndex != null) {
		final int index = Integer.parseInt(editIndex);
		if (index == 0) {
			editUser.setUserId(userId); 
			try {
				((AdminSession) userSess).addUser(editUser);
			} catch (UniquenessException e) {
				notUniqueError = true;
			} // try
		} else {
			try {
				((AdminSession) userSess).setUser(index, editUser);
			} catch (UniquenessException e) {
				notUniqueError = true;
			} //try
		} // if user is new
	} else if (realRole == User.ADMINISTRATOR) { // admin impersonating user
		final UserSession actedUserSess = new UserSession(user);
		actedUserSess.setProfile(editUser);
		((AdminSession) userSess).resetAllUsers();
	} else {
		userSess.setProfile(editUser);
	} // if user editing herself
	if (!notUniqueError && request.getParameter("secQ1") != null && havePwd) {
		final String[] secQsAndAnswers = new String[3];
		secQsAndAnswers[0] = Utils.toString(request.getParameter("secQ1"), ":", 
				request.getParameter("secQ2"));
		secQsAndAnswers[1] = Utils.inputToCERs(request.getParameter("secAns1"));
		secQsAndAnswers[2] = Utils.inputToCERs(request.getParameter("secAns2"));
		editUser.setSecurityAnswers(secQsAndAnswers);
	} // if there's no error and user is editing himself

	final String goBack = request.getParameter("goBack");

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
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<style type="text/css">
		* html body {
			padding:50px 0 0px 0; 
		}
	</style>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	// -->
	
	</script>
</head>
<body class="light" style="background-color:white;">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<div id="contentsWithoutTabs">
	 <% if (notUniqueError) { %>
		<br/>
		The username you entered has already been selected. 
		Please enter a different one.
		<p><%= makeButton("Back", "history.back();") %></p>
	 <% } else { %>
		<script type="text/javascript">
			// <!-- >
			self.location.href = '<%= Utils.toValidJS(goBack) %>';
			// -->
		</script>
	 <% } %>
	</div>
</body>
</html>
