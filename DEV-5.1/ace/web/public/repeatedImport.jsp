<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolFormatException,
	chemaxon.formats.MolImporter,
	chemaxon.struc.Molecule,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final int numRepeats = MathUtils.parseInt(request.getParameter("numRepeats"));
	String mrv = request.getParameter("mrv");
	final StringBuilder bld = new StringBuilder();
	if (!Utils.isEmpty(mrv)) {
		mrv = mrv.trim();
		for (int i = 0; i < numRepeats; i++) {
			try {
				final Molecule mol = MolImporter.importMol(mrv);
			} catch (MolFormatException e) {
				bld.append("Attempt ").append(i + 1).append(": ")
						.append(e.getMessage()).append("<br/>\n");
			}
		} // for each time
	} // if there's a molecule
	if (bld.length() == 0) bld.append("No failures to import.");

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<title>ACE Repeated Import</title>
<link rel="stylesheet" href="../includes/epoch.css" type="text/css"/>
<script src="../js/jslib.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- 
	function doIt() {
		setInnerHTML('msg', 'Processing...');
		document.tester.submit();
	}
	// -->
</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="repeatedImport.jsp" method="post">
<P class="boldtext big" align=center>
ACE Repeated Import
</P>
<table>
<tr><td class="regtext"  colspan=2>
	<% if (numRepeats > 0) { %>
		<%= bld.toString() %>
		<P>Try again, if you like.
	<% } else { %>
		Enter the MRV for a response and the number of times JChem 
		should try importing it.  
	<% } %>
</td></tr>
<tr><td id="msg">
	&nbsp;
</td></tr>

<tr><td>
	<input type="text" size="10" value="<%= 
			numRepeats == 0 ? "" : numRepeats %>" name="numRepeats"/>
</td></tr>

<tr><td>
	<textarea id="mrv" name="mrv" rows="50" cols="200"><%=
			mrv == null ? "" : Utils.toValidTextbox(mrv) %></textarea>
</td></tr>
<tr><td>
	<input type="button" value="Submit" onclick="doIt();"/>
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
