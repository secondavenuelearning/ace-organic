<%@ page language="java" %>
<%@ page import="
	com.epoch.chem.ChemUtils,
	com.epoch.qBank.Question,
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
	final String pathToRoot = "../";

	Textbook book;
	synchronized (session) {
		book = (Textbook) session.getAttribute("textbook");
	} // synchronized
	final int chapNum = MathUtils.parseInt(request.getParameter("chapNum"));
	int contentNum = MathUtils.parseInt(request.getParameter("contentNum"));
	final int contentType = MathUtils.parseInt(request.getParameter("contentType"));
	final int origType = MathUtils.parseInt(request.getParameter("origContentType"));
	final String caption = Utils.inputToCERs(request.getParameter("caption"));
	final boolean cancelling = "true".equals(request.getParameter("cancelling"));
	final boolean brandNew = "true".equals(request.getParameter("brandNew"));
	final TextChapter chapter = book.getChapter(chapNum);
	final TextContent content = chapter.getContent(contentNum);
	/* Utils.alwaysPrint("saveContent.jsp: contentNum = ", contentNum, 
			", contentType = ", contentType, ", origType = ", origType,
			", contentId = ", content.getId(), ", caption = ", caption); /**/ 
	String failure = null;
	if (cancelling && brandNew && content.getId() == 0) {
		chapter.deleteContent(contentNum);
		final int numContents = chapter.getNumContents();
		if (contentNum > numContents) contentNum = numContents;
	} else if (!cancelling) {
		content.setContentType(contentType);
		// posting converts \n to \r\n; convert it back
		if (content.isText()) {
			final String contentData = 
					Utils.inputToCERs(request.getParameter("contentData"));
			content.setContent(contentData.replaceAll("\r\n", "\n"), caption); 
		} else if (content.isImageURL()) {
			content.setContent(request.getParameter("contentData"), caption); 
		} else if (content.isACEQuestion()) {
			String data = request.getParameter("contentData");
			final boolean startHash = data.charAt(0) == '#';
			if ((startHash && MathUtils.parseInt(data.substring(1)) != 0) 
					|| (!startHash && MathUtils.parseInt(data) != 0)) {
				if (startHash) {
					data = data.substring(1);
					final int qId = MathUtils.parseInt(data);
					final InstructorSession instrSess = (InstructorSession) userSess;
					if (!instrSess.canReadQuestion(qId)) {
						failure = "You don't have access to the selected question.";
					} // if Q can't be read
				} // if qId written into textbox
				if (failure == null) content.setContent(data, caption);
			} else { // cancel after choosing inaccessible qId
				if (brandNew) chapter.deleteContent(contentNum);
				else content.setContentType(origType);
			} // if data is empty
		} else if (content.isImage() || content.isMovie()) {
			content.setContent(request.getParameter("srcFile"), caption);
		} else { // isMarvin() or isLewis() or isJmol()
			String contentData = Utils.inputToCERs(request.getParameter("contentData"));
			String addlData = request.getParameter("contentExtra");
			if (content.isMarvin()) { 
				contentData = ChemUtils.setFromMarvinJS(contentData);
			}
			if (content.isJmol()) {
				String addlData1 = Utils.inputToCERs(request.getParameter("addlData1"));
				String addlData2 = Utils.inputToCERs(request.getParameter("addlData2"));
				if (addlData1 == null) addlData1 = "";
				if (addlData2 == null) addlData2 = "";
				addlData = (!Utils.among("", addlData1, addlData2)
						? addlData1 + TextContent.JMOL_SEP + addlData2 : "");
			} else if (addlData == null) addlData = "";
			content.setContent(contentData.replaceAll("\r\n", "\n"), 
					caption, addlData.replaceAll("\r\n", "\n")); 
		} // if content type
		/* Utils.alwaysPrint("saveContent.jsp: chapNum = ", chapNum, 
				", contentNum = ", contentNum,
				", contentType = ", contentType,
				", contentExtra = ", content.getExtraData(),
				", contentData =\n", content.getContent()); /**/
	} // if contentType
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
			<% if (failure != null) { %>
				alert('<%= Utils.toValidJS(failure) %>');
				self.location.href = 'loadContent.jsp?chapNum=<%= 
						chapNum %>&contentNum=<%= 
						contentNum %>&origContentType=<%= origType %>';
			<% } else if (cancelling) { %>
				self.close();
			<% } else { %>
				opener.document.chapterForm.submit();
				self.close();
			<% } // if failure occurred %>
		}
		// -->
	</script>
</head>
<body onload="finish();">
</body>
</html>
