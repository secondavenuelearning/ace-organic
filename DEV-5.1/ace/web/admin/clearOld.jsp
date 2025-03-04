<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import=" 
	java.util.ArrayList, 
	java.util.List,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	final String pathToRoot = "../";
	if (role != User.ADMINISTRATOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	
	final int institutionId = 
			MathUtils.parseInt(request.getParameter("institutionId"));
	final boolean clearResponses = // other value is "users"
			"responses".equals(request.getParameter("recordType"));
	final String year = Utils.trim(request.getParameter("year"));
	final String month = Utils.trim(request.getParameter("month"));
	final String day = Utils.trim(request.getParameter("day"));
	final AdminSession admSess = (AdminSession) userSess;
	int numCleared = 0;
	List<String[]> removedStudents = new ArrayList<String[]>();
	if (clearResponses) {
		numCleared = admSess.removeOldResponses(
				new String[] {year, month, day}, institutionId);
	} else {
		removedStudents = 
	 			admSess.removeInactiveStudents(
						new String[] {year, month, day}, institutionId);
		numCleared = removedStudents.size();
		Utils.alwaysPrint("clearOld.jsp: removedStudents:\n", removedStudents);
	}
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<meta http-equiv="Pragma" content="no-cache"/>
<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<title>ACE Clear <%= clearResponses ? "old responses" : "inactive students" %></title>
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<style type="text/css">
	* html body {
		padding:55px 0 0px 0; 
	}
</style>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script type="text/javascript">
// <!-- >
<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
// -->
function goBackToAdmin() {
	alert('<%= numCleared %> <%= clearResponses 
			? "response(s) cleared" : "inactive students removed" %>.');
	self.location.href = 'listProfiles.jsp';
}
</script>
</head>
<body onload="javascript:goBackToAdmin();">
</body>
</html>
