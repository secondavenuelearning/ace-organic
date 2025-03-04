<%@ page language="java" %>
<%@ page import="
	com.epoch.db.dbConstants.DBLimits,
	com.epoch.qBank.Question,
	com.epoch.substns.RGroupCollection,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%
	response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
	response.setHeader("Pragma", "no-cache"); // HTTP 1.0
	response.setDateHeader("Expires", 0); // prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final String[] allClasses = RGroupCollection.getAllRGroupCollectionNames(); 
	String[] rGroups = new String[0];
	String rGroupClassName = "";
	final int rGroupClassNum = MathUtils.parseInt(request.getParameter("rGroupNum"));
	if (rGroupClassNum != 0) {
		final RGroupCollection existingRGroupCollection = 
				RGroupCollection.getRGroupCollection(rGroupClassNum);
		if (existingRGroupCollection != null) {
			rGroups = existingRGroupCollection.rGroups;
			rGroupClassName = existingRGroupCollection.name;
		}
	}	
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE R-Group Collection Editor</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>nosession/marvinJS/js/promise.min.js"></script>
	<script src="https://marvinjs.chemicalize.com/v1/<%= AppConfig.marvinJSLicense %>/client-settings.js"></script>
	<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
	<script type="text/javascript">

	// <!-- >

	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>

	function loadSelections() { ; }

	function saveRGroupColl(form) {
		if (isWhiteSpace(getValue('rGroupName'))) {
			alert('You must enter a name for this R group collection.');
			return;
		}
		if (isWhiteSpace(getValue('rGroupDef'))) {
			alert('You must enter a list of R group abbreviations.');
		}
		form.submit();
	}

	// -->
	</script>
</head>

<body class="light" style="margin:0px; margin-top:5px; overflow:auto; 
		background-color:#f6f7ed; text-align:left;">
<br/><br/>
	<form name="saveRGroupCollForm" id="saveRGroupCollForm"
			method="post" action="saveRGroupColl.jsp">
	<input type="hidden" NAME="rGroupNum" value="<%= rGroupClassNum %>" />
	<table style="margin-right:auto; margin-left:auto;">
		<tr><td class="boldtext" style="text-align:left;">
			Name this collection of R groups:
		</td></tr>
		<tr><td class="boldtext"> 
			<input type="text" id="rGroupName" name="rGroupName"
				 size="65" maxlength="<%= DBLimits.MAX_R_GROUP_COLL_NAME %>"
				 value="<%= Utils.toValidTextbox(rGroupClassName) %>" />
		</td></tr>
		<tr><td class="boldtext">
			Abbreviations of the R groups in this collection, 
			separated by commas:
		</td></tr>
		<tr><td class="boldtext"> 
			<textarea id="rGroupDef" name="rGroupDef"
				 cols="65" rows="1"><%= Utils.toValidHTML(Utils.join(rGroups)) %></textarea>
		</td></tr>
		<tr><td>
			<table style="margin-left:auto; margin-right:auto;"><tr><td>
			<br/><%= makeButton("Apply Changes",	
					"saveRGroupColl(document.getElementById('saveRGroupCollForm'));") %>
			</td><td>
			<br/><%= makeButton("Cancel", "self.close();") %>
			</td></tr></table>
		</td></tr>
		<tr><td class="regtext" 
				style="padding-top:10px; padding-bottom:0px; color:green;">
			Abbreviations may be atoms, SMILES strings, shortcut groups 
			recognized by Marvin, or shortcut groups defined in the
			ACE abbreviations definition file.
			<p>Use this MarvinJS&trade; panel to see if your shortcut group
			is understood by MarvinSketch.
		</td></tr>
		<tr><td class="boldtext" style="text-align:right; padding-left:10px; 
				padding-right:10px; padding-top:0px; font-style:italic;">
			MarvinJS&trade;
		</td></tr>
		<tr><td class="regtext">
			<table class="whiteTable"><tr><td>
				<div id="structureApplet" style="text-align:center;">
				<script type="text/javascript">
					// <!-- >
					startMarvinJS('<%= Utils.toValidJS(Question.EMPTY_MRV) %>', 
							 MARVIN, 0, 'structureApplet', '<%= pathToRoot %>');
					// -->
				</script>
				</div>
			</td></tr></table>
		</td></tr>
	</table>
	</form>
</body>
</html>
