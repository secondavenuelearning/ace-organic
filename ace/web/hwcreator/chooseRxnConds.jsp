<%@ page language="java" %>
<%@ page import="
	com.epoch.assgts.Assgt,
	com.epoch.synthesis.RxnCondition,
	com.epoch.utils.Utils,
	java.util.Map"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../";
	final String RXN_CONDN_ID_SEP = Assgt.RXN_CONDN_ID_SEP;
	
	final Map<Integer, String> reactionNamesByIds =
			RxnCondition.getRxnNamesKeyedByIds();
	final int[] reactionIds = RxnCondition.getAllReactionIdsAlphabetized(
			reactionNamesByIds); 
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Reaction condition selector</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon">
	<style type="text/css">
	body {
		margin:0;
		border:0;
		padding:0;
		height:100%; 
		max-height:100%; 
		font-family:arial, verdana, sans-serif; 
		font-size:76%;
		overflow: auto; 
	}

	#evalFooter {
		position:absolute; 
		bottom:0; 
		left:0;
		width:100%; 
		height:50px; 
		overflow:auto; 
		text-align:right; 
		vertical-align:bottom;
		padding-top:20px;
	}

	#evalContents {
		position:fixed; 
		top:0px;
		left:0;
		bottom:50px; 
		right:0; 
		overflow:auto; 
	}

	* html #evalContents {
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

	function setAllowedCondns() {
		var currentRxnConds = opener.getAllowedRxnCondns().split('<%= RXN_CONDN_ID_SEP %>');
		<% for (int rxnNum = 2; rxnNum <= reactionIds.length; rxnNum++) { %>
			if (currentRxnConds.contains('<%= reactionIds[rxnNum - 1] %>')) {
				checkCell('option<%= rxnNum %>');
			}
		<% } // for each reaction %>
	} // setAllowedCondns()

	var ON = 1;
	var TOGGLE = 2;
	function changeAll(how) {
		<% for (int rxnNum = 2; rxnNum <= reactionIds.length; rxnNum++) { %>
			document.rxnsform.option<%= rxnNum %>.checked = (how === ON 
					? true : !document.rxnsform.option<%= rxnNum %>.checked);
		<% } // for each reaction %>
	} // selectAll

	function submitIt() {
		var bld = new String.builder().append('<%= reactionIds[0] %>');
		<% for (int rxnNum = 2; rxnNum <= reactionIds.length; rxnNum++) { %>
			if (document.rxnsform.option<%= rxnNum %>.checked)
				bld.append('<%= RXN_CONDN_ID_SEP %><%= reactionIds[rxnNum - 1] %>');
		<% } // for each reaction %>
		var allowedRxnCondns = bld.toString();
		if (allowedRxnCondns.indexOf('<%= RXN_CONDN_ID_SEP %>') < 0 // <!-- >
				&& !canParseToInt(trim(allowedRxnCondns)))
			allowedRxnCondns = '';
		// alert('allowedRxnCondns = ' + allowedRxnCondns);
		setAndClose(allowedRxnCondns);
	} // submitIt

	function setAndClose(allowedRxnCondns) {
		opener.setAllowedRxnCondns(allowedRxnCondns);
    	self.close();
	} // setAndClose()

	// -->
	</script>
</head>

<body class="light" style="margin:0px; margin-top:5px; background-color:#f6f7ed; 
		text-align:center;" onload="setAllowedCondns();">

	<div id="evalContents">
	<table class="regtext" style="margin-left:auto; margin-right:auto; width:445px;">
	<tr><td style="text-align:left;">
		<%= user.translate("Choose the reactions the student will be "
		+ "permitted to use. If you choose none, all will be allowed.") %>
		<br/><%= user.translate("The first reaction is permanently selected.") %>
	</td>
	<tr><td style="text-align:left;">
		<table style="margin-right:auto; margin-left:auto;"><tr>
		<td><%= makeButton(user.translate("Select All"), 
				"changeAll(ON);") %></td>
		<td><%= makeButton(user.translate("Toggle All"), 
				"changeAll(TOGGLE);") %></td>
		</tr></table>
	</td>
	</tr>
	<tr><td style="text-align:left;">
		<form name="rxnsform">
		<table>
		<% if (reactionIds != null) { %>
			<tr><td class="boldtext"></td>
				<td class="boldtext" style="padding-left:10px;"><%= 
						user.translate("Reaction Condition") %></td>
			</tr>
			<% for (int rxnNum = 1; rxnNum <= reactionIds.length; rxnNum++) {
				final String bgColor = (rxnNum % 2 == 0 ? "F6F7ED" : "FFFFFF"); %>
				<tr style="background-color:#<%= bgColor %>">
				<td class="regtext" style="vertical-align:top;">
					<% if (rxnNum != 1) { %>	
						<input type="checkbox" name="option<%= rxnNum %>" 
								id="option<%= rxnNum %>" />
					<% } else { %>
						<img src="<%= pathToRoot %>images/checked_checkbox.jpg" 
								alt="&nbsp;&radic;&nbsp;" />
					<% } %>
			 	</td>
				<td class="regtext" style="vertical-align:middle;">
					<%= Utils.toDisplay(reactionNamesByIds.get(
							reactionIds[rxnNum - 1])) %>
			 	</td>
				</tr>
			<% } // for option index rxnNum 
		} // if there's at least one reaction %>
		<tr>
			<td>&nbsp;</td>
			<td class="regtext" style="vertical-align:middle;">
				<br/><%= user.translate("Contact the developers if "
						+ "you would like to add a reaction to this list.") %> 
			</td>
		</tr>
		</table>
		</form>
    </td></tr>
	</table>
	</div>
	<div id="evalFooter">
	<table style="margin-left:auto; margin-right:auto; width:445px;">
	<tr><td>
		<table style="margin-right:auto; margin-left:auto;"><tr>
		<td><%= makeButton(user.translate("Save choices"), "submitIt();") %></td>
		<td><%= makeButton(user.translate("Allow all"), "setAndClose('all');") %></td>
		<td><%= makeButton(user.translate("Cancel"), "self.close();") %></td>
		</tr></table>
	</td></tr>
	</table>
	</div>

</body>
</html>
