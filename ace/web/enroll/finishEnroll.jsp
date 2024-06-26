<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.courseware.Course,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	StudentSession studSess;
	synchronized (session) {
		studSess = (StudentSession) session.getAttribute("usersession");
	}
	boolean success = true;
	try {
		final String studentNum = request.getParameter("studentNum");
		studSess.setStudentNum(studentNum);
	} catch (Exception e1) {
		e1.printStackTrace();
		success = false;
	}
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<title>Edit Unique ID</title>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >
	function finish() {
		<% if (!success) { %>
			toAlert('<%= user.translateJS(
					"The operation failed, possibly because someone has already "
					+ "enrolled under the ***student ID number*** you entered.",
					user.getInstitutionStudentNumLabel()) %>');
		<% } %>
		opener.location.reload(true);
		self.close();
	} // finish()
	// -->
	</script>
</head>
<body onload="finish();" style="background-color:#f6f7ed;">
</body>
</html>

