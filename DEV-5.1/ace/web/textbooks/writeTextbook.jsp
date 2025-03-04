<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.courseware.Name,
	com.epoch.session.AnonSession,
	com.epoch.textbooks.Textbook,
	com.epoch.textbooks.TextChapter,
	com.epoch.textbooks.TextContent,
	com.epoch.utils.MathUtils,
	java.util.ArrayList,
	java.util.List"
%>

<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>

<%	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");

	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noAccess.html" /> <%
	}
	// bookId is negative when reloading, otherwise we came from chooseStudents.jsp
	final int bookId = MathUtils.parseInt(request.getParameter("bookId"), -1);
	final String userId = user.getUserId();
	// Utils.alwaysPrint("writeTextbook.jsp: bookId = ", bookId, ", userId = ", userId);
	Textbook book;
	String ownerId;
	boolean amCoauthor = false;
	synchronized (session) {
		if (bookId == 0) { // new book, from chooseTextbook.jsp
			ownerId = userId;
			book = new Textbook(ownerId);
			session.setAttribute("textbook", book);
		} else {
			if (bookId < 0) { // reloading this page
				book = (Textbook) session.getAttribute("textbook");
				ownerId = book.getOwnerId();
			} else { // existing book, from chooseTextbook.jsp or discarding changes
				final InstructorSession instrSess = (InstructorSession) userSess;
				final boolean notAnAuthor = 
						"true".equals(request.getParameter("notAnAuthor"));
				/* Utils.alwaysPrint("writeTextbook.jsp: notAnAuthor = ", 
						notAnAuthor); /**/
				if (notAnAuthor) {
					ownerId = userId;
					book = new Textbook(instrSess.getTextbook(bookId), ownerId);
				} else {
					final boolean LOCK = true;
					book = instrSess.getTextbook(bookId, LOCK);
					ownerId = book.getOwnerId();
				} // if editor is not an author or coauthor
				amCoauthor = !notAnAuthor && !userId.equals(ownerId);
				session.setAttribute("textbook", book);
			} // if bookId
		} // if bookId
		session.setAttribute("lastModifiedTextbook", new Textbook(book));
	} // synchronized
	final List<TextChapter> chapters = book.getChapters();
	final int numChaps = book.getNumChapters();
	String[] allAuthorNames = null;
	synchronized (session) {
		if (book.getId() != 0) allAuthorNames = book.getAllAuthorNames();
		if (Utils.isEmpty(allAuthorNames)) {
			allAuthorNames = new String[] {user.getName().toString1stName1st(
					user.prefersFamilyName1st())};
		} // if no authors are stored for this book
		session.setAttribute("allAuthorNames", allAuthorNames);
	} // synchronized

	final boolean saved = "true".equals(request.getParameter("saved"));
	final boolean newOwner = "true".equals(request.getParameter("newOwner"));
	final User owner = AnonSession.getUser(ownerId);
	final Name ownerName = owner.getName();
	final String ownerFullName = ownerName.toString1stName1st(
			owner.prefersFamilyName1st());
	final String owner1stName = ownerName.givenName;
	final String coauthor = Utils.toString(
			"The database questions that ACE associates with "
				+ "this textbook are those of the textbook owner, ", 
			ownerFullName, ".  If you add a practice"
				+ " question to this textbook, and ", owner1stName, 
			" has modified that question, you (and your students) will see ", 
			owner1stName, "'s modified version.  Furthermore, you "
				+ "will be able to add practice questions that ", owner1stName, 
			" has written to this textbook, "
				+ "but you will not be able to add any questions that you have "
				+ "written (unless you added them to the master database).  If "
				+ "you want to write any new questions for this textbook or to "
				+ "modify existing questions, then, when you go to the "
				+ "authoring tool, choose to assign your work to ", 
			owner1stName, '.');
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<meta http-equiv="Pragma" content="no-cache"/>
<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<title>ACE Embedded Textbooks</title>
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<style type="text/css">
		#footer {
			position:absolute; 
			bottom:0; 
			left:0;
			width:100%; 
			height:50px; 
			overflow:auto; 
			text-align:right; 
		}

		* html body {
			padding:55px 0 50px 0; 
		}
</style>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>

	function goBack() {
		self.location.href = 'chooseTextbook.jsp';
	} // goBack()

	function coauthors() {
		self.location.href = 'addCoauthor.jsp';
	} // coauthors()

	function moveChapter(chapNum) {
		var form = document.bookForm;
		form.action.value = 'moveChapter';
		form.chapNum.value = chapNum;
		form.moveTo.value = getValue(new String.builder().
				append('chap').append(chapNum).append('Selector').toString());
		form.submit();
	} // moveChapter()

	function deleteChapter(chapNum) {
		if (confirm('Are you sure you want to delete an entire chapter?')) {
			var form = document.bookForm;
			form.action.value = 'deleteChapter';
			form.chapNum.value = chapNum;
			form.submit();
		} // if confirm
	} // deleteChapter()

	function openChapter(chapNum) {
		if (titleOK()) {
			var form = document.bookForm;
			form.action.value = 'openChapter';
			form.chapNum.value = chapNum;
			form.submit();
		} // if title is OK
	} // openChapter()

	function releaseLock() {
		var form = document.bookForm;
		form.action.value = 'releaseLock';
		form.submit();
	} // releaseLock()

	function save() {
		if (titleOK()) {
			var form = document.bookForm;
			form.action.value = 'save';
			form.submit();
		} // if title is OK
	} // save()

	function reset() {
		self.location.href = 'writeTextbook.jsp?bookId=<%= book.getId() %>&saved=true';
	} // reset()

	function titleOK() {
		var titleOK = !isWhiteSpace(document.bookForm.bookName.value);
		if (!titleOK) {
			toAlert('Please enter a title for the textbook.');
		} // if title is not OK
		return titleOK;
	} // titleOK()

	function makeButtons() {
		setInnerHTML('saveCell', '<%= Utils.toValidJS(makeButton(
				"Save book", "save();")) %>');
		setInnerHTML('discardCell', '<%= Utils.toValidJS(makeButton(
				"Discard changes", "reset();")) %>');
	} // makeButtons()

	function loadMessage() {
		<% if (newOwner) { %>
			var bld = new String.builder().
					append('You have made a copy of '
						+ 'another instructor\'s textbook to use as your own. '
						+ 'ACE will not incorporate any changes that you make '
						+ 'into the original textbook, and vice versa.');
			<% final List<int[]> localQIds = new ArrayList<int[]>();
			int chapNum = 0;
			for (final TextChapter chapter : book.getChapters()) {
				chapNum++;
				int contentNum = 0;
				for (final TextContent content : chapter.getContents()) {
					contentNum++;
					if (content.isACEQuestion()
							&& MathUtils.parseInt(content.getContent()) < 0) {
						localQIds.add(new int[] {chapNum, contentNum});
					} // if have locally authored ACE question
				} // for each content
			} // for each chapter
			if (!localQIds.isEmpty()) { %>
				bld.append('\n\nThe following '
						+ 'questions referenced by the original textbook '
						+ 'will not be visible to you or your students. You '
						+ 'will need to delete or replace them.\n');
				<% for (final int[] localQId : localQIds) { %>
					bld.append(\n'Chapter <%= localQId[0] %>, '
							+ 'item <%= localQId[1] %>');
				<% } // for each local Q %>
			<% } // if there are local Qs %>
			toAlert(bld.toString());
		<% } // if a new owner %>
	} // loadMessage()

	function practiceQsAndCoauthors() {
		alert('<%= Utils.toValidJS(coauthor) %>');
	} // practiceQsAndCoauthors()
	// -->

</script>
</head>
<body class="light" style="background-color:white;" 
		onload="loadMessage();<%= saved ? "" : "makeButtons();" %>">

<%@ include file="/navigation/menuHeaderHtml.jsp.h" %>
<div id="contentsWithoutTabsWithFooter">

<form name="bookForm" action="textbookActions.jsp" method="post" accept-charset="UTF-8">
	<input type="hidden" name="action" value="" />
	<input type="hidden" name="chapNum" value="" />
	<input type="hidden" name="moveTo" value="" />
	<input type="hidden" name="saved" value="<%= saved %>" />
<table style="width:95%; margin-left:auto; margin-right:auto;" summary="">
	<tr>
	<td class="boldtext big" 
			style="vertical-align:top; padding-top:10px; padding-bottom:10px;">
		ACE Online Textbooks
	</td>
	</tr>
	<tr>
	<td class="regtext" style="text-align:left; padding-bottom:10px;">
		<table style="width:100%;">
		<tr>
		<td colspan="3" class="boldtext">
			<% final int numAuthors = allAuthorNames.length;
			if (numAuthors == 1) { %>
				<%= allAuthorNames[0] %>
			<% } else if (numAuthors == 2) {  %>
				<%= allAuthorNames[0] %> and <%= allAuthorNames[1] %>
			<% } else { 
				for (int authNum = 1; authNum <= numAuthors; authNum++) { %>
					<%= allAuthorNames[authNum - 1] 
							+ (authNum == numAuthors ? "" 
								: authNum == numAuthors - 1 ? ", and" : ",") %>
				<% } // for each author
			} // if numAuthors %>
		</td>
		</tr>
		<tr>
		<td style="width:60%;">
			<input type="text" name="bookName" size="80"
					onkeypress="makeButtons();"
					value="<%= Utils.toValidTextbox(book.getName()) %>" /> 
		</td>
		<td style="padding-left:20px; text-align:left; width:40%;">
			<input type="checkbox" name="visibleToAll" 
					onchange="makeButtons();" <%= book.isVisibleToAll()
					? "checked=\"checked\"" : "" %>> visible to all?
		</td>
		<% if (userId.equals(ownerId) && book.getId() != 0) { %>
			<td style="text-align:right;">
				<%= makeButton("Manage coauthors", "coauthors();") %>
			</td>
		<% } // if this user owns the book %>
		</tr>
		</table>
	</td>
	</tr>
	<% if (amCoauthor) { %>
		<tr><td>
		<table><tr><td class="boldtext" style="padding-bottom:10px;">
			You are a coauthor but not the owner of this textbook.
		</td><td class="regtext" style="padding-bottom:10px; padding-left:10px;"
				title="<%= Utils.toValidHTMLAttributeValue(coauthor) %>">
			<a href="javascript:practiceQsAndCoauthors();">Implications?</a>
		</td></tr></table>
		</td></tr>
	<% } // if coauthor but not owner of this book %>
	<% if (numChaps > 0) { %>
		<tr>
		<td class="regtext" style="text-align:left; padding-bottom:10px;">
			<table class="whiteTable" style="width:100%; margin-left:auto; 
					margin-right:auto; border-collapse:collapse;" summary="">
			<% int chapNum = 0;
			for (final TextChapter chapter : chapters) { 
				chapNum++;
				final String rowColor = (chapNum % 2 != 0 
						? "greenrow" : "whiterow");
			%>
				<tr class="<%= rowColor %>" 
						style="padding-top:5px; border-collapse:collapse;">
				<td style="padding-left:10px; width:20px;">
					<select name="chap<%= chapNum %>Selector" 
							id="chap<%= chapNum %>Selector" 
							onchange="moveChapter(<%= chapNum %>);">
					<% for (int num = 1; num <= numChaps; num++) { %>
						<option value="<%= num %>" <%= num == chapNum 
								? "selected=\"selected\"" 
								: "" %>><%= num %></option>
					<% } // for each number %>
					</select>
				</td>
				<td style="padding-left:10px; width:20px;">
					<%= makeButtonIcon("delete", pathToRoot,
							"deleteChapter(", chapNum, ");") %>
				</td>
				<td style="padding-left:10px;">
					<a href="javascript:openChapter(<%= chapNum 
							%>);"><%= chapter.getName() %></a>
				</td>
				</tr>
			<% } // for each chapter %>
			</table>
		</td>
		</tr>
	<% } // if there are chapters %>
</table>
</form>

</div>
<div id="footer">
<table summary="navigation" style="width:95%; margin-left:auto; margin-right:auto;">
	<tr>
	<td style="width:95%;"></td>
	<td>
		<%= makeButton("Add chapter", "openChapter(0);") %>
	</td>
	<td id="saveCell">
	</td>
	<td id="discardCell">
	</td>
	<td>
		<%= makeButton("Release lock", "releaseLock();") %>
	</td>
	<td>
		<%= makeButton("Choose new book", "goBack();") %>
	</td>
	</tr>
</table>
</div>
</body>
</html>
