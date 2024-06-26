<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%	
	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");
	if (role != User.ADMINISTRATOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	
	final AdminSession admSess = (AdminSession) userSess;
	boolean actionDone = "true".equals(request.getParameter("actionDone"));
	final String task = request.getParameter("taskSelector");
	if (!Utils.isEmpty(task)) {
		final int templateId = MathUtils.parseInt(request.getParameter("templateId"));
		final String templateName = request.getParameter("templateName");
		final String templateXML = request.getParameter("contentsBox");
		/*/ Utils.alwaysPrint("manageAssgtTemplates.jsp: task = ", task,
				", templateId = ", templateId, 
				", templateName = ", templateName,
				", templateXML length = ", Utils.getLength(templateXML)); /**/ 
		if ("add".equals(task)) {
			admSess.manageAssgtsTemplate(templateName, templateXML);
		} else if ("edit".equals(task)) {
			admSess.manageAssgtsTemplate(templateId, templateName, templateXML);
		} else if ("delete".equals(task)) {
			admSess.manageAssgtsTemplate(templateId);
		} // if task
		actionDone = true;
	} // if task isn't empty

	final String[][] templates = admSess.getAssgtsTemplates();
	final String onLoadStr = (actionDone ? "backToProfiles(true);" : "");
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<meta http-equiv="Pragma" content="no-cache"/>
<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<title>ACE Manage assignments templates</title>
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

	var templateIdsArr = [];
	var templateNamesArr = [];
	var templateXMLsArr = [];
	<% for (final String[] template : templates) { %>
		templateIdsArr.push('<%= Utils.toValidJS(template[0]) %>');
		templateNamesArr.push('<%= Utils.toValidJS(template[1]) %>');
		templateXMLsArr.push('<%= Utils.toValidJS(template[2]) %>');
	<% } // for each template %>

	function setForm() {
		var tForm = document.templateForm;
		var task = tForm.taskSelector.value;
		if (task === 'add' || task === 'edit' || task === 'delete') {
			var templateName = '', words1 = '',
					nameBld = new String.builder();
			if (task !== 'add') {
				nameBld.append('<p><select name="nameSelector" '
							+ 'onchange="fillTextboxes();">'
							+ '<option value="0">Choose an '
							+ 'assignments template...<\/option>');
				<% for (int tmpltNum = 0; tmpltNum < templates.length; tmpltNum++) { %>
					nameBld.append('<option value="<%= tmpltNum + 1 %>">')
							.append(templateNamesArr[<%= tmpltNum %>])
							.append('</option>');
				<% } // for each template %>
				nameBld.append('<\/select>');
				if (task === 'delete') hideCell('templateContentsCell');
				words1 = 'Modify';
			} // if adding
			if (task === 'add' || task === 'edit') {
				nameBld.append('<p><span id="textAndBoxName">');
				nameBld.append(task === 'add'
						? 'Name of the new assignments template: '
						: 'Edit the template name here: ');
				nameBld.append('<input type="text" name="nameBox" '
						+ 'id="nameBox" value="" size="50" \/><\/span>');
				showCell('templateContentsCell');
				if (task === 'add') words1 = 'Enter';
			} else {
				hideCell('templateContentsCell');
			}
			setInnerHTML('templateNameCell', nameBld.toString());
			setInnerHTML('words1', words1);
			if (task === 'edit') hideCell('textAndBoxName');
			else showCell('textAndBoxName');
		} else { // no task selected
			hideCell('templateNameCell');
			hideCell('templateContentsCell');
		} // if task
	} // setForm()

	function fillTextboxes() {
		var name = '', xml = '';
		var tForm = document.templateForm;
		var task = tForm.taskSelector.value;
		var templateNum = parseToInt(tForm.nameSelector.value);
		if (templateNum > 0) {
			name = templateNamesArr[templateNum - 1];
			xml = templateXMLsArr[templateNum - 1];
		}
		setValue('nameBox', name);
		setValue('contentsBox', xml);
		showCell('templateNameCell');
		if (task === 'edit' && templateNum === 0) hideCell('textAndBoxName');
		else showCell('textAndBoxName');
		if (task !== 'delete') {
			showCell('templateContentsCell');
		}
	} // fillTextboxes()

	function backToProfiles(isSaved) {
		if (isSaved) alert('The template has been saved.');
		self.location.href = 'listProfiles.jsp';
	}

	function submitIt() {
		var tForm = document.templateForm;
		var task = tForm.taskSelector.value;
		var templateNum = (task === 'add'
				? 0 : parseToInt(tForm.nameSelector.value));
		if ((task === 'edit' || task === 'delete') && templateNum === 0) {
			alert('Choose a template to ' + task + '.');
			return;
		}
		tForm.templateId.value = (task === 'add'
				? 0 : templateIdsArr[templateNum - 1]);
		tForm.templateName.value = (task !== 'delete'
				? tForm.nameBox.value : templateNamesArr[templateNum - 1]);
		tForm.submit();
	} // submitIt()

	// -->
</script>
</head>

<body style="text-align:center;" onload="<%= onLoadStr %>">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>

	<div id="contentsWithoutTabs">
	<form name="templateForm" action="manageAssgtTemplates.jsp" method="post">
	<table class="regtext" style="width:95%; margin-left:10px; margin-right:auto; 
			border-style:none; border-collapse:collapse;">
		<tr><td class="boldtext big" style="padding-top:10px; width:100%;">
		Manage course assignments templates
		</td></tr>
		<tr><td id="taskSelectorCell" class="regtext" 
				style="padding-bottom:10px; padding-top:20px;">
			<input type="hidden" id="templateId" name="templateId" value="0" />
			<input type="hidden" id="templateName" name="templateName" value="" />
			<select name="taskSelector" onchange="setForm();">
				<option value="">Choose an option...</option>
				<option value="add">Add a new template</option>
				<% if (!Utils.isEmpty(templates)) { %>
					<option value="edit">Edit an existing template</option>
					<option value="delete">Delete an existing template</option>
				<% } // if there are templates %>
			</select>
		</td></tr>
		<tr><td id="templateNameCell" class="regtext" 
				style="padding-bottom:10px; padding-top:10px;">
		</td></tr>
		<tr><td id="templateContentsCell" class="regtext" 
				style="padding-bottom:10px; padding-top:10px; visibility:hidden;">
			<span id="words1">Enter</span> the assignments XML:<p>
			<textarea id="contentsBox" name="contentsBox" 
					cols="65" rows="15" style="overflow:auto;"></textarea>
		</td></tr>
		<tr><td style="text-align:left; padding-top:10px; padding-left:20px;">
			<table><tr>
			<td><%= makeButton("Submit", "submitIt();") %></td>
			<td style="text-align:right; padding-left:10px;">
			<%= makeButton("Cancel", "backToProfiles(false);") %></td>
			</tr></table>
		</td></tr>
	</table>
	</form>
	</div>
</body>
</html>
