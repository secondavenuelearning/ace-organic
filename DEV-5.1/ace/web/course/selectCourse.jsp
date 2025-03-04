<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.utils.MathUtils"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final int index = MathUtils.parseInt(request.getParameter("index"));
	final int result = (role == User.ADMINISTRATOR ? (AdminSession) userSess
				: role == User.INSTRUCTOR ? (InstructorSession) userSess
				: (StudentSession) userSess
			).selectCourse(index);
	if (result == -1) {
		%><jsp:forward page="/course/courseDisabled.html" /><%	
	}
%>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%	final boolean needPwd = course.hasPassword() && role == User.STUDENT && !isTA;
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
	<title>ACE Select Course</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<% if (needPwd) { %>
		<script src="<%= pathToRoot %>js/md5.js" type="text/javascript"></script>
		<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
		<script type="text/javascript">
		// <!-- >
		<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
		function checkSubmit(e) {
			if (e && [10, 13].contains(e.keyCode)) {
				document.enterCourseForm.submit();
			}
		} // checkSubmit()
		// -->
		</script>

	<% } // if need password %>
</head>
<body class="light" style="background-color:white;">
	<% if (needPwd) { %>
		<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
		<div id="contentsWithoutTabs">
		<form name="enterCourseForm" id="enterCourseForm" method="post"
				action="enterCourseVerify.jsp">
			<table style="margin-left:auto; margin-right:auto;" summary="">
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<table>
					<tr>
					<td>
						<%= user.getName().toString() %>
					</tr>
					</td>
					<tr>
					<td>
						<%= user.getInstitutionStudentNumLabel() %>:
					</td>
					<td>
						<%= user.getStudentNum() %>
					</td>
					</tr>
					<tr>
					<td>
						<%= user.translate("Username") %>:
					</td>
					<td>
						<%= user.getUserId() %>
					</td>
					</tr>
					</table>
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<%= user.translate("Enter the course password for course ***1024***",
							course.getId()) %>, <b><%= course.getName() %></b>:
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<input type="password" name="pphraseEntry"
						onKeyPress="checkSubmit(event);"
						value="" size="16" />
				</td>
				</tr>
				<tr>
				<td>
					<%= makeButton(user.translate("Enter course"), 
							"document.getElementById('enterCourseForm').submit();") %>
				</td>
				</tr>
			</table>
		</form>
		</div>
	<% } else { %>
		<script type="text/javascript">
			this.location.href = '<%= pathToRoot %>course/courseHome.jsp';
		</script>
	<% } // if need password %>
</body>
</html>

