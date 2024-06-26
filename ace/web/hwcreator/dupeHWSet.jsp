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
	if (!Utils.among(role, User.ADMINISTRATOR, User.INSTRUCTOR)) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	request.setCharacterEncoding("UTF-8");

	final EpochEntry entry = new EpochEntry(user.getUserId());
	final int hwNum = MathUtils.parseInt(request.getParameter("hwNum"));
	final Assgt origAssgt = assgts[hwNum - 1];
	final int hwId = origAssgt.id;
	final HWCreateSession hwCreator = new HWCreateSession(hwId);
	final HWCreateSession dupeCreator = new HWCreateSession(0, hwCreator);
	final Assgt dupeAssgt = dupeCreator.assgt;
	dupeAssgt.setName(Utils.toString(user.translate("New"), ' ', 
			dupeAssgt.getName()));
	dupeCreator.save();
	dupeAssgt.setPage2ValuesChanged(); // so they will be saved even if 
									// they haven't changed from original
	synchronized (session) {
		session.setAttribute("entry", entry);
		session.setAttribute("hwCreator", dupeCreator);
		session.setAttribute("isNewAssgt", true);
	}
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<script type="text/javascript">
		// <!-- >
		function returnMe() {
			self.location.href = 'editHWProps.jsp';
		}
		// -->
	</script>
</head>
<body onload="returnMe();">
</body>
</html>
