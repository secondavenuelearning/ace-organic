<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolFormatException,
	chemaxon.struc.Molecule,
	com.epoch.chem.ChemUtils,
	com.epoch.substns.RGroupCollection,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	final int rGroupClassNum = MathUtils.parseInt(request.getParameter("rGroupNum"));
	final String rGroupClassName = Utils.inputToCERs(request.getParameter("rGroupName"));
	final String rGroupsStr = request.getParameter("rGroupDef");

	if (Utils.isEmpty(rGroupsStr) || Utils.isEmpty(rGroupClassName)) {
    	%> <jsp:forward page="../errorParam.jsp"/> <%
    }

	// Utils.alwaysPrint("saveRGroupColl.jsp: rGroupsStr = ", rGroupsStr);
	final String[] rGroupStrs = rGroupsStr.split(",");
	// check validity of the listed abbreviations: could be atoms, shortcut
	// groups, or formulas. shortcut groups need ot be recognized by JChem or in
	// the abbreviation definition file in WEB-INF
	String badRGroup = null;
	boolean proceed = false;
	if (rGroupStrs.length > 1) {
		for (final String rGroupStr : rGroupStrs) {
			try {
				final Molecule rGroupMol = 
						ChemUtils.getSGroupMolecule(rGroupStr.trim());
			} catch (MolFormatException e) {
				badRGroup = rGroupStr;
				break;
			} // try
		} // for each rGroup
		proceed = badRGroup == null;
		if (proceed) {
			final RGroupCollection rGroupClass = new RGroupCollection(
					rGroupClassNum, rGroupClassName.trim(), rGroupStrs);
			rGroupClass.setRGroupCollection();
		} // if all of the R groups are interpretable
	} // if there's more than one R group in this collection

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

	function proceed() { 
		<% if (proceed) { %>
			opener.location.reload(); 
		<% } else if (rGroupStrs.length <= 1) { %>
			alert('An R group collection must contain more than one R group.');
		<% } else { %>
			alert('ACE does not recognize the shortcut group <%= badRGroup %>.');
		<% } // if proceed %>
		self.close();
	} // proceed

	// -->
	</script>
</head>
<body onload="proceed();">
</body>
</html>
