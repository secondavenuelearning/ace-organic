<%@ page language="java" %>
<%@ page import="
	com.epoch.db.ReactorResultsRW,
	com.epoch.exceptions.DBException,
	com.epoch.synthesis.Synthesis,
	com.epoch.synthesis.SynthError,
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

	final String reactionStr = request.getParameter("reaction");
	if (reactionStr == null) {
    	%> <jsp:forward page="../errorParam.jsp"/> <%
    }
	final int rxnId = MathUtils.parseInt(request.getParameter("rxnId"));

	String msg = "";
	try {
		final Synthesis synthReaction = new Synthesis(reactionStr);
		synthReaction.storeReaction(rxnId);
	} catch (SynthError e) {
		msg = "A SynthError occurred. Your changes were not saved.";
		e.printStackTrace();
	}

%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<script type="text/javascript">
	// <!-- >
	function finish() {
		<% if ("".equals(msg)) { %>
			opener.location.reload(); 
		<% } else { %>
			alert('<%= msg %>');
		<% } %>
		self.close();
	}
	// -->
	</script>
</head>
<body onload="finish();">
</body>

</html>
