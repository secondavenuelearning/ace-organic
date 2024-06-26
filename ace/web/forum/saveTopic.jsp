<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	request.setCharacterEncoding("UTF-8");
	final int topics50 = MathUtils.parseInt(request.getParameter("topics50"), 1);
	final int topicId = MathUtils.parseInt(request.getParameter("topicId"));
	final boolean isSticky = "on".equals(request.getParameter("sticky"));
	final String title = Utils.inputToCERs(request.getParameter("title"));
	final InstructorSession instrSess =
			(role != User.STUDENT ? (InstructorSession) userSess
			: new InstructorSession(course.getId(), user));
	instrSess.setForumTopic(topicId, title, isSticky);

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
</head>
<script type="text/javascript">
	// <!-- >
	function goBack() {
		this.location.href = 'topics.jsp?topicId=<%= topicId %>&topics50=<%= topics50 %>';
	} // goBack()
	// -->
</script>
<body onload="goBack();">
</body>
</html>
