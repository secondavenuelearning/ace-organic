<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.translations.QSetTransln,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";
	final String which = request.getParameter("which");
	final String whichTitle = which.substring(0, 1).toUpperCase()
			+ which.substring(1);
	final boolean getPhrases = "phrases".equals(which);
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >

<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Translated <%= whichTitle %> Importer</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	function fileUpload() {
		if (isEmpty(document.fileupload.setfile.value)) {
			alert('Please browse to the file containing the translations.');
			return;
		}
		document.fileupload.action = 
				'getTranslationsFile.jsp?which=<%= which + (getPhrases ? "" 
						: "&qSetId=" + request.getParameter("qSetId")) %>';
		document.fileupload.submit();
	} 

	// -->
	</script>
</head>

<body class="light" >

<form name="fileupload" method="post" enctype="multipart/form-data" action="dummy" >
	<table class="regtext" style="width:95%; margin-left:auto; margin-top:10px;
			margin-right:auto; border-style:none; border-collapse:collapse;">
		<tr><td class="boldtext big" >
			Import Translated <%= whichTitle %>
		</td></tr>
		<tr><td class="regtext" style="padding-top:10px; padding-left:30px;">
			Choose a tab-delimited file containing translated <%= which %>:
		</td></tr>
		<tr><td style=" text-align:left; padding-top:10px; padding-left:30px;">
			<input type="file" name="setfile" size="30"  />
		</td></tr>
		<tr><td style="padding-bottom:10px; padding-top:10px; padding-left:30px;">
			<table><tr><td>
				<%= makeButton("Upload", "fileUpload();") %>
			</td>
			<td style="padding-bottom:10px; padding-top:10px;">
				<%= makeButton("Cancel", "self.close();") %>
			</td></tr>
			</table>
		</td></tr>
		<tr><td style="padding-bottom:10px; padding-top:10px; padding-left:30px;">
			The format of each line should be:
			<% if ("phrases".equalsIgnoreCase(which)) { %>
				<p><pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ID number [tab] translation</pre>
				</p><p>where the ID number is the hash code of the English phrase,
				always negative.
				</p>
			<% } else { %>
				<p><pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;type [tab] Q number [tab] item number [tab] translation</pre>
				</p><p>where:
				<ul>
				<li>type = <%= QSetTransln.HEADER_TAG %>, 
				<%= QSetTransln.QSTMT_TAG %>,
				<%= QSetTransln.QDTEXT_TAG %>, 
				or <%= QSetTransln.FEEDBACK_TAG %>;
				</li><li>the question number is 1-based (empty if this translation is of 
				the header);
				</li><li>the item number is 1-based (empty if this translation is of 
				the header or question statement).
				</li>
				</ul>
				The items do not have to be ordered.
				</p>
			<% } // which %>
		</td></tr>
	</table>
</form>
</body>
</html>
