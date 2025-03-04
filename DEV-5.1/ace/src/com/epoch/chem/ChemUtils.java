package com.epoch.chem;

import chemaxon.calculations.clean.Cleaner; // new to Marvin 5.9
import chemaxon.calculations.hydrogenize.Hydrogenize; // new to Marvin 5.9
import chemaxon.core.calculations.valencecheck.ValenceCheckOptions;
import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.io.MPropHandler;
import chemaxon.marvin.io.MolExportException;
import chemaxon.sss.search.MolSearch;
import chemaxon.sss.search.SearchException;
import chemaxon.struc.AtomProperty.Radical;
import chemaxon.struc.DPoint3;
import chemaxon.struc.MDocument;
import chemaxon.struc.Molecule;
import chemaxon.struc.MoleculeGraph;
import chemaxon.struc.MolBond;
import chemaxon.struc.MolAtom;
import chemaxon.struc.PeriodicSystem;
import chemaxon.struc.Sgroup;
import chemaxon.struc.SgroupType;
import chemaxon.struc.sgroup.MulticenterSgroup;
import chemaxon.struc.sgroup.SgroupAtom;
import chemaxon.struc.sgroup.SuperatomSgroup;
import com.epoch.chem.chemConstants.ChemConstants;
import com.epoch.evals.impl.chemEvals.MapProperty;
import com.epoch.exceptions.ValenceException;
import com.epoch.lewis.lewisConstants.LewisConstants;
import com.epoch.substns.RGroupDefs;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;

/** Holds utility chemistry functions involving manipulations 
 * of molecules (e.g., getting enantiomers, adding explicit H atoms),
 * electron-counting methods. */
public final class ChemUtils 
		implements ChemConstants, LewisConstants {

	private static void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

/* ***************** Molecule manipulations *******************/

	/** Sets that a molecule was drawn in MarvinJS.
	 * @param	molStr	the molecule
	 * @return	the MRV string with the property stored
	 */
	public static String setFromMarvinJS(String molStr) {
		return setWhetherFromMarvinJS(molStr, true);
	} // setFromMarvinJS(String)

	/** Sets that a molecule was drawn in MarvinJS.
	 * @param	mol	the molecule
	 */
	public static void setFromMarvinJS(Molecule mol) {
		setWhetherFromMarvinJS(mol, true);
	} // setFromMarvinJS(Molecule)

	/** Sets whether a molecule was drawn in MarvinJS.
	 * @param	molStr	the molecule
	 * @param	fromMarvinJS	true if the molecule was drawn in MarvinJS
	 * @return	the MRV string with the property stored
	 */
	public static String setWhetherFromMarvinJS(String molStr, 
			boolean fromMarvinJS) {
		return setProperty(molStr, FROM_MARVIN_JS, 
				String.valueOf(fromMarvinJS));
	} // setWhetherFromMarvinJS(String, boolean)

	/** Sets whether a molecule was drawn in MarvinJS.
	 * @param	mol	the molecule
	 * @param	fromMarvinJS	true if the molecule was drawn in MarvinJS
	 */
	public static void setWhetherFromMarvinJS(Molecule mol, 
			boolean fromMarvinJS) {
		mol.setProperty(FROM_MARVIN_JS, String.valueOf(fromMarvinJS));
	} // setWhetherFromMarvinJS(Molecule, boolean)

	/** Adds a molecule property to the MRV representing a molecule.
	 * @param	molStr	the MRV string
	 * @param	propName	name of the property
	 * @param	value	value of the property
	 * @return	the MRV string with the property stored
	 */
	public static String setProperty(String molStr, String propName,
			String value) {
		final String SELF = "ChemUtils.setProperty: ";
		debugPrint(SELF + "propName = ", propName, ", value = ", value);
		try {
			final Molecule mol = MolImporter.importMol(molStr);
			if (mol == null) {
				debugPrint(SELF + "mol is null");
				throw new MolFormatException();
			}
			mol.setProperty(propName, value);
			MDocument mdoc = mol.getDocument();
			if (mdoc == null) {
				debugPrint(SELF + "mdoc is null; creating a new one");
				mdoc = new MDocument(mol);
				debugPrint(SELF + "mdoc is ", 
						mdoc == null ? "still" : "no longer", " null");
			}
			final String modMolStr = 
					Utils.unicodeToCERs(MolString.toString(mdoc, MRV));
			debugPrint(SELF + "returning:\n", modMolStr);
			return modMolStr;
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "could not add molecule property to:\n", 
					molStr);
			e.printStackTrace();
		} catch (MolExportException e) {
			Utils.alwaysPrint(SELF + "could not add molecule property to:\n", 
					molStr);
			e.printStackTrace();
		} // try
		return molStr; 
	} // setProperty(String, String, String)

	/** Gets the value of a property of a molecule.
	 * @param	molStr	the molecule in String format
	 * @param	propName	the name of the property to get
	 * @return	the property value, as a string
	 */
	public static String getProperty(String molStr, String propName) {
		final String SELF = "ChemUtils.getProperty: ";
		try {
			final Molecule mol = MolImporter.importMol(molStr);
			return getProperty(mol, propName);
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "could get molecule from:\n", 
					molStr);
			e.printStackTrace();
		} // try
		return null;
	} // getProperty(String, String)

	/** Gets the value of a property of a molecule.
	 * @param	mol	the molecule
	 * @param	propName	the name of the property to get
	 * @return	the property value, as a string
	 */
	public static String getProperty(Molecule mol, String propName) {
		return MPropHandler.convertToString(mol.properties(), propName);
	} // getProperty(Molecule, String)

	/** Gets whether a molecule was drawn in MarvinJS.
	 * @param	molStr	the molecule in String format
	 * @return	true if the molecule was drawn in MarvinJS
	 */
	public static boolean getWhetherFromMarvinJS(String molStr) {
		final String SELF = "ChemUtils.getWhetherFromMarvinJS: ";
		try {
			final Molecule mol = MolImporter.importMol(molStr);
			return getWhetherFromMarvinJS(mol);
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "could get molecule from:\n", 
					molStr);
			e.printStackTrace();
		} // try
		return false;
	} // getWhetherFromMarvinJS(String)

	/** Gets whether a molecule was drawn in MarvinJS.
	 * @param	mol	the molecule
	 * @return	true if the molecule was drawn in MarvinJS
	 */
	public static boolean getWhetherFromMarvinJS(Molecule mol) {
		return "true".equals(getProperty(mol, FROM_MARVIN_JS));
	} // getWhetherFromMarvinJS(Molecule)

	/** Gets a String representation of a molecule's mirror image simply by
	 * multiplying all the molecule's <i>x</i>-coordinates by &minus;1.
	 * @param	molStruct	String representation of a molecule
	 * @return	MRV representation of the mirror image
	 */
	public static String getMirror(String molStruct) {
		try {
			final Molecule mol = MolImporter.importMol(molStruct);
			mirror(mol);
			return MolString.toString(mol, MRV);
		} catch (Exception e) {
			debugPrint("ChemUtils.getMirror: "
					+ "Not a recognized format; returning molStruct");
			return molStruct;
		}
	} // getMirror(String)

	/** Gets a molecule's mirror image simply by
	 * multiplying all its copy's <i>x</i>-coordinates by &minus;1.
	 * @param	origMol	a molecule
	 * @return	the mirror image of a copy of the original molecule
	 */
	public static Molecule getMirror(Molecule origMol) {
		final Molecule mol = origMol.clone();
		mirror(mol);
		return mol;
	} // getMirror(Molecule)

	/** Converts a molecule to its mirror image simply by multiplying
	 * all its <i>x</i>-coordinates by &minus;1.  Modifies the original 
	 * molecule.
	 * @param	mol	a molecule
	 */
	public static void mirror(Molecule mol) {
		StereoFunctions.convert0DTo2D(mol);
		for (final MolAtom atom : mol.getAtomArray()) {
			atom.setX(-atom.getX());
		}
	} // mirror(Molecule)

	/** Gets a new String representation of this molecule in which implicit H
	 * atoms are converted to explicit ones.
	 * @param	molStruct	String representation of a molecule
	 * @return	MRV representation of the molecule with all H atoms explicit
	 */
	public static String explicitizeH(String molStruct) {
		try {
			Molecule mol = MolImporter.importMol(molStruct);
			mol = explicitizeH(mol);
			return MolString.toString(mol, MRV);
		} catch (Exception e) {
			debugPrint("ChemUtils.explicitizeH: "
					+ "Not a recognized format; returning molStruct");
			return molStruct;
		}
	} // explicitizeH(String)

	/** Gets a copy of this molecule in which implicit H atoms are converted
	 * to explicit ones.
	 * @param	origMol	a molecule
	 * @return	a copy of the original molecule with all H atoms explicit
	 */
	public static Molecule explicitizeH(Molecule origMol) {
		final Molecule mol = origMol.clone();
		explicitizeHnoClone(mol);
		return mol;
	} // explicitizeH(Molecule)

	/** Converts implicit H atoms to explicit ones.  Modifies the original!
	 * @param	mol	a molecule
	 */
	public static void explicitizeHnoClone(Molecule mol) {
		final String SELF = "ChemUtils.explicitizeHnoClone: ";
		final int origNumBonds = mol.getBondCount();
		final Molecule origMolCopy = mol.clone();
		debugPrintMRV(SELF + "starting with:\n", mol);
		Hydrogenize.convertImplicitHToExplicit(mol);
		debugPrintMRV(SELF + "after explicitizing H atoms:\n", mol);
		for (int bNum = 0; bNum < origNumBonds; bNum++) {
			final MolBond bond = mol.getBond(bNum);
			final int type = bond.getType();
			final MolBond origBondCopy = origMolCopy.getBond(bNum);
			final int origType = origBondCopy.getType();
			if (type != origType) {
				debugPrint(SELF + "resetting a ", 
						bond.getAtom1(), "-", bond.getAtom2(),
						" bond from type ", type,
						" back to original type ", origType);
				bond.setType(origType);
			} // if stereo has been changed by hydrogenizing
		} // for each original bond in mol
		for (int bNum = origNumBonds; bNum < mol.getBondCount(); bNum++) {
			final MolBond bond = mol.getBond(bNum);
			final int stereo = StereoFunctions.getBondStereoFlags(bond);
			if (stereo == MolBond.WAVY) {
				debugPrint(SELF + "straightening a new wavy bond to H...");
				StereoFunctions.removeBondStereoFlags(bond);
			} // if the new bond (to H) is wavy
		} // for each new bond in mol
	} // explicitizeHnoClone(Molecule)

	/** Makes explicit H atoms implicit. 
	 * @param	mol	the molecule
	 */
	public static void implicitizeH(Molecule mol) {
		implicitizeH(mol, 0);
	} // implicitizeH(Molecule)

	/** Makes explicit H atoms implicit. 
	 * @param	mol	the molecule
	 * @param	flags	flags governing which C atoms NOT to remove H atoms from
	 */
	public static void implicitizeH(Molecule mol, int flags) {
		Hydrogenize.convertExplicitHToImplicit(mol, flags);
	} // implicitizeH(Molecule, int)

	/** Makes a molecule pretty. 
	 * @param	mol	the molecule
	 */
	public static void clean2D(MoleculeGraph mol) {
		clean(mol, 2, null);
	} // clean2D(MoleculeGraph)

	/** Makes a molecule pretty. 
	 * @param	mol	the molecule
	 * @param	opts	options
	 */
	public static void clean2D(MoleculeGraph mol, String opts) {
		clean(mol, 2, opts);
	} // clean2D(MoleculeGraph, String)

	/** Makes a molecule pretty. 
	 * @param	mol	the molecule
	 */
	public static void clean3D(MoleculeGraph mol) {
		clean(mol, 3, null);
	} // clean3D(MoleculeGraph)

	/** Makes a molecule pretty. 
	 * @param	mol	the molecule
	 * @param	opts	options
	 */
	public static void clean3D(MoleculeGraph mol, String opts) {
		clean(mol, 3, opts);
	} // clean3D(MoleculeGraph, String)

	/** Makes a molecule pretty. 
	 * @param	mol	the molecule
	 * @param	dim	the dimensions of the molecule to use for cleaning
	 * @param	opts	options
	 */
	public static void clean(MoleculeGraph mol, int dim, String opts) {
		Cleaner.clean(mol, dim, opts);
	} // clean(MoleculeGraph, int, String)

	/** Given a SMILES definition or a shortcut group name, return a Molecule
	 * representation, with the molecule's
	 * ATTACH_PT property set to a 1-based atom number.  First try using
	 * the R groups Hashtable, which also gives the attachment point; if that
	 * fails, just convert from SMILES, treating the first atom as the
	 * attachment point.  Special case: abbrev="H" means "[H]"; SMILES is
	 * unfortunately obscure here.
	 * NOTE: Assumes that the shortcut group has a single attachment point.
	 * @param	abbrev	name of the shortcut group, such as Ph
	 * @return	molecule representing the shortcut group, with the ATTACH_PT
	 * property set to a 1-based atom number
	 * @throws	MolFormatException	if the shortcut group can't be imported into
	 * a Molecule
	 */
	public static Molecule getSGroupMolecule(final String abbrev)
			throws MolFormatException {
		String myAbbrev = abbrev;
		final String[] group = RGroupDefs.getGroup(myAbbrev);
		Molecule groupMol = null;
		try {
			if (group == null) {
				// not a recognized shortcut group; probably an element
				if ("H".equals(myAbbrev)) myAbbrev = "[H]"; // special case
				// create molecule using group formula
				groupMol = MolImporter.importMol(myAbbrev);
				// make first atom attachment point
				groupMol.setProperty(ATTACH_PT, "1");
			} else {
				// defined among abbrevgroups (shortcut groups)
				groupMol = MolImporter.importMol(group[RGroupDefs.DEF]);
				final int attachPt = Integer.parseInt(group[RGroupDefs.ATTACH]);
				groupMol.setProperty(ATTACH_PT,
						attachPt > 0 ? String.valueOf(attachPt) : "1");
			}
		} catch (MolFormatException e) {
			System.out.println("ChemUtils.getSGroupMolecule: couldn't "
					+ "get Molecule from "
					+ (group == null ? myAbbrev : group[RGroupDefs.DEF]));
			e.printStackTrace();
			throw e;
		}
		return groupMol;
	} // getSGroupMolecule(String)

	/** Get the attachment point atom as defined by the attachPt
	 * property set in getSGroupMolecule().
	 * @param	mol	a molecule representing a shortcut group or fragment
	 * @return	the atom that is the attachment point of this shortcut group or
	 * fragment; null if not found, atom 1 if no ATTACH_PT property is set
	 */
	public static MolAtom getAttachmentPoint(Molecule mol) {
		if (mol == null) return null;
		final int count = mol.getAtomCount();
		int attachPt = 1;
		if (count > 1) {
			try {
				final String attachPtStr = getProperty(mol, ATTACH_PT);
				attachPt = Integer.parseInt(attachPtStr);
			} catch (Exception e) { // NullPointer, NumberFormat
				Utils.alwaysPrintMRV("ChemUtils.getAttachmentPoint: "
						+ "attachPt property of ", mol,
						"not set to an integer; returning "
						+ "first atom.");
			}
		} // if there's more than one atom
		return (count > 0 ? mol.getAtom(attachPt - 1) : null);
	} // getAttachmentPoint(Molecule)

	/** Gets the first selected atom.
	 * @param	mol	a molecule
	 * @return	the first atom that has been selected
	 */
	public static MolAtom getSelectedAtom(Molecule mol) {
		MolAtom selectedAtom = null;
		for (final MolAtom atom : mol.getAtomArray()) {
			if (atom.isSelected()) {
				selectedAtom = atom;
				break;
			} // if found a selected atom
		} // for each atom
		return selectedAtom;
	} // getSelectedAtom(Molecule)

	/** Selects the atoms with the indices in the string.
	 * @param	molStr	MRV of the molecule
	 * @param	selectionsStr	comma-separated string of 1-based atom indices
	 * @return	new MRV with atoms selected
	 */
	public static String setSelections(String molStr, String selectionsStr) {
		String modMolStr = molStr;
		if (molStr != null) try {
			final Molecule mol = MolImporter.importMol(molStr);
			setSelections(mol, selectionsStr);
			modMolStr = MolString.toString(mol, MRV);
		} catch (MolFormatException e) {
			Utils.alwaysPrint("ChemUtils.setSelections: cannot import mol:\n",
					molStr);
			e.printStackTrace();
		}
		return modMolStr;
	} // setSelections(String, String)

	/** Selects the atoms with the indices in the string.
	 * @param	mol	the molecule
	 * @param	selectionsStr	comma-separated string of 1-based atom indices
	 */
	public static void setSelections(Molecule mol, String selectionsStr) {
		final String[] selectionStrs = 
				selectionsStr.split(MJS_SELECTIONS_DIVIDER);
		for (final String selectionStr : selectionStrs) {
			final int atomNum = MathUtils.parseInt(selectionStr);
			if (atomNum > 0) mol.getAtom(atomNum - 1).setSelected(true);
		} // for each atom to select
	} // setSelections(Molecule, String)

	/** Adds a molecule property to the MRV indicating which atoms have been
	 * selected. Needed only for Marvin pre-5.8 (?).
	 * @param	molStr	MRV of the molecule
	 * @return	MRV modified with the molecule property
	 */
	public static String storeSelectedAtoms(String molStr) {
		String modMolStr = molStr;
		if (molStr != null) try {
			final Molecule mol = MolImporter.importMol(molStr);
			final String selectionsStr = 
					Utils.join(getSelectedAtomNums(mol), MapProperty.SEL_DIV);
			mol.setProperty(MapProperty.SELECTED, selectionsStr);
			modMolStr = MolString.toString(mol, MRV);
		} catch (MolFormatException e) {
			Utils.alwaysPrint("ChemUtils.storeSelectedAtoms: "
					+ "cannot import mol:\n", molStr);
			e.printStackTrace();
		}
		return modMolStr;
	} // storeSelectedAtoms(String)

	/** Gets a string containing 1-based indices of selected atoms.
	 * @param	molStr	MRV of the molecule
	 * @return	string containing comma-separated 1-based indices of selected 
	 * atoms
	 */
	public static String getSelectionsStr(String molStr) {
		String selectionsStr = "";
		if (molStr != null) try {
			final Molecule mol = MolImporter.importMol(molStr);
			selectionsStr = Utils.join(getSelectedAtomNums(mol),
					MJS_SELECTIONS_DIVIDER);
		} catch (MolFormatException e) {
			Utils.alwaysPrint("ChemUtils.getSelectionsStr: "
					+ "cannot import mol:\n", molStr);
			e.printStackTrace();
		}
		return selectionsStr;
	} // getSelectionsStr(String)

	/** Gets a list of 1-based indices of selected atoms.
	 * @param	mol	the molecule
	 * @return	list of 1-based indices of selected atoms
	 */
	private static List<Integer> getSelectedAtomNums(Molecule mol) {
		final List<Integer> selectedAtomNums = new ArrayList<Integer>();
		int atomNum = 1;
		for (final MolAtom atom : mol.getAtomArray()) {
			if (atom.isSelected()) {
				selectedAtomNums.add(Integer.valueOf(atomNum));
			} // if atom is selected
			atomNum++;
		} // for each atom
		return selectedAtomNums;
	} // getSelectedAtomNums(Molecule)

	/** Ungroups and aromatizes shortcut groups; preserves any map number
	 * by putting it on the attachment point.
	 * Called by evals/impl/BondAngle.java and MapProperty.java.
	 * @param	mol	a molecule
	 */
	public static void ungroupRemapSGroups(Molecule mol) {
		final String SELF = "ChemUtils.ungroupRemapSGroups: ";
		debugPrintMRV(SELF + "starting with:\n", mol);
		final List<int[]> atomsMaps = new ArrayList<int[]>();
		final Sgroup[] sgroups = mol.getSgroupArray();
		for (final Sgroup sgroupRaw : sgroups) {
			final SgroupType sgroupType = sgroupRaw.getSgroupType();
			if (sgroupType.equals(SgroupType.SUPERATOM)) {
				final SuperatomSgroup sgroup = (SuperatomSgroup) sgroupRaw;
				final SgroupAtom superatom = sgroup.getSuperAtom();
				final String symb = superatom.getSymbol();
				final int mapNum = superatom.getAtomMap();
				if (mapNum > 0) {
					final MolAtom attachPt = sgroup.findAttachAtom();
					if (attachPt != null) {
						final int attachIndex = mol.indexOf(attachPt);
						atomsMaps.add(new int[] {attachIndex, mapNum});
						debugPrint(SELF + "shortcut group ", symb, 
								" has map number ", mapNum,
								" and attachment point ", attachIndex);
					} // if there's an attachment point
				} else {
					debugPrint(SELF + "shortcut group ", symb, " is unmapped.");
				} // if mapNum
			} else {
				debugPrint(SELF + "S group found, but not a shortcut group.");
			} // if sgroupType
		} // for each group
		mol.ungroupSgroups(SHORTCUT_GROUPS);
		for (final int[] atomMap : atomsMaps) {
			final MolAtom attachPt = mol.getAtom(atomMap[0]);
			attachPt.setAtomMap(atomMap[1]);
		}
		debugPrintMRV(SELF + "returning:\n", mol);
	} // ungroupRemapSGroups(Molecule)

	/** Converts shortcut groups to pseudoatoms; preserves any map numbers.
	 * Modifies the original!  Called by evals/impl/MapProperty.java.
	 * @param	mol	a molecule
	 */
	public static void sGroupsToPseudoatoms(Molecule mol) {
		final String SELF = "ChemUtils.sGroupsToPseudoatoms: "; 
		debugPrintMRV(SELF + "starting with:\n", mol);
		final Sgroup[] sgroups = mol.getSgroupArray();
		for (final Sgroup sgroupRaw : sgroups) {
			final SuperatomSgroup sgroup = (SuperatomSgroup) sgroupRaw;
			final SgroupAtom superatom = sgroup.getSuperAtom();
			final String symb = superatom.getSymbol();
			final int mapNum = superatom.getAtomMap();
			final MolAtom attachPt = sgroup.findAttachAtom();
			final MolAtom[] sgAtoms = sgroup.getAtomArray();
			final int numSgAtoms = sgAtoms.length;
			for (int sgAtomNum = numSgAtoms - 1; sgAtomNum >= 0; sgAtomNum--) {
				final MolAtom sgAtom = sgAtoms[sgAtomNum];
				// In the following line, we really mean pointer equality.
				if (sgAtom == attachPt) {
					sgAtom.setAtno(MolAtom.PSEUDO);
					sgAtom.setAliasstr(symb);
					if (mapNum > 0) {
						sgAtom.setAtomMap(mapNum);
						debugPrint(SELF + "shortcut group ",
								symb, " has map number ", mapNum);
					} else {
						debugPrint(SELF + "shortcut group ",
								symb, " is unmapped.");
					} // if atom is mapped
				} else { // sgroup atom is not attachment point
					mol.removeAtom(sgAtom);
				} // if sgroup atom is not attachment point
			} // for each atom in sgroup
		} // for each shortcut group
		mol.ungroupSgroups(SHORTCUT_GROUPS);
		debugPrintMRV(SELF + "converted to:\n", mol);
	} // sGroupsToPseudoatoms(Molecule)

	/** Removes alkali metals and MgX from a molecule's copy, replaces with 
	 * negative charges.
	 * @param	respMolecule	a molecule
	 * @return	copy of the molecule with bonds to metals replaced with
	 * negative charges
	 */
	public static Molecule stripMetals(Molecule respMolecule) {
		final String SELF = "ChemUtils.stripMetals: ";
		debugPrint(SELF + "cloning ", respMolecule);
		final Molecule mol = respMolecule.clone();
		stripMetalsNoClone(mol);
		debugPrint(SELF + "returning ", mol);
		return mol;
	} // stripMetals(Molecule)

	/** Removes alkali metals and MgX from a molecule, replaces with negative 
	 * charges.  Modifies the original!
	 * @param	respMolecule	a molecule
	 */
	public static void stripMetalsNoClone(Molecule respMolecule) {
		final String SELF = "ChemUtils.stripMetals: ";
		if (respMolecule.getAtomCount() <= 1) return;
		debugPrint(SELF + "stripping alkali metals and Mg from ", respMolecule);
		try {
			final MolSearch s = new MolSearch();
			s.setTarget(respMolecule);
			// believe it or not, need H atoms in SMILES strings of univalent
			// metals
			final String[] metals = {"[LiH]", "[NaH]", "[KH]",
					"[RbH]", "[CsH]", "[Mg]"};
			final int[] atnos = {3, 11, 19, 37, 55, 12};
			for (final String metal : metals) {
				final Molecule query = MolImporter.importMol(metal);
				s.setQuery(query);
				while (respMolecule.getAtomCount() > 1) {
					final int[] hit = s.findNext();
					if (hit == null) break;
					final MolAtom metalAtom = respMolecule.getAtom(hit[0]);
					final int bondCt = metalAtom.getBondCount();
					debugPrint(SELF + "found ", query.getFormula(),
							" in ", respMolecule, "; metal has ", bondCt, 
							" bond", bondCt == 1 ? " ." : "s.");
					if (bondCt == 0) {
						metalAtom.setAtno(MolAtom.PSEUDO);
								// disguise the atom to avoid infinite loop
					} else if (bondCt == 1) {
						final MolAtom metalLig = metalAtom.getLigand(0);
						metalLig.setCharge(metalLig.getCharge() - 1);
						respMolecule.removeBond(metalAtom.getBond(0));
						respMolecule.removeAtom(metalAtom);
						debugPrint(SELF + "now cpd is ", respMolecule);
					} // if metal has exactly one bond
					else if (bondCt == 2 && "[Mg]".equals(metal)) {
						final MolAtom metalLig1 = metalAtom.getLigand(0);
						final MolAtom metalLig2 = metalAtom.getLigand(1);
						metalLig1.setCharge(metalLig1.getCharge() - 1);
						metalLig2.setCharge(metalLig2.getCharge() - 1);
						final int bondTo1Idx = respMolecule.indexOf(metalAtom.getBond(0));
						final int bondTo2Idx = respMolecule.indexOf(metalAtom.getBond(1));
						// need to remove bond with larger index first
						respMolecule.removeBond(bondTo1Idx > bondTo2Idx
								? bondTo1Idx : bondTo2Idx);
						respMolecule.removeBond(bondTo1Idx > bondTo2Idx
								? bondTo2Idx : bondTo1Idx);
						if (PeriodicSystem.isHalogen(metalLig1.getAtno())) {
							debugPrint(SELF + "removing ", metalLig1,
									" atom formerly attached to Mg.");
							respMolecule.removeAtom(metalLig1);
						}
						if (PeriodicSystem.isHalogen(metalLig2.getAtno())) {
							debugPrint(SELF + "removing ", metalLig2,
									" atom formerly attached to Mg.");
							respMolecule.removeAtom(metalLig2);
						}
						respMolecule.removeAtom(metalAtom);
						debugPrint(SELF + "now cpd is ", respMolecule);
					} // if metal is Mg and has exactly two bonds
					s.setTarget(respMolecule);
				} // until no more hits or only one atom
				final int metalIndex = Utils.indexOf(metals, metal);
				for (final MolAtom atom : respMolecule.getAtomArray()) {
					if (atom.getAtno() == MolAtom.PSEUDO) {
						atom.setAtno(atnos[metalIndex]);
					} // if the atom is a pseudoatom
				} // for each atom
			} // for each metal
		} catch (MolFormatException e) {
			System.out.println(SELF + "can't convert metal to a molecule");
		} catch (SearchException e) {
			System.out.println(SELF + "SearchException");
		}
	} // stripMetalsNoClone(Molecule)

	/** Arranges three or four atom indices so that they correspond to a
	 * sequence of bonds in the compound. Modifies the original atomIndices 
	 * parameter!
	 * @param	mol	the molecule
	 * @param	atomIndices	0-based indices of three or four atoms to arrange 
	 * in the order in which they are bonded. The method puts the ordered atom
	 * indices back into this array.
	 * @return	true if the atoms are bonded in sequence
	 */
	public static boolean arrangeAsBonded(Molecule mol, int[] atomIndices) {
		final String SELF = "ChemUtils.arrangeAsBonded: ";
		final int numAtoms = atomIndices.length;
		debugPrint(SELF + "unordered atomIndices = ", atomIndices);
		boolean arranged = false;
		if (numAtoms == 3) {
			final List<MolAtom> atoms = new ArrayList<MolAtom>();
			for (int atomIndexNum = 0; atomIndexNum < 3; atomIndexNum++) {
				atoms.add(mol.getAtom(atomIndices[atomIndexNum]));
			} // for each atom index
			for (int atomIndexNum = 0; atomIndexNum < 3; atomIndexNum++) {
				if (atoms.get(1).getBondTo(atoms.get(0)) != null
						&& atoms.get(1).getBondTo(atoms.get(2)) != null) {
					arranged = true;
					break;
				} // if the atom makes two bonds
				atoms.add(atoms.remove(0));
			} // for each atom index
			for (int atomIndexNum = 0; atomIndexNum < 3; atomIndexNum++) {
				final MolAtom atom = atoms.get(atomIndexNum);
				atomIndices[atomIndexNum] = mol.indexOf(atom);
			} // for each atom
		} else if (numAtoms == 4) {
			final MolAtom[] atoms = new MolAtom[4];
			for (int atomIndexNum = 0; atomIndexNum < 4; atomIndexNum++) {
				atoms[atomIndexNum] = mol.getAtom(atomIndices[atomIndexNum]);
			} // for each atom index
			// sort the two middle atoms from the two outside atoms
			final int[] atomNumsOrdered = new int[] {-1, -1, -1, -1};
			for (final int atomIndex : atomIndices) {
				final MolAtom refAtom = mol.getAtom(atomIndex);
				if (getNumBonded(atoms, refAtom) == 2) {
					if (atomNumsOrdered[1] == -1) {
						atomNumsOrdered[1] = atomIndex;
					} else {
						final MolAtom firstAtom =
								mol.getAtom(atomNumsOrdered[1]);
						if (atomNumsOrdered[2] == -1 
								&& refAtom.getBondTo(firstAtom) != null) {
							atomNumsOrdered[2] = atomIndex;
						} else if (atomNumsOrdered[0] == -1) {
							atomNumsOrdered[0] = atomIndex;
						} else if (atomNumsOrdered[3] == -1) {
							atomNumsOrdered[3] = atomIndex;
						} // if atomNumsOrdered[]
					} // if atomNumsOrdered[]
				} else {
					if (atomNumsOrdered[0] == -1) {
						atomNumsOrdered[0] = atomIndex;
					} else if (atomNumsOrdered[3] == -1) {
						atomNumsOrdered[3] = atomIndex;
					} // if atomNumsOrdered[]
				} // if atom is bonded to 1 or 2 other atoms
			} // for each atom
			debugPrint(SELF + "after finding internal atoms, "
					+ "atomNumsOrdered = ", atomNumsOrdered);
			if (atomNumsOrdered[1] != -1 && atomNumsOrdered[2] != -1) {
				// now it's either A-B-C-D or D-B-C-A
				if (mol.getAtom(atomNumsOrdered[0]).getBondTo(
						mol.getAtom(atomNumsOrdered[1])) == null) {
					final int dummy = atomNumsOrdered[0];
					atomNumsOrdered[0] = atomNumsOrdered[3];
					atomNumsOrdered[3] = dummy;
				} // if atom0 not bonded to atom1
				debugPrint(SELF + "after finding external atoms, "
						+ "atomNumsOrdered = ", atomNumsOrdered);
				System.arraycopy(atomNumsOrdered, 0, atomIndices, 0, 4);
				arranged = true;
			} // if there is a central bond
		} // not 3-4 atoms entered
		return arranged;
	} // arrangeAsBonded(Molecule, int[])

	/** Utility method for arrangeAsBonded(). Gets the number of atoms in an array
	 * that are bonded to a reference atom. 
	 * @param	atoms	an array of atoms
	 * @param	refAtom	the reference atom
	 * @return	the number of atoms in atoms that are bonded to refAtom
	 */
	private static int getNumBonded(MolAtom[] atoms, MolAtom refAtom) {
		int numBonds = 0;
		for (final MolAtom atom : atoms) {
			if (atom != refAtom && atom.getBondTo(refAtom) != null) numBonds++;
		} // for each atom
		return numBonds;
	} // getNumBonded(MolAtom[], MolAtom)

/* *********** Electron-counting and periodic table methods ************/

	/** Gets the number of unshared electrons (paired and unpaired) 
	 * of the atom.
	 * @param	atom	the atom
	 * @return	the number of unshared electrons of the atom
	 */
	public static int getElectrons(MolAtom atom) {
		int numElecs = 0;
		if (atom != null) {
			Integer elecsObj = (Integer) atom.getProperty(PAIRED_ELECS);
			if (elecsObj != null) numElecs += elecsObj.intValue();
			elecsObj = (Integer) atom.getProperty(UNPAIRED_ELECS);
			if (elecsObj != null) numElecs += elecsObj.intValue();
		} // if the atom isn't null
		return numElecs;
	} // getElectrons(MolAtom)

	/** Sets the number of unshared electrons of the atom.
	 * We assume that as many electrons as possible are paired.
	 * @param	atom	the atom
	 * @param	numElecs	the number of unshared electrons
	 */
	public static void setElectrons(MolAtom atom, int numElecs) {
		final int unpaired = numElecs % 2;
		final int paired = numElecs - unpaired;
		if (paired == 0) atom.removeProperty(PAIRED_ELECS);
		else atom.putProperty(PAIRED_ELECS, Integer.valueOf(paired));
		if (unpaired == 0) atom.removeProperty(UNPAIRED_ELECS);
		else atom.putProperty(UNPAIRED_ELECS, Integer.valueOf(unpaired));
	} // setElectrons(MolAtom, int)

	/** Clears the number of unshared electrons from the atom.
	 * @param	atom	the atom
	 */
	public static void clearElectrons(MolAtom atom) {
		atom.removeProperty(PAIRED_ELECS);
		atom.removeProperty(UNPAIRED_ELECS);
	} // clearElectrons(MolAtom)

	/** Gets the positive distance between two atoms
	 * @param	atom1	one atom
	 * @param	atom2	the other atom
	 * @return	the distance between the two atoms
	 */
	public static double distance(MolAtom atom1, MolAtom atom2) {
		final DPoint3 locn1 = atom1.getLocation();
		final DPoint3 locn2 = atom2.getLocation();
		return Math.abs(VectorMath.length(VectorMath.diff(locn1, locn2)));
	} // distance(MolAtom, MolAtom)

	/** Get the number of valence electrons of an atom, which
	 * is (1) column for columns 1 to 10, (2) column &minus; 10 for
	 * columns 11 to 18, and (3) for lanthanides &amp; actinides,
	 * 3 + f electrons, except the last column = 3.
	 * Noble gases are treated as having 8 valence electrons.
	 * @param	atom	an atom
	 * @return	the number of valence electrons
	 */
	public static int getValenceElectrons(MolAtom atom) {
		return getValenceElectrons(atom.getAtno());
	} // getValenceElectrons(MolAtom)

	/** Get the number of valence electrons of an atom, which is:
	 * <ol>
	 * <li>for lanthanides &amp; actinides: number of f electrons;
	 * <li>for columns &ge; 13 (B column or heavier)
	 * <li><i>or</i> columns 11-12 and row &le; 5 (Cu, Zn, Ag, Cd): column % 10;
	 * <li>for columns 1 to 10 or for columns 11-12 and row &ge; 6 
	 * (Au, Hg): column.
	 * </ol>
	 * Noble gases are treated as having 8 valence electrons.
	 * @param	atno	atomic number of an atom
	 * @return	the number of valence electrons
	 */
	public static int getValenceElectrons(int atno) {
		final int[] Ce_to_Yb = new int[] {PeriodicSystem.Ce, PeriodicSystem.Yb};
		final int[] Th_to_No = new int[] {PeriodicSystem.Th, PeriodicSystem.No};
		final int column = PeriodicSystem.getColumn(atno);
		return (isMulticenterAtom(atno) ? 0
				: MathUtils.inRange(atno, Ce_to_Yb) ? atno - PeriodicSystem.Xe 
				: MathUtils.inRange(atno, Th_to_No) ? atno - PeriodicSystem.Rn
				: column >= 13 // main block plus Cu, Zn, Ag, Cd 
						|| (column >= 11 && PeriodicSystem.getRow(atno) <= 5) 
					? column % 10 
				: column); // all columns up to 10 plus Au and Hg
	} // getValenceElectrons(int)

	/** Get the maximum number of electrons that can fill an atom's outer shell
	 * (2, 8, 12, 18, or 32).
	 * @param	atom	an atom
	 * @return	the number of electrons required to fill the outer shell
	 */
	public static int getMaxOuterElectrons(MolAtom atom) {
		return getMaxOuterElectrons(atom.getAtno());
	} // getMaxOuterElectrons(MolAtom)

	/** Get the maximum number of electrons that can fill an atom's outer shell
	 * (2, 8, 12, 18, or 32).
	 * @param	atno	atomic number of an atom
	 * @return	the number of electrons required to fill the outer shell
	 */
	public static int getMaxOuterElectrons(int atno) {
		final int row = PeriodicSystem.getRow(atno);
		final int column = PeriodicSystem.getColumn(atno);
		return (isMulticenterAtom(atno) || row == 0 ? 0 // unknown element
				: row == 1 ? 2
				: row == 2 ? 8
				: column >= 17 ? 14 // hypervalent halogen
				: row == 3 || column >= 13 ? 12 // hypervalent Si, S, etc.
				: Utils.among(row, 4, 5) || column >= 4 ? 18 // transition metals
				: 32); // lanthanides and actinides
	} // getMaxOuterElectrons(int)

	/** Get the number of electrons needed to fill an atom's outer shell
	 * (2, 8, 18, or 32).
	 * @param	atom	an atom
	 * @return	the number of electrons required to fill the outer shell
	 */
	public static int getNeededOuterElectrons(MolAtom atom) {
		return getNeededOuterElectrons(atom.getAtno());
	} // getNeededOuterElectrons(MolAtom)

	/** Get the number of electrons needed to fill an atom's outer shell
	 * (2, 8, 18, or 32).
	 * @param	atno	atomic number of an atom
	 * @return	the number of electrons required to fill the outer shell
	 */
	public static int getNeededOuterElectrons(int atno) {
		final int row = PeriodicSystem.getRow(atno);
		final int column = PeriodicSystem.getColumn(atno);
		return (isMulticenterAtom(atno) || row == 0 ? 0 // unknown element
				: row == 1 ? 2
				: Utils.among(row, 2, 3) ? 8
				: Utils.among(row, 4, 5) || column >= 4 ? 18
				: 32);
	} // getNeededOuterElectrons(int)

	/** Gets the sum of the bond orders of an atom in a molecule, using bond
	 * orders of 1 for coordinate bonds and 0 for query bonds, and returning 0 
	 * if the atom is a multicenter attachment point.
	 * @param	atom	an atom
	 * @return	the sum of the bond orders of this atom, as an int
	 */
	public static int getSumBondOrders(MolAtom atom) {
		if (isMulticenterAtom(atom)) return 0;
		int twiceSumBondOrders = 0;
		for (final MolBond bond : atom.getBondArray()) {
			final int type = bond.getType();
			// aromatic bond has order 1.5, query bond has order 0
			twiceSumBondOrders += (type <= 3 ? type * 2
					: type == MolBond.COORDINATE ? 2
					: type == MolBond.AROMATIC ? 3 : 0);
		} // for each bond of this atom
		return twiceSumBondOrders / 2;
	} // getSumBondOrders(MolAtom)

	/** Gets whether an atom is a metal.
	 * @param	atom	the atom
	 * @return	true if the atom is a metal
	 */
	public static boolean isMetal(MolAtom atom) {
		return isMetal(atom.getAtno());
	} // isMetal(MolAtom)

	/** Gets whether an element is a metal.
	 * @param	atNo	atomic number of the element
	 * @return	true if the element is a metal
	 */
	public static boolean isMetal(int atNo) {
		return PeriodicSystem.isMetal(atNo);
	} // isMetal(int)

	/** Gets whether an atom is a metalloid.
	 * @param	atom	the atom
	 * @return	true if the atom is a metalloid
	 */
	public static boolean isMetalloid(MolAtom atom) {
		return isMetalloid(atom.getAtno());
	} // isMetalloid(MolAtom)

	/** Gets whether an element is a metalloid.
	 * @param	atNo	atomic number of the element
	 * @return	true if the element is a metalloid
	 */
	public static boolean isMetalloid(int atNo) {
		return PeriodicSystem.isMetalloid(atNo);
	} // isMetalloid(int)

	/** Gets whether an atom is a transition metal.
	 * @param	atom	the atom
	 * @return	true if the atom is a transition metal
	 */
	public static boolean isTransitionMetal(MolAtom atom) {
		return isTransitionMetal(atom.getAtno());
	} // isTransitionMetal(MolAtom)

	/** Gets whether an element is a transition metal.
	 * @param	atNo	atomic number of the element
	 * @return	true if the element is a transition metal
	 */
	public static boolean isTransitionMetal(int atNo) {
		return PeriodicSystem.isTransitionMetal(atNo);
	} // isTransitionMetal(int)

	/** Gets whether an atom is an alkali metal.
	 * @param	atom	the atom
	 * @return	true if the atom is an alkali metal
	 */
	public static boolean isAlkaliMetal(MolAtom atom) {
		return isAlkaliMetal(atom.getAtno());
	} // isAlkaliMetal(MolAtom)

	/** Gets whether an element is an alkali metal.
	 * @param	atNo	atomic number of the element
	 * @return	true if the element is an alkali metal
	 */
	public static boolean isAlkaliMetal(int atNo) {
		return PeriodicSystem.isAlkaliMetal(atNo);
	} // isAlkaliMetal(int)

	/** Gets whether an atom is an alkaline earth metal.
	 * @param	atom	the atom
	 * @return	true if the atom is an alkaline earth metal
	 */
	public static boolean isAlkalineEarthMetal(MolAtom atom) {
		return isAlkalineEarthMetal(atom.getAtno());
	} // isAlkalineEarthMetal(MolAtom)

	/** Gets whether an element is an alkaline earth metal.
	 * @param	atNo	atomic number of the element
	 * @return	true if the element is an alkaline earth metal
	 */
	public static boolean isAlkalineEarthMetal(int atNo) {
		return PeriodicSystem.isAlkalineEarthMetal(atNo);
	} // isAlkalineEarthMetal(int)

	/** Gets whether an atom is a main group metal.
	 * @param	atom	the atom
	 * @return	true if the atom is a main group metal
	 */
	public static boolean isMainGroupMetal(MolAtom atom) {
		return isMainGroupMetal(atom.getAtno());
	} // isMainGroupMetal(MolAtom)

	/** Gets whether an element is a main group metal.
	 * @param	atNo	atomic number of the element
	 * @return	true if the element is a main group metal
	 */
	public static boolean isMainGroupMetal(int atNo) {
		return PeriodicSystem.isOtherMetal(atNo);
	} // isMainGroupMetal(int)

	/** Gets whether an atom is an alkali or alkaline earth metal.
	 * @param	atom	an atom
	 * @return	true if the atom is an alkali or alkaline earth metal
	 */
	public static boolean isCol1Or2Metal(MolAtom atom) {
		return isAlkaliMetal(atom) || isAlkalineEarthMetal(atom);
	} // isCol1Or2Metal(MolAtom)

	/** Gets whether an atom is a metal but not a transition metal.
	 * @param	atom	an atom
	 * @return	true if the atom is a metal but not a transition metal
	 */
	public static boolean isNontransitionMetal(MolAtom atom) {
		return isMetal(atom) && !isTransitionMetal(atom);
	} // isNontransitionMetal(MolAtom)

	/** Gets whether an atom is neither metal nor metalloid.
	 * @param	atom	an atom
	 * @return	true if the atom is neither metal nor metalloid
	 */
	public static boolean isNonmetal(MolAtom atom) {
		return !isMetal(atom) && !isMetalloid(atom);
	} // isNonmetal(MolAtom)

	/** Gets the number of unshared electrons of an atom.
	 * @param	atom	the atom
	 * @return	the number of lone pairs
	 */
	public static int getUnsharedElectronCount(MolAtom atom) {
		final String SELF = "ChemUtils.getUnsharedElectronCount: ";
		final int valenceElecCt = getValenceElectrons(atom);
		final int charge = atom.getCharge();
		final int numExplicitBonds = 
				atom.twicesumbonds(INCLUDE_EXPLICIT_H, USE_BOND_ORDERS) / 2;
		final int numImplicitHBonds = atom.getImplicitHcount();
		int unsharedElecCt = valenceElecCt - charge 
				- (numExplicitBonds + numImplicitHBonds);
		if (unsharedElecCt < 0) unsharedElecCt = 0;
		debugPrint(SELF + "for atom ", atom, " with charge ",
				charge > 0 ? "+" : "", charge, 
				", valenceElecCt = ", valenceElecCt,
				", numExplicitBonds = ", numExplicitBonds, 
				", numImplicitHBonds = ", numImplicitHBonds,
				", unsharedElecCt = ", unsharedElecCt);
		return unsharedElecCt;
	} // getUnsharedElectronCount(MolAtom)

	/** Checks for valence and coordination-bond errors.
	 * @param	origMol	the molecule
	 * @throws	ValenceException	if there is a valence or coordination-bond
	 * error
	 */
	public static void checkValence(Molecule origMol) throws ValenceException {
		final String SELF = "ChemUtils.checkValence: ";
		final ValenceCheckOptions.Builder valOptsBld = 
				new ValenceCheckOptions.Builder();
		valOptsBld.setTraditionalNitrogenAllowed(false);
		origMol.setValenceCheckOptions(valOptsBld.build());
		origMol.valenceCheck();
		final Molecule mol = origMol.clone();
		boolean throwError = false;
		debugPrintMRV(SELF + "checking for coordinate bonds starting at "
				+ "shortcut groups in:\n", mol);
		ValenceException exception = new ValenceException("You have drawn a "
				+ "coordinate bond that originates at a shortcut group.  A "
				+ "coordinate bond must originate at an atom. ");
		for (final Sgroup sgroupRaw : mol.getSgroupArray()) {
			if (sgroupRaw instanceof SuperatomSgroup) {
				final SuperatomSgroup sgroup = (SuperatomSgroup) sgroupRaw;
				final SgroupAtom superatom = sgroup.getSuperAtom();
				final MolAtom attachPt = sgroup.findAttachAtom();
				if (attachPt != null) {
					debugPrint(SELF + "found shortcut group ", superatom,
							" with attachment point ", attachPt,
							mol.indexOf(attachPt) + 1, " with ", 
							attachPt.getBondCount(), 
							" bond(s); checking for coordinate bond.");
					for (final MolBond bond : attachPt.getBondArray()) {
						if (bond.getType() == MolBond.COORDINATE) {
							debugPrint(SELF + "Coordinate bond originates "
									+ "at attachment point of shortcut group ", 
									superatom, mol.indexOf(attachPt) + 1);
							final MolAtom atom1 = bond.getAtom1();
							final MolAtom atom2 = bond.getAtom2();
							// pointer equality:
							final int[] badAtomNums = ArrayUtils.addAll(
									exception.getBadAtomNums(), 
									new int[] {mol.indexOf(superatom) + 1,
										mol.indexOf(atom1 == attachPt
											|| atom1 == (MolAtom) superatom
										? atom2 : atom1) + 1});
							exception.setBadAtomNums(badAtomNums);
							throwError = true;
						} // if bond is coordinate
					} // for each bond to the superatom attachment point
				} // if there's an attachment point
			} // if shortcut group is a superatom
		} // for each shortcut group
		if (throwError) {
			debugPrintMRV(SELF + "Coordinate bond error(s) in:\n", mol);
			throw exception;
		} // if should throw an error
		debugPrint(SELF + "no coordinate bonds starting at shortcut group; "
				+ "checking for valence errors.");
		mol.ungroupSgroups(SHORTCUT_GROUPS);
		debugPrintMRV(SELF + "after ungroupSgroups():\n", mol);
		final String VALENCE_ERROR = "ACE has highlighted one or more atoms "
				+ "in your response that have an invalid valence. If ACE has "
				+ "highlighted a shortcut group, you may need to expand the "
				+ "group to see the atoms with the invalid valences. ";
		exception = new ValenceException(VALENCE_ERROR);
		final List<Integer> badValenceNums = new ArrayList<Integer>();
		boolean throwErrorGlobal = false;
		for (int atomNum = 0; atomNum < mol.getAtomCount(); atomNum++) {
			final MolAtom atom = mol.getAtom(atomNum);
			final int atomicNum = atom.getAtno();
			throwError = !isMulticenterAtom(atomicNum)
					&& !PeriodicSystem.isTransitionMetal(atomicNum)
					&& !PeriodicSystem.isLanthanideMetal(atomicNum)
					&& !PeriodicSystem.isActinideMetal(atomicNum)
					&& atom.hasValenceError(); 
			final int maxOuter = getMaxOuterElectrons(atom);
			if (throwError && maxOuter > 8) {
				final int totalElecCt = atom.getImplicitHcount() * 2
						+ atom.twicesumbonds(INCLUDE_EXPLICIT_H, 
							USE_BOND_ORDERS)
						+ getUnsharedElectronCount(atom);
				final int radState = atom.getRadicalValue().getIntValue();
				throwError = totalElecCt > maxOuter 
						|| (totalElecCt >= maxOuter - 1 
							&& Utils.among(radState, 
								Radical.DIVALENT.getIntValue(),
								Radical.DIVALENT_SINGLET.getIntValue(),
								Radical.DIVALENT_TRIPLET.getIntValue()));
				if (!throwError) {
					debugPrint(SELF + "for ", atom, atomNum + 1, 
							", maxOuter = ", maxOuter, ", totalElecCt = ", 
							totalElecCt, ", not throwing error for element "
							+ "allowed to be hypervalent.");
				} // if not throwing error
			} // if could be hypervalent halogen, Si, S, etc.
			if (throwError) {
				debugPrint(SELF + "Valence error at ", atom, atomNum + 1, 
						" with charge ", atom.getCharge(), " in ", mol);
				badValenceNums.add(Integer.valueOf(atomNum + 1));
				continue;
			} // if should throw error
			// workaround for certain hypervalent N atoms not caught by
			// ChemAxon's valence check
			if (atom.getAtno() == 7) { // nitrogen
				final int bondElecCt = atom.getRadicalCount() +
						2 * (getSumBondOrders(atom) + atom.getImplicitHcount());
				if (bondElecCt > 8
						|| (bondElecCt == 8 && atom.getCharge() != 1)) { 
					debugPrint(SELF, atom, atomNum + 1, " with charge ", 
							atom.getCharge(), " is hypervalent or has "
							+ "valence error in ", mol);
					badValenceNums.add(Integer.valueOf(atomNum + 1));
				} // if N has valence error or is hypervalent
			} // if atom is N /**/
		} // for each atom
		if (!badValenceNums.isEmpty()) {
			exception.setBadAtomNums(Utils.listToIntArray(badValenceNums));
			throwErrorGlobal = true;
		} // if there are atoms with bad valences
		if (throwErrorGlobal) {
			debugPrintMRV(SELF + "Valence error(s) in:\n", mol);
			throw exception;
		} // if should throw error
		debugPrint(SELF + "no valence errors; checking for coordinate bonds "
				+ "starting at atoms with no lone pairs.");
		final List<MolBond> coordBondsStartingAtAtoms = 
				new ArrayList<MolBond>();
		exception = new ValenceException("You have drawn a coordinate bond "
				+ "that originates at an atom with fewer than two unshared "
				+ "electrons.  A coordinate bond must originate at an atom "
				+ "with at least two unshared electrons.  You can reverse "
				+ "the direction of a coordinate bond by pressing the "
				+ "coordinate bond toolbar button, then clicking on the "
				+ "coordinate bond.");
		throwError = false;
		for (final MolBond bond : mol.getBondArray()) {
			if (bond.getType() == MolBond.COORDINATE) {
				final MolAtom originAtom = bond.getAtom1();
				final int originAtomNum = mol.indexOf(originAtom) + 1;
				debugPrint(SELF + "Coordinate bond originating at atom ", 
						originAtom, originAtomNum); 
				if (!isMulticenterAtom(originAtom)) {
					coordBondsStartingAtAtoms.add(bond);
					final int numUnsharedElectrons = 
							getUnsharedElectronCount(originAtom);
					if (numUnsharedElectrons < 2) { 
						debugPrintMRV(SELF + "Coordinate bond originates "
								+ "at ", originAtom, originAtomNum,
								" with only ", numUnsharedElectrons, 
								" unshared electron(s) in:\n", mol);
						final int[] badAtomNums = ArrayUtils.addAll(
								exception.getBadAtomNums(), 
								new int[] {originAtomNum,
									mol.indexOf(bond.getAtom2()) + 1});
						exception.setBadAtomNums(badAtomNums);
						throwError = true;
					} // if coordinate bond direction error
				} else debugPrint(SELF + "Coordinate bond originates at "
						+ "multicenter attachment point; allowed.");
			} // if bond is coordinate
		} // for each bond 
		if (throwError) {
			debugPrintMRV(SELF + "Coordinate bond(s) starting at 0-electron "
					+ "atom(s) in:\n", mol);
			throw exception;
		} // if should throw an error
		debugPrint(SELF + "no coordinate bonds starting at atoms with no "
				+ "lone pairs; checking for coordinate bond pointing to "
				+ "multicenter attachment point.");
		exception = new ValenceException("You have drawn a coordinate bond "
				+ "that points to a multiatom group.  A coordinate bond should "
				+ "originate at such a group.  You can reverse the direction "
				+ "of a coordinate bond by pressing the coordinate bond "
				+ "toolbar button, then clicking on the coordinate bond.");
		throwError = false;
		for (final MolBond bond : coordBondsStartingAtAtoms) {
			final MolAtom targetAtom = bond.getAtom2();
			if (isMulticenterAtom(targetAtom)) {
				final int targetAtomNum = mol.indexOf(targetAtom) + 1;
				debugPrintMRV(SELF + "Coordinate bond points to "
						+ "multicenter attachment point ", targetAtom, 
						targetAtomNum, " in:\n", mol);
				final int[] badAtomNums = ArrayUtils.addAll(
						exception.getBadAtomNums(), 
						new int[] {mol.indexOf(bond.getAtom1()) + 1,
							targetAtomNum});
				exception.setBadAtomNums(badAtomNums);
				throwError = true;
			} // if coordinate bond direction error
		} // for each coordinate bond starting at an atom
		if (throwError) {
			debugPrintMRV(SELF + "Coordinate bond(s) pointing from atom(s) "
					+ "to multicenter attachment point(s) in:\n", mol);
			throw exception;
		} // if should throw an error
	} // checkValence(Molecule)

	/** Gets if a MolAtom represents the center of a multiatom group, not a real
	 * atom.
	 * @param	atom	the atom
	 * @return	true if the MolAtom represents the center of a multiatom group
	 */
	public static boolean isMulticenterAtom(MolAtom atom) {
		return isMulticenterAtom(atom.getAtno());
	} // isMulticenterAtom(MolAtom)

	/** Gets if a MolAtom represents the center of a multiatom group, not a real
	 * atom.
	 * @param	atno	the atomic number of the atom
	 * @return	true if the MolAtom represents the center of a multiatom group
	 */
	public static boolean isMulticenterAtom(int atno) {
		return atno == MolAtom.MULTICENTER;
	} // isMulticenterAtom(int)

	/** Gets the multicenter attachment points of the molecule. 
	 * @param	mol	the molecule
	 * @return	list of the "atoms" that are multicenter attachment points
	 */
	public static List<MolAtom> getMulticenterAtoms(Molecule mol) {
		final List<MolAtom> multicenterAttachPts = new ArrayList<MolAtom>();
		for (final Sgroup sgroup : mol.getSgroupArray()) {
			if (sgroup.getSgroupType().equals(SgroupType.MULTICENTER)) {
				final MolAtom multicenterAttachPt = 
						((MulticenterSgroup) sgroup).getCentralAtom();
				multicenterAttachPts.add(multicenterAttachPt);
			} // if is multicenter group
		} // for each Sgroup
		return multicenterAttachPts;
	} // getMulticenterAtoms(Molecule)

	/** Gets the multiatom groups of the molecule. 
	 * @param	mol	the molecule
	 * @return	list of the multiatom groups
	 */
	public static List<Sgroup> getMultiatomGroups(Molecule mol) {
		final List<Sgroup> multiatomGroups = new ArrayList<Sgroup>();
		for (final MolAtom atom : mol.getAtomArray()) {
			if (isMulticenterAtom(atom)) {
				multiatomGroups.add(mol.findContainingMulticenterSgroup(atom));
			} // if atom is multicenter attachment point
		} // for each atom
		return multiatomGroups;
	} // getMultiatomGroups(Molecule)

	/** Disables external instantiation. */
	private ChemUtils() { }

} // ChemUtils
