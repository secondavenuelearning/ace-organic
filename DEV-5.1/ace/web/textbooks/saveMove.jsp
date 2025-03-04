<%@ page language="java" %>
<%@ page import="
	com.epoch.textbooks.Textbook,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	final String pathToRoot = "../";

	Textbook book;
	synchronized (session) {
		book = (Textbook) session.getAttribute("textbook");
	} // synchronized
	final int[] range = new int[] {MathUtils.parseInt(request.getParameter("from")), 
			MathUtils.parseInt(request.getParameter("to"))};
	final int startChapNum = MathUtils.parseInt(request.getParameter("startChapNum"));
	final int targetChapNum = MathUtils.parseInt(request.getParameter("targetChapNum"));
	final int posn = MathUtils.parseInt(request.getParameter("posn"));
	final String newChapName = request.getParameter("newChapName");
	final boolean showTargetChap = "on".equals(request.getParameter("showTargetChap"));
	/* Utils.alwaysPrint("saveMove.jsp: startChapNum = ", startChapNum, 
			", range = ", range, ", targetChapNum = ", targetChapNum, 
			", posn = ", posn, ", newChapName = ", newChapName,
			", showTargetChap = ", showTargetChap); /**/ 
	book.moveContents(startChapNum, range, targetChapNum, posn, newChapName);
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<script type="text/javascript">
		// <!-- >
		function finish() {
			opener.location.href = 'writeChapter.jsp?chapNum=<%= 
					!showTargetChap ? startChapNum 
					: targetChapNum == 0 ? book.getNumChapters()
					: targetChapNum %>&saved=false&rand=<%= 
					Utils.getRandName() %>#content<%= posn %>';
			self.close();
		}
		// -->
	</script>
</head>
<body onload="finish();">
</body>
</html>
