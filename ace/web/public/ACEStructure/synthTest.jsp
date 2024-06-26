<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolFormatException,
	chemaxon.formats.MolImporter,
	chemaxon.struc.Molecule,
	com.epoch.AppConfig,
	com.epoch.chem.ChemUtils,
	com.epoch.chem.MolString,
	com.epoch.qBank.Question,
	com.epoch.synthesis.RxnCondition,
	com.epoch.synthesis.SynthError,
	com.epoch.synthesis.SynthSolver,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.Map"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final int[] reactionIds = RxnCondition.getAllReactionIds();
	final Map<Integer, String> reactionNamesByIds =
			RxnCondition.getRxnNamesKeyedByIds();
	final Map<Integer, String> reactionDefsByIds =
			RxnCondition.getRxnDefsKeyedByIds();
	final String allRxnsStr = RxnCondition.alphabetize(
			Utils.join(reactionIds, ":"), reactionNamesByIds);
	final String[] allRxns = allRxnsStr.split(":");
			
	final String reactants = request.getParameter("reactants");
	// if (reactants != null) Utils.alwaysPrint("synthTest.jsp: reactants = ", reactants);
	String products = null;
	final int chosenRxnNum = MathUtils.parseInt(request.getParameter("reactionNum"), -1);
	final String reactionDef = (chosenRxnNum != -1 ?
			reactionDefsByIds.get(chosenRxnNum) : null);
	final boolean productInMarvin = "true".equals(request.getParameter("productInMarvin"));
	
	final String reactionName =
			Utils.toDisplay(request.getParameter("reactionName"));
	boolean exceptionCaught = false;
	String errorMsg = "";
	String minorProds = "";
	if (reactants != null && chosenRxnNum != -1) {
		try {
			final SynthSolver solver = new SynthSolver();
			final Molecule[] productsArray = 
					solver.getProducts(reactants, chosenRxnNum);
			// Utils.alwaysPrintMRV("synthTest.jsp: got products:\n", productsArray);
			if (!Utils.isEmpty(productsArray)) {
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
				products = MolString.toString(productsMol, Utils.MRV);
				minorProds = MolString.toString(minorProdsMol, Utils.SMILES);
			} else {
				// Utils.alwaysPrint("synthTest.jsp: products array is null.");
				products = null;
			}
		} catch (SynthError e) {
			// Utils.alwaysPrint("synthTest.jsp: SynthError caught: products is null.");
			exceptionCaught = true;
			errorMsg = (e.errorNumber != SynthError.USE_MENU ?
					e.getErrorFeedback() : "Your submission contains a compound, "
					+ Utils.toDisplay(e.calcdProds) 
					+ ", that you should not write as one of "
					+ "the reagents, but should choose from the pulldown menu."); 
			products = null;
		} catch (Exception e) {
			// Utils.alwaysPrint("synthTest.jsp: Exception caught: products is null.");
			e.printStackTrace();
			exceptionCaught = true;
			errorMsg = e.getMessage();
			products = null;
		}
	}

	final String rxnWindowUrl = pathToRoot + "authortool/chooseRxnCondsUser.jsp"
			+ "?fromSynthTest=true&amp;rxnNum=1&amp;allowedRxns=" + allRxnsStr;
	final String APPLET_NAME = "synthAuthApplet";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<title>Synthesis calculator</title>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>

<!-- place values so updatePage() can find them
	follows ACE conventions: four-@-delimited strings.
	exceptionCaughtValue = @@@@<%= exceptionCaught %>@@@@
	errorMsgValue = @@@@<%= errorMsg %>@@@@
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
	chosenRxnNumValue = @@@@<%= chosenRxnNum != -1 ? chosenRxnNum : ""  %>@@@@
	productInMarvinValue = @@@@<%= productInMarvin %>@@@@
	reactionNameValue = @@@@<%= Utils.unicodeToCERs(reactionName) %>@@@@
-->

<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= 
		AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<script type="text/javascript">
	// <!-- >

	<%@ include file="../../js/marvinQuestionConstants.jsp.h" %>

	function loadSelections() { ; }

	function callServer() {
		var url = 'synthTest.jsp';
		var bld = new String.builder();
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(mol) {
			// alert('calling server with ' + mol);
			bld.append('reactants=').
					append(encodeURIComponent(mol)). 
					append('&productInMarvin=').
					append(document.tester.productInMarvin.checked).
					append('&reactionNum=').
					append(document.tester.reaction1Id.value).
					append('&reactionName=').
					append(encodeURIComponent(getInnerHTML('reaction1Name')));
			// alert(toSend);
			setValue('submit', ' Processing ... ');
			disableCell('submit');
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
				setInnerHTML('intro', 'ACE threw an exception during the calculation: ');
				document.getElementById('reactionImage').style.textAlign = 'left';
				setInnerHTML('reactionImage', '<p>' + errorMsgValue + '<br/><br/>');
			} else {
				var reactantsValue = extractField(response, 'reactantsValue');
				var productsValue = extractField(response, 'productsValue').replace(/\\N/g, '\\n');
				var minorProdsValue = extractField(response, 'minorProdsValue');
				var reactionNameValue = extractField(response, 'reactionNameValue');
				// alert('reactants = ' + reactantsValue);
				// alert('products = ' + productsValue);
				// alert('minor products = ' + minorProdsValue);
				// alert('reactionNameValue = ' + reactionNameValue);
				if (reactantsValue !== '') {
					if (productsValue !== '') {
						var productInMarvinValue = 
								extractField(response, 'productInMarvinValue');
						if (productInMarvinValue === 'true') {
							marvinSketcherInstances['<%= APPLET_NAME %>'].
									importStructure('<%= Utils.MRV %>', productsValue);
						}
						document.getElementById('reactionImage').style.textAlign = 
								'center';
						var outBld = new String.builder();
						outBld.append('<table><tr>'
									+ '<td id="reactants" '
									+ 'style="padding-right:10px; vertical-align:middle;">'
									+ '<\/td><td style="vertical-align:middle;">'
									+ '<table><tr><td class="regtext" '
									+ 'style="text-align:center">').
								append(reactionNameValue).
								append('<\/td><\/tr><tr>'
									+ '<td style="vertical-align:top; text-align:left;">'
									+ '<img hspace=20 height=12 '
									+ 'src="<%= pathToRoot %>public/img/rxnarrow.jpeg">'
									+ '<\/td><\/tr><\/table>'
									+ '<\/td><td id="products" '
									+ 'style="padding-left:10px; vertical-align:middle;">'
									+ '<\/td>'
									+ '<\/tr><\/table>');
						setInnerHTML('reactionImage', outBld.toString());
						var reactantsXMLValue = extractField(response, 'reactantsXMLValue');
						var productsXMLValue = extractField(response, 'productsXMLValue');
						setInnerHTML('reactants', reactantsXMLValue);
						setInnerHTML('products', productsXMLValue);
						if (!isEmpty(minorProdsValue))
							setInnerHTML('minorProds', 'The reaction also produces minor '
									+ 'products ' + minorProdsValue + ' which you can '
									+ 'copy and paste into Marvin below.');
						else setInnerHTML('minorProds', '&nbsp;');
						setInnerHTML('intro', 
								'The products are shown below.  Try it again!<br/>&nbsp;');
					} else {
						setInnerHTML('intro', 'No products were calculated from '
								+ 'this reaction.  Try different starting materials '
								+ 'or a different reaction.  ');
						setInnerHTML('reactionImage', '&nbsp;');
						setInnerHTML('minorProds', '&nbsp;');
					}
				} else {
					setInnerHTML('intro', 
							'No starting materials were detected; try again. ');
				}				
			}
			enableCell('submit');
			setValue('submit', ' View Products ');
		} // ready to continue
	} // updatePage()

	function setReactionDef(rxnId) {
		<% for (int rxnNum = 0; rxnNum < reactionIds.length; rxnNum++) { 
			final String rxnDef = reactionDefsByIds.get(reactionIds[rxnNum]); %>
			if (rxnId === <%= reactionIds[rxnNum] %>) 
				setInnerHTML('reactionDef', '<%= Utils.toValidHTML(rxnDef) %>');
		<% } %>
	}

	// -->
</script>
</head>

<body style="overflow:auto;">

<!-- self submit form -->
<form name="tester" action="synthTest.jsp" method="post">
<input type="hidden" name="reactants" value=""/>
<table style="margin-left:auto; margin-right:auto; width:600px;">
	<tr>
		<td>
			<p class="boldtext big" style="text-align:center;">
			ACE synthesis calculator
		</td>
	</tr>
	<tr>
		<td class="regtext" id="intro">
			Draw one or more compounds, choose reaction conditions,
			and press <b>View Products</b> to calculate the
			products of the reaction (if any).  <br/>&nbsp;
		</td>
	</tr>
	<tr>
		<td class="regtext">
			Note: This page neither retrieves previously calculated 
			products from the database nor stores newly calculated 
			products in the database.<br/>&nbsp;
		</td>
	</tr>
	<tr style="text-align:center;" valign="middle">
		<td class="regtext" id="reactionImage" valign="middle">
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
				startMarvinJS('', 
						-SYNTHESIS, SHOWMAPPING, 
						'<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
				// -->
			</script>
			</div>
		</td>
	</tr>
	<tr style="height:40px;">
		<td class="regtext" style="vertical-align:top; height:40px;">
			<br/>Reaction conditions (click on them to alter them): 
			<input type="hidden" id="reaction1Id" 
					value="<%= RxnCondition.NO_REAGENTS %>"/>
			<input type="hidden" id="reaction1OrigName" 
					value="<%= reactionNamesByIds.get(Integer.valueOf(
							RxnCondition.NO_REAGENTS)) %>"/>
			<a onclick="openReactionWindow('<%= rxnWindowUrl %>')">
			<span id="reaction1Name"><%= Utils.toDisplay(
					reactionNamesByIds.get(Integer.valueOf(
							RxnCondition.NO_REAGENTS))) %></span>
			</a>
		</td>
	</tr>
	<tr><td class="regtext" >
		<br/><input type="checkbox" name="productInMarvin" id="productInMarvin"/>
		replace starting materials with product in Marvin window
	</td></tr>
	<tr>
		<td style="text-align:center;">
			<br/>
			<input type="button" id="submit" value=" View Products " 
					onclick="callServer();"/>
			<br/>
		</td>
	</tr>
	<tr>
		<td align="left" class="regtext" >
			<a href="index.html">Back</a> to developer pages.
		</td>
	</tr>
</table>
</form>

<p class="regtext">Definition of the selected reaction (if the reaction 
definition has changed since this page was most recently loaded, 
<br/>the old definition will be displayed, but the new definition will 
be used to process reactants) (<a href="synthGlossary.html">glossary</a>):
<pre><span id="reactionDef"><%= Utils.toValidHTML(
		reactionDefsByIds.get(reactionIds[0])) %></span></pre>

</body>
</html>
