<%@ page language="java" %>
<%@ page import="
	com.epoch.utils.Utils,
	java.io.DataOutputStream,
	java.io.FileOutputStream,
	java.io.InputStream,
	org.apache.commons.fileupload.FileItemIterator,
	org.apache.commons.fileupload.FileItemStream,
	org.apache.commons.fileupload.servlet.ServletFileUpload"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";
	final String which = request.getParameter("which");
	final String whichTitle = which.substring(0, 1).toUpperCase()
			+ which.substring(1);
	final boolean getPhrases = "phrases".equals(which);

	final String tempfile  = "tempfiles/" + Utils.getRandName();
	final String fullpath = application.getRealPath(tempfile) + ".";

	Utils.alwaysPrint("import.jsp: temppath for qSet file ", fullpath);
	// multipart parser: Raphael 5/2008
	// based on http://commons.apache.org/fileupload/streaming.html
	final ServletFileUpload upload = new ServletFileUpload();
	final FileItemIterator iter = upload.getItemIterator(request);
	while (iter.hasNext()) {
		final FileItemStream item = iter.next();
		final String name = item.getFieldName(); // expect "setfile"
		if (item.isFormField()) {
	        Utils.alwaysPrint("*** Error: import.jsp got form field ",
					name, " instead of a file field\n");
		} else {
			Utils.alwaysPrint("import.jsp: detected file field ",
					name, " with file name ", item.getName(),
					", going to write to ", fullpath);
			final InputStream inStream = item.openStream();
			final DataOutputStream outStream = 
					new DataOutputStream(new FileOutputStream(fullpath));
			final int chunkSize = 1024;
			final byte[] b = new byte[chunkSize];
			int count = 0;
			while ((count = inStream.read(b, 0, chunkSize)) != -1 ) {
				outStream.write(b, 0, count);
			}
			outStream.close();
			inStream.close();
		} // a file field
	} // while iter.hasNext(), but we expect only one iteration
%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<title>ACE Translated <%= whichTitle %> Importer</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
		function loadExport() {
			var url = 'read<%= getPhrases ? "Phrases" : "Translations" %>.jsp';
			var toSend = 'tempfile=<%= Utils.toValidJS(fullpath) %>'; 
			callAJAX(url, toSend);
		}

	 	function updatePage() {
			if (xmlHttp.readyState === 4) { // ready to continue
				var response = xmlHttp.responseText;
				var messageValue = extractField(response, 'messageValue');
				setInnerHTML('message', messageValue);
				showCell('closeButton');
			}
		}

		function finish() {
			window.opener.location.href = 
					'<%= getPhrases ? "phraseTranslate.jsp?saved=false"
					: "translate.jsp?saved=false&qSetId=" 
							+ request.getParameter("qSetId") %>';
			self.close();
		}
	
	// -->
	</script>

<body onload="loadExport();" class="regtext" style="overflow:auto;">
<table style="width:95%">
	<tr><td class="boldtext">
		Reading <%= which %> translations file ....
		<p>(Please be patient.)
	</td></tr>
	<tr><td class="regtext" style="margin-top:20px; width:100%" id="message">
	</td></tr>
	<tr><td id="closeButton" 
			style="visibility:hidden; text-align:center; padding-top:10px; padding-left:10px;">
		<%= makeButton("Close", "finish();") %>
	</td></tr>
</table>
</body> 

</html>
