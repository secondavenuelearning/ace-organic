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
	Map<String, String> allQData;
	ArrayList<String> allKeys;
	synchronized (session) {
		allQData = (Map<String, String>) session.getAttribute("allQData");
		allKeys = (ArrayList<String>) session.getAttribute("allQDataKeys");
	}
	if (allQData == null || !haveLists) {
		allQData = QuestionRW.getConvertibleQData(local);
		Utils.alwaysPrint("qDataConvert.jsp: got from DB ",
				allQData.size(), " qData.");
		allKeys = new ArrayList<String>(allQData.keySet());
		synchronized (session) {
			session.setAttribute("allQData", allQData);
			session.setAttribute("allQDataKeys", allKeys);
		}
	} else
		Utils.alwaysPrint("qDataConvert.jsp: got from session ",
				allQData.size(), " qData.");
	final int numQDatas = allQData.size();
	final String posnStr = request.getParameter("posn");
	int posn = MathUtils.parseInt(posnStr);
	String key = (numQDatas > 0 ? allKeys.get(posn) : "");
	String origQData = (numQDatas > 0 ? allQData.get(key) : "");
	String toCERQData = Utils.inputToCERs(origQData);
	if (posnStr != null) {
		final boolean convert = "true".equals(request.getParameter("convert"));
		if (convert) {
			final boolean useText = "true".equals(request.getParameter("useText"));
			if (useText) {
				toCERQData = Utils.inputToCERs(request.getParameter("enteredTxt"));
			}
			Utils.alwaysPrint("qDataConvert.jsp: storing qData ", key, ", posn ",
					posn, ": original = ", origQData, "\nnew = ", toCERQData);
			QuestionRW.putQData(key, toCERQData, local);
		}
		origQData = toCERQData;
	} else posn--;
	while (toCERQData.equals(origQData) && ++posn < numQDatas) {
		key = allKeys.get(posn);
		origQData = allQData.get(key);
		toCERQData = Utils.inputToCERs(origQData);
	} // look through Q data until we find one to convert
	if (posn < 0) posn++;

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Question data converter</title>
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
<form name="converter" action="qDataConvert.jsp" method="post">
<input type="hidden" name="posn" value="<%= posn %>" />
<input type="hidden" name="convert" value="false" />
<input type="hidden" name="useText" value="false" />
<input type="hidden" name="haveLists" value="true" />
<input type="hidden" name="local" value="<%= local %>" />

<p>
<table style="width:95%; margin-left:auto; margin-right:auto;">
<tr>
<td class="boldtext big" style="padding-bottom:10px;">
ACE Question data converter
</td>
</tr>
<tr>
<td class="regtext" style="text-align-left;">

<% if (posn >= numQDatas) { %>

	There are no more question data to convert.

<% } else { %>

	Current value of question datum <%= posn + 1 %> / <%= numQDatas %> with key <%= key %>:

	<p>
	<span style="background-color:yellow;"><%= XMLUtils.toValidXML(origQData) %></span>
	</p>

	appearing as:

	<p>
	<span style="background-color:yellow;"><%= origQData %></span>
	</p>

	After conversion to CERs:

	<p>
	<span style="background-color:yellow;"><%= XMLUtils.toValidXML(toCERQData) %></span>
	</p>

	Will appear as:

	<p>
	<span style="background-color:yellow;"><%= toCERQData %></span>
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
