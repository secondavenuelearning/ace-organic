<%@ page language="java" %>
<%@ page import="
	com.epoch.chem.FnalGroupDef,
	com.epoch.db.dbConstants.DBLimits,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
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
		grpId is 0 | seq num of reaction
	 */
	
	final String[] allCategories = FnalGroupDef.getAllCategories(); 
	final int grpId = MathUtils.parseInt(request.getParameter("grpId"));
	final FnalGroupDef group = 
			FnalGroupDef.getFnalGroupDef(grpId); // returns empty object for id 0
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Functional Group Editor</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script type="text/javascript" src="<%= pathToRoot %>js/jslib.js"></script>
	<script type="text/javascript">

	function changeCategoryText() {
		var selectedCategory = document.savegrp.grpCategorySelector.value;
		// alert('value = ' + selectedCategory);
		if (selectedCategory === '') {
			showCell('grpCategoryName'); 
		} else {
			setValue('grpCategoryName', '');
			hideCell('grpCategoryName'); 
		}
	}

	function saveFnalGrp() {
		var form = document.savegrp;
		if (isWhiteSpace(form.grpDef.value)) {
			alert('You must enter a reaction definition.');
			return;
		}
		if (isWhiteSpace(form.grpName.value)) {
			alert('You must enter a reaction condition description.');
			return;
		}
		var selectedCategory = form.grpCategorySelector.value;
		if (selectedCategory === '') {
			if (isWhiteSpace(form.grpCategoryName.value)) {
				alert('Please choose a category, or choose New to enter a new one.');
				return;
			}
			selectedCategory = form.grpCategoryName.value; 
		} else {
		}
		form.grpCategory.value = selectedCategory; 
		form.submit();
	}

</script>
</head>

<body class="light" style="margin:0px; margin-top:5px; overflow:auto; 
		background-color:#f6f7ed; text-align:left;"
		onload="changeCategoryText();">
<br/><br/>
	<form name="savegrp" method="post" action="saveFnalGrp.jsp">
	<input type="hidden" name="grpId" value="<%= grpId %>" />
	<input type="hidden" name="grpCategory" 
			value="<%= Utils.toValidHTMLAttributeValue(group.category) %>" />
	<table style="margin-right:auto; margin-left:auto;">
		<tr>
		<td colspan="2" class="boldtext" style="text-align:left;">
			Functional group name (please capitalize):
		</td>
		</tr>
		<tr>
		<td colspan="2" class="boldtext" style="text-align:left;"> 
   			<input type="text" name="grpName" size="65"
				maxlength="<%= DBLimits.MAX_FNAL_GRP_NAME %>"
				value="<%= Utils.toValidTextbox(group.name) %>" />
		</td>
		</tr>
		<tr>
		<td colspan="2" class="boldtext" style="text-align:left;">
			Choose a category for the functional group:
		</td>
		</tr>
		<tr><td class="boldtext">
			<select name="grpCategorySelector"
					onchange="changeCategoryText();">
				<option value="">[New]
				<% if (allCategories != null) 
					for (final String category : allCategories) { %>
						<option value="<%= Utils.toValidHTMLAttributeValue(category) %>"
						<%= group.category != null 
								&& group.category.equals(category) ?
								"selected=\"selected\"" : "" %>
						><%= Utils.toPopupMenuDisplay(category) %>
					<% } // for each possible category %>
			</select>
		</td><td>
			<textarea id="grpCategoryName" name="grpCategoryName" 
					style="padding-top:2px;" 
					cols="20" rows="1"><%= 
				Utils.toValidTextbox(group.category) %></textarea>
		</td></tr>
		<tr><td colspan="2" class="regtext">
			<br/><br/>Enter a new functional group definition, 
			or edit the existing one: 
			<br/><textarea name="grpDef" style="font-family:Courier;" 
					cols="65" rows="5"><%= 
							Utils.toValidTextbox(group.definition) %></textarea>
		</td></tr>
		<tr><td>
			<br/><%= makeButton("Apply Changes", "saveFnalGrp();") %>
		</td><td>
			<br/><%= makeButton("Cancel", "self.close();") %>
		</td></tr>
	</table>
	</form>
</body>
</html>


