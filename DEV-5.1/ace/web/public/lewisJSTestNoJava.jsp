<?xml version="1.0" encoding="UTF-8"?>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
	<title>LewisJS</title>
	<link rel="stylesheet" href="../includes/epoch.css" type="text/css" />
	<link rel="icon" href="../images/favicon.ico" type="image/x-icon"/>
	<script src="../js/ajax.js" type="text/javascript"></script>
	<script src="../js/jslib.js" type="text/javascript"></script>
	<script src="../js/lewisJS.js" type="text/javascript"></script>
	<script src="../js/lewisEquals.js" type="text/javascript"></script>
	<script src="../js/offsets.js" type="text/javascript"></script>
	<script src="../js/openwindows.js" type="text/javascript"></script>
	<script src="../js/position.js" type="text/javascript"></script>
	<script src="../js/svgGraphics.js" type="text/javascript"></script>
	<script src="../js/wz_jsgraphics.js" type="text/javascript"></script>
	<script src="../js/xmlLib.js" type="text/javascript"></script>
	<script type="text/javascript">
		// <!-- >
		var mol = '';

		var CANVAS_WIDTH = 450; 
		var CANVAS_HEIGHT = 260;
		var MARVIN_WIDTH = 25;
		var PAIRED_ELECS = 'paired electrons';
		var UNPAIRED_ELECS = 'unpaired electrons';
		var UNSHARED_ELECS = 'unshared electrons';
		var LEWIS_PROPERTY = 'is Lewis Structure?';
		var HIGHLIGHT = 'highlighted';

		function init() {
			setHeight('lewisJSCanvas', '' + CANVAS_HEIGHT + 'px');
			setWidth('LewisSketch_canvas', '' + CANVAS_WIDTH + 'px');
			initLewisConstants(
					[CANVAS_WIDTH, CANVAS_HEIGHT],
					MARVIN_WIDTH, 
					[PAIRED_ELECS, UNPAIRED_ELECS, UNSHARED_ELECS],
					LEWIS_PROPERTY,
					HIGHLIGHT,
					'Enter an element symbol.',
					'There is no such element.  Please try again.',
					'Other');
			initLewisGraphics('../', 
					'lewisJSCanvas', 
					'lewisJSToolbars');
			parseLewisMRV(mol);
		} // init()

		function match() {
			var matcher = new LewisMatcher();
			matcher.setCompMols(document.mol1Form.mol1.value, lewisMol);
			matcher.setVerbosity(parseInt(document.mol1Form.verbosity.value));
			var match = matcher.match();
			alert('The structures you drew ' 
					+ (match ? 'match' : 'do not match') + '.');
		} // match()

		function getConfigurations() {
			var matcher = new LewisMatcher();
			matcher.setCompMol(lewisMol, 1);
			matcher.setVerbosity(parseInt(document.mol1Form.verbosity.value));
			matcher.displayConfigurations();
		} // getConfigurations()

		function viewImageMRV() {
			alert(getLewisImageMRV());
		} // viewMRV()

		// -->
	</script>
</head>
<body class="regtext" style="overflow:auto;" onload="init();">
<p class="boldtext big" style="text-align:center;">
LewisJS
</p>

<table summary="" style="width:500px; margin-left:auto; margin-right:auto;">
<tr><td>
This page demonstrates LewisJS, our JavaScript-only application 
for drawing and analyzing Lewis structures.

<p>LewisJS can determine the configurations of trigonal (E/Z) 
and tetrahedral (R/S) stereocenters.  Simply draw a structure and press the 
<b>Get configurations</b> button.  (Because this application is intended for Lewis 
structures, it does not consider implicit H atoms when determining whether an atom
is stereogenic, although one can easily imagine modifying the code to make it do so.)
</p>
<p>LewisJS can also determine whether two Lewis structures are 
identical.  Draw a structure, press the clipboard button, copy the source code, and 
paste it into the box below the drawing canvas.  Then draw another structure, and see 
if it matches to the first one.  (Currently, for two structures to match, stereocenter 
configurations and unshared electron configurations must match as well.  Also, 
again, the application does not consider implicit H atoms.)
</p>
<p>If you would like to use this code with or without modification, or if you find a bug, 
please contact <a href="mailto:robert.grossman@uky.edu">Robert Grossman</a>.
</p>
</td></tr>
</table>

<br/>
<table id="LewisSketch_canvas" summary="LewisSketch_canvas" class="rowsTable" 
		style="margin-left:auto; margin-right:auto;">
<tr><td style="width:100%;">
	<div id="lewisJSToolbars"></div>
</td></tr>
<tr><td style="width:100%;">
	<div id="lewisJSCanvas" style="position:relative; width:100%;"></div>
</td></tr>
<tr><td id="lewisJSOptions" style="width:100%;">
</td></tr>
<tr><td style="text-align:center;">
	<br/>
	<!-- <input type="button" value=" View image MRV " onclick="viewImageMRV()"/> -->
	<input type="button" value=" Get configurations " onclick="getConfigurations()"/>
	<input type="button" value=" Match to structure below " onclick="match()"/>
	<br/>
	<br/>
</td></tr>
<!--
<tr><td id="lewisJSMsgs" style="height:0px; text-align:center;">
</td></tr>
-->
</table>

<br/>
<br/>

<form name="mol1Form" action="">
<table summary="log_options" style="width:500px; margin-left:auto; margin-right:auto;">
<tr><td>
<select name="verbosity"> 
<option value="0">Don't provide a log</option>
<option value="1">Provide the log in a single alert</option>
<option value="2">Provide the log in frequent alerts</option>
</select>
<br/>
<br/>
</td></tr>
<tr><td>
Paste the source code of your comparison Lewis structure here:
</td></tr>
<tr><td>
<textarea name="mol1" style="font-family:Courier;" cols="80" rows="35">
</textarea>
</td></tr>
</table>
</form>

<table summary="back" style="width:500px; margin-left:auto; margin-right:auto;">
<tr>
<td align="left" class="regtext" >
<a href="welcome.html">Back</a> to public ACE pages.
</td>
</tr>
</table>

</body>
</html>
