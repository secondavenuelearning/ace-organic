<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.session.HWCreateSession,
	com.epoch.utils.DateUtils,
	com.epoch.utils.Utils,
	java.util.Date,
	java.util.TimeZone"
%>

<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>

<%
	request.setCharacterEncoding("UTF-8");
	final TimeZone zone = course.getTimeZone();
	final String dueDate_Date = request.getParameter("duedate_date");
	final String dueDate_Time = request.getParameter("duedate_time");
	final Date dueDate = DateUtils.parseDate(dueDate_Date, dueDate_Time, zone,
			user.prefersDay1st());
	if (dueDate == null) { %>
		<%= user.translate("Invalid due date or time. "
				+ "Click your browser's \"Back\" button "
				+ "and reenter the date or time.") %>
	<%
		return;
	}
	final String extensions = request.getParameter("extensions");
	for (int hwNum = 0; hwNum < assgts.length; hwNum++) {
		final int hwId = assgts[hwNum].id;
		final HWCreateSession hwCreator = new HWCreateSession(hwId); 
		final Assgt assgt = hwCreator.assgt;
		if (course.isExam() || assgt.isExam()) {
			assgt.setDueDate(dueDate);
			assgt.setExtensions(extensions);
			final boolean EXISTING_ASSGT = false;
			final boolean VISIBILITY_UNCHANGED = false;
			hwCreator.save(EXISTING_ASSGT, VISIBILITY_UNCHANGED);
		} // if course or assignment is exam
	} // for each assgt
	(role == User.ADMINISTRATOR
			? (AdminSession) userSess : (InstructorSession) userSess
			).refreshAssgts();
	/* Utils.alwaysPrint("saveExamData.jsp: due date = ", assgts[0].getDueDate(),
			", extensions = ", assgts[0].getExtensions()); /**/

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
	function finish() {
		this.location.href = 'hwSetList.jsp';
	} // finish();
	// -->
	</script>
</head>
<body onload="finish();">
</body>
</html>
