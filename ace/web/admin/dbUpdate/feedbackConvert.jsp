<%@ page language="java" %>
<%@ page import="
	com.epoch.db.QuestionRW,
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

	final boolean haveLists = "true".equals(request.getParameter("haveLists"));
	final boolean local = "true".equals(request.getParameter("local"));
	Map<String, String> allFeedback;
	ArrayList<String> allKeys;
	synchronized (session) {
		allFeedback = (Map<String, String>) session.getAttribute("allFeedback");
		allKeys = (ArrayList<String>) session.getAttribute("allFeedbackKeys");
	}
	if (allFeedback == null || !haveLists) {
		allFeedback = QuestionRW.getConvertibleFeedback(local);
		Utils.alwaysPrint("feedbackConvert.jsp: got from DB ",
				allFeedback.size(), " feedback.");
		allKeys = new ArrayList<String>(allFeedback.keySet());
		synchronized (session) {
			session.setAttribute("allFeedback", allFeedback);
			session.setAttribute("allFeedbackKeys", allKeys);
		}
	} else
		Utils.alwaysPrint("feedbackConvert.jsp: got from session ",
				allFeedback.size(), " feedback.");
	final int numFeedbacks = allFeedback.size();
	final String posnStr = request.getParameter("posn");
	int posn = MathUtils.parseInt(posnStr);
	String key = (numFeedbacks > 0 ? allKeys.get(posn) : "");
	String origFeedback = (numFeedbacks > 0 ? allFeedback.get(key) : "");
	String toCERFeedback = Utils.inputToCERs(origFeedback);
	if (posnStr != null) {
		final boolean convert = "true".equals(request.getParameter("convert"));
		if (convert) {
			final boolean useText = "true".equals(request.getParameter("useText"));
			if (useText) {
				toCERFeedback = Utils.inputToCERs(request.getParameter("enteredTxt"));
			}
			Utils.alwaysPrint("feedbackConvert.jsp: storing feedback ", key,
					", posn ", posn, ": original = ", origFeedback, "\nnew = ",
					toCERFeedback);
			QuestionRW.putFeedback(key, toCERFeedback, local);
		}
		origFeedback = toCERFeedback;
	} else posn--;
	while (toCERFeedback.equals(origFeedback) && ++posn < numFeedbacks) {
		key = allKeys.get(posn);
		origFeedback = allFeedback.get(key);
		toCERFeedback = Utils.inputToCERs(origFeedback);
	} // look through feedback until we find one to convert
	if (posn < 0) posn++;

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE feedback converter</title>
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
		document.converter.convert.value = doIt;
		document.converter.submit();
	}

	function saveMe() {
		document.converter.convert.value = true;
		document.converter.useText.value = true;
		document.converter.submit();
	}
	// -->

	</script>
</head>

<body style="overflow:auto;">

<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

<div id="contentsWithoutTabs">
<form name="converter" action="feedbackConvert.jsp" method="post">
<input type="hidden" name="posn" value="<%= posn %>" />
<input type="hidden" name="convert" value="false" />
<input type="hidden" name="useText" value="false" />
<input type="hidden" name="haveLists" value="true" />
<input type="hidden" name="local" value="<%= local %>" />

<p>
<table style="width:95%; margin-left:auto; margin-right:auto;">
<tr>
<td class="boldtext big" style="padding-bottom:10px;">
ACE feedback converter
</td>
</tr>
<tr>
<td class="regtext" style="text-align-left;">

<% if (posn >= numFeedbacks) { %>

	There are no more feedback to convert.

<% } else { %>

	Current value of feedback <%= posn + 1 %> / <%= numFeedbacks %> with key <%= key %>:

	<p>
	<span style="background-color:yellow;"><%= XMLUtils.toValidXML(origFeedback) %></span>
	</p>

	appearing as:

	<p>
	<span style="background-color:yellow;"><%= origFeedback %></span>
	</p>

	After conversion to CERs:

	<p>
	<span style="background-color:yellow;"><%= XMLUtils.toValidXML(toCERFeedback) %></span>
	</p>

	Will appear as:

	<p>
	<span style="background-color:yellow;"><%= toCERFeedback %></span>
	</p>

	<p>
	Convert? 
	<input type="button" onclick="convertMe('true');" value="Yes" />
	<input type="button" onclick="convertMe('false');" value="No" />
	</p>

	<p>
	Or enter your own text: 
	<br/><br/><textarea id="enteredTxt" name="enteredTxt"
		style="height:100px; width:100%; font-family:Courier;"></textarea>
	<br/><input type="button" onclick="saveMe();" value="Save" />
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
