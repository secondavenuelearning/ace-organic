<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.db.QuestionRW,
	com.epoch.db.ResponseWrite,
	com.epoch.exceptions.DBException,
	com.epoch.exceptions.NonExistentException,
	com.epoch.exceptions.ParameterException,
	com.epoch.evals.EvalResult,
	com.epoch.genericQTypes.*,
	com.epoch.qBank.Question,
	com.epoch.session.GradeSet,
	com.epoch.session.HWSession,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.Calendar"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");

	final int hwNum = MathUtils.parseInt(request.getParameter("hwNum"));
	final int hwId = MathUtils.parseInt(request.getParameter("hwId"));
	final int qId = MathUtils.parseInt(request.getParameter("qId"));
	final String userId = request.getParameter("userId");
	final boolean isTutorial = "true".equals(request.getParameter("isTutorial"));
	final double oldGrade = MathUtils.parseDouble(request.getParameter("oldGrade"));
	final double newGrade = MathUtils.parseDouble(request.getParameter("newGrade"));

	GradeSet hwGrades;
	synchronized (session) {
		hwGrades = (GradeSet) session.getAttribute(isTutorial ? "tutGrades" : "hwGrades");
	} // synchronized
	final int numAttempts = MathUtils.parseInt(request.getParameter("numAttempts"));
	final int attemptNum = 
			MathUtils.parseInt(request.getParameter("attemptNum"), numAttempts);
	final int newAttempts = 
			MathUtils.parseInt(request.getParameter("newAttempts"));
	/* Utils.alwaysPrint("doReset.jsp: hwId = ", hwId, ", hwNum = ", hwNum, 
			", qId = ", qId, ", userId = ", userId, ", isTutorial = ", isTutorial,
			", numAttempts = ", numAttempts, ", attemptNum = ", attemptNum,
			", newAttempts = ", newAttempts); /**/

	final boolean[] flags = new boolean[3];
	final int REWRITE_RESP = ResponseWrite.REWRITE_RESP;
	final int EDIT_MOST_RECENT = ResponseWrite.EDIT_MOST_RECENT;
	final int PRESERVE_SUBSTNS = ResponseWrite.PRESERVE_SUBSTNS;
	flags[PRESERVE_SUBSTNS] = "on".equals(request.getParameter("preserveSubstns"));

	boolean resultRewritten = true;
	try {
		EvalResult evalResult = hwGrades.getResult(hwNum, userId, qId);
		boolean humanGradingDone = false;
		final int qType = QuestionRW.getQuestionType(qId, user.getUserId()); 
		final boolean setToNull = "on".equals(request.getParameter("setToNull"));
		if (setToNull) {
			humanGradingDone = evalResult.status == EvalResult.HUMAN_NEEDED;
			evalResult = null;
			/* Utils.alwaysPrint("doReset.jsp: setting evalResult to null; "
					+ "flags[PRESERVE_SUBSTNS] = ", flags[PRESERVE_SUBSTNS]); /**/
		} else {
			if (evalResult == null) {
				/* Utils.alwaysPrint("doReset.jsp: retrieved evalResult is null."); /**/
				evalResult = new EvalResult();
				evalResult.qId = qId;
			}
			humanGradingDone = evalResult.status == EvalResult.HUMAN_NEEDED;
			evalResult.tries = newAttempts;
			evalResult.status = (newAttempts == 0
					? EvalResult.INITIALIZED : EvalResult.EVALUATED);
			flags[EDIT_MOST_RECENT] = attemptNum == numAttempts; 
					// changing the grade or reducing the number of tries of 
					// the most recent response
			if (newAttempts != 0) {
				evalResult.grade = newGrade;
			} else {
				evalResult.grade = 0;
				// if Q is rank, choice, or fillblank, 
				// remove info about last choices 
				if (Question.isChoice(qType) || Question.isFillBlank(qType)) {
					flags[REWRITE_RESP] = true;
					/* Utils.alwaysPrint("doReset.jsp: resetting choice or "
							+ "fillblank lastResponse = ", evalResult.lastResponse); /**/
					evalResult.lastResponse =
							Choice.getUnchosenString(evalResult.lastResponse);
					/* Utils.alwaysPrint("doReset.jsp: new lastResponse = ", 
							evalResult.lastResponse); /**/
				} else if (Question.isChooseExplain(qType)) {
					flags[REWRITE_RESP] = true;
					final String sep = String.valueOf(ChooseExplain.SEPARATOR);
					/* Utils.alwaysPrint("doReset.jsp: resetting choose-explain "
							+ "lastResponse = ", 
							evalResult.lastResponse.replaceAll(sep, "[tab]")); /**/
					evalResult.lastResponse =
							ChooseExplain.getUnchosenString(evalResult.lastResponse);
					/* Utils.alwaysPrint("doReset.jsp: new lastResponse = '", 
							evalResult.lastResponse.replaceAll(sep, "[tab]")); /**/
				} else if (Question.isRank(qType)) {
					flags[REWRITE_RESP] = true;
					/* Utils.alwaysPrint("doReset.jsp: resetting rank "
							+ "lastResponse = ", evalResult.lastResponse); /**/
					evalResult.lastResponse =
							Rank.getUnrankedString(evalResult.lastResponse);
					/* Utils.alwaysPrint("doReset.jsp: new lastResponse = ", 
							evalResult.lastResponse); /**/
				} // if choice, fillBlank, rank
			} // if newAttempts is 0
			final String comment = request.getParameter("comment");
			evalResult.comment = Utils.inputToCERs(comment);
			/* Utils.alwaysPrint("doReset.jsp: setting grade to ", evalResult.grade, 
					" (old grade = ", oldGrade, ") and tries to ", 
					evalResult.tries, " for attempt number ", attemptNum, 
					" out of ", numAttempts, " on question with ID ", qId, 
					" of assignment ", hwNum, "; flags[EDIT_MOST_RECENT] = ", 
					flags[EDIT_MOST_RECENT], ", flags[REWRITE_RESP] = ", 
					flags[REWRITE_RESP], ", flags[PRESERVE_SUBSTNS] = ", 
					flags[PRESERVE_SUBSTNS]); /**/
		} // if not setToNull 
		final HWSession hwsession = new HWSession(hwId, qId, userId);
		hwsession.setResult(evalResult, flags);
		hwGrades.putInGradebook(userId, hwNum, qId, evalResult);
		if (humanGradingDone) hwGrades.unsetHumanGradingReqd(userId, hwNum);
		/* if (evalResult != null) Utils.alwaysPrint("doReset.jsp: modified grade is ", 
				evalResult.modGrade); /**/
	} catch (NonExistentException e) {
		Utils.alwaysPrint("doReset.jsp: exception thrown: ");
		e.printStackTrace();
		resultRewritten = false;
	} catch (DBException e) {
		Utils.alwaysPrint("doReset.jsp: exception thrown: ");
		e.printStackTrace();
		resultRewritten = false;
	} // try

%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script type="text/javascript" src="<%= pathToRoot %>js/jslib.js"></script>
	<title>Reset Record Results</title>
</head>
<body style="background-color:#e0e6c2; margin:0px; overflow:auto;">
	<table style="width:400px; background-color:#e0e6c2; text-align:center; 
			margin-left:auto; margin-right:auto;">
		<tr>
			<td class="regtext" colspan="2" style="width:400px; 
					padding-left:10px; padding-top:10px;">
				<% if (resultRewritten) { %>
					<%= user.translate("Alteration complete.") %> 
					<script type="text/javascript">
						opener.location.reload();
						self.close();
					</script>
				<% } else { %>
					<%= user.translate("Couldn't get good value for "
							+ "new grade or number of attempts, or "
							+ "exception thrown; aborted.") %>
				<% } %>
			</td>
		</tr>
	</table>
	<div style="position:fixed; bottom:5px; width:100%;">
	<table><tr><td>
		<%= makeButton(user.translate("Close"), 
				(resultRewritten ? "opener.location.reload(); " : "") 
					+ "self.close();") %>
	</td></tr></table>	
	</div>			
</body>
</html>
