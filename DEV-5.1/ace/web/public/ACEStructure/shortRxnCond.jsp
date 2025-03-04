<%@ page language="java" %>
<%@ page import="
	com.epoch.synthesis.RxnCondition,
	com.epoch.utils.Utils,
	java.util.Map"
%>
<%
	final String pathToRoot = "../../";
	final int[] reactionIds = RxnCondition.getAllReactionIds();
	int defaultRxnNum = 0;
	final Map<Integer, String> reactionNamesByIds =
			RxnCondition.getRxnNamesKeyedByIds();
	final Map<Integer, String> reactionDefsByIds =
			RxnCondition.getRxnDefsKeyedByIds();
	final String allRxnsStr = RxnCondition.alphabetize(
			Utils.join(reactionIds, ":"), reactionNamesByIds);
			
	final String rxnWindowUrl = pathToRoot + "authortool/chooseRxnCondsUser.jsp"
			+ "?fromSynthTest=true&amp;rxnNum=1&amp;allowedRxns=" + allRxnsStr;

	final String[] shortRxnDefs = new String[reactionIds.length];
	final String name = "dictRef=\"NAME\" title=\"NAME\">";
	final int nameLen = name.length();
	final String scalar = "<scalar>";
	final int scalarLen = scalar.length();
	final String codeStart = "<![CDATA[";
	final int codeStartLen = codeStart.length();
	final String codeEnd = "]]>";
	final int codeEndLen = codeEnd.length();
	final String endName = "</scalar>";
	final int endNameLen = endName.length();
	final String divider = "***";
	final int dividerLen = divider.length();
	for (int rxnNum = 0; rxnNum < reactionIds.length; rxnNum++) { 
		if (reactionIds[rxnNum] == RxnCondition.NO_REAGENTS)
			defaultRxnNum = rxnNum;
		String rxnDef = reactionDefsByIds.get(reactionIds[rxnNum]); 
		shortRxnDefs[rxnNum] = "";
		int nameLocn = rxnDef.indexOf(name);
		int dividerLocn = rxnDef.indexOf(divider);
		String indent = "";
		while (dividerLocn >= 0 || nameLocn >= 0) {
			if (dividerLocn < 0 || nameLocn < dividerLocn) {
				rxnDef = rxnDef.substring(nameLocn + nameLen + 1);
				final int endNameLocn = rxnDef.indexOf(endName) + endNameLen;
				String oneRxnName = rxnDef.substring(1, endNameLocn + 1);
				int locn = oneRxnName.indexOf(scalar);
				if (locn == 0) oneRxnName = oneRxnName.substring(scalarLen);
				else if (locn > 0) oneRxnName = oneRxnName.substring(0, locn)
						+ (locn + scalarLen < oneRxnName.length() ?
							oneRxnName.substring(locn + scalarLen) : "");
				locn = oneRxnName.indexOf(codeStart);
				if (locn == 0) oneRxnName = oneRxnName.substring(codeStartLen);
				else if (locn > 0) oneRxnName = oneRxnName.substring(0, locn)
						+ (locn + codeStartLen < oneRxnName.length() ?
							oneRxnName.substring(locn + codeStartLen) : "");
				locn = oneRxnName.indexOf(codeEnd);
				if (locn == 0) oneRxnName = oneRxnName.substring(codeEndLen);
				else if (locn > 0) oneRxnName = oneRxnName.substring(0, locn)
						+ (locn + codeEndLen < oneRxnName.length() ?
							oneRxnName.substring(locn + codeEndLen) : "");
				locn = oneRxnName.indexOf(endName);
				if (locn == 0) oneRxnName = oneRxnName.substring(endNameLen);
				else if (locn > 0) oneRxnName = oneRxnName.substring(0, locn)
						+ (locn + endNameLen < oneRxnName.length() ?
							oneRxnName.substring(locn + endNameLen) : "");
				shortRxnDefs[rxnNum] += oneRxnName;
				rxnDef = rxnDef.substring(endNameLocn + endNameLen);
			} else {
				rxnDef = rxnDef.substring(dividerLocn + dividerLen);
				if (rxnDef.charAt(0) == '(') indent += '\t';
				final int endDivLocn = rxnDef.indexOf(divider);
				shortRxnDefs[rxnNum] += indent + rxnDef.substring(0, endDivLocn) + "\n";
				if (rxnDef.charAt(0) == ')') {
					if (indent.length() > 1) indent = indent.substring(1);
					else indent = "";
				}
				rxnDef = rxnDef.substring(endDivLocn + dividerLen);
			}
			dividerLocn = rxnDef.indexOf(divider);
			nameLocn = rxnDef.indexOf(name);
		}
	}
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>Short reaction condition display</title>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>

<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script type="text/javascript">

	// <!-- >
	
	function setReactionDef(rxnId) {
		<% for (int rxnNum = 0; rxnNum < reactionIds.length; rxnNum++) { %>
			if (rxnId === <%= reactionIds[rxnNum] %>) 
				setInnerHTML('reactionDef', 
						'<%= Utils.toValidHTML(shortRxnDefs[rxnNum]) %>');
		<% } %>
	}

	// -->

</script>
</head>

<body style="overflow:auto;">

<table style="width:600px; padding-left:10px;">
	<tr>
		<td>
			<p class="boldtext big" style="text-align:center;">
			ACE short reaction condition displayer
			</p>
		</td>
	</tr>
	<tr style="height:40px;">
		<td class="regtext" style="vertical-align:top; height:40px;">
			Reaction conditions (click on them to alter them): 
			<input type="hidden" id="reaction1Id" 
					value="<%= RxnCondition.NO_REAGENTS %>"/>
			<input type="hidden" id="reaction1OrigName" 
					value="<%= reactionNamesByIds.get(Integer.valueOf(
							RxnCondition.NO_REAGENTS)) %>"/>
			<a onclick="openReactionWindow('<%= rxnWindowUrl %>')">
			<span id="reaction1Name"><%= Utils.toDisplay(
					reactionNamesByIds.get(Integer.valueOf(
							RxnCondition.NO_REAGENTS))) %></span>
			</a>
		</td>
	</tr>
	<tr>
		<td class="regtext">
			<a href="index.html">Back</a> to developer pages.
		</td>
	</tr>
	<tr>
		<td class="regtext" style="padding-top:30px; padding-bottom:10px;">
			Definition of the selected reaction (if the reaction 
			definition has changed since this page was most recently loaded, 
			the old definition will be displayed, but the new definition will 
			be used to process reactants) (<a href="synthGlossary.html">glossary</a>):
		</td>
	</tr>
	<tr>
		<td>
			<pre><span id="reactionDef"><%= Utils.toValidHTML(
					shortRxnDefs[defaultRxnNum]) %></span></pre>
		</td>
	</tr>
</table>

</body>
</html>
