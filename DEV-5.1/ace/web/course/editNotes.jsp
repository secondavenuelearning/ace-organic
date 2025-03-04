<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ page import="
	com.epoch.courseware.Course,
	com.epoch.utils.Utils"
%>

<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	final String pathToRoot = "../";
	final Course course = (role == User.ADMINISTRATOR ?
			(AdminSession) userSess : (InstructorSession) userSess
			).getSelectedCourse();
	final String notes = course.getNotes();
	final boolean isTutorialCourse = course.getId() == AppConfig.tutorialId;
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
 	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<title><%= isTutorialCourse ? "System notice" : "Course notes" %></title>
	<script type="text/javascript">
		// <!-- >
		function submitIt(notify) {
			var form = document.getElementById('notesform');
			form.notify.value = notify;
			form.submit();
		}

		function cancel() {
			self.close();
		}
		// -->
	</script>
</head>
<body style="background-color:#f6f7ed;">
	<form name="notesform" action="saveNotes.jsp" id="notesform" method="post"
			accept-charset="UTF-8">
	<input type="hidden" name="notify" value="" />
	<table class="regtext" style="width:95%; margin-top:10px; margin-left:auto;
			margin-right:auto;">
	<tr><td class="boldtext big">
		<%= user.translate(isTutorialCourse ? "System notice" : "Course notes") %>
	</td></tr>
	<tr><td class="regtext" style="font-style:italic;">
		<%= user.translate("(Enter or paste the notes here. "
				+ "You can use the HTML tags: "
				+ "&lt;b>, &lt;u>, &lt;i>, &lt;br>, and &lt;p> for "
				+ "formatting. Hyperlinks (&lt;a href=\"http://...\">&lt;a>) "
				+ "are also allowed, as are character entity "
				+ "references such as ***&amp;deg;*** for a ***&deg;*** "
				+ "sign.  Please do not use any other HTML tags.)",
		new String[] {"&amp;deg;", "&deg;"}).replaceAll("<", "&lt;") %>
	</td></tr>
	<tr><td align="center">
		<textarea id="notes" name="notes" rows="20" cols="66"><%= 
				Utils.toValidTextbox(notes) %></textarea>
	</td></tr>
	<tr><td>
		<table style="text-align:center; margin-left:auto; margin-right:auto;">
			<tr><td>
				<%= makeButton(user.translate("Save"), "submitIt(false);") %>
			</td><td>
				<%= makeButton(user.translate("Save and notify all"), "submitIt(true);") %>
			</td><td>
				<%= makeButton(user.translate("Cancel"), "cancel();") %>
			</td></tr>
		</table>
	</td></tr>
	</table>
	</form>
</body>
</html>
