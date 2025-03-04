<%@ page language="java" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
%>

<%
	synchronized (session) {
		session.removeAttribute("qbank");
		session.removeAttribute("userset");
	}
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
	<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	</head>
<script type="text/javascript">
	// <!-- >
	function goNext() {
		self.location.href = 'LITE_adminconfig.jsp';
	}
	// -->
</script>
<body onload="goNext();">
</body>
</html>

