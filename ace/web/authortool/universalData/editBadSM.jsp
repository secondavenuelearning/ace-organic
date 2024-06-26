<%@ page language="java" %>
<%@ page import="
	com.epoch.chem.MolString,
	com.epoch.db.dbConstants.DBLimits,
	com.epoch.qBank.Question,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";
	final String pathToFolder = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	String smName = request.getParameter("smName");
	String smDef = request.getParameter("smDef");
	boolean replace = true;
	if (smName == null) {
		replace = false;
		smName = "";
		smDef = "";
	}
	final String APPLET_NAME = "structureApplet";
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Impossible Starting Material Editor</title>
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

	function saveIt() {
		var form = document.saveBadSM;
		if (isWhiteSpace(form.smDef.value)) {
			alert('You must enter a SMILES definition.');
			return;
		}
		if (isWhiteSpace(form.smName.value)) {
			alert('You must enter a description of the '
					+ 'impossible starting material.');
			return;
		}
		form.submit();
	} // saveIt()

	function getSmiles() {
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('smiles').then(function(source) {
			document.saveBadSM.smDef.value = source;
		}, function(error) {
			alert('Molecule export failed:' + error);	
		});
	} // getSmiles()

	// -->
</script>
</head>

<body class="light" style="margin-left:auto; margin-right:auto; margin-top:5px; 
		width:95%; overflow:auto; background-color:#f6f7ed; text-align:left;">
<br/><br/>
	<form name="saveBadSM" method="post" action="saveBadSM.jsp">
	<input type="hidden" name="replace" value="<%= replace %>" />
	<% if (replace) { %>
		<input type="hidden" name="oldName" value="<%= 
				Utils.toValidHTMLAttributeValue(smName) %>" />
	<% } %>
	<table style="margin-right:auto; margin-left:auto;">
		<tr>
		<td colspan="2" class="boldtext">
			Impossible starting material description:
		</td>
		</tr>
		<tr>
		<td colspan="2" class="boldtext"> 
   			<input type="text" name="smName" size="65" 
				maxlength="<%= DBLimits.MAX_IMPOSSIBLE_SM_NAME %>"
				value="<%= Utils.toValidTextbox(smName) %>" />
		</td>
		</tr>
		<tr><td colspan="2" class="regtext">
			<br/><br/>Enter a new SMILES definition or edit the existing one: 
			<br/><textarea name="smDef" style="font-family:Courier;" 
					cols="65" rows="5"><%= 
							Utils.toValidTextbox(smDef) %></textarea>
		</td></tr>
		<tr><td class="regtext" 
				style="padding-top:10px; padding-bottom:10px; color:green;">
			Use this MarvinJS&trade; 
			panel to generate or verify your SMILES definition.
		</td></tr>
		<tr><td colspan="2" class="boldtext" style="text-align:right; padding-left:10px; 
				padding-right:10px; padding-top:10px; font-style:italic;">
			MarvinJS&trade;
		</td></tr>
		<tr><td colspan="2" class="regtext">
			<table class="whiteTable"><tr><td>
				<div id="<%= APPLET_NAME %>" style="text-align:center;">
				<script type="text/javascript">
					// <!-- >
					startMarvinJS('<%= Utils.toValidJS(
								MolString.convertMol(smDef, Utils.MRV)) %>', 
							 MARVIN, SHOWNOH, '<%= APPLET_NAME %>', 
							 '<%= pathToRoot %>');
					// -->
				</script>
				</div>
			</td></tr></table>
		</td></tr>
		<tr><td style="text-align:center;">
			<br/>
			<table style="margin-right:auto; margin-left:auto;">
			<tr><td>
				<%= makeButton("Get SMILES", "getSmiles();") %>
			</td><td>
				<%= makeButton("Apply Changes", "saveIt();") %>
			</td><td>
				<%= makeButton("Cancel", "self.close();") %>
			</td></tr>
			</table>
		</td></tr>
	</table>
	</form>
</body>
</html>
