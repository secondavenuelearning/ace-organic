package com.epoch.synthesis;

import static com.epoch.synthesis.synthConstants.DielsAlderConstants.*;
import chemaxon.formats.MolFormatException;
import chemaxon.marvin.calculations.ChargePlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.sss.SearchConstants;
import chemaxon.sss.search.MolSearch;
import chemaxon.sss.search.MolSearchOptions;
import chemaxon.sss.search.SearchException;
import chemaxon.struc.DPoint3;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import chemaxon.struc.Molecule;
import chemaxon.struc.StereoConstants;
import com.epoch.chem.MolCompare;
import com.epoch.chem.MolCompareException;
import com.epoch.chem.StereoFunctions;
import com.epoch.chem.VectorMath;
import com.epoch.utils.MathUtils;
import com.epoch.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Sets the stereochemistry of the product of a Diels-Alder reaction. */
class DielsAlderStereo implements SearchConstants, StereoConstants {

	private void debugPrint(Object... msg) { 
		// Utils.printToLog(msg, SMARTS); 
	}

	private void debugPrintMRV(Object... msg) { 
		// Utils.printToLog(msg, MRV); 
	}

/* **************** Members ***********************/

	/** The calculated Diels-Alder product.  Atoms derived from the
	 * diene are mapped 1-4; those from the dienophile are mapped 5-6;
	 * atom 1 is attached to 6, and 4 to 5; the out ligands of the diene 
	 * termini are mapped 7 and 9; and the dienophile is mapped 
	 * (11)(12)5=6(13)(14), with 11 cis to 13, and 12 cis to 14. */
	transient final private Molecule d_a_Product;
	/** Starting materials that produced the Diels-Alder product. */
	transient final private Molecule[] d_a_SMs;

	/**	Array of atoms in the product, keyed by their map numbers. */
	transient private MolAtom[] prodAtomsByMaps = new MolAtom[NUM_MAPS];
	/** Whether the reaction gives primarily the endo product. */
	transient final private boolean isEndo; // set in constructor
	/** Whether the reaction uses a furan. */
	transient final private boolean usesFuran; // set in constructor
	/** Whether the diene is cyclic. */
	transient private boolean dieneIsCyclic = false;
	/** Position of the dienophile in the array of starting materials. */
	transient private int philePosn = 0;

/* **************** Constructor and parent method ***********************/

	/** Constructor. 
	 * @param	product	the calculated product of the Diels-Alder reaction
	 * @param	starters	the starting materials
	 * @param	rxnName	name of reaction
	 */
	DielsAlderStereo(Molecule product, Molecule[] starters, String rxnName) {
		d_a_Product = product;
		d_a_SMs = new Molecule[starters.length];
		int smNum = 0;
		for (final Molecule sm : starters) d_a_SMs[smNum++] = sm.clone();
		isEndo = rxnName.indexOf("endo") >= 0;
		usesFuran = rxnName.indexOf("furan") >= 0;
		debugPrintMRV("DielsAlderStereo: rxnName = ", rxnName, 
				", Diels-Alder product is:\n", d_a_Product);
	} // DielsAlderStereo(Molecule, Molecule[], String)
	
	/** Adds the stereochemistry to the product of a Diels-Alder reaction.
	 */
	void setDielsAlderStereo() {
		final String SELF = "DielsAlderStereo.setDielsAlderStereo: ";
		for (final MolAtom atom : d_a_Product.getAtomArray()) {
			final int mapNum = atom.getAtomMap();
			if (MathUtils.inRange(mapNum, MAPS_RANGE)) {
				prodAtomsByMaps[mapNum - 1] = atom;
			} // if map number is in range
		} // for each atom
		final List<MolAtom> stereocenters = new ArrayList<MolAtom>();
		for (final int mapNum : TERMINUS_MAPS) {
			final MolAtom possibleStereocenter = prodAtomsByMaps[mapNum - 1];
			final int psNum = d_a_Product.indexOf(possibleStereocenter);
			final int parity = d_a_Product.getParity(psNum);
			if (parity != 0) stereocenters.add(possibleStereocenter);
		} // for each terminal atom
		if (stereocenters.size() >= 2) try {
			boolean dieneTerminusBecomesStereocenter = false;
			 if (isDieneCyclic()) {
				debugPrint(SELF + "diene ", d_a_SMs[0], " is cyclic.");
				makeConvex();
				moveBicycDielsAlderAtoms();
			} // if diene is cyclic
			boolean phileIsMapped = false;
			for (final MolAtom stereocenter : stereocenters) {
				final int stereocenterMap = stereocenter.getAtomMap();
				if (stereocenterMap < TERMINUS_MAPS[PHILE_TERMINUS1_POSN]) {
					setDieneTermini(stereocenterMap);
					dieneTerminusBecomesStereocenter = true;
				} else {
					if (!phileIsMapped) {
						mapDienophile();
						phileIsMapped = true;
					} // if dienophile has not been mapped
					setPhileTermini(d_a_SMs[philePosn], stereocenterMap,
							dieneTerminusBecomesStereocenter);
				} // if diene terminus becomes stereocenter
			} // for each stereocenter in the product
			debugPrintMRV(SELF + "after setting stereobonds, "
					+ "d_a_Product:\n", d_a_Product);
		} catch (NullPointerException e) {
			Utils.alwaysPrint(SELF + "caught NullPointerException trying "
					+ "to assign stereochemistry to ", d_a_Product,
					"probably because dienophile atoms could not be "
					+ "mapped; returning without further change.");
			e.printStackTrace();
		} // try
	} // setDielsAlderStereo()

/* **************** Utility methods ***********************/

	/** Moves the atoms of the new ring in a bicyclic Diels-Alder adduct (and
	 * the nonring atoms attached to them) so that the ring is convex at every 
	 * vertex.
	 */
	private void makeConvex() {
		final String SELF = "DielsAlderStereo.makeConvex: ";
		final DPoint3[] ringAtomLocns = getRingAtomLocns();
		for (int mapNum = 1; mapNum <= NUM_RING_ATOMS; mapNum++) {
			final MolAtom atomA = prodAtomsByMaps[mapNum - 1];
			final MolAtom atomB = prodAtomsByMaps[mapNum % NUM_RING_ATOMS];
			final MolAtom atomC =
					prodAtomsByMaps[(mapNum + 1) % NUM_RING_ATOMS];
			final DPoint3 locnA = atomA.getLocation();
			final DPoint3 locnC = atomC.getLocation();
			final DPoint3 midpointAC = VectorMath.midpoint(locnA, locnC);
			if (!VectorMath.pointInPolygon(ringAtomLocns, midpointAC)) {
				// ring atom B makes concave vertex; calculate concavity angle
				final DPoint3 locnB = atomB.getLocation();
				final DPoint3 vectorAB = VectorMath.diff(locnB, locnA);
				final DPoint3 vectorAC = VectorMath.diff(locnC, locnA);
				final double angleBAC = VectorMath.angle(vectorAC, vectorAB)
						* VectorMath.angleSign(new DPoint3[]
							{locnC, locnB, locnA});
				final double rotateAB = 2 * angleBAC;
				debugPrint(SELF + "polygon is concave at ring atom mapped ", 
						atomB.getAtomMap(), "; angle made by atoms mapped [", 
						atomB.getAtomMap(), ", ", atomA.getAtomMap(), ", ", 
						atomC.getAtomMap(), "] is ", toDegrees(angleBAC), 
						" deg so rotate it by ", toDegrees(rotateAB), " deg.");
				final DPoint3 newVectorAB = 
						VectorMath.rotateVector(vectorAB, rotateAB);
				final DPoint3 newLocnB = VectorMath.sum(locnA, newVectorAB);
				final DPoint3 moveVector = VectorMath.diff(newLocnB, locnB);
				atomB.setLocation(newLocnB);
				// move nonring atoms attached to atom B by same amount
				for (final MolAtom atomBLig : atomB.getLigands()) {
					final int ligMap = atomBLig.getAtomMap();
					if (!MathUtils.inRange(ligMap, RING_MAPS_RANGE)) {
						atomBLig.setLocation(VectorMath.sum(
								atomBLig.getLocation(), moveVector));
					} // if atom is not in ring
				} // for each ligand of atomB
			} // if atom B is a concave vertex
		} // for each atom in the new ring
		debugPrintMRV(SELF + "after moving ring atoms to make polygon "
				+ "convex:\n", d_a_Product);
	} // makeConvex()

	/** Moves the substituents of the new ring in a bicyclic Diels-Alder 
	 * adduct so that stereochemistry can be assigned properly.
	 */
	private void moveBicycDielsAlderAtoms() {
		final String SELF = "DielsAlderStereo.moveBicycDielsAlderAtoms: ";
		final DPoint3[] ringAtomLocns = getRingAtomLocns();
		for (int terminusNum = DIENE_TERMINUS1_POSN; 
				terminusNum < DIENE_TERMINUS1_POSN + 2; terminusNum++) {
			final int dieneTerminusMap = TERMINUS_MAPS[terminusNum];
			final MolAtom dieneTerminus = 
					prodAtomsByMaps[dieneTerminusMap - 1];
			final int outAtomMap = getOutAtomMap(dieneTerminusMap);
			final MolAtom outAtom = prodAtomsByMaps[outAtomMap - 1];
			final MolAtom[] terminusLigands = dieneTerminus.getLigands();
			MolAtom inAtom = null;
			for (final MolAtom terminusLigand : terminusLigands) {
				final int ligMap = terminusLigand.getAtomMap();
				if (Utils.among(ligMap, 0, IN_ATOM_MAP)) {
					inAtom = terminusLigand;
					break;
				} // if the atom is unmapped
			} // for each ligand of the terminus
			if (inAtom == null) {
				debugPrint(SELF + "inAtom is null!");
				return;
			}
			final DPoint3 outAtomLocn = outAtom.getLocation();
			final DPoint3 inAtomLocn = inAtom.getLocation();
			if (VectorMath.pointInPolygon(ringAtomLocns, outAtomLocn)) {
				debugPrint(SELF + "out atom ", outAtom.getSymbol(),
						d_a_Product.indexOf(outAtom) + 1, " with map ",
						outAtomMap, " on terminus with map ", 
						dieneTerminusMap, " is physically inside "
						+ "the D-A product ring; need to move it out.");
				final DPoint3 terminusLocn = dieneTerminus.getLocation();
				final DPoint3 outToTerminus = 
						VectorMath.diff(outAtomLocn, terminusLocn);
				final DPoint3 opposite = 
						VectorMath.scalarProd(outToTerminus, -1);
				final DPoint3 newOutLocn = 
						VectorMath.sum(terminusLocn, opposite);
				outAtom.setLocation(newOutLocn);
			} // if out atom is inside the ring
			if (!VectorMath.pointInPolygon(ringAtomLocns, inAtomLocn)) {
				debugPrint(SELF + "in atom ", inAtom.getSymbol(),
						d_a_Product.indexOf(inAtom) + 1, 
						" on terminus with map ", dieneTerminusMap, 
						" is physically outside the D-A product ring; "
						+ "need to move it in.");
				final DPoint3 terminusLocn = dieneTerminus.getLocation();
				final int otherTerminusMap = TERMINUS_MAPS[
						2 * DIENE_TERMINUS1_POSN - terminusNum + 1];
				final MolAtom otherTerminus = 
						prodAtomsByMaps[otherTerminusMap - 1];
				final DPoint3 otherTerminusLocn = otherTerminus.getLocation();
				final DPoint3 terminiAverage = VectorMath.scalarQuot(
						VectorMath.sum(terminusLocn, otherTerminusLocn), 2);
				inAtom.setLocation(terminiAverage);
			} // if in atom is outside the ring
		} // for each terminus
		debugPrintMRV(SELF + "after moving atoms, d_a_Product:\n", 
				d_a_Product);
	} // moveBicycDielsAlderAtoms()

	/** Determines whether the diene is cyclic, and sets the dieneIsCyclic
	 * global variable.
	 * @return	true if the diene is cyclic
	 */
	private boolean isDieneCyclic() {
		final String SELF = "DielsAlderStereo.dieneIsCyclic: ";
		try {
			dieneIsCyclic = usesFuran || MolCompare.dieneIsCyclic(d_a_SMs[0]);
		} catch (MolCompareException e) {
			Utils.alwaysPrint(SELF + "caught MolCompareException when trying "
					+ "to determine whether ", d_a_SMs[0], " is cyclic.");
		} catch (MolFormatException e) {
			Utils.alwaysPrint(SELF + "caught MolFormatException when trying "
					+ "to determine whether ", d_a_SMs[0], " is cyclic.");
		} // try
		return dieneIsCyclic;
	} // isDieneCyclic()

	/** Maps the atoms of the dienophile.  */
	private void mapDienophile() {
		final String SELF = "DielsAlderStereo.mapDienophile: ";
		philePosn = d_a_SMs.length - 1;
		try {
			final Molecule retroProd = getMappedRetroDAProduct();
			final Molecule[] retroFrags = retroProd.convertToFrags();
			debugPrintMRV(SELF + "retro-D-A fragments:\n", retroFrags);
			// match the calculated dienophile to the actual one
			final MolSearchOptions searchOpts = getSearchOptions();
			for (final Molecule retroFrag : retroFrags) {
				final MolSearch search = new MolSearch();
				search.setSearchOptions(searchOpts);
				search.setTarget(d_a_SMs[philePosn]);
				search.setQuery(retroFrag);
				final int[] matchIndices = search.findNext();
				if (!Utils.isEmpty(matchIndices)) {
					debugPrint(SELF + "found match between retroFrag ",
							retroFrag, " and actual dienophile ",
							d_a_SMs[philePosn]);
					transferMaps(retroFrag, matchIndices);
					break;
				} // if there's a match
			} // for each fragment of the D-A reaction
		} catch (SearchException e) {
			Utils.alwaysPrint(SELF + "caught SearchException while trying "
					+ "to set stereochemistry of Diels-Alder adduct:\n", 
					e.getMessage());
			e.printStackTrace();
		} // try
		debugPrintMRV(SELF + "diene and mapped dienophile:\n", d_a_SMs);
	} // mapDienophile()

	/** Runs a retro-Diels-Alder reaction on the product from the Diels-Alder
	 * reaction.
	 * @return	the starting materials from running the Diels-Alder reaction
	 * backwards
	 */
	private Molecule getMappedRetroDAProduct() {
		final Molecule retroProd = d_a_Product.clone();
		final List<MolAtom> reactedTermini = 
				getRetroProdAtoms(retroProd, TERMINUS_MAPS);
		// disconnect the bonds made by the D-A reaction
		for (final int[] madeBondAtomMaps : MADE_BOND_ATOM_MAPS) {
			final MolAtom bondMaker1 = reactedTermini.get(madeBondAtomMaps[0]);
			final MolAtom bondMaker2 = reactedTermini.get(madeBondAtomMaps[1]);
			retroProd.removeBond(bondMaker1.getBondTo(bondMaker2));
		} // for each bond made by the D-A reaction
		// reset the dienophile bond order
		final MolBond copyPhileBond = 
				reactedTermini.get(PHILE_TERMINUS1_POSN).getBondTo(
					reactedTermini.get(PHILE_TERMINUS1_POSN + 1));
		copyPhileBond.setType(copyPhileBond.getType() + 1);
		if (d_a_SMs.length == 1) { 
			// intramolecular, so match between retro dienophile and 
			// actual dienophile requires diene to be retroed as well
			final List<MolAtom> retroDieneAtoms = 
					getRetroProdAtoms(retroProd, DIENE_MAPS);
			// change the diene bond orders
			for (int dieneAtomNum = 1; 
					dieneAtomNum < retroDieneAtoms.size(); 
					dieneAtomNum++) {
				final MolAtom thisDieneAtom = 
						retroDieneAtoms.get(dieneAtomNum - 1);
				final MolAtom nextDieneAtom = retroDieneAtoms.get(dieneAtomNum);
				final MolBond dieneBond =
						thisDieneAtom.getBondTo(nextDieneAtom);
				dieneBond.setType(dieneBond.getType()
						+ (dieneAtomNum % 2 == 0 ? -1 : 1));
			} // for each bond in diene
			if (usesFuran) retroProd.aromatize();
		} // if reaction is intramolecular
		return retroProd;
	} // getMappedRetroDAProduct()

	/** Gets the atoms in the copy of the Diels-Alder product that correspond to
	 * mapped atoms in the original Diels-Alder product.
	 * @param	retroProd	the retro-Diels-Alder product
	 * @param	maps	the maps of the atoms to get
	 * @return	a list of atoms in the copy of the Diels-Alder product that
	 * correspond to mapped atoms in the original Diels-Alder product
	 */
	final List<MolAtom> getRetroProdAtoms(Molecule retroProd, int[] maps) {
		final List<MolAtom> retroProdAtoms = new ArrayList<MolAtom>();
		for (final int map : maps) {
			final MolAtom origAtom = prodAtomsByMaps[map - 1];
			final int atomIndex = d_a_Product.indexOf(origAtom);
			retroProdAtoms.add(retroProd.getAtom(atomIndex));
		} // for each atom map
		return retroProdAtoms;
	} // getRetroProdAtoms(Molecule, int[])

	/** Gets the search options for mapping the starting materials.
	 * @return	search options
	 */
	private MolSearchOptions getSearchOptions() {
		final MolSearchOptions searchOpts = new MolSearchOptions(FULL);
		searchOpts.setStereoModel(STEREO_MODEL_GLOBAL);
		searchOpts.setChargeMatching(CHARGE_MATCHING_EXACT);
		searchOpts.setIsotopeMatching(ISOTOPE_MATCHING_EXACT);
		searchOpts.setRadicalMatching(RADICAL_MATCHING_EXACT);
		searchOpts.setVagueBondLevel(VAGUE_BOND_OFF);
		searchOpts.setStereoSearchType(STEREO_SPECIFIC);
		searchOpts.setDoubleBondStereoMatchingMode(DBS_NONE);
		searchOpts.setExactBondMatching(true);
		searchOpts.setOrderSensitiveSearch(true); // all permutations needed
		searchOpts.setIgnoreCumuleneOrRingCisTransStereo(false); // odd-numbered cumulenes
		searchOpts.setIgnoreAxialStereo(false); // allenes & biaryls
		searchOpts.setIgnoreSynAntiStereo(true);
		return searchOpts;
	} // getSearchOptions()

	/** Transfers the map numbers from the dienophile generated by the 
	 * retro-Diels-Alder reaction to the starting dienophile.
	 * @param	retroPhile	the retro-Diels-Alder dienophile
	 * @param	matchIndices	indices of match between the retro-Diels-Alder
	 * dienophile and the original dienophile
	 */
	private void transferMaps(Molecule retroPhile, int[] matchIndices) {
		int qIndex = 0;
		for (final int tIndex : matchIndices) {
			if (tIndex >= 0) {
				final MolAtom qAtom = retroPhile.getAtom(qIndex);
				final int qMap = qAtom.getAtomMap();
				final MolAtom origPhileAtom = d_a_SMs[philePosn].getAtom(tIndex);
				origPhileAtom.setAtomMap(qMap);
			} // if a target atom matches a query atom
			qIndex++;
		} // for each target index
	} // transferMaps(Molecule, int[])

	/** Sets the stereochemistry of the diene out groups in the product to be
	 * up, unless the diene is cyclic, in which case the in groups are set as
	 * up.
	 * @param	terminusMap	map atom of the terminus being handled
	 */
	private void setDieneTermini(int terminusMap) {
		final String SELF = "DielsAlderStereo.setDieneTermini: ";
		int outMapNum = getOutAtomMap(terminusMap);
		if (dieneIsCyclic) { // in atom needs to point up
			// place in atom in an unused position in atoms-by-maps array
			final MolAtom terminus = prodAtomsByMaps[terminusMap - 1];
			for (final MolAtom terminusLig : terminus.getLigands()) {
				final int ligMap = terminusLig.getAtomMap();
				if (ligMap == 0) { // must be in group
					prodAtomsByMaps[IN_ATOM_MAP - 1] = terminusLig;
					break;
				} // if atom is unmapped, hence in group
			} // for each ligand of atomB
			outMapNum = IN_ATOM_MAP;
		} // if diene is cyclic
		setRingStereo(terminusMap, outMapNum);
	} // setDieneTermini(int, boolean)

	/** Sets the stereochemistry of the dienophile substituents in the product.
	 * @param	mappedPhile	the starting dienophile
	 * @param	terminusMap	map atom of the terminus being handled
	 * @param	considerExoEndo	when true, need to consider exo/endo rule; 
	 * endo substituent inverts configuration when diene is cyclic
	 */
	private void setPhileTermini(Molecule mappedPhile, int terminusMap, 
			boolean considerExoEndo) {
		final String SELF = "DielsAlderStereo.setPhileTermini: ";
		int upMapNum = get1stPhileLigandMap(terminusMap);
		if (considerExoEndo) {
			debugPrintMRV(SELF + "need to consider exo/endo "
					+ "selectivity.");
			// which ligand is the most electron-withdrawing?
			final Molecule[] oneLigPhiles = getOneLigPhiles(mappedPhile);
			final int endoGroupMap = getMapOfBestEWG(oneLigPhiles);
			// upMapNum (endo group) has already been set to odd-mapped ligand
			// If most EWG is even and reaction is endo and diene is acyclic
			// or most EWG is odd and reaction is exo and diene is acyclic
			// or most EWG is even and reaction is exo and diene is cyclic
			// or most EWG is odd and reaction is endo and diene is cyclic
			// then set upMapNum to even-mapped ligand
			final boolean endoGroupMapIsEven = endoGroupMap % 2 == 0;
			if ((endoGroupMapIsEven == isEndo && !dieneIsCyclic) 
					|| (endoGroupMapIsEven != isEndo && dieneIsCyclic))
				upMapNum++;
			debugPrint(SELF + "ligand with map ", endoGroupMap, 
					" is most potent electron-withdrawing group; reaction is ",
					isEndo ? "endo" : "exo", " and diene is ", dieneIsCyclic 
						? "" : "not ", "cyclic, so setting map of "
					+ "up (endo) group to ", upMapNum);
		} // if considerExoEndo
		setRingStereo(terminusMap, upMapNum);
	} // setPhileTermini(Molecule, int, boolean)

	/** Gets array of molecules, each of which contains the dienophile
	 * stripped of all ligands but one.
	 * @param	mappedPhile	the starting dienophile
	 * @return	copies of the dienophile with one ligand attached to each
	 */
	private Molecule[] getOneLigPhiles(Molecule mappedPhile) {
		final String SELF = "DielsAlderStereo.getOneLigPhiles: ";
		final int[] phileAtomNumsByMaps = new int[NUM_MAPS];
		Arrays.fill(phileAtomNumsByMaps, -1);
		for (final MolAtom atom : mappedPhile.getAtomArray()) {
			final int mapNum = atom.getAtomMap();
			if (MathUtils.inRange(mapNum, MAPS_RANGE)) {
				phileAtomNumsByMaps[mapNum - 1] = mappedPhile.indexOf(atom);
			} // if map number is in range
		} // for each atom
		final int numPhileBonds = MAPS_PHILE_BONDS.length;
		final Molecule[] oneLigPhiles = new Molecule[numPhileBonds];
		for (int molNum = 0; molNum < numPhileBonds; molNum++) {
			final Molecule oneLigPhile = mappedPhile.clone();
			int bondMapsNum = 0;
			for (final int[] atomMaps : MAPS_PHILE_BONDS) {
				if (bondMapsNum != molNum) {
					final int phileMap = atomMaps[PHILE_ATOM];
					final int ligandMap = atomMaps[LIGAND_ATOM];
					final int phileAtomNum =
							phileAtomNumsByMaps[phileMap - 1];
					final int ligandAtomNum =
							phileAtomNumsByMaps[ligandMap - 1];
					if (phileAtomNum >= 0 && ligandAtomNum >= 0) {
						final MolAtom phileAtom =
								oneLigPhile.getAtom(phileAtomNum);
						final MolAtom ligandAtom =
								oneLigPhile.getAtom(ligandAtomNum);
						oneLigPhile.removeBond(
								phileAtom.getBondTo(ligandAtom));
					} // if bond atoms are found
				} // if bond is to be removed
				bondMapsNum++;
			} // for each bond to dienophile ligands to remove
			final Molecule[] phileFrags = oneLigPhile.convertToFrags();
			for (final Molecule phileFrag : phileFrags) {
				if (containsPhileAtom(phileFrag)) {
					oneLigPhiles[molNum] = phileFrag;
					break;
				} // if the fragment contains dienophile atom
			} // for each fragment
			debugPrintMRV(SELF + "dienophile ", molNum + 1, ":\n", 
					oneLigPhiles[molNum]);
		} // for each one-ligand dienophile
		return oneLigPhiles;
	} // getOneLigPhiles(Molecule)

	/** Determines whether a fragment of a disconnected dienophile contains
	 * either of the two essential atoms of the dienophile.
	 * @param	phileFrag	the fragment
	 * @return	true if the fragment contains a ring atom
	 */
	private boolean containsPhileAtom(Molecule phileFrag) {
		boolean foundPhile = false;
		for (final MolAtom atom : phileFrag.getAtomArray()) {
			if (isPhileAtom(atom)) {
				foundPhile = true;
				break;
			} // if fragment contains dienophile atom
		} // for each atom in the fragment
		return foundPhile;
	} // containsPhileAtom(Molecule)

	/** Gets the map number of the best electron-withdrawing group of the
	 * dienophile.
	 * @param	oneLigPhiles	four dienophiles, each with one of the
	 * original ligands
	 * @return	map number of the most electron-withdrawing ligand of the
	 * dienophile
	 */
	private int getMapOfBestEWG(Molecule[] oneLigPhiles) {
		final String SELF = "DielsAlderStereo.getMapOfBestEWG: ";
		debugPrint(SELF + "looking at dienophile fragments ",
				oneLigPhiles, " for one with best EWG.");
		int ligandNum = 0;
		int mapBestEWG = MAPS_PHILE_BONDS[0][1];
		double largestCharge = 0;
		for (final Molecule oneLigPhile : oneLigPhiles) try {
			final int mapOfLigand = MAPS_PHILE_BONDS[ligandNum][1];
			final ChargePlugin plugin = new ChargePlugin();
			plugin.setMolecule(oneLigPhile);
			plugin.run();
			final int numAtoms = oneLigPhile.getAtomCount();
			int numPhileAtomsFound = 0;
			for (int atomNum = 0; atomNum < numAtoms; atomNum++) {
				final MolAtom atom = oneLigPhile.getAtom(atomNum);
				if (isPhileAtom(atom)) {
					numPhileAtomsFound++;
					final double charge = plugin.getTotalCharge(atomNum);
					if (charge > largestCharge) {
						largestCharge = charge;
						mapBestEWG = mapOfLigand;
					} // if atom is more positively charged
				} // if atom is one of the dienophile atoms
				if (numPhileAtomsFound >= 2) break;
			} // for each atom
			ligandNum++;
		} catch (PluginException e) {
			Utils.alwaysPrint(SELF + "caught PluginException trying to "
					+ "calculate partial charges on ", oneLigPhile);
			e.printStackTrace();
		} // for each molecule with just one dienophile ligand
		debugPrint(SELF + "dienophile fragment with greatest partial charge ", 
				largestCharge, " has ligand with map ", mapBestEWG);
		return mapBestEWG;
	} // getMapOfBestEWG(Molecule[])

	/** Sets the stereochemistry of ring substituents in the product.
	 * @param	ringAtomMap	map number of the ring atom being handled
	 * @param	upMapNum	map number of the group attached to the ring atom
	 * that will automatically be set pointing up
	 */
	private void setRingStereo(int ringAtomMap, int upMapNum) {
		final String SELF = "DielsAlderStereo.setRingStereo: ";
		final MolAtom ringAtom = prodAtomsByMaps[ringAtomMap - 1];
		final MolAtom upAtom = prodAtomsByMaps[upMapNum - 1];
		if (upAtom != null) {
			final MolBond upBond = ringAtom.getBondTo(upAtom);
			if (upBond.getAtom1() == upAtom) upBond.swap();
			StereoFunctions.setBondStereoFlags(upBond, MolBond.UP);
			debugPrint(SELF + "up bond from ", upBond.getAtom1(),
					d_a_Product.indexOf(upBond.getAtom1()) + 1,
					" with map ", upBond.getAtom1().getAtomMap(),
					" to ", upBond.getAtom2(), 
					d_a_Product.indexOf(upBond.getAtom2()) + 1,
					" with map ", upBond.getAtom2().getAtomMap(),
					" has stereo ", 
					StereoFunctions.getBondStereo(upBond));
		} else {
			final int downMapNum = upMapNum + (upMapNum % 2) * 2 - 1;
			final MolAtom downAtom = prodAtomsByMaps[downMapNum - 1];
			if (downAtom != null) {
				final MolBond downBond = ringAtom.getBondTo(downAtom);
				if (downBond.getAtom1() == downAtom) downBond.swap();
				StereoFunctions.setBondStereoFlags(downBond, MolBond.DOWN);
				debugPrint(SELF + "down bond from ", downBond.getAtom1(),
						d_a_Product.indexOf(downBond.getAtom1()) + 1,
						" with map ", downBond.getAtom1().getAtomMap(),
						" to ", downBond.getAtom2(), 
						d_a_Product.indexOf(downBond.getAtom2()) + 1,
						" with map ", downBond.getAtom2().getAtomMap(),
						" has stereo ", 
						StereoFunctions.getBondStereo(downBond));
			} // if there's a down atom
		} // if there's an up atom
	} // setRingStereo(int, int)

	/** Gets an array of the locations of the atoms in the new ring of the
	 * Diels-Alder product.
	 * @return	array of locations
	 */
	private DPoint3[] getRingAtomLocns() {
		final DPoint3[] ringAtomLocns = new DPoint3[NUM_RING_ATOMS];
		for (int ringMapNum = 1; ringMapNum <= NUM_RING_ATOMS; ringMapNum++) {
			final MolAtom ringAtom = prodAtomsByMaps[ringMapNum - 1];
			ringAtomLocns[ringMapNum - 1] = ringAtom.getLocation();
		} // for each ring atom
		return ringAtomLocns;
	} // getRingAtomLocns()

	/** Gets if an atom is a terminus of the dienophile.
	 * @param	atom	the atom
	 * @return	true if the atom has a map number of 5 or 6
	 */
	private boolean isPhileAtom(MolAtom atom) {
		final int map = atom.getAtomMap();
		return Utils.among(map, TERMINUS_MAPS[PHILE_TERMINUS1_POSN],
				TERMINUS_MAPS[PHILE_TERMINUS1_POSN + 1]);
	} // isPhileAtom(MolAtom)

	/** Gets the map number of the out atom attached to a diene terminus.
	 * @param	dieneTerminusMap	map number of the diene terminus (1 or 4)
	 * @return	map number of the out atom (7 or 9)
	 */
	private int getOutAtomMap(int dieneTerminusMap) {
		return getLigandMap(dieneTerminusMap);
	} // getOutAtomMap(int)

	/** Gets the lower of the two map numbers of the atoms attached to a 
	 * dienophile terminus.
	 * @param	phileMap	map number of the dienophile terminus (5 or 6)
	 * @return	map number of the first ligand (11 or 13); second ligand's map
	 * number is 1 greater
	 */
	private int get1stPhileLigandMap(int phileMap) {
		return getLigandMap(phileMap);
	} // get1stPhileLigandMap(int)

	/** Gets the map number of the first ligand of a terminus of a diene or
	 * dienophile.
	 * @param	terminusMap	map number of the terminus
	 * @return	map number of the first ligand
	 */
	private int getLigandMap(int terminusMap) {
		return TERMINUS_LIGANDS[Utils.indexOf(TERMINUS_MAPS, terminusMap)];
	} // getLigandMap(int)

	/** Converts an angle in radians to whole degrees.
	 * @param	radians	the angle in radians
	 * @return	the angle in whole degrees
	 */
	private int toDegrees(double radians) {
		return MathUtils.roundToInt(VectorMath.toDegrees(radians));
	} // toDegrees(double)

} // DielsAlderStereo
