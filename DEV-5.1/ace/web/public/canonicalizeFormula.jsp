<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.physics.EquationFunctions,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String enteredEqn = Utils.inputToCERs(request.getParameter("enteredEqn"));
	final boolean entered = enteredEqn != null;
	final String canonicalized = (entered
			? EquationFunctions.formatExpression(enteredEqn)
			: null);
	Utils.alwaysPrint("canonicalizeFormula.jsp: enteredEqn = ", enteredEqn,
			", canonicalized = ", canonicalized);

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<title>ACE Formula and unit canonicalization tester</title>
<link rel="stylesheet" href="../includes/epoch.css" type="text/css"/>
<script src="../js/jslib.js" type="text/javascript"></script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="canonicalizeFormula.jsp" method="post">
<P class="boldtext big" align=center>
ACE Formula and unit canonicalization tester
</P>
<table>
<tr><td class="regtext"  colspan=2>
	<% if (entered) { %>
		You entered: 
		<blockquote>
			<%= Utils.toValidTextbox(enteredEqn) %>
		</blockquote>
		It has been converted to:
		<blockquote><%= Utils.toValidTextbox(canonicalized) %></blockquote>
		<P>Try again, if you like.
	<% } else { %>
		Enter a mathematical equation and see how ACE canonicalizes units.  
	<% } %>
</td></tr>
<tr><td>
	&nbsp;
</td></tr>

<tr><td>
	<input type="text" size="100" name="enteredEqn"/>
</td></tr>

<tr><td>
	<input type="button" value="Submit" onclick="document.tester.submit();"/>
</td></tr>

<tr>
<td align="left" class="regtext" >
<br/><br/><a href="welcome.html">Back</a> to public ACE pages.
</td>
</tr>

</table>
</form>

</body>
</html>
