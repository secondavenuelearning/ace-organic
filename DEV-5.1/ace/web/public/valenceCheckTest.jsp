<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.Molecule,
	chemaxon.struc.MolAtom,
	com.epoch.AppConfig,
	com.epoch.chem.ChemUtils,
	com.epoch.qBank.Question,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until MarvinJS understands export parameters

	final String substrate = request.getParameter("substrate");

	Molecule mol = null;
	int numAtoms = 0;
	if (substrate != null) {
		mol = MolImporter.importMol(substrate);
		numAtoms = mol.getAtomCount();
		// Utils.alwaysPrintMRV("valenceCheckTest.jsp: mol:\n", mol);
		mol.valenceCheck();
	}
	final String APPLET_NAME = "hybridMarvin";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Valence errors calculator</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>nosession/marvinJS/js/promise.min.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<script type="text/javascript">
	// <!-- >

	<%@ include file="../js/marvinQuestionConstants.jsp.h" %>

	function loadSelections() { ; }

	function getValenceErrors() {
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
<form name="tester" action="valenceCheckTest.jsp" method="post">
<input type="hidden" name="substrate" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Valence errors calculator
</P>
<p>
<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr><td class="regtext" >
	Draw a compound, and press the button below to see
	whether any atoms have valence errors.
</td></tr>
</table>
<br/>

<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr>
<td style="text-align:center;" colspan="2">
	<div id="<%= APPLET_NAME %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('<%= substrate != null ? Utils.toValidJS(substrate) : "C[O++]C |^3:1|" %>', 
				MARVIN, 0, '<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td>
</tr>
<tr>
<td style="text-align:center;" colspan="2">
	<br/>
	<input type="button" value=" View valence errors " onclick="getValenceErrors()"/>
	<br/>
	<br/>
</td>
</tr>
</table>
</form>

<% if (numAtoms > 0) { %>
	<p>
	<table border="1" class="whiteTable" style="text-align:center;">
		<tr>
		<th>Atom type</th>
		<th>Atom index</th>
		<th>Valence error?</th>
		</tr>
	<% for (int j = 0; j < numAtoms; j++) { 
		final MolAtom atom = mol.getAtom(j);
		if (!ChemUtils.isMulticenterAtom(atom)) { %>
			<tr>
			<td><%= atom.getSymbol() %></td>
			<td><%= j + 1 %></td>
			<td><%= atom.hasValenceError() ? "<b>YES</b>" : "no" %></td>
			</tr>
		<% } 
	} %>
	</table>
<% } %>

<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr>
<td align="left" class="regtext" >
<a href="welcome.html">Back</a> to public ACE pages.
</td>
</tr>
</table>

</body>
</html>
