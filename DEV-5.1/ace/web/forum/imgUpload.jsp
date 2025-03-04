<%@ page language="java" %>
<%@ page import="
	com.epoch.courseware.ForumPost,
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

	String topics50 = "";
	String topicId = "";
	String posts50 = "";
	String postId = "";
	String linkedHWId = "";
	String linkedQId = "";
	String title = "";
	String text = "";
	String sticky = "";

	String fullpath = application.getRealPath(tempfile);
	// Utils.alwaysPrint("  temppath for image ", fullpath);
	// multipart parser: Raphael 5/2008
	// based on http://commons.apache.org/fileupload/streaming.html
	final ServletFileUpload upload = new ServletFileUpload();
	final FileItemIterator iter = upload.getItemIterator(request);
	while (iter.hasNext()) {
		final FileItemStream item = iter.next();
		final String name = item.getFieldName(); // expect "srcFile"
		final InputStream inStream = item.openStream();
		if ("srcFile".equals(name)) {
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
		} else if ("topics50".equals(name)) {
			topics50 = Streams.asString(inStream);
		} else if ("topicId".equals(name)) {
			topicId = Streams.asString(inStream);
		} else if ("posts50".equals(name)) {
			posts50 = Streams.asString(inStream);
		} else if ("postId".equals(name)) {
			postId = Streams.asString(inStream);
		} else if ("linkedHWId".equals(name)) {
			linkedHWId = Streams.asString(inStream);
		} else if ("linkedQId".equals(name)) {
			linkedQId = Streams.asString(inStream);
		} else if ("title".equals(name)) {
			title = Streams.asString(inStream);
		} else if ("text".equals(name)) {
			text = Streams.asString(inStream);
		} else if ("sticky".equals(name)) {
			sticky = Streams.asString(inStream);
		} // a file field
		inStream.close();
	} // while iter.hasNext(), but we expect only one iteration

%>

<jsp:forward page="addPost.jsp">
	<jsp:param name="figType" value="<%= ForumPost.IMAGE %>" />
	<jsp:param name="figure" value="<%= tempfile %>" />
	<jsp:param name="topics50" value="<%= topics50 %>" />
	<jsp:param name="topicId" value="<%= topicId %>" />
	<jsp:param name="posts50" value="<%= posts50 %>" />
	<jsp:param name="postId" value="<%= postId %>" />
	<jsp:param name="linkedHWId" value="<%= linkedHWId %>" />
	<jsp:param name="linkedQId" value="<%= linkedQId %>" />
	<jsp:param name="title" value="<%= title %>" />
	<jsp:param name="text" value="<%= text %>" />
	<jsp:param name="sticky" value="<%= sticky %>" />
</jsp:forward>
