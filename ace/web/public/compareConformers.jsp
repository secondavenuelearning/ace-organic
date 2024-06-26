<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolFormatException,
	chemaxon.formats.MolImporter,
	chemaxon.struc.Molecule,
	com.epoch.AppConfig,
	com.epoch.chem.ChemUtils,
	com.epoch.chem.MolCompare,
	com.epoch.chem.MolString,
	com.epoch.qBank.Question,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until MarvinJS understands export parameters

	final String target = request.getParameter("target");
	final String query = request.getParameter("query");
	boolean cpdsSame = false;
	boolean searchResult = false;
	boolean searchEnantResult = false;
	if (target != null && query != null) try {
		final Molecule targetMol = MolImporter.importMol(target);
		final Molecule queryMol = MolImporter.importMol(query);
		cpdsSame = MolCompare.matchPrecise(targetMol, queryMol);
		if (cpdsSame) {
			searchResult = MolCompare.matchConformers(target, query);
			if (!searchResult) { 
				searchEnantResult = MolCompare.matchConformers(
						MolString.toString(
							ChemUtils.getMirror(targetMol), Utils.MRV), query);
			}
		} // if are same
	} catch (MolFormatException e) { 
		// empty
	}
	final String TARGET_APPLET = "targetapplet";
	final String QUERY_APPLET = "queryapplet";
	
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Conformer comparison calculator</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>

<!-- place values so updatePage() can find them
	follows ACE conventions: four-@-delimited strings.
	cpdsSameValue = @@@@<%= cpdsSame %>@@@@
	searchResultValue = @@@@<%= searchResult %>@@@@
	searchEnantResultValue = @@@@<%= searchEnantResult %>@@@@
-->

<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>nosession/marvinJS/js/promise.min.js"
		type="text/javascript"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<script type="text/javascript">
	// <!-- >
	<%@ include file="../js/marvinQuestionConstants.jsp.h" %>

	// define universal JS variables; initial values from Java values

	function loadSelections() { ; }

	function callServer() {
		var url = 'compareConformers.jsp';
		var bld = new String.builder();
		marvinSketcherInstances['<%= TARGET_APPLET %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(targetMol) {
			marvinSketcherInstances['<%= QUERY_APPLET %>'].
					exportStructure('<%= MRV_EXPORT %>').then(function(queryMol) {
				bld.append('target=').append(encodeURIComponent(targetMol)). 
						append('&query=').append(encodeURIComponent(queryMol));
				setValue('submit', ' Processing ... ');
				disableCell('submit');
				callAJAX(url, bld.toString());
			}, function(error) {
				alert('Molecule export failed:' + error);	
			});
		}, function(error) {
			alert('Molecule export failed:' + error);	
		});
	} // callServer()

	function updatePage() {
		if (xmlHttp.readyState === 4) { // ready to continue
			var response = xmlHttp.responseText;
			// the response is the entire web page
			var cpdsSameValue = extractField(response, 'cpdsSameValue');
			var searchResultValue = extractField(response, 'searchResultValue');
			var searchEnantResultValue = extractField(response, 'searchEnantResultValue');
			// use innerHTML to change text & images upon receipt of results
			if (cpdsSameValue === 'true') {
				if (searchResultValue === 'true') {
					setInnerHTML('intro', 'Structures 1 and 2 are the same compound ' 
							+ 'in identical conformations.');
				} else if (searchEnantResultValue === 'true') {
					setInnerHTML('intro', 'Structures 1 and 2 are the same compound ' 
							+ 'in enantiomeric conformations.');
				} else setInnerHTML('intro', 'Structures 1 and 2 are the same compound ' 
						+ 'in diastereomeric conformations.');
			} else setInnerHTML('intro', 'Structures 1 and 2 are different compounds, so ' 
					+ 'their conformations can\'t be compared.');
			setInnerHTML('drawtarget', 'Structure 1:');
			setInnerHTML('drawquery', 'Structure 2:');
			enableCell('submit');
			setValue('submit', ' View Comparison ');
		} // ready to continue
	} // updatePage()

	// -->
</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="compareConformers.jsp" method="post">
<input type="hidden" name="target" value=""/>
<input type="hidden" name="query" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Conformer comparison 
</P>
<table style="margin-left:auto; margin-right:auto;">
<tr><td id="intro" class="regtext" colspan="2">
	Please draw two conformers and compare them. Use one of the templates. 
	If you are comparing chairs, you will need to use the 
	H&plusmn; (Add/Remove Explicit Hydrogens) button to add the H atoms;
	if you don't see it, press the "scroll right" arrow in the upper right 
	corner.
</td>
</tr>
<tr><td>
	&nbsp;
</td></tr>
<tr><td>
	&nbsp;
</td></tr>
<tr><td class="regtext" id="drawtarget" >
	Draw structure 1:
</td><td class="regtext" id="drawquery" >
	Draw structure 2:
</td></tr>

<tr><td>
	<div id="<%= TARGET_APPLET %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('<%= target != null ? Utils.toValidJS(target) : "" %>', 
				MARVIN, THREEDIM, 
				'<%= TARGET_APPLET %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td><td>
	<div id="<%= QUERY_APPLET %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('<%= query != null ? Utils.toValidJS(query) : "" %>', 
				MARVIN, THREEDIM, 
				'<%= QUERY_APPLET %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td></tr>
<tr><td style="text-align:center;">
	<br/>
	<input type="button" id="submit" value=" View Comparison " onclick="callServer()"/>
	<br/>
</td></tr>

<tr>
<td align="left" class="regtext" >
<a href="welcome.html">Back</a> to public ACE pages.
</td>
</tr>

</table>
</form>

</body>
</html>
