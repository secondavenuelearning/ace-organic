<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String pphrase = request.getParameter("pphrase");
	final boolean mayEnter = course.checkPassword(pphrase);

%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<title>Entering Course</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	<% if (mayEnter) { %>
		this.location.href = '<%= pathToRoot %>course/courseHome.jsp';
	<% } else { %>
		toAlert('<%= user.translateJS("You entered an incorrect password.") %>');
		this.location.href = '<%= pathToRoot %>userHome.jsp';
	<% } // if the password was authenticated %>
	// -->
</script>
</head>
<body>
</body>
</html>
