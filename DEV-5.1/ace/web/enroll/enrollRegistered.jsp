<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.EnrollmentData,
	com.epoch.utils.MathUtils,
	java.util.ArrayList,
	java.util.List"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	request.setCharacterEncoding("UTF-8");
	final InstructorSession instrSess = (InstructorSession) userSess;
	EnrollmentData[] regdStudents;
	List<String> enrolledStudentNums;
	synchronized (session) {
		regdStudents = (EnrollmentData[]) session.getAttribute("regdStudents");
		enrolledStudentNums = 
				(ArrayList<String>) session.getAttribute("enrolledStudentNums");
	}
	final String chosenStudentsConcat = request.getParameter("chosenStudents");
		// x:y:z where x, y, z, etc. are indices of students in the enrollment list
	final String[] chosenStudentsStrs = chosenStudentsConcat.split(":");
	// if a chosen student is already enrolled (shouldn't happen), remove from list
	final List<Integer> chosenStudentNums = new ArrayList<Integer>();
	for (final String chosenStudentStr : chosenStudentsStrs) {
		final int chosenStudentIndex = MathUtils.parseInt(chosenStudentStr);
		final String studentNum = regdStudents[chosenStudentIndex - 1].getStudentNum();
		if (studentNum != null && !enrolledStudentNums.contains(studentNum)) 
			chosenStudentNums.add(Integer.valueOf(chosenStudentIndex));
	} // for each chosen student
	// add the remaining students to the enrollment list
   	instrSess.enrollRegistered(regdStudents, chosenStudentNums);

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
<title>ACE DEV-5.1</title>
<script type="text/javascript">
	// <!-- >
	function finish() {
		self.location.href = 'listEnrollment.jsp';
	}
	// -->
</script>
</head>
<body onload="finish();">
</body>
</html>
