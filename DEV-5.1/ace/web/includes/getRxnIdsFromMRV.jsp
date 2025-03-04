<%@ page language="java" %>
<%@ page import="
	com.epoch.synthesis.Synthesis,
	com.epoch.utils.Utils"
%>
<html>
<body>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String mrvStr = request.getParameter("mrvStr");
	final String rxnIdsStr = Synthesis.getRxnConditions(mrvStr);
	/* Utils.alwaysPrint("getRxnIdsFromMRV.jsp: rxnIdsStr = ", rxnIdsStr,
			", mrvStr = ", mrvStr); /**/
%>
	rxnIdsStrValue = @@@@<%= rxnIdsStr %>@@@@
</body>
</html>
