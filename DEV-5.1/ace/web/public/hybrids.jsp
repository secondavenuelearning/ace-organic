<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.MolAtom,
	chemaxon.struc.Molecule,
	com.epoch.AppConfig,
	com.epoch.chem.ChemUtils,
	com.epoch.qBank.Question,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final String substrate = request.getParameter("substrate");

	Molecule substrateMol = null;
	int numAtoms = 0;
	if (substrate != null) {
		substrateMol = MolImporter.importMol(substrate);
		numAtoms = substrateMol.getAtomCount();
		/* Utils.alwaysPrint("hybrids.jsp: calculating hybridization of ", numAtoms,
				" atom(s)."); /**/
		substrateMol.calcHybridization();
	}
	final String APPLET_NAME = "hybridMarvin";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Hybridization calculator</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>nosession/marvinJS/js/promise.min.js"
<script src="https://marvinjs.chemicalize.com/v1/<%= AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<script type="text/javascript">
	// <!-- >

	<%@ include file="../js/marvinQuestionConstants.jsp.h" %>

	function loadSelections() { ; }

	function getHybrids() {
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
<form name="tester" action="hybrids.jsp" method="post">
<input type="hidden" name="substrate" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Hybridization calculator
</P>
<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr><td class="regtext" >
	Draw a compound, and press the button below to see the hybridization state 
	of each atom.  (Note: The algorithm may not work for metals or for 
	atoms involved in resonance.)  
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
				MARVIN, 0, '<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td>
</tr>
<tr>
<td style="text-align:center;" colspan="2">
	<br/>
	<input type="button" value=" View Hybridization States " onclick="getHybrids()"/>
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
		<th>Atom</th>
		<th>Hybridization</th>
		</tr>
	<% for (int j = 0; j < numAtoms; j++) {
		final MolAtom atom = substrateMol.getAtom(j);
		if (!ChemUtils.isMulticenterAtom(atom)) { %>
			<tr>
			<td><%= atom.getSymbol() %><%= j + 1 %></td>
			<%  final int hybridization = atom.getHybridizationState() - 1;
			if (hybridization > 1) { %>
				<td><i>sp</i><sup><%= hybridization %></sup></td>
			<% } else if (hybridization == 1) { %>
				<td><i>sp</i></td>
			<% } else if (hybridization == 0) { %>
				<td><i>s</i></td>
			<% } else { %>
				<td>value <%= hybridization + 1 %></td>
			<% } %>
			</tr>
		<% } %>
	<% } %>
	</table>
<% } %>

<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr>
<td class="regtext" style="text-align:left; padding-top:10px;" >
<a href="welcome.html">Back</a> to public ACE pages.
</td>
</tr>
</table>

</body>
</html>
