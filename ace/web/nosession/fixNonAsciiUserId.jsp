<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.courseware.Institution,
	com.epoch.courseware.User,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>

<%	final String pathToRoot = "../"; 
	request.setCharacterEncoding("UTF-8");

	final String unicodeUserId = request.getParameter("unicodeUserId").trim();
	final String storedUserId = request.getParameter("storedUserId").trim();
	final int instnId = MathUtils.parseInt(request.getParameter("instnId"));
	final String studentNum = request.getParameter("studentNum").trim();
	String chosenLang = request.getParameter("language");
	if (chosenLang == null && !"English".equals(AppConfig.defaultLanguage)) {
		chosenLang = AppConfig.defaultLanguage;
	}

	final User tempUser = new User(storedUserId);
	tempUser.setInstitution(new Institution(instnId));
	tempUser.setStudentNum(studentNum);
	tempUser.setLanguage(chosenLang);

	boolean success = false;
	final String[] secResponses = new String[]
			{Utils.inputToCERs(request.getParameter("secResp1")), 
			Utils.inputToCERs(request.getParameter("secResp2"))};
	/*/ System.out.println("fixNonAsciiUserId.jsp: unicodeUserId = "
			+ unicodeUserId + ", storedUserId = " + storedUserId
			+ ", instnId = " + instnId + ", studentNum = "
			+ studentNum + ", secResp1 = " + secResponses[0] 
			+ ", secResp2 = " + secResponses[1]); /**/
	final boolean match = 
			tempUser.matchSecurityAnswers(secResponses, !User.BY_USER_ID);
	if (match) {
		success = tempUser.fixNonAsciiUserId(unicodeUserId);
	}
	/*/ Utils.alwaysPrint("fixNonAsciiUserId.jsp: match = ", match, 
			", success = ", success); /**/
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
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon">
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css">
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>

<script type="text/javascript">
	// <!-- >

	function finish() {
		<% if (match && success) { %>
			toAlert('<%= tempUser.translateJS("You should be able to login now.") %>');
		<% } else if (match) { %>
			toAlert('<%= tempUser.translateJS("If you are not able to log in, "
					+ "it's not because your tempUsername contains non-ASCII "
					+ "characters. Please contact your instructor.") %>');
		<% } else { %>
			toAlert('<%= tempUser.translateJS("Your answers to your security "
					+ "questions are incorrect. Please contact your instructor.") %>');
		<% } %>
		self.location.href = '<%= pathToRoot %>login.jsp?flag=Success&language='
				+ encodeURIComponent('<%= Utils.toValidJS(chosenLang) %>');
	} // finish()

	// -->
</script>

</head>

<body class="light" onload="finish();">
</body>
</html>
