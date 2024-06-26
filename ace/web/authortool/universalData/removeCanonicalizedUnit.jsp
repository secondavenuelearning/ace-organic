<%@ page language="java" %>
<%@ page import="
	com.epoch.db.CanonicalizedUnitRW,
	com.epoch.exceptions.DBException,
	com.epoch.physics.CanonicalizedUnit,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	final String unitSymbol = request.getParameter("unitSymbol");
	final String unitName = request.getParameter("unitName");
	Utils.alwaysPrint("removeCanonicalizedUnit.jsp: unitSymbol = ",
			unitSymbol, ", unitName = ", unitName);
	String msg = null;
	try {
		CanonicalizedUnitRW.removeUnit(unitSymbol, unitName);
	} catch (DBException e) {
		msg = "Unable to remove unit " + unitSymbol + " (" + unitName + ").";
	} // try
	/**/
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<script type="text/javascript">
	// <!-- >
	function finish() {
		<% if (msg != null) { %>
			alert('<%= Utils.toValidJS(msg) %>');
		<% } %>
		opener.location.reload(); 
		self.close();
	} // finish()
	// -->
	</script>
</head>
<body onload="finish();">
</body>

</html>
