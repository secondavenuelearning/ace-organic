<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.evals.Evaluator,
	com.epoch.qBank.QDatum,
	com.epoch.qBank.QSetDescr,
	com.epoch.qBank.Question,
	com.epoch.session.QSet,
	com.epoch.translations.QSetTransln,
	com.epoch.utils.Utils,
	java.io.DataOutputStream,
	java.io.File,
	java.io.FileOutputStream,
	java.io.IOException"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<% 
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	final String ENGLISH = QSetTransln.ENGLISH;
	final String qSetId = request.getParameter("qSetId");
	String language;
	QSetTransln translator;
	QSet qSet;
	synchronized (session) {
		language = (String) session.getAttribute("translationLanguage");
		translator = (QSetTransln) session.getAttribute("translationObj");
		qSet = (QSet) session.getAttribute("qSet");
	}
	final String paramLanguage = request.getParameter("language");
	final boolean english = ENGLISH.equals(paramLanguage);

	final QSetDescr qSetDescr = qSet.getQSetDescr();
	final String header = (english ? qSetDescr.header : translator.header);
	String[] qStmts;
	String[][] evalFeedbacks;
	String[][] qdTexts;
	if (!english) {
		qStmts = translator.qStmts;
		evalFeedbacks = translator.evalFeedbacks;
		qdTexts = translator.qdTexts;
	} else {
		final int numQs = qSet.getCount();
		final Question[] setQs = qSet.getQuestions();
		qStmts = new String[numQs];
		evalFeedbacks = new String[numQs][0];
		qdTexts = new String[numQs][0];
		for (int qNum = 0; qNum < numQs; qNum++) {
			final Question setQ = setQs[qNum]; 
			qStmts[qNum] = setQ.getStatement(); 
			final Evaluator[] evals = setQ.getAllEvaluators(); 
			evalFeedbacks[qNum] = new String[evals.length];
			int evalNum = 0;
			for (final Evaluator eval : evals) {
				evalFeedbacks[qNum][evalNum++] = eval.feedback;
			} // for each evaluator
			final QDatum[] qData = setQ.getQData(Question.GENERAL); 
			qdTexts[qNum] = new String[qData.length];
			int qdNum = 0;
			for (final QDatum qDatum : qData) {
				qdTexts[qNum][qdNum++] = 
						(qDatum.isMarvin() ? qDatum.name : qDatum.data);
			} // for each qDatum
		} // for each Q
	} // if English

	String propfilename = request.getParameter("propfilename");
	boolean settingName = true;
	boolean success = false;
	if (propfilename == null) {
		propfilename = Utils.toValidFileName(Utils.toString("qSet", qSetId, "In", 
				english ? ENGLISH : language, ".txt"));
	} else {
		settingName = false;
		try {
			final String fullPath = Utils.toString(AppConfig.appRoot, 
					AppConfig.appRoot.endsWith("/") ? "" : "/", AppConfig.relTempDir, 
					AppConfig.relTempDir.endsWith("/") ? "" : "/", propfilename);
			Utils.alwaysPrint("exportTranslations.jsp: writing "
					+ "translations from qSet ", qSetId, " in ",
					english ? ENGLISH : language, " to ", fullPath);
			final File outFile = new File(fullPath);
			final DataOutputStream dos = new DataOutputStream(
					new FileOutputStream(outFile));
			// format: type [tab] Q number [tab] item number [tab] data
			// export header
			String bld;
			if (!Utils.isEmpty(header)) {
				bld = Utils.toString(QSetTransln.HEADER_TAG, "\t\t\t", header, '\n');
				dos.writeBytes(bld);
			} // if there is a translated header
			// export Q statements
			for (int qNum = 0; qNum < qStmts.length; qNum++) {
				if (!Utils.isEmpty(qStmts[qNum])) {
					bld = Utils.toString(QSetTransln.QSTMT_TAG, '\t', qNum + 1, 
							"\t\t", qStmts[qNum], '\n');
					dos.writeBytes(bld);
				} // if there is a translated Q statement
				// export qData 
				final String[] qData = qdTexts[qNum];
				final int numQData = qData.length;
				for (int qdNum = 0; qdNum < numQData; qdNum++) {
					if (!Utils.isEmpty(qData[qdNum])) {
						bld = Utils.toString(QSetTransln.QDTEXT_TAG, 
								'\t', qNum + 1, '\t', qdNum + 1, '\t', 
								qData[qdNum], '\n');
						dos.writeBytes(bld);
					} // if there is a translated Q datum
				} // for each question datum
				// export evaluators
				final String[] evals = evalFeedbacks[qNum];
				final int numEvals = evals.length;
				for (int evalNum = 0; evalNum < numEvals; evalNum++) {
					if (!Utils.isEmpty(evals[evalNum])) {
						bld = Utils.toString(QSetTransln.FEEDBACK_TAG, 
								'\t', qNum + 1, '\t', evalNum + 1, '\t', 
								evals[evalNum], '\n');
						dos.writeBytes(bld);
					} // if there is a translated evaluator
				} // for each eval
			} // for each Q
			dos.close();
			success = outFile.exists();
			propfilename = Utils.toString(pathToRoot, AppConfig.relTempDir,
					AppConfig.relTempDir.endsWith("/") ? "" : '/',
					Utils.toValidHref(propfilename)).replaceAll(";", "");
			Utils.alwaysPrint("exportTranslations.jsp: href is ", propfilename);
		} catch (IOException e) {
			Utils.alwaysPrint("exportTranslations.jsp: IOException caught "
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
	<title>ACE Translated Question Set Exporter</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
	<script type="text/javascript">
		// <!-- >
		<% if (settingName) { %>
			function reload() {
				var filename = 
						trimWhiteSpaces(document.filenameform.propfilename.value);
				if (isWhiteSpace(filename)) {
					alert('Please enter a filename.');
					return;
				}
				document.filenameform.propfilename.value = filename;
				document.filenameform.submit();
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
	<form name="filenameform" action="exportTranslations.jsp" method="post">
		<input type="hidden" name="qSetId" value="<%= qSetId %>" />
		<input type="hidden" name="language" value="<%= paramLanguage %>" />
	<table class="regtext" style="width:95%; margin-left:auto; margin-top:10px;
			margin-right:auto; border-style:none; border-collapse:collapse; 
			padding-top:10px; padding-left:10px;">
	<tr><td class="boldtext big" style="padding-left:10px;">
		Export translations of question set
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
		<td style="padding-top:10px; padding-left:10px;">
			<%= makeButton("Submit", "reload();") %>
		</td>
		<td style="padding-top:10px; padding-left:10px;">
			<%= makeButton("Close", "closeMe();") %>
		</td></tr>
		</table>
	</td></tr>
	</table>
	</form>
<% } else if (success) { %>
	<table class="regtext" style="width:95%; margin-top:10px; margin-left:auto;
			margin-right:auto; border-style:none; border-collapse:collapse;">
	<tr><td class="boldtext" style="padding-left:10px; padding-bottom:10px;">
		ACE has successfully exported the translated parts of the question set 
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
			href="<%= propfilename %>">Exported translated set (.txt)</a>]
	</td></tr>
	<tr><td style="padding-top:10px; padding-left:10px;">
		<%= makeButton("Close", "closeMe();") %>
	</td></tr>
	</table>
<% } else {  %>
	<table class="regtext" style="width:95%; margin-left:auto; margin-top:10px;
			margin-right:auto; border-style:none; border-collapse:collapse;">
	<tr><td class="boldtext big" >
		Export translations of question set
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
