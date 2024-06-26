package com.epoch.mechanisms;

import chemaxon.calculations.Ring;
import chemaxon.marvin.calculations.GeometryPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.struc.graphics.MAtomSetPoint;
import chemaxon.struc.graphics.MEFlow;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import chemaxon.struc.Molecule;
import chemaxon.struc.MoleculeGraph;
import chemaxon.struc.PeriodicSystem;
import chemaxon.struc.SelectionMolecule;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolCompare;
import com.epoch.chem.MolCompareException;
import com.epoch.chem.MolString;
import com.epoch.chem.pKaFunctions;
import com.epoch.exceptions.VerifyException;
import com.epoch.mechanisms.mechConstants.MechConstants;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Analyzes mechanisms for violations of heuristic rules. */
public class MechRuleFunctions implements MechConstants {

	private static void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	//----------------------------------------------------------------------
	//	members
	//----------------------------------------------------------------------
	/** Student's response mechanism to be analyzed. */
	transient Mechanism givenMech;
	/** Stages of the student's response mechanism. */
	transient List<MechStage> stages;
	/** Index of the stage (in list of stages) that is the first stage of the
	 * cyclic part of a mechanism. */
	transient int firstCyclicStageIndex;

	//----------------------------------------------------------------------
	//	constructors
	//----------------------------------------------------------------------
	/** Constructor. */
	public MechRuleFunctions() {
		// intentionally empty
	}

	/** Constructor. 
	 * @param	givenMech	student's response mechanism
	 */
	public MechRuleFunctions(Mechanism givenMech) {
		this.givenMech = givenMech;
		if (givenMech == null) {
			stages = new ArrayList<MechStage>();
			firstCyclicStageIndex = NOT_FOUND;
		} else {
			stages = givenMech.parsedMech.getStages();
			firstCyclicStageIndex = givenMech.parsedMech.firstCyclicStageIndex;
		}
		// set(givenMech);
	} // constructor

	//----------------------------------------------------------------------
	//	noSameChargeReacting
	//----------------------------------------------------------------------
	/** Gets whether an eflow arrow connects two different compounds with the same 
	 * nonzero charge.
	 * @return	true if there is no such arrow
	 * @throws	MechError	if there is such an arrow
	 */
	public boolean noSameChargeReacting() throws MechError { 
		return noSameChargeReacting(false);
	} // noSameChargeReacting() 

	/** Gets whether an eflow arrow connects two different compounds with the same 
	 * nonzero charge.
	 * @param	allowProtonTransfers	don't consider as a violation a proton
	 * transfer (simultaneous gain and loss of H+)
	 * @return	true if there is no such arrow
	 * @throws	MechError	if there is such an arrow
	 */
	public boolean noSameChargeReacting(boolean allowProtonTransfers) 
			throws MechError { 
		final String SELF = "MechRuleFunctions.noSameChargeReacting: ";
		for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
			final MechStage stage = stages.get(stageIndex);
			final Molecule[] mols = stage.getMoleculeArray();
			if (mols.length == 1) {
				debugPrint(SELF + "stage ", stageIndex + 1, 
						" has only one molecule; no violation possible.");
				continue;
			}
			debugPrintMRV(SELF + "stage molecules:\n", mols);
			final List<MechFlow> flows = stage.getFlows();
			final int numFlows = flows.size();
			debugPrint(SELF + "looking at stage ", stageIndex + 1, " with ", 
					numFlows, " electron-flow arrow(s) and molecules ", mols);
			for (int flowNum = 0; flowNum < numFlows; flowNum++) {
				final MechFlow flow = flows.get(flowNum);
				// get source atom or one atom of source bond
				final MolAtom srcAtom = flow.getSrcAtoms()[0];
				debugPrint(SELF + "flow ", flowNum + 1, " source atom is ", 
						srcAtom);
				// get molecule containing source atom
				final Molecule srcMol = (Molecule) srcAtom.getParent();
				final int srcMolChg = 
						ChemUtils.stripMetals(srcMol).getTotalCharge();
				if (srcMolChg == 0) {
					debugPrint(SELF + "flow ", flowNum + 1, 
							" has neutral source ", srcMol, "; no violation.");
					continue;
				}
				debugPrint(SELF + "flow ", flowNum + 1, 
						" has charged source ", srcMol, "; look at sinks.");
				// get relevant atom or atoms of sink, convert to array 
				final MolAtom[] snkAtoms = flow.getSinkAtoms();
				debugPrint(SELF + "flow ", flowNum + 1, " sink atom(s): ", 
						snkAtoms);
				// get sink molecule of each relevant sink atom
				boolean allSnksInSrc = true; // for debugging
				for (final MolAtom snkAtom : snkAtoms) {
					final Molecule snkMol = (Molecule) snkAtom.getParent();
					debugPrint(SELF + "molecule containing ", snkAtom,
							" is ", snkMol);
					// violation possible only if sink atom not in srcMol
					if (srcMol != snkMol) { // pointer equality
						debugPrint(SELF + "source molecule ", srcMol,
								" not same as sink molecule ", snkMol);
						allSnksInSrc = false;
						final int snkMolChg = 
								ChemUtils.stripMetals(snkMol).getTotalCharge();
						if (snkMolChg == 0) {
							debugPrint(SELF + "flow ", 
									flowNum + 1, " has neutral sink ", 
									snkMol, "; no violation.");
							break;
						} else if ((srcMolChg > 0) != (snkMolChg > 0)) {
							debugPrint(SELF + "flow ", flowNum + 1, 
									" has sink ", snkMol, " charged "
									+ "opposite to source; no violation.");
							break;
						} else { // charges are same
							debugPrint(SELF + "flow ", flowNum + 1, 
									" has charged sink ", snkMol, "; stage ", 
									stageIndex + 1, " contains two ", 
									srcMolChg > 0 ?  "positively" : "negatively", 
									" charged compounds connected by " 
									+ "an electron-flow arrow: violation!");
							if (!allowProtonTransfers
									|| srcMolChg != 1 || snkMolChg != 1 // both mols +1
									|| snkAtom.getAtno() != 1 // not making bond to H
									|| !flow.sinkIsIncipBond() // electron transfer
									|| !isProtonTransfer(flows, flow, 
										srcMol, snkAtom)) {
								givenMech.throwMechError(stageIndex, RULE_VIOLATION); 
							} // if not proton transfer step
						} // if charge of sink molecule
					} // if molecule of this sink atom is not srcMol
				} // for each sink atom
				if (allSnksInSrc) {
					debugPrint(SELF + "flow ", flowNum + 1, " has all sink "
							+ "atoms in source molecule; no violation."); 
				} // if all reactions are intramolecular
			} // for each flow
		} // for each stage
		debugPrint(SELF + "no violation.");
		return true; 
	} // noSameChargeReacting() 

	/** Gets whether two molecules of charge +1 connected by electron-flow arrow
	 * and sink of arrow is H is a proton transfer.
	 * @param	flows	electron-flow arrows of the stage
	 * @param	origFlow	electron-flow arrow connecting two like-charged
	 * compounds
	 * @param	srcMol	the source molecule of flow
	 * @param	origSinkH	the sink atom of flow, already confirmed to be H,
	 * that is not in snkMol
	 * @return	true if this step is merely a proton transfer step
	 */
	private boolean isProtonTransfer(List<MechFlow> flows, MechFlow origFlow,
			Molecule srcMol, MolAtom origSinkH) {
		boolean isHTransfer = false;
		boolean snkIsProton = origSinkH.getCharge() == 1;
		if (!snkIsProton) {
			// maybe a bond to a neutral H in a cationic molecule is breaking
			for (final MechFlow otherFlow : flows) {
				if (otherFlow != origFlow // pointer equality
						&& otherFlow.srcIsBond()
						&& otherFlow.getNumElectrons() == 2) {
					final MolBond srcBond = otherFlow.getSrcBond();
					final MolBond bondOfOrigH = origSinkH.getBond(0);
					if (srcBond == bondOfOrigH) { // pointer equality
						snkIsProton = otherFlow.srcAtomNotSinkAtom(origSinkH);
						if (snkIsProton) break;
					} // if source is bond and other flow breaks bond to H
				} // if otherFlow is 2-e arrow from bond not same as origFlow
			} // for each flow
		} // if sink is not proton
		if (snkIsProton) {
			// is another bond to a proton breaking in srcMol?
			for (final MechFlow otherFlow : flows) {
				if (otherFlow != origFlow // pointer equality
						&& otherFlow.srcIsBond()
						&& otherFlow.getNumElectrons() == 2) {
					final MolBond srcBond = otherFlow.getSrcBond();
					if (srcMol.contains(srcBond)) {
						final MolAtom[] srcAtoms = 
								otherFlow.getSrcAtoms();
						for (final MolAtom srcAtom : srcAtoms) {
							if (srcAtom.getAtno() == 1) {
								isHTransfer = 
										otherFlow.srcAtomNotSinkAtom(srcAtom);
								if (isHTransfer) break;
							} // if atom is H
						} // for each source atom of other flow
					} // if arrow breaks bond in srcMol
				} // if otherFlow is 2-e arrow from bond not same as origFlow
			} // for each flow
		} // if sink is proton
		return isHTransfer;
	} // isProtonTransfer(List<MechFlow>, MechFlow, Molecule, Molecule)

	//----------------------------------------------------------------------
	//	containsSN2
	//----------------------------------------------------------------------
	/** Gets whether there is an S<sub>N</sub>2 substitution in this compound.
	 * 		<br>A.....B-----C (... = incipient bond, --- = single bond)
	 *		<br>AND eflow to A..B AND eflow B--C to C
	 *		<br>MAYBE AND B is sp or sp<sup>2</sup>-hybridized
	 * @param	spOrSp2Matters	whether the S<sub>N</sub>2 substitution must 
	 * be at an sp<sup>2</sup>- or sp-hybridized atom for it to "count"
	 * @return	true if there is no S<sub>N</sub>2 substitution
	 * @throws	MechError	if there is an S<sub>N</sub>2 substitution
	 */
	public boolean containsSN2(boolean spOrSp2Matters) throws MechError { 
		final String SELF = "MechRuleFunctions.containsSN2: "; 
		debugPrint(SELF + "begin");
		for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
			final MechStage stage = stages.get(stageIndex);
			final List<MolAtom> incipBondAtoms = new ArrayList<MolAtom>();
			final List<MolAtom> bearLGs = new ArrayList<MolAtom>();
			final List<MechFlow> nucArrows = new ArrayList<MechFlow>();
			for (final MechFlow flow : stage.getFlows()) {
				if (flow.getNumElectrons() == 2) {  // a two-electron arrow
					addAtomIfBearingLG(flow, bearLGs);
					if (flow.sinkIsIncipBond()) {
						// add both atoms of incipient bond to list of
						// atoms participating in incipient bonds
						final MolAtom[] incipBond = flow.getSinkAtoms();
						incipBondAtoms.add(incipBond[0]);
						incipBondAtoms.add(incipBond[1]);
						// add flow to list of flows pointing to incipient bonds
						nucArrows.add(flow);
					} // if sink is an incipient bond
				} // two-electron flow
			} // for each flow in this stage
			// Find which atoms are in both bearLGs and incipBondAtoms, i.e.,
			// which atoms BOTH lose the electrons in a single bond AND are
			// participating in the formation of an incipient bond.
			for (final MolAtom bearsLG : bearLGs) {
				if (incipBondAtoms.contains(bearsLG)) {
					// There is an SN2 reaction in this stage!
					debugPrint(SELF + "Found SN2 atom ", 
							bearsLG, " in stage ", stageIndex + 1);
					if (!spOrSp2Matters) {
						if ("C".equals(bearsLG.getSymbol())) {
							// find if this is really a 1,2-shift, not an SN2;
							// is 1,2-shift if flow source is bond involving an
							// atom adjacent to bearsLG
							final int bearsLGIndex = bearLGs.indexOf(bearsLG) / 2;
							final MechFlow nucFlow = nucArrows.get(bearsLGIndex); 
									// flow to incipient bond involving bearsLG
							final MolAtom[] sink = nucFlow.getSinkAtoms();
							final MolAtom snkSrcAtom = (sink[0] == bearsLG
									? sink[1] : sink[0]);
									// atom of incipient bond not bearing LG
							if (nucFlow.srcIsBond()) {
								final MolBond srcBond = nucFlow.getSrcBond();
								final MolAtom otherSrcAtom =
										(srcBond.getAtom1() == snkSrcAtom
										 ? srcBond.getAtom2() : srcBond.getAtom1());
								if (otherSrcAtom.getBondTo(bearsLG) == null) {
									debugPrint("SN2 atom is C; "
											+ "not a 1,2-shift; violation of rule");
									givenMech.throwMechError(stageIndex, 
											RULE_VIOLATION);
								} else 
									debugPrint("SN2 atom is C, but reaction "
											+ "is 1,2-shift; no violation");
							} else { // source is an atom
								debugPrint("SN2 atom is C; "
										+ "not a 1,2-shift; violation of rule");
								givenMech.throwMechError(stageIndex, 
										RULE_VIOLATION);
							} // if source is bond 
						} else 
							debugPrint("SN2 atom is not C; no violation");
					} else if (isSpOrSp2Hybridized(bearsLG)) {
						debugPrint("SN2 atom has sp or sp2 hybridization");
						if (isMetal(bearsLG)) {
							debugPrint("no violation of rule because "
									+ "SN2 atom is a metal");
						} else {
							debugPrint("SN2 atom is not a metal; "
									+ "violation of rule");
							givenMech.throwMechError(stageIndex, RULE_VIOLATION);
						}
					} else   // sp3 hybridization
						debugPrint("SN2 atom has sp3 hybridization; "
								+ "no violation of rule");
				} // if atom is in both bearLGs and incipBondAtoms
			} // for each member of bearLGs
		} // for each stage
		debugPrint(SELF + "no violation");
		return true; 
	} // containsSN2(boolean) 

	/** Gets whether an atom is a metal.
	 * @param	atom	an atom
	 * @return	true if the atom is a metal
	 */
	private boolean isMetal(MolAtom atom) {
		final String[] NONMETALS = new String[] {
				"C", "N", "O", "F", "P", "S", "Cl", "As", "Se", "Br", "I"};
		return !Utils.contains(NONMETALS, atom.getSymbol());
	} // isMetal(MolAtom)

	/** Determines whether an atom is or is not sp- or sp<sup>2</sup>-hybridized.
	 * @param	atom	the atom
	 * @return	true if the atom is sp- or sp<sup>2</sup>-hybridized
	 */
	private boolean isSpOrSp2Hybridized(MolAtom atom) {
		final Molecule parentMol = (Molecule) atom.getParent();
		final SelectionMolecule[] frags = parentMol.findFrags();
		final SelectionMolecule atomGraph = new SelectionMolecule();
		atomGraph.add(atom);
		for (final SelectionMolecule frag : frags)
			if (frag.contains(atomGraph)) {
				frag.calcHybridization();
				final int hybState = atom.getHybridizationState();
				return Utils.among(hybState, MolAtom.HS_SP2, MolAtom.HS_SP);
			}
		return false; // couldn't find atom in parentMol
	} // isSpOrSp2Hybridized()

	//----------------------------------------------------------------------
	//	noIonizedBondSpHybridAtom
	//----------------------------------------------------------------------
	/** Gets whether ionization occurs at an sp- or sp<sup>2</sup>-hybridized atom.
	 * 		<br>B-----C (--- = single bond)
	 *		<br>AND  eflow B--C to C AND no eflow to B 
	 *		<br>AND B is sp or sp<sup>2</sup> hybridized
	 * @return	true if there is no such ionization
	 * @throws	MechError	if there is such an ionization
	 */
	public boolean noIonizedBondSpHybridAtom() throws MechError { 
		final String SELF = "MechRuleFunctions.noIonizedBondSpHybridAtom: ";
		debugPrint(SELF + "begin");
		for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
			final MechStage stage = stages.get(stageIndex);
			final List<MolAtom> gainElecsAtoms = new ArrayList<MolAtom>();
			final List<MolAtom> bearLGs = new ArrayList<MolAtom>();
			for (final MechFlow flow : stage.getFlows()) {
				if (flow.getNumElectrons() == 2) {  // a two-electron arrow
					addAtomIfBearingLG(flow, bearLGs);
					final MolAtom[] sinkAtoms = flow.getSinkAtoms();
					// add atoms receiving electrons to list
					if (!gainElecsAtoms.contains(sinkAtoms[0]))
						gainElecsAtoms.add(sinkAtoms[0]); 
					if (sinkAtoms.length > 1
							&& !gainElecsAtoms.contains(sinkAtoms[1]))
						gainElecsAtoms.add(sinkAtoms[1]); 
				} else if (flow.srcIsAtom() && flow.sinkIsAtom()) {
					// one-electron transfer; add receiving atom to list
					final MolAtom increasedAtom = flow.getSinkAtom();
					if (!gainElecsAtoms.contains(increasedAtom))
						gainElecsAtoms.add(increasedAtom);
				} // if flow.getNumElectrons()
			} // for each flow in this stage
			// Find which atoms are in both bearLGs and NOT in gainElecsAtoms,
			// i.e., which atoms BOTH lose the electrons in a single bond AND
			// are NOT participating in the formation of a new bond.
			for (final MolAtom bearsLG : bearLGs) {
				if (!gainElecsAtoms.contains(bearsLG)) {
					// There is an ionization reaction in this stage!
					debugPrint(SELF + " Found ionizing atom in stage ", 
							stageIndex + 1);
					if (isSpOrSp2Hybridized(bearsLG)) {
						// sp or sp2 hybridization
						debugPrint("violation of rule");
						givenMech.throwMechError(stageIndex, RULE_VIOLATION);
					} else  // sp3 hybridization
						debugPrint("no violation of rule");
				} // if atom is in bearLGs and not in gainElecsAtoms
			} // for each member of bearLGs
		} // for each stage
		debugPrint(SELF + "no violation");
		return true; 
	} // noIonizedBondSpHybridAtom() 

	//----------------------------------------------------------------------
	//	addAtomIfBearingLG
	//----------------------------------------------------------------------
	/** Finds if an electron-flow arrow indicates a leaving group leaving an 
	 * atom.  If so, adds the atom bearing the LG to the list.
	 * @param	flow	the electron-flow arrow
	 * @param	bearLGs	list of atoms bearing leaving groups
	 */
	private void addAtomIfBearingLG(MechFlow flow, List<MolAtom> bearLGs) {
		if (flow.srcIsBond() && flow.getSrcBond().getType() == 1) {
			// a single bond is breaking; find out
			// which atom is losing the electrons and
			// add it to the list bearLGs
			final MolAtom[] srcAtoms = flow.getSrcAtoms();
			final MolAtom[] sinkAtoms = flow.getSinkAtoms();
			if (flow.sinkIsAtom()) {
				// the atom that is NOT the sink
				// is added to bearLGs
				if (sinkAtoms[0] != srcAtoms[0]
						&& !bearLGs.contains(srcAtoms[0]))
					bearLGs.add(srcAtoms[0]);
				if (sinkAtoms[0] != srcAtoms[1]
						&& !bearLGs.contains(srcAtoms[0]))
					bearLGs.add(srcAtoms[1]);
			} else { 
				// the atom NOT participating in 
				// formation of the existing or incipient bond
				// is added to bearLGs.
				// pointer equality:
				if (sinkAtoms[0] != srcAtoms[0] 
						&& sinkAtoms[1] != srcAtoms[0]
						&& !bearLGs.contains(srcAtoms[0]))
					bearLGs.add(srcAtoms[0]);
				// pointer equality:
				if (sinkAtoms[0] != srcAtoms[1] 
						&& sinkAtoms[1] != srcAtoms[1]
						&& !bearLGs.contains(srcAtoms[1]))
					bearLGs.add(srcAtoms[1]);
			} // if sink type
		} // if source is a single bond
	} // addAtomIfBearingLG(MechFlow, List<MolAtom>)  

	//----------------------------------------------------------------------
	//	noCarbocations
	//----------------------------------------------------------------------
	/** Gets whether the compound has a true carbocation other than an iminium
	 * ion.
	 * @return	true if the compound has no true carbocation
	 * @throws	MechError	if the compound has a true carbocation
	 */
	public boolean noCarbocations() throws MechError {
		final int[] errorAndStage = noCarbocations2();
		if ((errorAndStage[ERROR] & CARBOCAT_MASK) == CARBOCAT
				&& (errorAndStage[ERROR] & N_RESONANT) == 0) {
			debugPrint("noCarbocations: errorAndStage[ERROR] = ", 
					errorAndStage[ERROR], ", throwing error.");
			givenMech.throwMechError(errorAndStage[STAGE], RULE_VIOLATION);
		}
		return true; 
	} // noCarbocations() 

	/** Gets whether the compound has a true carbocation.
	 * @return	array of ints: first member indicates whether a carbocation
	 * was found and, if so, its nature; second member indicates the index 
	 * of the stage in which it was found.
	 */
	public int[] noCarbocations2() {
		final String SELF = "MechRuleFunctions.noCarbocations2: "; 
		int[] results = new int[2];
		boolean isAcarbocation = false;
		for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
			results[STAGE] = stageIndex;
			final MechStage stage = stages.get(stageIndex);
			for (final Molecule molecule : stage.getMoleculeArray()) {
				final int numAtoms = molecule.getAtomCount();
				final List<MolAtom> beenThere = new ArrayList<MolAtom>();
				boolean anotherAtomIsNeg = false;
				for (int atomIndex = 0; atomIndex < numAtoms; atomIndex++) {
					final MolAtom atom = molecule.getAtom(atomIndex);
					if (atom.getCharge() < 0) anotherAtomIsNeg = true;
					if ("C".equals(atom.getSymbol()) && atom.getCharge() > 0) {
						isAcarbocation = true;
						final int resSearch = isResonantWithLP(atom, beenThere);
						if ((resSearch & NEGATIVE_RESONANT) != 0) {
							// like (C+)-(O-)
							debugPrint(SELF + "in ", molecule, 
									", C", atomIndex + 1, " is cationic," 
									+ " but it has resonance with a negative "
									+ " atom, so we'll keep looking.");
						} else { // there's a true carbocation
							for (int atomNum = atomIndex + 1; 
									atomNum < numAtoms; atomNum++)
								if (molecule.getAtom(atomNum).getCharge() < 0) 
									anotherAtomIsNeg = true;
							if (anotherAtomIsNeg && 
									((resSearch & LONE_PAIR_RESONANT) != 0)) {
								debugPrint(SELF + "in ", molecule, ", C", 
										atomIndex + 1, " is cationic.  It has"
										+ " resonance with a lone pair,"
										+ " and there's a negative atom"
										+ " elsewhere in the compound.");
								results[ERROR] = (CARBOCAT | NEG_ELSEWHERE 
										| LONE_PAIR_RESONANT);
								if ((resSearch & N_RESONANT) != 0) {
									debugPrint("resonance is with N.");
									results[ERROR] |= N_RESONANT;
								}
								return results;
							} else if (anotherAtomIsNeg) {
								debugPrint(SELF + "in ", 
										molecule, ", C", atomIndex + 1, 
										" is cationic, and there's a negative"
										+ " atom elsewhere in the compound.");
								results[ERROR] = (CARBOCAT | NEG_ELSEWHERE);
								return results;
							} else if ((resSearch & LONE_PAIR_RESONANT) != 0) {
								debugPrint(SELF + "in ", molecule, ", C", 
										atomIndex + 1, " is cationic, and it has"
										+ " resonance with a lone pair.");
								results[ERROR] = (CARBOCAT | LONE_PAIR_RESONANT);
								if ((resSearch & N_RESONANT) != 0) {
									debugPrint("resonance is with N.");
									results[ERROR] |= N_RESONANT;
								}
								return results;
							} else {
								debugPrint(SELF + "in ", molecule, ", C", 
										atomIndex + 1, " is cationic.");
								results[ERROR] = CARBOCAT;
								return results;
							} // if there's neutral resonance or charge balance
						} // if there's resonance with a negative atom
					} // if atom is a carbocation
				} // for each atom
			} // for each molecule
		} // for each stage
		if (isAcarbocation) {
			debugPrint(SELF + " could only find a carbocation "
					+ "that had resonance with a negative atom.");
			results[ERROR] = (CARBOCAT | NEGATIVE_RESONANT);
		} else {
			debugPrint(SELF + " could not find a carbocation.");
			results[ERROR] = NO_CARBOCAT; 
		}
		return results;
	} // noCarbocations2() 

	//----------------------------------------------------------------------
	//	isResonantWithLP
	//----------------------------------------------------------------------
	/** Determines whether an electron-deficient atom has its electron-deficiency
	 * mitigated by resonance with an anionic or neutral lone-pair-bearing atom.
	 * @param	atom	atom to examine
	 * @param	alreadyBeenHere list of MolAtoms that have already been 
	 * examined; purpose is to prevent an infinite loop upon recursion
	 * @return	integer describing the kind of resonance found
	 */
	public int isResonantWithLP(MolAtom atom, List<MolAtom> alreadyBeenHere) {
		final String SELF = "MechRuleFunctions.isResonantWithLP: ";
		final MoleculeGraph parentMolG = atom.getParent();
		debugPrint(SELF + "looking for lone pair resonance for atom ", atom, 
				parentMolG.indexOf(atom) + 1, " in ", parentMolG, ".");
		boolean resonantWithLP = false;
		boolean resonantWithN = false;
		final MolAtom[] ligands = atom.getLigands();
		for (final MolAtom ligand : ligands) {
			if (!alreadyBeenHere.contains(ligand)) {
				// assume a negatively charged atom attached
				// to C+ has a lone pair for resonance
				if (ligand.getCharge() < 0) {
					debugPrint(SELF + "atom ", ligand, 
							parentMolG.indexOf(ligand) + 1, 
							" is negative and resonates with cation.");
					return (NEGATIVE_RESONANT | LONE_PAIR_RESONANT);
				}
				// see if there's a lone pair on the ligand
				final int ligUnsharedElecs = 
						ChemUtils.getValenceElectrons(ligand) 
						- ligand.getCharge() 
						- ligand.getImplicitHcount()
						- ligand.twicesumbonds(true, false)/2;
						// Gives number of electrons in bonds;
						// boolean1 = include explicit H atoms; 
						// boolean2 = count number of bonds 
						// instead of sum of bond orders
				if (ligUnsharedElecs >=2) { 
					resonantWithLP = true;
					final MolBond bondToLPAtom = atom.getBondTo(ligand);
					final int bondType = bondToLPAtom.getType();
					// check whether it's able to resonate
					try {
						bondToLPAtom.setType(bondType == 2 ? 3 : 2);
						atom.setCharge(atom.getCharge() - 1);
						ligand.setCharge(ligand.getCharge() + 1);
						final GeometryPlugin plugin = new GeometryPlugin();
						plugin.setMolecule((Molecule) parentMolG);
						plugin.setCalculateForLEConformer("if2D");
						plugin.setCalculateEnergy(true);
						plugin.run();
						final double energy = plugin.getDreidingEnergy();
						debugPrint(SELF + "molecule ", parentMolG, 
								" has Dreiding energy ", energy);
						resonantWithLP = energy < 50;
					} catch (PluginException e) {
						Utils.alwaysPrint(SELF + "GeometryPlugin exception "
								+ "thrown when trying to get geometry of ",
								parentMolG);
					} // try
					atom.setCharge(atom.getCharge() + 1);
					ligand.setCharge(ligand.getCharge() - 1);
					bondToLPAtom.setType(bondType);
					if (resonantWithLP) {
						debugPrint(SELF + "atom ", ligand, 
								parentMolG.indexOf(ligand) + 1, 
								" is neutral and resonates with cation.");
						resonantWithN = ligand.getAtno() == PeriodicSystem.N;
						debugPrint("resonantWithN = ", resonantWithN);
					} else {
						debugPrint(SELF + "atom ", ligand, 
								parentMolG.indexOf(ligand) + 1, 
								" is neutral but cannot resonate with "
								+ "cation because of geometry.");
					} // if resonant with LP
				} // if ligand has a lone pair
			} // if ligand hasn't been analyzed in prior recursion
		} // for each ligand of atom
		if (!resonantWithLP) for (final MolAtom ligand : ligands) {
			if (!alreadyBeenHere.contains(ligand)) {
				alreadyBeenHere.add(ligand);
				// Look for a pi bond to ligand that allows
				// vinylogous resonance; if so, recurse.
				for (final MolAtom ligLigand : ligand.getLigands()) {
					// pointer equality:
					if (ligLigand != atom) {
						final int ligToLigBondType = 
								ligand.getBondTo(ligLigand).getType();
						if (Utils.among(ligToLigBondType, 2, 3)) {
							debugPrint(SELF + "recursing...");
							final int resSearch = isResonantWithLP(ligLigand,
									alreadyBeenHere);
							debugPrint(SELF + "... back from recursing");
							if ((NEGATIVE_RESONANT & resSearch) != 0) {
								debugPrint(SELF + "atom ", ligLigand, 
										parentMolG.indexOf(ligLigand) + 1, 
										" is negative "
										+ "and resonates with cation.");
								return resSearch;
							} else if ((LONE_PAIR_RESONANT & resSearch) != 0) {
								resonantWithLP = true;
								debugPrint(SELF + "atom ", ligLigand, 
										parentMolG.indexOf(ligLigand) + 1, 
										" is neutral "
										+ "and resonates with cation.");
								resonantWithN = 
										(N_RESONANT & resSearch) != 0;
								debugPrint("resonantWithN = ", resonantWithN);
							} // if atom is negative-resonant
						} // if ligBondType is multiple
					} // if ligLigand != original atom
				} // for each bond to ligand
			} // if ligand hasn't been analyzed in prior recursion
		} // for each ligand of atom
		if (resonantWithLP && resonantWithN) {
			final int value = (LONE_PAIR_RESONANT | N_RESONANT);
			debugPrint(SELF + "LP & N, returning ", value);
			return value;
		} else if (resonantWithLP) {
			debugPrint(SELF + "LP only, returning ", LONE_PAIR_RESONANT);
			return LONE_PAIR_RESONANT;
		} else {
			debugPrint(SELF + "no resonance, returning 0");
			return 0;
		}
	} // isResonantWithLP(MolAtom, List<MolAtom>)

	//----------------------------------------------------------------------
	//	noSupplyReceive
	//---------------------------------------------------------------------	 
	/** Gets whether an atom both supplies and receives electrons.
	 * @return	true if the compound has no such atom
	 * @throws	MechError	if the compound has such an atom
	 */
	public boolean noSupplyReceive() throws MechError {
		final String SELF = "MechRuleFunctions.noSupplyReceive: ";
		debugPrint(SELF + "begin");
		for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
			final MechStage stage = stages.get(stageIndex);
			if (noSupplyReceive(stage) != NO_VIOLATION) {
				debugPrint(SELF + "violation in stage ", stageIndex + 1);
				givenMech.throwMechError(stageIndex, RULE_VIOLATION);
			} // if there is a supply/receive violation
		} // for each stageIndex
		debugPrint(SELF + "no violations");
		return true;
	} // noSupplyReceive()

	/** Gets whether an atom both supplies and receives electrons.
	 * @param	stage	stage to examine for the event
	 * @return	number of electrons in one of the offending electron-flow 
	 * arrows; NO_VIOLATION for no violation
	 */
	private int noSupplyReceive(MechStage stage) {
		final String SELF = "MechRuleFunctions.noSupplyReceive: ";
		final int numFlows = stage.getNumFlows();
		for (int flIndex1 = 0; flIndex1 < numFlows - 1; flIndex1++) {
			final MechFlow flow1 = stage.getFlow(flIndex1);
			for (int flIndex2 = flIndex1 + 1; flIndex2 < numFlows; flIndex2++) {
				final MechFlow flow2 = stage.getFlow(flIndex2);
				if (flow1.sinkOrSrcIsOthersSrcOrSink(flow2)) { 
					final MolAtom[] src1Atoms = flow1.getSrcAtoms();
					final MolAtom[] snk1Atoms = flow1.getSinkAtoms();
					final MolAtom[] src2Atoms = flow2.getSrcAtoms();
					final MolAtom[] snk2Atoms = flow2.getSinkAtoms();
					final StringBuilder out = Utils.getBuilder(
							SELF + "violation on flow ", flIndex1 + 1, 
							" {src=", flow1.srcIsAtom() ? " atom " : " bond ");
					for (int atm = 0; atm < src1Atoms.length; atm++) {
						if (atm > 0) out.append('-');
						out.append(src1Atoms[atm].getSymbol());
					} // for each atom in arrow terminus 
					Utils.appendTo(out, ", snk=", flow1.sinkIsAtom() 
							? " atom " : flow1.sinkIsBond() ? " bond "
							: " incipient bond ");
					for (int atm = 0; atm < snk1Atoms.length; atm++) {
						if (atm > 0) out.append('-');
						out.append(snk1Atoms[atm].getSymbol());
					} // for each atom in arrow terminus 
					Utils.appendTo(out, "} and flow ", flIndex2 + 1,
							" {src=", flow2.srcIsAtom() ? " atom " : " bond ");
					for (int atm = 0; atm < src2Atoms.length; atm++) {
						if (atm > 0) out.append('-');
						out.append(src2Atoms[atm].getSymbol());
					} // for each atom in arrow terminus 
					Utils.appendTo(out, ", snk=", 
							flow2.sinkIsAtom() ? " atom "
							: flow2.sinkIsBond() ? " bond "
							: " incipient bond ");
					for (int atm = 0; atm < snk2Atoms.length; atm++) {
						if (atm > 0) out.append('-');
						out.append(snk2Atoms[atm].getSymbol());
					} // for each atom in arrow terminus 
					debugPrint(out.append('}'));
					return flow1.getNumElectrons();
				} // if one flow's sink is another's source
			} // for each flow 2
		} // for each flow 1
		return NO_VIOLATION; 
	} // noSupplyReceive(stage)

	//----------------------------------------------------------------------
	//	noRadicals
	//----------------------------------------------------------------------
	/** Gets whether the mechanism has radicals.
	 * @return	true if the mechanism has no radicals
	 * @throws	MechError	if the mechanism has radicals
	 */
	public boolean noRadicals() throws MechError {
		final String SELF = "MechRuleFunctions.noRadicals: ";
		debugPrint(SELF + "begin");
		for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
			final MechStage stage = stages.get(stageIndex);
			for (final Molecule molecule : stage.getMoleculeArray()) {
				final int numAtoms = molecule.getAtomCount();
				for (int atomIndex = 0; atomIndex < numAtoms; atomIndex++) {
					final MolAtom atom = molecule.getAtom(atomIndex);
					if ((atom.getRadicalCount() % 2) != 0) {
						debugPrint(SELF, molecule, " has radical count = ", 
								atom.getRadicalCount(), " on atom ", atom, 
								atomIndex + 1);
						givenMech.throwMechError(stageIndex, RULE_VIOLATION); 
					} // if radical
				} // for each atom
			} // for each molecule
		} // for each stage
		debugPrint(SELF + "no radicals found, no violation");
		return true; 
	} // noRadicals()	 

	//----------------------------------------------------------------------
	//	noZwitterions
	//----------------------------------------------------------------------
	/** Gets whether the mechanism has zwitterions.
	 * @param	posRequiresH	counts as a zwitterion only if the positive atom
	 * bears an H.
	 * @return	true if the mechanism has no zwitterions
	 * @throws	MechError	if the mechanism has zwitterions
	 */
	public boolean noZwitterions(boolean posRequiresH) throws MechError {
		final String SELF = "MechRuleFunctions.noZwitterions: ";
		debugPrint(SELF + "begin");
		for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
			final MechStage stage = stages.get(stageIndex);
			for (final Molecule molecule : stage.getMoleculeArray()) {
				boolean pos = false;
				boolean neg = false;
				for (final MolAtom atom : molecule.getAtomArray()) {
					if (atom.getCharge() > 0 
							&& (!posRequiresH 
								|| atom.getImplicitHcount() 
									+ atom.getExplicitHcount() > 0))
						pos = true;
					if (atom.getCharge() < 0) neg = true;
					if (pos && neg) {
						debugPrint(SELF, molecule, " in stage ", 
								stageIndex + 1, " is zwitterionic");
						givenMech.throwMechError(stageIndex, RULE_VIOLATION); 
					} // if zwitterionic
				} // for each atom in molecule
			} // for each molecule in stage
		} // for each stage
		debugPrint(SELF + "no zwitterions found, no violation");
		return true; 
	} // noZwitterions()	 

	//----------------------------------------------------------------------
	//	noMultipleCharged
	//----------------------------------------------------------------------
	/** Gets whether the mechanism has multiply charged atoms.
	 * @return	true if the mechanism has no multiply charged atoms
	 * @throws	MechError	if the mechanism has multiply charged atoms
	 */
	public boolean noMultiplyCharged() throws MechError { 
		final String SELF = "MechRuleFunctions.noMultiplyCharged: ";
		debugPrint(SELF + "begin");
		for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
			final MechStage stage = stages.get(stageIndex);
			for (final Molecule molecule : stage.getMoleculeArray()) {
				final int numAtoms = molecule.getAtomCount();
				for (int atomIndex = 0; atomIndex < numAtoms; atomIndex++) {
					final MolAtom atom = molecule.getAtom(atomIndex);
					if (Math.abs(atom.getCharge()) >= 2) {  
						debugPrint(SELF + "atom ", atom, atomIndex + 1, 
								" in ", molecule, " in stage ", 
								stageIndex + 1, " has a multiply charged atom");
						givenMech.throwMechError(stageIndex, RULE_VIOLATION); 
					} // if atom is multiply charged
				} // for each atom in molecule
			} // for each molecule in stage
		} // for each stage
		debugPrint(SELF + "no multiply charged atoms found, no violation");
		return true; 
	} // noMultiplyCharged()	 

	//----------------------------------------------------------------------
	//	noMolMultiplyCharged
	//----------------------------------------------------------------------
	/** Gets whether the mechanism has multiply charged molecules.
	 * @return	true if the mechanism has no multiply charged molecules
	 * @throws	MechError	if the mechanism has multiply charged molecules
	 */
	public boolean noMolMultiplyCharged() throws MechError {
		final String SELF = "MechRuleFunctions.noMolMultiplyCharged: ";
		debugPrint(SELF + "begin");
		for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
			final MechStage stage = stages.get(stageIndex);
			for (final Molecule molecule : stage.getMoleculeArray()) {
				if (Math.abs(molecule.getTotalCharge()) >= 2) {  
					debugPrint(SELF + " molecule ", molecule, " in stage ", 
							stageIndex + 1, " is multiply charged");
					givenMech.throwMechError(stageIndex, RULE_VIOLATION); 
				} // if molecule is multiply charged
			} // for each molecule in stage
		} // for each stage
		debugPrint(SELF + "no multiply charged atoms found, no violation");
		return true; 
	} // noMolMultiplyCharged()

	//----------------------------------------------------------------------
	//	noTermolecular
	//----------------------------------------------------------------------
	/** Gets whether the mechanism has steps involving three or more molecules.
	 * @return	true if the mechanism has no termolecular steps
	 * @throws	MechError	if the mechanism has termolecular steps
	 */
	public boolean noTermolecular() throws MechError {
		final String SELF = "MechRuleFunctions.noTermolecular: ";
		debugPrint(SELF + "begin");
		for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
			final MechStage stage = stages.get(stageIndex);
			final int molecularity = stage.getMolecularity();
			if (molecularity >= 3) {  
				debugPrint(SELF + "stage ", stageIndex + 1, 
						" is termolecular or higher");
				givenMech.throwMechError(stageIndex, RULE_VIOLATION); 
			} // if stage is termolecular or higher
		} // for each stage
		debugPrint(SELF + "no termolecular steps found, no violation");
		return true; 
	} // noTermolecular()

	//----------------------------------------------------------------------
	//	pKaCheck
	//----------------------------------------------------------------------
	/** Gets whether any compound has a p<i>K<sub>a</sub></i> &lt; 
	 * or a p<i>K<sub>b</sub></i> &gt; a given value.
	 * @param	pKvalue	value to compare
	 * @param	acidic	whether we're comparing pKas or pKbs
	 * @return	true if the mechanism has no such compound
	 * @throws	MechError	if the mechanism has such compound
	 * @throws	PluginException	if the pK calculation won't proceed
	 * @throws	VerifyException	if a compound can't be analyzed for pK 
	 */
	public boolean pKaCheck(double pKvalue, boolean acidic) 
			throws PluginException, VerifyException, MechError {
		final String SELF = "MechRuleFunctions.pKaCheck: ";
		double acidicUpperLimit = 20.0, basicLowerLimit = -10.0; // defaults
		if (acidic) acidicUpperLimit = pKvalue;
		else basicLowerLimit = pKvalue;
		debugPrint(SELF + "pKvalue = ", pKvalue, ", acidic = ", 
				acidic, ", acidicUpperLimit = ", acidicUpperLimit, 
				", basicLowerLimit = ", basicLowerLimit);
		for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
			// mechanism could proceed in stages 2 -> 0 -> 1;
			// stageIndex makes sure that stages are checked in
			// order in which they appear in mechanism
			debugPrint(SELF, "Checking stage ", stageIndex + 1, 
					" for violations.");
			final MechStage stage = stages.get(stageIndex);
			for (final Molecule mol : stage.getMoleculeArray()) {
				final Molecule molecule = mol.clone();
				final String smiles = MolString.toString(molecule, SMILES);
				debugPrint(SELF + "calculating pKa's of ", smiles);
				final boolean isProton = "[H+]".equals(smiles);
				final double[] pKapKb = (acidic && isProton ? new double[2] 
						: pKaFunctions.pKapKbMolecule(molecule, stageIndex));
				if ( (acidic && isProton)
						|| (acidic && pKapKb[0] < pKvalue) 
						|| (!acidic && pKapKb[1] > pKvalue) ) {
					if (acidic && isProton)
						debugPrint(SELF + "violation found in stage ", 
								stageIndex + 1, ": H+ is present");
					else debugPrint(SELF + "violation found in stage ", 
							stageIndex + 1, (acidic ? ": pKa of" : ": pKb of "), 
							smiles, " = ", (acidic ? pKapKb[0] : pKapKb[1]));
					givenMech.throwMechError(stageIndex, RULE_VIOLATION);
				} // pKa is too small or large
			} // for each molecule in the stage
			// now check calculated products of previous stage for violations
			if (stageIndex == 0) continue; // no prior stage
			final MechStage priorStage = stages.get(stageIndex - 1);
			if (priorStage.getFlowArrowsStatus() == UNCHECKED)
				priorStage.calculateFlowProducts(); // initialize
			if (priorStage.getFlowArrowsStatus() != ARROWS_OK)
				continue; // bad arrows
			final Molecule[] calcProds = priorStage.getFlowProducts();
			final Molecule[] drawnProds = stage.getMoleculeArray();
			for (final Molecule calcProd : calcProds) {
				final Molecule molecule = calcProd.clone();
				final String smiles = MolString.toString(molecule, SMILES);
				if (MechSet.molInArray(molecule, drawnProds, 
						NOT_LENIENT, SRCHFRAG_AUTH) >= 0) continue; // already checked its pKa 
				debugPrint(SELF + "calculating pKa's of "
						+ "undrawn calculated product ", smiles, 
						" of prior stage ", stageIndex + 1);
				final boolean isProton = "[H+]".equals(smiles);
				final double[] pKapKb = (acidic && isProton ? new double[2] :
						pKaFunctions.pKapKbMolecule(molecule, stageIndex));
				if ( (acidic && isProton)
						|| (acidic && pKapKb[0] < pKvalue) 
						|| (!acidic && pKapKb[1] > pKvalue) ) {
					if (acidic && isProton)
						debugPrint(SELF + "violation found in stage ", 
								stageIndex + 1, ": H+ is present" );
					else debugPrint(SELF + "violation found in stage ", 
							stageIndex + 1, (acidic ? ": pKa of" : ": pKb of "), 
							smiles, " = ", (acidic ? pKapKb[0] : pKapKb[1]));
					givenMech.throwMechError(stageIndex, 
							BAD_PKA_OMITTED_COPRODUCTS, "", smiles);
				} // pKa is too small or large
			} // for each calculated product in the stage
		} // for each stage
		debugPrint(SELF + "no violation!");
		return true; 
	} // pKaCheck(double, boolean)

	//----------------------------------------------------------------------
	//	pKaRuleCheck
	//----------------------------------------------------------------------
	/** Gets whether any stage has two atoms with a p<i>K<sub>a</sub></i> 
	 * difference &gt; 8.
	 * @param	omitFirstStep	whether to consider the first step
	 * @param	omitLastStep	whether to consider the last step
	 * @return	true if the mechanism has no such stage
	 * @throws	MechError	if the mechanism has such compound
	 * @throws	PluginException	if the pK calculation won't proceed
	 * @throws	VerifyException	if a compound can't be analyzed for pK 
	 */
	public boolean pKaRuleCheck(boolean omitFirstStep, boolean omitLastStep) 
			throws PluginException, VerifyException, MechError {
		final int firstStageToCheck = (omitFirstStep ? 1 : 0);
		// there is no last step if it's a cycle
		final boolean omitLastStepMod = omitLastStep 
				&& givenMech.parsedMech.topology == LINEAR;
		debugPrint("pKaRuleCheck beginning, ", 
				(omitFirstStep ? "" : "not "), "omitting first step, ", 
				(omitLastStepMod ? "" : "not "), "omitting last step");
		final int lastStageToCheck = stages.size() - (omitLastStepMod ? 2 : 0); 
			// we actually omit the last two stages, because the penultimate
			// stage will contain the workup step in which a strong acid and
			// strong base may coexist, and the last stage contains merely
			// products 
		for (int stageIndex = firstStageToCheck; 
				stageIndex < lastStageToCheck; stageIndex++) {
			final MechStage stage = stages.get(stageIndex);
			double stageMostBasic = -15;
			double stageMostAcidic = 50;
			for (final Molecule mol : stage.getMoleculeArray()) {
				final Molecule molecule = mol.clone();
				final double[] pKapKb = 
						pKaFunctions.pKapKbMolecule(molecule, stageIndex);
				if (pKapKb[0] < stageMostAcidic) 
					stageMostAcidic = pKapKb[0]; 
				if (pKapKb[1] > stageMostBasic) 
					stageMostBasic = pKapKb[1]; 
			} // for each molecule in a stage indexed by molIndex
			if (stageMostBasic - stageMostAcidic > 8) {
				debugPrint("Violation of pKa rule in stage ", 
						stageIndex + 1, ", which has stageMostBasic = ", 
						stageMostBasic, " and stageMostAcidic = ", 
						stageMostAcidic);
				givenMech.throwMechError(stageIndex, RULE_VIOLATION);
			} // if pKa rule is violated
			else debugPrint("stage ", stageIndex + 1, 
					" has stageMostBasic = ", stageMostBasic, 
					" and stageMostAcidic = ", stageMostAcidic);
		} // for each stage indexed by stageIndex
		debugPrint("MechRuleFunctions.pKaRuleCheck: no violation!");
		return true; 
	} // pKaRuleCheck(boolean, boolean)

	//----------------------------------------------------------------------
	//	resConnect
	//----------------------------------------------------------------------  
	/** Gets whether all "adjacent" stages with resonance structures are 
	 * connected by resonance arrows. 
	 * @param	omit1stStep	whether to consider the first step
	 * @return	true if all resonance structures are connected with resonance
	 * arrows
	 * @throws	MechError	if any resonance structures are not connected with
	 * resonance arrows
	 */
	public boolean resConnect(boolean omit1stStep) throws MechError { 
		final String SELF = "MechRuleFunctions.resConnect: ";
		int start = 0;
		if (omit1stStep) {
			debugPrint(SELF + "omit 1st step, moving to next stage");
			start = 1;
		}
		MechStage stage = stages.get(start);
		for (int stageIndex = start; 
				stageIndex < stages.size() - 1; stageIndex++) {
			debugPrint(SELF + "checking stage ", stageIndex + 1, 
					" vs. stage ", stageIndex + 2);
			final MechStage nextStage = stages.get(stageIndex + 1);  
			if (!stage.getArrowToNext().isResonant() &&
					stagesHaveResStructs(stage, nextStage)) {
				final MechUtils util = new MechUtils(givenMech);
				throw new MechError(util.getXmlWithColoredArrow(stage), 
						RULE_VIOLATION);
			}
			stage = nextStage;
		} // for each stage to check
		if (givenMech.parsedMech.topology == CYCLIC) {
			final MechStage nextStage = stages.get(firstCyclicStageIndex);
			if (!stage.getArrowToNext().isResonant() &&
					stagesHaveResStructs(stage, nextStage)) {
				final MechUtils util = new MechUtils(givenMech);
				throw new MechError(util.getXmlWithColoredArrow(stage),
						RULE_VIOLATION);
			}
		} // if mechanism is cyclic
		return true; 
	} // resConnect()

	//----------------------------------------------------------------------
	//	connectRes
	//----------------------------------------------------------------------
	/** Gets whether all stages connected by resonance arrows contain 
	 * resonance structures. 
	 * @param	omit1stStep	whether to consider the first step
	 * @return	true if all resonance arrows connect resonance structures
	 * @throws	MechError	if any resonance arrows do not connect resonance
	 * structures
	 */
	public boolean connectRes(boolean omit1stStep) throws MechError { 
		final String SELF = "MechRuleFunctions.connectRes: ";
		int start = 0;
		if (omit1stStep) {
			debugPrint(SELF + "omit 1st step, moving to next stage");
			start = 1;
		}
		MechStage stage = stages.get(start);
		for (int stageIndex = start; 
				stageIndex < stages.size() - 1; stageIndex++) {
			debugPrint(SELF + "checking stage ", stageIndex + 1, 
					" vs. stage ", stageIndex + 2);
			final MechStage nextStage = stages.get(stageIndex + 1);
			if (stage.getArrowToNext().isResonant() &&
					!stagesHaveResStructs(stage, nextStage)) {
				final MechUtils util = new MechUtils(givenMech);
				throw new MechError(util.getXmlWithColoredArrow(stage),
						RULE_VIOLATION);
			}
			stage = nextStage;
		} // for each stage to check
		if (givenMech.parsedMech.topology == CYCLIC) { 
			final MechStage nextStage = stages.get(firstCyclicStageIndex);
			if (stage.getArrowToNext().isResonant() &&
					!stagesHaveResStructs(stage, nextStage)) {
				final MechUtils util = new MechUtils(givenMech);
				throw new MechError(util.getXmlWithColoredArrow(stage),
						RULE_VIOLATION);
			}
		} // if mechanism is cyclic
		return true; 
	} // connectRes()

	//----------------------------------------------------------------------
	//	stagesHaveResStructs
	//----------------------------------------------------------------------
	/** Gets whether two adjacent stages have resonance structures. 
	 * @param	stage1	a stage
	 * @param	stage2	an adjacent stage
	 * @return	true if the stages have resonance structures
	 */
	public boolean stagesHaveResStructs(MechStage stage1, MechStage stage2) { 
		final Molecule[] stage1Array = stage1.getMoleculeArray();
		final Molecule[] stage2Array = stage2.getMoleculeArray();
		debugPrint("Comparing ", stage1Array, " to ", stage2Array);
		for (final Molecule molecule1 : stage1Array) {
			for (final Molecule molecule2 : stage2Array) {
				try {
					if (MolCompare.areResonanceStructures(
							molecule1, molecule2)) {
						debugPrint("Resonance structures ", molecule1,
								" and ", molecule2, 
								" found in these two stages.");
						return true;
					} // molecules are resonance structures
				} catch (MolCompareException e) {
					Utils.alwaysPrint("MechRuleFunctions.stagesHaveResStructs: "
							+ "caught MolCompareException; continue looking.");
					e.printStackTrace();
				}
			} // for each molecule in stage 2
		} // for each molecule in stage1
		debugPrint("No resonance structures found in these two stages.");
		return false;
	} // stagesHaveResStructs(MechStage, MechStage)

	//----------------------------------------------------------------------
	//		printPeriCode 
	//----------------------------------------------------------------------
	/** Prints pericyclic reaction data from isPericyclic().
	 * @param	rxnData	pericyclic reaction data from isPericyclic()
	 */
	public void printPeriCode(int[] rxnData) {
		debugPrint("PeriCode: " 
				+ " type = ", rxnData[RXN_TYPE],
				", # elecs = ", rxnData[ELECS],
				", # atoms = ", rxnData[ATOMS],
				", comp1 = ", rxnData[COMPON1],
				", comp2 = ", rxnData[COMPON2],
				", index = ", rxnData[STAGE_INDEX]);
	} // printPeriCode 

	//----------------------------------------------------------------------
	// noPeriHasEvenPairsElecs
	//----------------------------------------------------------------------
	/** Gets whether any pericyclic reactions that are not electrocyclic 
	 * reactions involve an even number of pairs of electrons in the 
	 * pericyclic array and, if it is a cycloaddition or retro-cycloaddition,
	 * neither component is just one atom.
	 * @return	true if there are no pericyclic reactions that are not
	 * electrocyclic that involve an even number of pairs of electrons
	 * @throws	MechError	if there is such a reaction
	 */
	public boolean noPeriHasEvenPairsElecs() throws MechError {
		final String SELF = "noPeriHasEvenPairsElecs: ";
		final List<int[]> rxnDataList = isPericyclic();
		debugPrint(SELF + "# rxnData arrays returned = ", 
				rxnDataList.size()); 
		for (final int[] rxnData : rxnDataList) {
			if (!Utils.among(rxnData[RXN_TYPE], NOT_PERICYCLIC, 
						ELECTROCYCLIC_OPENING, ELECTROCYCLIC_CLOSING) 
					&& rxnData[ELECS] % 4 == 0
					&& ((rxnData[RXN_TYPE] != CYCLOADDN
							&& rxnData[RXN_TYPE] != RETROCYCLOADDN)
						|| (rxnData[COMPON1] > 1 
							&& rxnData[COMPON2] > 1))) {
				debugPrint(SELF + "violation at stage ", 
						rxnData[STAGE_INDEX] + 1);
				givenMech.throwMechError(rxnData[STAGE_INDEX], 
						RULE_VIOLATION);
			} // if stage is pericyclic but not electrocyclic 
				// and num electrons is divisible by 4
		} // for each periCyclic rxnData returned 
		return true;
	} // noPeriHasEvenPairsElecs()

	//----------------------------------------------------------------------
	// noPeriInvolvesFourAtoms
	//----------------------------------------------------------------------
	/** Gets whether any pericyclic reactions other than electrocyclic 
	 * reactions involve four atoms. 
	 * @return	true if there are no pericyclic reactions that are not
	 * electrocyclic that involve four atoms
	 * @throws	MechError	if there is such a reaction
	 */
	public boolean noPeriInvolvesFourAtoms() throws MechError {
		final String SELF = "noPeriInvolvesFourAtoms: ";
		final List<int[]> rxnDataList = isPericyclic();
		debugPrint(SELF + "# rxnData arrays returned = ",
				rxnDataList.size()); 
		for (final int[] rxnData : rxnDataList) {
			if (rxnData[RXN_TYPE] != NOT_PERICYCLIC 
					&& rxnData[RXN_TYPE] != ELECTROCYCLIC_OPENING 
					&& rxnData[RXN_TYPE] != ELECTROCYCLIC_CLOSING 
					&& rxnData[ATOMS] == 4) {
				debugPrint(SELF + "violation at stage ", 
						rxnData[STAGE_INDEX] + 1);
				givenMech.throwMechError(rxnData[STAGE_INDEX], 
						RULE_VIOLATION);
			} // if stage is pericyclic but not electrocyclic 
				// and number of atoms is 4 
		} // for each pericyclic rxnData returned 
		return true;
	} // noPeriInvolvesFourAtoms()

	//----------------------------------------------------------------------
	//	isPericyclic
	//----------------------------------------------------------------------
	/** Gets descriptions of the pericyclic steps of a mechanism. 
	 * @return	a list of integer arrays, each of which describes a pericyclic 
	 * step in a mechanism.
	 * Each int[] contains the type of pericyclic mechanism, 
	 * the number of electrons in the pericyclic transition state, 
	 * the number of atoms in the pericyclic transition state, 
	 * the size of one component of a sigmatropic rearrangement or 
	 * cycloaddition, 
	 * the size of the other component of a sigmatropic rearrangement or 
	 * cycloaddition, and the index of the stage in which the mechanistic 
	 * step occurs.
	 */
	public List<int[]> isPericyclic() {
		final String SELF = "isPericyclic: ";
		final List<int[]> periMechs = new ArrayList<int[]>();
		int stageIndex = 0;
		for (final MechStage stage : stages) {
			debugPrint(SELF + "Checking stage ", stageIndex + 1);
			try {
				int[] rxnData = isPericyclic(stage);
				if (rxnData[RXN_TYPE] != NOT_PERICYCLIC) { 
					// store stageIndex
					debugPrint("Pericyclic step found in stage ", 
							stageIndex + 1);
					rxnData[STAGE_INDEX] = stageIndex;
					periMechs.add(rxnData);
				}
			} catch (MechError e) {
				debugPrint("Stage ", stageIndex + 1, " has a two-electron "
						+ "arrow connecting an atom to its ligand.");
			}
			stageIndex++;
		} // for each stage
		if (!periMechs.isEmpty()) 
			debugPrint(SELF + "found ", periMechs.size(), 
					" pericyclic steps in mechanism.");
		else debugPrint(SELF + "Could not find a pericyclic step.");
		return periMechs;
	} // isPericyclic()

	/** Given a stage, gets description of its pericyclic step (if any). 
	 * Creates a new MechStage by reproducing reactive part of MechStage; 
	 * see isPericyclicNew() below. 
	 * @param	stage	stage of a mechanism
	 * @return	array of int containing the type of pericyclic mechanism, 
	 * the number of electrons in the pericyclic transition state, 
	 * the number of atoms in the pericyclic transition state, 
	 * the size of one component of a sigmatropic rearrangement or cycloaddition, 
	 * the size of the other component of a sigmatropic rearrangement or 
	 * cycloaddition, and the index of the stage in which the mechanistic 
	 * step occurs
	 * @throws	MechError	if a 2-electron arrow points from an atom to one of
	 * its ligands
	 */
	public int[] isPericyclic(MechStage stage) throws MechError {
		final String SELF = "isPericyclic: ";
		final Molecule origMol = stage.getFusedMolecule();
		boolean sigmaBondMakesOrBreaks = false;
		// make new molecule, reactArray, consisting of copies of only those 
		// atoms and bonds involved in bond-making and -breaking
		final Molecule reactArray = new Molecule();
		final List<Integer> origIndices = new ArrayList<Integer>();
			// (use to correlate atoms in original molecule and reactArray)
		// populate reactArray
		for (final MechFlow flow : stage.getFlows()) {
			final MolAtom[] srcAtoms = flow.getSrcAtoms();
			if (flow.srcIsBond()) {
				final Integer origAtom1Index = 
						Integer.valueOf(origMol.indexOf(srcAtoms[0]));
				final Integer origAtom2Index = 
						Integer.valueOf(origMol.indexOf(srcAtoms[1]));
				MolAtom newAtom1 = new MolAtom(srcAtoms[0].getAtno());
				MolAtom newAtom2 = new MolAtom(srcAtoms[1].getAtno());
				if (!origIndices.contains(origAtom1Index)) {
					reactArray.add(newAtom1);
					origIndices.add(origAtom1Index);
				} else newAtom1 =   // reset to atom already in array
						reactArray.getAtom(origIndices.indexOf(origAtom1Index));
				if (!origIndices.contains(origAtom2Index)) {
					reactArray.add(newAtom2);
					origIndices.add(origAtom2Index);
				} else newAtom2 =   // reset to atom already in array
						reactArray.getAtom(origIndices.indexOf(origAtom2Index));
				if (newAtom1.getBondTo(newAtom2) == null) {
					final MolBond newBond = new MolBond(newAtom1, newAtom2);
					reactArray.add(newBond);
				} // if bond does not already exist in array
				if (flow.getSrcBond().getType() == 1) 
					sigmaBondMakesOrBreaks = true; 
			} else { // source is a MolAtom
				final Integer origAtomIndex = 
						Integer.valueOf(origMol.indexOf(srcAtoms[0]));
				final MolAtom newAtom = new MolAtom(srcAtoms[0].getAtno());
				if (!origIndices.contains(origAtomIndex)) {
					reactArray.add(newAtom);
					origIndices.add(origAtomIndex);
				} // if atom not already in list of reactive atoms
			} // if source type
			final MolAtom[] sinkAtoms = flow.getSinkAtoms();
			if (flow.sinkIsIncipBond()) {
				MolAtom atom0 = new MolAtom(sinkAtoms[0].getAtno());
				MolAtom atom1 = new MolAtom(sinkAtoms[1].getAtno());
				final Integer origAtom0Index = 
						Integer.valueOf(origMol.indexOf(sinkAtoms[0]));
				final Integer origAtom1Index = 
						Integer.valueOf(origMol.indexOf(sinkAtoms[1]));
				if (!origIndices.contains(origAtom0Index)) {
					reactArray.add(atom0);
					origIndices.add(origAtom0Index);
				} else atom0 =   // reset to atom already in array
						reactArray.getAtom(origIndices.indexOf(origAtom0Index));
				if (!origIndices.contains(origAtom1Index)) {
					reactArray.add(atom1);
					origIndices.add(origAtom1Index);
				} else atom1 =   // reset to atom already in array
						reactArray.getAtom(origIndices.indexOf(origAtom1Index));
				if (atom0.getBondTo(atom1) == null) {
					final MolBond bond = new MolBond(atom0, atom1);
					reactArray.add(bond);
				} // bond does not already exist in array
				sigmaBondMakesOrBreaks = true;
			} else if (flow.sinkIsBond()) {
				final Integer origAtom1Index = 
						Integer.valueOf(origMol.indexOf(sinkAtoms[0]));
				final Integer origAtom2Index = 
						Integer.valueOf(origMol.indexOf(sinkAtoms[1]));
				MolAtom newAtom1 = new MolAtom(sinkAtoms[0].getAtno());
				MolAtom newAtom2 = new MolAtom(sinkAtoms[1].getAtno());
				if (!origIndices.contains(origAtom1Index)) {
					reactArray.add(newAtom1);
					origIndices.add(origAtom1Index);
				} else newAtom1 =   // reset to atom already in array
						reactArray.getAtom(origIndices.indexOf(origAtom1Index));
				if (!origIndices.contains(origAtom2Index)) {
					reactArray.add(newAtom2);
					origIndices.add(origAtom2Index);
				} else newAtom2 =   // reset to atom already in array
						reactArray.getAtom(origIndices.indexOf(origAtom2Index));
				if (newAtom1.getBondTo(newAtom2) == null) { 
					final MolBond newBond = new MolBond(newAtom1, newAtom2);
					reactArray.add(newBond);
				} // bond does not already exist in array
			} else { // sink is a MolAtom
				final Integer origAtomIndex = 
						Integer.valueOf(origMol.indexOf(sinkAtoms[0]));
				final MolAtom newAtom = new MolAtom(sinkAtoms[0].getAtno());
				if (!origIndices.contains(origAtomIndex)) {
					reactArray.add(newAtom);
					origIndices.add(origAtomIndex);
				} // if atom not already in list of reactive atoms
			} // if sink type
		} // for each flow
		if (!sigmaBondMakesOrBreaks) { // it's a resonance structure
			debugPrint("No sigma bonds making or breaking; not a "
					+ "pericyclic mechanism.");
			return new int[NUM_PERI_DATA]; 
		} // if a sigma bond makes or breaks

		// Find out if any two array atoms are adjacent in the original 
		// molecule but not in the array; if so, make them adjacent in the
		// array, too.
		for (int atomIndex = 0; atomIndex < reactArray.getAtomCount();
				atomIndex++) {
			final MolAtom newAtom = reactArray.getAtom(atomIndex);
			final int origAtomIndex = origIndices.get(atomIndex);
			final MolAtom origAtom = origMol.getAtom(origAtomIndex);
			// See if newAtom is adjacent in the original molecule
			// to an atom that is nonadjacent in the array.
			final MolAtom[] origLigs = origAtom.getLigands();
			for (final MolAtom origLig : origLigs) {
				final int origLigIndex = origMol.indexOf(origLig);
				if (origIndices.contains(origLigIndex)) {
					final int arrayLigIndex = 
							origIndices.indexOf(origLigIndex);
					final MolAtom arrayLig =
							reactArray.getAtom(arrayLigIndex);
					// Check that the bond in origMol is not already 
					// present in reactArray.
					if (newAtom.getBondTo(arrayLig) == null) {
						// set a new bond between this array 
						// atom and other array atom
						final MolBond newBond = new MolBond(
								newAtom, arrayLig);
						reactArray.add(newBond);
						break;
					} // if newAtom isn't connected to ligand in reactArray
				} // if ligand of original atom is in reactArray
			} // for each ligand of origAtom
		} // for each atom added to array
		final Ring countRings = new Ring();
		countRings.setMolecule(reactArray);
		final int atomsInArray = countRings.ringAtomCount();
		debugPrint("The reactive array ", reactArray, 
				" has ", atomsInArray, " ring atoms.");
		if (atomsInArray < 3) { // it's not pericyclic
			debugPrint("No cyclic array among reactive atoms; not "
					+ "a pericyclic mechanism.");
			return new int[NUM_PERI_DATA];
		} // if not pericyclic

		// the reaction is pericyclic.
		// determine number of electrons in pericyclic array
		final MechStage arrayPlusFlows = new MechStage();
		arrayPlusFlows.addMolecule(reactArray);
		int eCount = 0;
		int elecsInSigmaBondMakes = 0;
		int elecsInSigmaBondBreaks = 0;
		// record bonds that are making/breaking so as to break 
		// them later to get component sizes (if appropriate)
		final List<MolBond> changeSigBonds = new ArrayList<MolBond>();
		for (final MechFlow flow : stage.getFlows()) {
			boolean sourceInArray = false, sinkInArray = false,
					sourceIsSingleBond = false;
			MolBond newSourceBond = null, newSinkBond = null;
			// find if source is in the reactive ring 
			MolAtom[] src = flow.getSrcAtoms();
			if (flow.srcIsAtom()) {
				final Integer origAtomIndex = Integer.valueOf(origMol.indexOf(src[0]));
				final Integer newAtomIndex = origIndices.indexOf(origAtomIndex);
				final MolAtom newAtom = reactArray.getAtom(newAtomIndex);
				sourceInArray = !countRings.isChainAtom(reactArray.indexOf(newAtom));
				src = new MolAtom[] {newAtom};
			} else { // source is a MolBond
				final Integer origAtom1Index = Integer.valueOf(origMol.indexOf(src[0]));
				final Integer origAtom2Index = Integer.valueOf(origMol.indexOf(src[1]));
				final Integer newAtom1Index = origIndices.indexOf(origAtom1Index);
				final Integer newAtom2Index = origIndices.indexOf(origAtom2Index);
				final MolAtom newAtom1 = reactArray.getAtom(newAtom1Index);
				final MolAtom newAtom2 = reactArray.getAtom(newAtom2Index);
				newSourceBond = newAtom1.getBondTo(newAtom2);
				sourceInArray = !countRings.isChainBond(reactArray.indexOf(newSourceBond));
				sourceIsSingleBond = flow.getSrcBond().getType() == 1;
				src = new MolAtom[] {newAtom1, newAtom2};
			} // if source type
			// set the source of the flow arrow for the new array
			final MAtomSetPoint srcPt = new MAtomSetPoint(src); 
			// find if sink is in the reactive ring
			MolAtom[] snk = flow.getSinkAtoms();
			if (flow.sinkIsAtom()) {
				final Integer origAtomIndex = Integer.valueOf(origMol.indexOf(snk[0]));
				final Integer newAtomIndex = origIndices.indexOf(origAtomIndex);
				final MolAtom newAtom = reactArray.getAtom(newAtomIndex);
				sinkInArray = !countRings.isChainAtom(reactArray.indexOf(newAtom));
				snk = new MolAtom[] {newAtom};
			} else { // sink is MolBond or incipient bond
				final Integer origAtom1Index = Integer.valueOf(origMol.indexOf(snk[0]));
				final Integer origAtom2Index = Integer.valueOf(origMol.indexOf(snk[1]));
				final Integer newAtom1Index = origIndices.indexOf(origAtom1Index);
				final Integer newAtom2Index = origIndices.indexOf(origAtom2Index);
				final MolAtom newAtom1 = reactArray.getAtom(newAtom1Index);
				final MolAtom newAtom2 = reactArray.getAtom(newAtom2Index);
				newSinkBond = newAtom1.getBondTo(newAtom2);
				sinkInArray = !countRings.isChainBond(reactArray.indexOf(newSinkBond));
				snk = new MolAtom[] {newAtom1, newAtom2};
			} // if sink type
			// set the sink of the new flow arrow for the new array
			final MAtomSetPoint snkPt = new MAtomSetPoint(snk); 
			if (sourceInArray && sinkInArray) {
				// create a flow arrow for new array 
				final int flowElecs = flow.getNumElectrons();
				final MEFlow newFlow = new MEFlow(srcPt, snkPt, flowElecs);
				final MechFlow newMechFlow = new MechFlow(newFlow);
				arrayPlusFlows.addFlow(newMechFlow); 
				eCount += flowElecs;
				if (sourceIsSingleBond) {
					elecsInSigmaBondBreaks += flowElecs;
					if (newSourceBond != null &&
							!changeSigBonds.contains(newSourceBond)) {
						changeSigBonds.add(newSourceBond);
					} // if should add to changing bonds
				} // if source is a single bond
				if (flow.sinkIsIncipBond()) {
					elecsInSigmaBondMakes += flowElecs; 
					if (newSinkBond != null &&
							!changeSigBonds.contains(newSinkBond)) {
						changeSigBonds.add(newSinkBond);
					} // if should add to changing bonds
				} // if sink is an incipient bond
			} // if source & sink both in array
		} // for each flow
		// adjust electron count if the noSupplyReceive rule is broken 
		final int supplyReceiveElecs = noSupplyReceive(arrayPlusFlows);
		if (supplyReceiveElecs != NO_VIOLATION) {
			debugPrint(SELF + "violation of no supply/receive rule; ", 
					supplyReceiveElecs, " electrons involved");
			eCount = eCount - supplyReceiveElecs;
		} // if violation of noSupplyReceive rule
		debugPrint(SELF + "reactArray: ", reactArray, "\nchangeSigBonds: ",
				changeSigBonds, "\natomsInArray = ", atomsInArray,
				", eCount = ", eCount, ", elecsInSigmaBondBreaks = ",
				elecsInSigmaBondBreaks, ", elecsInSigmaBondMakes = ",
				elecsInSigmaBondMakes);
 		// finished with molecule manipulations and electron counting; 
		// now can calculate type of pericyclic reaction
		return createPeriOutput(reactArray, changeSigBonds, atomsInArray,
				eCount, elecsInSigmaBondBreaks, elecsInSigmaBondMakes);
	} // isPericyclic()

	/** Calculates the type of pericyclic reaction and the return values.
	 * @param	reactArray	atoms undergoing a pericyclic reaction
	 * @param	changeSigBonds	sigma bonds formed or broken in the pericyclic 
	 * reaction
	 * @param	atomsInArray	number of atoms involved in the pericyclic
	 * reaction
	 * @param	eCount	number of electrons involved in the pericyclic 
	 * reaction
	 * @param	elecsInSigmaBondBreaks	number of electrons in sigma bonds that
	 * break in the reaction
	 * @param	elecsInSigmaBondMakes	number of electrons in sigma bonds that
	 * are made in the reaction
	 * @return	array of ints for isPericyclic() or isPericyclicNew()
	 */
	public int[] createPeriOutput(Molecule reactArray,
			List<MolBond> changeSigBonds, int atomsInArray, int eCount, 
			int elecsInSigmaBondBreaks, int elecsInSigmaBondMakes) {
		int[] periData = new int[NUM_PERI_DATA]; 
		// Record number of electrons and electrons.
		periData[ATOMS] = atomsInArray;
		periData[ELECS] = eCount;
		// Record reaction type and, if appropriate, component sizes.
		List<Integer> componentSizes = new ArrayList<Integer>();
		int sigmaBondBreaks = 0, sigmaBondMakes = 0;
		if ((elecsInSigmaBondBreaks % 2 == 0) 
				&& (elecsInSigmaBondMakes % 2 == 0)) {
			sigmaBondBreaks = elecsInSigmaBondBreaks / 2;
			sigmaBondMakes = elecsInSigmaBondMakes / 2;
			if (sigmaBondMakes == 1 && sigmaBondBreaks == 1) {
				periData[RXN_TYPE] = SIGMATROPIC;
				componentSizes = getComponentSizes(reactArray, changeSigBonds);
				if (componentSizes.size() == 2) {
					periData[COMPON1] = componentSizes.get(0);
					periData[COMPON2] = componentSizes.get(1);
				}
			} else if (sigmaBondMakes + sigmaBondBreaks == 1) {
				periData[RXN_TYPE] = (sigmaBondMakes == 1 
						? ELECTROCYCLIC_CLOSING : ELECTROCYCLIC_OPENING);
			} else if (sigmaBondMakes + sigmaBondBreaks == 2) {
				periData[RXN_TYPE] = (sigmaBondMakes == 2 
						? CYCLOADDN : RETROCYCLOADDN);
				componentSizes = getComponentSizes(reactArray, changeSigBonds);
				if (componentSizes.size() == 2) {
					periData[COMPON1] = componentSizes.get(0);
					periData[COMPON2] = componentSizes.get(1);
				}
			} else if (sigmaBondMakes == 2 && sigmaBondBreaks == 1) {
				periData[RXN_TYPE] = ENE;
			} else if (sigmaBondMakes == 1 && sigmaBondBreaks == 2) {
				periData[RXN_TYPE] = RETROENE;
			} else if (sigmaBondMakes == 2 && sigmaBondBreaks == 2) {
				periData[RXN_TYPE] = GROUP_TRANSFER;
			} else if (sigmaBondMakes + sigmaBondBreaks == 3) {
				periData[RXN_TYPE] = (sigmaBondMakes == 3
							? THREE_COMPON_CYCLOADDN 
							: THREE_COMPON_RETROCYCLOADDN);
				componentSizes = getComponentSizes(reactArray, changeSigBonds);
				if (componentSizes.size() == 3) {
					periData[COMPON1] = componentSizes.get(0);
					periData[COMPON2] = componentSizes.get(1);
				}
			} else { // UNKNOWN 
				periData[RXN_TYPE] = UNKNOWN_TYPE;
			}
		} // even number of electrons in making and breaking bonds
		final StringBuilder start = Utils.getBuilder(
				"Stage is pericyclic!  The reaction is a ", periData[ATOMS], 
				"-atom, ", periData[ELECS], "-electron ");
		final boolean retro = Utils.among(periData[RXN_TYPE], 
				RETROCYCLOADDN, THREE_COMPON_RETROCYCLOADDN);
		int[] sorter;
		switch (periData[RXN_TYPE]) { 
			case SIGMATROPIC: 
				if (periData[ATOMS] != 0) {
					sorter = new int[2];
					sorter[0] = periData[COMPON1];
					sorter[1] = periData[COMPON2];
					Arrays.sort(sorter);
					Utils.appendTo(start, '[', sorter[0], ", ",
							sorter[1], "] ");
				}
				debugPrint(start.append("sigmatropic rearrangement."));
				break;
			case ELECTROCYCLIC_CLOSING: 
				debugPrint(start.append("electrocyclic ring closing."));
				break;
			case ELECTROCYCLIC_OPENING:
				debugPrint(start.append("electrocyclic ring opening."));
				break;
			case RETROCYCLOADDN: 
			case CYCLOADDN: 
				if (periData[ATOMS] != 0) {
					sorter = new int[2];
					sorter[0] = periData[COMPON1];
					sorter[1] = periData[COMPON2];
					Arrays.sort(sorter);
					Utils.appendTo(start, '[', sorter[1], " + ",
							sorter[0], "] ");
				}
				debugPrint(start.append(retro 
						? "retrocycloaddition." : "cycloaddition."));
				break;
			case ENE:
				debugPrint(start.append("ene reaction."));
				break;
			case RETROENE:
				debugPrint(start.append("retro-ene reaction."));
				break;
			case GROUP_TRANSFER:
				debugPrint(start.append("group transfer reaction."));
				break;
			case THREE_COMPON_RETROCYCLOADDN: 
			case THREE_COMPON_CYCLOADDN: 
				if (periData[ATOMS] != 0) {
					sorter = new int[3];
					sorter[0] = periData[COMPON1];
					sorter[1] = periData[COMPON2];
					sorter[2] = periData[ATOMS] - sorter[0] - sorter[1];
					Arrays.sort(sorter);
					Utils.appendTo(start, '[', sorter[2], " + ",
							sorter[1], " + ", sorter[0], "] ");
				}
				debugPrint(start, "three-component ", 
						(retro ? "retrocycloaddition." : "cycloaddition."));
				break;
			default: // UNKNOWN_TYPE
				debugPrint(start.append("reaction of unknown type."));
				break;
		} // switch
		return periData;
	} // createPeriOutput(Molecule, List<MolBond>, int, int, int, int)

	/** Gets the number of atoms in each component of a pericyclic reaction.
	 * @param	reactArray	atoms undergoing a pericyclic reaction
	 * @param	changeSigBonds	sigma bonds formed or broken in the pericyclic 
	 * reaction
	 * @return	list of component sizes
	 */
	private List<Integer> getComponentSizes(Molecule reactArray, 
			List<MolBond> changeSigBonds) {
		final String SELF = "MechRuleFunctions.getComponentSizes: ";
		final Ring ringAtomFinder = new Ring();
		ringAtomFinder.setMolecule(reactArray);
		for (int atomIndex = reactArray.getAtomCount() - 1; atomIndex >= 0;
				atomIndex--) {
			if (ringAtomFinder.ringCountOfAtom(atomIndex) == 0) {
				debugPrint(SELF + "removing non-ring atom ", 
						reactArray.getAtom(atomIndex).getSymbol(),
						atomIndex + 1, " from reactArray");
				reactArray.removeAtom(atomIndex);
			} // if atom is not part of a ring
		} // for each atom added to array
		for (final MolBond bond : changeSigBonds) {
			reactArray.removeBond(bond);
		} // for each changing bond
		final SelectionMolecule[] pieces = reactArray.findFrags();
		final List<Integer> piecesSizes = new ArrayList<Integer>();
		for (final SelectionMolecule piece : pieces) {
			piecesSizes.add(Integer.valueOf(piece.getAtomCount()));
		} // for each piece
		return piecesSizes;
	} // getComponentSizes(Molecule, List<MolBond>)

	/* Given a stage, gets description of its pericyclic step. 
	 * Not currently used because it modifies the MechStage directly; 
	 * there is no MechStage.clone() method, and to use this method requires
	 * creating a brand new Mechanism object from the original response string. 
	 * @param	stage	stage of a mechanism
	 * @return	array of int containing the type of pericyclic mechanism, 
	 * the number of electrons in the pericyclic transition state, 
	 * the number of atoms in the pericyclic transition state, 
	 * the size of one component of a sigmatropic rearrangement or cycloaddition, 
	 * the size of the other component of a sigmatropic rearrangement or 
	 * cycloaddition, and the index of the stage in which the mechanistic 
	 * step occurs
	public int[] isPericyclicNew(MechStage stage) {
		final String SELF = "isPericyclic: ";
		// MechStage stage = (MechStage) origStage.clone();
		final Molecule allMol = stage.getFusedMolecule();
		final int numFlows = stage.getNumFlows();
		debugPrint(SELF + "stage has ", numFlows, 
				" flow arrows, ", allMol.getAtomCount(), " atoms.");
		boolean sigmaBondMakesOrBreaks = false;
		// make list of atoms touched by flows
		final List<MolAtom> reactiveAtoms = new ArrayList<MolAtom>();
		final List<MolBond> madeSigmaBonds = new ArrayList<MolBond>();
		for (final MechFlow flow : stage.getFlows()) {
			final MolAtom[] srcAtoms = flow.getSrcAtoms();
			if (!reactiveAtoms.contains(srcAtoms[0])) 
				reactiveAtoms.add(srcAtoms[0]);
			if (flow.srcIsBond()) {
				if (!reactiveAtoms.contains(srcAtoms[1])) 
					reactiveAtoms.add(srcAtoms[1]);
				if (flow.getSrcBond().getType() == 1) 
					sigmaBondMakesOrBreaks = true; 
			} // if source is bond
			final MolAtom[] sinkAtoms = flow.getSinkAtoms();
			if (!reactiveAtoms.contains(sinkAtoms[0])) 
				reactiveAtoms.add(sinkAtoms[0]);
			if (!flow.sinkIsAtom()) {
				if (!reactiveAtoms.contains(sinkAtoms[1]))
					reactiveAtoms.add(sinkAtoms[1]);
				if (flow.sinkIsIncipBond()) {
					sigmaBondMakesOrBreaks = true;
					// make incipient bond into true bond
					final MolBond newBond = 
							new MolBond(sinkAtoms[0], sinkAtoms[1]);
					allMol.add(newBond);
					madeSigmaBonds.add(newBond);
				} // if incipient bond
			} // if sink is bond or incipient bond
		} // for each flow
		if (!sigmaBondMakesOrBreaks) { // it's a resonance structure
			debugPrint("No sigma bonds making or breaking; not a "
					+ "pericyclic mechanism.");
			return new int[NUM_PERI_DATA];
		} // if no sigma bond makes or breaks

		// remove all atoms not touched by flow arrows
		final int atomCt = allMol.getAtomCount();
		for (int atomIndex = atomCt - 1; atomIndex >= 0; atomIndex--) {
			final MolAtom atom = allMol.getAtom(atomIndex);
			if (!reactiveAtoms.contains(atom)) { 
				debugPrint("removing unreactive atom ", 
						atom, atomIndex + 1);
				allMol.removeAtom(atom); // also removes bonds to this atom
			}
		} // for each atom
		debugPrint("The reactive atoms in the molecule are: ", allMol);
		final Ring countRings = new Ring();
		countRings.setMolecule(allMol);
		final int atomsElecsType = countRings.ringAtomCount();
		debugPrint("ringsize among reactive atoms = ", atomsElecsType);
		if (atomsElecsType < 3) {  // it's not pericyclic
			debugPrint("No cyclic array among reactive atoms; not "
					+ "a pericyclic mechanism.");
			return new int[NUM_PERI_DATA];
		} // if no cyclic array

		// the reaction is pericyclic.
		// determine number of electrons in pericyclic array
		int eCount = 0;
		int elecsInSigmaBondMakes = 0;
		int elecsInSigmaBondBreaks = 0;
		debugPrint("now stage has ", 
				allMol.getAtomCount(), " atoms.");
		// record bonds that are making/breaking so as to break 
		// them later to get component sizes (if appropriate)
		final List<MolBond> changeSigBonds = new ArrayList<MolBond>();
		for (int flIndex = numFlows - 1; flIndex >= 0; flIndex--) {
			final MechFlow flow = stage.getFlow(flIndex);
			boolean sourceInArray = false;
			boolean sourceIsSingleBond = false;
			MolBond sourceBond = null;
			MolBond sinkNewBond = null;
			if (flow.srcIsBond()) {
				sourceBond = flow.getSrcBond();
				debugPrint("source of flow ", flIndex, 
						" is bond ", sourceBond.getAtom1(), 
						allMol.indexOf(sourceBond.getAtom1()) + 1, 
						"-", sourceBond.getAtom2(), 
						allMol.indexOf(sourceBond.getAtom2()) + 1); 
				sourceInArray = !countRings.isChainBond(
						allMol.indexOf(sourceBond));
				sourceIsSingleBond = (sourceBond.getType() == 1); 
			} else { // source is a MolAtom
				final MolAtom atom = flow.getSrcAtom();
				final int atomIndex = allMol.indexOf(atom);
				debugPrint("source of flow ", flIndex, 
						" is atom ", atom, atomIndex + 1); 
				sourceInArray = !countRings.isChainAtom(atomIndex);
			} // if source type
			boolean sinkInArray = false;
			if (flow.sinkIsBond()) {
				sinkNewBond = flow.getSinkBond();
				debugPrint("sink of flow ", flIndex, " is ", 
						(madeSigmaBonds.contains(sinkNewBond) 
						 	? "incipient bond " : "bond "), 
						sinkNewBond.getAtom1(), 
						allMol.indexOf(sinkNewBond.getAtom1()) + 1, 
						"-", sinkNewBond.getAtom2(), 
						allMol.indexOf(sinkNewBond.getAtom2()) + 1); 
				sinkInArray = !countRings.isChainBond(
						allMol.indexOf(sinkNewBond));
			} else { // sink is a MolAtom
				final MolAtom atom = flow.getSinkAtom();
				final int atomIndex = allMol.indexOf(atom);
				debugPrint("sink of flow ", flIndex, 
						" is atom ", atom, atomIndex + 1); 
				sinkInArray = !countRings.isChainAtom(atomIndex);
			} // if sink type; no MolAtom[] necessary because 
			// bond has already been placed there
			if (sourceInArray && sinkInArray) {
				final int flowElecs = flow.getNumElectrons();
				eCount += flowElecs;
				if (sourceIsSingleBond) {
					elecsInSigmaBondBreaks += flowElecs;
					if ((sourceBond != null) &&
							!changeSigBonds.contains(sourceBond)) 
						changeSigBonds.add(sourceBond);
				} // if source is a single bond
				if (flow.sinkIsIncipBond()
						|| (sinkNewBond != null
								&& madeSigmaBonds.contains(sinkNewBond))) {
					elecsInSigmaBondMakes += flowElecs; 
					if ((sinkNewBond != null) &&
							!changeSigBonds.contains(sinkNewBond)) 
						changeSigBonds.add(sinkNewBond);
				} // if sink is an incipient bond
			} else { // source & sink not both in array
				stage.removeFlow(flow);
			} // if source & sink both in array
		} // for each flow
		debugPrint("elecsInSigmaBondBreaks = ", 
				elecsInSigmaBondBreaks, "elecsInSigmaBondMakes = ", 
				elecsInSigmaBondMakes);
		// now we can adjust for whether the noSupplyReceive rule is broken 
		final int supplyReceiveElecs = noSupplyReceive(stage);
		if (supplyReceiveElecs != NO_VIOLATION) {
			debugPrint(SELF + "violation of no supply/receive rule; ", 
					supplyReceiveElecs, " electrons involved");  
			eCount = eCount - supplyReceiveElecs;
		} // if violation of noSupplyReceive rule
		// finished with molecule manipulations; now can calculate type of
		// pericyclic reaction
		return createPeriOutput(allMol, changeSigBonds, atomsElecsType, eCount, 
			elecsInSigmaBondBreaks, elecsInSigmaBondMakes);
	} // isPericyclicNew(MechStage)
	 */

} // MechRuleFunctions
