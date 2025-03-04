<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.chem.MolString,
	com.epoch.courseware.ForumPost,
	com.epoch.courseware.ForumTopic,
	com.epoch.lewis.LewisMolecule,
	com.epoch.qBank.Question,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.HashMap,
	java.util.List,
	java.util.Map,
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

	String regExp = Utils.inputToCERs(request.getParameter("regExp"));
	if (regExp == null) regExp = "";
	final boolean haveRegExp = !Utils.isEmpty(regExp);
	final int AND = 1;
	final int OR = 2;
	final List<Pattern> patterns = new ArrayList<Pattern>();
	final StudentSession studSess = (StudentSession) userSess;
	List<ForumTopic> topics = new ArrayList<ForumTopic>();
	Map<ForumTopic, ArrayList<ForumPost>> postsByTopics = 
			new HashMap<ForumTopic, ArrayList<ForumPost>>();
	if (haveRegExp) {
		regExp = regExp.trim();
		String[] regExps = regExp.split(" and ");
		if (regExps.length == 1) regExps = regExp.split(" or ");
		Utils.alwaysPrint("searchPosts.jsp: regExps = ", regExps);
		for (final String oneRegExp : regExps) {
			if (!Utils.isEmpty(oneRegExp)) {
				patterns.add(Pattern.compile(oneRegExp.trim()));
			} // if fragment is not empty
		} // for each fragment
		postsByTopics = studSess.getForumPosts(regExp);
		if (!postsByTopics.isEmpty()) {
			topics = new ArrayList<ForumTopic>(postsByTopics.keySet());
		} // if there are posts by topics
	} // if there's a regular expression
	final boolean isInstructorOrTA = role != User.STUDENT || studSess.isTA();

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

	function openTopic(topicId) {
		var bld = new String.builder().
				append('posts.jsp?posts50=1&topicId=').append(topicId);
		<% if (haveRegExp) { %>
			bld.append('&regExp=<%= Utils.toValidURI(regExp) %>');
		<% } // if there's a regular expression %>
		var go = bld.toString();
		self.location.href = go;
	} // openTopic()

	function cancel() {
		self.location.href = 'topics.jsp';
	} // cancel()

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
		<% int allPostsNum = 0;
		for (final ForumTopic topic : topics) {
			final ArrayList<ForumPost> posts = postsByTopics.get(topic);
			for (int postNum = 1; postNum <= posts.size(); postNum++) {
				allPostsNum++; %>
				if (cellExists('figData<%= allPostsNum %>')) {
					figuresData.push([
							getValue('figData<%= allPostsNum %>'),
							0,
							'<%= allPostsNum %>']);
				} // if figure is Lewis
		<% 	} // for each post in the topic 
		} // for each topic %>
		if (!isEmpty(figuresData)) {
			loadLewisInlineImages('<%= pathToRoot %>', figuresData);
		}
	} // initLewis()

	// -->
</script>
</head>
<body style="text-align:center; margin:0px; overflow:auto;" 
		onload="setTab('<%= toTabName(user.translateJS("Forum")) %>');"> 

<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

<div id="contentsWithTabsWithFooter">

<table summary="title" style="padding-top:10px; padding-left:10px;">
<tr>
<td class="boldtext big" style="padding-right:10px;">
	<%= user.translate("Forum search") %>
</td>
</tr>
</table>

<table class="whiteTable" style="width:90%; margin-left:auto; margin-right:auto;
		border-style:none; border-collapse:collapse; text-align:left;" 
		summary="searchEntryForm">
	<tr>
	<td style="padding-bottom:10px;">
		<form name="searchForm" action="searchPosts.jsp" method="post" accept-charset="UTF-8">
		<table summary="searchEntryForm">
		<tr>
		<td>
			<%= user.translate("Enter a word fragment, word, phrase, "
					+ "simple boolean expression, or regular expression:") %>
			<input type="text" name="regExp" size="80" 
					value="<%= Utils.toValidTextbox(regExp) %>"/>
		</td>
		<td>
			<%= makeButton(user.translate("Search"), "document.searchForm.submit();") %> 
		</td>
		</tr>
		<tr>
		<td class="regtext">
			<%= user.translate("If you are not entering a regular expression, "
					+ "and your search term contains any of the characters "
					+ "***metacharacters***, you must precede each instance of the "
					+ "character with a backslash ***\\***.", 
					new String[] { 
						Utils.toString("<span class=\"boldtext\" style=\"font-family:Courier;\">", 
							Utils.spanString("\\^$.|?*+[()"), "</span>"),
						Utils.toString("<span class=\"boldtext\" style=\"font-family:Courier;\">", 
							Utils.spanString("\\"), "</span>")
					}) %>
		</td>
		</tr>
		</table>
		</form>
	</td>
	</tr>

<% if (haveRegExp) { 
	if (!postsByTopics.isEmpty()) {
		int mviewNum = 0; %>
		<tr>
		<td style="padding-top:10px; padding-bottom:10px;">
			<span class="boldtext big">
			<%= user.translate("Found posts:") %>
			</span>
		</td>
		</tr>
		<tr>
		<td style="padding-bottom:10px;">
			<table class="whiteTable" style="width:100%; border-collapse:collapse;
					 text-align:left;" summary="posts">
			<% boolean parity = false;
			allPostsNum = 0;
			for (final ForumTopic topic : topics) {
				final int topicId = topic.getId();
				final ArrayList<ForumPost> posts = postsByTopics.get(topic);
				for (int postNum = 1; postNum <= posts.size(); postNum++) {
					final ForumPost post = posts.get(postNum - 1);
					allPostsNum++;
					parity = !parity;
					final String rowColor = (parity ? "whiterow" : "greenrow"); 
					final String dateCreated = post.getDateCreated(course.getTimeZone());
					final String dateLastEdited = 
							post.getDateLastEdited(course.getTimeZone());
					final boolean edited = !dateCreated.equals(dateLastEdited);
					String text = post.getText();
					if (haveRegExp) for (final Pattern pattern : patterns) {
						final Matcher matcher = pattern.matcher(text);
						final StringBuilder bld = new StringBuilder();
						int prevEnd = 0;
						while (true) {
							if (!matcher.find()) break;
							final int start = matcher.start();
							final int end = matcher.end();
							Utils.appendTo(bld, Utils.toDisplay(
										text.substring(prevEnd, start)), 
									"<span class=\"boldtext\" "
										+ "style=\"color:red;\">", 
									Utils.toDisplay(text.substring(start, end)), 
									"</span>");
							prevEnd = end;
						} // until pattern not found
						if (prevEnd < text.length()) {
							bld.append(Utils.toDisplay(text.substring(prevEnd)));
						} // if last pattern was not at end of string
						text = bld.toString();
					} // if have a regular expression to highlight
					final String rowStyle = (postNum < posts.size() ? ""
							: " style=\"border-bottom-style:solid; "
								+ "border-bottom-width:1px;\"");
					final boolean makeAnon = 
							post.isAnon() && !post.getAuthorId().equals(user.getUserId());
				%>
					<tr class="<%= rowColor %>" <%= rowStyle %>>
					<td class="regtext" style="padding-left:10px; padding-top:10px; 
							padding-right:10px;">
						<a name="topic<%= topicId %>"
								href="javascript:openTopic(<%= topicId %>)"><%= 
							Utils.toDisplay(topic.getTitle()) %></a>
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
					<td class="regtext" style="padding-left:10px; padding-top:10px; 
							padding-right:10px; text-align:center;">
						<% if (post.hasFigure()) { 
							final String postFigIdStr = Utils.toString(
									"fig", allPostsNum); %>
							<table class="whiteTable">
							<tr><td id="<%= postFigIdStr %>">
								<% if (post.figureIsImage()) { 
									final String figure = post.getFigure(); %>
									<script type="text/javascript">
									// <!-- >
									setImage('<%= postFigIdStr %>',
											'<%= Utils.isEmpty(figure) ? ""
												: Utils.toString(pathToRoot, figure) %>');
									// -->
									</script>
								<% } else if (post.figureIsLewis()) { %>
									<input type="hidden" id="figData<%= allPostsNum %>"
											value="<%= Utils.toValidHTMLAttributeValue(
												post.getFigure()) %>" />
								<% } else { %>
									<%= post.getImage(pathToRoot, user.prefersPNG(),
											postFigIdStr) %>
								<% } // if figureType %>
							</td></tr>
							</table>
						<% } // if post has figure %>
					</td>
					</tr>
				<% } // for each post in topic
			} // for each topic %>
			</table>
		</td>
		</tr>
		<% } else { %>
		<tr>
		<td style="padding-top:10px; padding-bottom:10px;">
			<span class="boldtext big">
			<%= user.translate("No posts found.") %>
			</span>
		</td>
		</tr>
		<% } // if have search results
	} // if have a regular expression to search for %>
</table>

</form>
</div>
<div id="footer">
<table>
	<tr><td class="regtext" style="padding-left:40px; padding-top:10px;">
		<table style="text-align:center;">
			<tr>
			<td>
				<%= makeButton(user.translate("Save"), "submitIt(true);") %> 
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
