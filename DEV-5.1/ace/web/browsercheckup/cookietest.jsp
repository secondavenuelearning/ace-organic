<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.AppConfig, 
	com.epoch.courseware.User,
	com.epoch.utils.Utils"
%>
<%	
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final User tempUser = new User();
	String chosenLang = request.getParameter("language");
	if (chosenLang == null && AppConfig.notEnglish) {
		chosenLang = AppConfig.defaultLanguage;
	}
	tempUser.setLanguage(chosenLang);
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
	<head>
		<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
		<script LANGUAGE="javascript" src="<%= pathToRoot %>js/jslib.js"></script>
	</head>
<title>Pop-up Test</title>
<body class="boldtext" style="background-color:E0E6C2; margin:0px;">
<div style="padding:30px;">
<%= tempUser.translate("Cookies are working!") %>
<br /><br /><br />
<%= makeButton(tempUser.translate("Close"), "self.close();") %>
</div>
</body>
</html>
