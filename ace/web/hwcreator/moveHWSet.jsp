<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>

<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	if (!Utils.among(role, User.ADMINISTRATOR, User.INSTRUCTOR)) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	request.setCharacterEncoding("UTF-8");

	final int from = MathUtils.parseInt(request.getParameter("from"));
	final int to = MathUtils.parseInt(request.getParameter("to"));

	if (role == User.ADMINISTRATOR) {
		((AdminSession) userSess).moveHWSet(from, to);
	} else if (role == User.INSTRUCTOR) {
		((InstructorSession) userSess).moveHWSet(from, to);
	}

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
<script type="text/javascript">
	// <!-- >
	function finish() {
		this.location.href = 'hwSetList.jsp';
	} // finish();
	// -->
</script>
</head>
<body onload="finish();">
</body>
</html>
