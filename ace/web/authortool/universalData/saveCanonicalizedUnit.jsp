<%@ page language="java" %>
<%@ page import="
	com.epoch.db.CanonicalizedUnitRW,
	com.epoch.physics.CanonicalizedUnit,
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

	final String unitSymbol = 
			Utils.inputToCERs(request.getParameter("unitSymbol").trim());
	final String unitName = 
			Utils.inputToCERs(request.getParameter("unitName").trim());
	final String whatMeasures = 
			Utils.inputToCERs(request.getParameter("whatMeasures").trim());
	final double coeff = 
			MathUtils.parseDouble(request.getParameter("coeff").trim());
	final int power10 = 
			MathUtils.parseInt(request.getParameter("power10").trim());
	final String[] SI_UNIT_SYMBOLS = CanonicalizedUnit.SI_UNIT_SYMBOLS;
	final int[] unitSIPowers = new int[SI_UNIT_SYMBOLS.length];
	int unitNum = 0;
	for (final String SI_UNIT : SI_UNIT_SYMBOLS) {
		unitSIPowers[unitNum++] = 
				MathUtils.parseInt(request.getParameter(SI_UNIT).trim());
	} // for each fundamental SI unit
	final CanonicalizedUnit unit = new CanonicalizedUnit(unitSymbol, 
			unitName, whatMeasures, coeff, power10, unitSIPowers);
	final boolean success = CanonicalizedUnitRW.setUnit(unit);
	final String msg = (success ? null : "Unable to write unit to the "
			+ "database; it probably exists already.");
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
		<% } else { %>
			opener.location.reload(); 
		<% } %>
		self.close();
	} // finish()
	// -->
	</script>
</head>
<body onload="finish();">
</body>

</html>
