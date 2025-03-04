<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.Molecule,
	chemaxon.struc.MolBond,
	chemaxon.struc.MolAtom,
	com.epoch.AppConfig,
	com.epoch.chem.MolString,
	com.epoch.chem.StereoFunctions,
	com.epoch.qBank.Question,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final String substrate = request.getParameter("substrate");

	Molecule substrateMol = null;
	String text = "No molecule was detected.  Try again.";
	if (substrate != null) {
		substrateMol = MolImporter.importMol(substrate);
		final StringBuilder bld = Utils.getBuilder("Substrate ", 
				MolString.toString(substrateMol, Utils.SMILES), 
				" :<p align=\"center\"><table class=\"regtext\"><tr>"
					+ "<th>Bond</th><th>Stereochemistry value</th></tr>"); 
		for (final MolBond bond : substrateMol.getBondArray()) {
			final MolAtom atom1 = bond.getAtom1();
			final MolAtom atom2 = bond.getAtom2();
			final int type = bond.getType();
			final int stereo = StereoFunctions.getBondStereo(bond);
 			Utils.appendTo(bld, "<tr><td style=\"text-align:center;\">", 
					atom1.getSymbol(), substrateMol.indexOf(atom1) + 1, 
					type == 3 ? "&equiv;" : type == 2 ? "=" : "&ndash;", 
					atom2.getSymbol(), substrateMol.indexOf(atom2) + 1, 
					"</td><td style=\"text-align:center;\">", stereo, 
					"</td></tr>");
		}
		bld.append("</table>");
		text = bld.toString();
	}
	final String APPLET_NAME = "hybridMarvin";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<title>Bond stereochemistry flags calculator</title>
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

	<%@ include file="../js/marvinQuestionConstants.jsp.h" %>

	function loadSelections() { ; }

	function getFlags() {
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(mol) {
			document.tester.substrate.value = mol;
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
<form name="tester" action="dblBondStereo.jsp" method="post">
<input type="hidden" name="substrate" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Bond stereochemistry flags calculator
</p>
<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr><td class="regtext" >
	<% if (substrate == null) { %>
		Draw a compound, and press the button below to see the stereochemistry flags of each bond.
	<% } else { %>
		<%= text %>
	<% } %>
</td></tr>
</table>
<br/>

<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr>
<td style="text-align:center;" colspan="2">
	<div id="<%= APPLET_NAME %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('<%= substrate != null ? Utils.toValidJS(substrate) : "" %>', 
				MARVIN, 
				SHOWMAPPING, 
				'<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td>
</tr>
<tr>
<td style="text-align:center;" colspan="2">
	<br/>
	<input type="button" value=" View Bond Stereochemistry Flags " onclick="getFlags()"/>
	<br/>
	<br/>
</td>
</tr>
</table>
</form>

<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr>
<td class="regtext" style="text-align:left; padding-top:10px;" >
<a href="welcome.html">Back</a> to public ACE pages.
</td>
</tr>
</table>

</body>
</html>
