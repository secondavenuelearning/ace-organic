<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.Molecule,
	com.epoch.AppConfig,
	com.epoch.qBank.Question,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	String moleculeStr = request.getParameter("molecule");
	// Utils.alwaysPrint("fragmentationTest.jsp: moleculeStr = ", moleculeStr);
	int numFrags = 0;

	if (!Utils.isEmptyOrWhitespace(moleculeStr)) {
		final Molecule mol = MolImporter.importMol(moleculeStr);
		final Molecule[] frags = mol.convertToFrags();
		numFrags = frags.length;
		Utils.alwaysPrintMRV("fragmentationTest.jsp: original mol:\n",
				moleculeStr, "converted to ", numFrags,
				" fragments:\n", frags);
	} else moleculeStr = "<cml xmlns=\"http://www.chemaxon.com\" version=\"ChemAxon file format v20.9.0, generated by vunknown\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.chemaxon.com http://www.chemaxon.com/marvin/schema/mrvSchema_20_9_0.xsd\"><MDocument><MChemicalStruct><molecule molID=\"m1\"><atomArray><atom id=\"a1\" elementType=\"C\" x2=\"-3.6666666666666665\" y2=\"2.7916666666666665\" sgroupRef=\"sg1\"/><atom id=\"a2\" elementType=\"C\" x2=\"-3.6666666666666665\" y2=\"1.2516666666666665\" sgroupRef=\"sg1\"/><atom id=\"a3\" elementType=\"Pd\" x2=\"-6.333333333333333\" y2=\"2.125\"/><atom id=\"a4\" elementType=\"X\" x2=\"-5\" y2=\"2.0733333333333333\"/></atomArray><bondArray><bond id=\"b1\" atomRefs2=\"a1 a2\" order=\"2\"/><bond id=\"b2\" atomRefs2=\"a4 a3\" convention=\"cxn:coord\"/></bondArray><molecule molID=\"m2\" id=\"sg1\" role=\"MulticenterSgroup\" atomRefs=\"a2 a1\" center=\"a4\"/></molecule></MChemicalStruct></MDocument></cml>";
	final String APPLET_NAME = "Marvin";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Fragment Molecules Test</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>

<!-- place values so updatePage() can find them
	follows ACE conventions: four-@-delimited strings.
	moleculeStrValue = @@@@<%= Utils.lineBreaksToJS(moleculeStr == null ? null
			: moleculeStr.replaceAll("\\\\n", "\\\\N")) %>@@@@
	numFragsValue = @@@@<%= numFrags %>@@@@
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

	function loadSelections() { ; }

	function callServer() {
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(mol) {
			// alert('calling server with ' + mol);
			var url = 'fragmentationTest.jsp';
			var toSend = 'molecule=' + encodeURIComponent(mol); 
			// alert(toSend);
			setValue('submit', ' Processing ... ');
			disableCell('submit');
			callAJAX(url, toSend);
		}, function(error) {
			alert('Molecule export failed:' + error);	
		});
	} // callServer

	function updatePage() {
		if (xmlHttp.readyState === 4) { // ready to continue
			var response = xmlHttp.responseText;
			// the response is the entire web page
			var moleculeStrValue = extractField(response, 'moleculeStrValue').replace(/\\N/g, '\\n');
			var numFragsValue = extractField(response, 'numFragsValue');
			// alert('moleculeStr = ' + moleculeStrValue);
			// use innerHTML to change text & images upon receipt of results
			if (!isEmpty(moleculeStrValue)) {
				marvinSketcherInstances['<%= APPLET_NAME %>'].
						importStructure('<%= Utils.MRV %>', moleculeStrValue);
				var text = (numFragsValue !== '1' ?
						'Your structure was fragmented into ' + numFragsValue
						+ ' independent molecules.' 
						: 'Your structure could not be fragmented.');
				text += '  Try again, if you like.';
				setInnerHTML('english', text);
			}
			enableCell('submit');
			setValue('submit', ' Fragment ');
		} // ready to continue
	} // updatePage

	// -->
</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="fragmentationTest.jsp" method="post">
<input type="hidden" name="molecule" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Test Molecule Fragmentation
</P>
<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr><td id="english" class="regtext" colspan="2">
	Please draw a compound, 
	and press the button to see how many fragments it contains.
</td></tr>
<tr><td>
	&nbsp;
</td></tr>

<tr><td style="text-align:center;">
	<div id="<%= APPLET_NAME %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('<%= moleculeStr != null ? Utils.toValidJS(moleculeStr) : "" %>', 
				-MARVIN, 0, '<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td></tr>
<tr><td>&nbsp;</td></tr>
<tr><td style="text-align:center;">
	<br/>
	<input type="button" id="submit" value=" Fragment " onclick="callServer();"/>
	<br/>
</td></tr>
<tr><td class="regtext" >
	<br/>
	<a href="welcome.html">Back</a> to public ACE pages.
	<br/>
</td></tr>
</table>
</form>

</body>
</html>
