<%@ page language="java" %>
<%@ page import="
	com.epoch.translations.QSetTransln,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.io.BufferedReader,
	java.io.EOFException,
	java.io.File,
	java.io.FileInputStream,
	java.io.InputStreamReader,
	java.io.IOException,
	java.nio.charset.StandardCharsets"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	final String tempfile = request.getParameter("tempfile");
	
	QSetTransln translator;
	synchronized (session) {
		translator = (QSetTransln) 
				session.getAttribute("translationObj");
	}

	final String[] qStmts = translator.qStmts;
	final String[][] qdTexts = translator.qdTexts;
	final String[][] evalFeedbacks = translator.evalFeedbacks;

	final String HEADER_TAG = QSetTransln.HEADER_TAG;
	final String QSTMT_TAG = QSetTransln.QSTMT_TAG;
	final String QDTEXT_TAG = QSetTransln.QDTEXT_TAG;
	final String FEEDBACK_TAG = QSetTransln.FEEDBACK_TAG;

	String message = "";
	int entryCt = 0;
	BufferedReader rdr = null;
	int written = 0;
	int overwrote = 0;
	try {
		// rdr = new BufferedReader(new FileReader(new File(tempfile)));
		rdr = new BufferedReader(
			new InputStreamReader(
				new FileInputStream(tempfile), StandardCharsets.UTF_8
			)
		);
		while (true) {
			String line = null;
			try {
				line = rdr.readLine();
				if (line == null) break;
				if (Utils.isWhitespace(line)) continue;
				// format: type [tab] Q number [tab] item number [tab] data
				final String[] phraseData = line.split("\t");
				if (phraseData.length != 4) {
					Utils.alwaysPrint("readTranslations.jsp: "
							+ "malformed entry:\n'", line, "'");
				} else {
					entryCt++;
					if (HEADER_TAG.equals(phraseData[0])) {
						if (!Utils.isEmpty(translator.header)) {
							overwrote++;
						}
						translator.header = Utils.inputToCERs(phraseData[3]);
						/* Utils.alwaysPrint("readTranslations.jsp: "
								+ "saving header: ", translator.header); /**/
					} else if (QSTMT_TAG.equals(phraseData[0])) {
						final int qNum = MathUtils.parseInt(phraseData[1]);
						if (!Utils.isEmpty(qStmts[qNum - 1])) {
							overwrote++;
						}
						qStmts[qNum - 1] = Utils.inputToCERs(phraseData[3]);
						/* Utils.alwaysPrint("readTranslations.jsp: "
								+ "saving qStmt for Q ", qNum, ": ", 
								qStmts[qNum - 1]); /**/
					} else if (QDTEXT_TAG.equals(phraseData[0])) {
						final int qNum = MathUtils.parseInt(phraseData[1]);
						final int qdNum = MathUtils.parseInt(phraseData[2]);
						if (!Utils.isEmpty(qdTexts[qNum - 1][qdNum - 1])) {
							overwrote++;
						}
						qdTexts[qNum - 1][qdNum - 1] = 
								Utils.inputToCERs(phraseData[3]);
						/* Utils.alwaysPrint("readTranslations.jsp: "
								+ "saving qDatum ", qdNum, " for Q ", qNum, 
								": ", qdTexts[qNum - 1][qdNum - 1]); /**/
					} else if (FEEDBACK_TAG.equals(phraseData[0])) {
						final int qNum = MathUtils.parseInt(phraseData[1]);
						final int evalNum = MathUtils.parseInt(phraseData[2]);
						if (!Utils.isEmpty(evalFeedbacks[qNum - 1][evalNum - 1])) {
							overwrote++;
						}
						evalFeedbacks[qNum - 1][evalNum - 1] = 
								Utils.inputToCERs(phraseData[3]);
						/* Utils.alwaysPrint("readTranslations.jsp: "
								+ "saving feedback ", evalNum, " for Q ", qNum, 
								": ", evalFeedbacks[qNum - 1][evalNum - 1]); /**/
					} else {
						Utils.alwaysPrint("readTranslations.jsp: "
								+ "Unrecognized datum type '", phraseData[0], 
								"' with ", phraseData[0].length(), " characters.");
						written--;
					} // if datum type
					written++;
				} // if there are four fields
			} catch (ArrayIndexOutOfBoundsException e) {
				Utils.alwaysPrint("readTranslations.jsp: "
						+ "ArrayIndexOutOfBoundsException when loading entry:\n " 
						+ "'", line, "':\n");
			} catch (NumberFormatException e) {
				Utils.alwaysPrint("readTranslations.jsp: "
						+ "NumberFormatException when loading entry:\n " 
						+ "'", line, "':\n", e.getMessage());
			} catch (EOFException e) {
				/* Utils.alwaysPrint("readTranslations.jsp: "
						+ "EOFException; ending reading."); /**/
				break;
			} // try
		} // while true
		synchronized (session) {
			session.removeAttribute("translationObj");
			session.setAttribute("translationObj", translator);
		}
		final StringBuilder msgBld = Utils.getBuilder("Imported ", written, 
				" translated item(s) out of ", entryCt, " lines read.  ");
		if (overwrote > 1) {
			Utils.appendTo(msgBld, "<p>", overwrote, " existing translations were "
						+ "overwritten by imported translations.</p>");
		} else if (overwrote == 1) {
			Utils.appendTo(msgBld, overwrote, " existing translation was overwritten "
						+ "by an imported translation.</p>");
		} // if overwrote
		Utils.appendTo(msgBld, "<p>Press <b>Save</b> on the translations page "
				+ "if you want to save the translations.</p>");
		message = msgBld.toString();
	} catch (IOException e1) {
		Utils.alwaysPrint("readTranslations.jsp: error in loading file.");
		e1.printStackTrace();
	} finally {
		try {
			rdr.close();
		} catch (Exception e) { }
	} // try

%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
</head>
<body onload="hideDoneMessage()">
		<!-- messageValue = @@@@<%= message %>@@@@ -->
</body>
</html>
