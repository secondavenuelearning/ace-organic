<%@ page language="java" %>
<%@ page import="
	com.epoch.session.QSet,
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
	final String pathToRoot = "../../";

	final String serialNos = request.getParameter("serialNos");
	final int qSetId = MathUtils.parseInt(request.getParameter("qSetId"));
	if (qSetId == 0) {
    	%> <jsp:forward page="../errorParam.jsp"/> <%
	}

	/* Utils.alwaysPrint("changeQset.jsp: moving Qs ", serialNos,
			" to qSet with id ", qSetId); /**/
    QSet qSet;
	synchronized (session) {
		qSet = (QSet) session.getAttribute("qSet");
	}
	qSet.moveQtoNewQset(serialNos, qSetId);

%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	function goBack() {
		var locn = window.opener.location.href;
		if (locn.indexOf('reentry=') >= 0) {
			var locnParts = locn.split('reentry=');
			var locnBld = new String.builder();
			locnBld.append(locnParts[0].endsWith('?') ?
					locnParts[0] :
					locnParts[0].substring(0, locnParts[0].length - 1));
			var ampPosn = locnParts[1].indexOf('&');
			locnBld.append(ampPosn === -1 ? 
					locnParts[1] : locnParts[1].substring(ampPosn));
			locn = locnBld.toString();
		}
		if (locn === window.opener.location.href) opener.location.reload();
		else window.opener.location.href = locn;
		self.close();
	} // goBack()

	// -->
</script>
</head>

<body onload="goBack();">
	Saved!!!
</body>
</html>

