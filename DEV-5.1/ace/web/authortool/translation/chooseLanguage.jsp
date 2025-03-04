<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.session.AnonSession,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	final boolean masterEdit = entry.isMasterEdit();
	user.refreshLanguages();
	final String[] myLanguages = (masterEdit ? AnonSession.getAllLanguages()
			: user.getLanguages());
	final int numLanguages = myLanguages.length;
	if (numLanguages > 1) {
		synchronized (session) {
			session.removeAttribute("translationObj");
			session.removeAttribute("phraseTranslator");
			session.removeAttribute("translationLanguage");
		}
	}
	final String qSetId = request.getParameter("qSetId");
	final boolean translateGeneral = "0".equals(qSetId);
	final boolean findUntranslated = "true".equals(request.getParameter("find"));

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Translation</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
		#footer {
			position:absolute; 
			bottom:0; 
			left:0;
			width:100%; 
			height:40px; 
			overflow:auto; 
			text-align:right; 
		}

		#qEditorContents {
			position:fixed; 
			top:55px;
			left:0;
			bottom:40px; 
			right:0; 
			overflow:auto; 
		}

		* html body {
			padding:55px 0 40px 0; 
		}

		* html #footer {
			height:100%; 
		}

		* html #qEditorContents {
			height:100%; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >

	<% if (numLanguages > 1) { %> 

		<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	
		function doTranslate() {
			<% if (findUntranslated) { %>
				alert('This action may take a while. Please be patient.');
			<% } %>
			document.langForm.submit();
		}
		
		function goBackAgain() {
        	self.location.href = '<%= pathToRoot %>authortool/'
					+ 'questionsList.jsp?qSetId=<%= qSetId %>';
		}

	<% } // if more than one language %>

	function loadMe() {
		<% if (numLanguages == 1) { %> 
			document.langForm.language.value = '<%= Utils.toValidJS(
					myLanguages[0]) %>';
			alert('This action may take a while. Please be patient.');
			document.langForm.submit();
		<% } // if only one language %>
	}
	// -->

	</script>
</head>

<body class="light"
		style="background-color:white; text-align:center; 
		margin:0px; margin-top:5px;
		overflow:auto;" onload="loadMe();">
<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

<div id="qEditorContents">
<table class="regtext" style="margin-left:auto; margin-right:auto; 
		border-style:none; border-collapse:collapse;">
<tr>
<td class="regtext" style="padding-top:25px;">
	<%= findUntranslated && numLanguages == 1 ? "Finding questions not yet translated"
			: findUntranslated ? "Find questions not yet translated"
			: Utils.toString("Translate ", translateGeneral 
				? "general phrases" : "the current question set") %> into:
</td>
<td style="text-align:right; padding-top:25px; padding-left:10px;">
	<form name="langForm" action="<%= findUntranslated ? "findUntranslated.jsp"
			: translateGeneral ? "phraseTranslate.jsp" 
			: Utils.toString("translate.jsp?qSetId=", qSetId) %>" method="post">
	<% if (numLanguages > 1) { %>
		<select name="language">
		<% for (final String lang : myLanguages) { %>
			<option value="<%= Utils.toValidHTMLAttributeValue(lang) %>">
				<%= lang %>
			</option>
		<% } // for each language %>
	<% } else { %>
		<%= myLanguages[0] %>
		<input type="hidden" name="language" 
				value="<%= Utils.toValidHTMLAttributeValue(myLanguages[0]) %>" />
	<% } // if there's more than one language %>
	</select>
	</form>
</td>
</tr>

<% if (numLanguages > 1) { %>
	<tr>
	<td colspan="2" style="text-align:center; padding-top:20px;">
	<table style="margin-left:auto; margin-right:auto; border-collapse:collapse;">
		<tr>
		<td><%= makeButton("Continue", "doTranslate();") %></td>
		<td><%= makeButton("Go back", "goBackAgain();") %></td>
		</tr>
	</table>
	</td>
	</tr>
<% } // if there's more than one language %>
</table>
</div>

</body>
</html>
