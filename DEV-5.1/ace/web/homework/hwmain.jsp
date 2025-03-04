<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.access.EpochEntry,
	com.epoch.session.HWSession,
	com.epoch.utils.MathUtils,
	java.util.ArrayList,
	java.util.List"
%>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache, must-revalidate"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final String userId = user.getUserId();
	final EpochEntry entry = new EpochEntry(userId);
	final int courseId = course.getId();

	final int hwNum = MathUtils.parseInt(request.getParameter("hwNum")); // 1-based
	final int goToQId = MathUtils.parseInt(request.getParameter("goToQId")); // 1-based
	final Assgt assgt = assgts[hwNum - 1];
	final boolean isTAOrCoinstructor = isTA || role == User.INSTRUCTOR;
	final boolean mayNotSeeSynthCalcdProds =
			!isTAOrCoinstructor && course.hideSynthCalcdProds();
	final HWSession hwsession = new HWSession(user, assgt, isTAOrCoinstructor, 
			request.getRemoteAddr(), mayNotSeeSynthCalcdProds);
	synchronized (session) {
		if (assgt.isExam() || assgt.logsAllToDisk()) {
			hwsession.prepareForExam(course.getTimeZone(), request.getRemoteHost());
		} // if this is an exam
		session.setAttribute("hwsession", hwsession);
		session.setAttribute("entry", entry);
	} // synchronized

%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>Main homework page</title>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
		// <!-- >
		function moveAlong() {
			var go = 'list.jsp<%= goToQId == 0 ? "" : "#Q" + goToQId %>';
			self.location.href = go;
		}
		// -->
	</script>
</head>
<body onload="moveAlong();">
</body>
</html>

