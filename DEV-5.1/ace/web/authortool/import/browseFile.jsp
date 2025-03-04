<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	final String howMuchStr = request.getParameter("howMuch");
	final String topicNum = request.getParameter("topicNum");
	synchronized (session) {
		session.setAttribute("howMuch", howMuchStr);
		session.setAttribute("topicNum", topicNum);
	}

	final int howMuch = Integer.parseInt(howMuchStr);
	final int QSET = 0;
	final int TOPIC = 1;
	final int BANK = 2;
	final String what = (howMuch == QSET ? "questions into this question set"
			: howMuch == TOPIC ? "question sets into this topic" 
			: "topics and question sets into the question bank");

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >

<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Question Importer</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">

	function fileUpload() {
		if (isEmpty(document.fileupload.setfile.value)) {
			alert('Please browse to the file containing the exported questions.');
			return;
		}
		document.fileupload.submit();
	} 

	</script>
</head>


<body class="light" style="background-color:white;">

<form name="fileupload" action="import.jsp" method="post" enctype="multipart/form-data">
	<table class="regtext" style="width:95%; margin-left:auto; margin-top:10px;
			margin-right:auto; border-style:none; border-collapse:collapse;"
			summary="">
		<tr><td class="boldtext big" >
			Import <%= what %>
		</td></tr>
		<tr><td class="regtext" 
				style="padding-top:10px;">
			<% if (howMuch == QSET) { %>
				Choose a zip file that someone exported from a list of questions 
				in a particular question set.  ACE will add the questions in the 
				zip file to the current question set.
			<% } else if (howMuch == TOPIC) { %>
				Choose a zip file that someone exported from a selected topic.  ACE 
				will add the question sets and questions of the exported 
				topic to the current topic.
			<% } else if (howMuch == BANK) { %>
				Choose a zip file that someone exported from the entire question
				bank.  ACE will add the topics, question sets, and questions 
				of the exported question bank to the current question bank.
			<% } // howMuch %>
		</td></tr>
		<tr><td style="padding-left:20px; padding-top:20px;">
			<input type="file" name="setfile" size="30"  />
		</td></tr>
		<tr><td style="padding-top:10px;">
			<table summary="">
				<tr><td style="padding-bottom:10px; padding-top:10px; padding-left:30px;">
					<%= makeButton("Upload", "fileUpload();") %>
				</td>
				<td style="padding-bottom:10px; padding-top:10px;">
					<%= makeButton("Cancel", "self.close();") %>
				</td></tr>
			</table>
		</td></tr>
	</table>
</form>
</body>
</html>







