<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.User,
	com.epoch.db.UserRead,
	com.epoch.qBank.Question,
	com.epoch.session.HWSession,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%
	response.setHeader("Cache-Control","no-cache, must-revalidate"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	String userId;
	Question question;
	String qTypeStr;
	String qFlagsStr;
	String qStmt;
	synchronized (session) {
		userId = (String) session.getAttribute("userId");
		question = (Question) session.getAttribute("qBuffer");
		qTypeStr = (String) session.getAttribute("qType");
		qFlagsStr = (String) session.getAttribute("qFlags");
		qStmt = Utils.toValidHTMLAttributeValue(
				(String) session.getAttribute("qStmt"));
		// Utils.alwaysPrint("startPreview.jsp: qStmt = ", qStmt);
	}
	question.setQType(MathUtils.parseInt(qTypeStr));
	question.setQFlags(MathUtils.parseLong(qFlagsStr));
	question.setQSetId(MathUtils.parseInt(request.getParameter("qSetId")));
	final boolean masterEdit = "true".equals(request.getParameter("masterEdit"));
	final User user = UserRead.getUser(userId);
	final HWSession hwsession = new HWSession(user, question, masterEdit);
	synchronized (session) {
		session.setAttribute("hwsession", hwsession);
		session.setAttribute("title", hwsession.getHW().getName());
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
		document.previewForm.submit();
	}
	// -->
	</script>
</head>
<body onload="doIt();">
<form name="previewForm" action="../homework/jumpGo.jsp" method="post">
  	<input type="hidden" name="qNum" value="1" />
  	<input type="hidden" name="mode" value="<%= HWSession.PREVIEW %>" />
  	<input type="hidden" name="qStmt" value="<%= qStmt %>" />
</form>
</body>
</html>

