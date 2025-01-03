<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.Institution,
	com.epoch.session.AnonSession,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");

	final int crsId = MathUtils.parseInt(request.getParameter("crsId"));
	final Institution[] institutions = AnonSession.getVerifiedInstitutions();
	int chosenInstnId = MathUtils.parseInt(request.getParameter("institutionId"));
	if (chosenInstnId == 0) chosenInstnId = user.getInstitutionId();

	User[] coinstructors;
	User[] instructors; 
	if (userSess instanceof AdminSession) {
		final AdminSession adminSess = (AdminSession) userSess;
		coinstructors = adminSess.getCoinstructors(crsId);
		instructors = adminSess.getNoncoinstructors(chosenInstnId, crsId);
	} else if (userSess instanceof InstructorSession) {
		final InstructorSession instrSess = (InstructorSession) userSess;
		coinstructors = instrSess.getCoinstructors(crsId);
		instructors = instrSess.getNoncoinstructors(chosenInstnId, crsId);
	} else {
		final StudentSession studSess = (StudentSession) userSess;
		coinstructors = studSess.getCoinstructors(crsId);
		instructors = studSess.getNoncoinstructors(chosenInstnId, crsId);
	}

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
	<title>ACE Coinstructors Management</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico"
		type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css"
		type="text/css"/>
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
		self.location.href = '<%= pathToRoot %>userHome.jsp';
	}
	
	function chooseInstitution() {
		var form = document.editform;
		self.location.href = 'addCoinstructor.jsp?crsId=<%= crsId %>&institutionId='
				+ form.institutionId.value;
	}
	
	function removeCoinstructor() {
		var form = document.editform;
		form.action = 'deleteCoinstructor.jsp';
		form.submit();
	}

	function addCoinstructor() {
		var form = document.editform;
		form.action = 'saveCoinstructor.jsp';
		form.submit();
	}
	// -->
	</script>
</head>
<body class="light" style="background-color:white;">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<div id="contentsWithoutTabs">

	<form name="editform" method="post" action="dummy">
		<input type="hidden" name="crsId" value="<%= crsId %>" />
	<table style="width:626px; text-align:center; margin-left:auto; margin-right:auto;">
	<tr>
	<td class="boldtext big" style="vertical-align:top; 
			padding-top:10px;">
		<%= user.translate("Add a New Coinstructor") %>
	</td>
	</tr>
	<tr>
	<td style="vertical-align:top; text-align:center;">
		<table class="whiteTable" style="width:626px; background-color:#f6f7ed;
				text-align:left;">
		<tr>
		<td class="regtext" style="padding-left:40px; padding-top:10px;
				padding-bottom:10px;">
			<%= user.translate("Institution of the coinstructor") %>:
		</td>
		<td style="padding-top:10px; padding-bottom:10px;">
			<select name="institutionId" onchange="chooseInstitution();">
			<% for (final Institution instn : institutions) { 
				final int instnId = instn.getId(); %>
				<option value="<%= instnId %>" <%= instnId == chosenInstnId
						? " selected=\"selected\"" : "" %>>
					<%= instn.getName() %> </option>
			<% } // for each institution %>
			</select>
		</td>
		</tr>
		<tr>
		<td class="regtext" style="padding-left:40px; padding-bottom:10px;">
			<%= user.translate("Name of the coinstructor") %>:
		</td>
		<td class="regtext" style="padding-bottom:10px;">
			<select name="instructorId">
			<% for (final User instructor : instructors) { 
				final String instructorId = instructor.getUserId();
				if (!instructorId.equals(user.getUserId())) { %>
					<option value="<%= instructorId %>">
						<%= instructor.getName().toString() %> </option>
				<% } // if the instructor is not the user %>
			<% } // for each potential coinstructor %>
			</select>
		</td>
		</tr>
		<tr>
		<td colspan="2" style="padding-left:40px; padding-bottom:10px;">
			<table><tr>
			<td>
				<%= makeButton(user.translate("Add"), "addCoinstructor();") %>
			</td>
			</tr></table>
		</td>
		</tr>
		</table>
	</td>
	</tr>
	</table>
	<% if (coinstructors != null && coinstructors.length > 0) { %>
	<table style="width:626px; text-align:center; margin-left:auto; margin-right:auto;">
	<tr>
	<td class="boldtext big" style="vertical-align:top; 
			padding-top:10px;">
		<%= user.translate("Remove a Current Coinstructor") %>
	</td>
	</tr>
	<tr>
	<td style="vertical-align:top; text-align:center;">
		<table class="whiteTable" style="width:626px; background-color:#f6f7ed;
				text-align:left;">
		<tr>
		<td class="regtext" style="padding-left:40px; padding-top:10px;">
			<%= user.translate("Current coinstructor" 
					+ (coinstructors.length != 1 ? "s" : "")) %>:
		</td>
		<td style="padding-top:10px;">
			<select name="coinstructor">
			<% for (final User coinstr : coinstructors) { %> 
				<option value="<%= coinstr.getUserId() %>">
					<%= coinstr.getName().toString() %> 
					(<%= coinstr.getInstitutionName() %>) </option>
			<% } %>
			</select>
		</td>
		</tr>
		<tr>
		<td colspan="2" style="padding-left:40px; padding-bottom:10px; padding-top:10px;">
			<table>
			<tr>
			<td>
				<%= makeButton(user.translate("Remove"), "removeCoinstructor();") %>
			</td>
			</tr>
			</table>
		</td>
		</tr>
		</table>
	</td>
	</tr>
	</table>
	<% } // if there are coinstructors %>	
	<table style="width:626px; margin-left:auto; margin-right:auto;">
	<tr>
	<td style="width:100%;">
		<table>
		<tr>
		<td style="padding-left:40px;">
			<%= makeButton(user.translate("Cancel"), "goBackAgain();") %>
		</td>
		</tr>
		</table>
	</td>
	</tr>
	</table>
	</form>
	</div>
</body>
</html>
