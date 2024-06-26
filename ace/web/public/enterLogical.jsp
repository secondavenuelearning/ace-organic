<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page language="java" %>
<%@ page import="
	com.epoch.utils.Utils"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../";

	String[] stmts = new String[] {
			"S is below O in the periodic table",
			"S is bigger than O"
			};
	if (Utils.isEmpty(stmts)) stmts = new String[] {""};
%>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Logical Statements Editor</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
		* html body {
			padding:100px 0 55px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/md5.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/logicStmts.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/wordCheck.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >

	function setUpStmts() {
		var stmts = new Array();
		<% for (final String stmt : stmts) { %>
			stmts.push('<%= Utils.toValidJS(stmt) %>');
		<% } // for each current statement %>
		setStmts(stmts);
	} // setUpStmts()

	function submitThis() {
		var blankAlert = 'Please do not submit any blank statements.';
		var unknownAlert = 'Please do not submit any statements that contain '
				+ 'unknown words (those struck through and in red).';
		document.editform.statements.value = 
				getStmtsXML(blankAlert, unknownAlert);
		if (!isEmpty(document.editform.statements.value))
			alert(document.editform.statements.value);
	} // submitThis()

	// -->
	</script>
</head>

<body style="text-align:center; background-color:#FFFFFF;" onload="setUpStmts();">

<form name="editform" action="saveHW.jsp" method="post">
	<input type="hidden" name="statements" value=""/>
<table class="whitetable" style="background-color:#f6f7ed; width:90%;
		margin-left:auto; margin-right:auto;">
	<tr><td class="boldtext big" style="vertical-align:top; 
			padding-bottom:10px;">
		Logical statements
	</td></tr>
	<tr><td id="stmtsTable" class="regtext">
	</td></tr>
	<td style="padding-top:10px;">
		<%= makeButton("Submit", "submitThis();") %> 
	</td></tr>
</table>
</form>
</body>
</html>
