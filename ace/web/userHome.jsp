<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.Course,
	com.epoch.db.CourseRW,
	java.util.Map"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "";

	/* Home page of a user: For instructor, a list of his courses, and for student,
	 * a list of the courses in which he is enrolled.  Components will be
	 * editable for the instructor and administrator.
	 */

	String ipAddr;
	final Course tutorialCourse = 
			CourseRW.getCourseInfo(AppConfig.tutorialId); // for user alerts
	synchronized (session) {	
		session.setAttribute("usersession", userSess);
		session.setAttribute("tutorialCourse", tutorialCourse);
		ipAddr = request.getRemoteAddr();
	}
	
	/* The session can be of an
	- administrator 
	- administrator impersonating an instructor or student
	- instructor
	- student
	*/
	Course[] courses = new Course[0];
	String onload = "";
	final boolean refresh = request.getParameter("refresh") != null;
	boolean isInstructor = false;
	Map<Integer, String[]> coursesIPs = null;
	boolean[] taStatuses = null;
	switch (realRole) {
		case User.ADMINISTRATOR:
			if (role == User.ADMINISTRATOR) {
				// in UK version, administrator goes straight to user list unless
				// impersonating someone
				onload = " onload=\"goToAdmin();\"";
			} else { 
				// either impersonating someone, or administrator in Pearson
				// version, who also owns & needs access to the tutorial course
				final AdminSession adminSess = (AdminSession) userSess;
				courses = adminSess.getCourses();
				coursesIPs = adminSess.getCoursesAllowedIPAddrs();
				isInstructor = role != User.STUDENT;
				if (!isInstructor) {
					if (refresh) adminSess.refreshCourses();
					taStatuses = new boolean[courses.length];
					int crsNum = 0;
					for (final Course course : courses) {
						taStatuses[crsNum++] = adminSess.isTA(course.getId());
					} // for each course
				} // if impersonating a student
			} // if administrator in UK version
			break;
		case User.INSTRUCTOR:
			isInstructor = true;
			final InstructorSession instrSess = (InstructorSession) userSess;
			courses = instrSess.getCourses();
			coursesIPs = instrSess.getCoursesAllowedIPAddrs();
			break;
		case User.STUDENT:
		default: // shouldn't happen
			final StudentSession studSess = (StudentSession) userSess;
			if (refresh) studSess.refreshCourses();
			courses = studSess.getCourses();
			coursesIPs = studSess.getCoursesAllowedIPAddrs();
			taStatuses = new boolean[courses.length];
			int crsNum = 0;
			for (final Course course : courses) {
				taStatuses[crsNum++] = studSess.isTA(course.getId());
			} // for each course
			break;
	} // switch realRole 
	if (role != User.ADMINISTRATOR) {
		final String instnPrimaryLanguage = 
				user.getInstitution().getPrimaryLanguage();
		user.refreshLanguages();
		if (!Utils.isEmpty(instnPrimaryLanguage)) {
			final String[] userCurrentLangs = user.getLanguages();
			/*/ Utils.alwaysPrint("userHome.jsp: primary language of the "
					+ "institution of ", user.getUserId(),
					" is ", instnPrimaryLanguage,
					"; userCurrentLangs = ", userCurrentLangs); /**/
			if (!Utils.contains(userCurrentLangs, instnPrimaryLanguage)) {
				/*/ Utils.alwaysPrint("userHome.jsp: adding ",
						instnPrimaryLanguage, " to this user's languages"); /**/
				user.addLanguage(instnPrimaryLanguage);
			} // if user is not using primary language of institution
		} // if there's a non-English language of instruction
	} // if user is not an administrator
	boolean coursesHidden = false;
	final String[] userLangs = user.getLanguages();

	/* if (Utils.isEmpty(onload) && user.isPastGracePeriod() && !user.hasPaid()) {
		onload = " onload=\"openPaymentWindow('enroll/payment.jsp');\"";
	} // if should pay
	/**/

	final String INSTRUCTOR_WILL = 
			"Your instructor will enroll you in a course. If you are sure "
			+ "that your instructor has already enrolled all students, press "
			+ "<b>My Profile</b> and check that you chose the correct "
			+ "institution from the popup menu and that you typed your "
			+ "student ID number correctly.";
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
	<title>ACE User Home</title>
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
	
	function addCourse() {
		self.location.href = '<%= pathToRoot %>course/editCourse.jsp?index=0';
	} // addCourse()

	function editCourse(index) {
		self.location.href = 
				'<%= pathToRoot %>course/editCourse.jsp?index=' + index;
	} // editCourse()

	function cloneCourse(index) {
		self.location.href = 
				'<%= pathToRoot %>course/editCourse.jsp?clone=true&index=' + index;
	} // cloneCourse()

	function revealCourse() {
		self.location.href = '<%= pathToRoot %>course/revealCourse.jsp';
	} // revealCourse()

	function deleteCourse(index) {
		if (toConfirm('<%= user.translateJS("You can hide a course from "
				+ "yourself and your students without actually deleting it. "
				+ "To hide a course, press its Edit icon, and check the "
				+ "appropriate box. \n\nDo you still want to delete this "
				+ "course?") %>')) {
			if (toConfirm('<%= user.translateJS("If you delete this course, "
					+ "you will irretrievably delete all assignments and "
					+ "grades associated with this course as well. Do you "
					+ "still wish to continue?") %>')) {
				self.location.href = 'course/deleteCourse.jsp?index=' + index;
			} // if confirm delete
		} // if confirm delete
	} // deleteCourse()

	function addCoinstructor(crsId) {
		self.location.href = 
				'<%= pathToRoot %>course/addCoinstructor.jsp?crsId=' + crsId; 
	} // addCoinstructor()

	function moveCourse(crsId, oldCrsNum) {
		var selectorArr = document.getElementsByName('courseNum' + crsId);
		var newCrsNum = (selectorArr[0] ? selectorArr[0].value : 0);
		var go = new String.builder();
		go.append('<%= pathToRoot %>course/moveCourse.jsp?oldCrsNum=');
		go.append(oldCrsNum);
		go.append('&newCrsNum=');
		go.append(newCrsNum);
		self.location.href = go.toString();
	} // moveCourse()

	function selectCourse(index) {
		<% if (user.isExamStudent() && !user.hasChangedProfile()) { %>
			toAlert('<%= user.translateJS("Please press the My Profile "
					+ "button above and enter your real name and student ID "
					+ "number.") %>');
		<% } else { %>
			self.location.href = 
					'<%= pathToRoot %>course/selectCourse.jsp?index=' + index;
		<% } // if exam student with unchanged profile %>
	} // selectCourse()

	function refresh() {
		self.location.href = '<%= pathToRoot %>userHome.jsp?refresh=true';
	} // refresh()

	var hangsAfterOneResponsePainted = false;
	var hangsAfterOneResponseOut = '<%= user.translateJS("If you are using "
			+ "Internet Explorer, switch to Firefox or Chrome.") %>';
			
	function paintHangsAfterOneResponse() {
		setInnerHTML('hangsAfterOneResponse', hangsAfterOneResponsePainted
				? '' : hangsAfterOneResponseOut);
		hangsAfterOneResponsePainted = !hangsAfterOneResponsePainted;
	} // paintHangsAfterOneResponse()

	var seemsToHangPainted = false;
	var seemsToHangOut = '<%= user.translateJS("Wait at least two minutes before "
			+ "declaring it hung. The system sometimes pauses for a couple "
			+ "of minutes to clean out garbage, and it won't do anything else "
			+ "while it is cleaning house. ACE may require a particularly "
			+ "long time to evaluate responses to mechanism questions. You might "
			+ "also try reloading the page. PC: press CTRL-R or F5; Mac: press "
			+ "COMMAND-R or F5.") %>';
			
	function paintSeemsToHang() {
		setInnerHTML('seemsToHang', seemsToHangPainted ? '' : seemsToHangOut);
		seemsToHangPainted = !seemsToHangPainted;
	} // paintSeemsToHang()

	var noSubmitPainted = false;
	var noSubmitOut = '<%= user.translateJS("Reload the page. PC: press "
			+ "CTRL-R or F5; Mac: press COMMAND-R or F5.") %>';
			
	function noSubmit() {
		setInnerHTML('noSubmit', noSubmitPainted ? '' : noSubmitOut);
		noSubmitPainted = !noSubmitPainted;
	} // noSubmit()

	var MarvinBlankPainted = false;
	var MarvinBlankOut = '<%= user.translateJS("Please take these steps:") %>'
			+ '<ol>'
			+ '<li><%= user.translateJS("Press the Undo button on the Marvin "
					+ "toolbar. Your response should reappear.") %>'
			+ '<li><%= user.translateJS("Choose Edit &rarr; Source from the "
					+ "Marvin menu. A new window will open.") %>'
			+ '<li><%= user.translateJS("Copy the entire contents "
						+ "of the window, and paste them into the body of "
						+ "an email message to ***Product Support***.",
					Utils.toString("<a href=\"mailto:", AppConfig.webmasterEmail, 
						"\">", user.translate("Product Support"), "</a>")) %>'
			+ '<li><%= user.translateJS("Add to the email message "
					+ "your course and section, the assignment number, "
					+ "and the question number. It would also help "
					+ "if you could copy the question figure and paste "
					+ "it into the email message, too.") %>'
			+ '<li><%= user.translateJS("Send the email.") %>'
			+ '<\/ol>';
					
	function paintMarvinBlank() {
		setInnerHTML('MarvinBlank', MarvinBlankPainted ? '' : MarvinBlankOut);
		MarvinBlankPainted = !MarvinBlankPainted;
	} // paintMarvinBlank()

	function ipMsg(courseNum) {
		var bld = new String.builder();
		<% for (int crsNum = 1; crsNum <= courses.length; crsNum++) { 
			final Course course = courses[crsNum - 1]; 
			final String[] allowedIPs = 
					coursesIPs.get(Integer.valueOf(course.getId()));
			if (!Utils.isEmpty(allowedIPs)) {  
				final boolean isInstructorOrTA = 
						isInstructor || taStatuses[crsNum - 1]; 
		%>
				if (courseNum === <%= crsNum %>) {
					bld.append('<%= user.translateJS(Utils.toString(
							isInstructorOrTA ? "Students" : "You", 
							" may enter this course only if ", 
							isInstructorOrTA ? "they" : "you", 
							" are using a computer whose IP address "
								+ "matches one of the following sequences: ")) %>\n');
		<% 			for (final String allowedIP : allowedIPs) { 
		%>
						bld.append('\t<%= Utils.toValidJS(allowedIP) %>\n');
		<% 			} // for each allowed IP address in this course
					if (!isInstructorOrTA) { 
		%>
						bld.append('<%= user.translateJS(
								"Contact your instructor if you believe ACE "
								+ "is denying you access to this course "
								+ "erroneously.") %>');
		<% 			} // if a student 
		%>
				} // if this is the selected course
		<% 	} // if course restricts IPs
		} // for each course 
		%>
		toAlert(bld.toString());
	} // ipMsg()

	// -->
</script>
</head>

<body style="text-align:center; margin:0px;"<%= onload %>>
<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
<div id="contentsWithoutTabs">
<% if (tutorialCourse != null
		&& !Utils.isEmpty(tutorialCourse.getNotes())) { %>
	<table class="boldtext" style="width:70%; padding:10px; 
			margin-left:auto; margin-right:auto; 
			border-style:solid; border-width:1px; border-color:#49521B; 
			background-color:#f6f7ed;">
		<tr><td class="boldtext big" style="color:red;">
			<%= Utils.isEmpty(userLangs) ? "IMPORTANT USER NOTICE!!"
					: user.translate(
						"IMPORTANT USER NOTICE!! (in English)") %>
		</td></tr>
		<tr><td>
			<table class="whiteTable"
					style="padding-left:10px; padding-right:10px;">
			<tr><td style="padding-top:10px; padding-bottom:10px;">
				<%= tutorialCourse.getNotes() %>
			</td></tr>
			</table>
		</td></tr>
	</table>
	<br /><br />
<% } // if there's a system notice 
final int colspan = (isInstructor ? 5 : 2);
%>
<table class="regtext" style="width:90%; margin-left:auto; 
		margin-right:auto; border-style:none; border-collapse:collapse;">
	<tr><td class="boldtext big" style="padding-bottom:10px;" colspan="3">
		<%= user.translate("My Courses") %>
	</td></tr>
	<tr>
	<td class="boldtext enlarged" style="border-bottom-style:solid;
			text-align:center; border-width:1px; border-color:#49521B;">
		<%= user.translate("No.") %>
	</td> 
	<td class="boldtext enlarged" style="border-bottom-style:solid;
			border-width:1px; width:<%= isInstructor ? 60 : 95 %>%; 
			border-color:#49521B; padding-left:10px;" 
			colspan="<%= isInstructor ? 1 : 3 %>">
		<% if (courses.length > 0) { %>
			<%= user.translate("Course") %>
		<% } // if there are courses %>
	</td> 
	<% if (isInstructor && user.isEnabled() && courses.length > 0) { %>
		<td class="boldtext enlarged" style="border-bottom-style:solid;
				border-width:1px; border-color:#49521B; text-align:center;
				padding-right:10px;">
			<%= user.translate("Edit") %>
		</td>
		<td class="boldtext enlarged" style="border-bottom-style:solid;
				border-width:1px; border-color:#49521B; text-align:center;
				padding-right:10px;">
			<%= user.translate("Clone") %>
		</td>
		<td class="boldtext enlarged" style="border-bottom-style:solid;
				border-width:1px; border-color:#49521B; text-align:center;
				padding-right:10px;">
			<%= user.translate("Delete") %>
		</td>
		<td class="boldtext enlarged" style="border-bottom-style:solid;
				border-width:1px; border-color:#49521B; text-align:center;">
			<%= user.translate("Manage coinstructors") %>
		</td>
	<% } // if enabled instructor and there are courses %>
	</tr>
	<% if (isInstructor && !user.isEnabled()) { %>
		<tr class="greenrow" style="border-left:solid; border-right:solid; 
					border-width:1px; border-color:#49521B;">
		<td colspan="<%= colspan %>" style="padding-top:10px; padding-bottom:10px; 
				border-left:solid; border-right:solid; border-width:1px; 
				border-color:#49521B; padding-left:10px; padding-right:10px;">
			<b><%=user.translate(
					"Your identity has not yet been verified.") %></b><br />
			<br />
			<%= user.translate("If you have not done so, please email "
						+ "***John Doe*** for full instructor status.  Include "
						+ "your name and email address so that ***John*** "
						+ "can confirm your identity.",
					new String[] {Utils.toString("<a href=\"mailto:", 
							AppConfig.verifierEmail, 
							"?subject=ACE instructor verification\">", 
							AppConfig.verifierName, "</a>"),
						AppConfig.verifierName}) %>
		</td>
		</tr>
	<% } // instructor not verified %>
	<% if (isInstructor && courses.length == 0 && user.isEnabled()) { %>
		<tr class="greenrow" style="border-left:solid; border-right:solid; 
					border-width:1px; border-color:#49521B;">
		<td colspan="<%= colspan %>" style="padding-top:10px; padding-bottom:10px; 
				border-left:solid; border-right:solid; border-width:1px; 
				border-color:#49521B; padding-left:10px; padding-right:10px;">
			<b><%= user.translate("You have not created any courses.") %></b>
			<br /><br />
			<%= user.translate("To create a course, press <b>Add Course</b>.") %>
		</td>
	</tr>
	<% } // no courses created %>
	<% if (user.isEnabled() || !isInstructor) { 
		boolean rowToggle = false;
		int visibleCourseNum = 0;
		int tutorialVisNum = 0;
		for (int crsNum = 1; crsNum <= courses.length; crsNum++) { 
			final Course course = courses[crsNum - 1];
			if (course.hide()) {
				coursesHidden = true;
				continue;
			} // if course is hidden
			visibleCourseNum++;
			final int courseId = course.getId();
			final boolean amCoinstructor = course.getAmCoinstructor(); 
			final boolean isTA = role == User.STUDENT 
					&& ((StudentSession) userSess).isTA(courseId);
			final boolean isEnabled = course.isEnabled();
			final boolean restrictIPs = !Utils.isEmpty(
					coursesIPs.get(Integer.valueOf(courseId)));
			final boolean allowedEntry = isInstructor || isTA 
					|| (isEnabled && course.isOkIPAddress(ipAddr, user));
			final boolean isTutCrs = courseId == tutorialCourse.getId();
			final boolean isTutCrsOwner =
					user.getUserId().equals(tutorialCourse.getOwnerId());
			rowToggle = !rowToggle;
			final String rowColor = (rowToggle ? "whiterow" : "greenrow"); %>
			<tr class="<%= rowColor %>">
				<td style="border-left-style:solid; border-width:1px; 
						text-align:center; border-color:#49521B;">
					<select name="courseNum<%= courseId %>"
							onchange="moveCourse(<%= courseId %>, <%= crsNum %>);">
					<% if (role != User.STUDENT
							&& courseId == AppConfig.tutorialId) { 
						tutorialVisNum = visibleCourseNum; %>
						<option>&nbsp;&nbsp;<%= visibleCourseNum %></option>
					<% } else {
						int visCrsNum = 0;
						for (int cNum = 1; cNum <= courses.length; cNum++) { 
							final Course crs = courses[cNum - 1];
							if (!crs.hide()) { 
								visCrsNum++; 
								if (role == User.STUDENT
										|| visCrsNum != tutorialVisNum) {
									final String visCrsNumStr = Utils.toString(
											visCrsNum < 10 ? "&nbsp;&nbsp;" : "", 
											visCrsNum); %>
									<option value="<%= cNum %>" <%=
											visCrsNum == visibleCourseNum
												? "selected=\"selected\"" : "" %>> 
										<%= visCrsNumStr %>
									</option>
							<% 	} // if not the tutorial course or a student
							} // if the course is visible
						} // for each course 
					} // if the tutorial course and not a student %>
					</select>
				</td>
				<td style="text-align:left; padding-left:10px; padding-right:10px;">
					<% if (allowedEntry) { %>
						<a href="javascript:selectCourse(<%= crsNum %>)"
								title="<%= user.translate("Course ID") %>: <%= 
									courseId %>"><%= 
							course.getName() %></a>
					<% } else { // student, and course disabled %>
						<%= course.getName() %>
					<% } // if student is allowed entry, or instructor or TA
					if (!isEnabled) { 
						final String enableDateStr = course.getEnableDateStr(); 
						if (enableDateStr == null) { %>
							(<span style="color:red;"><%= 
									user.translate("disabled indefinitely") 
									%></span>)
						<% } else { %>
							(<span style="color:red;"><%= 
									user.translate("disabled until") %> 
									<%= enableDateStr %></span>)
						<% } // if there is an enable date %>
					<% } // if course is disabled
					if (restrictIPs) { %> 
						(<a href="javascript:ipMsg(<%= crsNum %>);"><span 
							style="color:red;">IP-restricted</span></a>)
					<% } // if IP-restricted %>
				</td>
			<% if (isInstructor && (!isTutCrs || isTutCrsOwner)) { %>
				<td style="text-align:center; padding-right:10px;">
					<%= makeButtonIcon("edit", pathToRoot, 
						"editCourse(", crsNum, ");") %>
				</td>
				<td style="text-align:center; padding-right:10px;">
					<%= makeButtonIcon("duplicate", pathToRoot, 
						"cloneCourse(", crsNum, ");") %>
				</td>
				<td style="text-align:center; padding-right:10px;">
					<% if (!amCoinstructor) { %>
						<%= makeButtonIcon("delete", pathToRoot, 
							"deleteCourse(", crsNum, ");") %>
					<% } // if I'm not a coinstructor of this course %>
				</td>
				<td style="border-right-style:solid; border-width:1px; 
						border-color:#49521B; text-align:center;">
					<% if (!amCoinstructor) { %>
						<%= makeButtonIcon("view", pathToRoot, 
							"addCoinstructor(", courseId, ");") %>
					<% } // if I'm not a coinstructor of this course %>
				</td>
			<% } else { %>
				<td colspan="<%= isInstructor ? 6 : 1 %>" 
						style="border-right-style:solid; border-width:1px; 
						border-color:#49521B; text-align:center;">
				</td>
			<% } // if is instructor %>
			</tr>
		<% } // for each course crsNum
		if (courses.length <= 1 && !isInstructor) { %>
			<tr class="greenrow" style="border-left:solid; border-right:solid; 
					border-width:1px; border-color:#49521B;">
			<td colspan="<%= colspan %>" style="padding-top:10px; 
					padding-bottom:10px; border-left:solid; border-right:solid; 
					border-width:1px; border-color:#49521B; padding-left:10px; 
					padding-right:10px;">
				<%= user.translate(INSTRUCTOR_WILL) %>
			</td>
			</tr>
		<% } // student not enrolled in any courses 
	} // user is enabled, not isInstructor %>
	<tr>
	<td colspan="<%= colspan + 1 %>" style="border-top-style:solid; 
			border-width:1px; border-color:#49521B; width:100%; color:#FF0000;">
		<table style="width:100%;">
		<tr><td style="width:100%;"> &nbsp; </td>
		<% if (isInstructor && user.isEnabled()) { %>
			<td><%= makeButton(user.translate("Add course"), "addCourse();") %></td>
			<% if (coursesHidden) { %>
				<td><%= makeButton(user.translate("Reveal hidden"), 
						"revealCourse();") %></td>
			<% } // if any courses are hidden
		} // user is enabled, not isInstructor %>
		<td><%= makeButton(user.translate("Refresh"), "refresh();") %>
		</td></tr>
		</table>
	</td>
	</tr>
</table>
<br /><br />
<table class="regtext" style="width:70%;
		margin-left:auto; margin-right:auto; 
		border-style:none; border-collapse:collapse;">
	<tr><td class="boldtext big">
		<%= user.translate("Technical problems?") %>
	</td>
	</tr>
	<tr><td class="boldtext" style="border-style:solid; border-width:1px; 
			border-color:#49521B; background-color:#f6f7ed;">
		<table class="boldtext" style="padding-left:20px;">
			<% if (isInstructor) { %>
				<tr><td style="padding-top:10px;">
					<%= user.translate("If you have any problems or suggestions, " 
								+ "feel free to contact ***Product Support***. "
								+ "Your comments will help us make "
								+ "ACE Organic a better program.",
							"<a href=\"mailto:robert.grossman@uky.edu\">"
								+ "Dr. Grossman</a>") %>
				</td></tr>
			<% } // if is instructor %>
			<tr><td style="padding-top:10px;">
				<%= user.translate("Recommended browsers for ACE") %>:
				<ul>
					<li><%= user.translate("PC") %>: 
					<%= user.translate("Firefox or Chrome") %>.
					</li><li><%= user.translate("Mac") %>: 
					<%= user.translate("Safari or Chrome") %>. 
				</li></ul>
				<b><%= user.translate("Whatever browser you use, ACE requires "
						+ "you to enable Javascript and to turn "
						+ "off pop-up blockers.") %></b>
				<b><%= user.translate("Run the ***browser checkup***"
							+ " before you use a new computer with ACE.",
						Utils.toString("<a href='browsercheckup/browserCheck.jsp",
							Utils.isEmpty(userLangs) ? ""
								: Utils.toString("?language=",
									Utils.toValidURI(userLangs[0])),
							"' title='Browser Checkup'>", 
							user.translate("browser checkup"), "</a>")) %></b>
			</td></tr>
			<tr><td style="padding-top:10px;">
				<%= user.translate("Many technical problems can be solved "
						+ "if you QUIT your browser, restart, EMPTY YOUR "
						+ "CACHE, and then log in again. This procedure "
						+ "will solve your problem 90% of the time. "
						+ "To empty your cache:") %> 
				<ul>
				</li><li><%= Utils.toString(user.translate("Chrome"), ": ", 
						user.translate("Go to Chrome &rarr; Clear Browsing Data....")) %> 
				<li><%= Utils.toString(user.translate("Firefox"), ": ", 
						user.translate("Go to Firefox &rarr; Preferences... "
							+ "&rarr; Privacy &amp; Security &rarr; Clear Data....")) %> 
				</li><li><%= Utils.toString(user.translate("Safari"), ": ", 
						user.translate("Go to Safari &rarr; Clear History....")) %> 
				</li></ul>
			</td></tr>
			<tr><td style="padding-top:10px;">
				<%= user.translate("Other technical problems") %>:
				<ul>
					<li><a href="javascript:noSubmit()"><%= 
					user.translate("If the Submit button does not appear") 
							%></a>: <span id="noSubmit"></span>
					</li><li><a href="javascript:paintSeemsToHang()"><%= 
					user.translate("If your browser seems to hang") 
							%></a>: <span id="seemsToHang"></span>
					</li><li><a href="javascript:paintHangsAfterOneResponse()"><%= 
					user.translate("If you find that your browser hangs after "
							+ "you submit a single response") 
							%></a>: <span id="hangsAfterOneResponse"></span> 
					</li><li><a href="javascript:paintMarvinBlank()"><%= 
					user.translate("If you find that when you submit a "
							+ "response, the Marvin window turns blank and "
							+ "ACE returns no feedback") 
							%></a>: <span id="MarvinBlank"></span>
				</li></ul>
			</td></tr>
			<% if (!isInstructor) { %> 
				<tr><td>
					<br/><%= user.translate("If you have other problems, "
							+ "or if you receive feedback that you believe "
							+ "to be inappropriate to your response, "
							+ "contact your instructor.") %>
				</td></tr>
			<% } %>
		</table>
	</td>
	</tr>
</table>
<br /><br />
</div>
</body>
</html>
