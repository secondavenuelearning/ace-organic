<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.session.AnonSession,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.Arrays,
	java.util.List"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%	final String pathToRoot = "../"; 
	request.setCharacterEncoding("UTF-8");

	/* parameters 
			editindex - number if the user is edited by the admin
						 0  if the user is added by the admin
						null, if an ordinary user is editing his profile
	*/

	User editUser = user;
	// boolean adminEditMode = false;
	final String goBack = request.getParameter("goBack");
	String extraGoBack = "";
	final String editIndex = request.getParameter("editindex");
	if (editIndex != null) {
		extraGoBack = "?editindex=" + editIndex;
		// adminEditMode = true;
		final int index = MathUtils.parseInt(editIndex);
		if (index == 0) {
			if (goBack != null) {
				extraGoBack += "&goBack=" + Utils.toValidURI(goBack);
			}
			%><jsp:forward page="editProfile.jsp<%= extraGoBack %>" /><%
		} else {
			editUser = ((AdminSession) userSess).getUser(index);
		} // if index
	} // if editIndex is not null

	final String[] allLanguages = AnonSession.getAllLanguages();
	editUser.refreshLanguages();
	String[] editUserLangs = editUser.getLanguages();
	if (editUserLangs == null) editUserLangs = new String[0];
	final int numLangs = Utils.getLength(editUserLangs);
	final List<String> unchosenLangsList = 
			new ArrayList<String>(Arrays.asList(allLanguages));
	for (final String editUserLang : editUserLangs) {
		unchosenLangsList.remove(editUserLang);
	}
	final String[] unchosenLangs =
			unchosenLangsList.toArray(new String[unchosenLangsList.size()]);
	final String instnPrimaryLanguage = 
			editUser.getInstitution().getPrimaryLanguage();
	/*/ Utils.alwaysPrint("editLanguages.jsp: institution ",
			editUser.getInstitution().getName(), " has instnPrimaryLanguage = ",
			instnPrimaryLanguage, "; editUserLangs = ", editUserLangs); /**/

	final int OTHER = -2;
	final int NONE = -1;

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
	<title>ACE Language Management</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<style type="text/css">
		* html body {
			padding:50px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/md5.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	
	function goBackAgain() {
		var addr = 'editProfile.jsp?goBack='
				+ encodeURIComponent('<%= goBack %>');
		addr = addr.replaceAll('%3F', '&');
		if (<%= editIndex != null %>) { 
			addr += '&editindex=<%= editIndex %>';
		}
		this.location.href = addr;
	}

	function changeLanguage(form) {
		if (form.languageSelector.value === '<%= OTHER %>') {
			setValue('other_language', '');
			showCell('lang1');
			showCell('lang2');
		} else {
			hideCell('lang1');
			hideCell('lang2');
		}
	}

	function submitIt(form, removePosn) {
		if (form.languageSelector.value === '<%= OTHER %>') {
			if (isWhiteSpace(form.other_language.value)) {
				alert('If you choose [Other] from the menu, you must enter a language.');
				return;
			}
			form.newLanguage.value = trimWhiteSpaces(form.other_language.value);
		} else if (form.languageSelector.value !== '<%= NONE %>') {
			form.newLanguage.value = form.languageSelector.value;
			// alert('New language = ' + form.newLanguage.value);
		}
		var newOrder = ':';
		var origOrder = ':';
	 	for (var langNum = 0; langNum < <%= numLangs %>; langNum++) { // <!-- >
			var newRank = getValue('langRank' + langNum);
			if (newOrder.indexOf(':' + newRank + ':') >= 0) {
				alert('No two languages may have the same rank.');
				return;
			}
			newOrder += newRank + ':';
			origOrder += (langNum + 1) + ':';
	 	} // for each language 
		if (newOrder.length > 1 && newOrder !== origOrder) {
			if (removePosn !== 0) {
				alert('ACE cannot reorder the languages at the same time as it '
						+ 'removes one.  ACE will remove the selected language '
						+ 'without reordering the remaining ones.');
			} else {
				form.newOrder.value = newOrder.substring(1);
			}
		}
		if (removePosn !== 0) {
			form.removeLangPosn.value = removePosn;
		}
		form.submit();
	}

	// -->

	</script>
</head>
<body class="light" style="background-color:white;">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<div id="contentsWithoutTabs">

	<table style="width:626px; text-align:center; margin-left:auto; 
			margin-right:auto;">
	<tr>
		<td class="boldtext big" style="vertical-align:top; padding-top:10px;">
			<%= user.translate("Edit languages") %>
		</td>
	</tr>
	<tr>
		<td style="vertical-align:top; text-align:center;">
			<form name="editform" action="saveLanguages.jsp" method="post" 
					 accept-charset="UTF-8"> 
				<input type="hidden" name="newLanguage" value=""/>
				<input type="hidden" name="newOrder" value=""/>
				<input type="hidden" name="removeLangPosn" value=""/>
				<input type="hidden" name="goBack" 
						value="<%= Utils.toValidHTMLAttributeValue(goBack) %>"/>
				<% if (editIndex != null) { %>
					 <input type="hidden" name="editindex" value="<%= editIndex %>"/>
				<% } %>
			<table class="whiteTable" style="width:626px; background-color:#f6f7ed;
					text-align:left;">
				<tr>
				<% if (numLangs == 0) { %>
					<td class="regtext" style="padding-left:40px; padding-top:20px; 
							vertical-align:top; width:40%;">
						Your only language is English.
					</td>
				<% } else { %>
					<td class="regtext" style="padding-left:40px; padding-top:20px; 
							vertical-align:top; width:40%;">
						<%= user.translate(Utils.toString(
								"Your languages in order of priority (you may ", 
								numLangs > 1 ? "reorder or " : "", 
								"remove any language except English "
								+ "and your institution's primary language):")) %>
					</td>
					<td style="padding-top:12px; vertical-align:top">
						<table>
						<% for (int langNum = 0; langNum < numLangs; langNum++) { %>
							<tr>
							<td style="text-align:center;">
								<% if (numLangs > 1) { %>
									<select name="langRank<%= langNum %>" 
											id="langRank<%= langNum %>">
									<% for (int optNum = 0; optNum <
											numLangs; optNum++) { %>
										<option value="<%= optNum + 1 %>"
												<%= optNum == langNum 
												? "selected='selected'" : "" %>>
										<%= optNum + 1 %> </option>
									<% } %>
									</select>
								<% } else { %>
									<b>1</b>
									<input name="langRank0" id="langRank0"
											type="hidden" value="1"/>
								<% } %>
							</td>
							<td>
								<%= Utils.capitalize(editUserLangs[langNum]) %>
							</td>
							<td style="padding-left:10px;">
								<% if (!AppConfig.defaultLanguage.equals(
											editUserLangs[langNum])
										&& (Utils.isEmpty(instnPrimaryLanguage)
											|| !instnPrimaryLanguage.equals(
												editUserLangs[langNum]))) { %>
									<%= makeButtonIcon("delete", pathToRoot, 
											"submitIt(document.editform,", 
											langNum + 1, ");") %>
								<% } // if this language is not the default language %>
							</td></tr>
						<% } // for each language %>
							<tr>
							<td style="text-align:center;">
								<b><%= numLangs > 0 ? numLangs + 1 : "" %></b>
							</td>
							<td>
								English
							</td>
							</tr>
						</table>
					</td>
				<% } // if numLangs 
				final char editUserRole = editUser.getRole(); %>
				</tr>
				<tr>
					<td class="regtext" style="padding-top:20px; padding-left:40px; 
							vertical-align:middle">
						<%= user.translate("Choose a new language to add:") %>
					</td>
					<td colspan="2" style="padding-top:20px;">
						<select name="languageSelector" id="languageSelector" 
								onchange="changeLanguage(document.editform);">
							<option value="<%= NONE %>"> &nbsp;</option>
						<% for (final String unchosenLang : unchosenLangs) { %>
							<option value="<%= 
									Utils.toValidHTMLAttributeValue(unchosenLang) %>">
								<%= Utils.capitalize(unchosenLang) %> </option>
						<% } %>
						<% if (editUserRole != User.STUDENT) { %>
							<option value="<%= OTHER %>"> [Other] </option>
						<% } %>
						</select>
					</td>
				</tr>
				<% if (editUserRole != User.STUDENT) { %>
				<tr>
					<td id="lang1" class="regtext" 
							style="padding-left:40px; vertical-align:middle;
							visibility:hidden;">
						<%= user.translate("Enter a language not in the menu:") %>
					</td>
					<td id="lang2" colspan="2" style="visibility:hidden;">
						<input type="text" id="other_language" 
								name="other_language" size="40" value="" 
								style="background-color:#eeeeee;" />
					</td>
				</tr>
				<% } %>
				<tr>
					<td style="padding-bottom:10px; padding-top:10px; padding-left:40px;">
						<%= makeButton(user.translate("Save changes"),
								"submitIt(document.editform, 0);") %>
					</td>
					<td style="padding-bottom:10px; padding-top:10px;">
						<%= makeButton(user.translate("Cancel"), "goBackAgain();") %>
					</td>
				</tr>
			</table>
			</form>
		</td>
	</tr>
	</table>
	</div>
</body>
</html>
