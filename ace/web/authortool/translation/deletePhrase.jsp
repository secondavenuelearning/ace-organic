<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%	
	request.setCharacterEncoding("UTF-8");
	final int phraseId = MathUtils.parseInt(request.getParameter("phraseId"));
	// Utils.alwaysPrint("deletePhrase.jsp: phraseId = ", phraseId);
	switch (role) {
		case User.ADMINISTRATOR:
		case User.INSTRUCTOR:
			if (phraseId != 0) {
				((InstructorSession) userSess).deleteEnglishPhrase(phraseId);
				// Utils.alwaysPrint("deletePhrase.jsp: phrase deleted ");
			}
			break;
		default: // do nothing
	} // switch
%>
<html>
<head>
</head>
<body>
<!-- place values so updatePage() can find them
	follows ACE conventions: four-@-delimited strings.
	phraseIdValue = @@@@<%= phraseId %>@@@@
-->
</body>
</html>

