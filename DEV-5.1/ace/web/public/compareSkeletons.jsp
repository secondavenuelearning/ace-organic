<%@ page language="java" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.evals.impl.chemEvals.Contains,
	com.epoch.qBank.Question,
	com.epoch.responses.Response,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final String target = request.getParameter("target");
	final String query = request.getParameter("query");
	final boolean targetQueryFound = target != null && query != null;
	boolean foundSkeleton = false;

	if (targetQueryFound) {
		final String mode = request.getParameter("mode");
		final String chgRadIso = request.getParameter("chgRadIso");
		Utils.alwaysPrint("compareSkeletons.jsp: converting target to response.");
		final Response resp = new Response(Question.MARVIN, target);
		final String data = Utils.toString(Contains.ANY, '/', 
				"skel".equals(mode) ? Contains.SKELETON : Contains.SUBSTRUCTURE, 
				'/', chgRadIso);
		Utils.alwaysPrint("compareSkeletons.jsp: creating new evaluator.");
		final Contains skel = new Contains(data.toString());
		foundSkeleton = skel.isResponseMatching(resp, query).isSatisfied;
	}
	final String TARGET_APPLET = "targetapplet";
	final String QUERY_APPLET = "queryapplet";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<title>Substructures and Skeletons Comparator</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>

<!-- place values so updatePage() can find them
	follows ACE conventions: four-@-delimited strings.
	foundSkeletonValue = @@@@<%= foundSkeleton %>@@@@
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

	// define universal JS variables; initial values from Java values
	var targetQueryFoundValue = '<%= targetQueryFound %>';

	function loadSelections() { ; }

	function callServer() {
		var mode = document.tester.mode.value;
		var chgRadIso = document.tester.chgRadIso.value;
		var url = 'compareSkeletons.jsp';
		var toSendBld = new String.builder();
		marvinSketcherInstances['<%= TARGET_APPLET %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(targetMol) {
			marvinSketcherInstances['<%= QUERY_APPLET %>'].
					exportStructure('<%= MRV_EXPORT %>').then(function(queryMol) {
				toSendBld.append('target=').append(encodeURIComponent(targetMol)).
						append('&query=').append(encodeURIComponent(queryMol)).
						append('&mode=').append(mode).
						append('&chgRadIso=').append(chgRadIso);
				setValue('submit', ' Processing ... ');
				disableCell('submit');
				callAJAX(url, toSendBld.toString());
			}, function(error) {
				alert('Molecule export failed:' + error);	
			});
		}, function(error) {
			alert('Molecule export failed:' + error);	
		});
	} // callServer()

	var word = 'skeleton';
	function updatePage() {
		if (xmlHttp.readyState === 4) { // ready to continue
			var response = xmlHttp.responseText;
			// the response is the entire web page
			var foundSkeletonValue = extractField(response, 'foundSkeletonValue');
			targetQueryFoundValue = extractField(response, 'targetQueryFoundValue');
			// use innerHTML to change text & images upon receipt of results
			if (targetQueryFoundValue === 'true') {
				var bld = new String.builder();
				bld.append('The structure ').
						append(foundSkeletonValue === 'true' 
								? 'contains' : 'does not contain').
						append(' the <span id="word1">').
						append(word).append('<\/span>.');
				setInnerHTML('intro', bld.toString());
				setInnerHTML('drawtarget', 'The structure tested:');
				setInnerHTML('drawquery1', 'The');
			} else {
				var bld = new String.builder();
				bld.append('No test structure or <span id="word1">').
						append(word).append('<\/span> detected; try again. ');
				setInnerHTML('intro', bld.toString());
				setInnerHTML('drawtarget', 'Draw the structure to be tested:');
				setInnerHTML('drawquery1', 'Draw the');
			}
			enableCell('submit');
			setValue('submit', ' Find ' + word + ' ');
		} // ready to continue
	} // updatePage()

	function changeMode() {
		word = (document.tester.mode.value === 'skel' ? 'skeleton' : 'substructure');
		for (var i = 1; i <= 2; i++) setInnerHTML('word' + i, word); // <!-- > 
		setValue('submit', ' Find ' + word + ' ');
		if (targetQueryFoundValue === 'true') setInnerHTML('intro', '&nbsp;');
	}

	// -->
</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="compareSkeletons.jsp" method="post">
<input type="hidden" name="target" value=""/>
<input type="hidden" name="query" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Substructures and Skeletons Comparator 
</p>
<table style="margin-left:auto; margin-right:auto;">
<tr><td id="intro" class="regtext"  colspan="2">
	Please draw your structure on the left and a 
	<span id="word1">skeleton</span> on the right.  Press
	the button below to see if the structure contains the <span
	id="word2">skeleton</span>.
</td>
</tr>
<tr><td>
	&nbsp;
</td></tr>
<tr><td class="regtext" id="drawtarget" style="width:50%;">
	Draw the structure to be tested:
</td><td class="regtext" >
	<span id="drawquery1">Draw the</span> 
	<select name="mode" id="mode" onchange="javascript:changeMode()">
		<option value="skel">skeleton</option>
		<option value="sub">substructure</option>
	</select>
	<span id="drawquery2">with charges, radicals, and isotopes</span> 
	<select name="chgRadIso" id="chgRadIso">
		<option value="1">matching exactly</option>
		<option value="2">specified in query must be in target</option>
		<option value="3">ignored</option>
	</select>:
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
<tr><td style="text-align:center;" colspan="2">
	<br/>
	<input type="button" id="submit" value=" Find skeleton " onclick="callServer()"/>
</td></tr>
</table>

<table style="margin-left:auto; margin-right:auto; width:95%;">
<tr><td class="regtext" align="left">
	<br/>
	Compound <b>A</b> contains a substructure <b>S</b> when:
	<ul>
	<li>Every atom and bond in <b>S</b> is also present in <b>A</b>.  
	H atoms 
	in <b>S</b> must be explicit to be considered, but explicit H atoms 
	in <b>S</b> may be explicit or implicit in <b>A</b>. 
	</li><li>Every bond in <b>S</b> has the same order in <b>A</b>.
	</li><li>Corresponding atoms in <b>S</b> and <b>A</b> have the same charge.
	</li><li>Any isotopes or stereochemical configurations 
	indicated in <b>S</b> are the same in <b>A</b>.
	</li></ul>
	<p>
	Compound <b>A</b> contains a skeleton <b>S</b> when:
	</p>
	<ul>
	<li>Every atom and bond in <b>S</b> is also present in <b>A</b>.  
	These atoms and bonds in <b>A</b> are called "skeletal".  H atoms 
	in <b>S</b> must be explicit to be considered, but explicit H atoms 
	in <b>S</b> may be explicit or implicit in <b>A</b>. 
	</li><li>Each skeletal bond in <b>A</b> has the same order as or a higher 
	order than the corresponding bond in <b>S</b>.  (Double and 
	aromatic bonds are treated as identical.)  For example, cyclohexene contains 
	the cyclohexane skeleton, but not vice versa.
	</li><li>For every skeletal C atom in <b>A</b> that is attached to another C atom,
	both the bond and the latter C atom are skeletal.  For example, 
	ethoxybenzene contains both the benzene and the phenol 
	skeletons, but toluene does not contain the benzene skeleton;  
	and 2-Azabicyclo[3.1.0]hexane does not contain the piperidine skeleton,
	but 1-azabicyclo[3.1.0]hexane does.
	</li><li>Any charges, isotopes, or stereochemical configurations 
	indicated in <b>S</b> may differ or be absent in <b>A</b>, and vice versa.
	</li></ul>
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
