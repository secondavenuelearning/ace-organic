<%@ page language="java" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	final String figureIndex = request.getParameter("figureIndex");
	final boolean reloadFigs = request.getParameter("reloadFigs") != null;
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<script type="text/javascript">
		// <!-- >
		function finish() {
			var go = '../question.jsp?qId=same&figureIndex=<%= figureIndex %>';  
			if (<%= reloadFigs %>) go += '&reloadFigs=true';
			self.location.href = go;
		}
		// -->
	</script>
</head>
<body onload="finish();">
</body>
</html>


