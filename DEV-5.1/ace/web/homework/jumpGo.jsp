<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.session.HWSession,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final int SOLVE = HWSession.SOLVE;
	final int PREVIEW = HWSession.PREVIEW;
	final int TEXTBOOK = HWSession.TEXTBOOK;
	final int GRADEBOOK_VIEW = HWSession.GRADEBOOK_VIEW;
	final int SIMILAR = HWSession.SIMILAR;
	
	HWSession hwsession;
	synchronized (session) {
		hwsession = (HWSession) session.getAttribute("hwsession");
	} // session
	if (hwsession == null) {
		%> <jsp:forward page="../errormsgs/noSession.html"/> <%
	}
	final int qNum = MathUtils.parseInt(request.getParameter("qNum"));
	// negative Q number comes from gradebook: one question in assignment, want
	// to display assignment's question number
	hwsession.setCurrentIndex(qNum < 0 ? 1 : qNum);

	int mode = MathUtils.parseInt(request.getParameter("mode"), SOLVE);
	final boolean notAssignmentMode = 
			Utils.among(mode, PREVIEW, TEXTBOOK, GRADEBOOK_VIEW);
	final String goWhere = Utils.toString("answer", 
			notAssignmentMode ? "Preview" : "frame");
	final String qStmt = request.getParameter("qStmt");
	final boolean solveRelatedMasteryQ = 
			"true".equals(request.getParameter("solveRelatedMasteryQ"));
	if (mode == PREVIEW && qStmt != null) {
		synchronized (session) {
			session.setAttribute("qStmt", qStmt);
		} // session
	} else if (mode == SIMILAR) {
		final boolean usesSubstns = hwsession.getCurrentQuestion().usesSubstns();
		if (usesSubstns) { // could be "practice similar" or "solve related"
			hwsession.generateNewSubstns(solveRelatedMasteryQ);
			/* Utils.alwaysPrint("jumpGo.jsp: mode is SIMILAR, new R groups are ", 
					hwsession.getCurrentSubstns()); /**/
		} else { // must be "solve related"
			final int oldQId = hwsession.getCurrentQId();
			hwsession.reinstantiate(qNum);
			/* Utils.alwaysPrint("jumpGo.jsp: replaced qId ", oldQId, " with ",
			 		hwsession.getCurrentQId()); /**/
		} // if uses R groups
		if (solveRelatedMasteryQ) {
			mode = SOLVE;
			 Utils.alwaysPrint("jumpGo.jsp: mode is SIMILAR, but "
			 		+ "solveRelatedMasteryQ is true, so "
			 		+ "changing mode to SOLVE"); /**/
		} // if solveRelatedMasteryQ
	} // if mode
	/* Utils.alwaysPrint("jumpGo.jsp: mode = ", HWSession.getModeName(mode),
			", qNum = ", qNum); /**/
	synchronized (session) {
		session.setAttribute("mode", mode);
		session.setAttribute("pts", request.getParameter("pts"));
		if (notAssignmentMode) { 
			session.setAttribute("isInstructorOrTA", 
					request.getParameter("isInstructorOrTA"));
		 } // if mode
	} // synchronized
	final boolean showClock = "true".equals(request.getParameter("showClock"));

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
	function goToSolve() {
		self.location.href = '<%= goWhere %>.jsp?qNum=<%= 
				qNum %>&solveRelatedMasteryQ=<%= 
				solveRelatedMasteryQ %>&showClock=<%= showClock %>';
	} // goToSolve()
	// -->
	</script>
</head>
<body onload="goToSolve();">
</body>
</html>
