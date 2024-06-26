<%@ page language="java" %>
<%@ page import="
	com.epoch.qBank.QSetDescr,
	com.epoch.session.QSet,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../../";

	QSet currentQSet;
	synchronized (session) {
		currentQSet = (QSet) session.getAttribute("qSet");
	}
	final QSetDescr descr = currentQSet.revertQSetDescr(); 
	// Utils.alwaysPrint("revertStatement.jsp: new header = ", descr.header);
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
	<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
		// <!-- >
		function closeThis() {
			window.opener.setInnerHTML('commonQstatement', 
					'<%= (descr.header.length() == 0) ?  "[None]" 
					: Utils.toValidJS(Utils.toDisplay(descr.header)) %>');
			window.opener.hideCell('revertButton');
			self.close();
		} // closeThis
		// -->
	</script>
	</head>
<body onload="closeThis();"></body>
</html>

