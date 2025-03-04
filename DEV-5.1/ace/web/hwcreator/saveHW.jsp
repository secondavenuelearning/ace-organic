<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.session.HWCreateSession,
	com.epoch.utils.DateUtils,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.Date,
	java.util.TimeZone"
%>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	request.setCharacterEncoding("UTF-8");

	HWCreateSession hwCreator;
	boolean isNewAssgt;
	synchronized (session) {
		hwCreator = (HWCreateSession) session.getAttribute("hwCreator");
		final Boolean isNewAssgtVal = (Boolean) session.getAttribute("isNewAssgt");
		isNewAssgt = isNewAssgtVal != null && isNewAssgtVal.booleanValue();
	}
	final Assgt assgt = hwCreator.assgt;
	/* Utils.alwaysPrint("saveHW.jsp: assgt = ", assgt.toString(), 
			", id = ", assgt.id, ", isNewAssgt = ", isNewAssgt); /**/

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
	final boolean origVisible = assgt.isVisible();
	assgt.setVisible(
			"on".equals(request.getParameter("visible")));
	assgt.setRecordAfterDue(
			"on".equals(request.getParameter("recordAfterDue")));
	assgt.setIsExam(
			"on".equals(request.getParameter("isExam")));
	assgt.setSavePrevTries(
			"on".equals(request.getParameter("savePrevTries")));
	assgt.setDelayGrading(
			"on".equals(request.getParameter("delayGrading")));
	assgt.setShowReferences(
			MathUtils.parseInt(request.getParameter("showRefs")));
	assgt.setExcludeFromTotals(
			"on".equals(request.getParameter("excludeFromTotals")));
	assgt.setDueDate(dueDate);
	assgt.setIsTimed(
			"on".equals(request.getParameter("isTimed")));
	assgt.setLogAllToDisk(
			"on".equals(request.getParameter("logAllResponses")));
	assgt.setShowSaveWOSubmitting(
			"on".equals(request.getParameter("saveWOSubmitting")));
	assgt.setDuration(
			MathUtils.parseInt(request.getParameter("duration")));
	// Utils.alwaysPrint("saveHW.jsp: duration = ", assgt.getDuration());
	assgt.setMaxExtensionStr(request.getParameter("maxExtension"));
	assgt.setExtensions(request.getParameter("extensions"));
	assgt.setGradingParams(
			request.getParameter("attemptGradingParams"), Assgt.ATTEMPT);
	assgt.setGradingParams(
			request.getParameter("timeGradingParams"), Assgt.TIME);

	hwCreator.save(isNewAssgt, !origVisible && assgt.isVisible());
	if (isTA) ((StudentSession) userSess).refreshAssgts(isTA);
	else (role == User.ADMINISTRATOR
			? (AdminSession) userSess : (InstructorSession) userSess
			).refreshAssgts();

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
	</head>
<body>
<script type="text/javascript">
	// <!-- >
	this.location.href = 'hwSetList.jsp';
	// -->
</script>
</body>
</html>
