<%@ page language="java" %>
<%@ page import="
	com.epoch.chem.ChemUtils,
	com.epoch.physics.Maxima,
	com.epoch.utils.Utils,
	chemaxon.struc.PeriodicSystem,
	java.io.IOException"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String expression = request.getParameter("expression");
	String output = "";
	boolean success = true;
	if (expression != null) {
		try {
			final Maxima maximaInstance = Maxima.getMaxima();
			output = maximaInstance.evaluate(expression);
		} catch (IOException e) {
			success = false;
		}
	}
	
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>ACE Maxima tester</title>
<link rel="stylesheet" href="../includes/epoch.css" type="text/css"/>
<script type="text/javascript">

	function sendToMaxima() {
		document.tester.submit();
	}

</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="maximaTest.jsp" method="post">
<p class="boldtext big" style="text-align:center;">
ACE Maxima tester
</P>
<table>
<tr><td class="regtext"  colspan="2">
	<% if (expression == null) { %>
		Enter an expression for 
		<a href="http://maxima.sourceforge.net" target="window2">Maxima</a> 
		to evaluate.
	<% } else if (success) { %>
		You wrote: <blockquote><%= expression %></blockquote>
		<a href="http://maxima.sourceforge.net" target="window2">Maxima</a>'s 
		output is: <blockquote><%= output %></blockquote>
		Try again, if you like.
	<% } else { %>
		<a href="http://maxima.sourceforge.net" target="window2">Maxima</a> 
		could not understand the expression. Try again, if you like.
	<% } %>
</td></tr>

<tr><td>
	<input type="text" size="100" name="expression"/>
</td></tr>
<tr><td>
	<br/>
	<input type="button" value=" Submit " onclick="sendToMaxima()"/>
	<br/>
</td></tr>
<tr><td class="regtext">
	<br/>Visit 
	<a href="http://math-blog.com/2007/06/04/a-10-minute-tutorial-for-solving-math-problems-with-maxima/" 
			target="window2">this page</a> for a quick tutorial, or
	<a href="http://maxima.sourceforge.net/docs/manual/maxima.html"
			target="window2">this page</a> for a complete manual, with special attention
			to Chapter 10.
	<ul><li>Use %e for e and %pi for &pi;.
	</li><li>Maxima does not understand implicit multiplication; the * sign must always be included. 
	</li><li>Note that log(x) is ln(x); for log<sub>10</sub>(x), use log(x)/log(10).
	</li><li>If you enter an expression with integers (e.g., 2/6 or log(100)) or 
	irrational constants (e.g., %e or %pi), Maxima 
	will reduce the expression only so far as it can express the result in integers. 
	Either enter all integers as decimals (e.g., 2.0/6.0 or log(100.0)),
	or use float(expression) to force Maxima to display the result
	as a decimal rounded to the 15th place.
	</li></ul>
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
