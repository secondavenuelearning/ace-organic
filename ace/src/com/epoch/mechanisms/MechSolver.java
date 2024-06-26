package com.epoch.mechanisms;

import chemaxon.formats.MolImporter;
import chemaxon.formats.MolFormatException;
import chemaxon.struc.AtomProperty.Radical;
import chemaxon.struc.DPoint3;
import chemaxon.struc.MDocument;
import chemaxon.struc.MObject;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import chemaxon.struc.Molecule;
import chemaxon.struc.PeriodicSystem;
import chemaxon.struc.SelectionMolecule;
import chemaxon.struc.Sgroup;
import chemaxon.struc.SgroupType;
import chemaxon.struc.graphics.MEFlow;
import chemaxon.struc.sgroup.SgroupAtom;
import chemaxon.struc.sgroup.SuperatomSgroup;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.FormulaFunctions;
import com.epoch.chem.MolString;
import com.epoch.chem.Normalize;
import com.epoch.chem.StereoFunctions;
import com.epoch.chem.VectorMath;
import com.epoch.exceptions.ValenceException;
import com.epoch.mechanisms.mechConstants.MechConstants;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Calculates products of molecules and electron-flow arrows. */
public final class MechSolver implements MechConstants {

	private static void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

/* ******************* Members ***************************/

	/** MRV describing the reactants. */
	transient private final String reactants;
	/** The molecule whose products are to be calculated. */ 
	transient private Molecule molecule;
	/** The molecule's document; contains flow arrows. */ 
	transient private MDocument doc;
	/** Electron-flow arrows associated with the molecule. */
	transient final private List<MechFlow> eFlows = new ArrayList<MechFlow>();
	/** All atoms of the electron-flow sources and sinks. */
	transient private final List<MolAtom> allFlowAtoms = new ArrayList<MolAtom>();
	/** Number of electrons in each bond connecting two atoms, keyed by 
	 * bonds. */
	transient private final Map<MolBond, Integer> bondElecCts = 
			new HashMap<MolBond, Integer>();
	/** Number of implicit H atoms on each atom in the molecule.
	 * Used to assign charges and radicals after electron flows.  */
	transient private int[] initImplicitHcounts;
	/** Bonds newly formed by electron-flow arrows. */
	transient private final List<MolBond> newBonds = new ArrayList<MolBond>();
	/** Atoms of bonds removed by electron-flow arrows. */
	transient private final List<MolAtom[]> removedBondAtoms = 
			new ArrayList<MolAtom[]>();
	/** Newly added bonds to H to force valence errors where warranted.  They
	 * need to be located properly if this class is called from the mechanism 
	 * calculator JSP page. */
	transient private final List<MolBond> bondsToNewH = new ArrayList<MolBond>();
	/** Attachment points of multiatom groups; they masquerade as atoms. */
	transient private final List<MolAtom> multicenterAttachPts = new ArrayList<MolAtom>();
	/** Atoms in odd-electron multiatom groups that have an odd number of
	 * aromatic bonds.  Used to correct bond order in setChargesAndRadicals(). */
	transient private final List<MolAtom> atomsInOddAtomGroupsHavingOddAromBonds = 
			new ArrayList<MolAtom>();

/* ******************* Constructor ***************************/

	/** Constructor. 
	 * @param	xmlStr	MRV representation of the molecules and
	 * electron-flow arrows whose products will be calculated
	 */
	public MechSolver(String xmlStr) { 
		reactants = xmlStr;
		debugPrint("MechSolver: reactants:\n", reactants);
	} // MechSolver(String)

/* ******************* Public methods ***************************/

	/** Calculates products of molecules and electron-flow arrows.  
	 * @return	the calculated products
	 * @throws	MechError	when something is fundamentally wrong with the
	 * products of the electron-flow arrows, e.g., they contain hypervalent
	 * atoms or one-electron bonds 
	 * @throws	MolFormatException	when the reactants can't be imported into a
	 * Molecule
	 */
	public Molecule getProducts() throws MechError, MolFormatException {
		final boolean FROM_JSP = true;
		return getProducts(!FROM_JSP);
	} // getProducts()

	/** Calculates products of molecules and electron-flow arrows.  
	 * @return	the calculated products
	 * @param	fromJSP	whether the reactants come from the
	 * mechanism calculator JSP page; when true, atoms in product may be moved
	 * and MechErrors are not thrown
	 * @throws	MechError	when not called from the mechanism calculator and
	 * something is fundamentally wrong with the
	 * products of the electron-flow arrows, e.g., they contain hypervalent
	 * atoms or one-electron bonds 
	 * @throws	MolFormatException	when the reactants can't be imported into a
	 * Molecule
	 */
	public Molecule getProducts(boolean fromJSP) throws MechError, 
			MolFormatException {
		final String SELF = "MechSolver.getProducts: ";
		molecule = MolImporter.importMol(reactants);
		molecule.dearomatize();
		getDocAndFlows();
		if (Utils.isEmpty(eFlows)) return new Molecule();
		final int bondCt = molecule.getBondCount();
		final int numFlows = eFlows.size();
		debugPrint(SELF + "reaction ", fromJSP ? "from JSP page " : "", 
				"has ", FormulaFunctions.getAtomCount(molecule), " atoms, ", 
				bondCt, " bond", bondCt == 1 ? "" : "s", ", and ", 
				numFlows, " e-flow arrow", numFlows == 1 ? "" : "s", '.');
		standardizeStereoBonds();
		removeUntouchedMolecules();
		findMulticenterAtoms();
		countElectronsAndImplicitH();
		moveElectrons();
		checkNoNegativeUnshared();
		final List<MolBond> oddElectronBonds = setBondTypes(fromJSP);
		setChargesAndRadicals();
		final List<MolAtom> tooManyElectronAtoms = 
				resetHCounts(!oddElectronBonds.isEmpty());
		/* Normalize.standardizeAllylAndDienyl(molecule);
		debugPrintMRV(SELF + "after normalizing allyl- and dienyl-metal "
				+ " complexes:\n", molecule); /**/
		ungroupUnbondedGroups();
		if (fromJSP) cleanCalculatedProduct(!oddElectronBonds.isEmpty());
		else lookForInvalidProducts(oddElectronBonds, tooManyElectronAtoms);
		return molecule;
	} // getProducts(boolean)

/* ******************* Methods called by getProducts() ***************************/

	/** Imports the MDocument from the molecule, and extracts the electron-flow
	 * arrows therefrom.
	 * @throws	MechError	if a two-electron flow points from an atom to one of
	 * its ligands
	 */
	private void getDocAndFlows() throws MechError {
		final String SELF = "MechSolver.getDocAndFlows: ";
		doc = molecule.getDocument();
		if (doc != null) {
			for (int objNum = 0; objNum < doc.getObjectCount(); ++objNum) {
				final MObject obj = doc.getObject(objNum);
				if (obj instanceof MEFlow) {
					final MechFlow flow = 
							new MechFlow((MEFlow) obj, molecule);
					eFlows.add(flow);
				} // if it's a flow arrow
			} // for each object in document
		} else Utils.alwaysPrint("MechSolver.getDocAndFlows: error: " 
				+ "could not acquire MDocument");
	} // getDocAndFlows()

	/** Standardizes the bonds in the molecule to avoid stereochemistry matching
	 * problems during and after processing.  */
	private void standardizeStereoBonds() {
		final String SELF = "MechSolver.standardizeStereoBonds: ";
		Normalize.normalizeStereoBonds(molecule);
		StereoFunctions.allWavyToCrissCross(molecule);
		debugPrint(SELF + "after standardizing stereo bonds, molecule:\n",
				molecule);
	} // standardizeStereoBonds()

	/** Finds atoms in molecule fragments touched by e-flow arrows, and removes 
	 * atoms NOT in such molecule fragments.  Also stores the atoms of the
	 * e-flow arrows for later reference.  */
	private void removeUntouchedMolecules() {
		final String SELF = "MechSolver.removeUntouchedMolecules: ";
		final List<MolAtom> atomsInTouchedMols = new ArrayList<MolAtom>();
		int flowNum = 1;
		for (final MechFlow eFlow : eFlows) {
			final MolAtom[] srcAtoms = eFlow.getSrcAtoms();
			if (!Utils.isEmpty(srcAtoms)) {
				debugPrint(SELF + "source atoms of electron-flow arrow ",
						flowNum, ": ", srcAtoms);
				for (final MolAtom srcAtom : srcAtoms) allFlowAtoms.add(srcAtom);
				addAtomsInTouchedMols(srcAtoms[0], atomsInTouchedMols);
			} // if the source atoms are still present
			final MolAtom[] sinkAtoms = eFlow.getSinkAtoms();
			if (!Utils.isEmpty(sinkAtoms)) {
				debugPrint(SELF + "sink atoms of electron-flow arrow ",
						flowNum, ": ", sinkAtoms);
				for (final MolAtom sinkAtom : sinkAtoms) allFlowAtoms.add(sinkAtom);
				addAtomsInTouchedMols(sinkAtoms[0], atomsInTouchedMols);
				if (sinkAtoms.length > 1 && eFlow.sinkIsIncipBond()) { 
					addAtomsInTouchedMols(sinkAtoms[1], atomsInTouchedMols);
				} // end sink
			} // if the sink atoms are still present
			debugPrint(SELF + "atoms in fragment molecules touched by "
					+ "electron-flow arrows so far: ", atomsInTouchedMols);
			flowNum++;
		} // for each flow
		boolean didRemoveAtoms = false;
		for (int atomNum = molecule.getAtomCount() - 1; 
				atomNum >= 0; atomNum--) {
			final MolAtom atom = molecule.getAtom(atomNum);
			if (!atomsInTouchedMols.contains(atom)) {
				debugPrint(SELF + "removing atom ", atom, atomNum + 1, 
						" of molecule fragment not touched by e-flow arrows.");
				molecule.removeAtom(atom);
				didRemoveAtoms = true;
			} else { // make list shorter for next pass
				atomsInTouchedMols.remove(atom);
			} // if atom is not in a molecule touched by e-flow arrow
		} // for each atom
		if (didRemoveAtoms) debugPrintMRV(SELF + "After removing molecule "
				+ "fragments not touched by e-flow arrows:\n", molecule, 
				"has ", FormulaFunctions.getAtomCount(molecule), " atoms, ", 
				molecule.getBondCount(), " bonds, and ", eFlows.size(), 
				" e-flow arrows.\n");
	} // removeUntouchedMolecules()

	/** Finds and stores the multicenter attachment points of the molecule. */
	private void findMulticenterAtoms() {
		multicenterAttachPts.addAll(ChemUtils.getMulticenterAtoms(molecule));
	} // findMulticenterAtoms()

	/** Counts and stores the number of electrons currently in each bond,
	 * and counts and stores the unshared electrons and implicit H atoms of 
	 * each atom.  */
	private void countElectronsAndImplicitH() {
		final String SELF = "MechSolver.countElectronsAndImplicitH: ";
		debugPrintMRV(SELF + "\n", molecule);
		final int numAtoms = molecule.getAtomCount();
		initImplicitHcounts = new int[numAtoms];
		for (final MolBond bond : molecule.getBondArray()) {
			bondElecCts.put(bond, Integer.valueOf(getNumBondElecs(bond)));
		} // for each bond
		for (int atomNum = 0; atomNum < numAtoms; atomNum++) {
			final MolAtom atom = molecule.getAtom(atomNum);
			if (isMulticenterAtom(atom)) continue;
			initImplicitHcounts[atomNum] = atom.getImplicitHcount();
			final int numBonds = initImplicitHcounts[atomNum] 
					+ atom.twicesumbonds(INCLUDE_EXPLICIT_H, 
						USE_BOND_ORDERS) / 2;
			final int valenceElecs = getValenceElectrons(atom);
			int unshared = valenceElecs - numBonds - atom.getCharge();
			int sumBondElecs = getSumBondElecCts(atom, atomNum);
			if (unshared % 2 != 0 && sumBondElecs % 2 != 0
					&& inOddAtomGroupWithAromBond(atom)) {
				debugPrint(SELF + "atom ", atom, atomNum + 1, " is in "
						+ "odd-electron multiatom group, has aromatic bond; "
						+ "altering unshared electron count accordingly.");
				unshared--;
				sumBondElecs++;
			} // if have odd unshared electrons and odd bond electrons
			setElectrons(atom, unshared);
			debugPrint(SELF, atom, atomNum + 1, " has ", unshared, 
					" unshared electron", unshared == 1 ? "" : "s", 
					", ", numBonds, " bond", numBonds == 1 ? "" : "s", 
					" (including ", 
					initImplicitHcounts[atomNum], " implicit H atom", 
					initImplicitHcounts[atomNum] == 1 ? "" : "s", 
					"), and a total of ", sumBondElecs, " electron", 
					sumBondElecs == 1 ? "" : "s", " in its bonds.");
		} // each 0-based atom index atomNum
	} // countElectronsAndImplicitH()

	/** Calculates new bond electron counts, makes the new sigma bonds, 
	 * stores their indices.  Note: Bond orders of existing bonds are not 
	 * changed until later.
	 * @throws	MechError	if an atom has too few valence electrons for the
	 * electron-flow arrow originating at it
	 */
	private void moveElectrons() throws MechError {
		final String SELF = "MechSolver.moveElectrons: ";
		debugPrint(SELF + "Before calculating new bond orders: ");
		printAtoms();
		printBonds();
		int flowNum = 0;
		for (final MechFlow eFlow : eFlows) {
			debugPrint(SELF + "processing Flow # ", ++flowNum, 
					" with object index ", eFlow.getObjectIndex(), ": ");
			// calculate bond orders and unshared electron counts for sources
			if (eFlow.srcIsAtom()) {
				debugPrint(SELF + "source is MolAtom.");
				final MolAtom atom = eFlow.getSrcAtom();
				if (!isMulticenterAtom(atom)) {
					changeElectronCount(atom, -eFlow.getNumElectrons());
				} // if atom is not multicenter
			} else if (eFlow.srcIsBond()) {
				debugPrint(SELF + "source is MolBond.");
				final MolBond bond = eFlow.getSrcBond();
				final int oldBondElecCt = bondElecCts.remove(bond).intValue();
				final int newBondElecCt = 
						oldBondElecCt - eFlow.getNumElectrons();
				bondElecCts.put(bond, Integer.valueOf(newBondElecCt)); 
				debugPrint(SELF + "source bond electron count changed from ", 
						oldBondElecCt, " to ", newBondElecCt); 
				if (getNumBondElecs(bond) == 2) {
					// change parity of atoms to 0
					final MolAtom atom1 = bond.getAtom1();
					final MolAtom atom2 = bond.getAtom2();
					final int atom1Num = molecule.indexOf(atom1);
					final int atom2Num = molecule.indexOf(atom2);
					final int atom1Parity = molecule.getParity(atom1Num);
					final int atom2Parity = molecule.getParity(atom2Num);
					if (atom1Parity != 0 || atom2Parity != 0) {
						debugPrint(SELF + "breaking single bond connecting "
								+ "atoms ", atom1, atom1Num + 1, 
								" with parity ", atom1Parity, " and ", atom2, 
								atom2Num + 1, " with parity ", atom2Parity, 
								"; changing parities to 0.");
						molecule.setParity(atom1Num, 0);
						molecule.setParity(atom2Num, 0);
					} // if either atom of breaking single bond has parity
				} // if breaking bond is single
			} else debugPrint(SELF + "source has unknown type.");
			// calculate bond orders and unshared electron counts for sinks
			if (eFlow.sinkIsIncipBond()) {
				final MolAtom[] atoms = eFlow.getSinkAtoms();
				debugPrint(SELF + "sink is incipient bond:\n", SELF, 
						"   atom1: ", atoms[0], molecule.indexOf(atoms[0]) + 1,
						'\n', SELF, "   atom2: ", atoms[1], 
						molecule.indexOf(atoms[1]) + 1);
				MolBond bond = atoms[0].getBondTo(atoms[1]);
				if (bond == null) {
					// the incipient bond has not yet been created
					debugPrint(SELF + "   Adding a new bond between "
							+ "these atoms");
					bond = new MolBond(atoms[0], atoms[1]);
					molecule.add(bond);
					newBonds.add(bond); 
					final int numElectrons = eFlow.getNumElectrons();
					bondElecCts.put(bond, Integer.valueOf(numElectrons));
				} else {
					final int oldBondElecCt = 
							bondElecCts.remove(bond).intValue();
					final int newBondElecCt = 
							oldBondElecCt + eFlow.getNumElectrons();
					bondElecCts.put(bond, Integer.valueOf(newBondElecCt)); 
				} // if there is not already a bond there
				if (eFlow.srcIsBond()) {
					final MolBond srcBond = eFlow.getSrcBond();
					if (srcBond.getType() == MolBond.COORDINATE) {
						final MolAtom coordBondDonor = srcBond.getAtom1();
						final boolean coordDonorIsMulticenterAtom = 
								isMulticenterAtom(coordBondDonor);
						if (coordDonorIsMulticenterAtom) {
							final MolAtom[] groupAtoms = getAtomsOfGroup(
									getMultiatomGroup(coordBondDonor));
							if (groupAtoms.length == 2) {
								debugPrint(SELF + "Reducing electron count of "
										+ "two-atom group making source "
										+ "coordinate bond");
								final MolBond donorBond =
										groupAtoms[0].getBondTo(groupAtoms[1]);
								final int oldBondElecCt = 
										bondElecCts.remove(donorBond).intValue();
								final int newBondElecCt = 
										oldBondElecCt - eFlow.getNumElectrons();
								bondElecCts.put(donorBond, 
										Integer.valueOf(newBondElecCt)); 
							} // if electron donor is 2-atom group
						} // if coordinate atom is multiatom group
					} // if source bond is coordinate
				} // if source is bond
			} else if (eFlow.sinkIsAtom()) {
				debugPrint(SELF + "sink is MolAtom.");
				final MolAtom sinkAtom = eFlow.getSinkAtom();
				int numElecs = eFlow.getNumElectrons();
				if (eFlow.srcIsBond()) {
					final MolBond srcBond = eFlow.getSrcBond();
					if (srcBond.getType() == MolBond.COORDINATE) {
						final MolAtom bondFromAtom = srcBond.getAtom1();
						final MolAtom bondToAtom = srcBond.getAtom2();
						final boolean isMulticenterAtom = 
								isMulticenterAtom(bondFromAtom);
						final boolean isEvenAtomGroup = isMulticenterAtom
								&& isEvenAtomGroup(bondFromAtom);
						final boolean isOddAtomGroup = 
								isMulticenterAtom && !isEvenAtomGroup;
						final boolean coordinateDonorIsSink = 
								sinkAtom == bondFromAtom; // pointer equality
						debugPrint(SELF + "breaking coordinate bond starting "
								+ "at ", !isMulticenterAtom ? "atom" 
									: isEvenAtomGroup ? "even-atom ligand" 
									: "odd-atom ligand",
								" with ", numElecs, "-electron arrow; "
								+ "electrons in bond are going to coordinate "
								+ "bond ", coordinateDonorIsSink ? "donor"
									: "recipient", '.');
						if (coordinateDonorIsSink) {
							numElecs = (isOddAtomGroup ? 1 : 0);
						} else {
							changeElectronCount(bondFromAtom, 
									isOddAtomGroup ? -1 : -numElecs);
							if (numElecs == 1 && isOddAtomGroup) {
								changeElectronCount(bondToAtom, 1);
							} // if odd-electron arrow from bond from odd-atom group
						} // if sink is coordinate donor
						if (isOddAtomGroup) {
							changeElectronCount(bondToAtom, -1);
						} // if coordinate donor is odd-atom multiatom group
					} // if source bond is coordinate
				} // if source is bond
				changeElectronCount(sinkAtom, numElecs);
			} else if (eFlow.sinkIsBond()) {
				debugPrint(SELF + "sink is MolBond.");
				final MolBond bond = eFlow.getSinkBond();
				final int oldBondElecCt = bondElecCts.remove(bond).intValue();
				final int newBondElecCt = 
						oldBondElecCt + eFlow.getNumElectrons();
				bondElecCts.put(bond, Integer.valueOf(newBondElecCt)); 
				debugPrint(SELF + "sink bond electron count changed from ", 
						oldBondElecCt, " to ", newBondElecCt);
			} else debugPrint(SELF + "sink has unknown type.");
		} // next eflow arrow flowNum
		// needed for result display in mechanism calculator
		for (final MechFlow eFlow : eFlows) {
			doc.removeObject(eFlow.getMEFlow());
		} // next eflow arrow flowNum
		debugPrint(SELF + "After calculating new bond orders: ");
		printAtoms();
		printBonds();
	} // moveElectrons()

	/** Throws an error if there are any atoms with unshared electrons &lt; 0.
	 * @throws	MechError	if any atoms have a negative number of unshared
	 * electrons
	 */
	private void checkNoNegativeUnshared() throws MechError {
		final String SELF = "MechSolver.checkNoNegativeUnshared: ";
		for (final MolAtom atom : molecule.getAtomArray()) {
			final int numElecs = getElectrons(atom);
			if (numElecs < 0 && !isMulticenterAtom(atom)) {
				debugPrint(SELF + "atom ", molecule.indexOf(atom) + 1, 
						" has negative number of electrons: ", numElecs);
				final Integer atomOrigIndexObj =
						(Integer) atom.getProperty(ORIG_INDEX);
				if (atomOrigIndexObj == null) {
					throw new MechError("negative valence electrons", 
							NEGATIVE_UNSHARED_ELECTRONS);
				} else {
					throw new MechError("negative valence electrons", 
							NEGATIVE_UNSHARED_ELECTRONS, 
							new int[] {atomOrigIndexObj.intValue()});
				} // if atomOrigIndexObj == null
			} // if atom has insufficient valence electrons
		} // for each atom
	} // checkNoNegativeUnshared()

	/** After electrons are moved, sets new bond types, removes bonds of order 
	 * 0, and erases stereochemistry of double bonds whose atoms are involved in 
	 * changing bonds.
	 * @param	fromJSP	whether the reactants come from the mechanism 
	 * calculator JSP page
	 * @return	list of bonds that have an odd number of electrons
	 * @throws	MechError	if a bond is calculated to have a negative number of
	 * electrons
	 */
	private List<MolBond> setBondTypes(boolean fromJSP) throws MechError {
		final String SELF = "MechSolver.setBondTypes: ";
		final List<MolBond> oddElectronBonds = new ArrayList<MolBond>();
		for (int bondNum = molecule.getBondCount() - 1; 
				bondNum >= 0; bondNum--) {
			final MolBond bond = molecule.getBond(bondNum);
			final int newBondElecCt = bondElecCts.get(bond).intValue();
			if (newBondElecCt % 2 != 0 && !inOddAtomGroup(bond)) {
				// new bond type is half-integral; store fact for later
				oddElectronBonds.add(bond);
				debugPrint(SELF + "bond ", bondNum + 1, 
						" has odd number of electrons, ", newBondElecCt);
			} // if bond order is half-integral
			if (newBondElecCt < 0) {
				debugPrint(SELF + "bond ", bondNum + 1, 
						" has negative number of electrons, ", newBondElecCt);
				// can't display calculated products
				final MolAtom[] bondAtoms = new MolAtom[]
						{bond.getAtom1(), bond.getAtom2()};
				final Integer[] atomIndexObjs =
						{(Integer) bondAtoms[0].getProperty(ORIG_INDEX),
						(Integer) bondAtoms[1].getProperty(ORIG_INDEX)};
				throw new MechError("negative bond", NEGATIVE_BOND,
						Utils.toPrimitive(atomIndexObjs));
			} // if bond order is negative
			final boolean bondIsNew = newBonds.contains(bond); // pointer equality 
			if (newBondElecCt != getNumBondElecs(bond) || bondIsNew) { 
				setBondType(bond, bondNum, newBondElecCt, 
						bondIsNew, fromJSP);
			} // if newBondElecCt != getNumBondElecs(bond)
		} // next bond bondNum
		debugPrint(SELF + "After setting new bond types: ");
		printAtoms();
		printBonds();
		debugPrintMRV(molecule);
		return oddElectronBonds;
	} // setBondTypes(boolean)

	/** Sets charges and radical flags, calculating them from bond orders and 
	 * unshared electrons.  */
	private void setChargesAndRadicals() {
		final String SELF = "MechSolver.setChargesAndRadicals: ";
		int chargeFlag = 0;
		for (int atomNum = 0; atomNum < molecule.getAtomCount(); atomNum++) {
			final MolAtom atom = molecule.getAtom(atomNum);
			final int nowUnshared = getElectrons(atom);
			int sumBondElecCts = getSumBondElecCts(atom, atomNum);
			if (atomsInOddAtomGroupsHavingOddAromBonds.contains(atom)) { // pointer equality
				sumBondElecCts++;
			} // if atom is in odd-atom group and has an odd number of aromatic bonds
			final int nowImplicitH = atom.getImplicitHcount();
			debugPrint(SELF + "after calculating new bonds, ", atom, 
					atomNum + 1, " with ", getValenceElectrons(atom),
					" valence electron(s) has ", nowUnshared, 
					" unshared electron", nowUnshared == 1 ? "" : "s", 
					", a sum of bond orders of ", 
					sumBondElecCts / 2.0,
					", and ", nowImplicitH, " implicit H atom", 
					nowImplicitH == 1 ? " (" : "s (", 
					initImplicitHcounts[atomNum], " beforehand)");
			if (setAtomCharge(atom, atomNum, sumBondElecCts, chargeFlag)) {
				chargeFlag = 1 - chargeFlag;
				debugPrint(SELF + "toggling charge flag to ", chargeFlag);
			} // if should toggle the charge flag
			setRadicalState(atom, atomNum, sumBondElecCts);
			// Check if valence property is set, and, if so, if there is a
			// valence error.  If there is, turn valence property off.
			if (atom.hasValenceError() && atom.getValenceProp() != -1) {
				debugPrint(SELF + "atom ", atom, atomNum + 1, 
						" has valence error and valence property set;"
						+ " removing valence property.");
				atom.setValenceProp(-1);
			} // if there's a valence error
		} // for each atom
		debugPrint(SELF + "After setting charges and radicals: ");
		printAtoms();
		printBonds();
		debugPrintMRV(molecule);
	} // setChargesAndRadicals()

	/** Looks for atoms that lost implicit H atoms, and adds explicit H atoms to
	 * replace them; AND adds explicit H atoms to heteroatoms if there's a 
	 * half-integral bond (heteroatoms and terminal C atoms will not show 
	 * H atoms in SMARTS); AND checks for atoms with too many outer-shell 
	 * electrons; AND identifies the part of the molecule ready for 2D clean.
	 * @param	hasOddElectronBond	whether a bond in the product has 
	 * been calculated to have an odd number of electrons
	 * @return	list of atoms that have too many electrons
	 */
	private List<MolAtom> resetHCounts(boolean hasOddElectronBond) {
		final String SELF = "MechSolver.resetHCounts: ";
		final List<MolAtom> tooManyElectronAtoms = new ArrayList<MolAtom>();
		molecule.valenceCheck(); // recalculates implicit H counts
		final int numAtoms = molecule.getAtomCount();
		for (int atomNum = 0; atomNum < numAtoms; atomNum++) {
			final MolAtom atom = molecule.getAtom(atomNum);
			if (isMulticenterAtom(atom)) continue;
			final int totalElecs = getElectrons(atom)
					+ atom.twicesumbonds(INCLUDE_EXPLICIT_H, USE_BOND_ORDERS)
					+ initImplicitHcounts[atomNum] * 2;
			final int maxElecs = ChemUtils.getMaxOuterElectrons(atom);
			if (totalElecs > maxElecs) {
				debugPrint(SELF, atom, atomNum + 1, " now has ", totalElecs, 
						" total valence electrons, more than the maximum ", 
						maxElecs, "; will throw outer-shell-electrons error "
						+ "if not from JSP.");
				tooManyElectronAtoms.add(atom);
			} // if atom has more electrons than the maximum
			final int implicitHAtomCt = atom.getImplicitHcount();
			if (implicitHAtomCt < initImplicitHcounts[atomNum]
					|| (hasOddElectronBond
						&& atom.getAtno() != PeriodicSystem.C)) {
				// add explicit H atoms to replace lost ones or ones that won't
				// show in SMARTS
				debugPrint(SELF, atom, atomNum + 1, " began with ", 
						initImplicitHcounts[atomNum], " implicit H atoms " 
						+ "and now has ", implicitHAtomCt, 
						"; adding explicit H atoms.");
				addHAtoms(atom, initImplicitHcounts[atomNum]);
			} // if need to add explicit H atoms
		} // for each atom index
		final int numBondsToNewH = bondsToNewH.size();
		if (numBondsToNewH == 0) debugPrintMRV(SELF + "no new bonds to H.");
		else debugPrintMRV(SELF + "gained ", numBondsToNewH, " new bond", 
				numBondsToNewH == 1 ? "" : "s", " to H:\n", molecule);
		return tooManyElectronAtoms;
	} // resetHCounts(boolean)

	/** Ungroups multiatom groups whose attachment points have been disconnected 
	 * from the atoms to which they were bonded. */
	private void ungroupUnbondedGroups() {
		final int numAttachPts = multicenterAttachPts.size();
		for (int mcNum = numAttachPts - 1; mcNum >=0; mcNum--) {
			final MolAtom attachPt = multicenterAttachPts.get(mcNum);
			if (attachPt.getBondCount() == 0) {
				molecule.ungroupSgroup(getMultiatomGroup(attachPt));
			} // if the attachment point no longer has any bonds
		} // for each multicenter attachment point
	} // ungroupUnbondedGroups()

	/** Readies the product for display in the mechanism calculator JSP page.
	 * @param	hasOddElectronBond	whether a bond in the product has 
	 * been calculated to have an odd number of electrons
	 */
	private void cleanCalculatedProduct(boolean hasOddElectronBond) {
		final String SELF = "MechSolver.cleanCalculatedProduct: ";
		// when query bonds present, radical flags lost upon 2D clean
		final List<int[]> radFlags = new ArrayList<int[]>();
		for (int atNum = 0; atNum < molecule.getAtomCount(); atNum++) {
			final MolAtom atom = molecule.getAtom(atNum);
			clearElectrons(atom);
			if (hasOddElectronBond && !isMulticenterAtom(atom)) {
				final int rad = atom.getRadicalValue().getIntValue();
				if (rad != 0) {
					radFlags.add(new int[] {atNum, rad});
				} // if the atom has a radical flag set
			} // for each atom
		} // if there are half-integral bonds
		crissCrossToWavy(molecule);
		for (final int[] atomRad : radFlags) {
			final MolAtom atom = molecule.getAtom(atomRad[0]);
			atom.setRadicalValue(Radical.forIntValue(atomRad[1]));
		} // for each radical
		cleanNewBondsToH();
		if (!removedBondAtoms.isEmpty()) {
			separateFrags();
		} // if broke single bonds
		debugPrintMRV(SELF + "after 2D cleaning:\n", molecule);
	} // cleanCalculatedProduct(boolean)

	/** Checks for half-integral bond, valence, outer-shell electrons errors. 
	 * @param	oddElectronBonds	bonds in the product that have been
	 * calculated to have an odd number of electrons
	 * @param	tooManyElectronAtoms	atoms whose calculated number of
	 * electrons exceeds the maximum
	 * @throws	MechError	if there's an odd-electron bond, an atom with too 
	 * many electrons, or a valence error
	 */
	private void lookForInvalidProducts(List<MolBond> oddElectronBonds, 
			List<MolAtom> tooManyElectronAtoms) throws MechError {
		final String SELF = "MechSolver.lookForInvalidProducts: ";
		debugPrintMRV(SELF + "entering with:\n", molecule);
		if (!oddElectronBonds.isEmpty()) {
			crissCrossToWavy(molecule);
			final List<Integer> atomIndices = new ArrayList<Integer>();
			for (final MolBond oddElectronBond : oddElectronBonds) {
				MolAtom atom = oddElectronBond.getAtom1();
				atomIndices.add((Integer) atom.getProperty(ORIG_INDEX));
				atom = oddElectronBond.getAtom2();
				atomIndices.add((Integer) atom.getProperty(ORIG_INDEX));
			} // for each odd-electron bond
			final String smarts = MolString.toString(molecule, SMARTS);
			debugPrint(SELF + "obtained product ", smarts, 
					" with one or more half-integral bonds "
					+ "due to atoms with 0-based indices ", atomIndices,
					"; throwing MechError.");
			throw new MechError("half-integral bond", 
					ODD_ELECTRON_BOND, smarts, 
					Utils.listToIntArray(atomIndices));
		} else debugPrint(SELF + "calculated product ", 
				molecule, " has no half-integral bonds.");
		if (!tooManyElectronAtoms.isEmpty()) {
			crissCrossToWavy(molecule);
			final List<Integer> atomIndices = new ArrayList<Integer>();
			for (final MolAtom atom : tooManyElectronAtoms) {
				atomIndices.add((Integer) atom.getProperty(ORIG_INDEX));
			} // for each atom with too many electrons
			final String smarts = MolString.toString(molecule, SMARTS);
			debugPrint(SELF + "obtained product ",
					smarts, " with outer-shell-electrons error "
					+ "due to atoms with 0-based indices ", atomIndices,
					"; throwing MechError.");
			throw new MechError("outer-shell-electrons error",
					TOO_MANY_OUTER_ELECTRONS, smarts, 
					Utils.listToIntArray(atomIndices));
		} else debugPrint(SELF + "calculated product ", 
				molecule, " has no atom with too many electrons "
				+ "in its outer shell.");
		try { 
			ChemUtils.checkValence(molecule);
			debugPrint(SELF + "calculated product ", molecule, 
					" has no valence error.");
		} catch (ValenceException e) {
			crissCrossToWavy(molecule);
			final List<Integer> atomIndices = new ArrayList<Integer>();
			for (final int badAtomNum : e.getBadAtomNums()) { 
				final MolAtom atom = molecule.getAtom(badAtomNum - 1);
				atomIndices.add((Integer) atom.getProperty(ORIG_INDEX));
			} // for each atom with a valence error
			final String smarts = MolString.toString(molecule, SMARTS);
			debugPrint(SELF + "obtained product ",
					smarts, " with valence error; throwing MechError.");
			throw new MechError("valence error", VALENCE_ERROR, smarts,
					Utils.listToIntArray(atomIndices));
		} // try valence check
	} // lookForInvalidProducts(List<MolBond>, List<MolAtom>)

/* ******************* Utility methods ***************************/

	/** Gets whether an atom is in an array.
	 * @param	testAtom	the atom
	 * @param	arrAtoms	an array of atoms
	 * @return	true if the atom is in the array
	 */
	private boolean atomAmong(MolAtom testAtom, MolAtom[] arrAtoms) {
		boolean among = false;
		for (final MolAtom arrAtom : arrAtoms) {
			among = arrAtom == testAtom; // pointer equality
			if (among) break;
		} // for each atom in the test array
		return among;
	} // atomAmong(MolAtom, MolAtom[])

	/** Calculates the total number of electrons in an atom's bonds.
	 * @param	atom	an atom
	 * @param	atomNum	0-based index of the atom
	 * @return	twice the sum of the bond orders (corrected for coordinate
	 * bonds)
	 */
	private int getSumBondElecCts(MolAtom atom, int atomNum) {
		final String SELF = "MechSolver.getSumBondElecCts: ";
		int sumBondElecCts = initImplicitHcounts[atomNum] * 2;
		for (final MolBond bondToLig : atom.getBondArray()) {
			final int bondElecs = (bondToLig.getType() == MolBond.COORDINATE
					? 0 : bondElecCts.get(bondToLig).intValue());
			debugPrint(SELF, "bond with index ", 
					molecule.indexOf(bondToLig) + 1, " contributes ", 
					bondElecs, " electron", bondElecs == 1 ? "" : "s", 
					" to the total electron count of ", atom, atomNum + 1);
			sumBondElecCts += bondElecs;
		} // for each bond of the atom
		return sumBondElecCts;
	} // getSumBondElecCts(MolAtom, int)

	/** Adds all atoms connected to the given one to a list.  Called by 
	 * removeUntouchedMolecules() and recursively.
	 * @param	atom	the given atom
	 * @param	atomsInTouchedMols	list of atoms in molecules touched by
	 * electron-flow arrows
	 */
	private void addAtomsInTouchedMols(MolAtom atom, 
			List<MolAtom> atomsInTouchedMols) {
		final String SELF = "MechSolver.addAtomsInTouchedMols: ";
		if (!atomsInTouchedMols.contains(atom)) { // pointer equality
			debugPrint(SELF + "adding atom ", atom, molecule.indexOf(atom) + 1);
			atomsInTouchedMols.add(atom);
			if (isMulticenterAtom(atom)) {
				final MolAtom[] groupAtoms = getAtomsOfGroup(atom);
				debugPrint(SELF + "adding first atom of multiatom group "
						+ "represented by multicenter attachment point ", 
						molecule.indexOf(atom) + 1, " to list of atoms "
						+ "touched by the e-flow arrow.");
				addAtomsInTouchedMols(groupAtoms[0], atomsInTouchedMols);
			} // if is multicenter attachment point
			for (final MolAtom ligand : atom.getLigands()) {
				addAtomsInTouchedMols(ligand, atomsInTouchedMols);
			} // for each atom attached to atom
		} // if the atom is not already in the list
	} // addAtomsInTouchedMols(MolAtom, List<MolAtom>)

	/** Gets whether an atom with an odd number of unshared electrons and an 
	 * odd number of bond electrons has an aromatic bond to another, and both 
	 * are in a multiatom group with an odd number of atoms.  If so, stores the 
	 * atom for future reference.  
	 * @param	atom	the given atom
	 * @return	true if the atom has an aromatic bond to another, and both are
	 * in an odd-atom multiatom group
	 */
	private boolean inOddAtomGroupWithAromBond(MolAtom atom) {
		boolean inOddAtomGroupWithAromBond = false;
		for (final MolBond bond : atom.getBondArray()) {
			if (bond.getType() == MolBond.AROMATIC && inOddAtomGroup(bond)) {
				atomsInOddAtomGroupsHavingOddAromBonds.add(atom);
				inOddAtomGroupWithAromBond = true;
				break;
			} // if found aromatic bond
		} // for each bond of the atom
		return inOddAtomGroupWithAromBond;
	} // inOddAtomGroupWithAromBond(MolAtom)

	/** Gets whether the bond is in an odd-atom multiatom group.
	 * @param	bond	a bond
	 * @return	true if the bond is in an odd-atom multiatom group
	 */
	private boolean inOddAtomGroup(MolBond bond) {
		boolean inOddAtomGroup = false;
		for (final MolAtom multicenterAtom : multicenterAttachPts) {
			final Sgroup multiatomGroup = getMultiatomGroup(multicenterAtom);
			if (!isEvenAtomGroup(multiatomGroup)
					&& multiatomGroup.indexOf(bond) >= 0) {
				inOddAtomGroup = true;
				break;
			} // if is odd-atom group and contains bond
		} // for each multiatom group
		return inOddAtomGroup;
	} // inOddAtomGroup(MolBond)

	/** After electrons are moved, sets new type of bond or removes bond if 
	 * order 0.  Also erases stereochemistry of double bonds whose atoms are 
	 * involved in changing bonds.
	 * @param	bond	the bond whose order to change
	 * @param	bondNum	0-based index of the bond
	 * @param	newBondElecCt	number of electrons now in the bond
	 * @param	bondIsNew	whether the bond is a new one
	 * @param	fromJSP	whether the reactants come from the mechanism 
	 * calculator JSP page
	 */
	private void setBondType(MolBond bond, int bondNum, int newBondElecCt, 
			boolean bondIsNew, boolean fromJSP) {
		final String SELF = "MechSolver.setBondType: ";
		// new bond order or newly formed bond (order won't differ)
		final MolAtom[] atoms = new MolAtom[]
				{bond.getAtom1(), bond.getAtom2()};
		adjustDoubleBondStereo(bond, atoms, bondNum);
		if (newBondElecCt != 0) { // changing bond retains at least sigma bond
			final int CAtype = getChemAxonBondConstant(newBondElecCt);
			debugPrint(SELF + "setting bond ", bondNum + 1, 
					" to ChemAxon type ", CAtype);
			bond.setType(CAtype);
			if (bondIsNew && newBondElecCt == 2) {
				final boolean atom0IsMulticenter = isMulticenterAtom(atoms[0]);
				final boolean atom1IsMulticenter = isMulticenterAtom(atoms[1]);
				if (atom0IsMulticenter || atom1IsMulticenter) {
					debugPrint(SELF + "Making the new bond coordinate");
					bond.setType(MolBond.COORDINATE);
					if (atom1IsMulticenter) bond.swap();
				} // if new bond involves a multiatom group
			} // if bond is newly formed
			// if new bond is double and not part of an allene, set to 
			// unspecified geometry (would be 
			// better to find out first if it's stereogenic, but changing 
			// to wavy unnecessarily oughtn't hurt)
			if (newBondElecCt == 4 && !inCumulene(bond, atoms)) {
				debugPrint(SELF + "setting new ", atoms[0], 
						molecule.indexOf(atoms[0]) + 1, "=", atoms[1], 
						molecule.indexOf(atoms[1]) + 1, " double bond ", 
						bondNum + 1, " to C/T unspecified");
				StereoFunctions.setBondStereoFlags(bond, CRISSCROSS);
			} // if new bond is double
		} else {   // sigma bond has been broken
			debugPrint(SELF + "removing bond ", bondNum + 1);
			removedBondAtoms.add(atoms);
			/* unused
			final MolAtom mcAtom = 
					(isMulticenterAtom(bond.getAtom1()) ? bond.getAtom1()
					: isMulticenterAtom(bond.getAtom2()) ? bond.getAtom2()
					: null);
			unused */
			molecule.removeBond(bondNum);
			bondElecCts.remove(bond);
		} // if newBondElecCt != 0
	} // setBondType(MolBond, int, int, boolean, boolean)

	/** If an atom in the changing bond is participating in an unchanging 
	 * double bond, and each atom of the double bond has one or two 
	 * substituents, changes the stereo of that double bond to cis-trans 
	 * unspecified.  Would be better to find out first if it's stereogenic, 
	 * but changing to wavy unnecessarily won't hurt.
	 * @param	bond	the bond whose order to change
	 * @param	bondAtoms	atoms of the bond
	 * @param	bondNum	0-based index of the bond
	 */
	private void adjustDoubleBondStereo(MolBond bond, MolAtom[] bondAtoms, 
			int bondNum) {
		final String SELF = "MechSolver.adjustDoubleBondStereo: ";
		final int[] TWO_TO_THREE = new int[] {2, 3}; 
		for (final MolAtom bondAtom : bondAtoms) {
			final int numAtomBonds = bondAtom.getBondCount();
			if (MathUtils.inRange(numAtomBonds, TWO_TO_THREE)) { 
				for (final MolAtom ligand : bondAtom.getLigands()) {
					final MolBond adjacentBond = bondAtom.getBondTo(ligand);
					final int numLigBonds = ligand.getBondCount();
					if (getNumBondElecs(adjacentBond) == 4 
							&& adjacentBond != bond // pointer equality
							&& !inCumulene(adjacentBond)
							&& MathUtils.inRange(numLigBonds, TWO_TO_THREE)) {
									// bond count includes double bond
						final int bondAtomNum = molecule.indexOf(bondAtom);
						final int ligandNum = molecule.indexOf(ligand);
						final int adjBondNum = molecule.indexOf(adjacentBond);
						debugPrint(SELF + "atom ", bondAtom, bondAtomNum + 1, 
								" in changing bond ", bondNum + 1, " enjoys "
								+ "double bond ", adjBondNum + 1, " to atom ", 
								ligand, ligandNum + 1, "; stereochem of "
								+ "double bond set to unspecified");
						StereoFunctions.setBondStereoFlags(adjacentBond, 
								CRISSCROSS);
					} // if atom in changing bond has unchanging double bond
				} // for each attached atom
			} // if atom has double bond
		} // for each atom in changing bond
	} // adjustDoubleBondStereo(MolBond, MolAtom[], int)

	/** Determines whether a double bond is part of a cumulene.
	 * @param	bond	the bond
	 * @return	true if it is part of a cumulene
	 */
	private boolean inCumulene(MolBond bond) {
		return inCumulene(bond, new MolAtom[] {bond.getAtom1(), bond.getAtom2()});
	} // inCumulene(MolBond)

	/** Determines whether a double bond is part of a cumulene.
	 * @param	bond	the bond
	 * @param	bondAtoms	atoms of the bond
	 * @return	true if it is part of a cumulene
	 */
	private boolean inCumulene(MolBond bond, MolAtom[] bondAtoms) {
		if (getNumBondElecs(bond) != 4) return false;
		for (final MolAtom bondAtom : bondAtoms) {
			for (final MolAtom ligand : bondAtom.getLigands()) {
				final MolBond bondAtomLigand = bondAtom.getBondTo(ligand);
				if (bondAtomLigand != bond  // pointer equality
						&& getNumBondElecs(bondAtomLigand) == 4) {
					return true;
				} // if bond atom has another double bond
			} // for each atom attached to the bond atom
		} // for each atom of the bond
		return false;
	} // inCumulene(MolBond, MolAtom[])

	/** Calculates and sets the charge of an atom after electrons are moved.
	 * @param	atom	an atom
	 * @param	atomNum	0-based index of the atom
	 * @param	sumBondElecCts	twice the sum of the bond orders
	 * @param	chargeFlag	used to set charge when an atom has an odd-electron
	 * bond
	 * @return	true if the atom has a half-integral bond
	 */
	private boolean setAtomCharge(MolAtom atom, int atomNum, 
			int sumBondElecCts, int chargeFlag) {
		final String SELF = "MechSolver.setAtomCharge: ";
		boolean atomHasOddElectronBond = false;
		final int unshared = getElectrons(atom);
		if (isMulticenterAtom(atom)) {
			final boolean isOddAtomGroup = !isEvenAtomGroup(atom);
			if (unshared != 0 || (isOddAtomGroup && atom.getBondCount() == 0)) {
				debugPrint(SELF + "atom ", atomNum + 1, 
						" is multicenter attachment point with ",
						unshared < 0 ? Utils.getBuilder("a deficiency of ", 
							unshared, " electron(s) to remove from atoms of")
						: unshared == 0 && isOddAtomGroup
						? "a single electron to place on an atom of"
						: Utils.getBuilder(unshared,
							" unshared electron(s) to return to",
						" the multiatom group."));
				changeLigandElectrons(atom, unshared);
			} // if multicenter attachment point has unshared electrons
		} else if (sumBondElecCts % 2 == 0) {
			// sum of bond orders is integral -- usually the case
			final int valElecs = getValenceElectrons(atom);
			final int sumBondOrders = sumBondElecCts / 2;
			final int newCharge = valElecs - sumBondOrders - unshared;
			debugPrint(SELF + "atom ", atom, atomNum + 1, 
					" with current charge ", atom.getCharge(),
					" has valElecs = ", valElecs, ", sumBondOrders = ", 
					sumBondOrders, ", unsharedElecs = ", unshared,
					", valElecs - sumBondOrders - unshared (new charge) = ",
					newCharge);
			if (atom.getCharge() != newCharge) atom.setCharge(newCharge);
		} else { // sum of bond orders is half-integral (usu. user error)
			// Truncate half-integral bond order to integer, 
			// alternately add 0 or -1 to charges of atoms
			// of half-integral bonds to maintain total charge 
			atom.setCharge(getValenceElectrons(atom) 
					- (sumBondElecCts / 2) - unshared - chargeFlag); 
			debugPrint(SELF + "atom ", atom, atomNum + 1, 
					" has half-integral sum of bond orders; " 
					+ "setting charge to ", atom.getCharge());
			atomHasOddElectronBond = true;
		} // end set charge
		return atomHasOddElectronBond;
	} // setAtomCharge(MolAtom, int, int, int)

	/** When a bond to a multicenter attachment point is broken, adjusts
	 * the ligand atoms accordingly.  Even-atom ligands have alternating 
	 * single and multiple bonds or continuous aromatic bonds and can lose one 
	 * or two electrons.  Odd-atom ligands have continuous aromatic bonds and 
	 * can become an anion, cation, or radical, which requires converting the 
	 * aromatic bonds into alternating single and double bonds.
	 * @param	mcAtom	a multicenter attachment point
	 * @param	origUnshared	number of electrons of the multicenter 
	 * attachment point: -2 or -1 for even-atom ligands, and -2, 0, or +2 for
	 * odd-atom ligands
	 */
	private void changeLigandElectrons(MolAtom mcAtom, int origUnshared) {
		final String SELF = "MechSolver.changeLigandElectrons: ";
		final Sgroup multiatomGroup = getMultiatomGroup(mcAtom);
		MolAtom firstGroupAtom = getGroupTerminus(multiatomGroup);
		if (firstGroupAtom == null) firstGroupAtom = multiatomGroup.getAtom(0);
		debugPrint(SELF + "terminus of multiatom group of attachment point ",
				mcAtom, molecule.indexOf(mcAtom) + 1, " is ", firstGroupAtom,
				molecule.indexOf(firstGroupAtom) + 1);
		int unshared = origUnshared;
		final boolean wasEvenAtomGroup = isEvenAtomGroup(multiatomGroup);
		if (wasEvenAtomGroup) dearomatize(multiatomGroup);
		final MolAtom[] groupAtoms = new MolAtom[] {firstGroupAtom, null};
		final List<MolAtom> foundAtoms = new ArrayList<MolAtom>(); // avoid infinite loops
		while (true) {
			boolean foundNextGroupAtom = false;
			final int[] groupAtomNums = 
					new int[] {molecule.indexOf(groupAtoms[0]), -1};
			debugPrint(SELF + "looking for next atom in multiatom group "
					+ "attached to ", groupAtoms[0], groupAtomNums[0] + 1);
			for (final MolAtom groupAtomLig : groupAtoms[0].getLigands()) {
				if (multiatomGroup.hasAtom(groupAtomLig)
						&& !foundAtoms.contains(groupAtomLig)) { // pointer equality
					groupAtoms[1] = groupAtomLig;
					foundAtoms.add(groupAtoms[1]);
					final MolBond groupBond = 
							groupAtoms[0].getBondTo(groupAtoms[1]);
					int numBondElecs = getNumBondElecs(groupBond);
					if (numBondElecs <= 2) break;
					groupAtomNums[1] = molecule.indexOf(groupAtoms[1]);
					debugPrint(SELF + "next atom in multiatom group attached "
							+ "to ", groupAtoms[0], groupAtomNums[0] + 1, 
							" is ", groupAtoms[1], groupAtomNums[1] + 1, 
							"; bond between them has ", numBondElecs, 
							" electron(s); attachment point has ", unshared == 0 
								? 1 : unshared, " electron(s).");
					final boolean bondWasAromatic = numBondElecs == 3;
					// treat both atoms of the bond in turn
					for (int grpAtNum = 0; grpAtNum < 2; grpAtNum++) {
						final MolAtom groupAtom = groupAtoms[grpAtNum];
						final int groupAtomNum = groupAtomNums[grpAtNum];
						final int elecChange = MathUtils.sign(unshared);
						setNumBondElecs(groupBond, --numBondElecs);
						// we change electron count to elecChange + 1 because
						// the atom already owns the electrons that were in the
						// bond from the ligand that is now being broken
						changeElectronCount(groupAtom, elecChange + 1);
						changeElectronCount(mcAtom, -elecChange);
						aromaticBondsToSingle(groupAtom);
						final int grpAtomBondsElecCt = 
								getSumBondElecCts(groupAtom, groupAtomNum);
						setAtomCharge(groupAtom, groupAtomNum, 
								grpAtomBondsElecCt, 0); // sort-of-recursive call
						setRadicalState(groupAtom, groupAtomNum, 
								grpAtomBondsElecCt);
						multiatomGroup.removeAtom(groupAtom);
						atomsInOddAtomGroupsHavingOddAromBonds.remove(
								groupAtom);
						unshared -= elecChange;
						if (numBondElecs <= 2 
								|| (unshared == 0 && bondWasAromatic)) break;
					} // for each atom of groupBond
					debugPrint(SELF + "bond from ", groupAtoms[0], 
							groupAtomNums[0] + 1, " to ", groupAtoms[1], 
							groupAtomNums[1] + 1, " now has ", 
							getNumBondElecs(groupBond), " electron(s).");
					int atNum = 1;
					for (final MolAtom bondAtom : groupAtoms) {
						final int atomElecs = getElectrons(bondAtom);
						final int atomChg = bondAtom.getCharge();
						final Radical atomRad = bondAtom.getRadicalValue();
						debugPrint("   bond atom ", atNum++, " has ", 
								atomElecs, " unshared electron",
								atomElecs == 1 ? "" : "s", ", charge ", 
								atomChg > 0 ? "+" : "", atomChg, 
								", and radical state ", atomRad, '.'); 
					} // for each bond atom
					if (numBondElecs <= 2 || unshared == 0) break;
					// move to next atom in group
					groupAtoms[0] = groupAtoms[1]; 
					foundNextGroupAtom = groupAtoms[0] != firstGroupAtom; // pointer equality
					break;
				} // if the ligand is also in the group
			} // for each ligand to the group atom
			if (!foundNextGroupAtom) break;
		} // while true
		if (!wasEvenAtomGroup && isEvenAtomGroup(multiatomGroup)) {
			debugPrint(SELF + "multiatom group with attachment point ", mcAtom,
					molecule.indexOf(mcAtom) + 1, " has gone from odd- to "
					+ "even-atom; need to convert aromatic bonds to "
					+ "alternating single and double bonds.");
			aromaticBondsToAlternating(multiatomGroup);
		} // if multiatom group now has an even number of atoms
		debugPrintMRV(SELF + "after changing bond orders and charges in "
				+ "the multiatom group:\n", molecule);
	} // changeLigandElectrons(MolAtom, int)

	/** Dearomatizes a cyclic, even-atom multiatom group.
	 * @param	multiatomGroup	a multiatom group
	 */
	private void dearomatize(Sgroup multiatomGroup) {
		final String SELF = "MechSolver.dearomatize: ";
		final SelectionMolecule selectedGroup = new SelectionMolecule();
		final boolean ADD_BONDS = true;
		for (final MolAtom groupAtom : multiatomGroup.getAtomArray()) {
			selectedGroup.add(groupAtom, ADD_BONDS);
		} // for each atom in the multiatom group
		/* for (final MolBond bond : molecule.getBondArray()) {
			if (multiatomGroup.indexOf(bond) >= 0) {
				selectedGroup.add(bond);
			} // if the bond is in the multiatom group
		} // for each bond in the molecule */
		selectedGroup.dearomatize();
		for (final MolBond bond : selectedGroup.getBondArray()) {
			final int numElecs = getNumBondElecs(bond);
			bondElecCts.put(bond, Integer.valueOf(numElecs));
			debugPrint(SELF + "storing ", numElecs, " electron(s) for bond "
					+ "with index ", molecule.indexOf(bond) + 1);
		} // for each selected bond
		debugPrintMRV(SELF + "after dearomatizing the multiatom group "
				+ "whose attachment point's bond has broken:\n", molecule);
	} // dearomatize(Sgroup)

	/** Converts all of the aromatic bonds of this atom to single bonds.
	 * @param	grpAtom	an atom in a multiatom group
	 */
	private void aromaticBondsToSingle(MolAtom grpAtom) {
		// final int numBonds = grpAtom.getBondCount();
		for (final MolBond grpAtomBond : grpAtom.getBondArray()) {
			if (grpAtomBond.getType() == MolBond.AROMATIC) {
				setNumBondElecs(grpAtomBond, 2);
			} // if grpAtom is aromatically bound to another atom
		} // for each bond of ligand atom
	} // aromaticBondsToSingle(MolAtom)

	/** Converts the aromatic bonds in a formerly odd-atom and now even-atom
	 * multiatom group into alternating single and double bonds.
	 * @param	multiatomGroup	a multiatom group, altered by this method
	 */
	private void aromaticBondsToAlternating(Sgroup multiatomGroup) {
		final String SELF = "MechSolver.aromaticBondsToAlternating: ";
		while (true) {
			MolAtom groupAtom = getGroupTerminus(multiatomGroup);
			if (groupAtom == null) break;
			boolean makeDouble = true;
			final int groupAtomNum = molecule.indexOf(groupAtom);
			debugPrint(SELF, groupAtom, groupAtomNum + 1,
					" is at terminus of multiatom group.");
			while (true) {
				boolean foundNextGroupAtom = false;
				for (final MolAtom grpAtomLig : groupAtom.getLigands()) {
					if (multiatomGroup.indexOf(grpAtomLig) >= 0) {
						final MolBond groupBond = 
								groupAtom.getBondTo(grpAtomLig);
						if (groupBond.getType() == MolBond.AROMATIC) {
							debugPrint(SELF, "converting ", groupAtom, 
									groupAtomNum + 1, "-", grpAtomLig,
									molecule.indexOf(grpAtomLig) + 1,
									" group bond from aromatic to ",
									makeDouble ? "double" : "single");
							setNumBondElecs(groupBond, makeDouble ? 4 : 2);
							makeDouble = !makeDouble;
							multiatomGroup.removeAtom(groupAtom);
							atomsInOddAtomGroupsHavingOddAromBonds
									.remove(groupAtom);
							// move to next atom in group
							groupAtom = grpAtomLig; 
							foundNextGroupAtom = true;
							break;
						} // if bond between group atoms is aromatic
					} // if the ligand is also in the group
				} // for each ligand to the group atom
				if (!foundNextGroupAtom) break;
			} // while true
		} // while true
	} // aromaticBondsToAlternating(Sgroup)

	/** Gets an atom of the multiatom group that is bound to only one other atom
	 * in the multiatom group, preferentially the one that is touched by an
	 * electron-flow arrow, or null if the multiatom group is cyclic.
	 * @param	multiatomGroup	the multiatom group
	 * @return	a terminal atom of the multiatom group, or null if group is
	 * cyclic
	 */
	private MolAtom getGroupTerminus(Sgroup multiatomGroup) {
		final String SELF = "MechSolver.getGroupTerminus: ";
		final List<MolAtom> termini = new ArrayList<MolAtom>();
		final List<MolBond> allGrpBonds = 
				Arrays.asList(multiatomGroup.getBondArray());
		for (final MolAtom grpAtom : multiatomGroup.getAtomArray()) {
			int numBondsToGrpAtoms = 0;
			for (final MolBond grpAtomBond : grpAtom.getBondArray()) {
				if (allGrpBonds.contains(grpAtomBond)) numBondsToGrpAtoms++;
				if (numBondsToGrpAtoms > 1) break;
			} // for each ligand to the group atom
			if (numBondsToGrpAtoms == 1) termini.add(grpAtom);
		} // for each atom in the group
		final int numTermini = termini.size();
		if (numTermini > 1) {
			// choose terminus touched by eflow arrow if possible
			for (int termNum = numTermini - 1; termNum >= 0; termNum--) {
				if (!allFlowAtoms.contains(termini.get(termNum))) {
					termini.remove(termNum);
				} // if terminus is not touched by electron-flow arrow
				if (termini.size() <= 1) break;
			} // for each atom of terminus
		} // if there is more than one terminus
		return (termini.isEmpty() ? null : termini.get(0));
	} // getGroupTerminus(Sgroup)

	/** Calculates and sets the radical state of an atom.
	 * @param	atom	an atom
	 * @param	atomNum	index of the atom
	 * @param	sumBondElecCts	twice the sum of the bond orders
	 */
	private void setRadicalState(MolAtom atom, int atomNum, 
			int sumBondElecCts) {
		final String SELF = "MechSolver.setRadicalState: ";
		final int atomicNum = atom.getAtno();
		if (PeriodicSystem.isTransitionMetal(atomicNum)
				&& !PeriodicSystem.isAlkaliMetal(atomicNum)
				&& !PeriodicSystem.isAlkalineEarthMetal(atomicNum)) return;
		int maxOuterElecs = ChemUtils.getMaxOuterElectrons(atom);
		if (Utils.among(maxOuterElecs, 12, 14)) maxOuterElecs = 8;
		final int unshared = getElectrons(atom);
		final int elecDeficiency = maxOuterElecs - (sumBondElecCts + unshared);
		final int radState = (elecDeficiency < 0 ? unshared % 2 
				: unshared < elecDeficiency ? unshared 
				: elecDeficiency);
		final Radical ChemAxonRadState = getChemAxonRadicalConstant(radState);
		debugPrint(SELF + "atom ", atom, atomNum + 1, " has ", sumBondElecCts, 
				" electron(s), ", unshared, " unshared electron(s), "
				+ "a maximum of ", maxOuterElecs, " outer electrons, " 
				+ "an electron deficiency of ", elecDeficiency, 
				", and a radical state of ", radState, " corresponding "
				+ "to ChemAxon radical state of ", ChemAxonRadState, ".");
		if (!atom.getRadicalValue().equals(ChemAxonRadState)) {
			atom.setRadicalValue(ChemAxonRadState);
			debugPrint(atom, atomNum + 1, " radical state set to ", radState);
		} // if need to set radical state
	} // setRadicalState(MolAtom, int, int)

	/** Adds explicit H atoms to an atom.
	 * @param	atom	the atom gaining the explicit H atoms
	 * @param	numToAdd	the number of H atoms being added
	 */
	private void addHAtoms(MolAtom atom, int numToAdd) {
		for (int hNum = 0; hNum < numToAdd; hNum++) {
			final MolAtom newH = new MolAtom(PeriodicSystem.H);
			molecule.add(newH);
			final MolBond newBond = new MolBond(atom, newH);
			molecule.add(newBond);
			bondsToNewH.add(newBond);
		} // for each implicit H to add
	} // addHAtoms(MolAtom, int)

	/** Changes the number of electrons on an atom.
	 * @param	atom	the atom
	 * @param	change	change in number of electrons (negative for decrease)
	 */
	private void changeElectronCount(MolAtom atom, int change) {
		final String SELF = "MechSolver.changeElectronCount: ";
		if (change == 0) debugPrint(SELF + "unshared electron count ", 
				getElectrons(atom), " of atom ", atom.getSymbol(), 
				molecule.indexOf(atom) + 1, " will remain unchanged.");
		else {
			final int oldCt = getElectrons(atom);
			setElectrons(atom, oldCt + change);
			debugPrint(SELF + "atom ", atom.getSymbol(), 
					molecule.indexOf(atom) + 1, " changed from ", oldCt, 
					" unshared electron(s) to ", getElectrons(atom), '.');
		} // if change
	} // changeElectronCount(MolAtom, int)

	/** Gets the number of electrons of a bond, returning 2 if it is coordinate.
	 * @param	bond	the bond
	 * @return	the number of electrons in the bond (twice the order)
	 */
	private int getNumBondElecs(MolBond bond) {
		final int type = bond.getType();
		return (type == MolBond.COORDINATE ? 2 
				: type == MolBond.AROMATIC ? 3 
				: type * 2);
	} // getNumBondElecs(MolBond)

	/** Sets the order of a bond to match the given number of electrons, using
	 * AROMATIC for 3 electrons.
	 * @param	bond	the bond
	 * @param	numElecs	the number of electrons in the bond
	 */
	private void setNumBondElecs(MolBond bond, int numElecs) {
		bondElecCts.put(bond, Integer.valueOf(numElecs));
		bond.setType(numElecs == 1 ? MolBond.ANY
				: numElecs == 3 ? MolBond.AROMATIC 
				: numElecs == 5 ? MolBond.DOUBLE_OR_AROMATIC
				: numElecs / 2);
	} // setNumBondElecs(MolBond, int)

	/** Converts twice the bond order to bond type.  Called by
	 * setBondType().
	 * @param	bondElecCt	twice the calculated bond order; should be 
	 * 2, 4, or 6, but may be 1, 3, or 5
	 * @return	the bond order (half the value) for even values;
	 * nonstandard query bond type for odd values
	 */
	private int getChemAxonBondConstant(int bondElecCt) {
		if (bondElecCt % 2 == 0) return (bondElecCt / 2);
		else switch (bondElecCt) {
			case 5: return MolBond.SINGLE_OR_DOUBLE;
			case 3: return MolBond.DOUBLE_OR_AROMATIC;
			case 1: default: return MolBond.ANY;
		}
	} // getChemAxonBondConstant(int)

	/** Gets a ChemAxon radical constant corresponding to a number of 
	 * unshared electrons of an electron-deficient atom.  Called by
	 * setRadicalState().
	 * @param	electrons	number of electrons
	 * @return	a ChemAxon radical constant
	 */
	private Radical getChemAxonRadicalConstant(int electrons) {
		switch (electrons) {
			case 1: case -1: return Radical.MONOVALENT;
			case 2: return Radical.DIVALENT_SINGLET;
			case 3: return Radical.TRIVALENT_DOUBLET;
			default: return Radical.NO_RADICAL;
		} // switch (electrons)
	} // getChemAxonRadicalConstant(int)

	/** Lists all the atoms in the compound and their unshared electrons. */
	private void printAtoms() {
		int atomNum = 0;
		for (final MolAtom atom : molecule.getAtomArray()) {
			debugPrint("     Atom ", atom, ++atomNum, 
					" has unshared electron count ", getElectrons(atom));
		} // for each atom
	} // printAtoms()

	/** Lists all the bonds in the compound and their orders. */
	private void printBonds() {
		int bondNum = 0;
		for (final MolBond bond : molecule.getBondArray()) {
			final MolAtom atom1 = bond.getAtom1();
			final MolAtom atom2 = bond.getAtom2();
			final int atom1Num = molecule.indexOf(atom1) + 1;
			final int atom2Num = molecule.indexOf(atom2) + 1;
			final int numBondElecs = getNumBondElecs(bond);
			debugPrint("     Bond ", ++bondNum, ": ", atom1, atom1Num,
					numBondElecs == 6 ? "#" : numBondElecs == 4 ? "=" 
						: numBondElecs == 2 ? "-" : "_", 
					atom2, atom2Num, " has electron count ", 
					bondElecCts.get(bond));
		} // for each bond
	} // printBonds()

/* ******************* Very short methods *****************/

	/** Converts criss-cross representation of stereorandom double bonds into
	 * wavy-bond-to-ligand representation.  Modifies the original.
	 * @param	mol	a molecule
	 */
	private void crissCrossToWavy(Molecule mol) {
		StereoFunctions.allCrissCrossToWavy(mol);
	} // crissCrossToWavy(Molecule)

	/** Gets the number of valence electrons of the atom.
	 * @param	atom	the atom
	 * @return	the number of valence electrons of the atom
	 */
	private int getValenceElectrons(MolAtom atom) {
		return ChemUtils.getValenceElectrons(atom);
	} // getValenceElectrons(MolAtom)

	/** Gets the number of unshared electrons of the atom.
	 * @param	atom	the atom
	 * @return	the number of unshared electrons of the atom
	 */
	private int getElectrons(MolAtom atom) {
		return ChemUtils.getElectrons(atom);
	} // getElectrons(MolAtom)

	/** Sets the number of unshared electrons of the atom.
	 * @param	atom	the atom
	 * @param	numElecs	the number of unshared electrons
	 */
	private void setElectrons(MolAtom atom, int numElecs) {
		ChemUtils.setElectrons(atom, numElecs);
	} // setElectrons(MolAtom, int)

	/** Clears the number of unshared electrons from the atom.
	 * @param	atom	the atom
	 */
	private void clearElectrons(MolAtom atom) {
		ChemUtils.clearElectrons(atom);
	} // clearElectrons(MolAtom)

	/** Gets if a MolAtom represents the center of a multiatom group, not a real
	 * atom.
	 * @param	atom	the atom
	 * @return	true if the MolAtom represents the center of a multiatom group
	 */
	private boolean isMulticenterAtom(MolAtom atom) {
		return ChemUtils.isMulticenterAtom(atom);
	} // isMulticenterAtom(MolAtom)

	/** Gets the atoms of the multiatom group associated with the multicenter 
	 * attachment "atom".
	 * @param	mcAtom	the "atom" at the center of the multiatom group
	 * @return	the atoms of the group
	 */
	private MolAtom[] getAtomsOfGroup(MolAtom mcAtom) {
		return getAtomsOfGroup(getMultiatomGroup(mcAtom));
	} // getAtomsOfGroup(MolAtom)

	/** Gets the atoms of the multiatom group.
	 * @param	multiatomGroup	the multiatom group
	 * @return	the atoms of the group
	 */
	private MolAtom[] getAtomsOfGroup(Sgroup multiatomGroup) {
		return (multiatomGroup == null ? new MolAtom[0] 
				: multiatomGroup.getAtomArray());
	} // getAtomsOfGroup(Sgroup)

	/** Gets the multiatom Sgroup associated with the multicenter attachment
	 * "atom".
	 * @param	mcAtom	the "atom" at the center of the multiatom group
	 * @return	the multiatom group
	 */
	private Sgroup getMultiatomGroup(MolAtom mcAtom) {
		return molecule.findContainingMulticenterSgroup(mcAtom);
	} // getMultiatomGroup(MolAtom)

	/** Gets whether a multiatom group has an even number of atoms.
	 * @param	mcAtom	the "atom" at the center of the multiatom group
	 * @return	true if the group has an even number of atoms
	 */
	private boolean isEvenAtomGroup(MolAtom mcAtom) {
		return isEvenAtomGroup(getMultiatomGroup(mcAtom));
	} // isEvenAtomGroup(MolAtom)

	/** Gets whether a multiatom group has an even number of atoms.
	 * @param	multiatomGroup	the multiatom group
	 * @return	true if the group has an even number of atoms
	 */
	private boolean isEvenAtomGroup(Sgroup multiatomGroup) {
		return getAtomsOfGroup(multiatomGroup).length % 2 == 0;
	} // isEvenAtomGroup(Sgroup)

/* ******************* Appearance clean-up methods (JSP only) *****************/

	/** Moves any newly added explicit H atoms to appropriate places. */
	private void cleanNewBondsToH() {
		final String SELF = "MechSolver.cleanNewBondsToH: ";
		for (final MolBond bondToNewH : bondsToNewH) {
			final MolAtom atom1 = bondToNewH.getAtom1();
			final MolAtom atom2 = bondToNewH.getAtom2();
			// being cautious here
			final MolAtom nonH = (atom1.getAtno() != PeriodicSystem.H 
					? atom1 : atom2);
			final MolAtom isH = (atom1.getAtno() == PeriodicSystem.H 
					? atom1 : atom2);
			shortenBond(nonH, isH, bondToNewH);
			moveToGap(nonH, isH);
		} // for each new bond to H
	} // cleanNewBondsToH()

	/** Shortens the bond between two atoms.
	 * @param	fixedAtom	the fixed atom
	 * @param	moveAtom	the atom to move
	 * @param	bond	the bond between the atoms
	 */
	private void shortenBond(MolAtom fixedAtom, MolAtom moveAtom, 
			MolBond bond) {
		final String SELF = "MechSolver.shortenBond: ";
		final int order = getNumBondElecs(bond) / 2;
		final int DIMENSION = 2;
		final double bestLen = MolBond.desiredLength(
				fixedAtom.getAtno(), moveAtom.getAtno(), order, DIMENSION);
		final double nowLen = bond.getLength();
		debugPrint(SELF + "correcting length of bond between ",
				fixedAtom, molecule.indexOf(fixedAtom) + 1, " and ",
				moveAtom, molecule.indexOf(moveAtom) + 1, "; bestLen = ",
				bestLen, ", nowLen = ", nowLen);
		final double factor = bestLen / nowLen;
		final DPoint3 toMoveFromFixed = VectorMath.scalarProd(
				VectorMath.getVector(fixedAtom, moveAtom), factor);
		moveAtom.setX(fixedAtom.getX() + toMoveFromFixed.x);
		moveAtom.setY(fixedAtom.getY() + toMoveFromFixed.y);
		debugPrint(SELF, moveAtom, molecule.indexOf(moveAtom) + 1, 
				" is now at [", moveAtom.getX(), ", ", moveAtom.getY(), ']');
	} // shortenBond(MolAtom, MolAtom, MolBond)

	/** Finds if an atom is too close to the other ligands of a fixed atom; if
	 * so, moves it into a bigger gap in the ligand sphere.
	 * @param	fixedAtom	the fixed atom
	 * @param	moveAtom	the atom that may be moved; must be a ligand of 
	 * the fixed atom
	 */
	private void moveToGap(MolAtom fixedAtom, MolAtom moveAtom) {
		final String SELF = "MechSolver.moveToGap: ";
		final double MIN_ANGLE_SEP = toRadians(20); // empirically chosen
		final double[] NEAR_ZERO = new double[] {-MIN_ANGLE_SEP, MIN_ANGLE_SEP};
		boolean tooCloseToLig = false;
		final MolAtom[] ligands = fixedAtom.getLigands();
		final double[] anglesToLigs = new double[ligands.length - 1];
		int angleNum = 0;
		// get all [moveAtom, fixedAtom, ligand] angles; flag if any are near 0
		for (final MolAtom ligand : ligands) {
			if (ligand == moveAtom) continue; // pointer equality
			anglesToLigs[angleNum] = 
					angle(new MolAtom[] {moveAtom, fixedAtom, ligand});
			debugPrint(SELF + "angle between ligand ",
					ligand, molecule.indexOf(ligand) + 1, " of ",
					fixedAtom, molecule.indexOf(fixedAtom) + 1, 
					" and ", moveAtom, molecule.indexOf(moveAtom) + 1, 
					" is ", toDegrees(anglesToLigs[angleNum]),
					" deg (", anglesToLigs[angleNum], " radians)");
			if (MathUtils.inRange(anglesToLigs[angleNum], NEAR_ZERO)) {
				tooCloseToLig = true;
			} // if an angle is too small
			angleNum++;
		} // for each ligand
		if (tooCloseToLig) {
			final double rotation = 
					getRotation(fixedAtom, moveAtom, anglesToLigs);
			debugPrint(SELF + "angles of moveAtom ",
					moveAtom, molecule.indexOf(moveAtom) + 1, 
					" to other ligands of ", fixedAtom, 
					molecule.indexOf(fixedAtom) + 1,
					" are ", toDegrees(anglesToLigs), 
					"; rotating moveAtom by ", toDegrees(rotation),
					" (", rotation, " radians)"); 
			debugPrint(SELF + "before move, atom ", moveAtom, 
					molecule.indexOf(moveAtom) + 1, " is at [", 
					moveAtom.getX(), ", ", moveAtom.getY(), ']');
			rotateAboutAtom(fixedAtom, moveAtom, rotation);
			debugPrint(SELF + "after move, atom ", moveAtom, 
					molecule.indexOf(moveAtom) + 1, " is at [", 
					moveAtom.getX(), ", ", moveAtom.getY(), ']');
		} // if need to change the position of moveAtom
	} // moveToGap(MolAtom, MolAtom)

	/** Gets the optimum angle of rotation to put a ligand of a fixed atom
	 * as far as possible from other atoms in the ligand sphere.
	 * @param	fixedAtom	the fixed atom
	 * @param	moveAtom	the atom being moved
	 * @param	anglesToLigs	array of angles of moveAtom to other ligands of
	 * fixedAtom
	 * @return	the optimal angle, in radians
	 */
	private double getRotation(MolAtom fixedAtom, MolAtom moveAtom, 
			double[] anglesToLigs) {
		final String SELF = "MechSolver.getRotation: ";
		Arrays.sort(anglesToLigs);
		debugPrint(SELF + "sorted anglesToLigs (radians): ", anglesToLigs);
		final int numAngles = anglesToLigs.length;
		int biggestGapIndex = 0;
		double biggestGap = 0;
		for (int angleNum = 0; angleNum < numAngles; angleNum++) {
			final int nextAngleNum = MathUtils.getMod(angleNum + 1, numAngles);
			final double anglesDiff = (nextAngleNum == 0 ? 2 * Math.PI : 0)
					+ anglesToLigs[nextAngleNum] - anglesToLigs[angleNum];
			if (anglesDiff > biggestGap) {
				biggestGap = anglesDiff;
				biggestGapIndex = angleNum;
			} // if we found a bigger gap
		} // for each angle
		final int biggestGapNext = 
				MathUtils.getMod(biggestGapIndex + 1, numAngles);
		final double angle1 = anglesToLigs[biggestGapIndex];
		final double angle2 = anglesToLigs[biggestGapNext];
		final double rotation = (angle1 + angle2) / 2
				- (biggestGapNext == 0 ? Math.PI : 0);
		debugPrint(SELF + "angle1: ", toDegrees(angle1), " (", angle1, " radians), "
				+ "angle2: ", toDegrees(angle2), " (", angle2, " radians), "
				+ "rotation: ", toDegrees(rotation), " (", rotation, " radians).");
		return (Double.isNaN(rotation) ? Math.PI / 2 : rotation);
	} // getRotation(MolAtom, MolAtom, double[])

	/** Rotates one atom about another in the XY plane.
	 * @param	originAtom	the atom about which the rotation is occurring
	 * @param	moveAtom	the atom being moved
	 * @param	rotation	the angle of rotation (in radians)
	 */
	public void rotateAboutAtom(MolAtom originAtom, MolAtom moveAtom,
			double rotation) {
		final String SELF = "MechSolver.rotateAboutAtom: ";
		final DPoint3 originLocn = originAtom.getLocation();
		final DPoint3 startLocn = moveAtom.getLocation();
		final DPoint3 originToStart = VectorMath.diff(startLocn, originLocn);
		final DPoint3 originToNew = 
				VectorMath.rotateVector(originToStart, rotation);
		final DPoint3 newLocn = VectorMath.sum(originLocn, originToNew);
		moveAtom.setLocation(newLocn);
		debugPrint(SELF + 
				"\n   rotation = ", toDegrees(rotation), 
					" deg (", rotation, "radians)",
				"\n   originLocn = ", originLocn,
				"\n   startLocn = ", startLocn,
				"\n   originToStart = ", originToStart,
				"\n   originToNew = ", originToNew,
				"\n   newLocn = ", newLocn);
	} // rotateAboutAtom(MolAtom, MolAtom, double)

	/** Moves atoms or fragments from broken bonds away from each other. */
	private void separateFrags() {
		final String SELF = "MechSolver.separateFrags: ";
		final SelectionMolecule[] molFrags = molecule.findFrags();
		for (int bondNum = 0; bondNum < removedBondAtoms.size(); bondNum++) {
			final MolAtom[] atoms = removedBondAtoms.get(bondNum);
			// find fragments containing atoms of broken bond
			SelectionMolecule molFrag1 = null;
			SelectionMolecule molFrag2 = null;
			for (final SelectionMolecule frag : molFrags) {
				if (molFrag1 == null && frag.contains(atoms[0])) 
					molFrag1 = frag;
				if (molFrag2 == null && frag.contains(atoms[1])) 
					molFrag2 = frag;
				if (molFrag1 != null && molFrag2 != null) break;
			} // next fragNum
			final double distX = atoms[1].getX() - atoms[0].getX();
			final double distY = atoms[1].getY() - atoms[0].getY();
			final double angleMove = 
					(distX < 0 ? Math.atan(distY / distX) + Math.PI
					: distX == 0 ? MathUtils.sign(distY) * Math.PI / 2
					: Math.atan(distY / distX));
			// Hypotenuse is 4 Angstrom - ca. 1.5 already
			// separated; each atom moved half that distance
			final double moveX = Math.cos(angleMove) * 1.25;
			final double moveY = Math.sin(angleMove) * 1.25; 
			debugPrint(SELF + "angleMove = ", toDegrees(angleMove), 
					" degrees, moveX = ", moveX, " Angstroms, moveY = ", 
					moveY, " Angstroms."); 
			if (molFrag1 != molFrag2) { // pointer equality
				debugPrint(SELF + "the two atoms of the broken "
						+ "bond are in different fragments.");
				// move all the atoms in each fragment
				if (molFrag1 != null) {
					for (final MolAtom atom : molFrag1.getAtomArray()) {
						moveAtom(atom, molecule.indexOf(atom),
								moveX * -1.0, moveY * -1.0);
					} // for each atom in fragment 1
				} // if fragment 1 is not null
				if (molFrag2 != null) {
					for (final MolAtom atom : molFrag2.getAtomArray()) {
						moveAtom(atom, molecule.indexOf(atom), moveX, moveY);
					} // for each atom in fragment 2
				} // if fragment 2 is not null
				// move any shortcut groups in the fragments as well
				for (final Sgroup sGroup : molecule.getSgroupArray()) {
					if (sGroup.getSgroupType().equals(SgroupType.SUPERATOM)) {
						final SuperatomSgroup atomGroup = 
								(SuperatomSgroup) sGroup;
						final SgroupAtom superAtom = atomGroup.getSuperAtom();
						final MolAtom attachPt = atomGroup.findAttachAtom();
						if (attachPt != null && molFrag1 != null 
								&& molFrag1.contains(attachPt)) { 
							moveAtom(superAtom, molecule.indexOf(superAtom),
									moveX * -1.0, moveY * -1.0);
						} else if (attachPt != null && molFrag2 != null 
								&& molFrag2.contains(attachPt)) {
							moveAtom(superAtom, molecule.indexOf(superAtom),
									moveX, moveY);
						} // if a fragment contains the shortcut group
					} // if it's a real shortcut group
				} // for each shortcut group
			} else { 
				debugPrint(SELF + "the two atoms of the broken "
						+ "bond are in the same fragment.");
				// move just the atoms of the broken bond
				// because they're in the same fragment.
				moveAtom(atoms[0], molecule.indexOf(atoms[0]),
						moveX * -1.0, moveY * -1.0);
				moveAtom(atoms[1], molecule.indexOf(atoms[1]), moveX, moveY);
			} // if broken-bond atoms are in different fragments
			ungroupEvenAtomGroup(
					isMulticenterAtom(atoms[0]) ? atoms[0]
					: isMulticenterAtom(atoms[1]) ? atoms[1] 
					: null);
		} // for each broken bond
	} // separateFrags()

	/** Change the XY coordinates of an atom.
	 * @param	atom	the atom to move
	 * @param	index	the index of the atom
	 * @param	moveX	the amount to move the atom in the X direction
	 * @param	moveY	the amount to move the atom in the Y direction
	 */
	private void moveAtom(MolAtom atom, int index, double moveX, 
			double moveY) {
		final String SELF = "MechSolver.moveAtom: ";
		final double oldX = atom.getX();
		final double oldY = atom.getY();
		atom.setX(oldX + moveX);
		atom.setY(oldY + moveY);
		debugPrint(SELF + "Moving atom ", atom, index + 1, 
				" from [", oldX, ", ", oldY,
				"] to [", atom.getX(), ", ", atom.getY(), "]."); 
	} // moveAtom(MolAtom, int, double, double)

	/** Ungroups a multiatom group if it has an even number of atoms.
	 * @param	mcAtom	the "atom" at the center of the multiatom group
	 */
	private void ungroupEvenAtomGroup(MolAtom mcAtom) {
		if (mcAtom != null) {
			ungroupEvenAtomGroup(getMultiatomGroup(mcAtom));
			multicenterAttachPts.remove(mcAtom);
		} // if the atom is a multiatom group
	} // ungroupEvenAtomGroup(MolAtom)

	/** Ungroups a multiatom group if it has an even number of atoms.
	 * @param	multiatomGroup	the multiatom group
	 */
	private void ungroupEvenAtomGroup(Sgroup multiatomGroup) {
		if (isEvenAtomGroup(multiatomGroup)) {
			molecule.ungroupSgroup(multiatomGroup);
		} // if multiatom group has an even number of atoms
	} // ungroupEvenAtomGroup(Sgroup)

	/** Converts radians to degrees.
	 * @param	radians	angle in radians
	 * @return	angle in degrees (as a String)
	 */
	private String toDegrees(double radians) {
		return (Double.isNaN(radians) ? "NaN" 
				: String.valueOf(Math.round(VectorMath.toDegrees(radians))));
	} // toDegrees(double)

	/** Converts an array of radians to degrees.
	 * @param	radiansVals	array of angles in radians
	 * @return	String array of angles in degrees
	 */
	private String[] toDegrees(double[] radiansVals) {
		final String[] degreesVals = new String[radiansVals.length];
		int num = 0;
		for (final double radiansVal : radiansVals) {
			degreesVals[num++] = toDegrees(radiansVal);
		} // for each angle
		return degreesVals;
	} // toDegrees(double[])

	/** Converts degrees to radians.
	 * @param	degrees	angle in degrees
	 * @return	angle in radians
	 */
	private double toRadians(int degrees) {
		return VectorMath.toRadians(degrees);
	} // toRadians(int)

	/** Gets the positive or negative angle in radians about the middle 
	 * point formed by three atoms in the xy plane.
	 * @param	atoms	array of three atoms in the xy plane
	 * @return	the angle between -&pi; and &pi;
	 */
	private double angle(MolAtom[] atoms) {
		return VectorMath.angle(atoms);
	} // angle(MolAtom[])

} // MechSolver
