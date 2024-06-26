<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolFormatException,
	com.epoch.exceptions.DBException,
	com.epoch.synthesis.SynthMenuOnlyRgts,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	final String oldName = request.getParameter("oldName"); // null if new entry
	final String smDef = request.getParameter("smDef");
	final String smName = Utils.inputToCERs(request.getParameter("smName"));
	final String[] newMenuOnlyRgt = new String[] {smName, smDef};

	if (smDef == null || smName == null) {
    	%> <jsp:forward page="../errorParam.jsp"/> <%
    }

	String msg = "";
	try {
		SynthMenuOnlyRgts.saveMenuOnlyRgt(newMenuOnlyRgt, oldName);
	} catch (MolFormatException e) {
		msg = "The SMILES definition was invalid.  "
				+ "The reagent definition was not saved.";
		e.printStackTrace();
	} catch (DBException e) {
		msg = "A problem occurred.  Your changes were not saved.";
		e.printStackTrace();
	}

%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<script type="text/javascript">
	// <!-- >
	function finish() {
		<% if ("".equals(msg)) { %>
			opener.location.reload(); 
		<% } else { %>
			alert('<%= msg %>');
		<% } %>
		self.close();
	}
	// -->
	</script>
</head>
<body onload="finish();">
</body>

</html>
