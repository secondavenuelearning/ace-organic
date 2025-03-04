<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.Molecule,
	com.epoch.AppConfig,
	com.epoch.mechanisms.MechRuleFunctions,
	com.epoch.mechanisms.Mechanism,
	com.epoch.qBank.Question,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until MarvinJS understands export parameters

	final String substrate = request.getParameter("substrate");

	Molecule substrateMol = null;
	MechRuleFunctions thisMechanism = null;
	int numAtoms = 0;
	if (substrate != null) {
		final Mechanism thisMech = new Mechanism(substrate);
		thisMechanism = new MechRuleFunctions(thisMech);
		substrateMol = MolImporter.importMol(substrate);
		numAtoms = substrateMol.getAtomCount();
	}

	final String initMech = "<?xml version=\"1.0\" ?>"
			+ "<MDocument>"
			+ "  <MRectangle>"
			+ "    <MPoint x=\"-12.774999618530273\" y=\"7.116666793823242\" />"
			+ "    <MPoint x=\"19.359078884124756\" y=\"7.116666793823242\" />"
			+ "    <MPoint x=\"19.359078884124756\" y=\"-16.482592582702637\" />"
			+ "    <MPoint x=\"-12.774999618530273\" y=\"-16.482592582702637\" />"
			+ "  </MRectangle>"
			+ "  <MRectangle>"
			+ "    <MPoint x=\"22.713298797607422\" y=\"-3.253796488046646\" />"
			+ "    <MPoint x=\"26.09663200378418\" y=\"-3.253796488046646\" />"
			+ "    <MPoint x=\"26.09663200378418\" y=\"-6.05379655957222\" />"
			+ "    <MPoint x=\"22.713298797607422\" y=\"-6.05379655957222\" />"
			+ "  </MRectangle>"
			+ "  <MPolyline headLength=\"0.8\" headWidth=\"0.5\">"
			+ "    <MPoint x=\"19.359078884124756\" y=\"-4.682962894439697\" />"
			+ "    <MPoint x=\"22.713298797607422\" y=\"-4.653796523809433\" />"
			+ "  </MPolyline>"
			+ "  <MChemicalStruct>"
			+ "    <molecule molID=\"m1\">"
			+ "      <atomArray"
			+ "          atomID=\"a1\""
			+ "          elementType=\"At\""
			+ "          x2=\"23.99663257598877\""
			+ "          y2=\"-4.537129852920771\""
			+ "          />"
			+ "      <bondArray>"
			+ "      </bondArray>"
			+ "    </molecule>"
			+ "  </MChemicalStruct>"
			+ "</MDocument>";
	final String APPLET_NAME = "Marvin";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title> Carbocation finder</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
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

	function getCations() {
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(molstruct) {
			document.tester.substrate.value = molstruct;
			document.tester.submit();
		}, function(error) {
			alert('Molecule export failed:' + error);	
		});
	}

	// -->
</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="lookForCarbocation.jsp" method="post">
<input type="hidden" name="substrate" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Carbocation finder
</P>
<p>
<table style="width:500px; margin-left:auto; margin-right:auto;">
<% if (numAtoms > 1) { %>
	<p>
	<tr><td class="regtext" >
		The compound you drew 
		<%  final int cationState = thisMechanism.noCarbocations2()[MechRuleFunctions.ERROR];
		if ((cationState & MechRuleFunctions.CARBOCAT) == 0) { %>
			does not contain a carbocation.
		<% } else if ((cationState & MechRuleFunctions.NEGATIVE_RESONANT) != 0) { %>
			contains a carbocation in resonance with a negative atom.
		<% } else if ((cationState & MechRuleFunctions.LONE_PAIR_RESONANT) != 0) { %>
			contains a carbocation in resonance with a neutral lone-pair-bearing atom.
		<% } else if ((cationState & MechRuleFunctions.NEG_ELSEWHERE) != 0) { %>
			contains a carbocation balanced by a negative charge elsewhere in the compound.
		<% } else { %>
			contains a carbocation.
		<% } %>
	<p>Try again, if you like!
<% } else { %>
	<tr><td class="regtext" >
		Please draw a compound,
		and press the button below 
		to see whether this compound contains a carbocation, and, if so,
		whether the charge is mitigated by resonance with a lone pair
		or by charge balance.  
<% } %>
</td></tr>
</table>
<br/>

<table border="1" style="width:500px; margin-left:auto; margin-right:auto;">
<tr>
<td style="text-align:center;" colspan="2">
	<div id="<%= APPLET_NAME %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('<%= Utils.toValidJS(substrate != null ? substrate : initMech) %>',
				MARVIN, 0, '<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td>
</tr>
<tr>
<td style="text-align:center;" colspan="2">
	<br/>
	<input type="button" value=" Find Carbocations " onclick="getCations()"/>
	<br/>
	<br/>
</td>
</tr>

</table>
</form>

</body>
</html>
