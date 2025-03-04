<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page language="java" %>
<%@ page import="
	com.epoch.courseware.EnrollmentData,
	com.epoch.session.HWCreateSession,
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
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	if (!Utils.among(role, User.ADMINISTRATOR, User.INSTRUCTOR) && !isTA) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}

	final int hwNum = MathUtils.parseInt(request.getParameter("hwNum"));
	final boolean addMode = hwNum == 0;
	final TimeZone zone = course.getTimeZone();
	final int tutorialId = AppConfig.tutorialId;
	final int courseId = course.getId();
	final boolean isTutCourse = courseId == tutorialId;

	final int numHWSets = Utils.getLength(assgts);
	HWCreateSession hwCreator;
	synchronized (session) {
		hwCreator = (HWCreateSession) session.getAttribute("hwCreator");
	}
	final int numQs = hwCreator.getNumQsSeen();
	final Assgt assgt = hwCreator.assgt;
	/* Utils.alwaysPrint("editHWProps.jsp: assgt = ", assgt.toString(),
			", id = ", assgt.id); /**/
	final Assgt firstDescr = (numHWSets >= 1 && hwNum != 1 && course.isExam() 
			? assgts[0] : null);
	final EnrollmentData[] students = (isTutCourse ? new EnrollmentData[0]
			: isTA ? ((StudentSession) userSess).getEnrolledStudents()
			: ((InstructorSession) userSess).getEnrolledStudents());

	final int ATTEMPT = Assgt.ATTEMPT;
	final int TIME = Assgt.TIME;
	final int LIMITS = Assgt.LIMITS;
	final int FACTORS = Assgt.FACTORS;
	final String NO_MAX_EXTENSION = Assgt.NO_MAX_EXTENSION;

	final Map<String, String> extensions = assgt.getExtensions();
	final List<String> studentIds = new ArrayList<String>();
	if (!Utils.isEmpty(extensions)) studentIds.addAll(extensions.keySet());
	final String extensionsStr = assgt.extensionsToString();
	final String maxExtensionStr = assgt.getMaxExtensionStr();
	final boolean noMaxExtension = NO_MAX_EXTENSION.equals(maxExtensionStr);

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
	final int lastStudent = students.length + (haveUnusedExamStudents ? 1 : 0);
	
	if (addMode && course.isExam()) {
		assgt.setIsExam(true);
		if (firstDescr != null) {
			assgt.setDueDate(firstDescr.getDueDate());
			assgt.setExtensions(firstDescr.getExtensions());
		} // if there's a first assignment from which to crib
	} // if new assignment in exam course
	
	final String[][][] allGradeParams = assgt.getGradingParams();
	final int[] numGradingParams = new int[2];
	numGradingParams[ATTEMPT] = allGradeParams[ATTEMPT][FACTORS].length;
	numGradingParams[TIME] = allGradeParams[TIME][FACTORS].length;
	
	final String CHECKED = "checked=\"checked\"";
	final String SELECTED = "selected=\"selected\"";
%>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Assignment Properties Editor</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
		* html body {
			padding:100px 0 55px 0; 
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
	var extensionsPainted = true;
	var extensionsArr = {
			<% for (final String studentId : studentIds) { 
				String extensionStr = extensions.get(studentId);
				if (extensionStr.startsWith(".")) {
					extensionStr = Utils.toString('0', extensionStr);
				} %>
				'<%= studentId %>' : <%= extensionStr %>,
			<% } // for each extension %>
				'' : 0
			};
	
	var attemptGradingParamsCt = <%= numGradingParams[ATTEMPT] %>; 
	var timeGradingParamsCt = <%= numGradingParams[TIME] %>; 

	function submitThis() {
		var prevLimit = '0';
		var submit = true;
		var theAlert = '';
		var attemptGradingResult = '';
		var timeGradingResult = '';
		<% for (int gradingType = ATTEMPT; gradingType <= TIME; 
				gradingType += TIME - ATTEMPT) { 
			final String which = (gradingType == TIME ? "time" : "attempt"); 
		%>
			var gradingParamsCt = (<%= gradingType == TIME %> ?
					timeGradingParamsCt : attemptGradingParamsCt);
			var gradingResult = new String.builder();
			for (var paramNum = 1; paramNum <= gradingParamsCt; paramNum++) { // <!-- >
				var limit = getValue('<%= which %>Limit' + paramNum);
				var multiplier = getValue('<%= which %>Factor' + paramNum);
				if (isEmpty(multiplier) 
						|| (isEmpty(limit) && paramNum < gradingParamsCt)) { // <!-- >
					theAlert = '<%= user.translateJS(Utils.toString(
							"Please enter values for all boxes in the ", which, 
							"-dependent grading parameters except the last limit.")) %>';
					submit = false;
					break;
				}
				if (!canParseToFloat(multiplier) 
						|| parseToFloat(multiplier) < 0 // <!-- >
						|| parseToFloat(multiplier) > 1) {
					theAlert = '<%= user.translateJS(Utils.toString(
							"All multipliers in the ", which, 
							"-dependent grading parameters must be numbers "
							+ "between 0 and 1.")) %>';
					submit = false;
					break;
				}
				if ((!canParseToInt(limit) && paramNum !== gradingParamsCt)
						|| !canParseToFloat(multiplier)) { // <!-- -->
					theAlert = '<%= user.translateJS(Utils.toString("One of your ", 
							which, "-dependent grading parameters is not a number.")) %>';
					submit = false;
					break;
				}
				if (paramNum > 1 && paramNum < gradingParamsCt // <!-- >
						&& parseToInt(limit) <= parseToInt(prevLimit)) { // <!-- >
					theAlert = '<%= user.translateJS(Utils.toString(
							"Each upper limit in the ", which, 
							"-dependent grading parameters " 
							+ "must be greater than the previous one.")) %>';
					submit = false;
					break;
				}
				if (gradingResult.length() > 0) gradingResult.append('/');
				if (paramNum < gradingParamsCt) gradingResult.append(limit); // <!-- >
				gradingResult.append('/').append(multiplier);
				prevLimit = limit;
			} // for each grading parameter
			if (<%= gradingType == TIME %>)
				timeGradingResult = gradingResult.toString();
			else attemptGradingResult = gradingResult.toString();
		<% } // for each gradingType %>
		var form = document.editform;
		// get extension grantees
		if (extensionsPainted) { // get values from current document
			var bld = new String.builder();
			var extensionStr = '';
			var first = true;
			<% for (int studNum = 0; studNum < students.length; studNum++) { 
				final EnrollmentData student = students[studNum];
				final boolean isUnusedExamStudent = 
						student.isUnusedExamStudent();
				if (student.isRegistered() && !student.isTA()) { 
			%>
					extensionStr = form.s<%= isUnusedExamStudent
							? firstUnusedExamStudentNum : studNum %>.value;
					if (!isEmpty(extensionStr) && submit) {
						if (first) first = false;
						else bld.append('/');
						if (extensionStr.charCodeAt(0) === 8211) {
							extensionStr = '-' + extensionStr.substring(1);
						} // if starts with en dash
						if (!canParseToFloat(extensionStr)) { 
							theAlert = '<%= isUnusedExamStudent
									? user.translateJS("The extension for "
										+ "unused exam students is not a "
										+ "number.")
									: user.translateJS(
										"The extension for ***this student"
											+ "*** is not a number.", 
										student.getName()) %>';
							submit = false;
						} else if (parseToFloat(extensionStr) !== 0) {
							bld.append('<%= Utils.toValidJS(
										student.getUserId()) %>/').
									append(extensionStr);
						} // if there's a table entry and a value
					} // if there's an entry
			<% 	} // if student is registered and not TA
			} // for each student 
			%>
			extensions = bld.toString();
		} // if extensionsPainted; if not, value has already been stored
		if (getChecked('isTimed') && (!canParseToInt(form.duration.value)
				|| parseToInt(form.duration.value) <= 0)) { // <!-- >
			theAlert = '<%= user.translateJS("Please enter a positive integer "
					+ "for the duration of the timed assignment.") %>';
			submit = false;
		}
		if (form.maxExtension && !canParseToFloat(form.maxExtension.value)) {
			theAlert = '<%= user.translateJS("Please enter a number for the "
					+ "maximum extension a student may self-grant for this "
					+ "assignment.") %>';
			submit = false;
		}
		if (submit) {
			form.attemptGradingParams.value = attemptGradingResult;
			form.timeGradingParams.value = timeGradingResult;
			form.extensions.value = extensions;
			form.submit(); 
		} else toAlert(theAlert); 
	} // submitThis()

	var gradingOut = new String.builder();

	function addGradingParameter(which) {
		var gradingParamsCt = (which === 'attempt' ? 
				attemptGradingParamsCt : timeGradingParamsCt);
		initializeGradingOut();
		var prevLimit = 0;
		for (var paramNum = 1; paramNum <= gradingParamsCt; paramNum++) { // <!-- -->
			var limitCell = which + 'Limit' + paramNum;
			var multCell = which + 'Factor' + paramNum;
			limit = getValue(limitCell);
			multiplier = getValue(multCell);
			if (isEmpty(limit)) {
				toAlert('<%= user.translateJS(
						"Please enter upper limits on all lines "
						+ "before adding another line.") %>');
				return;
			}
			if (which === 'attempt' 
					&& (!canParseToInt(limit) || parseToInt(limit) <= 0)) { // <!-- -->
				toAlert('<%= user.translateJS(
						"Please enter only positive integers "
						+ "for the numbers of attempts.") %>');
				return;
			}
			if (paramNum > 1 && parseToFloat(limit) <= prevLimit) { // <!-- -->
				toAlert('<%= user.translateJS("Each upper limit must be "
						+ "greater than the previous one.") %>');
				return;
			}
			if (which === 'attempt') prevLimit++;
			addToGradingOut(which, paramNum, limit, multiplier, prevLimit);
			prevLimit = parseToFloat(limit);
		} // for each current grading parameter
		var lastParam = (gradingParamsCt === 0 ? 0 : prevLimit);
		if (which === 'attempt') lastParam++;
		addToGradingOut(which, gradingParamsCt + 1, '', '', '' + lastParam); 
		finishGradingOut();
		setInnerHTML(which + 'GradingTable', gradingOut.toString());
		if (gradingParamsCt === 0) { // otherwise not necessary
			setInnerHTML(which + 'RemoveGradingParam', 
					'<a href="javascript:removeGradingParameter(\'' 
					+ which + '\');"><%= user.translateJS(
							"Remove grading parameter") %><\/a>'); 
			setInnerHTML(which + 'LastValue', (which === 'attempt' 
					? '(<%= user.translateJS("ACE will set the last value for "
								+ "the number of attempts to infinity.") %>)'
					: '(<%= user.translateJS("ACE will set the last value for the "
						+ "number of days submitted past due to infinity.") %>)'));
		}
		if (which === 'attempt') attemptGradingParamsCt++;
		else timeGradingParamsCt++;
	} // addGradingParameter()

	function removeGradingParameter(which) {
		if (!document.getElementById(which + 'GradingTable')) {
			// return if the GradingTable is not available
			// (for some unfathomable reason, this function is being called early)
			return;
		}
		var gradingParamsCt = (which === 'attempt' 
				? attemptGradingParamsCt : timeGradingParamsCt);
		initializeGradingOut();
		var prevLimit = 0;
		for (var paramNum = 1; paramNum < gradingParamsCt; paramNum++) { // <!-- -->
			var limitCell = which + 'Limit' + paramNum;
			var multCell = which + 'Factor' + paramNum;
			limit = getValue(limitCell);
			multiplier = getValue(multCell);
			if (which === 'attempt') prevLimit++;
			addToGradingOut(which, paramNum, limit, multiplier, prevLimit);
			prevLimit = parseToFloat(limit);
		} // for each up to the penultimate grading parameter
		finishGradingOut();
		setInnerHTML(which + 'GradingTable', gradingOut.toString());
		if (gradingParamsCt === 1) { 
			clearInnerHTML(which + 'LastValue');
			clearInnerHTML(which + 'RemoveGradingParam');
		}
		if (which === 'attempt')
			attemptGradingParamsCt -= 1;
		else timeGradingParamsCt -= 1;
	} removeGradingParameter()
	
	function initializeGradingOut() {
		gradingOut = new String.builder().append('<table>'); 
	}

	function addToGradingOut(which, paramNum, limit, multiplier, prevlimit) {
		var phrase = (which === 'attempt' ? 
				'<%= user.translateJS(
					"If the number of attempts is between ***4*** "
					+ "and ***6***, multiply the grade by ***0.8***.") %>'
				: '<%= user.translateJS(
					"If the number of days submitted past due is "
					+ "greater than ***1*** and less than or equal to "
					+ "***2***, multiply the grade by ***0.8***.") %>'); 
		var parts = phrase.split('***');
		gradingOut.append('<tr><td class="regtext" '
					+ 'style="padding-left:40px;">').append(parts[0])
				.append(prevlimit).append(parts[2])
				.append('<input type="text" name="').append(which)
				.append('Limit').append(paramNum).append('" id="')
				.append(which).append('Limit').append(paramNum)
				.append('" size="3" value="').append(limit).append('"\/> ')
				.append(parts[4]).append(' <input type="text" name="')
				.append(which).append('Factor').append(paramNum)
				.append('" id="').append(which).append('Factor')
				.append(paramNum).append('" size="3" value="')
				.append(multiplier).append('"\/><\/td><\/tr>');
	} // addToGradingOut()

	function finishGradingOut() {
		gradingOut.append('<\/td><\/tr><\/table>');
	} // finishGradingOut()

	var numExtensions = <%= Utils.getSize(extensions) %>;

	function paintExtensions() {
		if (extensionsPainted) { // contract list, show only students with extensions
			var extListBld = new String.builder();
			var extValueBld = new String.builder();
			numExtensions = 0;
			var first = true;
			var extensionStr;
			extensionsArr = new Array();
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
						extValueBld.append(userId).append('/')
								.append(extensionStr);
						<% if (atUnusedExamStudent) { %>
							extListBld.append('<br\/>\n');
						<% } // if at first exam student %>
						if (extensionStr === '<%= Assgt.INDEFINITE %>') {
							extListBld.append('<%= atUnusedExamStudent
										? user.translateJS("Unused exam students "
												+ "have an indefinite extension.") 
										: user.translateJS("***Student X*** has "
												+ "an indefinite extension.",
											student.getName()) %>')
									.append('<input type="hidden" '
										+ 'id="s<%= studNum %>" '
										+ 'name="s<%= studNum %>" '
										+ 'value="<%= Assgt.INDEFINITE %>" />');
						} else {
							var phrase = '<%= atUnusedExamStudent
									? user.translateJS("Unused exam students "
											+ "have an extension of ***2 days***.",
										"@2 days@")
									: user.translateJS("***Student X*** has an "
											+ "extension of ***2 days***.",
										new String[] {student.getName(),
											"@2 days@"}) %>';
							var replacement = new String.builder().append(extensionStr)
									.append(' <span id="unit<%= studNum %>">')
									.append(extensionUnit(parseToFloat(extensionStr)))
									.append('<\/span>').toString();
							extListBld.append(phrase.replace(/@2 days@/, replacement))
									.append('<input type="hidden" '
										+ 'id="s<%= studNum %>" '
										+ 'name="s<%= studNum %>" '
										+ 'value="').append(extensionStr).append('" />');
						} // if extension is indefinite
						extListBld.append('<br\/>\n');
						extensionsArr[userId] = extensionStr;
						numExtensions++;
					} // if there is an extension
			<% 	} // if student is registered and not a TA
			}  // for each student %>
			extensions = extValueBld.toString();
			var extensionList = extListBld.toString();
			// alert(extensionList);
			if (!isEmpty(extensions)) setInnerHTML('extensionsTable', extensionList);
			else setInnerHTML('extensionsTable', '<%= user.translateJS(
					"You have granted no extensions.") %>');
			clearInnerHTML('instructionsTable');
		} else { // list all students and textboxes containing their current extensions
			var extOut = new String.builder();
			extOut.append('<table><tr>'
						+ '<td><%= Utils.toValidJS(
							makeButton(user.translate("Change all to"), "changeAllExtensions();")) %><\/td>'
						+ '<td style="text-align:center;">'
						+ '<input type="text" size="5" id="newAllExtension" \/><\/td></tr>'
						+ '<tr><td class="regtext" style="text-align:center;">'
						+ '<b><%= user.translateJS("Student") %><\/b><\/td>'
						+ '<td class="regtext" style="text-align:center; width:120px;">'
						+ '<b><%= user.translateJS("Length of extension") 
							%> (<span id="unitTitle">')
					.append(extensionUnit(2))
					.append('<\/span>)<\/b><\/td><\/tr>');
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
						extOut.append('<tr><td>&nbsp;<\/td><\/tr>');
					<% } // if at first exam student %>
					extOut.append('<tr><td class="regtext" '
							+ 'style="padding-left:10px; padding-top:10px;">'	
							+ '<%= atUnusedExamStudent
								? user.translateJS("Unused exam students")
								: Utils.toValidJS(student.getName()) %><\/td>'
							+ '<td class="regtext" style="text-align:center;">'
							+ '<input type="text" size="5" '
							+ 'name="s<%= studNum %>" id="s<%= studNum %>"');
					if (!isEmpty(extensionStr)) {
						extOut.append('value="').append(extensionStr).append('"');
					}
					extOut.append('\/><\/td>');
					if (parseToFloat(extensionStr) == <%= Assgt.INDEFINITE %>) {
						extOut.append('<td>(<%=
								user.translateJS("indefinite") %>)<\/td>');
					}
					extOut.append('<\/tr>');
			<% 	} // if student is registered and not a TA
			} // for each student %>
			extOut.append('<\/table>');
			setInnerHTML('extensionsTable', extOut.toString());
			setInnerHTML('instructionsTable', 
					'<%= user.translateJS(
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

	function changeAllExtensions() {
		var allExtensions = getValue('newAllExtension');
		<% for (int rawStudNum = 0; rawStudNum < lastStudent; rawStudNum++) {
			final boolean atUnusedExamStudent = rawStudNum == students.length;
			final int studNum = (atUnusedExamStudent
					? firstUnusedExamStudentNum : rawStudNum);
			final EnrollmentData student = students[studNum];
			if (student.isRegistered() && !student.isTA() 
					&& (!student.isUnusedExamStudent() || atUnusedExamStudent)) { %>
				document.editform.s<%= studNum %>.value = allExtensions;
		<% 	} // if student is registered and not a TA
		}  // for each student %>
	} // changeAllExtensions()

	function extensionUnit(number) {
		var isChecked = document.editform.isExam.checked;
		return (number === 1 ? (isChecked 
					? '<%= user.translateJS("minute") %>' 
					: '<%= user.translateJS("day") %>')
				: (isChecked 
					? '<%= user.translateJS("minutes") %>' 
					: '<%= user.translateJS("days") %>'));
	} // extensionUnit()

	function changeUnits() {
		var form = document.editform;
		if (!extensionsPainted) {
			for (var studNum = 0; studNum < <%= students.length %>; studNum++) { // <!-- >
				var valueCellName = 's' + studNum;
				if (cellExists(valueCellName)) {
					var extensionStr = getValue(valueCellName);
					var unit = extensionUnit(parseToFloat(extensionStr));
					setInnerHTML('unit' + studNum, unit);
				} // if cell exists
			} // for each student
		} else setInnerHTML('unitTitle', extensionUnit(2));
		var isExam = form.isExam && form.isExam.checked;
		if (isExam && <%= !assgt.isExam() && !assgt.savePrevTries() %>) {
			form.savePrevTries.checked = true;
			form.logAllResponses.checked = true;
		} else if (!isExam && <%= assgt.isExam() && assgt.savePrevTries() %>) {
			form.savePrevTries.checked = false;
			form.logAllResponses.checked = false;
		}
		if (!isExam && getChecked('isTimed')) {
			setChecked('isTimed', false);
			changeTimedStatus();
		}
		setInnerHTML('timeDependentGradingAdvice', isExam
				? '<%= user.translateJS("Use this feature to reduce the "
					+ "students' grades on this assignment as the number of " 
					+ "minutes since the due date increases.") %>'
				: '<%= user.translateJS("Use this feature to reduce the "
					+ "students' grades on this assignment as the number of " 
					+ "days since the due date increases.") %>');
		if (isExam) changeMaxExtensionVisibility();
	} // changeUnits()

	function changeMaxExtensionVisibility() {
		var maxExtensionType = getValue('selectMaxExtensionType');
		if (maxExtensionType === 'noMax') {
			hideCell('inputMaxExtension');
			setValue('maxExtension', '<%= NO_MAX_EXTENSION %>');
		} else {
			var form = document.editform;
			var isExam = form.isExam && form.isExam.checked;
			setValue('maxExtension', isExam ? '0' 
					: '<%= noMaxExtension ? "3" : maxExtensionStr %>');
			showCell('inputMaxExtension');
		}
	} // changeMaxExtensionVisibility()

	function changeTimedStatus() {
		var form = document.editform;
		if (getChecked('isTimed')) {
			showCell('durationCell');
			if (!form.isExam.checked) {
				form.isExam.checked = true;
				changeUnits();
			} // if not an exam
		} else {
			hideCell('durationCell');
		}
	} // changeTimedStatus()

	// -->
	</script>
</head>

<body style="text-align:center; background-color:#FFFFFF;"
		onload="setTab('<%= toTabName(user.translateJS("Assignments")) %>'); <%= 
			!isTutCourse ? "paintExtensions(); changeUnits();" : "" %>">

	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
	<%@ include file="/navigation/courseSidebarHtml.jsp.h" %>

<div id="contentsWithTabsWithFooter">

<table class="regtext" style="margin-left:auto; margin-right:auto;
		border-style:none; border-collapse:collapse;">
	<tr><td class="boldtext big" style="padding-top:10px; padding-bottom:10px;">
		<%= user.translate((addMode ? "Add new" : "Edit") + " assignment") %>
	</td>
	</tr>
	<tr><td>
		<form name="editform" action="saveHW.jsp" method="post">
			<input type="hidden" name="attemptGradingParams" value="" />
			<input type="hidden" name="timeGradingParams" value="" />
			<input type="hidden" name="extensions" value="<%= extensionsStr %>" />
		<table class="whitetable" style="background-color:#f6f7ed; width:650px;">
		<tr><td class="regtext" style="padding-left:40px; padding-top:10px;
			width:25%;"><%= user.translate("Name") %>:</td>
		<td class="regtext" style="padding-top:10px; width:30%;">
			<%= Utils.toDisplay(assgt.getName()) %></td>
		</tr>
		<tr>
		<td class="regtext" style="padding-left:40px;"><%= 
				user.translate("Visible to students?") %></td>
		<td><input type="checkbox" name="visible"
			<%= assgt.isVisible() ? CHECKED : "" %>/></td>
		</tr>
		<tr>
		<td class="regtext" style="padding-left:40px;"><%=
				user.translate("Creation date") %>:</td>
		<td class="regtext">
			<%= DateUtils.getString(assgt.creationDate, zone) %></td>
		</tr>
		<tr>
		<td class="regtext" style="padding-left:40px;"><%=
				user.translate("Due date") %>:</td>
		<td class="regtext">	
			<table><tr><td>
			<input type="text" name="duedate_date" size="12" class="inputText"
					value="<%= DateUtils.getShortDate(assgt.getDueDate(), zone,
						user.prefersDay1st()) %>"/>
			</td><td>
			(<%= user.translate(user.prefersDay1st() ? "dd-mm-yyyy" : "mm-dd-yyyy") %>)
			</td></tr></table>
		</td></tr>
		<tr>
		<td class="regtext" style="padding-left:40px;"><%=
				user.translate("Due time") %>:</td>
		<td class="regtext">	
			<table><tr><td>
			<input type="text" name="duedate_time" size="5" class="inputText"
					value="<%= DateUtils.getShortTime(assgt.getDueDate(), zone) %>"/>
			</td><td>
			(<%= user.translate("hh:mm, 24-hour format") %>)
			</td></tr></table>
		</td></tr>
		<tr>
		<td class="regtext" style="padding-left:40px;">
			<%= user.translate("Record <i>all</i> students' work "
					+ "after the due date?") %></td>
		<td class="regtext"><input type="checkbox" name="recordAfterDue"
			<%= assgt.recordAfterDue() ? CHECKED : "" %>/></td>
		<td><%= user.translate("If this box is unchecked, ACE will enforce "
				+ "the due date, and it will not record in the gradebook any "
				+ "work done past the due date.  Check this box if you want "
				+ "the time-dependent grading parameters to have an effect.") %></td>
		</tr>
		<% if (!isTutCourse) { %>
			<tr>
			<td class="regtext" style="padding-left:40px;">
				<%= user.translate("Is this assignment an exam?") %></td>
			<td class="regtext"><input type="checkbox" name="isExam"
				onchange="javascript:changeUnits();"
				<%= assgt.isExam() ? CHECKED : "" %>/></td>
			<td><%= user.translate("If this box is checked, extensions and "
					+ "time past due will be measured in minutes, not days, "
					+ "and ACE will check due dates and times <i>every "
					+ "time</i> a student submits a response, rather than "
					+ "just when it displays the assignment list page.") %></td>
			</tr>
			<tr>
			<td class="regtext" style="padding-left:40px;">
				<%= user.translate("Is this assignment a timed exam?") %></td>
			<td colspan="2" class="regtext">
				<input type="checkbox" id="isTimed" name="isTimed"
						onchange="javascript:changeTimedStatus();"
				<%= assgt.isTimed() ? CHECKED : "" %>/>
				<span id="durationCell" style="padding-left:10px; visibility:<%= 
						assgt.isTimed() ? "visible" : "hidden" %>">
					<%= user.translate("Duration") %>:
					<input type="text" name="duration" size="5" class="inputText"
							value="<%= assgt.getDuration() %>"/>
					<%= user.translate("minutes") %>
				</span></td>
			</tr>
			<tr>
			<td class="regtext" style="padding-left:40px;">
				<%= user.translate("Delay grading?") %></td>
			<td class="regtext"><input type="checkbox" name="delayGrading"
				<%= assgt.delayGrading() ? CHECKED : "" %>/></td>
			<td><%= user.translate("If this box is checked, ACE will "
					+ "grade students' responses and provide feedback only after "
					+ "you press the Regrade button in the gradebook.") %></td>
			</tr>
			<tr>
			<td class="regtext" style="padding-left:40px;">
				<%= user.translate("Exclude assignment from course totals?") %></td>
			<td class="regtext"><input type="checkbox" name="excludeFromTotals"
				<%= assgt.excludeFromTotals() ? CHECKED : "" %>/></td>
			<td><%= user.translate("If this box is checked, ACE will exclude the "
					+ "grades on this assignment from the course grade totals.") %></td>
			</tr>
			<tr>
			<td class="regtext" style="padding-left:40px;">
				<%= user.translate("Show literature references to students "
						+ "(where available)?") %></td>
			<td class="regtext">
				<select name="showRefs">
				<option value="0" <%= !assgt.showRefsBeforeAnswered() 
						&& !assgt.showRefsAfterAnswered() ? SELECTED : "" %>/>
					<%= user.translate("never") %>
				</option>
				<option value="<%= Assgt.SHOW_REFS_AFTER_ANSWERED %>" 
						<%= assgt.showRefsAfterAnswered() ? SELECTED : "" %>/>
					<%= user.translate("after question answered") %>
				</option>
				<option value="<%= Assgt.SHOW_REFS_BEFORE_ANSWERED 
							| Assgt.SHOW_REFS_AFTER_ANSWERED %>" 
						<%= assgt.showRefsBeforeAnswered() ? SELECTED : "" %>/>
					<%= user.translate("always") %>
				</option>
				</select>
			</td>
			<td><%= user.translate("Question authors can enter literature "
					+ "references in the question-authoring tool.") %></td>
			</tr>
			<tr>
			<td class="regtext" style="padding-left:40px;">
				<%= user.translateJS("Write every response to database?") %>
			</td>
			<td class="regtext">
				<input type="checkbox" name="savePrevTries" <%=
						assgt.savePrevTries() ? CHECKED : "" %> />
			</td>
			<td>
				<%= user.translateJS("If this box is checked, new "
					+ "responses will not overwrite previous responses "
					+ "in the database, and you will be able to see "
					+ "previous responses in the gradebook.") %>
			</td>
			</tr>
			<tr>
			<td class="regtext" style="padding-left:40px;">
				<%= user.translateJS("Log every response to disk?") %>
			</td>
			<td class="regtext">
				<input type="checkbox" name="logAllResponses" <%=
						assgt.logsAllToDisk() ? CHECKED : "" %> />
			</td>
			<td>
				<%= user.translateJS("If this box is checked, ACE "
					+ "will log every response to disk in a format "
					+ "that you can view in your browser.") %>
			</td>
			</tr>
			<tr>
			<td class="regtext" style="padding-left:40px;">
				<%= user.translateJS("Allow saving without submitting?") %>
			</td>
			<td class="regtext">
				<input type="checkbox" name="saveWOSubmitting" <%=
						assgt.showSaveWOSubmitting() ? CHECKED : "" %> />
			</td>
			<td>
				<%= user.translateJS("If this box is checked, ACE "
					+ "will allow students to save their work on a question "
					+ "without submitting it for grading until they are "
					+ "ready. This option is useful when the assignment "
					+ "uses attempt-dependent grading.") %>
			</td>
			</tr>
			<% for (int gradingType = ATTEMPT; 
					gradingType <= TIME; gradingType += TIME - ATTEMPT) {
				final int numParams = numGradingParams[gradingType];
				final String[] limits = allGradeParams[gradingType][LIMITS];
				final String[] factors = allGradeParams[gradingType][FACTORS];
				final String which = (gradingType == TIME ? "time" : "attempt"); %>
				<tr><td colspan="3"><hr/></td></tr>
				<tr><td class="regtext" style="vertical-align:top; 
						padding-left:40px;" colspan="2">
					<b><%= user.translate((gradingType == TIME 
							? "Time" : "Attempt") + "-dependent grading") %></b>
				</td><td class="regtext">
					<% if (gradingType == ATTEMPT) { %>
						<%= user.translate("Use this feature to reduce the "
						+ "students' grades on this assignment as the number of " 
						+ "attempts increases. Grades are adjusted as they are "
						+ "displayed in the gradebook only.") %>
					<% } else { %>
						<span id="timeDependentGradingAdvice"></span>
						<%= user.translate("Grades are adjusted as they are "
							+ "displayed in the gradebook only.") %>
					<% } // if ATTEMPT %>
				</td></tr>
				<tr><td class="regtext small" style="padding-left:40px;">
					<a href="javascript:addGradingParameter('<%= which %>');"><%= 
							user.translate("Add grading parameter") %></a>
				</td><td id="<%= which %>RemoveGradingParam"
						class="regtext small" style="padding-left:20px;">
					<% if (numParams > 0) { %>
						<a href="javascript:removeGradingParameter('<%= 
								which %>');"><%= 
								user.translate("Remove grading parameter") %></a>
					<% } %>
				</td></tr>
				<tr><td id="<%= which %>GradingTable" class="regtext" colspan="3"
						style="padding-left:20px;">
					<% if (numParams > 0) { %>
						<table>
					<%	String prevLim = "0"; 
						for (int paramNum = 1; paramNum <= numParams; paramNum++) { %>
							<tr><td class="regtext" style="padding-left:40px;">
								<%= user.translate("If the number of "
								+ (gradingType == TIME ? "days submitted past due" : "attempts") 
								+ " is "
								+ (gradingType == TIME ? "greater than" : "between")
								+ " ***4*** and "
								+ (gradingType == TIME ? "less than or equal to " : "")
								+ "***6***, multiply the grade by ***0.8***.",
								new String[] {
									String.valueOf(Integer.parseInt(prevLim) 
										+ (1 - gradingType)),
									"<input type=\"text\" name=\"" + which 
										+ "Limit" + paramNum + "\"id=\"" + which 
										+ "Limit" + paramNum + "\" size=\"3\" value=\""
										+ limits[paramNum - 1] + "\"/>",
									"<input type=\"text\" name=\"" + which 
										+ "Factor" + paramNum + "\" id=\"" + which 
										+ "Factor" + paramNum + "\" size=\"3\" value=\""
										+ factors[paramNum - 1] + "\"/>"}) %> 
							</td></tr>
						<% 	prevLim = limits[paramNum - 1];
						} // for each grading parameter set paramNum %>
						</table>
					<% } // if there are already grading parameters %>
				</td></tr>
				<tr><td id="<%= which %>LastValue" class="regtext" colspan="3" 
						style="padding-left:60px; color:green;">
				</td></tr>
			<% } // for each grading type %>
			<tr><td colspan="3"><hr/></td></tr>
			<tr><td colspan="2" class="regtext" style="padding-left:40px;">
				<b><%= user.translate("Extensions") %></b>
			</td></tr>
			<tr><td colspan="3" class="regtext" 
					style="padding-left:40px; padding-bottom:10px;">
				<select id="selectMaxExtensionType" name="selectMaxExtensionType" 
						onchange="changeMaxExtensionVisibility();">
					<option value="noMax" <%= noMaxExtension && !addMode ? SELECTED : "" %>>
						<%= user.translate("Any self-granted extension up to semester maximum") %>
					</option>
					<option value="max" <%= noMaxExtension && !addMode ? "" : SELECTED %>>
						<%= user.translate("Maximum self-granted extension") %>:
					</option>
				</select>
				<span id="inputMaxExtension" name="inputMaxExtension" 
						style="visibility:<%= noMaxExtension && !addMode 
							? "hidden" : "visible" %>;">
					<input type="text" name="maxExtension" id="maxExtension" 
							class="inputText" size="1"
							value="<%= addMode ? 3 : maxExtensionStr %>"/> 
					<%= user.translate("days") %>
				</span>
			</td></tr>
			<tr><td colspan="2" class="regtext" 
					style="padding-left:40px; vertical-align:top;">
				<b><a href="javascript:paintExtensions();"><%=
						user.translate("Extensions for individual students") %></a></b>
			</td></tr>
			<tr> 
			<td colspan="2" id="extensionsTable" class="regtext" 
					style="padding-top:10px; padding-left:40px;">
				<% for (int rawStudNum = 0; rawStudNum < lastStudent; rawStudNum++) {
					final boolean atUnusedExamStudent = rawStudNum == students.length;
					final int studNum = (atUnusedExamStudent
							? firstUnusedExamStudentNum : rawStudNum);
					final EnrollmentData student = students[studNum];
					if (student.isRegistered() && !student.isTA() 
							&& (!student.isUnusedExamStudent() || atUnusedExamStudent)) { 
						final String studentId = student.getUserId();
						String extension = (extensions == null 
								? null : extensions.get(studentId));
						if (extension != null && extension.startsWith(".")) {
							extension = Utils.toString('0', extension);
						}
				%>
						<input type="hidden" size="5" 
								name="s<%= studNum %>" id="s<%= studNum %>"
								value="<%= extension == null ? "0" : extension %>" />
				<% 	} // if student is registered, not a TA, not an exam student
				} // for each student in course %>
			</td>
		<% } // if not the tutorial course %>
		<td id="instructionsTable" class="regtext" style="vertical-align:top;"></td>
		</tr>
		<tr><td colspan="3"><hr/></td></tr>
		</table>
		</form>
	</td></tr>
</table>
</div>
<div id="footer">
<center>
<table>
	<tr><td class="regtext" style="padding-left:40px; padding-top:10px; 
			text-align:center;">
		<table style="text-align:center;">
			<tr><td>
				<%= makeButton(user.translate("Submit"), "submitThis();") %> 
			</td>
			<% if (!addMode) { %>
				<td style="padding-left:20px;">
					<%= makeButton(user.translate("Cancel"), "goBackAgain();") %>
				</td>
			<% } // if editing existing assignment %>
			</tr>
		</table>
	</td></tr>
</table>
</center>
</div>
</body>
</html>
