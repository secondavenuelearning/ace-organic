<%@ page language="java" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.translations.PhraseTransln,
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

	final String ENGLISH = PhraseTransln.ENGLISH;
	PhraseTransln translator;
	String language;
	synchronized (session) {
		translator = 
				(PhraseTransln) session.getAttribute("phraseTranslator");
		language = (String) session.getAttribute("translationLanguage");
	}

	final String[] phrases = translator.allPhrases;
	final String[] translations = translator.translations;

	String propfilename = request.getParameter("propfilename");
	final String paramLanguage = request.getParameter("language");
	final boolean english = ENGLISH.equals(paramLanguage);
	final boolean untranslatedOnly = 
			"true".equals(request.getParameter("untranslatedOnly"));
	boolean settingName = true;
	boolean success = false;
	if (propfilename == null) {
		propfilename = Utils.toValidFileName("phrasesIn" 
				+ (english ? ENGLISH : language) + ".txt");
	} else {
		settingName = false;
		try {
			final String fullPath = Utils.toString(AppConfig.appRoot,
					AppConfig.appRoot.endsWith("/") ? "" : '/',
					AppConfig.relTempDir,
					AppConfig.relTempDir.endsWith("/") ? "" : '/',
					propfilename);
			int numPhrasesWritten = 0;
			Utils.alwaysPrint("exportPhrases.jsp: writing ", phrases.length,
					" phrases in ", english ? ENGLISH : language,
					" to ", fullPath);
			final File outFile = new File(fullPath);
			final DataOutputStream dos = new DataOutputStream(
					new FileOutputStream(outFile));
			for (int phrNum = 0; phrNum < translations.length; phrNum++) {
				final String transln = translations[phrNum];
				final boolean untranslated = Utils.isEmpty(transln);
				if (untranslated || (english && !untranslatedOnly)) {
					final String phrase = phrases[phrNum];
					final int phraseId = PhraseTransln.getPhraseId(phrase);
					dos.writeBytes(Utils.toString(phraseId,
							"\t", english ? phrase : transln, "\n"));
					Utils.alwaysPrint("exportPhrases.jsp: writing phrase \"",
							phrase, "\"");
					numPhrasesWritten++;
				} // if there's a translation of the phrase
			} // for each phrase
			Utils.alwaysPrint("exportPhrases.jsp: wrote ", numPhrasesWritten,
					" phrases.");
			dos.close();
			success = outFile.exists();
			propfilename = Utils.toString(pathToRoot, AppConfig.relTempDir, 
					AppConfig.relTempDir.endsWith("/") ? "" : '/',
					Utils.toValidHref(propfilename)).replaceAll(";", "");
			Utils.alwaysPrint("exportPhrases.jsp: href is ", propfilename);
		} catch (IOException e) {
			Utils.alwaysPrint("exportPhrases.jsp: IOException caught "
					+ "during export:\n", e.getMessage());
			e.printStackTrace();
		} // try
	} // if there's a proposed file name
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Translated Phrases Exporter</title>
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
	<form name="filenameform" action="exportPhrases.jsp" method="post">
		<input type="hidden" name="language" value="<%= paramLanguage %>" />
		<input type="hidden" name="untranslatedOnly" value="<%= untranslatedOnly %>" />
	<p>
	<table class="regtext" style="width:95%; margin-left:auto; margin-top:10px;
			margin-right:auto; border-style:none; border-collapse:collapse; 
			padding-top:10px; padding-left:10px;">
	<tr><td class="boldtext big" style="padding-left:10px;">
		Export translated phrases
	</td></tr>
	<tr><td style="text-align:left; vertical-align:middle; padding-left:20px;">
		<br/>Please enter a name for the export file, or use the suggested one.
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
		ACE has successfully exported the phrases into a text file.
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
			href="<%= propfilename %>">Exported 
			<%= !english ? "translated" : untranslatedOnly ? "untranslated" : "" %> 
			phrases (.txt)</a>]
	</td></tr>
	<tr><td style="padding-top:10px; padding-left:10px;">
		<%= makeButton("Close", "closeMe();") %>
	</td></tr>
	</table>
<% } else {  %>
	<table class="regtext" style="width:95%; margin-left:auto; margin-top:10px;
			margin-right:auto; border-style:none; border-collapse:collapse;">
	<tr><td class="boldtext big" >
		Export translated phrases
		</td></tr>
	<tr><td style="padding-left:10px;">
		An error occurred while exporting the translated phrases. <br/>
		Contact the administrator if the error persists. <br/>
	</td></tr>
	<tr><td style="padding-top:10px; padding-left:10px;"><%= 
		makeButton("Close", "closeMe();") %>
	</td></tr>
	</table>
<% } // if settingName, success %>
</body>
</html>
