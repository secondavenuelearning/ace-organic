<%@ page import="
	com.epoch.assgts.Assgt,
	com.epoch.courseware.Course"
%><%
	Course course = null;
	Assgt[] assgts = null;
    boolean isInstructor = false;
	boolean isTA = false;

	/* The session can be of an
			- administrator impersonating an instructor or TA or regular student
			- instructor
			- student who is a TA
			- student who is not a TA
	*/
	switch (role) {
		case User.ADMINISTRATOR:
			final AdminSession adminSess = (AdminSession) userSess;
			course = adminSess.getSelectedCourse();
			isInstructor = adminSess.getActedRole() == User.INSTRUCTOR;
			if (!isInstructor) isTA = adminSess.isTA();
			if (course.isExam()) adminSess.refreshAssgts(isInstructor || isTA);
			assgts = adminSess.getHWs();
			break;
		case User.INSTRUCTOR:
			final InstructorSession instrSess = (InstructorSession) userSess;
			course = instrSess.getSelectedCourse(); 
			isInstructor = true;
			if (course.isExam()) instrSess.refreshAssgts(isInstructor);
			assgts = instrSess.getHWs();
			break;
		case User.STUDENT:
		default: // should never happen
			final StudentSession studSess = (StudentSession) userSess;
			course = studSess.getSelectedCourse();
			isTA = studSess.isTA();
			if (!isTA) {
				// make sure student hasn't entered course illicitly
				synchronized (session) {
					final String ipAddr = request.getRemoteAddr();
					if (!course.isEnabled() 
							|| !course.isOkIPAddress(ipAddr, user)) {
						%> <jsp:forward page="/errormsgs/noAccess.html" /> <%
					} // if not allowed entry to this course
				} // synchronized
			} // not a TA
			if (course.isExam() || realRole != User.STUDENT) {
				studSess.refreshAssgts(isTA);
			} // if is exam course or student is impersonated
			assgts = studSess.getHWs();
			break;
	} // switch role

// vim:filetype=jsp
%>
