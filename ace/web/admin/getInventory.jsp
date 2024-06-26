<%-- Set the content type header with the JSP directive 
--%><%@ page contentType = "application/vnd.ms-excel" 
%><%@ page language="java" 
%><%@ page import="
	com.epoch.db.QuestionRW"
%><%@ page errorPage="/errormsgs/errorHandler.jsp"
%><%@ include file="/navigation/menuHeaderJava.jsp.h" 
%><%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../";

	final String[][] inventory = QuestionRW.getInventory();

%><table>
<% boolean first = true;
for (final String[] question : inventory) { %>
<tr><%	
	for (final String datum : question) { 
		final String tx = (first ? "th" : "td"); %>
<%= "<" + tx + ">" %><%= datum %><%= "</" + tx + ">" %><% 	
	} // for each datum 
	first = false; %>
</tr><% 
} // for each question %>
</table>
