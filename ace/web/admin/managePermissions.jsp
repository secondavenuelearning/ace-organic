<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../";

	final AdminSession admSess = (AdminSession) userSess;
	final boolean sortByInstnToo = 
			"true".equals(request.getParameter("sortByInstnToo"));
	admSess.resetAllUsers(sortByInstnToo);
	final User[] allInstructors = admSess.getAllUsers();

	admSess.getAllInstructorsLanguages();
	final String CHECKED = " checked=\"checked\"";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>Database Permissions Manager</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<style type="text/css">
		* html body {
			padding:55px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

	function savePermissions(instructorNum) {
		var url = 'setPermissions.jsp';
		var toSend = new String.builder()
				.append('instructorNum=')
				.append(instructorNum)
				.append('&allowMasterEdit=')
				.append(getChecked('masteredit' + instructorNum))
				.append('&allowTranslation=')
				.append(getChecked('translation' + instructorNum));
		callAJAX(url, toSend.toString());
	} // savePermissions()
	
	function updatePage() {
		if (xmlHttp.readyState === 4) { // ready to continue
			var response = xmlHttp.responseText;
			var instructorName = extractField(response, 'instructorName');
			alert(new String.builder()
					.append('Permissions saved for user ')
					.append(cerToUnicode(instructorName))
					.append('.')
					.toString());
		}
	} // updatePage()

	function reorder() {
		self.location.href = 'managePermissions.jsp?sortByInstnToo=<%= !sortByInstnToo %>';
	} // reorder()

	// -->
	</script>
</head>
<body class="regtext">

<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

<div id="contentsWithoutTabs">
<form name="usersList" action="dummy">
<table class="regtext" style="margin-left:auto; margin-right:auto; width:95%"> 
	<tr><td class="boldtext big" style="width:100%;">
		Database permissions manager
	</td><td style=""><%= makeButton("Sort by " + (sortByInstnToo 
			? "name only" : "institution, then name"), "reorder();") %>
	</td></tr>
	<tr><td style="padding-top:10px; padding-bottom:10px;">
		To exit, press <b>Admin Tool</b> above.
	</td></tr>
</table>

<table class="regtext" 
		style="border-collapse:collapse; margin-left:auto; margin-right:auto; width:95%;"> 
	<tr><th><u>User</u></th>
	<th style="padding-left:10px;"><u>Institution</u></th>
	<th style="padding-left:10px;"><u>Email</u></th>
	<th style="padding-left:10px;"><u>Language(s)</u></th>
	<th><u>Master author</u></th>
	<th><u>Translator</u></th></tr>
	<tr>
		<% 
		boolean rowToggle = false;
		for (int instructorNum = 1; instructorNum <= allInstructors.length; instructorNum++) {
			rowToggle = !rowToggle;
			final String rowColor = (rowToggle ? "whiterow" : "greenrow");
			final User instructor = allInstructors[instructorNum - 1];
			final String nameFamily1st = instructor.getName().toString();
			if (!"admin".equals(instructor.getUserId())) { 
				%>
				<tr class="<%= rowColor %>">
				<td><span id="name<%= instructorNum %>"><%= nameFamily1st %></span>
				</td>
				<td style="padding-left:10px;"><%= instructor.getInstitutionName() %>
				</td>
				<td style="padding-left:10px;"><%= instructor.getEmail() %>
				</td>
				<td style="padding-left:10px;">
					<% final String[] languages = instructor.getLanguages();
					final String userLangs = (Utils.isEmpty(languages)
							? "" : Utils.join(languages));
					%>
					<%= userLangs %>
				</td>
				<td style="text-align:center;">
					<input type="checkbox" id="masteredit<%= instructorNum %>"
						<%= instructor.isMasterAuthor() ? CHECKED : "" %> />
				</td>
				<td style="text-align:center;">
					<input type="checkbox" id="translation<%= instructorNum %>"
						<%= instructor.isTranslator() ? CHECKED : "" %> />
				</td>
				<td>
					<%= makeButton("Save changes", "savePermissions(" + instructorNum + ");") %>
				</td>
				</tr>
			<% } // if user is not admin
		} // for each user %>
	</tr> 
</table>
<hr>
<br/>
</form>
</div>

</body>
</html>
