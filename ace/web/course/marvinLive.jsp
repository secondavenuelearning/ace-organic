<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final String url = ((HttpServletRequest) request).getRequestURL().toString();
	final String[] doubleSlashSplit = url.split("//");
	final String[] slashSplit = doubleSlashSplit[1].split("/");
	final String[] colonSplit = slashSplit[0].split(":");
	final String rootURL = Utils.toString("http://", colonSplit[0]);

	int port = MathUtils.parseInt(request.getParameter("port"));
	final String idleTime = request.getParameter("idleTime");
	final boolean noActivePort = port == 0;
	 Utils.alwaysPrint("marvinLive.jsp: rootURL = ", rootURL, 
	 		", port = ", port, ", idleTime = ", idleTime); /**/
	if (noActivePort) {
		final InstructorSession instrSess = (InstructorSession) userSess;
		port = instrSess.createMarvinLivePort(rootURL, idleTime);
		 Utils.alwaysPrint("marvinLive.jsp: new port = ", port); /**/
	} // if it's a new Marvin Live session
%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<title>Entering Marvin Live</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	function goToMarvinLive() {
		var locn = '<%= rootURL %>:<%= port %>/';
		<% if (noActivePort && port != 0) { %>
			window.opener.document.getElementById('marvinLiveButton').innerHTML =
					'<%= Utils.toValidJS(makeButton(
						user.translate("Join chat room"), 
						"marvinLive(", port, ");")) %>';
			window.opener.document.getElementById('idleTimeCell').innerHTML =
					'<%= Utils.toValidJS(makeButton(
						user.translate("Close chat room"), 
						"endMarvinLive();")) %>';
			toAlert('<%= user.translateJS("You may need to refresh the page "
					+ "before Marvin Live appears.") %>');
		<% } // if port has been made active %>
		this.location.href = locn;
	} // goToMarvinLive()
	// -->
</script>
</head>
<body onload="goToMarvinLive();">
</body>
</html>
