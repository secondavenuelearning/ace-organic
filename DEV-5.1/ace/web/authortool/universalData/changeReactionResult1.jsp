<%@ page language="java" %>
<%@ page import="
	com.epoch.synthesis.RxnCondition,
	com.epoch.utils.MathUtils,
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
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final int rxnId = MathUtils.parseInt(request.getParameter("rxnId"));
	final RxnCondition rxnCondn = RxnCondition.getRxnCondition(rxnId);
	final String rxnCondnName = rxnCondn.name;
	final String APPLET_NAME = "structureApplet";
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Reaction Result Changer</title>
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

	function storeReaction() {
		var form = document.deleteReactionResult;
			marvinSketcherInstances['<%= APPLET_NAME %>'].
					exportStructure('<%= MRV_EXPORT %>').then(function(source) {
				form.reaction.value = source;
			form.submit();
		}, function(error) {
			alert('Molecule export failed:' + error);	
		});
	}

	// -->
</script>
</head>

<body class="light" style="margin:0px; margin-top:5px; overflow:auto; 
		background-color:#f6f7ed; text-align:left;">
<br/><br/>
	<form name="changeReactionResult" method="post" action="changeReactionResult2.jsp">
	<input type="hidden" name="rxnId" value="<%= rxnId %>" />
	<input type="hidden" name="reaction" value="" />
	<table style="margin-right:auto; margin-left:auto; width:95%;">
		<tr><td class="regtext" 
				style="padding-top:10px; padding-bottom:10px;">
			Enter a synthesis with the starting material(s) and product(s), 
			for <%= Utils.toDisplay(rxnCondnName) %> that you want
			to store in the database, overwriting any existing
			record of the same starting material(s) under these
			reaction conditions:
		</td></tr>
		<tr><td class="boldtext" style="text-align:right; padding-left:10px; 
				padding-right:10px; padding-top:10px; font-style:italic;">
			MarvinJS&trade;
		</td></tr>
		<tr><td class="regtext">
			<table class="whiteTable"><tr><td>
				<div id="<%= APPLET_NAME %>" style="text-align:center;">
				<script type="text/javascript">
					// <!-- >
					startMarvinJS('<%= Utils.toValidJS(Question.EMPTY_MRV) %>', 
							 MARVIN, 0, '<%= APPLET_NAME %>', '<%= pathToRoot %>');
					// -->
				</script>
				</div>
			</td></tr></table>
		</td></tr>
		<tr><td style="text-align:center;">
			<br/>
			<table style="margin-right:auto; margin-left:auto;">
			<tr><td>
				<%= makeButton("Store reaction", "storeReaction();") %>
			</td><td>
				<%= makeButton("Cancel", "self.close();") %>
			</td></tr>
			</table>
		</td></tr>
	</table>
	</form>
</body>
</html>
