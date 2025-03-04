<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.session.ExportImportSession,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	if (!Utils.among(role, User.ADMINISTRATOR, User.INSTRUCTOR)) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}

	final int oldCrsNum = MathUtils.parseInt(request.getParameter("oldCrsNum"));
	final int newCrsIndex = MathUtils.parseInt(request.getParameter("index"));
	final int addDays = MathUtils.parseInt(request.getParameter("addDays"));
	final boolean makeInvisible = "on".equals(request.getParameter("makeInvisible"));
	/* Utils.alwaysPrint("cloneHWSets.jsp: oldCrsNum = ", oldCrsNum, ", addDays = ",
			addDays, ", makeInvisible = ", makeInvisible, ", new course ID = ",
			course.getId(), ", newCrsIndex = ", newCrsIndex); /* */
	Assgt[] oldDescrs = null;
	if (userSess instanceof AdminSession) {
		final AdminSession adminSess = (AdminSession) userSess;
		oldDescrs = adminSess.getHWs(oldCrsNum);
	} else if (userSess instanceof InstructorSession) {
		final InstructorSession instrSess = (InstructorSession) userSess;
		oldDescrs = instrSess.getHWs(oldCrsNum);
	}
	final int numHWs = Utils.getLength(oldDescrs);
	final int[] oldIds = new int[numHWs];
	for (int hwNum = 0; hwNum < numHWs; hwNum++) {
		oldIds[hwNum] = oldDescrs[hwNum].id;
	} // for each assignment to clone
	/* Utils.alwaysPrint("cloneHWSets.jsp: numHWs in original course = ", numHWs, 
			", oldIds = ", oldIds); /* */

	// current course is the new course
	ExportImportSession.cloneHWSets(oldIds, course.getId(), addDays, !makeInvisible);

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
		self.location.href = '<%= pathToRoot %>userHome.jsp';
	}
	// -->
	</script>
</head>
<body onload="finish();">
</body>
</html>

