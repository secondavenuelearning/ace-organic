package com.epoch.mechanisms;

import chemaxon.sss.search.SearchException;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import chemaxon.struc.Molecule;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.StereoFunctions;
import com.epoch.mechanisms.mechConstants.MechConstants;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** Analyzes the validity of a mechanism. */
class MechValidFlows implements MechConstants {
	
	private static void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	//----------------------------------------------------------------------
	// 					shorthands
	//----------------------------------------------------------------------
	/** Used in debugging output. */
	private static final String STAGE = "Stage "; 

	//--------------------------------------------------------------------
	//					members
	//--------------------------------------------------------------------
	/** The mechanism being analyzed. */
	transient final private Mechanism mech;
	/**	Sets resonance leniency for starting materials, stereochemistry 
	 * leniency for all compounds. */
	transient final private int flags;
	/**	Permissible starting materials for this mechanism. */
	transient final private Molecule[] startMols;
	/**	Accumulated products of this mechanism. */
	transient final private List<Molecule> allPrevProds = new ArrayList<Molecule>();
	/** Number of stages in the mechanism. */
	transient final private int numStages;
	/** True if the mechanism has a linear part. */
	transient final private boolean hasLinearPart;
	/** True if the mechanism has a cyclic part. */
	transient final private boolean hasCyclicPart;
	/** 0-based index of last stage in linear part of mechanism (-1 if none). */
	transient final private int lastLinearIndex;
	/** 0-based index of first stage in cyclic part of mechanism (-1 if none). */
	transient final private int firstCyclicIndex;
	/** Current stage of the mechanism. */
	transient private MechStage currentStage;

	//--------------------------------------------------------------------
	//					constructor
	//--------------------------------------------------------------------
	/** Constructor. 
	 * @param	mechanism	the parsed mechanism whose validity is being
	 * assessed
	 * @param	startMolecule	permissible starting materials for this
	 * mechanism as a single Molecule
	 * @param	leniency	sets resonance leniency for starting 
	 * materials, stereochemistry leniency for all compounds
	 * @throws	MechFormatException	if the mechanism has cyclic part but can't
	 * find first stage of cyclic part
	 */
	MechValidFlows(Mechanism mechanism, Molecule startMolecule, 
			int leniency) throws MechFormatException {
		final String SELF = "MechValidFlows: ";
		mech = mechanism;
		flags = leniency;
		numStages = mech.getNumStages();
		hasCyclicPart = mech.getTopology() != LINEAR;
		firstCyclicIndex = mech.getFirstCyclicStageIndex();
		if (hasCyclicPart && firstCyclicIndex < 0) { // shouldn't happen
			debugPrint(SELF + "unable to find first cyclic stage of "
					+ "cyclic mechanism.");
			throw new MechFormatException("Mechanism ought to be "
					+ "cyclic, but firstCyclicIndex is -1.");
		} // if mechanism has cyclic part but can't find first stage of cyclic part
		lastLinearIndex = (hasCyclicPart ? firstCyclicIndex : numStages) - 1;
		hasLinearPart = lastLinearIndex >= 0;
		debugPrint(SELF, numStages, " stages in mechanism.");
		final Molecule startMol = startMolecule.clone();
		debugPrint(SELF + "startMol = ", startMol);
		startMol.ungroupSgroups(SHORTCUT_GROUPS);
		debugPrint(SELF + "after ungroup, startMol = ", startMol);
		ChemUtils.explicitizeHnoClone(startMol);
		debugPrint(SELF + "after explicitizing H, startMol = ", startMol);
		startMols = startMol.convertToFrags();
		debugPrint(SELF + "after fragmenting, startMols = ", startMols);
	} // MechValidFlows(Mechanism, Molecule, int)

	//--------------------------------------------------------------------
	//  				checkFlowsValid
	//--------------------------------------------------------------------
	/** For all stages, are their contents composed of starting materials
	 * and products of electron flows of their previous stages??
	 * @throws	MechError	if the electron-flow arrows do not flow logically
	 * @throws	SearchException	if compounds cannot be compared 
	 */
	void checkFlowsValid() throws MechError, SearchException {
		if (hasLinearPart) checkLinear();
		if (hasCyclicPart) checkCyclic();
	} // checkFlowsValid()

	//----------------------------------------------------------------------
	//					checkLinear
	//----------------------------------------------------------------------
	/** Checks the linear portion of the mechanism for validity, throwing an
	 * error if the mechanism is invalid. 
	 * @throws	MechError	if the electron-flow arrows do not flow logically
	 * @throws	SearchException	if compounds cannot be compared 
	 */
	private void checkLinear() throws MechError, SearchException {
		final String SELF = "MechValidFlows.checkLinear: ";
		debugPrint(SELF + "   lastLinearIndex = ", lastLinearIndex + 1);
		for (int stageNum = 0; stageNum <= lastLinearIndex; stageNum++) {
			currentStage = getStage(stageNum);
			debugPrint(SELF + "all previous products for stage ",
					stageNum + 1, " (box ", currentStage.getBoxIndex() + 1,
					"): ", allPrevProds);
			if (currentStage.getNumFlows() == 0 
					&& (hasCyclicPart || stageNum != lastLinearIndex)) {
				debugPrint(SELF + STAGE, stageNum + 1, 
						" (box ", currentStage.getBoxIndex() + 1, 
						") contains no electron-flow arrows.");
				mech.throwMechError(stageNum, NO_FLOW_ARROWS);
			} else if (currentStage.getNumFlows() != 0) {
				debugPrint(SELF + STAGE, stageNum + 1, 
						" (box ", currentStage.getBoxIndex() + 1, 
						") contains electron-flow arrows.");
			} else debugPrint(SELF + STAGE, stageNum + 1, 
					" (box ", currentStage.getBoxIndex() + 1, 
					") is last, shouldn't contain electron-flow arrows.");
			if (stageNum > 0) checkForPrevStageProds();
			checkForOffendingCpds();
			// add products of this stage to set of start materials 
			// (unless it is last stage of linear mechanism)
			if (stageNum < lastLinearIndex || hasCyclicPart) {
				mech.processFlows(currentStage);
				MechSet.addDifferent(allPrevProds, 
						currentStage.getFlowProducts(), 
						flags & ~RESON_LENIENT);
			} // if not at last stage of linear part of mechanism
		} // for each stage in linear portion
	} // checkLinear()

	//----------------------------------------------------------------------
	//					checkCyclic
	//----------------------------------------------------------------------
	/** Checks the cyclic portion of the mechanism for validity, throwing an
	 * error if the mechanism is invalid. 
	 * @throws	MechError	if the electron-flow arrows do not flow logically
	 * @throws	SearchException	if compounds cannot be compared 
	 */
	private void checkCyclic() throws MechError, SearchException {
		final String SELF = "MechValidFlows.checkCyclic: ";
		final MechStage firstCyclicStage = getStage(firstCyclicIndex);
		if (hasLinearPart) { // there's a linear part of cyclic mechanism
			// check that at least 1 product of last linear step 
			// is in 1st cycle step
			debugPrint(SELF + "Checking that at least one product of last linear "
					+ "step is in first cycle step.");
			currentStage = firstCyclicStage;
			checkForPrevStageProds();
		} // if there is a linear part of cyclic mechanism
		// add products of ALL remaining stages to list of valid starters
		debugPrint(SELF + "Entering cycle, getting all electron-flow products");
		for (int stageNum = lastLinearIndex + 1; stageNum < numStages; stageNum++) {
			currentStage = getStage(stageNum);
			if (currentStage.getNumFlows() == 0) {
				debugPrint(SELF + STAGE, stageNum + 1, 
						" (box ", currentStage.getBoxIndex() + 1, 
						") contains no electron-flow arrows.");
				mech.throwMechError(stageNum, NO_FLOW_ARROWS);
			} else debugPrint(SELF + STAGE, stageNum + 1, 
					" (box ", currentStage.getBoxIndex() + 1, 
					") contains electron-flow arrows.");
			mech.processFlows(currentStage);
			final Molecule[] stageProds = currentStage.getFlowProducts();
			debugPrint(SELF + STAGE, stageNum + 1, " contains these "
					+ "electron-flow products: ", stageProds);
			MechSet.addDifferent(allPrevProds, stageProds, flags & ~RESON_LENIENT);
		} // for each remaining stage in cycle
		debugPrint(SELF + "Valid start materials for cycle are ", startMols, 
				" and accumulated previous products are ", allPrevProds);
		debugPrint(SELF + "Checking cycle stages for valid flow products.");
		// 1st cycle stage checked separately later 
		for (int stageNum = lastLinearIndex + 2; stageNum < numStages; stageNum++) {
			currentStage = getStage(stageNum);
			checkForPrevStageProds();
			checkForOffendingCpds();
		} // for each cyclic stage except first
		// check that last cyclic stage gives at least 1 product in 1st stage of cycle
		currentStage = firstCyclicStage;
		checkForPrevStageProds(LAST_STAGE_IS_PREV);
		checkForOffendingCpds();
		// check that products of last stage in cycle, products of last linear
		// stage, and contents of first cyclic stage overlap
		if (hasLinearPart) {
			final Molecule[] lastLinearProds = getFlowProdsForStage(lastLinearIndex);
			final Molecule[] lastCyclicProds = getFlowProdsForStage(numStages - 1);
			final Molecule[] intersection = MechSet.intersection(
					lastLinearProds, lastCyclicProds, flags);
			if (Utils.isEmpty(intersection)) {
				debugPrint(SELF + "Last linear stage ", lastLinearIndex + 1, 
						" contains no products of electron-flow arrows" 
						+ " in common with last cyclic stage ", numStages, ".");
				debugPrint(SELF + "lastLinearProds: ", lastLinearProds);
				debugPrint(SELF + "lastCyclicProds: ", lastCyclicProds);
				mech.throwMechError(firstCyclicIndex, MALFORMED_CHAIN);
			} // if last linear and last cyclic stages don't produce common products 
			final Molecule[] firstCyclicContents = firstCyclicStage.getMoleculeArray(); 
			if (MechSet.overlapNull(firstCyclicContents, intersection, flags)) {
				debugPrint(SELF + "Intersection of products of last linear stage ", 
						lastLinearIndex + 1, " and last cyclic stage ", numStages, 
						" contains no compounds in common with contents of first "
						+ "cyclic stage ", firstCyclicIndex + 1, ".");
				debugPrint(SELF + "lastLinearProds: ", lastLinearProds);
				debugPrint(SELF + "lastCyclicProds: ", lastCyclicProds);
				debugPrint(SELF + "intersection: ", intersection);
				debugPrint(SELF + "firstCyclicContents: ", firstCyclicContents);
				mech.throwMechError(firstCyclicIndex, MALFORMED_CHAIN);
			} // if common products of last linear and last cyclic aren't in 1st cyclic
			debugPrint(SELF + "Last linear stage ", lastLinearIndex + 1, 
					" contains at least some products of electron-flow arrows" + 
					" in common with last cyclic stage ", numStages, 
					", and at least one of those common products is in first" 
					+ " cyclic stage ", firstCyclicIndex + 1, ".");
		} // if there's a linear part
	} // checkCyclic()

	//----------------------------------------------------------------------
	//					checkForPrevStageProds
	//----------------------------------------------------------------------
	/** Checks whether a stage contains products of electron-flow arrows of the
	 * previous stage.  If stage is first cyclic, the previous stage is the last
	 * cyclic stage, not the last linear stage.
	 * @throws	SearchException	if compounds cannot be compared 
	 * @throws	MechError	if the stage contains no products of electron-flow
	 * arrows of the previous stage
	 */
	private void checkForPrevStageProds() throws SearchException, MechError {
		checkForPrevStageProds(!LAST_STAGE_IS_PREV);
	} // checkForPrevStageProds()

	/** Checks whether a stage contains products of electron-flow arrows of the
	 * previous stage.  If stage is first cyclic, the previous stage is the last
	 * cyclic stage, not the last linear stage.
	 * @param	lastStageIsPrev	if true, the previous stage is the last stage
	 * of the mechanism
	 * @throws	SearchException	if compounds cannot be compared 
	 * @throws	MechError	if the stage contains no products of electron-flow
	 * arrows of the previous stage
	 */
	private void checkForPrevStageProds(boolean lastStageIsPrev) 
			throws SearchException, MechError {
		final String SELF = "MechValidFlows.checkForPrevStageProds: ";
		final int stageNum = currentStage.getIndexInMech();
		final Molecule[] stageContents = currentStage.getMoleculeArray();
		final boolean isFirstCyclic = stageNum == firstCyclicIndex;
		final int prevStageNum = (lastStageIsPrev 
				? numStages - 1 : stageNum - 1);
		final Molecule[] prevStageProds = getFlowProdsForStage(prevStageNum);
		debugPrint(SELF, (isFirstCyclic ? "First cyclic stage " : STAGE), 
				stageNum + 1, " (box ", currentStage.getBoxIndex() + 1, "):"); 
		debugPrint("\tstageContents: ", stageContents);
		final String prodOut = Utils.molArrayToString(prevStageProds, 
				getBestFormat(prevStageProds));
		debugPrint("\tprevStageProds: ", (Utils.isEmpty(prodOut) 
				? "[none]" : prodOut));
		if (MechSet.overlapNull(stageContents, prevStageProds, 
				flags & ~RESON_LENIENT)) {
			debugPrint(SELF, (isFirstCyclic ? "First cyclic stage " : STAGE), 
					stageNum + 1, " (box ", currentStage.getBoxIndex() + 1, 
					") contains no products of electron-flow arrows in prior",
					(lastStageIsPrev ? ", last cyclic" 
						: isFirstCyclic ? ", last linear" : ""), " stage ", 
					(isFirstCyclic && lastStageIsPrev 
						? numStages : stageNum), ".");
			final MechStage prevStage = mech.getStage(prevStageNum);
			final int[] flowsToIncipientBondsIndices = 
					prevStage.getFlowsToIncipientBondsIndices();
			mech.throwMechError(prevStageNum, FLOWS_GIVE_NO_PRODS, prodOut,
					flowsToIncipientBondsIndices);
		} else debugPrint(SELF, (isFirstCyclic ? "First cyclic stage " : STAGE), 
					stageNum + 1, " (box ", currentStage.getBoxIndex() + 1, 
				") contains at least some products of electron-flow"
				+ " arrows in prior",
				(lastStageIsPrev ? ", last cyclic" 
					: isFirstCyclic ? ", last linear" : ""), " stage ", 
				(lastStageIsPrev ? numStages : stageNum), ".");
	} // checkForPrevStageProds(boolean)

	//----------------------------------------------------------------------
	//					checkForOffendingCpds
	//----------------------------------------------------------------------
	/** Checks whether a stage contains compounds that are neither products of 
	 * electron-flow arrows of the previous stage nor legitimate starting 
	 * materials.  If stage is first cyclic, the previous stage is the last
	 * cyclic stage, not the last linear stage.
	 * @throws	SearchException	if compounds cannot be compared 
	 * @throws	MechError	if the stage contains no products of electron-flow
	 * arrows of the previous stage
	 */
	private void checkForOffendingCpds() throws SearchException, MechError {
		final String SELF = "MechValidFlows.checkForOffendingCpds: ";
		final int stageNum = currentStage.getIndexInMech();
		final Molecule[] stageContents = currentStage.getNormalizedMolArray();
		final Molecule[] stageNotStarters = 
				MechSet.getNonmembers(stageContents, startMols, flags);
		debugPrint(SELF + STAGE, stageNum + 1, " (box ", 
				currentStage.getBoxIndex() + 1, "):");
		debugPrint("\tstageContents (normalized): ", stageContents);
		debugPrint("\tstartMols: ", startMols);
		debugPrint("\taccumulated calculated products: ", allPrevProds);
		debugPrint("\tstage compounds that are not "
				+ "permissible starting materials: ", stageNotStarters);
		final int prodsFlags = (flags & ~RESON_LENIENT);
		final Molecule[] notProdsOrStarters = MechSet.getNonmembers(
				stageNotStarters, allPrevProds, prodsFlags);
		if (!Utils.isEmpty(notProdsOrStarters)) {
			final String notProdsOrStartersStr =
					Utils.molArrayToString(notProdsOrStarters, SMILES);
			debugPrint(SELF + STAGE, stageNum + 1, 
					" (box ", currentStage.getBoxIndex() + 1,
					") contains compound(s) ", notProdsOrStartersStr,
					" that is (are) neither valid starting material(s) "
					+ "nor product(s) calculated from electron-flow arrows.");
			if (firstCyclicIndex >= 0
					&& stageNum >= firstCyclicIndex) {
				// see if offending compound is actually in a branched-off
				// stage of cyclic mechanism; will throw an error if it is
				checkForBranchedStage();
			} // if stage is in cyclic part of mechanism
			if (stageNum == firstCyclicIndex) {
				final int lastLinearIndex = firstCyclicIndex - 1;
				final int lastCyclicIndex = numStages - 1;
				final Molecule[] bothPrevStageProds = MechSet.union(
						getFlowProdsForStage(lastLinearIndex),
						getFlowProdsForStage(lastCyclicIndex), flags);
				final int[] lastLinearFlowsToIncipientBondsIndices = 
						mech.getStage(lastLinearIndex
							).getFlowsToIncipientBondsIndices();
				final int[] lastCyclicFlowsToIncipientBondsIndices = 
						mech.getStage(lastCyclicIndex
							).getFlowsToIncipientBondsIndices();
				final int[] flowsToIncipientBondsIndices = 
						Utils.addAll(lastLinearFlowsToIncipientBondsIndices, 
								lastCyclicFlowsToIncipientBondsIndices);
				mech.throwMechError(stageNum, NOT_PROD_NOR_STARTER_1ST_CYCLIC,
						Utils.molArrayToString(makePretty(bothPrevStageProds),
							getBestFormat(bothPrevStageProds)), 
						notProdsOrStartersStr, flowsToIncipientBondsIndices);
			} else if (stageNum != 0) {
				final Molecule[] flowProds = 
						getFlowProdsForStage(stageNum - 1);
				final int[] flowsToIncipientBondsIndices = 
						mech.getStage(stageNum - 1
							).getFlowsToIncipientBondsIndices();
				mech.throwMechError(stageNum, NOT_PROD_NOR_STARTER,
						Utils.molArrayToString(makePretty(flowProds), 
							getBestFormat(flowProds)),
						notProdsOrStartersStr, flowsToIncipientBondsIndices);
			} else {
				mech.throwMechError(stageNum, NOT_STARTER, "", 
						notProdsOrStartersStr);
			}
		} else debugPrint(SELF + STAGE, stageNum + 1, 
				" (box ", currentStage.getBoxIndex() + 1, 
				") contains only valid starting materials", 
				(stageNum == 0 ? "." : " and products calculated "
						+ "from electron-flow arrows."));
	} // checkForOffendingCpds()

	//----------------------------------------------------------------------
	//					checkForBranchedStage
	//----------------------------------------------------------------------
	/** Finds a branched-off stage whose contents were copied into the current
	 * one, and sees if it contains a compound that is neither permissible
	 * starting material nor product of electron-flow arrows.
	 * @throws	SearchException	if compounds cannot be compared 
	 * @throws	MechError	if branched-off stage contains a compound that is
	 * neither permissible starting material nor product of electron-flow arrows
	 */
	private void checkForBranchedStage() throws SearchException, MechError {
		final String SELF = "MechValidFlows.checkForBranchedStage: ";
		final int stageNum = currentStage.getIndexInMech();
		final int currentBoxIndex = currentStage.getBoxIndex();
		for (final MechStage branchStage : mech.getRemovedStages()) {
			final int boxCopiedIntoIndex = 
					branchStage.getBoxIndexOfStageCopiedInto();
			if (boxCopiedIntoIndex == currentBoxIndex) {
				debugPrint(SELF + "Found a stage branching off this one.");
				final Molecule[] branchContents = 
						branchStage.getMoleculeArray();
				final Molecule[] branchNotStarters = 
						MechSet.getNonmembers(branchContents, startMols, flags);
				final int prodsFlags = (flags & ~RESON_LENIENT);
				final Molecule[] notProdsOrStarters = MechSet.getNonmembers(
						branchNotStarters, allPrevProds, prodsFlags);
				if (!Utils.isEmpty(notProdsOrStarters)) {
					final int branchBoxIndex = branchStage.getBoxIndex();
					debugPrint(SELF + "Offending compound(s) ",
							notProdsOrStarters, " is (are) actually in "
							+ "removed, branched-off stage with box ",
							branchBoxIndex + 1, ".");
					final boolean isFirstCyclic = 
							stageNum == firstCyclicIndex;
					final int prevIndex = (isFirstCyclic
							? numStages : stageNum) - 1;
					final MechStage prevStage = getStage(prevIndex); 
					final Molecule[] prevFlowProds =
							prevStage.getFlowProducts();
					final String prevFlowProdsStr = Utils.molArrayToString(
							prevFlowProds, getBestFormat(prevFlowProds));
					debugPrint(SELF + "Previous stage flow products are: ",
							(Utils.isEmpty(prevFlowProdsStr) ? "[none]" 
								: prevFlowProdsStr));
					mech.colorAndThrow(branchBoxIndex, NOT_PROD_NOR_STARTER,
							prevFlowProdsStr, Utils.molArrayToString(
								notProdsOrStarters, SMILES));
				} else {
					debugPrint(SELF + "Offending compound not in "
							+ "branched-off stage.");
					return;
				} // if offending compound is in branched-off stage
			} // if the removed stage was copied into the current one
		} // for each removed stage
		debugPrint(SELF + "Did not find a branched-off stage.");
	} // checkForBranchedStage()

	//--------------------------------------------------------------------
	//  	initiationOK, checkForInitiator, checkForInitiatorProds
	//--------------------------------------------------------------------
	/** Is there an initiation, and, if so, does the initiator or any portion of
	 * the initiator appear as a starting material or intermediate in the
	 * propagation?
	 * @throws	MechError	if the electron-flow arrows do not flow logically
	 * @throws	SearchException	if compounds cannot be compared 
	 */
	void initiationOK() throws MechError, SearchException {
		checkForInitiator();
		checkForInitiatorProds();
	} // initiationOK()

	/** Does the initiator appear as a starting material or intermediate in the
	 * propagation?
	 * @throws	MechError	if the electron-flow arrows do not flow logically
	 * @throws	SearchException	if compounds cannot be compared 
	 */
	private void checkForInitiator() throws MechError, SearchException {
		final String SELF = "MechValidFlows.checkForInitiator: ";
		final Molecule[] initiators = startMols;
		for (int stageNum = lastLinearIndex + 1; stageNum < numStages; stageNum++) {
			currentStage = getStage(stageNum);
			final Molecule[] stageCpds = currentStage.getNormalizedMolArray();
			debugPrint(SELF + STAGE, stageNum + 1, " contains these "
					+ "compounds: ", stageCpds);
			final Molecule[] intersection = 
					MechSet.intersection(initiators, stageCpds, flags);
			if (!Utils.isEmpty(intersection)) {
				debugPrint(SELF + STAGE, stageNum + 1, " contains these "
						+ "initiators: ", intersection);
				mech.throwMechError(stageNum, INITIATOR_IN_PROPAGATION);
			} // if one of the initiators is in the cyclic stage
		} // for each stage in cycle
	} // checkForInitiator()

	/** Does any portion of the initiator appear as a starting material or 
	 * intermediate in the propagation?
	 * @throws	MechError	if the electron-flow arrows do not flow logically
	 * @throws	SearchException	if compounds cannot be compared 
	 */
	private void checkForInitiatorProds() throws MechError, SearchException {
		final String SELF = "MechValidFlows.checkForInitiatorProds: ";
		final List<Molecule> cpdsFromInitiator = 
				Utils.molArrayToList(startMols);
		for (int stageNum = 0; stageNum < numStages; stageNum++) {
			currentStage = getStage(stageNum);
			final Molecule[] normalizedMols = 
					currentStage.getNormalizedMolArray();
			boolean containsInitiator = false;
			int molNum = 0;
			// set initiator property to true for each initiator in stage
			for (final Molecule normalizedMol : normalizedMols) {
				final int initiatorNum = MechSet.molInList(normalizedMol, 
						cpdsFromInitiator, flags);
				if (initiatorNum != NOT_FOUND) { // cpd is derived from initiator
					if (stageNum > lastLinearIndex) { // in cyclic part
						final int status = currentStage.getMolStatus(molNum);
						if (status != RESPONSE_PRODUCT) {
							debugPrint(SELF + STAGE, stageNum + 1, "in " 
									+ "cyclic part of mechanism has molecule ",
									currentStage.getMolecule(molNum),
									" derived from an initiator ",
									cpdsFromInitiator.get(initiatorNum),
									" and classified as a ",
									MechStage.statusToEnglish(status),
									"; throwing error.");
							mech.throwMechError(stageNum, 
									INITIATOR_IN_PROPAGATION);
						} // if the initiator-derived cpd is not only a product
					} // if we are in cyclic part of mechanism
					final Molecule origMol = currentStage.getMolecule(molNum);
					containsInitiator = true;
					setFromInitiator(origMol, currentStage);
					debugPrint(SELF + STAGE, stageNum + 1, " has molecule ",
							origMol, " derived from an initiator");
				} // if the stage molecule is derived from an initiator
				molNum++;
			} // for each molecule in stage
			if (containsInitiator) {
				// recreate stageXML, recalculate products to get which ones are
				// from initiator, add initiator-derived prods to list
				debugPrint(SELF + STAGE, stageNum + 1, " XML:\n", 
						currentStage.getStageXML());
				currentStage.calculateFlowProducts();
				final Molecule[] calcdProds = currentStage.getFlowProducts();
				debugPrint(SELF + STAGE, stageNum + 1, 
						" has calculated products ", calcdProds);
				for (final Molecule calcdProd : calcdProds) {
					if (isFromInitiator(calcdProd)) {
						debugPrint(SELF + "calculated product ", calcdProd,
								"is derived from initiator");
						cpdsFromInitiator.add(calcdProd);
					} // if any of calcdProd's atoms derive from the initiator
				} // for each calculated product
			} // if there's an initiator 
		} // for each stage in cycle
	} // checkForInitiatorProds()

	/** Finds the atoms in the stage molecule that correspond to the atoms in
	 * the initiator molecule, and sets their fromInitiator property to true. 
	 * @param	initiatorMol	an initiator molecule
	 * @param	stage	the stage containing the original atoms
	 */
	private void setFromInitiator(Molecule initiatorMol, MechStage stage) {
		for (final MolAtom initiatorAtom : initiatorMol.getAtomArray()) {
			final Integer initiatorOrigIndex = 
					(Integer) initiatorAtom.getProperty(ORIG_INDEX);
			final MolAtom stageAtom = 
					currentStage.getOrigAtomByIndex(initiatorOrigIndex);
			stageAtom.putProperty(FROM_INITIATOR, TRUE);
		} // for each atom
	} // setFromInitiator(Molecule, Molecule)

	/** Gets if the fromInitiator property of any atom in the molecule is true. 
	 * @param	mol	a molecule
	 * @return	true if the fromInitiator property of any atom in the molecule
	 * is true
	 */
	private boolean isFromInitiator(Molecule mol) {
		for (final MolAtom atom : mol.getAtomArray()) {
			if (TRUE.equals((String) atom.getProperty(FROM_INITIATOR)))
				return true;
		} // for each atom
		return false;
	} // isFromInitiator(Molecule)

	//--------------------------------------------------------------------
	//  				getStage
	//--------------------------------------------------------------------
	/** Gets a stage of the mechanism.
	 * @param	stageNum	0-based index of the stage
	 * @return a stage in the mechanism
	 */
 	private MechStage getStage(int stageNum) { 
		return mech.getStage(stageNum); 
	} // getStage(int) 

	//----------------------------------------------------------------------
	//					getFlowProdsForStage
	//----------------------------------------------------------------------
	/** Gets the flow products of a stage; returns empty if stageNum is
	 * invalid. 
	 * @param	stageNum	0-based index of the stage
	 * @return	the flow products as an array of Molecules
	 */
	private Molecule[] getFlowProdsForStage(int stageNum) {
		Molecule[] flowProds = new Molecule[0];
		if (MathUtils.inRange(stageNum, new int[] {0, numStages - 1})) {
			final MechStage aStage = getStage(stageNum);
			flowProds = aStage.getFlowProducts();
		}
		return flowProds;
	} // getFlowProdsForStage(int)

	//----------------------------------------------------------------------
	//					getBestFormat
	//----------------------------------------------------------------------
	/** Determines whether it's best to convert the molecules into SMILES or
	 * SMARTS format.
	 * @param	mols	array of molecules
	 * @return	SMARTS if any of the molecules have a query bond, SMARTS
	 * otherwise
	 */
	private String getBestFormat(Molecule[] mols) {
		boolean smilesOK = true;
		for (final Molecule mol : mols) {
			for (final MolBond bond : mol.getBondArray()) {
				if (!Utils.among(bond.getType(), 1, 2, 3, MolBond.AROMATIC,
						MolBond.COORDINATE)) {
					smilesOK = false;
					break;
				} // if the bond is a query type
			} // for each bond
			if (!smilesOK) break;
		} // for each molecule
		return (smilesOK ? SMILES : SMARTS);
	} // getBestFormat(Molecule[])

	//----------------------------------------------------------------------
	//					makePretty
	//----------------------------------------------------------------------
	/** Converts criss-cross representation of stereorandom double bonds into
	 * wavy-bond-to-ligand representation.
	 * Modifies the original.
	 * @param	products	an array of molecules
	 * @return	the modified molecules
	 */
	private Molecule[] makePretty(Molecule[] products) {
		StereoFunctions.allCrissCrossToWavy(products);
		return products;
	} // makePretty(Molecule[])

} // MechValidFlows

