<%@ page language="java" %>
<%@ page import="
	com.epoch.textbooks.TextContent,
	com.epoch.utils.Utils,
	java.io.InputStream,
	java.nio.charset.StandardCharsets,
	org.apache.commons.fileupload.FileItemIterator,
	org.apache.commons.fileupload.FileItemStream,
	org.apache.commons.fileupload.servlet.ServletFileUpload,
	org.apache.commons.fileupload.util.Streams"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>

<%
	// multipart parser: Raphael 5/2008
	// based on http://commons.apache.org/fileupload/streaming.html
	final ServletFileUpload upload = new ServletFileUpload();
	final FileItemIterator iter = upload.getItemIterator(request);
	String chapNumStr = "";
	String contentNumStr = "";
	final StringBuilder calcBld = new StringBuilder();
	while (iter.hasNext()) {
		final FileItemStream item = iter.next();
		final String name = item.getFieldName();
		final InputStream inStream = item.openStream();
		if ("contentNum".equals(name)) {
			contentNumStr = Streams.asString(inStream);
	        /* Utils.alwaysPrint("imgUpload.jsp got form field ",
					name, " for contentNum with value ", contentNumStr); /**/
		} else if ("chapNum".equals(name)) {
			chapNumStr = Streams.asString(inStream);
	        /* Utils.alwaysPrint("imgUpload.jsp got form field ",
					name, " for chapNum with value ", chapNumStr); /**/
		} else { // "srcFile", a file field
			Utils.alwaysPrint("calcUpload.jsp: detected file field ", name,
					" with file name ", item.getName(), ", going to append to calcBld");
			final int chunkSize = 1024;
			final byte[] b = new byte[chunkSize];
			int count = 0;
			while ((count = inStream.read(b, 0, chunkSize)) != -1) {
				calcBld.append(new String(b, 0, count, StandardCharsets.UTF_8));
			}
		} // a file field
		inStream.close();
	} // while iter.hasNext(), but we expect only one iteration
	/* Utils.alwaysPrint("calcUpload.jsp:\n", calcBld.toString()); /**/

%>

<jsp:forward page="loadContent.jsp">
	<jsp:param name="chapNum" value="<%= chapNumStr %>" />
	<jsp:param name="contentNum" value="<%= contentNumStr %>" />
	<jsp:param name="contentType" value="<%= TextContent.JMOL %>" />
	<jsp:param name="calcResults" value="<%= calcBld.toString() %>" />
</jsp:forward>
