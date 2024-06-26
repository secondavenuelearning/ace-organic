<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.access.EpochEntry,
	com.epoch.session.HWCreateSession"
%>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../";

	final EpochEntry entry = new EpochEntry(user.getUserId());
	// use course owner to establish session in case instructor is coinstructor
	final HWCreateSession hwCreator = new HWCreateSession(course.getOwnerId()); 
	hwCreator.assgt.courseId = course.getId();
	synchronized (session) {
		session.setAttribute("entry", entry);
		session.setAttribute("hwCreator", hwCreator);
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

	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
		// <!-- >
		function loadEditor() {
			setCookie('epoch_hwCreator_rxnConds', '');
			self.location.href = 'hwEditor.jsp';
		}
		// -->
	</script>
</head>
	
<body onload="loadEditor();">
</body>
</html>

