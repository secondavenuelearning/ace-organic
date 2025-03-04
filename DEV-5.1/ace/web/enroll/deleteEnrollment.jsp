<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>

<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	request.setCharacterEncoding("UTF-8");

	final String indicesStr = request.getParameter("indices");
	final String[] indicesStrs = indicesStr.split(":");
	((InstructorSession) userSess).disenroll(indicesStrs);

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
<script type="text/javascript">
	// <!-- >
	function finish() {
		self.location.href = 'listEnrollment.jsp';
	} // finish()
	// -->
</script>
</head>
<body onload="finish();">
</body>
</html>
	
