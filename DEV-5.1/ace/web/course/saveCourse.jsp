<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.Course,
	com.epoch.utils.DateUtils,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.text.ParseException,
	java.util.Date,
	java.util.TimeZone"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%	
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	// form the course object
	int index = MathUtils.parseInt(request.getParameter("index"));
	final int oldCrsNum = index;
	final boolean clone = "true".equals(request.getParameter("clone"));
	final boolean changePassword =
			"on".equals(request.getParameter("changePassword"));
	/* Utils.alwaysPrint("saveCourse.jsp: index = ", index, ", clone = ", clone,
			", changePassword = ", changePassword); /* */
	final String doneAction = (clone 
			? Utils.toString("cloneHWSets.jsp?oldCrsNum=", index) 
			: Utils.toString(pathToRoot, "userHome.jsp"));
	String msg = null;

	Course course = null;
	if (index != 0 && !clone) {
		if (userSess instanceof AdminSession) {
			course = ((AdminSession) userSess).getCourse(index);
		} else if (userSess instanceof InstructorSession) {
			course = ((InstructorSession) userSess).getCourse(index);
		}
	} else {
		course = new Course();
	} // if editing an existing course
	/* Utils.alwaysPrint("saveCourse.jsp: modifying course with index ", index, 
			" and id ", course.getId()); /* */
	course.setOwnerId(user.getUserId());
	course.setName(Utils.inputToCERs(request.getParameter("name")));
	course.setBook(Utils.inputToCERs(request.getParameter("book")));
	course.setHomePage(Utils.inputToCERs(request.getParameter("homepage")));
	course.setDescription(Utils.inputToCERs(request.getParameter("description")));
	course.setACEBookId(MathUtils.parseInt(request.getParameter("ACEBookId")));
	course.setMaxExtensionsStr(request.getParameter("maxExtensions"));
	course.setHide("on".equals(request.getParameter("hide")));
	course.setIsExam("on".equals(request.getParameter("isExam")));
	final boolean hideSynthCalcdProds = 
			"on".equals(request.getParameter("hideSynthCalcdProds"));
	final boolean changeHideSynthCalcdProds = 
			course.hideSynthCalcdProds() != hideSynthCalcdProds;
	course.setHideSynthCalcdProds(hideSynthCalcdProds);
	course.setTAsMayGrade("on".equals(request.getParameter("tasMayGrade")));
	course.setNumDecimals(MathUtils.parseInt(request.getParameter("decimalDigits")));
	course.setForumEnabled("on".equals(request.getParameter("forumEnabled")));
	boolean initializeForum = "on".equals(request.getParameter("initializeForum"));
	course.setSortByStudentNum("on".equals(request.getParameter("sortByStudentNum")));
	final boolean restrictIPs = "on".equals(request.getParameter("restrictIPs"));
	String[] ipAddresses = new String[0];
	if (restrictIPs) {
		final String ipAddrStr = Utils.condenseWhitespace(
				request.getParameter("ipAddresses")).replace(' ', ':');
		if (!Utils.isEmpty(ipAddrStr)) ipAddresses = ipAddrStr.split(":");
	} // if restrictIPs
	final TimeZone tz = TimeZone.getTimeZone(request.getParameter("zone"));
	course.setTimeZone(tz);
	final String enableDateStr = request.getParameter("enableDate");
	final boolean neverEnable = "on".equals(request.getParameter("neverEnable"));
	Date enableDate = (enableDateStr == null ? null
			: DateUtils.parseStringNoTimeZone(enableDateStr, tz));
	if (enableDate == null && !neverEnable) {
		msg = Utils.toString("Could not parse date ", enableDateStr,
				"; setting enable date to now.");
		enableDate = new Date(System.currentTimeMillis());
	} // if date was parsed
	course.setEnableDate(enableDate);
	/* Utils.alwaysPrint("saveCourse.jsp: enableDateStr = ", enableDateStr,
			", getEnableDateStr() = ", course.getEnableDateStr(),
			", isEnabled() = ", course.isEnabled(), ", is never enabled = ",
			course.getEnableDate() == null); /**/

	if (index == 0 || clone) {
		if (userSess instanceof AdminSession) {
			final AdminSession adminSess = (AdminSession) userSess;
			adminSess.addCourse(course, ipAddresses);
			index = adminSess.getNumCourses(); 
			if (clone) { 
				adminSess.selectCourse(index); // use 1-based index
				/* Utils.alwaysPrint("saveCourse.jsp: selected course ", index, 
						" with id ", adminSess.getSelectedCourse().getId()); /**/
			} // if clone
		} else if (userSess instanceof InstructorSession) {
			final InstructorSession instrSess = (InstructorSession) userSess;
			instrSess.addCourse(course, ipAddresses);
			index = instrSess.getNumCourses(); 
			if (clone) { 
				instrSess.selectCourse(index); // use 1-based index
				/* Utils.alwaysPrint("saveCourse.jsp: selected course ", index,
						" with id ", instrSess.getSelectedCourse().getId()); /**/
			} // if clone
		} // if admin or instructor
	} else {
		if (userSess instanceof AdminSession) {
			((AdminSession) userSess).setCourse(course, ipAddresses);
		} else if (userSess instanceof InstructorSession) {
			((InstructorSession) userSess).setCourse(course, ipAddresses);
		} // if admin or instructor
		if (changeHideSynthCalcdProds && !course.hideSynthCalcdProds()) {
			course.letUsersSeeSynthCalcdProds();
		} // if should let users see calculated synthesis products
	} // if new or cloned
	if (course.forumEnabled() && initializeForum && !course.hasForumTopics()) {
		/* Utils.alwaysPrint("saveCourse.jsp: initializing forum "
				+ "of course with id ", course.getId()); /**/
		if (userSess instanceof AdminSession) {
			((AdminSession) userSess).initializeForum(course.getId());
		} else if (userSess instanceof InstructorSession) {
			((InstructorSession) userSess).initializeForum(course.getId());
		} // if admin or instructor
	} // if should initialize forum topics

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
	<title>ACE Course Management</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<style type="text/css">
		* html body {
			padding:50px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/md5.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

	function alertMe() {
		<% if (msg != null) { %>
			alert('<%= Utils.toValidJS(msg) %>');
		<% } // if there's a message %>
	} // alertMe()

	function setChangePasswd() {
		var deletePasswordCell = document.getElementById('deletePassword');
		if (!deletePasswordCell || !deletePasswordCell.checked) {
			setInnerHTML('pwdCell1', 
					'<%= user.translateJS("Enter course password") %>:');
			setInnerHTML('pwdCell2', 
					'<input type="password" id="passwordEntry" '
					+ 'name="passwordEntry" size="20" value="" />');
			setInnerHTML('pwdCell3', 
					'<%= user.translateJS("Confirm course password") %>:');
			setInnerHTML('pwdCell4', 
					'<input type="password" id="passwordConfirm" '
					+ 'name="passwordConfirm" size="20" value="" />');
		} else if (deletePasswordCell) {
			for (var num = 1; num <= 4; num++) { // <!-- >
				clearInnerHTML('pwdCell' + num);
			}
		}
	} // setSavePasswd()

	function submitIt() {
		var form = document.pwdForm;
		if (form.passwordEntry) {
			if (form.passwordEntry.value !== form.passwordConfirm.value) { 
				toAlert('<%= user.translateJS(
						"The passwords you entered don't match.") %>');
				return;
			} // if the passwords don't match
			form.password.value = b64_md5(form.passwordEntry.value);
		} // if there's a password to store
		form.submit();
	} // submitIt()

	function done() {
		this.location.href = '<%= doneAction %>';
	}
	// -->
	</script>
</head>
<body class="light" style="background-color:white;" onload="setChangePasswd();">
<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
<div id="contentsWithoutTabs">
	<% if (changePassword) { %>
		<script type="text/javascript">
			// <!-- >
			alertMe();
			// -->
		</script>
		<form name="pwdForm" action="saveCoursePassword.jsp" method="post">
			<input type="hidden" name="index" value="<%= index %>" />
			<input type="hidden" name="clone" value="<%= clone %>" />
			<input type="hidden" name="password" value=""/>
		<table style="margin-left:auto; margin-right:auto;" summary="">
		<tr>
		<td colspan="2" class="boldtext big" style="vertical-align:top; padding-top:10px;">
			<%= user.translate("Edit course password") %>
		</td>
		</tr>
		<% if (course.hasPassword()) { %>
			<tr>
			<td class="regtext">
				<%= user.translate("Delete password?") %>
			</td>
			<td>
				<input type="checkbox" name="deletePassword" id="deletePassword"
						onchange="setChangePasswd();" />
			</td>
			</tr>
		<% } // if course has password %>
		<tr>
		<td id="pwdCell1" class="regtext" 
				style="padding-left:40px; vertical-align:middle">
		</td>
		<td id="pwdCell2">
		</td>
		</tr>
		<tr>
		<td id="pwdCell3" class="regtext" 
				style="padding-left:40px; vertical-align:middle">
		</td>
		<td id="pwdCell4">
		</td>
		</tr>
		<tr>
		<td colspan="2" style="padding-bottom:10px; padding-top:10px;">
			<table><tr>
			<td>
				<%= makeButton(user.translate("Save changes"), "submitIt();") %>
			</td>
			<td>
				<%= makeButton(user.translate("Cancel"), "done();") %>
			</td>
			</tr></table>
		</td>
		</tr>
		</table>
		</form>
	<% } else if (clone) { %>
		<form name="addDaysForm" action="cloneHWSets.jsp" method="post">
			<input type="hidden" name="oldCrsNum" value="<%= oldCrsNum %>" />
			<input type="hidden" name="index" value="<%= index %>" />
		<table class="regtext" style="margin-left:auto; margin-right:auto;" summary="">
		<tr>
		<td><%= user.translate("Number of days to add to due dates of assignments") %>:
		</td><td>
			<input type="text" name="addDays" size="5" value="364" />
		</td>
		</tr>
		<tr>
		<td><%= user.translate("Make assignments invisible?") %>
		</td><td>
			<input type="checkbox" name="makeInvisible" checked="checked" />
		</td>
		</tr>
		<tr>
		<td colspan="2" style="padding-bottom:10px; padding-top:10px;">
			<table><tr>
			<td>
				<%= makeButton(user.translate("Submit"), "document.addDaysForm.submit();") %>
			</td>
			</tr></table>
		</td>
		</tr>
		</table>
		</form>
	<% } else { %>
		<script type="text/javascript">
			// <!-- >
			done();
			// -->
		</script>
	<% } // if should change the password %>
</div>
</body>
</html>
