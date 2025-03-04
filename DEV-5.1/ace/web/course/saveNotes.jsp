<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.utils.Utils"
%>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	request.setCharacterEncoding("UTF-8");
	final String notes = Utils.inputToCERs(request.getParameter("notes"));
	final boolean notify = "true".equals(request.getParameter("notify"));
	(role == User.ADMINISTRATOR ?
			(AdminSession) userSess : (InstructorSession) userSess
			).setCourseNotes(notes, notify);
%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<script type="text/javascript">
	// <!-- >
	var courseNotes = window.opener.document.getElementById('courseNotes');
	<% if (Utils.isWhitespace(notes)) { %>
		courseNotes.innerHTML = '';
		courseNotes.style.width = '0px;';
		courseNotes.colSpan = '1';
	<% } else { %>
		courseNotes.innerHTML = 
				'<table style="width:100%; border:1px solid #49521B; background-color:#FFFFFF">'
				+ '<tr><td class="regtext" style="vertical-align:top; padding-top:5px; '
				+ 'padding-left:5px; padding-right:5px; padding-bottom:5px;">'
				+ '<%= Utils.toValidJS(Utils.toDisplay(notes)) %>'
				+ '</td></tr></table>';
		courseNotes.style.width = '';
		courseNotes.colSpan = '2';
	<% } %>
	self.close();
	// -->
</script>
</html>


