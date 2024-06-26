<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.EnrollmentData,
	com.epoch.courseware.Name,
	com.epoch.exceptions.DBException,
	com.epoch.servlet.Base64Coder,
	com.epoch.utils.Utils"
%>

<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%	
	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	request.setCharacterEncoding("UTF-8");

	final String namesStr = request.getParameter("names");
	final String pwdsStr = request.getParameter("passwds");
	final boolean pwdsSame = "true".equals(request.getParameter("pwdsSame"));
	final String[] names = namesStr.split("\\.");
	final String[] pwds = pwdsStr.split("\\.");
	final int numStudents = names.length;
	final User[] newStudents = new User[numStudents];
	final EnrollmentData[] newEnrollData = new EnrollmentData[numStudents];
	for (int studNum = 0; studNum < numStudents; studNum++) {
		// names serve as login IDs, passwords, first names, student ID numbers
		newEnrollData[studNum] = new EnrollmentData();
		final byte[] hashValueArr = Base64Coder.decode(pwds[studNum]);
		newStudents[studNum] = new User(names[studNum], hashValueArr);
		newStudents[studNum].setStudentNum(names[studNum]);
		newStudents[studNum].setName(new Name(names[studNum], "", User.RANDOM_SURNAME));
		newStudents[studNum].setInstitution(user.getInstitution()); // instructor's
		if (!pwdsSame) newStudents[studNum].disallowChangePwd();
		final String domain = user.getEmail().split("@")[1];
		newStudents[studNum].setEmail(names[studNum] + "@" + domain);
		newEnrollData[studNum].setName(newStudents[studNum].getName().toString());
		newEnrollData[studNum].setStudentNum(newStudents[studNum].getStudentNum());
	} // for each invented student
	final InstructorSession instrSess = (InstructorSession) userSess;
	try {
		instrSess.addExamStudents(newStudents);
		instrSess.saveBatchEnrollment(newEnrollData);
		Utils.alwaysPrint("inventFinish.jsp: added users");
	} catch (DBException e) {
		// shouldn't happen
		Utils.alwaysPrint("inventFinish.jsp: DBException while trying to add users.");
	} // try
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
	// <!-- >
	function goBackAgain() {
		self.location.href = 'listEnrollment.jsp?refresh=true';
	}
	// -->
	</script>
</head>

<body class="light" style="background-color:white;" onload="goBackAgain()">
</body>
</html>
