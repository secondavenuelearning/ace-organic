<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolFormatException,
	chemaxon.formats.MolImporter,
	com.epoch.AppConfig,
	com.epoch.qBank.Question,
	com.epoch.synthesis.Synthesis,
	com.epoch.synthesis.SynthError,
	com.epoch.synthesis.SynthStage,
	com.epoch.translations.PhraseTransln,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	String moleculeStr = request.getParameter("molecule");
	String message = null;

	if (!Utils.isEmpty(moleculeStr)) {
		final Synthesis synth = new Synthesis();
		final SynthStage stage = new SynthStage();
		stage.setParentSynth(synth);
		try {
			stage.isBadSM(MolImporter.importMol(moleculeStr));
			message = "The compound may be used as a starting material in syntheses.";
		} catch (MolFormatException e) {
			message = "Could not import the molecule. " + e.getMessage();
		} catch (SynthError e) {
			message = e.getErrorFeedback();
			final String[] msgPieces = message.split(PhraseTransln.STARS_REGEX);
			// calcdProds is SMILESofOffendingCpd [tab] impossibleSMName
			final String[] errorpieces = e.calcdProds.split("\t");
			message = msgPieces[0] + errorpieces[1] + msgPieces[2];
		}
		// Utils.alwaysPrint("impossibleSM.jsp: message = ", message);
	} else moleculeStr = "";
	final String APPLET_NAME = "Marvin";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<title>Test Impossible Synthetic Starting Materials</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>

<!-- place values so updatePage() can find them
	follows ACE conventions: four-@-delimited strings.
	messageValue = @@@@<%= Utils.lineBreaksToJS(message) %>@@@@
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
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(mol) {
			// alert('calling server with ' + mol);
			var url = 'impossibleSM.jsp';
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
			var messageValue = extractField(response, 'messageValue');
			// alert('messageValue = ' + messageValue);
			// use innerHTML to change text & images upon receipt of results
			messageValue += '<p>Try again, if you like.<\/p>';
			setInnerHTML('english', messageValue);
			enableCell('submit');
			setValue('submit', ' Assess ');
		} // ready to continue
	} // updatePage

	// -->
</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="impossibleSM.jsp" method="post">
<input type="hidden" name="molecule" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Identify impossible synthetic starting materials
</p>
<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr><td id="english" class="regtext" colspan="2">
		Please draw a compound, and press the button to see whether it cannot 
		be used as a starting material in a synthesis.
</td></tr>
<tr><td>
	&nbsp;
</td></tr>

<tr><td style="text-align:center;">
	<div id="<%= APPLET_NAME %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('<%= moleculeStr != null ? Utils.toValidJS(moleculeStr) : "" %>', 
				MARVIN, 0, '<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td></tr>
<tr><td>&nbsp;</td></tr>
<tr><td style="text-align:center;">
	<br/>
	<input type="button" id="submit" value=" Assess " onclick="callServer();"/>
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
