<%@ page language="java" %>
<%@ page import="
	com.epoch.db.DataConversion,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	com.epoch.xmlparser.XMLUtils,
	java.util.ArrayList,
	java.util.Map"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	final String language = request.getParameter("language");
	Utils.alwaysPrint("translationConvert.jsp: language = ", language);
	final boolean haveLists = "true".equals(request.getParameter("haveLists"));
	Map<Integer, String> allTranslations;
	ArrayList<Integer> allPhraseIds;
	synchronized (session) {
		allTranslations = (Map<Integer, String>) session.getAttribute("allXlatns");
		allPhraseIds = (ArrayList<Integer>) session.getAttribute("allPhraseIds");
	}
	if (allTranslations == null || !haveLists) {
		allTranslations = DataConversion.getAllTranslations(language);
		allPhraseIds = new ArrayList<Integer>(allTranslations.keySet());
		Utils.alwaysPrint("translationConvert.jsp: got from DB ",
				allTranslations.size(), " translations.");
		synchronized (session) {
			session.setAttribute("allPhraseIds", allPhraseIds);
			session.setAttribute("allXlatns", allTranslations);
		}
	} else
		Utils.alwaysPrint("translationConvert.jsp: got from session ",
				allTranslations.size(), " translations.");
	final int numTranslations = allTranslations.size();
	final String posnStr = request.getParameter("posn");
	int posn = MathUtils.parseInt(posnStr);
	Integer phraseId = allPhraseIds.get(posn);
	String origXlatn = allTranslations.get(phraseId);
	String toCERXlatn = Utils.inputToCERs(origXlatn);
	if (posnStr != null) {
		final boolean convert = "true".equals(request.getParameter("convert"));
		if (convert) {
			Utils.alwaysPrint("translationConvert.jsp: storing phrase ",
				phraseId, ", posn ", posn, ": original = ", origXlatn,
				"\nnew = ", toCERXlatn);
			DataConversion.putOne(phraseId.intValue(), toCERXlatn, language);
			origXlatn = toCERXlatn;
		}
	} else posn--;
	while (toCERXlatn.equals(origXlatn) && ++posn < numTranslations) {
		phraseId = allPhraseIds.get(posn);
		origXlatn = allTranslations.get(phraseId);
		toCERXlatn = Utils.inputToCERs(origXlatn);
	}
	if (posn < 0) posn++;

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE translation converter</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon">
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<style type="text/css">
		* html body {
			padding:55px 0 40px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

	function convertMe(doIt) {
		// alert('in convertMe, doIt = ' + doIt);
		document.converter.convert.value = doIt;
		document.converter.submit();
	}
	// -->

	</script>
</head>

<body style="overflow:auto;">

<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

<div id="contentsWithoutTabs">
<form name="converter" action="translationConvert.jsp?haveLists=true" method="post">
<input type="hidden" name="posn" value="<%= posn %>" />
<input type="hidden" name="convert" value="false" />
<input type="hidden" name="language" value="<%= Utils.toValidHTMLAttributeValue(language) %>" />

<p>
<table style="width:95%; margin-left:auto; margin-right:auto;">
<tr>
<td class="boldtext big" style="padding-bottom:10px;">
ACE translation converter
</td>
</tr>
<tr>
<td class="regtext" style="text-align-left;">

<% if (posn >= numTranslations) { %>

	There are no more translations.

<% } else { %>

	Current value of phrase <%= posn + 1 %> / <%= numTranslations %> with key <%= phraseId %>:

	<p>
	<span style="background-color:yellow;"><%= XMLUtils.toValidXML(origXlatn) %></span>
	</p>

	appearing as:

	<p>
	<span style="background-color:yellow;"><%= origXlatn %></span>
	</p>

	After conversion to CERs:

	<p>
	<span style="background-color:yellow;"><%= XMLUtils.toValidXML(toCERXlatn) %></span>
	</p>

	Will appear as:

	<p>
	<span style="background-color:yellow;"><%= toCERXlatn %></span>
	</p>

	<p>
	Convert? 
	<input type="button" onclick="convertMe('true');" value="Yes" />
	<input type="button" onclick="convertMe('false');" value="No" />
	</p>

	<% } %>
</td>
</tr>

<tr>
<td style="text-align:left; padding-top:25px;">
	<%= makeButton("Back", "self.location.href='updateDB.jsp';") %>
</td>
</tr>
</table>
</P>
</form>

</div>
</body>
</html>
