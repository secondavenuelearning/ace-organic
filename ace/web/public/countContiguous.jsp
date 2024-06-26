<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.Molecule,
	com.epoch.AppConfig,
	com.epoch.chem.FormulaFunctions,
	com.epoch.qBank.Question,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final String substrate = request.getParameter("substrate");
	
	int numContiguous = 0;
	if (substrate != null) {
		// Utils.alwaysPrint("countContiguous.jsp: substrate = ", substrate);
		final Molecule molecule = MolImporter.importMol(substrate);
		numContiguous = FormulaFunctions.countContiguousCAtoms(molecule);
	}
	final String APPLET_NAME = "responseApplet";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Count Contiguous C Atoms</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="../includes/epoch.css" type="text/css"/>
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

	function getCount() {
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
<form name="tester" action="countContiguous.jsp" method="post">
<input type="hidden" name="substrate" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Count Contiguous C Atoms
</P>
<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr><td class="regtext" >
	Draw a structure, and submit to get the largest number of contiguous C atoms.
	<% if (numContiguous > 0) { %>
		<p>The largest set of contiguous C atoms in the structure you submitted is 
		<%= numContiguous %>.
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
				MARVIN, 0, '<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td>
</tr>
<tr>
<td style="text-align:center;" colspan="2">
	<br/>
	<input type="button" value=" Submit " onclick="getCount()"/>
	<br/>
	<br/>
</td>
</tr>
</table>

<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr>
<td align="left" class="regtext" >
<a href="welcome.html">Back</a> to public ACE pages.
</td>
</tr>
</table>

</form>
</body>
</html>
