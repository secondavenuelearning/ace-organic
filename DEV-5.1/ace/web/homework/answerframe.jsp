<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="java.util.TimeZone" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<% 	final String caller = "answerframe.jsp"; 
	final boolean isInstructorOrTA = isInstructor || isTA;
	final String pathToChooseRxnCondsUser = "";
	final int courseId = course.getId();
	final TimeZone timeZone = course.getTimeZone();
%>
<%@ include file="answerJava.jsp.h" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<title>ACE Question-answering page</title>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<style type="text/css">
	* html body {
		padding:100px 0 0px 0; 
	}
</style>
<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/clock.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/lewisJS.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/position.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/svgGraphics.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/wz_jsgraphics.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= 
		AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<% if (isSynthesis) { %>
	<script src="<%= pathToRoot %>js/rxnCondsEditor.js" type="text/javascript"></script>
<% } else if (isOED || isRCD) { %>
	<script src="<%= pathToRoot %>js/oedAndRcd.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/<%= isOED 
			? "oed" : "rcd" %>.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/position.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/wz_jsgraphics.js" type="text/javascript"></script>
<% } else if (isClickableImage || isDrawVectors) { %>
	<script src="<%= pathToRoot %>js/drawOnFig.js" type="text/javascript"></script>
<% } else if (isLogicalStmts) { %>
	<script src="<%= pathToRoot %>js/logicStmts.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/wordCheck.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<% } else if (isEquations) { %>
	<script src="<%= pathToRoot %>js/equations.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
 	<script src="<%= pathToRoot %>nosession/mathjax/MathJax.js?config=TeX-AMS-MML_HTMLorMML.js" 
			type="text/javascript"></script>
<% } // if question type %>
<% if (question.hasJmolFigure()) { %>
	<script src="<%= pathToRoot %>js/jmolStart.js" type="text/javascript"></script>
		<!-- the next two resources must be called in the given order -->
	<script src="<%= pathToRoot %>nosession/jsmol/JSmol.min.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>nosession/jsmol/Jmol2.js" type="text/javascript"></script>
<% } // if question has a Jmol figure %>
<script type="text/javascript">

// <!-- >
<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
<%@ include file="/navigation/courseSidebarJS.jsp.h" %>
<%@ include file="/js/marvinQuestionConstants.jsp.h" %>
<%@ include file="answerJS.jsp.h" %>
// -->

</script>
</head>

<body style="text-align:center; margin:0px; overflow:auto;" 
		onload="setTab('<%= toTabName(user.translateJS("Assignments")) %>'); 
				setStatusFeedback(); getButtons(); setNavigation();<%= 
				hwsession.isExam() ? " setUpClock();" : "" %><%=
				showClock ? " showClockRemaining();" : "" %><%=
				isClickableImage || isDrawVectors ? " initDrawOnFigure();" 
				: isLewis ? " initLewis();"
				: isSynthesis ? " initSynthesis();"
				: isLogicalStmts ? " setUpStmts();" 
				: isEquations ? " setUpEqns();" 
				: isOED || isRCD ? " initED();"
				: "" %>">

<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>
<div id="contentsWithTabsWithoutFooter">
<%@ include file="answerHTML.jsp.h" %>
</div>
</body>
</html>

