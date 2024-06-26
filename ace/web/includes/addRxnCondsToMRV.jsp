<%@ page language="java" %><%@ 
	page import="
	com.epoch.chem.ChemUtils,
	com.epoch.synthesis.Synthesis,
	com.epoch.utils.Utils"
%><%@ 
	page errorPage="/errormsgs/errorHandler.jsp" %><%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters
	String mrvStr = request.getParameter("mrvStr");
	final String rxnIdsStr = request.getParameter("rxnIdsStr");
	/* Utils.alwaysPrint("addRxnCondsToMRV.jsp: rxnIds = ", rxnIdsStr,
			", mrvStr = ", mrvStr); /**/
	if (!Utils.isEmpty(mrvStr)) {
		mrvStr = ChemUtils.setProperty(mrvStr, Synthesis.RXN_IDS, rxnIdsStr);
		// Utils.alwaysPrint("addRxnCondsToMRV.jsp: modified mrvStr to: ", mrvStr);
	} // if there are a molecule and reaction IDs
%><?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Source code</title>
<script type="text/javascript" src="<%= pathToRoot %>js/jslib.js"></script>
<script type="text/javascript">
	// <!-- >

	function closeMe(e) {
		if (e && ([10, 13].contains(e.keyCode))) self.close();
	} // closeMe()

	function selectAll() {
		selectText(document.getElementById('sourceCode'));
	} // selectAll()

	// mrvStrValue = @@@@<%= Utils.lineBreaksToJS(mrvStr) %>@@@@

	// -->
</script></head><body onload="selectAll();"
	onkeypress="closeMe(event);" style="overflow:auto;"><table 
	class="regtext" style="margin-left:auto; margin-right:auto; 
		width:95%;"><tr><td id="sourceCode"><pre><%= 
			Utils.toValidHTML(mrvStr == null ? "" : mrvStr) 
%></pre></td></tr></table></body></html>
