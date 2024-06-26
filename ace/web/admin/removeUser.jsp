<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.session.AdminSession,
	com.epoch.session.UserSession,
	com.epoch.utils.MathUtils"
%>

<%
	/* parameters
	           delindex - number if to be deleted 
	*/

	UserSession userSess = null;
	synchronized (session) {
		userSess = (UserSession) session.getAttribute("usersession");
	}
	if (!(userSess instanceof AdminSession)) {
		%> <jsp:forward page="../errormsgs/noSession.html" /> <%
	}
	request.setCharacterEncoding("UTF-8");
	final int index = MathUtils.parseInt(request.getParameter("delindex"));
	((AdminSession) userSess).removeUser(index);

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
	</head>
<body>
<script type="text/javascript">
	// <!-- >
	self.location.href = 'listProfiles.jsp';
	// -->
</script>
</body>
</html>



