package com.epoch.mechanisms;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.io.MolExportException;
import chemaxon.struc.DPoint3;
import chemaxon.struc.MDocument;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import chemaxon.struc.Molecule;
import chemaxon.struc.MObject;
import chemaxon.struc.MPoint;
import chemaxon.struc.graphics.MEFlow;
import chemaxon.struc.graphics.MPolyline;
import chemaxon.struc.graphics.MRectangle;
import chemaxon.struc.graphics.MTextBox;
import chemaxon.struc.graphics.MRectanglePoint;
import chemaxon.struc.graphics.MMidPoint;
import chemaxon.struc.sgroup.Expandable;
import com.epoch.chem.ChemUtils;
import com.epoch.chem.MolString;
import com.epoch.chem.VectorMath;
import com.epoch.mechanisms.mechConstants.MechConstants;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Extracts data from MRV of a response mechanism, parses and orders the stages. */
class MechParser implements MechConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	//----------------------------------------------------------------------
	// 					shorthands
	//----------------------------------------------------------------------
	/** Shorthand in bug reporting. */
	private final static String WITHBOXINDEX = " with box index ";

	//----------------------------------------------------------------------
	//						members
	//----------------------------------------------------------------------
	/** MRV representation of the original response. */
	transient private final String respMRV;
	/** Copy of original response (with colors removed) imported into Java; 
	 * contains molecules and graphical objects.  */
	transient private MDocument archiveDoc = null;
	/** Original response as imported into Java; contains molecules and
	 * graphical objects, which are extracted.   It is destroyed in the parsing
	 * process; do not use afterwards! */
	transient private MDocument mechDoc = null;
	/** All of the molecules of the original response in a single Molecule.
	 * Don't use after the mechanism has been parsed! */
	transient private Molecule importedMolecule = null;

	/** Molecules in this mechanism. */
	transient private Molecule[] mols = null;
	/** List of reaction and resonance arrows in this mechanism. */
	transient private List<MechArrow> arrows = null;
	/** List of stages remaining in this mechanism. */
	transient private List<MechStage> stages = null;
	/** List of stages that were removed by parsing; contents would have been
	 * added to a remaining stage. */
	transient private List<MechStage> removedStages = null;

	/** Before stages are reordered, index of the first stage in the cyclic part
	 * of the mechanism in the list of unordered stages.
	 * It has two previous stages, one of which is also in the cyclic
	 * part, the other of which is the last stage in the linear part. */
	transient private int firstCyclicPhysicalStageIndex = UNCHECKED;
	/** After stages are
	 * reordered, logical index of the first stage in the cyclic part of the
	 * mechanism.  It has two previous stages, one of which is also in the cyclic
	 * part, the other of which is the last stage in the linear part. */
	transient public int firstCyclicStageIndex = UNCHECKED;
	/** Before stages are reordered, index of the first logical stage of the
	 * mechanism in the list of unordered stages; not used after stages are
	 * reordered, because it will be 0. */
	transient private int firstStageIndex = UNCHECKED;

	/** The number of regular reaction arrows in this mechanism.  */
	transient public int numReactionArrows = 0;
	/** The number of resonance arrows in this mechanism.  */
	transient public int numResonanceArrows = 0;
	/** The mechanism's topology.  */
	transient public int topology = UNCHECKED;
	/** Whether the mechanism's compounds could all be classified as starting
	 * materials, products, or intermediates.  (Errors in electron-flow arrows
	 * could prevent it.) */
	public int allMoleculesClassified = UNCHECKED;

	//----------------------------------------------------------------------
	//						constructor
	//----------------------------------------------------------------------
	/** Constructor.
	 * @param	responseMRV	original MRV of the response
	 * @throws	MolFormatException	if molecule can't be imported
	 * @throws	MechError	if something goes wrong during parsing
	 */
	MechParser(String responseMRV) throws MechError, MolFormatException {
		final String SELF = "MechParser: ";
		respMRV = responseMRV;
		debugPrint("Entering " + SELF + " with MRV:\n", respMRV);
		setupMDoc();
		setupStages();
		extractMolecules();
		extractFlows();
		extractArrows();
		findFirstStage();
		reorderStages();
		topology = (firstCyclicStageIndex == UNCHECKED ? LINEAR : CYCLIC);
		if (topology == LINEAR) checkLastStageForFlows();
		debugPrint(SELF, "finished parsing.");
	} // MechParser(String)

	//----------------------------------------------------------------------
	//					short, public get methods
	//----------------------------------------------------------------------
	/** Gets a stage of the mechanism.
	 * @param	stageNum	0-based number of the stage
	 * @return	the stage
	 */
	MechStage getStage(int stageNum)		{ return stages.get(stageNum); }
	/** Gets all stages of the mechanism.
	 * @return	the stages
	 */
	public List<MechStage> getStages()		{ return stages; }
	/** Gets the number of stages of the mechanism.
	 * @return	the number of stages
	 */
	final int getNumStages()				{ return stages.size(); }
	/** Gets all removed stages of the mechanism.
	 * @return	the removed stages
	 */
	List<MechStage> getRemovedStages()		{ return removedStages; }
	/** Gets the number of removed stages in the mechanism.
	 * @return the number of removed stages in the mechanism
	 */
 	int getNumRemovedStages() 				{ return removedStages.size(); } 
	/** Gets a copy of the MDocument of the mechanism.
	 * @return	a copy of the MDocument of the mechanism
	 */
	MDocument getMDocCopy()					{ return (MDocument) archiveDoc.clone(); }
	/** Gets the MRV of the original mechanism response.
	 * @return	MRV of the original mechanism response
	 */
	String getMRV()							{ return respMRV; }

/* ************** Methods called by init *******************/

	//----------------------------------------------------------------------
	//							setupMDoc
	//----------------------------------------------------------------------
	/** Extract MDocument (contains flows, arrows, etc) and molecules
	 * from the given student response, and uncolor.
	 * @throws	MolFormatException	if molecule can't be imported
	 * @throws	MechError	if there are no rectangles or arrows
	 */
	private void setupMDoc() throws MechError, MolFormatException {
		final String SELF = "MechParser.setupMDoc: ";
		importedMolecule = MolImporter.importMol(respMRV);
		mechDoc = importedMolecule.getDocument();
		if (mechDoc == null) {
			fail("Each set of reactants or products "
					+ "must be enclosed in a rectangle, and the "
					+ "rectangles must be connected by arrows.");
		} // if null
		mechDoc.selectAllObjects(false);
		MolString.uncolorMPolylines(mechDoc);
		MolString.adjustArrowsAttachedToRectangles(mechDoc);
		/* debugPrint(SELF + "MDocument after adjusting arrows attached "
				+ "to rectangles from MarvinJS drawing:\n", mechDoc); /**/
		archiveDoc = (MDocument) mechDoc.clone(); // necessary before parsing
	} // setupMDoc()

	//----------------------------------------------------------------------
	//							setupStages
	//----------------------------------------------------------------------
	/** Creates a stage for each rectangle found in the MDocument.
	 * @throws	MechError	if there are not at least two rectangles
	 */
	private void setupStages() throws MechError {
		final String SELF = "MechParser.setupStages: ";
		stages = new ArrayList<MechStage>();
		removedStages = new ArrayList<MechStage>();
		final int numObjects = mechDoc.getObjectCount();
		for (int objectIndex = 0; objectIndex < numObjects; objectIndex++) {
			final MObject anObject = mechDoc.getObject(objectIndex);
			if (anObject instanceof MRectangle &&
					!(anObject instanceof MTextBox)) {
				stages.add(new MechStage(
						(MRectangle) anObject, objectIndex));
				debugPrint(SELF + "Found box with index ",
						objectIndex + 1, ", made it stage ",
						stages.size(), ".");
			} // if object is a rectangle
			else debugPrint(SELF + "object with index ",
					objectIndex + 1, " is not a rectangle.");
		} // for each object in the MDocument
		if (stages.size() < 2) {
			debugPrint(SELF + "number of stages is only ",
					stages.size(), ".");
			fail("A mechanism must contain at least two rectangles, "
					+ "one containing starting materials "
					+ "and the other products.");
		} // if there are fewer than 2 stages
		int atomNum = 0;
		for (final MolAtom atom : importedMolecule.getAtomArray()) {
			atom.putProperty(ORIG_INDEX, Integer.valueOf(atomNum++));
			atom.setSelected(false);
		} // for each atom
		int bondNum = 0;
		for (final MolBond bond : importedMolecule.getBondArray()) {
			bond.putProperty(ORIG_INDEX, Integer.valueOf(bondNum++));
		} // for each bond
	} // setupStages()

	//----------------------------------------------------------------------
	//							extractMolecules
	//----------------------------------------------------------------------
	/** Extract individual molecules from the imported molecule and place them in
	 * their stages, ensuring each atom of each molecule is inside the same
	 * rectangle. Supergroups are contracted for purposes of determining
	 * whether or not all atoms in the group are "inside the rectangle".
	 * @throws	MechError	if there are no molecules, if an atom is outside any
	 * box, if an atom is in two boxes, if a molecule spans two rectangles, if
	 * there are valence or wedge errors
	 */
	private void extractMolecules() throws MechError {
		final String SELF = "MechParser.extractMolecules: ";
		mols = importedMolecule.convertToFrags();
		if (Utils.isEmpty(mols))
			fail("There are no molecules in your response!");
		debugPrint(SELF + "mols = ", mols);
		// final List<Integer> valenceErrorBoxIndices = new ArrayList<Integer>();
		// final List<Integer> wedgeErrorBoxIndices = new ArrayList<Integer>();
		int molIndex = 1;
		for (final Molecule mol : mols) {
			// contracted molecule needed so atoms in sgroup are considered
			// "in rectangle" if their contracted symbol is "in rectangle"
			// debugPrintMRV(SELF + "before contraction:\n", mol);
			mol.contractSgroups(Expandable.DEFAULT_OPTIONS); 
			final MolAtom[] atoms = mol.getAtomArray();
			if (Utils.isEmpty(atoms)) fail("Error! Empty fragment!");
			MolAtom atom = atoms[0];
			final int molStageIndex = getStageOf(atom);
			if (molStageIndex == NOT_FOUND) {
				debugPrint(SELF + "Atom ", atom, "1 of molecule ", 
						molIndex, ", ", mol, ", is not found in any box.");
				debugPrintMRV(SELF + "after Sgroup contraction:\n", mol);
				failAtomOutside(atom, 1, mol);
			} // if not in a box
			if (molStageIndex == FOUND_IN_2) {
				debugPrint(SELF + "Atom ", atom, "1 of molecule ", 
						molIndex, ", ", mol, ", is found in two boxes.");
				debugPrintMRV(SELF + "after Sgroup contraction:\n", mol);
				failAtomIn2(atom);
			} // if in more than one box
			debugPrint(SELF + "Atom ", atom, "1 of molecule ", molIndex,
					", ", mol, ", is in stage ", molStageIndex + 1,
					"; let's see if the other ", atoms.length - 1,
					" atoms in this molecule are.");
			for (int atomIndex = 1; atomIndex < atoms.length; atomIndex++) {
				atom = atoms[atomIndex];
				final int stageIndex = getStageOf(atom);
				if (stageIndex == NOT_FOUND) {
					debugPrint(SELF + "atom ", atom, atomIndex + 1, " in ",
							mol, " is not found in any stage.");
					debugPrintMRV(SELF + "after Sgroup contraction:\n", mol);
					failAtomOutside(atom, atomIndex + 1, mol);
				} // if not in a box
				if (stageIndex == FOUND_IN_2) {
					debugPrint(SELF + "atom ", atom, atomIndex + 1, " in ",
							mol, " is found in more than 1 stage.");
					failAtomIn2(atom);
				} // if in more than one stage
				if (stageIndex != molStageIndex) {
					debugPrint(SELF + "Atom ", atom, atomIndex + 1, 
							" is in stage ", stageIndex + 1, "with box ",
							getStageBoxIndex(stageIndex) + 1, "; error!");
					fail("At least two atoms within one molecule are not in "
							+ "the same rectangle.");
				} // if in more than one box
			} // for each atom or supergroup
			debugPrint(SELF + "The remaining atoms are in the same stage; "
					+ "adding to stage ", molStageIndex + 1, ": ", mol);
			stages.get(molStageIndex).addMolecule(mol);
			molIndex++;
		} // for each molecule
	} // extractMolecules()

	//----------------------------------------------------------------------
	//							extractFlows
	//----------------------------------------------------------------------
	/** Get electron-flow arrows from the MDocument and put them in their correct
	 * stage.
	 * @throws	MechError	if an electron-flow arrow connects molecules in two
	 * rectangles, if a two-electron electron-flow arrow points from an atom
	 * to one of its ligands, or if an electron-flow arrow begins at a shortcut
	 * group
	 */
	private void extractFlows() throws MechError {
		final String SELF = "MechParser.extractFlows: ";
		final Map<MEFlow, int[]> indicesByFlows = getIndicesByFlows();
		setMechFlows(indicesByFlows);
		ungroupShortcutGroups();
	} // extractFlows()

	//----------------------------------------------------------------------
	//							extractArrows
	//----------------------------------------------------------------------
	/** Get lists of reaction and resonance arrows, find the stages
	 * that they link.
	 * @throws	MechError	if a reaction/resonance arrow points to an arrow,
	 * if lines connect boxes instead of arrows, if a product-only box contains 
	 * electron-flow arrows, if an arrow points to nothing, if a 
	 * reaction/resonance arrow does not connect one box to one other box, if 
	 * a box is linked to four or more boxes, if the tails of two arrows overlap
	 */
	private void extractArrows() throws MechError {
		final String SELF = "MechParser.extractArrows: ";
		arrows = new ArrayList<MechArrow>();
		final List<MechArrow> branchedArrows = new ArrayList<MechArrow>();
		final List<MechArrow> unsureArrows = new ArrayList<MechArrow>();
		assignArrows(branchedArrows, unsureArrows);
		assignUnsureArrows(branchedArrows, unsureArrows);
		mergeBoxesOfBranchedArrows(branchedArrows);
		setArrowEndpoints();
	} // extractArrows()

	//----------------------------------------------------------------------
	//							findFirstStage
	//----------------------------------------------------------------------
	/** Finds the terminal and first cyclic stages (if any), and ensures that 
	 * the number of stages with 1 or 3 linking arrows is consistent with an 
	 * acceptable topology.
	 * <table>
	 * <caption>Acceptable</caption>
	 * <tr>
	 * <th>topology</th>
	 * <th># stages with one linking arrow</th>
	 * <th># stages with three linking arrows</th>
	 * </tr>
	 * <tr><td>linear</td><td>two</td><td>none</td></tr>
	 * <tr><td>cyclic</td><td>none</td><td>none</td></tr>
	 * <tr><td>linear + cyclic</td><td>one</td><td>one</td></tr>
	 * </table>
	 * @throws	MechError	if there is an incorrect number of first or last
	 * stages, or if some stages have too few or too many links,
	 * or if more than one stage has three links, or if the first stage could
	 * not be identified
	 */
	private void findFirstStage() throws MechError {
		final String SELF = "MechParser.findFirstStage: ";
		debugPrint(SELF + "beginning.");
		final List<MechStage> oneLinkStages = new ArrayList<MechStage>();
		final MechStage threeLinksStage = sortStagesByNumLinks(oneLinkStages);
		final int numOneLinkStages = oneLinkStages.size();
		switch (numOneLinkStages) {
			case 0: findFirstStage(threeLinksStage); break;
			case 1: findFirstStage(oneLinkStages.get(0), threeLinksStage); 
				break;
			case 2: findFirstStage(oneLinkStages, threeLinksStage); break;
			default: failFindFirstStage(oneLinkStages); break;
		} // switch numOneLinkStages
	} // findFirstStage()

	//----------------------------------------------------------------------
	//						reorderStages
	//----------------------------------------------------------------------
	/** Starting with firstStage, follow the reaction/resonance arrow links
	 * in each stage to physically reorder the stages in logical order.
	 * @throws	MechError	if the reaction/resonance arrows don't indicate
	 * a logical sequence
	 */
	private void reorderStages() throws MechError {
		final String SELF = "MechParser.reorderStages: ";
		final List<MechStage> newStages = new ArrayList<MechStage>();
		int thisStageIndex = firstStageIndex;
		final int numStages = stages.size();
		debugPrint(SELF, numStages, " stages to reorder; stage ",
				thisStageIndex + 1, " is first logical stage in mechanism.");
		for (int stageNum = 0; stageNum < numStages; stageNum++) {
			MechStage thisStage = stages.get(thisStageIndex);
			debugPrint(SELF + "Setting temp stage number ", thisStageIndex + 1,
					" to logical stage number ", stageNum + 1);
			newStages.add(thisStage);
			thisStage.setIndexInMech(stageNum);
			final List<MechArrow> linkArrows = thisStage.getLinkArrows();
			int arrowNum = 0;
			for (final MechArrow arrow : linkArrows) {
				debugPrint(SELF + "Arrow ", ++arrowNum,
						" of this stage originates at temp stage number ",
						stages.indexOf(arrow.getPrevStage()) + 1,
						" and points to ",
						stages.indexOf(arrow.getNextStage()) + 1);
				if (arrow.isResonant()) {
					// determine directionality of resonance arrow
					redirectResArrow(arrow, newStages, stageNum);
				} // if the arrow is a resonance arrow
				final MechStage arrowPrevStage = arrow.getPrevStage();
				// pointer equality:
				if (arrowPrevStage == thisStage) {
					final String msg = "Reaction/resonance arrow originates "
							+ "at this stage.  ";
					if (thisStage.getArrowToNext() == null) {
						debugPrint(SELF + msg, 
								"Setting stage's arrowHeadNext.");
						thisStage.setArrowToNext(arrow);
					} else {
						debugPrint(SELF + msg, 
								"Stage already has an arrowHeadNext.");
						fail(thisStage.getBoxIndex(),
								"The highlighted stage has too many reaction "
								+ " or resonance arrows originating from it.");
					} // if there is an arrow to next already
				} else {
					final String MSG = "Reaction/resonance arrow "
							+ "points to this stage.  ";
					if (stageNum == 0 && thisStageIndex != 
							firstCyclicPhysicalStageIndex) {
						debugPrint(SELF + MSG, "First stage cannot have arrow "
								+ "pointing to it (unless it is in a cyclic "
								+ "part of a mechanism).");
						if (firstCyclicPhysicalStageIndex != UNCHECKED) {
							final MechStage firstMechStage = 
									stages.get(firstCyclicPhysicalStageIndex);
							fail(thisStage.getBoxIndex(), 
									firstMechStage.getBoxIndex(),
									"Either one of the highlighted stages "
									+ "should not have a reaction or resonance "
									+ "arrow pointing to it, or the other has "
									+ "two arrows pointing to it and yet is "
									+ "not part of a cyclic mechanism.");
						} else {
							fail(thisStage.getBoxIndex(),
									"The highlighted stage should not have a "
									+ "reaction or resonance arrow pointing "
									+ "to it.");
						} // if mechanism has been identified as cyclic
					} else if (thisStage.getArrowFromPrev1() == null) {
						debugPrint(SELF + MSG, 
								"Setting stage's arrowTailPrev1.");
						thisStage.setArrowFromPrev1(arrow);
					} else if (thisStage.getArrowFromPrev2() == null) {
						debugPrint(SELF + MSG, 
								"Setting stage's arrowTailPrev2.");
						thisStage.setArrowFromPrev2(arrow);
					} else {
						debugPrint(SELF + MSG, 
								"Stage already has two arrowTailPrev.");
						fail(thisStage.getBoxIndex(),
								"The highlighted stage has too many reaction "
								+ " or resonance arrows pointing to it.");
					} // if there are arrows to previous already
				} // if arrow originates at this stage
			} // for each arrow of this stage
			// set firstCyclicStageIndex?
			if (thisStageIndex == firstCyclicPhysicalStageIndex) {
				debugPrint(SELF + "Setting firstCyclicStageIndex to ", 
						stageNum + 1);
				firstCyclicStageIndex = stageNum;
			} // if this stage is the first in a cyclic part
			// prepare for next stage
			if (stageNum < numStages - 1) {
				final MechArrow arrowToNext = thisStage.getArrowToNext();
				thisStage = (arrowToNext == null ? null
						: arrowToNext.getNextStage());
				if (thisStage != null) {
					thisStageIndex = stages.indexOf(thisStage);
				} else if (stageNum + 1 != stages.size()) {
					fail("Your mechanism contains two separate sequences "
							+ "of stages, so it cannot be interpreted.  "
							+ "Please connect all your stages into a "
							+ "single sequence.");
				} // if there's no next logical stage but physical stages remain
			} else { // last stage completed, all done
				debugPrint(SELF + "Reordering of stages is complete.");
			} // if this was the last stage
		} // for each stage
		stages = newStages;
		debugPrint(SELF + "Finished reordering stages.");
	} // reorderStages()

/* ************** Utility methods *******************/

	//----------------------------------------------------------------------
	//							getStageOf
	//----------------------------------------------------------------------
	/** Gets 0-based index of stage in stageList in which the given atom falls.
	 * @param	atom	the atom
	 * @return	index of stage in stages in which the given atom falls;
	 * -1 for not found in a stage, -2 for found in more than one stage
	 */
	private int getStageOf(MolAtom atom) {
		return getStageOf(atom.getLocation(), !POINT_OF_ARROW);
	} // getStageOf(MolAtom)

	/** Gets 0-based index of stage with which the arrow endpoint is
	 * associated.  First checks whether the arrow endpoint coincides with
	 * an MRectangle point; if not, checks whether the arrow endpoint falls
	 * within a box physically.
	 * @param	point	the arrow endpoint
	 * @return	index of stage in stageList in which the given point falls;
	 * -1 for not found, -2 for found in more than one stage, or -3 if found 
	 *  in a stage that has been removed
	 */
	private int getStageOf(MPoint point) {
		final String SELF = "MechParser.getStageOf: ";
		if (point instanceof MRectanglePoint) {
			debugPrint(SELF + "Arrow point is an MRectanglePoint.");
			final MRectangle rect = ((MRectanglePoint) point).getParentRect();
			int foundStageIndex = NOT_FOUND;
			for (int stageNum = 0; stageNum < stages.size(); stageNum++) {
				if (getStageBox(stageNum) == rect) { // pointer equality
					if (foundStageIndex != NOT_FOUND) {
						debugPrint(SELF + "Oops! arrow point associated "
								+ "with stage ", stageNum + 1,
								" with box with object number ",
								getStageBoxIndex(stageNum) + 1,
								" AND with stage ", foundStageIndex + 1,
								" with box with object number ",
								getStageBoxIndex(foundStageIndex) + 1, 
								".");
						return FOUND_IN_2;
					} // if already found a stage for this rectangle
					foundStageIndex = stageNum;
				} // if this stage owns the rectangle
			} // for each stage
			if (foundStageIndex == NOT_FOUND) {
				debugPrint(SELF + "Couldn't find it in regular stage; "
						+ "looking at removed stages.");
				for (final MechStage removedStage : removedStages)
					if (removedStage.getBox() == rect) {
						debugPrint(SELF + "MRectanglePoint associated with "
								+ "removed stage with box with object number ",
								removedStage.getBoxIndex() + 1);
						return FOUND_IN_REMOVED_STAGE;
					}
				// for each removed stage
			} // if not in regular stage
			return foundStageIndex;
		} else { // point instanceof other kind of MPoint
			debugPrint(SELF + "Arrow point is not an MRectanglePoint.");
			return getStageOf(point.getLocation(), POINT_OF_ARROW);
		} // point instanceof
	} // getStageOf(MPoint)

	/** Gets 0-based index of stage in stageList in which the given point falls.
	 * @param	p	coordinates of point
	 * @param	isArrowPoint	if point is head or tail of an arrow
	 * @return	index of stage in stages in which the given point falls;
	 * -1 for not found in a stage, -2 for found in more than one stage
	 */
	private int getStageOf(DPoint3 p, boolean isArrowPoint) {
		final double scale = (isArrowPoint ? 1.03 : 1);
		int found = NOT_FOUND;
		for (int stageNum = 0; stageNum < stages.size(); stageNum++) {
			final MRectangle box = getStageBox(stageNum);
			if (box != null && VectorMath.pointInRect(p, box, scale)) {
				if (found == NOT_FOUND) found = stageNum;
				else {
					debugPrint("Oops! point associated with stage ",
							found + 1, " with box with object number ",
							getStageBoxIndex(found) + 1,
							" also associated with stage ", stageNum + 1,
							" with box with object number ",
							getStageBoxIndex(stageNum) + 1);
					return FOUND_IN_2;
				} // if found
			} // if the point is in this box
		} // for each stage
		if (found == NOT_FOUND) {
			debugPrint("Couldn't find it in regular stage; "
					+ "looking at removed stages.");
			for (final MechStage removedStage : removedStages) {
				final MRectangle rect = removedStage.getBox();
				if (rect != null && VectorMath.pointInRect(p, rect, scale)) {
					debugPrint("point associated with removed "
							+ "stage with box with object number ",
							removedStage.getBoxIndex() + 1);
					return FOUND_IN_REMOVED_STAGE;
				} // if the point is in the box
			} // for each removedStage
		} // if not found in regular stage
		return found;
	} // getStageOf(DPoint3, boolean)

	//----------------------------------------------------------------------
	//						getStageBox, getStageBoxIndex
	//----------------------------------------------------------------------
	/** Gets the box of a stage.
	 * @param	num	the 0-based index of the stage
	 * @return	the stage's box
	 */
	private MRectangle getStageBox(int num) {
		return stages.get(num).getBox();
	} // getStageBox(int)

	/** Gets the 0-based index of the box of a stage.
	 * @param	num	the 0-based index of the stage
	 * @return	the 0-based index of the stage's box
	 */
	private int getStageBoxIndex(int num) {
		return stages.get(num).getBoxIndex();
	} // getStageBoxIndex(int)

	//----------------------------------------------------------------------
	//							getIndicesByFlows
	//----------------------------------------------------------------------
	/** Gets the electron-flow arrows and both their indices and the indices of
	 * the boxes in which they reside.
	 * @return	map of array of flow and box indices, keyed by electron-flow
	 * arrow
	 * @throws	MechError	if an electron-flow arrow connects molecules in two
	 * rectangles
	 */
	private Map<MEFlow, int[]> getIndicesByFlows() throws MechError {
		final String SELF = "MechParser.getIndicesByFlows: ";
		final Map<MEFlow, int[]> indicesByFlows = 
				new LinkedHashMap<MEFlow, int[]>();
		final int numObjects = mechDoc.getObjectCount();
		for (int objIndex = 0; objIndex < numObjects; objIndex++) {
			final MObject mObject = mechDoc.getObject(objIndex);
			if (mObject instanceof MEFlow) {
				final MEFlow flow = (MEFlow) mObject;
				final MolAtom[] atoms = MechFlow.getFlowAtoms(flow);
				final int atom0StageIndex = getStageOf(atoms[0]);
				for (final MolAtom atom : atoms) {
					if (getStageOf(atom) != atom0StageIndex) {
						fail(objIndex, "Electron-flow arrows must not "
								+ "connect compounds in different "
								+ "rectangles.");
					} // if atoms are in different stages
				} // for each atom to check
				debugPrint(SELF + "flow arrow ", objIndex + 1, 
						" resides in stage ", atom0StageIndex + 1);
				indicesByFlows.put(flow, 
						new int[] {objIndex, atom0StageIndex});
			} // if object is an MEFlow
		} // for each object in the MDocument
		return indicesByFlows;
	} // getIndicesByFlows()

	//----------------------------------------------------------------------
	//							setMechFlows
	//----------------------------------------------------------------------
	/** Creates the MechFlows and adds them to the stages that they're in.
	 * @param	indicesByFlows	map of array of flow and box indices, keyed 
	 * by electron-flow arrow
	 * @throws	MechError	if a two-electron electron-flow arrow points from 
	 * an atom to one of its ligands, or if it points from a bond directly
	 * to an atom not in that bond, or if an electron-flow arrow begins at a 
	 * shortcut group
	 */
	private void setMechFlows(Map<MEFlow, int[]> indicesByFlows) 
			throws MechError {
		final String SELF = "MechParser.setMechFlows: ";
		final int OBJ_INDEX = 0;
		final int STAGE_INDEX = 1;
		debugPrint(SELF + "entering.");
		// connects flows involving shortcut groups to attachment points
		for (final Molecule mol : mols) {
			mol.expandSgroups(Expandable.LEAVE_COORDS_UNCHANGED); 
		} // for each molecule
		String failureMsg = null;
		final List<Integer> badFlowBoxIndices = new ArrayList<Integer>();
		int count = 1;
		for (final Map.Entry<MEFlow, int[]> entry : indicesByFlows.entrySet()) {
			final int[] indices = entry.getValue();
			final int flowObjIndex = indices[OBJ_INDEX];
			final int flowStageIndex = indices[STAGE_INDEX];
			debugPrint(SELF + "setting up MechFlow ", count);
			try {
				final MechFlow flow =
						new MechFlow(entry.getKey(), flowObjIndex);
				debugPrint(SELF + "adding MechFlow whose MEFlow has object "
						+ "index ", flowObjIndex + 1, " to temp stage ", 
						flowStageIndex + 1);
				stages.get(flowStageIndex).addFlow(flow);
			} catch (MechError e) {
				final int boxObjIndex = 
						stages.get(flowStageIndex).getBoxIndex();
				debugPrint(SELF + "caught MechError on flow arrow with "
						+ " object index ", flowObjIndex + 1, 
						" to temp stage ", flowStageIndex + 1,
						" with box with object index ", boxObjIndex + 1);
				final String errorMsg = e.getMessage();
				if (failureMsg == null) failureMsg = errorMsg;
				if (errorMsg.equals(failureMsg)) {
					badFlowBoxIndices.add(Integer.valueOf(boxObjIndex));
				} // if first error or same one as previously
			} catch (RuntimeException e2) {
				final int boxObjIndex = 
						stages.get(flowStageIndex).getBoxIndex();
				debugPrint(SELF + "caught RunTimeException on flow arrow with "
						+ " object index ", flowObjIndex + 1, 
						" to temp stage ", flowStageIndex + 1,
						" with box with object index ", boxObjIndex + 1);
				final String msg = e2.getMessage();
				if (msg.contains("MultiFaceAtom")) {
					fail(e2.getMessage());
				} else throw e2;
			} // try
		} // for each electron-flow arrow
		if (failureMsg != null) fail(Utils.listToIntArray(badFlowBoxIndices), 
				failureMsg);
	} // setMechFlows(Map<MEFlow, int[]>)

	//----------------------------------------------------------------------
	//							ungroupShortcutGroups
	//----------------------------------------------------------------------
	/** Ungroups the molecules' shortcut groups. */
	private void ungroupShortcutGroups() {
		final String SELF = "MechParser.ungroupShortcutGroups: ";
		int molIndex = 0;
		for (final Molecule mol : mols) {
			mol.ungroupSgroups(SHORTCUT_GROUPS);
			debugPrint("After ungrouping shortcut groups in molecule ", 
					++molIndex, ": ", mol);
			debugPrintMRV(SELF + "molecule ", molIndex, " in MRV:\n", mol);
		} // for each molecule
	} // ungroupShortcutGroups()

	//----------------------------------------------------------------------
	//							assignArrows
	//----------------------------------------------------------------------
	/** Classifies each graphical (straight) arrow as originating at a box, 
	 * branched (originating at midpoint of another arrow), or originating in
	 * space.
	 * @param	branchedArrows	empty when passed; arrows found to originate at 
	 * the midpoint of another arrow
	 * @param	unsureArrows	empty when passed; arrows originating in space
	 * @throws	MechError	if a reaction/resonance arrow points to an arrow, if
	 * lines connect boxes instead of arrows
	 */
	private void assignArrows(List<MechArrow> branchedArrows, 
			List<MechArrow> unsureArrows) throws MechError {
		final String SELF = "MechParser.assignArrows: ";
		for (int objIndex = 0; objIndex < mechDoc.getObjectCount(); objIndex++) {
			final MObject mObject = mechDoc.getObject(objIndex);
			if (MolString.isLineOrRxnArrow(mObject)) {
				if (MolString.isReactionArrow(mObject)) {
					debugPrint(SELF + "examining arrow with object number ",
							objIndex + 1);
					final MechArrow arrow =
							new MechArrow((MPolyline) mObject, objIndex);
					final MPoint arrowTail = arrow.getPoint(TAIL);
					if (arrowTail instanceof MRectanglePoint) { // box to box arrow
						debugPrint(SELF + "arrow starts at box");
						arrows.add(arrow);
					} else if (arrowTail instanceof MMidPoint) {
						// branched arrow
						debugPrint(SELF + "arrow starts at arrow midpoint");
						branchedArrows.add(arrow);
					} else {
						debugPrint(SELF + "arrow starts in space");
						unsureArrows.add(arrow);
					} // if arrow originates from another arrow
					final MPoint arrowHead = arrow.getPoint(HEAD);
					if (arrowHead instanceof MMidPoint)
						fail(objIndex, "Reaction arrows may originate "
								+ "from the midpoint of another arrow, "
								+ "but they must point to a rectangle.");
				} else { // line but not arrow
					fail(objIndex, "Please connect your boxes with "
							+ "reaction or resonance arrows, not with "
							+ "straight lines.");
				} // if is an arrow
			} // if is a straight line or arrow
		} // for each object in the MDocument
	} // assignArrows(List<MechArrow>, List<MechArrow>)

	//----------------------------------------------------------------------
	//							assignUnsureArrows
	//----------------------------------------------------------------------
	/** Goes through the list of arrows that originate in space (instead of at a
	 * box or the midpoint of another arrow) and assigns them to boxes or as
	 * branched arrows.
	 * @param	branchedArrows	arrows found to originate at the midpoint of
	 * another arrow
	 * @param	unsureArrows	arrows originating in space
	 * @throws	MechError	if the tails of two arrows overlap
	 */
	private void assignUnsureArrows(List<MechArrow> branchedArrows, 
			List<MechArrow> unsureArrows) throws MechError {
		final String SELF = "MechParser.assignUnsureArrows: ";
		final String NO_TAIL_OVERLAP = "Two reaction arrows cannot have tails "
				+ "that overlap with one another. Please modify your drawing "
				+ "so that the two overlapping arrows are more perpendicular "
				+ "to each other.";
		// see if any of the unassigned arrows are originating near the midpoint
		// of another arrow
		debugPrint(SELF + "assigning ", unsureArrows.size(), 
				" arrow(s) originating in space.");
		for (int arrowNum = unsureArrows.size() - 1; arrowNum >= 0; arrowNum--) {
			final MechArrow unsureArrow = unsureArrows.remove(arrowNum);
			debugPrint(SELF + "finding origin of arrow ", arrowNum + 1, 
					" with object number ", unsureArrow.getObjectNumber() + 1);
			final MPoint arrowTail = unsureArrow.getPoint(TAIL);
			boolean foundMainArrow = false;
			// search arrows known to originate at rectangles
			for (final MechArrow toRectArrow : arrows) {
				if (onArrow(arrowTail, toRectArrow)) {
					if (onArrow(toRectArrow.getPoint(TAIL), unsureArrow)) {
						final int unsureArrowObjNum = 
								unsureArrow.getObjectNumber();
						final int toRectArrowObjNum = 
								toRectArrow.getObjectNumber();
						debugPrint(SELF + "tail of arrow ", arrowNum + 1, 
								" with object number ", 
								unsureArrowObjNum + 1,
								" originates near midpoint of rectangle-"
								+ "originating arrow with object number ", 
								toRectArrowObjNum + 1,
								" ***and vice versa***."); 
						fail(unsureArrowObjNum, toRectArrowObjNum, 
								NO_TAIL_OVERLAP);
					} else {
						debugPrint(SELF + "tail of arrow ", arrowNum + 1, 
								" with object number ", 
								unsureArrow.getObjectNumber() + 1,
								" originates near midpoint of rectangle-"
								+ "originating arrow with object number ", 
								toRectArrow.getObjectNumber() + 1); 
						foundMainArrow = true;
						unsureArrow.getArrow().setPoints(new MPoint[]
								{toRectArrow.getPoint(MIDPT),
								unsureArrow.getPoint(HEAD)});
						branchedArrows.add(unsureArrow);
						break;
					} // if the arrow tails overlap
				} else {
					debugPrint(SELF + "tail of arrow ", arrowNum + 1, 
							" with object number ", 
							unsureArrow.getObjectNumber() + 1, 
							" doesn't originate at rectangle-originating "
							+ "arrow with object number ", 
							toRectArrow.getObjectNumber() + 1);
				} // if the unsure arrow originates at an arrow originating at a rectangle
			} // for each arrow originating at a rectangle
			// search arrows known to originate at other arrows
			if (!foundMainArrow) for (final MechArrow otherArrow : branchedArrows) {
				if (onArrow(arrowTail, otherArrow)) {
					if (onArrow(otherArrow.getPoint(TAIL), unsureArrow)) {
						final int unsureArrowObjNum = 
								unsureArrow.getObjectNumber();
						final int otherArrowObjNum = 
								otherArrow.getObjectNumber();
						debugPrint(SELF + "tail of arrow ", arrowNum + 1, 
								" with object number ", 
								unsureArrowObjNum + 1,
								" originates near midpoint of rectangle-"
								+ "originating arrow with object number ", 
								otherArrowObjNum + 1,
								" ***and vice versa***."); 
						fail(unsureArrowObjNum, otherArrowObjNum, 
								NO_TAIL_OVERLAP);
					} else {
						debugPrint(SELF + "tail of arrow ", arrowNum + 1, 
								" with object number ", 
								unsureArrow.getObjectNumber() + 1,
								" originates on arrow-originating arrow with "
								+ "object number ", 
								otherArrow.getObjectNumber() + 1); 
						foundMainArrow = true;
						unsureArrow.getArrow().setPoints(new MPoint[]
								{otherArrow.getPoint(MIDPT),
								unsureArrow.getPoint(HEAD)});
						branchedArrows.add(unsureArrow);
						break;
					} // if the arrow tails overlap
				} else {
					debugPrint(SELF + "tail of arrow ", arrowNum + 1, 
							" with object number ", 
							unsureArrow.getObjectNumber() + 1,
							" doesn't originate on arrow-originating arrow "
							+ "with object number ", 
							otherArrow.getObjectNumber() + 1); 
				} // if the unsure arrow originates at an arrow originating at another arrow
			} // for each arrow originating at an arrow
			// search arrows with thus-far-unknown origins
			if (!foundMainArrow) for (final MechArrow otherArrow : unsureArrows) {
				if (onArrow(arrowTail, otherArrow)) {
					if (onArrow(otherArrow.getPoint(TAIL), unsureArrow)) {
						final int unsureArrowObjNum = 
								unsureArrow.getObjectNumber();
						final int otherArrowObjNum = 
								otherArrow.getObjectNumber();
						debugPrint(SELF + "tail of arrow ", arrowNum + 1, 
								" with object number ", 
								unsureArrowObjNum + 1,
								" originates near midpoint of rectangle-"
								+ "originating arrow with object number ", 
								otherArrowObjNum + 1,
								" ***and vice versa***."); 
						fail(unsureArrowObjNum, otherArrowObjNum, 
								NO_TAIL_OVERLAP);
					} else {
						debugPrint(SELF + "tail of arrow ", arrowNum + 1, 
								" with object number ", 
								unsureArrow.getObjectNumber() + 1,
								" originates on unknown-originating arrow with "
								+ "object number ", 
								otherArrow.getObjectNumber() + 1);
						foundMainArrow = true;
						unsureArrow.getArrow().setPoints(new MPoint[]
								{otherArrow.getPoint(MIDPT),
								unsureArrow.getPoint(HEAD)});
						branchedArrows.add(unsureArrow);
						break;
					} // if the arrow tails overlap
				} else {
					debugPrint(SELF + "tail of arrow ", arrowNum + 1, 
							" with object number ", 
							unsureArrow.getObjectNumber() + 1,
							" doesn't originate on unknown-originating arrow "
							+ "with object number ", 
							otherArrow.getObjectNumber() + 1);
				} // if the unsure arrow starts at an arrow originating in space
			} // for each arrow not originating at a rectangle or arrow
			if (!foundMainArrow) {
				debugPrint(SELF + "couldn't find where arrow ", arrowNum + 1,
						" with object number ",
						unsureArrow.getObjectNumber() + 1,
						" originates; assume it starts at a rectangle.");
				arrows.add(unsureArrow); // assume it originates at a rectangle
			} // if couldn't find its origin
		} // for each unassigned arrow
	} // assignUnsureArrows(List<MechArrow>, List<MechArrow>)

	//----------------------------------------------------------------------
	//							onArrow	
	//----------------------------------------------------------------------
	/** Determines whether a point lies on an arrow.
	 * @param	pt	the point
	 * @param	arrow	the arrow
	 * @return	true if the point lies on the arrow
	 */
	private boolean onArrow(MPoint pt, MechArrow arrow) {
		final String SELF = "MechParser.onArrow: ";
		final MPolyline arrowPolyline = arrow.getArrow();
		final boolean DEBUGPRINT = false;
		final boolean onArrow = VectorMath.pointOnLine(
				pt.getLocation(), arrowPolyline, DEBUGPRINT);
		debugPrint(SELF + "point ", pt, " is ", onArrow ? "" : "not ", 
				"on arrow with object number ", arrow.getObjectNumber() + 1, 
				" and endpoints [", arrowPolyline.getPoint(0), ", ", 
				arrowPolyline.getPoint(1), ']');
		return onArrow;
	} // onArrow(MPoint, MechArrow)

	//----------------------------------------------------------------------
	//					mergeBoxesOfBranchedArrows
	//----------------------------------------------------------------------
	/** It's permissible for reaction arrow 1 to originate at the midpoint 
	 * of reaction arrow 2 if, (1) the box to which arrow 1 points has no 
	 * other arrow originating from or pointing to it, and (2) arrow 2 
	 * originates at a stage and points to a stage.  We'll merge the stage 
	 * to which arrow 1 points into the stage to which arrow 2 points.
	 * @param	branchedArrows	list of arrows branching from other arrows
	 * @throws	MechError	if a product-only box contains electron-flow arrows,
	 * if an arrow points to nothing
	 */
	private void mergeBoxesOfBranchedArrows(List<MechArrow> branchedArrows) 
			throws MechError {
		final String SELF = "MechParser.mergeBoxesOfBranchedArrows: ";
		for (final MechArrow branchArrow : branchedArrows) {
			final MPoint branchArrowHead = branchArrow.getPoint(HEAD);
			final MPoint branchArrowTail = branchArrow.getPoint(TAIL);
			final int branchHeadStageNum = getStageOf(branchArrowHead);
			final MPolyline mainArrow =
					((MMidPoint) branchArrowTail).getParentLine();
			final MPoint mainArrowTail = getPoint(mainArrow, TAIL);
			final MPoint mainArrowHead = getPoint(mainArrow, HEAD);
			final boolean mainArrowTailIsMidpt =
					mainArrowTail instanceof MMidPoint;
			final int mainArrowHeadStageNum = getStageOf(mainArrowHead);
			if (mainArrowTailIsMidpt) {
				fail(branchArrow.getObjectNumber(), "If reaction arrow 1 "
						+ "originates at the midpoint of reaction "
						+ "arrow 2, then reaction arrow 2 must originate "
						+ "at a stage, not at another reaction arrow.");
			} else if (mainArrowHeadStageNum >= 0) { // main arrow points to a stage
				final int flowsInHeadStage = 
						stages.get(branchHeadStageNum).getNumFlows();
				final int flowsInMainHeadStage = 
						stages.get(mainArrowHeadStageNum).getNumFlows();
				if (flowsInHeadStage > 0 && flowsInMainHeadStage > 0) {
					fail(branchArrow.getObjectNumber(), "If the reaction arrow "
							+ "that points to a box originates at the "
							+ "midpoint of another reaction arrow, "
							+ "then that box should contain only "
							+ "products and no electron-flow arrows.");
				} // if branched-off stage contains electron-flow arrows
				if (flowsInMainHeadStage > 0) {
					copyStageContents(branchHeadStageNum, mainArrowHeadStageNum);
				} else {
					// branch arrow should be main arrow and vice versa
					mainArrow.setPoints(new MPoint[] {
							mainArrowTail, branchArrowHead});
					branchArrow.getArrow().setPoints(new MPoint[] {
							branchArrowTail, mainArrowHead});
					copyStageContents(mainArrowHeadStageNum, branchHeadStageNum);
				}
			} else {
				final int mainArrowObjIndex = mechDoc.indexOf(mainArrow);
				debugPrint(SELF + "unable to find stage for head of main arrow "
						+ "with object index ", mainArrowObjIndex + 1);
				fail(mainArrowObjIndex, "Every reaction arrow must "
						+ "point to exactly one rectangle.");
			} // if is a legitimate stage
		} // for each branched arrow
	} // mergeBoxesOfBranchedArrows()

	//----------------------------------------------------------------------
	//							setArrowEndpoints
	//----------------------------------------------------------------------
	/** For each reaction or resonance arrow of this stage, set the stages
	 * at which it originates and to which it points, and store all the stages.
	 * Does NOT set directionality of resonance arrows.
	 * @throws	MechError	if a reaction/resonance arrow does not connect one
	 * box to one other box, if a box is linked to four or more boxes
	 */
	private void setArrowEndpoints() throws MechError {
		final String SELF = "MechParser.setArrowEndPoints: ";
		int arrowIndex = 0;
		for (final MechArrow arrow : arrows) {
			final int objNumber = arrow.getObjectNumber();
			// tail and head do not indicate directionality for resonance arrows
			debugPrint(SELF + "Examining tail of arrow ", ++arrowIndex,
					" with object number ", objNumber + 1);
			final MPoint arrowTail = arrow.getPoint(TAIL);
			final int tailStageNum = getStageOf(arrowTail);
			debugPrint(SELF + "Examining head of same arrow ...");
			final MPoint arrowHead = arrow.getPoint(HEAD);
			final int headStageNum = getStageOf(arrowHead);
			if (Utils.among(NOT_FOUND, headStageNum, tailStageNum)) {
				if (arrow.isResonant())
					fail(objNumber, "All resonance arrows "
							+ "must originate from a rectangle and "
							+ "point to a rectangle.");
				else fail(objNumber, "All reaction arrows "
						+ "must originate from a rectangle or the "
						+ "midpoint of another reaction arrow and "
						+ "must point to a rectangle.");
			}
			if (Utils.among(FOUND_IN_2, headStageNum, tailStageNum))
				fail(objNumber, "All reaction and resonance arrows "
						+ "must originate from and "
						+ "point to exactly one rectangle.");
			if (Utils.among(FOUND_IN_REMOVED_STAGE, headStageNum, tailStageNum))
				fail(objNumber, "If a reaction arrow originates at "
						+ "the midpoint of another, then the rectangle to "
						+ "which the first reaction arrow points cannot "
						+ "have any other reaction arrows pointing to it "
						+ "or originating from it.");
			if (headStageNum == tailStageNum)
				fail(objNumber, "A reaction or resonance arrow must "
						+ "not be entirely contained in a single rectangle.  "
						+ "It must connect two rectangles or point from the "
						+ "midpoint of a reaction arrow to a rectangle.");
			// record stages in MechArrow
			debugPrint(SELF + "Recording stages in MechArrow...");
			final MechStage tailStage = stages.get(tailStageNum);
			final MechStage headStage = stages.get(headStageNum);
			if (tailStage.getNumLinkArrows() >= MAX_LINKING_ARROWS) {
				final int boxIndex = tailStage.getBoxIndex();
				debugPrint(SELF + "Stage ", tailStageNum + 1, WITHBOXINDEX,
						boxIndex + 1, " has >= 4 linking arrows.");
				fail(boxIndex, "At least one stage in your response "
						+ "mechanism is connected to other stages "
						+ "with four or more reaction or resonance arrows.");
			} // if the tail stage already has three linking arrows
			if (headStage.getNumLinkArrows() >= MAX_LINKING_ARROWS) {
				final int boxIndex = headStage.getBoxIndex();
				debugPrint(SELF + "Stage ", headStageNum + 1, WITHBOXINDEX,
						boxIndex + 1, " has >= 4 linking arrows.");
				fail(boxIndex, "At least one stage in your response "
						+ "mechanism is connected to other stages "
						+ "with four or more reaction or resonance arrows.");
			} // if the head stage already has three linking arrows
			arrow.setStagesPrevNext(tailStage, headStage);
			if (arrow.isResonant()) {
				numResonanceArrows++;
				debugPrint(SELF + "arrow ", arrowIndex, " is resonant.");
			} else {
				numReactionArrows++;
				debugPrint(SELF + "reaction arrow ", arrowIndex, 
						" originates at stage ", tailStageNum + 1, 
						WITHBOXINDEX, tailStage.getBoxIndex() + 1, 
						" and points to stage ", headStageNum + 1, 
						WITHBOXINDEX, headStage.getBoxIndex() + 1, ".");
			} // if this is a reaction arrow (not resonance)
			// store the arrow in the stages it links
			tailStage.addLink(arrow);
			headStage.addLink(arrow);
		} // for each arrow
		debugPrint(SELF + "all arrows assigned to starting and ending stages.");
	} // setArrowEndpoints()

	//----------------------------------------------------------------------
	//						sortStagesByNumLinks
	//----------------------------------------------------------------------
	/** Sorts the stages by the number of links they have to other stages,
	 * storing one-link stages in oneLinkStages and returning a three-link 
	 * stage.
	 * @param	oneLinkStages	empty when passed; stores the stages with one 
	 * link
	 * @return	a stage that has three links, or null if none found
	 * @throws	MechError	if stages aren't linked correctly
	 */
	private MechStage sortStagesByNumLinks(List<MechStage> oneLinkStages) 
			throws MechError {
		final String SELF = "MechParser.sortStagesByNumLinks: ";
		MechStage threeLinksStage = null;
		for (int stageIndex = stages.size() - 1; stageIndex >= 0; stageIndex--) {
			final MechStage stage = stages.get(stageIndex);
			final int boxIndex = stage.getBoxIndex();
			final int numLinkArrows = stage.getNumLinkArrows();
			final StringBuilder msg = Utils.getBuilder(SELF + "Stage ",
					stageIndex + 1, WITHBOXINDEX, boxIndex + 1,
					" has ", numLinkArrows, " linking arrow");
			if (numLinkArrows == 0) {
				debugPrint(msg.append("s."));
				if (stage.getNumMolecules() > 0) {
					fail(boxIndex, "At least one stage in your response "
							+ "mechanism is not connected to any other stages "
							+ "with reaction or resonance arrows.");
				} // if unconnected box is occupied
				debugPrint(SELF + "Found empty stage not connected to "
						+ "any other stage; removing it.");
				stages.remove(stageIndex);
				continue;
			} else if (numLinkArrows == 1) {
				debugPrint(msg.append('.'));
				oneLinkStages.add(stage);
			} else if (numLinkArrows == 2) {
				debugPrint(msg.append("s."));
			} else if (numLinkArrows == 3) {
				if (threeLinksStage == null) {
					debugPrint(msg.append("s, may be first in "
								+ "cyclic part of mechanism."));
					threeLinksStage = stage;
				} else {
					debugPrint(msg.append("s, but we already "
								+ "have such a stage."));
					fail(boxIndex, threeLinksStage.getBoxIndex(),
							"There are two or more stages in your "
							+ "response that are connected to three "
							+ "other stages with reaction or resonance arrows."
							+ "  There can be at most one such stage.");
				}
			} else { // won't happen; caught in extractArrows()
				Utils.alwaysPrint(SELF + "bad numLinkArrows");
			} // if numLinkArrows
			if (stage.getNumMolecules() == 0) {
				debugPrint(SELF + "Stage ", stageIndex + 1, " out of ",
						stages.size(), " with box ", stage.getBoxIndex() + 1,
						" doesn't contain a molecule.");
				fail(stage.getBoxIndex(), "Every stage of your "
						+ "mechanism must contain at least one compound.");
			} // if stage is empty
		} // for each stage
		return threeLinksStage;
	} // sortStagesByNumLinks(List<MechStage>)

		//----------------------------------------------------------------------
	//						findFirstStage
	//----------------------------------------------------------------------
	/** Finds the first stage when there are no stages with one link to other
	 * stages.
	 * @param	threeLinksStage	a stage linked to three other stages
	 * @throws	MechError	if a stage has three links, or if the first stage 
	 * could not be identified
	 */
	private void findFirstStage(MechStage threeLinksStage) throws MechError {
		final String SELF = "MechParser.findFirstStage: ";
		if (threeLinksStage != null) { // one stage connects to three others
			debugPrint(SELF 
					+ "We have a first cyclic stage and no end stages.");
			fail(threeLinksStage.getBoxIndex(),
					"A mechanism with a stage that is connected to three "
					+ "others must have a linear part and a cyclic "
					+ "part, and the linear part must start with a stage "
					+ "that is linked to exactly one other.");
		} else { // no stage connected to three others
			// cyclic-only mechanism; find first stage with a reaction arrow
			for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
				if (!stages.get(stageIndex).hasOnlyResonanceArrows()) {
					firstStageIndex = stageIndex;
					firstCyclicPhysicalStageIndex = stageIndex;
					debugPrint(SELF + "No first-in-cyclic or end stages, so "
							+ "mechanism must be completely cyclic.  "
							+ "Chose stage ", firstStageIndex + 1, 
							" with box with index ",
							getStageBoxIndex(firstStageIndex) + 1,
							" to be first stage because it was first "
							+ "encountered stage with a reaction arrow.");
					// first arrow in list of first stage's arrows 
					// must be reaction (not resonance) arrow for later
					// determination of directions of arrows
					final MechStage firstStage = stages.get(firstStageIndex);
					final List<MechArrow> linkArrows = 
							firstStage.getLinkArrows();
					while (linkArrows.get(0).isResonant()) {
						linkArrows.add(linkArrows.remove(0));
					} // while the arrow is a resonance arrow
					return;
				} // if stage has a reaction arrow
			} // for each stage
			debugPrint(SELF + "No first in cyclic and no end stages, so "
					+ "mechanism must be completely cyclic, but could not "
					+ "find a reaction arrow.");
			fail("Your mechanism is completely cyclic, and it has no "
					+ "reaction arrows.");
		} // if there's a three-links stage
	} // findFirstStage(MechStage)

	/** Finds the first stage when there is one stage with one link to other
	 * stages.
	 * @param	firstStage	the only stage linked to one other stage
	 * @param	threeLinksStage	a stage linked to three other stages
	 * @throws	MechError	if there is no stage with three links
	 */
	private void findFirstStage(MechStage firstStage, MechStage threeLinksStage) 
			throws MechError {
		final String SELF = "MechParser.findFirstStage: ";
		if (threeLinksStage == null) {
			debugPrint(SELF + "One end stage and no first-in-cyclic??!");
			fail(firstStage.getBoxIndex(),
					"A mechanism with only one stage that is connected "
					+ "to exactly one other must have a cyclic part.");
		} else {
			firstStageIndex = stages.indexOf(firstStage);
			firstCyclicPhysicalStageIndex = stages.indexOf(threeLinksStage);
			debugPrint(SELF + "We have an end stage with index ",
					firstStageIndex + 1, " and box index ",
					firstStage.getBoxIndex() + 1,
					" and a first cyclic stage with index ",
					firstCyclicPhysicalStageIndex + 1, " and box index ",
					threeLinksStage.getBoxIndex() + 1);
		} // if there's a three-links stage
	} // findFirstStage(MechStage, MechStage)

	/** Finds the first stage when there are two stages with one link to other
	 * stages.
	 * @param	oneLinkStages	the two stages linked to one other stage
	 * @param	threeLinksStage	a stage linked to three other stages
	 * @throws	MechError	if a stage has three links, or if the first stage 
	 * could not be identified
	 */
	private void findFirstStage(List<MechStage> oneLinkStages, 
			MechStage threeLinksStage) throws MechError {
		final String SELF = "MechParser.findFirstStage: ";
		if (threeLinksStage != null) { // one stage connects to three others
			debugPrint(SELF + "We have a first cyclic stage and at least two "
					+ "end stages.");
			fail(threeLinksStage.getBoxIndex(),
					"A mechanism with two end stages cannot have a "
					+ "stage that is connected to three other stages.");
		} else findFirstStage(oneLinkStages);
	} // findFirstStage(MechStage, MechStage)

	/** Finds the first stage when there are two stages with one link to other
	 * stages.
	 * @param	oneLinkStages	the two stages linked to one other stage
	 * @throws	MechError	if the first stage could not be identified
	 */
	private void findFirstStage(List<MechStage> oneLinkStages) 
			throws MechError {
		final String SELF = "MechParser.findFirstStage: ";
		debugPrint(SELF + "Two end stages and no first-in-cyclic.  Find "
				+ "end stage that uniquely has electron-flow arrows.");
		final MechStage stage0 = oneLinkStages.get(0);
		final MechStage stage1 = oneLinkStages.get(1);
		final int stage0Index = stages.indexOf(stage0);
		final int stage1Index = stages.indexOf(stage1);
		final int numStage0Flows = stage0.getNumFlows();
		final int numStage1Flows = stage1.getNumFlows();
		if (numStage0Flows != 0 && numStage1Flows == 0) {
			firstStageIndex = stage0Index;
			debugPrint(SELF + "Mechanism has no flows in stage ",
					stage1Index + 1, WITHBOXINDEX, stage1.getBoxIndex() + 1,
					" and does have them in stage ", stage0Index + 1, 
					WITHBOXINDEX, stage0.getBoxIndex() + 1,
					"; setting the latter as first stage.");
		} else if (numStage0Flows == 0 && numStage1Flows != 0) {
			firstStageIndex = stage1Index;
			debugPrint(SELF + "Mechanism has no flows in stage ",
					stage0Index + 1, WITHBOXINDEX, stage0.getBoxIndex() + 1,
					" and does have them in stage ", stage1Index + 1, 
					WITHBOXINDEX, stage1.getBoxIndex() + 1,
					"; setting the latter as first stage.");
		} else { // both or neither have flows
			final String neitherBoth = (numStage0Flows == 0 
					? "neither " : "both ");
			final String norAnd = (numStage0Flows == 0 
					? " nor " : " and ");
			debugPrint(SELF + "Mechanism has flows in ", neitherBoth,
					"stage ", stage0Index + 1, WITHBOXINDEX,
					getStageBoxIndex(stage0Index) + 1,
					norAnd, "stage ", stage1Index + 1, WITHBOXINDEX,
					getStageBoxIndex(stage1Index) + 1);
			debugPrint(SELF + "See if we can set direction from "
					+ "reaction arrows.");
			final MechArrow arrow0 = stage0.getLinkArrows().get(0);
			if (!arrow0.isResonant()) {
				final MechArrow arrow1 = stage1.getLinkArrows().get(0);
				if (!arrow1.isResonant()) {
					final boolean twoFirst = arrow0.getPrevStage() == stage0
							&& arrow1.getPrevStage() == stage1;
					final boolean twoLast = arrow0.getNextStage() == stage0
							&& arrow1.getNextStage() == stage1;
					if (twoFirst || twoLast) {
						final int stage0BoxIndex = stage0.getBoxIndex();
						final int stage1BoxIndex = stage1.getBoxIndex();
						debugPrint(SELF + "Mechanism has two stages -- "
								+ " stage ", stage0Index + 1, WITHBOXINDEX,
								stage0BoxIndex + 1, " and stage ", 
								stage1Index + 1, WITHBOXINDEX,
								stage1BoxIndex + 1, " -- that have a "
								+ "single reaction arrow pointing ", 
									twoFirst ? "away from" : "towards", 
								" them; mechanism malformed.");
						final String msg = Utils.toString(
								"Your mechanism has two ",
								twoFirst ? "first" : "last",
								" stages. A mechanism can ",
								twoFirst ? "begin" : "end",
								" only at one place.");
						fail(stage0BoxIndex, stage1BoxIndex, msg);
					} // if there are two first or last stages
				} // if arrow1 is not a resonance arrow
				firstStageIndex = (arrow0.getPrevStage() == stage0
						? stage0Index : stage1Index);
				debugPrint(SELF + "Mechanism has reaction arrow "
						+ "pointing ", firstStageIndex == stage0Index 
							? "away from" : "towards", " stage ", 
						stage0Index + 1, WITHBOXINDEX,
						stage0.getBoxIndex() + 1, "; setting ",
						firstStageIndex == stage0Index ? "it"
							: "stage " + (stage1Index + 1)
								+ WITHBOXINDEX
								+ (stage1.getBoxIndex() + 1),
						" as first stage.");
			} else { // arrow0 is resonant
				final MechArrow arrow1 = stage1.getLinkArrows().get(0);
				if (!arrow1.isResonant()) {
					firstStageIndex = (arrow1.getPrevStage() == stage1
							? stage1Index : stage0Index);
					debugPrint(SELF + "Mechanism has reaction arrow "
							+ "pointing ", firstStageIndex == stage1Index 
								? "away from" : "towards",
							" stage ", stage1Index + 1, WITHBOXINDEX,
							stage1.getBoxIndex() + 1, "; setting ",
							firstStageIndex == stage1Index ? "it"
								: "stage " + (stage0Index + 1)
									+ WITHBOXINDEX
									+ (stage0.getBoxIndex() + 1),
							" as first stage.");
				} else { // arrow 1 is also resonant
					fail(stage0Index, stage1Index,
							"Your mechanism has electron-flow arrows in "
							+ neitherBoth + "of its two terminal stages, "
							+ "and the arrows connecting them are resonant, "
							+ "so ACE cannot determine where the mechanism "
							+ "begins or ends.");
				} // if arrow linking stage 1 is not resonant
			} // if arrow linking stage 0 is not resonant
		} // if numStage0Flows and numStage1Flows
	} // findFirstStage(List<MechStage>)

	//----------------------------------------------------------------------
	//					checkLastStageForFlows
	//----------------------------------------------------------------------
	/** Checks whether the last stage of this linear mechanism has 
	 * electron-flow arrows.
	 * @throws	MechError	if the last stage has electron-flow arrows
	 */
	private void checkLastStageForFlows() throws MechError {
		final String SELF = "MechParser.checkLastStageForFlows: ";
		final MechStage lastStage = getStage(getNumStages() - 1);
		if (lastStage.getNumFlows() != 0) {
			fail(lastStage.getBoxIndex(), "A linear mechanism may not "
					+ "have electron-flow arrows in its last step.");
		} else debugPrint(SELF + "linear mechanism has no flows "
				+ "in last stage.");
	} // checkLastStageForFlows()

	//----------------------------------------------------------------------
	//						failFindFirstStage
	//----------------------------------------------------------------------
	/** Throws an error because there are three or more stages with one link 
	 * to other stages.
	 * @param	oneLinkStages	all stages linked to one other stage
	 * @throws	MechError	always
	 */
	private void failFindFirstStage(List<MechStage> oneLinkStages) 
			throws MechError {
		final String SELF = "MechParser.failFindFirstStage: ";
		final StringBuilder msg = Utils.getBuilder(
				"Mechanism has more than two stages with one link: ");
		final int numOneLinkStages = oneLinkStages.size();
		int[] boxIndices = new int[numOneLinkStages];
		for (int endStageNum = 0; endStageNum < numOneLinkStages;
				endStageNum++) {
			final MechStage stage = oneLinkStages.get(endStageNum);
			final int stageIndex = stages.indexOf(stage);
			boxIndices[endStageNum] = stage.getBoxIndex();
			Utils.appendTo(msg, SELF + "stage ", stageIndex + 1,
					WITHBOXINDEX, boxIndices[endStageNum],
					endStageNum < numOneLinkStages - 1 ? "; " : ".");
		} // for each end stage
		debugPrint(msg);
		fail(boxIndices, "A mechanism cannot have more than one "
				+ "beginning and one end.");
	} // failFindFirstStage(List<MechStage>)

	//----------------------------------------------------------------------
	//						getPoint
	//----------------------------------------------------------------------
	/** Gets a point of an MPolyline.
	 * @param	line	the MPolyline
	 * @param	kind	head, tail, midpoint
	 * @return	one of the MPolyline's points
	 */
	private MPoint getPoint(MPolyline line, int kind) {
		return line.getPointRef(kind, null);
	} // getPoint(MPolyline, int)

	//----------------------------------------------------------------------
	//						copyStageContents
	//----------------------------------------------------------------------
	/** Copies the contents of one stage into another.
	 * @param	sourceStageNum	index of stage whose contents will be copied
	 * @param	destStageNum	index of stage accepting the contents
	 */
	private void copyStageContents(int sourceStageNum, int destStageNum) {
		final MechStage sourceStage = stages.get(sourceStageNum);
		final MechStage destStage = stages.get(destStageNum);
		// copy sourceStage objects into destStage
		for (final Molecule mol : sourceStage.getMoleculeArray()) {
			destStage.addMolecule(mol);
		} // for each molecule molNum in sourceStage
		for (final MechFlow flow : sourceStage.getFlows()) {
			destStage.addFlow(flow);
		} // for each flow flowNum in sourceStage
		sourceStage.setBoxIndexOfStageCopiedInto(
				destStage.getBoxIndex());
		removedStages.add(sourceStage);
		stages.remove(sourceStageNum);
	} // copyStageContents(int, int)

	//----------------------------------------------------------------------
	//						redirectResArrow
	//----------------------------------------------------------------------
	/** Decides whether a resonance arrow's prevStage and nextStage are correct
	 * or should be switched.  Utility method for reorderStages().
	 * @param	arrow	the resonance arrow
	 * @param	newStages	stages that have already been ordered
	 * @param	stageNum	index of current stage in physical list
	 */
	private void redirectResArrow(MechArrow arrow,
			List<MechStage> newStages, int stageNum) {
		final String SELF = "MechParser.redirectResArrow: ";
		debugPrint(SELF + "Arrow is double-headed (resonance); from "
				+ "which of its stages should it originate?");
		final MechStage thisStage = newStages.get(stageNum);
		final MechStage arrowTail = arrow.getPrevStage();
		final MechStage arrowHead = arrow.getNextStage();
		if (stageNum == 0) { // we're at the first stage
			if (firstStageIndex != firstCyclicPhysicalStageIndex) {
				if (thisStage == arrowHead) {
					// no arrow should point to the first stage;
					// switch arrow direction
					debugPrint(SELF + "Arrow is \"pointing\" to the first "
							+ "stage; switch its direction.");
					arrow.setStagesPrevNext(arrowHead, arrowTail);
				} else { // arrow originates at first stage
					debugPrint(SELF + "Arrow is \"originating\" from first "
							+ "stage; don't switch direction.");
				} // if direction of arrow
			} else { // firstStageIndex == firstCyclicPhysicalStageIndex
				debugPrint(SELF + "We're at first stage in cyclic-only "
						+ "mechanism...");
				final List<MechArrow> linkArrows = thisStage.getLinkArrows();
				// we know this resonant arrow is not first in list because we 
				// already ensured that the first arrow is nonresonant
				debugPrint(SELF + "This arrow is not the very first.  Get "
						+ "direction from previously set reaction arrow.");
				final MechArrow firstArrow = linkArrows.get(0);
				final MechStage nextStage = firstArrow.getNextStage();
				if (nextStage == thisStage) {
					debugPrint(SELF + "First arrow points to this stage, so "
							+ "resonance arrow is supposed to point to "
							+ "next stage.");
					if (arrowHead == thisStage) {
						debugPrint(SELF + "Current arrow is \"pointing\" to "
								+ "this stage; switch its direction.");
						arrow.setStagesPrevNext(arrowHead, arrowTail);
					} else {
						debugPrint(SELF + "Current arrow is \"pointing\" to "
								+ "the next stage like it ought.");
					} // if arrow is pointing to this stage
				} else {
					debugPrint(SELF + "First arrow points to next stage, so "
							+ "resonance arrow is supposed to point to "
							+ "this stage.");
					if (arrowTail == thisStage) {
						debugPrint(SELF + "Current arrow is \"pointing\" to "
								+ "the next stage; switch its direction.");
						arrow.setStagesPrevNext(arrowHead, arrowTail);
					} else {
						debugPrint(SELF + "Current arrow is \"pointing\" to "
								+ "this stage like it ought.");
					} // if arrow is pointing to this stage
				} // with which stage is arrow linking this one, and in what direction
			} // if mechanism is linear or has linear part
		} else { // we're not at the first stage
			final MechStage prevStage = newStages.get(stageNum - 1);
			// get arrow from previous stage that points to current one
			final MechArrow prevStageArrowToNext = prevStage.getArrowToNext();
			if (prevStageArrowToNext == arrow) {
				debugPrint(SELF + "This arrow is shared with the previous stage; "
						+ "must have already been set to the right direction.");
			} else {
				final String msg = "Arrow should be \"pointing\" to a next stage, ";
				if (thisStage == arrowHead) {
					debugPrint(SELF + msg, " but it is \"pointing\" at this one; "
							+ "switch its direction.");
					arrow.setStagesPrevNext(arrowHead, arrowTail);
				} // if arrow points to current stage
				else debugPrint(SELF + msg, 
						" and it is; don't switch its direction.");
			} // if current arrow is the toNext arrow of previous stage
		} // if	we're at the first stage
	} // redirectResArrow(MechArrow, List<MechStage>, int)

/* ************** Error-throwing methods *******************/

	//----------------------------------------------------------------------
	//							fail
	//----------------------------------------------------------------------
	/** Throws a MechError of the appropriate type.
	 * @param	message	message to store in the MechError
	 * @throws	MechError	contains information on what went wrong
	 */
	private void fail(String message) throws MechError {
		throw new MechError("MechParser", message, VERIFICATION_ERROR);
	} // fail(String)

	/** Throws a MechError of the appropriate type.
	 * @param	message	message to store in the MechError
	 * @param	variablePart	a string to substitute for part of the message
	 * @throws	MechError	contains information on what went wrong
	 */
	private void fail(String message, String variablePart) throws MechError {
		debugPrint("MechParser.fail: message = ", message, ", variablePart = ",
				variablePart);
		throw new MechError("MechParser", message, VERIFICATION_ERROR,
				variablePart);
	} // fail(String, String)

	/** Creates a modified response with the indicated MObject colored, throws
	 * a MechError of the appropriate type.
	 * @param	objIndex	0-based index of an object to highlight
	 * @param	message	message to store in the MechError
	 * @throws	MechError	contains information on what went wrong
	 */
	private void fail(int objIndex, String message) throws MechError {
		String modResp = respMRV;
		try {
			MolString.colorMObject(archiveDoc, objIndex);
			modResp = MolString.toString(archiveDoc, MRV);
		} catch (MolExportException e) {
			debugPrint("MechParser.fail: export of uncolored, recolored "
					+ "MDocument failed; color original response instead.");
			modResp = MolString.colorMObject(respMRV, objIndex);
		}
		throw new MechError("MechParser", message, modResp, VERIFICATION_ERROR);
	} // fail(int, String)

	/** Creates a modified response with the indicated MObjects colored, throws
	 * a MechError of the appropriate type.
	 * @param	objIndex1	objIndex in the MDocument of the response of first
	 * object to be colored
	 * @param	objIndex2	objIndex in the MDocument of the response of second
	 * object to be colored
	 * @param	message	error message for student
	 * @throws	MechError	always
	 */
	private void fail(int objIndex1, int objIndex2, String message)
			throws MechError {
		String modResp = respMRV;
		try {
			MolString.colorMObjects(archiveDoc, objIndex1, objIndex2);
			modResp = MolString.toString(archiveDoc, MRV);
		} catch (MolExportException e) {
			debugPrint("MechParser.fail: export of uncolored, recolored "
					+ "MDocument failed; color original response instead.");
			modResp = MolString.colorMObjects(respMRV, objIndex1, objIndex2);
		}
		throw new MechError("MechParser", message, modResp);
	} // fail(int, int, String)

	/** Creates a modified response with the indicated MObjects colored, throws a
	 * MechError of the appropriate type.
	 * @param	objIndices	indices in the MDocument of the response of
	 * objects to be colored
	 * @param	message	error message for student
	 * @throws	MechError	always
	 */
	private void fail(int[] objIndices, String message) throws MechError {
		final String SELF = "MechParser.fail: ";
		String modResp = respMRV;
		try {
			debugPrint(SELF + "trying to color objects with 0-based indices ",
					objIndices, "; ", message);
			MolString.colorMObjects(archiveDoc, objIndices);
			modResp = MolString.toString(archiveDoc, MRV);
		} catch (MolExportException e) {
			debugPrint(SELF + "export of uncolored, recolored "
					+ "MDocument failed; color original response instead.");
			modResp = MolString.colorMObjects(respMRV, objIndices);
		}
		debugPrint(SELF + "colored MRV: ", modResp);
		throw new MechError("MechParser", message, modResp);
	} // fail(int[], String)

	//----------------------------------------------------------------------
	//							failAtom
	//----------------------------------------------------------------------
	/** A parsing error occurred because an atom was in two boxes.
	 * Throws a MechError of the appropriate type.
	 * @param	atom	atom that was in two boxes
	 * @throws	MechError	contains information on what went wrong
	 */
	private void failAtomIn2(MolAtom atom) throws MechError {
		fail("At least one atom, ***C***, is inside two rectangles. "
				+ "The rectangles should not overlap.", atom.getSymbol());
	} // failAtomIn2(MolAtom)

	/** A parsing error occurred because an atom was outside any box.
	 * Throws a MechError of the appropriate type.
	 * @param	atom	atom that was outside any box
	 * @param	atomIndex	the atom's 1-based index
	 * @param	mol	the molecule containing the atom
	 * @throws	MechError	contains information on what went wrong
	 */
	private void failAtomOutside(MolAtom atom, int atomIndex, Molecule mol) 
			throws MechError {
		final String msg = Utils.toString(atom.getSymbol(), '^', atomIndex,
				" of ", MolString.toString(mol, SMILES));
		fail("At least one atom, ***C of CH4***, is "
				+ "outside any rectangle. All atoms should be within "
				+ "rectangles.", msg); 
	} // failAtomOutside(MolAtom)

} // MechParser
