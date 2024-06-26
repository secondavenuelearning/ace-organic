<%@ page language="java" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../";
	if (role != User.STUDENT) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}

	final String userId = user.getUserId();
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Payment Page</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
		* html body {
			padding:100px 0 50px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	
	// -->
	</script>
</head>

<body style="text-align:center; background-color:#FFFFFF;">

<div id="contentsWithTabsWithFooter">

<table class="regtext" style="margin-left:auto; margin-right:auto;
		border-style:none; border-collapse:collapse;
		width:626px;">
	<tr><td class="boldtext big" style="padding-top:10px; padding-bottom:10px;">
		Support ACE!
	</td></tr>
	<tr><td style="padding-bottom:20px;">
		Your financial contribution will ensure that ACE continues to be maintained
		into the future.
	</td></tr>
	<tr><td>
	<form action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
	<input type="hidden" name="cmd" value="_s-xclick">
	<input type="hidden" name="custom" value="<%= userId %>">
	<input type="hidden" name="hosted_button_id" value="HP2ZDME359N3Y">
	<table>
		<tr><td class="boldtext">
		<input type="hidden" name="on0" value="Support level">Support level:
		</td></tr>
		<tr><td>
			<select name="os0">
			<option value="Strong support">Strong $15.00 USD</option>
			<option value="Moderate support">Moderate $6.00 USD</option>
			<option value="Minimum support">Minimum $2.00 USD</option>
			</select> 
		</td></tr>
	</table>
	<input type="hidden" name="currency_code" value="USD">
	<p>
	<input type="image" 
			src="https://www.paypalobjects.com/en_US/i/btn/btn_buynowCC_LG.gif" 
			border="0" name="submit" 
			alt="PayPal - The safer, easier way to pay online!">
	<img alt="" border="0" 
			src="https://www.paypalobjects.com/en_US/i/scr/pixel.gif" 
			width="1" height="1">
	</form>
	</td></tr>
</table>
</div>
</body>
</html>
