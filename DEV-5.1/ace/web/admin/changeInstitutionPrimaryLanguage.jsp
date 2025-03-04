<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.constants.AppConstants,
	com.epoch.courseware.Institution,
	com.epoch.db.dbConstants.UserRWConstants,
	com.epoch.db.dbConstants.CourseRWConstants,
	com.epoch.db.UserRead,
	com.epoch.session.AdminSession,
	com.epoch.session.AnonSession,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.List"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%	
	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");
	if (role != User.ADMINISTRATOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}

	final String ENGLISH = AppConstants.ENGLISH;
	boolean resetDone = false;
	final int instnId = MathUtils.parseInt(request.getParameter("institutionId"));
	final String newLanguage = request.getParameter("newLang");
	if (instnId > 0 && !Utils.isEmpty(newLanguage)) {
		((AdminSession) userSess).setInstitutionPrimaryLanguage(instnId, 
				ENGLISH.equals(newLanguage) ? null : newLanguage);
		resetDone = true;
	} // if there's data
	final String onLoadStr = (resetDone ? "goBackAgain(true);" 
			: "setArrs(); setInstnSelector(); setLangSelector();");

	final Institution[] institutions = (resetDone 
			? new Institution[0] : AnonSession.getAllInstitutions());
	final String[] allLangs = (resetDone 
			? new String[0] : AnonSession.getAllLanguages());

	/*/ final List<String[]> instnNamesLangs = new ArrayList<String[]>();
	for (Institution institution : institutions) {
		final String lang = institution.getPrimaryLanguage();
		instnNamesLangs.add(new String[] {institution.getName(), 
				lang == null ? ENGLISH : lang});
	} 
	System.out.println("changeInstitutionPrimaryLanguage.jsp: newLanguage = "
			+ newLanguage + ", instnId = " + instnId + ", resetDone = " + resetDone
			+ ", onLoadStr = " + onLoadStr); // + ", instnNamesLangs = ",
			instnNamesLangs + ", allLangs = " + allLangs); /**/ 
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
<title>ACE Change institution's primary language of instruction</title>
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

	function goBackAgain(showAlert) {
		if (showAlert) {
			alert(new String.builder()
					.append('You have changed the primary language of instruction of ')
					.append(cerToUnicode('<%= Utils.toValidJS(
						request.getParameter("chosenInstitution")) %>'))
					.append(' from ')
					.append(cerToUnicode('<%= Utils.toValidJS(
						request.getParameter("oldLang")) %>'))
					.append(' to ')
					.append(cerToUnicode('<%= Utils.toValidJS(newLanguage) %>'))
					.append('.')
					.toString());
		}
		self.location.href = 'listProfiles.jsp';
	}

	var instnIdsArr = [0];
	var instnNamesArr = [''];
	var instnLangsArr = ['<%= ENGLISH %>'];
	var allLangsArr = ['<%= ENGLISH %>'];

	function setArrs() {
		<% for (final Institution institution : institutions) { 
			final String lang = Utils.toValidJS(institution.getPrimaryLanguage()); %>
			instnIdsArr.push(<%= institution.getId() %>);
			instnNamesArr.push('<%= Utils.toValidJS(institution.getName()) %>');
			instnLangsArr.push('<%= Utils.isEmpty(lang) ? ENGLISH : Utils.toValidJS(lang) %>');
		<% } // for each institution %>
		<% for (final String language : allLangs) { %>
			allLangsArr.push('<%= language %>');
		<% } // for each language %>
	} // setArrs()

	function setOldLanguage() {
		var selForm = document.selectionForm;
		var chosenInstnId = parseToInt(selForm.institutionId.value);
		var instnNum = instnIdsArr.indexOf(chosenInstnId);
		setInnerHTML('currentLanguageCell', '&nbsp;&nbsp;from ' + 
				cerToUnicode(instnLangsArr[instnNum]));
	} // setOldLanguage()

	function setInstnSelector() {
		var instnSelBld = new String.builder();
		instnSelBld.append('<select name="institutionId" id="institutionId" '
				+ 'onchange="setOldLanguage();">');
		for (var instnNum = 0; instnNum < instnIdsArr.length; instnNum++) { //<!>
			instnSelBld.append('<option value="')
					.append(instnIdsArr[instnNum])
					.append('">')
					.append(instnNamesArr[instnNum])
					.append('<\/option>');
		} // for each institution
		instnSelBld.append('<\/select>');
		setInnerHTML('institutionCell', instnSelBld.toString());
	} // setInstnSelector()
	
	function setLangSelector() {
		var langSelBld = new String.builder();
		langSelBld.append('<select name="newLangSelector" id="newLangSelector">');
		for (var langNum = 0; langNum < allLangsArr.length; langNum++) { //<!>
			langSelBld.append('<option value="')
					.append(unicodeToCER(allLangsArr[langNum]))
					.append('">')
					.append(allLangsArr[langNum])
					.append('<\/option>');
		} // for each language 
		langSelBld.append('<\/select>');
		setInnerHTML('newLanguageCell', langSelBld.toString());
	} // setLangSelector()

	function submitIt() {
		var selForm = document.selectionForm;
		var instnId = parseToInt(selForm.institutionId.value);
		var instnNum = instnIdsArr.indexOf(instnId);
		selForm.newLang.value = unicodeToCER(selForm.newLangSelector.value);
		selForm.oldLang.value = instnLangsArr[instnNum];
		selForm.chosenInstitution.value = instnNamesArr[instnNum];
		var newLanguage = selForm.newLang.value;
		/*/ alert('instnLangsArr has ' + instnLangsArr.length + ' members '
				+ 'of which instnLangsArr[' + instnNum + '] = '
				+ instnLangsArr[instnNum]
				+ '; new language = ' + selForm.newLangSelector.value
				+ ', unicodeToCER = ' + newLanguage); /**/
		var bld = new String.builder()
				.append('Change default language of ')
				.append(cerToUnicode(instnNamesArr[instnNum]))
				.append(' from ')
				.append(cerToUnicode(instnLangsArr[instnNum]))
				.append(' to ')
				.append(cerToUnicode(newLanguage))
				.append('?');
		if (confirm(bld.toString())) {
			selForm.submit();
		}
	} // submitIt()

	// -->
</script>
</head>

<body style="text-align:center;" onload="<%= onLoadStr %>">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>

	<div id="contentsWithTabsWithoutFooter">
	<table class="regtext" style="font-size:10pt; width:95%; margin-left:10px; margin-right:auto; 
			border-style:none; border-collapse:collapse;">
		<tr><td style="padding-bottom:10px; padding-top:10px;">
			<form name="selectionForm" action="changeInstitutionPrimaryLanguage.jsp" method="post">
			Change the primary language of instruction of&nbsp;&nbsp;
			<span id="institutionCell"></span>
			<span id="currentLanguageCell"></span>
			&nbsp;&nbsp;to&nbsp;&nbsp;
			<span id="newLanguageCell"></span>
			<input type="hidden" id="oldLang" name="oldLang" value="" />
			<input type="hidden" id="newLang" name="newLang" value="" />
			<input type="hidden" id="chosenInstitution" name="chosenInstitution" value="" />
			</form>
		</td></tr>
		<tr><td style="text-align:left; padding-top:10px; padding-left:20px;">
			<table><tr>
			<td><%= makeButton("Change", "submitIt();") %></td>
			<td style="text-align:right; padding-left:10px;">
			<%= makeButton("Cancel", "goBackAgain(false);") %></td>
			</tr></table>
		</td></tr>
	 </table>
	 </div>
</body>
</html>
