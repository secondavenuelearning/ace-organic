<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.MolAtom,
	chemaxon.struc.Molecule,
	com.epoch.AppConfig,
	com.epoch.chem.ChemUtils,
	com.epoch.chem.MolString,
	com.epoch.chem.StereoFunctions,
	com.epoch.qBank.Question,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final String ROTATE = "0";
	final String SET = "1";
	final String SELECT = "0";
	final String TYPE = "1";
	final String SELECTED = "selected='selected'";

	String moleculeStr = request.getParameter("molecule");
	final String selectMethod = request.getParameter("selectMethod");
	String atomString = request.getParameter("atomString");
	String angleStr = request.getParameter("angle");
	final String setNotRotateStr = request.getParameter("setNotRotate");
	boolean doCalculation = true;
	/* Utils.alwaysPrint("rotateDihedral.jsp: atomString = ", atomString,
			", angleStr = ", angleStr, ", moleculeStr:\n", moleculeStr); /**/

	Molecule molecule = null;
	if (!Utils.isEmptyOrWhitespace(moleculeStr)) {
		molecule = MolImporter.importMol(moleculeStr);
	} else doCalculation = false;

	int[] atomIndices = new int[4];
	int numAtoms = 0;
	if (!Utils.isEmpty(atomString)) {
		String[] atomIndexStrs = atomString.split("-");
		if (atomIndexStrs.length == 1) atomIndexStrs = atomString.split(":");
		if (atomIndexStrs.length == 1) atomIndexStrs = atomString.split(",");
		numAtoms = atomIndexStrs.length;
		/* Utils.alwaysPrint("rotateDihedral.jsp: ",
		 		"doCalculation = ", doCalculation, 
				", numAtoms = ", numAtoms,
				", unordered atomIndexStrs = ", atomIndexStrs); /**/
		if (Utils.among(numAtoms, 4, 2) && doCalculation) {
			final int maxIndex = molecule.getAtomCount() - 1;
			try {
				for (int j = 0; j < atomIndexStrs.length; j++) {
					final int k = (numAtoms == 4 ? j : j + 1);
					atomIndices[k] = Integer.parseInt(atomIndexStrs[j]) - 1;
					if (atomIndices[k] > maxIndex)  // bad atom index
						doCalculation = false;
				} // for each atom of the dihedral
				if (SELECT.equals(selectMethod) && numAtoms == 4) {
					doCalculation = ChemUtils.arrangeAsBonded(molecule, atomIndices);
					 String out1 = "";
					for (int j = 0; j < 4; j++) {
						out1 = Utils.toString(out1, atomIndices[j] + 1, j < 3 ? "-" : "");
					} // for each atom index
					/* Utils.alwaysPrint("rotateDihedral.jsp: ordered atoms are ", out1); /**/
				} // if four atoms were chosen by selection
			} catch (Exception e) {
				doCalculation = false; // atomIndexStrs[j] not an integer
			}
		} else doCalculation = false;  // two or four atoms not entered
	} else doCalculation = false; // no atoms entered

	final int angle = MathUtils.parseInt(angleStr);

	boolean setNotRotate = false;
	if (setNotRotateStr != null) {
		setNotRotate = (numAtoms == 2 ? false : setNotRotateStr.equals(SET));
	} else doCalculation = false;

	if (doCalculation) {
		/* Utils.alwaysPrint("rotateDihedral.jsp: doing calculation; setNotRotate = ",
				setNotRotate); /**/
		if (setNotRotate)
			StereoFunctions.setDihedral(molecule, atomIndices, angle);
		else
			StereoFunctions.rotateDihedral(molecule, atomIndices[1], 
					atomIndices[2], angle);
		moleculeStr = MolString.toString(molecule, Utils.MRV);
		/* Utils.alwaysPrint("rotateDihedral.jsp: after calculation, moleculeStr:\n",
		 		moleculeStr); /**/
	}  else Utils.alwaysPrint("rotateDihedral.jsp: doCalculation is false."); /**/

	if (angleStr == null) angleStr = "";
	if (atomString == null) atomString = "";
	final String APPLET_NAME = "responseApplet";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<title>Rotation about bonds</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>

<!-- place values so updatePage() can find them
	follows ACE conventions: four-@-delimited strings.
	moleculeValue = @@@@<%= Utils.lineBreaksToJS(moleculeStr == null ? null
			: moleculeStr.replaceAll("\\\\n", "\\\\N")) %>@@@@
	setNotRotateValue = @@@@<%= setNotRotate ? SET : ROTATE %>@@@@
	selectMethodValue = @@@@<%= selectMethod %>@@@@
	atomStringValue = @@@@<%= atomString %>@@@@
-->

<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
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
		var atomString = '';
		var selectMethod = document.tester.selectMethod.value;
		var toSendBld = new String.builder();
		var url = 'rotateDihedral.jsp';
		var selectedAtomsStr;
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(mol) {
			marvinSketcherInstances['<%= APPLET_NAME %>'].getSelection().
					then(function(selection) { 
				if (selectMethod === '<%= SELECT %>') {
					selectedAtomsStr = selection.atoms;
					atomString = (isEmpty(selectedAtomsStr)
							? selection.bonds
							: selectedAtomsStr.replace(/,/g, '-'));
				} // if select method
				if (selectMethod !== '<%= SELECT %>') {
					atomString = document.tester.atomString.value;
				} // if select method
				// alert('Selected atoms: ' + atomString); 
				toSendBld.append('molecule=').append(encodeURIComponent(mol)).
						append('&atomString=').append(encodeURIComponent(atomString)).
						append('&selectMethod=').append(selectMethod).
						append('&angle=').append(document.tester.angle.value).
						append('&setNotRotate=').append(document.tester.setNotRotate.value);
				setValue('submit', ' Processing ... ');
				disableCell('submit');
				callAJAX(url, toSendBld.toString());
			}, function(error) {
				alert('Getting selected atoms failed:' + error);	
			});
		}, function(error) {
			alert('Molecule export failed:' + error);	
		});
	} // callServer()

	function updatePage() {
		if (xmlHttp.readyState === 4) { // ready to continue
			var response = xmlHttp.responseText;
			// the response is the entire web page
			var moleculeValue = extractField(response, 'moleculeValue').replace(/\\N/g, '\\n');
			// alert('moleculeValue = ' + moleculeValue);
			var setNotRotateValue = extractField(response, 'setNotRotateValue');
			if (!isEmpty(moleculeValue)) {
				if (document.responseApplet) {
					document.responseApplet.setMol(moleculeValue);
				} else {
					marvinSketcherInstances['<%= APPLET_NAME %>'].
							importStructure('<%= Utils.MRV %>', moleculeValue);
					var selectMethodValue = extractField(response, 'selectMethodValue');
					if (selectMethodValue === '<%= SELECT %>') {
						var atomStringValue = extractField(response, 'atomStringValue');
						var selectionsJSO = { 'atoms' : atomStringValue.replace(/-/g, ',') };
						marvinSketcherInstances['<%= APPLET_NAME %>'].
								setSelection(selectionsJSO);
					}
				} // if Java applet
			}
			document.tester.setNotRotate.selectedIndex = parseInt(setNotRotateValue);
			enableCell('submit');
			setValue('submit', ' Set Dihedral ');
		} // ready to continue
	} // updatePage()

	function changeMethod() {
		var meth = '';
	  	if (document.tester.selectMethod.value === '<%= SELECT %>') {
			setInnerHTML('atomStrInstr', 'Highlight one bond/two atoms or three bonds/four '
					+ 'atoms in the structure you have drawn.');
			setInnerHTML('atomString', '&nbsp;');
			meth = 'highlight only two atoms/one bond';
		} else {
			setInnerHTML('atomStrInstr', 
					'Enter the numbers of the two atoms defining the bond or '
					+ 'the four atoms defining the dihedral angle, separating '
					+ 'them with hyphens, colons, or commas.  The first one '
					+ 'or three atoms will be fixed; the last will move.');
			setInnerHTML('atomString', '<input type="text" size="25" '
					+ 'name="atomString" value="<%= atomString %>"/>');
			meth = 'enter only two atoms';
		}
		setInnerHTML('setRotateInstr', 'Enter a dihedral angle (negative '
				+ 'or positive integer and choose an option.  (If you ' 
				+ meth + ', only rotation is permitted.)'); 
	}

	// -->
</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="rotateDihedral.jsp" method="post">
<input type="hidden" name="molecule" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Rotation about bonds
</p>
<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr><td class="regtext"  colspan="2">
	<% if (doCalculation) { %>
		Here's the product of 
			<%= setNotRotate ? 
				"setting the " + atomString + " dihedral angle to " + angleStr
				: "rotating the " + atomString + " dihedral angle by " + angleStr
			%> 
		degrees.  Try again, if you like.
	<% } else { %>
		Draw a structure, do a 3D minimization (northern toolbar, 3D button
		with perpendicular arrows), and designate a bond and a new dihedral angle
		for it or an angle by which to rotate about the bond.  
	<% } %>
</td></tr>
<tr><td>
	<table class="regtext" >
	<tr><td>
		<br/>Choose a technique:	
	</td><td>
		<select name="selectMethod" id="selectMethod" onchange="changeMethod();">
			<option value="<%= SELECT %>" <%= setNotRotate ? "" : SELECTED %> > 
				highlight bond(s) in structure</option>
			<option value="<%= TYPE %>" <%= setNotRotate ? SELECTED : "" %> > 
				type atom numbers</option>
		</select>
	</td></tr>
	</table>
</td></tr>

<tr><td>
	&nbsp;
</td></tr>
<tr><td style="text-align:center;">
	<div id="<%= APPLET_NAME %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('<%= moleculeStr != null ? Utils.toValidJS(moleculeStr) : "" %>', 
				MARVIN, THREEDIM, '<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td>
</tr>
<tr><td>&nbsp;</td></tr>
<tr><td style="text-align:center;">
	<table class="regtext" >
	<tr><td id="atomStrInstr" style="width:60%">
		Highlight one bond/two atoms or three bonds/four atoms 
		in the structure you have drawn. 
	</td><td id="atomString">
		&nbsp;
	</td></tr>
	<tr><td>&nbsp;</td></tr>
	<tr><td id="setRotateInstr">
		Enter a dihedral angle (negative or positive integer) and choose an option.
		If you select only two atoms/one bond, only rotation is permitted. 
	</td><td>
		<select name="setNotRotate" id="setNotRotate">
			<option value="<%= ROTATE %>" <%= setNotRotate ? "" : SELECTED %> > rotate by
			</option>
			<option value="<%= SET %>" <%= setNotRotate ? SELECTED : "" %> > set to
			</option>
		</select>
		<input type="text" size="5" name="angle" value="<%= angleStr %>"/> 
		degrees.
	</td></tr>
	</table>
</td></tr>
<tr><td style="text-align:center;">
	<br/>
	<input type="button" name="submit" id="submit" value=" Set Dihedral "
			onclick="callServer()"/>
	<br/>
</td></tr>

<tr>
<td align="left" class="regtext" >
<a href="welcome.html">Back</a> to public ACE pages.
</td>
</tr>

</table>
</form>

</body>
</html>
