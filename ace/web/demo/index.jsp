<%@ page language="java" %>
<%@ page import="
	java.sql.*"
%>
<%

final boolean track = false;

if(track) {

//java.util.Date datetime = java.util.Date();
// final java.util.GregorianCalendar c = new java.util.GregorianCalendar();
final String connectURL = "jdbc:mysql://128.151.75.99:3306/ace_feedback_logs";
Connection connection = null;
Statement statement = null;
int count342 = 0;


final String track_user = "Demo User";
final String track_role = "0";
final String track_institution = "Unknown Institution";

Class.forName("com.mysql.jdbc.Driver").getConstructor().newInstance();
connection = DriverManager.getConnection(connectURL, "feedback", "aceorg");
statement = connection.createStatement();
count342 = statement.executeUpdate("INSERT INTO `event_logs` (`user_id`, `url`, `timestamp`, `ip`, `browser`, `institution`, `instructor`, `key`) VALUES ('"+track_user+"', '"+request.getRequestURI()+"', null, '"+request.getRemoteAddr()+"', '"+request.getHeader("USER-AGENT")+"', '"+track_institution+"', '"+track_role+"', null)");
System.out.println(count342+" entry added to the user event table");

}
%>
<HTML>
<HEAD>
<TITLE>ACE Organic Demo</TITLE>
</HEAD>
<link rel="stylesheet" href="ace.css" type="text/css"/>
<BODY bgcolor="#E0E6C2" text=#49521B>

<h3><b>ACE Organic Demo</b></h3>
<a href="ace-demo.html">View Online</a><br/><br/>
Download for <a href="ace-demo_3-25-05.exe">Windows</a> / <a href="ace-demo_3-25-05.hqx">Mac</a><br/>

</BODY>
</HTML>


