<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final int oldCrsNum = MathUtils.parseInt(request.getParameter("oldCrsNum"));
	final int newCrsNum = MathUtils.parseInt(request.getParameter("newCrsNum"));
	switch (role) {
		case User.ADMINISTRATOR:
			final AdminSession adminSess = (AdminSession) userSess;
			adminSess.moveCourse(oldCrsNum, newCrsNum);
			break;
		case User.INSTRUCTOR:
			final InstructorSession instrSess = (InstructorSession) userSess;
			instrSess.moveCourse(oldCrsNum, newCrsNum);
			break;
		case User.STUDENT:
			final StudentSession studSess = (StudentSession) userSess;
			studSess.moveStudentCourse(oldCrsNum, newCrsNum);
			break;
		default: break;// shouldn't happen
	} // switch role 
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
	<title>ACE Select Course</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >
	function goHome() {
		self.location.href = '<%= pathToRoot %>userHome.jsp';
	} // goHome()
	// -->
	</script>
</head>
<body class="light" style="background-color:white;" onload="goHome();">
</body>
</html>

