<%@ page language="java" %>
<%@ page import="
	com.epoch.courseware.EnrollmentData,
	com.epoch.utils.DateUtils,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.List,
	java.util.Map,
	java.util.TimeZone"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	final String pathToRoot = "../";
	if (!Utils.among(role, User.ADMINISTRATOR, User.INSTRUCTOR)) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}

	final TimeZone zone = course.getTimeZone();
	final Assgt firstDescr = assgts[0];
	EnrollmentData[] students = null;
	switch (role) {
		case User.ADMINISTRATOR:
			final AdminSession adminSess = (AdminSession) userSess;
			students = (course.getId() != AppConfig.tutorialId
					? adminSess.getEnrolledStudents()
					: new EnrollmentData[0]);
			break;
		case User.INSTRUCTOR:
		default: // shouldn't happen
			final InstructorSession instrSess = (InstructorSession) userSess;
			students = (course.getId() != AppConfig.tutorialId
					? instrSess.getEnrolledStudents()
					: new EnrollmentData[0]);
			break;
	} // switch role
	
	final Map<String, String> extensions = firstDescr.getExtensions();
	final List<String> studentIds = new ArrayList<String>();
	if (!Utils.isEmpty(extensions)) studentIds.addAll(extensions.keySet());
	final String extensionsStr = firstDescr.extensionsToString();

	// makes it easier to find a name of a student from his id
	final List<String> userIds = new ArrayList<String>();
	int firstUnusedExamStudentNum = -1;
	for (final EnrollmentData student : students) {
		userIds.add(student.isRegistered()  && !student.isTA()
				? student.getUserId() : "");
		if (student.isUnusedExamStudent() && firstUnusedExamStudentNum < 0) {
			firstUnusedExamStudentNum = userIds.size() - 1;
		} // if student is unused exam student and we don't already have an extension
	} // for each student
	final boolean haveUnusedExamStudents = firstUnusedExamStudentNum >= 0;
	
	// button titles and headers 
	// final String saveButtonStr = " Save changes ";
	final String header = " Edit all exam due times and extensions ";
	
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
	<title>ACE Exam Properties Editor</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
		* html body {
			padding:100px 0 50px 0; 
		}
	</style>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/md5.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/navigation/courseSidebarJS.jsp.h" %>
	
	function goBackAgain() {
		this.location.href = 'hwSetList.jsp';
	}

	var extensions = '<%= extensionsStr %>';
	var extensionsPainted = false;
	var extensionsArr = {
			<% for (final String studentId : studentIds) { %>
				'<%= studentId %>' : <%= extensions.get(studentId) %>,
			<% } // for each extension %>
				'' : 0
			};
	
	function submitThis() {
		// find out if grading should be based on # of tries or submission time
		var submit = true;
		var theAlert = '';
		var form = document.editform;
		// get extension grantees
		if (extensionsPainted) { // get values from current document
			var extValueBld = new String.builder();
			var extensionStr = '';
			var first = true;
			var unusedExamStudentExtension = '';
			<% for (int studNum = 0; studNum < students.length; studNum++) { 
				final EnrollmentData student = students[studNum];
				final boolean isUnusedExamStudent = student.isUnusedExamStudent();
				if (student.isRegistered() && !student.isTA()) { %>
					extensionStr = form.s<%= isUnusedExamStudent
							? firstUnusedExamStudentNum : studNum %>.value;
					if (!isEmpty(extensionStr) && submit) {
						if (first) first = false;
						else extValueBld.append('/');
						if (!canParseToFloat(extensionStr)) {
							theAlert = '<%= isUnusedExamStudent
									? user.translateJS("The extension for "
										+ "unused exam students is not a number.")
									: user.translateJS(
										"The extension for ***this student*** "
											+ "is not a number.",
										student.getName()) %>';
							submit = false;
						} else if (parseFloat(extensionStr) !== 0) {
							extValueBld.append('<%= Utils.toValidJS(
										student.getUserId()) %>/').
									append(extensionStr);
						} // if there's a table entry and a value
					} // if there's an entry
				<% } // for each non-TA who is registered
			} // for each student %>
			extensions = extValueBld.toString();
		} // if extensionsPainted; if not, value has already been stored
		if (extensions === '/') extensions = '';
		if (submit) {
			form.extensions.value = extensions;
			form.submit(); 
		} else alert(theAlert); 
	} // submitThis()

	var numExtensions = <%= Utils.getSize(extensions) %>;

	function paintExtensions() {
		<% final int lastStudent = students.length
				+ (haveUnusedExamStudents ? 1 : 0); %>
		if (extensionsPainted) { // contract list, show only students with extensions
			var extListBld = new String.builder();
			var extValueBld = new String.builder();
			var first = true;
			var extensionStr;
			extensionsArr = new Array();
			numExtensions = 0;
			<% for (int rawStudNum = 0; rawStudNum < lastStudent; rawStudNum++) {
				final boolean atUnusedExamStudent = rawStudNum == students.length;
				final int studNum = (atUnusedExamStudent
						? firstUnusedExamStudentNum : rawStudNum);
				final EnrollmentData student = students[studNum];
				if (student.isRegistered() && !student.isTA() 
						&& (!student.isUnusedExamStudent() || atUnusedExamStudent)) { %>
					extensionStr = document.editform.s<%= studNum %>.value;
					if (!['', '0'].contains(extensionStr)) {
						if (first) first = false;
						else extValueBld.append('/');
						var userId ='<%= Utils.toValidJS(student.getUserId()) %>';
						extValueBld.append(userId).append('/').
								append(extensionStr);
						<% if (atUnusedExamStudent) { %>
							extListBld.append('<br\/>\n');
						<% } // if at first exam student %>
						if (extensionStr === '<%= Assgt.INDEFINITE %>')
							extListBld.append('<%= atUnusedExamStudent
									? user.translateJS("Unused exam students "
											+ "have an indefinite extension.") 
									: user.translateJS("***Student X*** has "
											+ "an indefinite extension.",
										student.getName()) %>');
						else {
							var phrase = '<%= atUnusedExamStudent
									? user.translateJS("Unused exam students "
											+ "have an extension of ***2 days***.",
										"@2 days@")
									: user.translateJS("***Student X*** has an "
											+ "extension of ***2 days***.",
										new String[] {student.getName(),
											"@2 days@"}) %>';
							extListBld.append(getExtensionText(phrase,
									numExtensions, parseFloat(extensionStr)));
						}
						extListBld.append('<br\/>\n');
						extensionsArr[userId] = extensionStr;
						numExtensions++;
					} // if there is an extension
				<% } // for each non-TA who is registered and is not an exam student
			} // for each student %>
			// alert(extListBld.toString());
			extensions = (extValueBld.length() > 1 ? extValueBld.toString() : '');
			if (!isEmpty(extensions))
				setInnerHTML('extensionsTable', extListBld.toString());
			else setInnerHTML('extensionsTable', '<%= user.translateJS( 
					"You have granted no extensions.") %>');
			clearInnerHTML('instructionsTable');
		} else { // put out list of students and current extensions
			var extensionsOut = new String.builder().
					append('<table><tr>'
						+ '<td class="regtext" style="text-align:center;">'
						+ '<b><%= user.translateJS("Student") %><\/b><\/td>'
						+ '<td class="regtext" style="text-align:center; width:120px;">'
						+ '<%= "<b>" + user.translateJS("Length of extension") 
							%> (<span id="unit0">').
					append(extensionUnit(2)).
					append('<\/span>)<\/b><\/td><\/tr>');
			var userId;
			var extensionStr;
			<% for (int rawStudNum = 0; rawStudNum < lastStudent; rawStudNum++) {
				final boolean atUnusedExamStudent = rawStudNum == students.length;
				final int studNum = (atUnusedExamStudent
						? firstUnusedExamStudentNum : rawStudNum);
				final EnrollmentData student = students[studNum];
				if (student.isRegistered() && !student.isTA() 
						&& (!student.isUnusedExamStudent() || atUnusedExamStudent)) { %>
					userId = '<%= Utils.toValidJS(student.getUserId()) %>';
					extensionStr = extensionsArr[userId];
					if (isNaN(extensionStr)) extensionStr = '';
					<% if (atUnusedExamStudent) { %>
						extensionsOut.append('<tr><td>&nbsp;<\/td><\/tr>');
					<% } // if at first exam student %>
					extensionsOut.append('<tr><td class="regtext" '
							+ 'style="padding-left:10px; padding-top:10px;">'	
							+ '<%= atUnusedExamStudent
								? user.translateJS("Unused exam students")
								: Utils.toValidJS(student.getName()) %><\/td>'
							+ '<td class="regtext" style="text-align:center;">'
							+ '<input type="text" size="3" name="s<%= studNum %>"');
					if (!isEmpty(extensionStr)) {
						extensionsOut.append('value="').
								append(extensionStr).append('"');
					}
					extensionsOut.append('\/><\/td>');
					if (extensionStr === '<%= Assgt.INDEFINITE %>') {
						extensionsOut.append('<td>(<%=
								user.translateJS("indefinite") %>)<\/td>');
					}
					extensionsOut.append('<\/tr>');
				<% } // for each non-TA who is registered and is not an exam student
			} // for each student %>
			extensionsOut.append('<\/table>');
			setInnerHTML('extensionsTable', extensionsOut.toString());
			setInnerHTML('instructionsTable', '<%= user.translateJS(
					"For each student to whom you want to grant an extension, "
					+ "enter the number of ***days*** " 
					+ "the extension should last. Enter &ndash;1 for an indefinite "
					+ "extension.  After the due date, ACE will record in the "
					+ "gradebook only the "
					+ "work of students to whom extensions have been granted; "
					+ "other students will continue to have access to the "
					+ "assignment, but ACE will not record their work.", "@days@")
					%>'.replace(/@days@/, extensionUnit(2))); 
		} // if extensionsPainted
		extensionsPainted = !extensionsPainted;
	} // paintExtensions()

	function getExtensionText(phrase, numExtensions, extension) {
		var phraseRepl = new String.builder().
				append('<span id="length').append(numExtensions).
				append('">').append(extension).
				append('</span> <span id="unit').append(numExtensions).
				append('">').append(extensionUnit(extension)).
				append('</span>');
		return phrase.replace(/@2 days@/, phraseRepl.toString()); 
	} // getExtensionText()

	function extensionUnit(number) {
		if (number === 1.0) return '<%= user.translateJS("minute") %>';
		else return '<%= user.translateJS("minutes") %>'; 
	} // extensionUnit()

	function changeUnits() {
		if (!extensionsPainted) {
			for (var studNum = 0; studNum < numExtensions; studNum++) { // <!-- >
				var studCell = document.getElementById('unit' + studNum);
				if (studCell) {
					var num = getInnerHTML('length' + studNum); 
					studCell.innerHTML = extensionUnit(parseInt(num));
				}
			}
		} else {
			if (document.getElementById('unit0')) {
				setInnerHTML('unit0', extensionUnit(2));
			}
		}
	} // changeUnits()

	function getPhrase(phrase, studNum, extension) {
		setInnerHTML('extension' + studNum, 
				getExtensionText(phrase, studNum, extension));
	} // getPhrase()

	// -->
	</script>
</head>

<body style="text-align:center; background-color:#FFFFFF;"
		onload="setTab('<%= toTabName(user.translateJS("Assignments")) %>');">

	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

<div id="contentsWithTabsWithFooter">

<table class="regtext" style="margin-left:auto; margin-right:auto;
		border-style:none; border-collapse:collapse;
		width:626px;">
	<tr><td class="boldtext big" style="padidng-top:10px; padding-bottom:10px;">
		<%= header %>
	</td>
	</tr>
	<tr><td class="regtext" style="padding-bottom:10px;">
		<%= user.translate("Use this page to change the due date and time of all "
				+ "exam assignments to a uniform value and to provide the same "
				+ "extension to one or more students on every exam assignment.") %>
	</td>
	</tr>
	<tr><td>
		<form name="editform" action="saveExamData.jsp" method="post">
			<input type="hidden" name="extensions" value="<%= extensionsStr %>" />
		<table class="whitetable" style="background-color:#f6f7ed; width:100%;">
		<tr>
		<td class="regtext" style="padding-left:40px;"><%=
				user.translate("Due date") %>:</td>
		<td class="regtext">	
			<input type="text" name="duedate_date" size="12" 
					value="<%= DateUtils.getShortDate(firstDescr.getDueDate(), zone,
						user.prefersDay1st()) %>"/>
		</td><td class="regtext">	
			(<%= user.translate(user.prefersDay1st() ? "dd-mm-yyyy" : "mm-dd-yyyy") %>)
		</tr>
		<tr>
		<td class="regtext" style="padding-left:40px;"><%=
				user.translate("Due time") %>:</td>
		<td class="regtext">	
			<input type="text" name="duedate_time" size="5" 
			value="<%= DateUtils.getShortTime(firstDescr.getDueDate(), zone) %>"/>
		</td><td class="regtext">	
			(<%= user.translate("hh:mm, 24-hour format") %>) </td>
		</tr>

		<tr><td colspan="3"><hr></td></tr>
		<tr><td class="regtext" style="padding-left:40px; vertical-align:top;" colspan="2">
			<b><a href="javascript:paintExtensions();"><%=
					user.translate("Extensions") %></a></b>
		</td><td class="regtext" style="vertical-align:top;">
			<%= user.translate("Click the link to grant extensions "
					+ "to or change extensions for selected students.") %>
		</td></tr>
		<tr> 
		<td id="extensionsTable" class="regtext" style="padding-left:40px;" colspan="2">
			<% if (Utils.isEmpty(extensions)) { %>
				<%= user.translate("You have granted no extensions.") %>
			<% } else { 
				final List<String> studentIds2 = new ArrayList<String>(studentIds);
				String firstUnusedExamStudentUserId = null;
				if (haveUnusedExamStudents) {
					firstUnusedExamStudentUserId = userIds.get(firstUnusedExamStudentNum);
					studentIds2.add(firstUnusedExamStudentUserId);
				} // if have unused exam students
				for (final String studentId : studentIds2) {
					final boolean atFirstUnusedExamStudent =
							studentId.equals(firstUnusedExamStudentUserId);
					final String extension = extensions.get(studentId);
					final int studNum = userIds.indexOf(studentId); 
					if (studNum >= 0 && MathUtils.parseDouble(extension) != 0) {
						final EnrollmentData student = students[studNum];
						if (student.isRegistered() && !student.isTA() 
								&& (!student.isUnusedExamStudent()
									|| atFirstUnusedExamStudent)) {
							final boolean isIndefinite = 
									MathUtils.parseDouble(extension) == Assgt.INDEFINITE;
							final String phrase = 
									(atFirstUnusedExamStudent ? "<br/>" : "")
									+ (isIndefinite && atFirstUnusedExamStudent
									? user.translateJS("Unused exam students "
										+ "have an indefinite extension.")
									: atFirstUnusedExamStudent
									? user.translateJS("Unused exam students "
											+ "have an extension of ***2 days***.",
										"@2 days@")
									: isIndefinite
									? user.translateJS("***Student X*** has an "
											+ "indefinite extension.", 
										student.getName())
									: user.translateJS("***Student X*** has an "
											+ "extension of ***2 days***.",
										new String[] {student.getName(),
											"@2 days@"})) 
									+ "<br/>";
			%>
						<span id="extension<%= studNum %>"></span>
						<script type="text/javascript">
							// <!-- >
							getPhrase('<%= phrase %>', <%= studNum %>, 
									<%= extension %>);
							// -->
						</script>
			<%			} // if student is registered, not a TA, not an exam student
					} // if student with extension is in users list
				} // for each student
			} // if there are extensions %>
		</td>
		<td id="instructionsTable" class="regtext" style="vertical-align:top; width:50%"></td>
		</tr>

		</table>
		</form>
	</td></tr>
</table>
</div>
<div id="footer">
<center>
<table>
	<tr><td class="regtext" style="padding-left:40px; 
			padding-bottom:10px; text-align:center;">
		<table style="text-align:center; margin-top:10px;">
			<tr><td>
				<%= makeButton(user.translate("Submit"), "submitThis();") %> 
			</td><td style="padding-left:20px;">
				<%= makeButton(user.translate("Cancel"), "goBackAgain();") %>
			</td></tr>
		</table>
	</td></tr>
</table></center>
</form>
</div>
</body>
</html>
