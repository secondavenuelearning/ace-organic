<%@ page language="java" %>
<%@ page import="com.epoch.exceptions.*" %>
<%@ page isErrorPage="true" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%><%

	//get the instance of exception

	String msg = "";
	boolean doReport = true;
	boolean add = true;
	if (exception != null) exception.printStackTrace();

	if (exception instanceof LicenseException) {
		msg = "<font size=2>Maximum searches permitted by the LICENSE is exceeded.<br/>";
		msg += " <a href=\"javascript:history.back()\"> Back to toolbar </a> ";
		msg += "</font>";
		doReport = false;
		add = false;
	} else if (exception instanceof ConfigurationException) {
		msg = " <u>Configuration error:</u><br/>"; 
	} else if (exception instanceof DBException) {
        msg = " <u>DB error:</u><br/>";
    } else if (exception instanceof InvalidOpException) {
        msg = " <u>Invalid operation:</u><br/>";
    } else if (exception instanceof LimitException) {
        msg = " <u>Operation disallowed by predefined limit:</u><br/>";
    } else if (exception instanceof NonExistentException ) {
        msg = " <u>The item you requested is nonexistent:</u><br/>";
	} else if (exception instanceof ParameterException ) {
        msg = " <u>Invalid parameters:</u><br/>";
    } else if (exception instanceof VerifyException ) {
        msg = " <u>Cannot verify response:</u><br/>";
    } else {
		msg = " <u>Generic error:</u><br/>"
                + "<i>" + exception.toString() + "<br/></i>"
				+ "<p>The details:<p>" + exception.getMessage();
		add = false;
	}

	if (add) msg += "<i>" + exception.getMessage() + "<br/></i>";
%>


<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<link rel="stylesheet" href="../includes/epoch.css" type="text/css" />
</head>
<body>
<font color="red">
<% if (doReport) { %>
<b>An error occurred while processing your request.</b><br/> 
Please report this error and the following message to the administrator.<br/>
<% } %>
</font>
<br/>
<%= msg %>
</body>
</html>

