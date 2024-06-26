package com.epoch.chem;

import chemaxon.calculations.ElementalAnalyser;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import chemaxon.struc.PeriodicSystem;
import com.epoch.chem.chemConstants.ChemConstants;
import com.epoch.chem.chemConstants.FormulaConstants;
import com.epoch.lewis.lewisConstants.LewisConstants;
import com.epoch.utils.MathUtils; 
import com.epoch.utils.Utils; 

/** Contains formula matching and calculation functions.  */
public final class FormulaFunctions 
		implements ChemConstants, FormulaConstants, LewisConstants {

	private static void debugPrint(Object... msg) { 
		// Utils.printToLog(msg);
	}

	private static void debugPrintMRV(Object... msg) {
		// Utils.printToLog(msg, MRV);
	}

	/** Determines if the response molecule's formula is the same as 
	 * the author's formula.   
	 * No order of atoms is assumed for the formula.
	 * Author's formula is of the form (([A-Z][a-z]*)(\*?|[0-9]*))+
	 * or an exception is thrown.
	 * D and T are treated as different from H only if the author's formula
	 * contains D or T.  Other isotopes are not considered.
	 * @param	respMol	a response molecule
	 * @param	authFormulaStr	reference formula to which to compare; 
	 * asterisk has a value -1 and will match against any number &ge; 0.
	 * @param	numRGroups	number of R groups included in the formula; 
	 * equals the number of spurious H atoms in authFormulaStr due to dangling 
	 * valences in R groups; if negative, this is a Lewis structure
	 * @return	true if the molecular formula is same as authFormulaStr
	 * @throws	FormulaException	if a formula doesn't match the pattern 
	 * for a formula or if an element symbol is invalid
	 */
	public static boolean hasFormula(Molecule respMol, 
			String authFormulaStr, int numRGroups) throws FormulaException {
		final String SELF = "FormulaFunctions.hasFormula: ";
		debugPrint(SELF + "comparing formula of response molecule ", 
				respMol, " to author formula ", authFormulaStr);
		final boolean isLewis = numRGroups < 0;
		final Formula respFormula = new Formula(respMol, isLewis);
		final Formula authFormula = new Formula(authFormulaStr, ALLOW_ASTERISK);
		// reduce author formula by number of connected R groups
		final int authNumH = authFormula.getNumberOf("H");
		if (numRGroups > 0 && authNumH != -1) { 
			authFormula.put("H", authNumH - numRGroups);
			debugPrint(SELF + "reduced number of H atoms in author formula " 
					+ "by the number of R groups, ", numRGroups);
		} // if author formula must be altered to account for R groups
		// account for H isotopes for non-Lewis structures
		if (!isLewis) {
			final String[] H_ISOTOPES = new String[] {"D", "T"};
			for (final String isoH : H_ISOTOPES) {
				if (authFormula.get(isoH) == null) {
					// author has not specified isotope; convert all 
					// examples of this isotope to H
					final Integer respIsoCt = respFormula.remove(isoH);
					if (respIsoCt != null) {
						respFormula.put("H", 
								respFormula.getNumberOf("H")
								+ respIsoCt.intValue());
					} // if response has an H isotope
				} // if no isotopes specified in author's formula
			} // for each H isotope
		} // if not Lewis structure
		return authFormula.matches(respFormula);
	} // hasFormula(Molecule, String, int)

	/** Counts the number of C atoms in a compound.
	 * @param	mol	a molecule
	 * @return	number of C atoms
	 */
	public static int countCAtoms(Molecule mol) {
		return countAtoms(mol, "C");
	} // countCAtoms(Molecule)

	/** Counts the number of C atoms in a compound.
	 * @param	mol	a molecule
	 * @param	isLewis	is a Lewis structure
	 * @return	number of C atoms
	 */
	public static int countCAtoms(Molecule mol, boolean isLewis) {
		return countAtoms(mol, "C", isLewis);
	} // countCAtoms(Molecule, boolean)

	/** Counts the number of atoms of an element in a compound.
	 * D and T atoms are counted among H atoms when the H atom count is
	 * requested.
	 * @param	mol	a molecule
	 * @param	elem	element to count
	 * @return	number of atoms of the element
	 */
	public static int countAtoms(Molecule mol, String elem) {
		final String isLewisStr = 
				ChemUtils.getProperty(mol, LEWIS_PROPERTY);
		final boolean isLewis = TRUE.equals(isLewisStr);
		return countAtoms(mol, elem, isLewis);
	} // countAtoms(Molecule, String)

	/** Counts the number of atoms of an element in a compound.
	 * Specific isotopes are included in the total atom count if no isotope is
	 * specified by the author.
	 * @param	mol	a molecule
	 * @param	elem	element to count; may be any (X) or an isotope
	 * @param	isLewis	is a Lewis structure
	 * @return	number of atoms of the element
	 */
	public static int countAtoms(Molecule mol, String elem, boolean isLewis) {
		final String SELF = "FormulaFunctions.countAtoms: "; 
		int numAtoms = 0;
		if ("X".equals(elem)) numAtoms = getAtomCount(mol);
		else {
			// separate into atom and mass numbers
			int atomicNo = 1;
			int massNo = "DT".indexOf(elem) + 2;
			if (massNo < 2) {
				final StringBuilder digits = new StringBuilder();
				final StringBuilder letters = new StringBuilder();
				for (final char ch : elem.toCharArray()) {
					if (Utils.isDigit(ch)) digits.append(ch);
					if (Utils.isLetter(ch)) letters.append(ch);
				} // for each char
				atomicNo = PeriodicSystem.findAtomicNumber(letters.toString());
				massNo = MathUtils.parseInt(digits.toString());
			} // parse for isotopes
			debugPrint(SELF + "element ", elem, " has atomic number ", 
					atomicNo, " and mass number ", massNo);
			final ElementalAnalyser analyzer = new ElementalAnalyser();
			analyzer.setMolecule(mol);
			numAtoms = (massNo == 0 ? analyzer.atomCount(atomicNo)
					: analyzer.atomCount(atomicNo, massNo));
			if ("H".equals(elem) && isLewis) {
				numAtoms -= mol.getImplicitHcount();
			} // if Lewis structure and counting H atoms
		} // if counting specific elements
		debugPrint(SELF + "returning ", numAtoms, " ", elem, " atom(s) in ", 
				(isLewis ? "Lewis structure." : "molecule."));
		return numAtoms;
	} // countAtoms(Molecule, String, boolean)

	/** Counts the largest number of contiguous C atoms.
	 * @param	mol	a molecule
	 * @return	the largest number of contiguous C atoms
	 */
	public static int countContiguousCAtoms(Molecule mol) {
		return countContiguous(mol, "C");
	} // countContiguousCAtoms(Molecule)

	/** Counts the largest number of contiguous atoms of an element.
	 * @param	mol	a molecule
	 * @param	element	symbol of element to look for 
	 * @return	the largest number of contiguous atoms of the element
	 */
	public static int countContiguous(Molecule mol, String element) {
		final String SELF = "FormulaFunctions.countContiguous: "; 
		debugPrintMRV(SELF + "counting contiguous ", element, 
				" atoms in:\n", mol);
		int maxNumContiguous = 0;
		final int atomCount = mol.getAtomCount();
		final boolean[] beenThere = new boolean[atomCount];
		for (int atomNum = 0; atomNum < atomCount; atomNum++) {
			final MolAtom atom = mol.getAtom(atomNum);
			if (atom.getSymbol().equals(element) && !beenThere[atomNum]) { 
				final int numContiguous = countContiguous(mol, atomNum, 
						beenThere, element, 0);
				debugPrint(SELF + "Found ", numContiguous, 
						" ", element, " atoms.");
				if (numContiguous > maxNumContiguous) 
					maxNumContiguous = numContiguous;
			} // if we did a new recursive search
		} // for each atom in molecule
		return maxNumContiguous;
	} // countContiguous(Molecule, String)

	/** Counts the largest number of contiguous atoms of an element
	 * starting from a particular atom.  This method is called recursively.
	 * @param	mol	a molecule
	 * @param	atom0Idx	index of an atom of the element we're looking for
	 * @param	beenThere	array in which each position corresponds to the
	 * index of an atom; that element is true if the corresponding atom has 
	 * already been examined for being part of a contiguous region  
	 * @param	element	symbol of element to look for 
	 * @param	numContiguous the number of contiguous atoms of the element
	 * recursively counted so far
	 * @return	the number of contiguous atoms of the element that contain atom0
	 */
	private static int countContiguous(Molecule mol, int atom0Idx, 
			boolean[] beenThere, String element, final int numContiguous) {
		final String SELF = "FormulaFunctions.countContiguous: "; 
		debugPrint(SELF + "Atom ", atom0Idx + 1, " is ", element);
		beenThere[atom0Idx] = true;
		int answer = numContiguous + 1;
		final MolAtom atom0 = mol.getAtom(atom0Idx);
		for (final MolAtom atom1 : atom0.getLigands()) {
			final int atom1Idx = mol.indexOf(atom1);
			if (atom1.getSymbol().equals(element) && !beenThere[atom1Idx])
				answer = countContiguous(mol, atom1Idx, beenThere,
						element, answer);
		} // for each atom attached to atom0
		return answer;
	} // countContiguous(Molecule, MolAtom, boolean[], String, int)

	/** Returns the number of real atoms in the compound, omitting shortcut
	 * groups and multicenter attachment points and excluding implicit H atoms.
	 * @param	mol	a molecule
	 * @return	the number of real atoms in the compound, excluding implicit H
	 * atoms
	 */
	public static int getExplicitAtomCount(Molecule mol) {
		return getAtomCount(mol, false);
	} // getExplicitAtomCount(Molecule)

	/** Returns the number of real atoms in the compound, omitting shortcut
	 * groups and multicenter attachment points, and including implicit H atoms.
	 * @param	mol	a molecule
	 * @return	the number of real atoms and implicit H atoms in the compound
	 */
	public static int getAtomCount(Molecule mol) {
		return getAtomCount(mol, true);
	} // getAtomCount(Molecule)

	/** Returns the number of real atoms in the compound, omitting shortcut
	 * groups and multicenter attachment points.
	 * @param	mol	a molecule
	 * @param	withImplicitH	include the implicit H atoms
	 * @return	the number of real atoms in the compound
	 */
	public static int getAtomCount(Molecule mol, boolean withImplicitH) {
		final String SELF = "FormulaFunctions.getAtomCount: "; 
		final ElementalAnalyser analyzer = new ElementalAnalyser();
		analyzer.setMolecule(mol);
		int atomCount = analyzer.atomCount();
		if (!withImplicitH) atomCount -= mol.getImplicitHcount();
		/* int atomCount = mol.getAtomCount();
		debugPrint(SELF + "initial atom count = ", atomCount);
		for (final MolAtom atom : mol.getAtomArray()) {
			if (ChemUtils.isMulticenterAtom(atom)
					|| atom instanceof SgroupAtom) {
				debugPrint(SELF + "atom ", atom, mol.indexOf(atom), 
						" is not a real atom.");
				atomCount--;
			} // if not a real atom
		} // for each atom in molecule
		if (withImplicitH) atomCount += mol.getImplicitHcount(); /**/
		return atomCount; 
	} // getAtomCount(Molecule, boolean)

	/** Disables external instantiation. */
	private FormulaFunctions() { }

} // FormulaFunctions
