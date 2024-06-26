<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.sss.SearchConstants,
	chemaxon.sss.search.MolSearch,
	chemaxon.sss.search.MolSearchOptions,
	chemaxon.struc.Molecule,
	chemaxon.struc.StereoConstants,
	com.epoch.AppConfig,
	com.epoch.chem.MolString,
	com.epoch.qBank.Question,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final String target = request.getParameter("target");
	final String query = request.getParameter("query");
	final String TRUE = "true";
	final String ON = "on";

	final boolean targetQueryFound = target != null && query != null;
	boolean searchResult = false;
	boolean listAllMatches = false;
	String allMatches = "";

	if (targetQueryFound) {
		final String searchTypeStr = request.getParameter("searchType");
		final int searchType = MathUtils.parseInt(searchTypeStr);
		Utils.alwaysPrint("compareJChem.jsp: searchType = ", searchTypeStr);

		final String stereoSrchStr = request.getParameter("setStereoSearch");
		final int setStereoSearch = MathUtils.parseInt(stereoSrchStr);
		Utils.alwaysPrint("compareJChem.jsp: setStereoSearch = ", setStereoSearch);

		final String considerDoubleBondStereoStr = request.getParameter("considerDoubleBondStereoMatching");
		final int doubleBondStereoMatching = 
				(Utils.among(considerDoubleBondStereoStr, ON, TRUE)
							? StereoConstants.DBS_ALL
							: StereoConstants.DBS_NONE);
		Utils.alwaysPrint("compareJChem.jsp: considerDoubleBondStereoMatching = ",
				considerDoubleBondStereoStr);

		final String considerOddCumuleneStereoStr = request.getParameter("considerOddCumuleneStereoMatching");
		final boolean considerOddCumuleneStereoMatching = 
				Utils.among(considerOddCumuleneStereoStr, ON, TRUE);
		Utils.alwaysPrint("compareJChem.jsp: considerOddCumuleneStereoMatching = ",
				considerOddCumuleneStereoStr);

		final String considerAxialStereoStr = request.getParameter("considerAxialStereoMatching");
		final boolean considerAxialStereoMatching = 
				Utils.among(considerAxialStereoStr, ON, TRUE);
		Utils.alwaysPrint("compareJChem.jsp: considerAxialStereoMatching = ",
				considerAxialStereoStr);

		final String considerSynAntiStereoStr = request.getParameter("considerSynAntiStereoMatching");
		final boolean considerSynAntiStereoMatching = 
				Utils.among(considerSynAntiStereoStr, ON, TRUE);
		Utils.alwaysPrint("compareJChem.jsp: considerSynAntiStereoMatching = ",
				considerSynAntiStereoStr);

		final String stereoMatchingModelStr = request.getParameter("stereoMatchingModel");
		final int stereoMatchingModel = MathUtils.parseInt(stereoMatchingModelStr); 
		Utils.alwaysPrint("compareJChem.jsp: stereoMatchingModel = ",
				stereoMatchingModelStr);

		final String chargeTypeStr = request.getParameter("chargeType");
		final int chargeType = MathUtils.parseInt(chargeTypeStr);
		Utils.alwaysPrint("compareJChem.jsp: chargeType = ", chargeTypeStr);

		final String radicalTypeStr = request.getParameter("radicalType");
		final int radicalType = MathUtils.parseInt(radicalTypeStr);
		Utils.alwaysPrint("compareJChem.jsp: radicalType = ", radicalTypeStr);

		final String isotopeTypeStr = request.getParameter("isotopeType");
		final int isotopeType = MathUtils.parseInt(isotopeTypeStr); 
		Utils.alwaysPrint("compareJChem.jsp: isotopeType = ", isotopeTypeStr);

		final String valenceTypeStr = request.getParameter("valenceType");
		final boolean valenceType = 
				valenceTypeStr == null || TRUE.equals(valenceTypeStr);
		Utils.alwaysPrint("compareJChem.jsp: valenceType = ", valenceTypeStr);

		final String bondVaguenessStr = request.getParameter("bondVagueness");
		final int bondVagueness = MathUtils.parseInt(bondVaguenessStr, 
				SearchConstants.VAGUE_BOND_DEFAULT);
		Utils.alwaysPrint("compareJChem.jsp: bondVagueness = ", bondVagueness);

		final String orderSensitiveStr = request.getParameter("orderSensitive");
		final boolean orderSensitive = Utils.among(orderSensitiveStr, ON, TRUE);
		Utils.alwaysPrint("compareJChem.jsp: orderSensitive = ", orderSensitive);

		final String listAllMatchesStr = request.getParameter("listAllMatches");
		listAllMatches = Utils.among(listAllMatchesStr, ON, TRUE);
		Utils.alwaysPrint("compareJChem.jsp: listAllMatches = ", listAllMatches);

		final MolSearchOptions ourSearchOpts = new MolSearchOptions(searchType);
		final Molecule targetMol = MolImporter.importMol(target);
		final Molecule queryMol = MolImporter.importMol(query);
		Utils.alwaysPrint("compareJChem.jsp: target = ", 
				MolString.toString(targetMol, "cxsmarts"));
		Utils.alwaysPrint("compareJChem.jsp: query = ",
				MolString.toString(queryMol, "cxsmarts"));
		ourSearchOpts.setOrderSensitiveSearch(orderSensitive);
		ourSearchOpts.setStereoModel(stereoMatchingModel); 
		if (searchType != SearchConstants.DUPLICATE) {
			ourSearchOpts.setStereoSearchType(setStereoSearch);
			ourSearchOpts.setDoubleBondStereoMatchingMode(doubleBondStereoMatching); 
			ourSearchOpts.setChargeMatching(chargeType);
			ourSearchOpts.setRadicalMatching(radicalType);
			ourSearchOpts.setIsotopeMatching(isotopeType);
			ourSearchOpts.setValenceMatching(valenceType);
			ourSearchOpts.setVagueBondLevel(bondVagueness); 
		}
		ourSearchOpts.setIgnoreCumuleneOrRingCisTransStereo(!considerOddCumuleneStereoMatching);
		ourSearchOpts.setIgnoreAxialStereo(!considerAxialStereoMatching);
		ourSearchOpts.setIgnoreSynAntiStereo(!considerSynAntiStereoMatching);
		final MolSearch ourSearch = new MolSearch();
		ourSearch.setSearchOptions(ourSearchOpts);
		ourSearch.setTarget(targetMol);
		ourSearch.setQuery(queryMol);
		searchResult = ourSearch.isMatching();
		Utils.alwaysPrint("compareJChem.jsp: searchResult = ", searchResult);
		if (searchResult && listAllMatches) {
			int[] arrayResult = ourSearch.findFirst();
			final StringBuilder bld = new StringBuilder();
			while (arrayResult != null) {
 				Utils.appendTo(bld, "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;[", 
						Utils.join(arrayResult, ", ", 1), "]<br/>");
				arrayResult = ourSearch.findNext();
			} // while arrayResult != null
			allMatches = bld.toString();
		} // if searchResult && listAllMatches
	} // if targetQueryFound 
	final String TARGET_APPLET = "targetapplet";
	final String QUERY_APPLET = "queryapplet";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<title>JChem&reg; Structure Comparator</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>

<!-- place values so updatePage() can find them
	follows ACE conventions: four-@-delimited strings.
	searchResultValue = @@@@<%= searchResult %>@@@@
	targetQueryFoundValue = @@@@<%= targetQueryFound %>@@@@
	listAllMatchesValue = @@@@<%= listAllMatches %>@@@@
	allMatchesValue = @@@@<%= allMatches %>@@@@
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
		var url = 'compareJChem.jsp';
		var menu = document.tester;
		var bld = new String.builder();
		marvinSketcherInstances['<%= TARGET_APPLET %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(targetMol) {
			marvinSketcherInstances['<%= QUERY_APPLET %>'].
					exportStructure('<%= MRV_EXPORT %>').then(function(queryMol) {
				bld.append('target=').append(encodeURIComponent(targetMol)).
						append('&query=').append(encodeURIComponent(queryMol)).
						append('&searchType=').append(menu.searchType.value).
						append('&setStereoSearch=').
							append(menu.setStereoSearch.value).
						append('&considerDoubleBondStereoMatching=').
							append(menu.considerDoubleBondStereoMatching.checked).
						append('&considerOddCumuleneStereoMatching=').
							append(menu.considerOddCumuleneStereoMatching.checked).
						append('&considerAxialStereoMatching=').
							append(menu.considerAxialStereoMatching.checked).
						append('&considerSynAntiStereoMatching=').
							append(menu.considerSynAntiStereoMatching.checked).
						append('&stereoMatchingModel=').
							append(menu.stereoMatchingModel.value).
						append('&chargeType=').append(menu.chargeType.value).
						append('&radicalType=').append(menu.radicalType.value).
						append('&isotopeType=').append(menu.isotopeType.value).
						append('&valenceType=').append(menu.valenceType.value).
						append('&bondVagueness=').append(menu.bondVagueness.value).
						append('&orderSensitive=').append(menu.orderSensitive.checked).
						append('&listAllMatches=').append(menu.listAllMatches.checked);
				clearInnerHTML('intro'); 
				setValue('submit', ' Processing ... ');
				disableCell('submit');
				clearInnerHTML('allMatches');
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
			var listAllMatchesValue = extractField(response, 'listAllMatchesValue');
			var allMatchesValue = extractField(response, 'allMatchesValue');
			// use innerHTML to change text & images upon receipt of results
			if (targetQueryFoundValue === 'true') {
				if (searchResultValue === 'true') {
					setInnerHTML('intro', 'The query is found in the target.  '
							+ '(The target is a more specific case '
							+ 'of the query.)');
					if (listAllMatchesValue === 'true') {
						setInnerHTML('allMatches', 'The atom indices of the matches '
								+ 'are: <br/>' + allMatchesValue
								+ 'Each number represents an atom in the target; '
								+ 'its position indicates the atom in the query '
								+ 'to which it corresponds.');
					} 
				} else setInnerHTML('intro', 'The query is not found in the target.  '
						+ '(The target is not a more specific case '
						+ 'of the query.)');
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

	var memStereoSearch = 0;
	var memDoubleBondStereo = false;
	var memCharge = 'exact';
	var memRadical = 'exact';
	var memIsotope = 'exact';
	var memValence = 'true';

	function disableOptions() {
		var menu = document.tester;
		if (menu.searchType.value === '<%= SearchConstants.DUPLICATE %>') {
			memStereoSearch = menu.setStereoSearch.value;
			menu.setStereoSearch.value = '<%= SearchConstants.STEREO_EXACT %>';
			menu.setStereoSearch.disabled = true;
			memDoubleBondStereo = menu.considerDoubleBondStereoMatching.checked;
			menu.considerDoubleBondStereoMatching.checked = true;
			menu.considerDoubleBondStereoMatching.disabled = true;
			memCharge = menu.chargeType.value;
			menu.chargeType.value = 'exact';
			menu.chargeType.disabled = true;
			memRadical = menu.radicalType.value;
			menu.radicalType.value = 'exact';
			menu.radicalType.disabled = true;
			memIsotope = menu.isotopeType.value;
			menu.isotopeType.value = 'exact';
			menu.isotopeType.disabled = true;
			memValence = menu.valenceType.value;
			menu.valenceType.value = 'true';
			menu.valenceType.disabled = true;
		} else if (menu.searchType.value === '<%= SearchConstants.FULL %>') {
			memStereoSearch = menu.setStereoSearch.value;
			menu.setStereoSearch.value = '<%= SearchConstants.STEREO_SPECIFIC %>';
			menu.setStereoSearch.disabled = false;
			memDoubleBondStereo = menu.considerDoubleBondStereoMatching.checked;
			menu.considerDoubleBondStereoMatching.checked = true;
			menu.considerDoubleBondStereoMatching.disabled = false;
			menu.chargeType.value = memCharge;
			menu.chargeType.disabled = false;
			menu.radicalType.value = memRadical;
			menu.radicalType.disabled = false;
			menu.isotopeType.value = memIsotope;
			menu.isotopeType.disabled = false;
			menu.valenceType.value = memValence;
			menu.valenceType.disabled = false;
		} else {
			menu.setStereoSearch.disabled = false;
			menu.setStereoSearch.value = memStereoSearch;
			menu.considerDoubleBondStereoMatching.disabled = false;
			menu.considerDoubleBondStereoMatching.checked = memDoubleBondStereo;
			menu.chargeType.value = memCharge;
			menu.chargeType.disabled = false;
			menu.radicalType.value = memRadical;
			menu.radicalType.disabled = false;
			menu.isotopeType.value = memIsotope;
			menu.isotopeType.disabled = false;
			menu.valenceType.value = memValence;
			menu.valenceType.disabled = false;
		}
	}

	function setStereoOpts() {
		var menu = document.tester;
		if (menu.setStereoSearch.value === '<%= SearchConstants.STEREO_IGNORE %>') {
			memDoubleBondStereo = menu.considerDoubleBondStereoMatching.checked;
			menu.considerDoubleBondStereoMatching.checked = true;
			menu.considerDoubleBondStereoMatching.disabled = true;
		} else {
			menu.considerDoubleBondStereoMatching.disabled = false;
			menu.considerDoubleBondStereoMatching.checked = memDoubleBondStereo;
		}
	}

	function orderCell() {
		var menu = document.tester;
		if (menu.listAllMatches.checked) {
			showCell('orderSensitiveCell1');
			showCell('orderSensitiveCell2');
		} else {
			hideCell('orderSensitiveCell1');
			hideCell('orderSensitiveCell2');
		}
	}

	// -->
</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="compareJChem.jsp" method="post">
<input type="hidden" name="target" value=""/>
<input type="hidden" name="query" value=""/>
<p class="boldtext big" style="text-align:center;">
JChem<sup>&reg;</sup> Structure Comparator
</p>
<table style="margin-left:auto; margin-right:auto;">
<tr><td id="intro" class="regtext" colspan="2">
	Please draw two structures and compare them with JChem<sup>&reg;</sup>.    
	Draw explicit H atoms on an atom in the query (author's structure) 
	to exclude substitution at that site in the target (response).
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
				MARVIN, SHOWNOH, 
				'<%= QUERY_APPLET %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td></tr>
</table>

<table style="margin-left:auto; margin-right:auto;">
<tr><td style="text-align:center; width:60%;">
	<table class="regtext" >
	<tr><td>
		type of match
	</td><td>
		<select name="searchType" id="searchType" onchange="disableOptions()">
			<option value="<%= SearchConstants.SUBSTRUCTURE %>">substructure</option>
			<option value="<%= SearchConstants.FULL_FRAGMENT %>" selected="selected">full fragment</option>
			<option value="<%= SearchConstants.FULL %>" selected="selected">full</option>
			<option value="<%= SearchConstants.DUPLICATE %>">duplicate (diagonal)</option>
		</select>
	</td></tr>
	<tr><td>
		tetrahedral stereo mode
		<br/>(specific means unspecified query matches specified target)
	</td><td>
		<select name="setStereoSearch" id="setStereoSearch" onchange="setStereoOpts()">
			<option value="<%= SearchConstants.STEREO_IGNORE %>">ignore</option>
			<option value="<%= SearchConstants.STEREO_SPECIFIC %>" selected="selected">specific (default)</option>
			<option value="<%= SearchConstants.STEREO_DIASTEREOMER %>">exact or specified stereoisomer</option>
			<option value="<%= SearchConstants.STEREO_ENANTIOMER %>">exact or enantiomer</option>
			<option value="<%= SearchConstants.STEREO_EXACT %>">exact (diagonal)</option>
		</select>
	</td></tr>
	<tr><td>
		consider double bond stereochemistry
	</td><td>
		<input type="checkbox" name="considerDoubleBondStereoMatching" id="considerDoubleBondStereoMatching" 
				checked="checked"/>
	</td></tr>
	<tr><td>
		consider stereochemistry of odd-numbered cumulenes
		<br/>(requires global stereo matching model)
	</td><td>
		<input type="checkbox" name="considerOddCumuleneStereoMatching" id="considerOddCumuleneStereoMatching" 
				checked="checked"/>
	</td></tr>
	<tr><td>
		consider axial stereochemistry
		<br/>(requires global stereo matching model)
	</td><td>
		<input type="checkbox" name="considerAxialStereoMatching" id="considerAxialStereoMatching" 
				checked="checked"/>
	</td></tr>
	<tr><td>
		consider syn/anti stereochemistry
	</td><td>
		<input type="checkbox" name="considerSynAntiStereoMatching" id="considerSynAntiStereoMatching" 
				checked="checked"/>
	</td></tr>
	<tr><td>
		stereo matching model
	</td><td>
		<select name="stereoMatchingModel" id="stereoMatchingModel">
			<option value="<%= SearchConstants.STEREO_MODEL_LOCAL %>">local</option>
			<option value="<%= SearchConstants.STEREO_MODEL_GLOBAL %>">global</option>
			<option value="<%= SearchConstants.STEREO_MODEL_COMPREHENSIVE %>">comprehensive</option>
		</select>
	</td></tr>
	<tr><td>
		charge matching
	</td><td>
		<select name="chargeType" id="chargeType">
			<option value="<%= SearchConstants.CHARGE_MATCHING_EXACT %>">exact</option>
			<option value="<%= SearchConstants.CHARGE_MATCHING_DEFAULT %>">default</option>
			<option value="<%= SearchConstants.CHARGE_MATCHING_IGNORE %>">ignore</option>
		</select>
	</td></tr>
	<tr><td>
		radical matching
	</td><td>
		<select name="radicalType" id="radicalType">
			<option value="<%= SearchConstants.RADICAL_MATCHING_EXACT %>">exact</option>
			<option value="<%= SearchConstants.RADICAL_MATCHING_DEFAULT %>">default</option>
			<option value="<%= SearchConstants.RADICAL_MATCHING_IGNORE %>">ignore</option>
		</select>
	</td></tr>
	<tr><td>
		isotope matching
	</td><td>
		<select name="isotopeType" id="isotopeType">
			<option value="<%= SearchConstants.ISOTOPE_MATCHING_EXACT %>">exact</option>
			<option value="<%= SearchConstants.ISOTOPE_MATCHING_DEFAULT %>">default</option>
			<option value="<%= SearchConstants.ISOTOPE_MATCHING_IGNORE %>">ignore</option>
		</select>
	</td></tr>
	<tr><td>
		valence matching
	</td><td>
		<select name="valenceType" id="valenceType">
			<option value="true">true</option>
			<option value="false">false</option>
		</select>
	</td></tr>
	<tr><td>
		bond vagueness
	</td><td>
		<select name="bondVagueness" id="bondVagueness">
			<option value="<%= SearchConstants.VAGUE_BOND_OFF %>">off</option>
			<option value="<%= SearchConstants.VAGUE_BOND_LEVEL_HALF %>">level 0.5</option>
			<option value="<%= SearchConstants.VAGUE_BOND_LEVEL1 %>" 
					selected="selected">level 1 (default)</option>
			<option value="<%= SearchConstants.VAGUE_BOND_LEVEL2 %>">level 2</option>
			<option value="<%= SearchConstants.VAGUE_BOND_LEVEL3 %>">level 3</option>
			<option value="<%= SearchConstants.VAGUE_BOND_LEVEL4 %>">level 4</option>
		</select>
	</td></tr>
	<tr><td>
		list all matches' atom index arrays
	</td><td>
		<input type="checkbox" name="listAllMatches" id="listAllMatches" 
				onclick="javascript:orderCell()"/>
	</td></tr>
	<tr><td id="orderSensitiveCell1" style="visibility:hidden">
		set OrderSensitiveSearch to true
	</td><td id="orderSensitiveCell2" style="visibility:hidden">
		<input type="checkbox" name="orderSensitive" id="orderSensitive"/>
	</td></tr>
	</table>
</td><td style="text-align:center;">
	<br/>
	<input type="button" id="submit" value=" View Comparison " onclick="callServer()"/>
	<br/>
</td></tr>

<tr><td id="allMatches" class="regtext" style="padding-bottom:20px;" colspan="2">
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
