package com.epoch.mechanisms;

import chemaxon.formats.MolFormatException;
import chemaxon.marvin.io.MolExportException;
import chemaxon.sss.search.SearchException;
import chemaxon.struc.MDocument;
import chemaxon.struc.MObject;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import chemaxon.struc.MoleculeGraph;
import chemaxon.struc.graphics.MEFlow;
import com.epoch.chem.MolString;
import com.epoch.evals.impl.chemEvals.chemEvalConstants.MapConstants;
import com.epoch.mechanisms.mechConstants.MechConstants;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** Wraps a response mechanism, and includes methods for analyzing its validity. */
public class Mechanism implements MapConstants, MechConstants {
	
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
	/** The parsed mechanism. */
	transient public MechParser parsedMech = null; 
	/** Has the mechanism been parsed? */
	transient public boolean initialized = false; 
	/** Stores any MechError that may have been thrown while parsing. */
	transient public MechError errorObject = null;

	//--------------------------------------------------------------------
	//					constructors
	//--------------------------------------------------------------------
	/** Constructor. */
	public Mechanism() {
		// intentionally empty
	} // Mechanism()
	
	/** Constructor. 
	 * @param	respMRV	MRV representation of the student's response 
	 * @throws	MolFormatException	if molecule can't be imported
	 */
	public Mechanism(String respMRV) throws MolFormatException { 
		final String SELF = "Mechanism.init: ";
		debugPrint("Entering mechanism initialization...");
		if (initialized) {
			errorObject = new MechError(SELF,
					"Tried to reinitialize mechanism. Please report this "
					+ "error to the webmaster.\n");
			return;
		} // if already initialized
		try {
			parsedMech = new MechParser(respMRV); 
			initialized = true;
		} catch (MechError e) {
			errorObject = e;
		} 
		debugPrint(SELF + "initialization complete.");
	} // Mechanism(String)

	//--------------------------------------------------------------------
	//  				short get methods
	//--------------------------------------------------------------------
	/** Gets the number of reaction (not resonance) arrows.
	 * @return number of reaction arrows
	 */
	public int getNumReactionArrows() 		{ return parsedMech.numReactionArrows; }
	/** Gets the number of resonance arrows.
	 * @return number of resonance arrows
	 */
	public int getNumResonanceArrows() 		{ return parsedMech.numResonanceArrows; }
	/** Gets the topology of the parsed mechanism. 
	 * @return	int representing the topology, including an invalid one 
	 */
	public int getTopology() 				{ return parsedMech.topology; } 
	/** Gets the number of stages in the mechanism.
	 * @return the number of stages in the mechanism
	 */
 	public int getNumStages() 				{ return parsedMech.getNumStages(); } 
	/** Gets the stages in the mechanism whose contents were merged
	 * into other stages.
	 * @return the stages in the mechanism whose contents were merged
	 * into other stages
	 */
 	public List<MechStage> getRemovedStages()	{ return parsedMech.getRemovedStages(); } 
	/** Gets the number of stages in the mechanism whose contents were merged
	 * into other stages.
	 * @return the number of stages in the mechanism whose contents were merged
	 * into other stages
	 */
 	public int getNumRemovedStages()	 	{ return parsedMech.getNumRemovedStages(); } 
	/** Gets the index of the first cyclic stage in a mechanism; -1 if it's
	 * linear.
	 * @return	index of the first cyclic stage in a mechanism
	 */
 	public int getFirstCyclicStageIndex()	{ return parsedMech.firstCyclicStageIndex; }
	/** Gets a copy of the MDocument of the mechanism.
	 * @return	a copy of the MDocument of the mechanism
	 */
	public MDocument getMDocCopy()			{ return parsedMech.getMDocCopy(); }
	/** Gets the MRV description of the mechanism.
	 * @return	MRV of the mechanism
	 */
	public String getMRV()					{ return parsedMech.getMRV(); }
	/** Gets the number of boxes that the student drew.  Corresponds to the
	 * number of stages in the mechanism plus any branched-off stages (from 
	 * cyclic mechanisms) that the student drew.
	 * @return number of boxes that the student drew
	 */
	public int getNumBoxes() 	{ return getNumStages() + getNumRemovedStages(); }
	/** Gets a stage of the mechanism.
	 * @param	stageNum	0-based index of the stage
	 * @return a stage in the mechanism
	 */
 	public MechStage getStage(int stageNum) { 
		return (MathUtils.inRange(stageNum, new int[] {0, getNumStages() - 1})
				? parsedMech.getStage(stageNum) : null); 
	} // getStage(int)

	//--------------------------------------------------------------------
	//  				checkFlowsValid
	//--------------------------------------------------------------------
	/** For all stages, are their contents composed of starting materials
	 * and products of electron flows of their previous stages?  Throws error 
	 * if false, does nothing otherwise.
	 * @param	startMol	permissible starting materials for this
	 * mechanism as a single Molecule
	 * @param	flags	sets resonance permissiveness for starting materials, 
	 * stereochemistry leniency for all compounds
	 * @throws	SearchException	if compounds cannot be compared 
	 * @throws	MechFormatException	if the parsed mechanism is not
	 * self-consistent
	 * @throws	MechError	if the electron-flow arrows do not flow logically
	 */
	public void checkFlowsValid(Molecule startMol, int flags) 
			throws SearchException, MechFormatException, MechError {
		final String SELF = "Mechanism.checkFlowsValid: ";
		debugPrint(SELF + "startMol: ", startMol);
		final MechValidFlows mechAnalyzer = 
				new MechValidFlows(this, startMol, flags);
		mechAnalyzer.checkFlowsValid();
	} // checkFlowsValid(Molecule, int)

	//--------------------------------------------------------------------
	//  				initiationOK
	//--------------------------------------------------------------------
	/** Is there an initiation, and, if so, does the initiator or any 
	 * portion of the initiator appear as a starting material or intermediate 
	 * in the propagation?
	 * @param	initiator	initiator of this mechanism
	 * @param	flags	sets resonance permissiveness for starting materials, 
	 * stereochemistry leniency for all compounds
	 * @throws	MechError	if the electron-flow arrows do not flow logically
	 * @throws	MechFormatException	if the parsed mechanism is not
	 * self-consistent
	 * @throws	SearchException	if compounds cannot be compared 
	 */
	public void initiationOK(Molecule initiator, int flags) 
			throws MechError, MechFormatException, SearchException {
		if (getTopology() != CYCLIC || getFirstCyclicStageIndex() == 0) {
			throw new MechError(NO_INITIATION);
		} // if there's no initiation
		final MechValidFlows mechAnalyzer = 
				new MechValidFlows(this, initiator, flags);
		mechAnalyzer.initiationOK();
	} // initiationOK(Molecule, int)

	//----------------------------------------------------------------------
	//					getAllResponseProducts
	//----------------------------------------------------------------------
	/** Gets all the products of the student's mechanism. 
	 * @param	flags	whether resonance structures, stereoisomers are treated 
	 * as equivalent
	 * @return	array of compounds produced by electron-flow arrows and not
	 * touched by them
	 * @throws	SearchException	if compounds cannot be compared 
	 */
	public Molecule[] getAllResponseProducts(int flags)
			 throws SearchException {
		final String SELF = "Mechanism.getAllResponseProducts: ";
		Molecule[] allProducts = null;
		classifyMolecules(flags);
		for (final MechStage stage : parsedMech.getStages()) {
			final Molecule[] stageProducts = stage.getResponseProducts();
			allProducts = MechSet.union(stageProducts, allProducts, flags);
		} // for each stage
		debugPrint(SELF + "all response products: ", allProducts);
		return allProducts;
	} // getAllResponseProducts()

	//----------------------------------------------------------------------
	//					getAllResponseStarters
	//----------------------------------------------------------------------
	/** Gets all the starting materials of the student's mechanism. 
	 * @param	flags	whether resonance structures, stereoisomers are treated
	 * as equivalent.
	 * @return	array of compounds not produced by electron-flow arrows
	 * @throws	SearchException	if compounds cannot be compared 
	 */
	public Molecule[] getAllResponseStarters(int flags)
			throws SearchException {
		final String SELF = "Mechanism.getAllResponseStarters: ";
		Molecule[] allStarters = null;
		classifyMolecules(flags);
		for (final MechStage stage : parsedMech.getStages()) {
			final Molecule[] stageStarters = stage.getResponseStarters();
			allStarters = MechSet.union(stageStarters, allStarters, flags);
		} // for each stage
		debugPrint(SELF + "all response starters: ", allStarters);
		return allStarters;
	} // getAllResponseStarters()

	//----------------------------------------------------------------------
	//					 getAllResponseIntermediates 
	//----------------------------------------------------------------------
	/** Gets all the intermediates of the student's mechanism. 
	 * @param	flags	whether resonance structures, stereoisomers are treated
	 * as equivalent.
	 * @return	array of compounds produced by electron-flow arrows and also
	 * touched by them
	 * @throws	SearchException	if compounds cannot be compared 
	 */
	public Molecule[] getAllResponseIntermediates(int flags)
			throws SearchException {
		final String SELF = "Mechanism.getAllResponseIntermediates: ";
		Molecule[] allIntermediates = null;
		classifyMolecules(flags);
		for (final MechStage stage : parsedMech.getStages()) {
			final Molecule[] stageIntermediates = 
					stage.getResponseIntermediates();
			allIntermediates = MechSet.union(stageIntermediates, 
					allIntermediates, flags);
		} // for each stage
		debugPrint(SELF + "all response intermediates: ", allIntermediates);
		return allIntermediates;
	} // getAllResponseIntermediates()

	//----------------------------------------------------------------------
	//					classifyMolecules
	//----------------------------------------------------------------------
	/** Classify all molecules in each stage as starting materials, products or
	 * intermediates.
	 * @param	flags	whether resonance structures, stereoisomers are treated 
	 * as equivalent
	 * @throws	SearchException	if compounds cannot be compared 
	 */
	private void classifyMolecules(int flags) throws SearchException {
		final String SELF = "Mechanism.classifyMolecules: ";
		if (parsedMech.allMoleculesClassified == flags) return;
		parsedMech.allMoleculesClassified = UNCHECKED;
		debugPrint(SELF + "classifying molecules in mechanism with MDoc:\n",
				parsedMech.getMDocCopy());
		final int numStages = getNumStages();
		Molecule[] prevProds = null;
		// find last index of linear portion of mechanism
		int lastLinearStageNum = (parsedMech.topology == LINEAR 
				? numStages : parsedMech.firstCyclicStageIndex) - 1;
		if (lastLinearStageNum < 0) {
			debugPrint(SELF + "unable to find cycle "
					 + "index! Contact a programmer to fix!!!");
			lastLinearStageNum = numStages - 1; // treat as linear
		} // if lastLinearStageNum < 0
		// process linear portion of mechanism
		for (int stageNum = 0; stageNum <= lastLinearStageNum; stageNum++) {
			final MechStage stage = getStage(stageNum);
			debugPrint(SELF + "classifying molecules in stage ", stageNum + 1);
			stage.classifyMolecules(prevProds, flags);
			try {
				processFlows(stage);
			} catch (MechError e) { // won't happen
				debugPrint(SELF + "MechError");
			}
			prevProds = MechSet.union(prevProds, stage.getFlowProducts(), flags);
		} // for each stage in the linear portion
		if (parsedMech.topology != LINEAR) {
			// collect calculated products of cyclic stages
			for (int stageNum = lastLinearStageNum + 1; 
					stageNum < numStages; stageNum++) {
				final MechStage stage = getStage(stageNum);
				debugPrint(SELF + "classifying molecules in stage ", 
						stageNum + 1);
				try {
					processFlows(stage);
				} catch (MechError e) { // won't happen
					debugPrint(SELF + "MechError");
				} // try
				prevProds = MechSet.union(prevProds, 
						stage.getFlowProducts(), flags);
			} // for each stage of the cyclic part
			// *now* classify response compounds of cyclic stages
			for (int stageNum = lastLinearStageNum + 1; 
					stageNum < numStages; stageNum++) {
				final MechStage stage = getStage(stageNum);
				debugPrint(SELF + "classifying molecules in stage ", 
						stageNum + 1);
				stage.classifyMolecules(prevProds, flags);
			} // for each stage of the cyclic part
		} // if mechanism is not linear
		parsedMech.allMoleculesClassified = flags;
	} // classifyMolecules()

	//----------------------------------------------------------------------
	//					processFlows
	//----------------------------------------------------------------------
	/** Calculates electron-flow products of the stage, makes sure there are no
	 * errors.
	 * @param	stage	the stage
	 * @throws	MechError	if the arrows are invalid
	 */
	public void processFlows(MechStage stage) throws MechError {
		final String SELF = "Mechanism.processFlows: ";
		final int stageNum = stage.getIndexInMech();
		int arrowsStatus = stage.getFlowArrowsStatus();
		if (arrowsStatus == UNCHECKED) {
			// populate electron-flow products
			stage.calculateFlowProducts();
			arrowsStatus = stage.getFlowArrowsStatus();
		}
		if (arrowsStatus != ARROWS_OK) {
			debugPrint(SELF + "Electron-flow arrow error in stage ", 
					stageNum + 1, ": ", stage.getFlowArrowsStatusStr());
			throwMechError(stageNum, arrowsStatus, 
					stage.getFlowArrowsProds(),
					stage.getOffendingFlowArrows());
		} // if the arrows are invalid
		else debugPrint(STAGE, stageNum + 1, 
				" (box ", stage.getBoxIndex() + 1, 
				") has no error in electron-flow arrows.");
	} // processFlows(MechStage)

	//----------------------------------------------------------------------
	//			substituteRGroups
	//----------------------------------------------------------------------
	/** Replaces generic numbered R groups in an author's mechanism with 
	 * instantiated R groups.  Must be done after parsing or atoms will stick
	 * outside of boxes.
	 * @param	rgMols	the R groups to be substituted
	 */
	public void substituteRGroups(Molecule[] rgMols) {
		for (final MechStage stage : parsedMech.getStages()) {
			stage.substituteRGroups(rgMols);
		} // for each stage
	} // substituteRGroups(Molecule[])

	//----------------------------------------------------------------------
	//			throwMechError, colorAndThrow
	//----------------------------------------------------------------------
	/** An error in the indicated stage warrants ACE throwing a MechError.
	 * @param	stageNum	0-based index of the stage that contains the error
	 * @param	kindOfError	represents the kind of error that the student made
	 * @throws	MechError	always
	 */
	public void throwMechError(int stageNum, int kindOfError) throws MechError {
		throwMechError(stageNum, kindOfError, NO_CALC_PRODS, NO_CALC_PRODS,
				new int[0]);
	} // throwMechError(int, int)

	/** An error in the indicated stage warrants ACE throwing a MechError.
	 * @param	stageNum	0-based index of stage that contains the error
	 * @param	kindOfError	represents the kind of error that the student made
	 * @param	calcdProds	the products calculated from this stage's
	 * electron-flow arrows
	 * @throws	MechError	always
	 */
	public void throwMechError(int stageNum, int kindOfError, String calcdProds) 
			throws MechError {
		throwMechError(stageNum, kindOfError, calcdProds, NO_CALC_PRODS,
				new int[0]);
	} // throwMechError(int, int, String)

	/** An error in the indicated stage warrants ACE throwing a MechError.
	 * @param	stageNum	0-based index of stage that contains the error
	 * @param	kindOfError	represents the kind of error that the student made
	 * @param	calcdProds	the products calculated from this stage's
	 * electron-flow arrows
	 * @param	highlightObjIndices	0-based indices of electron-flow arrows in the
	 * indicated stage that point to incipient bonds
	 * @throws	MechError	always
	 */
	public void throwMechError(int stageNum, int kindOfError, String calcdProds,
			int[] highlightObjIndices) throws MechError {
		throwMechError(stageNum, kindOfError, calcdProds, NO_CALC_PRODS, 
				highlightObjIndices);
	} // throwMechError(int, int, String, int[])

	/** An error in the indicated stage warrants ACE throwing a MechError.
	 * @param	stageNum	0-based index of stage that contains the error
	 * @param	kindOfError	represents the kind of error that the student made
	 * @param	calcdProds	the products calculated from this stage's
	 * electron-flow arrows
	 * @param	offendingCpds	the compounds drawn by the student that
	 * triggered this error
	 * @throws	MechError	always
	 */
	public void throwMechError(int stageNum, int kindOfError, String calcdProds,
			String offendingCpds) throws MechError {
		throwMechError(stageNum, kindOfError, calcdProds, offendingCpds, 
				new int[0]);
	} // throwMechError(int, int, String, String)

	/** An error in the indicated stage warrants ACE throwing a MechError.
	 * @param	stageNum	0-based index of stage that contains the error
	 * @param	kindOfError	represents the kind of error that the student made
	 * @param	calcdProds	the products calculated from this stage's
	 * electron-flow arrows
	 * @param	offendingCpds	the compounds drawn by the student that
	 * triggered this error
	 * @param	highlightObjIndices	0-based indices of electron-flow arrows in the
	 * indicated stage that point to incipient bonds
	 * @throws	MechError	always
	 */
	public void throwMechError(int stageNum, int kindOfError, String calcdProds,
			String offendingCpds, int[] highlightObjIndices) throws MechError {
		final String SELF = "Mechanism.throwMechError: ";
		debugPrint(SELF + "stage ", stageNum + 1, 
				", kindOfError = ", kindOfError); 
		final MechStage stage = getStage(stageNum);
		if (stage == null) { // unlikely
			debugPrint(SELF + "could not find stage ", stageNum + 1, 
					" among ", getNumStages(), " stage(s).");
			throw new MechError();
		}
		final int rectNum = stage.getBoxIndex();
		colorAndThrow(rectNum, kindOfError, calcdProds, offendingCpds,
				highlightObjIndices);
	} // throwMechError(int, int, String, String, int[])

	/** An error in the indicated stage warrants ACE throwing a MechError.
	 * @param	boxIndex	0-based index of the box of the stage that contains 
	 * the error
	 * @param	kindOfError	represents the kind of error that the student made
	 * @param	calcdProds	the products calculated from this stage's
	 * electron-flow arrows
	 * @param	offendingCpds	the compounds drawn by the student that
	 * triggered this error
	 * @throws	MechError	always
	 */
	public void colorAndThrow(int boxIndex, int kindOfError, String calcdProds,
			String offendingCpds) throws MechError {
		colorAndThrow(boxIndex, kindOfError, calcdProds, offendingCpds, 
				new int[0], parsedMech.getMDocCopy());
	} // colorAndThrow(int, int, String, String)

	/** An error in the indicated stage warrants ACE throwing a MechError.
	 * @param	boxIndex	0-based index of the box of the stage that contains 
	 * the error
	 * @param	kindOfError	represents the kind of error that the student made
	 * @param	calcdProds	the products calculated from this stage's
	 * electron-flow arrows
	 * @param	offendingCpds	the compounds drawn by the student that
	 * triggered this error
	 * @param	highlightObjIndices	0-based indices of electron-flow arrows in the
	 * indicated stage that point to incipient bonds
	 * @throws	MechError	always
	 */
	public void colorAndThrow(int boxIndex, int kindOfError, String calcdProds,
			String offendingCpds, int[] highlightObjIndices) throws MechError {
		colorAndThrow(boxIndex, kindOfError, calcdProds, offendingCpds, 
				highlightObjIndices, parsedMech.getMDocCopy());
	} // colorAndThrow(int, int, String, String, int[])

	/** An error in the indicated stage warrants ACE throwing a MechError.
	 * @param	boxIndex	0-based index of the box of the stage that contains 
	 * the error
	 * @param	kindOfError	represents the kind of error that the student made
	 * @param	calcdProds	the products calculated from this stage's
	 * electron-flow arrows
	 * @param	offendingCpds	the compounds drawn by the student that
	 * triggered this error
	 * @param	newMDoc	the MDocument to color
	 * @throws	MechError	always
	 */
	public void colorAndThrow(int boxIndex, int kindOfError, String calcdProds, 
			String offendingCpds, MDocument newMDoc) throws MechError {
		colorAndThrow(boxIndex, kindOfError, calcdProds, offendingCpds, 
				new int[0], parsedMech.getMDocCopy());
	} // colorAndThrow(int, int, String, String, MDocument)

	/** An error in the indicated stage warrants ACE throwing a MechError.
	 * @param	boxIndex	0-based index of the box of the stage that contains 
	 * the error
	 * @param	kindOfError	represents the kind of error that the student made
	 * @param	calcdProds	the products calculated from this stage's
	 * electron-flow arrows
	 * @param	offendingCpds	the compounds drawn by the student that
	 * triggered this error
	 * @param	highlightObjIndices	0-based indices of atoms or flow arrows that
	 * need to be highlighted
	 * @param	newMDoc	the MDocument to color
	 * @throws	MechError	always
	 */
	public void colorAndThrow(int boxIndex, int kindOfError, String calcdProds, 
			String offendingCpds, int[] highlightObjIndices, MDocument newMDoc) 
			throws MechError {
		final String SELF = "Mechanism.colorAndThrow: ";
		try {
			/* debugPrint(SELF + "stage box index ", boxIndex + 1, 
					", kindOfError = ", kindOfError, ", calcdProds = ",
					calcdProds, ", offendingCpds = ", offendingCpds,
					", 0-based highlightObjIndices = ", highlightObjIndices); /**/
			// debugPrint(SELF, "working on mDoc:\n", newMDoc);
			MolString.colorMObject(newMDoc, boxIndex);
			if (!Utils.isEmpty(highlightObjIndices)) {
				if (Utils.among(kindOfError, FLOWS_GIVE_NO_PRODS,
						NOT_PROD_NOR_STARTER, NOT_PROD_NOR_STARTER_1ST_CYCLIC)) {
					/* MolString.colorMObjects(newMDoc, highlightObjIndices, 
							Color.GREEN); /**/
					for (final int objIndex : highlightObjIndices) {
						final MObject obj = newMDoc.getObject(objIndex);
						/* debugPrint(SELF + "selecting object ", objIndex + 1,
								", ", obj instanceof MEFlow ? "an e-flow arrow"
								: obj instanceof MRectangle ? "a box"
								: obj instanceof MPolyline ? "a reaction arrow"
								: "some kind of MObject"); /**/
						obj.setSelected(true);
					} // for each object to select
				} // if kindOfError
				selectAtoms(newMDoc, highlightObjIndices, kindOfError);
			} // if there are flows to color
			final String mrv = MolString.toString(newMDoc, MRV);
			// debugPrint(SELF, "recolored MRV:\n", mrv);
			throw new MechError(mrv, kindOfError, calcdProds, offendingCpds,
					highlightObjIndices);
		} catch (MolExportException e) {
			System.out.println(SELF + "MolExportException; "
					+ "cannot export modified document");
			throw new MechError(parsedMech.getMRV(), kindOfError);
		} // try
	} // colorAndThrow(int, int, String, String, int[], MDocument)

	/** Select certain atoms for certain kinds of electron-flow arrow errors. 
	 * For display in versions of Marvin 5.8 (?) and following, it is not 
	 * necessary to set the SELECTED molecule property.
	 * @param	mdoc	the MDocument
	 * @param	highlightObjIndices	0-based indices of atoms or flow arrows that
	 * need to be highlighted
	 * @param	kindOfError	the kind of error
	 * @throws	MolExportException	if the SELECTED property of the molecule
	 * can't be set
	 */
	private void selectAtoms(MDocument mdoc, int[] highlightObjIndices, 
			int kindOfError) throws MolExportException {
		final String SELF = "Mechanism.selectAtoms: ";
		if (kindOfError != FLOWS_GIVE_NO_PRODS 
				|| highlightObjIndices.length == 1) {
			final MoleculeGraph molG = mdoc.getMainMoleculeGraph();
			final List<Integer> atomIndices = new ArrayList<Integer>();
			if (Utils.among(kindOfError, FLOWS_GIVE_NO_PRODS,
					NOT_PROD_NOR_STARTER, NOT_PROD_NOR_STARTER_1ST_CYCLIC)) {
				// object indices are for flow arrows
				final MEFlow flow = 
						(MEFlow) mdoc.getObject(highlightObjIndices[0]);
				final MolAtom[] atoms = flow.getSinkAtoms();
				for (final MolAtom atom : atoms) {
					atom.setSelected(true);
					atomIndices.add(Integer.valueOf(molG.indexOf(atom) + 1));
				} // for each atom of source
			} else {
				// object indices are for atoms
				for (final int atomIndex : highlightObjIndices) {
					final MolAtom atom = molG.getAtom(atomIndex);
					atom.setSelected(true);
					atomIndices.add(Integer.valueOf(atomIndex + 1));
				} // for each atom to highlight
			} // if error
			debugPrint(SELF + "selected atoms: ", atomIndices);
			/* ChemUtils.setProperty((Molecule) molG, SELECTED, 
					Utils.join(atomIndices, SEL_DIV)); /**/
		} // if error or highlightObjIndices.length
	} // selectAtoms(MDocument, int[], int)

	//----------------------------------------------------------------------
	//							isEqualTo
	//----------------------------------------------------------------------	
	/** Gets whether a mechanism is equal to this one.  Two mechanisms are equal 
	 * if they have equal stages in identical order connected by identical
	 * arrows.  This definition of equality is very strict; for
	 * example, a mechanism that shows Br<sup>&minus;</sup> as a coproduct of a
	 * step will be judged different from one that does not.
	 * @param	other	another mechanism
	 * @return	true if the mechanism equals this one
	 */
	public boolean isEqualTo(Mechanism other) {
		final String SELF = "Mechanism.isEqualTo: ";
		// low-hanging fruit: count stages & arrows, compare topology
		final int numStages = getNumStages();
		if (other == null) {
			debugPrint(SELF + "mechanisms are different: second mechanism is null");
			return false;
		} else if (!initialized || !other.initialized) {
			debugPrint(SELF + "mechanisms are different: initializations are ",
					initialized, " and ", other.initialized);
			return false;
		} else if (numStages != other.getNumStages()
				|| getTopology() != other.getTopology()
				|| getFirstCyclicStageIndex() != other.getFirstCyclicStageIndex()
				|| getNumReactionArrows() != other.getNumReactionArrows()
				|| getNumResonanceArrows() != other.getNumResonanceArrows()) {
			debugPrint(SELF + "mechanisms are different:\nnumStages are ", 
					numStages, " and ", other.getNumStages(),
					"\ntopologies are ", getTopology(), " and ", 
					other.getTopology(), "\nfirst cyclic stage indices are ", 
					getFirstCyclicStageIndex(), " and ", 
					other.getFirstCyclicStageIndex(),
					"\nnumReactionArrows are ", getNumReactionArrows(), 
					" and ", other.getNumReactionArrows(),
					"\nnumResonanceArrows are ", getNumResonanceArrows(), 
					" and ", other.getNumResonanceArrows());
			return false;
		} // if counts are different
		// compare stages in turn
		for (int stageNum = 0; stageNum < numStages; stageNum++) {
			final MechStage myStage = getStage(stageNum);
			final MechStage itsStage = other.getStage(stageNum);
			final MechArrow myNextArrow = myStage.getArrowToNext();
			final MechArrow itsNextArrow = itsStage.getArrowToNext();
			if ((myNextArrow == null && itsNextArrow != null)
					|| (myNextArrow != null 
						&& !myNextArrow.equals(itsNextArrow))) {
				debugPrint(SELF + "mechanisms are different: stages ", 
						stageNum + 1, " have different kinds of arrows "
						+ "pointing to the next stage; stage 1 arrow is ", 
						(myNextArrow == null ? "null"
							: myNextArrow.isResonant() ? "resonant"
							: "a regular reaction arrow"), 
						", whereas stage 2 arrow is ", 
						(itsNextArrow == null ? "null" 
							: itsNextArrow.isResonant() ? "resonant"
							: "a regular reaction arrow"));
				return false;
			} else if (!myStage.isEqualTo(itsStage)) {
				debugPrint(SELF + "stages ", stageNum + 1, 
						" have different contents.");
				return false;
			} // if stages are different
		} // for each stage
		debugPrint(SELF + "mechanisms are identical in all respects.");
		return true;
	} // isEqualTo(Mechanism)

} // Mechanism
