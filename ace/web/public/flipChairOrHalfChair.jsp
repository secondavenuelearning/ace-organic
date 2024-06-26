<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.sss.SearchConstants,
	chemaxon.sss.search.MolSearch,
	chemaxon.sss.search.MolSearchOptions,
	chemaxon.struc.CTransform3D,
	chemaxon.struc.DPoint3,
	chemaxon.struc.MolAtom,
	chemaxon.struc.MolBond,
	chemaxon.struc.Molecule,
	chemaxon.struc.SelectionMolecule,
	com.epoch.AppConfig,
	com.epoch.chem.MolString,
	com.epoch.chem.VectorMath,
	com.epoch.qBank.Question,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	String moleculeStr = request.getParameter("molecule");

	if (!Utils.isEmptyOrWhitespace(moleculeStr)) {
		Utils.alwaysPrint("Before flipping, molecule is:\n", moleculeStr);
		final Molecule molecule = MolImporter.importMol(moleculeStr);
		final MolSearchOptions ourOptions = 
				new MolSearchOptions(SearchConstants.SUBSTRUCTURE);
		ourOptions.setExactBondMatching(true);
		final MolSearch ourSearch = new MolSearch();
		ourSearch.setSearchOptions(ourOptions);
		Molecule sixMembRing = MolImporter.importMol("*1*****1");
		ourSearch.setTarget(molecule);
		ourSearch.setQuery(sixMembRing);
		int[] ringIndices = ourSearch.findFirst(); 
		boolean chair = true;
		if (ringIndices == null) {
			chair = false;
			sixMembRing = MolImporter.importMol("*1=*****1");
			ourSearch.setQuery(sixMembRing);
			ringIndices = ourSearch.findFirst(); 
		}

		if (ringIndices != null) { 
			// a six-membered ring was found
			if (chair) Utils.alwaysPrint(
					"Found a six-membered ring with all single bonds.");
			else Utils.alwaysPrint(
					"Found a six-membered ring with one double bond.");
			final MolAtom[] ringAtoms = new MolAtom[6];
			final DPoint3[] ringAtomLocs = new DPoint3[6];
			for (int i = 0; i < 6; i++) {
				ringAtoms[i] = molecule.getAtom(ringIndices[i]);
				ringAtomLocs[i] = ringAtoms[i].getLocation();
			} // for i

			// calculate translation vector
			DPoint3 vecTranslate = new DPoint3();
			final DPoint3[] normals = new DPoint3[2];
			if (chair) { // find vector connecting planes of alternating ring atoms
				// will translate alternating atoms up and down along this vector
				final DPoint3 middleOdd = new DPoint3(
						(ringAtomLocs[1].x + ringAtomLocs[3].x + ringAtomLocs[5].x) / 3,
						(ringAtomLocs[1].y + ringAtomLocs[3].y + ringAtomLocs[5].y) / 3,
						(ringAtomLocs[1].z + ringAtomLocs[3].z + ringAtomLocs[5].z) / 3
						);
				final DPoint3 middleEven = new DPoint3(
						(ringAtomLocs[0].x + ringAtomLocs[2].x + ringAtomLocs[4].x) / 3,
						(ringAtomLocs[0].y + ringAtomLocs[2].y + ringAtomLocs[4].y) / 3,
						(ringAtomLocs[0].z + ringAtomLocs[2].z + ringAtomLocs[4].z) / 3
						);
				final DPoint3 vec0to2 = VectorMath.diff(ringAtomLocs[2], ringAtomLocs[0]);
				final DPoint3 vec0to4 = VectorMath.diff(ringAtomLocs[4], ringAtomLocs[0]);
				normals[0] = VectorMath.crossProd(vec0to4, vec0to2);
				final DPoint3 vec3to5 = VectorMath.diff(ringAtomLocs[5], ringAtomLocs[3]);
				final DPoint3 vec3to1 = VectorMath.diff(ringAtomLocs[1], ringAtomLocs[3]);
				normals[1] = VectorMath.crossProd(vec3to5, vec3to1);
				final DPoint3 vecMidEvenToMidOdd = VectorMath.diff(middleEven, middleOdd);
				final DPoint3 projMidEvenOnPlaneOdd = 
						VectorMath.proj1onPlaneNormalTo2(vecMidEvenToMidOdd, normals[0]);
				vecTranslate = VectorMath.diff(vecMidEvenToMidOdd, projMidEvenOnPlaneOdd);
			} else { // half-chair; find vector between atoms 3/4 and plane of other ring atoms
				// will translate alternating atoms up and down along this vector
				final DPoint3 vec3to4 = VectorMath.diff(ringAtomLocs[4], ringAtomLocs[3]);
				final DPoint3 vec2to1 = VectorMath.diff(ringAtomLocs[2], ringAtomLocs[1]);
				final DPoint3 vec2to0 = VectorMath.diff(ringAtomLocs[2], ringAtomLocs[0]);
				normals[0] = VectorMath.crossProd(vec2to1, vec2to0);
				normals[1] = VectorMath.scalarProd(normals[0], -1);
				final DPoint3 projvec3to4OnPlane = 
						VectorMath.proj1onPlaneNormalTo2(vec3to4, normals[0]);
				vecTranslate = VectorMath.diff(vec3to4, projvec3to4OnPlane);
			} // if chair

			// calculate rotation axis and angle of each ring atom and its ligands 
			final DPoint3[] vecRotate = new DPoint3[6];
			final DPoint3 vecNull = new DPoint3(0, 0, 0);
			final double[] rotateAngle = new double[6];
			final int[] direction = new int[2];
			direction[0] = (VectorMath.calcDihedral(new MolAtom[] 
					{ringAtoms[1], ringAtoms[2],
					ringAtoms[3], ringAtoms[4]}, 360) < 0 ? 1 : -1); 
			direction[1] = -direction[0];
			for (int i = (chair ? 0 : 2); i < 6; i++) {  // if half-chair, won't rotate sp2 atoms
				Utils.alwaysPrint("atom ", ringIndices[i] + 1, ":");
				// find equatorial and axial angles to determine rotation angle
				double axAngle = 0;
				double eqAngle = 0;
				final DPoint3 vecToPrev = VectorMath.diff(
						ringAtomLocs[i], 
						ringAtomLocs[MathUtils.getMod(i - 1, 6)]
						);
				final DPoint3 vecToNext = VectorMath.diff(
						ringAtomLocs[MathUtils.getMod(i + 1, 6)],
						ringAtomLocs[i]
						);
				final DPoint3 vecInLigPlane = 
						VectorMath.crossProd(vecToNext, vecToPrev);
				for (int j = 0; j < ringAtoms[i].getBondCount(); j++) { 
					final MolAtom ligand = (MolAtom) ringAtoms[i].getLigand(j);
					if (ligand != ringAtoms[MathUtils.getMod(i - 1, 6)]
							&& ligand != ringAtoms[MathUtils.getMod(i + 1, 6)]) {
						final DPoint3 ligVec = VectorMath.diff(
								ligand.getLocation(), 
								ringAtomLocs[i]
								); // vector ring atom to ligand
						final double angleToNormal = VectorMath.angle(
								ligVec, normals[i % 2]
								);
						// assign angle to equatorial or axial;
						// correct value if normal is pointing wrong way
						Utils.alwaysPrint("   ligand ", j, ": angle to normal is ",
								angleToNormal * 180 / Math.PI, " degrees.");
						if (angleToNormal < Math.PI / 6)
							axAngle = angleToNormal;
						else if (angleToNormal > 5 * Math.PI / 6)
							axAngle = Math.PI - angleToNormal;
						else if (angleToNormal > Math.PI / 2)
							eqAngle = angleToNormal;
						else eqAngle = Math.PI - angleToNormal;

						if (vecRotate[i] == null 
								|| vecRotate[i] == vecNull) { 
							// calculate rotation axis
							final DPoint3 vecRingAtomToLig = VectorMath.diff(
									ligand.getLocation(), 
									ringAtomLocs[i]
									);
							vecRotate[i] = VectorMath.crossProd(
									vecRingAtomToLig, vecInLigPlane);
						} // if vecRotate[i] is null
					} // ligand not a ring atom
				} // for each ligand j
				if (vecRotate[i] == vecNull) {
					vecRotate[i] = VectorMath.crossProd(vecInLigPlane,
						VectorMath.diff(ringAtomLocs[i], 
								ringAtomLocs[MathUtils.getMod(i - 3, 6)]));
					Utils.alwaysPrint("vectors for calculating vector of "
							+ "rotation were collinear, so calculated "
							+ "with opposite atom in ring.");
				} // if the rotation vector was a zero vector
				Utils.alwaysPrint("ax angle = ", axAngle * 180 / Math.PI,
						" degrees; eq angle = ", eqAngle * 180 / Math.PI, " degrees.");
				if (eqAngle != 0) 
					rotateAngle[i] = Math.PI - eqAngle - axAngle;
				else if (axAngle != 0) 
					rotateAngle[i] = Math.PI / 3 - axAngle;
				else rotateAngle[i] = 0;
				rotateAngle[i] = rotateAngle[i] * direction[i % 2];
				Utils.alwaysPrint("rotateAngle[", i, "] = ",
						rotateAngle[i] * 180 / Math.PI, " degrees");
			} // for each ring-atom index i

			for (int i = (chair ? 0 : 2); i < 6; i++) { // if half-chair, don't process sp2 atoms
				final int prev = MathUtils.getMod(i - 1, 6);
				final int next = MathUtils.getMod(i + 1, 6);

				// remove ring bonds to allow selection of one fragment; 
				// will restore soon
				final MolBond prevBond = 
						ringAtoms[prev].getBondTo(ringAtoms[i]);
				final MolBond nextBond = 
						ringAtoms[i].getBondTo(ringAtoms[next]);
				molecule.removeBond(nextBond); 
				molecule.removeBond(prevBond); 

				// find (newly) disconnected fragments
				final SelectionMolecule[] fragments = molecule.findFrags();

				// find the fragment j containing atom i
				final SelectionMolecule atomGraph = new SelectionMolecule();
				atomGraph.add(ringAtoms[i]);
				int j;
				for (j = 0; j < fragments.length; j++) 
					if (fragments[j].contains(atomGraph)) 
						break;

				// rotate fragment j
				if (vecRotate[i] != null && rotateAngle[i] != 0) {
					final CTransform3D turnRingAtom = new CTransform3D();
					turnRingAtom.setRotation(
							vecRotate[i].x, vecRotate[i].y, 
							vecRotate[i].z, rotateAngle[i]);
					turnRingAtom.setRotationCenter(ringAtomLocs[i]);
					fragments[j].transform(turnRingAtom);
				}

				// translate fragment j
				final CTransform3D moveRingAtom = new CTransform3D();
				vecTranslate = VectorMath.scalarProd(vecTranslate, -1);
				moveRingAtom.setTranslation(vecTranslate);
				if (chair || Utils.among(i, 3, 4)) // if half-chair, translate only two atoms
					fragments[j].transform(moveRingAtom);

				// restore the deleted bonds
				molecule.add(nextBond);
				molecule.add(prevBond);
			} // for each ring atom index i

			moleculeStr = MolString.toString(molecule, Utils.MRV);
		}  // a six-membered ring with all single bonds was found
		else moleculeStr = null;
	}  // molecule is not null
	final String APPLET_NAME = "responseApplet";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Chair or Half-Chair Flip</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
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

	function getRotation() {
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(mol) {
			document.tester.molecule.value = mol;
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
<form name="tester" action="flipChairOrHalfChair.jsp" method="post">
<input type="hidden" name="molecule" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Chair or Half-Chair Flip
</P>
<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr><td class="regtext"  colspan="2">
	<% if (moleculeStr != null) { %>
		Here's the flipped chair or half-chair.  Try again, if you like.
	<% } else { %>
		Draw a substituted saturated or monounsaturated six-membered ring (cyclohexane,
		cyclohexene, piperidine, etc.), do a 3D minimization (Structure &rarr; Clean 3D
		&rarr; Clean in 3D), and flip the chair or half-chair.  (If you get this message
		repeatedly, the compound you drew lacks a saturated or monounsaturated six-membered ring.)
	<% } %>
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
</td></tr>
<tr><td>&nbsp;</td></tr>
<tr><td style="text-align:center;">
	<br/>
	<input type="button" value=" Flip Ring " onclick="getRotation()"/>
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
