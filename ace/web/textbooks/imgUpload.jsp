<%@ page language="java" %>
<%@ page import="
	com.epoch.textbooks.TextContent,
	com.epoch.utils.Utils,
	java.io.DataOutputStream,
	java.io.FileOutputStream,
	java.io.InputStream,
	org.apache.commons.fileupload.FileItemIterator,
	org.apache.commons.fileupload.FileItemStream,
	org.apache.commons.fileupload.servlet.ServletFileUpload,
	org.apache.commons.fileupload.util.Streams"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>

<%
	String tempfile = Utils.toString("tempfiles/", Utils.getRandName());
	// add any subdirs when getting real path

	String fullpath = application.getRealPath(tempfile);
	/* Utils.alwaysPrint("imgUpload.jsp: temppath for image ", fullpath); /**/
	// multipart parser: Raphael 5/2008
	// based on http://commons.apache.org/fileupload/streaming.html
	final ServletFileUpload upload = new ServletFileUpload();
	final FileItemIterator iter = upload.getItemIterator(request);
	String chapNumStr = "";
	String contentNumStr = "";
	String contentTypeStr = "";
	while (iter.hasNext()) {
		final FileItemStream item = iter.next();
		final String name = item.getFieldName();
		final InputStream inStream = item.openStream();
		if ("contentNum".equals(name)) {
			contentNumStr = Streams.asString(inStream);
	        /* Utils.alwaysPrint("imgUpload.jsp got form field ",
					name, " for contentNum with value ", contentNumStr); /**/
		} else if ("contentType".equals(name)) {
			contentTypeStr = Streams.asString(inStream);
	        /* Utils.alwaysPrint("imgUpload.jsp got form field ",
					name, " for contentType with value ", contentTypeStr); /**/
		} else if ("chapNum".equals(name)) {
			chapNumStr = Streams.asString(inStream);
	        /* Utils.alwaysPrint("imgUpload.jsp got form field ",
					name, " for chapNum with value ", chapNumStr); /**/
		} else { // "srcFile", a file field
			final String origFileName = item.getName();
			final String extension = Utils.getExtension(origFileName);
			fullpath = Utils.toString(fullpath, '.', extension);
			tempfile = Utils.toString(tempfile, '.', extension);
			/* Utils.alwaysPrint("imgUpload.jsp: detected file field ", name,
					" with file name ", origFileName, ", going to write to ",
					fullpath, ", tempfile = ", tempfile); /**/
			final DataOutputStream outStream =
					new DataOutputStream(new FileOutputStream(fullpath));
			final int chunkSize = 1024;
			final byte[] b = new byte[chunkSize];
			int count = 0;
			while ((count = inStream.read(b, 0, chunkSize)) != -1) {
				outStream.write(b, 0, count);
			}
			outStream.close();
		} // a file field
		inStream.close();
	} // while iter.hasNext(), but we expect only one iteration

	/* Utils.alwaysPrint("imgUpload.jsp: returning contentNum ", contentNumStr); /**/

%>

<jsp:forward page="loadContent.jsp">
	<jsp:param name="chapNum" value="<%= chapNumStr %>" />
	<jsp:param name="contentNum" value="<%= contentNumStr %>" />
	<jsp:param name="contentType" value="<%= contentTypeStr %>" />
	<jsp:param name="uploadedfile" value="<%= tempfile %>" />
</jsp:forward>
