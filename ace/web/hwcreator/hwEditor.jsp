<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.assgts.AssgtQGroup,
	com.epoch.chem.MolString,
	com.epoch.db.HWRead,
	com.epoch.exceptions.DBException,
	com.epoch.lewis.LewisMolecule,
	com.epoch.qBank.Figure,
	com.epoch.qBank.QDatum,
	com.epoch.qBank.QSetDescr,
	com.epoch.qBank.Question,
	com.epoch.qBank.Topic,
	com.epoch.session.BookSet,
	com.epoch.session.ChapterSet,
	com.epoch.session.HWCreateSession,
	com.epoch.session.KeywordSet,
	com.epoch.session.QuestionBank,
	com.epoch.session.QSet,
	com.epoch.synthesis.Synthesis,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.List"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../";
	if (!Utils.among(role, User.ADMINISTRATOR, User.INSTRUCTOR) && !isTA) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	request.setCharacterEncoding("UTF-8");

	final String SELECTED = " selected=\"selected\" ";
	final int TOPIC = 0;
	final int BOOK = 1;
	final int KEYWORD = 2;

	final String book = course.getBook();
	final String viewTypeStr = request.getParameter("viewType");
	final int viewType = (viewTypeStr != null ? MathUtils.parseInt(viewTypeStr)
			: book == null || !Utils.among(book, "Bruice", "Wade") ? TOPIC 
			: BOOK);
	final int hwNum = MathUtils.parseInt(request.getParameter("hwNum"));
	final boolean newHW = hwNum == 0;
	// Utils.alwaysPrint("hwEditor.jsp: hwNum = ", hwNum);

	// for coinstructors, need owner of course
	final String instructorId = course.getOwnerId();

	QuestionBank qstore = null;
	QSet qSet = null;
	Question[] questions = null;
	QSetDescr descr = null;
	Topic[] topics = null;
	String[] chapterNums = null;
	String[] chapterNames = null;
	String chapterName = "";
	int qSetId = 0;
	int chapNumInit = 0;
	String keywords = "";
	String message = null;

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
		qSetId = MathUtils.parseInt(request.getParameter("qSetId"));
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
		chapNumInit = MathUtils.parseInt(request.getParameter("chapNum"));
	} else if (viewType == BOOK) {
		BookSet bkset;
		synchronized (session) {
			session.removeAttribute("qSet");
			bkset = (BookSet) session.getAttribute("bkset");
			if (bkset == null) {
				bkset = new BookSet(book, instructorId);
				session.setAttribute("bkset", bkset);
			} // if need to get chapter names and numbers
		} // synchronized
		chapNumInit = MathUtils.parseInt(request.getParameter("chapNum"));
		chapterNums = bkset.getChapters();
		chapterNames = bkset.getChapterNames(book);
		try {
			chapterName = chapterNums[chapNumInit - 1];
		} catch (Exception e) { } // ArrayOutOfBounds, NullPointer
		ChapterSet chset;
		synchronized (session) {
			chset = (ChapterSet) session.getAttribute("chset");
			if (chset == null || !chset.getChapter().equals(chapterName)) {
				chset = new ChapterSet(book, chapterName, instructorId);
				session.setAttribute("chset", chset);
			} // if should get a new chapter
		} // synchronized
		if (chset != null) {
			questions = chset.getQuestions();
			qSet = (QSet) chset;
		} // if there's a chapter to load
		/* Utils.alwaysPrint("hwEditor.jsp: chapterName = ",
				chapterName, ", chapNumInit = ", chapNumInit); /**/
	} else { // viewType == KEYWORD
		KeywordSet kwset;
		keywords = request.getParameter("keywords");
		// Utils.alwaysPrint("hwEditor.jsp: keywords = ", keywords);
		if (keywords == null) keywords = "";
		synchronized (session) {
			kwset = (KeywordSet) session.getAttribute("kwset");
			if (kwset == null || !kwset.getKeywords().equals(keywords)) {
				try {
					// Utils.alwaysPrint("hwEditor.jsp: creating new KeywordSet.");
					kwset = new KeywordSet(keywords, instructorId);
					session.setAttribute("kwset", kwset);
				} catch (DBException e) {
					message = user.translate(e.getMessage());
				} // try
			} // if should get a new set of questions by keywords
		} // synchronized
		if (kwset != null) {
			questions = kwset.getQuestions();
			if (Utils.isEmpty(questions) && !Utils.isEmpty(keywords)) {
				message = user.translate("The search yielded no questions.");
			}
			/* Utils.alwaysPrint("hwEditor.jsp: found ", questions.length,
					" question(s)."); /**/
			qSet = (QSet) kwset;
		} // if there's a set of questions by keywords
	} // if viewType

	// now get the assignment that is being created
	HWCreateSession hwCreator;
	synchronized (session) {
		hwCreator = (HWCreateSession) session.getAttribute("hwCreator");
	}
	final String qNumsStr = request.getParameter("qNums");
	String assgtName = Utils.inputToCERs(request.getParameter("assgtName"));
	String remarks = Utils.inputToCERs(request.getParameter("remarks"));
	int numTries = MathUtils.parseInt(request.getParameter("numTries"));
	String allowedRxnCondns = request.getParameter("allowedRxnCondns");
	final Assgt assgt = hwCreator.assgt;
	if (assgt != null) { // shouldn't happen
		if (assgtName == null) assgtName = assgt.getName();
		if (remarks == null) remarks = assgt.getRemarks();
		if (numTries == 0) {
			numTries = (assgt.allowUnlimitedTries() 
					? Assgt.UNLIMITED : assgt.getMaxTries());
		}
		if (Utils.isEmpty(allowedRxnCondns)) {
			allowedRxnCondns = assgt.getAllowedRxnCondnsStr();
		} // if no chosenRxnConds
		final String ptsPerQGrpStr = request.getParameter("ptsPerQGrpStr");
		if (ptsPerQGrpStr != null) {
			final String[] ptsPerQGrpStrs = ptsPerQGrpStr.split("/");
			for (int grpNum = 1; grpNum <= hwCreator.getNumQGroups(); grpNum++) {
				final String pts = ptsPerQGrpStrs[grpNum - 1];
				hwCreator.setPts(grpNum, pts);
			} // for each question group
		} // if new point values for question groups are available
		final String dependencies = request.getParameter("dependencies");
		if (!Utils.isEmpty(dependencies)) {
			final String[] dependsOnStrs = dependencies.split(Assgt.DEP_PAIRS_SEP);
			int grpNum = 1;
			for (final String dependsOnStr : dependsOnStrs) {
				final int dependsOn = MathUtils.parseInt(dependsOnStr);
				hwCreator.setDependsOn(grpNum, dependsOn);
				grpNum++;
			} // for each question group
		} // if dependencies for question groups are available
	} // if assgt
	final boolean isViewed = assgt != null && HWRead.assignmentViewed(assgt.id);
	if (numTries == 0) numTries = 5;
	boolean hasBeenSaved = "true".equals(request.getParameter("hasBeenSaved"));

	// do the reentry action
	String hwErrorMessage = null;
	String footerErrorMessage = null;
	String saveMessage = null;
	String editAction = request.getParameter("editAction");
	if (!Utils.isEmpty(editAction)) {
		if ("addQ".equals(editAction) && !Utils.isEmpty(qNumsStr)) {
			// when chset has a value, qSet is the enclosing qSet of the chset
			// qNumsStr is of the form 1:3:4:
			final String[] qNums = qNumsStr.split(":");
			for (int qNumNum = 0; // qNum in a qSet or a chset
					qNumNum < qNums.length; qNumNum++) {
				final int qNum = MathUtils.parseInt(qNums[qNumNum]);
				// when chset is not null, still need to use enclosing qSet to
				// get the desired question
				final Question qToAdd = (qSet == null ? null
						: qSet.getQuestionBySerialNo(qNum));
				if (qToAdd == null) {
					Utils.alwaysPrint("hwEditor.jsp: qToAdd ", qNum, " is null.");
					continue;
				}
				final int qId = qToAdd.getQId();
				final String commonQstmt = (viewType == TOPIC && qSet != null
						? qSet.getQSetDescr().header : null);
				if (!Utils.isEmptyOrWhitespace(commonQstmt))
					qToAdd.setStatement(Utils.toString(commonQstmt, ' ', 
							qToAdd.getStatement()));
				final int addResult = hwCreator.addQuestion(qToAdd);
				if (addResult == HWCreateSession.OK) {
					/* Utils.alwaysPrint("hwEditor.jsp: qToAdd ", qNum,
							" with qId ", qId, " added successfully."); /**/
				} else hwErrorMessage = Utils.toString(
						addResult == HWCreateSession.ALREADY
							? user.translate("Question "
								+ "***1***  with ID number ***6*** "
								+ " is already in the assignment.",
								new int[] {qNum, qId})
						: addResult == HWCreateSession.QID_0
							? user.translate("Question ***1** "
								+ "has ID number 0; ACE cannot add it to "
								+ "the assignment.", qNum)
						: user.translate("For an unknown reason, "
								+ "ACE cannot add question ***1*** with "
								+ "ID number ***6*** to the assignment.",
								new int[] {qNum, qId}), 
						"<br/>");
			} // for each qNum qNumNum
		} else if ("addQById".equals(editAction)) {
			final int qId = MathUtils.parseInt(request.getParameter("qId"));
			final int addResult = hwCreator.addQuestion(qId);
			if (addResult == HWCreateSession.OK) {
				/* Utils.alwaysPrint("hwEditor.jsp: Question with qId ",
						qId, " added successfully."); /**/
			} else hwErrorMessage = Utils.toString(
					addResult == HWCreateSession.QID_0
						? user.translate("Question has ID number 0; "
								+ "ACE cannot add it to the assignment.")
					: addResult == HWCreateSession.ALREADY
						? user.translate("Question with ID number "
							+ "***6*** is already in the assignment.", qId)
					: addResult == HWCreateSession.CANNOTFINDQ
						? user.translate("ACE cannot retrieve "
							+ "question with ID number ***6***.", qId)
					: user.translate("For an unknown reason, "
							+ "ACE cannot add question with ID number "
							+ "***6*** to the assignment.", qId), 
					"<br/>");
		} else if ("removeQ".equals(editAction)) {
			int[] grpNums = new int[0];
			hwErrorMessage = "";
			final List<Integer> removedQIds = new ArrayList<Integer>();
			if (!Utils.isEmpty(qNumsStr)) {
				grpNums = Utils.stringToIntArray(qNumsStr.split(":"));
				for (int grpNumNum = grpNums.length; grpNumNum > 0; grpNumNum--) {
					removedQIds.addAll(hwCreator.removeQGroup(grpNums[grpNumNum - 1]));
				} // for each grpNumNum
			} // if there are groups to remove
			final String randIndicesStr = request.getParameter("randIndices");
			if (!Utils.isEmpty(randIndicesStr)) {
				final String[] randIndices = randIndicesStr.split(":");
				for (int randIndexNum = randIndices.length; 
						randIndexNum > 0; randIndexNum--) {
					final String[] randIndexParts = 
							randIndices[randIndexNum - 1].split("_");
					int qGrpNum = MathUtils.parseInt(randIndexParts[0]);
					if (Utils.contains(grpNums, qGrpNum)) {
						Utils.alwaysPrint("hwEditor.jsp: not removing Q ",
								randIndices[randIndexNum], " because group ",
								qGrpNum, " has already been removed.");
						continue;
					} // if groupNum is in list of removed grpNums
					// modify qGrpNum if preceding groups have been removed
					for (int removedGrpNum : grpNums) {
						if (removedGrpNum < qGrpNum) qGrpNum--;
						else break;
					} // for each removed group
					final int grpQNum = MathUtils.parseInt(randIndexParts[1]);
					removedQIds.add(Integer.valueOf(
							hwCreator.removeGroupQ(qGrpNum, grpQNum)));
				} // for each random Q to be removed
			} // if there are random Qs to remove
			if (hwCreator.haveResponses(removedQIds)) {
				hwErrorMessage += user.translate("ACE will irreversibly delete "
						+ "all responses to questions that you have removed from "
						+ "the assignment after you save your changes.");
			} // if there are responses to the removed questions
		} else if ("ungroupRandom".equals(editAction) && !Utils.isEmpty(qNumsStr)) {
			final String[] grpNums = qNumsStr.split(":");
			for (int grpNumNum = grpNums.length - 1; grpNumNum >= 0; grpNumNum--) {
				final int grpNum = MathUtils.parseInt(grpNums[grpNumNum]);
				hwCreator.ungroupRandom(grpNum);
			}
		} else if ("randQ".equals(editAction) && !Utils.isEmpty(qNumsStr)) {
			final int randCount = MathUtils.parseInt(request.getParameter("count"));
			final String[] qNums = qNumsStr.split(":"); // gives 0-based indices
			final List<Integer> randGroup = new ArrayList<Integer>();
			for (int randNum = 0; randNum < qNums.length; randNum++) {
				randGroup.add(Integer.decode(qNums[randNum]));
			}
			hwCreator.makeRandom(randGroup, randCount);
		} else if ("changePick".equals(editAction)) {
			final int count = MathUtils.parseInt(request.getParameter("count"));
			final int groupNum = MathUtils.parseInt(request.getParameter("groupNum"));
			 Utils.alwaysPrint("hwEditor.jsp: changePick: count = ",
					count, ", groupNum = ", groupNum); /**/
			hwCreator.setBundlesPick(groupNum, count);
		} else if ("changeBundleSize".equals(editAction)) {
			final int bundleSize = MathUtils.parseInt(request.getParameter("bundleSize"));
			final int groupNum = MathUtils.parseInt(request.getParameter("groupNum"));
			 Utils.alwaysPrint("hwEditor.jsp: changeBundleSize: bundleSize = ",
					bundleSize, ", groupNum = ", groupNum); /**/
			hwCreator.setBundleSize(groupNum, bundleSize);
		} else if ("moveQ".equals(editAction) && !Utils.isEmpty(qNumsStr)) {
			// qNumsStr is a single serialNo in the existing assignment
			final int moveFrom = MathUtils.parseInt(qNumsStr);
			final int moveTo = MathUtils.parseInt(request.getParameter("moveTo"));
			hwCreator.moveQuestion(moveFrom, moveTo);
		} else if ("save".equals(editAction)) {
			if (hwCreator.getNumQsSeen() == 0) {
				footerErrorMessage = user.translate("ACE cannot save the "
						+ "assignment because it is empty.");
			} else {
				assgt.setIsMasteryAssgt("on".equals(
						request.getParameter("isMastery")));
				final boolean isMasteryAssgt = assgt.isMasteryAssgt();
				final boolean isValidAssgt = !isMasteryAssgt
						|| assgt.validateMastery();
				if (isValidAssgt) {
					assgt.instructorId = instructorId;
					assgt.setName(assgtName);
					assgt.setRemarks(remarks);
					assgt.setMaxTries(MathUtils.parseInt(
							request.getParameter("numTries")));
					assgt.setAllowedRxnCondns("all".equals(allowedRxnCondns)
							? "" : allowedRxnCondns);
					hwCreator.save();
					(role == User.ADMINISTRATOR ? (AdminSession) userSess 
								: (InstructorSession) userSess
							).refreshAssgts();
					hasBeenSaved = true;
					saveMessage = "Assignment saved.";
					if ("true".equals(request.getParameter("exitContinue"))) {
						editAction = "exit";
					} // if continue to next page
				} else {
					footerErrorMessage = user.translate("ACE cannot save the "
							+ "assignment because a mastery assignment may "
							+ "contain only randomized questions and fixed "
							+ "R-group questions.");
				} // not mastery assignment or validated mastery
			} // non-empty
		} // editAction values
		if ("exit".equals(editAction)) { // continue or save & continue
			if (hwCreator.getNumQsSeen() != 0) { // not empty
				synchronized (session) {
					session.removeAttribute("qSet");
				} // synchronized
				response.sendRedirect(Utils.toString(
						"editHWProps.jsp?hwNum=", hwNum));
			} else { // empty, no questions
				footerErrorMessage = user.translate("ACE cannot continue "
						+ "because the assignment is empty.");
			} // if there are questions
		} // editAction == exit
	} // editAction != null

	final int numQGroups = hwCreator.getNumQGroups();
	final int numQsSeen = hwCreator.getNumQsSeen();
	final String[] myLanguages = user.getLanguages();

	final StringBuilder chooseSetBld = Utils.getBuilder( 
			"<p align=\"center\"><img id=\"image\" src=\"", 
			pathToRoot, "images/ace_nonanim.gif\" alt=\"set\"/>"
				+ "<br /><span class=\"boldtext\">", 
			user.translate((viewType != KEYWORD 
				? Utils.toString("Select a ", viewType == TOPIC 
						? "topic and question set" : "chapter", " from above.")
				: Utils.toString("Enter one or more keywords ", 
						!Utils.isEmpty(myLanguages) ? " (in English)" : "", 
						" above."))), 
			"</span></p>");
	if (viewType == BOOK && Utils.among(book, "Bruice", "Wade")) {
 		Utils.appendTo(chooseSetBld, "<p>", user.translate(
				"Did you know that you can access additional questions "
				+ "from sources other than your textbook? Press <b>Switch "
				+ "to Topic Organization</b> above. Questions in each topic and "
				+ "subtopic may derive from your textbook or another Pearson "
				+ "textbook, or they may be unique to ACE."), "</p>");
	}
	final String chooseSet = chooseSetBld.toString();

	int jmolNum = 0;
	int mviewNum = 0;
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html class="hwEditorHtml" xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
<head>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<title>ACE Assignment Assembly</title>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<style type="text/css">
	#footer {
		position:absolute;
		bottom:0;
		left:0;
		width:100%;
		height:55px;
		overflow:auto;
		text-align:right;
	}

	* html body {
		padding:100px 0 55px 0;
	}
</style>
<script src="hwEditor.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/lewisJS.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jmolStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= 
		AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
	<!-- the next two resources must be called in the given order -->
<script src="<%= pathToRoot %>nosession/jsmol/JSmol.min.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>nosession/jsmol/Jmol2.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/navigation/courseSidebarJS.jsp.h" %>
	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>

	var UNLIMITED = <%= Assgt.UNLIMITED %>;

	// Values of interest.  Follows ACE conventions: 
	//		valueName = @@@@...
	// Whitespace around = sign must be spaces. 
	//	footerErrorMessageValue = @@@@<%= Utils.lineBreaksToJS(footerErrorMessage) %>@@@@
	//	saveMessageValue = @@@@<%= Utils.lineBreaksToJS(saveMessage) %>@@@@

	<% if (viewType == TOPIC) { %>

	var firstLoad = true;

	function loadAllQSets() {
		var topicNumSel = document.selectform.topicOrChapSelector.selectedIndex;
		if (topicNumSel === 0) {
			document.selectform.qSetId.selectedIndex = 0;
			openQSet();
			return;
		}
		var out = new String.builder()
				.append('<select name="qSetId" size="1" onchange="openQSet()" '
					+ 'style="width:375px;"><option value="0">'
					+ '<%= user.translateJS("No question set selected") %>');
		<% for (int topicNum = 1; topicNum <= topics.length; topicNum++) { %>
			if (topicNumSel === <%= topicNum %>) {
				<% final QSetDescr[] qSetDescrs =
						qstore.getQSetDescrs(topicNum);
				for (int qSetNum = 1; qSetNum <= qSetDescrs.length; qSetNum++) { 
					final boolean select = qSetId == qSetDescrs[qSetNum - 1].id 
							|| (qSetDescrs.length == 1 && qSetNum == 1); %>
					out.append('<option value="<%= qSetDescrs[qSetNum - 1].id %>"<%= 
							select ? SELECTED : "" %>><%= 
							Utils.toValidJS(Utils.toPopupMenuDisplay(
								qSetDescrs[qSetNum - 1].name)) %>');
				<% } // for each qSet %>
				if (<%= qSetDescrs.length %> === 1) out.append(' [<%=
						user.translateJS("only set") %>]');
			} // if the topic is selected
		<% } // for each topic %>
		out.append('<\/select>');
		setInnerHTML('qSetsOptions', out.toString());
		firstLoad = false;
		if (document.selectform.qSetId.selectedIndex !== 0) {
			openQSet();
		}
	} // loadAllQSets()

	function addByQId() {
		document.selectform.editAction.value = 'addQById';
		reloadMe();
	} // addByQId()

	<% } // if viewType %>

	function openQSet() {
		var qSetId = (document.selectform.qSetId 
				? document.selectform.qSetId.value : 0);
		if (<%= viewType != TOPIC %> || parseInt(qSetId) !== <%= qSetId %>) {
			reloadMe();
		}
	} // openQSet()

	function doBeforeReload() {
		document.selectform.dependencies.value = getDependencies(
				<%= numQGroups %>, '<%= Assgt.DEP_PAIRS_SEP %>');
		document.selectform.ptsPerQGrpStr.value = getPtsPerQGrpStr();
		var proceed = true;
		<% if (viewType == KEYWORD) { %>
			if (isWhiteSpace(document.selectform.keywords.value)
					&& URI.indexOf('&viewType=') === 0) {
				toAlert('<%= user.translateJS("Please enter one or more "
						+ "keywords in the space provided.") %>');
				proceed = false;
			} // if no keywords
		<% } else { %>
			document.selectform.chapNum.value =
					document.selectform.topicOrChapSelector.value;
		<% } // if viewType %>
		return proceed;
	} // doBeforeReload()

	function setSelectorButtons() {
		<% if ((viewType == TOPIC && qSetId == 0)
				|| (viewType == BOOK && chapNumInit == 0)
				|| (viewType == KEYWORD && "".equals(keywords))) { %>
			hideSelectorButtons();
		<% } else { %>
			showSelectorButtons();
		<% } %>
	} // setSelectorButtons()

	function paintChooseRxnCondns() {
		paintChooseRxnCondnsNow('<%= user.translateJS("In multistep synthesis "
					+ "questions, the students will be able to choose "
					+ "any reaction conditions.") %>',
				'<%= Utils.toValidJS(makeButton(
					user.translate("Choose reaction conditions"),
					"chooseAllowedCondns();")) %>');
	} // paintChooseRxnCondns()

	function saveHW(exitContinue) {
		saveHWNow(exitContinue, 
				'<%= user.translateJS("Enter a name for the assignment.") %>',
				'<%= user.translateJS("Please enter the maximum number "
						+ "of tries allowed.") %>');
	} // saveHW()

	function describeDependencies() {
		toAlert('<%= user.translateJS("If a question depends on another, ACE "
				+ "will display the dependent question only if the student "
				+ "has answered the independent question correctly.") %>');
	} // describeDependencies()

	function updateRandSetList() {
		<% if (isViewed) { %>
			toAlert('<%= user.translateJS(
					"Students have already viewed these questions.") %>');
			return;
		<% } else { %>
			if (document.selectform.randQBoxes) {
				removeRandQNums = 
						getSelectedValues(document.selectform.randQBoxes);
				if (!isEmpty(removeRandQNums)) {
					toAlert('<%= user.translateJS(
							"ACE will not include questions that are already part "
							+ "of random groups in the new random group.") %>');
				} // if random Qs are checked
			} // if there are random Qs
			var selQNums = getSelectedValues(document.selectform.removeBoxes);
			document.selectform.qNums.value = selQNums.join(':');
			document.selectform.editAction.value = 'randQ';
			document.selectform.action = '?count=1';
			reloadMe();
		<% } // if already viewed by student %>
	} // updateRandSetList()

	function selectRandGroup(groupNum) {
		numRandGroupsChecked += (getChecked('removeBox' + groupNum) ? 1 : -1);
		if (numRandGroupsChecked === 0) {
			setInnerHTML('groupUngroup', '<%= Utils.toValidJS(makeButton(
					user.translate("Random group"), "updateRandSetList();")) %>');
			updateRandomVisibility();
		} else {
			setInnerHTML('groupUngroup', '<%= Utils.toValidJS(makeButton(
					user.translate("Ungroup"), "ungroupRandom();")) %>');
			showChooseRandom();
		}
	} // selectRandGroup()

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
		<% if (!Utils.isEmpty(questions)) {
			for (int qNum = 1; qNum <= questions.length; qNum++) { %>
				if (cellExists('figData1_<%= qNum %>')) {
					figuresData.push([
							getValue('figData1_<%= qNum %>'),
							getValue('figQFlags1_<%= qNum %>'),
							'1_<%= qNum %>']);
				} // if the question has a Lewis figure
		<%	} // for each question
		} // if there are questions 
		for (int qGrpNum = 1; qGrpNum <= numQGroups; qGrpNum++) {
			final AssgtQGroup qGrp = hwCreator.getQGroup(qGrpNum);
			for (int grpQNum = 1; grpQNum <= qGrp.getNumQs(); grpQNum++) { %>
				if (cellExists('figData2_<%= qGrpNum %>_<%= grpQNum %>')) {
					figuresData.push([
							getValue('figData2_<%= qGrpNum %>_<%= grpQNum %>'),
							getValue('figQFlags2_<%= qGrpNum %>_<%= grpQNum %>'),
							'2_<%= qGrpNum %>_<%= grpQNum %>']);
				} // if the question has a Lewis figure
		<%	} // for each question in group
		} // for each group of questions %>
		if (!isEmpty(figuresData)) {
			loadLewisInlineImages('<%= pathToRoot %>', figuresData);
		}
	} // initLewis()

	function toggleMastery() {
		setInnerHTML('triesText', document.selectform.isMastery.checked
				? '<%= user.translateJS("Maximum number of tries for mastery") %>'
				: '<%= user.translateJS("Maximum allowed tries per question") %>');
	} // toggleMastery()

	function describeMastery() {
		toBigAlert('<%= pathToRoot %>', 
				'<%= user.translateJS("A mastery assignment requires that a "
				+ "student answer correctly every question in an assignment "
				+ "before ACE considers the assignment to be mastered. If a "
				+ "student submits the correct response within the number of "
				+ "attempts specified by the instructor, then the student has "
				+ "mastered the question. If a student fails to master a "
				+ "question, the student is permitted to continue to work on "
				+ "it (with no limit on the number of tries), or the student "
				+ "can press the Solve related button to reinstantiate the "
				+ "question. So that ACE can reinstantiate a question, a "
				+ "mastery assignment may contain only randomized questions "
				+ "and R-group questions. When ACE reinstantiates an R group "
				+ "question, it sets the student's assigned R groups to new "
				+ "values; when it reinstantiates a randomized question, ACE "
				+ "assigns a new question at random from the appropriate "
				+ "question group.") %>');
	} // describeMastery()

	jmolInitialize('<%= pathToRoot %>nosession/jmol'); 

	function setAfterSaveButtons() {
		if (document.selectform.hwNum.value === '0') setInnerHTML('closeButton', '');
		setInnerHTML('continueButton', '<%= Utils.toValidJS(
				makeButton(user.translate("Continue"), "exitContinue();")) %>');
	} // setAfterSaveButtons()

	// -->
</script>
</head>

<body style="text-align:center; margin:0px;"
	onload="setTab('<%= toTabName(user.translateJS("Assignments")) %>');
			initLewis(); toggleNumTries(); toggleMastery();
			setSelectorButtons(); hideChooseRandom(); <%= 
			viewType == TOPIC ? "loadAllQSets(document.selectform);" : "" %>">

<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

<form name="selectform" action="hwEditor.jsp" method="post" accept-charset="UTF-8">
	<input type="hidden" name="hwNum" value="<%= hwNum %>" />
	<input type="hidden" name="hasBeenSaved" value="<%= hasBeenSaved %>" />
	<input type="hidden" name="allowedRxnCondns" value="<%= 
			allowedRxnCondns != null ? allowedRxnCondns : "" %>" />
	<input type="hidden" name="dependencies" value="" />
	<input type="hidden" name="ptsPerQGrpStr" value="" />
	<input type="hidden" name="qNums" value="" />
	<input type="hidden" name="editAction" value="" />
	<input type="hidden" name="exitContinue" />
	<% if (viewType != KEYWORD) { %>
		<input type="hidden" name="chapNum" />
	<% } // if viewType %>
<table class="regtext"
		style="margin-left:auto; margin-right:auto; 
		width:98%; margin-top:5px; vertical-align:top;">
<tr>

<td style="width:50%; vertical-align:top; padding-right:5px;">
<div id="leftContentsWithTabsWithFooter">
<table style="width:100%;" summary="">
<tr>
<td class="boldtext big" style="margin-left:auto; margin-right:auto;">
	<%= user.translate("Database Questions") %>
</td>
<td class="boldtext" style="text-align:right;
		margin-left:auto; margin-right:auto; margin-top:10px;">
	<%= user.translate("Organize questions by ") %>
	<select name="viewType" onchange="reloadMe()">
		<option value="<%= TOPIC %>" <%= viewType == TOPIC
				? SELECTED : "" %>><%= user.translate("topic") %></option>
		<option value="<%= KEYWORD %>" <%= viewType == KEYWORD
				? SELECTED : "" %>><%= user.translate("keywords") %></option>
	</select>
</td>
</tr>
</table>
<table id="theTable" class="whiteTable" style="width:100%;
		background-color:#f6f7ed; margin-left:auto; margin-right:auto;
		border-collapse:collapse; text-align:left;" summary="">
<% if (viewType == TOPIC) { %>
	<tr><td style="padding-left:10px; padding-top:5px; padding-right:10px;">
		<b><%= user.translate("Select topic") %></b> <br />
		<select name="topicOrChapSelector" size="1"
				onchange="loadAllQSets()" style="width:375px">
			<option value="0">
				<%= user.translate("No topic selected") %>
			</option>
			<% for (int topicNum = 1; topicNum <= topics.length; topicNum++)  {
				final Topic topic = topics[topicNum - 1]; %>
				<option value="<%= topicNum %>"
						<%= topicNum == chapNumInit ? SELECTED : "" %>>
					<%= Utils.toPopupMenuDisplay(topic.name) %>
				</option>
			<% } // for each topic %>
		</select>
	</td></tr>
	<tr><td style="padding-left:10px; padding-right:10px;">
		<b><%= user.translate("Select question set") %></b> <br/>
		<div id="qSetsOptions">
		<select name="qSetId" size="1" onchange="openQSet()" style="width:375px;">
			<option value="0"><%=
					user.translate("No question set selected") %></option>
		</select>
		</div>
	</td></tr>
	<tr><td id="selectHow" style="padding-left:10px; padding-right:10px;">
		<% if (qSetId == 0) { %>
			<table class="regtext" summary=""><tr>
			<td>
				<b><%= user.translate("or enter a question ID number") %></b>:
				<input type="text" name="qId" value="" size="4" />
			</td><td>
				<%= makeButton(user.translate("Add"), "addByQId();") %>
			</td>
			</tr></table>
		<% } // if there's no chosen qSet %>
	</td></tr>
<% } else if (viewType == BOOK) { %>
	<tr><td colspan="3"
			style="padding-left:10px; padding-top:5px; padding-right:10px;">
		<b><%= user.translate("Book") %>: </b><%= book %> <br />
	</td></tr>
	<tr><td colspan="3"
			style="padding-left:10px; padding-top:5px; padding-right:10px;">
		<b><%= user.translate(Utils.toString("Select ",
				"Other".equals(book) ? "author" : "chapter")) %>:</b> <br />
		<select name="topicOrChapSelector" size="1" onchange="openQSet()"
				style="width:375px" >
			<option value="0">
				<%= user.translate(Utils.toString("No ", "Other".equals(book) 
						? "author" : "chapter", " selected")) %>
			</option>
			<% for (int chapNum = 1; chapNum <= chapterNums.length; chapNum++) {
				String chapName = "";
				try {
					chapName = (chapterNames == null ? ""
							: chapterNames[MathUtils.parseInt(
								chapterNums[chapNum - 1]) - 1]);
				} catch (NumberFormatException e) {
					if (!"Other".equals(book))
						Utils.alwaysPrint("hwEditor.jsp: chapter number ",
								chapterNums[chapNum - 1], " not an integer");
				} catch (ArrayIndexOutOfBoundsException e) {
					if (!"Other".equals(book))
						Utils.alwaysPrint("hwEditor.jsp: chapter number ",
								chapterNums[chapNum - 1], " out of range");
				} // try %>
				<option value="<%= chapNum %>"
						<%= chapNum == chapNumInit ? SELECTED : "" %>>
					<%= chapterNums[chapNum - 1] %><%=
							!Utils.isEmpty(chapName) ? Utils.toString(". ", chapName) : "" %>
				</option>
			<% } // for each book chapter chapNum %>
		</select>
	</td></tr>
	<tr><td class="regtext" style="padding-left:10px; padding-top:10px;">
		<div id="fixed1" style="visibility:hidden;">
		<input type="checkbox" name="selectallbox"
				onclick="javascript:selectAll(this)" />
		<%= user.translate("Select All") %>
		</div>
	</td><td>
	</td><td style="padding-right:10px; padding-left:10px; padding-top:10px;">
		<div id="fixed2" style="visibility:hidden;">
		<%= makeButton(user.translate("Add Selected"), "addSelected();") %>
		</div>
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
		<b><%= user.translate("Enter keywords") %>:</b>
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
		<%= makeButton(user.translate("Get questions"), "openQSet();") %>
	</td>
	</tr>
	<tr>
	<td colspan="3" style="padding-left:10px; padding-top:5px;
			padding-right:10px; color:green;">
		<%= user.translate("An example of a search for more than one keyword:") %>
	</td>
	</tr>
	<tr>
	<td colspan="3" style="padding-left:30px; padding-right:10px; color:green;">
		[tosylate or alkyl halide] and [substitution or elimination]
	</td>
	</tr>
	<tr>
	<td colspan="3" style="padding-left:10px; padding-right:10px; color:green;">
		<%= user.translate("Note the use of square brackets instead of parentheses.") %>
		<% if (!Utils.isEmpty(myLanguages)) { %>
			<%= user.translate("The search must be in English.") %>
		<% } %>
	</td>
	</tr>
	<tr><td class="regtext" style="padding-left:10px; padding-top:10px;">
		<div id="fixed1" style="visibility:hidden;">
		<input type="checkbox" name="selectallbox"
				onclick="javascript:selectAll(this)" />
		<%= user.translate("Select All") %>
		</div>
	</td><td>
	</td><td style="padding-right:10px; padding-left:10px; padding-top:10px;">
		<div id="fixed2" style="visibility:hidden;">
		<%= makeButton(user.translate("Add Selected"), "addSelected();") %>
		</div>
	</td></tr>
<% } // if viewType %>
</table>
<% if (Utils.isEmpty(questions)) { %>
<table style="width:100%; padding-left:30px; padding-right:30px;" summary="">
<tr><td id="dbQuestions" class="regtext" style="vertical-align:middle;">
	<%= chooseSet %>
</td></tr>
</table>
<% } else { %>
<table style="width:100%; margin-left:auto; margin-right:auto;
		border-collapse:collapse;" summary="">
<tr><td style="padding-bottom:10px;" id="dbQuestions">
	<table class="whiteTable" style="width:100%; text-align:center;
			margin-left:auto; margin-right:auto; background-color:#f6f7ed;" summary="">
	<% if (viewType == TOPIC) { %>
		<tr><td class="boldtext" colspan="2" style="vertical-align:top;
				padding-left:15px; padding-right:15px; padding-top:10px;">
		<% final String topicName = (descr != null && descr.topicName != null 
				? descr.topicName.replace("[null]", "") : ""); %>
		<%= Utils.toDisplay(topicName).trim() %>, <%= Utils.toDisplay(
				descr != null ? descr.name : "") %>
		</td></tr>
		<tr><td style="vertical-align:top; text-align:left; padding-left:15px;
				padding-right:15px;" colspan="2">
			<span class="boldtext"><%=
					user.translate("Remarks") %>:</span>&nbsp;<span
					class="regtext"><%= descr == null ||
							Utils.isEmpty(descr.remarks)
							? "[None]" : descr.remarks %></span>
		</td></tr>
		<tr><td style="vertical-align:top; text-align:left; padding-left:15px;
				padding-right:15px; padding-bottom:10px;" colspan="2">
			<div class="boldtext"><%= user.translate(
					"Statement to be repeated for every question") %>:</div>
			<span class="regtext">
			<%= descr == null || Utils.isEmpty(descr.header)
					? Utils.toString('[', user.translate("None"), ']')
					: Utils.toDisplay(descr.header) %>
			</span>
		</td></tr>
	<% } else if (viewType == BOOK) { %>
		<tr><td class="boldtext" colspan="2" style="vertical-align:top;
				padding-left:15px; padding-right:15px; padding-top:10px;">
			<%= book %>, <%= user.translate("Other".equals(book)
					? "Author" : "Chapter") %>
			<%= chapterName %>
		</td></tr>
	<% } // if viewType %>
	</table>
</td></tr>
<tr><td id="dbQuestions2">
	<table style="width:100%; border-collapse:collapse; margin-left:auto;
			margin-right:auto;" summary="">
	<% if (!Utils.isEmpty(questions)) { %>
		<tr><td style="border-bottom:solid; border-width:1px;
				border-color:#49521B;"></td></tr>
	<%	for (int qNum = 1; qNum <= questions.length; qNum++) {
			final Question oneQ = questions[qNum - 1];
			final String rowColor = (qNum % 2 == 0 ? "whiterow" : "greenrow");
			final boolean hideQ = oneQ.hide();
	%>
			<tr class="<%= rowColor %>">
			<td class="regtext" style="padding-left:10px; padding-right:10px;
					padding-top:10px; border-left:solid; border-right:solid;
					border-color:#49521B; border-width:1px; vertical-align:middle;">
				<input type="checkbox" id="qChecker<%= qNum - 1 %>"
						name="qChecker" value="<%= qNum %>" <%= 
							hideQ ? "disabled" : "" %> />
				(<b><%= qNum %></b>)
	<% 			if (hideQ) { %>
					<%= user.translate("This question is not yet available.") %>
					</td></tr>
	<% 				continue;
				} // if hidden from local authors
				if (oneQ.isModified()) {
					%><span class="modified"><%= user.translate("Modified") %></span><%
				} else if (oneQ.isNew()) {
					%><span class="new"><%= user.translate("New") %></span><%
				} // if oneQ
				final String majorQTypeStr = 
						Utils.toString(oneQ.getQTypeDescription(user), ", #", 
						Utils.formatNegative(oneQ.getQId()));
				final String qBook = oneQ.getBook();
				final String qRemarks = oneQ.getRemarks();
	%>
				<span class="boldtext">(<%= majorQTypeStr %>,
					<%= "Other".equals(qBook) 
							? Utils.toString(user.translate("by"), ' ', oneQ.getChapter())
							: Utils.toString(qBook, "[None]".equals(qRemarks) 
								? "" : Utils.toString(' ', qRemarks))
					%>)</span>
				<%= oneQ.getDisplayStatement(!Question.CONVERT_VARS_TO_VALUES) %>
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
					final boolean useMView = !figure.isJmol() && !figure.hasImage(); %>
					<br/>
					<table summary="">
	<% 				if (numFigures > 1 || useMView) { %>
						<tr>
						<td <%= figure.isReaction() ? "colspan=\"2\"" : "" %>>
							<table style="width:100%;" summary="">
							<tr>
								<td class="boldtext" style="width:100%;">
	<%							if (numFigures > 1) { %>
									<%= user.translate(
											"Fig. ***1*** of ***2***",
											new int[] {1, numFigures}) %>
	<% 							} // more than one figure %>
								</td>
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
	<%				} else if (figure.isLewis()) { %>
						<tr><td id="fig1_<%= qNum %>" class="whiteTable">
						<input type="hidden" id="figData1_<%= qNum %>"
								value="<%= Utils.toValidHTMLAttributeValue(
									figData[Figure.STRUCT]) %>" />
						<input type="hidden" id="figQFlags1_<%= qNum %>"
								value="<%= oneQ.getQFlags() %>" />
						</td></tr>
	<%				} else if (!figure.hasImage()) {
						mviewNum++;
						final String figIdStr = Utils.toString(
								"mview", mviewNum);
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
					&& oneQ.getNumQData(Question.GENERAL) > 0) { 
	%>
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
	<%	} // for each Q qNum in Qset %>
		<tr><td style="border-top:solid; border-width:1px; border-color:#49521B;"></td></tr>
	<% } else { %>
		<%= user.translate("The question set is empty.") %>
	<% } // if questions is not null %>
	</table>
</td></tr>
</table>
<% } // if questions is not null or empty  %>
</div>
</td>

<td style="vertical-align:top;">
<div id="rightContentsWithTabsWithFooter">
<table style="margin-left:auto; margin-right:auto; width:98%;
		border-collapse:collapse;" summary="">
	<tr>
		<td class="boldtext big" colspan="3">
			<%= user.translate("Current Assignment") %>
		</td>
	</tr>
	<tr>
		<td style="padding-bottom:10px; text-align:left;" colspan="3">
			<table class="whiteTable"
					style="background-color:#f6f7ed; width:100%;" summary="">
			<tr><td style="padding-left:10px; width:90px;
					padding-top:10px; white-space:nowrap;">
				<%= user.translate("Title") %>:
			</td><td style="padding-right:10px; padding-top:10px;">
				<input type="text" name="assgtName"
					value="<%= Utils.toValidTextbox(assgtName) %>"
					style="width:300px;" />
			</td></tr>
			<tr><td style="padding-left:10px;">
				<%= user.translate("Remarks") %>:
			</td><td style="padding-right:10px;">
				<textarea name="remarks" rows="3" cols="30"
						style="width:300px;"><%= remarks != null
								? Utils.toValidTextbox(remarks)
								: "" %></textarea>
			</td></tr>
			<tr><td style="padding-left:10px; padding-right:10px;
					white-space:nowrap;" colspan="2">
				<table summary="">
				<tr><td>
				<span id="triesText">
					<%= user.translate("Maximum allowed tries per question") %>
				</span>:
				</td><td>
				<input type="text" name="numTries"
						value="<%= numTries != Assgt.UNLIMITED ?
							numTries : "" %>" size="2" />
				</td><td>
				<input type="checkbox" name="unlimited" onclick="toggleNumTries()"
						<%= numTries == Assgt.UNLIMITED 
							? " checked=\"checked\"" : "" %>/>
						<%= user.translate("Allow unlimited tries") %>
				</td><td style="padding-left:20px;">
				<input type="checkbox" name="isMastery" onclick="toggleMastery()"
						<%= assgt.isMasteryAssgt()
							? " checked=\"checked\"" : "" %>/>
						<a href="javascript:describeMastery();"><%= 
								user.translate("Is mastery assignment") %></a>
				</td></tr>
				</table>
			</td></tr>
			<tr><td style="padding-left:10px; padding-right:10px;
					white-space:nowrap;" colspan="2">
				<tr><td>
				<%= makeButton(user.translate("Set all questions to"), 
						"setAllPoints();") %>
				</td><td>
				<input type="text" id="allQPts" value="" size="2" />
				&nbsp;points
				</td><td>
			</td></tr>
			<tr><td id="chooseRxnConds1" style="padding-left:10px;
					padding-right:10px;" colspan="2">
			</td></tr>
			<tr><td id="chooseRxnConds2" style="padding-left:10px; padding-right:10px;
					text-align:center;" colspan="2">
			</td></tr>
			</table>
		</td>
	</tr>
	<tr><td style="text-align:center;">
		<% if (!Utils.isEmpty(hwErrorMessage)) { %>
			<div id="error_message">
			<div class="whiteTable" style="text-align:center; border-color:#FF0000;
					background-color:#f6f7ed; width:300px; padding:10px;">
			<div style="float:right; position:relative; top:-10px; left:10px;">
				<%= makeButtonIcon("delete", pathToRoot, "removeErrorMessage();") %>
			</div>
			<span style="color:#FF0000;"><%= hwErrorMessage %></span>
			</div>
			<br />
			</div>
		<% } // error message isn't null %>
		<table style="border-collapse:collapse; width:100%;" summary="">
		<% int rowToggle = 0;
		final String[] rowColor = {"greenrow", "whiterow", "bluerow"};
		if (numQsSeen == 0) { %>
			<tr class="<%= rowColor[rowToggle] %>">
			<td class="regtext" style="text-align:center; border-left:solid;
					border-top:solid; border-right:solid; border-width:1px;
					border-color:#49521B;" colspan="2">
				<%= user.translate("The assignment is empty.") %>
			</td></tr>
		<% } else { 
			final double totalPts = hwCreator.getMaxGrade(); 
			String totalPtsStr = String.valueOf(totalPts); 
			if (totalPtsStr.endsWith(".0")) {
				totalPtsStr = Utils.rightChop(totalPtsStr, 2);
			} // if string ends in .0
			%>
			<tr class="<%= rowColor[rowToggle] %>">
				<td class="boldtext enlarged"
						style="border-left:solid; border-color:#49521B;
						padding-left:10px; border-top:solid; border-width:1px;">
					<%= user.translate("Questions") %>
					(<%= user.translate("***8*** total", numQsSeen) %>,
					<span id="sumPoints"><%= totalPtsStr %></span> 
					<%= user.translate(totalPts == 1.0 ? "point" : "points") %>)
				</td>
				<td class="boldtext enlarged" style="text-align:center; 
						padding-left:5px; padding-right:5px;
						border-top:solid; border-width:1px; border-color:#49521B;">
					<%= user.translate("Select") %>
				</td>
				<td class="boldtext enlarged" style="text-align:center; 
						padding-left:5px; padding-right:5px;
						border-top:solid; border-width:1px; border-color:#49521B;">
					<a href="javascript:describeDependencies();">
					<%= user.translate("Depends on") %>
					</a>
				</td>
				<td class="boldtext enlarged" style="text-align:center; 
						padding-left:5px; padding-right:5px; border-right:solid;
						border-top:solid; border-width:1px; border-color:#49521B;">
					<a href="javascript:describeDependencies();">
					<%= user.translate("Points") %>
					</a>
				</td>
			</tr>
		<% } // if there's one Q in assignment
		int realQNum = 1;
		for (int qGrpNum = 1; qGrpNum <= numQGroups; qGrpNum++) {
			final AssgtQGroup qGrp = hwCreator.getQGroup(qGrpNum);
			final int groupSize = qGrp.getNumQs();
			final boolean isRandomGroup = qGrp.isRandom();
			final int numToPick = hwCreator.getBundlesPick(qGrpNum);
			final int bundleSize = hwCreator.getBundleSize(qGrpNum);
			boolean startOfRandGroup = isRandomGroup;
			for (int grpQNum = 1; grpQNum <= groupSize; grpQNum++) {
				final Question hwSetQ = qGrp.getQ(grpQNum);
				if (startOfRandGroup) { %>
					</td></tr>
					<tr class="<%= rowColor[2] %>">
						<td class="regtext" style="padding-left:10px;
								padding-bottom:10px; padding-top:10px; border-left:solid;
								border-width:1px; border-color:#49521B;">
							<table summary=""><tr><td style="vertical-align:top;">
				<% } else { 
					rowToggle = 1 - rowToggle; %>
					<tr class="<%= rowColor[rowToggle] %>">
						<td class="regtext" id="row<%= qGrpNum - 1 %>"
								style="padding-left:10px; padding-bottom:10px; 
								padding-top:10px; border-left:solid;
								border-width:1px; border-color:#49521B;"> 
				<% } // if start of random group
				if (numQGroups > 1 && (!isRandomGroup || startOfRandGroup)) { %>
							<input type="hidden" id="realQNumForQGrp<%= qGrpNum %>" 
									value="<%= realQNum %>" />
							<select id="moveMenu<%= qGrpNum %>" 
									onchange="moveSelected(<%= qGrpNum %>)">
					<% for (int ddQNum = 1; ddQNum <= numQGroups; ddQNum++) {
						final AssgtQGroup oneQGrp = hwCreator.getQGroup(ddQNum);
						final boolean rgstart = oneQGrp.getNumQs() > 1;
						if (!oneQGrp.isRandom() || rgstart) { %>
							<option value="<%= ddQNum %>"
								<%= qGrpNum == ddQNum ? SELECTED : "" %> >
									<%= ddQNum %>
							</option>
					<% 	} // if Q should be listed in pulldown menu
					} // for each group of Qs %>
							</select>
				<% 
				} // if more than one question, should make pulldown menu
				if (startOfRandGroup) {
					String pickInsert = null;
					String sizeInsert = null;
					boolean canBundle = false;
					if (!isViewed) {
						final StringBuilder pickSelector = 
								Utils.getBuilder("<select id=\"picker", 
								qGrpNum, "\" onchange=\"changePick(this, ", 
								qGrpNum, ")\">");
						for (int ddPick = 1; 
								ddPick < groupSize / bundleSize; ddPick++) {
 							Utils.appendTo(pickSelector, 
									"<option value=\"", ddPick, "\"",
									numToPick != ddPick ? "" : SELECTED,
 									">", ddPick, "</option>");
						} // for each number of Qs that might be picked
						pickInsert = pickSelector.append("</select>").toString();
						final StringBuilder sizeSelector = 
								Utils.getBuilder("<select id=\"sizer", 
								qGrpNum, "\" onchange=\"changeBundleSize"
								+ "(this, ", qGrpNum, ")\">");
						for (int ddSize = 1; ddSize < groupSize; ddSize++) {
							if (groupSize % ddSize == 0) {
								if (ddSize != 1) canBundle = true;
 								Utils.appendTo(sizeSelector, 
										"<option value=\"", ddSize, "\"",
										bundleSize != ddSize ? "" : SELECTED,
 										'>', ddSize, "</option>");
							} // if bundle size divides questions evenly
						} // for each bundle size
						sizeInsert = sizeSelector.append("</select>").toString();
					} else {
						pickInsert = Utils.toString("<input type=\"hidden\""
								+ "id=\"picker", qGrpNum, "\" value=\"", 
								numToPick, ")\" \\/>", numToPick);
						sizeInsert = Utils.toString("<input type=\"hidden\""
								+ "id=\"sizer", qGrpNum, "\" value=\"", 
								bundleSize, ")\" \\/>", bundleSize);
						canBundle = bundleSize != 1;
					} // if assignment has been viewed
					final String randDescrip = Utils.toString("For each student, "
							+ "ACE will randomly select ***2*** ", 
							canBundle ? "group(s) of ***3*** question(s)" 
								: "question(s)",
							" from the following ***12*** questions",
							bundleSize == 1 ? "" 
								: " grouped into ***4*** contiguous groups",
							'.');
					final String[] randInserts = (!canBundle 
							? new String[] {pickInsert, String.valueOf(groupSize)}
							: bundleSize == 1 ? new String[] {pickInsert, sizeInsert,
								String.valueOf(groupSize)}
							: new String[] {pickInsert, sizeInsert,
								String.valueOf(groupSize),
								String.valueOf(groupSize / bundleSize)});
				%>
						</td><td>
						<b><%= user.translate(randDescrip, randInserts) %></b>
						</td></tr></table>
					</td>
				<!-- select button --> 
					<td style="vertical-align:top; padding-top:10px;
							text-align:center; padding-right:10px;">
						<input type="checkbox"
							onchange="selectRandGroup(<%= qGrpNum %>)"
							id="removeBox<%= qGrpNum %>"
							name="removeBoxes" value="<%= qGrpNum %>" />
					</td>
					<td style="vertical-align:top; padding-top:10px;
							text-align:center; padding-right:10px;">
						&nbsp;
					</td>
				<!-- points box --> 
					<td style="vertical-align:top; padding-top:10px;
							padding-right:10px; border-right:solid;
							border-width:1px; border-color:#49521B;">
						<input type="text" size="1" name="pointsOfQGrp<%= qGrpNum %>"
								id="pointsOfQGrp<%= qGrpNum %>"
								value="<%= qGrp.getPts() %>" 
								onkeyup="setSumPoints();" />
					</td>
					</tr>
					<tr class="<%= rowColor[rowToggle] %>">
					<td class="regtext" style="padding-left:10px;
							padding-bottom:10px; padding-top:10px; border-left:solid;
							border-width:1px; border-color:#49521B;" id="row<%= qGrpNum - 1 %>">
				<% } // if is first Q in a random group
				if (isRandomGroup) { %>
						<table width="100%" summary=""><tr class="<%= rowColor[2] %>" width="10%">
						<td>&nbsp;</td><td class="<%= rowColor[rowToggle] %>">
				<% } // if Q is in a random group
				final String majorQTypeStr = 
						Utils.toString(hwSetQ.getQTypeDescription(), ", #", 
						Utils.formatNegative(hwSetQ.getQId()));
				final String qBook = hwSetQ.getBook();
				final String qRemarks = hwSetQ.getRemarks();
				%>
				<script type="text/javascript">
					// <!-- >
					if (<%= hwSetQ.isSynthesis() %> && !paintedChooseRxnConds) {
						paintChooseRxnCondns();
						paintedChooseRxnConds = true;
					}
					// -->
				</script>
				<span class="boldtext">(<%= majorQTypeStr %>,
					<%= "Other".equals(qBook)
							? Utils.toString(user.translate("by"), ' ', hwSetQ.getChapter())
							: Utils.toString(qBook, "[None]".equals(qRemarks) 
								? hwSetQ.getChapter() : Utils.toString(' ', qRemarks))
					%>)
					</span>
				<%= hwSetQ.getDisplayStatement(!Question.CONVERT_VARS_TO_VALUES) %>
	
	<!-- display Q figure -->
				<% final int numFigures = hwSetQ.getNumFigures();
				if (numFigures > 0) {
					final Figure figure = hwSetQ.getFigure(1);
					final String[] figData = (figure.isSynthesis()
							? figure.getDisplayData(
									Synthesis.getRxnsDisplayPhrases(user))
							: figure.getDisplayData());
					final boolean useMView = !figure.isJmol() && !figure.hasImage(); %>
					<br/>
					<table summary="">
		<% 			if (numFigures > 1 || useMView) { %>
						<tr>
						<td <%= figure.isReaction() ? "colspan=\"2\"" : "" %>>
							<table style="width:100%;" summary="">
							<tr>
								<td class="boldtext" style="width:100%;">
		<%						if (numFigures > 1) { %>
									<%= user.translate(
											"Fig. ***1*** of ***2***",
											new int[] {1, numFigures}) %>
		<% 						} // more than one figure %>
								</td>
							</tr>
							</table>
						</td>
						</tr>
		<%			} // if figure has text above it
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
		<%			} else if (figure.isLewis()) { %>
						<tr><td id="fig2_<%= qGrpNum %>_<%= grpQNum %>" 
								class="whiteTable">
						<input type="hidden" 
								id="figData2_<%= qGrpNum %>_<%= grpQNum %>"
								value="<%= Utils.toValidHTMLAttributeValue(
									figData[Figure.STRUCT]) %>" />
						<input type="hidden" 
								id="figQFlags2_<%= qGrpNum %>_<%= grpQNum %>"
								value="<%= hwSetQ.getQFlags() %>" />
						</td></tr>
		<% 			} else if (!figure.hasImage()) {
						mviewNum++;
						final String figIdStr = Utils.toString(
								"mview", mviewNum);
						if (figure.isReaction()) { %>
							<tr><td>
								<table class="whiteTable" summary="">
								<tr><td id="<%= figIdStr %>">
		<%				} else { %>
							<tr><td id="<%= figIdStr %>" class="whiteTable">
		<%				} %>
								<%= figure.getImage(pathToRoot, user.prefersPNG(), 
										hwSetQ.getQFlags(), figIdStr) %>
							</td>
		<% 				if (figure.isReaction()) { 
							final String above = Utils.toDisplay(figData[Figure.RXN_ABOVE]);
							final String below = Utils.toDisplay(figData[Figure.RXN_BELOW]);
							final int arrowSize = 36; %>
							<td style="width:85px; vertical-align:middle;
									background-color:#FFFFFF; text-align:center;">
								<%@ include file="/includes/reactionArrow.jsp.h" %>
							</td> <!-- 4 -->
							</tr>
							</table>
							</td></tr>
		<%				} else if (figure.isSynthesis()) { %>
							<%= figData[Synthesis.RXNID] %>
		<% 				} // if synthesis figure
						else { %>
						</tr>
						<% } 
					} else { // image %>
						<tr><td>
						<img class="whiteTable" src="<%= pathToRoot %><%= 
									figure.bufferedImage %>"
								alt="picture" onload="fixImageSize(this);"
								style="visibility:hidden;" />
						</td></tr>
		<% 			} // figure.type %>
					</table>
		<% 		} // numFigures %>
	
	<!-- display Q data -->
				<% if ((hwSetQ.isChoice() || hwSetQ.isChooseExplain() || hwSetQ.isRank()) 
						&& hwSetQ.getNumQData(Question.GENERAL) > 0) { %>
					<br /><br />
	
					<table style="padding-left:10px;" summary="">
				<% 		final QDatum[] qData = hwSetQ.getQData(Question.GENERAL);
						if (qData != null) {
							for (int optNum = 0; optNum < qData.length; optNum++) {
								final QDatum qDatum = qData[optNum];
								if (qDatum != null) {
				%>
									<tr><td class="regtext">
										<b>Option <%= qDatum.serialNo %>.</b>
										<%= qDatum.toShortDisplay(hwSetQ.chemFormatting()) %>
									</td></tr>
				<%  			} // if qDatum is not null 
							} // for each qDatum optNum
						} else {
							Utils.alwaysPrint("hwEditor.jsp: Couldn't get qdata from Q");
						} %>
					</table>
				<% } // if choice or rank
				if (isRandomGroup) { %>
					</td></tr></table>
				<% } // if is random group %>
				</td>
	
	<!-- select button --> 
				<td style="vertical-align:top; padding-top:10px;
						text-align:center; padding-right:10px; padding-top:10px;"> 
					<% if (isRandomGroup && !isViewed) { %>
						<input type="checkbox" name="randQBoxes"
							value="<%= qGrpNum %>_<%= grpQNum %>" />
					<% } else if (!isRandomGroup) { %>
						<input type="checkbox" onchange="updateRandomVisibility()"
							name="removeBoxes" value="<%= qGrpNum %>" />
					<% } // if is random Q %>
				</td>
	<!-- dependencies selector --> 
				<td style="vertical-align:top; padding-top:10px;
						text-align:center; padding-right:10px; padding-top:10px;"> 
					<% if (!isRandomGroup) { 
						final int thisQId = hwSetQ.getQId(); 
						final int dependsOn = assgt.getDependsOn(thisQId); %>
						<select name="dependenceOf<%= qGrpNum %>" 
								id="dependenceOf<%= qGrpNum %>">
							<option value=""></option>
							<% for (int ddQGrpNum = 1; ddQGrpNum <= numQGroups; ddQGrpNum++) {
								if (qGrpNum == ddQGrpNum) continue;
								final AssgtQGroup ddQGrp = hwCreator.getQGroup(ddQGrpNum);
								final int ddGrpQId = ddQGrp.getQId(1);
								if (!ddQGrp.isRandom() && ddQGrpNum != qGrpNum) { %>
									<option value="<%= ddGrpQId %>"
										<%= dependsOn == ddGrpQId ? SELECTED : "" %> >
										<%= ddQGrpNum %>
									</option>
							<% 	} // if Q should be listed in pulldown menu
							} // for each group of Qs %>
						</select>
					<% } // if is not random Q %>
				</td>
	<!-- points box --> 
				<td style="vertical-align:top; padding-top:10px;
						text-align:center; padding-right:10px; padding-top:10px; 
						border-right:solid; border-width:1px; border-color:#49521B;">
					<% if (!isRandomGroup) { %>
						<input type="text" size="1" name="pointsOfQGrp<%= qGrpNum %>"
								id="pointsOfQGrp<%= qGrpNum %>"
								value="<%= qGrp.getPts() %>" 
								onkeyup="setSumPoints();" />
					<% } // if is not random Q %>
				</td>
				</tr>
				<% startOfRandGroup = false;
			} // for each Q within the group 
			realQNum += numToPick * bundleSize;
		} // for each group of Qs %>
		<tr><td style="border-top:solid; border-width:1px;
				border-color:#49521B;" colspan="4"></td></tr>
		</table>
	</td>
	</tr>
	</table>
</div>
</td>
</tr>
</table>

</form>
<div id="footer">
<table style="margin-left:auto; margin-right:auto;
		border-top:solid; border-width:1px; border-color:#49521B;
		text-align:left; border-collapse:collapse; width:100%;" summary="">
<tr>
<td style="width:40%; padding-top:5px; text-align:left;">
	<table summary="">
	<tr><td> 
		<table summary=""><tr> 
		<td id="msgCell"><div class="whiteTable"
				style="padding-bottom:5px; padding-left:10px; padding-right:10px;
					border-color:#FF0000; color:#FF0000; padding:5px;
					visibility:<%= footerErrorMessage == null ? "hidden" : "visible" %>">
			<%= footerErrorMessage == null ? "" : user.translate(footerErrorMessage) %>
			<%= saveMessage == null ? "" : user.translate(saveMessage) %>
		</div></td>
		</tr></table>
	</td>
	<% if (qSetId != 0) { %>
	<td>
	<table class="regtext" summary="">
		<tr>
		<td style="width:50%;">
			<div id="fixed1" style="visibility:hidden;">
				<input type="checkbox" name="selectallbox"
						onclick="javascript:selectAll(this)" />
				<%= user.translate("Select All") %>
			</div>
		</td><td style="padding-right:10px;">
			<div id="fixed2" style="visibility:hidden;">
				<%= makeButton(user.translate("Add Selected"),
							"addSelected();") %>
			</div>
		</td></tr>
	</table>
	</td>
	<% } // if a question set has been chosen %>
	</tr></table>
</td>
<td style="padding-top:5px;">
	<table summary="">
	<tr>
	<td style="padding-right:10px; text-align:center;" id="groupUngroup">
		<%= makeButton(user.translate("Random group"), "updateRandSetList();") %>
	</td>
	<td style="padding-right:10px; text-align:center;">
		<%= makeButton(user.translate("Remove selected"), "removeSelected();") %>
	</td>
	<% if (!newHW || !hasBeenSaved) { %>
		<td id="closeButton"><table><tr>
		<td style="padding-right:10px; text-align:center;">
			<%= makeButton(user.translate("Close"), "cancelOp();") %>
		</td>
		</tr></table></td>
	<% } // if should allow close without proceeding %>
	<td style="padding-right:10px; text-align:center;">
		<%= makeButton(user.translate("Save"), "saveHW(!CONTINUE);") %>
	</td>
	<% if (!newHW || hasBeenSaved) { %>
		<td id="continueButton"><table><tr>
		<td style="padding-right:10px; text-align:center;">
			<%= makeButton(user.translate("Continue"), "exitContinue();") %>
		</td>
		</tr></table></td>
	<% } // if new assignment, need to save and continue %>
	<td style="padding-right:10px; text-align:center;">
		<%= makeButton(user.translate("Save &amp; Continue"), "saveHW(CONTINUE);") %>
	</td>
	</tr>
	</table>
</td>
</tr>
</table>
</div>
</body>
</html>

