<%@ page language="java" %>
<%@ page import="
	com.epoch.translations.QSetTransln,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	String language;
	QSetTransln translator;
	synchronized (session) {
		language = (String) session.getAttribute("translationLanguage");
		translator = (QSetTransln) session.getAttribute("translationObj");
	}
	final String qSetId = request.getParameter("qSetId");
	final String openQs = request.getParameter("openQs");

	if (translator != null) {
		final String ERASE_TRANSLN = QSetTransln.ERASE_TRANSLN;
		final int numQs = translator.qStmts.length;
		boolean send = false;
		// get common question statement
		String newHeader = null;
		String element = "xlatnHeader";
		String origXlatn = translator.header;
		if (origXlatn == null) origXlatn = "";
		String newXlatn = Utils.inputToCERs(
				Utils.trim(request.getParameter(element)));
		if (ERASE_TRANSLN.equals(newXlatn)) {
			translator.header = null;
			newHeader = "";
			/* Utils.alwaysPrint("saveTranslations.jsp: "
					+ "erasing translator.header"); /**/
			send = true;
		} else if (!Utils.among(newXlatn, "", origXlatn)) {
			translator.header = newXlatn;
			newHeader = newXlatn;
			/* Utils.alwaysPrint("saveTranslations.jsp: new header = ", 
					translator.header); /**/
			send = true;
		} // if the translation is new
		// get qStmts, evals, qData
		final String[] newQStmts = new String[numQs];
		final String[][] newEvalFeedbacks = new String[numQs][0];
		final String[][] newQDTexts = new String[numQs][0];
		for (int qNum = 0; qNum < numQs; qNum++) {
			// get Q statement
			element = "xlatnQStmt" + (qNum + 1);
			origXlatn = translator.qStmts[qNum];
			if (origXlatn == null) origXlatn = "";
			newXlatn = Utils.inputToCERs(
					Utils.trim(request.getParameter(element)));
			if (ERASE_TRANSLN.equals(newXlatn)) {
				translator.qStmts[qNum] = null;
				newQStmts[qNum] = "";
				/* Utils.alwaysPrint("saveTranslations.jsp: "
						+ "erasing translator.qStmts[", qNum, "]"); /**/
				send = true;
			} else if (!Utils.among(newXlatn, "", origXlatn)) {
				translator.qStmts[qNum] = newXlatn;
				newQStmts[qNum] = newXlatn;
				/* Utils.alwaysPrint("saveTranslations.jsp: "
						+ "new translator.Qstmts[", qNum, "] = ", 
						newXlatn); /**/
				send = true;
			} // if the translation is new
			// get evaluators
			final int numEvals = translator.evalFeedbacks[qNum].length;
			newEvalFeedbacks[qNum] = new String[numEvals];
			for (int evalNum = 0; evalNum < numEvals; evalNum++) {
				element = "xlatnEval" + (qNum + 1) + "_" + (evalNum + 1);
				origXlatn = translator.evalFeedbacks[qNum][evalNum];
				if (origXlatn == null) origXlatn = "";
				newXlatn = Utils.inputToCERs(
						Utils.trim(request.getParameter(element)));
				if (ERASE_TRANSLN.equals(newXlatn)) {
					translator.evalFeedbacks[qNum][evalNum] = null;
					newEvalFeedbacks[qNum][evalNum] = "";
					/* Utils.alwaysPrint("saveTranslations.jsp: "
							+ "erasing translator.evalFeedbacks[", 
							qNum, "][", evalNum, "]"); /**/
					send = true;
				} else if (!Utils.among(newXlatn, "", origXlatn)) {
					translator.evalFeedbacks[qNum][evalNum] = newXlatn;
					newEvalFeedbacks[qNum][evalNum] = newXlatn;
					/* Utils.alwaysPrint("saveTranslations.jsp: "
							+ "new translator.evalFeedbacks[", qNum, 
							"][", evalNum, "] = ", newXlatn); /**/
					send = true;
				} // if the translation is new
			} // for each evaluator
			// get Q data
			final int numQData = translator.qdTexts[qNum].length;
			newQDTexts[qNum] = new String[numQData];
			for (int qdNum = 0; qdNum < numQData; qdNum++) {
				element = "xlatnQD" + (qNum + 1) + "_" + (qdNum + 1);
				origXlatn = translator.qdTexts[qNum][qdNum];
				if (origXlatn == null) origXlatn = "";
				newXlatn = Utils.inputToCERs(
						Utils.trim(request.getParameter(element)));
				if (ERASE_TRANSLN.equals(newXlatn)) {
					translator.qdTexts[qNum][qdNum] = null;
					newQDTexts[qNum][qdNum] = "";
					/* Utils.alwaysPrint("saveTranslations.jsp: "
							+ "erasing translator.qdTexts[[", qNum, 
							"][", qdNum, "]"); /**/
					send = true;
				} else if (!Utils.among(newXlatn, "", origXlatn)) {
					translator.qdTexts[qNum][qdNum] = newXlatn;
					newQDTexts[qNum][qdNum] = newXlatn;
					/* Utils.alwaysPrint("saveTranslations.jsp: "
							+ "new translator.qdTexts[[", qNum, 
							"][", qdNum, "] = ", newXlatn); /**/
					send = true;
				} // if the translation is new
			} // for each qDatum
		} // for each Q
		if (send) {
			translator.setTranslations(newHeader, newQStmts, 
					newEvalFeedbacks, newQDTexts, language);
		}
	} else Utils.alwaysPrint("saveTranslations.jsp: translator is null.");
	synchronized (session) {
		session.removeAttribute("translationObj");
	}

%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Translation</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css">
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon">
	<script type="text/javascript">
		function finish() {
			<% if (translator == null) { %>
				alert('Unable to save because translator is null.');
			<% } %>
			self.location.href = 'translate.jsp?saved=true&qSetId=<%= 
					qSetId %>&openQs=<%= openQs %>';
		}
	</script>
</head>
<body style="boldtext" onload="finish();">
</body>
</html>
