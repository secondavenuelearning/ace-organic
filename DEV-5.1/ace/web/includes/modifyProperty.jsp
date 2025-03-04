<%@ page language="java" %>
<%@ page import="
	com.epoch.chem.ChemUtils,
	com.epoch.utils.Utils"
%>
<html>
<body>
<%
	request.setCharacterEncoding("UTF-8");
	final String mrv = request.getParameter("mrv");
	final String propertyName = request.getParameter("propertyName");
	final String propertyValue = request.getParameter("propertyValue");

	if (!Utils.isEmpty(mrv)) {
		Utils.alwaysPrint("includes/modifyProperty.jsp: propertyName = ",
				propertyName, ", propertyValue = ", propertyValue, ", mrv=\n", mrv);
		final String newMrv = ChemUtils.setProperty(mrv, propertyName, propertyValue);
		Utils.alwaysPrint("includes/modifyProperty.jsp: newMrv=\n", newMrv);
%>
		mrvValue = @@@@<%= Utils.lineBreaksToJS(newMrv) %>@@@@
<% 	} // if there's a zeroeth figure
%>
</body>
</html>
