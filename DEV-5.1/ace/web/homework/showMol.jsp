<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.chem.MolString,
	com.epoch.qBank.Question" 
%>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	
	final String cpds = request.getParameter("cpds");
	final boolean showLonePairs = "true".equals(request.getParameter("lonePairs"));
	long flags = Question.SHOWMAPPING;
	if (showLonePairs) flags |= Question.SHOWLONEPAIRS;
	final String title = user.translate(
			"offending".equals(request.getParameter("title"))
				? "Offending Compounds" : "Calculated Products");
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<title><%= title %></title>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= 
		AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<script type="text/javascript">
	// <!-- >

	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>

	function closeMe(e) {
		if (e && ([10, 13].contains(e.keyCode))) self.close();
	} // closeMe()

	// -->
</script>
</head>
<body onkeypress="closeMe(event);" style="background-color:#f6f7ed; margin:0px; overflow:auto;">
	<table style="margin-left:auto; margin-right:auto; 
			background-color:#f6f7ed; width:378px;" summary="">
	<tr><td class="boldtext big" style="padding-left:10px; 
			padding-right:10px; padding-top:10px;">
		<%= title %>
	</td></tr>
	<tr><td class="whiteTable" id="calcProds"
			style="text-align:center; padding-left:10px; padding-right:10px;">
		<%= MolString.getImage(pathToRoot, cpds, flags, "1", user.prefersPNG()) %>
	</td></tr>
	</table>
</body>
</html>
