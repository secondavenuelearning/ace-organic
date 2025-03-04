<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page language="java" %>
<%@ page import="
	java.util.List,
	java.util.ArrayList,
	com.epoch.physics.Equations,
	com.epoch.utils.Utils"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final List<String> eqnsList = new ArrayList<String>();
	int eqnNum = 1;
	while (true) {
		final String eqnName = "eqn" + eqnNum;
		Utils.alwaysPrint("enterEqns.jsp: eqnName = ", eqnName);
		final String eqn = request.getParameter(eqnName);
		if (eqn != null) eqnsList.add(eqn);
		else break;
		eqnNum++;
	} // while
	final String[] eqns = eqnsList.toArray(new String[eqnsList.size()]);
	Utils.alwaysPrint("enterEqns.jsp: eqns = ", eqns);
	/**/
	// final String[] eqns = new String[] {};
%>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Equations Editor</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
		* html body {
			padding:100px 0 55px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/md5.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/equations.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>nosession/mathjax/MathJax.js?config=TeX-AMS_HTML-full" 
			type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >

 	<%@ include file="/js/equations.jsp.h" %>

	function setUpEqns() {
		initEqnConstants();
		var eqns = new Array();
		<% for (final String eqn : eqns) { %>
			eqns.push('<%= Utils.toValidJS(eqn) %>');
		<% } // for each current equation %>
		setEqns(eqns);
	} // setUpEqns()

	function seeXML() {
		var blankAlert = 'Please do not submit any blank equations.';
		var unknownAlert = 'Please do not submit any equations that contain '
				+ 'unknown words (those struck through and in red).';
		document.eqnsForm.equations.value = 
				getEqnsXML(blankAlert, unknownAlert);
		if (!isEmpty(document.eqnsForm.equations.value))
			alert(document.eqnsForm.equations.value);
	} // submitThis()

	// -->
	</script>
</head>

<body style="text-align:center; background-color:#FFFFFF;" onload="setUpEqns();">

	When \(a \ne 0\), there are two solutions to \(ax^2 + bx + c = 0\) and they
	are
	$$x = {-b \pm \sqrt{b^2-4ac} \over 2a}.$$
<form name="eqnsForm" action="enterEqns.jsp" method="post">
	<input type="hidden" name="equations" value=""/>
<table class="whitetable" style="background-color:#f6f7ed; width:90%;
		margin-left:auto; margin-right:auto;">
	<tr><td class="boldtext big" style="vertical-align:top; 
			padding-bottom:10px;">
		Equations
	</td></tr>
	<tr><td>
		<table class="whitetable" style="width:100%;">
		<tr>
		<td id="eqnsTable" class="regtext" style="width:50%;">
		</td>
		</tr>
	</td></tr>

	<td style="padding-top:10px;">
		<%= makeButton("See XML", "seeXML();") %>
	</td></tr>
	<tr><td class="regtext" style="vertical-align:top; padding-bottom:10px; padding-top:20px;">
		<a href="<%= pathToRoot %>nosession/mathjax/test/examples.html">MathJax example pages</a>
	</td></tr>
</table>
</form>
</body>
</html>
