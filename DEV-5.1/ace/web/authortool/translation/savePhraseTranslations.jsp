<%@ page language="java" %>
<%@ page import="
	com.epoch.translations.PhraseTransln,
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
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	String language;
	PhraseTransln translator;
	synchronized (session) {
		language = (String) session.getAttribute("translationLanguage");
		translator = (PhraseTransln) 
				session.getAttribute("phraseTranslator");
	}
	final int mostRecent = MathUtils.parseInt(request.getParameter("mostRecent"));
	final String untranslatedOnly = request.getParameter("untranslatedOnly");

	if (translator != null) {
		final String[] translations = translator.translations;
		final int numPhrases = translations.length;
		final String[] newTranslations = new String[numPhrases];
		boolean send = false;
		for (int phraseNum = 0; phraseNum < numPhrases; phraseNum++) {
			final String element = "xlatn" + phraseNum;
			String newXlatn = request.getParameter(element);
			if (newXlatn == null) continue;
			newXlatn = Utils.inputToCERs(newXlatn.trim());
			String origXlatn = translations[phraseNum];
			if (origXlatn == null) origXlatn = "";
			if (newXlatn.equals(PhraseTransln.ERASE_TRANSLN)) {
				translations[phraseNum] = null;
				newTranslations[phraseNum] = "";
				/* Utils.alwaysPrint("savePhraseTranslations.jsp: "
						+ "erasing translation[", phraseNum, "]");
				*/
				send = true;
			} else if (!Utils.among(newXlatn, "", origXlatn)) {
				translations[phraseNum] = newXlatn;
				newTranslations[phraseNum] = newXlatn;
				/* Utils.alwaysPrint("savePhraseTranslations.jsp: "
						+ "new translations[", phraseNum, "] = ", newXlatn);
				*/
				send = true;
			} // if the translation is new
		} // for each phrase
		if (send) {
			translator.setTranslations(newTranslations, language);
		}
	} else Utils.alwaysPrint("savePhraseTranslations.jsp: translator is null.");

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
			self.location.href = 'phraseTranslate.jsp?saved=true'
					+ '&untranslatedOnly=<%= untranslatedOnly  
							+ (mostRecent > 4 ? "#phrase" + (mostRecent - 4) : "") %>';
		}
	</script>
</head>
<body style="boldtext" onload="finish();">
	Saved !!!!! 
</body>
</html>
