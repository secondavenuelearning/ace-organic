<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
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
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

String moleculeStr = request.getParameter("molecule");
final String task = request.getParameter("task");
boolean inversionNotDone = true;

if (!Utils.isEmptyOrWhitespace(moleculeStr)) {
	/* Utils.alwaysPrint("Before inversions, molecule is:\n", moleculeStr); /**/
	final Molecule molecule = MolImporter.importMol(moleculeStr);

	boolean is2D = false;
	for (int i = 0; i < molecule.getBondCount(); i++) {
		final MolBond bond = molecule.getBond(i);
		if ("mirror".equals(task) || bond.getAtom1().getAtomMap() != 0) {
			final int bondStereoType = bond.getFlags() & MolBond.STEREO1_MASK;
			/* Utils.alwaysPrint("invertStereo.jsp: Bond ", i, 
					" has stereo type ", bondStereoType); /**/
			if (bondStereoType == MolBond.UP) 
				// statement borrowed from JChem API
				bond.setFlags(
						(bond.getFlags() & ~MolBond.STEREO1_MASK) 
						| MolBond.DOWN);
			else if (bondStereoType == MolBond.DOWN) 
				bond.setFlags(
						(bond.getFlags() & ~MolBond.STEREO1_MASK) 
						| MolBond.UP);
			if (Utils.among(bondStereoType, MolBond.UP, MolBond.DOWN)) {
				is2D = true;
				inversionNotDone = false;
				/* Utils.alwaysPrint("invertStereo.jsp: Inverted bond ", i, 
						" to stereo type ",
						(bond.getFlags() & MolBond.STEREO1_MASK)); /**/
			} // if bondStereoType
		} // if atom1 of bond is mapped
	} // for each bond i

	if (is2D) {
		for (final MolAtom atom : molecule.getAtomArray())
			if (atom.getAtomMap() != 0) atom.setAtomMap(0);
	} else if ("mirror".equals(task)) {  // 3D, invert everything
		for (final MolAtom atom : molecule.getAtomArray()) {
			DPoint3 atomLoc = atom.getLocation();
			atomLoc = VectorMath.scalarProd(atomLoc, -1);
			atom.setLocation(atomLoc);
		} // for each atom i
		inversionNotDone = false;
	} else { // 3D, invert selected atoms
		for (int i = 0; i < molecule.getAtomCount(); i++) { 
			final MolAtom atom = molecule.getAtom(i);
			final int numBonds = atom.getBondCount();
			if (atom.getAtomMap() != 0 && numBonds >= 3) {
				/* Utils.alwaysPrint("Processing atom ", atom, i + 1,
						", which has ", atom.getBondCount(), " ligands."); /**/
				// find atoms that can be rotated.
				// neither of the two ligands that are rotated can be
				// in same fragment as either of the two ligands that
				// are not rotated, but the pair that are rotated or the
				// pair that aren't rotated can be in the same fragment
				final MolAtom[] oneCxnToAtom = new MolAtom[2];
				int oneCxnCount = 0;
				final SelectionMolecule graphAtom = new SelectionMolecule();
				graphAtom.add(atom);
				for (int j = numBonds - 1; j >= 0; j--) { 
					// go backwards so ligands aren't reoordered
					// when bonds are deleted and then added back in
					final MolAtom ligj = (MolAtom) atom.getLigand(j); 
					/* Utils.alwaysPrint("Looking at ligand ", ligj,
						 	molecule.indexOf(ligj) + 1); /**/
					final MolBond bondToJ = atom.getBondTo(ligj);
					final SelectionMolecule graphJ = new SelectionMolecule();
					graphJ.add(ligj);
					if (numBonds == 3) {
						// need to find just one ligand with a single connection
						// to atom; fourth ligand is implicit H.
						molecule.removeBond(bondToJ); 
						// find (newly) disconnected fragments
						final SelectionMolecule[] fragments = molecule.findFrags();
						if (oneCxnCount == 0)
							for (int m = 0; m < fragments.length; m++) 
								if (fragments[m].contains(graphAtom) 
										&& !fragments[m].contains(graphJ)) {
									oneCxnToAtom[0] = ligj;
									oneCxnCount++;
								} // if j and atom have > one connection
						// restore deleted bond
						molecule.add(bondToJ);
						// Utils.alwaysPrint("Found ", oneCxnCount, " rotatable ligands.");
						if (oneCxnCount == 1) break;
					} else { // numBonds == 4
						// need to find two ligands that when both 
						// are disconnected from atom, neither is still
						// connected to atom through another route
						for (int k = numBonds - 1; k >= j + 1; k--) {
							final MolAtom ligk = atom.getLigand(k); 
							final MolBond bondToK = atom.getBondTo(ligk);
							final SelectionMolecule graphK = new SelectionMolecule();
							graphK.add(ligk);
							// remove bonds to determine connections; 
							// will restore soon
							molecule.removeBond(bondToJ); 
							molecule.removeBond(bondToK); 
							// find (newly) disconnected fragments
							final SelectionMolecule[] fragments = molecule.findFrags();
							for (int m = 0; m < fragments.length; m++) 
								if (fragments[m].contains(graphAtom) 
										&& !fragments[m].contains(graphJ) 
										&& !fragments[m].contains(graphK)) {
									oneCxnToAtom[0] = ligj;
									oneCxnToAtom[1] = ligk;
									oneCxnCount += 2;
								} // if j and atom have > one connection
							// restore deleted bonds
							molecule.add(bondToK);
							molecule.add(bondToJ);
							/* Utils.alwaysPrint("At k= ", k, ", found ",
									oneCxnCount, " rotatable ligands."); /**/
							if (oneCxnCount == 2) break;
						} // for k = each ligand
					} // if numBonds
					/* Utils.alwaysPrint("Found a total of ", oneCxnCount,
							" rotatable ligands."); /**/
					if (numBonds - oneCxnCount == 2) break;
				} // for j = each ligand

				if (numBonds - oneCxnCount == 2) {
					// Utils.alwaysPrint("Ready to calculate vector of rotation.");
					// calculate vector of rotation
					DPoint3 vecRotate = new DPoint3();
					final DPoint3 atomLoc = atom.getLocation();
					final MolAtom[] statLig = new MolAtom[2];

					// the atoms to rotate are in oneCxnToAtom[]
					// the atoms not in oneCxnToAtom[] need to be disconnected 
					// so that the atoms in oneCxnToAtom[] can be rotated
					// Utils.alwaysPrint("Looking for nonrotating ligands.");
					int numStatLigs = 0;
					for (int j = 0; j < numBonds; j++) {
						final MolAtom ligj = atom.getLigand(j); 
						/* Utils.alwaysPrint("Checking ligand ", j,
								" is not among rotating ligands; numStatLigs = ",
								numStatLigs); /**/
						if ((oneCxnCount == 2 
									&& ligj != oneCxnToAtom[0] 
									&& ligj != oneCxnToAtom[1]) 
								|| (oneCxnCount == 1 
									&& ligj != oneCxnToAtom[0])) {
							statLig[numStatLigs] = ligj;
							numStatLigs++;
						} // if oneCxnCount
						/* Utils.alwaysPrint("Found ", numStatLigs,
								" nonrotating ligands."); /**/
						if (numStatLigs == 2) break;
					} // for each bond j to atom

					DPoint3 lig0Loc = new DPoint3();
					DPoint3 lig1Loc = new DPoint3();
					if (numBonds == 3) {
						lig0Loc = statLig[0].getLocation();
						lig1Loc = statLig[1].getLocation();
					} else { // numBonds = 4
						lig0Loc = oneCxnToAtom[0].getLocation();
						lig1Loc = oneCxnToAtom[1].getLocation();
					} // if numBonds

					// normalize vector bond lengths before calculating
					// direction of rotation vector
					DPoint3 vecAtomToLig0 = VectorMath.diff(lig0Loc, atomLoc);
					DPoint3 vecAtomToLig1 = VectorMath.diff(lig1Loc, atomLoc);
					vecAtomToLig0 = VectorMath.scalarQuot(
							vecAtomToLig0, VectorMath.length(vecAtomToLig0));
					vecAtomToLig1 = VectorMath.scalarQuot(
							vecAtomToLig1, VectorMath.length(vecAtomToLig1));
					vecRotate = VectorMath.sum(vecAtomToLig0, vecAtomToLig1);

					// remove bonds to nonrotating ligands to allow 
					// selection of one fragment; will restore soon
					final MolBond bondTo0 = atom.getBondTo(statLig[0]);
					final MolBond bondTo1 = atom.getBondTo(statLig[1]);
					molecule.removeBond(bondTo0); 
					molecule.removeBond(bondTo1); 

					// find (newly) disconnected fragments
					final SelectionMolecule[] fragments = molecule.findFrags();

					// find the fragment j containing atom
					final SelectionMolecule atomGraph = new SelectionMolecule();
					atomGraph.add(atom);
					int j;
					for (j = 0; j < fragments.length; j++) 
						if (fragments[j].contains(atomGraph)) 
							break;

					// rotate fragment j
					final CTransform3D turnAtom = new CTransform3D();
					turnAtom.setRotation(
							vecRotate.x, vecRotate.y, vecRotate.z, Math.PI);
					turnAtom.setRotationCenter(atomLoc);
					fragments[j].transform(turnAtom);
					inversionNotDone = false;

					// restore the deleted bonds
					molecule.add(bondTo0);
					molecule.add(bondTo1);
				} // else Utils.alwaysPrint("Couldn't find ligands to invert."); 
			} // atom is mapped & there are at least 3 groups attached to it
			if (atom.getAtomMap() != 0) atom.setAtomMap(0);
		}  // for each atom
	}  // molecule is 3D
	moleculeStr = MolString.toString(molecule, Utils.MRV);
}  // molecule is not null
final String APPLET_NAME = "responseApplet";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<title>Invert configurations</title>
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

	function invert() {
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(mol) {
			document.tester.molecule.value = mol;
			document.tester.task.value = 'invert';
			document.tester.submit();
		}, function(error) {
			alert('Molecule export failed:' + error);	
		});
	}

	function mirror() {
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(mol) {
			document.tester.molecule.value = mol;
			document.tester.task.value = 'mirror';
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
<form name="tester" action="invertStereo.jsp" method="post">
<input type="hidden" name="molecule" value=""/>
<input type="hidden" name="task" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Stereocenter configuration inversion
</p>
<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr><td class="regtext"  colspan="2">
	<% if (moleculeStr != null) { 
		if (inversionNotDone) { %>
			Either there were no mapped atoms, or the compound's topology 
			made it impossible to invert them. 
		<% } else { %>
			Here's the isomer.  
		<% } %>
		Try again, if you like.  
		<% if ("invert".equals(task)) { %>
			(You will need to designate again which centers to invert.)
		<% } 
	} else { %>
		Draw a compound with one or more sp<sup>3</sup> stereocenters (you may 
		use bold and hashed wedges to show the stereochemistry or you may use 
		3D minimization (Edit &rarr; Clean &rarr; 3D &rarr; Clean in 3D)), 
		map the stereocenters you want to invert with the number 1 
		(right-click or control-click &rarr; Map &rarr; M1), and press the
		Invert button below to invert them.  (If you get this message
		repeatedly, something is wrong with the chair you drew.)  Or, you can
		get the mirror image of the entire compound by pressing the Mirror button.  
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
				MARVIN, SHOWMAPPING, 
				'<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td></tr>
<tr><td>&nbsp;</td></tr>
<tr><td style="text-align:center;">
	<input type="button" value=" Invert " onclick="invert()"/>
	&nbsp;&nbsp;&nbsp;
	<input type="button" value=" Mirror " onclick="mirror()"/>
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

