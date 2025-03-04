<%@ page language="java" %>
<%@ page import="
	com.epoch.db.dbConstants.DBLimits,
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
	<title>ACE Unit Conversion Adder</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script type="text/javascript" src="<%= pathToRoot %>js/jslib.js"></script>
	<script type="text/javascript">

	function saveMe() {
		var form = document.saveUnitConversion;
		if (isWhiteSpace(form.unitFrom.value)
				|| isWhiteSpace(form.unitTo.value)
				|| isWhiteSpace(form.factor.value)) {
			alert('You must enter data in all three boxes.');
		} else if (!canParseToFloat(form.power.value)
				|| !canParseToFloat(form.factor.value)) {
			alert('You must enter a numerical conversion power and factor.');
			return;
		} else form.submit();
	}

</script>
</head>

<body class="light" style="margin:0px; margin-top:5px; overflow:auto; 
		background-color:#f6f7ed; text-align:left;">
<br/><br/>
	<form name="saveUnitConversion" method="post" action="saveUnitConversion.jsp">
	<table style="margin-right:auto; margin-left:auto; width:90%;">
		<tr>
		<td colspan="2" class="boldtext" style="text-align:left; padding-bottom:10px;">
			<h3>Add a Unit Conversion</h3>
		</td>
		</tr>
		<tr>
		<td colspan="2" class="regtext" style="text-align:left; padding-bottom:10px;">
			Enter the units and the conversion power and factor by which 
			the number in the original unit should be raised and then multiplied.
			ACE will add both the forward and inverse conversions to the table.
		</td>
		</tr>
		<tr>
		<td class="boldtext" style="text-align:left;">
			Original unit: 
		</td>
		<td class="regtext" style="text-align:left;"> 
   			<input type="text" name="unitFrom" size="20"
					maxlength="<%= DBLimits.MAX_UNIT_NAME %>" value="" />
		</td>
		</tr>
		<tr>
		<td class="boldtext" style="text-align:left;">
			New unit: 
		</td>
		<td class="regtext" style="text-align:left;"> 
   			<input type="text" name="unitTo" size="20"
					maxlength="<%= DBLimits.MAX_UNIT_NAME %>" value="" />
		</td>
		</tr>
		<tr>
		<td class="boldtext" style="text-align:left;">
			Power: 
		</td>
		<td class="regtext" style="text-align:left;"> 
   			<input type="text" name="power" size="20" value="1" />
		</td>
		</tr>
		<tr>
		<td class="boldtext" style="text-align:left;">
			Factor: 
		</td>
		<td class="regtext" style="text-align:left;"> 
   			<input type="text" name="factor" size="20" value="1" />
		</td>
		</tr>
		<tr><td colspan="2" style="padding-top:10px;">
			<table><tr><td>
			<%= makeButton("Apply Changes", "saveMe();") %>
			</td><td>
			<%= makeButton("Cancel", "self.close();") %>
			</td></tr></table>
		</td></tr>
	</table>
	</form>
</body>
</html>
