<%@ page language="java" %>
<%@ page import="
	com.epoch.qBank.Figure,
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
	// Utils.alwaysPrint("  temppath for image ", fullpath);
	// multipart parser: Raphael 5/2008
	// based on http://commons.apache.org/fileupload/streaming.html
	final ServletFileUpload upload = new ServletFileUpload();
	final FileItemIterator iter = upload.getItemIterator(request);
	String figNumStr = "";
	String figTypeStr = "";
	while (iter.hasNext()) {
		final FileItemStream item = iter.next();
		final String name = item.getFieldName(); // expect "srcFile", "figNum"
		final InputStream inStream = item.openStream();
		if ("figNum".equals(name)) {
			figNumStr = Streams.asString(inStream);
	        /* Utils.alwaysPrint("imgUpload.jsp got form field ",
					name, " for figNum with value ", figNumStr); /**/
		} else if ("figType".equals(name)) {
			figTypeStr = Streams.asString(inStream);
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

	// Utils.alwaysPrint("imgUpload.jsp: ret figNum ", figNumStr);

%>

<jsp:forward page="loadFigure.jsp">
	<jsp:param name="figNum" value="<%= figNumStr %>" />
	<jsp:param name="figType" value="<%= figTypeStr %>" />
	<jsp:param name="uploadedfile" value="<%= tempfile %>" />
</jsp:forward>
