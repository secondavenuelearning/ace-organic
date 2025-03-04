<%@ page language="java" %>
<%@ page import="
	com.epoch.qBank.Figure,
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
	String figNumStr = "";
	final StringBuilder calcBld = new StringBuilder();
	while (iter.hasNext()) {
		final FileItemStream item = iter.next();
		final String name = item.getFieldName(); // expect "srcFile", "figNum"
		final InputStream inStream = item.openStream();
		if ("figNum".equals(name)) {
			figNumStr = Streams.asString(inStream);
	        /* Utils.alwaysPrint("calcUpload.jsp got form field ", name,
					" for figNum with value ", figNumStr); /**/
		} else if (!"figType".equals(name)) {
			/* Utils.alwaysPrint("calcUpload.jsp: detected file field ", name,
					" with file name ", item.getName(), 
					", going to append to calcBld"); /**/
			final int chunkSize = 1024;
			final byte[] b = new byte[chunkSize];
			int count = 0;
			while ((count = inStream.read(b, 0, chunkSize)) != -1) {
				calcBld.append(new String(b, 0, count, StandardCharsets.UTF_8));
			}
		} // a file field
		inStream.close();
	} // while iter.hasNext(), but we expect only one iteration
	// Utils.alwaysPrint("calcUpload.jsp:\n", calcBld.toString());

%>

<jsp:forward page="loadFigure.jsp">
	<jsp:param name="figNum" value="<%= figNumStr %>" />
	<jsp:param name="figType" value="<%= Figure.JMOL %>" />
	<jsp:param name="calcResults" value="<%= calcBld.toString() %>" />
</jsp:forward>
