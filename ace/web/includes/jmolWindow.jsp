<%@ page language="java" %>
<%@ page import="
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String data = request.getParameter("data"); 
	final String scripts = request.getParameter("scripts"); 
	final String commands = request.getParameter("commands"); 
%>
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Jmol sketcher window</title>

<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jmolStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>nosession/jsmol/JSmol.min.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>nosession/jsmol/Jmol2.js" type="text/javascript"></script>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
</head>

<body style="overflow:auto;">
	<table class="whiteTable" style="width:100%;">
	<tr>
	<td>
		<script type="text/javascript">
			// <!-- avoid parsing the following as HTML
			setJmol(1, 
					'<%= Utils.toValidJS(data) %>',
					'white', 
					window.innerWidth - 120,
					window.innerHeight,
					'<%= Utils.toValidJS(scripts) %>');
			<%= Utils.toString(commands, Utils.isEmpty(commands) 
					|| commands.endsWith(";") ? "" : ";") %>
			// --> end HTML comment
		</script>
	</td>
	</tr>
	</table>
</body>
</html>


