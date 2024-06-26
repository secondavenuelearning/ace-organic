<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.servlet.GroupLoader,
	com.epoch.servlet.LoginServlet"
%>
<%
	// reinitiate courseware values
	final ServletContext context = getServletContext();
	String appRoot = context.getRealPath("");
	if (!appRoot.endsWith("/")) appRoot += "/";
	final String confFile = appRoot + context.getInitParameter("chimp_conf_file");
	LoginServlet.initConfiguration(confFile);
	GroupLoader.initStandalone(appRoot, confFile);

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
<body>
	<script type="text/javascript">
		// <!-- >
		alert('Done.');
		self.location.href = 'listProfiles.jsp';
		// -->
	</script>
</body>
</html>
