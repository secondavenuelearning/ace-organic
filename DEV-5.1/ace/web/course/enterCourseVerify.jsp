<%@ page contentType="text/html; charset=iso-8859-1" language="java"
	errorPage=""
	import="com.epoch.utils.Utils" %>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String passwd = request.getParameter("pphraseEntry");
%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<title>Entering Course</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<script src="<%= pathToRoot %>js/md5.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	var h_pass = str_md5('<%= Utils.toValidJS(passwd) %>');
	var pphrase = b64_md5(h_pass);

	function submitIt() {
		document.enterCourseForm.pphrase.value = pphrase;
		document.enterCourseForm.submit();
	}
	// -->
</script>
</head>

<body onload="submitIt()">
<form name="enterCourseForm" method="post" action="enterCourse.jsp">
	<input type="hidden" name="pphrase" value="" />
</form>
</body>
</html>
