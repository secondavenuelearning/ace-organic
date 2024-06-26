<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.chem.MolString,
	com.epoch.exceptions.DBException,
	com.epoch.lewis.LewisMolecule,
	com.epoch.qBank.Figure,
	com.epoch.qBank.QDatum,
	com.epoch.qBank.QSetDescr,
	com.epoch.qBank.Question,
	com.epoch.qBank.Topic,
	com.epoch.session.KeywordSet,
	com.epoch.session.QuestionBank,
	com.epoch.session.QSet,
	com.epoch.synthesis.Synthesis,
	com.epoch.textbooks.Textbook,
	com.epoch.textbooks.TextChapter,
	com.epoch.textbooks.TextContent,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server

	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noAccess.html" /> <%
	}

	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final int TEXT = TextContent.TEXT;
	final int MARVIN = TextContent.MARVIN;
	final int LEWIS = TextContent.LEWIS;
	final int IMAGE = TextContent.IMAGE;
	final int IMAGE_URL = TextContent.IMAGE_URL;
	final int ACE_Q = TextContent.ACE_Q;
	final int JMOL = TextContent.JMOL;
	final int MOVIE = TextContent.MOVIE;
	final String CHECKED = " checked=\"checked\"";
	final String SELECTED = "selected=\"selected\" ";

	Textbook book;
	synchronized (session) {
		book = (Textbook) session.getAttribute("textbook");
	} // synchronized
	final int chapNum = MathUtils.parseInt(request.getParameter("chapNum"));
	final TextChapter chapter = book.getChapter(chapNum);
	int contentNum = MathUtils.parseInt(request.getParameter("contentNum"));
	boolean brandNew = "true".equals(request.getParameter("brandNew"));
	if (contentNum == 0) {
		chapter.addNewContent();
		contentNum = chapter.getContents().size();
		brandNew = true;
	} // if content is new
	TextContent content = chapter.getContent(contentNum);
	final boolean duplicate = "true".equals(request.getParameter("dupe"));
	if (duplicate) {
		chapter.addNewContent(content);
		contentNum = chapter.getContents().size();
		content = chapter.getContent(contentNum);
		brandNew = true;
	} // if duplicating existing content
	String contentData = content.getContent();
	String contentExtra = content.getExtraData();
	int contentType = content.getContentType();
	final int loadedContentType = 
			MathUtils.parseInt(request.getParameter("contentType"), -1);
	if (!Utils.among(loadedContentType, contentType, -1)) {
		contentType = loadedContentType;
		contentData = "";
		contentExtra = "";
	} // if content type has changed
	final String origTypeStr = request.getParameter("origContentType");
	final int origType = (origTypeStr == null ? contentType
			: MathUtils.parseInt(origTypeStr));
	/* Utils.alwaysPrint("loadContent.jsp: contentNum = ", contentNum, 
			", contentType = ", contentType, ", origType = ", origType, 
			", contentData:\n", Utils.isEmpty(contentData) ? "[empty]"
				: contentData.replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r")); /**/

	String srcFile = null;
	final String uploadedfile = request.getParameter("uploadedfile");
	final String calcResults = request.getParameter("calcResults");
	if (calcResults != null) {
		/* Utils.alwaysPrint("loadContent.jsp: calcResults:\n", calcResults); /**/
		contentData = calcResults;
	}
	if (uploadedfile != null) {
		srcFile = uploadedfile;
	} else if (contentNum != 0 
			&& ((contentType == IMAGE && content.isImage()) 
				|| (contentType == MOVIE && content.isMovie()))) {
		srcFile = (MathUtils.parseInt(contentData, -1) == -1
				? contentData : content.makeImageFileName());
	}
	final String uploadDescrip = (contentType == IMAGE ? "image" 
			: contentType == MOVIE ? "movie" : contentType == JMOL ? "Jmol data" : "");
	/* Utils.alwaysPrint("loadContent.jsp: uploadedfile = '", uploadedfile,
			"', srcFile = '", srcFile, "'."); /**/

	final int TOPIC = 0;
	final int KEYWORD = 2;
	final String viewTypeStr = request.getParameter("viewType");
	final int viewType = MathUtils.parseInt(viewTypeStr);
	int qSetId = 0;
	int jmolNum = 0;
	int mviewNum = 0;
	final String APPLET_NAME = "contentApplet";
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<title>ACE Embedded Textbook Content Editor</title>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css"
	type="text/css" />
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico"
	type="image/x-icon"/>
<style type="text/css">
body {
	margin:0;
	border:0;
	padding:0;
	height:100%; 
	max-height:100%; 
	font-family:arial, verdana, sans-serif; 
	font-size:76%;
	overflow: hidden; 
}

#loadContentsFooter {
	position:absolute; 
	bottom:0; 
	left:0;
	width:100%; 
	height:50px; 
	overflow:auto; 
	text-align:right; 
	vertical-align:bottom;
	padding-top:10px;
}

#questionsList {
	position:fixed; 
	top:0px;
	left:0;
	bottom:50px; 
	right:0; 
	overflow:auto; 
}

* html #questionsList {
	height:100%; 
}

* html body {
	padding:0px 0 50px 0; 
}
</style>
<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<% if (contentType == MARVIN) { %>
	<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>nosession/marvinJS/js/promise.min.js"></script>
	<script src="https://marvinjs.chemicalize.com/v1/<%= AppConfig.marvinJSLicense %>/client-settings.js"></script>
	<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<% } else if (contentType == LEWIS) { %>
	<script src="<%= pathToRoot %>js/lewisJS.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/position.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/svgGraphics.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/wz_jsgraphics.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<% } else if (contentType == JMOL) { %>
	<script src="<%= pathToRoot %>js/jmolStart.js" type="text/javascript"></script>
		<!-- the next two resources must be called in the given order -->
	<script src="<%= pathToRoot %>nosession/jsmol/JSmol.min.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>nosession/jsmol/Jmol2.js" type="text/javascript"></script>
<% } // if contentType %>
<script type="text/javascript">
	// <!-- >

	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>

	function changeContentType() {
		document.selectorForm.submit();
	} // changeContentType()

	function cancelMe() {
		var form = document.contentForm;
		form.cancelling.value = 'true';
		form.submit();
	} // cancelMe()

<% if (Utils.among(contentType, IMAGE, MOVIE, JMOL)) { %>

	function upload() {
		var form = document.fileupload;
		if (isWhiteSpace(form.srcFile.value)) {
			alert('Type or browse to the file that contains the <%= uploadDescrip %>.');
		} else form.submit();
	} // upload()

	function warn(kind) {
		alert('Jmol is very intolerant of incorrect ' + kind
				+ '.  Read the documentation before you use them. '
				+ 'If the question-editing page misbehaves '
				+ 'after you change or enter new ' + kind 
				+ ' and apply the changes, it probably means that '
				+ 'you did not format the ' + kind + ' correctly.');
	} // warn()

	<% if (contentType == JMOL) { %>
		jmolInitialize('<%= pathToRoot %>nosession/jmol'); 
	<% } // if is Jmol figure %>

	function submitIt() {
		<% if (calcResults == null && contentData == null) { %>
			var anAlert = 'You have not uploaded a file.';
			alert(anAlert);
		<% } else { %>
			var form = document.contentForm;
			<% if (contentType == JMOL) { %>
				form.contentData.value = '<%= Utils.toValidJS(contentData) %>';
			<% } // if Jmol %>
			form.submit();
		<% } // if there's a molecule to submit %>
	} // submitIt() for IMAGE, MOVIE, JMOL

<% } else if (Utils.among(contentType, TEXT, IMAGE_URL)) { %>

	function submitIt() {
		var form = document.contentForm;
		form.contentData.value = form.enteredText.value;
		form.submit();
	} // submitIt() for TEXT or IMAGE_URL

<% } else if (contentType == ACE_Q) { %>

	function chooseSelected() {
		var form = document.contentForm;
		var selectedQId = 0;
		if (form.qSetIdSelector.selectedIndex === 0) {
			selectedQId = form.qId.value;
			if (!canParseToInt(selectedQId) || parseInt(selectedQId) === 0) {
				alert('Please enter a valid question ID number.');
				return;
			}
			selectedQId = '#' + selectedQId; // indicates comes from textbox
		} else {
			for (var bNum = 0; bNum < form.qChecker.length; bNum++) { // <!-- >
				var button = form.qChecker[bNum];
				if (button.checked) {
					selectedQId = button.value;
					break;
				} // if button is checked
			} // for each radio button
			if (selectedQId === 0) {
				alert('Please select a question.');
				return;
			} // if no button selected
		} // if qSetId
		form.contentData.value = selectedQId;
		form.submit();
	} // chooseSelected()

<% } else if (contentType == LEWIS) { %>

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
				'Enter an element symbol.',
				'There is no such element.  Please try again.',
				'Other');
		initLewisGraphics('<%= pathToRoot %>', 
				'lewisJSCanvas', 
				'lewisJSToolbars');
		parseLewisMRV('<%= Utils.toValidJS(contentData) %>');
	} // initLewis()

	function submitIt() {
		var form = document.contentForm;
		form.contentData.value = getLewisMRV();
		// alert(form.contentData.value);
		form.submit();
	} // submitIt() for LEWIS

<% } else { // MARVIN %>

	function loadSelections() { ; }

	function submitIt() {
		var form = document.contentForm;
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(source) {
			form.contentData.value = source;
		<% if (contentType == MARVIN) { %>
			var marvinFlags = 0;
			if (form.howShowHydrogens.checked) marvinFlags |= parseInt(form.allNo.value);
			if (form.withMapping.checked) marvinFlags |= SHOWMAPPING;
			if (form.withLonePairs.checked) marvinFlags |= SHOWLONEPAIRS;
			if (form.withRSLabels.checked) marvinFlags |= SHOWRSLABELS;
			form.contentExtra.value = marvinFlags;
		<% } // if not Lewis %>
			form.submit();
		}, function(error) {
			alert('Molecule export failed:' + error);	
		});
	} // submitIt()

<% } // if figtype %>

	// -->
	</script>
</head>

<body class="light" style="margin:0px; margin-top:5px; background-color:#f6f7ed; 
		text-align:center; overflow:auto;"
		<% if (contentType == ACE_Q) { %>
		onload="setApplyButton(); <%= viewType == TOPIC 
					? "loadAllQSets();" : "" %>"
		<% } else if (contentType == LEWIS) { %>
		onload="initLewis();"
		<% } // if contentType %>
		>

<table style="margin-left:auto; margin-right:auto; width:95%;" summary="">
	<tr><td class="boldtext enlarged" style="text-align:left;">
		Content <%= contentNum %>:
	</td></tr>
</table>

<div id="questionsList">
	<form name="selectorForm" action="loadContent.jsp" method="post">
		<input type="hidden" name="chapNum" value="<%= chapNum %>"  />
		<input type="hidden" name="contentNum" value="<%= contentNum %>"  />
		<input type="hidden" name="origContentType" value="<%= origType %>"  />
		<input type="hidden" name="brandNew" value="<%= brandNew %>"  />
		<input type="hidden" name="cancelling" value="false"  />
	<table style="margin-left:auto; margin-right:auto; width:35%;
			text-align:center;" summary="">
		<tr><td>
			<table class="whiteTable" style="text-align:center; padding-left:10px;
					padding-right:10px; padding-top:5px; padding-bottom:5px;
					width:100%;" summary="">
				<tr><td>
					<span class="regtext">Type of figure: </span>
					<select name="contentType" onchange="changeContentType();" >
						<option value="<%= TEXT %>"
								<%= contentType == TEXT ? SELECTED : "" %>> 
							Text </option> 
						<option value="<%= MARVIN %>"
								<%= contentType == MARVIN ? SELECTED : "" %>> 
							Marvin </option> 
						<option value="<%= LEWIS %>"
								<%= contentType == LEWIS ? SELECTED : "" %>> 
							Lewis </option> 
						<option value="<%= IMAGE %>"
								<%= contentType == IMAGE ? SELECTED : "" %>> 
							Image </option>
						<option value="<%= IMAGE_URL %>"
								<%= contentType == IMAGE_URL ? SELECTED : "" %>> 
							Image URL </option>
						<option value="<%= MOVIE %>"
								<%= contentType == MOVIE ? SELECTED : "" %>> 
							Movie </option>
						<option value="<%= JMOL %>"
								<%= contentType == JMOL ? SELECTED : "" %>> 
							Jmol (orbitals, etc.) </option>
						<option value="<%= ACE_Q %>"
								<%= contentType == ACE_Q ? SELECTED : "" %>> 
							ACE practice question </option> 
					</select>
				</td></tr>
			</table>
		</td></tr>
	</table>
	</form>	

<% if (contentType == TEXT) { %>

	<form name="contentForm" action="saveContent.jsp" method="post" accept-charset="UTF-8">
		<input type="hidden" name="chapNum" value="<%= chapNum %>"  />
		<input type="hidden" name="contentNum" value="<%= contentNum %>"  />
		<input type="hidden" name="contentType" value="<%= contentType %>"  />
		<input type="hidden" name="origContentType" value="<%= origType %>"  />
		<input type="hidden" name="contentData" value="<%= 
				Utils.toValidHTMLAttributeValue(contentData) %>" />
		<input type="hidden" name="caption" value=""  />
		<input type="hidden" name="brandNew" value="<%= brandNew %>"  />
		<input type="hidden" name="cancelling" value="false"  />
	<table style="margin-left:auto; margin-right:auto; width:95%;" summary="">
	<tr><td class="regtext" style="vertical-align:top;">
		Enter the text:
	</td></tr><tr><td class="regtext" style="text-align:center;">
		<textarea name="enteredText" cols="100" rows="20"><%= 
			contentNum == 0 ? "" : Utils.toValidTextbox(contentData) %></textarea>
	</td></tr>
	<tr><td class="regtext" style="padding-top:20px; color:green;">
	 	Insert a hyperlink to content #5 in this chapter as follows:
		<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span 
				class="boldtext" style="font-family:Courier;"><%= 
				TextContent.LINK_OPEN1 %>5<%=
				TextContent.LINK_OPEN2 %>diastereomers<%= 
				TextContent.LINK_CLOSE %></span>
	 	</p><p>Insert a hyperlink to content #22 in chapter 14 as follows:
		</p><p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span
				class="boldtext" style="font-family:Courier;"><%= 
				TextContent.LINK_OPEN1 %>14.22<%=
				TextContent.LINK_OPEN2 %>NMR<%= 
				TextContent.LINK_CLOSE %>
		</p>
	</td></tr>
	</table>

<% } else if (contentType == IMAGE_URL) { %>

	<form name="contentForm" action="saveContent.jsp" method="post" accept-charset="UTF-8">
		<input type="hidden" name="chapNum" value="<%= chapNum %>"  />
		<input type="hidden" name="contentNum" value="<%= contentNum %>"  />
		<input type="hidden" name="contentType" value="<%= contentType %>"  />
		<input type="hidden" name="origContentType" value="<%= origType %>"  />
		<input type="hidden" name="contentData" value="<%= 
				Utils.toValidHTMLAttributeValue(contentData) %>" />
		<input type="hidden" name="brandNew" value="<%= brandNew %>"  />
		<input type="hidden" name="cancelling" value="false"  />
	<table style="margin-left:auto; margin-right:auto; width:95%;" summary="">
	<tr><td class="regtext" style="vertical-align:top;">
		Enter the image's URL:
	</td></tr><tr><td class="regtext" style="text-align:center;">
		<input type="text" name="enteredText" size="100" value="<%= 
			contentNum == 0 ? "" : Utils.toValidTextbox(contentData) %>" />
	</td></tr>
	<tr><td class="regtext" style="vertical-align:top;">
		Caption (optional):
	</td></tr><tr><td class="regtext" style="text-align:center;">
		<input type="text" name="caption" size="100" value="<%= 
			contentNum == 0 ? "" : Utils.toValidTextbox(content.getCaption()) %>" />
	</td></tr>
	</table>

<% } else if (contentType == ACE_Q) { 
	QuestionBank qstore = null;
	QSet qSet = null;
	Question[] questions = null;
	QSetDescr descr = null;
	Topic[] topics = null;
	int qSetNumInit = 0;
	String keywords = "";
	String message = null;
	final String instructorId = book.getOwnerId();

	// depending on view type, set how database questions will be displayed
	if (viewType == TOPIC) {
		synchronized (session) {
			session.removeAttribute("chset");
			qstore = (QuestionBank) session.getAttribute("qbank");
		} // synchronized
		if (qstore == null) {
			qstore = new QuestionBank(instructorId);
			synchronized (session) {
				session.setAttribute("qbank", qstore);
			} // synchronized
		} // if need to get list of topics
		topics = qstore.getTopics();
		synchronized (session) {
			qSet = (QSet) session.getAttribute("qSet");
		} // synchronized
		qSetId = MathUtils.parseInt(request.getParameter("qSetIdSelector"));
		if ((qSet == null && qSetId != 0)
				|| (qSet != null && !Utils.among(qSetId, 0, qSet.getQSetId()))) {
			qSet = new QSet(qSetId, instructorId);
			synchronized (session) {
				session.setAttribute("qSet", qSet);
			} // synchronized
		} else if (qSet != null && qSetId == 0) {
			qSet = null;
			synchronized (session) {
				session.removeAttribute("qSet");
			} // synchronized
		} // if need new qSet or need to remove old one
		if (qSet != null) {
			questions = qSet.getQuestions();
			descr = qSet.getQSetDescr();
		} // if have qSet in hand
		qSetNumInit = MathUtils.parseInt(request.getParameter("qSetNum"));
	} else { // viewType == KEYWORD
		KeywordSet kwset;
		keywords = request.getParameter("keywords");
		// Utils.alwaysPrint("loadContent.jsp: keywords = ", keywords);
		if (keywords == null) keywords = "";
		synchronized (session) {
			kwset = (KeywordSet) session.getAttribute("kwset");
			if (kwset == null || !kwset.getKeywords().equals(keywords)) {
				try {
					// Utils.alwaysPrint("loadContent.jsp: creating new KeywordSet.");
					kwset = new KeywordSet(keywords, instructorId);
					session.setAttribute("kwset", kwset);
				} catch (DBException e) {
					message = e.getMessage();
				} // try
			} // if should get a new set of questions by keywords
		} // synchronized
		if (kwset != null) {
			questions = kwset.getQuestions();
			if (Utils.isEmpty(questions) && !Utils.isEmpty(keywords)) {
				message = "The search yielded no questions.";
			}
			/* Utils.alwaysPrint("loadContent.jsp: found ", questions.length,
					" question(s)."); /**/
			qSet = (QSet) kwset;
		} // if there's a set of questions by keywords
	} // if viewType
%>
	<script type="text/javascript">
	// <!-- >

	<% if (viewType == TOPIC) { %>

	var firstLoad = true;

	function loadAllQSets() {
		var form = document.contentForm;
		var topicNumSel = form.topicOrChapSelector.selectedIndex;
		if (topicNumSel === 0) {
			form.qSetIdSelector.selectedIndex = 0;
			openQSet();
			return;
		}
		var out = new String.builder().
				append('<select name="qSetIdSelector" size="1" '
					+ 'onchange="openQSet();" '
					+ 'style="width:375px;"><option value="0">'
					+ 'No question set selected');
		<% for (int topicNum = 1; topicNum <= topics.length; topicNum++) { %>
			if (topicNumSel === <%= topicNum %>) {
				<% final QSetDescr[] qSetDescrs =
						qstore.getQSetDescrs(topicNum);
				for (int qSetNum = 1; qSetNum <= qSetDescrs.length; qSetNum++) { %>
					out.append('<option value="<%=
							qSetDescrs[qSetNum - 1].id %>"');
					<% if (qSetId == qSetDescrs[qSetNum - 1].id 
							|| (qSetDescrs.length == 1 && qSetNum == 1)) { %>
						out.append('<%= SELECTED %>');
					<% } // if should select %>
					out.append('><%= Utils.toValidJS(Utils.toPopupMenuDisplay(
							qSetDescrs[qSetNum - 1].name)) %>');
				<% } %>
				if (<%= qSetDescrs.length %> === 1) out.append(' [only set]');
			} // if the topic is selected
		<% } // for each topic %>
		out.append('<\/select>');
		setInnerHTML('qSetsOptions', out.toString());
		firstLoad = false;
		if (form.qSetIdSelector.selectedIndex !== 0) {
			openQSet();
		}
	} // loadAllQSets()

	<% } // if viewType %>

	function openQSet() {
		var form = document.contentForm;
		var qSetId = (form.qSetIdSelector ? form.qSetIdSelector.value : 0);
		if (<%= viewType != TOPIC %> || qSetId != <%= qSetId %>) { // do not change to !==
			reloadMe();
		}
	} // openQSet()

	function reloadMe() {
		var proceed = doBeforeReload();
		if (proceed) {
			document.contentForm.action = 'loadContent.jsp';
			document.contentForm.submit();
		}
	} // reloadMe()

	function doBeforeReload() {
		var proceed = true;
		<% if (viewType == KEYWORD) { %>
			if (isWhiteSpace(document.contentForm.keywords.value)
					&& URI.indexOf('&viewType=') === 0) {
				alert('Please enter one or more keywords in the space provided.');
				proceed = false;
			} // if no keywords
		<% } else { %>
			document.contentForm.qSetNum.value =
					document.contentForm.topicOrChapSelector.value;
		<% } // if viewType %>
		return proceed;
	} // doBeforeReload()

	function setApplyButton() {
		var form = document.contentForm;
		var buttonSelected = false;
		if (form.qChecker) {
			for (var bNum = 0; bNum < form.qChecker.length; bNum++) { // <!-- >
				if (form.qChecker[bNum].checked) {
					buttonSelected = true;
					break;
				} // if button is checked
			} // for each radio button
		} // if the buttons exist
		if ((<%= viewType == TOPIC %> && <%= qSetId != 0 %> && !buttonSelected)
				|| <%= viewType == KEYWORD && "".equals(keywords) %>) {
			hideApplyButton();
		} else {
			showApplyButton();
		}
	} // setApplyButton()

	function hideApplyButton() {
		clearInnerHTML('applyButton');
	} // hideApplyButton()

	function showApplyButton() {
		setInnerHTML('applyButton', '<%= Utils.toValidJS(makeButton(
				"Apply" + (qSetId == 0 ? "" : " selected"), "chooseSelected();")) %>');
	} // showApplyButton()

	// -->
	</script>
	<form name="contentForm" action="saveContent.jsp" method="post">
		<input type="hidden" name="chapNum" value="<%= chapNum %>"  />
		<input type="hidden" name="contentNum" value="<%= contentNum %>"  />
		<input type="hidden" name="contentType" value="<%= contentType %>"  />
		<input type="hidden" name="origContentType" value="<%= origType %>"  />
		<input type="hidden" name="contentData" value="<%= 
				Utils.toValidHTMLAttributeValue(contentData) %>" />
		<input type="hidden" name="qSetNum" value=""  />
		<input type="hidden" name="brandNew" value="<%= brandNew %>"  />
		<input type="hidden" name="cancelling" value="false"  />
	<table style="width:95%;" summary="">
	<tr>
	<td colspan="2" class="regtext" style="margin-left:auto; margin-right:auto;">
		<table class="regtext">
		<tr><td style="vertical-align:top;">
			Caption (optional):
		</td></tr><tr><td style="text-align:center;">
			<input type="text" name="caption" size="100" value="<%= 
					contentNum == 0 ? "" : Utils.toValidTextbox(content.getCaption()) %>" />
		</table>
	</td>
	</tr>
	<tr>
	<td class="boldtext big" style="margin-left:auto; margin-right:auto;">
		Database Questions
	</td>
	<td class="boldtext" style="text-align:right;
			margin-left:auto; margin-right:auto; margin-top:10px;">
		Organize questions by
		<select name="viewType" onchange="reloadMe();">
			<option value="<%= TOPIC %>" <%= viewType == TOPIC
					? SELECTED : "" %>>topic</option>
			<option value="<%= KEYWORD %>" <%= viewType == KEYWORD
					? SELECTED : "" %>>keywords</option>
		</select>
	</td>
	</tr>
	</table>
	<table id="theTable" style="width:100%;
			background-color:#f6f7ed; margin-left:auto; margin-right:auto;
			border-collapse:collapse; text-align:left;" summary="">
	<% if (viewType == TOPIC) { %>
		<tr><td style="padding-left:10px; padding-top:5px; padding-right:10px;">
			<b>Select topic</b><br />
			<select name="topicOrChapSelector" size="1"
					onchange="loadAllQSets();" style="width:375px">
				<option value="0">
					No topic selected
				</option>
				<% for (int topicNum = 1; topicNum <= topics.length; topicNum++)  {
					final Topic topic = topics[topicNum - 1]; %>
					<option value="<%= topicNum %>"
							<%= topicNum == qSetNumInit ? SELECTED : "" %>>
						<%= Utils.toPopupMenuDisplay(topic.name) %>
					</option>
				<% } // for each topic %>
			</select>
		</td></tr>
		<tr><td style="padding-left:10px; padding-right:10px;">
			<b>Select question set</b><br/>
			<div id="qSetsOptions">
			<select name="qSetIdSelector" size="1" onchange="openQSet();" style="width:375px;">
				<option value="0">No question set selected</option>
			</select>
			</div>
		</td></tr>
		<tr><td id="selectHow" style="padding-left:10px; padding-right:10px;">
			<% if (qSetId == 0) { %>
				<b>or enter a question ID number</b>:
				<input type="text" name="qId" size="4" value="<%= 
						MathUtils.parseInt(contentData) == 0 ? "" : contentData %>" />
			<% } // if there's a chosen qSet %>
		</td></tr>
	<% } else { // viewType == KEYWORD %>
		<tr><td colspan="3"
				style="padding-left:10px; padding-top:5px; padding-right:10px;">
			<% if (message != null) { %>
				<p><span class="boldtext" style="color:red;">
					<%= message %>
				</span>
				</p>
			<% } %>
			<b>Enter keywords:</b>
		</td></tr>
		<tr><td colspan="2"
				style="padding-left:10px; padding-top:5px; padding-right:10px;">
			<textarea name="keywords" rows="3"
					style="width:300px;"><%= keywords != null
							? Utils.toValidTextbox(keywords)
							: "" %></textarea>
		</td>
		<td style="padding-left:10px; padding-top:5px; padding-right:10px;
				vertical-align:top;">
			<%= makeButton("Get questions", "openQSet();") %>
		</td>
		</tr>
		<tr>
		<td colspan="3" style="padding-left:10px; padding-top:5px;
				padding-right:10px; color:green;">
			An example of a search for more than one keyword:
		</td>
		</tr>
		<tr>
		<td colspan="3" style="padding-left:30px; padding-right:10px; color:green;">
			[tosylate or alkyl halide] and [substitution or elimination]
		</td>
		</tr>
		<tr>
		<td colspan="3" style="padding-left:10px; padding-right:10px; color:green;">
			Note the use of square brackets instead of parentheses.
		</td>
		</tr>
	<% } // if viewType %>
	</table>
	<% if (Utils.isEmpty(questions)) { %>
	<table style="width:100%; padding-left:30px; padding-right:30px;" summary="">
	<tr><td id="dbQuestions" class="regtext" style="vertical-align:middle;">
		<p align="center"><img id="image" 
				src="<%= pathToRoot %>images/ace_nonanim.gif" alt="set"/>
		<br /><span class="boldtext"> <%= viewType == KEYWORD
				? "Enter one or more keywords (in English) above."
				: "Select a topic and question set from above." %>
		</span></p>
	</td></tr>
	</table>
	<% } else { %>
	<table class="regtext" style="width:95%; margin-left:auto; 
			margin-right:auto; border-collapse:collapse;" summary="">
	<tr><td style="padding-bottom:10px;" id="dbQuestions">
		<table class="regtext" style="width:100%; text-align:center;
				margin-left:auto; margin-right:auto; 
				background-color:#f6f7ed;" summary="">
		<% if (viewType == TOPIC) { %>
			<tr><td class="boldtext" colspan="2" 
					style="vertical-align:top; padding-top:10px;">
			<% final String topicName = (descr != null && descr.topicName != null 
					? descr.topicName.replace("[null]", "") : ""); %>
			<%= Utils.toDisplay(topicName).trim() %>, <%= Utils.toDisplay(
					descr != null ? descr.name : "") %>
			</td></tr>
			<tr><td style="vertical-align:top; text-align:left;" colspan="2">
				<span class="boldtext">Remarks:</span>&nbsp;<span
						class="regtext"><%= descr == null 
									|| Utils.isEmpty(descr.remarks)
								? "[None]" : descr.remarks %></span>
			</td></tr>
			<tr><td style="vertical-align:top; text-align:left;" colspan="2">
				<div class="boldtext">Statement to be repeated for every question:</div>
				<span class="regtext">
				<%= descr == null || Utils.isEmpty(descr.header)
						? "[None]" : Utils.toDisplay(descr.header) %>
				</span>
			</td></tr>
		<% } // if viewType %>
		</table>
	</td></tr>
	<tr><td id="dbQuestions2">
		<table class="regtext" style="width:100%; border-collapse:collapse; margin-left:auto;
				margin-right:auto;" summary="">
		<% if (!Utils.isEmpty(questions)) { %>
			<tr><td style="border-bottom:solid; border-width:1px;
					border-color:#49521B;"></td></tr>
		<%	for (int qNum = 1; qNum <= questions.length; qNum++) {
				final Question oneQ = questions[qNum - 1];
				final int qId = oneQ.getQId();
				final String rowColor = (qNum % 2 == 0 ? "whiterow" : "greenrow");
				final boolean hideQ = oneQ.hide();
		%>
				<tr class="<%= rowColor %>">
				<td class="regtext" style="padding-left:10px; padding-right:10px;
						padding-top:10px; border-left:solid; border-right:solid;
						border-color:#49521B; border-width:1px; vertical-align:middle;">
					<input type="radio" id="qChecker" name="qChecker" value="<%= qId %>" <%= 
								hideQ ? "disabled=\"disabled\"" : "" %> 
								onchange="showApplyButton();" />
					(<b><%= qNum %></b>)
		<% 			if (hideQ) { %>
						This question is not yet available.
						</td></tr>
		<% 				continue;
					} // if hidden from local authors
					if (oneQ.isModified()) {
						%><span class="modified">Modified</span><%
					} else if (oneQ.isNew()) {
						%><span class="new">New</span><%
					}
					final String majorQTypeStr = oneQ.getQTypeDescription(user)
							+ ", #" + qId;
					final String qBook = oneQ.getBook();
					final String qRemarks = oneQ.getRemarks();
		%>
					<span class="boldtext">(<%= majorQTypeStr %>,
						<%= "Other".equals(qBook)
								? "by " + oneQ.getChapter()
								: qBook + ("[None]".equals(qRemarks) ? "" : " " + qRemarks)
						%>)</span>
					<%= oneQ.getDisplayStatement() %>
				</td>
				</tr>
	<!-- display Q figure -->
		<% 		final int numFigures = oneQ.getNumFigures();
				if (numFigures > 0) { %>
					<tr class="<%= rowColor %>">
					<td class="regtext" style="vertical-align:top;
							padding-left:10px; padding-right:10px;
							padding-bottom:10px; border-left:solid;
							border-right:solid; border-color:#49521B;
							border-width:1px;">
		<% 				final Figure figure = oneQ.getFigure(1);
						final String[] figData = (figure.isSynthesis()
									? figure.getDisplayData(
											Synthesis.getRxnsDisplayPhrases(user))
									: figure.getDisplayData());
						final boolean useMView = !figure.isJmol() && !figure.isImage(); %>
						<br/>
						<table summary="">
		<% 				if (numFigures > 1 || useMView) { %>
							<tr>
							<td <%= figure.isReaction() ? "colspan=\"2\"" : "" %>>
								<table style="width:100%;" summary="">
								<tr>
									<td class="boldtext" style="width:100%;">
		<%							if (numFigures > 1) { %>
										Fig. 1 of <%= numFigures %>
		<% 							} // more than one figure %>
									</td>
		<%						if (useMView) { %>
									<td class="boldtext" style="text-align:right; 
											font-style:italic;">
									</td>
		<%						} %>
								</tr>
								</table>
							</td>
							</tr>
		<%				} // if figure has text above it
						if (figure.isJmol()) {
							jmolNum++; %>
							<tr><td>
							<script type="text/javascript">
								// <!-- >
								setJmol(<%= jmolNum %>, 
										'<%= Utils.toValidJS(
											figData[Figure.STRUCT]) %>',
										'white', 250, 250,
										'<%= Utils.toValidJS(
											figData[Figure.JMOL_SCRIPTS]) %>');
								// -->
							</script>
							</td></tr>
		<%				} else if (!figure.isImage()) {
							mviewNum++;
							final String figIdStr = 
									Utils.toString("mview", mviewNum);
							if (figure.isReaction()) { %>
								<tr><td>
									<table class="whiteTable" summary="">
									<tr><td id="<%= figIdStr %>">
		<%					} else { %>
								<tr><td id="<%= figIdStr %>" class="whiteTable">
		<%					} %>
								<%= figure.getImage(pathToRoot, user.prefersPNG(), 
										oneQ.getQFlags(), figIdStr) %>
								</td>
		<% 					if (figure.isReaction()) { 
								final String above = Utils.toDisplay(figData[Figure.RXN_ABOVE]);
								final String below = Utils.toDisplay(figData[Figure.RXN_BELOW]);
								final int arrowSize = 36; %>
								<td style="width:85px; vertical-align:middle;
										background-color:#FFFFFF; text-align:center;">
									<%@ include file="/includes/reactionArrow.jsp.h" %>
								</td>
								</tr>
								</table>
								</td></tr>
		<%					} else if (figure.isSynthesis()) { %>
								<%= figData[Synthesis.RXNID] %>
		<% 					} // if synthesis figure
						} else { // image %>
							<tr><td>
								<img class="whiteTable" src="<%= pathToRoot
											+ figure.bufferedImage %>"
										alt="picture"
										onload="fixImageSize(this);"
										style="visibility:hidden;" />
							</td></tr>
		<% 				} // figure.type %>
						</table>
					</td>
					</tr>
		<% 		} // numFigures %>
	<!-- display Q data -->
		<% 		if ((oneQ.isChoice() || oneQ.isChooseExplain() || oneQ.isRank()) 
						&& oneQ.getNumQData(Question.GENERAL) > 0) { %>
					<tr class="<%= rowColor %>">
					<td class="regtext" style="vertical-align:top;
							padding-left:10px; padding-right:10px; padding-bottom:10px;
							border-left:solid; border-right:solid; border-color:#49521B;
							border-width:1px;">
						<table style="padding-left:10px;" summary="">
		<% 				final QDatum[] qData = oneQ.getQData(Question.GENERAL);
						if (qData != null) {
							final boolean chemFormatting = oneQ.chemFormatting();
							for (int qdNum = 0; qdNum < qData.length; qdNum++) {
								final QDatum qDatum = qData[qdNum];
								if (qDatum != null) {
		%>
									<tr><td class="regtext">
										<b>Option <%= qDatum.serialNo %>.</b>
										<%= qDatum.toShortDisplay(chemFormatting) %>
									</td></tr>
		<% 						} // if qDatum is not null
							} // for each Qdatum qdNum
						} else {
							Utils.alwaysPrint("dbQuestions.jsp: "
									+ "Couldn't get Q data from question");
						} %>
						</table>
					</td>
					</tr>
		<% 		} // if choice or rank %>
		<%	} // for each Q qNum - 1 in Qset %>
			<tr><td style="border-top:solid; border-width:1px; border-color:#49521B;"></td></tr>
		<% } else { %>
			The question set is empty.
		<% } // if questions is not null %>
		</table>
	</td></tr>
	</table>
	<% } // if questions is not null or empty  %>

<% } else if (contentType == MARVIN) { 
		final boolean showNoH = content.showNoH(); 
		final boolean showHeteroH = content.showHeteroH(); 
		final boolean showAllH = content.showAllH(); 
		final boolean showAllC = content.showAllC(); 
%>
	<form name="contentForm" action="saveContent.jsp" method="post">
		<input type="hidden" name="chapNum" value="<%= chapNum %>" />
		<input type="hidden" name="contentNum" value="<%= contentNum %>" />
		<input type="hidden" name="contentType" value="<%= contentType %>" />
		<input type="hidden" name="origContentType" value="<%= origType %>" />
		<input type="hidden" name="contentData" value="<%= 
				Utils.toValidHTMLAttributeValue(contentData) %>" />
		<input type="hidden" name="contentExtra" value="<%= 
				Utils.toValidHTMLAttributeValue(contentExtra) %>" />
		<input type="hidden" name="brandNew" value="<%= brandNew %>" />
		<input type="hidden" name="cancelling" value="false" />
	<table style="margin-left:auto; margin-right:auto; width:445px;" summary="">
	<td class="regtext" style="margin-left:auto; margin-right:auto;">
		<table class="regtext">
		<tr><td style="vertical-align:top;">
			Caption (optional):
		</td></tr><tr><td style="text-align:center;">
			<input type="text" name="caption" size="100" value="<%= 
					contentNum == 0 ? "" 
					: Utils.toValidTextbox(content.getCaption()) %>" />
		</table>
	</td>
	<tr><td class="boldtext" style="text-align:right; padding-left:10px; 
			padding-right:10px; padding-top:10px; font-style:italic;">
		MarvinJS&trade;
	</td></tr>
	<tr><td style="text-align:center; padding-left:10px; padding-right:10px;">
		<table class="whiteTable" summary=""><tr>
			<td>
			<div id="<%= APPLET_NAME %>">
			<script type="text/javascript">
				// <!-- >
				startMarvinJS('<%= Utils.toValidJS(contentData) %>', 
						MARVIN, 
						<%= Utils.isEmpty(contentExtra) ? 0 : contentExtra %>, 
						'<%= APPLET_NAME %>', '<%= pathToRoot %>');
				// -->
			</script>
			</div>
			</td>
		</tr></table>
	</td>
	</tr>
	<tr><td class="regtext" style="text-align:left; padding-top:10px;">
		Display options:
		<table>
		<tr><td>
			<input type="checkbox" value="true" name="howShowHydrogens" <%= 
					showNoH || showHeteroH || showAllH || showAllC ? CHECKED : "" %> /> 
		</td><td>show 
			<select name="allNo">
				<option value="<%= Question.SHOWNOH %>"
						<%= showNoH ? SELECTED : "" %>>explicit H atoms only</option>
				<option value="<%= Question.SHOWHETEROH %>"
						<%= showHeteroH ? SELECTED : "" %>>heteroatom H atoms</option>
				<option value="<%= Question.SHOWALLH %>"
						<%= showAllH ? SELECTED : "" %>>all H atoms</option>
				<option value="<%= Question.SHOWALLH | Question.SHOWALLC %>"
						<%= showAllC ? SELECTED : "" %>>all H and C atoms</option>
			</select>
		</td></tr>
		<tr><td>
			<input type="checkbox" name="withLonePairs"
					<%= content.showLonePairs() ? CHECKED : "" %> /> 
		</td><td>show lone pairs
		</td></tr>
		<tr><td>
			<input type="checkbox" value="true" name="withMapping"
					<%= content.showMapping() ? CHECKED : "" %> /> 
		</td><td>show mapping
		</td></tr>
		<tr><td>
			<input type="checkbox" value="true" name="withRSLabels"
					<%= content.showRSLabels() ? CHECKED : "" %> /> 
		</td><td>show R,S labels
		</td></tr>
		</table>

<% } else if (contentType == LEWIS) { 
%>
	<form name="contentForm" action="saveContent.jsp" method="post">
		<input type="hidden" name="chapNum" value="<%= chapNum %>"  />
		<input type="hidden" name="contentNum" value="<%= contentNum %>"  />
		<input type="hidden" name="contentType" value="<%= contentType %>"  />
		<input type="hidden" name="origContentType" value="<%= origType %>"  />
		<input type="hidden" name="contentData" value="<%= 
				Utils.toValidHTMLAttributeValue(contentData) %>" />
		<input type="hidden" name="contentExtra" value="" />
		<input type="hidden" name="brandNew" value="<%= brandNew %>"  />
		<input type="hidden" name="cancelling" value="false"  />
	<table style="margin-left:auto; margin-right:auto; width:445px;" summary="">
	<td class="regtext" style="margin-left:auto; margin-right:auto;">
		<table class="regtext">
		<tr><td style="vertical-align:top;">
			Caption (optional):
		</td></tr><tr><td style="text-align:center;">
			<input type="text" name="caption" size="100" value="<%= 
					contentNum == 0 ? "" : Utils.toValidTextbox(content.getCaption()) %>" />
		</table>
	</td>
	<tr><td class="boldtext" style="text-align:right; padding-left:10px; 
			padding-right:10px; padding-top:10px; font-style:italic;">
		Lewis JS&trade;
	</td></tr>
	<tr><td style="text-align:center; padding-left:10px; padding-right:10px;">
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
 	</td></tr>

<% } else if (Utils.among(contentType, IMAGE, MOVIE, JMOL)) { %>

	<table style="width:95%; margin-right:auto; margin-left:auto;" summary="">
	<tr><td class="boldtext" style="text-align:left;">
		File containing the <%= uploadDescrip %>:
	</td><td>
		<form name="fileupload" action="<%= Utils.among(contentType, IMAGE, MOVIE)
				? "imgUpload.jsp" : "calcUpload.jsp" %>" method="post" 
				enctype="multipart/form-data">
			<input type="hidden" name="chapNum" value="<%= chapNum %>"  />
			<input type="hidden" name="contentNum" value="<%= contentNum %>" />
			<input type="hidden" name="contentType" value="<%= contentType %>"  />
			<input type="file" name="srcFile" size="30" />
			<input type="button" value="Upload" onclick="upload();" />
		</form>
 	</td></tr>
	</table>

	<form name="contentForm" action="saveContent.jsp" method="post">
		<input type="hidden" name="chapNum" value="<%= chapNum %>"  />
		<input type="hidden" name="contentNum" value="<%= contentNum %>" />
		<input type="hidden" name="contentType" value="<%= contentType %>"  />
		<input type="hidden" name="origContentType" value="<%= origType %>"  />
		<input type="hidden" name="srcFile" 
				value="<%= Utils.toValidHTMLAttributeValue(srcFile) %>" />
		<input type="hidden" name="contentData" value="<%= 
				Utils.toValidHTMLAttributeValue(contentData) %>" />
		<input type="hidden" name="contentExtra" value="<%= 
				Utils.toValidHTMLAttributeValue(contentExtra) %>" />
		<input type="hidden" name="brandNew" value="<%= brandNew %>"  />
		<input type="hidden" name="cancelling" value="false"  />
	<table style="width:95%; margin-right:auto; margin-left:auto;" summary="">
		<tr><td class="regtext" style="text-align:left;">
		<% if (contentType == IMAGE) { %>
			<p style="text-align:center";>
			<% if (srcFile != null) { %>
				<img src="<%= pathToRoot + srcFile %>" alt="picture" 
						onload="fixImageSize(this);" /></p>
				</p><p>
				<table class="regtext">
				<tr><td style="vertical-align:top;">
					Caption (optional):
				</td></tr><tr><td style="text-align:center;">
					<input type="text" name="caption" size="100" value="<%= 
							contentNum == 0 ? "" : Utils.toValidTextbox(content.getCaption()) %>" />
				</table>
			<% } else { %>
				No image loaded.
			<% } // if there's an image to show %>
			</p>
			</tr>
			<tr><td style="color:green; text-align:left;">
				To display MOs, upload a file in the .mo format.
			</td>
		<% } else if (contentType == MOVIE) { %>
			<p style="text-align:center";>
			<% if (srcFile != null) { %>
				<object width="400" height="300">
				<param name="SRC" value="<%= pathToRoot + srcFile %>" />
				<param name="AUTOPLAY" value="false" />
				<param name="SCALE" value="aspect" />
				<embed src="<%= pathToRoot + srcFile %>" width="400" height="300" 
						autostart="false" scale="aspect"/>
				</object>
				</p><p>
				<table class="regtext">
				<tr><td style="vertical-align:top;">
					Caption (optional):
				</td></tr><tr><td style="text-align:center;">
					<input type="text" name="caption" size="100" value="<%= 
							contentNum == 0 ? "" : Utils.toValidTextbox(content.getCaption()) %>" />
				</table>
			<% } else { %>
				No movie loaded.
			<% } // if there's a movie to show %>
			</p>
			</tr>
		<% } else if (contentType == JMOL) { %>
			<% if (contentData != null) { 
				final String[] allJmolScripts = (contentNum != 0 && content.isJmol()
						? content.getJmolScripts() : new String[2]); 
			%>
				<p style="text-align:center;">
				<script type="text/javascript">
					// <!-- >
					setJmol(1, '<%= Utils.toValidJS(contentData) %>', 
							'#f6f7ed', 400, 250,
							'<%= Utils.toValidJS(allJmolScripts[0]) %>');
					// -->
				</script>
				</p><p>
				<table class="regtext">
				<tr><td style="vertical-align:top;">
					Caption (optional):
				</td></tr><tr><td style="text-align:center;">
					<input type="text" name="caption" size="100" value="<%= 
							contentNum == 0 ? "" : Utils.toValidTextbox(content.getCaption()) %>" />
				</table>
				</p>
				<p>Enter any <a href="http://wiki.jmol.org/index.php/Scripting"
				target="window2">Jmol scripts</a> (semicolon-separated) that you would 
				like <a href="http://jmol.sourceforge.net" target="window2">Jmol</a> 
				to run when it starts 
				(<a href="javascript:warn('scripts');"><b>warning</b></a>).
				</p>
				<p style="text-align:center;"><textarea name="addlData1" id="addlData1"
						style="overflow:auto;" cols="50" rows="4"><%= 
								Utils.toValidTextbox(allJmolScripts[0]) %></textarea>
				</p>
				<p>Enter any <a href="http://jmol.sourceforge.net/jslibrary/"
				target="window2">Jmol Javascript library</a> commands 
				(semicolon-separated) that you would like this page to run when the 
				<a href="http://jmol.sourceforge.net" target="window2">Jmol</a> 
				applet starts 
				(<a href="javascript:warn('Javascript%20library%20commands');"><b>warning</b></a>).
				</p>
				<p style="text-align:center;"><textarea name="addlData2" id="addlData2"
						style="overflow:auto;" cols="50" rows="4"><%= 
								Utils.toValidTextbox(allJmolScripts[1]) %></textarea>
				</p>
			<% } else { %>
				No data loaded.
			<% } // if there's data to show %>
 		<% } // if contentType %>
 	</td></tr>

<% } // if contentType %>

	</table>
	</form>
</div>
<div id="loadContentsFooter">
	<table style="margin-left:auto; margin-right:auto; width:445px;" summary="">
	<tr><td style="text-align:center;">
		<table style="margin-right:auto; margin-left:auto;" summary="">
		<tr>
		<td>
			<% if (contentType == ACE_Q) { %>
				<div id="applyButton">
				</div>
			<% } else { %>
				<%= makeButton("Apply Changes", "submitIt();") %>
			<% } // if contentType %>
		</td>
		<td><%= makeButton("Cancel", "cancelMe();") %></td>
		</tr>
		</table>
	</td></tr>
	</table>
</div>
</body>
</html>


