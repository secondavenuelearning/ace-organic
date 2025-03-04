<%@ page import="
	com.epoch.AppConfig,
	com.epoch.courseware.User,
	com.epoch.session.AdminSession,
	com.epoch.session.InstructorSession,
	com.epoch.session.StudentSession,
	com.epoch.session.UserSession,
	com.epoch.utils.Utils"
%><%	
	final String pathToAppRoot = "/ace/";
	UserSession userSess = null;
	synchronized (session) {
		userSess = (UserSession) session.getAttribute("usersession");
	}
	if (userSess == null) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}

	final User realUser = userSess.getUser();
	final char realRole = (userSess instanceof AdminSession ? User.ADMINISTRATOR
			: userSess instanceof InstructorSession ? User.INSTRUCTOR
			: User.STUDENT);
	User user = realUser;
	char role = realRole;
	if (realRole == User.ADMINISTRATOR) {
		final AdminSession adminSess = (AdminSession) userSess;
		user = adminSess.getActedUser();
		role = adminSess.getActedRole();
	}
	
	// vim:filetype=jsp
%>
