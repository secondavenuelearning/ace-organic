<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.db.UserRead,
	com.epoch.chem.MolString,
	com.epoch.evals.EvalManager,
	com.epoch.evals.Evaluator,
	com.epoch.evals.Subevaluator,
	com.epoch.lewis.LewisMolecule,
	com.epoch.qBank.Figure,
	com.epoch.qBank.QSetDescr,
	com.epoch.qBank.Question,
	com.epoch.qBank.Topic,
	com.epoch.session.QuestionBank,
	com.epoch.session.QSet,
	com.epoch.synthesis.Synthesis,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.List,
	java.util.Locale"
%>

<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String QSET = "qSet";

	synchronized (session) {
		session.removeAttribute("translationObj");
		session.removeAttribute("phraseTranslator");
		session.removeAttribute("clone");
	}

	// determine if this is a master edit (entry defined in verifyEntry.jsp.h)
	final boolean masterEdit = entry.isMasterEdit();
	final String bgColor = (masterEdit ? "#f6edf7" : "#f6f7ed");

	String userId;
	synchronized (session) {
		userId = (String) session.getAttribute("userId");
	}
	boolean isTranslator = masterEdit || (user.isTranslator() 
			&& !Utils.isEmpty(user.getLanguages()));
	
	// are we already impersonating the owner of a course that we coinstruct?
	boolean assigned = userId != null && !userId.equals(user.getUserId());
	// if not, do we want to impersonate the owner of a course that we coinstruct?
	final String coinstructorId = request.getParameter("coinstructorId");
	final User[] owners = (!masterEdit && !assigned && coinstructorId == null
				? ((InstructorSession) userSess)
					.getCoinstructedCrsAndCoauthoredBkOwners()
				: new User[0]);
	final boolean isCoinstructor = !Utils.isEmpty(owners);
	/* Utils.alwaysPrint("questionsList.jsp: userId = ", userId,
			", coinstructorId = ", coinstructorId, ", assigned = ",
			coinstructorId != null ? true : assigned,
			", numCoinstructedCourseOwners = ", owners.length);
	/**/
	if (coinstructorId != null) {
		userId = coinstructorId;
		assigned = true;
		synchronized (session) {
			session.setAttribute("userId", userId);
			session.removeAttribute("qBank");
			session.removeAttribute(QSET);
		}
		isTranslator = false; // can't translate when impersonating a coinstructor
	} // if setting to a coinstructor who owns a course

	QuestionBank qBank;
	synchronized (session) {
		qBank = (QuestionBank) session.getAttribute("qBank");
	}
	if (qBank == null) {
		qBank = (masterEdit ? new QuestionBank() : new QuestionBank(userId));
		synchronized (session) {
			session.setAttribute("qBank", qBank);
		}
	} // if qBank is null

	final Topic[] topics = qBank.getTopics();
	int topicNum = MathUtils.parseInt(request.getParameter("topicNum"));
	Topic topic = (topicNum != 0 ? topics[topicNum - 1] : null);
	QSetDescr[] qSetDescrs = (topicNum != 0 
			? qBank.getQSetDescrs(topicNum) : new QSetDescr[0]);
	QSet qSet = null;
	final boolean refreshQSet = "true".equals(request.getParameter("refreshQSet"));
	if (!refreshQSet) synchronized (session) {
		qSet = (QSet) session.getAttribute(QSET);
	}
	int qSetId = (qSetDescrs.length == 1 ? qSetDescrs[0].id
			: MathUtils.parseInt(request.getParameter("qSetId")));
	if (qSetId != 0) {
		if (qSet == null) { // we do not have a qSet already in session
			// Utils.alwaysPrint("questionsList.jsp: getting ", qSetId);
			qSet = (masterEdit ? new QSet(qSetId) : new QSet(qSetId, userId));
			if (qSet == null) { // shouldn't happen
				Utils.alwaysPrint("questionsList.jsp: qSet is still null!");
			} 
			synchronized (session) {
				session.setAttribute(QSET, qSet);
			}
		} else { // we do have a qSet session bound
			if (qSet.getQSetId() != qSetId) { // request to display another qSet
				qSet = (masterEdit ? new QSet(qSetId) : new QSet(qSetId, userId));
				if (qSet == null) // shouldn't happen
					Utils.alwaysPrint("questionsList.jsp: new qSet is null!");
				synchronized (session) {
					session.setAttribute(QSET, qSet);
				}
			} // not to display another qSet
		} // qSet session is bound
		final String reentry = request.getParameter("reentry");
		if (reentry != null) {
			// Utils.alwaysPrint("questionsList.jsp: reentry = ", reentry);
			if ("delete".equals(reentry)) {
				final int delId = MathUtils.parseInt(request.getParameter("deleteId"));
				qSet.deleteQuestion(delId);
			} else if ("revertQ".equals(reentry)) {
				final int revId = MathUtils.parseInt(request.getParameter("revertId"));
				qSet.revertQuestion(revId);
			} else if ("renumber".equals(reentry)) {
				final String posnsStr = request.getParameter("posns");
				final int[] posns = Utils.stringToIntArray(posnsStr.split(":"));
				qSet.renumberQuestion(posns[0], posns[1]);
			} else if ("revertCommonStmt".equals(reentry)) {
				qSet.revertQSetDescr();
			} // reentry
			synchronized (session) {
				session.setAttribute(QSET, qSet);
				session.removeAttribute("translationObj");
			}
		} // reentering
	} else if (qSet != null && topicNum != 0 
			&& topics[topicNum - 1].id != qSet.getQSetDescr().topicId) {
		// qSet in memory doesn't match topic; remove qSet
		qSet = null;
		synchronized (session) {
			session.removeAttribute(QSET);
		}
	} // qSetId != 0
	QSetDescr qSetDescr = null;
	if (qSet != null) {
		qSetId = qSet.getQSetId();
		qSetDescr = qSet.getQSetDescr();
		if (qSetDescr == null) {
			// question set was just deleted
			qSet = null;
			synchronized (session) {
				session.removeAttribute(QSET);
			}
		} else if (topicNum == 0) {
			for (int topNum = 1; topNum <= topics.length; topNum++) {
				if (topics[topNum - 1].id == qSetDescr.topicId) {
					topicNum = topNum;
					topic = topics[topicNum - 1];
					qSetDescrs = qBank.getQSetDescrs(topicNum);
					break;
				}
			} // for each topic
		} // if we don't know the topic
	} // if qSet is not null

	final Question[] lightQs = 
			(qSet == null ? new Question[0] : qSet.getQuestions());
	final int numQs = lightQs.length;

	final int NUM_ATOMS = EvalManager.NUM_ATOMS;
	final int HAS_FORMULA = EvalManager.HAS_FORMULA;
	final int TEXT_CONT = EvalManager.TEXT_CONT;

	final int QUESTIONS = 0;
	final int TOPIC = 1;
	final int BANK = 2;

	final String INFO_ASSIGN = "If you coinstruct a course that "
			+ "another instructor created, or if you are coauthoring "
			+ "a textbook that another author created, and you want "
			+ "students in that course or reading that "
			+ "textbook to see any new or modified questions that "
			+ "result from your current authoring session, choose "
			+ "that instructor or author from the menu. ";
	final String PLEASE_WAIT = "<span class=\"boldtext\"><br /><br /><br />"
			+ "Loading, please be patient...<\\/span>";
	final String SELECTED = " selected=\"selected\"";

	int jmolNum = 0;
	int mviewNum = 0;

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Authoring Tool</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
		* html body {
			padding:55px 0 0px 0;
		}
	</style>
	<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/jmolStart.js" type="text/javascript"></script>
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
		<!-- the next two resources must be called in the given order -->
	<script src="<%= pathToRoot %>nosession/jsmol/JSmol.min.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>nosession/jsmol/Jmol2.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>

	function getTopic() {
		setInnerHTML('questionList', '<%= PLEASE_WAIT %>');
		document.qSetForm.topics.disabled = 'disabled';
		document.qSetForm.qSets.disabled = 'disabled';
		self.location.href = 'questionsList.jsp?topicNum='
				+ document.qSetForm.topics.value;
	} // getTopic()

	function loadQSet() {
		setInnerHTML('questionList', '<%= PLEASE_WAIT %>');
		document.qSetForm.topics.disabled = 'disabled';
		document.qSetForm.qSets.disabled = 'disabled';
		self.location.href = 'questionsList.jsp?topicNum='
				+ document.qSetForm.topics.value
				+ '&qSetId=' + document.qSetForm.qSets.value;
	} // loadQSet()

	function goToQuestion(qId) {
		self.location.href = 'question.jsp?qId=' + qId;
	} // goToQuestion()

	function jumpToQuestion(qId) {
		var go = 'jumpToQ.jsp?qId=' + qId + '&masterEdit=<%= masterEdit %>';
		self.location.href = go;
	} // jumpToQuestion()

	function addNewQuestion() {
		if (document.qSetForm.qSets.selectedIndex === 0) {
			alert('Please choose a topic and question set before adding a question.');
			return;
		}
		self.location.href = 'question.jsp?addnew=yes';
	} // addNewQuestion()

	function cloneQuestion(qId) {
		self.location.href = 'question.jsp?clone=yes&cloneOld=yes&qId=' + qId;
	}

	function openQuestion(qId) {
		self.location.href = 'question.jsp?qId=' + qId;
	}

	function deleteQuestion(qId) {
		if (confirm("Deletion of this question is an "
				+ "irreversible action.  The grades in any "
				+ "assignments that contain this question "
				+ "will be modified accordingly.  Are you sure "
				+ "you wish to continue? "))
			self.location.href =
					'questionsList.jsp?qSetId=<%= qSetId %>'
					+ '&topicNum=<%= topicNum %>'
					+ '&reentry=delete&deleteId=' + qId;
	}

	function revertQuestion(qId) {
		if (confirm("Reversion of this question to the version in "
				+ "the master database is an irreversible action. "
				+ "Are you sure you wish to continue? "))
			self.location.href =
					'questionsList.jsp?qSetId=<%= qSetId %>'
					+ '&topicNum=<%= topicNum %>'
					+ '&reentry=revertQ&revertId=' + qId;
	}

	function renumberQuestion(from) {
		var to = getValue('renumber' + from);
		self.location.href =
				'questionsList.jsp?qSetId=<%= qSetId %>'
				+ '&topicNum=<%= topicNum %>'
				+ '&reentry=renumber&posns=' + from + ':' + to;
	}

	function loadTopicsEditor() {
		self.location.href = 'qBank/topicsEditor.jsp?qSetId=<%= qSetId %>';
	}

	function addQSet() {
		openQSetWindow('qBank/editQset.jsp?topicNum=<%= topicNum %>&index=0'
				+ '&from=questionsList');
	}

   	function editCommonQStatement() {
		var qSetForm = document.qSetForm;
		var go = new String.builder()
				.append('editCommonQStatement.jsp?master=<%= masterEdit %>')
				.append('&topicNum=')
				.append(qSetForm.topics.selectedIndex)
				.append('&indexInTopic=')
				.append(qSetForm.qSets.selectedIndex)
				.append('&statement=')
				.append(encodeURIComponent(qSetForm.commonQStmtHidden.value));
		openNotesWindow(go.toString());
	} // editCommonQStatement()
	
	function revertCommonQStatement() {
		self.location.href = 'questionsList.jsp?qSetId=<%= qSetId %>'
				+ '&topicNum=<%= topicNum %>'
				+ '&reentry=revertCommonStmt';
	}

	function selectAll(box) {
		setAllCheckBoxes(document.setQsForm.qChecker, box.checked);
	} // selectAll()

	function changeQSet() {
		var qSetForm = document.qSetForm;
		var topicNum = qSetForm.topics.value;
		if (topicNum !== 0) {
			var selSerialNos = getSelectedValues(document.setQsForm.qChecker);
			var numSelected = selSerialNos.length;
			var allowedToMove = new Array();
			var numAllowed = 0;
			for (var selNum = 0; selNum < numSelected; selNum++) { // <!-- >
				var selSerialNo = selSerialNos[selNum];
				var mayMove = getValue('mayMove' + selSerialNo);
				if (mayMove === 'true') allowedToMove[numAllowed++] = selSerialNo;
			} // for each selection
			if (numSelected > 0 && numAllowed > 0) {
				if (numSelected > numAllowed) {
					alert('You selected ' + numSelected 
							+ ' questions to move, but you are permitted '
							+ 'to move only the ' + numAllowed 
							+ ' that you authored yourself.');
				} // if some were disallowed
				var serialNos = allowedToMove.join(':');
				var selectedQsetIndex = qSetForm.qSets.selectedIndex;
				var currentQSetId = 
						qSetForm.qSets.options[selectedQsetIndex].value;
				openEvaluatorWindow('move/changeQSet.jsp?serialNos=' + serialNos
						+ '&currentQSetId=' + currentQSetId
						+ '&topicNum=' + topicNum);
			} else if (numSelected > 0) {
				alert('You are permitted to move only questions that you '
						+ 'have authored yourself.');
			} else alert('Please select questions to move.');
		} else alert('You must select a topic and question set ' 
				+ 'from which to choose questions to move.');
	} // changeQSet()

	function getPersonal() {
		self.location.href = 'enterAuthortool.jsp?getPersonal=true';
	}
	
	function exportSet(howMuch) {
		var qSetForm = document.qSetForm;
		var topicName = '<%= topic != null ? Utils.toValidJS(topic.name) : "" %>';
		if (howMuch === <%= QUESTIONS %>) {
			if (document.getElementById('topics').selectedIndex !== 0) {
				var selSerialNos = getSelectedValues(document.setQsForm.qChecker);
				var serialNos = selSerialNos.join(':');
				var serialNosFilename = '_' + selSerialNos.join('_');
				var topicNum = qSetForm.topics.selectedIndex;
				var selectAllBox = document.getElementById('selectallbox');
				var nameBld = new String.builder()
						.append(topicName).append('_')
						.append('<%= qSetDescr != null 
							? Utils.toValidJS(qSetDescr.name) : "" %>')
						.append(selectAllBox && selectAllBox.checked
							? '' : serialNosFilename);
				var propfilename = nameBld.toString();
				// remove spaces from proposed name, capitalize word beginnings
				filenamePieces = propfilename.split(' ');
				nameBld.clear();
				for (var itemNum = 0; itemNum < filenamePieces.length; itemNum++) { // <!-- >
					var filenamePiece = filenamePieces[itemNum];
					nameBld.append(filenamePiece.substring(0, 1).toUpperCase())
							.append(filenamePiece.substring(1));
				}
				propfilename = nameBld.toString().replace(/;/g, '');
				if (serialNos !== '') {
					var go = new String.builder()
							.append('export/exportSet.jsp?howMuch=')
							.append(howMuch)
							.append('&serialNos=')
							.append(serialNos)
							.append('&propfilename=')
							.append(encodeURIComponent(propfilename));
					openExportWindow(go.toString());
				} else alert('Please select questions for export.');
			} else alert('You must select a topic and question set ' +
				'from which to choose questions to export.');
		} else if (howMuch === <%= TOPIC %>) {
			var topicNum = qSetForm.topics.selectedIndex;
			var propfilename = 'Topic_' + topicName;
			var go = new String.builder()
					.append('export/exportSet.jsp?howMuch=')
					.append(howMuch)
					.append('&serialNos=').append(topicNum)
					.append('&propfilename=')
					.append(encodeURIComponent(propfilename));
			openExportWindow(go.toString());
		} else {
			var go = new String.builder()
					.append('export/exportSet.jsp?howMuch=')
					.append(howMuch)
					.append('&propfilename=QuestionBank');
			openExportWindow(go.toString());
		}
	} // exportSet()

	function importSet(howMuch) {
		openExportWindow('import/browseFile.jsp?howMuch=' + howMuch
				+ '&topicNum=' + document.qSetForm.topics.selectedIndex);
	} // importSet()

	function translateText() {
		self.location.href = 'translation/chooseLanguage.jsp?qSetId=<%= qSetId %>';
	} // translateText()

	function findUntranslatedQs() {
		self.location.href = 'translation/chooseLanguage.jsp?find=true';
	} // findUntranslatedQs()

	function exportAllTranslatables() {
		openExportWindow('translation/exportAllTranslatables.jsp');
	} // exportAllTranslatables()

	<% if (!masterEdit && !assigned && isCoinstructor) { %>
		function INFO_ASSIGN() {
			alert('<%= Utils.toValidJS(INFO_ASSIGN) %>');
		}

		function assignToCoinstructor() {
			var coinstructorId = getValue('coinstructor');
			if (coinstructorId !== '') {
				alert('ACE will assign new and modified questions to your '
						+ 'coinstructor or coauthor in this authoring session '
						+ 'only.  \n\nTo assign work to yourself again, '
						+ 'press Question Bank above.');
				self.location.href =
						'questionsList.jsp?coinstructorId='
						+ encodeURIComponent(coinstructorId);
			} // if there is a coinstructor
		} // assignToCoinstructor()
	<% } // if there are owners of coinstructed courses %>

	function toggleQ(qNum) {
		var box = document.getElementById('qChecker' + qNum);
		box.checked = !box.checked;
	} // toggleQ

	jmolInitialize('<%= pathToRoot %>nosession/jsmol'); 

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
		<% for (int qNum = 1; qNum <= numQs; qNum++) { %>
			if (cellExists('figData<%= qNum %>')) {
				figuresData.push([
						getValue('figData<%= qNum %>'),
						getValue('figQFlags<%= qNum %>'),
						<%= qNum %>]);
			} // if the question has a Lewis figure
		<% } // for each question %>
		if (!isEmpty(figuresData)) {
			loadLewisInlineImages('<%= pathToRoot %>', figuresData);
		}
	} // initLewis()

	// -->
	</script>
</head>
<body class="light" onload="initLewis();" style="text-align:center;
		margin:0px; margin-bottom:3px; background-color:white;">

	<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

<div id="contentsWithoutTabs">
<form name="qSetForm" action="jumpToQ.jsp" method="post" >
<table style="width:95%; margin-left:auto; margin-right:auto;" summary="">
	<tr style="vertical-align:middle; text-align:left;">
<% final String guideLink = "&nbsp;&nbsp;&nbsp;<span class=\"regtext\">"
		+ "[<a href=\"" + pathToRoot + "public/authoring.html\" target=\"window2\">"
		+ "Guide to authoring</a>]</span>";
if (masterEdit) { %>
	<td class="boldtext" style="background-color:<%= bgColor %>;
			border-style:solid; border-width:1px; width:50%; padding-left:5px;">
		<table style="width:100%" summary=""><tr>
		<td class="boldtext big">
			<a name="top"></a>
			Master Authoring Tool
		</td><td style="text-align:center;">
			<%= guideLink %>
		</td><td style="text-align:right;">
			[<a href="javascript:getPersonal()">Change to Personal Authoring Tool</a>]
		</td>
		</tr></table>
	</td>
<% } else { // not masterEdit %>
	<td>
		<table style="width:100%" summary=""><tr>
		<td class="boldtext big">
			<a name="top"></a>
			Authoring Tool
		</td><td class="boldtext"
				style="text-align:center; color:green;">
		<% if (assigned) { %>
			(work assigned to <%= UserRead.getUser(userId).getName().toString() %>)
		<% } %>
		</td>
		<td class="boldtext" style="text-align:right;">
			<%= guideLink %>
		</td>
		</tr></table>
	</td>
<% } // if masterEdit %>
	</tr>
	<tr>
	<td colspan="2" style="vertical-align:top; text-align:center;">
		<table class="whiteTable"
				style="width:100%; background-color:<%= bgColor %>;" summary="">
		<tr>
		<td style="width:50%; vertical-align:top;">
			<table summary="">
			<tr>
			<td class="boldtext" style="padding-left:10px;
					padding-right:20px;
					vertical-align:top; padding-top:6px;">
				Select the topic:
			</td>
			<td class="boldtext" style="padding-right:10px;
					vertical-align:top;
					padding-top:6px; text-align:right;">
				[<a href="javascript:loadTopicsEditor()">Edit
				topics &amp; question sets</a>]
			</td>
			</tr>
			<tr>
			<td colspan="2" class="boldtext" style="padding-left:10px;
					padding-right:10px; vertical-align:top;">
				<select id="topics" name="topics" style="width:375px;"
						onchange="getTopic();">
					<option value="0">Select a topic</option>
					<% for (int topNum = 1;
							topNum <= topics.length; topNum++)  {
						final Topic aTopic = topics[topNum - 1]; %>
						<option value="<%= topNum %>" <%=
							topNum == topicNum ? SELECTED : "" %>>
						<%= Utils.toPopupMenuDisplay(aTopic.name) %>
						</option>
					<% } // for topNum %>
				</select>
			</td>
			</tr>
			<tr>
			<td colspan="2" class="boldtext"
					style="padding-left:10px; vertical-align:top;">
				Select the question set: 
				<% if (topicNum != 0) { %>
					<%= makeButtonIcon("add", pathToRoot, "addQSet();") %>
				<% } // if a topic has been chosen %>
			</td>
			</tr>
			<tr>
			<td colspan="2" class="boldtext" style="padding-left:10px;
					padding-right:10px; vertical-align:top; padding-bottom:6px;">
				<select id="qSets" name="qSets"
						style="width:375px;" onchange="loadQSet();">
					<% if (topicNum == 0) { %>
						<option value="0">
							Choose a topic to view the question sets.
						</option>
					<% } else { 
						final int numQSetsInTopic = qSetDescrs.length;
						if (numQSetsInTopic > 0) { %>
							<option value="0">Select a question set</option>
							<% for (int qSetNum = 1; 
									qSetNum <= numQSetsInTopic; qSetNum++) { 
								final int thisQSetId = qSetDescrs[qSetNum - 1].id;
								%>
								<option value="<%= thisQSetId %>"
										<%= qSetId == thisQSetId ? SELECTED : "" %> >
									<%= Utils.toPopupMenuDisplay(
											qSetDescrs[qSetNum - 1].name) 
									+ (numQSetsInTopic == 1 ? " [only set]" : "") %>
								</option>
							<% } // for each qSet %>
						<% } else { %>
							<option value="0">
								There is no question set in this topic.
							</option>
						<% } // if there are qSets %>
					<% } // if a topic has been chosen %>
				</select>
			</td>
			</tr>
			<tr>
			<td colspan="2" class="boldtext"
					style="padding-left:10px; vertical-align:top;">
				<table class="boldtext" summary="">
					<tr>
					<td>
						Or go directly to question number
						<input type="text" size="5" name="qId" id="direct" />
						<input type="hidden" name="masterEdit" 
								value="<%= masterEdit %>" />
					</td>
					<td>
					<%= makeButton("Go", "jumpToQuestion(getValue('direct'));") %>
					</td>
					</tr>
				</table>
			</td>
			</tr>
			</table>
		</td>
		<td style="padding-right:10px; vertical-align:top;
				padding-top:4px; padding-bottom:10px;">
			<table class="whiteTable" style="height:110px; width:100%; overflow:scroll"
					border="0" summary="">
			<tr><td id="infoframe">
	<% 			if (qSetDescr != null) { 
					/* Utils.alwaysPrint("questionsList.jsp: qSetDescr.header = ", 
							qSetDescr.header); /**/
	%>
					<table summary="" style="width:100%; text-align:center;  
							margin-left:auto; margin-right:auto;"> 
					<tr><td class="boldtext" style="vertical-align:top;  
							padding-left:15px;  padding-right:15px;">
						<br />
						<%= Utils.toDisplay(
								qSetDescr.topicName.replace("[null]", "")) %>
						: <%= Utils.toDisplay(qSetDescr.name) %>
					</td></tr> 
					<tr><td style="vertical-align:top; text-align:left;  
							padding-left:15px; padding-right:15px;"> 
						<span class="boldtext">Remarks:</span>&nbsp; 
						<span class="regtext">
						<%= qSetDescr.remarks.length() == 0 
								? "[None]" : qSetDescr.remarks %>
						</span>
					</td></tr> 
					<tr><td style="vertical-align:top; text-align:left;  
							padding-left:15px; padding-right:15px;"> 
						<span class="boldtext"> 
						Statement to be repeated for every question:
						</span>
						<%= makeButtonIcon("edit", pathToRoot, 
								"editCommonQStatement();") %>
						<span id="revertButton" <%= 
								!qSetDescr.headerModifiedLocally 
								? " style=\"visibility:hidden;\"" : "" %>>
						<%= makeButtonIcon("revert", pathToRoot, 
								"revertCommonQStatement();") %>
						</span> 
						<br/>
						<input type="hidden" name="commonQStmtHidden"
								id="commonQStmtHidden"
								value="<%= Utils.toValidHTMLAttributeValue(
										qSetDescr.header) %>" />
						<span id="commonQstatement" class="regtext">
						<%= qSetDescr.header.length() == 0 ? "[None]"
								: Utils.toDisplay(qSetDescr.header) %>
						</span> 
					</td></tr>
					</table>
	<%			} else { %>
	 				<table summary="" style="width:100%; height:100%;">
					<tr><td class="boldtext" style="text-align:center; 
							vertical-align:middle;">
						Select a topic and question set from the left.
					</td></tr>
					</table>
	<%			} // if qSetDescr is not null %>
			</td></tr>
			</table>
		</td>
		</tr>
		<tr>
		<td colspan="2" style="padding-left:10px; vertical-align:top;
				padding-bottom:10px;">
			<table style="padding-left:0px;" summary="">
			<tr>
			<% if (qSetId != 0) { %>
				<td style="padding-left:0px">
					<%= makeButton("Add question", "addNewQuestion();") %>
				</td>
			<% } // if topic and qSet have been chosen %>
			<td id="importButton">
				<% if (topicNum == 0 && qSetId == 0) { %>
					<%= makeButton("Import topics", "importSet(", BANK, ");") %>
				<% } else if (qSetId == 0) { %>
					<%= makeButton("Import sets", "importSet(", TOPIC, ");") %>
				<% } else { %>
					<%= makeButton("Import questions", "importSet(", QUESTIONS, ");") %>
				<% } // if topic and qSet have been chosen %>
			</td>
			<% if (!Utils.among(0, topicNum, qSetId)) { %>
				<td class="boldtext" style="padding-left:20px;">
					Select all <input type="checkbox" id="selectallbox"
					onclick="javascript:selectAll(this)" />
				</td>
				<td>
					<%= makeButton("Relocate selected", "changeQSet();") %>
				</td>
			<% } // if topic and qSet have been chosen %>
			<td id="exportButton">
				<% if (topicNum == 0 && qSetId == 0) { %>
					<%= makeButton("Export all", "exportSet(", BANK, ");") %>
				<% } else if (qSetId == 0) { %>
					<%= makeButton("Export topic", "exportSet(", TOPIC, ");") %>
				<% } else { %>
					<%= makeButton("Export selected", "exportSet(", QUESTIONS, ");") %>
				<% } // if topic and qSet have been chosen %>
			</td>
			<% if (!masterEdit && !assigned && isCoinstructor) {
			%>
				<td class="boldtext" style="padding-left:10px;">
					<a href="javascript:INFO_ASSIGN();"
							title="<%= INFO_ASSIGN %>">Assign to</a>:
				</td>
				<td>
					<select id="coinstructor"
							name="coinstructor"
							onchange="assignToCoinstructor();">
						<option value="">Select a coinstructor or coauthor</option>
						<% for (final User coinstructedCourseOwner : owners) { %>
							<option value="<%=
								coinstructedCourseOwner.getUserId() %>"><%=
								coinstructedCourseOwner.getName().toString()
							%></option>
						<% } // for each coinstructor %>
					</select>
				</td>
			<% } // if there are owners of coinstructed courses %>
			<% if (isTranslator) { %>
				<td id="translateButton">
					<%= makeButton("Translate " + (qSet == null 
							? "general" : "set"), "translateText();") %>
				</td>
				<% if (topicNum == 0 || qSetId == 0) { %>
					<td id="exportAllTranslatablesButton">
						<%= makeButton("Export all translatables",
								"exportAllTranslatables();") %>
					</td>
					<td id="findUntranslatedQsButton">
						<%= makeButton("Find untranslated questions",
								"findUntranslatedQs();") %>
					</td>
				<% } // if topic and qSet have been chosen %>
			<% } // if the user is a translator %>
			</tr>
			</table>
		</td>
		</tr>
		</table>
	</td>
	</tr>
</table>
</form>

<img src="<%= pathToRoot %>images/border.jpg" alt="" style="width:100%; height:1px;" />
<div id="questionList">
<form name="setQsForm" action="" method="post">
	<% if (qSet != null) { %>
	<table summary="" style="width:95%; margin-left:auto; 
			margin-right:auto; text-align:left;">
	<tr><td class="boldtext big">
		Questions&nbsp;&nbsp;&nbsp;</td>
	<td class="regtext" style="text-align:right;">
		<% for (int qNum = 1; qNum <= numQs; qNum++) { %>
			[<a href="#Q<%= qNum %>"><%= qNum %></a>]
		<% } // for each qNum %>
	</td></tr>
	<% if (!Utils.isEmpty(lightQs)) { %>
		<tr><td class="regtext" colspan="2"><p>You may not
			be able to delete some or all questions in the set.  
			You may modify any question or duplicate it 
			and modify the duplicate.  </p>
		 </td></tr>
	<% } // if set is occupied
	int firstMovePosn = (masterEdit ? 1 : 0);
	for (int qNum = 1; qNum <= numQs; qNum++) {
		final Question lightQ = lightQs[qNum - 1];
		if (lightQ == null) continue;
		final int qId = lightQ.getQId();
		if (lightQ.isCorrupted()) {
			Utils.alwaysPrint("Ack! questionsList.jsp: Q ", qId, 
					" is corrupted! ");
	%>
			<tr><td class="regtext">
				<a name="Q<%= qNum %>"></a>
	<%			if (qNum == 1) { %> <br/> <% } %>
				Question <%= qNum %> is corrupted; delete me, please!&nbsp;&nbsp;
				<%= makeButtonIcon("delete", pathToRoot,
						"deleteQuestion(", qId, ");") %>
				<p>&nbsp;</p>
			</td></tr>
	<%		continue;
		} // if Q is corrupted
		if (!masterEdit && lightQ.hide()) {
	%>
			<tr><td class="regtext">
				<a name="Q<%= qNum %>"></a>
	<%			if (qNum == 1) { %> <br/> <% } %>
				Question <%= qNum %> is not yet available.
				<p>&nbsp;</p>
			</td></tr>
	<%		continue;
		} // if hide
		String quality = "";
		if (!masterEdit) {
			if (lightQ.isNew()) {
				if (firstMovePosn <= 0) firstMovePosn = qNum;
				quality = "New";
			} else if (lightQ.isModified()) quality = "Modified";
		} // !masterEdit
		final String style = quality.toLowerCase(Locale.US);
		final boolean chemFormatting = lightQ.chemFormatting();
	%>
		<!-- Begin Q display -->
		<tr><td colspan="2">
			<table summary="" class="whiteTable"
					style="width:100%;background-color:<%= bgColor %>;">
			<tr><td colspan="3" style="vertical-align:middle;">
				<a name="Q<%= qNum %>"></a>
				<table summary="" style="width:100%; background-color:<%= bgColor %>;">
				<tr>
	<!--  display Q buttons -->
				<td style="vertical-align:middle;">
					<% if (masterEdit || lightQ.isNew()) { %>
						<select id="renumber<%= qNum %>"
								onchange="javascript:renumberQuestion(<%= qNum %>);">
						<% for (int rNum = firstMovePosn; rNum <= numQs; rNum++) { %>
							<option value="<%= rNum %>" <%= 
									rNum == qNum ? SELECTED : "" %>>
								<%= rNum %></option>
						<% } // for each valid renumber position %>
						</select>
					<% } else { %>
						<span class="boldtext notnewormodified" style="height:22px">
						<%= qNum %>.</span>
					<% } // if Q can be renumbered %>
					<% if (!Utils.isEmpty(quality)) { %>
						<span class="<%= style %>" style="height:22px">
						<%= quality %>
						</span>
					<% } %>
					<%= makeButtonIcon("edit", pathToRoot, "openQuestion(", qId, ");") %>
	<%				if (!masterEdit) {
						if (lightQ.isNew() && !qSet.isAssignedAsRandom(qNum)) { %>
							<%= makeButtonIcon("delete", pathToRoot,
									"deleteQuestion(", qId, ");") %>
	<%					} else if (lightQ.isModified()) { %>
							<%= makeButtonIcon("revert", pathToRoot,
									"revertQuestion(", qId, ");") %>
	<%					} // if editType
					} else if (!qSet.isAssignedAsRandom(qNum)) { %>
						<%= makeButtonIcon("delete", pathToRoot,
								"deleteQuestion(", qId, ");") %>
	<%				} // if masterEdit %>
					<%= makeButtonIcon("duplicate", pathToRoot,
							"cloneQuestion(", qId, ");") %>
					&nbsp;&nbsp;&nbsp;
					<a class="boldtext" onclick="toggleQ(<%= qNum %>);">Select</a>
					<input type="checkbox" id="qChecker<%= qNum %>" 
							name="qChecker" value="<%= qNum %>" />
					<input type="hidden" id="mayMove<%= qNum %>"
							value="<%= masterEdit || lightQ.isNew() %>" />
	<%				if (qNum > 1) { %>
						<span class="regtext">
						&nbsp;&nbsp;&nbsp;<a href="#top"><img src="<%= pathToRoot 
								%>images/top.png" title="Go to top" alt="Top"></a>
						</span> 
	<%				} // if masterEdit %>
				</td>
	<!--  display Q provenance -->
				<td style="vertical-align:middle; text-align:center;">
					<span class="regtext">
	<%				if (lightQ != null) {
						final String book = lightQ.getBook();
						final String chap = lightQ.getChapter();
						final String remarks = lightQ.getRemarks(); 
						if (book != null && "Other".equals(book)) { %>
							Question by <%= chap %>
	<%					} else if (book != null && "Literature".equals(book)) { 
							if (!Utils.isEmpty(remarks)) { %>
								<a href="<%= remarks %>" target="window2"><%= chap %></a>
	<%						} else { %>
								<%= chap %>
	<%						} // if there's a URL 
						} else { %>
							<%= book %>, Chap. <%= chap 
								+ (remarks != null 
									&& !"[None]".equals(remarks) 
								? ", " + remarks : "") %>
	<%					} // if not from a Pearson book
					} else {
						Utils.alwaysPrint("questionsList.jsp: lightQ is null");
					} // if lightQ is not null %>
					</span>
				</td>
	<!--  display Q type & number -->
				<td style="vertical-align:middle; text-align:right;">
					<span class="regtext">
					<%= lightQ.getQTypeDescription() %>, ID #<%= 
							Utils.formatNegative(qId) %>
				</span>
				</td></tr>
				</table>
			</td></tr>
	<!--  display Q statement -->
			<tr><td class="regtext" style="width:80%; vertical-align:middle;"><br/>
				<%= lightQ.getDisplayStatement(!Question.CONVERT_VARS_TO_VALUES) %>
			</td>
	<!--  display Q figure -->
			<td rowspan="2" style="padding-right:10px; vertical-align:middle; 
					text-align:center;">
	<%			final int numFigures = lightQ.getNumFigures();
				if (numFigures > 0) {
					final Figure figure = lightQ.getFigure(1);
					final String[] figData = (figure.isSynthesis() 
							? figure.getDisplayData(
									Synthesis.getRxnsDisplayPhrases())
							: figure.getDisplayData()); %>
					<table summary=""><tr><td style="width:100%;">
						<table style="width:100%;" summary="">
						<tr>
	<%					if (numFigures > 1) { %>
							<td class="boldtext" style="width:100%;">
								Fig 1 of <%= numFigures %>
							</td>
	<%					} // if numFigures > 1
						if (figure.isImageAndVectors()) { %>
							<td class="boldtext" style="text-align:right;">
								ACE will display vectors on this image.
							</td>
						<% } // if figure type %>
						</tr>
						</table>
					</td></tr>
					<tr><td>
					<table class="whiteTable" summary=""  
							style="margin-left:0px; margin-right:0px;">
	<%					if (figure.isJmol()) { 
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
	<%					} else if (figure.isLewis()) { %>
							<tr><td id="fig<%= qNum %>">
							<input type="hidden" id="figData<%= qNum %>"
									value="<%= Utils.toValidHTMLAttributeValue(
										figData[Figure.STRUCT]) %>" />
							<input type="hidden" id="figQFlags<%= qNum %>"
									value="<%= lightQ.getQFlags() %>" />
							</td></tr>
	<%					} else if (!figure.hasImage()) { %>
							<tr><td id="fig<%= qNum %>">
								<%= figure.getImage(pathToRoot, user.prefersPNG(), 
										lightQ.getQFlags(), qNum) %>
							</td>
	<%						if (figure.isReaction()) { 
								final String above = Utils.toDisplay(figData[Figure.RXN_ABOVE]);
								final String below = Utils.toDisplay(figData[Figure.RXN_BELOW]);
								final int arrowSize = 36; %>
								<td>
									<%@ include file="/includes/reactionArrow.jsp.h" %>
								</td></tr>
	<%					 	} else { %>
								</tr>
	<%							if (figure.isSynthesis()) { %>
									<%= figData[Synthesis.RXNID] %>
	<%							} // if synthesis
							} // lightQ.figureType
						} else { // Figure.IMAGE or IMAGE_AND_VECTORS %>
							<tr><td>
								<img src="<%= pathToRoot %><%= figure.bufferedImage %>" 
										alt="picture"
										onload="fixTheImageSize(this,250);" />
							</td></tr>
	<%					} // if figure.type %>
					</table>
					</td>
					</tr>
					</table>
	<%			} // numFigures > 0 %>
			</td></tr>
	<!--  display Q evaluators -->
			<tr><td style="background-color:<%= bgColor %>; 
					vertical-align:top; padding-left:10px;">
				<table summary="">
	<%				final Evaluator[] evaluators = lightQ.getAllEvaluators();
					final String[] qDataTexts = lightQ.getQDataTexts();
					final boolean usesSubstns = lightQ.usesSubstns();
					for (final Evaluator eval : evaluators) { 
						final boolean isHumanReqd = eval.isHumanGradingReqd(); 
						final boolean calcsGrade = eval.calculatesGrade();
						final String colorGrd = (isHumanReqd || calcsGrade ? "gray"
								: eval.grade == 1 ? "color:#008800;"
								: eval.grade == 0 ? "color:#CC0000;"
								: "color:#0000CC;"); 
						final String symb = (isHumanReqd ? "[?]"
								: calcsGrade ? "<i>x</i>%"
								: eval.grade == 1 ? "[C]"
								: eval.grade == 0 ? "[W]"
								: "[" + ((int) (eval.grade * 100)) + "%]"); %>
						<tr><td>
						<span class="boldtext" style="<%= colorGrd %>">
							<%= symb %>
						</span>
	<% 					if (eval.isComplex()) { %>
								<%= eval.exprCodeToEnglish() %>
							</td></tr>
	<%					} // if the evaluator is complex, has an expression code
						final List<Subevaluator> subevals =
								eval.getSubevaluators();
						final String openTag = (eval.isComplex()
								? "<tr><td class=\"regtext\" "
									+ "style=\"padding-left:20px;\">"
								: "<span class=\"regtext\">");
						final String closeTag = (eval.isComplex()
								? "</td></tr>"
								: "</span></td></tr>");
						int subevalNum = 0;
						for (final Subevaluator subeval : subevals) {
							final int matchType = subeval.getEvalType(); %>
							<%= openTag %>
							<%= !eval.isComplex() ? ""
									: "<i>" + ++subevalNum + "</i>.&nbsp;" %>
							<%= matchType == TEXT_CONT || !chemFormatting
									? subeval.toEnglish(qDataTexts)
									: Utils.toDisplay(
										subeval.toEnglish(qDataTexts), 
										usesSubstns) 
										%>
	<%						if (usesSubstns) { %> 
								<%= Utils.toValidJS(
										subeval.getRGroupsExcludedText()) %>
	<% 						} // if R-group question %>
							<%= closeTag %>
	<%					} // for each subevaluator
					} // for each evaluator %>
				</table>
			</td></tr>
			</table>
		</td></tr>
		<tr><td>
			<br/>
		</td></tr>
	<% } // for each qNum %>
	</table><br/>
		<% if (lightQs.length == 0) { %>
			This question set is empty.  Press the Add Question button 
			above to add a question.
		<% } // if there are no Qs %>
	<% } else { // if qset is null %>
		<table summary="" style="width:100%; height:100%;">
			<tr><td class="boldtext" style="text-align:center; 
					vertical-align:middle;">
				<br /><br /><br /><br /><br />
				<img id="image" src="<%= pathToRoot %>images/ace_nonanim.gif" alt="" />
				<br />Select a topic and question set from above.
			</td></tr>
		</table>
	<% } // if qset is not null%>
</form>
</div>

</div>
</body>
</html>

