<%-- Set the content type header with the JSP directive 
--%><%@ page contentType = "application/vnd.ms-excel" 
%><%@ page language="java" 
%><%@ page errorPage="/errormsgs/errorHandler.jsp" 
%><%@ page import="
	com.epoch.courseware.EnrollmentData,
	com.epoch.utils.Utils"
%><%@ include file="/navigation/menuHeaderJava.jsp.h" 
%><%@ include file="/navigation/courseSidebarJava.jsp.h" 
%><% 		
	EnrollmentData[] students = null;
	boolean multiinstitution = false;
	InstructorSession instrSess;
	switch (role) {
		case User.ADMINISTRATOR:
			%> <jsp:forward page="/errormsgs/noSession.html" /> <%
			break;
		case User.INSTRUCTOR:
			instrSess = (InstructorSession) userSess;
			students = instrSess.getEnrolledStudents();
			multiinstitution = instrSess.courseIsMultiinstitution();
			break;
		case User.STUDENT:
			if (!isTA) {
				%> <jsp:forward page="/errormsgs/noSession.html" /> <%
			} // if not a TA
			final StudentSession studSess = (StudentSession) userSess;
			final int courseId = studSess.getSelectedCourseId();
			instrSess = new InstructorSession(courseId, user);
			students = instrSess.getEnrolledStudents();
			multiinstitution = instrSess.courseIsMultiinstitution();
			break;
	} // switch role
	String uniqLabel = user.getInstitutionStudentNumLabel();
	if (multiinstitution) uniqLabel += " (or other)";

	// Set the content disposition header
	response.setHeader("Content-Disposition", 
			Utils.toString("attachment; filename=\"", 
				Utils.cersToUnicode(course.getName()),
				"_Enrollment.xls\"")); 

%><%= Utils.cersToUnicode(user.translate("Name")) %>	<%= 
	Utils.cersToUnicode(uniqLabel) %>	<%= 
	Utils.cersToUnicode(user.translate("Login name")) %>	<%= 
	Utils.cersToUnicode(user.translate("Email")) %>	<%
	if (multiinstitution) { %><%= 
		Utils.cersToUnicode(user.translate("Institution")) %>	<% 
	} %><%= 
	Utils.cersToUnicode(user.translate("Registered")) 
%>
<% for (final EnrollmentData student : students) {
%><%= 
	Utils.cersToUnicode(student.getName()) 
			+ (!student.isTA() ? "" : " (TA)") %>	<%= 
			student.getStudentNum() %><% 	
	if (student.isRegistered()) { %>	<%= 
			Utils.cersToUnicode(student.getUserId()) %>	<%= 
			Utils.cersToUnicode(student.getEmail()) %>	<%
		if (multiinstitution) { %><%= 
				Utils.cersToUnicode(student.getInstitutionName()) %>	<% } %><%= 
				Utils.cersToUnicode(user.translate("Yes")) 
%><% } else { %>	-----	-----	<%
		if (multiinstitution) { %><%= 
			Utils.cersToUnicode(student.getInstitutionName()) %>	<% 
		} // if multiinstitution
%><%=	Utils.cersToUnicode(user.translate("No"))
%><% } // if registered %>
<% } // for each student %>
