<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.db.TranslnRead,
	com.epoch.translations.PhraseTransln,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.HashMap,
	java.util.List,
	java.util.Map"
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

	final boolean mayDelete = "bob".equals(user.getUserId())
			|| "raphael".equals(user.getUserId())
			|| realRole == User.ADMINISTRATOR;
	final String saved = request.getParameter("saved");
	PhraseTransln translator;
	String language;
	synchronized (session) {
		translator = 
				(PhraseTransln) session.getAttribute("phraseTranslator");
		language = (String) session.getAttribute("translationLanguage");
	}
	if (translator == null) {
		language = request.getParameter("language");
		/* Utils.alwaysPrint("phraseTranslate.jsp: creating new translator for ",
				language, "."); /**/
		translator = new PhraseTransln(language, user.getUserId());
		synchronized (session) {
			session.setAttribute("translationLanguage", language);
			session.setAttribute("phraseTranslator", translator);
		}
	} // if we are starting anew
	/* else Utils.alwaysPrint("phraseTranslate.jsp: using existing translator for ",
			language, "."); /**/
	final boolean untranslatedOnly = 
			"true".equals(request.getParameter("untranslatedOnly"));
	final String[] phrases = translator.allPhrases;
	final String[] translations = translator.translations;

	final String VAR = PhraseTransln.STARS_SIMPLE;
	// NOTE: the following array clarifies for translators the meanings of 
	// some of the terms that are hardwired into ACE.
	final String[][] phraseComments = new String[][] {
			{"Back", "as in \"Go back to where you were previously\""},
			{"correct", "describing a response"},
			{"disabled", "describing a course that a student may not enter"},
			{"Unlimited", "referring to number of allowed attempts"},
			{"Item", "referring to rank or number"},
			{"Option", "from which to choose"},
			{"structure", "indicating a drawn compound"},
			{"Excellent.", "for a correct response without feedback"},
			{"Addition", "as to a &pi; bond"},
			{"Basic", "describing a pH"},
			{"Reduction", "as in adding H<sub>2</sub> or electrons"},
			{"Tut", "as in a tutorial"},
			{"Set", "as in an assignment"},
			{"pts.", "abbreviation for \"points\""},
			{"Q", "abbreviation for \"Question\""},
			{"No.", "abbreviation for \"number\""},
			{"M.I.", "abbreviation for \"middle initial\""},
			{"TA", "teaching assistant"},
			{"not TA", "not a teaching assistant"},
			{"PC", "personal computer"},
			{"Mac", "Macintosh computer"},
			{"mm-dd-yyyy", "a date in month-day-year format"},
			{"only set", "the only question set in a topic"},
			{"indefinite", "an indeterminate length of time"},
			{"by", "written by"},
			{"chemical structure", "a drawing of a chemical structure"},
			{"Title", "of an assignment"},
			{"Source code", "the string of characters encoding a response"},
			{"entry", "referring to the act of entering a course"},
			{"Safari", "the Web browser"},
			{"What are Tut 1&ndash;" + VAR + '5' + VAR + '?', 
				"referring to the tutorials"},
			{"Comment", "a written note"},
			{"Get questions", "as in a search"},
			{"Close", "as in a pop-up window"},
			{"Text", "written words"},
			{VAR + '1' + VAR + " remaining", "referring to the number of attempts"},
			{VAR + '2' + VAR + " remaining", "referring to the number of attempts"},
			{"last", "the final item in a series"}
			};
	final Map<String, String> needComments = new HashMap<String, String>();
	for (final String[] phraseComment : phraseComments) {
		needComments.put(phraseComment[0], phraseComment[1]);
	} // for each phrase and comment
	final StringBuilder alphabetBld = new StringBuilder()
			.append("[<a href=\"#top\">&uarr;</a>]&nbsp;&nbsp; ");
	char prev1stChar = '\t';
	char very1stChar = '\t';
	final List<Integer> untranslatedPhrs = new ArrayList<Integer>();
	int translnCt = 0;
	int numPhrases = 0;
	for (int phraseNum = 0; phraseNum < phrases.length; phraseNum++) { 
		final boolean untranslated = Utils.isEmpty(translations[phraseNum]);
		if (!untranslated && untranslatedOnly) continue;
		numPhrases++;
		final String phrase = phrases[phraseNum]
				.replaceFirst("<[A-Za-z][A-Za-z0-9]*>", "");
		int charPosn = 0;
		while (charPosn < phrase.length() - 1 
				&& !Utils.isAlphanumeric(phrase.charAt(charPosn))) {
			charPosn++;
		}
		final char firstChar = Character.toUpperCase(phrase.charAt(charPosn));
		if (very1stChar == '\t') very1stChar = firstChar;
		if (firstChar != prev1stChar) {
			Utils.appendTo(alphabetBld, "[<a href=\"#letter", 
					(int) firstChar, "\">", firstChar, "</a>]&nbsp;&nbsp; ");
		}
		prev1stChar = firstChar;	
		if (untranslated) 
			untranslatedPhrs.add(Integer.valueOf(phraseNum));
		else translnCt++;
	} // for each phrase 
	final String alphabet = alphabetBld.toString();
	final double proportionUntranslated = 
			((double) (phrases.length - translnCt)) / phrases.length;
	final boolean linkUntranslated = 
			(phrases.length > 20 && proportionUntranslated < 0.25);
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >

<head> 
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Translation</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
		#footer {
			position:absolute; 
			bottom:0; 
			left:0;
			width:100%; 
			height:50px; 
			overflow:auto; 
		}

		#translationContents {
			position:fixed; 
			top:55px;
			left:0;
			bottom:50px; 
			right:0; 
			overflow:auto; 
		}

		* html body {
			padding:55px 0 50px 0; 
		}

		* html #translationContents {
			height:100%; 
		}
	</style>
	<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

	function exitTranslator() {
        self.location.href = '<%= pathToRoot %>authortool/'
				+ 'questionsList.jsp?qSetId=0'; 
	}

	function toggleUntranslatedOnly() {
        self.location.href = 'phraseTranslate.jsp?untranslatedOnly=<%= 
				!untranslatedOnly %>';
	}

	function exportMe(english) {
		var url = 'exportPhrases.jsp?untranslatedOnly=<%= 
				untranslatedOnly %>';
		if (english) url += '&language=English';
        openExportWindow(url);
	}

	function importPhrases() {
        openExportWindow('importTranslations.jsp?which=phrases&language='
				+ encodeURIComponent('<%= Utils.toValidJS(language) %>')); 
	}

	var backNoSaveButton = '<%= Utils.toValidJS(makeButton("Back w/o Saving", 
			"exitTranslator();")) %>';
			
	var mostRecentChanged = 0;

	function setNotSavedFlag(phraseNum) {
		mostRecentChanged = phraseNum;
		setInnerHTML('savedFlag', '<span class="boldtext">Not saved.<\/span>');
		setInnerHTML('backButton', backNoSaveButton);
	}

	function saveTranslations() {
		document.translatorForm.mostRecent.value = mostRecentChanged;
		document.translatorForm.submit();
	} // saveTranslations()

	<% if (mayDelete) { %>

		function deletePhrase(phraseId) {
			var toSend = new String.builder();
			toSend.append('phraseId=').append(phraseId);
			callAJAX('deletePhrase.jsp', toSend.toString());
		} // deletePhrase()

		function updatePage() { 
			if (xmlHttp.readyState === 4) { // ready to continue
				var response = xmlHttp.responseText;
				// the response is the entire web page
				var phraseIdValue = extractField(response, 'phraseIdValue');
				setInnerHTML('td1' + phraseIdValue, '');
				setInnerHTML('td2' + phraseIdValue, '');
				setInnerHTML('td3' + phraseIdValue, '');
			} // if ready
		} // updatePage()

	<% } // mayDelete %>
	// -->
	</script>

</head>
<body class="light" style="background-color:white; text-align:center; 
		overflow:auto;">

	<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

	<form name="translatorForm" method="post" action="savePhraseTranslations.jsp"
			accept-charset="UTF-8">
		<input type="hidden" name="language" 
				value="<%= Utils.toValidHTMLAttributeValue(language) %>" /> 
		<input type="hidden" name="untranslatedOnly" value="<%= untranslatedOnly %>" /> 
		<input type="hidden" name="mostRecent" value="0"/> 
	<div id="translationContents">
	<p>&nbsp;</p><table class="regtext" style="width:95%; margin-left:auto; margin-right:auto;
			border-style:none; border-collapse:collapse;">
		<tr>
			<td class="boldtext big" style="padding-bottom:10px;" colspan="4">
				<a name="top"></a>
				Translatable phrases used in ACE 
			</td>
		</tr>
		<tr>
			<td class="regtext" style="padding-left:5px; padding-bottom:10px;">
				<p>If you leave a textbox blank, ACE will leave unchanged any
				existing translation for that phrase.  If you wish to erase a
				translation, type <%= PhraseTransln.ERASE_TRANSLN %>.  
				</p><p>Be careful to preserve the presence or absence of
				periods (full stops) when translating phrases.
				</p><p>Character entity references of the form &amp;#<i>n</i>;
				will appear as the characters they represent; those of the form
				&amp;<i>foo</i>; will appear as such.
				</p><p>When you encounter a phrase containing <%= VAR %>, 
				please read the instructions for that phrase carefully. 
				</p>
			</td>
			<td colspan="2" class="regtext" style="padding-left:20px;">
				<%= alphabet %>
			</td>
		</tr>
		<tr style="background-color:#ffffff;">
			<td class="boldtext enlarged"
			style="padding-left:5px; padding-top:15px; width:40%;">
				<a name="letter<%= 
						very1stChar == '(' ? "OpenParen" : very1stChar %>"></a>
				<%= untranslatedOnly ? "Untranslated phrases" : "Phrases" %> 
				in English (<%= numPhrases %>) and <span style="color:green">comments</span>
			</td>
			<td colspan="2" style="width:40%; text-align:left; 
					padding-left:15px; padding-top:15px;">
				<table style="width:100%;">
				<tr><td class="boldtext enlarged">
					Translations <%= untranslatedOnly ? "" : "(" + translnCt + ")" %> 
					into <%= language %>
				</td>
				<% if (linkUntranslated) { %>
					<td class="regtext" style="text-align:right;">
						[<a href="#unXlated1">first untranslated phrase</a>]
					</td>
				<% } %>
				</tr></table>
			</td>
		</tr>
		<% boolean color = false;
		prev1stChar = '\t';
		int untranslatedNum = 1;
		for (int phraseNum = 0; phraseNum < phrases.length; phraseNum++) { 
			final String phrase = phrases[phraseNum];
			final boolean untranslated = Utils.isEmpty(translations[phraseNum]);
			if (!untranslated && untranslatedOnly) continue;
			final StringBuilder phraseBld = new StringBuilder().append(phrase);
			final String phraseSubstd = phrase
					.replaceFirst("<[A-Za-z][A-Za-z0-9]*>", "");
			int charPosn = 0;
			while (charPosn < phraseSubstd.length() - 1 
					&& !Utils.isAlphanumeric(phraseSubstd.charAt(charPosn))) {
				charPosn++;
			}
			final char firstChar = Character.toUpperCase(
					phraseSubstd.charAt(charPosn));
			final String comment = needComments.get(phrase);
			if (comment != null) {
				Utils.appendTo(phraseBld, "<p><span style=\""
						+ "color:green;\">(", comment, ")</span></p>");
			} // if this phrase needs a comment
			if (phrase.indexOf(VAR + "See the products" + VAR) >= 0
					|| phrase.indexOf(VAR + "webmaster" + VAR) >= 0) {
				Utils.appendTo(phraseBld, 
						"<p><span style=\"color:green;\">"
						+ "(translate the phrase inside the ", VAR, 
						" demarcations, but leave the demarcations "
						+ "in your translation; the translated phrase inside "
						+ "the demarcations will be hyperlinked)</span></p>");
				charPosn = 1;
			} else if (phrase.indexOf(VAR) >= 0) {
				Utils.appendTo(phraseBld, "<p><span style=\""
						+ "color:green;\">(please retain the ", VAR, 
						" demarcations and their contents <i>unchanged</i> "
						+ "in your translation)</span></p>");
				charPosn = 1;
			}
			final int phraseId = TranslnRead.getPhraseId(phrase);
			final int len = phraseBld.length();
			int numRows = (len / 50) + 2;
			if (charPosn >= 0) numRows++;
			if (firstChar != prev1stChar) {
			%>
				<tr style="background-color:#FFFCCC;">
					<td class="regtext" colspan="3" style="padding-left:10px; 
							padding-top:10px; padding-bottom:10px;">
						<a name="letter<%= (int) firstChar %>"></a>
						<%= prev1stChar == '\t' ? "" : alphabet %>
					</td>
				</tr>
			<% } // if at new character
			else color = !color;
			final String bgColor = (color ? "#E6E6FA" : "#FFFFFF");
			%>
			<tr style="background-color:<%= bgColor %>;">
				<td id="td1<%= phraseId %>" class="regtext" style="padding-top:10px; 
						padding-left:10px; width:50%;">
					<a name="phrase<%= phraseNum %>"></a>
					<!-- <%= phraseId %> -->
					<%= phraseBld.toString() %>
				</td>
				<td id="td2<%= phraseId %>" class="regtext" 
						style="padding-top:10px; width:40%; padding-left:15px;">
					<% if (linkUntranslated 
							&& !untranslatedPhrs.isEmpty() 
							&& phraseNum == untranslatedPhrs.get(0).intValue() - 1) { 
						untranslatedPhrs.remove(0); 
					%>
						<a name="unXlated<%= untranslatedNum %>"></a>
					<% } // if next phrase is untranslated %>
					<textarea name="xlatn<%= phraseNum %>"
							id="xlatn<%= phraseNum %>"
							cols="65" rows="<%= numRows %>"
							onchange="setNotSavedFlag(<%= phraseNum %>);"><%= 
						untranslated ? "" : Utils.toValidTextbox(
								translations[phraseNum]) %></textarea>
				</td>
				<td id="td3<%= phraseId %>" class="regtext" style="padding-top:10px;
						padding-left:15px;">
				<%= mayDelete ? makeButtonIcon("delete", pathToRoot,
						"deletePhrase(", phraseId, ");") : "" %>
				</td>
			</tr> 
			<tr style="background-color:<%= bgColor %>;">
				<td colspan="3" class="regtext" style="text-align:right; 
						color:green; padding-bottom:10px;">
					<% if (untranslated && linkUntranslated) { 
						if (!untranslatedPhrs.isEmpty()) { 
							untranslatedNum++; 
					%>
							[<a href="#unXlated<%= untranslatedNum
									%>">next untranslated phrase</a>]
					<% 	} else if (linkUntranslated) { %>
							[<a href="#unXlated1">first untranslated phrase</a>]
					<% 	} // if last untranslated phrase 
					} // if untranslated %>
				</td>
			</tr>
			<% prev1stChar = firstChar;
		} // for each phrase %>
		<tr style="background-color:#FFFCCC;">
			<td class="regtext" colspan="3" style="padding-left:10px; 
					padding-top:10px; padding-bottom:10px;">
				<%= alphabet %>
			</td>
		</tr>
	</table>
	</div>
	<div id="footer">
	<table class="regtext" style="width:90%; margin-left:auto; margin-right:auto;
			margin-top:5px; border-style:none; border-collapse:collapse;">
		<tr><td>
			<table style="margin:0px; margin-left:auto; margin-right:auto;"><tr>
				<td style="margin-right:0px;">
					 <%= makeButton(untranslatedOnly ? "All phrases" : "Untranslated only", 
					 		"toggleUntranslatedOnly();") %>
				</td>
				<td style="margin-right:0px;">
					 <%= makeButton("Import", "importPhrases();") %>
				</td>
				<td style="margin-right:0px;">
					 <%= makeButton("Save", "saveTranslations();") %>
				</td>
				<td style="margin-right:0px;">
					 <%= makeButton("Export Saved", "exportMe(false);") %>
				</td>
				<td style="margin-right:0px;">
					 <%= makeButton("Export English", "exportMe(true);") %>
				</td>
				<td id="backButton" style="margin-right:0px;">
					 <%= makeButton("Back", "exitTranslator();") %>
				</td>
				<td id="savedFlag" style="text-align:right; margin-right:0px; 
						padding-left:10px; width:80px;">
					<%= saved == null ? "" : "true".equals(saved) 
							? "Saved." : "<span class=\"boldtext\">Not saved</span>" %> 
				</td>
			</tr></table>
		</td></tr>
	</table>
	</div>
	</form>
	
</body>
</html>
