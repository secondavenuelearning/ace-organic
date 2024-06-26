<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.utils.MathUtils"
%>

<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%	
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final int index = MathUtils.parseInt(request.getParameter("index"));

	if (userSess instanceof AdminSession) {
	    ((AdminSession) userSess).removeCourse(index);
	} else if (userSess instanceof InstructorSession) {
 		((InstructorSession) userSess).removeCourse(index);
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
	<title>ACE DEV-5.1</title>
	<script type="text/javascript">
		// <!-- >
		function goBack() {
			this.location.href = '<%= pathToRoot %>userHome.jsp';
		} // goBack()
		// -->
	</script>
</head>
<body onload="goBack();">
</body>
</html>

