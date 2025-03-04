<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.physics.EquationFunctions,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String enteredNum = Utils.inputToCERs(request.getParameter("enteredNum"));
	int numSigFigs = 0;
	int[] numSigFigParts = new int[] {0, 0};
	final boolean entered = enteredNum != null;
	if (entered) {
		numSigFigs = MathUtils.countSigFigs(enteredNum.trim());
		numSigFigParts = MathUtils.countPartSigFigs(enteredNum.trim());
		Utils.alwaysPrint("countSigFigs.jsp: enteredNum = ", enteredNum,
				", numSigFigs = ", numSigFigs,
				", numSigFigParts = ", numSigFigParts);
	} // if entered

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<title>ACE Significant figure counter</title>
<link rel="stylesheet" href="../includes/epoch.css" type="text/css"/>
<script src="../js/jslib.js" type="text/javascript"></script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="countSigFigs.jsp" method="post">
<P class="boldtext big" align=center>
ACE Significant figure counter
</P>
<table>
<tr><td class="regtext"  colspan=2>
	<% if (entered) { %>
		The number <%= enteredNum %>
		has <%= numSigFigs %> significant digits, of which
		<%= numSigFigParts[0] %> are before the decimal point, and
		<%= numSigFigParts[1] %> are after.
		<P>Try again, if you like.
	<% } else { %>
		Enter a number and see how ACE counts significant figures.  
	<% } %>
</td></tr>
<tr><td>
	&nbsp;
</td></tr>

<tr><td>
	<input type="text" size="100" name="enteredNum"/>
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
