<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.access.EpochEntry,
	com.epoch.session.HWCreateSession,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	if (!Utils.among(role, User.ADMINISTRATOR, User.INSTRUCTOR) && !isTA) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final EpochEntry entry = new EpochEntry(user.getUserId());
	final int hwNum = MathUtils.parseInt(request.getParameter("hwNum"));
	final Assgt origDescr = assgts[hwNum - 1];
	final int hwId = origDescr.id;
	final HWCreateSession hwCreator = new HWCreateSession(hwId); 
	synchronized (session) {
		session.setAttribute("entry", entry);
		session.setAttribute("hwCreator", hwCreator);
		session.setAttribute("isNewAssgt", false);
	}
	final String editAction = request.getParameter("editAction");

%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
	<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />

	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
		// <!-- >
		function loadEditor() {
			var go = 'hwEditor.jsp?hwNum=<%= hwNum %><%= editAction != null 
					? "&editAction=" + editAction : "" %>';
			self.location.href = go;
		}
		// -->
	</script>

</head>
<body onload="loadEditor()">
</body>
</html>

