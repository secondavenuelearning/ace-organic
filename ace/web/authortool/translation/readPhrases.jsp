<%@ page language="java" %>
<%@ page import="
	com.epoch.translations.PhraseTransln,
	com.epoch.utils.Utils,
	java.io.BufferedReader,
	java.io.EOFException,
	java.io.File,
	java.io.FileInputStream,
	java.io.InputStreamReader,
	java.io.IOException,
	java.nio.charset.StandardCharsets,
	java.util.ArrayList,
	java.util.List"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	final String tempfile = request.getParameter("tempfile");
	
	PhraseTransln translator;
	synchronized (session) {
		translator = (PhraseTransln) 
				session.getAttribute("phraseTranslator");
	}
	final String[] phrases = translator.allPhrases;
	final String[] translations = translator.translations;
	final List<Integer> phraseIds = new ArrayList<Integer>();
	for (final String phrase : phrases) {
		phraseIds.add(Integer.valueOf(PhraseTransln.getPhraseId(phrase)));
	}

	String message = "";
	int entryCt = 0;
	int written = 0;
	BufferedReader rdr = null;
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
				final String[] phraseData = line.split("\t");
				if (phraseData.length != 2) {
					Utils.alwaysPrint("readPhrases.jsp: "
							+ "malformed entry:\n'", line, "'");
				} else {
					entryCt++;
					final Integer phraseId = Integer.decode(phraseData[0]);
					final int posn = phraseIds.indexOf(phraseId);
					if (posn >= 0) {
						/* if (translations[posn] == null)
							Utils.alwaysPrint("readPhrases.jsp: adding:\n",
									phraseData[1]);
						else Utils.alwaysPrint("readPhrases.jsp: replacing:\n",
								translations[posn], "\nwith:\n", phraseData[1]); /**/
						translations[posn] = Utils.inputToCERs(phraseData[1]);
						written++;
					} else Utils.alwaysPrint("readPhrases.jsp: no English phrase "
							+ "with ID ", phraseData[0], " corresponding to: ", 
							phraseData[1]);
				}
			} catch (EOFException e) {
				Utils.alwaysPrint("readPhrases.jsp: "
						+ "EOFException; ending reading."); 
				break;
			} catch (NumberFormatException e) {
				Utils.alwaysPrint("readPhrases.jsp: "
						+ "NumberFormatException when loading entry:\n " 
						+ "'", line, "':\n", e.getMessage());
			} // try
		} // while true
		synchronized (session) {
			session.removeAttribute("phraseTranslator");
			session.setAttribute("phraseTranslator", translator);
		}
		message = Utils.toString("Imported ", written, " translated phrase(s) out of ", 
				entryCt, " line(s) read.  <p>Press <b>Save</b> on the " 
					+ "translations page if you want to save them.");
	} catch (IOException e1) {
		Utils.alwaysPrint("readPhrases.jsp: error in loading file.");
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
