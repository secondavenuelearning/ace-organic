<%@ page language="java" %>
<%@ page import="
	chemaxon.calculations.ElementalAnalyser,
	chemaxon.formats.MolImporter,
	chemaxon.struc.Molecule,
	com.epoch.utils.Utils,
	java.text.NumberFormat"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String mrvStr = request.getParameter("mrvStr");
	final Molecule wholeMol = (Utils.isEmpty(mrvStr) ? new Molecule()
			: MolImporter.importMol(mrvStr));
	final Molecule[] frags = wholeMol.clone().convertToFrags();
	final boolean oneFrag = frags.length == 1;
	final NumberFormat numberFormat = NumberFormat.getInstance();
	numberFormat.setMaximumFractionDigits(5);
	numberFormat.setMinimumFractionDigits(5);
	final ElementalAnalyser elemanal = new ElementalAnalyser();
%>
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>MarvinJS elemental analysis window</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
</head>
<body style="overflow:auto;">
<% for (int molNum = 0; molNum <= frags.length; molNum++) {
	if (oneFrag && molNum == 1) continue;
	final Molecule mol = (molNum == 0 ? wholeMol : frags[molNum - 1]);
	elemanal.setMolecule(mol);
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
	final String composn = elemanal.composition(2)
			.replaceAll("\\.(\\d)%", "\\.$10%") // 1 digit after decimal -> 2
			.replaceAll("\\((\\d+)%", "($1.00%"); // 0 decimal digits -> 2
%>
	<%= oneFrag ? "Formula"
			: molNum == 0 ? "Total formula"
			: Utils.toString("Formula of fragment ", molNum) %>:
		<%= Utils.toDisplay(elemanal.isotopeFormula()) %><%= chgBld.toString() %>
	<br/>
	Molecular weight: <%= elemanal.mass() %> g / mol
	<br/>
	Exact mass: <%= numberFormat.format(elemanal.exactMass()) %> amu
	<br/>
	Elemental composition: <%= composn %>
	</p>
	<p>
<% } // for each analysis %>
</body>
</html>
