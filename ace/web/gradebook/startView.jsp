<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.db.UserRead,
	com.epoch.session.HWSession,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache, must-revalidate"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final int hwId = MathUtils.parseInt(request.getParameter("hwId"));
	final int qId = MathUtils.parseInt(request.getParameter("qId"));
	final int qNum = MathUtils.parseInt(request.getParameter("qNum"));
	final boolean isInstructorOrTA = 
			"true".equals(request.getParameter("isInstructorView"));
	String userId = request.getParameter("userId");
	/* Utils.alwaysPrint("startView.jsp: isInstructorOrTA = ", isInstructorOrTA, 
			"; opening homework session for ", 
			userId == null ? "instructor or TA"
				: isInstructorOrTA ? "instructor or TA with student " + userId
				: "student " + userId, 
			" on Q ", qId); /**/
	final User viewer = (userId == null ? user : UserRead.getUser(userId));
	if (userId != null && isInstructorOrTA){
		viewer.setLanguages(user.getLanguages());
	} // if instructor looking at single student's record
	final HWSession hwsession = 
			new HWSession(viewer, hwId, qId, isInstructorOrTA);
	 
	synchronized (session) {
		session.setAttribute("hwsession", hwsession);
		session.setAttribute("dueDatePast", Boolean.FALSE);
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
	<script src="../js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	function doIt() {
		document.viewForm.submit();
	}
	</script>
</head>
<body onload="doIt();">
<form name="viewForm" action="<%= pathToRoot %>homework/jumpGo.jsp" method="post">
  	<input type="hidden" name="qId" value="<%= qId %>" />
  	<input type="hidden" name="qNum" value="<%= qNum == 0 ? 1 : -qNum %>" />
  	<input type="hidden" name="mode" value="<%= HWSession.GRADEBOOK_VIEW %>" />
  	<input type="hidden" name="isInstructorOrTA" value="<%= isInstructorOrTA %>" />
</form>
</body>
</html>

