<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.Course,
	com.epoch.session.AnonSession,
	com.epoch.textbooks.Textbook,
	com.epoch.utils.DateUtils,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.Date,
	java.util.HashMap,
	java.util.List,
	java.util.Map,
	java.util.TimeZone"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%	
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final int index = MathUtils.parseInt(request.getParameter("index"));
	final boolean clone = "true".equals(request.getParameter("clone"));
	final int EDIT = 0;
	final int ADD = 1;
	final int CLONE = 2;
	int mode = EDIT;
	final String SELECTED = "selected=\"selected\" ";
	final String CHECKED = "checked=\"checked\" ";
 
	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noAccess.html" /> <%
	}
	final InstructorSession instrSess = (InstructorSession) userSess;
	Course course = null;
	if (index != 0) {
		instrSess.selectCourse(index);
 		course = instrSess.getSelectedCourse(); 
		if (clone) {
			course = new Course(0, course);
			mode = CLONE;
		}
	} else {
		course = new Course();
		mode = ADD;
	}
	final String[] ipAddrs = course.getAllowedIPAddrs();

	String saveButtonStr = "Save changes";
	String header = "Edit course"; 
	if (mode == ADD) {
		saveButtonStr = "Add course";
		header = "Add new course";
		course.setBook("");
	} else if (mode == CLONE) {
		saveButtonStr = "Clone course";
		header = "Clone course";
	}
	// final String fullURL = Utils.getFullURL(request);

	final String enableDateStr = course.getEnableDateStr();
	final boolean neverEnable = enableDateStr == null;
	String studentNumLabel = user.getInstitutionStudentNumLabel();
	final boolean multiinstitution = instrSess.courseIsMultiinstitution()
			|| instrSess.studentsAreMultiinstitution();
	if (multiinstitution) studentNumLabel = 
			Utils.toString(studentNumLabel, ' ', 
				user.translate("(or other institution's student ID number)"));

	final Textbook[] books = instrSess.getTextbooks(true);
	final Map<String, User> ownersByIds = new HashMap<String, User>();
	for (final Textbook book : books) {
		final String ownerId = book.getOwnerId();
		if (ownerId != Textbook.MASTER_AUTHOR) {
			User owner = ownersByIds.get(ownerId);
			if (owner == null) {
				owner = AnonSession.getUser(ownerId);
				ownersByIds.put(ownerId, owner);
			} // if haven't stored owner yet
		} // if there's an owner
	} // for each book
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
	<title>ACE Course Management</title>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<style type="text/css">
		* html body {
			padding:50px 0 0px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	
	function goBackAgain() {
		self.location.href = '<%= pathToRoot %>userHome.jsp';
	} // goBackAgain()
	
	function toggleExam() {
		var isExam = document.editform.isExam.checked;
		if (isExam) {
			if (!document.editform.restrictIPs 
					|| getInnerHTML('restrictIPsCell1') === '') {
				setInnerHTML('restrictIPsCell1', 
						'<%= user.translateJS("Restrict IP addresses?") %>');
				setInnerHTML('restrictIPsCell2', '<input type="checkbox" '
						+ 'name="restrictIPs" <%= Utils.isEmpty(ipAddrs) 
							? "" : CHECKED %> onchange="toggleExam()" \/>');
			} // if need to paint restrict IP options
			var restrictIPs = document.editform.restrictIPs.checked;
			if (restrictIPs) {
				setInnerHTML('allowedIPs', '<%= user.translateJS(
							"Enter colon-separated list of IP addresses "
							+ "that students may use.  You may enter "
							+ "a three-number domain to cover all IP "
							+ "addresses in that domain, or you may use subnet "
							+ "mask or CIDR notation to cover a range of IP "
							+ "addresses.") %><br \/>'
						+ '<textarea name="ipAddresses" cols="50" rows="1">'
						+ '<%= Utils.toValidJS(Utils.join(ipAddrs, ":")) %><\/textarea>');
			} else {
				clearInnerHTML('allowedIPs');
			} // if should restrict IPs
			hideSynthCalcdProdsChanged();
		} else {
			clearInnerHTML('restrictIPsCell1');
			clearInnerHTML('restrictIPsCell2');
			clearInnerHTML('allowedIPs');
		} // if we have an exam
	} // toggleExam()

	function hideSynthCalcdProdsChanged() {
		var hideBox = document.editform.hideSynthCalcdProds;
		if (hideBox) {
			setInnerHTML('restrictSynthCalcdProdsCell3', hideBox.checked
					? '<%= user.translateJS("Remember to uncheck this box "
						+ "after the exam is over to allow students to see "
						+ "calculated synthesis products again.") %>'
					: '<%= user.translateJS("If you check this box, then, "
						+ "after a student enters <i>this</i> course, ACE will "
						+ "not show him or her calculated products in the "
						+ "feedback to synthesis questions in <i>any</i> course.") %>'
					);
		} // if hideBox
	} // hideSynthCalcdProdsChanged()

	function changeEnabled() {
		var neverEnable = document.editform.neverEnable.checked;
		var bld = new String.builder();
		if (!neverEnable) {
			bld.append('<input type="text" name="enableDate" size="25" '
					+ 'value="<%= neverEnable 
						? DateUtils.getStringNoTimeZone(
							new Date(System.currentTimeMillis())) 
						: Utils.toValidJS(enableDateStr) 
							%>"\/>&nbsp;&nbsp;&nbsp;'); 
		} // if has an enabling date
		bld.append(neverEnable 
					? '<%= user.translateJS("Entry barred indefinitely") %>'
					: '<%= user.translateJS(
						"or check to bar entry indefinitely:") %>').
				append('&nbsp;&nbsp;&nbsp;<input type="checkbox" '
					+ 'name="neverEnable" onchange="changeEnabled();"');
		if (neverEnable) bld.append(' <%= Utils.toValidJS(CHECKED) %>');
		bld.append('\/>');
		setInnerHTML('enabledCell', bld.toString());
	} // changeEnabled()

	function changeForumEnabled() {
		var forumEnabled = document.editform.forumEnabled.checked;
		if (forumEnabled) {
			setInnerHTML('initializeForumCell', '<%= user.translate(
					"Initialize forum with posts on using forum?")
					%>&nbsp;&nbsp;&nbsp;'
					+ '<input type="checkbox" name="initializeForum" \/>');
		} else {
			clearInnerHTML('initializeForumCell');
		} // if the forum is enabled
	} // changeForumEnabled()

	function submitIt() {
		var form = document.editform;
		if (isWhiteSpace(form.name.value)) {	
			toAlert('<%= user.translateJS("Please enter a name.") %>');
			return;
		}
		if (form.book.value.indexOf('rganic') >= 0) {
			toAlert('<%= user.translateJS("Please enter only the textbook author's "
					+ "surname; omit the title of the textbook.") %>');
			return;
		}
		if (!canParseToFloat(form.maxExtensions.value)
				|| parseToFloat(form.maxExtensions.value) < 0) { // <!-- >
			toAlert('<%= user.translateJS("Please enter a nonnegative number "
					+ "for the maximum length of extensions students may "
					+ "self-assign.") %>');
			return;
		}
		form.submit();
	} // submitIt()

	// -->

	</script>
</head>
<body class="light" style="background-color:white;" 
		onload="toggleExam(); hideSynthCalcdProdsChanged(); changeForumEnabled();">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<div id="contentsWithoutTabs">

	<form name="editform" action="saveCourse.jsp" method="post"
			accept-charset="UTF-8">
		<input type="hidden" name="index" value="<%= index %>" />
		<input type="hidden" name="clone" value="<%= clone %>" />
	<table style="width:700px; text-align:center; margin-left:auto;
			margin-right:auto;" summary="">
		<tr>
		<td class="boldtext big" style="vertical-align:top; padding-top:10px;">
			<%= user.translate(header) %>
		</td>
		</tr>
		<% if (clone) { %>
			<tr>
			<td class="regtext" style="vertical-align:top; 
					padding-top:10px; padding-bottom:10px;">
				<%= user.translate("ACE will clone all of the assignments of the "
						+ "original course. ACE will not copy any student "
						+ "records from the original course.") %>
			</td>
			</tr>
		<% } // if clone %>
		<tr>
		<td class="whiteTable" style="background-color:#f6f7ed;
				vertical-align:top; text-align:center; width:100%;">
			<table style="width:90%; margin-left:auto; margin-right:auto; 
					text-align:left;" summary="">
				<tr>
				<td class="regtext" style="width:40%; padding-top:10px;">
					<%= user.translate("Course name") %>:
				</td>
				<td colspan="2" style="padding-top:10px;">
					<input type="text" name="name" size="40"
						value="<%= Utils.toValidTextbox(course.getName()) %>"/>
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<%= user.translate("Course textbook author") 
							+ " (" + user.translate("last name only") + "):" %>	
				</td>
				<td colspan="2" class="regtext" style="padding-top:10px;">
					<input type="text" name="book" size="40" 
						value="<%= Utils.toValidTextbox(course.getBook()) %>"/>
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<%= user.translate("ACE online textbook") %>:
				</td>
				<td colspan="2" class="regtext" 
						style="text-align:left; padding-bottom:10px;">
					<select name="ACEBookId">
						<option value="0">
							<%= user.translate("None") %>
						</option>
						<% for (final Textbook book : books) { 
							final int bookId = book.getId(); %>
							<option value="<%= bookId %>" <%=
									bookId == course.getACEBookId() ? SELECTED : "" %>>
								<%= book.getName() %><% 
								final String ownerId = book.getOwnerId();
								if (ownerId != Textbook.MASTER_AUTHOR) { 
									%>: <%= ownersByIds.get(ownerId).getName().toString() %>
								<% } // if book is owned %>
							</option>
						<% } // for each book %>
					</select>
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<%= user.translate("Course time zone") %>:
				</td>
				<td colspan="2" style="padding-top:10px;">
					<select name="zone" size="1">
					<% 
					final String[] zones = DateUtils.outputTimeZones(); 
					for (final String zone : zones) { 
						final TimeZone tz = TimeZone.getTimeZone(zone); %> 
						<option value="<%= zone %>" 
								<%= tz.equals(course.getTimeZone()) ?
									SELECTED : "" %>><%= zone %></option>
					<% } // for each time zone %>
					</select>
				</td>
				</tr>
				<tr>
				<td class="regtext" style="vertical-align:top;">
					<%= user.translate("Description") %>:
				</td>
				<td colspan="2" style="width:100%;">
					<textarea class="textArea" name="description" cols="50" rows="5"><%= 
							Utils.toValidTextbox(course.getDescription()) %></textarea>
				</td>
				</tr>
				<tr>
				<td class="regtext">
					<%= user.translate("Course home page") %>:
				</td>
				<td colspan="2">
					<% if ("".equals(course.getHomePage()))  { %>
						<input type="text" name="homepage" size="60"
							value="http://"/>
					<% } else { %>
						<input type="text" name="homepage" size="60" 
							value="<%= Utils.toValidTextbox(course.getHomePage()) %>"/>
					<% } %>
				</td>
				</tr>
				<tr>
				<td class="regtext">
					<%= user.translate("Hide course?") %>
				</td>
				<td>
					<input type="checkbox" name="hide" 
						<%= course.hide() ? CHECKED : "" %>/>
				</td>
				<td>
					<%= user.translate("Hide a course if you don't want it "
							+ "to appear in your or your students' list of "
							+ "courses, but you don't want to delete it, "
							+ "either. You may unhide it later.") %>
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<%= user.translate("When students may enter course:") %>
				</td>
				<td colspan="2" style="width:25%; padding-top:10px;" id="enabledCell">
					<% if (!neverEnable) { %>
						<input type="text" name="enableDate" size="25" 
								value="<%= enableDateStr %>"/>&nbsp;&nbsp;&nbsp;<%= 
								user.translate("or check to bar entry indefinitely:") %>
					<% } else { %>
						<%= user.translate("Entry barred indefinitely") %>
					<% } // if has an enabling date %>
					&nbsp;&nbsp;&nbsp;
					<input type="checkbox" name="neverEnable" 
							onchange="changeEnabled();" 
						<%= neverEnable ? CHECKED : "" %>/>
				</td>
				</tr>
				<tr>
				<td class="regtext" style="padding-top:10px;">
					<%= user.translate("Enable discussion forum?") %>
				</td>
				<td style="width:25%; padding-top:10px;">
					<input type="checkbox" name="forumEnabled" 
						onchange="changeForumEnabled();"
						<%= course.forumEnabled() ? CHECKED : "" %>/>
				</td>
				<td class="regtext" style="padding-top:10px;" id="initializeForumCell">
					<%= user.translate("Initialize forum with posts on using forum?") %>
					<span style="vertical-align:top;">&nbsp;&nbsp;&nbsp;<input 
							type="checkbox" name="initializeForum" /></span>
				</td>
				</tr>
				<tr>
				<td class="regtext">
					<%= user.translate("Maximum total extensions students may "
							+ "self-assign (instructors may override):") %>
				</td>
				<td style="width:25%;">
					<input type="text" name="maxExtensions" size="5" 
							value="<%= course.getMaxExtensionsStr() %>"/>
							<%= user.translate("days") %>
				</td>
				</tr>
				<tr>
				<td class="regtext">
					<%= user.translate("Number of digits past decimal point "
							+ "to display in gradebook:") %>
				</td>
				<td style="width:25%;">
					<select name="decimalDigits"> 
						<option value="0" <%= course.getNumDecimals() == 0 
								? SELECTED : "" %>>0</option>
						<option value="1" <%= course.getNumDecimals() == 1 
								? SELECTED : "" %>>1</option>
						<option value="2" <%= course.getNumDecimals() == 2 
								? SELECTED : "" %>>2</option>
					</select>
				</td>
				</tr>
				<tr>
				<td class="regtext">
					<%= user.translate("Allow TAs to alter students' grades?") %>
				</td>
				<td style="width:25%;">
					<input type="checkbox" name="tasMayGrade" 
						<%= course.tasMayGrade() ? CHECKED : "" %>/>
				</td>
				</tr>
				<tr>
				<td class="regtext">
					<%= user.translate("Sort students by ***student ID number***?",
							studentNumLabel) %>
				</td>
				<td style="width:25%;">
					<input type="checkbox" name="sortByStudentNum" 
						<%= course.sortByStudentNum() ? CHECKED : "" %>/>
				</td>
				</tr>
				<tr>
				<td class="regtext">
					<%= user.translate("Use course for exams?") %>
				</td>
				<td style="width:25%;">
					<input type="checkbox" name="isExam" onchange="toggleExam();"
						<%= course.isExam() ? CHECKED : "" %>/>
				</td>
				</tr>
				<tr>
				<td id="restrictIPsCell1" class="regtext">
				</td>
				<td id="restrictIPsCell2">
				</td>
				<td style="padding-right:10px;">
				</td>
				</tr>
				<tr>
				<td id="allowedIPs" colspan="3" style="padding-top:10px;">
				</td>
				</tr>
				<tr>
				<td id="restrictSynthCalcdProdsCell1" class="regtext">
					<%= user.translateJS("Hide calculated synthesis products?") %>
				</td>
				<td id="restrictSynthCalcdProdsCell2">
					<input type="checkbox" name="hideSynthCalcdProds" 
							onchange="hideSynthCalcdProdsChanged();" 
							<%= course.hideSynthCalcdProds() ? CHECKED : "" %> />
				</td>
				<td id="restrictSynthCalcdProdsCell3" style="padding-right:10px;">
				</td>
				</tr>
				<tr>
				<td class="regtext">
					<%= user.translate(course.hasPassword()
							? "Change or remove course password?"
							: "Require password to enter course?") %>
				</td>
				<td style="width:25%;">
					<input type="checkbox" name="changePassword" />
				</td>
				</tr>
				<tr>
				<td style="padding-bottom:10px; padding-top:10px;" colspan="2">
					<table><tr>
					<td>
						<%= makeButton(user.translate(saveButtonStr),
								"submitIt();") %>
					</td>
					<td>
						<%= makeButton(user.translate("Cancel"), "goBackAgain();") %>
					</td>
					</tr></table>
				</td>
				</tr>
			</table>
		</td>
		</tr>
	</table>

	</form>
	</div>
</body>
</html>
