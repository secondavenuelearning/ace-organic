<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.translations.PhraseTransln,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%	
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../"; 
	if (role != User.ADMINISTRATOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	String phrase = Utils.inputToCERs(request.getParameter("phrase"));
	// Utils.alwaysPrint("addEnglishPhrase.jsp: phrase = ", phrase);
	if (phrase != null) {
		phrase = phrase.trim();
		PhraseTransln.addEnglish(phrase);
	}
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<title>Add a Phrase</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<style type="text/css">
		* html body {
			padding:50px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
		<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

		function submitIt(form) {
			if (isWhiteSpace(form.phrase.value)) {
				alert('You must enter a phrase.');
				return;
			}
			form.submit();
		}
	// -->
	</script>
</head>
<body class="regtext">
	<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

	<div id="contentsWithoutTabs">
	<form name="editform" action="addEnglishPhrase.jsp" method="post">
	<table style="width:626px; text-align:center; margin-left:auto; margin-right:auto;">
		<% if (phrase != null) { %>
		<tr>
			<td class="regtext" style="vertical-align:top; 
					padding-top:10px;">
				<span style="color:blue;"><%= phrase %></span><p>has been added to the
				database.  A translator may now enter a translation for it 
				by going to the question bank.</p>
				<hr/>
			</td>
		</tr>
		<% } %>
		<tr>
			<td class="regtext" style="vertical-align:top; padding-top:10px;">
				<p><span class="boldtext" >Add an English 
				phrase</span> to the database so that translators may
				translate it.
				</p><p>The phrase should be one that is hardwired into the code, not
				one that can be edited through ACE.</p>
			</td>
		</tr>
		<tr>
			<td style="padding-top:20px; text-align:left;">
				Enter an English phrase to add:
			</td>
		</tr>
		<tr>
			<td style="padding-top:10px; text-align:left;">
				<textarea name="phrase" rows="4" cols="65"></textarea>
			</td>
		</tr>
		<tr><td colspan="2"><table><tr>
			<td style="padding-bottom:10px; padding-top:10px; padding-left:30px;">
				<%= makeButton("Add", "submitIt(document.editform);") %>
			</td>
			<td style="padding-bottom:10px; padding-top:10px;">
				<%= makeButton("Cancel", 
						"self.location.href='listProfiles.jsp';") %>
			</td>
			</tr></table></td></tr>
	</table>
	</form>
	</div>
</body>
</html>

