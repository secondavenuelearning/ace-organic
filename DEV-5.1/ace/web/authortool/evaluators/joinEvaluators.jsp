<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.access.EpochEntry,
	com.epoch.evals.CombineExpr,
	com.epoch.evals.EvalManager,
	com.epoch.evals.Evaluator,
	com.epoch.evals.Subevaluator,
	com.epoch.qBank.Question,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.List"
%>

<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	EpochEntry entry1;
	Question question;
	int qType;
	long qFlags;
	synchronized (session) {
		entry1 = (EpochEntry) session.getAttribute("entry");
		question = (Question) session.getAttribute("qBuffer");
		qType = MathUtils.parseInt((String) session.getAttribute("qType"));
		qFlags = MathUtils.parseLong((String) session.getAttribute("qFlags"));
	}

	/* 
	  evalNumsStr -  1:2:3: or just 1 if already formed
	*/

	final String evalNumsStr = request.getParameter("evalNums");
	if (evalNumsStr == null || question == null) {
	%>
		<jsp:forward page="../errorParam.jsp"/>
	<%
	}

	final int RETURN = 0;
	final int ADD_NEW = 1;
	final int ADD_CLONE = 2;
	final String SELECTED = "selected=\"selected\" ";

	final boolean masterEdit = entry1.isMasterEdit();
	final String bgColor = (masterEdit ? "#f6edf7" : "#f6f7ed");
	final boolean isFromQuestionJSP = "true".equals(request.getParameter("virgin"));
	final int[] evalNums = Utils.stringToIntArray(evalNumsStr.split(":"));
	final boolean existingEval = evalNums.length == 1;
	final Evaluator inputParentEval = (existingEval
			? question.getEvaluator(evalNums[0])
			: question.getJoinedEvaluator(evalNums));
	final boolean chemFormatting = question.chemFormatting(qType, qFlags);
	synchronized (session) {
		session.setAttribute("joinedEvaluator", inputParentEval);
	}
	final List<Subevaluator> subevals = inputParentEval.getSubevaluators();
	final String[] combParts = 
			CombineExpr.getNestedArray(inputParentEval.exprCodeToNested());
	final int numCombParts = combParts.length;
	final int evalNum = (existingEval ? evalNums[0] : 0);
	final boolean foreignEval = false;
	final boolean cloneEdit = false;
	final boolean editingSubeval = false;
	final boolean editingParent = true;
	final int[] allowedEvalConstants = new int[0];
	final int evalConstant = 0;

	final int headerHeight = 80;
	final int footerHeight = 135;
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Joined Evaluator Editor</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon">
	<style type="text/css">
	body {
		margin:0;
		border:0;
		padding:0;
		height:100%; 
		max-height:100%; 
		font-family:arial, verdana, sans-serif; 
		font-size:76%;
		overflow: hidden; 
	}

	#evalHeader {
		position:absolute; 
		top:0; 
		left:0; 
		width:100%; 
		height:<%= headerHeight %>px; 
		overflow:auto; 
	}

	#evalFooter {
		position:absolute; 
		bottom:0; 
		left:0;
		width:100%; 
		height:<%= footerHeight %>px; 
		overflow:auto; 
		text-align:right; 
		vertical-align:bottom;
		padding-top:20px;
	}

	#evalContents {
		position:fixed; 
		top:<%= headerHeight %>px;
		left:0;
		bottom:<%= footerHeight %>px; 
		right:0; 
		overflow:auto; 
	}

	* html #evalContents {
		height:100%; 
	}

	* html body {
		padding:<%= footerHeight %>px 0 <%= footerHeight %>px 0; 
	}
	</style>
	<script src="<%= pathToRoot %>js/combineExpr.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script>
		// <!-- >

		function initGrade() {
			toggleGradeSelector();
			changeGrade();
		} // initGrade()

		function initiateCombParts() {
			var combParts = new Array();
			<% for (final String combPart : combParts) { %>
				combParts.push('<%= combPart %>');
			<% } // for each character %>
			initiateConstants('<%= CombineExpr.OPEN_PAREN %>', 
					'<%= CombineExpr.CLOSE_PAREN %>',
					'<%= CombineExpr.AND %>', 
					'<%= CombineExpr.OR %>', 
					'<%= CombineExpr.OF %>',
					'<%= CombineExpr.TO %>');
			var numRules = <%= subevals.size() %>;
			initiateSelectors(combParts, numRules);
		} // initiateCombParts()

		function submitIt(afterSave) {
			document.qDataForm.submit();
		} // submitIt()

		function changeGrade() {
			var form = document.evaluatorForm;
			var gradeType = form.correct_type.value;
			if (gradeType === 'full') {
				form.grade.value = 1.0;
				hideLayer('gradebox');
			} else if (gradeType === 'partial') {
				if (['1', '0'].contains(form.grade.value)) form.grade.value = 0.5;
				showLayer('gradebox');
			} else if (gradeType === 'wrong') {
				form.grade.value = 0.0;
				hideLayer('gradebox');
			}
		} // changeGrade()

		function getFeedback() {
			return '<%= Utils.toValidJS(
					Utils.toValidTextbox(inputParentEval.feedback)) %>';
		} // getFeedback()

		function submitIt() {
			var form = document.evaluatorForm;
			if (form.feedback) {
				var feedback = form.feedback.value;
				form.feedback.value = trimWhiteSpaces(feedback);
			}
			var expr = getCombineExpr();
			if ('(123456789'.indexOf(expr.charAt(0)) < 0) { // <!-- >
				alert(expr);
				return;
			} // if the expression doesn't begin properly
			form.exprCodeNested.value = expr;
			form.submit();
		} // submitIt()

		// -->
	</script>
</head>
<body class="light" style="margin:0px; margin-top:5px; overflow:auto;
		background-color:<%= bgColor %>; text-align:center;"
		onload="initGrade(); initiateCombParts();">

<form name="evaluatorForm" action="saveJoinedEvaluators.jsp" method="post"
		accept-charset="UTF-8">
	<input type="hidden" name="evalNums" value="<%= evalNumsStr %>" />
	<input type="hidden" name="exprCodeNested" value="" />

<%@ include file="selectorFooter.jsp.h" %>

<div id="evalContents">
<table class="whiteTable" 
		style="margin-left:auto; margin-right:auto; width:80%; text-align:center;">
	<tr><td class="regtext" style="color:green;
			padding-top:10px; padding-bottom:10px;">
		Enter an expression for combining the rules about permissible starting
		materials.  
		You may use rules more than once and in any sequence.  
		Use as many sets of parentheses as you need to make the expression 
		unambiguous.
	</td></tr>
	<tr><td id="combinationExpression" style="text-align:left; padding-top:10px;">
	</td></tr>
<%
	boolean autoFeedback = false;
	int subevalNum = 0;
	for (final Subevaluator subeval : subevals) {
		if (subeval.givesAutoFeedback()) autoFeedback = true;
%>
		<tr> 
		<td class="regtext" style="padding-left:20px;">
			<b><%= ++subevalNum %></b>
			<%= chemFormatting ? Utils.toDisplay(subeval.toEnglish()) 
					: subeval.toEnglish() %> 
		</td>
		</tr>
	<% } // for each subevaluator %>
	</table>
	<% if (autoFeedback) { %>
		<table style="width:90%; margin-left:auto; margin-right:auto;" summary="">
			<tr><td class="regtext" style="color:green; 
					padding-top:30px;">
				<b>Note</b>: One of the subevaluators listed above 
				may generate feedback automatically.  ACE will display 
				this feedback to the user if and only if the subevaluator 
				is <b>last</b> in a list combined by <b>OR</b> 
				or <b>first</b> in a list combined by <b>AND</b>.
				If ACE displays automatic feedback, it will append your 
				feedback.</td></tr>
		</table>
	<% } // if autoFeedback %>
</div>

</form>
</body>
</html>
