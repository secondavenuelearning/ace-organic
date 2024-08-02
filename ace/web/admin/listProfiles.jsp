<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page contentType="text/html; charset=iso-8859-1" %>
<%@ page import="
	java.util.Date,
	com.epoch.courseware.Name,
	com.epoch.utils.DateUtils,
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	// display all users 
	final AdminSession admSess = (AdminSession) userSess;
	admSess.resetActedRole(); // resets to administrator
	role = User.ADMINISTRATOR;

	final boolean sortByInstnToo = 
			"true".equals(request.getParameter("sortByInstnToo"));
	admSess.resetAllUsers(sortByInstnToo);
	String searchStr = request.getParameter("query");
	final boolean haveSearch = !Utils.isEmpty(searchStr);
	if (haveSearch) {
		searchStr = Utils.inputToCERs(searchStr);
		admSess.setSearchString(searchStr);
	} // if searching
	/* Utils.alwaysPrint("admin/listProfiles.jsp: searchStr = ", searchStr,
	 		", sortByInstnToo = ", sortByInstnToo); /**/
	final User[] users = admSess.getAllUsers();

	int numCols = 7;
	if (haveSearch) numCols++;
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Type" content="text/css;charset=utf-8" />
	<title>ACE Administration tool</title>
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
	
	var roles = [];
	<% for (int userNum = 1; userNum <= users.length; userNum++) { 
		final User oneUser = users[userNum - 1]; %>
		roles.push('<%= oneUser.getRole() %>');
	<% } %> // for each user

	function init() {
		var myCrsButton = document.getElementById('myCourses');
		if (myCrsButton) myCrsButton.style.visibility = 'hidden';
	} // init()

	function reorder() {
		self.location.href = 'listProfiles.jsp?sortByInstnToo=<%= !sortByInstnToo %>';
	} // reorder()

	function doAdminTask() {
		var val = parseInt(document.searchform.taskSelector.value);
		document.searchform.taskSelector.selectedIndex = 0;
		switch (val) {
			case 1: managePermissions(); break;
			case 2: changeDefaultLanguage(); break;
			case 3: reloadSingletons(); break;
			case 4: addEnglish(); break;
			case 5: updateDB(); break;
			case 6: getInventory(); break;
			case 7: removeOldUsers(); break;
			case 8: removeOldPostImages(); break;
			case 9: setGracePeriod(); break;
			case 10: clearOldResponses(); break;
			case 11: renameInstitution(); break;
			case 12: sendEmails(); break;
			case 13: manageAssgtTemplates(); break;
			case 14: editSelectedUser(); break;
			case 15: impersonateSelectedUser(); break;
			case 16: deleteSelectedUser(); break;
			case 17: changeInstitutionPrimaryLanguage(); break;
			case 18: verifyInstructors(); break;
			default: ;
		} // switch
	} // doAdminTask()

	function addUser() {
		self.location.href = new String.builder()
				.append('<%= pathToRoot %>profile/editProfile.jsp?editindex=0')
				.append('&goBack=')
				.append(encodeURIComponent(self.location.href))
				.toString();
	} // addUser()

	function getSelectedUserNum(task) {
		var checkboxes = document.userForm.usersChecker;
		var selectedBoxesArr = getSelected(checkboxes);
		if (selectedBoxesArr.length !== 1) {
			alert('Please choose a single user to ' + task + '.');
			document.searchform.taskSelector.selectedIndex = 0;
			return;
		}
		return selectedBoxesArr[0].index + 1; // index is 0-based, userNum is 1-based
	} // getSelectedUserNum()

	function editSelectedUser() {
		var userNum = getSelectedUserNum('edit');
		if (!isEmpty(userNum)) {
			self.location.href = new String.builder()
					.append('<%= pathToRoot %>profile/editProfile.jsp?editindex=')
					.append(userNum)
					.append('&goBack=')
					.append(encodeURIComponent(self.location.href))
					.toString();
		} // if there's a userNum
	} // editSelectedUser()

	function impersonateSelectedUser() {
		var userNum = getSelectedUserNum('impersonate');
		var role = roles[userNum - 1];
		// alert('userNum = ' + userNum + ', role = ' + role);
		if (!isEmpty(userNum)) {
			if (confirm('Warning: ACE will record any work you do '
					+ 'while impersonating this person as if they did '
					+ 'it themselves.  Are you sure you want to '
					+ 'continue?')) {
				alert('Be careful!');
				hideCell('qBank');
			} else return;
			if (role !== '<%= User.ADMINISTRATOR %>') {
				showCell('myCourses');
			}
			self.location.href = 'setImpersonatedUser.jsp?userNum=' + userNum;
		} // if there's a userNum
	} // impersonateSelectedUser()

	function deleteSelectedUser() {
		var checkboxes = document.userForm.usersChecker;
		var selectedBoxesArr = getSelected(checkboxes);
		if (selectedBoxesArr.length !== 1) {
			alert('Please choose a single user to delete.');
			document.searchform.taskSelector.selectedIndex = 0;
			return;
		}
		var userNum = selectedBoxesArr[0].index + 1; // index is 0-based, userNum is 1-based
		var userName = selectedBoxesArr[0].value;
		if (confirm(new String.builder()
				.append('Delete the account of ')
				.append(userName)
				.append('?')
				.toString())) {
			self.location.href = 'removeUser.jsp?delindex=' + userNum;
		} else {
			setAllCheckBoxes(checkboxes, false);
			document.searchform.taskSelector.selectedIndex = 0;
		}
	} // deleteSelectedUser()

	function sendEmail(userNum) {
		var checkboxes = document.userForm.usersChecker;
		var name = checkboxes[userNum - 1].value;
		finishEmails([name]);
	} // sendEmail()

	function sendEmails() {
		var checkboxes = document.userForm.usersChecker;
		var selectedNames = getSelectedValues(checkboxes);
		finishEmails(selectedNames);
	} // sendEmails()

	function finishEmails(selectedNames) {
		if (selectedNames.length == 0) {
			alert('No users have been selected.');
			return;
		} // if there are no selected users
		var bld = new String.builder();
		for (var nameNum = 0; nameNum < selectedNames.length; nameNum++) {
			bld.append(nameNum === 0 ? 'mailto:' : ',');
			bld.append(cerToUnicode(selectedNames[nameNum]));
		} // for each nameNum
		// alert(bld.toString());
		window.location.href = bld.append(';').toString();
	} // finishEmails() 

	function reloadSingletons() {
		if (confirm('Are you sure you want to reload shortcut groups, ' +
				'menu-only reagents for synthesis questions, structure ' +
				'normalization rules, text formatting rules, and other ' +
				'values stored in WEB-INF files?')) {
			self.location.href = 'reloadSingletons.jsp';
		}
	} // reloadSingletons()

	function updateDB() {
		self.location.href = 'dbUpdate/updateDB.jsp';
	}

	function changeDefaultLanguage() {
		alert('This change will be reversed if the server restarts. '
				+ 'To make the change permanent, edit the default '
				+ 'language property in WEB-INF/epoch.properties.');
		self.location.href = 'setDefaultLanguage.jsp';
	}

	function getInventory() {
		self.location.href = 'getInventory.jsp';
	}

	function clearOldResponses() {
		self.location.href = 'getHowOld.jsp?recordType=responses';
	}

	function removeOldUsers() {
		self.location.href = 'getHowOld.jsp?recordType=users';
	}

	function removeOldPostImages() {
		self.location.href = 'removeOldPostImages.jsp';
	}

	function managePermissions() {
		self.location.href = 'managePermissions.jsp';
	}

	function verifyInstructors() {
		self.location.href = 'verifyInstructors.jsp';
	}

	function setGracePeriod() {
		self.location.href = 'setPaymentGracePeriod.jsp';
	}

	function renameInstitution() {
		self.location.href = 'renameInstitution.jsp';
	}

	function addEnglish() {
		self.location.href = 'addEnglishPhrase.jsp';
	}

	function changeInstitutionPrimaryLanguage() {
		self.location.href = 'changeInstitutionPrimaryLanguage.jsp';
	} 

	function manageAssgtTemplates() {
		self.location.href = 'manageAssgtTemplates.jsp';
	} 

	function setSearchString() {
		var query = document.searchform.query.value;
		self.location.href = new String.builder()
				.append('listProfiles.jsp?sortByInstnToo=<%= sortByInstnToo %>&query=')
				.append(encodeURIComponent(query))
				.toString();
	} // setSearchString()

	// -->
	</script>
</head>
<body onload="init();" class="regtext">

<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

<div id="contentsWithoutTabsWithFooter">

<table class="regtext" 
		style="margin-top:5px; width:95%; margin-left:auto; margin-right:auto;">
	<tr><td class="boldtext big" style="width:100%;">Administration tool</td>
	<td style=""><%= makeButton("Sort by " + (sortByInstnToo 
			? "name only" : "institution, then name"), "reorder();") %>
	</td></tr>
</table>

<br/>
<form name="userForm" action="dummy">
<table class="adminTable" style="background:white;margin-left:auto; 
		margin-right:auto; width:95%;">
<tr class="greenrow">
<th><input type="checkbox" title="check all"
	onclick="setAllCheckBoxes(document.userForm.usersChecker, this.checked)" />&nbsp;All</th>
<th>Name</th>
<% if (haveSearch) { %>
	<th>Student ID number</th>
<% } %>
<th>Institution</th>
<th>Role<sup>1</sup></th>
<th>Username</th>
<th>Registration date</th>
<th>Most recent login date</th>
</tr>
<% for (int userNum = 1; userNum <= users.length; userNum++) { 
	final User oneUser = users[userNum - 1];
	final String rowColor = (userNum % 2 == 0 ? "greenrow" : "whiterow");
	final char userRole = oneUser.getRole();
	final String roleStr = (userRole == User.INSTRUCTOR 
				&& !oneUser.isEnabled() ? 
			Utils.toString('[', userRole, ']') : String.valueOf(userRole));
	String studentNum = oneUser.getStudentNum();
	if (studentNum == null) studentNum = "[none]";
	final Date lastLoginDate = oneUser.getLastLoginDate();
	final Date regDate = oneUser.getRegDate();
	final String email = oneUser.getEmail();
	final Name name = oneUser.getName();
	final String nameFamily1st = name.toString();
	final String nameFamilyLast = name.toString1stName1st();
	final String userId = oneUser.getUserId();
%>
	<tr class="<%= rowColor %>">
		<td style="width:40px; text-align:center;">
			<input type="checkbox" title="check" name="usersChecker"
					style="visibility:<%= userRole == User.ADMINISTRATOR 
						? "hidden" : "visible" %>;"
					value="<%= Utils.toValidHTMLAttributeValue(
						nameFamilyLast.replaceAll(",", "")) %> <<%= email %>>" />
		</td>
		<td style="width:200px;">	
			<a href="javascript:sendEmail(<%= userNum %>);"> <%= nameFamily1st %> </a>
		</td>
		<% if (userRole == User.STUDENT) { %>
			<td style="width:100px;"><%= studentNum %></td>
		<% } else if (haveSearch) { %>
			<td></td>
		<% } %>
		<td style="width:300px;"><%= oneUser.getShortInstitutionName() %></td>
		<td style="width:30px; text-align:center;"><%= 
			userRole == User.INSTRUCTOR && !oneUser.isEnabled() 
					? Utils.toString('[', User.INSTRUCTOR, ']') 
					: userRole
		%></td>
		<td style="width:100px;">&nbsp;&nbsp;<%= userId %></td>
		<td style="width:150px;">&nbsp;&nbsp;<%= DateUtils.getStringDate(regDate) %></td>
		<td style="width:150px;">&nbsp;&nbsp;<%= DateUtils.getStringDate(lastLoginDate) %></td>
	</tr>
<% } // for each user %> 

</table>
<table class="regtext" style="background:white;margin-left:auto; 
		margin-right:auto; width:95%;">
<tr class="whiterow">
<td>
	<sup>1</sup><%= User.ADMINISTRATOR %> = administrator &amp; master author,
	<%= User.INSTRUCTOR %> = instructor,
	[<%= User.INSTRUCTOR %>] = unverified instructor,
	<%= User.STUDENT %> = student.
</td>
</tr>
</table>
</form>
</div>

<div id="footer">
<form name="searchform" method="post" action="javascript:setSearchString();"
		accept-charset="UTF-8">
<table class="boldtext" style="width:95%; margin-top:10px; margin-left:auto; 
		margin-right:auto; border-style:none; border-collapse:collapse;"
		summary="">
	<tr>
	<td class="boldtext" style="text-align:left; padding-right:10px; width:auto;">
		<select name="taskSelector" onchange="doAdminTask();">
			<option value="0">Choose a task...</option>
			<option value="12">Send emails to selected users</option> 
			<option value="14">Edit the selected user</option> 
			<option value="15">Impersonate the selected user</option> 
			<option value="16">Delete the selected user</option> 
			<option value="18">Verify newly registered instructors</option> 
			<option value="1">Manage author and translator permissions</option>
			<option value="3">Reload values in WEB-INF files</option>
			<option value="6">Export question inventory</option>
			<option value="13">Manage assignments templates</option>
			<option value="10">Clear old responses</option> 
			<option value="7">Remove obsolete students</option> 
			<option value="8">Remove old forum images</option> 
			<option value="5">Update database upon upgrading JChem or ACE</option>
			<option value="4">Add an English phrase for translation</option>
			<option value="11">Rename an institution</option>
			<option value="17">Change an institution's primary language of instruction</option>
			<option value="2">Change the default language</option>
			<option value="9">Set an institution's payment grace period</option>
		</select>
	</td>
	<td style="padding:0px; text-align:right margin-right:auto;">
		<table>
		<tr><td style="width:100%; text-align:right;">
		or search for students whose surnames begin with:&nbsp;
		</td><td>
		<input name="query" type="text" 
				value="<%= Utils.toValidTextbox(searchStr) %>" size="20"/>
		</td><td>
		<%= makeButton("Search", "setSearchString();") %>
		</td><td>
		</tr>
		</table>
	</td>
	</tr>
</table>
</form>
</div>

</body>
</html>
