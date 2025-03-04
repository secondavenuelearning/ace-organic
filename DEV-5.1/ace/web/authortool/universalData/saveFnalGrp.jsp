<%@ page language="java" %>
<%@ page import="
	com.epoch.chem.FnalGroupDef,
	com.epoch.exceptions.DBException,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.List,
	java.util.Locale"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

    /* 
		grpId is  0 (new) | ID of functional group
	 */
	final int grpId = MathUtils.parseInt(request.getParameter("grpId"));
	final String grpDef = request.getParameter("grpDef");
	String grpName = Utils.inputToCERs(request.getParameter("grpName"));
	final String grpCategory = Utils.inputToCERs(request.getParameter("grpCategory"));

	if (grpDef == null || grpName == null || grpCategory == null) {
    	%> <jsp:forward page="../errorParam.jsp"/> <%
    }

	grpName = grpName.trim();
	String msg = null;
	final List<String> existingNames = 
			FnalGroupDef.getAllGroupNames(FnalGroupDef.MAKE_LOWER); 
	boolean newName = grpId == 0;
	if (!newName) {
		final FnalGroupDef newGrp = FnalGroupDef.getFnalGroupDef(grpId);
		newName = !newGrp.name.toLowerCase(Locale.US).
			equals(grpName.toLowerCase(Locale.US));
	}
	if (newName && existingNames.contains(grpName.toLowerCase(Locale.US))) {
		msg = "A functional group with that name already exists.";
	} else {
		final String grpName1 = grpName.substring(0, 1);
		grpName = grpName1.toUpperCase(Locale.US) + grpName.substring(1);
		final FnalGroupDef group = new FnalGroupDef(grpId, grpName, grpDef, grpCategory);
		try {
			group.saveFnalGroupDef();
		} catch (DBException e) {
			msg = "Unable to write functional group to the database.";
		} // try
	} // if name already exists
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
		<% if (msg == null && newName) { %>
			opener.location.reload(); 
		<% } else if (msg != null) { %>
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
