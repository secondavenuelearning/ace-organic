<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.Institution,
	com.epoch.db.dbConstants.CourseRWConstants,
	com.epoch.session.AnonSession,
	com.epoch.utils.Utils,
	java.util.HashMap,
	java.util.Map"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%	
	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");
	if (role != User.ADMINISTRATOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	final Institution[] institutions = AnonSession.getAllInstitutions();
	final Map<Integer, String> instnNamesByIds = new HashMap<Integer, String>();
	for (final Institution institution : institutions) {
		instnNamesByIds.put(Integer.valueOf(institution.getId()), institution.getName());
	} // for each institution
	final boolean clearResponses = "responses".equals(request.getParameter("recordType"));
	final String[] descrips = new String[] {
			clearResponses // 0
			? "a response was last submitted"
			: "a student logged in",
			clearResponses // 1
			? "Clear responses"
			: "Remove students",
			clearResponses // 2
			? "that"
			: "who last logged in",
			clearResponses // 3
			? "are"
			: "",
			clearResponses // 4
			? "were submitted"
			: "",
			clearResponses // 5
			? "old"
			: "ago"
			};

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<meta http-equiv="Pragma" content="no-cache"/>
<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<title>ACE clear old responses or users</title>
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<style type="text/css">
	* html body {
		padding:100px 0 50px 0; 
	}
</style>

<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

	function goBackAgain() {
		self.location.href = 'listProfiles.jsp';
	}

	var instnIdsArr = [<%= CourseRWConstants.ANY_INSTITUTION %>];
	var instnNamesArr = ['any institution'];
	<% for (final Institution institution : institutions) { %>
		instnIdsArr.push('<%= institution.getId() %>');
		instnNamesArr.push('<%= Utils.toValidJS(institution.getName()) %>');
	<% } // for each institution %>

	function setSelector() {
		var instnSelBld = new String.builder();
		instnSelBld.append('<select name="institutionId" id="institutionId">');
		for (var instnNum = 0; instnNum < instnIdsArr.length; instnNum++) {
			instnSelBld.append('<option value="')
					.append(instnIdsArr[instnNum])
					.append('">')
					.append(instnNamesArr[instnNum])
					.append('<\/option>');
		} // for each institution
		instnSelBld.append('<\/select>');
		setInnerHTML('institutionCell', instnSelBld.toString());
	} // setSelector()

	function changeTimeType() {
		var selForm = document.userSelForm;
		if (selForm.timeType.value === 'moreThan') {
			setValue('year', '6');
			setValue('month', '0');
			setValue('day', '0');
			setInnerHTML('yearsText', 'years,');
			setInnerHTML('monthsText', 'months, and');
			setInnerHTML('daysText', 'days');
			setInnerHTML('old', '<%= descrips[5] %>.');
		} else {
			setValue('year', '2010');
			setValue('month', '12');
			setValue('day', '31');
			setInnerHTML('yearsText', '/');
			setInnerHTML('monthsText', '/');
			setInnerHTML('daysText', '');
			setInnerHTML('old', '');
		} // if timeType selection'
	} // changeTimeType()

	function checkAndSubmit() {
		var selForm = document.userSelForm;
		var timeType = selForm.timeType.value; 
		var yearStr = selForm.year.value;
		var monthStr = selForm.month.value;
		var dayStr = selForm.day.value;
		var year, month, day;
		if (timeType === 'moreThan') {
			if (!canParseToInt(yearStr)) { // <!-- >
				alert('Please enter an integer of 6 or more for the '
						+ 'number of years ago <%= descrips[0] %>.');
				return;
			} else {
				year = parseInt(yearStr);
				if (year < 6) { // <!-- >
					alert('Please enter an integer of 6 or more for the '
							+ 'number of years ago <%= descrips[0] %>.');
					return;
				} // if year
			} // if year parsable
			if (!canParseToInt(monthStr)) { // <!-- >
				alert('Please enter an integer between 0 and 11 '
						+ 'for the number of months ago.');
				return;
			} else {
				month = parseInt(monthStr);
				if (month >= 12 || month < 0) { // <!-- >
					alert('Please enter an integer between 0 and 11 '
							+ 'for the number of months ago.');
					return;
				} // if month
			} // if month parsable
			if (!canParseToInt(dayStr)) { // <!-- >
				alert('Please enter an integer between 0 and 30 '
						+ 'for the number of days ago.');
				return;
			} else { 
				day = parseInt(dayStr);
				if (day >= 31 || day < 0) { // <!-- >
					alert('Please enter an integer between 0 and 30 '
							+ 'for the number of days ago.');
					return;
				} // if day
			} // if day parsable
		} else {
			if (!canParseToInt(yearStr)) { // <!-- >
				alert('Please enter a year at least 6 years prior to the current one.');
				return;
			} else {
				year = parseInt(yearStr);
				if (year > new Date().getFullYear() - 6) { // <!-- >
					alert('Please enter a year at least 6 years prior to the current one.');
					return;
				} // if year
			} // if year parsable
			if (!canParseToInt(monthStr)) { // <!-- >
				alert('Please enter an integer between 1 and 12 for the month.');
				return;
			} else {
				month = parseInt(monthStr);
				if (month > 12 || month < 1) { // <!-- >
					alert('Please enter an integer between 1 and 12 for the month.');
					return;
				} // if month
			} // if month parsable
			if (!canParseToInt(dayStr)) { // <!-- >
				alert('For the day, please enter an integer between '
						+ '1 and the number of days in the indicated month.');
				return;
			} else {
				day = parseInt(dayStr);
				if (day > 31 || day < 1 
					|| (day === 31 && [2, 4, 6, 9, 11].contains(month))
					|| (day === 30 && month === 2)
					|| (day === 29 && month === 2 
						&& !isLeapYear(parseInt(yearStr)))) { // <!-- >
					alert('For the day, please enter an integer between '
							+ '1 and the number of days in the indicated month.');
					return;
				} // if day
			} // if day parsable
		} // if timeType
		var selectedIndex = selForm.institutionId.selectedIndex;
		setValue('institutionName', instnNamesArr[selectedIndex]);
		if (confirm(new String.builder()
				.append('Remove <%= clearResponses ? "responses" : "users" %> ')
				.append(year < 2000
					? new String.builder().append('<%= clearResponses ? "" 
							: "who last logged in" %> more than ')
						.append(yearStr).append(' year(s), ') 
						.append(monthStr).append(' month(s), and ')
						.append(dayStr).append(' day(s) ')
						.append('<%= clearResponses ? "old" : "ago" %>')
						.toString()
					: new String.builder().append('<%= clearResponses ? "submitted" 
							: "who last logged in" %> before ')
						.append(yearStr).append('/').append(monthStr)
						.append('/').append(dayStr)
						.toString())
				.append(' and attending ')
				.append(instnNamesArr[selectedIndex])
				.append(' (ID = ').append(instnIdsArr[selectedIndex])
				.append(')? This step is irreversible!').toString())) {
			document.userSelForm.submit();
		} // if confirmed
	} // checkAndSubmit()

	function isLeapYear(year) {
		var year4 = year % 4 === 0;
		var year100 = year % 100 === 0;
		return (year4 && !year100) || (year100 && year % 400 === 0);
	} // isLeapYear()

	// -->
</script>
</head>

<body style="text-align:center;" onload="setSelector();">
	<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>

	<div id="contentsWithTabsWithoutFooter">
	<table class="regtext" style="width:95%; margin-left:10px; margin-right:auto; 
			border-style:none; border-collapse:collapse;">
		<tr><td class="regtext" style="padding-bottom:10px; padding-top:10px;">
			<form name="userSelForm" action="clearOld.jsp" method="post">
			<input type="hidden" name="institutionName" id="institutionName" />
			<input type="hidden" name="recordType" id="recordType" value="<%=
				clearResponses ? "responses" : "users" %>"/>
			<%= descrips[1] %> from&nbsp;&nbsp;<span id="institutionCell"></span><br/>
			<%= descrips[2] %>&nbsp; 
			<select name="timeType" id="timeType" onchange="javascript:changeTimeType();">
				<option value="moreThan"><%= descrips[3] %> more than</option>
				<option value="before"><%= descrips[4] %> before</option>
			</select>&nbsp;&nbsp;<input type="text" name="year" id="year" 
					value="6" size="3" />&nbsp;&nbsp;<span 
					name="yearsText" id="yearsText">years,</span>
			&nbsp;&nbsp;<input type="text" name="month" id="month" 
					value="0" size="3" />&nbsp;&nbsp;<span 
					name="monthsText" id="monthsText">months, and </span>
			&nbsp;&nbsp;<input type="text" name="day" id="day" 
					value="0" size="3" />&nbsp;&nbsp;<span 
					name="daysText" id="daysText">days</span>
			<span id="old" name="old"><%= descrips[5] %>.</span>
			</form>
		</td></tr>
		<tr><td style="text-align:left; padding-top:10px; padding-left:20px;">
			<table><tr>
			<td><%= makeButton(user.translate("Submit"), "checkAndSubmit();") %></td>
			<td style="text-align:right; padding-left:10px;">
			<%= makeButton(user.translate("Cancel"), "goBackAgain();") %></td>
			</tr></table>
		</td></tr>
	 </table>
	 </div>
</body>
</html>
