<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	com.epoch.AppConfig,
	com.epoch.chem.MolString,
	com.epoch.mechanisms.Mechanism,
	com.epoch.mechanisms.MechError,
	com.epoch.qBank.Question,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final String reactants = request.getParameter("reactants");
	// Utils.alwaysPrint("flowsValidTest.jsp: reactants = ", reactants);
	String products = null;
	final String startMech = ""
			+ "<?xml version=\"1.0\" ?>\n"
			+ "<cml>\n"
			+ "<MDocument>\n"
			+ "<MRectangle id=\"o1\">\n"
			+ "<MPoint x=\"3.8259374797344208\" y=\"7.844374895095825\" />\n"
			+ "<MPoint x=\"12.055312424898148\" y=\"7.844374895095825\" />\n"
			+ "<MPoint x=\"12.055312424898148\" y=\"1.7806248664855957\" />\n"
			+ "<MPoint x=\"3.8259374797344208\" y=\"1.7806248664855957\" />\n"
			+ "</MRectangle>\n"
			+ "<MPolyline id=\"o2\" headLength=\"0.8\" headWidth=\"0.5\">\n"
			+ "<MPoint x=\"0.3849995732307434\" y=\"4.860624969005585\" />\n"
			+ "<MPoint x=\"3.8259374797344208\" y=\"4.8124998807907104\" />\n"
			+ "</MPolyline>\n"
			+ "<MEFlow id=\"o3\" arcAngle=\"248.39738999999997\" headSkip=\"0.25\"\n"
			+ "headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.15\">\n"
			+ "<MAtomSetPoint atomRefs=\"m1.a2 m1.a4\" />\n"
			+ "<MAtomSetPoint atomRefs=\"m1.a4\" />\n"
			+ "</MEFlow>\n"
			+ "<MEFlow id=\"o4\" arcAngle=\"-254.995522631729\" headSkip=\"0.15\"\n"
			+ "headFlags=\"2\" headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.25\">\n"
			+ "<MEFlowBasePoint atomRef=\"m1.a3\" />\n"
			+ "<MAtomSetPoint atomRefs=\"m1.a2 m1.a3\" />\n"
			+ "</MEFlow>\n"
			+ "<MRectangle id=\"o5\">\n"
			+ "<MPoint x=\"-7.868437558412552\" y=\"7.844374895095825\" />\n"
			+ "<MPoint x=\"0.3609373867511749\" y=\"7.844374895095825\" />\n"
			+ "<MPoint x=\"0.3609373867511749\" y=\"1.7806248664855957\" />\n"
			+ "<MPoint x=\"-7.868437558412552\" y=\"1.7806248664855957\" />\n"
			+ "</MRectangle>\n"
			+ "<MChemicalStruct>\n"
			+ "<molecule molID=\"m1\">\n"
			+ "<atomArray\n"
			+ "atomID=\"a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19\"\n"
			+ "elementType=\"C C C O H H C C C O H H C O C H C O C\"\n"
			+ "formalCharge=\"0 0 -1 0 0 0 0 0 0 -1 0 0 0 1 0 0 0 0 0\"\n"
			+ "x2=\"-5.10124945640564 -4.012305013378357 -2.9233605703510737 -4.012305013378357 -1.4358347978659083 -2.9233605703510737 6.433278180639964 7.522222623667247 8.61116706669453 7.522222623667247 10.098692839179694 8.61116706669453 -5.47087734774052 -4.137198225912484 -2.8035191040844483 -4.137198225912484 6.079122842994344 7.566648615479509 9.054174387964675\"\n"
			+ "y2=\"4.3793748915195465 5.4683193345468295 4.3793748915195465 7.0083193345468295 4.344831201903942 2.8393748915195465 4.2627756861525885 5.3517201291798715 4.2627756861525885 6.8917201291798715 4.228231996536984 2.7227756861525885 -3.8208254498036966 -3.0508254498036966 -3.8208254498036966 -1.5108254498036964 -3.9170754772218332 -3.518494147763951 -3.9170754772218332\"\n"
			+ "/>\n"
			+ "<bondArray>\n"
			+ "<bond atomRefs2=\"a1 a2\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a2 a3\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a2 a4\" order=\"2\" />\n"
			+ "<bond atomRefs2=\"a3 a5\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a3 a6\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a7 a8\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a8 a9\" order=\"2\" />\n"
			+ "<bond atomRefs2=\"a8 a10\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a9 a11\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a9 a12\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a13 a14\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a14 a15\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a14 a16\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a17 a18\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a18 a19\" order=\"1\" />\n"
			+ "</bondArray>\n"
			+ "</molecule>\n"
			+ "</MChemicalStruct>\n"
			+ "<MEFlow id=\"o7\" arcAngle=\"248.39738999999997\" headSkip=\"0.25\"\n"
			+ "headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.15\">\n"
			+ "<MAtomSetPoint atomRefs=\"m1.a14 m1.a16\" />\n"
			+ "<MAtomSetPoint atomRefs=\"m1.a16\" />\n"
			+ "</MEFlow>\n"
			+ "<MRectangle id=\"o8\">\n"
			+ "<MPoint x=\"-7.756814686807978\" y=\"0.366049565693511\" />\n"
			+ "<MPoint x=\"0.4725602583557482\" y=\"0.366049565693511\" />\n"
			+ "<MPoint x=\"0.4725602583557482\" y=\"-5.6977004629167185\" />\n"
			+ "<MPoint x=\"-7.756814686807978\" y=\"-5.6977004629167185\" />\n"
			+ "</MRectangle>\n"
			+ "<MPolyline id=\"o9\" headLength=\"0.8\" headWidth=\"0.5\">\n"
			+ "<MPoint x=\"0.4966224448353167\" y=\"-2.6177003603967295\" />\n"
			+ "<MRectanglePoint pos=\"7\" rectRef=\"o10\" />\n"
			+ "</MPolyline>\n"
			+ "<MRectangle id=\"o10\">\n"
			+ "<MPoint x=\"3.937560351338994\" y=\"0.366049565693511\" />\n"
			+ "<MPoint x=\"12.166935296502722\" y=\"0.366049565693511\" />\n"
			+ "<MPoint x=\"12.166935296502722\" y=\"-5.6977004629167185\" />\n"
			+ "<MPoint x=\"3.937560351338994\" y=\"-5.6977004629167185\" />\n"
			+ "</MRectangle>\n"
			+ "</MDocument>\n"
			+ "</cml>\n";
	String feedback = "";
	final String permissibleSMs = ""
			+ "<?xml version=\"1.0\" ?>\n"
			+ "<cml>\n"
			+ "<MDocument>\n"
			+ "<MChemicalStruct>\n"
			+ "<molecule molID=\"m1\">\n"
			+ "<atomArray\n"
			+ "atomID=\"a1 a2 a3 a4 a5 a6 a7 a8 a9 a10\"\n"
			+ "elementType=\"C C C O H H C O C H\"\n"
			+ "formalCharge=\"0 0 -1 0 0 0 0 1 0 0\"\n"
			+ "x2=\"-7.122499465942383 -6.0335550229151 -4.944610579887817 -6.0335550229151 -3.4570848074026514 -4.944610579887817 -0.048124998807907104 1.2855541230201284 2.619233244848164 1.2855541230201284\"\n"
			+ "y2=\"0.288750022649765 1.3776944656770482 0.288750022649765 2.917694465677048 0.2542063330341606 -1.251249977350235 0.721875011920929 1.491875011920929 0.7218750119209291 3.031875011920929\"\n"
			+ "/>\n"
			+ "<bondArray>\n"
			+ "<bond atomRefs2=\"a1 a2\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a2 a3\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a2 a4\" order=\"2\" />\n"
			+ "<bond atomRefs2=\"a3 a5\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a3 a6\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a7 a8\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a8 a9\" order=\"1\" />\n"
			+ "<bond atomRefs2=\"a8 a10\" order=\"1\" />\n"
			+ "</bondArray>\n"
			+ "</molecule>\n"
			+ "</MChemicalStruct>\n"
			+ "</MDocument>\n"
			+ "</cml>\n";
	if (reactants != null) {
		Utils.alwaysPrint("flowsValidTest.jsp: reactants:\n", reactants);
		try {
			final Mechanism mechanism = new Mechanism(reactants);
		 	mechanism.checkFlowsValid(MolImporter.importMol(permissibleSMs), 0);
			Utils.alwaysPrint("flowsValidTest.jsp: no MechError thrown.");
		} catch (MechError e) {
			feedback = e.getErrorFeedback();
			System.out.println(feedback);
			products = MolString.convertMol(e.calcdProds, MRV_EXPORT);
			System.out.println(products);
		} catch (Exception e) {
			e.printStackTrace();
			feedback = e.getMessage();
			products = reactants;
		}
	}
	final String APPLET_NAME = "mechapplet";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Mechanism calculator</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="../includes/epoch.css" type="text/css"/>

<!-- place values so updatePage() can find them
	follows ACE conventions: four-@-delimited strings.
	productsValue = @@@@<%= Utils.lineBreaksToJS(products == null ? null
			: products.replaceAll("\\\\n", "\\\\N")) %>@@@@
	feedbackValue = @@@@<%= Utils.lineBreaksToJS(feedback) %>@@@@
-->

<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>nosession/marvinJS/js/promise.min.js"></script>
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
			var url = 'flowsValidTest.jsp';
			var toSend = 'reactants=' + encodeURIComponent(mol);
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
			var productsValue = extractField(response, 'productsValue').replace(/\\N/g, '\\n');
			 alert('products = ' + productsValue);
			var feedbackValue = extractField(response, 'feedbackValue');
			// use innerHTML to change text & images upon receipt of results
			if (!isEmpty(feedbackValue)) {
				setInnerHTML('intro', 'The mechanism calculation threw the '
						+ 'following error:<br/>' + feedbackValue);
			} else {
				setInnerHTML('intro', 'No error thrown. ');
			}				
			if (document.mechapplet) {
				document.mechapplet.setMol(productsValue);
			} else {
				marvinSketcherInstances['<%= APPLET_NAME %>'].
						importStructure('<%= Utils.MRV %>', productsValue);
			} // if Java applet
			enableCell('submit');
			setValue('submit', ' View Products ');
		} // ready to continue
	} // updatePage

	// -->
</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="flowsValidTest.jsp" method="post">
<input type="hidden" name="reactants" value=""/>
<table style="width:650px; margin-left:auto; margin-right:auto;">
<tr><td colspan="2">
<p class="boldtext big" style="text-align:center;">
ACE mechanism calculator
<br/>&nbsp;
</td></tr>
<tr><td class="regtext" id="intro"  colspan="2">
	Delete one of the mechanisms, and press <b>View Products</b> to see if there are
	any errors in the remaining one.
</td></tr>
<tr><td>
	&nbsp;
</td></tr>
<tr>
<td style="text-align:center;" colspan="2">
	<div id="<%= APPLET_NAME %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('<%= Utils.toValidJS(products != null ? products : startMech) %>', 
				MECHANISM, SHOWLONEPAIRS, 
				'<%= APPLET_NAME %>', '<%= pathToRoot %>');
		// -->
	</script>
	</div>
</td>
</tr>
<tr>
<td style="text-align:center;" colspan="2">
	<br/>
	<input type="button" id="submit" value=" View Products " onclick="callServer()"/>
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


