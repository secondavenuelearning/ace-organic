<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.db.UserRead,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	final String pathToRoot = "../";

	/* Home page of a course 
	   Components will be editable for the instructor and administrator

	   The session can be of an
		- administrator impersonating an instructor or student
		- instructor
		- student
	*/

	final int crsId = course.getId();
	final User instructor = UserRead.getUser(course.getOwnerId());
	User[] coinstructors;
	int marvinLivePort;
	if (userSess instanceof AdminSession) {
		final AdminSession adminSess = (AdminSession) userSess;
		coinstructors = adminSess.getCoinstructors(crsId);
		marvinLivePort = adminSess.getMarvinLivePort();
	} else if (userSess instanceof InstructorSession) {
		final InstructorSession instrSess = (InstructorSession) userSess;
		coinstructors = instrSess.getCoinstructors(crsId);
		marvinLivePort = instrSess.getMarvinLivePort();
	} else {
		final StudentSession studSess = (StudentSession) userSess;
		coinstructors = studSess.getCoinstructors(crsId);
		marvinLivePort = studSess.getMarvinLivePort();
	}

	Course tutorialCourse; 
	synchronized (session) {	
		tutorialCourse = (Course) session.getAttribute("tutorialCourse");
	}
	final boolean isTutCrs = crsId == tutorialCourse.getId();
	final boolean isTutCrsOwner = user.getUserId().equals(tutorialCourse.getOwnerId());

	final StringBuilder onloadBld = new StringBuilder();
	onloadBld.append("onload=\"setTab('")
			.append(toTabName(user.translateJS("Course Home")))
			.append("');");
	/* if (user.isPastGracePeriod() && !user.hasPaid()) { 
			onloadBld.append(" openPaymentWindow('")
					.append(pathToRoot)
					.append("enroll/payment.jsp');");
	} // if should pay
	/**/
	final String onload = onloadBld.append("\"").toString();

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Course Home</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<style type="text/css">
		* html body {
			padding:50px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/navigation/courseSidebarJS.jsp.h" %>
	
	<% if (role != User.STUDENT) { %>
	function editNotes() {
		openNotesWindow('<%= pathToRoot %>course/editNotes.jsp');
	}
	<% } %>

	function marvinLive(port) {
		var bld = new String.builder();
		bld.append('marvinLive.jsp?port=').append(port).
				append('&idleTime=').append(trim(getValue('idleTime')));
		var url = bld.toString();
		openMarvinLiveWindow(url);
	} // marvinLive()

	function endMarvinLive() {
		this.location.href = 'endMarvinLive.jsp';
	} // endMarvinLive()

	// -->
	</script>
</head>
<body class="light" style="background-color:white; text-align:center; margin:0px;"
		<%= onload %>>

	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

	<div id="contentsWithTabsWithoutFooter">
	<table style="width:680; text-align:left; margin-left:auto;
			margin-right:auto;" summary="">
		<tr><td class="boldtext big" style="vertical-align:top;
				padding-top:10px;">
			<%= user.translate("Course Information") %>
		</td></tr> 
		<% if (isTA) { %>
			<tr><td class="boldtext" style="vertical-align:top;
					padding-top:1px; color:green;">
				<%= user.translate("You are a TA for this course.") %>
			</td></tr>
		<% } %>
		<tr><td style="vertical-align:top; text-align:center;">
			<table class="whiteTable" style="width:680px;
					background-color:#f6f7ed; text-align:left;" summary="">
				<tr><td class="boldtext" style="padding-left:30px;
						padding-top:10px; vertical-align:top; width:130px;">
					<%= Utils.group(user.translate("Course")) %>:
				</td><td class="regtext" colspan="2"
						style="padding-top:10px; vertical-align:top;
						padding-right:30px;">
					<span title="<%= user.translate("Course ID") %>: <%= crsId %>">
							<%= course.getName() %></span>
				</td></tr>
				<tr><td class="boldtext" style="padding-left:30px;
						padding-top:10px; vertical-align:top; width:130px;">
					<%= user.translate("Book") %>:
				</td><td class="regtext" colspan="2"
						style="padding-top:10px; vertical-align:top;
						padding-right:30px;">
					<% final String book = course.getBook();
					if (!Utils.isEmpty(book)) { %>
						<%= book %>
					<% } else { %>
						<%= user.translate("Other") %>
					<% } %>	
				</td></tr>
				<% final String desc = course.getDescription();
				if (!Utils.isEmpty(desc)) { %>
					<tr><td class="boldtext" style="padding-left:30px;
							padding-top:10px; vertical-align:top; width:130px;">
						<%= user.translate("Description") %>:
					</td><td class="regtext" colspan="2"
							style="vertical-align:top; padding-top:10px;
							padding-right:30px;"> 
						<%= desc %>
					</td></tr>
				<% } // if there's a course description %>
				<tr><td class="boldtext" style="padding-left:30px;
						padding-top:10px; vertical-align:top;">
					<%= user.translate(Utils.isEmpty(coinstructors)
							? "Instructor" : "Instructors") %>:
				</td><td class="regtext" colspan="2"
						style="vertical-align:top; padding-top:10px;
						padding-right:30px;">
					<a href="mailto:<%= instructor.getEmail() %>"><%= 
							Utils.unicodeToCERs(instructor.getName().toString()) %></a>, 
					<%= instructor.getInstitutionName() %>
					<% if (coinstructors != null) {
						for (final User coinstructor : coinstructors) { %>
						<br /><a href="mailto:<%= coinstructor.getEmail() %>"><%= 
								coinstructor.getName().toString() %></a>, 
						<%= coinstructor.getInstitutionName() %>
						<% } // for each coinstructor %>
					<% } // if coinstructors != null %>
				</td></tr> 
				<% if (!"http://".equals(course.getHomePage())) { %>
					<tr><td class="boldtext" style="padding-left:30px;
							padding-top:10px; vertical-align:top;">
						<%= user.translate("Course home page") %>:
					</td><td class="regtext" colspan="2" style="
							vertical-align:top; padding-top:10px; padding-right:30px;">
						<a href="<%= course.getHomePage() %>" target="_blank">
							<%= course.getHomePage() %></a>
					</td></tr>
				<% } // if there's a course home page %>
				<% final String notes = course.getNotes(); 
				if (isInstructor && (!isTutCrs || isTutCrsOwner)) { %>
					<tr><td class="boldtext" style="padding-left:30px;
							padding-top:10px; vertical-align:top;">
						<% if (isTutCrs) { %> 
							System notice: 
						<% } else { %>
							<%= user.translate("Notes") %>:
						<% } %>
					</td>
					<td id="courseNotes" class="regtext" 
						<%= Utils.isEmpty(notes) ? "style=\"width:0px;\""
								: "colspan=\"2\" style=\"vertical-align:top; "
										+ "padding-top:10px; padding-right:5px;\""
							%> >
						<% if (!Utils.isEmpty(notes)) { %>
							<table style="width:100%; border:1px solid #49521B;
									background-color:#FFFFFF" summary="">
								<tr><td class="regtext"
										style="vertical-align:top; padding-top:5px;
										padding-left:5px; padding-right:5px;
										padding-bottom:5px;">
									<%= Utils.toDisplay(notes) %>
								</td></tr>
							</table>
						<% } %>
					</td><td class="regtext" style="vertical-align:top;
							padding-top:10px; padding-right:10px;">
						<%= makeButtonIcon("edit", pathToRoot, "editNotes();") %>
					</td></tr>
				<% } else if (!Utils.isEmpty(notes)) { %>
					<tr><td class="boldtext" style="padding-left:30px;
							padding-top:10px; vertical-align:top;">
						<%= user.translate("Notes") %>:
					</td><td class="regtext" colspan="2"
							style="vertical-align:top; padding-top:10px;
							padding-right:30px;">
						<table style="width:100%; border:1px solid #49521B;
								background-color:#FFFFFF" summary="">
							<tr><td class="regtext" style="vertical-align:top;
									padding-top:5px; padding-left:5px;
									padding-right:5px; padding-bottom:5px;">
								<%= Utils.toDisplay(notes) %>
							</td></tr>
						</table>
					</td></tr>
				<% } %>
				<tr><td class="boldtext" style="padding-left:30px;
						padding-top:10px; vertical-align:top;">
					<%= Utils.group(user.translate("Problems")) %>?
				</td><td class="regtext" colspan="2"
						style="vertical-align:top; padding-top:10px;
						padding-right:30px;">
					<% String transln = null;
					if (isInstructor) { 
						transln = user.translate("Report software bugs (errors, "
								+ "crashing problems, inability to access content, etc.) "
								+ "and problems with the content (correct answers rejected "
								+ "as incorrect, irrelevant feedback, etc.) to the "
								+ "***webmaster***.");
					} else {
						transln = user.translate("Report software bugs (errors, "
								+ "crashing problems, inability to access content, etc.) "
								+ "to the ***webmaster***.")
								+ "<br/><br/>"
								+ user.translate("Report problems with the content (correct "
								+ "answers rejected as incorrect, irrelevant feedback, etc.) "
								+ "to your instructor.");
					} 
					transln = transln.replaceFirst("\\*\\*\\*", 
							"<a href=\"mailto:" + AppConfig.webmasterEmail + "\">");
					transln = transln.replaceFirst("\\*\\*\\*", "</a>");
					%>
					<%= transln %>
				</td></tr>
				<% if (false && !isTutCrs 
						&& (marvinLivePort != 0 || role != User.STUDENT || isTA)) { %>
					<tr><td id="marvinLiveButton" class="boldtext" 
							style="padding-left:30px; padding-top:10px;">
						<%= makeButton(user.translate(
								Utils.toString(marvinLivePort == 0
									? "Open" : "Join", " chat room")), 
								"marvinLive(", marvinLivePort, ");") %>
					</td>
					<% if (marvinLivePort == 0) { %>
						<td class="boldtext" id="idleTimeCell" 
								style="padding-top:10px;">
						<%= user.translate("Idle time before conversation is "
								+ "deleted (e.g., 90m, 2h, 30d)") %>:
						<input type="text" id="idleTime" size="2" value="30d" />
						</td>
					<% } else if (role != User.STUDENT) { %>
						<td class="boldtext" style="padding-top:10px;">
						<%= makeButton(user.translate("Close chat room"), 
								"endMarvinLive();") %>
						</td>
					<% } // if session is new or user is instructor %>
					</tr>
				<% } // if chat room is possible %>
			</table>
		</td></tr>
	</table>
	</div>
</body>
</html>
