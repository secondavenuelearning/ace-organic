<%@ page language="java" %>
<%@ page import="
	com.epoch.db.UnitConvertRW,
	com.epoch.exceptions.DBException,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	final String unitFrom = Utils.inputToCERs(request.getParameter("unitFrom").trim());
	final String unitTo = Utils.inputToCERs(request.getParameter("unitTo").trim());
	final double power = MathUtils.parseDouble(request.getParameter("power").trim());
	final double factor = MathUtils.parseDouble(request.getParameter("factor").trim());

	String msg = null;
	if (factor == 0.0) {
		msg = "Unable to parse conversion factor.";
	} else try {
		UnitConvertRW.setUnitConversion(unitFrom, unitTo, new double[] {power, factor});
	} catch (DBException e) {
		msg = "Unable to write conversion power and factor to the database; "
				+ "it probably exists already.";
	} // try
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >
	function finish() {
		<% if (msg != null) { %>
			toAlert('<%= pathToRoot %>', '<%= Utils.toValidJS(msg) %>');
		<% } else { %>
			opener.location.reload(); 
		<% } %>
		self.close();
	}
	// -->
	</script>
</head>
<body onload="finish();">
</body>

</html>
