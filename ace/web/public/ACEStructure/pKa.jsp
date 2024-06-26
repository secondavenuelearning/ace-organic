<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.marvin.calculations.pKaPlugin,
	chemaxon.struc.MolAtom,
	chemaxon.struc.Molecule,
	chemaxon.struc.MoleculeGraph,
	chemaxon.struc.PeriodicSystem,
	com.epoch.AppConfig,
	com.epoch.chem.ChemUtils,
	com.epoch.chem.MolString,
	com.epoch.chem.pKaFunctions,
	com.epoch.qBank.Question,
	com.epoch.utils.Utils,
	java.text.NumberFormat"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final String substrate = request.getParameter("substrate");
	final boolean useJChem = "JChem".equals(request.getParameter("method"));
	Molecule origMolecule = null;
	Molecule molecule = null;
	String[] pKas;
	String[] pKbs;
	final double[] molpKas = {0, 0, 0};
	final double[] molpKbs = {0, 0, 0};
	final int[] molpKaAtms = {0, 0, 0};
	final int[] molpKbAtms = {0, 0, 0};
	int numAtoms = 0;
	int origNumAtoms = 0;
	final NumberFormat numberFormat = NumberFormat.getInstance(); 
	numberFormat.setMaximumFractionDigits(1);
	numberFormat.setMinimumFractionDigits(1);
	try {
	if (substrate != null) {
		origMolecule = MolImporter.importMol(substrate);
		molecule = MolImporter.importMol(substrate);
		 Utils.alwaysPrint("pKa.jsp: molecule:\n",
				MolString.toString(molecule, Utils.MRV)); /**/
		origNumAtoms = molecule.getAtomCount();
		molecule.aromatize(MoleculeGraph.AROM_BASIC);
		ChemUtils.stripMetalsNoClone(molecule);
		 Utils.alwaysPrint("pKa.jsp: after aromatizing and stripping metals, molecule:\n",
				MolString.toString(molecule, Utils.MRV)); /**/
		numAtoms = molecule.getAtomCount();
		pKas = new String[numAtoms];
		pKbs = new String[numAtoms];
		final pKaPlugin plugin = new pKaPlugin();
		if (useJChem) {
			plugin.setMaxIons(8);  // default 8
			plugin.setBasicpKaLowerLimit(pKaFunctions.SMALLEST_PKA);  
			plugin.setAcidicpKaUpperLimit(pKaFunctions.LARGEST_PKA);  
			plugin.setMicropKaCalc(true);
			plugin.setpKaPrefixType(pKaPlugin.DYNAMICpKaPREFIX); 
			plugin.setModel(pKaPlugin.MODEL_LARGE);
			plugin.setConsiderTautomerization(true);
			plugin.setMolecule(molecule);
			plugin.run();
		} // if use JChem
		final double[][] allAtomPKs = (useJChem ? null 
				: pKaFunctions.pKapKbAtoms(molecule));
		 if (!useJChem) Utils.alwaysPrint("pKa.jsp: allAtomPKs = ", allAtomPKs);
		for (int atmIdx = 0; atmIdx < numAtoms; atmIdx++) {
			pKas[atmIdx] = "";
			pKbs[atmIdx] = "";
			final MolAtom atm = molecule.getAtom(atmIdx);
			if (ChemUtils.isMulticenterAtom(atm)
					|| ("H".equals(atm.getSymbol()) && atm.getCharge() == 0
					&& !"[H][H]".equals(MolString.toString(molecule, Utils.SMILES)))) 
				continue;
			final boolean bears_H = 
					atm.getImplicitHcount() + atm.getExplicitHcount() > 0;
			double[] atomPKs = null;
			if (useJChem) {
				final double[] pKaAcidic = (bears_H ?
						plugin.getpKaValues(atmIdx, pKaPlugin.ACIDIC)
						: null);
				final double[] pKaBasic = 
						plugin.getpKaValues(atmIdx, pKaPlugin.BASIC);
				atomPKs = new double[] { 
						(pKaAcidic != null ? pKaAcidic[0] : Double.NaN),
						(pKaBasic != null ? pKaBasic[0] : Double.NaN)
						};
			} else atomPKs = allAtomPKs[atmIdx];
			Utils.alwaysPrint("pKa.jsp: for atom ", atm, atmIdx + 1,
					", atomPKs = ", atomPKs);
			/**/
			if (!Double.isNaN(atomPKs[0])) {
				Utils.alwaysPrint("acidic pKa for atom ", atm, atmIdx + 1,
						": ", atomPKs[0]);
				/**/
				pKas[atmIdx] = numberFormat.format(atomPKs[0]);
			} // else Utils.alwaysPrint("no acidic pKa for atom ", atm, atmIdx + 1);
			if (!Double.isNaN(atomPKs[1])) {
				Utils.alwaysPrint("basic pKa for atom ", atm, atmIdx + 1, 
						": ", atomPKs[1]);
				/**/
				pKbs[atmIdx] = numberFormat.format(atomPKs[1]);
			} // else Utils.alwaysPrint("no basic pKa for atom ", atm, atmIdx + 1);
		} // for each atom in the molecule
		if (useJChem) {
			plugin.getMacropKaValues(pKaPlugin.ACIDIC, molpKas, molpKaAtms);
			plugin.getMacropKaValues(pKaPlugin.BASIC, molpKbs, molpKbAtms);
		} // if use JChem or ACE method
	} else { // substrate is null
		pKas = new String[0];
		pKbs = new String[0];
	} // if substrate is/is not null
	} catch (Exception e) {
		pKas = new String[0];
		pKbs = new String[0];
		Utils.alwaysPrint("pka.jsp: caught exception while trying to calculate pKas "
				+ "using Marvin JS for substrate:\n", substrate);
		Utils.alwaysPrint(e.getMessage());
		e.printStackTrace();
	} // try
	final String APPLET_NAME = Utils.toString(substrate == null ? "pKa" : "hybrid", "Marvin");

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Acidity and basicity calculator</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>nosession/marvinJS/js/promise.min.js"
		type="text/javascript"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<script type="text/javascript">
	// <!-- >

	<%@ include file="../../js/marvinQuestionConstants.jsp.h" %>

	function loadSelections() { ; }

	function getPKas() {
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(molstruct) {
			document.tester.substrate.value = molstruct;
			document.tester.submit();
		}, function(error) {
			alert('Molecule export failed:' + error);	
		});
	}

	// -->
</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="pKa.jsp" method="post">
<input type="hidden" name="substrate" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE and JChem acidity and basicity calculator
</p>
<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr><td class="regtext" >
	Please draw a compound, and press the button below to see the 
	<%= Utils.toDisplay("pKa and pKb") %>
	of each atom, as calculated by 
	<select name="method">
		<option value="ACE" <%= !useJChem ? "selected=\"selected\"" : "" %>>
		ACE's modification of JChem.</option>
		<option value="JChem" <%= useJChem ? "selected=\"selected\"" : "" %>>
		JChem.</option>
	</select>
	<p>(Here, <%= Utils.toDisplay("pKb is defined as the pKa") %>
	of the conjugate acid.)
	</p>
</td></tr>
<tr>
<td style="text-align:center;">
	<div id="<%= APPLET_NAME %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('<%= substrate != null ? Utils.toValidJS(substrate) : "" %>', 
				MARVIN, 0, '<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td>
</tr>
<tr>
<td style="text-align:center;">
	<br/>
	<input type="button" value=" View pKas " onclick="getPKas()"/>
	<br/><br/>
</td>
</tr>
</table>
</form>

<% if (numAtoms > 0) { %>
	<p>
	<table border=1 class="whiteTable" style="text-align:center;">
		<tr>
		<th style="width:40px; text-align:center;">Atom</th>
		<th style="width:40px; text-align:center;"><%= Utils.toDisplay("pKa") %></th>
		<th style="width:40px; text-align:center;"><%= Utils.toDisplay("pKb") %></th>
		</tr>
	<% 
	int metals = 0;
	for (int j = 0; j < origNumAtoms; j++) { 
		final MolAtom atom = origMolecule.getAtom(j);
		final String atomSymb = atom.getSymbol();
		final int atomNo = atom.getAtno();
		final int H = 1;
		final int Mg = 12;
		final int ALKALIS = 1;
		if ((PeriodicSystem.getColumn(atomNo) == ALKALIS && atomNo != H)
				|| atomNo == Mg) {
			metals++;
		} else if (!ChemUtils.isMulticenterAtom(atomNo) 
				&& j - metals < Utils.getLength(pKas)
				&& j - metals < Utils.getLength(pKbs)
				&& j - metals >= 0) { %>
			<tr>
			<td style="width:40px; text-align:center;"><%= atomSymb + (j + 1) %></td>
			<td style="width:40px; text-align:center;"><%= pKas[j - metals] %></td>
			<td style="width:40px; text-align:center;"><%= pKbs[j - metals] %></td>
			</tr>
	<% 	} // if a metal 
	} // for each atom %>
	</table>
<% } // if there are atoms %>

<table style="width:500px; margin-left:auto; margin-right:auto;">
<% if (numAtoms > 0 && useJChem) { // change < to > to examine molecular pKa values %>
	<tr><td class="regtext" >
		<%= !Double.isNaN(molpKas[0]) ? 
		"<br/><br/>" + Utils.toDisplay("The molecular pKa values: ") 
			+ numberFormat.format(molpKas[0]) 
			+ " (atom " + (molpKaAtms[0] + 1) + ")" 
			+ (!Double.isNaN(molpKas[1]) ? ", " + numberFormat.format(molpKas[1]) 
					+ " (atom " + (molpKaAtms[1] + 1) + ")" 
				: "") 
			+ (!Double.isNaN(molpKas[2]) ? ", " + numberFormat.format(molpKas[2]) 
					+ " (atom " + (molpKaAtms[2] + 1) + ")" 
				: "") 
			+ ".<br/>" : "" %>
		<%= !Double.isNaN(molpKbs[0]) ? 
		Utils.toDisplay("The molecular pKb values: ") 
			+ numberFormat.format(molpKbs[0]) 
			+ " (atom " + (molpKbAtms[0] + 1) + ")" 
			+ (!Double.isNaN(molpKbs[1]) ? ", " + numberFormat.format(molpKbs[1]) 
					+ " (atom " + (molpKbAtms[1] + 1) + ")" 
				: "") 
			+ (!Double.isNaN(molpKbs[2]) ? ", " + numberFormat.format(molpKbs[2]) 
					+ " (atom " + (molpKbAtms[2] + 1) + ")" 
				: "") 
			+ "." : "" %>
	</td> </tr>
<% } // if should get molecular pKa values %>
<tr>
<td class="regtext" style="text-align:left; padding-top:10px;" >
<a href="index.html">Back</a> to private ACE pages.
</td>
</tr>
</table>

</body>
</html>

