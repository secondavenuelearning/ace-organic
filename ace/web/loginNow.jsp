<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page contentType="text/html; charset=iso-8859-1" %>
<%@ page import="
	java.util.Locale,
	com.epoch.AppConfig,
	com.epoch.utils.AuthUtils,
	com.epoch.utils.Utils" %>
<%
	 request.setCharacterEncoding("UTF-8"); // turned off so userId is raw input
	final String nonce = Utils.toValidJS(AuthUtils.getNonce());
	synchronized (session) {
		session.setAttribute("nonce", nonce);
	}
	final String userId = request.getParameter("userid");
	final String userIdToCERs = Utils.inputToCERs(userId);
	/*/ System.out.println("loginNow.jsp: raw userId = " + userId +
			", toCers = " + userIdToCERs); /**/
	final String passwd = request.getParameter("pphrase_entry");
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<title>Logging In</title>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<script src="js/md5.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	var unencodedPwd = '<%= Utils.toValidJS(passwd) %>';
	var encodedPwd = b64_md5('<%= nonce %>' + str_md5(unencodedPwd));

	function login() {
		document.loginform.pphraseEncoded.value = encodedPwd;
		document.loginform.pphraseUnencoded.value = unencodedPwd;
		document.loginform.submit();
	}
	// -->
</script>
</head>

<body onload="login()">
<form name="loginform" method="post" action="Login" accept-charset="UTF-8">
	<input type="hidden" name="userid" value="<%= 
			Utils.toValidHTMLAttributeValue(userId) %>" />
	<input type="hidden" name="useridToCERs" value="<%= 
			Utils.toValidHTMLAttributeValue(userIdToCERs) %>" />
	<input type="hidden" name="cmd" value="login" />
	<input type="hidden" name="pphraseEncoded" value="" />
	<input type="hidden" name="pphraseUnencoded" value="" />
	<input type="hidden" name="language" value="<%= Utils.toValidHTMLAttributeValue(
			request.getParameter("language")) %>"/> 
</form>
</body>
</html>
