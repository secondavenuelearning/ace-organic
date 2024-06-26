<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.Course"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%	final String pathToRoot = "../";

	Course[] courses = new Course[0];
	switch (role) {
		case User.ADMINISTRATOR:
			// either impersonating someone, or administrator in Pearson
			// version, who also owns & needs access to the tutorial course
			final AdminSession adminSess = (AdminSession) userSess;
			courses = adminSess.getHiddenCourses();
			break;
		case User.INSTRUCTOR:
			final InstructorSession instrSess = (InstructorSession) userSess;
			courses = instrSess.getHiddenCourses();
			break;
		default: ; // shouldn't happen
	} // switch role 

%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Course Management</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<style type="text/css">
		* html body {
			padding:50px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	
	function goBackAgain() {
		self.location.href='<%= pathToRoot %>userHome.jsp';
	}

	function removeSelected() {
		var crsIds = getSelectedValues(document.editform.course_checker);
		if (crsIds.length > 0) {
			if (toConfirm('<%= user.translateJS("If you delete these courses, "
					+ "you will irretrievably delete all assignments and "
					+ "grades associated with them as well. Do you "
					+ "still wish to continue?") %>')) {
				submitIt('remove', crsIds);
			} // if confirm delete
		} else {
			goBackAgain();
		} // if courses have been selected
	} // removeSelected()
	
	function reveal() {
		var crsIds = getSelectedValues(document.editform.course_checker);
		if (crsIds.length > 0) {
			submitIt('reveal', crsIds);
		} else {
			goBackAgain();
		} // if courses have been selected
	} // reveal()

	function submitIt(task, crsIds) {
		var form = document.editform;
		form.task.value = task;
		form.crsIds.value = crsIds.join(':');
		form.submit();
	} // submitIt()

	// -->

	</script>
</head>
<body class="light" style="background-color:white;">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<div id="contentsWithoutTabs">

	<table style="width:626px; text-align:center; margin-left:auto; margin-right:auto;">
		<tr>
			<td class="boldtext big" style="vertical-align:top;
			padding-top:10px;">
				<%= user.translate("Reveal or delete hidden courses") %>
			</td>
		</tr>
		<tr>
			<td class="regtext" style="vertical-align:top; padding-bottom:10px;
					padding-top:10px;">
				<%= user.translate("Select the courses you would like to "
						+ "see again or delete.") %>
			</td>
		</tr>
		<tr>
			<td style="vertical-align:top; text-align:center;">
				<form name="editform" action="saveRevealed.jsp" method="post">
					<input type="hidden" name="crsIds" value="" />
					<input type="hidden" name="task" value="" />
				<table class="regtext" style="width:90%; margin-left:auto; 
						margin-right:auto; border-style:none; border-collapse:collapse;">
				<tr>
				<td class="boldtext" style="border-bottom-style:solid; border-width:1px; 
						width:60%; border-color:#49521B; padding-left:10px; font-size:14px;">
					<%= user.translate("Course") %>
				</td>
				<td class="boldtext" style="border-bottom-style:solid; border-width:1px; 
						border-color:#49521B; text-align:center; font-size:14px;">
					<%= user.translate("Course ID") %>
				</td>
				<td class="boldtext" style="border-bottom-style:solid; border-width:1px; 
						border-color:#49521B; text-align:center; font-size:14px;">
					&nbsp;
				</td>
				</tr>
				<% boolean parity = false;
				for (int crsNum = 0; crsNum < courses.length; crsNum++) {
					final Course course = courses[crsNum];
					if (!course.hide()) continue;
					final String rowColor = (parity ? "greenrow" : "whiterow");
					parity = !parity;
				%>
				<tr class="<%= rowColor %>">
				<td style="border-left-style:solid; border-width:1px; 
						border-color:#49521B; padding-left:10px;">
					<%= course.getName() %>
				</td>
				<td style="text-align:center;">
					<%= course.getId() %>
				</td>
				<td style="border-right-style:solid; border-width:1px; 
						border-color:#49521B; text-align:center;">
					<input type="checkbox" name="course_checker" value="<%= course.getId() %>" />
				</td>
				</tr>
				<% } // for each course %>
				<tr>
					<td colspan="5" style="border-top-style:solid; border-width:1px; 
					border-color:#49521B; width:100%; color:#FF0000;
					padding-top:10px;">
				<table style="width:100%;">
				<tr><td style="width:100%;">
					&nbsp; 
				</td>
				<td>
					<%= makeButton(user.translate("Reveal selected"), "reveal();") %>
				</td>
				<td>
					<%= makeButton(user.translate("Delete selected"), "removeSelected();") %>
				</td>
				<td>
					<%= makeButton(user.translate("Cancel"), "goBackAgain();") %>
				</td>
				</table>
				</form>
			</td>
		</tr>
		<tr>
		</tr>
	</table>

	</div>
</body>
</html>
