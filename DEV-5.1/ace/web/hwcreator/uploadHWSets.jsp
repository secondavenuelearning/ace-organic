<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	final String pathToRoot = "../";

	// import one or more assignments  
	if (!Utils.among(role, User.ADMINISTRATOR, User.INSTRUCTOR)) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	final String[][] templates = 
			(userSess instanceof AdminSession 
				? ((AdminSession) userSess).getAssgtsTemplates()
				: userSess instanceof InstructorSession
				? ((InstructorSession) userSess).getAssgtsTemplates()
				: new String[0][0]);
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
	<title>ACE Assignment Import</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
		* html body {
			padding:100px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/navigation/courseSidebarJS.jsp.h" %>

	function uploadIt(fromDb) {
		var form = document.fileupload;
		if (fromDb) {
			form.enctype = 'application/x-www-form-urlencoded';
			form.templateId.value = form.templateSelector.value;
		} else {
			if (isWhiteSpace(form.coursefile.value)) {
				toAlert('<%= user.translateJS(
						"Type in or browse to the file containing the "
						+ "exported assignments.") %>');
				return;
			}
			form.enctype = 'multipart/form-data';
		}
		form.fromDb.value = fromDb;
		form.submit();
	}
	
	// -->
	</script>
</head>
<body class="light" style="background-color:white;" onload="setTab('Assignments');">
	
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

<div id="contentsWithTabsWithoutFooter">
<form name="fileupload" action="importHWSets.jsp" method="post" enctype="">
	<input type="hidden" name="templateId" />
	<input type="hidden" name="fromDb" value="true" />
<table style="text-align:center; margin-left:auto; margin-right:auto;">
	<tr><td class="boldtext big" style="vertical-align:top; padding-top:10px;">
		<%= user.translate("Upload a List of Assignments for a Course") %>
	</td></tr>
	<tr><td style="vertical-align:top; text-align:center;">
	<table class="whiteTable" style="background-color:#f6f7ed;">
		<tr><td>
		<table>
			<tr><td class="regtext" style="padding-top:10px; 
				padding-left:30px; padding-right:30px;">
			<%= user.translate("Upload a .course file (text "
					+ "format, not zipped) that was exported "
					+ "previously.") %>
			</td></tr>
		</table>
		</td></tr>
		<tr><td>
		<table>
			<tr><td class="regtext" style="padding-top:10px; padding-left:30px;">
				<%= user.translate("File location") %>:
			</td>
			<td style="text-align:left; padding-top:10px;">
				<input type="file" NAME="coursefile" size="30" />
			</td></tr>
		</table>
		</td></tr>
		<tr><td>
		<table>
			<tr><td style="padding-bottom:10px; padding-top:10px; padding-left:30px;">
				<%= makeButton(user.translate("Upload"), "uploadIt(false);") %>
			</td>
			<td style="padding-bottom:10px; padding-top:10px;">
				<%= makeButton(user.translate("Cancel"),
						"document.location.href='hwSetList.jsp';") %>
			</td></tr>
		</table>
		</td></tr>
		<tr><td style="vertical-align:top;">
			<table class="whiteTable" 
					style="width:100%; text-align:left; border-collapse:collapse; 
						border-width:0px; background-color:#f6f7ed;">
				<tr><td style="padding-left:30px;"><%= user.translate(
						"Or import one of these assignment lists") %>:&nbsp;
				<select name="templateSelector" id="templateSelector">
					<option value="0"><%= user.translate(
							"Choose an assignments template") %></option>
					<% for (final String[] template : templates) { %>
						<option value="<%= template[0] %>">
							<%= template[1] %>
						</option>
					<% } // for each template %>
				</select>
				</td><td style="padding-left:20px;">
					<%= makeButton(user.translate("Submit"), "uploadIt(true);") %>
				</td></tr>
			</table>
		</td></tr>
		</table>
	</td></tr>
</table>
</form>
</div>
</body>
</html>

