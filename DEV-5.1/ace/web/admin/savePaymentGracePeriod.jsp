<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.Institution,
	com.epoch.utils.MathUtils"
%>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	Institution[] institutions;
	synchronized(session) {
		institutions = (Institution[]) session.getAttribute("institutions");
	}
	final int instnId = MathUtils.parseInt(request.getParameter("instnId"));
	final int gracePeriod = MathUtils.parseInt(request.getParameter("gracePeriod"));
	final String instnName = request.getParameter("instnName");
	 Utils.alwaysPrint("savePaymentGracePeriod.jsp: instn = ", instnName,
			", instnId = ", instnId, ", gracePeriod = ", gracePeriod); /**/
	final AdminSession adminSess = (AdminSession) userSess;
	adminSess.setPaymentGracePeriod(instnId, gracePeriod); 
%>

<html>
<body>
</body>
</html>
