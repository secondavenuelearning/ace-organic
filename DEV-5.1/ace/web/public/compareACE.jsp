<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.Molecule,
	com.epoch.AppConfig,
	com.epoch.chem.MolCompare,
	com.epoch.chem.Normalize,
	com.epoch.qBank.Question,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final String target = request.getParameter("target");
	final String query = request.getParameter("query");

	final boolean targetQueryFound = target != null && query != null;
	boolean searchResult = false;

	if (targetQueryFound) {
		final Molecule targetMol = MolImporter.importMol(target);
		final Molecule queryMol = MolImporter.importMol(query);
		final String searchTypeStr = request.getParameter("searchType");
		final boolean normalize = "true".equals(request.getParameter("normalize"));
		if (normalize) {
			Normalize.normalizeNoClone(targetMol);
			Normalize.normalizeNoClone(queryMol);
		} // if should normalize structures
		searchResult = 
				("sigma".equals(searchTypeStr) 
				? MolCompare.matchSigmaNetwork(targetMol, queryMol)
				: "resonance".equals(searchTypeStr) 
				? MolCompare.areResonanceStructures(targetMol, queryMol)
				: "resonOrPrecise".equals(searchTypeStr) 
				? MolCompare.areResonanceOrIdentical(targetMol, queryMol)
				: "precise".equals(searchTypeStr) 
				? MolCompare.matchPrecise(targetMol, queryMol)
				: "perfect".equals(searchTypeStr) 
				? MolCompare.matchPerfect(targetMol, queryMol)
				: "enant".equals(searchTypeStr) 
				? MolCompare.matchExact(targetMol, queryMol, MolCompare.OR_ENANTIOMER)
				: MolCompare.matchExact(targetMol, queryMol));
	} // if targetQueryFound 
	final String TARGET_APPLET = "targetapplet";
	final String QUERY_APPLET = "queryapplet";
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>ACE Structure Comparator</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>

<!-- place values so updatePage() can find them
	follows ACE conventions: four-@-delimited strings.
	searchResultValue = @@@@<%= searchResult %>@@@@
	targetQueryFoundValue = @@@@<%= targetQueryFound %>@@@@
-->

<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
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

	function callServer() {
		var url = 'compareACE.jsp';
		var bld = new String.builder();
		marvinSketcherInstances['<%= TARGET_APPLET %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(targetMol) {
			marvinSketcherInstances['<%= QUERY_APPLET %>'].
					exportStructure('<%= MRV_EXPORT %>').then(function(queryMol) {
				bld.append('target=').
						append(encodeURIComponent(targetMol)). 
						append('&query='). 
						append(encodeURIComponent(queryMol)).
						append('&searchType=').
						append(document.tester.searchType.value).
						append('&normalize=').
						append(document.tester.normalize.checked);
				clearInnerHTML('intro'); 
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
			var searchResultValue = extractField(response, 'searchResultValue');
			var targetQueryFoundValue = extractField(response, 'targetQueryFoundValue');
			// use innerHTML to change text & images upon receipt of results
			if (targetQueryFoundValue === 'true') {
				setInnerHTML('intro', 'The target '
						+ (searchResultValue === 'true'
							? 'matches' : 'does not match')
						+ ' the query.');
				setInnerHTML('drawtarget', 'The target (the student\'s response):');
				setInnerHTML('drawquery', 
						'The query (the author\'s reference structure):');
			} else {
				setInnerHTML('intro', 'No target or query detected; try again. ');
				setInnerHTML('drawtarget', 
						'Draw the target (the student\'s response):');
				setInnerHTML('drawquery', 
						'Draw the query (the author\'s reference structure):');
			}
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
<form name="tester" action="compareACE.jsp" method="post">
<input type="hidden" name="target" value=""/>
<input type="hidden" name="query" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Structure Comparator
</P>
<table style="margin-left:auto; margin-right:auto;">
<tr><td id="intro" class="regtext" colspan="2">
	Please draw two structures and compare them with ACE.    
</td>
</tr>
<tr><td>
	&nbsp;
</td></tr>
<tr><td class="regtext" id="drawtarget" >
	Draw the target (student's response):
</td><td class="regtext" id="drawquery" >
	Draw the query (author's reference structure):
</td></tr>

<tr><td>
	<div id="<%= TARGET_APPLET %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('<%= target != null ? Utils.toValidJS(target) : "" %>', 
				MARVIN, 0, 
				'<%= TARGET_APPLET %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td><td>
	<div id="<%= QUERY_APPLET %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('<%= query != null ? Utils.toValidJS(query) : "" %>', 
				MARVIN, 0, 
				'<%= QUERY_APPLET %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td></tr>
<tr><td style="text-align:center;">
	<table class="regtext" >
	<tr><td>
		type of match:
	</td><td>
		<select name="searchType" id="searchType" onchange="disableOptions()">
			<option value="sigma">&sigma;-bond network</option>
			<option value="resonance">resonance structures</option>
			<option value="resonOrPrecise">resonance structures or identical</option>
			<option value="enant" selected="selected">exact or enantiomer</option>
			<option value="exact" selected="selected">exact</option>
			<option value="precise">precise</option>
			<option value="perfect">perfect</option>
		</select>
	</td></tr>
	<tr><td>
		normalize first?
	</td><td>
		<input type="checkbox" name="normalize" id="normalize" checked="checked"/>
	</td></tr>
	<tr><td colspan="2" style="padding-top:10px;">
		<i>exact</i>: query straight bond matches any stereobond in target
		<br /><i>precise</i>: stereobonds correspond exactly
		<br /><i>perfect</i>: stereobonds and explicit H atoms correspond exactly
	</td></tr>
	</table>
</td><td>
	<br/>
	<input type="button" id="submit" value=" View Comparison " onclick="callServer()"/>
	<br/>
</td></tr>

<tr>
<td align="left" class="regtext" >
<br/>
<br/>
<a href="welcome.html">Back</a> to public ACE pages.
</td>
</tr>

</table>
</form>

</body>
</html>
