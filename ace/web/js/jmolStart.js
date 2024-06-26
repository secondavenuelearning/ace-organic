// <!-- avoid parsing the following as HTML
/*  Javascript to display Jmol, http://jmol.sourceforge.net, in a jsp page.
*  Host page must include these resources in the given order:
		<script src="<%= pathToRoot %>nosession/jsmol/JSmol.min.js" type="text/javascript"></script>
		<script src="<%= pathToRoot %>nosession/jsmol/Jmol2.js" type="text/javascript"></script>
*  Host page must call jmolInitialize() from the head (not as part of body onload).
*  Host page should call setJmol() from the location where the applet should be  placed.
*  Additional calls to Jmol JS methods can be placed after the call to setJmol().
*/

/*jsl:option explicit*/

var jmolStart = { // file-local globals
	jmolMols: [],
	addlScripts: []
};

function setJmol(jmolNum, jmolMol, bgcolor, width, height, addlScript) {
	"use strict";
	var bgColor = bgcolor.replace('#', 'x'), // if color has format
		jmolStartScript;
	jmolStart.jmolMols[jmolNum] = jmolMol;
	jmolStart.addlScripts[jmolNum] = addlScript;
	// for use in HTML style attribute
	if (!isNaN(parseInt(bgColor, 16))) {
		bgColor = 'x' + bgColor; // if color is hexadecimal without x prefix
	}
	if (bgColor.charAt(0) == 'x') {
		bgColor = '[' + bgColor + ']'; // if color is not English
	}
	jmolStartScript = 'background ' + bgColor +
		'; javascript jmolLoaded(' + jmolNum + '); ';
	jmolApplet([width, height], jmolStartScript, jmolNum);
} // setJmol()

// this method will be called after the applet initialization is complete
function jmolLoaded(jmolNum) {
	"use strict";
	jmolLoadInline(jmolStart.jmolMols[jmolNum], jmolNum);
	jmolScript(jmolStart.addlScripts[jmolNum], jmolNum);
} // jmolLoaded()

// --> end HTML comment
