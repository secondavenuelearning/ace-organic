<%@ page language="java" %>
<%@ page import="
	com.epoch.db.CanonicalizedUnitRW,
	com.epoch.physics.CanonicalizedUnit,
	com.epoch.utils.Utils"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../../";

%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Canonicalized Unit Adder</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script type="text/javascript" src="<%= pathToRoot %>js/jslib.js"></script>
	<script type="text/javascript">

	function saveMe() {
		var form = document.saveCanonicalizedUnit;
		if (isWhiteSpace(form.unitSymbol.value)
				|| isWhiteSpace(form.unitName.value)) {
			alert('You must enter a symbol and name.');
		} else if (!canParseToFloat(form.coeff.value)
				|| parseFloat(form.coeff.value) === 0.0
				|| !canParseToInt(form.power10.value)
				<% for (final String SI_UNIT : CanonicalizedUnit.SI_UNIT_SYMBOLS) { %>
					|| !canParseToInt(form.<%= SI_UNIT %>.value)
				<% } // for each fundamental SI unit %>
				) {
			alert('You must enter a numerical, nonzero coefficient and integral exponents.');
			return;
		} else form.submit();
	}

</script>
</head>

<body class="light" style="margin:0px; overflow:auto; 
		background-color:#f6f7ed; text-align:left;">
	<form name="saveCanonicalizedUnit" method="post" action="saveCanonicalizedUnit.jsp">
	<table style="margin-right:auto; margin-left:auto; width:95%;">
		<tr>
		<td colspan="2" class="boldtext" style="text-align:left; padding-bottom:10px;">
			<h3>Add a Unit</h3>
		</td>
		</tr>
		<tr>
		<td colspan="2" class="regtext" style="text-align:left; padding-bottom:10px;">
			Enter the definition of the unit in fundamental units.
			<b>Note</b>: Do not enter a unit such as kJ, mL, or nM that has 
			an order-of-magnitude prefix. 
		</td>
		</tr>
		<tr>
		<td class="boldtext" style="text-align:left; width:40%;">
			Unit symbol (case matters):
		</td>
		<td class="regtext" style="text-align:left;"> 
   			<input type="text" name="unitSymbol" size="20" value="" />
		</td>
		</tr>
		<tr>
		<td class="boldtext" style="text-align:left;">
			Unit name:
		</td>
		<td class="regtext" style="text-align:left;"> 
   			<input type="text" name="unitName" size="30" value="" />
		</td>
		</tr>
		<tr>
		<td class="boldtext" style="text-align:left;">
			Property that the unit measures*:
		</td>
		<td class="regtext" style="text-align:left;"> 
   			<input type="text" name="whatMeasures" size="30" value="" />
		</td>
		</tr>
		<tr>
		<td class="boldtext" style="text-align:left;">
			Equivalent in fundamental units: 
		</td>
		</tr>
		<tr>
		<td class="regtext" style="text-align:left;" colspan="2"> 
   			<input type="text" name="coeff" size="20" value="1.0" />
			&times; 10<sup><input type="text" name="power10" size="1" value="0" /></sup>
			<br/>
			<% int unitNum = 0;
			for (final String SI_UNIT : CanonicalizedUnit.SI_UNIT_SYMBOLS) { 
				unitNum++; %>
				<%= unitNum > 1 ? "&middot;" : "" %>
				<%= SI_UNIT %><sup><input type="text" name="<%= SI_UNIT %>"
						size="1" value="0" /></sup>
			<% } // for each fundamental SI unit %>
		</td>
		</tr>
		<tr><td colspan="2" style="padding-top:10px;">
			<table><tr><td>
			<%= makeButton("Apply Changes", "saveMe();") %>
			</td><td>
			<%= makeButton("Cancel", "self.close();") %>
			</td></tr></table>
		</td></tr>
		<tr><td colspan="2" style="color:green; padding-top:10px;">
			*Properties already in database:
			<table summary="">
			<% final String[] properties = CanonicalizedUnitRW.getAllUnitProperties();
			for (final String property : properties) { %>
				<tr><td><%= property %></td></tr>
			<% } // for each property %>
			</table>
		</td></tr>
	</table>
	</form>
</body>
</html>
