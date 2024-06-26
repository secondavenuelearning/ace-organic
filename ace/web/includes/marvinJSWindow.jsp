<%@ page language="java" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.qBank.Question,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters
	final String qFlags = request.getParameter("qFlags");
	final String openerAppletName = request.getParameter("appletName");
	final String APPLET_NAME = (openerAppletName == "sketcher"
			? "sketcher1" : "sketcher");
%>
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>MarvinJS sketcher window</title>

<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>nosession/marvinJS/js/promise.min.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<script type="text/javascript">
	// <!-- >

	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>

	function loadSelections() { ; }

	function getStarted() {
		opener.marvinSketcherInstances['<%= openerAppletName %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(mol) {
			startMarvinJS(
					mol,
					MARVIN, 
					<%= qFlags %>, 
					'<%= APPLET_NAME %>', 
					'<%= pathToRoot %>',
					changeOpener); 
		}, function(error) {
			alert('Molecule export from opener failed:' + error);	
		});
	} // getStarted()

	function changeOpener() {
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(mol) {
			opener.marvinSketcherInstances['<%= openerAppletName %>'].
					importStructure(null, mol);
		}, function(error) {
			alert('Molecule export to opener failed:' + error);	
		});
	} // changeOpener()

	// -->
</script>
</head>

<body onload="getStarted();" style="overflow:auto;">
	<div id="<%= APPLET_NAME %>" style="text-align:center;">
	</div>
</body>
</html>


