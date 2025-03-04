<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.evals.EvalResult,
	com.epoch.exceptions.ParameterException,
	com.epoch.exceptions.VerifyException,
	com.epoch.session.HWSession,
	com.epoch.utils.Utils"
%>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache, must-revalidate"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final String submitHWNumsStr = request.getParameter("hwNums"); // 1-based
	final int[] submitHWNums = Utils.stringToIntArray(submitHWNumsStr.split(":"));

	final String userId = user.getUserId();
	final boolean isCoInstructorOrTA = false;
	final String[] userLangs = null; // irrelevant
	int numEvaluated = 0;
	for (final int hwNum : submitHWNums) {
		final Assgt assgt = assgts[hwNum - 1];
		// set indefinite extension to ensure result will be recorded
		final double origExtension = assgt.getExtension(userId);
		assgt.addExtension(userId, Assgt.INDEFINITE);
		/* Utils.alwaysPrint("submitUnsubmitted.jsp: setting session for hw ", 
				hwNum, " with ID = ", assgt.id); /**/
		final HWSession oneHW = new HWSession(user, assgt, 
				isCoInstructorOrTA, request.getRemoteAddr(), true);
		if (oneHW.isExam()) {
			/* Utils.alwaysPrint("submitUnsubmitted.jsp: responses to be evaluated "
					+ "are from an exam."); /**/
			synchronized (session) {
				oneHW.prepareForExam(course.getTimeZone(), request.getRemoteHost());
			} // synchronized
		} // if is exam
		final int numQs = oneHW.getCount();
		for (int qNum = 1; qNum <= numQs; qNum++) {
			// Utils.alwaysPrint("submitUnsubmitted.jsp: looking at Q ", qNum);
			try {
				oneHW.setCurrentIndex(qNum);
				final EvalResult currentResult = oneHW.getCurrentResult();
				if (currentResult != null
						&& currentResult.status == EvalResult.SAVED) {
					/* Utils.alwaysPrint("submitUnsubmitted.jsp: "
							+ "evaluating unsubmitted response to Q", qNum, 
							" of assignment ", hwNum); /**/
					numEvaluated++;
					oneHW.submitResponse(currentResult.lastResponse, 
							HWSession.SOLVE);
				} // if this Q was saved but not submitted
			} catch (VerifyException e0) {
				// do nothing
			} catch (ParameterException e1) {
				Utils.alwaysPrint("submitUnsubmitted.jsp: "
						+ "caught ParameterException while getting Q", 
						qNum, " for assignment ", hwNum);
				e1.printStackTrace();
			} // try
		} // for each Q in this assignment
		assgt.addExtension(userId, origExtension);
	} // for each assignment with unsubmitted responses
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
</head>
<body onload="alert('<%= user.translateJS("***1*** response(s) submitted and evaluated", 
	numEvaluated) %>'); self.location.href='<%= pathToRoot %>hwcreator/hwSetList.jsp';">
</body>
</html>

