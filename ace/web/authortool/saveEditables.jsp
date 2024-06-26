<%@ page language="java" %>
<%@ page import="
	com.epoch.utils.Utils"
%>

<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final String qTypeStr = request.getParameter("qType");
	final String qFlagsStr = request.getParameter("qFlags");
	final String qStmt = Utils.inputToCERs(request.getParameter("qStmt"));
	final String book = request.getParameter("book");
	final String chapter = Utils.inputToCERs(request.getParameter("chapter"));
	final String bookQNumber = Utils.inputToCERs(request.getParameter("bookQNumber"));
	final String keywords = Utils.inputToCERs(request.getParameter("keywords"));
	synchronized (session) {
		if (qTypeStr != null) session.setAttribute("qType", qTypeStr);
		if (qFlagsStr != null) session.setAttribute("qFlags", qFlagsStr);
		if (qStmt != null) session.setAttribute("qStmt", qStmt);
		if (book != null) session.setAttribute("book", book);
		if (chapter != null) session.setAttribute("chapter", chapter);
		if (bookQNumber != null) session.setAttribute("bookQNumber", bookQNumber);
		if (keywords != null) session.setAttribute("keywords", keywords);
	}
	/* Utils.alwaysPrint("saveEditables.jsp: saving qType ", qTypeStr, 
			", qFlags ", qFlagsStr); /**/
	final String destination = request.getParameter("destination");
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
		function goToDestination() {
			self.location.href = '<%= Utils.toValidJS(destination) %>'; 
		}
		// -->
	</script>
</head>
<body onload="goToDestination();">
</body>
</html>

