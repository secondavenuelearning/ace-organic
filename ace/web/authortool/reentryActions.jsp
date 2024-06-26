<%@ page language="java" %>
<%@ page import="
	com.epoch.qBank.Question,
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

	final String action = request.getParameter("action");
	Question question;
	synchronized (session) {
		question = (Question) session.getAttribute("qBuffer");
	}

	if ("split".equals(action)) {
		final int evalNum = MathUtils.parseInt(request.getParameter("evalNum"));
		question.splitEvaluator(evalNum);
	} else if ("moveEval".equals(action)) {
		final int from = MathUtils.parseInt(request.getParameter("from"));
		final int to = MathUtils.parseInt(request.getParameter("to"));
		question.moveEvaluator(from, to);
    } else if ("cloneEval".equals(action)) {
		final int evalNum = MathUtils.parseInt(request.getParameter("evalNum"));
        question.cloneEvaluator(evalNum);
    } else if ("deleteEval".equals(action)) {
		final int evalNum = MathUtils.parseInt(request.getParameter("evalNum"));
		question.deleteEvaluator(evalNum);
	} else if ("moveQDatum".equals(action)) {
		final int tableNum = MathUtils.parseInt(request.getParameter("tableNum"));
		final int from = MathUtils.parseInt(request.getParameter("from"));
		final int to = MathUtils.parseInt(request.getParameter("to"));
		question.moveQDatum(tableNum, from, to);
	} else if ("deleteQDatum".equals(action)) { 
		final int tableNum = MathUtils.parseInt(request.getParameter("tableNum"));
        final int qDatumNum = MathUtils.parseInt(request.getParameter("qDatumNum"));
        question.deleteQDatum(tableNum, qDatumNum);
	} else if ("deleteFigure".equals(action)) {
		final int index = MathUtils.parseInt(request.getParameter("index"));
		question.deleteFigure(index);
    } // if action

%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<script type="text/javascript">
		// <!-- >
		function reloadQuestion() {
			self.location.href = 'question.jsp?qId=same'; 
		}
		// -->
	</script>
</head>
<body onload="reloadQuestion();">
</body>
</html>

