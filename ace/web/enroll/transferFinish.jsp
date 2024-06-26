<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.EnrollmentData,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.List"
%>

<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%	
	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	request.setCharacterEncoding("UTF-8");
	ArrayList<EnrollmentData> unmatchedExamStudents;
	ArrayList<EnrollmentData> unmatchedRegCourseStudents;
	synchronized (session) {
		unmatchedExamStudents = 
				(ArrayList<EnrollmentData>)
				session.getAttribute("unmatchedExamStudents");
		unmatchedRegCourseStudents = 
				(ArrayList<EnrollmentData>)
				session.getAttribute("unmatchedRegCourseStudents");
		session.removeAttribute("unmatchedExamStudents");
		session.removeAttribute("unmatchedRegCourseStudents");
	}

	final String matches = request.getParameter("matches");
	final String[] matchesArray = matches.split(";");
	Utils.alwaysPrint("transferFinish.jsp: ", matchesArray.length,
			" chosen match(es): ", matches);
	final List<String[]> matchedStudents = new ArrayList<String[]>();
	for (final String matchedPair : matchesArray) {
		final String[] studentNumStrs = matchedPair.split(":");
		final int examStudentNum = MathUtils.parseInt(studentNumStrs[0]);
		final int regCourseStudentNum = MathUtils.parseInt(studentNumStrs[1]);
		final EnrollmentData examStudent = 
				unmatchedExamStudents.get(examStudentNum);
		final EnrollmentData regCourseStudent = 
				unmatchedRegCourseStudents.get(regCourseStudentNum);
		final String[] match = new String[] {
				examStudent.getName() + " alias " + regCourseStudent.getName(),
				examStudent.getStudentNum() + " alias " + regCourseStudent.getStudentNum(),
				examStudent.getUserId(),
				regCourseStudent.getUserId()
				};
		if (!match[2].equals(match[3])) {
			Utils.alwaysPrint("transferFinish.jsp: matching ", examStudent.getName(),
					", ", examStudent.getStudentNum(), ", ",
					examStudent.getUserId(), " to ", regCourseStudent.getName(),
					", ", regCourseStudent.getStudentNum(),
					", ", regCourseStudent.getUserId());
			matchedStudents.add(match);
		} else Utils.alwaysPrint("transferFinish.jsp: ", examStudent.getName(),
				", ", examStudent.getStudentNum(), 
				" and ", regCourseStudent.getName(), ", ", 
				regCourseStudent.getStudentNum(), " have same login ID ", 
				regCourseStudent.getUserId(), "; cannot transfer work.");
	} // for each match

	final InstructorSession instrSess = (InstructorSession) userSess;
	instrSess.transferWork(matchedStudents, 2, 3); 
			// indices of exam login, regular course login in matches

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache">
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT">
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<script type="text/javascript">

	function goOn() {
		self.location.href = 'deleteExamIds.jsp?refresh=true';
	}

	</script>
</head>

<body class="light" style="background-color:white;" onload="goOn()">
</body>
</html>
