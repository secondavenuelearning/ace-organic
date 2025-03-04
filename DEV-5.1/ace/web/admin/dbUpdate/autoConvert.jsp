<%@ page language="java" %>
<%@ page import="
	com.epoch.db.DataConversion,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	String msg = "Done.";
	final String toConvert = request.getParameter("toConvert");
	if ("synFormat".equals(toConvert)) 
		DataConversion.convertSynthesisFormat();
	else if ("makeForeignKeysDeferrable".equals(toConvert)) 
		DataConversion.makeForeignKeysDeferrable();
	else if ("Lewis".equals(toConvert)) 
		DataConversion.convertLewisFormat();
	else if ("BLOBs".equals(toConvert)) 
		msg = DataConversion.writeBLOBsAsCLOBs();
	else if ("textContains".equals(toConvert)) 
		DataConversion.convertTextCont();
	else if ("1HAnd2H".equals(toConvert)) 
		DataConversion.convert1H2H();
	else if ("synthSMExpr".equals(toConvert)) 
		DataConversion.convertSynthSMExprs();
	else if ("images".equals(toConvert)) 
		DataConversion.writeImageFiles();
	else if ("clickHereToXML".equals(toConvert)) 
		DataConversion.convertClickHereCoordsToXML();
	else if ("newAssgtTables".equals(toConvert)) {
		DataConversion.makeNewHWQsTable();
	} else if ("hwQs".equals(toConvert)) 
		DataConversion.convertHWQs();
	else if ("captions".equals(toConvert)) 
		DataConversion.splitCaptions();
	else msg = "Unknown conversion request '" + toConvert + "'.";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon">
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<style type="text/css">
		* html body {
			padding:55px 0 40px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">

	function saveMe() {
		alert('<%= Utils.toValidJS(msg) %>');
		self.location.href = 'updateDB.jsp';
	}

	</script>
</head>

<body style="overflow:auto;" onload="saveMe();">

</body>
</html>
