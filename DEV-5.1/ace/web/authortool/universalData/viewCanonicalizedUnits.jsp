<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.db.CanonicalizedUnitRW,
	com.epoch.physics.CanonicalizedUnit,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	final boolean masterEdit = "true".equals(request.getParameter("masterEdit"));
	final CanonicalizedUnit[] units = CanonicalizedUnitRW.getAllUnits();
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>Unit Canonicalizations</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
	body {
		margin:0;
		border:0;
		padding:0;
		height:100%; 
		max-height:100%; 
		font-family:arial, verdana, sans-serif; 
		font-size:76%;
		overflow: hidden; 
	}

	#qDataFooter {
		position:absolute; 
		bottom:0; 
		left:0;
		width:100%; 
		height:50px; 
		overflow:auto; 
		text-align:right; 
		vertical-align:bottom;
		padding-top:10px;
	}

	#qDataContents {
		position:fixed; 
		top:0px;
		left:0;
		bottom:50px; 
		right:0; 
		overflow:auto; 
	}

	* html #qDataContents {
		height:100%; 
	}

	* html body {
		padding:0px 0 50px 0; 
	}
	</style>

	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
	<script type="text/javascript">
		// <!-- >

		function addUnit() {
			openReactionWindow('addCanonicalizedUnit.jsp');
		} // addUnit()
 
		function removeUnit(unitSymbol, unitName) {
			var go = new String.builder().
					append('removeCanonicalizedUnit.jsp?unitSymbol=').
					append(encodeURIComponent(unitSymbol)).
					append('&unitName=').
					append(encodeURIComponent(unitName));
			var url = go.toString();
			openReactionWindow(url);
		} // removeUnit()
 
 		// -->
	</script>
</head>
<body class="light" style="margin:0px; margin-top:5px; background-color:#f6f7ed;">

<div id="qDataContents">
<table style="margin-left:auto; margin-right:auto; width:95%;">
	<tr><td class="boldtext big" style="padding-top:10px; padding-bottom:10px;">
		ACE Unit Canonicalizations
	</td></tr>
	<% if (!masterEdit) { %>
		<tr><td class="regtext" style="padding-bottom:10px;">
			Contact the programmers if you would like to add to this list.
		</td></tr>
	<% } %>
	<tr><td class="regtext" style="text-align:left; padding-bottom:10px;">
		The fundamental units are
		<% final int numSIUnits = CanonicalizedUnit.SI_UNIT_SYMBOLS.length;
		for (int unitNum = 1; unitNum <= numSIUnits; unitNum++) { %>
			<%= CanonicalizedUnit.SI_UNIT_SYMBOLS[unitNum - 1] %>
			(<%= CanonicalizedUnit.SI_UNIT_NAMES[unitNum - 1] %>)<%= 
					unitNum < numSIUnits - 1 ? ", " : unitNum < numSIUnits
						? ", and " : "." %>
		<% } // for each SI unit %>
	</td></tr>
	<tr><td style="text-align:center;">
		<table class="regtext"
				style="margin-left:auto; margin-right:auto; width:100%; text-align:left;">
			<tr>
			<th>Symbol</th>
			<th>Name</th>
			<th>Measures</th>
			<th>Equivalent in fundamental units</th>
			</tr>
			<% 
			// int unitNum = 0; // unused 11/6/2012
			for (final CanonicalizedUnit unit : units) { 
				final String symbol = unit.getSymbol(); %>
				<tr>
				<td><%= symbol %>
				<span style="color:green;"><%= symbol.charAt(0) == '&' 
						?  "&nbsp;&nbsp;[&amp;" + symbol.substring(1) + "]" : "" %></span>
				</td>
				<td><%= unit.getName() %></td>
				<td><%= unit.getWhatMeasures() %></td>
				<td><%= unit.toDisplay() %></td>
				<% if (masterEdit) { %>
					<td><%= makeButtonIcon("delete", pathToRoot, 
							"removeUnit('", 
							Utils.toValidJS(unit.getSymbol().replaceAll("&", "&amp;")), 
							"','",
							Utils.toValidJS(unit.getName().replaceAll("&", "&amp;")), 
							"');") %></td>
				<% } // if masterEdit %>
				</tr>
			<% } // for each unit %>
		</table>
	</td></tr>
</table>
</div>
<div id="qDataFooter">
<table style="padding-top:10px; align:center; margin-left:auto; margin-right:auto;">
	<tr>
		<td><%= makeButton("Close", "self.close();") %></td>
		<% if (masterEdit) { %>
			<td style="padding-left:30px;">
				<%= makeButton("Add unit", "addUnit();") %>
			</td>
		<% } // if masterEdit %>
	</tr>
</table>
</div>
</body>
</html>

