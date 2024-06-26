
<%-- Set the content type header with the JSP directive --%>
<%@ page contentType = "application/vnd.ms-excel" %>

<%-- Set the content disposition header --%>
<% 	
	request.setCharacterEncoding("UTF-8");
	response.setHeader("Content-Disposition", "attachment; filename=\"" 
			+ request.getParameter("filename") + ".xls\""); 
%>
<%@ include file="grade.jsp" %>
