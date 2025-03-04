<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.Course,
	com.epoch.utils.Utils"
%>

<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%	
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final String task = request.getParameter("task");
	final String selectedCrsIdsStr = request.getParameter("crsIds");
	final int[] selectedCrsIds = 
			Utils.stringToIntArray(selectedCrsIdsStr.split(":"));
	/* Utils.alwaysPrint("saveRevealed.jsp: task = ", task,
			", selectedCrsIds = ", selectedCrsIds); /**/
	if ("remove".equals(task)) {
		if (userSess instanceof AdminSession) {
			((AdminSession) userSess).removeHiddenCourses(selectedCrsIds);
		} else if (userSess instanceof InstructorSession) {
			((InstructorSession) userSess).removeHiddenCourses(selectedCrsIds);
		} // if userSess
	} else { // reveal
		if (userSess instanceof AdminSession) {
			((AdminSession) userSess).revealCourses(selectedCrsIds);
		} else if (userSess instanceof InstructorSession) {
			((InstructorSession) userSess).revealCourses(selectedCrsIds);
		} // if userSess
	} // if task

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
		function goBack() {
			self.location.href = '<%= pathToRoot %>userHome.jsp';
		} // goBack()
		// -->
	</script>
</head>
<body class="light" style="background-color:white;" onload="goBack();">
</body>
</html>
