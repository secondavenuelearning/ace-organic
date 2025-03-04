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
	
	Textbook book;
	synchronized (session) {
		book = (Textbook) session.getAttribute("textbook");
	}
	final String name = 
			Utils.inputToCERs(request.getParameter("bookName").trim());
	final boolean isVisibleToAll = 
			"on".equals(request.getParameter("visibleToAll"));
	book.setName(name);
	book.setVisibility(isVisibleToAll);

	final String action = request.getParameter("action");
	final StringBuilder endActionBld = new StringBuilder();

	if ("moveChapter".equals(action)) {
		final int from = MathUtils.parseInt(request.getParameter("chapNum"));
		final int to = MathUtils.parseInt(request.getParameter("moveTo"));
		book.moveChapter(from, to);
		endActionBld.append("writeTextbook.jsp");
	} else if ("deleteChapter".equals(action)) {
		book.deleteChapter(MathUtils.parseInt(request.getParameter("chapNum")));
		endActionBld.append("writeTextbook.jsp");
	} else if ("openChapter".equals(action)) {
 		Utils.appendTo(endActionBld, "writeChapter.jsp?chapNum=", 
				request.getParameter("chapNum"), "&saved=", 
				request.getParameter("saved"));
	} else if ("save".equals(action)) {
		final boolean newOwner = book.getId() != 0
				&& !user.getUserId().equals(book.getLockHolder());
		/* Utils.alwaysPrint("textbookActions.jsp: userId = ", user.getUserId(),
				", owner = ", book.getOwnerId(), ", lock holder = ",
				book.getLockHolder(), ", book ID = ", book.getId(),
				", newOwner = ", newOwner); /**/
		book.setOwnerId(user.getUserId());
		book.save();
		synchronized (session) {
			session.setAttribute("lastModifiedTextbook", new Textbook(book));
		} // synchronized
		endActionBld.append("writeTextbook.jsp?saved=true");
		if (newOwner) endActionBld.append("&newOwner=true");
	} else if ("releaseLock".equals(action)) {
		(role == User.INSTRUCTOR 
				? (InstructorSession) userSess
				: (AdminSession) userSess).releaseLockedTextbook(book.getId());
		endActionBld.append("chooseTextbook.jsp");
	} // if action
	
	final String endAction = endActionBld.toString();
	// Utils.alwaysPrint("dbactions.jsp: endAction = ", endAction);

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
			self.location.href = '<%= endAction %>';
		} // finish()
		// -->
	</script>
</head>
<body class="regtext" onload="finish();">
</body>
</html>
