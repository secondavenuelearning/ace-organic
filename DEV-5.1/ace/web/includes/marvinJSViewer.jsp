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
	final String viewOpts = request.getParameter("viewOpts");
	final String mol = request.getParameter("mol");
	final String getMolMethodName = request.getParameter("getMolMethodName");
	// Utils.alwaysPrint("marvinJSViewer.jsp: mol = ", mol);
%>
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>MarvinJS viewer window</title>

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

	// -->
</script>
</head>

<body style="overflow:auto;">
	<div id="sketcher" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS(
				opener.<%= getMolMethodName %>,
				MARVIN, 
				<%= viewOpts %>, 
				'sketcher', 
				'<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</body>
</html>


