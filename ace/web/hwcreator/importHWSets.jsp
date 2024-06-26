<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.session.ExportImportSession,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.io.InputStream,
	java.nio.charset.StandardCharsets,
	org.apache.commons.fileupload.FileItemIterator,
	org.apache.commons.fileupload.FileItemStream,
	org.apache.commons.fileupload.servlet.ServletFileUpload"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	final String pathToRoot = "../";
	if (!Utils.among(role, User.ADMINISTRATOR, User.INSTRUCTOR)) {
		%> <jsp:forward page="/errormsgs/noSession.html" /> <%
	}

	String hwSetsStr;
	final boolean fromDb = "true".equals(request.getParameter("fromDb"));
	if (fromDb) { // using template from database
		final String templateIdStr = request.getParameter("templateId");
		final int templateId = MathUtils.parseInt(templateIdStr);
		/*/ Utils.alwaysPrint("importHWSets.jsp: templateIdStr = ", 
				templateIdStr, ", templateId = ", templateId); /**/
		hwSetsStr = (role == User.ADMINISTRATOR ? (AdminSession) userSess
				: (InstructorSession) userSess).getAssgtsTemplate(templateId);
	} else { // upload a course file
		// multipart parser: Raphael 5/2008
		// based on http://commons.apache.org/fileupload/streaming.html
		final StringBuilder hwSetsStrBld = new StringBuilder();
		final ServletFileUpload upload = new ServletFileUpload();
		final FileItemIterator iter = upload.getItemIterator(request);
		while (iter.hasNext()) {
			final FileItemStream item = iter.next();
			final String name = item.getFieldName(); // expect "coursefile"
			final InputStream inStream = item.openStream();
			if ("coursefile".equals(name)) { 
				/*/ Utils.alwaysPrint("importHWSets.jsp: detected file field ",
						name, " with file name ", item.getName(),
						", going to append to hwSetsStrBld"); /**/
				final int chunkSize = 1024;
				final byte[] b = new byte[chunkSize];
				int count = 0;
				while ((count = inStream.read(b, 0, chunkSize)) != -1) {
					hwSetsStrBld.append(new String(b, 0, count,
						StandardCharsets.UTF_8));
				}
			} // a file field
			inStream.close();
		} // while iter.hasNext(), but we expect only one iteration
		hwSetsStr = hwSetsStrBld.toString();
	} // uploaded file or file from database?

	/*/ Utils.alwaysPrint("importHWSets.jsp: hwSetsStr length = ",
	 		Utils.getLength(hwSetsStr)); /**/
	final int numNewHWs = /**/ ExportImportSession.importHWSets(
			hwSetsStr, user.getUserId(), course.getId());
	(role == User.ADMINISTRATOR ? (AdminSession) userSess
			: (InstructorSession) userSess).refreshAssgts();

%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Assignment Import</title>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- >

	function finishUp() {
		toAlert('<%= user.translateJS("***1*** assignment(s) imported", 
				numNewHWs) %>');
		document.location.href = 'hwSetList.jsp';
	} // finishUp()

	// -->
	</script>
</head>
<body onload="finishUp();">
</body>
</html>
