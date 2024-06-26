<%@ page language="java" %>
<%@ page import="
	com.epoch.db.DataConversion,
	com.epoch.utils.Utils,
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
	final Map<Integer, String> allTranslations = 
			DataConversion.getAllTranslations(language);
	Utils.alwaysPrint("translationConvert.jsp: got from DB ",
			allTranslations.size(), " translations.");
	int numConverted = 0;
	// for (final Integer phraseId : allTranslations.keySet()) {
	for (final Map.Entry<Integer,String> theEntry: allTranslations.entrySet()) {
		// final String origXlatn = allTranslations.get(phraseId);
		final String origXlatn = theEntry.getValue();
		final String toCERXlatn = Utils.inputToCERs(origXlatn);
		if (toCERXlatn != null && !toCERXlatn.equals(origXlatn)) {
			Utils.alwaysPrint("translationConvert.jsp: storing phrase ",
				theEntry.getKey(), ":\noriginal = ", origXlatn, "\nnew = ",
					toCERXlatn
			);
			DataConversion.putOne(theEntry.getKey().intValue(),
				toCERXlatn, language);
			numConverted++;
		} // if the phrase changed
	} // for each phrase and translation

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
	// -->
	</script>
</head>

<body style="overflow:auto;">

<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

<div id="contentsWithoutTabs">
<p class="boldtext big" align="center">
ACE translation converter
</P>
<p>
<table style="width:95%; margin-left:auto; margin-right:auto;">
<tr>
<td class="regtext" style="text-align-left;">
From <%= allTranslations.size() %> translations in <%= language %>,
<%= numConverted %> translation<%= numConverted == 1 ? " was" : "s were" %>
converted.

</td>
</tr>

<tr><td class="regtext" colspan="2">
<td align="left" class="regtext" >
<br/><br/><a href="chooseLanguage.jsp">Choose another language to convert</a>.
</td>
</tr>

<tr>
<td style="text-align:left; padding-top:25px;">
	<%= makeButton("Back", "self.location.href='updateDB.jsp';") %>
</td>
</tr>
</table>
</p>

</div>
</body>
</html>
