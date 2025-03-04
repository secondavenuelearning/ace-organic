<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.Molecule,
	com.epoch.AppConfig,
	com.epoch.chem.MolString,
	com.epoch.qBank.Question,
	com.epoch.servlet.Base64Coder,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final String structure = request.getParameter("structure");
	String imageData = "";
	String imageDataDisplay = "";

	// Utils.alwaysPrint("getDataURL.jsp: structure:\n", structure);
	if (structure != null) {
		try {
			final int scale = MathUtils.parseInt(request.getParameter("scale"));
	 		// Utils.alwaysPrint("getDataURL.jsp: scale = ", scale, ", structure:\n", structure);
			final Molecule mol = MolImporter.importMol(structure);
			String opts = Utils.toString(Utils.SVG, ":H_heteroterm");
			if (scale != 0) opts = Utils.toString(opts, ",scale", scale);
	 		// Utils.alwaysPrint("getDataURL.jsp: opts = ", opts);
			imageData = Utils.toString("data:image/", Utils.SVG, ";base64,", 
					Base64Coder.encodeAsString(
						MolString.toBinFormat(mol, opts))); /**/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	final String APPLET_NAME = "mechapplet";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Image URL generator</title>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>

<!-- place values so updatePage() can find them
	follows ACE conventions: four-@-delimited strings.
	imageDataValue = @@@@<%= Utils.lineBreaksToJS(imageData) %>@@@@
	imageDataDisplayValue = @@@@<%= Utils.lineBreaksToJS(imageDataDisplay) %>@@@@
-->

<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
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
			var url = 'getDataURL.jsp';
			setValue('submit', ' Processing ... ');
			disableCell('submit');
			var toSend = 'structure=' + encodeURIComponent(mol)
					+ '&scale=' + getValue('scale');
			callAJAX(url, toSend);
		}, function(error) {
			alert('Molecule export failed:' + error);	
		});
	} // callServer

	function updatePage() {
		if (xmlHttp.readyState === 4) { // ready to continue
			var response = xmlHttp.responseText;
			var imageDataValue = extractField(response, 'imageDataValue');
			var imageDataDisplayValue = extractField(response, 'imageDataDisplayValue');
			setInnerHTML('imageCell', imageDataValue);
			setInnerHTML('imageDataDisplay', imageDataDisplayValue);
			enableCell('submit');
			setValue('submit', ' View Data URL ');
		} // ready to continue
	} // updatePage

	// -->
</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" id="tester" action="getDataURL.jsp" method="post">
<input type="hidden" name="structure" value=""/>
<input type="hidden" name="scale" value=""/>
<table style="table-layout:fixed; width:650px; margin-left:auto; margin-right:auto;">
<tr><td>
<p class="boldtext big" style="text-align:center;">
ACE SVG calculator
<br/>&nbsp;
</td></tr>
<tr><td class="regtext" id="intro">
	Draw a structure and convert to an image.
	<br/>&nbsp;
</td></tr>
<tr>
<td id="imageCell" style="text-align:center;">
</td>
</tr>
<tr>
<td style="text-align:center;">
	<div id="<%= APPLET_NAME %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('', 
				MARVIN, THREEDIM | SHOWLONEPAIRS, 
				'<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td>
</tr>
<tr>
<td style="text-align:center;">
	Enter a scale factor (28 is standard size):
	<input type="text" id="scale" size="5" value="28"/>
</td>
</tr>
<tr>
<td style="text-align:center;">
	<br/>
	<input type="button" id="submit" value=" View image data " onclick="callServer();"/>
</td>
</tr>

<tr>
<td align="left" class="regtext" >
<a href="welcome.html">Back</a> to public ACE pages.
</td>
</tr>

<tr>
<td style="padding-top:18px; text-align:left;" id="imageDataDisplay">
</td>
</tr>

</table>
</form>
</body>
</html>


