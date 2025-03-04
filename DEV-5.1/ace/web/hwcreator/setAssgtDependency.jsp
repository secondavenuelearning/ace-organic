<%@ page language="java" %>
<%@ page import="
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	response.setHeader("Cache-Control", "no-cache, must-revalidate"); // HTTP 1.1
	response.setHeader("Pragma", "no-cache"); // HTTP 1.0
	response.setDateHeader ("Expires", 0); // prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	final int dependentHWNum = MathUtils.parseInt(request.getParameter("dependentHWNum"));
	final int masteryHWNum = MathUtils.parseInt(request.getParameter("masteryHWNum"));
	/* Utils.alwaysPrint("setAssgtDependency.jsp: dependentHWNum = ", dependentHWNum,
			", masteryHWNum = ", masteryHWNum); /**/
	InstructorSession instrSess;
	switch (role) {
		case User.ADMINISTRATOR:
			final AdminSession adminSess = (AdminSession) userSess;
			adminSess.setAssgtDependency(dependentHWNum, masteryHWNum);
			break;
		case User.INSTRUCTOR:
			instrSess = (InstructorSession) userSess;
			instrSess.setAssgtDependency(dependentHWNum, masteryHWNum);
			break;
		case User.STUDENT:
		default: // shouldn't happen
			if (isTA) {
				instrSess = (InstructorSession) userSess;
				instrSess.setAssgtDependency(dependentHWNum, masteryHWNum);
			} // if isTA
			break;
	} // switch role
%>
<html>
<body>
</body>
</html>
