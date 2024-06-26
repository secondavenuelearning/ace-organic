<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	final int instructorNum = MathUtils.parseInt(request.getParameter("instructorNum"));
	final User instructor = ((AdminSession) userSess).getUser(instructorNum);
	boolean allowMasterEdit = "true".equals(request.getParameter("allowMasterEdit"));
	boolean allowTranslation = "true".equals(request.getParameter("allowTranslation"));
	/* Utils.alwaysPrint("setPermissions.jsp: instructor = ", instructor.getName(),
			", instructorNum = ", instructorNum,
			", allowMasterEdit = ", allowMasterEdit,
			", allowTranslation = ", allowTranslation); /**/
	instructor.setInstructorPermissions(allowMasterEdit, allowTranslation); 
%>

<html>
<head>
</head>
<body>
		instructorName = @@@@<%= instructor.getName().toString() %>@@@@
</body>
</html>
