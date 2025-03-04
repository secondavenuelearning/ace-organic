<%@ page language="java" %>
<%@ page import="
	com.epoch.synthesis.RxnCondition,
	com.epoch.utils.Utils,
	java.util.Map"
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
	<title>Reaction conditions</title>
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
		
		function editReaction(rxnId, rxnNum) {
			if (rxnNum > 8) {
				var href = self.location.href;
				var index = href.indexOf('#rxn');
				var bld = new String.builder();
				if (index < 0) { // <!-- >
					bld.append(self.location.href).append('#rxn').append(rxnNum);
				} else {
					bld.append(href.substring(0, index)).append('#rxn').append(rxnNum);
				}
				self.location.href = bld.toString();
			}
			openReactionWindow('editReaction.jsp?rxnId=' + rxnId);
		} // editReaction()

		function deleteResult(rxnId) {
			openReactionWindow('deleteReactionResult1.jsp?rxnId=' + rxnId);
		} // deleteResult()

		function changeResult(rxnId) {
			openReactionWindow('changeReactionResult1.jsp?rxnId=' + rxnId);
		} // deleteResult()

		// -->
	</script>
</head>
<body class="light" style="margin:0px; margin-top:5px; background-color:#f6f7ed;">

<div id="qDataContents">
<table style="margin-left:auto; margin-right:auto; width:90%;">
	<tr><td class="boldtext big" style="padding-top:10px; padding-bottom:10px;">
		ACE Reaction Conditions
	</td></tr>
	<% if (!masterEdit) { %>
		<tr><td class="regtext" style="padding-bottom:10px;">
			Contact the programmers if you would like to add to this list.
		</td></tr>
	<% } %>
	<tr><td style="text-align:center;">
		<table>
			<% if (reactionIds != null) { %>
				<tr><th style="text-align:left;"><u>Reaction Condition</u></th>
				<% if (masterEdit) { %>
					<th><u>Edit</u></th>
					<th style="padding-left:10px;"><u>Delete a result</u></th>
					<th style="padding-left:10px;"><u>Change a result</u></th>
				<% } // if master edit %>
				</tr>
				<% for (int rxnNum = 1; rxnNum <= reactionIds.length; rxnNum++) {
					String forDisplay = Utils.toDisplay(
							reactionNamesByIds.get(reactionIds[rxnNum - 1]));
					if (masterEdit) {
						forDisplay = Utils.toString("<a onclick=\"editReaction(", 
								reactionIds[rxnNum - 1], ", ", rxnNum, ");\">", 
								forDisplay, "</a>");
					} // if master edit %>
					<tr>
					<td class="regtext" style="padding-left:10px;">
						<a name="rxn<%= rxnNum %>"></a>
						<%= forDisplay %>
				 	</td>
					<% if (masterEdit) { %>
						<td class="regtext" style="text-align:center;">
							<%= makeButtonIcon("edit", pathToRoot, 
									"editReaction(", reactionIds[rxnNum - 1], ", ",
											rxnNum, ");") %>
				 		</td>
						<td class="regtext" style="padding-left:10px; text-align:center;">
							<%= makeButtonIcon("delete", pathToRoot, 
									"deleteResult(", reactionIds[rxnNum - 1], ", ",
											rxnNum, ");") %>
				 		</td>
						<td class="regtext" style="padding-left:10px; text-align:center;">
							<%= makeButtonIcon("edit", pathToRoot, 
									"changeResult(", reactionIds[rxnNum - 1], ", ",
											rxnNum, ");") %>
				 		</td>
					<% } // if master edit %>
					</tr>
				<% } // for each rxnNum 
			} // if there's at least one reaction %>
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
				<%= makeButton("Add New Reaction", "editReaction(0, 0);") %>
			</td>
		<% } %>
	</tr>
</table>
</div>
</body>
</html>

