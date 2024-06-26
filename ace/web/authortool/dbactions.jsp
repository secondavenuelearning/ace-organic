<%@ page language="java" %>
<%@ page import="
	com.epoch.qBank.Question,
	com.epoch.session.QSet,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	
	/* dbactions handled by this action=
		"save" , "delete" , "reset" , "moveprev" , "movenext" 
		"view_responses"
	
		all are performed on qSet 
	*/
	
	QSet qSet;
	Question qBuffer;
	synchronized (session) {
		qSet = (QSet) session.getAttribute("qSet");
		qBuffer = (Question) session.getAttribute("qBuffer");
		session.removeAttribute("clone");
		session.removeAttribute("addnew");
		session.removeAttribute("qType");
		session.removeAttribute("qFlags");
		session.removeAttribute("qStmt");
		session.removeAttribute("book");
		session.removeAttribute("chapter");
		session.removeAttribute("bookQNumber");
		session.removeAttribute("keywords");
		session.removeAttribute("qBuffer");
	}
	// Utils.alwaysPrint("dbactions.jsp: qSet is ", qSet == null ? "" : "not ", "null.");
	final int qSetId = qSet.getQSetId(); 
	int qId = qBuffer.getQId();
	
	final String action = request.getParameter("action");
	final int NO_SAVE = 0;
	final int ADD_ADDNEW = 1;
	final int ADD_CLONE = 2;
	final int ADD_RETURN = 3;
	final int ADD_CONTINUE = 4;
	final int SAVE = 5;
	final int SAVE_CLONE = 6;
	final int SAVE_RETURN = 7;
	final int actionInt = ("add_addnew".equals(action) ? ADD_ADDNEW
			: "add_clone".equals(action) ? ADD_CLONE
			: "add_return".equals(action) ? ADD_RETURN
			: "add_continue".equals(action) ? ADD_CONTINUE
			: "save".equals(action) ? SAVE
			: "save_clone".equals(action) ? SAVE_CLONE
			: "save_return".equals(action) ? SAVE_RETURN
			: NO_SAVE);
	final String SHOW_MESSAGE = "qSetId=same&showmessage=yes";
	/* Utils.alwaysPrint("dbactions.jsp: qBuffer.getQSetId() = ", qSetId,
			", qSet.getQSetId() = ", qSet.getQSetId(), ", action = ", action); /**/
	
	StringBuilder endActionBld = new StringBuilder().append("question.jsp?");
	
	boolean embeddedMov = false;
	if (actionInt != NO_SAVE) {
		synchronized (session) {
			session.removeAttribute("translationObj");
		}
		final String stmnt = Utils.inputToCERs(request.getParameter("statement"));
		embeddedMov = (stmnt != null
				&& stmnt.contains("<embed") && stmnt.contains(".mov\""));
		qBuffer.setStatement(stmnt);
		final int qType = MathUtils.parseInt(request.getParameter("qType"));
		qBuffer.setQType(qType);
		final long qFlags = MathUtils.parseLong(request.getParameter("qFlags"));
		qBuffer.setQFlags(qFlags);
		qBuffer.setBook(Utils.inputToCERs(request.getParameter("book")));
		qBuffer.setChapter(Utils.inputToCERs(request.getParameter("chapter")));
		qBuffer.setRemarks(Utils.inputToCERs(request.getParameter("bookQNumber")));
		qBuffer.setKeywords(Utils.inputToCERs(request.getParameter("keywords")));
		final Question newCopyQ = (actionInt <= ADD_CONTINUE ?
				qSet.addQuestion(qBuffer) : qSet.setQuestion(qBuffer));
		if (newCopyQ == null) {
			// There was an error in saving the question
			// So buffer remains the same
			qBuffer.miscMessage = Utils.toString(" ACE could not ", 
					actionInt <= ADD_CONTINUE ? "add" : "save", 
					" the question.<br />", qSet.getDbMessage());
			endActionBld.append(SHOW_MESSAGE);
		} else {
			// Now we have a fresh instance of the question
			// make it the question buffer
			synchronized (session) {
				session.setAttribute("qBuffer", newCopyQ);
			}
			newCopyQ.miscMessage = Utils.toString(" Question ", 
					actionInt <= ADD_CONTINUE ? "added. " : "saved. ", 
					qSet.getDbMessage());
			switch (actionInt) {
				case ADD_CONTINUE:
					endActionBld.append("qId=same&"); // intentional fall-through
				case SAVE:
					endActionBld.append(SHOW_MESSAGE); break;
				case ADD_ADDNEW:
					endActionBld.append("addnew=yes"); break;
				case ADD_CLONE:
				case SAVE_CLONE:
					final int newQId = newCopyQ.getQId();
					if (newQId != 0) qId = newQId; 
					synchronized (session) {
						session.setAttribute("clone", "true");
					}
					Utils.appendTo(endActionBld, "clone=yes&qId=", 
							qId, "&savedWasNew=", actionInt == ADD_CLONE);
					break;
				case SAVE_RETURN:
					if (embeddedMov) {
						endActionBld.append(SHOW_MESSAGE); 
						break;
					}
					// else fall through
				default:
					endActionBld = Utils.getBuilder( 
							"questionsList.jsp?qSetId=", qSetId);
			} // switch
		} // if newCopyQ is null
	} else if ("reset".equals(action)) {
		Utils.appendTo(endActionBld, "qId=", qId, "&reset=true");
	} else if ("movenext".equals(action)) {
		Utils.appendTo(endActionBld, "qId=", qSet.getNextQId());
	} else if ("moveprev".equals(action)) {
		Utils.appendTo(endActionBld, "qId=", qSet.getPrevQId());
	} else if ("jump".equals(action)) {
		final int jumpQId = MathUtils.parseInt(request.getParameter("jumpvalue"));
		Utils.appendTo(endActionBld, "qId=", qSet.getQId(jumpQId));
	} else if ("view_responses".equals(action)) {
		endActionBld = Utils.getBuilder("FIHeader.jsp");
		synchronized (session) {
			session.setAttribute("qBuffer", qBuffer);
		}
	} // if action

	final String endAction = endActionBld.toString();
	// Utils.alwaysPrint("dbactions.jsp: endAction = ", endAction);

%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<script type="text/javascript">
	// <!-- >
	function finish() {
		<% if (embeddedMov) { %>
			var anAlert = 'Your question statement appears to contain an '
					+ 'embedded movie.  Because they may cause problems with '
					+ 'communication between the ACE server and users\' '
					+ 'browsers, we strongly recommend against embedding '
					+ 'movies in ACE pages.  Better to provide a hyperlink '
					+ 'that opens the movie in a separate tab or window.';
			<% if (actionInt == SAVE_RETURN) { %>
				anAlert += '\n\nACE will return you to the question-editing '
						+ 'page.  If you wish to keep the embedded movie, '
						+ 'simply press Exit w/o Saving (your changes will be '
						+ 'saved).  Otherwise, change the embedded movie to a '
						+ 'hyperlink and press Save and Exit again.';
			<% } // if save-return %>
			alert(anAlert);
		<% } // if there's a movie %>
		self.location.href = '<%= endAction %>';
	} // finish()
	// -->
	</script>
</head>
<body class="regtext" onload="finish();">
</body>
</html>
