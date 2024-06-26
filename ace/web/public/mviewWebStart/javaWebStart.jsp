<?xml version="1.0" encoding="utf-8"?>
<%@ page language="java" %>
<%
	final String pathToRoot = "../../";
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<meta NAME="author" CONTENT="Gabor Bartha"/>
<title>MarvinView via Internet</title>
<script src="https://www.java.com/js/deployJava.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >

	function launchMarvin(which) {
		var code = document.getElementById('argument').value;
		startMViewWebStart(code, '<%= pathToRoot %>', 'tempUser', which);
	} // launchMarvin()

/* Uses Java Web Start to launch MarvinView (default) or MarvinSketch. 
The first method calls a JSP page that writes a file containing the
structure's MRV to disk.
*/
function startMViewWebStart(mol, pathToRoot, userName, applet) {
	"use strict";
	var toSend = new String.builder().
			append('pathToRoot=').append(pathToRoot).
			append('&userName=').append(userName == null ?
				'tempUser' : encodeURIComponent(userName)).
			append('&applet=').append(applet == null ? 'view' : applet).
			append('&mol=').append(encodeURIComponent(mol)).toString();
	// alert(toSend);
	callAJAX('writeMolFile.jsp', toSend, finishMViewWebStart);
} // startMViewWebStart()

/* Deliver the JNLP page required to launch MarvinView. 
In the case of Chrome, the file will be downloaded to the user's drive, 
so the JNLP file defines the codebase with an absolute Web
URL instead of just a relative URL as it does for other browsers. */
function finishMViewWebStart() {
	"use strict";
	if (xmlHttp.readyState === 4) { // ready to continue
		var responsePage = xmlHttp.responseText;
		var pathToRoot = extractField(responsePage, 'pathToRootValue');
		var molFileUrl = extractField(responsePage, 'molFileUrlValue');
		var userAgent = extractField(responsePage, 'userAgentValue');
		var applet = extractField(responsePage, 'appletValue');
		var isChrome = userAgent.indexOf('Chrome') >= 0;
		var isFirefox = userAgent.indexOf('Firefox') >= 0;
		var param = new String.builder().
				append('mview-jnlp.jsp?applet=').append(applet).
				append('&isChrome=').append(isChrome).
				append('&argument=').append(molFileUrl).
				toString();
		// alert(param);
		if (isFirefox) {
			alert('Sorry, this feature does not work in the Firefox browser. ' +
					'Try a different browser, such as Safari or Chrome.');
		} else if (isChrome) {
			alert('It appears that you are using the Chrome browser. If so, ' +
					'the browser will download a file with the extension jnlp. ' +
					'In the downloads box that appears in the bottom of the ' +
					'browser, choose to keep the file, then find it on your drive. ' +
					'Right- or control-click (don\'t double-click) on the file, ' +
					'and choose Open with -> Java Web Start.');
			self.location.href = param; // downloads the file
		} else {
			deployJava.launchWebStartApplication(param);
		}
	} // if ready to continue
} // finishMViewWebStart()

	// -->
</script>
</head>
<body>

<h1 align=center>MarvinView or MarvinSketch via Internet</h1>

Launch MarvinView or MarvinSketch via the Internet using the Java Web Start technology. 

<p>Enter your molecule's code:
<br/><textarea id="argument" name="argument" rows="20" cols="100"><?xml version="1.0" ?>
&lt;cml&gt;
&lt;MDocument&gt;
&lt;MChemicalStruct&gt;
&lt;molecule molID="m1"&gt;
&lt;atomArray
atomID="a1 a2 a3 a4 a5 a6 a7 a8"
elementType="C C C N C C O C"
x3="-0.9605793291129503 -2.153371876432826 -2.514126840989448 -1.474312575600967 -0.28196546845623804 0.296619122761033 0.20425260091903025 -1.451217758123855"
y3="1.234344843627626 0.3755871982672688 -0.45266238608531145 0.10348965938596222 -0.3823086523898616 0.34088949101820115 -1.2803930202445126 1.4897323970356278"
z3="-0.5764035891388528 -1.138503847393507 0.1341646282453004 0.8868380942208632 0.8336789434568816 -0.2740692997095784 1.5035273249262244 0.8631361155901126"
/&gt;
&lt;bondArray&gt;
&lt;bond atomRefs2="a1 a2" order="1" /&gt;
&lt;bond atomRefs2="a2 a3" order="1" /&gt;
&lt;bond atomRefs2="a3 a4" order="1" /&gt;
&lt;bond atomRefs2="a4 a5" order="1" /&gt;
&lt;bond atomRefs2="a5 a6" order="1" /&gt;
&lt;bond atomRefs2="a1 a6" order="1" /&gt;
&lt;bond atomRefs2="a5 a7" order="2" /&gt;
&lt;bond atomRefs2="a1 a8" order="1" /&gt;
&lt;bond atomRefs2="a4 a8" order="1" /&gt;
&lt;/bondArray&gt;
&lt;/molecule&gt;
&lt;/MChemicalStruct&gt;
&lt;/MDocument&gt;
&lt;/cml&gt;</textarea>
<br/><input type="button" value="Launch MarvinView"
	onclick="launchMarvin('view');"/>
<input type="button" value="Launch MarvinSketch"
	onclick="launchMarvin('sketch');"/>
</p>

<p>If you are using Chrome, the browser will download a file with
the extension jnlp. In the downloads box that appears in the bottom
of the browser, choose to keep the file, then find it on your
drive. Right- or control-click on the file, and choose 
Open with &rarr; Java Web Start.
</body>
</html>
