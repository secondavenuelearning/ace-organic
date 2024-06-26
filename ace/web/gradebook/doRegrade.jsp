<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.session.GradeSet,
	com.epoch.session.RegradeSession,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.List"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");

	final int hwNum = MathUtils.parseInt(request.getParameter("hwNum"));
	final int hwId = MathUtils.parseInt(request.getParameter("hwId"));
	final int qId = MathUtils.parseInt(request.getParameter("qId"));
	final int qNum = MathUtils.parseInt(request.getParameter("qNum"));
	final int studentNum = MathUtils.parseInt(request.getParameter("studentNum"));
	final boolean isTutorial = "true".equals(request.getParameter("isTutorial"));

	GradeSet hwGrades;
	String[] userIds; 
	String[] userNames; 
	synchronized (session) {
		hwGrades = (GradeSet) session.getAttribute(isTutorial ? "tutGrades" : "hwGrades");
		userIds = (String[]) session.getAttribute("userIds");
		userNames = (String[]) session.getAttribute("userNames");
	}
	final String instructorId = user.getUserId();
	final RegradeSession rgs = new RegradeSession(hwId, hwNum, qId, instructorId, 
			studentNum > 0 ? userIds[studentNum - 1] : null, hwGrades);

	final String[] userIdsGradeChange = rgs.doRegrade();
	final boolean gradesHaveChanged = !Utils.isEmpty(userIdsGradeChange);
	// get names of users whose grades have changed, keeping them in the same
	// alphabetical order as in userIds and userNames
	final String[] userNamesGradeChangeArr = new String[userIds.length];
	for (final String userId : userIdsGradeChange) { 
		final int posn = Utils.indexOf(userIds, userId);
		if (posn >= 0) userNamesGradeChangeArr[posn] = userNames[posn];
	} // for each student whose grade changed
	// get rid of empty records
	final List<String> userNamesGradeChangeList = new ArrayList<String>();
	for (final String userName : userNamesGradeChangeArr) { 
		if (!Utils.isEmpty(userName)) {
			userNamesGradeChangeList.add(userName);
		} // if this user's grade has changed
	} // for each potential username

%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<title>Regrade Results</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>

	<script type="text/javascript">
	// <!-- >
	function notify() {
		self.location.href = 'notify.jsp?hwNum=<%= hwNum %>&qNum=<%= qNum
				%>&userIds=<%= Utils.toValidJS(Utils.join(userIdsGradeChange)) %>';
	} // notify()
	// -->
	</script>

</head>
<body style="background-color:#e0e6c2; margin:0px; overflow:auto;">
	<form name="dummy" action="doRegrade.jsp" method="post">
	<table style="width:400px; background-color:#e0e6c2; 
			text-align:center; margin-left:auto; margin-right:auto;">
		<tr>
			<td class="boldtext enlarged" style="padding-left:10px;
					padding-top:10px;">
				<%= user.translate("Regrade Results") %>
			</td>
		</tr>
		<tr>
			<td class="regtext" style="padding-left:10px; 
					padding-right:10px; padding-top:10px;">
				<% if (gradesHaveChanged) { %>
					<%= user.translate("The regrade changed the grades "
							+ "of the following students.") %> 
					<br/><br/>
					<% for (final String userName : userNamesGradeChangeList) { %>
						<%= userName %><br/>
					<% } // for each student whose grade changed %>
					<br/>
				<% } else { %>
					<%= user.translate("The regrade changed no "
							+ "students' grades.") %>
				<% } // if any students' grade changed %>
			</td>
		</tr>
		<tr><td style="text-align:left; padding-left:10px; 
				padding-right:10px; padding-top:10px;">
		<table><tr>
			<% if (gradesHaveChanged) { %>
				<td style="padding-bottom:10px;">
					<%= makeButton(user.translate("Notify"),
							Utils.toString(Utils.isEmpty(userIdsGradeChange) ? "" 
								: "opener.location.reload(); ", "notify();")) %>
				</td>
			<% } // if there are changed grades %>
			<td style="padding-bottom:10px;">
				<%= makeButton(user.translate("Close"),
						Utils.toString(Utils.isEmpty(userIdsGradeChange) ? "" 
							: "opener.location.reload(); ", "self.close();")) %>
			</td>
		</tr></table></td></tr>
	</table>
	</form>
</body>
</html>
