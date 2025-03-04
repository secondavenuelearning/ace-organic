<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.session.HWSession,
	com.epoch.textbooks.Textbook,
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

	Textbook book;
	synchronized (session) {
		book = (Textbook) session.getAttribute("textbook");
	} // synchronized
	final int qId = MathUtils.parseInt(request.getParameter("qId"));
	final boolean isInstructorOrTA = 
			"true".equals(request.getParameter("isInstructorOrTA"));
	final String ownerId = book.getOwnerId();
	Utils.alwaysPrint("startQuestion.jsp: qId = ", qId, ", ownerId = ",
			ownerId, ", isInstructorOrTA = ", isInstructorOrTA); /**/
	final HWSession hwsession = new HWSession(ownerId, qId, user);
	 
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
	// <!-- >
	function doIt() {
		document.viewForm.submit();
	}
	</script>
	// -->
</head>
<body onload="doIt();">
<form name="viewForm" action="<%= pathToRoot %>homework/jumpGo.jsp" method="post">
  	<input type="hidden" name="qId" value="<%= qId %>" />
  	<input type="hidden" name="qNum" value="1" />
  	<input type="hidden" name="mode" value="<%= HWSession.TEXTBOOK %>" />
  	<input type="hidden" name="isInstructorOrTA" value="<%= isInstructorOrTA %>" />
</form>
</body>
</html>

