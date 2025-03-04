<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.chem.ChemUtils,
	com.epoch.courseware.ForumPost,
	com.epoch.courseware.ForumTopic,
	com.epoch.synthesis.Synthesis,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.List"
%>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-store, must-revalidate"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final int topics50 = MathUtils.parseInt(request.getParameter("topics50"), 1);
	final int posts50 = MathUtils.parseInt(request.getParameter("posts50"), 1);

	int topicId = MathUtils.parseInt(request.getParameter("topicId"));
	final StudentSession studSess = (StudentSession) userSess;
	final boolean topicIsNew = topicId == 0;
	ForumTopic topic;
	if (topicIsNew) {
		final String title = Utils.inputToCERs(request.getParameter("title"));
		final int[] linkedAssgtQ = new int[] {
				MathUtils.parseInt(request.getParameter("linkedHWId")),
				MathUtils.parseInt(request.getParameter("linkedQId"))};
		final boolean isSticky = "on".equals(request.getParameter("sticky"));
		topic = new ForumTopic(course.getId(), user.getUserId(), title, 
				isSticky, linkedAssgtQ);
		if (isInstructor) {
			final InstructorSession instrSess = (InstructorSession) userSess;
			instrSess.addForumTopic(topic); // alerts all students in course
		} else {
			studSess.addForumTopic(topic);
		} // if instructor
		topicId = topic.getId();
	} else {
		topic = studSess.getForumTopic(topicId);
	} // if topic is new

	int postId = MathUtils.parseInt(request.getParameter("postId"));
	final String text = Utils.inputToCERs(request.getParameter("text"));
	final int figType = MathUtils.parseInt(request.getParameter("figType"));
	String figure = Utils.inputToCERs(request.getParameter("figure"));
	// Utils.alwaysPrint("savePost.jsp: original MarvinJS figure: ", figure);
	if (figure != null) figure = figure.replaceAll("\r\n", "\n"); 
	// Utils.alwaysPrint("savePost.jsp: after modifying return characters: ", figure);
	if (figType == ForumPost.MOLECULE || figType == ForumPost.SYNTHESIS) {
		if (figType == ForumPost.SYNTHESIS) {
			final String pastedSyn = request.getParameter("pasteSynthesis");
			if (Utils.isEmpty(pastedSyn)) {
				final String rxnIdsStr = request.getParameter("rxnIds");
				figure = ChemUtils.setProperty(figure, Synthesis.RXN_IDS, rxnIdsStr);
			} else {
				figure = pastedSyn;
			} // if there is a pasted synthesis
		} // if synthesis
		figure = ChemUtils.setFromMarvinJS(figure);
		// Utils.alwaysPrint("savePost.jsp: after adding molecule property(ies): ", figure);
	} // if MOLECULE or SYNTHESIS
	final int flags = ("on".equals(request.getParameter("anonymous")) 
			? ForumPost.ANON : 0);
	// Utils.alwaysPrint("savePost.jsp: figure:\n", figure);
	if (postId != 0) {
		final ForumPost post = studSess.getForumPost(postId);
		if (post != null) {
			post.setText(text);
			if (figure != null) post.setFigure(figure); 
			post.setFigureType(figType);
			post.setFlags(flags);
			switch (role) {
				case User.ADMINISTRATOR:
				case User.INSTRUCTOR:
					((InstructorSession) userSess).editForumPost(post);
					break;
				case User.STUDENT:
				default: // shouldn't happen
					if (studSess.isTA()) {
						final InstructorSession instrSess = 
								new InstructorSession(course.getId(), user);
						instrSess.editForumPost(post);
					} else studSess.editForumPost(post);
					break;
			} // switch
		} else postId = 0;
	} // if the post already exists
	if (postId == 0) { // or couldn't find it in database
		final ForumPost post = 
				new ForumPost(topicId, user.getUserId(), text, figure, figType, flags);
		studSess.addForumPost(post);
	} // if post is new
	final String modTopicTitle = 
			Utils.toValidJS(topic.getTitle()).replaceAll("&", "and");

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
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
		// <!-- >
		function backToPosts() {
			this.location.href = new String.builder().
					append('posts.jsp?topicId=<%= topicId %>').
					append('&topics50=<%= topics50 %>').
					append('&posts50=<%= posts50 %>').
					toString();
		} // backToPosts()
		// -->
	</script>
</head>
<body class="regtext" style="width:90%;" onload="backToPosts();">
</body>
</html>
