<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	final String pathToRoot = "../";
	if (role != User.ADMINISTRATOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	request.setCharacterEncoding("UTF-8");
	final AdminSession admSess = (AdminSession) userSess;
	final boolean goAhead = "true".equals(request.getParameter("goAhead"));
   	final int numRemoved = (goAhead ? admSess.deleteYearOldImages() : 0);
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Removed Images from Obsolete Posts</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	
	function confirmMe() {
		if (confirm('Do you want to remove <%= admSess.getNumYearOldPostImages() %>'
				+ ' post image(s) one year or older from all '
				+ '<%= admSess.getNumPostImages() %> image(s)?')) {
			self.location.href = 'removeOldPostImages.jsp?goAhead=true';
		} else goToAdmin();
	} // confirmMe()

	function goBack() {
		alert('Removed <%= numRemoved %> image(s).');
		goToAdmin();
	} // goBack()

	// -->
	</script>
</head>
<body onload="<%= goAhead ? "goBack();" : "confirmMe();" %>">
</body>
</html>
