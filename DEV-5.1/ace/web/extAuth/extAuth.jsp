<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.courseware.Institution,
	com.epoch.courseware.Name,
	com.epoch.courseware.User,
	com.epoch.exceptions.DBException,
	com.epoch.exceptions.StudentEmailExistsException,
	com.epoch.exceptions.StudentNumConstraintException,
	com.epoch.exceptions.UniquenessException,
	com.epoch.session.AnonSession,
	com.epoch.session.AdminSession,
	com.epoch.session.InstructorSession,
	com.epoch.session.StudentSession,
	com.epoch.utils.Utils"
%>

<%	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");

	final String url = request.getRequestURL().toString();
	if (url != null && url.matches("http://ace\\.chem\\.illinois\\.edu:91[\\d]+/.*")) { 
		response.sendRedirect(url.replaceFirst("http:", "https:").replaceFirst("91", "93"));
	} else if (url != null && url.matches("http://ace\\.chem\\.illinois\\.edu/.*")) {
		response.sendRedirect("https://ace.chem.illinois.edu/ace");
	} // if url should be redirected
	final boolean aceIsDown = false;

	String flag = request.getParameter("flag");
	if (flag == null) flag = ""; 

	final String uID = request.getAttribute("eppn").toString();
	final String[] splitUID = uID.split("@");
	final String userID = splitUID[0];

	String chosenLang = request.getParameter("language");
	if (chosenLang == null && AppConfig.notEnglish) {
		chosenLang = AppConfig.defaultLanguage;
	}

	final User user = AnonSession.getUser(userID);
	boolean userFound = AnonSession.isRegdACEUser(userID, uID, 
			request.getAttribute("AJP_iTrustUIN").toString());
	boolean haveError = false;
	
	if (userFound) { 
		final char role = user.getRole();
	} else {
		final byte[] hashValueArr = new byte[0];
		final Institution instn = AnonSession.getDefaultInstitution();
		final char role = User.STUDENT;
		final User newUser = new User(userID, hashValueArr);
		newUser.setInstitution(instn);
		newUser.setRole(role);
		newUser.setName(new Name(Utils.inputToCERs(
					request.getAttribute("AJP_givenName").toString()),
				Utils.inputToCERs(request.getAttribute("AJP_sn").toString())));
		newUser.setEmail(Utils.inputToCERs(request.getAttribute("eppn").toString()));
		newUser.setStudentNum(Utils.inputToCERs(
				request.getAttribute("AJP_iTrustUIN").toString()));
	
		boolean notUniqueError = false;
		boolean studentNumExists = false; 
		boolean studentEmailExists = false; 
		boolean registerError = false;
		
		try {
			AnonSession.registerUser(newUser); 
		} catch (StudentNumConstraintException e) {
			studentNumExists = true;
		} catch (StudentEmailExistsException e) {
			studentEmailExists = true;
		} catch (UniquenessException e) {
			notUniqueError = true;
		} catch (Exception e) {
			registerError = true;
		}
		
		haveError = notUniqueError || studentNumExists
				|| studentEmailExists || registerError;
	} // if new user
	
	if (userFound || !haveError) {
		// user verified
		// invalidate any previous sessions
		// create instructorSession and bind to it
		final HttpSession oldsession = request.getSession(false);
		if (oldsession != null) oldsession.invalidate();
		final HttpSession newsession = request.getSession(true);

		// Load the session of user with all data
		// If an error occurs while loading, user is directed to
		// an error page
		if (user.getRole() == User.ADMINISTRATOR) {
			final AdminSession sess = new AdminSession(user);
			newsession.setAttribute("usersession", sess);
		} else if (user.getRole() == User.INSTRUCTOR) {
			final InstructorSession sess = new InstructorSession(user);
			newsession.setAttribute("usersession", sess);
		} else if (user.getRole() == User.STUDENT) {
			final StudentSession sess = new StudentSession(user);
			newsession.setAttribute("usersession", sess);
			if (!sess.isEnrolled(AppConfig.tutorialId)) {
				try {
					AnonSession.enrollInTutorialsCourse(userID);
					sess.refreshCourses();
				} catch (Exception e) {
					e.printStackTrace();
				} // try
			} // if not enrolled in tutorial course
		} // if role
	} // if user found or no error in registering new user

	final String forwarder = "userHome.jsp";

%>

<!doctype html>
<html>
<head>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="shortcut icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/> 
	<title>Authenticate <%= AppConfig.defaultInstitution %> User</title>
	<script type="text/javascript">
		function finish() {
			self.location.href = '<%= pathToRoot %><%= forwarder %>?flag=Success&language='
					+ encodeURIComponent('<%= Utils.toValidJS(chosenLang) %>');
		} // finish()
	</script>
</head>

<body class="light" <% if (!haveError) {%>onload="finish()"<%}%>>
	<div id="content" role="main">
		<div>
			<img src="<%= pathToRoot %>images/acelogo.jpg" alt="logo"/>
		</div>
	
		<h2>An error occurred while completing your ACE Organic registraton.</h2>
		<p>Please contact <a href="mailto:<%= AppConfig.webmasterEmail 
			%>?subject=Problem with ACE Organic registration&body=Failed to add netID <%= 
			userID %>, UIN <%= request.getAttribute( "AJP_iTrustUIN" ).toString() 
			%>, email <%= request.getAttribute( "eppn" ).toString() %>"><%= 
			AppConfig.webmasterEmail %></a> for assistance.</p>

	</div> 
</body>
</html>
