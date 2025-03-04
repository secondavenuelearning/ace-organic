<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.courseware.User,
	com.epoch.servlet.Base64Coder,
	com.epoch.session.UserSession,
	com.epoch.utils.Utils"
%>
<%	final String pathToRoot = "../";
	String chosenLang = request.getParameter("language");
	if (chosenLang == null && !"English".equals(AppConfig.defaultLanguage)) {
		chosenLang = AppConfig.defaultLanguage;
	}
	// Note: password hash is Base64 decoded.
	final String userId = request.getParameter("userId");
	final byte[] hashValueArr = 
			Base64Coder.decode(request.getParameter("password"));
	final User editUser = new User(userId, hashValueArr);
	final UserSession userSess = new UserSession(editUser);
	userSess.setPassword();
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
	<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Password Reset</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >
	function goBack() {
		this.location.href = '<%= pathToRoot %>login.jsp?language=<%= 
				Utils.toValidURI(chosenLang) %>';
	}
	// -->
	</script>
</head>
<body class="light" onload="goBack();">
</body>
</html>
