<%@ page language="java" %>
<%@ page import="
	com.epoch.utils.Utils"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%
final String command = "/home/aceorg/aceorg/DEV-5.1/ace/web/nosession/email.pl " +
	request.getParameter("name") + " " +
	request.getParameter("username") + " " +
	request.getParameter("email") + " " +
	request.getParameter("number") + " " +
	request.getParameter("institution");

Runtime.getRuntime().exec(command);
%>
<script type="text/javascript">
// <!-- >
// alert('<%= command %>');
document.location.href="../login.jsp?flag=email";
// -->
</script>
