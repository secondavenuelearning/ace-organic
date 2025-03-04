<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.Institution,
	com.epoch.session.AnonSession,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%	
	final String pathToRoot = "../"; 
	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	final Institution[] institutions = AnonSession.getVerifiedInstitutions();
	final int defaultInstitn = user.getInstitution().getId(); 
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<title>ACE Student Enrollment</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<style type="text/css">
		* html body {
			padding:50px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-->
		<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
		<%@ include file="/navigation/courseSidebarJS.jsp.h" %>

		function submitIt(form) {
			if (isWhiteSpace(form.studentnum.value)) {
				toAlert('<%= user.translateJS(
						"You must enter a student number.") %>');
				return;
			} else if (isWhiteSpace(form.name.value)) {
				toAlert('<%= user.translateJS(
						"You must enter a student name. "
						+ "If you do not know the student's name, "
						+ "enter a temporary name.") %>');
				return;
			}
			form.submit();
		}
		
		function changeStudentNumLabel() {
			var selectedInstitn = parseInt(document.editform.institutionSel.value);
			<% for (final Institution institution : institutions) { 
				final String studentNumLabel = institution.getStudentNumLabel(); %>
				if (<%= institution.getId() %> === selectedInstitn) {
					setInnerHTML('studentNumLabelCell1', 
							'<%= user.translateJS("Enroll New Student by ***ID Number***", 
								studentNumLabel) %>');
					setInnerHTML('studentNumLabelCell2', '<%= Utils.toValidJS(studentNumLabel) %>:');
				} // if this is the selected institution
			<% } // for each institution %>
		} // changeStudentNumLabel()

	// -->
	</script>
</head>
<body class="light" style="background-color:white;"
		onload="setTab('<%= toTabName(user.translateJS("Enrollment")) %>'); 
			changeStudentNumLabel();">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

	<div id="contentsWithTabsWithoutFooter">
	<table style="width:626px; text-align:center; margin-left:auto; margin-right:auto;">
		<tr>
		<td class="boldtext big" id="studentNumLabelCell1" 
				style="vertical-align:top; padding-top:10px;">
		</td>
		</tr>
		<tr>
		<td style="vertical-align:top; text-align:center;">
			<form name="editform" action="saveEnrollment.jsp" method="post" 
					accept-charset="UTF-8">
			<table class="whiteTable" style="width:626px; background-color:#f6f7ed;
					text-align:left;">
				<tr>
				<td class="regtext" style="padding-top:10px; padding-left:30px;">
					<%= user.translate("Name") %> (<%= user.translate("surname first") %>):
				</td>
				<td style="padding-top:10px;">
					<input type="text" name="name" size="40" value=""/>
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px; padding-left:30px;">
					<%= user.translate("Institution") %>:
				</td>
				<td style="padding-top:10px;">
					<select name="institutionSel" onchange="changeStudentNumLabel();">
						<% for (final Institution institution : institutions) { %>
							<option value="<%= institution.getId() %>"
									<%= institution.getId() == defaultInstitn 
										?  " selected=\"selected\"" : "" %> >
							<%= institution.getName() %></option>
						<% } // for each institution %>
					</select>
				</td>
				</tr>
				<tr>
				<td class="regtext" id="studentNumLabelCell2" 
						style="width:100%; padding-left:30px; padding-top:10px;">
				</td>
				<td style="text-align:left; padding-top:10px;">
					<input type="text" name="studentnum" size="40" value=""/>
				</td>
				</tr>
				<tr><td colspan="2"><table><tr>
				<td style="padding-bottom:10px; padding-top:10px; padding-left:30px;">
					<%= makeButton(user.translate("Enroll"), 
							"submitIt(document.editform);") %>
				</td>
				<td style="padding-bottom:10px; padding-top:10px;">
					<%= makeButton(user.translate("Cancel"), 
							"self.location.href='listEnrollment.jsp';") %>
				</td>
				</tr></table></td></tr>
			</table>
			</form>
		</td>
		</tr>
	</table>
	</div>
</body>
</html>

