<%@ page language="java" %>
<%@ page import="
	com.epoch.evals.Evaluator,
	com.epoch.evals.Expression,
	com.epoch.qBank.Question,
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
	
    /* Input
            evalNums 1:2:3 etc..
            feedback
            grade
    */

    final String evalNumsStr = request.getParameter("evalNums");
	final String feedback = Utils.inputToCERs(request.getParameter("feedback"));
	final String exprCodeNested = request.getParameter("exprCodeNested");
    final String gradeStr = request.getParameter("grade");

	Question question;
	Evaluator joinedEval;
	synchronized (session) {
		question = (Question) session.getAttribute("qBuffer");
		joinedEval = (Evaluator) session.getAttribute("joinedEvaluator");
	}
    if (evalNumsStr == null || question == null) {
    %>
<jsp:forward page="../errorParam.jsp"/>
    <%
    }

	final int[] evalNums = Utils.stringToIntArray(evalNumsStr.split(":"));
	final boolean existingEval = evalNums.length == 1;
	joinedEval.setNestedExprCode(exprCodeNested);
	joinedEval.grade = MathUtils.parseDouble(gradeStr, 1);
	joinedEval.feedback = feedback;
	/*
	Utils.alwaysPrint("saveJoinedEvaluators.jsp: evalNums = ", evalNums,
			", majorId of input eval = ", joinedEval.majorId,
			", exprCodeNested = ", exprCodeNested, 
			", exprCode of input eval = ", joinedEval.exprCode);
	/**/
	if (existingEval) question.setEvaluator(evalNums[0], joinedEval);
	else question.joinEvaluators(evalNums, joinedEval);

%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<script type="text/javascript">
		function finish() {
			opener.location.href = '../question.jsp?qId=same';
			self.close();
		}
	</script>
</head>
<body onload="finish();">
	Saved.
</body>
</html>
