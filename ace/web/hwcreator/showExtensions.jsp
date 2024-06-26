<%@ page language="java" %>
<%@ page import="
	com.epoch.courseware.EnrollmentData,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.HashMap,
	java.util.List,
	java.util.Map"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final int courseId = course.getId();
	String userId = null; // leave as null if not student
	EnrollmentData[] students = new EnrollmentData[0];
	switch (role) {
		case User.ADMINISTRATOR:
			final AdminSession adminSess = (AdminSession) userSess;
			if (courseId != AppConfig.tutorialId && isInstructor) {
				students = adminSess.getEnrolledStudents();
			} // if instructor and not tutorial course
			break;
		case User.INSTRUCTOR:
			final InstructorSession instrSess = (InstructorSession) userSess;
			if (courseId != AppConfig.tutorialId) {
				students = instrSess.getEnrolledStudents();
			} // if not tutorial course
			break;
		case User.STUDENT:
		default: // shouldn't happen
			userId = user.getUserId();
			final StudentSession studSess = (StudentSession) userSess;
			if (isTA) students = studSess.getEnrolledStudents();
			break;
	} // switch role

	final Map<String, String> nameByUserId = new HashMap<String, String>(); 
	EnrollmentData firstUnusedExamStudent = null;
	for (final EnrollmentData student : students) {
		if (student.isUnusedExamStudent()) {
			if (firstUnusedExamStudent == null) 
				firstUnusedExamStudent = student;
		} else if (student.isRegistered()) 
			nameByUserId.put(student.getUserId(), student.getName());
	} // for each registered student

	final int hwNum = MathUtils.parseInt(request.getParameter("hwNum"));
	final Assgt assgt = assgts[hwNum - 1];
	final Map<String, String> extensions = assgt.getExtensions();

%>
<html>
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>Extensions</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- > avoid parsing the following as HTML

	function closeMe(e) {
		if (e && ([10, 13].contains(e.keyCode))) self.close();
	} // closeMe()

	// --> end HTML comment
	</script>
</head>
<body onkeypress="closeMe(event);" style="overflow:auto;">
<table class="regtext" style="margin-left:auto; margin-right:auto; width:95%;">
<tr><td><% 
	boolean foundExtension = false;
	if (!Utils.isEmpty(extensions)) {
		final List<String> extStudentIds = 
				new ArrayList<String>(extensions.keySet());
		for (final String extStudentId : extStudentIds) {
			final String name = nameByUserId.get(extStudentId);
			if (name != null) {
				String[] substns = null;
				String extensionStr = extensions.get(extStudentId);
				final double extension = 
						MathUtils.parseDouble(extensionStr);
				final boolean isDueDatePast = 
						!assgt.isSolvingAllowed(extStudentId);
				final StringBuilder oneAlertBld = new StringBuilder()
						.append("***Student X*** has an ");
				if (extension == -1.0) {
					oneAlertBld.append("indefinite extension.");
					substns = new String[] {name};
				} else { 
					if (extensionStr.startsWith(".")) {
						extensionStr = Utils.toString('0', extensionStr);
					}
 					Utils.appendTo(oneAlertBld, "extension of ***1*** ",
							assgt.isExam() ? "minute" : "day");
					if (extension != 1.0) oneAlertBld.append('s');
					oneAlertBld.append('.');
					substns = new String[] {name, extensionStr};
				} // if extension is indefinite
				String oneAlert = 
						user.translate(oneAlertBld.toString(), substns);
				if (!assgt.isSolvingAllowed(extStudentId)) {
					oneAlert = Utils.toString("<span style=\"color:#E0E0E0;\">",
							oneAlert, "</span>");
				} // if due date is past
	%>
				<%= oneAlert %><br/>
	<%
				foundExtension = true;
			} // if student is in extension list
		} // for each extension
		if (firstUnusedExamStudent != null) {
			final String studentId = firstUnusedExamStudent.getUserId();
			final String extensionStr = extensions.get(studentId);
			final double extension = 
					MathUtils.parseDouble(extensionStr);
			if (extension != 0.0) {
				foundExtension = true;
				final boolean indefinite = extension == -1.0;
				final StringBuilder oneAlertBld = new StringBuilder()
						.append("Unused exam students have an ");
				if (indefinite) oneAlertBld.append("indefinite extension.");
				else { 
					oneAlertBld.append("extension of ***1*** ")
							.append(assgt.isExam() ? "minute" : "day");
					if (extension != 1.0) oneAlertBld.append('s');
					oneAlertBld.append('.');
				} // if indefinite
	%>
				<br/><%= indefinite 
						? user.translate(oneAlertBld.toString())
						: user.translate(oneAlertBld.toString(), 
							extensionStr) %><br/>
	<%		} // if unused exam students have an extension
		} // if there are unused exam students
	} // if there are extensions 
	if (!foundExtension) { 
	%>
		<%= user.translate("No extensions have been granted.") %>
	<% } // foundExtension 
%></td></tr>
<tr><td>
	<table>
		<tr><td style="width:100%;">&nbsp;
		</td><td><%= makeButton("Close", "self.close();") %>
		</td></tr>
	</table>
</td></tr>
</table>
</body>
</html>
