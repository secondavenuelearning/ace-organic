<%@ page language="java" %>
<%@ page import="
	com.epoch.synthesis.RxnCondition,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

    /* 
		rxnId is  0  | seq num of reaction
	 */
	final int rxnId = MathUtils.parseInt(request.getParameter("rxnId"));
	final String rxnDef = request.getParameter("rxnDef");
	final String rxnName = Utils.inputToCERs(request.getParameter("rxnName"));
	final String rxnClassifn = Utils.inputToCERs(request.getParameter("rxnClassifn"));

	if (rxnDef == null || rxnName == null) {
    	%> <jsp:forward page="../errorParam.jsp"/> <%
    }

	final boolean threeComponent = "on".equals(request.getParameter("threeComponent"));
	Utils.alwaysPrint("saveReaction.jsp: threeComponent = ", threeComponent,
			", rxnDef:\n", rxnDef.length() > 300 
				? rxnDef.substring(0, 300) + "\n..." : rxnDef);
	final RxnCondition rxnCondn = new RxnCondition(rxnId, 
			rxnName.trim(), rxnDef, rxnClassifn.trim(), threeComponent);
	rxnCondn.setRxnCondition();

%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
</head>
<body onload="opener.location.reload(); self.close();">
	Saved.
</body>

</html>
