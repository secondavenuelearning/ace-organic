package com.epoch.mechanisms;

import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import chemaxon.struc.Molecule;
import chemaxon.struc.Sgroup;
import chemaxon.struc.SgroupType;
import chemaxon.struc.graphics.MAtomSetPoint;
import chemaxon.struc.graphics.MEFlow;
import chemaxon.struc.sgroup.MulticenterSgroup;
import chemaxon.struc.sgroup.SgroupAtom;
import chemaxon.struc.sgroup.SuperatomSgroup;
import com.epoch.chem.ChemUtils;
import com.epoch.mechanisms.mechConstants.MechConstants;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang.ArrayUtils;

/** Describes an electron-flow arrow. Contains methods for getting the source
 * and sink of the arrow. Two-electron atom-to-atom arrows are reinterpreted
 * here to mean the same as two-electron atom-to-incipient-bond arrows.  */
final class MechFlow implements MechConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	//----------------------------------------------------------------------
	//						  members
	//----------------------------------------------------------------------
	/** ChemAxon representation of the electron-flow arrow. */
	transient private MEFlow meFlow = null;
	/** 0-Based index of the electron-flow arrow in the MDocument of the
	 * response. */
	transient private int objectIndex;
	/** The origin of the electron-flow arrow (MolAtom or MolBond). */
	transient private Object source;
	/** The destination of the electron-flow arrow (MolAtom, MolBond, or
	 * MolAtom[2] for incipient bond). */
	transient private Object sink;

	//----------------------------------------------------------------------
	//						  constructors
	//----------------------------------------------------------------------
	/** Constructor. */
	MechFlow() { /* intentionally empty */ }

	/** Constructor called by MechSubstructSearch and MechRuleFunctions. 
	 * @param	newFlow	the electron-flow arrow
	 * @throws	MechError	if this arrow represents two electrons and points
	 * from an atom to one of its ligands
	 */
	MechFlow(MEFlow newFlow) throws MechError { 
		initFlow(newFlow);
	} // MechFlow(MEFlow)

	/** Constructor called by MechParser. 
	 * @param	newFlow	the electron-flow arrow
	 * @param	objIndex	0-based index of the electron-flow arrow in the
	 * MDocument of the response
	 * @throws	MechError	if this arrow represents two electrons and points
	 * from an atom to one of its ligands
	 */
	MechFlow(MEFlow newFlow, int objIndex) throws MechError { 
		initFlow(newFlow);
		objectIndex = objIndex;
	} // MechFlow(MEFlow, int)

	/** Constructor called by MechSolver. 
	 * @param	newFlow	the electron-flow arrow
	 * @param	molecule	the molecule from which the arrow derives; modified
	 * by this method by ungrouping shortcut groups if they are touched by
	 * electron-flow arrows
	 * @throws	MechError	if this arrow represents two electrons and points
	 * from an atom to one of its ligands
	 */
	MechFlow(MEFlow newFlow, Molecule molecule) throws MechError { 
		initFlow(newFlow);
		final MolAtom[] srcAtoms = getSrcAtoms();
		if (removeSgroupTermini(srcAtoms, molecule)) {
			source = (srcIsAtom() ? srcAtoms[0] 
					: srcAtoms[0].getBondTo(srcAtoms[1]));
		} // if removed shortcut groups
		final MolAtom[] sinkAtoms = getSinkAtoms();
		if (removeSgroupTermini(sinkAtoms, molecule)) {
			sink = (sinkIsAtom() ? sinkAtoms[0]
					: sinkIsBond() ? sinkAtoms[0].getBondTo(sinkAtoms[1])
					: sinkAtoms); // incipient bond
		} // if removed shortcut groups
	} // MechFlow(MEFlow, Molecule)

	/** Sets the flow and initiates the source and sink values.  If the flow is
	 * a 2-electron atom-to-atom arrow, and the atoms aren't already connected, 
	 * sets the sink as an incipient bond between the atoms.
	 * @param	newFlow	the electron-flow arrow
	 * @throws	MechError	if this arrow represents two electrons and points
	 * from an atom to one of its ligands, or if it points from a bond directly
	 * to an atom not in that bond.
	 */
	private void initFlow(MEFlow newFlow) throws MechError {
		final String SELF = "MechFlow.initFlow: ";
		meFlow = newFlow; 
		source = meFlow.getMolObject(MEFlow.E_SOURCE);
		sink = meFlow.getMolObject(MEFlow.E_SINK);
		if (sinkIsAtom()) {
			MolAtom sinkAtom = getSinkAtom();
			if (srcIsAtom() && getNumElectrons() == 2) {
				final MolAtom srcAtom = getSrcAtom();
				if (srcAtom.getBondTo(sinkAtom) != null) {
					debugPrint(SELF + "srcAtom ", srcAtom, " of ",
							srcAtom.getParent(), " and sink atom ",
							sinkAtom, " of ", sinkAtom.getParent(),
							" connected directly by two-electron atom-to-atom "
							+ "arrow are bound.");
					throw new MechError("The highlighted stage contains "
							+ "a two-electron electron-flow arrow that "
							+ "illegally points from one atom to another "
							+ "atom to which it is already bonded.");
				} // if source and sink are already connected
				debugPrint(SELF + "two electron atom-to-atom arrow; changing "
						+ "sink to incipient ", srcAtom, '-', sinkAtom, 
						" bond.");
				sink = new MolAtom[] {srcAtom, sinkAtom};
			} else if (srcIsBond()) {
				final MolAtom[] srcAtoms = getSrcAtoms();
				// next line uses pointer equality
				if (srcAtoms[0] != sinkAtom && srcAtoms[1] != sinkAtom) {
					final boolean noSinkToSrc0 = 
							sinkAtom.getBondTo(srcAtoms[0]) == null;
					final boolean noSinkToSrc1 = 
							sinkAtom.getBondTo(srcAtoms[1]) == null;
					if (noSinkToSrc0 != noSinkToSrc1
							&& getNumElectrons() == 2
							&& getSrcBond().getType() == 1) {
						debugPrint(SELF + "sink atom ", sinkAtom, " in ", 
								sinkAtom.getParent(), " not part of src bond ", 
								srcAtoms, " in ", srcAtoms[0].getParent(),
								", but is attached to exactly one of the src "
								+ "bond atoms, and src bond has order 1, and "
								+ "flow arrow has two electrons, so we have "
								+ "1,2-shift.");
						final MolAtom migratingAtom = 
								(noSinkToSrc0 ? srcAtoms[0] : srcAtoms[1]);
						sink = new MolAtom[] {sinkAtom, migratingAtom};
					} else {
						debugPrint(SELF + "sink atom ", sinkAtom, " in ", 
								sinkAtom.getParent(), 
								" is neither part of src bond ", srcAtoms, 
								" in ", srcAtoms[0].getParent(),
								" nor satisfies conditions of 1,2-shift; "
								+ "may be forming a coordinate bond"); 
						if (ChemUtils.isTransitionMetal(sinkAtom)) {
							debugPrint(SELF + "sink atom ", sinkAtom, " in ", 
									sinkAtom.getParent(), 
									" is making a coordinate bond from ",
									srcAtoms, " in ", srcAtoms[0].getParent(),
									"; replace existing flow arrow with "
									+ "multicenter attachment point and "
									+ "new incipient bond from it to metal.");
							// create multicenter attachment point, add to
							// parent molecule
							final Molecule mol = 
									(Molecule) sinkAtom.getParent();
							final MulticenterSgroup piBondLigand = 
									new MulticenterSgroup(mol);
							piBondLigand.add(srcAtoms[0]);
							piBondLigand.add(srcAtoms[1]);
							piBondLigand.addCentralAtom();
							mol.addSgroup(piBondLigand, true);
							// change flow source and sink
							final MolAtom multictrAttachPt = 
									piBondLigand.getCentralAtom();
							source = multictrAttachPt;
							sink = new MolAtom[] {multictrAttachPt, sinkAtom};
						} else {
							debugPrint(SELF + "sink atom ", sinkAtom, " in ", 
									sinkAtom.getParent(), 
									" neither part of src bond ", srcAtoms, 
									" in ", srcAtoms[0].getParent(),
									" nor satisfies conditions of 1,2-shift "
									+ "nor forming a coordinate bond to a metal"); 
							throw new MechError("Each highlighted stage contains "
									+ "an electron-flow arrow "
									+ "that originates at an X&ndash;Y bond and "
									+ "points directly to a third atom Z not in "
									+ "that bond. The meaning of such an arrow is "
									+ "ambiguous. If you want to make a new bond "
									+ "between one of the bond atoms (say, X) and "
									+ "Z, the electron-flow arrow originating at "
									+ "the X&ndash;Y bond must point to "
									+ "<i>between</i> X and Z. If X and Z are "
									+ "very close to one another, you may need to "
									+ "move them apart.");
						} // if not pointing to a metal
					} // if not a 1,2-shift
				} // if sink atom is not one of the bond atoms
			} // if source
		} else if (sinkIsBond() && srcIsBond()
				&& getSrcBond() == getSinkBond()) { // pointer equality
			debugPrint(SELF + "source and sink bond ", getSrcAtoms(),
					" are identical");
			throw new MechError("The highlighted stage contains an "
					+ "electron-flow arrow that "
					+ "originates at and points to the same bond.");
		} // if sink
	} // initFlow(MEFlow)

	//----------------------------------------------------------------------
	//						  simple is and get methods
	//
	//	Do not replace these methods with native MEFlow methods; we need these
	//	methods to handle arrows that touch superatoms and also 2-electron
	//	atom-to-atom arrows.
	//----------------------------------------------------------------------
	/** Gets whether this arrow's source is an atom.  
	 * @return	whether this arrow's source is an atom
	 */
	boolean srcIsAtom() 			{ return isAtom(getSource()); }
	/** Gets whether this arrow's source is a bond.  
	 * @return	whether this arrow's source is a bond
	 */
	boolean srcIsBond() 			{ return isBond(getSource()); }
	/** Gets whether this arrow's sink is an atom.
	 * @return	whether this arrow's sink is an atom
	 */
	boolean sinkIsAtom() 			{ return isAtom(getSink()); }
	/** Gets whether this arrow's sink is a bond.  
	 * @return	whether this arrow's sink is a bond
	 */
	boolean sinkIsBond() 			{ return isBond(getSink()); }
	/** Gets whether this arrow's sink is an incipient bond.  
	 * @return	whether this arrow's sink is an incipient bond
	 */
	boolean sinkIsIncipBond() 		{ return isIncipBond(getSink()); }
	/** Gets the electron-flow arrow.
	 * @return	the electron-flow arrow
	 */
	public MEFlow getMEFlow()		{ return meFlow; }
	/** Gets the number of electrons represented by the electron-flow arrow.
	 * @return	number of electrons of the electron-flow arrow
	 */
	public int getNumElectrons()	{ return meFlow.getNumElectrons(); }
	/** Gets the 0-based index of the electron-flow arrow in the MDocument of 
	 * the response.
	 * @return	0-based index of the electron-flow arrow
	 */
	public int getObjectIndex() 	{ return objectIndex; }
	/** Gets the atom at the source of this arrow.  Use only after srcIsAtom(). 
	 * @return	the atom at the source of this arrow
	 */
	MolAtom getSrcAtom() 			{ return getAtom(getSource()); }
	/** Gets the atom at the sink of this arrow.  Use only after sinkIsAtom(). 
	 * @return	the atom at the sink of this arrow
	 */
	MolAtom getSinkAtom() 			{ return getAtom(getSink()); }
	/** Gets the bond at the source of this arrow.  Use only after srcIsBond(). 
	 * @return	the bond at the source of this arrow
	 */
	MolBond getSrcBond() 			{ return getBond(getSource()); }
	/** Gets the bond at the sink of this arrow.  Use only after sinkIsBond(). 
	 * @return	the bond at the sink of this arrow
	 */
	MolBond getSinkBond() 			{ return getBond(getSink()); }
	/** Gets the atom or atoms at the arrow source.  
	 * @return	the atom or atoms at the arrow source
	 */
	MolAtom[] getSrcAtoms() 		{ return getAtoms(getSource()); }
	/** Gets the atom or atoms at the arrow sink.  
	 * @return	the atom or atoms at the arrow sink
	 */
	MolAtom[] getSinkAtoms() 		{ return getAtoms(getSink()); }
	/** Gets all the atoms touched by this electron-flow arrow.
	 * @return	array of atoms touched by this electron-flow arrow
	 */
	MolAtom[] getFlowAtoms() 		{ return getFlowAtoms(getMEFlow()); }

	/** Gets all the atoms touched by a ChemAxon electron-flow arrow.
	 * @param	flow	the ChemAxon electron-flow arrow
	 * @return	array of atoms touched by the electron-flow arrow
	 */
	static MolAtom[] getFlowAtoms(MEFlow flow) {
		return (MolAtom[]) ArrayUtils.addAll(
				flow.getSourceAtoms(), flow.getSinkAtoms());
	} // getFlowAtoms(MEFlow)

	//------------------------------------------------------------------------
	//							srcAtomNotSinkAtom
	//------------------------------------------------------------------------
	/** Determines if a source atom of an electron-flow arrow is not also a sink
	 * atom.
	 * @param	srcAtom	a source atom of this flow
	 * @return	true if the source atom is not also a sink atom
	 */
	public boolean srcAtomNotSinkAtom(MolAtom srcAtom) {
		boolean notSinkAtom = true;
		for (final MolAtom snkAtom : getSinkAtoms()) {
			if (snkAtom == srcAtom) { // pointer equality
				notSinkAtom = false;
				break;
			} // if source atom is a sink atom
		} // for each sink atom
		return notSinkAtom;
	} // srcAtomNotSinkAtom(MolAtom)

	//------------------------------------------------------------------------
	//						sinkOrSrcIsOthersSrcOrSink
	//------------------------------------------------------------------------
	/** Determines if this electron-flow arrow's source is the sink of the given
	 * electron-flow arrow, or vice versa.
	 * @param	flow	the other electron-flow arrow
	 * @return true if this electron-flow arrow's source is the sink of the
	 * given electron-flow arrow, or vice versa
	 */
	public boolean sinkOrSrcIsOthersSrcOrSink(MechFlow flow) {
		return getSource() == flow.getSink() || getSink() == flow.getSource();
	} // sinkOrSrcIsOthersSrcOrSink(MechFlow)

	//------------------------------------------------------------------------
	//							getFlowCode
	//------------------------------------------------------------------------
	/** Gets a code for this electron-flow arrow, as in, "two electrons, atom to
	 * incipient bond".  Calculated as (sourceType * 10) + sinkType, 
	 * negative if it is a one-electron arrow.  The code characterizes each
	 * electron-flow arrow, but the information therein is never deconvoluted.
	 * @return	code for this electron-flow arrow
	 */
	int getFlowCode() {
		final int code = getCode(getSource()) * 10 + getCode(getSink());
		return (getNumElectrons() == 2 ? code : -code); 
	} // getFlowCode()

	/** Encodes the nature (atom, bond, or incipient bond) of the source or sink 
	 * of an electron-flow arrow.
	 * @param	flowSinkOrSource	the source or sink of an electron-flow arrow 
	 * @return	code indicating whether the source or sink is an atom, bond, or 
	 * incipient bond
	 */
	private int getCode(Object flowSinkOrSource) {
		if (isAtom(flowSinkOrSource)) return ATOM;
		else if (isBond(flowSinkOrSource)) return BOND;
		else if (isIncipBond(flowSinkOrSource)) return INCIP_BOND;
		else return UNKNOWN;
	} // getCode(Object)

	//----------------------------------------------------------------------
	//					private get, is methods
	//
	//	Do not replace these methods with native MEFlow methods; we need these
	//	methods to handle arrows that touch superatoms and also 2-electron
	//	atom-to-atom arrows.
	//----------------------------------------------------------------------
	/** Gets the origin of the electron-flow arrow (MolAtom or MolBond).
	 * @return	origin of the electron-flow arrow
	 */
	private Object getSource()						{ return source; }
	/** Gets the destination of the electron-flow arrow (MolAtom, MolBond, or
	 * MolAtom[2] for incipient bond).
	 * @return	destination of the electron-flow arrow
	 */
	private Object getSink()	 					{ return sink; }
	/** Gets the atom at the terminus of an arrow.  
	 * @param	t	the arrow terminus
	 * @return	the atom at the terminus of an arrow
	 */
	private static MolAtom getAtom(Object t) 		{ return (MolAtom) t; } 
	/** Gets the bond at the terminus of an arrow.  
	 * @param	t	the arrow terminus
	 * @return	the bond at the terminus of an arrow
	 */
	private static MolBond getBond(Object t) 		{ return (MolBond) t; }
	/** Gets the atoms of the incipient bond at the terminus of an arrow.  
	 * @param	t	the arrow terminus
	 * @return	the atoms of the incipient bond at the terminus of an arrow
	 */
	private static MolAtom[] getIncipBond(Object t) { return (MolAtom[]) t; }
	/** Gets whether the arrow's terminus is an atom.  
	 * @param	t	the arrow terminus
	 * @return	whether the arrow's terminus is an atom
	 */
	private static boolean isAtom(Object t) 		{ return t instanceof MolAtom; }
	/** Gets whether the arrow's terminus is a bond.  
	 * @param	t	the arrow terminus
	 * @return	whether the arrow's terminus is a bond
	 */
	private static boolean isBond(Object t) 		{ return t instanceof MolBond; }
	/** Gets whether the arrow's sink is an incipient bond.  
	 * @param	t	the arrow sink
	 * @return	whether the arrow's sink is an incipient bond
	 */
	private static boolean isIncipBond(Object t) 	{ return t instanceof MolAtom[]; }

	/** Gets the atom or atoms at the terminus of an arrow as a one- or
	 * two-member array.  
	 * @param	terminus	the arrow terminus
	 * @return	array containing the one or two atoms at the terminus of an
	 * arrow
	 */
	private static MolAtom[] getAtoms(Object terminus) {
		final String SELF = "MechFlow.getAtoms: ";
		MolAtom[] atoms;
		if (isAtom(terminus)) {
			debugPrint(SELF + "arrow terminus is atom.");
			atoms = new MolAtom[] {getAtom(terminus)};
		} else if (isBond(terminus)) {
			debugPrint(SELF + "arrow terminus is bond.");
			final MolBond bond = getBond(terminus);
			atoms = new MolAtom[] {bond.getAtom1(), bond.getAtom2()};
		} else if (isIncipBond(terminus)) {
			debugPrint(SELF + "arrow terminus is incipient bond.");
			final MolAtom[] incipBond = getIncipBond(terminus);
			atoms = new MolAtom[] {incipBond[0], incipBond[1]};
		} else {
			if (terminus != null) {
				debugPrint(SELF + "terminus is of unknown type ",
						terminus.getClass().getName(), 
						", getting empty array.");
			} else debugPrint(SELF + "terminus is null, getting empty array.");
			atoms = new MolAtom[0];
		}
		return atoms;
	} // getAtoms(Object)

	//----------------------------------------------------------------------
	//					removeSgroupTermini
	//----------------------------------------------------------------------
	/** Replaces source and sink atoms that are shortcut groups with 
	 * their attachment points, and ungroups all shortcut groups that contain 
	 * source and sink atoms.  Invoked only when MechFlow() called by
	 * MechSolver.getDocAndFlows().  This method works only if the MEFlow of 
	 * this MechFlow is not going to be exported into an MRV document.
	 * @param	flowAtoms	atoms from source or sink of an electron-flow arrow
	 * @param	molecule	the molecule from which the arrow derives; modified
	 * by this method by ungrouping shortcut groups if they are touched by
	 * electron-flow arrows
	 * @return	true if a flow arrow terminus was a shortcut group (may remove
	 * shortcut groups and still return false if the terminus was within a
	 * shortcut group but was not the shortcut group itself)
	 */
	private boolean removeSgroupTermini(MolAtom[] flowAtoms, 
			Molecule molecule) {
		final String SELF = "MechFlow.removeSgroupTermini: ";
		boolean terminusWasSGroup = false;
		if (molecule == null) return terminusWasSGroup;
		boolean removedGroup = false;
		final Sgroup[] sGroups = molecule.getSgroupArray();
		final ArrayList<Sgroup> sGroupList = 
				new ArrayList<Sgroup>(Arrays.asList(sGroups));
		final int numFlowAtoms = flowAtoms.length;
		final int numSGroups = sGroups.length;
		debugPrint(SELF + "looking at flow atom(s)", flowAtoms, 
				" and ", numSGroups, " shortcut group(s).");
		for (int atNum = 0; atNum < numFlowAtoms; atNum++) {
			final MolAtom flowAtom = flowAtoms[atNum];
			if (flowAtom instanceof SgroupAtom) {
				final SgroupAtom superAtom = (SgroupAtom) flowAtom;
				final SuperatomSgroup sGroup = superAtom.getSgroup();
				flowAtoms[atNum] = sGroup.findAttachAtom();
				if (flowAtoms[atNum] != null) {
					debugPrint(SELF + "removing ", superAtom, 
							molecule.indexOf(superAtom));
					molecule.ungroupSgroup(sGroup);
					terminusWasSGroup = true;
					removedGroup = true;
				} // if there was an attachment point
			} // if flow atom is a shortcut group
			for (int sgNum = sGroupList.size() - 1; sgNum >= 0; sgNum--) {
				final Sgroup sGroup = sGroupList.get(sgNum);
				if (sGroup.getSgroupType().equals(SgroupType.SUPERATOM)) {
					final SuperatomSgroup atomGroup = (SuperatomSgroup) sGroup;
					if (atomGroup.hasAtom(flowAtom)) {
						final SgroupAtom superAtom = atomGroup.getSuperAtom();
						debugPrint(SELF, superAtom, molecule.indexOf(superAtom),
								" contains ", flowAtom, "; removing.");
						sGroupList.remove(sgNum);
						molecule.ungroupSgroup(sGroup);
						removedGroup = true;
					} // if flow atom is in a shortcut group
				} // if shortcut group is a superatom group
			} // for each shortcut group
		} // for each atom in terminus
		if (removedGroup) {
			debugPrintMRV(SELF, "removed one or more shortcut groups.");
		} //  if a group was removed
		return terminusWasSGroup;
	} // removeSgroupTermini(MolAtom[], Molecule)

	//------------------------------------------------------------------------
	//							print
	//------------------------------------------------------------------------
	/** Prints details about the electron-flow arrow.  */
	void print() {
		final MolAtom[] srcAtoms = getSrcAtoms();
		final Molecule parent = (Molecule) srcAtoms[0].getParent();
		print(parent);
	} // print()

	/** Prints details about the electron-flow arrow.
	 * @param	parent	molecule containing the atoms of the electron-flow
	 * arrows
	 */
	void print(Molecule parent) {
		final MolAtom[] srcAtoms = getSrcAtoms();
		debugPrint("	Parent molecule is ", parent);
		if (parent != null) {
			if (srcIsAtom()) {
				debugPrint("   Source Atom: ", srcAtoms[0],
						parent.indexOf(srcAtoms[0]) + 1);
			} else if (srcIsBond()) {
				debugPrint("   Source Bond: ", srcAtoms[0],
						parent.indexOf(srcAtoms[0]) + 1, " to ",
						srcAtoms[1], parent.indexOf(srcAtoms[1]) + 1,
						", order ", getSrcBond().getType());
			} else debugPrint("	Source:	INVALID! Neither atom nor bond");
		} else {
			if (srcIsAtom()) {
				debugPrint("   Source Atom: ", srcAtoms[0]);
			} else if (srcIsBond()) {
				debugPrint("   Source Bond: ", srcAtoms[0],
						" to ", srcAtoms[1], 
						", order ", getSrcBond().getType());
			} else debugPrint("	Source:	INVALID! Neither atom nor bond");
		}
		final MolAtom[] sinkAtoms = getSinkAtoms();
		if (parent != null) {
			if (sinkIsAtom()) {
				debugPrint("   Sink Atom: ", sinkAtoms[0],
						parent.indexOf(sinkAtoms[0]) + 1);
			} else if (sinkIsBond()) {
				debugPrint("   Sink Bond: ", sinkAtoms[0],
						parent.indexOf(sinkAtoms[0]) + 1, " to ",
						sinkAtoms[1], parent.indexOf(sinkAtoms[1]) + 1,
						", order ", getSinkBond().getType());
			} else if (sinkIsIncipBond()) {
				debugPrint("   Sink Incip Bond: ", sinkAtoms[0],
						parent.indexOf(sinkAtoms[0]) + 1, " to ",
						sinkAtoms[1], parent.indexOf(sinkAtoms[1]) + 1);
			} else debugPrint("	Sink: INVALID! Neither atom "
					+ "nor bond nor incipient bond");
		} else {
			if (sinkIsAtom()) {
				debugPrint("   Sink Atom: ", sinkAtoms[0]);
			} else if (sinkIsBond()) {
				debugPrint("   Sink Bond: ", sinkAtoms[0],
						" to ", sinkAtoms[1], 
						", order ", getSinkBond().getType());
			} else if (sinkIsIncipBond()) {
				debugPrint("   Sink Incip Bond: ", sinkAtoms[0],
						" to ", sinkAtoms[1]);
			} else debugPrint("	Sink: INVALID! Neither atom "
					+ "nor bond nor incipient bond");
		}
	} // print(Molecule)

} // MechFlow
