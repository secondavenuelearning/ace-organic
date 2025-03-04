<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.session.AnonSession,
	com.epoch.textbooks.Textbook,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.HashMap,
	java.util.List,
	java.util.Map"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");

	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noAccess.html" /> <%
	}
	final InstructorSession instrSess = (InstructorSession) userSess;
	final boolean deleteBook = "true".equals(request.getParameter("deleteBook"));
	if (deleteBook) {
		final int deleteBookId = 
				MathUtils.parseInt(request.getParameter("deleteBookId"));
		instrSess.deleteTextbook(deleteBookId);
	} // if should delete a book
	final boolean INCLUDE_VISIBLE = true;
	final String userId = user.getUserId();
	final Textbook[] visibleBooks = instrSess.getTextbooks(INCLUDE_VISIBLE);
	final Textbook[] myBooks = instrSess.getTextbooks(!INCLUDE_VISIBLE);
	final Textbook[] myLockedBooks = instrSess.getLockedTextbooks();
	final EpochEntry authorEntry = new EpochEntry(userId);
	final Map<String, User> ownersByIds = new HashMap<String, User>();
	final StringBuilder visibleBld = new StringBuilder();
	for (final Textbook book : visibleBooks) {
		final String ownerId = book.getOwnerId();
		User owner = ownersByIds.get(ownerId);
		if (owner == null) {
			owner = AnonSession.getUser(ownerId);
			ownersByIds.put(ownerId, owner);
		} // if haven't stored owner yet
 		Utils.appendTo(visibleBld, "<option value=\"", 
				book.getId(), "\">", book.getName(), ": ", 
				owner.getName().toString1stName1st(
					owner.prefersFamilyName1st()), "</option>");
	} // for each book
	synchronized (session) {
		session.setAttribute("entry", authorEntry);
		session.setAttribute("textbookOwnersByIds", ownersByIds);
	}
	boolean ownBooks = false;
	final StringBuilder deleteBld = new StringBuilder();
	// final List<Integer> ownedBookIds = new ArrayList<Integer>();
	for (final Textbook book : myBooks) { 
		final String ownerId = book.getOwnerId();
		if (userId.equals(ownerId)) { 
			ownBooks = true;
			final User owner = ownersByIds.get(ownerId);
 			Utils.appendTo(deleteBld, "<option value=\"", 
					book.getId(), "\">", book.getName(), ": ", 
					owner.getName().toString1stName1st(
						owner.prefersFamilyName1st()), "</option>");
			// ownedBookIds.add(Integer.valueOf(book.getId()));
		} // if I am the owner
	} // for each book of which I am coauthor
%>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<meta http-equiv="Pragma" content="no-cache"/>
<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<title>ACE Embedded Textbooks</title>
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<style type="text/css">
		* html body {
			padding:55px 0 0px 0; 
		}
</style>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

	var amAnAuthor = new Array();
	var amOwner = new Array();
	amAnAuthor[0] = true;
	amOwner[0] = true;
	<% for (final Textbook book : myBooks) {
		final int bkId = book.getId(); %>
		amAnAuthor[<%= bkId %>] = true;
		<% if (userId.equals(book.getOwnerId())) { %>
			amOwner[<%= bkId %>] = true;
		<% } // if user is owner of this book
	} // for each book authored by this instructor %>

	var isLocked = new Array();
	var lockHolders = new Array();
	<% for (final Textbook book : myLockedBooks) { 
		final int bkId = book.getId(); 
		final User lockHolder = AnonSession.getUser(book.getLockHolder()); %>
		isLocked[<%= bkId %>] = true;
		lockHolders[<%= bkId %>] = '<%= Utils.toValidJS(
				lockHolder.getName().toString1stName1st(
					lockHolder.prefersFamilyName1st())) %>';
	<% } // for each book locked for edit by another author %>

	function chooseBook() {
		var form = document.chooseForm;
		var bookId = form.bookId.value;
		if (isLocked[bookId]) {
			if (!confirm(new String.builder().
					append('Another author of the book you have chosen to edit, ').
					append(lockHolders[bookId]).
					append(', has locked this book for editing.  Do you want to take '
						+ 'possession of the lock?').toString())) {
				return;
			} // if user changes his or her mind
			alert('Be sure that the author who has owned the lock until '
					+ 'now is no longer in an authoring session, or you '
					+ 'may write over each other\'s work.');
			form.lock.value = true;
		} // if chosen book is locked
		if (!amAnAuthor[bookId]) form.notAnAuthor.value = true;
		form.submit();
	} // chooseBook()

	function deleteBook() {
		var form = document.chooseForm;
		if (confirm('Are you sure you want to '
				+ 'delete your textbook? This act is irreversible.')) {
			form.action = 'chooseTextbook.jsp?deleteBook=true';
			form.submit();
		} // if confirmed
	} // deleteBook()

	function writeButton() {
		var bookId = document.chooseForm.bookId.value;
		var button = (bookId === '0'
				? '<%= Utils.toValidJS(makeButton("Write book", "chooseBook();")) %>'
				: '<%= Utils.toValidJS(makeButton("Edit book", "chooseBook();")) %>');
		setInnerHTML('writeBook', button);
		var bld = new String.builder();
		if (!amAnAuthor[bookId]) {
			bld.append('If you edit this textbook that you '
					+ 'have not authored, ACE will make a copy of the '
					+ 'textbook and assign the copy to you.  Thereafter, '
					+ 'any changes that you make in the textbook will '
					+ 'affect only your copy, and any changes that the '
					+ 'author of the original textbook makes will not '
					+ 'affect your copy.');
		} // if selected book is not owned by this user
		bld.append('<p class="boldtext">By using this tool, you are agreeing '
				+ 'that you will not upload copyrighted materials without '
				+ 'permission of the copyright owners. If you violate this '
				+ 'stricture, we will delete your work, and you will lose '
				+ 'access to this program.<\/p>');
		setInnerHTML('warning', bld.toString());
	} // writeButton()

	// -->
</script>
</head>
<body class="light" style="background-color:white;" onload="writeButton();">

<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
<div id="contentsWithoutTabs">

<form name="chooseForm" action="writeTextbook.jsp" method="post">
	<input type="hidden" name="saved" value="true" />
	<input type="hidden" name="lock" value="false" />
	<input type="hidden" name="notAnAuthor" value="false" />
<table style="width:95%; margin-left:auto; margin-right:auto;" summary="">
	<tr>
	<td colspan="2" class="boldtext big" 
			style="vertical-align:top; padding-top:10px; padding-bottom:10px;">
		ACE Online Textbooks
	</td>
	</tr>
	<tr>
	<td class="regtext" style="width:50%; text-align:left; padding-bottom:10px;">
		Choose a textbook to write or edit.
	</td>
	<td class="regtext" style="width:50%; text-align:left; padding-bottom:10px;">
		<% if (ownBooks) { %>
			Choose a textbook to delete.
		<% } // if I own books %>
	</td>
	</tr>
	<tr>
	<td class="regtext" style="width:50%; text-align:left; padding-bottom:10px;">
		<select name="bookId" onchange="writeButton();">
			<%= visibleBld.toString() %>
			<option value="0">New</option>
		</select>
	</td>
	<td class="regtext" style="width:50%; text-align:left; padding-bottom:10px;">
		<% if (ownBooks) { %>
			<select name="deleteBookId">
				<%= deleteBld.toString() %>
			</select>
		<% } // if I own books %>
	</td>
	</tr>
	<tr>
	<td id="writeBook">
	</td>
	<td>
		<% if (ownBooks) { %>
			<%= makeButton("Delete book", "deleteBook();") %>
		<% } // if I own books %>
	</td>
	</tr>
	<tr>
	<td id="warning" class="regtext" style="text-align:left; padding-top:20px;">
	</td>
	</tr>
</table>
</form>
</div>
</body>
</html>
