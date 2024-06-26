<%@ page language="java" %>
<%@ page import="
	com.epoch.chem.ChemUtils,
	com.epoch.utils.Utils,
	chemaxon.struc.PeriodicSystem"
%>
<%
	request.setCharacterEncoding("UTF-8");
	String element = request.getParameter("element");
	String elementMod = element;
	final boolean entered = !Utils.isEmpty(element);
	final int[] values = new int[5];
	if (entered) {
		try {
			final int atno = PeriodicSystem.findAtomicNumber(element);
			values[0] = atno;
			values[1] = PeriodicSystem.getRow(atno);
			values[2] = PeriodicSystem.getColumn(atno);
			values[3] = ChemUtils.getValenceElectrons(atno);
			values[4] = ChemUtils.getMaxOuterElectrons(atno);
		} catch (IllegalArgumentException e) {
			; // do nothing
		}
		elementMod = Utils.toString(element.substring(0, 1).toUpperCase(),
				element.substring(1).toLowerCase());
	}
	
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>ACE Periodic table tester</title>
<link rel="stylesheet" href="../includes/epoch.css" type="text/css"/>
<script type="text/javascript">

	function periodic() {
		document.tester.submit();
	}

</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="periodic.jsp" method="post">
<p class="boldtext big" style="text-align:center;">
ACE Periodic table tester
</P>
<table>
<tr><td class="regtext"  colspan="2">
	<% if (entered && values[0] != 0) { %>
		The element <%= elementMod %> has atomic number <%= values[0] %>,
		it is located in row <%= values[1] %> and column <%= values[2] %>, 
		it has <%= values[3] %> valence electron<%= values[3] != 1 ? "s" : "" %>,
		and it can accomodate
		<%= values[4] %> electrons in its outer shell.
		<p>Try again, if you like.
	<% } else if (entered) { %>
		There is no such element as 
		<%= Utils.toString(element, elementMod.equals(element) ? ""
				: Utils.getBuilder(" or ", elementMod)) %>.
		<p>Try again, if you like.
	<% } else { %>
		Enter an element's symbol.
	<% } %>
</td></tr>
<tr><td>
	&nbsp;
</td></tr>

<tr><td>
	<input type="text" size="10" name="element"/>
</td></tr>
<tr><td>
	<br/>
	<input type="button" value=" Submit " onclick="periodic()"/>
	<br/>
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
