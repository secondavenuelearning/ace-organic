<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.utils.MathUtils"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");

	final int hwNum = MathUtils.parseInt(request.getParameter("hwNum"));
	final int hwId = MathUtils.parseInt(request.getParameter("hwId"));
	final int qId = MathUtils.parseInt(request.getParameter("qId"));
	final int qNum = MathUtils.parseInt(request.getParameter("qNum"));
	final int studentNum = MathUtils.parseInt(request.getParameter("studentNum"));
	final boolean isTutorial = "true".equals(request.getParameter("isTutorial"));
%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >

<head>
	<title>Forced Regrade</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>

	<script type="text/javascript">
	// <!-- >
	function doRegrade() {
		setInnerHTML('narrative', 
				'<%= user.translateJS("ACE is regrading the students' "
					+ "responses. This action may take a while, so please "
					+ "be patient. A new message will "
					+ "appear when this action is complete.") %>');
		clearInnerHTML('buttons');
		document.regradeForm.submit();
	} // doRegrade()
	// -->
	</script>

</head>
<body style="background-color:#e0e6c2; margin:0px;">
	<form name="regradeForm" action="doRegrade.jsp" method="post">
	<input type="hidden" name="hwNum" value="<%= hwNum %>" />
	<input type="hidden" name="hwId" value="<%= hwId %>" />
	<input type="hidden" name="qId" value="<%= qId %>" />
	<input type="hidden" name="qNum" value="<%= qNum %>" />
	<input type="hidden" name="studentNum" value="<%= studentNum %>" />
	<input type="hidden" name="isTutorial" value="<%= isTutorial %>" />
	<table style="width:400px; background-color:#e0e6c2; text-align:center; 
			margin-left:auto; margin-right:auto;">
		<tr>
			<td class="boldtext enlarged" style="padding-left:10px;
			padding-top:10px;">
				<%= user.translate("Forced Regrade") %>
			</td>
		</tr>
		<tr>
			<td id="narrative" class="regtext" style="padding-left:10px; 
					padding-right:10px; padding-top:10px;">
				<%= user.translate(Utils.toString(
						"If you continue, ACE will recalculate ", 
						qId != 0 && studentNum <= 0 
							? "the grades of all students who attempted this question"
						: qId != 0
							? "the grade of this student on this question"
						: studentNum <= 0 
							? "all students' grades"
						: "all of this student's grades", 
						" in this assignment."))
				%>
				<%= user.translate("This function is "
					+ "useful if you have had to amend the correct answer to "
					+ "a question after students have already started working "
					+ "on the assignment. ACE will provide a list of students "
					+ "whose grades have changed.") %>
			</td>
		</tr>
		<tr><td id="buttons" style="text-align:left; padding-left:10px; 
				padding-right:10px; padding-top:10px;">
		<table><tr>
			<td style="padding-bottom:10px;">
				<%= makeButton(user.translate("Regrade now"), "doRegrade();") %>
			</td>
			<td style="padding-bottom:10px;">
				<%= makeButton(user.translate("Cancel"), "self.close();") %>
			</td>
		</tr></table></td></tr>
	</table>
	</form>

</body>
</html>
