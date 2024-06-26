<%@ page language="java" %>
<%@ page import="
	com.epoch.chem.MolString,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<html>
<body>
<%
	request.setCharacterEncoding("UTF-8");
	String pathToRoot = request.getParameter("pathToRoot");
	String molStr = request.getParameter("molStr");
	if (!Utils.isEmpty(molStr)) {
		final long qFlags = MathUtils.parseLong(request.getParameter("qFlags"));
		/* Utils.alwaysPrint("updateFigure.jsp: qFlags = ", qFlags,
				", molecule:\n", molStr);  /**/
		final boolean isLewis = molStr.contains("Lewis ");
		final String imgXML = MolString.getImage(pathToRoot, molStr, qFlags);
		/* Utils.alwaysPrint("updateFigure.jsp: imgXML:\n", imgXML); /**/
%>
		imageXMLValue = @@@@<%= Utils.lineBreaksToJS(imgXML) %>@@@@
<% 	} // if there's a zeroeth figure
	int figNum = 1;
	while (true) {
		final String figId = request.getParameter(Utils.toString("figId", figNum));
		/* Utils.alwaysPrint("updateFigure.jsp: figId = ", figId); /**/
		if (Utils.isEmpty(figId)) break;
		molStr = request.getParameter(Utils.toString("molStr", figNum));
		final long qFlags = MathUtils.parseLong(request.getParameter(
				Utils.toString("qFlags", figNum)));
		/* Utils.alwaysPrint("updateFigure.jsp: qFlags = ", qFlags,
				", molecule ", figNum, ":\n", molStr); /**/
		final boolean isLewis = molStr.contains("Lewis ");
		final String imgXML = MolString.getImage(pathToRoot, molStr, qFlags);
		/* Utils.alwaysPrint("updateFigure.jsp: imgXML", figNum, ":\n", imgXML); /**/
%>
		figId<%= figNum %> = @@@@<%= Utils.lineBreaksToJS(figId) %>@@@@
		imageXMLValue<%= figNum %> = @@@@<%= Utils.lineBreaksToJS(imgXML) %>@@@@
<%		figNum++;
	} // while there are more figures 
	final String addClick = request.getParameter("addClick");
	if (addClick != null) { %>
		addClickValue = @@@@<%= addClick %>@@@@
<% 	} // if there's an addClick parameter
	final String figCellPrefix = request.getParameter("figCellPrefix");
	if (figCellPrefix != null) { %>
		figCellPrefixValue = @@@@<%= figCellPrefix %>@@@@
<% 	} // if there's an figCellPrefix parameter
%>
</body>
</html>
