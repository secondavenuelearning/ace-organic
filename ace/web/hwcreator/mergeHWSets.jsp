<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.session.ExportImportSession,
	com.epoch.utils.Utils,
	java.io.DataOutputStream,
	java.io.FileOutputStream"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	final String pathToRoot = "../";
	if (!Utils.among(role, User.ADMINISTRATOR, User.INSTRUCTOR)) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	request.setCharacterEncoding("UTF-8");

	// merge one or more assignments  
	final String hwNumsStr = request.getParameter("hwNums");
	// Utils.alwaysPrint("exportHWSets.jsp: hwNumsStr = ", hwNumsStr);
	final String[] hwNumStrs = hwNumsStr.split(":");
	final int[] hwNums = Utils.stringToIntArray(hwNumStrs);
	(role == User.ADMINISTRATOR ? (AdminSession) userSess
			: (InstructorSession) userSess).mergeAssgts(hwNums);

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
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Assignments Merge</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
		* html body {
			padding:100px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >
	function goBackAgain() {
		this.location.href = 'hwSetList.jsp';
	}
	// -->
	</script>
</head>

<body class="light" style="background-color:white;" onload="goBackAgain();">
</body>
</html>
