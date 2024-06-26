<%@ page language="java" %>
<%@ page import="
	com.epoch.db.dbConstants.DBLimits,
	com.epoch.synthesis.RxnCondition,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.Arrays"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	/* 
		rxnId is 0 | seq num of reaction
	 */
	
	final String[] allClassifns = 
			RxnCondition.getAllClassifns(RxnCondition.UNIQUE); 
	Arrays.sort(allClassifns);
	
	String rxnDef = "";
	String rxnName = "";
	String rxnClassifn = "";
	boolean threeComponent = false;
	final int rxnId = MathUtils.parseInt(request.getParameter("rxnId"));
	if (rxnId != 0) {
		final RxnCondition existingRxn = RxnCondition.getRxnCondition(rxnId);
		if (existingRxn != null) {
			rxnDef = existingRxn.reactionDef;
			rxnName = existingRxn.name;
			rxnClassifn = existingRxn.classifn;
			threeComponent = existingRxn.threeComponent;
		} // if there's an existing reaction
	} // if the reaction ID is not 0
	final String[] rxnClassifns = (rxnClassifn.indexOf(',') >= 0 ?
			rxnClassifn.split(",") : new String[] {rxnClassifn});
	final String SELECTED = "selected=\"selected\"";
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Reaction Definition Editor</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script type="text/javascript" src="<%= pathToRoot %>js/jslib.js"></script>
	<script type="text/javascript">
	// <!-- >

	function changeClassifnText(classNum) {
		var theValue = getValue('classifnSelector' + classNum);
		// alert('value = ' + theValue);
		setValue('rxnClassifn' + classNum, theValue);
		setVisibility('rxnClassifnCell' + classNum, 
				theValue === '' ? 'visible' : 'hidden');
	}

	var numClassifns = <%= rxnClassifns.length %>;
	var out = new String.builder();

	function addClassifn() {
		numClassifns++;
		var classifns = new Array(numClassifns + 1);
		// store existing classifns
		for (var classNum = 1; classNum < numClassifns; classNum++) { // <!-- >
			var classifn = 'rxnClassifn' + classNum;
			classifns[classNum] = getValue(classifn);
		}
		// remake existing table 
		initializeOut();
		for (var classNum = 1; classNum < numClassifns; classNum++) { // <!-- >
			addToOut(classNum, classifns[classNum]);
		}
		// add new reaction to table
		addToOut(numClassifns, '');
		finishOut();
		setInnerHTML('classificnTable', out.toString());
	}

	function removeClassifn(removed) {
		numClassifns--;
		var classifns = new Array(numClassifns + 2);
		// store existing classifns 
		for (var classNum = 1; classNum <= numClassifns + 1; classNum++) { // <!-- >
			var classifn = 'rxnClassifn' + classNum;
			classifns[classNum] = getValue(classifn);
		}
		// make new table up to removed reaction
		initializeOut();
		for (var classNum = 1; classNum < removed; classNum++) { // <!-- >
			addToOut(classNum, classifns[classNum]);
		}
		// complete new table starting at reaction after removed one;
		// decrease reaction numbers by 1
		for (var classNum = (removed + 1); classNum <= numClassifns + 1; classNum++) { // <!-- >
			addToOut((classNum - 1), classifns[classNum]);
		}
		finishOut();
		setInnerHTML('classificnTable', out.toString());
	}

	function initializeOut() {
		out = new String.builder().append('<table>');
	}

	function addToOut(classNumJS, className) {
		out.append('<tr><td colspan="2" class="boldtext">' 
					+ 'Classify the reaction conditions:'
					+ '<select name="classifnSelector').
				append(classNumJS).append('" id="classifnSelector').
				append(classNumJS).append('" onchange="changeClassifnText(').
				append(classNumJS).append(');"><option value="">[New]');
		var knownSelected = false;
		<% for (final String classifn : allClassifns) { %>
				out.append('<option value="<%= Utils.toValidJS(
						Utils.toValidHTMLAttributeValue(classifn)) %>"');
				if (className === '<%= Utils.toValidJS(classifn) %>') {
					out.append(' <%= SELECTED %>');
					knownSelected = true;
				}
				out.append('><%= Utils.toValidJS(
						Utils.toPopupMenuDisplay(classifn)) %>');
			<% } // for each possible class %>
		out.append('</select></td></tr>'
					+ '<tr><td class="boldtext" id="rxnClassifnCell').
				append(classNumJS).append('"');
		if (knownSelected) out.append(' style="visibility:hidden;"');
		out.append('><input type="text" id="rxnClassifn').
				append(classNumJS).append('" size="40" value="').
				append(className).append('"\/><\/td><td>');
		if (classNumJS > 1) {
			out.append(' [<a onclick="javascript:removeClassifn(').
					append(classNumJS).append(')">Remove</a>]');
		}
		if (classNumJS === numClassifns) {
			out.append(' [<a onclick="javascript:addClassifn()">Add class</a>]');
		}
		out.append('</td></tr>');
		// alert(out);
	}

	function finishOut() {
		out.append('</table>');
	}

	function saveReaction(form) {
		if (isWhiteSpace(form.rxnDef.value)) {
			alert('You must enter a reaction definition.');
			return;
		}
		if (isWhiteSpace(form.rxnName.value)) {
			alert('You must enter a reaction condition description.');
			return;
		}
		var classifnsBld = new String.builder();
		for (var classNum = 1; classNum <= numClassifns; classNum++) { // <!-- >
			var classifn = getValue('rxnClassifn' + classNum);
			if (isWhiteSpace(classifn)) {
				alert('Please choose at least one classification, '
						+ 'and remove unused textboxes.');
				return;
			}
			classifnsBld.append(classifn).
					append(classNum < numClassifns ? ',' : ''); // <!-- >
		}
		var classifns = classifnsBld.toString();
		// alert('classifns = ' + classifns);
		form.rxnClassifn.value = classifns;
		form.submit();
	}

	// -->
</script>
</head>

<body class="light" style="margin:0px; margin-top:5px; overflow:auto; 
		background-color:#f6f7ed; text-align:left;">
<br/><br/>
	<form name="saverxn" method="post" action="saveReaction.jsp">
	<input type="hidden" NAME="rxnId" value="<%= rxnId %>" />
	<input type="hidden" NAME="rxnClassifn" 
			value="<%= Utils.toValidHTMLAttributeValue(rxnClassifn) %>" />
	<table style="margin-right:auto; margin-left:auto;">
		<tr>
		<td colspan="2" class="boldtext">
			Reaction condition description for student:
		</td>
		</tr>
		<tr>
		<td colspan="2" class="boldtext"> 
   			<input type="text" name="rxnName" size="65"
					maxlength="<%= DBLimits.MAX_RXNCOND_NAME %>"
					value="<%= Utils.toValidTextbox(rxnName) %>" />
		</td>
		</tr>
		<tr>
		<td colspan="2" class="boldtext" id="classificnTable">
			<table>
				<% for (int chosenClass = 1; 
						chosenClass <= rxnClassifns.length; 
						chosenClass++) { %>
					<tr><td colspan="2" class="boldtext"> 
						Classify the reaction conditions:
						<select name="classifnSelector<%= chosenClass %>" 
								id="classifnSelector<%= chosenClass %>" 
								onchange="changeClassifnText(<%= chosenClass %>);">
							<option value="">[New]
							<% for (final String classifn : allClassifns) { %>
									<option value="<%= 
											Utils.toValidHTMLAttributeValue(classifn) %>"
									<%= rxnClassifns[chosenClass - 1].equals(
											classifn) ? SELECTED : "" %>
									><%= Utils.toPopupMenuDisplay(classifn) %>
								<% } // for each possible class %>
						</select>
					</td></tr>
					<tr><td class="boldtext" id="rxnClassifnCell<%= chosenClass %>"
							style="visibility:hidden;"> 
   						<input type="text" id="rxnClassifn<%= chosenClass %>" 
								size="40" value="<%= Utils.toValidTextbox(
											rxnClassifns[chosenClass - 1]) %>" />
					</td><td>
						<% if (chosenClass > 1) { %>
							[<a onclick="javascript:removeClassifn(<%= 
									chosenClass %>)">Remove</a>]
						<% } 
						if (chosenClass == rxnClassifns.length) { %>
							[<a onclick="javascript:addClassifn()">Add class</a>]
						<% } %>
					</td></tr>
				<% } // for each chosen class %>
			</table>
		</td></tr>
		<tr><td class="regtext">
			<input type="checkbox" name="threeComponent" 
					<%= threeComponent ? "checked=\"checked\"" : "" %>>at least one reaction
			can accept three starting materials
		</td><td>
		<tr><td>
			<br/><%= makeButton("Apply Changes", "saveReaction(document.saverxn);") %>
		</td><td>
			<br/><%= makeButton("Cancel", "self.close();") %>
		</td></tr>
		<tr><td colspan="2" class="regtext">
			<br/><br/>Paste a new reaction definition or edit the existing one: 
			<br/><textarea name="rxnDef" style="font-family:Courier;" 
					cols="65" rows="30"><%= 
							Utils.toValidTextbox(rxnDef) %></textarea>
		</td></tr>
	</table>
	</form>
</body>
</html>
