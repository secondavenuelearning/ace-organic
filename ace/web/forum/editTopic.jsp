<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.ForumTopic,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>

<%
	response.setHeader("Cache-Control","no-store, must-revalidate"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final int topics50 = MathUtils.parseInt(request.getParameter("topics50"), 1);
	final int topicId = MathUtils.parseInt(request.getParameter("topicId"));

	final StudentSession studSess = (StudentSession) userSess;
	final ForumTopic topic = studSess.getForumTopic(topicId);
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<title>ACE Course Forum</title>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<style type="text/css">
	* html body {
		padding:100px 0 55px 0; 
	}
</style>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/navigation/courseSidebarJS.jsp.h" %>

	function submitIt() {
		var form = document.forumTopicForm;
		if (isWhiteSpace(form.title.value)) {
			toAlert('<%= user.translateJS("Please enter a title for "
					+ "this forum topic.") %>');
			return;
		} // if no title
		form.submit();
	} // submitIt()

	function cancel() {
		self.location.href = 'topics.jsp?topics50=<%= topics50 %>';
	} // cancel()

	// -->
</script>
</head>
<body style="text-align:center; margin:0px; overflow:auto;" 
		onload="setTab('<%= toTabName(user.translateJS("Forum")) %>');"> 

	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>
<div id="contentsWithTabsWithFooter">

<form name="forumTopicForm" action="saveTopic.jsp" method="post" accept-charset="UTF-8">
	<input name="topics50" type="hidden" value="<%= topics50 %>" />
	<input name="topicId" type="hidden" value="<%= topicId %>" />
<table class="whiteTable" style="width:90%; margin-left:auto; margin-right:auto;
		border-style:none; border-collapse:collapse; text-align:left;" 
		summary="topicEntryForm">
	<tr>
	<td class="boldtext big" style="padding-top:10px; padding-bottom:10px;">
		<%= user.translate("Edit topic") %>
	</td>
	</tr>
	<tr>
	<td style="padding-bottom:10px;">
		<table style="width:100%;">
		<tr>
		<td>
			<%= user.translate("Topic title:") %>
				<input type="text" name="title" size="80" 
						value="<%= Utils.toValidTextbox(topic.getTitle()) %>"/>
		</td>
		<td style="text-align:right;">
			<%= user.translate("Sticky") %>
			<input type="checkbox" name="sticky" <%=
					topic.isSticky() ? "checked=\"checked\"" : "" %>>
		</td>
		</tr>
		</table>
	</td>
	</tr>
</table>

</form>
</div>
<div id="footer">
<table>
	<tr><td class="regtext" style="padding-left:40px; padding-top:10px;">
		<table style="text-align:center;">
			<tr>
			<td>
				<%= makeButton(user.translate("Save"), "submitIt();") %> 
			</td>
			<td>
				<%= makeButton(user.translate("Cancel"), "cancel();") %> 
			</td>
			</tr>
		</table>
	</td></tr>
</table>

</div>
</body>
</html>
