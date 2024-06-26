<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.utils.Utils"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	final String[] userLangs = user.getLanguages();
	final String preferredLang = (Utils.isEmpty(userLangs)
			? "" : userLangs[0]);
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<script type="text/javascript">
	function logout() {
		document.logoutform.submit();
	}
</script>
<body onload="logout()">
	<form name="logoutform" method="post" action="Login">
		<input type="hidden" name="cmd" value="logout"/>
		<input type="hidden" name="language" value="<%=
				Utils.toValidHTMLAttributeValue(preferredLang) %>"/>
	</form>
</body>
</html>
