<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.lewis.LewisMolecule,
	com.epoch.qBank.Question,
	com.epoch.textbooks.Textbook,
	com.epoch.textbooks.TextChapter,
	com.epoch.textbooks.TextContent,
	com.epoch.utils.MathUtils,
	java.util.ArrayList,
	java.util.List"
%>

<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%
	response.setHeader("Cache-Control","no-store, must-revalidate"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noAccess.html" /> <%
	}
	Textbook book;
	synchronized (session) {
		book = (Textbook) session.getAttribute("textbook");
	} // synchronized
	int chapNum = MathUtils.parseInt(request.getParameter("chapNum"));
	if (chapNum == 0) {
		book.addNewChapter();
		chapNum = book.getNumChapters();
	}
	final TextChapter chapter = book.getChapter(chapNum);
	final List<TextContent> contents = chapter.getContents();
	final int numContents = contents.size();
	final String[] allAuthorNames = (String[]) session.getAttribute("allAuthorNames");

	final boolean newOwner = "true".equals(request.getParameter("newOwner"));
	final boolean saved = "true".equals(request.getParameter("saved"));

	final boolean containsJmol = chapter.containsJmol();
	final boolean containsLewis = chapter.containsLewis();
	int jmolNum = 0;
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<meta http-equiv="Pragma" content="no-cache"/>
<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<title>ACE Embedded Textbooks</title>
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<style type="text/css">
		#footer {
			position:absolute; 
			bottom:0; 
			left:0;
			width:100%; 
			height:50px; 
			overflow:auto; 
			text-align:right; 
		}

		* html body {
			padding:55px 0 50px 0; 
		}
</style>
<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<% if (containsJmol) { %>
	<script src="<%= pathToRoot %>js/jmolStart.js" type="text/javascript"></script>
		<!-- the next two resources must be called in the given order -->
	<script src="<%= pathToRoot %>nosession/jsmol/JSmol.min.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>nosession/jsmol/Jmol2.js" type="text/javascript"></script>
<% } else if (containsLewis) { %>
	<script src="<%= pathToRoot %>js/lewisJS.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<% } // if there's a Jmol or Lewis figure %>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= 
		AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>

	<% if (containsJmol) { %>
		jmolInitialize('../nosession/jmol');
	<% } else if (containsLewis) { %>

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
			<% int cNum = 0;
			for (final TextContent content : contents) { 
				cNum++; %>
				if (cellExists('figData<%= cNum %>')) {
					figuresData.push([
							getValue('figData<%= cNum %>'),
							0,
							'<%= cNum %>']);
				} // if content is Lewis
			<% } // for each content %>
			if (!isEmpty(figuresData)) {
				loadLewisInlineImages('<%= pathToRoot %>', figuresData);
			}
		} // initLewis()

	<% } // if there's a Jmol or Lewis figure %>

	function goBack() {
		self.location.href = 'chooseTextbook.jsp';
	} // goBack()

	function showChapter() {
		self.location.href = 'previewChapter.jsp?chapNum=<%= chapNum %>&preview=true';
	} // showChapter()

	function reloadMe() {
		var form = document.chapterForm;
		form.action.value = 'reload';
		form.submit();
	} // reloadMe()

	function releaseLock() {
		var form = document.chapterForm;
		form.action.value = 'releaseLock';
		form.submit();
	} // releaseLock()

	function writeTextbook() {
		if (titleOK()) {
			var form = document.chapterForm;
			form.action.value = 'writeTextbook';
			form.submit();
		} // if title is OK
	} // goBack()

	function editContent(contentNum) {
		if (titleOK()) {
			var form = document.chapterForm;
			form.action.value = 'editContent';
			form.contentNum.value = (contentNum === 0
					? <%= numContents + 1 %> : contentNum);
			openEvaluatorWindow('loadContent.jsp?chapNum=<%= chapNum %>&contentNum=' 
					+ contentNum);
		} // if title is OK
	} // editContent()

	function dupeContent(contentNum) {
		if (titleOK()) {
			var form = document.chapterForm;
			form.action.value = 'editContent';
			form.contentNum.value = 0;
			openEvaluatorWindow('loadContent.jsp?dupe=true&chapNum=<%= chapNum %>&contentNum=' 
					+ contentNum);
		} // if title is OK
	} // dupeContent()

	function deleteContent(contentNum) {
		var form = document.chapterForm;
		form.action.value = 'deleteContent';
		form.contentNum.value = contentNum;
		form.submit();
	} // deleteContent()

	function moveContent(contentNum) {
		var form = document.chapterForm;
		form.action.value = 'moveContent';
		form.contentNum.value = contentNum;
		form.moveTo.value = 
				getCell('content' + contentNum + 'Selector').value;
		form.submit();
	} // moveContent()

	function moveSections() {
		document.chapterForm.action.value = 'moveSections';
		openEvaluatorWindow('moveSections.jsp?chapNum=<%= chapNum %>');
	} // moveSections()

	function startQ(qId) {
		var go = 'startQuestion.jsp?isInstructorOrTA=true&qId=' + qId;
		openPreviewWindow(go);
	} // startQ()

	function save() {
		if (titleOK()) {
			var form = document.chapterForm;
			form.action.value = 'save';
			form.submit();
		} // if title is OK
	} // save()

	function reset(recentOnly) {
		var form = document.chapterForm;
		if (recentOnly) {
			form.action.value = 'reset';
			form.submit();
		} else self.location.href = 'writeTextbook.jsp?bookId=<%= book.getId() %>&saved=true';
	} // reset()

	function titleOK() {
		var titleOK = !isWhiteSpace(document.chapterForm.chapterName.value);
		if (!titleOK) {
			toAlert('Please enter a title for the chapter.');
		} // if title is not OK
		return titleOK;
	} // titleOK()

	function loadMessage() {
		<% if (newOwner) { %>
			var bld = new String.builder().
					append('You have made a copy of another instructor\'s textbook '
						+ 'to use as your own. ACE will not incorporate any changes '
						+ 'that you make into the original textbook, and vice versa.');
			<% final List<int[]> localQIds = new ArrayList<int[]>();
			int oneChapNum = 0;
			for (final TextChapter chap : book.getChapters()) {
				oneChapNum++;
				int contentNum = 0;
				for (final TextContent content : chap.getContents()) {
					contentNum++;
					if (content.isACEQuestion()
							&& MathUtils.parseInt(content.getContent()) < 0) {
						localQIds.add(new int[] {oneChapNum, contentNum});
					} // if have locally authored ACE question
				} // for each content
			} // for each chapter
			if (!localQIds.isEmpty()) { %>
				bld.append('\n\nThe following '
						+ 'questions referenced by the original textbook '
						+ 'will not be visible to you or your students. You '
						+ 'will need to delete or replace them.\n');
				<% for (final int[] localQId : localQIds) { %>
					bld.append('\nChapter <%= localQId[0] %>, '
							+ 'item <%= localQId[1] %>');
				<% } // for each local Q %>
			<% } // if there are local Qs %>
			toAlert(bld.toString());
		<% } // if a new owner %>
	} // loadMessage()

	function formatting() {
		alert('Each piece of content that is text or an ACE question will appear on '
				+ 'the left as a separate paragraph.\n\nOther kinds of content will '
				+ 'appear to the right of the text paragraph that they follow.');
	} // formatting()

	function makeButtons() {
		setInnerHTML('saveCell', '<%= Utils.toValidJS(makeButton(
				"Save book", "save();")) %>');
		setInnerHTML('discardRecentCell', '<%= Utils.toValidJS(makeButton(
				"Discard recent changes", "reset(true);")) %>');
		setInnerHTML('discardAllCell', '<%= Utils.toValidJS(makeButton(
				"Discard all changes", "reset(false);")) %>');
	} // makeButtons()

	// -->

</script>
</head>
<body class="light" style="background-color:white;" 
		onload="loadMessage();<%= saved ? "" : " makeButtons();" %><%=
			containsLewis ? " initLewis();" : "" %>">

<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
<div id="contentsWithoutTabsWithFooter">

<form name="chapterForm" action="chapterActions.jsp" method="post" accept-charset="UTF-8">
	<input type="hidden" name="chapNum" value="<%= chapNum %>" />
	<input type="hidden" name="contentNum" value="" />
	<input type="hidden" name="action" value="" />
	<input type="hidden" name="moveTo" value="" />
<table style="width:95%; margin-left:auto; margin-right:auto;" summary="">
	<tr>
	<td class="boldtext big" 
			style="vertical-align:top; padding-top:10px; padding-bottom:10px;">
		ACE Online Textbooks
	</td>
	</tr>
	<tr>
	<td class="boldtext" style="text-align:left; padding-bottom:10px;">
		<i><%= book.getName() %></i>, 
		<% final int numAuthors = allAuthorNames.length;
		if (numAuthors == 2) { %>
			<%= allAuthorNames[0] %> and <%= allAuthorNames[1] %>,
		<% } else { 
			for (int authNum = 1; authNum <= numAuthors; authNum++) { %>
				<%= allAuthorNames[authNum - 1] 
						+ (authNum == numAuthors - 1 ? ", and" : ",") %>
			<% } // for each author
		} // if numAuthors %>
		Chapter <%= chapNum %>:
		<input type="text" name="chapterName" size="80"
				onkeypress="makeButtons();"
				value="<%= Utils.toValidTextbox(chapter.getName()) %>" /> 
	</td>
	<td class="regtext" style="text-align:center;">
		<a href="#content<%= numContents %>">Go to bottom</a>
	</td>
	<td class="regtext" style="text-align:right;">
		<a href="javascript:formatting();">formatting</a>
	</td>
	</tr>
	<% if (numContents > 0) { %>
		<tr>
		<td colspan="3" class="regtext" style="text-align:left; padding-bottom:10px;">
			<table class="whiteTable" style="width:100%; margin-left:auto; 
					margin-right:auto; border-collapse:collapse;" summary="">
			<% int contentNum = 0;
			int figNum = 0;
			for (final TextContent content : contents) { 
				contentNum++;
				final String data = content.getFormattedContent("write",
						pathToRoot, 400, 300);
				final String rowColor = (contentNum % 2 != 0 
						? "greenrow" : "whiterow");
			%>
				<tr class="<%= rowColor %>" 
						style="padding-top:5px; border-collapse:collapse;">
				<td style="padding-left:10px; padding-top:5px; padding-bottom:5px;">
					<table>
					<tr>
					<td>
						<select name="content<%= contentNum %>Selector" 
								id="content<%= contentNum %>Selector" 
								onchange="moveContent(<%= contentNum %>);">
						<% for (int num = 1; num <= numContents; num++) { %>
							<option value="<%= num %>" <%= num == contentNum 
									? "selected=\"selected\"" : "" %>><%= num %></option>
						<% } // for each number %>
						</select>
					</td>
					<td>
						<%= makeButtonIcon("edit", pathToRoot,
								"editContent(", contentNum, ", ", 
								content.getContentType(), ");") %>
					</td>
					<td>
						<%= makeButtonIcon("delete", pathToRoot,
								"deleteContent(", contentNum, ");") %>
					</td>
					<td>
						<%= makeButtonIcon("duplicate", pathToRoot,
								"dupeContent(", contentNum, ");") %>
					</td>
					</tr>
					</table>
				</td>
				<td style="width:90%; padding-left:10px; 
						padding-top:5px; padding-bottom:5px;">
					<a name="content<%= contentNum %>"></a>
					<% if (content.isText()) { %>
						<%=	data %>
					<% } else if (content.isACEQuestion()) { %>
						<%= makeButton("Practice question " 
								+ Utils.formatNegative(data),
								"startQ(", data, ");") %>
					<% } else if (content.isMarvin()) { 
						final int viewOpts = MathUtils.parseInt(
								content.getExtraData());
						final int[] dims = content.getBestAppletSize();
						final String contentIdStr = Utils.toString(
								"content", contentNum);
					%>
						<table>
						<tr><td id="launchMViewCell<%= contentNum %>" 
								class="boldtext" style="text-align:right; 
									padding-left:10px; font-style:italic;">
							<a onclick="launchMView_<%= contentNum %>();"><u>Launch 
								MarvinJS&trade; viewer</u></a>
						</td></tr>
						<tr><td id="<%= contentIdStr %>">
						<%= content.getImage(pathToRoot, user.prefersPNG(),
								contentIdStr) %>
							<script type="text/javascript">
								// <!-- >
								function getMolForMView_<%= contentNum %>() {
									return '<%= Utils.toValidJS(data) %>';
								} // getMolForMView_<%= contentNum %>()

								function launchMView_<%= contentNum %>() {
									var url = new String.builder().
											append('<%= pathToRoot 
												%>includes\/marvinJSViewer.jsp' +
												'?viewOpts=<%= viewOpts %>&getMolMethodName=').
											append(encodeURIComponent(
												'getMolForMView_<%= contentNum %>()')).
											toString();
									openSketcherWindow(url);
									/* startMViewWebStart(
											'<%= Utils.toValidJS(data) %>', 
											'<%= pathToRoot %>', 
											'<%= user.getUserId() %>'); /**/
								} // launchMView_<%= contentNum %>()
								// -->
							</script>
						</td></tr>
						</table>
					<% } else if (content.isLewis()) { %>
						<table><tr><td style="background-color:white;">
						<span id="fig<%= contentNum %>">
						<input type="hidden" id="figData<%= contentNum %>"
								value="<%= Utils.toValidHTMLAttributeValue(data) %>" />
						</span>
						</td></tr></table>
					<% } else if (content.isJmol()) { 
						jmolNum++;
						final String[] jmolCmds = content.getJmolScripts(); 
						final String jmolScripts = jmolCmds[TextContent.JMOL_SCRIPTS];
						final String jmolJSCmds = jmolCmds[TextContent.JMOL_JS_CMDS]; %>
						<script type="text/javascript">
							// <!-- >
							setJmol(<%= jmolNum %>, 
									'<%= Utils.toValidJS(data) %>',
									'#ffffff', 250, 250,
									'<%= Utils.toValidJS(jmolScripts) %>');
							<%= jmolJSCmds + (Utils.isEmpty(jmolJSCmds) 
									|| jmolJSCmds.endsWith(";") ? "" : ";") %>
							// -->
						</script>
					<% } else if (content.isImage() || content.isImageURL()) { 
						/* Utils.alwaysPrint("writeChapter.jsp: content number ",
								contentNum, " is image; data = ", data); /**/ %>
						<table>
						<tr><td id="enlargeCell<%= contentNum %>" class="boldtext" 
								style="width:100%; text-align:right;">
							Click image to enlarge
						</td></tr>
						<tr><td>
							<a href="javascript:enlargeImage('<%= data %>')">
							<img src="<%= data %>" alt="picture"
									style="visibility:hidden;"
									onload="prepareImage(this, 'enlargeCell<%= 
											contentNum %>');" 
									onmouseover="this.style.cursor='pointer'" /></a>
						</td></tr>
						</table>
					<% } else if (content.isMovie()) { 
						/* Utils.alwaysPrint("writeChapter.jsp: content number ",
								contentNum, " is movie; data = ", data); /**/ %>
						<%= data %>
					<% } // if content type %>
				<% final String caption = content.getCaption(); 
				if (!content.isText() && 
						(!content.isACEQuestion() || !Utils.isEmpty(caption))) { %>
					<br/><span class="boldtext">
						<% if (!content.isACEQuestion()) { %>
							Figure <%= chapNum %>.<%= ++figNum %>. 
						<% } // if not an ACE question %>
						<%= Utils.toDisplay(caption) %>
					</span>
				<% } // if content type %>
				</td>
				</tr>
			<% } // for each content %>
			</table>
		</td>
		</tr>
	<% } // if there are contents %>
</table>
</form>
</div>

<div id="footer">
<table summary="navigation" style="width:95%; margin-left:auto; margin-right:auto;">
	<tr>
	<td style="width:95%;"></td>
	<td>
		<%= makeButton("Add content", "editContent(0);") %>
	</td>
	<td>
		<%= makeButton("Move sections", "moveSections();") %>
	</td>
	<td>
		<%= makeButton("Preview", "showChapter();") %>
	</td>
	<td id="saveCell">
	</td>
		<% if (chapter.getId() != 0) { %>
			<td id="discardRecentCell">
			</td>
		<% } // if chapter is not new %>
		<td id="discardAllCell">
		</td>
	<td>
		<%= makeButton("Back to chapters list", "writeTextbook();") %>
	</td>
	<td>
		<%= makeButton("Release lock", "releaseLock();") %>
	</td>
	<td>
		<%= makeButton("Choose new book", "goBack();") %>
	</td>
	</tr>
</table>
</div>
</body>
</html>
