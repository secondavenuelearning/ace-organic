<%@ page language="java" %>
<%@ page errorPage="/errorlangss/errorHandler.jsp" %>
<%@ page import="
	com.epoch.utils.Utils,
	java.util.Arrays"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");
	/* parameters
				editindex - number if the user is edited by the admin
							null, if an ordinary user is editing his profile
	*/

	User editUser = user;
	final String editIndex = request.getParameter("editindex");
	if (editIndex != null) {
		final int index = Integer.parseInt(editIndex);
		editUser = ((AdminSession) userSess).getUser(index);
	}

	final String newOrder = request.getParameter("newOrder");
	if (!Utils.isEmpty(newOrder)) {
		editUser.setLanguageOrder(
				Utils.stringToIntArray(newOrder.split(":")));
	}

	final String newLanguageRaw = request.getParameter("newLanguage");
	final String newLanguage = Utils.inputToCERs(newLanguageRaw);
	if (!Utils.isEmptyOrWhitespace(newLanguage)) {
		Utils.alwaysPrint("saveLanguages.jsp: newLanguageRaw = ", newLanguageRaw,
				", newLanguage = ", newLanguage);
		editUser.addLanguage(newLanguage.trim());
	}

	final String removeLangPosn = request.getParameter("removeLangPosn");
	if (!Utils.isEmpty(removeLangPosn)) {
		editUser.removeLanguage(Integer.parseInt(removeLangPosn));
	}

	final String[] editUserLangs = editUser.getLanguages();
	String langsStr = Arrays.toString(editUserLangs);
	if (editUserLangs.length > 0) langsStr = Utils.endsChop(langsStr, 1, 1);

	final String goBack = request.getParameter("goBack");

%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
</head>
<body class="light" style="background-color:white;">
	<script type="text/javascript">
		// <!-- >
		var out =
		<% if (editUserLangs.length > 1) { %>
			'<%= editUser.translateJS("Your languages are now ***French, Spanish***, "
					+ "and English, in that order.", langsStr) %>';
		<% } else if (editUserLangs.length == 1) { %>
			'<%= editUser.translateJS("Your languages are now ***Spanish*** "
					+ "and English, in that order.", langsStr) %>';
		<% } else { %>
			'Your only language now is English.';
		<% } %>
		toAlert(out);
		self.location.href = '<%= goBack %>';
		// -->
	</script>
</body>
</html>