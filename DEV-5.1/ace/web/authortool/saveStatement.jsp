<%@ page language="java" %>
<%@ page import="
	com.epoch.qBank.QSetDescr,
	com.epoch.session.QuestionBank,
	com.epoch.session.QSet,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	/*
		chapindex
		index
		name
		author
		header
		remarks
	*/
	
	QuestionBank qBank;
	QSet currentQSet;
	synchronized (session) {
		qBank = (QuestionBank) session.getAttribute("qBank");
		currentQSet = (QSet) session.getAttribute("qSet");
	}
	final QSetDescr descr = currentQSet.getQSetDescr();
	final String newHeader = Utils.inputToCERs(request.getParameter("statement"));
	final boolean masterEdit = "true".equals(request.getParameter("master"));
    final int topicNum = MathUtils.parseInt(request.getParameter("topicNum"));
	final int indexInTopic = MathUtils.parseInt(request.getParameter("indexInTopic"));
	/* Utils.alwaysPrint("saveStatement.jsp: master = ", masterEdit,
			", topicNum (in order, not unique ID) = ", topicNum,
			", Qset indexInTopic = ", indexInTopic,
			", new statement = ", newHeader); /**/
	if (!descr.header.equals(newHeader)) {
		descr.header = newHeader;
		if (masterEdit) {
			try {
				qBank.setQSetDescr(topicNum, indexInTopic, descr);
				currentQSet.setHeader(newHeader);
			} catch (Exception e) {
				Utils.alwaysPrint("Exception while trying to save new statement "
						+ "in QSetDescr.");
				e.printStackTrace();
			}
		} else {
			try {
				currentQSet.addLocalHeader(newHeader);
			} catch (Exception e) {
				Utils.alwaysPrint("Exception while trying to save "
						+ "locally modified header.");
				e.printStackTrace();
			}
		}
	}
	
	/* descr = currentQSet.getQSetDescr();
	Utils.alwaysPrint("saveStatement.jsp: new header now = ", descr.header, 
			", locally modified = ", descr.headerModifiedLocally); /**/
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
	<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
		// <!-- >
		function closeThis() {
			window.opener.setInnerHTML('commonQstatement', 
					'<%= Utils.isEmpty(descr.header) ? "[None]" 
					: Utils.toValidJS(Utils.toDisplay(descr.header)) %>');
			window.opener.setValue('commonQStmtHidden', 
					'<%= Utils.toValidJS(descr.header) %>');
			<% if (descr.headerModifiedLocally) { %>
				window.opener.showCell('revertButton');
			<% } %>	
			self.close();
		} // closeThis
		// -->
	</script>
	</head>

<body onload="closeThis();"></body>
</html>

