<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolFormatException,
	chemaxon.formats.MolImporter,
	chemaxon.struc.MolAtom,
	chemaxon.struc.Molecule,
	com.epoch.AppConfig,
	com.epoch.chem.ChemUtils,
	com.epoch.chem.MolString,
	com.epoch.qBank.Question,
	com.epoch.synthesis.SynthError,
	com.epoch.synthesis.SynthSolver,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final String reactants = request.getParameter("reactants");
	String products = null;

	final String reactionDef = request.getParameter("reaction");
	final String productInMarvinStr = request.getParameter("productInMarvin");
	final boolean productInMarvin = productInMarvinStr != null
			&& Utils.among(productInMarvinStr, "true", "yes", "on");
	final String reverseStr = request.getParameter("reverse");
	final boolean reverse = reverseStr != null
			&& Utils.among(reverseStr, "true", "yes", "on");
	
	boolean exceptionCaught = false;
	String errorMsg = "";
	String minorProds = "";
	if (reactants != null && reactionDef != null) {
		try {
			final SynthSolver solver = new SynthSolver();
			final Molecule[] productsArray = 
					solver.getProducts(reactants, reactionDef, reverse);
			if (!Utils.isEmpty(productsArray)) {
				Utils.alwaysPrint("pasteSynthTest.jsp: productsArray:\n", productsArray);
				final Molecule productsMol = new Molecule();
				final Molecule minorProdsMol = new Molecule();
				for (final Molecule product : productsArray) {
					final String minorValue =
							ChemUtils.getProperty(product, SynthSolver.MAJ_MIN_PROD);
					if (SynthSolver.MINOR.equals(minorValue)) {
						minorProdsMol.fuse(product);
					} else productsMol.fuse(product);
				}
				ChemUtils.clean2D(productsMol, "2");
				ChemUtils.clean2D(minorProdsMol, "2");
				ChemUtils.implicitizeH(productsMol, MolAtom.ALL_H & ~MolAtom.WEDGED_H);
				products = MolString.toString(productsMol, Utils.MRV);
				minorProds = MolString.toString(minorProdsMol, Utils.SMILES);
			} else {
				products = null;
			}
		} catch (SynthError e) {
			Utils.alwaysPrint("pasteSynthTest.jsp: SynthError caught: products is null.");
			exceptionCaught = true;
			errorMsg = e.getErrorFeedback();
			products = null;
		} catch (Exception e) {
			Utils.alwaysPrint("pasteSynthTest.jsp: Exception caught: products is null.");
			e.printStackTrace();
			exceptionCaught = true;
			errorMsg = e.getMessage();
			products = null;
		}
	}
	// there are two applets, but one is for drawing only
	final String APPLET_NAME = "starters";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>Reaction definition tester</title>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>

<!-- place values so updatePage() can find them
	follows ACE conventions: four-@-delimited strings.
	exceptionCaughtValue = @@@@<%= exceptionCaught %>@@@@
	errorMsgValue = @@@@<%= Utils.lineBreaksToJS(errorMsg) %>@@@@
	reactantsValue = @@@@<%= Utils.lineBreaksToJS(reactants) %>@@@@
	productsValue = @@@@<%= Utils.lineBreaksToJS(products == null ? null
			: products.replaceAll("\\\\n", "\\\\N")) %>@@@@
	reactantsXMLValue = @@@@<%= Utils.lineBreaksToJS(
			MolString.getImage(pathToRoot, reactants, Question.SHOWLONEPAIRS, 
				"reactants", false)) %>@@@@
	productsXMLValue = @@@@<%= Utils.lineBreaksToJS(
			MolString.getImage(pathToRoot, products, Question.SHOWLONEPAIRS, 
				"products", false)) %>@@@@
	minorProdsValue = @@@@<%= Utils.lineBreaksToJS(minorProds) %>@@@@
	productInMarvinValue = @@@@<%= productInMarvin %>@@@@
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

	<%@ include file="../../js/marvinQuestionConstants.jsp.h" %>

	// define universal JS variables; initial values from Java values

	function loadSelections() { ; }

	function callServer() {
		var url = 'pasteSynthTest.jsp';
		var bld = new String.builder();
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(mol) {
			bld.append('reactants=').
					append(encodeURIComponent(mol)).
					append('&productInMarvin=').
					append(document.tester.productInMarvin.checked).
					append('&reverse=').
					append(document.tester.reverse.checked).
					append('&reaction=').
					append(encodeURIComponent(document.tester.reaction.value));
			// alert(toSend);
			setValue('submit', ' Processing ... ');
			disableCell('submit');
			clearInnerHTML('errorMsg');
			callAJAX(url, bld.toString());
		}, function(error) {
			alert('Molecule export failed:' + error);	
		});
	} // callServer()

	function updatePage() {
		if (xmlHttp.readyState === 4) { // ready to continue
			var response = xmlHttp.responseText;
			// the response is the entire web page
			var exceptionCaughtValue = extractField(response, 'exceptionCaughtValue');
			if (exceptionCaughtValue === 'true') {
				var errorMsgValue = extractField(response, 'errorMsgValue');
				setInnerHTML('intro', 'ACE threw an exception during the calculation. '
						+ 'There is probably an error in the reaction definition\'s '
						+ 'reactivity or selectivity rules.  See the error message below.');
				setInnerHTML('reactionImage', '&nbsp;');
				setInnerHTML('errorMsg', '<br/><br/>' + errorMsgValue);
			} else {
				var reactantsValue = extractField(response, 'reactantsValue');
				var productsValue = extractField(response, 'productsValue').replace(/\\N/g, '\\n');
				var minorProdsValue = extractField(response, 'minorProdsValue');
				// alert('reactants = ' + reactantsValue);
				// alert('products = ' + productsValue);
				// alert('minor products = ' + minorProdsValue);
				if (!isEmpty(reactantsValue)) {
					if (!isEmpty(productsValue)) {
						var productInMarvinValue = 
								extractField(response, 'productInMarvinValue');
						if (productInMarvinValue === 'true') {
							marvinSketcherInstances['<%= APPLET_NAME %>'].
									importStructure('<%= Utils.MRV %>', productsValue);
						}
						var reactantsXMLValue = 
								extractField(response, 'reactantsXMLValue');
						var productsXMLValue = 
								extractField(response, 'productsXMLValue');
						setInnerHTML('reactants', reactantsXMLValue);
						setInnerHTML('reactionArrow', 
								'<img style="vertical-align:middle;" hspace=20 '
									+ 'height=12 width=64 '
									+ 'src="<%= pathToRoot %>public/img/rxnarrow.jpeg">');
						setInnerHTML('products', productsXMLValue);
						setInnerHTML('intro', 
								'The products are shown below.  Try it again!<br/>&nbsp;');
						if (!isEmpty(minorProdsValue))
							setInnerHTML('minorProds', 'The reaction also produces minor '
									+ 'products ' + minorProdsValue + ' which you can '
									+ 'copy and paste into Marvin below.');
						else setInnerHTML('minorProds', '&nbsp;');
					} else {
						setInnerHTML('intro', 'No products were calculated from '
								+ 'this reaction. Try different starting materials '
								+ 'or a different reaction.  ');
						setInnerHTML('reactants', '&nbsp;');
						setInnerHTML('reactionArrow', '&nbsp;');
						setInnerHTML('products', '&nbsp;');
						setInnerHTML('minorProds', '&nbsp;');
					}
				} else {
					setInnerHTML('intro', 'No starting materials were detected; try again. ');
				}			
			}
			enableCell('submit');
			setValue('submit', ' View Products ');
		} // ready to continue
	} // updatePage()

	// -->
</script>
</head>

<body style="overflow:auto;">

<!-- self submit form -->
<form name="tester" action="pasteSynthTest.jsp" method="post">
<input type="hidden" name="reactants" value=""/>
<table style="margin-left:auto; margin-right:auto; width:600px;">
<tr><td class="boldtext big" style="text-align:center;">
ACE Reaction definition tester
</td></tr>
<tr><td class="regtext" id="intro" >
	<br/>Draw one or more compounds, paste a reaction definition,
	and press <b>View Products</b> to calculate the
	products.  <br/>&nbsp;
</td></tr>
	<tr style="text-align:center;">
	<td style="text-align:center;" id="reactionImage">
	<table style="margin-left:auto; margin-right:auto;"><tr>
		<td style="text-align:center;" id="reactants"></td>
		<td style="text-align:center;" id="reactionArrow"></td>
		<td style="text-align:center;" id="products"></td>
	</tr></table>
	</td>
	</tr>
	<tr style="text-align:center;" valign="middle">
		<td class="regtext" id="minorProds" valign="middle">
		</td>
	</tr>
<tr>
<td style="text-align:center;">
	<div id="<%= APPLET_NAME %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('', -SYNTHESIS, SHOWMAPPING, 
				'<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td>
</tr>
<tr><td class="regtext" >
<br/><input type="checkbox" name="productInMarvin" id="productInMarvin"/>
replace starting materials with product in Marvin window
</td></tr>
<tr><td class="regtext" >
<br/><input type="checkbox" name="reverse" id="reverse"/>
run reaction in reverse
</td></tr>
<tr>
<td style="text-align:center;">
	<br/>
	<input type="button" id="submit" value=" View Products " onclick="callServer();"/>
	<br/>
</td>
</tr>
<tr>
<td class="regtext" ><br/><br/>Paste a reaction definition 
(<a href="synthGlossary.html">glossary</a>):
	<br/><br/><textarea id="reaction" name="reaction" rows="10" cols="40"
		style="height:400px; width:100%; font-family:Courier;"></textarea>
</td>
</tr>
<tr>
<td align="left" class="regtext" >
<pre><span id="errorMsg"></span></pre>
</td>
</tr>
<tr>
<td class="regtext" ><br/><br/>Use this window to edit the reaction definition.
(Only the contents of the textbox above will be used to calculate products.)
</td>
</tr>
<tr>
<td style="text-align:center;">
	<div id="synthEditorApplet" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('', -SYNTHESIS, SHOWMAPPING, 'synthEditorApplet'); 
		// -->
	</script>
	</div>
</td>
</tr>
<tr>
<td align="left" class="regtext" >
<br/><a href="index.html">Back</a> to developer pages.
</td>
</tr>
</table>

</form>
</body>
</html>


