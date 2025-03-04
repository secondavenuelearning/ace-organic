<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.Molecule,
	com.epoch.AppConfig,
	com.epoch.chem.MolString,
	com.epoch.chem.StereoFunctions,
	com.epoch.chem.Normalize,
	com.epoch.qBank.Question,
	com.epoch.utils.Utils,
	java.io.File,
	java.io.FileOutputStream"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final String reactants = request.getParameter("reactants");
	// Utils.alwaysPrint("standardize.jsp: reactants = ", reactants);
	String products = null;

	if (reactants != null) {
		try {
			products = Normalize.normalize(reactants);
			/* final Molecule prodMol = MolImporter.importMol(products);
			StereoFunctions.allCrissCrossToWavy(prodMol);
			products = MolString.toString(prodMol);
			*/
			// Utils.alwaysPrint("standardize.jsp: got products:\n", products);
		} catch (Exception e) {
			products = reactants;
		}
	}
	final String APPLET_NAME = "responseApplet";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Standardizer test</title>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>

<!-- place values so updatePage() can find them
	follows ACE conventions: four-@-delimited strings.
	reactantsValue = @@@@<%= Utils.lineBreaksToJS(reactants) %>@@@@
	reactantsXMLValue = @@@@<%= Utils.lineBreaksToJS(
			MolString.getImage(pathToRoot, reactants, 0L, "reactants", false)) %>@@@@
	productsXMLValue = @@@@<%= Utils.lineBreaksToJS(
			MolString.getImage(pathToRoot, products, 0L, "products", false)) %>@@@@
-->

<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= 
		AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<script type="text/javascript">
	// <!-- >

	<%@ include file="../js/marvinQuestionConstants.jsp.h" %>

	function loadSelections() { ; }

	function callServer() {
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(mol) {
			// alert('calling server with ' + mol);
			var url = 'standardize.jsp';
			var toSend = 'reactants=' + encodeURIComponent(mol);
			setValue('submit', ' Processing ... ');
			disableCell('submit');
			callAJAX(url, toSend);
		}, function(error) {
			alert('Molecule export failed:' + error);	
		});
	} // callServer()

	function updatePage() {
		if (xmlHttp.readyState === 4) { // ready to continue
			var response = xmlHttp.responseText;
			// the response is the entire web page
			var reactantsValue = extractField(response, 'reactantsValue');
			var reactantsXMLValue = extractField(response, 'reactantsXMLValue');
			var productsXMLValue = extractField(response, 'productsXMLValue');
			// use innerHTML to change text & images upon receipt of results
			if (!isEmpty(reactantsValue)) {
				setInnerHTML('reactants', reactantsXMLValue);
				setInnerHTML('reactionArrow', 
						'<img style="vertical-align:middle;" '
						+ 'hspace=20 height=12 width=64 src="img/rxnarrow.jpeg">');
				setInnerHTML('products', productsXMLValue);
				setInnerHTML('intro', 'Try it again!<br/>&nbsp;');
			} else {
				setInnerHTML('intro', 'No reactants detected; try again. ');
			}				
			enableCell('submit');
			setValue('submit', ' View Standardized ');
		} // ready to continue
	} // updatePage()

	// -->
</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="standardize.jsp" method="post">
<input type="hidden" name="reactants" value=""/>
<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr><td colspan="2">
<p class="boldtext big" style="text-align:center;">
ACE standardization tester 
<br/>&nbsp;
</td></tr>
<tr><td class="regtext" id="intro"  colspan="2">
	Draw a compound and see how it is standardized.
	<br/>&nbsp;
</td></tr>
<tr style="text-align:center;" valign="middle">
<td style="text-align:center;" colspan="2">
	<table style="margin-left:auto; margin-right:auto;"><tr>
		<td style="text-align:center;" id="reactants"></td>
		<td style="text-align:center;" id="reactionArrow"></td>
		<td style="text-align:center;" id="products"></td>
	</tr></table>
</td>
</tr>
<tr>
<td style="text-align:center;" colspan="2">
	<div id="<%= APPLET_NAME %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('',
				MARVIN, 0, '<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td>
</tr>
<tr>
<td style="text-align:center;" colspan="2">
	<br/>
	<input type="button" id="submit" value=" View Standardized " onclick="callServer()"/>
	<br/>
	<br/>
</td>
</tr>

<tr>
<td align="left" class="regtext" >
<a href="welcome.html">Back</a> to public ACE pages.
</td>
</tr>

</table>
</form>
</body>
</html>
