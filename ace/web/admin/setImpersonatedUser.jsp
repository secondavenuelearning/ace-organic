<%@ page language="java" %>
<%@ page errorPage="../errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.session.AdminSession,
	com.epoch.session.UserSession,
	com.epoch.utils.MathUtils"
%>

<%
	UserSession usrSess = null;
	synchronized (session) {
		usrSess = (UserSession) session.getAttribute("usersession");
	}
	if (!(usrSess instanceof AdminSession)) {
		%> <jsp:forward page="../errormsgs/noSession.html" /> <%
	}
	request.setCharacterEncoding("UTF-8");
	final AdminSession admSess = (AdminSession) usrSess;
	final int userNum = MathUtils.parseInt(request.getParameter("userNum"));
	admSess.setActedUser(userNum);
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
	<script type="text/javascript">
	function loadUserHome() {
		self.location.href = '../userHome.jsp';
	}
	</script>
</head>
<body onload="loadUserHome()">
</body>
</html>
