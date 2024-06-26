<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.Course,
	com.epoch.servlet.Base64Coder,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%	
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final int index = MathUtils.parseInt(request.getParameter("index"));
	final boolean clone = "true".equals(request.getParameter("clone"));
	final String doneAction = (clone 
			? "cloneHWSets.jsp?oldCrsNum=" + index
			: pathToRoot + "userHome.jsp");
	final boolean deletePassword =
			"on".equals(request.getParameter("deletePassword"));
	final byte[] passwordHash = (deletePassword ? new byte[0]
			: Base64Coder.decode(request.getParameter("password")));
	/*
	Utils.alwaysPrint("saveCoursePassword.jsp: index = ", index,
			", clone = ", clone, ", deletePassword = ", deletePassword);
	/**/

	Course course = null;
	if (userSess instanceof AdminSession) {
		course = ((AdminSession) userSess).getCourse(index);
	} else if (userSess instanceof InstructorSession) {
		course = ((InstructorSession) userSess).getCourse(index);
	}
	course.setPasswordHash(passwordHash);
	if (userSess instanceof AdminSession) {
		((AdminSession) userSess).setCoursePassword(course);
	} else if (userSess instanceof InstructorSession) {
		((InstructorSession) userSess).setCoursePassword(course);
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
	<title>ACE Course Management</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<style type="text/css">
		* html body {
			padding:50px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

	function done() {
		this.location.href = '<%= doneAction %>';
	}
	// -->
	</script>
</head>
<body class="light" style="background-color:white;" onload="done();">
</body>
</html>
