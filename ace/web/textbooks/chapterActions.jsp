<%@ page language="java" %>
<%@ page import="
	com.epoch.textbooks.Textbook,
	com.epoch.textbooks.TextChapter,
	com.epoch.textbooks.TextContent,
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
	final int chapNum = MathUtils.parseInt(request.getParameter("chapNum"));
	final TextChapter chapter = book.getChapter(chapNum);
	final String name = Utils.inputToCERs(request.getParameter("chapterName").trim());
	chapter.setName(name);

	final String action = request.getParameter("action");
	final int contentNum = MathUtils.parseInt(request.getParameter("contentNum"));
	StringBuilder endActionBld = Utils.getBuilder("writeChapter.jsp?chapNum=", chapNum);
	/* Utils.alwaysPrint("textbookActions.jsp: action = ", action, 
			", chapNum = ", chapNum, ", contentNum = ", contentNum); /**/

	if ("moveContent".equals(action)) {
		final int to = MathUtils.parseInt(request.getParameter("moveTo"));
		chapter.moveContent(contentNum, to);
 		Utils.appendTo(endActionBld, "#content", to);
	} else if ("deleteContent".equals(action)) {
		chapter.deleteContent(contentNum);
		final int numContents = chapter.getNumContents();
 		Utils.appendTo(endActionBld, "#content", 
				contentNum > numContents ? numContents : contentNum);
	} else if (Utils.among(action, "editContent", "reload")) {
		// do nothing, return to page
		final int numContents = chapter.getNumContents();
 		Utils.appendTo(endActionBld, "#content", 
				contentNum > numContents || contentNum == 0 
					? numContents : contentNum);
	} else if (Utils.among(action, "cancel", "reset")) {
		synchronized (session) {
			final Textbook lastModifiedBook = 
					(Textbook) session.getAttribute("lastModifiedTextbook");
			final TextChapter origChap = lastModifiedBook.getChapter(chapNum);
			if (origChap == null) book.deleteChapter(chapNum);
			else book.setChapter(chapNum, new TextChapter(origChap));
		} // synchronized
		if ("cancel".equals(action)) {
			endActionBld = new StringBuilder().append("writeTextbook.jsp");
		} else {
 			Utils.appendTo(endActionBld, "#content", contentNum);
		} // if cancelling
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
		if (newOwner) endActionBld.append("&newOwner=true");
 		Utils.appendTo(endActionBld, "&saved=true#content", contentNum);
	} else if ("writeTextbook".equals(action)) {
		synchronized (session) {
			final Textbook lastModifiedBook = 
					(Textbook) session.getAttribute("lastModifiedTextbook");
			if (chapter.getId() == 0) lastModifiedBook.addChapter(chapter);
			else lastModifiedBook.setChapter(chapNum, chapter);
		} // if chapter already existed
		endActionBld = new StringBuilder().append("writeTextbook.jsp");
	} else if ("releaseLock".equals(action)) {
		(role == User.INSTRUCTOR 
				? (InstructorSession) userSess
				: (AdminSession) userSess).releaseLockedTextbook(book.getId());
		endActionBld = new StringBuilder().append("chooseTextbook.jsp");
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
<body onload="finish();">
</body>
</html>
