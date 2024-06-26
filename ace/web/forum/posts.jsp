<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.chem.MolString,
	com.epoch.courseware.ForumPost,
	com.epoch.courseware.ForumTopic,
	com.epoch.lewis.LewisMolecule,
	com.epoch.qBank.Question,
	com.epoch.exceptions.ParameterException,
	com.epoch.synthesis.Synthesis,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.regex.Matcher,
	java.util.regex.Pattern"
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
	final int posts50 = MathUtils.parseInt(request.getParameter("posts50"), 1);
	final int topicId = MathUtils.parseInt(request.getParameter("topicId"));
	final String regExp = request.getParameter("regExp");
	final boolean haveRegExp = !Utils.isEmpty(regExp); 
	final Pattern pattern = (haveRegExp ? Pattern.compile(regExp) : null);

	final StudentSession studSess = (StudentSession) userSess;
	final boolean isInstructorOrTA = role != User.STUDENT || studSess.isTA();
	final int deletePostId =
			MathUtils.parseInt(request.getParameter("deletePostId"));
	boolean deleteFailed = false;
	if (deletePostId > 0) {
		try {
			if (isInstructorOrTA) {
				final InstructorSession instrSess =
						(role != User.STUDENT ? (InstructorSession) userSess
						: new InstructorSession(course.getId(), user));
				instrSess.deleteForumPost(deletePostId);
			} else studSess.deleteForumPost(deletePostId);
		} catch (ParameterException e) {
			deleteFailed = true;
		}
	} // if there's a post to delete
	final ForumTopic topic = studSess.getForumTopic(topicId);
	final ForumPost[] posts = studSess.getForumPosts(topicId, posts50);
	/* Utils.alwaysPrint("posts.jsp: topicId = ", topicId,
			", posts50 = ", posts50,
			", posts size = ", Utils.getLength(posts)); */

	int mviewNum = 0;
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
<script src="<%= pathToRoot %>js/lewisJS.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= 
		AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/navigation/courseSidebarJS.jsp.h" %>
	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>

	function goToTopics() {
		self.location.href = 'topics.jsp?topics50=<%= topics50 %>';
	} // goToTopics()

	function morePosts(increment) {
		var newPosts50 = <%= posts50 %> + increment;
		var bld = new String.builder().
				append('posts.jsp?topicId=<%= topicId %>'
					+ '&topics50=<%= topics50 %>'
					+ 'posts50=').append(newPosts50);
		<% if (haveRegExp) { %>
			bld.append('&regExp=<%= Utils.toValidURI(regExp) %>');
		<% } // if there's a regular expression %>
		var go = bld.toString();
		self.location.href = go;
	} // morePosts()

	function addPost() {
		editPost(0);
	} // addPost()

	function editPost(postId) {
		var go = 'addPost.jsp?topicId=<%= topicId %>'
				+ '&topics50=<%= topics50 %>'
				+ '&posts50=<%= posts50 %>';
		if (postId > 0) go += '&postId=' + postId;
		self.location.href = go;
	} // editPost()

	function deletePost(postId) {
		if (toConfirm('<%= user.translateJS(
				"Deleting this forum post is an irreversible act.  Are " 
				+ "you sure you wish to continue?") %>')) {
			var go = 'posts.jsp?deletePostId=' + postId
					+ '&topicId=<%= topicId %>'
					+ '&topics50=<%= topics50 %>'
					+ '&posts50=<%= posts50 %>';
			self.location.href = go;
		} // if delete is confirmed
	} // deletePost()

	function openHW(hwNum, qId) {
		var bld = new String.builder().
				append('<%= pathToRoot %>homework/hwmain.jsp?hwNum=').
				append(hwNum).append('&goToQId=').append(qId);
		self.location.href = bld.toString();
	} // openHW()

	function searchPosts() {
		self.location.href = 'searchPosts.jsp';
	} // searchPosts()

	function setImage(cell, href) {
		var bld = new String.builder();
		if (!isEmpty(href)) {
			bld.append('<table><tr><td id="enlarge').append(cell).
					append('" class="boldtext" '
						+ 'style="width:100%; text-align:right;">'
						+ '<%= user.translateJS("Click image to enlarge") %>'
						+ '</td></tr><tr><td>'
						+ '<a href="javascript:enlargeImage(\'').append(href).
					append('\')"><img src="').append(href).
					append('" alt="picture" style="visibility:hidden;" '
						+ 'onload="prepareImage(this, \'enlarge').append(cell).
					append('\');" onmouseover="this.style.cursor=\'pointer\'" /></a>'
						+ '</td></tr></table>');
			html = bld.toString();
		} else bld.append('<%= user.translateJS("No image available.") %>');
		setInnerHTML(cell, bld.toString());
	} // setImage()

	function initLewis() {
		initLewisConstants(
				[<%= LewisMolecule.CANVAS_WIDTH %>, 
					<%= LewisMolecule.CANVAS_HEIGHT %>],
				<%= LewisMolecule.MARVIN_WIDTH %>, 
				['<%= LewisMolecule.PAIRED_ELECS %>',
					'<%= LewisMolecule.UNPAIRED_ELECS %>',
					'<%= LewisMolecule.UNSHARED_ELECS %>' ],
				'<%= LewisMolecule.LEWIS_PROPERTY %>',
				'<%= LewisMolecule.HIGHLIGHT %>');
		var figuresData = [];
		<% for (int postNum = 1; postNum <= posts.length; postNum++) { %>
			if (cellExists('figData<%= postNum %>')) {
				figuresData.push([
						getValue('figData<%= postNum %>'),
						0,
						'<%= postNum %>']);
				} // if figure is Lewis
		<% } // for each post %>
		if (!isEmpty(figuresData)) {
			loadLewisInlineImages('<%= pathToRoot %>', figuresData, 'true');
		}
	} // initLewis()

	function showSource(postNum) {
		var bld = new String.builder().
				append('<%= pathToRoot %>includes/showSourceCode.jsp'
					+ '?sourceCodeNum=').append(postNum);
		openSourceCodeWindow(bld.toString());
	} // showSource()

	function deleteFailed() {
		toAlert('<%= user.translateJS("ACE could not delete the post because "
				+ "there are more recent posts in the same topic.") %>');
	} // deleteFailed()

	<% if (topic == null) { %>
		toAlert('<%= user.translateJS("No such topic.") %>');
		goToTopics();
	<% } // if there is no such topic %>

	<% for (int postNum = 1; postNum <= posts.length; postNum++) { 
		final ForumPost post = posts[postNum - 1];
	%>
		function getMolForMView_<%= postNum %>() {
			return '<%= Utils.toValidJS(post.getFigure()) %>';
		} // getMolForMView_<%= postNum %>()

		function launchMView_<%= postNum %>() {
			var url = new String.builder().
					append('<%= pathToRoot %>includes\/marvinJSViewer.jsp' +
						'?viewOpts=' + SHOWLONEPAIRS + '&getMolMethodName=').
					append(encodeURIComponent('getMolForMView_<%= postNum %>()')).
					toString();
			openSketcherWindow(url);
		} // launchMView_<%= postNum %>()
	<% } // for each post %>

	// --> end HTML comment
</script>
</head>
<body style="text-align:center; margin:0px; overflow:auto;" 
		onload="setTab('<%= toTabName(user.translateJS("Forum")) %>'); 
				initLewis(); <%= deleteFailed ? "deleteFailed();" : "" %>"> 

	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>
	<div id="contentsWithTabsWithFooter">

	<table style="width:90%; margin-left:auto; margin-right:auto; 
			margin-top:10px; margin-bottom:10px;
			border-collapse:collapse;" summary="title">
		<tr>
		<td class="boldtext big" style="padding-right:10px;">
			<%= user.translate("Forum topic") %>:
			<%= topic != null ? Utils.toDisplay(topic.getTitle()) : "No topic" %>
		</td>
		<% final int[] linkedAssgtQIds = (topic == null ? null : topic.getLinkedAssgtQ());
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
			// Utils.alwaysPrint("posts.jsp: hwNum = ", hwNum, ", qNumArr = ", qNumArr);
			if (hwNum > 0 && !Utils.isEmpty(qNumArr)) { 
				final String hwName = 
						Utils.toDisplay(assgts[hwNum - 1].getName());
				final String qNumStr = (qNumArr.length == 1
						? user.translate("question ***1***", qNumArr[0])
						: user.translate("one of questions "
							+ "***1***&ndash;***5***",
							new int[] {qNumArr[0], qNumArr[1]})); %>
				<td class="regtext" style="text-align:right;">
					<%= user.translate("Linked to") %> <i><%= hwName %></i>,
					<a href="javascript:openHW(<%= hwNum %>, <%= 
							linkedAssgtQIds[1] %>);"><%= qNumStr %></a>
				</td>
			<% } // if there are assigned questions
		} // if there's a linked assignment and question %>
		</tr>
	</table>

	<table class="whiteTable" style="width:90%; border-collapse:collapse;
			 text-align:left;" summary="posts">
		<% for (int postNum = 1; postNum <= posts.length; postNum++) {
			final ForumPost post = posts[postNum - 1];
			final String rowColor = (postNum % 2 == 0 ? "whiterow" : "greenrow"); 
			final String dateCreated = post.getDateCreated(course.getTimeZone());
			final String dateLastEdited = 
					post.getDateLastEdited(course.getTimeZone());
			final boolean edited = !dateCreated.equals(dateLastEdited);
			String text = post.getText();
			if (haveRegExp) {
				final Matcher matcher = pattern.matcher(text);
				final StringBuilder bld = new StringBuilder();
				int prevEnd = 0;
				while (true) {
					if (!matcher.find()) break;
					final int start = matcher.start();
					final int end = matcher.end();
					Utils.appendTo(bld, 
							Utils.toDisplay(text.substring(prevEnd, start)), 
							"<span class=\"boldtext\" style=\"color:red;\">", 
							Utils.toDisplay(text.substring(start, end)), 
							"</span>");
					prevEnd = end;
				} // until pattern not found
				if (prevEnd < text.length()) {
					bld.append(Utils.toDisplay(text.substring(prevEnd)));
				} // if last pattern was not at end of string
				text = bld.toString();
			} // if have a regular expression to highlight
			final boolean makeAnon = 
					post.isAnon() && !post.getAuthorId().equals(user.getUserId());
		%>
			<tr class="<%= rowColor %>">
			<td class="regtext" style="padding-left:10px; padding-top:10px; 
					padding-right:10px;">
				<a name="post<%= post.getId() %>"></a>
				(<b><%= postNum + ((posts50 - 1) * 50) %></b>)
			</td>
			<td class="regtext" style="padding-left:10px; padding-top:10px; 
					padding-right:10px;">
				<%= makeAnon && isInstructorOrTA 
						? Utils.getBuilder(user.translate("Anonymous"), 
							" <!-- ", post.getAuthorName(), " -->")
						: makeAnon ? user.translate("Anonymous")
						: post.getAuthorName() %>
				<br />
				<%= user.translate("First posted") %> <%= dateCreated %>
				<% if (edited) { %>
					<br />
					<%= user.translate("Last edited") %> <%= dateLastEdited %>
				<% } // if post has been edited %>
			</td>
			<td class="regtext" style="padding-left:10px; padding-top:10px; 
					padding-right:10px;">
				<%= Utils.toDisplay(text) %>
			</td>
			<td style="padding-left:10px; padding-top:10px; 
					padding-right:10px; text-align:center;">
			<% 
			if (post.hasFigure()) { %>
				<table>
				<% final String figure = post.getFigure(); 
				if (!post.figureIsImage() && isInstructorOrTA) { %>
					<tr>
					<td id="launchMViewCell<%= postNum %>" class="boldtext"
							style="text-align:right; padding-left:10px; font-style:italic;">
						<a onclick="launchMView_<%= postNum %>();">
							<u>Launch MarvinJS&trade; viewer</u></a>
						or click image to copy source
					</td></tr>
				<% } // if figure not an image %>
				<tr><td>
				<table class="whiteTable">
				<tr><td id="fig<%= postNum %>">
					<% if (post.figureIsImage()) { %>
						<script type="text/javascript">
						// <!-- >
						setImage('fig<%= postNum %>',
								'<%= Utils.isEmpty(figure) ? ""
									: Utils.toString(pathToRoot, figure) %>');
						// -->
						</script>
					<% } else {
						final StringBuilder bld = new StringBuilder();
						if (isInstructorOrTA) {
							synchronized (session) {
								session.setAttribute(Utils.toString(
										"sourceCode", postNum), figure);
							} // synchronized
							Utils.appendTo(bld, 
									"<a onclick=\"showSource(", postNum, ");\">");
						} // if instructor or TA
						if (post.figureIsLewis()) {
							Utils.appendTo(bld, 
									"<input type=\"hidden\" id=\"figData", 
									postNum, "\" value=\"", 
									Utils.toValidHTMLAttributeValue(figure), 
									"\" />");
						} else {
							Utils.appendTo(bld, 
									post.getImage(pathToRoot, user.prefersPNG(),
										postNum), 
									Synthesis.getRxnsDisplay(figure, user));
						} // if figureType
						if (isInstructorOrTA) bld.append("</a>"); %>
						<%= bld.toString() %>
					<% } // if figureType %>
				</td></tr>
				</table>
				</td></tr>
				</table>
			<% } // if post has figure %>
			</td>
			<td class="regtext" style="padding-left:10px; padding-top:10px; 
					padding-right:10px;">
				<% final String currentUserId = user.getUserId();
				if (isInstructorOrTA || currentUserId.equals(post.getAuthorId())) { %>
					<%= makeButtonIcon("edit", pathToRoot,
							"editPost(", post.getId(), ");") %>
					<%= makeButtonIcon("delete", pathToRoot,
							"deletePost(", post.getId(), ");") %>
				<% } // if has authority to edit or delete post %>
			</td>
			</tr>
		<% } // for each post in topic %>
	</table>
</div>
<div id="footer">
<table>
	<tr><td class="regtext" style="padding-left:40px; padding-top:10px;">
		<table style="text-align:center;">
			<tr>
			<td>
				<% if (posts50 > 1) { %>
					<%= makeButtonIcon("back", pathToRoot, "morePosts(-1);") %>
				<% } else { %>
					&nbsp;&nbsp;&nbsp;&nbsp;
				<% } // if there are previous posts %>
			</td>
			<td>
				<%= makeButton(user.translate("New post"), "addPost();") %> 
			</td>
			<td>
				<% if (posts.length >= 50) { %>
					<%= makeButtonIcon("next", pathToRoot, "morePosts(1);") %>
				<% } else { %>
					&nbsp;&nbsp;&nbsp;&nbsp;
				<% } // if there are more posts %>
			</td>
			<td>
				<%= makeButton(user.translate("Back to topics"), "goToTopics();") %> 
			</td>
			<td>
				<%= makeButton(user.translate("Search posts"), "searchPosts();") %> 
			</tr>
		</table>
	</td></tr>
</table>

</div>
</body>
</html>
