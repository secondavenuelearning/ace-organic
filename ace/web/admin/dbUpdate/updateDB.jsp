<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.session.AnonSession,
	com.epoch.access.EpochEntry,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../../";

	final EpochEntry entry = new EpochEntry(user.getUserId());
	synchronized (session) {
		session.setAttribute("entry", entry);
	}

	final String[] languages = AnonSession.getAllLanguages();

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Database Updater</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
		* html body {
			padding:55px 0 40px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

		function translate(how) {
			document.langForm.action = 
					(how === 'all' ? 'translationConvertAll.jsp'
						: 'translationConvert.jsp');
			document.langForm.submit();
		}

		function convertMe(where) {
			self.location.href = where;
		}
		// -->
		
	</script>
</head>

<body class="regtext" style="text-align:center; margin:0px; margin-top:5px;
		overflow:auto;">

<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

<div id="contentsWithoutTabs">
<table class="regtext" style="width:95%; margin-left:auto; margin-right:auto; 
		border-style:none; border-collapse:collapse;">
<tr><td style="padding-top:10px;">
Press the following button each time you upgrade JChem in ACE 3.1 or higher.
</td></tr>
<tr>
<td style="text-align:left; padding-top:10px;">
	<%= makeButton("Clear Reactor cache", 
			"convertMe('clearReactorCache.jsp');") %>
</td>
</tr>

<tr><td colspan="4"><hr></td></tr>
<tr><td style="padding-top:10px;">
Press the following button if you are upgrading from ACE 4.5 or 
below to ACE 5.0 or higher.	
</td></tr>
<tr>
<td style="text-align:left; padding-top:10px;">
	<%= makeButton("Make certain foreign keys deferrable", 
			"convertMe('autoConvert.jsp?toConvert=makeForeignKeysDeferrable');") %>
</td>
</tr>

<tr><td colspan="4"><hr></td></tr>
<tr><td colspan="4" style="padding-top:10px;">
Press the following button if you are upgrading from ACE 4.0 or 
below to ACE 4.1 or higher.	
</td></tr>
<tr><td colspan="4">
	<table>
	<tr>
	<td style="text-align:left; padding-top:10px;">
		<%= makeButton("Split caption strings into separate entries", 
				"convertMe('autoConvert.jsp?toConvert=captions');") %>
	</td>
	</tr>
	</table>
</td>
</tr>

<tr><td colspan="4"><hr></td></tr>
<tr><td colspan="4" style="padding-top:10px;">
Press the following button if you are upgrading from ACE 3.9 or 
below to ACE 4.0 or higher.	
</td></tr>
<tr><td colspan="4">
	<table>
	<tr>
	<td style="text-align:left; padding-top:10px;">
		<%= makeButton("Add points to assignment questions table", 
				"convertMe('autoConvert.jsp?toConvert=hwQs');") %>
	</td>
	</tr>
	</table>
</td>
</tr>

<tr><td colspan="4"><hr></td></tr>
<tr><td colspan="4" style="padding-top:10px;">
Press the following buttons if you are upgrading from ACE 3.4 or 
below to ACE 3.5 or higher.	
</td></tr>
<tr><td colspan="4">
	<table>
	<tr>
	<td style="text-align:left; padding-top:10px;">
		<%= makeButton("Write images to disk", 
				"convertMe('autoConvert.jsp?toConvert=images');") %>
	</td>
	</tr>
	<tr>
	<td style="text-align:left; padding-top:10px;">
		<%= makeButton("Convert clickable image coordinates to XML", 
				"convertMe('autoConvert.jsp?toConvert=clickHereToXML');") %>
	</td>
	</tr>
	</table>
</td>
</tr>

<tr><td colspan="4"><hr></td></tr>
<tr><td colspan="4" style="padding-top:10px;">
Press the following buttons if you are upgrading from ACE 3.3 or 
below to ACE 3.4 or higher.	
</td></tr>
<tr><td colspan="4">
	<table>
	<tr>
	<td style="text-align:left; padding-top:10px;">
		<%= makeButton("Convert synthesis starting material expressions", 
				"convertMe('autoConvert.jsp?toConvert=synthSMExpr');") %>
	</td>
	</tr>
	</table>
</td>
</tr>

<tr><td colspan="4"><hr></td></tr>
<tr><td colspan="4" style="padding-top:10px;">
Press the following buttons if you are upgrading from ACE 3.2 or 
below to ACE 3.3 or higher.	
<br />(These conversions may take a very long time.  Be patient.)
</td></tr>
<tr><td colspan="4">
	<table>
	<tr>
	<td style="text-align:left; padding-top:10px;">
		<%= makeButton("Make new questions, dependencies tables", 
				"convertMe('autoConvert.jsp?toConvert=newAssgtTables');") %>
	</td>
	</tr>
	</table>
</td>
</tr>

<tr><td colspan="4"><hr></td></tr>
<tr><td colspan="4" class="regtext" style="padding-top:10px;">
<p>
The following buttons convert any non-ASCII text stored in the 
database in the platform-specific UTF-8 or Latin-1 formats into 
platform-independent character entity representations.
<br />Press them if you are upgrading from ACE 3.2 to ACE 3.3 or higher.
</p>
</td>
</tr>

<tr>
<td colspan="2" style="text-align:left; padding-top:10px;">
	<%= makeButton("Convert common question statements", 
			"convertMe('headersConvert.jsp?local=false');") %>
</td>
<td colspan="2" style="text-align:left; padding-top:10px;">
	<%= makeButton("Convert local authors' common question statements", 
			"convertMe('headersConvert.jsp?local=true');") %>
</td>
</tr>
<tr>
<td colspan="2" style="text-align:left; padding-top:25px;">
	<%= makeButton("Convert question statements", 
			"convertMe('statementsConvert.jsp?local=false');") %>
</td>
<td colspan="2" style="text-align:left; padding-top:25px;">
	<%= makeButton("Convert local authors' question statements", 
			"convertMe('statementsConvert.jsp?local=true');") %>
</td>
</tr>
<tr>
<td colspan="2" style="text-align:left; padding-top:25px;">
	<%= makeButton("Convert question data", 
			"convertMe('questionDataConvert.jsp?local=false');") %>
</td>
<td colspan="2" style="text-align:left; padding-top:25px;">
	<%= makeButton("Convert local authors' question data", 
			"convertMe('questionDataConvert.jsp?local=true');") %>
</td>
</tr>
<tr>
<td colspan="2" style="text-align:left; padding-top:25px;">
	<%= makeButton("Convert feedback", 
			"convertMe('feedbackConvert.jsp?local=false');") %>
</td>
<td colspan="2" style="text-align:left; padding-top:25px;">
	<%= makeButton("Convert local authors' feedback", 
			"convertMe('feedbackConvert.jsp?local=true');") %>
</td>
</tr>
<tr>
<td colspan="2" style="text-align:left; padding-top:25px;">
	<%= makeButton("Convert evaluator text", 
			"convertMe('evalTextConvert.jsp?local=false');") %>
</td>
<td colspan="2" style="text-align:left; padding-top:25px;">
	<%= makeButton("Convert local authors' evaluator text", 
			"convertMe('evalTextConvert.jsp?local=true');") %>
</td>
</tr>
<tr>
<td class="regtext" style="text-align:left; padding-top:25px;">
	Convert translations in:
</td>
<td style="text-align:left; padding-top:25px; padding-left:10px;">
	<form name="langForm" action="" method="post">
	<select name="language">
	<% for (final String lang : languages) { %>
		<option value="<%= Utils.toValidHTMLAttributeValue(lang) %>">
			<%= lang %>
		</option>
	<% } // for each language %>
	</select>
	</form>
</td>
<td style="text-align:left; padding-top:25px;">
	<%= makeButton("Convert one by one", "translate('one');") %>
</td>
<td style="text-align:left; padding-top:25px;">
	<%= makeButton("Convert all at once", "translate('all');") %>
</td>
</tr>

<tr><td colspan="4"><hr></td></tr>
<tr><td colspan="4" style="padding-top:10px;">
Press the following button if you are upgrading from ACE 3.1 or below to ACE 3.2 or higher.	
<br />(This conversion may take a very long time.  Be patient.)
<br /><b>Warning</b>: This conversion is irreversible, and you will not be able to revert 
back to ACE 3.1 or below afterwards.
</td></tr>
<tr><td colspan="4">
	<table>
	<tr>
	<td style="text-align:left; padding-top:10px;">
		<%= makeButton("Convert Lewis structure format", 
				"convertMe('autoConvert.jsp?toConvert=Lewis');") %>
	</td>
	</tr>
	</table>
</td>
</tr>

<tr><td colspan="4"><hr></td></tr>
<tr><td colspan="4" style="padding-top:10px;">
Press the following buttons if you are upgrading from ACE 3.0 or below to ACE 3.1 or higher.	
<br />(These conversions may take a very long time.  Be patient.)
</td></tr>
<tr><td colspan="4">
	<table>
	<tr>
	<td style="text-align:left; padding-top:10px;">
		<%= makeButton("Move BLOB data to CLOBs, update evaluator data", 
				"convertMe('autoConvert.jsp?toConvert=BLOBs');") %>
	</td>
	<td style="text-align:left; padding-top:10px;">
		<%= makeButton("Convert synthesis format", 
				"convertMe('autoConvert.jsp?toConvert=synFormat');") %>
	</td>
	</tr>
	</table>
</td>
</tr>

<tr><td colspan="4"><hr></td></tr>
<tr><td colspan="4" style="padding-top:10px;">
Press the following buttons if you are upgrading from ACE 2.x or below to ACE 3.0 or higher.
</td></tr>
<tr><td colspan="4">
	<table>
	<tr>
	<td style="text-align:left; padding-top:10px;">
		<%= makeButton("Convert 1H and 2H to ^1H and ^2H", 
				"convertMe('autoConvert.jsp?toConvert=1HAnd2H');") %>
	</td>
	<td style="text-align:left; padding-top:10px; padding-left:20px;">
		<%= makeButton("Move test strings to BLOBs", 
				"convertMe('autoConvert.jsp?toConvert=textContains');") %>
	</td>
	</tr>
	</table>
</td>
</tr>

</table>

</div>
</body>
</html>
