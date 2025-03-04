<%@ page language="java" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.chem.MolString,
	com.epoch.mechanisms.MechRuleFunctions,
	com.epoch.mechanisms.Mechanism,
	com.epoch.qBank.Question,
	com.epoch.utils.Utils,
	java.util.List,
	java.util.Arrays"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final String responseStr = request.getParameter("response");
	String errorString = null;

	MechRuleFunctions thisMechanism = null;
	if (!Utils.isEmptyOrWhitespace(responseStr)) {
		try {
			errorString = null;
			final Mechanism thisMech = new Mechanism(responseStr);
			thisMechanism = new MechRuleFunctions(thisMech);
		} catch (Exception e) {
			errorString = e.getMessage();
		}
	} 
	final String APPLET_NAME = "Marvin";
%>
<!DOCTYPE html>
<html lang="en">
<head>
<title>Pericyclic reaction finder</title>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<meta charset="UTF-8">
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= 
		AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<script type="text/javascript">
	// <!-- >

	<%@ include file="../js/marvinQuestionConstants.jsp.h" %>

	function loadSelections() { ; }

	function getPericyclic() {
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(mol) {
			document.tester.response.value = mol;
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
<form name="tester" action="isPericyclic.jsp" method="post">
<input type="hidden" name="response" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Pericyclic reaction finder
</P>
<p>
<table style="width:500px; margin-left:auto; margin-right:auto;">
<% if (!Utils.isEmptyOrWhitespace(responseStr) && errorString == null) { %>
	<p>
	<tr><td class="regtext" >
		The mechanism you drew 
		<% final List<int[]> periMechsInfo = thisMechanism.isPericyclic();
		final int size = periMechsInfo.size();
		if (size == 0) { %>
			does not contain a pericyclic reaction.
		<% } else for (int periNum = 0; periNum < size; periNum++) { 
			final StringBuilder feedback = new StringBuilder();
			if (periNum == 0) feedback.append(" contains a ");
			else if (periNum + 1 == size) feedback.append(" and a ");
			else feedback.append(" a ");
			final int[] mechInfo = periMechsInfo.get(periNum);
			final int rxnType = mechInfo[MechRuleFunctions.RXN_TYPE];
			final int numAtoms = mechInfo[MechRuleFunctions.ATOMS];
			final int numElecs = mechInfo[MechRuleFunctions.ELECS];
			final int compon1 = mechInfo[MechRuleFunctions.COMPON1];
			final int compon2 = mechInfo[MechRuleFunctions.COMPON2];
 			Utils.appendTo(feedback, 
					numAtoms, "-atom, ", numElecs, "-electron ");
			final boolean retro = Utils.among(rxnType,
					MechRuleFunctions.RETROCYCLOADDN,
					MechRuleFunctions.THREE_COMPON_RETROCYCLOADDN);
			int[] sorter;
			switch (rxnType) {
				case MechRuleFunctions.SIGMATROPIC:
					if (numAtoms != 0) {
						sorter = new int[2];
						sorter[0] = compon1;
						sorter[1] = compon2;
						Arrays.sort(sorter);
 						Utils.appendTo(feedback, 
								'[', sorter[0], ", ", sorter[1], "] ");
					} 
					feedback.append("sigmatropic rearrangement");
					break;
				case MechRuleFunctions.ELECTROCYCLIC_CLOSING:
					feedback.append("electrocyclic ring closing");
					break;
				case MechRuleFunctions.ELECTROCYCLIC_OPENING:
					feedback.append("electrocyclic ring opening");
					break;
				case MechRuleFunctions.RETROCYCLOADDN:
				case MechRuleFunctions.CYCLOADDN:
					if (numAtoms != 0) {
						sorter = new int[2];
						sorter[0] = compon1;
						sorter[1] = compon2;
						Arrays.sort(sorter);
 						Utils.appendTo(feedback, 
								'[', sorter[1], " + ", sorter[0], "] ");
					} 
					feedback.append(retro ? "retro" : "")
							.append("cycloaddition");
					break;
				case MechRuleFunctions.ENE:
					feedback.append("ene reaction");
					break;
				case MechRuleFunctions.RETROENE:
					feedback.append("retro-ene reaction");
					break;
				case MechRuleFunctions.GROUP_TRANSFER:
					feedback.append("group transfer reaction");
					break;
				case MechRuleFunctions.THREE_COMPON_RETROCYCLOADDN:
				case MechRuleFunctions.THREE_COMPON_CYCLOADDN:
					if (numAtoms != 0) {
						sorter = new int[3];
						sorter[0] = compon1;
						sorter[1] = compon2;
						sorter[2] = numAtoms - sorter[0] - sorter[1];
						Arrays.sort(sorter);
 						Utils.appendTo(feedback, '[', sorter[2], " + ", 
								sorter[1], " + ", sorter[0], "] ");
					} 
 					Utils.appendTo(feedback, "three-component ", retro 
							? "retro" : "", "cycloaddition");
					break;
				default: // UNKNOWN_TYPE
					feedback.append("pericyclic reaction of unknown type");
					break;
			} // switch
			if (periNum + 1 == size) feedback.append(".");
			else if (periNum != 0 || size != 2) feedback.append(",");
		%>
			<%= feedback.toString() %>
		<% } // for periNum %>
	<p>Try again, if you like!
<% } else { // there's no response or there's an error %>
	<tr><td class="regtext" >
		<% if (errorString != null) { %>
			<span style="color: red">Error:
			<%= errorString %>
			</span><p>
		<% } %>
			Please draw a mechanism with electron-flow arrows.  Place each step in a 
			rectangle, and connect the rectangles with graphical arrows.
			(See example below.)  Then press the button below to
			see whether this mechanism contains any pericyclic steps.
		<% } // if responseStr %>
</td></tr>
</table>
<br/>

<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr>
<td style="text-align:center;">
	<div id="<%= APPLET_NAME %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('<%= responseStr != null ? Utils.toValidJS(responseStr) : "" %>', 
				MECHANISM, SHOWLONEPAIRS, 
				'<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td>
</tr>
<tr>
<td style="text-align:center;">
	<br/>
	<input type="button" value=" Find Pericyclic Steps " onclick="getPericyclic()"/>
	<br/>
	<br/>
</td>

<tr>
<td class="regtext" >
<a href="welcome.html">Back</a> to public ACE pages.
</td>
</tr>

<tr>
<td class="regtext" >
<a id="example"></a>
<p>An example of a correctly drawn mechanism:
<p style="align:center"><%= MolString.getImage(pathToRoot, 
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
		+ "<cml xmlns=\"http://www.chemaxon.com\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.chemaxon.com/marvin/schema/mrvSchema_14_12_01.xsd\" version=\"ChemAxon file format v14.12.01, generated by v14.12.15.0\">"
		+ "<MDocument>"
		+ "<MEFlow id=\"o1\" arcAngle=\"-140.0\" headSkip=\"0.25\" headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.25\" baseElectronContainerIndex=\"-1\" baseElectronIndexInContainer=\"0\">"
		+ "<MAtomSetPoint atomRefs=\"m1.a5 m1.a6\"/>"
		+ "<MAtomSetPoint atomRefs=\"m1.a6 m1.a1\" weights=\"0.25 0.75\"/>"
		+ "</MEFlow>"
		+ "<MEFlow id=\"o2\" arcAngle=\"181.79999999999998\" headSkip=\"0.25\" headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.25\" baseElectronContainerIndex=\"-1\" baseElectronIndexInContainer=\"0\">"
		+ "<MAtomSetPoint atomRefs=\"m1.a1 m1.a2\"/>"
		+ "<MAtomSetPoint atomRefs=\"m1.a2 m1.a3\"/>"
		+ "</MEFlow>"
		+ "<MEFlow id=\"o3\" arcAngle=\"140.0\" headSkip=\"0.25\" headLength=\"0.5\" headWidth=\"0.4\" tailSkip=\"0.25\" baseElectronContainerIndex=\"-1\" baseElectronIndexInContainer=\"0\">"
		+ "<MAtomSetPoint atomRefs=\"m1.a3 m1.a4\"/>"
		+ "<MAtomSetPoint atomRefs=\"m1.a4 m1.a5\" weights=\"0.25 0.75\"/>"
		+ "</MEFlow>"
		+ "<MRectangle id=\"o4\">"
		+ "<MPoint x=\"-8.536000490188599\" y=\"3.871999979019165\"/>"
		+ "<MPoint x=\"-0.26400017738342285\" y=\"3.871999979019165\"/>"
		+ "<MPoint x=\"-0.26400017738342285\" y=\"-2.376000165939331\"/>"
		+ "<MPoint x=\"-8.536000490188599\" y=\"-2.376000165939331\"/>"
		+ "</MRectangle>"
		+ "<MRectangle id=\"o5\">"
		+ "<MPoint x=\"1.8040000200271606\" y=\"3.7839999198913574\"/>"
		+ "<MPoint x=\"8.447999954223633\" y=\"3.7839999198913574\"/>"
		+ "<MPoint x=\"8.447999954223633\" y=\"-2.3320000171661377\"/>"
		+ "<MPoint x=\"1.8040000200271606\" y=\"-2.3320000171661377\"/>"
		+ "</MRectangle>"
		+ "<MPolyline id=\"o6\" headLength=\"0.6\" headWidth=\"0.4\">"
		+ "<MRectanglePoint pos=\"5\" rectRef=\"o4\"/>"
		+ "<MRectanglePoint pos=\"7\" rectRef=\"o5\"/>"
		+ "</MPolyline>"
		+ "<MChemicalStruct>"
		+ "<molecule molID=\"m1\">"
		+ "<propertyList>"
		+ "<property dictRef=\"fromMarvinJS\" title=\"fromMarvinJS\">"
		+ "<scalar>true</scalar>"
		+ "</property>"
		+ "</propertyList>"
		+ "<atomArray atomID=\"a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16\" elementType=\"C C C C C C C C C C C C C C O O\" x2=\"-6.116000175476074 -7.449669516524333 -7.449669516524333 -6.116000175476074 -3.022330843964559 -3.022330843964559 -1.6886517221365231 4.062949986892567 2.7292806458443106 2.7292806458443106 4.062949986892567 5.396619327940828 5.396619327940828 6.730298449768862 -1.6886517221365231 6.730298449768862\" y2=\"1.5400338811180803 0.7700169405590397 -0.7700169405590415 -1.540033881118082 -0.7700169405590415 0.7700169405590397 1.5400169405590396 1.4520314603229625 0.6820145197639218 -0.8580193613541593 -1.6280363019131998 -0.8580193613541593 0.6820145197639218 1.4520145197639218 3.0800169405590396 2.992014519763922\"/>"
		+ "<bondArray>"
		+ "<bond id=\"b1\" atomRefs2=\"a1 a2\" order=\"2\"/>"
		+ "<bond id=\"b2\" atomRefs2=\"a2 a3\" order=\"1\"/>"
		+ "<bond id=\"b3\" atomRefs2=\"a3 a4\" order=\"2\"/>"
		+ "<bond id=\"b4\" atomRefs2=\"a5 a6\" order=\"2\"/>"
		+ "<bond id=\"b5\" atomRefs2=\"a6 a7\" order=\"1\"/>"
		+ "<bond id=\"b6\" atomRefs2=\"a8 a9\" order=\"1\"/>"
		+ "<bond id=\"b7\" atomRefs2=\"a9 a10\" order=\"2\"/>"
		+ "<bond id=\"b8\" atomRefs2=\"a10 a11\" order=\"1\"/>"
		+ "<bond id=\"b9\" atomRefs2=\"a12 a13\" order=\"1\"/>"
		+ "<bond id=\"b10\" atomRefs2=\"a13 a14\" order=\"1\"/>"
		+ "<bond id=\"b11\" atomRefs2=\"a8 a13\" order=\"1\"/>"
		+ "<bond id=\"b12\" atomRefs2=\"a12 a11\" order=\"1\"/>"
		+ "<bond id=\"b13\" atomRefs2=\"a7 a15\" order=\"2\"/>"
		+ "<bond id=\"b14\" atomRefs2=\"a14 a16\" order=\"2\"/>"
		+ "</bondArray>"
		+ "</molecule>"
		+ "</MChemicalStruct>"
		+ "</MDocument>"
		+ "</cml>"
) %>
</td>
</tr>

</table>

</form>
</body>
</html>
