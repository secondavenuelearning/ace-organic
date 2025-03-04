<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.constants.AppConstants,
	com.epoch.session.AnonSession,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%	final String pathToRoot = "../"; 

	/* parameters 
			editindex - number if the user is edited by the admin
						 0  if the user is added by the admin
						null, if an ordinary user is editing his profile
	*/

	final String[] allLangs = AnonSession.getAllLanguages();
	final String ENGLISH = AppConstants.ENGLISH;
	final String SELECTED = "selected=\"selected\"";

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Default Language</title>
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
		this.location.href = 'listProfiles.jsp';
	}

	function submitIt(form, removePosn) {
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
			Change default language
		</td>
	</tr>
	<tr>
		<td style="vertical-align:top; text-align:center;">
			<form name="editform" action="saveDefaultLanguage.jsp" method="post" 
					 accept-charset="UTF-8"> 
			<table class="whiteTable" style="width:626px; background-color:#f6f7ed;
					text-align:left;">
				<tr>
					<td class="regtext" style="padding-top:20px; padding-left:40px; 
							vertical-align:middle">
						Choose default language:
						<select name="languageSelector" id="languageSelector" 
								onchange="changeLanguage(document.editform);">
							<option value="<%= ENGLISH %>"
								<%= AppConfig.defaultLanguage.equals(ENGLISH)
									? SELECTED : "" %>>
								<%= ENGLISH %> </option>
						<% for (final String lang : allLangs) { %>
							<option value="<%= Utils.toValidHTMLAttributeValue(lang) %>"
								<%= AppConfig.defaultLanguage.equals(lang)
									? SELECTED : "" %>>
								<%= lang %> </option>
						<% } %>
						</select>
					</td>
				</tr>
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
