<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.utils.Utils,
	java.io.DataOutputStream,
	java.io.FileOutputStream,
	java.io.InputStream,
	org.apache.commons.fileupload.FileItemIterator,
	org.apache.commons.fileupload.FileItemStream,
	org.apache.commons.fileupload.servlet.ServletFileUpload,
	org.apache.commons.fileupload.util.Streams"
%>

<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	if (role != User.INSTRUCTOR) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}
	String tempfile = "tempfiles/" + Utils.getRandName();
	String fullpath = application.getRealPath(tempfile);
	// Utils.alwaysPrint("fullpath for tab-delimited spreadsheet: ", fullpath);
	// multipart parser: Raphael 7/2008
	// based on http://commons.apache.org/fileupload/streaming.html
	final ServletFileUpload upload = new ServletFileUpload();
	final FileItemIterator iter = upload.getItemIterator(request);
	String index_str = "";
	while (iter.hasNext()) {
		final FileItemStream item = iter.next();
		final String name = item.getFieldName();
		final InputStream inStream = item.openStream();
		if ("index".equals(name)) {
			index_str = Streams.asString(inStream);
			/* Utils.alwaysPrint("fileUpload.jsp got form field ",
					name, " with value ", index_str); /**/
		} else { // a file field
			final String origFileName = item.getName();
			final String extension = "txt";
			fullpath = Utils.toString(fullpath, '.', extension);
			tempfile = Utils.toString(tempfile, '.', extension);
			/* Utils.alwaysPrint("fileUpload.jsp: detected file field ", name, 
					" with file name ", origFileName, ", going to write to ",
					fullpath, ", tempfile = ", tempfile); /**/
			final DataOutputStream outStream =
					new DataOutputStream(new FileOutputStream(fullpath));
			final int chunkSize = 1024;
			final byte[] b = new byte[chunkSize];
			int count = 0;
			while ((count = inStream.read(b, 0, chunkSize)) != -1 ) {
				outStream.write(b, 0, count);
			}
			outStream.close();
		} // a file field
		inStream.close();
	} // while iter.hasNext(), but we expect only one iteration
%>

<jsp:forward page="batchEnrollment.jsp">
	<jsp:param name="uploadedfile" value="<%= tempfile %>" />
</jsp:forward>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
</head>
</html>
 

