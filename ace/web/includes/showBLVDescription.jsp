<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.Molecule,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String mrvStr = request.getParameter("mrvStr");
	final Molecule wholeMol = (Utils.isEmpty(mrvStr) ? new Molecule()
			: MolImporter.importMol(mrvStr));
	final Molecule[] frags = wholeMol.clone().convertToFrags();
	final int numFrags = frags.length;
%>
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Description of window contents for BLV people</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
</head>
<body style="overflow:auto;">
<%= Utils.toDisplay(mrvStr) %>
<p>The MarvinJS drawing contains <%= numFrags %> 
molecule<%= numFrags == 1 ? "" : "s" %>.  
</p>
<% for (int molNum = 0; molNum < frags.length; molNum++) {
	final Molecule mol = frags[molNum];
	final int chg = mol.getTotalCharge();
	final StringBuilder chgBld = new StringBuilder();
	if (chg != 0) {
		chgBld.append("<sup>");
		final boolean isNeg = chg < 0;
		final int absChg = (isNeg ? -chg : chg);
		if (absChg != 1) chgBld.append(absChg);
		chgBld.append(isNeg ? "&minus;" : '+');
		chgBld.append("</sup>");
	} // if there's a charge 
%>
	<% if (numFrags > 1) { %>
		<p>Molecule <%= molNum + 1 %>
	<% } else { %>
		<p>The molecule
	<% } // if numFrags %>
	has the formula <%= Utils.toDisplay(mol.getFormula()) %><%= chgBld.toString() %>.</p>
<% } // for each fragment %>
</body>
</html>
