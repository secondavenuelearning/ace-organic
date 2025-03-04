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
		final Molecule sixMembRing = MolImporter.importMol("*1*****1");
		ourSearch.setTarget(molecule);
		ourSearch.setQuery(sixMembRing);
		final int[] ringIndices = ourSearch.findFirst(); 
		if (ringIndices != null) { 
			// a six-membered ring with all single bonds was found
			Utils.alwaysPrint(
					"Found a six-membered ring with all single bonds.");
			final VectorMath dh = new VectorMath(molecule);
			final MolAtom[] ringAtoms = new MolAtom[6];
			final DPoint3[] ringAtomLocs = new DPoint3[6];
			for (int i = 0; i < 6; i++) {
				ringAtoms[i] = molecule.getAtom(ringIndices[i]);
				ringAtomLocs[i] = ringAtoms[i].getLocation();
			}

			// find vector connecting planes of alternating ring atoms
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
			final DPoint3 vec0to2 = dh.diff(ringAtomLocs[2], ringAtomLocs[0]);
			final DPoint3 vec0to4 = dh.diff(ringAtomLocs[4], ringAtomLocs[0]);
			final DPoint3 vec1to3 = dh.diff(ringAtomLocs[3], ringAtomLocs[1]);
			final DPoint3 vec1to5 = dh.diff(ringAtomLocs[5], ringAtomLocs[1]);
			final DPoint3[] normals = new DPoint3[] {
					dh.crossProd(vec0to4, vec0to2),
					dh.crossProd(vec1to3, vec1to5)
					};
			final DPoint3 vecMidEvenToMidOdd = dh.diff(middleEven, middleOdd);
			final DPoint3 projMidEvenOnPlaneOdd = 
					dh.proj1onPlaneNormalTo2(vecMidEvenToMidOdd, normals[0]);
			final DPoint3 vecPlaneEvenToPlaneOdd = 
					dh.diff(vecMidEvenToMidOdd, projMidEvenOnPlaneOdd);

			// will rotate each ring atom and its ligands 
			// store this info now before we start moving atoms
			final DPoint3[] vecPrev2ToNext2 = new DPoint3[6];
			final double[] rotateAngle = new double[6];
			for (int i = 0; i < 6; i++)  {
				// rotation axis is along vector between 3rd and 5th atoms
				vecPrev2ToNext2[i] = dh.diff(
						ringAtomLocs[MathUtils.getMod(i + 2, 6)],
						ringAtomLocs[MathUtils.getMod(i - 2, 6)]
						);
				// find equatorial and axial angles to determine rotation angle
				double axAngle = 0;
				double eqAngle = 0;
				for (int j = 0; j < ringAtoms[i].getBondCount(); j++) { 
					final MolAtom ligand = (MolAtom) ringAtoms[i].getLigand(j);
					if (ligand != ringAtoms[MathUtils.getMod(i - 1, 6)]
							&& ligand != ringAtoms[MathUtils.getMod(i + 1, 6)]) {
						final DPoint3 ligVec = dh.diff(
								ligand.getLocation(), 
								ringAtomLocs[i]
								); // dh ring atom to ligand
						final double angleToNormal = dh.angle(
								ligVec, 
								dh.scalarProd(normals[i % 2], -1)
								);
						// assign angle to equatorial or axial;
						// correct value if normal is pointing wrong way
						if (angleToNormal < Math.PI / 6)
							axAngle = angleToNormal;
						else if (angleToNormal > 5 * Math.PI / 6)
							axAngle = Math.PI - angleToNormal;
						else if (angleToNormal > Math.PI / 2)
							eqAngle = angleToNormal;
						else eqAngle = Math.PI - angleToNormal;
					} // if ligand is not a ring atom
				} // for each ligand to the ring atom
				Utils.alwaysPrint("atom ", ringIndices[i] + 1, ": ax angle = ",
						axAngle * 180 / Math.PI, " degrees and eq angle = ",
						eqAngle * 180 / Math.PI, " degrees.");
				if (eqAngle != 0) 
					rotateAngle[i] = Math.PI - eqAngle - axAngle;
				else if (axAngle != 0) 
					rotateAngle[i] = Math.PI / 3 - axAngle;
				else rotateAngle[i] = 0;
				// sign of rotation is opposite to sign of ring bonds dihedral
				final double ringDihedral = dh.calcDihedral(new MolAtom[]
						{ringAtoms[MathUtils.getMod(i - 1, 6)], 
						ringAtoms[i], 
						ringAtoms[MathUtils.getMod(i + 1, 6)], 
						ringAtoms[MathUtils.getMod(i + 2, 6)]}, 
						"radians", 360
						);
				if (ringDihedral > 0) rotateAngle[i] = -rotateAngle[i];
				Utils.alwaysPrint("rotateAngle[ ", i, "] = ",
						rotateAngle[i] * 180 / Math.PI, " degrees");
			} // for each ring-atom index i

			for (int i = 0; i < 6; i++) { 
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
					if (fragments[j].contains(atomGraph)) break;

				// rotate fragment j
				final CTransform3D turnRingAtom = new CTransform3D();
				turnRingAtom.setRotation(
						vecPrev2ToNext2[i].x, vecPrev2ToNext2[i].y, 
						vecPrev2ToNext2[i].z, rotateAngle[i]);
				turnRingAtom.setRotationCenter(ringAtomLocs[i]);
				fragments[j].transform(turnRingAtom);

				// translate fragment j
				final CTransform3D moveRingAtom = new CTransform3D();
				final DPoint3 vecTranslate = (i % 2 == 0 
						? dh.scalarProd(vecPlaneEvenToPlaneOdd, -1) 
						: vecPlaneEvenToPlaneOdd);
				moveRingAtom.setTranslation(vecTranslate);
				fragments[j].transform(moveRingAtom);

				// restore the deleted bonds
				molecule.add(nextBond);
				molecule.add(prevBond);
			} // repeat for each ring atom index i

			moleculeStr = MolString.toString(molecule, Utils.MRV);
		}  // a six-membered ring with all single bonds was found
		else moleculeStr = null;
	}  // molecule is not null
	final String APPLET_NAME = "responseApplet";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Chair Flip</title>
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
<form name="tester" action="flipChair.jsp" method="post">
<input type="hidden" name="molecule" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Chair Flip
</P>
<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr><td class="regtext"  colspan="2">
	<% if (moleculeStr != null) { %>
		Here's the flipped chair.  Try again, if you like.
	<% } else { %>
		Please draw a substituted saturated six-membered ring (cyclohexane,
		piperidine, etc.), do a 3D minimization (Structure &rarr; Clean 3D
		&rarr; Clean in 3D), and flip the chair.  (If you get this message
		repeatedly, something is wrong with the chair you drew.)
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
	<input type="button" value=" Flip Chair " onclick="getRotation()"/>
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
