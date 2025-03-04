<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
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
	final StudentSession studSess = (StudentSession) userSess;
	final boolean isInstructorOrTA = role != User.STUDENT || studSess.isTA();
	if (isInstructorOrTA) {
		final int deleteTopicId =
				MathUtils.parseInt(request.getParameter("deleteTopicId"));
		if (deleteTopicId > 0) {
			final InstructorSession instrSess =
					(role != User.STUDENT ? (InstructorSession) userSess
					: new InstructorSession(course.getId(), user));
			if (deleteTopicId > 0) {
				instrSess.deleteForumTopic(deleteTopicId);
			} // if delete topic
		} // if there's a topic to delete
	} else {
		final String setWatchedTo = request.getParameter("setWatchedTo");
		if (setWatchedTo != null) {
			final int topicId =
					MathUtils.parseInt(request.getParameter("topicId"));
			studSess.setWatched(topicId, "true".equals(setWatchedTo));
		} // if there is a topic to set the watched state to
	} // if instructor or TA
	final int numTopics = studSess.getNumForumTopics();
	final ForumTopic[] topics = studSess.getForumTopics(topics50);
	if (!isInstructorOrTA) {
		studSess.getWatched(topics);
	} // get watchedness of topics

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
<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/navigation/courseSidebarJS.jsp.h" %>

	function openTopic(topicId) {
		var go = 'posts.jsp?topics50=<%= topics50 %>'
				+ '&posts50=1&topicId=' + topicId;
		self.location.href = go;
	} // openTopic()

	function moreTopics(increment) {
		var newTopics50 = <%= topics50 %> + increment;
		var go = 'topics.jsp?topics50=' +  newTopics50;
		self.location.href = go;
	} // moreTopics()

	function addTopic() {
		var go = 'addPost.jsp?topics50=<%= topics50 %>';
		self.location.href = go;
	} // addTopic()

	function searchPosts() {
		self.location.href = 'searchPosts.jsp';
	} // searchPosts()

	<% if (isInstructorOrTA) { %>
		function editTopic(topicId) {
			var go = 'editTopic.jsp?topics50=<%= topics50 %>&topicId=' + topicId;
			self.location.href = go;
		} // editTopic()

		function deleteTopic(topicId) {
			if (toConfirm('<%= user.translateJS(
					"Deleting this forum topic is an irreversible act.  Are " 
					+ "you sure you wish to continue?") %>')) {
				var go = 'topics.jsp?deleteTopicId=' + topicId;
				self.location.href = go;
			} // if delete is confirmed
		} // deleteTopic()
	<% } else { %>
		function setWatched(topicId) {
			var watchCheckbox = document.getElementById('watch' + topicId);
			var newWatchedness = watchCheckbox.checked;
			self.location.href = new String.builder().
					append('topics.jsp?topics50=<%= topics50 %>&topicId=').
					append(topicId).append('&setWatchedTo=').
					append(newWatchedness).toString();
		} // setWatched()
	<% } // if isInstructorOrTA %>

	function openHW(hwNum, qId) {
		var bld = new String.builder().
				append('<%= pathToRoot %>homework/hwmain.jsp?hwNum=').
				append(hwNum).append('&goToQId=').append(qId);
		self.location.href = bld.toString();
	} // openHW()

	// -->
</script>
</head>
<body style="text-align:center; margin:0px; overflow:auto;" 
		onload="setTab('<%= toTabName(user.translateJS("Forum")) %>');"> 

	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>
	<div id="contentsWithTabsWithFooter">

	<table summary="title" style="width:90%; padding-top:10px; padding-left:10px;
			margin-left:auto; margin-right:auto;">
		<tr>
		<td class="boldtext big" style="width:30%; padding-right:10px; 
				padding-bottom:10px;">
			<%= user.translate("Course Forum") %>
		</td>
		<% if (isInstructorOrTA) { %>
			<td class="regtext" style="padding-right:10px; padding-bottom:10px;
					text-align:right; width:70%;">
				<%= user.translate("Use the ***Enrollment*** tab to block a "
						+ "student from this forum.", 
						"<a href=\"javascript:openEnrollment();\">"
							+ user.translate("Enrollment") + "</a>") %>
			</td>
		<% } // if isInstructorOrTA %>
		</tr>
	</table>

	<table class="whiteTable" style="width:90%; border-collapse:collapse;
			 text-align:left;" summary="topics">
		<% if (Utils.isEmpty(topics)) { %>
			<%= user.translate("This forum has no topics.") %>
		<% } else { %>
			<tr style="border-bottom-style:solid; border-bottom-width:1px;">
			<th class="boldtext" style="padding-left:10px; padding-top:5px; 
					padding-right:10px; padding-bottom:5px; text-align:right;">
				<%= user.translate("No.") %>
			</th>
			<th class="boldtext" style="padding-left:10px; padding-top:5px; 
					padding-right:10px; padding-bottom:5px;">
				<%= user.translate("Topic") %>
			</th>
			<th class="boldtext" style="padding-left:10px; padding-top:5px; 
					padding-right:10px; padding-bottom:5px; text-align:center;">
				<%= user.translate("Number of posts") %>
			</th>
			<th class="boldtext" style="padding-left:10px; padding-top:5px; 
					padding-right:10px; padding-bottom:5px;">
				<%= user.translate("Created by") %>
			</th>
			<th class="boldtext" style="padding-left:10px; padding-top:5px; 
					padding-right:10px; padding-bottom:5px;">
				<%= user.translate("Last modified by") %>
			</th>
			<th class="boldtext" style="padding-left:10px; padding-top:5px; 
					padding-right:10px; padding-bottom:5px; text-align:center;">
				<%= user.translate("Linked to") %>
			</th>
			<th class="boldtext" style="padding-left:10px; padding-top:5px; 
					padding-right:10px; padding-bottom:5px; text-align:center;">
			<% if (isInstructorOrTA) { %>
				&nbsp;
			<% } else { %>
				<%= user.translate("Watched?") %>
			<% } // if isInstructorOrTA %>
			</th>
			</tr>
			<% int numSticky = 0;
			for (int topNum = 1; topNum <= topics.length; topNum++) {
				final ForumTopic topic = topics[topNum - 1];
				if (topic.isSticky()) numSticky++;
				final int topicId = topic.getId();
				final String lastUserName = topic.getLastUserName();
				final String rowColor = (topNum % 2 == 0 ? "whiterow" : "greenrow"); 
				final boolean makeAnon = topic.isCreatorAnon(); 
				final boolean isWatched = topic.isWatched(); %>
				<tr class="<%= rowColor %>">
				<td class="regtext" style="padding-left:10px; padding-top:10px; 
						padding-right:10px; text-align:right;">
					<%= topic.isSticky() ? topNum 
							: (topics50 - 1) * 50 + topNum %>.
				</td>
				<td class="regtext" style="padding-left:10px; padding-top:10px; 
						padding-right:10px;">
					<% if (topic.isSticky()) { %>
						<span class="boldtext"><%= user.translate("Sticky") %>:</span>
					<% } // if topic is sticky %>
					<a name="topic<%= topicId %>"
							href="javascript:openTopic(<%= topicId %>)"><%= 
						Utils.toDisplay(topic.getTitle()) %></a>
				</td>
				<td class="regtext" style="padding-left:10px; padding-top:10px; 
						padding-right:10px; text-align:center;">
					<%= topic.getNumPosts() %>
				</td>
				<td class="regtext" style="padding-left:10px; padding-top:10px; 
						padding-right:10px;">
					<%= makeAnon && isInstructorOrTA 
							? Utils.getBuilder(user.translate("Anonymous"), 
								" <!-- ", topic.getCreatorName(), " -->")
							: makeAnon ? user.translate("Anonymous")
							: topic.getCreatorName() %>
					<br />
					<%= topic.getDateCreated(course.getTimeZone()) %>
				</td>
				<td class="regtext" style="padding-left:10px; padding-top:10px; 
						padding-right:10px;">
					<%= "?".equals(lastUserName) ? user.translate("Anonymous")
							: lastUserName %>
					<br />
					<%= topic.getDateLastChanged(course.getTimeZone()) %>
				</td>
				<td class="regtext" style="padding-left:10px; 
						padding-top:10px; padding-right:10px;
						text-align:center;">
					<% final int[] linkedAssgtQIds = topic.getLinkedAssgtQ();
					if (!Utils.isEmpty(linkedAssgtQIds) 
							&& Utils.indexOf(linkedAssgtQIds, 0) < 0) {
						int hwNum = 0;
						int[] qNumArr = new int[0];
						if (isInstructorOrTA) {
							final InstructorSession instrSess =
									(role != User.STUDENT ? (InstructorSession) userSess
									: new InstructorSession(course.getId(), user));
							final int[][] assgtAndQNums = 
									instrSess.getAssgtAndQNums(linkedAssgtQIds);
							hwNum = assgtAndQNums[0][0];
							qNumArr = assgtAndQNums[1];
						} else {
							final int[] assgtAndQNums = 
									studSess.getAssgtAndQNum(linkedAssgtQIds);
							hwNum = assgtAndQNums[0];
							qNumArr = new int[] {assgtAndQNums[1]};
						} // if instructor or TA
						if (hwNum > 0 && !Utils.isEmpty(qNumArr)) { 
							final String hwName = 
									Utils.toDisplay(assgts[hwNum - 1].getName());
							final String qNumStr = (qNumArr.length == 1
									? user.translate("question ***1***", qNumArr[0])
									: user.translate("one of questions "
										+ "***1***&ndash;***5***",
										new int[] {qNumArr[0], qNumArr[1]})); %>
							<i><%= hwName %></i>,
							<br/><a href="javascript:openHW(<%= hwNum %>, 
									<%= linkedAssgtQIds[1] %>);"><%= qNumStr %></a>
						<% } // if there are assigned questions
					} // if there's a linked assignment and question %>
				</td>
				<td class="regtext" style="padding-left:10px; padding-top:10px; 
						padding-right:10px; text-align:center;">
				<% if (isInstructorOrTA) { %>
					<table style="margin-left:auto; margin:right:5px;"><tr><td>
					<%= makeButtonIcon("edit", pathToRoot, 
							"editTopic(", topicId, ");") %>
					</td><td>
					<%= makeButtonIcon("delete", pathToRoot,
							"deleteTopic(", topicId, ");") %>
					</td></tr></table>
				<% } else { %>
					<input type="checkbox" id="watch<%= topicId %>" 
							onchange="setWatched(<%= topicId %>, <%= isWatched %>);" <%=
							isWatched ? "checked=\"checked\"" : "" %>>
				<% } // if isInstructorOrTA %>
				</td>
				</tr>
			<% } // for each topic in forum %>
		<% } // if there are topics %>
	</table>
</div>
<div id="footer">
<table>
	<tr><td class="regtext" style="padding-left:40px; padding-top:10px;">
		<table style="text-align:center;">
			<tr>
			<td>
				<% if (topics50 > 1) { %>
					<%= makeButtonIcon("back", pathToRoot, "moreTopics(-1);") %>
				<% } else { %>
					&nbsp;&nbsp;&nbsp;&nbsp;
				<% } // if there are previous topics %>
			</td>
			<td>
				<%= makeButton(user.translate("New topic"), "addTopic();") %> 
			</td>
			<td>
				<% if (numTopics > topics50 * 50) { %>
					<%= makeButtonIcon("next", pathToRoot, "moreTopics(1);") %>
				<% } else { %>
					&nbsp;&nbsp;&nbsp;&nbsp;
				<% } // if there are more topics %>
			</td>
			<td>
				<%= makeButton(user.translate("Search posts"), "searchPosts();") %> 
			</td>
			</tr>
		</table>
	</td></tr>
</table>

</div>
</body>
</html>
