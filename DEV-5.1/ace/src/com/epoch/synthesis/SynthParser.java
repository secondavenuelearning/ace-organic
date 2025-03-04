package com.epoch.synthesis;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.struc.DPoint3;
import chemaxon.struc.MDocument;
import chemaxon.struc.MObject;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import chemaxon.struc.MPoint;
import chemaxon.struc.graphics.MBracket;
import chemaxon.struc.graphics.MPolyline;
import chemaxon.struc.graphics.MRectangle;
import chemaxon.struc.graphics.MRectanglePoint;
import chemaxon.struc.graphics.MTextBox;
import chemaxon.struc.sgroup.Expandable;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolString;
import com.epoch.chem.Normalize;
import com.epoch.chem.VectorMath;
import com.epoch.synthesis.synthConstants.SynthConstants;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/** Parses a synthesis into stages, stores the parsed synthesis. */
class SynthParser implements SynthConstants {
	
	private void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	private void debugPrintMRV(Object... msg) { 
		// Utils.printToLog(msg, MRV);
	}

	//----------------------------------------------------------------------
	//						 members
	//----------------------------------------------------------------------		
	/** String representation of the original response. Includes both MRV and
	 * chosen reactions.  May be modified with color after parsing.  */
	private final String response;
	/** MRV part of the original response. */
	private transient String originalMRV;
	/** Contains the reaction IDs associated with each reaction number
	 * in the MRV.  */
 	private transient String rxnConditions;
	/** Original MRV as imported into Java; contains molecules and
	 * graphical objects. */
	private transient MDocument archiveDoc = null;
	/** Copy of original MRV as imported into Java; contains molecules and
	 * graphical objects.  Apparently the operations herein cause it to become
	 * unexportable.  */
	private transient MDocument synthDoc = null;
	/** All of the molecules of the original response in a single Molecule.
	 * Don't use after the synthesis has been parsed! */
	private transient Molecule importedMolecule = null;
	/** List of arrows in this synthesis. */
	private transient List<SynthArrow> arrows = null;
	/** List of stages in this synthesis. */
	private transient List<SynthStage> stages = null;
	/** 0-Based index of the stage containing the target of the synthesis. */
	transient int targetStageIndex = NOT_FOUND;
	
	//----------------------------------------------------------------------
	//						 constructors
	//----------------------------------------------------------------------		
	/** Constructor.
	 * @param	resp	MRV and chosen reactions of the response
	 * @param	emptyBoxOK	whether a box may be empty
	 * @throws	MolFormatException	if molecule can't be imported
	 * @throws	SynthError	if something goes wrong during parsing
	 */
	public SynthParser(String resp, boolean emptyBoxOK) 
			throws MolFormatException, SynthError {
		debugPrint("Entering SynthParser: emptyBoxOK = ", emptyBoxOK);
		response = resp;
		init();
		setupStages();
		extractArrows();
		extractMolecules(emptyBoxOK);
		extractRxnSelections();
		findTargetStageIndex();
		debugPrint("Parsing SynthParser complete.");
	} // SynthParser(String, boolean)

	//----------------------------------------------------------------------
	//							setParentSynth
	//----------------------------------------------------------------------
	/** Sets the parentSynth variable of each stage to a Synthesis. 
	 * @param	synth	a Synthesis object
	 * @throws	SynthError	if stages is null
	 */
	void setParentSynth(Synthesis synth) throws SynthError {
		if (stages == null) throw new SynthError();
		for (final SynthStage stage : stages) {
			stage.setParentSynth(synth);
		} // for each stage
	} // setParentSynth(Synthesis)

	//----------------------------------------------------------------------
	//							short get methods
	//----------------------------------------------------------------------
	/** Gets a stage of the synthesis.
	 * @param	stageNum	0-based number of the stage
	 * @return	the stage
	 */
	SynthStage getStage(int stageNum) { 
		return (MathUtils.inRange(stageNum, new int[] {0, stages.size()})
				? stages.get(stageNum) : null); 
	} // getStage(int)

	/** Gets all stages of the synthesis.
	 * @return	the stages
	 */
	List<SynthStage> getStages()		{ return stages; }
	/** Gets the number of arrows in the synthesis
	 * @return	thenumber of arrows 
	 */
	int getNumArrows()					{ return arrows.size(); }
	/** Gets a copy of the MDocument of the synthesis.
	 * @return	a copy of the MDocument of the synthesis
	 */
	MDocument getMDocCopy()				{ return (MDocument) archiveDoc.clone(); }
	/** Gets the original synthesis response.
	 * @return	the original synthesis response
	 */
	String getResponse()				{ return response; }
	/** Gets the MRV part of the original synthesis response.
	 * @return	the MRV part of the original synthesis response
	 */
	String getOriginalMRV()				{ return originalMRV; }
	/** Gets the reaction conditions of the synthesis response.
	 * @return	the reaction conditions of the synthesis response
	 */
	String getRxnConditions()			{ return rxnConditions; }

	//----------------------------------------------------------------------
	//								init
	//----------------------------------------------------------------------
	/** Extract MDocument (contains flows, arrows, etc.) and molecules
	 * from the given response.
	 * @throws	MolFormatException	if molecule can't be imported
	 * @throws	SynthError	if there are no rectangles or arrows
	 */
	private void init() throws MolFormatException, SynthError {
		final String SELF = "SynthParser.init: ";
		if (response.trim().endsWith(">")) {
			originalMRV = response;
			importedMolecule = MolImporter.importMol(response);
			rxnConditions = Synthesis.getRxnConditions(importedMolecule);
		} else {
			final String[] components = 
					Synthesis.getSynthesisComponents(response);
			originalMRV = components[STRUC];
			rxnConditions = components[RXNID];
			importedMolecule = MolImporter.importMol(originalMRV);
		} // if old format
		synthDoc = importedMolecule.getDocument();
		if (synthDoc == null)
			fail("Each set of reactants or products "
					+ "must be enclosed in a rectangle, and the "
					+ "rectangles must be connected by arrows.");
		MolString.uncolorMPolylines(synthDoc);
		if (!ChemUtils.getWhetherFromMarvinJS(importedMolecule)) {
			MolString.removeDuplicateObjects(synthDoc);
			debugPrint(SELF + "MDocument after removing duplicate "
					+ "objects from MarvinSketch drawing:\n", synthDoc);
		} // if molecule is from MarvinJS
		removeEmptyTextBoxes();
		archiveDoc = (MDocument) synthDoc.clone();
	} // init()

	//----------------------------------------------------------------------
	//							removeEmptyTextBoxes	
	//----------------------------------------------------------------------
	/** Removes empty textboxes from the MDocument.  The empty textboxes cause
	 * an error when loaded into Marvin 4.1.
	 */
	private void removeEmptyTextBoxes() {
		final int numObjects = synthDoc.getObjectCount();
		for (int objIndex = numObjects - 1; objIndex >= 0; objIndex--) {
			final MObject anObject = synthDoc.getObject(objIndex);		
			if (anObject instanceof MTextBox) {
				final String text = ((MTextBox) anObject).getText();
				if (Utils.isEmptyOrWhitespace(text)) {
					synthDoc.removeObject(anObject);
				} // if textbox is empty
			} // if object is a textbox
		} // for each object in the MDocument
	} // removeEmptyTextBoxes()

	//----------------------------------------------------------------------
	//							setupStages
	//----------------------------------------------------------------------
	/** Creates a stage for each box found in the MDocument.
	 * @throws	SynthError	if there are fewer than two boxes
	 */
	private void setupStages() throws SynthError {
		stages = new ArrayList<SynthStage>();
		final int numObjects = synthDoc.getObjectCount();
		for (int objIndex = 0; objIndex < numObjects; objIndex++) {
			final MObject anObject = synthDoc.getObject(objIndex);		
			if ((anObject instanceof MRectangle) &&
			   !(anObject instanceof MTextBox)) {
				stages.add(new SynthStage((MRectangle) anObject, objIndex));
			} // if object is a box
		} // for each object in the MDocument
		if (stages.size() < 2)
			fail("A synthesis must contain at least two boxes, one "
					+ "containing starting materials, and the other, "
					+ "products.");
	} // setupStages()
	
	//----------------------------------------------------------------------
	//							extractArrows
	//----------------------------------------------------------------------
	/** Extracts arrows from the MDocument, adds them to the list.
	 * @throws	SynthError	if arrow has no heads or two
	 */
	private void extractArrows() throws SynthError {
		arrows = new ArrayList<SynthArrow>();		
		for (int objIndex = 0;
				objIndex < synthDoc.getObjectCount(); objIndex++) {
			final MObject mObject = synthDoc.getObject(objIndex);
			if (MolString.isLineOrRxnArrow(mObject)) {
				if (MolString.isReactionArrow(mObject)) {
					final SynthArrow arrow =
							new SynthArrow((MPolyline) mObject, objIndex);
					if (arrow.hasWedgeAt(TAIL)) { // double-headed
						fail(objIndex, "Do not use resonance arrows in "
								+ "your response to a multistep synthesis "
								+ "question.");
					}
					arrows.add(arrow);
				} else { // not a reaction arrow
					fail(objIndex, "Please connect your rectangles "
							+ "with reaction arrows, not with lines.");
				} // if is an arrow
			} // if is a straight line or arrow
		} // for each object in the MDocument
		int arrowIndex = 0;
		for (final SynthArrow arrow : arrows) {
			final int objNum = arrow.getObjectNumber();
			debugPrint("Examining tail of arrow ", ++arrowIndex, 
					" with object number ", objNum + 1);
			final MPoint arrowTail = arrow.getPoint(0);	
			final int tailStageNum = getStageForArrowPoint(arrowTail);
			debugPrint("Examining head of same arrow ...");
			final MPoint arrowHead = arrow.getPoint(1);
			final int headStageNum = getStageForArrowPoint(arrowHead);
			if (Utils.among(NOT_FOUND, headStageNum, tailStageNum))
				fail(objNum, "All reaction arrows must originate from "
						+ "a box and point to a box.");
			if (Utils.among(FOUND_IN_2, headStageNum, tailStageNum))
				fail(objNum, "All reaction arrows must originate from and "
						+ "point to exactly one box.");
			if (headStageNum == tailStageNum)
				fail(objNum, "A reaction arrow must not be entirely contained "
						+ "in a single box. It must connect two boxes.");
			// record stages in SynthArrow
			debugPrint("Recording stages in SynthArrow...");
			final SynthStage tailStage = stages.get(tailStageNum);
			final SynthStage headStage = stages.get(headStageNum);
			arrow.setStagesPrevNext(tailStage, headStage);
			// record arrow in SynthStages
			debugPrint("Recording arrow in SynthStages...");
			// a stage may have multiple arrows pointing to it, but only one
			// pointing away
			headStage.addArrowFromPrev(arrow);
			headStage.addPrevStage(tailStage);
			if (tailStage.getArrowToNext() == null
					&& !tailStage.hasNextStage()) {
				tailStage.setArrowToNext(arrow);
				tailStage.setNextStage(headStage);
			} else {
				debugPrint("Oops! Stage with box object number ",
						tailStage.getBoxIndex() + 1,
						" already has an arrow pointing to the next stage.");
				fail(objNum, tailStage.getArrowToNext().getObjectNumber(),
						"No box may have more than one reaction arrow "
						+ "pointing away from it.");
			}
		} // for each arrow
	} // extractArrows()
	
	//----------------------------------------------------------------------
	//						getStageForArrowPoint
	//----------------------------------------------------------------------
	/** Gets 0-based index of stage with which the arrow endpoint is
	 * associated.  First checks whether the arrow endpoint coincides with
	 * an MRectangle point; if not, checks whether the arrow endpoint falls
	 * within a box physically.
	 * @param	point	a point in an MDocument
	 * @return	index of stage; NOT_FOUND for not found, FOUND_IN_2 for found
	 * in more than one stage
	 */
	private int getStageForArrowPoint(MPoint point) {
		final String SELF = "SynthParser.getStageForArrowPoint: ";
		if (point instanceof MRectanglePoint) {
			final MRectangle rect = ((MRectanglePoint) point).getParentRect();
			if (!(rect instanceof MTextBox) && !(rect instanceof MBracket)) {
				debugPrint(SELF + "arrow point is part of a box.");
				int foundStageIndex = NOT_FOUND;
				for (int stageNum = 0; stageNum < stages.size(); stageNum++)
					if (stages.get(stageNum).getBox() == rect) {
						if (foundStageIndex != NOT_FOUND) {
							debugPrint("Oops! arrow point also associated "
									+ "with box with object number ",
									stages.get(stageNum).getBoxIndex() + 1);
							return FOUND_IN_2;
						}
						foundStageIndex = stageNum;
						debugPrint("arrow point associated "
								+ "with box with object number ",
								stages.get(stageNum).getBoxIndex() + 1);
					} // if rectangle has already been assigned to a stage
				// for each stage
				return foundStageIndex;
			} // if rectangle is a regular rectangle
		} // if point instanceof MRectanglePoint
		debugPrint(SELF + "arrow point is not associated with a box.");
		return getStageAround(point);
	} // getStageForArrowPoint(Object)

	//----------------------------------------------------------------------
	//							getStageAround
	//----------------------------------------------------------------------
	/** Gets index of stage in which the given point falls.
	 * @param	point	a point in an MDocument
	 * @return	index of stage; NOT_FOUND for not found, FOUND_IN_2 for found
	 * in more than one stage
	 */
	private int getStageAround(MPoint point) {
		return getStageAround(point.getLocation(), 1.03);
	} // getStageAround(MPoint)

	/** Gets index of stage in which the given atom falls.
	 * @param	atom	an atom in an MDocument
	 * @return	index of stage; NOT_FOUND for not found, FOUND_IN_2 for found
	 * in more than one stage
	 */
	private int getStageAround(MolAtom atom) {
		return getStageAround(atom.getLocation(), 1);
	} // getStageAround(MolAtom)

	/** Gets index of stage in which the given location falls.
	 * @param	loc	a location in an MDocument
	 * @return	index of stage; NOT_FOUND for not found, FOUND_IN_2 for found
	 * in more than one stage
	 */
	private int getStageAround(DPoint3 loc) {
		return getStageAround(loc, 1.03);
	} // getStageAround(DPoint3)

	/** Gets index of stage in which the given location falls.
	 * @param	loc	a location in an MDocument
	 * @param	scale	amount by which to increase the stage's box's size
	 * @return	index of stage; NOT_FOUND for not found, FOUND_IN_2 for found
	 * in more than one stage
	 */
	private int getStageAround(DPoint3 loc, double scale) {
		final String SELF = "SynthParser.getStageAround: ";
		int found = NOT_FOUND;
		for (int stageNum = 0; stageNum < stages.size(); stageNum++) {
			final MRectangle box = stages.get(stageNum).getBox();
			if (box != null && VectorMath.pointInRect(loc, box, scale)) {
				if (found == NOT_FOUND) {
					/* debugPrint("point associated "
							+ "with box with object number ",
							stages.get(stageNum).getBoxIndex() + 1); */
					found = stageNum;
				} else {
					debugPrint(SELF + "Oops! point associated "
							+ "with two boxes with object numbers ",
							stages.get(stageNum).getBoxIndex() + 1,
							" and ", found + 1);
					return FOUND_IN_2;
				} // if already found
			} // if the point is in the box
		} // for each stage
		if (found == NOT_FOUND) {
			debugPrint(SELF + "point [", loc.x, ", ", loc.y, 
					"] not found in any box.");
			for (int stageNum = 0; stageNum < stages.size(); stageNum++) {
				final MRectangle box = stages.get(stageNum).getBox();
				final DPoint3 pt1 = box.getPointRef(0, null).getLocation();
				final DPoint3 pt2 = box.getPointRef(1, null).getLocation();
				final DPoint3 pt3 = box.getPointRef(2, null).getLocation();
				final DPoint3 pt4 = box.getPointRef(3, null).getLocation();
				debugPrint("\tBox ", stageNum + 1, ":",
						"\n\t\t[", pt1.x, ", ", pt1.y, ']',
						"\n\t\t[", pt2.x, ", ", pt2.y, ']',
						"\n\t\t[", pt3.x, ", ", pt3.y, ']',
						"\n\t\t[", pt4.x, ", ", pt4.y, ']');
			} // for each stage
		} // if not found
		return found;
	} // getStageAround(DPoint3, double)
	
	//----------------------------------------------------------------------
	//							extractMolecules
	//----------------------------------------------------------------------
	/** Extract individual molecules from the imported molecule and place them in
	 * their stages, ensuring each atom of each molecule is inside the same
	 * box.  Supergroups are contracted for purposes of determining
	 * whether or not all atoms in the group are "inside the rectangle".
	 * @param	emptyBoxOK	whether a box may be empty
	 * @throws	SynthError	if a molecule is found not to be entirely in
	 * a single box or if a box is impermissibly empty
	 */
	private void extractMolecules(boolean emptyBoxOK) throws SynthError {	
		final String SELF = "SynthParser.extractMolecules: ";
		final Molecule[] molList = importedMolecule.convertToFrags();	
		if (Utils.isEmpty(molList))
			fail("There are no molecules in your response!");
		final List<Molecule> molecules = Utils.molArrayToList(molList);			
		for (final Molecule aMolecule : molecules) {		
			debugPrint(SELF + "looking at ", aMolecule);
			// contracted molecule needed so atoms in sgroup are considered
			// "in rect" if their contracted symbol is "in rect"
			// debugPrintMRV(SELF + "before contraction:\n", aMolecule);
			aMolecule.contractSgroups(Expandable.DEFAULT_OPTIONS);
			final MolAtom[] atoms = aMolecule.getAtomArray();
			if (atoms.length == 0) fail("Error! Empty fragment!");
			// find stage of first atom or supergroup
			MolAtom atom = atoms[0];
			final int molStageIndex = getStageAround(atom);	
			if (molStageIndex == NOT_FOUND) {
				debugPrint(SELF + "atom ", atom, "1 in ", aMolecule, 
						" is not found in any stage.");
				debugPrintMRV(SELF + "after Sgroup contraction:\n", aMolecule);
				failAtomOutside(atom);
			} // if not in any stage
			if (molStageIndex == FOUND_IN_2) {
				debugPrint(SELF + "atom ", atom, "1 in ", aMolecule, 
						" is found in more than 1 stage.");
				debugPrintMRV(SELF + "after Sgroup contraction:\n", aMolecule);
				failAtomIn2(atom);
			} // if in more than one stage
			// ensure remaining atoms are inside the same box
			for (int atomIndex = 1; atomIndex < atoms.length; atomIndex++) {		
				atom = atoms[atomIndex];
				final int stageIndex = getStageAround(atom);			
				if (stageIndex == NOT_FOUND) {
					debugPrint(SELF + "atom ", atom, atomIndex + 1, " in ",
							aMolecule, " is not found in any stage.");
					debugPrintMRV(SELF + "after Sgroup contraction:\n", 
							aMolecule);
					failAtomOutside(atom);
				} else if (stageIndex == FOUND_IN_2) {
					debugPrint(SELF + "atom ", atom, atomIndex + 1, " in ",
							aMolecule, " is found in more than 1 stage.");
					failAtomIn2(atom);				
				} else if (stageIndex != molStageIndex) {
					debugPrint(SELF + "atom ", atom, atomIndex + 1, " in ",
							aMolecule, " is found in stage ", stageIndex + 1,
							" and not stage ", molStageIndex + 1, " where ",
							atoms[0], "1 is found.");
					fail("At least two atoms within one molecule are not in "
							+ "the same rectangle.");
				} // if whole molecule is not in a single box
			} // for each atom or supergroup
			// remove the shortcut groups now; they are nothing but trouble
			aMolecule.ungroupSgroups(SHORTCUT_GROUPS);
			final SynthStage stage = stages.get(molStageIndex);
			// see if this reagent should be chosen from pulldown menu
			try {
				Synthesis.checkForMenuReagent(
						Normalize.normalize(aMolecule));
			} catch (SynthError e) {
				debugPrint(SELF + "found menu reagent."
						+ " e.errorNumber = ", e.errorNumber);
				fail(stage.getBoxIndex(), e);
			}
			stage.addMolecule(aMolecule);
			debugPrint(SELF + "adding ", aMolecule, " to stage with "
					+ "box index ", stage.getBoxIndex() + 1, ".");
		} // for each molecule
		// ensure there are molecules in every stage
		for (int stageNum = stages.size() - 1; stageNum >= 0; stageNum--) {
			// ignore Jlint complaint about line above.  Raphael 11/2010
			final SynthStage stage = stages.get(stageNum);
			if (stage.getNumMolecules() <= 0) {
				if (!stage.hasNextStage() && !stage.hasPrevStage()) {
					debugPrint(SELF + "Found empty stage not connected to "
							+ "any other stage; removing it.");
					stages.remove(stageNum);
				} else if (!emptyBoxOK) {
					fail(stage.getBoxIndex(), "Every box of your synthesis "
							+ "must contain at least one compound.");
				} // if stage is unconnected
			} // if stage is empty
		} // for each stage
	} // extractMolecules(boolean, boolean)

	//----------------------------------------------------------------------
	//							extractRxnSelections
	//----------------------------------------------------------------------
	/** Gets reaction numbers from the MDocument, finds corresponding reaction
	 * selections, and puts them in their correct stage.
	 * @throws	SynthError	if a textbox is found to be in two boxes, or two
	 * reaction numbers are found in a single box
	 */
	private void extractRxnSelections() throws SynthError {
		final String SELF = "SynthParser.extractRxnNumbers: ";
		if (rxnConditions == null) {
			debugPrint(SELF + "No reaction numbers to parse.");
			return;
		} else debugPrint(SELF + "rxnConditions = ", rxnConditions);
		final String[] conditions = rxnConditions.split(RXN_ID_SEP);
		final int[] conditionsRange = new int[] {1, conditions.length};
		boolean haveTextBox = false;
		for (int objIndex = 0;
				objIndex < synthDoc.getObjectCount(); objIndex++) {
			final MObject mObject = synthDoc.getObject(objIndex);
			if (mObject instanceof MTextBox) {
				haveTextBox = true;
				final MTextBox tBox = (MTextBox) mObject;
				String tBoxText = tBox.getText();
				if (tBoxText != null) {
					tBoxText = tBoxText.trim();
					if (tBoxText.endsWith(".")) {
						tBoxText = Utils.rightChop(tBoxText, 1);
					} // if text ends with period
				} // if there's text
				final int rxnPosn = MathUtils.parseInt(tBoxText);
				if (rxnPosn == 0) {
					debugPrint(SELF + "textbox ", objIndex + 1,
							" contains 0 or nonnumerical value; ignore.");
					continue;
				} // if no number could be extracted from textbox
				// find number of stage in which textbox resides
				final MPoint nwCorner = tBox.getPoint(MRectangle.P_NW);
				int stageOfText = getStageAround(nwCorner);
				if (stageOfText == NOT_FOUND) {
					final DPoint3 center = new DPoint3();
					tBox.calcCenter(center, null); // stores value in center
					stageOfText = getStageAround(center);
					if (stageOfText == NOT_FOUND) {
						debugPrint(SELF + "Synthesis contains textbox ", 
								objIndex + 1, " that is not in a box; ignore.");
						continue;
					} // if stage not found for center of box
				} // if stage not found for NW corner of box
				if (stageOfText == FOUND_IN_2) { // unlikely
					debugPrint(SELF + "Synthesis contains textbox ", 
							objIndex + 1, " that is in two boxes; throw error.");
					fail(objIndex, "Textboxes cannot be contained within "
							+ "more than one box.");
				} // if stage not found
				final SynthStage stage = stages.get(stageOfText);
				try {
					stage.setRxnCondition(rxnPosn, conditions, conditionsRange);
				} catch (SynthError e) {
					fail(stage.getBoxIndex(), e.getMessage());
				} // try
			} // object is an MTextBox
		} // for each object in the MDocument
		if (!haveTextBox) {
			debugPrint(SELF + "no textboxes found; perhaps response is from "
					+ "MarvinJS and pseudoatoms are used instead.");
			for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
				final SynthStage stage = stages.get(stageIndex);
				final List<Molecule> stageMols = stage.getOrigMoleculeList();
				final int numStageMols = stageMols.size();
				for (int molNum = numStageMols - 1; molNum >= 0; molNum--) {
					final Molecule stageMol = stageMols.get(molNum);
					if (stageMol.getAtomCount() == 1) {
						final MolAtom atom = stageMol.getAtom(0);
						if (atom.isPseudo()) {
							stageMols.remove(molNum);
							String aliasStr = atom.getAliasstr();
							if (aliasStr != null) {
								aliasStr = aliasStr.trim();
								if (aliasStr.endsWith(".")) {
									aliasStr = Utils.rightChop(aliasStr, 1);
								} // if text ends with period
							} // if there's text
							final int rxnPosn = MathUtils.parseInt(aliasStr);
							if (rxnPosn == 0) {
								debugPrint(SELF + "pseudatom ", aliasStr,
										" contains 0 or nonnumerical value; "
										+ "ignore.");
								continue;
							} // if no number could be extracted from textbox
							try {
								stage.setRxnCondition(rxnPosn, conditions, 
										conditionsRange);
							} catch (SynthError e) {
								fail(stage.getBoxIndex(), e.getMessage());
							} // try
						} // if is pseudoatom
					} // if the molecule has one atom
				} // for each molecule in the stage
			} // for each stage
		} // if no textboxes
	} // extractRxnSelections()

	//----------------------------------------------------------------------
	//							findTargetStageIndex
	//----------------------------------------------------------------------	
	/** Finds and sets the index of the stage containing the target of the
	 * synthesis.
	 * @throws	SynthError	if the target stage can't be found, or more than
	 * one is found.
	 */
	private void findTargetStageIndex() throws SynthError {
		debugPrint("Entering findTargetStageIndex.");
		for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
			final SynthStage stage = stages.get(stageIndex);
			if (!stage.hasNextStage()) {
				if (targetStageIndex == NOT_FOUND) {
					debugPrint("Found targetStageIndex = ",
							stageIndex + 1, " with box number ",
							stage.getBoxIndex() + 1);
					targetStageIndex = stageIndex;
					if (stage.getAllPrevStages().size() > 1) {
						debugPrint("A single target means only one arrow can "
								+ "come into that box.");
						fail(stage.getBoxIndex(), "A synthesis can "
								+ "have only one target compound, but each "
								+ "reaction arrow leading into a box must "
								+ "produce at least one compound in that box, "
								+ "and the final box in your synthesis has "
								+ "at least two arrows pointing into it. ");
					}
					if (stage.getNumMolecules() > 1) {
						debugPrint("A synthesis should have a single target.");
						fail(stage.getBoxIndex(), "A synthesis can "
								+ "have only one target compound, but "
								+ "the final box in your synthesis contains "
								+ "more than one compound. ");
					}
				} else { // uh-oh
					debugPrint("Found another stage, box ",
							stage.getBoxIndex() + 1,
							", with no exit arrows. ");
					fail(stages.get(targetStageIndex).getBoxIndex(),
							stage.getBoxIndex(), "A synthesis can "
							+ "have only one target compound, but your "
							+ "response contains at least two boxes that "
							+ "do not lead to other boxes.");
				} // if target stage has not already been found
			} // if this stage has no next stage
		} // for each stage stageIndex
		if (targetStageIndex == NOT_FOUND) {
			fail("A synthesis must have a target compound, but your "
				+ "response contains no boxes that do not lead "
				+ "to other boxes. ");
		} // if no target stage was found
	} // findTargetStageIndex()

	//----------------------------------------------------------------------
	//							fail
	//----------------------------------------------------------------------
	/** Throws a SynthError of the appropriate type.
	 * @param	message	message to store in the SynthError
	 * @throws	SynthError	contains information on what went wrong
	 */
	private void fail(String message) throws SynthError {
		throw new SynthError("SynthParser", message, VERIFICATION_ERROR);
	} // fail(String)
	
	/** Throws a SynthError of the appropriate type.
	* @param	message	message to store in the SynthError
	* @param	variablePart	a string to substitute for part of the message
	* @throws	SynthError	contains information on what went wrong
	*/
	private void fail(String message, String variablePart) throws SynthError {
		throw new SynthError("SynthParser", message, VERIFICATION_ERROR, 
				variablePart);
	} // fail(String, String)
	
	/** Creates a modified response with the indicated MObject colored, throws
	 * a SynthError of the appropriate type.
	 * @param	objIndex	0-based index of an object to highlight
	 * @param	message	message to store in the SynthError
	 * @throws	SynthError	contains information on what went wrong
	 */
	private void fail(int objIndex, String message) throws SynthError {
		final String modResp = MolString.colorMObject(originalMRV, objIndex);
		throw new SynthError("SynthParser", message, modResp,
				VERIFICATION_ERROR);
	} // fail(int, String)

	/** Creates a modified response with the indicated MObject colored, throws
	 * a SynthError of the appropriate type.
	 * @param	objIndex	0-based index of an object to highlight
	 * @param	synthError	original SynthError that was caught
	 * @throws	SynthError	contains information on what went wrong
	 */
	private void fail(int objIndex, SynthError synthError) throws SynthError {
		final String modResp = MolString.colorMObject(originalMRV, objIndex);
		synthError.modifiedResponse = modResp;
		debugPrint("SynthParser throwing a SynthError...");
		throw synthError;
	} // fail(int, SynthError)

	/** Creates a modified response with the indicated MObjects colored, throws
	 * a SynthError of the appropriate type.
	 * @param	objIndex1	index in the MDocument of the response of first
	 * object to be colored
	 * @param	objIndex2	index in the MDocument of the response of second
	 * object to be colored
	 * @param	message	error message for student
	 * @throws	SynthError	always
	 */
	private void fail(int objIndex1, int objIndex2, String message)
			throws SynthError {
		final String modResp = MolString.colorMObjects(originalMRV, objIndex1,
				objIndex2);
		throw new SynthError("SynthParser", message, modResp,
				VERIFICATION_ERROR);
	} // fail(int, int, String)

	/** Creates a modified response with the indicated MObjects colored, throws a
	 * SynthError of the appropriate type.
	 * @param	objIndices	indices in the MDocument of the response of
	 * objects to be colored
	 * @param	message	error message for student
	 * @throws	SynthError	always
	 */
	/*
	// This method is apparently never called.
	private void fail(int[] objIndices, String message) throws SynthError {
		final String modResp = MolString.colorMObjects(originalMRV, objIndices);
		throw new SynthError("SynthParser", message, modResp,
				VERIFICATION_ERROR);
	} // fail(int[], String)
	*/

	//----------------------------------------------------------------------
	//					failAtomIn2, failAtomOutside
	//----------------------------------------------------------------------	
	/** A parsing error occurred because an atom was in two boxes.
	 * Throws a SynthError of the appropriate type.
	 * @param	atom	atom that was in two boxes
	 * @throws	SynthError	contains information on what went wrong
	 */
	private void failAtomIn2(MolAtom atom) throws SynthError {		
		fail("At least one atom, ***C***, is inside two rectangles. "
				+ "The rectangles should not overlap.", atom.getSymbol());
	} // failAtomIn2(MolAtom)
	
	/** A parsing error occurred because an atom was outside any box.
	 * Throws a SynthError of the appropriate type.
	 * @param	atom	atom that was outside any box
	 * @throws	SynthError	contains information on what went wrong
	 */
	private void failAtomOutside(MolAtom atom) throws SynthError {
		fail("At least one atom, ***C***, is outside any rectangle. "
				+ "All atoms should be within rectangles.", atom.getSymbol()); 
	} // failAtomOutside(MolAtom)	
	
} // SynthParser
