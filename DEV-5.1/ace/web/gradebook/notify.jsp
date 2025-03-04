<%@ page language="java" %>
<%@ page import="
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final int hwNum = MathUtils.parseInt(request.getParameter("hwNum"));
	final int qNum = MathUtils.parseInt(request.getParameter("qNum"));
	final String userIdsStr = request.getParameter("userIds");
	final String[] userIds = userIdsStr.split(Utils.COMMA);
	final String msg = (qNum == 0 
			? user.translate("Your instructor regraded your responses to all "
				+ "questions in assignment ***1***, and one or more of your "
				+ "grades changed.", hwNum)
			: user.translate("Your instructor regraded your response to "
				+ "question ***1*** in assignment ***1***, and your grade "
				+ "changed.", new int[] {qNum, hwNum}));
	userSess.sendTextMessagesToIds(userIds, msg);
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon">
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >
	function finish() {
		toAlert('<%= user.translateJS("Done.") %>');
		self.close();
	} // finish()
	// -->
	</script>
</head>

<body style="overflow:auto;" onload="finish();">

</body>
</html>
