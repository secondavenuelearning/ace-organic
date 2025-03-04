<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.evals.Evaluator,
	com.epoch.qBank.QDatum,
	com.epoch.qBank.QSetDescr,
	com.epoch.qBank.Question,
	com.epoch.qBank.Topic,
	com.epoch.session.QSet,
	com.epoch.session.QuestionBank,
	com.epoch.translations.QSetTransln,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.List,
	java.io.DataOutputStream,
	java.io.File,
	java.io.FileOutputStream,
	java.io.IOException"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<% 
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";
	final int GET_ALL = 0;
	final int GET_UNIQUE = 1;
	final int GET_UNTRANSLATED = 2;

	QuestionBank qBank;
	synchronized (session) {
		qBank = (QuestionBank) session.getAttribute("qBank");
	}
	user.refreshLanguages();
	final String[] myLanguages = user.getLanguages();
	final int numLanguages = myLanguages.length;
	final int actionType = MathUtils.parseInt(request.getParameter("actionType"));
	final String chosenLang = request.getParameter("chosenLang");
	String propfilename = request.getParameter("propfilename");
	boolean settingName = true;
	boolean success = false;
	if (propfilename == null) {
		propfilename = Utils.toValidFileName("allQSetsInEnglish.txt");
	} else {
		/* Utils.alwaysPrint("exportAllTranslatables.jsp: actionType = ", 
				actionType, ", chosenLang = ", chosenLang); /**/
		final List<String> allHeaders = new ArrayList<String>();
		final List<String> allStmts = new ArrayList<String>();
		final List<String> allFeedbacks = new ArrayList<String>();
		final List<String> allQDataTexts = new ArrayList<String>();
		settingName = false;
		final String fullPath = Utils.toString(AppConfig.appRoot, 
				AppConfig.appRoot.endsWith("/") ? "" : "/", AppConfig.relTempDir, 
				AppConfig.relTempDir.endsWith("/") ? "" : "/", propfilename);
		final File outFile = new File(fullPath);
		final DataOutputStream dos = new DataOutputStream(
				new FileOutputStream(outFile));
		try {
			final Topic[] topics = qBank.getTopics();
			for (final Topic topic : topics) {
				final String topicName = (topic.name == null ? "" : topic.name.trim());
				if (Utils.isEmpty(topicName) || Utils.among(topicName, 
						"Development questions", "Exams", 
						"Questions from AWRORM")) continue;
				/* Utils.alwaysPrint("exportAllTranslatables.jsp: topic = ",
						topicName); /**/
				final QSetDescr[] qSetDescrs = topic.getQSetDescrs();
				for (final QSetDescr qSetDescr : qSetDescrs) {
					/* Utils.alwaysPrint("exportAllTranslatables.jsp: qSetDescr = ",
							qSetDescr.name); /**/
					final QSet qSet = new QSet(qSetDescr.id);
					final QSetTransln translator = 
							new QSetTransln(qSet, chosenLang);
					String exportTxt = Utils.toString("Topic: ", topic.name, 
							"; Set: ", qSetDescr.name, '\n');
					dos.writeBytes(exportTxt);
					// format: type [tab] Q number [tab] item number [tab] data
					// export header
					if (!Utils.isEmpty(qSetDescr.header)) {
						final boolean write = (actionType == GET_ALL
								|| (actionType == GET_UNTRANSLATED 
									&& Utils.isEmpty(translator.header)
									&& !allHeaders.contains(qSetDescr.header.trim()))
								|| (actionType == GET_UNIQUE
									&& !allHeaders.contains(qSetDescr.header.trim())));
						/* Utils.alwaysPrint("exportAllTranslatables.jsp: "
								+ "header transln = ", translator.header,
								write ? "; writing" : "; not writing"); /**/
						if (write) {
							exportTxt = Utils.toString(QSetTransln.HEADER_TAG, 
									"\t\t\t", qSetDescr.header, '\n');
							dos.writeBytes(exportTxt);
							if (actionType != GET_ALL) {
								allHeaders.add(qSetDescr.header.trim());
							} // if only get unique
						} // if GET_ALL or untranslated or header is unique
					} // if there is a translatable header
					// export Q statements
					final Question[] setQs = qSet.getQuestions();
					for (int qNum = 0; qNum < setQs.length; qNum++) {
						final Question setQ = setQs[qNum];
						final String qStmt = setQ.getStatement(); 
						if (!Utils.isEmpty(qStmt)) {
							final String translatedQStmt = translator.qStmts[qNum];
							final boolean write = (actionType == GET_ALL 
									|| (actionType == GET_UNTRANSLATED 
										&& Utils.isEmpty(translatedQStmt)
										&& !allStmts.contains(qStmt.trim()))
									|| (actionType == GET_UNIQUE
										&& !allStmts.contains(qStmt.trim())));
							/* Utils.alwaysPrint("exportAllTranslatables.jsp: qNum = ",
									qNum + 1, ", transln = ", translatedQStmt,
									write ? "; writing" : "; not writing"); /**/
							if (write) {
								exportTxt = Utils.toString(QSetTransln.QSTMT_TAG, 
										'\t', qNum + 1, "\t\t", qStmt, '\n');
								dos.writeBytes(exportTxt);
								if (actionType != GET_ALL) {
									allStmts.add(qStmt.trim());
								} // if only get unique
							} // if GET_ALL or untranslated or is unique
						} // if there is a translatable Q statement
						// export qData 
						final QDatum[] qData = (setQ.hasTranslatableQData() 
								? setQ.getQData(Question.GENERAL) : new QDatum[0]); 
						for (int qdNum = 0; qdNum < qData.length; qdNum++) {
							final QDatum qDatum = qData[qdNum];
							final String qdText = (qDatum.isMarvin() 
									? qDatum.name : qDatum.data);
							if (!Utils.isEmpty(qdText)) { 
								final String translatedQDText = 
										translator.qdTexts[qNum][qdNum];
								final boolean write = (actionType == GET_ALL
										|| (actionType == GET_UNTRANSLATED 
											&& Utils.isEmpty(translatedQDText)
											&& !allQDataTexts.contains(qdText.trim()))
										|| (actionType == GET_UNIQUE
											&& !allQDataTexts.contains(qdText.trim())));
								/* Utils.alwaysPrint("exportAllTranslatables.jsp: "
										+ "qdNum = ", qdNum + 1, ", qdText = ", qdText,
										", translatedQDText = ", translatedQDText,
										write ? "; writing" : "; not writing"); /**/
								if (write) {
									exportTxt = Utils.toString(
											QSetTransln.QDTEXT_TAG, 
											'\t', qNum + 1, '\t', qdNum + 1, 
											'\t', qdText, '\n');
									dos.writeBytes(exportTxt);
									if (actionType != GET_ALL) {
										allQDataTexts.add(qdText.trim());
									} // if only get unique
								} // if GET_ALL or untranslated or is unique
							} // if there is a translated Q datum
						} // for each question datum
						// export evaluators
						final Evaluator[] evals = setQ.getAllEvaluators(); 
						for (int evalNum = 0; evalNum < evals.length; 
								evalNum++) {
							final Evaluator eval = evals[evalNum];
							if (!Utils.isEmpty(eval.feedback)) {
								final String translatedEval =
										translator.evalFeedbacks[qNum][evalNum];
								final boolean write = (actionType == GET_ALL 
										|| (actionType == GET_UNTRANSLATED 
											&& Utils.isEmpty(translatedEval)
											&& !allFeedbacks.contains(eval.feedback.trim()))
										|| (actionType == GET_UNIQUE
											&& !allFeedbacks.contains(eval.feedback.trim())));
								/* Utils.alwaysPrint("exportAllTranslatables.jsp: "
										+ "evalNum = ", evalNum + 1, 
										", translatedEval = ", translatedEval,
										write ? "; writing" : "; not writing"); /**/
								if (write) {
									exportTxt = Utils.toString(
											QSetTransln.FEEDBACK_TAG, 
											'\t', qNum + 1, '\t', evalNum + 1, 
											'\t', eval.feedback, '\n');
									dos.writeBytes(exportTxt);
									if (actionType != GET_ALL) {
										allFeedbacks.add(eval.feedback.trim());
									} // if only get unique
								} // if GET_ALL or untranslated or is unique
							} // if there is a translated evaluator
						} // for each eval
					} // for each Q
				} // for each Qset
			} // for each topic
			dos.close();
			success = outFile.exists();
			propfilename = Utils.toString(pathToRoot, AppConfig.relTempDir,
					AppConfig.relTempDir.endsWith("/") ? "" : '/',
					Utils.toValidHref(propfilename)).replaceAll(";", "");
			// Utils.alwaysPrint("exportAllTranslatables.jsp: href is ", propfilename);
		} catch (IOException e) {
			Utils.alwaysPrint("exportAllTranslatables.jsp: IOException caught "
					+ "during export:\n", e.getMessage());
			e.printStackTrace();
		} // try
	} // if there's a proposed file name
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Exporter of Translatable Parts of Questions</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
	<script type="text/javascript">
		// <!-- >
		<% if (settingName) { %>
			function reload(actionType) {
				var form = document.filenameform;
				var filename = 
						trimWhiteSpaces(form.propfilename.value);
				if (isWhiteSpace(filename)) {
					alert('Please enter a filename.');
					return;
				}
				form.propfilename.value = filename;
				form.actionType.value = actionType;
				form.submit();
			} // reload
		<% } %>

		function closeMe() {
			self.close();
		}
		// -->
	</script>
</head>
<body class="light">

<% if (settingName) { %>
	<form name="filenameform" action="exportAllTranslatables.jsp" method="post">
	<input type="hidden" name="actionType" value="" />
	<table class="regtext" style="width:95%; margin-left:auto; margin-top:10px;
			margin-right:auto; border-style:none; border-collapse:collapse; 
			padding-top:10px; padding-left:10px;">
	<tr><td class="boldtext big" style="padding-left:10px;">
		Export translatable parts of questions
	</td></tr>
	<tr><td style="text-align:left; vertical-align:middle; padding-left:20px;">
		<br/>Please enter a name for the export file, or use the suggested one.
		(If the suggested name includes non-ASCII characters, please change them
		to ASCII characters before proceeding.)
	</td></tr>
	<tr><td style="padding-top:10px; text-align:left; 
			vertical-align:middle; padding-left:20px;">
		<input type="text" name="propfilename"
				style="width:400px; height:20px;"
				value="<%= Utils.toValidTextbox(propfilename) %>" />
	</td></tr>
	<tr><td style="text-align:left; vertical-align:middle; padding-left:20px;">
		<table>
		<tr>
		<td>
			<table>
			<tr>
			<td style="padding-top:10px; padding-left:10px;">
				<%= makeButton("Get all phrases", 
						Utils.toString("reload(", GET_ALL, ");")) %>
			</td>
			<td style="padding-top:10px; padding-left:10px;">
				<%= makeButton("Get unique phrases", 
						Utils.toString("reload(", GET_UNIQUE, ");")) %>
			</td>
			</tr>
			</table>
		<td>
		<tr>
		<td style="padding-top:10px; padding-left:10px;">
			<%= makeButton(Utils.toString("Get unique phrases not translated into ",
						numLanguages == 1 ? myLanguages[0] : ""), 
					Utils.toString("reload(", GET_UNTRANSLATED, ");")) %>
		</td>
		<td style="padding-top:10px; padding-left:10px;">
			<% if (numLanguages > 1) { %>
				<select name="chosenLang">
					<% for (final String lang : myLanguages) { %>
						<option value="<%= Utils.toValidHTMLAttributeValue(lang) %>">
						<%= lang %>
						</option>
					<% } // for each language %>
				</select>
			<% } else { %>
				<input type="hidden" name="chosenLang" 
					value="<%= Utils.toValidHTMLAttributeValue(myLanguages[0]) %>" />
			<% } // if numLanguages %>
		</td>
		</tr>
		<tr>
		<td style="padding-top:10px; padding-left:10px;">
			<%= makeButton("Close", "closeMe();") %>
		</td>
		</tr>
		</table>
	</td></tr>
	</table>
	</form>
<% } else if (success) { %>
	<table class="regtext" style="width:95%; margin-top:10px; margin-left:auto;
			margin-right:auto; border-style:none; border-collapse:collapse;">
	<tr><td class="boldtext" style="padding-left:10px; padding-bottom:10px;">
		ACE has successfully exported the 
		translatable parts of all question sets 
		<%= actionType == GET_UNIQUE ? "that are unique" 
			: actionType == GET_UNTRANSLATED 
			? Utils.toString("that have not been been translated into ", chosenLang)
			: "" %>
		into a text file.
	</td></tr>
	<tr><td style="padding-left:10px;">
		<span class="boldtext">PC</span>
			&mdash; Right-click on the link below and select 
			"Save Target As..." to download the file to your disk.<br/>
		<span class="boldtext">Mac</span>
			&mdash; Control-click on the link below and select "Download Linked
			File" or "Download Linked File As..." to download the file to 
			your disk.<br/>
		<br/>
		[<a onclick="alert('Please follow the directions above the link.'); return false"
			href="<%= propfilename %>">Exported phrases (.txt)</a>]
	</td></tr>
	<tr><td style="padding-top:10px; padding-left:10px;">
		<%= makeButton("Close", "closeMe();") %>
	</td></tr>
	</table>
<% } else {  %>
	<table class="regtext" style="width:95%; margin-left:auto; margin-top:10px;
			margin-right:auto; border-style:none; border-collapse:collapse;">
	<tr><td class="boldtext big" >
		Export translatable parts of questions
		</td></tr>
	<tr><td style="padding-left:10px;">
		An error occurred while exporting the translations. <br/>
		Contact the administrator if the error persists. <br/>
	</td></tr>
	<tr><td style="padding-top:10px; padding-left:10px;"><%= 
		makeButton("Close", "closeMe();") %>
	</td></tr>
	</table>
<% } // if settingName, success %>
</body>
</html>
