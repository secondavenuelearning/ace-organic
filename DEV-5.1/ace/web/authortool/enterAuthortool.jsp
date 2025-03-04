<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	
	// Check whether this user is allowed master Edit
	final String userId = user.getUserId();
	final EpochEntry authorEntry = new EpochEntry(userId);
	final boolean getPersonal = request.getParameter("getPersonal") != null;
	if (role == User.ADMINISTRATOR || (user.isMasterAuthor() && !getPersonal)) {
		authorEntry.setMasterEdit();
	}
	synchronized (session) {
		session.setAttribute("userId", userId);
		session.setAttribute("entry", authorEntry);
		session.removeAttribute("qBuffer");
		session.removeAttribute("qSet");
		session.removeAttribute("qBank");
		session.removeAttribute("translationObj");
	} // synchronized

%>

<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
		setCookie('selected_topic', '');
		setCookie('selected_qSet', '');
	</script>
</head>
<body style="text-align:center; background-color:#FFFFFF;" 
		onload="self.location.href='questionsList.jsp'">
<table style="width:100%; height:100%;">
<tr><td class="boldtext" style="text-align:center; vertical-align:middle;">
	<img id="image" src="<%= pathToRoot %>images/ace_anim.gif" alt="wait..." /><br />
	Loading...
</td></tr>
</table>
</body>
</html>
