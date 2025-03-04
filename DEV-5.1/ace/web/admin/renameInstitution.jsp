<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.Institution,
	com.epoch.db.dbConstants.UserRWConstants,
	com.epoch.db.dbConstants.CourseRWConstants,
	com.epoch.db.UserRead,
	com.epoch.session.AnonSession,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.HashMap,
	java.util.Map"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%	
	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");
	if (role != User.ADMINISTRATOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	final Institution[] institutions = AnonSession.getAllInstitutions();
	final Map<Integer, String> instnNamesByIds = new HashMap<Integer, String>();
	for (final Institution institution : institutions) {
		instnNamesByIds.put(Integer.valueOf(institution.getId()), institution.getName());
	} // for each institution
	final String newName = request.getParameter("newName");
	final String oldName = request.getParameter("oldName");
	final int institutionId = 
			MathUtils.parseInt(request.getParameter("institutionId"));
	final AdminSession admSess = (AdminSession) userSess;
	admSess.resetActedRole(); // resets to administrator
	admSess.resetAllUsers(UserRWConstants.INSTN_TOO);
	/* Utils.alwaysPrint("admin/renameInstitution.jsp: institutionId = ",
			institutionId, ", oldName = ", oldName,
			", newName = ", newName); /**/
	boolean resetDone = false;
	if (!Utils.isEmpty(newName)) {
		admSess.setInstitutionName(institutionId, newName);
		resetDone = true;
	} // if there's a name
	final String onLoadStr = (resetDone ? "goBackAgain();" : "setSelector();");

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
<title>ACE Rename institution</title>
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<style type="text/css">
	* html body {
		padding:100px 0 50px 0; 
	}
</style>

<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

	function goBackAgain() {
		alert('The name has been changed.');
		self.location.href = 'listProfiles.jsp';
	}

	var instnIdsArr = [''];
	var instnNamesArr = [''];
	<% for (final Institution institution : institutions) { %>
		instnIdsArr.push('<%= institution.getId() %>');
		instnNamesArr.push('<%= Utils.toValidJS(institution.getName()) %>');
	<% } // for each institution %>

	function storeOldName() {
		var selForm = document.instnSelForm;
		var selectedIndex = selForm.institutionId.selectedIndex;
		selForm.oldName.value = instnNamesArr[selectedIndex];
		selForm.newName.value = selForm.oldName.value;
	} // storeOldName()

	function setSelector() {
		var instnSelBld = new String.builder();
		instnSelBld.append('<select name="institutionId" id="institutionId"'
				+ ' onchange="storeOldName();">');
		for (var instnNum = 0; instnNum < instnIdsArr.length; instnNum++) {
			instnSelBld.append('<option value="')
					.append(instnIdsArr[instnNum])
					.append('">')
					.append(instnNamesArr[instnNum])
					.append('<\/option>');
		} // for each institution
		instnSelBld.append('<\/select>');
		setInnerHTML('institutionCell', instnSelBld.toString());
	} // setSelector()

	function submitIt() {
		var selForm = document.instnSelForm;
		var selectedIndex = selForm.institutionId.selectedIndex;
		var oldName = selForm.oldName.value;
		var newName = selForm.newName.value;
		if (confirm(new String.builder()
				.append('Are you sure you want to modify the name of ')
				.append(oldName)
				.append(' (ID = ')
				.append(instnIdsArr[selectedIndex])
				.append(') to ')
				.append(newName)
				.append('?')
				.toString())) {
			selForm.submit();
		} // if confirm
	} // submitIt()

	// -->
</script>
</head>

<body style="text-align:center;" onload="<%= onLoadStr %>">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>

	<div id="contentsWithTabsWithoutFooter">
	<table class="regtext" style="width:95%; margin-left:10px; margin-right:auto; 
			border-style:none; border-collapse:collapse;">
		<tr><td class="regtext" style="padding-bottom:10px; padding-top:10px;">
			<form name="instnSelForm" action="renameInstitution.jsp" method="post">
			<input type="hidden" name="oldName" id="oldName" />
			Modify the name of&nbsp;&nbsp;
			<span id="institutionCell"></span>&nbsp;&nbsp;to:&nbsp;&nbsp;
			<input type="text" name="newName" id="newName" value="" size="50" />
			</form>
		</td></tr>
		<tr><td style="text-align:left; padding-top:10px; padding-left:20px;">
			<table><tr>
			<td><%= makeButton("Modify", "submitIt();") %></td>
			<td style="text-align:right; padding-left:10px;">
			<%= makeButton("Cancel", "goBackAgain();") %></td>
			</tr></table>
		</td></tr>
	 </table>
	 </div>
</body>
</html>
