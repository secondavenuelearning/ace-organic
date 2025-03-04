<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.MolAtom,
	chemaxon.struc.Molecule,
	chemaxon.struc.MoleculeGraph,
	chemaxon.marvin.calculations.GeometryPlugin,
	com.epoch.chem.ChemUtils,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.text.NumberFormat,
	java.util.ArrayList,
	java.util.List"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String mrvStr = request.getParameter("mrvStr");
	final String selectedAtomsStr = request.getParameter("selectedAtomsStr");
	// Utils.alwaysPrint("includes/measure.jsp: selectedAtomsStr = ", selectedAtomsStr);
	final String[] atomIndexStrs = selectedAtomsStr.split(",");
	int numSelectedAtoms = atomIndexStrs.length;
	final int[] atomIndices = new int[numSelectedAtoms];
	for (int atomIndexNum = 0; atomIndexNum < numSelectedAtoms; atomIndexNum++) {
		atomIndices[atomIndexNum] = 
				MathUtils.parseInt(atomIndexStrs[atomIndexNum]) - 1;
	} // for each atom
	double measurement = 0.0;
	String type = null;
	boolean doCalculation = true;

	Molecule molecule = null;
	final GeometryPlugin calculator = new GeometryPlugin();
	if (!Utils.isEmptyOrWhitespace(mrvStr)) {
		molecule = MolImporter.importMol(mrvStr);
		final int numFrags = molecule.getFragCount(
				MoleculeGraph.FRAG_KEEPING_MULTICENTERS);
		if (numFrags > 1) {
			// workaround for GeometryPlugin bug when there is > 1 fragment
			/* Utils.alwaysPrint("measure.jsp: found ", numFrags, 
					" in submission; 0-based atom indices are ", atomIndices,
					" among ", molecule.getAtomCount(), " atoms."); /**/
			for (int atomIndexNum = 0; atomIndexNum < numSelectedAtoms; atomIndexNum++) {
				final int atomIndex = atomIndices[atomIndexNum];
				final MolAtom atom = molecule.getAtom(atomIndex);
				atom.putProperty("selectionNum", Integer.valueOf(atomIndexNum));
			} // for each selected atom
			final Molecule[] molFrags = molecule.convertToFrags();
			boolean foundMolFragWithSelections = false;
			for (final Molecule molFrag : molFrags) {
				int newNumSelectedAtoms = 0;
				final MolAtom[] atoms = molFrag.getAtomArray();
				for (final MolAtom atom : atoms) {
					final Integer atomIndexObj = 
							(Integer) atom.removeProperty("selectionNum");
					if (atomIndexObj != null) {
						foundMolFragWithSelections = true;
						atomIndices[atomIndexObj.intValue()] = molFrag.indexOf(atom);
						newNumSelectedAtoms++;
					} // if the atom was selected
				} // for each atom in the fragment
				if (foundMolFragWithSelections) {
					/* Utils.alwaysPrint("measure.jsp: found the fragment "
							+ "with ", newNumSelectedAtoms, 
							" selections; atomIndices are now ", 
							atomIndices, " for fragment with ", 
							molFrag.getAtomCount(), " atoms."); /**/
					molecule = molFrag;
					doCalculation = numSelectedAtoms == newNumSelectedAtoms;
					break;
				} // if foundMolFragWithSelections
			} // for each fragment molecule
		} // if there are molecule fragments
		calculator.setMolecule(molecule);
	} else doCalculation = false;
	/* Utils.alwaysPrint("measure.jsp: 0-based atomIndices = ", atomIndices,
			"; molecule has ", molecule.getAtomCount(), " atoms.") ;/**/

	if (!Utils.isEmpty(selectedAtomsStr) && doCalculation) {
		if (numSelectedAtoms == 2) {
			final StringBuilder bld = new StringBuilder();
			bld.append("distance between atoms ");
			for (int atomIndexNum = 0; atomIndexNum < numSelectedAtoms; atomIndexNum++) {
				final int atomIndex = atomIndices[atomIndexNum];
				final MolAtom atom = molecule.getAtom(atomIndex);
				if (atomIndexNum > 0) bld.append(" and ");
				bld.append(atom.getSymbol()).append(atomIndex + 1);
			} // for each atom
			type = bld.toString();
			measurement = calculator.getDistance(atomIndices);
		} else if (numSelectedAtoms == 3 || numSelectedAtoms == 4) {
			doCalculation = ChemUtils.arrangeAsBonded(molecule, atomIndices);
			if (doCalculation) {
				final StringBuilder bld = new StringBuilder();
				if (numSelectedAtoms == 4) bld.append("dihedral angle defined by ");
				for (int atomIndexNum = 0; atomIndexNum < numSelectedAtoms; atomIndexNum++) {
					final int atomIndex = atomIndices[atomIndexNum];
					final MolAtom atom = molecule.getAtom(atomIndex);
					if (atomIndexNum > 0) bld.append('-');
					bld.append(atom.getSymbol()).append(atomIndex + 1);
				} // for each atom
				if (numSelectedAtoms == 3) bld.append(" bond angle");
				type = bld.toString();
				measurement = (numSelectedAtoms == 3 
						? calculator.getAngle(atomIndices)
						: calculator.getDihedral(atomIndices));
			} // if atoms can be arranged in a bonded sequence
		} else doCalculation = false; // not 2-4 atoms entered
	} else doCalculation = false; // no atoms entered

	final NumberFormat numberFormat = NumberFormat.getInstance();
	numberFormat.setMaximumFractionDigits(numSelectedAtoms == 2 ? 2 : 1);
	numberFormat.setMinimumFractionDigits(numSelectedAtoms == 2 ? 2 : 1);
	final String valueStr = Utils.toString(numberFormat.format(measurement),
			numSelectedAtoms == 2 ? " &Aring;" : "&deg;");
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<title>Measure distance/angle/dihedral</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>

<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >

	function showMeasurement() {
		if (<%= doCalculation %>) {
			alert(cerToUnicode('The <%= type %> is <%= valueStr %>.'));
		} else {
			alert('Invalid selection. Draw or paste a 3D structure, ' + 
					'and highlight two atoms to get their distance, ' + 
					'three contiguous atoms to get their angle, or four ' + 
					'contiguous atoms to get their dihedral angle. (If ' +
					'your drawing contains two or more independent ' +
					'molecules, all selected atoms must be in a single ' +
					'molecule.)');
		}
		self.close();
	} // showMeasurement()

	// -->
</script>
</head>

<body onload="showMeasurement();">
</body>
</html>
