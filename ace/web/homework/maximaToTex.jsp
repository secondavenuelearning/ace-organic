<%@ page language="java" %>
<%@ page import="
	com.epoch.physics.EquationFunctions,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
%>
<html>
<body>
texValue = @@@@<%= Utils.lineBreaksToJS(EquationFunctions.toTeX(
		request.getParameter("maximaEqn"))) %>@@@@
eqnNum = @@@@<%= request.getParameter("eqnNum") %>@@@@
</body>
</html>
