<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.Molecule,
	com.epoch.AppConfig,
	com.epoch.chem.MolString,
	com.epoch.chem.StereoFunctions,
	com.epoch.qBank.Question,
	com.epoch.utils.Utils,
	java.util.List"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	String moleculeStr = request.getParameter("molecule");
	// Utils.alwaysPrint("enumerateStereoisomers.jsp: moleculeStr = ", moleculeStr);
	int numMols = 0;

	if (!Utils.isEmptyOrWhitespace(moleculeStr)) {
		final Molecule mol = MolImporter.importMol(moleculeStr);
		final List<Molecule> molList = StereoFunctions.enumerateStereo(mol);
		numMols = molList.size();
		final Molecule combined = new Molecule();
		for (final Molecule cmol : molList) combined.fuse(cmol);
		moleculeStr = MolString.toString(combined, Utils.SMILES);
		moleculeStr = MolString.convertMol(moleculeStr, MRV_EXPORT);
	} else moleculeStr = "";
	final String APPLET_NAME = "Marvin";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Generate Stereoisomers of Unspecified Structures</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>

<!-- place values so updatePage() can find them
	follows ACE conventions: four-@-delimited strings.
	moleculeStrValue = @@@@<%= Utils.lineBreaksToJS(moleculeStr == null ? null
			: moleculeStr.replaceAll("\\\\n", "\\\\N")) %>@@@@
	numMolsValue = @@@@<%= numMols %>@@@@
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
			var url = 'enumerateStereoisomers.jsp';
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
			var numMolsValue = extractField(response, 'numMolsValue');
			// alert('moleculeStr = ' + moleculeStrValue);
			// use innerHTML to change text & images upon receipt of results
			if (!isEmpty(moleculeStrValue)) {
				if (document.Marvin) {
					document.Marvin.setMol(moleculeStrValue);
				} else {
					marvinSketcherInstances['<%= APPLET_NAME %>'].
							importStructure('<%= Utils.MRV %>', moleculeStrValue);
				} // if Java applet
				var text = (numMolsValue !== '1' ?
						'Here are the ' + numMolsValue
						+ ' calculated stereoisomers.' 
						: 'Here is the one calculated stereoisomer.');
				text += '  Try again, if you like.';
				setInnerHTML('english', text);
			}
			enableCell('submit');
			setValue('submit', ' Get isomers ');
		} // ready to continue
	} // updatePage

	// -->
</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="enumerateStereoisomers.jsp" method="post">
<input type="hidden" name="molecule" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Generate Stereoisomers of Unspecified Structures
</P>
<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr><td id="english" class="regtext" colspan="2">
	Please draw a compound without stereocenters specified, 
	and press the button to see all the stereoisomers.  Note:
	Bonds whose configurations are already specified will remain
	unaltered.
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
	<input type="button" id="submit" value=" Get isomers " onclick="callServer();"/>
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
