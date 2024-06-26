<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.chem.MolString,
	com.epoch.courseware.ForumPost,
	com.epoch.courseware.ForumTopic,
	com.epoch.lewis.LewisMolecule,
	com.epoch.qBank.Question,
	com.epoch.synthesis.RxnCondition,
	com.epoch.synthesis.Synthesis,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.Map"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%@ include file="/js/rxnCondsJava.jsp.h" %>

<%
	response.setHeader("Cache-Control","no-store, must-revalidate"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String pathToChooseRxnCondsUser = "../homework/";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final int topics50 = MathUtils.parseInt(request.getParameter("topics50"), 1);
	final int posts50 = MathUtils.parseInt(request.getParameter("posts50"), 1);
	final int topicId = MathUtils.parseInt(request.getParameter("topicId"));
	final int postId = MathUtils.parseInt(request.getParameter("postId"));

	final StudentSession studSess = (StudentSession) userSess;
	final boolean isInstructorOrTA = role != User.STUDENT || studSess.isTA();
	ForumTopic topic;
	int[] linkedAssgtQIds;
	if (topicId == 0) { 
		linkedAssgtQIds = new int[] {
				MathUtils.parseInt(request.getParameter("linkedHWId")),
				MathUtils.parseInt(request.getParameter("linkedQId"))};
		topic = new ForumTopic(course.getId(), user.getUserId(), linkedAssgtQIds); 
	} else {
		topic = studSess.getForumTopic(topicId);
		linkedAssgtQIds = topic.getLinkedAssgtQ();
	} // if topicId
	final ForumPost post = (postId == 0 
			? new ForumPost(topicId, user.getUserId())
			: studSess.getForumPost(postId));

	final int MOLECULE = ForumPost.MOLECULE;
	final int SYNTHESIS = ForumPost.SYNTHESIS;
	final int LEWIS = ForumPost.LEWIS;
	final int IMAGE = ForumPost.IMAGE;
	final int UNKNOWN = ForumPost.UNKNOWN;
	final String SELECTED = " selected=\"selected\"";

	final String titleStr = Utils.inputToCERs(request.getParameter("title"));
	final String title = (titleStr == null ? topic.getTitle() : titleStr); 
	final String textStr = Utils.inputToCERs(request.getParameter("text"));
	final String text = (textStr == null ? post.getText() : textStr); 
	// Utils.alwaysPrint("addPost.jsp: textStr = ", textStr, ", text = ", text);
	final String figTypeStr = request.getParameter("figType");
	final int figType = (figTypeStr == null ? post.getFigureType()
			: MathUtils.parseInt(figTypeStr)); 
	final boolean usesChemAxon = figType == MOLECULE || figType == SYNTHESIS;
	final String figureStr = request.getParameter("figure");
	final boolean changeFigType = 
			"true".equals(request.getParameter("changeFigType"));
	final String uploadedFileName = request.getParameter("uploadedFileName");
	final String figure = (uploadedFileName != null ? uploadedFileName
			: changeFigType ? ""
			: figureStr == null ? post.getFigure() 
			: figureStr); 

	final Map<Integer, String> reactionNamesByIds =
			(figType == SYNTHESIS ? RxnCondition.getRxnNamesKeyedByIds() : null);
	String[] chosenRxns = new String[0];
	if (figType == SYNTHESIS) {
		// translate [simply mix]
		final Integer noRgts = Integer.valueOf(RxnCondition.NO_REAGENTS);
		final String defaultRgt = user.translate(reactionNamesByIds.get(noRgts));
		reactionNamesByIds.put(noRgts, defaultRgt);
		final String rxnIds = Synthesis.getRxnConditions(figure);
		/* Utils.alwaysPrint("addPost.jsp: rxnIds = ", rxnIds, 
		 		" for synthesis:\n", figure); /**/
		if (!Utils.isEmpty(rxnIds)) {
			chosenRxns = rxnIds.split(Synthesis.RXN_ID_SEP);
		 	 Utils.alwaysPrint("addPost.jsp: chosenRxns = ", chosenRxns); /**/ 
		} // if there are selected synthesis reactions
	} // if synthesis Q 
	final String APPLET_NAME = "responseApplet";
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
<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/lewisJS.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/position.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/svgGraphics.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/wz_jsgraphics.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= 
		AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<% if (figType == SYNTHESIS) { %>
	<script src="<%= pathToRoot %>js/rxnCondsEditor.js" type="text/javascript"></script>
<% } // if question type %>
<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/navigation/courseSidebarJS.jsp.h" %>
	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>

	function initLewis() {
		initLewisConstants(
				[<%= LewisMolecule.CANVAS_WIDTH %>, 
					<%= LewisMolecule.CANVAS_HEIGHT %>],
				<%= LewisMolecule.MARVIN_WIDTH %>, 
				['<%= LewisMolecule.PAIRED_ELECS %>',
					'<%= LewisMolecule.UNPAIRED_ELECS %>',
					'<%= LewisMolecule.UNSHARED_ELECS %>' ],
				'<%= LewisMolecule.LEWIS_PROPERTY %>',
				'<%= LewisMolecule.HIGHLIGHT %>',
				'<%= user.translateJS("Enter an element symbol.") %>',
				'<%= user.translateJS("There is no such element.  Please try again.") %>',
				'<%= user.translateJS("Other") %>');
		<% if (figType == LEWIS) { %>
			initLewisGraphics('<%= pathToRoot %>', 
					'lewisJSCanvas', 
					'lewisJSToolbars');
			parseLewisMRV('<%= Utils.toValidJS(figure) %>');
		<% } // if LEWIS %>
		var figuresData = [];
		<% if (postId == 0 && topicId != 0) { 
			final ForumPost[] posts = studSess.getForumPosts(topicId);
			for (int postNum = 1; postNum <= posts.length; postNum++) { %>
				if (cellExists('figData<%= postNum %>')) {
					figuresData.push([
							getValue('figData<%= postNum %>'),
							0,
							<%= postNum %>]);
				} // if figure is Lewis
		<% 	} // for each post
		} // if there are older posts in this topic %>
		if (!isEmpty(figuresData)) {
			loadLewisInlineImages('<%= pathToRoot %>', figuresData);
		}
	} // initLewis()

	function initSynthesis() {
		<% if (figType == SYNTHESIS) { %>
			"use strict";
			var constants = new Array();
			constants[NO_REAGENTS] = '<%= RxnCondition.NO_REAGENTS %>';
			constants[RXN_ID_SEP] = '<%= Synthesis.RXN_ID_SEP %>';
			constants[RXN_IDS] = '<%= Synthesis.RXN_IDS %>';
			constants[CLICK_HERE] = '<%= Utils.toValidJS(rcPhrases.get(CLICK_RXN)) %>';
			constants[INSERT_HERE] = '<%= rcPhrases.get(INSERT_HERE) == null ? "" 
						: Utils.toValidJS(rcPhrases.get(INSERT_HERE)) %>';
			constants[ADD_1ST] = '<%= Utils.toValidJS(rcPhrases.get(ADD_1ST)) %>';
			constants[RXN_CONDN] = '<%= Utils.toValidJS(rcPhrases.get(RXN_CONDN)) %>';
			constants[PATH_TO_CHOOSE_RXN_CONDS_USER] = '<%= pathToChooseRxnCondsUser %>';
			constants[REMOVE] = '<%= Utils.toValidJS(rcPhrases.get(REMOVE)) %>';
			constants[AFTER_HERE] = '<%= rcPhrases.get(AFTER_HERE) == null ? ""
					: Utils.toValidJS(rcPhrases.get(AFTER_HERE)) %>';
			initRxnConds(constants);
			<% final int[] allRxnIds = RxnCondition.getAllReactionIds();
			for (final int rxnId : allRxnIds) { %>
				setRxnName(<%= rxnId %>, '<%= Utils.toValidJS(
						Utils.toDisplay(reactionNamesByIds.get(
							Integer.valueOf(rxnId)))) %>');
				setAllowedRxn(<%= rxnId %>);
			<% } // for each rxn id
			final int numRxns = chosenRxns.length;
			for (final String chosenRxn : chosenRxns) { %>
				setChosenRxn(<%= chosenRxn %>); 
			<% } // for each initial reaction condition %>
			writeRxnConds(false);
		<% } // if SYNTHESIS %>
	} // initSynthesis()

	<% if (figType == SYNTHESIS) { %>
	
		function loadPastedSynthMRV() {
			"use strict";
			var pastedSynMRV = getValue('pasteSynthesis');
			loadJSMol(pastedSynMRV, '<%= APPLET_NAME %>');
			ajaxURIArr = [];
			ajaxURIArr.push(['mrvStr', encodeURIComponent(pastedSynMRV)]);
			callAJAX('<%= pathToRoot %>includes/getRxnIdsFromMRV.jsp', 
					getAjaxURI(), showRxnCondns);
		} // loadPastedSynthMRV()

		function showRxnCondns() {
			"use strict";
			if (xmlHttp.readyState === 4) { // ready to continue
				var responsePage = xmlHttp.responseText;
				var rxnIdsStr = extractField(responsePage, 'rxnIdsStrValue');
				if (isEmpty(rxnIdsStr)) chosenRxns = [];
				else {
					chosenRxns = rxnIdsStr.split('<%= Synthesis.RXN_ID_SEP %>');
					chosenRxns.unshift('');
				} // if rxnIds have been chosen
				writeRxnConds(false);
				clearInnerHTML('pasteSynCell');
			} // if ready
		} // showRxnCondns()

	<% } // if SYNTHESIS %>

	function changeFigureType() {
		form = document.forumPostForm;
		form.changeFigType.value = 'true';
		form.action = 'addPost.jsp?topicId=<%= topicId %>'
				+ '&topics50=<%= topics50 %>'
				+ '&postId=<%= postId %>';
		form.submit();
	} // changeFigureType()

	function uploadImage() {
		form = document.forumPostForm;
		if (isWhiteSpace(form.srcFile.value)) {
			alert('Type or browse to the file that contains the image.');
		} else {
			form.action = 'imgUpload.jsp';
			form.enctype = 'multipart/form-data';
			form.submit();
		}
	} // uploadImage()

	function loadSelections() { ; }

	function submitIt(checkValues) {
		var form = document.forumPostForm;
		<% if (usesChemAxon) { %> 
			marvinSketcherInstances['<%= APPLET_NAME %>'].
					exportStructure('<%= MRV_EXPORT %>').then(function(mrv) {
		<% } // if uses MarvinJS %>
		<% if (figType == IMAGE) { %>
			form.figure.value = '<%= figure %>'; // filename
		<% } else if (figType == LEWIS) { %>
			form.figure.value = getLewisMRV();
		<% } else if (figType == MOLECULE || figType == SYNTHESIS) { %>
			form.figure.value = mrv;
			<% if (figType == SYNTHESIS) { %>
				form.rxnIds.value = getRxnIds();
			<% } // if figType %>
		<% } else { // no figure %>
			form.figure.value = '';
		<% } // if figType %>
		if (checkValues) {
			<% if (topicId == 0) { %>
				if (isWhiteSpace(form.title.value)) {
					toAlert('<%= user.translateJS("Please enter a title for "
							+ "this forum topic.") %>');
					return;
				} // if no title
			<% } // if new topic %>
			if (isWhiteSpace(form.text.value)) {
				toAlert('<%= user.translateJS("Please enter some text for "
						+ "your post.") %>');
				return;
			} // if no text
		} // if checkValues
		form.submit();
		<% if (usesChemAxon) { %> 
			}, function(error) {
				alert('Molecule export failed:' + error);	
			});
		<% } // if uses MarvinJS %>
	} // submitIt()

	function cancel() {
		<% if (topicId == 0) { %>
			self.location.href = 'topics.jsp?topics50=<%= topics50 %>';
		<% } else { %>
			self.location.href = 'posts.jsp?topicId=<%= topicId %>&topics50=<%= topics50 %>';
		<% } // if there's a topicId %>
	} // cancel()

	function setImage(cell, href, extra) {
		var bld = new String.builder();
		if (!isEmpty(href)) {
			bld.append('<table><tr><td id="enlarge').append(cell).
					append('" class="boldtext" '
						+ 'style="width:100%; text-align:right;">'
						+ '<%= user.translateJS("Click image to enlarge") %>'
						+ '</td></tr><tr><td>'
						+ '<a href="javascript:enlargeImage(\'').
					append(href).append('\')"><img src="').append(href).
					append('" alt="picture" style="visibility:hidden;" '
						+ 'onload="prepareImage(this, \'enlarge').append(cell).
					append('\');" onmouseover="this.style.cursor=\'pointer\'" /></a>'
						+ '</td></tr></table>');
			html = bld.toString();
		} else bld.append('<%= user.translateJS("No image available.") %>');
		bld.append(extra);
		setInnerHTML(cell, bld.toString());
	} // setImage()

	// -->
</script>
</head>
<body style="text-align:center; margin:0px; overflow:auto;" 
		onload="setTab('<%= toTabName(user.translateJS("Forum")) %>'); 
			initLewis();
			initSynthesis();">

	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>
<div id="contentsWithTabsWithFooter">

<form name="forumPostForm" action="savePost.jsp" method="post" accept-charset="UTF-8">
	<input name="topics50" type="hidden" value="<%= topics50 %>" />
	<input name="topicId" type="hidden" value="<%= topicId %>" />
	<input name="posts50" type="hidden" value="<%= posts50 %>" />
	<input name="postId" type="hidden" value="<%= postId %>" />
	<input name="linkedHWId" type="hidden" value="<%= linkedAssgtQIds[0] %>" />
	<input name="linkedQId" type="hidden" value="<%= linkedAssgtQIds[1] %>" />
	<input name="changeFigType" type="hidden" value="false" />
	<input name="figure" type="hidden" value="" />
	<input name="rxnIds" type="hidden" value="" />
<% if (topicId == 0) { %>
	<table summary="title" style="padding-top:10px; padding-left:10px;">
	<tr>
	<td class="boldtext big" style="padding-right:10px;">
		<%= user.translate("Start a new topic") %>
	</td>
	</tr>
	</table>
<% } // if a new topic %>

<table class="whiteTable" style="width:90%; margin-left:auto; margin-right:auto;
		border-style:none; border-collapse:collapse; text-align:left;" 
		summary="postEntryForm">
	<tr>
	<td style="padding-bottom:10px;">
		<table style="width:100%;">
		<tr>
		<td>
			<% if (topicId == 0) { %>
				<%= user.translate("Enter a title for the topic:") %>
				<input type="text" name="title" size="80" 
						value="<%= Utils.toValidTextbox(title) %>"/>
			<% } else { %>
				<span class="boldtext big">
				<%= user.translate(postId == 0 
						? "Add a post to: " : "Edit a post of:") %>
				<%= Utils.toDisplay(topic.getTitle()) %>
				</span>
			<% } // if there's a topic %>
		</td>
		<% if (!Utils.isEmpty(linkedAssgtQIds) 
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
			if (hwNum > 0 && !Utils.isEmpty(qNumArr)) { %>
				<td class="regtext;">
					<% if (qNumArr.length == 1) { %>
						<%= user.translate("Linked to assignment "
								+ "***1***, question ***1***", 
								new int[] {hwNum, qNumArr[0]}) %> 
					<% } else { %>
						<%= user.translate("Linked to assignment ***1***, "
								+ "one of questions ***1***&ndash;***5***", 
								new int[] {hwNum, qNumArr[0], qNumArr[1]}) %> 
					<% } // if the question is fixed %>
				</td>
			<% } // if there are assigned questions
		} // if there's a linked assignment and question
		if (topicId == 0 && isInstructorOrTA) { %>
			<td style="text-align:right;">
				<%= user.translate("Sticky") %>
				<input type="checkbox" name="sticky" <%=
						topic.isSticky() ? "checked=\"checked\"" : "" %>>
			</td>
		<% } // if should have sticky option %>
		</tr>
		</table>
	</td>
	</tr>
	<tr>
	<td style="width:100%; height:100%; background-color:#f6f7ed;
			padding-left:10px; padding-right:10px; vertical-align:bottom;
			border-style:solid; border-width:1px; border-color:black; margin-right:0px;">
		<table style="width:100%" summary="">
		<tr>
		<td style="width:50%; height:100%; background-color:#f6f7ed;
				padding-left:10px; padding-right:10px; margin-left:0px;">
			<%= user.translate("Enter your post here:") %>
		</td>
		<td style="text-align:right">
			<select name="figType" onchange="changeFigureType();">
				<option value="<%= UNKNOWN %>"
						<%= figType == UNKNOWN ? SELECTED : "" 
						%>>None</option>
				<option value="<%= MOLECULE %>"
						<%= figType == MOLECULE ? SELECTED : "" 
						%>>MarvinJS&trade;</option>
				<option value="<%= SYNTHESIS %>"
						<%= figType == SYNTHESIS ? SELECTED : "" 
						%>><%= Utils.toString("MarvinJS&trade; ",
								Utils.toPopupMenuDisplay(
									user.translateJS("synthesis"))) %></option>
				<option value="<%= LEWIS %>"
						<%= figType == LEWIS ? SELECTED : "" 
						%>>LewisJS&trade;</option>
				<option value="<%= IMAGE %>"
						<%= figType == IMAGE ? SELECTED : "" 
						%>>Image</option>
			</select>
		</td>
		</tr>
		<tr>
		<td style="width:50%; height:100%; background-color:#f6f7ed;
				padding-left:10px; padding-right:10px; padding-top:10px;
				padding-bottom:10px; vertical-align:top; text-align:center; 
				margin-right:0px;">
			<textarea name="text" rows="1" cols="10"
					style="height:180px; width:100%;"><%= 
					Utils.toValidTextbox(text) %></textarea>
		</td>
		<td style="width:50%; height:100%; background-color:#f6f7ed;
				padding-left:10px; padding-right:10px; padding-top:10px;
				padding-bottom:10px; vertical-align:top; margin-left:0px;
				text-align:center;">
			<% if (usesChemAxon) { %>
				<table style="margin-left:auto; margin-right:auto;">
		   		<tr><td class="boldtext" style="text-align:right; padding-left:10px; 
						padding-right:10px; padding-top:10px; font-style:italic;">
					MarvinJS&trade;
				</td></tr>
				<tr><td id="<%= APPLET_NAME %>">
				<script type="text/javascript">
				// <!-- >
					startMarvinJS('<%= Utils.toValidJS(figure) %>', 
							<%= figType == SYNTHESIS ? "SYNTHESIS" : "MARVIN" %>, 
							<%= figType == SYNTHESIS ? "0" : "SHOWLONEPAIRS" %>, 
							'<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
				// -->
				</script>
				</td></tr>
				<% if (figType == SYNTHESIS) { %>
					<tr><td id="reagentTable" class="regtext">
					</td></tr>
					<tr><td>
					<table>
					<tr><td>
					<p><%= user.translate("Or paste a synthesis copied with "
							+ "chosen reaction conditions here:") %>
					<p><textarea name="pasteSynthesis" 
							id="pasteSynthesis"
							rows="1" cols="10"
							onkeyup="loadPastedSynthMRV();"
							style="height:40px; width:100%;"></textarea>
					</td></tr>
					</table>
					</td></tr>
				<% } // if is synthesis %>
				</table>
			<% } else if (figType == LEWIS) { %>
				<table class="whiteTable" summary="">
				<tr><td>
					<table class="rowsTable" 
							style="margin-left:auto; margin-right:auto; 
								width:<%= LewisMolecule.CANVAS_WIDTH %>px;">
					<tr><td style="width:100%;" id="lewisJSToolbars">
					</td></tr>
					<tr><td style="width:100%;"><div id="lewisJSCanvas" 
							style="position:relative;height:<%= 
								LewisMolecule.CANVAS_HEIGHT %>px;width:100%;"></div>
					</td></tr>
					</table>
				</td></tr>
				</table>
			<% } else if (figType == IMAGE) { 
				final boolean emptyFig = Utils.isEmpty(figure);
				final String figURL = (emptyFig ? "" : Utils.toString(pathToRoot, figure));
				final String msg = (emptyFig ? "" 
						: user.translateJS("Load a new figure: "));
			%>
				<span id="responseAppletCell"></span>
				<script type="text/javascript">
				// <!-- >
					setImage('responseAppletCell', 
							'<%= figURL %>',
							'<br \/><br \/><%= msg %>'
							+ '<input type="file" name="srcFile" size="30" \/>'
							+ '<input type="button" value="Upload" '
							+ 'onclick="uploadImage();" \/>');
				// -->
				</script>
			<% } // if figType %>
		</td>
		</tr>
		<% if (role == User.STUDENT) { %>
			<tr>
			<td>
				<input type="checkbox" name="anonymous" 
						<%= post.isAnon() ? "checked=\"checked\"" : "" %> />
				<%= user.translate("Post anonymously?") %>
			</td>
			</tr>
		<% } // if student %>
		</table>
	</td>
	</tr>

<% if (topicId != 0) { 
	final ForumPost[] posts = studSess.getForumPosts(topicId);
	int mviewNum = 0; %>
		<tr>
		<td style="padding-top:10px; padding-bottom:10px;">
			<span class="boldtext big">
			<%= user.translate("Previous posts:") %>
			</span>
		</td>
		</tr>
		<tr>
		<td style="padding-bottom:10px;">
			<table class="whiteTable" style="width:100%; border-collapse:collapse;
					 text-align:left;" summary="posts">
			<% for (int postNum = posts.length; postNum >= 1; postNum--) {
				final ForumPost oldPost = posts[postNum - 1];
				if (postId == oldPost.getId()) continue;
				final String rowColor = 
						(postNum % 2 == 0 ? "whiterow" : "greenrow"); 
				final String dateCreated = 
						oldPost.getDateCreated(course.getTimeZone());
				final String dateLastEdited = 
						oldPost.getDateLastEdited(course.getTimeZone());
				final boolean edited = !dateCreated.equals(dateLastEdited);
				final boolean makeAnon = oldPost.isAnon() 
						&& !oldPost.getAuthorId().equals(user.getUserId());
			%>
				<tr class="<%= rowColor %>">
				<td class="regtext" style="padding-left:10px; padding-top:10px; 
						padding-right:10px;">
					(<b><%= postNum + ((posts50 - 1) * 50) %></b>)
				</td>
				<td class="regtext" style="padding-left:10px; padding-top:10px; 
						padding-right:10px;">
					<%= makeAnon && isInstructorOrTA 
							? Utils.getBuilder(user.translate("Anonymous"), 
								" <!-- ", oldPost.getAuthorName(), " -->")
							: makeAnon ? user.translate("Anonymous")
							: oldPost.getAuthorName() %>
					<br />
					<%= user.translate("First posted") %> <%= dateCreated %>
					<% if (edited) { %>
						<br />
						<%= user.translate("Last edited") %> <%= dateLastEdited %>
					<% } // if post has been edited %>
				</td>
				<td class="regtext" style="padding-left:10px; padding-top:10px; 
						padding-right:10px;">
					<%= Utils.toDisplay(oldPost.getText()) %>
				</td>
				<td class="regtext" style="padding-left:10px; padding-top:10px; 
					padding-right:10px; text-align:center;">
					<% if (oldPost.hasFigure()) { 
						final String oldFig = oldPost.getFigure(); 
						final String postFigIdStr = Utils.toString("fig", postNum); %>
						<table class="whiteTable">
						<tr><td id="<%= postFigIdStr %>">
							<% if (oldPost.figureIsImage()) { %>
								<script type="text/javascript">
								// <!-- >
								setImage('<%= postFigIdStr %>',
										'<%= Utils.isEmpty(oldFig) ? ""
											: Utils.toString(pathToRoot, oldFig) %>');
								// -->
								</script>
							<% } else if (oldPost.figureIsLewis()) { %>
								<input type="hidden" id="figData<%= postNum %>"
										value="<%= Utils.toValidHTMLAttributeValue(oldFig) %>" />
							<% } else { %>
								<%= oldPost.getImage(pathToRoot, user.prefersPNG(),
											postFigIdStr) %>
								<%= Synthesis.getRxnsDisplay(oldFig, user) %>
							<% } // if figureType %>
						</td></tr>
						</table>
					<% } // if old post has figure %>
				</td>
				</tr>
			<% } // for each post in topic %>
			</table>
		</td>
		</tr>
<% } // if adding a post to an existing topic %>
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
