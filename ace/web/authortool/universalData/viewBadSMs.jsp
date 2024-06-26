<%@ page language="java" %>
<%@ page import="
	com.epoch.synthesis.SynthBadSMs,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";

	final boolean masterEdit = "true".equals(request.getParameter("masterEdit"));
	final String[][] badSMs = SynthBadSMs.getBadSMsStrs();
	final int numBadSMs = badSMs.length;
	final int SM_NAME = SynthBadSMs.SM_NAME;
	final int SM_DEF = SynthBadSMs.SM_DEF;
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>Impossible Starting Materials</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon">
	<style type="text/css">
	body {
		margin:0;
		border:0;
		padding:0;
		height:100%; 
		max-height:100%; 
		font-family:arial, verdana, sans-serif; 
		font-size:76%;
		overflow: hidden; 
	}

	#qDataFooter {
		position:absolute; 
		bottom:0; 
		left:0;
		width:100%; 
		height:45px; 
		overflow:auto; 
		text-align:right; 
		vertical-align:bottom;
		padding-top:10px;
	}

	#qDataContents {
		position:fixed; 
		top:0px;
		left:0;
		bottom:50px; 
		right:0; 
		overflow:auto; 
	}

	* html #qDataContents {
		height:100%; 
	}

	* html body {
		padding:0px 0 50px 0; 
	}
	</style>

	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
	<script type="text/javascript">
		// <!-- >

		function editBadSM(name, def) {
			var go = new String.builder();
			go.append('editBadSM.jsp');
			if (name !== '') {
				go.append('?smName=').append(encodeURIComponent(name))
						.append('&smDef=').append(encodeURIComponent(def));
			}
			openReactionWindow(go.toString());
		} // editReaction()
 
		// -->
	</script>
</head>
<body class="light" style="margin:0px; margin-top:5px; background-color:#f6f7ed;">

<div id="qDataContents">
<table style="margin-left:auto; margin-right:auto; width:90%;">
	<tr><td class="boldtext big" style="padding-top:10px; padding-bottom:10px;">
		ACE Impossible Starting Materials
	</td></tr>
	<% if (!masterEdit) { %>
		<tr><td class="regtext" style="padding-bottom:10px;">
			Contact the programmers if you would like to add to this list.
		</td></tr>
	<% } %>
	<tr><td style="text-align:center;">
		<table>
			<% for (int smNum = 1; smNum <= numBadSMs; smNum++) { 
				final String[] badSM = badSMs[smNum - 1]; %>
				<tr>
				<td class="regtext" style="padding-left:10px;">
					<a name="sm<%= smNum %>"></a>
					<% String forDisplay = Utils.toDisplay(badSM[SM_NAME]);
					if (masterEdit) {
						forDisplay = "<a onclick=\"editBadSM('" 
									+ badSM[SM_NAME] + "', '" 
									+ badSM[SM_DEF] + "');\">"
								+ forDisplay + "</a>";
					} %>
					<%= forDisplay %>
			 	</td>
				</tr>
			<% } // for each smNum %>
		</table>

	</td></tr>
</table>
</div>
<div id="qDataFooter">
<table style="padding-top:10px; align:center; margin-left:auto; margin-right:auto;">
	<tr>
		<td><%= makeButton("Close", "self.close()") %></td>
		<% if (masterEdit) { %>
			<td style="padding-left:30px;">
				<%= makeButton("Add New Impossible SM", "editBadSM('', '')") %>
			</td>
		<% } %>
	</tr>
</table>
</div>
</body>
</html>

