<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.session.ExportImportSession,
	com.epoch.utils.Utils,
	java.io.DataOutputStream,
	java.io.FileOutputStream"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	final String pathToRoot = "../";
	if (!Utils.among(role, User.ADMINISTRATOR, User.INSTRUCTOR)) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	request.setCharacterEncoding("UTF-8");

	// export one or more course documents  
	final String hwNumsStr = request.getParameter("hwNums");
	// Utils.alwaysPrint("exportHWSets.jsp: hwNumsStr = ", hwNumsStr);
	final String[] hwNumStrs = hwNumsStr.split(":");
	final int[] hwNums = Utils.stringToIntArray(hwNumStrs);
	final String courseName = course.getName();
	final String exportFilename = Utils.toString(courseName != null 
				? Utils.toValidFileName(courseName) : "Assgnts", 
			'_', hwNumsStr.replace(':', '_'));
	final int numHwSets = hwNums.length;
	final int[] hwIds = new int[numHwSets];
	for (int hwNum = 0; hwNum < numHwSets; hwNum++) {
		hwIds[hwNum] = assgts[hwNums[hwNum] - 1].id;
	} // for each assignment

	final String descrsXML = ExportImportSession.exportHWSets(hwIds);
	final String tempfile = Utils.toString(AppConfig.relTempDir, 
			AppConfig.relTempDir.endsWith("/") ? "" : '/',
			exportFilename, ".course");
	/* Utils.alwaysPrint("exportHWSets.jsp: exportFilename = ",
			exportFilename, ", tempfile = ", tempfile); /**/
	final String fullpath = application.getRealPath(tempfile);
	final DataOutputStream dos = new DataOutputStream(
			new FileOutputStream(fullpath));
	dos.writeBytes(descrsXML);
	dos.close();

%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Assignment Export</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
		* html body {
			padding:100px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/navigation/courseSidebarJS.jsp.h" %>
	
	function goBackAgain() {
		this.location.href = 'hwSetList.jsp';
	}
	// -->
	</script>
</head>

<body class="light" style="background-color:white;"
		onload="setTab('<%= toTabName(user.translateJS("Assignments")) %>');">

	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

	<div id="contentsWithTabsWithoutFooter">
	<table style="width:626px; text-align:center; margin-left:auto; margin-right:auto;">
		<tr>
		<td class="boldtext big" style="vertical-align:top; padding-top:10px;">
			<%= user.translate("Export Assignment") %>
		</td>
		</tr>
		<tr>
		<td style="vertical-align:top; text-align:left;">
			<table class="whiteTable" style="width:626px; background-color:#f6f7ed;">
				<tr><td style="padding-left:10px; padding-right:10px; padding-top:10px;">
					<span class="boldtext"><%= user.translate("PC") %></span>: 
					<%= user.translate("Right-click on the link below and select "
					+ "\"Save Target As...\" to download the text file to disk.") %><br />
				</td></tr>
				<tr><td style="padding-left:10px; padding-right:10px;">
					<span class="boldtext"><%= user.translate("Mac") %></span>: 
					<%= user.translate("Control-click on the link below and select "
					+ "\"Download Linked File\" to download the text file to disk.") %>
				</td></tr>
				<tr><td style="padding-left:10px; padding-right:10px; padding-top:10px;">
					[<a onclick="toAlert('<%= user.translateJS(
							"Please follow the directions above the link.") 
							%>'); return false"
						href="../<%= Utils.toValidHref(tempfile) %>"><%=
							user.translate("Exported assignments") %></a>]
				</td></tr>
				<tr>
				<td style="padding-bottom:10px; padding-top:10px; padding-left:10px;">
					<%= makeButton(user.translate("Back"), "goBackAgain();") %>
				</td>
				</tr>
			</table>
		</td>
		</tr>
	</table>
	</div>
</body>
</html>
